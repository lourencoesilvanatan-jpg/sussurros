package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import com.sussurros.bloco.CinzaEspalhadaBlock;

/**
 * Sistema reutilizável de escolha de pontos para aparições.
 *
 * A ideia é separar "quero uma aparição" de "onde ela deve acontecer".
 * O chamador fornece uma intenção geométrica e o sistema procura candidatos plausíveis,
 * evitando a tela atual, preferindo cobertura/penumbra quando pedido e lembrando de chunks
 * usados recentemente nesta sessão.
 *
 * Não depende de bibliotecas externas. É deliberadamente pequeno para poder servir tanto ao
 * Diretor quanto às cenas compostas sem transformar cada cena em uma coleção de regras de spawn.
 */
final class Aparicao {
	private static final int MAX_RECENTES = 8;

	private Aparicao() {
	}

	public record Config(
			double anguloMin,
			double anguloMax,
			double distanciaMin,
			double distanciaMax,
			int tentativas,
			boolean exigirVisivelAoVirar,
			boolean exigirCobertura,
			boolean preferirPenumbra,
			boolean evitarTela,
			boolean alternarLado,
			int lado,
			double bonusCobertura,
			double bonusPenumbra,
			double bonusVisibilidade,
			double distanciaPreferida) {
	}

	public record Candidato(
			BlockPos chao,
			double angulo,
			double distancia,
			boolean cobertura,
			boolean visivelAoVirar,
			boolean penumbra,
			double nota) {
		public long chaveChunk() {
			int cx = chao.getX() >> 4;
			int cz = chao.getZ() >> 4;
			return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		}

		public String resumo() {
			return String.format(java.util.Locale.ROOT,
					"ang=%.0f dist=%.1f cobertura=%s visivelVirando=%s penumbra=%s nota=%.2f",
					angulo, distancia, cobertura ? "sim" : "nao", visivelAoVirar ? "sim" : "nao",
					penumbra ? "sim" : "nao", nota);
		}
	}

	/** Procura um ponto ao redor do jogador. Não retorna um ponto dentro do FOV seguro atual. */
	@Nullable
	static Candidato buscarAoRedor(ServerLevel level, ServerPlayer p, EstadoJogador e, Config cfg) {
		return buscarAoRedor(level, p, e, cfg, 3);
	}

	/**
	 * O mesmo, dizendo quantos blocos de ar ele precisa em cima do chão. De pé ele tem três blocos de altura;
	 * na caça ele se abaixa e cabe em dois (ver HospedeEntity.CORPO_NA_CACA). Com três para todo mundo, a caçada
	 * não achava onde começar numa mina de corredor de dois blocos, que é onde o jogador mais fica no escuro.
	 */
	@Nullable
	static Candidato buscarAoRedor(ServerLevel level, ServerPlayer p, EstadoJogador e, Config cfg, int arLivre) {
		RandomSource rnd = level.getRandom();
		Candidato melhor = null;
		double melhorNota = Double.NEGATIVE_INFINITY;
		int tentativas = Math.max(4, Math.min(36, cfg.tentativas()));
		Set<Long> recentes = new HashSet<>(e.aparicoesRecentes);

		for (int i = 0; i < tentativas; i++) {
			double sinal = cfg.alternarLado() ? (rnd.nextBoolean() ? 1.0 : -1.0) : (cfg.lado() < 0 ? -1.0 : 1.0);
			double angulo = sinal * (cfg.anguloMin() + rnd.nextDouble() * Math.max(0.001, cfg.anguloMax() - cfg.anguloMin()));
			double distancia = cfg.distanciaMin() + rnd.nextDouble() * Math.max(0.001, cfg.distanciaMax() - cfg.distanciaMin());
			Vec3 alvo = pontoRelativo(p, angulo, distancia);
			BlockPos chao = acharChao(level, alvo.x, p.getY(), alvo.z, arLivre);
			if (chao == null || Diretor.emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			// Ele não aparece colado numa Linha de Cinza que ainda segura.
			if (CinzaEspalhadaBlock.linhaPerto(level, chao, 2) != null) {
				continue;
			}

			if (cfg.evitarTela() && naTela(p, chao)) {
				continue;
			}

			boolean cobertura = temCobertura(level, p, chao);
			if (cfg.exigirCobertura() && !cobertura) {
				continue;
			}
			if (Diretor.expostoDemais(level, p, chao, cobertura)) {
				continue;
			}

			int luz = luzEfetiva(level, chao.above());
			boolean penumbra = luz >= 2 && luz <= 8;
			boolean visivelAoVirar = linhaLivre(level, p.getEyePosition(), Vec3.atCenterOf(chao).add(0, 1.2, 0));
			boolean visivelPorLuz = distancia <= 20.0 || luz >= 4;
			if (cfg.exigirVisivelAoVirar() && (!visivelAoVirar || !visivelPorLuz)) {
				continue;
			}

			double nota = rnd.nextDouble();
			if (cobertura) {
				nota += cfg.bonusCobertura();
			}
			if (cfg.preferirPenumbra() && penumbra) {
				nota += cfg.bonusPenumbra();
			}
			if (visivelAoVirar) {
				nota += cfg.bonusVisibilidade();
			}
			if (cfg.distanciaPreferida() > 0) {
				nota += Math.max(0.0, 1.0 - Math.abs(distancia - cfg.distanciaPreferida()) / Math.max(1.0, cfg.distanciaMax() - cfg.distanciaMin()));
			}
			// Reutilizar o mesmo chunk é permitido, mas perde bastante prioridade por alguns turnos.
			if (recentes.contains(chunkKey(chao))) {
				nota -= 1.35;
			}
			// Ângulos de borda têm uma pequena preferência: é mais fácil o jogador notar "de canto"
			// depois de girar, sem precisar materializar algo diretamente atrás dele.
			if (Math.abs(angulo) >= 55 && Math.abs(angulo) <= 105) {
				nota += 0.35;
			}

			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = new Candidato(chao, angulo, distancia, cobertura, visivelAoVirar, penumbra, nota);
			}
		}
		return melhor;
	}

