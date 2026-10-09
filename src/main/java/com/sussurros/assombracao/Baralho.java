package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import com.sussurros.rede.Rede;

/**
 * O baralho (0.9): o que faz cada mundo ter a sua ordem.
 *
 * Cada jogador tem um baralho de doze cartas, embaralhado com a semente do mundo e a identidade dele. Uma carta
 * é virada a cada 40 a 90 minutos de jogo, da fase 2 em diante. Três cartas são "nada". As outras nove não
 * inventam coisa nova: cada uma adianta, fora de hora, algo que o mod já sabe fazer.
 *
 * | carta    | o que adianta                                                              |
 * | VEU      | o Véu abre agora, sem respeitar o intervalo (fase 3+)                       |
 * | SOLEIRA  | a porta sozinha é erguida perto (30 a 55 blocos), se ainda não existe       |
 * | BONECO   | a figura de palha começa a vir esta noite; se já vem, pula um passo         |
 * | SONO     | na próxima vez que ele deitar, é levado para o outro lado (fase 3+)         |
 * | CACADA   | uma caçada fica devendo: vem quando as condições de sempre valerem (fase 4) |
 * | PRESENTE | na próxima manhã há algo ao lado da cama, sem sorteio                       |
 * | CHAMAS   | por dez minutos, até três tochas a mais empalidecem                         |
 * | CASA     | a casa de quem escreveu o diário é erguida agora, se ainda não existe       |
 * | LINHAS   | na próxima manhã, a linha de cinza mais perto da cama amanhece rompida      |
 *
 * Quem lê o código sabe quais são as cartas, mas não a ordem delas num mundo: ela só existe depois que o mundo
 * existe. Esgotado o baralho, ele é embaralhado de novo, em outra ordem.
 *
 * Uma carta que não cabe agora (o Véu com a vela acesa, por exemplo) espera um minuto e tenta de novo, até dez
 * vezes. Nenhuma é virada com criatura presente, com o Véu aberto ou com o jogador em perigo de verdade.
 */
public final class Baralho {
	public enum Carta {
		NADA, VEU, SOLEIRA, BONECO, SONO, CACADA, PRESENTE, CHAMAS, CASA, LINHAS
	}

	static final String CICLO = "baralho_ciclo";
	static final String POS = "baralho_pos";
	static final String FALTA = "baralho_falta";
	static final String TENTATIVAS = "baralho_tentativas";
	/** Marcas que duas cartas deixam para a manhã seguinte (lidas no acordar do Diretor). */
	static final String PRESENTE = "baralho_presente";
	static final String LINHAS = "baralho_linhas";

	private static final int NADAS = 3;

	private Baralho() {
	}

	/** A ordem das cartas de um ciclo. Depende só da semente do mundo, do jogador e do número do ciclo. */
	public static List<Carta> ordem(long semente, UUID jogador, int ciclo) {
		List<Carta> cartas = new ArrayList<>();
		for (Carta c : Carta.values()) {
			if (c != Carta.NADA) {
				cartas.add(c);
			}
		}
		for (int i = 0; i < NADAS; i++) {
			cartas.add(Carta.NADA);
		}
		RandomSource sorte = RandomSource.create(semente ^ jogador.getMostSignificantBits()
				^ Long.rotateLeft(jogador.getLeastSignificantBits(), 17) ^ (ciclo * 0x9E3779B97F4A7C15L));
		for (int i = cartas.size() - 1; i > 0; i--) {
			int j = sorte.nextInt(i + 1);
			Carta troca = cartas.get(i);
			cartas.set(i, cartas.get(j));
			cartas.set(j, troca);
		}
		return cartas;
	}

