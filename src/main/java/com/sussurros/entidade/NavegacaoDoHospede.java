package com.sussurros.entidade;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/**
 * A navegação do Hóspede (0.9.0-alpha24): a de qualquer criatura que anda, com uma diferença só.
 *
 * O jogo calcula o caminho de uma criatura com o tamanho que ela tem na hora da conta: com 1,9 de altura, um vão
 * de um bloco é parede, e a conta responde "não há caminho". Na caça ele cabe nesse vão (de quatro), então a
 * conta é feita com esse corpo. O corpo de verdade só muda onde o caminho pede (ver Cacada.ajustarCorpo).
 *
 * Toda conta de caminho do jogo passa por este método, inclusive as que a própria navegação refaz sozinha.
 */
final class NavegacaoDoHospede extends GroundPathNavigation {
	private final HospedeEntity hospede;

	NavegacaoDoHospede(HospedeEntity hospede, Level level) {
		super(hospede, level);
		this.hospede = hospede;
	}

	@Override
	@Nullable
	protected Path createPath(Set<BlockPos> targets, int radiusOffset, boolean above, int reachRange, float maxPathLength) {
		// "hospede" ainda é nulo enquanto a criatura está sendo construída; ali ninguém pede caminho.
		if (this.hospede == null || !this.hospede.planejaDeQuatro()) {
			return super.createPath(targets, radiusOffset, above, reachRange, maxPathLength);
		}
		this.hospede.pensarDeQuatro(true);
		try {
			return super.createPath(targets, radiusOffset, above, reachRange, maxPathLength);
		} finally {
			this.hospede.pensarDeQuatro(false);
		}
	}
}
