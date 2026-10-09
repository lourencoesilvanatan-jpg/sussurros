package com.sussurros.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import org.jspecify.annotations.Nullable;

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
		state.alfa = entity.alfaVisual(tickProgress);
		// Caçando e com teto a menos de três blocos: o corpo dobra. A caixa de colisão já é baixa na caça;
		// isto é só o desenho acompanhando.
		BlockPos acima = entity.blockPosition().above(2);
		state.agachado = state.modoVisual == HospedeEntity.Modo.CACAR.ordinal()
				&& !entity.level().getBlockState(acima).getCollisionShape(entity.level(), acima).isEmpty();
	}

	// Enquanto dissolve, o corpo é desenhado translúcido e cada vez mais transparente.
	@Override
	protected int getModelTint(HospedeRenderState state) {
		return state.alfa >= 1.0F ? -1 : ARGB.white(Math.max(0.0F, state.alfa));
	}

	@Override
	@Nullable
	protected RenderType getRenderType(HospedeRenderState state, boolean corpoVisivel, boolean forcarTransparente, boolean brilhando) {
		if (corpoVisivel && state.alfa < 1.0F) {
			return RenderTypes.entityTranslucent(this.getTextureLocation(state));
		}
		return super.getRenderType(state, corpoVisivel, forcarTransparente, brilhando);
	}

	@Override
	protected float getShadowStrength(HospedeRenderState state) {
		return super.getShadowStrength(state) * state.alfa;
	}

	@Override
	public Identifier getTextureLocation(HospedeRenderState state) {
		return TEXTURA;
	}
}
