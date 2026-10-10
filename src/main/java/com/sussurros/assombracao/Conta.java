package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.sussurros.bloco.LampiaoPalidoBlock;
import com.sussurros.registro.ModSons;

/**
 * A Conta (0.9): a dívida escondida que une todos os itens.
 *
 * Cada uso de um item ligado à criatura soma um pouco. Sem usar nada, a conta cai 1 a cada dez minutos.
 * O limite é sorteado a cada ciclo (6 a 10), para ninguém decorar. Antes de cobrar vêm três avisos sem
 * texto, um por degrau; ao estourar, ele cobra na moeda do item em que o jogador mais se apoiou, e a conta
 * zera. A cobrança nunca mata e nunca estraga nada.
 *
 * É isto que deixa os itens serem bons de verdade: o preço não está em cada uso, está em se apoiar demais.
 *
 * 0.9.0-alpha14: a regra do monstro continua escondida (o limite, quanto cada item pesa), mas a ligação
 * entre causa e efeito deixou de ser. Na primeira sessão de verdade, doze das vinte e sete coisas que o
 * jogador percebeu vieram daqui, e ele as ouviu como ruído. O que mudou:
 *   - carência: os três primeiros usos de cada item, na vida do personagem, não somam;
 *   - marcador: todo uso que soma faz o mesmo som baixo, na hora (um risco de giz). A primeira vez que ele
 *     soa é a virada: dali em diante aquele item conta;
 *   - a cobrança é sempre no próprio item, no uso seguinte, e não mais meio minuto ou dois depois;
 *   - recibo: depois de cobrado, o Caderno ganha uma linha contando o que aconteceu.
 *
 * Tudo aqui fica na Memoria (salva com o personagem). Uso forçado por comando de teste não soma.
 */
public final class Conta {
	public enum Item {
		VELA(1), SINO(1), FIO(1), ISCA(2), OLHO(3), CAIXA(2), OSSOS(1), LINHA(1), LAMPIAO(1);

		final int peso;

		Item(int peso) {
			this.peso = peso;
		}

		String chave() {
			return "conta_" + this.name().toLowerCase(Locale.ROOT);
		}

		/** Marca "a próxima vez que ele usar este item, é cobrança". */
		String chaveCobrado() {
			return "cobrado_" + this.name().toLowerCase(Locale.ROOT);
		}

		/** Quantas vezes ele usou este item na vida do personagem (não zera com a cobrança). */
		String chaveVida() {
			return "conta_vida_" + this.name().toLowerCase(Locale.ROOT);
		}

		/** Quantas vezes este item já foi cobrado: é o que o Caderno conta. */
		String chaveRecibo() {
			return "recibo_" + this.name().toLowerCase(Locale.ROOT);
		}
	}

	static final String TOTAL = "conta";
	static final String LIMITE = "conta_limite";
	static final String AVISOS = "conta_avisos";
	static final String DECAIU_EM = "conta_decaiu_em";
	/** Quantas cobranças já aconteceram, somando todos os itens. */
	static final String RECIBOS = "recibos";

	/** A cada tantos segundos sem estourar, a conta perde 1. */
	private static final int DECAI_A_CADA = 600;
	/** Os primeiros usos de cada item não somam: o jogador aprende o item antes de pagar por ele. */
	public static final int CARENCIA = 3;

	private Conta() {
	}

	/** Um uso de item. Abre e salva a Memoria: chamar só de fora do tick do Diretor (uso de item, clique em bloco). */
	public static void somar(ServerPlayer p, Item item) {
		Memoria m = Memoria.de(p);
		somar(p, m, item, 1);
		m.salvar();
	}

	/**
	 * Um uso de item feito de fora do tick do Diretor (um bloco clicado, um item usado no chão): confere se
	 * este uso é a cobrança, soma e salva. Devolve true se era a cobrança: quem chama mostra a cara dela.
	 */
	public static boolean usar(ServerPlayer p, Item item) {
		Memoria m = Memoria.de(p);
		boolean cobrado = cobrarNoUso(p, m, item);
		somar(p, m, item, 1);
		m.salvar();
		return cobrado;
	}

	/** O mesmo, para quem já tem a Memoria aberta e vai salvá-la. Cada chamada é um uso; "vezes" é só o peso. */
	static void somar(ServerPlayer p, Memoria m, Item item, int vezes) {
		long seg = p.level().getGameTime() / 20;
		if (m.get(LIMITE) == 0) {
			m.set(LIMITE, 6 + p.getRandom().nextInt(5));
		}
		int usosNaVida = m.get(item.chaveVida());
		m.add(item.chaveVida(), 1);
		if (usosNaVida < CARENCIA) {
			Depuracao.log(p, seg, String.format(Locale.ROOT, "CONTA carencia item=%s uso=%d/%d", item, usosNaVida + 1, CARENCIA));
			return;
		}
		if (m.get(TOTAL) <= 0) {
			// O relógio de esfriar só anda com a conta acima de zero. Sem isto ele ficava velho, e o primeiro uso
			// depois de um tempo parado era apagado no segundo seguinte.
			m.set(DECAIU_EM, (int) seg);
		}
		m.add(TOTAL, item.peso * vezes);
		m.add(item.chave(), vezes);
		// O marcador: o mesmo som, baixo, em todo uso que soma, no instante do uso. Não explica nada; só deixa
		// o jogador ligar "usei" a "alguma coisa anotou". Sem ele, a cobrança no uso seguinte seria atribuída
		// só a esse uso. É o som de giz na pedra, que já existia no mod e nunca tinha tocado.
		ModSons.tocarNaCabeca(p, ModSons.Som.GIZ, 0.30F, 0.92F);
		Depuracao.log(p, seg, String.format(Locale.ROOT, "CONTA +%d item=%s total=%d limite=%d",
				item.peso * vezes, item, m.get(TOTAL), m.get(LIMITE)));
	}

