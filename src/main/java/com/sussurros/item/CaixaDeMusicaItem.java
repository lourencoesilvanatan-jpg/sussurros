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

import com.sussurros.assombracao.Cantiga;

/**
 * Caixa de Música (0.9). Dar corda toca a cantiga no lugar onde o jogador está, e a música fica ali mesmo
 * que ele se afaste. Acalma; se ele estiver por perto, algo cantarola junto; na caçada, ele vai até a música.
 *
 * É o objeto inocente que ensina o tema do mod. Cada vez que toca, ele aprende um pouco: a caixa gasta, e
 * depois de algumas vezes a cantiga volta sem a caixa (ver {@link Cantiga}).
 */
public class CaixaDeMusicaItem extends Item {
	public CaixaDeMusicaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			ItemStack stack = player.getItemInHand(hand);
			if (!Cantiga.darCorda(jogador)) {
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(stack, 20 * 8);
			stack.hurtAndBreak(1, player, hand);
			if (stack.isEmpty()) {
				Cantiga.aCordaArrebentou(jogador);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		Dicas.acrescentar(textConsumer, "caixa_de_musica", true);
	}
}
