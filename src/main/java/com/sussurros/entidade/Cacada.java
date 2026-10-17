package com.sussurros.entidade;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.ApoioCaca;
import com.sussurros.assombracao.Depuracao;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Percepcao;
import com.sussurros.assombracao.Saltos;
import com.sussurros.assombracao.VozDaCacada;
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
 *
 * 0.9.0-alpha24: ele passa por vão de um bloco. O caminho é medido com o corpo de quatro (NavegacaoDoHospede), o
 * corpo de verdade muda só onde o lugar pede (ajustarCorpo), e antes de entrar num vão perto do jogador ele para
 * na boca e avisa. Um vão aberto é caminho, não exceção: vale também na primeira caçada.
 *
 * 0.9.0-alpha26: a caçada com chance de verdade (as contas estão em assombracao/Saltos). Os saltos (atalho e
 * atravessar) destrancam pelo truque que o jogador repete, nunca por captura; nenhum acontece nos quinze segundos
 * depois do aviso; o atalho nunca cai nas costas, e depois de qualquer salto ele fica parado um tempo maior quanto
 * mais perto caiu. Ser ferido o tira da caçada por uns segundos, longe, e a volta é anunciada por passos. Cada
 * caçada deixa uma linha de resumo no log.
 *
 * 0.9.0-alpha27: a voz. Raramente, e só em três momentos (ver assombracao/VozDaCacada), ele grita ou rosna, de onde
 * está e só para o alvo; nunca em cima de outro sinal da caçada.
 */
final class Cacada {
	enum Estagio {
		AVISO,       // ele nasceu e não se mexe: o mundo emudeceu, uma luz falhou
		PERSEGUE,    // anda quando não é visto; procura quando perde o rastro
		ESPERA_VELA, // o alvo está na zona da vela: espera na borda, por tempo limitado
		FINGE,       // fingiu que desistiu: calado, fora de vista, prestes a voltar
		ATRAVESSA,   // sem caminho até o alvo: avisa num bloco e passa por ele
		FERIDO       // o jogador o feriu: longe, parado, por uns segundos; volta anunciado por passos
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
	/** De quatro ele anda a pouco mais da metade: é no vão que o jogador tem tempo de vê-lo entrar. */
	private static final double LENTO_DE_QUATRO = 0.55;
	/** Só avisa na boca de um vão que esteja a até isto do jogador, no chão (blocos). Mais longe é só caminho. */
	private static final double AVISO_DO_VAO = 8.0;

	private final HospedeEntity h;
	private final HospedeBusca busca;
	/** Sorteios da caçada. Não usa o gerador do mundo, para não mudar os sorteios do Diretor. */
	private final RandomSource sorte = RandomSource.create();
	/**
	 * O que está destrancado nesta caçada, pelo que o jogador fez nas anteriores (ver Saltos). Na primeira caçada da
	 * vida dele nada está: ele aprende a regra antes das exceções. Até a alpha25 era isso e só isso, e da segunda
	 * em diante valia tudo.
	 */
	private final Saltos.Liberacao liberacao;
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
	/** Ticks seguidos sem caminho com o alvo entre 8 e 16 blocos: longe demais para atravessar, perto demais para o atalho. */
	private int semCaminhoLonge;
	/** Só para o log: no último teste de caminho, não havia como chegar ao alvo. */
	private boolean semCaminhoAgora;
	private boolean fingiu;
	private int fingeRestante;
	private double velocidade = VELOCIDADE_INICIAL;
	private boolean encerrada;
	/** Ticks parado antes de tentar de novo: uma Linha de Cinza o segurou, ou ele atravessou e não achou ninguém. */
	private int barrado;
	/** Quantas vezes a Caixa de Música o chamou nesta caçada. Cada vez ele acredita menos. */
	private int iscas;
	private long ultimaIscaTick = -1000;

	@Nullable
	private BlockPos avisoBloco;
	private int avisoAtravessar;
	/** O bloco do aviso é o que está debaixo dos pés do alvo: ele sobe por dentro, e subir mais não escapa. */
	private boolean avisoPorBaixo;
	@Nullable
	private BlockPos porta;
	private int portaEspera;
	private int esperaVela;
	private int esperaVelaMaxima;
	private int soprarEm = -1;
	@Nullable
	private Vec3 pontoDaBorda;

	/** Ticks que ele ainda fica parado na boca de um vão, avisando, antes de entrar. */
	private int vaoEspera;
	@Nullable
	private BlockPos vaoBoca;
	/** Quando ele avisou num vão pela última vez: não avisa de novo antes de dez segundos. */
	private long vaoAvisadoTick = -1000;
	/** Quantas vezes ele ficou de quatro nesta caçada (só para o log). */
	private int vaos;
	/** Ticks seguidos em que o corpo abaixado já cabe: ele só se levanta depois de alguns, para não ficar trocando. */
	private int cabendo;

	/** Quando (na idade da caçada) ele começou a perseguir: nenhum salto nos quinze segundos seguintes. -1 no aviso. */
	private int persegueDesde;
	/** O mais perto que o jogador esteve dele, e se depois disso o jogador fugiu para longe (é uma das táticas). */
	private double menorDist = Double.MAX_VALUE;
	private boolean fugiu;
	/** O jogador ficou onde não havia caminho, e a resposta a isso ainda estava trancada: desta vez funcionou. */
	private boolean truque;
	/** Ticks que ele ainda fica fora da caçada, depois de ser ferido. */
	private int feridoRestante;
	/** Só para o log: quantas vezes atravessou, quando foi o último salto e a menor janela que um salto deu (ticks). */
	private int travessias;
	private int ultimoSalto = -1;
	private int menorJanela = Integer.MAX_VALUE;
	/** A voz (0.9.0-alpha27): quantas nesta caçada, quando foi a última e quando foi o último outro sinal (ticks da caçada). */
	private int vozes;
	private int ultimaVoz = -1;
	private int ultimoSinal = -1;
	/** Ele ficou muito tempo sem saber do jogador: vê-lo de novo é um reencontro. */
	private boolean perdido;

