package com.sussurros.bloco;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Vela Pálida acesa, no chão (0.9). Antes a vela era só um cronômetro invisível; agora, enquanto a zona de
 * calma dura, há uma vela de verdade ali, com chama. Quando o tempo acaba ela some; se alguém a quebra, ou se
 * ele a sopra, a zona acaba junto (ver Diretor).
 *
 * Usa os modelos da vela branca do próprio jogo, então não tem textura própria. Não solta nada ao quebrar.
 */
public class VelaAcesaBlock extends AbstractCandleBlock {
	public static final MapCodec<VelaAcesaBlock> CODEC = simpleCodec(VelaAcesaBlock::new);
	private static final VoxelShape FORMA = Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0);
	private static final List<Vec3> CHAMA = List.of(new Vec3(0.5, 0.5, 0.5));

	public VelaAcesaBlock(Properties propriedades) {
		super(propriedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
	}

	@Override
	protected MapCodec<? extends VelaAcesaBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected Iterable<Vec3> getParticleOffsets(BlockState estado) {
		return CHAMA;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState estado, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direcao, BlockPos vizinho, BlockState estadoVizinho, RandomSource random) {
		return estado.canSurvive(level, pos) ? estado : Blocks.AIR.defaultBlockState();
	}
}
