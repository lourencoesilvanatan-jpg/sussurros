package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Miragens: blocos que só um jogador vê.
 *
 * O servidor manda para um jogador um bloco que não existe. O mundo de verdade não muda: não há o que
 * restaurar ao fechar o servidor, a construção de ninguém é tocada e quem está ao lado não vê nada.
 *
 * Conferido em jogo em 08/10/2026 (/sussurros teste miragem): a tocha falsa ilumina no cliente, e quando o
 * jogador clica nela o próprio jogo reenvia o bloco de verdade e ela some. O mesmo acontece se o chunk for
 * recarregado. Faz parte do efeito, mas nenhuma mecânica pode depender de a miragem durar.
 */
final class Miragem {
	private record Ativa(UUID jogador, BlockPos pos, long expiraTick, double raioDesfaz, String motivo) {
	}

	private static final List<Ativa> ATIVAS = new ArrayList<>();

	private Miragem() {
	}

	/**
	 * Mostra o bloco "falso" na posição, só para este jogador.
	 * raioDesfaz: se o jogador chegar mais perto que isto, a miragem se desfaz (0 = só pelo tempo).
	 */
	static boolean mostrar(ServerLevel level, ServerPlayer p, BlockPos pos, BlockState falso, long duracaoTicks,
			double raioDesfaz, String motivo) {
		if (level.getBlockState(pos).equals(falso)) {
			return false;
		}
		p.connection.send(new ClientboundBlockUpdatePacket(pos, falso));
		ATIVAS.add(new Ativa(p.getUUID(), pos.immutable(), level.getGameTime() + Math.max(1, duracaoTicks), raioDesfaz, motivo));
		return true;
	}

	/** Desfaz o que venceu ou aquilo de que o jogador chegou perto. Desfazer é reenviar o bloco de verdade. */
	static void tick(ServerLevel level, long tick) {
		if (ATIVAS.isEmpty() || tick % 5 != 0) {
			return;
		}
		Iterator<Ativa> it = ATIVAS.iterator();
		while (it.hasNext()) {
			Ativa a = it.next();
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(a.jogador());
			if (p == null || p.hasDisconnected() || p.level() != level) {
				it.remove(); // saiu do mundo: o cliente dele recebe os blocos de verdade quando voltar
				continue;
			}
			boolean perto = a.raioDesfaz() > 0 && p.distanceToSqr(Vec3.atCenterOf(a.pos())) < a.raioDesfaz() * a.raioDesfaz();
			if (tick < a.expiraTick() && !perto) {
				continue;
			}
			it.remove();
			p.connection.send(new ClientboundBlockUpdatePacket(level, a.pos()));
			Depuracao.log(p, tick / 20, String.format(Locale.ROOT, "MIRAGEM fim motivo=%s por=%s pos=(%d,%d,%d)",
					a.motivo(), perto ? "CHEGOU_PERTO" : "TEMPO", a.pos().getX(), a.pos().getY(), a.pos().getZ()));
		}
	}

	static void limpar() {
		ATIVAS.clear();
	}
}
