package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import com.sussurros.Sussurros;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.assombracao.diretor.Agenda;
import com.sussurros.assombracao.selecao.Seletor;
import com.sussurros.rede.PacoteEfeito;
import com.sussurros.rede.PacoteSentidos;
import com.sussurros.rede.Rede;
import com.sussurros.registro.ModEntidades;
import com.sussurros.registro.ModItems;
import com.sussurros.registro.ModSons;

/**
 * O Diretor é o "cérebro" da assombração (v0.4: "O Diretor te conhece").
 *
 * Inspirado no diretor de Alien: Isolation: um cérebro que sabe tudo e dosa o ritmo,
 * separado da criatura. A cada segundo, para cada jogador, ele:
 *  1. mede inquietação, hábitos e o perfil do jogador (traços lentos)
 *  2. calcula a vulnerabilidade do momento (V) e atualiza seu próprio estado
 *     (CALMO, OBSERVANDO, TESTANDO, ESCALANDO, AMEACANDO, RECUANDO)
 *  3. escolhe se algo acontece, o quê e quando, evitando repetir padrões
 *  4. lê a reação comparando com o comportamento ANTERIOR (baseline), considerando se o
 *     jogador provavelmente PERCEBEU o acontecimento, e aprende em duas memórias (curta e longa)
 *
 * ATENÇÃO: este arquivo contém spoilers de tudo que o mod faz.
 */
public final class Diretor {
	/** Segundos de assombração necessários para cada fase. */
	public static final int[] LIMIAR_FASE = {0, 600, 1800, 3300, 5400};

	/** Intervalo entre eventos (em segundos) por fase: {mínimo, máximo}. */
	private static final int[][] INTERVALO = {{0, 0}, {110, 230}, {85, 185}, {60, 150}, {45, 110}};

	/** Piso: depois de tanto tempo sem nada, algo bem fraco acontece (segurança nunca desliga o mod). */
	private static final int[] PISO = {0, 420, 420, 480, 480};

	/** Olho Sussurrante: por quantos segundos um uso conta como "recente", e a recarga depois de ele chamar uma aparição. */
	private static final int OLHO_JANELA = 300;
	private static final int OLHO_RECARGA = 300;

	/**
	 * Chance de o mundo emudecer quando ele aparece, e quando é só um sinal falso. Nenhuma das duas é 100%
	 * ou 0% de propósito: aviso que nunca falha vira dica, e os jogadores aprendem a ler.
	 */
	private static final float CHANCE_SILENCIO_APARICAO = 0.7F;
	private static final float CHANCE_SILENCIO_FALSO = 0.3F;

	/**
	 * Vulto distante: faixa de distância em blocos. O teto fica abaixo do limite em que o jogo ainda desenha
	 * uma entidade deste tamanho com a "distância de entidades" em 100% (cerca de 94 blocos).
	 */
	private static final double VULTO_DIST_MIN = 48;
	private static final double VULTO_DIST_MAX = 80;

	/** De dia, a céu aberto e sem cobertura, ele não nasce mais perto do que isto (blocos). */
	private static final double DIST_MIN_EXPOSTO = 25;

	/** Uma aparição num ponto do Rastro só vale se estiver, no máximo, tantos blocos acima ou abaixo do jogador. */
	private static final double DESNIVEL_MAX_APARICAO = 12;

	private static final double DECAIMENTO_CURTO = Math.exp(-1.0 / 1200.0); // tau = 20 min de jogo

	/** Confiança mínima de uma reação para o Diretor concluir "isso funciona" e escalar (0.4: 0,5). */
	private static final double CONFIANCA_ESCALAR = 0.45;

	/** Vulnerabilidade a partir da qual ele não espera o cronômetro: "o momento é agora". */
	private static final int V_OPORTUNIDADE = 60;

	/** Quantos eventos ele tenta no mesmo segundo quando o sorteado não cabe no mundo. */
	private static final int MAX_TENTATIVAS_POR_SEGUNDO = 3;

	/** Uma reação forte vale por este tempo mesmo que o estado mude no meio (segundos). */
	private static final int MEMORIA_REACAO = 120;

	/** Só conta como "ele ignorou" um evento com pelo menos esta chance de ter sido percebido. */
	private static final double OBS_INDIFERENCA = 0.7;

	/** Quantas vezes a Caixa de Música precisa tocar para ele aprender a cantiga e passar a assobiá-la. */
	static final int CANTIGA_APRENDIDA = 3;

	/** Obsessão necessária (0-100) para ele montar a sequência de ameaça (v0.4.2). */
	private static final double OBSESSAO_AMEACA = 60;

	/** Sem ponto do Rastro por perto, o lugar de uma ação antiga só serve de fonte de som até esta distância (blocos). */
	static final double ALCANCE_ACAO_ANTIGA = 30;

	private static final Map<UUID, EstadoJogador> ESTADOS = new HashMap<>();

	// Telemetria (0.4.2a-test): contadores simples. NUNCA números aleatórios aqui (mudaria as decisões).
	private static int contadorManifestacao;
	private static int contadorCena;

	private record SinalResultado(Vec3 fonte, double observabilidade) {
	}

	private enum Modo {
		NORMAL, PISO, FRACO
	}

	private Diretor() {
	}

	// =====================================================================
	// Registro
	// =====================================================================