	/**
	 * O jogador vai usar este item agora: há cobrança pendente nele? Consome a marca e anota o recibo.
	 * Quem chama é que mostra a cobrança, no próprio item (a vela que dura a metade, o sino que toca sozinho).
	 */
	static boolean cobrarNoUso(ServerPlayer p, Memoria m, Item item) {
		if (m.get(item.chaveCobrado()) == 0) {
			return false;
		}
		m.set(item.chaveCobrado(), 0);
		m.add(item.chaveRecibo(), 1);
		m.add(RECIBOS, 1);
		if (m.get(RECIBOS) == 1) {
			// A primeira vez que alguma coisa volta: o caderno, que é onde isso fica anotado, passa a ter receita.
			Ensino.aoCobrar(p);
		}
		Depuracao.log(p, p.level().getGameTime() / 20, "CONTA cobrada no uso item=" + item + " recibos=" + m.get(RECIBOS));
		return true;
	}

	/** Quantas vezes este item já foi cobrado (para o Caderno). */
	static int recibos(Memoria m, Item item) {
		return m.get(item.chaveRecibo());
	}

	/** Uma vez por segundo, dentro do tick do Diretor (recebe a Memoria do tick). */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick) {
		int total = m.get(TOTAL);
		if (total <= 0) {
			return;
		}
		int limite = m.get(LIMITE, 8);

		// Sem estourar, esfria devagar.
		if (m.get(DECAIU_EM) == 0 || seg < m.get(DECAIU_EM)) {
			m.set(DECAIU_EM, (int) seg);
		} else if (seg - m.get(DECAIU_EM) >= DECAI_A_CADA) {
			m.set(DECAIU_EM, (int) seg);
			m.set(TOTAL, total - 1);
			Depuracao.log(p, seg, "CONTA esfriou total=" + (total - 1) + " limite=" + limite);
			return;
		}

		// Os três avisos, um por degrau antes do limite.
		int degrau = total >= limite - 1 ? 3 : total >= limite - 2 ? 2 : total >= limite - 3 ? 1 : 0;
		// Os avisos não passam pelo orçamento de atenção: são consequência do que o próprio jogador fez com os
		// itens. (Na alpha12 passavam, e quem usava muito item gastava com avisos o saldo dos eventos.)
		if (degrau > m.get(AVISOS) && !Diretor.bloqueado(p, e, tick)) {
			m.set(AVISOS, degrau);
			avisar(level, p, m, e, degrau, seg, tick);
		}

		if (total >= limite) {
			// Estourou. A cobrança fica marcada no item em que ele mais se apoiou e acontece no próximo uso dele,
			// no próprio item. (Até a alpha13 vinha solta, de meio minuto a dois depois, "quando ele já tiver
			// esquecido o que fez": para quem joga, era um som sem dono.)
			Item item = maisUsado(m);
			m.set(item.chaveCobrado(), 1);
			Depuracao.log(p, seg, "CONTA estourou total=" + total + " limite=" + limite + " cobraNoProximoUso=" + item);
			m.set(TOTAL, 0);
			m.set(AVISOS, 0);
			m.set(LIMITE, 6 + p.getRandom().nextInt(5));
			m.set(DECAIU_EM, (int) seg);
			for (Item i : Item.values()) {
				m.set(i.chave(), 0);
			}
		}
	}

	private static void avisar(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, int degrau, long seg, long tick) {
		RandomSource sorte = p.getRandom();
		String como;
		switch (degrau) {
			case 1 -> {
				// As chamas encolhem: os lampiões por perto ficam inquietos, e uma ou duas tochas falham.
				int lampioes = LampiaoPalidoBlock.perturbar(level, p.blockPosition(), 16, LampiaoPalidoBlock.Chama.INQUIETA, 120);
				int tochas = ApoioCaca.apagarLuzPerto(level, p, p.blockPosition(), 8, 50 + sorte.nextInt(30), 2);
				como = "CHAMAS lampioes=" + lampioes + " tochas=" + tochas;
			}
			case 2 -> {
				// A cantiga, com notas faltando. Para quem ainda não tem a caixa, um sino longe.
				Vec3 ponto = Diretor.pontoRelativo(p, 150 + sorte.nextDouble() * 60, 14);
				boolean conhece = m.get(Memoria.CAIXA_USOS) > 0;
				ModSons.Som som = conhece ? ModSons.Som.CAIXA_QUEBRADA : ModSons.Som.SINO_LONGE;
				ModSons.tocarPara(p, ponto.x, p.getY() + 1.0, ponto.z, som,
						Diretor.volumePara(p, ponto.x, p.getY() + 1.0, ponto.z, 0.6F), conhece ? 0.9F : 1.0F);
				como = conhece ? "CANTIGA_FALHADA" : "SINO";
			}
			default -> {
				// O último: um zumbido no ouvido e a sensação de que há alguém.
				ModSons.tocarNaCabeca(p, ModSons.Som.ZUMBIDO, 0.7F, 1.0F);
				e.vigiaFalsaAte = tick + 20L * 20;
				e.vigiaFalsaForca = 0.7F;
				como = "ZUMBIDO";
			}
		}
		Depuracao.log(p, seg, "CONTA aviso=" + degrau + "/3 como=" + como + " total=" + m.get(TOTAL) + " limite=" + m.get(LIMITE));
	}

	private static Item maisUsado(Memoria m) {
		Item maior = Item.VELA;
		int usos = -1;
		for (Item item : Item.values()) {
			int n = m.get(item.chave()) * item.peso;
			if (n > usos) {
				usos = n;
				maior = item;
			}
		}
		return maior;
	}
}
