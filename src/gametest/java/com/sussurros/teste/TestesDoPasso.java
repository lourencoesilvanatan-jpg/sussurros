package com.sussurros.teste;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Diretor;
import com.sussurros.entidade.HospedeEntity;

/**
 * O passo da caçada, medido (0.9.0-alpha29): quanto tempo ele fica parado no começo, quanto demora para sair do lugar
 * quando o jogador para de olhar, e como fica a distância de quem foge correndo. O dono, em 18/10/2026, depois de
 * duas caçadas: "ele não corre tanto parece, e ele parece meio travado até agir, como um delay".
 */
public class TestesDoPasso {
	@GameTest(maxTicks = 1200)
	public void oPassoDaCacada(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -10700, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		int[] relogio = new int[1];
		int[] criadoEm = {-1};
		int[] persegueEm = {-1};
		int[] virouEm = {-1};
		int[] andouEm = {-1};
		Vec3[] parado = new Vec3[1];
		Vec3[] rumo = new Vec3[1];
		List<String> distancias = new ArrayList<>();
		boolean[] acabou = new boolean[1];
		helper.runAfterDelay(5, () -> {
			helper.setTime(18000);
			for (int i = 0; i < 6 && !Diretor.cacadaParaTeste(jogador, false); i++) {
				// tenta de novo
			}
		});
		TestesDePerto.cadaTick(helper, () -> {
			if (acabou[0]) {
				return;
			}
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null) {
				if (virouEm[0] >= 0) {
					relatar(helper, jogador, criadoEm, persegueEm, virouEm, andouEm, distancias, "pego (ou sumiu)");
					acabou[0] = true;
				}
				return;
			}
			if (criadoEm[0] < 0) {
				criadoEm[0] = relogio[0];
			}
			if (persegueEm[0] < 0 && "PERSEGUE".equals(h.getEstagioDaCaca())) {
				persegueEm[0] = relogio[0];
			}
			if (persegueEm[0] < 0 || relogio[0] < persegueEm[0] + 60) {
				// No aviso e nos três primeiros segundos da perseguição, olhando para ele.
				jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
				parado[0] = h.position();
				return;
			}
			if (virouEm[0] < 0) {
				virouEm[0] = relogio[0];
				rumo[0] = jogador.position().subtract(h.position()).multiply(1, 0, 1).normalize();
			}
			// Correndo para longe dele, a 5,6 blocos por segundo (a corrida do jogo), olhando para a frente.
			Vec3 passo = rumo[0].scale(5.6 / 20.0);
			jogador.snapTo(jogador.getX() + passo.x, jogador.getY(), jogador.getZ() + passo.z, 0.0F, 0.0F);
			jogador.lookAt(EntityAnchorArgument.Anchor.EYES, jogador.getEyePosition().add(rumo[0].scale(10)));
			jogador.setSprinting(true);
			if (andouEm[0] < 0 && h.position().distanceTo(parado[0]) > 0.3) {
				andouEm[0] = relogio[0];
			}
			int desde = relogio[0] - virouEm[0];
			if (desde % 20 == 0) {
				distancias.add(String.format(Locale.ROOT, "%.1f", h.distanceTo(jogador)));
			}
			if (desde >= 200) {
				relatar(helper, jogador, criadoEm, persegueEm, virouEm, andouEm, distancias, "dez segundos correndo");
				h.sumir(helper.getLevel(), false, "FIM_DO_TESTE");
				acabou[0] = true;
			}
		});
	}

	/**
	 * O que o teste exige: o aviso de 4,5 a 7 segundos (desde a alpha29, de 5 a 6,5; eram 8 a 10) e ele saindo do lugar em
	 * até meio segundo depois que o jogador para de olhar. A distância de quem foge vai para o log e não é exigida: o
	 * jogador de mentira faz menos barulho que um de verdade correndo (o jogo não conta o movimento dele), então ele
	 * ouve menos e a medida sai pessimista. Para comparar uma versão com outra, serve.
	 */
	private static void relatar(GameTestHelper helper, ServerPlayer jogador, int[] criadoEm, int[] persegueEm, int[] virouEm, int[] andouEm,
			List<String> distancias, String fim) {
		double aviso = (persegueEm[0] - criadoEm[0]) / 20.0;
		double saiu = andouEm[0] < 0 ? -1.0 : (andouEm[0] - virouEm[0]) / 20.0;
		Sussurros.LOGGER.info(String.format(Locale.ROOT,
				"[passo] aviso=%.1fs saiu_do_lugar=%.2fs depois_de_virar distancia_a_cada_segundo=%s fim=%s", aviso, saiu, distancias, fim));
		helper.assertTrue(aviso >= 4.5 && aviso <= 7.0, "o aviso deveria durar de 5 a 6,5 s; durou " + aviso);
		helper.assertTrue(saiu >= 0 && saiu <= 0.5, "ele deveria sair do lugar logo que o jogador para de olhar; levou " + saiu + " s");
		JogadorDeTeste.remover(jogador);
		helper.succeed();
	}
}
