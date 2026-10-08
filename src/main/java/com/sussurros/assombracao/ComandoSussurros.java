package com.sussurros.assombracao;

import java.util.Map;
import java.util.TreeMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Comando de TESTE (precisa de cheats ligados):
 *   /sussurros fase <0-4>        pula para uma fase (subindo, entrega os itens das fases puladas)
 *   /sussurros evento <nome>     força um evento agora (não conta para o aprendizado)
 *   /sussurros obsessao <0-100>  ajusta a obsessão (testar a sequência de ameaça sem esperar)
 *   /sussurros cena casa         começa a cena "Ele voltou com você" (teste: não aprende)
 *   /sussurros cena tunel        começa a cena "Algo no túnel" (teste: não aprende)
 *   /sussurros cena campo        começa a cena "Na linha das árvores" (teste: não aprende)
 *   /sussurros cena marco        começa a cena "Foi aqui" (teste: não aprende)
 *   /sussurros cena janela       começa a cena "Do outro lado do vidro" (teste: não aprende)
 *   /sussurros cena eco          toca um eco de ação do lugar onde você fez / do rastro (teste: não aprende)
 *   /sussurros cena parar        interrompe a cena em andamento (inclusive o silêncio do fim) e tira o Hóspede
 *   /sussurros teste sussurro estereo|cabeca|folego|lado   experimento: som sem direção (ver ROTEIRO-DE-TESTE.md)
 *   /sussurros teste sussurro tudo                         os três sons em sequência, numerados no chat
 *   /sussurros teste miragem [apagar|vermelha]             experimento: bloco que só você vê
 *   /sussurros cena luzfim                                 a luz no fim do túnel (miragem)
 *   /sussurros teste sosia <jogador> [agachado|deitado]    experimento: manequim com a pele de um jogador
 *   /sussurros memoria           mostra o que o mod lembra sobre você (SPOILER)
 *   /sussurros esquecer          apaga tudo e recomeça do zero
 *   /sussurros debug [on|off]    grava as decisões do Diretor em sussurros-debug.log (SPOILER)
 */
public final class ComandoSussurros {
	private ComandoSussurros() {
	}

	public static void inicializar() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("sussurros")
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

			raiz.then(Commands.literal("fase")
					.then(Commands.argument("numero", IntegerArgumentType.integer(0, 4))
							.executes(ctx -> {
								ServerPlayer p = ctx.getSource().getPlayerOrException();
								int fase = IntegerArgumentType.getInteger(ctx, "numero");
								String msg = Diretor.definirFase(p, fase);
								ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
								return 1;
							})));

			raiz.then(Commands.literal("obsessao")
					.then(Commands.argument("valor", IntegerArgumentType.integer(0, 100))
							.executes(ctx -> {
								ServerPlayer p = ctx.getSource().getPlayerOrException();
								String msg = Diretor.definirObsessao(p, IntegerArgumentType.getInteger(ctx, "valor"));
								ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
								return 1;
							})));

			raiz.then(Commands.literal("cena")
					.then(Commands.literal("parar").executes(ctx -> {
						String msg = Diretor.pararCenas(ctx.getSource().getPlayerOrException());
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("casa").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarCenaCasa(p);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("tunel").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarCenaTunel(p);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("campo").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarCenaCampo(p);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("marco").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarCenaMarco(p);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("janela").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarCenaJanela(p);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("eco").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarEcoDeAcao(p);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("animais").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarPerturbacao(p, Perturbacao.TODOS_OLHANDO);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("luzfim").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarPerturbacao(p, Perturbacao.LUZ_NO_FIM);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("luz").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarPerturbacao(p, Perturbacao.PASSOU_PELA_MINA);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("caminho").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarPerturbacao(p, Perturbacao.O_CAMINHO_MUDOU);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("curral").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarPerturbacao(p, Perturbacao.HA_ALGO_NO_CURRAL);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					})));

