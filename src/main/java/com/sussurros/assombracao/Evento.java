package com.sussurros.assombracao;

/**
 * Tipos de acontecimento.
 *
 * faseMinima:  a partir de qual fase pode acontecer.
 * categoria:   o "tipo de medo". O Diretor aprende por evento E por categoria.
 * intensidade: quanto aumenta a pressão (tensão momentânea). Pressão alta demais = trégua.
 * noSorteio:   se false, só acontece como continuação de outro evento (cadeia) ou por comando.
 */
public enum Evento {
	PASSOS(1, Categoria.SOM, 8, true),
	PASSO_UNICO(1, Categoria.SOM, 4, true),
	ECO(1, Categoria.SOM, 10, true),
	PASSAGEM(1, Categoria.SOM, 11, true),
	ANIMAIS(1, Categoria.AMBIENTE, 12, true),
	VESTIGIO(1, Categoria.AMBIENTE, 12, true),
	LUZ_ERRADA(1, Categoria.AMBIENTE, 13, true),
	SINAL_DISTANTE(1, Categoria.MENTE, 13, true),
	RUIDO_RETORNO(1, Categoria.SOM, 14, true),
	OBJETO_FORA_LUGAR(1, Categoria.MENTE, 14, true),
	TRILHA_INTERROMPIDA(1, Categoria.AMBIENTE, 15, true),
	SUSSURRO(2, Categoria.MENTE, 6, true),
	SINAL(2, Categoria.MENTE, 8, true), // falso positivo contextual: algo acontece, mas nem sempre existe criatura
	SEGUIDOR(2, Categoria.SOM, 16, true), // passos que percorrem pontos reais do Rastro sem garantir criatura
	PEGADAS(2, Categoria.AMBIENTE, 15, true), // vestígios visuais curtos no caminho antigo, sem garantir criatura
	VULTO(2, Categoria.VISAO, 9, true), // silhueta parada a dezenas de blocos, de dia; some um segundo depois de mirada
	PORTA(2, Categoria.AMBIENTE, 14, true),
	TOCHA(2, Categoria.AMBIENTE, 12, true),
	BATIDA(2, Categoria.AMBIENTE, 16, true),
	ECO_CHAT(3, Categoria.MENTE, 18, true),
	PRESENCA(3, Categoria.VISAO, 22, true),
	ATRAS(3, Categoria.VISAO, 30, true),
	TUMULO(3, Categoria.VISAO, 28, true),
	ESPREITA(3, Categoria.VISAO, 26, false), // v0.4.2: só na sequência de ameaça (ou por comando)
	VISTO(3, Categoria.MENTE, 10, false),
	CACA(4, Categoria.AMEACA, 45, true),
	ESPERA(4, Categoria.AMEACA, 30, false);

	public enum Categoria {
		SOM, AMBIENTE, MENTE, VISAO, AMEACA;

		public String chavePeso() {
			return Memoria.PESO + "cat_" + this.name().toLowerCase();
		}
	}

	public final int faseMinima;
	public final Categoria categoria;
	public final int intensidade;
	public final boolean noSorteio;

	Evento(int faseMinima, Categoria categoria, int intensidade, boolean noSorteio) {
		this.faseMinima = faseMinima;
		this.categoria = categoria;
		this.intensidade = intensidade;
		this.noSorteio = noSorteio;
	}

	public String chavePeso() {
		return Memoria.PESO + this.name().toLowerCase();
	}
}
