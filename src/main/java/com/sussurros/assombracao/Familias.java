package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.Collection;

import net.minecraft.server.level.ServerPlayer;

/**
 * As famílias do que o mod faz (0.9.0-alpha22), e as duas regras que valem debaixo da terra.
 *
 * Em 11/10/2026 o dono saiu de uma caverna dizendo que tudo ali era "mudar a tocha de cor, e sons". O log
 * confirma: das quarenta coisas que saíram com ele no subsolo, vinte e uma foram luz (dezoito delas, chamas
 * pálidas), oito foram som, e de coisa que se toca houve um objeto e duas aparições. A pesquisa de habituação
 * diz o mesmo por outro caminho: repetir a mesma família de estímulo gasta a resposta, e o que a reacende é um
 * estímulo diferente. O problema não era quantidade; era ser sempre a mesma coisa.
 *
 * Tudo o que o mod empurra para o jogador cai numa de seis famílias: as cinco do cardápio (luz, som, marca,
 * objeto, corpo) e mais uma, CABECA, para o que não acontece no mundo (a borda escura, o texto, o Véu).
 * Quem faz a saída chama {@link #registrar}; a linha FAMILIA do log é o que o analisador conta, em qualquer
 * tipo de lugar.
 *
 * As regras valem só com o contexto SUBSOLO. Na superfície quase tudo o que o Diretor sorteia é som, e barrar
 * ali mudaria a mistura inteira do mod; a queixa é da caverna.
 *
 *  1. Nenhuma família passa de duas nas últimas cinco saídas. Isso já dá o "nunca três seguidas" do cardápio, e
 *     dá mais: com a regra das três seguidas uma família ainda podia ser dois terços do que sai, e a prova que
 *     a pesquisa pede é ninguém acima de 40%. Assim o teto é um terço. As duas famílias físicas (objeto e
 *     corpo) nunca são barradas: a regra existe para tirar luz e som de cima, não para segurar o que faltava.
 *     Quem pergunta é quem ia fazer a saída ({@link #barrada}): o sorteio do Diretor (que cede quando só sobra
 *     a família barrada: variedade não pode virar silêncio), os presságios e perturbações da Atmosfera, e as
 *     chamas pálidas (que não cedem: uma tocha a menos mudando de cor é o que se quer).
 *     Não são barrados os elos de cadeia, a sequência de ameaça, as cenas, o primeiro contato, a cota e o
 *     plano B: são composições ou regras, e só registram.
 *  2. A cada quinze minutos de subsolo sem nada de objeto nem de corpo, a próxima coisa é uma delas
 *     ({@link #devendo}; quem paga é a Atmosfera, ver Atmosfera.pagarDivida). É regra, como a cota de corpo:
 *     não espera saldo de atenção, só o respiro.
 *
 * O que é forçado por comando não entra na conta. Nada aqui sorteia.
 */
final class Familias {
	enum Familia {
		LUZ, SOM, MARCA, OBJETO, CORPO, CABECA;

		/** As duas que se podem tocar: nunca são barradas, e são elas que pagam a dívida do subsolo. */
		boolean fisica() {
			return this == OBJETO || this == CORPO;
		}
	}

	/** Quantas saídas a regra olha para trás, e quantas delas podem ser da mesma família. */
	static final int JANELA = 5;
	static final int MAXIMO_NA_JANELA = 2;
	/** Segundos de subsolo sem objeto nem corpo até a dívida abrir. Ponto de partida do cardápio, sem teste. */
	static final int DIVIDA_SEG = 15 * 60;

	static final class Estado {
		/** As últimas saídas com ele no subsolo, da mais antiga para a mais nova. */
		final ArrayDeque<Familia> ultimas = new ArrayDeque<>();
		/** Segundos de subsolo desde a última coisa de objeto ou de corpo. Não zera ao sair da caverna. */
		int subsoloSemFisico;
		/** A dívida falhou (não coube nada): só tenta de novo a partir deste segundo. */
		long dividaProximaTentativa;
	}

	private Familias() {
	}

	// ----- De que família é cada coisa -----

	static Familia de(Evento ev) {
		return switch (ev) {
			case PASSOS, PASSO_UNICO, ECO, PASSAGEM, SINAL_DISTANTE, RUIDO_RETORNO, ECO_PASSOS, SINAL, SEGUIDOR, CANTIGA,
					BATIDA, PRENUNCIO -> Familia.SOM;
			case LUZ_ERRADA, TOCHA -> Familia.LUZ;
			case VESTIGIO, TRILHA_INTERROMPIDA, PEGADAS -> Familia.MARCA;
			case OBJETO_FORA_LUGAR, PORTA -> Familia.OBJETO;
			case ANIMAIS, VULTO, PRESENCA, ATRAS, TUMULO, ESPREITA, CACA, ESPERA -> Familia.CORPO;
			case VIGIA, SUSSURRO, NEBLINA, ECO_CHAT, VEU, VISTO -> Familia.CABECA;
		};
	}

