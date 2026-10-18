package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Alguém esteve aqui (0.9.0-alpha28): coisas de verdade, que ficam, postas por alguém que não é o jogador.
 *
 * Em 18/10/2026 o dono respondeu à pergunta que esperava desde 11/10 (blocos de verdade em caverna natural): "sim
 * se possivel ate perto do que eu construi, é pra pertubar o jogador mesmo". Até aqui tudo o que o mod punha perto
 * dele sumia sozinho (miragem, AlteracoesTemporarias) ou ficava longe da base (a Soleira, a Casa do Vigia). Isto fica.
 *
 * Dois lugares, e sempre com ele sem olhar:
 *
 *  - NA CAVERNA, um trecho escuro que ele ainda não andou aparece já iluminado: duas a quatro tochas na parede, como
 *    alguém que minerou ali antes e foi marcando o caminho (o ENGRAM pré-minera cavernas assim: pesquisa profunda de
 *    11/10). Às vezes, em vez disso, uma galeria curta cavada na pedra, com uma tocha no fundo. Uma vez por descida, e
 *    no máximo a cada vinte e cinco minutos de mundo;
 *  - PERTO DA CASA, uma coisa pequena do lado de fora, no estilo dele (a mesma luz que ele usa): uma tocha na parede
 *    de fora, ao lado de uma janela; uma tocha fincada no chão diante da porta; uma bancada com uma tocha, como o
 *    acampamento da primeira noite de alguém. Uma de cada vez, nunca a mesma duas vezes seguidas, em parte dos dias, e
 *    só com ele longe da cama ou dormindo.
 *
 * A regra "nunca estragar a construção" continua valendo: nada dele é quebrado, trocado ou tapado. Na casa só se usa
 * lugar vazio a céu aberto, nunca colado numa porta; a tocha da janela se apoia na parede dele, e só. Na caverna só se
 * cava pedra natural (nunca minério, nunca perto de água, lava ou areia que caia).
 *
 * É coisa que ele só encontra se olhar: não gasta saldo de atenção (CLAUDE.md, "Atenção"). Quando ele chega perto de
 * uma delas, o log diz que foi achada, e ela conta como uma "marca" para as famílias. Usa o gerador do jogador, nunca
 * o do mundo.
 */
public final class AlguemEsteveAqui {
	enum Tipo {
		TOCHAS, GALERIA,                           // na caverna
		TOCHA_NA_JANELA, TOCHA_NO_CHAO, BANCADA    // perto da casa (nesta ordem, em rodízio)
	}

	private static final Tipo[] DA_CASA = {Tipo.TOCHA_NA_JANELA, Tipo.TOCHA_NO_CHAO, Tipo.BANCADA};

	// ===== Memória =====
	static final String CAVERNA_SEG = "alguem_caverna_seg";   // segundo do mundo da última vez na caverna
	static final String CASA_DIA = "alguem_casa_dia";         // o último dia do jogo em que se decidiu se haveria
	static final String CASA_HOJE = "alguem_casa_hoje";       // 1: hoje vai haver na casa, e ainda não houve
	static final String CASA_PROXIMA = "alguem_casa_proxima"; // qual das três da casa vem primeiro na próxima vez
	static final String POSTOS = "alguem_postos";             // quantas já foram postas (as últimas ANEL ficam lembradas)

	/** Quantas ficam lembradas para saber quando ele as acha. */
	static final int ANEL = 6;

	// ===== Números (ponto de partida; cada um com linha no log) =====
	/** Segundos de subsolo, na descida, antes de poder acontecer na caverna. */
	static final int ESPERA_NA_DESCIDA = 240;
	/** Segundos do mundo entre uma vez e outra na caverna. */
	static final int ENTRE_CAVERNAS = 1500;
	/** A cada vinte segundos, depois da espera. */
	static final float CHANCE_CAVERNA = 0.12F;
	/** Das vezes na caverna, quantas são a galeria (se houver pedra para cavar). */
	static final float CHANCE_GALERIA = 0.3F;
	/** Por dia do jogo. */
	static final float CHANCE_CASA = 0.45F;
	/** A casa só muda com ele a esta distância da cama (no chão), ou dormindo. */
	static final double LONGE_DA_CAMA = 32.0;
	/** A esta distância (blocos) de uma delas, ele achou. */
	static final double ACHOU = 4.5;
	/** Perto disto de por onde ele andou, o trecho não é "que ele ainda não andou". */
	static final double LONGE_DO_RASTRO = 10.0;

