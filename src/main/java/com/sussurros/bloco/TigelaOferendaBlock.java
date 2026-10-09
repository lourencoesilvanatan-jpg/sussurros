package com.sussurros.bloco;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.sussurros.assombracao.Oferenda;
import com.sussurros.registro.ModItems;
import com.sussurros.registro.ModSons;

/**
 * Tigela de Oferenda (0.9). O jogador deixa um item nela; à noite, quando ninguém olha, ele pode aceitar.
 *
 * O bloco só guarda e mostra. Quem decide se a oferenda é aceita, o que isso compra e o que ele deixa em
 * troca é {@link Oferenda}. De manhã a tigela conta o que houve sem uma palavra:
 *  - CHEIA com o mesmo item: ele recusou (ou não veio);
 *  - CINZAS: aceitou;
 *  - CHEIA com outra coisa: ele deixou algo.
 */
public class TigelaOferendaBlock extends Block implements EntityBlock {
	public enum Conteudo implements StringRepresentable {
		VAZIA, CHEIA, CINZAS;

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	public static final MapCodec<TigelaOferendaBlock> CODEC = simpleCodec(TigelaOferendaBlock::new);
	public static final EnumProperty<Conteudo> CONTEUDO = EnumProperty.create("conteudo", Conteudo.class);
	private static final VoxelShape FORMA = Block.box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);

	public TigelaOferendaBlock(Properties propriedades) {
		super(propriedades);
		this.registerDefaultState(this.stateDefinition.any().setValue(CONTEUDO, Conteudo.VAZIA));
	}

	@Override
	protected MapCodec<? extends TigelaOferendaBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONTEUDO);
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new TigelaBlockEntity(pos, estado);
	}

	/** Com um item na mão e a tigela vazia: deixa um na tigela. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState estado, Level level, BlockPos pos, Player jogador,
			InteractionHand mao, BlockHitResult acerto) {
		if (estado.getValue(CONTEUDO) != Conteudo.VAZIA || stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel servidor) || !(level.getBlockEntity(pos) instanceof TigelaBlockEntity tigela)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack oferecido = stack.copyWithCount(1);
		stack.consume(1, jogador);
		tigela.guardar(servidor, oferecido, estado);
		ModSons.tocar(servidor, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, ModSons.Som.TIGELA, 0.6F, 1.0F);
		if (jogador instanceof ServerPlayer p) {
			Oferenda.aoOferecer(p, pos, oferecido);
		}
		return InteractionResult.SUCCESS;
	}

	/** Mão vazia: pega de volta o que está na tigela, ou limpa as cinzas. */
	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jogador, BlockHitResult acerto) {
		if (!(level instanceof ServerLevel servidor) || !(level.getBlockEntity(pos) instanceof TigelaBlockEntity tigela)) {
			return InteractionResult.SUCCESS;
		}
		switch (estado.getValue(CONTEUDO)) {
			case CHEIA -> {
				ItemStack item = tigela.esvaziar(servidor, estado, Conteudo.VAZIA);
				if (!item.isEmpty() && !jogador.addItem(item)) {
					jogador.drop(item, false);
				}
			}
			case CINZAS -> {
				tigela.esvaziar(servidor, estado, Conteudo.VAZIA);
				// Às vezes sobra cinza de verdade no fundo. Nem sempre: senão a tigela vira fonte de cinza.
				if (level.getRandom().nextFloat() < 0.35F) {
					ItemStack cinza = new ItemStack(ModItems.CINZA_PALIDA);
					if (!jogador.addItem(cinza)) {
						jogador.drop(cinza, false);
					}
				}
			}
			default -> {
				return InteractionResult.PASS;
			}
		}
		level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.5F, 0.8F);
		return InteractionResult.SUCCESS;
	}
}
