package com.sussurros.assombracao;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteEfeito;
import com.sussurros.rede.Rede;
import com.sussurros.registro.ModSons;

/**
 * O que a caçada (com.sussurros.entidade.Cacada) pede ao lado do jogador: sons só para ele, luz que apaga
 * só para ele, o piscar, o aviso no bloco, a vela, e os contadores que ficam na memória.
 *
 * A criatura decide o que fazer; aqui fica o que isso faz ao jogador e ao que ele percebe. Criatura criada
 * por comando de teste não deixa nada na memória.
 */
public final class ApoioCaca {
	private ApoioCaca() {
	}

	/** O jogador nunca foi caçado: a primeira caçada ensina a regra sem as exceções (sem atalho, sem atravessar). */
	public static boolean primeiraCacada(ServerPlayer p) {
		return Memoria.de(p).get(Memoria.CACADAS) == 0;
	}

	/**
	 * Mantém o mundo mudo para o alvo enquanto ele estiver por perto: sem música, sem ambiente, bichos calados.
	 * Chamado a cada cinco segundos pela caçada. Quando para de ser chamado, o som volta sozinho em poucos
	 * segundos: é assim que o jogador sabe que ele foi embora de verdade.
	 */
	public static void manterSilencio(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
		p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.AMBIENT));
		for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0, 10.0, 24.0), m -> m.isAlive() && m != h)) {
			mob.ambientSoundTime = Math.min(mob.ambientSoundTime, -160);
		}
		Diretor.estadoParaTeste(p).semMusicaAte = level.getGameTime() + 140;
	}

	/** O piscar forçado. Sem o mod no cliente, a escuridão do próprio jogo faz as vezes. */
	public static void piscar(ServerPlayer p) {
		if (Rede.temCliente(p)) {
			Rede.efeito(p, PacoteEfeito.Tipo.PISCAR, 8, 1.0F);
		} else {
			p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false));
		}
	}

	/**
	 * Apaga, só para este jogador, até "maximo" luzes em volta de um ponto: o bloco some da tela dele e a luz
	 * some junto (conferido em jogo em 08/10/2026). No mundo nada muda, e a luz volta sozinha depois.
	 */
	public static int apagarLuzPerto(ServerLevel level, ServerPlayer p, BlockPos centro, int raio, int duracaoTicks, int maximo) {
		int apagadas = 0;
		for (int dx = -raio; dx <= raio && apagadas < maximo; dx++) {
			for (int dy = -2; dy <= 3 && apagadas < maximo; dy++) {
				for (int dz = -raio; dz <= raio && apagadas < maximo; dz++) {
					BlockPos pos = centro.offset(dx, dy, dz);
					if (!ehLuzQueApaga(level.getBlockState(pos))) {
						continue;
					}
					if (Miragem.mostrar(level, p, pos, Blocks.AIR.defaultBlockState(), duracaoTicks, 0, "LUZ_APAGADA")) {
						ModSons.tocarEventoPara(p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
								pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.35F, 0.6F);
						apagadas++;
					}
				}
			}
		}
		return apagadas;
	}

	static boolean ehLuzQueApaga(BlockState estado) {
		return estado.is(Blocks.TORCH) || estado.is(Blocks.WALL_TORCH) || estado.is(Blocks.SOUL_TORCH)
				|| estado.is(Blocks.SOUL_WALL_TORCH) || estado.is(Blocks.LANTERN) || estado.is(Blocks.SOUL_LANTERN);
	}

	/** Um passo dele, com o som do chão onde ele pisa. Só o alvo ouve, e ouve de longe. */
	public static void passo(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		BlockState chao = level.getBlockState(h.getOnPos());
		if (chao.isAir()) {
			return;
		}
		ModSons.tocarEventoPara(p, chao.getSoundType().getStepSound(), SoundSource.HOSTILE,
				h.getX(), h.getY(), h.getZ(), 1.5F, 0.7F);
	}

	public static void somDePano(ServerPlayer p, HospedeEntity h) {
		ModSons.tocarPara(p, h.getX(), h.getY() + 1.0, h.getZ(), ModSons.Som.PANO, 0.9F, 0.85F);
	}

	public static void somDeRecuo(ServerPlayer p, HospedeEntity h) {
		ModSons.tocarPara(p, p.getX(), p.getY() + 1.0, p.getZ(), ModSons.Som.GRAVE, 0.45F, 1.05F);
	}

	/** A maçaneta: ele está atrás da porta. Este som é do mundo (a porta vai abrir de verdade). */
	public static void macaneta(ServerLevel level, BlockPos porta) {
		level.playSound(null, porta.getX() + 0.5, porta.getY() + 0.5, porta.getZ() + 0.5,
				SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.3F, 1.7F);
	}

	public static int vezesAtravessou(ServerPlayer p) {
		return Memoria.de(p).get(Memoria.CACA_ATRAVESSOU);
	}

	public static void contarAtravessou(ServerPlayer p, HospedeEntity h) {
		if (h.ehTeste()) {
			return;
		}
		Memoria m = Memoria.de(p);
		m.add(Memoria.CACA_ATRAVESSOU, 1);
		m.salvar();
	}

	/**
	 * O aviso de que ele vai atravessar: batidas e arranhões abafados vindos de dentro de um bloco, e poeira
	 * escura na face dele. É para o jogador entender "sai de perto daqui" com dois segundos de folga.
	 */
	public static void avisoNoBloco(ServerLevel level, ServerPlayer p, BlockPos bloco, boolean comSom) {
		double x = bloco.getX() + 0.5;
		double y = bloco.getY() + 0.5;
		double z = bloco.getZ() + 0.5;
		level.sendParticles(p, ParticleTypes.SMOKE, true, false, x, y, z, 10, 0.42, 0.42, 0.42, 0.004);
		level.sendParticles(p, ParticleTypes.ASH, true, false, x, y, z, 14, 0.5, 0.5, 0.5, 0.0);
		if (comSom) {
			boolean batida = (level.getGameTime() / 10) % 2 == 0;
			ModSons.tocarPara(p, x, y, z, batida ? ModSons.Som.PANCADA : ModSons.Som.ARRANHAR, 0.85F, 0.8F);
		}
	}

	/** Na fase 4, para quem já se apoiou muito na vela, ele pode soprá-la em vez de esperar. */
	public static boolean podeSoprarVela(ServerPlayer p) {
		Memoria m = Memoria.de(p);
		return m.get(Memoria.FASE) >= 4 && m.get(Memoria.VELAS) >= 4;
	}

	/** Um ponto de chão logo fora da zona da vela, do lado em que ele está. */
	@Nullable
	public static Vec3 pontoNaBordaDaVela(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		double dx = h.getX() - e.zonaX;
		double dz = h.getZ() - e.zonaZ;
		double comprimento = Math.sqrt(dx * dx + dz * dz);
		if (comprimento < 0.01) {
			dx = 1;
			dz = 0;
			comprimento = 1;
		}
		double r = e.zonaRaio + 2.5;
		BlockPos chao = Diretor.acharChao(level, e.zonaX + dx / comprimento * r, e.zonaY, e.zonaZ + dz / comprimento * r);
		return chao == null ? null : new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
	}

	/** Ele sopra a vela: a zona acaba agora. O aviso de "a vela apagou" sai pelo caminho normal, no tick do Diretor. */
	public static void soprarVela(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		if (e.zonaAteTick <= level.getGameTime()) {
			return;
		}
		e.zonaAteTick = level.getGameTime();
		ModSons.tocarPara(p, e.zonaX, e.zonaY + 0.5, e.zonaZ, ModSons.Som.SOPRO, 0.9F, 1.0F);
		Depuracao.log(p, level.getGameTime() / 20, "CACA id=" + h.getIdManifestacao() + " soprou a vela");
	}

	public static void capturar(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		Captura.executar(level, p, h);
	}
}
