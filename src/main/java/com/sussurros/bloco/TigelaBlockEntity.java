package com.sussurros.bloco;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.sussurros.registro.ModBlocos;

/**
 * O que está na Tigela de Oferenda: um item só. O item é mostrado por uma entidade de exibição do próprio
 * jogo (ver {@link Mostruario}), então não precisa de desenho próprio no cliente.
 */
public class TigelaBlockEntity extends BlockEntity {
	private static final String ETIQUETA = "sussurros_oferenda";

	private ItemStack item = ItemStack.EMPTY;

	public TigelaBlockEntity(BlockPos pos, BlockState estado) {
		super(ModBlocos.TIGELA_ENTIDADE, pos, estado);
	}

	public ItemStack item() {
		return this.item;
	}

	/** Põe um item na tigela e o mostra. */
	public void guardar(ServerLevel level, ItemStack novo, BlockState estado) {
		this.item = novo;
		this.setChanged();
		level.setBlock(this.worldPosition, estado.setValue(TigelaOferendaBlock.CONTEUDO, TigelaOferendaBlock.Conteudo.CHEIA), 3);
		Mostruario.limpar(level, this.worldPosition, ETIQUETA);
		Mostruario.mostrar(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.22, this.worldPosition.getZ() + 0.5,
				novo, 0.42F, 90.0F, level.getRandom().nextFloat() * 360.0F, ETIQUETA);
	}

	/** Tira o que houver e deixa a tigela no estado pedido (vazia, ou com cinzas). Devolve o item que saiu. */
	public ItemStack esvaziar(ServerLevel level, BlockState estado, TigelaOferendaBlock.Conteudo depois) {
		ItemStack saiu = this.item;
		this.item = ItemStack.EMPTY;
		this.setChanged();
		Mostruario.limpar(level, this.worldPosition, ETIQUETA);
		level.setBlock(this.worldPosition, estado.setValue(TigelaOferendaBlock.CONTEUDO, depois), 3);
		return saiu;
	}

	@Override
	protected void saveAdditional(ValueOutput saida) {
		super.saveAdditional(saida);
		if (!this.item.isEmpty()) {
			saida.store("item", ItemStack.CODEC, this.item);
		}
	}

	@Override
	protected void loadAdditional(ValueInput entrada) {
		super.loadAdditional(entrada);
		this.item = entrada.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	/** A tigela foi quebrada: o que estava nela cai, e a exibição some. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState estado) {
		if (this.level instanceof ServerLevel servidor) {
			if (!this.item.isEmpty()) {
				Containers.dropItemStack(servidor, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, this.item);
				this.item = ItemStack.EMPTY;
			}
			Mostruario.limpar(servidor, pos, ETIQUETA);
		}
	}
}
