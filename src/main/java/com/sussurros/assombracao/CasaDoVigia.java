package com.sussurros.assombracao;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.bloco.TigelaOferendaBlock;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

/**
 * A Casa do Vigia (0.9): a casa de quem escreveu o diário.
 *
 * Uma cabana pequena de abeto, uma por jogador, longe da base dele. Do lado de fora, uma fogueira acesa sobre
 * um fardo de feno: a fumaça sobe alto e é o que o faz encontrar o lugar. Dentro não há ninguém. Há a cama
 * dele, um baú com o que ele deixou, a tigela com cinzas, o lampião apagado pendurado no teto, uma placa na
 * parede coberta de riscos (os dias contados) e, no chão diante da porta, a linha de cinza dele: gasta dos
 * lados e rompida no meio.
 *
 * Cada vez que o jogador vai embora e volta, alguma coisa mudou, e há um risco a mais na placa:
 *  - depois da primeira visita, a fogueira está fria e a porta, aberta;
 *  - depois da segunda, o lampião está aceso e a tigela, limpa;
 *  - depois, a porta alterna e os riscos continuam.
 * Ninguém nunca é visto lá. As mudanças só acontecem com o jogador a mais de quarenta blocos.
 *
 * Só blocos, só em terreno livre e longe de qualquer construção dele.
 */
public final class CasaDoVigia {
	static final String EXISTE = "casa_vigia";
	static final String X = "casa_vigia_x";
	static final String Y = "casa_vigia_y";
	static final String Z = "casa_vigia_z";
	static final String VISITAS = "casa_vigia_visitas";
	static final String DENTRO = "casa_vigia_dentro";
	static final String MUDAR = "casa_vigia_mudar";

	/** Riscos na placa no dia em que o jogador a encontra. Cada visita acrescenta um. */
	private static final int RISCOS_INICIAIS = 37;

	private CasaDoVigia() {
	}

