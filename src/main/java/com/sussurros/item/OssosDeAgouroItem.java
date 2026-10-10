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

import com.sussurros.assombracao.Ossos;

/**
 * Ossos de Agouro (0.9). Jogá-los no chão dá um de sete desfechos, e não se escolhe qual. O resultado não
 * vem escrito: lê-se em como os ossos caem (ver {@link Ossos}).
 */
public class OssosDeAgouroItem extends Item {
	public OssosDeAgouroItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			ItemStack stack = player.getItemInHand(hand);
			if (!Ossos.jogar(jogador)) {
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(stack, 20 * 10);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		Dicas.acrescentar(textConsumer, "ossos_de_agouro", true);
	}
}
