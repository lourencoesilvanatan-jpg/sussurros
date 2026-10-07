package com.sussurros.assombracao;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leitura barata do tipo de lugar em que o jogador esta agora.
 *
 * Nao cria uma nova "IA": so da contexto ao Diretor para que a mesma memoria
 * se manifeste de forma diferente em casa, no subsolo, em area aberta e em lugares com cobertura.
 */
final class ContextoMundo {
	enum Tipo {
		CASA, SUBSOLO, ABERTO, FLORESTA, OUTRO
	}

	private ContextoMundo() {
	}

	static Tipo detectar(ServerLevel level, ServerPlayer p, Memoria m, boolean subterraneo) {
		if (pertoDaCasa(p, m, 26)) {
			return Tipo.CASA;
		}
		if (subterraneo) {
			return Tipo.SUBSOLO;
		}
		BlockPos base = p.blockPosition();
		if (areaAberta(level, base)) {
			return Tipo.ABERTO;
		}
		if (areaFlorestal(level, base)) {
			return Tipo.FLORESTA;
		}
		return Tipo.OUTRO;
	}

	static boolean pertoDaCasa(ServerPlayer p, Memoria m, double raio) {
		double r2 = raio * raio;
		if (m.get(Memoria.TEM_CAMA) == 1) {
			double dx = p.getX() - (m.get(Memoria.CAMA_X) + 0.5);
			double dz = p.getZ() - (m.get(Memoria.CAMA_Z) + 0.5);
			if (dx * dx + dz * dz <= r2) {
				return true;
			}
		}
		if (m.get(Memoria.TEM_PORTA) == 1) {
			double dx = p.getX() - (m.get(Memoria.PORTA_X) + 0.5);
			double dz = p.getZ() - (m.get(Memoria.PORTA_Z) + 0.5);
			return dx * dx + dz * dz <= r2;
		}
		return false;
	}

	/**
	 * Area aberta = ceu visivel e poucas paredes/obstaculos altos em volta.
	 * Sao apenas oito amostras, uma vez por segundo por jogador.
	 */
	static boolean areaAberta(ServerLevel level, BlockPos base) {
		if (!level.canSeeSky(base.above())) {
			return false;
		}
		int[][] dirs = {
				{1, 0}, {-1, 0}, {0, 1}, {0, -1},
				{1, 1}, {1, -1}, {-1, 1}, {-1, -1}
		};
		int bloqueadas = 0;
		for (int[] d : dirs) {
			boolean bloqueada = false;
			for (int y = 0; y <= 3 && !bloqueada; y++) {
				BlockPos pos = base.offset(d[0] * 5, y, d[1] * 5);
				BlockState s = level.getBlockState(pos);
				if (!s.getCollisionShape(level, pos).isEmpty()) {
					bloqueada = true;
				}
			}
			if (bloqueada && ++bloqueadas >= 4) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Floresta/borda fechada = céu ainda visível, mas muitos obstáculos verticais próximos.
	 * Não tenta identificar bioma: interessa a geometria que permite esconder uma silhueta.
	 */
	static boolean areaFlorestal(ServerLevel level, BlockPos base) {
		if (!level.canSeeSky(base.above())) {
			return false;
		}
		int[][] dirs = {
				{1, 0}, {-1, 0}, {0, 1}, {0, -1},
				{1, 1}, {1, -1}, {-1, 1}, {-1, -1}
		};
		int bloqueadas = 0;
		for (int[] d : dirs) {
			boolean bloqueada = false;
			for (int passo = 3; passo <= 7 && !bloqueada; passo += 2) {
				for (int y = 0; y <= 4 && !bloqueada; y++) {
					BlockPos pos = base.offset(d[0] * passo, y, d[1] * passo);
					BlockState st = level.getBlockState(pos);
					if (!st.getCollisionShape(level, pos).isEmpty()) {
						bloqueada = true;
					}
				}
			}
			if (bloqueada) {
				bloqueadas++;
			}
		}
		return bloqueadas >= 4;
	}

	/** Quanto este tipo de lugar favorece cada evento. */
	static double multiplicador(Tipo tipo, Evento ev) {
		return switch (tipo) {
			case CASA -> switch (ev) {
				case ANIMAIS -> 1.25;
				case OBJETO_FORA_LUGAR -> 1.70;
				case LUZ_ERRADA -> 1.35;
				case PASSAGEM -> 0.85;
				case VESTIGIO -> 0.80;
				case PORTA -> 1.85;
				case BATIDA -> 1.70;
				case SUSSURRO -> 1.30;
				case SINAL -> 1.55;
				case PEGADAS -> 0.55;
				case ECO -> 1.15;
				case TOCHA -> 1.20;
				case PRESENCA -> 0.75;
				case PASSOS -> 0.80;
				default -> 1.0;
			};
			case SUBSOLO -> switch (ev) {
				case LUZ_ERRADA -> 1.80;
				case RUIDO_RETORNO -> 1.80;
				case TRILHA_INTERROMPIDA -> 1.55;
				case VESTIGIO -> 1.45;
				case ANIMAIS -> 0.15;
				case ECO -> 1.90;
				case SINAL -> 1.70;
				case PEGADAS -> 1.55;
				case PASSO_UNICO -> 1.40;
				case PASSOS -> 1.20;
				case ATRAS -> 1.20;
				case PRESENCA -> 1.10;
				case PORTA, BATIDA -> 0.15;
				case TOCHA -> 0.60;
				default -> 1.0;
			};
			case ABERTO -> switch (ev) {
				case ANIMAIS -> 1.55;
				case SINAL_DISTANTE -> 1.45;
				case PASSAGEM -> 1.20;
				case OBJETO_FORA_LUGAR -> 0.85;
				case PRESENCA -> 1.85;
				case SINAL -> 1.50;
				case PEGADAS -> 0.90;
				case SEGUIDOR -> 1.35;
				case PASSOS -> 1.25;
				case PASSO_UNICO -> 1.15;
				case ECO -> 0.80;
				case PORTA, BATIDA, TOCHA -> 0.10;
				default -> 1.0;
			};
			case FLORESTA -> switch (ev) {
				case ANIMAIS -> 1.85;
				case PASSAGEM -> 1.55;
				case SINAL_DISTANTE -> 1.50;
				case VESTIGIO -> 1.35;
				case PRESENCA -> 1.65;
				case SEGUIDOR -> 1.70;
				case SINAL -> 1.35;
				case PEGADAS -> 1.75;
				case PASSOS -> 1.35;
				case PASSO_UNICO -> 1.25;
				case ECO -> 0.95;
				case PORTA, BATIDA, TOCHA -> 0.10;
				default -> 1.0;
			};
			case OUTRO -> switch (ev) {
				case SINAL, SEGUIDOR, PEGADAS, OBJETO_FORA_LUGAR, PASSAGEM, SINAL_DISTANTE -> 1.20;
				default -> 1.0;
			};
		};
	}
}
