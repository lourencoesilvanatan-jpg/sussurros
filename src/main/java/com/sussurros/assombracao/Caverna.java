package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import com.sussurros.registro.ModSons;

/**
 * A caverna com corpo (0.9.0-alpha22): três coisas que se veem debaixo da terra, no lugar de mais luz e mais som.
 *
 * Em 11/10/2026 o dono disse que na caverna tudo era "mudar a tocha de cor, e sons", e pediu "mais interações
 * físicas". Das famílias do que o mod faz (ver Familias), a caverna dele só teve duas. Aqui entram as outras:
 *
 *  - A RACHADURA (marca). Ele está minerando, e no bloco ao lado do que acabou de quebrar aparecem rachaduras
 *    que crescem sozinhas até a metade e param, com duas batidas surdas vindas de dentro da pedra. Se ele
 *    quebrar aquele bloco: pedra, e nada atrás. É o pacote de progresso de quebra do jogo mandado só para ele,
 *    com um número que não é de entidade nenhuma: o mundo não muda e quem está ao lado não vê. É resposta a
 *    uma ação dele: não gasta atenção, só impõe o respiro. No máximo uma por descida, e só em pedra natural
 *    (nunca num bloco que ele possa ter posto).
 *  - A PEDRINHA (objeto). Alguma coisa pequena vem do escuro e quica no chão até perto dele: lascas do chão
 *    do lugar e um estalo seco em cada toque. Partículas e som, só para ele. Sem item, sem entidade, sem dano.
 *    No máximo duas por sessão.
 *  - OS MORCEGOS (corpo). Um baque surdo numa boca de túnel fora da tela, e de três a cinco morcegos saem de
 *    lá e passam por ele. São morcegos de verdade (todos veem), que não fazem mal, etiquetados e tirados em
 *    dez a quinze segundos. O que os espantou não aparece; mas a próxima aparição de perto prefere aquele túnel.
 *
 * A pedrinha e os morcegos são perturbações da Atmosfera (entram no sorteio dela no subsolo) e são o que paga a
 * dívida do subsolo (ver Familias). Nada aqui usa o sorteio do mundo: o que varia sai do relógio e do lugar.
 */
final class Caverna {
	/** A etiqueta dos morcegos do mod. Fica salva com eles: é por ela que um que sobrou é achado e tirado. */
	static final String ETIQUETA_MORCEGO = "sussurros_morcego";

	// ----- Números -----
	/** Segundos de descida até a rachadura poder acontecer, e entre uma rachadura e a seguinte. */
	private static final int RACHADURA_ESPERA = 90;
	private static final int RACHADURA_INTERVALO = 600;
	/** Quanto a rachadura fica no bloco (ticks). O cliente a esquece sozinho aos 400 sem notícia. */
	private static final int RACHADURA_DURACAO = 20 * 14;
	static final int PEDRINHAS_POR_SESSAO = 2;
	static final int REVOADAS_POR_SESSAO = 3;
	private static final int INTERVALO_FISICO = 20 * 60;
	/** Por quanto tempo o túnel dos morcegos é o lugar preferido da próxima aparição de perto (segundos). */
	private static final int BOCA_VALE = 10 * 60;
	/** Um morcego do mod vive de dez a quinze segundos: depois dos dez sai assim que o jogador não o vê. */
	private static final int MORCEGO_VIDA_MINIMA = 200;
	private static final int MORCEGO_VIDA_MAXIMA = 300;
	/** Por quantos ticks o voo é conduzido (da boca até passar por ele); depois é um morcego qualquer. */
	private static final int MORCEGO_CONDUZIDO = 70;
	private static final double MORCEGO_VELOCIDADE = 0.38;

	static final class Estado {
		// A rachadura.
		boolean rachouNestaDescida;
		int quebrasNaDescida;
		long rachaduraLiberadaEm;
		@Nullable BlockPos rachaduraPos;
		// A pedrinha.
		int pedrinhas;
		long pedrinhaLiberadaEm;
		// Os morcegos.
		int revoadas;
		long morcegosLiberadosEm;
		@Nullable BlockPos boca;
		long bocaAteSeg;
		/** Só para os testes: a última coisa que aconteceu aqui ("-" se nada). */
		String resumo = "-";
	}

