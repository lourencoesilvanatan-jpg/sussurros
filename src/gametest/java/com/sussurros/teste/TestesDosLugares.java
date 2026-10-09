package com.sussurros.teste;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DoorBlock;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.CasaDoVigia;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.bloco.TigelaOferendaBlock;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

/** O que o mod ergue no mundo: a porta sozinha e a figura de palha. */
public class TestesDosLugares {
	private static BlockPos lido(ServerPlayer jogador, String prefixo) {
		Memoria m = Memoria.de(jogador);
		return new BlockPos(m.get(prefixo + "_x"), m.get(prefixo + "_y"), m.get(prefixo + "_z"));
	}

	/** A porta é erguida inteira; a terceira travessia abre o Véu e a deixa descansando. */
	@GameTest(maxTicks = 400)
	public void aTerceiraTravessiaAbreOVeu(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1300, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		ServerLevel level = helper.getLevel();
		BlockPos[] porta = new BlockPos[1];

		helper.runAfterDelay(5, () -> {
			helper.assertTrue(Diretor.testarLugar(jogador, "soleira").startsWith("Soleira erguida"), "deveria erguer a soleira");
			porta[0] = lido(jogador, "soleira");
			helper.assertTrue(level.getBlockState(porta[0]).getBlock() instanceof DoorBlock
					&& level.getBlockState(porta[0].above()).getBlock() instanceof DoorBlock, "as duas metades da porta deveriam existir");
			helper.assertTrue(level.getBlockState(porta[0].above(2)).is(Blocks.STONE_BRICKS) || level.getBlockState(porta[0].above(2)).is(Blocks.MOSSY_STONE_BRICKS)
					|| level.getBlockState(porta[0].above(2)).is(Blocks.CRACKED_STONE_BRICKS), "deveria haver uma verga de pedra sobre a porta");
		});
		// O jogador de mentira não anda: é posto de um lado e do outro, com mais de um segundo entre um e outro.
		for (int i = 0; i < 4; i++) {
			int lado = i % 2 == 0 ? -2 : 2;
			int n = i;
			helper.runAfterDelay(40 + i * 40, () -> {
				jogador.snapTo(porta[0].getX() + 0.5, porta[0].getY(), porta[0].getZ() + 0.5 + lado, 0.0F, 0.0F);
				if (n == 2) {
					helper.assertTrue(Memoria.de(jogador).get("soleira_travessias") == 1, "depois da primeira travessia o contador deveria ser 1, é "
							+ Memoria.de(jogador).get("soleira_travessias"));
					helper.assertFalse(Diretor.veuAberto(jogador), "uma travessia só não abre nada");
				}
			});
		}
		helper.runAfterDelay(40 + 4 * 40 + 30, () -> {
			helper.assertTrue(Diretor.veuAberto(jogador), "na terceira travessia o Véu deveria abrir");
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("soleira_travessias") == 0 && m.get("veus") == 1, "o contador zera e o Véu conta: travessias="
					+ m.get("soleira_travessias") + " veus=" + m.get("veus"));
			for (BlockPos pos : BlockPos.betweenClosed(porta[0].offset(-1, -3, -1), porta[0].offset(1, 2, 1))) {
				if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && !level.getBlockState(pos).is(Blocks.DIRT)
						&& !level.getBlockState(pos).is(Blocks.BEDROCK)) {
					level.removeBlock(pos.immutable(), false);
				}
			}
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** A casa é erguida inteira, guarda o que ele deixou, e alguma coisa muda cada vez que o jogador vai embora. */
	@GameTest(maxTicks = 400)
	public void aCasaMudaQuandoEleVaiEmbora(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2000, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		ServerLevel level = helper.getLevel();
		BlockPos[] c = new BlockPos[1];

		helper.runAfterDelay(5, () -> {
			helper.assertTrue(Diretor.testarLugar(jogador, "casa").startsWith("Casa erguida"), "deveria erguer a casa");
			c[0] = lido(jogador, "casa_vigia");
			BlockPos o = c[0];
			helper.assertTrue(level.getBlockState(o.offset(0, 0, 2)).getBlock() instanceof DoorBlock, "porta ao sul");
			helper.assertTrue(level.getBlockState(o.offset(-2, 0, -1)).getBlock() instanceof BedBlock
					&& level.getBlockState(o.offset(-1, 0, -1)).getBlock() instanceof BedBlock, "a cama, inteira");
			boolean temCaixa = false;
			Container bau = (Container) level.getBlockEntity(o.offset(2, 0, -1));
			for (int i = 0; i < bau.getContainerSize(); i++) {
				temCaixa |= bau.getItem(i).is(ModItems.CAIXA_DE_MUSICA);
			}
			helper.assertTrue(temCaixa, "o baú deveria guardar a caixa de música");
			helper.assertTrue(level.getBlockState(o.offset(2, 0, 1)).getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.CINZAS,
					"a tigela com cinzas");
			BlockState lampiao = level.getBlockState(o.offset(0, 2, 0));
			helper.assertTrue(lampiao.is(ModBlocos.LAMPIAO_PALIDO) && lampiao.getValue(LampiaoPalidoBlock.CHAMA) == LampiaoPalidoBlock.Chama.APAGADA
					&& lampiao.getValue(LanternBlock.HANGING), "o lampião apagado, pendurado no teto");
			helper.assertTrue(CinzaEspalhadaBlock.estadoEm(level, o.offset(0, 0, 1)) == CinzaEspalhadaBlock.Estado.ROMPIDA, "a linha rompida diante da porta");
			helper.assertTrue(level.getBlockState(o.offset(2, 0, 4)).getValue(CampfireBlock.LIT), "a fogueira acesa lá fora");
			helper.assertTrue(CasaDoVigia.riscosNaPlaca(level, o) == 37, "trinta e sete riscos na placa, há " + CasaDoVigia.riscosNaPlaca(level, o));
			jogador.snapTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(Memoria.de(jogador).get("casa_vigia_visitas") == 1, "entrar conta uma visita");
			helper.assertTrue(level.getBlockState(c[0].offset(2, 0, 4)).getValue(CampfireBlock.LIT), "com ele lá dentro nada muda");
			jogador.snapTo(c[0].getX() + 60.5, c[0].getY(), c[0].getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.runAfterDelay(100, () -> {
			helper.assertFalse(level.getBlockState(c[0].offset(2, 0, 4)).getValue(CampfireBlock.LIT), "depois da primeira visita a fogueira deveria estar fria");
			helper.assertTrue(level.getBlockState(c[0].offset(0, 0, 2)).getValue(DoorBlock.OPEN), "e a porta, aberta");
			helper.assertTrue(CasaDoVigia.riscosNaPlaca(level, c[0]) == 38, "um risco a mais na placa");
			jogador.snapTo(c[0].getX() + 0.5, c[0].getY(), c[0].getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.runAfterDelay(150, () -> {
			helper.assertTrue(Memoria.de(jogador).get("casa_vigia_visitas") == 2, "voltar conta a segunda visita");
			jogador.snapTo(c[0].getX() + 60.5, c[0].getY(), c[0].getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.runAfterDelay(200, () -> {
			BlockState lampiao = level.getBlockState(c[0].offset(0, 2, 0));
			helper.assertTrue(lampiao.getValue(LampiaoPalidoBlock.CHAMA) != LampiaoPalidoBlock.Chama.APAGADA, "depois da segunda visita o lampião deveria estar aceso");
			helper.assertTrue(level.getBlockState(c[0].offset(2, 0, 1)).getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.VAZIA,
					"e a tigela, limpa");
			helper.assertTrue(CasaDoVigia.riscosNaPlaca(level, c[0]) == 39, "mais um risco");
			((Container) level.getBlockEntity(c[0].offset(2, 0, -1))).clearContent();
			for (BlockPos pos : BlockPos.betweenClosed(c[0].offset(-3, 0, -2), c[0].offset(3, 3, 4))) {
				level.removeBlock(pos.immutable(), false);
			}
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** A figura de palha chega mais perto a cada noite, olha para a cama, e derrubá-la não a impede de voltar. */
	@GameTest(maxTicks = 200)
	public void oBonecoChegaMaisPerto(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1400, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		ServerLevel level = helper.getLevel();
		BlockPos cama = jogador.blockPosition();
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.TEM_CAMA, 1);
		m.set(Memoria.CAMA_X, cama.getX());
		m.set(Memoria.CAMA_Y, cama.getY());
		m.set(Memoria.CAMA_Z, cama.getZ());
		m.salvar();

		helper.runAfterDelay(5, () -> {
			double anterior = Double.MAX_VALUE;
			BlockPos velho = null;
			for (int noite = 1; noite <= 4; noite++) {
				String feito = Diretor.testarLugar(jogador, "boneco");
				helper.assertTrue(feito.contains("ANDOU"), "na noite " + noite + " ele deveria andar: " + feito);
				BlockPos pos = lido(jogador, "boneco");
				helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_FENCE) && level.getBlockState(pos.above()).is(Blocks.HAY_BLOCK)
						&& level.getBlockState(pos.above(2)).is(Blocks.CARVED_PUMPKIN), "a figura deveria estar inteira na noite " + noite);
				Direction rosto = level.getBlockState(pos.above(2)).getValue(CarvedPumpkinBlock.FACING);
				Direction paraACama = Direction.getApproximateNearest(cama.getX() - pos.getX(), 0.0, cama.getZ() - pos.getZ());
				helper.assertTrue(rosto == paraACama, "o rosto deveria estar virado para a cama: " + rosto + " em vez de " + paraACama);
				double dist = Math.sqrt(pos.distSqr(cama));
				helper.assertTrue(dist < anterior - 4, "deveria estar mais perto que na noite anterior: " + dist + " contra " + anterior);
				if (velho != null) {
					helper.assertTrue(level.getBlockState(velho).isAir() && level.getBlockState(velho.above(2)).isAir(), "o lugar de ontem deveria estar vazio");
				}
				anterior = dist;
				velho = pos;
				if (noite == 2) {
					// Derrubado pelo jogador: na noite seguinte está de pé, um passo adiante.
					level.removeBlock(pos.above(2), false);
					level.removeBlock(pos.above(), false);
					level.removeBlock(pos, false);
				}
			}
			BlockPos ultimo = lido(jogador, "boneco");
			level.removeBlock(ultimo.above(2), false);
			level.removeBlock(ultimo.above(), false);
			level.removeBlock(ultimo, false);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
