package com.sussurros.bloco;

import java.util.Locale;

import com.mojang.serialization.DataResult;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.sussurros.Sussurros;

/**
 * Mostra um item parado no mundo usando a "entidade de exibição" do próprio jogo. Serve para a oferenda na
 * tigela e para os ossos caídos: aparece igual para todo mundo, não precisa de código no cliente e some
 * quando o mod manda.
 *
 * A entidade é criada pelo comando "summon", porque os campos dela não têm como ser preenchidos por fora.
 */
public final class Mostruario {
	private Mostruario() {
	}

	/**
	 * deitar: graus em torno do eixo X (90 deita o item no chão). girar: graus em torno do eixo vertical.
	 * A etiqueta identifica a exibição depois, para tirá-la.
	 */
	public static void mostrar(ServerLevel level, double x, double y, double z, ItemStack item, float escala,
			float deitar, float girar, String etiqueta) {
		if (item.isEmpty()) {
			return;
		}
		DataResult<Tag> codificado = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, level.registryAccess()), item);
		Tag tag = codificado.result().orElse(null);
		if (tag == null) {
			return;
		}
		// Quatérnio de "gira em Y, depois deita em X".
		double mx = Math.toRadians(deitar) / 2.0;
		double my = Math.toRadians(girar) / 2.0;
		double qx = Math.sin(mx) * Math.cos(my);
		double qy = Math.cos(mx) * Math.sin(my);
		double qz = -Math.sin(mx) * Math.sin(my);
		double qw = Math.cos(mx) * Math.cos(my);
		String comando = String.format(Locale.ROOT,
				"summon minecraft:item_display %.3f %.3f %.3f {item:%s,item_display:\"fixed\",Tags:[\"%s\"],"
						+ "transformation:{translation:[0f,0f,0f],left_rotation:[%.4ff,%.4ff,%.4ff,%.4ff],"
						+ "scale:[%.3ff,%.3ff,%.3ff],right_rotation:[0f,0f,0f,1f]}}",
				x, y, z, tag, etiqueta, qx, qy, qz, qw, escala, escala, escala);
		CommandSourceStack fonte = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
		try {
			level.getServer().getCommands().performPrefixedCommand(fonte, comando);
		} catch (RuntimeException ex) {
			Sussurros.LOGGER.warn("Não foi possível mostrar um item no mundo", ex);
		}
	}

	/** Tira as exibições com esta etiqueta no bloco e nos vizinhos imediatos. */
	public static void limpar(ServerLevel level, BlockPos pos, String etiqueta) {
		limpar(level, Vec3.atCenterOf(pos), 1.5, etiqueta);
	}

	public static void limpar(ServerLevel level, Vec3 centro, double raio, String etiqueta) {
		for (Display.ItemDisplay d : level.getEntitiesOfClass(Display.ItemDisplay.class,
				AABB.ofSize(centro, raio * 2, raio * 2, raio * 2), e -> e.entityTags().contains(etiqueta))) {
			d.discard();
		}
	}
}
