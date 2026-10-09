package com.sussurros.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import com.sussurros.rede.PacoteSentidos;
import com.sussurros.registro.ModSons;

/**
 * A trilha em camadas. Cada camada é um som em loop, sem posição (toca "dentro da cabeça"), cujo volume
 * segue os sentidos: o fundo grave sobe com o peso da assombração, a camada fina sobe quando algo olha, e a
 * perseguição entra em três partes conforme ele chega perto. Nunca é alta: é para ser sentida antes de ser ouvida.
 *
 * As três camadas da perseguição têm a mesma duração e começam juntas, para ficarem sincronizadas.
 * O batimento é um som curto repetido, e o intervalo encurta com a proximidade.
 */
final class TrilhaCliente {
	private enum Camada {
		FUNDO(ModSons.Som.FUNDO_GRAVE),
		VIGIA(ModSons.Som.FUNDO_VIGIA),
		PULSO(ModSons.Som.CACA_PULSO),
		CORDAS(ModSons.Som.CACA_CORDAS),
		TEMA(ModSons.Som.CACA_TEMA),
		AVESSO(ModSons.Som.AVESSO_AR);

		final ModSons.Som som;

		Camada(ModSons.Som som) {
			this.som = som;
		}
	}

	private static final Camada[] CAMADAS = Camada.values();
	private static final Laco[] ATIVAS = new Laco[CAMADAS.length];
	private static final float[] ALVOS = new float[CAMADAS.length];
	private static final RandomSource SORTE = SoundInstance.createUnseededRandom();

	private static int ateBatimento;
	private static int ateCortarMusica;

	private TrilhaCliente() {
	}

	static void tick(Minecraft mc) {
		float peso = SentidosCliente.peso;
		float vigia = SentidosCliente.vigia;
		float caca = SentidosCliente.caca;

		float fundo = peso * 0.5F + caca * 0.25F + vigia * 0.15F;
		ALVOS[Camada.FUNDO.ordinal()] = fundo < 0.06F ? 0.0F : Math.min(0.8F, fundo);
		ALVOS[Camada.VIGIA.ordinal()] = vigia * 0.55F;
		ALVOS[Camada.PULSO.ordinal()] = suave(caca, 0.12F, 0.5F) * 0.8F;
		ALVOS[Camada.CORDAS.ordinal()] = suave(caca, 0.4F, 0.8F) * 0.7F;
		ALVOS[Camada.TEMA.ordinal()] = suave(caca, 0.65F, 1.0F) * 0.6F;
		ALVOS[Camada.AVESSO.ordinal()] = SentidosCliente.noAvesso() ? 0.75F : 0.0F;

		boolean perseguicao = ALVOS[Camada.PULSO.ordinal()] > 0.0F;
		for (Camada c : CAMADAS) {
			int i = c.ordinal();
			Laco laco = ATIVAS[i];
			if (laco != null && laco.isStopped()) {
				laco = null;
				ATIVAS[i] = null;
			}
			boolean daPerseguicao = c == Camada.PULSO || c == Camada.CORDAS || c == Camada.TEMA;
			boolean precisa = daPerseguicao ? perseguicao : ALVOS[i] > 0.0F;
			if (laco == null && precisa) {
				SoundEvent evento = ModSons.evento(c.som);
				if (evento != null) {
					laco = new Laco(evento, i);
					ATIVAS[i] = laco;
					mc.getSoundManager().play(laco);
				}
			}
		}

		// Batimento: só quando a perseguição já é sentida. De 24 ticks entre batidas até 11.
		if (caca >= 0.2F) {
			if (--ateBatimento <= 0) {
				ateBatimento = Math.round(Mth.lerp(suave(caca, 0.2F, 1.0F), 24.0F, 11.0F));
				SoundEvent batimento = ModSons.evento(ModSons.Som.BATIMENTO);
				if (batimento != null) {
					float volume = 0.25F + 0.45F * caca;
					mc.getSoundManager().play(new SimpleSoundInstance(batimento.location(), SoundSource.HOSTILE, volume,
							0.95F + SORTE.nextFloat() * 0.08F, SORTE, false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
				}
			}
		} else {
			ateBatimento = 0;
		}

		// A música do jogo some enquanto o servidor pedir. O contraste é o silêncio, não um acorde.
		if (SentidosCliente.tem(PacoteSentidos.FLAG_SEM_MUSICA) || caca > 0.25F || SentidosCliente.noAvesso()) {
			if (--ateCortarMusica <= 0) {
				ateCortarMusica = 20;
				mc.getMusicManager().stopPlaying();
			}
		}
	}

	static void parar() {
		for (int i = 0; i < ATIVAS.length; i++) {
			if (ATIVAS[i] != null) {
				ATIVAS[i].encerrar();
				ATIVAS[i] = null;
			}
			ALVOS[i] = 0.0F;
		}
	}

	/** 0 abaixo de "de", 1 acima de "ate", curva suave no meio. */
	private static float suave(float v, float de, float ate) {
		float t = Mth.clamp((v - de) / (ate - de), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	/** Uma camada em loop. O volume anda devagar até o alvo; muda há muito tempo, ela se encerra e libera o canal. */
	private static final class Laco extends AbstractTickableSoundInstance {
		private final int indice;
		private int mudoHa;

		Laco(SoundEvent evento, int indice) {
			super(evento, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
			this.indice = indice;
			this.looping = true;
			this.delay = 0;
			this.volume = 0.001F;
			this.pitch = 1.0F;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			float alvo = ALVOS[this.indice];
			if (alvo > this.volume) {
				this.volume = Math.min(alvo, this.volume + 0.015F);
			} else {
				this.volume = Math.max(Math.max(alvo, 0.001F), this.volume - 0.01F);
			}
			if (alvo <= 0.0F && this.volume <= 0.002F) {
				if (++this.mudoHa > 100) {
					this.stop();
				}
			} else {
				this.mudoHa = 0;
			}
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		void encerrar() {
			this.stop();
		}
	}

	@Nullable
	static Float volumeAtual(String camada) {
		for (Camada c : CAMADAS) {
			if (c.name().equals(camada)) {
				Laco laco = ATIVAS[c.ordinal()];
				return laco == null ? null : laco.getVolume();
			}
		}
		return null;
	}
}