	/** Registra o chunk da aparição para reduzir repetições locais sem proibir o lugar para sempre. */
	static void registrar(EstadoJogador e, @Nullable Candidato candidato) {
		if (candidato == null) {
			return;
		}
		long chave = candidato.chaveChunk();
		e.aparicoesRecentes.remove(chave);
		e.aparicoesRecentes.addLast(chave);
		while (e.aparicoesRecentes.size() > MAX_RECENTES) {
			e.aparicoesRecentes.removeFirst();
		}
	}

	private static long chunkKey(BlockPos pos) {
		int cx = pos.getX() >> 4;
		int cz = pos.getZ() >> 4;
		return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
	}

	private static Vec3 pontoRelativo(ServerPlayer p, double graus, double dist) {
		Vec3 view = p.getViewVector(1.0F);
		double vx = view.x;
		double vz = view.z;
		double len = Math.sqrt(vx * vx + vz * vz);
		if (len < 1.0E-4) {
			vx = 0;
			vz = 1;
			len = 1;
		}
		vx /= len;
		vz /= len;
		double a = Math.toRadians(graus);
		double rx = vx * Math.cos(a) - vz * Math.sin(a);
		double rz = vx * Math.sin(a) + vz * Math.cos(a);
		return new Vec3(p.getX() + rx * dist, p.getY(), p.getZ() + rz * dist);
	}

	@Nullable
	private static BlockPos acharChao(ServerLevel level, double x, double yBase, double z, int arLivre) {
		for (int dy = 6; dy >= -12; dy--) {
			BlockPos pos = BlockPos.containing(x, yBase + dy, z);
			BlockPos baixo = pos.below();
			if (level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.above()).isAir()
					&& (arLivre < 3 || level.getBlockState(pos.above(2)).isAir())
					&& !level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty()) {
				return pos;
			}
		}
		return null;
	}

	private static boolean naTela(ServerPlayer p, BlockPos chao) {
		Vec3 ponto = Vec3.atCenterOf(chao).add(0, 1.2, 0);
		Vec3 direcao = ponto.subtract(p.getEyePosition()).normalize();
		return p.getViewVector(1.0F).dot(direcao) > Percepcao.coneSeguro(p);
	}

	private static boolean temCobertura(ServerLevel level, ServerPlayer p, BlockPos chao) {
		double dx = p.getX() - (chao.getX() + 0.5);
		double dz = p.getZ() - (chao.getZ() + 0.5);
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < 1.0E-4) {
			return false;
		}
		dx /= d;
		dz /= d;
		for (int passo = 1; passo <= 2; passo++) {
			BlockPos frente = BlockPos.containing(chao.getX() + 0.5 + dx * passo,
					chao.getY() + 1, chao.getZ() + 0.5 + dz * passo);
			if (!level.getBlockState(frente).getCollisionShape(level, frente).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	static boolean linhaLivre(ServerLevel level, Vec3 de, Vec3 para) {
		double dx = para.x - de.x;
		double dy = para.y - de.y;
		double dz = para.z - de.z;
		double distancia = Math.sqrt(dx * dx + dy * dy + dz * dz);
		int passos = Math.max(2, (int) Math.ceil(distancia / 0.75));
		for (int i = 1; i < passos; i++) {
			double t = i / (double) passos;
			BlockPos pos = BlockPos.containing(de.x + dx * t, de.y + dy * t, de.z + dz * t);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static int luzEfetiva(ServerLevel level, BlockPos pos) {
		int luzBloco = level.getBrightness(LightLayer.BLOCK, pos);
		int luzCeu = level.getBrightness(LightLayer.SKY, pos);
		return Math.max(luzBloco, Diretor.ehNoite(level) ? luzCeu - 11 : luzCeu);
	}
}
