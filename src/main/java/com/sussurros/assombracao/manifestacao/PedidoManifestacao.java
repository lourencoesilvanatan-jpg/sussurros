package com.sussurros.assombracao.manifestacao;

import org.jspecify.annotations.Nullable;

import com.sussurros.assombracao.Evento;
import com.sussurros.entidade.HospedeEntity;

/**
 * Pedido tipado para uma manifestação do Hóspede.
 *
 * Agrupa a origem, o evento motivador e a nota de depuração que antes
 * trafegavam por campos temporários separados em EstadoJogador.
 */
public record PedidoManifestacao(
        HospedeEntity.Origem origem,
        @Nullable Evento evento,
        String nota) {

    public static PedidoManifestacao doDiretor(@Nullable Evento evento) {
        return new PedidoManifestacao(HospedeEntity.Origem.DIRETOR, evento, "");
    }

    public static PedidoManifestacao deComando(@Nullable Evento evento) {
        return new PedidoManifestacao(HospedeEntity.Origem.COMANDO, evento, "");
    }

    public static PedidoManifestacao deOrigem(HospedeEntity.Origem origem, @Nullable Evento evento) {
        return new PedidoManifestacao(origem, evento, "");
    }

    public PedidoManifestacao comNota(String novaNota) {
        return new PedidoManifestacao(this.origem, this.evento, novaNota);
    }

    public boolean ehTeste() {
        return this.origem == HospedeEntity.Origem.COMANDO;
    }
}