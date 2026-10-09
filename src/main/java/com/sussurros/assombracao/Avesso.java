package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteEfeito;
import com.sussurros.rede.PacoteSentidos;
import com.sussurros.rede.Rede;

/**
 * O Avesso (0.9): o mesmo lugar, do outro lado.
 *
 * É uma dimensão com o mesmo terreno do mundo normal (a mesma semente, o mesmo ruído), mas com um bioma só,
 * morto: os mesmos morros, rios e cavernas nas mesmas coordenadas, sem árvore, sem bicho, sem minério, com um
 * céu parado. O jogador nunca escolhe ir. Ele é levado, fica pouco, e volta para o ponto de onde saiu.
 *
 * A CÓPIA. Na chegada, a região em volta dele (29 x 16 x 29 blocos) é copiada do mundo normal para as mesmas
 * coordenadas: é a casa dele, vazia e apagada. Luzes somem, portas ficam abertas, baús vêm vazios, nada de
 * entidade é copiado. A cada visita a cópia tem mais coisa errada: blocos de parede faltando.
 *
 * LÁ DENTRO não se quebra nem se põe bloco e não se abre baú (senão a cópia viraria fábrica de itens). Portas
 * funcionam. A primeira visita é só o lugar. Da segunda em diante, depois de uns vinte segundos, ele está lá.
 *
 * SAIR: o tempo acaba (de 45 s a 2,5 min), ele se afasta mais de 40 blocos, deita na cópia da própria cama,
 * ou leva um dano que mataria. Em todos os casos é "acordar": apagão, e ele está de volta onde estava, com
 * tudo o que tinha. Por alguns minutos, uma ou duas luzes da casa de verdade aparecem apagadas.
 *
 * O Diretor normal só cuida do mundo normal; aqui quem conduz é esta classe.
 */
public final class Avesso {
	public static final ResourceKey<Level> DIMENSAO = ResourceKey.create(Registries.DIMENSION, Sussurros.id("avesso"));

	static final String DENTRO = "avesso_dentro";     // 1 enquanto ele está lá; sobrevive a fechar o jogo
	static final String VOLTA_X = "avesso_volta_x";
	static final String VOLTA_Y = "avesso_volta_y";
	static final String VOLTA_Z = "avesso_volta_z";
	static final String VISITAS = "avesso_visitas";
	static final String ULTIMO_DIA = "avesso_dia";

	static final int RAIO = 14;
	static final int ABAIXO = 5;
	static final int ACIMA = 10;
	private static final int LONGE_DEMAIS = 40;

	private Avesso() {
	}

