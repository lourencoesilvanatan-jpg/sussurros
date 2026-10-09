package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.registro.ModSons;

/**
 * Cena "Ele voltou com você" (v0.4.2a): depois de um tempo longe, na volta para casa: passos no
 * caminho por onde o jogador veio, a porta de sempre se mexe e, na fase 3+, ele aparece no caminho
 * de volta.
 *
 * O Diretor chama verificar/conduzir a cada segundo. O estado da cena fica em {@link EstadoJogador};
 * manifestação, rastro, portas e sons ainda vêm dos helpers do Diretor.
 */
final class CenaVoltouComVoce {
	private CenaVoltouComVoce() {
	}

	/** Detecta a volta para casa depois de 3+ minutos a 55+ blocos da cama e, às vezes, começa a cena. */
	static void verificarVoltaParaCasa(ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg, RandomSource rnd) {
		if (m.get(Memoria.TEM_CAMA) != 1) {
			e.longeDesde = -1;
			e.voltaPendente = false;
			return;
		}
		double d = Math.sqrt(Diretor.distanciaSqr(p, m.get(Memoria.CAMA_X) + 0.5, m.get(Memoria.CAMA_Z) + 0.5));
		if (d >= 55) {
			if (e.longeDesde < 0) {
				e.longeDesde = seg;
			}
			return;
		}
		if (e.longeDesde >= 0) {
			if (seg - e.longeDesde >= 180) {
				e.voltaPendente = true;
				Depuracao.log(p, seg, "volta para casa: esteve longe " + (seg - e.longeDesde) + "s");
			}
			e.longeDesde = -1;
		}
		if (!e.voltaPendente || d > 16) {
			return;
		}
		e.voltaPendente = false; // um sorteio por volta
		boolean criaturaPresente = e.criatura != null && !e.criatura.isRemoved();
		if (fase < 2 || e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA || e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA
				|| e.estado == EstadoDiretor.AMEACANDO || e.estado == EstadoDiretor.RECUANDO
				|| seg < e.cenaCasaLiberadaEm || criaturaPresente) {
			Depuracao.log(p, seg, "volta para casa: não é hora (fase, estado ou intervalo)");
			return;
		}
		double chance = e.cenasCasaFeitas == 0 ? 0.6 : 0.25;
		if (rnd.nextDouble() >= chance) {
			Depuracao.log(p, seg, "volta para casa: desta vez não");
			return;
		}
		iniciarCenaCasa(p, e, seg, rnd, false);
	}

