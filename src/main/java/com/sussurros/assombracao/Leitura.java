package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Lê o comportamento do jogador antes e depois de um acontecimento.
 *
 * Mantém um anel com os últimos 3 s (uma amostra a cada 5 ticks). Quando algo acontece,
 * congela esse "baseline" e observa a janela dos 3 s seguintes. A reação é a MUDANÇA,
 * não o estado: quem já corria e continuou correndo não "reagiu", e quem já andava na
 * direção da fonte e continuou andando não "investigou".
 */
final class Leitura {
	private static final int TAMANHO_ANEL = 12;
	private static final int DURACAO_JANELA = 60; // ticks

	/**
	 * Giro (graus entre duas amostras seguidas) em que a reação começa a contar e em que passa a valer o
	 * máximo. Era um degrau em 60°: virar 58° e olhar para a fonte valia zero.
	 */
	private static final double GIRO_MINIMO = 30;
	private static final double GIRO_CHEIO = 60;

	/** Mais que isto entre duas amostras (0,25 s) não é movimento: é teleporte, respawn ou portal. */
	private static final double SALTO_BLOCOS = 24;

	/** A partir deste intervalo sem amostras (s), o anel já não descreve "os últimos 3 s". */
	private static final double BURACO_SEGUNDOS = 2;

	record Amostra(double vel, float yaw, boolean correndo, boolean agachado, double x, double z, long tick) {
	}

	/**
	 * percebeu: a própria reação prova que o jogador notou (virou e ficou olhando para a fonte).
	 * semDados: o jogador estava totalmente parado antes e depois (chat, menu, AFK): não dá para ler nada.
	 */
	record Resultado(Evento evento, @Nullable Vec3 fonte, double observabilidade, String origem,
			double confianca, double engajamento, boolean fugiu, boolean investigou, boolean percebeu,
			boolean semDados, String sinais) {
	}

	private final ArrayDeque<Amostra> anel = new ArrayDeque<>();
	private final List<Amostra> janela = new ArrayList<>();
	@Nullable private Amostra ultima;

	// Leitura pendente
	@Nullable private Evento evento;
	@Nullable private Vec3 fonte;
	private boolean temFonte;
	private double fonteX;
	private double fonteZ;
	private double observabilidade;
	private String origem = "";
	private long fimTick;
	private double baseVel;
	private double baseGiro;
	private double baseCorrendo;
	private double baseAgachado;
	/** Ele já vinha andando na direção da fonte antes do acontecimento? Então continuar não é "investigar". */
	private boolean baseIndoParaFonte;

	/**
	 * Guarda uma amostra. Devolve true se houve um salto (teleporte, respawn, volta de outra dimensão):
	 * a leitura em andamento é cancelada aqui, e o chamador deve esquecer o Rastro.
	 */
	boolean amostrar(ServerPlayer p, long tick) {
		return this.amostrar(p.getX(), p.getZ(), p.getYRot(), p.isSprinting(), p.isCrouching(), tick);
	}

	/** Mesma coisa, sem depender do jogador (é o que os testes usam). */
	boolean amostrar(double x, double z, float yaw, boolean correndo, boolean agachado, long tick) {
		double vel = 0;
		boolean salto = false;
		if (this.ultima != null) {
			double dx = x - this.ultima.x();
			double dz = z - this.ultima.z();
			double dist = Math.sqrt(dx * dx + dz * dz);
			double dt = (tick - this.ultima.tick()) / 20.0;
			if (dist > SALTO_BLOCOS || dt >= BURACO_SEGUNDOS) {
				salto = true;
			} else if (dt > 0) {
				vel = dist / dt;
			}
		}
		if (salto) {
			// O que veio antes não descreve mais o jogador: nem o baseline, nem uma leitura em andamento.
			this.anel.clear();
			this.cancelar();
		}
		Amostra a = new Amostra(vel, yaw, correndo, agachado, x, z, tick);
		this.ultima = a;
		this.anel.addLast(a);
		while (this.anel.size() > TAMANHO_ANEL) {
			this.anel.removeFirst();
		}
		if (this.evento != null) {
			this.janela.add(a);
		}
		return salto;
	}

	boolean pendente() {
		return this.evento != null;
	}

	boolean pronta(long tick) {
		return this.evento != null && tick >= this.fimTick;
	}

	void cancelar() {
		this.evento = null;
		this.fonte = null;
		this.janela.clear();
	}

	void iniciar(Evento ev, @Nullable Vec3 fonte, double observabilidade, long tickAgora, String origem) {
		if (fonte == null) {
			this.iniciar(ev, false, 0, 0, observabilidade, tickAgora, origem);
		} else {
			this.iniciar(ev, true, fonte.x, fonte.z, observabilidade, tickAgora, origem);
		}
		this.fonte = fonte;
	}

