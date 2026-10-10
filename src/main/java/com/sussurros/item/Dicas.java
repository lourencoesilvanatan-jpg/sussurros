package com.sussurros.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * As dicas dos itens (0.9.0-alpha14).
 *
 * Até a alpha13 cada item tinha só uma frase de clima ("Não tem badalo. Mesmo assim, alguma coisa responde."),
 * e quem explicava o uso era uma página do diário que podia demorar dezenas de páginas. O dono do mod jogou
 * uma hora e disse que recebia itens "no qual você não sabe exatamente como usar".
 *
 * Agora a dica tem até três linhas: a de clima, que continua; uma que diz como se usa; e, nos itens que somam
 * na Conta, uma que admite que há preço, sem dizer qual. A linha do preço é a mesma em todos, de propósito:
 * é o que deixa o jogador perceber que eles têm algo em comum.
 *
 * O princípio: esconder as regras do monstro, nunca os verbos do jogador.
 */
final class Dicas {
	private Dicas() {
	}

	static void acrescentar(Consumer<Component> texto, String item, boolean temPreco) {
		texto.accept(Component.translatable("itemTooltip.sussurros." + item).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		texto.accept(Component.translatable("itemTooltip.sussurros." + item + ".uso").withStyle(ChatFormatting.GRAY));
		if (temPreco) {
			texto.accept(Component.translatable("itemTooltip.sussurros.preco").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
	}
}
