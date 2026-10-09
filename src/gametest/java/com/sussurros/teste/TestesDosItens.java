package com.sussurros.teste;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Avesso;
import com.sussurros.assombracao.Conta;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.bloco.TigelaBlockEntity;
import com.sussurros.bloco.TigelaOferendaBlock;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

/**
 * Os itens e blocos da 0.9, usados por um jogador de mentira num servidor de verdade.
 * Confere as regras (o que muda de estado, o que é consumido, o que some depois), não a aparência.
 */
public class TestesDosItens {
	private static void comecarCacada(GameTestHelper helper, ServerPlayer jogador) {
		helper.setTime(18000);
		for (int i = 0; i < 6; i++) {
			if (Diretor.cacadaParaTeste(jogador, false)) {
				return;
			}
		}
		helper.fail("a caçada não achou lugar para começar");
	}

	private static BlockHitResult cliqueEmCima(BlockPos pos) {
		return new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
	}

	private static int exibicoes(ServerLevel level, Vec3 centro, String etiqueta) {
		return level.getEntitiesOfClass(Display.ItemDisplay.class, AABB.ofSize(centro, 6, 6, 6),
				d -> d.entityTags().contains(etiqueta)).size();
	}

	/** Usar Cinza Pálida no chão faz a linha, gasta a cinza e soma na Conta. */
	@GameTest(maxTicks = 100)
	public void aCinzaViraLinha(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -100, 0);
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		BlockPos chao = JogadorDeTeste.chao(helper, 6, -100).below();
		jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CINZA_PALIDA, 3));
		helper.runAfterDelay(5, () -> {
			jogador.gameMode.useItemOn(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND, cliqueEmCima(chao));
			helper.assertTrue(CinzaEspalhadaBlock.estadoEm(level, chao.above()) == CinzaEspalhadaBlock.Estado.INTACTA,
					"a cinza usada no chão deveria virar uma linha intacta");
			helper.assertTrue(jogador.getMainHandItem().getCount() == 2, "deveria gastar uma cinza, sobrou " + jogador.getMainHandItem().getCount());
			helper.assertTrue(Memoria.de(jogador).get("conta") == 1, "a linha deveria somar 1 na Conta");
			// Três tentativas de passar: riscada, gasta, rompida. A quarta não encontra mais nada segurando.
			helper.assertTrue(CinzaEspalhadaBlock.desgastar(level, chao.above()), "1a tentativa deveria ser barrada");
			helper.assertTrue(CinzaEspalhadaBlock.desgastar(level, chao.above()), "2a tentativa deveria ser barrada");
			helper.assertTrue(CinzaEspalhadaBlock.desgastar(level, chao.above()), "3a tentativa deveria ser barrada");
			helper.assertTrue(CinzaEspalhadaBlock.estadoEm(level, chao.above()) == CinzaEspalhadaBlock.Estado.ROMPIDA, "deveria estar rompida");
			helper.assertFalse(CinzaEspalhadaBlock.desgastar(level, chao.above()), "linha rompida não segura mais");
			level.removeBlock(chao.above(), false);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Um anel de linha em volta do jogador segura a caçada por um tempo e sai gasto. No fim, ele passa. */
	@GameTest(maxTicks = 2400)
	public void aLinhaSeguraACacadaPorUmTempo(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -200, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "linha");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		ServerLevel level = helper.getLevel();
		BlockPos centro = jogador.blockPosition();
		BlockState linha = ModBlocos.CINZA_ESPALHADA.defaultBlockState();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) == 3) {
					level.setBlockAndUpdate(centro.offset(dx, 0, dz), linha);
				}
			}
		}
		Vec3 inicio = jogador.position();
		int[] barradoEm = {-1};
		int[] relogio = new int[1];

		helper.runAfterDelay(5, () -> comecarCacada(helper, jogador));
		helper.onEachTick(() -> {
			relogio[0]++;
			if (barradoEm[0] < 0) {
				for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-3, 0, -3), centro.offset(3, 0, 3))) {
					CinzaEspalhadaBlock.Estado e = CinzaEspalhadaBlock.estadoEm(level, pos);
					if (e != null && e != CinzaEspalhadaBlock.Estado.INTACTA) {
						barradoEm[0] = relogio[0];
						break;
					}
				}
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(barradoEm[0] > 0, "nenhuma linha foi testada");
			helper.assertTrue(jogador.position().distanceTo(inicio) > 8, "ele ainda não passou pela linha");
			helper.assertTrue(relogio[0] - barradoEm[0] >= 20 * 8,
					"a linha deveria segurar pelo menos uns nove segundos, segurou " + (relogio[0] - barradoEm[0]) / 20.0);
			boolean rompida = false;
			for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-3, 0, -3), centro.offset(3, 0, 3))) {
				rompida |= CinzaEspalhadaBlock.estadoEm(level, pos) == CinzaEspalhadaBlock.Estado.ROMPIDA;
			}
			helper.assertTrue(rompida, "para ele passar, alguma linha deveria ter rompido");
			for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-3, 0, -3), centro.offset(3, 0, 3))) {
				if (CinzaEspalhadaBlock.estadoEm(level, pos) != null) {
					level.removeBlock(pos.immutable(), false);
				}
			}
			JogadorDeTeste.remover(jogador);
		});
	}

	/**
	 * O Lampião Pálido muda a chama quando ele chega perto, e apaga quando ele passa.
	 * Não usa uma caçada (que tem sorteio e já falhou no GitHub por não terminar a tempo): usa a criatura
	 * como ela é do outro lado, que vem em linha reta, devagar, até encostar.
	 */
	@GameTest(maxTicks = 900)
	public void oLampiaoReage(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -300, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "lampiao");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		ServerLevel level = helper.getLevel();
		BlockPos centro = jogador.blockPosition();
		BlockState aceso = ModBlocos.LAMPIAO_PALIDO.defaultBlockState()
				.setValue(LampiaoPalidoBlock.CHAMA, LampiaoPalidoBlock.Chama.CALMA)
				.setValue(LampiaoPalidoBlock.COMBUSTIVEL, 3);
		List<BlockPos> lampioes = List.of(centro.offset(2, 0, 2), centro.offset(-2, 0, 2), centro.offset(2, 0, -2), centro.offset(-2, 0, -2));
		lampioes.forEach(pos -> level.setBlockAndUpdate(pos, aceso));
		boolean[] reagiu = new boolean[1];
		HospedeEntity[] ele = new HospedeEntity[1];

		helper.runAfterDelay(5, () -> {
			helper.assertTrue(level.getBlockState(lampioes.get(0)).getLightEmission() == 12, "aceso e calmo deveria dar luz 12");
			helper.assertTrue(Avesso.criaturaParaTeste(jogador, true), "deveria haver lugar para ele");
			ele[0] = Diretor.criatura(jogador);
		});
		helper.onEachTick(() -> {
			for (BlockPos pos : lampioes) {
				BlockState s = level.getBlockState(pos);
				if (s.is(ModBlocos.LAMPIAO_PALIDO)) {
					LampiaoPalidoBlock.Chama c = s.getValue(LampiaoPalidoBlock.CHAMA);
					reagiu[0] |= c == LampiaoPalidoBlock.Chama.INQUIETA || c == LampiaoPalidoBlock.Chama.FRIA;
				}
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(ele[0] != null && ele[0].isRemoved(), "ele ainda não chegou ao jogador");
			helper.assertTrue(reagiu[0], "nenhum lampião ficou inquieto ou frio com ele chegando");
			boolean apagou = lampioes.stream().anyMatch(pos -> level.getBlockState(pos).is(ModBlocos.LAMPIAO_PALIDO)
					&& level.getBlockState(pos).getValue(LampiaoPalidoBlock.CHAMA) == LampiaoPalidoBlock.Chama.APAGADA);
			helper.assertTrue(apagou, "ele passou a menos de quatro blocos: algum lampião deveria estar apagado");
			lampioes.forEach(pos -> level.removeBlock(pos, false));
			JogadorDeTeste.remover(jogador);
		});
	}

	/** A tigela guarda um item, mostra, e o que ele decide muda o que sobra nela. */
	@GameTest(maxTicks = 200)
	public void aTigelaGuardaEEleDecide(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -400, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		ServerLevel level = helper.getLevel();
		BlockPos pos = jogador.blockPosition().offset(2, 0, 0);
		level.setBlockAndUpdate(pos, ModBlocos.TIGELA_OFERENDA.defaultBlockState());
		Vec3 centro = Vec3.atCenterOf(pos);
		jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD, 4));

		helper.runAfterDelay(5, () -> {
			jogador.gameMode.useItemOn(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND, cliqueEmCima(pos));
			helper.assertTrue(level.getBlockState(pos).getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.CHEIA, "deveria ficar cheia");
			helper.assertTrue(jogador.getMainHandItem().getCount() == 3, "deveria levar um pão só");
			helper.assertTrue(level.getBlockEntity(pos) instanceof TigelaBlockEntity t && t.item().is(Items.BREAD), "o pão deveria estar guardado");
		});
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(exibicoes(level, centro, "sussurros_oferenda") == 1, "o item oferecido deveria aparecer na tigela");
			// Recusada: nada muda.
			helper.assertTrue(Diretor.testarOferenda(jogador, false).contains("RECUSADA"), "deveria recusar");
			helper.assertTrue(level.getBlockState(pos).getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.CHEIA, "recusada continua cheia");
			// Aceita: o item some e ficam as cinzas.
			helper.assertTrue(Diretor.testarOferenda(jogador, true).contains("ACEITA"), "deveria aceitar");
			helper.assertTrue(level.getBlockState(pos).getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.CINZAS, "aceita vira cinzas");
			helper.assertTrue(level.getBlockEntity(pos) instanceof TigelaBlockEntity t && t.item().isEmpty(), "o item deveria ter sumido");
			helper.assertTrue(Memoria.de(jogador).get("oferendas_aceitas") == 1, "deveria contar uma oferenda aceita");
		});
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(exibicoes(level, centro, "sussurros_oferenda") == 0, "a exibição deveria ter sumido com o item");
			// Mão vazia limpa as cinzas.
			jogador.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			jogador.gameMode.useItemOn(jogador, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, cliqueEmCima(pos));
			helper.assertTrue(level.getBlockState(pos).getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.VAZIA, "deveria voltar a vazia");
			// Uma das ferramentas contra ele é afronta, não oferenda.
			jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VELA_PALIDA));
			jogador.gameMode.useItemOn(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND, cliqueEmCima(pos));
			helper.assertTrue(Diretor.testarOferenda(jogador, true).contains("AFRONTA"), "vela na tigela deveria ser afronta");
			level.removeBlock(pos, false);
		});
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(exibicoes(level, centro, "sussurros_oferenda") == 0, "quebrar a tigela deveria tirar a exibição");
			level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AABB.ofSize(centro, 6, 6, 6)).forEach(e -> e.discard());
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** A caixa toca, gasta a corda, conta os usos e soma na Conta. Tocando, não dá para dar corda de novo. */
	@GameTest(maxTicks = 200)
	public void aCaixaTocaEGasta(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -500, 0);
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAIXA_DE_MUSICA));
		helper.runAfterDelay(5, () -> {
			jogador.gameMode.useItem(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND);
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get(Memoria.CAIXA_USOS) == 1, "deveria contar um uso");
			helper.assertTrue(m.get("conta") == 2, "a caixa soma 2 na Conta, veio " + m.get("conta"));
			helper.assertTrue(jogador.getMainHandItem().getDamageValue() == 1, "deveria gastar a corda");
			jogador.getCooldowns().removeCooldown(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.CAIXA_DE_MUSICA));
			jogador.gameMode.useItem(jogador, level, jogador.getMainHandItem(), InteractionHand.MAIN_HAND);
			helper.assertTrue(Memoria.de(jogador).get(Memoria.CAIXA_USOS) == 1, "com a música tocando, não deveria dar corda de novo");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Os ossos caem (três, ou dois quando um se desfaz), somem depois, e cada jogada soma na Conta. */
	@GameTest(maxTicks = 500)
	public void osOssosCaemESomem(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -600, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 3);
		ServerLevel level = helper.getLevel();
		Vec3 centro = jogador.position();
		helper.runAfterDelay(5, () -> helper.assertTrue(Diretor.testarOssos(jogador, "silencio").startsWith("Ossos jogados"), "deveria jogar"));
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(exibicoes(level, centro, "sussurros_ossos") == 3, "deveriam cair três ossos, caíram " + exibicoes(level, centro, "sussurros_ossos"));
			helper.assertTrue(Memoria.de(jogador).get("conta") == 1, "uma jogada soma 1 na Conta");
		});
		helper.runAfterDelay(340, () -> {
			helper.assertTrue(exibicoes(level, centro, "sussurros_ossos") == 0, "os ossos deveriam ter sumido");
			Diretor.testarOssos(jogador, "conta");
		});
		helper.runAfterDelay(345, () -> {
			helper.assertTrue(exibicoes(level, centro, "sussurros_ossos") == 2, "no desfecho 'conta' um osso se desfaz");
			helper.assertTrue(Memoria.de(jogador).get("conta") == 5, "esse desfecho soma 4, veio " + Memoria.de(jogador).get("conta"));
			level.getEntitiesOfClass(Display.ItemDisplay.class, AABB.ofSize(centro, 8, 8, 8)).forEach(d -> d.discard());
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** A Conta estoura, avisa, cobra no item mais usado e zera. */
	@GameTest(maxTicks = 3400)
	public void aContaCobraEZera(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -700, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		helper.runAfterDelay(5, () -> {
			for (int i = 0; i < 11; i++) {
				Conta.somar(jogador, Conta.Item.VELA);
			}
			helper.assertTrue(Memoria.de(jogador).get("conta") == 11, "onze usos de vela deveriam dar 11");
		});
		helper.succeedWhen(() -> {
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("conta") == 0, "a Conta ainda não foi cobrada: " + m.get("conta"));
			helper.assertTrue(m.get("cobrado_vela") == 1, "a cobrança deveria ficar marcada para a próxima vela");
			helper.assertTrue(m.get("conta_avisos") == 0 && m.get("conta_vela") == 0, "depois de cobrar, tudo zera");
			// A vela seguinte dura a metade e consome a marca.
			Diretor.acenderVela(jogador);
			helper.assertTrue(Memoria.de(jogador).get("cobrado_vela") == 0, "a marca deveria ser consumida");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** A vela é um bloco de verdade enquanto a zona dura; tirar o bloco acaba com a zona. */
	@GameTest(maxTicks = 200)
	public void aVelaEUmBloco(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -800, 0);
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		BlockPos pe = jogador.blockPosition();
		helper.runAfterDelay(5, () -> {
			Diretor.acenderVela(jogador);
			helper.assertTrue(level.getBlockState(pe).is(ModBlocos.VELA_ACESA), "deveria haver uma vela de verdade aos pés dele; há "
					+ level.getBlockState(pe) + " sobre " + level.getBlockState(pe.below()) + ", ele em " + jogador.blockPosition().toShortString()
					+ " (era " + pe.toShortString() + ")");
			helper.assertTrue(Diretor.temZonaCalma(jogador), "a zona de calma deveria estar valendo");
			level.setBlockAndUpdate(pe, Blocks.AIR.defaultBlockState());
		});
		helper.runAfterDelay(60, () -> {
			helper.assertFalse(Diretor.temZonaCalma(jogador), "sem a vela, a zona deveria ter acabado");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * As receitas carregam, e as que usam Cinza Pálida aparecem no livro de receitas de quem pega a primeira
	 * cinza: é assim que o jogador descobre o que dá para fazer, sem ler nada fora do jogo.
	 */
	@GameTest(maxTicks = 200)
	public void asReceitasAparecem(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		String[] nomes = {"lampiao_palido", "tigela_oferenda", "caixa_de_musica", "ossos_de_agouro", "vela_palida", "sino_oco",
				"fio_vigilia", "isca_palida", "caderno_vestigios"};
		for (String nome : nomes) {
			helper.assertTrue(server.getRecipeManager().byKey(receita(nome)).isPresent(), "a receita " + nome + " não carregou");
			helper.assertTrue(server.getAdvancements().get(Identifier.fromNamespaceAndPath("sussurros", "recipes/" + nome)) != null,
					"falta o desbloqueio da receita " + nome);
		}
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -900, 0);
		helper.assertFalse(jogador.getRecipeBook().contains(receita("lampiao_palido")), "não deveria conhecer a receita antes da cinza");
		helper.runAfterDelay(5, () -> jogador.getInventory().add(new ItemStack(ModItems.CINZA_PALIDA)));
		helper.succeedWhen(() -> {
			for (String nome : new String[] {"lampiao_palido", "tigela_oferenda", "ossos_de_agouro", "vela_palida"}) {
				helper.assertTrue(jogador.getRecipeBook().contains(receita(nome)), "com a cinza na mochila, deveria conhecer " + nome);
			}
			helper.assertFalse(jogador.getRecipeBook().contains(receita("caixa_de_musica")), "a receita da caixa só vem com a caixa");
			JogadorDeTeste.remover(jogador);
		});
	}

	/** Da fase 2 em diante, uma tocha fora da tela passa a queimar pálida só para ele. O mundo não muda. */
	@GameTest(maxTicks = 900)
	public void asChamasEmpalidecem(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1000, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		ServerLevel level = helper.getLevel();
		Vec3 olhar = jogador.getLookAngle();
		BlockPos atras = jogador.blockPosition().offset((int) Math.round(-olhar.x * 4), 0, (int) Math.round(-olhar.z * 4));
		level.setBlockAndUpdate(atras, Blocks.TORCH.defaultBlockState());
		helper.assertTrue(Diretor.miragensParaTeste(jogador, "CHAMA_PALIDA") == 0, "não deveria haver chama pálida antes da hora");
		helper.succeedWhen(() -> {
			helper.assertTrue(Diretor.miragensParaTeste(jogador, "CHAMA_PALIDA") == 1, "a tocha atrás dele deveria ter empalidecido");
			helper.assertTrue(level.getBlockState(atras).is(Blocks.TORCH), "no mundo de verdade a tocha continua a mesma");
			level.removeBlock(atras, false);
			JogadorDeTeste.remover(jogador);
		});
	}

	private static ResourceKey<Recipe<?>> receita(String nome) {
		return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("sussurros", nome));
	}

	/** Uma criatura qualquer por perto não quebra nada do que foi posto: sanidade do que é só registro. */
	@GameTest
	public void osBlocosNovosExistem(GameTestHelper helper) {
		helper.assertTrue(ModBlocos.LAMPIAO_PALIDO.defaultBlockState().getValue(LampiaoPalidoBlock.COMBUSTIVEL) == 1, "lampião nasce com uma carga");
		helper.assertFalse(new ItemStack(ModItems.LAMPIAO_PALIDO).isEmpty(), "item do lampião");
		helper.assertFalse(new ItemStack(ModItems.TIGELA_OFERENDA).isEmpty(), "item da tigela");
		helper.assertTrue(new ItemStack(ModItems.CAIXA_DE_MUSICA).getMaxDamage() == 12, "a caixa tem doze voltas de corda");
		helper.assertTrue(HospedeEntity.TICKS_FADE > 0, "constante da criatura");
		helper.succeed();
	}
}
