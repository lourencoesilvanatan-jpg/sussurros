package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.registro.ModSons;

/**
 * Cena "Foi aqui" (0.5-alpha2): ao voltar a um marco persistente (um lugar onde uma reação forte
 * aconteceu), o lugar antigo é reutilizado em som, manifestação, espreita e silêncio.
 *
 * O Diretor chama verificar/conduzir a cada segundo. O estado da cena fica em {@link EstadoJogador};
 * manifestação, rastro e sons ainda vêm dos helpers do Diretor.
 */
final class CenaFoiAqui {
	private CenaFoiAqui() {
	}

	/**
	 * Um chunk onde um evento forte já funcionou é um marco persistente. Ao voltar ali em outra
	 * situação, o Diretor pode reutilizar o próprio lugar em vez de inventar uma coordenada nova.
	 */
	static void verificarCenaMarco(ServerPlayer p, EstadoJogador e, int fase, boolean calma,
			long seg, RandomSource rnd) {
		if (e.marcoPendente == Long.MIN_VALUE || e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA) {
			return;
		}
		if (e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA) {
			return;
		}
		int cx = p.getBlockX() >> 4;
		int cz = p.getBlockZ() >> 4;
		long atual = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		if (atual != e.marcoPendente) {
			e.marcoPendente = Long.MIN_VALUE;
			return;
		}
		if (fase < 3 || calma || seg < e.cenaMarcoLiberadaEm
				|| e.estado == EstadoDiretor.AMEACANDO || e.estado == EstadoDiretor.RECUANDO
				|| (e.criatura != null && !e.criatura.isRemoved())) {
			return;
		}

		long marco = e.marcoPendente;
		e.marcoPendente = Long.MIN_VALUE;
		e.marcosUsadosSessao.add(marco); // cruzar a borda do chunk não permite rerrolar a cena
		if (rnd.nextDouble() >= 0.62) {
			Depuracao.log(p, seg, "MARCO revisitado: o lugar foi reconhecido, mas ficou quieto desta vez");
			return;
		}
		BlockPos memoriaLugar = Lugares.de(p).posMarco(cx, cz);
		iniciarCenaMarco(p, e, seg, rnd, false, memoriaLugar);
	}

