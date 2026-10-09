package com.sussurros.teste;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Atencao;
import com.sussurros.assombracao.Diretor;
import com.sussurros.rede.Rede;

/**
 * A sessão sintética: uma hora de jogo com dois jogadores de mentira, um na fase 2 e outro na fase 4, andando
 * em campo aberto com pausas. Não diz se o mod assusta. Diz se o RITMO está dentro do esperado antes de gastar
 * o tempo do dono: o log que ela deixa passa por ferramentas/log/analisar.py.
 *
 * Fica desligada no dia a dia (leva uns quinze minutos de relógio). Para rodar:
 *
 *     SUSSURROS_SESSAO=1 ./gradlew runGameTest
 *     SUSSURROS_SESSAO=1 SUSSURROS_SESSAO_SEM_LIMITE=1 ./gradlew runGameTest     (como era sem o orçamento de atenção)
 *     python ferramentas/log/analisar.py build/run/gameTest/sussurros-debug.log --jogador <nome>
 *
 * Os nomes dos dois jogadores saem no log do servidor, numa linha que começa por "[sessao]".
 *
 * Limites, para ninguém ler demais no resultado: o jogador de mentira não reage (o Diretor o vê como alguém
 * indiferente), não usa itens, não entra em casa nem em caverna, e não visita a dimensão.
 */
public class TestesDeSessao {
	private static final boolean LIGADA = "1".equals(System.getenv("SUSSURROS_SESSAO"));
	/** SUSSURROS_SESSAO_SEM_LIMITE=1 mede o ritmo como era antes do orçamento de atenção, para comparar. */
	private static final boolean SEM_LIMITE = "1".equals(System.getenv("SUSSURROS_SESSAO_SEM_LIMITE"));
	/** Minutos de jogo. SUSSURROS_SESSAO_MINUTOS muda (o padrão, uma hora, leva uns nove minutos de relógio). */
	private static final int TICKS = 20 * 60 * Integer.parseInt(System.getenv().getOrDefault("SUSSURROS_SESSAO_MINUTOS", "60"));

	@GameTest(maxTicks = 20 * 60 * 60 * 4)
	public void umaHoraDeJogo(GameTestHelper helper) {
		if (!LIGADA) {
			helper.succeed();
			return;
		}
		// Sem isto, o que só existe com o mod no cliente nunca seria sorteado para um jogador de mentira.
		Rede.fingirClienteNosTestes = true;
		Atencao.semLimiteNosTestes = SEM_LIMITE;
		// O servidor de teste roda quase em tempo real. Com isto ele corre os ticks o mais depressa que consegue.
		helper.getLevel().getServer().tickRateManager().requestGameToSprint(TICKS + 400);
		ServerPlayer[] jogadores = {
				JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3000, 0),
				JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -3400, 0)};
		int[] fases = {2, 4};
		Vec3[] centros = new Vec3[2];
		for (int i = 0; i < 2; i++) {
			JogadorDeTeste.acompanhar(helper, jogadores[i], "sessao-fase-" + fases[i]);
			Diretor.esquecer(jogadores[i]);
			Diretor.definirFase(jogadores[i], fases[i]);
			centros[i] = jogadores[i].position();
		}
		Sussurros.LOGGER.info("[sessao] fase 2 = {}, fase 4 = {}", jogadores[0].getName().getString(), jogadores[1].getName().getString());
		int[] relogio = new int[1];
		helper.onEachTick(() -> {
			int t = relogio[0]++;
			for (int i = 0; i < 2; i++) {
				andar(jogadores[i], centros[i], t);
			}
		});
		helper.runAfterDelay(TICKS, () -> {
			Rede.fingirClienteNosTestes = false;
			Atencao.semLimiteNosTestes = false;
			for (ServerPlayer jogador : jogadores) {
				JogadorDeTeste.remover(jogador);
			}
			helper.succeed();
		});
	}

	/**
	 * Um quadrado de 32 blocos de lado, a quatro blocos por segundo (o passo de quem anda), com meio minuto
	 * parado a cada dois minutos e meio. O rosto acompanha a direção em que ele anda.
	 */
	private static void andar(ServerPlayer jogador, Vec3 centro, int tick) {
		int ciclo = tick % (20 * 150);
		if (ciclo >= 20 * 120) {
			return; // parado
		}
		// Só conta o tempo andado, para ele retomar de onde parou.
		int andado = (tick / (20 * 150)) * (20 * 120) + ciclo;
		double percorrido = andado * 0.2;
		double lado = 32.0;
		double noLado = percorrido % (lado * 4);
		int qual = (int) (noLado / lado);
		double d = noLado % lado - lado / 2;
		double x;
		double z;
		float rosto;
		switch (qual) {
			case 0 -> {
				x = d;
				z = -lado / 2;
				rosto = -90.0F;
			}
			case 1 -> {
				x = lado / 2;
				z = d;
				rosto = 0.0F;
			}
			case 2 -> {
				x = -d;
				z = lado / 2;
				rosto = 90.0F;
			}
			default -> {
				x = -lado / 2;
				z = -d;
				rosto = 180.0F;
			}
		}
		jogador.snapTo(centro.x + x, centro.y, centro.z + z, rosto, 0.0F);
	}
}
