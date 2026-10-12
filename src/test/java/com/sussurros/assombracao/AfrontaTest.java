package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** As contas da afronta, sem mundo: quanto esfria, onde para, quanto pesa e quando ele vem. */
class AfrontaTest {
	@Test
	void caiPelaMetadeACadaDia() {
		assertEquals(80, Afronta.decair(80, 0));
		assertEquals(40, Afronta.decair(80, 1));
		assertEquals(20, Afronta.decair(80, 2));
		assertEquals(10, Afronta.decair(80, 3));
		assertEquals(0, Afronta.decair(100, 7), "depois de uma semana não sobra nada");
		assertEquals(0, Afronta.decair(100, 400), "muitos dias não podem virar conta estranha");
		assertEquals(80, Afronta.decair(80, -3), "o relógio do mundo voltou: não mexe");
	}

	@Test
	void ficaEntreZeroECem() {
		assertEquals(25, Afronta.somar(0, Afronta.Motivo.BATEU.pontos));
		assertEquals(100, Afronta.somar(90, Afronta.Motivo.BATEU.pontos));
		assertEquals(0, Afronta.somar(10, Afronta.Motivo.OFERECEU.pontos));
		assertEquals(7, Afronta.somar(10, Afronta.Motivo.DEIXOU.pontos));
	}

	@Test
	void asDuasMaos() {
		// O que ele faz de propósito soma; o que acalma tira. Nenhum motivo é neutro.
		for (Afronta.Motivo motivo : Afronta.Motivo.values()) {
			assertTrue(motivo.pontos != 0, motivo + " não pode ser neutro");
		}
		assertTrue(Afronta.Motivo.BATEU.pontos > Afronta.Motivo.FECHOU.pontos, "bater é o que mais pesa");
		assertTrue(Afronta.Motivo.OFERECEU.pontos < 0 && Afronta.Motivo.CONSULTOU.pontos < 0 && Afronta.Motivo.DEIXOU.pontos < 0);
		// Um golpe só não chama a visita; dois, no mesmo dia, chamam.
		assertTrue(Afronta.somar(0, Afronta.Motivo.BATEU.pontos) < Afronta.IRRITADO);
		assertTrue(Afronta.somar(Afronta.Motivo.BATEU.pontos, Afronta.Motivo.BATEU.pontos) >= Afronta.IRRITADO);
	}

	@Test
	void pesaNaPortaEUmPoucoNaPresenca() {
		assertEquals(1.0, Afronta.peso(0, Evento.BATIDA), 1.0E-9);
		assertEquals(1.0, Afronta.peso(100, Evento.PASSOS), 1.0E-9, "o resto do sorteio não muda");
		assertTrue(Afronta.peso(100, Evento.BATIDA) > 4.0 && Afronta.peso(100, Evento.PORTA) > 4.0, "no máximo, a porta pesa quatro vezes mais");
		assertEquals(2.0, Afronta.peso(100, Evento.PRESENCA), 1.0E-9);
		assertTrue(Afronta.peso(40, Evento.BATIDA) > Afronta.peso(20, Evento.BATIDA), "cresce com o número");
	}

	@Test
	void eleVemNaPrimeiraNoiteEmCasaUmaVezPorNoite() {
		assertTrue(Afronta.deveVisitar(40, true, true, 30, 4, 5, true));
		assertFalse(Afronta.deveVisitar(39, true, true, 600, 4, 5, true), "abaixo de quarenta ele não vem");
		assertFalse(Afronta.deveVisitar(90, false, true, 600, 4, 5, true), "só de noite");
		assertFalse(Afronta.deveVisitar(90, true, false, 600, 4, 5, true), "só com ele em casa");
		assertFalse(Afronta.deveVisitar(90, true, true, 29, 4, 5, true), "espera meio minuto de casa");
		assertFalse(Afronta.deveVisitar(90, true, true, 600, 5, 5, true), "uma vez por noite");
		assertFalse(Afronta.deveVisitar(90, true, true, 600, 4, 5, false), "não por cima de outra coisa");
	}
}
