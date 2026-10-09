package com.sussurros.teste;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Testes que abrem o jogo de verdade, entram num mundo e tiram fotos (./gradlew runClientGameTest).
 *
 * Rodam no GitHub, numa tela virtual. Não rode na máquina do dono com ele por perto: abre uma janela
 * do jogo e mostra exatamente o que ele não quer ver antes da hora. As fotos ficam em
 * build/run/clientGameTest/screenshots e sobem como artefato da execução.
 */
public class TestesDeCliente implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext mundo = context.worldBuilder().create()) {
			mundo.getClientLevel().waitForChunksRender();
			mundo.getServer().runCommand("time set noon");
			context.waitTicks(20);
			context.takeScreenshot("01-mundo");

			// O modelo da criatura, de perto e de dia: só para conferir que desenha.
			mundo.getServer().runCommand("execute as @p at @s run summon sussurros:hospede ^ ^ ^6");
			context.waitTicks(8);
			context.takeScreenshot("02-criatura-de-perto");
		}
	}
}
