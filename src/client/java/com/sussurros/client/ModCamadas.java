package com.sussurros.client;

import net.minecraft.client.model.geom.ModelLayerLocation;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import com.sussurros.Sussurros;

public class ModCamadas {
	public static final ModelLayerLocation HOSPEDE = new ModelLayerLocation(Sussurros.id("hospede"), "main");

	public static void registrar() {
		ModelLayerRegistry.registerModelLayer(HOSPEDE, HospedeModel::criarCamada);
	}
}
