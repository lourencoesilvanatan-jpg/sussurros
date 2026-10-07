package com.sussurros.registro;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.sussurros.Sussurros;
import com.sussurros.item.CadernoVestigiosItem;
import com.sussurros.item.CinzaPalidaItem;
import com.sussurros.item.FioVigiliaItem;
import com.sussurros.item.IscaPalidaItem;
import com.sussurros.item.OlhoSussurranteItem;
import com.sussurros.item.SinoOcoItem;
import com.sussurros.item.PaginaRasgadaItem;
import com.sussurros.item.VelaPalidaItem;

public class ModItems {
	public static final Item OLHO_SUSSURRANTE = register("olho_sussurrante", OlhoSussurranteItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	public static final Item VELA_PALIDA = register("vela_palida", VelaPalidaItem::new,
			new Item.Properties().stacksTo(16));

	public static final Item PAGINA_RASGADA = register("pagina_rasgada", PaginaRasgadaItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

	public static final Item CINZA_PALIDA = register("cinza_palida", CinzaPalidaItem::new,
			new Item.Properties().stacksTo(32).rarity(Rarity.UNCOMMON));

	public static final Item CADERNO_VESTIGIOS = register("caderno_vestigios", CadernoVestigiosItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final Item SINO_OCO = register("sino_oco", SinoOcoItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	public static final Item FIO_VIGILIA = register("fio_vigilia", FioVigiliaItem::new,
			new Item.Properties().stacksTo(16));

	public static final Item ISCA_PALIDA = register("isca_palida", IscaPalidaItem::new,
			new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON));

	public static final Item OVO_HOSPEDE = register("ovo_hospede", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntidades.HOSPEDE));

	// Aba própria no modo criativo
	public static final ResourceKey<CreativeModeTab> ABA_KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(), Sussurros.id("aba_sussurros"));

	public static final CreativeModeTab ABA = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(OLHO_SUSSURRANTE))
			.title(Component.translatable("creativeTab.sussurros"))
			.displayItems((params, output) -> {
				output.accept(PAGINA_RASGADA);
				output.accept(CINZA_PALIDA);
				output.accept(CADERNO_VESTIGIOS);
				output.accept(FIO_VIGILIA);
				output.accept(ISCA_PALIDA);
				output.accept(SINO_OCO);
				output.accept(VELA_PALIDA);
				output.accept(OLHO_SUSSURRANTE);
				output.accept(OVO_HOSPEDE);
			})
			.build();

	public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Sussurros.id(name));
		T item = itemFactory.apply(settings.setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return item;
	}

	public static void inicializar() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ABA_KEY, ABA);
	}
}
