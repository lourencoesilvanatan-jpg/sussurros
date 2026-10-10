package com.sussurros.assombracao;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * O diário de quem morou neste mundo antes de você.
 * O texto de cada página está no arquivo de textos (lang/en_us.json, que está em português de propósito).
 *
 * Até a alpha13 as páginas saíam em ordem fixa, qualquer que fosse a página achada: a que explica um item
 * podia chegar dezenas de páginas depois do item. Desde a alpha14 uma página pode ser PEDIDA (ver Ensino):
 * a próxima leitura mostra a pedida, e só depois volta à ordem. O cabeçalho mostra o número de verdade da
 * página, então quem lê a 11 antes da 2 vê que há páginas faltando.
 */
public final class Diario {
	public static final int TOTAL_PAGINAS = 28;

	/** Bit N ligado: a página N (contando do zero) já foi lida. */
	static final String MASCARA = "paginas_mascara";
	/** Bit N ligado: a página N foi pedida e ainda não foi lida; passa na frente da ordem. */
	static final String PEDIDAS = "paginas_pedidas";

	private static final int TODAS = (1 << TOTAL_PAGINAS) - 1;

	private Diario() {
	}

	/** As páginas já lidas, como máscara. Quem vem de uma versão anterior tinha só a contagem: leu as N primeiras. */
	static int lidas(Memoria m) {
		int mascara = m.get(MASCARA);
		int contagem = m.get(Memoria.PAGINAS_LIDAS);
		if (mascara == 0 && contagem > 0) {
			mascara = contagem >= TOTAL_PAGINAS ? TODAS : (1 << contagem) - 1;
			m.set(MASCARA, mascara);
		}
		return mascara & TODAS;
	}

	static boolean foiLida(Memoria m, int pagina) {
		return (lidas(m) >> pagina & 1) == 1;
	}

	/** Pede uma página: se ainda não foi lida, é ela que a próxima Página Rasgada mostra. */
	static void pedir(Memoria m, int pagina) {
		if (pagina < 0 || pagina >= TOTAL_PAGINAS || foiLida(m, pagina)) {
			return;
		}
		m.set(PEDIDAS, m.get(PEDIDAS) | (1 << pagina));
	}

	/** Há página pedida esperando para ser lida? */
	static boolean temPedida(Memoria m) {
		return quantasPedidas(m) != 0;
	}

	/** Quantas páginas pedidas esperam para ser lidas. */
	static int quantasPedidas(Memoria m) {
		return Integer.bitCount(m.get(PEDIDAS) & ~lidas(m) & TODAS);
	}

	/** A página que a próxima leitura mostra: a pedida mais antiga na ordem do diário, senão a primeira não lida. -1 se acabou. */
	static int proxima(Memoria m) {
		int lidas = lidas(m);
		int pedidas = m.get(PEDIDAS) & ~lidas & TODAS;
		if (pedidas != 0) {
			return Integer.numberOfTrailingZeros(pedidas);
		}
		int faltam = ~lidas & TODAS;
		return faltam == 0 ? -1 : Integer.numberOfTrailingZeros(faltam);
	}

	/** Mostra a próxima página. Retorna true se uma página foi lida (e deve ser consumida). */
	public static boolean lerProxima(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		int pagina = proxima(m);

		if (pagina < 0) {
			p.sendOverlayMessage(Component.translatable("message.sussurros.diario.em_branco").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return false;
		}

		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);

		p.sendSystemMessage(Component.literal(""));
		p.sendSystemMessage(Component.translatable("diario.sussurros.cabecalho", pagina + 1, TOTAL_PAGINAS)
				.withStyle(ChatFormatting.DARK_GRAY));
		p.sendSystemMessage(Component.translatable("diario.sussurros.pagina." + pagina, p.getName())
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		p.sendSystemMessage(Component.literal(""));

		int lidas = lidas(m) | (1 << pagina);
		m.set(MASCARA, lidas);
		m.set(PEDIDAS, m.get(PEDIDAS) & ~(1 << pagina));
		m.set(Memoria.PAGINAS_LIDAS, Integer.bitCount(lidas));
		// Ler sobre ele... atrai a atenção dele.
		m.add(Memoria.TEMPO, 60);
		m.salvar();
		Depuracao.log(p, p.level().getGameTime() / 20, "DIARIO pagina=" + (pagina + 1) + " lidas=" + Integer.bitCount(lidas));
		return true;
	}
}
