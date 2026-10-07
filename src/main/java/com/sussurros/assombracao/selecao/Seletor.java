package com.sussurros.assombracao.selecao;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.sussurros.assombracao.Evento;

public final class Seletor {
	private Seletor() {
	}

	/** Nenhuma categoria pode ficar com mais de "limite" da probabilidade total. */
	public static void aplicarTeto(List<Evento> candidatos, List<Double> pesos, double limite) {
		for (int volta = 0; volta < 2; volta++) {
			EnumMap<Evento.Categoria, Double> somas = new EnumMap<>(Evento.Categoria.class);
			double total = 0;
			for (int i = 0; i < candidatos.size(); i++) {
				somas.merge(candidatos.get(i).categoria, pesos.get(i), Double::sum);
				total += pesos.get(i);
			}
			if (somas.size() < 2 || total <= 0) {
				return;
			}
			for (Map.Entry<Evento.Categoria, Double> en : somas.entrySet()) {
				double s = en.getValue();
				if (s / total > limite) {
					double fator = limite * (total - s) / ((1 - limite) * s);
					for (int i = 0; i < candidatos.size(); i++) {
						if (candidatos.get(i).categoria == en.getKey()) {
							pesos.set(i, pesos.get(i) * fator);
						}
					}
					break;
				}
			}
		}
	}

	public static double taxaExploracao(double base, Collection<Double> confiancasRecentes, boolean escalando) {
		if (confiancasRecentes.size() >= 3) {
			double soma = 0;
			for (double c : confiancasRecentes) {
				soma += c;
			}
			double media = soma / confiancasRecentes.size();
			if (media < 0.15) {
				return Math.max(base, 0.25);
			}
			if (media >= 0.4 && escalando) {
				return 0.05;
			}
		}
		return base;
	}

	public static double antiRepeticao(Collection<Evento> recentes, Evento ev) {
		double f = 1;
		int i = 0;
		for (Evento recente : recentes) {
			if (recente == ev) {
				f *= i == 0 ? 0.15 : (i == 1 ? 0.4 : 0.7);
			}
			i++;
		}
		return f;
	}

	/** Evita repetir a mesma "lógica": o mesmo tipo de medo seguido do mesmo tipo de medo. */
	public static double aversaoSequencia(@Nullable Evento ultimo, Collection<String> pares, Evento ev) {
		if (ultimo == null) {
			return 1;
		}
		String par = ultimo.categoria + ">" + ev.categoria;
		int vezes = 0;
		for (String s : pares) {
			if (s.equals(par)) {
				vezes++;
			}
		}
		return Math.max(0.25, Math.pow(0.5, vezes));
	}

	public static double pesoIntensidade(int intensidade, double alvo) {
		double z = (intensidade - alvo) / 10.0;
		return Math.max(0.15, Math.exp(-z * z));
	}

	public static int sortearIndice(List<Double> pesos, double sorteio) {
		int escolhido = pesos.size() - 1;
		for (int i = 0; i < pesos.size(); i++) {
			sorteio -= pesos.get(i);
			if (sorteio < 0) {
				escolhido = i;
				break;
			}
		}
		return escolhido;
	}

	public static int portao(int intensidade) {
		return intensidade <= 10 ? 20 : intensidade <= 20 ? 40 : intensidade <= 30 ? 55 : 70;
	}
}
