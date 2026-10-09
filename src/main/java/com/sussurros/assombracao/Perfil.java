package com.sussurros.assombracao;

/**
 * Opinião lenta do Diretor sobre COMO o jogador joga. Seis traços de 0 a 100 (começam em 50),
 * salvos na Memoria. Mudam devagar: leva cerca de meia hora para mudar de opinião.
 */
final class Perfil {
	enum Traco {
		CAUTELA, LUZ, CASEIRO, EXPLORADOR, CONFRONTO, FUGA;

		String chave() {
			return "traco_" + this.name().toLowerCase();
		}
	}

	private static final double TAXA_MINUTO = 0.03;

	private Perfil() {
	}

	static int get(Memoria m, Traco t) {
		return m.get(t.chave(), 50);
	}

	static boolean alto(Memoria m, Traco t) {
		return get(m, t) > 65;
	}

	static void puxar(Memoria m, Traco t, double observado, double taxa) {
		double v = get(m, t);
		v += taxa * (observado - v);
		m.set(t.chave(), (int) Math.round(Math.max(0, Math.min(100, v))));
	}

	/** Estratégia: quanto cada evento vale para este tipo de jogador. */
	static double multiplicador(Memoria m, Evento ev) {
		double f = 1.0;
		if (alto(m, Traco.CAUTELA) && (ev == Evento.PASSO_UNICO || ev == Evento.ECO || ev == Evento.SEGUIDOR)) {
			f *= ev == Evento.SEGUIDOR ? 1.4 : 1.3;
		}
		// Quem anda atento percebe melhor o que é quase nada: o passo a mais, a sensação nas costas.
		if (alto(m, Traco.CAUTELA) && (ev == Evento.ECO_PASSOS || ev == Evento.VIGIA)) {
			f *= 1.3;
		}
		if (alto(m, Traco.LUZ) && ev.categoria == Evento.Categoria.AMBIENTE) {
			f *= 1.4;
		}
		if (alto(m, Traco.CASEIRO) && (ev == Evento.PORTA || ev == Evento.BATIDA)) {
			f *= 1.5;
		}
		if (alto(m, Traco.EXPLORADOR)) {
			if (ev == Evento.PRESENCA) {
				f *= 1.3;
			}
			if (ev == Evento.PORTA || ev == Evento.BATIDA) {
				f *= 0.8;
			}
		}
		if (alto(m, Traco.CONFRONTO)) {
			if (ev.categoria == Evento.Categoria.VISAO) {
				f *= 0.7;
			}
			if (ev.categoria == Evento.Categoria.MENTE) {
				f *= 1.3;
			}
		}
		if (alto(m, Traco.FUGA)) {
			if (ev.categoria == Evento.Categoria.VISAO) {
				f *= 1.2;
			}
			if (ev == Evento.ATRAS) {
				f *= 1.2;
			}
			if (ev == Evento.SEGUIDOR) {
				f *= 1.15;
			}
		}
		return f;
	}

	/** Fecha o minuto: transforma o que foi observado em ajuste lento dos traços. */
	static void fecharMinuto(Memoria m, EstadoJogador e) {
		if (e.minEscuro >= 10) {
			double obs = 100.0 * e.minEscuroCauteloso / e.minEscuro + e.minOlhadas * 10.0;
			puxar(m, Traco.CAUTELA, Math.min(100, obs), TAXA_MINUTO);
		}
		if (e.minNoiteOuSubsolo >= 10) {
			puxar(m, Traco.LUZ, 100.0 * e.minComLuz / e.minNoiteOuSubsolo, TAXA_MINUTO);
		}
		if (e.minNoite >= 10) {
			puxar(m, Traco.CASEIRO, 100.0 * e.minEmCasa / e.minNoite, TAXA_MINUTO);
		}
		puxar(m, Traco.EXPLORADOR, Math.min(100, e.minChunksNovos * 25.0), TAXA_MINUTO);

		e.minEscuro = 0;
		e.minEscuroCauteloso = 0;
		e.minNoiteOuSubsolo = 0;
		e.minComLuz = 0;
		e.minNoite = 0;
		e.minEmCasa = 0;
		e.minOlhadas = 0;
		e.minChunksNovos = 0;
	}

	static String resumo(Memoria m) {
		StringBuilder sb = new StringBuilder();
		for (Traco t : Traco.values()) {
			sb.append(t.name().toLowerCase()).append('=').append(get(m, t)).append(' ');
		}
		return sb.toString().trim();
	}
}
