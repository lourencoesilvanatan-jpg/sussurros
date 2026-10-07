package com.sussurros.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import com.sussurros.Sussurros;
import com.sussurros.entidade.HospedeEntity;

public class HospedeRenderer extends MobRenderer<HospedeEntity, HospedeRenderState, HospedeModel> {
	private static final Identifier TEXTURA = Sussurros.id("textures/entity/hospede.png");

	public HospedeRenderer(EntityRendererProvider.Context context) {
		super(context, new HospedeModel(context.bakeLayer(ModCamadas.HOSPEDE)), 0.4f);
	}

	@Override
	public HospedeRenderState createRenderState() {
		return new HospedeRenderState();
	}

	@Override
	public void extractRenderState(HospedeEntity entity, HospedeRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		state.observando = entity.isObservando();
		state.modoVisual = entity.getModoVisual();
		state.avistado = entity.foiAvistadoVisual();
		state.varianteVisual = entity.getVarianteVisual();
		state.olhos = entity.temOlhosVisuais();
	}

	@Override
	public Identifier getTextureLocation(HospedeRenderState state) {
		return TEXTURA;
	}
}
