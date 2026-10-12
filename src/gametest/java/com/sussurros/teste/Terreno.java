package com.sussurros.teste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Monta, no mundo de teste, um pedaço de terreno de verdade (0.9.0-alpha19).
 *
 * O mundo de teste é um gramado raspado, e teste em gramado não prova nada que dependa de terreno: a alpha17
 * passou em todos e não funcionou na casa do dono. Os arquivos de src/gametest/resources/terrenos saem de um
 * mundo salvo, por ferramentas/log/ler_mundo.py exportar: só os nomes dos blocos, sem as coordenadas de origem.
 *
 * O terreno é montado em cima do chão do mundo de teste, dentro de uma casca de pedra (lados e teto): o que
 * era caverna continua sem luz do céu, e o que era corredor cortado pela borda da caixa não vira saída.
 * Os blocos entram sem avisar os vizinhos: a água não escorre, o cascalho não cai e a tocha sem parede não solta.
 */
final class Terreno {
	private static final int SEM_AVISAR = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;

	private Terreno() {
	}

	/** Onde o terreno ficou: o canto de baixo (o menor x, y e z do arquivo) e o tamanho. */
	record Montado(BlockPos canto, int larguraX, int altura, int larguraZ) {
		/** A posição, no mundo de teste, do bloco que no arquivo está em (x, y, z) a partir do canto. */
		BlockPos em(int x, int y, int z) {
			return this.canto.offset(x, y, z);
		}
	}

	/** Monta o terreno "nome" com o canto de baixo em "canto". Leva um tick longo: dezenas de milhares de blocos. */
	static Montado montar(ServerLevel level, String nome, BlockPos canto) {
		String caminho = "/terrenos/" + nome + ".txt";
		try (InputStream entrada = Terreno.class.getResourceAsStream(caminho)) {
			if (entrada == null) {
				throw new IllegalStateException("não achei o terreno de teste " + caminho);
			}
			BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
			int[] tamanho = null;
			BlockState[] paleta = null;
			int indice = 0;
			int total = 0;
			String linha;
			while ((linha = leitor.readLine()) != null) {
				linha = linha.trim();
				if (linha.isEmpty() || linha.startsWith("#")) {
					continue;
				}
				String[] partes = linha.split(" ");
				if (partes[0].equals("tamanho")) {
					tamanho = new int[] {Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3])};
					total = tamanho[0] * tamanho[1] * tamanho[2];
					casca(level, canto, tamanho);
				} else if (partes[0].equals("paleta")) {
					paleta = new BlockState[Integer.parseInt(partes[1])];
					for (int i = 0; i < paleta.length; i++) {
						String[] par = leitor.readLine().trim().split(" ");
						paleta[Integer.parseInt(par[0])] = estado(par[1]);
					}
				} else if (partes[0].equals("blocos")) {
					continue;
				} else {
					if (tamanho == null || paleta == null) {
						throw new IllegalStateException(caminho + ": blocos antes do tamanho ou da paleta");
					}
					for (String corrida : partes) {
						int vezes = corrida.indexOf('*');
						BlockState bloco = paleta[Integer.parseInt(corrida.substring(0, vezes))];
						for (int n = Integer.parseInt(corrida.substring(vezes + 1)); n > 0; n--, indice++) {
							int x = indice % tamanho[0];
							int z = indice / tamanho[0] % tamanho[2];
							int y = indice / (tamanho[0] * tamanho[2]);
							level.setBlock(canto.offset(x, y, z), bloco, SEM_AVISAR);
						}
					}
				}
			}
			if (tamanho == null || indice != total) {
				throw new IllegalStateException(caminho + ": esperava " + total + " blocos, vieram " + indice);
			}
			return new Montado(canto, tamanho[0], tamanho[1], tamanho[2]);
		} catch (IOException ex) {
			throw new IllegalStateException("não consegui ler " + caminho, ex);
		}
	}

	/** O bloco de nome "minecraft:..." no seu estado padrão. Nome que o jogo não conhece vira pedra. */
	private static BlockState estado(String nome) {
		Identifier id = Identifier.tryParse(nome);
		Block bloco = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.getValue(id);
		if (bloco == Blocks.AIR && !nome.endsWith(":air")) {
			return nome.endsWith(":cave_air") || nome.endsWith(":void_air") ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
		}
		return bloco.defaultBlockState();
	}

	/**
	 * Uma camada de pedra em volta, em cima e embaixo da caixa. Embaixo também: o chão do mundo de teste não é
	 * plano, e onde ele é mais baixo que o canto a caverna ficava com o fundo aberto para o lado de fora.
	 */
	private static void casca(ServerLevel level, BlockPos canto, int[] tamanho) {
		BlockState pedra = Blocks.STONE.defaultBlockState();
		for (int x = -1; x <= tamanho[0]; x++) {
			for (int z = -1; z <= tamanho[2]; z++) {
				boolean borda = x == -1 || x == tamanho[0] || z == -1 || z == tamanho[2];
				for (int y = -1; y <= tamanho[1]; y++) {
					if (borda || y == tamanho[1] || y == -1) {
						level.setBlock(canto.offset(x, y, z), pedra, SEM_AVISAR);
					}
				}
			}
		}
	}
}
