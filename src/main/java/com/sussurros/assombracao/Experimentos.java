package com.sussurros.assombracao;

import java.util.List;
import java.util.Locale;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.sussurros.registro.ModSons;

/**
 * Experimentos de `/sussurros teste ...` (ver PLANO-MECANICAS.md).
 *
 * Cada um responde, dentro do jogo, a uma pergunta de que uma etapa inteira do plano depende. Nenhum mexe em
 * memória, pressão, agenda ou aprendizado, e nada aqui muda o mundo de verdade além do sósia, que some sozinho.
 */
final class Experimentos {
	private static final String ETIQUETA_SOSIA = "sussurros_teste";

	private Experimentos() {
	}

	/**
	 * Pergunta: dá para tocar um som "dentro da cabeça", sem direção, sem código no cliente?
	 * São quatro formas para comparar de ouvido.
	 */
	static String sussurro(ServerPlayer p, String modo) {
		Vec3 esquerda = Diretor.pontoRelativo(p, -90, 10);
		return switch (modo) {
			case "estereo" -> {
				// Arquivo estéreo do próprio jogo, enviado como som posicionado. A wiki diz que estéreo ignora a posição.
				p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMBIENT_UNDERWATER_ENTER),
						SoundSource.HOSTILE, esquerda.x, p.getEyeY(), esquerda.z, 1.0F, 1.0F, p.getRandom().nextLong()));
				yield "Som ESTÉREO posicionado 10 blocos à sua esquerda. Se soar no meio, sem lado, o estéreo ignora a posição.";
			}
			case "cabeca" -> {
				ModSons.tocarNaCabeca(p, ModSons.Som.RESPIRACAO, 1.0F, 0.9F);
				yield "Respiração do mod presa a você. Deve soar no meio, sem lado, e continuar no meio se você andar ou girar.";
			}
			case "folego" -> {
				ModSons.tocarNaCabeca(p, SoundEvents.PLAYER_BREATH, 1.0F, 0.8F);
				yield "Respiração do próprio jogo presa a você. Compare com a do mod (teste sussurro cabeca).";
			}
			default -> {
				ModSons.tocarPara(p, esquerda.x, p.getEyeY(), esquerda.z, ModSons.Som.RESPIRACAO, 1.0F, 0.9F);
				yield "Respiração do mod 10 blocos à sua esquerda, só para você. É a referência: esta TEM de ter lado.";
			}
		};
	}

	/**
	 * Pergunta: um bloco enviado só para um jogador aparece, ilumina, e o que acontece ao clicar nele?
	 * apagar=false põe uma tocha falsa à frente; apagar=true esconde a tocha de verdade mais próxima.
	 */
	static String miragem(ServerPlayer p, boolean apagar) {
		ServerLevel level = p.level();
		if (apagar) {
			BlockPos centro = p.blockPosition();
			BlockPos achada = null;
			double melhor = Double.MAX_VALUE;
			for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-8, -3, -8), centro.offset(8, 3, 8))) {
				BlockState s = level.getBlockState(pos);
				if ((s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)) && pos.distSqr(centro) < melhor) {
					melhor = pos.distSqr(centro);
					achada = pos.immutable();
				}
			}
			if (achada == null) {
				return "Não achei uma tocha comum a até 8 blocos. Coloque uma e tente de novo.";
			}
			enviarMiragem(level, p, achada, Blocks.AIR.defaultBlockState(), 20 * 20);
			return "A tocha mais próxima sumiu SÓ PARA VOCÊ por 20 s (no mundo ela continua lá). Veja se a luz some junto e clique no lugar dela.";
		}
		Vec3 frente = Diretor.pontoRelativo(p, 0, 4);
		BlockPos pos = Diretor.acharChao(level, frente.x, p.getY(), frente.z);
		BlockState tocha = Blocks.TORCH.defaultBlockState();
		if (pos == null || !tocha.canSurvive(level, pos)) {
			return "Não achei chão livre 4 blocos à sua frente. Tente num lugar plano.";
		}
		enviarMiragem(level, p, pos, tocha, 20 * 20);
		return "Uma tocha que SÓ VOCÊ vê está 4 blocos à frente por 20 s. Veja se ela ilumina (teste no escuro) e clique nela.";
	}

	private static void enviarMiragem(ServerLevel level, ServerPlayer p, BlockPos pos, BlockState falso, int duracaoTicks) {
		p.connection.send(new ClientboundBlockUpdatePacket(pos, falso));
		// Desfazer é reenviar o estado de verdade. O mundo nunca mudou.
		Diretor.agendar(level, duracaoTicks, () -> {
			if (!p.hasDisconnected()) {
				p.connection.send(new ClientboundBlockUpdatePacket(level, pos));
			}
		});
	}

	/**
	 * Pergunta: a entidade Mannequin do jogo aparece com a pele de um jogador e sem o rótulo embaixo do nome?
	 * Invoca pelo comando do próprio jogo, que já sabe ler o perfil; some sozinho em 30 s.
	 */
	static String sosia(ServerPlayer p, String nome, String pose) {
		ServerLevel level = p.level();
		MinecraftServer server = level.getServer();
		Vec3 frente = Diretor.pontoRelativo(p, 0, 8);
		BlockPos chao = Diretor.acharChao(level, frente.x, p.getY(), frente.z);
		if (chao == null) {
			return "Não achei chão livre 8 blocos à sua frente. Tente num lugar plano.";
		}
		Vec3 lugar = new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
		String comando = String.format(Locale.ROOT,
				"summon minecraft:mannequin %.2f %.2f %.2f {profile:{name:\"%s\"},hide_description:true,immovable:true,pose:\"%s\","
						+ "Rotation:[%.1ff,0.0f],Tags:[\"%s\"]}",
				lugar.x, lugar.y, lugar.z, nome, pose, p.getYRot() + 180.0F, ETIQUETA_SOSIA);
		// Um comando disparado de dentro de outro comando só roda depois que o de fora termina. Por isso o
		// "summon" sai no tick seguinte, fora deste comando: aí ele roda na hora e dá para conferir o resultado.
		Diretor.agendar(level, 1, () -> {
			CommandSourceStack fonte = server.createCommandSourceStack().withLevel(level);
			server.getCommands().performPrefixedCommand(fonte, comando);
			List<Mannequin> criados = level.getEntitiesOfClass(Mannequin.class, AABB.ofSize(lugar, 3, 6, 3),
					m -> m.entityTags().contains(ETIQUETA_SOSIA));
			if (criados.isEmpty()) {
				if (!p.hasDisconnected()) {
					p.sendSystemMessage(Component.literal("[Sussurros] O jogo recusou o comando de invocar o manequim "
							+ "(o motivo fica no log do jogo). Comando usado: /" + comando));
				}
				return;
			}
			Diretor.agendar(level, 20 * 30, () -> criados.forEach(m -> {
				if (!m.isRemoved()) {
					m.discard();
				}
			}));
		});
		return "Sósia de \"" + nome + "\" 8 blocos à frente por 30 s. Confira: a pele é a dele? Aparece algum rótulo embaixo do nome? "
				+ "(Nome de quem não está online pode demorar a carregar a pele, ou nem carregar.)";
	}
}
