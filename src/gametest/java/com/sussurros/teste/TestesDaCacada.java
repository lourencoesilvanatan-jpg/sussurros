package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.entidade.HospedeEntity;

/**
 * A caçada contra um jogador de mentira, num servidor de verdade (./gradlew runGameTest).
 *
 * Cada teste põe o jogador numa situação (de costas, encarando, em cima de um pilar, dentro da vela) e
 * confere o desfecho. Não mede medo; mede se as regras valem e se nada trava.
 */
public class TestesDaCacada {
	/** Começa a caçada. O lugar onde ele nasce é sorteado, então tenta algumas vezes antes de reclamar. */
	private static void comecar(GameTestHelper helper, ServerPlayer jogador, boolean deVerdade) {
		helper.setTime(18000);
		for (int i = 0; i < 6; i++) {
			if (Diretor.cacadaParaTeste(jogador, deVerdade)) {
				return;
			}
		}
		helper.fail("a caçada não achou lugar para começar");
	}

	/** Quem fica parado e de costas é alcançado e levado. Não morre, larga o que tinha na mão e acorda longe. */
	@GameTest(maxTicks = 1600)
	public void quemFicaDeCostasELevado(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -12, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "quemFicaDeCostasELevado");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH, 5));
		Vec3 inicio = jogador.position();
		double[] medida = new double[2]; // blocos andados e ticks andando, durante a perseguição
		Vec3[] ultima = new Vec3[1];

		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null || !"PERSEGUE".equals(h.getEstagioDaCaca())) {
				ultima[0] = null;
				return;
			}
			if (ultima[0] != null) {
				double d = h.position().distanceTo(ultima[0]);
				if (d > 0.15 && d < 1.0) {
					medida[0] += d;
					medida[1]++;
				}
			}
			ultima[0] = h.position();
		});
		helper.succeedWhen(() -> {
			double levado = jogador.position().distanceTo(inicio);
			helper.assertTrue(levado > 8, "o jogador ainda não foi levado");
			helper.assertTrue(levado >= 18 && levado < 60, "deveria ser levado a 20-40 blocos, foi a " + levado);
			helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F, "a captura não pode deixar abaixo de dois corações");
			helper.assertTrue(jogador.getHealth() < 20.0F, "a captura deveria custar vida");
			helper.assertTrue(jogador.getMainHandItem().isEmpty(), "o que estava na mão deveria ter ficado para trás");
			boolean achou = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
					new net.minecraft.world.phys.AABB(inicio.subtract(3, 3, 3), inicio.add(3, 3, 3)),
					item -> item.getItem().is(Items.TORCH)).isEmpty();
			helper.assertTrue(achou, "o item da mão deveria estar caído onde ele foi pego");
			// Criatura de teste não deixa marca.
			helper.assertTrue(jogador.getAttributeValue(Attributes.MAX_HEALTH) == 20.0, "caçada de teste não pode marcar");
			helper.assertTrue(medida[1] > 20, "ele quase não andou: " + medida[1] + " ticks");
			double blocosPorSegundo = medida[0] / medida[1] * 20.0;
			// Pedimos 4,9 blocos/s no começo. Entre a caminhada (4,3) e a corrida (5,6) do jogador.
			helper.assertTrue(blocosPorSegundo > 4.2 && blocosPorSegundo < 5.7,
					"velocidade fora do esperado: " + String.format("%.2f", blocosPorSegundo) + " blocos/s");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Encarar segura, mas não vence: ele não some por ser olhado, e a cada piscar chega mais perto. */
	@GameTest(maxTicks = 2400)
	public void encararNaoVence(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -12, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "encararNaoVence");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		helper.setTime(18000);
		Vec3 inicio = jogador.position();
		double[] distancias = {-1, -1}; // a primeira e a menor distância vistas
		boolean[] existiu = new boolean[1];

		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			existiu[0] = true;
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
			double d = h.distanceTo(jogador);
			if (distancias[0] < 0) {
				distancias[0] = d;
			}
			distancias[1] = distancias[1] < 0 ? d : Math.min(distancias[1], d);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(existiu[0], "a caçada não começou");
			helper.assertTrue(jogador.position().distanceTo(inicio) > 8, "encarando sem parar, ele deveria acabar chegando");
			helper.assertTrue(distancias[1] < distancias[0] - 6, "ele não avançou enquanto era encarado");
			helper.assertTrue(jogador.isAlive(), "não pode matar");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Em cima de um pilar não há caminho. Ele avisa no bloco de baixo e passa por dentro. */
	@GameTest(maxTicks = 2400)
	public void pilarNaoSegura(GameTestHelper helper) {
		BlockPos base = JogadorDeTeste.chao(helper, 4, -12);
		for (int y = 0; y < 5; y++) {
			helper.getLevel().setBlockAndUpdate(base.above(y), Blocks.COBBLESTONE.defaultBlockState());
		}
		// O "chão" dessa coluna agora é o topo do pilar.
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -12, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "pilarNaoSegura");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		helper.setTime(18000);
		// Não é a primeira caçada da vida dele: na primeira, o pilar ainda funciona.
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.CACADAS, 1);
		m.salvar();
		Vec3 inicio = jogador.position();
		boolean[] atravessou = new boolean[1];
		int[] relogio = new int[1];

		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			if ("ATRAVESSA".equals(h.getEstagioDaCaca())) {
				atravessou[0] = true;
			}
			// Olha para ele um segundo a cada quatro: o bastante para ele saber onde o jogador está.
			if (++relogio[0] % 80 < 20) {
				jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
			} else {
				jogador.setXRot(-80.0F); // para o céu
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(atravessou[0], "ele não usou a regra de atravessar");
			helper.assertTrue(jogador.position().distanceTo(inicio) > 8, "quem fica no pilar depois do aviso deveria ser levado");
			helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F, "não pode matar");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Dentro da vela ele não entra: espera na borda por tempo limitado e vai embora. */
	@GameTest(maxTicks = 2400)
	public void aVelaSeguraEEleVaiEmbora(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -12, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "aVelaSeguraEEleVaiEmbora");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3); // na fase 4, com muitas velas usadas, ele pode soprar; aqui não
		Vec3 inicio = jogador.position();
		boolean[] esperou = new boolean[1];
		boolean[] existiu = new boolean[1];
		double[] menor = {Double.MAX_VALUE};

		helper.runAfterDelay(5, () -> {
			comecar(helper, jogador, false);
			Diretor.acenderVela(jogador);
		});
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			existiu[0] = true;
			menor[0] = Math.min(menor[0], h.distanceTo(jogador));
			if ("ESPERA_VELA".equals(h.getEstagioDaCaca())) {
				esperou[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(existiu[0] && Diretor.criatura(jogador) == null, "ele ainda está aqui");
			helper.assertTrue(esperou[0], "ele deveria ter esperado na borda da vela");
			helper.assertTrue(menor[0] > 7.5, "ele entrou na zona da vela: chegou a " + menor[0]);
			helper.assertTrue(jogador.position().distanceTo(inicio) < 1, "o jogador não deveria ter sido levado");
			helper.assertTrue(jogador.getHealth() == 20.0F, "o jogador não deveria ter sido tocado");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Numa caçada de verdade, ser pego deixa a marca (um coração a menos), e ela não passa de três. */
	@GameTest(maxTicks = 1600)
	public void aCapturaDeVerdadeDeixaAMarca(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -12, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "aCapturaDeVerdadeDeixaAMarca");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		Vec3 inicio = jogador.position();

		helper.runAfterDelay(5, () -> comecar(helper, jogador, true));
		helper.succeedWhen(() -> {
			helper.assertTrue(jogador.position().distanceTo(inicio) > 8, "o jogador ainda não foi levado");
			helper.assertTrue(jogador.getAttributeValue(Attributes.MAX_HEALTH) == 18.0,
					"a marca deveria tirar um coração: vida máxima " + jogador.getAttributeValue(Attributes.MAX_HEALTH));
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get(Memoria.CAPTURAS) == 1 && m.get(Memoria.MARCAS) == 1 && m.get(Memoria.CACADAS) == 1,
					"contadores errados: " + m.copia());
			JogadorDeTeste.remover(jogador);
		});
	}
}
