package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModSons;

/**
 * A afronta (0.9.0-alpha25): o que o jogador faz ao Hóspede fica guardado, por dias.
 *
 * Até aqui cada coisa que ele fazia ao Hóspede tinha o seu contador, e nenhum respondia de um jeito que desse
 * para ler: bater somava num número que o deixava mais ousado, oferecer uma arma contra ele na Tigela pesava a
 * porta por quatro minutos. O dono pediu "ações que podem irritar o monstro" (11/10/2026), bateu nele na caverna,
 * e a resposta foi a de sempre.
 *
 * Agora há um número só por jogador, de 0 a 100, escondido, que **cai pela metade a cada dia do jogo**.
 *
 * Sobe com o que ele faz de propósito: bater nele, correr para cima dele, fechar a passagem com um bloco na
 * frente dele, oferecer na Tigela uma ferramenta contra ele, deixar a Tigela vazia depois de três noites aceitas.
 * Desce com a oferenda aceita, com a primeira jogada de Ossos do dia, e quando ele o vê de verdade e o deixa em
 * paz. São as duas mãos: a relação anda para os dois lados, e nenhum dos gatilhos é coisa que se faz sem querer.
 *
 * Tem duas respostas, e é a segunda que é nova:
 *
 *  1. **Na hora** (o sinal de que ele registrou, sempre o mesmo): as chamas em volta do jogador ficam pálidas
 *     todas de uma vez, por uns segundos. Sem chama por perto, o mundo emudece. Fechar a passagem tem a sua:
 *     duas batidas do outro lado do bloco e uma rachadura nele.
 *  2. **Nos dias seguintes**: enquanto o número está alto, a porta e a janela pesam mais no sorteio; de 40 para
 *     cima, na primeira noite em que o jogador estiver em casa, ele vem à janela (ou bate à porta) sem sorteio,
 *     uma vez por noite; de 80 para cima, na fase 4, o descanso até a próxima sequência de ameaça acaba em dois
 *     minutos (a sequência vem com todos os avisos dela).
 *
 * O que não faz: não tira vida, não tira item, não mexe em bloco, não começa caçada na hora. Quem não quer
 * irritar, não irrita. A trégua da oferenda aceita continua valendo: com ela, nada disto acontece dentro de casa.
 *
 * Nada aqui usa o sorteio do mundo, e criatura de comando não conta.
 */
public final class Afronta {
	static final String VALOR = "afronta";
	static final String DIA = "afronta_dia";
	static final String VISITA_DIA = "afronta_visita_dia";
	/** Quantas vezes ele veio por causa da afronta. É o que faz a página do diário sobre isso chegar depois. */
	static final String VISITAS = "afronta_visitas";

	static final int MAXIMO = 100;
	/** Daqui para cima, na primeira noite em casa, ele vem. */
	static final int IRRITADO = 40;
	/** Daqui para cima, na fase 4, o descanso até a próxima ameaça acaba. */
	static final int FURIOSO = 80;
	/** Quanto tempo de casa, à noite, antes de ele vir (segundos). */
	static final int ESPERA_DA_VISITA = 30;
	/** O descanso que sobra quando a ameaça é adiantada (segundos). */
	static final int DESCANSO_CURTO = 120;

	static final String MIRAGEM = "AFRONTA";

	/** O que soma e o que tira. O sinal dos pontos é a mão. */
	public enum Motivo {
		BATEU(25), INVESTIU(10), FECHOU(15), OFENDEU(20), DESFEZ(15),
		OFERECEU(-15), CONSULTOU(-5), DEIXOU(-3),
		/** Ele veio: uma parte da conta está paga. */
		VISITOU(-20), COBROU(-40);

		final int pontos;

		Motivo(int pontos) {
			this.pontos = pontos;
		}
	}

	private Afronta() {
	}

	// ===== As contas, sem mundo (testadas em JUnit) =====

	/** Cai pela metade a cada dia do jogo. Depois de uma semana, não sobra nada. */
	static int decair(int valor, int dias) {
		if (dias <= 0) {
			return valor;
		}
		return dias >= 7 ? 0 : valor >> dias;
	}

	static int somar(int valor, int pontos) {
		return Math.max(0, Math.min(MAXIMO, valor + pontos));
	}

	/**
	 * Quanto a afronta pesa no sorteio de um evento. É na porta que ele cobra (até quatro vezes mais), e ele
	 * aparece um pouco mais (até duas vezes). O resto não muda.
	 */
	static double peso(int valor, Evento ev) {
		return switch (ev) {
			case BATIDA, PORTA -> 1.0 + valor / 33.0;
			case PRESENCA, ATRAS -> 1.0 + valor / 100.0;
			default -> 1.0;
		};
	}

