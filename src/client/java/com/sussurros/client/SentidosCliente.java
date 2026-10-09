package com.sussurros.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteCampo;
import com.sussurros.rede.PacoteEfeito;
import com.sussurros.rede.PacoteSentidos;

/**
 * O lado do cliente dos "sentidos": guarda o que o servidor mandou e leva os valores até lá aos poucos.
 *
 * O servidor fala uma vez por segundo; aqui cada valor anda um pouco por tick, então a cor, a borda, a
 * neblina e a trilha mudam de forma contínua. Tudo o que desenha ou toca (TelaSentidos, CorDrenada,
 * NeblinaSussurros, TrilhaCliente, EcoDePasso) lê daqui.
 */
public final class SentidosCliente {
	private static float pesoAlvo;
	private static float vigiaAlvo;
	private static float cacaAlvo;
	private static float neblinaAlvo;
	private static int flags;

	static float peso;
	static float vigia;
	static float caca;
	static float neblina;

	/** Ticks de cliente desde que entrou no mundo. Serve de relógio para a "respiração" dos efeitos. */
	static int idade;
	private static int semPacote;

	// Efeitos pontuais. "Escuro" vai de 0 (olhos abertos) a 1 (preto).
	private static int piscarTotal;
	private static int piscarPassado;
	private static boolean apagado;
	private static int apagaoFecha;
	private static int apagaoPassado;
	private static int apagaoMaximo;
	private static int acordarTotal;
	private static int acordarPassado = -1;
	private static float escuroAnterior;
	private static float escuroAtual;

	private static float campoEnviado = -1;
	private static float proporcaoEnviada = -1;
	private static int campoHa;

	private SentidosCliente() {
	}

