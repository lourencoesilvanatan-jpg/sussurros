package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;

import com.sussurros.Sussurros;
import com.sussurros.registro.ModItems;

/**
 * O que o jogo ensina pelo que o jogador já teve na mão (0.9.0-alpha14).
 *
 * Duas coisas chegavam fora de ordem. As páginas do diário saíam em sequência fixa, e a que explica a Linha de
 * Cinza, a Tigela e a Caixa eram as três últimas de 28. E todas as receitas com Cinza Pálida apareciam juntas
 * no livro, com a primeira cinza: oito itens de uma vez, antes de o jogador saber do que eles tratam.
 *
 * Agora:
 *   - na primeira vez em que ele tem um item na mochila, a página daquele item é pedida ao diário e uma Página
 *     Rasgada é deixada para ele, do jeito de sempre (passos atrás, a página no chão), fora de qualquer aparição;
 *   - as receitas chegam uma de cada vez: a de perguntar com a primeira cinza, a de afastar depois de a de
 *     perguntar ter respondido, e as outras quando ele acha o item num lugar, ou na última fase.
 *
 * As regras da criatura continuam escondidas. O que deixa de ficar escondido são os verbos do jogador.
 */
public final class Ensino {
	/** Cada item que tem uma página do diário só para ele. O número é o da página, contando do zero. */
	enum Licao {
		CINZA(9), SINO(10), VELA(4), OLHO(5), FIO(11), ISCA(14), TIGELA(26), CAIXA(27);

		final int pagina;

		Licao(int pagina) {
			this.pagina = pagina;
		}

		Item item() {
			return switch (this) {
				case CINZA -> ModItems.CINZA_PALIDA;
				case SINO -> ModItems.SINO_OCO;
				case VELA -> ModItems.VELA_PALIDA;
				case OLHO -> ModItems.OLHO_SUSSURRANTE;
				case FIO -> ModItems.FIO_VIGILIA;
				case ISCA -> ModItems.ISCA_PALIDA;
				case TIGELA -> ModItems.TIGELA_OFERENDA;
				case CAIXA -> ModItems.CAIXA_DE_MUSICA;
			};
		}

		String chave() {
			return "teve_" + this.name().toLowerCase(Locale.ROOT);
		}
	}

	/** A Linha de Cinza não é um item à parte: a página dela vem na primeira vez em que ele risca uma. */
	private static final String RISCOU_LINHA = "teve_linha";
	private static final int PAGINA_DA_LINHA = 25;

	/** A página chega um pouco depois de ele pegar o item, nunca na mesma hora. */
	private static final int ESPERA_DA_PAGINA = 20;

	private Ensino() {
	}

	/** Uma vez por segundo, dentro do tick do Diretor (recebe a Memoria do tick). */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick) {
		for (Licao licao : Licao.values()) {
			if (m.get(licao.chave()) == 1) {
				continue;
			}
			Item item = licao.item();
			if (!p.getInventory().contains(pilha -> pilha.is(item))) {
				continue;
			}
			m.set(licao.chave(), 1);
			boolean nova = !Diario.foiLida(m, licao.pagina);
			Diario.pedir(m, licao.pagina);
			if (nova && e.paginaDeItemApos < 0) {
				e.paginaDeItemApos = seg + ESPERA_DA_PAGINA;
			}
			Depuracao.log(p, seg, "ENSINO primeiro=" + licao + " pagina=" + (licao.pagina + 1) + " nova=" + (nova ? "sim" : "nao"));
		}
		entregarPagina(level, p, m, e, seg, tick);
	}

	/** A página pedida chega fora de qualquer aparição, e só se ele não tiver uma página por ler na mochila. */
	private static void entregarPagina(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick) {
		if (e.paginaDeItemApos < 0 || seg < e.paginaDeItemApos) {
			return;
		}
		if (!Diario.temPedida(m)) {
			e.paginaDeItemApos = -1;
			return;
		}
		if ((e.criatura != null && !e.criatura.isRemoved()) || EstruturasSussurros.temCenaAtiva(e) || tick < e.veuAte
				|| Diretor.bloqueado(p, e, tick)) {
			return; // espera: explicar durante uma aparição é o pior momento
		}
		e.paginaDeItemApos = -1;
		if (p.getInventory().contains(pilha -> pilha.is(ModItems.PAGINA_RASGADA))) {
			Depuracao.log(p, seg, "ENSINO pagina pedida: ele já tem uma página por ler");
			return;
		}
		m.add(Memoria.PAGINAS_ENTREGUES, 1);
		Diretor.deixarPagina(level, p);
		Depuracao.log(p, seg, "ENSINO pagina entregue proxima=" + (Diario.proxima(m) + 1));
	}

	/** Ele riscou uma Linha de Cinza. Chamado do uso do item, fora do tick do Diretor. */
	public static void aoRiscarLinha(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		if (m.get(RISCOU_LINHA) == 1) {
			return;
		}
		m.set(RISCOU_LINHA, 1);
		boolean nova = !Diario.foiLida(m, PAGINA_DA_LINHA);
		Diario.pedir(m, PAGINA_DA_LINHA);
		m.salvar();
		EstadoJogador e = Diretor.estadoParaTeste(p);
		long seg = p.level().getGameTime() / 20;
		if (nova && e.paginaDeItemApos < 0) {
			e.paginaDeItemApos = seg + ESPERA_DA_PAGINA;
		}
		Depuracao.log(p, seg, "ENSINO primeiro=LINHA pagina=" + (PAGINA_DA_LINHA + 1) + " nova=" + (nova ? "sim" : "nao"));
	}

	// ----- Receitas: uma de cada vez -----

	/** A de perguntar respondeu de verdade pela primeira vez: agora chega a de afastar. */
	static void aoSinoResponder(ServerPlayer p) {
		liberar(p, "vela_palida");
	}

	/** Na passagem de fase, o que ainda não chegou por outro caminho. */
	static void naFase(ServerPlayer p, int fase) {
		if (fase >= 2) {
			// Quem nunca fez a de perguntar não fica sem a de afastar.
			liberar(p, "vela_palida");
		}
		if (fase >= 4) {
			liberar(p, "fio_vigilia", "isca_palida", "lampiao_palido", "tigela_oferenda", "ossos_de_agouro");
		}
	}

	/** Alguma coisa voltou pela primeira vez: o caderno é onde isso fica anotado. */
	static void aoCobrar(ServerPlayer p) {
		liberar(p, "caderno_vestigios");
	}

	private static void liberar(ServerPlayer p, String... nomes) {
		List<ResourceKey<Recipe<?>>> chaves = new ArrayList<>();
		for (String nome : nomes) {
			ResourceKey<Recipe<?>> chave = ResourceKey.create(Registries.RECIPE, Sussurros.id(nome));
			if (!p.getRecipeBook().contains(chave)) {
				chaves.add(chave);
			}
		}
		if (!chaves.isEmpty()) {
			p.awardRecipesByKey(chaves);
			Depuracao.log(p, p.level().getGameTime() / 20, "ENSINO receitas=" + String.join(",", nomes));
		}
	}
}
