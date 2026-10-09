package com.sussurros.entidade;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.ApoioCaca;
import com.sussurros.assombracao.Depuracao;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Percepcao;
import com.sussurros.bloco.CinzaEspalhadaBlock;

/**
 * Uma caçada, do aviso ao desfecho (0.9).
 *
 * A regra cabe numa frase: ele só anda quando você não está olhando, ele ouve o que você faz, e ele não entra
 * na luz da vela. O resto é consequência dela e de três cuidados:
 *
 *  1. Ele sempre tem resposta, e a resposta sempre avisa. Pilar, buraco e muralha não o seguram, mas ele só
 *     atravessa depois de segundos de sinal claro num bloco específico, e sair de perto do bloco resolve.
 *  2. Ele não mata e não estraga construção. Abre porta de madeira e apaga luz (por miragem; ela volta).
 *  3. A caçada tem teto de tempo. Não existe empate: ou ele pega, ou vai embora, e quando vai embora de
 *     verdade o som do mundo volta. Esse é o sinal honesto de fim.
 *
 * As referências e os números de partida estão em pesquisa/2026-10-08-cacada.md.
 */
final class Cacada {
	enum Estagio {
		AVISO,       // ele nasceu e não se mexe: o mundo emudeceu, uma luz falhou
		PERSEGUE,    // anda quando não é visto; procura quando perde o rastro
		ESPERA_VELA, // o alvo está na zona da vela: espera na borda, por tempo limitado
		FINGE,       // fingiu que desistiu: calado, fora de vista, prestes a voltar
		ATRAVESSA    // sem caminho até o alvo: avisa num bloco e passa por ele
	}

	/** Relógio de contato: só anda com ele fora da tela e sabendo onde o alvo está. */
	private static final int TETO_CONTATO = 20 * 45;
	private static final int TETO_BUSCA = 20 * 40;
	private static final int TETO_TOTAL = 20 * 120;
	private static final double ALCANCE_TOQUE = 2.4;
	/** Até onde ele vê o jogador, sem nada no meio: de pé e agachado. Mais longe que isso, ele depende do ouvido. */
	private static final double ALCANCE_VISAO = 8.0;
	private static final double ALCANCE_VISAO_AGACHADO = 3.5;

	/**
	 * Velocidade de um mob no chão, em blocos por segundo: FATOR x (atributo x modificador)^2.
	 * O quadrado existe porque o jogo usa a velocidade do mob duas vezes (como força e como direção).
	 * O fator foi medido no jogo (teste "quemFicaDeCostasELevado"): a conta de cabeça dava 58,5 e ele
	 * andava a 3,6 blocos/s quando pedíamos 4,9.
	 */
	static final double FATOR_VELOCIDADE = 43.4;
	/** O jogador anda a 4,3 blocos/s, corre a 5,6 e corre pulando a 7,1. */
	private static final double VELOCIDADE_INICIAL = 4.9;
	private static final double VELOCIDADE_MAXIMA = 6.3;

	private final HospedeEntity h;
	private final HospedeBusca busca;
	/** Sorteios da caçada. Não usa o gerador do mundo, para não mudar os sorteios do Diretor. */
	private final RandomSource sorte = RandomSource.create();
	/** Primeira caçada da vida do jogador: sem atalho e sem atravessar. Ele aprende a regra antes das exceções. */
	private final boolean primeira;
	/** Quanto o Diretor quer que ele seja mais rápido que o normal (cresce cada vez que o jogador o fere). */
	private final double fatorDoDiretor;

	private Estagio estagio = Estagio.AVISO;
	private int idade;
	private int avisoRestante;
	private int contato;
	private int semContato;
	private int naTela;
	private int foraDaTela;
	private int proximoPiscar;
	private int piscarEm = -1;
	private int piscadas;
	private int recargaAtalho;
	private int atalhos;
	private int golpes;
	private int semCaminho;
	/** Só para o log: no último teste de caminho, não havia como chegar ao alvo. */
	private boolean semCaminhoAgora;
	private boolean fingiu;
	private int fingeRestante;
	private double velocidade = VELOCIDADE_INICIAL;
	private boolean encerrada;
	/** Uma Linha de Cinza o segurou: ticks parado antes de tentar de novo. */
	private int barrado;
	/** Quantas vezes a Caixa de Música o chamou nesta caçada. Cada vez ele acredita menos. */
	private int iscas;
	private long ultimaIscaTick = -1000;

