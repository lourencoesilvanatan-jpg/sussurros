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
		BlockState tocha = Blocks.TORCH.defaultBlockState();
		BlockPos pos = chaoAFrente(level, p, 4, 1);
		if (pos == null || !tocha.canSurvive(level, pos)) {
			return "Não achei chão livre perto, à sua frente. Vire para um lado com chão à vista e tente de novo.";
		}
		enviarMiragem(level, p, pos, tocha, 20 * 20);
		return "Uma tocha que SÓ VOCÊ vê está logo à frente por 20 s. Veja se ela ilumina (teste no escuro) e clique nela.";
	}

	/**
	 * Chão firme com "altura" blocos de ar em cima, o mais perto possível de "dist" blocos à frente do jogador.
	 * Tenta algumas distâncias e ângulos: dentro de casa ou em terreno irregular o ponto exato quase nunca serve.
	 */
	private static BlockPos chaoAFrente(ServerLevel level, ServerPlayer p, double dist, int altura) {
		double[] distancias = {dist, dist - 1, dist + 1, dist - 2, dist + 2};
		double[] angulos = {0, 15, -15, 30, -30};
		for (double d : distancias) {
			if (d < 2) {
				continue;
			}
			for (double a : angulos) {
				Vec3 v = Diretor.pontoRelativo(p, a, d);
				for (int dy = 3; dy >= -4; dy--) {
					BlockPos pos = BlockPos.containing(v.x, p.getY() + dy, v.z);
					BlockPos baixo = pos.below();
					if (level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty()) {
						continue;
					}
					boolean livre = true;
					for (int h = 0; h < altura && livre; h++) {
						livre = level.getBlockState(pos.above(h)).isAir();
					}
					if (livre) {
						return pos;
					}
				}
			}
		}
		return null;
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
		BlockPos chao = chaoAFrente(level, p, 7, 2);
		if (chao == null) {
			return "Não achei chão livre perto, à sua frente. Vire para um lado com chão à vista e tente de novo.";
		}
		Vec3 lugar = new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
		String comando = String.format(Locale.ROOT,
				"summon minecraft:mannequin %.2f %.2f %.2f {profile:{name:\"%s\"},hide_description:true,immovable:true,pose:\"%s\","
						+ "Rotation:[%.1ff,0.0f],Tags:[\"%s\"]}",
				lugar.x, lugar.y, lugar.z, nome, pose, p.getYRot() + 180.0F, ETIQUETA_SOSIA);
		// Um comando disparado de dentro de outro comando só roda depois que o de fora termina. Por isso o
		// "summon" sai no tick seguinte, fora deste comando: aí ele roda na hora e dá para conferir o resultado.
		Diretor.agendar(level, 1, () -> {
			// Sem saída: senão o chat mostra "[Server: Summoned new Mannequin]", que entregaria o truque.
			CommandSourceStack fonte = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
			server.getCommands().performPrefixedCommand(fonte, comando);
			List<Mannequin> criados = level.getEntitiesOfClass(Mannequin.class, AABB.ofSize(lugar, 3, 6, 3),
					m -> m.entityTags().contains(ETIQUETA_SOSIA));
			if (criados.isEmpty()) {
				if (!p.hasDisconnected()) {
					p.sendSystemMessage(Component.literal("[Sussurros] O jogo recusou o comando de invocar o manequim. "
							+ "Para ver o motivo, rode você mesmo: /" + comando));
				}
				return;
			}
			Diretor.agendar(level, 20 * 30, () -> criados.forEach(m -> {
				if (!m.isRemoved()) {
					m.discard();
				}
			}));
		});
		return "Sósia de \"" + nome + "\" logo à frente por 30 s. Confira: a pele é a dele? Aparece algum rótulo embaixo do nome? "
				+ "(Nome de quem não está online pode demorar a carregar a pele, ou nem carregar.)";
	}
}
