package com.sussurros.assombracao;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.registro.ModSons;

/**
 * A cantiga da Caixa de Música (0.9): o tema do mod, apresentado como um objeto do jogador.
 *
 * Enquanto toca (uns vinte segundos, no lugar onde ele deu corda):
 *  - a inquietação de quem está por perto cai;
 *  - se o Hóspede estiver a até 20 blocos da caixa, algo cantarola junto, sem direção: confirma que ele
 *    está por ali, não onde;
 *  - na caçada, ele vai até a música em vez de ir até o jogador. A isca funciona cada vez menos na mesma
 *    caçada (quem decide é a Cacada).
 *
 * O preço: cada vez que toca, ele aprende. Com 3 usos a caixa passa a soar gasta e ele já sabe a cantiga
 * (o evento CANTIGA e o assobio do aviso passam a existir). Com 6, soa arruinada.
 */
public final class Cantiga {
	private static final int ALCANCE_CALMA = 10;
	private static final int ALCANCE_CANTAROLAR = 20;

	private Cantiga() {
	}

	/** O jogador deu corda. Devolve false se a caixa dele ainda está tocando. */
	public static boolean darCorda(ServerPlayer p) {
		ServerLevel level = p.level();
		EstadoJogador e = Diretor.estadoParaTeste(p);
		long tick = level.getGameTime();
		if (tick < e.caixaAteTick) {
			return false;
		}
		Memoria m = Memoria.de(p);
		int usos = m.get(Memoria.CAIXA_USOS);
		ModSons.Som som = usos >= 6 ? ModSons.Som.CAIXA_ARRUINADA : usos >= Diretor.CANTIGA_APRENDIDA ? ModSons.Som.CAIXA_GASTA : ModSons.Som.CAIXA_MUSICA;
		int duracao = som == ModSons.Som.CAIXA_ARRUINADA ? 545 : som == ModSons.Som.CAIXA_GASTA ? 410 : 400;

		e.caixaX = p.getX();
		e.caixaY = p.getY() + 0.5;
		e.caixaZ = p.getZ();
		e.caixaDesdeTick = tick;
		e.caixaAteTick = tick + duracao;
		e.caixaCantarolou = false;
		// É um objeto de verdade tocando: quem estiver perto ouve. Alcance de uns 20 blocos.
		ModSons.tocar(level, e.caixaX, e.caixaY, e.caixaZ, som, 1.25F, 1.0F);

		m.add(Memoria.CAIXA_USOS, 1);
		Conta.somar(p, m, Conta.Item.CAIXA, 1);
		m.salvar();
		Depuracao.log(p, tick / 20, String.format(Locale.ROOT, "CAIXA tocou usos=%d som=%s pos=%s",
				usos + 1, som, Diretor.pos(e.caixaX, e.caixaY, e.caixaZ)));
		return true;
	}

	public static void aCordaArrebentou(ServerPlayer p) {
		ModSons.tocar(p.level(), p.getX(), p.getY() + 1.0, p.getZ(), ModSons.Som.CAIXA_QUEBRADA, 0.9F, 1.0F);
		Depuracao.log(p, p.level().getGameTime() / 20, "CAIXA quebrou");
	}

	/** A caixa deste jogador está tocando agora? */
	static boolean tocando(EstadoJogador e, long tick) {
		return tick < e.caixaAteTick;
	}

	/** Uma vez por segundo, dentro do tick do Diretor. */
	static void segundo(ServerLevel level, ServerPlayer p, Memoria m, EstadoJogador e, long seg, long tick) {
		if (!tocando(e, tick)) {
			return;
		}
		Vec3 caixa = new Vec3(e.caixaX, e.caixaY, e.caixaZ);
		if (p.distanceToSqr(caixa) <= ALCANCE_CALMA * ALCANCE_CALMA) {
			m.add(Memoria.INQUIETACAO, -3);
			m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
		}
		HospedeEntity h = e.criatura;
		if (h == null || h.isRemoved()) {
			return;
		}
		// Depois de uns segundos de música, ele acompanha. Só o dono da caixa ouve, e não sabe de onde vem.
		if (!e.caixaCantarolou && tick - e.caixaDesdeTick >= 100
				&& h.distanceToSqr(caixa) <= ALCANCE_CANTAROLAR * ALCANCE_CANTAROLAR) {
			e.caixaCantarolou = true;
			ModSons.tocarNaCabeca(p, ModSons.Som.CANTAROLAR, 0.6F, 1.0F);
			Depuracao.log(p, seg, String.format(Locale.ROOT, "CAIXA ele cantarolou manifestacao=%s distDaCaixa=%.0f",
					h.getIdManifestacao(), Math.sqrt(h.distanceToSqr(caixa))));
		}
		if (seg % 2 == 0) {
			h.ouvirIsca(caixa);
		}
	}
}
