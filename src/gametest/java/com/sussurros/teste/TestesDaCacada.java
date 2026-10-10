package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
	private static void comecarCacadaDeVerdade(GameTestHelper helper, ServerPlayer jogador) {
		helper.setTime(18000);
		for (int i = 0; i < 6; i++) {
			if (Diretor.cacadaParaTeste(jogador, true)) {
				return;
			}
		}
		helper.fail("a caçada não achou lugar para começar");
	}

	/**
	 * Ele vê de perto. Posto a 2,6 blocos atrás de um jogador imóvel (mais longe que o toque, mais perto que o
	 * "cheguei ao último lugar conhecido"), ele tem de pegá-lo. Antes ele parava ali, olhava em volta, não
	 * achava ninguém e ia embora: era a causa do teste de captura que falhava de vez em quando.
	 */
	@GameTest(maxTicks = 900)
	public void eleNaoDesisteAoLadoDeQuemEstaParado(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2500, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "parado-ao-lado");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		Vec3 inicio = jogador.position();
		boolean[] posto = new boolean[1];
		helper.runAfterDelay(5, () -> {
			helper.setTime(18000);
			for (int i = 0; i < 6 && !Diretor.cacadaParaTeste(jogador, false); i++) {
				// tenta de novo: o lugar de nascer é sorteado
			}
			helper.assertTrue(Diretor.criatura(jogador) != null, "a caçada deveria ter começado");
		});
		// Depois do aviso (até dez segundos), já perseguindo: ele é posto logo atrás do jogador, que olha para o sul.
		helper.runAfterDelay(215, () -> {
			HospedeEntity ele = Diretor.criatura(jogador);
			if (ele != null && !ele.isRemoved()) {
				ele.snapTo(jogador.getX(), jogador.getY(), jogador.getZ() - 2.6, 0.0F, 0.0F);
			}
			posto[0] = true;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(posto[0], "ele ainda não foi posto atrás do jogador");
			helper.assertTrue(jogador.position().distanceTo(inicio) > 8, "a 2,6 blocos de um jogador parado, ele deveria tê-lo pegado");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Quem sai do jogo no meio de uma caçada fica devendo. */
	@GameTest(maxTicks = 300)
	public void quemSaiNoMeioFicaDevendo(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1800, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "sai-no-meio");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		helper.runAfterDelay(5, () -> comecarCacadaDeVerdade(helper, jogador));
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(Diretor.criatura(jogador) != null && !Diretor.criatura(jogador).isRemoved(), "a caçada deveria estar em curso");
			helper.assertTrue(Memoria.de(jogador).get("caca_devida") == 0, "em curso, ainda não deve nada");
			Diretor.aoSair(jogador);
			helper.assertTrue(Memoria.de(jogador).get("caca_devida") == 1, "quem sai no meio deveria ficar devendo a caçada");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Quem foge para longe fica devendo, e ela volta sozinha um ou dois minutos depois, onde ele estiver. */
	@GameTest(maxTicks = 3600)
	public void quemFogeParaLongeEEncontrado(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1900, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "foge-longe");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		boolean[] deveu = new boolean[1];
		boolean[] fugiu = new boolean[1];
		helper.runAfterDelay(5, () -> comecarCacadaDeVerdade(helper, jogador));
		helper.runAfterDelay(80, () -> {
			// Cento e sessenta blocos de uma vez, como quem sai voando.
			jogador.snapTo(jogador.getX() + 160, jogador.getY(), jogador.getZ(), jogador.getYRot(), jogador.getXRot());
			fugiu[0] = true;
		});
		helper.onEachTick(() -> {
			helper.setTime(18000);
			deveu[0] |= fugiu[0] && Memoria.de(jogador).get("caca_devida") == 1;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(deveu[0], "depois de fugir, deveria ficar devendo a caçada");
			helper.assertTrue(Memoria.de(jogador).get("caca_devida") == 0, "a caçada devida ainda não voltou");
			HospedeEntity h = Diretor.criatura(jogador);
			helper.assertTrue(h != null && !h.isRemoved() && h.getModo() == HospedeEntity.Modo.CACAR, "deveria haver uma caçada nova em curso");
			helper.assertTrue(h.distanceTo(jogador) < 40, "e ela deveria ser onde ele está agora, não onde ele estava");
			JogadorDeTeste.remover(jogador);
		});
	}
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
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3900, 0);
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
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4000, 0);
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
		BlockPos base = JogadorDeTeste.chao(helper, 4, -4100);
		for (int y = 0; y < 5; y++) {
			helper.getLevel().setBlockAndUpdate(base.above(y), Blocks.COBBLESTONE.defaultBlockState());
		}
		// O "chão" dessa coluna agora é o topo do pilar.
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4100, 0);
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
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4200, 0);
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

	// ===== 0.9.0-alpha16: os truques que encerravam a caçada de graça (pesquisa/2026-10-10-a-cacada.md) =====

	/** Um pilar de cinco blocos, com o jogador em cima. Não é a primeira caçada da vida dele. */
	private static ServerPlayer noPilar(GameTestHelper helper, int dz, String nome) {
		BlockPos base = JogadorDeTeste.chao(helper, 4, dz);
		for (int y = 0; y < 5; y++) {
			helper.getLevel().setBlockAndUpdate(base.above(y), Blocks.COBBLESTONE.defaultBlockState());
		}
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, dz, 0);
		JogadorDeTeste.acompanhar(helper, jogador, nome);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		helper.setTime(18000);
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.CACADAS, 1);
		m.salvar();
		return jogador;
	}

	/** Olha para ele um segundo a cada quatro: o bastante para ele saber onde o jogador está, sem segurá-lo. */
	private static void olharDeVezEmQuando(ServerPlayer jogador, HospedeEntity h, int relogio) {
		if (relogio % 80 < 20) {
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
		} else {
			jogador.setXRot(-80.0F); // para o céu
		}
	}

	/** Um barco vazio em cima dele não o recolhe, ele recusa montar e o laço não o prende. */
	@GameTest(maxTicks = 300)
	public void barcoELacoNaoOSeguram(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3100, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "barco");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		ServerLevel level = helper.getLevel();
		Boat[] barco = new Boat[1];
		helper.runAfterDelay(5, () -> {
			comecar(helper, jogador, false);
			HospedeEntity h = Diretor.criatura(jogador);
			// No jogo, um barco sem ninguém no leme recolhe qualquer criatura mais estreita que ele que encoste.
			barco[0] = EntityTypes.OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
			helper.assertTrue(barco[0] != null && h != null, "deveria haver um barco e uma caçada");
			barco[0].setPos(h.getX(), h.getY(), h.getZ());
			level.addFreshEntity(barco[0]);
		});
		// Três segundos com o barco em cima dele, ainda dentro do aviso (ele está parado).
		helper.runAfterDelay(65, () -> {
			HospedeEntity h = Diretor.criatura(jogador);
			helper.assertTrue(h != null && !h.isRemoved(), "a caçada deveria estar em curso");
			helper.assertFalse(h.isPassenger(), "ele não pode virar passageiro de um barco");
			helper.assertTrue(barco[0].getPassengers().isEmpty(), "o barco deveria continuar vazio");
			helper.assertFalse(h.startRiding(barco[0]), "ele recusa montar");
			helper.assertFalse(h.canBeLeashed(), "ele não aceita laço");
			barco[0].discard();
			h.sumir(level, false, "FIM_DO_TESTE");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Em cima do pilar e olhando para ele o tempo todo. Encarar o segura, mas não esconde que não há caminho:
	 * ele avisa no bloco de baixo e passa por dentro. (Ficava congelado até a caçada acabar sozinha.)
	 */
	@GameTest(maxTicks = 2400)
	public void encararDeCimaDoPilarNaoVence(GameTestHelper helper) {
		ServerPlayer jogador = noPilar(helper, -3200, "pilar-encarando");
		Vec3 inicio = jogador.position();
		boolean[] atravessou = new boolean[1];
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			atravessou[0] |= "ATRAVESSA".equals(h.getEstagioDaCaca());
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(atravessou[0], "encarado de cima do pilar, ele deveria usar a regra de atravessar");
			helper.assertTrue(jogador.position().distanceTo(inicio) > 8, "quem fica no pilar depois do aviso deveria ser levado");
			helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F, "não pode matar");
			JogadorDeTeste.remover(jogador);
		});
	}

	/**
	 * Ele sobe por dentro do pilar: subir mais três blocos durante o aviso não escapa. O que escapa é sair da
	 * coluna. (Subir dois blocos escapava todas as vezes.)
	 */
	@GameTest(maxTicks = 2400)
	public void subirMaisNaoEscapaDoPilar(GameTestHelper helper) {
		ServerPlayer jogador = noPilar(helper, -3300, "pilar-subindo");
		boolean[] atravessando = new boolean[1];
		int[] relogio = new int[1];
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			boolean agora = "ATRAVESSA".equals(h.getEstagioDaCaca());
			if (agora && !atravessando[0]) {
				// Toda vez que o aviso começa, o jogador sobe mais três blocos na mesma coluna.
				jogador.snapTo(jogador.getX(), jogador.getY() + 3, jogador.getZ(), jogador.getYRot(), jogador.getXRot());
			}
			atravessando[0] = agora;
			olharDeVezEmQuando(jogador, h, ++relogio[0]);
		});
		helper.succeedWhen(() -> {
			// A captura custa vida. (Tão alto, ela não acha para onde levar o jogador: ele fica onde está.)
			helper.assertTrue(jogador.getHealth() < 20.0F, "subir mais durante o aviso não deveria escapar: ele sobe por dentro");
			helper.assertTrue(Diretor.criatura(jogador) == null, "depois de pegar, ele vai embora");
			helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F, "não pode matar");
			JogadorDeTeste.remover(jogador);
		});
	}

	/**
	 * "Sair de perto resolve": quem sai da coluna durante o aviso não é pego, e ele, ao aparecer ali, fica um
	 * instante parado. (Quem tinha saído a dois ou três blocos era tocado no tick seguinte.)
	 */
	@GameTest(maxTicks = 2400)
	public void quemSaiDePertoTemUmRespiro(GameTestHelper helper) {
		ServerPlayer jogador = noPilar(helper, -4900, "pilar-saindo");
		Vec3[] depois = new Vec3[1];
		boolean[] atravessando = new boolean[1];
		int[] relogio = new int[1];
		int[] desdeAVolta = {-1};
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (desdeAVolta[0] >= 0) {
				if (++desdeAVolta[0] == 20) {
					helper.assertTrue(h != null && !h.isRemoved(), "a caçada deveria continuar");
					helper.assertTrue(jogador.position().distanceTo(depois[0]) < 0.1 && jogador.getHealth() == 20.0F,
							"um segundo depois de ele atravessar e não achar ninguém, o jogador não deveria ter sido tocado");
					h.sumir(helper.getLevel(), false, "FIM_DO_TESTE");
					JogadorDeTeste.remover(jogador);
					helper.succeed();
				}
				return;
			}
			if (h == null) {
				return;
			}
			boolean agora = "ATRAVESSA".equals(h.getEstagioDaCaca());
			if (agora && !atravessando[0]) {
				// O aviso começou: o jogador sai da coluna, 2,3 blocos para o lado (mais que os 1,9 da captura,
				// menos que os 2,4 do toque).
				jogador.snapTo(jogador.getX() + 2.3, jogador.getY(), jogador.getZ(), jogador.getYRot(), jogador.getXRot());
				depois[0] = jogador.position();
			}
			if (!agora && atravessando[0]) {
				desdeAVolta[0] = 0; // ele acabou de aparecer no lugar onde o jogador estava
			}
			atravessando[0] = agora;
			if (!agora) {
				olharDeVezEmQuando(jogador, h, ++relogio[0]);
			}
		});
	}

	/**
	 * Andar para longe durante o aviso não encerra a caçada: quando o aviso acaba ele sabe onde o jogador está
	 * e vai até lá. (Ele saía atrás do lugar de dez segundos antes, e a mais de 42 blocos não ouve.)
	 */
	@GameTest(maxTicks = 1200)
	public void andarParaLongeNoAvisoNaoBasta(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3500, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "longe-no-aviso");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		// Não é a primeira caçada da vida dele: sabendo onde o jogador está, ele pode usar o atalho.
		Memoria memoria = Memoria.de(jogador);
		memoria.set(Memoria.CACADAS, 1);
		memoria.salvar();
		boolean[] andou = new boolean[1];
		boolean[] chegou = new boolean[1];
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.runAfterDelay(60, () -> {
			// Cinquenta blocos para o lado, ainda dentro do aviso, sem olhar para trás.
			jogador.snapTo(jogador.getX() + 50, jogador.getY(), jogador.getZ(), jogador.getYRot(), jogador.getXRot());
			andou[0] = true;
		});
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			chegou[0] |= andou[0] && h != null && h.distanceTo(jogador) < 14;
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(chegou[0], "depois do aviso ele deveria ter ido até onde o jogador está");
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				h.sumir(helper.getLevel(), false, "FIM_DO_TESTE");
			}
			JogadorDeTeste.remover(jogador);
		});
	}

	/**
	 * Dentro de uma caixa de vidro, olhando para ele o tempo todo. O piscar o traz até o vidro e para ali: ele só
	 * entra pela regra de atravessar, que avisa antes. (O avanço do piscar não testava o trajeto e passava pelo vidro.)
	 */
	@GameTest(maxTicks = 2400)
	public void oPiscarNaoAtravessaVidro(GameTestHelper helper) {
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -5000);
		ServerLevel level = helper.getLevel();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
					for (int dy = 0; dy < 3; dy++) {
						level.setBlockAndUpdate(pes.offset(dx, dy, dz), Blocks.GLASS.defaultBlockState());
					}
				}
			}
		}
		// A coluna do meio não tem vidro: o "chão" dela continua sendo o chão.
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -5000, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "caixa-de-vidro");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		helper.setTime(18000);
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.CACADAS, 1);
		m.salvar();
		boolean[] atravessou = new boolean[1];
		double[] maisPertoAntes = {Double.MAX_VALUE};
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				return;
			}
			atravessou[0] |= "ATRAVESSA".equals(h.getEstagioDaCaca());
			if (!atravessou[0]) {
				// Antes do aviso de atravessar ele tem de estar do lado de fora do vidro (que fica a dois blocos).
				maisPertoAntes[0] = Math.min(maisPertoAntes[0], Math.max(Math.abs(h.getX() - jogador.getX()), Math.abs(h.getZ() - jogador.getZ())));
			}
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(jogador.getHealth() < 20.0F, "encarado de dentro da caixa, ele deveria acabar entrando pela regra de atravessar");
			helper.assertTrue(atravessou[0], "ele só pode entrar depois do aviso de atravessar");
			helper.assertTrue(maisPertoAntes[0] >= 2.0, "antes do aviso ele não pode ter passado pelo vidro: chegou a " + maisPertoAntes[0]);
			helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F, "não pode matar");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Colocar um bloco faz barulho, como quebrar: ele passa a saber onde foi. Clicar com outra coisa na mão, não. */
	@GameTest(maxTicks = 300)
	public void colocarBlocoEntregaAPosicao(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3600, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "colocar-bloco");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		ServerLevel level = helper.getLevel();
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		// Ainda dentro do aviso: ele está parado e só sabe onde o jogador estava quando nasceu.
		helper.runAfterDelay(40, () -> {
			HospedeEntity h = Diretor.criatura(jogador);
			helper.assertTrue(h != null && !h.isRemoved(), "a caçada deveria estar em curso");
			// O jogador a dez blocos dele (ele só ouve uma ação a até dezesseis).
			jogador.snapTo(h.getX(), h.getY(), h.getZ() + 10, jogador.getYRot(), jogador.getXRot());
			BlockPos chao = jogador.blockPosition().below();
			BlockHitResult clique = new BlockHitResult(Vec3.atCenterOf(chao).add(0, 0.5, 0), Direction.UP, chao, false);
			double antes = h.getConfiancaBusca();
			jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
			UseBlockCallback.EVENT.invoker().interact(jogador, level, InteractionHand.MAIN_HAND, clique);
			helper.assertTrue(h.getConfiancaBusca() == antes, "clicar com um graveto na mão não é barulho");
			jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
			UseBlockCallback.EVENT.invoker().interact(jogador, level, InteractionHand.MAIN_HAND, clique);
			helper.assertTrue(Math.abs(h.getConfiancaBusca() - 0.8) < 1.0E-6 && h.getConfiancaBusca() != antes,
					"colocar um bloco deveria entregar a posição: confiança " + h.getConfiancaBusca() + " (antes " + antes + ")");
			h.sumir(level, false, "FIM_DO_TESTE");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Na caça ele se abaixa e cabe em dois blocos de altura, então também nasce onde só há dois: debaixo de um
	 * teto baixo, como o de um corredor de mina. (Exigia três, e ali a caçada não achava onde começar.)
	 */
	@GameTest(maxTicks = 300)
	public void aCacadaComecaDebaixoDeTetoBaixo(GameTestHelper helper) {
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -3700);
		ServerLevel level = helper.getLevel();
		// Um teto de cinco blocos de espessura, dois blocos acima do chão, cobrindo todo o lugar onde ele pode nascer
		// (atrás do jogador, de 18 a 26 blocos). Espesso para a busca não achar o lado de cima dele.
		for (int dx = -15; dx <= 15; dx++) {
			for (int dz = -28; dz <= -14; dz++) {
				for (int dy = 2; dy <= 6; dy++) {
					level.setBlock(pes.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 2);
				}
			}
		}
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3700, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "teto-baixo");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		helper.runAfterDelay(5, () -> {
			comecar(helper, jogador, false);
			HospedeEntity h = Diretor.criatura(jogador);
			helper.assertTrue(h != null && !h.isRemoved(), "a caçada deveria ter começado debaixo do teto");
			helper.assertFalse(level.getBlockState(h.blockPosition().above(2)).isAir(), "ele deveria ter nascido debaixo do teto, em "
					+ h.blockPosition().toShortString());
			helper.assertTrue(h.getBbHeight() < 2.0F, "na caça ele tem menos de dois blocos de altura: " + h.getBbHeight());
			h.sumir(level, false, "FIM_DO_TESTE");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Numa caçada de verdade, ser pego deixa a marca (um coração a menos), e ela não passa de três. */
	@GameTest(maxTicks = 1600)
	public void aCapturaDeVerdadeDeixaAMarca(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4300, 0);
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
			// O descanso até a próxima ameaça (meia hora ou mais depois de uma captura) fica guardado na memória
			// do jogador: fechar o mundo não o apaga.
			long agora = helper.getLevel().getGameTime() / 20;
			helper.assertTrue(m.get(Memoria.AMEACA_LIBERADA_EM) > agora + 1000,
					"o descanso depois da captura deveria estar guardado: " + m.get(Memoria.AMEACA_LIBERADA_EM) + " (agora " + agora + ")");
			JogadorDeTeste.remover(jogador);
		});
	}
}
