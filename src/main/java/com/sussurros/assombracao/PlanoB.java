package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.registro.ModSons;

/**
 * O plano B (0.9.0-alpha20): nenhuma construção acaba em nada.
 *
 * Quando o mod prepara uma aparição e não acha onde pôr o corpo (a sequência de ameaça que esperou um minuto, a
 * presença de uma cena, o vulto do Véu, a aparição chamada pelo Olho), a preparação terminava em silêncio. Em
 * 11/10/2026 duas de quatro sequências de ameaça do dono acabaram assim, e quem jogava com ele perguntou "cadê
 * o bicho?". Agora ela termina num sinal físico, com lugar, do lado em que ele estaria:
 *
 *  - PO: cai pó do teto, com um toque surdo no bloco de cima. Só onde há teto, e só onde o jogador vê;
 *  - TOCHA: uma tocha daquele lado apaga por uns segundos (miragem: só ele vê, e o mundo não muda);
 *  - PASSOS: dois passos se afastam, com o som do chão de verdade daquele lugar.
 *
 * "O lado em que ele estaria" é o quase lugar da procura que falhou (Aparicao.Quase): o lugar em que ela quis
 * pô-lo e não pôde. No bolsão de caverna em que o único espaço fica à frente do jogador, é ali, à vista dele,
 * que o pó cai. Sem quase lugar, vale um ponto por onde ele passou; sem isso, atrás dele.
 *
 * Regras:
 *  - só o alvo vê e ouve (partículas e sons vão só para ele);
 *  - nada dentro da zona da vela: se não sobra lugar fora dela, não sai nada, e o log diz NENHUM;
 *  - não é manifestação e não conta como uma: a falta de lugar continua aparecendo no relatório do analisador;
 *  - não gasta saldo de atenção (é o fim de algo que já tinha começado), só impõe o respiro;
 *  - não usa o sorteio do mundo: o que sai depende do lugar e do que saiu da última vez (nunca o mesmo duas
 *    vezes seguidas, havendo escolha).
 *
 * O vazio que o mod ESCOLHE (o falso aviso, a sensação de ser olhado sem ninguém, a cena que não mostra ninguém
 * antes da fase 3) continua existindo e não passa por aqui: esperar e não receber faz parte do ritmo. O que
 * acabou foi o vazio por falha.
 */
final class PlanoB {
	enum Tipo {
		PO, TOCHA, PASSOS
	}

	static final String MIRAGEM = "PLANO_B";
	/** Quando não saiu nada (só dentro da zona da vela). */
	static final String NENHUM = "NENHUM";
	/** Além disto do jogador o sinal já não parece ser com ele: vem do mesmo lado, mais de perto. */
	private static final double LONGE = 14.0;
	private static final double PERTO = 9.0;

	private record Lugar(Vec3 pes, @Nullable BlockPos chao, String lado, boolean aVista) {
	}

	private PlanoB() {
	}