	/** Ele vem esta noite? Uma vez por noite, com o jogador em casa há meio minuto, e nada mais acontecendo. */
	static boolean deveVisitar(int valor, boolean noite, boolean emCasa, long segundosEmCasa, int diaDaUltima, int dia, boolean livre) {
		return valor >= IRRITADO && noite && emCasa && segundosEmCasa >= ESPERA_DA_VISITA && diaDaUltima != dia && livre;
	}

	// ===== O número =====

	/** A noite pertence ao dia em que começou: de madrugada ainda é a de ontem. */
	static int diaDaNoite(ServerLevel level) {
		long relogio = level.getDefaultClockTime();
		return (int) (relogio / 24000L) - (Math.floorMod(relogio, 24000L) < 12000 ? 1 : 0);
	}

	static int valor(Memoria m) {
		return m.get(VALOR);
	}

	static boolean irritado(Memoria m) {
		return m.get(VALOR) >= IRRITADO;
	}

	/**
	 * O jogador fez (ou deixou de fazer) alguma coisa. Pode ser chamado de qualquer lugar: do tick da criatura, de
	 * um clique, de dentro do tick do Diretor. O número só muda no próximo segundo do Diretor (com a Memoria do
	 * tick; ver segundo), mas a resposta na hora sai agora. onde: de onde ele "responde" (o lugar dele, ou o bloco).
	 */
	static void anotar(ServerPlayer p, EstadoJogador e, Motivo motivo, @Nullable Vec3 onde) {
		e.afrontasPendentes.add(motivo);
		// A resposta na hora é para o que ele faz diante do Hóspede. O que acontece na Tigela, de noite e sem
		// ninguém olhando, já tem a sua consequência (a porta), e ele nem está ali para ver.
		boolean naCara = motivo == Motivo.BATEU || motivo == Motivo.INVESTIU || motivo == Motivo.FECHOU;
		if (naCara && p.level() instanceof ServerLevel level) {
			e.afrontaResposta = responder(level, p, e, motivo, onde);
		}
	}

