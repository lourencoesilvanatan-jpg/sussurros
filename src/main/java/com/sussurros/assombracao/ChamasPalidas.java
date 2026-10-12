package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Chamas Pálidas (0.9): a progressão lida no mundo, sem tela.
 *
 * Da fase 2 em diante, uma ou duas tochas que o próprio jogador pôs passam a queimar pálidas (a chama azulada
 * da tocha de almas), só para ele, por alguns minutos. É miragem: o mundo não muda, quem está ao lado não vê,
 * e clicar na tocha a desfaz. Quanto mais fundo ele está, mais tochas ao mesmo tempo.
 *
 * 0.9.0-alpha18: entre uma tocha que empalidece e a seguinte passam pelo menos três minutos. Antes a cota era
 * reposta a cada vinte segundos: na sessão de 11/10/2026 foram noventa em noventa minutos, e numa caverna
 * iluminada por tochas isso virou o assunto do mod ("muito focado em mudar a tocha de cor", disse o dono).
 * O acréscimo de três que alguns acontecimentos dão por um tempo (chamasExtraAteSeg) não espera.
 *
 * Não é evento: não passa pelo sorteio do Diretor, não lê reação, não conta para pressão nem aprendizado.
 * A troca só acontece com a tocha fora da tela, para ele nunca ver a chama mudar; só encontrá-la mudada.
 * Dentro da vela nenhuma tocha nova empalidece.
 */
final class ChamasPalidas {
	static final String MOTIVO = "CHAMA_PALIDA";
	private static final double ALCANCE = 24.0;
	/** Segundos entre uma tocha que empalidece e a seguinte. */
	private static final int INTERVALO = 180;

	private ChamasPalidas() {
	}

	/** Uma vez por segundo, dentro do tick do Diretor. Só age a cada vinte segundos. */
	static void segundo(ServerLevel level, ServerPlayer p, EstadoJogador e, int fase, boolean calma, long seg) {
		if (fase < 2 || calma || seg % 20 != Math.floorMod(p.getUUID().hashCode(), 20)) {
			return;
		}
		boolean extra = seg < e.chamasExtraAteSeg;
		int cota = (fase >= 4 ? 2 : 1) + (e.obsessao >= 60 ? 1 : 0) + (extra ? 3 : 0);
		if (Miragem.ativas(p, MOTIVO) >= cota || (!extra && seg < e.chamaPalidaLiberadaEm)) {
			return;
		}
		// 0.9.0-alpha22: debaixo da terra, luz não passa de duas nas últimas cinco coisas que saíram (ver
		// Familias). Aqui a regra não cede: a tocha espera até outra família ter a vez. Só o acréscimo de uma
		// carta do baralho passa por cima: várias tochas de uma vez é o acontecimento que a carta pediu.
		if (!extra && Familias.barrada(e, Familias.Familia.LUZ)) {
			return;
		}
		// A lista de tochas por perto só é refeita quando alguém pede (e no máximo a cada dez segundos).
		Diretor.atualizarCacheAmbiente(level, p, e);
		List<BlockPos> candidatas = new ArrayList<>();
		for (BlockPos pos : e.tochas) {
			BlockState st = level.getBlockState(pos);
			Vec3 centro = Vec3.atCenterOf(pos);
			if ((st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) && p.distanceToSqr(centro) <= ALCANCE * ALCANCE
					&& !Diretor.pontoNaFrente(p, centro, Percepcao.coneSeguro(p)) && !Miragem.tem(p, pos)) {
				candidatas.add(pos);
			}
		}
		if (candidatas.isEmpty()) {
			return;
		}
		// Gerador do jogador, não o do mundo: isto é apresentação e não pode mexer nos sorteios do Diretor.
		RandomSource sorte = p.getRandom();
		BlockPos pos = candidatas.get(sorte.nextInt(candidatas.size()));
		BlockState real = level.getBlockState(pos);
		BlockState palida = real.is(Blocks.WALL_TORCH)
				? Blocks.SOUL_WALL_TORCH.defaultBlockState()
						.setValue(BlockStateProperties.HORIZONTAL_FACING, real.getValue(BlockStateProperties.HORIZONTAL_FACING))
				: Blocks.SOUL_TORCH.defaultBlockState();
		int duracao = 20 * (120 + sorte.nextInt(181));
		if (Miragem.mostrar(level, p, pos, palida, duracao, 0, MOTIVO)) {
			e.chamaPalidaLiberadaEm = seg + INTERVALO;
			Depuracao.log(p, seg, String.format(Locale.ROOT, "CHAMA_PALIDA pos=(%d,%d,%d) duracao=%ds ativas=%d cota=%d",
					pos.getX(), pos.getY(), pos.getZ(), duracao / 20, Miragem.ativas(p, MOTIVO), cota));
			Familias.registrar(p, e, Familias.Familia.LUZ, MOTIVO, seg);
		}
	}
}