	/**
	 * Faz o sinal e devolve o nome dele, para a linha de log de quem chamou ("... SEM_LUGAR planoB=PO").
	 * "origem" é só para o log: AMEACA, CENA_TUNEL, VEU, OLHO...
	 */
	static String executar(ServerLevel level, ServerPlayer p, EstadoJogador e, String origem, long seg, long tick) {
		Lugar lugar = escolherLugar(level, p, e, seg, tick);
		if (lugar == null) {
			e.planoBResumo = NENHUM;
			Depuracao.log(p, seg, "PLANO_B tipo=NENHUM origem=" + origem + " motivo=ZONA_CALMA");
			return NENHUM;
		}
		BlockPos teto = lugar.chao() != null && lugar.aVista() ? tetoDe(level, lugar.chao()) : null;
		BlockPos tocha = tochaPerto(level, p, e, lugar.pes());
		Tipo tipo = escolherTipo(teto != null, tocha != null, lugar.aVista(), e.planoBUltimo);
		Vec3 onde = switch (tipo) {
			case PO -> po(level, p, teto);
			case TOCHA -> apagarTocha(level, p, tocha, seg);
			case PASSOS -> passos(level, p, lugar);
		};
		e.planoBUltimo = tipo;
		e.planoBResumo = tipo.name();
		Atencao.respirar(e, seg);
		// A família do sinal (ver Familias). O que veio de comando não entra na conta.
		if (!"COMANDO".equals(origem)) {
			Familias.registrar(p, e, Familias.de(tipo), "PLANO_B_" + tipo, seg);
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT, "PLANO_B tipo=%s origem=%s lado=%s pos=%s dist=%.1f aVista=%s",
				tipo, origem, lugar.lado(), Diretor.pos(onde.x, onde.y, onde.z), Math.sqrt(p.distanceToSqr(onde)),
				lugar.aVista() ? "sim" : "nao"));
		return tipo.name();
	}

	/**
	 * Qual sinal sai. À vista do jogador, o que se vê vem primeiro (o pó, depois a tocha); fora da vista, o que se
	 * ouve (os passos, depois a tocha, que muda a luz do lugar). Havendo escolha, nunca o mesmo duas vezes seguidas.
	 * Função pura, para poder ser testada sem mundo.
	 */
	static Tipo escolherTipo(boolean temTeto, boolean temTocha, boolean aVista, @Nullable Tipo ultimo) {
		List<Tipo> opcoes = new ArrayList<>();
		if (aVista) {
			if (temTeto) {
				opcoes.add(Tipo.PO);
			}
			if (temTocha) {
				opcoes.add(Tipo.TOCHA);
			}
			opcoes.add(Tipo.PASSOS);
		} else {
			opcoes.add(Tipo.PASSOS);
			if (temTocha) {
				opcoes.add(Tipo.TOCHA);
			}
		}
		for (Tipo tipo : opcoes) {
			if (tipo != ultimo) {
				return tipo;
			}
		}
		return opcoes.get(0);
	}

	// ----- Onde -----

	@Nullable
	private static Lugar escolherLugar(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick) {
		// 1. Onde a procura que acabou de falhar quis pô-lo.
		Vec3 olho = p.getEyePosition();
		Aparicao.Quase quase = e.quaseLugar;
		if (quase != null && tick - quase.tick() <= 20) {
			BlockPos chao = quase.chao();
			Vec3 pes = new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
			String lado = "QUASE_" + quase.motivo().toUpperCase(Locale.ROOT);
			boolean aVista = quase.aVista();
			double dx = pes.x - p.getX();
			double dz = pes.z - p.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > LONGE) {
				// A procura de sempre tenta lugares a trinta blocos (outro bolsão da caverna, o lado de lá de um
				// morro). Passos a trinta blocos não são com ninguém: o sinal vem do mesmo lado, a nove.
				double x = p.getX() + dx / dist * PERTO;
				double z = p.getZ() + dz / dist * PERTO;
				chao = chaoNoNivel(level, p, x, z);
				pes = chao != null ? new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5) : new Vec3(x, p.getY(), z);
				aVista = aVista && Aparicao.linhaLivre(level, olho, pes.add(0, 1.2, 0));
				lado += "_PERTO";
			}
			if (!Diretor.emZonaCalma(p, pes.x, pes.y, pes.z)) {
				return new Lugar(pes, chao, lado, aVista);
			}
		}
		// 2. Um lugar por onde o jogador passou: de preferência fora da tela, e o mais perto de nove blocos.
		Rastro.Ponto melhor = null;
		double melhorNota = Double.NEGATIVE_INFINITY;
		for (Rastro.Ponto pt : e.rastro.comIdade(seg, 8, 600)) {
			double d = Math.sqrt(p.distanceToSqr(pt.x(), pt.y(), pt.z()));
			if (d < 5 || d > 14 || Diretor.emZonaCalma(p, pt.x(), pt.y(), pt.z())) {
				continue;
			}
			boolean naFrente = Diretor.pontoNaFrente(p, new Vec3(pt.x(), pt.y() + 1.2, pt.z()), Percepcao.coneSeguro(p));
			double nota = (naFrente ? 0.0 : 10.0) - Math.abs(d - 9.0);
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = pt;
			}
		}
		if (melhor != null) {
			Vec3 pes = new Vec3(melhor.x(), melhor.y(), melhor.z());
			boolean aVista = Diretor.pontoNaFrente(p, pes.add(0, 1.2, 0), Percepcao.coneSeguro(p))
					&& Aparicao.linhaLivre(level, olho, pes.add(0, 1.2, 0));
			return new Lugar(pes, BlockPos.containing(pes.x, pes.y + 0.2, pes.z), "RASTRO", aVista);
		}
		// 3. Atrás dele. Cada vez mais longe, até sair da zona da vela.
		for (double dist : new double[] {6.0, 10.0, 14.0}) {
			Vec3 v = Diretor.pontoRelativo(p, 180, dist);
			BlockPos chao = chaoNoNivel(level, p, v.x, v.z);
			Vec3 pes = chao != null ? new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5) : v;
			if (!Diretor.emZonaCalma(p, pes.x, pes.y, pes.z)) {
				return new Lugar(pes, chao, "ATRAS", false);
			}
		}
		return null;
	}

	/**
	 * O chão daquele ponto, se ficar mais ou menos na altura do jogador (até quatro blocos acima ou abaixo). A
	 * procura de chão desce doze blocos: numa caverna ela acharia o andar de baixo, e o sinal viria de lá.
	 */
	@Nullable
	private static BlockPos chaoNoNivel(ServerLevel level, ServerPlayer p, double x, double z) {
		BlockPos chao = Diretor.acharChao(level, x, p.getY(), z);
		return chao != null && Math.abs(chao.getY() - p.getY()) <= 4 ? chao : null;
	}

	/** O primeiro bloco firme acima de um lugar, de dois a sete blocos do chão. Copa de árvore não é teto. */
	@Nullable
	private static BlockPos tetoDe(ServerLevel level, BlockPos chao) {
		for (int dy = 1; dy <= 7; dy++) {
			BlockPos pos = chao.above(dy);
			BlockState s = level.getBlockState(pos);
			if (s.getCollisionShape(level, pos).isEmpty()) {
				continue; // ar, capim, uma tocha na parede: o teto é o que vem depois
			}
			return dy >= 2 && s.isCollisionShapeFullBlock(level, pos) && !s.is(BlockTags.LEAVES) ? pos : null;
		}
		return null;
	}

	/** Uma tocha de verdade daquele lado: a até oito blocos do lugar e dezesseis do jogador. A mais perto do lugar. */
	@Nullable
	private static BlockPos tochaPerto(ServerLevel level, ServerPlayer p, EstadoJogador e, Vec3 lugar) {
		Diretor.atualizarCacheAmbiente(level, p, e);
		BlockPos melhor = null;
		double melhorDist = 8.0 * 8.0;
		for (BlockPos pos : e.tochas) {
			BlockState s = level.getBlockState(pos);
			if (!s.is(Blocks.TORCH) && !s.is(Blocks.WALL_TORCH)) {
				continue;
			}
			Vec3 centro = Vec3.atCenterOf(pos);
			double d = centro.distanceToSqr(lugar);
			if (d >= melhorDist || p.distanceToSqr(centro) > 16 * 16 || Miragem.tem(p, pos)
					|| Diretor.emZonaCalma(p, centro.x, centro.y, centro.z)) {
				continue;
			}
			melhorDist = d;
			melhor = pos.immutable();
		}
		return melhor;
	}

	// ----- O quê -----

	/** Cai pó do teto: três punhados em um segundo, e um toque surdo no bloco de onde ele sai. */
	private static Vec3 po(ServerLevel level, ServerPlayer p, BlockPos teto) {
		BlockState bloco = level.getBlockState(teto);
		double x = teto.getX() + 0.5;
		double y = teto.getY() - 0.05;
		double z = teto.getZ() + 0.5;
		for (int i = 0; i < 3; i++) {
			Diretor.agendar(level, i * 9, () -> {
				if (p.isRemoved() || p.level() != level) {
					return;
				}
				level.sendParticles(p, new BlockParticleOption(ParticleTypes.FALLING_DUST, bloco), false, false,
						x, y, z, 9, 0.22, 0.0, 0.22, 0.0);
				level.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, bloco), false, false,
						x, y, z, 4, 0.18, 0.0, 0.18, 0.02);
			});
		}
		ModSons.tocarEventoPara(p, bloco.getSoundType().getHitSound(), SoundSource.BLOCKS, x, y, z,
				Diretor.volumePara(p, x, y, z, 0.5F), 0.7F);
		return new Vec3(x, y, z);
	}

	/** A tocha apaga por uns segundos, com o som de uma chama apagando. Miragem: o mundo não muda. */
	private static Vec3 apagarTocha(ServerLevel level, ServerPlayer p, BlockPos tocha, long seg) {
		Vec3 centro = Vec3.atCenterOf(tocha);
		// De quatro a sete segundos. Sai do relógio, não de um sorteio.
		Miragem.mostrar(level, p, tocha, Blocks.AIR.defaultBlockState(), 80 + 20 * (seg % 4), 0, MIRAGEM);
		ModSons.tocarEventoPara(p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, centro.x, centro.y, centro.z,
				Diretor.volumePara(p, centro.x, centro.y, centro.z, 0.5F), 0.9F);
		return centro;
	}

	/** Dois passos se afastam do jogador, a partir do lugar, com o som do chão de verdade em cada um. */
	private static Vec3 passos(ServerLevel level, ServerPlayer p, Lugar lugar) {
		Vec3 pes = lugar.pes();
		double dx = pes.x - p.getX();
		double dz = pes.z - p.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-4) {
			dx = 0;
			dz = 1;
			len = 1;
		}
		dx /= len;
		dz /= len;
		for (int i = 0; i < 2; i++) {
			double x = pes.x + dx * 1.4 * i;
			double z = pes.z + dz * 1.4 * i;
			BlockPos sob = BlockPos.containing(x, pes.y - 0.5, z);
			if (level.getBlockState(sob).getCollisionShape(level, sob).isEmpty()) {
				sob = BlockPos.containing(pes.x, pes.y - 0.5, pes.z); // o segundo passo cairia no vazio: o chão do primeiro
			}
			SoundEvent passo = level.getBlockState(sob).getSoundType().getStepSound();
			float volume = Diretor.volumePara(p, x, pes.y, z, 0.9F);
			Diretor.agendar(level, i * 9, () -> {
				if (!p.isRemoved() && p.level() == level) {
					ModSons.tocarEventoPara(p, passo, SoundSource.HOSTILE, x, pes.y, z, volume, 0.6F);
				}
			});
		}
		return pes;
	}
}
