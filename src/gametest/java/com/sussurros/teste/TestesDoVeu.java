package com.sussurros.teste;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Diretor;
import com.sussurros.rede.PacoteSentidos;

/**
 * O Véu num servidor de verdade. O jogador de mentira não tem tela: o que se confere é o que o servidor manda
 * sentir, o que vira miragem e, principalmente, que o mundo de verdade não muda.
 */
public class TestesDoVeu {
	/** Uma tocha, uma porta fechada e um vidro atrás do jogador: o que o Véu tem para trocar. */
	private static BlockPos[] montarCasa(ServerLevel level, ServerPlayer jogador) {
		Vec3 olhar = jogador.getLookAngle();
		int dx = (int) Math.round(-olhar.x);
		int dz = (int) Math.round(-olhar.z);
		BlockPos pe = jogador.blockPosition();
		BlockPos tocha = pe.offset(dx * 3, 0, dz * 3);
		BlockPos porta = pe.offset(dx * 4 + dz, 0, dz * 4 + dx);
		BlockPos vidro = pe.offset(dx * 4 - dz * 2, 1, dz * 4 - dx * 2);
		level.setBlockAndUpdate(tocha, Blocks.TORCH.defaultBlockState());
		BlockState baixo = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
		level.setBlock(porta, baixo, 3);
		level.setBlock(porta.above(), baixo.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
		level.setBlockAndUpdate(vidro.below(), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(vidro, Blocks.GLASS.defaultBlockState());
		return new BlockPos[] {tocha, porta, vidro};
	}

	private static void desmontar(ServerLevel level, BlockPos[] casa) {
		level.removeBlock(casa[0], false);
		level.removeBlock(casa[1].above(), false);
		level.removeBlock(casa[1], false);
		level.removeBlock(casa[2], false);
		level.removeBlock(casa[2].below(), false);
	}

	/** Abre, muda o que ele sente, troca blocos só por miragem, e fecha sozinho deixando tudo como estava. */
	@GameTest(maxTicks = 1500)
	public void oVeuAbreEFechaSozinho(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1100, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		ServerLevel level = helper.getLevel();
		BlockPos[] casa = montarCasa(level, jogador);
		boolean[] abriu = new boolean[1];

		helper.runAfterDelay(5, () -> {
			String falha = Diretor.abrirVeuParaTeste(jogador);
			helper.assertTrue(falha == null, "o Véu deveria abrir: " + falha);
		});
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(Diretor.veuAberto(jogador), "meio segundo depois da piscada o Véu deveria estar aberto");
			PacoteSentidos s = Diretor.sentidos(jogador);
			helper.assertTrue(s.tem(PacoteSentidos.FLAG_VEU) && s.tem(PacoteSentidos.FLAG_AVESSO) && s.tem(PacoteSentidos.FLAG_SEM_MUSICA),
					"o cliente deveria ter sido mandado esconder as entidades, trocar a cor e cortar a música: flags=" + s.flags());
			helper.assertTrue(s.neblina() >= 0.8F && s.peso() >= 0.85F, "neblina e peso deveriam estar no alto: " + s);
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "VEU") == 3, "porta (duas metades) e vidro deveriam ser miragem, são "
					+ Diretor.miragensParaTeste(jogador, "VEU"));
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "LUZ_APAGADA") == 1, "a tocha deveria aparecer apagada");
			// O mundo de verdade não muda.
			helper.assertTrue(level.getBlockState(casa[0]).is(Blocks.TORCH), "a tocha de verdade continua lá");
			helper.assertFalse(level.getBlockState(casa[1]).getValue(BlockStateProperties.OPEN), "a porta de verdade continua fechada");
			helper.assertTrue(level.getBlockState(casa[2]).is(Blocks.GLASS), "o vidro de verdade continua lá");
			abriu[0] = true;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(abriu[0], "o Véu ainda não foi conferido aberto");
			helper.assertFalse(Diretor.veuAberto(jogador), "o Véu ainda está aberto");
			PacoteSentidos s = Diretor.sentidos(jogador);
			helper.assertFalse(s.tem(PacoteSentidos.FLAG_VEU) || s.tem(PacoteSentidos.FLAG_AVESSO), "as marcas do Véu deveriam ter saído: flags=" + s.flags());
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "VEU") == 0 && Diretor.miragensParaTeste(jogador, "LUZ_APAGADA") == 0,
					"as miragens deveriam ter sido desfeitas");
			helper.assertTrue(Diretor.criatura(jogador) == null || Diretor.criatura(jogador).isRemoved(), "o vulto do Véu deveria ter ido embora com ele");
			helper.assertTrue(Diretor.abrirVeuParaTeste(jogador) == null, "fechado, deveria poder abrir de novo por comando");
			desmontar(level, casa);
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Acender a Vela Pálida rasga o Véu na hora. */
	@GameTest(maxTicks = 300)
	public void aVelaRasgaOVeu(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1200, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		ServerLevel level = helper.getLevel();
		BlockPos[] casa = montarCasa(level, jogador);
		helper.runAfterDelay(5, () -> helper.assertTrue(Diretor.abrirVeuParaTeste(jogador) == null, "o Véu deveria abrir"));
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(Diretor.veuAberto(jogador), "deveria estar aberto antes da vela");
			Diretor.acenderVela(jogador);
		});
		helper.runAfterDelay(100, () -> {
			helper.assertFalse(Diretor.veuAberto(jogador), "com a vela acesa o Véu deveria ter fechado");
			helper.assertFalse(Diretor.sentidos(jogador).tem(PacoteSentidos.FLAG_VEU), "as marcas do Véu deveriam ter saído");
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "VEU") == 0 && Diretor.miragensParaTeste(jogador, "LUZ_APAGADA") == 0,
					"as luzes e os blocos deveriam ter voltado");
			desmontar(level, casa);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
