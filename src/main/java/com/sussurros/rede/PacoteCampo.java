package com.sussurros.rede;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.sussurros.Sussurros;

/**
 * O campo de visão de verdade do jogador (cliente -> servidor): o FOV vertical efetivo, em graus (já com o
 * efeito de correr), e a proporção da janela. Com isso o servidor sabe o que cabe na tela dele em vez de
 * supor FOV 70 em 16:9.
 */
public record PacoteCampo(float fov, float proporcao) implements CustomPacketPayload {
	public static final Type<PacoteCampo> TIPO = new Type<>(Sussurros.id("campo"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PacoteCampo> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, PacoteCampo::fov,
			ByteBufCodecs.FLOAT, PacoteCampo::proporcao,
			PacoteCampo::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TIPO;
	}
}