	static void iniciarCenaCasa(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean teste) {
		e.cenaCasa = EstadoJogador.CenaCasa.ESPERA;
		e.cenaCasaDesde = seg;
		e.cenaCasaAte = seg + 10 + rnd.nextInt(11);
		e.cenaCasaTeste = teste;
		if (!teste) {
			e.cenasCasaFeitas++;
			e.cenaCasaLiberadaEm = seg + 1500;
		}
		e.cenaCasaId = Diretor.novoIdCena();
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CENA id=%s tipo=VOLTOU_COM_VOCE INICIO teste=%s espera=%ds",
				e.cenaCasaId, teste ? "sim" : "nao", e.cenaCasaAte - seg));
	}

	/**
	 * Nada imediato. Depois: algo anda no caminho por onde você voltou, do lado de fora;
	 * a porta de sempre se mexe; e (fase 3+) ele está num ponto desse caminho, sem aviso, com uma espreita no máximo.
	 * No fim, 90-150 s de silêncio. Nada aleatório atrapalha enquanto isso.
	 */
	static void conduzirCenaCasa(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			long seg, long tick, RandomSource rnd) {
		HospedeEntity atual = e.criatura;
		boolean criaturaPresente = atual != null && !atual.isRemoved();
		boolean teste = e.cenaCasaTeste;
		switch (e.cenaCasa) {
			case NENHUMA -> {
			}
			case ESPERA -> {
				if (seg >= e.cenaCasaAte) {
					e.cenaCasa = EstadoJogador.CenaCasa.SOM_FORA;
					e.cenaCasaDesde = seg;
				}
			}
			case SOM_FORA -> {
				if (Diretor.bloqueado(p, e, tick) && seg - e.cenaCasaDesde < 30) {
					return;
				}
				somNoCaminho(level, p, m, e, seg, tick, teste);
				e.cenaCasa = EstadoJogador.CenaCasa.PORTA;
				e.cenaCasaDesde = seg;
				e.cenaCasaAte = seg + 6 + rnd.nextInt(7);
			}
			case PORTA -> {
				if (seg < e.cenaCasaAte) {
					return;
				}
				// Se existe uma porta habitual, a cena espera ESSA porta sair da sua visão.
				// Antes, olhar para ela fazia a cena recorrer a outra porta próxima, quebrando a âncora narrativa.
				BlockPos habitualExistente = Diretor.portaHabitualExistente(level, p);
				BlockPos porta = null;
				boolean portaHabitual = habitualExistente != null;
				if (habitualExistente != null) {
					Vec3 centro = Vec3.atCenterOf(habitualExistente);
					if (!Diretor.pontoNaFrente(p, centro, 0.5)) {
						porta = habitualExistente;
					} else if (seg - e.cenaCasaAte < 15) {
						return; // espera você parar de olhar para a porta de sempre
					}
				} else {
					porta = Diretor.acharPorta(level, p);
					if (porta == null && seg - e.cenaCasaAte < 15) {
						return;
					}
				}
				if (porta != null) {
					portaNaCena(level, p, e, porta, seg, tick, teste, portaHabitual);
				} else {
					Depuracao.log(p, seg, "CENA id=" + e.cenaCasaId + " etapa=PORTA SEM_PORTA (pulou)");
				}
				if (fase >= 3 || teste) {
					e.cenaCasa = EstadoJogador.CenaCasa.ESPERA_DENTRO;
					e.cenaCasaAte = seg + 10 + rnd.nextInt(16);
				} else {
					Depuracao.log(p, seg, "CENA id=" + e.cenaCasaId + " etapa=PRESENCA PULADA motivo=FASE_2");
					silencioCenaCasa(p, e, seg, rnd); // fase 2: só o som e a porta; ele ainda não aparece
				}
			}
			case ESPERA_DENTRO -> {
				if (seg >= e.cenaCasaAte) {
					e.cenaCasa = EstadoJogador.CenaCasa.PRESENCA;
					e.cenaCasaDesde = seg;
				}
			}
			case PRESENCA -> {
				if (criaturaPresente || Diretor.bloqueado(p, e, tick)) {
					return;
				}
				if (presencaNoCaminho(level, p, e, seg, tick, teste)) {
					e.cenaCasa = EstadoJogador.CenaCasa.ESPREITANDO;
					e.cenaCasaDesde = seg;
				} else if (seg - e.cenaCasaDesde > 30) {
					Depuracao.log(p, seg, "CENA id=" + e.cenaCasaId + " etapa=PRESENCA SEM_LUGAR");
					silencioCenaCasa(p, e, seg, rnd);
				}
			}
			case ESPREITANDO -> {
				if (!criaturaPresente) {
					silencioCenaCasa(p, e, seg, rnd);
				} else if (seg - e.cenaCasaDesde > 120) {
					atual.sumir(level, false, "LIMITE_DA_CENA");
				}
			}
			case SILENCIO -> {
				if (seg >= e.cenaCasaAte) {
					e.cenaCasa = EstadoJogador.CenaCasa.NENHUMA;
					e.ultimoEventoSeg = seg;
					e.proximoEvento = Math.max(e.proximoEvento, seg + 30);
					Depuracao.log(p, seg, "SILENCIO fim motivo=FIM_CENA_CASA cena=" + e.cenaCasaId);
					Depuracao.log(p, seg, "CENA id=" + e.cenaCasaId + " FIM");
				}
			}
		}
	}

	private static void silencioCenaCasa(ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd) {
		e.cenaCasa = EstadoJogador.CenaCasa.SILENCIO;
		e.cenaCasaAte = seg + 90 + rnd.nextInt(61);
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CENA id=%s etapa=SILENCIO duracao=%ds silencioAte=%ds",
				e.cenaCasaId, e.cenaCasaAte - seg, e.cenaCasaAte));
		Diretor.logSilencioInicio(p, seg, "FIM_CENA_CASA", e.cenaCasaId, e.cenaCasaAte - seg);
	}

	/** Passos (e às vezes um estalo) num ponto do caminho por onde você voltou, do lado de fora. */
	private static void somNoCaminho(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick, boolean teste) {
		RandomSource rnd = level.getRandom();
		boolean temCama = m.get(Memoria.TEM_CAMA) == 1;
		double camaX = m.get(Memoria.CAMA_X) + 0.5;
		double camaZ = m.get(Memoria.CAMA_Z) + 0.5;
		Vec3 escolhido = null;
		String nota = "LADO_DE_FORA";
		for (int i = 0; i < 6 && escolhido == null; i++) {
			Rastro.Ponto pt = Diretor.pontoDoRastro(p, e, seg, 20, 150, 10, 28, false);
			if (pt == null) {
				break;
			}
			double dx = pt.x() - camaX;
			double dz = pt.z() - camaZ;
			if (!temCama || Math.sqrt(dx * dx + dz * dz) > 12) {
				escolhido = new Vec3(pt.x(), pt.y(), pt.z());
				nota = "RASTRO idadeRastro=" + (seg - pt.seg()) + "s";
			}
		}
		if (escolhido == null) {
			// Sem um bom ponto do rastro: do lado oposto à cama, que é "lá fora".
			double ang = temCama ? Diretor.anguloRelativo(p, camaX, camaZ) + 180 : 180;
			escolhido = Diretor.pontoRelativo(p, ang + (rnd.nextDouble() - 0.5) * 60, 14 + rnd.nextInt(7));
		}
		Vec3 lugar = escolhido;
		BlockPos chao = BlockPos.containing(lugar.x, lugar.y - 0.5, lugar.z);
		BlockState piso = level.getBlockState(chao);
		if (piso.isAir()) {
			piso = level.getBlockState(chao.below());
		}
		SoundEvent passo = piso.getSoundType().getStepSound();
		float volume = Diretor.volumePara(p, lugar.x, lugar.y, lugar.z, 0.8F);
		int n = 2 + rnd.nextInt(2);
		for (int i = 0; i < n; i++) {
			Diretor.agendar(level, i * (9 + rnd.nextInt(4)), () ->
					ModSons.tocarEventoPara(p, passo, SoundSource.HOSTILE, lugar.x, lugar.y, lugar.z, volume, 0.75F));
		}
		boolean comEstalo = rnd.nextFloat() < 0.3F; // mesma chamada de antes, só guardada para o log
		if (comEstalo) {
			Diretor.agendar(level, n * 12 + 6, () -> ModSons.tocarPara(p, lugar.x, lugar.y + 0.5, lugar.z, ModSons.Som.ESTALO, volume, 0.9F));
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CENA id=%s etapa=RASTRO som=%dxPASSO%s motivoPosicao=%s pos=%s dist=%.1f",
				e.cenaCasaId, n, comEstalo ? "+ESTALO" : "", nota, Diretor.pos(lugar.x, lugar.y, lugar.z), Diretor.distancia(p, lugar)));
		if (!teste) {
			Diretor.posEvento(p, e, Evento.PASSOS, lugar, Diretor.limitar(1 - Diretor.distancia(p, lugar) / 28.0, 0, 1), seg, tick);
		}
	}

	/** A porta de sempre se mexe... e às vezes, um pouco depois, de novo. Como se alguém tivesse entrado. */
	private static void portaNaCena(ServerLevel level, ServerPlayer p, EstadoJogador e, BlockPos porta, long seg, long tick,
			boolean teste, boolean portaHabitual) {
		RandomSource rnd = level.getRandom();
		Diretor.alternarPorta(level, porta);
		boolean deNovo = rnd.nextFloat() < 0.5F;
		if (deNovo) {
			Diretor.agendar(level, 25 + rnd.nextInt(20), () -> Diretor.alternarPorta(level, porta));
		}
		Vec3 fonte = Vec3.atCenterOf(porta);
		double obs = Diretor.limitar(1 - Diretor.distancia(p, fonte) / 16.0, 0, 1);
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CENA id=%s etapa=PORTA porta=%s acao=%s habitual=%s dist=%.1f",
				e.cenaCasaId, Diretor.pos(porta.getX(), porta.getY(), porta.getZ()), deNovo ? "MEXEU_DUAS_VEZES" : "MEXEU",
				portaHabitual ? "sim" : "nao", Diretor.distancia(p, fonte)));
		if (!teste) {
			if (obs < 0.5) {
				Diretor.adicionarAtrasado(e, Evento.PORTA, fonte, seg);
			}
			Diretor.posEvento(p, e, Evento.PORTA, fonte, obs, seg, tick);
		}
	}

	/** Ele aparece, sem aviso, num ponto do caminho que você usou para voltar. Uma espreita, no máximo. */
	private static boolean presencaNoCaminho(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick, boolean teste) {
		PedidoManifestacao pedido = teste ? PedidoManifestacao.deComando(Evento.PRESENCA) : PedidoManifestacao.doDiretor(Evento.PRESENCA);
		boolean ok;
		
			ok = Diretor.invocarNoRastro(level, p, e, HospedeEntity.Modo.ESPREITAR, seg, 15, 150, 14, 35, 20 * 90, 8.0, pedido);
			if (!ok) {
				ok = Diretor.invocar(level, p, e, HospedeEntity.Modo.ESPREITAR, 55, 85, 18, 30, 20 * 90, 1.0, true, 8.0, true, pedido);
			}

		HospedeEntity h = e.criatura;
		if (ok && h != null) {
			h.definirMaxReposicoes(1);
			Depuracao.log(p, seg, "CENA id=" + e.cenaCasaId + " etapa=PRESENCA manifestacao=" + h.getIdManifestacao() + " anuncio=nenhum");
			if (!teste) {
				Diretor.posEvento(p, e, Evento.PRESENCA, null, 0, seg, tick);
			}
		}
		return ok;
	}
}
