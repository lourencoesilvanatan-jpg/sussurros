package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.assombracao.Percepcao;
import com.sussurros.assombracao.Saltos;
import com.sussurros.entidade.HospedeEntity;

/**
 * A caçada com chance de verdade (0.9.0-alpha26): o que vem depois de um salto, o que acontece a quem bate nele, o
 * truque que funciona duas vezes, e a captura que não larga o jogador para morrer. As contas puras estão em
 * SaltosTest (JUnit); aqui elas são conferidas com a criatura andando de verdade.
 *
 * Na sessão de 12/10/2026 as duas caçadas do dono acabaram em captura de 14 e 22 segundos depois do aviso, com
 * três saltos sem tempo de reação no meio. Estes testes medem esse tempo.
 */
public class TestesDosSaltos {
	/** Começa a caçada. O lugar onde ele nasce é sorteado, então tenta algumas vezes antes de reclamar. */
	private static void comecar(GameTestHelper helper, ServerPlayer jogador, boolean deVerdade) {
		helper.setTime(18000);
		for (int i = 0; i < 6; i++) {
			if (Diretor.cacadaParaTeste(jogador, deVerdade)) {
				return;
			}
		}
		helper.fail("a caçada não achou lugar para começar");
	}

	private static ServerPlayer jogadorNaFase4(GameTestHelper helper, int dz, String nome) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, dz, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		// Virado para o sul, parado: o jogador de mentira não vira o rosto sozinho.
		jogador.setYRot(0.0F);
		jogador.setXRot(0.0F);
		return jogador;
	}

	/** Ele está nas costas do jogador (o mesmo critério da caçada: só o plano do chão)? */
	private static boolean nasCostas(ServerPlayer jogador, HospedeEntity h) {
		Vec3 virado = Vec3.directionFromRotation(0.0F, jogador.getYRot());
		return Saltos.nasCostas(virado.x, virado.z, h.getX() - jogador.getX(), h.getZ() - jogador.getZ());
	}

	/** Ele está dentro da tela do jogador? */
	private static boolean naTela(ServerPlayer jogador, HospedeEntity h) {
		Vec3 olho = jogador.getEyePosition();
		Vec3 ponto = h.position().add(0, 1.2, 0);
		return jogador.getViewVector(1.0F).dot(ponto.subtract(olho).normalize()) > Percepcao.coneSeguro(jogador);
	}

	/**
	 * O salto do atalho, seis vezes, com o jogador parado de costas para onde ele nasceu: nunca cai nas costas, nunca
	 * na tela, nunca a menos de doze blocos, e depois ele fica parado pelo menos dois segundos. Da última vez ele é
	 * deixado vir: entre o salto e o toque passa a janela inteira, nunca menos de três segundos.
	 * (Até a alpha25, com o jogador parado, o lugar era atrás dele, e ele voltava a andar no tick seguinte.)
	 */
	@GameTest(maxTicks = 1800)
	public void oSaltoNuncaCaiNasCostasEDaTempo(GameTestHelper helper) {
		ServerPlayer jogador = jogadorNaFase4(helper, -9600, "saltos-costas");
		JogadorDeTeste.acompanhar(helper, jogador, "saltos-costas");
		Vec3 inicio = jogador.position();
		int[] relogio = new int[1];
		int[] saltos = new int[1];
		int[] saltouEm = {-1};
		Vec3[] ondeCaiu = new Vec3[1];
		int[] tocouEm = {-1};
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		TestesDePerto.cadaTick(helper, () -> {
			relogio[0]++;
			if (tocouEm[0] >= 0) {
				return;
			}
			if (jogador.position().distanceTo(inicio) > 8) {
				tocouEm[0] = relogio[0];
				helper.assertTrue(saltos[0] == 6, "ele tocou o jogador antes do último salto (no salto " + saltos[0] + ")");
				helper.assertTrue(tocouEm[0] - saltouEm[0] >= 60,
						"entre o último salto e o toque passaram só " + (tocouEm[0] - saltouEm[0]) + " ticks");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
				return;
			}
			HospedeEntity h = Diretor.criatura(jogador);
			if (h == null || !"PERSEGUE".equals(h.getEstagioDaCaca())) {
				return;
			}
			// Parado depois do salto: os dois primeiros segundos ele não se mexe.
			if (saltouEm[0] >= 0 && relogio[0] - saltouEm[0] <= Saltos.ESPERA_MINIMA) {
				helper.assertTrue(h.position().distanceTo(ondeCaiu[0]) < 0.2,
						"ele andou " + String.format("%.2f", h.position().distanceTo(ondeCaiu[0])) + " blocos "
								+ (relogio[0] - saltouEm[0]) + " ticks depois do salto " + saltos[0]);
			}
			// Um salto a cada três segundos e meio; o último é deixado ir até o fim.
			if (saltos[0] < 6 && (saltouEm[0] < 0 || relogio[0] - saltouEm[0] >= 70)) {
				helper.assertTrue(h.saltarParaTeste(), "o salto " + (saltos[0] + 1) + " não achou lugar em campo aberto");
				saltos[0]++;
				saltouEm[0] = relogio[0];
				ondeCaiu[0] = h.position();
				double d = h.distanceTo(jogador);
				helper.assertTrue(d >= Saltos.DISTANCIA_MINIMA - 1.0, "o salto " + saltos[0] + " caiu a " + String.format("%.1f", d) + " blocos");
				helper.assertFalse(nasCostas(jogador, h), "o salto " + saltos[0] + " caiu nas costas do jogador, em "
						+ h.blockPosition().toShortString() + " (jogador em " + jogador.blockPosition().toShortString() + ")");
				helper.assertFalse(naTela(jogador, h), "o salto " + saltos[0] + " caiu dentro da tela");
			}
		});
	}

	/**
	 * Bater nele, de perto: ele some e reaparece longe (dezesseis blocos ou mais, nunca nas costas), fica fora da
	 * caçada pelo menos seis segundos sem tocar ninguém, e antes de voltar os passos dele se ouvem de onde ele está.
	 * (Na sessão de 12/10/2026 ele reapareceu a dez blocos nas costas do jogador e o tocou dois segundos depois.)
	 */
	@GameTest(maxTicks = 1200)
	public void baterNeleCompraTempoEAVoltaTemPassos(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jogador = jogadorNaFase4(helper, -9700, "saltos-ferido");
		JogadorDeTeste.acompanhar(helper, jogador, "saltos-ferido");
		Vec3 inicio = jogador.position();
		int[] relogio = new int[1];
		int[] feridoEm = {-1};
		int[] voltouEm = {-1};
		int[] passos = new int[1];
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		TestesDePerto.cadaTick(helper, () -> {
			relogio[0]++;
			HospedeEntity h = Diretor.criatura(jogador);
			JogadorDeTeste.recebidos(jogador); // passa os sons que chegaram para a conta de sonsVindosDe
			if (h == null) {
				return;
			}
			String estagio = h.getEstagioDaCaca();
			if (feridoEm[0] < 0 && "PERSEGUE".equals(estagio)) {
				// Três blocos à frente do jogador (fora do toque, ao alcance do braço), e o jogador olhando para ele.
				h.snapTo(jogador.getX(), jogador.getY(), jogador.getZ() + 3.0, 180.0F, 0.0F);
				jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
				h.hurtServer(level, level.damageSources().playerAttack(jogador), 1.0F);
				feridoEm[0] = relogio[0];
				helper.assertTrue("FERIDO".equals(h.getEstagioDaCaca()), "ferido, ele deveria sair da caçada; está em " + h.getEstagioDaCaca());
				helper.assertTrue(h.distanceTo(jogador) >= 15.0, "ferido, ele deveria reaparecer longe; está a "
						+ String.format("%.1f", h.distanceTo(jogador)));
				// Para a conta do "nunca nas costas", vale para onde o jogador estava virado na hora do golpe.
				jogador.setYRot(0.0F);
				helper.assertFalse(nasCostas(jogador, h), "ferido, ele reapareceu nas costas do jogador");
				JogadorDeTeste.esquecerSons(jogador);
				return;
			}
			if (feridoEm[0] < 0) {
				return;
			}
			int desde = relogio[0] - feridoEm[0];
			if ("FERIDO".equals(estagio)) {
				passos[0] += JogadorDeTeste.sonsVindosDeQueTem(jogador, h.position(), 1.6, ".step");
				JogadorDeTeste.esquecerSons(jogador);
			} else if (voltouEm[0] < 0) {
				voltouEm[0] = relogio[0];
				helper.assertTrue(desde >= 120, "ele voltou à caçada só " + desde + " ticks depois do golpe");
				helper.assertTrue(passos[0] >= 2, "antes de voltar, os passos dele deveriam ser ouvidos; foram " + passos[0]);
			}
			if (desde <= 120) {
				helper.assertTrue(jogador.position().distanceTo(inicio) < 1.0 && jogador.getHealth() == 20.0F,
						"nos seis segundos depois do golpe ninguém deveria ser tocado (" + desde + " ticks)");
			}
			if (voltouEm[0] >= 0 && relogio[0] - voltouEm[0] >= 20) {
				h.sumir(level, false, "FIM_DO_TESTE");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * O truque funciona duas vezes. Em cima de um pilar, numa caçada de verdade em que ele já usou o pilar uma vez:
	 * nesta ainda funciona (nenhuma travessia, nenhuma captura), e fica anotado. Na seguinte não funciona mais: ele
	 * avisa no bloco e passa, mas só quinze segundos ou mais depois do aviso.
	 */
	@GameTest(maxTicks = 4400)
	public void oPilarFuncionaDuasVezes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = JogadorDeTeste.chao(helper, 4, -9800);
		for (int y = 0; y < 5; y++) {
			level.setBlockAndUpdate(base.above(y), Blocks.COBBLESTONE.defaultBlockState());
		}
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -9800, 0);
		JogadorDeTeste.acompanhar(helper, jogador, "saltos-pilar");
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		Memoria m = Memoria.de(jogador);
		m.set(Memoria.CACADAS, 1);
		m.set(Memoria.CACA_TRUQUES, 1);
		m.salvar();
		int[] relogio = new int[1];
		int[] cacada = {0};
		int[] perseguiuEm = {-1};
		boolean[] atravessou = new boolean[2];
		float[] vida = {20.0F};
		helper.runAfterDelay(5, () -> {
			comecar(helper, jogador, true);
			cacada[0] = 1;
		});
		TestesDePerto.cadaTick(helper, () -> {
			relogio[0]++;
			helper.setTime(18000);
			HospedeEntity h = Diretor.criatura(jogador);
			if (cacada[0] == 1 && h == null && relogio[0] > 20) {
				// A primeira acabou. Ninguém foi pego, e o truque ficou anotado: duas caçadas com ele.
				helper.assertFalse(atravessou[0], "nesta caçada o pilar ainda deveria funcionar");
				helper.assertTrue(jogador.getHealth() == 20.0F, "nesta caçada ninguém deveria ser pego");
				helper.assertTrue(Memoria.de(jogador).get(Memoria.CACA_TRUQUES) == 2,
						"o pilar deveria ter ficado anotado: " + Memoria.de(jogador).get(Memoria.CACA_TRUQUES));
				comecar(helper, jogador, true);
				cacada[0] = 2;
				perseguiuEm[0] = -1;
				return;
			}
			if (h == null) {
				return;
			}
			String estagio = h.getEstagioDaCaca();
			if (perseguiuEm[0] < 0 && "PERSEGUE".equals(estagio)) {
				perseguiuEm[0] = relogio[0];
			}
			if ("ATRAVESSA".equals(estagio) && !atravessou[cacada[0] - 1]) {
				atravessou[cacada[0] - 1] = true;
				if (cacada[0] == 2) {
					helper.assertTrue(perseguiuEm[0] >= 0 && relogio[0] - perseguiuEm[0] >= Saltos.SEM_SALTO_DEPOIS_DO_AVISO,
							"ele atravessou " + (relogio[0] - perseguiuEm[0]) + " ticks depois do aviso: nos quinze segundos seguintes não há salto");
				}
			}
			vida[0] = jogador.getHealth();
			if (cacada[0] == 2 && atravessou[1]) {
				h.sumir(level, false, "FIM_DO_TESTE");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
				return;
			}
			// Olha para ele um segundo a cada quatro: o bastante para ele saber onde o jogador está, sem segurá-lo.
			if (relogio[0] % 80 < 20) {
				jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.position().add(0, 1.2, 0));
			} else {
				jogador.setXRot(-80.0F);
			}
		});
	}

	/**
	 * Fugir para longe fica anotado, uma vez por caçada: correr cinquenta blocos durante o aviso é uma fuga. (É o
	 * que, repetido, destranca o atalho de longe.)
	 */
	@GameTest(maxTicks = 400)
	public void fugirParaLongeFicaAnotado(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jogador = jogadorNaFase4(helper, -9900, "saltos-fuga");
		JogadorDeTeste.acompanhar(helper, jogador, "saltos-fuga");
		helper.runAfterDelay(5, () -> comecar(helper, jogador, true));
		helper.runAfterDelay(40, () -> {
			// Cinquenta blocos para o lado, em cinco pulos, ainda dentro do aviso.
			for (int i = 0; i < 5; i++) {
				jogador.snapTo(jogador.getX() + 10, jogador.getY(), jogador.getZ(), jogador.getYRot(), jogador.getXRot());
			}
		});
		helper.runAfterDelay(60, () -> {
			HospedeEntity h = Diretor.criatura(jogador);
			helper.assertTrue(h != null && !h.isRemoved(), "a caçada deveria estar em curso");
			helper.assertTrue(Memoria.de(jogador).get(Memoria.CACA_FUGAS) == 0, "com a caçada em curso, nada foi anotado ainda");
			h.sumir(level, false, "FIM_DO_TESTE");
		});
		helper.runAfterDelay(70, () -> {
			helper.assertTrue(Memoria.de(jogador).get(Memoria.CACA_FUGAS) == 1,
					"a fuga deveria ter ficado anotada: " + Memoria.de(jogador).get(Memoria.CACA_FUGAS));
			helper.assertTrue(Memoria.de(jogador).get(Memoria.CACA_TRUQUES) == 0, "fugir não é o truque de ficar sem caminho");
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/**
	 * A captura pesa, mas não larga para morrer: a vida não fica abaixo da metade, ele é levado para o lado que tem
	 * luz (há tochas de um lado só, a doze blocos ou mais), e nenhuma tocha é apagada em volta de onde ele acorda.
	 * (Até a alpha25 o lugar preferido era o mais escuro, e as luzes em volta eram apagadas por um minuto.)
	 */
	@GameTest(maxTicks = 1600)
	public void aCapturaNaoLargaNoEscuro(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -10000);
		// Tochas no chão, a leste, de 12 a 44 blocos, a cada três: o único lado com luz.
		for (int dx = 12; dx <= 44; dx += 3) {
			for (int dz = -44; dz <= 44; dz += 3) {
				BlockPos alto = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pes.offset(dx, 0, dz));
				level.setBlock(alto, Blocks.TORCH.defaultBlockState(), 2);
			}
		}
		ServerPlayer jogador = jogadorNaFase4(helper, -10000, "saltos-captura");
		JogadorDeTeste.acompanhar(helper, jogador, "saltos-captura");
		Vec3 inicio = jogador.position();
		boolean[] levado = new boolean[1];
		helper.runAfterDelay(5, () -> comecar(helper, jogador, false));
		TestesDePerto.cadaTick(helper, () -> {
			HospedeEntity h = Diretor.criatura(jogador);
			if (!levado[0] && h != null && h.distanceTo(jogador) < 4.0) {
				// Logo antes do toque, a vida em 12: a captura tiraria 8, e a metade é 10.
				jogador.setHealth(12.0F);
			}
			if (!levado[0] && jogador.position().distanceTo(inicio) > 8) {
				levado[0] = true;
				helper.assertTrue(jogador.getHealth() >= jogador.getMaxHealth() / 2.0F,
						"a captura não pode deixar abaixo da metade da vida: " + jogador.getHealth());
				helper.assertTrue(jogador.getX() > inicio.x + 10,
						"ele deveria ser levado para o lado com luz; foi para " + jogador.blockPosition().toShortString());
				int luz = level.getBrightness(LightLayer.BLOCK, jogador.blockPosition());
				helper.assertTrue(luz >= 6, "onde ele acorda deveria ter luz; tem " + luz);
				long apagadas = Diretor.posicoesDeMiragemParaTeste(jogador, "LUZ_APAGADA").stream()
						.filter(pos -> pos.distSqr(jogador.blockPosition()) <= 10 * 10).count();
				helper.assertTrue(apagadas == 0, "nenhuma tocha deveria ser apagada em volta de onde ele acorda; foram " + apagadas);
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}
}
