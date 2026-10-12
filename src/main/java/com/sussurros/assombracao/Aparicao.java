package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
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

	/**
	 * Por que as procuras de lugar não acharam nada (0.9.0-alpha19): quantas vezes cada uma rodou, quantos
	 * lugares tentou e em que exigência eles caíram. Só contagem, para o log: não gasta sorteio e não muda nada.
	 *
	 * Até a alpha18 o log dizia "SEM_LUGAR" e mais nada. Para descobrir que o motivo, na casa do dono, era o
	 * capim, foi preciso copiar o mundo dele e refazer a procura à mão.
	 */
	static final class Falhas {
		private final Map<String, Map<String, Integer>> porProcura = new LinkedHashMap<>();

		/** Soma uma procura que falhou. "pares" alterna nome e valor: "tentados", 18, "semChao", 11... */
		void registrar(String procura, Object... pares) {
			Map<String, Integer> contagem = this.porProcura.computeIfAbsent(procura, k -> new LinkedHashMap<>());
			contagem.merge("vezes", 1, Integer::sum);
			for (int i = 0; i + 1 < pares.length; i += 2) {
				contagem.merge((String) pares[i], (Integer) pares[i + 1], Integer::sum);
			}
		}

		boolean vazio() {
			return this.porProcura.isEmpty();
		}

		/** Quantas procuras falharam desde a última vez que isto foi lido. */
		int vezes() {
			int n = 0;
			for (Map<String, Integer> contagem : this.porProcura.values()) {
				n += contagem.getOrDefault("vezes", 0);
			}
			return n;
		}

		void limpar() {
			this.porProcura.clear();
		}

		/** "NORMAL[vezes=3 tentados=54 semChao=40 ...] DE_PERTO[vezes=1 celulas=41 ...]". Zeros ficam de fora. */
		String resumo() {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, Map<String, Integer>> procura : this.porProcura.entrySet()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(procura.getKey()).append('[');
				boolean primeiro = true;
				for (Map.Entry<String, Integer> c : procura.getValue().entrySet()) {
					if (c.getValue() == 0) {
						continue;
					}
					if (!primeiro) {
						sb.append(' ');
					}
					sb.append(c.getKey()).append('=').append(c.getValue());
					primeiro = false;
				}
				sb.append(']');
			}
			return sb.toString();
		}
	}

	/**
	 * O "quase lugar" (0.9.0-alpha20): onde a última procura que falhou quis pô-lo e não pôde. É o "lado em que
	 * ele estaria" do plano B (ver PlanoB): quando uma construção acaba sem ninguém, o sinal físico acontece aqui.
	 *
	 * "aVista" diz se o jogador enxerga o lugar de onde está. O caso típico é o bolsão de caverna em que o único
	 * espaço fica à frente dele: o lugar caiu por estar na tela (ele nunca nasce na tela), e é ali, à vista, que
	 * alguma coisa acontece. Lugares que caíram pela zona da vela ou por uma Linha de Cinza não entram: ali nada
	 * acontece mesmo.
	 */
	record Quase(BlockPos chao, String motivo, boolean aVista, long tick) {
	}

	/** Junta, durante uma procura, o melhor "quase lugar": primeiro o que está à vista, depois o mais perto. */
	private static final class MelhorQuase {
		@Nullable
		private BlockPos chao;
		private String motivo = "";
		private boolean aVista;
		private double distancia = Double.MAX_VALUE;

		void ver(BlockPos c, String motivo, boolean aVista, double distancia) {
			if (this.chao != null && (this.aVista && !aVista || this.aVista == aVista && distancia >= this.distancia)) {
				return;
			}
			this.chao = c;
			this.motivo = motivo;
			this.aVista = aVista;
			this.distancia = distancia;
		}

		/** Guarda no estado do jogador. No mesmo tick, uma procura só troca o da outra se o seu estiver à vista. */
		void guardar(EstadoJogador e, long tick) {
			if (this.chao == null) {
				return;
			}
			Quase atual = e.quaseLugar;
			if (atual == null || atual.tick() != tick || (this.aVista && !atual.aVista())) {
				e.quaseLugar = new Quase(this.chao.immutable(), this.motivo, this.aVista, tick);
			}
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
		// Só para o log (ver Falhas): em que exigência cada lugar tentado caiu.
		int semChao = 0;
		int naZona = 0;
		int naCinza = 0;
		int caiuNaTela = 0;
		int semCobertura = 0;
		int exposto = 0;
		int semVisao = 0;
		// Para o plano B (ver Quase). Não sorteia nada; a linha de visão do que caiu na tela só é olhada se a
		// procura inteira falhar.
		MelhorQuase quase = new MelhorQuase();
		List<BlockPos> naTelaChao = new ArrayList<>();
		List<Double> naTelaDist = new ArrayList<>();

		for (int i = 0; i < tentativas; i++) {
			double sinal = cfg.alternarLado() ? (rnd.nextBoolean() ? 1.0 : -1.0) : (cfg.lado() < 0 ? -1.0 : 1.0);
			double angulo = sinal * (cfg.anguloMin() + rnd.nextDouble() * Math.max(0.001, cfg.anguloMax() - cfg.anguloMin()));
			double distancia = cfg.distanciaMin() + rnd.nextDouble() * Math.max(0.001, cfg.distanciaMax() - cfg.distanciaMin());
			Vec3 alvo = pontoRelativo(p, angulo, distancia);
			BlockPos chao = acharChao(level, alvo.x, p.getY(), alvo.z, arLivre);
			if (chao == null) {
				semChao++;
				continue;
			}
			if (Diretor.emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				naZona++;
				continue;
			}
			// Ele não aparece colado numa Linha de Cinza que ainda segura.
			if (CinzaEspalhadaBlock.linhaPerto(level, chao, 2) != null) {
				naCinza++;
				continue;
			}

			if (cfg.evitarTela() && naTela(p, chao)) {
				caiuNaTela++;
				naTelaChao.add(chao);
				naTelaDist.add(distancia);
				continue;
			}

			boolean cobertura = temCobertura(level, p, chao);
			if (cfg.exigirCobertura() && !cobertura) {
				semCobertura++;
				quase.ver(chao, "semCobertura", false, distancia);
				continue;
			}
			if (Diretor.expostoDemais(level, p, chao, cobertura)) {
				exposto++;
				quase.ver(chao, "exposto", false, distancia);
				continue;
			}

			int luz = luzEfetiva(level, chao.above());
			boolean penumbra = luz >= 2 && luz <= 8;
			boolean visivelAoVirar = linhaLivre(level, p.getEyePosition(), Vec3.atCenterOf(chao).add(0, 1.2, 0));
			boolean visivelPorLuz = distancia <= 20.0 || luz >= 4;
			if (cfg.exigirVisivelAoVirar() && (!visivelAoVirar || !visivelPorLuz)) {
				semVisao++;
				quase.ver(chao, "semVisao", false, distancia);
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
		if (melhor == null) {
			e.falhasDeLugar.registrar(cfg.exigirCobertura() ? "COBERTURA" : "NORMAL", "tentados", tentativas,
					"semChao", semChao, "zona", naZona, "cinza", naCinza, "tela", caiuNaTela, "semCobertura", semCobertura,
					"exposto", exposto, "semVisao", semVisao);
			Vec3 olho = p.getEyePosition();
			for (int i = 0; i < naTelaChao.size(); i++) {
				BlockPos chao = naTelaChao.get(i);
				quase.ver(chao, "tela", linhaLivre(level, olho, Vec3.atCenterOf(chao).add(0, 1.2, 0)), naTelaDist.get(i));
			}
			quase.guardar(e, level.getGameTime());
		}
		return melhor;
	}

	// =====================================================================
	// De perto: a procura de lugar apertado (0.9.0-alpha19)
	// =====================================================================

	/** A faixa de distância de quem aparece de perto (blocos, do jogador até o lugar). */
	static final double DE_PERTO_MIN = 8.0;
	static final double DE_PERTO_MAX = 14.0;
	/** Quantas células a procura visita, no máximo. Num túnel são dezenas; num salão de caverna, centenas. */
	private static final int DE_PERTO_CELULAS = 1200;
	/** Quanto vale, na nota de um lugar, ficar no túnel de onde os morcegos saíram (ver Caverna). */
	private static final double BONUS_DA_BOCA = 5.0;
	private static final Direction[] LADOS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	/** O que a procura de lugar apertado achou. */
	record DePerto(BlockPos chao, double distancia, boolean luz, boolean quina, boolean abaixado, int candidatos) {
		String resumo() {
			return String.format(java.util.Locale.ROOT, "dist=%.1f luz=%s quina=%s abaixado=%s candidatos=%d",
					distancia, luz ? "sim" : "nao", quina ? "sim" : "nao", abaixado ? "sim" : "nao", candidatos);
		}
	}

	/**
	 * A segunda procura, para lugar fechado: caverna, mina, corredor, dentro de casa.
	 *
	 * A procura de sempre sorteia um ângulo e uma distância (de 14 a 38 blocos) e pede três blocos de altura.
	 * Num túnel de mina quase todo sorteio cai dentro da pedra, e onde não cai o teto tem dois blocos: em 11/10/2026
	 * o Diretor quis mostrá-lo 8 vezes em 90 minutos e conseguiu 3, e uma sequência de ameaça no fundo de uma
	 * caverna acabou sem ninguém. Esta não sorteia: anda, a partir dos pés do jogador, pelo espaço em que ele
	 * está (cada célula com chão e dois blocos livres, subindo um degrau ou descendo até três) e fica com as
	 * células de 8 a 14 blocos que:
	 *  - estão fora da tela (a regra de sempre: ele nunca nasce na frente do jogador);
	 *  - dá para ver de onde o jogador está, se ele virar o rosto;
	 *  - são um lugar fechado (sem céu nenhum, ou com teto baixo): em campo aberto, de perto, ele é um boneco;
	 *  - não estão na zona da vela nem coladas numa Linha de Cinza.
	 * Entre elas prefere a que tem luz para ele ser visto e a que fica numa quina, de onde um passo o tiraria
	 * da vista. Onde o teto só deixa dois blocos, ele nasce abaixado (ver HospedeEntity.definirAbaixado).
	 *
	 * Não usa o sorteio do mundo: o desempate sai da posição e da "semente" (o segundo do jogo), para que o
	 * mesmo corredor não dê sempre o mesmo lugar.
	 *
	 * soComLuz (0.9.0-alpha20): só aceita lugar com luz para ele ser visto. É o que a cota de corpo pede (ver
	 * Cota): ali a aparição existe para ser vista, e no breu ele estaria lá sem ninguém ver.
	 */
	@Nullable
	static DePerto buscarDePerto(ServerLevel level, ServerPlayer p, EstadoJogador e, long semente, boolean soComLuz) {
		return buscarDePerto(level, p, e, semente, soComLuz, true);
	}

	/**
	 * A boca de túnel de onde saem os morcegos (0.9.0-alpha22, ver Caverna): o mesmo lugar que a procura de
	 * perto daria a ele. Aqui ninguém aparece, então a falta de lugar não entra na conta das falhas nem deixa
	 * "quase lugar" para o plano B.
	 */
	@Nullable
	static DePerto bocaDeTunel(ServerLevel level, ServerPlayer p, EstadoJogador e, long semente) {
		return buscarDePerto(level, p, e, semente, false, false);
	}

	@Nullable
	private static DePerto buscarDePerto(ServerLevel level, ServerPlayer p, EstadoJogador e, long semente, boolean soComLuz,
			boolean paraACriatura) {
		// O túnel de onde os morcegos saíram há pouco: a aparição que vier prefere estar ali (ver Caverna).
		BlockPos bocaDosMorcegos = paraACriatura ? Caverna.bocaValida(e, level.getGameTime() / 20) : null;
		BlockPos inicio = null;
		for (int dy : new int[] {0, 1, -1, -2}) {
			BlockPos c = p.blockPosition().above(dy);
			if (cabe(level, c)) {
				inicio = c;
				break;
			}
		}
		if (inicio == null) {
			if (paraACriatura) {
				e.falhasDeLugar.registrar("DE_PERTO", "semChaoDoJogador", 1);
			}
			return null;
		}
		Vec3 olho = p.getEyePosition();
		ArrayDeque<BlockPos> fila = new ArrayDeque<>();
		Set<Long> vistas = new HashSet<>();
		fila.add(inicio);
		vistas.add(inicio.asLong());
		int celulas = 0;
		int naFaixa = 0;
		int naZona = 0;
		int naCinza = 0;
		int caiuNaTela = 0;
		int semVisao = 0;
		int exposto = 0;
		int aberto = 0;
		int semLuz = 0;
		int candidatos = 0;
		DePerto melhor = null;
		double melhorNota = Double.NEGATIVE_INFINITY;
		// Para o plano B (ver Quase): o que caiu na tela só tem a linha de visão olhada se a procura falhar.
		MelhorQuase quase = new MelhorQuase();
		List<BlockPos> naTelaChao = new ArrayList<>();
		List<Double> naTelaDist = new ArrayList<>();

		while (!fila.isEmpty() && celulas < DE_PERTO_CELULAS) {
			BlockPos c = fila.poll();
			celulas++;
			double distancia = Math.sqrt(p.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5));
			if (distancia > DE_PERTO_MAX) {
				continue; // além da faixa não interessa, nem o que vem depois
			}
			for (Direction lado : LADOS) {
				for (int dy : new int[] {0, 1, -1, -2, -3}) {
					BlockPos n = c.relative(lado).above(dy);
					if (!cabe(level, n) || !passa(level, c, lado, dy)) {
						continue;
					}
					if (vistas.add(n.asLong())) {
						fila.add(n);
					}
					break;
				}
			}
			if (distancia < DE_PERTO_MIN) {
				continue;
			}
			naFaixa++;
			if (Diretor.emZonaCalma(p, c.getX(), c.getY(), c.getZ())) {
				naZona++;
				continue;
			}
			if (CinzaEspalhadaBlock.linhaPerto(level, c, 2) != null) {
				naCinza++;
				continue;
			}
			if (naTela(p, c)) {
				caiuNaTela++;
				if (naTelaChao.size() < 96) {
					naTelaChao.add(c);
					naTelaDist.add(distancia);
				}
				continue;
			}
			boolean abaixado = !Diretor.livre(level, c.above(2));
			double altura = abaixado ? 1.9 : 3.0;
			if (!enxergaCorpo(level, olho, c, altura)) {
				semVisao++;
				quase.ver(c, "semVisao", false, distancia);
				continue;
			}
			if (Diretor.expostoDemais(level, p, c, temCobertura(level, p, c))) {
				exposto++;
				quase.ver(c, "exposto", false, distancia);
				continue;
			}
			if (!fechado(level, c)) {
				aberto++;
				quase.ver(c, "aberto", false, distancia);
				continue;
			}
			boolean luz = Diretor.temLuzParaVer(level, c);
			if (soComLuz && !luz) {
				semLuz++;
				quase.ver(c, "semLuz", false, distancia);
				continue;
			}
			candidatos++;
			boolean quina = umPassoEsconde(level, olho, c);
			double meio = (DE_PERTO_MIN + DE_PERTO_MAX) / 2.0;
			double nota = (luz ? 2.0 : 0.0) + (quina ? 1.5 : 0.0)
					+ 0.5 * (1.0 - Math.abs(distancia - meio) / (DE_PERTO_MAX - meio)) + 0.6 * ruido(c, semente);
			if (bocaDosMorcegos != null && c.closerThan(bocaDosMorcegos, 3.5)) {
				nota += BONUS_DA_BOCA; // mais do que todo o resto somado: havendo lugar ali, é ali
			}
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = new DePerto(c, distancia, luz, quina, abaixado, 0);
			}
		}
		if (melhor == null) {
			if (!paraACriatura) {
				return null;
			}
			e.falhasDeLugar.registrar("DE_PERTO", "celulas", celulas, "naFaixa", naFaixa, "zona", naZona, "cinza", naCinza,
					"tela", caiuNaTela, "semVisao", semVisao, "exposto", exposto, "aberto", aberto, "semLuz", semLuz);
			for (int i = 0; i < naTelaChao.size(); i++) {
				BlockPos c = naTelaChao.get(i);
				quase.ver(c, "tela", enxergaCorpo(level, olho, c, 1.9), naTelaDist.get(i));
			}
			quase.guardar(e, level.getGameTime());
			return null;
		}
		return new DePerto(melhor.chao(), melhor.distancia(), melhor.luz(), melhor.quina(), melhor.abaixado(), candidatos);
	}

	/** Ele cabe aqui abaixado: chão firme e dois blocos livres. (De pé precisa de três; quem chama confere.) */
	private static boolean cabe(ServerLevel level, BlockPos pos) {
		BlockPos baixo = pos.below();
		return Diretor.livre(level, pos) && Diretor.livre(level, pos.above())
				&& !level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty();
	}

	/**
	 * Dá para ir de "c" à célula vizinha que fica "dy" blocos acima ou abaixo? Subindo, a cabeça precisa de
	 * espaço em cima de onde se está; descendo, a coluna vizinha tem de estar livre da altura de onde se sai até
	 * o chão de chegada. É a continuidade do espaço que importa (ele não anda este caminho; nasce no fim dele).
	 */
	private static boolean passa(ServerLevel level, BlockPos c, Direction lado, int dy) {
		if (dy == 0) {
			return true;
		}
		if (dy > 0) {
			return Diretor.livre(level, c.above(2));
		}
		BlockPos coluna = c.relative(lado);
		for (int y = dy + 2; y <= 1; y++) {
			if (!Diretor.livre(level, coluna.above(y))) {
				return false;
			}
		}
		return true;
	}

	/** Do olho do jogador dá para ver o corpo dele ali? Basta o peito ou a cabeça. */
	private static boolean enxergaCorpo(ServerLevel level, Vec3 olho, BlockPos chao, double altura) {
		Vec3 pe = new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
		return linhaLivre(level, olho, pe.add(0, altura * 0.85, 0)) || linhaLivre(level, olho, pe.add(0, altura * 0.5, 0));
	}

	/** Uma quina: há uma célula vizinha, onde ele também caberia, que o jogador não enxerga de onde está. */
	private static boolean umPassoEsconde(ServerLevel level, Vec3 olho, BlockPos c) {
		for (Direction lado : LADOS) {
			for (int dy : new int[] {0, 1, -1}) {
				BlockPos n = c.relative(lado).above(dy);
				if (cabe(level, n) && passa(level, c, lado, dy)) {
					if (!enxergaCorpo(level, olho, n, 1.9)) {
						return true;
					}
					break;
				}
			}
		}
		return false;
	}

	/**
	 * Lugar fechado: nenhuma luz do céu chega ali (caverna, mina, porão), ou há teto de verdade a até quatro
	 * blocos do chão (um cômodo, um corredor, uma varanda). Copa de árvore não é teto.
	 */
	private static boolean fechado(ServerLevel level, BlockPos chao) {
		if (level.getBrightness(LightLayer.SKY, chao) == 0) {
			return true;
		}
		for (int dy = 2; dy <= 4; dy++) {
			BlockPos cima = chao.above(dy);
			BlockState s = level.getBlockState(cima);
			if (!s.getCollisionShape(level, cima).isEmpty() && !s.is(BlockTags.LEAVES)) {
				return true;
			}
		}
		return false;
	}

	/** De 0 a 1, fixo para a mesma célula no mesmo segundo. Não é sorteio: não toca em gerador nenhum. */
	private static double ruido(BlockPos pos, long semente) {
		long h = pos.asLong() * 0x9E3779B97F4A7C15L ^ (semente + 1) * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h & 0xFFFFL) / 65535.0;
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
			if (Diretor.livre(level, pos)
					&& Diretor.livre(level, pos.above())
					&& (arLivre < 3 || Diretor.livre(level, pos.above(2)))
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