	@Nullable
	private BlockPos avisoBloco;
	private int avisoAtravessar;
	@Nullable
	private BlockPos porta;
	private int portaEspera;
	private int esperaVela;
	private int esperaVelaMaxima;
	private int soprarEm = -1;
	@Nullable
	private Vec3 pontoDaBorda;

	Cacada(HospedeEntity h, HospedeBusca busca, ServerPlayer alvo, double fatorDoDiretor, boolean comAviso) {
		this.h = h;
		this.busca = busca;
		this.fatorDoDiretor = Math.max(0.9, Math.min(1.2, fatorDoDiretor));
		this.primeira = ApoioCaca.primeiraCacada(alvo) && !h.ehTeste();
		this.avisoRestante = comAviso ? 160 + this.sorte.nextInt(41) : 0;
		this.estagio = comAviso ? Estagio.AVISO : Estagio.PERSEGUE;
		this.proximoPiscar = 80 + this.sorte.nextInt(41);
		h.getNavigation().setCanOpenDoors(true);
	}

	Estagio estagio() {
		return this.estagio;
	}

	boolean encerrada() {
		return this.encerrada;
	}

	/** Chamado quando a criatura some por qualquer motivo, para o fim ser contado uma vez só. */
	void marcarEncerrada() {
		this.encerrada = true;
	}

	/** O quanto o jogador deve "sentir" a caçada agora, de 0 a 1 (ver Sentidos). */
	float sentir(ServerPlayer alvo, double dist) {
		return switch (this.estagio) {
			case AVISO -> 0.2F;
			case FINGE -> 0.0F;
			default -> dist < 12 && this.h.hasLineOfSight(alvo)
					? (float) (0.45 + 0.55 * (1.0 - dist / 12.0))
					: 0.25F;
		};
	}

	void tick(ServerLevel level, ServerPlayer alvo, boolean percebido, double distSqr) {
		this.idade++;
		double dist = Math.sqrt(distSqr);
		// Enquanto ele estiver aqui, o mundo fica mudo para o alvo. O som só volta quando ele vai embora de verdade.
		if (this.idade % 100 == 1) {
			ApoioCaca.manterSilencio(level, alvo, this.h);
		}
		if (this.idade > TETO_TOTAL) {
			this.encerrar(level, alvo, "TETO_TOTAL");
			return;
		}
		if (this.recargaAtalho > 0) {
			this.recargaAtalho--;
		}

		switch (this.estagio) {
			case AVISO -> {
				this.h.pararEOlhar();
				if (--this.avisoRestante <= 0) {
					this.mudar(level, alvo, Estagio.PERSEGUE, "FIM_DO_AVISO");
				}
			}
			case FINGE -> {
				this.h.getNavigation().stop();
				if (--this.fingeRestante <= 0) {
					this.busca.recomecar();
					this.semContato = TETO_BUSCA / 2;
					this.mudar(level, alvo, Estagio.PERSEGUE, "VOLTOU");
				}
			}
			case ATRAVESSA -> this.tickAtravessa(level, alvo);
			case ESPERA_VELA -> this.tickEsperaVela(level, alvo, percebido);
			case PERSEGUE -> this.tickPersegue(level, alvo, percebido, dist);
		}
	}

	// ===== Perseguição e busca =====

