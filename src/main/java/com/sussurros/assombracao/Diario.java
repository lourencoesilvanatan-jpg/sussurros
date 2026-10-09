package com.sussurros.assombracao;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * O diário de quem morou neste mundo antes de você.
 * O texto de cada página está nos arquivos de tradução (lang/pt_br.json).
 */
public final class Diario {
	public static final int TOTAL_PAGINAS = 25;

	private Diario() {
	}

	/** Mostra a próxima página. Retorna true se uma página foi lida (e deve ser consumida). */
	public static boolean lerProxima(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		int lidas = m.get(Memoria.PAGINAS_LIDAS);

		if (lidas >= TOTAL_PAGINAS) {
			p.sendOverlayMessage(Component.translatable("message.sussurros.diario.em_branco").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			return false;
		}

		p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);

		p.sendSystemMessage(Component.literal(""));
		p.sendSystemMessage(Component.translatable("diario.sussurros.cabecalho", lidas + 1, TOTAL_PAGINAS)
				.withStyle(ChatFormatting.DARK_GRAY));
		p.sendSystemMessage(Component.translatable("diario.sussurros.pagina." + lidas, p.getName())
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		p.sendSystemMessage(Component.literal(""));

		m.add(Memoria.PAGINAS_LIDAS, 1);
		// Ler sobre ele... atrai a atenção dele.
		m.add(Memoria.TEMPO, 60);
		m.salvar();
		return true;
	}
}
