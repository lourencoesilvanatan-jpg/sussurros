package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * O caminho recente do jogador (v0.4.2).
 *
 * Um ponto a cada 2 s, só quando ele andou pelo menos 2 blocos desde o último, guardando
 * no máximo os últimos ~6 minutos. É isso que deixa o Hóspede aparecer ONDE VOCÊ ESTEVE,
 * e não num ponto sorteado em volta de você: "ele veio atrás de mim".
 */
final class Rastro {
	record Ponto(double x, double y, double z, long seg) {
	}

	private static final int MAX_PONTOS = 180;
	private static final double PASSO_MINIMO = 2.0;

	private final ArrayDeque<Ponto> pontos = new ArrayDeque<>();

	void registrar(double x, double y, double z, long seg) {
		Ponto ultimo = this.pontos.peekLast();
		if (ultimo != null) {
			double dx = x - ultimo.x();
			double dz = z - ultimo.z();
			if (dx * dx + dz * dz < PASSO_MINIMO * PASSO_MINIMO) {
				return;
			}
		}
		this.pontos.addLast(new Ponto(x, y, z, seg));
		while (this.pontos.size() > MAX_PONTOS) {
			this.pontos.removeFirst();
		}
	}

	/** Pontos com idade entre idadeMin e idadeMax segundos, do mais antigo para o mais novo. */
	List<Ponto> comIdade(long agora, int idadeMin, int idadeMax) {
		List<Ponto> lista = new ArrayList<>();
		for (Ponto pt : this.pontos) {
			long idade = agora - pt.seg();
			if (idade >= idadeMin && idade <= idadeMax) {
				lista.add(pt);
			}
		}
		return lista;
	}

	int tamanho() {
		return this.pontos.size();
	}

	/** Depois de um teleporte ou respawn, os pontos antigos deixam de ser "por onde ele veio". */
	void limpar() {
		this.pontos.clear();
	}
}