			raiz.then(Commands.literal("pressagio").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				String msg = Diretor.testarPressagio(p);
				ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
				return 1;
			}));

			raiz.then(Commands.literal("estrutura")
					.then(Commands.literal("marco").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarEstrutura(p, "marco");
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("posto").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarEstrutura(p, "posto");
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("nicho").executes(ctx -> {
						ServerPlayer p = ctx.getSource().getPlayerOrException();
						String msg = Diretor.testarEstrutura(p, "nicho");
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					})));

			LiteralArgumentBuilder<CommandSourceStack> evento = Commands.literal("evento");
			for (Evento ev : Evento.values()) {
				evento.then(Commands.literal(ev.name().toLowerCase()).executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					String falha = Diretor.forcarEvento(p, ev);
					String msg = falha == null
							? "[Sussurros] Evento: " + ev.name() + " (forçado: não conta para o aprendizado)"
							: "[Sussurros] " + ev.name() + " não pôde acontecer: " + falha;
					ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
					return 1;
				}));
			}
			raiz.then(evento);

			raiz.then(Commands.literal("memoria").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				Map<String, Integer> dados = new TreeMap<>(Memoria.de(p).copia());
				ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + dados), false);
				ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] agora: " + Diretor.resumo(p)), false);
				return 1;
			}));

			raiz.then(Commands.literal("debug")
					.executes(ctx -> {
						String estadoLog = Diretor.depuracaoLigada() ? "ligado" : "desligado";
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] Log " + estadoLog
								+ ". Use /sussurros debug on ou /sussurros debug off."), false);
						return 1;
					})
					.then(Commands.literal("on").executes(ctx -> {
						Diretor.alternarDepuracao(true);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] Log ligado: " + Diretor.arquivoDepuracao()), false);
						return 1;
					}))
					.then(Commands.literal("off").executes(ctx -> {
						Diretor.alternarDepuracao(false);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] Log desligado."), false);
						return 1;
					})));

			// Experimentos do PLANO-MECANICAS.md: cada um responde a uma pergunta que só dá para ver jogando.
			LiteralArgumentBuilder<CommandSourceStack> teste = Commands.literal("teste");
			LiteralArgumentBuilder<CommandSourceStack> testeSussurro = Commands.literal("sussurro");
			for (String modo : new String[] {"tudo", "estereo", "cabeca", "folego", "lado"}) {
				testeSussurro.then(Commands.literal(modo).executes(ctx -> {
					String msg = Experimentos.sussurro(ctx.getSource().getPlayerOrException(), modo);
					ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
					return 1;
				}));
			}
			teste.then(testeSussurro);
			teste.then(Commands.literal("miragem")
					.executes(ctx -> {
						String msg = Experimentos.miragem(ctx.getSource().getPlayerOrException(), false);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					})
					.then(Commands.literal("apagar").executes(ctx -> {
						String msg = Experimentos.miragem(ctx.getSource().getPlayerOrException(), true);
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					}))
					.then(Commands.literal("vermelha").executes(ctx -> {
						String msg = Experimentos.miragem(ctx.getSource().getPlayerOrException(), "vermelha");
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					})));
			RequiredArgumentBuilder<CommandSourceStack, String> testeSosia = Commands.argument("jogador", StringArgumentType.word())
					.executes(ctx -> {
						String msg = Experimentos.sosia(ctx.getSource().getPlayerOrException(),
								StringArgumentType.getString(ctx, "jogador"), "standing");
						ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
						return 1;
					});
			for (String[] pose : new String[][] {{"agachado", "crouching"}, {"deitado", "sleeping"}}) {
				testeSosia.then(Commands.literal(pose[0]).executes(ctx -> {
					String msg = Experimentos.sosia(ctx.getSource().getPlayerOrException(),
							StringArgumentType.getString(ctx, "jogador"), pose[1]);
					ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] " + msg), false);
					return 1;
				}));
			}
			teste.then(Commands.literal("sosia")
					.executes(ctx -> {
						ctx.getSource().sendSuccess(() -> Component.literal(
								"[Sussurros] Falta o nome: /sussurros teste sosia <jogador> [agachado|deitado]. No jogo de teste o seu nome é Jogador."), false);
						return 1;
					})
					.then(testeSosia));
			raiz.then(teste);

			raiz.then(Commands.literal("esquecer").executes(ctx -> {
				ServerPlayer p = ctx.getSource().getPlayerOrException();
				Diretor.esquecer(p);
				ctx.getSource().sendSuccess(() -> Component.literal("[Sussurros] Memória apagada."), false);
				return 1;
			}));

			dispatcher.register(raiz);
		});
	}
}
