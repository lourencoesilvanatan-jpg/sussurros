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
 * não o estado: quem já corria e continuou correndo não "reagiu".
 */
final class Leitura {
	private static final int TAMANHO_ANEL = 12;
	private static final int DURACAO_JANELA = 60; // ticks

	record Amostra(double vel, float yaw, boolean correndo, boolean agachado, double x, double z, long tick) {
	}

	/**
	 * percebeu: a própria reação prova que o jogador notou (virou para a fonte ou foi até ela).
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
	private double observabilidade;
	private String origem = "";
	private long fimTick;
	private double baseVel;
	private double baseGiro;
	private double baseCorrendo;
	private double baseAgachado;

	void amostrar(ServerPlayer p, long tick) {
		double vel = 0;
		if (this.ultima != null) {
			double dx = p.getX() - this.ultima.x();
			double dz = p.getZ() - this.ultima.z();
			double dt = (tick - this.ultima.tick()) / 20.0;
			if (dt > 0 && dt < 2) {
				vel = Math.sqrt(dx * dx + dz * dz) / dt;
			}
		}
		Amostra a = new Amostra(vel, p.getYRot(), p.isSprinting(), p.isCrouching(), p.getX(), p.getZ(), tick);
		this.ultima = a;
		this.anel.addLast(a);
		while (this.anel.size() > TAMANHO_ANEL) {
			this.anel.removeFirst();
		}
		if (this.evento != null) {
			this.janela.add(a);
		}
	}

	boolean pendente() {
		return this.evento != null;
	}

	boolean pronta(long tick) {
		return this.evento != null && tick >= this.fimTick;
	}

	void cancelar() {
		this.evento = null;
		this.janela.clear();
	}

	void iniciar(Evento ev, @Nullable Vec3 fonte, double observabilidade, long tickAgora, String origem) {
		this.evento = ev;
		this.fonte = fonte;
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
	}

	Resultado avaliar() {
		Evento ev = this.evento;
		Vec3 f = this.fonte;
		List<Amostra> j = new ArrayList<>(this.janela);
		double obs = this.observabilidade;
		String org = this.origem;
		cancelar();

		if (ev == null || j.size() < 2) {
			return new Resultado(ev == null ? Evento.PASSOS : ev, f, 0, org, 0, 0, false, false, false, true, "sem dados");
		}

		// Giro súbito (maior giro entre duas amostras seguidas)
		double giroMax = 0;
		for (int i = 1; i < j.size(); i++) {
			giroMax = Math.max(giroMax, Math.abs(diferencaAngulo(j.get(i).yaw(), j.get(i - 1).yaw())));
		}
		double velJ = mediaVel(j);

		// v0.4.1: parado antes E depois, sem mexer a câmera = provavelmente chat/menu/AFK.
		// Isso não é "não reagiu": é falta de dados. Não pode ensinar nada ao Diretor.
		if (this.baseVel < 0.05 && this.baseGiro < 1.0 && velJ < 0.05 && giroMax < 3.0) {
			return new Resultado(ev, f, 0, org, 0, 0, false, false, false, true,
					"parado antes e depois (chat/menu/AFK?): sem dados");
		}

		double sGiro = (giroMax >= 60 && giroMax >= 3 * this.baseGiro) ? 0.35 : 0;

		// A fonte tem direção? (sussurros na tela não têm)
		Amostra primeira = j.get(0);
		Amostra ultimaJ = j.get(j.size() - 1);
		boolean temDirecao = f != null && distH(primeira.x(), primeira.z(), f.x, f.z) > 1.0;

		boolean olhou = false;
		if (temDirecao) {
			for (Amostra a : j) {
				double[] dir = direcao(a.x(), a.z(), f.x, f.z);
				double[] vis = visao(a.yaw());
				if (dir[0] * vis[0] + dir[1] * vis[1] > 0.8) {
					olhou = true;
					break;
				}
			}
		}
		double sOlhou = (sGiro > 0 && olhou) ? 0.20 : 0;

		double sCongelou = (this.baseVel > 1.0 && velJ < 0.25) ? 0.50 : 0;

		boolean comecouCorrer = this.baseCorrendo < 0.3 && fracao(j, true) > 0.6;
		double sFugiu = (velJ - this.baseVel > 1.5 || comecouCorrer) ? 0.40 : 0;

		double mx = ultimaJ.x() - primeira.x();
		double mz = ultimaJ.z() - primeira.z();
		double mov = Math.sqrt(mx * mx + mz * mz);
		double rumo = 0;
		if (temDirecao && mov > 0.01) {
			double[] dir = direcao(primeira.x(), primeira.z(), f.x, f.z);
			rumo = (mx / mov) * dir[0] + (mz / mov) * dir[1];
		}
		double sFugiuDaFonte = (sFugiu > 0 && mov > 2 && rumo < -0.5) ? 0.20 : 0;

		double sAgachou = (this.baseAgachado < 0.2 && fracao(j, false) > 0.6) ? 0.30 : 0;

		boolean investigou = temDirecao && mov > 3 && rumo > 0.5;
		double sInvestigou = investigou ? 0.15 : 0;

		double naoReagiu = 1;
		for (double s : new double[] {sGiro, sOlhou, sCongelou, sFugiu, sFugiuDaFonte, sAgachou, sInvestigou}) {
			naoReagiu *= (1 - s);
		}
		double c = 1 - naoReagiu;
		boolean fugiu = sFugiu > 0;
		double eng = investigou ? 1.0 : ((olhou && !fugiu) ? 0.5 : 0.0);
		boolean percebeu = investigou || (sGiro > 0 && olhou);

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
