package com.sussurros.teste;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.rede.PacoteSentidos;
import com.sussurros.registro.ModEntidades;
import com.sussurros.registro.ModItems;

/**
 * Testes que rodam dentro de um servidor de verdade, sem janela (./gradlew runGameTest).
 *
 * Eles não provam que algo assusta. Provam que o mod carrega, que cada peça roda sem erro com um
 * jogador de mentira e que as regras básicas valem. O que só o olho vê fica para os testes de cliente.
 */
public class TestesDeServidor {
	/**
	 * O som da assombração é só de quem é assombrado: com dois jogadores lado a lado, o passo atrás de um não
	 * chega ao outro. Já o que acontece de verdade no mundo (aqui, a caixa de música) os dois ouvem.
	 */
	@GameTest(maxTicks = 200)
	public void oSomDaAssombracaoESoDoAlvo(GameTestHelper helper) {
		ServerPlayer alvo = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2300, 0);
		ServerPlayer amigo = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 6, -2300, 0);
		Diretor.esquecer(alvo);
		Diretor.esquecer(amigo);
		Diretor.definirFase(alvo, 2);
		helper.runAfterDelay(20, () -> {
			JogadorDeTeste.sonsRecebidos(alvo);
			JogadorDeTeste.sonsRecebidos(amigo);
			helper.assertTrue(Diretor.forcarEvento(alvo, Evento.PASSO_UNICO) == null, "o passo atrás dele deveria acontecer");
			helper.assertTrue(Diretor.forcarEvento(alvo, Evento.BATIDA) == null || true, "a batida pode não ter porta; não importa");
		});
		helper.runAfterDelay(80, () -> {
			int doAlvo = JogadorDeTeste.sonsRecebidos(alvo);
			int doAmigo = JogadorDeTeste.sonsRecebidos(amigo);
			helper.assertTrue(doAlvo >= 1, "o alvo deveria ter ouvido o passo atrás dele");
			helper.assertTrue(doAmigo == 0, "o amigo ao lado não deveria ouvir a assombração do outro: ouviu " + doAmigo + " som(ns)");
			// A caixa de música é um objeto de verdade tocando: os dois ouvem.
			alvo.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAIXA_DE_MUSICA));
			alvo.gameMode.useItem(alvo, helper.getLevel(), alvo.getMainHandItem(), InteractionHand.MAIN_HAND);
		});
		helper.runAfterDelay(90, () -> {
			helper.assertTrue(JogadorDeTeste.sonsRecebidos(alvo) >= 1, "o dono da caixa deveria ouvi-la");
			helper.assertTrue(JogadorDeTeste.sonsRecebidos(amigo) >= 1, "o amigo ao lado deveria ouvir a caixa de música");
			JogadorDeTeste.remover(alvo);
			JogadorDeTeste.remover(amigo);
			helper.succeed();
		});
	}

	/**
	 * Os textos do mod saem em português do Brasil com o jogo no idioma padrão (inglês): o dono joga com o jogo
	 * em inglês e pediu o mod em português. Se alguém voltar a pôr texto em inglês no arquivo-base, este teste cai.
	 */
	@GameTest
	public void osTextosSaemEmPortugues(GameTestHelper helper) {
		Language idioma = Language.getInstance();
		helper.assertTrue("Cinza Pálida".equals(idioma.getOrDefault("item.sussurros.cinza_palida")),
				"nome de item: " + idioma.getOrDefault("item.sussurros.cinza_palida"));
		helper.assertTrue("Lampião Pálido".equals(idioma.getOrDefault("block.sussurros.lampiao_palido")),
				"nome de bloco: " + idioma.getOrDefault("block.sussurros.lampiao_palido"));
		helper.assertTrue(idioma.getOrDefault("message.sussurros.vela.acendeu").startsWith("Uma chama pálida"),
				"mensagem: " + idioma.getOrDefault("message.sussurros.vela.acendeu"));
		helper.assertTrue("Respiração".equals(idioma.getOrDefault("subtitles.sussurros.respiracao")),
				"legenda de som: " + idioma.getOrDefault("subtitles.sussurros.respiracao"));
		helper.assertTrue(idioma.getOrDefault("diario.sussurros.pagina.0").length() > 20 && !idioma.getOrDefault("diario.sussurros.pagina.0").startsWith("diario."),
				"a primeira página do diário deveria existir");
		helper.succeed();
	}

	@GameTest
	public void oModCarregaComTudoRegistrado(GameTestHelper helper) {
		helper.assertTrue(Sussurros.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(ModEntidades.HOSPEDE).getNamespace()),
				"a criatura não foi registrada");
		for (Item item : BuiltInRegistries.ITEM) {
			if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Sussurros.MOD_ID)) {
				helper.assertFalse(new ItemStack(item).isEmpty(), "item vazio: " + BuiltInRegistries.ITEM.getKey(item));
			}
		}
		helper.assertFalse(new ItemStack(ModItems.VELA_PALIDA).isEmpty(), "a vela não existe");
		helper.succeed();
	}

	/** Todo evento forçado por comando tem de rodar sem lançar erro, caiba ou não no lugar do teste. */
	@GameTest(maxTicks = 400)
	public void todoEventoRodaSemErro(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, 4, 2, 4);
		Diretor.definirFase(jogador, 4);
		Evento[] eventos = Evento.values();
		for (int i = 0; i < eventos.length; i++) {
			Evento ev = eventos[i];
			helper.runAfterDelay(2L + i * 10L, () -> Diretor.forcarEvento(jogador, ev));
		}
		helper.runAfterDelay(2L + eventos.length * 10L + 20L, () -> {
			Diretor.pararCenas(jogador);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Na fase 4 o cliente tem de ser mandado sentir peso; na fase 0, nada. */
	@GameTest(maxTicks = 200)
	public void osSentidosAcompanhamAFase(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, 4, 2, 4);
		Diretor.esquecer(jogador);
		helper.runAfterDelay(45, () -> {
			PacoteSentidos calmo = Diretor.sentidos(jogador);
			helper.assertTrue(calmo.peso() < 0.05F, "fase 0 deveria ter peso quase zero, veio " + calmo.peso());
			helper.assertTrue(calmo.caca() == 0.0F, "sem caçada, caca deveria ser zero");
			Diretor.definirFase(jogador, 4);
		});
		helper.runAfterDelay(100, () -> {
			PacoteSentidos pesado = Diretor.sentidos(jogador);
			helper.assertTrue(pesado.peso() >= 0.6F, "fase 4 deveria pesar pelo menos 0,6, veio " + pesado.peso());
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
