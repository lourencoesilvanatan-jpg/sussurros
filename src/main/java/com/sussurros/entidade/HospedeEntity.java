package com.sussurros.entidade;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.Avesso;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.assombracao.Percepcao;
import com.sussurros.registro.ModSons;

/**
 * O Hóspede.
 *
 * Ele não aparece sozinho: quem decide quando, onde e como ele surge é o Diretor.
 * A criatura só executa o "papel" que recebeu (observar, espreitar, esperar ou caçar)
 * e avisa o Diretor do que aconteceu, para que ele aprenda.
 *
 * v0.4.2: dois cones de visão.
 *   PERCEBEU = está na sua tela (cone largo). Conta como "avistado", congela a caça.
 *   ENCAROU  = você está olhando direto para ele (cone estreito). Faz ele sumir mais rápido.
 */
public class HospedeEntity extends PathfinderMob {
	private static final EntityDataAccessor<Boolean> OBSERVANDO =
			SynchedEntityData.defineId(HospedeEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> MODO_VISUAL =
			SynchedEntityData.defineId(HospedeEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> AVISTADO_VISUAL =
			SynchedEntityData.defineId(HospedeEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> VARIANTE_VISUAL =
			SynchedEntityData.defineId(HospedeEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> OLHOS_VISUAIS =
			SynchedEntityData.defineId(HospedeEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SUMINDO =
			SynchedEntityData.defineId(HospedeEntity.class, EntityDataSerializers.BOOLEAN);

	/** Quando ele some na frente do jogador, dissolve por este tempo em vez de piscar para fora (ticks). */
	public static final int TICKS_FADE = 4;

	public enum Modo {
		OBSERVAR,  // fica parado, olhando. Some quando é visto por tempo demais.
		ESPREITAR, // quando você tira os olhos dele, muda de lugar (mais perto, outro lado, fica, ou some)
		ESPERAR,   // fica na borda da luz da vela, esperando ela apagar.
		CACAR,     // procura pela última posição conhecida e só se aproxima quando não está vendo.
		VULTO,     // parado a dezenas de blocos. Some um segundo depois de você mirar nele, ou se você chegar perto.
		AVESSO     // do outro lado: não some ao ser visto. Fica parado; depois vem andando devagar, olhado ou não.
	}

	/** Quem criou esta criatura. Fica gravado nela (0.4.1 guardava no jogador e confundia aparições). */
	public enum Origem {
		DIRETOR, OLHO, COMANDO
	}

	/** Distância padrão em que ele "não está mais lá" quando você se aproxima (modo OBSERVAR). */
	public static final double DIST_SUMIR_PADRAO = 12.0;

	/** Cone "percebeu" (~45°), cone seguro de tela (~55°) e cone "olhando direto" (~15°). */
	public static final double CONE_PERCEBEU = 0.70;
	public static final double CONE_TELA_SEGURA = 0.57;
	public static final double CONE_ENCAROU = 0.965;

	/** Vulto distante: cone em que o jogador está "mirando" nele (~30° do centro) e a distância em que deixa de ser um vulto. */
	public static final double CONE_MIROU = 0.866;
	public static final double DIST_SUMIR_VULTO = 36.0;

	/** O atributo de velocidade dele. A caçada converte blocos por segundo a partir daqui (ver Cacada). */
	public static final double VELOCIDADE_BASE = 0.3;

	/**
	 * Na caça ele se abaixa: a caixa de colisão cai de 3,0 para 1,9 blocos de altura. Com 3,0 ele não passava
	 * por nenhuma porta nem entrava em nenhum túnel de dois blocos, ou seja, não entrava em casa nenhuma.
	 */
	private static final EntityDimensions CORPO_NA_CACA = EntityDimensions.scalable(0.7F, 1.9F);

	/** Até esta ousadia ele ainda é tímido: basta olhar direto e desviar para ele não estar mais lá. */
	public static final int OUSADIA_FICA = 4;

	/**
	 * O primeiro contato aguenta ser olhado por mais tempo que um relance: cerca de um segundo de olhar direto,
	 * ou dois de canto. É a única aparição que precisa ser vista com certeza (ver PrimeiroContato).
	 */
	private static final int LIMITE_VISTO_NO_CONTATO = 36;

	/**
	 * Quantos ticks seguidos na mira do jogador, com luz para enxergá-lo, fazem o primeiro contato valer como
	 * visto (0,3 s). Uma virada de câmera que passa por ele dá dois ou três; quem parou para olhar dá muito mais.
	 */
	private static final int MIRA_DO_CONTATO = 6;

	@Nullable
	private ServerPlayer alvo;
	private Modo modo = Modo.OBSERVAR;
	private Origem origem = Origem.DIRETOR;
	@Nullable
	private Evento eventoOrigem;
	private double distSumir = DIST_SUMIR_PADRAO;
	private int vida = 400;      // ticks até sumir sozinho
	private int ousadia = 0;     // 0 a 10, cresce cada vez que você o vê
	private int vistoTicks = 0;  // quanto você está olhando para ele
	private int semAlvo = 0;
	private boolean jaAvistada = false;
	/** No modo AVESSO: já começou a vir. */
	private boolean avessoAnda = false;
	private double velocidade = 1.0;

	// Telemetria (0.4.2a-test): só para o log, nada disso muda o comportamento.
	private String idManifestacao = "M???";
	@Nullable
	private String motivoFim;      // por que a espreita mandou sumir (escrito pelo Diretor antes de devolver false)
	private int vezesPercebida = 0;
	/** Telemetria de exposição: ticks em que esteve na tela do alvo, e quantos deles perto (25 blocos) e iluminado (luz 8+). */
	private int ticksNaTela = 0;
	private int ticksPertoEClaro = 0;
	private int ticksSemPerceber = 0;
	private boolean jaEncarada = false;

	// Sumiço rápido (0.8.1): isto é comportamento, não telemetria.
	private boolean foiEncarado = false; // já foi olhado direto alguma vez
	private int foraDaTelaTicks = 0;     // há quantos ticks seguidos não está na tela
	private int naTelaTicks = 0;         // vulto: quantos ticks já passou na tela
	private int limiteNaTela = -1;       // vulto: quanto aguenta na borda da tela (sorteado na primeira vez)
	private int atrasoMirado = -1;       // vulto: ticks entre ser mirado e sumir (sorteado na hora)
	private int fadeRestante = 0;        // servidor: ticks até ser descartado, depois de começar a dissolver
	private int fadeInicioCliente = -1;  // cliente: idade (ticks) em que começou a dissolver
	private boolean avisaVigia = true;   // se esta manifestação é "sentida" quando olha de fora da tela
	private boolean contato = false;     // é a aparição do primeiro contato (ver PrimeiroContato)
	private int contatoMira = 0;         // contato: ticks seguidos na mira dele, com luz (cai um por tick fora dela)
	private boolean contatoVisto = false; // contato: já esteve na mira por tempo bastante para valer
	private int ticksNaMira = 0;         // contato, só para o log: quantos ticks ao todo esteve na mira, com luz

	// Espreita
	private int reposicoes = 0;
	private int maxReposicoes = 3;
	private int naoVistoTicks = 0;
	private int proximaEspera = 50;
	// Busca (0.8): memória transitória de investigação quando perde a visão.
	private final HospedeBusca busca = new HospedeBusca();
	/** A caçada em curso, do aviso ao desfecho. Só existe no modo CACAR. */
	@Nullable
	private Cacada cacada;

	public HospedeEntity(EntityType<? extends HospedeEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder criarAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 200)
				.add(Attributes.MOVEMENT_SPEED, VELOCIDADE_BASE)
				.add(Attributes.FOLLOW_RANGE, 64);
	}

	/** Chamado pelo Diretor logo depois de criar a criatura. */
	public void configurar(ServerPlayer alvo, Modo modo, int duracaoTicks, int ousadia, double velocidade) {
		this.alvo = alvo;
		this.setModo(modo);
		this.vida = duracaoTicks;
		this.ousadia = Math.max(0, Math.min(10, ousadia));
		this.velocidade = velocidade;
		this.olharPara(alvo);
		this.busca.inicializar(alvo, this.level().getGameTime());
		if (modo == Modo.CACAR) {
			this.cacada = new Cacada(this, this.busca, alvo, velocidade, true);
		}
	}

	/** Uma ação barulhenta do alvo (quebrar bloco, porta, baú) a até 16 blocos: na caça, entrega a posição. */
	public void ouvirAcao(Vec3 onde, String oQue) {
		if (this.modo == Modo.CACAR && this.cacada != null && this.level() instanceof ServerLevel level
				&& this.distanceToSqr(onde) <= 16 * 16) {
			this.busca.ouvirAcao(level, this, onde, 0.8, oQue);
		}
	}

	/** A Caixa de Música do alvo está tocando neste lugar: na caça, ele pode ir até ela. */
	public void ouvirIsca(Vec3 onde) {
		if (this.modo == Modo.CACAR && this.cacada != null && this.level() instanceof ServerLevel level) {
			this.cacada.iscar(level, onde);
		}
	}

	/** O quanto o alvo deve "sentir" esta criatura como perseguição, de 0 a 1 (ver Sentidos). */
	public float intensidadeDaCaca(ServerPlayer p, double dist) {
		if (this.modo == Modo.ESPERAR) {
			return 0.2F;
		}
		return this.modo == Modo.CACAR && this.cacada != null ? this.cacada.sentir(p, dist) : 0.0F;
	}

	/** Só para o log e para os testes: em que estágio a caçada está. */
	public String getEstagioDaCaca() {
		return this.cacada == null ? "-" : this.cacada.estagio().name();
	}

	/** Parado, virado para o alvo. A caçada usa quando ele está na tela, no aviso e atrás de uma porta. */
	void pararEOlhar() {
		this.ficarParadoOlhando();
	}

	void soltarOlhar() {
		this.setObservando(false);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		// No cliente o modo vem pelo dado sincronizado; no servidor, pelo campo. Durante a construção
		// nenhum dos dois existe ainda, e vale o corpo normal.
		boolean cacando = this.level().isClientSide()
				? this.entityData != null && this.entityData.get(MODO_VISUAL) == Modo.CACAR.ordinal()
				: this.modo == Modo.CACAR;
		return cacando ? CORPO_NA_CACA : super.getDefaultDimensions(pose);
	}

	public void definirOrigem(Origem origem, @Nullable Evento evento) {
		this.origem = origem;
		this.eventoOrigem = evento;
	}

	/** ID sequencial da sessão (M001, M002...), só para o log. */
	public void definirId(String id) {
		this.idManifestacao = id;
		// Aparência derivada do próprio ID: não consome RNG do Diretor e dá pequenas diferenças
		// entre manifestações sem criar uma nova decisão de gameplay.
		int h = id.hashCode() & 0x7fffffff;
		this.entityData.set(VARIANTE_VISUAL, h % 3);
		boolean eventoVisual = this.eventoOrigem == Evento.PRESENCA || this.eventoOrigem == Evento.ESPREITA
				|| this.eventoOrigem == Evento.CACA || this.eventoOrigem == Evento.ATRAS;
		this.entityData.set(OLHOS_VISUAIS, eventoVisual && h % 4 == 0);
		// Sete em cada dez manifestações são sentidas quando olham de fora da tela. As outras chegam sem
		// aviso nenhum: sensação que nunca falha vira radar. Também sai do ID, para não gastar sorteio.
		this.avisaVigia = (h / 7) % 10 < 7;
	}

	/** Esta é a aparição do primeiro contato: aguenta mais o olhar e sempre deixa cinza quando é vista. */
	public void marcarContato() {
		this.contato = true;
	}

	public boolean ehContato() {
		return this.contato;
	}

	/**
	 * O primeiro contato foi visto? A definição é uma só, aqui e no analisador de log: olhado direto, por uns
	 * instantes seguidos, num lugar com luz para enxergá-lo. Passar a mira por ele numa virada de câmera não
	 * conta; tê-lo no canto da tela não conta; olhar para onde ele está no breu não conta.
	 */
	public boolean contatoFoiVisto() {
		return this.contatoVisto;
	}

	/** Só para o log. */
	public int getTicksNaMira() {
		return this.ticksNaMira;
	}

	/** Já foi olhada direto alguma vez. */
	public boolean jaFoiEncarado() {
		return this.foiEncarado;
	}

	/** Esta manifestação dá ao jogador a sensação de estar sendo olhado? (ver Sentidos) */
	public boolean avisaVigia() {
		return this.avisaVigia;
	}

	/** Já começou a dissolver: para todos os efeitos, não está mais lá. */
	public boolean isSumindo() {
		return this.entityData.get(SUMINDO);
	}

	/** Só no cliente: de 1 (inteiro) a 0 (sumiu), para o desenho. */
	public float alfaVisual(float parcial) {
		if (this.fadeInicioCliente < 0) {
			return 1.0F;
		}
		float passado = (this.tickCount - this.fadeInicioCliente) + parcial;
		return Math.max(0.0F, 1.0F - passado / TICKS_FADE);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> dado) {
		super.onSyncedDataUpdated(dado);
		if (SUMINDO.equals(dado) && this.level().isClientSide() && this.isSumindo() && this.fadeInicioCliente < 0) {
			this.fadeInicioCliente = this.tickCount;
		}
		if (MODO_VISUAL.equals(dado)) {
			this.refreshDimensions();
		}
	}

	public String getIdManifestacao() {
		return this.idManifestacao;
	}

	/** Só para o log: o motivo do próximo sumiço pedido pela espreita. */
	public void definirMotivoFim(String motivo) {
		this.motivoFim = motivo;
	}

	public void definirDistanciaSumir(double distancia) {
		this.distSumir = distancia;
	}

	public void definirMaxReposicoes(int max) {
		this.maxReposicoes = max;
	}

	public Origem getOrigem() {
		return this.origem;
	}

	@Nullable
	public Evento getEventoOrigem() {
		return this.eventoOrigem;
	}

	/** Criada por comando: ser vista, ferida ou tocar em você não fica na memória. */
	public boolean ehTeste() {
		return this.origem == Origem.COMANDO;
	}

	/** Só para o log. */
	public int ticksVivo() {
		return this.tickCount;
	}

	/** Só para o log. */
	public int getTicksNaTela() {
		return this.ticksNaTela;
	}

	public int getTicksPertoEClaro() {
		return this.ticksPertoEClaro;
	}

	public int getVezesPercebida() {
		return this.vezesPercebida;
	}

	public int getReposicoes() {
		return this.reposicoes;
	}

	public int getMaxReposicoes() {
		return this.maxReposicoes;
	}

	/** Estado interno do Hóspede quando ele está procurando um alvo perdido. */
	public String getEstadoBusca() {
		return this.busca.estado().name();
	}

	public double getConfiancaBusca() {
		return this.busca.confianca();
	}

	public int getPontosBusca() {
		return this.busca.pontosVisitados();
	}

	public void contarReposicao() {
		this.reposicoes++;
	}

	@Nullable
	public ServerPlayer getAlvo() {
		return this.alvo;
	}

	public Modo getModo() {
		return this.modo;
	}

	// ===== Dado sincronizado com o cliente (para a animação da cabeça) =====

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OBSERVANDO, false);
		builder.define(MODO_VISUAL, Modo.OBSERVAR.ordinal());
		builder.define(AVISTADO_VISUAL, false);
		builder.define(VARIANTE_VISUAL, 0);
		builder.define(OLHOS_VISUAIS, false);
		builder.define(SUMINDO, false);
	}

	public boolean isObservando() {
		return this.entityData.get(OBSERVANDO);
	}

	private void setObservando(boolean valor) {
		if (this.isObservando() != valor) {
			this.entityData.set(OBSERVANDO, valor);
		}
	}

	/** Modo sincronizado com o cliente para a postura do modelo. */
	public int getModoVisual() {
		return this.entityData.get(MODO_VISUAL);
	}

	/** Depois da primeira vez que entra na tela, a silhueta muda discretamente no cliente. */
	public boolean foiAvistadoVisual() {
		return this.entityData.get(AVISTADO_VISUAL);
	}

	/** Pequena variação corporal determinística (0..2), só visual. */
	public int getVarianteVisual() {
		return this.entityData.get(VARIANTE_VISUAL);
	}

	/** Algumas manifestações carregam olhos pálidos; não são uma pista garantida. */
	public boolean temOlhosVisuais() {
		return this.entityData.get(OLHOS_VISUAIS);
	}

	private void setModo(Modo novo) {
		this.modo = novo;
		this.entityData.set(MODO_VISUAL, novo.ordinal());
		this.refreshDimensions();
	}

	// ===== Comportamento =====

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
			return;
		}

		// Dissolvendo: o Diretor já foi avisado do sumiço; falta só o corpo desaparecer para o jogador.
		if (this.fadeRestante > 0) {
			if (--this.fadeRestante == 0) {
				this.discard();
			}
			return;
		}

		// Sem alvo válido (por exemplo, depois de recarregar o mundo): some.
		if (this.alvo == null || this.alvo.isRemoved() || !this.alvo.isAlive()
				|| this.alvo.level() != level || this.distanceToSqr(this.alvo) > 110 * 110) {
			if (++this.semAlvo > 40) {
				if (this.alvo != null) {
					// Vivo e no jogo, mas a mais de 110 blocos ou em outra dimensão: fugiu no meio da caçada.
					if (this.modo == Modo.CACAR && !this.alvo.isRemoved() && this.alvo.isAlive()) {
						Diretor.cacadaFugiu(this.alvo, this, this.alvo.level() != level ? "TROCOU_DE_MUNDO" : "FUGIU_LONGE");
					}
					Diretor.registrarFimSemAlvo(this.alvo, this); // só log
					this.avisarFimDaCacada("SEM_ALVO");
				}
				this.discard();
			}
			return;
		}
		this.semAlvo = 0;

		if (--this.vida <= 0) {
			this.sumir(level, false, "TEMPO_ESGOTADO");
			return;
		}

		boolean naZonaCalma = Diretor.emZonaCalma(this.alvo, this.getX(), this.getY(), this.getZ());
		// As zonas de vela são do mundo normal: do outro lado não valem.
		if (naZonaCalma && this.modo != Modo.ESPERAR && this.modo != Modo.AVESSO) {
			this.sumir(level, true, "ZONA_CALMA");
			return;
		}

		boolean percebido = Diretor.estaVendo(this.alvo, this, Percepcao.conePercebeu(this.alvo));
		boolean encarado = percebido && Diretor.estaVendo(this.alvo, this, CONE_ENCAROU);
		this.foraDaTelaTicks = percebido ? 0 : this.foraDaTelaTicks + 1;
		if (percebido) {
			this.ticksNaTela++;
			if (this.distanceToSqr(this.alvo) < 25 * 25 && level.getMaxLocalRawBrightness(this.blockPosition().above()) >= 8) {
				this.ticksPertoEClaro++;
			}
		}
		if (encarado) {
			this.marcarEncarado();
		}
		if (this.contato) {
			if (encarado && Diretor.temLuzParaVer(level, this.blockPosition())) {
				this.ticksNaMira++;
				if (++this.contatoMira >= MIRA_DO_CONTATO) {
					this.contatoVisto = true;
				}
			} else {
				this.contatoMira = Math.max(0, this.contatoMira - 1);
			}
		}
		if (percebido && !this.jaAvistada) {
			this.jaAvistada = true;
			this.entityData.set(AVISTADO_VISUAL, true);
			Diretor.criaturaAvistada(this.alvo, this);
		}
		// Telemetria: quando entra na sua tela (depois de 1 s+ fora dela) e a primeira vez que é encarado. Só log.
		if (Diretor.telemetriaAtiva()) {
			if (percebido) {
				if (this.vezesPercebida == 0 || this.ticksSemPerceber >= 20) {
					this.vezesPercebida++;
					Diretor.registrarVisao(this.alvo, this, "PERCEBEU", this.vezesPercebida, this.ticksSemPerceber);
				}
				this.ticksSemPerceber = 0;
				if (encarado && !this.jaEncarada) {
					this.jaEncarada = true;
					Diretor.registrarVisao(this.alvo, this, "ENCAROU", 1, 0);
				}
			} else {
				this.ticksSemPerceber++;
			}
		}
		double distSqr = this.distanceToSqr(this.alvo);

		switch (this.modo) {
			case OBSERVAR -> this.tickObservar(level, percebido, encarado, distSqr);
			case ESPREITAR -> this.tickEspreitar(level, percebido, encarado, distSqr);
			case ESPERAR -> this.tickEsperar(level, encarado, distSqr);
			case CACAR -> this.tickCacar(level, percebido, distSqr);
			case VULTO -> this.tickVulto(level, percebido, distSqr);
			case AVESSO -> this.tickAvesso(level, distSqr);
		}
	}

