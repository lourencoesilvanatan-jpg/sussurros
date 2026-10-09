package com.sussurros.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import com.sussurros.client.SentidosCliente;

/**
 * O único mixin do mod. No Véu, as entidades deixam de ser desenhadas para o jogador (ver Veu, no servidor).
 * Não há evento da Fabric para "não desenhe esta entidade", e esconder pelo servidor exigiria mexer no
 * rastreamento de entidades. Aqui a pergunta "desenho esta?" passa a ter mais um motivo para dizer não.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EsconderNoVeuMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void sussurros$esconderNoVeu(E entidade, Frustum campo, double x, double y, double z,
			CallbackInfoReturnable<Boolean> retorno) {
		if (SentidosCliente.esconde(entidade)) {
			retorno.setReturnValue(false);
		}
	}
}
