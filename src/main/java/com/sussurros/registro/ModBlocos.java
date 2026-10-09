package com.sussurros.registro;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import com.sussurros.Sussurros;
import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.bloco.TigelaBlockEntity;
import com.sussurros.bloco.TigelaOferendaBlock;
import com.sussurros.bloco.VelaAcesaBlock;

/** Os blocos do mod (0.9). Os itens que os colocam ficam em {@link ModItems}. */
public final class ModBlocos {
	/** Cinza Pálida no chão: a linha que o jogador faz e a pegada que ele deixa. Não tem item próprio. */
	public static final Block CINZA_ESPALHADA = registrar("cinza_espalhada", CinzaEspalhadaBlock::new,
			BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion().replaceable()
					.sound(SoundType.SAND).pushReaction(PushReaction.DESTROY));

	public static final Block LAMPIAO_PALIDO = registrar("lampiao_palido", LampiaoPalidoBlock::new,
			BlockBehaviour.Properties.of().strength(2.0F).noOcclusion().sound(SoundType.LANTERN)
					.pushReaction(PushReaction.DESTROY).randomTicks()
					.lightLevel(estado -> estado.getValue(LampiaoPalidoBlock.CHAMA).luz));

	public static final Block TIGELA_OFERENDA = registrar("tigela_oferenda", TigelaOferendaBlock::new,
			BlockBehaviour.Properties.of().strength(0.8F).noOcclusion().sound(SoundType.DECORATED_POT));

	/** A Vela Pálida enquanto queima. Colocada e tirada pelo mod; não solta nada. */
	public static final Block VELA_ACESA = registrar("vela_acesa", VelaAcesaBlock::new,
			BlockBehaviour.Properties.of().instabreak().noOcclusion().noLootTable().sound(SoundType.CANDLE)
					.pushReaction(PushReaction.DESTROY).lightLevel(estado -> 9));

	public static final BlockEntityType<TigelaBlockEntity> TIGELA_ENTIDADE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Sussurros.id("tigela_oferenda"),
			FabricBlockEntityTypeBuilder.create(TigelaBlockEntity::new, TIGELA_OFERENDA).build());

	private ModBlocos() {
	}

	private static <T extends Block> T registrar(String nome, Function<BlockBehaviour.Properties, T> fabrica,
			BlockBehaviour.Properties propriedades) {
		ResourceKey<Block> chave = ResourceKey.create(Registries.BLOCK, Sussurros.id(nome));
		return Registry.register(BuiltInRegistries.BLOCK, chave, fabrica.apply(propriedades.setId(chave)));
	}

	/** Só garante que a classe carregue e os blocos sejam registrados. */
	public static void inicializar() {
	}
}
