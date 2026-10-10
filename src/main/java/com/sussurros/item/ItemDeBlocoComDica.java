package com.sussurros.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * Um bloco do mod que se carrega como item (o Lampião Pálido, a Tigela de Oferenda), com dica.
 * Até a alpha13 esses dois não tinham texto nenhum: nem dica, nem página do diário.
 */
public class ItemDeBlocoComDica extends BlockItem {
	private final String nome;
	private final boolean temPreco;

	public ItemDeBlocoComDica(Block bloco, Properties propriedades, String nome, boolean temPreco) {
		super(bloco, propriedades);
		this.nome = nome;
		this.temPreco = temPreco;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		Dicas.acrescentar(textConsumer, this.nome, this.temPreco);
	}
}
