package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.diretor.Agenda;
import com.sussurros.registro.ModSons;

/**
 * Camada ambiental da 0.6.0. Ela não substitui o Diretor: dá a ele um repertório de perturbações
 * do mundo com orçamento, cooldown por família e memória de repetição.
 */
final class Atmosfera {
	enum Familia {
		ANIMAIS, LUZ, OBJETO, RUIDO, VESTIGIO
	}

	static final class Estado {
		double orcamento = 6.0;
		long ultimoSeg = -1;
		long proximoPressagio = -1;
		long proximaPerturbacao = -1;
		final EnumMap<Familia, Long> cooldown = new EnumMap<>(Familia.class);
		final ArrayDeque<String> recentes = new ArrayDeque<>();
	}

	record Resultado(@Nullable Vec3 fonte, double observabilidade, String detalhe) {
	}

	private record OlharAnimal(ResourceKey<Level> dimensao, UUID animal, UUID jogador, @Nullable Vec3 inicio, @Nullable Vec3 fim,
			long inicioTick, long fimTick, boolean olharJogador) {
	}

	private static final List<OlharAnimal> OLHARES = new ArrayList<>();
	private static final double ORCAMENTO_MAX = 7.5;

	private Atmosfera() {
	}

	static void tickRapido(ServerLevel level, long tick) {
		AlteracoesTemporarias.tick(level, tick);
		if (OLHARES.isEmpty() || tick % 5 != 0) {
			return;
		}
		Iterator<OlharAnimal> it = OLHARES.iterator();
		while (it.hasNext()) {
			OlharAnimal a = it.next();
			if (!a.dimensao().equals(level.dimension())) {
				continue;
			}
			if (tick >= a.fimTick()) {
				it.remove();
				continue;
			}
			Entity ent = level.getEntity(a.animal());
			if (!(ent instanceof Animal animal) || !animal.isAlive()) {
				it.remove();
				continue;
			}
			Vec3 alvo;
			if (a.olharJogador()) {
				Entity jogador = level.getEntity(a.jogador());
				if (jogador == null) {
					it.remove();
					continue;
				}
				alvo = jogador.getEyePosition();
			} else if (a.inicio() != null && a.fim() != null) {
				double t = (tick - a.inicioTick()) / (double) Math.max(1, a.fimTick() - a.inicioTick());
				alvo = a.inicio().lerp(a.fim(), Math.max(0, Math.min(1, t)));
			} else if (a.inicio() != null) {
				alvo = a.inicio();
			} else {
				it.remove();
				continue;
			}
			animal.getNavigation().stop();
			animal.getLookControl().setLookAt(alvo.x, alvo.y, alvo.z, 30.0F, 30.0F);
		}
	}

	static void limpar() {
		OLHARES.clear();
		AlteracoesTemporarias.limpar();
	}

