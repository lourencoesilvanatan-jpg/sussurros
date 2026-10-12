package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Afronta;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.assombracao.Memoria;
import com.sussurros.assombracao.PrimeiroContato;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

/**
 * A afronta (0.9.0-alpha25): o que o jogador faz ao Hóspede fica guardado, e ele responde na hora e depois.
 *
 * Cada teste faz uma das coisas que somam ou tiram e confere o número e a resposta. As contas puras (quanto
 * esfria, quanto pesa, quando ele vem) estão no AfrontaTest.
 */
public class TestesDaAfronta {
	/** O número só muda no segundo do Diretor seguinte ao gatilho: os testes olham um segundo e meio depois. */
	private static final int UM_SEGUNDO_E_MEIO = 30;

	/**
	 * Bater nele: o número sobe 25 a cada golpe, e na hora as chamas em volta do jogador ficam pálidas, só para
	 * ele. O amigo ao lado não vê nada mudar.
	 */
	@GameTest(maxTicks = 300)
	public void baterNeleFicaGuardadoEAsChamasRespondem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -8900);
		BlockPos tocha1 = JogadorDeTeste.chao(helper, 6, -8900);
		BlockPos tocha2 = JogadorDeTeste.chao(helper, 2, -8899);
		level.setBlock(tocha1, Blocks.TORCH.defaultBlockState(), 2);
		level.setBlock(tocha2, Blocks.TORCH.defaultBlockState(), 2);
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -8900, 0);
		ServerPlayer amigo = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 5, -8900, 0);
		Diretor.esquecer(jogador);
		Diretor.esquecer(amigo);
		// Fase 1 de propósito: da fase 2 em diante as chamas pálidas de sempre podem pegar uma destas tochas antes
		// do golpe (a que já está pálida não empalidece de novo), e o teste contaria uma só.
		Diretor.definirFase(jogador, 1);
		BlockPos ondeEle = JogadorDeTeste.chao(helper, 4, -8908);
		helper.runAfterDelay(20, () -> {
			Diretor.cacadaEmParaTeste(jogador, ondeEle, true);
			helper.assertTrue(Diretor.criatura(jogador) != null, "ele deveria estar ali");
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 0, "antes de qualquer coisa, a afronta é zero");
		});
		helper.runAfterDelay(30, () -> {
			HospedeEntity h = Diretor.criatura(jogador);
			h.hurtServer(level, level.damageSources().playerAttack(jogador), 1.0F);
			int palidas = Diretor.miragensParaTeste(jogador, "AFRONTA");
			helper.assertTrue(palidas == 2, "no golpe, as duas tochas em volta dele deveriam empalidecer; foram " + palidas);
			helper.assertTrue(Diretor.miragensParaTeste(amigo, "AFRONTA") == 0, "as chamas pálidas são só do alvo");
			helper.assertTrue(level.getBlockState(tocha1).is(Blocks.TORCH), "a tocha de verdade não muda");
		});
		helper.runAfterDelay(30 + UM_SEGUNDO_E_MEIO, () -> helper.assertTrue(Afronta.valorParaTeste(jogador) == 25,
				"um golpe vale 25; está em " + Afronta.valorParaTeste(jogador)));
		helper.runAfterDelay(90, () -> {
			HospedeEntity h = Diretor.criatura(jogador);
			helper.assertTrue(h != null, "na caçada, bater não o manda embora");
			h.hurtServer(level, level.damageSources().playerAttack(jogador), 1.0F);
		});
		helper.runAfterDelay(90 + UM_SEGUNDO_E_MEIO, () -> {
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 50, "dois golpes valem 50; está em " + Afronta.valorParaTeste(jogador));
			helper.assertTrue(Afronta.valorParaTeste(amigo) == 0, "a afronta é de quem bateu");
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				h.sumir(level, false, "FIM_DO_TESTE");
			}
			level.removeBlock(tocha1, false);
			level.removeBlock(tocha2, false);
			JogadorDeTeste.remover(jogador);
			JogadorDeTeste.remover(amigo);
			helper.succeed();
		});
	}

	/**
	 * Pôr um bloco entre os dois, com ele perto: duas batidas do outro lado do bloco e uma rachadura nele, só para
	 * o alvo, e o número sobe 15. Uma vez por aparição: o segundo bloco não soma.
	 */
	@GameTest(maxTicks = 300)
	public void fecharAPassagemNaCaraDeleTemResposta(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -9000);
		// Um piso de pedra, para o chão ser plano entre os dois.
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(-2, -1, -8), pes.offset(2, 3, 2))) {
			level.setBlock(pos, (pos.getY() < pes.getY() ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
		}
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes);
		Diretor.definirFase(jogador, 4);
		BlockPos posto = pes.offset(0, 0, -2);
		Vec3 centro = Vec3.atCenterOf(posto);
		int[] rachaduras = new int[1];
		helper.runAfterDelay(20, () -> Diretor.cacadaEmParaTeste(jogador, pes.offset(0, 0, -6), true));
		helper.runAfterDelay(30, () -> {
			JogadorDeTeste.esquecerSons(jogador);
			jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 8));
			BlockPos chao = posto.below();
			UseBlockCallback.EVENT.invoker().interact(jogador, level, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(chao).add(0, 0.5, 0), Direction.UP, chao, false));
			level.setBlock(posto, Blocks.COBBLESTONE.defaultBlockState(), 3);
		});
		for (int t = 31; t <= 60; t++) {
			helper.runAfterDelay(t, () -> rachaduras[0] += JogadorDeTeste.recebidos(jogador)[3]);
		}
		helper.runAfterDelay(30 + UM_SEGUNDO_E_MEIO + 2, () -> {
			int batidas = JogadorDeTeste.sonsVindosDe(jogador, centro, 1.5, "sussurros:pancada");
			Sussurros.LOGGER.info("[afronta] fechou: batidas {}, rachaduras {}, valor {}", batidas, rachaduras[0], Afronta.valorParaTeste(jogador));
			helper.assertTrue(batidas == 2, "duas batidas do outro lado do bloco; foram " + batidas + " de " + JogadorDeTeste.ultimosSons(jogador));
			helper.assertTrue(rachaduras[0] >= 1, "o bloco que ele pôs deveria rachar para ele");
			helper.assertTrue(level.getBlockState(posto).is(Blocks.COBBLESTONE), "o bloco de verdade não muda");
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 15, "fechar a passagem vale 15; está em " + Afronta.valorParaTeste(jogador));
			// O segundo bloco, diante da mesma aparição, não soma.
			BlockPos chao = posto.below().north();
			UseBlockCallback.EVENT.invoker().interact(jogador, level, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(chao).add(0, 0.5, 0), Direction.UP, chao, false));
		});
		helper.runAfterDelay(30 + 2 * UM_SEGUNDO_E_MEIO + 4, () -> {
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 15, "é uma vez por aparição; está em " + Afronta.valorParaTeste(jogador));
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				h.sumir(level, false, "FIM_DO_TESTE");
			}
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Na Tigela: a oferenda aceita baixa 15, e oferecer uma ferramenta contra ele soma 20. */
	@GameTest(maxTicks = 300)
	public void aOferendaBaixaEAOfensaSobe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -9100, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		BlockPos tigela = jogador.blockPosition().offset(2, 0, 0);
		level.setBlockAndUpdate(tigela, ModBlocos.TIGELA_OFERENDA.defaultBlockState());
		BlockHitResult clique = new BlockHitResult(Vec3.atCenterOf(tigela).add(0, 0.5, 0), Direction.UP, tigela, false);
		helper.runAfterDelay(10, () -> {
			Afronta.definirParaTeste(jogador, 50);
			jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 2));
			jogador.gameMode.useItemOn(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND, clique);
			helper.assertTrue(Diretor.testarOferenda(jogador, true).contains("ACEITA"), "o pão deveria ser aceito");
		});
		helper.runAfterDelay(10 + UM_SEGUNDO_E_MEIO, () -> {
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 35, "a oferenda aceita tira 15; está em " + Afronta.valorParaTeste(jogador));
			// Limpa as cinzas e oferece a vela: é ofensa.
			jogador.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			jogador.gameMode.useItemOn(jogador, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, clique);
			jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VELA_PALIDA));
			jogador.gameMode.useItemOn(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND, clique);
			helper.assertTrue(Diretor.testarOferenda(jogador, true).contains("AFRONTA"), "vela na tigela é afronta");
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "AFRONTA") == 0, "o que acontece na tigela não tem a resposta das chamas");
		});
		helper.runAfterDelay(10 + 2 * UM_SEGUNDO_E_MEIO, () -> {
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 55, "a ofensa soma 20; está em " + Afronta.valorParaTeste(jogador));
			level.removeBlock(tigela, false);
			level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					net.minecraft.world.phys.AABB.ofSize(Vec3.atCenterOf(tigela), 6, 6, 6)).forEach(e -> e.discard());
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Irritado, ele vem à janela sem sorteio: a cena começa, uma parte da conta fica paga (menos 20), a visita
	 * fica contada, e a página do diário que fala disso é pedida. Ele aparece do lado de fora do vidro.
	 */
	@GameTest(maxTicks = 900)
	public void irritadoEleVemAJanela(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -9200);
		for (int dx = -5; dx <= 5; dx++) {
			for (int dy = 0; dy < 9; dy++) {
				level.setBlockAndUpdate(pes.offset(dx, dy, -5), Blocks.STONE.defaultBlockState());
			}
		}
		BlockPos vidro = pes.offset(0, 1, -5);
		level.setBlockAndUpdate(vidro, Blocks.GLASS.defaultBlockState());
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -9200, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "afronta-janela");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		boolean[] veio = new boolean[1];
		boolean[] apareceu = new boolean[1];
		helper.runAfterDelay(10, () -> {
			Afronta.definirParaTeste(jogador, 50);
			String como = Afronta.visitarParaTeste(jogador);
			helper.assertTrue("JANELA".equals(como), "com vidro na parede, ele vem à janela; veio " + como);
			helper.assertTrue(Afronta.valorParaTeste(jogador) == 30, "a visita paga 20 da conta; ficou em " + Afronta.valorParaTeste(jogador));
			helper.assertTrue(Memoria.de(jogador).get("afronta_visitas") == 1, "a visita deveria ficar contada");
			veio[0] = true;
		});
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null && veio[0]) {
				apareceu[0] = true;
				helper.assertTrue(h.getZ() < vidro.getZ(), "ele deveria estar do lado de fora do vidro, em " + h.blockPosition().toShortString());
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(veio[0] && apareceu[0], "ele ainda não apareceu na janela");
			helper.assertTrue((Memoria.de(jogador).get("paginas_pedidas") >> 29 & 1) == 1, "a página 30 (a da afronta) deveria ter sido pedida");
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				h.sumir(level, false, "FIM_DO_TESTE");
			}
			JogadorDeTeste.remover(jogador);
		});
	}

	/**
	 * Furioso, na última fase, o descanso até a próxima sequência de ameaça acaba em dois minutos, e uma parte
	 * grande da conta fica paga. Na fase 3 isso não acontece. E o número esfria com os dias.
	 */
	@GameTest(maxTicks = 300)
	public void furiosoODescansoAcabaEComOsDiasEsfria(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer naFase3 = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -700, 0);
		ServerPlayer naFase4 = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -9400, 0);
		ServerPlayer esfriando = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -9500, 0);
		for (ServerPlayer p : new ServerPlayer[] {naFase3, naFase4, esfriando}) {
			Diretor.esquecer(p);
		}
		Diretor.definirFase(naFase3, 3);
		Diretor.definirFase(naFase4, 4);
		long[] agora = new long[1];
		helper.runAfterDelay(10, () -> {
			agora[0] = level.getGameTime() / 20;
			for (ServerPlayer p : new ServerPlayer[] {naFase3, naFase4}) {
				Memoria m = Memoria.de(p);
				m.set(Memoria.AMEACA_LIBERADA_EM, (int) agora[0] + 1500);
				m.set("afronta", 90);
				m.salvar();
			}
			Memoria m = Memoria.de(esfriando);
			m.set("afronta", 80);
			m.set("afronta_dia", (int) (level.getDefaultClockTime() / 24000L) - 2);
			m.salvar();
		});
		helper.runAfterDelay(10 + 2 * UM_SEGUNDO_E_MEIO, () -> {
			long falta4 = Memoria.de(naFase4).get(Memoria.AMEACA_LIBERADA_EM) - agora[0];
			long falta3 = Memoria.de(naFase3).get(Memoria.AMEACA_LIBERADA_EM) - agora[0];
			helper.assertTrue(falta4 <= 125, "furioso, na fase 4, o descanso cai para dois minutos; faltam " + falta4 + " s");
			helper.assertTrue(Afronta.valorParaTeste(naFase4) == 50, "adiantar a ameaça paga 40 da conta; ficou em " + Afronta.valorParaTeste(naFase4));
			helper.assertTrue(falta3 >= 1400, "na fase 3 o descanso não muda; faltam " + falta3 + " s");
			helper.assertTrue(Afronta.valorParaTeste(naFase3) == 90, "e nada é cobrado; está em " + Afronta.valorParaTeste(naFase3));
			helper.assertTrue(Afronta.valorParaTeste(esfriando) == 20, "dois dias depois, 80 vira 20; está em " + Afronta.valorParaTeste(esfriando));
			for (ServerPlayer p : new ServerPlayer[] {naFase3, naFase4, esfriando}) {
				JogadorDeTeste.remover(p);
			}
			helper.succeed();
		});
	}

	/**
	 * No túnel dos testes de perto: ele o vê de verdade e não faz nada, e a afronta desce 3; depois corre para
	 * cima dele com a mira nele, e ela sobe 10, com a tocha da curva empalidecendo na hora.
	 */
	@GameTest(maxTicks = 900)
	public void deixarEmPazBaixaEInvestirSobe(GameTestHelper helper) {
		BlockPos pes = TestesDePerto.montarTunel(helper, -9300);
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes);
		// Fase 1 de propósito, pelo mesmo motivo do teste de bater: a tocha da curva é a única do túnel, e as
		// chamas pálidas de sempre (da fase 2 em diante) às vezes a pegavam antes da investida.
		Diretor.definirFase(jogador, 1);
		PrimeiroContato.darPorFeitoParaTeste(jogador);
		ServerLevel level = helper.getLevel();
		int[] relogio = {0};
		int[] passo = {0};
		int[] desde = {0};
		TestesDePerto.cadaTick(helper, () -> {
			if (passo[0] > 4) {
				return;
			}
			level.getChunkSource().move(jogador);
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					TestesDePerto.olharParaOSul(jogador);
					if (relogio[0] > 200) {
						helper.fail("o Diretor deveria ter lido o túnel como SUBSOLO; leu " + Diretor.contextoParaTeste(jogador));
					}
					if (relogio[0] >= 40 && "SUBSOLO".equals(Diretor.contextoParaTeste(jogador))) {
						Afronta.definirParaTeste(jogador, 10);
						helper.assertTrue(Diretor.eventoDoDiretorParaTeste(jogador, Evento.PRESENCA), "no túnel a presença deveria achar lugar");
						passo[0] = 1;
						desde[0] = relogio[0];
					}
				}
				case 1 -> {
					// Ele vira, olha, e não faz mais nada: visto de verdade, ele some.
					if (h != null) {
						jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
						if (relogio[0] - desde[0] > 60) {
							helper.fail("olhado, de perto, ele deveria ter sumido");
						}
						return;
					}
					passo[0] = 2;
					desde[0] = relogio[0];
				}
				case 2 -> {
					TestesDePerto.olharParaOSul(jogador);
					if (relogio[0] - desde[0] == UM_SEGUNDO_E_MEIO) {
						helper.assertTrue(Afronta.valorParaTeste(jogador) == 7, "vê-lo e deixá-lo em paz tira 3; está em " + Afronta.valorParaTeste(jogador));
						helper.assertTrue(Diretor.eventoDoDiretorParaTeste(jogador, Evento.PRESENCA), "a segunda presença deveria achar lugar");
						passo[0] = 3;
						desde[0] = relogio[0];
					}
				}
				case 3 -> {
					// Agora ele corre para cima, com a mira nele: de um tick para o outro está a quatro blocos.
					helper.assertTrue(h != null, "a segunda presença deveria estar na curva");
					if (relogio[0] - desde[0] == 5) {
						Vec3 rumo = h.position().subtract(jogador.position()).normalize();
						Vec3 perto = h.position().subtract(rumo.scale(4.0));
						jogador.snapTo(perto.x, jogador.getY(), perto.z, jogador.getYRot(), 0.0F);
						jogador.setSprinting(true);
						jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
						passo[0] = 4;
						desde[0] = relogio[0];
					}
				}
				case 4 -> {
					if (relogio[0] - desde[0] == 3) {
						int palidas = Diretor.miragensParaTeste(jogador, "AFRONTA");
						helper.assertTrue(palidas >= 1, "na investida, a tocha da curva deveria empalidecer; foram " + palidas);
					}
					if (relogio[0] - desde[0] == UM_SEGUNDO_E_MEIO + 3) {
						helper.assertTrue(Afronta.valorParaTeste(jogador) == 17, "investir soma 10; está em " + Afronta.valorParaTeste(jogador));
						HospedeEntity ele = Diretor.criatura(jogador);
						if (ele != null) {
							ele.sumir(level, false, "FIM_DO_TESTE");
						}
						JogadorDeTeste.remover(jogador);
						passo[0] = 5;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}
}
