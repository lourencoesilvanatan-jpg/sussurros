package com.sussurros.assombracao;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.registro.ModSons;

/**
 * Cena "Do outro lado do vidro" (0.5.0-alpha4): à noite, em casa, o Hóspede aparece do lado de fora
 * de uma janela real. Vê-lo o faz sumir; não vê-lo permite um estalo no vidro antes do fim.
 *
 * O Diretor chama verificar/conduzir a cada segundo. O estado da cena fica em {@link EstadoJogador};
 * manifestação e sons ainda vêm dos helpers do Diretor.
 */
final class CenaDoOutroLadoDoVidro {
	private CenaDoOutroLadoDoVidro() {
	}

	private record JanelaAlvo(BlockPos vidro, BlockPos chao) {
	}

	static void verificarCenaJanela(ServerLevel level, ServerPlayer p, EstadoJogador e, int fase,
			boolean noite, boolean calma, long seg, RandomSource rnd) {
		if (e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA
				|| e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA) {
			return;
		}
		if (fase < 3 || !noite || calma || e.contexto != ContextoMundo.Tipo.CASA
				|| e.estado == EstadoDiretor.AMEACANDO || e.estado == EstadoDiretor.RECUANDO
				|| seg < e.cenaJanelaLiberadaEm) {
			return;
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return;
		}
		if (e.contextoDesde < 0 || seg - e.contextoDesde < 45) {
			return;
		}
		Diretor.atualizarCacheAmbiente(level, p, e);
		if (e.janelas.isEmpty()) {
			return;
		}
		double chance = e.cenasJanelaFeitas == 0 ? 0.48 : 0.18;
		// Só uma oportunidade a cada ~2 minutos em casa, mesmo antes do cooldown longo.
		if ((seg - e.contextoDesde) % 120 != 0 || rnd.nextDouble() >= chance) {
			return;
		}
		iniciarCenaJanela(p, e, seg, rnd, false);
	}

