package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.registro.ModSons;

/**
 * A Conta (0.9): a dívida escondida que une todos os itens.
 *
 * Cada uso de um item ligado à criatura soma um pouco. Sem usar nada, a conta cai 1 a cada dez minutos.
 * O limite é sorteado a cada ciclo (6 a 10), para ninguém decorar. Antes de cobrar vêm três avisos sem
 * texto, um por degrau; ao estourar, ele cobra na moeda do item em que o jogador mais se apoiou, e a conta
 * zera. A cobrança nunca mata e nunca estraga nada.
 *
 * É isto que deixa os itens serem bons de verdade: o preço não está em cada uso, está em se apoiar demais.
 *
 * Tudo aqui fica na Memoria (salva com o personagem). Uso forçado por comando de teste não soma.
 */
public final class Conta {
	public enum Item {
		VELA(1), SINO(1), FIO(1), ISCA(2), OLHO(3), CAIXA(2), OSSOS(1), LINHA(1), LAMPIAO(1);

		final int peso;

		Item(int peso) {
			this.peso = peso;
		}

		String chave() {
			return "conta_" + this.name().toLowerCase(Locale.ROOT);
		}

		/** Marca "a próxima vez que ele usar este item, é cobrança". */
		String chaveCobrado() {
			return "cobrado_" + this.name().toLowerCase(Locale.ROOT);
		}
	}

	static final String TOTAL = "conta";
	static final String LIMITE = "conta_limite";
	static final String AVISOS = "conta_avisos";
	static final String DECAIU_EM = "conta_decaiu_em";

	/** A cada tantos segundos sem estourar, a conta perde 1. */
	private static final int DECAI_A_CADA = 600;

	private Conta() {
	}

	/** Um uso de item. Abre e salva a Memoria: chamar só de fora do tick do Diretor (uso de item, clique em bloco). */
	public static void somar(ServerPlayer p, Item item) {
		Memoria m = Memoria.de(p);
		somar(p, m, item, 1);
		m.salvar();
	}

	/** O mesmo, para quem já tem a Memoria aberta e vai salvá-la. */
	static void somar(ServerPlayer p, Memoria m, Item item, int vezes) {
		if (m.get(LIMITE) == 0) {
			m.set(LIMITE, 6 + p.getRandom().nextInt(5));
		}
		m.add(TOTAL, item.peso * vezes);
		m.add(item.chave(), vezes);
		Depuracao.log(p, p.level().getGameTime() / 20, String.format(Locale.ROOT, "CONTA +%d item=%s total=%d limite=%d",
				item.peso * vezes, item, m.get(TOTAL), m.get(LIMITE)));
	}

	/** O jogador vai usar este item agora: há cobrança pendente nele? Consome a marca. */
	static boolean cobrarNoUso(Memoria m, Item item) {
		if (m.get(item.chaveCobrado()) == 0) {
			return false;
		}
		m.set(item.chaveCobrado(), 0);
		return true;
	}