	static Familia de(Pressagio pr) {
		return switch (pr) {
			case PASSO_DISTANTE, ECO_TARDIO, RUIDO_NA_PAREDE -> Familia.SOM;
			case ANIMAIS_VAZIO, ANIMAIS_JOGADOR, ANIMAIS_MOVIMENTO -> Familia.CORPO;
			case CINZA_NO_RASTRO -> Familia.MARCA;
			case LUZ_DISTANTE -> Familia.LUZ;
		};
	}

	static Familia de(Perturbacao pt) {
		return switch (pt) {
			case TODOS_OLHANDO, HA_ALGO_NO_CURRAL, MORCEGOS -> Familia.CORPO;
			case LUZ_NO_FIM, PASSOU_PELA_MINA -> Familia.LUZ;
			case O_CAMINHO_MUDOU, MARCA_IMPOSSIVEL, PEDRINHA, TAMPA, PASSAGEM_FECHADA -> Familia.OBJETO;
			case NAO_FOI_VOCE -> Familia.SOM;
		};
	}

	static Familia de(PlanoB.Tipo tipo) {
		return switch (tipo) {
			case PO -> Familia.OBJETO;
			case TOCHA -> Familia.LUZ;
			case PASSOS -> Familia.SOM;
		};
	}

	// ----- A primeira regra -----

	/**
	 * A regra, sem mundo: esta família já tem o seu máximo nas últimas saídas? Função pura, para o teste.
	 * "ultimas" vem da mais antiga para a mais nova, e pode ter mais do que a janela.
	 */
	static boolean barrada(Collection<Familia> ultimas, Familia familia) {
		if (familia.fisica()) {
			return false;
		}
		int pular = Math.max(0, ultimas.size() - JANELA);
		int iguais = 0;
		for (Familia f : ultimas) {
			if (pular > 0) {
				pular--;
			} else if (f == familia) {
				iguais++;
			}
		}
		return iguais >= MAXIMO_NA_JANELA;
	}

	/** Esta família não pode sair agora, para este jogador? Só debaixo da terra, e nunca o que veio de comando. */
	static boolean barrada(EstadoJogador e, Familia familia) {
		return e.contexto == ContextoMundo.Tipo.SUBSOLO && !e.forcando && barrada(e.familias.ultimas, familia);
	}

	// ----- A segunda regra -----

	/** Uma vez por segundo, no tick do Diretor: o relógio do subsolo sem nada que se toque. */
	static void segundo(EstadoJogador e) {
		if (e.contexto == ContextoMundo.Tipo.SUBSOLO) {
			e.familias.subsoloSemFisico++;
		}
	}

	/** Já faz quinze minutos de subsolo sem objeto nem corpo? */
	static boolean devendo(EstadoJogador e) {
		return e.contexto == ContextoMundo.Tipo.SUBSOLO && e.familias.subsoloSemFisico >= DIVIDA_SEG;
	}

	// ----- O registro -----

	/**
	 * Uma saída aconteceu. "de" é o nome do que saiu, para o log (LUZ_ERRADA, CHAMA_PALIDA, PEDRINHA...).
	 * Fora do subsolo só escreve a linha; dentro, entra na janela e, se for coisa que se toca, zera o relógio.
	 */
	static void registrar(ServerPlayer p, EstadoJogador e, Familia familia, String de, long seg) {
		if (e.forcando) {
			return;
		}
		Estado st = e.familias;
		boolean subsolo = e.contexto == ContextoMundo.Tipo.SUBSOLO;
		int naJanela = 0;
		if (subsolo) {
			st.ultimas.addLast(familia);
			while (st.ultimas.size() > JANELA) {
				st.ultimas.removeFirst();
			}
			for (Familia f : st.ultimas) {
				if (f == familia) {
					naJanela++;
				}
			}
		}
		String semFisico = subsolo ? " naJanela=" + naJanela + "/" + st.ultimas.size() + " semFisico=" + st.subsoloSemFisico + "s" : "";
		if (subsolo && familia.fisica()) {
			st.subsoloSemFisico = 0;
		}
		Depuracao.log(p, seg, "FAMILIA familia=" + familia + " de=" + de + " lugar=" + e.contexto + semFisico);
	}
}
