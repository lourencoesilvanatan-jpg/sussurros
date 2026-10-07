package com.sussurros.assombracao;

/**
 * O "humor" do Diretor. Cada estado limita a intensidade do que pode acontecer.
 * Durações são sorteadas a cada ciclo para o ritmo não virar um relógio.
 */
public enum EstadoDiretor {
	CALMO(0, 0.0, 60, 180),
	OBSERVANDO(13, 0.10, 90, 240),
	TESTANDO(20, 0.40, 0, 0),
	ESCALANDO(30, 0.10, 0, 0),
	AMEACANDO(100, 0.05, 0, 0),
	RECUANDO(0, 0.0, 150, 300);

	public final int intensidadeMax;
	public final double exploracao;
	public final int duracaoMin;
	public final int duracaoMax;

	EstadoDiretor(int intensidadeMax, double exploracao, int duracaoMin, int duracaoMax) {
		this.intensidadeMax = intensidadeMax;
		this.exploracao = exploracao;
		this.duracaoMin = duracaoMin;
		this.duracaoMax = duracaoMax;
	}

	public boolean permiteCadeias() {
		return this == ESCALANDO || this == AMEACANDO;
	}
}
