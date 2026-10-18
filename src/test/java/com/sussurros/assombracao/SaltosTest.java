package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** As contas dos saltos da caçada, sem mundo: o que destranca, quanto ele espera, onde não cai, e a vida que sobra. */
class SaltosTest {
	@Test
	void naPrimeiraCacadaNadaEstaDestrancado() {
		Saltos.Liberacao l = Saltos.liberacao(0, 0, 0, 0);
		assertFalse(l.deLonge());
		assertFalse(l.semCaminho());
	}

	@Test
	void oTruqueFuncionaDuasVezes() {
		// Fugir para longe: duas caçadas funcionam, na terceira o atalho de longe vale.
		assertFalse(Saltos.liberacao(1, 1, 0, 0).deLonge());
		assertTrue(Saltos.liberacao(2, 2, 0, 0).deLonge());
		// Ficar onde não há caminho: o mesmo, para atravessar (e o atalho do anel).
		assertFalse(Saltos.liberacao(1, 0, 1, 0).semCaminho());
		assertTrue(Saltos.liberacao(2, 0, 2, 0).semCaminho());
		// Cada truque destranca só a sua resposta.
		assertFalse(Saltos.liberacao(2, 2, 0, 0).semCaminho());
		assertFalse(Saltos.liberacao(2, 0, 2, 0).deLonge());
	}

	@Test
	void asTravessiasDeAntesContamComoTruque() {
		// Quem já atravessou numa versão anterior (o dono, uma vez em 12/10/2026) tem um truque a menos por fazer.
		assertFalse(Saltos.liberacao(3, 0, 0, 1).semCaminho());
		assertTrue(Saltos.liberacao(3, 0, 1, 1).semCaminho());
	}

	@Test
	void muitasCacadasDestrancamTudo() {
		assertFalse(Saltos.liberacao(Saltos.CACADAS_PARA_TUDO - 1, 0, 0, 0).deLonge());
		Saltos.Liberacao l = Saltos.liberacao(Saltos.CACADAS_PARA_TUDO, 0, 0, 0);
		assertTrue(l.deLonge() && l.semCaminho());
	}

	@Test
	void quantoMaisPertoMaisEspera() {
		double v = 4.9;
		double alcance = 2.4;
		int perto = Saltos.esperaDepois(3.0, v, alcance);
		int meio = Saltos.esperaDepois(12.0, v, alcance);
		int longe = Saltos.esperaDepois(40.0, v, alcance);
		assertTrue(perto >= meio && meio >= longe, perto + " / " + meio + " / " + longe);
		// Sempre de dois a três segundos parado.
		for (double d = 0; d <= 60; d += 0.5) {
			for (double vel : new double[] {2.0, 5.3, 6.9 * 1.2}) {
				int e = Saltos.esperaDepois(d, vel, alcance);
				assertTrue(e >= Saltos.ESPERA_MINIMA && e <= Saltos.ESPERA_MAXIMA, "espera " + e + " a " + d + " blocos");
			}
		}
		// Perto, a espera é a máxima; e a janela nunca é menor que a espera.
		assertEquals(Saltos.ESPERA_MAXIMA, perto);
		assertTrue(Saltos.janela(perto, 3.0, v, alcance) >= perto);
	}

	@Test
	void aJanelaDeUmAtalhoTemQuatroSegundosOuMais() {
		// O atalho de longe cai de 12 a 16 blocos; na velocidade máxima que a caçada chega a pedir (6,9 vezes 1,2, desde a alpha29).
		for (double d = Saltos.DISTANCIA_MINIMA; d <= Saltos.DISTANCIA_MINIMA + 4; d += 0.5) {
			int espera = Saltos.esperaDepois(d, 6.9 * 1.2, 2.4);
			assertTrue(Saltos.janela(espera, d, 6.9 * 1.2, 2.4) >= 80, "janela curta a " + d + " blocos");
		}
	}

	@Test
	void nasCostasEOQueFicaAtras() {
		// Virado para o sul (+z).
		assertTrue(Saltos.nasCostas(0, 1, 0, -10), "bem atrás");
		assertTrue(Saltos.nasCostas(0, 1, 3, -10), "atrás, um pouco de lado");
		assertFalse(Saltos.nasCostas(0, 1, 10, 0), "do lado");
		assertFalse(Saltos.nasCostas(0, 1, 10, -3), "do lado, um pouco para trás");
		assertFalse(Saltos.nasCostas(0, 1, 0, 10), "na frente");
		assertFalse(Saltos.nasCostas(0, 0, 0, -10), "sem direção, não diz nada");
		// A borda: sessenta graus do ponto bem atrás, ou seja, cento e vinte da frente, dos dois lados.
		for (int lado : new int[] {1, -1}) {
			double dentro = Math.toRadians(180 - Saltos.COSTAS_GRAUS + 1) * lado;
			double fora = Math.toRadians(180 - Saltos.COSTAS_GRAUS - 1) * lado;
			assertTrue(Saltos.nasCostas(0, 1, Math.sin(dentro), Math.cos(dentro)), "a 121 graus da frente já é costas");
			assertFalse(Saltos.nasCostas(0, 1, Math.sin(fora), Math.cos(fora)), "a 119 graus da frente ainda é lado");
		}
	}

	@Test
	void fugir() {
		assertFalse(Saltos.fugiu(20, 8), "perto ainda");
		assertFalse(Saltos.fugiu(30, 20), "longe, mas quase onde já estava");
		assertTrue(Saltos.fugiu(33, 19), "o que o dono fez em 12/10/2026: de 19 para 33 blocos no aviso");
	}

	@Test
	void aCapturaNaoDeixaAbaixoDaMetade() {
		assertEquals(12.0F, Saltos.vidaDepoisDaCaptura(20.0F, 20.0F), 1.0E-4);
		assertEquals(9.0F, Saltos.vidaDepoisDaCaptura(14.0F, 18.0F), 1.0E-4);
		// O caso da segunda captura de 12/10/2026: 14 de vida, 14 de máximo (três marcas). Ficou com 6; agora 7.
		assertEquals(7.0F, Saltos.vidaDepoisDaCaptura(14.0F, 14.0F), 1.0E-4);
		// Quem já está abaixo da metade não piora (e não é curado).
		assertEquals(5.0F, Saltos.vidaDepoisDaCaptura(5.0F, 20.0F), 1.0E-4);
	}
}
