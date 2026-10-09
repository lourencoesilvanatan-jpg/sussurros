package com.sussurros.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.sussurros.assombracao.Conta;
import com.sussurros.registro.ModBlocos;

/**
 * Resíduo físico deixado por algumas manifestações. Não serve como prova perfeita:
 * ele também pode aparecer em lugares antigos ligados à assombração.
 */
public class CinzaPalidaItem extends Item {
	public CinzaPalidaItem(Properties properties) {
		super(properties);
	}

	/**
	 * 0.9: usada no chão, vira a Linha de Cinza. Ele não cruza uma linha que ainda segura; cada tentativa a
	 * desgasta (ver CinzaEspalhadaBlock). No Lampião Pálido quem trata a cinza é o próprio bloco.
	 */
	@Override
	public InteractionResult useOn(UseOnContext contexto) {
		Level level = contexto.getLevel();
		BlockPos clicado = contexto.getClickedPos();
		boolean sobre = level.getBlockState(clicado).canBeReplaced();
		if (!sobre && contexto.getClickedFace() != Direction.UP) {
			return InteractionResult.PASS;
		}
		BlockPos alvo = sobre ? clicado : clicado.above();
		BlockState linha = ModBlocos.CINZA_ESPALHADA.defaultBlockState();
		BlockState atual = level.getBlockState(alvo);
		if (!atual.canBeReplaced() || atual.is(ModBlocos.CINZA_ESPALHADA) || !atual.getFluidState().isEmpty()
				|| !linha.canSurvive(level, alvo)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(alvo, linha, 3);
			level.playSound(null, alvo, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.5F, 1.3F);
			if (contexto.getPlayer() != null) {
				contexto.getItemInHand().consume(1, contexto.getPlayer());
			}
			if (contexto.getPlayer() instanceof ServerPlayer jogador) {
				Conta.somar(jogador, Conta.Item.LINHA);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		textConsumer.accept(Component.translatable("itemTooltip.sussurros.cinza_palida")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