	static void atualizar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean subterraneo, boolean noite, long seg, long tick, RandomSource rnd) {
		Estado a = e.atmosfera;
		if (a.ultimoSeg < 0) {
			a.ultimoSeg = seg;
			a.proximoPressagio = seg + 130 + rnd.nextInt(111); // 2m10-4m iniciais
			a.proximaPerturbacao = seg + 240 + rnd.nextInt(181);
			return;
		}
		long delta = Math.max(0, seg - a.ultimoSeg);
		a.ultimoSeg = seg;
		a.orcamento = Math.min(ORCAMENTO_MAX, a.orcamento + delta * 0.018);

		if (e.zonaAteTick > tick || e.forcando || temCenaAtiva(e) || tick - e.ultimoDanoTick < 200) {
			return;
		}

		// Presságios existem especialmente na fase 0. Depois continuam, mas mais espaçados.
		if (seg >= a.proximoPressagio) {
			boolean feito = tentarPressagio(level, p, m, e, fase, subterraneo, noite, seg, tick, rnd);
			if (feito) {
				int base = fase == 0 ? 120 : 210;
				int variacao = fase == 0 ? 150 : 210;
				a.proximoPressagio = seg + base + rnd.nextInt(variacao + 1);
			} else {
				a.proximoPressagio = seg + 35 + rnd.nextInt(31);
			}
		}

		if (fase >= 1 && seg >= a.proximaPerturbacao && e.criatura == null) {
			boolean feito = tentarMicrocena(level, p, m, e, fase, subterraneo, noite, seg, tick, rnd);
			a.proximaPerturbacao = seg + (feito ? 260 + rnd.nextInt(281) : 60 + rnd.nextInt(61));
		}
	}

	static boolean podeEvento(ServerLevel level, ServerPlayer p, EstadoJogador e, Evento ev, long seg) {
		// Custos e pontos do Rastro abaixo repetem o que a execução de cada evento exige. Quando a checagem
		// era mais frouxa, o Diretor sorteava um evento que não conseguia acontecer.
		return switch (ev) {
			case ANIMAIS -> disponivel(e, Familia.ANIMAIS, 2.2, seg) && animaisProximos(level, p).size() >= 2;
			case LUZ_ERRADA -> disponivel(e, Familia.LUZ, 2.0, seg);
			case OBJETO_FORA_LUGAR -> disponivel(e, Familia.OBJETO, 2.5, seg) && (e.rastro.tamanho() >= 5 || !e.portas.isEmpty());
			case PASSAGEM, SINAL_DISTANTE -> disponivel(e, Familia.RUIDO, 1.5, seg);
			case VESTIGIO -> disponivel(e, Familia.VESTIGIO, 1.8, seg) && !rastroAoAlcance(p, e, seg, 8, 150, 5, 22).isEmpty();
			case TRILHA_INTERROMPIDA -> disponivel(e, Familia.VESTIGIO, 2.0, seg) && rastroAoAlcance(p, e, seg, 8, 180, 5, 26).size() >= 3;
			case RUIDO_RETORNO -> disponivel(e, Familia.RUIDO, 1.8, seg) && !e.acoes.isEmpty();
			default -> true;
		};
	}

	@Nullable
	static Resultado executarEvento(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Evento ev,
			int fase, long seg, long tick, RandomSource rnd) {
		return switch (ev) {
			case ANIMAIS -> animais(level, p, e, seg, tick, rnd, false);
			case LUZ_ERRADA -> luzErrada(level, p, e, fase, seg, tick, rnd, false);
			case OBJETO_FORA_LUGAR -> objetoForaLugar(level, p, m, e, fase, seg, tick, rnd, false);
			case PASSAGEM -> passagem(level, p, e, seg, rnd, false);
			case VESTIGIO -> vestigio(level, p, e, seg, rnd, false);
			case SINAL_DISTANTE -> sinalDistante(level, p, e, seg, rnd, false);
			case RUIDO_RETORNO -> ruidoRetorno(level, p, e, seg, rnd, false);
			case TRILHA_INTERROMPIDA -> trilhaInterrompida(level, p, e, seg, rnd, false);
			default -> null;
		};
	}

	static String testarPressagio(ServerLevel level, ServerPlayer p) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		Memoria m = Memoria.de(p);
		RandomSource rnd = level.getRandom();
		boolean antigo = e.forcando;
		e.forcando = true;
		try {
			boolean ok = tentarPressagio(level, p, m, e, Math.max(0, m.get(Memoria.FASE)), false,
					Diretor.ehNoiteParaTeste(level), level.getGameTime() / 20, level.getGameTime(), rnd);
			return ok ? "Presságio executado." : "Nenhum presságio coube neste lugar agora.";
		} finally {
			e.forcando = antigo;
		}
	}

	static String testarPerturbacao(ServerLevel level, ServerPlayer p, Perturbacao tipo) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		Memoria m = Memoria.de(p);
		long seg = level.getGameTime() / 20;
		boolean antigo = e.forcando;
		e.forcando = true;
		try {
			boolean ok = executarPerturbacao(level, p, m, e, Math.max(1, m.get(Memoria.FASE)), tipo,
					seg, level.getGameTime(), level.getRandom(), true);
			return ok ? "Cena ambiental: " + tipo.name() + "." : "A cena " + tipo.name() + " não encontrou condições válidas.";
		} finally {
			e.forcando = antigo;
		}
	}

	private static boolean tentarPressagio(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean subterraneo, boolean noite, long seg, long tick, RandomSource rnd) {
		List<Pressagio> opcoes = new ArrayList<>();
		opcoes.add(Pressagio.PASSO_DISTANTE);
		opcoes.add(Pressagio.RUIDO_NA_PAREDE);
		if (!e.acoes.isEmpty()) opcoes.add(Pressagio.ECO_TARDIO);
		if (e.rastro.tamanho() >= 5) opcoes.add(Pressagio.CINZA_NO_RASTRO);
		if (animaisProximos(level, p).size() >= 2) {
			opcoes.add(Pressagio.ANIMAIS_VAZIO);
			opcoes.add(Pressagio.ANIMAIS_JOGADOR);
			opcoes.add(Pressagio.ANIMAIS_MOVIMENTO);
		}
		if (subterraneo || noite) opcoes.add(Pressagio.LUZ_DISTANTE);
		if (opcoes.isEmpty()) return false;

		// Evita repetir o mesmo presságio recentemente sem precisar de outro sistema de peso.
		for (int tentativa = 0; tentativa < 6 && !opcoes.isEmpty(); tentativa++) {
			Pressagio pr = opcoes.remove(rnd.nextInt(opcoes.size()));
			String chave = "P:" + pr.name();
			if (e.atmosfera.recentes.contains(chave)) continue;
			boolean ok = executarPressagio(level, p, m, e, pr, fase, seg, tick, rnd);
			if (ok) {
				lembrar(e, chave);
				Depuracao.log(p, seg, "PRESSAGIO tipo=" + pr + " fase=" + fase + " contexto=" + e.contexto + " semAprendizado=sim");
				return true;
			}
		}
		return false;
	}

	private static boolean executarPressagio(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, Pressagio pr,
			int fase, long seg, long tick, RandomSource rnd) {
		return switch (pr) {
			case PASSO_DISTANTE -> sinalDistante(level, p, e, seg, rnd, true) != null;
			case ECO_TARDIO -> ruidoRetorno(level, p, e, seg, rnd, true) != null;
			case ANIMAIS_VAZIO -> animaisComTipo(level, p, e, seg, tick, rnd, "VAZIO", true) != null;
			case ANIMAIS_JOGADOR -> animaisComTipo(level, p, e, seg, tick, rnd, "JOGADOR", true) != null;
			case ANIMAIS_MOVIMENTO -> animaisComTipo(level, p, e, seg, tick, rnd, "MOVIMENTO", true) != null;
			case CINZA_NO_RASTRO -> vestigio(level, p, e, seg, rnd, true) != null;
			case LUZ_DISTANTE -> luzFantasma(level, p, e, seg, tick, rnd, true);
			case RUIDO_NA_PAREDE -> {
				Vec3 pt = pontoRelativo(p, 100 + rnd.nextDouble() * 160, 9 + rnd.nextInt(10));
				ModSons.tocar(level, pt.x, p.getY() + 0.5, pt.z,
						rnd.nextBoolean() ? ModSons.Som.MADEIRA : ModSons.Som.ESTALO,
						Diretor.volumePara(p, pt.x, pt.z, 0.36F), 0.88F);
				yield gastar(e, Familia.RUIDO, 1.0, seg, rnd, 120, 240);
			}
		};
	}

	private static boolean tentarMicrocena(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			boolean subterraneo, boolean noite, long seg, long tick, RandomSource rnd) {
		List<Perturbacao> opcoes = new ArrayList<>();
		if (animaisProximos(level, p).size() >= 2) {
			opcoes.add(Perturbacao.TODOS_OLHANDO);
			if (e.contexto == ContextoMundo.Tipo.CASA) opcoes.add(Perturbacao.HA_ALGO_NO_CURRAL);
		}
		if (subterraneo) {
			opcoes.add(Perturbacao.LUZ_NO_FIM);
			opcoes.add(Perturbacao.PASSOU_PELA_MINA);
		}
		if (e.rastro.tamanho() >= 8) opcoes.add(Perturbacao.O_CAMINHO_MUDOU);
		if (e.contexto == ContextoMundo.Tipo.CASA && !e.portas.isEmpty()) opcoes.add(Perturbacao.NAO_FOI_VOCE);
		if (fase >= 3 && e.rastro.tamanho() >= 12) opcoes.add(Perturbacao.MARCA_IMPOSSIVEL);
		if (opcoes.isEmpty()) return false;
		for (int i = 0; i < 6 && !opcoes.isEmpty(); i++) {
			Perturbacao pt = opcoes.remove(rnd.nextInt(opcoes.size()));
			String chave = "C:" + pt.name();
			if (e.atmosfera.recentes.contains(chave)) continue;
			if (executarPerturbacao(level, p, m, e, fase, pt, seg, tick, rnd, false)) {
				lembrar(e, chave);
				return true;
			}
		}
		return false;
	}

	private static boolean executarPerturbacao(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			Perturbacao pt, long seg, long tick, RandomSource rnd, boolean teste) {
		boolean ok = switch (pt) {
			case TODOS_OLHANDO -> animaisComTipo(level, p, e, seg, tick, rnd, "VAZIO", teste) != null;
			case HA_ALGO_NO_CURRAL -> animaisComTipo(level, p, e, seg, tick, rnd, "MOVIMENTO", teste) != null;
			case LUZ_NO_FIM -> luzFantasma(level, p, e, seg, tick, rnd, teste);
			case PASSOU_PELA_MINA -> sequenciaLuzes(level, p, e, seg, tick, rnd, teste);
			case O_CAMINHO_MUDOU -> marcaTemporaria(level, p, e, seg, tick, rnd, false, teste);
			case MARCA_IMPOSSIVEL -> marcaTemporaria(level, p, e, seg, tick, rnd, true, teste);
			case NAO_FOI_VOCE -> naoFoiVoce(level, p, e, seg, rnd, teste);
		};
		if (ok) {
			Depuracao.log(p, seg, "ATMOSFERA cena=" + pt + " teste=" + (teste ? "sim" : "nao") + " contexto=" + e.contexto);
		}
		return ok;
	}

	@Nullable
	private static Resultado animais(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick,
			RandomSource rnd, boolean sutil) {
		String[] tipos = {"VAZIO", "JOGADOR", "MOVIMENTO"};
		return animaisComTipo(level, p, e, seg, tick, rnd, tipos[rnd.nextInt(tipos.length)], sutil);
	}

	@Nullable
	private static Resultado animaisComTipo(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick,
			RandomSource rnd, String tipo, boolean sutil) {
		double custo = sutil ? 1.5 : 2.2;
		if (!disponivel(e, Familia.ANIMAIS, custo, seg)) return null;
		List<Animal> animais = animaisProximos(level, p);
		if (animais.size() < 2) return null;
		animais.sort(Comparator.comparingDouble(a -> a.distanceToSqr(p)));
		int qtd = Math.min(animais.size(), 2 + rnd.nextInt(Math.min(4, animais.size()) - 1));
		Vec3 inicio;
		Vec3 fim = null;
		boolean jogador = "JOGADOR".equals(tipo);
		if (jogador) {
			inicio = p.getEyePosition();
		} else {
			inicio = pontoRelativo(p, 100 + rnd.nextDouble() * 160, 9 + rnd.nextInt(10)).add(0, 1.2, 0);
			if ("MOVIMENTO".equals(tipo)) {
				fim = inicio.add((rnd.nextBoolean() ? 1 : -1) * (6 + rnd.nextInt(7)), 0, (rnd.nextBoolean() ? 1 : -1) * (6 + rnd.nextInt(7)));
			}
		}
		long fimTick = tick + 60 + rnd.nextInt(61);
		for (int i = 0; i < qtd; i++) {
			Animal a = animais.get(i);
			OLHARES.add(new OlharAnimal(level.dimension(), a.getUUID(), p.getUUID(), inicio, fim, tick, fimTick, jogador));
		}
		if (!gastar(e, Familia.ANIMAIS, custo, seg, rnd, 420, 780)) return null;
		Depuracao.log(p, seg, "ANIMAIS tipo=" + tipo + " qtd=" + qtd + " duracao=" + ((fimTick - tick) / 20) + "s semCriatura=sim");
		return new Resultado(inicio, 0.65, "ANIMAIS_" + tipo);
	}

	@Nullable
	private static Resultado luzErrada(ServerLevel level, ServerPlayer p, EstadoJogador e, int fase, long seg, long tick,
			RandomSource rnd, boolean sutil) {
		if (!disponivel(e, Familia.LUZ, sutil ? 1.3 : 2.0, seg)) return null;
		List<BlockPos> tochas = tochasValidas(level, p, e, 16);
		int escolha = rnd.nextInt(100);
		String tipo;
		Vec3 fonte;
		boolean ok;
		if (!tochas.isEmpty() && escolha < 42) {
			BlockPos pos = tochas.get(rnd.nextInt(tochas.size()));
			ok = AlteracoesTemporarias.substituir(level, pos, Blocks.AIR.defaultBlockState(), 40 + rnd.nextInt(81), "LUZ_PISCA");
			fonte = Vec3.atCenterOf(pos);
			tipo = "PISCA";
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.55F, 0.9F);
		} else if (!tochas.isEmpty() && escolha < 67) {
			BlockPos origem = tochas.get(rnd.nextInt(tochas.size()));
			BlockPos destino = acharPontoParaTocha(level, p, rnd, 8, 22);
			if (destino == null) return null;
			long duracao = 100 + rnd.nextInt(101);
			ok = AlteracoesTemporarias.substituir(level, origem, Blocks.AIR.defaultBlockState(), duracao, "LUZ_MIGRA_ORIGEM")
					&& AlteracoesTemporarias.substituir(level, destino, Blocks.TORCH.defaultBlockState(), duracao, "LUZ_MIGRA_DESTINO");
			fonte = Vec3.atCenterOf(destino);
			tipo = "MIGRA";
			level.playSound(null, origem.getX() + 0.5, origem.getY() + 0.5, origem.getZ() + 0.5,
					SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.45F, 0.85F);
		} else if (!tochas.isEmpty() && fase >= 3 && escolha < 74) {
			BlockPos pos = tochas.get(rnd.nextInt(tochas.size()));
			level.destroyBlock(pos, true);
			fonte = Vec3.atCenterOf(pos);
			tipo = "QUEBROU_COM_DROP";
			ok = true;
		} else {
			BlockPos destino = acharPontoParaTocha(level, p, rnd, 10, 28);
			if (destino == null) return null;
			ok = AlteracoesTemporarias.substituir(level, destino, Blocks.TORCH.defaultBlockState(), 140 + rnd.nextInt(181), "LUZ_APARECE");
			fonte = Vec3.atCenterOf(destino);
			tipo = "APARECE";
		}
		if (!ok || !gastar(e, Familia.LUZ, sutil ? 1.3 : 2.0, seg, rnd, 360, 720)) return null;
		Depuracao.log(p, seg, "LUZ_ERRADA tipo=" + tipo + " pos=" + pos(fonte) + " semCriatura=sim");
		return new Resultado(fonte, 0.70, tipo);
	}

	private static boolean luzFantasma(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick,
			RandomSource rnd, boolean sutil) {
		double custo = sutil ? 1.1 : 2.0;
		if (!disponivel(e, Familia.LUZ, custo, seg)) return false;
		BlockPos destino = acharPontoParaTocha(level, p, rnd, sutil ? 12 : 14, sutil ? 28 : 34);
		if (destino == null) return false;
		long duracao = sutil ? 80 + rnd.nextInt(101) : 120 + rnd.nextInt(161);
		if (!AlteracoesTemporarias.substituir(level, destino, Blocks.TORCH.defaultBlockState(), duracao, "LUZ_FANTASMA")) {
			return false;
		}
		if (!gastar(e, Familia.LUZ, custo, seg, rnd, sutil ? 300 : 480, sutil ? 600 : 900)) return false;
		Depuracao.log(p, seg, "LUZ_ERRADA tipo=FANTASMA pos=" + pos(Vec3.atCenterOf(destino))
				+ " duracao=" + (duracao / 20) + "s semCriatura=sim");
		return true;
	}

	private static boolean sequenciaLuzes(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick,
			RandomSource rnd, boolean teste) {
		if (!teste && !disponivel(e, Familia.LUZ, 3.0, seg)) return false;
		List<BlockPos> tochas = tochasValidas(level, p, e, 18);
		if (tochas.size() < 2) return false;
		tochas.sort(Comparator.comparingDouble(pos -> distancia(p, Vec3.atCenterOf(pos))));
		int qtd = Math.min(3, tochas.size());
		for (int i = 0; i < qtd; i++) {
			BlockPos pos = tochas.get(i);
			int atraso = i * 12;
			Agenda.agendar(level, atraso, () -> {
				AlteracoesTemporarias.substituir(level, pos, Blocks.AIR.defaultBlockState(), 90, "LUZ_SEQUENCIA");
				level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.45F, 0.82F);
			});
		}
		if (!teste) gastar(e, Familia.LUZ, 3.0, seg, rnd, 600, 960);
		Depuracao.log(p, seg, "LUZ_ERRADA tipo=SEQUENCIA qtd=" + qtd + " semCriatura=sim");
		return true;
	}

	@Nullable
	private static Resultado objetoForaLugar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int fase,
			long seg, long tick, RandomSource rnd, boolean sutil) {
		if (!disponivel(e, Familia.OBJETO, sutil ? 1.5 : 2.5, seg)) return null;
		boolean ok = marcaTemporaria(level, p, e, seg, tick, rnd, fase >= 3 && rnd.nextFloat() < 0.25F, sutil);
		if (!ok) return null;
		return new Resultado(p.position(), 0.45, "OBJETO_FORA_LUGAR");
	}

	private static boolean marcaTemporaria(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, long tick,
			RandomSource rnd, boolean impossivel, boolean teste) {
		if (!teste && !disponivel(e, Familia.OBJETO, impossivel ? 4.5 : 2.5, seg)) return false;
		Rastro.Ponto pt = escolherRastro(p, e, seg, 15, 240, 10, 42, rnd);
		Vec3 baseV = pt != null ? new Vec3(pt.x(), pt.y(), pt.z()) : pontoRelativo(p, 110 + rnd.nextDouble() * 140, 14 + rnd.nextInt(16));
		BlockPos base = acharChao(level, BlockPos.containing(baseV.x, baseV.y, baseV.z));
		if (base == null || naTela(p, Vec3.atCenterOf(base.above()))) return false;
		List<BlockPos> alvos = new ArrayList<>();
		alvos.add(base.above());
		if (impossivel) {
			alvos.add(base.above(2));
			alvos.add(base.offset(1, 1, 0));
		}
		for (BlockPos pos : alvos) {
			if (!level.isEmptyBlock(pos)) return false;
		}
		long duracao = impossivel ? 20L * (120 + rnd.nextInt(121)) : 20L * (45 + rnd.nextInt(76));
		for (int i = 0; i < alvos.size(); i++) {
			BlockState st = i == alvos.size() - 1 && impossivel ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
					: (rnd.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			AlteracoesTemporarias.substituir(level, alvos.get(i), st, duracao, impossivel ? "MARCA_IMPOSSIVEL" : "CAMINHO_MUDOU");
		}
		if (!teste) gastar(e, Familia.OBJETO, impossivel ? 4.5 : 2.5, seg, rnd, impossivel ? 900 : 480, impossivel ? 1500 : 840);
		Depuracao.log(p, seg, "OBJETO tipo=" + (impossivel ? "MARCA_IMPOSSIVEL" : "CAMINHO_MUDOU")
				+ " pos=" + pos(Vec3.atCenterOf(base.above())) + " duracao=" + (duracao / 20) + "s");
		return true;
	}

	private static boolean naoFoiVoce(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean teste) {
		if (!teste && !disponivel(e, Familia.OBJETO, 2.2, seg)) return false;
		BlockPos porta = null;
		for (BlockPos pos : e.portas) {
			BlockState st = level.getBlockState(pos);
			if (st.getBlock() instanceof DoorBlock && distancia(p, Vec3.atCenterOf(pos)) <= 16 && !naTela(p, Vec3.atCenterOf(pos))) {
				porta = pos;
				break;
			}
		}
		if (porta == null) return false;
		Vec3 fonte = Vec3.atCenterOf(porta);
		// Volume abaixo de 1 só alcança 16 blocos e já chega quase mudo perto disso: escala com a distância.
		float volumePorta = Diretor.volumePara(p, fonte.x, fonte.z, 0.75F);
		float volumeEstalo = Diretor.volumePara(p, fonte.x, fonte.z, 0.38F);
		level.playSound(null, porta.getX() + 0.5, porta.getY() + 0.5, porta.getZ() + 0.5,
				rnd.nextBoolean() ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
				SoundSource.BLOCKS, volumePorta, 0.88F);
		Agenda.agendar(level, 30 + rnd.nextInt(31), () -> ModSons.tocar(level, fonte.x, fonte.y, fonte.z,
				ModSons.Som.ESTALO, volumeEstalo, 0.9F));
		if (!teste) gastar(e, Familia.OBJETO, 2.2, seg, rnd, 420, 780);
		Depuracao.log(p, seg, "OBJETO tipo=NAO_FOI_VOCE porta=" + porta + " semCriatura=sim");
		return true;
	}

	@Nullable
	private static Resultado passagem(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean sutil) {
		if (!disponivel(e, Familia.RUIDO, sutil ? 1.0 : 1.5, seg)) return null;
		int lado = rnd.nextBoolean() ? 1 : -1;
		Vec3 primeiro = pontoRelativo(p, 110 * lado, 10 + rnd.nextInt(7));
		for (int i = 0; i < 4; i++) {
			int n = i;
			Vec3 pt = pontoRelativo(p, (110 - n * 35) * lado, 10 + n * 2);
			float volume = Diretor.volumePara(p, pt.x, pt.z, 0.38F);
			Agenda.agendar(level, i * (7 + rnd.nextInt(5)), () -> ModSons.tocar(level, pt.x, p.getY(), pt.z,
					n % 2 == 0 ? ModSons.Som.PANO : ModSons.Som.ESTALO, volume, 0.88F));
		}
		if (!gastar(e, Familia.RUIDO, sutil ? 1.0 : 1.5, seg, rnd, 180, 360)) return null;
		Depuracao.log(p, seg, "PASSAGEM lado=" + (lado > 0 ? "direita" : "esquerda") + " semCriatura=sim");
		return new Resultado(primeiro, 0.55, "PASSAGEM");
	}

	@Nullable
	private static Resultado vestigio(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean sutil) {
		if (!disponivel(e, Familia.VESTIGIO, sutil ? 1.0 : 1.8, seg)) return null;
		Rastro.Ponto pt = escolherRastro(p, e, seg, 8, 150, 5, 22, rnd);
		if (pt == null) return null;
		Vec3 v = new Vec3(pt.x(), pt.y() + 0.08, pt.z());
		level.sendParticles(ParticleTypes.ASH, v.x, v.y, v.z, sutil ? 5 : 10, 0.35, 0.04, 0.35, 0.002);
		if (!sutil && !e.forcando && rnd.nextFloat() < 0.20F) {
			Vestigios.de(p).registrar(BlockPos.containing(v.x, v.y, v.z), Vestigios.Tipo.PEGADAS, seg);
		}
		if (!gastar(e, Familia.VESTIGIO, sutil ? 1.0 : 1.8, seg, rnd, 210, 420)) return null;
		Depuracao.log(p, seg, "VESTIGIO tipo=CINZA_RASTRO pos=" + pos(v) + " semCriatura=sim");
		return new Resultado(v, 0.50, "VESTIGIO");
	}

	@Nullable
	private static Resultado sinalDistante(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean sutil) {
		if (!disponivel(e, Familia.RUIDO, sutil ? 0.8 : 1.5, seg)) return null;
		Rastro.Ponto pt = escolherRastro(p, e, seg, 10, 240, 18, 42, rnd);
		Vec3 v = pt != null ? new Vec3(pt.x(), pt.y() + 0.7, pt.z())
				: pontoRelativo(p, 100 + rnd.nextDouble() * 160, 24 + rnd.nextInt(17)).add(0, 0.7, 0);
		// O ponto fica a 18-42 blocos: sem escalar o volume, o servidor nem chega a enviar o som (alcance de 16).
		ModSons.tocar(level, v.x, v.y, v.z, rnd.nextBoolean() ? ModSons.Som.ESTALO : ModSons.Som.PANO,
				Diretor.volumePara(p, v.x, v.z, sutil ? 0.32F : 0.45F), 0.86F + rnd.nextFloat() * 0.10F);
		if (!gastar(e, Familia.RUIDO, sutil ? 0.8 : 1.5, seg, rnd, 150, 330)) return null;
		Depuracao.log(p, seg, "SINAL_DISTANTE pos=" + pos(v) + " dist=" + String.format(Locale.ROOT, "%.1f", distancia(p, v)) + " semCriatura=sim");
		return new Resultado(v, 0.35, "SINAL_DISTANTE");
	}

	@Nullable
	private static Resultado ruidoRetorno(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean sutil) {
		if (e.acoes.isEmpty() || !disponivel(e, Familia.RUIDO, sutil ? 1.0 : 1.8, seg)) return null;
		List<EstadoJogador.Acao> lista = new ArrayList<>(e.acoes);
		EstadoJogador.Acao acao = lista.get(rnd.nextInt(lista.size()));
		Rastro.Ponto pt = escolherRastro(p, e, seg, 20, 300, 9, 30, rnd);
		Vec3 v = pt != null ? new Vec3(pt.x(), pt.y(), pt.z()) : new Vec3(acao.x(), acao.y(), acao.z());
		// Sem ponto do Rastro, o lugar da ação antiga só serve se ainda estiver ao alcance do ouvido.
		if (pt == null && distancia(p, v) > Diretor.ALCANCE_ACAO_ANTIGA) return null;
		level.playSound(null, v.x, v.y, v.z, acao.som(), SoundSource.HOSTILE,
				Diretor.volumePara(p, v.x, v.z, sutil ? 0.34F : 0.52F), 0.80F);
		if (!gastar(e, Familia.RUIDO, sutil ? 1.0 : 1.8, seg, rnd, 210, 420)) return null;
		Depuracao.log(p, seg, "RUIDO_RETORNO origemAcao=" + acao.tipo() + " idade=" + (seg - acao.seg()) + "s pos=" + pos(v));
		return new Resultado(v, 0.55, "RUIDO_RETORNO");
	}

	@Nullable
	private static Resultado trilhaInterrompida(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg, RandomSource rnd, boolean sutil) {
		if (!disponivel(e, Familia.VESTIGIO, sutil ? 1.2 : 2.0, seg)) return null;
		List<Rastro.Ponto> pts = rastroAoAlcance(p, e, seg, 8, 180, 5, 26);
		if (pts.size() < 3) return null;
		int qtd = Math.min(5, 3 + rnd.nextInt(3));
		int inicio = rnd.nextInt(Math.max(1, pts.size() - Math.min(qtd, pts.size()) + 1));
		Vec3 fim = null;
		for (int i = 0; i < qtd && inicio + i < pts.size(); i++) {
			Rastro.Ponto pt = pts.get(inicio + i);
			Vec3 v = new Vec3(pt.x(), pt.y() + 0.08, pt.z());
			fim = v;
			int atraso = i * 7;
			Agenda.agendar(level, atraso, () -> level.sendParticles(ParticleTypes.ASH, v.x, v.y, v.z, 5, 0.25, 0.03, 0.25, 0.001));
		}
		if (fim == null || !gastar(e, Familia.VESTIGIO, sutil ? 1.2 : 2.0, seg, rnd, 300, 540)) return null;
		Depuracao.log(p, seg, "TRILHA_INTERROMPIDA pontos=" + qtd + " fim=" + pos(fim) + " semCriatura=sim");
		return new Resultado(fim, 0.60, "TRILHA_INTERROMPIDA");
	}

	private static List<Animal> animaisProximos(ServerLevel level, ServerPlayer p) {
		return level.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(18.0, 7.0, 18.0), a -> a.isAlive());
	}

	private static List<BlockPos> tochasValidas(ServerLevel level, ServerPlayer p, EstadoJogador e, double raio) {
		List<BlockPos> lista = new ArrayList<>();
		for (BlockPos pos : e.tochas) {
			BlockState st = level.getBlockState(pos);
			if ((st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) && distancia(p, Vec3.atCenterOf(pos)) <= raio && !naTela(p, Vec3.atCenterOf(pos))) {
				lista.add(pos.immutable());
			}
		}
		return lista;
	}

	@Nullable
	private static BlockPos acharPontoParaTocha(ServerLevel level, ServerPlayer p, RandomSource rnd, int min, int max) {
		for (int i = 0; i < 16; i++) {
			Vec3 v = pontoRelativo(p, 70 + rnd.nextDouble() * 220, min + rnd.nextInt(Math.max(1, max - min + 1)));
			BlockPos chao = acharChao(level, BlockPos.containing(v.x, v.y, v.z));
			if (chao == null) continue;
			BlockPos pos = chao.above();
			if (!level.isEmptyBlock(pos) || naTela(p, Vec3.atCenterOf(pos))) continue;
			BlockState torch = Blocks.TORCH.defaultBlockState();
			if (torch.canSurvive(level, pos)) return pos;
		}
		return null;
	}

	@Nullable
	private static BlockPos acharChao(ServerLevel level, BlockPos perto) {
		for (int dy = 5; dy >= -12; dy--) {
			BlockPos base = perto.offset(0, dy, 0);
			if (!level.getBlockState(base).getCollisionShape(level, base).isEmpty() && level.isEmptyBlock(base.above())) {
				return base;
			}
		}
		return null;
	}

	private static Rastro.@Nullable Ponto escolherRastro(ServerPlayer p, EstadoJogador e, long seg, int idadeMin, int idadeMax,
			double distMin, double distMax, RandomSource rnd) {
		List<Rastro.Ponto> lista = rastroAoAlcance(p, e, seg, idadeMin, idadeMax, distMin, distMax);
		if (lista.isEmpty()) return null;
		return lista.get(rnd.nextInt(lista.size()));
	}

	/** Pontos do Rastro com a idade (s) e a distância horizontal (blocos) pedidas, do mais antigo para o mais novo. */
	private static List<Rastro.Ponto> rastroAoAlcance(ServerPlayer p, EstadoJogador e, long seg, int idadeMin, int idadeMax,
			double distMin, double distMax) {
		List<Rastro.Ponto> lista = e.rastro.comIdade(seg, idadeMin, idadeMax);
		lista.removeIf(pt -> {
			double d = Math.sqrt(distanciaSqr(p, pt.x(), pt.z()));
			return d < distMin || d > distMax;
		});
		return lista;
	}

	private static boolean disponivel(EstadoJogador e, Familia familia, double custo, long seg) {
		if (e.forcando) return true;
		return e.atmosfera.orcamento >= custo && seg >= e.atmosfera.cooldown.getOrDefault(familia, 0L);
	}

	private static boolean gastar(EstadoJogador e, Familia familia, double custo, long seg, RandomSource rnd,
			int cooldownMin, int cooldownMax) {
		if (e.forcando) return true;
		if (!disponivel(e, familia, custo, seg)) return false;
		e.atmosfera.orcamento = Math.max(0, e.atmosfera.orcamento - custo);
		e.atmosfera.cooldown.put(familia, seg + cooldownMin + rnd.nextInt(Math.max(1, cooldownMax - cooldownMin + 1)));
		return true;
	}

	private static void lembrar(EstadoJogador e, String chave) {
		e.atmosfera.recentes.addFirst(chave);
		while (e.atmosfera.recentes.size() > 5) e.atmosfera.recentes.removeLast();
	}

	private static boolean temCenaAtiva(EstadoJogador e) {
		return e.cena != EstadoJogador.Cena.NENHUMA
				|| e.cenaCasa != EstadoJogador.CenaCasa.NENHUMA
				|| e.cenaTunel != EstadoJogador.CenaTunel.NENHUMA
				|| e.cenaCampo != EstadoJogador.CenaCampo.NENHUMA
				|| e.cenaMarco != EstadoJogador.CenaMarco.NENHUMA
				|| e.cenaJanela != EstadoJogador.CenaJanela.NENHUMA;
	}

	private static Vec3 pontoRelativo(ServerPlayer p, double graus, double dist) {
		double a = Math.toRadians(p.getYRot() + graus);
		return new Vec3(p.getX() - Math.sin(a) * dist, p.getY(), p.getZ() + Math.cos(a) * dist);
	}

	private static boolean naTela(ServerPlayer p, Vec3 pos) {
		Vec3 olho = p.getEyePosition();
		Vec3 dir = pos.subtract(olho);
		if (dir.lengthSqr() < 0.001) return true;
		dir = dir.normalize();
		return p.getLookAngle().dot(dir) > 0.58; // cone largo (~55°) para não materializar na borda
	}

	private static double distancia(ServerPlayer p, Vec3 v) {
		return Math.sqrt(p.distanceToSqr(v));
	}

	private static double distanciaSqr(ServerPlayer p, double x, double z) {
		double dx = p.getX() - x;
		double dz = p.getZ() - z;
		return dx * dx + dz * dz;
	}

	private static String pos(Vec3 v) {
		return String.format(Locale.ROOT, "(%.0f,%.0f,%.0f)", v.x, v.y, v.z);
	}
}
