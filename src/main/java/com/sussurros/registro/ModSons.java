package com.sussurros.registro;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import com.sussurros.Sussurros;

/**
 * Sons próprios do mod (v0.4.2a): sintetizados do zero, sem material de terceiros.
 * Os arquivos ficam em assets/sussurros/sounds/ e a lista em assets/sussurros/sounds.json.
 *
 * TUDO o que é som próprio passa por aqui. Se o registro der problema numa versão do Minecraft,
 * troque este arquivo pela versão em alternativas/ModSons_sem_sons.java: o resto do mod continua igual,
 * só sem esses sons.
 */
public final class ModSons {
	public enum Som {
		PANO,       // tecido se mexendo, bem curto
		RESPIRACAO, // respiração distante, sem voz
		MADEIRA,    // madeira tensionando
		ARRASTO,    // algo pesado arrastado devagar
		ESTALO,     // estalo seco, como galho pisado
		GRAVE       // baque surdo e grave
	}

	private static final Map<Som, SoundEvent> SONS = new EnumMap<>(Som.class);

	private ModSons() {
	}

	public static void inicializar() {
		for (Som som : Som.values()) {
			Identifier id = Sussurros.id(som.name().toLowerCase(Locale.ROOT));
			SoundEvent evento = SoundEvent.createVariableRangeEvent(id);
			Registry.register(BuiltInRegistries.SOUND_EVENT, ResourceKey.create(Registries.SOUND_EVENT, id), evento);
			SONS.put(som, evento);
		}
	}

	/** Toca um som próprio no mundo. Volume acima de 1 só aumenta o alcance (16 blocos x volume). */
	public static void tocar(ServerLevel level, double x, double y, double z, Som som, float volume, float pitch) {
		SoundEvent evento = SONS.get(som);
		if (evento != null) {
			level.playSound(null, x, y, z, evento, SoundSource.HOSTILE, volume, pitch);
		}
	}
}
