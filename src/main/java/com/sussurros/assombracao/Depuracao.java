package com.sussurros.assombracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.loader.api.FabricLoader;

import com.sussurros.Sussurros;

/**
 * Log de decisões do Diretor, gravado em arquivo (nunca no chat).
 * Ligue com /sussurros debug on. CONTÉM SPOILERS: é para enviar, não para ler.
 */
public final class Depuracao {
	public static boolean ativo = false;

	private Depuracao() {
	}

	public static Path arquivo() {
		return FabricLoader.getInstance().getGameDir().resolve("sussurros-debug.log");
	}

	public static void log(ServerPlayer p, long seg, String msg) {
		if (!ativo) {
			return;
		}
		String linha = "[" + seg + "s][" + p.getName().getString() + "] " + msg + System.lineSeparator();
		try {
			Files.writeString(arquivo(), linha, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException ex) {
			Sussurros.LOGGER.warn("Não foi possível escrever o log de depuração", ex);
		}
	}
}
