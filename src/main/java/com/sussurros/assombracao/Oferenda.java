package com.sussurros.assombracao;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.TigelaBlockEntity;
import com.sussurros.bloco.TigelaOferendaBlock;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

/**
 * A oferenda (0.9): um pacto pequeno, com regra escondida.
 *
 * O jogador deixa um item na Tigela de Oferenda. Uma vez por noite, quando ninguém está olhando, ele
 * decide. Se aceita:
 *  - o item some, a tigela fica com cinzas e pegadas de cinza saem dela;
 *  - até amanhecer, nada acontece dentro de casa (a trégua vale só para a casa);
 *  - a obsessão cede um pouco.
 * Às vezes, depois de aceitar algumas, ele deixa algo na tigela.
 *
 * O que não aparece para o jogador:
 *  - aceitar não é garantido, e depende do que foi oferecido (comida vale 1, cinza 2, coisa valiosa 3);
 *  - repetir a mesma oferenda vale cada vez menos;
 *  - oferecer uma das ferramentas contra ele (vela, olho, sino, fio, isca) é afronta;
 *  - depois de três noites aceitas, uma noite sem oferenda é desfeita: ele vem à porta.
 *
 * Não pode virar "seja bonzinho e está salvo": mesmo na melhor fase do pacto ele continua aparecendo fora
 * de casa.
 */
public final class Oferenda {
	static final String TEM_TIGELA = "tem_tigela";
	static final String TIGELA_X = "tigela_x";
	static final String TIGELA_Y = "tigela_y";
	static final String TIGELA_Z = "tigela_z";
	static final String ACEITAS = "oferendas_aceitas";
	static final String SEGUIDAS = "oferendas_seguidas";
	static final String ULTIMA = "oferenda_ultima";   // código do último item aceito
	static final String REPETIDAS = "oferenda_repetidas";
	static final String DIA = "oferenda_dia";         // o último dia em que ele passou pela tigela

	private Oferenda() {
	}

	/** O jogador pôs algo numa tigela: ela passa a ser a tigela dele. */
	public static void aoOferecer(ServerPlayer p, BlockPos pos, ItemStack item) {
		Memoria m = Memoria.de(p);
		m.set(TEM_TIGELA, 1);
		m.set(TIGELA_X, pos.getX());
		m.set(TIGELA_Y, pos.getY());
		m.set(TIGELA_Z, pos.getZ());
		m.salvar();
		Depuracao.log(p, p.level().getGameTime() / 20, "OFERENDA posta item=" + BuiltInRegistries.ITEM.getKey(item.getItem())
				+ " pos=" + pos.toShortString());
	}

	/** Dentro de casa, com a oferenda da noite aceita, nada acontece até amanhecer. */
	static boolean emTregua(EstadoJogador e, long tick) {
		return tick < e.treguaCasaAte && e.contexto == ContextoMundo.Tipo.CASA;
	}

	/** Ele veio cobrar a oferenda que faltou. */
	static boolean emDesfeita(EstadoJogador e, long tick) {
		return tick < e.desfeitaAte;
	}