	/**
	 * Ele não senta em nada. No jogo, um barco sem jogador no leme recolhe qualquer criatura mais estreita que
	 * ele, e um carrinho em movimento faz o mesmo; quem senta é devolvido ao assento todo tick, e nenhum estágio
	 * da caçada o tirava de lá: um barco no caminho dele encerrava a caçada de graça. (O Warden e o Wither
	 * recusam do mesmo jeito.) A tag cannot_be_pushed_onto_boats, nos dados, faz o barco só empurrá-lo.
	 */
	@Override
	protected boolean canRide(Entity veiculo) {
		return false;
	}

	/** Nem é preso por laço. O jogo só recusa o laço para quem é "inimigo" (Enemy), e ele não é. */
	@Override
	public boolean canBeLeashed() {
		return false;
	}

	/**
	 * A mais de 128 blocos de qualquer jogador o jogo apagaria a criatura sem avisar ninguém: a caçada de quem
	 * saiu voando acabava em silêncio, sem registro. Quem decide quando ela some é ela (ver "sem alvo válido").
	 */
	@Override
	public boolean removeWhenFarAway(double distanciaAoQuadrado) {
		return false;
	}

	/** Do outro lado ele deixa de ficar parado e vem. */
	public void virNoAvesso() {
		this.avessoAnda = true;
	}

