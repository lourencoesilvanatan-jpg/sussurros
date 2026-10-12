package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.assombracao.PrimeiroContato;
import com.sussurros.entidade.HospedeEntity;

/**
 * Ele aparece em lugar apertado, e o Véu nunca vem vazio (0.9.0-alpha19).
 *
 * Até a alpha18 ele só cabia, fora da caça, onde havia três blocos de altura e de 14 a 38 blocos de distância:
 * numa mina não havia lugar nenhum. Estes testes montam a mina (o mundo de teste é um gramado raspado, e teste
 * em gramado não prova nada que dependa de terreno): um túnel de pedra de um bloco de largura e dois de altura,
 * com uma curva, fechado por todos os lados.
 *
 * Conferem a regra contra um jogador de mentira. A pose abaixada, parada, quem confere são as fotos.
 */
public class TestesDePerto {
	/** A curva do túnel fica dez blocos atrás do jogador. */
	private static final int CURVA = -10;

	/**
	 * O túnel, visto de cima (o jogador J olha para o sul, para baixo no desenho):
	 *
	 *   T C - - - - - - - -      z = -10: a curva C, o trecho que sai para o leste e, a oeste, o nicho da tocha T
	 *     |
	 *     |                      dez blocos de túnel atrás do jogador
	 *     J                      z = 0
	 *     |
	 *     |                      z = 3: o fundo
	 *
	 * Tudo dentro de um bloco maciço de pedra, com três blocos de teto: lá dentro não chega luz do céu.
	 * Devolve onde ficam os pés do jogador.
	 */
	static BlockPos montarTunel(GameTestHelper helper, int dz) {
		BlockPos pes = JogadorDeTeste.chao(helper, 4, dz);
		ServerLevel level = helper.getLevel();
		for (int x = -3; x <= 11; x++) {
			for (int z = CURVA - 3; z <= 6; z++) {
				for (int y = 0; y <= 4; y++) {
					level.setBlock(pes.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
				}
			}
		}
		for (int y = 0; y <= 1; y++) {
			for (int z = CURVA; z <= 3; z++) {
				level.setBlock(pes.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);
			}
			for (int x = 0; x <= 8; x++) {
				level.setBlock(pes.offset(x, y, CURVA), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		// Uma tocha num nicho da parede, na altura da cabeça, junto da curva: a curva fica iluminada e o jogador,
		// a dez blocos, continua no escuro.
		level.setBlock(pes.offset(-1, 1, CURVA), Blocks.TORCH.defaultBlockState(), 3);
		return pes;
	}

	static ServerPlayer jogadorNoTunel(GameTestHelper helper, BlockPos pes) {
		Vec3 relativo = helper.relativeVec(new Vec3(pes.getX() + 0.5, pes.getY(), pes.getZ() + 0.5));
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, relativo.x, relativo.y, relativo.z);
		Diretor.esquecer(jogador);
		return jogador;
	}

	/** Roda a ação em todo tick, agendando só o tick seguinte de cada vez (ver TestesDeSessao). */
	static void cadaTick(GameTestHelper helper, Runnable acao) {
		helper.runAfterDelay(1, () -> {
			acao.run();
			cadaTick(helper, acao);
		});
	}

	static void olharParaOSul(ServerPlayer jogador) {
		jogador.lookAt(EntityAnchorArgument.Anchor.EYES, jogador.getEyePosition().add(0, 0, 8));
	}

	static boolean naCurva(HospedeEntity h, BlockPos pes) {
		return h.blockPosition().equals(pes.offset(0, 0, CURVA));
	}

	/** Uma semente com que o Véu sorteia "sem vulto": é o caso que a regra nova tem de cobrir. */
	private static long sementeSemVulto() {
		for (long semente = 1; ; semente++) {
			RandomSource sorte = RandomSource.create(semente);
			sorte.nextInt(31);
			if (!sorte.nextBoolean()) {
				return semente;
			}
		}
	}

	/**
	 * Num túnel de dois blocos de altura ele aparece: de 8 a 14 blocos, na curva (iluminada, e de onde um passo
	 * o tiraria da vista), abaixado. Olhado, some como sempre, sem sair andando.
	 */
	@GameTest(maxTicks = 600)
	public void noTunelEleApareceDePertoEAbaixado(GameTestHelper helper) {
		BlockPos pes = montarTunel(helper, -5700);
		ServerPlayer jogador = jogadorNoTunel(helper, pes);
		ServerLevel level = helper.getLevel();
		int[] passo = {0};
		int[] relogio = {0};
		cadaTick(helper, () -> {
			level.getChunkSource().move(jogador);
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					// Espera a luz assentar e o Diretor perceber que ali é subsolo.
					olharParaOSul(jogador);
					if (++relogio[0] > 200) {
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
					helper.assertTrue(h != null, "ele deveria ter aparecido");
					double dist = h.distanceTo(jogador);
					helper.assertTrue(dist >= 8.0 && dist <= 14.0, "de perto é de 8 a 14 blocos; nasceu a " + dist);
					helper.assertTrue(naCurva(h, pes), "ele deveria nascer na curva, em vez de " + h.blockPosition().toShortString()
							+ " (a curva é " + pes.offset(0, 0, CURVA).toShortString() + ")");
					helper.assertTrue(h.isAbaixado() && h.getBbHeight() < 2.0F, "sob um teto de dois blocos ele nasce abaixado; altura " + h.getBbHeight());
					helper.assertTrue(h.ehDePerto() && h.getModo() == HospedeEntity.Modo.OBSERVAR, "deveria ser a aparição de perto, parada");
					helper.assertTrue(level.noCollision(h), "ele não pode nascer dentro da pedra");
					// Dez ticks sem ser olhado: continua lá.
					if (++relogio[0] >= 10) {
						passo[0] = 2;
						relogio[0] = 0;
					}
				}
				case 2 -> {
					// O jogador vira e olha: em menos de um segundo e meio ele não está mais lá, e não saiu andando.
					if (h == null) {
						helper.assertTrue(jogador.getHealth() == 20.0F, "ele não toca no jogador");
						JogadorDeTeste.remover(jogador);
						passo[0] = 3;
						helper.succeed();
						return;
					}
					helper.assertFalse(h.estaIndoEmbora(), "de perto ele nunca vai embora andando");
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
					if (++relogio[0] > 30) {
						helper.fail("olhado, de perto, ele deveria ter sumido");
					}
				}
				default -> {
				}
			}
		});
	}

	/**
	 * A sequência de ameaça dentro do túnel. Até a alpha18 ela esperava um minuto por um lugar e acabava sem
	 * ninguém ("SEM_LUGAR"): foi o que aconteceu com o dono no fundo de uma caverna, em 11/10/2026. Agora a
	 * presença que espreita acha a curva, e o golpe também.
	 */
	@GameTest(maxTicks = 3600)
	public void aAmeacaNoTunelNaoAcabaSemNinguem(GameTestHelper helper) {
		BlockPos pes = montarTunel(helper, -5800);
		ServerPlayer jogador = jogadorNoTunel(helper, pes);
		ServerLevel level = helper.getLevel();
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		int[] relogio = {0};
		int[] vistoHa = {0};
		int[] aparicoes = {0};
		boolean[] comecou = new boolean[1];
		boolean[] haviaCriatura = new boolean[1];
		boolean[] acabou = new boolean[1];
		cadaTick(helper, () -> {
			if (acabou[0]) {
				return;
			}
			level.getChunkSource().move(jogador);
			relogio[0]++;
			if (!comecou[0]) {
				olharParaOSul(jogador);
				if (relogio[0] > 200) {
					helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
				}
				if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
					Diretor.ameacaParaTeste(jogador);
					comecou[0] = true;
				}
				return;
			}
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				if (!haviaCriatura[0]) {
					aparicoes[0]++;
					vistoHa[0] = 0;
					helper.assertTrue(naCurva(h, pes) && h.isAbaixado() && h.ehDePerto(),
							"na ameaça ele deveria nascer na curva do túnel, abaixado; nasceu em " + h.blockPosition().toShortString()
									+ " modo=" + h.getModo() + " abaixado=" + h.isAbaixado());
				}
				// Um segundo sem olhar, e o jogador vira para ele: de perto, some como sempre.
				if (++vistoHa[0] > 20) {
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
				}
				helper.assertFalse(h.estaIndoEmbora(), "de perto ele nunca vai embora andando");
			} else {
				// Sem ninguém à vista, o jogador volta a olhar para o fundo do túnel: a curva fica às costas dele.
				olharParaOSul(jogador);
			}
			haviaCriatura[0] = h != null;
			String fim = Diretor.fimDaAmeacaParaTeste(jogador);
			if (!"-".equals(fim)) {
				acabou[0] = true;
				helper.assertTrue("GOLPE_CONCLUIDO".equals(fim), "a sequência deveria ir até o golpe; acabou em " + fim);
				helper.assertTrue(aparicoes[0] >= 2, "deveriam ter sido duas aparições (a espreita e o golpe); foram " + aparicoes[0]);
				helper.assertTrue(jogador.getHealth() == 20.0F, "ele não toca no jogador");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * O Véu num campo, sem porta e sem vidro por perto, com "não" no sorteio do vulto: até a alpha18 não havia
	 * nada dentro dele. Agora o vulto vem.
	 */
	@GameTest(maxTicks = 1500)
	public void oVeuSemPortaNemVidroTrazOVulto(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -5900, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		ServerLevel level = helper.getLevel();
		boolean[] abriu = new boolean[1];
		boolean[] veio = new boolean[1];
		helper.runAfterDelay(5, () -> {
			jogador.getRandom().setSeed(sementeSemVulto());
			String falha = Diretor.abrirVeuParaTeste(jogador);
			helper.assertTrue(falha == null, "o Véu deveria abrir: " + falha);
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "VEU") == 0, "este teste é o do Véu sem porta e sem vidro");
			abriu[0] = true;
		});
		cadaTick(helper, () -> {
			level.getChunkSource().move(jogador);
			if (abriu[0] && !veio[0] && Diretor.veuAberto(jogador) && Diretor.criatura(jogador) != null) {
				veio[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(abriu[0], "o Véu ainda não abriu");
			helper.assertTrue(veio[0], "sem porta e sem vidro, o vulto do Véu deveria ter vindo");
			helper.assertFalse(Diretor.veuAberto(jogador), "o Véu ainda está aberto");
			helper.assertTrue(Diretor.criatura(jogador) == null, "o vulto vai embora com o Véu");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** O mesmo dentro do túnel: o vulto do Véu pedia altura de pé e de 12 a 17 blocos, e ali não cabia. */
	@GameTest(maxTicks = 1700)
	public void noTunelOVeuTambemTrazOVulto(GameTestHelper helper) {
		BlockPos pes = montarTunel(helper, -6000);
		ServerPlayer jogador = jogadorNoTunel(helper, pes);
		ServerLevel level = helper.getLevel();
		Diretor.definirFase(jogador, 3);
		int[] relogio = {0};
		boolean[] abriu = new boolean[1];
		boolean[] veio = new boolean[1];
		cadaTick(helper, () -> {
			level.getChunkSource().move(jogador);
			olharParaOSul(jogador);
			relogio[0]++;
			if (!abriu[0]) {
				if (relogio[0] > 200) {
					helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
				}
				if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
					jogador.getRandom().setSeed(sementeSemVulto());
					String falha = Diretor.abrirVeuParaTeste(jogador);
					helper.assertTrue(falha == null, "o Véu deveria abrir: " + falha);
					helper.assertTrue(Diretor.miragensParaTeste(jogador, "VEU") == 0, "no túnel não há porta nem vidro");
					abriu[0] = true;
				}
				return;
			}
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null && !veio[0]) {
				helper.assertTrue(Diretor.veuAberto(jogador), "o vulto vem com o Véu aberto");
				helper.assertTrue(naCurva(h, pes) && h.isAbaixado(), "o vulto do Véu deveria nascer na curva, abaixado; nasceu em "
						+ h.blockPosition().toShortString() + " abaixado=" + h.isAbaixado());
				veio[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(veio[0], "no túnel, o vulto do Véu deveria ter vindo");
			helper.assertFalse(Diretor.veuAberto(jogador), "o Véu ainda está aberto");
			helper.assertTrue(Diretor.criatura(jogador) == null, "o vulto vai embora com o Véu");
			JogadorDeTeste.remover(jogador);
		});
	}

	// =====================================================================
	// No terreno de verdade
	// =====================================================================

	/** Ele cabe aqui abaixado: chão firme e dois blocos livres (a mesma régua da procura). */
	private static boolean cabe(ServerLevel level, BlockPos pos) {
		return Diretor.livre(level, pos) && Diretor.livre(level, pos.above())
				&& !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
	}

	/** O lugar mais próximo de "alvo" onde alguém fica de pé, a até "raio" blocos. Com "escuro", só onde a luz é pouca. */
	private static BlockPos emPe(ServerLevel level, BlockPos alvo, int raio, boolean escuro) {
		BlockPos melhor = null;
		for (BlockPos pos : BlockPos.betweenClosed(alvo.offset(-raio, -4, -raio), alvo.offset(raio, 4, raio))) {
			if (cabe(level, pos) && (!escuro || level.getBrightness(LightLayer.BLOCK, pos) <= 3)
					&& (melhor == null || pos.distSqr(alvo) < melhor.distSqr(alvo))) {
				melhor = pos.immutable();
			}
		}
		return melhor;
	}

	/** Vira o rosto do jogador para um rumo (0 é o sul, 90 o oeste), sem tirá-lo do lugar. */
	private static void olharNoRumo(ServerPlayer jogador, float graus) {
		double a = Math.toRadians(graus);
		jogador.lookAt(EntityAnchorArgument.Anchor.EYES, jogador.getEyePosition().add(-Math.sin(a) * 8, 0, Math.cos(a) * 8));
	}

	private static void por(ServerPlayer jogador, BlockPos onde, float yaw) {
		jogador.teleportTo(jogador.level(), onde.getX() + 0.5, onde.getY(), onde.getZ() + 0.5, java.util.Set.of(), yaw, 0.0F, false);
	}

	/**
	 * A caverna em que a sequência de ameaça do dono acabou sem ninguém, em 11/10/2026 (um salão de uns vinte
	 * blocos, de teto baixo, com as tochas dele). De dois lugares perto dessas tochas, olhando para os quatro
	 * lados, a procura de lugar apertado acha onde pô-lo em sete das oito vezes: no salão, de pé, entre dezenas
	 * de lugares; no bolsão ao lado, abaixado, no único lugar que há. A oitava é esse bolsão com o jogador virado
	 * justamente para esse lugar: ele nunca nasce na tela, e ali não aparece (o log diz "tela=19 semVisao=2").
	 */
	@GameTest(maxTicks = 300)
	public void naCavernaDeVerdadeEleAchaLugar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = JogadorDeTeste.chao(helper, 4, -6200);
		Terreno.Montado caverna = Terreno.montar(level, "caverna", base.offset(-23, 0, -21));
		BlockPos[] lugares = {emPe(level, caverna.em(11, 8, 19), 5, false), emPe(level, caverna.em(34, 5, 23), 5, false)};
		helper.assertTrue(lugares[0] != null && lugares[1] != null, "a caverna montada deveria ter chão perto das duas tochas");
		ServerPlayer jogador = jogadorNoTunel(helper, lugares[0]);
		helper.runAfterDelay(40, () -> {
			int achou = 0;
			StringBuilder relato = new StringBuilder();
			for (BlockPos lugar : lugares) {
				for (float yaw : new float[] {0.0F, 90.0F, 180.0F, 270.0F}) {
					por(jogador, lugar, yaw);
					String achado = Diretor.dePertoParaTeste(jogador, false);
					HospedeEntity h = Diretor.criatura(jogador);
					relato.append(String.format(java.util.Locale.ROOT, " [%s yaw=%.0f: %s]", lugar.subtract(caverna.canto()).toShortString(), yaw, achado));
					if (h != null) {
						achou++;
						double dist = h.distanceTo(jogador);
						helper.assertTrue(dist >= 8.0 && dist <= 14.0 && level.noCollision(h),
								"de perto é de 8 a 14 blocos e fora da pedra; nasceu a " + dist + " em " + h.blockPosition().toShortString());
						h.sumir(level, false, "FIM_DO_TESTE");
					}
				}
			}
			Sussurros.LOGGER.info("[caverna de verdade] achou lugar em {} de 8:{}", achou, relato);
			helper.assertTrue(achou >= 7, "na caverna do dono a procura de perto acha lugar em sete das oito vezes; achou " + achou + ":" + relato);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * A sequência de ameaça inteira, na mesma caverna: a que acabou em SEM_LUGAR depois de um minuto de procura.
	 * Agora alguém aparece. (O golpe só vem no escuro: o jogador fica num canto sem tocha.)
	 *
	 * O jogador olha em volta, um quarto de volta a cada dois segundos, como quem está numa caverna. Parado e de
	 * rosto fixo, neste canto, a sequência acaba sem ninguém mesmo com a procura nova: os cinco lugares de 8 a 14
	 * blocos ficam todos à frente dele, e ele nunca nasce na tela (o log disse "naFaixa=305 tela=305").
	 */
	@GameTest(maxTicks = 5200)
	public void aAmeacaNaCavernaDeVerdadeNaoAcabaSemNinguem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = JogadorDeTeste.chao(helper, 4, -6300);
		Terreno.Montado caverna = Terreno.montar(level, "caverna", base.offset(-23, 0, -21));
		BlockPos entrada = emPe(level, caverna.em(11, 8, 19), 5, false);
		helper.assertTrue(entrada != null, "a caverna montada deveria ter chão perto da tocha");
		ServerPlayer jogador = jogadorNoTunel(helper, entrada);
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		int[] relogio = {0};
		int[] vistoHa = {0};
		int[] aparicoes = {0};
		boolean[] comecou = new boolean[1];
		boolean[] haviaCriatura = new boolean[1];
		boolean[] acabou = new boolean[1];
		cadaTick(helper, () -> {
			if (acabou[0]) {
				return;
			}
			level.getChunkSource().move(jogador);
			relogio[0]++;
			if (relogio[0] == 30) {
				// A luz das tochas já assentou: o jogador vai para o lugar escuro mais perto do meio do salão, se
				// houver um (com ele ao lado de uma tocha o golpe não vem, e a sequência acaba em GOLPE_SEM_MOMENTO).
				BlockPos escuro = emPe(level, caverna.em(22, 7, 24), 18, true);
				Sussurros.LOGGER.info("[ameaça na caverna de verdade] lugar escuro: {}", escuro == null ? "nenhum" : escuro.subtract(caverna.canto()).toShortString());
				if (escuro != null) {
					por(jogador, escuro, 0.0F);
				}
			}
			if (!comecou[0]) {
				if (relogio[0] > 240) {
					helper.fail("o Diretor deveria ter lido a caverna como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
				}
				if (relogio[0] >= 70 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
					Diretor.ameacaParaTeste(jogador);
					comecou[0] = true;
				}
				return;
			}
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				olharNoRumo(jogador, 90.0F * (relogio[0] / 40 % 4));
			}
			if (h != null) {
				if (!haviaCriatura[0]) {
					aparicoes[0]++;
					vistoHa[0] = 0;
					Sussurros.LOGGER.info("[ameaça na caverna de verdade] apareceu a {} blocos, modo={} dePerto={} abaixado={}",
							String.format(java.util.Locale.ROOT, "%.1f", h.distanceTo(jogador)), h.getModo(), h.ehDePerto(), h.isAbaixado());
					helper.assertTrue(level.noCollision(h), "ele não pode nascer dentro da pedra: " + h.blockPosition().toShortString());
				}
				// Um segundo sem olhar, e o jogador vira para ele.
				if (++vistoHa[0] > 20) {
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
				}
			}
			haviaCriatura[0] = h != null;
			String fim = Diretor.fimDaAmeacaParaTeste(jogador);
			if (!"-".equals(fim)) {
				acabou[0] = true;
				helper.assertTrue("GOLPE_CONCLUIDO".equals(fim) || "GOLPE_SEM_MOMENTO".equals(fim),
						"na caverna do dono a sequência não pode acabar sem ninguém; acabou em " + fim);
				helper.assertTrue(aparicoes[0] >= 1, "alguém deveria ter aparecido; foram " + aparicoes[0] + " aparições");
				helper.assertTrue(jogador.getHealth() == 20.0F, "ele não toca no jogador");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * A céu aberto ele não vem de perto: ali, a poucos blocos, ele é um boneco parado. A procura de lugar
	 * apertado só aceita lugar fechado.
	 */
	@GameTest(maxTicks = 200)
	public void aCeuAbertoEleNaoVemDePerto(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -6100, 0);
		Diretor.esquecer(jogador);
		helper.runAfterDelay(20, () -> {
			String achado = Diretor.dePertoParaTeste(jogador, false);
			helper.assertTrue(achado == null, "num gramado aberto não há lugar apertado; achou " + achado);
			helper.assertTrue(Diretor.criatura(jogador) == null, "ninguém deveria ter aparecido");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
