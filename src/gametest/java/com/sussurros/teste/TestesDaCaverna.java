package com.sussurros.teste;

import java.util.List;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.entidade.HospedeEntity;

/**
 * A caverna com corpo (0.9.0-alpha22 e alpha23): a rachadura, a pedrinha, os morcegos, a tampa do baú, a
 * passagem fechada e a regra das famílias.
 *
 * Tudo dentro de um túnel de pedra montado pelo teste (o mundo de teste é um gramado, e nada disto acontece
 * em gramado): o mesmo túnel com curva dos testes de perto. Conferem a regra contra um jogador de mentira: o
 * que chega a ele, o que não chega a quem está ao lado, e que o mundo não muda. Se a rachadura se vê, se os
 * morcegos passam bonito e se o estalo parece pedra, só as fotos e o dono dizem.
 */
public class TestesDaCaverna {
	private static final String ETIQUETA = "sussurros_morcego";
	/**
	 * A folga de lugar de um som. O pacote de som leva a posição em oitavos de bloco, mas quem a lê recebe um
	 * número de precisão simples, e o jogo põe as estruturas de teste num lugar diferente do mundo a cada
	 * execução: a dez milhões de blocos da origem a posição chega arredondada para o bloco inteiro. Um teste que
	 * pedia o som a 0,6 bloco do lugar passou em três execuções e falhou na quarta. Com o nome do som na conta,
	 * a folga pode ser larga.
	 */
	private static final double FOLGA = 1.8;