	private static final class Voo {
		final UUID morcego;
		final UUID jogador;
		final Vec3 passagem;
		final Vec3 alem;
		final long nasceuTick;
		/** 0: indo até o jogador; 1: seguindo em frente; 2: solto. */
		int etapa;

		Voo(UUID morcego, UUID jogador, Vec3 passagem, Vec3 alem, long nasceuTick) {
			this.morcego = morcego;
			this.jogador = jogador;
			this.passagem = passagem;
			this.alem = alem;
			this.nasceuTick = nasceuTick;
		}
	}

	private static final List<Voo> VOOS = new ArrayList<>();
	/** Os morcegos que o mod soltou nesta execução. Um etiquetado que não está aqui sobrou de outra. */
	private static final Set<UUID> NOSSOS = new HashSet<>();
	private static final List<Entity> SOBRAS = new ArrayList<>();

	private Caverna() {
	}

	static void inicializar() {
		// Um morcego do mod que foi salvo com o mundo (o jogo caiu, ou o chunk descarregou com ele dentro) volta
		// com a etiqueta e sem ninguém cuidando dele. É tirado no tick seguinte ao de ser carregado.
		ServerEntityEvents.ENTITY_LOAD.register((entidade, level) -> {
			if (entidade instanceof Bat && entidade.entityTags().contains(ETIQUETA_MORCEGO) && !NOSSOS.contains(entidade.getUUID())) {
				SOBRAS.add(entidade);
			}
		});
	}

	/** Todo tick do mundo normal: conduz os morcegos, tira os que venceram e os que sobraram. */
	static void tick(ServerLevel level, long tick) {
		if (!SOBRAS.isEmpty()) {
			for (Entity sobra : SOBRAS) {
				if (!sobra.isRemoved()) {
					sobra.discard();
				}
			}
			SOBRAS.clear();
		}
		if (VOOS.isEmpty()) {
			return;
		}
		Iterator<Voo> it = VOOS.iterator();
		while (it.hasNext()) {
			Voo v = it.next();
			if (!(level.getEntity(v.morcego) instanceof Bat morcego) || !morcego.isAlive()) {
				NOSSOS.remove(v.morcego);
				it.remove();
				continue;
			}
			long idade = tick - v.nasceuTick;
			if (idade >= MORCEGO_VIDA_MAXIMA || (idade >= MORCEGO_VIDA_MINIMA && !aVistaDe(level, v.jogador, morcego))) {
				morcego.discard();
				NOSSOS.remove(v.morcego);
				it.remove();
				continue;
			}
			if (idade > MORCEGO_CONDUZIDO) {
				continue;
			}
			// O morcego do jogo voa para um ponto sorteado à sua volta, corrigindo um décimo por tick. Aqui a
			// velocidade é reposta a cada tick: ele vai da boca até o jogador, passa, e segue em frente.
			if (v.etapa >= 2) {
				continue;
			}
			// Debaixo de um teto o morcego do jogo resolve pousar (uma chance em cem por tick): num túnel de dois
			// blocos, um em cada quatro ficaria pendurado no meio do caminho.
			if (morcego.isResting()) {
				morcego.setResting(false);
			}
			Vec3 falta = (v.etapa == 0 ? v.passagem : v.alem).subtract(morcego.position());
			if (falta.lengthSqr() < 1.6 * 1.6) {
				v.etapa++; // passou por ele; ou chegou ao fim do caminho, e daqui em diante é um morcego qualquer
				if (v.etapa >= 2) {
					continue;
				}
				falta = v.alem.subtract(morcego.position());
			}
			if (falta.lengthSqr() > 1.0E-4) {
				morcego.setDeltaMovement(falta.normalize().scale(MORCEGO_VELOCIDADE));
			}
		}
	}

