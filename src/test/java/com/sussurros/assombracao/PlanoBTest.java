package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Qual sinal fecha uma construção que acabou sem ninguém: depende do lugar e do que saiu da última vez. */
class PlanoBTest {
	@Test
	void aVistaOQueSeVeVemPrimeiro() {
		assertEquals(PlanoB.Tipo.PO, PlanoB.escolherTipo(true, true, true, null));
		assertEquals(PlanoB.Tipo.TOCHA, PlanoB.escolherTipo(false, true, true, null), "sem teto, a tocha");
		assertEquals(PlanoB.Tipo.PASSOS, PlanoB.escolherTipo(false, false, true, null), "sem teto e sem tocha, os passos");
	}

	@Test
	void foraDaVistaOQueSeOuveVemPrimeiro() {
		assertEquals(PlanoB.Tipo.PASSOS, PlanoB.escolherTipo(true, true, false, null), "pó que ninguém vê não é sinal");
		assertEquals(PlanoB.Tipo.PASSOS, PlanoB.escolherTipo(true, false, false, null));
		assertEquals(PlanoB.Tipo.TOCHA, PlanoB.escolherTipo(true, true, false, PlanoB.Tipo.PASSOS));
	}

	@Test
	void nuncaOMesmoDuasVezesSeguidasHavendoEscolha() {
		assertEquals(PlanoB.Tipo.TOCHA, PlanoB.escolherTipo(true, true, true, PlanoB.Tipo.PO));
		assertEquals(PlanoB.Tipo.PASSOS, PlanoB.escolherTipo(true, false, true, PlanoB.Tipo.PO));
		assertEquals(PlanoB.Tipo.PO, PlanoB.escolherTipo(true, true, true, PlanoB.Tipo.TOCHA));
		assertEquals(PlanoB.Tipo.PO, PlanoB.escolherTipo(true, true, true, PlanoB.Tipo.PASSOS));
	}

	@Test
	void semEscolhaRepete() {
		assertEquals(PlanoB.Tipo.PASSOS, PlanoB.escolherTipo(false, false, true, PlanoB.Tipo.PASSOS));
		assertEquals(PlanoB.Tipo.PASSOS, PlanoB.escolherTipo(true, false, false, PlanoB.Tipo.PASSOS), "fora da vista o pó não entra");
	}
}
