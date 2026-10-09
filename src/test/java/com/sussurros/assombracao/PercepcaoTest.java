package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Os cones calculados a partir do campo de visão do jogador. */
class PercepcaoTest {
	private static double graus(double cosseno) {
		return Math.toDegrees(Math.acos(cosseno));
	}

	@Test
	void fov70Em16por9FicaPertoDosConesAntigos() {
		double[] c = Percepcao.cones(70, 16.0 / 9.0);
		// Antes: "percebeu" a ~45,6 graus e "seguro" a ~55 graus.
		assertEquals(47.0, graus(c[0]), 2.0);
		assertTrue(graus(c[1]) >= 55.0, "o cone seguro não pode ser mais estreito que o canto da tela");
		assertTrue(graus(c[1]) <= 66.0);
	}

	@Test
	void fovMaiorAlargaOsDoisCones() {
		double[] normal = Percepcao.cones(70, 16.0 / 9.0);
		double[] largo = Percepcao.cones(110, 16.0 / 9.0);
		assertTrue(graus(largo[0]) > graus(normal[0]) + 10);
		assertTrue(graus(largo[1]) > graus(normal[1]) + 10);
	}

	@Test
	void oConeSeguroSempreCobreOConeDePercepcao() {
		for (double fov = 30; fov <= 140; fov += 10) {
			for (double proporcao : new double[] {1.0, 4.0 / 3.0, 16.0 / 9.0, 21.0 / 9.0}) {
				double[] c = Percepcao.cones(fov, proporcao);
				assertTrue(graus(c[1]) > graus(c[0]), "fov=" + fov + " proporcao=" + proporcao);
				assertTrue(graus(c[1]) <= 86.01);
			}
		}
	}
}
