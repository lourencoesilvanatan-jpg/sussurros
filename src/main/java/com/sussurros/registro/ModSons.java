package com.sussurros.registro;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	/**
	 * O mesmo som, mas só para um jogador: quem está ao lado não ouve a assombração do outro.
	 * Como no envio normal do jogo, fora do alcance o pacote nem sai.
	 */
	public static void tocarPara(ServerPlayer p, double x, double y, double z, Som som, float volume, float pitch) {
		SoundEvent evento = SONS.get(som);
		if (evento == null) {
			return;
		}
		double alcance = evento.getRange(volume);
		if (p.distanceToSqr(x, y, z) > alcance * alcance) {
			return;
		}
		// A semente vem do próprio jogador, não do mundo: assim não muda os sorteios do Diretor.
		p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(evento), SoundSource.HOSTILE,
				x, y, z, volume, pitch, p.getRandom().nextLong()));
	}

	/** "Dentro da cabeça": o som fica preso ao próprio jogador, então não vem de lado nenhum. Só ele ouve. */
	public static void tocarNaCabeca(ServerPlayer p, Som som, float volume, float pitch) {
		SoundEvent evento = SONS.get(som);
		if (evento != null) {
			tocarNaCabeca(p, evento, volume, pitch);
		}
	}

	/** Igual, para um som que não é do mod (a respiração do próprio jogo, por exemplo). */
	public static void tocarNaCabeca(ServerPlayer p, SoundEvent evento, float volume, float pitch) {
		p.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(evento), SoundSource.HOSTILE,
				p, volume, pitch, p.getRandom().nextLong()));
	}
}
