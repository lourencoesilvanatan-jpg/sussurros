package com.sussurros.assombracao;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import com.sussurros.registro.ModItems;

/**
 * Esconde páginas do diário (e, raramente, o Olho) em baús de estruturas pelo mapa.
 */
public final class LootDoDiario {
	private LootDoDiario() {
	}

	public static void inicializar() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			adicionar(key, tableBuilder, BuiltInLootTables.SIMPLE_DUNGEON, 0.45F, 0.08F);
			adicionar(key, tableBuilder, BuiltInLootTables.ABANDONED_MINESHAFT, 0.30F, 0.05F);
			adicionar(key, tableBuilder, BuiltInLootTables.STRONGHOLD_LIBRARY, 0.70F, 0.20F);
			adicionar(key, tableBuilder, BuiltInLootTables.WOODLAND_MANSION, 0.50F, 0.15F);
			adicionar(key, tableBuilder, BuiltInLootTables.ANCIENT_CITY, 0.50F, 0.30F);
			adicionar(key, tableBuilder, BuiltInLootTables.SHIPWRECK_MAP, 0.35F, 0.0F);

			// Vestígios/contramedidas: continuam raros. O fluxo principal de Cinza Pálida é encontrar
			// um resíduo deixado por uma manifestação, mas estruturas antigas podem antecipar a descoberta.
			adicionarVestigios(key, tableBuilder, BuiltInLootTables.ABANDONED_MINESHAFT, 0.06F, 0.00F, 0.10F);
			adicionarVestigios(key, tableBuilder, BuiltInLootTables.WOODLAND_MANSION, 0.16F, 0.05F, 0.12F);
			adicionarVestigios(key, tableBuilder, BuiltInLootTables.ANCIENT_CITY, 0.24F, 0.08F, 0.10F);
			adicionarVestigios(key, tableBuilder, BuiltInLootTables.STRONGHOLD_LIBRARY, 0.10F, 0.04F, 0.08F);

			adicionarIsca(key, tableBuilder, BuiltInLootTables.ABANDONED_MINESHAFT, 0.05F);
			adicionarIsca(key, tableBuilder, BuiltInLootTables.WOODLAND_MANSION, 0.08F);
			adicionarIsca(key, tableBuilder, BuiltInLootTables.ANCIENT_CITY, 0.04F);
			adicionarIsca(key, tableBuilder, BuiltInLootTables.STRONGHOLD_LIBRARY, 0.05F);

			adicionarCaderno(key, tableBuilder, BuiltInLootTables.WOODLAND_MANSION, 0.05F);
			adicionarCaderno(key, tableBuilder, BuiltInLootTables.ANCIENT_CITY, 0.04F);
			adicionarCaderno(key, tableBuilder, BuiltInLootTables.STRONGHOLD_LIBRARY, 0.08F);
		});
	}

	private static void adicionarCaderno(ResourceKey<LootTable> key, LootTable.Builder tableBuilder,
			ResourceKey<LootTable> alvo, float chance) {
		if (!alvo.equals(key) || chance <= 0) {
			return;
		}
		tableBuilder.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(ModItems.CADERNO_VESTIGIOS))
				.when(LootItemRandomChanceCondition.randomChance(chance)));
	}

	private static void adicionarIsca(ResourceKey<LootTable> key, LootTable.Builder tableBuilder,
			ResourceKey<LootTable> alvo, float chance) {
		if (!alvo.equals(key) || chance <= 0) {
			return;
		}
		tableBuilder.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(ModItems.ISCA_PALIDA))
				.when(LootItemRandomChanceCondition.randomChance(chance)));
	}

	private static void adicionarVestigios(ResourceKey<LootTable> key, LootTable.Builder tableBuilder,
			ResourceKey<LootTable> alvo, float chanceCinza, float chanceSino, float chanceFio) {
		if (!alvo.equals(key)) {
			return;
		}
		if (chanceCinza > 0) {
			tableBuilder.withPool(LootPool.lootPool()
					.add(LootItem.lootTableItem(ModItems.CINZA_PALIDA))
					.when(LootItemRandomChanceCondition.randomChance(chanceCinza)));
		}
		if (chanceSino > 0) {
			tableBuilder.withPool(LootPool.lootPool()
					.add(LootItem.lootTableItem(ModItems.SINO_OCO))
					.when(LootItemRandomChanceCondition.randomChance(chanceSino)));
		}
		if (chanceFio > 0) {
			tableBuilder.withPool(LootPool.lootPool()
					.add(LootItem.lootTableItem(ModItems.FIO_VIGILIA))
					.when(LootItemRandomChanceCondition.randomChance(chanceFio)));
		}
	}

	private static void adicionar(ResourceKey<LootTable> key, LootTable.Builder tableBuilder,
			ResourceKey<LootTable> alvo, float chancePagina, float chanceOlho) {
		if (!alvo.equals(key)) {
			return;
		}
		tableBuilder.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(ModItems.PAGINA_RASGADA))
				.when(LootItemRandomChanceCondition.randomChance(chancePagina)));
		if (chanceOlho > 0) {
			tableBuilder.withPool(LootPool.lootPool()
					.add(LootItem.lootTableItem(ModItems.OLHO_SUSSURRANTE))
					.when(LootItemRandomChanceCondition.randomChance(chanceOlho)));
		}
	}
}
