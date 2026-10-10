package com.sussurros.teste;

import java.util.List;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Atencao;
import com.sussurros.assombracao.Conta;
import com.sussurros.assombracao.Diario;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.assombracao.PrimeiroContato;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModItems;

/**
 * A primeira hora (0.9.0-alpha14): o primeiro contato garantido, o primeiro uso verdadeiro do sino, a página
 * que chega com o item, as receitas uma de cada vez e a Conta com carência, marcador e recibo.
 *
 * Conferem as regras contra um jogador de mentira. Não dizem se assusta nem se o dono entende os itens.
 */
public class TestesDaPrimeiraHora {
	private static ResourceKey<Recipe<?>> receita(String nome) {
		return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("sussurros", nome));
	}

	private static List<ItemEntity> noChao(ServerLevel level, Vec3 centro, double raio, net.minecraft.world.item.Item item) {
		return level.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(centro, raio * 2, 16, raio * 2), i -> i.getItem().is(item));
	}

	/** Antes dos quinze minutos de jogo, nada. */
	@GameTest(maxTicks = 200)
	public void antesDaJanelaNaoHaContato(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4400, 0);
		Diretor.esquecer(jogador);
		PrimeiroContato.relogioParaTeste(jogador, 60);
		helper.runAfterDelay(120, () -> {
			helper.assertTrue(Diretor.criatura(jogador) == null, "com um minuto de jogo ele não deveria aparecer");
			helper.assertFalse(PrimeiroContato.feitoParaTeste(jogador), "o contato não pode estar feito");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Passado o prazo, ele aparece a qualquer hora. Enquanto não é olhado, o contato não conta; a mira que passa
	 * por ele numa virada de câmera também não conta, nem o manda embora. Olhado por um segundo, dissolve, e
	 * ficam a cinza e o vestígio. O sino tocado ali responde de verdade, e só então a receita da vela aparece.
	 */
	@GameTest(maxTicks = 1600)
	public void oContatoSoContaQuandoEVisto(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4500, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "contato");
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		PrimeiroContato.relogioParaTeste(jogador, PRAZO);
		Vec3[] onde = new Vec3[1];
		int[] passo = {0};
		int[] espera = {0};
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					// Espera ele nascer. Nasce fora da tela: ainda não foi visto.
					if (h != null && h.ehContato()) {
						onde[0] = h.position();
						helper.assertFalse(PrimeiroContato.feitoParaTeste(jogador), "nascer não é ser visto");
						helper.assertFalse(jogador.getRecipeBook().contains(receita("sino_oco")), "sem cinza, sem receita do sino");
						passo[0] = 1;
						espera[0] = 30;
					}
				}
				case 1 -> {
					// Um segundo e meio sem olhar: ele continua lá e o contato continua devendo.
					if (--espera[0] <= 0) {
						helper.assertTrue(h != null && !h.isRemoved(), "sem ser olhado, ele deveria continuar lá");
						helper.assertFalse(PrimeiroContato.feitoParaTeste(jogador), "sem ser olhado, o contato não conta");
						passo[0] = 5;
						espera[0] = 14;
					}
				}
				case 5 -> {
					// Uma virada de câmera: a mira passa por ele durante dois ticks e segue para o outro lado.
					// Ele não some (sumia, pelo ramo do "tímido") e o contato não conta (contava).
					helper.assertTrue(h != null && !h.isRemoved() && !h.isSumindo(), "uma virada de câmera não deveria mandá-lo embora");
					Vec3 olho = jogador.getEyePosition();
					Vec3 ate = h.getEyePosition().subtract(olho);
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, espera[0] > 12 ? olho.add(ate) : olho.subtract(ate));
					if (--espera[0] <= 0) {
						helper.assertFalse(PrimeiroContato.feitoParaTeste(jogador), "a mira que só passou por ele não é tê-lo visto");
						passo[0] = 2;
					}
				}
				case 2 -> {
					// Agora o jogador vira e olha direto para ele, até ele sumir.
					if (h != null && !h.isRemoved() && !h.isSumindo()) {
						jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
					} else if (PrimeiroContato.feitoParaTeste(jogador)) {
						helper.assertTrue(noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).size() == 1,
								"onde ele estava deveria haver uma Cinza Pálida, há " + noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).size());
						helper.assertTrue(Diretor.vestigiosParaTeste(jogador) == 1, "o lugar deveria ficar marcado");
						// O jogador pega a cinza: é dela que nasce a primeira ferramenta.
						noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).forEach(ItemEntity::discard);
						jogador.getInventory().add(new ItemStack(ModItems.CINZA_PALIDA));
						passo[0] = 3;
						espera[0] = 200;
					}
				}
				case 3 -> {
					// O livro de receitas é avisado pelo próprio jogo, uns ticks depois de a cinza entrar na mochila.
					if (--espera[0] <= 0) {
						helper.fail("com a primeira cinza deveria vir a receita do sino");
					}
					if (jogador.getRecipeBook().contains(receita("sino_oco"))) {
						for (String nome : new String[] {"vela_palida", "fio_vigilia", "isca_palida", "lampiao_palido", "tigela_oferenda", "ossos_de_agouro", "caderno_vestigios"}) {
							helper.assertFalse(jogador.getRecipeBook().contains(receita(nome)), "a receita " + nome + " não deveria vir junto com a cinza");
						}
						// O primeiro toque do sino, perto de onde ele esteve.
						Diretor.usarSino(jogador);
						passo[0] = 4;
						espera[0] = 80;
					}
				}
				case 4 -> {
					if (--espera[0] <= 0) {
						helper.assertTrue(Memoria.de(jogador).get(Memoria.SINO_RESPONDEU) == 1, "com um vestígio por perto, o primeiro toque deveria ser respondido de verdade");
						helper.assertTrue(jogador.getRecipeBook().contains(receita("vela_palida")), "depois de o sino responder, vem a receita da vela");
						JogadorDeTeste.remover(jogador);
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	private static final int PRAZO = 25 * 60;

	/**
	 * Ele apareceu e ninguém olhou: não fica cinza, o contato não conta, a tentativa não gasta atenção e ele não
	 * volta na mesma hora. A tentativa seguinte existe, espera o Diretor sair do recuo e vem sem o aviso inteiro.
	 */
	@GameTest(maxTicks = 900)
	public void oContatoQueNinguemViuContinuaDevendo(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4600, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "contato-nao-visto");
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		PrimeiroContato.relogioParaTeste(jogador, PRAZO);
		// Na fase 0 o saldo do Diretor não volta nem é gasto por mais nada: qualquer diferença é da tentativa.
		double saldoAntes = Atencao.saldoParaTeste(jogador);
		int[] passo = {0};
		int[] espera = {0};
		Vec3[] onde = new Vec3[1];
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					if (h != null && h.ehContato() && h.tickCount > 20) {
						helper.assertTrue(PrimeiroContato.avisoAtivoParaTeste(jogador), "a primeira tentativa traz o aviso inteiro");
						onde[0] = h.position();
						// O tempo dele acabou sem ninguém ter olhado.
						h.sumir(level, false, "TEMPO_ESGOTADO");
						passo[0] = 1;
						espera[0] = 100;
					}
				}
				case 1 -> {
					if (--espera[0] <= 0) {
						helper.assertFalse(PrimeiroContato.feitoParaTeste(jogador), "sem ter sido visto, o contato continua devendo");
						helper.assertTrue(noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).isEmpty(), "sem ter sido visto, ele não deixa cinza");
						helper.assertTrue(Diretor.criatura(jogador) == null, "a nova tentativa não deveria vir em seguida");
						helper.assertTrue(Memoria.de(jogador).get("contato_tentativas") == 1, "deveria contar uma tentativa");
						helper.assertTrue(Atencao.saldoParaTeste(jogador) == saldoAntes, "a tentativa que ninguém viu não gasta atenção: de "
								+ saldoAntes + " foi para " + Atencao.saldoParaTeste(jogador));
						// A hora da tentativa seguinte chegou, mas o Diretor está recuando: ela espera.
						PrimeiroContato.recuarParaTeste(jogador, true);
						PrimeiroContato.liberarTentativaParaTeste(jogador);
						passo[0] = 2;
						// Tempo de sobra para o aviso da primeira tentativa (dez segundos) ter acabado.
						espera[0] = 160;
					}
				}
				case 2 -> {
					helper.assertTrue(h == null, "com o Diretor recuando, a repetição do contato deveria esperar");
					if (--espera[0] <= 0) {
						helper.assertTrue(Memoria.de(jogador).get("contato_tentativas") == 1, "recuando, não deveria haver outra tentativa");
						PrimeiroContato.recuarParaTeste(jogador, false);
						PrimeiroContato.liberarTentativaParaTeste(jogador);
						passo[0] = 3;
						espera[0] = 200;
					}
				}
				case 3 -> {
					if (--espera[0] <= 0) {
						helper.fail("fora do recuo, a segunda tentativa deveria ter vindo");
					}
					if (h != null && h.ehContato()) {
						helper.assertTrue(Memoria.de(jogador).get("contato_tentativas") == 2, "deveria ser a segunda tentativa");
						helper.assertFalse(PrimeiroContato.avisoAtivoParaTeste(jogador), "a segunda tentativa não repete o aviso inteiro");
						helper.assertTrue(Atencao.saldoParaTeste(jogador) == saldoAntes, "a segunda tentativa também não gasta atenção");
						h.sumir(level, false, "TEMPO_ESGOTADO");
						JogadorDeTeste.remover(jogador);
						passo[0] = 4;
						helper.succeed();
					}
				}
				default -> {
				}
			}
		});
	}

	/**
	 * Ele ficou no canto da tela até dissolver, e o jogador nunca pôs a mira nele: isso não é tê-lo visto.
	 * Não fica cinza e o contato continua devendo. (Até a alpha14 contava, e a garantia era gasta à toa.)
	 */
	@GameTest(maxTicks = 600)
	public void oContatoDeCantoDeOlhoNaoConta(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2700, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "contato-de-canto");
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		PrimeiroContato.relogioParaTeste(jogador, PRAZO);
		int[] passo = {0};
		int[] espera = {0};
		Vec3[] onde = new Vec3[1];
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					if (h != null && h.ehContato()) {
						onde[0] = h.position();
						passo[0] = 1;
						espera[0] = 200;
					}
				}
				case 1 -> {
					if (--espera[0] <= 0) {
						helper.fail("no canto da tela por dois segundos ele deveria ter dissolvido");
					}
					if (h == null || h.isRemoved()) {
						passo[0] = 2;
						// Tempo para o Diretor, que roda uma vez por segundo, gravar o que tiver de gravar.
						espera[0] = 60;
						return;
					}
					// Trinta graus para o lado dele: dentro da tela, fora da mira.
					Vec3 olho = jogador.getEyePosition();
					Vec3 ate = onde[0].add(0, h.getBbHeight() * 0.6, 0).subtract(olho);
					double a = Math.toRadians(30);
					Vec3 doLado = new Vec3(ate.x * Math.cos(a) - ate.z * Math.sin(a), ate.y, ate.x * Math.sin(a) + ate.z * Math.cos(a));
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, olho.add(doLado));
					helper.assertFalse(h.jaFoiEncarado(), "o teste deveria mantê-lo fora da mira");
				}
				case 2 -> {
					if (--espera[0] <= 0) {
						helper.assertFalse(PrimeiroContato.feitoParaTeste(jogador), "de canto de olho não é ter visto: o contato continua devendo");
						helper.assertTrue(noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).isEmpty(), "sem ter sido visto, ele não deixa cinza");
						helper.assertTrue(Memoria.de(jogador).get(Memoria.VEZES_VISTO) == 0, "não deveria contar como uma vez em que ele foi visto");
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

	/** Sem nada por perto e sem nunca ter respondido, o sino diz a verdade: silêncio. Não inventa uma resposta. */
	@GameTest(maxTicks = 300)
	public void oSinoNaoInventaResposta(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4700, 0);
		Diretor.esquecer(jogador);
		helper.runAfterDelay(60, () -> {
			JogadorDeTeste.sonsRecebidos(jogador);
			Diretor.usarSino(jogador);
		});
		helper.runAfterDelay(160, () -> {
			helper.assertTrue(Memoria.de(jogador).get(Memoria.SINO_RESPONDEU) == 0, "sem criatura e sem vestígio não há resposta de verdade");
			helper.assertFalse(jogador.getRecipeBook().contains(receita("vela_palida")), "a receita da vela espera o sino responder");
			// O toque do próprio sino são dois sons do mundo. Uma resposta inventada seria um terceiro.
			int sons = JogadorDeTeste.sonsRecebidos(jogador);
			helper.assertTrue(sons == 2, "só o toque do sino deveria ter soado (2 sons), foram " + sons);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Na primeira vez em que ele tem um item, a página daquele item é a próxima que o diário mostra, e uma página é deixada para ele. */
	@GameTest(maxTicks = 900)
	public void aPaginaChegaComOItem(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -4800, 0);
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		helper.runAfterDelay(10, () -> jogador.getInventory().add(new ItemStack(ModItems.SINO_OCO)));
		helper.runAfterDelay(60, () -> {
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("teve_sino") == 1, "deveria ter notado o sino na mochila");
			helper.assertTrue((m.get("paginas_pedidas") >> 10 & 1) == 1, "a página 11 (a do sino) deveria ter sido pedida");
		});
		helper.runAfterDelay(700, () -> {
			helper.assertTrue(noChao(level, jogador.position(), 6, ModItems.PAGINA_RASGADA).size() == 1, "uma página deveria ter sido deixada para ele");
			helper.assertTrue(Diario.lerProxima(jogador), "deveria haver página para ler");
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("paginas_mascara") == 1 << 10, "a primeira página lida deveria ser a 11, a do sino; máscara " + m.get("paginas_mascara"));
			helper.assertTrue(m.get(Memoria.PAGINAS_LIDAS) == 1, "uma página lida");
			// A leitura seguinte volta à ordem: a página 1.
			helper.assertTrue(Diario.lerProxima(jogador), "deveria haver outra página");
			helper.assertTrue(Memoria.de(jogador).get("paginas_mascara") == (1 << 10 | 1), "depois da pedida, a ordem volta ao começo");
			noChao(level, jogador.position(), 8, ModItems.PAGINA_RASGADA).forEach(ItemEntity::discard);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * Ele ficou um segundo e meio no canto da tela e o jogador pôs a mira nele bem na hora em que dissolvia:
	 * isso é tê-lo visto (a dissolução acontece no meio da tela), mesmo sem a mira ter completado os seus instantes.
	 */
	@GameTest(maxTicks = 600)
	public void oContatoVistoQuandoDissolveConta(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2900, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "contato-na-hora");
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		PrimeiroContato.relogioParaTeste(jogador, PRAZO);
		int[] passo = {0};
		int[] espera = {0};
		Vec3[] onde = new Vec3[1];
		helper.onEachTick(() -> {
			HospedeEntity h = Diretor.criatura(jogador);
			switch (passo[0]) {
				case 0 -> {
					if (h != null && h.ehContato()) {
						onde[0] = h.position();
						passo[0] = 1;
						espera[0] = 200;
					}
				}
				case 1 -> {
					if (--espera[0] <= 0) {
						helper.fail("olhado, ele deveria ter dissolvido");
					}
					if (h == null || h.isRemoved()) {
						passo[0] = 2;
						espera[0] = 60;
						return;
					}
					Vec3 olho = jogador.getEyePosition();
					Vec3 ate = onde[0].add(0, h.getBbHeight() * 0.6, 0).subtract(olho);
					// Trinta ticks no canto da tela (trinta graus para o lado), e só então a mira nele.
					double a = h.getTicksNaTela() < 30 ? Math.toRadians(30) : 0;
					Vec3 para = new Vec3(ate.x * Math.cos(a) - ate.z * Math.sin(a), ate.y, ate.x * Math.sin(a) + ate.z * Math.cos(a));
					jogador.lookAt(EntityAnchorArgument.Anchor.EYES, olho.add(para));
				}
				case 2 -> {
					if (--espera[0] <= 0) {
						helper.assertTrue(PrimeiroContato.feitoParaTeste(jogador), "com a mira nele quando dissolveu, o contato está feito");
						helper.assertTrue(noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).size() == 1, "onde ele estava deveria haver uma Cinza Pálida");
						noChao(level, onde[0], 3, ModItems.CINZA_PALIDA).forEach(ItemEntity::discard);
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
	 * Dois itens pegos juntos pedem duas páginas, e as duas chegam, uma de cada vez: enquanto a primeira está
	 * no chão ao lado dele não vem outra, e a segunda vem depois de ele ler a primeira. (Até a alpha14 só uma
	 * era deixada: quem pegava vários itens no mesmo baú ficava sem a explicação dos outros.)
	 */
	@GameTest(maxTicks = 2500)
	public void duasPaginasParaDoisItens(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2800, 0);
		Diretor.esquecer(jogador);
		ServerLevel level = helper.getLevel();
		helper.runAfterDelay(10, () -> {
			jogador.getInventory().add(new ItemStack(ModItems.SINO_OCO));
			jogador.getInventory().add(new ItemStack(ModItems.VELA_PALIDA));
		});
		// A primeira chega uns vinte segundos depois. Aos noventa segundos ela continua no chão ao lado dele, e
		// por isso a segunda ainda não veio (o intervalo entre as duas, de um minuto, já passou).
		helper.runAfterDelay(1800, () -> {
			helper.assertTrue(noChao(level, jogador.position(), 8, ModItems.PAGINA_RASGADA).size() == 1,
					"com uma página por pegar ao lado dele, não deveria haver outra; há " + noChao(level, jogador.position(), 8, ModItems.PAGINA_RASGADA).size());
			helper.assertTrue(Memoria.de(jogador).get(Memoria.PAGINAS_ENTREGUES) == 1, "uma página entregue até aqui");
			// Ele pega a página e lê: é a da vela (a pedida de número mais baixo).
			noChao(level, jogador.position(), 8, ModItems.PAGINA_RASGADA).forEach(ItemEntity::discard);
			helper.assertTrue(Diario.lerProxima(jogador), "deveria haver página para ler");
			helper.assertTrue(Memoria.de(jogador).get("paginas_mascara") == 1 << 4, "a primeira lida deveria ser a 5, a da vela; máscara "
					+ Memoria.de(jogador).get("paginas_mascara"));
		});
		helper.runAfterDelay(2400, () -> {
			helper.assertTrue(Memoria.de(jogador).get(Memoria.PAGINAS_ENTREGUES) == 2, "depois de ele ler a primeira, a segunda deveria ter sido deixada");
			helper.assertTrue(noChao(level, jogador.position(), 8, ModItems.PAGINA_RASGADA).size() == 1, "a segunda página deveria estar ao lado dele");
			helper.assertTrue(Diario.lerProxima(jogador), "deveria haver a segunda página para ler");
			helper.assertTrue(Memoria.de(jogador).get("paginas_mascara") == (1 << 4 | 1 << 10), "a segunda lida deveria ser a 11, a do sino; máscara "
					+ Memoria.de(jogador).get("paginas_mascara"));
			noChao(level, jogador.position(), 8, ModItems.PAGINA_RASGADA).forEach(ItemEntity::discard);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * A Conta: os três primeiros usos de um item não somam e não fazem som; do quarto em diante cada uso soma e
	 * faz o marcador. Ao estourar, a cobrança fica marcada no item e acontece no uso seguinte, com recibo.
	 */
	@GameTest(maxTicks = 400)
	public void aContaTemCarenciaMarcadorERecibo(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -1600, 0);
		Diretor.esquecer(jogador);
		helper.runAfterDelay(60, () -> {
			JogadorDeTeste.sonsRecebidos(jogador);
			for (int i = 0; i < Conta.CARENCIA; i++) {
				Conta.somar(jogador, Conta.Item.VELA);
			}
			helper.assertTrue(Memoria.de(jogador).get("conta") == 0, "na carência a conta não sobe");
		});
		// O pacote de som só chega à fila do jogador de mentira uns ticks depois de enviado.
		helper.runAfterDelay(66, () -> {
			int sons = JogadorDeTeste.sonsRecebidos(jogador);
			helper.assertTrue(sons == 0, "na carência não há marcador, vieram " + sons + " sons");
			Conta.somar(jogador, Conta.Item.VELA);
			helper.assertTrue(Memoria.de(jogador).get("conta") == 1, "o quarto uso soma");
		});
		helper.runAfterDelay(72, () -> {
			int sons = JogadorDeTeste.sonsRecebidos(jogador);
			helper.assertTrue(sons == 1, "o uso que soma faz o marcador, uma vez; vieram " + sons + " sons");
			for (int i = 0; i < 10; i++) {
				Conta.somar(jogador, Conta.Item.VELA);
			}
			helper.assertTrue(Memoria.de(jogador).get("conta") == 11, "onze usos fora da carência deveriam dar 11");
		});
		helper.runAfterDelay(120, () -> {
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("conta") == 0 && m.get("conta_avisos") == 0 && m.get("conta_vela") == 0, "ao estourar, tudo zera: " + m.get("conta"));
			helper.assertTrue(m.get("cobrado_vela") == 1, "a cobrança deveria ficar marcada para a próxima vela, na hora");
			helper.assertTrue(m.get("recibos") == 0, "ainda não foi cobrado");
			helper.assertFalse(jogador.getRecipeBook().contains(receita("caderno_vestigios")), "o caderno só vem depois da primeira cobrança");
			// A vela seguinte é a cobrança: consome a marca e deixa o recibo.
			Diretor.acenderVela(jogador);
			Memoria depois = Memoria.de(jogador);
			helper.assertTrue(depois.get("cobrado_vela") == 0, "a marca deveria ser consumida");
			helper.assertTrue(depois.get("recibos") == 1 && depois.get("recibo_vela") == 1, "deveria ficar o recibo da vela");
			helper.assertTrue(jogador.getRecipeBook().contains(receita("caderno_vestigios")), "com a primeira cobrança vem a receita do caderno");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
