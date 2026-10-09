package com.sussurros.client;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

import com.sussurros.Sussurros;

/**
 * A cor do mundo drena conforme a assombração pesa. São degraus pequenos: o jogador não vê a troca, só
 * percebe um dia que as cores não são mais as mesmas. Dentro da vela, e de dia em casa, ela volta um pouco.
 *
 * Usa o efeito de tela do próprio jogo (o mesmo mecanismo da visão de creeper em modo espectador), com
 * definições em assets/sussurros/post_effect. O jogo não expõe um jeito de escolher o efeito, então os dois
 * campos são alcançados por reflexão. Se isso falhar numa versão futura, o efeito se desliga sozinho e o
 * resto do mod continua. Com shader pack (Iris) a cor pode não mudar.
 */
final class CorDrenada {
	private static final Identifier[] DEGRAUS = {
			null,
			Sussurros.id("pesar_1"),
			Sussurros.id("pesar_2"),
			Sussurros.id("pesar_3"),
			Sussurros.id("pesar_4")
	};
	private static final Identifier AVESSO = Sussurros.id("pesar_avesso");

	/** Folga para trocar de degrau: sem ela, um valor oscilando na fronteira ficaria trocando a cada segundo. */
	private static final float FOLGA = 0.03F;

	@Nullable
	private static Field campoEfeito;
	@Nullable
	private static Field campoAtivo;
	private static boolean desligado;
	private static int degrau;

	private CorDrenada() {
	}

	static void tick(Minecraft mc) {
		if (desligado) {
			return;
		}
		degrau = proximoDegrau(degrau, SentidosCliente.peso);
		Identifier desejado = !OpcoesCliente.cor ? null : SentidosCliente.noAvesso() ? AVESSO : DEGRAUS[degrau];
		try {
			if (campoEfeito == null || campoAtivo == null) {
				campoEfeito = GameRenderer.class.getDeclaredField("postEffectId");
				campoEfeito.setAccessible(true);
				campoAtivo = GameRenderer.class.getDeclaredField("effectActive");
				campoAtivo.setAccessible(true);
			}
			Identifier atual = (Identifier) campoEfeito.get(mc.gameRenderer);
			boolean nosso = atual != null && Sussurros.MOD_ID.equals(atual.getNamespace());
			if (desejado == null) {
				if (nosso) {
					mc.gameRenderer.clearPostEffect();
				}
				return;
			}
			// Um efeito do próprio jogo (câmera dentro de um creeper, por exemplo) tem prioridade.
			if (atual == null || (nosso && !atual.equals(desejado))) {
				campoEfeito.set(mc.gameRenderer, desejado);
				campoAtivo.setBoolean(mc.gameRenderer, true);
			}
		} catch (ReflectiveOperationException | RuntimeException ex) {
			desligado = true;
			Sussurros.LOGGER.warn("Não foi possível aplicar o efeito de cor; ele fica desligado nesta sessão.", ex);
		}
	}

	/**
	 * De 0 (cor normal) a 4, um degrau por vez. Sobe ao passar de 0,125, 0,375, 0,625 e 0,875 (mais a folga)
	 * e desce ao voltar abaixo da mesma fronteira (menos a folga).
	 */
	static int proximoDegrau(int atual, float peso) {
		if (atual < 4 && peso >= (atual + 0.5F) / 4.0F + FOLGA) {
			return atual + 1;
		}
		if (atual > 0 && peso <= (atual - 0.5F) / 4.0F - FOLGA) {
			return atual - 1;
		}
		return atual;
	}
}
