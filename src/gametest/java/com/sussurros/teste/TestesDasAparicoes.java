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
import com.sussurros.assombracao.Retirada;
import com.sussurros.entidade.HospedeEntity;

/**
 * Como uma aparição de observação termina (0.9.0-alpha17): o relance de sempre, e a exceção em que ele vai
 * embora andando em vez de sumir.
 *
 * Conferem a regra contra um jogador de mentira. Não dizem se o andar dele convence: isso é com as fotos e,
 * no fim, com o dono.
 */
public class TestesDasAparicoes {
	private static void aparecer(GameTestHelper helper, ServerPlayer jogador, boolean saiAndando) {
		for (int i = 0; i < 6; i++) {
			if (Retirada.presencaParaTeste(jogador, saiAndando)) {
				return;
			}
		}
		helper.fail("não achou lugar para ele aparecer");
	}

	private static void olharParaOOutroLado(ServerPlayer jogador, HospedeEntity h) {
		Vec3 olho = jogador.getEyePosition();
		jogador.lookAt(EntityAnchorArgument.Anchor.EYES, olho.subtract(h.getEyePosition().subtract(olho)));
	}

	/**
	 * Visto, ele não dissolve: segura o olhar um instante, vira e sai andando. Enquanto o jogador olha, ele
	 * continua existindo e muda de lugar. Quando o jogador deixa de olhar, ele não está mais lá.
	 */
	@GameTest(maxTicks = 600)
	public void asVezesEleVaiEmboraAndando(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -5100, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "vai-embora-andando");
		Diretor.esquecer(jogador);
		Vec3[] ondeEstava = new Vec3[1];
		int[] passo = {0};
		int[] relogio = {0};
		helper.runAfterDelay(5, () -> aparecer(helper, jogador, true));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					// O jogador vira e olha para ele até ele começar a sair.
					if (h == null) {
						return;
					}
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
					if (++relogio[0] > 200) {
						helper.fail("olhado por dez segundos, ele deveria ter começado a ir embora");
					}
					if (h.estaIndoEmbora()) {
						ondeEstava[0] = h.position();
						passo[0] = 1;
						relogio[0] = 0;
					}
				}
				case 1 -> {
					// Três segundos olhando para ele enquanto sai: continua lá, sem dissolver, e andou.
					helper.assertTrue(h != null && !h.isRemoved() && !h.isSumindo(), "enquanto é olhado, ele não some nem dissolve: vai andando");
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
					if (++relogio[0] == 60) {
						double andou = h.position().distanceTo(ondeEstava[0]);
						helper.assertTrue(andou > 1.5, "em três segundos ele deveria ter andado; andou " + andou);
						passo[0] = 2;
						relogio[0] = 0;
					}
				}
				case 2 -> {
					// O jogador olha para o outro lado: em um segundo e meio ele não está mais lá.
					if (h == null) {
						helper.assertTrue(jogador.getHealth() == 20.0F, "ele não toca no jogador");
						JogadorDeTeste.remover(jogador);
						passo[0] = 3;
						helper.succeed();
						return;
					}
					olharParaOOutroLado(jogador, h);
					if (++relogio[0] > 30) {
						helper.fail("sem ser visto, ele já deveria ter ido embora");
					}
				}
				default -> {
				}
			}
		});
	}

	/** A regra não mudou: sem a saída andando, visto, ele some em menos de um segundo e nunca "vai embora". */
	@GameTest(maxTicks = 400)
	public void oRelanceContinuaSendoARegra(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -5200, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "relance");
		Diretor.esquecer(jogador);
		boolean[] existiu = new boolean[1];
		int[] olhando = {0};
		helper.runAfterDelay(5, () -> aparecer(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			existiu[0] = true;
			helper.assertFalse(h.estaIndoEmbora(), "sem a saída andando, ele não vai embora andando");
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
			helper.assertTrue(++olhando[0] <= 25, "olhado direto, ele deveria sumir em menos de um segundo");
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(existiu[0] && Diretor.criatura(jogador) == null, "ele ainda não apareceu e sumiu");
			JogadorDeTeste.remover(jogador);
		});
	}

	/**
	 * A cena da janela. Uma parede com um vidro atrás do jogador; ele aparece do lado de fora. Quando o jogador
	 * olha pelo vidro, ele não dissolve: sai andando ao longo da parede, para fora do quadro, e some dali.
	 */
	@GameTest(maxTicks = 1200)
	public void pelaJanelaEleSaiDoQuadroAndando(GameTestHelper helper) {
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -5300);
		ServerLevel level = helper.getLevel();
		// A parede fica cinco blocos atrás do jogador (ele olha para o sul), alta o bastante para ninguém subir
		// nela, com um vidro na altura dos olhos.
		for (int dx = -5; dx <= 5; dx++) {
			for (int dy = 0; dy < 9; dy++) {
				level.setBlockAndUpdate(pes.offset(dx, dy, -5), Blocks.STONE.defaultBlockState());
			}
		}
		BlockPos vidro = pes.offset(0, 1, -5);
		level.setBlockAndUpdate(vidro, Blocks.GLASS.defaultBlockState());
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -5300, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "janela");
		Diretor.esquecer(jogador);
		boolean[] foiEmbora = new boolean[1];
		boolean[] dissolveu = new boolean[1];
		Vec3[] ondeNasceu = new Vec3[1];
		double[] deLado = {0};
		helper.runAfterDelay(5, () -> {
			String resposta = Diretor.testarCenaJanela(jogador);
			helper.assertTrue(resposta.startsWith("Cena"), "a cena da janela deveria começar: " + resposta);
		});
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			if (ondeNasceu[0] == null) {
				ondeNasceu[0] = h.position();
				helper.assertTrue(h.getZ() < vidro.getZ(), "ele deveria nascer do lado de fora da parede, em " + h.blockPosition().toShortString());
			}
			// O jogador se vira e olha pelo vidro.
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(vidro));
			foiEmbora[0] |= h.estaIndoEmbora();
			dissolveu[0] |= h.isSumindo();
			deLado[0] = Math.max(deLado[0], Math.abs(h.getX() - ondeNasceu[0].x));
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(ondeNasceu[0] != null && Diretor.criatura(jogador) == null, "ele ainda não apareceu e foi embora");
			helper.assertTrue(foiEmbora[0], "visto pelo vidro, ele deveria ir embora andando");
			helper.assertFalse(dissolveu[0], "ele não deveria dissolver na frente do jogador");
			helper.assertTrue(deLado[0] > 1.0, "ele deveria ter saído de lado, ao longo da parede; andou " + deLado[0]);
			helper.assertTrue(jogador.getHealth() == 20.0F, "ele não toca no jogador");
			JogadorDeTeste.remover(jogador);
		});
	}
}
