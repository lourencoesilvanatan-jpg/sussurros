package com.sussurros.bloco;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.Conta;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModItems;

/**
 * Lampião Pálido (0.9): uma luz que queima Cinza Pálida e reage a ele.
 *
 * A chama é o sinal, e fica presa a um objeto do mundo em vez de a um filtro de tela:
 *  - CALMA: nada por perto;
 *  - INQUIETA: ele está a até 20 blocos;
 *  - FRIA: a até 9;
 *  - APAGADA: ele passou a até 4 blocos, ou acabou a cinza. Não reacende sozinha: de manhã, o lampião
 *    apagado no perímetro é a prova de por onde ele passou.
 *
 * Cada carga de cinza dura uns sete minutos (três cargas no máximo). A chama também pode mentir: o Diretor
 * consegue deixá-la inquieta ou fria por alguns segundos sem ninguém ali (ver {@link #perturbar}).
 */
public class LampiaoPalidoBlock extends LanternBlock {
	public enum Chama implements StringRepresentable {
		APAGADA(0), CALMA(12), INQUIETA(8), FRIA(4);

		public final int luz;

		Chama(int luz) {
			this.luz = luz;
		}

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}
	}

	public static final MapCodec<LampiaoPalidoBlock> CODEC_LAMPIAO = simpleCodec(LampiaoPalidoBlock::new);
	public static final EnumProperty<Chama> CHAMA = EnumProperty.create("chama", Chama.class);
	public static final IntegerProperty COMBUSTIVEL = IntegerProperty.create("combustivel", 0, 3);

	private static final double ALCANCE_INQUIETA = 20.0;
	private static final double ALCANCE_FRIA = 9.0;
	private static final double ALCANCE_APAGA = 4.0;

	/** Chamas impostas pelo Diretor por alguns segundos (posição -> até quando e qual). Não é salvo. */
	private record Imposta(long ateTick, Chama chama) {
	}

	private static final Map<BlockPos, Imposta> IMPOSTAS = new HashMap<>();

	public LampiaoPalidoBlock(Properties propriedades) {
		super(propriedades);
		this.registerDefaultState(this.defaultBlockState().setValue(CHAMA, Chama.CALMA).setValue(COMBUSTIVEL, 1));
	}

	@Override
	public MapCodec<? extends LampiaoPalidoBlock> codec() {
		return CODEC_LAMPIAO;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CHAMA, COMBUSTIVEL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		BlockState base = super.getStateForPlacement(contexto);
		// Sai da bancada com uma carga e já aceso.
		return base == null ? null : base.setValue(CHAMA, Chama.CALMA).setValue(COMBUSTIVEL, 1);
	}

	@Override
	protected void onPlace(BlockState estado, Level level, BlockPos pos, BlockState antigo, boolean movido) {
		super.onPlace(estado, level, pos, antigo, movido);
		if (!level.isClientSide() && estado.getValue(CHAMA) != Chama.APAGADA) {
			level.scheduleTick(pos, this, 20);
		}
	}

	/** Cinza Pálida no lampião: mais uma carga, e ele acende. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState estado, Level level, BlockPos pos, Player jogador,
			InteractionHand mao, BlockHitResult acerto) {
		if (!stack.is(ModItems.CINZA_PALIDA)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		int carga = estado.getValue(COMBUSTIVEL);
		if (carga >= 3) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			stack.consume(1, jogador);
			level.setBlock(pos, estado.setValue(COMBUSTIVEL, carga + 1).setValue(CHAMA, Chama.CALMA), 3);
			level.scheduleTick(pos, this, 20);
			level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6F, 0.8F);
			if (jogador instanceof ServerPlayer servidor) {
				Conta.somar(servidor, Conta.Item.LAMPIAO);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Mão vazia: apaga, ou reacende se ainda houver cinza. */
	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jogador, BlockHitResult acerto) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (estado.getValue(CHAMA) != Chama.APAGADA) {
			level.setBlock(pos, estado.setValue(CHAMA, Chama.APAGADA), 3);
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.4F);
		} else if (estado.getValue(COMBUSTIVEL) > 0) {
			level.setBlock(pos, estado.setValue(CHAMA, Chama.CALMA), 3);
			level.scheduleTick(pos, this, 20);
			level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6F, 0.8F);
		} else {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState estado) {
		return estado.getValue(CHAMA) != Chama.APAGADA;
	}

	/** A cinza queima: em média, uma carga a cada sete minutos. */
	@Override
	protected void randomTick(BlockState estado, ServerLevel level, BlockPos pos, RandomSource random) {
		if (estado.getValue(CHAMA) == Chama.APAGADA || random.nextInt(6) != 0) {
			return;
		}
		int carga = estado.getValue(COMBUSTIVEL) - 1;
		if (carga <= 0) {
			level.setBlock(pos, estado.setValue(COMBUSTIVEL, 0).setValue(CHAMA, Chama.APAGADA), 3);
			level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.2F);
		} else {
			level.setBlock(pos, estado.setValue(COMBUSTIVEL, carga), 3);
		}
	}

	/** Uma vez por segundo, enquanto aceso: olha em volta e ajusta a chama. */
	@Override
	protected void tick(BlockState estado, ServerLevel level, BlockPos pos, RandomSource random) {
		Chama atual = estado.getValue(CHAMA);
		if (atual == Chama.APAGADA) {
			return;
		}
		Chama nova = this.chamaPara(level, pos);
		if (nova != atual) {
			level.setBlock(pos, estado.setValue(CHAMA, nova), 3);
			if (nova == Chama.APAGADA) {
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.35F, 0.7F);
				return;
			}
		}
		level.scheduleTick(pos, this, 20);
	}

	private Chama chamaPara(ServerLevel level, BlockPos pos) {
		Vec3 centro = Vec3.atCenterOf(pos);
		List<HospedeEntity> perto = level.getEntitiesOfClass(HospedeEntity.class,
				AABB.ofSize(centro, ALCANCE_INQUIETA * 2, 24, ALCANCE_INQUIETA * 2), h -> !h.isRemoved() && !h.isSumindo());
		double menor = Double.MAX_VALUE;
		for (HospedeEntity h : perto) {
			menor = Math.min(menor, Math.sqrt(h.distanceToSqr(centro)));
		}
		if (menor <= ALCANCE_APAGA) {
			return Chama.APAGADA;
		}
		if (menor <= ALCANCE_FRIA) {
			return Chama.FRIA;
		}
		if (menor <= ALCANCE_INQUIETA) {
			return Chama.INQUIETA;
		}
		Imposta imposta = IMPOSTAS.get(pos);
		if (imposta != null) {
			if (level.getGameTime() < imposta.ateTick()) {
				return imposta.chama();
			}
			IMPOSTAS.remove(pos);
		}
		return Chama.CALMA;
	}

	/**
	 * O Diretor mexe nas chamas sem ninguém ali: todos os lampiões acesos a até "raio" blocos ficam com a
	 * chama pedida por alguns segundos. Devolve quantos reagiram.
	 */
	public static int perturbar(ServerLevel level, BlockPos centro, int raio, Chama chama, int ticks) {
		int n = 0;
		for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-raio, -6, -raio), centro.offset(raio, 6, raio))) {
			BlockState s = level.getBlockState(pos);
			if (s.getBlock() instanceof LampiaoPalidoBlock && s.getValue(CHAMA) != Chama.APAGADA) {
				IMPOSTAS.put(pos.immutable(), new Imposta(level.getGameTime() + ticks, chama));
				n++;
			}
		}
		return n;
	}

	public static void limpar() {
		IMPOSTAS.clear();
	}
}
