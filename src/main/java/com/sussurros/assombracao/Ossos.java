package com.sussurros.assombracao;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.bloco.Mostruario;
import com.sussurros.entidade.HospedeEntity;

/**
 * Ossos de Agouro (0.9): um sorteio que o jogador não controla.
 *
 * Sete desfechos. O que saiu não é dito: os três ossos ficam caídos por alguns segundos, e cada desfecho os
 * derruba de um jeito.
 *
 * | desfecho  | peso | o que faz                                              | como caem                         |
 * | NADA      | 25   | nada                                                   | espalhados, sem ordem             |
 * | SILENCIO  | 15   | o mundo emudece por 25 s                               | três lado a lado                  |
 * | TREGUA    | 15   | dez minutos sem nenhum acontecimento                   | em estrela, as pontas se tocando  |
 * | APONTAM   | 15   | alinham-se na direção do vestígio mais próximo         | em fila, apontando                |
 * | PRESENCA  | 15   | ele aparece, agora                                     | dois cruzados, um afastado        |
 * | AMIGO     | 10   | ele aparece para o jogador mais próximo, não para você | em fila, apontando para o amigo   |
 * | CONTA     | 5    | a Conta sobe 3                                         | só dois: o terceiro se desfez     |
 *
 * Jogar várias vezes na mesma noite piora os pesos: os bons perdem para a PRESENCA. O "nada" nunca sai da lista.
 */
public final class Ossos {
	enum Desfecho {
		NADA, SILENCIO, TREGUA, APONTAM, PRESENCA, AMIGO, CONTA
	}

	private static final String ETIQUETA = "sussurros_ossos";
	private static final int[] PESOS = {25, 15, 15, 15, 15, 10, 5};

	private Ossos() {
	}

	/** Joga os ossos à frente do jogador. Devolve false se não há chão onde caírem. */
	public static boolean jogar(ServerPlayer p) {
		return jogar(p, null);
	}

	/** forcado: só para teste, escolhe o desfecho. */
	static boolean jogar(ServerPlayer p, @Nullable Desfecho forcado) {
		ServerLevel level = p.level();
		EstadoJogador e = Diretor.estadoParaTeste(p);
		Memoria m = Memoria.de(p);
		RandomSource sorte = p.getRandom();
		long tick = level.getGameTime();

		Vec3 frente = Diretor.pontoRelativo(p, 0, 1.6);
		BlockPos chao = null;
		for (int dy = 1; dy >= -2 && chao == null; dy--) {
			BlockPos c = BlockPos.containing(frente.x, p.getY() + dy, frente.z);
			if (level.getBlockState(c).isAir() && !level.getBlockState(c.below()).getCollisionShape(level, c.below()).isEmpty()) {
				chao = c;
			}
		}
		if (chao == null) {
			return false;
		}
		Vec3 centro = new Vec3(chao.getX() + 0.5, chao.getY() + 0.03, chao.getZ() + 0.5);

		long dia = level.getDefaultClockTime() / 24000L;
		if (e.ossosDia != dia) {
			e.ossosDia = dia;
			e.ossosNoDia = 0;
		}
		Desfecho d = forcado != null ? forcado : sortear(sorte, e.ossosNoDia);
		if (forcado == null && Conta.cobrarNoUso(p, m, Conta.Item.OSSOS)) {
			d = Desfecho.PRESENCA;
		}
		e.ossosNoDia++;

		// O que precisa de alvo e não tem vira o desfecho vizinho.
		Vec3 alvo = null;
		ServerPlayer amigo = null;
		if (d == Desfecho.APONTAM) {
			alvo = vestigioOuCriatura(p, e, tick);
			if (alvo == null) {
				d = Desfecho.NADA;
			}
		}
		if (d == Desfecho.AMIGO) {
			amigo = amigoMaisPerto(level, p);
			if (amigo == null) {
				d = Desfecho.PRESENCA;
			} else {
				alvo = amigo.position();
			}
		}

		cair(level, centro, d, alvo, sorte);
		level.playSound(null, centro.x, centro.y, centro.z, SoundEvents.BONE_BLOCK_HIT, SoundSource.PLAYERS, 0.5F, 1.3F);
		Conta.somar(p, m, Conta.Item.OSSOS, d == Desfecho.CONTA ? 4 : 1);
		m.salvar();
		Depuracao.log(p, tick / 20, String.format(Locale.ROOT, "OSSOS desfecho=%s jogadaDoDia=%d pos=%s",
				d, e.ossosNoDia, Diretor.pos(centro.x, centro.y, centro.z)));

		switch (d) {
			case SILENCIO -> Diretor.emudecer(level, p, 25, "OSSOS");
			case TREGUA -> e.treguaAte = tick + 20L * 600;
			case PRESENCA -> chamar(level, p, 60 + sorte.nextInt(60));
			case AMIGO -> chamar(level, amigo, 60 + sorte.nextInt(60));
			default -> {
			}
		}
		Vec3 limpar = centro;
		Diretor.agendar(level, 20 * 15, () -> Mostruario.limpar(level, limpar, 2.0, ETIQUETA));
		return true;
	}

