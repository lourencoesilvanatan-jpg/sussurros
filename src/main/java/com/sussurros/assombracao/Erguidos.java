package com.sussurros.assombracao;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.sussurros.registro.ModSons;

/**
 * O que o mod ergue no mundo (0.9): duas coisas que o jogador encontra.
 *
 * A SOLEIRA. Uma porta sozinha na paisagem, entre dois batentes de pedra, longe da base. Uma por jogador.
 * Atravessá-la não leva a lugar nenhum. A primeira travessia responde com um grave baixo; a segunda, com alguns
 * segundos de silêncio; a terceira abre o Véu, a porta bate atrás dele e a soleira descansa até o dia seguinte.
 *
 * O BONECO. Uma figura de palha (estaca, fardo de feno, abóbora esculpida) com o rosto virado para a cama do
 * jogador. Aparece longe e, a cada noite, está mais perto, sempre vindo da mesma direção. Derrubá-lo não
 * resolve: na noite seguinte está de pé, um passo adiante. Fica duas noites no último degrau e some; dias
 * depois volta, de outro lado. É progressão que se vê da janela.
 *
 * Os dois usam só blocos do próprio jogo, só em lugar vazio e sobre chão natural, nunca dentro de casa, e só
 * são postos ou movidos com ninguém olhando. Isto é apresentação: usa o gerador do jogador, não o do mundo.
 */
public final class Erguidos {
	static final String SOLEIRA = "soleira";
	static final String SOLEIRA_X = "soleira_x";
	static final String SOLEIRA_Y = "soleira_y";
	static final String SOLEIRA_Z = "soleira_z";
	static final String SOLEIRA_EIXO = "soleira_eixo";         // 0: atravessa-se de norte a sul; 1: de leste a oeste
	static final String SOLEIRA_TRAVESSIAS = "soleira_travessias";
	static final String SOLEIRA_DIA = "soleira_dia";           // o dia em que ela abriu o Véu pela última vez

	static final String BONECO = "boneco";
	static final String BONECO_X = "boneco_x";
	static final String BONECO_Y = "boneco_y";
	static final String BONECO_Z = "boneco_z";
	static final String BONECO_PASSO = "boneco_passo";
	static final String BONECO_ANG = "boneco_ang";
	static final String BONECO_DIA = "boneco_dia";             // a última noite em que ele andou
	static final String BONECO_VOLTA = "boneco_volta";         // o dia a partir do qual pode começar de novo

	/** A que distância da cama ele está em cada noite. As duas últimas são o mesmo degrau: ele fica. */
	static final int[] DISTANCIAS = {48, 38, 29, 21, 14, 9, 9};

	private Erguidos() {
	}

