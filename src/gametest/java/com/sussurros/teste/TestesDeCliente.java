package com.sussurros.teste;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Avesso;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.entidade.HospedeEntity;
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
			saidaAndando(context, mundo);
			dePerto(context, mundo);
			planoB(context, mundo);
			caverna(context, mundo);
			deQuatro(context, mundo);
			cacada(context, mundo);
			capturaQueLeva(context, mundo);
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
		// Antes de montar a casa das fotos: a volta de lá apaga, por uns minutos, as luzes que houver por perto.
		primeiraIdaGarantida(context, mundo, partida);
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
			Avesso.definirVisitas(p, 2);
		});

		// Terceira visita: a medida está errada, e aos quarenta e cinco segundos ele vem até encostar.
		camera(mundo, partida, 0, 0, 10);
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso");
		context.waitTicks(120);
		long chegada = mundo.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			ServerLevel la = server.getLevel(Avesso.DIMENSAO);
			conferir(p.level() == la && Avesso.visitaAtual(p) == 3, "deveria estar lá, na terceira visita");
			conferir(la.getBlockState(ouro).is(Blocks.GOLD_BLOCK) && la.getBlockState(ouro.east()).is(Blocks.GOLD_BLOCK),
					"na terceira visita a fatia do bloco de ouro deveria estar repetida");
			conferir(la.getBlockState(bau).is(Blocks.CHEST) && !la.getBlockState(bau.east()).is(Blocks.CHEST), "o lado de cá não muda");
			return server.overworld().getGameTime();
		});
		context.takeScreenshot("09x6-avesso-terceira-visita-chegada");
		// Espera ele começar a vir (aos 45 s de visita; a visita começou uns 5 s antes desta medida).
		context.waitTicks(20 * 42);
		mundo.getServer().runCommand("execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] eyes run tp @s ~ ~ ~ ~ ~");
		context.waitTicks(30);
		context.takeScreenshot("09x7-avesso-terceira-visita-ele-vem");
		boolean voltou = false;
		for (int i = 0; i < 40 && !voltou; i++) {
			context.waitTicks(20);
			voltou = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).level() == server.overworld());
		}
		conferir(voltou, "na terceira visita ele deveria ter encostado e o jogador, acordado");
		context.waitTicks(80);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			long durou = (server.overworld().getGameTime() - chegada) / 20;
			Sussurros.LOGGER.info("[teste] terceira visita: de volta {} s depois da conferência de chegada", durou);
			conferir(p.blockPosition().equals(partida), "deveria voltar ao ponto de onde saiu");
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

	/** Uma semente com que o próximo sorteio do jogador dá "sem sorte" (acima de qualquer chance de ser levado). */
	private static long sementeSemSorte() {
		for (long semente = 1; ; semente++) {
			if (RandomSource.create(semente).nextFloat() >= 0.5F) {
				return semente;
			}
		}
	}

	/**
	 * A primeira ida garantida (0.9.0-alpha21): quem nunca foi e deita três noites, sem sorte nenhuma no sorteio,
	 * é levado na terceira. É o caminho de verdade, do evento de deitar até chegar do outro lado, que o servidor
	 * de teste não alcança (ele não carrega a dimensão). Sem foto: não há nada novo na tela.
	 */
	private static void primeiraIdaGarantida(ClientGameTestContext context, TestSingleplayerContext mundo, BlockPos partida) {
		BlockPos pe = partida.offset(3, 0, 2);
		BlockPos cabeca = pe.east();
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			BlockState cama = Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
			level.setBlock(pe, cama.setValue(BedBlock.PART, BedPart.FOOT), 3);
			level.setBlock(cabeca, cama.setValue(BedBlock.PART, BedPart.HEAD), 3);
			Avesso.definirVisitas(server.getPlayerList().getPlayers().get(0), 0);
		});
		// Noite (quem dorme de dia é acordado pelo próprio jogo). Cada volta do laço é outro dia de jogo.
		mundo.getServer().runCommand("time set 14000");
		for (int noite = 1; noite <= 3; noite++) {
			int n = noite;
			mundo.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().get(0);
				p.getRandom().setSeed(sementeSemSorte());
				p.startSleeping(cabeca);
			});
			// Dois segundos de olhos fechados, o apagão, a travessia.
			context.waitTicks(90);
			String onde = mundo.getServer().computeOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().get(0);
				return (p.level().dimension() == Avesso.DIMENSAO ? "LA" : "AQUI") + " visita=" + Avesso.visitaAtual(p)
						+ " dormindo=" + p.isSleeping();
			});
			Sussurros.LOGGER.info("[primeira ida garantida] noite {}: {}", n, onde);
			if (n < 3) {
				conferir(onde.startsWith("AQUI"), "na noite " + n + ", sem sorte, ele ainda não deveria ter sido levado: " + onde);
				mundo.getServer().runOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().get(0);
					if (p.isSleeping()) {
						p.stopSleepInBed(true, true);
					}
				});
				mundo.getServer().runCommand("time add 24000");
				context.waitTicks(10);
			} else {
				conferir(onde.startsWith("LA visita=1"), "na terceira noite deitado ele deveria ter sido levado, na primeira visita: " + onde);
			}
		}
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso voltar");
		context.waitTicks(100);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.level() == server.overworld(), "deveria ter voltado ao mundo normal");
			conferir(Memoria.de(p).get("avesso_visitas") == 1, "a ida garantida conta como a primeira visita: " + Memoria.de(p).get("avesso_visitas"));
			// As fotos a seguir começam do zero, como se ele nunca tivesse ido.
			Avesso.definirVisitas(p, 0);
			ServerLevel level = server.overworld();
			level.setBlock(cabeca, Blocks.AIR.defaultBlockState(), 2 | 16);
			level.setBlock(pe, Blocks.AIR.defaultBlockState(), 2 | 16);
		});
		mundo.getServer().runCommand("kill @e[type=item]");
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
	}

	/** Uma captura que, em vez de deslocar, leva para o outro lado; a volta é para o ponto onde ele foi pego. */
	private static void capturaQueLeva(ClientGameTestContext context, TestSingleplayerContext mundo) {
		mundo.getServer().runCommand("difficulty peaceful");
		mundo.getServer().runCommand("time set 18000");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		context.waitTicks(20);
		BlockPos partida = mundo.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			Diretor.esquecer(p);
			Avesso.forcarNaProximaCaptura = true;
			return p.blockPosition();
		});
		mundo.getServer().runCommand("execute as @p run sussurros fase 4");
		context.waitTicks(20);
		mundo.getServer().runCommand("execute as @p run sussurros evento caca");
		boolean levado = false;
		for (int i = 0; i < 110 && !levado; i++) {
			context.waitTicks(20);
			levado = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).level().dimension() == Avesso.DIMENSAO);
		}
		conferir(levado, "a captura deveria ter levado o jogador para o outro lado");
		context.waitTicks(130);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.blockPosition().distManhattan(partida) <= 2, "deveria acordar lá no mesmo ponto em que foi pego: " + p.blockPosition() + " e " + partida);
			conferir(!p.isInWall(), "não pode acordar dentro de um bloco");
		});
		context.takeScreenshot("37-captura-do-outro-lado");
		mundo.getServer().runCommand("execute as @p run sussurros teste avesso voltar");
		context.waitTicks(100);
		mundo.getServer().runOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			conferir(p.level() == server.overworld() && p.blockPosition().distManhattan(partida) <= 2, "deveria voltar para onde foi pego");
			Avesso.forcarNaProximaCaptura = false;
			Diretor.esquecer(p);
		});
		mundo.getServer().runCommand("kill @e[type=item]");
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

		// A casa: de fora (com a fumaça), e por dentro, olhando para o fundo.
		camera(mundo, partida, 0, 0, 0);
		context.waitTicks(5);
		mundo.getServer().runCommand("execute as @p run sussurros teste lugar casa");
		context.waitTicks(60);
		context.takeScreenshot("09w4-casa-de-fora");
		BlockPos casa = mundo.getServer().computeOnServer(server -> {
			Memoria m = Memoria.de(server.getPlayerList().getPlayers().get(0));
			return new BlockPos(m.get("casa_vigia_x"), m.get("casa_vigia_y"), m.get("casa_vigia_z"));
		});
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 8", casa.getX() + 0.5, casa.getY(), casa.getZ() + 1.5));
		context.waitTicks(20);
		context.takeScreenshot("09w5-casa-por-dentro");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 300 20", casa.getX() - 1.5, casa.getY(), casa.getZ() + 0.5));
		context.waitTicks(20);
		context.takeScreenshot("09w6-casa-por-dentro-outro-lado");
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			if (level.getBlockEntity(casa.offset(2, 0, -1)) instanceof Container bau) {
				bau.clearContent();
			}
			for (BlockPos pos : BlockPos.betweenClosed(casa.offset(-3, 0, -2), casa.offset(3, 3, 4))) {
				level.setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
			}
		});

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

	/**
	 * A saída andando (0.9.0-alpha17), de dia para dar para ver: ele aparece fora da tela, o jogador se vira,
	 * e em vez de dissolver ele segura o olhar, vira e vai embora de lado. As fotos são o único jeito de saber
	 * se o andar dele, de pé e devagar, parece alguém andando ou um boneco deslizando.
	 */
	private static void saidaAndando(ClientGameTestContext context, TestSingleplayerContext mundo) {
		String virar = "execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] eyes run tp @s ~ ~ ~ ~ ~";
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		context.waitTicks(20);
		mundo.getServer().runCommand("execute as @p run sussurros teste saida");
		context.waitTicks(5);
		boolean nasceu = mundo.getServer().computeOnServer(server -> Diretor.criatura(server.getPlayerList().getPlayers().get(0)) != null);
		conferir(nasceu, "a presença de teste deveria ter nascido");
		mundo.getServer().runCommand(virar);
		context.waitTicks(12);
		context.takeScreenshot("37-saida-segura-o-olhar");
		context.waitTicks(26);
		boolean indoEmbora = mundo.getServer().computeOnServer(server -> {
			HospedeEntity h = Diretor.criatura(server.getPlayerList().getPlayers().get(0));
			return h != null && h.estaIndoEmbora() && !h.isSumindo();
		});
		conferir(indoEmbora, "visto, ele deveria estar indo embora andando, sem dissolver");
		mundo.getServer().runCommand(virar);
		context.waitTicks(4);
		context.takeScreenshot("38-saida-andando");
		context.waitTicks(24);
		mundo.getServer().runCommand(virar);
		context.waitTicks(4);
		context.takeScreenshot("39-saida-andando-mais-longe");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		context.waitTicks(20);
	}

	/**
	 * Ele de perto, num lugar apertado (0.9.0-alpha19): um túnel de pedra de um bloco de largura e dois de altura,
	 * com uma curva e uma tocha, igual ao dos testes de servidor. A procura é a de verdade; só o modo é outro,
	 * para ele não sumir quando o jogador vira (ver Diretor.dePertoParaTeste). Ninguém tinha visto a pose
	 * abaixada parada, olhando: estas fotos são para isso.
	 */
	private static void dePerto(ClientGameTestContext context, TestSingleplayerContext mundo) {
		String virar = "execute as @p at @s facing entity @e[type=sussurros:hospede,limit=1,sort=nearest] eyes run tp @s ~ ~ ~ ~ ~";
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		// Longe do que as outras fotos montam e de onde a caçada, a seguir, vai procurar lugar.
		BlockPos pes = partida.offset(80, 0, 0);
		int curva = -10;
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().get(0).level();
			for (int x = -3; x <= 11; x++) {
				for (int z = curva - 3; z <= 6; z++) {
					for (int y = 0; y <= 4; y++) {
						level.setBlock(pes.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
					}
				}
			}
			for (int y = 0; y <= 1; y++) {
				for (int z = curva; z <= 3; z++) {
					level.setBlock(pes.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
				for (int x = 0; x <= 8; x++) {
					level.setBlock(pes.offset(x, y, curva), Blocks.AIR.defaultBlockState(), 3);
				}
			}
			level.setBlock(pes.offset(-1, 1, curva), Blocks.TORCH.defaultBlockState(), 3);
		});
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5));
		context.waitTicks(40);
		mundo.getServer().runCommand("execute as @p run sussurros teste deperto parado");
		context.waitTicks(10);
		String nasceu = mundo.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			HospedeEntity h = Diretor.criatura(p);
			return h == null ? "ninguém" : String.format(Locale.ROOT, "dist=%.1f abaixado=%s altura=%.1f", h.distanceTo(p), h.isAbaixado(), h.getBbHeight());
		});
		Sussurros.LOGGER.info("[de perto] {}", nasceu);
		conferir(nasceu.startsWith("dist=10.0 abaixado=true altura=1.9"), "no túnel ele deveria nascer na curva, a dez blocos, abaixado: " + nasceu);
		// O que o jogador vê: vira, e ele está na curva, à luz da tocha.
		mundo.getServer().runCommand(virar);
		context.waitTicks(15);
		context.takeScreenshot("40-de-perto-na-curva-do-tunel");
		// Mais perto do que ele deixaria num jogo de verdade, só para ver a pose.
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 0", pes.getX() + 0.5, pes.getY(), pes.getZ() + curva + 4.5));
		context.waitTicks(5);
		mundo.getServer().runCommand(virar);
		context.waitTicks(15);
		context.takeScreenshot("41-de-perto-a-quatro-blocos");
		// A mesma pose à luz do dia: o teto do túnel vira vidro.
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().get(0).level();
			for (int y = 2; y <= 4; y++) {
				for (int z = curva; z <= 3; z++) {
					level.setBlock(pes.offset(0, y, z), Blocks.GLASS.defaultBlockState(), 3);
				}
			}
		});
		context.waitTicks(30);
		mundo.getServer().runCommand(virar);
		context.waitTicks(10);
		context.takeScreenshot("42-de-perto-com-teto-de-vidro");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
	}

	/**
	 * O plano B (0.9.0-alpha20): o sinal que fecha uma construção que acabou sem ninguém. Um corredor de pedra sem
	 * saída, igual ao dos testes de servidor, com uma tocha no fundo: o jogador olha para o único lado que tem
	 * espaço, a procura de lugar falha (ele nunca nasce na tela) e o sinal acontece ali, à vista. Primeiro o pó
	 * que cai do teto; da segunda vez, a tocha do fundo apagada. As fotos são para ver se o pó aparece, e de que cor.
	 */
	private static void planoB(ClientGameTestContext context, TestSingleplayerContext mundo) {
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		// Longe do túnel das fotos de perto e de onde a caçada, a seguir, vai procurar lugar.
		BlockPos pes = partida.offset(80, 0, 40);
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().get(0).level();
			for (int x = -5; x <= 5; x++) {
				for (int z = -5; z <= 15; z++) {
					for (int y = 0; y <= 4; y++) {
						level.setBlock(pes.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
					}
				}
			}
			for (int z = 0; z <= 12; z++) {
				for (int y = 0; y <= 1; y++) {
					level.setBlock(pes.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
			level.setBlock(pes.offset(0, 0, 11), Blocks.TORCH.defaultBlockState(), 3);
		});
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5));
		context.waitTicks(40);
		context.takeScreenshot("43-plano-b-o-corredor-antes");
		mundo.getServer().runCommand("execute as @p run sussurros teste planob");
		// O pó cai em três punhados (aos 0, 9 e 18 ticks): a foto pega os dois primeiros no ar.
		context.waitTicks(14);
		String primeiro = mundo.getServer().computeOnServer(server -> Diretor.planoBParaTeste(server.getPlayerList().getPlayers().get(0)));
		conferir("PO".equals(primeiro), "no corredor sem saída, à vista e com teto, o sinal deveria ser o pó: " + primeiro);
		context.takeScreenshot("44-plano-b-po-do-teto");
		context.waitTicks(60);
		mundo.getServer().runCommand("execute as @p run sussurros teste planob");
		context.waitTicks(12);
		String segundo = mundo.getServer().computeOnServer(server -> Diretor.planoBParaTeste(server.getPlayerList().getPlayers().get(0)));
		conferir("TOCHA".equals(segundo), "da segunda vez seguida o sinal deveria ser a tocha: " + segundo);
		boolean tochaDeVerdade = mundo.getServer().computeOnServer(server ->
				server.getPlayerList().getPlayers().get(0).level().getBlockState(pes.offset(0, 0, 11)).is(Blocks.TORCH));
		conferir(tochaDeVerdade, "a tocha apagada é miragem: o bloco de verdade tem de continuar no lugar");
		context.takeScreenshot("45-plano-b-a-tocha-apagada");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
	}

	/**
	 * A caverna com corpo (0.9.0-alpha22 e alpha23): a rachadura, a pedrinha, os morcegos, a tampa do baú e a
	 * passagem fechada, no mesmo túnel dos testes de servidor, com uma tocha a mais perto do fundo para a parede
	 * aparecer na foto. Todas vêm pelos comandos de teste, mas o que desenha é o caminho de verdade. A rachadura é o que mais precisa de foto: é o progresso de
	 * quebra do jogo mandado só para ele, com um número que não é de entidade nenhuma, e nenhum teste de servidor
	 * diz se o jogo desenha isso.
	 */
	private static void caverna(ClientGameTestContext context, TestSingleplayerContext mundo) {
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		// Longe do túnel das fotos de perto, do corredor do plano B e de onde a caçada vai procurar lugar.
		BlockPos pes = partida.offset(80, 0, 80);
		int curva = -10;
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().get(0).level();
			for (int x = -3; x <= 11; x++) {
				for (int z = curva - 3; z <= 6; z++) {
					for (int y = 0; y <= 4; y++) {
						level.setBlock(pes.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
					}
				}
			}
			for (int y = 0; y <= 1; y++) {
				for (int z = curva; z <= 3; z++) {
					level.setBlock(pes.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
				for (int x = 0; x <= 8; x++) {
					level.setBlock(pes.offset(x, y, curva), Blocks.AIR.defaultBlockState(), 3);
				}
			}
			level.setBlock(pes.offset(-1, 1, curva), Blocks.TORCH.defaultBlockState(), 3);
			level.setBlock(pes.offset(-1, 1, 2), Blocks.TORCH.defaultBlockState(), 3);
		});
		String noLugar = "tp @p %.2f %d %.2f %d %d";
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5, 0, 0));
		context.waitTicks(40);
		context.takeScreenshot("46-caverna-o-fundo-do-tunel");

		// A rachadura, no bloco do fundo, para onde ele olha: cresce em um segundo e para na metade.
		mundo.getServer().runCommand("execute as @p run sussurros cena rachadura");
		context.waitTicks(30);
		BlockPos rachado = mundo.getServer().computeOnServer(server -> Diretor.rachaduraParaTeste(server.getPlayerList().getPlayers().get(0)));
		conferir(pes.offset(0, 1, 4).equals(rachado), "o comando deveria rachar o bloco do fundo, na altura dos olhos: " + rachado);
		boolean pedraInteira = mundo.getServer().computeOnServer(server ->
				server.getPlayerList().getPlayers().get(0).level().getBlockState(pes.offset(0, 1, 4)).is(Blocks.STONE));
		conferir(pedraInteira, "a rachadura é só desenho: o bloco de verdade tem de continuar inteiro");
		context.takeScreenshot("47-caverna-a-rachadura");

		// A pedrinha: três toques no chão à frente dele. Olhando um pouco para baixo, para o chão caber na foto.
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5, 0, 30));
		context.waitTicks(20);
		mundo.getServer().runCommand("execute as @p run sussurros cena pedrinha");
		context.waitTicks(3);
		String pedrinha = mundo.getServer().computeOnServer(server -> Diretor.cavernaParaTeste(server.getPlayerList().getPlayers().get(0)));
		conferir("PEDRINHA".equals(pedrinha), "no túnel a pedrinha deveria achar onde cair: " + pedrinha);
		context.takeScreenshot("48-caverna-a-pedrinha-primeiro-toque");
		context.waitTicks(5);
		context.takeScreenshot("48-caverna-a-pedrinha-segundo-toque");

		// Os morcegos: saem da curva, às costas dele. Ele vira para ver.
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5, 0, 0));
		context.waitTicks(20);
		mundo.getServer().runCommand("execute as @p run sussurros cena morcegos");
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5, 180, 0));
		context.waitTicks(22);
		int morcegos = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).level()
				.getEntitiesOfClass(Bat.class, new AABB(pes).inflate(30.0), m -> m.entityTags().contains("sussurros_morcego")).size());
		conferir(morcegos >= 3 && morcegos <= 5, "deveriam ter saído de três a cinco morcegos: " + morcegos);
		context.takeScreenshot("49-caverna-os-morcegos-vindo");
		context.waitTicks(12);
		context.takeScreenshot("49-caverna-os-morcegos-passando");
		mundo.getServer().runCommand("kill @e[tag=sussurros_morcego]");

		// A tampa (0.9.0-alpha23): um baú no fundo do túnel levanta a tampa só para ele. A lista de baús por perto
		// só é refeita a cada dez segundos: por isso a espera antes do comando.
		BlockPos bau = pes.offset(0, 0, 3);
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().get(0).level();
			level.setBlock(bau, Blocks.CHEST.defaultBlockState(), 3);
			level.setBlock(pes.offset(-1, 1, -5), Blocks.TORCH.defaultBlockState(), 3);
		});
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5, 0, 20));
		context.waitTicks(230);
		mundo.getServer().runCommand("execute as @p run sussurros cena tampa");
		context.waitTicks(12);
		String tampa = mundo.getServer().computeOnServer(server -> Diretor.cavernaParaTeste(server.getPlayerList().getPlayers().get(0)));
		conferir("TAMPA".equals(tampa), "com um baú a três blocos, a tampa deveria levantar: " + tampa);
		int aberto = mundo.getServer().computeOnServer(server ->
				ChestBlockEntity.getOpenCount(server.getPlayerList().getPlayers().get(0).level(), bau));
		conferir(aberto == 0, "a tampa levantada é só desenho: o baú de verdade tem de continuar fechado, e está aberto por " + aberto);
		context.takeScreenshot("49t-caverna-a-tampa-levantada");
		context.waitTicks(40);
		context.takeScreenshot("49t-caverna-a-tampa-de-volta");

		// A passagem fechada (0.9.0-alpha23): ele anda o túnel da curva até o fundo, e o caminho por onde veio
		// aparece fechado. Cada passo espera mais de dois segundos, que é de quanto em quanto o caminho é anotado.
		mundo.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).level().setBlock(bau, Blocks.AIR.defaultBlockState(), 3));
		mundo.getServer().runCommand("kill @e[type=item]");
		for (double z : new double[] {-9.5, -7.5, -5.5, -3.5, -1.5, 0.5, 2.5, 3.5}) {
			mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + z, 0, 0));
			context.waitTicks(45);
		}
		context.waitTicks(220);
		mundo.getServer().runCommand("execute as @p run sussurros cena passagem");
		context.waitTicks(5);
		int pedras = mundo.getServer().computeOnServer(server -> Diretor.miragensParaTeste(server.getPlayerList().getPlayers().get(0), "PASSAGEM_FECHADA"));
		conferir(pedras == 2, "o túnel de um bloco por dois deveria fechar com duas pedras: " + pedras);
		boolean tunelAberto = mundo.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			for (BlockPos pos : Diretor.posicoesDeMiragemParaTeste(p, "PASSAGEM_FECHADA")) {
				if (!p.level().getBlockState(pos).isAir()) {
					return false;
				}
			}
			return true;
		});
		conferir(tunelAberto, "a parede é miragem: o túnel de verdade tem de continuar aberto");
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 3.5, 180, 0));
		context.waitTicks(15);
		context.takeScreenshot("49p-caverna-a-passagem-de-longe");
		// Ele volta para ver: a cinco blocos a pedra já não está.
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() - 2.5, 180, 0));
		context.waitTicks(15);
		pedras = mundo.getServer().computeOnServer(server -> Diretor.miragensParaTeste(server.getPlayerList().getPlayers().get(0), "PASSAGEM_FECHADA"));
		conferir(pedras == 0, "de perto a parede já deveria ter se desfeito: " + pedras);
		context.takeScreenshot("49p-caverna-a-passagem-de-perto");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
	}

	/**
	 * A pose de quatro (0.9.0-alpha24), que ninguém viu: num buraco de um bloco numa parede de pedra, de frente, e
	 * em campo aberto, de perfil, com cada um dos braços à frente. De dia, para ver o desenho; ele fica parado no
	 * modo do outro lado (ver Diretor.deQuatroParaFoto). As conferências derrubam o teste: de quatro ele tem menos
	 * de um bloco de altura e cabe no buraco sem encostar em nada.
	 */
	private static void deQuatro(ClientGameTestContext context, TestSingleplayerContext mundo) {
		BlockPos partida = mundo.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).blockPosition());
		// Longe do que as outras fotos montam e de onde a caçada, a seguir, vai procurar lugar.
		BlockPos pes = partida.offset(-80, 0, 0);
		BlockPos buraco = pes.offset(0, 0, -5);
		String noLugar = "tp @p %.2f %d %.2f %d %d";
		mundo.getServer().runCommand("time set noon");
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runOnServer(server -> {
			ServerLevel level = server.getPlayerList().getPlayers().get(0).level();
			for (BlockPos pos : BlockPos.betweenClosed(pes.offset(-4, 0, -5), pes.offset(4, 3, -5))) {
				level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
			}
			level.setBlock(buraco, Blocks.AIR.defaultBlockState(), 3);
		});
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5, 180, 12));
		context.waitTicks(40);
		String noBuraco = mundo.getServer().computeOnServer(server -> {
			ServerPlayer p = server.getPlayerList().getPlayers().get(0);
			Diretor.deQuatroParaFoto(p, buraco, Float.NaN);
			HospedeEntity h = Diretor.criatura(p);
			return h == null ? "ninguém" : String.format(Locale.ROOT, "deQuatro=%s altura=%.1f livre=%s", h.isDeQuatro(), h.getBbHeight(), p.level().noCollision(h));
		});
		Sussurros.LOGGER.info("[de quatro] no buraco: {}", noBuraco);
		conferir(noBuraco.equals("deQuatro=true altura=0.9 livre=true"), "de quatro ele deveria caber no buraco de um bloco: " + noBuraco);
		context.waitTicks(20);
		context.takeScreenshot("55-de-quatro-no-buraco-a-cinco-blocos");
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, pes.getX() + 0.5, pes.getY(), pes.getZ() - 1.3, 180, 20));
		context.waitTicks(15);
		context.takeScreenshot("56-de-quatro-no-buraco-de-perto");
		// De perfil, em campo aberto: cada bloco troca o braço que vai à frente.
		for (int lado = 0; lado < 2; lado++) {
			BlockPos chao = pes.offset(8 + lado, 0, 6);
			mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
			mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, chao.getX() + 0.5, chao.getY(), chao.getZ() + 5.0, 180, 12));
			context.waitTicks(10);
			mundo.getServer().runOnServer(server -> Diretor.deQuatroParaFoto(server.getPlayerList().getPlayers().get(0), chao, 90.0F));
			context.waitTicks(20);
			context.takeScreenshot(lado == 0 ? "57-de-quatro-de-perfil" : "58-de-quatro-de-perfil-o-outro-braco");
		}
		// De cima, para ver o corpo inteiro sem o chão na frente; e em cima de um bloco, de lado, para ver o que
		// o chão esconderia (um membro que mergulha na terra não aparece nas fotos de perfil).
		BlockPos alto = pes.offset(16, 0, 6);
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		mundo.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).level()
				.setBlock(alto, Blocks.STONE.defaultBlockState(), 3));
		context.waitTicks(5);
		mundo.getServer().runOnServer(server -> Diretor.deQuatroParaFoto(server.getPlayerList().getPlayers().get(0), alto.above(), 90.0F));
		mundo.getServer().runCommand(String.format(Locale.ROOT, noLugar, alto.getX() + 0.5, alto.getY(), alto.getZ() + 5.0, 180, 0));
		context.waitTicks(20);
		context.takeScreenshot("59-de-quatro-em-cima-de-um-bloco");
		// De cima: o jogador em pé num bloco invisível, seis blocos acima dele, olhando para baixo.
		mundo.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).level()
				.setBlock(alto.above(5), Blocks.BARRIER.defaultBlockState(), 3));
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 90", alto.getX() + 0.5, alto.getY() + 6, alto.getZ() + 0.5));
		context.waitTicks(15);
		context.takeScreenshot("60-de-quatro-de-cima");
		mundo.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).level()
				.setBlock(alto.above(5), Blocks.AIR.defaultBlockState(), 3));
		mundo.getServer().runCommand("execute as @p run sussurros cena parar");
		mundo.getServer().runCommand("kill @e[type=sussurros:hospede]");
		mundo.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 0", partida.getX() + 0.5, partida.getY(), partida.getZ() + 0.5));
		context.waitTicks(20);
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