	Cacada(HospedeEntity h, HospedeBusca busca, ServerPlayer alvo, double fatorDoDiretor, boolean comAviso) {
		this.h = h;
		this.busca = busca;
		this.fatorDoDiretor = Math.max(0.9, Math.min(1.2, fatorDoDiretor));
		// A origem da criatura ainda não está definida aqui (o Diretor a marca depois de configurar): uma caçada de
		// comando segue a memória do jogador como uma de verdade, e é isso que os testes usam.
		this.liberacao = ApoioCaca.liberacao(alvo);
		this.persegueDesde = comAviso ? -1 : 0;
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

	/** Nestes estágios ele anda, e o corpo acompanha o lugar: o caminho é medido de quatro (ver NavegacaoDoHospede). */
	boolean cuidaDoCorpo() {
		return this.estagio == Estagio.PERSEGUE || this.estagio == Estagio.ESPERA_VELA;
	}

	/** Chamado quando a criatura some por qualquer motivo, para o fim ser contado uma vez só. */
	void marcarEncerrada() {
		this.encerrada = true;
	}

	/**
	 * A caçada acabou, por qualquer motivo (HospedeEntity chama uma vez só, antes de avisar o Diretor): a linha de
	 * resumo do log, que o analisador junta numa tabela. A menor janela é a que um salto deu, em conta (parado mais
	 * o tempo de chegar andando); "desdeOSalto" é medido, do último salto até o fim.
	 */
	void terminar(ServerLevel level, ServerPlayer alvo, String motivo) {
		if (this.encerrada) {
			return;
		}
		this.encerrada = true;
		this.log(level, alvo, String.format(Locale.ROOT,
				"CACA id=%s resumo fim=%s duracao=%ds perseguiu=%ds atalhos=%d travessias=%d golpes=%d menorJanela=%s desdeOSalto=%s liberado=%s fugiu=%s truque=%s vozes=%d",
				this.h.getIdManifestacao(), motivo, this.idade / 20,
				this.persegueDesde < 0 ? 0 : (this.idade - this.persegueDesde) / 20,
				this.atalhos, this.travessias, this.golpes,
				this.menorJanela == Integer.MAX_VALUE ? "-" : String.format(Locale.ROOT, "%.1fs", this.menorJanela / 20.0),
				this.ultimoSalto < 0 ? "-" : String.format(Locale.ROOT, "%.1fs", (this.idade - this.ultimoSalto) / 20.0),
				this.liberacao.paraLog(), this.fugiu ? "sim" : "nao", this.truque ? "sim" : "nao", this.vozes));
	}

	/** O jogador fugiu para longe nesta caçada (o Diretor guarda na memória quando ela acaba). */
	boolean fugiu() {
		return this.fugiu;
	}

	/** O jogador ficou onde não havia caminho, e funcionou (o Diretor guarda na memória quando ela acaba). */
	boolean usouTruque() {
		return this.truque;
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
		// Fugir para longe é uma das táticas que ele aprende (ver Saltos.fugiu): conta no aviso e na perseguição,
		// não enquanto ele mesmo está longe por outro motivo (fingindo, ferido).
		if (this.estagio == Estagio.AVISO || this.estagio == Estagio.PERSEGUE) {
			this.menorDist = Math.min(this.menorDist, dist);
			if (!this.fugiu && Saltos.fugiu(dist, this.menorDist)) {
				this.fugiu = true;
				this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s fugiu dist=%.0f menor=%.0f estagio=%s",
						this.h.getIdManifestacao(), dist, this.menorDist, this.estagio));
			}
		}

