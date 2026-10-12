package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** O que acontece quando o jogador deita: o sorteio de sempre, a carta, e a primeira ida garantida. */
class AvessoTest {
	private static final float SEM_SORTE = 0.99F;
	private static final float COM_SORTE = 0.01F;

	@Test
	void quemNuncaFoiVaiNaTerceiraNoiteMesmoSemSorte() {
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(0, false, 100, 1, SEM_SORTE));
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(0, false, 100, 2, SEM_SORTE));
		assertEquals(Avesso.AoDeitar.GARANTIDA, Avesso.decidir(0, false, 100, 3, SEM_SORTE));
		assertEquals(Avesso.AoDeitar.GARANTIDA, Avesso.decidir(0, false, 100, 7, SEM_SORTE), "se saiu da cama antes da hora, a seguinte também leva");
	}

	@Test
	void antesDaTerceiraNoiteOSorteioContinuaValendo() {
		assertEquals(Avesso.AoDeitar.SORTEIO, Avesso.decidir(0, false, 100, 1, COM_SORTE));
		assertEquals(Avesso.AoDeitar.SORTEIO, Avesso.decidir(0, false, 100, 2, 0.14F));
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(0, false, 100, 2, 0.15F), "a primeira é quinze em cem");
	}

	@Test
	void aGarantiaESoDaPrimeiraVisita() {
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(1, false, 10, 3, SEM_SORTE));
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(4, false, 10, 30, SEM_SORTE));
		assertEquals(Avesso.AoDeitar.SORTEIO, Avesso.decidir(1, false, 3, 3, 0.07F));
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(1, false, 3, 3, 0.08F), "as seguintes são oito em cem");
		assertEquals(Avesso.AoDeitar.NADA, Avesso.decidir(1, false, 2, 3, COM_SORTE), "menos de três dias depois da última, nem com sorte");
	}

	@Test
	void aCartaPassaPorCimaDeTudo() {
		assertEquals(Avesso.AoDeitar.CARTA, Avesso.decidir(0, true, 100, 1, SEM_SORTE));
		assertEquals(Avesso.AoDeitar.CARTA, Avesso.decidir(2, true, 0, 9, SEM_SORTE));
	}

	@Test
	void oSorteioSoEGastoQuandoVale() {
		assertTrue(Avesso.dependeDoSorteio(0, false, 100, 1));
		assertTrue(Avesso.dependeDoSorteio(1, false, 3, 3));
		assertFalse(Avesso.dependeDoSorteio(0, false, 100, 3), "na noite garantida não há o que sortear");
		assertFalse(Avesso.dependeDoSorteio(0, true, 100, 1), "com a carta também não");
		assertFalse(Avesso.dependeDoSorteio(1, false, 1, 3), "nem dentro dos três dias");
	}
}
