package com.sussurros.assombracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Casos tirados de um log de jogo real (ver PESQUISA-E-ANALISE.md, itens 3.2, 3.5 e 3.11).
 *
 * Convenções: uma amostra a cada 5 ticks (0,25 s); 12 amostras de baseline e 12 de janela.
 * Yaw 0 olha para +Z, yaw 90 olha para -X, yaw -90 olha para +X.
 */
class LeituraTest {
	private static final int PASSO = 5;

	/** Guarda o estado de um jogador de mentira e vai alimentando a Leitura. */
	private static final class Jogador {
		final Leitura leitura = new Leitura();
		double x;
		double z;
		float yaw;
		long tick;

		/** Anda n amostras com a velocidade (blocos/s) na direção (dx, dz), olhando para yaw. */
		Jogador andar(int n, double vel, double dx, double dz, float yaw, boolean correndo) {
			double passo = vel * PASSO / 20.0;
			for (int i = 0; i < n; i++) {
				this.x += dx * passo;
				this.z += dz * passo;
				this.yaw = yaw;
				this.tick += PASSO;
				this.leitura.amostrar(this.x, this.z, yaw, correndo, false, this.tick);
			}
			return this;
		}

		Jogador evento(double fonteX, double fonteZ) {
			this.leitura.iniciar(Evento.ECO, true, fonteX, fonteZ, 0.6, this.tick, "teste");
			return this;
		}
	}

	@Test
	void virar58GrausEOlharParaAFonteContaComoReacao() {
		// Log, 1701 s: LUZ_ERRADA, giro=58, olhou=true e mesmo assim c=0.00 (o corte era um degrau em 60°).
		Jogador j = new Jogador().andar(12, 0.7, 0, 1, 0, false);
		// Fonte a 20 blocos, 58° à direita de quem olha para +Z (yaw -58 olha para lá).
		double rad = Math.toRadians(58);
		j.evento(j.x + Math.sin(rad) * 20, j.z + Math.cos(rad) * 20);
		j.andar(1, 0.2, 0, 1, 0, false);
		j.andar(11, 0.2, 0, 1, -58, false);

		Leitura.Resultado r = j.leitura.avaliar();

		assertFalse(r.semDados());
		assertTrue(r.confianca() >= 0.45, "c=" + r.confianca());
		assertTrue(r.percebeu());
		assertTrue(r.sinais().contains("olhou=true"), r.sinais());
	}

	@Test
	void giroPequenoNaoViraPercepcao() {
		// 35° é pouco acima do mínimo: vale quase nada e não prova que ele percebeu.
		Jogador j = new Jogador().andar(12, 0.7, 0, 1, 0, false);
		double rad = Math.toRadians(35);
		j.evento(j.x + Math.sin(rad) * 20, j.z + Math.cos(rad) * 20);
		j.andar(1, 0.7, 0, 1, 0, false);
		j.andar(11, 0.7, 0, 1, -35, false);

		Leitura.Resultado r = j.leitura.avaliar();

		assertTrue(r.confianca() < 0.15, "c=" + r.confianca());
		assertFalse(r.percebeu());
	}

	@Test
	void quemJaAndavaParaAFonteNaoInvestigou() {
		// Log, 1052 s: um SINAL_DISTANTE inaudível "investigado" por quem só voltava pelo próprio caminho.
		Jogador j = new Jogador().andar(12, 4.0, 0, 1, 0, false);
		j.evento(j.x, j.z + 25);
		j.andar(12, 4.0, 0, 1, 0, false);

		Leitura.Resultado r = j.leitura.avaliar();

		assertFalse(r.investigou());
		assertFalse(r.fugiu());
		assertFalse(r.percebeu());
		assertEquals(0.0, r.confianca(), 1.0E-9);
	}

	@Test
	void mudarDeRumoParaAFonteEInvestigar() {
		// Andava para +X; depois do evento vira e vai até a fonte, que está em +Z.
		Jogador j = new Jogador().andar(12, 4.0, 1, 0, -90, false);
		j.evento(j.x, j.z + 20);
		j.andar(12, 4.0, 0, 1, 0, false);

		Leitura.Resultado r = j.leitura.avaliar();

		assertTrue(r.investigou());
		assertFalse(r.fugiu());
		assertEquals(1.0, r.engajamento(), 1.0E-9);
	}

	@Test
	void acelerarNaDirecaoDaFonteNaoEFuga() {
		// Log, 823 s: "fugiu=true ... investigou=true" na mesma linha.
		Jogador j = new Jogador().andar(12, 3.6, 1, 0, -90, false);
		j.evento(j.x, j.z + 20);
		j.andar(12, 5.6, 0, 1, 0, true);

		Leitura.Resultado r = j.leitura.avaliar();

		assertFalse(r.fugiu(), r.sinais());
		assertTrue(r.investigou(), r.sinais());
	}

