package com.sussurros.rede;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.sussurros.assombracao.Percepcao;

/** Os pacotes próprios do mod. O mod funciona sem eles (cliente sem o mod não recebe nada), só fica mais pobre. */
public final class Rede {
	private Rede() {
	}

	public static void inicializar() {
		PayloadTypeRegistry.clientboundPlay().register(PacoteSentidos.TIPO, PacoteSentidos.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PacoteEfeito.TIPO, PacoteEfeito.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PacoteCampo.TIPO, PacoteCampo.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PacoteCampo.TIPO, (pacote, contexto) ->
				Percepcao.definirCampo(contexto.player(), pacote.fov(), pacote.proporcao()));
	}

	/** Só envia a quem declarou que entende o pacote. Jogador de teste e cliente sem o mod ficam de fora. */
	public static void enviar(ServerPlayer p, CustomPacketPayload pacote) {
		if (ServerPlayNetworking.canSend(p, pacote.type())) {
			ServerPlayNetworking.send(p, pacote);
		}
	}

	public static void efeito(ServerPlayer p, PacoteEfeito.Tipo tipo, int duracaoTicks, float forca) {
		enviar(p, PacoteEfeito.de(tipo, duracaoTicks, forca));
	}
}