	/** Uma vez por segundo, dentro do tick do Diretor. */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase, long seg) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}
		if (m.get(EXISTE) != 1) {
			// 0.9.0-alpha14: a casa é o lugar com mais ferramentas do mod. Espera o primeiro contato.
			if (fase < 2 || !PrimeiroContato.liberado(m) || seg % 20 != 11 || e.forcando || EstruturasSussurros.temCenaAtiva(e)
					|| m.get(Memoria.CHUNKS_VISITADOS) < 20 || p.getRandom().nextFloat() >= 0.15F) {
				return;
			}
			BlockPos pos = EstruturasSussurros.procurarSuperficie(level, p, p.getRandom(), 90, 140, 4);
			if (pos != null && EstruturasSussurros.longeDaCasa(m, pos, 90)) {
				erguer(level, m, pos, p.getRandom());
				Depuracao.log(p, seg, "CASA_DO_VIGIA erguida pos=" + pos.toShortString());
			}
			return;
		}
		BlockPos centro = new BlockPos(m.get(X), m.get(Y), m.get(Z));
		double dist = Math.sqrt(p.blockPosition().distSqr(centro));
		if (dist < 5 && m.get(DENTRO) == 0) {
			m.set(DENTRO, 1);
			m.add(VISITAS, 1);
			m.set(MUDAR, 1);
			Depuracao.log(p, seg, "CASA_DO_VIGIA visita n=" + m.get(VISITAS));
		} else if (dist > 40 && m.get(DENTRO) == 1) {
			m.set(DENTRO, 0);
		}
		if (dist > 40 && m.get(MUDAR) == 1 && level.isLoaded(centro)) {
			m.set(MUDAR, 0);
			String mudou = mudar(level, centro, m.get(VISITAS));
			Depuracao.log(p, seg, "CASA_DO_VIGIA mudou depois da visita " + m.get(VISITAS) + ": " + mudou);
		}
	}

	// ----- As posições, relativas ao centro do piso. A porta fica no lado sul (z positivo). -----

	static BlockPos porta(BlockPos c) {
		return c.offset(0, 0, 2);
	}

	static BlockPos bau(BlockPos c) {
		return c.offset(2, 0, -1);
	}

	static BlockPos tigela(BlockPos c) {
		return c.offset(2, 0, 1);
	}

	static BlockPos lampiao(BlockPos c) {
		return c.offset(0, 2, 0);
	}

	static BlockPos placa(BlockPos c) {
		return c.offset(0, 1, -1);
	}

	static BlockPos fogueira(BlockPos c) {
		return c.offset(2, 0, 4);
	}

	/** Ergue a cabana. c é o bloco de ar no centro do piso. */
	static void erguer(ServerLevel level, Memoria m, BlockPos c, RandomSource sorte) {
		BlockState tabua = Blocks.SPRUCE_PLANKS.defaultBlockState();
		// Alicerce, piso, e o volume inteiro limpo (mato, flor, um degrau de terra).
		for (int x = -3; x <= 3; x++) {
			for (int z = -2; z <= 2; z++) {
				for (int fundo = 2; fundo <= 4 && level.isEmptyBlock(c.offset(x, -fundo, z)); fundo++) {
					level.setBlock(c.offset(x, -fundo, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
				}
				level.setBlock(c.offset(x, -1, z), tabua, 3);
				for (int y = 0; y <= 3; y++) {
					level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		// Paredes: troncos nos cantos, tábuas no resto. Teto de lajes.
		for (int x = -3; x <= 3; x++) {
			for (int z = -2; z <= 2; z++) {
				boolean borda = Math.abs(x) == 3 || Math.abs(z) == 2;
				boolean canto = Math.abs(x) == 3 && Math.abs(z) == 2;
				if (borda) {
					for (int y = 0; y <= 2; y++) {
						level.setBlock(c.offset(x, y, z), canto ? Blocks.SPRUCE_LOG.defaultBlockState() : tabua, 3);
					}
				}
				level.setBlock(c.offset(x, 3, z), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
			}
		}
		// Porta ao sul, duas janelas.
		BlockPos porta = porta(c);
		BlockState folha = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
		level.setBlock(porta, folha.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
		level.setBlock(porta.above(), folha.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
		for (BlockPos janela : new BlockPos[] {c.offset(-2, 1, 2), c.offset(3, 1, 0), c.offset(-3, 1, 0)}) {
			level.setBlock(janela, Blocks.GLASS_PANE.defaultBlockState(), 3);
			level.setBlock(janela, Block.updateFromNeighbourShapes(Blocks.GLASS_PANE.defaultBlockState(), level, janela), 3);
		}
		// A cama dele, encostada na parede do fundo.
		// Na 26.2 as camas são uma coleção por cor, não um campo por cor.
		BlockState cama = Blocks.BED.pick(DyeColor.GRAY).defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
		level.setBlock(c.offset(-2, 0, -1), cama.setValue(BedBlock.PART, BedPart.FOOT), 3);
		level.setBlock(c.offset(-1, 0, -1), cama.setValue(BedBlock.PART, BedPart.HEAD), 3);
		// O baú com o que ele deixou.
		level.setBlock(bau(c), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
		if (level.getBlockEntity(bau(c)) instanceof Container bau) {
			ItemStack[] deixado = {new ItemStack(ModItems.CAIXA_DE_MUSICA), new ItemStack(ModItems.PAGINA_RASGADA, 2),
					new ItemStack(ModItems.CINZA_PALIDA, 4), new ItemStack(ModItems.OSSOS_DE_AGOURO, 2), new ItemStack(ModItems.VELA_PALIDA, 2),
					new ItemStack(Items.BREAD, 3), new ItemStack(Items.BOOK)};
			for (ItemStack item : deixado) {
				int lugar = sorte.nextInt(bau.getContainerSize());
				for (int i = 0; i < bau.getContainerSize() && !bau.getItem(lugar).isEmpty(); i++) {
					lugar = (lugar + 1) % bau.getContainerSize();
				}
				bau.setItem(lugar, item);
			}
		}
		// A tigela com cinzas, o lampião apagado no teto, a placa dos dias.
		level.setBlock(tigela(c), ModBlocos.TIGELA_OFERENDA.defaultBlockState()
				.setValue(TigelaOferendaBlock.CONTEUDO, TigelaOferendaBlock.Conteudo.CINZAS), 3);
		level.setBlock(lampiao(c), ModBlocos.LAMPIAO_PALIDO.defaultBlockState().setValue(LanternBlock.HANGING, true)
				.setValue(LampiaoPalidoBlock.CHAMA, LampiaoPalidoBlock.Chama.APAGADA).setValue(LampiaoPalidoBlock.COMBUSTIVEL, 0), 3);
		level.setBlock(placa(c), Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH), 3);
		riscar(level, placa(c), RISCOS_INICIAIS);
		// A linha de cinza dele, por dentro, diante da porta: gasta dos lados, rompida no meio.
		CinzaEspalhadaBlock.Estado[] linha = {CinzaEspalhadaBlock.Estado.GASTA, CinzaEspalhadaBlock.Estado.ROMPIDA, CinzaEspalhadaBlock.Estado.RISCADA};
		for (int i = 0; i < 3; i++) {
			level.setBlock(c.offset(i - 1, 0, 1), ModBlocos.CINZA_ESPALHADA.defaultBlockState()
					.setValue(CinzaEspalhadaBlock.ESTADO, linha[i]).setValue(CinzaEspalhadaBlock.FRENTE, Direction.SOUTH), 3);
		}
		// Lá fora, a fogueira sobre o feno: a fumaça sobe alto e se vê de longe.
		BlockPos fogueira = fogueira(c);
		if (level.isEmptyBlock(fogueira) || level.getBlockState(fogueira).canBeReplaced()) {
			level.setBlock(fogueira.below(), Blocks.HAY_BLOCK.defaultBlockState(), 3);
			level.setBlock(fogueira, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
					.setValue(CampfireBlock.SIGNAL_FIRE, true), 3);
		}
		m.set(EXISTE, 1);
		m.set(X, c.getX());
		m.set(Y, c.getY());
		m.set(Z, c.getZ());
		m.set(VISITAS, 0);
		m.set(DENTRO, 0);
		m.set(MUDAR, 0);
	}

	/** O que muda enquanto ele está longe. Só mexe no que ainda é o que a casa pôs. Devolve o que mudou, para o log. */
	static String mudar(ServerLevel level, BlockPos c, int visitas) {
		StringBuilder mudou = new StringBuilder();
		riscar(level, placa(c), RISCOS_INICIAIS + visitas);
		mudou.append("riscos=").append(RISCOS_INICIAIS + visitas);
		BlockPos porta = porta(c);
		BlockState folha = level.getBlockState(porta);
		BlockPos fogueira = fogueira(c);
		BlockPos lampiao = lampiao(c);
		BlockPos tigela = tigela(c);
		if (visitas == 1) {
			if (level.getBlockState(fogueira).is(Blocks.CAMPFIRE)) {
				level.setBlock(fogueira, level.getBlockState(fogueira).setValue(CampfireBlock.LIT, false), 3);
				mudou.append(" fogueira=fria");
			}
			abrir(level, porta, folha, true, mudou);
		} else if (visitas == 2) {
			BlockState l = level.getBlockState(lampiao);
			if (l.is(ModBlocos.LAMPIAO_PALIDO)) {
				level.setBlock(lampiao, l.setValue(LampiaoPalidoBlock.CHAMA, LampiaoPalidoBlock.Chama.CALMA)
						.setValue(LampiaoPalidoBlock.COMBUSTIVEL, 1), 3);
				level.scheduleTick(lampiao, ModBlocos.LAMPIAO_PALIDO, 20);
				mudou.append(" lampiao=aceso");
			}
			BlockState t = level.getBlockState(tigela);
			if (t.is(ModBlocos.TIGELA_OFERENDA) && t.getValue(TigelaOferendaBlock.CONTEUDO) == TigelaOferendaBlock.Conteudo.CINZAS) {
				level.setBlock(tigela, t.setValue(TigelaOferendaBlock.CONTEUDO, TigelaOferendaBlock.Conteudo.VAZIA), 3);
				mudou.append(" tigela=limpa");
			}
		} else if (folha.getBlock() instanceof DoorBlock) {
			abrir(level, porta, folha, !folha.getValue(DoorBlock.OPEN), mudou);
		}
		return mudou.toString();
	}

	private static void abrir(ServerLevel level, BlockPos porta, BlockState folha, boolean aberta, StringBuilder mudou) {
		if (folha.getBlock() instanceof DoorBlock bloco && folha.getValue(DoorBlock.OPEN) != aberta) {
			bloco.setOpen(null, level, folha, porta, aberta);
			mudou.append(aberta ? " porta=aberta" : " porta=fechada");
		}
	}

	/** Escreve os riscos na placa: grupos de quatro, três grupos por linha. */
	private static void riscar(ServerLevel level, BlockPos pos, int riscos) {
		if (!(level.getBlockEntity(pos) instanceof SignBlockEntity placa)) {
			return;
		}
		placa.updateText(texto -> {
			int restam = Math.min(riscos, 48);
			for (int linha = 0; linha < 4; linha++) {
				StringBuilder s = new StringBuilder();
				for (int grupo = 0; grupo < 3 && restam > 0; grupo++) {
					int n = Math.min(4, restam);
					s.append("||||", 0, n).append(grupo < 2 ? " " : "");
					restam -= n;
				}
				texto = texto.setMessage(linha, Component.literal(s.toString().trim()));
			}
			return texto;
		}, true);
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
	}

	/** Quantos riscos a placa tem agora (para os testes). */
	public static int riscosNaPlaca(ServerLevel level, BlockPos centro) {
		if (!(level.getBlockEntity(placa(centro)) instanceof SignBlockEntity placa)) {
			return -1;
		}
		int n = 0;
		for (int linha = 0; linha < 4; linha++) {
			for (char ch : placa.getFrontText().getMessage(linha, false).getString().toCharArray()) {
				n += ch == '|' ? 1 : 0;
			}
		}
		return n;
	}

	/** Comando de teste: ergue a casa doze blocos à frente de quem pediu. */
	static String testar(ServerLevel level, ServerPlayer p, Memoria m) {
		var frente = Diretor.pontoRelativo(p, 0, 12.0);
		BlockPos chao = Diretor.acharChao(level, frente.x, p.getY(), frente.z);
		if (chao == null) {
			return "não há chão livre doze blocos à sua frente.";
		}
		erguer(level, m, chao, p.getRandom());
		return "Casa erguida em " + chao.toShortString() + ".";
	}
}
