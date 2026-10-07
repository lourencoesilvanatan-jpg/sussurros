package com.sussurros.client;

import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.api.ClientModInitializer;

import com.sussurros.registro.ModEntidades;

public class SussurrosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModCamadas.registrar();
		EntityRenderers.register(ModEntidades.HOSPEDE, HospedeRenderer::new);
	}
}
