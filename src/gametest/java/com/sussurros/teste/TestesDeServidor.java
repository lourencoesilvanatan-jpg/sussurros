package com.sussurros.teste;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Evento;
import com.sussurros.rede.PacoteSentidos;
import com.sussurros.registro.ModEntidades;
import com.sussurros.registro.ModItems;

/**
 * Testes que rodam dentro de um servidor de verdade, sem janela (./gradlew runGameTest).
 *
 * Eles não provam que algo assusta. Provam que o mod carrega, que cada peça roda sem erro com um
 * jogador de mentira e que as regras básicas valem. O que só o olho vê fica para os testes de cliente.
 */
public class TestesDeServidor {
	@GameTest
	public void oModCarregaComTudoRegistrado(GameTestHelper helper) {
		helper.assertTrue(Sussurros.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(ModEntidades.HOSPEDE).getNamespace()),
				"a criatura não foi registrada");
		for (Item item : BuiltInRegistries.ITEM) {
			if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Sussurros.MOD_ID)) {
				helper.assertFalse(new ItemStack(item).isEmpty(), "item vazio: " + BuiltInRegistries.ITEM.getKey(item));
			}
		}
		helper.assertFalse(new ItemStack(ModItems.VELA_PALIDA).isEmpty(), "a vela não existe");
		helper.succeed();
	}

	/** Todo evento forçado por comando tem de rodar sem lançar erro, caiba ou não no lugar do teste. */
	@GameTest(maxTicks = 400)
	public void todoEventoRodaSemErro(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, 4, 2, 4);
		Diretor.definirFase(jogador, 4);
		Evento[] eventos = Evento.values();
		for (int i = 0; i < eventos.length; i++) {
			Evento ev = eventos[i];
			helper.runAfterDelay(2L + i * 10L, () -> Diretor.forcarEvento(jogador, ev));
		}
		helper.runAfterDelay(2L + eventos.length * 10L + 20L, () -> {
			Diretor.pararCenas(jogador);
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}

	/** Na fase 4 o cliente tem de ser mandado sentir peso; na fase 0, nada. */
	@GameTest(maxTicks = 200)
	public void osSentidosAcompanhamAFase(GameTestHelper helper) {
		ServerPlayer jogador = JogadorDeTeste.criar(helper, GameType.SURVIVAL, 4, 2, 4);
		Diretor.esquecer(jogador);
		helper.runAfterDelay(45, () -> {
			PacoteSentidos calmo = Diretor.sentidos(jogador);
			helper.assertTrue(calmo.peso() < 0.05F, "fase 0 deveria ter peso quase zero, veio " + calmo.peso());
			helper.assertTrue(calmo.caca() == 0.0F, "sem caçada, caca deveria ser zero");
			Diretor.definirFase(jogador, 4);
		});
		helper.runAfterDelay(100, () -> {
			PacoteSentidos pesado = Diretor.sentidos(jogador);
			helper.assertTrue(pesado.peso() >= 0.6F, "fase 4 deveria pesar pelo menos 0,6, veio " + pesado.peso());
			JogadorDeTeste.remover(jogador);
			helper.succeed();
		});
	}
}
