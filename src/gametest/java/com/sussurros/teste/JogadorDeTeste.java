package com.sussurros.teste;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
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

	/** O primeiro bloco de ar acima do chão, numa coluna relativa à origem da estrutura do teste. */
	public static BlockPos chao(GameTestHelper helper, int dx, int dz) {
		BlockPos coluna = helper.absolutePos(new BlockPos(dx, 0, dz));
		int y = helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, coluna.getX(), coluna.getZ());
		return new BlockPos(coluna.getX(), y, coluna.getZ());
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
		new EmbeddedChannel(conexao);
		level.getServer().getPlayerList().placeNewPlayer(conexao, jogador, cookie);
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
	}
}
