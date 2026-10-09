package com.sussurros.assombracao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.loader.api.FabricLoader;

import com.sussurros.Sussurros;

/**
 * A atenção do jogador (0.9): um orçamento só, por jogador, para tudo o que o mod empurra para ele.
 *
 * Antes cada sistema tinha o seu relógio (o Diretor, a Atmosfera, as cinco cenas, os avisos da Conta, o
 * baralho) e nenhum sabia dos outros: o ritmo final não era decidido por ninguém, era a soma. No log de
 * 08/10/2026, antes de a expansão acrescentar mais fontes, a soma já dava 45 saídas por hora, com mediana de
 * 56 s entre uma e outra. É muito: o que acontece a cada minuto vira rotina, e rotina não assusta.
 *
 * Como funciona:
 *  - o jogador tem um saldo que volta devagar (mais depressa nas fases altas);
 *  - cada saída perceptível custa: um evento do Diretor custa 12 mais a intensidade dele; um presságio, 14;
 *    uma perturbação de ambiente, 20; o começo de uma cena, 36; um aviso da Conta, 10; uma caçada, 50;
 *  - para COMEÇAR algo novo é preciso ter saldo e ter passado o respiro mínimo desde a última saída;
 *  - o que já começou termina: elos de cadeia, a sequência de ameaça e os passos de uma cena não esperam o
 *    saldo, só o gastam (ele pode ficar negativo, e aí o silêncio seguinte é mais longo);
 *  - com o Diretor recuando (depois de um pico, de uma captura), nada começa e o saldo não volta: a trégua
 *    é trégua para todos os sistemas.
 *
 * Fica de fora o que é contínuo (cor, fundo sonoro), o que é resposta direta a uma ação do jogador (usar um
 * item, atravessar a Soleira) e o que ele só encontra se olhar (as chamas pálidas, o boneco, a tigela).
 *
 * O ritmo tem três perfis, para o dono comparar sem ler o que muda: calmo, padrão e intenso. O intenso é
 * perto do que o mod fazia antes desta classe existir.
 */
public final class Atencao {
	public enum Ritmo {
		CALMO(0.7), PADRAO(1.0), INTENSO(3.0);

		final double fator;

		Ritmo(double fator) {
			this.fator = fator;
		}
	}

	static final double CAPACIDADE = 60.0;
	/** Quanto do saldo volta por segundo, por fase, no ritmo padrão. */
	private static final double[] VOLTA = {0.06, 0.08, 0.10, 0.14, 0.20};
	/** Respiro mínimo, em segundos, entre uma saída e o começo da seguinte, por fase, no ritmo padrão. */
	private static final int[] RESPIRO = {120, 110, 90, 60, 45};

	static final double MINIMO = 16.0;          // o evento mais barato que existe (intensidade 4)
	static final double PRESSAGIO = 14.0;
	static final double MICROCENA = 20.0;
	static final double CENA = 36.0;
	static final double AVISO_DA_CONTA = 10.0;
	static final double CARTA = 16.0;
	static final double VEU = 32.0;
	static final double CACADA = 50.0;

	/** Só para a sessão sintética: mede o ritmo sem o limite (os gastos continuam no log). */
	public static boolean semLimiteNosTestes;

	private static final String ARQUIVO = "sussurros-ajustes.properties";
	private static Ritmo ritmo = Ritmo.PADRAO;

	private Atencao() {
	}

	static double custo(int intensidade) {
		return 12.0 + intensidade;
	}

	/** Dá para começar algo novo agora? O que é forçado por comando de teste sempre pode. */
	static boolean podeGastar(EstadoJogador e, double custo, long seg) {
		if (e.forcando || semLimiteNosTestes) {
			return true;
		}
		return e.estado != EstadoDiretor.RECUANDO && seg >= e.atencaoLivreEm && e.atencaoSaldo >= custo;
	}

	/** Uma saída aconteceu. Não confere nada: quem precisava conferir já chamou podeGastar. */
	static void gastar(ServerPlayer p, EstadoJogador e, String fonte, double custo, long seg) {
		if (e.forcando) {
			return;
		}
		e.atencaoSaldo = Math.max(-CAPACIDADE, e.atencaoSaldo - custo);
		e.atencaoLivreEm = seg + (long) (RESPIRO[e.atencaoFase] / ritmo.fator);
		Depuracao.log(p, seg, String.format(Locale.ROOT, "ATENCAO fonte=%s custo=%.0f saldo=%.0f", fonte, custo, e.atencaoSaldo));
	}

	/** Uma vez por segundo, no começo do tick do Diretor: o saldo volta. */
	static void segundo(EstadoJogador e, int fase) {
		e.atencaoFase = Math.max(0, Math.min(4, fase));
		if (e.estado != EstadoDiretor.RECUANDO) {
			e.atencaoSaldo = Math.min(CAPACIDADE, e.atencaoSaldo + VOLTA[e.atencaoFase] * ritmo.fator);
		}
	}

	// ----- O ritmo escolhido (vale para o servidor inteiro, e fica guardado na pasta de configuração) -----

	public static Ritmo ritmo() {
		return ritmo;
	}

	/** Lê o ritmo guardado. Chamado quando o mod carrega. */
	public static void carregar() {
		Path arquivo = FabricLoader.getInstance().getConfigDir().resolve(ARQUIVO);
		if (!Files.exists(arquivo)) {
			return;
		}
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(arquivo)) {
			p.load(in);
			ritmo = Ritmo.valueOf(p.getProperty("ritmo", "padrao").trim().toUpperCase(Locale.ROOT));
		} catch (IOException | IllegalArgumentException ex) {
			Sussurros.LOGGER.warn("Não foi possível ler {}: vale o ritmo padrão", ARQUIVO, ex);
			ritmo = Ritmo.PADRAO;
		}
	}

	/** Troca o ritmo e guarda a escolha. */
	public static void definirRitmo(Ritmo novo) {
		ritmo = novo;
		Properties p = new Properties();
		p.setProperty("ritmo", novo.name().toLowerCase(Locale.ROOT));
		Path arquivo = FabricLoader.getInstance().getConfigDir().resolve(ARQUIVO);
		try (OutputStream out = Files.newOutputStream(arquivo)) {
			p.store(out, "Sussurros: ritmo = calmo, padrao ou intenso");
		} catch (IOException ex) {
			Sussurros.LOGGER.warn("Não foi possível gravar {}", ARQUIVO, ex);
		}
	}

	/** Só para os testes: pergunta e gasta como um sistema do mod faria, no segundo atual do mundo. */
	public static boolean podeParaTeste(ServerPlayer p, double custo) {
		return podeGastar(Diretor.estadoParaTeste(p), custo, p.level().getGameTime() / 20);
	}

	public static void gastarParaTeste(ServerPlayer p, double custo) {
		gastar(p, Diretor.estadoParaTeste(p), "teste", custo, p.level().getGameTime() / 20);
	}

	/** Só para os testes: o saldo de um jogador. */
	public static double saldoParaTeste(ServerPlayer p) {
		return Diretor.estadoParaTeste(p).atencaoSaldo;
	}
}
