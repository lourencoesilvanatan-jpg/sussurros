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
 * Uma ferramenta de investigação deliberadamente imperfeita. O sino chama uma resposta,
 * mas a resposta pode vir da criatura, de um lugar do Rastro... ou não vir.
 */
public class SinoOcoItem extends Item {
	private static final int RECARGA = 20 * 45;

	public SinoOcoItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			ItemStack stack = player.getItemInHand(hand);
			Diretor.usarSino(jogador);
			player.getCooldowns().addCooldown(stack, RECARGA);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("itemTooltip.sussurros.sino_oco")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
