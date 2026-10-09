package com.sussurros.teste;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Avesso;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
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
			veu(context, mundo);
			lugares(context, mundo);
			avesso(context, mundo);
			sentidos(context, mundo);
			cacada(context, mundo);
		}
		// Um segundo mundo, de terreno normal, só para a dimensão: o mundo de cima é plano, e num mundo plano
		// não dá para ver se os morros de lá são os mesmos daqui.
		try (TestSingleplayerContext mundo = context.worldBuilder().setUseConsistentSettings(false).create()) {
			mundo.getClientLevel().waitForChunksRender();
			avessoEmTerrenoNormal(context, mundo);
		}
	}

	/**
	 * Num mundo de terreno normal: a altura do chão em nove pontos fora da cópia tem de ser a mesma dos dois
	 * lados, e o jogador não pode chegar dentro de um bloco. As alturas vão para o log do jogo.
	 */
	private static void avessoEmTerrenoNormal(ClientGameTestContext context, TestSingleplayerContext mundo) {
		mundo.getServer().runCommand("difficulty peaceful");
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("weather clear");
		mundo.getServer().runCommand("execute as @p run sussurros fase 3");
		mundo.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 5");
		context.waitTicks(40);
		context.takeScreenshot("50-terreno-normal-antes");
		int[][] pontos = {{-30, -30}, {0, -30}, {30, -30}, {-30, 0}, {30, 0}, {-30, 30}, {0, 30}, {30, 30}, {22, 22}};
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		int[] daqui = mundo.getServer().computeOnServer(server -> alturas(server.overworld(), partida, pontos));
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso");
		context.waitTicks(160);
		mundo.getClientLevel().waitForChunksRender();
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			ServerLevel la = server.getLevel(Avesso.DIMENSAO);
			conferir(la != null && p.level() == la, "deveria estar na dimensão");
			conferir(!p.isInWall(), "não pode chegar dentro de um bloco");
			int[] deLa = alturas(la, partida, pontos);
			int iguais = 0;
			StringBuilder texto = new StringBuilder();
			for (int i = 0; i < pontos.length; i++) {
				iguais += Math.abs(daqui[i] - deLa[i]) <= 1 ? 1 : 0;
				texto.append(daqui[i]).append('/').append(deLa[i]).append(' ');
			}
			Sussurros.LOGGER.info("[teste] alturas do chão, mundo normal/avesso: {}({} de {} iguais)", texto, iguais, pontos.length);
			// Árvore conta como chão no mundo normal e não existe do outro lado: por isso não se exige os nove.
			conferir(iguais >= 5, "o terreno de lá deveria ser o mesmo daqui: " + texto);
		});
		context.takeScreenshot("51-terreno-normal-do-outro-lado");
		mundo.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 120 0");
		context.waitTicks(20);
		context.takeScreenshot("52-terreno-normal-do-outro-lado-virado");
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso voltar");
		context.waitTicks(100);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.level() == server.overworld() && p.blockPosition().equals(partida), "deveria voltar ao ponto de onde saiu");
		});
	}

	private static int[] alturas(ServerLevel level, BlockPos centro, int[][] pontos) {
		int[] resultado = new int[pontos.length];
		for (int i = 0; i < pontos.length; i++) {
			int x = centro.getX() + pontos[i][0];
			int z = centro.getZ() + pontos[i][1];
			level.getChunk(x >> 4, z >> 4);
			resultado[i] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		}
		return resultado;
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
		String[] desfechos = {"silencio", "tregua", "conta", "presenca"};
		for (int i = 0; i < desfechos.length; i++) {
			mundo.getServer().runCommand("execute as @p run sussurros teste ossos " + desfechos[i]);
			context.waitTicks(12);
			context.takeScreenshot("07-ossos-" + (i + 1) + "-" + desfechos[i]);
			mundo.getServer().runCommand("kill @e[tag=sussurros_ossos]");
			context.waitTicks(4);
		}
		// O último desfecho o chama em até seis segundos. Com ele ali, os ossos que "apontam" têm para onde:
		// o jogador se vira para ele antes de jogar, então na foto a fila tem de aparecer de pé na tela.
		context.waitTicks(140);
		mundo.getServer().runCommand("execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] feet run tp @s ~ ~ ~ ~ ~");
		context.waitTicks(4);
		mundo.getServer().runCommand("execute as @p run sussurros teste ossos apontam");
		mundo.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ ~ 60");
		context.waitTicks(12);
		context.takeScreenshot("07-ossos-5-apontam");
		mundo.getServer().runCommand("kill @e[tag=sussurros_ossos]");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		camera(mundo, partida, 0, 5, 60);
		context.waitTicks(4);

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

	/**
	 * O Véu, de dia, sobre o piso de pedra que sobrou das fotos dos itens: antes (uma vaca, um suporte com
	 * armadura e uma tocha na frente), aberto (as entidades somem, a cor e a neblina mudam, a tocha apaga) e
	 * rasgado pela vela (tudo de volta). É também a única foto que prova que o mixin do cliente carregou.
	 */
	private static void veu(ClientGameTestContext context, TestSingleplayerContext mundo) {
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		int x = partida.getX();
		int y = partida.getY();
		int z = partida.getZ();
		camera(mundo, partida, 0, 0, 8);
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros fase 3");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "summon minecraft:cow %d %d %d {NoAI:1b,Tags:[\"sussurros_teste\"]}", x - 2, y, z - 5));
		mundo.getServer().runCommand(String.format(Locale.ROOT,
				"summon minecraft:armor_stand %d %d %d {Tags:[\"sussurros_teste\"],equipment:{head:{id:\"minecraft:iron_helmet\"},chest:{id:\"minecraft:iron_chestplate\"}}}",
				x + 2, y, z - 5));
		mundo.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:torch", x, y, z - 4));
		context.waitTicks(20);
		context.takeScreenshot("09v1-veu-antes");
		mundo.getServer().runCommand("execute as @p run sussurros evento veu");
		context.waitTicks(40);
		context.takeScreenshot("09v2-veu-aberto");
		context.waitTicks(120);
		context.takeScreenshot("09v3-veu-oito-segundos");
		mundo.getServer().runOnServer(server -> Diretor.acenderVela(server.getPlayerList().getPlayers().get(0)));
		context.waitTicks(40);
		context.takeScreenshot("09v4-veu-rasgado-pela-vela");
		mundo.getServer().runCommand("kill @e[tag=sussurros_teste]");
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			Diretor.esquecer(p);
			p.level().setBlockAndUpdate(partida, Blocks.AIR.defaultBlockState());
			p.level().setBlockAndUpdate(partida.offset(0, 0, -4), Blocks.AIR.defaultBlockState());
		});
		mundo.getServer().runCommand("execute as @p run sussurros fase 0");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", x + 0.5, y, z + 0.5));
		context.waitTicks(20);
	}

	/**
	 * A dimensão. O servidor de teste não a carrega, então é aqui, num mundo de verdade, que se confere ir e
	 * voltar: além das fotos, as verificações rodam no servidor e derrubam o teste se falharem.
	 */
	private static void avesso(ClientGameTestContext context, TestSingleplayerContext mundo) {
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		BlockPos ouro = partida.offset(2, 0, -4);
		BlockPos bau = partida.offset(-2, 0, -4);
		BlockPos porta = partida.offset(0, 0, -5);
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros fase 3");
		camera(mundo, partida, 0, 0, 10);
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			level.setBlockAndUpdate(ouro, Blocks.GOLD_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(ouro.above(), Blocks.TORCH.defaultBlockState());
			level.setBlockAndUpdate(bau, Blocks.CHEST.defaultBlockState());
			((Container) level.getBlockEntity(bau)).setItem(0, new ItemStack(Items.BREAD));
			BlockState baixo = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
			level.setBlock(porta, baixo, 3);
			level.setBlock(porta.above(), baixo.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
			for (int dx = -1; dx <= 1; dx += 2) {
				for (int dy = 0; dy <= 2; dy++) {
					level.setBlockAndUpdate(porta.offset(dx, dy, 0), Blocks.OAK_PLANKS.defaultBlockState());
				}
			}
		});
		context.waitTicks(20);
		context.takeScreenshot("09x1-avesso-antes");

		// Primeira visita: o mesmo lugar, apagado e vazio.
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso");
		context.waitTicks(120);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			ServerLevel la = server.getLevel(Avesso.DIMENSAO);
			conferir(la != null, "a dimensão deveria existir num mundo de verdade");
			conferir(p.level() == la, "deveria estar na dimensão, está em " + p.level().dimension());
			conferir(p.blockPosition().equals(partida), "deveria estar nas mesmas coordenadas: " + p.blockPosition() + " em vez de " + partida);
			conferir(la.getBlockState(ouro).is(Blocks.GOLD_BLOCK), "o bloco de ouro deveria ter sido copiado");
			conferir(la.getBlockState(ouro.above()).isAir(), "a tocha não deveria existir do outro lado");
			conferir(la.getBlockState(bau).is(Blocks.CHEST) && ((Container) la.getBlockEntity(bau)).isEmpty(), "o baú deveria ter sido copiado vazio");
			conferir(la.getBlockState(porta).getValue(BlockStateProperties.OPEN), "a porta deveria estar aberta do outro lado");
			ServerLevel aqui = server.overworld();
			conferir(aqui.getBlockState(ouro.above()).is(Blocks.TORCH), "a tocha de verdade continua lá");
			conferir(((Container) aqui.getBlockEntity(bau)).getItem(0).is(Items.BREAD), "o pão de verdade continua no baú");
			conferir(!aqui.getBlockState(porta).getValue(BlockStateProperties.OPEN), "a porta de verdade continua fechada");
			conferir(Avesso.visitaAtual(p) == 1, "deveria ser a primeira visita");
		});
		context.takeScreenshot("09x2-avesso-dentro");
		mundo.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 90 -25");
		context.waitTicks(15);
		context.takeScreenshot("09x3-avesso-ceu-e-horizonte");
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso voltar");
		context.waitTicks(100);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.level() == server.overworld(), "deveria ter voltado ao mundo normal");
			conferir(p.blockPosition().equals(partida), "deveria voltar ao ponto de onde saiu: " + p.blockPosition() + " em vez de " + partida);
			conferir(Memoria.de(p).get("avesso_dentro") == 0, "não deveria estar marcado como lá dentro");
			Avesso.definirVisitas(p, 1);
		});
		camera(mundo, partida, 0, 0, 10);
		context.waitTicks(20);
		context.takeScreenshot("09x4-avesso-de-volta");

		// Segunda visita: depois de vinte segundos ele está lá.
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso");
		context.waitTicks(120 + 20 * 22);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.level().dimension() == Avesso.DIMENSAO && Avesso.visitaAtual(p) == 2, "deveria estar lá, na segunda visita");
			conferir(Diretor.criatura(p) != null && !Diretor.criatura(p).isRemoved() && Diretor.criatura(p).level() == p.level(),
					"na segunda visita ele deveria estar lá");
		});
		mundo.getServer().runCommand("execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] eyes run tp @s ~ ~ ~ ~ ~");
		context.waitTicks(10);
		context.takeScreenshot("09x5-avesso-segunda-visita");
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso voltar");
		context.waitTicks(100);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.level() == server.overworld(), "deveria ter voltado da segunda visita");
			conferir(Diretor.criatura(p) == null || Diretor.criatura(p).isRemoved(), "ele não vem junto");
			ServerLevel level = server.overworld();
			((Container) level.getBlockEntity(bau)).clearContent();
			for (BlockPos pos : BlockPos.betweenClosed(partida.offset(-3, 0, -6), partida.offset(3, 3, -3))) {
				level.setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
			}
			Diretor.esquecer(p);
		});
		mundo.getServer().runCommand("kill @e[type=item]");
		mundo.getServer().runCommand("execute as @p run sussurros fase 0");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
	}

	private static void conferir(boolean certo, String mensagem) {
		if (!certo) {
			throw new AssertionError("Avesso: " + mensagem);
		}
	}

	/** O que o mod ergue no mundo: a porta sozinha, vista de frente, e a figura de palha, de longe e de perto. */
	private static void lugares(ClientGameTestContext context, TestSingleplayerContext mundo) {
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		mundo.getServer().runCommand("time set noon");
		camera(mundo, partida, 0, 8, 0);
		context.waitTicks(5);
		mundo.getServer().runCommand("execute as @p run sussurros teste lugar soleira");
		context.waitTicks(20);
		context.takeScreenshot("09w1-soleira");

		// A figura de palha: a cama passa a ser o ponto de partida, e cinco noites passam de uma vez.
		BlockPos boneco = mundo.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			Memoria m = Memoria.de(p);
			m.set(Memoria.TEM_CAMA, 1);
			m.set(Memoria.CAMA_X, partida.getX());
			m.set(Memoria.CAMA_Y, partida.getY());
			m.set(Memoria.CAMA_Z, partida.getZ());
			m.salvar();
			for (int noite = 0; noite < 5; noite++) {
				Diretor.testarLugar(p, "boneco");
			}
			m = Memoria.de(p);
			return new BlockPos(m.get("boneco_x"), m.get("boneco_y"), m.get("boneco_z"));
		});
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f facing %.2f %.2f %.2f",
				partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5, boneco.getX() + 0.5, boneco.getY() + 1.6, boneco.getZ() + 0.5));
		context.waitTicks(20);
		context.takeScreenshot("09w2-boneco-da-cama");
		double dx = partida.getX() - boneco.getX();
		double dz = partida.getZ() - boneco.getZ();
		double comprimento = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f facing %.2f %.2f %.2f",
				boneco.getX() + 0.5 + dx / comprimento * 4.0, boneco.getY(), boneco.getZ() + 0.5 + dz / comprimento * 4.0,
				boneco.getX() + 0.5, boneco.getY() + 1.6, boneco.getZ() + 0.5));
		context.waitTicks(20);
		context.takeScreenshot("09w3-boneco-de-perto");

		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = p.level();
			Memoria m = Memoria.de(p);
			BlockPos porta = new BlockPos(m.get("soleira_x"), m.get("soleira_y"), m.get("soleira_z"));
			for (BlockPos pos : BlockPos.betweenClosed(porta.offset(-1, 0, -1), porta.offset(1, 2, 1))) {
				level.setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
			}
			for (int y = 2; y >= 0; y--) {
				level.setBlockAndUpdate(boneco.above(y), Blocks.AIR.defaultBlockState());
			}
			Diretor.esquecer(p);
		});
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
