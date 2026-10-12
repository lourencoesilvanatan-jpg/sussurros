package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.entidade.HospedeEntity;

/**
 * A cota de corpo (0.9.0-alpha20): da fase 3 em diante, o jogador vê a criatura de verdade de tempos em tempos.
 *
 * O primeiro contato garante que ele seja visto UMA vez. Depois disso nada garantia mais nada: para o Diretor,
 * "tentei mostrar" e "ele viu" contavam igual. Em 11/10/2026, em noventa minutos nas fases 3 e 4, o Diretor quis
 * mostrá-lo oito vezes e conseguiu três, e quem jogava com o dono perguntou "cadê o bicho?".
 *
 * A regra, por jogador, depois do primeiro contato e a partir da fase 3:
 *  - um relógio conta os segundos de jogo desde a última aparição VISTA. "Visto" tem a definição de sempre
 *    (HospedeEntity.foiVistoDeVerdade: na mira dele por uns instantes, com luz, ou dissolvendo debaixo da mira).
 *    Uma caçada em que ele esteve na tela também zera: ali ninguém pergunta onde ele está;
 *  - aos 20 minutos a cota fica devendo. A presença comum seguinte, que o Diretor escolher por conta própria,
 *    usa a colocação mais fácil de ver (ver colocar) e vem sempre com os dois passos que dizem o lado;
 *  - aos 30 minutos ela não espera mais o Diretor: tenta sozinha, quando o momento deixa (ninguém presente,
 *    nenhuma cena, fora da vela, fora de perigo, sem trégua, sem o Diretor ameaçando nem recuando);
 *  - a tentativa que ninguém viu não gasta atenção (só segura os outros pelo respiro), e a seguinte espera de
 *    quatro a seis minutos, mais dois a cada tentativa, até dez a doze.
 *
 * O que não muda: ele continua sendo relance (some ao ser olhado), nunca nasce na tela, e de perto só em lugar
 * fechado. A cota não põe ninguém DENTRO da casa do jogador: em casa ela só usa o lugar com luz, que fica do
 * lado de fora ou longe.
 *
 * O risco conhecido é a familiaridade: a colocação fácil é justamente a que mais mostra, e o que aparece demais
 * deixa de assustar. Por isso a cota só existe enquanto ele NÃO foi visto, os números são ponto de partida (sem
 * sessão de gente), e o analisador soma a exposição por hora ao lado das tentativas dela.
 */
public final class Cota {
	/** Segundos de jogo desde a última aparição vista. Só anda depois do primeiro contato, da fase 3 em diante. */
	static final String RELOGIO = "cota_relogio";
	/** Tentativas (do Diretor ou da própria cota) desde a última aparição vista. */
	static final String TENTATIVAS = "cota_tentativas";

	/** Aos 20 minutos sem ser visto a cota fica devendo; aos 30, tenta por conta própria. */
	static final int JANELA_ABRE = 20 * 60;
	static final int JANELA_FECHA = 30 * 60;
	/** Quanto ele fica esperando ser visto, quando é a cota que o põe (o do primeiro contato). */
	private static final int VIDA_TICKS = 20 * 40;
	private static final int ESPERA_MIN = 240;
	private static final int ESPERA_POR_TENTATIVA = 120;
	private static final int ESPERA_SORTEIO = 121;

	private Cota() {
	}

	/** A cota vale para este jogador? Depois do primeiro contato (que é quem garante a primeira vez) e da fase 3. */
	static boolean ativa(Memoria m, int fase) {
		return fase >= 3 && PrimeiroContato.feito(m);
	}

	/** Ele não é visto há tempo bastante para a presença seguinte usar a colocação fácil. */
	static boolean devendo(Memoria m) {
		return ativa(m, m.get(Memoria.FASE)) && m.get(RELOGIO) >= JANELA_ABRE;
	}

	/**
	 * Uma vez por segundo, no tick do Diretor, com a Memoria do tick: o relógio anda, ou zera se ele foi visto.
	 * A criatura some no tick dela, fora daqui; o "foi visto" chega por um campo do estado (ver "Memoria dentro
	 * do tick" no CLAUDE.md).
	 */
	static void contar(ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg) {
		if (e.cotaVisto) {
			e.cotaVisto = false;
			if (m.get(RELOGIO) >= JANELA_ABRE) {
				Depuracao.log(p, seg, "COTA paga relogio=" + m.get(RELOGIO) + "s tentativas=" + m.get(TENTATIVAS));
			}
			m.set(RELOGIO, 0);
			m.set(TENTATIVAS, 0);
			return;
		}
		if (!ativa(m, fase)) {
			return;
		}
		m.add(RELOGIO, 1);
		if (m.get(RELOGIO) == JANELA_ABRE) {
			Depuracao.log(p, seg, "COTA devendo relogio=" + JANELA_ABRE + "s");
		}
	}