	/** Uma vez por segundo, dentro do tick do Diretor. Ele passa pela tigela uma vez por noite. */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, boolean noite, long seg, long tick) {
		if (m.get(TEM_TIGELA) != 1 || fase < 1 || !noite) {
			return;
		}
		long relogio = level.getDefaultClockTime();
		int dia = (int) (relogio / 24000L);
		long hora = Math.floorMod(relogio, 24000L);
		if (m.get(DIA, -1) == dia || hora < 15000 || seg % 5 != 0) {
			return;
		}
		BlockPos pos = new BlockPos(m.get(TIGELA_X), m.get(TIGELA_Y), m.get(TIGELA_Z));
		if (!level.isLoaded(pos)) {
			return;
		}
		if (!(level.getBlockEntity(pos) instanceof TigelaBlockEntity tigela)) {
			m.set(TEM_TIGELA, 0); // a tigela não existe mais
			return;
		}
		// Só quando ninguém olha: o dono dormindo, longe, ou de costas.
		Vec3 centro = Vec3.atCenterOf(pos);
		boolean olhando = !p.isSleeping() && p.distanceToSqr(centro) < 16 * 16
				&& Diretor.pontoNaFrente(p, centro, Percepcao.conePercebeu(p));
		if (olhando || (!p.isSleeping() && p.distanceToSqr(centro) < 5 * 5)) {
			return;
		}
		m.set(DIA, dia);
		resolver(level, p, m, e, tigela, pos, p.getRandom(), tick, null);
	}

	/**
	 * Quem dorme ao anoitecer pula a hora em que ele passaria pela tigela. Ao acordar de uma noite dormida,
	 * a decisão daquela noite é tomada ali: de manhã a tigela já está como ele a deixou.
	 * Recebe a Memoria de quem chamou e não a salva.
	 */
	static void aoAcordar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long tick) {
		if (m.get(TEM_TIGELA) != 1 || fase < 1) {
			return;
		}
		long relogio = level.getDefaultClockTime();
		long hora = Math.floorMod(relogio, 24000L);
		// A noite pertence ao dia em que começou: de manhã, é a do dia anterior.
		int dia = (int) (relogio / 24000L) - (hora < 12000 ? 1 : 0);
		if (m.get(DIA, -1) == dia) {
			return;
		}
		BlockPos pos = new BlockPos(m.get(TIGELA_X), m.get(TIGELA_Y), m.get(TIGELA_Z));
		if (!level.isLoaded(pos)) {
			return;
		}
		if (!(level.getBlockEntity(pos) instanceof TigelaBlockEntity tigela)) {
			m.set(TEM_TIGELA, 0);
			return;
		}
		m.set(DIA, dia);
		resolver(level, p, m, e, tigela, pos, p.getRandom(), tick, null);
	}

	/**
	 * A decisão dele. aceitar: null no jogo normal; true ou false nos testes, para tirar o sorteio.
	 * Recebe a Memoria de quem chamou e não a salva.
	 */
	static String resolver(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, TigelaBlockEntity tigela, BlockPos pos,
			RandomSource sorte, long tick, @Nullable Boolean aceitar) {
		long seg = tick / 20;
		BlockState estado = level.getBlockState(pos);
		ItemStack item = tigela.item();
		if (item.isEmpty() || estado.getValue(TigelaOferendaBlock.CONTEUDO) != TigelaOferendaBlock.Conteudo.CHEIA) {
			// Nada na tigela. Depois de três noites aceitas, isso é desfeita.
			if (m.get(SEGUIDAS) >= 3) {
				e.desfeitaAte = tick + 20L * 240;
				m.set(SEGUIDAS, 0);
				Depuracao.log(p, seg, "OFERENDA faltou: desfeita por 240s");
				return "DESFEITA";
			}
			Depuracao.log(p, seg, "OFERENDA faltou seguidas=" + m.get(SEGUIDAS));
			m.set(SEGUIDAS, 0);
			return "VAZIA";
		}

		double valor = valor(item);
		int codigo = BuiltInRegistries.ITEM.getId(item.getItem());
		boolean repetida = m.get(ULTIMA, -1) == codigo;
		int repetidas = repetida ? m.get(REPETIDAS) + 1 : 0;

		if (valor < 0) {
			// Afronta: ele não leva, e vem tirar satisfação.
			e.desfeitaAte = tick + 20L * 240;
			Depuracao.log(p, seg, "OFERENDA afronta item=" + BuiltInRegistries.ITEM.getKey(item.getItem()));
			return "AFRONTA";
		}

		double chance = Diretor.limitar(0.45 + 0.15 * valor, 0.0, 0.92) * Math.pow(0.6, repetidas);
		boolean aceita = aceitar != null ? aceitar : sorte.nextDouble() < chance;
		if (!aceita) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "OFERENDA recusada item=%s valor=%.1f repetidas=%d chance=%.2f",
					BuiltInRegistries.ITEM.getKey(item.getItem()), valor, repetidas, chance));
			return "RECUSADA";
		}

		tigela.esvaziar(level, estado, TigelaOferendaBlock.Conteudo.CINZAS);
		m.add(ACEITAS, 1);
		m.add(SEGUIDAS, 1);
		m.set(ULTIMA, codigo);
		m.set(REPETIDAS, repetidas);
		m.add(Memoria.INQUIETACAO, -30);
		m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
		e.obsessao = Diretor.limitar(e.obsessao - 3.0 * valor, 0, 100);
		// A trégua vai até o amanhecer.
		long hora = Math.floorMod(level.getDefaultClockTime(), 24000L);
		e.treguaCasaAte = tick + Math.max(600, (hora <= 23500 ? 23500 - hora : 0));
		int pegadas = rastro(level, pos, m, sorte);

		// Depois de algumas aceitas, de vez em quando ele deixa algo.
		String presente = "-";
		if (m.get(ACEITAS) >= 2 && (aceitar != null ? false : sorte.nextFloat() < 0.25F)) {
			ItemStack deixado;
			if (m.get(Memoria.PAGINAS_ENTREGUES) < Diario.TOTAL_PAGINAS) {
				m.add(Memoria.PAGINAS_ENTREGUES, 1);
				deixado = new ItemStack(ModItems.PAGINA_RASGADA);
			} else {
				deixado = new ItemStack(sorte.nextBoolean() ? ModItems.CINZA_PALIDA : Items.BONE);
			}
			tigela.guardar(level, deixado, level.getBlockState(pos));
			presente = BuiltInRegistries.ITEM.getKey(deixado.getItem()).toString();
		}
		Depuracao.log(p, seg, String.format(Locale.ROOT,
				"OFERENDA aceita item=%s valor=%.1f repetidas=%d chance=%.2f aceitas=%d seguidas=%d tregua=%ds pegadas=%d presente=%s",
				BuiltInRegistries.ITEM.getKey(item.getItem()), valor, repetidas, chance, m.get(ACEITAS), m.get(SEGUIDAS),
				(e.treguaCasaAte - tick) / 20, pegadas, presente));
		return "ACEITA";
	}

	/** Quanto ele dá pelo que foi oferecido. Negativo é afronta. */
	static double valor(ItemStack item) {
		if (item.is(ModItems.VELA_PALIDA) || item.is(ModItems.OLHO_SUSSURRANTE) || item.is(ModItems.SINO_OCO)
				|| item.is(ModItems.FIO_VIGILIA) || item.is(ModItems.ISCA_PALIDA)) {
			return -1;
		}
		if (item.is(ModItems.CINZA_PALIDA)) {
			return 2;
		}
		if (item.is(Items.GOLD_INGOT) || item.is(Items.DIAMOND) || item.is(Items.EMERALD) || item.is(Items.AMETHYST_SHARD)
				|| item.is(Items.GOLDEN_APPLE) || item.is(Items.GOLDEN_CARROT)) {
			return 3;
		}
		if (item.has(DataComponents.FOOD)) {
			return 1;
		}
		return 0.5;
	}

	/** Pegadas de cinza saindo da tigela, para o lado oposto ao da cama. Somem sozinhas em um dia de jogo. */
	private static int rastro(ServerLevel level, BlockPos tigela, Memoria m, RandomSource sorte) {
		Direction lado = Direction.Plane.HORIZONTAL.getRandomDirection(sorte);
		if (m.get(Memoria.TEM_CAMA) == 1) {
			lado = Direction.getApproximateNearest(tigela.getX() - m.get(Memoria.CAMA_X), 0.0, tigela.getZ() - m.get(Memoria.CAMA_Z));
			if (lado.getAxis().isVertical()) {
				lado = Direction.NORTH;
			}
		}
		int postas = 0;
		for (int passo = 1; passo <= 5 && postas < 3; passo++) {
			BlockPos pos = tigela.relative(lado, passo);
			if (pegada(level, pos, lado) || pegada(level, pos.below(), lado) || pegada(level, pos.above(), lado)) {
				postas++;
			}
		}
		return postas;
	}

	/** Põe uma pegada de cinza virada para "rumo", se couber. É temporária e não substitui nada. */
	static boolean pegada(ServerLevel level, BlockPos pos, Direction rumo) {
		BlockState marca = ModBlocos.CINZA_ESPALHADA.defaultBlockState()
				.setValue(CinzaEspalhadaBlock.ESTADO, CinzaEspalhadaBlock.Estado.PEGADA)
				.setValue(CinzaEspalhadaBlock.FRENTE, rumo);
		if (!level.getBlockState(pos).isAir() || !marca.canSurvive(level, pos)) {
			return false;
		}
		return AlteracoesTemporarias.substituir(level, pos, marca, 24000, "PEGADA");
	}
}