		switch (this.estagio) {
			case AVISO -> {
				this.h.pararEOlhar();
				if (--this.avisoRestante <= 0) {
					// O aviso é a chance de o jogador se preparar, não de sumir: quando acaba, ele sabe onde o
					// jogador está. Sem isto ele saía atrás do lugar de oito a dez segundos antes, e bastava andar
					// para longe durante o aviso (a mais de 42 blocos ele não ouve) para a caçada acabar sozinha.
					this.busca.ouvirAcao(level, this.h, alvo.position(), 0.9, "FIM_DO_AVISO");
					this.persegueDesde = this.idade;
					this.mudar(level, alvo, Estagio.PERSEGUE, "FIM_DO_AVISO");
					this.voz(level, alvo, VozDaCacada.Momento.INICIO);
				}
			}
			case FERIDO -> this.tickFerido(level, alvo, percebido, dist);
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
			this.talvezReencontro(level, alvo, true);
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
			// Encarar o segura, mas não esconde que não há caminho. Este teste só existia no ramo de baixo: quem
			// subia num pilar e ficava olhando para ele o deixava congelado até o teto de tempo, porque o piscar
			// não sobe e a regra de atravessar nunca era consultada.
			if (this.h.tickCount % 20 == 0) {
				this.verificarCaminho(level, alvo, dist, true);
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
		this.talvezReencontro(level, alvo, ve);
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
		double passo = this.passoAgora(level);
		this.busca.definirVelocidade(modificadorPara(passo));

		if (this.tentarAtalho(level, alvo, dist, sabe, passo)) {
			return;
		}

		// O vão de um bloco: perto do jogador, ele para na boca e avisa antes de entrar (como atrás de uma porta).
		if (this.vaoEspera > 0) {
			this.tickVao(level, alvo);
			return;
		}
		if (this.ajustarCorpo(level, alvo, true)) {
			return;
		}

		if (this.porta != null) {
			this.tickPorta(level, alvo);
			return;
		}
		if (ve) {
			// Vendo o alvo, ele não procura: vai direto até ele.
			if (this.h.tickCount % 5 == 0) {
				this.h.getNavigation().moveTo(alvo, modificadorPara(passo));
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

	// ===== O corpo: abaixado onde cabe, de quatro onde o teto só deixa um bloco =====

	/**
	 * O corpo acompanha o lugar: abaixado (1,9) onde cabe, de quatro (0,9) onde o teto só deixa um bloco ou onde
	 * o próximo passo do caminho só deixa. Só é chamado com ele fora da tela: o jogador nunca o vê trocar de
	 * corpo, vê uma pose de cada vez. Devolve true quando ele acabou de parar na boca de um vão para avisar.
	 */
	private boolean ajustarCorpo(ServerLevel level, ServerPlayer alvo, boolean comAviso) {
		boolean cabeAqui = this.h.cabeAbaixado(level, this.h.position());
		BlockPos adiante = this.vaoAdiante(level);
		if (cabeAqui && adiante == null) {
			// Andando, ele se levanta um quarto de segundo depois de sair do vão. Parado e sem caminho (acabou de
			// avisar na boca e espera o próximo passo da busca), só depois de dois segundos: sem isto ele se
			// levantava e voltava a ficar de quatro no mesmo lugar, conforme o tick em que a busca rodava.
			Path caminho = this.h.getNavigation().getPath();
			boolean andando = caminho != null && !caminho.isDone();
			if (this.h.isDeQuatro() && ++this.cabendo >= (andando ? 5 : 40)) {
				this.h.definirDeQuatro(false);
				this.logCorpo(level, alvo, "SAIU");
			}
			return false;
		}
		this.cabendo = 0;
		if (!this.h.isDeQuatro()) {
			this.h.definirDeQuatro(true);
			this.vaos++;
			this.logCorpo(level, alvo, cabeAqui ? "VAO" : "TETO");
		}
		// O aviso não depende de ele ter acabado de se abaixar: quem vem de longe por um túnel de um bloco já
		// chega de quatro, e avisa do mesmo jeito quando o trecho que falta entra nos oito blocos do jogador.
		if (!comAviso || adiante == null || !this.deveAvisar(level, alvo, adiante)) {
			return false;
		}
		this.vaoBoca = adiante.immutable();
		this.vaoAvisadoTick = level.getGameTime();
		this.vaoEspera = 30 + this.sorte.nextInt(31); // de 1,5 a 3 s, como a espera atrás de uma porta
		this.sinal();
		this.h.getNavigation().stop();
		ApoioCaca.contarVao(alvo, this.h);
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s vao boca=%s espera=%dt dist=%.1f",
				this.h.getIdManifestacao(), HospedeBusca.pos(this.vaoBoca), this.vaoEspera, Math.sqrt(this.h.distanceToSqr(alvo))));
		return true;
	}

	/**
	 * Um dos dois próximos passos do caminho, a até dois blocos, onde o corpo abaixado não cabe: a boca de um vão.
	 * Conta também o degrau para cima debaixo de teto baixo, em que abaixado ele bateria a cabeça no pulo.
	 */
	@Nullable
	private BlockPos vaoAdiante(ServerLevel level) {
		Path caminho = this.h.getNavigation().getPath();
		if (caminho == null || caminho.isDone()) {
			return null;
		}
		int proximo = caminho.getNextNodeIndex();
		for (int i = proximo; i < Math.min(caminho.getNodeCount(), proximo + 2); i++) {
			BlockPos no = caminho.getNodePos(i);
			double dx = no.getX() + 0.5 - this.h.getX();
			double dz = no.getZ() + 0.5 - this.h.getZ();
			if (dx * dx + dz * dz > 4.0) {
				break;
			}
			double chao = WalkNodeEvaluator.getFloorLevel(level, no);
			if (!this.h.cabeAbaixado(level, new Vec3(no.getX() + 0.5, chao, no.getZ() + 0.5))) {
				return no;
			}
			double sobe = chao - this.h.getY();
			if (sobe > 0.6 && !this.h.cabeAbaixado(level, this.h.position().add(0, sobe, 0))) {
				return no;
			}
		}
		return null;
	}

	/** O vão é a proteção de alguém quando está perto dele; longe, é só o caminho por onde ele vem. */
	private boolean deveAvisar(ServerLevel level, ServerPlayer alvo, BlockPos boca) {
		double dx = alvo.getX() - (boca.getX() + 0.5);
		double dz = alvo.getZ() - (boca.getZ() + 0.5);
		if (dx * dx + dz * dz > AVISO_DO_VAO * AVISO_DO_VAO || Math.abs(alvo.getY() - boca.getY()) > 5.0) {
			return false;
		}
		return level.getGameTime() - this.vaoAvisadoTick > 200;
	}

	/** Parado na boca do vão, de quatro, olhando para dentro: algo se arrasta ali, e só o alvo ouve. */
	private void tickVao(ServerLevel level, ServerPlayer alvo) {
		this.h.pararEOlhar();
		if (this.vaoEspera % 10 == 0 && this.vaoBoca != null) {
			ApoioCaca.avisoNoVao(level, alvo, this.vaoBoca, this.vaoEspera / 10 % 2 == 0);
		}
		if (--this.vaoEspera <= 0) {
			this.vaoBoca = null;
		}
	}

	/**
	 * Ele acabou de mudar de lugar sem andar (o piscar, o atalho, atravessar, o recuo): o corpo é o que cabe ali, e o
	 * embalo de antes fica para trás. Sem zerar o embalo, ele chegava escorregando uns vinte centímetros na direção em
	 * que andava antes do salto (achado pelo teste do salto, na alpha26).
	 */
	private void aoMudarDeLugar(ServerLevel level) {
		this.h.definirDeQuatro(!this.h.cabeAbaixado(level, this.h.position()));
		this.h.setDeltaMovement(Vec3.ZERO);
		this.cabendo = 0;
	}

	private void logCorpo(ServerLevel level, ServerPlayer alvo, String motivo) {
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s corpo=%s motivo=%s pos=%s dist=%.1f",
				this.h.getIdManifestacao(), this.h.isDeQuatro() ? "DE_QUATRO" : "ABAIXADO", motivo,
				HospedeBusca.pos(this.h.blockPosition()), Math.sqrt(this.h.distanceToSqr(alvo))));
	}

