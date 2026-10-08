package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.sussurros.Sussurros;

/**
 * Alterações reversíveis no mundo. Nunca sobrescreve uma mudança feita pelo jogador na hora de restaurar:
 * só desfaz o que ainda está exatamente no estado que o Sussurros colocou.
 */
final class AlteracoesTemporarias {
	private record Alteracao(ResourceKey<Level> dimensao, BlockPos pos, BlockState original, BlockState colocado, long restauraTick, String motivo,
			@Nullable Runnable aoRestaurar) {
	}

	private static final List<Alteracao> ATIVAS = new ArrayList<>();

	private AlteracoesTemporarias() {
	}

	static boolean substituir(ServerLevel level, BlockPos pos, BlockState novo, long duracaoTicks, String motivo) {
		return substituir(level, pos, novo, duracaoTicks, motivo, null);
	}

	/** aoRestaurar: roda só quando o bloco volta no tempo normal (um som, por exemplo), não no fechamento do mundo. */
	static boolean substituir(ServerLevel level, BlockPos pos, BlockState novo, long duracaoTicks, String motivo,
			@Nullable Runnable aoRestaurar) {
		BlockState original = level.getBlockState(pos);
		if (original.equals(novo)) {
			return false;
		}
		level.setBlock(pos, novo, 3);
		ATIVAS.add(new Alteracao(level.dimension(), pos.immutable(), original, novo,
				level.getGameTime() + Math.max(1, duracaoTicks), motivo, aoRestaurar));
		return true;
	}

	static void tick(ServerLevel level, long tick) {
		if (ATIVAS.isEmpty()) {
			return;
		}
		Iterator<Alteracao> it = ATIVAS.iterator();
		while (it.hasNext()) {
			Alteracao a = it.next();
			if (!a.dimensao().equals(level.dimension())) {
				continue;
			}
			if (tick < a.restauraTick()) {
				continue;
			}
			it.remove();
			// Se o jogador mexeu no bloco enquanto isso, respeitamos a ação dele.
			if (level.getBlockState(a.pos()).equals(a.colocado())) {
				level.setBlock(a.pos(), a.original(), 3);
				if (a.aoRestaurar() != null) {
					try {
						a.aoRestaurar().run();
					} catch (Exception ex) {
						Sussurros.LOGGER.error("Erro ao restaurar uma alteração temporária", ex);
					}
				}
			}
		}
	}

	/**
	 * O mundo está fechando: desfaz agora tudo o que ainda está pendente. A lista só existe na memória,
	 * então sem isto o que era "temporário" ficaria gravado no mundo para sempre.
	 */
	static void restaurarTudo(MinecraftServer server) {
		for (Alteracao a : ATIVAS) {
			ServerLevel level = server.getLevel(a.dimensao());
			if (level != null && level.getBlockState(a.pos()).equals(a.colocado())) {
				level.setBlock(a.pos(), a.original(), 3);
			}
		}
		ATIVAS.clear();
	}

	static void limpar() {
		ATIVAS.clear();
	}
}
