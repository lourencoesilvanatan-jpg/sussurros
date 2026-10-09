package com.sussurros.rede;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.sussurros.Sussurros;

/**
 * Um efeito pontual na tela de um jogador (servidor -> cliente).
 *
 * tipo: um dos valores de {@link Tipo}, pelo ordinal. duracao: em ticks. forca: de 0 a 1.
 */
public record PacoteEfeito(int tipo, int duracao, float forca) implements CustomPacketPayload {
	public enum Tipo {
		/** A tela fecha e abre rápido, como um piscar de olhos que o jogador não escolheu. */
		PISCAR,
		/** A tela escurece até o preto e fica assim pela duração. */
		APAGAO,
		/** A tela abre do preto, devagar. */
		ACORDAR,
		/** A câmera treme de leve. */
		TREMOR
	}

	public static final Type<PacoteEfeito> TIPO = new Type<>(Sussurros.id("efeito"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PacoteEfeito> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PacoteEfeito::tipo,
			ByteBufCodecs.VAR_INT, PacoteEfeito::duracao,
			ByteBufCodecs.FLOAT, PacoteEfeito::forca,
			PacoteEfeito::new);

	public static PacoteEfeito de(Tipo tipo, int duracao, float forca) {
		return new PacoteEfeito(tipo.ordinal(), duracao, forca);
	}

	public Tipo qual() {
		Tipo[] todos = Tipo.values();
		return todos[Math.max(0, Math.min(todos.length - 1, this.tipo))];
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TIPO;
	}
}
