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

import com.sussurros.assombracao.Diretor;

/**
 * Marca temporariamente o ponto onde o jogador está. Não repele o Hóspede:
 * apenas avisa, de forma física, se ele realmente atravessar aquela pequena área.
 */
public class FioVigiliaItem extends Item {
	public FioVigiliaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			if (!Diretor.armarFioVigilia(jogador)) {
				jogador.sendOverlayMessage(Component.translatable("message.sussurros.fio.ja_armado")
						.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
				return InteractionResult.FAIL;
			}
			player.getItemInHand(hand).consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("itemTooltip.sussurros.fio_vigilia")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
