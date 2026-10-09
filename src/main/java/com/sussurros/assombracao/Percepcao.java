package com.sussurros.assombracao;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;

/**
 * O que cabe na tela de cada jogador.
 *
 * Antes o servidor supunha FOV 70 em 16:9 para todo mundo (os cones fixos do HospedeEntity). Com FOV 90,
 * com FOV 110 ou correndo, a tela é mais larga que isso, e a criatura podia nascer dentro dela. Agora o
 * cliente informa o campo de visão de verdade, e os cones são calculados para aquele jogador. Sem informação
 * recente (cliente sem o mod, jogador de teste), valem os cones antigos.
 */
public final class Percepcao {
	private record Campo(double conePercebeu, double coneSeguro, long tick) {
	}

	/** Depois de tanto tempo sem notícia do cliente, o campo informado deixa de valer (ticks). */
	private static final long VALIDADE = 20L * 30L;

	private static final Map<UUID, Campo> CAMPOS = new HashMap<>();

	private Percepcao() {
	}

	public static void definirCampo(ServerPlayer p, float fovGraus, float proporcao) {
		if (!(fovGraus >= 20 && fovGraus <= 170) || !(proporcao >= 0.3 && proporcao <= 6)) {
			return; // valor absurdo: fica com o que já tinha
		}
		double[] cones = cones(fovGraus, proporcao);
		CAMPOS.put(p.getUUID(), new Campo(cones[0], cones[1], p.level().getGameTime()));
	}

	/**
	 * Devolve {conePercebeu, coneSeguro} (cossenos) para um FOV vertical e uma proporção de janela.
	 * "Percebeu" é um pouco para dentro da borda lateral; "seguro" passa do canto da tela, com folga.
	 */
	static double[] cones(double fovGraus, double proporcao) {
		double tanV = Math.tan(Math.toRadians(fovGraus / 2.0));
		double tanH = tanV * proporcao;
		double meioH = Math.atan(tanH);
		double canto = Math.atan(Math.hypot(tanV, tanH));
		double percebeu = Math.cos(meioH * 0.92);
		double seguro = Math.cos(Math.min(canto + Math.toRadians(6.0), Math.toRadians(86.0)));
		return new double[] {percebeu, seguro};
	}

	/** Cosseno do cone em que algo "está na tela" deste jogador. */
	public static double conePercebeu(ServerPlayer p) {
		Campo c = valido(p);
		return c != null ? c.conePercebeu() : HospedeEntity.CONE_PERCEBEU;
	}

	/** Cosseno do cone fora do qual é seguro fazer algo nascer sem o jogador ver. Nunca mais estreito que o antigo. */
	public static double coneSeguro(ServerPlayer p) {
		Campo c = valido(p);
		return c != null ? Math.min(c.coneSeguro(), HospedeEntity.CONE_TELA_SEGURA) : HospedeEntity.CONE_TELA_SEGURA;
	}

	@Nullable
	private static Campo valido(ServerPlayer p) {
		Campo c = CAMPOS.get(p.getUUID());
		if (c == null) {
			return null;
		}
		return p.level().getGameTime() - c.tick() <= VALIDADE ? c : null;
	}

	/**
	 * Dá para enxergar a entidade daqui? Diferente do hasLineOfSight do jogo, vidro não tampa (o jogador vê
	 * pela janela), e basta uma de três alturas do corpo aparecer: cabeça, tronco ou pernas.
	 */
	public static boolean linhaDeVisao(ServerPlayer p, Entity alvo) {
		if (!(p.level() instanceof ServerLevel level) || alvo.level() != level) {
			return false;
		}
		Vec3 olho = p.getEyePosition();
		double altura = alvo.getBbHeight();
		for (double fracao : new double[] {0.9, 0.55, 0.15}) {
			Vec3 ponto = alvo.position().add(0, altura * fracao, 0);
			HitResult r = level.clip(new ClipContext(olho, ponto, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
			if (r.getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	static void esquecer(UUID jogador) {
		CAMPOS.remove(jogador);
	}

	static void limpar() {
		CAMPOS.clear();
	}
}
