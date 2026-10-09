package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Avesso;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.entidade.HospedeEntity;

/**
 * A dimensão, até onde o servidor de teste alcança. Ele não carrega dimensões de pacotes de dados (o próprio
 * jogo as deixa de fora no GameTestServer), então ir e voltar é conferido no teste de cliente, que abre um
 * mundo de verdade. Aqui ficam as regras da cópia e o que acontece quando a dimensão não existe.
 */
public class TestesDoAvesso {
	/** O que é copiado para o outro lado fica sem luz e sem vida; o resto fica igual. */
	@GameTest
	public void oQueSeCopiaFicaApagado(GameTestHelper helper) {
		helper.assertTrue(Avesso.apagado(Blocks.TORCH.defaultBlockState()).isAir(), "tocha some");
		helper.assertTrue(Avesso.apagado(Blocks.LANTERN.defaultBlockState()).isAir(), "lanterna some");
		helper.assertTrue(Avesso.apagado(Blocks.FIRE.defaultBlockState()).isAir(), "fogo some");
		helper.assertTrue(Avesso.apagado(Blocks.SPAWNER.defaultBlockState()).isAir(), "gerador de monstros some");
		helper.assertTrue(Avesso.apagado(Blocks.GLOWSTONE.defaultBlockState()).is(Blocks.COBBLED_DEEPSLATE), "bloco de luz vira pedra");
		helper.assertTrue(Avesso.apagado(Blocks.LAVA.defaultBlockState()).is(Blocks.OBSIDIAN), "lava parada vira obsidiana");
		BlockState fornalha = Avesso.apagado(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
		helper.assertTrue(fornalha.is(Blocks.FURNACE) && !fornalha.getValue(BlockStateProperties.LIT), "fornalha acesa fica apagada");
		BlockState fogueira = Avesso.apagado(Blocks.CAMPFIRE.defaultBlockState());
		helper.assertTrue(fogueira.is(Blocks.CAMPFIRE) && !fogueira.getValue(BlockStateProperties.LIT), "fogueira fica apagada");
		helper.assertTrue(Avesso.apagado(Blocks.OAK_DOOR.defaultBlockState()).getValue(BlockStateProperties.OPEN), "porta de madeira fica aberta");
		helper.assertFalse(Avesso.apagado(Blocks.IRON_DOOR.defaultBlockState()).getValue(BlockStateProperties.OPEN), "porta de ferro fica como está");
		helper.assertTrue(Avesso.apagado(Blocks.CHEST.defaultBlockState()).is(Blocks.CHEST), "baú continua baú");
		helper.assertTrue(Avesso.apagado(Blocks.GOLD_BLOCK.defaultBlockState()).is(Blocks.GOLD_BLOCK), "bloco comum fica igual");
		helper.assertTrue(Avesso.apagado(Blocks.WATER.defaultBlockState()).is(Blocks.WATER), "água fica igual");
		for (BlockState s : new BlockState[] {Blocks.TORCH.defaultBlockState(), Blocks.GLOWSTONE.defaultBlockState(), Blocks.LAVA.defaultBlockState(),
				Blocks.SEA_LANTERN.defaultBlockState(), Blocks.JACK_O_LANTERN.defaultBlockState(), Blocks.REDSTONE_TORCH.defaultBlockState()}) {
			helper.assertTrue(Avesso.apagado(s).getLightEmission() == 0, "nada do que é copiado dá luz: " + s);
		}
		helper.succeed();
	}

	/** Até a segunda visita a cópia vem do mesmo lugar; da terceira em diante há uma fatia repetida a leste. */
	@GameTest
	public void aMedidaErradaDaTerceiraVisita(GameTestHelper helper) {
		BlockPos ancora = new BlockPos(100, 64, -40);
		for (int visita = 1; visita <= 2; visita++) {
			for (int dx = -14; dx <= 14; dx++) {
				BlockPos pos = ancora.offset(dx, 1, 3);
				helper.assertTrue(Avesso.fonte(pos, ancora, visita).equals(pos), "na visita " + visita + " cada bloco vem do seu lugar");
			}
		}
		for (int dx = -14; dx <= 2; dx++) {
			BlockPos pos = ancora.offset(dx, 0, -5);
			helper.assertTrue(Avesso.fonte(pos, ancora, 3).equals(pos), "na terceira, até dois blocos a leste nada muda (dx=" + dx + ")");
		}
		helper.assertTrue(Avesso.fonte(ancora.offset(3, 0, 0), ancora, 3).equals(ancora.offset(2, 0, 0)), "a fatia a três blocos repete a de dois");
		helper.assertTrue(Avesso.fonte(ancora.offset(9, 2, 4), ancora, 3).equals(ancora.offset(8, 2, 4)), "e tudo depois está um bloco mais longe");
		helper.succeed();
	}

	/** Como ele é do outro lado: ser olhado não o faz sumir; quando vem, vem até encostar. */
	@GameTest(maxTicks = 700)
	public void doOutroLadoEleNaoSomeEVem(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1700, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		BlockPos pe = jogador.blockPosition();
		HospedeEntity[] ele = new HospedeEntity[1];
		double[] distInicial = new double[1];

		helper.runAfterDelay(5, () -> {
			helper.assertTrue(Avesso.criaturaParaTeste(jogador, false), "deveria haver lugar para ele");
			ele[0] = Diretor.criatura(jogador);
			distInicial[0] = ele[0].distanceTo(jogador);
			// O jogador vira o rosto para ele e fica olhando.
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, ele[0].getEyePosition());
		});
		helper.runAfterDelay(85, () -> {
			helper.assertFalse(ele[0].isRemoved(), "quatro segundos encarado, ele deveria continuar lá");
			helper.assertTrue(Math.abs(ele[0].distanceTo(jogador) - distInicial[0]) < 0.5, "parado, não deveria ter saído do lugar");
			ele[0].virNoAvesso();
		});
		helper.runAfterDelay(125, () -> helper.assertTrue(ele[0].isRemoved() || ele[0].distanceTo(jogador) < distInicial[0] - 2.0,
				"dois segundos depois de começar a vir, deveria estar mais perto mesmo sendo olhado"));
		helper.succeedWhen(() -> {
			helper.assertTrue(ele[0] != null && ele[0].isRemoved(), "ele ainda não encostou");
			helper.assertTrue(jogador.level().dimension() == Level.OVERWORLD && jogador.blockPosition().equals(pe),
					"fora da dimensão, encostar não leva ninguém a lugar nenhum");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Num mundo sem a dimensão, ninguém é levado e ninguém fica marcado como "lá dentro". */
	@GameTest(maxTicks = 100)
	public void semADimensaoNinguemFicaPreso(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1500, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		helper.assertTrue(helper.getLevel().getServer().getLevel(Avesso.DIMENSAO) == null,
				"o servidor de teste passou a carregar a dimensão: mova para cá os testes de ir e voltar");
		helper.runAfterDelay(5, () -> {
			String resposta = Diretor.testarAvesso(jogador, true);
			helper.assertTrue(resposta.contains("não existe"), "sem a dimensão o comando deveria dizer isso: " + resposta);
		});
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(jogador.level().dimension() == Level.OVERWORLD, "deveria continuar no mundo normal");
			helper.assertTrue(Memoria.de(jogador).get("avesso_dentro") == 0, "não deveria estar marcado como lá dentro");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
