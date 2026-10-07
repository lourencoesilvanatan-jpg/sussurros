package com.sussurros.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import com.sussurros.assombracao.Diretor;
import com.sussurros.registro.ModSons;

/**
 * O Olho mostra onde a criatura está.
 * Mas usar o Olho tem um preço: a escuridão te cerca, e olhar... chama atenção.
 */
public class OlhoSussurranteItem extends Item {
	private static final int RECARGA = 20 * 30;

	public OlhoSussurranteItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer jogador) {
			ItemStack stack = player.getItemInHand(hand);
			ModSons.tocar(jogador.level(), player.getX(), player.getY() + 1.0, player.getZ(), ModSons.Som.GRAVE, 0.65F, 0.68F);
			ModSons.tocar(jogador.level(), player.getX(), player.getY() + 1.0, player.getZ(), ModSons.Som.RESPIRACAO, 0.38F, 0.78F);
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 5));
			Diretor.usarOlho(jogador);
			player.getCooldowns().addCooldown(stack, RECARGA);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("itemTooltip.sussurros.olho_sussurrante").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
