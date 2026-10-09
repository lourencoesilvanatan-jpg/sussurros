package com.sussurros.assombracao;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.registro.ModItems;

/**
 * Pequenas estruturas narrativas geradas em runtime e sempre fora da tela. São deliberadamente
 * compactas: conteúdo para descobrir, não dungeons nem substituto do worldgen vanilla.
 */
final class EstruturasSussurros {
	private EstruturasSussurros() {
	}

	static void verificar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean subterraneo, long seg, RandomSource rnd) {
		verificarDescobertas(p, m, seg);
		if (seg % 20 != 0 || e.forcando || temCenaAtiva(e)) {
			return;
		}
		int chunks = m.get(Memoria.CHUNKS_VISITADOS);
		if (m.get(Memoria.ESTRUTURA_MARCO) == 0 && chunks >= 5 && rnd.nextFloat() < 0.20F) {
			BlockPos pos = procurarSuperficie(level, p, rnd, 42, 72, 3);
			if (pos != null && longeDaCasa(m, pos, 40) && gerarMarco(level, pos, rnd)) {
				registrar(m, Memoria.ESTRUTURA_MARCO, Memoria.ESTRUTURA_MARCO_X, Memoria.ESTRUTURA_MARCO_Y,
						Memoria.ESTRUTURA_MARCO_Z, pos);
				Depuracao.log(p, seg, "ESTRUTURA gerada=MARCO_ESTRADA pos=" + pos + " foraDaTela=sim");
			}
		}
		if (fase >= 1 && m.get(Memoria.ESTRUTURA_POSTO) == 0 && chunks >= 10 && rnd.nextFloat() < 0.16F) {
			BlockPos pos = procurarSuperficie(level, p, rnd, 55, 95, 6);
			if (pos != null && longeDaCasa(m, pos, 52) && gerarPosto(level, pos, rnd)) {
				registrar(m, Memoria.ESTRUTURA_POSTO, Memoria.ESTRUTURA_POSTO_X, Memoria.ESTRUTURA_POSTO_Y,
						Memoria.ESTRUTURA_POSTO_Z, pos);
				Depuracao.log(p, seg, "ESTRUTURA gerada=POSTO_VIGILIA pos=" + pos + " foraDaTela=sim");
			}
		}
		if (fase >= 2 && subterraneo && m.get(Memoria.ESTRUTURA_NICHO) == 0 && rnd.nextFloat() < 0.18F) {
			BlockPos pos = procurarCaverna(level, p, rnd);
			if (pos != null && gerarNicho(level, pos, rnd)) {
				registrar(m, Memoria.ESTRUTURA_NICHO, Memoria.ESTRUTURA_NICHO_X, Memoria.ESTRUTURA_NICHO_Y,
						Memoria.ESTRUTURA_NICHO_Z, pos);
				Depuracao.log(p, seg, "ESTRUTURA gerada=NICHO_SELADO pos=" + pos + " foraDaTela=sim");
			}
		}
		m.salvar();
	}

	static String testar(ServerLevel level, ServerPlayer p, String tipo) {
		Memoria m = Memoria.de(p);
		RandomSource rnd = level.getRandom();
		BlockPos pos;
		boolean ok;
		switch (tipo) {
			case "marco" -> {
				pos = procurarSuperficie(level, p, rnd, 18, 32, 3);
				ok = pos != null && gerarMarco(level, pos, rnd);
				if (ok) registrar(m, Memoria.ESTRUTURA_MARCO, Memoria.ESTRUTURA_MARCO_X, Memoria.ESTRUTURA_MARCO_Y, Memoria.ESTRUTURA_MARCO_Z, pos);
			}
			case "posto" -> {
				pos = procurarSuperficie(level, p, rnd, 24, 40, 6);
				ok = pos != null && gerarPosto(level, pos, rnd);
				if (ok) registrar(m, Memoria.ESTRUTURA_POSTO, Memoria.ESTRUTURA_POSTO_X, Memoria.ESTRUTURA_POSTO_Y, Memoria.ESTRUTURA_POSTO_Z, pos);
			}
			case "nicho" -> {
				pos = procurarCaverna(level, p, rnd);
				ok = pos != null && gerarNicho(level, pos, rnd);
				if (ok) registrar(m, Memoria.ESTRUTURA_NICHO, Memoria.ESTRUTURA_NICHO_X, Memoria.ESTRUTURA_NICHO_Y, Memoria.ESTRUTURA_NICHO_Z, pos);
			}
			default -> {
				return "Estrutura desconhecida.";
			}
		}
		m.salvar();
		return ok ? "Estrutura " + tipo + " gerada fora da tela." : "Não achei um lugar seguro para a estrutura " + tipo + ".";
	}

	private static boolean gerarMarco(ServerLevel level, BlockPos base, RandomSource rnd) {
		if (!livre(level, base, 2, 3)) return false;
		colocarSeVazio(level, base, rnd.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
		colocarSeVazio(level, base.above(), Blocks.COBBLESTONE_WALL.defaultBlockState());
		colocarSeVazio(level, base.above(2), Blocks.OAK_FENCE.defaultBlockState());
		BlockPos lateral = base.offset(rnd.nextBoolean() ? 1 : -1, 0, rnd.nextBoolean() ? 1 : -1);
		colocarSeVazio(level, lateral, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
		return true;
	}

	private static boolean gerarPosto(ServerLevel level, BlockPos base, RandomSource rnd) {
		if (!livre(level, base, 3, 4)) return false;
		// piso quebrado 5x5
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (Math.abs(x) == 2 && Math.abs(z) == 2 && rnd.nextBoolean()) continue;
				BlockPos p = base.offset(x, 0, z);
				if (level.isEmptyBlock(p)) level.setBlock(p, rnd.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), 3);
			}
		}
		// dois postes e uma parede incompleta
		for (int y = 1; y <= 3; y++) {
			colocarSeVazio(level, base.offset(-2, y, -2), Blocks.OAK_LOG.defaultBlockState());
			colocarSeVazio(level, base.offset(2, y, -2), Blocks.OAK_LOG.defaultBlockState());
		}
		for (int x = -1; x <= 1; x++) {
			colocarSeVazio(level, base.offset(x, 1, -2), Blocks.OAK_PLANKS.defaultBlockState());
		}
		BlockPos barrel = base.offset(1, 1, 1);
		if (level.isEmptyBlock(barrel)) {
			level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), 3);
			if (level.getBlockEntity(barrel) instanceof Container c) {
				c.setItem(0, new ItemStack(ModItems.PAGINA_RASGADA));
				c.setItem(3, new ItemStack(ModItems.CINZA_PALIDA, 1 + rnd.nextInt(2)));
				if (rnd.nextFloat() < 0.45F) c.setItem(5, new ItemStack(ModItems.CADERNO_VESTIGIOS));
				if (rnd.nextFloat() < 0.25F) c.setItem(7, new ItemStack(ModItems.FIO_VIGILIA));
			}
		}
		colocarSeVazio(level, base.offset(-1, 1, 1), Blocks.CANDLE.defaultBlockState());
		return true;
	}

	private static boolean gerarNicho(ServerLevel level, BlockPos base, RandomSource rnd) {
		if (!livre(level, base, 2, 3)) return false;
		for (int x = -1; x <= 1; x++) {
			BlockPos piso = base.offset(x, 0, 0);
			if (level.isEmptyBlock(piso)) level.setBlock(piso, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
			for (int y = 1; y <= 2; y++) {
				BlockPos fundo = base.offset(x, y, 1);
				if (level.isEmptyBlock(fundo)) level.setBlock(fundo,
						x == 0 && y == 1 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
			}
		}
		BlockPos barrel = base.offset(0, 1, 0);
		if (level.isEmptyBlock(barrel)) {
			level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), 3);
			if (level.getBlockEntity(barrel) instanceof Container c) {
				c.setItem(0, new ItemStack(ModItems.PAGINA_RASGADA));
				c.setItem(2, new ItemStack(ModItems.CINZA_PALIDA, 1 + rnd.nextInt(3)));
				if (rnd.nextFloat() < 0.35F) c.setItem(4, new ItemStack(ModItems.SINO_OCO));
			}
		}
		return true;
	}

	@Nullable
	static BlockPos procurarSuperficie(ServerLevel level, ServerPlayer p, RandomSource rnd, int minDist, int maxDist, int raioLivre) {
		for (int tentativa = 0; tentativa < 20; tentativa++) {
			double ang = Math.toRadians(p.getYRot() + 95 + rnd.nextDouble() * 170);
			double dist = minDist + rnd.nextInt(Math.max(1, maxDist - minDist + 1));
			int x = (int) Math.floor(p.getX() - Math.sin(ang) * dist);
			int z = (int) Math.floor(p.getZ() + Math.cos(ang) * dist);
			int topo = Math.min(320, p.getBlockY() + 28);
			// Até um bloco acima do fundo do mundo (era -60 fixo, e num mundo plano o chão fica em -61).
			int baixo = Math.max(level.getMinY() + 1, p.getBlockY() - 36);
			for (int y = topo; y >= baixo; y--) {
				BlockPos base = new BlockPos(x, y, z);
				if (level.getBlockState(base).getCollisionShape(level, base).isEmpty()) continue;
				if (!level.isEmptyBlock(base.above()) || !level.canSeeSky(base.above(2))) continue;
				if (naTela(p, Vec3.atCenterOf(base.above()))) continue;
				if (livre(level, base.above(), raioLivre, 4)) return base.above();
				break;
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos procurarCaverna(ServerLevel level, ServerPlayer p, RandomSource rnd) {
		for (int tentativa = 0; tentativa < 28; tentativa++) {
			double ang = Math.toRadians(p.getYRot() + 80 + rnd.nextDouble() * 200);
			double dist = 16 + rnd.nextInt(22);
			BlockPos base = BlockPos.containing(p.getX() - Math.sin(ang) * dist,
					p.getY() - 2 + rnd.nextInt(5), p.getZ() + Math.cos(ang) * dist);
			if (naTela(p, Vec3.atCenterOf(base))) continue;
			if (livre(level, base, 2, 3) && !level.getBlockState(base.below()).getCollisionShape(level, base.below()).isEmpty()) {
				return base;
			}
		}
		return null;
	}

	private static boolean livre(ServerLevel level, BlockPos centro, int raio, int altura) {
		for (int x = -raio; x <= raio; x++) {
			for (int z = -raio; z <= raio; z++) {
				for (int y = 0; y < altura; y++) {
					BlockPos pos = centro.offset(x, y, z);
					if (!level.isEmptyBlock(pos) && y > 0) return false;
				}
			}
		}
		return true;
	}

	private static void colocarSeVazio(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.isEmptyBlock(pos)) level.setBlock(pos, state, 3);
	}

	private static void registrar(Memoria m, String flag, String x, String y, String z, BlockPos pos) {
		m.set(flag, 1);
		m.set(x, pos.getX());
		m.set(y, pos.getY());
		m.set(z, pos.getZ());
	}

	private static void verificarDescobertas(ServerPlayer p, Memoria m, long seg) {
		verificarDescoberta(p, m, Memoria.ESTRUTURA_MARCO, Memoria.ESTRUTURA_MARCO_X, Memoria.ESTRUTURA_MARCO_Y,
				Memoria.ESTRUTURA_MARCO_Z, Memoria.ESTRUTURA_MARCO_VISTA, "MARCO_ESTRADA", seg);
		verificarDescoberta(p, m, Memoria.ESTRUTURA_POSTO, Memoria.ESTRUTURA_POSTO_X, Memoria.ESTRUTURA_POSTO_Y,
				Memoria.ESTRUTURA_POSTO_Z, Memoria.ESTRUTURA_POSTO_VISTA, "POSTO_VIGILIA", seg);
		verificarDescoberta(p, m, Memoria.ESTRUTURA_NICHO, Memoria.ESTRUTURA_NICHO_X, Memoria.ESTRUTURA_NICHO_Y,
				Memoria.ESTRUTURA_NICHO_Z, Memoria.ESTRUTURA_NICHO_VISTA, "NICHO_SELADO", seg);
	}

	private static void verificarDescoberta(ServerPlayer p, Memoria m, String flag, String xk, String yk, String zk,
			String vista, String nome, long seg) {
		if (m.get(flag) == 0 || m.get(vista) != 0) return;
		double dx = p.getX() - (m.get(xk) + 0.5);
		double dy = p.getY() - (m.get(yk) + 0.5);
		double dz = p.getZ() - (m.get(zk) + 0.5);
		if (dx * dx + dy * dy + dz * dz <= 14 * 14) {
			m.set(vista, 1);
			Depuracao.log(p, seg, "ESTRUTURA descoberta=" + nome + " pos=(" + m.get(xk) + "," + m.get(yk) + "," + m.get(zk) + ")");
		}
	}

	static boolean longeDaCasa(Memoria m, BlockPos pos, double raio) {
		double r2 = raio * raio;
		if (m.get(Memoria.TEM_CAMA) == 1) {
			double dx = pos.getX() - m.get(Memoria.CAMA_X);
			double dz = pos.getZ() - m.get(Memoria.CAMA_Z);
			if (dx * dx + dz * dz < r2) return false;
		}
		if (m.get(Memoria.TEM_PORTA) == 1) {
			double dx = pos.getX() - m.get(Memoria.PORTA_X);
			double dz = pos.getZ() - m.get(Memoria.PORTA_Z);
			if (dx * dx + dz * dz < r2) return false;
		}
		return true;
	}

	static boolean naTela(ServerPlayer p, Vec3 pos) {
		Vec3 olho = p.getEyePosition();
		Vec3 dir = pos.subtract(olho);
		if (dir.lengthSqr() < 0.001) return true;
		return p.getLookAngle().dot(dir.normalize()) > Percepcao.coneSeguro(p);
	}

	static boolean temCenaAtiva(EstadoJogador e) {
		return e.cena != EstadoJogador.Cena.NENHUMA
				|| e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA;
	}
}
