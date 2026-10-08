package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;

/**
 * Cena "Na linha das árvores" (0.5-alpha): em área aberta e longe de casa, duas aparições sem anúncio
 * garantido, a segunda do lado oposto e mais perto. Cobertura e penumbra pesam na escolha do lugar.
 *
 * O Diretor chama verificar/conduzir a cada segundo. O estado da cena fica em {@link EstadoJogador};
 * manifestação e posicionamento ainda vêm do Diretor e da {@link Aparicao}.
 */
final class CenaLinhaDasArvores {
	private CenaLinhaDasArvores() {
	}

	/**
	 * Em area aberta, depois de algum tempo longe da casa, ele pode ser visto duas vezes em pontos
	 * diferentes. A segunda aparicao cruza o lado da primeira e chega mais perto: nao e uma "caca",
	 * e uma historia espacial curta para vender a sensacao de acompanhamento.
	 */
	static void verificarCenaCampo(ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean calma, long seg, RandomSource rnd) {
		if (e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return;
		}
		if (fase < 3 || calma || e.contexto != ContextoMundo.Tipo.ABERTO
				|| e.estado == EstadoDiretor.AMEACANDO || e.estado == EstadoDiretor.RECUANDO) {
			return;
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return;
		}
		if (e.abertoDesde < 0 || seg - e.abertoDesde < 90 || seg < e.cenaCampoLiberadaEm
				|| e.tentouCenaCampo || e.rastro.tamanho() < 18) {
			return;
		}
		e.tentouCenaCampo = true;
		double chance = e.cenasCampoFeitas == 0 ? 0.62 : 0.24;
		if (rnd.nextDouble() >= chance) {
			Depuracao.log(p, seg, "campo: havia condicoes, mas desta vez nao");
			return;
		}
		iniciarCenaCampo(p, e, seg, rnd, false);
	}

