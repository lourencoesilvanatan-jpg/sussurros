package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.registro.ModSons;

/**
 * Cena "Algo no túnel" (0.4.3-exp1): numa descida longa ao subsolo, o som de uma quebra real do jogador
 * volta de um ponto antigo, um ruído vem de outro ponto do túnel e, na fase 3+, o Hóspede aparece em
 * silêncio no Rastro.
 *
 * O Diretor chama verificar/conduzir a cada segundo. O estado da cena fica em {@link EstadoJogador};
 * manifestação, rastro e sons ainda vêm dos helpers do Diretor.
 */
final class CenaAlgoNoTunel {
	private CenaAlgoNoTunel() {
	}

	/** Uma descida longa ao subsolo pode virar uma pequena historia: sua acao volta, algo se move e ele aparece no rastro. */
	static void verificarCenaTunel(ServerPlayer p, EstadoJogador e, int fase, boolean subterraneo,
			boolean calma, long seg, RandomSource rnd) {
		if (e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA || e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA || e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return;
		}
		if (!subterraneo || fase < 2 || calma || e.estado == EstadoDiretor.AMEACANDO || e.estado == EstadoDiretor.RECUANDO) {
			return;
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return;
		}
		if (e.subsoloDesde < 0 || seg - e.subsoloDesde < 120 || seg < e.cenaTunelLiberadaEm || e.tentouCenaTunel) {
			return;
		}
		if (!temQuebraRecente(e, seg)) {
			return;
		}

		e.tentouCenaTunel = true; // um sorteio por descida longa
		double chance = e.cenasTunelFeitas == 0 ? 0.55 : 0.20;
		if (rnd.nextDouble() >= chance) {
			Depuracao.log(p, seg, "tunel: havia condicoes, mas desta vez nao");
			return;
		}
		iniciarCenaTunel(p, e, seg, rnd, false);
	}

	private static boolean temQuebraRecente(EstadoJogador e, long seg) {
		for (EstadoJogador.Acao acao : e.acoes) {
			if (acao.tipo() == EstadoJogador.TipoAcao.QUEBRA && seg - acao.seg() <= 300) {
				return true;
			}
		}
		return false;
	}

	static EstadoJogador.@Nullable Acao sortearQuebraRecente(EstadoJogador e, long seg, RandomSource rnd) {
		List<EstadoJogador.Acao> quebras = new ArrayList<>();
		for (EstadoJogador.Acao acao : e.acoes) {
			if (acao.tipo() == EstadoJogador.TipoAcao.QUEBRA && seg - acao.seg() <= 360) {
				quebras.add(acao);
			}
		}
		return quebras.isEmpty() ? null : quebras.get(rnd.nextInt(quebras.size()));
	}

