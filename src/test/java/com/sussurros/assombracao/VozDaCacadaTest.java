package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sussurros.assombracao.VozDaCacada.Momento;

/** As regras da voz na caçada, sem mundo: rara, nunca em cima de outro sinal, e a de longe só pela distância. */
class VozDaCacadaTest {
	@Test
	void noMaximoDuasPorCacada() {
		assertTrue(VozDaCacada.pode(Momento.FERIDO, 1, -1, -1, 0.0));
		assertFalse(VozDaCacada.pode(Momento.FERIDO, VozDaCacada.MAXIMO_POR_CACADA, -1, -1, 0.0));
		assertFalse(VozDaCacada.pode(Momento.INICIO, VozDaCacada.MAXIMO_POR_CACADA, -1, -1, 0.0));
	}

	@Test
	void quinzeSegundosEntreUmaEOutraMenosNoGolpe() {
		assertFalse(VozDaCacada.pode(Momento.REENCONTRO, 1, VozDaCacada.INTERVALO - 1, -1, 0.0));
		assertTrue(VozDaCacada.pode(Momento.REENCONTRO, 1, VozDaCacada.INTERVALO, -1, 0.0));
		// O golpe é resposta ao que o jogador acabou de fazer: vem na hora.
		assertTrue(VozDaCacada.pode(Momento.FERIDO, 1, 10, -1, 0.0));
	}

	@Test
	void nuncaEmCimaDeOutroSinal() {
		for (Momento m : Momento.values()) {
			assertFalse(VozDaCacada.pode(m, 0, -1, VozDaCacada.LONGE_DOS_SINAIS - 1, 0.0), m + " em cima de um sinal");
			assertTrue(VozDaCacada.pode(m, 0, -1, VozDaCacada.LONGE_DOS_SINAIS, 0.0), m + " longe dos sinais");
		}
	}

	@Test
	void oInicioEOReencontroSaoAsVezesOGolpeSempre() {
		assertTrue(VozDaCacada.pode(Momento.INICIO, 0, -1, -1, 0.0));
		assertFalse(VozDaCacada.pode(Momento.INICIO, 0, -1, -1, 0.99));
		assertTrue(VozDaCacada.pode(Momento.REENCONTRO, 0, -1, -1, 0.0));
		assertFalse(VozDaCacada.pode(Momento.REENCONTRO, 0, -1, -1, 0.99));
		assertTrue(VozDaCacada.pode(Momento.FERIDO, 0, -1, -1, 0.99));
	}

	@Test
	void aDeLongeNaoFicaMaisForte() {
		assertFalse(VozDaCacada.deLonge(8.0));
		assertTrue(VozDaCacada.deLonge(20.0));
		assertEquals(1.0F, VozDaCacada.volume(5.0), 1.0E-6);
		// De longe o volume só cresce para o som alcançar o jogador; nunca abaixo de 1, e sempre com folga de alcance.
		for (double d = VozDaCacada.DE_LONGE; d <= 64.0; d += 1.0) {
			float v = VozDaCacada.volume(d);
			assertTrue(v >= 1.0F);
			assertTrue(16.0 * v >= d * 1.2, "a " + d + " blocos o som não chegaria");
		}
	}

	@Test
	void aAlturaVariaPouco() {
		assertEquals(0.93F, VozDaCacada.altura(0.0), 1.0E-6);
		assertEquals(1.05F, VozDaCacada.altura(1.0), 1.0E-6);
		assertEquals(1.05F, VozDaCacada.altura(7.0), 1.0E-6);
		assertEquals(0.93F, VozDaCacada.altura(-1.0), 1.0E-6);
	}
}
