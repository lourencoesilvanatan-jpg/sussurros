package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.sussurros.Sussurros;

/**
 * Mapa de calor dos lugares do jogador, por chunk (16x16 blocos).
 * Chaves: "m:x:z" = minutos ali, "u:x:z" = último dia de jogo visitado, "k:x:z" = marco.
 * Para marcos novos, "x:/y:/z:" guardam a coordenada exata onde a reação forte aconteceu.
 */
public final class Lugares {
	static final int LIMITE_CHUNKS = 400;

	static final AttachmentType<Map<String, Integer>> TIPO = AttachmentRegistry.create(
			Sussurros.id("lugares"),
			builder -> builder
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
					.copyOnDeath()
	);

	private final ServerPlayer jogador;
	private final Map<String, Integer> dados;

	private Lugares(ServerPlayer jogador, Map<String, Integer> dados) {
		this.jogador = jogador;
		this.dados = dados;
	}

	static Lugares de(ServerPlayer p) {
		return new Lugares(p, new HashMap<>(p.getAttachedOrCreate(TIPO)));
	}

	public static void inicializar() {
	}

	private static String k(String tipo, int cx, int cz) {
		return tipo + ":" + cx + ":" + cz;
	}

	/** Registra o chunk se for novo. Retorna true se nunca tinha estado ali. */
	boolean registrarSeNovo(int cx, int cz) {
		String chave = k("m", cx, cz);
		if (this.dados.containsKey(chave)) {
			return false;
		}
		this.dados.put(chave, 0);
		podar();
		return true;
	}

	void somarMinuto(int cx, int cz, int dia) {
		this.dados.merge(k("m", cx, cz), 1, Integer::sum);
		this.dados.put(k("u", cx, cz), dia);
		podar();
	}

	int minutos(int cx, int cz) {
		return this.dados.getOrDefault(k("m", cx, cz), 0);
	}

	void marcarMarco(int cx, int cz) {
		registrarSeNovo(cx, cz);
		this.dados.put(k("k", cx, cz), 1);
	}

	/** Marca o chunk e lembra o ponto exato onde o evento forte aconteceu. */
	void marcarMarco(int cx, int cz, BlockPos pos) {
		marcarMarco(cx, cz);
		this.dados.put(k("x", cx, cz), pos.getX());
		this.dados.put(k("y", cx, cz), pos.getY());
		this.dados.put(k("z", cx, cz), pos.getZ());
	}

	boolean ehMarco(int cx, int cz) {
		return this.dados.containsKey(k("k", cx, cz));
	}

	/** Ponto exato do marco, quando foi gravado por uma versão que já suportava essa informação. */
	@Nullable
	BlockPos posMarco(int cx, int cz) {
		String xk = k("x", cx, cz);
		if (!this.dados.containsKey(xk)) {
			return null;
		}
		return new BlockPos(this.dados.get(xk), this.dados.getOrDefault(k("y", cx, cz), 64),
				this.dados.getOrDefault(k("z", cx, cz), cz << 4));
	}

	/** Chunks onde o jogador passou 20+ minutos (exceto o da casa). Cada item: {cx, cz}. */
	List<int[]> rotas(boolean temCasa, int casaCx, int casaCz) {
		List<int[]> lista = new ArrayList<>();
		for (Map.Entry<String, Integer> en : this.dados.entrySet()) {
			if (!en.getKey().startsWith("m:") || en.getValue() < 20) {
				continue;
			}
			String[] partes = en.getKey().split(":");
			int cx = Integer.parseInt(partes[1]);
			int cz = Integer.parseInt(partes[2]);
			if (temCasa && cx == casaCx && cz == casaCz) {
				continue;
			}
			lista.add(new int[] {cx, cz});
		}
		return lista;
	}

	/** Mantém no máximo LIMITE_CHUNKS chunks, descartando os menos visitados. */
	private void podar() {
		List<Map.Entry<String, Integer>> chunks = new ArrayList<>();
		for (Map.Entry<String, Integer> en : this.dados.entrySet()) {
			if (en.getKey().startsWith("m:")) {
				chunks.add(en);
			}
		}
		if (chunks.size() <= LIMITE_CHUNKS) {
			return;
		}
		chunks.sort(Map.Entry.comparingByValue());
		List<String> remover = new ArrayList<>();
		for (int i = 0; i < 40 && i < chunks.size(); i++) {
			remover.add(chunks.get(i).getKey().substring(2));
		}
		for (String sufixo : remover) {
			this.dados.remove("m:" + sufixo);
			this.dados.remove("u:" + sufixo);
			this.dados.remove("k:" + sufixo);
			this.dados.remove("x:" + sufixo);
			this.dados.remove("y:" + sufixo);
			this.dados.remove("z:" + sufixo);
		}
	}

	void salvar() {
		this.jogador.setAttached(TIPO, new HashMap<>(this.dados));
	}

	static void apagar(ServerPlayer p) {
		p.setAttached(TIPO, new HashMap<>());
	}
}