	static void iniciarCenaTunel(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean teste) {
		e.cenaTunel = EstadoJogador.CenaTunel.ESPERA;
		e.cenaTunelDesde = seg;
		e.cenaTunelAte = seg + 5 + rnd.nextInt(6);
		e.cenaTunelTeste = teste;
		e.cenaTunelId = Diretor.novoIdCena();
		if (!teste) {
			e.cenasTunelFeitas++;
			e.cenaTunelLiberadaEm = seg + 1200; // no maximo uma natural a cada ~20 min
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s tipo=ALGO_NO_TUNEL INICIO teste=%s espera=%ds",
				e.cenaTunelId, teste ? "sim" : "nao", e.cenaTunelAte - seg));
	}

	static void conduzirCenaTunel(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			long seg, long tick, RandomSource rnd) {
		HospedeEntity atual = e.criatura;
		boolean criaturaPresente = atual != null && !atual.isRemoved();
		boolean teste = e.cenaTunelTeste;

		switch (e.cenaTunel) {
			case NENHUMA -> {
			}
			case ESPERA -> {
				if (seg >= e.cenaTunelAte) {
					e.cenaTunel = EstadoJogador.CenaTunel.ECO;
					e.cenaTunelDesde = seg;
				}
			}
			case ECO -> {
				EstadoJogador.Acao acao = sortearQuebraRecente(e, seg, rnd);
				if (acao == null) {
					Depuracao.log(p, seg, "CENA id=" + e.cenaTunelId + " etapa=ECO SEM_ACAO");
				} else {
					Vec3 lugar = null;
					String motivo = "FRENTE_DO_TUNEL";
					Rastro.Ponto pt = Diretor.pontoDoRastro(p, e, seg, 25, 240, 10, 28, false);
					if (pt != null) {
						lugar = new Vec3(pt.x(), pt.y(), pt.z());
						motivo = "RASTRO idadeRastro=" + (seg - pt.seg()) + "s";
					} else {
						StringBuilder nota = new StringBuilder();
						lugar = Diretor.lugarParaEco(p, e, acao, seg, true, nota);
						if (lugar != null && !nota.isEmpty()) {
							motivo = nota.toString();
						}
					}
					if (lugar == null) {
						lugar = Diretor.pontoRelativo(p, 145 + rnd.nextDouble() * 70, 14 + rnd.nextInt(8));
					}
					Vec3 som = Diretor.eco(level, p, acao.som(), false, false, null, lugar);
					Diretor.logEco(p, "CENA id=" + e.cenaTunelId + " etapa=ECO", acao, som, motivo, seg);
					if (!teste) {
						Diretor.posEvento(p, e, Evento.ECO, som, Diretor.limitar(1 - Diretor.distancia(p, som) / 30.0, 0, 1), seg, tick);
					}
				}
				e.cenaTunel = EstadoJogador.CenaTunel.RUIDO;
				e.cenaTunelDesde = seg;
				e.cenaTunelAte = seg + 4 + rnd.nextInt(5);
			}
			case RUIDO -> {
				if (seg < e.cenaTunelAte) {
					return;
				}
				Rastro.Ponto pt = Diretor.pontoDoRastro(p, e, seg, 15, 180, 12, 26, true);
				Vec3 lugar = pt != null
						? new Vec3(pt.x(), pt.y() + 0.8, pt.z())
						: Diretor.pontoRelativo(p, 140 + rnd.nextDouble() * 80, 14 + rnd.nextInt(8));
				ModSons.Som som = switch (rnd.nextInt(3)) {
					case 0 -> ModSons.Som.MADEIRA;
					case 1 -> ModSons.Som.ARRASTO;
					default -> ModSons.Som.PANO;
				};
				ModSons.tocar(level, lugar.x, lugar.y, lugar.z, som, Diretor.volumePara(p, lugar.x, lugar.y, lugar.z, 0.7F), 0.82F + rnd.nextFloat() * 0.14F);
				Depuracao.log(p, seg, String.format(Locale.ROOT,
						"CENA id=%s etapa=RUIDO som=%s motivoPosicao=%s pos=%s dist=%.1f",
						e.cenaTunelId, som, pt != null ? "RASTRO" : "TUNEL", Diretor.pos(lugar.x, lugar.y, lugar.z), Diretor.distancia(p, lugar)));
				if (fase >= 3 || teste) {
					e.cenaTunel = EstadoJogador.CenaTunel.PRESENCA;
					e.cenaTunelDesde = seg;
					e.cenaTunelAte = seg + 5 + rnd.nextInt(7);
				} else {
					silencioCenaTunel(p, e, seg, rnd);
				}
			}
			case PRESENCA -> {
				if (seg < e.cenaTunelAte || criaturaPresente || Diretor.bloqueado(p, e, tick)) {
					return;
				}
				if (presencaNoTunel(level, p, e, seg, tick, teste)) {
					e.cenaTunel = EstadoJogador.CenaTunel.ESPREITANDO;
					e.cenaTunelDesde = seg;
				} else if (seg - e.cenaTunelDesde > 30) {
					Depuracao.log(p, seg, "CENA id=" + e.cenaTunelId + " etapa=PRESENCA SEM_LUGAR");
					silencioCenaTunel(p, e, seg, rnd);
				}
			}
			case ESPREITANDO -> {
				if (!criaturaPresente) {
					silencioCenaTunel(p, e, seg, rnd);
				} else if (seg - e.cenaTunelDesde > 90) {
					atual.sumir(level, false, "LIMITE_CENA_TUNEL");
				}
			}
			case SILENCIO -> {
				if (seg >= e.cenaTunelAte) {
					e.cenaTunel = EstadoJogador.CenaTunel.NENHUMA;
					e.ultimoEventoSeg = seg;
					e.proximoEvento = Math.max(e.proximoEvento, seg + 30);
					Depuracao.log(p, seg, "SILENCIO fim motivo=FIM_CENA_TUNEL cena=" + e.cenaTunelId);
					Depuracao.log(p, seg, "CENA id=" + e.cenaTunelId + " FIM");
				}
			}
		}
	}

	private static boolean presencaNoTunel(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick, boolean teste) {
		PedidoManifestacao pedido = teste ? PedidoManifestacao.deComando(Evento.PRESENCA) : PedidoManifestacao.doDiretor(Evento.PRESENCA);
		boolean ok;
		
			ok = Diretor.invocarNoRastro(level, p, e, HospedeEntity.Modo.ESPREITAR, seg, 20, 200, 14, 30, 20 * 75, 7.0, pedido);
			if (!ok) {
				ok = Diretor.invocar(level, p, e, HospedeEntity.Modo.ESPREITAR, 65, 105, 15, 26, 20 * 75, 1.0, true, 7.0, true, pedido);
			}

		HospedeEntity h = e.criatura;
		if (ok && h != null) {
			h.definirMaxReposicoes(1);
			Depuracao.log(p, seg, "CENA id=" + e.cenaTunelId + " etapa=PRESENCA manifestacao=" + h.getIdManifestacao() + " anuncio=nenhum");
			if (!teste) {
				Diretor.posEvento(p, e, Evento.PRESENCA, null, 0, seg, tick);
			}
		}
		return ok;
	}

	private static void silencioCenaTunel(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		e.cenaTunel = EstadoJogador.CenaTunel.SILENCIO;
		e.cenaTunelAte = seg + 60 + rnd.nextInt(41);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s etapa=SILENCIO duracao=%ds silencioAte=%ds",
				e.cenaTunelId, e.cenaTunelAte - seg, e.cenaTunelAte));
		Diretor.logSilencioInicio(p, seg, "FIM_CENA_TUNEL", e.cenaTunelId, e.cenaTunelAte - seg);
	}
}