	public static void registrar() {
		ClientPlayNetworking.registerGlobalReceiver(PacoteSentidos.TIPO, (pacote, contexto) -> receber(pacote));
		ClientPlayNetworking.registerGlobalReceiver(PacoteEfeito.TIPO, (pacote, contexto) -> efeito(pacote));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> zerar());
		ClientTickEvents.END_CLIENT_TICK.register(SentidosCliente::tick);
	}

	static boolean tem(int flag) {
		return (flags & flag) != 0;
	}

	static boolean noAvesso() {
		return tem(PacoteSentidos.FLAG_AVESSO);
	}

	/**
	 * No Véu, tudo o que é entidade deixa de ser desenhado para este jogador, menos o Hóspede e ele mesmo.
	 * Quem pergunta é o EsconderNoVeuMixin.
	 */
	public static boolean esconde(Entity entidade) {
		return tem(PacoteSentidos.FLAG_VEU) && !(entidade instanceof HospedeEntity) && entidade != Minecraft.getInstance().player;
	}

	private static void receber(PacoteSentidos pacote) {
		pesoAlvo = pacote.peso();
		vigiaAlvo = pacote.vigia();
		cacaAlvo = pacote.caca();
		neblinaAlvo = pacote.neblina();
		flags = pacote.flags();
		semPacote = 0;
	}

	private static void efeito(PacoteEfeito pacote) {
		int duracao = Math.max(1, pacote.duracao());
		switch (pacote.qual()) {
			case PISCAR -> {
				piscarTotal = duracao;
				piscarPassado = 0;
			}
			case APAGAO -> {
				apagado = true;
				apagaoFecha = Math.min(duracao, 14);
				apagaoPassado = 0;
				// Trava de segurança: se o "acordar" não chegar (queda de conexão, erro), a tela abre sozinha.
				apagaoMaximo = duracao + 200;
				acordarPassado = -1;
			}
			case ACORDAR -> {
				apagado = false;
				acordarTotal = duracao;
				acordarPassado = 0;
			}
			case TREMOR -> {
				// Reservado: o tremor de câmera precisa de um gancho no desenho do mundo que ainda não existe.
			}
		}
	}

	private static void zerar() {
		pesoAlvo = vigiaAlvo = cacaAlvo = neblinaAlvo = 0;
		peso = vigia = caca = neblina = 0;
		flags = 0;
		idade = 0;
		semPacote = 0;
		piscarTotal = 0;
		apagado = false;
		acordarPassado = -1;
		escuroAnterior = escuroAtual = 0;
		campoEnviado = -1;
		TrilhaCliente.parar();
	}

	private static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		idade++;

		// O servidor parou de falar (outra dimensão, mod desligado no servidor): tudo volta ao neutro, devagar.
		if (++semPacote > 100) {
			pesoAlvo = vigiaAlvo = cacaAlvo = neblinaAlvo = 0;
			flags = 0;
		}

		peso = andar(peso, pesoAlvo, 0.004F, 0.004F);
		vigia = andar(vigia, vigiaAlvo, 0.025F, 0.012F);
		caca = andar(caca, cacaAlvo, 0.04F, 0.008F);
		neblina = andar(neblina, neblinaAlvo, 0.008F, 0.006F);

		escuroAnterior = escuroAtual;
		escuroAtual = calcularEscuro();
		// De olhos fechados o mundo troca de uma vez: quando a tela abre, já está tudo como vai ficar.
		if (escuroAtual > 0.9F) {
			peso = pesoAlvo;
			vigia = vigiaAlvo;
			neblina = neblinaAlvo;
		}

		CorDrenada.tick(mc);
		TrilhaCliente.tick(mc);
		EcoDePasso.tick(mc);
		enviarCampo(mc);
	}

	private static float andar(float atual, float alvo, float sobe, float desce) {
		if (alvo > atual) {
			return Math.min(alvo, atual + sobe);
		}
		return Math.max(alvo, atual - desce);
	}

	/** Avança os efeitos pontuais um tick e devolve o quanto a tela está fechada agora (0 a 1). */
	private static float calcularEscuro() {
		float escuro = 0;

		if (piscarTotal > 0) {
			float t = piscarPassado / (float) piscarTotal;
			// Fecha rápido, segura, abre um pouco mais devagar.
			float alfa = t < 0.25F ? t / 0.25F : t > 0.65F ? (1.0F - t) / 0.35F : 1.0F;
			escuro = Math.max(escuro, Mth.clamp(alfa, 0.0F, 1.0F));
			if (++piscarPassado > piscarTotal) {
				piscarTotal = 0;
			}
		}

		if (apagado) {
			float alfa = apagaoFecha <= 0 ? 1.0F : Math.min(1.0F, apagaoPassado / (float) apagaoFecha);
			escuro = Math.max(escuro, alfa);
			if (++apagaoPassado > apagaoMaximo) {
				apagado = false;
				acordarTotal = 40;
				acordarPassado = 0;
			}
		} else if (acordarPassado >= 0) {
			float t = acordarPassado / (float) Math.max(1, acordarTotal);
			// Abre devagar no começo: os olhos demoram a focar.
			escuro = Math.max(escuro, 1.0F - t * t);
			if (++acordarPassado > acordarTotal) {
				acordarPassado = -1;
			}
		}
		return escuro;
	}

	/** O quanto a tela está fechada, suavizado entre dois ticks. */
	static float escuro(float parcial) {
		return Mth.lerp(parcial, escuroAnterior, escuroAtual);
	}

	/** Conta ao servidor o campo de visão de verdade, quando muda e de tempos em tempos. */
	private static void enviarCampo(Minecraft mc) {
		if (++campoHa < 10) {
			return;
		}
		int largura = mc.getWindow().getWidth();
		int altura = mc.getWindow().getHeight();
		if (largura <= 0 || altura <= 0) {
			return;
		}
		float fov = mc.gameRenderer.mainCamera().getFov();
		float proporcao = largura / (float) altura;
		boolean mudou = Math.abs(fov - campoEnviado) > 1.5F || Math.abs(proporcao - proporcaoEnviada) > 0.02F;
		if (!mudou && campoHa < 200) {
			return;
		}
		if (!ClientPlayNetworking.canSend(PacoteCampo.TIPO)) {
			return;
		}
		campoHa = 0;
		campoEnviado = fov;
		proporcaoEnviada = proporcao;
		ClientPlayNetworking.send(new PacoteCampo(fov, proporcao));
	}
}