	/** O passo dele agora, em blocos por segundo. Na luz forte ele é mais lento: é por isso que apaga as luzes por onde passa. */
	private double passoAgora(ServerLevel level) {
		boolean claro = level.getBrightness(LightLayer.BLOCK, this.h.blockPosition().above()) >= 12;
		return this.velocidade * this.fatorDoDiretor * (claro ? 0.85 : 1.0) * (this.h.isDeQuatro() ? LENTO_DE_QUATRO : 1.0);
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
		// Ele não salta uma Linha de Cinza no piscar: para antes dela. E não atravessa nada que tenha corpo: o
		// avanço não testava o trajeto, e quem o olhava por uma janela (vidro, grade, cerca) podia ser tocado com
		// a tela ainda fechada.
		Vec3 meioPasso = direcao.normalize().scale(0.5);
		Vec3 ponto = this.h.position();
		double livre = 0;
		int andar = this.h.blockPosition().getY();
		for (double d = 0.5; d <= avanco + 0.01; d += 0.5) {
			ponto = ponto.add(meioPasso);
			if (CinzaEspalhadaBlock.linhaPerto(level, BlockPos.containing(ponto), 0) != null) {
				break;
			}
			int novoAndar = andarLivre(level, ponto.x, ponto.z, andar);
			if (novoAndar == Integer.MIN_VALUE) {
				break;
			}
			andar = novoAndar;
			livre = d;
		}
		if (livre < 0.5) {
			// A linha reta está tampada. Se há caminho por outro lado (uma fresta, a volta na parede), é por ele
			// que o piscar o traz: encarar segura, mas não fecha uma passagem que está aberta. Sem isto, olhar para
			// ele por um buraco de um bloco o deixava parado do lado de fora até a caçada acabar.
			this.avancarPeloCaminho(level, alvo, avanco);
			return;
		}
		Vec3 destino = this.h.position().add(direcao.normalize().scale(livre));
		// O chão é o do andar em que o avanço chegou, ou logo abaixo. Até a alpha23 era o primeiro chão achado de
		// cima para baixo, a partir de cinco blocos acima dele: debaixo de um teto de até cinco blocos de espessura
		// (uma casa, uma caverna rasa, a copa de uma árvore) o chão achado era o lado de cima do teto, o avanço era
		// recusado por desnível, e quem o encarava ali o segurava até a caçada acabar.
		BlockPos chao = chaoNoAndar(level, destino.x, andar, destino.z);
		if (chao == null || Math.abs(chao.getY() - this.h.getY()) > 2.5
				|| Diretor.emZonaCalma(alvo, chao.getX(), chao.getY(), chao.getZ())) {
			return;
		}
		this.h.getNavigation().stop();
		this.h.snapTo(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5, this.h.getYRot(), 0.0F);
		this.aoMudarDeLugar(level);
		this.h.olharPara(alvo);
	}

