package com.sussurros.assombracao;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Progressão opcional: organiza o que o jogador já provou sem virar um radar ou HUD de debug. */
public final class ProgressoInvestigacao {
	private ProgressoInvestigacao() {
	}

	public static void usarCaderno(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		m.add(Memoria.CADERNO_USOS, 1);
		int vestigios = Vestigios.de(p).quantidade();
		int estruturas = m.get(Memoria.ESTRUTURA_MARCO_VISTA) + m.get(Memoria.ESTRUTURA_POSTO_VISTA) + m.get(Memoria.ESTRUTURA_NICHO_VISTA);
		int nivel = nivel(m, vestigios, estruturas);
		p.sendSystemMessage(Component.literal(""));
		p.sendSystemMessage(Component.translatable("message.sussurros.caderno.titulo").withStyle(ChatFormatting.DARK_GRAY));
		p.sendSystemMessage(Component.translatable("message.sussurros.caderno.nivel." + nivel).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		p.sendSystemMessage(Component.translatable("message.sussurros.caderno.resumo", vestigios, estruturas)
				.withStyle(ChatFormatting.DARK_GRAY));
		p.sendSystemMessage(Component.literal(""));
		m.salvar();
	}

	static int nivel(Memoria m, int vestigios, int estruturas) {
		if (vestigios >= 2 && estruturas >= 2 && (m.get(Memoria.ISCAS_ATENDIDAS) > 0 || m.get(Memoria.FIOS_ROMPIDOS) > 0)) {
			return 3;
		}
		if (estruturas >= 1 || m.get(Memoria.FIOS_ROMPIDOS) > 0 || m.get(Memoria.SINOS_USADOS) >= 2) {
			return 2;
		}
		if (vestigios > 0 || m.get(Memoria.CINZAS_GERADAS) > 0 || m.get(Memoria.VEZES_VISTO) > 0) {
			return 1;
		}
		return 0;
	}
}
