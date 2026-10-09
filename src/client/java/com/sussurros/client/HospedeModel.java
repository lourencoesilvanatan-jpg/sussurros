package com.sussurros.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Modelo do Hóspede: alto (~3 blocos), magro, braços longos demais.
 * Em modelos do Minecraft, o eixo Y cresce PARA BAIXO e o chão fica em y = 24.
 */
public class HospedeModel extends EntityModel<HospedeRenderState> {
	private final ModelPart cabeca;
	private final ModelPart corpo;
	private final ModelPart bracoEsquerdo;
	private final ModelPart bracoDireito;
	private final ModelPart pernaEsquerda;
	private final ModelPart pernaDireita;
	private final ModelPart olhoEsquerdo;
	private final ModelPart olhoDireito;

	/** Quanto o tronco tomba para a frente quando ele se abaixa (radianos, uns 72 graus). */
	private static final float DOBRA = 1.26f;

	public HospedeModel(ModelPart root) {
		super(root);
		this.cabeca = root.getChild(PartNames.HEAD);
		this.corpo = root.getChild(PartNames.BODY);
		this.bracoEsquerdo = root.getChild(PartNames.LEFT_ARM);
		this.bracoDireito = root.getChild(PartNames.RIGHT_ARM);
		this.pernaEsquerda = root.getChild(PartNames.LEFT_LEG);
		this.pernaDireita = root.getChild(PartNames.RIGHT_LEG);
		this.olhoEsquerdo = this.cabeca.getChild("olho_esquerdo");
		this.olhoDireito = this.cabeca.getChild("olho_direito");
	}

	public static LayerDefinition criarCamada() {
		MeshDefinition malha = new MeshDefinition();
		PartDefinition raiz = malha.getRoot();

		// Cabeça alongada (6 x 10 x 6)
		PartDefinition cabeca = raiz.addOrReplaceChild(PartNames.HEAD,
				CubeListBuilder.create().texOffs(0, 0).addBox(-3, -10, -3, 6, 10, 6),
				PartPose.offset(0, -16, 0));
		// Dois pontos pálidos opcionais. São geometria normal (não emissiva), justamente para
		// aparecerem só quando a iluminação permitir em vez de virarem um marcador permanente.
		cabeca.addOrReplaceChild("olho_esquerdo",
				CubeListBuilder.create().texOffs(60, 0).addBox(1.0f, -6.4f, -3.28f, 1.0f, 1.0f, 0.35f),
				PartPose.ZERO);
		cabeca.addOrReplaceChild("olho_direito",
				CubeListBuilder.create().texOffs(60, 2).addBox(-2.0f, -6.4f, -3.28f, 1.0f, 1.0f, 0.35f),
				PartPose.ZERO);

		// Tronco estreito (8 x 18 x 3)
		raiz.addOrReplaceChild(PartNames.BODY,
				CubeListBuilder.create().texOffs(24, 0).addBox(-4, 0, -1.5f, 8, 18, 3),
				PartPose.offset(0, -16, 0));

		// Braços finos e longos demais (2 x 30 x 2)
		raiz.addOrReplaceChild(PartNames.RIGHT_ARM,
				CubeListBuilder.create().texOffs(48, 0).addBox(-1, -1, -1, 2, 30, 2),
				PartPose.offset(-5, -15, 0));
		raiz.addOrReplaceChild(PartNames.LEFT_ARM,
				CubeListBuilder.create().texOffs(56, 0).addBox(-1, -1, -1, 2, 30, 2),
				PartPose.offset(5, -15, 0));

		// Pernas (3 x 22 x 3)
		raiz.addOrReplaceChild(PartNames.RIGHT_LEG,
				CubeListBuilder.create().texOffs(0, 32).addBox(-1.5f, 0, -1.5f, 3, 22, 3),
				PartPose.offset(-2, 2, 0));
		raiz.addOrReplaceChild(PartNames.LEFT_LEG,
				CubeListBuilder.create().texOffs(12, 32).addBox(-1.5f, 0, -1.5f, 3, 22, 3),
				PartPose.offset(2, 2, 0));

		return LayerDefinition.create(malha, 64, 64);
	}

