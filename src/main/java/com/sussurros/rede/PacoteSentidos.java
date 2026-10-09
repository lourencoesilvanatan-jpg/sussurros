package com.sussurros.rede;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.sussurros.Sussurros;

/**
 * O que o jogador deve SENTIR agora (servidor -> cliente, uma vez por segundo).
 *
 * O servidor nunca manda "desenhe isto". Manda quatro medidas de 0 a 1, e o cliente decide como elas
 * viram cor, borda escura, neblina e som. Assim o que era estado escondido do Diretor passa a ser percebido
 * sem nenhum número na tela.
 *
 * peso:    quanto a assombração já avançou (fase e obsessão). Anda devagar. Drena a cor do mundo.
 * vigia:   alguma coisa está olhando para ele agora, de fora da tela. Às vezes mente.
 * caca:    perseguição em curso; cresce com a proximidade.
 * neblina: quanto a neblina fecha.
 */
public record PacoteSentidos(float peso, float vigia, float caca, float neblina, int flags) implements CustomPacketPayload {
	/** O jogador está no Avesso: ambiente sonoro e cor próprios. */
	public static final int FLAG_AVESSO = 1;
	/** Janela em que os próprios passos do jogador ecoam, como se alguém pisasse junto. */
	public static final int FLAG_ECO_PASSO = 2;
	/** Corta a música do jogo enquanto valer. */
	public static final int FLAG_SEM_MUSICA = 4;
	/** O Véu: bichos, amigos e tudo o que é entidade deixam de aparecer. Só o Hóspede aparece. */
	public static final int FLAG_VEU = 8;

	public static final PacoteSentidos NEUTRO = new PacoteSentidos(0, 0, 0, 0, 0);

	public static final Type<PacoteSentidos> TIPO = new Type<>(Sussurros.id("sentidos"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PacoteSentidos> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, PacoteSentidos::peso,
			ByteBufCodecs.FLOAT, PacoteSentidos::vigia,
			ByteBufCodecs.FLOAT, PacoteSentidos::caca,
			ByteBufCodecs.FLOAT, PacoteSentidos::neblina,
			ByteBufCodecs.VAR_INT, PacoteSentidos::flags,
			PacoteSentidos::new);

	public boolean tem(int flag) {
		return (this.flags & flag) != 0;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TIPO;
	}
}
