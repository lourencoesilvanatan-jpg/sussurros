package com.sussurros.teste;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.sussurros.assombracao.Diretor;
import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.bloco.TigelaBlockEntity;
import com.sussurros.bloco.TigelaOferendaBlock;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

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

			itens(context, mundo);
			sentidos(context, mundo);
			cacada(context, mundo);
		}
	}

	/**
	 * Os blocos e itens da 0.9 numa bancada de pedra, de dia: cada estado de cada bloco, as duas direções da
	 * linha, os ossos caídos, a vela aos pés e os ícones no inventário. Confere desenho, recorte e posição.
	 */
	private static void itens(ClientGameTestContext context, TestSingleplayerContext mundo) {
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		mundo.getServer().runCommand("weather clear");
		// A câmera anda por coordenadas absolutas, sempre com os pés no chão: em sobrevivência, um salto
		// relativo para cima vira queda, e o salto de volta enterra o jogador (aconteceu na primeira versão).
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		// De frente para o norte: daqui em diante "na frente" é z negativo.
		camera(mundo, partida, 0, 0, 35);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = p.level();
			BlockPos pe = p.blockPosition();
			for (BlockPos pos : BlockPos.betweenClosed(pe.offset(-5, -1, -9), pe.offset(5, 4, 9))) {
				level.setBlockAndUpdate(pos.immutable(), pos.getY() < pe.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
			}
			BlockState cinza = ModBlocos.CINZA_ESPALHADA.defaultBlockState();
			// Uma linha de três, de oeste a leste, e os estágios do desgaste ao lado.
			CinzaEspalhadaBlock.Estado[] fileira = {CinzaEspalhadaBlock.Estado.INTACTA, CinzaEspalhadaBlock.Estado.INTACTA,
					CinzaEspalhadaBlock.Estado.INTACTA, CinzaEspalhadaBlock.Estado.RISCADA, CinzaEspalhadaBlock.Estado.GASTA,
					CinzaEspalhadaBlock.Estado.ROMPIDA};
			for (int i = 0; i < fileira.length; i++) {
				level.setBlockAndUpdate(pe.offset(i - 4, 0, -2), cinza.setValue(CinzaEspalhadaBlock.ESTADO, fileira[i]));
			}
			// Uma linha de norte a sul, e as pegadas viradas para os quatro lados.
			for (int i = 0; i < 3; i++) {
				level.setBlockAndUpdate(pe.offset(4, 0, -2 - i), cinza.setValue(CinzaEspalhadaBlock.FRENTE, Direction.EAST));
			}
			Direction[] rumos = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
			for (int i = 0; i < 4; i++) {
				level.setBlockAndUpdate(pe.offset(i - 3, 0, -3), cinza.setValue(CinzaEspalhadaBlock.ESTADO, CinzaEspalhadaBlock.Estado.PEGADA)
						.setValue(CinzaEspalhadaBlock.FRENTE, rumos[i]));
			}
			// Os lampiões: as quatro chamas no chão e, atrás, as quatro penduradas.
			LampiaoPalidoBlock.Chama[] chamas = LampiaoPalidoBlock.Chama.values();
			for (int i = 0; i < chamas.length; i++) {
				BlockState lampiao = ModBlocos.LAMPIAO_PALIDO.defaultBlockState().setValue(LampiaoPalidoBlock.COMBUSTIVEL, 3)
						.setValue(LampiaoPalidoBlock.CHAMA, chamas[i]);
				BlockPos noChao = pe.offset(i * 2 - 3, 0, -5);
				BlockPos pendurado = pe.offset(i * 2 - 3, 1, -7);
				level.setBlockAndUpdate(noChao, lampiao);
				level.setBlockAndUpdate(pendurado.above(), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(pendurado, lampiao.setValue(LanternBlock.HANGING, true));
				// Sem isto, em um segundo todos voltariam à chama calma.
				LampiaoPalidoBlock.perturbar(level, noChao, 0, chamas[i], 20 * 600);
				LampiaoPalidoBlock.perturbar(level, pendurado, 0, chamas[i], 20 * 600);
			}
			// As tigelas: vazia, com um pão, com cinzas.
			TigelaOferendaBlock.Conteudo[] conteudos = TigelaOferendaBlock.Conteudo.values();
			for (int i = 0; i < conteudos.length; i++) {
				BlockPos pos = pe.offset(i * 2 - 2, 0, -4);
				BlockState tigela = ModBlocos.TIGELA_OFERENDA.defaultBlockState();
				level.setBlockAndUpdate(pos, tigela);
				if (conteudos[i] != TigelaOferendaBlock.Conteudo.VAZIA && level.getBlockEntity(pos) instanceof TigelaBlockEntity t) {
					t.guardar(level, new ItemStack(Items.BREAD), tigela);
					if (conteudos[i] == TigelaOferendaBlock.Conteudo.CINZAS) {
						t.esvaziar(level, level.getBlockState(pos), TigelaOferendaBlock.Conteudo.CINZAS);
					}
				}
			}
			p.getInventory().clearContent();
			for (ItemStack item : new ItemStack[] {new ItemStack(ModItems.CINZA_PALIDA, 8), new ItemStack(ModItems.LAMPIAO_PALIDO),
					new ItemStack(ModItems.TIGELA_OFERENDA), new ItemStack(ModItems.CAIXA_DE_MUSICA), new ItemStack(ModItems.OSSOS_DE_AGOURO, 4),
					new ItemStack(ModItems.VELA_PALIDA)}) {
				p.getInventory().add(item);
			}
		});
		context.waitTicks(30);
		context.takeScreenshot("03-itens-bancada");
		camera(mundo, partida, 0, -1, 62);
		context.waitTicks(10);
		context.takeScreenshot("04-itens-de-cima");
		camera(mundo, partida, 0, -1, 12);
		context.waitTicks(10);
		context.takeScreenshot("05-itens-lampioes");
		// De noite, para ver a luz de cada chama.
		mundo.getServer().runCommand("time set midnight");
		context.waitTicks(30);
		context.takeScreenshot("06-itens-lampioes-de-noite");
		mundo.getServer().runCommand("time set noon");

		// Os ossos: quatro desfechos, vistos de cima. Cada jogada cai 1,6 bloco à frente.
		camera(mundo, partida, 0, 5, 60);
		context.waitTicks(10);
		String[] desfechos = {"silencio", "tregua", "presenca", "conta"};
		for (int i = 0; i < desfechos.length; i++) {
			mundo.getServer().runCommand("execute as @p run sussurros teste ossos " + desfechos[i]);
			context.waitTicks(12);
			context.takeScreenshot("07-ossos-" + (i + 1));
			mundo.getServer().runCommand("kill @e[tag=sussurros_ossos]");
			mundo.getServer().runCommand("execute as @p run sussurros cena parar");
			mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
			context.waitTicks(4);
		}

		// A vela aos pés, e os ícones no inventário.
		mundo.getServer().runOnServer(server -> Diretor.acenderVela(server.getPlayerList().getPlayers().get(0)));
		camera(mundo, partida, 0, 7, 50);
		context.waitTicks(10);
		context.takeScreenshot("08-vela-acesa");
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(10);
		context.takeScreenshot("09-inventario");
		context.setScreen(() -> null);
		// Desmonta a bancada (fica só o piso de pedra) e devolve o jogador ao ponto de partida, de costas para ela.
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = p.level();
			for (BlockPos pos : BlockPos.betweenClosed(partida.offset(-5, 0, -9), partida.offset(5, 4, 9))) {
				level.setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
			}
			p.getInventory().clearContent();
			p.setHealth(p.getMaxHealth());
			Diretor.esquecer(p);
		});
		mundo.getServer().runCommand("kill @e[tag=sussurros_ossos]");
		mundo.getServer().runCommand("kill @e[tag=sussurros_oferenda]");
		mundo.getServer().runCommand("kill @e[type=item]");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
	}

	/** Põe o jogador no chão, a "dx" e "dz" blocos do ponto de partida, olhando para o norte com a inclinação dada. */
	private static void camera(TestSingleplayerContext mundo, BlockPos partida, int dx, int dz, int inclinacao) {
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 %d",
				partida.getX() + dx + 0.5, partida.getY(), partida.getZ() + dz + 0.5, inclinacao));
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