	@Override
	public void setupAnim(HospedeRenderState state) {
		super.setupAnim(state);

		float grausParaRad = (float) (Math.PI / 180.0);
		float t = state.ageInTicks;
		boolean espreitando = state.modoVisual == 1;
		boolean esperando = state.modoVisual == 2;
		boolean cacando = state.modoVisual == 3;

		// Cabeça segue o olhar, mas cada modo tem uma silhueta própria.
		this.cabeca.yRot = state.yRot * grausParaRad;
		this.cabeca.xRot = state.xRot * grausParaRad;
		float inclinacao = state.observando ? (espreitando ? 0.48f : esperando ? -0.30f : 0.38f) : 0.0f;
		// Antes de ser percebido ele parece mais torto. Quando você o encontra, ele "se recompõe" um pouco.
		if (state.avistado && !esperando) {
			inclinacao *= 0.58f;
		}
		if (cacando) {
			inclinacao *= 0.35f;
		}
		this.cabeca.zRot = inclinacao + Mth.sin(t * (espreitando ? 0.055f : 0.04f)) * (espreitando ? 0.08f : 0.06f);

		float anda = state.walkAnimationSpeed;
		float pos = state.walkAnimationPos;
		float passo = cacando ? 1.18f : 0.9f;
		this.pernaDireita.xRot = Mth.cos(pos * 0.4f) * passo * anda;
		this.pernaEsquerda.xRot = Mth.cos(pos * 0.4f + (float) Math.PI) * passo * anda;
		this.pernaDireita.zRot = 0.0f;
		this.pernaEsquerda.zRot = 0.0f;

		// Braços: a caça fica mais agressiva; a espreita fica assimétrica, como se ele se inclinasse para ver melhor.
		float bracoBase = cacando ? -0.28f : 0.0f;
		this.bracoDireito.xRot = bracoBase + Mth.cos(pos * 0.4f + (float) Math.PI) * (cacando ? 0.72f : 0.5f) * anda;
		this.bracoEsquerdo.xRot = bracoBase + Mth.cos(pos * 0.4f) * (cacando ? 0.72f : 0.5f) * anda;
		this.bracoDireito.zRot = (espreitando ? 0.13f : 0.05f) + Mth.sin(t * 0.03f) * 0.03f;
		this.bracoEsquerdo.zRot = (espreitando ? -0.02f : -0.05f) - Mth.sin(t * 0.03f + 1.0f) * 0.03f;

		// O tronco ajuda a leitura à distância sem depender de uma biblioteca de animação.
		this.corpo.xRot = (cacando ? 0.11f : 0.0f) + Mth.sin(t * 0.05f) * 0.02f;
		this.corpo.yRot = 0.0f;
		this.corpo.zRot = espreitando ? 0.065f + Mth.sin(t * 0.025f) * 0.025f : 0.0f;
		if (state.avistado && state.observando && !cacando) {
			this.corpo.zRot *= 0.45f;
			this.bracoDireito.zRot *= 0.7f;
			this.bracoEsquerdo.zRot *= 0.7f;
		}

		// Três silhuetas discretas e determinísticas por manifestação. Não muda hitbox nem IA.
		if (state.varianteVisual == 1) {
			this.corpo.zRot -= 0.055f;
			this.cabeca.zRot -= 0.10f;
			this.bracoDireito.zRot += 0.09f;
		} else if (state.varianteVisual == 2) {
			this.corpo.xRot += 0.055f;
			this.cabeca.zRot += 0.12f;
			this.bracoEsquerdo.zRot -= 0.10f;
		}

		// Os olhos não aparecem em toda manifestação e piscam de forma irregular, mas determinística.
		boolean pisca = ((int) (state.ageInTicks / 7.0f) % 11) != 0;
		boolean mostrarOlhos = state.olhos && pisca && (!state.avistado || cacando || espreitando);
		this.olhoEsquerdo.visible = mostrarOlhos;
		this.olhoDireito.visible = mostrarOlhos;

		if (esperando) {
			// Na borda da vela ele parece quase imóvel, diferente da respiração normal.
			this.corpo.xRot *= 0.25f;
			this.bracoDireito.xRot *= 0.2f;
			this.bracoEsquerdo.xRot *= 0.2f;
		}

		// Dobrado para caber sob um teto de dois blocos: o tronco tomba para a frente a partir do quadril,
		// e a cabeça e os ombros descem junto. O tronco gira em torno do topo, então desloca-se o topo para
		// onde ele ficaria se o giro fosse no quadril (que está em y = 2, com 18 de comprimento).
		float dobra = state.agachado ? DOBRA : 0.0f;
		float topoY = 2.0f - 18.0f * Mth.cos(dobra);
		float topoZ = -18.0f * Mth.sin(dobra);
		this.corpo.y = topoY;
		this.corpo.z = topoZ;
		this.cabeca.y = topoY;
		this.cabeca.z = topoZ;
		this.bracoDireito.y = topoY + 1.0f;
		this.bracoDireito.z = topoZ;
		this.bracoEsquerdo.y = topoY + 1.0f;
		this.bracoEsquerdo.z = topoZ;
		if (state.agachado) {
			this.corpo.xRot += dobra;
			// A cabeça vai à frente do corpo, erguida o bastante para continuar olhando para você.
			this.cabeca.xRot += dobra * 0.55f;
			// Os braços são longos demais para pender: vão para trás, quase arrastando.
			this.bracoDireito.xRot += 0.95f;
			this.bracoEsquerdo.xRot += 0.95f;
		}
	}
}
