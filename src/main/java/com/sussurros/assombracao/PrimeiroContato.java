package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModItems;

/**
 * O primeiro contato (0.9.0-alpha14): a primeira vez que o jogador o vê é garantida por regra, não por sorteio.
 *
 * Até a alpha13 a criatura só existia por sorteio, atrás de fase, de escuro e do portão de vulnerabilidade. Em
 * 2h06 de jogo de verdade o Diretor a fez aparecer três vezes, a primeira aos 69 minutos. Os itens chegavam
 * antes da ameaça que eles respondem.
 *
 * A regra, por jogador:
 *   - de 15 a 25 minutos de jogo, na primeira vez em que ele estiver fora de casa com pouca luz;
 *   - passados os 25 minutos, acontece assim mesmo, de dia e mais longe (prazo final);
 *   - o aviso é o da caçada (o mundo emudece, uma luz falha), e aqui ele é verdadeiro: a criatura está perto;
 *   - ele nasce fora da tela, parado, e dois passos vindos dele dizem de que lado está;
 *   - olhado por cerca de um segundo, dissolve. Não toca, não persegue, não captura;
 *   - onde ele estava ficam a primeira Cinza Pálida e um vestígio.
 *
 * Só conta quando foi visto, e "visto" tem uma definição só (HospedeEntity.contatoFoiVisto, a mesma do
 * analisador de log): olhado direto, por uns instantes, num lugar com luz. Por isso ele só nasce onde há luz
 * para enxergá-lo. Se não foi visto, o contato continua devendo e é refeito alguns minutos depois, cada vez
 * mais perto da borda da tela.
 *
 * Não passa pelo portão de vulnerabilidade nem espera saldo de atenção. A tentativa segura os outros sistemas
 * pelo respiro, mas só custa atenção quando é vista; só a primeira traz o aviso inteiro, as seguintes ficam
 * com os passos; e, fora a primeira do prazo final, nenhuma acontece com o Diretor recuando. Na alpha14 cada tentativa
 * custava e avisava, vista ou não: na sessão sintética, quatro tentativas deram 25 minutos de avisos sem
 * nenhum evento do Diretor (pesquisa/2026-10-10-revisao-da-alpha14.md, achados 2.1 e 2.2).
 *
 * As pesquisas que sustentam isto estão em pesquisa/2026-10-09-analise-de-design.md (seções 6.1 e 10) e em
 * pesquisa/2026-10-10-primeiro-encontro-e-ferramentas.md.
 */
public final class PrimeiroContato {
	/** Segundos de jogo, no mundo normal, contados só enquanto o contato não aconteceu. */
	static final String RELOGIO = "contato_relogio";
	/** 1: o jogador já o viu. A partir daí as fontes de ferramenta e o resto do repertório ficam liberados. */
	static final String FEITO = "contato_feito";
	static final String TENTATIVAS = "contato_tentativas";

	/** A janela: abre aos 15 minutos de jogo e fecha aos 25. Depois do fim, o contato acontece na forma que couber. */
	static final int JANELA_ABRE = 15 * 60;
	static final int JANELA_FECHA = 25 * 60;
	/** Luz efetiva a partir da qual "pouca luz" deixa de valer: entardecer, sombra de floresta, caverna. */
	private static final int POUCA_LUZ = 7;
	/** Quanto ele fica esperando ser visto. */
	private static final int VIDA_TICKS = 20 * 40;
	/**
	 * Depois de uma tentativa que ninguém viu, espera de quatro a seis minutos antes da próxima. A sessão
	 * sintética mostrou seis em dezesseis minutos com a espera mais curta.
	 */
	private static final int ESPERA_MIN = 240;
	private static final int ESPERA_SORTEIO = 121;

	private PrimeiroContato() {
	}

	public static boolean feito(Memoria m) {
		return m.get(FEITO) == 1;
	}

	/**
	 * O que entrega ferramenta (os lugares, o baralho) e o que mostra a criatura antes da fase 3 espera o
	 * primeiro contato. Da fase 3 em diante nada mais espera: se ele ainda não foi visto, as aparições normais
	 * já fazem esse papel.
	 */
	static boolean liberado(Memoria m) {
		return feito(m) || m.get(Memoria.FASE) >= 3;
	}