	static void iniciarCenaMarco(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean teste,
			BlockPos memoriaLugar) {
		e.cenaMarco = EstadoJogador.CenaMarco.ESPERA;
		e.cenaMarcoDesde = seg;
		e.cenaMarcoAte = seg + 6 + rnd.nextInt(8);
		e.cenaMarcoTeste = teste;
		e.cenaMarcoPos = memoriaLugar;
		e.cenaMarcoId = Diretor.novoIdCena();
		if (!teste) {
			e.cenaMarcoLiberadaEm = seg + 900; // no máximo uma natural a cada ~15 min
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s tipo=FOI_AQUI INICIO teste=%s espera=%ds contexto=%s marcoExato=%s",
				e.cenaMarcoId, teste ? "sim" : "nao", e.cenaMarcoAte - seg, e.contexto,
				memoriaLugar == null ? "nao" : Diretor.pos(memoriaLugar.getX(), memoriaLugar.getY(), memoriaLugar.getZ())));
	}

	static void conduzirCenaMarco(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			long seg, long tick, RandomSource rnd) {
		HospedeEntity atual = e.criatura;
		boolean criaturaPresente = atual != null && !atual.isRemoved();
		boolean teste = e.cenaMarcoTeste;

		switch (e.cenaMarco) {
			case NENHUMA -> {
			}
			case ESPERA -> {
				if (seg >= e.cenaMarcoAte) {
					e.cenaMarco = EstadoJogador.CenaMarco.ECO;
				}
			}
			case ECO -> {
				Rastro.Ponto pt = Diretor.pontoDoRastro(p, e, seg, 8, 100, 7, 24, false);
				Vec3 lugar;
				String motivoLugar;
				if (e.cenaMarcoPos != null) {
					Vec3 antigo = Vec3.atCenterOf(e.cenaMarcoPos);
					double dAntigo = Diretor.distancia(p, antigo);
					if (dAntigo >= 6 && dAntigo <= 28 && !Diretor.pontoNaFrente(p, antigo, HospedeEntity.CONE_TELA_SEGURA)) {
						lugar = antigo;
						motivoLugar = "MARCO_EXATO";
					} else if (pt != null) {
						lugar = new Vec3(pt.x(), pt.y() + 0.8, pt.z());
						motivoLugar = "RASTRO";
					} else {
						lugar = Diretor.pontoRelativo(p, 135 + rnd.nextDouble() * 90, 10 + rnd.nextInt(7));
						motivoLugar = "MARCO";
					}
				} else if (pt != null) {
					lugar = new Vec3(pt.x(), pt.y() + 0.8, pt.z());
					motivoLugar = "RASTRO";
				} else {
					lugar = Diretor.pontoRelativo(p, 135 + rnd.nextDouble() * 90, 10 + rnd.nextInt(7));
					motivoLugar = "MARCO";
				}
				ModSons.Som som = switch (rnd.nextInt(3)) {
					case 0 -> ModSons.Som.ESTALO;
					case 1 -> ModSons.Som.PANO;
					default -> ModSons.Som.RESPIRACAO;
				};
				ModSons.tocar(level, lugar.x, lugar.y, lugar.z, som,
						Diretor.volumePara(p, lugar.x, lugar.z, 0.48F), 0.80F + rnd.nextFloat() * 0.12F);
				Depuracao.log(p, seg, String.format(Locale.ROOT,
						"CENA id=%s etapa=ECO memoriaDoLugar=sim som=%s motivoPosicao=%s pos=%s dist=%.1f",
						e.cenaMarcoId, som, motivoLugar, Diretor.pos(lugar.x, lugar.y, lugar.z), Diretor.distancia(p, lugar)));
				e.cenaMarco = EstadoJogador.CenaMarco.PRESENCA;
				e.cenaMarcoDesde = seg;
				e.cenaMarcoAte = seg + 5 + rnd.nextInt(7);
			}
			case PRESENCA -> {
				if (seg < e.cenaMarcoAte || criaturaPresente || Diretor.bloqueado(p, e, tick)) {
					return;
				}
				if (presencaNoMarco(level, p, m, e, seg, tick, teste)) {
					e.cenaMarco = EstadoJogador.CenaMarco.OBSERVANDO;
					e.cenaMarcoDesde = seg;
				} else if (seg - e.cenaMarcoDesde > 30) {
					Depuracao.log(p, seg, "CENA id=" + e.cenaMarcoId + " etapa=PRESENCA SEM_LUGAR");
					silencioCenaMarco(p, e, seg, rnd);
				}
			}
			case OBSERVANDO -> {
				if (!criaturaPresente) {
					silencioCenaMarco(p, e, seg, rnd);
				} else if (seg - e.cenaMarcoDesde > 70) {
					atual.sumir(level, false, "LIMITE_CENA_MARCO");
				}
			}
			case SILENCIO -> {
				if (seg >= e.cenaMarcoAte) {
					e.cenaMarco = EstadoJogador.CenaMarco.NENHUMA;
					e.ultimoEventoSeg = seg;
					e.proximoEvento = Math.max(e.proximoEvento, seg + 30);
					Depuracao.log(p, seg, "SILENCIO fim motivo=FIM_CENA_MARCO cena=" + e.cenaMarcoId);
					Depuracao.log(p, seg, "CENA id=" + e.cenaMarcoId + " FIM");
				}
			}
		}
	}

	private static boolean presencaNoMarco(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e,
			long seg, long tick, boolean teste) {
		PedidoManifestacao pedido = teste ? PedidoManifestacao.deComando(Evento.PRESENCA) : PedidoManifestacao.doDiretor(Evento.PRESENCA);
		boolean ok = false;
		
			double[] ang = Diretor.angulosPresencaAdaptativa(m, e, pedido);
			if (e.cenaMarcoPos != null) {
				ok = Diretor.invocarPertoDoMarco(level, p, e, e.cenaMarcoPos, HospedeEntity.Modo.ESPREITAR, 20 * 70, 7.0, pedido);
			}
			if (!ok) {
				ok = Diretor.invocarComCobertura(level, p, e, HospedeEntity.Modo.ESPREITAR,
					Math.max(60, ang[0]), Math.max(100, ang[1]), 17, 30, 20 * 70, 7.0, pedido);
			}
			if (!ok) {
				ok = Diretor.invocarNoRastro(level, p, e, HospedeEntity.Modo.ESPREITAR, seg, 12, 150, 12, 32, 20 * 70, 7.0, pedido);
			}
			if (!ok) {
				ok = Diretor.invocar(level, p, e, HospedeEntity.Modo.ESPREITAR,
						Math.max(60, ang[0]), Math.max(105, ang[1]), 17, 29, 20 * 70, 1.0, true, 7.0, true, pedido);
			}

		HospedeEntity h = e.criatura;
		if (ok && h != null) {
			h.definirMaxReposicoes(1);
			Depuracao.log(p, seg, "CENA id=" + e.cenaMarcoId + " etapa=PRESENCA manifestacao="
					+ h.getIdManifestacao() + " memoriaDoLugar=sim anuncio=nenhum");
			if (!teste) {
				Diretor.posEvento(p, e, Evento.PRESENCA, null, 0, seg, tick);
			}
		}
		return ok;
	}

	private static void silencioCenaMarco(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		e.cenaMarco = EstadoJogador.CenaMarco.SILENCIO;
		e.cenaMarcoAte = seg + 70 + rnd.nextInt(41);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s etapa=SILENCIO duracao=%ds silencioAte=%ds",
				e.cenaMarcoId, e.cenaMarcoAte - seg, e.cenaMarcoAte));
		Diretor.logSilencioInicio(p, seg, "FIM_CENA_MARCO", e.cenaMarcoId, e.cenaMarcoAte - seg);
	}
}
