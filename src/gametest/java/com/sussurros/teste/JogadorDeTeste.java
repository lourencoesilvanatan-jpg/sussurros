package com.sussurros.teste;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Diretor;
import com.sussurros.entidade.HospedeEntity;

/**
 * Um jogador de mentira, mas ligado ao servidor de verdade: entra na lista de jogadores, recebe pacotes
 * (que vão para lugar nenhum) e é visto pelo Diretor como qualquer outro. Cada teste cria o seu e o tira no fim.
 */
public final class JogadorDeTeste {
	private static int contador;

	private JogadorDeTeste() {
	}

	/**
	 * Cria o jogador em pé no chão de verdade do mundo de teste, fora da estrutura do teste (que fica uns
	 * blocos acima do chão e tem piso próprio: quem fica dentro dela está flutuando e com a visão tampada).
	 * dx e dz são relativos à origem da estrutura; alturaExtra serve para pô-lo em cima de algo.
	 */
	public static ServerPlayer criarNoChao(GameTestHelper helper, GameType modo, int dx, int dz, int alturaExtra) {
		BlockPos base = chao(helper, dx, dz);
		Vec3 relativo = helper.relativeVec(new Vec3(base.getX() + 0.5, base.getY() + alturaExtra, base.getZ() + 0.5));
		return criar(helper, modo, relativo.x, relativo.y, relativo.z);
	}

	/**
	 * A origem de todas as colunas: a estrutura do primeiro teste que pediu um chão. Uma só para a execução
	 * inteira, e não a de cada teste, porque o jogo espalha as estruturas numa grade (15 blocos de um teste ao
	 * seguinte, 17 de uma fileira à outra) e com sessenta testes a grade tem mais de cem blocos de fundo: contado
	 * a partir de cada estrutura, um teste em dz=-5000 ficava a quinze blocos de outro em dz=-5100, e a criatura
	 * de um apagava o lampião do outro.
	 */
	private static BlockPos origemDasColunas;

