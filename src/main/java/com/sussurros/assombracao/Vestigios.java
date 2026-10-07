package com.sussurros.assombracao;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.sussurros.Sussurros;

/**
 * Pequena memória persistente de marcas físicas deixadas pela assombração.
 *
 * Não altera blocos do mundo: guarda até 18 pontos importantes por jogador para que
 * o Olho, o Sino e cenas futuras possam voltar a um lugar onde algo realmente aconteceu.
 */
public final class Vestigios {
	enum Tipo {
		DESAPARECIMENTO(1), VIGILIA(2), PEGADAS(3);

		final int codigo;

		Tipo(int codigo) {
			this.codigo = codigo;
		}

		static Tipo deCodigo(int codigo) {
			for (Tipo t : values()) {
				if (t.codigo == codigo) {
					return t;
				}
			}
			return DESAPARECIMENTO;
		}
	}

	record Marca(BlockPos pos, Tipo tipo, long seg, int slot) {
		long idade(long agora) {
			return Math.max(0, agora - this.seg);
		}
	}

	private static final int MAX = 18;
	private static final String PROXIMO = "proximo";

	private static final AttachmentType<Map<String, Integer>> TIPO = AttachmentRegistry.create(
			Sussurros.id("vestigios"),
			builder -> builder
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
					.copyOnDeath()
	);

	private final ServerPlayer jogador;
	private final Map<String, Integer> dados;

	private Vestigios(ServerPlayer jogador, Map<String, Integer> dados) {
		this.jogador = jogador;
		this.dados = dados;
	}

	static Vestigios de(ServerPlayer jogador) {
		return new Vestigios(jogador, new HashMap<>(jogador.getAttachedOrCreate(TIPO)));
	}

	public static void inicializar() {
	}

	void registrar(BlockPos pos, Tipo tipo, long seg) {
		int slot = Math.floorMod(this.dados.getOrDefault(PROXIMO, 0), MAX);
		String p = "v" + slot + "_";
		this.dados.put(p + "x", pos.getX());
		this.dados.put(p + "y", pos.getY());
		this.dados.put(p + "z", pos.getZ());
		this.dados.put(p + "t", tipo.codigo);
		this.dados.put(p + "s", (int) Math.min(Integer.MAX_VALUE, Math.max(0, seg)));
		this.dados.put(PROXIMO, (slot + 1) % MAX);
		salvar();
	}

	@Nullable
	Marca maisPerto(double x, double y, double z, double raio, long agora) {
		double melhor = raio * raio;
		Marca encontrada = null;
		for (int slot = 0; slot < MAX; slot++) {
			String p = "v" + slot + "_";
			if (!this.dados.containsKey(p + "x")) {
				continue;
			}
			int px = this.dados.get(p + "x");
			int py = this.dados.getOrDefault(p + "y", 64);
			int pz = this.dados.getOrDefault(p + "z", 0);
			double dx = (px + 0.5) - x;
			double dy = (py + 0.5) - y;
			double dz = (pz + 0.5) - z;
			double d2 = dx * dx + dy * dy + dz * dz;
			if (d2 > melhor) {
				continue;
			}
			long seg = this.dados.getOrDefault(p + "s", 0);
			// Marcas antiquíssimas podem continuar existindo, mas não dominam para sempre.
			if (agora - seg > 60L * 60L * 4L) {
				continue;
			}
			melhor = d2;
			encontrada = new Marca(new BlockPos(px, py, pz),
					Tipo.deCodigo(this.dados.getOrDefault(p + "t", 1)), seg, slot);
		}
		return encontrada;
	}

	int quantidade() {
		int n = 0;
		for (int slot = 0; slot < MAX; slot++) {
			if (this.dados.containsKey("v" + slot + "_x")) {
				n++;
			}
		}
		return n;
	}

	private void salvar() {
		this.jogador.setAttached(TIPO, new HashMap<>(this.dados));
	}

	static void apagar(ServerPlayer jogador) {
		jogador.setAttached(TIPO, new HashMap<>());
	}
}
