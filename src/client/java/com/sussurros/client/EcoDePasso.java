package com.sussurros.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.rede.PacoteSentidos;

/**
 * O passo em eco. Enquanto o servidor mantém a janela aberta, parte dos passos do próprio jogador toca de novo
 * uma fração de segundo depois, um pouco mais baixo e um pouco atrás, com o som do chão onde ele está.
 * Se ele para, o eco para junto. É para dar a impressão de alguém pisando no mesmo ritmo.
 *
 * Fica no cliente porque depende do instante exato do passo, que o servidor não conhece.
 */
final class EcoDePasso {
	/** A cada tantos blocos andados o jogo toca um passo; o eco segue a mesma medida. */
	private static final double PASSO = 2.2;

	private static final RandomSource SORTE = SoundInstance.createUnseededRandom();

	private static double ultimoX;
	private static double ultimoZ;
	private static boolean temUltimo;
	private static double andado;
	private static int espera = -1;

	private EcoDePasso() {
	}

	static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) {
			return;
		}
		if (!SentidosCliente.tem(PacoteSentidos.FLAG_ECO_PASSO)) {
			temUltimo = false;
			andado = 0;
			espera = -1;
			return;
		}

		if (temUltimo && p.onGround() && !p.isCrouching() && !p.isPassenger()) {
			double dx = p.getX() - ultimoX;
			double dz = p.getZ() - ultimoZ;
			double d = Math.sqrt(dx * dx + dz * dz);
			if (d < 1.5) { // um salto maior que isso é teleporte, não passo
				andado += d;
			}
		}
		ultimoX = p.getX();
		ultimoZ = p.getZ();
		temUltimo = true;

		if (andado >= PASSO) {
			andado = 0;
			// Nem todo passo ecoa: um eco perfeito parece efeito de som, não outra pessoa.
			if (espera < 0 && SORTE.nextFloat() < 0.7F) {
				espera = 4 + SORTE.nextInt(4);
			}
		}

		if (espera >= 0 && --espera < 0) {
			BlockState chao = mc.level.getBlockState(p.getOnPos());
			if (chao.isAir()) {
				return;
			}
			SoundType tipo = chao.getSoundType();
			Vec3 olhar = p.getLookAngle();
			double x = p.getX() - olhar.x * 1.6;
			double z = p.getZ() - olhar.z * 1.6;
			mc.getSoundManager().play(new SimpleSoundInstance(tipo.getStepSound(), SoundSource.HOSTILE,
					tipo.getVolume() * 0.11F, tipo.getPitch() * 0.9F, SORTE, x, p.getY(), z));
		}
	}
}