	/** Antes de o mundo ser salvo: nenhum morcego do mod fica gravado. */
	static void aoParar(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (Voo v : VOOS) {
				Entity morcego = level.getEntity(v.morcego);
				if (morcego != null && !morcego.isRemoved()) {
					morcego.discard();
				}
			}
		}
		limpar();
	}

	static void limpar() {
		VOOS.clear();
		NOSSOS.clear();
		SOBRAS.clear();
	}

	/** Só para os testes: quantos morcegos do mod estão voando agora. */
	static int morcegosVoando() {
		return VOOS.size();
	}

	/** Ele saiu de debaixo da terra: a próxima descida é outra. */
	static void aoSairDoSubsolo(EstadoJogador e) {
		e.caverna.rachouNestaDescida = false;
		e.caverna.quebrasNaDescida = 0;
	}

	// =====================================================================
	// A rachadura
	// =====================================================================

	/** Chamado quando o jogador quebra um bloco (depois de o bloco sair do mundo). */
	static void aoQuebrar(ServerLevel level, ServerPlayer p, EstadoJogador e, BlockPos quebrado) {
		Estado c = e.caverna;
		long tick = level.getGameTime();
		long seg = tick / 20;
		if (quebrado.equals(c.rachaduraPos)) {
			// Ele foi ver: pedra, e nada atrás. A marca sai com o bloco.
			apagarRachadura(p, c);
			Depuracao.log(p, seg, "RACHADURA fim por=QUEBROU_O_BLOCO");
			return;
		}
		if (!podeRachar(level, p, e, tick, seg)) {
			return;
		}
		c.quebrasNaDescida++;
		if (c.quebrasNaDescida < quebrasAteRachar(e.subsoloDesde)) {
			return;
		}
		Direction lado = ladoParaRachar(level, p, quebrado);
		if (lado == null) {
			return; // nenhum vizinho serve (minério, cascalho, bloco dele): fica para a próxima quebra
		}
		rachar(level, p, e, quebrado.relative(lado), lado, seg, false);
	}

	/** Só para o comando de teste e para as fotos: racha o bloco para onde o jogador olha, sem contar para nada. */
	static boolean racharOlhado(ServerLevel level, ServerPlayer p, EstadoJogador e) {
		if (!(p.pick(6.0, 1.0F, false) instanceof BlockHitResult mira) || mira.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		if (e.caverna.rachaduraPos != null) {
			apagarRachadura(p, e.caverna);
		}
		rachar(level, p, e, mira.getBlockPos(), mira.getDirection().getOpposite(), level.getGameTime() / 20, true);
		return true;
	}

	/**
	 * Em que quebra da descida a rachadura vem: da quarta à oitava das que contam. Sai do segundo em que a
	 * descida começou, não de um sorteio: é sempre depois de ele pegar ritmo, e nunca na mesma conta.
	 */
	static int quebrasAteRachar(long subsoloDesde) {
		return 4 + (int) Math.floorMod(subsoloDesde, 5L);
	}

	private static boolean podeRachar(ServerLevel level, ServerPlayer p, EstadoJogador e, long tick, long seg) {
		Estado c = e.caverna;
		if (e.contexto != ContextoMundo.Tipo.SUBSOLO || e.subsoloDesde < 0 || seg - e.subsoloDesde < RACHADURA_ESPERA) {
			return false;
		}
		if (c.rachouNestaDescida || seg < c.rachaduraLiberadaEm) {
			return false;
		}
		// O que cala os outros sistemas cala este: a trégua de depois de um pico, uma cena, uma sequência de
		// ameaça, o respiro de algo que acabou de sair, a criatura por perto, a trégua comprada, o Véu.
		if (e.forcando || e.estado == EstadoDiretor.RECUANDO || e.estado == EstadoDiretor.AMEACANDO || seg < e.atencaoLivreEm) {
			return false;
		}
		if ((e.criatura != null && !e.criatura.isRemoved()) || !"-".equals(Diretor.cenaAtiva(e))) {
			return false;
		}
		if (tick < e.treguaAte || tick < e.veuAte || Diretor.bloqueado(p, e, tick)) {
			return false;
		}
		// Ele está minerando, e não quebrando um bloco só: três quebras em meio minuto, com esta.
		int recentes = 0;
		for (long t : e.quebrasRecentes) {
			if (tick - t <= 600) {
				recentes++;
			}
		}
		// As batidas são da família das que o mod só faz da fase 2 em diante.
		return recentes >= 3 && Memoria.de(p).get(Memoria.FASE) >= 2;
	}

	/**
	 * Qual vizinho do bloco quebrado racha: pedra natural, com a face que ficou à mostra virada para ele.
	 * De preferência um dos lados (o bloco de trás é o próximo que ele vai quebrar), e o mais no meio da tela.
	 */
	@Nullable
	static Direction ladoParaRachar(ServerLevel level, ServerPlayer p, BlockPos quebrado) {
		Vec3 olho = p.getEyePosition();
		Vec3 olhar = p.getViewVector(1.0F);
		Vec3 centro = Vec3.atCenterOf(quebrado);
		Direction melhor = null;
		double melhorNota = Double.NEGATIVE_INFINITY;
		for (Direction lado : Direction.values()) {
			BlockPos vizinho = quebrado.relative(lado);
			// Pedra natural e mais nada: um bloco que ele pôs, um baú, um minério (que ele quer) ficam de fora.
			if (!level.getBlockState(vizinho).is(BlockTags.BASE_STONE_OVERWORLD)
					|| Diretor.emZonaCalma(p, vizinho.getX() + 0.5, vizinho.getY() + 0.5, vizinho.getZ() + 0.5)) {
				continue;
			}
			Vec3 passo = new Vec3(lado.getStepX(), lado.getStepY(), lado.getStepZ());
			Vec3 face = centro.add(passo.scale(0.5));
			Vec3 ate = face.subtract(olho);
			// A face nova olha para dentro do buraco: ele só a vê se estiver do lado de cá dela.
			if (ate.dot(passo) <= 0.05 || !Aparicao.linhaLivre(level, olho, face.subtract(passo.scale(0.1)))) {
				continue;
			}
			double nota = olhar.dot(ate.normalize());
			if (Math.abs(olhar.dot(passo)) < 0.7) {
				nota += 2.0; // ao lado, e não atrás
			}
			if (lado.getAxis().isHorizontal()) {
				nota += 0.5;
			}
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = lado;
			}
		}
		return melhor;
	}

	private static void rachar(ServerLevel level, ServerPlayer p, EstadoJogador e, BlockPos pos, Direction lado, long seg,
			boolean teste) {
		Estado c = e.caverna;
		BlockPos alvo = pos.immutable();
		if (!teste) {
			c.rachouNestaDescida = true;
			c.rachaduraLiberadaEm = seg + RACHADURA_INTERVALO;
		}
		c.rachaduraPos = alvo;
		c.resumo = "RACHADURA";
		BlockState bloco = level.getBlockState(alvo);
		double x = alvo.getX() + 0.5;
		double y = alvo.getY() + 0.5;
		double z = alvo.getZ() + 0.5;
		// Duas batidas, de dentro da pedra. A cada uma a rachadura anda; na segunda ela chega à metade e para.
		int[] quando = {0, 3, 6, 16, 19};
		for (int i = 0; i < quando.length; i++) {
			int etapa = i;
			Diretor.agendar(level, quando[i], () -> mandarRachadura(level, p, c, alvo, bloco, etapa));
		}
		ModSons.tocarPara(p, x, y, z, ModSons.Som.PANCADA, 0.6F, 0.74F);
		Diretor.agendar(level, 16, () -> {
			if (!p.isRemoved() && p.level() == level) {
				ModSons.tocarPara(p, x, y, z, ModSons.Som.PANCADA, 0.6F, 0.68F);
			}
		});
		Diretor.agendar(level, RACHADURA_DURACAO, () -> {
			if (alvo.equals(c.rachaduraPos)) {
				apagarRachadura(p, c);
				Depuracao.log(p, level.getGameTime() / 20, "RACHADURA fim por=TEMPO");
			}
		});
		Depuracao.log(p, seg, String.format(Locale.ROOT, "RACHADURA pos=%s lado=%s bloco=%s descida=%ds quebra=%d teste=%s",
				Diretor.pos(x, y, z), lado, bloco.getBlock().getName().getString(), e.subsoloDesde < 0 ? -1 : seg - e.subsoloDesde,
				c.quebrasNaDescida, teste ? "sim" : "nao"));
		if (!teste) {
			Atencao.respirar(e, seg);
			Familias.registrar(p, e, Familias.Familia.MARCA, "RACHADURA", seg);
		}
	}

	private static void mandarRachadura(ServerLevel level, ServerPlayer p, Estado c, BlockPos pos, BlockState bloco, int etapa) {
		if (p.isRemoved() || p.hasDisconnected() || p.level() != level || !pos.equals(c.rachaduraPos)
				|| !level.getBlockState(pos).equals(bloco)) {
			return;
		}
		p.connection.send(new ClientboundBlockDestructionPacket(idDaRachadura(p), pos, etapa));
	}

	private static void apagarRachadura(ServerPlayer p, Estado c) {
		BlockPos pos = c.rachaduraPos;
		c.rachaduraPos = null;
		if (pos != null && !p.isRemoved() && !p.hasDisconnected()) {
			// Qualquer valor fora de 0 a 9 apaga.
			p.connection.send(new ClientboundBlockDestructionPacket(idDaRachadura(p), pos, -1));
		}
	}

	/**
	 * O número com que o cliente guarda esta rachadura. O jogo usa o número da entidade que está quebrando; os de
	 * entidade são sempre positivos, então um negativo nunca se confunde com a quebra de ninguém (nem a dele).
	 */
	private static int idDaRachadura(ServerPlayer p) {
		return -1000 - p.getId();
	}

	// =====================================================================
	// A pedrinha
	// =====================================================================

	private record Queda(Vec3 longe, Vec3 perto, BlockState chao, double graus, int corredor, int luz) {
	}

	/** Os lados de onde ela pode vir, a partir de para onde ele olha. Primeiro os que ele vê. */
	private static final double[] LADOS_DA_PEDRINHA = {30, -30, 60, -60, 0, 100, -100, 150, -150, 180};

	static boolean podePedrinha(EstadoJogador e, long seg) {
		return e.contexto == ContextoMundo.Tipo.SUBSOLO && e.caverna.pedrinhas < PEDRINHAS_POR_SESSAO
				&& seg >= e.caverna.pedrinhaLiberadaEm;
	}

	/** Alguma coisa pequena vem do escuro e quica até perto dele. "teste": por comando, sem cota e em qualquer lugar. */
	static boolean pedrinha(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, boolean teste) {
		Estado c = e.caverna;
		if (!teste && !podePedrinha(e, seg)) {
			return false;
		}
		Queda q = escolherQueda(level, p);
		if (q == null) {
			return false;
		}
		if (!teste) {
			c.pedrinhas++;
			c.pedrinhaLiberadaEm = seg + INTERVALO_FISICO;
		}
		c.resumo = "PEDRINHA";
		// Três toques: longe, perto, e um último resto de rolar. Cada um mais agudo e mais fraco que o anterior.
		Vec3 resto = q.perto().add(p.position().subtract(q.perto()).multiply(1, 0, 1).normalize().scale(0.35));
		quicar(level, p, q.longe(), q.chao(), 0, 0.5F, 1.5F, 4);
		quicar(level, p, q.perto(), q.chao(), 5, 0.42F, 1.75F, 5);
		quicar(level, p, resto, q.chao(), 9, 0.3F, 1.95F, 2);
		Depuracao.log(p, seg, String.format(Locale.ROOT, "PEDRINHA de=%s para=%s lado=%.0f corredor=%d luzNoFim=%d teste=%s",
				Diretor.pos(q.longe().x, q.longe().y, q.longe().z), Diretor.pos(q.perto().x, q.perto().y, q.perto().z),
				q.graus(), q.corredor(), q.luz(), teste ? "sim" : "nao"));
		return true;
	}

	/** Um toque no chão: lascas do bloco do lugar e um estalo seco, só para ele. */
	private static void quicar(ServerLevel level, ServerPlayer p, Vec3 onde, BlockState chao, int atraso, float volume, float tom, int lascas) {
		Diretor.agendar(level, atraso, () -> {
			if (p.isRemoved() || p.level() != level) {
				return;
			}
			level.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, chao), false, false,
					onde.x, onde.y + 0.05, onde.z, lascas, 0.06, 0.02, 0.06, 0.04);
			ModSons.tocarPara(p, onde.x, onde.y, onde.z, ModSons.Som.ESTALO, volume, tom);
		});
	}

	/**
	 * De que lado ela vem e onde cai: a 1,7 bloco dele, depois de quicar a 3,4. Entre os lados em que há chão e
	 * nada no caminho, fica com o que continua mais fundo e termina mais escuro ("vinda do escuro"), de
	 * preferência à frente dele, onde ele vê as lascas.
	 */
	@Nullable
	private static Queda escolherQueda(ServerLevel level, ServerPlayer p) {
		Vec3 olho = p.getEyePosition();
		Queda melhor = null;
		double melhorNota = Double.NEGATIVE_INFINITY;
		for (double graus : LADOS_DA_PEDRINHA) {
			BlockPos perto = piso(level, Diretor.pontoRelativo(p, graus, 1.7), p.getY());
			BlockPos longe = perto == null ? null : piso(level, Diretor.pontoRelativo(p, graus, 3.4), perto.getY());
			if (longe == null || Diretor.emZonaCalma(p, perto.getX() + 0.5, perto.getY(), perto.getZ() + 0.5)
					|| !Aparicao.linhaLivre(level, olho, Vec3.atBottomCenterOf(longe).add(0, 0.4, 0))) {
				continue;
			}
			// Até onde o espaço continua naquela direção, e quanta luz há no fim.
			int corredor = 0;
			BlockPos fim = longe;
			for (int d = 5; d <= 12; d++) {
				BlockPos celula = piso(level, Diretor.pontoRelativo(p, graus, d), fim.getY());
				if (celula == null || !Aparicao.linhaLivre(level, olho, Vec3.atBottomCenterOf(celula).add(0, 0.6, 0))) {
					break;
				}
				corredor++;
				fim = celula;
			}
			int luz = level.getBrightness(LightLayer.BLOCK, fim);
			double lado = Math.abs(graus);
			double nota = corredor + (15 - luz) * 0.4 + (lado <= 60 ? 6.0 : lado <= 100 ? 2.0 : 0.0);
			if (nota > melhorNota) {
				melhorNota = nota;
				BlockPos sob = perto.below();
				melhor = new Queda(Vec3.atBottomCenterOf(longe), Vec3.atBottomCenterOf(perto), level.getBlockState(sob), graus, corredor, luz);
			}
		}
		return melhor;
	}

	/** O bloco livre com chão firme embaixo, naquele ponto, na altura dada ou um degrau acima ou abaixo. */
	@Nullable
	private static BlockPos piso(ServerLevel level, Vec3 ponto, double y) {
		for (int dy : new int[] {0, -1, 1}) {
			BlockPos pos = BlockPos.containing(ponto.x, y + dy, ponto.z);
			BlockPos baixo = pos.below();
			if (Diretor.livre(level, pos) && level.getBlockState(baixo).isCollisionShapeFullBlock(level, baixo)) {
				return pos;
			}
		}
		return null;
	}

	// =====================================================================
	// Os morcegos
	// =====================================================================

	static boolean podeMorcegos(EstadoJogador e, long seg) {
		return e.contexto == ContextoMundo.Tipo.SUBSOLO && e.caverna.revoadas < REVOADAS_POR_SESSAO
				&& seg >= e.caverna.morcegosLiberadosEm;
	}

	/** Quantos saem: de três a cinco. Do relógio, não de um sorteio. */
	static int quantosMorcegos(long seg) {
		return 3 + (int) Math.floorMod(seg, 3L);
	}

	/**
	 * Um baque numa boca de túnel fora da tela, e os morcegos saem de lá e passam por ele. "teste": por comando,
	 * sem cota e em qualquer lugar fechado.
	 */
	static boolean morcegos(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick, boolean teste) {
		Estado c = e.caverna;
		if (!teste && !podeMorcegos(e, seg)) {
			return false;
		}
		// A boca é o lugar que a procura de perto acharia para ele: fora da tela, de 8 a 14 blocos, num lugar
		// fechado, e com linha livre até os olhos do jogador (é por ela que os morcegos vêm).
		Aparicao.DePerto achado = Aparicao.bocaDeTunel(level, p, e, seg);
		if (achado == null) {
			return false;
		}
		BlockPos boca = achado.chao().immutable();
		int quantos = quantosMorcegos(seg);
		if (!teste) {
			c.revoadas++;
			c.morcegosLiberadosEm = seg + INTERVALO_FISICO;
		}
		c.boca = boca;
		c.bocaAteSeg = seg + BOCA_VALE;
		c.resumo = "MORCEGOS";
		double bx = boca.getX() + 0.5;
		double by = boca.getY() + 0.9;
		double bz = boca.getZ() + 0.5;
		// O que os espantou: um baque surdo lá dentro, só para ele. Meio segundo depois eles saem.
		ModSons.tocarPara(p, bx, by, bz, ModSons.Som.GRAVE, Diretor.volumePara(p, bx, by, bz, 0.4F), 0.9F);
		Diretor.agendar(level, 10, () -> soltar(level, p, boca, quantos));
		Depuracao.log(p, seg, String.format(Locale.ROOT, "MORCEGOS qtd=%d boca=%s dist=%.1f luz=%s teste=%s",
				quantos, Diretor.pos(bx, boca.getY(), bz), achado.distancia(), achado.luz() ? "sim" : "nao", teste ? "sim" : "nao"));
		return true;
	}

	private static void soltar(ServerLevel level, ServerPlayer p, BlockPos boca, int quantos) {
		if (p.isRemoved() || p.level() != level) {
			return;
		}
		Vec3 origem = new Vec3(boca.getX() + 0.5, boca.getY() + 0.6, boca.getZ() + 0.5);
		Vec3 cabeca = p.getEyePosition().add(0, 0.35, 0);
		Vec3 rumo = cabeca.subtract(origem).multiply(1, 0, 1);
		rumo = rumo.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : rumo.normalize();
		Vec3 deLado = new Vec3(-rumo.z, 0, rumo.x);
		long tick = level.getGameTime();
		// O som de morcego levantando voo é o do jogo, e é do mundo: os morcegos estão lá de verdade.
		level.levelEvent(null, 1025, boca, 0);
		for (int i = 0; i < quantos; i++) {
			Bat morcego = EntityTypes.BAT.create(level, EntitySpawnReason.EVENT);
			if (morcego == null) {
				continue;
			}
			double espalha = (i - (quantos - 1) / 2.0) * 0.55;
			morcego.setResting(false);
			morcego.addTag(ETIQUETA_MORCEGO);
			morcego.snapTo(origem.x + deLado.x * espalha * 0.4, origem.y + (i % 2) * 0.35, origem.z + deLado.z * espalha * 0.4,
					(float) (Math.toDegrees(Math.atan2(rumo.z, rumo.x)) - 90.0), 0.0F);
			morcego.setDeltaMovement(rumo.scale(MORCEGO_VELOCIDADE));
			// Cada um passa a um palmo de um lado ou do outro da cabeça dele, e segue oito blocos em frente.
			Vec3 passagem = cabeca.add(deLado.scale(espalha)).add(0, (i % 2) * 0.3, 0);
			Vec3 alem = passagem.add(rumo.scale(8.0));
			NOSSOS.add(morcego.getUUID());
			if (level.addFreshEntity(morcego)) {
				VOOS.add(new Voo(morcego.getUUID(), p.getUUID(), passagem, alem, tick));
			} else {
				NOSSOS.remove(morcego.getUUID());
			}
		}
	}

	private static boolean aVistaDe(ServerLevel level, UUID jogador, Bat morcego) {
		ServerPlayer p = level.getServer().getPlayerList().getPlayer(jogador);
		return p != null && p.level() == level && p.distanceToSqr(morcego) < 24 * 24
				&& Diretor.estaOlhando(p, morcego, Percepcao.coneSeguro(p));
	}

	/** O túnel de onde os morcegos saíram, enquanto valer como lugar preferido da próxima aparição de perto. */
	@Nullable
	static BlockPos bocaValida(EstadoJogador e, long seg) {
		return e.caverna.boca != null && seg <= e.caverna.bocaAteSeg ? e.caverna.boca : null;
	}
}
