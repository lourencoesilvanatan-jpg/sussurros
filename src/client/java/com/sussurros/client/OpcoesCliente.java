package com.sussurros.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;

import com.sussurros.Sussurros;

/**
 * Opções de quem joga, guardadas no computador dele (config/sussurros-cliente.properties).
 *
 * Efeito de tela incomoda de verdade algumas pessoas, e filtro sem opção de desligar é reclamação antiga em
 * jogos de terror. Então a cor e a borda podem ser desligadas, cada uma, sem precisar de permissão no
 * servidor: /sussurros_tela cor nao, /sussurros_tela borda nao. O resto do mod não muda.
 */
public final class OpcoesCliente {
	private static final String ARQUIVO = "sussurros-cliente.properties";

	static boolean cor = true;
	static boolean borda = true;

	private OpcoesCliente() {
	}

	static void registrar() {
		carregar();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, acesso) -> {
			var raiz = ClientCommands.literal("sussurros_tela");
			for (String qual : new String[] {"cor", "borda"}) {
				var opcao = ClientCommands.literal(qual);
				for (boolean ligar : new boolean[] {true, false}) {
					opcao.then(ClientCommands.literal(ligar ? "sim" : "nao").executes(ctx -> {
						if (qual.equals("cor")) {
							cor = ligar;
						} else {
							borda = ligar;
						}
						salvar();
						ctx.getSource().sendFeedback(Component.literal("[Sussurros] Efeito de tela \"" + qual + "\": "
								+ (ligar ? "ligado" : "desligado") + "."));
						return 1;
					}));
				}
				raiz.then(opcao);
			}
			dispatcher.register(raiz);
		});
	}

	private static Path caminho() {
		return FabricLoader.getInstance().getConfigDir().resolve(ARQUIVO);
	}

	private static void carregar() {
		Path arquivo = caminho();
		if (!Files.exists(arquivo)) {
			return;
		}
		Properties p = new Properties();
		try (InputStream entrada = Files.newInputStream(arquivo)) {
			p.load(entrada);
			cor = !"nao".equals(p.getProperty("cor", "sim"));
			borda = !"nao".equals(p.getProperty("borda", "sim"));
		} catch (IOException ex) {
			Sussurros.LOGGER.warn("Não foi possível ler {}", arquivo, ex);
		}
	}

	private static void salvar() {
		Properties p = new Properties();
		p.setProperty("cor", cor ? "sim" : "nao");
		p.setProperty("borda", borda ? "sim" : "nao");
		try (OutputStream saida = Files.newOutputStream(caminho())) {
			p.store(saida, "Sussurros: efeitos de tela (sim ou nao)");
		} catch (IOException ex) {
			Sussurros.LOGGER.warn("Não foi possível gravar as opções do cliente", ex);
		}
	}
}
