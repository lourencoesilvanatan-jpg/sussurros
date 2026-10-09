package com.sussurros.teste;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.assombracao.Baralho;
import com.sussurros.assombracao.Baralho.Carta;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Memoria;
import com.sussurros.bloco.CinzaEspalhadaBlock;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModItems;

/** O baralho: a ordem é do mundo e do jogador, cada carta sai uma vez por ciclo, e as cartas fazem o que dizem. */
public class TestesDoBaralho {
	/** A mesma semente e o mesmo jogador dão sempre a mesma ordem; mudar um dos dois, ou o ciclo, muda a ordem. */
	@GameTest
	public void aOrdemEDoMundoEDoJogador(GameTestHelper helper) {
		UUID ana = UUID.fromString("00000000-0000-0001-0000-000000000001");
		UUID beto = UUID.fromString("00000000-0000-0002-0000-000000000002");
		List<Carta> ordem = Baralho.ordem(1234L, ana, 1);
		helper.assertTrue(ordem.equals(Baralho.ordem(1234L, ana, 1)), "a mesma semente e o mesmo jogador deveriam dar a mesma ordem");
		helper.assertTrue(ordem.size() == 12, "doze cartas, vieram " + ordem.size());
		for (Carta carta : Carta.values()) {
			long vezes = ordem.stream().filter(c -> c == carta).count();
			helper.assertTrue(vezes == (carta == Carta.NADA ? 3 : 1), carta + " aparece " + vezes + " vezes");
		}
		int diferentes = 0;
		for (long semente = 1; semente <= 8; semente++) {
			diferentes += Baralho.ordem(semente, ana, 1).equals(Baralho.ordem(semente + 100, ana, 1)) ? 0 : 1;
			diferentes += Baralho.ordem(semente, ana, 1).equals(Baralho.ordem(semente, beto, 1)) ? 0 : 1;
			diferentes += Baralho.ordem(semente, ana, 1).equals(Baralho.ordem(semente, ana, 2)) ? 0 : 1;
		}
		helper.assertTrue(diferentes >= 22, "outro mundo, outro jogador ou outro ciclo deveriam dar outra ordem (" + diferentes + " de 24)");
		helper.succeed();
	}

	/** Virar anda no baralho na ordem do mundo, cada carta uma vez, e as cartas deixam o que dizem. */
	@GameTest(maxTicks = 200)
	public void virarAndaNaOrdemEAsCartasFazemOQueDizem(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2200, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 4);
		ServerLevel level = helper.getLevel();
		helper.runAfterDelay(5, () -> {
			List<Carta> esperada = Baralho.ordem(level.getSeed(), jogador.getUUID(), 1);
			List<Carta> viradas = new ArrayList<>();
			for (int i = 0; i < 60 && Memoria.de(jogador).get("baralho_ciclo") < 2; i++) {
				String feito = Baralho.virarParaTeste(jogador);
				if (!feito.endsWith("adiada")) {
					viradas.add(Carta.valueOf(feito.substring(0, feito.indexOf(' '))));
				}
			}
			helper.assertTrue(viradas.equals(esperada), "as cartas deveriam sair na ordem do mundo: " + viradas + " em vez de " + esperada);
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("baralho_ciclo") == 2 && m.get("baralho_pos") == 0, "esgotado, o baralho recomeça em outro ciclo");
			helper.assertTrue(m.get("baralho_falta") >= 2400 && m.get("baralho_falta") <= 5400, "a próxima carta fica para daqui a 40 a 90 minutos: " + m.get("baralho_falta"));
			helper.assertTrue(m.get("caca_devida") == 1, "a carta da caçada deveria deixá-la devendo");
			helper.assertTrue(m.get("soleira") == 1, "a carta da soleira deveria erguê-la");
			helper.assertTrue(m.get("casa_vigia") == 1, "a carta da casa deveria erguê-la");
			helper.assertTrue(m.get("baralho_presente") == 1 && m.get("baralho_linhas") == 1, "duas cartas ficam marcadas para a manhã seguinte");
			helper.assertTrue(m.get("avesso_marcado") == 0, "sem a dimensão, a carta do sono não marca ninguém");
			// A caçada devida não é para acontecer dentro deste teste.
			m.set("caca_devida", 0);
			m.salvar();
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** As duas cartas da manhã seguinte: a linha amanhece rompida e há algo ao lado da cama. */
	@GameTest(maxTicks = 100)
	public void asCartasDaManhaSeguinte(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criarNoChao(helper, GameType.SURVIVAL, 4, -2100, 0);
		Diretor.esquecer(jogador);
		Diretor.definirFase(jogador, 2);
		ServerLevel level = helper.getLevel();
		BlockPos linha = jogador.blockPosition().offset(3, 0, 0);
		level.setBlockAndUpdate(linha, ModBlocos.CINZA_ESPALHADA.defaultBlockState());
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(Baralho.aplicarParaTeste(jogador, Carta.LINHAS).startsWith("LINHAS"), "carta das linhas");
			helper.assertTrue(Baralho.aplicarParaTeste(jogador, Carta.PRESENTE).startsWith("PRESENTE"), "carta do presente");
			helper.assertTrue(Memoria.de(jogador).get("baralho_pos") == 0, "carta aplicada por teste não anda no baralho");
			Diretor.acordarParaTeste(jogador);
			helper.assertTrue(CinzaEspalhadaBlock.estadoEm(level, linha) == CinzaEspalhadaBlock.Estado.ROMPIDA,
					"a linha deveria amanhecer rompida, está " + CinzaEspalhadaBlock.estadoEm(level, linha));
			List<ItemEntity> itens = level.getEntitiesOfClass(ItemEntity.class, new AABB(jogador.blockPosition()).inflate(3.0),
					i -> i.getItem().is(ModItems.PAGINA_RASGADA));
			helper.assertTrue(itens.size() == 1, "deveria haver uma página ao lado da cama, há " + itens.size());
			Memoria m = Memoria.de(jogador);
			helper.assertTrue(m.get("baralho_presente") == 0 && m.get("baralho_linhas") == 0, "as marcas são gastas ao acordar");
			itens.forEach(ItemEntity::discard);
			for (BlockPos pos : BlockPos.betweenClosed(linha.offset(-3, 0, -3), linha.offset(3, 0, 3))) {
				if (CinzaEspalhadaBlock.estadoEm(level, pos) != null) {
					level.removeBlock(pos.immutable(), false);
				}
			}
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
