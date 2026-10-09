package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteEfeito;
import com.sussurros.rede.Rede;

/**
 * O Véu (0.9): por meio minuto ou um, o mundo de sempre fica do outro lado de alguma coisa.
 *
 * Não é dimensão e o jogador não sai do lugar. A tela pisca e, quando abre:
 *  - a cor é a do Avesso, a neblina está perto e o som sumiu (as mesmas camadas que a dimensão vai usar);
 *  - os bichos, os amigos e tudo o que é entidade deixam de aparecer para ele. Só o Hóspede aparece;
 *  - as luzes em volta estão apagadas, uma porta está aberta e um vidro falta. Tudo miragem: nada muda de
 *    verdade, e quem está ao lado não vê nada disso;
 *  - em metade das vezes, a partir do meio, ele está parado no limite da neblina.
 * Outra piscada e tudo voltou. É o "eu vi isso mesmo?" do mod: curto, raro, sem consequência nenhuma.
 *
 * Regras: nada novo começa enquanto o Véu dura (nem evento, nem cena, nem caçada); acender uma Vela Pálida
 * o rasga na hora; nunca começa com criatura presente, na vela, ou com o jogador em perigo de verdade.
 *
 * Isto é apresentação: usa o gerador do jogador, não o do mundo.
 */
public final class Veu {
	static final String MOTIVO = "VEU";
	/** Ticks de tela fechada na entrada e na saída. O mundo troca com a tela fechada. */
	private static final int PISCADA = 12;

	private Veu() {
	}

	static boolean ativo(EstadoJogador e, long tick) {
		return tick >= e.veuDesde && tick < e.veuAte;
	}

	/** O Véu pode abrir agora? Usado pelo sorteio de eventos; o comando de teste passa por cima do intervalo. */
	static boolean pode(ServerLevel level, ServerPlayer p, EstadoJogador e, long tick, boolean respeitarIntervalo) {
		if (tick < e.veuAte || (respeitarIntervalo && tick < e.veuProximoEm)) {
			return false;
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			return false;
		}
		return !Diretor.bloqueado(p, e, tick) && !Diretor.emZonaCalma(p, p.getX(), p.getY(), p.getZ());
	}

	/** Abre o Véu. Devolve um resumo para o log e para o comando de teste. */
	static String abrir(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long tick, boolean teste) {
		RandomSource sorte = p.getRandom();
		int duracao = 20 * (30 + sorte.nextInt(31));
		e.veuDesde = tick + PISCADA / 2;
		e.veuAte = e.veuDesde + duracao;
		e.veuVultoEm = sorte.nextBoolean() ? e.veuDesde + duracao / 2 : -1;
		e.veuFechou = false;
		// De 30 a 50 minutos até o próximo. É raro de propósito: repetido, vira efeito especial.
		e.veuProximoEm = e.veuAte + 20L * 60 * (30 + sorte.nextInt(21));
		if (!teste) {
			m.add(Memoria.VEUS, 1);
		}

		Rede.efeito(p, PacoteEfeito.Tipo.PISCAR, PISCADA, 1.0F);
		Diretor.agendar(level, PISCADA / 2 - 1, () -> {
			if (!p.isRemoved() && !e.veuFechou) {
				Sentidos.veuAgora(p, e, true);
			}
		});
		Diretor.emudecer(level, p, duracao / 20 + 2, MOTIVO);

		// O que está errado na casa. As miragens duram exatamente o Véu e somem na piscada da saída.
		Diretor.atualizarCacheAmbiente(level, p, e);
		int luzes = ApoioCaca.apagarLuzPerto(level, p, p.blockPosition(), 14, duracao + PISCADA, 16);
		boolean porta = abrirUmaPorta(level, p, e, duracao + PISCADA, sorte);
		boolean vidro = tirarUmVidro(level, p, e, duracao + PISCADA, sorte);

		String resumo = String.format(Locale.ROOT, "VEU abriu duracao=%ds luzes=%d porta=%s vidro=%s vulto=%s teste=%s",
				duracao / 20, luzes, porta ? "sim" : "nao", vidro ? "sim" : "nao", e.veuVultoEm >= 0 ? "sim" : "nao", teste ? "sim" : "nao");
		Depuracao.log(p, tick / 20, resumo);
		return resumo;
	}

