package com.sussurros.teste;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.AlguemEsteveAqui;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;

/**
 * Alguém esteve aqui (0.9.0-alpha28): o que é posto de verdade, e fica, na caverna e perto da casa. Os testes
 * conferem as três coisas que não podem falhar: nunca à vista do jogador, nunca em cima de nada que é dele (na casa,
 * todo bloco dela é comparado antes e depois), e na caverna só onde ele ainda não andou e só pedra natural cavada.
 */
public class TestesDeAlguem {
	/** Um maciço de pedra com um túnel em L: doze blocos para leste, depois vinte e quatro para o sul. O jogador no começo. */
	private static BlockPos tunelEmL(ServerLevel level, BlockPos pes) {
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(-4, -1, -4), pes.offset(18, 5, 30))) {
			level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
		}
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(0, 0, 0), pes.offset(14, 2, 2))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(12, 0, 2), pes.offset(14, 2, 28))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
		return pes;
	}

	private static Map<BlockPos, BlockState> fotografar(ServerLevel level, BlockPos de, BlockPos ate) {
		Map<BlockPos, BlockState> foto = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(de, ate)) {
			foto.put(pos.immutable(), level.getBlockState(pos));
		}
		return foto;
	}

	private static boolean enxerga(ServerLevel level, ServerPlayer p, BlockPos c) {
		return level.clip(new ClipContext(p.getEyePosition(), Vec3.atCenterOf(c), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
				.getType() == HitResult.Type.MISS;
	}

	/**
	 * No túnel em L, com o jogador no começo do braço de leste: as tochas vão para o braço do sul, que ele não vê e por
	 * onde não andou, a catorze blocos ou mais, presas na parede, e nada além delas muda.
	 */
	@GameTest(maxTicks = 200)
	public void naCavernaOTrechoQueEleNaoAndouAmanheceAceso(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = tunelEmL(level, JogadorDeTeste.chao(helper, 4, -10200));
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes.offset(1, 0, 1));
		jogador.setYRot(-90.0F); // olhando para o leste, ao longo do braço
		helper.runAfterDelay(30, () -> {
			Map<BlockPos, BlockState> antes = fotografar(level, pes.offset(-4, -1, -4), pes.offset(18, 5, 30));
			List<BlockPos> postas = AlguemEsteveAqui.naCavernaParaTeste(jogador, false);
			helper.assertTrue(postas.size() >= 2, "deveria ter acendido o braço do sul; pôs " + postas.size());
			for (BlockPos t : postas) {
				BlockState s = level.getBlockState(t);
				helper.assertTrue(s.is(Blocks.WALL_TORCH), "tocha de parede em " + t.toShortString() + ", achei " + s);
				helper.assertTrue(antes.get(t).isAir(), "a tocha só vai onde era ar");
				helper.assertTrue(t.getZ() - pes.getZ() >= 3, "tocha no braço que ele vê: " + t.subtract(pes).toShortString());
				helper.assertTrue(jogador.distanceToSqr(Vec3.atCenterOf(t)) >= 14 * 14, "tocha perto demais: " + t.subtract(pes).toShortString());
				helper.assertFalse(enxerga(level, jogador, t), "tocha à vista do jogador: " + t.subtract(pes).toShortString());
			}
			int mudou = 0;
			for (Map.Entry<BlockPos, BlockState> antesDele : antes.entrySet()) {
				if (!level.getBlockState(antesDele.getKey()).equals(antesDele.getValue())) {
					mudou++;
				}
			}
			helper.assertTrue(mudou == postas.size(), "só as tochas deveriam mudar: mudaram " + mudou + " blocos para " + postas.size() + " tochas");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * A galeria: três blocos de fundo e dois de altura, cavados só em pedra, fora da vista, com uma tocha no fundo. O
	 * resto do maciço não muda.
	 */
	@GameTest(maxTicks = 200)
	public void aGaleriaSoCavaPedraEFicaFora(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = tunelEmL(level, JogadorDeTeste.chao(helper, 4, -10300));
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes.offset(1, 0, 1));
		jogador.setYRot(-90.0F);
		helper.runAfterDelay(30, () -> {
			Map<BlockPos, BlockState> antes = fotografar(level, pes.offset(-4, -1, -4), pes.offset(18, 5, 30));
			List<BlockPos> feito = AlguemEsteveAqui.naCavernaParaTeste(jogador, true);
			helper.assertTrue(feito.size() == 7, "a galeria são seis blocos cavados e uma tocha; foram " + feito.size());
			int cavados = 0;
			for (BlockPos b : feito) {
				BlockState agora = level.getBlockState(b);
				helper.assertTrue(antes.get(b).is(BlockTags.BASE_STONE_OVERWORLD), "só pedra natural é cavada: " + antes.get(b));
				helper.assertFalse(enxerga(level, jogador, b), "a galeria não pode ser cavada à vista");
				if (agora.isAir()) {
					cavados++;
				}
			}
			// A tocha ocupa o sexto (o de cima, no fundo): cinco de ar e ela.
			helper.assertTrue(cavados == 5, "cinco blocos de ar e a tocha; achei " + cavados + " de ar");
			helper.assertTrue(level.getBlockState(feito.get(feito.size() - 1)).is(Blocks.WALL_TORCH), "a tocha no fundo");
			int mudou = 0;
			for (Map.Entry<BlockPos, BlockState> antesDele : antes.entrySet()) {
				if (!level.getBlockState(antesDele.getKey()).equals(antesDele.getValue())) {
					mudou++;
				}
			}
			helper.assertTrue(mudou == 6, "fora a galeria (seis blocos, a tocha está num deles), nada muda: mudaram " + mudou);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Na caverna de verdade do dono (o pedaço exportado em 11/10, com as tochas dele): ele já a acendeu inteira (na
	 * primeira execução, das 110 células a dezesseis blocos ou mais, nenhuma estava escura). Então nada é posto, e nada
	 * da caverna muda: alguém só passa por onde ele ainda não passou.
	 */
	@GameTest(maxTicks = 300)
	public void naCavernaQueEleJaAcendeuNadaMuda(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = JogadorDeTeste.chao(helper, 4, -10400);
		Terreno.Montado caverna = Terreno.montar(level, "caverna", base.offset(-23, 0, -21));
		BlockPos entrada = TestesDePerto.emPe(level, caverna.em(11, 8, 19), 5, false);
		helper.assertTrue(entrada != null, "a caverna montada deveria ter chão perto da tocha");
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, entrada);
		helper.runAfterDelay(40, () -> {
			Map<BlockPos, BlockState> antes = fotografar(level, caverna.canto(),
					caverna.canto().offset(caverna.larguraX() - 1, caverna.altura() - 1, caverna.larguraZ() - 1));
			List<BlockPos> postas = AlguemEsteveAqui.naCavernaParaTeste(jogador, false);
			Sussurros.LOGGER.info("[alguém na caverna do dono] {} tocha(s)", postas.size());
			helper.assertTrue(postas.isEmpty(), "na caverna que ele já acendeu nada deveria ser posto; foram " + postas.size());
			for (Map.Entry<BlockPos, BlockState> b : antes.entrySet()) {
				helper.assertTrue(level.getBlockState(b.getKey()).equals(b.getValue()), "nada da caverna dele pode mudar");
			}
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Uma casinha de tábua com porta ao sul, janela de vidro a leste e cama dentro. Com o jogador a sessenta blocos, de
	 * costas: as três coisas da casa são postas do lado de fora, e nenhum bloco da casa (nem o ar em volta da porta)
	 * muda. Depois, ele chega perto da tocha do chão, e ela conta como achada.
	 */
	@GameTest(maxTicks = 400)
	public void pertoDaCasaNadaDeleMuda(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos chao = JogadorDeTeste.chao(helper, 4, -10500);
		BlockPos canto = casinha(level, chao);
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -10500, 0);
		Diretor.esquecer(jogador);
		BlockPos cama = canto.offset(2, 1, 2);
		BlockPos porta = canto.offset(3, 1, 6);
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.TEM_CAMA, 1);
		m.set(Memoria.CAMA_X, cama.getX());
		m.set(Memoria.CAMA_Y, cama.getY());
		m.set(Memoria.CAMA_Z, cama.getZ());
		m.set(Memoria.TEM_PORTA, 1);
		m.set(Memoria.PORTA_X, porta.getX());
		m.set(Memoria.PORTA_Y, porta.getY());
		m.set(Memoria.PORTA_Z, porta.getZ());
		m.salvar();
		// Sessenta blocos ao norte, olhando para o norte.
		jogador.snapTo(cama.getX() + 0.5, cama.getY() + 0.1, cama.getZ() - 60.5, 180.0F, 0.0F);
		BlockPos[] tocha = new BlockPos[1];
		helper.runAfterDelay(20, () -> {
			Map<BlockPos, BlockState> antes = fotografar(level, canto.offset(-1, 0, -1), canto.offset(7, 5, 8));
			BlockPos janela = AlguemEsteveAqui.naCasaParaTeste(jogador, 0);
			BlockPos noChao = AlguemEsteveAqui.naCasaParaTeste(jogador, 1);
			BlockPos bancada = AlguemEsteveAqui.naCasaParaTeste(jogador, 2);
			helper.assertTrue(janela != null && noChao != null && bancada != null,
					"as três da casa deveriam caber: janela=" + janela + " chao=" + noChao + " bancada=" + bancada);
			BlockState sJanela = level.getBlockState(janela);
			helper.assertTrue(sJanela.is(Blocks.WALL_TORCH) && level.canSeeSky(janela), "a tocha da janela fica do lado de fora: " + sJanela);
			BlockPos apoio = janela.relative(sJanela.getValue(WallTorchBlock.FACING).getOpposite());
			helper.assertTrue(level.getBlockState(apoio).is(Blocks.OAK_PLANKS), "ela se apoia na parede dele: " + level.getBlockState(apoio));
			helper.assertTrue(level.getBlockState(noChao).is(Blocks.TORCH), "a tocha fincada no chão");
			double daPorta = Math.sqrt(noChao.distSqr(porta.atY(noChao.getY())));
			helper.assertTrue(daPorta >= 3.0 && daPorta <= 9.0, "a tocha do chão perto da porta: a " + daPorta);
			helper.assertTrue(level.getBlockState(bancada).is(Blocks.CRAFTING_TABLE), "a bancada");
			boolean tochaAoLado = false;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				tochaAoLado |= level.getBlockState(bancada.relative(d)).is(Blocks.TORCH);
			}
			helper.assertTrue(tochaAoLado, "com uma tocha ao lado");
			for (Map.Entry<BlockPos, BlockState> b : antes.entrySet()) {
				BlockPos pos = b.getKey();
				if (pos.equals(janela) || pos.equals(noChao) || pos.equals(bancada)) {
					helper.assertTrue(b.getValue().isAir() || b.getValue().is(BlockTags.REPLACEABLE), "só onde era vazio");
					continue;
				}
				helper.assertTrue(level.getBlockState(pos).equals(b.getValue()) || b.getValue().is(BlockTags.REPLACEABLE),
						"nada da casa pode mudar: " + pos.subtract(canto).toShortString() + " era " + b.getValue() + " e é " + level.getBlockState(pos));
			}
			tocha[0] = noChao;
			// Ele volta e passa ao lado da tocha do chão.
			jogador.snapTo(noChao.getX() + 2.5, noChao.getY(), noChao.getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(AlguemEsteveAqui.achadoParaTeste(jogador, tocha[0]), "passando ao lado, ele achou a tocha");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Com o jogador olhando para a casa, a vinte blocos, nada é posto: tudo estaria à vista. */
	@GameTest(maxTicks = 200)
	public void comEleOlhandoNadaAparece(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos chao = JogadorDeTeste.chao(helper, 4, -10600);
		BlockPos canto = casinha(level, chao);
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -10600, 0);
		Diretor.esquecer(jogador);
		BlockPos cama = canto.offset(2, 1, 2);
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.TEM_CAMA, 1);
		m.set(Memoria.CAMA_X, cama.getX());
		m.set(Memoria.CAMA_Y, cama.getY());
		m.set(Memoria.CAMA_Z, cama.getZ());
		m.salvar();
		// Vinte blocos ao sul da porta, olhando para a casa (norte).
		jogador.snapTo(cama.getX() + 0.5, cama.getY() + 0.1, cama.getZ() + 22.5, 180.0F, 0.0F);
		helper.runAfterDelay(20, () -> {
			Map<BlockPos, BlockState> antes = fotografar(level, canto.offset(-10, -1, -10), canto.offset(16, 5, 16));
			helper.assertTrue(AlguemEsteveAqui.naCasaParaTeste(jogador, 1) == null, "com ele olhando, a tocha do chão não pode aparecer");
			for (Map.Entry<BlockPos, BlockState> b : antes.entrySet()) {
				helper.assertTrue(level.getBlockState(b.getKey()).equals(b.getValue()), "nada pode mudar com ele olhando");
			}
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Uma casinha de tábua de 7 por 7, três de altura, teto de tábua. Porta ao sul (no meio), uma janela de vidro a
	 * leste, a cama dentro. Devolve o canto de baixo (o chão do lado de dentro fica um acima dele).
	 */
	private static BlockPos casinha(ServerLevel level, BlockPos chao) {
		BlockPos canto = chao.below();
		for (BlockPos pos : BlockPos.betweenClosed(canto, canto.offset(6, 4, 6))) {
			int x = pos.getX() - canto.getX();
			int y = pos.getY() - canto.getY();
			int z = pos.getZ() - canto.getZ();
			boolean borda = x == 0 || x == 6 || z == 0 || z == 6;
			BlockState s = y == 0 || y == 4 || borda ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
			level.setBlock(pos, s, 2);
		}
		// Janela a leste, no meio da parede, na altura dos olhos.
		level.setBlock(canto.offset(6, 2, 3), Blocks.GLASS.defaultBlockState(), 2);
		// Porta ao sul.
		BlockState porta = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
		level.setBlock(canto.offset(3, 1, 6), porta.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
		level.setBlock(canto.offset(3, 2, 6), porta.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
		// A cama.
		BlockState cama = Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
		level.setBlock(canto.offset(2, 1, 2), cama.setValue(BedBlock.PART, BedPart.HEAD), 2);
		level.setBlock(canto.offset(2, 1, 3), cama.setValue(BedBlock.PART, BedPart.FOOT), 2);
		return canto;
	}
}