	static void iniciarCenaCampo(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean teste) {
		e.cenaCampo = EstadoJogador.CenaCampo.ESPERA;
		e.cenaCampoDesde = seg;
		e.cenaCampoAte = seg + 8 + rnd.nextInt(11);
		e.cenaCampoTeste = teste;
		e.cenaCampoLado = rnd.nextBoolean() ? 1 : -1;
		e.cenaCampoId = Diretor.novoIdCena();
		if (!teste) {
			e.cenasCampoFeitas++;
			e.cenaCampoLiberadaEm = seg + 1500; // raro: no maximo uma natural a cada ~25 min
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s tipo=LINHA_DAS_ARVORES INICIO teste=%s ladoInicial=%s espera=%ds",
				e.cenaCampoId, teste ? "sim" : "nao", e.cenaCampoLado > 0 ? "direita" : "esquerda",
				e.cenaCampoAte - seg));
	}

	static void conduzirCenaCampo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			long seg, long tick, RandomSource rnd) {
		HospedeEntity atual = e.criatura;
		boolean criaturaPresente = atual != null && !atual.isRemoved();
		boolean teste = e.cenaCampoTeste;

		switch (e.cenaCampo) {
			case NENHUMA -> {
			}
			case ESPERA -> {
				if (seg >= e.cenaCampoAte) {
					e.cenaCampo = EstadoJogador.CenaCampo.PRIMEIRA;
					e.cenaCampoDesde = seg;
				}
			}
			case PRIMEIRA -> {
				if (criaturaPresente || Diretor.bloqueado(p, e, tick)) {
					return;
				}
				if (invocarCenaCampo(level, p, e, seg, tick, teste, false)) {
					e.cenaCampo = EstadoJogador.CenaCampo.OBSERVANDO_1;
					e.cenaCampoDesde = seg;
				} else if (seg - e.cenaCampoDesde > 35) {
					Depuracao.log(p, seg, "CENA id=" + e.cenaCampoId + " etapa=PRIMEIRA SEM_LUGAR");
					silencioCenaCampo(p, e, seg, rnd);
				}
			}
			case OBSERVANDO_1 -> {
				if (!criaturaPresente) {
					e.cenaCampo = EstadoJogador.CenaCampo.PAUSA;
					e.cenaCampoDesde = seg;
					e.cenaCampoAte = seg + 14 + rnd.nextInt(18);
					Depuracao.log(p, seg, "CENA id=" + e.cenaCampoId + " etapa=PAUSA duracao=" + (e.cenaCampoAte - seg) + "s");
				} else if (seg - e.cenaCampoDesde > 45) {
					atual.sumir(level, false, "LIMITE_CENA_CAMPO_1");
				}
			}
			case PAUSA -> {
				if (seg >= e.cenaCampoAte) {
					e.cenaCampo = EstadoJogador.CenaCampo.SEGUNDA;
					e.cenaCampoDesde = seg;
				}
			}
			case SEGUNDA -> {
				if (criaturaPresente || Diretor.bloqueado(p, e, tick)) {
					return;
				}
				if (invocarCenaCampo(level, p, e, seg, tick, teste, true)) {
					e.cenaCampo = EstadoJogador.CenaCampo.OBSERVANDO_2;
					e.cenaCampoDesde = seg;
				} else if (seg - e.cenaCampoDesde > 35) {
					Depuracao.log(p, seg, "CENA id=" + e.cenaCampoId + " etapa=SEGUNDA SEM_LUGAR");
					silencioCenaCampo(p, e, seg, rnd);
				}
			}
			case OBSERVANDO_2 -> {
				if (!criaturaPresente) {
					silencioCenaCampo(p, e, seg, rnd);
				} else if (seg - e.cenaCampoDesde > 75) {
					atual.sumir(level, false, "LIMITE_CENA_CAMPO_2");
				}
			}
			case SILENCIO -> {
				if (seg >= e.cenaCampoAte) {
					e.cenaCampo = EstadoJogador.CenaCampo.NENHUMA;
					e.ultimoEventoSeg = seg;
					e.proximoEvento = Math.max(e.proximoEvento, seg + 35);
					Depuracao.log(p, seg, "SILENCIO fim motivo=FIM_CENA_CAMPO cena=" + e.cenaCampoId);
					Depuracao.log(p, seg, "CENA id=" + e.cenaCampoId + " FIM");
				}
			}
		}
	}

	/** Procura uma posicao fora da tela, visivel se o jogador virar e preferencialmente junto de cobertura. */
	private static boolean invocarCenaCampo(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick,
			boolean teste, boolean segunda) {
		int lado = segunda ? -e.cenaCampoLado : e.cenaCampoLado;
		double aMin = segunda ? 48 : 58;
		double aMax = segunda ? 88 : 105;
		double dMin = segunda ? 20 : 30;
		double dMax = segunda ? 34 : 46;
		Aparicao.Config cfg = new Aparicao.Config(
				aMin, aMax, dMin, dMax, 28,
				true, false, true, true, false, lado,
				3.0, 0.9, 1.4, (dMin + dMax) / 2.0);
		Aparicao.Candidato candidato = Aparicao.buscarAoRedor(level, p, e, cfg);
		if (candidato == null) {
			return false;
		}
		PedidoManifestacao pedido = teste ? PedidoManifestacao.deComando(Evento.PRESENCA) : PedidoManifestacao.doDiretor(Evento.PRESENCA);
		pedido = pedido.comNota("APARICAO2_CAMPO " + candidato.resumo());
		
			HospedeEntity.Modo modo = segunda ? HospedeEntity.Modo.ESPREITAR : HospedeEntity.Modo.OBSERVAR;
			Diretor.criar(level, p, e, candidato.chao(), modo, 20 * (segunda ? 70 : 35), 1.0, segunda ? 7.0 : 10.0, pedido);
			HospedeEntity h = e.criatura;
			if (h != null && segunda) {
				h.definirMaxReposicoes(1);
			}
			if (h == null) {
				return false;
			}
			if (!teste) {
				Aparicao.registrar(e, candidato);
			}
			Depuracao.log(p, seg, String.format(Locale.ROOT,
					"CENA id=%s etapa=%s manifestacao=%s %s anuncio=nenhum",
					e.cenaCampoId, segunda ? "SEGUNDA" : "PRIMEIRA", h.getIdManifestacao(), candidato.resumo()));
			if (!teste) {
				Diretor.posEvento(p, e, Evento.PRESENCA, null, 0, seg, tick);
			}
			return true;
	}

	private static void silencioCenaCampo(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		e.cenaCampo = EstadoJogador.CenaCampo.SILENCIO;
		e.cenaCampoAte = seg + 75 + rnd.nextInt(56);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s etapa=SILENCIO duracao=%ds silencioAte=%ds",
				e.cenaCampoId, e.cenaCampoAte - seg, e.cenaCampoAte));
		Diretor.logSilencioInicio(p, seg, "FIM_CENA_CAMPO", e.cenaCampoId, e.cenaCampoAte - seg);
	}
}