	/** Uma vez por segundo, dentro do tick do Diretor (recebe a Memoria do tick). */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg) {
		int dia = (int) (level.getDefaultClockTime() / 24000L);
		int ultimo = m.get(DIA, dia);
		if (ultimo != dia) {
			int antes = m.get(VALOR);
			int depois = decair(antes, dia - ultimo);
			m.set(VALOR, depois);
			if (antes != depois) {
				Depuracao.log(p, seg, "AFRONTA esfriou de=" + antes + " para=" + depois + " dias=" + (dia - ultimo));
			}
		}
		m.set(DIA, dia);

		if (!e.afrontasPendentes.isEmpty()) {
			List<Motivo> pendentes = new ArrayList<>(e.afrontasPendentes);
			e.afrontasPendentes.clear();
			for (Motivo motivo : pendentes) {
				int total = somar(m.get(VALOR), motivo.pontos);
				m.set(VALOR, total);
				Depuracao.log(p, seg, String.format(Locale.ROOT, "AFRONTA %+d por=%s total=%d resposta=%s",
						motivo.pontos, motivo, total, e.afrontaResposta));
			}
			e.afrontaResposta = "-";
		}

		// Furioso, na última fase: o descanso até a próxima sequência de ameaça acaba em dois minutos. Ela vem
		// como sempre vem, com os avisos dela; isto só tira a espera. Uma parte da conta fica paga.
		if (fase >= 4 && m.get(VALOR) >= FURIOSO && e.estado != EstadoDiretor.AMEACANDO
				&& e.ameacaLiberadaEm > seg + DESCANSO_CURTO) {
			long faltavam = e.ameacaLiberadaEm - seg;
			e.ameacaLiberadaEm = seg + DESCANSO_CURTO;
			m.set(Memoria.AMEACA_LIBERADA_EM, (int) Math.min(Integer.MAX_VALUE, e.ameacaLiberadaEm));
			int total = somar(m.get(VALOR), Motivo.COBROU.pontos);
			m.set(VALOR, total);
			Depuracao.log(p, seg, "AFRONTA adiantou ameaca faltavam=" + faltavam + "s agora=" + DESCANSO_CURTO + "s total=" + total);
		}
	}

	/**
	 * A visita: irritado, na primeira noite em que o jogador está em casa, ele vem à janela, ou bate à porta se a
	 * casa não tem vidro. Chamado junto com as outras cenas (mesma trégua, mesmo saldo de atenção). Devolve true
	 * se ele veio.
	 */
	static boolean visitar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, boolean noite, boolean calma,
			long seg, long tick, RandomSource rnd) {
		int dia = diaDaNoite(level);
		boolean livre = !calma && (e.criatura == null || e.criatura.isRemoved()) && !EstruturasSussurros.temCenaAtiva(e)
				&& e.cenaJanela == EstadoJogador.CenaJanela.NENHUMA
				&& e.estado != EstadoDiretor.AMEACANDO && e.estado != EstadoDiretor.RECUANDO
				&& CenaDoOutroLadoDoVidro.faseServe(fase, PrimeiroContato.feito(m));
		boolean emCasa = e.contexto == ContextoMundo.Tipo.CASA;
		long emCasaHa = emCasa && e.contextoDesde >= 0 ? seg - e.contextoDesde : 0;
		if (!deveVisitar(m.get(VALOR), noite, emCasa, emCasaHa, m.get(VISITA_DIA, Integer.MIN_VALUE), dia, livre)) {
			return false;
		}
		return visitarAgora(level, p, m, e, dia, seg, tick, rnd);
	}

	private static boolean visitarAgora(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int dia, long seg, long tick, RandomSource rnd) {
		Diretor.atualizarCacheAmbiente(level, p, e);
		String como;
		if (!e.janelas.isEmpty()) {
			CenaDoOutroLadoDoVidro.iniciarCenaJanela(p, e, seg, rnd, false);
			como = "JANELA";
		} else if (Diretor.baterNaPortaPelaAfronta(level, p, m, e, seg, tick)) {
			como = "BATIDA";
		} else {
			return false; // nem vidro nem porta: tenta de novo mais tarde, na mesma noite
		}
		m.set(VISITA_DIA, dia);
		m.add(VISITAS, 1);
		int total = somar(m.get(VALOR), Motivo.VISITOU.pontos);
		m.set(VALOR, total);
		Depuracao.log(p, seg, "AFRONTA visita=" + como + " total=" + total + " visitas=" + m.get(VISITAS));
		return true;
	}

	// ===== A resposta na hora =====

	/**
	 * O sinal de que ele registrou. Sempre o mesmo, para dar para aprender: as chamas em volta do jogador (até
	 * três, a até doze blocos) ficam pálidas todas juntas, por dez a quinze segundos. É miragem: só ele vê, e a
	 * chama de verdade não muda. Sem chama por perto, o mundo emudece por oito segundos.
	 */
	private static String responder(ServerLevel level, ServerPlayer p, EstadoJogador e, Motivo motivo, @Nullable Vec3 onde) {
		long tick = level.getGameTime();
		if (tick - e.afrontaRespostaTick < 40) {
			return "JA_RESPONDEU";
		}
		e.afrontaRespostaTick = tick;
		long seg = tick / 20;
		if (motivo == Motivo.FECHOU && onde != null) {
			bater(level, p, BlockPos.containing(onde));
			Familias.registrar(p, e, Familias.Familia.OBJETO, "AFRONTA_BATIDAS", seg);
			return "BATIDAS";
		}
		int palidas = empalidecer(level, p);
		if (palidas > 0) {
			Familias.registrar(p, e, Familias.Familia.LUZ, "AFRONTA_CHAMAS", seg);
			return "CHAMAS_" + palidas;
		}
		Diretor.emudecer(level, p, 8, "AFRONTA");
		Familias.registrar(p, e, Familias.Familia.SOM, "AFRONTA_SILENCIO", seg);
		return "SILENCIO";
	}

	private static int empalidecer(ServerLevel level, ServerPlayer p) {
		BlockPos centro = p.blockPosition();
		List<BlockPos> chamas = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-12, -4, -12), centro.offset(12, 5, 12))) {
			BlockState st = level.getBlockState(pos);
			if ((st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) && !Miragem.tem(p, pos)) {
				chamas.add(pos.immutable());
			}
		}
		chamas.sort((a, b) -> Double.compare(a.distSqr(centro), b.distSqr(centro)));
		int feitas = 0;
		for (BlockPos pos : chamas) {
			if (feitas >= 3) {
				break;
			}
			BlockState real = level.getBlockState(pos);
			BlockState palida = real.is(Blocks.WALL_TORCH)
					? Blocks.SOUL_WALL_TORCH.defaultBlockState()
							.setValue(BlockStateProperties.HORIZONTAL_FACING, real.getValue(BlockStateProperties.HORIZONTAL_FACING))
					: Blocks.SOUL_TORCH.defaultBlockState();
			// A duração sai do lugar da tocha, não de um sorteio: isto é apresentação.
			int duracao = 20 * (10 + Math.floorMod(pos.hashCode(), 6));
			if (Miragem.mostrar(level, p, pos, palida, duracao, 0, MIRAGEM)) {
				feitas++;
			}
		}
		return feitas;
	}

	/**
	 * Ele fechou a passagem com um bloco na frente dele: duas batidas do outro lado, só para o alvo, e o bloco
	 * racha (o desenho da rachadura do jogo, mandado só para ele; o bloco de verdade não muda). O clique chega
	 * antes de o bloco existir, então a rachadura espera uns ticks por ele.
	 */
	private static void bater(ServerLevel level, ServerPlayer p, BlockPos bloco) {
		double x = bloco.getX() + 0.5;
		double y = bloco.getY() + 0.5;
		double z = bloco.getZ() + 0.5;
		for (int atraso : new int[] {6, 16}) {
			Diretor.agendar(level, atraso, () -> {
				if (!p.isRemoved() && p.level() == level) {
					ModSons.tocarPara(p, x, y, z, ModSons.Som.PANCADA, 0.9F, 0.85F);
				}
			});
		}
		int id = -5000 - p.getId();
		Diretor.agendar(level, 8, () -> {
			if (!p.isRemoved() && p.level() == level && !level.getBlockState(bloco).isAir()) {
				p.connection.send(new ClientboundBlockDestructionPacket(id, bloco, 4));
			}
		});
		Diretor.agendar(level, 18, () -> {
			if (!p.isRemoved() && p.level() == level && !level.getBlockState(bloco).isAir()) {
				p.connection.send(new ClientboundBlockDestructionPacket(id, bloco, 7));
			}
		});
		Diretor.agendar(level, 20 * 6, () -> {
			if (!p.isRemoved() && !p.hasDisconnected()) {
				p.connection.send(new ClientboundBlockDestructionPacket(id, bloco, -1));
			}
		});
	}

	// ===== Os gatilhos que não têm outro dono =====

	/**
	 * O jogador pôs um bloco. Se foi entre ele e a criatura, com ela a até dez blocos, fechou a passagem na cara
	 * dela. Uma vez por aparição.
	 */
	static void aoColocarBloco(ServerPlayer p, EstadoJogador e, BlockPos posto) {
		HospedeEntity h = e.criatura;
		if (h == null || h.isRemoved() || h.ehTeste() || h.getIdManifestacao().equals(e.afrontaFechouEm)) {
			return;
		}
		Vec3 ele = h.position().add(0, 1.0, 0);
		Vec3 jogador = p.getEyePosition();
		Vec3 eixo = jogador.subtract(ele);
		double comprimento = eixo.length();
		if (comprimento > 10.0 || comprimento < 1.5) {
			return;
		}
		Vec3 bloco = Vec3.atCenterOf(posto);
		double aoLongo = bloco.subtract(ele).dot(eixo) / (comprimento * comprimento);
		if (aoLongo < 0.1 || aoLongo > 0.95) {
			return;
		}
		Vec3 naLinha = ele.add(eixo.scale(aoLongo));
		if (bloco.distanceToSqr(naLinha) > 1.6 * 1.6) {
			return;
		}
		e.afrontaFechouEm = h.getIdManifestacao();
		anotar(p, e, Motivo.FECHOU, bloco);
	}

	/** Só para os testes: põe o número num valor. */
	public static void definirParaTeste(ServerPlayer p, int valor) {
		Memoria m = Memoria.de(p);
		m.set(VALOR, valor);
		m.salvar();
	}

	/** Só para os testes: o número de agora. */
	public static int valorParaTeste(ServerPlayer p) {
		return Memoria.de(p).get(VALOR);
	}

	/** Só para os testes: ele vem agora, como viria na primeira noite em casa. Devolve "JANELA", "BATIDA" ou "NADA". */
	public static String visitarParaTeste(ServerPlayer p) {
		ServerLevel level = p.level();
		Memoria m = Memoria.de(p);
		EstadoJogador e = Diretor.estadoParaTeste(p);
		long tick = level.getGameTime();
		int antes = m.get(VISITAS);
		boolean veio = visitarAgora(level, p, m, e, diaDaNoite(level), tick / 20, tick, p.getRandom());
		m.salvar();
		if (!veio) {
			return "NADA";
		}
		return e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA ? "JANELA" : m.get(VISITAS) > antes ? "BATIDA" : "NADA";
	}
}