	public static void inicializar() {
		ServerTickEvents.END_LEVEL_TICK.register(Diretor::tickMundo);

		// Ele escuta o que você quebra... para imitar depois. E percebe quando você está ocupado.
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer jogador) {
				EstadoJogador e = estado(jogador);
				if (level instanceof ServerLevel nivel && nivel == nivel.getServer().getLevel(Level.OVERWORLD)) {
					registrarAcao(e, EstadoJogador.TipoAcao.QUEBRA, state.getSoundType().getBreakSound(), pos, level.getGameTime() / 20);
				}
				e.quebrasRecentes.addLast(level.getGameTime());
				while (e.quebrasRecentes.size() > 16) {
					e.quebrasRecentes.removeFirst();
				}
				avisarCacador(e, Vec3.atCenterOf(pos), "QUEBRA");
			}
		});

		// Clicar numa cama (mesmo de dia, só para marcar o respawn) já diz a ele onde é a sua casa.
		// Na 0.4 só dormir contava: quem só marcava o respawn ficava "sem casa" para sempre.
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ServerPlayer jogador && world instanceof ServerLevel nivel
					&& nivel == nivel.getServer().getLevel(Level.OVERWORLD)) {
				BlockPos clicado = hitResult.getBlockPos();
				BlockState bloco = nivel.getBlockState(clicado);
				if (bloco.getBlock() instanceof BedBlock) {
					registrarCasa(jogador, clicado);
				} else if (bloco.getBlock() instanceof DoorBlock && !bloco.is(Blocks.IRON_DOOR)) {
					registrarPorta(jogador, nivel, clicado); // v0.4.2: a porta mais usada vira âncora
					avisarCacador(estado(jogador), Vec3.atCenterOf(clicado), "PORTA");
				} else if (nivel.getBlockEntity(clicado) instanceof Container) {
					avisarCacador(estado(jogador), Vec3.atCenterOf(clicado), "BAU");
				}
			}
			return InteractionResult.PASS;
		});

		EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
			if (entity instanceof ServerPlayer jogador) {
				aoDormir(jogador, sleepingPos);
			}
		});

		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
			if (entity instanceof ServerPlayer jogador) {
				aoAcordar(jogador, sleepingPos);
			}
		});

		// Ele lembra onde você morreu.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer jogador && jogador.level() == jogador.level().getServer().getLevel(Level.OVERWORLD)) {
				Memoria m = Memoria.de(jogador);
				m.set(Memoria.TEM_MORTE, 1);
				m.set(Memoria.MORTE_X, jogador.getBlockX());
				m.set(Memoria.MORTE_Y, jogador.getBlockY());
				m.set(Memoria.MORTE_Z, jogador.getBlockZ());
				m.salvar();
			}
		});

		// Renascer devolve os atributos ao padrão; a marca da captura tem de ser reposta.
		ServerPlayerEvents.AFTER_RESPAWN.register((antigo, novo, vivo) -> Captura.repor(novo));

		// Machucado recentemente? Não é hora (anti-frustração).
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer jogador) {
				estado(jogador).ultimoDanoTick = jogador.level().getGameTime();
			}
		});

		// E escuta o que você diz.
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, boundChatType) -> {
			String texto = message.signedContent().trim();
			if (texto.length() >= 3 && texto.length() <= 60) {
				EstadoJogador e = estado(sender);
				e.falas.addLast(texto);
				while (e.falas.size() > 4) {
					e.falas.removeFirst();
				}
			}
		});

		// Antes de o mundo ser salvo: desfaz o que era temporário (tocha apagada, marca no caminho).
		// No SERVER_STOPPED já é tarde: o mundo já foi gravado com a alteração.
		ServerLifecycleEvents.SERVER_STOPPING.register(AlteracoesTemporarias::restaurarTudo);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ESTADOS.clear();
			Percepcao.limpar();
			Agenda.limpar();
			Atmosfera.limpar();
			contadorManifestacao = 0;
			contadorCena = 0;
		});
	}

	private static EstadoJogador estado(ServerPlayer p) {
		return ESTADOS.computeIfAbsent(p.getUUID(), id -> new EstadoJogador());
	}

	// Pontos de integração internos da 0.6: usados por Atmosfera/Estruturas e pelos comandos de teste.
	static EstadoJogador estadoParaTeste(ServerPlayer p) {
		return estado(p);
	}


	/** O que o cliente deste jogador foi mandado sentir por último (para os testes e para o comando memoria). */
	public static PacoteSentidos sentidos(ServerPlayer p) {
		return estado(p).sentidos;
	}

	/** A criatura que assombra este jogador agora, se houver (para os testes). */
	@Nullable
	public static HospedeEntity criatura(ServerPlayer p) {
		HospedeEntity h = estado(p).criatura;
		return h == null || h.isRemoved() ? null : h;
	}

	/**
	 * Começa uma caçada agora, com o aviso e tudo. contaNaMemoria=false é o teste comum (nada fica gravado);
	 * true existe para os testes automáticos conferirem o que só acontece numa caçada de verdade (a marca).
	 */
	public static boolean cacadaParaTeste(ServerPlayer p, boolean contaNaMemoria) {
		ServerLevel level = p.level();
		EstadoJogador e = estado(p);
		if (e.criatura != null && !e.criatura.isRemoved()) {
			e.criatura.sumir(level, false, "SUBSTITUIDA_POR_COMANDO");
		}
		PedidoManifestacao pedido = contaNaMemoria
				? PedidoManifestacao.doDiretor(Evento.CACA)
				: PedidoManifestacao.deComando(Evento.CACA);
		boolean ok = invocar(level, p, e, HospedeEntity.Modo.CACAR, 150, 180, 18, 26, 20 * 130, 1.0, true, pedido);
		if (ok) {
			prenunciar(level, p, e, level.getGameTime(), level.getRandom(), true);
		}
		return ok;
	}

	public static String testarSentidos(ServerPlayer p, float peso, float vigia, float caca, float neblina, int flags) {
		return Sentidos.forcar(p, peso, vigia, caca, neblina, flags, 120);
	}

	public static String testarEfeito(ServerPlayer p, PacoteEfeito.Tipo tipo, int ticks) {
		Rede.efeito(p, tipo, ticks, 1.0F);
		return "Efeito " + tipo + " por " + ticks + " ticks.";
	}

	// Compatibilidade interna: mantém as chamadas curtas durante a refatoração.
	static void agendar(ServerLevel level, int atrasoTicks, Runnable acao) {
		Agenda.agendar(level, atrasoTicks, acao);
	}

	// =====================================================================
	// Tick
	// =====================================================================

	private static void tickMundo(ServerLevel level) {
		if (level != level.getServer().getLevel(Level.OVERWORLD)) {
			return;
		}
		long tick = level.getGameTime();
		// Atualizações curtas da Atmosfera: restaura blocos temporários e mantém animais olhando.
		Atmosfera.tickRapido(level, tick);

		Agenda.tick(level);

		// Leitura fina das reações (a cada 0,25 s).
		if (tick % 5 == 0) {
			for (ServerPlayer jogador : level.getPlayers(j -> !j.isSpectator())) {
				EstadoJogador e = estado(jogador);
				if (e.leitura.amostrar(jogador, tick)) {
					aoSaltar(jogador, e, tick / 20);
				}
				if (e.leitura.pronta(tick)) {
					concluirLeitura(level, jogador, e, tick / 20);
				}
			}
		}

		if (tick % 20 != 0) {
			return;
		}
		long seg = tick / 20;
		for (ServerPlayer jogador : level.getPlayers(j -> !j.isSpectator())) {
			try {
				segundo(level, jogador, seg, tick);
			} catch (Exception ex) {
				Sussurros.LOGGER.error("Erro no Diretor", ex);
			}
		}
	}

	/**
	 * Teleporte, respawn ou volta de outra dimensão. A leitura em andamento já foi cancelada pela própria
	 * Leitura; aqui ele esquece o caminho, que deixou de descrever "por onde você veio".
	 */
	private static void aoSaltar(ServerPlayer p, EstadoJogador e, long seg) {
		e.rastro.limpar();
		e.temUltimo = false;
		Depuracao.log(p, seg, "SALTO: teleporte, respawn ou portal; leitura cancelada e Rastro esquecido");
	}

	private static void segundo(ServerLevel level, ServerPlayer p, long seg, long tick) {
		Memoria m = Memoria.de(p);
		EstadoJogador e = estado(p);
		RandomSource rnd = level.getRandom();

		if (e.ultimoEventoSeg < 0) {
			e.ultimoEventoSeg = seg;
		}
		if (e.estadoDesde < 0) {
			e.estadoDesde = seg;
			e.duracaoEstado = sortearDuracao(EstadoDiretor.CALMO, rnd);
		}

		// --- Zona da vela acabou? ---
		if (e.zonaAteTick > 0 && tick >= e.zonaAteTick) {
			e.zonaAteTick = -1;
			p.sendOverlayMessage(Component.translatable("message.sussurros.vela.apagou").withStyle(s -> s.withColor(0x8A8A8A).withItalic(true)));
			level.playSound(null, e.zonaX, e.zonaY, e.zonaZ, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.8F);
		}

		// --- Isca Pálida: persiste por pouco tempo e deixa um vestígio visual mínimo no chão. ---
		if (e.iscaAtiva) {
			if (tick >= e.iscaAteTick) {
				e.iscaAtiva = false;
				Depuracao.log(p, seg, "ISCA expirou pos=" + pos(e.iscaX, e.iscaY, e.iscaZ));
			} else if (seg % 5 == 0) {
				level.sendParticles(ParticleTypes.ASH, e.iscaX, e.iscaY + 0.08, e.iscaZ, 3, 0.28, 0.02, 0.28, 0.001);
			}
		}

		// --- Movimento e "olhadas para trás" ---
		if (e.temUltimo) {
			double mx = p.getX() - e.ultX;
			double mz = p.getZ() - e.ultZ;
			e.velocidade = Math.sqrt(mx * mx + mz * mz);
			float giro = Math.abs(diferencaAngulo(p.getYRot(), e.ultYaw));
			if (giro > 120 && seg - e.ultimoEventoSeg > 8) {
				m.add(Memoria.OLHADAS, 1);
				e.minOlhadas++;
			}
		}
		e.temUltimo = true;
		e.ultX = p.getX();
		e.ultZ = p.getZ();
		e.ultYaw = p.getYRot();

		// --- Rastro: por onde você andou (v0.4.2) ---
		if (seg % 2 == 0 && p.onGround()) {
			e.rastro.registrar(p.getX(), p.getY(), p.getZ(), seg);
		}

		// --- Luz, escuro, inquietação ---
		BlockPos pos = p.blockPosition();
		int luzBloco = level.getBrightness(LightLayer.BLOCK, pos);
		int luzCeu = level.getBrightness(LightLayer.SKY, pos);
		boolean noite = ehNoite(level);
		int luz = Math.max(luzBloco, noite ? luzCeu - 11 : luzCeu);
		boolean escuro = luz <= 4;
		boolean subterraneo = luzCeu == 0 && pos.getY() < 50;
		if (subterraneo) {
			if (e.subsoloDesde < 0) {
				e.subsoloDesde = seg;
			}
		} else {
			e.subsoloDesde = -1;
			e.tentouCenaTunel = false;
		}
		boolean calma = emZonaCalma(p, p.getX(), p.getY(), p.getZ());
		boolean perto24DaCama = m.get(Memoria.TEM_CAMA) == 1 && distanciaSqr(p, m.get(Memoria.CAMA_X), m.get(Memoria.CAMA_Z)) < 24 * 24;

		// --- Contexto do mundo (0.5-alpha): muda O QUE ele faz, nao so QUANDO. ---
		ContextoMundo.Tipo contextoNovo = ContextoMundo.detectar(level, p, m, subterraneo);
		if (contextoNovo == e.contexto) {
			e.contextoCandidato = contextoNovo;
			e.contextoCandidatoDesde = -1;
		} else {
			boolean imediato = contextoNovo == ContextoMundo.Tipo.CASA || contextoNovo == ContextoMundo.Tipo.SUBSOLO;
			if (e.contextoCandidato != contextoNovo) {
				e.contextoCandidato = contextoNovo;
				e.contextoCandidatoDesde = seg;
			}
			long espera = (e.contexto == ContextoMundo.Tipo.CASA || e.contexto == ContextoMundo.Tipo.SUBSOLO) ? 2 : 4;
			if (imediato || (e.contextoCandidatoDesde >= 0 && seg - e.contextoCandidatoDesde >= espera)) {
				Depuracao.log(p, seg, "CONTEXTO " + e.contexto + " -> " + contextoNovo + " (estavel=" + (imediato ? 0 : espera) + "s)");
				e.contexto = contextoNovo;
				e.contextoDesde = seg;
				e.contextoCandidatoDesde = -1;
			}
		}
		if (e.contexto == ContextoMundo.Tipo.ABERTO && !calma && !ContextoMundo.pertoDaCasa(p, m, 40)) {
			if (e.abertoDesde < 0) {
				e.abertoDesde = seg;
			}
		} else {
			e.abertoDesde = -1;
			e.tentouCenaCampo = false;
		}

		int delta = escuro ? 3 : -2;
		if (subterraneo) {
			delta += 1;
		}
		if (noite && longeDeCasa(m, p)) {
			delta += 1;
		}
		if (luzBloco >= 12) {
			delta -= 2;
		}
		if (calma) {
			delta = -8;
		}
		m.add(Memoria.INQUIETACAO, delta);
		m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
		int inq = m.get(Memoria.INQUIETACAO);

		// --- Observações para o perfil ---
		if (escuro) {
			e.minEscuro++;
			if (p.isCrouching() || e.velocidade < 1.5) {
				e.minEscuroCauteloso++;
			}
		}
		if (noite || subterraneo) {
			e.minNoiteOuSubsolo++;
			if (luzBloco >= 8) {
				e.minComLuz++;
			}
		}
		if (noite) {
			e.minNoite++;
			if (perto24DaCama) {
				e.minEmCasa++;
				m.add(Memoria.CASEIRO, 1);
			}
		}
		if (subterraneo) {
			m.add(Memoria.SUBSOLO, 1);
		}

		// --- Lugares ---
		int cx = p.getBlockX() >> 4;
		int cz = p.getBlockZ() >> 4;
		long chaveChunk = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		if (chaveChunk != e.ultimoChunk) {
			e.ultimoChunk = chaveChunk;
			Lugares l = Lugares.de(p);
			if (l.registrarSeNovo(cx, cz)) {
				e.minChunksNovos++;
				m.add(Memoria.CHUNKS_VISITADOS, 1);
				l.salvar();
			}
			e.chunkEhMarco = l.ehMarco(cx, cz);
			if (e.chunkEhMarco && !e.marcosUsadosSessao.contains(chaveChunk)) {
				e.marcoPendente = chaveChunk;
				Depuracao.log(p, seg, "MARCO revisitado chunk=(" + cx + "," + cz + ") pendente=sim");
			}
		}
		if (seg % 60 == 0) {
			Lugares l = Lugares.de(p);
			l.somarMinuto(cx, cz, (int) (tick / 24000L));
			l.salvar();
			Perfil.fecharMinuto(m, e);
		}

		// --- Tempo e fase ---
		m.add(Memoria.TEMPO, inq >= 100 ? 2 : 1);
		int faseNova = faseDoTempo(m.get(Memoria.TEMPO));
		if (faseNova > m.get(Memoria.FASE)) {
			m.set(Memoria.FASE, faseNova);
			transicao(level, p, m, faseNova);
			Depuracao.log(p, seg, "FASE -> " + faseNova);
		}
		int fase = m.get(Memoria.FASE);

		// --- Conteúdo físico e atmosfera (0.6): o mundo começa antes do primeiro Hóspede. ---
		EstruturasSussurros.verificar(level, p, m, e, fase, subterraneo, seg, rnd);
		Atmosfera.atualizar(level, p, m, e, fase, subterraneo, noite, seg, tick, rnd);

		// --- Pressão (cai mais rápido quando ele está recuando) ---
		boolean criaturaPresente = e.criatura != null && !e.criatura.isRemoved();
		verificarFioVigilia(level, p, m, e, seg, tick);
		double queda = e.estado == EstadoDiretor.RECUANDO ? 0.8 : 0.4;
		e.pressao = Math.max(0, e.pressao - queda + (criaturaPresente ? 0.6 : 0));

		// --- Memória curta volta ao neutro ---
		decairCurto(e);

		// --- Obsessão: sobe devagar enquanto ele escala, cai pouco quando recua (v0.4.2) ---
		if (!e.obsessaoCarregada) {
			e.obsessao = m.get(Memoria.OBSESSAO) / 10.0;
			e.obsessaoCarregada = true;
		}
		if (fase >= 1) {
			double dObs = switch (e.estado) {
				case ESCALANDO -> 0.05 + (e.ultimoV >= 60 ? 0.03 : 0);
				case TESTANDO -> 0.02;
				case RECUANDO -> -0.02;
				default -> 0;
			};
			somarObsessao(e, dObs);
		}
		m.set(Memoria.OBSESSAO, (int) Math.round(e.obsessao * 10));

		// --- Coisas que ele só percebe depois ---
		verificarAtrasados(p, e, seg, tick);

		// --- Momento e estado do Diretor ---
		int v = vulnerabilidade(level, p, m, e, escuro, subterraneo, inq, seg, tick, fase);
		e.ultimoV = v;
		atualizarEstado(p, m, e, fase, v, seg, rnd);

		if (seg % 60 == 0) {
			Depuracao.log(p, seg, String.format(Locale.ROOT,
					"estado=%s V=%d pressao=%.0f obsessao=%.0f inq=%d fase=%d tempo=%d contexto=%s cena=%s rastro=%d atm=%.1f manifestacaoAtiva=%s cenaAtiva=%s | %s",
					e.estado, v, e.pressao, e.obsessao, inq, fase, m.get(Memoria.TEMPO), e.contexto, e.cena, e.rastro.tamanho(), e.atmosfera.orcamento, manifestacaoAtiva(e), cenaAtiva(e),
					Perfil.resumo(m)));
		}

		// --- Eventos ---
		CenaVoltouComVoce.verificarVoltaParaCasa(p, m, e, fase, seg, rnd);
		CenaAlgoNoTunel.verificarCenaTunel(p, e, fase, subterraneo, calma, seg, rnd);
		CenaLinhaDasArvores.verificarCenaCampo(p, m, e, fase, calma, seg, rnd);
		CenaFoiAqui.verificarCenaMarco(p, e, fase, calma, seg, rnd);
		CenaDoOutroLadoDoVidro.verificarCenaJanela(level, p, e, fase, noite, calma, seg, rnd);
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA) {
			// Cena "Ele voltou com você": nada aleatório atrapalha a composição.
			CenaVoltouComVoce.conduzirCenaCasa(level, p, m, e, fase, seg, tick, rnd);
		} else if (e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			// Cena "Do outro lado do vidro": presença doméstica que usa uma janela real da casa.
			CenaDoOutroLadoDoVidro.conduzirCenaJanela(level, p, e, seg, tick, rnd);
		} else if (e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA) {
			// Cena "Algo no túnel": eco da própria ação, ruído contextual e presença no rastro.
			CenaAlgoNoTunel.conduzirCenaTunel(level, p, m, e, fase, seg, tick, rnd);
		} else if (e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA) {
			// Cena "Na linha das arvores": duas aparicoes espaciais, com cobertura e sem anuncio garantido.
			CenaLinhaDasArvores.conduzirCenaCampo(level, p, m, e, fase, seg, tick, rnd);
		} else if (e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA) {
			// Cena "Foi aqui": um lugar que já funcionou volta a ser usado como memória.
			CenaFoiAqui.conduzirCenaMarco(level, p, m, e, fase, seg, tick, rnd);
		} else if (e.estado == EstadoDiretor.AMEACANDO) {
			// Sequência de ameaça: nada aleatório atrapalha a composição.
			conduzirAmeaca(level, p, m, e, fase, escuro, v, seg, tick, rnd);
		} else if (fase >= 1 && !criaturaPresente) {
			decidir(level, p, m, e, fase, escuro, inq, calma, v, seg, tick, rnd);
		}

		// O que o cliente dele deve mostrar e tocar (cor, borda, neblina, trilha). Só apresentação.
		Sentidos.atualizar(p, e, fase, calma, noite, tick);

		m.salvar();
	}

	// =====================================================================
	// Fases
	// =====================================================================

	public static int faseDoTempo(int tempo) {
		for (int f = 4; f >= 1; f--) {
			if (tempo >= LIMIAR_FASE[f]) {
				return f;
			}
		}
		return 0;
	}

	private static void transicao(ServerLevel level, ServerPlayer p, Memoria m, int fase) {
		boolean darPagina = m.get(Memoria.PAGINAS_ENTREGUES) < Diario.TOTAL_PAGINAS;
		boolean darOlho = fase >= 3 && m.get(Memoria.RECEBEU_OLHO) == 0;
		if (darPagina) {
			m.add(Memoria.PAGINAS_ENTREGUES, 1);
		}
		if (darOlho) {
			m.set(Memoria.RECEBEU_OLHO, 1);
		}
		if (!darPagina && !darOlho) {
			return;
		}
		passos(level, p, 4);
		agendar(level, 34, () -> {
			if (p.isRemoved()) {
				return;
			}
			if (darPagina) {
				soltarAtras(level, p, new ItemStack(ModItems.PAGINA_RASGADA), 2.0);
			}
			if (darOlho) {
				soltarAtras(level, p, new ItemStack(ModItems.OLHO_SUSSURRANTE), 2.5);
			}
		});
	}

	private static int intervalo(int fase, int inq, Memoria m, EstadoJogador e, RandomSource rnd) {
		int min = INTERVALO[fase][0];
		int max = INTERVALO[fase][1];
		double v = min + rnd.nextInt(max - min + 1);
		v *= 1.0 - inq / 400.0;
		if (Perfil.alto(m, Perfil.Traco.CAUTELA)) {
			v *= 1.2; // o silêncio pesa mais para quem escuta
		}
		if (e.estado == EstadoDiretor.OBSERVANDO) {
			v *= 1.5; // 0.4: 2.0 (deixava a fase 1 parada demais)
		}
		return (int) Math.max(20, v);
	}

	private static int intervaloMedio(int fase) {
		return Math.max(60, (INTERVALO[fase][0] + INTERVALO[fase][1]) / 2);
	}

	// =====================================================================
	// Momento (vulnerabilidade) e estados
	// =====================================================================

	static boolean bloqueado(ServerPlayer p, EstadoJogador e, long tick) {
		return p.getHealth() < 6
				|| tick - e.ultimoDanoTick < 200
				|| p.isInWater() || p.isInLava()
				|| (!p.onGround() && p.getDeltaMovement().y < -0.6)
				|| p.isPassenger()
				|| p.isSleeping();
	}

	private static int vulnerabilidade(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e,
			boolean escuro, boolean subterraneo, int inq, long seg, long tick, int fase) {
		if (bloqueado(p, e, tick)) {
			return 0;
		}
		int v = 0;
		if (escuro) {
			v += 25;
		}
		boolean sozinho = level.getPlayers(o -> o != p && !o.isSpectator() && o.distanceToSqr(p) < 48 * 48).isEmpty();
		if (sozinho) {
			v += 15;
		}
		if (longeDeCasa(m, p) || subterraneo) {
			v += 15;
		}
		int quebras = 0;
		for (long t : e.quebrasRecentes) {
			if (tick - t < 3600) {
				quebras++;
			}
		}
		if (quebras >= 8) {
			v += 15; // rotina: minerando sem parar há minutos
		}
		if (inq >= 100) {
			v += 10;
		}
		if (e.chunkEhMarco) {
			v += 10; // "foi aqui que aconteceu aquilo"
		}
		if (fase >= 1) {
			v += (int) Math.min(20, 20.0 * (seg - e.ultimoEventoSeg) / (2.0 * intervaloMedio(fase)));
		}
		return Math.max(0, Math.min(100, v));
	}

	private static int portao(int intensidade) {
		return intensidade <= 10 ? 20 : intensidade <= 20 ? 40 : intensidade <= 30 ? 55 : 70;
	}

	private static int sortearDuracao(EstadoDiretor st, RandomSource rnd) {
		if (st.duracaoMax <= 0) {
			return 0;
		}
		return st.duracaoMin + rnd.nextInt(st.duracaoMax - st.duracaoMin + 1);
	}

	private static void atualizarEstado(ServerPlayer p, Memoria m, EstadoJogador e, int fase, int v, long seg, RandomSource rnd) {
		long no = seg - e.estadoDesde;
		switch (e.estado) {
			case CALMO -> {
				if (fase >= 1 && no >= e.duracaoEstado) {
					mudarEstado(p, e, EstadoDiretor.OBSERVANDO, seg, rnd);
				}
			}
			case OBSERVANDO -> {
				if (no >= e.duracaoEstado && v >= 40 && e.pressao < 30) {
					mudarEstado(p, e, EstadoDiretor.TESTANDO, seg, rnd);
				}
			}
			case TESTANDO -> {
				if (e.ultimaConfianca >= CONFIANCA_ESCALAR) {
					mudarEstado(p, e, EstadoDiretor.ESCALANDO, seg, rnd);
				} else if (e.testesSemReacao >= 3 || no > 600) {
					// Indiferença não resolve (página 6 do diário): duas rodadas inteiras sem reação, e ele perde a paciência.
					if (e.testesSemReacao >= 3 && fase >= 2 && ++e.testesFrustrados >= 2) {
						Depuracao.log(p, seg, "indiferença: duas rodadas de teste sem reação, ele escala mesmo assim");
						mudarEstado(p, e, EstadoDiretor.ESCALANDO, seg, rnd);
					} else {
						mudarEstado(p, e, EstadoDiretor.OBSERVANDO, seg, rnd);
					}
				}
			}
			case ESCALANDO -> {
				boolean semCriatura = e.criatura == null || e.criatura.isRemoved();
				if (e.pressao >= 75 || no > 900) {
					String motivoRecuo = e.pressao >= 75 ? "PRESSAO_ALTA" : "TEMPO_ESCALANDO"; // telemetria
					mudarEstado(p, e, EstadoDiretor.RECUANDO, seg, rnd);
					marcarSilencioDoRecuo(p, e, seg, motivoRecuo, "-");
				} else if (fase >= 3 && e.obsessao >= OBSESSAO_AMEACA && v >= 60 && e.pressao < 60
						&& seg >= e.ameacaLiberadaEm && semCriatura
						&& e.cenaCasa == EstadoJogador.CenaCasa.NENHUMA
						&& e.cenaTunel == EstadoJogador.CenaTunel.NENHUMA
						&& e.cenaCampo == EstadoJogador.CenaCampo.NENHUMA
						&& e.cenaMarco == EstadoJogador.CenaMarco.NENHUMA
						&& e.cenaJanela == EstadoJogador.CenaJanela.NENHUMA) {
					// v0.4.2: a ameaça vem da obsessão (escalada lenta), não da pressão (tensão do momento).
					mudarEstado(p, e, EstadoDiretor.AMEACANDO, seg, rnd);
				}
			}
			case AMEACANDO -> {
				// Quem encerra é a própria sequência (fimAmeaca). Isto é só uma trava de segurança.
				if (no > 600) {
					fimAmeaca(p, e, seg, rnd, "TEMPO_ESGOTADO", 0.7);
				}
			}
			case RECUANDO -> {
				if (no >= e.duracaoEstado) {
					logFimDoRecuo(p, e, seg);
					mudarEstado(p, e, EstadoDiretor.CALMO, seg, rnd);
				}
			}
		}
	}

	private static void mudarEstado(ServerPlayer p, EstadoJogador e, EstadoDiretor novo, long seg, RandomSource rnd) {
		Depuracao.log(p, seg, "ESTADO " + e.estado + " -> " + novo);
		e.estado = novo;
		e.estadoDesde = seg;
		e.duracaoEstado = sortearDuracao(novo, rnd);
		e.esperandoDesde = -1;
		switch (novo) {
			case TESTANDO -> {
				e.testesSemReacao = 0;
				e.testadasNoTeste.clear();
				// Uma reação forte de pouco antes continua valendo (0.4 zerava sempre).
				if (seg - e.ultimaConfiancaSeg > MEMORIA_REACAO) {
					e.ultimaConfianca = 0;
				}
			}
			case ESCALANDO -> e.testesFrustrados = 0;
			case AMEACANDO -> {
				e.cena = EstadoJogador.Cena.NENHUMA;
				e.cenaDesde = seg;
				e.golpeDado = false;
			}
			case RECUANDO -> {
				e.pressao *= 0.5;
				e.sequencia = null;
				e.elosCadeia = 0;
				e.contato = false;
			}
			default -> {
			}
		}

		// Agenda: ao entrar num estado ativo, o cronômetro antigo (às vezes sorteado em OBSERVANDO,
		// muito mais longo) não pode valer mais. Na 0.4 isso fazia ele perder a primeira noite inteira.
		if (novo == EstadoDiretor.TESTANDO || novo == EstadoDiretor.ESCALANDO) {
			long cedo = seg + 15 + rnd.nextInt(31);
			if (e.proximoEvento < 0 || e.proximoEvento > cedo) {
				e.proximoEvento = cedo;
				Depuracao.log(p, seg, "agenda: próximo evento em " + (cedo - seg) + "s");
			}
		}
	}

	// =====================================================================
	// Decisão: o quê, e quando
	// =====================================================================

	private static void decidir(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean escuro, int inq, boolean calma, int v, long seg, long tick, RandomSource rnd) {
		EstadoDiretor st = e.estado;
		e.falhasAgora.clear();

		// Piso: estar seguro reduz muito a atividade, mas nunca desliga o mod.
		if (st != EstadoDiretor.RECUANDO && seg - e.ultimoEventoSeg >= PISO[fase] && !bloqueado(p, e, tick)) {
			Evento ev = escolher(level, p, m, e, fase, escuro, inq, calma, v, Modo.PISO, seg, rnd);
			if (ev != null && executar(level, p, m, e, ev, seg, tick)) {
				Depuracao.log(p, seg, "PISO: " + ev);
				e.proximoEvento = seg + intervalo(fase, inq, m, e, rnd);
				return;
			}
		}

		if (st.intensidadeMax == 0) {
			return;
		}
		if (seg < e.semLugarAte) {
			return; // no último sorteio nada coube no mundo: dá um tempo antes de tentar de novo
		}
		if (e.proximoEvento < 0) {
			e.proximoEvento = seg + 30 + rnd.nextInt(30);
		}
		// Oportunidade: o momento perfeito não espera o cronômetro (só fora de OBSERVANDO,
		// que é o estado de ficar quieto vigiando).
		boolean adiantado = false;
		if (seg < e.proximoEvento) {
			boolean oportunidade = st != EstadoDiretor.OBSERVANDO
					&& e.sequencia == null // cadeias têm o próprio horário
					&& v >= V_OPORTUNIDADE
					&& seg - e.ultimoEventoSeg >= Math.max(30, INTERVALO[fase][0] / 2);
			if (!oportunidade) {
				return;
			}
			adiantado = true;
		}

		// Continuação de uma cadeia?
		Evento ev = null;
		if (e.sequencia != null) {
			Evento s = e.sequencia;
			e.sequencia = null;
			if (st.permiteCadeias() && s.faseMinima <= fase && v >= Seletor.portao(s.intensidade)
					&& podeAcontecer(level, p, m, e, s, escuro, inq, calma)) {
				ev = s;
			} else {
				terminarCadeia(e, seg, fase);
			}
		}
		if (ev == null) {
			ev = escolher(level, p, m, e, fase, escuro, inq, calma, v, Modo.NORMAL, seg, rnd);
		}

		if (adiantado) {
			if (ev == null) {
				return; // nada cabe agora: segue esperando o cronômetro normal
			}
			Depuracao.log(p, seg, "oportunidade: V=" + v + ", adiantou " + (e.proximoEvento - seg) + "s");
		}

		Modo modo = Modo.NORMAL;
		if (ev == null) {
			// Nenhum momento bom. Ele espera, até 2 minutos.
			if (e.esperandoDesde < 0) {
				e.esperandoDesde = seg;
			}
			if (seg - e.esperandoDesde < 120) {
				return;
			}
			e.esperandoDesde = -1;
			e.proximoEvento = seg + intervalo(fase, inq, m, e, rnd);
			modo = Modo.FRACO;
			ev = escolher(level, p, m, e, fase, escuro, inq, calma, v, Modo.FRACO, seg, rnd);
			if (ev == null) {
				Depuracao.log(p, seg, "o momento não veio; desistiu do ciclo");
				return;
			}
			Depuracao.log(p, seg, "o momento não veio; evento fraco");
		} else {
			e.esperandoDesde = -1;
		}

		// O sorteado pode não caber no mundo agora (sem chão livre, sem ponto do Rastro, sem orçamento).
		// Antes isso queimava o ciclo, porque a agenda andava antes de saber se o evento rodou. Agora ele
		// sorteia outro entre os que sobraram, e a agenda só anda quando algo de fato aconteceu.
		for (int tentativa = 1; ev != null; tentativa++) {
			if (executar(level, p, m, e, ev, seg, tick)) {
				if (modo == Modo.NORMAL) {
					e.proximoEvento = Math.max(e.proximoEvento, seg + intervalo(fase, inq, m, e, rnd));
				}
				e.falhasAgora.clear();
				return;
			}
			e.falhasAgora.add(ev);
			Depuracao.log(p, seg, "SELECAO falhou evento=" + ev + " tentativa=" + tentativa + "/" + MAX_TENTATIVAS_POR_SEGUNDO);
			ev = tentativa < MAX_TENTATIVAS_POR_SEGUNDO
					? escolher(level, p, m, e, fase, escuro, inq, calma, v, modo, seg, rnd)
					: null;
		}
		e.falhasAgora.clear();
		e.semLugarAte = seg + 15 + rnd.nextInt(16);
		Depuracao.log(p, seg, "SELECAO sem lugar: nada coube agora; tenta de novo em " + (e.semLugarAte - seg) + "s");
	}

	/** As condições do mundo permitem esse evento agora? */
	private static boolean podeAcontecer(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Evento ev,
			boolean escuro, int inq, boolean calma) {
		if (calma) {
			return ev == Evento.ESPERA && m.get(Memoria.FASE) >= 4 && m.get(Memoria.VELAS) >= 2;
		}
		return switch (ev) {
			case PASSOS, PASSO_UNICO, SUSSURRO, SINAL, PRESENCA, VISTO -> true;
			case PASSAGEM, ANIMAIS, VESTIGIO, LUZ_ERRADA, SINAL_DISTANTE, RUIDO_RETORNO, OBJETO_FORA_LUGAR, TRILHA_INTERROMPIDA ->
					Atmosfera.podeEvento(level, p, e, ev, level.getGameTime() / 20);
			case SEGUIDOR -> e.rastro.tamanho() >= 8;
			case PEGADAS -> e.rastro.tamanho() >= 6;
			case VULTO -> podeVulto(level, p);
			// Os três abaixo só existem no cliente: sem o mod do outro lado, não acontece nada.
			case ECO_PASSOS -> Rede.temCliente(p) && p.onGround() && e.velocidade > 1.0;
			case VIGIA -> Rede.temCliente(p) && level.getGameTime() >= e.vigiaFalsaAte;
			case NEBLINA -> Rede.temCliente(p) && level.getGameTime() >= e.neblinaAte && level.canSeeSky(p.blockPosition().above());
			// Ele só assobia a cantiga depois de aprendê-la, e aprende ouvindo a caixa do jogador.
			case CANTIGA -> m.get(Memoria.CAIXA_USOS) >= CANTIGA_APRENDIDA;
			case ECO -> !e.acoes.isEmpty();
			case PORTA -> temPorta(level, p);
			case TOCHA -> acharTochaAtras(level, p, 12) != null;
			case BATIDA -> (m.get(Memoria.CASEIRO) >= 300 || Perfil.get(m, Perfil.Traco.CASEIRO) >= 60) && temPorta(level, p);
			case ECO_CHAT -> !e.falas.isEmpty();
			case ATRAS -> escuro;
			case TUMULO -> {
				if (m.get(Memoria.TEM_MORTE) != 1) {
					yield false;
				}
				double d = distanciaSqr(p, m.get(Memoria.MORTE_X), m.get(Memoria.MORTE_Z));
				yield d > 16 * 16 && d < 72 * 72;
			}
			case CACA -> escuro && inq >= 60 && podeComecarCacada(level, p, m, e);
			// O falso aviso tem de caber nos mesmos lugares da caçada, senão o lugar denuncia qual dos dois é.
			case PRENUNCIO -> escuro && podeComecarCacada(level, p, m, e);
			case ESPERA, ESPREITA -> false;
		};
	}

	@Nullable
	private static Evento escolher(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean escuro, int inq, boolean calma, int v, Modo modo, long seg, RandomSource rnd) {
		EstadoDiretor st = e.estado;
		int maxInt = switch (modo) {
			case NORMAL -> st.intensidadeMax;
			case PISO -> 8;
			case FRACO -> st.intensidadeMax > 0 ? Math.min(10, st.intensidadeMax) : 10;
		};
		boolean explorar = modo == Modo.NORMAL && rnd.nextDouble() < Seletor.taxaExploracao(
				e.estado.exploracao, e.confiancasRecentes, e.estado == EstadoDiretor.ESCALANDO);

		List<Evento> candidatos = new ArrayList<>();
		List<Double> pesos = new ArrayList<>();
		for (Evento ev : Evento.values()) {
			if (ev.faseMinima > fase || ev.intensidade > maxInt) {
				continue;
			}
			if (e.falhasAgora.contains(ev)) {
				continue; // já foi sorteado neste segundo e não coube no mundo
			}
			if (!ev.noSorteio && !(calma && ev == Evento.ESPERA)) {
				continue;
			}
			if (modo == Modo.NORMAL && v < Seletor.portao(ev.intensidade)) {
				continue;
			}
			if (!podeAcontecer(level, p, m, e, ev, escuro, inq, calma)) {
				continue;
			}

			double peso = 100;
			if (!explorar) {
				peso *= longo(m, ev) * longoCat(m, ev.categoria) * curto(e, ev) * curtoCat(e, ev.categoria);
			}
			peso *= Seletor.antiRepeticao(e.recentes, ev);
			peso *= Perfil.multiplicador(m, ev);
			peso *= ContextoMundo.multiplicador(e.contexto, ev);
			peso *= Seletor.aversaoSequencia(e.ultimoEvento, e.pares, ev);
			if (st == EstadoDiretor.OBSERVANDO || st == EstadoDiretor.TESTANDO) {
				peso *= e.interesse.getOrDefault(ev.categoria, 1.0);
			}
			if (st == EstadoDiretor.TESTANDO && e.testadasNoTeste.contains(ev.categoria)) {
				peso *= 0.3; // ao testar, ele prefere sondar um tipo de medo diferente
			}
			if (ev == Evento.ECO && m.get(Memoria.SUBSOLO) > 900) {
				peso *= 1.4;
			}
			if (ev == Evento.TUMULO) {
				peso *= 1.5;
			}
			candidatos.add(ev);
			pesos.add(peso);
		}
		if (candidatos.isEmpty()) {
			return null;
		}

		// Intensidade-alvo (v0.4.2): em ESCALANDO ele QUER coisas mais fortes com o tempo.
		// Começa em 10 e chega a 30 em 10 minutos, limitada ao mais forte que cabe agora.
		// Peso pela proximidade do alvo, com piso de 15% para os fracos não sumirem de vez.
		double alvoInt = -1;
		String alvoLog = "alvo=-"; // telemetria: as partes do alvo, para a linha SELECAO
		if (st == EstadoDiretor.ESCALANDO && modo == Modo.NORMAL) {
			int maisForte = 0;
			for (Evento c : candidatos) {
				maisForte = Math.max(maisForte, c.intensidade);
			}
			double alvoBase = 10 + 20 * Math.min(1.0, (seg - e.estadoDesde) / 600.0);
			double bonusObsessao = 8 * e.obsessao / 100.0;
			double bonusV = 6 * Math.max(0, (v - 50) / 50.0);
			double abatimento = 8 * Math.max(0, 1 - (seg - e.ultimoForteSeg) / 150.0); // evita forte-forte-forte em sequência
			alvoInt = limitar(alvoBase + bonusObsessao + bonusV - abatimento, 8, Math.max(8, maisForte));
			alvoLog = String.format(Locale.ROOT,
					"alvoBase=%.1f bonusObsessao=%.1f bonusV=%.1f abatimento=%.1f alvoFinal=%.1f maisForte=%d",
					alvoBase, bonusObsessao, bonusV, abatimento, alvoInt, maisForte);
			for (int i = 0; i < candidatos.size(); i++) {
				pesos.set(i, pesos.get(i) * Seletor.pesoIntensidade(candidatos.get(i).intensidade, alvoInt));
			}
		}

		Seletor.aplicarTeto(candidatos, pesos, 0.45);

		double total = 0;
		for (double w : pesos) {
			total += w;
		}
		if (Depuracao.ativo) {
			StringBuilder sb = new StringBuilder("escolha[" + modo + (explorar ? ",explorando" : "") + "] V=" + v
					+ (alvoInt > 0 ? String.format(Locale.ROOT, " alvo=%.0f", alvoInt) : "") + ":");
			for (int i = 0; i < candidatos.size(); i++) {
				sb.append(String.format(Locale.ROOT, " %s=%.0f", candidatos.get(i), pesos.get(i)));
			}
			Depuracao.log(p, seg, sb.toString());
		}
		double sorteio = rnd.nextDouble() * total;
		int indiceEscolhido = Seletor.sortearIndice(pesos, sorteio);
		Evento escolhido = candidatos.get(indiceEscolhido);
		if (Depuracao.ativo) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "SELECAO estado=%s modo=%s contexto=%s V=%d %s escolhido=%s intensidade=%d",
					st, modo, e.contexto, v, alvoLog, escolhido, escolhido.intensidade));
		}
		return escolhido;
	}

	// =====================================================================
	// Pesos aprendidos (memória longa salva, memória curta da sessão)
	// =====================================================================

	private static double longo(Memoria m, Evento ev) {
		String chave = "l_ev_" + ev.name().toLowerCase();
		int salvo = m.get(chave, -1);
		if (salvo < 0) {
			int antigo = m.get(ev.chavePeso(), -1); // migração da v0.3
			return antigo < 0 ? 1.0 : limitar(antigo / 100.0, 0.5, 2.0);
		}
		return salvo / 100.0;
	}

	private static double longoCat(Memoria m, Evento.Categoria cat) {
		String chave = "l_cat_" + cat.name().toLowerCase();
		int salvo = m.get(chave, -1);
		if (salvo < 0) {
			int antigo = m.get(cat.chavePeso(), -1);
			return antigo < 0 ? 1.0 : limitar(antigo / 100.0, 0.6, 1.7);
		}
		return salvo / 100.0;
	}

	private static double curto(EstadoJogador e, Evento ev) {
		return e.curtoEv.getOrDefault(ev, 1.0);
	}

	private static double curtoCat(EstadoJogador e, Evento.Categoria cat) {
		return e.curtoCat.getOrDefault(cat, 1.0);
	}

	private static void aprender(Memoria m, EstadoJogador e, Evento ev, double d, double eng) {
		double le = limitar(longo(m, ev) * (1 + 0.06 * d), 0.5, 2.0);
		m.set("l_ev_" + ev.name().toLowerCase(), (int) Math.round(le * 100));
		double lc = limitar(longoCat(m, ev.categoria) * (1 + 0.04 * d), 0.6, 1.7);
		m.set("l_cat_" + ev.categoria.name().toLowerCase(), (int) Math.round(lc * 100));
		e.curtoEv.put(ev, limitar(curto(e, ev) * (1 + 0.30 * d), 0.4, 2.5));
		e.curtoCat.put(ev.categoria, limitar(curtoCat(e, ev.categoria) * (1 + 0.20 * d), 0.5, 2.0));
		double interesse = e.interesse.getOrDefault(ev.categoria, 1.0);
		e.interesse.put(ev.categoria, limitar(interesse * (1 + 0.2 * (eng - 0.3)), 0.6, 1.8));
	}

	private static void decairCurto(EstadoJogador e) {
		e.curtoEv.replaceAll((k, w) -> 1 + (w - 1) * DECAIMENTO_CURTO);
		e.curtoCat.replaceAll((k, w) -> 1 + (w - 1) * DECAIMENTO_CURTO);
		e.interesse.replaceAll((k, w) -> 1 + (w - 1) * DECAIMENTO_CURTO);
	}

	static double limitar(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}



	// =====================================================================
	// Execução (com "ele provavelmente percebeu?")
	// =====================================================================

	/** Executa um evento. A criatura que ele criar fica marcada com a origem e o evento (v0.4.2). */
	private static boolean executar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Evento ev, long seg, long tick) {
		PedidoManifestacao pedido = e.forcando ? PedidoManifestacao.deComando(ev) : PedidoManifestacao.doDiretor(ev);
		return executarInterno(level, p, m, e, ev, seg, tick, pedido);
	}
	private static boolean executarInterno(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Evento ev, long seg, long tick, PedidoManifestacao pedido) {
		RandomSource rnd = level.getRandom();
		int ousadia = Math.min(10, m.get(Memoria.VEZES_VISTO) / 2 + m.get(Memoria.VEZES_FERIDO));
		boolean ok = true;
		Vec3 fonte = null;
		double obs = 0; // observabilidade: chance de o jogador ter percebido (0 = não aprende agora)
		boolean ocupado = tick - e.ultimoDanoTick < 100 || quebrouHaPouco(e, tick, 60);
		double barulho = ocupado ? 0.6 : 1.0;

		switch (ev) {
			case PASSOS -> {
				passos(level, p, 3 + rnd.nextInt(3));
				fonte = pontoRelativo(p, 180, 4);
				obs = 0.9 * barulho;
			}
			case PASSO_UNICO -> {
				if (rnd.nextFloat() < 0.25F) {
					// Às vezes não é um passo: um estalo, um pano, uma respiração. Sem ninguém ali.
					Vec3 atras = pontoRelativo(p, 180, 3.0);
					float qual = rnd.nextFloat();
					ModSons.Som som = qual < 0.4F ? ModSons.Som.ESTALO : qual < 0.8F ? ModSons.Som.PANO : ModSons.Som.RESPIRACAO;
					ModSons.tocar(level, atras.x, p.getY() + 1.0, atras.z, som, 0.6F, 0.9F + rnd.nextFloat() * 0.2F);
					Depuracao.log(p, seg, "passo único virou " + som + " (falso positivo)");
				} else {
					passoUnico(level, p);
				}
				fonte = pontoRelativo(p, 180, 2.5);
				obs = 0.5 * barulho;
			}
			case PASSAGEM, ANIMAIS, VESTIGIO, LUZ_ERRADA, SINAL_DISTANTE, RUIDO_RETORNO, OBJETO_FORA_LUGAR, TRILHA_INTERROMPIDA -> {
				Atmosfera.Resultado r = Atmosfera.executarEvento(level, p, m, e, ev, m.get(Memoria.FASE), seg, tick, rnd);
				if (r == null) {
					ok = false;
				} else {
					fonte = r.fonte();
					obs = r.observabilidade() * barulho;
				}
			}
			case ECO -> {
				// v0.4.2a: ele devolve algo que VOCÊ fez. De preferência do lugar onde você fez,
				// senão de um lugar por onde você passou, senão de algum ponto perto.
				EstadoJogador.Acao acao = sortearAcao(e, rnd);
				Vec3 deCasa = null;
				Vec3 lugar = null;
				String motivoEco = e.ecoPerto ? "motivo=PERTO_CADEIA" : "motivo=PERTO"; // telemetria
				if (!e.ecoPerto) {
					StringBuilder nota = new StringBuilder();
					lugar = lugarParaEco(p, e, acao, seg, rnd.nextFloat() < 0.65F, nota);
					if (lugar != null) {
						motivoEco = nota.toString();
					} else if (longeDeCasa(m, p) && rnd.nextFloat() < 0.12F) {
						deCasa = new Vec3(m.get(Memoria.CAMA_X) + 0.5, p.getY(), m.get(Memoria.CAMA_Z) + 0.5);
						motivoEco = "motivo=DIRECAO_DE_CASA";
					}
				}
				fonte = eco(level, p, acao.som(), acao.tipo() == EstadoJogador.TipoAcao.PORTA, e.ecoPerto, deCasa, lugar);
				e.ecoPerto = false;
				logEco(p, "ECO", acao, fonte, motivoEco, seg);
				obs = limitar(1 - distancia(p, fonte) / 28.0, 0, 1) * barulho;
			}
			case SUSSURRO -> {
				sussurro(p, m.get(Memoria.FASE), rnd);
				obs = 0.9;
			}
			case SINAL -> {
				SinalResultado sinal = sinalFalso(level, p, m, e, seg, rnd);
				fonte = sinal.fonte();
				obs = sinal.observabilidade() * barulho;
			}
			case SEGUIDOR -> {
				SinalResultado seguidor = passosNoRastro(level, p, e, seg, rnd);
				if (seguidor == null) {
					ok = false;
				} else {
					fonte = seguidor.fonte();
					obs = seguidor.observabilidade() * barulho;
				}
			}
			case PEGADAS -> {
				SinalResultado pegadas = pegadasNoRastro(level, p, e, seg, rnd);
				if (pegadas == null) {
					ok = false;
				} else {
					fonte = pegadas.fonte();
					obs = pegadas.observabilidade() * barulho;
				}
			}
			// A reação ao vulto só é lida quando (e se) ele for avistado: ver criaturaAvistada.
			case VULTO -> ok = invocarVulto(level, p, e, pedido);
			case ECO_PASSOS -> {
				// Por meio minuto, parte dos passos dele toca de novo logo depois, um pouco atrás (ver EcoDePasso).
				e.ecoPassoAte = tick + 20L * (25 + rnd.nextInt(16));
				fonte = pontoRelativo(p, 180, 2.0);
				obs = 0.55 * barulho;
				Depuracao.log(p, seg, "ECO_PASSOS janela=" + (e.ecoPassoAte - tick) / 20 + "s");
			}
			case VIGIA -> {
				// A mesma sensação de quando ele olha de fora da tela, sem ele. Aviso que nunca falha vira radar.
				e.vigiaFalsaAte = tick + 20L * (15 + rnd.nextInt(16));
				e.vigiaFalsaForca = 0.45F + rnd.nextFloat() * 0.35F;
				fonte = pontoRelativo(p, 180, 12.0);
				obs = 0.5;
				Depuracao.log(p, seg, String.format(Locale.ROOT, "VIGIA falsa forca=%.2f duracao=%ds",
						e.vigiaFalsaForca, (e.vigiaFalsaAte - tick) / 20));
			}
			case NEBLINA -> {
				e.neblinaAte = tick + 20L * (60 + rnd.nextInt(61));
				e.neblinaForca = 0.45F + rnd.nextFloat() * 0.25F;
				// Sem fonte e sem leitura: não há para onde virar. Conta para o ritmo, não para o aprendizado.
				Depuracao.log(p, seg, String.format(Locale.ROOT, "NEBLINA forca=%.2f duracao=%ds",
						e.neblinaForca, (e.neblinaAte - tick) / 20));
			}
			case CANTIGA -> {
				fonte = assobiar(level, p, rnd);
				obs = limitar(1 - distancia(p, fonte) / 60.0, 0.3, 1) * barulho;
				Depuracao.log(p, seg, String.format(Locale.ROOT, "CANTIGA pos=%s dist=%.0f",
						pos(fonte.x, fonte.y, fonte.z), distancia(p, fonte)));
			}
			case PORTA -> {
				BlockPos porta = mexerNaPorta(level, p);
				ok = porta != null;
				if (ok) {
					fonte = Vec3.atCenterOf(porta);
					obs = limitar(1 - distancia(p, fonte) / 16.0, 0, 1) * barulho;
					if (obs < 0.5 && !e.forcando) {
						adicionarAtrasado(e, ev, fonte, seg);
					}
				}
			}
			case TOCHA -> {
				// 0.5-alpha: mexer no mundo sem transformar o mod em griefing.
				// Normalmente a tocha apenas apaga por alguns segundos e volta. Em fase 4,
				// com obsessao alta, uma minoria ainda pode ser levada de verdade.
				boolean roubar = m.get(Memoria.FASE) >= 4 && e.obsessao >= 75 && rnd.nextFloat() < 0.22F;
				BlockPos tocha = roubar ? roubarTocha(level, p) : piscarTocha(level, p, 45 + rnd.nextInt(76));
				ok = tocha != null;
				if (ok) {
					fonte = Vec3.atCenterOf(tocha);
					if (roubar) {
						Depuracao.log(p, seg, "TOCHA acao=LEVOU pos=" + pos(tocha.getX(), tocha.getY(), tocha.getZ()));
						if (!e.forcando) {
							m.add(Memoria.TOCHAS_ROUBADAS, 1);
							adicionarAtrasado(e, ev, fonte, seg);
						}
					} else {
						Depuracao.log(p, seg, "TOCHA acao=PISCOU pos=" + pos(tocha.getX(), tocha.getY(), tocha.getZ()));
						obs = 0.45 * barulho;
					}
				}
			}
			case BATIDA -> {
				BlockPos porta = bater(level, p);
				ok = porta != null;
				if (ok) {
					fonte = Vec3.atCenterOf(porta);
					obs = limitar(1 - distancia(p, fonte) / 14.0, 0, 1) * barulho;
				}
			}
			case ECO_CHAT -> {
				ok = ecoDaFala(p, e, rnd);
				obs = 0.9;
			}
			case PRESENCA -> {
				// v0.4.2: visível de verdade. Nasce logo FORA da tela (55-80° do olhar), a 18-35 blocos,
				// onde dá para enxergá-lo, fica mais tempo e é anunciado por um som vindo dele.
				ok = false;
				int duracao = 20 * (25 + ousadia * 2);
				if (e.iscaAtiva && tick < e.iscaAteTick) {
					int atendidas = m.get(Memoria.ISCAS_ATENDIDAS);
					double chanceIgnorar = atendidas < 3 ? 0.0 : Math.min(0.45, 0.15 + (atendidas - 3) * 0.07);
					if (rnd.nextDouble() >= chanceIgnorar) {
						ok = invocarPertoDaIsca(level, p, m, e, duracao, pedido);
					} else {
						Depuracao.log(p, seg, String.format(Locale.ROOT, "ISCA ignorada aprendida=sim atendidas=%d chance=%.2f", atendidas, chanceIgnorar));
					}
				}
				// Só tenta a rota se a isca não foi atendida: sem o !ok, um segundo Hóspede nascia
				// por cima do primeiro, que ficava órfão (vivo, mas fora de e.criatura).
				if (!ok && rnd.nextFloat() < 0.12F) {
					ok = invocarNaRota(level, p, m, e, ousadia, pedido);
					if (ok) {
						Depuracao.log(p, seg, "lugar: presença num caminho que você usa");
					}
				}
				if (!ok && rnd.nextFloat() < 0.45F) {
					ok = invocarNoRastro(level, p, e, HospedeEntity.Modo.OBSERVAR, seg, 20, 180, 18, 40, duracao,
							HospedeEntity.DIST_SUMIR_PADRAO, pedido);
				}
				double[] angulos = angulosPresencaAdaptativa(m, e, pedido);
				if (!ok && rnd.nextFloat() < 0.60F) {
					ok = invocarComCobertura(level, p, e, HospedeEntity.Modo.OBSERVAR, angulos[0], angulos[1],
							Math.max(14, 18 - ousadia), Math.max(24, 35 - ousadia * 1.5), duracao,
							HospedeEntity.DIST_SUMIR_PADRAO, pedido);
				}
				if (!ok) {
					ok = invocar(level, p, e, HospedeEntity.Modo.OBSERVAR, angulos[0], angulos[1],
							Math.max(14, 18 - ousadia), Math.max(24, 35 - ousadia * 1.5),
							duracao, 1.0, true, HospedeEntity.DIST_SUMIR_PADRAO, true, pedido);
				}
				if (ok) {
					anunciar(level, p, e.criatura);
				}
			}
			case ESPREITA -> ok = invocarEspreita(level, p, e, ousadia, seg, pedido);
			case ATRAS -> {
				boolean vigiaAsCostas = m.get(Memoria.OLHADAS) >= 12;
				// v0.4.1: ele está PERTO de propósito. Só some se você chegar a 4 blocos.
				ok = vigiaAsCostas
						? invocar(level, p, e, HospedeEntity.Modo.OBSERVAR, 65, 110, 8, 13, 20 * 12, 1.0, true, 4.0, pedido)
						: invocar(level, p, e, HospedeEntity.Modo.OBSERVAR, 160, 180, 9, 14, 20 * 12, 1.0, true, 4.0, pedido);
				HospedeEntity atras = e.criatura;
				if (ok && atras != null && rnd.nextFloat() < 0.25F) {
					agendar(level, 20 + rnd.nextInt(25), () -> {
						if (!atras.isRemoved()) {
							ModSons.tocar(level, atras.getX(), atras.getY() + 2.2, atras.getZ(), ModSons.Som.RESPIRACAO, 0.8F, 0.95F);
						}
					});
				}
			}
			case TUMULO -> ok = invocarNoTumulo(level, p, m, e, pedido);
			case VISTO -> p.sendOverlayMessage(Component.translatable("message.sussurros.visto", p.getName())
					.withStyle(s -> s.withColor(0x7A1010).withItalic(true)));
			case CACA -> {
				// 0.9: a caçada inteira mora em entidade.Cacada. O teto de 130 s é só uma trava: quem encerra é ela.
				// O último número é quanto mais rápido que o normal ele vem (cresce cada vez que o jogador o fere).
				ok = invocar(level, p, e, HospedeEntity.Modo.CACAR, 150, 180, 18, 26,
						20 * 130, 1.0 + 0.03 * Math.min(6, m.get(Memoria.VEZES_FERIDO)), true, pedido);
				if (ok) {
					prenunciar(level, p, e, tick, rnd, true);
				}
			}
			case PRENUNCIO -> {
				// O mesmo aviso, sem ninguém. Aviso que nunca falha vira radar; por isso ele mente mais do que acerta.
				prenunciar(level, p, e, tick, rnd, false);
			}
			case ESPERA -> ok = invocarNaBordaDaZona(level, p, e, ousadia, pedido);
		}

		if (!ok) {
			return false;
		}

		if (e.forcando) {
			// Teste por comando: acontece de verdade, mas não mexe em agenda, pressão, anti-repetição nem aprendizado.
			Depuracao.log(p, seg, "EVENTO " + ev + " (forçado por comando: não conta para nada)");
			return true;
		}

		posEvento(p, e, ev, fonte, obs, seg, tick);
		return true;
	}

	/** Contabiliza um evento que aconteceu: pressão, ritmo, anti-repetição e (se ele percebeu) a leitura da reação. */
	static void posEvento(ServerPlayer p, EstadoJogador e, Evento ev, @Nullable Vec3 fonte, double obs, long seg, long tick) {
		e.pressao += ev.intensidade;
		e.ultimoEventoSeg = seg;
		if (ev.intensidade >= 22) {
			e.ultimoForteSeg = seg;
		}
		e.recentes.addFirst(ev);
		while (e.recentes.size() > 3) {
			e.recentes.removeLast();
		}
		if (e.ultimoEvento != null) {
			e.pares.addLast(e.ultimoEvento.categoria + ">" + ev.categoria);
			while (e.pares.size() > 6) {
				e.pares.removeFirst();
			}
		}
		e.ultimoEvento = ev;
		if (e.estado == EstadoDiretor.TESTANDO) {
			e.testadasNoTeste.add(ev.categoria);
		}

		Depuracao.log(p, seg, String.format(Locale.ROOT, "EVENTO %s (estado=%s, obs=%.2f, pressao=%.0f)", ev, e.estado, obs, e.pressao));
		if (obs >= 0.15) {
			if (!e.leitura.pendente()) {
				e.leitura.iniciar(ev, fonte, obs, tick, "direto");
			}
		} else if (ev.categoria != Evento.Categoria.VISAO && ev.categoria != Evento.Categoria.AMEACA && ev != Evento.TOCHA) {
			Depuracao.log(p, seg, "provavelmente não percebeu " + ev + ": não aprende");
		}
	}

	static void adicionarAtrasado(EstadoJogador e, Evento ev, Vec3 pos, long seg) {
		e.atrasados.add(new EstadoJogador.Atrasado(ev, pos, seg + 300));
		while (e.atrasados.size() > 4) {
			e.atrasados.remove(0);
		}
	}

	/** Algumas coisas só são percebidas depois: a tocha que falta, a porta que ficou aberta. */
	private static void verificarAtrasados(ServerPlayer p, EstadoJogador e, long seg, long tick) {
		Iterator<EstadoJogador.Atrasado> it = e.atrasados.iterator();
		while (it.hasNext()) {
			EstadoJogador.Atrasado a = it.next();
			if (seg > a.expira()) {
				it.remove();
				continue;
			}
			if (!e.leitura.pendente() && distancia(p, a.pos()) < 10 && pontoNaFrente(p, a.pos(), 0.9)) {
				it.remove();
				e.leitura.iniciar(a.evento(), a.pos(), 0.9, tick, "atrasada");
				Depuracao.log(p, seg, "percebeu depois: " + a.evento());
				break;
			}
		}
	}

	// =====================================================================
	// Aprendizado: o que a reação ensinou
	// =====================================================================

	private static void concluirLeitura(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg) {
		Leitura.Resultado r = e.leitura.avaliar();
		Memoria m = Memoria.de(p);
		int fase = m.get(Memoria.FASE);
		RandomSource rnd = level.getRandom();
		Evento ev = r.evento();
		double o = r.observabilidade();
		double c = r.confianca();

		Depuracao.log(p, seg, String.format(Locale.ROOT, "REACAO %s [%s] obs=%.2f c=%.2f eng=%.1f | %s",
				ev, r.origem(), o, c, r.engajamento(), r.sinais()));

		if (r.semDados()) {
			return; // parado no chat/menu: não ensina nada (0.4 contava como "não reagiu")
		}

		// Avistamento: a reação mais comum é virar e olhar direto para ele, e esse giro acontece ANTES de a
		// leitura começar (é ele que põe a criatura na tela). Sem isto, todo avistamento era lido como
		// "não reagiu", ensinava que aparições não funcionam e contava para a punição por indiferença.
		double eng = r.engajamento();
		if ("avistada".equals(r.origem()) && seg - e.encarouSeg <= 4 && c < 0.35) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "avistamento encarado: conta como reação (c %.2f -> 0.35)", c));
			c = 0.35;
			eng = Math.max(eng, 0.5);
		}

		// Se ele virou para a fonte ou foi até ela, ele percebeu, não importa o que a distância dizia.
		if (r.percebeu() && o < 0.8) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "percepção confirmada pela reação: obs %.2f -> 0.80", o));
			o = 0.8;
		}

		if (o < 0.15) {
			return;
		}

		// O quanto aprende depende de quão provável é que ele tenha percebido.
		double d = (c - 0.3) / 0.7 * o;
		aprender(m, e, ev, d, eng);

		if (o >= 0.5) {
			Perfil.puxar(m, Perfil.Traco.FUGA, r.fugiu() ? 100 : 0, 0.05);
			Perfil.puxar(m, Perfil.Traco.CONFRONTO, r.investigou() ? 100 : (r.fugiu() ? 0 : 50), 0.05);
			e.confiancasRecentes.addLast(c);
			while (e.confiancasRecentes.size() > 5) {
				e.confiancasRecentes.removeFirst();
			}
			e.ultimaConfianca = c;
			e.ultimaConfiancaSeg = seg;
			if (e.estado == EstadoDiretor.TESTANDO && c < 0.3) {
				e.testesSemReacao++;
			}
			// Reação forte enquanto ele só observava: não precisa testar, já sabe que funciona.
			if (e.estado == EstadoDiretor.OBSERVANDO && c >= CONFIANCA_ESCALAR && fase >= 1 && e.pressao < 75) {
				Depuracao.log(p, seg, String.format(Locale.ROOT, "reação forte (c=%.2f) observando: escala direto", c));
				mudarEstado(p, e, EstadoDiretor.ESCALANDO, seg, rnd);
			}
		}

		if (c >= 0.3) {
			m.add(Memoria.INQUIETACAO, (int) Math.round(12 * c));
			m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
			e.pressao += 6 * c;
			m.set(Memoria.SEM_REACAO, 0);
			somarObsessao(e, 2.0 * c); // funcionou: ele quer mais
		} else if (c < 0.15 && o >= 0.5) {
			// Para a punição por indiferença só vale o que quase certamente foi percebido. Um log real mostrou
			// ela disparando por cinza no chão e passos baixos no meio da mineração.
			if (o >= OBS_INDIFERENCA) {
				m.add(Memoria.SEM_REACAO, 1);
			}
			somarObsessao(e, 1.5); // ignorado: isso o fixa ainda mais em você
			// Indiferença não resolve: a assombração avança, em silêncio.
			if (m.get(Memoria.SEM_REACAO) >= 6 && fase >= 2) {
				m.set(Memoria.SEM_REACAO, 0);
				m.add(Memoria.TEMPO, 300);
				Depuracao.log(p, seg, "indiferença: assombração avança 300 s");
			}
		}

		// Um lugar onde algo forte aconteceu vira "marco".
		if (ev.intensidade >= 28 && c >= 0.5 && r.fonte() != null) {
			Lugares l = Lugares.de(p);
			BlockPos marcoPos = BlockPos.containing(r.fonte().x, r.fonte().y, r.fonte().z);
			l.marcarMarco(marcoPos.getX() >> 4, marcoPos.getZ() >> 4, marcoPos);
			l.salvar();
			Depuracao.log(p, seg, "MARCO criado evento=" + ev + " pos=" + pos(marcoPos.getX(), marcoPos.getY(), marcoPos.getZ()));
		}

		// Cadeias: curtas, sorteadas, com carência depois.
		if (e.estado.permiteCadeias() && seg >= e.carenciaAte && e.sequencia == null) {
			if (c >= 0.3 && e.elosCadeia < 2 && rnd.nextDouble() < 0.35 + 0.4 * c) {
				Evento prox = sortearContinuacao(ev, fase, m, e, rnd);
				if (prox != null) {
					e.sequencia = prox;
					e.elosCadeia++;
					e.proximoEvento = seg + (prox == Evento.PASSO_UNICO ? 4 + rnd.nextInt(6) : 15 + rnd.nextInt(26));
					if (prox == Evento.ECO) {
						e.ecoPerto = true;
					}
					Depuracao.log(p, seg, "cadeia: " + ev + " -> " + prox + " (elo " + e.elosCadeia + ")");
				} else {
					terminarCadeia(e, seg, fase);
				}
			} else if (c < 0.15 && o >= 0.5 && fase >= 2 && e.elosCadeia == 0 && rnd.nextDouble() < 0.3) {
				e.sequencia = fase >= 3 ? Evento.PRESENCA : Evento.TOCHA;
				e.elosCadeia = 1;
				e.proximoEvento = seg + 30 + rnd.nextInt(30);
				Depuracao.log(p, seg, "sem reação: sobe o tom -> " + e.sequencia);
			} else {
				terminarCadeia(e, seg, fase);
			}
		}

		m.salvar();
	}

	private static void terminarCadeia(EstadoJogador e, long seg, int fase) {
		if (e.elosCadeia > 0) {
			e.carenciaAte = seg + 2L * intervaloMedio(Math.max(1, fase));
		}
		e.elosCadeia = 0;
	}

	@Nullable
	private static Evento sortearContinuacao(Evento ev, int fase, Memoria m, EstadoJogador e, RandomSource rnd) {
		Evento[] opcoes = switch (ev) {
			case PASSOS -> new Evento[] {Evento.PASSO_UNICO, Evento.PASSAGEM, Evento.ECO};
			case PASSO_UNICO -> new Evento[] {Evento.PASSOS, Evento.SINAL_DISTANTE, Evento.SUSSURRO};
			case PASSAGEM -> new Evento[] {Evento.SINAL_DISTANTE, Evento.VESTIGIO, Evento.PASSO_UNICO};
			case ANIMAIS -> new Evento[] {Evento.SINAL_DISTANTE, Evento.VESTIGIO};
			case VESTIGIO -> new Evento[] {Evento.PASSAGEM, Evento.TRILHA_INTERROMPIDA, Evento.SINAL};
			case LUZ_ERRADA -> new Evento[] {Evento.PASSO_UNICO, Evento.SINAL, Evento.OBJETO_FORA_LUGAR};
			case SINAL_DISTANTE -> new Evento[] {Evento.PASSAGEM, Evento.RUIDO_RETORNO};
			case RUIDO_RETORNO -> new Evento[] {Evento.PASSO_UNICO, Evento.SEGUIDOR};
			case OBJETO_FORA_LUGAR -> new Evento[] {Evento.SINAL, Evento.PASSO_UNICO};
			case TRILHA_INTERROMPIDA -> new Evento[] {Evento.VESTIGIO, Evento.SINAL, Evento.PEGADAS};
			case ECO_PASSOS -> new Evento[] {Evento.PASSO_UNICO, Evento.VIGIA, Evento.SEGUIDOR};
			case VIGIA -> new Evento[] {Evento.PASSO_UNICO, Evento.SUSSURRO, Evento.PRESENCA};
			case NEBLINA -> new Evento[] {Evento.VULTO, Evento.SINAL_DISTANTE, Evento.CANTIGA};
			case CANTIGA -> new Evento[] {Evento.VIGIA, Evento.PASSOS, Evento.SINAL_DISTANTE};
			case PRENUNCIO -> new Evento[] {Evento.VIGIA, Evento.PASSO_UNICO};
			case ECO -> new Evento[] {Evento.ECO, Evento.RUIDO_RETORNO, Evento.PASSO_UNICO, Evento.SEGUIDOR};
			case SEGUIDOR -> new Evento[] {Evento.PASSO_UNICO, Evento.PEGADAS, Evento.PRESENCA, Evento.ECO_PASSOS};
			case PEGADAS -> new Evento[] {Evento.SINAL, Evento.PRESENCA};
			case VULTO -> new Evento[] {Evento.SINAL_DISTANTE, Evento.PASSO_UNICO};
			case PORTA -> new Evento[] {Evento.BATIDA, Evento.SUSSURRO, Evento.TOCHA};
			case BATIDA -> new Evento[] {Evento.PORTA, Evento.PASSO_UNICO};
			case TOCHA -> new Evento[] {Evento.ATRAS, Evento.SUSSURRO};
			case SUSSURRO -> new Evento[] {Evento.PASSO_UNICO, Evento.ECO_CHAT, Evento.VIGIA};
			case ECO_CHAT -> new Evento[] {Evento.SUSSURRO};
			case PRESENCA -> new Evento[] {Evento.ATRAS, Evento.VISTO};
			default -> new Evento[0];
		};
		boolean comNada = ev != Evento.PORTA;
		List<Evento> lista = new ArrayList<>();
		List<Double> pesos = new ArrayList<>();
		double total = comNada ? 100 : 0;
		for (Evento op : opcoes) {
			if (op.faseMinima > fase) {
				continue;
			}
			double w = 100 * longo(m, op) * curto(e, op) * Seletor.aversaoSequencia(e.ultimoEvento, e.pares, op);
			lista.add(op);
			pesos.add(w);
			total += w;
		}
		if (lista.isEmpty() || total <= 0) {
			return null;
		}
		double sorteio = rnd.nextDouble() * total;
		for (int i = 0; i < lista.size(); i++) {
			sorteio -= pesos.get(i);
			if (sorteio < 0) {
				return lista.get(i);
			}
		}
		return null; // "nada": a cadeia termina aqui
	}

	/** Coloca a criatura num caminho que o jogador costuma usar. */
	private static boolean invocarNaRota(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int ousadia, PedidoManifestacao pedido) {
		boolean temCasa = m.get(Memoria.TEM_CAMA) == 1;
		List<int[]> rotas = Lugares.de(p).rotas(temCasa, m.get(Memoria.CAMA_X) >> 4, m.get(Memoria.CAMA_Z) >> 4);
		List<int[]> boas = new ArrayList<>();
		for (int[] r : rotas) {
			Vec3 centro = new Vec3(r[0] * 16 + 8, p.getY(), r[1] * 16 + 8);
			double d = distancia(p, centro);
			if (d >= 30 && d <= 90 && !pontoNaFrente(p, centro, 0.6)) {
				boas.add(r);
			}
		}
		if (boas.isEmpty()) {
			return false;
		}
		int[] escolhida = boas.get(level.getRandom().nextInt(boas.size()));
		BlockPos chao = acharChao(level, escolhida[0] * 16 + 8.5, p.getY(), escolhida[1] * 16 + 8.5);
		if (chao == null || emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
			return false;
		}
		pedido = pedido.comNota("ROTA chunk=(" + escolhida[0] + "," + escolhida[1] + ")");
		criar(level, p, e, chao, HospedeEntity.Modo.OBSERVAR, 20 * (15 + ousadia * 2), 1.0, pedido);
		return true;
	}

	private static boolean quebrouHaPouco(EstadoJogador e, long tick, int janelaTicks) {
		Long ultima = e.quebrasRecentes.peekLast();
		return ultima != null && tick - ultima < janelaTicks;
	}

	static double distancia(ServerPlayer p, Vec3 v) {
		double dx = p.getX() - v.x;
		double dy = p.getY() - v.y;
		double dz = p.getZ() - v.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	static void atualizarCacheAmbiente(ServerLevel level, ServerPlayer p, EstadoJogador e) {
		long agora = level.getGameTime();
		double dx = p.getX() - e.cacheX;
		double dz = p.getZ() - e.cacheZ;
		if (agora - e.cacheTick < 200 && dx * dx + dz * dz < 36) {
			return;
		}
		e.cacheTick = agora;
		e.cacheX = p.getX();
		e.cacheZ = p.getZ();
		e.portas.clear();
		e.tochas.clear();
		e.janelas.clear();
		BlockPos centro = p.blockPosition();
		for (int ox = -12; ox <= 12; ox++) {
			for (int oy = -4; oy <= 4; oy++) {
				for (int oz = -12; oz <= 12; oz++) {
					BlockPos pos = centro.offset(ox, oy, oz);
					BlockState s = level.getBlockState(pos);
					if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)) {
						e.tochas.add(pos);
					} else if (s.getBlock() instanceof DoorBlock && !s.is(Blocks.IRON_DOOR) && s.hasProperty(BlockStateProperties.OPEN)) {
						e.portas.add(pos);
					} else if (!s.isAir()) {
						String caminho = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
						if (caminho.contains("glass")) {
							e.janelas.add(pos);
						}
					}
				}
			}
		}
	}

	// =====================================================================
	// Implementação dos eventos
	// =====================================================================


	/** Passos se aproximando por trás, com o som do bloco onde "ele" pisa. */
	private static void passos(ServerLevel level, ServerPlayer p, int quantidade) {
		for (int i = 0; i < quantidade; i++) {
			final double dist = 6.0 - i * 0.8;
			agendar(level, 6 + i * 7, () -> {
				if (p.isRemoved()) {
					return;
				}
				Vec3 atras = pontoRelativo(p, 180, Math.max(1.5, dist));
				BlockPos chao = BlockPos.containing(atras.x, p.getY() - 0.5, atras.z);
				BlockState estado = level.getBlockState(chao);
				if (estado.isAir()) {
					estado = level.getBlockState(p.blockPosition().below());
				}
				SoundEvent som = estado.getSoundType().getStepSound();
				level.playSound(null, atras.x, p.getY(), atras.z, som, SoundSource.HOSTILE, 0.5F, 0.85F);
			});
		}
	}

	/**
	 * Passos que percorrem o caminho REAL do jogador. O som avança do ponto mais antigo para o mais
	 * novo do Rastro e termina antes de alcançá-lo. Às vezes não existe criatura nenhuma depois.
	 */
	@Nullable
	private static SinalResultado passosNoRastro(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		List<Rastro.Ponto> candidatos = new ArrayList<>();
		for (Rastro.Ponto pt : e.rastro.comIdade(seg, 12, 180)) {
			double d = Math.sqrt(distanciaSqr(p, pt.x(), pt.z()));
			if (d >= 7 && d <= 40) {
				candidatos.add(pt);
			}
		}
		if (candidatos.size() < 3) {
			return null;
		}

		int qtd = Math.min(candidatos.size(), 3 + rnd.nextInt(3));
		int janela = Math.min(candidatos.size(), Math.max(qtd, 12));
		int inicioJanela = candidatos.size() - janela;
		int deslocamento = janela > qtd ? rnd.nextInt(Math.max(1, janela - qtd + 1)) : 0;
		int inicio = Math.min(candidatos.size() - qtd, inicioJanela + deslocamento);
		Rastro.Ponto primeiro = candidatos.get(inicio);
		Rastro.Ponto ultimo = candidatos.get(inicio + qtd - 1);

		for (int i = 0; i < qtd; i++) {
			final Rastro.Ponto pt = candidatos.get(inicio + i);
			final int n = i;
			agendar(level, 4 + i * 9, () -> {
				if (p.isRemoved()) {
					return;
				}
				BlockPos chao = BlockPos.containing(pt.x(), pt.y() - 0.35, pt.z());
				BlockState estado = level.getBlockState(chao);
				if (estado.isAir()) {
					estado = level.getBlockState(chao.below());
				}
				float volume = volumePara(p, pt.x(), pt.y(), pt.z(), 0.34F + n * 0.03F);
				level.playSound(null, pt.x(), pt.y(), pt.z(), estado.getSoundType().getStepSound(),
						SoundSource.HOSTILE, volume, 0.76F + n * 0.025F);
			});
		}

		Vec3 fonte = new Vec3(ultimo.x(), ultimo.y(), ultimo.z());
		double obs = limitar(1 - distancia(p, fonte) / 42.0, 0.25, 0.9);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"SEGUIDOR pontos=%d idade=%ds->%ds dist=%.1f->%.1f semCriatura=sim", qtd,
				seg - primeiro.seg(), seg - ultimo.seg(), distancia(p, new Vec3(primeiro.x(), primeiro.y(), primeiro.z())),
				distancia(p, fonte)));
		return new SinalResultado(fonte, obs);
	}

	/**
	 * Vestígio visual curto no caminho antigo do jogador. Usa partículas vanilla discretas para não
	 * depender de pipeline gráfico extra nesta alpha. As pegadas terminam antes de chegar ao jogador.
	 */
	@Nullable
	private static SinalResultado pegadasNoRastro(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		List<Rastro.Ponto> candidatos = new ArrayList<>();
		for (Rastro.Ponto pt : e.rastro.comIdade(seg, 10, 210)) {
			double d = Math.sqrt(distanciaSqr(p, pt.x(), pt.z()));
			if (d >= 5 && d <= 30) {
				candidatos.add(pt);
			}
		}
		if (candidatos.size() < 4) {
			return null;
		}

		int qtd = Math.min(candidatos.size(), 4 + rnd.nextInt(4));
		int maxInicio = Math.max(0, candidatos.size() - qtd);
		int inicio = maxInicio == 0 ? 0 : rnd.nextInt(maxInicio + 1);
		boolean inverter = rnd.nextBoolean();
		Rastro.Ponto primeiro = candidatos.get(inverter ? inicio + qtd - 1 : inicio);
		Rastro.Ponto ultimo = candidatos.get(inverter ? inicio : inicio + qtd - 1);

		for (int i = 0; i < qtd; i++) {
			int indice = inverter ? inicio + qtd - 1 - i : inicio + i;
			final Rastro.Ponto pt = candidatos.get(indice);
			final int n = i;
			agendar(level, 4 + i * 7, () -> {
				if (p.isRemoved()) {
					return;
				}
				level.sendParticles(ParticleTypes.ASH, pt.x(), pt.y() + 0.08, pt.z(),
						5 + (n % 2), 0.22, 0.025, 0.22, 0.002);
				if (n == 0 || n == qtd - 1 || n % 2 == 0) {
					BlockPos chao = BlockPos.containing(pt.x(), pt.y() - 0.3, pt.z());
					BlockState st = level.getBlockState(chao);
					if (st.isAir()) {
						st = level.getBlockState(chao.below());
					}
					level.playSound(null, pt.x(), pt.y(), pt.z(), st.getSoundType().getStepSound(),
							SoundSource.HOSTILE, volumePara(p, pt.x(), pt.y(), pt.z(), 0.18F), 0.67F + n * 0.025F);
				}
			});
		}

		BlockPos fim = BlockPos.containing(ultimo.x(), ultimo.y(), ultimo.z());
		boolean marcouVestigio = !e.forcando;
		if (marcouVestigio) {
			Vestigios.de(p).registrar(fim, Vestigios.Tipo.PEGADAS, seg);
		}
		Vec3 fonte = new Vec3(ultimo.x(), ultimo.y(), ultimo.z());
		double obs = limitar(1 - distancia(p, fonte) / 34.0, 0.25, 0.92);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"PEGADAS pontos=%d sentido=%s idade=%ds->%ds distFim=%.1f semCriatura=sim vestigio=%s", qtd,
				inverter ? "ANTIGO" : "RECENTE", seg - primeiro.seg(), seg - ultimo.seg(), distancia(p, fonte),
				marcouVestigio ? "sim" : "nao"));
		return new SinalResultado(fonte, obs);
	}

	/**
	 * Repete um som que VOCÊ fez. lugarFixo: de onde vem (o lugar da ação ou do rastro);
	 * sem ele, vem da direção de casa ou de um ponto perto. Porta: abre... e, um tempo depois, fecha.
	 */
	static Vec3 eco(ServerLevel level, ServerPlayer p, SoundEvent som, boolean porta, boolean perto,
			@Nullable Vec3 deCasa, @Nullable Vec3 lugarFixo) {
		RandomSource rnd = level.getRandom();
		Vec3 lugar;
		if (lugarFixo != null) {
			lugar = lugarFixo;
		} else if (deCasa != null) {
			// O som vem da direção da sua casa.
			double dx = deCasa.x - p.getX();
			double dz = deCasa.z - p.getZ();
			double d = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
			lugar = new Vec3(p.getX() + dx / d * 12, p.getY(), p.getZ() + dz / d * 12);
		} else {
			lugar = pontoRelativo(p, rnd.nextInt(360), perto ? 5 + rnd.nextInt(3) : 10 + rnd.nextInt(7));
		}
		double y = lugarFixo != null ? lugarFixo.y : p.getY() - 3 + rnd.nextInt(4);
		// Volume acima de 1 aumenta o ALCANCE (16 blocos x volume), não a altura do som.
		float volume = volumePara(p, lugar.x, lugar.y, lugar.z, 0.9F);
		if (porta) {
			level.playSound(null, lugar.x, y, lugar.z, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, volume, 0.95F);
			agendar(level, 18 + rnd.nextInt(14), () ->
					level.playSound(null, lugar.x, y, lugar.z, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, volume, 0.95F));
		} else {
			int golpes = 2 + rnd.nextInt(3);
			for (int i = 0; i < golpes; i++) {
				agendar(level, i * (6 + rnd.nextInt(5)), () ->
						level.playSound(null, lugar.x, y, lugar.z, som, SoundSource.HOSTILE, volume, 0.9F));
			}
		}
		return new Vec3(lugar.x, y, lugar.z);
	}

	/** Guarda uma ação. Minerar num lugar só vira UMA lembrança (atualizada), não cinquenta. */
	private static void registrarAcao(EstadoJogador e, EstadoJogador.TipoAcao tipo, SoundEvent som, BlockPos pos, long seg) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		EstadoJogador.Acao ultima = e.acoes.peekLast();
		if (ultima != null && ultima.tipo() == tipo && seg - ultima.seg() < 30
				&& Math.abs(ultima.x() - x) <= 4 && Math.abs(ultima.y() - y) <= 4 && Math.abs(ultima.z() - z) <= 4) {
			e.acoes.removeLast();
		}
		e.acoes.addLast(new EstadoJogador.Acao(tipo, som, x, y, z, seg));
		while (e.acoes.size() > 10) {
			e.acoes.removeFirst();
		}
	}

	/** Telemetria: uma linha por eco, ligando o som à ação passada que ele repete. */
	static void logEco(ServerPlayer p, String prefixo, EstadoJogador.Acao acao, Vec3 posSom, String motivo, long seg) {
		if (!Depuracao.ativo) {
			return;
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT, "%s origemAcao=%s idade=%ds posOriginal=%s posSom=%s %s distSom=%.1f",
				prefixo, acao.tipo(), seg - acao.seg(), pos(acao.x(), acao.y(), acao.z()),
				pos(posSom.x, posSom.y, posSom.z), motivo, distancia(p, posSom)));
	}

	/** Uma das suas ações recentes (só chamar com a lista não vazia). */
	private static EstadoJogador.Acao sortearAcao(EstadoJogador e, RandomSource rnd) {
		List<EstadoJogador.Acao> lista = new ArrayList<>(e.acoes);
		return lista.get(rnd.nextInt(lista.size()));
	}

	/**
	 * De onde deve vir o eco desta ação: do lugar onde você a fez (se faz pelo menos 45 s e fica a 8-24 blocos)
	 * ou, se usarRastro, de um ponto por onde você passou. Escreve em nota o que escolheu.
	 */
	@Nullable
	static Vec3 lugarParaEco(ServerPlayer p, EstadoJogador e, EstadoJogador.Acao acao, long seg,
			boolean usarRastro, StringBuilder nota) {
		double d = Math.sqrt(distanciaSqr(p, acao.x(), acao.z()));
		long idade = seg - acao.seg();
		if (idade >= 45 && d >= 8 && d <= 24) {
			nota.append(String.format(Locale.ROOT, "motivo=LUGAR_DA_ACAO distOriginal=%.0f", d));
			return new Vec3(acao.x(), acao.y(), acao.z());
		}
		if (usarRastro) {
			Rastro.Ponto pt = pontoDoRastro(p, e, seg, 20, 300, 8, 24, false);
			if (pt != null) {
				nota.append(String.format(Locale.ROOT, "motivo=RASTRO idadeRastro=%ds", seg - pt.seg()));
				return new Vec3(pt.x(), pt.y(), pt.z());
			}
		}
		return null;
	}

	/** Um único passo, bem perto. E depois nada. */
	private static void passoUnico(ServerLevel level, ServerPlayer p) {
		Vec3 atras = pontoRelativo(p, 180, 2.5);
		BlockState estado = level.getBlockState(p.blockPosition().below());
		level.playSound(null, atras.x, p.getY(), atras.z, estado.getSoundType().getStepSound(), SoundSource.HOSTILE, 0.35F, 0.8F);
	}

	/** Alguém bate na porta. Três vezes. */
	@Nullable
	private static BlockPos bater(ServerLevel level, ServerPlayer p) {
		BlockPos porta = escolherPorta(level, p);
		if (porta == null) {
			return null;
		}
		// A porta de sempre pode estar a até 24 blocos: o volume escala com a distância para a batida chegar.
		float volume = volumePara(p, porta.getX() + 0.5, porta.getY() + 0.5, porta.getZ() + 0.5, 0.35F);
		for (int i = 0; i < 3; i++) {
			agendar(level, i * 9, () -> level.playSound(null, porta.getX() + 0.5, porta.getY() + 0.5, porta.getZ() + 0.5,
					SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, volume, 1.4F));
		}
		return porta;
	}

	/** Ele repete algo que VOCÊ escreveu. */
	private static boolean ecoDaFala(ServerPlayer p, EstadoJogador e, RandomSource rnd) {
		if (e.falas.isEmpty()) {
			return false;
		}
		List<String> lista = new ArrayList<>(e.falas);
		String fala = lista.get(rnd.nextInt(lista.size())).toLowerCase(Locale.ROOT);
		p.sendOverlayMessage(Component.literal("..." + fala + "...").withStyle(s -> s.withColor(0x5A5A5A).withItalic(true)));
		ModSons.tocarNaCabeca(p, ModSons.Som.RESPIRACAO, 0.4F, 0.78F + rnd.nextFloat() * 0.1F);
		return true;
	}

	/** Ele fica parado exatamente onde você morreu. */
	private static boolean invocarNoTumulo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, PedidoManifestacao pedido) {
		BlockPos chao = acharChao(level, m.get(Memoria.MORTE_X) + 0.5, m.get(Memoria.MORTE_Y), m.get(Memoria.MORTE_Z) + 0.5);
		if (chao == null || naTela(p, chao)) {
			return false;
		}
		pedido = pedido.comNota("TUMULO");
		criar(level, p, e, chao, HospedeEntity.Modo.OBSERVAR, 20 * 30, 1.0, pedido);
		return true;
	}

	private static void sussurro(ServerPlayer p, int fase, RandomSource rnd) {
		int limite = fase >= 3 ? 10 : 6;
		int n = rnd.nextInt(limite);
		p.sendOverlayMessage(Component.translatable("message.sussurros.sussurro." + n, p.getName())
				.withStyle(s -> s.withColor(0x5A5A5A).withItalic(true)));
		// O texto vem com uma voz "dentro da cabeça": sem direção e só para este jogador. Na maioria das
		// vezes é um sussurro que não dá para entender; nas outras, só uma respiração.
		if (rnd.nextFloat() < 0.7F) {
			ModSons.tocarNaCabeca(p, ModSons.Som.SUSSURRO_VOZ, 0.55F, 0.9F + rnd.nextFloat() * 0.16F);
		} else {
			ModSons.tocarNaCabeca(p, ModSons.Som.RESPIRACAO, 0.5F, 0.82F + rnd.nextFloat() * 0.12F);
		}
	}

	/**
	 * Um pedaço do tema, assobiado de longe, de algum lugar que o jogador não está vendo. Só ele ouve.
	 * Devolve de onde veio.
	 */
	static Vec3 assobiar(ServerLevel level, ServerPlayer p, RandomSource rnd) {
		double angulo = (rnd.nextBoolean() ? 1 : -1) * (95 + rnd.nextDouble() * 85);
		Vec3 ponto = pontoRelativo(p, angulo, 24 + rnd.nextDouble() * 18);
		double y = p.getY() + 1.0;
		// Volume acima de 1 só dá alcance: de longe e baixo, como alguém que não sabe que está sendo ouvido.
		ModSons.tocarPara(p, ponto.x, y, ponto.z, ModSons.Som.ASSOBIO, volumePara(p, ponto.x, y, ponto.z, 0.5F),
				0.95F + rnd.nextFloat() * 0.08F);
		return new Vec3(ponto.x, y, ponto.z);
	}

	/**
	 * Silêncio de verdade: corta a música e o som ambiente deste jogador e adia os sons de fundo dos mobs em
	 * volta. Nada fica salvo no mundo. O contraste é o silêncio, não um acorde de susto.
	 */
	static void emudecer(ServerLevel level, ServerPlayer p, int segundos, String motivo) {
		p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
		p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
		int mobs = 0;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0, 10.0, 24.0), Mob::isAlive)) {
			// O contador sobe 1 por tick e o mob só "fala" quando ele passa de um sorteio de 0 a 999.
			// Voltar o contador adia a fala, e ele não é salvo com o mob.
			mob.ambientSoundTime = Math.min(mob.ambientSoundTime, -segundos * 20);
			mobs++;
		}
		Depuracao.log(p, level.getGameTime() / 20, "SILENCIO_REAL motivo=" + motivo + " duracao=" + segundos + "s mobs=" + mobs);
	}

	/**
	 * Falso positivo contextual. O jogador recebe um sinal coerente com o lugar, mas nenhuma criatura nasce.
	 * A ideia e quebrar a regra mental "som estranho = procure o Hospede".
	 */
	private static SinalResultado sinalFalso(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, RandomSource rnd) {
		Vec3 lugar;
		String tipo;
		switch (e.contexto) {
			case CASA -> {
				BlockPos porta = portaHabitualExistente(level, p);
				if (porta != null && rnd.nextFloat() < 0.72F) {
					lugar = Vec3.atCenterOf(porta);
					tipo = "PORTA_SEM_MOVER";
					float vol = volumePara(p, lugar.x, lugar.y, lugar.z, 0.55F);
					ModSons.tocar(level, lugar.x, lugar.y, lugar.z, ModSons.Som.MADEIRA, vol, 0.88F);
					if (rnd.nextFloat() < 0.28F) {
						agendar(level, 16 + rnd.nextInt(18), () -> ModSons.tocar(level, lugar.x, lugar.y, lugar.z,
							ModSons.Som.ESTALO, vol, 0.9F));
					}
				} else {
					lugar = pontoRelativo(p, 145 + rnd.nextDouble() * 70, 7 + rnd.nextInt(7));
					tipo = "RESPIRACAO_DA_CASA";
					ModSons.tocar(level, lugar.x, p.getY() + 1.0, lugar.z,
						rnd.nextBoolean() ? ModSons.Som.RESPIRACAO : ModSons.Som.PANO,
						volumePara(p, lugar.x, lugar.y, lugar.z, 0.45F), 0.9F);
				}
			}
			case SUBSOLO -> {
				EstadoJogador.Acao acao = CenaAlgoNoTunel.sortearQuebraRecente(e, seg, rnd);
				Vec3 ecoCurto = null;
				if (acao != null && rnd.nextFloat() < 0.58F) {
					Rastro.Ponto pt = pontoDoRastro(p, e, seg, 20, 300, 10, 28, true);
					if (pt != null) {
						ecoCurto = new Vec3(pt.x(), pt.y(), pt.z());
					} else {
						// Sem ponto do Rastro, o lugar da quebra só serve se ainda estiver ao alcance do ouvido
						// (um log real mostrou este som tocando a 236 blocos).
						Vec3 daAcao = new Vec3(acao.x(), acao.y(), acao.z());
						if (distancia(p, daAcao) <= ALCANCE_ACAO_ANTIGA) {
							ecoCurto = daAcao;
						}
					}
				}
				if (ecoCurto != null) {
					lugar = ecoCurto;
					tipo = "ECO_CURTO_DA_ACAO";
					float vol = volumePara(p, lugar.x, lugar.y, lugar.z, 0.55F);
					level.playSound(null, lugar.x, lugar.y, lugar.z, acao.som(), SoundSource.HOSTILE, vol, 0.82F);
				} else {
					lugar = pontoRelativo(p, 130 + rnd.nextDouble() * 100, 12 + rnd.nextInt(12));
					tipo = "COISA_NO_TUNEL";
					ModSons.tocar(level, lugar.x, p.getY(), lugar.z,
						rnd.nextBoolean() ? ModSons.Som.ARRASTO : ModSons.Som.MADEIRA,
						volumePara(p, lugar.x, lugar.y, lugar.z, 0.6F), 0.84F);
				}
			}
			case ABERTO -> {
				Rastro.Ponto pt = pontoDoRastro(p, e, seg, 15, 240, 14, 36, true);
				if (pt != null) {
					lugar = new Vec3(pt.x(), pt.y() + 0.5, pt.z());
					tipo = "RUIDO_NO_RASTRO";
				} else {
					lugar = pontoRelativo(p, 70 + rnd.nextDouble() * 110, 18 + rnd.nextInt(15));
					tipo = "RUIDO_DISTANTE";
				}
				ModSons.Som som = rnd.nextFloat() < 0.18F ? ModSons.Som.RESPIRACAO
						: (rnd.nextBoolean() ? ModSons.Som.ESTALO : ModSons.Som.PANO);
				ModSons.tocar(level, lugar.x, lugar.y, lugar.z, som, volumePara(p, lugar.x, lugar.y, lugar.z, 0.5F), 0.9F);
			}
			case FLORESTA -> {
				Rastro.Ponto pt = pontoDoRastro(p, e, seg, 12, 220, 10, 30, true);
				if (pt != null) {
					lugar = new Vec3(pt.x(), pt.y() + 0.8, pt.z());
					tipo = "ENTRE_AS_ARVORES";
				} else {
					lugar = pontoRelativo(p, 80 + rnd.nextDouble() * 120, 12 + rnd.nextInt(14));
					tipo = "GALHO_FORA_DA_VISAO";
				}
				ModSons.Som som = rnd.nextFloat() < 0.55F ? ModSons.Som.ESTALO : ModSons.Som.PANO;
				ModSons.tocar(level, lugar.x, lugar.y, lugar.z, som, volumePara(p, lugar.x, lugar.y, lugar.z, 0.46F),
						0.86F + rnd.nextFloat() * 0.10F);
			}
			case OUTRO -> {
				lugar = pontoRelativo(p, 110 + rnd.nextDouble() * 140, 8 + rnd.nextInt(12));
				tipo = "RUIDO_SEM_FONTE";
				ModSons.tocar(level, lugar.x, p.getY() + 0.8, lugar.z,
					rnd.nextBoolean() ? ModSons.Som.PANO : ModSons.Som.ESTALO,
					volumePara(p, lugar.x, lugar.y, lugar.z, 0.45F), 0.92F);
			}
			default -> throw new IllegalStateException("Contexto desconhecido: " + e.contexto);
		}
		// O mesmo silêncio que anuncia uma aparição, de vez em quando sem aparição nenhuma.
		if (rnd.nextFloat() < CHANCE_SILENCIO_FALSO) {
			emudecer(level, p, 15, "SINAL");
		}
		double obs = limitar(1 - distancia(p, lugar) / 34.0, 0.25, 0.85);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"SINAL contexto=%s tipo=%s semCriatura=sim pos=%s dist=%.1f",
				e.contexto, tipo, pos(lugar.x, lugar.y, lugar.z), distancia(p, lugar)));
		return new SinalResultado(lugar, obs);
	}

	@Nullable
	static BlockPos acharPorta(ServerLevel level, ServerPlayer p) {
		EstadoJogador e = estado(p);
		atualizarCacheAmbiente(level, p, e);
		for (BlockPos pos : e.portas) {
			BlockState s = level.getBlockState(pos);
			if (s.getBlock() instanceof DoorBlock && !s.is(Blocks.IRON_DOOR) && s.hasProperty(BlockStateProperties.OPEN)
					&& distancia(p, Vec3.atCenterOf(pos)) <= 11
					&& !pontoNaFrente(p, Vec3.atCenterOf(pos), 0.5)) {
				return pos;
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos mexerNaPorta(ServerLevel level, ServerPlayer p) {
		BlockPos pos = escolherPorta(level, p);
		if (pos == null) {
			return null;
		}
		alternarPorta(level, pos);
		return pos;
	}

	/** Abre a porta se está fechada, fecha se está aberta. Com o som de sempre. */
	static void alternarPorta(ServerLevel level, BlockPos pos) {
		BlockState estado = level.getBlockState(pos);
		if (!(estado.getBlock() instanceof DoorBlock) || !estado.hasProperty(BlockStateProperties.OPEN)) {
			return;
		}
		boolean aberta = estado.getValue(BlockStateProperties.OPEN);
		level.setBlock(pos, estado.setValue(BlockStateProperties.OPEN, !aberta), 10);
		level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				aberta ? SoundEvents.WOODEN_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.9F);
	}

	@Nullable
	private static BlockPos acharTochaAtras(ServerLevel level, ServerPlayer p, int raio) {
		EstadoJogador e = estado(p);
		atualizarCacheAmbiente(level, p, e);
		for (BlockPos pos : e.tochas) {
			BlockState s = level.getBlockState(pos);
			if ((s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH))
					&& distancia(p, Vec3.atCenterOf(pos)) <= raio + 1
					&& !pontoNaFrente(p, Vec3.atCenterOf(pos), 0.0)) {
				return pos;
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos roubarTocha(ServerLevel level, ServerPlayer p) {
		BlockPos pos = acharTochaAtras(level, p, 12);
		if (pos == null) {
			return null;
		}
		// 0.9: "levar" a tocha deixou de tirá-la do mundo. Ela some para o jogador por alguns minutos
		// (miragem), ou até ele clicar no lugar. A construção de ninguém perde um bloco por causa do mod.
		if (!Miragem.mostrar(level, p, pos, Blocks.AIR.defaultBlockState(), 20L * (180 + level.getRandom().nextInt(121)), 0, "TOCHA_LEVADA")) {
			return null;
		}
		ModSons.tocarEventoPara(p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.25F, 0.7F);
		estado(p).tochas.remove(pos);
		return pos;
	}

	/** Apaga uma tocha sem destruir a construcao e a restaura se o espaco continuar livre. */
	@Nullable
	private static BlockPos piscarTocha(ServerLevel level, ServerPlayer p, int duracaoTicks) {
		BlockPos pos = acharTochaAtras(level, p, 12);
		if (pos == null) {
			return null;
		}
		BlockState original = level.getBlockState(pos);
		if (!(original.is(Blocks.TORCH) || original.is(Blocks.WALL_TORCH))) {
			return null;
		}
		// 0.9: miragem. A tocha some só para este jogador (a luz some junto) e volta sozinha; no mundo ela
		// nunca saiu do lugar, então não há o que restaurar se o mundo fechar no meio.
		if (!Miragem.mostrar(level, p, pos, Blocks.AIR.defaultBlockState(), duracaoTicks, 0, "TOCHA_PISCA")) {
			return null;
		}
		ModSons.tocarEventoPara(p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.22F, 0.72F);
		estado(p).tochas.remove(pos);
		return pos;
	}

	/**
	 * O Perfil deixa de mexer só em pesos: ele também muda a geometria das aparições.
	 * Jogador cauteloso, que vive checando as costas, recebe mais aparições laterais; quem confronta
	 * o Hóspede recebe ângulos mais traseiros e difíceis de encarar imediatamente.
	 */
	static double[] angulosPresencaAdaptativa(Memoria m, EstadoJogador e, PedidoManifestacao pedido) {
		double min = 55;
		double max = 80;
		String estrategia = "PADRAO";
		if (Perfil.alto(m, Perfil.Traco.CAUTELA)) {
			min = 56;
			max = 70;
			estrategia = "CONTORNAR_CAUTELA";
		} else if (Perfil.alto(m, Perfil.Traco.CONFRONTO)) {
			min = 78;
			max = 118;
			estrategia = "NEGAR_CONFRONTO";
		} else if (Perfil.alto(m, Perfil.Traco.FUGA)) {
			min = 62;
			max = 92;
			estrategia = "INTERCEPTAR_FUGA";
		}
		if (e.contexto == ContextoMundo.Tipo.FLORESTA) {
			min = Math.max(min, 62);
			max = Math.max(max, 100);
			estrategia += "+FLORESTA";
		}
		// TODO: a nota da estratégia já era uma reatribuição local sem efeito para o chamador; bug pré-existente.

		return new double[] {min, max};
	}

	/**
	 * Coloca o Hóspede em algum lugar ao redor do jogador.
	 * angMin/angMax: ângulo (em graus) em relação a para onde o jogador olha. 180 = bem atrás.
	 */
	static boolean invocar(ServerLevel level, ServerPlayer p, EstadoJogador e, HospedeEntity.Modo modo,
			double angMin, double angMax, double distMin, double distMax,
			int duracao, double velocidade, boolean preferirEscuro, PedidoManifestacao pedido) {
		return invocar(level, p, e, modo, angMin, angMax, distMin, distMax, duracao, velocidade, preferirEscuro,
				HospedeEntity.DIST_SUMIR_PADRAO, pedido);
	}

	/** distSumir: se o jogador chegar mais perto que isso (modo OBSERVAR), ele não está mais lá. */
	static boolean invocar(ServerLevel level, ServerPlayer p, EstadoJogador e, HospedeEntity.Modo modo,
			double angMin, double angMax, double distMin, double distMax,
			int duracao, double velocidade, boolean preferirEscuro, double distSumir, PedidoManifestacao pedido) {
		return invocar(level, p, e, modo, angMin, angMax, distMin, distMax, duracao, velocidade, preferirEscuro,
				distSumir, false, pedido);
	}

	/**
	 * exigirVisivel: nas primeiras tentativas, só aceita lugares onde ele poderia ser ENXERGADO
	 * (perto, ou com luz suficiente). Regra geral da v0.4.2: ele nunca nasce dentro da sua tela.
	 */
	static boolean invocar(ServerLevel level, ServerPlayer p, EstadoJogador e, HospedeEntity.Modo modo,
			double angMin, double angMax, double distMin, double distMax,
			int duracao, double velocidade, boolean preferirEscuro, double distSumir, boolean exigirVisivel, PedidoManifestacao pedido) {
		Aparicao.Config cfg = new Aparicao.Config(
				angMin, angMax, distMin, distMax, exigirVisivel ? 20 : 18,
				exigirVisivel, false, preferirEscuro, true, true, 1,
				2.2, 0.9, exigirVisivel ? 1.4 : 0.8, (distMin + distMax) / 2.0);
		Aparicao.Candidato candidato = Aparicao.buscarAoRedor(level, p, e, cfg);
		if (candidato == null) {
			return false;
		}
		pedido = pedido.comNota("APARICAO2 " + candidato.resumo());
		criar(level, p, e, candidato.chao(), modo, duracao, velocidade, distSumir, pedido);
		if (!e.forcando) {
			Aparicao.registrar(e, candidato);
		}
		return true;
	}


	/**
	 * Variante de PRESENCA que so aceita pontos onde ha alguma cobertura entre ele e o jogador,
	 * mas em que a posicao ainda seria visivel se o jogador virasse. E o equivalente a "metade atras da arvore".
	 */
	static boolean invocarComCobertura(ServerLevel level, ServerPlayer p, EstadoJogador e, HospedeEntity.Modo modo,
			double angMin, double angMax, double distMin, double distMax, int duracao, double distSumir, PedidoManifestacao pedido) {
		Aparicao.Config cfg = new Aparicao.Config(
				angMin, angMax, distMin, distMax, 22,
				true, true, true, true, true, 1,
				3.0, 1.0, 1.3, (distMin + distMax) / 2.0);
		Aparicao.Candidato candidato = Aparicao.buscarAoRedor(level, p, e, cfg);
		if (candidato == null) {
			return false;
		}
		pedido = pedido.comNota("APARICAO2_COBERTURA " + candidato.resumo());
		criar(level, p, e, candidato.chao(), modo, duracao, 1.0, distSumir, pedido);
		if (!e.forcando) {
			Aparicao.registrar(e, candidato);
		}
		return true;
	}

	/**
	 * Tenta materializar o Hóspede perto do ponto exato em que um marco persistente foi criado.
	 * A memória deixa de ser apenas "este chunk": quando o terreno ainda permite, ele reutiliza o lugar.
	 */
	static boolean invocarPertoDoMarco(ServerLevel level, ServerPlayer p, EstadoJogador e, BlockPos marco,
			HospedeEntity.Modo modo, int duracao, double distSumir, PedidoManifestacao pedido) {
		if (marco == null) {
			return false;
		}
		RandomSource rnd = level.getRandom();
		BlockPos melhor = null;
		double melhorNota = -1;
		double melhorDistMarco = 0;
		boolean melhorCobertura = false;
		for (int i = 0; i < 16; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0;
			double raio = i < 4 ? 0.5 + rnd.nextDouble() * 1.5 : 2.0 + rnd.nextDouble() * 5.0;
			double x = marco.getX() + 0.5 + Math.cos(a) * raio;
			double z = marco.getZ() + 0.5 + Math.sin(a) * raio;
			BlockPos chao = acharChao(level, x, marco.getY(), z);
			if (chao == null || naTela(p, chao) || emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			double distJogador = Math.sqrt(distanciaSqr(p, chao.getX() + 0.5, chao.getZ() + 0.5));
			if (distJogador < 8 || distJogador > 34 || !pontoVisivel(level, p, chao)) {
				continue;
			}
			boolean cobertura = temCobertura(level, p, chao);
			double mdx = chao.getX() - marco.getX();
			double mdy = chao.getY() - marco.getY();
			double mdz = chao.getZ() - marco.getZ();
			double distMarco = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
			double nota = (cobertura ? 2.0 : 0.0) + Math.max(0, 1.5 - distMarco / 6.0);
			int luz = luzEfetiva(level, chao.above());
			if (luz >= 2 && luz <= 8) {
				nota += 0.6;
			}
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
				melhorDistMarco = distMarco;
				melhorCobertura = cobertura;
			}
		}
		if (melhor == null) {
			return false;
		}
		pedido = pedido.comNota(String.format(Locale.ROOT, "MARCO_EXATO distMarco=%.1f cobertura=%s",
				melhorDistMarco, melhorCobertura ? "sim" : "nao"));
		criar(level, p, e, melhor, modo, duracao, 1.0, distSumir, pedido);
		return true;
	}

	private static boolean invocarNaBordaDaZona(ServerLevel level, ServerPlayer p, EstadoJogador e, int ousadia, PedidoManifestacao pedido) {
		RandomSource rnd = level.getRandom();
		for (int tentativa = 0; tentativa < 14; tentativa++) {
			double ang = rnd.nextDouble() * Math.PI * 2;
			double r = e.zonaRaio + 3 + rnd.nextDouble() * 2;
			double x = e.zonaX + Math.cos(ang) * r;
			double z = e.zonaZ + Math.sin(ang) * r;
			BlockPos chao = acharChao(level, x, e.zonaY, z);
			if (chao != null) {
				pedido = pedido.comNota("BORDA_DA_VELA");
				criar(level, p, e, chao, HospedeEntity.Modo.ESPERAR, 20 * 150, 1.0 + ousadia * 0.03, pedido);
				return true;
			}
		}
		return false;
	}

	static void criar(ServerLevel level, ServerPlayer p, EstadoJogador e, BlockPos chao,
			HospedeEntity.Modo modo, int duracao, double velocidade, PedidoManifestacao pedido) {
		criar(level, p, e, chao, modo, duracao, velocidade, HospedeEntity.DIST_SUMIR_PADRAO, pedido);
	}

	static void criar(ServerLevel level, ServerPlayer p, EstadoJogador e, BlockPos chao,
			HospedeEntity.Modo modo, int duracao, double velocidade, double distSumir, PedidoManifestacao pedido) {
		Memoria m = Memoria.de(p);
		int ousadia = Math.min(10, m.get(Memoria.VEZES_VISTO) / 2 + m.get(Memoria.VEZES_FERIDO));
		HospedeEntity h = new HospedeEntity(ModEntidades.HOSPEDE, level);
		h.setPos(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
		h.configurar(p, modo, duracao, ousadia, velocidade);
		h.definirDistanciaSumir(distSumir);
		h.definirOrigem(pedido.origem(), pedido.evento()); // de comando: nada do que acontece com ele fica na memória
		h.definirId(novoIdManifestacao());
		level.addFreshEntity(h);
		e.criatura = h;
		// Ele nasce fora da tela, e na maioria das vezes o mundo emudece antes de o jogador virar e ver.
		// O vulto distante é mais frequente e mais discreto: emudece na metade das vezes das outras aparições.
		float chanceSilencio = modo == HospedeEntity.Modo.VULTO ? CHANCE_SILENCIO_APARICAO / 2 : CHANCE_SILENCIO_APARICAO;
		if (level.getRandom().nextFloat() < chanceSilencio) {
			emudecer(level, p, 25, "APARICAO");
		}
		if (Depuracao.ativo) {
			String motivo = pedido.nota().isEmpty() ? "NORMAL" : pedido.nota();
			if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA) {
				motivo = "CENA_CASA+" + motivo;
			} else if (e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA) {
				motivo = "CENA_TUNEL+" + motivo;
			} else if (e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA) {
				motivo = "CENA_CAMPO+" + motivo;
			} else if (e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA) {
				motivo = "CENA_MARCO+" + motivo;
			} else if (e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
				motivo = "CENA_JANELA+" + motivo;
			}
			Depuracao.log(p, level.getGameTime() / 20, String.format(Locale.ROOT,
					"HOSPEDE id=%s criado origem=%s evento=%s modo=%s pos=%s dist=%.1f motivoPosicao=%s cena=%s",
					h.getIdManifestacao(), pedido.origem(), pedido.evento(), modo, pos(h.getX(), h.getY(), h.getZ()),
					Math.sqrt(h.distanceToSqr(p)), motivo, cenaAtiva(e)));
		}
	}

	/** Tenta colocar a manifestação perto da Isca Pálida, respeitando tela, terreno e zona calma. */
	private static boolean invocarPertoDaIsca(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int duracao, PedidoManifestacao pedido) {
		if (!e.iscaAtiva || level.getGameTime() >= e.iscaAteTick) {
			return false;
		}
		double dxp = p.getX() - e.iscaX;
		double dzp = p.getZ() - e.iscaZ;
		double distJogador = Math.sqrt(dxp * dxp + dzp * dzp);
		if (distJogador < 7 || distJogador > 42) {
			return false;
		}
		RandomSource rnd = level.getRandom();
		BlockPos melhor = null;
		boolean melhorCobertura = false;
		double melhorNota = -999;
		for (int tentativa = 0; tentativa < 14; tentativa++) {
			double ang = rnd.nextDouble() * Math.PI * 2;
			double r = 1.5 + rnd.nextDouble() * 4.5;
			double x = e.iscaX + Math.cos(ang) * r;
			double z = e.iscaZ + Math.sin(ang) * r;
			BlockPos chao = acharChao(level, x, e.iscaY, z);
			if (chao == null || naTela(p, chao) || emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			double d = Math.sqrt(distanciaSqr(p, chao.getX() + 0.5, chao.getZ() + 0.5));
			if (d < 7 || d > 44) {
				continue;
			}
			boolean cobertura = temCobertura(level, p, chao);
			double nota = rnd.nextDouble() + (cobertura ? 2.0 : 0.0) - Math.abs(r - 3.0) * 0.08;
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
				melhorCobertura = cobertura;
			}
		}
		if (melhor == null) {
			return false;
		}
		pedido = pedido.comNota(String.format(Locale.ROOT, "ISCA cobertura=%s distIsca=%.1f", melhorCobertura,
				Math.sqrt(Math.pow(melhor.getX() + 0.5 - e.iscaX, 2) + Math.pow(melhor.getZ() + 0.5 - e.iscaZ, 2))));
		criar(level, p, e, melhor, HospedeEntity.Modo.OBSERVAR, duracao, 1.0, HospedeEntity.DIST_SUMIR_PADRAO, pedido);
		e.iscaAtiva = false;
		m.add(Memoria.ISCAS_ATENDIDAS, 1);
		m.salvar();
		Depuracao.log(p, level.getGameTime() / 20, "ISCA atendida manifestacao=" + manifestacaoAtiva(e)
				+ " pos=" + pos(melhor.getX(), melhor.getY(), melhor.getZ()));
		return true;
	}

	// =====================================================================
	// Rastro, espreita e anúncio (v0.4.2)
	// =====================================================================

	/** Um ponto do rastro com a idade (s) e a distância (blocos) pedidas. foraDaTela: só o que você não está vendo agora. */
	static Rastro.@Nullable Ponto pontoDoRastro(ServerPlayer p, EstadoJogador e, long seg, int idadeMin, int idadeMax,
			double distMin, double distMax, boolean foraDaTela) {
		List<Rastro.Ponto> bons = new ArrayList<>();
		for (Rastro.Ponto pt : e.rastro.comIdade(seg, idadeMin, idadeMax)) {
			double d = Math.sqrt(distanciaSqr(p, pt.x(), pt.z()));
			if (d < distMin || d > distMax) {
				continue;
			}
			if (foraDaTela && pontoNaFrente(p, new Vec3(pt.x(), pt.y() + 1.5, pt.z()), Percepcao.conePercebeu(p))) {
				continue;
			}
			bons.add(pt);
		}
		if (bons.isEmpty()) {
			return null;
		}
		return bons.get(p.level().getRandom().nextInt(bons.size()));
	}

	/** O Hóspede aparece num lugar por onde você passou: "ele veio atrás de mim". */
	static boolean invocarNoRastro(ServerLevel level, ServerPlayer p, EstadoJogador e, HospedeEntity.Modo modo,
			long seg, int idadeMin, int idadeMax, double distMin, double distMax, int duracao, double distSumir, PedidoManifestacao pedido) {
		for (int tentativa = 0; tentativa < 6; tentativa++) {
			Rastro.Ponto pt = pontoDoRastro(p, e, seg, idadeMin, idadeMax, distMin, distMax, true);
			if (pt == null) {
				return false;
			}
			// Procura o chão a partir da altura dos seus pés naquele momento.
			BlockPos chao = acharChao(level, pt.x(), pt.y() - 5, pt.z());
			if (chao == null || naTela(p, chao) || emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			if (expostoDemais(level, p, chao, temCobertura(level, p, chao))) {
				continue;
			}
			// O Rastro guarda por onde ele andou, inclusive a caverna embaixo dos pés dele. Um ponto assim fica
			// "perto" no mapa, mas a aparição nasce onde não dá para ver (um log real mostrou duas a 30 blocos abaixo).
			if (Math.abs(chao.getY() - p.getY()) > DESNIVEL_MAX_APARICAO) {
				continue;
			}
			pedido = pedido.comNota(String.format(Locale.ROOT, "RASTRO idadeRastro=%ds distRastro=%.0f", seg - pt.seg(),
					Math.sqrt(distanciaSqr(p, pt.x(), pt.z()))));
			criar(level, p, e, chao, modo, duracao, 1.0, distSumir, pedido);
			return true;
		}
		return false;
	}

	/**
	 * De dia, a céu aberto, sem nada na frente e de perto, a aparição parece só um boneco parado
	 * ("looks great behind the tree but too goofy out in the open"). Nesses lugares ele não nasce.
	 */
	static boolean expostoDemais(ServerLevel level, ServerPlayer p, BlockPos chao, boolean cobertura) {
		if (cobertura || ehNoite(level)) {
			return false;
		}
		if (distanciaSqr(p, chao.getX() + 0.5, chao.getZ() + 0.5) >= DIST_MIN_EXPOSTO * DIST_MIN_EXPOSTO) {
			return false;
		}
		return luzEfetiva(level, chao.above()) >= 12 && level.canSeeSky(chao.above());
	}

	/** O vulto distante só faz sentido de dia e com o jogador ao ar livre: à noite uma silhueta tão longe não aparece. */
	private static boolean podeVulto(ServerLevel level, ServerPlayer p) {
		return !ehNoite(level) && level.canSeeSky(p.blockPosition().above());
	}

	/**
	 * Vulto distante (0.8.1): uma silhueta parada a dezenas de blocos, de preferência num ponto alto, recortada
	 * contra o céu. Nasce fora da tela, num lugar com linha livre até o jogador. Longe e curto assim, é negável;
	 * por isso é fraco (intensidade 9) e pode acontecer mais vezes que as outras aparições.
	 */
	private static boolean invocarVulto(ServerLevel level, ServerPlayer p, EstadoJogador e, PedidoManifestacao pedido) {
		RandomSource rnd = level.getRandom();
		BlockPos melhor = null;
		double melhorNota = Double.NEGATIVE_INFINITY;
		double melhorDist = 0;
		for (int i = 0; i < 24; i++) {
			double ang = (rnd.nextBoolean() ? 1 : -1) * (60 + rnd.nextDouble() * 80);
			double dist = VULTO_DIST_MIN + rnd.nextDouble() * (VULTO_DIST_MAX - VULTO_DIST_MIN);
			Vec3 alvo = pontoRelativo(p, ang, dist);
			BlockPos coluna = BlockPos.containing(alvo.x, p.getY(), alvo.z);
			if (!level.isLoaded(coluna)) {
				continue;
			}
			// O topo do terreno (sem contar folhas) em vez do nível do jogador: a essa distância o chão pode estar bem acima ou abaixo.
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, coluna.getX(), coluna.getZ());
			BlockPos chao = new BlockPos(coluna.getX(), y, coluna.getZ());
			BlockPos baixo = chao.below();
			if (!level.getBlockState(chao).isAir() || !level.getBlockState(chao.above()).isAir() || !level.getBlockState(chao.above(2)).isAir()
					|| level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty() || !level.getFluidState(baixo).isEmpty()) {
				continue;
			}
			if (naTela(p, chao) || emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			// Tem de dar para vê-lo de onde o jogador está agora.
			if (!Aparicao.linhaLivre(level, p.getEyePosition(), new Vec3(chao.getX() + 0.5, chao.getY() + 2.6, chao.getZ() + 0.5))) {
				continue;
			}
			double nota = rnd.nextDouble() + Math.max(0.0, Math.min(2.0, (chao.getY() - p.getY()) / 6.0));
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
				melhorDist = dist;
			}
		}
		if (melhor == null) {
			return false;
		}
		pedido = pedido.comNota(String.format(Locale.ROOT, "VULTO dist=%.0f altura=%+d", melhorDist, melhor.getY() - p.getBlockY()));
		criar(level, p, e, melhor, HospedeEntity.Modo.VULTO, 20 * (30 + rnd.nextInt(16)), 1.0, pedido);
		return true;
	}

	/** Começo da espreita: num ponto do seu rastro ou logo fora da tela, longe o bastante para poder se aproximar. */
	private static boolean invocarEspreita(ServerLevel level, ServerPlayer p, EstadoJogador e, int ousadia, long seg, PedidoManifestacao pedido) {
		RandomSource rnd = level.getRandom();
		int duracao = 20 * 120;
		boolean ok = rnd.nextFloat() < 0.5F
				&& invocarNoRastro(level, p, e, HospedeEntity.Modo.ESPREITAR, seg, 20, 240, 22, 38, duracao, 8.0, pedido);
		if (!ok) {
			ok = invocar(level, p, e, HospedeEntity.Modo.ESPREITAR, 55, 85, 24, 36, duracao, 1.0, true, 8.0, true, pedido);
		}
		HospedeEntity h = e.criatura;
		if (ok && h != null) {
			h.definirMaxReposicoes(2 + rnd.nextInt(2));
			anunciar(level, p, h);
		}
		return ok;
	}

	/**
	 * Chamado pela criatura (modo ESPREITAR) depois de um tempo sorteado sem você olhar para ela.
	 * Ela pode chegar mais perto (de preferência atrás de alguma cobertura ou num ponto do seu rastro),
	 * trocar de lado, ficar onde está ou sumir. Sem padrão fixo. Devolve false se ela deve sumir.
	 */
	public static boolean reposicionarEspreita(ServerPlayer p, HospedeEntity h) {
		ServerLevel level = p.level();
		EstadoJogador e = estado(p);
		RandomSource rnd = level.getRandom();
		long seg = level.getGameTime() / 20;
		int n = h.getReposicoes() + 1;
		double distAtual = Math.sqrt(h.distanceToSqr(p));
		String rotulo = String.format(Locale.ROOT, "ESPREITA id=%s step=%d/%d ", h.getIdManifestacao(), n, h.getMaxReposicoes());

		if (h.getReposicoes() >= h.getMaxReposicoes()) {
			h.definirMotivoFim("PERDEU_CONTATO");
			Depuracao.log(p, seg, String.format(Locale.ROOT, "ESPREITA id=%s step=fim/%d acao=PERDEU_CONTATO dist=%.1f",
					h.getIdManifestacao(), h.getMaxReposicoes(), distAtual));
			return false;
		}
		double r = rnd.nextDouble();
		if (n > 1 && r < 0.15) {
			h.definirMotivoFim("SUMIR");
			Depuracao.log(p, seg, String.format(Locale.ROOT, "%sacao=SUMIR dist=%.1f", rotulo, distAtual));
			return false;
		}
		if (r < 0.30) {
			h.contarReposicao();
			Depuracao.log(p, seg, String.format(Locale.ROOT, "%sacao=FICAR dist=%.1f motivo=SORTEIO", rotulo, distAtual));
			return true;
		}
		boolean trocarLado = r < 0.50;
		double angAtual = anguloRelativo(p, h.getX(), h.getZ());

		double origX = h.getX();
		double origY = h.getY();
		double origZ = h.getZ();
		BlockPos melhor = null;
		double melhorNota = -1;
		boolean melhorCobertura = false;
		boolean melhorRastro = false;
		boolean melhorVisivel = false; // telemetria
		long melhorIdadeRastro = -1;   // telemetria
		int candidatosValidos = 0;     // telemetria

		for (int i = 0; i < 16; i++) {
			double x;
			double yBase;
			double z;
			boolean doRastro = false;
			long idadeRastro = -1;
			if (!trocarLado && i < 6) {
				Rastro.Ponto pt = pontoDoRastro(p, e, seg, 5, 300, Math.max(10, distAtual - 12), distAtual - 3, true);
				if (pt == null || Math.abs(pt.y() - p.getY()) > DESNIVEL_MAX_APARICAO) {
					continue;
				}
				x = pt.x();
				yBase = pt.y() - 5;
				z = pt.z();
				doRastro = true;
				idadeRastro = seg - pt.seg();
			} else {
				double dNovo = trocarLado
						? distAtual * (0.8 + rnd.nextDouble() * 0.2)
						: Math.max(10, distAtual - 4 - rnd.nextDouble() * 5);
				double ang = (trocarLado ? -angAtual : angAtual) + (rnd.nextDouble() - 0.5) * 40;
				Vec3 alvo = pontoRelativo(p, ang, dNovo);
				x = alvo.x;
				yBase = p.getY();
				z = alvo.z;
			}
			BlockPos chao = acharChao(level, x, yBase, z);
			if (chao == null || naTela(p, chao) || emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			if (distanciaSqr(p, chao.getX() + 0.5, chao.getZ() + 0.5) < 9 * 9) {
				continue;
			}
			boolean cobertura = temCobertura(level, p, chao);
			// Se você virar, dá para ver pelo menos a cabeça dele dali? (testa e volta para o lugar)
			h.setPos(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
			boolean visivel = p.hasLineOfSight(h);
			h.setPos(origX, origY, origZ);
			double nota = rnd.nextDouble() + (cobertura ? 2.0 : 0) + (visivel ? 1.5 : 0) + (doRastro ? 1.0 : 0);
			candidatosValidos++;
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
				melhorCobertura = cobertura;
				melhorRastro = doRastro;
				melhorVisivel = visivel;
				melhorIdadeRastro = idadeRastro;
			}
		}

		h.contarReposicao();
		String pretendida = trocarLado ? "TROCAR_LADO" : "APROXIMAR";
		if (melhor == null) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "%sacao=FICAR dist=%.1f motivo=SEM_LUGAR pretendia=%s",
					rotulo, distAtual, pretendida));
			return true;
		}
		h.setPos(melhor.getX() + 0.5, melhor.getY(), melhor.getZ() + 0.5);
		h.olharPara(p);
		double novaDist = Math.sqrt(h.distanceToSqr(p));
		if (rnd.nextFloat() < 0.3F) {
			// Às vezes o deslocamento faz barulho. Baixinho, vindo exatamente de onde ele foi parar.
			float vol = volumePara(p, h.getX(), h.getY(), h.getZ(), 0.5F);
			if (rnd.nextBoolean()) {
				BlockState piso = level.getBlockState(melhor.below());
				level.playSound(null, h.getX(), h.getY(), h.getZ(), piso.getSoundType().getStepSound(), SoundSource.HOSTILE, vol, 0.7F);
			} else {
				ModSons.tocar(level, h.getX(), h.getY() + 1.0, h.getZ(),
						rnd.nextBoolean() ? ModSons.Som.PANO : ModSons.Som.ARRASTO, vol, 0.9F);
			}
		}
		String motivoPos = melhorRastro && melhorCobertura ? "RASTRO+COBERTURA"
				: melhorRastro ? "RASTRO" : melhorCobertura ? "COBERTURA" : "NORMAL";
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"%sacao=%s dist=%.1f->%.1f motivo=%s cabecaVisivel=%s%s candidatos=%d pos=%s",
				rotulo, pretendida, distAtual, novaDist, motivoPos, melhorVisivel ? "sim" : "nao",
				melhorIdadeRastro >= 0 ? " idadeRastro=" + melhorIdadeRastro + "s" : "", candidatosValidos,
				pos(h.getX(), h.getY(), h.getZ())));
		return true;
	}

	/** Ângulo (graus) de um ponto em relação a para onde o jogador olha. Mesma convenção de pontoRelativo. */
	static double anguloRelativo(ServerPlayer p, double x, double z) {
		Vec3 view = p.getViewVector(1.0F);
		double tx = x - p.getX();
		double tz = z - p.getZ();
		double dot = view.x * tx + view.z * tz;
		double cross = view.x * tz - view.z * tx;
		return Math.toDegrees(Math.atan2(cross, dot));
	}

	/** Há algo sólido logo à frente dele, entre ele e você (tronco, parede, pilar)? */
	static boolean temCobertura(ServerLevel level, ServerPlayer p, BlockPos chao) {
		double dx = p.getX() - (chao.getX() + 0.5);
		double dz = p.getZ() - (chao.getZ() + 0.5);
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < 1.0E-4) {
			return false;
		}
		dx /= d;
		dz /= d;
		for (int passo = 1; passo <= 2; passo++) {
			BlockPos frente = BlockPos.containing(chao.getX() + 0.5 + dx * passo, chao.getY() + 1, chao.getZ() + 0.5 + dz * passo);
			if (!level.getBlockState(frente).getCollisionShape(level, frente).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** O ponto (na altura do corpo dele) está dentro da sua tela agora? */
	static boolean naTela(ServerPlayer p, BlockPos chao) {
		// Mais largo que PERCEBEU: evita materialização na borda do FOV real em 16:9/FOV 70.
		return pontoNaFrente(p, new Vec3(chao.getX() + 0.5, chao.getY() + 1.5, chao.getZ() + 0.5), Percepcao.coneSeguro(p));
	}

	/** Luz que realmente ilumina um lugar (à noite o céu conta bem menos). */
	private static int luzEfetiva(ServerLevel level, BlockPos pos) {
		int luzBloco = level.getBrightness(LightLayer.BLOCK, pos);
		int luzCeu = level.getBrightness(LightLayer.SKY, pos);
		return Math.max(luzBloco, ehNoite(level) ? luzCeu - 11 : luzCeu);
	}

	/** Daria para enxergá-lo ali? Perto sempre; longe, só com alguma luz. */
	private static boolean pontoVisivel(ServerLevel level, ServerPlayer p, BlockPos chao) {
		return distanciaSqr(p, chao.getX() + 0.5, chao.getZ() + 0.5) < 20 * 20 || luzEfetiva(level, chao.above()) >= 4;
	}

	/**
	 * Volume acima de 1 só aumenta o alcance do som (16 blocos x volume). Garante que ele chegue até você.
	 * Usa a distância de verdade, com a altura. Numa caverna a fonte pode estar "perto" no mapa e 20 blocos
	 * abaixo; com a distância só horizontal o som saía sem alcance para chegar (num log real, um eco a 36 blocos
	 * saiu com alcance de 32).
	 */
	static float volumePara(ServerPlayer p, double x, double y, double z, float base) {
		double dx = p.getX() - x;
		double dy = p.getY() - y;
		double dz = p.getZ() - z;
		double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
		return (float) Math.max(base, (d + 4) / 16.0);
	}

	/**
	 * Anúncio de uma aparição, sem virar regra (v0.4.2a): 55% dois passos secos vindos dele,
	 * 30% nada (você o encontra sem aviso), 15% um único ruído discreto do lugar.
	 */
	private static void anunciar(ServerLevel level, ServerPlayer p, @Nullable HospedeEntity h) {
		if (h == null) {
			return;
		}
		RandomSource rnd = level.getRandom();
		long seg = level.getGameTime() / 20;
		float r = rnd.nextFloat();
		float volume = volumePara(p, h.getX(), h.getY(), h.getZ(), 1.0F);
		if (r < 0.30F) {
			Depuracao.log(p, seg, "anúncio: nenhum");
			return;
		}
		if (r < 0.45F) {
			ModSons.Som som = rnd.nextBoolean() ? ModSons.Som.PANO : (rnd.nextBoolean() ? ModSons.Som.ESTALO : ModSons.Som.ARRASTO);
			agendar(level, 8 + rnd.nextInt(20), () -> {
				if (!h.isRemoved()) {
					ModSons.tocar(level, h.getX(), h.getY() + 1.0, h.getZ(), som, volume, 0.85F + rnd.nextFloat() * 0.3F);
				}
			});
			Depuracao.log(p, seg, "anúncio: discreto (" + som + ")");
			return;
		}
		BlockState piso = level.getBlockState(h.blockPosition().below());
		SoundEvent passo = piso.getSoundType().getStepSound();
		for (int i = 0; i < 2; i++) {
			agendar(level, 10 + i * 9, () -> {
				if (!h.isRemoved()) {
					level.playSound(null, h.getX(), h.getY(), h.getZ(), passo, SoundSource.HOSTILE, volume, 0.6F);
				}
			});
		}
		Depuracao.log(p, seg, "anúncio: dois passos");
	}

	private static void somarObsessao(EstadoJogador e, double delta) {
		e.obsessao = limitar(e.obsessao + delta, 0, 100);
	}

	// =====================================================================
	// Sequência de ameaça (estado AMEACANDO) (v0.4.2)
	// =====================================================================

	/**
	 * A caça não é sorteada: é montada. Presença que espreita -> perda de contato -> silêncio curto ->
	 * golpe (caça na fase 4, aparição logo atrás de você na fase 3) -> silêncio longo (RECUANDO).
	 */
	private static void conduzirAmeaca(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean escuro, int v, long seg, long tick, RandomSource rnd) {
		HospedeEntity atual = e.criatura;
		boolean criaturaPresente = atual != null && !atual.isRemoved();
		switch (e.cena) {
			case NENHUMA -> {
				e.cena = EstadoJogador.Cena.PRESENCA;
				e.cenaDesde = seg;
				e.ameacaId = novoIdCena();
				Depuracao.log(p, seg, String.format(Locale.ROOT, "CENA id=%s tipo=AMEACA INICIO obsessao=%.0f V=%d fase=%d",
						e.ameacaId, e.obsessao, v, fase));
			}
			case PRESENCA -> {
				if (criaturaPresente || bloqueado(p, e, tick)) {
					return;
				}
				if (executar(level, p, m, e, Evento.ESPREITA, seg, tick)) {
					e.cena = EstadoJogador.Cena.ESPREITANDO;
					e.cenaDesde = seg;
					Depuracao.log(p, seg, "CENA id=" + e.ameacaId + " etapa=ESPREITA manifestacao=" + manifestacaoAtiva(e));
				} else if (seg - e.cenaDesde > 60) {
					fimAmeaca(p, e, seg, rnd, "SEM_LUGAR", 0.8);
				}
			}
			case ESPREITANDO -> {
				if (!criaturaPresente) {
					e.cena = EstadoJogador.Cena.PAUSA;
					e.cenaAte = seg + 15 + rnd.nextInt(21);
					Depuracao.log(p, seg, "CENA id=" + e.ameacaId + " etapa=PAUSA (perdeu contato) duracao=" + (e.cenaAte - seg) + "s");
				} else if (seg - e.cenaDesde > 150) {
					atual.sumir(level, false, "LIMITE_DA_CENA");
				}
			}
			case PAUSA -> {
				if (seg >= e.cenaAte) {
					e.cena = EstadoJogador.Cena.GOLPE;
					e.cenaDesde = seg;
					Depuracao.log(p, seg, "CENA id=" + e.ameacaId + " etapa=GOLPE aguardando escuro (até 120s)");
				}
			}
			case GOLPE -> {
				if (criaturaPresente) {
					return; // a caça (ou a aparição) está acontecendo
				}
				if (e.golpeDado) {
					fimAmeaca(p, e, seg, rnd, "GOLPE_CONCLUIDO", 0.4);
					return;
				}
				if (seg - e.cenaDesde > 120) {
					fimAmeaca(p, e, seg, rnd, "GOLPE_SEM_MOMENTO", 0.7);
					return;
				}
				if (!escuro || bloqueado(p, e, tick)) {
					return; // espera o escuro
				}
				if (fase >= 4 && !podeComecarCacada(level, p, m, e)) {
					return; // caçada não começa na base nem colado num amigo: espera ele sair
				}
				Evento golpe = fase >= 4 ? Evento.CACA : Evento.ATRAS;
				if (executar(level, p, m, e, golpe, seg, tick)) {
					e.golpeDado = true;
					Depuracao.log(p, seg, "CENA id=" + e.ameacaId + " etapa=GOLPE evento=" + golpe + " manifestacao=" + manifestacaoAtiva(e));
				}
			}
		}
	}

	private static void fimAmeaca(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, String motivo, double fatorObsessao) {
		double antes = e.obsessao;
		e.obsessao *= fatorObsessao;
		e.cena = EstadoJogador.Cena.NENHUMA;
		e.golpeDado = false;
		e.ameacaLiberadaEm = Math.max(e.ameacaLiberadaEm, seg + 600);
		mudarEstado(p, e, EstadoDiretor.RECUANDO, seg, rnd);
		e.duracaoEstado = 180 + rnd.nextInt(121); // silêncio de verdade depois do pico
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CENA id=%s FIM motivo=%s obsessao=%.0f->%.0f",
				e.ameacaId, motivo, antes, e.obsessao));
		marcarSilencioDoRecuo(p, e, seg, "POS_AMEACA", e.ameacaId);
	}

	// =====================================================================
	// Âncora: a porta habitual (v0.4.2)
	// =====================================================================

	/** Conta quantas vezes você usa cada porta. A mais usada vira "a porta de sempre". */
	private static void registrarPorta(ServerPlayer p, ServerLevel level, BlockPos clicada) {
		BlockPos pos = level.getBlockState(clicada.below()).getBlock() instanceof DoorBlock ? clicada.below() : clicada;
		EstadoJogador e = estado(p);
		Memoria m = Memoria.de(p);
		String chave = pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
		int usos = e.usosPortas.merge(chave, 1, Integer::sum);
		registrarAcao(e, EstadoJogador.TipoAcao.PORTA, SoundEvents.WOODEN_DOOR_OPEN, pos, level.getGameTime() / 20);
		boolean temHabitual = m.get(Memoria.TEM_PORTA) == 1;
		String chaveHabitual = m.get(Memoria.PORTA_X) + ":" + m.get(Memoria.PORTA_Y) + ":" + m.get(Memoria.PORTA_Z);
		if (temHabitual && chave.equals(chaveHabitual)) {
			m.set(Memoria.PORTA_USOS, Math.max(m.get(Memoria.PORTA_USOS), usos));
		} else if (usos >= 3 && (!temHabitual || usos > m.get(Memoria.PORTA_USOS)
				|| (usos >= 5 && !e.usosPortas.containsKey(chaveHabitual)))) {
			m.set(Memoria.TEM_PORTA, 1);
			m.set(Memoria.PORTA_X, pos.getX());
			m.set(Memoria.PORTA_Y, pos.getY());
			m.set(Memoria.PORTA_Z, pos.getZ());
			m.set(Memoria.PORTA_USOS, usos);
			Depuracao.log(p, level.getGameTime() / 20, "ANCORA: porta habitual em " + chave + " (" + usos + " usos)");
		}
		m.salvar();
	}

	/** A porta de sempre, se ainda existe e está a até 24 blocos, sem considerar para onde o jogador olha. */
	@Nullable
	static BlockPos portaHabitualExistente(ServerLevel level, ServerPlayer p) {
		Memoria m = Memoria.de(p);
		if (m.get(Memoria.TEM_PORTA) != 1) {
			return null;
		}
		BlockPos pos = BlockPos.containing(m.get(Memoria.PORTA_X) + 0.5, m.get(Memoria.PORTA_Y) + 0.5, m.get(Memoria.PORTA_Z) + 0.5);
		BlockState s = level.getBlockState(pos);
		if (!(s.getBlock() instanceof DoorBlock) || s.is(Blocks.IRON_DOOR) || !s.hasProperty(BlockStateProperties.OPEN)) {
			return null;
		}
		return distancia(p, Vec3.atCenterOf(pos)) <= 24 ? pos : null;
	}

	/** A porta de sempre, se ainda existe, está a até 24 blocos e fora da sua visão. */
	@Nullable
	private static BlockPos acharPortaHabitual(ServerLevel level, ServerPlayer p) {
		BlockPos pos = portaHabitualExistente(level, p);
		if (pos == null || pontoNaFrente(p, Vec3.atCenterOf(pos), 0.5)) {
			return null;
		}
		return pos;
	}

	private static boolean temPorta(ServerLevel level, ServerPlayer p) {
		return acharPortaHabitual(level, p) != null || acharPorta(level, p) != null;
	}

	/** Prefere a porta de sempre (80%). A mesma porta voltando à história assusta mais que qualquer porta. */
	@Nullable
	private static BlockPos escolherPorta(ServerLevel level, ServerPlayer p) {
		BlockPos habitual = acharPortaHabitual(level, p);
		if (habitual != null && level.getRandom().nextFloat() < 0.8F) {
			Depuracao.log(p, level.getGameTime() / 20, "ANCORA: a porta de sempre");
			return habitual;
		}
		BlockPos perto = acharPorta(level, p);
		return perto != null ? perto : habitual;
	}

	// =====================================================================
	// Cama
	// =====================================================================

	private static boolean mesmaCama(Memoria m, BlockPos pos) {
		return m.get(Memoria.TEM_CAMA) == 1
				&& Math.abs(m.get(Memoria.CAMA_X) - pos.getX()) <= 4
				&& Math.abs(m.get(Memoria.CAMA_Y) - pos.getY()) <= 4
				&& Math.abs(m.get(Memoria.CAMA_Z) - pos.getZ()) <= 4;
	}

	/** Cama clicada: vira a "casa". Só dormir conta para o "você sempre dorme aqui". */
	private static void registrarCasa(ServerPlayer p, BlockPos pos) {
		Memoria m = Memoria.de(p);
		if (mesmaCama(m, pos)) {
			return;
		}
		m.set(Memoria.CAMA_VEZES, 0);
		m.set(Memoria.TEM_CAMA, 1);
		m.set(Memoria.CAMA_X, pos.getX());
		m.set(Memoria.CAMA_Y, pos.getY());
		m.set(Memoria.CAMA_Z, pos.getZ());
		m.salvar();
		Depuracao.log(p, p.level().getGameTime() / 20, "casa: cama em " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
	}

	private static void aoDormir(ServerPlayer p, BlockPos pos) {
		Memoria m = Memoria.de(p);
		if (mesmaCama(m, pos)) {
			m.add(Memoria.CAMA_VEZES, 1);
		} else {
			m.set(Memoria.CAMA_VEZES, 1);
		}
		m.set(Memoria.TEM_CAMA, 1);
		m.set(Memoria.CAMA_X, pos.getX());
		m.set(Memoria.CAMA_Y, pos.getY());
		m.set(Memoria.CAMA_Z, pos.getZ());
		m.salvar();
	}

	private static void aoAcordar(ServerPlayer p, BlockPos pos) {
		ServerLevel level = p.level();
		if (level != level.getServer().getLevel(Level.OVERWORLD)) {
			return;
		}
		Memoria m = Memoria.de(p);
		int fase = m.get(Memoria.FASE);
		RandomSource rnd = level.getRandom();

		// Dormir de verdade com uma vela acesa tira a marca da captura. Sair da cama no meio não conta.
		Captura.tentarCurar(p, m, p.isSleepingLongEnough());

		// Ele deixou algo ao lado da cama enquanto você dormia.
		if (fase >= 2 && m.get(Memoria.PAGINAS_ENTREGUES) < Diario.TOTAL_PAGINAS && rnd.nextFloat() < 0.45F) {
			m.add(Memoria.PAGINAS_ENTREGUES, 1);
			BlockPos chao = acharChao(level, pos.getX() + 0.5 + rnd.nextInt(3) - 1, pos.getY(), pos.getZ() + 0.5 + rnd.nextInt(3) - 1);
			if (chao != null) {
				ItemEntity item = new ItemEntity(level, chao.getX() + 0.5, chao.getY() + 0.1, chao.getZ() + 0.5, new ItemStack(ModItems.PAGINA_RASGADA));
				item.setDeltaMovement(0, 0, 0);
				level.addFreshEntity(item);
			}
		}

		if (fase >= 3) {
			// 0.9: as luzes perto da cama aparecem apagadas ao acordar, só para ele, por um ou dois minutos
			// (antes eram tiradas do mundo de verdade, sem devolver nada: item 3.10 da análise).
			int apagadas = rnd.nextBoolean()
					? ApoioCaca.apagarLuzPerto(level, p, pos, 6, 20 * (60 + rnd.nextInt(61)), 1 + rnd.nextInt(3))
					: 0;
			if (m.get(Memoria.CAMA_VEZES) >= 4) {
				p.sendOverlayMessage(Component.translatable("message.sussurros.acordar.mesma_cama", p.getName())
						.withStyle(s -> s.withColor(0x7A1010).withItalic(true)));
			} else if (apagadas > 0 || rnd.nextBoolean()) {
				p.sendOverlayMessage(Component.translatable("message.sussurros.acordar.observado")
						.withStyle(s -> s.withColor(0x5A5A5A).withItalic(true)));
			}
		}
		m.salvar();
	}

	// =====================================================================
	// Chamadas vindas da criatura e dos itens
	// =====================================================================

	/** Na caça, quem mexe no mundo se entrega: o som de uma ação a até 16 blocos dá a posição a ele. */
	private static void avisarCacador(EstadoJogador e, Vec3 onde, String oQue) {
		HospedeEntity h = e.criatura;
		if (h != null && !h.isRemoved() && h.getModo() == HospedeEntity.Modo.CACAR) {
			h.ouvirAcao(onde, oQue);
		}
	}

	/**
	 * A caçada nunca começa na base, nem com o jogador montado ou planando, nem colado num amigo.
	 * Base invadida é a reclamação que mais mata o medo; e ficar junto dos amigos tem de ser um alívio.
	 */
	private static boolean podeComecarCacada(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e) {
		if (e.contexto == ContextoMundo.Tipo.CASA || ContextoMundo.pertoDaCasa(p, m, 24)) {
			return false;
		}
		if (p.isPassenger() || p.isFallFlying()) {
			return false;
		}
		return level.getPlayers(o -> o != p && !o.isSpectator() && o.distanceToSqr(p) < 7 * 7).isEmpty();
	}

	/**
	 * O aviso da caçada: o mundo emudece e uma luz perto do jogador falha. Dura o tempo em que ele, se existir,
	 * fica parado (8 a 10 s). É o que dá ao jogador a chance de correr para a vela ou sair de um beco.
	 * O mesmo aviso acontece sem caçada (evento PRENUNCIO), para nunca virar certeza.
	 */
	private static void prenunciar(ServerLevel level, ServerPlayer p, EstadoJogador e, long tick, RandomSource rnd, boolean deVerdade) {
		emudecer(level, p, deVerdade ? 12 : 10 + rnd.nextInt(8), deVerdade ? "CACA" : "PRENUNCIO");
		e.semMusicaAte = tick + 20L * (deVerdade ? 12 : 10 + rnd.nextInt(8));
		e.cacaAvisoAte = tick + 20L * 10;
		int apagadas = ApoioCaca.apagarLuzPerto(level, p, p.blockPosition(), 9, 60 + rnd.nextInt(60), 1);
		ModSons.tocarPara(p, p.getX(), p.getY() + 1.0, p.getZ(), ModSons.Som.GRAVE, 0.55F, 0.8F);
		boolean assobio = Memoria.de(p).get(Memoria.CAIXA_USOS) >= CANTIGA_APRENDIDA && rnd.nextFloat() < 0.6F;
		if (assobio) {
			agendar(level, 30 + rnd.nextInt(40), () -> {
				if (!p.isRemoved()) {
					assobiar(level, p, p.getRandom());
				}
			});
		}
		Depuracao.log(p, tick / 20, "PRENUNCIO real=" + (deVerdade ? "sim" : "nao") + " luz=" + apagadas
				+ " assobio=" + (assobio ? "sim" : "nao"));
	}

	/**
	 * A caçada acabou: ele sumiu, por qualquer motivo. O som do mundo volta alguns segundos depois (é o sinal
	 * honesto de fim), e a próxima caçada fica longe.
	 */
	public static void cacadaTerminou(ServerPlayer p, HospedeEntity h, String motivo) {
		EstadoJogador e = estado(p);
		ServerLevel level = p.level();
		long tick = level.getGameTime();
		RandomSource sorte = p.getRandom();
		e.semMusicaAte = tick + 70 + sorte.nextInt(31);
		e.cacaAvisoAte = -1;
		if (h.ehTeste()) {
			return;
		}
		Memoria m = Memoria.de(p);
		m.add(Memoria.CACADAS, 1);
		m.salvar();
		// Caçada é rara: a próxima sequência de ameaça só daqui a 25-40 minutos.
		e.ameacaLiberadaEm = Math.max(e.ameacaLiberadaEm, tick / 20 + 1500 + sorte.nextInt(901));
		Depuracao.log(p, tick / 20, "CACA terminou motivo=" + motivo + " cacadas=" + m.get(Memoria.CACADAS)
				+ " proximaAmeacaEm=" + (e.ameacaLiberadaEm - tick / 20) + "s");
	}

	/** Depois de pegar o jogador, ele some por um bom tempo. O Diretor não pode emendar outra coisa. */
	static void depoisDaCaptura(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		RandomSource sorte = p.getRandom();
		e.pressao = 0;
		e.obsessao *= 0.4;
		e.cena = EstadoJogador.Cena.NENHUMA;
		e.golpeDado = false;
		e.sequencia = null;
		e.elosCadeia = 0;
		Depuracao.log(p, seg, "ESTADO " + e.estado + " -> RECUANDO (captura)");
		e.estado = EstadoDiretor.RECUANDO;
		e.estadoDesde = seg;
		e.duracaoEstado = 360 + sorte.nextInt(241);
		e.ameacaLiberadaEm = Math.max(e.ameacaLiberadaEm, seg + 1800 + sorte.nextInt(901));
		marcarSilencioDoRecuo(p, e, seg, "POS_CAPTURA", e.ameacaId);
	}

	/**
	 * Primeira vez que a criatura aparece na tela do jogador. A reação só é medida para aparições
	 * do Diretor (não do Olho nem de comando), e só para as de categoria VISÃO.
	 * v0.4.2: a origem vem da própria criatura (0.4.1 usava um campo do jogador que ficava velho).
	 */
	public static void criaturaAvistada(ServerPlayer p, HospedeEntity criatura) {
		EstadoJogador e = estado(p);
		long tick = p.level().getGameTime();
		Evento ev = criatura.getEventoOrigem();
		boolean aprende = criatura.getOrigem() == HospedeEntity.Origem.DIRETOR && ev != null && ev.categoria == Evento.Categoria.VISAO;
		Depuracao.log(p, tick / 20, "HOSPEDE id=" + criatura.getIdManifestacao() + " primeira vista: origem="
				+ criatura.getOrigem() + " evento=" + ev + " aprende=" + (aprende ? "sim" : "nao"));
		if (!aprende) {
			return;
		}
		somarObsessao(e, 3);
		if (!e.leitura.pendente()) {
			e.leitura.iniciar(ev, criatura.getEyePosition(), 1.0, tick, "avistada");
		}
	}

	/**
	 * O jogador olhou direto para a criatura pela primeira vez. Guarda o momento para a leitura do
	 * avistamento (ver concluirLeitura): encarar já é uma reação.
	 */
	public static void criaturaEncarada(ServerPlayer p, HospedeEntity criatura) {
		if (!criatura.ehTeste()) {
			estado(p).encarouSeg = p.level().getGameTime() / 20;
		}
	}

	public static void criaturaFoiVista(ServerPlayer p, HospedeEntity criatura) {
		if (criatura.ehTeste()) {
			return; // criatura de comando: não deixa ele mais ousado nem soma pressão
		}
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		e.pressao += 12;
		if (criatura.getOrigem() == HospedeEntity.Origem.DIRETOR) {
			somarObsessao(e, 5);
		}
		if (e.estado.permiteCadeias() && seg >= e.carenciaAte && e.sequencia == null
				&& p.level().getRandom().nextFloat() < 0.5F) {
			e.sequencia = Evento.VISTO;
			e.proximoEvento = seg + 20 + p.level().getRandom().nextInt(26);
		}
		Memoria m = Memoria.de(p);
		// Só as aparições que o Diretor montou deixam ele mais ousado. As chamadas pelo Olho não contam:
		// senão usar o Olho em sequência virava um jeito de aumentar a ousadia.
		if (criatura.getOrigem() == HospedeEntity.Origem.DIRETOR) {
			m.add(Memoria.VEZES_VISTO, 1);
		}
		m.add(Memoria.INQUIETACAO, 15);
		m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
		m.salvar();
	}

	public static void criaturaSumiu(ServerPlayer p, HospedeEntity criatura, String motivo) {
		if (Depuracao.ativo) {
			Depuracao.log(p, p.level().getGameTime() / 20, String.format(Locale.ROOT,
					"HOSPEDE id=%s sumiu motivo=%s viveu=%ds dist=%.1f vezesNaTela=%d reposicoes=%d",
					criatura.getIdManifestacao(), motivo, criatura.ticksVivo() / 20, Math.sqrt(criatura.distanceToSqr(p)),
					criatura.getVezesPercebida(), criatura.getReposicoes()));
		}
		tentarDeixarCinza(p, criatura, motivo);
		EstadoJogador e = ESTADOS.get(p.getUUID());
		if (e != null && e.criatura == criatura) {
			e.criatura = null;
		}
	}

	public static void criaturaFerida(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		m.add(Memoria.VEZES_FERIDO, 1);
		Perfil.puxar(m, Perfil.Traco.CONFRONTO, 100, 0.1);
		m.salvar();
		if (m.get(Memoria.VEZES_FERIDO) % 2 == 0) {
			p.sendOverlayMessage(Component.translatable("message.sussurros.ferido")
					.withStyle(s -> s.withColor(0x7A1010).withItalic(true)));
		}
	}

	public static void criaturaTocou(ServerPlayer p, HospedeEntity criatura) {
		if (!criatura.ehTeste()) {
			estado(p).contato = true;
			Memoria m = Memoria.de(p);
			m.add(Memoria.INQUIETACAO, 40);
			m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
			m.salvar();
		}
		p.sendOverlayMessage(Component.translatable("message.sussurros.toque", p.getName())
				.withStyle(s -> s.withColor(0x7A1010).withItalic(true)));
	}

	/**
	 * Algumas manifestações realmente deixam matéria para trás. O vestígio não cai de criaturas de teste
	 * e exige que a aparição tenha sido percebida (ou ferida), para não virar loot automático invisível.
	 */
	private static void tentarDeixarCinza(ServerPlayer p, HospedeEntity criatura, String motivo) {
		if (criatura.ehTeste() || criatura.getOrigem() != HospedeEntity.Origem.DIRETOR) {
			return;
		}
		boolean confronto = "FERIDO".equals(motivo) || "ENCARADO_DEMAIS".equals(motivo);
		boolean percebida = criatura.foiAvistadoVisual();
		if (motivo.startsWith("VULTO_")) {
			// O vulto visto some sem deixar cinza. Às vezes fica só uma marca que o Olho e o Sino acham depois:
			// "eu vi alguma coisa" ganha uma prova tardia.
			if (percebida && p.level().getRandom().nextFloat() < 0.25F) {
				Vestigios.de(p).registrar(criatura.blockPosition(), Vestigios.Tipo.DESAPARECIMENTO, p.level().getGameTime() / 20);
			}
			return;
		}
		boolean qualificou = confronto || (percebida && ("VISTO_DEMAIS".equals(motivo) || "SUMIU_NO_DESVIO".equals(motivo)
				|| "CHEGOU_PERTO".equals(motivo) || "TEMPO_ESGOTADO".equals(motivo)));
		if (!qualificou) {
			return;
		}
		Vestigios.de(p).registrar(criatura.blockPosition(), Vestigios.Tipo.DESAPARECIMENTO, p.level().getGameTime() / 20);
		Memoria m = Memoria.de(p);
		int geradas = m.get(Memoria.CINZAS_GERADAS);
		float chance = geradas < 2 ? 0.68F : 0.24F;
		if (confronto) {
			chance += 0.16F;
		}
		if (p.level().getRandom().nextFloat() >= Math.min(0.9F, chance)) {
			return;
		}
		ServerLevel level = p.level();
		ItemEntity item = new ItemEntity(level, criatura.getX(), criatura.getY() + 0.15, criatura.getZ(),
				new ItemStack(ModItems.CINZA_PALIDA));
		item.setDeltaMovement(0, 0, 0);
		level.addFreshEntity(item);
		m.add(Memoria.CINZAS_GERADAS, 1);
		m.salvar();
		Depuracao.log(p, level.getGameTime() / 20, "VESTIGIO CINZA motivo=" + motivo + " manifestacao="
				+ criatura.getIdManifestacao() + " pos=" + pos(criatura.getX(), criatura.getY(), criatura.getZ()));
	}

	/**
	 * Isca Pálida: o jogador escolhe um ponto e tenta convencer a próxima presença a usar aquele lugar.
	 * Não é uma armadilha nem uma garantia; usos repetidos ensinam o Hóspede a ignorar o truque.
	 */
	public static boolean armarIscaPalida(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long tick = p.level().getGameTime();
		if (e.iscaAtiva && e.iscaAteTick > tick) {
			return false;
		}
		e.iscaX = p.getX();
		e.iscaY = p.getY();
		e.iscaZ = p.getZ();
		e.iscaAteTick = tick + 20L * 120L;
		e.iscaAtiva = true;
		Memoria m = Memoria.de(p);
		m.add(Memoria.ISCAS_ARMADAS, 1);
		m.salvar();
		ModSons.tocar(p.level(), e.iscaX, e.iscaY + 0.2, e.iscaZ, ModSons.Som.PANO, 0.28F, 0.66F);
		p.level().sendParticles(ParticleTypes.ASH, e.iscaX, e.iscaY + 0.08, e.iscaZ, 10, 0.35, 0.03, 0.35, 0.002);
		p.sendOverlayMessage(Component.translatable("message.sussurros.isca.armada")
				.withStyle(s -> s.withColor(0xC9C4B8).withItalic(true)));
		Depuracao.log(p, tick / 20, "ISCA armada pos=" + pos(e.iscaX, e.iscaY, e.iscaZ) + " duracao=120s");
		return true;
	}

	/**
	 * Fio de Vigília: não protege. Ele só transforma uma pequena área num detector físico temporário.
	 * Se o Hóspede realmente entrar nela, o fio se rompe; se nada cruzar, nada acontece.
	 */
	public static boolean armarFioVigilia(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long tick = p.level().getGameTime();
		if (e.vigiaAtiva && e.vigiaAteTick > tick) {
			return false;
		}
		e.vigiaX = p.getX();
		e.vigiaY = p.getY();
		e.vigiaZ = p.getZ();
		e.vigiaRaio = 6;
		e.vigiaAteTick = tick + 20L * 180L;
		e.vigiaAtiva = true;
		Memoria m = Memoria.de(p);
		m.add(Memoria.FIOS_ARMADOS, 1);
		m.salvar();
		ModSons.tocar(p.level(), p.getX(), p.getY() + 0.4, p.getZ(), ModSons.Som.PANO, 0.32F, 1.18F);
		p.sendOverlayMessage(Component.translatable("message.sussurros.fio.armado")
				.withStyle(s -> s.withColor(0xBEB9A7).withItalic(true)));
		Depuracao.log(p, tick / 20, String.format(Locale.ROOT,
				"VIGILIA armada pos=%s raio=%.1f duracao=180s", pos(e.vigiaX, e.vigiaY, e.vigiaZ), e.vigiaRaio));
		return true;
	}

	private static void verificarFioVigilia(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick) {
		if (!e.vigiaAtiva) {
			return;
		}
		if (tick >= e.vigiaAteTick) {
			e.vigiaAtiva = false;
			Depuracao.log(p, seg, "VIGILIA expirou intacta pos=" + pos(e.vigiaX, e.vigiaY, e.vigiaZ));
			return;
		}
		HospedeEntity h = e.criatura;
		if (h == null || h.isRemoved()) {
			return;
		}
		double dx = h.getX() - e.vigiaX;
		double dy = h.getY() - e.vigiaY;
		double dz = h.getZ() - e.vigiaZ;
		if (dx * dx + dy * dy + dz * dz > e.vigiaRaio * e.vigiaRaio) {
			return;
		}
		e.vigiaAtiva = false;
		ModSons.tocar(level, e.vigiaX, e.vigiaY + 0.3, e.vigiaZ, ModSons.Som.ESTALO, 0.85F, 0.72F);
		ModSons.tocar(level, h.getX(), h.getY() + 1.0, h.getZ(), ModSons.Som.PANO, 0.34F, 0.84F);
		p.sendOverlayMessage(Component.translatable("message.sussurros.fio.rompeu")
				.withStyle(s -> s.withColor(0xDDD6C2).withItalic(true)));
		// Usa a Memoria do tick: uma cópia própria aqui era sobrescrita pelo salvar() no fim de segundo(),
		// e o fio rompido nunca chegava ao disco (nem ao Caderno).
		m.add(Memoria.FIOS_ROMPIDOS, 1);
		Vestigios.de(p).registrar(BlockPos.containing(e.vigiaX, e.vigiaY, e.vigiaZ), Vestigios.Tipo.VIGILIA, seg);
		somarObsessao(e, 1.5);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"VIGILIA rompeu manifestacao=%s distCentro=%.1f pos=%s", h.getIdManifestacao(),
				Math.sqrt(dx * dx + dy * dy + dz * dz), pos(e.vigiaX, e.vigiaY, e.vigiaZ)));
	}

	/**
	 * Sino Oco: faz uma pergunta ao mundo, não uma varredura. Uma manifestação presente pode responder
	 * de onde está; sem criatura, um lugar do Rastro pode responder; às vezes só há silêncio.
	 */
	public static void usarSino(ServerPlayer p) {
		ServerLevel level = p.level();
		EstadoJogador e = estado(p);
		Memoria m = Memoria.de(p);
		long seg = level.getGameTime() / 20;
		if (!e.obsessaoCarregada) {
			e.obsessao = m.get(Memoria.OBSESSAO) / 10.0;
			e.obsessaoCarregada = true;
		}
		m.add(Memoria.SINOS_USADOS, 1);
		int usosSino = m.get(Memoria.SINOS_USADOS);
		m.add(Memoria.INQUIETACAO, 6);
		m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
		somarObsessao(e, 2.0);
		m.set(Memoria.OBSESSAO, (int) Math.round(e.obsessao * 10));
		m.salvar();

		ModSons.tocar(level, p.getX(), p.getY() + 1.0, p.getZ(), ModSons.Som.GRAVE, 0.38F, 1.35F);
		ModSons.tocar(level, p.getX(), p.getY() + 1.0, p.getZ(), ModSons.Som.ESTALO, 0.30F, 0.72F);
		Depuracao.log(p, seg, "SINO tocado usos=" + usosSino);

		int atraso = 28 + level.getRandom().nextInt(35);
		agendar(level, atraso, () -> {
			if (p.isRemoved()) {
				return;
			}
			HospedeEntity h = e.criatura;
			boolean hospedePerto = h != null && !h.isRemoved() && h.distanceToSqr(p) <= 52 * 52;
			double chanceEnganar = usosSino < 4 ? 0.0 : Math.min(0.45, 0.12 + (usosSino - 4) * 0.05);
			boolean enganar = hospedePerto && level.getRandom().nextDouble() < chanceEnganar;
			if (hospedePerto && !enganar) {
				ModSons.Som som = level.getRandom().nextBoolean() ? ModSons.Som.RESPIRACAO : ModSons.Som.PANO;
				float volume = volumePara(p, h.getX(), h.getY(), h.getZ(), 0.65F);
				ModSons.tocar(level, h.getX(), h.getY() + 1.2, h.getZ(), som, volume, 0.78F + level.getRandom().nextFloat() * 0.18F);
				Depuracao.log(p, level.getGameTime() / 20, "SINO resposta=HOSPEDE manifestacao=" + h.getIdManifestacao()
						+ " dist=" + String.format(Locale.ROOT, "%.1f", Math.sqrt(h.distanceToSqr(p))));
				return;
			}
			long agora = level.getGameTime() / 20;
			Vestigios.Marca marca = Vestigios.de(p).maisPerto(p.getX(), p.getY(), p.getZ(), 42, agora);
			if (!enganar && marca != null && level.getRandom().nextFloat() < 0.38F) {
				BlockPos mp = marca.pos();
				ModSons.Som som = marca.tipo() == Vestigios.Tipo.VIGILIA ? ModSons.Som.ESTALO : ModSons.Som.PANO;
				ModSons.tocar(level, mp.getX() + 0.5, mp.getY() + 0.7, mp.getZ() + 0.5, som,
						volumePara(p, mp.getX() + 0.5, mp.getY() + 0.5, mp.getZ() + 0.5, 0.52F), 0.72F);
				Depuracao.log(p, agora, "SINO resposta=VESTIGIO tipo=" + marca.tipo() + " idade=" + marca.idade(agora)
						+ "s pos=" + pos(mp.getX(), mp.getY(), mp.getZ()));
				return;
			}
			Rastro.Ponto pt = pontoDoRastro(p, e, level.getGameTime() / 20, 18, 300, 10, 38, true);
			if (pt != null && (enganar || level.getRandom().nextFloat() < 0.62F)) {
				ModSons.Som som = level.getRandom().nextBoolean() ? ModSons.Som.ESTALO : ModSons.Som.MADEIRA;
				ModSons.tocar(level, pt.x(), pt.y() + 0.8, pt.z(), som, volumePara(p, pt.x(), pt.y(), pt.z(), 0.55F), 0.82F);
				Depuracao.log(p, level.getGameTime() / 20, "SINO resposta=" + (enganar ? "ISCA_RASTRO" : "RASTRO") + " idade="
						+ (level.getGameTime() / 20 - pt.seg()) + "s pos=" + pos(pt.x(), pt.y(), pt.z()));
			} else {
				Depuracao.log(p, level.getGameTime() / 20, "SINO resposta=SILENCIO");
			}
		});
	}

	private static void revelarVestigio(ServerLevel level, ServerPlayer p, Vestigios.Marca marca, long agora) {
		BlockPos mp = marca.pos();
		double x = mp.getX() + 0.5;
		double y = mp.getY() + 0.15;
		double z = mp.getZ() + 0.5;
		int dist = (int) Math.round(Math.sqrt(Math.pow(p.getX() - x, 2) + Math.pow(p.getZ() - z, 2)));
		p.sendOverlayMessage(Component.translatable("message.sussurros.olho.vestigio",
				Component.translatable("message.sussurros.dir." + direcaoPara(p, x, z)), dist)
				.withStyle(st -> st.withColor(0xA3A098).withItalic(true)));
		for (int i = 0; i < 4; i++) {
			final int n = i;
			agendar(level, i * 8, () -> {
				if (!p.isRemoved()) {
					level.sendParticles(ParticleTypes.ASH, x, y + n * 0.03, z, 7, 0.42, 0.06, 0.42, 0.002);
				}
			});
		}
		Depuracao.log(p, agora, "OLHO vestigio tipo=" + marca.tipo() + " idade=" + marca.idade(agora)
				+ "s dist=" + dist + " pos=" + pos(mp.getX(), mp.getY(), mp.getZ()));
	}

	/** Vela Pálida: cria uma zona de calma. Cada vela usada dura menos (ele se acostuma). */
	public static void acenderVela(ServerPlayer p) {
		ServerLevel level = p.level();
		Memoria m = Memoria.de(p);
		EstadoJogador e = estado(p);
		int velas = m.get(Memoria.VELAS);
		int segundos = Math.max(30, 90 - 12 * (velas / 2));

		e.zonaX = p.getX();
		e.zonaY = p.getY();
		e.zonaZ = p.getZ();
		e.zonaRaio = 8;
		e.zonaAteTick = level.getGameTime() + segundos * 20L;

		m.add(Memoria.VELAS, 1);
		m.add(Memoria.INQUIETACAO, -60);
		somarObsessao(e, -5);
		m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
		m.salvar();

		p.sendOverlayMessage(Component.translatable("message.sussurros.vela.acendeu").withStyle(s -> s.withColor(0xE8E0C8).withItalic(true)));
	}

	/** Olho Sussurrante: revela onde ele está. Mas olhar chama atenção. */
	public static void usarOlho(ServerPlayer p) {
		ServerLevel level = p.level();
		Memoria m = Memoria.de(p);
		EstadoJogador e = estado(p);
		m.add(Memoria.OLHOS, 1);
		m.add(Memoria.TEMPO, 90);
		// Sempre no log: usar o Olho adianta a assombração, e sem esta linha as fases chegavam "cedo" sem explicação.
		Depuracao.log(p, level.getGameTime() / 20, "OLHO usado fase=" + m.get(Memoria.FASE) + " tempo=" + m.get(Memoria.TEMPO) + " (+90)");

		HospedeEntity c = e.criatura;
		if (c != null && !c.isRemoved()) {
			c.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160));
			int dist = (int) Math.sqrt(c.distanceToSqr(p));
			p.sendOverlayMessage(Component.translatable("message.sussurros.olho.revelou",
					Component.translatable("message.sussurros.dir." + direcao(p, c)), dist)
					.withStyle(s -> s.withColor(0xB01818)));
		} else {
			long agora = level.getGameTime() / 20;
			// Usar o Olho em sequência não pode virar um botão de chamar a criatura (um log real mostrou três
			// aparições em dois minutos). A chance cai pela metade a cada uso recente, e depois de chamar
			// uma aparição ele fica um tempo sem poder chamar outra.
			while (!e.olhoUsos.isEmpty() && agora - e.olhoUsos.peekFirst() > OLHO_JANELA) {
				e.olhoUsos.removeFirst();
			}
			int usosRecentes = e.olhoUsos.size();
			e.olhoUsos.addLast(agora);
			float chance = 0.4F / (1 << Math.min(usosRecentes, 4));
			boolean emRecarga = agora < e.olhoRecargaAte;
			Vestigios.Marca marca = Vestigios.de(p).maisPerto(p.getX(), p.getY(), p.getZ(), 48, agora);
			if (marca != null) {
				revelarVestigio(level, p, marca, agora);
			} else if (m.get(Memoria.FASE) >= 3 && !emRecarga && level.getRandom().nextFloat() < chance) {
			e.olhoRecargaAte = agora + OLHO_RECARGA;
			Depuracao.log(p, agora, String.format(Locale.ROOT, "OLHO chamou aparicao usosRecentes=%d chance=%.2f", usosRecentes, chance));
			// Você olhou. Ele sentiu.
			p.sendOverlayMessage(Component.translatable("message.sussurros.olho.atencao").withStyle(s -> s.withColor(0xB01818).withItalic(true)));
			agendar(level, 60 + level.getRandom().nextInt(80), () -> {
				if (!p.isRemoved() && (e.criatura == null || e.criatura.isRemoved())) {
					PedidoManifestacao pedido = PedidoManifestacao.deOrigem(HospedeEntity.Origem.OLHO, null);
					try {
						invocar(level, p, e, HospedeEntity.Modo.OBSERVAR, 100, 170, 26, 40, 20 * 20, 1.0, true, pedido);
					} finally {
					}
				}
			});
			} else {
				if (m.get(Memoria.FASE) >= 3) {
					Depuracao.log(p, agora, String.format(Locale.ROOT, "OLHO nada usosRecentes=%d chance=%.2f recarga=%s",
							usosRecentes, chance, emRecarga ? "sim" : "nao"));
				}
				p.sendOverlayMessage(Component.translatable("message.sussurros.olho.nada").withStyle(s -> s.withColor(0x5A5A5A).withItalic(true)));
			}
		}
		m.salvar();
	}

	// =====================================================================
	// Usado pelo comando de teste
	// =====================================================================

	/**
	 * Pula para uma fase. Subindo, entrega o que as fases puladas teriam entregado (página, Olho).
	 * Descendo, o tempo de assombração PRECISA voltar (senão ele subiria de novo no segundo seguinte).
	 * Devolve a mensagem para o chat.
	 */
	public static String definirFase(ServerPlayer p, int fase) {
		ServerLevel level = p.level();
		Memoria m = Memoria.de(p);
		EstadoJogador e = estado(p);
		int atual = m.get(Memoria.FASE);
		int tempoAntes = m.get(Memoria.TEMPO);
		String msg;
		if (fase > atual) {
			m.set(Memoria.TEMPO, Math.max(tempoAntes, LIMIAR_FASE[fase]));
			for (int f = atual + 1; f <= fase; f++) {
				m.set(Memoria.FASE, f);
				transicao(level, p, m, f);
			}
			msg = "Fase " + fase + ". O que as fases puladas entregariam caiu atrás de você.";
		} else if (fase < atual) {
			m.set(Memoria.TEMPO, LIMIAR_FASE[fase]);
			m.set(Memoria.FASE, fase);
			msg = "Fase " + fase + ". Atenção: o tempo de assombração voltou de " + tempoAntes + " s para " + LIMIAR_FASE[fase] + " s.";
		} else {
			msg = "Você já está na fase " + fase + ".";
		}
		m.salvar();
		e.proximoEvento = -1;
		Depuracao.log(p, level.getGameTime() / 20, "COMANDO fase " + atual + " -> " + fase);
		return msg;
	}

	private static boolean usaCriatura(Evento ev) {
		return ev == Evento.PRESENCA || ev == Evento.ATRAS || ev == Evento.TUMULO || ev == Evento.CACA
				|| ev == Evento.ESPERA || ev == Evento.ESPREITA || ev == Evento.VULTO;
	}

	/** Por que este evento não pode ser forçado aqui? null = pode. */
	@Nullable
	private static String motivoImpossivel(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Evento ev) {
		return switch (ev) {
			case PASSAGEM, ANIMAIS, VESTIGIO, LUZ_ERRADA, SINAL_DISTANTE, RUIDO_RETORNO, OBJETO_FORA_LUGAR, TRILHA_INTERROMPIDA -> null;
			case ECO -> e.acoes.isEmpty()
					? "precisa que você tenha quebrado algum bloco ou usado uma porta desde que entrou no mundo." : null;
			case SEGUIDOR -> e.rastro.tamanho() < 8
					? "precisa que você tenha caminhado o suficiente para formar um Rastro." : null;
			case PEGADAS -> e.rastro.tamanho() < 6
					? "precisa que você tenha caminhado um pouco para formar um Rastro." : null;
			case VULTO -> !podeVulto(level, p)
					? "precisa ser de dia e você precisa estar ao ar livre (à noite uma silhueta tão longe não aparece)." : null;
			case PORTA, BATIDA -> !temPorta(level, p)
					? "precisa de uma porta de madeira a até 11 blocos (ou a porta de sempre a até 24), FORA da sua visão." : null;
			case TOCHA -> acharTochaAtras(level, p, 12) == null
					? "precisa de uma tocha comum (não lanterna) a até 12 blocos, ATRÁS de você." : null;
			case ECO_CHAT -> e.falas.isEmpty()
					? "precisa que você tenha escrito algo no chat (3 a 60 letras). Comandos não contam." : null;
			case TUMULO -> {
				if (m.get(Memoria.TEM_MORTE) != 1) {
					yield "precisa que você já tenha morrido neste mundo.";
				}
				double d = distanciaSqr(p, m.get(Memoria.MORTE_X), m.get(Memoria.MORTE_Z));
				yield d > 100 * 100 ? "você está a mais de 100 blocos de onde morreu." : null;
			}
			case ESPERA -> !temZonaCalma(p) ? "precisa de uma Vela Pálida acesa." : null;
			case NEBLINA -> !level.canSeeSky(p.blockPosition().above()) ? "precisa estar a céu aberto." : null;
			case CANTIGA -> null; // por comando ele assobia mesmo sem ter aprendido
			default -> null;
		};
	}

	/**
	 * Força um evento por comando. Ele acontece de verdade, mas NÃO conta para agenda, pressão,
	 * anti-repetição nem aprendizado, e a criatura que ele criar não deixa marcas na memória.
	 * Devolve null se deu certo, ou o motivo de não ter acontecido.
	 */
	@Nullable
	public static String forcarEvento(ServerPlayer p, Evento ev) {
		ServerLevel level = p.level();
		EstadoJogador e = estado(p);
		Memoria m = Memoria.de(p);
		String motivo = motivoImpossivel(level, p, m, e, ev);
		if (motivo != null) {
			return motivo;
		}
		// Só tira a criatura atual se o novo evento também precisa dela (0.4 tirava sempre:
		// forçar um ECO encerrava a CAÇA que estava acontecendo).
		if (usaCriatura(ev) && e.criatura != null && !e.criatura.isRemoved()) {
			e.criatura.sumir(level, false, "SUBSTITUIDA_POR_COMANDO");
		}
		long tick = level.getGameTime();
		boolean ok;
		e.forcando = true;
		try {
			ok = executar(level, p, m, e, ev, tick / 20, tick);
		} finally {
			e.forcando = false;
		}
		m.salvar();
		if (ok) {
			return null;
		}
		// O motivo depende do tipo de evento: só os que usam a criatura precisam de chão livre para ela.
		return usaCriatura(ev)
				? "não achei um lugar livre para ele (chão firme com 3 blocos de ar em cima, fora da sua tela). Tente num lugar mais aberto."
				: "não encontrou condições agora (sem ponto do seu caminho por perto, sem ação recente ao alcance ou sem lugar livre). Ande um pouco e tente de novo.";
	}

	public static String testarPressagio(ServerPlayer p) {
		return Atmosfera.testarPressagio(p.level(), p);
	}

	public static String testarPerturbacao(ServerPlayer p, Perturbacao tipo) {
		return Atmosfera.testarPerturbacao(p.level(), p, tipo);
	}

	public static String testarEstrutura(ServerPlayer p, String tipo) {
		return EstruturasSussurros.testar(p.level(), p, tipo);
	}

	/** Estado do momento (não salvo), para o comando de teste. */
	public static String resumo(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		return String.format(Locale.ROOT,
				"estado=%s (%ds) V=%d pressao=%.0f obsessao=%.0f contexto=%s atm=%.1f cena=%s cenaCasa=%s cenaTunel=%s cenaCampo=%s cenaMarco=%s cenaJanela=%s proximo=%ds recentes=%s sequencia=%s elos=%d rastro=%d manifestacaoAtiva=%s cenaAtiva=%s | %s",
				e.estado, seg - e.estadoDesde, e.ultimoV, e.pressao, e.obsessao, e.contexto, e.atmosfera.orcamento, e.cena, e.cenaCasa, e.cenaTunel, e.cenaCampo, e.cenaMarco, e.cenaJanela, Math.max(0, e.proximoEvento - seg),
				e.recentes, e.sequencia, e.elosCadeia, e.rastro.tamanho(), manifestacaoAtiva(e), cenaAtiva(e), Perfil.resumo(Memoria.de(p)));
	}

	private static final String HOSPEDE_ATIVO = "Já existe um Hóspede ativo. Espere ele sumir, ou use /sussurros cena parar.";

	/** Cada cena termina com um a dois minutos de silêncio. Para quem está testando, isso trava a próxima: o comando parar resolve. */
	private static String cenaOcupada(EstadoJogador e) {
		return "Já existe outra cena em andamento (" + cenaAtiva(e) + "). Para interromper: /sussurros cena parar";
	}

	/**
	 * Interrompe a cena composta em andamento (inclusive o silêncio do fim) e tira o Hóspede de cena.
	 * Existe para testar uma cena atrás da outra; num jogo normal as cenas terminam sozinhas.
	 */
	public static String pararCenas(ServerPlayer p) {
		ServerLevel level = p.level();
		EstadoJogador e = estado(p);
		long seg = level.getGameTime() / 20;
		String id = cenaAtiva(e);
		boolean haviaCena = e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA;
		boolean haviaHospede = e.criatura != null && !e.criatura.isRemoved();
		if (!haviaCena && !haviaHospede) {
			return "Não há cena nem Hóspede para interromper.";
		}
		e.cenaCasa = EstadoJogador.CenaCasa.NENHUMA;
		e.cenaTunel = EstadoJogador.CenaTunel.NENHUMA;
		e.cenaCampo = EstadoJogador.CenaCampo.NENHUMA;
		e.cenaMarco = EstadoJogador.CenaMarco.NENHUMA;
		e.cenaJanela = EstadoJogador.CenaJanela.NENHUMA;
		if (haviaHospede) {
			e.criatura.sumir(level, false, "PARADA_POR_COMANDO");
		}
		Depuracao.log(p, seg, "COMANDO cena parar: cena=" + (haviaCena ? id : "-") + " hospede=" + (haviaHospede ? "sim" : "nao"));
		return haviaCena ? "Cena " + id + " interrompida. Já pode começar outra." : "Hóspede retirado. Já pode começar uma cena.";
	}

	/** Começa a cena "Ele voltou com você" agora. Teste: não conta para o aprendizado nem para a memória. */
	public static String testarCenaCasa(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return cenaOcupada(e);
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return HOSPEDE_ATIVO;
		}
		CenaVoltouComVoce.iniciarCenaCasa(p, e, seg, p.level().getRandom(), true);
		return "Cena \"Ele voltou com você\" em 10-20 s (teste: não conta para o aprendizado). "
				+ "Funciona melhor perto da sua porta, depois de ter andado um pouco.";
	}

	/** Começa a cena "Algo no túnel" agora. Teste: não conta para aprendizado ou memória. */
	public static String testarCenaTunel(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return cenaOcupada(e);
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return HOSPEDE_ATIVO;
		}
		CenaAlgoNoTunel.iniciarCenaTunel(p, e, seg, p.level().getRandom(), true);
		return "Cena \"Algo no túnel\" iniciada (teste: não aprende). Funciona melhor no subsolo depois de minerar e andar um pouco.";
	}

	/** Começa a cena "Na linha das arvores" agora. Teste: nao conta para aprendizado ou memoria. */
	public static String testarCenaCampo(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA || e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA || e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return cenaOcupada(e);
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return HOSPEDE_ATIVO;
		}
		CenaLinhaDasArvores.iniciarCenaCampo(p, e, seg, p.level().getRandom(), true);
		return "Cena \"Na linha das arvores\" iniciada (teste: nao aprende). Funciona melhor em area externa com algum espaco e cobertura ao redor.";
	}

	/** Começa a cena "Foi aqui" agora. Em teste não exige um marco real e não aprende. */
	public static String testarCenaMarco(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA || e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA || e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return cenaOcupada(e);
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return HOSPEDE_ATIVO;
		}
		CenaFoiAqui.iniciarCenaMarco(p, e, seg, p.level().getRandom(), true, p.blockPosition());
		return "Cena \"Foi aqui\" iniciada (teste: não aprende). Em jogo normal ela só nasce ao revisitar um lugar marcado por uma reação forte anterior.";
	}

	/** Começa a cena "Do outro lado do vidro" agora. Teste: não aprende nem cria memória persistente. */
	public static String testarCenaJanela(ServerPlayer p) {
		EstadoJogador e = estado(p);
		long seg = p.level().getGameTime() / 20;
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return cenaOcupada(e);
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return HOSPEDE_ATIVO;
		}
		atualizarCacheAmbiente(p.level(), p, e);
		if (e.janelas.isEmpty()) {
			return "Não encontrei vidro por perto. Faça o teste dentro de uma casa com janela.";
		}
		CenaDoOutroLadoDoVidro.iniciarCenaJanela(p, e, seg, p.level().getRandom(), true);
		return "Cena \"Do outro lado do vidro\" iniciada (teste: não aprende). Fique dentro de uma casa com janela e jogue normalmente.";
	}

	/** Toca agora um eco de ação vindo de onde você fez a ação ou de um ponto do rastro. Teste: não aprende. */
	public static String testarEcoDeAcao(ServerPlayer p) {
		ServerLevel level = p.level();
		EstadoJogador e = estado(p);
		long seg = level.getGameTime() / 20;
		if (e.acoes.isEmpty()) {
			return "Ainda não há ações para ecoar: quebre alguns blocos ou use uma porta.";
		}
		EstadoJogador.Acao acao = sortearAcao(e, level.getRandom());
		StringBuilder nota = new StringBuilder();
		Vec3 lugar = lugarParaEco(p, e, acao, seg, true, nota);
		if (lugar == null) {
			return "Nenhum lugar seu a 8-24 blocos agora (nem onde você fez " + acao.tipo()
					+ ", nem no rastro). Ande um pouco e tente de novo.";
		}
		Vec3 som = eco(level, p, acao.som(), acao.tipo() == EstadoJogador.TipoAcao.PORTA, false, null, lugar);
		logEco(p, "COMANDO ECO", acao, som, nota.toString(), seg);
		return "Eco de " + acao.tipo() + " de " + (seg - acao.seg()) + "s atrás (" + nota + ").";
	}

	/** Para testar a sequência de ameaça sem esperar: ela ainda exige fase 3+, ESCALANDO e um bom momento. */
	public static String definirObsessao(ServerPlayer p, int valor) {
		EstadoJogador e = estado(p);
		Memoria m = Memoria.de(p);
		e.obsessao = limitar(valor, 0, 100);
		e.obsessaoCarregada = true;
		m.set(Memoria.OBSESSAO, (int) Math.round(e.obsessao * 10));
		m.salvar();
		Depuracao.log(p, p.level().getGameTime() / 20, "COMANDO obsessao -> " + valor);
		return "Obsessão: " + valor + ". A ameaça começa quando ele estiver escalando, na fase 3+, num bom momento.";
	}

	// ===== Telemetria (0.4.2a-test): só leitura de valores e log, sem números aleatórios =====

	public static boolean telemetriaAtiva() {
		return Depuracao.ativo;
	}

	private static String novoIdManifestacao() {
		return String.format(Locale.ROOT, "M%03d", ++contadorManifestacao);
	}

	static String novoIdCena() {
		return String.format(Locale.ROOT, "C%03d", ++contadorCena);
	}

	static String pos(double x, double y, double z) {
		return String.format(Locale.ROOT, "(%.0f,%.0f,%.0f)", x, y, z);
	}

	/** Cena em andamento: casa, tunel, campo ou sequencia de ameaca. "-" se nenhuma. */
	private static String cenaAtiva(EstadoJogador e) {
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA) {
			return e.cenaCasaId;
		}
		if (e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA) {
			return e.cenaTunelId;
		}
		if (e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA) {
			return e.cenaCampoId;
		}
		if (e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA) {
			return e.cenaMarcoId;
		}
		if (e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return e.cenaJanelaId;
		}
		if (e.estado == EstadoDiretor.AMEACANDO && e.cena != EstadoJogador.Cena.NENHUMA) {
			return e.ameacaId;
		}
		return "-";
	}

	static String manifestacaoAtiva(EstadoJogador e) {
		HospedeEntity h = e.criatura;
		return h != null && !h.isRemoved() ? h.getIdManifestacao() : "-";
	}

	/** A criatura entrou na sua tela (ou foi encarada pela primeira vez). Chamado por ela, só com o log ligado. */
	public static void registrarVisao(ServerPlayer p, HospedeEntity h, String tipo, int n, int ticksFora) {
		if (!Depuracao.ativo) {
			return;
		}
		Depuracao.log(p, p.level().getGameTime() / 20, String.format(Locale.ROOT,
				"HOSPEDE id=%s %s n=%d dist=%.1f ang=%.0f luz=%d modo=%s%s",
				h.getIdManifestacao(), tipo, n, Math.sqrt(h.distanceToSqr(p)), Math.abs(anguloRelativo(p, h.getX(), h.getZ())),
				luzEfetiva(p.level(), h.blockPosition().above()), h.getModo(),
				n > 1 ? String.format(Locale.ROOT, " foraDaTela=%.1fs", ticksFora / 20.0) : ""));
	}

	/** A criatura foi descartada por falta de alvo válido (longe demais, outra dimensão, jogador saiu). Só log. */
	public static void registrarFimSemAlvo(ServerPlayer p, HospedeEntity h) {
		Depuracao.log(p, p.level().getGameTime() / 20, "HOSPEDE id=" + h.getIdManifestacao() + " sumiu motivo=SEM_ALVO");
	}

	static void logSilencioInicio(ServerPlayer p, long seg, String motivo, String cena, long duracao) {
		Depuracao.log(p, seg, String.format(Locale.ROOT, "SILENCIO inicio motivo=%s cena=%s duracao=%ds silencioAte=%ds",
				motivo, cena, duracao, seg + duracao));
	}

	/** O recuo (RECUANDO) guarda o motivo para o log do fim. Só telemetria. */
	private static void marcarSilencioDoRecuo(ServerPlayer p, EstadoJogador e, long seg, String motivo, String cena) {
		e.silencioMotivo = motivo;
		e.silencioCena = cena;
		logSilencioInicio(p, seg, motivo, cena, e.duracaoEstado);
	}

	private static void logFimDoRecuo(ServerPlayer p, EstadoJogador e, long seg) {
		Depuracao.log(p, seg, "SILENCIO fim motivo=" + (e.silencioMotivo.isEmpty() ? "RECUANDO" : e.silencioMotivo)
				+ " cena=" + e.silencioCena);
		e.silencioMotivo = "";
		e.silencioCena = "-";
	}

	public static boolean alternarDepuracao(boolean ligar) {
		Depuracao.ativo = ligar;
		return ligar;
	}

	public static boolean depuracaoLigada() {
		return Depuracao.ativo;
	}

	public static String arquivoDepuracao() {
		return Depuracao.arquivo().toString();
	}

	public static void esquecer(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		// Estruturas são físicas: apagar a memória não remove blocos do mundo. Preservamos apenas
		// os flags/coordenadas de geração para não duplicá-las a cada reset de teste.
		String[] persistir = {
				Memoria.ESTRUTURA_MARCO, Memoria.ESTRUTURA_MARCO_X, Memoria.ESTRUTURA_MARCO_Y, Memoria.ESTRUTURA_MARCO_Z,
				Memoria.ESTRUTURA_POSTO, Memoria.ESTRUTURA_POSTO_X, Memoria.ESTRUTURA_POSTO_Y, Memoria.ESTRUTURA_POSTO_Z,
				Memoria.ESTRUTURA_NICHO, Memoria.ESTRUTURA_NICHO_X, Memoria.ESTRUTURA_NICHO_Y, Memoria.ESTRUTURA_NICHO_Z
		};
		Map<String, Integer> fisico = new HashMap<>();
		for (String chave : persistir) {
			if (m.get(chave) != 0) fisico.put(chave, m.get(chave));
		}
		m.limpar();
		for (Map.Entry<String, Integer> en : fisico.entrySet()) m.set(en.getKey(), en.getValue());
		m.salvar();
		Lugares.apagar(p);
		Vestigios.apagar(p);
		ESTADOS.remove(p.getUUID());
	}

	// =====================================================================
	// Utilidades
	// =====================================================================

	static boolean ehNoiteParaTeste(ServerLevel level) {
		return ehNoite(level);
	}

	public static boolean ehNoite(ServerLevel level) {
		long t = Math.floorMod(level.getDefaultClockTime(), 24000L);
		return t >= 13000 && t < 23000;
	}

	private static boolean longeDeCasa(Memoria m, ServerPlayer p) {
		if (m.get(Memoria.TEM_CAMA) != 1) {
			return false;
		}
		double dx = p.getX() - m.get(Memoria.CAMA_X);
		double dz = p.getZ() - m.get(Memoria.CAMA_Z);
		return dx * dx + dz * dz > 80 * 80;
	}

	static double distanciaSqr(ServerPlayer p, double x, double z) {
		double dx = p.getX() - x;
		double dz = p.getZ() - z;
		return dx * dx + dz * dz;
	}

	public static boolean temZonaCalma(ServerPlayer p) {
		EstadoJogador e = ESTADOS.get(p.getUUID());
		return e != null && e.zonaAteTick > p.level().getGameTime();
	}

	public static boolean emZonaCalma(ServerPlayer p, double x, double y, double z) {
		EstadoJogador e = ESTADOS.get(p.getUUID());
		if (e == null || e.zonaAteTick <= p.level().getGameTime()) {
			return false;
		}
		double dx = x - e.zonaX;
		double dy = y - e.zonaY;
		double dz = z - e.zonaZ;
		return dx * dx + dy * dy + dz * dz <= e.zonaRaio * e.zonaRaio;
	}

	/** O jogador está olhando para a entidade (dentro de um cone) e a enxerga de verdade? */
	public static boolean estaOlhando(ServerPlayer p, Entity alvo, double limiar) {
		Vec3 olho = p.getEyePosition();
		Vec3 corpo = alvo.position().add(0, alvo.getBbHeight() * 0.6, 0);
		Vec3 direcao = corpo.subtract(olho).normalize();
		double dot = p.getViewVector(1.0F).dot(direcao);
		return dot > limiar && Percepcao.linhaDeVisao(p, alvo);
	}

	/**
	 * Olhando E dá para enxergar: de longe, no breu total, uma silhueta preta não aparece na tela.
	 * (0.4 contava como "visto" qualquer olhar na direção, mesmo a 50 blocos no escuro.)
	 */
	public static boolean estaVendo(ServerPlayer p, Entity alvo, double limiar) {
		if (!estaOlhando(p, alvo, limiar)) {
			return false;
		}
		if (p.distanceToSqr(alvo) < 20 * 20) {
			return true;
		}
		return luzEfetiva(p.level(), alvo.blockPosition().above()) >= 4;
	}

	static boolean pontoNaFrente(ServerPlayer p, Vec3 ponto, double limiar) {
		Vec3 direcao = ponto.subtract(p.getEyePosition()).normalize();
		return p.getViewVector(1.0F).dot(direcao) > limiar;
	}

	/** Ponto no chão a "dist" blocos do jogador, girado "graus" a partir de onde ele olha. */
	static Vec3 pontoRelativo(ServerPlayer p, double graus, double dist) {
		Vec3 view = p.getViewVector(1.0F);
		double vx = view.x;
		double vz = view.z;
		double len = Math.sqrt(vx * vx + vz * vz);
		if (len < 1.0E-4) {
			vx = 0;
			vz = 1;
			len = 1;
		}
		vx /= len;
		vz /= len;
		double a = Math.toRadians(graus);
		double rx = vx * Math.cos(a) - vz * Math.sin(a);
		double rz = vx * Math.sin(a) + vz * Math.cos(a);
		return new Vec3(p.getX() + rx * dist, p.getY(), p.getZ() + rz * dist);
	}

	/** Procura um chão firme com 3 blocos de ar em cima (a criatura é alta). */
	@Nullable
	static BlockPos acharChao(ServerLevel level, double x, double yBase, double z) {
		for (int dy = 6; dy >= -12; dy--) {
			BlockPos pos = BlockPos.containing(x, yBase + dy, z);
			BlockPos baixo = pos.below();
			if (level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.above()).isAir()
					&& level.getBlockState(pos.above(2)).isAir()
					&& !level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty()) {
				return pos;
			}
		}
		return null;
	}

	private static void soltarAtras(ServerLevel level, ServerPlayer p, ItemStack stack, double dist) {
		Vec3 atras = pontoRelativo(p, 180, dist);
		BlockPos chao = acharChao(level, atras.x, p.getY() + 1, atras.z);
		double x = chao != null ? chao.getX() + 0.5 : p.getX();
		double y = chao != null ? chao.getY() + 0.1 : p.getY();
		double z = chao != null ? chao.getZ() + 0.5 : p.getZ();
		ItemEntity item = new ItemEntity(level, x, y, z, stack);
		item.setDeltaMovement(0, 0, 0);
		level.addFreshEntity(item);
	}

	private static String direcaoPara(ServerPlayer p, double x, double z) {
		Vec3 view = p.getViewVector(1.0F);
		double tx = x - p.getX();
		double tz = z - p.getZ();
		double dot = view.x * tx + view.z * tz;
		double cross = view.x * tz - view.z * tx;
		double ang = Math.toDegrees(Math.atan2(cross, dot));
		if (Math.abs(ang) <= 45) {
			return "frente";
		} else if (Math.abs(ang) >= 135) {
			return "atras";
		}
		return ang > 0 ? "direita" : "esquerda";
	}

	private static String direcao(ServerPlayer p, Entity alvo) {
		Vec3 view = p.getViewVector(1.0F);
		double tx = alvo.getX() - p.getX();
		double tz = alvo.getZ() - p.getZ();
		double dot = view.x * tx + view.z * tz;
		double cross = view.x * tz - view.z * tx;
		double ang = Math.toDegrees(Math.atan2(cross, dot));
		if (Math.abs(ang) <= 45) {
			return "frente";
		} else if (Math.abs(ang) >= 135) {
			return "atras";
		} else {
			return ang > 0 ? "direita" : "esquerda";
		}
	}

	private static float diferencaAngulo(float a, float b) {
		float d = (a - b) % 360.0F;
		if (d >= 180.0F) {
			d -= 360.0F;
		}
		if (d < -180.0F) {
			d += 360.0F;
		}
		return d;
	}
}
