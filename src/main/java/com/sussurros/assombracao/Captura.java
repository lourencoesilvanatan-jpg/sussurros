package com.sussurros.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import com.sussurros.Sussurros;
import com.sussurros.assombracao.diretor.Agenda;
import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteEfeito;
import com.sussurros.rede.Rede;
import com.sussurros.registro.ModSons;

/**
 * O que acontece quando ele pega o jogador (0.9). Ele não mata.
 *
 * Morrer e renascer acaba com o medo; ser pego e nada acontecer também. Então a captura custa, é visível e
 * tem cura:
 *
 *  - tudo emudece e a tela fecha;
 *  - o que estava na mão fica caído onde ele foi pego (voltar para buscar é a segunda cena);
 *  - ele acorda a 20-40 blocos, num lugar escuro por onde já passou, com a vida reduzida (nunca abaixo de
 *    dois corações) e as luzes em volta apagadas;
 *  - fica a MARCA: um coração a menos de vida máxima por captura (até três), até dormir com uma vela acesa.
 *
 * Depois vem um descanso longo. Numa versão seguinte, parte das capturas leva ao Avesso em vez de deslocar.
 */
public final class Captura {
	private static final Identifier MARCA = Sussurros.id("marca");
	private static final int MARCAS_MAXIMAS = 3;
	/** Sorteios da captura: não usa o gerador do mundo. */
	private static final RandomSource SORTE = RandomSource.create();

	private Captura() {
	}

