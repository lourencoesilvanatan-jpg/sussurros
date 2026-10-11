package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Quando a cena da janela pode acontecer: a fase, e as chances dentro de uma estadia em casa. */
class CenaDoOutroLadoDoVidroTest {
	@Test
	void naFaseDoisSoDepoisDoPrimeiroContato() {
		assertFalse(CenaDoOutroLadoDoVidro.faseServe(0, true));
		assertFalse(CenaDoOutroLadoDoVidro.faseServe(1, true));
		assertFalse(CenaDoOutroLadoDoVidro.faseServe(2, false), "na fase 2, sem o primeiro contato, a janela espera");
		assertTrue(CenaDoOutroLadoDoVidro.faseServe(2, true));
		assertTrue(CenaDoOutroLadoDoVidro.faseServe(3, false), "da fase 3 em diante nada mais espera o contato");
		assertTrue(CenaDoOutroLadoDoVidro.faseServe(4, true));
	}

	@Test
	void aPrimeiraChanceVemAosQuarentaECincoSegundosEmCasa() {
		assertFalse(CenaDoOutroLadoDoVidro.horaDaChance(0));
		assertFalse(CenaDoOutroLadoDoVidro.horaDaChance(44));
		assertTrue(CenaDoOutroLadoDoVidro.horaDaChance(45));
		assertFalse(CenaDoOutroLadoDoVidro.horaDaChance(46));
		assertFalse(CenaDoOutroLadoDoVidro.horaDaChance(120), "a chance dos dois minutos em ponto deixou de existir");
		assertTrue(CenaDoOutroLadoDoVidro.horaDaChance(165));
		assertTrue(CenaDoOutroLadoDoVidro.horaDaChance(285));
	}
}
