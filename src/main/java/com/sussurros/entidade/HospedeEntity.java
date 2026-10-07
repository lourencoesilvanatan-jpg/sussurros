package com.sussurros.entidade;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
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

	public enum Modo {
		OBSERVAR,  // fica parado, olhando. Some quando é visto por tempo demais.
		ESPREITAR, // quando você tira os olhos dele, muda de lugar (mais perto, outro lado, fica, ou some)
		ESPERAR,   // fica na borda da luz da vela, esperando ela apagar.
		CACAR      // procura pela última posição conhecida e só se aproxima quando não está vendo.
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
	private double velocidade = 1.0;

	// Telemetria (0.4.2a-test): só para o log, nada disso muda o comportamento.
	private String idManifestacao = "M???";
	@Nullable
	private String motivoFim;      // por que a espreita mandou sumir (escrito pelo Diretor antes de devolver false)
	private int vezesPercebida = 0;
	private int ticksSemPerceber = 0;
	private boolean jaEncarada = false;

	// Espreita
	private int reposicoes = 0;
	private int maxReposicoes = 3;
	private int naoVistoTicks = 0;
	private int proximaEspera = 50;
	// Busca (0.8): memória transitória de investigação quando perde a visão.
	private final HospedeBusca busca = new HospedeBusca();

	public HospedeEntity(EntityType<? extends HospedeEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder criarAtributos() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 200)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
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
	}

	// ===== Comportamento =====

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) {
			return;
		}

		// Sem alvo válido (por exemplo, depois de recarregar o mundo): some.
		if (this.alvo == null || this.alvo.isRemoved() || !this.alvo.isAlive()
				|| this.alvo.level() != level || this.distanceToSqr(this.alvo) > 110 * 110) {
			if (++this.semAlvo > 40) {
				if (this.alvo != null) {
					Diretor.registrarFimSemAlvo(this.alvo, this); // só log
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
		if (naZonaCalma && this.modo != Modo.ESPERAR) {
			this.sumir(level, true, "ZONA_CALMA");
			return;
		}

		boolean percebido = Diretor.estaVendo(this.alvo, this, CONE_PERCEBEU);
		boolean encarado = percebido && Diretor.estaVendo(this.alvo, this, CONE_ENCAROU);
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
			case CACAR -> this.tickCacar(level, percebido, encarado, distSqr);
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

		// Quanto mais ousado, mais tempo ele aguenta ser visto (1,5 s olhando direto, 3 s de canto).
		if (this.contarVisto(percebido, encarado, 30 + this.ousadia * 4)) {
			Diretor.criaturaFoiVista(this.alvo, this);
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
			this.vida = Math.max(this.vida, 900);
			this.vistoTicks = 0;
			return;
		}

		if (encarado && ++this.vistoTicks > 200) {
			Diretor.criaturaFoiVista(this.alvo, this);
			this.sumir(level, true, "VISTO_DEMAIS");
		}
	}

	private void tickCacar(ServerLevel level, boolean percebido, boolean encarado, double distSqr) {
		if (percebido) {
			// Congela enquanto está na sua tela e atualiza a última posição conhecida.
			this.ficarParadoOlhando();
			this.busca.ouvirMovimento(level, this, this.alvo, true);
			// Encarar por tempo suficiente faz ele desistir... desta vez.
			if (encarado && ++this.vistoTicks > 40 + this.ousadia * 6) {
				Diretor.criaturaFoiVista(this.alvo, this);
				this.sumir(level, true, "ENCARADO_DEMAIS");
			}
			return;
		}

		this.setObservando(false);
		this.vistoTicks = Math.max(0, this.vistoTicks - 2);
		// A partir daqui ele não recebe mais a posição atual gratuitamente. Primeiro tenta
		// ouvir movimento e, se não ouvir, procura em torno da última posição conhecida.
		this.busca.ouvirMovimento(level, this, this.alvo, false);
		if (this.tickCount % 5 == 0) {
			boolean desistiu = this.busca.tick(level, this, this.alvo);
			if (desistiu) {
				this.sumir(level, false, "PERDEU_RASTRO");
				return;
			}
		}

		// Apaga a luz por onde passa.
		if (this.tickCount % 30 == 0) {
			this.apagarTochaProxima(level);
		}

		if (distSqr < 2.4 * 2.4) {
			this.tocar(level);
		}
	}

	private void tocar(ServerLevel level) {
		ServerPlayer p = this.alvo;
		p.hurtServer(level, level.damageSources().mobAttack(this), 5.0F);
		p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160));
		p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
		// Nada de assinatura do shrieker/warden: o pico sonoro agora pertence ao proprio mod.
		ModSons.tocar(level, p.getX(), p.getY() + 1.0, p.getZ(), ModSons.Som.GRAVE, 0.95F, 0.72F);
		ModSons.tocar(level, this.getX(), this.getY() + 1.8, this.getZ(), ModSons.Som.RESPIRACAO, 0.75F, 0.82F);
		Diretor.criaturaTocou(p, this);
		this.sumir(level, false, "TOCOU");
	}

	private void apagarTochaProxima(ServerLevel level) {
		BlockPos centro = this.blockPosition();
		for (int dx = -4; dx <= 4; dx++) {
			for (int dy = -2; dy <= 3; dy++) {
				for (int dz = -4; dz <= 4; dz++) {
					BlockPos pos = centro.offset(dx, dy, dz);
					BlockState estado = level.getBlockState(pos);
					if (estado.is(Blocks.TORCH) || estado.is(Blocks.WALL_TORCH)) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
						level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
								SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 0.6F);
						return;
					}
				}
			}
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
		if (this.isRemoved()) {
			return;
		}
		if (comSom) {
			// O teleporte do Enderman fazia a entidade parecer um mob conhecido. Sumicos normais agora
			// sao quase silenciosos; ferir ou expulsar pela zona calma ainda deixa uma assinatura discreta.
			if ("FERIDO".equals(motivo)) {
				ModSons.tocar(level, this.getX(), this.getY() + 1.0, this.getZ(), ModSons.Som.GRAVE, 0.45F, 1.05F);
			} else if ("ZONA_CALMA".equals(motivo)) {
				ModSons.tocar(level, this.getX(), this.getY() + 1.0, this.getZ(), ModSons.Som.PANO, 0.30F, 0.95F);
			} else if ("CHEGOU_PERTO".equals(motivo) || "VISTO_DEMAIS".equals(motivo)
					|| "ENCARADO_DEMAIS".equals(motivo)) {
				ModSons.tocar(level, this.getX(), this.getY() + 1.4, this.getZ(), ModSons.Som.PANO, 0.18F, 0.82F);
			}
		}
		if (this.alvo != null) {
			Diretor.criaturaSumiu(this.alvo, this, motivo);
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
		this.sumir(level, true, "FERIDO");
		return false;
	}
}
