package com.sussurros.registro;

import net.minecraft.server.level.ServerLevel;

/**
 * Versão SEM sons próprios. Use só se o ModSons.java normal não compilar:
 * copie este arquivo por cima de src/main/java/com/sussurros/registro/ModSons.java
 * (o nome do arquivo de destino continua ModSons.java). O mod funciona igual, sem esses sons.
 */
public final class ModSons {
	public enum Som {
		PANO, RESPIRACAO, MADEIRA, ARRASTO, ESTALO, GRAVE
	}

	private ModSons() {
	}

	public static void inicializar() {
	}

	public static void tocar(ServerLevel level, double x, double y, double z, Som som, float volume, float pitch) {
	}
}
