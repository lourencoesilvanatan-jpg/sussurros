package com.sussurros.registro;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.sussurros.Sussurros;
import com.sussurros.entidade.HospedeEntity;

public class ModEntidades {
	public static final ResourceKey<EntityType<?>> HOSPEDE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Sussurros.id("hospede"));

	public static final EntityType<HospedeEntity> HOSPEDE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			HOSPEDE_KEY,
			EntityType.Builder.<HospedeEntity>of(HospedeEntity::new, MobCategory.MISC)
					.sized(0.7f, 3.0f)        // largura e altura da caixa de colisão (em blocos)
					.clientTrackingRange(10)  // visível de longe (10 chunks)
					.fireImmune()
					.build(HOSPEDE_KEY)
	);

	public static void inicializar() {
		FabricDefaultAttributeRegistry.register(HOSPEDE, HospedeEntity.criarAtributos());
	}
}
