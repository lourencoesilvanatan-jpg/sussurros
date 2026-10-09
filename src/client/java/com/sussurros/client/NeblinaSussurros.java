package com.sussurros.client;

import java.lang.reflect.Field;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;

import com.sussurros.Sussurros;

/**
 * A neblina que fecha. Entra na lista de "ambientes de neblina" do jogo logo antes do ambiente normal do ar:
 * quando os sentidos pedem neblina, calcula a neblina normal e depois a aproxima. Cegueira, escuridão, água
 * e lava continuam valendo antes, porque vêm antes na lista. A cor continua sendo a do lugar.
 *
 * A lista é privada no jogo; é alcançada por reflexão. Se falhar, a neblina própria simplesmente não existe.
 */
final class NeblinaSussurros extends FogEnvironment {
	/** No máximo, a neblina fecha até esta distância (blocos). */
	private static final float FIM_MAXIMO = 22.0F;
	private static final float INICIO_MAXIMO = 2.0F;

	private final AtmosphericFogEnvironment normal = new AtmosphericFogEnvironment();

	private NeblinaSussurros() {
	}

	static void instalar() {
		try {
			Field campo = FogRenderer.class.getDeclaredField("FOG_ENVIRONMENTS");
			campo.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<FogEnvironment> lista = (List<FogEnvironment>) campo.get(null);
			int onde = lista.size();
			for (int i = 0; i < lista.size(); i++) {
				if (lista.get(i) instanceof AtmosphericFogEnvironment) {
					onde = i;
					break;
				}
			}
			lista.add(onde, new NeblinaSussurros());
		} catch (ReflectiveOperationException | RuntimeException ex) {
			Sussurros.LOGGER.warn("Não foi possível instalar a neblina própria; ela fica desligada.", ex);
		}
	}

	@Override
	public boolean isApplicable(@Nullable FogType tipo, Entity entidade) {
		return tipo == FogType.ATMOSPHERIC && SentidosCliente.neblina > 0.01F;
	}

	@Override
	public void setupFog(FogData fog, Camera camera, ClientLevel level, float distanciaDeDesenho, DeltaTracker delta) {
		this.normal.setupFog(fog, camera, level, distanciaDeDesenho, delta);
		// Fecha rápido no começo e devagar no fim: metade da força já traz a neblina para perto.
		float n = Mth.clamp(SentidosCliente.neblina, 0.0F, 1.0F);
		float t = 1.0F - (1.0F - n) * (1.0F - n);
		float fimNormal = Math.min(fog.environmentalEnd, distanciaDeDesenho);
		float inicioNormal = Math.min(fog.environmentalStart, fimNormal * 0.6F);
		fog.environmentalEnd = Mth.lerp(t, fimNormal, FIM_MAXIMO);
		fog.environmentalStart = Mth.lerp(t, inicioNormal, INICIO_MAXIMO);
		fog.skyEnd = Math.min(fog.skyEnd, fog.environmentalEnd * 1.3F);
		fog.cloudEnd = Math.min(fog.cloudEnd, fog.environmentalEnd * 1.3F);
	}
}
