package com.sussurros.teste;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Diario;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModItems;

/**
 * Ele passa por vão de um bloco (0.9.0-alpha24).
 *
 * Os quatro primeiros testes cavam o lugar dentro de um maciço de pedra, para o único caminho ser o vão: uma sala
 * com um buraco rente ao chão, a mesma sala com uma janela na altura dos olhos, duas câmaras ligadas por um
 * túnel de um bloco, e um túnel comprido que desemboca na sala. O quinto roda a caçada no pedaço de caverna do mundo do dono em que ela aconteceu em
 * 11/10/2026, com ele nascendo onde nasceu. Os dois últimos são do que entrou junto: o piscar debaixo de teto, e
 * as páginas do diário que chegam depois da caçada.
 */
public class TestesDoVao {
	/** Um maciço de pedra de (x0,-1,z0) a (x1,4,z1), contado dos pés do jogador. O que não for cavado é parede. */
	private static void macico(ServerLevel level, BlockPos pes, int x0, int z0, int x1, int z1) {
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(x0, -1, z0), pes.offset(x1, 4, z1))) {
			level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
		}
	}

	private static void cavar(ServerLevel level, BlockPos pes, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(x0, y0, z0), pes.offset(x1, y1, z1))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
	}

	/** Atravessar já valeria (ele já usou o truque em duas caçadas), e o teste confere que não foi por aí. */
	private static ServerPlayer jogadorJaCacado(GameTestHelper helper, BlockPos pes) {
		ServerPlayer jogador = TestesDePerto.jogadorNoTunel(helper, pes);
		Diretor.definirFase(jogador, 4);
		TestesDaCacada.jaUsouOTruque(jogador);
		return jogador;
	}

	private static int sonsDoVao(ServerPlayer jogador, BlockPos boca) {
		Vec3 ponto = Vec3.atCenterOf(boca);
		return JogadorDeTeste.sonsVindosDe(jogador, ponto, 2.0, "sussurros:arrasto")
				+ JogadorDeTeste.sonsVindosDe(jogador, ponto, 2.0, "sussurros:arranhar");
	}

	/** O que um teste anota, tick a tick, de uma caçada. */
	private static final class Anotacoes {
		int relogio;
		boolean houve;
		boolean atravessou;
		int dentroDeBloco;
		int deQuatroDesde = -1;
		float alturaDeQuatro = 9.0F;
		int noVaoDesde = -1;
		boolean deQuatroNoVao;
		int sonsAoEntrar = -1;
		boolean dePeDepois;
		Vec3 inicio;
	}

	/**
	 * Fechado numa sala, de costas para um buraco de um bloco rente ao chão. Ele vem pelo buraco: para na boca,
	 * de quatro, e avisa (só o alvo ouve) antes de entrar. Até a alpha23 o buraco era parede para ele, e a
	 * resposta era atravessar um bloco.
	 */
	@GameTest(maxTicks = 1600)
	public void eleEntraPeloVaoDeUmBloco(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -8300);
		macico(level, pes, -5, -16, 5, 5);
		cavar(level, pes, -2, 0, -2, 2, 1, 2);     // a sala do jogador: cinco por cinco, dois de altura
		cavar(level, pes, -2, 0, -13, 2, 2, -4);   // a câmara de fora
		BlockPos buraco = pes.offset(0, 0, -3);
		level.setBlock(buraco, Blocks.AIR.defaultBlockState(), 2);
		ServerPlayer jogador = jogadorJaCacado(helper, pes);
		Anotacoes d = new Anotacoes();
		d.inicio = jogador.position();
		helper.runAfterDelay(10, () -> Diretor.cacadaEmParaTeste(jogador, pes.offset(0, 0, -11), false));
		TestesDePerto.cadaTick(helper, () -> {
			if (jogador.isRemoved()) {
				return;
			}
			level.getChunkSource().move(jogador);
			d.relogio++;
			JogadorDeTeste.recebidos(jogador);
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				d.houve = true;
				d.atravessou |= "ATRAVESSA".equals(h.getEstagioDaCaca());
				d.dentroDeBloco += level.noCollision(h) ? 0 : 1;
				if (h.isDeQuatro()) {
					d.deQuatroDesde = d.deQuatroDesde < 0 ? d.relogio : d.deQuatroDesde;
					d.alturaDeQuatro = Math.min(d.alturaDeQuatro, h.getBbHeight());
				}
				if (h.blockPosition().equals(buraco) && d.noVaoDesde < 0) {
					d.noVaoDesde = d.relogio;
					d.deQuatroNoVao = h.isDeQuatro();
					d.sonsAoEntrar = sonsDoVao(jogador, buraco);
				}
			}
			if (d.houve && jogador.position().distanceTo(d.inicio) > 6) {
				Sussurros.LOGGER.info("[vão] buraco: de quatro no tick {}, no vão no tick {}, sons na boca {}, dentro de bloco {} ticks",
						d.deQuatroDesde, d.noVaoDesde, d.sonsAoEntrar, d.dentroDeBloco);
				helper.assertFalse(d.atravessou, "com o buraco aberto ele entra por ele, não atravessando um bloco");
				helper.assertTrue(d.noVaoDesde >= 0 && d.deQuatroNoVao, "ele deveria ter passado pelo buraco, de quatro");
				helper.assertTrue(d.alturaDeQuatro < 1.0F, "de quatro ele tem menos de um bloco de altura: " + d.alturaDeQuatro);
				helper.assertTrue(d.noVaoDesde - d.deQuatroDesde >= 28,
						"antes de entrar ele para na boca do vão por um segundo e meio ou mais; parou " + (d.noVaoDesde - d.deQuatroDesde) + " ticks");
				helper.assertTrue(d.sonsAoEntrar >= 2, "o alvo deveria ter ouvido o aviso na boca do vão; ouviu " + d.sonsAoEntrar
						+ " de " + JogadorDeTeste.ultimosSons(jogador));
				helper.assertTrue(d.dentroDeBloco == 0, "ele não pode ficar dentro de um bloco: " + d.dentroDeBloco + " ticks");
				helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F && jogador.getHealth() < 20.0F, "pego, e vivo");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * A mesma sala, com uma janela de um bloco na altura dos olhos, e o jogador olhando para ele por ela o tempo
	 * todo. Encarar segura, mas não fecha uma passagem aberta: o piscar o traz pelo caminho, e a pose que o jogador
	 * vê ao abrir os olhos é ele de quatro dentro da janela. (Sem isto, olhar por um buraco o deixava parado do
	 * lado de fora até a caçada acabar.)
	 */
	@GameTest(maxTicks = 2600)
	public void encararPelaJanelaDeUmBlocoNaoVence(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -8400);
		macico(level, pes, -5, -16, 5, 5);
		cavar(level, pes, -2, 0, -2, 2, 1, 2);
		cavar(level, pes, -2, 0, -13, 2, 2, -4);
		BlockPos janela = pes.offset(0, 1, -3);
		level.setBlock(janela, Blocks.AIR.defaultBlockState(), 2);
		ServerPlayer jogador = jogadorJaCacado(helper, pes);
		Anotacoes d = new Anotacoes();
		d.inicio = jogador.position();
		boolean[] vistoDeFora = new boolean[1];
		helper.runAfterDelay(10, () -> Diretor.cacadaEmParaTeste(jogador, pes.offset(0, 0, -11), false));
		TestesDePerto.cadaTick(helper, () -> {
			if (jogador.isRemoved()) {
				return;
			}
			level.getChunkSource().move(jogador);
			d.relogio++;
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				d.houve = true;
				jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
				d.atravessou |= "ATRAVESSA".equals(h.getEstagioDaCaca());
				d.dentroDeBloco += level.noCollision(h) ? 0 : 1;
				vistoDeFora[0] |= h.getZ() < janela.getZ() && Diretor.estaVendo(jogador, h, 0.9);
				if (h.blockPosition().equals(janela)) {
					d.noVaoDesde = d.noVaoDesde < 0 ? d.relogio : d.noVaoDesde;
					d.deQuatroNoVao |= h.isDeQuatro();
				}
			}
			if (d.houve && jogador.position().distanceTo(d.inicio) > 6) {
				Sussurros.LOGGER.info("[vão] janela: na janela no tick {}, dentro de bloco {} ticks", d.noVaoDesde, d.dentroDeBloco);
				helper.assertTrue(vistoDeFora[0], "o teste não vale se o jogador não o viu pela janela");
				helper.assertFalse(d.atravessou, "com a janela aberta ele entra por ela, não atravessando um bloco");
				helper.assertTrue(d.noVaoDesde >= 0 && d.deQuatroNoVao, "encarado, ele deveria ter vindo pela janela, de quatro");
				helper.assertTrue(d.dentroDeBloco == 0, "ele não pode ficar dentro de um bloco: " + d.dentroDeBloco + " ticks");
				helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F && jogador.getHealth() < 20.0F, "pego, e vivo");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * Duas câmaras ligadas por um túnel de um bloco, com o jogador longe da saída. Ele entra de quatro, sai do
	 * outro lado e se levanta. Longe do jogador o vão é só caminho: não há aviso na boca.
	 */
	@GameTest(maxTicks = 1600)
	public void depoisDoVaoEleSeLevanta(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -8500);
		macico(level, pes, -5, -30, 5, 5);
		cavar(level, pes, -2, 0, -12, 2, 2, 2);    // a câmara do jogador
		cavar(level, pes, 0, 0, -15, 0, 0, -13);   // o túnel: um por um, três de comprimento
		cavar(level, pes, -2, 0, -26, 2, 2, -16);  // a câmara de onde ele vem
		BlockPos boca = pes.offset(0, 0, -15);
		ServerPlayer jogador = jogadorJaCacado(helper, pes);
		Anotacoes d = new Anotacoes();
		d.inicio = jogador.position();
		helper.runAfterDelay(10, () -> Diretor.cacadaEmParaTeste(jogador, pes.offset(0, 0, -24), false));
		TestesDePerto.cadaTick(helper, () -> {
			if (jogador.isRemoved()) {
				return;
			}
			level.getChunkSource().move(jogador);
			d.relogio++;
			JogadorDeTeste.recebidos(jogador);
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				d.houve = true;
				d.atravessou |= "ATRAVESSA".equals(h.getEstagioDaCaca());
				d.dentroDeBloco += level.noCollision(h) ? 0 : 1;
				int z = h.blockPosition().getZ() - pes.getZ();
				if (z >= -15 && z <= -13) {
					if (d.noVaoDesde < 0) {
						d.noVaoDesde = d.relogio;
						d.sonsAoEntrar = sonsDoVao(jogador, boca);
					}
					d.deQuatroNoVao |= h.isDeQuatro();
					d.alturaDeQuatro = Math.min(d.alturaDeQuatro, h.getBbHeight());
				}
				d.dePeDepois |= d.noVaoDesde >= 0 && z >= -11 && !h.isDeQuatro() && h.getBbHeight() > 1.8F;
			}
			if (d.houve && jogador.position().distanceTo(d.inicio) > 6) {
				Sussurros.LOGGER.info("[vão] túnel: no túnel no tick {}, altura {}, de pé depois {}, dentro de bloco {} ticks",
						d.noVaoDesde, d.alturaDeQuatro, d.dePeDepois, d.dentroDeBloco);
				helper.assertFalse(d.atravessou, "com o túnel aberto ele vem por ele");
				helper.assertTrue(d.noVaoDesde >= 0 && d.deQuatroNoVao && d.alturaDeQuatro < 1.0F,
						"no túnel de um bloco ele vai de quatro: altura " + d.alturaDeQuatro);
				helper.assertTrue(d.dePeDepois, "do outro lado do túnel ele deveria ter se levantado");
				helper.assertTrue(d.sonsAoEntrar == 0, "longe do jogador não há aviso na boca do vão; houve " + d.sonsAoEntrar);
				helper.assertTrue(d.dentroDeBloco == 0, "ele não pode ficar dentro de um bloco: " + d.dentroDeBloco + " ticks");
				helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F && jogador.getHealth() < 20.0F, "pego, e vivo");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * Um túnel de um bloco, comprido, que começa longe e desemboca na sala do jogador. Ele entra de quatro lá longe,
	 * sem aviso (ali é só caminho), e avisa quando o trecho que falta entra nos oito blocos do jogador: quem chega
	 * já de quatro não chega calado.
	 *
	 * Ele nasce a vinte blocos, e não mais longe, de propósito: a mais de dezesseis e três segundos sem ser visto,
	 * vale o atalho da caçada, que o tira do túnel e o põe do lado de fora do maciço deste teste.
	 */
	@GameTest(maxTicks = 1800)
	public void peloTunelCompridoEleAvisaAoChegarPerto(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -7900);
		macico(level, pes, -5, -28, 5, 5);
		cavar(level, pes, -2, 0, -3, 2, 1, 2);     // a sala do jogador
		cavar(level, pes, 0, 0, -15, 0, 0, -4);    // o túnel: um por um, doze de comprimento
		cavar(level, pes, -2, 0, -25, 2, 2, -16);  // a câmara de onde ele vem
		Vec3 meioDoFim = Vec3.atCenterOf(pes.offset(0, 0, -8));
		ServerPlayer jogador = jogadorJaCacado(helper, pes);
		Anotacoes d = new Anotacoes();
		d.inicio = jogador.position();
		int[] sonsAoEntrarLonge = {-1};
		int[] sonsPerto = {0};
		int[] paradoPerto = {0};
		double[] zAntes = {Double.NaN};
		helper.runAfterDelay(10, () -> Diretor.cacadaEmParaTeste(jogador, pes.offset(0, 0, -20), false));
		TestesDePerto.cadaTick(helper, () -> {
			if (jogador.isRemoved()) {
				return;
			}
			level.getChunkSource().move(jogador);
			JogadorDeTeste.recebidos(jogador);
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				d.houve = true;
				d.atravessou |= "ATRAVESSA".equals(h.getEstagioDaCaca());
				d.dentroDeBloco += level.noCollision(h) ? 0 : 1;
				double z = h.getZ() - pes.getZ();
				if (z > -15 && z < -3) {
					d.deQuatroNoVao |= h.isDeQuatro();
					if (sonsAoEntrarLonge[0] < 0) {
						sonsAoEntrarLonge[0] = JogadorDeTeste.sonsVindosDe(jogador, meioDoFim, 6.0, "sussurros:arrasto")
								+ JogadorDeTeste.sonsVindosDe(jogador, meioDoFim, 6.0, "sussurros:arranhar");
					}
					// Dentro do trecho perto do jogador: quantos ticks ele ficou sem sair do lugar (a espera do aviso).
					if (z > -12 && !Double.isNaN(zAntes[0]) && Math.abs(z - zAntes[0]) < 0.01) {
						paradoPerto[0]++;
					}
					sonsPerto[0] = Math.max(sonsPerto[0], JogadorDeTeste.sonsVindosDe(jogador, meioDoFim, 6.0, "sussurros:arrasto")
							+ JogadorDeTeste.sonsVindosDe(jogador, meioDoFim, 6.0, "sussurros:arranhar"));
				}
				zAntes[0] = z;
			}
			if (d.houve && jogador.position().distanceTo(d.inicio) > 6) {
				Sussurros.LOGGER.info("[vão] túnel comprido: sons ao entrar longe {}, sons perto {}, parado perto {} ticks, dentro de bloco {} ticks",
						sonsAoEntrarLonge[0], sonsPerto[0], paradoPerto[0], d.dentroDeBloco);
				helper.assertFalse(d.atravessou, "com o túnel aberto ele vem por ele");
				helper.assertTrue(d.deQuatroNoVao, "no túnel ele vai de quatro");
				helper.assertTrue(sonsAoEntrarLonge[0] == 0, "ao entrar no túnel, a quinze blocos do jogador, não há aviso; houve " + sonsAoEntrarLonge[0]);
				helper.assertTrue(sonsPerto[0] >= 2, "ao chegar a oito blocos do jogador ele avisa; o alvo ouviu " + sonsPerto[0]
						+ " de " + JogadorDeTeste.ultimosSons(jogador));
				helper.assertTrue(paradoPerto[0] >= 25, "e para ali por um segundo e meio ou mais; parou " + paradoPerto[0] + " ticks");
				helper.assertTrue(d.dentroDeBloco == 0, "ele não pode ficar dentro de um bloco: " + d.dentroDeBloco + " ticks");
				helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F && jogador.getHealth() < 20.0F, "pego, e vivo");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * A galeria em que o dono foi caçado em 11/10/2026, com ele nascendo onde nasceu (a boca de um túnel, no alto).
	 * Naquela caçada a busca disse CAMINHO_FALHOU três vezes. Com o jogador parado em três lugares da galeria (o
	 * fundo, o outro lado e o corredor de baixo), ele chega aos três sem a navegação desistir nenhuma vez e sem
	 * nunca ficar dentro de um bloco.
	 *
	 * Não é a prova de que a falha de 11/10 acabou: com o jogador parado ela não acontecia nem antes (ver o
	 * DESIGN-SPOILERS). É a guarda de que o corpo novo e o caminho medido de quatro não estragam o que funcionava,
	 * num terreno de verdade, cheio de degraus e de frestas.
	 */
	@GameTest(maxTicks = 9000)
	public void naGaleriaDeVerdadeACacadaChega(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = JogadorDeTeste.chao(helper, 4, -8200);
		Terreno.Montado g = Terreno.montar(level, "galeria", base.offset(-23, 0, -25));
		BlockPos nascer = g.em(20, 10, 9);
		BlockPos[] lugares = {g.em(2, 0, 22), g.em(39, 7, 23), g.em(34, 2, 42)};
		int[] vez = {-1};
		ServerPlayer[] jogador = new ServerPlayer[1];
		Anotacoes[] d = new Anotacoes[1];
		int[] desistiu = new int[1];
		String[] buscaAntes = {""};
		TestesDePerto.cadaTick(helper, () -> {
			if (vez[0] >= lugares.length) {
				return;
			}
			if (jogador[0] == null) {
				if (++vez[0] >= lugares.length) {
					helper.succeed();
					return;
				}
				jogador[0] = TestesDePerto.jogadorNoTunel(helper, lugares[vez[0]]);
				Diretor.definirFase(jogador[0], 4);
				// De costas para onde ele nasce, e parado.
				Vec3 olhos = jogador[0].getEyePosition();
				Vec3 contra = olhos.add(olhos.subtract(Vec3.atBottomCenterOf(nascer)).multiply(1, 0, 1).normalize().scale(8));
				jogador[0].lookAt(EntityAnchorArgument.Anchor.EYES, contra);
				d[0] = new Anotacoes();
				d[0].inicio = jogador[0].position();
				desistiu[0] = 0;
				buscaAntes[0] = "";
				return;
			}
			level.getChunkSource().move(jogador[0]);
			if (++d[0].relogio == 10) {
				Diretor.cacadaEmParaTeste(jogador[0], nascer, true);
				return;
			}
			HospedeEntity h = Diretor.criatura(jogador[0]);
			if (h != null) {
				d[0].houve = true;
				d[0].dentroDeBloco += level.noCollision(h) ? 0 : 1;
				d[0].deQuatroNoVao |= h.isDeQuatro();
				String busca = h.getEstadoBusca();
				if (!busca.equals(buscaAntes[0]) && busca.equals("INVESTIGANDO")) {
					desistiu[0]++;
				}
				buscaAntes[0] = busca;
			}
			boolean pego = d[0].houve && jogador[0].position().distanceTo(d[0].inicio) > 6;
			if (pego || d[0].relogio > 2700) {
				String onde = lugares[vez[0]].subtract(g.canto()).toShortString();
				Sussurros.LOGGER.info("[vão] galeria, lugar {} ({}): {} em {}s, desistências={}, de quatro={}, dentro de bloco={} ticks",
						vez[0], onde, pego ? "pego" : "não pego", d[0].relogio / 20, desistiu[0], d[0].deQuatroNoVao, d[0].dentroDeBloco);
				helper.assertTrue(pego, "na galeria do dono, com o jogador parado em " + onde + ", ele deveria ter chegado");
				// No segundo lugar (uma quina de pedra entre os dois, a dois passos do jogador) ele "chegava" sem ver
				// ninguém e saía procurando: 24 s ou 70 s, conforme a sorte. Agora anda o caminho até o fim.
				helper.assertTrue(d[0].relogio < 20 * 45, "deveria ter chegado em menos de 45 s em " + onde + "; levou " + d[0].relogio / 20 + " s");
				helper.assertTrue(desistiu[0] == 0, "a navegação não deveria ter desistido (CAMINHO_FALHOU) em " + onde + ": " + desistiu[0]);
				helper.assertTrue(d[0].dentroDeBloco == 0, "ele não pode ficar dentro de um bloco: " + d[0].dentroDeBloco + " ticks em " + onde);
				if (h != null) {
					h.sumir(level, false, "FIM_DO_TESTE");
				}
				JogadorDeTeste.remover(jogador[0]);
				jogador[0] = null;
			}
		});
	}

	/**
	 * Debaixo de um teto de um bloco de espessura, três blocos acima do chão (uma casa, uma caverna rasa), olhando
	 * para ele o tempo todo. O piscar o traz, como em campo aberto. Até a alpha23 o avanço do piscar procurava o
	 * chão de cima para baixo, achava o lado de cima do teto e desistia: quem o encarava debaixo de um teto fino o
	 * segurava até a caçada acabar.
	 */
	@GameTest(maxTicks = 2600)
	public void encararDebaixoDeTetoNaoVence(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pes = JogadorDeTeste.chao(helper, 4, -8600);
		// Um piso e um teto de pedra, e nada entre os dois: o mundo de teste não é plano.
		for (BlockPos pos : BlockPos.betweenClosed(pes.offset(-8, -1, -22), pes.offset(8, 3, 8))) {
			int y = pos.getY() - pes.getY();
			level.setBlock(pos, (y == -1 || y == 3 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
		}
		ServerPlayer jogador = jogadorJaCacado(helper, pes);
		Anotacoes d = new Anotacoes();
		d.inicio = jogador.position();
		double[] distancias = {-1, -1};
		helper.runAfterDelay(10, () -> Diretor.cacadaEmParaTeste(jogador, pes.offset(0, 0, -16), false));
		TestesDePerto.cadaTick(helper, () -> {
			if (jogador.isRemoved()) {
				return;
			}
			level.getChunkSource().move(jogador);
			HospedeEntity h = Diretor.criatura(jogador);
			if (h != null) {
				d.houve = true;
				jogador.lookAt(EntityAnchorArgument.Anchor.EYES, h.getEyePosition());
				d.atravessou |= "ATRAVESSA".equals(h.getEstagioDaCaca());
				double dist = h.distanceTo(jogador);
				distancias[0] = distancias[0] < 0 ? dist : distancias[0];
				distancias[1] = distancias[1] < 0 ? dist : Math.min(distancias[1], dist);
			}
			if (d.houve && jogador.position().distanceTo(d.inicio) > 6) {
				helper.assertFalse(d.atravessou, "debaixo de um teto há caminho: ele não precisa atravessar nada");
				helper.assertTrue(distancias[1] < distancias[0] - 6, "encarado debaixo do teto, ele deveria ter avançado a cada piscar: de "
						+ distancias[0] + " a " + distancias[1]);
				helper.assertTrue(jogador.isAlive() && jogador.getHealth() >= 4.0F && jogador.getHealth() < 20.0F, "pego, e vivo");
				JogadorDeTeste.remover(jogador);
				helper.succeed();
			}
		});
	}

	/**
	 * As páginas da caçada chegam depois dela, pelo que o jogador já viveu: a dos dois verbos dele depois da
	 * primeira caçada (também para quem foi caçado antes desta versão), e a do vão depois da primeira vez em que
	 * ele avisou numa boca e entrou. Quem nunca foi caçado não recebe nenhuma.
	 */
	@GameTest(maxTicks = 2200)
	public void asPaginasDaCacadaChegamDepois(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer novato = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -8700, 0);
		ServerPlayer cacado = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -8800, 0);
		Diretor.esquecer(novato);
		Diretor.esquecer(cacado);
		Memoria m0 = Memoria.de(cacado);
		m0.set(Memoria.CACADAS, 1);
		m0.set(Memoria.CACA_VAOS, 1);
		m0.salvar();
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(Memoria.de(novato).get("paginas_pedidas") == 0, "quem nunca foi caçado não tem página da caçada pedida");
			int pedidas = Memoria.de(cacado).get("paginas_pedidas");
			helper.assertTrue((pedidas >> 22 & 1) == 1 && (pedidas >> 28 & 1) == 1,
					"depois da caçada e do vão, as páginas 23 e 29 deveriam ter sido pedidas: " + Integer.toBinaryString(pedidas));
		});
		int[] lidas = new int[1];
		TestesDePerto.cadaTick(helper, () -> {
			if (cacado.isRemoved()) {
				return;
			}
			level.getChunkSource().move(cacado);
			// Lê cada página deixada assim que ela aparece: a seguinte só vem depois de a anterior ser lida.
			for (ItemEntity pagina : level.getEntitiesOfClass(ItemEntity.class, cacado.getBoundingBox().inflate(10.0),
					item -> item.getItem().is(ModItems.PAGINA_RASGADA))) {
				pagina.discard();
				helper.assertTrue(Diario.lerProxima(cacado), "a página deixada deveria ter o que ler");
				lidas[0]++;
				int mascara = Memoria.de(cacado).get("paginas_mascara");
				helper.assertTrue(mascara == (lidas[0] == 1 ? 1 << 22 : 1 << 22 | 1 << 28),
						"a primeira lida é a 23 (os dois verbos), a segunda a 29 (o vão); máscara " + Integer.toBinaryString(mascara));
			}
			if (lidas[0] == 2) {
				JogadorDeTeste.remover(novato);
				JogadorDeTeste.remover(cacado);
				helper.succeed();
			}
		});
	}
}
