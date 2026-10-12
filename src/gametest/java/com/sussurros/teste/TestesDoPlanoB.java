package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.PrimeiroContato;

/**
 * O plano B (0.9.0-alpha20): uma construção que não acha onde pôr o corpo não acaba em silêncio.
 *
 * Cada teste monta o lugar em que a procura falha de verdade (o mundo de teste é um gramado, e ali ela acha
 * lugar): uma cela de pedra fechada, onde não há lugar nenhum, e um corredor sem saída em que o único espaço
 * fica à frente do jogador, onde todo lugar cai por estar na tela. Conferem o que chega ao jogador (sons,
 * partículas, a miragem) e que quem está ao lado não recebe nada. Se o sinal convence, só jogando.
 */
public class TestesDoPlanoB {
	/** Um bloco maciço de pedra em volta dos pés do jogador, com teto: lá dentro não chega luz do céu. */
	private static BlockPos macico(GameTestHelper helper, int dz, int paraOSul) {
		BlockPos pes = JogadorDeTeste.chao(helper, 4, dz);
		ServerLevel level = helper.getLevel();
		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= paraOSul + 3; z++) {
				for (int y = 0; y <= 4; y++) {
					level.setBlock(pes.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
				}
			}
		}
		return pes;
	}

	/** Uma cela de três por três, com dois blocos de altura, fechada por todos os lados: não cabe ninguém. */
	private static BlockPos montarCela(GameTestHelper helper, int dz) {
		BlockPos pes = macico(helper, dz, 2);
		ServerLevel level = helper.getLevel();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 0; y <= 1; y++) {
					level.setBlock(pes.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		return pes;
	}

	/**
	 * Um corredor sem saída, de um bloco de largura e dois de altura, que só vai para a frente do jogador (o sul):
	 * doze blocos. Atrás dele, pedra. É o bolsão em que o único espaço fica para onde ele olha.
	 */
	private static BlockPos montarCorredor(GameTestHelper helper, int dz) {
		BlockPos pes = macico(helper, dz, 12);
		ServerLevel level = helper.getLevel();
		for (int z = 0; z <= 12; z++) {
			for (int y = 0; y <= 1; y++) {
				level.setBlock(pes.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		return pes;
	}

	private static ServerPlayer jogadorEm(GameTestHelper helper, BlockPos pes, double dx) {
		Vec3 relativo = helper.relativeVec(new Vec3(pes.getX() + 0.5 + dx, pes.getY(), pes.getZ() + 0.5));
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, relativo.x, relativo.y, relativo.z);
		Diretor.esquecer(jogador);
		return jogador;
	}

	private static void cadaTick(GameTestHelper helper, Runnable acao) {
		helper.runAfterDelay(1, () -> {
			acao.run();
			cadaTick(helper, acao);
		});
	}

	private static void olharParaOSul(ServerPlayer jogador) {
		jogador.lookAt(EntityAnchorArgument.Anchor.EYES, jogador.getEyePosition().add(0, 0, 8));
	}

	private static void somar(int[] total, int[] parcela) {
		for (int i = 0; i < total.length; i++) {
			total[i] += parcela[i];
		}
	}

	/**
	 * A sequência de ameaça numa cela fechada: não há onde pôr ninguém, ela espera um minuto e acaba em
	 * SEM_LUGAR. Até a alpha19 acabava em silêncio. Agora dois passos se afastam, do outro lado da parede: sons,
	 * e só para o alvo. Ninguém aparece, e o fim continua sendo SEM_LUGAR (a falta fica no relatório).
	 */
	@GameTest(maxTicks = 2200)
	public void naCelaFechadaAAmeacaAcabaEmPassos(GameTestHelper helper) {
		BlockPos pes = montarCela(helper, -6400);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerLevel level = helper.getLevel();
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		int[] relogio = {0};
		int[] fimHa = {-1};
		int[] recebido = new int[3];
		boolean[] comecou = new boolean[1];
		cadaTick(helper, () -> {
			if (fimHa[0] > 40) {
				return;
			}
			level.getChunkSource().move(jogador);
			olharParaOSul(jogador);
			relogio[0]++;
			if (!comecou[0]) {
				if (relogio[0] > 200) {
					helper.fail("o Diretor deveria ter lido a cela como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
				}
				if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
					JogadorDeTeste.recebidos(jogador);
					Diretor.ameacaParaTeste(jogador);
					comecou[0] = true;
				}
				return;
			}
			somar(recebido, JogadorDeTeste.recebidos(jogador));
			helper.assertTrue(Diretor.criatura(jogador) == null, "na cela fechada não cabe ninguém");
			String fim = Diretor.fimDaAmeacaParaTeste(jogador);
			if ("-".equals(fim)) {
				helper.assertTrue("-".equals(Diretor.planoBParaTeste(jogador)), "o plano B só sai quando a sequência acaba");
				return;
			}
			// Acabou: espera dois segundos (o segundo passo vem nove ticks depois do primeiro) e confere.
			if (++fimHa[0] == 40) {
				helper.assertTrue("SEM_LUGAR".equals(fim), "na cela fechada a sequência acaba sem lugar; acabou em " + fim);
				helper.assertTrue("PASSOS".equals(Diretor.planoBParaTeste(jogador)),
						"sem lugar à vista e sem tocha, o plano B são os passos; saiu " + Diretor.planoBParaTeste(jogador));
				helper.assertTrue(recebido[0] >= 2, "o alvo deveria ter ouvido os dois passos; ouviu " + recebido[0] + " som(ns)");
				helper.assertTrue(recebido[1] == 0, "sem lugar à vista não cai pó nenhum; partículas: " + recebido[1]);
				helper.assertTrue(jogador.getHealth() == 20.0F, "o plano B não toca no jogador");
				JogadorDeTeste.remover(jogador);
				fimHa[0] = 99;
				helper.succeed();
			}
		});
	}

	/**
	 * O bolsão em que o único espaço fica à frente do jogador (o limite que a alpha19 deixou conhecido): todo
	 * lugar de 8 a 14 blocos está na tela, e ele nunca nasce na tela. A sequência acaba em SEM_LUGAR, e é ali, à
	 * vista dele, que cai pó do teto. Só para ele: o amigo parado ao lado não recebe partícula nem som.
	 */
	@GameTest(maxTicks = 2200)
	public void noCorredorSemSaidaCaiPoAVistaDele(GameTestHelper helper) {
		BlockPos pes = montarCorredor(helper, -6500);
		ServerLevel level = helper.getLevel();
		// O amigo fica numa reentrância ao lado, colado nele.
		level.setBlock(pes.offset(1, 0, 0), Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(pes.offset(1, 1, 0), Blocks.AIR.defaultBlockState(), 3);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerPlayer amigo = jogadorEm(helper, pes, 1);
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		int[] relogio = {0};
		int[] fimHa = {-1};
		int[] doAlvo = new int[3];
		int[] doAmigo = new int[3];
		// De onde vem o toque do pó: o teto do lugar mais perto que a procura recusou por estar na tela, oito
		// blocos à frente. O amigo também ouve o mundo (um bicho andando em cima da pedra, a assombração dele):
		// o que se confere é se AQUELE som chegou a ele.
		Vec3 teto = new Vec3(pes.getX() + 0.5, pes.getY() + 2, pes.getZ() + 8.5);
		boolean[] comecou = new boolean[1];
		cadaTick(helper, () -> {
			if (fimHa[0] > 40) {
				return;
			}
			level.getChunkSource().move(jogador);
			level.getChunkSource().move(amigo);
			olharParaOSul(jogador);
			olharParaOSul(amigo);
			relogio[0]++;
			if (!comecou[0]) {
				if (relogio[0] > 200) {
					helper.fail("o Diretor deveria ter lido o corredor como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
				}
				if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
					JogadorDeTeste.recebidos(jogador);
					JogadorDeTeste.recebidos(amigo);
					Diretor.ameacaParaTeste(jogador);
					comecou[0] = true;
				}
				return;
			}
			somar(doAlvo, JogadorDeTeste.recebidos(jogador));
			somar(doAmigo, JogadorDeTeste.recebidos(amigo));
			helper.assertTrue(Diretor.criatura(jogador) == null, "olhando para o único lado com espaço, ninguém nasce (nunca na tela)");
			String fim = Diretor.fimDaAmeacaParaTeste(jogador);
			if ("-".equals(fim)) {
				return;
			}
			if (++fimHa[0] == 40) {
				helper.assertTrue("SEM_LUGAR".equals(fim), "a sequência acaba sem lugar; acabou em " + fim);
				helper.assertTrue("PO".equals(Diretor.planoBParaTeste(jogador)),
						"com o lugar à vista e teto em cima, o plano B é o pó; saiu " + Diretor.planoBParaTeste(jogador));
				helper.assertTrue(doAlvo[1] >= 2, "o alvo deveria ter visto o pó cair; partículas: " + doAlvo[1]);
				helper.assertTrue(JogadorDeTeste.sonsVindosDe(jogador, teto, 1.0) >= 1,
						"o pó vem com um toque no teto, oito blocos à frente; o alvo ouviu " + JogadorDeTeste.ultimosSons(jogador));
				helper.assertTrue(JogadorDeTeste.sonsVindosDe(amigo, teto, 1.0) == 0 && doAmigo[1] == 0,
						"o amigo ao lado não vê nem ouve a assombração do outro: " + doAmigo[1] + " partícula(s), e ouviu "
								+ JogadorDeTeste.ultimosSons(amigo));
				JogadorDeTeste.remover(jogador);
				JogadorDeTeste.remover(amigo);
				fimHa[0] = 99;
				helper.succeed();
			}
		});
	}

	/**
	 * O mesmo corredor, com uma tocha lá na frente: da segunda vez seguida o sinal é outro (nunca o mesmo duas
	 * vezes, havendo escolha). A tocha apaga só para ele, por miragem: o bloco de verdade continua no lugar.
	 */
	@GameTest(maxTicks = 400)
	public void doisSinaisSeguidosNaoSaoOMesmo(GameTestHelper helper) {
		BlockPos pes = montarCorredor(helper, -6600);
		ServerLevel level = helper.getLevel();
		BlockPos tocha = pes.offset(0, 0, 11);
		level.setBlock(tocha, Blocks.TORCH.defaultBlockState(), 3);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		int[] relogio = {0};
		int[] passo = {0};
		cadaTick(helper, () -> {
			level.getChunkSource().move(jogador);
			olharParaOSul(jogador);
			relogio[0]++;
			if (passo[0] == 0 && relogio[0] == 40) {
				JogadorDeTeste.recebidos(jogador);
				String primeiro = Diretor.testarPlanoB(jogador);
				helper.assertTrue("PO".equals(primeiro), "o primeiro sinal, à vista e com teto, é o pó; saiu " + primeiro);
				String segundo = Diretor.testarPlanoB(jogador);
				helper.assertTrue("TOCHA".equals(segundo), "o segundo sinal seguido é a tocha; saiu " + segundo);
				helper.assertTrue(Diretor.miragensParaTeste(jogador, "PLANO_B") == 1, "a tocha apagada é uma miragem");
				helper.assertTrue(level.getBlockState(tocha).is(Blocks.TORCH), "a tocha de verdade continua no lugar");
				passo[0] = 1;
			}
			// Os pacotes saem na vez da conexão: um segundo depois, a miragem e o pó já chegaram ao alvo.
			if (passo[0] == 1 && relogio[0] == 40 + 20) {
				int[] chegou = JogadorDeTeste.recebidos(jogador);
				helper.assertTrue(chegou[2] >= 1, "a miragem chega ao alvo como um bloco trocado; blocos: " + chegou[2]);
				helper.assertTrue(chegou[1] >= 1, "o pó chega ao alvo como partículas; partículas: " + chegou[1]);
			}
			// De quatro a sete segundos depois a tocha volta sozinha.
			if (passo[0] == 1 && relogio[0] == 40 + 170) {
				helper.assertTrue(Diretor.miragensParaTeste(jogador, "PLANO_B") == 0, "a tocha deveria ter voltado");
				JogadorDeTeste.remover(jogador);
				passo[0] = 2;
				helper.succeed();
			}
		});
	}

	/**
	 * Uma cena que não acha onde pô-lo também não acaba em silêncio: a cena do túnel, na cela fechada, chega à
	 * presença, espera meio minuto e termina com o plano B.
	 */
	@GameTest(maxTicks = 2400)
	public void aCenaQueNaoAchaLugarTambemTemSinal(GameTestHelper helper) {
		BlockPos pes = montarCela(helper, -6700);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerLevel level = helper.getLevel();
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		int[] relogio = {0};
		boolean[] comecou = new boolean[1];
		boolean[] acabou = new boolean[1];
		cadaTick(helper, () -> {
			if (acabou[0]) {
				return;
			}
			level.getChunkSource().move(jogador);
			olharParaOSul(jogador);
			relogio[0]++;
			if (!comecou[0]) {
				if (relogio[0] >= 40) {
					Diretor.testarCenaTunel(jogador);
					comecou[0] = true;
				}
				return;
			}
			helper.assertTrue(Diretor.criatura(jogador) == null, "na cela fechada não cabe ninguém");
			if (!"-".equals(Diretor.planoBParaTeste(jogador))) {
				acabou[0] = true;
				helper.assertTrue("PASSOS".equals(Diretor.planoBParaTeste(jogador)), "na cela, o sinal da cena são os passos; saiu "
						+ Diretor.planoBParaTeste(jogador));
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * O Véu numa cela fechada: sem porta, sem vidro, e o vulto não cabe em nenhuma tentativa. Até a alpha19 ele
	 * fechava sem nada dentro. Agora o sinal acontece com o Véu ainda aberto.
	 */
	@GameTest(maxTicks = 1700)
	public void oVeuEmQueOVultoNaoCabeTambemTemSinal(GameTestHelper helper) {
		BlockPos pes = montarCela(helper, -6800);
		ServerPlayer jogador = jogadorEm(helper, pes, 0);
		ServerLevel level = helper.getLevel();
		Diretor.definirFase(jogador, 3);
		int[] relogio = {0};
		boolean[] abriu = new boolean[1];
		boolean[] acabou = new boolean[1];
		cadaTick(helper, () -> {
			if (acabou[0]) {
				return;
			}
			level.getChunkSource().move(jogador);
			olharParaOSul(jogador);
			relogio[0]++;
			if (!abriu[0]) {
				if (relogio[0] >= 40) {
					String falha = Diretor.abrirVeuParaTeste(jogador);
					helper.assertTrue(falha == null, "o Véu deveria abrir: " + falha);
					abriu[0] = true;
				}
				return;
			}
			helper.assertTrue(Diretor.criatura(jogador) == null, "na cela fechada o vulto não cabe");
			if (!"-".equals(Diretor.planoBParaTeste(jogador))) {
				acabou[0] = true;
				helper.assertTrue(Diretor.veuAberto(jogador), "o sinal vem com o Véu ainda aberto");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			} else if (relogio[0] > 60 && !Diretor.veuAberto(jogador)) {
				helper.fail("o Véu fechou sem vulto e sem sinal nenhum");
			}
		});
	}
}