	public static void inicializar() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.dimension() == DIMENSAO && level.getGameTime() % 20 == 0) {
				for (ServerPlayer p : List.copyOf(level.players())) {
					segundo(level, p);
				}
			}
		});
		// Nada se quebra, nada se põe, nenhum baú se abre: a cópia não pode virar fonte de itens.
		PlayerBlockBreakEvents.BEFORE.register((level, jogador, pos, estado, entidadeDeBloco) ->
				level.dimension() != DIMENSAO || jogador.isCreative());
		UseBlockCallback.EVENT.register((jogador, level, mao, acerto) -> {
			if (level.dimension() != DIMENSAO || jogador.isCreative() || jogador.isSpectator()) {
				return InteractionResult.PASS;
			}
			Block bloco = level.getBlockState(acerto.getBlockPos()).getBlock();
			if (bloco instanceof BedBlock) {
				if (jogador instanceof ServerPlayer p) {
					voltar(p, "CAMA");
				}
				return InteractionResult.SUCCESS;
			}
			return bloco instanceof DoorBlock || bloco instanceof TrapDoorBlock || bloco instanceof FenceGateBlock
					? InteractionResult.PASS : InteractionResult.FAIL;
		});
		// Morrer lá é acordar.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entidade, fonte, dano) -> {
			if (entidade instanceof ServerPlayer p && p.level().dimension() == DIMENSAO) {
				p.setHealth(4.0F);
				voltar(p, "MORTE");
				return false;
			}
			return true;
		});
	}

	static boolean existe(MinecraftServer server) {
		return server.getLevel(DIMENSAO) != null;
	}

	// =====================================================================
	// Ir
	// =====================================================================

	/** Ele deitou. Raramente, da fase 3 em diante, em vez de dormir ele é levado. */
	static void aoDeitar(ServerPlayer p) {
		ServerLevel mundo = p.level();
		Memoria m = Memoria.de(p);
		if (mundo.dimension() != Level.OVERWORLD || !existe(mundo.getServer()) || !Rede.temCliente(p)
				|| m.get(Memoria.FASE) < 3 || m.get(DENTRO) == 1) {
			return;
		}
		int dia = (int) (mundo.getDefaultClockTime() / 24000L);
		int visitas = m.get(VISITAS);
		// Pelo menos três dias de jogo entre uma visita e outra. A primeira é mais provável que as seguintes.
		if (visitas > 0 && dia - m.get(ULTIMO_DIA) < 3) {
			return;
		}
		float chance = visitas == 0 ? 0.15F : 0.08F;
		if (p.getRandom().nextFloat() >= chance) {
			return;
		}
		// Dois segundos de olhos fechados na cama, e então não é sono.
		Diretor.agendar(mundo, 40, () -> {
			if (!p.isRemoved() && p.isSleeping() && p.level() == mundo) {
				levar(p, "SONO", false);
			}
		});
	}

	/** Leva o jogador agora. Abre e salva a Memoria: chamar de fora do tick do Diretor. */
	static boolean levar(ServerPlayer p, String origem, boolean teste) {
		ServerLevel mundo = p.level();
		ServerLevel avesso = mundo.getServer().getLevel(DIMENSAO);
		Memoria m = Memoria.de(p);
		EstadoJogador e = Diretor.estadoParaTeste(p);
		if (avesso == null || mundo.dimension() != Level.OVERWORLD || m.get(DENTRO) == 1 || e.avessoSaindo) {
			return false;
		}
		if (e.criatura != null && !e.criatura.isRemoved()) {
			e.criatura.sumir(mundo, false, "AVESSO");
		}
		m.set(DENTRO, 1);
		m.salvar();
		Rede.efeito(p, PacoteEfeito.Tipo.APAGAO, 40, 1.0F);
		Diretor.emudecer(mundo, p, 6, "AVESSO");
		Depuracao.log(p, mundo.getGameTime() / 20, "AVESSO levado origem=" + origem + " visita=" + (m.get(VISITAS) + 1)
				+ " teste=" + (teste ? "sim" : "nao"));
		Diretor.agendar(mundo, 16, () -> atravessar(mundo, avesso, p, teste));
		return true;
	}

	private static void atravessar(ServerLevel mundo, ServerLevel avesso, ServerPlayer p, boolean teste) {
		Memoria m = Memoria.de(p);
		if (p.isRemoved() || p.hasDisconnected() || p.level() != mundo) {
			m.set(DENTRO, 0);
			m.salvar();
			return;
		}
		if (p.isSleeping()) {
			p.stopSleepInBed(true, false);
		}
		EstadoJogador e = Diretor.estadoParaTeste(p);
		RandomSource sorte = p.getRandom();
		int visita = m.get(VISITAS) + 1;
		BlockPos ancora = p.blockPosition();
		m.set(VOLTA_X, ancora.getX());
		m.set(VOLTA_Y, ancora.getY());
		m.set(VOLTA_Z, ancora.getZ());
		m.salvar();

		int erros = copiar(mundo, avesso, ancora, visita, sorte);

		long tick = mundo.getGameTime();
		e.avessoAncora = ancora;
		e.avessoDesde = tick;
		e.avessoAte = tick + 20L * (visita == 1 ? 45 + sorte.nextInt(31) : 60 + sorte.nextInt(91));
		e.avessoVisita = visita;
		e.avessoVulto = false;
		e.avessoTeste = teste;
		p.teleportTo(avesso, p.getX(), p.getY(), p.getZ(), Set.of(), p.getYRot(), p.getXRot(), false);
		p.fallDistance = 0;
		sentir(p, e, 0.0F);
		Rede.efeito(p, PacoteEfeito.Tipo.ACORDAR, 60, 1.0F);
		Depuracao.log(p, tick / 20, String.format(Locale.ROOT, "AVESSO chegou visita=%d ancora=%s duracao=%ds erros=%d",
				visita, ancora.toShortString(), (e.avessoAte - tick) / 20, erros));
	}

	/**
	 * Copia a região em volta da âncora para as mesmas coordenadas do Avesso, apagada e vazia. Sobrescreve o que
	 * houver lá (inclusive o que sobrou da visita anterior). Devolve quantos blocos "errados" a visita ganhou.
	 */
	static int copiar(ServerLevel mundo, ServerLevel avesso, BlockPos ancora, int visita, RandomSource sorte) {
		BlockPos de = ancora.offset(-RAIO, -ABAIXO, -RAIO);
		BlockPos ate = ancora.offset(RAIO, ACIMA, RAIO);
		// Gera os chunks de lá antes de escrever neles.
		for (int cx = de.getX() >> 4; cx <= ate.getX() >> 4; cx++) {
			for (int cz = de.getZ() >> 4; cz <= ate.getZ() >> 4; cz++) {
				avesso.getChunk(cx, cz);
			}
		}
		// O que ficou caído da outra vez não volta.
		for (ItemEntity item : avesso.getEntitiesOfClass(ItemEntity.class, AABB.encapsulatingFullBlocks(de, ate))) {
			item.discard();
		}
		int sinal = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		List<BlockPos> paredes = new ArrayList<>();
		int minY = Math.max(mundo.getMinY(), de.getY());
		int maxY = Math.min(mundo.getMaxY() - 1, ate.getY());
		for (BlockPos pos : BlockPos.betweenClosed(de.getX(), minY, de.getZ(), ate.getX(), maxY, ate.getZ())) {
			BlockState copia = apagado(mundo.getBlockState(pos));
			if (!avesso.getBlockState(pos).equals(copia)) {
				avesso.setBlock(pos, copia, sinal);
			}
			// Parede: bloco cheio, feito por gente (não é o chão do mundo), com ar de um lado.
			if (visita >= 2 && copia.isSolidRender() && pos.getY() >= ancora.getY() && pos.getY() <= ancora.getY() + 2
					&& mundo.getBlockState(pos.north()).isAir() != mundo.getBlockState(pos.south()).isAir()) {
				paredes.add(pos.immutable());
			}
		}
		// Da segunda visita em diante, a cópia erra: blocos de parede faltando, dois a mais por visita.
		int erros = 0;
		for (int i = 0; i < Math.min(6, 2 * (visita - 1)) && !paredes.isEmpty(); i++) {
			avesso.setBlock(paredes.remove(sorte.nextInt(paredes.size())), Blocks.AIR.defaultBlockState(), sinal);
			erros++;
		}
		return erros;
	}

	/** O mesmo bloco, sem luz e sem vida: é assim que ele existe do outro lado. */
	public static BlockState apagado(BlockState s) {
		if (s.isAir()) {
			return s;
		}
		if (s.is(Blocks.SPAWNER) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) {
			return Blocks.AIR.defaultBlockState();
		}
		if (s.getFluidState().is(Fluids.LAVA) || s.getFluidState().is(Fluids.FLOWING_LAVA)) {
			return s.getFluidState().isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		if (s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT)) {
			s = s.setValue(BlockStateProperties.LIT, false);
		}
		if (s.getLightEmission() > 0) {
			return s.isSolidRender() ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		if (s.getBlock() instanceof DoorBlock && !s.is(Blocks.IRON_DOOR) && s.hasProperty(BlockStateProperties.OPEN)) {
			return s.setValue(BlockStateProperties.OPEN, true);
		}
		return s;
	}

	// =====================================================================
	// Lá dentro
	// =====================================================================

	private static void segundo(ServerLevel avesso, ServerPlayer p) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		long tick = avesso.getGameTime();
		if (e.avessoSaindo) {
			return;
		}
		if (e.avessoAte == 0 || e.avessoAncora == null) {
			// Está aqui sem visita em curso (o servidor reiniciou com ele dentro, ou veio por comando): devolve.
			if (!p.isCreative() && !p.isSpectator()) {
				voltar(p, "SEM_VISITA");
			}
			return;
		}
		double longe = Math.sqrt(p.blockPosition().distSqr(e.avessoAncora));
		if (tick >= e.avessoAte) {
			voltar(p, "TEMPO");
			return;
		}
		if (longe > LONGE_DEMAIS) {
			voltar(p, "LONGE");
			return;
		}
		// Da segunda visita em diante, depois de vinte segundos, ele está lá: de lado, parado, até o fim.
		if (e.avessoVisita >= 2 && !e.avessoVulto && tick - e.avessoDesde >= 20 * 20) {
			e.avessoVulto = true;
			int resta = (int) Math.max(60, e.avessoAte - tick);
			boolean veio = Diretor.invocar(avesso, p, e, HospedeEntity.Modo.OBSERVAR, 75, 130, 10, 16, resta, 1.0, false,
					PedidoManifestacao.deOrigem(HospedeEntity.Origem.OLHO, null));
			Depuracao.log(p, tick / 20, "AVESSO vulto=" + (veio ? "sim" : "nao_coube"));
		}
		HospedeEntity h = e.criatura;
		boolean olhando = h != null && !h.isRemoved() && h.level() == avesso;
		sentir(p, e, olhando ? 0.6F : 0.0F);
	}

	/** O que o cliente mostra lá: a cor e o som do Avesso, a neblina perto, nenhuma música. */
	private static void sentir(ServerPlayer p, EstadoJogador e, float vigia) {
		e.sentidos = new PacoteSentidos(0.9F, vigia, 0.0F, 0.6F, PacoteSentidos.FLAG_AVESSO | PacoteSentidos.FLAG_SEM_MUSICA);
		Rede.enviar(p, e.sentidos);
	}

	// =====================================================================
	// Voltar
	// =====================================================================

	/** "Acordar": apagão, e ele está de volta no ponto de onde saiu. */
	static void voltar(ServerPlayer p, String motivo) {
		MinecraftServer server = p.level().getServer();
		ServerLevel mundo = server.overworld();
		EstadoJogador e = Diretor.estadoParaTeste(p);
		if (e.avessoSaindo) {
			return;
		}
		e.avessoSaindo = true;
		Rede.efeito(p, PacoteEfeito.Tipo.APAGAO, 30, 1.0F);
		Depuracao.log(p, mundo.getGameTime() / 20, "AVESSO saindo motivo=" + motivo);
		Diretor.agendar(mundo, 12, () -> chegarDeVolta(mundo, p, e, motivo));
	}

	private static void chegarDeVolta(ServerLevel mundo, ServerPlayer p, EstadoJogador e, String motivo) {
		e.avessoSaindo = false;
		if (p.isRemoved() || p.hasDisconnected()) {
			// Saiu do jogo no meio do apagão: ao voltar, cai em "sem visita em curso" e é devolvido.
			e.avessoAte = 0;
			return;
		}
		Memoria m = Memoria.de(p);
		long tick = mundo.getGameTime();
		if (e.criatura != null && !e.criatura.isRemoved() && e.criatura.level().dimension() == DIMENSAO) {
			e.criatura.discard();
		}
		boolean temVolta = m.get(DENTRO) == 1;
		BlockPos volta = temVolta ? new BlockPos(m.get(VOLTA_X), m.get(VOLTA_Y), m.get(VOLTA_Z)) : mundo.getRespawnData().pos();
		boolean teste = e.avessoTeste;
		long ficou = e.avessoDesde > 0 ? (tick - e.avessoDesde) / 20 : 0;
		p.teleportTo(mundo, volta.getX() + 0.5, volta.getY(), volta.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), false);
		p.fallDistance = 0;
		p.clearFire();
		if (p.getHealth() < 4.0F) {
			p.setHealth(4.0F);
		}
		m.set(DENTRO, 0);
		if (!teste && e.avessoAte != 0) {
			m.add(VISITAS, 1);
			m.set(ULTIMO_DIA, (int) (mundo.getDefaultClockTime() / 24000L));
		}
		m.salvar();
		e.avessoAte = 0;
		e.avessoAncora = null;
		e.sentidos = PacoteSentidos.NEUTRO;
		Rede.enviar(p, PacoteSentidos.NEUTRO);
		Rede.efeito(p, PacoteEfeito.Tipo.ACORDAR, 60, 1.0F);
		// O que ele traz de volta é dúvida: por alguns minutos, uma ou duas luzes da casa de verdade estão apagadas.
		// E o Diretor o deixa em paz por dois minutos.
		int residuo = ApoioCaca.apagarLuzPerto(mundo, p, volta, 10, 20 * (120 + p.getRandom().nextInt(181)), 1 + p.getRandom().nextInt(2));
		e.treguaAte = Math.max(e.treguaAte, tick + 20L * 120);
		Depuracao.log(p, tick / 20, String.format(Locale.ROOT, "AVESSO voltou motivo=%s ficou=%ds visitas=%d residuo=%d pos=%s",
				motivo, ficou, m.get(VISITAS), residuo, volta.toShortString()));
	}

	// =====================================================================
	// Testes
	// =====================================================================

	/** Comando de teste e testes automáticos: leva agora (não conta como visita) ou traz de volta. */
	public static String testar(ServerPlayer p, boolean ir) {
		if (!existe(p.level().getServer())) {
			return "a dimensão não existe neste mundo.";
		}
		if (ir) {
			return levar(p, "COMANDO", true) ? "Levado." : "não dá para levar agora (já está lá, ou não está no mundo normal).";
		}
		if (p.level().dimension() != DIMENSAO) {
			return "você não está lá.";
		}
		voltar(p, "COMANDO");
		return "De volta.";
	}

	/** Para os testes: de qual visita é a cópia em que ele está (0 fora de lá). */
	public static int visitaAtual(ServerPlayer p) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		return e.avessoAte == 0 ? 0 : e.avessoVisita;
	}

	/** Só para os testes: finge que este jogador já esteve lá tantas vezes. */
	public static void definirVisitas(ServerPlayer p, int visitas) {
		Memoria m = Memoria.de(p);
		m.set(VISITAS, visitas);
		m.salvar();
	}
}
