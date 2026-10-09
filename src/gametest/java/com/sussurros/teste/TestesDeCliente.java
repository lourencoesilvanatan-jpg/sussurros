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
			context.waitTicks(60);

			sentidos(context, mundo);
			cacada(context, mundo);
		}
	}

	/** A caçada vista pelo jogador: o aviso, ele parado ao longe, e o que sobra na tela quando ele chega. */
	private static void cacada(ClientGameTestContext context, TestSingleplayerContext mundo) {
		mundo.getServer().runCommand("difficulty peaceful");
		mundo.getServer().runCommand("time set 18000");
		mundo.getServer().runCommand("execute as @p run sussurros debug on");
		mundo.getServer().runCommand("execute as @p run sussurros fase 4");
		context.waitTicks(40);
		mundo.getServer().runCommand("execute as @p run sussurros evento caca");
		context.waitTicks(20);
		// Vira o jogador para ele.
		mundo.getServer().runCommand("execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] eyes run tp @s ~ ~ ~ ~ ~");
		context.waitTicks(10);
		context.takeScreenshot("30-caca-aviso-de-noite");
		context.waitTicks(260);
		context.takeScreenshot("31-caca-encarando-13s");
		context.waitTicks(200);
		context.takeScreenshot("32-caca-encarando-23s");
		context.waitTicks(300);
		context.takeScreenshot("33-caca-encarando-38s");
		context.waitTicks(300);
		context.takeScreenshot("34-caca-depois");
		context.waitTicks(200);
		context.takeScreenshot("35-caca-acordou");

		// A pose abaixada: de dia, um teto de vidro dois blocos acima dele, visto de lado e de perto.
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		context.waitTicks(20);
		mundo.getServer().runCommand("execute as @p run sussurros evento caca");
		context.waitTicks(10);
		mundo.getServer().runCommand("execute at @e[type=sussurros:hospede,limit=1,sort=nearest] run fill ~-3 ~2 ~-3 ~3 ~2 ~3 minecraft:glass");
		mundo.getServer().runCommand("execute as @p at @e[type=sussurros:hospede,limit=1,sort=nearest] run tp @s ~4 ~ ~1.5");
		mundo.getServer().runCommand("execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] feet run tp @s ~ ~ ~ ~ ~");
		context.waitTicks(15);
		context.takeScreenshot("36-caca-abaixado-de-lado");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		context.waitTicks(20);
	}

	/** Cada sentido no máximo, isolado, para ver o que o jogador veria no pior caso. */
	private static void sentidos(ClientGameTestContext context, TestSingleplayerContext mundo) {
		// Referência: flores e blocos coloridos na frente, para a cor ter o que perder.
		mundo.getServer().runCommand("execute as @p at @s run fill ^-3 ^ ^4 ^3 ^2 ^4 minecraft:red_wool");
		mundo.getServer().runCommand("execute as @p at @s run fill ^-1 ^ ^4 ^1 ^2 ^4 minecraft:yellow_wool");
		mundo.getServer().runCommand("execute as @p at @s run setblock ^ ^1 ^4 minecraft:blue_wool");
		context.waitTicks(10);
		context.takeScreenshot("10-cor-normal");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 1 0 0 0");
		context.waitTicks(320);
		context.takeScreenshot("11-cor-drenada-no-maximo");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0.5 0 0 0");
		context.waitTicks(200);
		context.takeScreenshot("12-cor-drenada-na-metade");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 1 0 0");
		context.waitTicks(200);
		context.takeScreenshot("13-vigia-no-maximo");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 0 1 0");
		context.waitTicks(160);
		context.takeScreenshot("14-caca-no-maximo");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 0 0 1");
		context.waitTicks(220);
		context.takeScreenshot("15-neblina-no-maximo");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 0 0 0.5");
		context.waitTicks(120);
		context.takeScreenshot("16-neblina-na-metade");

		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 0 0 0");
		context.waitTicks(200);
		mundo.getServer().runCommand("execute as @p run sussurros teste efeito apagao 20");
		context.waitTicks(8);
		context.takeScreenshot("17-apagao-fechando");
		context.waitTicks(20);
		context.takeScreenshot("18-apagao-fechado");
		mundo.getServer().runCommand("execute as @p run sussurros teste efeito acordar 40");
		context.waitTicks(30);
		context.takeScreenshot("19-acordando");
		context.waitTicks(30);

		// Avesso: a cor própria do lugar (flag 1).
		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 0 0 0.6 1");
		context.waitTicks(200);
		context.takeScreenshot("20-cor-do-avesso");
		mundo.getServer().runCommand("execute as @p run sussurros teste sentidos 0 0 0 0");
		context.waitTicks(40);
	}
}