	static void iniciarCenaJanela(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean teste) {
		e.cenaJanela = EstadoJogador.CenaJanela.ESPERA;
		e.cenaJanelaDesde = seg;
		e.cenaJanelaAte = seg + 7 + rnd.nextInt(10);
		e.cenaJanelaVistaDesde = -1;
		e.cenaJanelaTeste = teste;
		e.cenaJanelaPos = null;
		e.cenaJanelaId = Diretor.novoIdCena();
		if (!teste) {
			e.cenasJanelaFeitas++;
			e.cenaJanelaLiberadaEm = seg + 1500;
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s tipo=DO_OUTRO_LADO_DO_VIDRO INICIO teste=%s espera=%ds",
				e.cenaJanelaId, teste ? "sim" : "nao", e.cenaJanelaAte - seg));
	}

	static void conduzirCenaJanela(ServerLevel level, ServerPlayer p, EstadoJogador e,
			long seg, long tick, RandomSource rnd) {
		HospedeEntity h = e.criatura;
		boolean presente = h != null && !h.isRemoved();
		switch (e.cenaJanela) {
			case NENHUMA -> {
			}
			case ESPERA -> {
				if (seg >= e.cenaJanelaAte) {
					e.cenaJanela = EstadoJogador.CenaJanela.APARICAO;
					e.cenaJanelaDesde = seg;
				}
			}
			case APARICAO -> {
				if (presente || Diretor.bloqueado(p, e, tick)) {
					return;
				}
				JanelaAlvo alvo = acharJanelaCena(level, p, e);
				if (alvo == null) {
					if (seg - e.cenaJanelaDesde > 25) {
						Depuracao.log(p, seg, "CENA id=" + e.cenaJanelaId + " etapa=APARICAO SEM_JANELA_VALIDA");
						silencioCenaJanela(p, e, seg, rnd);
					}
					return;
				}
				PedidoManifestacao pedido = e.cenaJanelaTeste ? PedidoManifestacao.deComando(Evento.PRESENCA) : PedidoManifestacao.doDiretor(Evento.PRESENCA);
				pedido = pedido.comNota("JANELA vidro=" + alvo.vidro());
				
					Diretor.criar(level, p, e, alvo.chao(), HospedeEntity.Modo.OBSERVAR, 20 * 35, 1.0, 2.6, pedido);

				e.cenaJanelaPos = alvo.vidro();
				e.cenaJanela = EstadoJogador.CenaJanela.OBSERVANDO;
				e.cenaJanelaDesde = seg;
				e.cenaJanelaAte = seg + 6 + rnd.nextInt(5);
				Depuracao.log(p, seg, "CENA id=" + e.cenaJanelaId + " etapa=APARICAO manifestacao="
						+ Diretor.manifestacaoAtiva(e) + " vidro=" + alvo.vidro() + " anuncio=nenhum");
			}
			case OBSERVANDO -> {
				if (!presente) {
					silencioCenaJanela(p, e, seg, rnd);
					return;
				}
				boolean naDirecao = Diretor.pontoNaFrente(p, h.position().add(0, 1.4, 0), Percepcao.conePercebeu(p));
				if (naDirecao) {
					if (e.cenaJanelaVistaDesde < 0) {
						e.cenaJanelaVistaDesde = seg;
						Depuracao.log(p, seg, "CENA id=" + e.cenaJanelaId + " etapa=VIU_ATRAVES_DO_VIDRO manifestacao=" + h.getIdManifestacao());
					}
					if (seg - e.cenaJanelaVistaDesde >= 2) {
						if (!e.cenaJanelaTeste) {
							Vestigios.de(p).registrar(h.blockPosition(), Vestigios.Tipo.DESAPARECIMENTO, seg);
						}
						h.sumir(level, false, "JANELA_VISTA");
						silencioCenaJanela(p, e, seg, rnd);
					}
					return;
				}
				e.cenaJanelaVistaDesde = -1;
				if (seg >= e.cenaJanelaAte) {
					e.cenaJanela = EstadoJogador.CenaJanela.TOQUE;
					e.cenaJanelaAte = seg + 5 + rnd.nextInt(5);
					BlockPos vidro = e.cenaJanelaPos;
					if (vidro != null) {
						ModSons.tocarPara(p, vidro.getX() + 0.5, vidro.getY() + 0.6, vidro.getZ() + 0.5,
								ModSons.Som.ESTALO, Diretor.volumePara(p, vidro.getX() + 0.5, vidro.getY() + 0.5, vidro.getZ() + 0.5, 0.50F), 0.58F);
						level.sendParticles(ParticleTypes.ASH, vidro.getX() + 0.5, vidro.getY() + 0.5, vidro.getZ() + 0.5,
								3, 0.12, 0.22, 0.12, 0.001);
					}
					Depuracao.log(p, seg, "CENA id=" + e.cenaJanelaId + " etapa=TOQUE_NO_VIDRO");
				}
			}
			case TOQUE -> {
				if (!presente) {
					silencioCenaJanela(p, e, seg, rnd);
					return;
				}
				if (Diretor.pontoNaFrente(p, h.position().add(0, 1.4, 0), Percepcao.conePercebeu(p))) {
					if (!e.cenaJanelaTeste) {
						Vestigios.de(p).registrar(h.blockPosition(), Vestigios.Tipo.DESAPARECIMENTO, seg);
					}
					h.sumir(level, false, "JANELA_VISTA");
					silencioCenaJanela(p, e, seg, rnd);
				} else if (seg >= e.cenaJanelaAte) {
					h.sumir(level, false, "JANELA_NAO_VISTA");
					silencioCenaJanela(p, e, seg, rnd);
				}
			}
			case SILENCIO -> {
				if (seg >= e.cenaJanelaAte) {
					e.cenaJanela = EstadoJogador.CenaJanela.NENHUMA;
					e.cenaJanelaPos = null;
					e.cenaJanelaVistaDesde = -1;
					e.ultimoEventoSeg = seg;
					e.proximoEvento = Math.max(e.proximoEvento, seg + 30);
					Depuracao.log(p, seg, "SILENCIO fim motivo=FIM_CENA_JANELA cena=" + e.cenaJanelaId);
					Depuracao.log(p, seg, "CENA id=" + e.cenaJanelaId + " FIM");
				}
			}
		}
	}

	@Nullable
	private static JanelaAlvo acharJanelaCena(ServerLevel level, ServerPlayer p, EstadoJogador e) {
		Diretor.atualizarCacheAmbiente(level, p, e);
		JanelaAlvo melhor = null;
		double melhorNota = -999;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (BlockPos vidro : e.janelas) {
			Vec3 centroVidro = Vec3.atCenterOf(vidro);
			double dv = Diretor.distancia(p, centroVidro);
			if (dv < 3 || dv > 16 || Diretor.pontoNaFrente(p, centroVidro, Percepcao.coneSeguro(p))) {
				continue;
			}
			for (int[] d : dirs) {
				double x = vidro.getX() + 0.5 + d[0] * 1.35;
				double z = vidro.getZ() + 0.5 + d[1] * 1.35;
				BlockPos chao = Diretor.acharChao(level, x, vidro.getY(), z);
				if (chao == null || Math.abs(chao.getY() - vidro.getY()) > 3 || Diretor.naTela(p, chao)
						|| Diretor.emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
					continue;
				}
				double dc = Math.sqrt(Diretor.distanciaSqr(p, chao.getX() + 0.5, chao.getZ() + 0.5));
				if (dc < dv - 0.3 || dc > 19) {
					continue; // prefere o lado de fora: um pouco mais longe do jogador que o vidro
				}
				double nota = (Diretor.temCobertura(level, p, chao) ? 1.0 : 0.0) - Math.abs(dc - 8.0) * 0.04;
				if (nota > melhorNota) {
					melhorNota = nota;
					melhor = new JanelaAlvo(vidro, chao);
				}
			}
		}
		return melhor;
	}

	private static void silencioCenaJanela(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		e.cenaJanela = EstadoJogador.CenaJanela.SILENCIO;
		e.cenaJanelaAte = seg + 55 + rnd.nextInt(46);
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"CENA id=%s etapa=SILENCIO duracao=%ds silencioAte=%ds", e.cenaJanelaId,
				e.cenaJanelaAte - seg, e.cenaJanelaAte));
		Diretor.logSilencioInicio(p, seg, "FIM_CENA_JANELA", e.cenaJanelaId, e.cenaJanelaAte - seg);
	}
}