	/** Uma vez por segundo, dentro do tick do Diretor: o vulto do meio e o fim. */
	static void segundo(ServerLevel level, ServerPlayer p, EstadoJogador e, long tick) {
		if (e.veuAte == 0) {
			return;
		}
		if (ativo(e, tick) && e.veuVultoEm >= 0 && tick >= e.veuVultoEm) {
			e.veuVultoEm = -1;
			// De lado, fora da tela (ele nunca nasce na frente do jogador), dentro do que a neblina deixa ver:
			// o jogador o encontra quando vira o rosto. Fica até o Véu acabar.
			int resta = (int) Math.max(40, e.veuAte - tick);
			boolean veio = (e.criatura == null || e.criatura.isRemoved()) && Diretor.invocar(level, p, e, HospedeEntity.Modo.OBSERVAR,
					75, 130, 12, 17, resta, 1.0, false, PedidoManifestacao.deOrigem(HospedeEntity.Origem.OLHO, null));
			Depuracao.log(p, tick / 20, "VEU vulto=" + (veio ? "sim" : "nao_coube"));
		}
		if (!e.veuFechou && tick >= e.veuAte - PISCADA / 2) {
			fechar(level, p, e, tick, "TEMPO");
		}
	}

	/** Fecha o Véu agora (o tempo acabou, ou uma vela foi acesa). */
	static void fechar(ServerLevel level, ServerPlayer p, EstadoJogador e, long tick, String motivo) {
		if (e.veuFechou || e.veuAte == 0) {
			return;
		}
		e.veuFechou = true;
		e.veuVultoEm = -1;
		e.veuAte = Math.min(e.veuAte, tick + PISCADA / 2);
		Rede.efeito(p, PacoteEfeito.Tipo.PISCAR, PISCADA, 1.0F);
		Diretor.agendar(level, PISCADA / 2 - 1, () -> {
			if (!p.isRemoved()) {
				Sentidos.veuAgora(p, e, false);
				Miragem.encerrar(level, p, MOTIVO);
				Miragem.encerrar(level, p, "LUZ_APAGADA");
				if (e.criatura != null && !e.criatura.isRemoved() && e.criatura.getModo() == HospedeEntity.Modo.OBSERVAR) {
					e.criatura.sumir(level, false, "VEU_FECHOU");
				}
			}
		});
		Depuracao.log(p, tick / 20, "VEU fechou motivo=" + motivo);
	}

	/** Uma porta fechada, fora da tela, aparece aberta. As duas metades. */
	private static boolean abrirUmaPorta(ServerLevel level, ServerPlayer p, EstadoJogador e, int ticks, RandomSource sorte) {
		List<BlockPos> portas = new ArrayList<>();
		for (BlockPos pos : e.portas) {
			BlockState s = level.getBlockState(pos);
			if (s.getBlock() instanceof DoorBlock && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
					&& !s.getValue(BlockStateProperties.OPEN) && foraDaTela(p, pos)) {
				portas.add(pos);
			}
		}
		if (portas.isEmpty()) {
			return false;
		}
		BlockPos baixo = portas.get(sorte.nextInt(portas.size()));
		BlockState cima = level.getBlockState(baixo.above());
		boolean ok = Miragem.mostrar(level, p, baixo, level.getBlockState(baixo).setValue(BlockStateProperties.OPEN, true), ticks, 0, MOTIVO);
		if (ok && cima.getBlock() instanceof DoorBlock) {
			Miragem.mostrar(level, p, baixo.above(), cima.setValue(BlockStateProperties.OPEN, true), ticks, 0, MOTIVO);
		}
		return ok;
	}

	/** Um bloco de vidro de uma janela, fora da tela, deixa de estar lá. */
	private static boolean tirarUmVidro(ServerLevel level, ServerPlayer p, EstadoJogador e, int ticks, RandomSource sorte) {
		List<BlockPos> vidros = new ArrayList<>();
		for (BlockPos pos : e.janelas) {
			if (!level.getBlockState(pos).isAir() && foraDaTela(p, pos)) {
				vidros.add(pos);
			}
		}
		if (vidros.isEmpty()) {
			return false;
		}
		return Miragem.mostrar(level, p, vidros.get(sorte.nextInt(vidros.size())), Blocks.AIR.defaultBlockState(), ticks, 0, MOTIVO);
	}

	private static boolean foraDaTela(ServerPlayer p, BlockPos pos) {
		return !Diretor.pontoNaFrente(p, Vec3.atCenterOf(pos), Percepcao.coneSeguro(p));
	}
}