	private void tickPersegue(ServerLevel level, ServerPlayer alvo, boolean percebido, double dist) {
		if (Diretor.emZonaCalma(alvo, alvo.getX(), alvo.getY(), alvo.getZ())) {
			this.iniciarEsperaVela(level, alvo);
			return;
		}

		// A Linha de Cinza: ele não cruza uma linha que ainda segura. Cada tentativa a desgasta e o deixa
		// parado três segundos; na terceira ela rompe. Vem antes do toque: quem está atrás da linha está a salvo
		// enquanto ela durar.
		if (this.barrado > 0) {
			this.barrado--;
			this.h.pararEOlhar();
			return;
		}
		BlockPos linha = this.linhaNoCaminho(level);
		if (linha != null && CinzaEspalhadaBlock.desgastar(level, linha)) {
			this.barrado = 60;
			this.h.getNavigation().stop();
			ApoioCaca.linhaSegurou(level, alvo, this.h, linha);
			return;
		}

		// O toque exige linha de visão (nada de ser pego através da parede), mas não depende de ele estar sendo
		// visto: encarar o segura de longe; de perto, encostar é o fim.
		if (dist < ALCANCE_TOQUE && this.h.hasLineOfSight(alvo)) {
			this.capturar(level, alvo, "TOCOU");
			return;
		}

		// O avanço do piscar acontece mesmo com ele "na tela": a tela está preta nesse instante.
		if (this.piscarEm >= 0 && --this.piscarEm < 0) {
			this.avancarNoPiscar(level, alvo, dist);
			return;
		}

		if (percebido) {
			// Congela e sabe exatamente onde você está. Encarar segura, mas não vence: o relógio da caçada
			// para, e de tempos em tempos você pisca.
			this.h.pararEOlhar();
			this.busca.ouvirMovimento(level, this.h, alvo, true);
			this.foraDaTela = 0;
			if (++this.naTela >= this.proximoPiscar && this.piscarEm < 0) {
				ApoioCaca.piscar(alvo);
				this.piscarEm = 3;
				this.piscadas++;
				this.proximoPiscar = this.naTela + 60 + this.sorte.nextInt(41);
				this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s piscar n=%d dist=%.1f",
						this.h.getIdManifestacao(), this.piscadas, dist));
			}
			return;
		}
		if (++this.foraDaTela > 10) {
			this.naTela = 0;
			this.proximoPiscar = 80 + this.sorte.nextInt(41);
		}
		this.h.soltarOlhar();

		this.busca.ouvirMovimento(level, this.h, alvo, false);
		// Ele também vê. De perto e sem nada no meio, sabe onde o jogador está, parado ou não: ficar imóvel só
		// esconde quem tem uma parede entre os dois, ou quem está agachado a mais de três blocos e meio. Sem isto
		// ele chegava ao último lugar conhecido, parava a três blocos de um jogador imóvel, "não achava ninguém"
		// e ia embora (aconteceu num teste: fim por PERDEU_RASTRO a 3,7 blocos do alvo).
		boolean ve = dist <= (alvo.isCrouching() ? ALCANCE_VISAO_AGACHADO : ALCANCE_VISAO) && this.h.hasLineOfSight(alvo);
		if (ve) {
			this.busca.ver(level, this.h, alvo.position());
		}
		boolean sabe = this.busca.confianca() >= 0.6 && this.busca.idadeDoConhecimento(level.getGameTime()) <= 60;
		if (sabe) {
			this.contato++;
			this.semContato = 0;
			this.velocidade = Math.min(VELOCIDADE_MAXIMA, this.velocidade * (1.0 + 0.04 / 20.0));
		} else {
			this.semContato++;
			this.velocidade = Math.max(VELOCIDADE_INICIAL, this.velocidade * (1.0 - 0.01 / 20.0));
		}
		if (this.contato > TETO_CONTATO) {
			this.encerrar(level, alvo, "TETO_CONTATO");
			return;
		}
		if (this.semContato > TETO_BUSCA) {
			this.desistir(level, alvo, "TETO_BUSCA");
			return;
		}
		// Na luz forte ele é mais lento. É por isso que apaga as luzes por onde passa.
		boolean claro = level.getBrightness(LightLayer.BLOCK, this.h.blockPosition().above()) >= 12;
		this.busca.definirVelocidade(modificadorPara(this.velocidade * this.fatorDoDiretor * (claro ? 0.85 : 1.0)));

		if (this.tentarAtalho(level, alvo, dist, sabe)) {
			return;
		}

		if (this.porta != null) {
			this.tickPorta(level, alvo);
			return;
		}
		if (ve) {
			// Vendo o alvo, ele não procura: vai direto até ele.
			if (this.h.tickCount % 5 == 0) {
				this.h.getNavigation().moveTo(alvo, modificadorPara(this.velocidade * this.fatorDoDiretor * (claro ? 0.85 : 1.0)));
				this.procurarPorta(level);
			}
		} else if (this.h.tickCount % 5 == 0) {
			if (this.busca.tick(level, this.h, alvo)) {
				this.desistir(level, alvo, "PERDEU_RASTRO");
				return;
			}
			this.procurarPorta(level);
		}
		if (this.h.tickCount % 20 == 0) {
			this.verificarCaminho(level, alvo, dist, sabe);
		}
		if (this.h.tickCount % 30 == 0) {
			ApoioCaca.apagarLuzPerto(level, alvo, this.h.blockPosition(), 4, 20 * 100, 1);
		}
		// Os passos dele dizem uma coisa só: ele está andando, logo você não está olhando.
		if (this.h.tickCount % 7 == 0 && this.h.getNavigation().isInProgress()
				&& this.h.getDeltaMovement().horizontalDistanceSqr() > 0.0006) {
			ApoioCaca.passo(level, alvo, this.h);
		}
	}

	/** Modificador de navegação que dá a velocidade pedida, em blocos por segundo. */
	static double modificadorPara(double blocosPorSegundo) {
		return Math.sqrt(blocosPorSegundo / FATOR_VELOCIDADE) / HospedeEntity.VELOCIDADE_BASE;
	}

	/**
	 * Uma Linha de Cinza que ainda segura no caminho dele: o bloco onde ele está ou um dos dois próximos passos.
	 * Linha ao lado do caminho não o segura; só a que ele teria de pisar. Olhado a cada tick: na corrida ele
	 * anda mais de um bloco em cinco ticks e passaria por cima entre duas olhadas.
	 */
	private BlockPos linhaNoCaminho(ServerLevel level) {
		BlockPos aqui = this.h.blockPosition();
		BlockPos achada = CinzaEspalhadaBlock.linhaPerto(level, aqui, 0);
		if (achada != null) {
			return achada;
		}
		Path caminho = this.h.getNavigation().getPath();
		if (caminho == null || caminho.isDone()) {
			return null;
		}
		int proximo = caminho.getNextNodeIndex();
		for (int i = proximo; i < Math.min(caminho.getNodeCount(), proximo + 2); i++) {
			BlockPos no = caminho.getNodePos(i);
			if (no.distSqr(aqui) <= 6.25) {
				achada = CinzaEspalhadaBlock.linhaPerto(level, no, 0);
				if (achada != null) {
					return achada;
				}
			}
		}
		return null;
	}

	/**
	 * Piscar forçado: a tela do jogador fecha por uma fração de segundo e ele avança até três blocos.
	 * De perto, é o fim. De longe, é o que impede "ficar parado encarando" de ser uma vitória.
	 */
	private void avancarNoPiscar(ServerLevel level, ServerPlayer alvo, double dist) {
		double avanco = Math.min(3.0, dist - 1.6);
		if (avanco < 0.5) {
			return;
		}
		Vec3 direcao = alvo.position().subtract(this.h.position());
		direcao = new Vec3(direcao.x, 0, direcao.z);
		if (direcao.lengthSqr() < 1.0E-4) {
			return;
		}
		// Ele não salta uma Linha de Cinza no piscar: para antes dela.
		Vec3 meioPasso = direcao.normalize().scale(0.5);
		Vec3 ponto = this.h.position();
		double livre = 0;
		for (double d = 0.5; d <= avanco + 0.01; d += 0.5) {
			ponto = ponto.add(meioPasso);
			if (CinzaEspalhadaBlock.linhaPerto(level, BlockPos.containing(ponto), 0) != null) {
				break;
			}
			livre = d;
		}
		if (livre < 0.5) {
			return;
		}
		Vec3 destino = this.h.position().add(direcao.normalize().scale(livre));
		BlockPos chao = HospedeBusca.acharChao(level, destino.x, this.h.getY() - 2, destino.z);
		if (chao == null || Math.abs(chao.getY() - this.h.getY()) > 2.5
				|| Diretor.emZonaCalma(alvo, chao.getX(), chao.getY(), chao.getZ())) {
			return;
		}
		this.h.getNavigation().stop();
		this.h.snapTo(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5, this.h.getYRot(), 0.0F);
		this.h.olharPara(alvo);
	}

	/**
	 * Atalho fora de vista: três segundos sem ser visto e longe (ou sem caminho), ele reaparece a 8-12 blocos,
	 * fora da tela, de preferência à frente de para onde o jogador está indo. Nunca perto, nunca na tela.
	 */
	private boolean tentarAtalho(ServerLevel level, ServerPlayer alvo, double dist, boolean sabe) {
		if (this.primeira || this.recargaAtalho > 0 || this.foraDaTela < 60 || !sabe) {
			return false;
		}
		// Só de longe. De perto e sem caminho vale a outra regra: atravessar, com aviso.
		if (dist <= 16) {
			return false;
		}
		BlockPos lugar = this.lugarForaDaTela(level, alvo, 8, 12, true);
		if (lugar == null) {
			this.recargaAtalho = 40;
			return false;
		}
		this.h.getNavigation().stop();
		this.h.snapTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, this.h.getYRot(), 0.0F);
		this.h.olharPara(alvo);
		this.busca.ouvirAcao(level, this.h, alvo.position(), 0.9, "ATALHO");
		this.recargaAtalho = 200;
		this.atalhos++;
		if (this.sorte.nextBoolean()) {
			ApoioCaca.somDePano(alvo, this.h);
		}
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s atalho n=%d de=%.0f para=%.0f",
				this.h.getIdManifestacao(), this.atalhos, dist, Math.sqrt(this.h.distanceToSqr(alvo))));
		return true;
	}

	/**
	 * Um chão livre entre distMin e distMax do jogador, fora da tela dele e fora da vela.
	 * aFrente: prefere os lados da direção em que ele está andando ("ele estava me esperando").
	 */
	@Nullable
	private BlockPos lugarForaDaTela(ServerLevel level, ServerPlayer alvo, double distMin, double distMax, boolean aFrente) {
		Vec3 movimento = alvo.getKnownMovement();
		boolean andando = movimento.horizontalDistanceSqr() > 0.004;
		Vec3 olhar = alvo.getViewVector(1.0F);
		double base = andando && aFrente
				? Math.atan2(movimento.z, movimento.x)
				: Math.atan2(-olhar.z, -olhar.x); // parado: atrás dele
		double cone = Percepcao.coneSeguro(alvo);
		BlockPos melhor = null;
		double melhorNota = -1;
		for (int i = 0; i < 16; i++) {
			double abertura = andando && aFrente ? Math.toRadians(35 + this.sorte.nextDouble() * 75) : this.sorte.nextDouble() * Math.toRadians(70);
			double ang = base + (this.sorte.nextBoolean() ? abertura : -abertura);
			double r = distMin + this.sorte.nextDouble() * (distMax - distMin);
			BlockPos chao = HospedeBusca.acharChao(level, alvo.getX() + Math.cos(ang) * r, alvo.getY() + 2, alvo.getZ() + Math.sin(ang) * r);
			if (chao == null || Diretor.emZonaCalma(alvo, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			Vec3 ponto = new Vec3(chao.getX() + 0.5, chao.getY() + 1.2, chao.getZ() + 0.5);
			double d = ponto.distanceTo(alvo.getEyePosition());
			if (d < distMin || d > distMax + 2) {
				continue;
			}
			if (olhar.dot(ponto.subtract(alvo.getEyePosition()).normalize()) > cone) {
				continue;
			}
			double nota = this.sorte.nextDouble() + (Math.abs(chao.getY() - alvo.getY()) < 2 ? 1.0 : 0.0);
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
			}
		}
		return melhor;
	}

	/**
	 * A Caixa de Música está tocando: ele vai até a música em vez de ir até o jogador. Funciona bem na
	 * primeira vez da caçada, mal na segunda e quase nada depois. Se ele acabou de ver o jogador, não adianta.
	 */
	void iscar(ServerLevel level, Vec3 onde) {
		long tick = level.getGameTime();
		if (tick - this.ultimaIscaTick > 200) {
			this.iscas++;
		}
		this.ultimaIscaTick = tick;
		if (this.estagio != Estagio.PERSEGUE || this.naTela > 0) {
			return;
		}
		double certeza = this.iscas <= 1 ? 0.7 : this.iscas == 2 ? 0.45 : 0.2;
		if (this.busca.confianca() > certeza + 0.25 && this.busca.idadeDoConhecimento(tick) < 40) {
			return;
		}
		this.busca.ouvirAcao(level, this.h, onde, certeza, "CAIXA");
	}

	// ===== Portas =====

	/** Há uma porta de madeira fechada logo adiante no caminho? Então ele para atrás dela e mexe na maçaneta. */
	private void procurarPorta(ServerLevel level) {
		Path caminho = this.h.getNavigation().getPath();
		if (caminho == null) {
			return;
		}
		int proximo = caminho.getNextNodeIndex();
		for (int i = proximo; i < Math.min(caminho.getNodeCount(), proximo + 2); i++) {
			BlockPos pos = caminho.getNodePos(i);
			BlockState estado = level.getBlockState(pos);
			if (DoorBlock.isWoodenDoor(estado) && estado.getBlock() instanceof DoorBlock bloco && !bloco.isOpen(estado)
					&& this.h.blockPosition().distSqr(pos) <= 6.25) {
				this.porta = pos.immutable();
				this.portaEspera = 30 + this.sorte.nextInt(31); // de 1,5 a 3 s
				return;
			}
		}
	}

	private void tickPorta(ServerLevel level, ServerPlayer alvo) {
		BlockPos pos = this.porta;
		BlockState estado = level.getBlockState(pos);
		if (!(estado.getBlock() instanceof DoorBlock bloco) || bloco.isOpen(estado)) {
			this.porta = null;
			return;
		}
		this.h.getNavigation().stop();
		if (this.portaEspera % 10 == 0) {
			ApoioCaca.macaneta(level, pos);
		}
		if (--this.portaEspera <= 0) {
			bloco.setOpen(this.h, level, estado, pos, true);
			this.porta = null;
			this.log(level, alvo, "CACA id=" + this.h.getIdManifestacao() + " abriu porta pos=" + pos.toShortString());
		}
	}

	// ===== "Sem caminho não é sem saída" =====

	private void verificarCaminho(ServerLevel level, ServerPlayer alvo, double dist, boolean sabe) {
		this.semCaminhoAgora = false;
		// "Perto" aqui é no chão: quem sobe dez, vinte blocos numa torre continua perto. Altura não é distância.
		double dx = alvo.getX() - this.h.getX();
		double dz = alvo.getZ() - this.h.getZ();
		double noChao = Math.sqrt(dx * dx + dz * dz);
		if (noChao > 10 || Math.abs(alvo.getY() - this.h.getY()) > 32) {
			this.semCaminho = 0;
			return;
		}
		if (!sabe) {
			// Sem notícia recente o relógio não anda, mas também não volta: quem só se esconde em cima do
			// pilar por uns segundos não zera a conta. Muito tempo sem notícia, aí sim, ele perdeu o alvo.
			if (this.busca.idadeDoConhecimento(level.getGameTime()) > 200) {
				this.semCaminho = 0;
			}
			return;
		}
		Path caminho = this.h.getNavigation().createPath(alvo, 0);
		boolean chega = caminho != null && (caminho.canReach()
				|| (caminho.getEndNode() != null && caminho.getEndNode().asBlockPos().distSqr(alvo.blockPosition()) <= 2.25));
		if (chega) {
			this.semCaminho = 0;
			return;
		}
		this.semCaminhoAgora = true;
		this.semCaminho += 20;
		if (this.primeira || noChao > 8) {
			return;
		}
		// A cada vez que o jogador usa o mesmo truque, ele espera menos (de 10 s até 4 s).
		int usos = ApoioCaca.vezesAtravessou(alvo);
		if (this.semCaminho >= Math.max(80, 200 - 40 * usos)) {
			this.iniciarAtravessar(level, alvo, usos);
		}
	}

	private void iniciarAtravessar(ServerLevel level, ServerPlayer alvo, int usos) {
		BlockPos pes = alvo.blockPosition();
		BlockPos escolhido = null;
		if (alvo.getY() - this.h.getY() > 1.5 && solido(level, pes.below())) {
			escolhido = pes.below(); // pilar: ele sobe por dentro
		} else {
			double dx = this.h.getX() - alvo.getX();
			double dz = this.h.getZ() - alvo.getZ();
			Direction lado = Direction.getApproximateNearest(dx, 0.0, dz);
			for (int k = 1; k <= 2 && escolhido == null; k++) {
				for (BlockPos b : new BlockPos[] {pes.relative(lado, k), pes.relative(lado, k).above()}) {
					if (solido(level, b)) {
						escolhido = b;
						break;
					}
				}
			}
			if (escolhido == null && solido(level, pes.below())) {
				escolhido = pes.below();
			}
		}
		if (escolhido == null) {
			this.semCaminho = 0;
			return;
		}
		this.avisoBloco = escolhido.immutable();
		// O aviso também encurta com o uso: de 2,5 s até 1,5 s.
		this.avisoAtravessar = Math.max(30, 50 - 5 * usos);
		this.h.getNavigation().stop();
		ApoioCaca.contarAtravessou(alvo, this.h);
		this.mudar(level, alvo, Estagio.ATRAVESSA, "SEM_CAMINHO bloco=" + escolhido.toShortString());
	}

	private void tickAtravessa(ServerLevel level, ServerPlayer alvo) {
		this.h.pararEOlhar();
		BlockPos bloco = this.avisoBloco;
		if (bloco == null) {
			this.mudar(level, alvo, Estagio.PERSEGUE, "SEM_BLOCO");
			return;
		}
		if (this.avisoAtravessar % 5 == 0) {
			ApoioCaca.avisoNoBloco(level, alvo, bloco, this.avisoAtravessar % 10 == 0);
		}
		if (--this.avisoAtravessar > 0) {
			return;
		}
		this.semCaminho = 0;
		this.avisoBloco = null;
		Vec3 centro = Vec3.atCenterOf(bloco);
		if (alvo.position().add(0, 0.5, 0).distanceToSqr(centro) <= 1.9 * 1.9) {
			this.capturar(level, alvo, "ATRAVESSOU");
			return;
		}
		// O jogador saiu de perto: ele aparece ali, e a perseguição continua.
		BlockPos saida = null;
		double melhor = Double.MAX_VALUE;
		for (Direction d : Direction.values()) {
			BlockPos c = bloco.relative(d);
			if (level.getBlockState(c).isAir() && level.getBlockState(c.above()).isAir() && solido(level, c.below())) {
				double dd = c.distSqr(alvo.blockPosition());
				if (dd < melhor) {
					melhor = dd;
					saida = c;
				}
			}
		}
		if (saida != null) {
			this.h.snapTo(saida.getX() + 0.5, saida.getY(), saida.getZ() + 0.5, this.h.getYRot(), 0.0F);
			this.h.olharPara(alvo);
			this.busca.ouvirAcao(level, this.h, alvo.position(), 0.9, "ATRAVESSOU");
		}
		this.mudar(level, alvo, Estagio.PERSEGUE, saida != null ? "ATRAVESSOU_VAZIO" : "NAO_COUBE");
	}

	private static boolean solido(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	// ===== Vela =====

	private void iniciarEsperaVela(ServerLevel level, ServerPlayer alvo) {
		this.esperaVela = 0;
		this.esperaVelaMaxima = 400 + this.sorte.nextInt(401); // 20 a 40 s: a vela é abrigo, não sala de espera
		this.soprarEm = ApoioCaca.podeSoprarVela(alvo) && this.sorte.nextFloat() < 0.35F
				? (int) (this.esperaVelaMaxima * (0.4 + this.sorte.nextDouble() * 0.3))
				: -1;
		this.pontoDaBorda = ApoioCaca.pontoNaBordaDaVela(level, alvo, this.h);
		this.h.getNavigation().stop();
		this.mudar(level, alvo, Estagio.ESPERA_VELA, "ALVO_NA_VELA soprar=" + (this.soprarEm >= 0 ? "sim" : "nao"));
	}

	private void tickEsperaVela(ServerLevel level, ServerPlayer alvo, boolean percebido) {
		if (!Diretor.emZonaCalma(alvo, alvo.getX(), alvo.getY(), alvo.getZ())) {
			// A vela acabou, ou o jogador saiu dela.
			this.busca.ouvirAcao(level, this.h, alvo.position(), 0.9, "VELA_ACABOU");
			this.mudar(level, alvo, Estagio.PERSEGUE, "VELA_ACABOU");
			return;
		}
		Vec3 ponto = this.pontoDaBorda;
		if (percebido || ponto == null || this.h.position().distanceToSqr(ponto) < 2.25) {
			this.h.pararEOlhar();
		} else if (this.h.tickCount % 10 == 0) {
			this.h.soltarOlhar();
			this.h.getNavigation().moveTo(ponto.x, ponto.y, ponto.z, modificadorPara(VELOCIDADE_INICIAL));
		}
		this.esperaVela++;
		if (this.soprarEm >= 0 && this.esperaVela == this.soprarEm) {
			ApoioCaca.soprarVela(level, alvo, this.h);
			return;
		}
		if (this.esperaVela > this.esperaVelaMaxima) {
			this.encerrar(level, alvo, "VELA");
		}
	}

	// ===== Ser ferido =====

	/**
	 * O jogador bateu nele durante a caçada. Nas duas primeiras vezes ele recua (some e reaparece fora da
	 * tela); da terceira em diante, nada acontece. Bater deixou de encerrar a caçada.
	 */
	void aoSerFerido(ServerLevel level, ServerPlayer alvo) {
		if (this.golpes >= 2) {
			return;
		}
		this.golpes++;
		BlockPos lugar = this.lugarForaDaTela(level, alvo, 10, 14, false);
		if (lugar != null) {
			this.h.getNavigation().stop();
			this.h.snapTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, this.h.getYRot(), 0.0F);
			this.h.olharPara(alvo);
		}
		ApoioCaca.somDeRecuo(alvo, this.h);
		this.recargaAtalho = Math.max(this.recargaAtalho, 100);
		this.log(level, alvo, "CACA id=" + this.h.getIdManifestacao() + " ferido n=" + this.golpes
				+ " recuou=" + (lugar != null ? "sim" : "nao"));
	}

	// ===== Desfechos =====

	/** Perdeu o rastro. Em parte das vezes é mentira: ele fica calado, fora de vista, e volta. Uma vez por caçada. */
	private void desistir(ServerLevel level, ServerPlayer alvo, String motivo) {
		if (!this.fingiu && this.foraDaTela > 20 && this.sorte.nextFloat() < 0.35F) {
			this.fingiu = true;
			this.fingeRestante = 160 + this.sorte.nextInt(141); // 8 a 15 s
			this.h.getNavigation().stop();
			this.mudar(level, alvo, Estagio.FINGE, "FINGIU_" + motivo);
			return;
		}
		this.encerrar(level, alvo, motivo);
	}

	private void encerrar(ServerLevel level, ServerPlayer alvo, String motivo) {
		if (this.encerrada) {
			return;
		}
		this.log(level, alvo, String.format(Locale.ROOT,
				"CACA id=%s FIM motivo=%s duracao=%ds contato=%ds piscadas=%d atalhos=%d golpes=%d fingiu=%s",
				this.h.getIdManifestacao(), motivo, this.idade / 20, this.contato / 20, this.piscadas, this.atalhos,
				this.golpes, this.fingiu ? "sim" : "nao"));
		this.h.sumir(level, false, motivo);
	}

	private void capturar(ServerLevel level, ServerPlayer alvo, String como) {
		if (this.encerrada) {
			return;
		}
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s CAPTURA como=%s duracao=%ds contato=%ds",
				this.h.getIdManifestacao(), como, this.idade / 20, this.contato / 20));
		ApoioCaca.capturar(level, alvo, this.h);
		this.h.sumir(level, false, "TOCOU");
	}

	private void mudar(ServerLevel level, ServerPlayer alvo, Estagio novo, String motivo) {
		this.log(level, alvo, "CACA id=" + this.h.getIdManifestacao() + " " + this.estagio + " -> " + novo + " motivo=" + motivo);
		this.estagio = novo;
	}

	private void log(ServerLevel level, ServerPlayer alvo, String linha) {
		if (Depuracao.ativo) {
			Depuracao.log(alvo, level.getGameTime() / 20, linha);
		}
	}
}