	/** Uma vez por segundo, dentro do tick do Diretor. */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg, long tick) {
		if (fase < 2 || level.dimension() != Level.OVERWORLD) {
			return;
		}
		if (m.get(CICLO) == 0) {
			m.set(CICLO, 1);
			m.set(POS, 0);
			m.set(FALTA, intervalo(p));
			return;
		}
		if (m.get(FALTA) > 0) {
			m.add(FALTA, -1);
			return;
		}
		if ((e.criatura != null && !e.criatura.isRemoved()) || tick < e.veuAte || Diretor.bloqueado(p, e, tick)
				|| !Atencao.podeGastar(e, Atencao.CARTA, seg)) {
			return;
		}
		virar(level, p, m, e, fase, seg, tick, null);
	}

	/** Vira a próxima carta (ou a carta "forcada", nos testes). Devolve "CARTA efeito". */
	static String virar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg, long tick, @Nullable Carta forcada) {
		int ciclo = Math.max(1, m.get(CICLO));
		List<Carta> cartas = ordem(level.getSeed(), p.getUUID(), ciclo);
		int pos = Math.min(m.get(POS), cartas.size() - 1);
		Carta carta = forcada != null ? forcada : cartas.get(pos);
		String efeito = aplicar(level, p, m, e, carta, fase, seg, tick);
		if (efeito == null && forcada == null && m.get(TENTATIVAS) < 10) {
			// Não cabe agora: espera um minuto e tenta a mesma carta de novo.
			m.add(TENTATIVAS, 1);
			m.set(FALTA, 60);
			Depuracao.log(p, seg, String.format(Locale.ROOT, "BARALHO carta=%s adiada tentativa=%d", carta, m.get(TENTATIVAS)));
			return carta + " adiada";
		}
		if (efeito == null) {
			efeito = "nao_coube";
		}
		if (forcada == null) {
			m.set(TENTATIVAS, 0);
			m.set(FALTA, intervalo(p));
			if (pos + 1 >= cartas.size()) {
				m.set(CICLO, ciclo + 1);
				m.set(POS, 0);
			} else {
				m.set(CICLO, ciclo);
				m.set(POS, pos + 1);
			}
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT, "BARALHO carta=%s efeito=%s pos=%d/%d ciclo=%d proximaEm=%ds%s",
				carta, efeito, pos + 1, cartas.size(), ciclo, m.get(FALTA), forcada != null ? " teste=sim" : ""));
		return carta + " " + efeito;
	}

	/** De quarenta a noventa minutos de jogo. */
	private static int intervalo(ServerPlayer p) {
		return 2400 + p.getRandom().nextInt(3001);
	}

	/** O que a carta faz. Devolve o efeito para o log, ou null se ela não cabe agora e deve esperar. */
	@Nullable
	private static String aplicar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Carta carta, int fase, long seg, long tick) {
		RandomSource sorte = p.getRandom();
		switch (carta) {
			case VEU -> {
				if (fase < 3 || !Rede.temCliente(p)) {
					return "nada";
				}
				if (!Veu.pode(level, p, e, tick, false)) {
					return null;
				}
				Veu.abrir(level, p, m, e, tick, false);
				Atencao.gastar(p, e, "carta:VEU", Atencao.VEU, seg);
				return "abriu";
			}
			case SOLEIRA -> {
				if (m.get(Erguidos.SOLEIRA) == 1) {
					return "ja_existe";
				}
				BlockPos pos = EstruturasSussurros.procurarSuperficie(level, p, sorte, 30, 55, 2);
				if (pos == null || !EstruturasSussurros.longeDaCasa(m, pos, 30)) {
					return null;
				}
				Erguidos.erguerSoleira(level, m, pos, sorte.nextInt(2), sorte);
				return "erguida pos=" + pos.toShortString();
			}
			case BONECO -> {
				if (m.get(Memoria.TEM_CAMA) != 1) {
					return "sem_cama";
				}
				if (m.get(Erguidos.BONECO) == 1) {
					m.set(Erguidos.BONECO_PASSO, Math.min(Erguidos.DISTANCIAS.length - 1, m.get(Erguidos.BONECO_PASSO) + 1));
					return "pulou_um_passo";
				}
				m.set(Erguidos.BONECO_VOLTA, 0);
				return "comeca_esta_noite";
			}
			case SONO -> {
				if (fase < 3 || !Rede.temCliente(p) || !Avesso.existe(level.getServer())) {
					return "nada";
				}
				m.set(Avesso.MARCADO, 1);
				return "marcado";
			}
			case CACADA -> {
				if (fase < 4 || m.get(Memoria.CACA_DEVIDA) == 1) {
					return "nada";
				}
				m.set(Memoria.CACA_DEVIDA, 1);
				e.cacaDevidaApos = 0;
				return "devida";
			}
			case PRESENTE -> {
				m.set(PRESENTE, 1);
				return "na_proxima_manha";
			}
			case CHAMAS -> {
				e.chamasExtraAteSeg = seg + 600;
				return "dez_minutos";
			}
			case CASA -> {
				if (m.get(CasaDoVigia.EXISTE) == 1) {
					return "ja_existe";
				}
				BlockPos pos = EstruturasSussurros.procurarSuperficie(level, p, sorte, 90, 140, 4);
				if (pos == null || !EstruturasSussurros.longeDaCasa(m, pos, 90)) {
					return null;
				}
				CasaDoVigia.erguer(level, m, pos, sorte);
				return "erguida pos=" + pos.toShortString();
			}
			case LINHAS -> {
				m.set(LINHAS, 1);
				return "na_proxima_manha";
			}
			default -> {
				return "nada";
			}
		}
	}

	// ----- Testes -----

	/** Vira agora a próxima carta deste jogador, sem esperar o relógio. */
	public static String virarParaTeste(ServerPlayer p) {
		return virarComMemoria(p, null);
	}

	/** Aplica agora uma carta escolhida, sem andar no baralho. */
	public static String aplicarParaTeste(ServerPlayer p, Carta carta) {
		return virarComMemoria(p, carta);
	}

	private static String virarComMemoria(ServerPlayer p, @Nullable Carta carta) {
		ServerLevel level = p.level();
		Memoria m = Memoria.de(p);
		if (m.get(CICLO) == 0) {
			m.set(CICLO, 1);
		}
		String feito = virar(level, p, m, Diretor.estadoParaTeste(p), m.get(Memoria.FASE), level.getGameTime() / 20, level.getGameTime(), carta);
		m.salvar();
		return feito;
	}
}
