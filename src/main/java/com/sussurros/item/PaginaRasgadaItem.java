package com.sussurros.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.sussurros.assombracao.Diario;

/**
 * Uma página arrancada de um diário. Não importa qual página você encontrar:
 * você sempre lê a próxima da história, na ordem.
 */
public class PaginaRasgadaItem extends Item {
	public PaginaRasgadaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			if (Diario.lerProxima(jogador)) {
				player.getItemInHand(hand).consume(1, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("itemTooltip.sussurros.pagina_rasgada").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
