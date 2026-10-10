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
 * Fica desligada no dia a dia. Para rodar (uma hora de jogo leva poucos minutos de relógio):
 *
 *     SUSSURROS_SESSAO=1 ./gradlew runGameTest
 *     SUSSURROS_SESSAO=1 SUSSURROS_SESSAO_SEM_LIMITE=1 ./gradlew runGameTest     (como era sem o orçamento de atenção)
 *     python ferramentas/log/analisar.py build/run/gameTest/sussurros-debug.log --jogador <nome>
 *
 * Os nomes dos dois jogadores saem no log do servidor, numa linha que começa por "[sessao]".
 *
 * Limites, para ninguém ler demais no resultado: o jogador de mentira "reage" sempre do mesmo jeito (para e
 * vira o rosto a cada coisa que acontece), não usa itens, não entra em casa nem em caverna, e não visita a
 * dimensão. Ao ler o resultado, olhe a MISTURA (quantos eventos do Diretor, quantas aparições), não só os
 * intervalos: o analisador põe os alertas no começo do relatório.
 */
public class TestesDeSessao {
	private static final boolean LIGADA = "1".equals(System.getenv("SUSSURROS_SESSAO"));
	/** SUSSURROS_SESSAO_SEM_LIMITE=1 mede o ritmo como era antes do orçamento de atenção, para comparar. */
	private static final boolean SEM_LIMITE = "1".equals(System.getenv("SUSSURROS_SESSAO_SEM_LIMITE"));
	/** Minutos de jogo. SUSSURROS_SESSAO_MINUTOS muda (até 240). */
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
			Diretor.esquecer(jogadores[i]);
			Diretor.definirFase(jogadores[i], fases[i]);
			centros[i] = jogadores[i].position();
		}
		Sussurros.LOGGER.info("[sessao] fase 2 = {}, fase 4 = {}", jogadores[0].getName().getString(), jogadores[1].getName().getString());
		int[] relogio = new int[1];
		double[] saldoAnterior = {Atencao.saldoParaTeste(jogadores[0]), Atencao.saldoParaTeste(jogadores[1])};
		int[] reageAte = new int[2];
		// Não usa helper.onEachTick nem JogadorDeTeste.acompanhar: os dois agendam de uma vez uma tarefa para cada tick
		// até o fim do teste, e o servidor de teste percorre a lista inteira a cada tick. Com um teste de horas isso
		// são centenas de milhares de tarefas, e a sessão andava mais devagar que o jogo de verdade.
		cadaTick(helper, () -> {
			int t = relogio[0]++;
			for (int i = 0; i < 2; i++) {
				// Um jogador de verdade avisa o servidor a cada passo, e é isso que mantém o mundo carregado em volta.
				helper.getLevel().getChunkSource().move(jogadores[i]);
				// Ele "reage" a cada coisa que o mod faz (o saldo de atenção cai quando algo acontece): para por
				// três segundos e vira o rosto. Sem isso o Diretor o lê como indiferente e fica só observando, e a
				// sessão não mostra o que acontece com um jogador de verdade (na alpha12 isso escondeu um defeito).
				double saldo = Atencao.saldoParaTeste(jogadores[i]);
				if (saldo < saldoAnterior[i] - 5) {
					reageAte[i] = t + 60;
				}
				saldoAnterior[i] = saldo;
				if (t < reageAte[i]) {
					if (t % 10 == 0) {
						jogadores[i].snapTo(jogadores[i].getX(), jogadores[i].getY(), jogadores[i].getZ(), jogadores[i].getYRot() + 35.0F, 0.0F);
					}
				} else {
					andar(jogadores[i], centros[i], t);
				}
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

	/** Roda a ação em todo tick, agendando só o tick seguinte de cada vez. */
	private static void cadaTick(GameTestHelper helper, Runnable acao) {
		helper.runAfterDelay(1, () -> {
			acao.run();
			cadaTick(helper, acao);
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
