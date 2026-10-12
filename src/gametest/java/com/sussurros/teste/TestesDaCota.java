package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Atencao;
import com.sussurros.assombracao.Cota;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.assombracao.Memoria;
import com.sussurros.assombracao.PrimeiroContato;
import com.sussurros.entidade.HospedeEntity;

/**
 * A cota de corpo (0.9.0-alpha20): depois do primeiro contato, da fase 3 em diante, o jogador vê a criatura de
 * verdade de tempos em tempos.
 *
 * Todos os testes usam o túnel de pedra de TestesDePerto (dois blocos de altura, uma curva iluminada dez blocos
 * atrás do jogador): é um lugar em que a colocação fácil existe de dia e de noite, e em que nada mais acontece
 * nos poucos segundos de cada teste. Conferem a regra. Se vinte e trinta minutos são os números certos, só uma
 * sessão de gente diz.
 */
public class TestesDaCota {
	private static final int VINTE_MINUTOS = 20 * 60;
	private static final int TRINTA_MINUTOS = 30 * 60;

	/** Espera a luz assentar e o Diretor ler o túnel como subsolo. Devolve true quando já pode começar. */
	private static boolean pronto(GameTestHelper helper, ServerPlayer jogador, int relogio) {
		if (relogio > 200) {
			helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
		}
		return relogio >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador));
	}

	private static ServerPlayer jogadorNaFase3(GameTestHelper helper, BlockPos pes) {
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes);
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		return jogador;
	}

	/**
	 * Trinta minutos sem ser visto: a cota tenta sozinha. Ele nasce no lugar mais fácil de ver (a curva
	 * iluminada, de perto), fora da tela. Enquanto ninguém olha, a tentativa não gasta atenção; olhado, ele some
	 * como sempre, e aí sim a cota está paga: o relógio zera e a atenção é cobrada.
	 */
	@GameTest(maxTicks = 700)
	public void aCotaTentaSozinhaESoCustaQuandoEVista(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -6900);
		ServerPlayer jogador = jogadorNaFase3(helper, pes);
		ServerLevel level = helper.getLevel();
		int[] relogio = {0};
		int[] passo = {0};
		int[] desde = {0};
		double[] saldo = new double[1];
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 3) {
				return;
			}
			level.getChunkSource().move(jogador);
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					TestesDePerto.olharParaOSul(jogador);
					if (pronto(helper, jogador, relogio[0])) {
						helper.assertTrue(h == null, "antes de a cota dever, ninguém deveria estar ali");
						saldo[0] = Atencao.saldoParaTeste(jogador);
						Cota.relogioParaTeste(jogador, TRINTA_MINUTOS);
						passo[0] = 1;
						desde[0] = relogio[0];
					}
				}
				case 1 -> {
					TestesDePerto.olharParaOSul(jogador);
					if (h == null) {
						if (relogio[0] - desde[0] > 60) {
							helper.fail("com trinta minutos sem ser visto, a cota deveria ter tentado em três segundos");
						}
						return;
					}
					helper.assertTrue(h.ehCota() && h.ehCotaDaRegra(), "esta aparição deveria ser a da cota");
					helper.assertTrue(h.ehDePerto() && TestesDePerto.naCurva(h, pes),
							"a cota usa o lugar mais fácil de ver: a curva iluminada; nasceu em " + h.blockPosition().toShortString());
					helper.assertTrue(Diretor.temLuzParaVer(level, h.blockPosition()), "a cota só o põe onde há luz para vê-lo");
					helper.assertTrue(Cota.tentativasDeTeste(jogador) == 1, "deveria contar uma tentativa; contou " + Cota.tentativasDeTeste(jogador));
					passo[0] = 2;
					desde[0] = relogio[0];
				}
				case 2 -> {
					// Três segundos sem olhar: ele continua lá, e nada foi cobrado.
					TestesDePerto.olharParaOSul(jogador);
					helper.assertTrue(h != null, "sem ser olhado, ele deveria continuar lá");
					if (relogio[0] - desde[0] >= 60) {
						helper.assertTrue(Atencao.saldoParaTeste(jogador) >= saldo[0] - 0.5,
								"a tentativa que ninguém viu não gasta atenção: de " + saldo[0] + " para " + Atencao.saldoParaTeste(jogador));
						helper.assertTrue(Cota.relogioDeTeste(jogador) >= TRINTA_MINUTOS, "sem ser visto, o relógio da cota continua andando");
						passo[0] = 3;
						desde[0] = relogio[0];
					}
				}
				case 3 -> {
					// O jogador vira e olha: ele some, e dois segundos depois a cota está paga.
					if (h != null) {
						jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
						if (relogio[0] - desde[0] > 40) {
							helper.fail("olhado, de perto, ele deveria ter sumido");
						}
						return;
					}
					if (Cota.relogioDeTeste(jogador) >= TRINTA_MINUTOS) {
						if (relogio[0] - desde[0] > 100) {
							helper.fail("visto de verdade, o relógio da cota deveria ter zerado; está em " + Cota.relogioDeTeste(jogador));
						}
						return;
					}
					helper.assertTrue(Cota.relogioDeTeste(jogador) < 60 && Cota.tentativasDeTeste(jogador) == 0,
							"a cota paga zera o relógio e as tentativas: " + Cota.relogioDeTeste(jogador) + " s, " + Cota.tentativasDeTeste(jogador));
					helper.assertTrue(Atencao.saldoParaTeste(jogador) < saldo[0] - 5,
							"vista, a aparição da cota custa atenção: de " + saldo[0] + " para " + Atencao.saldoParaTeste(jogador));
					helper.assertTrue(jogador.getHealth() == 20.0F, "ele não toca no jogador");
					JogadorDeTeste.remover(jogador);
					passo[0] = 4;
					helper.succeed();
				}
				default -> {
				}
			}
		});
	}

	/**
	 * A tentativa que ninguém viu: ele espera quarenta segundos e vai embora. A cota continua devendo (o relógio
	 * não zera) e a tentativa seguinte não vem em seguida: espera minutos.
	 */
	@GameTest(maxTicks = 1500)
	public void semSerVistaACotaContinuaDevendo(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -7000);
		ServerPlayer jogador = jogadorNaFase3(helper, pes);
		ServerLevel level = helper.getLevel();
		int[] relogio = {0};
		int[] passo = {0};
		int[] desde = {0};
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 2) {
				return;
			}
			level.getChunkSource().move(jogador);
			TestesDePerto.olharParaOSul(jogador);
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					if (pronto(helper, jogador, relogio[0])) {
						Cota.relogioParaTeste(jogador, TRINTA_MINUTOS);
						passo[0] = 1;
						desde[0] = relogio[0];
					}
				}
				case 1 -> {
					if (h != null) {
						helper.assertTrue(h.ehCota(), "esta aparição deveria ser a da cota");
						passo[0] = 2;
						desde[0] = relogio[0];
					} else if (relogio[0] - desde[0] > 60) {
						helper.fail("a cota deveria ter tentado");
					}
				}
				case 2 -> {
					// Ninguém olha. Ele fica os quarenta segundos dele e some; depois disso, mais cinco segundos sem nada.
					if (relogio[0] - desde[0] < 900) {
						if (relogio[0] - desde[0] > 820) {
							helper.assertTrue(h == null, "depois de ele ir embora sem ser visto, a cota não tenta de novo em seguida");
						}
						return;
					}
					helper.assertTrue(Cota.relogioDeTeste(jogador) >= TRINTA_MINUTOS + 30,
							"sem ser visto, a cota continua devendo; o relógio está em " + Cota.relogioDeTeste(jogador));
					helper.assertTrue(Cota.tentativasDeTeste(jogador) == 1, "foi uma tentativa só; contou " + Cota.tentativasDeTeste(jogador));
					JogadorDeTeste.remover(jogador);
					passo[0] = 3;
					helper.succeed();
				}
				default -> {
				}
			}
		});
	}

	/**
	 * Entre vinte e trinta minutos a cota não tenta sozinha: espera o Diretor. A presença comum que ele escolher
	 * nesse intervalo é a da cota (o lugar com luz). Sem a cota dever, a presença é a de sempre.
	 */
	@GameTest(maxTicks = 500)
	public void aPresencaDoDiretorPagaACotaQuandoElaDeve(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -7100);
		ServerPlayer jogador = jogadorNaFase3(helper, pes);
		ServerLevel level = helper.getLevel();
		int[] relogio = {0};
		int[] passo = {0};
		int[] desde = {0};
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 2) {
				return;
			}
			level.getChunkSource().move(jogador);
			TestesDePerto.olharParaOSul(jogador);
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					if (pronto(helper, jogador, relogio[0])) {
						helper.assertTrue(Diretor.eventoDoDiretorParaTeste(jogador, Evento.PRESENCA), "no túnel a presença deveria achar lugar");
						HospedeEntity comum = Diretor.criatura(jogador);
						helper.assertTrue(comum != null && !comum.ehCota(), "sem a cota dever, a presença é a de sempre");
						comum.sumir(level, false, "FIM_DO_TESTE");
						Cota.relogioParaTeste(jogador, VINTE_MINUTOS);
						passo[0] = 1;
						desde[0] = relogio[0];
					}
				}
				case 1 -> {
					// Três segundos: com vinte minutos a cota deve, mas ainda não tenta sozinha.
					helper.assertTrue(h == null, "antes dos trinta minutos a cota espera o Diretor");
					if (relogio[0] - desde[0] >= 60) {
						helper.assertTrue(Diretor.eventoDoDiretorParaTeste(jogador, Evento.PRESENCA), "a presença deveria achar lugar");
						HospedeEntity daCota = Diretor.criatura(jogador);
						helper.assertTrue(daCota != null && daCota.ehCota() && !daCota.ehCotaDaRegra(),
								"com a cota devendo, a presença do Diretor é a da cota");
						helper.assertTrue(TestesDePerto.naCurva(daCota, pes) && Diretor.temLuzParaVer(level, daCota.blockPosition()),
								"e nasce no lugar com luz; nasceu em " + daCota.blockPosition().toShortString());
						helper.assertTrue(Cota.tentativasDeTeste(jogador) == 1, "deveria contar uma tentativa");
						JogadorDeTeste.remover(jogador);
						passo[0] = 3;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	/**
	 * A cota só existe da fase 3 em diante (antes disso quem cuida de ele ser visto é o primeiro contato), e não
	 * põe ninguém dentro da casa do jogador: em casa, o lugar apertado e perto não vale.
	 */
	@GameTest(maxTicks = 700)
	public void aCotaNaoValeAntesDaFase3NemDentroDeCasa(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -7200);
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes);
		ServerLevel level = helper.getLevel();
		Diretor.definirFase(jogador, 2);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		int[] relogio = {0};
		int[] passo = {0};
		int[] desde = {0};
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 3) {
				return;
			}
			level.getChunkSource().move(jogador);
			TestesDePerto.olharParaOSul(jogador);
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					if (pronto(helper, jogador, relogio[0])) {
						Cota.relogioParaTeste(jogador, TRINTA_MINUTOS + 600);
						passo[0] = 1;
						desde[0] = relogio[0];
					}
				}
				case 1 -> {
					// Fase 2: cinco segundos sem nada, e o relógio parado.
					helper.assertTrue(h == null, "na fase 2 a cota não existe");
					if (relogio[0] - desde[0] >= 100) {
						helper.assertTrue(Cota.relogioDeTeste(jogador) == TRINTA_MINUTOS + 600, "na fase 2 o relógio da cota não anda");
						// Fase 3, mas com a cama do jogador ali mesmo: é a casa dele.
						Memoria m = Memoria.de(jogador);
						m.set(Memoria.TEM_CAMA, 1);
						m.set(Memoria.CAMA_X, jogador.getBlockX());
						m.set(Memoria.CAMA_Y, jogador.getBlockY());
						m.set(Memoria.CAMA_Z, jogador.getBlockZ());
						m.salvar();
						Diretor.definirFase(jogador, 3);
						passo[0] = 2;
						desde[0] = relogio[0];
					}
				}
				case 2 -> {
					// Em casa: cinco segundos, e ninguém aparece a dez blocos dele, dentro de casa.
					helper.assertTrue(h == null, "em casa a cota não põe ninguém de perto; contexto " + Diretor.contextoParaTeste(jogador));
					if (relogio[0] - desde[0] >= 100) {
						helper.assertTrue("CASA".equals(Diretor.contextoParaTeste(jogador)), "o túnel com a cama deveria ser lido como CASA");
						Memoria m = Memoria.de(jogador);
						m.set(Memoria.TEM_CAMA, 0);
						m.salvar();
						passo[0] = 3;
						desde[0] = relogio[0];
					}
				}
				case 3 -> {
					// Fora de casa, na fase 3: agora sim.
					if (h != null) {
						helper.assertTrue(h.ehCota() && TestesDePerto.naCurva(h, pes), "fora de casa, na fase 3, a cota tenta");
						JogadorDeTeste.remover(jogador);
						passo[0] = 4;
						helper.succeed();
					} else if (relogio[0] - desde[0] > 200) {
						helper.fail("fora de casa, na fase 3, a cota deveria ter tentado; contexto " + Diretor.contextoParaTeste(jogador));
					}
				}
				default -> {
				}
			}
		});
	}
}