	@Test
	void comecarACorrerParaLongeDaFonteEFuga() {
		// Fonte atrás (em -Z); ele andava para +Z e dispara a correr na mesma direção.
		Jogador j = new Jogador().andar(12, 2.3, 0, 1, 0, false);
		j.evento(j.x, j.z - 15);
		j.andar(12, 5.6, 0, 1, 0, true);

		Leitura.Resultado r = j.leitura.avaliar();

		assertTrue(r.fugiu());
		assertTrue(r.sinais().contains("daFonte=true"), r.sinais());
		assertTrue(r.confianca() >= 0.45, "c=" + r.confianca());
	}

	@Test
	void comecarACorrerDeLadoEEvidenciaFraca() {
		// Log, 754 s: SINAL_DISTANTE inaudível, sem giro, só "vel 2.3->4.6", valeu c=0.40.
		// Correr de lado, sem se afastar da fonte, agora fica abaixo de 0,30 (não conta como reação).
		Jogador j = new Jogador().andar(12, 2.3, 1, 0, -90, false);
		j.evento(j.x + 6, j.z + 28);
		j.andar(12, 4.6, 1, 0, -90, true);

		Leitura.Resultado r = j.leitura.avaliar();

		assertTrue(r.fugiu());
		assertTrue(r.confianca() < 0.30, "c=" + r.confianca());
	}

	@Test
	void quemJaCorriaNaoFugiu() {
		Jogador j = new Jogador().andar(12, 5.6, 0, 1, 0, true);
		j.evento(j.x, j.z - 15);
		j.andar(12, 5.6, 0, 1, 0, true);

		Leitura.Resultado r = j.leitura.avaliar();

		assertFalse(r.fugiu());
		assertEquals(0.0, r.confianca(), 1.0E-9);
	}

	@Test
	void quemJaCorriaEPassaAPularNaoFugiu() {
		// Log de 08/10, 4089 s: PASSOS, "vel 5.1->6.7", fugiu=true daFonte=true, c=0.52, e o Diretor escalou.
		// Ele já corria; só começou a correr pulando.
		Jogador j = new Jogador().andar(12, 5.6, 0, 1, 0, true);
		j.evento(j.x, j.z - 4);
		j.andar(12, 7.2, 0, 1, 0, true);

		Leitura.Resultado r = j.leitura.avaliar();

		assertFalse(r.fugiu(), r.sinais());
		assertEquals(0.0, r.confianca(), 1.0E-9);
	}

	@Test
	void pararDeRepenteContaComoCongelar() {
		Jogador j = new Jogador().andar(12, 4.0, 0, 1, 0, false);
		j.evento(j.x + 10, j.z);
		j.andar(12, 0.0, 0, 1, 0, false);

		Leitura.Resultado r = j.leitura.avaliar();

		assertTrue(r.sinais().contains("congelou=true"), r.sinais());
		assertEquals(0.5, r.confianca(), 1.0E-9);
	}

	@Test
	void paradoAntesEDepoisNaoEnsinaNada() {
		Jogador j = new Jogador().andar(12, 0.0, 0, 1, 0, false);
		j.evento(j.x + 10, j.z);
		j.andar(12, 0.0, 0, 1, 0, false);

		Leitura.Resultado r = j.leitura.avaliar();

		assertTrue(r.semDados());
	}

	@Test
	void teleporteCancelaALeituraEAvisaQuemChamou() {
		// Log, 1557 s: uma leitura atravessou um teleporte e registrou "vel 35.9".
		Jogador j = new Jogador().andar(12, 4.0, 0, 1, 0, false);
		j.evento(j.x, j.z - 10);
		j.andar(3, 4.0, 0, 1, 0, false);
		assertTrue(j.leitura.pendente());

		j.tick += PASSO;
		boolean salto = j.leitura.amostrar(j.x + 100, j.z, 0, false, false, j.tick);

		assertTrue(salto);
		assertFalse(j.leitura.pendente());
	}

	@Test
	void buracoDeAmostrasTambemEUmSalto() {
		// Voltar de outra dimensão: o jogador ficou segundos sem ser amostrado.
		Jogador j = new Jogador().andar(12, 4.0, 0, 1, 0, false);

		j.tick += 20 * 30;
		boolean salto = j.leitura.amostrar(j.x, j.z, 0, false, false, j.tick);

		assertTrue(salto);
	}

	@Test
	void andarNormalmenteNaoEUmSalto() {
		Jogador j = new Jogador().andar(11, 5.6, 0, 1, 0, true);

		j.tick += PASSO;
		// Elytra rápida: 30 blocos por segundo ainda é movimento.
		boolean salto = j.leitura.amostrar(j.x, j.z + 7.5, 0, false, false, j.tick);

		assertFalse(salto);
	}
}