	/** A tentativa da própria cota, quando o Diretor não o mostrou a tempo. Chamada só fora de trégua. */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, boolean calma,
			long seg, long tick, RandomSource rnd) {
		if (!ativa(m, fase) || m.get(RELOGIO) < JANELA_FECHA || seg < e.cotaProximaTentativa || e.forcando) {
			return;
		}
		boolean criaturaPresente = e.criatura != null && !e.criatura.isRemoved();
		if (criaturaPresente || EstruturasSussurros.temCenaAtiva(e) || e.estado == EstadoDiretor.AMEACANDO
				|| e.estado == EstadoDiretor.RECUANDO || tick < e.veuAte || calma || Diretor.bloqueado(p, e, tick)
				|| seg < e.atencaoLivreEm) {
			return;
		}
		PedidoManifestacao pedido = PedidoManifestacao.doDiretor(Evento.PRESENCA);
		if (!colocar(level, p, m, e, VIDA_TICKS, pedido, "REGRA", seg, rnd)) {
			// Sem lugar agora: tenta de novo em instantes, e a cada minuto o log diz o que faltou.
			e.cotaProximaTentativa = seg + 5;
			if (e.falhasDeLugar.vezes() >= 12) {
				Diretor.logarFaltaDeLugar(p, e, seg);
			}
			return;
		}
		// Segura os outros sistemas enquanto ele espera ser visto, sem gastar: o custo vem em aoSumir, se foi visto.
		Atencao.respirar(e, seg);
	}

	/**
	 * A colocação mais fácil de ver. Fora de casa, primeiro de perto, num lugar fechado e com luz (de 8 a 14
	 * blocos: a caverna, a mina). Senão, num lugar com luz que ele enxerga ao virar, perto da borda da tela, à
	 * distância do primeiro contato. Devolve false se nada disso coube agora.
	 *
	 * Quem chama é a presença comum do Diretor (origem DIRETOR) ou a própria cota (origem REGRA). Nos dois casos
	 * conta uma tentativa e marca a espera da cota, para ela não vir por cima de uma aparição recente.
	 */
	static boolean colocar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int duracao,
			PedidoManifestacao pedido, String origem, long seg, RandomSource rnd) {
		int feitas = m.get(TENTATIVAS);
		boolean ok = e.contexto != ContextoMundo.Tipo.CASA
				&& Diretor.invocarDePerto(level, p, e, HospedeEntity.Modo.OBSERVAR, duracao, HospedeEntity.DIST_SUMIR_PADRAO, pedido, true);
		if (!ok) {
			// De dia, a céu aberto e sem nada na frente, de perto ele parece só um boneco (ver Diretor.expostoDemais).
			boolean diaAberto = !Diretor.ehNoite(level) && level.canSeeSky(p.blockPosition().above());
			double distMin = diaAberto ? 26 : 18;
			double distMax = diaAberto ? 36 : 26;
			// De lado, nunca na tela; da segunda tentativa em diante, mais perto da borda dela: basta virar um pouco.
			double angMin = feitas == 0 ? 80 : 62;
			double angMax = feitas == 0 ? 140 : 110;
			ok = Diretor.invocarOndeHaLuz(level, p, e, HospedeEntity.Modo.OBSERVAR, angMin, angMax, distMin, distMax, duracao,
					10.0, pedido);
		}
		HospedeEntity h = e.criatura;
		if (!ok || h == null) {
			return false;
		}
		h.marcarCota("REGRA".equals(origem));
		m.add(TENTATIVAS, 1);
		e.cotaProximaTentativa = seg + ESPERA_MIN + ESPERA_POR_TENTATIVA * Math.min(3, feitas) + rnd.nextInt(ESPERA_SORTEIO);
		// Nascer fora da tela é o jeito mais fácil de ninguém ver. Dois passos vindos dele dizem o lado; se ainda
		// não foi visto, mais dois, uma vez. (É o anúncio do primeiro contato, sem o aviso da caçada.)
		PrimeiroContato.passos(level, p, h, 44);
		PrimeiroContato.passos(level, p, h, 260);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"COTA tentativa=%d origem=%s relogio=%ds dist=%.1f dePerto=%s manifestacao=%s",
				m.get(TENTATIVAS), origem, m.get(RELOGIO), Math.sqrt(h.distanceToSqr(p)), h.ehDePerto() ? "sim" : "nao",
				h.getIdManifestacao()));
		return true;
	}

	/**
	 * Uma criatura deste jogador sumiu. Chamado do tick dela (fora do tick do Diretor): por isso só marca o
	 * estado, e quem zera o relógio é contar(), com a Memoria do tick.
	 */
	static void aoSumir(ServerPlayer p, EstadoJogador e, HospedeEntity h, String motivo) {
		if (h.ehTeste()) {
			return; // criatura de comando não conta para nada
		}
		// Na caçada ele é visto correndo, quase sempre no escuro: ali basta ter estado um segundo na tela.
		boolean visto = h.foiVistoDeVerdade() || (h.getModo() == HospedeEntity.Modo.CACAR && h.getTicksNaTela() >= 20);
		long seg = p.level().getGameTime() / 20;
		if (h.ehCota()) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "COTA visto=%s motivo=%s naTela=%dt naMira=%dt manifestacao=%s",
					visto ? "sim" : "nao", motivo, h.getTicksNaTela(), h.getTicksNaMira(), h.getIdManifestacao()));
			if (visto && h.ehCotaDaRegra()) {
				// A tentativa da própria cota só custa atenção quando foi vista (como o primeiro contato).
				Atencao.gastar(p, e, "cota", Atencao.custo(Evento.PRESENCA.intensidade), seg);
				e.ultimoEventoSeg = seg;
			}
		}
		if (visto) {
			e.cotaVisto = true;
		}
	}

	// ----- Para os testes -----

	/** Põe o relógio da cota num ponto, para um teste não esperar vinte minutos, e libera a tentativa. */
	public static void relogioParaTeste(ServerPlayer p, int segundos) {
		Memoria m = Memoria.de(p);
		m.set(RELOGIO, segundos);
		m.salvar();
		Diretor.estadoParaTeste(p).cotaProximaTentativa = 0;
	}

	public static int relogioDeTeste(ServerPlayer p) {
		return Memoria.de(p).get(RELOGIO);
	}

	public static int tentativasDeTeste(ServerPlayer p) {
		return Memoria.de(p).get(TENTATIVAS);
	}
}
