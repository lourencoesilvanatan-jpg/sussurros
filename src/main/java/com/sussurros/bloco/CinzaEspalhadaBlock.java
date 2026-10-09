package com.sussurros.bloco;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cinza Pálida espalhada no chão (0.9). É dois objetos num bloco só:
 *
 *  - a LINHA que o jogador faz ao usar Cinza Pálida no chão. O Hóspede não cruza uma linha que ainda segura.
 *    Cada tentativa dele a desgasta um estágio (intacta, riscada, gasta) e, na terceira, ela se rompe.
 *    De manhã, a linha riscada é a prova de que ele esteve ali sem ninguém ter visto nada;
 *  - a PEGADA que ele deixa. Não segura nada; é só a marca.
 *
 * Só a linha intacta devolve a Cinza ao ser quebrada, para o que ele gasta não virar fazenda de recurso.
 */
public class CinzaEspalhadaBlock extends Block {
	public enum Estado implements StringRepresentable {
		INTACTA, RISCADA, GASTA, ROMPIDA, PEGADA;

		/** Esta linha ainda impede a passagem dele? */
		public boolean segura() {
			return this == INTACTA || this == RISCADA || this == GASTA;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	public static final MapCodec<CinzaEspalhadaBlock> CODEC = simpleCodec(CinzaEspalhadaBlock::new);
	public static final EnumProperty<Estado> ESTADO = EnumProperty.create("estado", Estado.class);
	/**
	 * Para onde a marca está virada. Na linha, a faixa atravessa essa direção (virada para o norte ou para o sul,
	 * corre de oeste a leste). Na pegada, é para onde os dedos apontam.
	 */
	public static final EnumProperty<Direction> FRENTE = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape FORMA = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

	public CinzaEspalhadaBlock(Properties propriedades) {
		super(propriedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(ESTADO, Estado.INTACTA).setValue(FRENTE, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends CinzaEspalhadaBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ESTADO, FRENTE);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
		BlockPos baixo = pos.below();
		return level.getBlockState(baixo).isFaceSturdy(level, baixo, Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState estado, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direcao, BlockPos vizinho, BlockState estadoVizinho, RandomSource random) {
		return estado.canSurvive(level, pos) ? estado : Blocks.AIR.defaultBlockState();
	}

	/**
	 * Para onde virar uma linha nova: ela continua a linha vizinha, se há vizinhas numa direção só; senão
	 * atravessa na frente de quem a faz.
	 */
	public static Direction frentePara(BlockGetter level, BlockPos pos, Direction olhando) {
		boolean lesteOeste = estadoEm(level, pos.east()) != null || estadoEm(level, pos.west()) != null;
		boolean norteSul = estadoEm(level, pos.north()) != null || estadoEm(level, pos.south()) != null;
		if (lesteOeste != norteSul && (olhando.getAxis() == Direction.Axis.X) == lesteOeste) {
			return olhando.getClockWise();
		}
		return olhando;
	}

	/** O estado desta posição, se houver cinza espalhada ali. */
	@Nullable
	public static Estado estadoEm(BlockGetter level, BlockPos pos) {
		BlockState s = level.getBlockState(pos);
		return s.getBlock() instanceof CinzaEspalhadaBlock ? s.getValue(ESTADO) : null;
	}

	/**
	 * Uma tentativa dele de passar. A linha perde um estágio. Devolve true se havia uma linha segurando ali
	 * (mesmo que esta tentativa a tenha rompido): ou seja, se ele foi barrado desta vez.
	 */
	public static boolean desgastar(ServerLevel level, BlockPos pos) {
		BlockState s = level.getBlockState(pos);
		if (!(s.getBlock() instanceof CinzaEspalhadaBlock) || !s.getValue(ESTADO).segura()) {
			return false;
		}
		Estado proximo = switch (s.getValue(ESTADO)) {
			case INTACTA -> Estado.RISCADA;
			case RISCADA -> Estado.GASTA;
			default -> Estado.ROMPIDA;
		};
		level.setBlock(pos, s.setValue(ESTADO, proximo), 3);
		return true;
	}

	/** Uma linha que ainda segura, a até "raio" blocos (no plano) e um de altura. Null se não houver. */
	@Nullable
	public static BlockPos linhaPerto(BlockGetter level, BlockPos centro, int raio) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -raio; dx <= raio; dx++) {
				for (int dz = -raio; dz <= raio; dz++) {
					BlockPos pos = centro.offset(dx, dy, dz);
					Estado e = estadoEm(level, pos);
					if (e != null && e.segura()) {
						return pos;
					}
				}
			}
		}
		return null;
	}
}
