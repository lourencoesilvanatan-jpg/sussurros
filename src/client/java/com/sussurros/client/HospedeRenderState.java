package com.sussurros.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class HospedeRenderState extends LivingEntityRenderState {
	public boolean observando;
	public int modoVisual;
	public boolean avistado;
	public int varianteVisual;
	public boolean olhos;
	/** Na caça, sob teto baixo: ele dobra o corpo para caber onde o jogador cabe. */
	public boolean agachado;
	/** De 1 (inteiro) a 0: ele está dissolvendo. */
	public float alfa = 1.0F;
}
