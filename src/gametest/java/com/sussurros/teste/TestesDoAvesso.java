package com.sussurros.teste;

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
