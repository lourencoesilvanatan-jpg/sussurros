package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.manifestacao.PedidoManifestacao;
import com.sussurros.entidade.HospedeEntity;

/**
 * Para onde ele vai quando, em vez de sumir, vai embora andando (0.9.0-alpha17).
 *
 * Até a alpha16 toda aparição de observação acabava do mesmo jeito: dissolvia no instante em que era olhada.
 * Numa hora de jogo de verdade (10 e 11/10/2026) o jogador o viu por dois segundos somados, em três relances,
 * e pediu que ele às vezes saísse "indo para o lado, e não sumindo". O relance continua sendo a regra; isto é
 * a exceção (ver HospedeEntity.Saida).
 *
 * O lugar é escolhido uma vez, quando ele começa a sair: de lado em relação ao jogador (o lado sai do ID da
 * manifestação, sem sorteio do mundo), a poucos blocos, de preferência onde o jogador deixa de enxergá-lo
 * (atrás de uma árvore, de uma parede, fora do quadro da janela), e só onde há caminho. Sem lugar, ele some
 * como sempre.
 */
public final class Retirada {
	/** Blocos por segundo de quem vai embora sem pressa. O jogador anda a 4,3. */
	public static final double VELOCIDADE = 3.0;

	private Retirada() {
	}

	private record Candidato(Vec3 ponto, double nota, boolean escondido) {
	}

	/** O primeiro trecho: de lado, para fora da vista. Null se não há para onde ir. */
	@Nullable
	public static Vec3 escolher(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		Vec3 eixo = new Vec3(h.getX() - p.getX(), 0, h.getZ() - p.getZ());
		if (eixo.lengthSqr() < 1.0E-4) {
			return null;
		}
		eixo = eixo.normalize();
		Vec3 lado = new Vec3(-eixo.z, 0, eixo.x);
		int sinal = h.numeroDoId() % 2 == 0 ? 1 : -1;
		List<Candidato> candidatos = new ArrayList<>();
		for (int s : new int[] {sinal, -sinal}) {
			for (double d : new double[] {5.0, 7.0, 9.0, 3.5}) {
				for (double recuo : new double[] {1.5, 3.5, 0.0}) {
					Vec3 alvo = h.position().add(lado.scale(s * d)).add(eixo.scale(recuo));
					BlockPos chao = Diretor.acharChao(level, alvo.x, h.getY() + 1, alvo.z);
					if (chao == null || Math.abs(chao.getY() - h.getY()) > 2
							|| Diretor.emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
						continue;
					}
					Vec3 ponto = new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
					boolean escondido = !enxerga(level, p, ponto.add(0, 0.6, 0)) && !enxerga(level, p, ponto.add(0, 2.4, 0));
					double nota = (escondido ? 3.0 : 0.0) + (s == sinal ? 0.3 : 0.0) + (d >= 5.0 && d <= 7.0 ? 0.2 : 0.0);
					candidatos.add(new Candidato(ponto, nota, escondido));
				}
			}
		}
		candidatos.sort(Comparator.comparingDouble(Candidato::nota).reversed());
		// Calcular caminho custa: só para os seis melhores.
		for (int i = 0; i < Math.min(6, candidatos.size()); i++) {
			Candidato c = candidatos.get(i);
			if (chega(h, c.ponto())) {
				Depuracao.log(p, level.getGameTime() / 20, String.format(Locale.ROOT,
						"HOSPEDE id=%s saida=RECUA dist=%.1f para=%s escondido=%s",
						h.getIdManifestacao(), Math.sqrt(h.distanceToSqr(p)), Diretor.pos(c.ponto().x, c.ponto().y, c.ponto().z),
						c.escondido() ? "sim" : "nao"));
				return c.ponto();
			}
		}
		return null;
	}

	/** O segundo trecho, para quem chegou ao lugar e continua à vista: de costas para o jogador, para longe. */
	@Nullable
	public static Vec3 maisLonge(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		Vec3 eixo = new Vec3(h.getX() - p.getX(), 0, h.getZ() - p.getZ());
		if (eixo.lengthSqr() < 1.0E-4) {
			return null;
		}
		eixo = eixo.normalize();
		for (double graus : new double[] {0, 25, -25, 50, -50}) {
			double a = Math.toRadians(graus);
			Vec3 direcao = new Vec3(eixo.x * Math.cos(a) - eixo.z * Math.sin(a), 0, eixo.x * Math.sin(a) + eixo.z * Math.cos(a));
			Vec3 alvo = h.position().add(direcao.scale(10.0));
			BlockPos chao = Diretor.acharChao(level, alvo.x, h.getY() + 1, alvo.z);
			if (chao == null || Math.abs(chao.getY() - h.getY()) > 3) {
				continue;
			}
			Vec3 ponto = new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
			if (chega(h, ponto)) {
				return ponto;
			}
		}
		return null;
	}

	private static boolean chega(HospedeEntity h, Vec3 ponto) {
		BlockPos destino = BlockPos.containing(ponto);
		Path caminho = h.getNavigation().createPath(destino, 0);
		return caminho != null && (caminho.canReach()
				|| (caminho.getEndNode() != null && caminho.getEndNode().asBlockPos().distSqr(destino) <= 2.25));
	}

	/** O jogador enxerga este ponto? Como na Percepcao: pela forma que se vê, então vidro não tampa. */
	private static boolean enxerga(ServerLevel level, ServerPlayer p, Vec3 ponto) {
		HitResult r = level.clip(new ClipContext(p.getEyePosition(), ponto, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
		return r.getType() == HitResult.Type.MISS;
	}

	/**
	 * Só para os testes e para o comando de teste: uma presença a 18 blocos do jogador, fora da tela (atrás e
	 * de lado), com a saída escolhida. O lugar é fixo, sem a busca de sempre: de dia e em campo aberto a busca
	 * recusa pôr alguém tão perto (ver Diretor.expostoDemais), e as fotos precisam justamente da luz do dia.
	 */
	public static boolean presencaParaTeste(ServerPlayer p, boolean saiAndando) {
		ServerLevel level = p.level();
		EstadoJogador e = Diretor.estadoParaTeste(p);
		if (e.criatura != null && !e.criatura.isRemoved()) {
			e.criatura.sumir(level, false, "SUBSTITUIDA_POR_COMANDO");
		}
		for (double angulo : new double[] {130, -130, 150, -150, 110, -110}) {
			Vec3 ponto = Diretor.pontoRelativo(p, angulo, 18.0);
			BlockPos chao = Diretor.acharChao(level, ponto.x, p.getY() + 1, ponto.z);
			if (chao == null || Math.abs(chao.getY() - p.getY()) > 3 || Diretor.emZonaCalma(p, chao.getX(), chao.getY(), chao.getZ())) {
				continue;
			}
			Diretor.criar(level, p, e, chao, HospedeEntity.Modo.OBSERVAR, 20 * 30, 1.0, HospedeEntity.DIST_SUMIR_PADRAO,
					PedidoManifestacao.deComando(Evento.PRESENCA).comNota("TESTE_DE_SAIDA"));
			if (e.criatura == null) {
				return false;
			}
			if (saiAndando) {
				e.criatura.definirSaida(HospedeEntity.Saida.RECUA, 0.0);
			}
			return true;
		}
		return false;
	}
}
