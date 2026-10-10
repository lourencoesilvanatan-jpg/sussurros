package com.sussurros.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Vela de cera pálida. Consumível: acende uma pequena área de calma ao seu redor.
 * Não dá para depender dela para sempre.
 */
public class VelaPalidaItem extends Item {
	public VelaPalidaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			ItemStack stack = player.getItemInHand(hand);
			if (Diretor.temZonaCalma(jogador)) {
				jogador.sendOverlayMessage(Component.translatable("message.sussurros.vela.ja_acesa").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
				return InteractionResult.FAIL;
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
			Diretor.acenderVela(jogador);
			player.getCooldowns().addCooldown(stack, 20 * 10);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		Dicas.acrescentar(textConsumer, "vela_palida", true);
	}
}
