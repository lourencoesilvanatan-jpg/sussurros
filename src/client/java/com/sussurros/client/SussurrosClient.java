package com.sussurros.client;

import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.api.ClientModInitializer;

import com.sussurros.registro.ModEntidades;

public class SussurrosClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModCamadas.registrar();
		EntityRenderers.register(ModEntidades.HOSPEDE, HospedeRenderer::new);
		// Sentidos: o que o servidor mede vira cor, borda, neblina e som.
		SentidosCliente.registrar();
		TelaSentidos.registrar();
		NeblinaSussurros.instalar();
	}
}