	/**
	 * O primeiro bloco de ar acima do chão, numa coluna contada a partir da origem comum (ver acima). Dois
	 * testes com dz diferentes em cem ficam a cem blocos um do outro, estejam onde estiverem na grade.
	 */
	public static BlockPos chao(GameTestHelper helper, int dx, int dz) {
		if (origemDasColunas == null) {
			origemDasColunas = helper.absolutePos(BlockPos.ZERO);
		}
		BlockPos coluna = origemDasColunas.offset(dx, 0, dz);
		// Carrega o chunk antes de perguntar a altura: num chunk ainda não carregado a resposta é o fundo do
		// mundo, e o jogador nasceria dentro da rocha-mãe (acontecia às vezes, longe da estrutura do teste).
		helper.getLevel().getChunk(coluna);
		int y = helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, coluna.getX(), coluna.getZ());
		return new BlockPos(coluna.getX(), y, coluna.getZ());
	}

	/** O canal de mentira de cada jogador: tudo o que o servidor manda para ele fica guardado ali. */
	private static final Map<UUID, EmbeddedChannel> CANAIS = new HashMap<>();
	/** O chunk central do pedaço de mundo forçado para cada jogador de mentira (para soltar quando ele sai). */
	private static final Map<UUID, int[]> FORCADOS = new HashMap<>();

	/**
	 * Quantos sons o servidor mandou este jogador ouvir desde a última vez que isto foi chamado.
	 * É o único jeito de um teste saber quem "ouviu" o quê: o jogador de mentira não tem alto-falante, mas
	 * os pacotes que chegariam ao jogo dele ficam na fila.
	 */
	public static int sonsRecebidos(ServerPlayer jogador) {
		EmbeddedChannel canal = CANAIS.get(jogador.getUUID());
		int sons = 0;
		Object pacote;
		while (canal != null && (pacote = canal.readOutbound()) != null) {
			if (pacote instanceof ClientboundSoundPacket || pacote instanceof ClientboundSoundEntityPacket) {
				sons++;
			}
		}
		return sons;
	}

	/**
	 * O mesmo, separando o que chegou: [0] sons, [1] partículas, [2] blocos trocados (é assim que uma miragem chega)
	 * [3] rachaduras (o progresso de quebra de um bloco; quem quebra de verdade não recebe o seu) e [4] eventos
	 * de bloco (a tampa de um baú abrindo ou fechando).
	 * Esvazia a fila, como sonsRecebidos: quem quer o total de um trecho soma as chamadas.
	 */
	public static int[] recebidos(ServerPlayer jogador) {
		EmbeddedChannel canal = CANAIS.get(jogador.getUUID());
		int[] n = new int[5];
		Object pacote;
		while (canal != null && (pacote = canal.readOutbound()) != null) {
			if (pacote instanceof ClientboundSoundPacket || pacote instanceof ClientboundSoundEntityPacket) {
				n[0]++;
				// Que som foi, e de onde: um jogador de mentira também ouve o mundo (um bicho andando ali perto), e
				// só pelo lugar dá para saber se um som era o que o teste procura.
				if (pacote instanceof ClientboundSoundPacket som) {
					java.util.ArrayDeque<SomOuvido> fila = ULTIMOS_SONS.computeIfAbsent(jogador.getUUID(), k -> new java.util.ArrayDeque<>());
					fila.addLast(new SomOuvido(som.getSound().getRegisteredName(), som.getX(), som.getY(), som.getZ()));
					while (fila.size() > 64) {
						fila.removeFirst();
					}
				}
			} else if (pacote instanceof ClientboundLevelParticlesPacket) {
				n[1]++;
			} else if (pacote instanceof ClientboundBlockUpdatePacket) {
				n[2]++;
			} else if (pacote instanceof ClientboundBlockDestructionPacket rachadura && rachadura.getProgress() >= 0
					&& rachadura.getProgress() <= 9) {
				n[3]++;
			} else if (pacote instanceof ClientboundBlockEventPacket) {
				n[4]++;
			}
		}
		return n;
	}

	/** Um som que chegou a um jogador de mentira: qual, e de que ponto do mundo. */
	public record SomOuvido(String nome, double x, double y, double z) {
		@Override
		public String toString() {
			return String.format(java.util.Locale.ROOT, "%s@(%.1f,%.1f,%.1f)", this.nome, this.x, this.y, this.z);
		}
	}

	private static final Map<UUID, java.util.ArrayDeque<SomOuvido>> ULTIMOS_SONS = new HashMap<>();

	/**
	 * Entre os últimos sons que "recebidos" contou para este jogador (até 64), quantos vieram de até "raio"
	 * blocos deste ponto. É como um teste pergunta "ele ouviu AQUELE som?" sem se confundir com os sons do mundo.
	 */
	public static int sonsVindosDe(ServerPlayer jogador, Vec3 ponto, double raio) {
		int n = 0;
		for (SomOuvido s : ULTIMOS_SONS.getOrDefault(jogador.getUUID(), new java.util.ArrayDeque<>())) {
			if (ponto.distanceToSqr(s.x(), s.y(), s.z()) <= raio * raio) {
				n++;
			}
		}
		return n;
	}

	/**
	 * O mesmo, contando só o som de nome "nome" (por exemplo "sussurros:estalo"). Para quando no mesmo lugar há
	 * coisa de verdade fazendo barulho: um morcego do mundo guincha para todos, e o estalo do mod é só do alvo.
	 */
	public static int sonsVindosDe(ServerPlayer jogador, Vec3 ponto, double raio, String nome) {
		int n = 0;
		for (SomOuvido s : ULTIMOS_SONS.getOrDefault(jogador.getUUID(), new java.util.ArrayDeque<>())) {
			if (s.nome().equals(nome) && ponto.distanceToSqr(s.x(), s.y(), s.z()) <= raio * raio) {
				n++;
			}
		}
		return n;
	}

	/**
	 * Entre os últimos sons que "recebidos" contou para este jogador, quantos NÃO são de bicho (os do jogo cujo
	 * nome começa por "entity."). No gramado do mundo de teste há cavalos e vacas andando, e o som deles chega a
	 * qualquer jogador de mentira: quem pergunta "chegou a ele algum som da assombração do outro?" não pode
	 * contar o passo de um cavalo. Use depois de esquecerSons, para a conta começar do zero.
	 */
	public static int sonsQueNaoSaoDeBicho(ServerPlayer jogador) {
		int n = 0;
		for (SomOuvido s : ULTIMOS_SONS.getOrDefault(jogador.getUUID(), new java.util.ArrayDeque<>())) {
			if (!s.nome().startsWith("minecraft:entity.")) {
				n++;
			}
		}
		return n;
	}

	/** Esvazia a fila do que chegou a este jogador e esquece os sons guardados: o que vier depois é de agora. */
	public static void esquecerSons(ServerPlayer jogador) {
		recebidos(jogador);
		ULTIMOS_SONS.remove(jogador.getUUID());
	}

	/** Os mesmos sons, por extenso, para a mensagem de um teste que falha. */
	public static String ultimosSons(ServerPlayer jogador) {
		return String.valueOf(ULTIMOS_SONS.getOrDefault(jogador.getUUID(), new java.util.ArrayDeque<>()));
	}

	public static ServerPlayer criar(GameTestHelper helper, GameType modo, double x, double y, double z) {
		ServerLevel level = helper.getLevel();
		// O log de decisões fica ligado nos testes: quando um falha, é nele que está o porquê
		// (build/run/gameTest/sussurros-debug.log).
		Diretor.alternarDepuracao(true);
		// Sem monstros do jogo: de noite eles matam o jogador de mentira antes de o teste terminar.
		// O Hóspede não é "monstro" para o jogo e continua existindo no pacífico.
		if (level.getServer().getWorldData().getDifficulty() != Difficulty.PEACEFUL) {
			level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
		}
		String nome = "Teste" + (++contador);
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), nome), false);
		ServerPlayer jogador = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return modo;
			}
		};
		Connection conexao = new Connection(PacketFlow.SERVERBOUND);
		CANAIS.put(cookie.gameProfile().id(), new EmbeddedChannel(conexao));
		level.getServer().getPlayerList().placeNewPlayer(conexao, jogador, cookie);
		// O modo de jogo de verdade, não só o que a criatura consulta: em criativo os itens não são gastos.
		jogador.setGameMode(modo);
		modo.updatePlayerAbilities(jogador.getAbilities());
		Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
		jogador.teleportTo(level, pos.x, pos.y, pos.z, java.util.Set.of(), 0.0F, 0.0F, false);
		// O mundo de teste só mantém vivo o pedaço onde fica a estrutura do teste. Um jogador de verdade
		// carrega o mundo em volta de si; para o de mentira, forçamos uns 70 blocos para cada lado.
		int cx = ((int) Math.floor(pos.x)) >> 4;
		int cz = ((int) Math.floor(pos.z)) >> 4;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				level.setChunkForced(cx + dx, cz + dz, true);
			}
		}
		FORCADOS.put(jogador.getUUID(), new int[] {cx, cz});
		return jogador;
	}