	/**
	 * Do outro lado as regras de cá não valem: olhar não o faz sumir nem o segura. Parado, ele só olha.
	 * Quando vem, vem devagar (2,2 blocos por segundo: metade do passo do jogador), sem parar. Encostar é o fim
	 * da visita, e mais nada.
	 */
	private void tickAvesso(ServerLevel level, double distSqr) {
		if (distSqr < 2.3 * 2.3 && this.hasLineOfSight(this.alvo)) {
			ServerPlayer tocado = this.alvo;
			this.discard();
			Avesso.tocado(tocado);
			return;
		}
		if (!this.avessoAnda) {
			this.ficarParadoOlhando();
			return;
		}
		this.setObservando(false);
		this.olharPara(this.alvo);
		if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
			this.getNavigation().moveTo(this.alvo, 0.75);
		}
	}

	/** A primeira vez que o jogador olha direto para ele. Para o Diretor, encarar já é uma reação. */
	private void marcarEncarado() {
		if (!this.foiEncarado) {
			this.foiEncarado = true;
			Diretor.criaturaEncarada(this.alvo, this);
		}
	}

	/**
	 * Vulto distante: longe e curto, para ser negável ("será que eu vi alguma coisa?"). O jogador não pode
	 * conseguir focar nele. Some sem som, e não conta para a ousadia:
	 *  - 2 a 5 ticks depois de entrar no miolo da tela (o jogador está virando para ele);
	 *  - depois de 0,5 a 0,8 s na borda da tela, mesmo sem ser mirado;
	 *  - assim que sai da tela depois de ter sido visto: quando o jogador olha de novo, não há nada;
	 *  - antes de dar para ver de perto.
	 */
	private void tickVulto(ServerLevel level, boolean percebido, double distSqr) {
		this.ficarParadoOlhando();

		if (distSqr < DIST_SUMIR_VULTO * DIST_SUMIR_VULTO) {
			this.sumir(level, false, "CHEGOU_PERTO");
			return;
		}

		if (!percebido) {
			if (this.jaAvistada && this.foraDaTelaTicks >= 3) {
				this.sumir(level, false, "VULTO_DESVIOU");
			}
			return;
		}

		if (this.limiteNaTela < 0) {
			this.limiteNaTela = 10 + this.random.nextInt(7);
		}
		if (Diretor.estaVendo(this.alvo, this, CONE_MIROU)) {
			this.marcarEncarado();
			if (this.atrasoMirado < 0) {
				this.atrasoMirado = 2 + this.random.nextInt(4);
			}
			if (--this.atrasoMirado <= 0) {
				this.sumir(level, false, "VULTO_MIRADO");
				return;
			}
		}
		if (++this.naTelaTicks > this.limiteNaTela) {
			this.sumir(level, false, "VULTO_VISTO");
		}
	}

	private void ficarParadoOlhando() {
		this.getNavigation().stop();
		this.setObservando(true);
		this.olharPara(this.alvo);
	}

	/** Conta o "ser visto": olhar direto pesa o dobro da visão de canto. Devolve true se passou do limite. */
	private boolean contarVisto(boolean percebido, boolean encarado, int limite) {
		if (percebido) {
			this.vistoTicks += encarado ? 2 : 1;
			return this.vistoTicks > limite;
		}
		this.vistoTicks = Math.max(0, this.vistoTicks - 1);
		return false;
	}

	private void tickObservar(ServerLevel level, boolean percebido, boolean encarado, double distSqr) {
		this.ficarParadoOlhando();

		// Se você chegar perto, ele não está mais lá.
		if (distSqr < this.distSumir * this.distSumir) {
			this.sumir(level, true, "CHEGOU_PERTO");
			return;
		}

		// Enquanto ele é tímido, basta olhar direto e desviar: quando o jogador olha de novo, ele não está
		// mais lá. Sumir fora da tela é o que deixa a dúvida ("será que eu vi?").
		// O primeiro contato só entra aqui depois de ter sido visto de verdade: uma virada de câmera que passa
		// a mira por ele durante um tick não o manda embora (ele sumia sem ninguém ter visto, e contava).
		if (this.foiEncarado && !percebido && this.foraDaTelaTicks >= 4 && this.ousadia < OUSADIA_FICA
				&& (!this.contato || this.contatoVisto)) {
			Diretor.criaturaFoiVista(this.alvo, this);
			this.sumir(level, false, "SUMIU_NO_DESVIO");
			return;
		}

		// Quanto mais ousado, mais tempo ele aguenta ser visto. No começo é um relance: 0,3 s olhando direto
		// ou 0,6 s de canto (era 0,75 s e 1,5 s, e dava para focar nele). Com ousadia 10 chega a 1,8 s.
		if (this.contarVisto(percebido, encarado, this.contato ? LIMITE_VISTO_NO_CONTATO : 12 + this.ousadia * 6)) {
			// Quem o teve no canto da tela e pôs a mira nele bem na hora em que ele dissolve viu: a dissolução
			// acontece no meio da tela dele, mesmo que a mira não tenha chegado a completar os seus instantes.
			if (this.contato && encarado && Diretor.temLuzParaVer(level, this.blockPosition())) {
				this.contatoVisto = true;
			}
			// O contato que dissolveu só de canto de olho não foi visto: não soma pressão nem ousadia, e
			// continua devendo (ver PrimeiroContato.aoSumir).
			if (!this.contato || this.contatoVisto) {
				Diretor.criaturaFoiVista(this.alvo, this);
			}
			this.sumir(level, true, "VISTO_DEMAIS");
		}
	}

	/**
	 * Espreita: quando você tira os olhos dele, depois de um tempo sorteado ele pode chegar mais perto,
	 * trocar de lado, ficar onde está ou sumir. Quem escolhe o lugar é o Diretor (cobertura, rastro).
	 */
	private void tickEspreitar(ServerLevel level, boolean percebido, boolean encarado, double distSqr) {
		this.ficarParadoOlhando();

		if (distSqr < this.distSumir * this.distSumir) {
			this.sumir(level, true, "CHEGOU_PERTO");
			return;
		}

		if (percebido) {
			this.naoVistoTicks = 0;
			if (this.contarVisto(true, encarado, 60 + this.ousadia * 6)) {
				Diretor.criaturaFoiVista(this.alvo, this);
				this.sumir(level, true, "VISTO_DEMAIS");
			}
			return;
		}
		this.contarVisto(false, false, 0);

		// Antes de ser visto pela primeira vez, dá tempo de você notar (12 s); depois, 1,5-5 s sorteados.
		int espera = this.jaAvistada ? this.proximaEspera : 240;
		if (++this.naoVistoTicks >= espera) {
			this.naoVistoTicks = 0;
			this.proximaEspera = 30 + level.getRandom().nextInt(70);
			if (!Diretor.reposicionarEspreita(this.alvo, this)) {
				this.sumir(level, false, this.motivoFim != null ? this.motivoFim : "ESPREITA"); // sem som, sem aviso
			}
		}
	}

	private void tickEsperar(ServerLevel level, boolean encarado, double distSqr) {
		this.ficarParadoOlhando();

		if (distSqr < 6 * 6) {
			this.sumir(level, true, "CHEGOU_PERTO");
			return;
		}

		// A vela apagou: agora ele vem.
		if (!Diretor.temZonaCalma(this.alvo)) {
			this.setModo(Modo.CACAR);
			this.vida = Math.max(this.vida, 20 * 130);
			this.vistoTicks = 0;
			this.cacada = new Cacada(this, this.busca, this.alvo, this.velocidade, false);
			return;
		}

		if (encarado && ++this.vistoTicks > 200) {
			Diretor.criaturaFoiVista(this.alvo, this);
			this.sumir(level, true, "VISTO_DEMAIS");
		}
	}

	/**
	 * A caça inteira mora em {@link Cacada}: aviso, perseguição, busca, atalho, atravessar e desfecho.
	 * Aqui só se garante que ela existe (uma criatura criada pelo ovo, por exemplo, não passa por configurar).
	 */
	private void tickCacar(ServerLevel level, boolean percebido, double distSqr) {
		if (this.cacada == null) {
			this.cacada = new Cacada(this, this.busca, this.alvo, this.velocidade, false);
		}
		this.cacada.tick(level, this.alvo, percebido, distSqr);
	}

	/** A caçada acabou (ele sumiu, por qualquer motivo): o Diretor é avisado uma vez só. */
	private void avisarFimDaCacada(String motivo) {
		if (this.cacada != null && !this.cacada.encerrada() && this.alvo != null) {
			this.cacada.marcarEncerrada();
			Diretor.cacadaTerminou(this.alvo, this, motivo);
		}
	}

	/** Vira o corpo e a cabeça para a entidade. Público porque o Diretor usa depois de reposicionar. */
	public void olharPara(Entity e) {
		double dx = e.getX() - this.getX();
		double dz = e.getZ() - this.getZ();
		float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.getLookControl().setLookAt(e, 30.0F, 30.0F);
	}

	/** Some sem deixar rastro. */
	public void sumir(ServerLevel level, boolean comSom) {
		this.sumir(level, comSom, "OUTRO");
	}

	/** Igual, com o motivo para o log (0.4.2a-test). O motivo não muda nada do comportamento. */
	public void sumir(ServerLevel level, boolean comSom, String motivo) {
		if (this.isRemoved() || this.fadeRestante > 0) {
			return;
		}
		if (comSom) {
			// O teleporte do Enderman fazia a entidade parecer um mob conhecido. Sumicos normais agora
			// sao quase silenciosos; ferir ou expulsar pela zona calma ainda deixa uma assinatura discreta.
			if ("FERIDO".equals(motivo)) {
				ModSons.tocar(level, this.getX(), this.getY() + 1.0, this.getZ(), ModSons.Som.GRAVE, 0.45F, 1.05F);
			} else if ("ZONA_CALMA".equals(motivo)) {
				ModSons.tocar(level, this.getX(), this.getY() + 1.0, this.getZ(), ModSons.Som.PANO, 0.30F, 0.95F);
			} else if (("CHEGOU_PERTO".equals(motivo) || "VISTO_DEMAIS".equals(motivo)
					|| "ENCARADO_DEMAIS".equals(motivo)) && this.random.nextBoolean()) {
				// Só em metade das vezes: som que sempre confirma o sumiço tira a dúvida de "eu vi mesmo?".
				ModSons.tocar(level, this.getX(), this.getY() + 1.4, this.getZ(), ModSons.Som.PANO, 0.18F, 0.82F);
			}
		}
		if (this.alvo != null) {
			this.avisarFimDaCacada(motivo);
			Diretor.criaturaSumiu(this.alvo, this, motivo);
		}
		// Na frente do jogador ele dissolve em quatro ticks; "piscar para fora" parecia um mob sendo apagado.
		// O vulto distante continua sumindo de um quadro para o outro: é o que deixa a dúvida.
		if (this.modo != Modo.VULTO && this.foraDaTelaTicks == 0 && this.jaAvistada) {
			this.fadeRestante = TICKS_FADE;
			this.entityData.set(SUMINDO, true);
			this.getNavigation().stop();
			this.setDeltaMovement(0, 0, 0);
			return;
		}
		this.discard();
	}

	// Ele não morre. Ferir só o espanta (e ele lembra disso).
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Só conta se for o jogador que ele estava assombrando, e se não for uma criatura de teste
		// (bater num Hóspede do ovo gerador ou de comando não deixa a criatura de verdade mais rápida).
		if (source.getEntity() instanceof ServerPlayer jogador && jogador == this.alvo && !this.ehTeste()) {
			Diretor.criaturaFerida(jogador);
		}
		// Na caça, bater não o manda embora: nas duas primeiras vezes ele recua, depois nem isso.
		if (this.modo == Modo.CACAR && this.cacada != null && this.alvo != null && this.fadeRestante == 0) {
			if (source.getEntity() == this.alvo) {
				this.cacada.aoSerFerido(level, this.alvo);
			}
			return false;
		}
		this.sumir(level, true, "FERIDO");
		return false;
	}
}
