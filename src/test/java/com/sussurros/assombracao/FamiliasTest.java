package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sussurros.assombracao.Familias.Familia;

/** A regra das famílias, sem mundo: nenhuma passa de duas nas últimas cinco saídas, e as que se tocam nunca são barradas. */
class FamiliasTest {
	@Test
	void duasNasUltimasCincoBarramATerceira() {
		assertFalse(Familias.barrada(List.of(), Familia.LUZ));
		assertFalse(Familias.barrada(List.of(Familia.LUZ), Familia.LUZ));
		assertTrue(Familias.barrada(List.of(Familia.LUZ, Familia.LUZ), Familia.LUZ), "nunca três seguidas");
		assertFalse(Familias.barrada(List.of(Familia.LUZ, Familia.LUZ), Familia.SOM), "a regra é por família");
		assertTrue(Familias.barrada(List.of(Familia.LUZ, Familia.SOM, Familia.MARCA, Familia.LUZ), Familia.LUZ),
				"não precisam ser seguidas: duas em quatro já são duas nas últimas cinco");
	}

	@Test
	void oQueSaiuHaMaisDeCincoNaoConta() {
		// As duas luzes do começo ficaram para trás: só as cinco últimas valem.
		List<Familia> ultimas = List.of(Familia.LUZ, Familia.LUZ, Familia.SOM, Familia.MARCA, Familia.CABECA, Familia.SOM, Familia.OBJETO);
		assertFalse(Familias.barrada(ultimas, Familia.LUZ));
		assertTrue(Familias.barrada(ultimas, Familia.SOM));
		assertTrue(Familias.barrada(List.of(Familia.LUZ, Familia.LUZ, Familia.SOM, Familia.MARCA, Familia.CABECA), Familia.LUZ),
				"com cinco saídas as duas primeiras ainda estão na janela");
	}

	@Test
	void oQueSeTocaNuncaEBarrado() {
		List<Familia> soObjeto = List.of(Familia.OBJETO, Familia.OBJETO, Familia.OBJETO, Familia.OBJETO, Familia.OBJETO);
		List<Familia> soCorpo = List.of(Familia.CORPO, Familia.CORPO, Familia.CORPO);
		assertFalse(Familias.barrada(soObjeto, Familia.OBJETO));
		assertFalse(Familias.barrada(soCorpo, Familia.CORPO));
	}

	/**
	 * A prova que a pesquisa pede: nenhuma família acima de 40% do que sai. Um sistema que tenta luz sempre que
	 * pode (o pior caso: uma caverna cheia de tochas) e outro que faz qualquer outra coisa quando a luz é barrada.
	 */
	@Test
	void quemTentaSempreAMesmaFamiliaFicaComUmTerco() {
		ArrayDeque<Familia> ultimas = new ArrayDeque<>();
		Map<Familia, Integer> conta = new EnumMap<>(Familia.class);
		Familia[] outras = {Familia.SOM, Familia.MARCA, Familia.CABECA, Familia.OBJETO};
		int vez = 0;
		for (int i = 0; i < 600; i++) {
			Familia f = Familias.barrada(ultimas, Familia.LUZ) ? outras[vez++ % outras.length] : Familia.LUZ;
			ultimas.addLast(f);
			while (ultimas.size() > Familias.JANELA) {
				ultimas.removeFirst();
			}
			conta.merge(f, 1, Integer::sum);
		}
		double luz = conta.get(Familia.LUZ) / 600.0;
		assertTrue(luz <= 0.40, "a luz não pode passar de 40% do que sai; ficou em " + luz);
		assertTrue(luz >= 0.30, "e a regra não a cala: fica perto de um terço; ficou em " + luz);
	}

	@Test
	void tudoOQueOModFazTemFamilia() {
		for (Evento ev : Evento.values()) {
			assertNotNull(Familias.de(ev), ev.name());
		}
		for (Pressagio pr : Pressagio.values()) {
			assertNotNull(Familias.de(pr), pr.name());
		}
		for (Perturbacao pt : Perturbacao.values()) {
			assertNotNull(Familias.de(pt), pt.name());
		}
		for (PlanoB.Tipo tipo : PlanoB.Tipo.values()) {
			assertNotNull(Familias.de(tipo), tipo.name());
		}
		// As três coisas novas da caverna são das famílias que faltavam nela.
		assertEquals(Familia.OBJETO, Familias.de(Perturbacao.PEDRINHA));
		assertEquals(Familia.CORPO, Familias.de(Perturbacao.MORCEGOS));
		assertTrue(Familias.de(Perturbacao.PEDRINHA).fisica() && Familias.de(Perturbacao.MORCEGOS).fisica());
		assertEquals(Familia.LUZ, Familias.de(Evento.LUZ_ERRADA));
	}

	/** A dívida do subsolo tenta primeiro a coisa que saiu menos vezes; a pedra no caminho fica sempre por último. */
	@Test
	void aDividaRevezaOQueSeToca() {
		assertArrayEquals(new Perturbacao[] {Perturbacao.MORCEGOS, Perturbacao.PEDRINHA, Perturbacao.TAMPA, Perturbacao.PASSAGEM_FECHADA,
				Perturbacao.O_CAMINHO_MUDOU}, Atmosfera.ordemDaDivida(0, 0, 0, 0), "no empate, a ordem de sempre");
		assertArrayEquals(new Perturbacao[] {Perturbacao.PEDRINHA, Perturbacao.TAMPA, Perturbacao.PASSAGEM_FECHADA, Perturbacao.MORCEGOS,
				Perturbacao.O_CAMINHO_MUDOU}, Atmosfera.ordemDaDivida(1, 0, 0, 0), "os morcegos já saíram: vão para o fim da fila");
		assertArrayEquals(new Perturbacao[] {Perturbacao.PASSAGEM_FECHADA, Perturbacao.TAMPA, Perturbacao.MORCEGOS, Perturbacao.PEDRINHA,
				Perturbacao.O_CAMINHO_MUDOU}, Atmosfera.ordemDaDivida(2, 2, 1, 0));
		for (Perturbacao pt : Atmosfera.ordemDaDivida(3, 1, 2, 0)) {
			assertTrue(Familias.de(pt).fisica(), pt + " paga a dívida, então tem de ser coisa que se toca");
		}
	}

	@Test
	void aRachaduraVemDepoisDeElePegarRitmo() {
		for (long descida = 0; descida < 50; descida++) {
			int n = Caverna.quebrasAteRachar(descida);
			assertTrue(n >= 4 && n <= 8, "da quarta à oitava quebra; deu " + n);
		}
		for (long seg = 0; seg < 50; seg++) {
			int n = Caverna.quantosMorcegos(seg);
			assertTrue(n >= 3 && n <= 5, "de três a cinco morcegos; deu " + n);
		}
	}
}
