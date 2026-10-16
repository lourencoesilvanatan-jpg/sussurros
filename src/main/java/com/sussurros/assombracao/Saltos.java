package com.sussurros.assombracao;

/**
 * As contas dos saltos da caçada (0.9.0-alpha26), sem mundo: os testes de JUnit usam direto.
 *
 * "Salto" é toda vez que ele muda de lugar sem andar perto do jogador: o atalho, o atravessar e o recuo depois de
 * ser ferido. Na sessão de 12/10/2026 foram três saltos em duas caçadas, e as duas acabaram em captura de 14 e 22
 * segundos depois do aviso: depois de cada salto ele voltava a andar no tick seguinte, às vezes nas costas do
 * jogador. Daí as regras, tiradas da IA de Alien: Isolation (pesquisa/2026-10-15-analise-das-ideias-de-fora.md):
 *
 *  - um salto só destranca pelo truque que o jogador repete, nunca por captura: o atalho de longe, depois de duas
 *    caçadas em que ele fugiu para longe; atravessar (e o atalho de quem fica num anel sem caminho), depois de duas
 *    em que ele ficou onde não havia caminho. Muitas caçadas destrancam os dois de qualquer jeito (o gatilho de
 *    segurança do Alien: dá a incerteza sem parecer arbitrário);
 *  - nenhum salto nos primeiros quinze segundos depois do aviso;
 *  - quando salta, nunca cai nas costas nem a menos de doze blocos, e fica parado um tempo que é maior quanto mais
 *    perto ele caiu, para a janela até o primeiro toque possível ter o mesmo tamanho.
 *
 * Os números são ponto de partida, cada um com a sua linha no log (TELEMETRIA.md). As garantias de forma (nunca nas
 * costas, a espera proporcional à distância) não são número a ajustar.
 */
public final class Saltos {
	/** Quantas caçadas um truque funciona antes de ele aprender a resposta. */
	public static final int REPETICOES = 2;
	/** Depois de tantas caçadas, os saltos valem de qualquer jeito. */
	public static final int CACADAS_PARA_TUDO = 6;
	/** Nenhum salto nestes ticks depois do aviso: é quando o jogador decide entre correr, se esconder ou encarar. */
	public static final int SEM_SALTO_DEPOIS_DO_AVISO = 20 * 15;
	/** Nunca mais perto do jogador que isto, num salto de longe (blocos). */
	public static final double DISTANCIA_MINIMA = 12.0;
	/** "Nas costas": a menos disto (graus) do ponto bem atrás de para onde ele está virado. */
	public static final double COSTAS_GRAUS = 60.0;
	/** A janela que se quer entre um salto e o primeiro toque possível (ticks). */
	public static final int JANELA = 90;
	/** Os limites da espera parada depois de um salto (ticks): de dois a três segundos. */
	public static final int ESPERA_MINIMA = 40;
	public static final int ESPERA_MAXIMA = 60;
	/** Fuga: a esta distância dele, ou mais, e tanto mais longe do que já esteve (blocos). */
	public static final double FUGA_DISTANCIA = 24.0;
	public static final double FUGA_AFASTOU = 12.0;

	private Saltos() {
	}

	/** O que está destrancado: o atalho de quem está longe, e as respostas a quem fica onde não há caminho. */
	public record Liberacao(boolean deLonge, boolean semCaminho) {
		public String paraLog() {
			return "longe:" + (this.deLonge ? "sim" : "nao") + ",semCaminho:" + (this.semCaminho ? "sim" : "nao");
		}
	}

	/**
	 * Pelo que a memória do jogador conta. As travessias de antes desta versão contam como truques (cada uma foi um
	 * jogador sem caminho até ele); depois de destrancado, cada travessia nova soma ali e o truque não soma mais.
	 */
	public static Liberacao liberacao(int cacadas, int fugas, int truques, int travessias) {
		boolean tempo = cacadas >= CACADAS_PARA_TUDO;
		return new Liberacao(tempo || fugas >= REPETICOES, tempo || truques + travessias >= REPETICOES);
	}

	/**
	 * Quantos ticks ele fica parado depois de um salto que o deixou a "distancia" blocos do jogador, sabendo que
	 * ele anda a "blocosPorSegundo" e toca a "alcance". Quanto mais perto ele caiu, mais espera: a janela até o
	 * primeiro toque possível é a mesma, dentro dos limites de dois a três segundos parado.
	 */
	public static int esperaDepois(double distancia, double blocosPorSegundo, double alcance) {
		long espera = Math.round(JANELA - caminhada(distancia, blocosPorSegundo, alcance));
		return (int) Math.max(ESPERA_MINIMA, Math.min(ESPERA_MAXIMA, espera));
	}

	/** A janela que essa espera dá: parado, e depois andando até tocar (ticks). */
	public static int janela(int espera, double distancia, double blocosPorSegundo, double alcance) {
		return espera + (int) Math.round(caminhada(distancia, blocosPorSegundo, alcance));
	}

	/** Ticks andando até chegar ao alcance do toque. */
	private static double caminhada(double distancia, double blocosPorSegundo, double alcance) {
		return Math.max(0.0, distancia - alcance) / Math.max(0.1, blocosPorSegundo) * 20.0;
	}

	/**
	 * O ponto (na direção para, a partir do jogador) está nas costas de quem está virado na direção olhar? Só o
	 * plano do chão conta: quem olha para o céu continua de costas para o que está atrás dele.
	 */
	public static boolean nasCostas(double olharX, double olharZ, double paraX, double paraZ) {
		double no = Math.sqrt(olharX * olharX + olharZ * olharZ);
		double np = Math.sqrt(paraX * paraX + paraZ * paraZ);
		if (no < 1.0E-6 || np < 1.0E-6) {
			return false;
		}
		double cos = (olharX * paraX + olharZ * paraZ) / (no * np);
		return cos < -Math.cos(Math.toRadians(COSTAS_GRAUS));
	}

	/** Ele fugiu para longe: está a pelo menos FUGA_DISTANCIA blocos, e FUGA_AFASTOU mais longe que o mais perto em que esteve. */
	public static boolean fugiu(double distancia, double menorDistancia) {
		return distancia >= FUGA_DISTANCIA && distancia >= menorDistancia + FUGA_AFASTOU;
	}

	/**
	 * A vida depois de uma captura (ver Captura): oito pontos a menos, nunca abaixo da metade do máximo que ele pode
	 * ter; quem já estava abaixo disso não piora. Até a alpha25 o piso era de dois corações.
	 */
	public static float vidaDepoisDaCaptura(float vida, float maxima) {
		float metade = maxima / 2.0F;
		return vida > metade ? Math.max(metade, vida - 8.0F) : vida;
	}
}