	static void executar(ServerLevel level, ServerPlayer p, HospedeEntity h) {
		boolean teste = h.ehTeste();
		long seg = level.getGameTime() / 20;
		Vec3 onde = p.position();

		Diretor.emudecer(level, p, 20, "CAPTURA");
		ModSons.tocarNaCabeca(p, ModSons.Som.APAGAO, 0.8F, 1.0F);
		if (Rede.temCliente(p)) {
			Rede.efeito(p, PacoteEfeito.Tipo.APAGAO, 60, 1.0F);
		} else {
			p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
		}
		// Preso no lugar enquanto a tela fecha.
		p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 6, false, false));

		ItemStack naMao = p.getMainHandItem();
		if (!naMao.isEmpty()) {
			ItemStack caido = p.getInventory().removeFromSelected(true);
			if (!caido.isEmpty()) {
				ItemEntity item = new ItemEntity(level, onde.x, onde.y + 0.2, onde.z, caido);
				item.setDeltaMovement(0, 0, 0);
				item.setUnlimitedLifetime(); // não some: é dele, e ele vai querer voltar para buscar
				item.setPickUpDelay(40);
				level.addFreshEntity(item);
			}
		}
		// O lugar fica marcado: o Olho e o Sino conseguem apontar de volta para onde ele foi pego.
		Vestigios.de(p).registrar(p.blockPosition(), Vestigios.Tipo.DESAPARECIMENTO, seg);

		Depuracao.log(p, seg, String.format(Locale.ROOT, "CAPTURA inicio pos=%s vida=%.0f teste=%s",
				Diretor.pos(onde.x, onde.y, onde.z), p.getHealth(), teste ? "sim" : "nao"));

		// O deslocamento acontece com a tela já preta.
		Agenda.agendar(level, 16, () -> {
			if (!p.isRemoved() && p.level() == level) {
				deslocar(level, p, teste);
			}
		});
	}

	private static void deslocar(ServerLevel level, ServerPlayer p, boolean teste) {
		EstadoJogador e = Diretor.estadoParaTeste(p);
		long tick = level.getGameTime();
		long seg = tick / 20;
		Vec3 de = p.position();
		// Em parte das capturas ele não é deslocado: acorda do outro lado, e volta depois para onde foi pego.
		boolean avesso = Avesso.sorteiaCaptura(level, p, teste);
		BlockPos destino = avesso ? null : escolherDestino(level, p, e, seg);
		if (destino != null) {
			p.stopRiding();
			p.teleportTo(level, destino.getX() + 0.5, destino.getY(), destino.getZ() + 0.5, Set.of(),
					SORTE.nextFloat() * 360.0F, 10.0F, true);
			ApoioCaca.apagarLuzPerto(level, p, destino, 8, 20 * 60, 8);
		}

		// Nunca mata, nunca deixa abaixo de dois corações. Quem já estava pior que isso não piora.
		float vida = p.getHealth();
		if (vida > 4.0F) {
			p.setHealth(Math.max(4.0F, vida - 8.0F));
		}

		if (!teste) {
			Memoria m = Memoria.de(p);
			m.add(Memoria.CAPTURAS, 1);
			m.set(Memoria.MARCAS, Math.min(MARCAS_MAXIMAS, m.get(Memoria.MARCAS) + 1));
			m.add(Memoria.INQUIETACAO, 40);
			m.limitar(Memoria.INQUIETACAO, 0, Memoria.MAX_INQUIETACAO);
			m.salvar();
			aplicarMarca(p, m.get(Memoria.MARCAS));
			Diretor.depoisDaCaptura(p);
		}

		Depuracao.log(p, seg, String.format(Locale.ROOT, "CAPTURA deslocou de=%s para=%s dist=%.0f vida=%.0f marcas=%d",
				Diretor.pos(de.x, de.y, de.z),
				destino == null ? "-" : Diretor.pos(destino.getX(), destino.getY(), destino.getZ()),
				destino == null ? 0.0 : Math.sqrt(destino.distToCenterSqr(de)), p.getHealth(),
				teste ? 0 : Memoria.de(p).get(Memoria.MARCAS)));

		if (avesso) {
			Avesso.atravessarNaCaptura(level, p, teste);
			return;
		}

		// Acorda devagar, três segundos depois, no silêncio.
		Agenda.agendar(level, 60, () -> {
			if (!p.isRemoved()) {
				Rede.efeito(p, PacoteEfeito.Tipo.ACORDAR, 60, 1.0F);
				ModSons.tocarNaCabeca(p, ModSons.Som.DESPERTAR, 0.6F, 1.0F);
			}
		});
	}

	/**
	 * Para onde ele é levado: de preferência um ponto do próprio Rastro (um lugar que ele reconhece), a
	 * 20-40 blocos, escuro. Sem isso, um chão qualquer nessa faixa. Sem isso também, ele fica onde está.
	 */
	@Nullable
	private static BlockPos escolherDestino(ServerLevel level, ServerPlayer p, EstadoJogador e, long seg) {
		BlockPos melhor = null;
		double melhorNota = -1;
		List<Rastro.Ponto> pontos = new ArrayList<>(e.rastro.comIdade(seg, 20, 100000));
		for (Rastro.Ponto pt : pontos) {
			double d = Math.sqrt(p.distanceToSqr(pt.x(), pt.y(), pt.z()));
			if (d < 20 || d > 40) {
				continue;
			}
			BlockPos chao = chaoParaJogador(level, pt.x(), pt.y(), pt.z());
			if (chao == null) {
				continue;
			}
			double nota = notaDoLugar(level, chao) + 1.0 + SORTE.nextDouble() * 0.5;
			if (nota > melhorNota) {
				melhorNota = nota;
				melhor = chao;
			}
		}
		if (melhor != null) {
			return melhor;
		}
		// Primeiro a faixa pedida (20 a 40 blocos); só se nada servir, mais perto (10 a 20).
		for (int faixa = 0; faixa < 2 && melhor == null; faixa++) {
			double min = faixa == 0 ? 20 : 10;
			double largura = faixa == 0 ? 20 : 10;
			for (int i = 0; i < 20; i++) {
				double ang = SORTE.nextDouble() * Math.PI * 2.0;
				double r = min + SORTE.nextDouble() * largura;
				BlockPos chao = chaoParaJogador(level, p.getX() + Math.cos(ang) * r, p.getY() + 3, p.getZ() + Math.sin(ang) * r);
				if (chao == null || Math.abs(chao.getY() - p.getY()) > 10) {
					continue;
				}
				double nota = notaDoLugar(level, chao) + SORTE.nextDouble() * 0.5;
				if (nota > melhorNota) {
					melhorNota = nota;
					melhor = chao;
				}
			}
		}
		return melhor;
	}

	/** Quanto mais escuro, melhor. */
	private static double notaDoLugar(ServerLevel level, BlockPos chao) {
		int luz = level.getBrightness(LightLayer.BLOCK, chao);
		int ceu = Diretor.ehNoite(level) ? 0 : level.getBrightness(LightLayer.SKY, chao);
		return 1.0 - Math.max(luz, ceu) / 15.0;
	}

	/** Chão firme, sem líquido, com dois blocos de ar em cima. */
	@Nullable
	private static BlockPos chaoParaJogador(ServerLevel level, double x, double yBase, double z) {
		for (int dy = 4; dy >= -8; dy--) {
			BlockPos pos = BlockPos.containing(x, yBase + dy, z);
			BlockPos baixo = pos.below();
			if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
					&& !level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty()
					&& level.getFluidState(baixo).isEmpty() && level.getFluidState(pos).isEmpty()) {
				return pos;
			}
		}
		return null;
	}

	/** Aplica (ou tira, com zero) a marca: um coração a menos de vida máxima por captura. */
	static void aplicarMarca(ServerPlayer p, int marcas) {
		AttributeInstance vida = p.getAttribute(Attributes.MAX_HEALTH);
		if (vida == null) {
			return;
		}
		if (marcas <= 0) {
			vida.removeModifier(MARCA);
			return;
		}
		vida.addOrReplacePermanentModifier(new AttributeModifier(MARCA,
				-2.0 * Math.min(MARCAS_MAXIMAS, marcas), AttributeModifier.Operation.ADD_VALUE));
		if (p.getHealth() > p.getMaxHealth()) {
			p.setHealth(p.getMaxHealth());
		}
	}

	/**
	 * A cura: dormir de verdade com uma vela acesa. Chamado quando o jogador acorda.
	 * Devolve true se a marca saiu.
	 */
	static boolean tentarCurar(ServerPlayer p, Memoria m, boolean dormiuDeVerdade) {
		if (m.get(Memoria.MARCAS) <= 0 || !dormiuDeVerdade || !Diretor.temZonaCalma(p)) {
			return false;
		}
		m.set(Memoria.MARCAS, 0);
		aplicarMarca(p, 0);
		ModSons.tocarNaCabeca(p, ModSons.Som.DESPERTAR, 0.35F, 0.8F);
		Depuracao.log(p, p.level().getGameTime() / 20, "MARCA curada (dormiu com a vela acesa)");
		return true;
	}

	/** Depois de morrer e renascer os atributos voltam ao padrão; a marca tem de ser reposta. */
	static void repor(ServerPlayer p) {
		aplicarMarca(p, Memoria.de(p).get(Memoria.MARCAS));
	}
}
