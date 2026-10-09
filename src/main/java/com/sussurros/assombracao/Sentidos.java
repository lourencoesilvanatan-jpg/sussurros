package com.sussurros.assombracao;

import net.minecraft.server.level.ServerPlayer;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteSentidos;
import com.sussurros.rede.Rede;

/**
 * Traduz o estado escondido do Diretor em quatro medidas que o cliente transforma em cor, borda escura,
 * neblina e som (ver {@link PacoteSentidos}). Roda uma vez por segundo, no fim do tick de cada jogador.
 *
 * Nada aqui decide o que acontece: é só apresentação. Por isso não usa o gerador de números aleatórios do
 * mundo (mudaria os sorteios do Diretor) e não entra em pressão, agenda nem aprendizado.
 */
final class Sentidos {
	/** Quanto cada fase pesa por si só. A obsessão soma até 0,34 em cima. */
	private static final float[] PESO_FASE = {0.0F, 0.10F, 0.28F, 0.48F, 0.66F};

	/** Além desta distância, uma criatura olhando não é sentida. */
	private static final double ALCANCE_VIGIA = 72.0;

	private Sentidos() {
	}

	static void atualizar(ServerPlayer p, EstadoJogador e, int fase, boolean calma, boolean noite, long tick) {
		PacoteSentidos pacote = calcular(p, e, fase, calma, noite, tick);
		e.sentidos = pacote;
		Rede.enviar(p, pacote);
	}

	static PacoteSentidos calcular(ServerPlayer p, EstadoJogador e, int fase, boolean calma, boolean noite, long tick) {
		if (e.sentidosForcados != null && tick < e.sentidosForcadosAte) {
			return e.sentidosForcados;
		}
		float peso = PESO_FASE[Math.max(0, Math.min(4, fase))] + 0.34F * (float) (e.obsessao / 100.0);
		if (calma) {
			peso *= 0.35F; // dentro da vela a cor volta: é o sinal de que ela funciona, e de que o mundo estava drenado
		} else if (e.contexto == ContextoMundo.Tipo.CASA && !noite) {
			peso *= 0.8F;
		}
		// Quando o Diretor recua (depois de um pico, de uma captura), o peso cai à metade: a cor volta um pouco e o
		// fundo grave quase some. O silêncio depois do susto só é silêncio se o fundo também calar.
		if (e.estado == EstadoDiretor.RECUANDO) {
			peso *= 0.5F;
		}

		float vigia = 0;
		float caca = 0;
		HospedeEntity h = e.criatura;
		if (h != null && !h.isRemoved() && !h.isSumindo()) {
			double dist = Math.sqrt(h.distanceToSqr(p));
			boolean naTela = Diretor.estaVendo(p, h, Percepcao.conePercebeu(p));
			if (!naTela && dist < ALCANCE_VIGIA && h.avisaVigia() && Percepcao.linhaDeVisao(p, h)) {
				vigia = (float) Diretor.limitar(1.05 - dist / ALCANCE_VIGIA, 0.25, 1.0);
			}
			// Longe, a caçada é quase só silêncio. O batimento é para quando ele está perto e sem parede no meio.
			caca = h.intensidadeDaCaca(p, dist);
		}
		if (tick < e.vigiaFalsaAte) {
			vigia = Math.max(vigia, e.vigiaFalsaForca);
		}
		if (tick < e.cacaAvisoAte) {
			caca = Math.max(caca, 0.15F);
		}

		float neblina = tick < e.neblinaAte ? e.neblinaForca : 0;

		int flags = 0;
		if (tick < e.ecoPassoAte) {
			flags |= PacoteSentidos.FLAG_ECO_PASSO;
		}
		if (tick < e.semMusicaAte) {
			flags |= PacoteSentidos.FLAG_SEM_MUSICA;
		}
		if (Veu.ativo(e, tick)) {
			peso = Math.max(peso, VEU_PESO);
			neblina = Math.max(neblina, VEU_NEBLINA);
			flags |= VEU_FLAGS;
		}
		return new PacoteSentidos(limitar01(peso), limitar01(vigia), limitar01(caca), limitar01(neblina), flags);
	}

	static final float VEU_PESO = 0.9F;
	static final float VEU_NEBLINA = 0.85F;
	static final int VEU_FLAGS = PacoteSentidos.FLAG_AVESSO | PacoteSentidos.FLAG_SEM_MUSICA | PacoteSentidos.FLAG_VEU;

	/**
	 * O Véu abre e fecha no meio de uma piscada, não no segundo cheio: reenvia já o último pacote, com ou sem
	 * as marcas do Véu. O cliente troca tudo de uma vez enquanto a tela está fechada.
	 */
	static void veuAgora(ServerPlayer p, EstadoJogador e, boolean aberto) {
		PacoteSentidos a = e.sentidos != null ? e.sentidos : PacoteSentidos.NEUTRO;
		e.sentidos = aberto
				? new PacoteSentidos(Math.max(a.peso(), VEU_PESO), a.vigia(), a.caca(), Math.max(a.neblina(), VEU_NEBLINA), a.flags() | VEU_FLAGS)
				: new PacoteSentidos(Math.min(a.peso(), 0.66F), a.vigia(), a.caca(), 0, a.flags() & ~VEU_FLAGS);
		Rede.enviar(p, e.sentidos);
	}

	/** Comando de teste: impõe os quatro valores por um tempo, para conferir cada efeito isolado. */
	static String forcar(ServerPlayer p, float peso, float vigia, float caca, float neblina, int flags, int segundos) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		long tick = p.level().getGameTime();
		e.sentidosForcados = new PacoteSentidos(limitar01(peso), limitar01(vigia), limitar01(caca), limitar01(neblina), flags);
		e.sentidosForcadosAte = tick + segundos * 20L;
		e.sentidos = e.sentidosForcados;
		Rede.enviar(p, e.sentidosForcados);
		return "Sentidos impostos por " + segundos + " s: peso=" + peso + " vigia=" + vigia + " caca=" + caca
				+ " neblina=" + neblina + " flags=" + flags + ".";
	}

	private static float limitar01(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}
}
