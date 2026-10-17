package com.sussurros.assombracao;

/**
 * Quando ele usa a voz na caçada, e qual (0.9.0-alpha27). Contas puras: a Cacada decide com o sorteio dela, e os
 * testes de JUnit usam direto.
 *
 * O dono pediu "música e gritos" na perseguição e, em 16/10/2026, aceitou a recomendação que colou: sim a um grito
 * ou rugido, em volume normal, sem pico, raro e ligado ao momento, variado, e mudando com a distância. Daí:
 *
 *  - três momentos só: quando a perseguição começa (às vezes), quando o jogador o fere pela primeira vez (sempre,
 *    é a resposta ao golpe), e quando ele reencontra o jogador depois de tê-lo perdido (às vezes);
 *  - no máximo duas vozes por caçada, e quinze segundos entre uma e outra (o golpe não espera: é a resposta a ele);
 *  - nunca em cima de outro sinal da caçada (o pano do atalho, o aviso na boca do vão, a maçaneta, o aviso no bloco):
 *    esses dizem ao jogador o que fazer, e a voz não pode cobri-los. Dois segundos de distância;
 *  - de longe, a voz abafada; de perto, a seca e curta. Os arquivos têm o mesmo nível (ver gerar_sons.py): quem
 *    deixa a de longe mais baixa é a distância. A altura (pitch) varia um pouco a cada vez.
 *
 * Os números são ponto de partida, cada um com linha no log (CACA ... voz=).
 */
public final class VozDaCacada {
	public enum Momento {
		INICIO,     // a perseguição começa (fim do aviso)
		FERIDO,     // o primeiro golpe do jogador nesta caçada
		REENCONTRO  // ele tinha perdido o jogador e voltou a vê-lo
	}

	/** Quantas vozes cabem numa caçada. */
	public static final int MAXIMO_POR_CACADA = 2;
	/** Ticks entre uma voz e a seguinte. */
	public static final int INTERVALO = 20 * 15;
	/** Ticks de distância de qualquer outro sinal da caçada. */
	public static final int LONGE_DOS_SINAIS = 40;
	/** Ticks sem saber do jogador para que vê-lo de novo conte como reencontro. */
	public static final int PERDIDO_HA = 20 * 8;
	/** A partir daqui (blocos), a voz é a de longe. */
	public static final double DE_LONGE = 14.0;
	/** Chance de cada momento (o golpe é sempre). */
	public static final double CHANCE_INICIO = 0.6;
	public static final double CHANCE_REENCONTRO = 0.5;

	private VozDaCacada() {
	}

	/**
	 * Pode usar a voz agora? sorteio é um número de 0 a 1 do gerador da própria caçada. Os tempos são em ticks,
	 * e um valor negativo quer dizer "nunca aconteceu".
	 */
	public static boolean pode(Momento momento, int vozes, int desdeAUltimaVoz, int desdeOUltimoSinal, double sorteio) {
		if (vozes >= MAXIMO_POR_CACADA) {
			return false;
		}
		// O golpe não espera o intervalo: é a resposta ao que o jogador acabou de fazer, e tem de vir na hora.
		if (momento != Momento.FERIDO && desdeAUltimaVoz >= 0 && desdeAUltimaVoz < INTERVALO) {
			return false;
		}
		if (desdeOUltimoSinal >= 0 && desdeOUltimoSinal < LONGE_DOS_SINAIS) {
			return false;
		}
		return switch (momento) {
			case INICIO -> sorteio < CHANCE_INICIO;
			case FERIDO -> true;
			case REENCONTRO -> sorteio < CHANCE_REENCONTRO;
		};
	}

	/** A de longe? */
	public static boolean deLonge(double distancia) {
		return distancia >= DE_LONGE;
	}

	/**
	 * O volume com que o som sai: 1 de perto; de longe, o bastante para alcançar o jogador com um pouco de folga (o
	 * alcance de um som do jogo é dezesseis blocos vezes o volume, e a força cai em linha reta até lá). Não deixa o
	 * som mais forte: acima de 1 o jogo só aumenta o alcance.
	 */
	public static float volume(double distancia) {
		return deLonge(distancia) ? (float) Math.max(1.0, distancia / 10.0) : 1.0F;
	}

	/** A altura de cada vez, de 0,93 a 1,05: a mesma gravação não soa igual duas vezes. */
	public static float altura(double sorteio) {
		return (float) (0.93 + 0.12 * Math.max(0.0, Math.min(1.0, sorteio)));
	}
}