	/**
	 * O piscar por onde há caminho: até "avanco" blocos ao longo dele, um passo de cada vez. Para antes de uma
	 * Linha de Cinza, da zona da vela e de encostar no jogador, como o avanço em linha reta. O caminho só passa
	 * por onde ele cabe, então isto continua não atravessando nada que tenha corpo. Onde o passo seguinte é um vão,
	 * ele já fica de quatro: é a pose que o jogador vê ao abrir os olhos.
	 */
	private void avancarPeloCaminho(ServerLevel level, ServerPlayer alvo, double avanco) {
		Path caminho = this.h.getNavigation().createPath(alvo, 0);
		if (caminho == null) {
			return;
		}
		Node fim = caminho.getEndNode();
		if (!caminho.canReach() && (fim == null || fim.asBlockPos().distSqr(alvo.blockPosition()) > 2.25)) {
			return;
		}
		Vec3 de = this.h.position();
		Vec3 destino = null;
		boolean vaoLogo = false;
		double andado = 0;
		for (int i = caminho.getNextNodeIndex(); i < caminho.getNodeCount(); i++) {
			BlockPos no = caminho.getNodePos(i);
			Vec3 ponto = new Vec3(no.getX() + 0.5, WalkNodeEvaluator.getFloorLevel(level, no), no.getZ() + 0.5);
			double trecho = ponto.distanceTo(de);
			if (andado + trecho > avanco + 0.01
					|| CinzaEspalhadaBlock.linhaPerto(level, no, 0) != null
					|| Diretor.emZonaCalma(alvo, ponto.x, ponto.y, ponto.z)
					|| ponto.distanceToSqr(alvo.position()) < 1.6 * 1.6) {
				vaoLogo = destino != null && !this.h.cabeAbaixado(level, ponto);
				break;
			}
			andado += trecho;
			de = ponto;
			destino = ponto;
		}
		if (destino == null || andado < 0.5) {
			return;
		}
		this.h.getNavigation().stop();
		this.h.snapTo(destino.x, destino.y, destino.z, this.h.getYRot(), 0.0F);
		this.aoMudarDeLugar(level);
		if (vaoLogo) {
			this.h.definirDeQuatro(true);
		}
		this.h.olharPara(alvo);
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s piscar pelo caminho andou=%.1f corpo=%s",
				this.h.getIdManifestacao(), andado, this.h.isDeQuatro() ? "DE_QUATRO" : "ABAIXADO"));
	}

	/** O chão de uma coluna a partir de um andar: nele, ou até três blocos abaixo. Nunca acima. */
	@Nullable
	private static BlockPos chaoNoAndar(ServerLevel level, double x, int andar, double z) {
		for (int dy = 0; dy >= -3; dy--) {
			BlockPos pos = BlockPos.containing(x, andar + dy, z);
			if (Diretor.livre(level, pos) && Diretor.livre(level, pos.above()) && solido(level, pos.below())) {
				return pos;
			}
		}
		return null;
	}

	/**
	 * Dá para dar meio passo até esta coluna? Procura dois blocos sem corpo no mesmo andar, um acima (degrau)
	 * ou até dois abaixo (descida). Devolve o andar em que ele fica, ou Integer.MIN_VALUE se há algo no caminho.
	 * Um degrau só vale se o bloco em que ele sobe tem até um bloco de altura: cerca e muro não são degrau.
	 */
	private static int andarLivre(ServerLevel level, double x, double z, int andar) {
		for (int dy : new int[] {0, 1, -1, -2}) {
			BlockPos pes = BlockPos.containing(x, andar + dy, z);
			if (solido(level, pes) || solido(level, pes.above())) {
				continue;
			}
			if (dy > 0) {
				BlockPos degrau = pes.below();
				if (level.getBlockState(degrau).getCollisionShape(level, degrau).max(Direction.Axis.Y) > 1.0) {
					continue;
				}
			}
			return andar + dy;
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * Atalho fora de vista: três segundos sem ser visto e longe (ou sem caminho), ele reaparece fora da tela, de
	 * preferência à frente de para onde o jogador está indo. Nunca na tela, nunca nas costas, e depois fica parado,
	 * com o som de pano, o bastante para o jogador poder reagir (ver Saltos).
	 */
	private boolean tentarAtalho(ServerLevel level, ServerPlayer alvo, double dist, boolean sabe, double passo) {
		if (this.recargaAtalho > 0 || this.foraDaTela < 60 || !sabe) {
			return false;
		}
		// Só de longe. De perto e sem caminho vale a outra regra: atravessar, com aviso. Entre as duas (de 8 a 16
		// blocos, sem caminho) não valia nenhuma: um anel fechado em volta do jogador o deixava parado do lado
		// de fora até a caçada acabar. Três segundos assim, e o atalho vale também ali.
		boolean anel = dist <= 16;
		if (anel && this.semCaminhoLonge < 60) {
			return false;
		}
		// 0.9.0-alpha26: cada um destranca pelo truque a que responde. O de longe, depois de duas caçadas em que o
		// jogador fugiu para longe; o do anel, depois de duas em que ele ficou onde não havia caminho.
		if (anel ? !this.liberacao.semCaminho() : !this.liberacao.deLonge()) {
			if (anel) {
				this.anotarTruque(level, alvo, "ANEL");
			}
			return false;
		}
		if (this.cedo()) {
			return false;
		}
		// O de longe cai a pelo menos doze blocos. O do anel é o lado de dentro de um muro e continua de 8 a 12 (a
		// doze ele cairia do lado de fora de novo): a espera, que é maior quanto mais perto ele cai, cobre a diferença.
		boolean saltou = anel
				? this.saltarPara(level, alvo, dist, passo, 8, 12, "atalho")
				: this.saltarPara(level, alvo, dist, passo, Saltos.DISTANCIA_MINIMA, Saltos.DISTANCIA_MINIMA + 4, "atalho");
		if (!saltou) {
			this.recargaAtalho = 40;
			return false;
		}
		this.recargaAtalho = 200;
		this.semCaminhoLonge = 0;
		return true;
	}

	/**
	 * O salto do atalho: um lugar fora da tela entre distMin e distMax, e a espera parada que a distância pede. Sem
	 * lugar, não salta. Sempre com som (era em metade das vezes): ele acabou de mudar de lugar sem andar, e o jogador
	 * tem de ter como saber de que lado ele está agora.
	 */
	private boolean saltarPara(ServerLevel level, ServerPlayer alvo, double dist, double passo, double distMin, double distMax, String oQue) {
		BlockPos lugar = this.lugarForaDaTela(level, alvo, distMin, distMax, true, false);
		if (lugar == null) {
			return false;
		}
		this.h.getNavigation().stop();
		this.h.snapTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, this.h.getYRot(), 0.0F);
		this.aoMudarDeLugar(level);
		this.h.olharPara(alvo);
		this.busca.ouvirAcao(level, this.h, alvo.position(), 0.9, "ATALHO");
		this.atalhos++;
		ApoioCaca.somDePano(alvo, this.h);
		this.sinal();
		double para = Math.sqrt(this.h.distanceToSqr(alvo));
		// Parado, olhando para o jogador: o "barrado" é a mesma espera de quando uma linha o segura.
		this.barrado = Saltos.esperaDepois(para, passo, ALCANCE_TOQUE);
		int janela = Saltos.janela(this.barrado, para, passo, ALCANCE_TOQUE);
		this.saltou(janela);
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s %s n=%d de=%.0f para=%.0f espera=%dt janela=%.1fs costas=%s",
				this.h.getIdManifestacao(), oQue, this.atalhos, dist, para, this.barrado, janela / 20.0,
				this.nasCostasDe(alvo) ? "sim" : "nao"));
		return true;
	}

	/** Só para os testes: o salto do atalho agora, sem as travas de quando ele pode acontecer. */
	boolean saltarParaTeste(ServerLevel level, ServerPlayer alvo) {
		double dist = Math.sqrt(this.h.distanceToSqr(alvo));
		return this.saltarPara(level, alvo, dist, this.passoAgora(level), Saltos.DISTANCIA_MINIMA, Saltos.DISTANCIA_MINIMA + 4, "atalho_teste");
	}

	/** Um sinal da caçada que diz ao jogador o que fazer (pano, vão, maçaneta, aviso no bloco): a voz não vem em cima. */
	private void sinal() {
		this.ultimoSinal = this.idade;
	}

	/**
	 * A voz, se cabe agora (ver VozDaCacada): a de longe ou a de perto, de onde ele está, só para o alvo. O sorteio é
	 * o da caçada, nunca o do mundo.
	 */
	private void voz(ServerLevel level, ServerPlayer alvo, VozDaCacada.Momento momento) {
		double sorteio = this.sorte.nextDouble();
		if (!VozDaCacada.pode(momento, this.vozes, this.ultimaVoz < 0 ? -1 : this.idade - this.ultimaVoz,
				this.ultimoSinal < 0 ? -1 : this.idade - this.ultimoSinal, sorteio)) {
			return;
		}
		double dist = Math.sqrt(this.h.distanceToSqr(alvo));
		boolean longe = VozDaCacada.deLonge(dist);
		this.vozes++;
		this.ultimaVoz = this.idade;
		ApoioCaca.voz(alvo, this.h, longe, VozDaCacada.volume(dist), VozDaCacada.altura(this.sorte.nextDouble()));
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s voz=%s momento=%s dist=%.1f n=%d",
				this.h.getIdManifestacao(), longe ? "LONGE" : "PERTO", momento, dist, this.vozes));
	}

	/** Muito tempo sem saber do jogador, e agora ele o vê (ou é visto): um reencontro, que às vezes tem voz. */
	private void talvezReencontro(ServerLevel level, ServerPlayer alvo, boolean viu) {
		if (viu) {
			if (this.perdido) {
				this.perdido = false;
				this.voz(level, alvo, VozDaCacada.Momento.REENCONTRO);
			}
		} else if (this.busca.idadeDoConhecimento(level.getGameTime()) > VozDaCacada.PERDIDO_HA) {
			this.perdido = true;
		}
	}

	/** Nos quinze segundos depois do aviso não há salto: é quando o jogador decide o que fazer. */
	private boolean cedo() {
		return this.persegueDesde < 0 || this.idade - this.persegueDesde < Saltos.SEM_SALTO_DEPOIS_DO_AVISO;
	}

	/** Um salto aconteceu agora, e deu esta janela (ticks) até o primeiro toque possível. Só para o resumo do log. */
	private void saltou(int janela) {
		this.ultimoSalto = this.idade;
		this.menorJanela = Math.min(this.menorJanela, janela);
	}

	/** O jogador ficou onde não havia caminho, e a resposta ainda estava trancada: desta vez funciona (uma vez por caçada). */
	private void anotarTruque(ServerLevel level, ServerPlayer alvo, String qual) {
		if (this.truque) {
			return;
		}
		this.truque = true;
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s truque=%s trancado dist=%.1f",
				this.h.getIdManifestacao(), qual, Math.sqrt(this.h.distanceToSqr(alvo))));
	}

	/** Ele está nas costas do jogador (para o log, que confere a garantia)? */
	private boolean nasCostasDe(ServerPlayer alvo) {
		Vec3 virado = Vec3.directionFromRotation(0.0F, alvo.getYRot());
		return Saltos.nasCostas(virado.x, virado.z, this.h.getX() - alvo.getX(), this.h.getZ() - alvo.getZ());
	}

	/**
	 * Um chão livre entre distMin e distMax do jogador, fora da tela dele, fora da vela e nunca nas costas.
	 * aFrente: prefere os lados da direção em que ele está andando ("ele estava me esperando").
	 * escondido: prefere o lugar que o jogador não enxerga de onde está.
	 *
	 * Até a alpha25, com o jogador parado, o lugar era atrás dele: na sessão de 12/10/2026 o recuo de quem o feriu
	 * caiu a dez blocos nas costas do jogador, e dois segundos depois ele o tocou. Agora é dos lados, entre a borda
	 * da tela e o começo das costas, de onde virar a cabeça o encontra.
	 */
	@Nullable
	private BlockPos lugarForaDaTela(ServerLevel level, ServerPlayer alvo, double distMin, double distMax, boolean aFrente, boolean escondido) {
		Vec3 movimento = alvo.getKnownMovement();
		boolean andando = movimento.horizontalDistanceSqr() > 0.004;
		Vec3 olhar = alvo.getViewVector(1.0F);
		Vec3 virado = Vec3.directionFromRotation(0.0F, alvo.getYRot());
		boolean pelaFrente = andando && aFrente;
		double base = pelaFrente ? Math.atan2(movimento.z, movimento.x) : Math.atan2(virado.z, virado.x);
		double cone = Percepcao.coneSeguro(alvo);
		double ladoMaximo = 180.0 - Saltos.COSTAS_GRAUS;
		BlockPos melhor = null;
		double melhorNota = -1;
		for (int i = 0; i < 24; i++) {
			double abertura = pelaFrente
					? Math.toRadians(35 + this.sorte.nextDouble() * 75)
					: Math.toRadians(55 + this.sorte.nextDouble() * (ladoMaximo - 55));
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
			if (Saltos.nasCostas(virado.x, virado.z, ponto.x - alvo.getX(), ponto.z - alvo.getZ())) {
				continue;
			}
			double nota = this.sorte.nextDouble() + (Math.abs(chao.getY() - alvo.getY()) < 2 ? 1.0 : 0.0)
					+ (escondido && !enxerga(level, alvo, ponto) ? 1.5 : 0.0);
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
			}
		}
		return melhor;
	}

	/** O jogador enxerga este ponto de onde está? Pela forma que se vê: vidro não esconde. */
	private static boolean enxerga(ServerLevel level, ServerPlayer p, Vec3 ponto) {
		return level.clip(new ClipContext(p.getEyePosition(), ponto, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
				.getType() == HitResult.Type.MISS;
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
				this.sinal();
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
		// (Havia aqui também um limite de 32 blocos de desnível, que contradizia a frase de cima: um pilar de
		// quarenta blocos zerava a conta para sempre.)
		if (noChao > 16) {
			this.semCaminho = 0;
			this.semCaminhoLonge = 0;
			return;
		}
		if (!sabe) {
			// Sem notícia recente o relógio não anda, mas também não volta: quem só se esconde em cima do
			// pilar por uns segundos não zera a conta. Muito tempo sem notícia, aí sim, ele perdeu o alvo.
			if (this.busca.idadeDoConhecimento(level.getGameTime()) > 200) {
				this.semCaminho = 0;
				this.semCaminhoLonge = 0;
			}
			return;
		}
		Path caminho = this.h.getNavigation().createPath(alvo, 0);
		boolean chega = caminho != null && (caminho.canReach()
				|| (caminho.getEndNode() != null && caminho.getEndNode().asBlockPos().distSqr(alvo.blockPosition()) <= 2.25));
		if (chega) {
			this.semCaminho = 0;
			this.semCaminhoLonge = 0;
			return;
		}
		this.semCaminhoAgora = true;
		// De 8 a 16 blocos quem responde é o atalho (ver tentarAtalho). A conta de atravessar só anda de perto,
		// como antes, para ele não chegar já com o tempo de espera vencido.
		this.semCaminhoLonge = noChao > 8 ? this.semCaminhoLonge + 20 : 0;
		if (noChao > 10) {
			this.semCaminho = 0;
			return;
		}
		this.semCaminho += 20;
		if (noChao > 8) {
			return;
		}
		// A cada vez que o jogador usa o mesmo truque, ele espera menos (de 10 s até 4 s).
		int usos = ApoioCaca.vezesAtravessou(alvo);
		if (this.semCaminho >= Math.max(80, 200 - 40 * usos)) {
			// 0.9.0-alpha26: nas duas primeiras caçadas em que o jogador fica onde não há caminho, isso funciona (até
			// a alpha25, só na primeira caçada da vida dele, fosse qual fosse o truque). E nunca logo depois do aviso.
			if (!this.liberacao.semCaminho()) {
				this.anotarTruque(level, alvo, "SEM_CAMINHO");
				return;
			}
			if (!this.cedo()) {
				this.iniciarAtravessar(level, alvo, usos);
			}
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
		// Água funda, longe da margem: não há bloco firme perto do jogador, e a conta zerava para sempre. O aviso
		// que já existe passa a ser no próprio bloco de água em que ele está; sair de perto resolve, como sempre.
		if (escolhido == null && !level.getFluidState(pes).isEmpty()) {
			escolhido = pes;
		}
		if (escolhido == null) {
			this.semCaminho = 0;
			return;
		}
		this.avisoPorBaixo = escolhido.equals(pes.below());
		this.avisoBloco = escolhido.immutable();
		// Um piscar que estivesse marcado fica para trás: quando ele voltasse a perseguir, a tela já estaria aberta.
		this.piscarEm = -1;
		// O aviso também encurta com o uso: de 2,5 s até 1,5 s.
		this.avisoAtravessar = Math.max(30, 50 - 5 * usos);
		this.h.getNavigation().stop();
		ApoioCaca.contarAtravessou(alvo, this.h);
		this.travessias++;
		// A janela de quem está em cima do bloco é o próprio aviso: o tempo de sair de perto.
		this.saltou(this.avisoAtravessar);
		this.sinal();
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
		boolean pegou;
		if (this.avisoPorBaixo) {
			// Ele sobe por dentro do pilar: o que escapa é sair da coluna, não subir mais dois blocos durante o
			// aviso (escapava, e cada tentativa ainda encurtava os avisos seguintes).
			double dx = alvo.getX() - centro.x;
			double dz = alvo.getZ() - centro.z;
			pegou = alvo.getY() >= bloco.getY() && dx * dx + dz * dz <= 1.9 * 1.9;
		} else {
			pegou = alvo.position().add(0, 0.5, 0).distanceToSqr(centro) <= 1.9 * 1.9;
		}
		if (pegou) {
			this.capturar(level, alvo, "ATRAVESSOU");
			return;
		}
		// O jogador saiu de perto: ele aparece ali, e a perseguição continua.
		BlockPos saida = null;
		double melhor = Double.MAX_VALUE;
		for (Direction d : Direction.values()) {
			BlockPos c = bloco.relative(d);
			if (Diretor.livre(level, c) && Diretor.livre(level, c.above()) && solido(level, c.below())) {
				double dd = c.distSqr(alvo.blockPosition());
				if (dd < melhor) {
					melhor = dd;
					saida = c;
				}
			}
		}
		if (saida != null) {
			this.h.snapTo(saida.getX() + 0.5, saida.getY(), saida.getZ() + 0.5, this.h.getYRot(), 0.0F);
			this.aoMudarDeLugar(level);
			this.h.olharPara(alvo);
			this.busca.ouvirAcao(level, this.h, alvo.position(), 0.9, "ATRAVESSOU");
			// "Sair de perto resolve" tem de valer: ele aparece ao lado do bloco e fica parado. Sem a pausa, quem
			// tinha saído a dois ou três blocos era tocado no tick seguinte. Até a alpha25 era um segundo e meio
			// fixo; agora é a espera de qualquer salto, maior quanto mais perto (aqui, quase sempre três segundos).
			double d = Math.sqrt(this.h.distanceToSqr(alvo));
			double passo = this.passoAgora(level);
			this.barrado = Saltos.esperaDepois(d, passo, ALCANCE_TOQUE);
			this.saltou(Saltos.janela(this.barrado, d, passo, ALCANCE_TOQUE));
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
		if (!percebido) {
			this.ajustarCorpo(level, alvo, false);
		}
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
	 * O jogador bateu nele durante a caçada. Nas duas primeiras vezes ele sai da caçada por uns segundos (some e
	 * reaparece longe, fora da tela, nunca nas costas, de preferência onde o jogador não o vê) e volta anunciado por
	 * passos; da terceira em diante, nada acontece. Bater deixou de encerrar a caçada.
	 *
	 * Até a alpha25 o recuo era de dez a catorze blocos, atrás de quem estava parado, e ele voltava a andar no tick
	 * seguinte: na sessão de 12/10/2026, dois segundos depois do golpe ele tocou o jogador. Bater não pode ser erro
	 * fatal; é a coisa que compra tempo (é o "volta para os dutos" do Alien).
	 */
	void aoSerFerido(ServerLevel level, ServerPlayer alvo) {
		if (this.golpes >= 2 || this.estagio == Estagio.FERIDO) {
			return;
		}
		this.golpes++;
		// No primeiro golpe, a voz sai de onde ele foi ferido, antes de ele sumir dali.
		if (this.golpes == 1) {
			this.voz(level, alvo, VozDaCacada.Momento.FERIDO);
		}
		BlockPos lugar = this.lugarForaDaTela(level, alvo, 16, 24, false, true);
		if (lugar != null) {
			this.h.getNavigation().stop();
			this.h.snapTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, this.h.getYRot(), 0.0F);
			this.aoMudarDeLugar(level);
			this.h.olharPara(alvo);
		}
		ApoioCaca.somDeRecuo(alvo, this.h);
		this.recargaAtalho = Math.max(this.recargaAtalho, 100);
		// Seis a nove segundos fora. O que ele estava fazendo fica para trás: um piscar marcado, a boca de um vão,
		// uma porta, o aviso de atravessar.
		this.feridoRestante = 120 + this.sorte.nextInt(61);
		this.piscarEm = -1;
		this.vaoEspera = 0;
		this.vaoBoca = null;
		this.porta = null;
		this.avisoBloco = null;
		this.barrado = 0;
		this.h.getNavigation().stop();
		double dist = Math.sqrt(this.h.distanceToSqr(alvo));
		this.menorDist = dist;
		this.saltou(Saltos.janela(this.feridoRestante, dist, this.passoAgora(level), ALCANCE_TOQUE));
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s ferido n=%d recuou=%s dist=%.1f fora=%dt costas=%s",
				this.h.getIdManifestacao(), this.golpes, lugar != null ? "sim" : "nao", dist, this.feridoRestante,
				this.nasCostasDe(alvo) ? "sim" : "nao"));
		this.mudar(level, alvo, Estagio.FERIDO, "FERIDO");
	}

	/**
	 * Ferido: parado, longe, olhando. Nos dois últimos segundos os passos dele anunciam a volta, e só então ele
	 * persegue de novo, sabendo mais ou menos onde o jogador está. Se na hora do golpe não havia lugar longe (um
	 * quarto fechado), ele tenta de novo a cada segundo em que o jogador não está olhando; se não conseguir, a
	 * espera de qualquer salto vem depois, para quem ficou perto dele também ter como sair.
	 */
	private void tickFerido(ServerLevel level, ServerPlayer alvo, boolean percebido, double dist) {
		this.h.getNavigation().stop();
		this.h.pararEOlhar();
		if (dist < Saltos.DISTANCIA_MINIMA && !percebido && this.feridoRestante % 20 == 0) {
			BlockPos lugar = this.lugarForaDaTela(level, alvo, Saltos.DISTANCIA_MINIMA, 24, false, true);
			if (lugar != null) {
				this.h.snapTo(lugar.getX() + 0.5, lugar.getY(), lugar.getZ() + 0.5, this.h.getYRot(), 0.0F);
				this.aoMudarDeLugar(level);
				this.h.olharPara(alvo);
				this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s ferido afastou dist=%.1f",
						this.h.getIdManifestacao(), Math.sqrt(this.h.distanceToSqr(alvo))));
			}
		}
		if (this.feridoRestante <= 40 && this.feridoRestante % 8 == 0) {
			ApoioCaca.passo(level, alvo, this.h);
		}
		if (--this.feridoRestante > 0) {
			return;
		}
		double agora = Math.sqrt(this.h.distanceToSqr(alvo));
		if (this.persegueDesde < 0) {
			this.persegueDesde = this.idade; // ferido ainda no aviso: a perseguição começa agora
		}
		this.recargaAtalho = Math.max(this.recargaAtalho, 200);
		this.busca.ouvirAcao(level, this.h, alvo.position(), 0.8, "VOLTOU_FERIDO");
		this.barrado = agora < Saltos.DISTANCIA_MINIMA ? Saltos.esperaDepois(agora, this.passoAgora(level), ALCANCE_TOQUE) : 0;
		this.naTela = 0;
		this.foraDaTela = 0;
		this.menorDist = agora;
		this.mudar(level, alvo, Estagio.PERSEGUE, String.format(Locale.ROOT, "VOLTOU dist=%.0f espera=%dt", agora, this.barrado));
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
				"CACA id=%s FIM motivo=%s duracao=%ds contato=%ds piscadas=%d atalhos=%d golpes=%d fingiu=%s vaos=%d",
				this.h.getIdManifestacao(), motivo, this.idade / 20, this.contato / 20, this.piscadas, this.atalhos,
				this.golpes, this.fingiu ? "sim" : "nao", this.vaos));
		this.h.sumir(level, false, motivo);
	}

	private void capturar(ServerLevel level, ServerPlayer alvo, String como) {
		if (this.encerrada) {
			return;
		}
		this.log(level, alvo, String.format(Locale.ROOT, "CACA id=%s CAPTURA como=%s duracao=%ds contato=%ds vaos=%d",
				this.h.getIdManifestacao(), como, this.idade / 20, this.contato / 20, this.vaos));
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