/**
	 * Um jogador de verdade avisa o servidor a cada passo, e é isso que mantém carregado o mundo em volta dele.
	 * O de mentira não avisa nada; sem isto, o que nasce a vinte blocos fica num pedaço do mundo parado no tempo.
	 * Também escreve no log, a cada vinte segundos, o que está acontecendo com ele.
	 */
	public static void acompanhar(GameTestHelper helper, ServerPlayer jogador, String teste) {
		ServerLevel level = helper.getLevel();
		int[] relogio = new int[1];
		helper.onEachTick(() -> {
			if (jogador.isRemoved()) {
				return;
			}
			level.getChunkSource().move(jogador);
			if (relogio[0]++ % 400 == 0) {
				HospedeEntity h = Diretor.criatura(jogador);
				Sussurros.LOGGER.info("[{}] t={} jogador=({},{},{}) vida={} criatura={}", teste, relogio[0] - 1,
						(int) jogador.getX(), (int) jogador.getY(), (int) jogador.getZ(), jogador.getHealth(),
						h == null ? "nenhuma" : h.getModo() + "/" + h.getEstagioDaCaca() + " dist="
								+ String.format("%.1f", h.distanceTo(jogador)) + " pos=(" + (int) h.getX() + "," + (int) h.getY() + "," + (int) h.getZ() + ")");
			}
		});
	}

	public static void remover(ServerPlayer jogador) {
		if (jogador.level().getServer().getPlayerList().getPlayers().contains(jogador)) {
			jogador.level().getServer().getPlayerList().remove(jogador);
		}
		// Solta o pedaço de mundo que foi forçado para ele. Sem isto cada teste deixava 81 chunks vivos até o
		// fim da execução, com os bichos e tudo, e a sessão longa (TestesDeSessao) carregava o peso de todos.
		int[] centro = FORCADOS.remove(jogador.getUUID());
		if (centro != null && jogador.level() instanceof ServerLevel level) {
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					int cx = centro[0] + dx;
					int cz = centro[1] + dz;
					// Os testes ficam a cem blocos uns dos outros e os pedaços forçados se sobrepõem: não solta o
					// chunk que ainda é de outro jogador de mentira.
					boolean deOutro = FORCADOS.values().stream().anyMatch(o -> Math.abs(o[0] - cx) <= 4 && Math.abs(o[1] - cz) <= 4);
					if (!deOutro) {
						level.setChunkForced(cx, cz, false);
					}
				}
			}
		}
	}
}