	/** Um jogador de pé no túnel, "dx" blocos a leste do eixo (o amigo fica numa reentrância ao lado). */
	private static ServerPlayer jogadorEm(GameTestHelper helper, BlockPos pes, double dx) {
		Vec3 relativo = helper.relativeVec(new Vec3(pes.getX() + 0.5 + dx, pes.getY(), pes.getZ() + 0.5));
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, relativo.x, relativo.y, relativo.z);
		Diretor.esquecer(jogador);
		return jogador;
	}

	/** A reentrância do amigo: um bloco a leste do jogador, colado nele. */
	private static void abrirNicho(ServerLevel level, BlockPos pes) {
		level.setBlock(pes.offset(1, 0, 0), Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pes.offset(1, 1, 0), Blocks.AIR.defaultBlockState(), 3);
	}

	/** O jogador quebra o bloco, como quem minera: o teste repõe a pedra antes, para haver sempre o que quebrar. */
	private static void quebrar(ServerPlayer jogador, ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
		jogador.gameMode.destroyBlock(pos);
	}

	private static void somar(int[] total, int[] parcela) {
		for (int i = 0; i < total.length; i++) {
			total[i] += parcela[i];
		}
	}

	private static List<Bat> morcegosDoMod(ServerLevel level, BlockPos centro) {
		return level.getEntitiesOfClass(Bat.class, new AABB(centro).inflate(30.0), m -> m.entityTags().contains(ETIQUETA));
	}

	/**
	 * Ele minera, e o bloco ao lado do que acabou de quebrar racha sozinho, com duas batidas de dentro da pedra.
	 * Só para ele: o amigo colado nele não recebe a rachadura nem as batidas. O bloco de verdade continua pedra.
	 * Uma por descida. Se ele quebra o bloco rachado, a marca sai, e não havia nada.
	 */
	@GameTest(maxTicks = 2600)
	public void minerandoOBlocoAoLadoRacha(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -7400);
		ServerLevel level = helper.getLevel();
		abrirNicho(level, pes);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerPlayer amigo = jogadorEm(helper, pes, 1);
		// O fundo do túnel, na altura dos olhos: é o bloco que ele quebra.
		BlockPos alvo = pes.offset(0, 1, 4);
		int[] relogio = {0};
		int[] passo = {0};
		int[] quebras = {0};
		BlockPos[] rachado = new BlockPos[1];
		int[] doAlvo = new int[4];
		int[] doAmigo = new int[4];
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 4) {
				return;
			}
			level.getChunkSource().move(jogador);
			level.getChunkSource().move(amigo);
			TestesDePerto.olharParaOSul(jogador);
			TestesDePerto.olharParaOSul(amigo);
			relogio[0]++;
			switch (passo[0]) {
				case 0 -> {
					if (relogio[0] > 200 && !"SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
						helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
					}
					long descida = Diretor.descidaParaTeste(jogador);
					// Antes da fase 2 a pedra não responde, por mais que ele minere.
					if (descida >= 60 && descida < 88 && relogio[0] % 10 == 0) {
						quebrar(jogador, level, alvo);
						helper.assertTrue(Diretor.rachaduraParaTeste(jogador) == null, "na fase 0 nada racha");
					}
					// A fase muda logo depois de um segundo do Diretor: as quebras seguintes cabem todas antes do próximo.
					if (descida >= 91 && level.getGameTime() % 20 == 1) {
						Diretor.definirFase(jogador, 2);
						JogadorDeTeste.recebidos(jogador);
						JogadorDeTeste.recebidos(amigo);
						passo[0] = 1;
					}
				}
				case 1 -> {
					quebrar(jogador, level, alvo);
					quebras[0]++;
					BlockPos r = Diretor.rachaduraParaTeste(jogador);
					if (r == null) {
						helper.assertTrue(quebras[0] < Diretor.quebrasAteRacharParaTeste(jogador),
								"minerando, na quebra " + Diretor.quebrasAteRacharParaTeste(jogador) + " desta descida o bloco ao lado deveria ter rachado; já são " + quebras[0]);
						return;
					}
					helper.assertTrue(quebras[0] == Diretor.quebrasAteRacharParaTeste(jogador),
							"a rachadura vem na quebra " + Diretor.quebrasAteRacharParaTeste(jogador) + "; veio na " + quebras[0]);
					helper.assertTrue(r.distManhattan(alvo) == 1, "racha um vizinho do bloco quebrado; rachou " + r.toShortString());
					helper.assertFalse(r.equals(alvo.offset(0, 0, 1)), "racha o bloco AO LADO, não o de trás (que ele vai quebrar em seguida)");
					helper.assertTrue("RACHADURA".equals(Diretor.cavernaParaTeste(jogador)), "o resumo deveria dizer RACHADURA");
					rachado[0] = r;
					passo[0] = 2;
					relogio[0] = 0;
				}
				case 2 -> {
					somar(doAlvo, JogadorDeTeste.recebidos(jogador));
					somar(doAmigo, JogadorDeTeste.recebidos(amigo));
					if (relogio[0] == 25) {
						Vec3 centro = Vec3.atCenterOf(rachado[0]);
						helper.assertTrue(doAlvo[3] >= 4, "a rachadura cresce em cinco etapas, só para o alvo; chegaram " + doAlvo[3]);
						helper.assertTrue(doAmigo[3] == 0, "o amigo ao lado não vê a rachadura do outro; chegaram " + doAmigo[3]);
						helper.assertTrue(JogadorDeTeste.sonsVindosDe(jogador, centro, FOLGA, "sussurros:pancada") >= 2,
								"duas batidas de dentro do bloco; o alvo ouviu " + JogadorDeTeste.ultimosSons(jogador));
						helper.assertTrue(JogadorDeTeste.sonsVindosDe(amigo, centro, 6.0, "sussurros:pancada") == 0,
								"o amigo não ouve as batidas; ouviu " + JogadorDeTeste.ultimosSons(amigo));
						helper.assertTrue(level.getBlockState(rachado[0]).is(Blocks.STONE), "o bloco de verdade continua inteiro");
						helper.assertTrue(jogador.getHealth() == 20.0F, "a rachadura não toca no jogador");
						passo[0] = 3;
						relogio[0] = 0;
					}
				}
				case 3 -> {
					// Ele continua minerando: nesta descida não racha mais nada.
					quebrar(jogador, level, alvo);
					helper.assertTrue(rachado[0].equals(Diretor.rachaduraParaTeste(jogador)),
							"uma rachadura por descida; agora está em " + Diretor.rachaduraParaTeste(jogador));
					if (relogio[0] >= 20) {
						passo[0] = 4;
					}
				}
				case 4 -> {
					// Ele vai ver: quebra o bloco rachado. Pedra, e nada atrás.
					jogador.gameMode.destroyBlock(rachado[0]);
					helper.assertTrue(Diretor.rachaduraParaTeste(jogador) == null, "quebrado o bloco, a marca sai com ele");
					helper.assertTrue(level.getBlockState(rachado[0]).isAir(), "o bloco quebra como qualquer pedra");
					helper.assertTrue(Diretor.criatura(jogador) == null, "e não havia ninguém");
					JogadorDeTeste.remover(jogador);
					JogadorDeTeste.remover(amigo);
					passo[0] = 5;
					helper.succeed();
				}
				default -> {
				}
			}
		});
	}

	/**
	 * Quinze minutos debaixo da terra sem nada que se toque, e a próxima coisa é uma delas. Primeiro os morcegos:
	 * de três a cinco, saem da curva do túnel (fora da tela), passam por ele e somem em quinze segundos. Depois a
	 * pedrinha: lascas e estalos no chão à frente dele, só para ele.
	 */
	@GameTest(maxTicks = 2800)
	public void aDividaDoSubsoloTrazOQueSeToca(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -7500);
		ServerLevel level = helper.getLevel();
		abrirNicho(level, pes);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerPlayer amigo = jogadorEm(helper, pes, 1);
		Diretor.definirFase(jogador, 1);
		BlockPos curva = pes.offset(0, 0, -10);
		Vec3 daCurva = new Vec3(curva.getX() + 0.5, curva.getY() + 0.9, curva.getZ() + 0.5);
		BlockState chaoAntes = level.getBlockState(pes.offset(0, -1, 2));
		int[] relogio = {0};
		int[] passo = {0};
		int[] maisMorcegos = {0};
		double[] maisPerto = {Double.MAX_VALUE};
		int[] doAlvo = new int[4];
		int[] doAmigo = new int[4];
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 4) {
				return;
			}
			level.getChunkSource().move(jogador);
			level.getChunkSource().move(amigo);
			TestesDePerto.olharParaOSul(jogador);
			TestesDePerto.olharParaOSul(amigo);
			relogio[0]++;
			switch (passo[0]) {
				case 0 -> {
					if (relogio[0] > 200) {
						helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
					}
					if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
						helper.assertTrue("-".equals(Diretor.cavernaParaTeste(jogador)), "antes da dívida nada disto acontece");
						Diretor.definirSubsoloSemFisicoParaTeste(jogador, 15 * 60);
						JogadorDeTeste.recebidos(jogador);
						passo[0] = 1;
						relogio[0] = 0;
					}
				}
				case 1 -> {
					JogadorDeTeste.recebidos(jogador);
					if ("MORCEGOS".equals(Diretor.cavernaParaTeste(jogador))) {
						helper.assertTrue(curva.equals(Diretor.bocaDosMorcegosParaTeste(jogador)),
								"os morcegos saem da curva do túnel, às costas dele; saíram de " + Diretor.bocaDosMorcegosParaTeste(jogador));
						passo[0] = 2;
						relogio[0] = 0;
					} else if (relogio[0] > 60) {
						helper.fail("com a dívida aberta, os morcegos deveriam ter vindo em três segundos; veio " + Diretor.cavernaParaTeste(jogador));
					}
				}
				case 2 -> {
					JogadorDeTeste.recebidos(jogador);
					List<Bat> morcegos = morcegosDoMod(level, pes);
					maisMorcegos[0] = Math.max(maisMorcegos[0], morcegos.size());
					for (Bat m : morcegos) {
						maisPerto[0] = Math.min(maisPerto[0], Math.sqrt(m.distanceToSqr(jogador.getEyePosition())));
					}
					if (relogio[0] == 60) {
						helper.assertTrue(maisMorcegos[0] >= 3 && maisMorcegos[0] <= 5, "de três a cinco morcegos; foram " + maisMorcegos[0]);
						helper.assertTrue(maisPerto[0] < 3.0, "eles passam por ele; o mais perto chegou a " + maisPerto[0] + " blocos");
						helper.assertTrue(JogadorDeTeste.sonsVindosDe(jogador, daCurva, FOLGA, "sussurros:grave") >= 1,
								"antes deles, um baque na boca do túnel; o alvo ouviu " + JogadorDeTeste.ultimosSons(jogador));
						helper.assertTrue(Diretor.subsoloSemFisicoParaTeste(jogador) < 10, "coisa de corpo zera o relógio do subsolo");
						helper.assertTrue(jogador.getHealth() == 20.0F, "morcego não faz mal");
					}
					if (relogio[0] == 335) {
						helper.assertTrue(morcegos.isEmpty(), "em quinze segundos não sobra nenhum morcego do mod; sobraram " + morcegos.size());
						// A dívida abre de novo: agora é a vez da pedrinha, quando o respiro deixar.
						Diretor.definirSubsoloSemFisicoParaTeste(jogador, 15 * 60);
						JogadorDeTeste.recebidos(jogador);
						JogadorDeTeste.recebidos(amigo);
						passo[0] = 3;
						relogio[0] = 0;
					}
				}
				case 3 -> {
					somar(doAlvo, JogadorDeTeste.recebidos(jogador));
					somar(doAmigo, JogadorDeTeste.recebidos(amigo));
					if ("PEDRINHA".equals(Diretor.cavernaParaTeste(jogador))) {
						passo[0] = 4;
						relogio[0] = 0;
					} else if (relogio[0] > 1900) {
						helper.fail("passado o respiro, a pedrinha deveria ter vindo; o último foi " + Diretor.cavernaParaTeste(jogador));
					}
				}
				case 4 -> {
					somar(doAlvo, JogadorDeTeste.recebidos(jogador));
					somar(doAmigo, JogadorDeTeste.recebidos(amigo));
					if (relogio[0] == 15) {
						// Ela quica de três blocos e meio até dois e pouco, à frente dele (o único lado com chão).
						Vec3 chao = new Vec3(pes.getX() + 0.5, pes.getY(), pes.getZ() + 2.8);
						helper.assertTrue(doAlvo[1] >= 3, "três toques no chão, com lascas; partículas: " + doAlvo[1]);
						helper.assertTrue(JogadorDeTeste.sonsVindosDe(jogador, chao, 1.2 + FOLGA, "sussurros:estalo") >= 3,
								"um estalo em cada toque, à frente dele; o alvo ouviu " + JogadorDeTeste.ultimosSons(jogador));
						helper.assertTrue(doAmigo[1] == 0 && JogadorDeTeste.sonsVindosDe(amigo, chao, 6.0, "sussurros:estalo") == 0,
								"o amigo ao lado não vê nem ouve a pedrinha do outro (os morcegos, que são do mundo, ele ouve): " + doAmigo[1] + " partícula(s), e ouviu "
										+ JogadorDeTeste.ultimosSons(amigo));
						helper.assertTrue(Diretor.subsoloSemFisicoParaTeste(jogador) < 10, "coisa de objeto zera o relógio do subsolo");
						helper.assertTrue(level.getBlockState(pes.offset(0, -1, 2)).equals(chaoAntes), "o chão continua como estava");
						JogadorDeTeste.remover(jogador);
						JogadorDeTeste.remover(amigo);
						passo[0] = 5;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	/**
	 * Como o túnel dos testes de perto, mais comprido: a curva (com a tocha) fica treze blocos atrás do jogador.
	 * Assim cabem, entre 8 e 14 blocos, dois lugares a mais de três blocos e meio um do outro.
	 */
	private static BlockPos montarTunelLongo(GameTestHelper helper, int dz) {
		BlockPos pes = JogadorDeTeste.chao(helper, 4, dz);
		ServerLevel level = helper.getLevel();
		for (int x = -3; x <= 11; x++) {
			for (int z = -16; z <= 6; z++) {
				for (int y = 0; y <= 4; y++) {
					level.setBlock(pes.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
				}
			}
		}
		for (int y = 0; y <= 1; y++) {
			for (int z = -13; z <= 3; z++) {
				level.setBlock(pes.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);
			}
			for (int x = 0; x <= 8; x++) {
				level.setBlock(pes.offset(x, y, -13), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		level.setBlock(pes.offset(-1, 1, -13), Blocks.TORCH.defaultBlockState(), 3);
		return pes;
	}

	/**
	 * "A próxima coisa com lugar acontece naquele túnel." Sem morcegos, o lugar dele neste túnel é a curva (tem
	 * luz, e um passo o tira da vista). Com os morcegos tendo saído de outro trecho, a aparição seguinte nasce
	 * junto desse trecho, e não na curva. Para os morcegos não saírem da própria curva, o teste a tampa enquanto
	 * eles saem e a destampa depois.
	 */
	@GameTest(maxTicks = 900)
	public void aProximaAparicaoPrefereOTunelDosMorcegos(GameTestHelper helper) {
		BlockPos pes = montarTunelLongo(helper, -7600);
		ServerLevel level = helper.getLevel();
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		BlockPos curva = pes.offset(0, 0, -13);
		int[] relogio = {0};
		int[] passo = {0};
		BlockPos[] boca = new BlockPos[1];
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 4) {
				return;
			}
			level.getChunkSource().move(jogador);
			HospedeEntity h = Diretor.criatura(jogador);
			relogio[0]++;
			switch (passo[0]) {
				case 0 -> {
					TestesDePerto.olharParaOSul(jogador);
					if (relogio[0] > 200) {
						helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
					}
					if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
						String falha = Diretor.forcarEvento(jogador, Evento.PRESENCA);
						helper.assertTrue(falha == null, "no túnel a presença deveria achar lugar: " + falha);
						passo[0] = 1;
						relogio[0] = 0;
					}
				}
				case 1 -> {
					// O controle: sem morcegos, ele nasce na curva. Depois o jogador olha, e ele some.
					if (relogio[0] == 1) {
						helper.assertTrue(h != null && h.blockPosition().equals(curva),
								"sem morcegos, o lugar dele é a curva; nasceu em " + (h == null ? "lugar nenhum" : h.blockPosition().toShortString()));
					}
					if (h == null) {
						TestesDePerto.olharParaOSul(jogador);
						// A curva é tampada: os morcegos vão ter de sair de mais perto.
						level.setBlock(pes.offset(0, 0, -10), Blocks.STONE.defaultBlockState(), 3);
						level.setBlock(pes.offset(0, 1, -10), Blocks.STONE.defaultBlockState(), 3);
						passo[0] = 2;
						relogio[0] = 0;
					} else if (relogio[0] > 5) {
						jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
						if (relogio[0] > 60) {
							helper.fail("olhado, de perto, ele deveria ter sumido");
						}
					}
				}
				case 2 -> {
					TestesDePerto.olharParaOSul(jogador);
					if (relogio[0] == 10) {
						helper.assertTrue(Diretor.testarPerturbacaoPeloNome(jogador, "MORCEGOS"), "no túnel tampado ainda há de onde os morcegos saírem");
						boca[0] = Diretor.bocaDosMorcegosParaTeste(jogador);
						int z = boca[0] == null ? 0 : boca[0].getZ() - pes.getZ();
						helper.assertTrue(boca[0] != null && boca[0].getX() == pes.getX() && (z == -8 || z == -9),
								"com a curva tampada, os morcegos saem do trecho reto, a oito ou nove blocos; saíram de " + boca[0]);
						// A curva volta a existir.
						level.setBlock(pes.offset(0, 0, -10), Blocks.AIR.defaultBlockState(), 3);
						level.setBlock(pes.offset(0, 1, -10), Blocks.AIR.defaultBlockState(), 3);
					}
					if (relogio[0] == 40) {
						String falha = Diretor.forcarEvento(jogador, Evento.PRESENCA);
						helper.assertTrue(falha == null, "a presença deveria achar lugar: " + falha);
						passo[0] = 3;
						relogio[0] = 0;
					}
				}
				case 3 -> {
					helper.assertTrue(h != null, "ele deveria ter aparecido");
					helper.assertFalse(h.blockPosition().equals(curva), "depois dos morcegos, o lugar dele deixa de ser a curva");
					helper.assertTrue(h.blockPosition().closerThan(boca[0], 3.5),
							"ele nasce junto de onde os morcegos saíram (" + boca[0].toShortString() + "); nasceu em " + h.blockPosition().toShortString());
					h.discard();
					for (Bat m : morcegosDoMod(level, pes)) {
						m.discard();
					}
					JogadorDeTeste.remover(jogador);
					passo[0] = 5;
					helper.succeed();
				}
				default -> {
				}
			}
		});
	}

	/**
	 * A regra das famílias, debaixo da terra: com duas saídas de luz nas últimas cinco, a tocha que ia mudar de
	 * cor espera. Passadas quatro coisas de outras famílias, ela muda. (As tochas ficam às costas dele: a troca
	 * só acontece fora da tela.)
	 */
	@GameTest(maxTicks = 1300)
	public void debaixoDaTerraALuzEsperaAVez(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -7700);
		ServerLevel level = helper.getLevel();
		for (int z = -2; z >= -6; z -= 2) {
			level.setBlock(pes.offset(0, 0, z), Blocks.TORCH.defaultBlockState(), 3);
		}
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		int[] relogio = {0};
		int[] passo = {0};
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 2) {
				return;
			}
			level.getChunkSource().move(jogador);
			TestesDePerto.olharParaOSul(jogador);
			relogio[0]++;
			switch (passo[0]) {
				case 0 -> {
					if (relogio[0] > 200) {
						helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
					}
					if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
						Diretor.definirFase(jogador, 2);
						helper.assertFalse(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "sem nada ter saído, nada é barrado");
						Diretor.registrarFamiliaParaTeste(jogador, "LUZ");
						helper.assertFalse(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "uma saída de luz não barra a seguinte");
						Diretor.registrarFamiliaParaTeste(jogador, "LUZ");
						helper.assertTrue(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "duas de luz: a terceira espera");
						helper.assertFalse(Diretor.familiaBarradaParaTeste(jogador, "SOM"), "a regra é por família");
						Diretor.registrarFamiliaParaTeste(jogador, "CORPO");
						Diretor.registrarFamiliaParaTeste(jogador, "CORPO");
						helper.assertFalse(Diretor.familiaBarradaParaTeste(jogador, "CORPO"), "o que se toca nunca é barrado");
						passo[0] = 1;
						relogio[0] = 0;
					}
				}
				case 1 -> {
					// Vinte e dois segundos: a vez das chamas pálidas passa uma vez inteira, e nenhuma tocha muda.
					helper.assertTrue(Diretor.miragensParaTeste(jogador, "CHAMA_PALIDA") == 0, "com a luz barrada, nenhuma tocha muda de cor");
					if (relogio[0] == 440) {
						helper.assertTrue(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "a luz ainda deveria estar barrada");
						// Mais duas coisas de outras famílias: das últimas cinco, só uma é luz.
						Diretor.registrarFamiliaParaTeste(jogador, "SOM");
						helper.assertTrue(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "luz, luz, corpo, corpo, som: ainda são duas em cinco");
						Diretor.registrarFamiliaParaTeste(jogador, "MARCA");
						helper.assertFalse(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "luz, corpo, corpo, som, marca: a luz tem a vez de novo");
						passo[0] = 2;
						relogio[0] = 0;
					}
				}
				case 2 -> {
					if (Diretor.miragensParaTeste(jogador, "CHAMA_PALIDA") >= 1) {
						helper.assertTrue(level.getBlockState(pes.offset(0, 0, -2)).is(Blocks.TORCH), "a tocha de verdade não muda");
						JogadorDeTeste.remover(jogador);
						passo[0] = 3;
						helper.succeed();
					} else if (relogio[0] > 460) {
						helper.fail("com a vez de volta, uma tocha deveria ter mudado de cor em vinte segundos");
					}
				}
				default -> {
				}
			}
		});
	}

	/**
	 * A tampa do baú (0.9.0-alpha23): um baú que ele pôs lá embaixo levanta a tampa e fecha um segundo depois.
	 * Só para ele: o evento do bloco e os dois sons chegam ao alvo e não ao amigo colado nele. O baú de verdade
	 * não abre, e o que há dentro fica onde estava.
	 */
	@GameTest(maxTicks = 400)
	public void aTampaDoBauLevantaSoParaEle(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -8000);
		ServerLevel level = helper.getLevel();
		abrirNicho(level, pes);
		// O baú fica no fundo do túnel, três blocos à frente dele, com uma coisa dentro.
		BlockPos bau = pes.offset(0, 0, 3);
		level.setBlock(bau, Blocks.CHEST.defaultBlockState(), 3);
		if (level.getBlockEntity(bau) instanceof ChestBlockEntity dentro) {
			dentro.setItem(4, new ItemStack(Items.IRON_INGOT, 7));
		}
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerPlayer amigo = jogadorEm(helper, pes, 1);
		Vec3 centro = Vec3.atCenterOf(bau);
		int[] relogio = {0};
		int[] passo = {0};
		int[] doAlvo = new int[5];
		int[] doAmigo = new int[5];
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 1) {
				return;
			}
			level.getChunkSource().move(jogador);
			level.getChunkSource().move(amigo);
			TestesDePerto.olharParaOSul(jogador);
			TestesDePerto.olharParaOSul(amigo);
			relogio[0]++;
			if (passo[0] == 0) {
				if (relogio[0] > 200) {
					helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
				}
				if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
					JogadorDeTeste.esquecerSons(jogador);
					JogadorDeTeste.esquecerSons(amigo);
					helper.assertTrue(Diretor.testarPerturbacaoPeloNome(jogador, "TAMPA"), "com um baú a três blocos, a tampa deveria levantar");
					helper.assertTrue("TAMPA".equals(Diretor.cavernaParaTeste(jogador)), "o resumo deveria dizer TAMPA");
					passo[0] = 1;
					relogio[0] = 0;
				}
				return;
			}
			somar(doAlvo, JogadorDeTeste.recebidos(jogador));
			somar(doAmigo, JogadorDeTeste.recebidos(amigo));
			helper.assertTrue(ChestBlockEntity.getOpenCount(level, bau) == 0, "o baú de verdade não abre em momento nenhum");
			if (relogio[0] == 8) {
				helper.assertTrue(doAlvo[4] == 1, "a tampa levanta para o alvo: um evento de bloco; chegaram " + doAlvo[4]);
				helper.assertTrue(JogadorDeTeste.sonsVindosDe(jogador, centro, FOLGA, "minecraft:block.chest.open") == 1,
						"com o som de baú abrindo; o alvo ouviu " + JogadorDeTeste.ultimosSons(jogador));
			}
			if (relogio[0] == 40) {
				helper.assertTrue(doAlvo[4] == 2, "um segundo depois a tampa fecha: outro evento de bloco; chegaram " + doAlvo[4]);
				helper.assertTrue(JogadorDeTeste.sonsVindosDe(jogador, centro, FOLGA, "minecraft:block.chest.close") == 1,
						"com o som de baú fechando; o alvo ouviu " + JogadorDeTeste.ultimosSons(jogador));
				helper.assertTrue(doAmigo[4] == 0 && JogadorDeTeste.sonsVindosDe(amigo, centro, 6.0, "minecraft:block.chest.open") == 0
						&& JogadorDeTeste.sonsVindosDe(amigo, centro, 6.0, "minecraft:block.chest.close") == 0,
						"o amigo ao lado não vê nem ouve o baú do outro mexer: " + doAmigo[4] + " evento(s), e ouviu " + JogadorDeTeste.ultimosSons(amigo));
				helper.assertTrue(level.getBlockEntity(bau) instanceof ChestBlockEntity dentro && dentro.getItem(4).is(Items.IRON_INGOT)
						&& dentro.getItem(4).getCount() == 7, "dentro, tudo igual");
				JogadorDeTeste.remover(jogador);
				JogadorDeTeste.remover(amigo);
				passo[0] = 2;
				helper.succeed();
			}
		});
	}

	/**
	 * A passagem fechada (0.9.0-alpha23): ele anda quinze blocos por um túnel; o caminho por onde veio, visto de
	 * longe, aparece fechado com pedra (duas miragens, um bloco de largura por dois de altura). O túnel de
	 * verdade continua aberto. Quando ele volta e chega perto, a pedra já não está: desfaz-se a mais de quatro
	 * blocos e meio, e ele nunca encosta nela.
	 */
	@GameTest(maxTicks = 900)
	public void aPassagemPorOndeEleVeioPareceFechadaDeLonge(GameTestHelper helper) {
		BlockPos pes = montarTunelLongo(helper, -8100);
		ServerLevel level = helper.getLevel();
		// Ele começa doze blocos ao norte do ponto de sempre e anda para o sul até o fundo do túnel.
		Vec3 partida = helper.relativeVec(new Vec3(pes.getX() + 0.5, pes.getY(), pes.getZ() - 11.5));
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, partida.x, partida.y, partida.z);
		Diretor.esquecer(jogador);
		double x = pes.getX() + 0.5;
		double y = pes.getY();
		double[] z = {pes.getZ() - 11.5};
		double fundo = pes.getZ() + 3.5;
		int[] relogio = {0};
		int[] passo = {0};
		int[] blocosTrocados = {0};
		List<BlockPos> parede = new java.util.ArrayList<>();
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 3) {
				return;
			}
			level.getChunkSource().move(jogador);
			jogador.setOnGround(true);
			relogio[0]++;
			blocosTrocados[0] += JogadorDeTeste.recebidos(jogador)[2];
			switch (passo[0]) {
				case 0 -> {
					// Parado até o Diretor ler o lugar; depois anda para o sul, um bloco e meio por segundo.
					if (relogio[0] > 300 && !"SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
						helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
					}
					if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador)) && z[0] < fundo) {
						z[0] = Math.min(fundo, z[0] + 0.075);
					}
					jogador.snapTo(x, y, z[0], 0.0F, 0.0F);
					if (z[0] >= fundo) {
						passo[0] = 1;
						relogio[0] = 0;
					}
				}
				case 1 -> {
					// No fundo, de costas para o caminho. Espera o caminho andado "envelhecer" dez segundos.
					jogador.snapTo(x, y, fundo, 0.0F, 0.0F);
					if (relogio[0] == 1) {
						helper.assertTrue(Diretor.rastroParaTeste(jogador) >= 4, "andando quinze blocos ele deixa caminho; pontos: " + Diretor.rastroParaTeste(jogador));
					}
					if (relogio[0] == 120) {
						blocosTrocados[0] = 0;
						helper.assertTrue(Diretor.testarPerturbacaoPeloNome(jogador, "PASSAGEM_FECHADA"), "o caminho por onde ele veio deveria aparecer fechado");
						parede.addAll(Diretor.posicoesDeMiragemParaTeste(jogador, "PASSAGEM_FECHADA"));
						helper.assertTrue(parede.size() == 2, "um túnel de um bloco por dois fecha com duas pedras; foram " + parede.size());
						for (BlockPos pos : parede) {
							double dist = Math.sqrt(jogador.distanceToSqr(Vec3.atCenterOf(pos)));
							helper.assertTrue(pos.getX() == pes.getX() && dist >= 10.0 && dist <= 26.0, "a parede fica no túnel, de dez a vinte e seis blocos dele; ficou em "
									+ pos.toShortString() + ", a " + dist);
							helper.assertTrue(level.getBlockState(pos).isAir(), "o túnel de verdade continua aberto");
						}
						passo[0] = 2;
						relogio[0] = 0;
					}
				}
				case 2 -> {
					jogador.snapTo(x, y, fundo, 180.0F, 0.0F);
					if (relogio[0] == 20) {
						helper.assertTrue(blocosTrocados[0] == 2, "as duas pedras chegam ao jogo dele como blocos trocados; chegaram " + blocosTrocados[0]);
						helper.assertTrue(Diretor.miragensParaTeste(jogador, "PASSAGEM_FECHADA") == 2, "de longe, a parede fica");
						passo[0] = 3;
						relogio[0] = 0;
					}
				}
				case 3 -> {
					// Ele volta para ver, no passo de quem anda. Antes de chegar, a parede já não está.
					z[0] -= 0.2;
					jogador.snapTo(x, y, z[0], 180.0F, 0.0F);
					double perto = Double.MAX_VALUE;
					for (BlockPos pos : parede) {
						perto = Math.min(perto, Math.sqrt(jogador.distanceToSqr(Vec3.atCenterOf(pos))));
					}
					if (Diretor.miragensParaTeste(jogador, "PASSAGEM_FECHADA") == 0) {
						helper.assertTrue(perto >= 4.5, "a parede se desfaz antes de ele chegar; desfez-se com ele a " + perto + " blocos");
						helper.assertTrue(jogador.getHealth() == 20.0F, "a parede falsa não toca no jogador");
						JogadorDeTeste.remover(jogador);
						passo[0] = 4;
						helper.succeed();
					} else {
						helper.assertTrue(perto > 4.5, "ele não pode chegar a quatro blocos e meio de uma parede que não existe; chegou a " + perto);
					}
				}
				default -> {
				}
			}
		});
	}

	/** Fora do subsolo a regra não vale: na superfície a mistura do mod fica como era. */
	@GameTest(maxTicks = 200)
	public void naSuperficieARegraDasFamiliasNaoVale(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -7800, 0);
		Diretor.esquecer(jogador);
		helper.runAfterDelay(60, () -> {
			helper.assertFalse("SUBSOLO".equals(Diretor.contextoParaTeste(jogador)), "o gramado não é subsolo");
			for (int i = 0; i < 5; i++) {
				Diretor.registrarFamiliaParaTeste(jogador, "LUZ");
			}
			helper.assertFalse(Diretor.familiaBarradaParaTeste(jogador, "LUZ"), "na superfície nenhuma família é barrada");
			helper.assertTrue(Diretor.subsoloSemFisicoParaTeste(jogador) == 0, "o relógio do subsolo só anda debaixo da terra");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Um morcego do mod que ficou salvo no mundo (o jogo caiu com eles voando) volta com a etiqueta e sem ninguém
	 * cuidando dele: é tirado assim que é carregado. Um morcego do jogo, sem etiqueta, fica.
	 */
	@GameTest(maxTicks = 100)
	public void oMorcegoQueSobrouETirado(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 onde = helper.absoluteVec(new Vec3(1.5, 3.0, 1.5));
		Bat sobra = EntityTypes.BAT.create(level, EntitySpawnReason.COMMAND);
		Bat comum = EntityTypes.BAT.create(level, EntitySpawnReason.COMMAND);
		sobra.addTag(ETIQUETA);
		sobra.setPersistenceRequired();
		comum.setPersistenceRequired();
		sobra.snapTo(onde.x, onde.y, onde.z);
		comum.snapTo(onde.x + 1.0, onde.y, onde.z);
		level.addFreshEntity(sobra);
		level.addFreshEntity(comum);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(sobra.isRemoved(), "o morcego etiquetado que ninguém soltou deveria ter sido tirado");
			helper.assertFalse(comum.isRemoved(), "morcego do jogo não é da conta do mod");
			comum.discard();
			helper.succeed();
		});
	}
}