	private AlguemEsteveAqui() {
	}

	/** Uma vez por segundo, dentro do tick do Diretor (recebe a Memoria do tick). */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}
		achados(level, p, m, e, seg);
		if (e.forcando || fase < 2 || !PrimeiroContato.liberado(m)) {
			return;
		}
		if (seg % 20 == 11) {
			caverna(level, p, m, e, seg);
		}
		if (seg % 10 == 4) {
			casa(level, p, m, e, seg, false);
		}
	}

	/** Ele acordou: se hoje era dia, a casa amanhece com a coisa (enquanto ele dormia, ninguém olhava). */
	static void aoAcordar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase) {
		if (level.dimension() != Level.OVERWORLD || e.forcando || fase < 2 || !PrimeiroContato.liberado(m)) {
			return;
		}
		casa(level, p, m, e, level.getGameTime() / 20, true);
	}

	// =====================================================================
	// Na caverna
	// =====================================================================

	private static void caverna(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg) {
		if (e.contexto != ContextoMundo.Tipo.SUBSOLO || e.subsoloDesde < 0 || seg - e.subsoloDesde < ESPERA_NA_DESCIDA
				|| e.alguemNaDescida == e.subsoloDesde) {
			return;
		}
		if (seg - m.get(CAVERNA_SEG, -100000) < ENTRE_CAVERNAS) {
			return;
		}
		if ((e.criatura != null && !e.criatura.isRemoved()) || EstruturasSussurros.temCenaAtiva(e)) {
			return;
		}
		RandomSource sorte = p.getRandom();
		if (sorte.nextFloat() >= CHANCE_CAVERNA) {
			return;
		}
		List<BlockPos> postos = naCaverna(level, p, m, e, sorte, sorte.nextFloat() < CHANCE_GALERIA);
		if (postos.isEmpty()) {
			Depuracao.log(p, seg, "ALGUEM sem_lugar onde=CAVERNA");
			return;
		}
		e.alguemNaDescida = e.subsoloDesde;
		m.set(CAVERNA_SEG, (int) seg);
	}

	/**
	 * Põe na caverna em volta do jogador. Devolve onde pôs (vazio: não achou trecho bom). Com galeria=true, tenta antes a
	 * galeria cavada; se não houver pedra para isso, faz as tochas.
	 */
	static List<BlockPos> naCaverna(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, RandomSource sorte, boolean galeria) {
		long seg = level.getGameTime() / 20;
		List<Rastro.Ponto> andou = e.rastro.comIdade(seg, 0, Integer.MAX_VALUE);
		BlockPos inicio = p.blockPosition();
		if (!andavel(level, inicio)) {
			inicio = inicio.below();
		}
		if (!andavel(level, inicio)) {
			return List.of();
		}
		// A caverna em que ele está, percorrida a partir dele: a ordem de chegada é a distância pelo caminho. O trecho é
		// o primeiro pedaço dela que fica a dezesseis blocos ou mais, escuro, fora da vista e longe de por onde ele andou;
		// as tochas vão ali, até vinte blocos adiante do começo do trecho.
		List<BlockPos> caverna = trecho(level, inicio, 2500, 34);
		List<BlockPos> candidatos = new ArrayList<>();
		BlockPos comeco = null;
		int longe = 0;
		int semRastro = 0;
		int escuro = 0;
		for (BlockPos c : caverna) {
			if (p.distanceToSqr(Vec3.atCenterOf(c)) < 16 * 16 || (comeco != null && c.distSqr(comeco) > 20 * 20)) {
				continue;
			}
			longe++;
			if (!longeDoRastro(c, andou)) {
				continue;
			}
			semRastro++;
			if (!escuro(level, c) || !escuro(level, c.above())) {
				continue;
			}
			escuro++;
			if (enxerga(level, p, c) || enxerga(level, p, c.above())) {
				continue;
			}
			if (comeco == null) {
				comeco = c;
			}
			candidatos.add(c);
		}
		if (candidatos.isEmpty()) {
			// Por que não: quantas células da caverna em volta dele, quantas longe, longe do caminho, escuras e fora da vista.
			Depuracao.log(p, seg, String.format(Locale.ROOT, "ALGUEM sem_lugar onde=CAVERNA caverna=%d longe=%d semRastro=%d escuras=%d",
					caverna.size(), longe, semRastro, escuro));
			return List.of();
		}
		BlockState parede = luzDele(level, p.blockPosition()).is(Blocks.SOUL_TORCH)
				? Blocks.SOUL_WALL_TORCH.defaultBlockState() : Blocks.WALL_TORCH.defaultBlockState();
		if (galeria) {
			List<BlockPos> cavado = galeria(level, p, candidatos, parede, sorte);
			if (!cavado.isEmpty()) {
				anotar(p, m, Tipo.GALERIA, cavado.get(cavado.size() - 1), seg,
						"cavados=" + (cavado.size() - 1) + " de=" + pos(p.blockPosition()));
				return cavado;
			}
		}
		int quantas = 2 + sorte.nextInt(3);
		List<BlockPos> postas = new ArrayList<>();
		for (BlockPos c : candidatos) {
			if (postas.size() >= quantas) {
				break;
			}
			BlockPos t = c.above();
			boolean espacado = true;
			for (BlockPos ja : postas) {
				if (ja.distSqr(t) < 6 * 6) {
					espacado = false;
					break;
				}
			}
			if (espacado && naParede(level, t, parede, sorte)) {
				postas.add(t);
			}
		}
		if (postas.size() < 2) {
			// Uma tocha sozinha não é "alguém passou por aqui": é uma tocha. Desfaz.
			for (BlockPos t : postas) {
				level.removeBlock(t, false);
			}
			return List.of();
		}
		anotar(p, m, Tipo.TOCHAS, postas.get(0), seg, "tochas=" + postas.size() + " ate=" + pos(postas.get(postas.size() - 1)));
		return postas;
	}

	/** Uma galeria de três blocos de fundo e dois de altura, cavada numa parede de pedra do trecho, com uma tocha no fundo. */
	private static List<BlockPos> galeria(ServerLevel level, ServerPlayer p, List<BlockPos> candidatos, BlockState parede, RandomSource sorte) {
		for (BlockPos c : candidatos) {
			for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(sorte)) {
				List<BlockPos> cavar = new ArrayList<>();
				for (int k = 1; k <= 3; k++) {
					cavar.add(c.relative(d, k));
					cavar.add(c.relative(d, k).above());
				}
				BlockPos fundo = c.relative(d, 4).above();
				if (!podeCavar(level, cavar) || !level.getBlockState(fundo).isFaceSturdy(level, fundo, d.getOpposite())) {
					continue;
				}
				boolean escondido = true;
				for (BlockPos b : cavar) {
					if (enxerga(level, p, b)) {
						escondido = false;
						break;
					}
				}
				if (!escondido) {
					continue;
				}
				for (BlockPos b : cavar) {
					level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
				}
				BlockPos tocha = c.relative(d, 3).above();
				BlockState estado = parede.setValue(WallTorchBlock.FACING, d.getOpposite());
				if (estado.canSurvive(level, tocha)) {
					level.setBlock(tocha, estado, 3);
				}
				List<BlockPos> feito = new ArrayList<>(cavar);
				feito.add(tocha);
				return feito;
			}
		}
		return List.of();
	}

	/** Só pedra natural, sem minério, sem líquido em volta, com chão firme e sem nada que caia de cima. */
	private static boolean podeCavar(ServerLevel level, List<BlockPos> cavar) {
		for (BlockPos b : cavar) {
			BlockState s = level.getBlockState(b);
			if (!s.is(BlockTags.BASE_STONE_OVERWORLD)) {
				return false;
			}
			for (Direction v : Direction.values()) {
				BlockPos viz = b.relative(v);
				if (!level.getFluidState(viz).isEmpty() || level.getBlockState(viz).getBlock() instanceof FallingBlock) {
					return false;
				}
			}
		}
		// Chão firme debaixo da fileira de baixo (os três primeiros da lista são os de baixo, em posições pares).
		for (int i = 0; i < cavar.size(); i += 2) {
			BlockPos chao = cavar.get(i).below();
			if (!level.getBlockState(chao).isFaceSturdy(level, chao, Direction.UP)) {
				return false;
			}
		}
		return true;
	}

	/** Uma tocha na parede, ao lado desta célula livre: a parede tem de aguentar. */
	private static boolean naParede(ServerLevel level, BlockPos t, BlockState parede, RandomSource sorte) {
		if (!Diretor.livre(level, t) || !level.getBlockState(t).isAir()) {
			return false;
		}
		for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(sorte)) {
			BlockPos apoio = t.relative(d);
			if (!level.getBlockState(apoio).isFaceSturdy(level, apoio, d.getOpposite())) {
				continue;
			}
			BlockState estado = parede.setValue(WallTorchBlock.FACING, d.getOpposite());
			if (estado.canSurvive(level, t)) {
				level.setBlock(t, estado, 3);
				return true;
			}
		}
		return false;
	}

	/** As células andáveis ligadas a esta, em largura, até "maximo" delas e "raio" blocos de distância. */
	private static List<BlockPos> trecho(ServerLevel level, BlockPos semente, int maximo, int raio) {
		List<BlockPos> ordem = new ArrayList<>();
		Map<BlockPos, Boolean> visto = new HashMap<>();
		ArrayDeque<BlockPos> fila = new ArrayDeque<>();
		fila.add(semente);
		visto.put(semente, true);
		while (!fila.isEmpty() && ordem.size() < maximo) {
			BlockPos c = fila.removeFirst();
			ordem.add(c);
			for (Direction d : Direction.Plane.HORIZONTAL) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos v = c.relative(d).offset(0, dy, 0);
					if (visto.containsKey(v) || v.distSqr(semente) > raio * raio || !level.isLoaded(v)) {
						continue;
					}
					visto.put(v, true);
					if (andavel(level, v)) {
						fila.addLast(v);
					}
				}
			}
		}
		return ordem;
	}

	/** Dá para ficar de pé aqui: dois blocos livres e chão firme. */
	private static boolean andavel(ServerLevel level, BlockPos c) {
		return level.isLoaded(c) && Diretor.livre(level, c) && Diretor.livre(level, c.above())
				&& level.getBlockState(c.below()).isFaceSturdy(level, c.below(), Direction.UP);
	}

	/** Sem céu, sem luz de tocha (ninguém acendeu ainda), e fora da vista do jogador. */
	/** Sem céu e sem luz de tocha: ninguém acendeu ainda. */
	private static boolean escuro(ServerLevel level, BlockPos c) {
		return level.getBrightness(LightLayer.SKY, c) == 0 && level.getBrightness(LightLayer.BLOCK, c) <= 2;
	}

	/** O jogador enxerga o centro deste bloco de onde está? Pela forma que se vê: vidro não esconde. */
	private static boolean enxerga(ServerLevel level, ServerPlayer p, BlockPos c) {
		return level.clip(new ClipContext(p.getEyePosition(), Vec3.atCenterOf(c), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p))
				.getType() == HitResult.Type.MISS;
	}

	private static boolean longeDoRastro(BlockPos c, List<Rastro.Ponto> andou) {
		for (Rastro.Ponto pt : andou) {
			double dx = pt.x() - (c.getX() + 0.5);
			double dy = pt.y() - c.getY();
			double dz = pt.z() - (c.getZ() + 0.5);
			if (dx * dx + dy * dy + dz * dz < LONGE_DO_RASTRO * LONGE_DO_RASTRO) {
				return false;
			}
		}
		return true;
	}

	// =====================================================================
	// Perto da casa
	// =====================================================================

	private static void casa(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, boolean acordou) {
		if (m.get(Memoria.TEM_CAMA) != 1) {
			return;
		}
		int dia = (int) (level.getDefaultClockTime() / 24000L);
		if (m.get(CASA_DIA, -1) != dia) {
			m.set(CASA_DIA, dia);
			m.set(CASA_HOJE, p.getRandom().nextFloat() < CHANCE_CASA ? 1 : 0);
		}
		if (m.get(CASA_HOJE) != 1) {
			return;
		}
		BlockPos cama = new BlockPos(m.get(Memoria.CAMA_X), m.get(Memoria.CAMA_Y), m.get(Memoria.CAMA_Z));
		double dx = p.getX() - (cama.getX() + 0.5);
		double dz = p.getZ() - (cama.getZ() + 0.5);
		if ((!acordou && dx * dx + dz * dz < LONGE_DA_CAMA * LONGE_DA_CAMA) || !level.isLoaded(cama)
				|| (e.criatura != null && !e.criatura.isRemoved()) || EstruturasSussurros.temCenaAtiva(e)) {
			return;
		}
		int primeira = Math.floorMod(m.get(CASA_PROXIMA), DA_CASA.length);
		for (int k = 0; k < DA_CASA.length; k++) {
			int i = (primeira + k) % DA_CASA.length;
			BlockPos posto = naCasa(level, p, m, DA_CASA[i], p.getRandom());
			if (posto != null) {
				m.set(CASA_HOJE, 0);
				m.set(CASA_PROXIMA, i + 1);
				return;
			}
		}
		// Nenhuma coube hoje (tudo à vista, ou nada vazio em volta). Tenta de novo daqui a pouco, no mesmo dia.
	}

	/** Põe a coisa deste tipo perto da casa. Devolve onde pôs, ou null se não coube. */
	@Nullable
	static BlockPos naCasa(ServerLevel level, ServerPlayer p, Memoria m, Tipo tipo, RandomSource sorte) {
		BlockPos cama = new BlockPos(m.get(Memoria.CAMA_X), m.get(Memoria.CAMA_Y), m.get(Memoria.CAMA_Z));
		BlockPos porta = m.get(Memoria.TEM_PORTA) == 1 ? new BlockPos(m.get(Memoria.PORTA_X), m.get(Memoria.PORTA_Y), m.get(Memoria.PORTA_Z)) : cama;
		BlockState luz = luzDele(level, cama);
		long seg = level.getGameTime() / 20;
		BlockPos posto = switch (tipo) {
			case TOCHA_NA_JANELA -> tochaNaJanela(level, p, cama, luz, sorte);
			case TOCHA_NO_CHAO -> noChao(level, p, porta, 4, 8, sorte, luz, null);
			case BANCADA -> noChao(level, p, cama, 9, 16, sorte, Blocks.CRAFTING_TABLE.defaultBlockState(), luz);
			default -> null;
		};
		if (posto != null) {
			anotar(p, m, tipo, posto, seg, String.format(Locale.ROOT, "daCama=%.0f", Math.sqrt(posto.distSqr(cama))));
		}
		return posto;
	}

	/**
	 * Uma tocha na parede de fora, ao lado de uma janela da casa: o vão em frente é vazio e vê o céu (é o lado de fora),
	 * a parede aguenta a tocha e não é vidro, e não há porta colada.
	 */
	@Nullable
	private static BlockPos tochaNaJanela(ServerLevel level, ServerPlayer p, BlockPos cama, BlockState luz, RandomSource sorte) {
		BlockState parede = luz.is(Blocks.SOUL_TORCH) ? Blocks.SOUL_WALL_TORCH.defaultBlockState() : Blocks.WALL_TORCH.defaultBlockState();
		List<BlockPos[]> opcoes = new ArrayList<>(); // {lugar da tocha, apoio}
		List<Direction> viradas = new ArrayList<>();
		for (BlockPos g : BlockPos.betweenClosed(cama.offset(-10, -3, -10), cama.offset(10, 4, 10))) {
			if (!vidro(level.getBlockState(g))) {
				continue;
			}
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos fora = g.relative(d);
				if (!level.isEmptyBlock(fora) || !level.canSeeSky(fora)) {
					continue;
				}
				for (Direction lado : new Direction[] {d.getClockWise(), d.getCounterClockWise()}) {
					BlockPos apoio = g.relative(lado);
					BlockPos lugar = apoio.relative(d);
					BlockState sApoio = level.getBlockState(apoio);
					if (vidro(sApoio) || !sApoio.isFaceSturdy(level, apoio, d) || !level.isEmptyBlock(lugar) || !level.canSeeSky(lugar)
							|| pertoDePorta(level, lugar)) {
						continue;
					}
					opcoes.add(new BlockPos[] {lugar.immutable(), apoio.immutable()});
					viradas.add(d);
				}
			}
		}
		for (int tentativa = 0; tentativa < 8 && !opcoes.isEmpty(); tentativa++) {
			int i = sorte.nextInt(opcoes.size());
			BlockPos lugar = opcoes.get(i)[0];
			BlockState estado = parede.setValue(WallTorchBlock.FACING, viradas.get(i));
			if (!naTela(p, lugar) && estado.canSurvive(level, lugar)) {
				level.setBlock(lugar, estado, 3);
				return lugar;
			}
		}
		return null;
	}

	/**
	 * No chão, entre "de" e "ate" blocos de "centro": chão natural, a céu aberto, dois blocos vazios, longe de qualquer
	 * coisa que não seja natural (não encosta em cerca, horta nem parede dele), fora da tela. Com "junto", põe também
	 * aquilo num lugar vizinho (a tocha ao lado da bancada).
	 */
	@Nullable
	private static BlockPos noChao(ServerLevel level, ServerPlayer p, BlockPos centro, int de, int ate, RandomSource sorte,
			BlockState estado, @Nullable BlockState junto) {
		for (int i = 0; i < 24; i++) {
			double ang = sorte.nextDouble() * Math.PI * 2.0;
			double r = de + sorte.nextDouble() * (ate - de);
			int x = (int) Math.floor(centro.getX() + 0.5 + Math.cos(ang) * r);
			int z = (int) Math.floor(centro.getZ() + 0.5 + Math.sin(ang) * r);
			BlockPos base = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (!level.isLoaded(base) || Math.abs(base.getY() - centro.getY()) > 3 || !lugarLimpo(level, base) || naTela(p, base)) {
				continue;
			}
			BlockPos vizinho = null;
			if (junto != null) {
				for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(sorte)) {
					BlockPos v = base.relative(d);
					if (v.getY() == level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, v.getX(), v.getZ()) && lugarLimpo(level, v)
							&& !naTela(p, v)) {
						vizinho = v;
						break;
					}
				}
				if (vizinho == null) {
					continue;
				}
			}
			if (!estado.canSurvive(level, base)) {
				continue;
			}
			level.setBlock(base, estado, 3);
			if (vizinho != null && junto.canSurvive(level, vizinho)) {
				level.setBlock(vizinho, junto, 3);
			}
			return base;
		}
		return null;
	}

	/** Vazio com um bloco livre em cima, a céu aberto, sobre chão natural, e sem nada que não seja natural em volta. */
	private static boolean lugarLimpo(ServerLevel level, BlockPos base) {
		if (!level.isEmptyBlock(base) || !level.isEmptyBlock(base.above()) || !level.canSeeSky(base) || !chaoNatural(level.getBlockState(base.below()))) {
			return false;
		}
		for (BlockPos v : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(1, 1, 1))) {
			BlockState s = level.getBlockState(v);
			if (!s.isAir() && !chaoNatural(s) && !s.is(BlockTags.REPLACEABLE) && !s.is(BlockTags.FLOWERS)) {
				return false;
			}
		}
		return true;
	}

	private static boolean chaoNatural(BlockState chao) {
		return chao.is(BlockTags.DIRT) || chao.is(BlockTags.GRASS_BLOCKS) || chao.is(BlockTags.SAND) || chao.is(BlockTags.BASE_STONE_OVERWORLD)
				|| chao.is(BlockTags.TERRACOTTA) || chao.is(Blocks.GRAVEL) || chao.is(Blocks.SNOW_BLOCK);
	}

	private static boolean vidro(BlockState s) {
		return !s.isAir() && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().contains("glass");
	}

	private static boolean pertoDePorta(ServerLevel level, BlockPos lugar) {
		for (BlockPos v : BlockPos.betweenClosed(lugar.offset(-1, -1, -1), lugar.offset(1, 1, 1))) {
			if (level.getBlockState(v).getBlock() instanceof DoorBlock) {
				return true;
			}
		}
		return false;
	}

	/** A luz que ele usa: a mais comum perto daqui (tocha ou tocha de alma; lanterna vira tocha). Sem nenhuma, tocha. */
	static BlockState luzDele(ServerLevel level, BlockPos centro) {
		int comuns = 0;
		int almas = 0;
		for (BlockPos v : BlockPos.betweenClosed(centro.offset(-10, -4, -10), centro.offset(10, 4, 10))) {
			BlockState s = level.getBlockState(v);
			if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.LANTERN)) {
				comuns++;
			} else if (s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH) || s.is(Blocks.SOUL_LANTERN)) {
				almas++;
			}
		}
		return almas > comuns ? Blocks.SOUL_TORCH.defaultBlockState() : Blocks.TORCH.defaultBlockState();
	}

	private static boolean naTela(ServerPlayer p, BlockPos pos) {
		Vec3 centro = Vec3.atCenterOf(pos);
		return p.distanceToSqr(centro) < 128 * 128 && EstruturasSussurros.naTela(p, centro);
	}

	// =====================================================================
	// Lembrar e achar
	// =====================================================================

	private static String chave(int i, String campo) {
		return "alguem_" + i + "_" + campo;
	}

	/** Lembra o que foi posto (as últimas ANEL), para saber quando ele acha, e escreve no log. Usa a Memoria de quem chamou. */
	private static void anotar(ServerPlayer p, Memoria m, Tipo tipo, BlockPos pos, long seg, String detalhe) {
		lembrar(m, tipo, pos, seg);
		Depuracao.log(p, seg, String.format(Locale.ROOT, "ALGUEM posto onde=%s tipo=%s pos=%s dist=%.0f %s",
				tipo == Tipo.TOCHAS || tipo == Tipo.GALERIA ? "CAVERNA" : "CASA", tipo, pos(pos), Math.sqrt(p.distanceToSqr(Vec3.atCenterOf(pos))), detalhe));
	}

	static void lembrar(Memoria m, Tipo tipo, BlockPos pos, long seg) {
		int n = m.get(POSTOS);
		int i = n % ANEL;
		m.set(chave(i, "x"), pos.getX());
		m.set(chave(i, "y"), pos.getY());
		m.set(chave(i, "z"), pos.getZ());
		m.set(chave(i, "tipo"), tipo.ordinal());
		m.set(chave(i, "seg"), (int) seg);
		m.set(chave(i, "achado"), 0);
		m.set(POSTOS, n + 1);
	}

	/** Ele chegou perto de uma delas: achou. Uma linha no log, e uma "marca" para as famílias se ela ainda está lá. */
	private static void achados(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg) {
		int n = Math.min(m.get(POSTOS), ANEL);
		for (int i = 0; i < n; i++) {
			if (m.get(chave(i, "achado")) == 1) {
				continue;
			}
			BlockPos pos = new BlockPos(m.get(chave(i, "x")), m.get(chave(i, "y")), m.get(chave(i, "z")));
			if (p.distanceToSqr(Vec3.atCenterOf(pos)) > ACHOU * ACHOU || !level.isLoaded(pos)) {
				continue;
			}
			Tipo tipo = Tipo.values()[Math.floorMod(m.get(chave(i, "tipo")), Tipo.values().length)];
			BlockState s = level.getBlockState(pos);
			boolean esta = !s.isAir();
			m.set(chave(i, "achado"), 1);
			Depuracao.log(p, seg, String.format(Locale.ROOT, "ALGUEM achado tipo=%s pos=%s depois=%ds esta=%s",
					tipo, pos(pos), seg - m.get(chave(i, "seg")), esta ? "sim" : "nao"));
			if (esta) {
				Familias.registrar(p, e, Familias.Familia.MARCA, "ALGUEM_" + tipo, seg);
			}
		}
	}

	private static String pos(BlockPos b) {
		return "(" + b.getX() + "," + b.getY() + "," + b.getZ() + ")";
	}

	// =====================================================================
	// Para os testes
	// =====================================================================

	/** Põe na caverna em volta do jogador agora, sem as travas de tempo e de sorte. */
	public static List<BlockPos> naCavernaParaTeste(ServerPlayer p, boolean galeria) {
		Memoria m = Memoria.de(p);
		List<BlockPos> postos = naCaverna(p.level(), p, m, Diretor.estadoParaTeste(p), RandomSource.create(42L), galeria);
		m.salvar();
		return postos;
	}

	/** Põe perto da casa agora a coisa deste tipo (0, 1, 2: janela, chão, bancada), sem as travas de tempo e de sorte. */
	@Nullable
	public static BlockPos naCasaParaTeste(ServerPlayer p, int qual) {
		Memoria m = Memoria.de(p);
		BlockPos posto = naCasa(p.level(), p, m, DA_CASA[qual], RandomSource.create(42L + qual));
		m.salvar();
		return posto;
	}

	/** Conta, para os testes, se esta posição já foi dada como achada. */
	public static boolean achadoParaTeste(ServerPlayer p, BlockPos pos) {
		Memoria m = Memoria.de(p);
		int n = Math.min(m.get(POSTOS), ANEL);
		for (int i = 0; i < n; i++) {
			if (m.get(chave(i, "x")) == pos.getX() && m.get(chave(i, "y")) == pos.getY() && m.get(chave(i, "z")) == pos.getZ()) {
				return m.get(chave(i, "achado")) == 1;
			}
		}
		return false;
	}
}