	/** Mesma coisa, com a fonte em coordenadas (é o que os testes usam). */
	void iniciar(Evento ev, boolean temFonte, double fonteX, double fonteZ, double observabilidade, long tickAgora, String origem) {
		this.evento = ev;
		this.fonte = null;
		this.temFonte = temFonte;
		this.fonteX = fonteX;
		this.fonteZ = fonteZ;
		this.observabilidade = observabilidade;
		this.origem = origem;
		this.fimTick = tickAgora + DURACAO_JANELA;
		this.janela.clear();

		// Baseline: como ele estava nos 3 s anteriores.
		List<Amostra> base = new ArrayList<>(this.anel);
		this.baseVel = mediaVel(base);
		this.baseCorrendo = fracao(base, true);
		this.baseAgachado = fracao(base, false);
		double giros = 0;
		for (int i = 1; i < base.size(); i++) {
			giros += Math.abs(diferencaAngulo(base.get(i).yaw(), base.get(i - 1).yaw()));
		}
		this.baseGiro = base.size() > 1 ? giros / (base.size() - 1) : 0;

		// Para onde ele já estava indo em relação à fonte.
		this.baseIndoParaFonte = false;
		if (temFonte && base.size() >= 2) {
			Amostra b0 = base.get(0);
			Amostra b1 = base.get(base.size() - 1);
			double bx = b1.x() - b0.x();
			double bz = b1.z() - b0.z();
			double bmov = Math.sqrt(bx * bx + bz * bz);
			if (bmov > 1.0) {
				double[] dir = direcao(b1.x(), b1.z(), fonteX, fonteZ);
				this.baseIndoParaFonte = (bx / bmov) * dir[0] + (bz / bmov) * dir[1] > 0.3;
			}
		}
	}