	private static Desfecho sortear(RandomSource sorte, int jaJogouHoje) {
		int[] pesos = PESOS.clone();
		// Cada jogada no mesmo dia tira 6 pontos de cada desfecho bom e os dá à PRESENCA.
		int tira = Math.min(12, 6 * jaJogouHoje);
		for (Desfecho bom : new Desfecho[] {Desfecho.SILENCIO, Desfecho.TREGUA, Desfecho.APONTAM}) {
			pesos[bom.ordinal()] -= tira;
			pesos[Desfecho.PRESENCA.ordinal()] += tira;
		}
		int total = 0;
		for (int w : pesos) {
			total += w;
		}
		int r = sorte.nextInt(total);
		for (Desfecho d : Desfecho.values()) {
			r -= pesos[d.ordinal()];
			if (r < 0) {
				return d;
			}
		}
		return Desfecho.NADA;
	}

	@Nullable
	private static Vec3 vestigioOuCriatura(ServerPlayer p, EstadoJogador e, long tick) {
		Vestigios.Marca marca = Vestigios.de(p).maisPerto(p.getX(), p.getY(), p.getZ(), 64, tick / 20);
		if (marca != null) {
			return Vec3.atCenterOf(marca.pos());
		}
		HospedeEntity h = e.criatura;
		return h != null && !h.isRemoved() ? h.position() : null;
	}

	@Nullable
	private static ServerPlayer amigoMaisPerto(ServerLevel level, ServerPlayer p) {
		ServerPlayer melhor = null;
		double menor = 64 * 64;
		for (ServerPlayer o : level.getPlayers(j -> j != p && !j.isSpectator())) {
			double d = o.distanceToSqr(p);
			if (d < menor) {
				menor = d;
				melhor = o;
			}
		}
		return melhor;
	}

	/** Ele aparece para este jogador daqui a alguns segundos. Como a aparição chamada pelo Olho: não conta para a ousadia. */
	private static void chamar(ServerLevel level, ServerPlayer quem, int atrasoTicks) {
		Diretor.agendar(level, atrasoTicks, () -> {
			if (quem.isRemoved() || quem.level() != level) {
				return;
			}
			EstadoJogador e = Diretor.estadoParaTeste(quem);
			if (e.criatura != null && !e.criatura.isRemoved()) {
				return;
			}
			Diretor.invocar(level, quem, e, HospedeEntity.Modo.OBSERVAR, 100, 170, 18, 30, 20 * 20, 1.0, true,
					PedidoManifestacao.deOrigem(HospedeEntity.Origem.OLHO, null));
		});
	}

	/** Derruba os ossos no padrão do desfecho. Cada osso é um item deitado no chão. */
	private static void cair(ServerLevel level, Vec3 centro, Desfecho d, @Nullable Vec3 alvo, RandomSource sorte) {
		// {deslocamento em x, em z, giro em graus}
		double[][] ossos;
		switch (d) {
			case SILENCIO -> ossos = new double[][] {{0, -0.28, 0}, {0, 0, 0}, {0, 0.28, 0}};
			case TREGUA -> ossos = new double[][] {{0, -0.2, 90}, {-0.18, 0.12, 30}, {0.18, 0.12, 150}};
			case PRESENCA -> ossos = new double[][] {{0, 0, 45}, {0, 0, 135}, {0.42, 0.3, 80}};
			case APONTAM, AMIGO -> {
				double dx = alvo.x - centro.x;
				double dz = alvo.z - centro.z;
				double comprimento = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
				dx /= comprimento;
				dz /= comprimento;
				// Com giro 0 o osso fica deitado de oeste a leste; o giro positivo o vira do leste para o norte.
				double giro = Math.toDegrees(Math.atan2(-dz, dx));
				ossos = new double[][] {{-dx * 0.5, -dz * 0.5, giro}, {0, 0, giro}, {dx * 0.5, dz * 0.5, giro}};
			}
			case CONTA -> ossos = new double[][] {{-0.2, 0.1, sorte.nextInt(180)}, {0.25, -0.15, sorte.nextInt(180)}};
			default -> ossos = new double[][] {
					{sorte.nextDouble() * 0.9 - 0.45, sorte.nextDouble() * 0.9 - 0.45, sorte.nextInt(180)},
					{sorte.nextDouble() * 0.9 - 0.45, sorte.nextDouble() * 0.9 - 0.45, sorte.nextInt(180)},
					{sorte.nextDouble() * 0.9 - 0.45, sorte.nextDouble() * 0.9 - 0.45, sorte.nextInt(180)}};
		}
		ItemStack osso = new ItemStack(Items.BONE);
		for (int i = 0; i < ossos.length; i++) {
			// O desenho do osso é diagonal no item; os 45 graus o alinham com o giro pedido.
			Mostruario.mostrar(level, centro.x + ossos[i][0], centro.y + i * 0.004, centro.z + ossos[i][1], osso, 0.5F,
					90.0F, (float) ossos[i][2] + 45.0F, ETIQUETA);
		}
		if (d == Desfecho.CONTA) {
			level.sendParticles(ParticleTypes.ASH, centro.x, centro.y + 0.1, centro.z, 16, 0.2, 0.05, 0.2, 0.0);
		}
	}
}