	/** Uma vez por segundo, dentro do tick do Diretor (recebe a Memoria do tick). */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick) {
		int total = m.get(TOTAL);
		if (total <= 0) {
			return;
		}
		int limite = m.get(LIMITE, 8);

		// Sem estourar, esfria devagar.
		if (m.get(DECAIU_EM) == 0 || seg < m.get(DECAIU_EM)) {
			m.set(DECAIU_EM, (int) seg);
		} else if (seg - m.get(DECAIU_EM) >= DECAI_A_CADA && e.cobrancaEm < 0) {
			m.set(DECAIU_EM, (int) seg);
			m.set(TOTAL, total - 1);
			Depuracao.log(p, seg, "CONTA esfriou total=" + (total - 1) + " limite=" + limite);
			return;
		}

		// Os três avisos, um por degrau antes do limite.
		int degrau = total >= limite - 1 ? 3 : total >= limite - 2 ? 2 : total >= limite - 3 ? 1 : 0;
		// Os avisos não passam pelo orçamento de atenção: são consequência do que o próprio jogador fez com os
		// itens. (Na alpha12 passavam, e quem usava muito item gastava com avisos o saldo dos eventos.)
		if (degrau > m.get(AVISOS) && !Diretor.bloqueado(p, e, tick)) {
			m.set(AVISOS, degrau);
			avisar(level, p, m, e, degrau, seg, tick);
		}

		if (total >= limite && e.cobrancaEm < 0) {
			// Não cobra na hora: daqui a meio minuto ou dois, quando ele já tiver esquecido o que fez.
			e.cobrancaEm = tick + 20L * (30 + p.getRandom().nextInt(91));
			Depuracao.log(p, seg, "CONTA estourou total=" + total + " limite=" + limite + " cobraEm=" + (e.cobrancaEm - tick) / 20 + "s");
		}
		if (e.cobrancaEm >= 0 && tick >= e.cobrancaEm && !Diretor.bloqueado(p, e, tick)) {
			e.cobrancaEm = -1;
			cobrar(level, p, m, e, maisUsado(m), seg);
		}
	}

	private static void avisar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int degrau, long seg, long tick) {
		RandomSource sorte = p.getRandom();
		String como;
		switch (degrau) {
			case 1 -> {
				// As chamas encolhem: os lampiões por perto ficam inquietos, e uma ou duas tochas falham.
				int lampioes = LampiaoPalidoBlock.perturbar(level, p.blockPosition(), 16, LampiaoPalidoBlock.Chama.INQUIETA, 120);
				int tochas = ApoioCaca.apagarLuzPerto(level, p, p.blockPosition(), 8, 50 + sorte.nextInt(30), 2);
				como = "CHAMAS lampioes=" + lampioes + " tochas=" + tochas;
			}
			case 2 -> {
				// A cantiga, com notas faltando. Para quem ainda não tem a caixa, um sino longe.
				Vec3 ponto = Diretor.pontoRelativo(p, 150 + sorte.nextDouble() * 60, 14);
				boolean conhece = m.get(Memoria.CAIXA_USOS) > 0;
				ModSons.Som som = conhece ? ModSons.Som.CAIXA_QUEBRADA : ModSons.Som.SINO_LONGE;
				ModSons.tocarPara(p, ponto.x, p.getY() + 1.0, ponto.z, som,
						Diretor.volumePara(p, ponto.x, p.getY() + 1.0, ponto.z, 0.6F), conhece ? 0.9F : 1.0F);
				como = conhece ? "CANTIGA_FALHADA" : "SINO";
			}
			default -> {
				// O último: um zumbido no ouvido e a sensação de que há alguém.
				ModSons.tocarNaCabeca(p, ModSons.Som.ZUMBIDO, 0.7F, 1.0F);
				e.vigiaFalsaAte = tick + 20L * 20;
				e.vigiaFalsaForca = 0.7F;
				como = "ZUMBIDO";
			}
		}
		Depuracao.log(p, seg, "CONTA aviso=" + degrau + "/3 como=" + como + " total=" + m.get(TOTAL) + " limite=" + m.get(LIMITE));
	}

	private static Item maisUsado(Memoria m) {
		Item maior = Item.VELA;
		int usos = -1;
		for (Item item : Item.values()) {
			int n = m.get(item.chave()) * item.peso;
			if (n > usos) {
				usos = n;
				maior = item;
			}
		}
		return maior;
	}

	/** A cobrança, na moeda do item em que ele mais se apoiou. Depois, tudo zera e um limite novo é sorteado. */
	private static void cobrar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Item item, long seg) {
		String como;
		switch (item) {
			case SINO -> {
				// O sino toca sozinho, de onde estiver guardado. Três vezes, espaçadas.
				for (int i = 0; i < 3; i++) {
					Diretor.agendar(level, 40 + i * (240 + p.getRandom().nextInt(200)), () -> {
						if (!p.isRemoved()) {
							ModSons.tocarNaCabeca(p, ModSons.Som.GRAVE, 0.38F, 1.35F);
							ModSons.tocarNaCabeca(p, ModSons.Som.ESTALO, 0.30F, 0.72F);
						}
					});
				}
				como = "O_SINO_TOCA_SOZINHO";
			}
			case FIO -> {
				// Alguém dedilha o fio por fora: três estalos, nenhum rompimento.
				for (int i = 0; i < 3; i++) {
					Diretor.agendar(level, 20 + i * (100 + p.getRandom().nextInt(120)), () -> {
						if (!p.isRemoved()) {
							Vec3 ponto = Diretor.pontoRelativo(p, 120 + p.getRandom().nextDouble() * 120, 6);
							ModSons.tocarPara(p, ponto.x, p.getY() + 0.3, ponto.z, ModSons.Som.ESTALO, 0.7F, 0.72F);
						}
					});
				}
				como = "DEDILHA_O_FIO";
			}
			case CAIXA -> {
				// A caixa toca sozinha, da mochila: gasta, torta, sem ninguém ter dado corda.
				ModSons.tocarNaCabeca(p, ModSons.Som.CAIXA_ARRUINADA, 0.5F, 0.92F);
				como = "A_CAIXA_TOCA_SOZINHA";
			}
			case LINHA -> {
				// Uma das linhas dele é gasta de uma vez, até romper.
				BlockPos linha = CinzaEspalhadaBlock.linhaPerto(level, p.blockPosition(), 16);
				if (linha != null) {
					while (CinzaEspalhadaBlock.desgastar(level, linha)) {
						// até romper
					}
					ModSons.tocar(level, linha.getX() + 0.5, linha.getY() + 0.2, linha.getZ() + 0.5, ModSons.Som.ARRASTO, 0.8F, 0.8F);
					como = "ROMPEU_UMA_LINHA pos=" + linha.toShortString();
				} else {
					m.set(Item.VELA.chaveCobrado(), 1);
					como = "SEM_LINHA_PERTO->VELA";
				}
			}
			case LAMPIAO -> {
				int n = LampiaoPalidoBlock.perturbar(level, p.blockPosition(), 24, LampiaoPalidoBlock.Chama.FRIA, 20 * 60);
				como = "LAMPIOES_FRIOS n=" + n;
			}
			default -> {
				// Vela, Olho, Isca e Ossos: vale no próximo uso (ver cobrarNoUso).
				m.set(item.chaveCobrado(), 1);
				como = "NO_PROXIMO_USO";
			}
		}
		Depuracao.log(p, seg, "CONTA cobranca item=" + item + " como=" + como + " total=" + m.get(TOTAL));
		m.set(TOTAL, 0);
		m.set(AVISOS, 0);
		m.set(LIMITE, 6 + p.getRandom().nextInt(5));
		m.set(DECAIU_EM, (int) seg);
		for (Item i : Item.values()) {
			m.set(i.chave(), 0);
		}
	}
}