	/** Uma vez por segundo, dentro do tick do Diretor. */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, boolean noite, long seg, long tick) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}
		soleira(level, p, m, e, fase, seg, tick);
		if (noite && seg % 5 == 3 && Math.floorMod(level.getDefaultClockTime(), 24000L) >= 14000) {
			noiteDoBoneco(level, p, m, e, fase, (int) (level.getDefaultClockTime() / 24000L), false);
		}
	}

	/** Quem dormiu a noite inteira pulou a hora em que ele anda: anda ao acordar, enquanto ninguém olhava. */
	static void aoAcordar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase) {
		long relogio = level.getDefaultClockTime();
		int dia = (int) (relogio / 24000L) - (Math.floorMod(relogio, 24000L) < 12000 ? 1 : 0);
		noiteDoBoneco(level, p, m, e, fase, dia, true);
	}

	// =====================================================================
	// A Soleira
	// =====================================================================

	private static void soleira(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg, long tick) {
		if (m.get(SOLEIRA) != 1) {
			if (fase < 2 || seg % 20 != 7 || e.forcando || EstruturasSussurros.temCenaAtiva(e)
					|| m.get(Memoria.CHUNKS_VISITADOS) < 12 || p.getRandom().nextFloat() >= 0.2F) {
				return;
			}
			BlockPos pos = EstruturasSussurros.procurarSuperficie(level, p, p.getRandom(), 60, 100, 2);
			if (pos != null && EstruturasSussurros.longeDaCasa(m, pos, 60)) {
				erguerSoleira(level, m, pos, p.getRandom().nextInt(2), p.getRandom());
				Depuracao.log(p, seg, "SOLEIRA erguida pos=" + pos.toShortString() + " eixo=" + m.get(SOLEIRA_EIXO));
			}
			return;
		}

		BlockPos porta = new BlockPos(m.get(SOLEIRA_X), m.get(SOLEIRA_Y), m.get(SOLEIRA_Z));
		boolean deNorteASul = m.get(SOLEIRA_EIXO) == 0;
		double ax = p.getX() - (porta.getX() + 0.5);
		double az = p.getZ() - (porta.getZ() + 0.5);
		double frente = deNorteASul ? az : ax;
		double lado = deNorteASul ? ax : az;
		if (Math.abs(frente) > 8 || Math.abs(lado) > 8 || Math.abs(p.getY() - porta.getY()) > 3) {
			e.soleiraFrente = Double.NaN;
			return;
		}
		if (!(level.getBlockState(porta).getBlock() instanceof DoorBlock)) {
			// Alguém a desmontou. Ela volta a existir em outro lugar, outro dia.
			m.set(SOLEIRA, 0);
			m.set(SOLEIRA_TRAVESSIAS, 0);
			e.soleiraFrente = Double.NaN;
			Depuracao.log(p, seg, "SOLEIRA desfeita (a porta não está mais lá)");
			return;
		}
		// Atravessou? Entre uma olhada e outra ele mudou de lado, e a reta entre as duas posições passa pelo vão.
		if (!Double.isNaN(e.soleiraFrente) && e.soleiraFrente * frente < 0) {
			double t = e.soleiraFrente / (e.soleiraFrente - frente);
			double ladoNoVao = e.soleiraLado + (lado - e.soleiraLado) * t;
			if (Math.abs(ladoNoVao) <= 0.6) {
				atravessou(level, p, m, e, porta, seg, tick);
			}
		}
		e.soleiraFrente = frente;
		e.soleiraLado = lado;
	}

	private static void atravessou(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, BlockPos porta, long seg, long tick) {
		int dia = (int) (level.getDefaultClockTime() / 24000L);
		if (m.get(SOLEIRA_DIA, -1) == dia) {
			Depuracao.log(p, seg, "SOLEIRA atravessada (descansando hoje)");
			return;
		}
		int n = m.get(SOLEIRA_TRAVESSIAS) + 1;
		String resposta;
		if (n == 1) {
			ModSons.tocarPara(p, porta.getX() + 0.5, porta.getY() + 1.0, porta.getZ() + 0.5, ModSons.Som.GRAVE, 0.35F, 0.7F);
			resposta = "GRAVE";
		} else if (n == 2) {
			Diretor.emudecer(level, p, 5, "SOLEIRA");
			e.semMusicaAte = tick + 20L * 5;
			resposta = "SILENCIO";
		} else if (Veu.pode(level, p, e, tick, false)) {
			Veu.abrir(level, p, m, e, tick, false);
			bater(level, porta);
			m.set(SOLEIRA_DIA, dia);
			n = 0;
			resposta = "VEU";
		} else {
			// Não dá para abrir agora (criatura presente, vela acesa). A próxima travessia tenta de novo.
			n = 2;
			resposta = "NAO_ABRIU";
		}
		m.set(SOLEIRA_TRAVESSIAS, n);
		Depuracao.log(p, seg, "SOLEIRA atravessada n=" + n + " resposta=" + resposta);
	}

	/** A porta bate: se estava aberta, fecha. O som é do mundo. */
	private static void bater(ServerLevel level, BlockPos porta) {
		BlockState s = level.getBlockState(porta);
		if (s.getBlock() instanceof DoorBlock bloco && s.getValue(DoorBlock.OPEN)) {
			bloco.setOpen(null, level, s, porta, false);
		}
		level.playSound(null, porta, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
	}

	/** Dois batentes de pedra, uma verga e a porta. pos é o bloco de ar onde fica a metade de baixo da porta. */
	static void erguerSoleira(ServerLevel level, Memoria m, BlockPos pos, int eixo, RandomSource sorte) {
		int lx = eixo == 0 ? 1 : 0;
		int lz = eixo == 0 ? 0 : 1;
		for (int lado = -1; lado <= 1; lado += 2) {
			BlockPos pe = pos.offset(lx * lado, 0, lz * lado);
			// Em chão torto, o batente desce até achar apoio (no máximo três blocos).
			for (int baixo = 1; baixo <= 3 && level.isEmptyBlock(pe.below(baixo)); baixo++) {
				level.setBlock(pe.below(baixo), pedra(sorte), 3);
			}
			colocarSeVazio(level, pe, pedra(sorte));
			colocarSeVazio(level, pe.above(), pedra(sorte));
			colocarSeVazio(level, pe.above(2), pedra(sorte));
		}
		colocarSeVazio(level, pos.above(2), pedra(sorte));
		BlockState porta = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, eixo == 0 ? Direction.NORTH : Direction.EAST);
		if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
			level.setBlock(pos, porta.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
			level.setBlock(pos.above(), porta.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
		}
		m.set(SOLEIRA, 1);
		m.set(SOLEIRA_X, pos.getX());
		m.set(SOLEIRA_Y, pos.getY());
		m.set(SOLEIRA_Z, pos.getZ());
		m.set(SOLEIRA_EIXO, eixo);
		m.set(SOLEIRA_TRAVESSIAS, 0);
	}

	private static BlockState pedra(RandomSource sorte) {
		int r = sorte.nextInt(10);
		return (r < 5 ? Blocks.STONE_BRICKS : r < 8 ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
	}

	private static void colocarSeVazio(ServerLevel level, BlockPos pos, BlockState estado) {
		if (level.isEmptyBlock(pos)) {
			level.setBlock(pos, estado, 3);
		}
	}

	// =====================================================================
	// O Boneco
	// =====================================================================

	/**
	 * Uma noite do boneco: tira o de ontem e o põe um passo mais perto. Só uma vez por noite, e só quando nem o
	 * lugar velho nem o novo estão na tela ("semOlhar": ele acabou de acordar, ou é teste).
	 * Recebe a Memoria de quem chamou e não a salva. Devolve o que fez, para o log e os testes.
	 */
	static String noiteDoBoneco(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, int dia, boolean semOlhar) {
		if (fase < 2 || m.get(Memoria.TEM_CAMA) != 1 || m.get(BONECO_DIA, -1) == dia) {
			return "NADA";
		}
		BlockPos cama = new BlockPos(m.get(Memoria.CAMA_X), m.get(Memoria.CAMA_Y), m.get(Memoria.CAMA_Z));
		boolean ativo = m.get(BONECO) == 1;
		if (!ativo && dia < m.get(BONECO_VOLTA)) {
			return "NADA";
		}
		BlockPos velho = ativo ? new BlockPos(m.get(BONECO_X), m.get(BONECO_Y), m.get(BONECO_Z)) : null;
		if (velho != null && !semOlhar && naTela(p, velho)) {
			return "OLHANDO";
		}
		RandomSource sorte = p.getRandom();
		int passo = ativo ? m.get(BONECO_PASSO) : 0;
		if (!ativo) {
			m.set(BONECO_ANG, sorte.nextInt(360));
		}
		String feito;
		if (passo >= DISTANCIAS.length) {
			// Ficou as duas noites na porta. Some, e só volta dias depois, de outro lado.
			tirarBoneco(level, velho);
			m.set(BONECO, 0);
			m.set(BONECO_PASSO, 0);
			m.set(BONECO_VOLTA, dia + 5 + sorte.nextInt(4));
			feito = "SUMIU";
		} else {
			BlockPos novo = lugarDoBoneco(level, p, cama, m.get(BONECO_ANG), DISTANCIAS[passo], velho, semOlhar, sorte);
			if (novo == null) {
				return "SEM_LUGAR";
			}
			tirarBoneco(level, velho);
			Direction rosto = Direction.getApproximateNearest(cama.getX() - novo.getX(), 0.0, cama.getZ() - novo.getZ());
			if (rosto.getAxis().isVertical()) {
				rosto = Direction.NORTH;
			}
			level.setBlock(novo, Blocks.OAK_FENCE.defaultBlockState(), 3);
			level.setBlock(novo.above(), Blocks.HAY_BLOCK.defaultBlockState(), 3);
			level.setBlock(novo.above(2), Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, rosto), 3);
			m.set(BONECO, 1);
			m.set(BONECO_X, novo.getX());
			m.set(BONECO_Y, novo.getY());
			m.set(BONECO_Z, novo.getZ());
			m.set(BONECO_PASSO, passo + 1);
			// No último degrau ele pesa: a obsessão sobe um pouco a cada noite que ele passa ali.
			if (passo >= DISTANCIAS.length - 2) {
				e.obsessao = Diretor.limitar(e.obsessao + 2.0, 0, 100);
			}
			feito = String.format(Locale.ROOT, "ANDOU passo=%d dist=%d pos=%s rosto=%s", passo + 1, DISTANCIAS[passo],
					novo.toShortString(), rosto);
		}
		m.set(BONECO_DIA, dia);
		Depuracao.log(p, level.getGameTime() / 20, "BONECO " + feito);
		return feito;
	}

	/** Tira o boneco, se os blocos ainda são os dele. O que o jogador mexeu fica como ele deixou. */
	private static void tirarBoneco(ServerLevel level, @Nullable BlockPos pos) {
		if (pos == null || !level.isLoaded(pos)) {
			return;
		}
		if (level.getBlockState(pos.above(2)).is(Blocks.CARVED_PUMPKIN)) {
			level.removeBlock(pos.above(2), false);
		}
		if (level.getBlockState(pos.above()).is(Blocks.HAY_BLOCK)) {
			level.removeBlock(pos.above(), false);
		}
		if (level.getBlockState(pos).is(Blocks.OAK_FENCE)) {
			level.removeBlock(pos, false);
		}
	}

	/** Um lugar a essa distância da cama, na direção dele: chão natural, a céu aberto, três blocos livres, fora da tela. */
	@Nullable
	private static BlockPos lugarDoBoneco(ServerLevel level, ServerPlayer p, BlockPos cama, int angulo, int distancia,
			@Nullable BlockPos velho, boolean semOlhar, RandomSource sorte) {
		for (int i = 0; i < 14; i++) {
			double a = Math.toRadians(angulo + (i == 0 ? 0 : sorte.nextInt(25) - 12));
			double d = distancia + (i == 0 ? 0 : sorte.nextInt(5) - 2);
			int x = (int) Math.floor(cama.getX() + 0.5 - Math.sin(a) * d);
			int z = (int) Math.floor(cama.getZ() + 0.5 + Math.cos(a) * d);
			if (!level.isLoaded(new BlockPos(x, cama.getY(), z))) {
				continue;
			}
			BlockPos base = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (base.equals(velho) || Math.abs(base.getY() - cama.getY()) > 12) {
				continue;
			}
			BlockState chao = level.getBlockState(base.below());
			// Na 26.2 a grama saiu da tag "dirt" e ganhou a sua.
			boolean natural = chao.is(BlockTags.DIRT) || chao.is(BlockTags.GRASS_BLOCKS) || chao.is(BlockTags.SAND) || chao.is(BlockTags.BASE_STONE_OVERWORLD)
					|| chao.is(BlockTags.TERRACOTTA) || chao.is(Blocks.GRAVEL) || chao.is(Blocks.SNOW_BLOCK);
			if (!natural || !level.isEmptyBlock(base) || !level.isEmptyBlock(base.above()) || !level.isEmptyBlock(base.above(2))
					|| !level.canSeeSky(base)) {
				continue;
			}
			if (!semOlhar && naTela(p, base)) {
				continue;
			}
			return base;
		}
		return null;
	}

	private static boolean naTela(ServerPlayer p, BlockPos pos) {
		return p.distanceToSqr(Vec3.atCenterOf(pos.above())) < 96 * 96 && EstruturasSussurros.naTela(p, Vec3.atCenterOf(pos.above()));
	}

	// =====================================================================
	// Testes por comando
	// =====================================================================

	/** Ergue a soleira cinco blocos à frente de quem pediu, atravessada na direção em que ele olha. */
	static String testarSoleira(ServerLevel level, ServerPlayer p, Memoria m) {
		Vec3 frente = Diretor.pontoRelativo(p, 0, 5.0);
		BlockPos chao = Diretor.acharChao(level, frente.x, p.getY(), frente.z);
		if (chao == null) {
			return "não há chão livre cinco blocos à sua frente.";
		}
		int eixo = p.getDirection().getAxis() == Direction.Axis.Z ? 0 : 1;
		erguerSoleira(level, m, chao, eixo, p.getRandom());
		return "Soleira erguida em " + chao.toShortString() + ".";
	}

	/** Faz o boneco andar uma noite agora, sem esperar a noite nem conferir se alguém olha. */
	static String testarBoneco(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e) {
		if (m.get(Memoria.TEM_CAMA) != 1) {
			return "precisa de uma cama conhecida (durma uma vez).";
		}
		m.set(BONECO_DIA, -1);
		m.set(BONECO_VOLTA, 0);
		return "Boneco: " + noiteDoBoneco(level, p, m, e, Math.max(2, m.get(Memoria.FASE)), (int) (level.getDefaultClockTime() / 24000L), true);
	}
}
