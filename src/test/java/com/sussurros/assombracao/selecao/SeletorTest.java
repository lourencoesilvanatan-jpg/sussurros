package com.sussurros.assombracao.selecao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sussurros.assombracao.Evento;

class SeletorTest {

	@Test
	void tetoFuncionaQuandoEViiavel() {
		List<Evento> candidatos = List.of(Evento.PASSOS, Evento.SUSSURRO, Evento.ANIMAIS);
		List<Double> pesos = new ArrayList<>(List.of(900.0, 100.0, 100.0));

		Seletor.aplicarTeto(candidatos, pesos, 0.45);

		double total = pesos.stream().mapToDouble(Double::doubleValue).sum();
		assertEquals(0.45, pesos.get(0) / total, 1.0E-9);
	}

	@Test
	void tetoComDuasCategoriasTerminaEm55Por45() {
		// Comportamento herdado: com so duas categorias o teto de 45% e inviavel.
		// A primeira volta corta SOM para 45%; a segunda corta MENTE, e SOM termina com 55%.
		List<Evento> candidatos = List.of(Evento.PASSOS, Evento.ECO, Evento.SUSSURRO);
		List<Double> pesos = new ArrayList<>(List.of(900.0, 900.0, 100.0));

		Seletor.aplicarTeto(candidatos, pesos, 0.45);

		double total = pesos.stream().mapToDouble(Double::doubleValue).sum();
		assertEquals(0.55, (pesos.get(0) + pesos.get(1)) / total, 1.0E-9);
	}

	@Test
	void tetoNaoMexeComUmaCategoriaSo() {
		List<Evento> candidatos = List.of(Evento.PASSOS, Evento.ECO);
		List<Double> pesos = new ArrayList<>(List.of(900.0, 100.0));

		Seletor.aplicarTeto(candidatos, pesos, 0.45);

		assertEquals(List.of(900.0, 100.0), pesos);
	}

	@Test
	void pesoIntensidadeTemPisoDeQuinzePorCento() {
		assertEquals(0.15, Seletor.pesoIntensidade(45, 10), 1.0E-12);
	}

	@Test
	void antiRepeticaoPenalizaEventoMaisRecente() {
		assertEquals(0.15, Seletor.antiRepeticao(
				List.of(Evento.PASSOS, Evento.ECO, Evento.PASSAGEM),
				Evento.PASSOS), 1.0E-12);
	}

	@Test
	void aversaoSequenciaTemPisoDepoisDeDuasRepeticoes() {
		assertEquals(0.25, Seletor.aversaoSequencia(
				Evento.PASSOS,
				List.of("SOM>SOM", "SOM>SOM"),
				Evento.ECO), 1.0E-12);
	}

	@Test
	void sorteioPonderadoSelecionaSegundoIndice() {
		assertEquals(1, Seletor.sortearIndice(List.of(1.0, 1.0), 1.5));
	}

	@Test
	void portaoMantemOsLimitesAtuais() {
		assertEquals(20, Seletor.portao(10));
		assertEquals(40, Seletor.portao(20));
		assertEquals(55, Seletor.portao(30));
		assertEquals(70, Seletor.portao(45));
	}

	@Test
	void exploracaoRespeitaPisoQuandoAsUltimasConfiancasSaoBaixas() {
		assertEquals(0.25, Seletor.taxaExploracao(
				0.10, List.of(0.10, 0.10, 0.10), false), 1.0E-12);
	}

	@Test
	void exploracaoCaiQuandoEscalandoEConfiancaEstaAlta() {
		assertEquals(0.05, Seletor.taxaExploracao(
				0.30, List.of(0.50, 0.50, 0.50), true), 1.0E-12);
	}

	@Test
	void exploracaoMantemBaseQuandoNaoEstaEscalando() {
		assertEquals(0.30, Seletor.taxaExploracao(
				0.30, List.of(0.50, 0.50, 0.50), false), 1.0E-12);
	}
}
