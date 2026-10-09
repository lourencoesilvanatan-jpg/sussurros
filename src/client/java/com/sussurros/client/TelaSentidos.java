package com.sussurros.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.sussurros.Sussurros;

/**
 * O que os sentidos desenham por cima do mundo: a borda escura de "tem algo olhando" e os olhos fechando.
 *
 * A borda usa a mesma mistura da vinheta do próprio jogo (escurece o que está atrás, sem pintar por cima),
 * então não parece um filtro colado: parece o jogo mais escuro nas beiradas. A textura é própria
 * (ferramentas/texturas/gerar_vinheta.py), porque a do jogo escurece no máximo um quarto. Fica por baixo da
 * barra de itens. Os olhos fechando ficam por cima de tudo.
 */
final class TelaSentidos {
	private static final Identifier VINHETA = Sussurros.id("textures/misc/vinheta.png");

	private TelaSentidos() {
	}

	static void registrar() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Sussurros.id("vinheta"), TelaSentidos::vinheta);
		HudElementRegistry.addLast(Sussurros.id("olhos"), TelaSentidos::olhos);
	}

	private static void vinheta(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (!OpcoesCliente.borda) {
			return;
		}
		// Respira devagar: uma borda parada o olho esquece em segundos.
		float respiracao = 0.85F + 0.15F * Mth.sin(SentidosCliente.idade * 0.05F);
		float forca = SentidosCliente.vigia * 0.55F * respiracao + SentidosCliente.caca * 0.4F;
		if (SentidosCliente.noAvesso()) {
			forca += 0.3F;
		}
		forca = Mth.clamp(forca, 0.0F, 0.9F);
		if (forca < 0.01F) {
			return;
		}
		int cor = ARGB.colorFromFloat(1.0F, forca, forca, forca);
		g.blit(RenderPipelines.VIGNETTE, VINHETA, 0, 0, 0.0F, 0.0F, g.guiWidth(), g.guiHeight(), g.guiWidth(), g.guiHeight(), cor);
	}

	private static void olhos(GuiGraphicsExtractor g, DeltaTracker delta) {
		float escuro = SentidosCliente.escuro(delta.getGameTimeDeltaPartialTick(false));
		if (escuro < 0.004F) {
			return;
		}
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), ARGB.colorFromFloat(Mth.clamp(escuro, 0.0F, 1.0F), 0.0F, 0.0F, 0.0F));
	}
}