	/** Uma vez por segundo, dentro do tick do Diretor (recebe a Memoria do tick). */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int luz, boolean noite,
			boolean calma, long seg, long tick, RandomSource rnd) {
		if (feito(m)) {
			return;
		}
		// A criatura some no tick dela, fora daqui. O "foi visto" chega por este campo e é gravado na Memoria
		// do tick, que é a que vale (ver "Memoria dentro do tick" no CLAUDE.md).
		if (e.contatoVisto) {
			m.set(FEITO, 1);
			return;
		}
		// Mundos de antes desta versão, ou um vulto que apareceu por outro caminho: quem já o viu não precisa.
		if (m.get(Memoria.VEZES_VISTO) > 0 || m.get(Memoria.CINZAS_GERADAS) > 0) {
			m.set(FEITO, 1);
			Depuracao.log(p, seg, "CONTATO dispensado: ele já tinha sido visto");
			return;
		}
		m.add(RELOGIO, 1);
		int relogio = m.get(RELOGIO);
		if (relogio < JANELA_ABRE || seg < e.contatoProximaTentativa || e.forcando) {
			return;
		}
		boolean criaturaPresente = e.criatura != null && !e.criatura.isRemoved();
		if (criaturaPresente || EstruturasSussurros.temCenaAtiva(e) || e.estado == EstadoDiretor.AMEACANDO
				|| tick < e.veuAte || calma || Diretor.bloqueado(p, e, tick)) {
			return;
		}
		boolean prazo = relogio >= JANELA_FECHA;
		// Com o Diretor recuando (a trégua depois de um pico), o contato também espera. Só a primeira tentativa
		// do prazo final passa por cima: é a garantia. Repetição nenhuma passa.
		if (e.estado == EstadoDiretor.RECUANDO && !(prazo && m.get(TENTATIVAS) == 0)) {
			return;
		}
		boolean poucaLuz = luz <= POUCA_LUZ || noite || entardecer(level);
		boolean foraDeCasa = e.contexto != ContextoMundo.Tipo.CASA;
		if (!prazo && !(poucaLuz && foraDeCasa)) {
			return;
		}
		if (tentar(level, p, m, e, relogio, prazo, luz, seg, tick, rnd)) {
			e.contatoProximaTentativa = seg + ESPERA_MIN + rnd.nextInt(ESPERA_SORTEIO);
		} else {
			// Sem lugar agora (dentro de casa, num buraco, de frente para um paredão): tenta de novo em instantes.
			e.contatoProximaTentativa = seg + 5;
		}
	}

	/** O fim da tarde já conta como pouca luz: o sol baixo, antes de o jogo chamar de noite. */
	private static boolean entardecer(ServerLevel level) {
		long hora = Math.floorMod(level.getDefaultClockTime(), 24000L);
		return hora >= 11500 && hora < 13000;
	}

	private static boolean tentar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int relogio,
			boolean prazo, int luz, long seg, long tick, RandomSource rnd) {
		// De dia, a céu aberto e sem nada na frente, de perto ele parece só um boneco parado (ver
		// Diretor.expostoDemais): a forma diurna é a mesma, mais longe.
		boolean diaAberto = !Diretor.ehNoite(level) && level.canSeeSky(p.blockPosition().above());
		double distMin = diaAberto ? 26 : 18;
		double distMax = diaAberto ? 36 : 26;
		PedidoManifestacao pedido = PedidoManifestacao.doDiretor(Evento.PRESENCA);
		// De lado e para trás, nunca na tela; e só num lugar onde dê para enxergá-lo ao virar. A cada tentativa
		// que ninguém viu, ele nasce mais perto da borda da tela: da terceira em diante basta virar um pouco.
		int feitas = m.get(TENTATIVAS);
		double angMin = feitas == 0 ? 100 : feitas == 1 ? 80 : 62;
		double angMax = feitas == 0 ? 165 : feitas == 1 ? 140 : 110;
		// Só onde há luz para enxergá-lo: no breu ele estaria ali e ninguém veria.
		if (!Diretor.invocarOndeHaLuz(level, p, e, HospedeEntity.Modo.OBSERVAR, angMin, angMax, distMin, distMax, VIDA_TICKS,
				10.0, pedido)) {
			return false;
		}
		HospedeEntity h = e.criatura;
		if (h == null) {
			return false;
		}
		h.marcarContato();
		m.add(TENTATIVAS, 1);
		// O aviso da caçada, e aqui ele diz a verdade: há alguém perto. Só na primeira tentativa: aviso repetido
		// sem nada à vista é o sinal sem referente que o contato veio tirar. As seguintes ficam com os passos.
		boolean aviso = feitas == 0;
		if (aviso) {
			Diretor.prenunciar(level, p, e, tick, rnd, true, "contato");
			// O aviso inteiro é uma saída que o jogador percebe: o piso do Diretor conta a partir dele. Os passos
			// das repetições não zeram esse relógio (zeravam, e o piso nunca chegava).
			e.ultimoEventoSeg = seg;
		}
		// Nascer fora da tela é o jeito mais fácil de ninguém ver. Dois passos vindos dele dizem o lado;
		// se ainda não foi visto, mais dois, uma vez.
		passos(level, p, h, 44);
		passos(level, p, h, 260);
		// Segura os outros sistemas enquanto ele espera ser visto, sem gastar: o custo vem em aoSumir, se foi visto.
		Atencao.respirar(e, seg);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CONTATO tentativa=%d relogio=%ds prazo=%s luz=%d dia=%s dist=%.1f aviso=%s manifestacao=%s",
				m.get(TENTATIVAS), relogio, prazo ? "sim" : "nao", luz, diaAberto ? "sim" : "nao",
				Math.sqrt(h.distanceToSqr(p)), aviso ? "sim" : "nao", h.getIdManifestacao()));
		return true;
	}

	/** Dois passos no chão em que ele está. São do corpo dele: como os outros sons do corpo, quem está perto ouve. */
	private static void passos(ServerLevel level, ServerPlayer p, HospedeEntity h, int atraso) {
		for (int i = 0; i < 2; i++) {
			Diretor.agendar(level, atraso + i * 9, () -> {
				if (h.isRemoved() || h.isSumindo() || h.jaFoiEncarado() || p.isRemoved()) {
					return;
				}
				SoundEvent passo = level.getBlockState(h.blockPosition().below()).getSoundType().getStepSound();
				level.playSound(null, h.getX(), h.getY(), h.getZ(), passo, SoundSource.HOSTILE,
						Diretor.volumePara(p, h.getX(), h.getY(), h.getZ(), 1.0F), 0.6F);
			});
		}
	}

	/**
	 * A criatura do contato sumiu. Chamado do tick dela (fora do tick do Diretor), por isso abre a sua Memoria.
	 * Se foi vista, o contato está feito e ela deixa matéria; se não, continua devendo.
	 */
	static void aoSumir(ServerPlayer p, HospedeEntity h, String motivo) {
		ServerLevel level = p.level();
		long seg = level.getGameTime() / 20;
		// "Visto" é ter estado na mira dele por uns instantes, com luz (HospedeEntity.contatoFoiVisto). Ficar no
		// canto da tela até dissolver não conta, nem a mira que passou por ele numa virada de câmera: na sessão
		// sintética os contatos dados como vistos tinham naTela=37t e encarado=nao.
		boolean visto = h.contatoFoiVisto();
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CONTATO visto=%s motivo=%s naTela=%dt encarado=%s naMira=%dt manifestacao=%s",
				visto ? "sim" : "nao", motivo, h.getTicksNaTela(), h.jaFoiEncarado() ? "sim" : "nao", h.getTicksNaMira(),
				h.getIdManifestacao()));
		if (!visto) {
			return;
		}
		EstadoJogador e = Diretor.estadoParaTeste(p);
		e.contatoVisto = true;
		// Agora sim foi uma saída, e das fortes: custa atenção e o piso do Diretor conta a partir daqui.
		Atencao.gastar(p, e, "contato", Atencao.CENA, seg);
		e.ultimoEventoSeg = seg;
		Memoria m = Memoria.de(p);
		m.set(FEITO, 1);
		m.add(Memoria.CINZAS_GERADAS, 1);
		m.salvar();
		Vestigios.de(p).registrar(h.blockPosition(), Vestigios.Tipo.DESAPARECIMENTO, seg);
		// A primeira cinza não some: é dela que nasce a primeira ferramenta, e ele pode demorar a ir até lá.
		ItemEntity item = new ItemEntity(level, h.getX(), h.getY() + 0.15, h.getZ(), new ItemStack(ModItems.CINZA_PALIDA));
		item.setDeltaMovement(0, 0, 0);
		item.setUnlimitedLifetime();
		level.addFreshEntity(item);
		Depuracao.log(p, seg, "VESTIGIO CINZA motivo=CONTATO manifestacao=" + h.getIdManifestacao()
				+ " pos=" + Diretor.pos(h.getX(), h.getY(), h.getZ()));
	}

	// ----- Para os testes -----

	/** Põe o relógio do contato num ponto da janela, para um teste não esperar quinze minutos. */
	public static void relogioParaTeste(ServerPlayer p, int segundos) {
		Memoria m = Memoria.de(p);
		m.set(RELOGIO, segundos);
		m.salvar();
		Diretor.estadoParaTeste(p).contatoProximaTentativa = 0;
	}

	public static boolean feitoParaTeste(ServerPlayer p) {
		return feito(Memoria.de(p));
	}

	/** O aviso da caçada (o mundo emudece, uma luz falha) está valendo para este jogador agora? */
	public static boolean avisoAtivoParaTeste(ServerPlayer p) {
		return Diretor.estadoParaTeste(p).cacaAvisoAte > p.level().getGameTime();
	}

	/** Libera já a próxima tentativa, sem mexer no relógio, para um teste não esperar os minutos entre uma e outra. */
	public static void liberarTentativaParaTeste(ServerPlayer p) {
		Diretor.estadoParaTeste(p).contatoProximaTentativa = 0;
	}

	/** Põe o Diretor recuando (a trégua depois de um pico) por um bom tempo, ou o tira de lá. */
	public static void recuarParaTeste(ServerPlayer p, boolean recuando) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		e.estado = recuando ? EstadoDiretor.RECUANDO : EstadoDiretor.CALMO;
		e.estadoDesde = p.level().getGameTime() / 20;
		e.duracaoEstado = 600;
	}

	/** Dá o contato por feito, para os testes que tratam do que vem depois dele. */
	public static void darPorFeitoParaTeste(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		m.set(FEITO, 1);
		m.salvar();
	}
}