	Resultado avaliar() {
		Evento ev = this.evento;
		Vec3 f = this.fonte;
		boolean temFonteEv = this.temFonte;
		double fx = this.fonteX;
		double fz = this.fonteZ;
		List<Amostra> j = new ArrayList<>(this.janela);
		double obs = this.observabilidade;
		String org = this.origem;
		cancelar();

		if (ev == null || j.size() < 2) {
			return new Resultado(ev == null ? Evento.PASSOS : ev, f, 0, org, 0, 0, false, false, false, true, "sem dados");
		}

		// Giro súbito: o maior giro entre duas amostras seguidas, e em qual amostra ele terminou.
		double giroMax = 0;
		int iGiro = 0;
		for (int i = 1; i < j.size(); i++) {
			double g = Math.abs(diferencaAngulo(j.get(i).yaw(), j.get(i - 1).yaw()));
			if (g > giroMax) {
				giroMax = g;
				iGiro = i;
			}
		}
		double velJ = mediaVel(j);

		// v0.4.1: parado antes E depois, sem mexer a câmera = provavelmente chat/menu/AFK.
		// Isso não é "não reagiu": é falta de dados. Não pode ensinar nada ao Diretor.
		if (this.baseVel < 0.05 && this.baseGiro < 1.0 && velJ < 0.05 && giroMax < 3.0) {
			return new Resultado(ev, f, 0, org, 0, 0, false, false, false, true,
					"parado antes e depois (chat/menu/AFK?): sem dados");
		}

		// Rampa em vez de degrau: abaixo de 30° não conta, de 60° para cima vale o máximo.
		// Continua exigindo que seja bem maior que o giro normal dele (quem vive girando a câmera não "reagiu").
		double fatorGiro = 0;
		if (giroMax >= 3 * this.baseGiro) {
			fatorGiro = Math.max(0, Math.min(1, (giroMax - GIRO_MINIMO) / (GIRO_CHEIO - GIRO_MINIMO)));
		}
		double sGiro = 0.35 * fatorGiro;

		// A fonte tem direção? (sussurros na tela não têm)
		Amostra primeira = j.get(0);
		Amostra ultimaJ = j.get(j.size() - 1);
		boolean temDirecao = temFonteEv && distH(primeira.x(), primeira.z(), fx, fz) > 1.0;

		// "Olhou" = depois do giro, ficou de frente para a fonte. Já estar virado para lá por acaso não conta.
		boolean olhou = false;
		if (temDirecao && fatorGiro > 0) {
			for (int i = iGiro; i < j.size(); i++) {
				Amostra a = j.get(i);
				double[] dir = direcao(a.x(), a.z(), fx, fz);
				double[] vis = visao(a.yaw());
				if (dir[0] * vis[0] + dir[1] * vis[1] > 0.8) {
					olhou = true;
					break;
				}
			}
		}
		double sOlhou = olhou ? 0.20 * fatorGiro : 0;

		double sCongelou = (this.baseVel > 1.0 && velJ < 0.25) ? 0.50 : 0;

		// Para onde ele foi durante a janela, em relação à fonte (1 = direto para ela, -1 = direto para longe).
		double mx = ultimaJ.x() - primeira.x();
		double mz = ultimaJ.z() - primeira.z();
		double mov = Math.sqrt(mx * mx + mz * mz);
		double rumo = 0;
		if (temDirecao && mov > 0.01) {
			double[] dir = direcao(primeira.x(), primeira.z(), fx, fz);
			rumo = (mx / mov) * dir[0] + (mz / mov) * dir[1];
		}
		boolean deslocou = temDirecao && mov > 2;

		// Fuga: acelerou ou começou a correr. Mas acelerar NA DIREÇÃO da fonte não é fuga, e acelerar sem se
		// afastar dela é evidência fraca (muita gente liga a corrida o tempo todo).
		boolean comecouCorrer = this.baseCorrendo < 0.3 && fracao(j, true) > 0.6;
		boolean acelerou = velJ - this.baseVel > 1.5;
		boolean fugiu = (acelerou || comecouCorrer) && !(deslocou && rumo > 0.3);
		boolean afastou = deslocou && rumo < -0.2;
		double sFugiu = !fugiu ? 0 : ((!temDirecao || afastou) ? 0.40 : 0.25);
		double sFugiuDaFonte = (fugiu && deslocou && rumo < -0.5) ? 0.20 : 0;

		double sAgachou = (this.baseAgachado < 0.2 && fracao(j, false) > 0.6) ? 0.30 : 0;

		// Investigar é MUDAR de rumo para a fonte. Quem já vinha andando para lá só continuou o caminho
		// (muitas fontes ficam no próprio Rastro: voltar por onde veio não é investigar).
		boolean investigou = temDirecao && mov > 3 && rumo > 0.5 && !this.baseIndoParaFonte;
		double sInvestigou = investigou ? 0.15 : 0;

		double naoReagiu = 1;
		for (double s : new double[] {sGiro, sOlhou, sCongelou, sFugiu, sFugiuDaFonte, sAgachou, sInvestigou}) {
			naoReagiu *= (1 - s);
		}
		double c = 1 - naoReagiu;
		double eng = investigou ? 1.0 : ((olhou && !fugiu) ? 0.5 : 0.0);
		// Só virar de verdade (45° ou mais) e ficar olhando para a fonte prova que ele percebeu.
		boolean percebeu = olhou && fatorGiro >= 0.5;

		String sinais = String.format(Locale.ROOT,
				"giro=%.0f(base %.0f) olhou=%s congelou=%s fugiu=%s daFonte=%s agachou=%s investigou=%s vel %.1f->%.1f",
				giroMax, this.baseGiro, olhou, sCongelou > 0, fugiu, sFugiuDaFonte > 0, sAgachou > 0, investigou, this.baseVel, velJ);

		return new Resultado(ev, f, obs, org, c, eng, fugiu, investigou, percebeu, false, sinais);
	}

	// ===== utilidades =====

	private static double mediaVel(List<Amostra> lista) {
		if (lista.isEmpty()) {
			return 0;
		}
		double soma = 0;
		for (Amostra a : lista) {
			soma += a.vel();
		}
		return soma / lista.size();
	}

	private static double fracao(List<Amostra> lista, boolean correndo) {
		if (lista.isEmpty()) {
			return 0;
		}
		int n = 0;
		for (Amostra a : lista) {
			if (correndo ? a.correndo() : a.agachado()) {
				n++;
			}
		}
		return (double) n / lista.size();
	}

	private static double distH(double x1, double z1, double x2, double z2) {
		double dx = x2 - x1;
		double dz = z2 - z1;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static double[] direcao(double x1, double z1, double x2, double z2) {
		double d = distH(x1, z1, x2, z2);
		if (d < 1.0E-4) {
			return new double[] {0, 0};
		}
		return new double[] {(x2 - x1) / d, (z2 - z1) / d};
	}

	/** Vetor horizontal do olhar a partir do yaw (no Minecraft, yaw 0 olha para +Z). */
	private static double[] visao(float yaw) {
		double r = Math.toRadians(yaw);
		return new double[] {-Math.sin(r), Math.cos(r)};
	}

	static float diferencaAngulo(float a, float b) {
		float d = (a - b) % 360.0F;
		if (d >= 180.0F) {
			d -= 360.0F;
		}
		if (d < -180.0F) {
			d += 360.0F;
		}
		return d;
	}
}
