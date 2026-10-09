package com.sussurros.teste;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Um jogador de mentira, mas ligado ao servidor de verdade: entra na lista de jogadores, recebe pacotes
 * (que vão para lugar nenhum) e é visto pelo Diretor como qualquer outro. Cada teste cria o seu e o tira no fim.
 */
public final class JogadorDeTeste {
	private static int contador;

	private JogadorDeTeste() {
	}

	public static ServerPlayer criar(GameTestHelper helper, GameType modo, double x, double y, double z) {
		ServerLevel level = helper.getLevel();
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
		return jogador;
	}

	public static void remover(ServerPlayer jogador) {
		if (jogador.level().getServer().getPlayerList().getPlayers().contains(jogador)) {
			jogador.level().getServer().getPlayerList().remove(jogador);
		}
	}
}
