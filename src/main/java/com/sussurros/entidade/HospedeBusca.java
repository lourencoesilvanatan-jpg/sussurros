package com.sussurros.entidade;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.sussurros.assombracao.Depuracao;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Percepcao;

/**
 * Cérebro de busca do Hóspede.
 *
 * O Diretor decide QUANDO o Hóspede deve agir. Esta classe decide COMO ele tenta
 * localizar um jogador depois de perder contato: última posição conhecida,
 * investigação, busca por pontos plausíveis e, finalmente, desistência.
 *
 * Não usa a posição atual do alvo como telemetria gratuita. A posição só é
 * atualizada por visão direta, por um ruído de movimento plausível ou por uma ação
 * barulhenta do jogador (quebrar bloco, porta, baú).
 *
 * 0.9: ouve ações; os sentidos afiam com o tempo de busca; ao chegar ao último lugar ele para e olha
 * em volta; os pontos de busca fecham o cerco em vez de se espalharem.
 */
final class HospedeBusca {
    enum Estado {
        ULTIMA_POSICAO, OLHANDO, INVESTIGANDO, PROCURANDO, DESISTINDO
    }

    private static final int OUVIR_CADA_TICKS = 20;
    private static final double RAIO_OUVIDO_MAX = 42.0;
    private static final double MIN_MOVIMENTO_PARA_OUVIR = 2.5;
    private static final int MAX_PONTOS_BUSCA = 4;
    private static final int TEMPO_MAX_BUSCA = 300;
    private static final int TEMPO_DESISTINDO = 45;
    /** Raio dos pontos de busca, um por ponto já visitado: o cerco fecha. */
    private static final double[] RAIO_BUSCA = {12.0, 8.0, 5.0, 4.0};

    private Estado estado = Estado.ULTIMA_POSICAO;
    private Vec3 ultimaPosicaoConhecida = Vec3.ZERO;
    private long ultimaPosicaoTick = -1;
    private double confianca = 0.35;
    private Vec3 pontoBusca;
    private int pontosVisitados;
    private int buscaTicks;
    private long ultimoOuvidoTick = -100000;
    private Vec3 ultimaPosicaoOuvida;
    private int falhasNavegacao;
    private boolean inicializado;
    /** Multiplica as velocidades de navegação. Quem manda é a caçada (ver Cacada). */
    private double velocidade = 1.0;
    /** Ticks seguidos sem nenhuma notícia do alvo: quanto mais tempo, mais ele escuta. */
    private int semNoticia;
    private int olhandoRestante;
    private float olhandoBase;

    void inicializar(ServerPlayer alvo, long tick) {
        this.ultimaPosicaoConhecida = alvo.position();
        this.ultimaPosicaoTick = tick;
        this.ultimaPosicaoOuvida = alvo.position();
        this.confianca = 0.65;
        this.estado = Estado.ULTIMA_POSICAO;
        this.pontoBusca = null;
        this.pontosVisitados = 0;
        this.buscaTicks = 0;
        this.falhasNavegacao = 0;
        this.semNoticia = 0;
        this.inicializado = true;
    }

    Estado estado() {
        return this.estado;
    }

    double confianca() {
        return this.confianca;
    }

    @Nullable
    Vec3 ultimaPosicaoConhecida() {
        return this.inicializado ? this.ultimaPosicaoConhecida : null;
    }

    /** Há quantos ticks ele não tem notícia nenhuma do alvo. */
    long idadeDoConhecimento(long tick) {
        return this.ultimaPosicaoTick < 0 ? Long.MAX_VALUE : tick - this.ultimaPosicaoTick;
    }

    int pontosVisitados() {
        return this.pontosVisitados;
    }

    void definirVelocidade(double fator) {
        this.velocidade = fator;
    }

    /** Volta a procurar a partir do último lugar conhecido, com o relógio zerado (a falsa desistência). */
    void recomecar() {
        this.estado = Estado.ULTIMA_POSICAO;
        this.pontoBusca = null;
        this.pontosVisitados = 0;
        this.buscaTicks = 0;
        this.falhasNavegacao = 0;
    }

    /**
     * Uma ação barulhenta do jogador (quebrar bloco, porta, baú). Dá a posição com boa certeza, sem precisar
     * de sorteio: quem mexe no mundo se entrega.
     */
    void ouvirAcao(ServerLevel level, HospedeEntity hospede, Vec3 onde, double certeza, String oQue) {
        if (!this.inicializado) {
            return;
        }
        registrarConhecimento(onde, level.getGameTime(), certeza);
        this.ultimaPosicaoOuvida = onde;
        if (this.estado != Estado.ULTIMA_POSICAO) {
            mudarEstado(level, hospede, Estado.ULTIMA_POSICAO, "OUVIU_" + oQue);
        }
        this.pontoBusca = null;
        this.buscaTicks = 0;
        this.falhasNavegacao = 0;
    }

    /** Ele está vendo o jogador agora: sabe exatamente onde ele está. Sem linha de log a cada olhada. */
    void ver(ServerLevel level, HospedeEntity hospede, Vec3 onde) {
        if (!this.inicializado) {
            return;
        }
        registrarConhecimento(onde, level.getGameTime(), 1.0);
        this.ultimaPosicaoOuvida = onde;
        if (this.estado != Estado.ULTIMA_POSICAO) {
            mudarEstado(level, hospede, Estado.ULTIMA_POSICAO, "VIU");
        }
        this.pontoBusca = null;
        this.buscaTicks = 0;
        this.falhasNavegacao = 0;
    }

    /** Atualiza o rastro mental e retorna true se houve um novo ruído perceptível. */
    boolean ouvirMovimento(ServerLevel level, HospedeEntity hospede, ServerPlayer alvo, boolean percebido) {
        if (!this.inicializado) {
            this.inicializar(alvo, level.getGameTime());
        }
        long tick = level.getGameTime();

        if (percebido) {
            registrarConhecimento(alvo.position(), tick, 1.0);
            this.estado = Estado.ULTIMA_POSICAO;
            this.pontoBusca = null;
            this.buscaTicks = 0;
            this.falhasNavegacao = 0;
            return true;
        }

        if (tick - this.ultimoOuvidoTick < OUVIR_CADA_TICKS) {
            return false;
        }
        this.ultimoOuvidoTick = tick;
        this.semNoticia += OUVIR_CADA_TICKS;

        double distancia = Math.sqrt(hospede.distanceToSqr(alvo));
        if (distancia > RAIO_OUVIDO_MAX) {
            return false;
        }

        Vec3 movimento = alvo.getDeltaMovement();
        double velocidadeAlvo = Math.sqrt(movimento.x * movimento.x + movimento.z * movimento.z);
        double deslocamento = this.ultimaPosicaoOuvida == null
                ? 0.0
                : this.ultimaPosicaoOuvida.distanceTo(alvo.position());

        boolean correndo = alvo.isSprinting();
        boolean pulando = !alvo.onGround() && movimento.y > 0.03;
        double intensidade = velocidadeAlvo * 3.6 + (correndo ? 0.55 : 0.0) + (pulando ? 0.20 : 0.0);
        intensidade += Math.min(0.55, deslocamento / 8.0);
        double alcance = 1.0 - distancia / RAIO_OUVIDO_MAX;
        // Os sentidos afiam: +10% a cada 10 s sem notícia, até +50%.
        double afiado = 1.0 + Math.min(0.5, 0.1 * (this.semNoticia / 200));
        double chance = limitar((0.05 + intensidade * 0.24 + alcance * 0.34) * afiado, 0.0, 0.94);

        // Agachado e quieto é silêncio de verdade, por mais perto que ele esteja.
        if (alvo.isCrouching() && deslocamento < 1.0) {
            return false;
        }
        // Movimento muito pequeno não deve virar GPS disfarçado.
        if (deslocamento < MIN_MOVIMENTO_PARA_OUVIR && !correndo && velocidadeAlvo < 0.12) {
            return false;
        }
        if (level.getRandom().nextDouble() > chance) {
            return false;
        }

        double certeza = limitar(0.28 + alcance * 0.42 + Math.min(0.25, intensidade * 0.12), 0.25, 0.92);
        registrarConhecimento(alvo.position(), tick, certeza);
        this.ultimaPosicaoOuvida = alvo.position();
        // Ruído forte dá uma posição bastante útil; ruído fraco vira investigação aproximada.
        this.estado = certeza >= 0.62 ? Estado.ULTIMA_POSICAO : Estado.INVESTIGANDO;
        this.pontoBusca = null;
        this.buscaTicks = 0;
        this.falhasNavegacao = 0;
        return true;
    }

    /**
     * Executa um passo da busca. Retorna true quando a criatura deve desaparecer
     * porque perdeu completamente o rastro.
     */
    boolean tick(ServerLevel level, HospedeEntity hospede, ServerPlayer alvo) {
        if (!this.inicializado) {
            this.inicializar(alvo, level.getGameTime());
        }
        this.buscaTicks++;

        if (this.estado == Estado.DESISTINDO) {
            return this.buscaTicks >= TEMPO_DESISTINDO;
        }

        if (this.buscaTicks > TEMPO_MAX_BUSCA) {
            mudarEstado(level, hospede, Estado.DESISTINDO, "TEMPO_ESGOTADO");
            return false;
        }

        if (this.estado == Estado.ULTIMA_POSICAO) {
            if (chegou(hospede, this.ultimaPosicaoConhecida, 2.8)) {
                // Chegou e não achou: para, olha para os lados. É a pausa em que o jogador escondido prende o fôlego.
                mudarEstado(level, hospede, Estado.OLHANDO, "CHEGOU_ULTIMA_POSICAO");
                this.olhandoRestante = 8 + level.getRandom().nextInt(5); // em passos de 5 ticks: 2 a 3 s
                this.olhandoBase = hospede.getYRot();
                hospede.getNavigation().stop();
                return false;
            }
            if (!navegarPara(hospede, this.ultimaPosicaoConhecida, 1.0)) {
                if (++this.falhasNavegacao >= 3) {
                    mudarEstado(level, hospede, Estado.INVESTIGANDO, "CAMINHO_FALHOU");
                    this.pontoBusca = null;
                    this.falhasNavegacao = 0;
                }
            } else {
                this.falhasNavegacao = 0;
            }
            return false;
        }

        if (this.estado == Estado.OLHANDO) {
            hospede.getNavigation().stop();
            float giro = (float) Math.sin(this.olhandoRestante * 0.9) * 70.0F;
            hospede.setYRot(this.olhandoBase + giro);
            hospede.setYHeadRot(this.olhandoBase + giro);
            if (--this.olhandoRestante <= 0) {
                mudarEstado(level, hospede, Estado.PROCURANDO, "OLHOU_EM_VOLTA");
                this.pontoBusca = null;
                this.pontosVisitados = 0;
            }
            return false;
        }

        if (this.estado == Estado.INVESTIGANDO) {
            if (chegou(hospede, this.ultimaPosicaoConhecida, 4.0)) {
                mudarEstado(level, hospede, Estado.PROCURANDO, "AREA_INVESTIGADA");
                this.pontoBusca = null;
                this.pontosVisitados = 0;
                return false;
            }
            if (this.pontoBusca == null) {
                this.pontoBusca = pontoInvestigacao(level, hospede, this.ultimaPosicaoConhecida, alvo);
            }
            if (this.pontoBusca != null) {
                navegarPara(hospede, this.pontoBusca, 0.88);
                if (chegou(hospede, this.pontoBusca, 2.1)) {
                    this.pontosVisitados++;
                    this.pontoBusca = null;
                    if (this.pontosVisitados >= 2) {
                        mudarEstado(level, hospede, Estado.PROCURANDO, "SEGUNDA_BUSCA");
                    }
                }
            } else {
                this.pontosVisitados++;
                if (this.pontosVisitados >= 2) {
                    mudarEstado(level, hospede, Estado.PROCURANDO, "SEM_PONTO");
                }
            }
            return false;
        }

        // PROCURANDO: procura pontos alternativos ao redor da última posição,
        // sem convergir diretamente para a posição atual do jogador.
        if (this.pontoBusca == null) {
            this.pontoBusca = novoPontoBusca(level, hospede, alvo);
            if (this.pontoBusca == null) {
                if (++this.falhasNavegacao >= 2) {
                    mudarEstado(level, hospede, Estado.DESISTINDO, "SEM_AREA");
                }
                return false;
            }
        }

        navegarPara(hospede, this.pontoBusca, 0.76);
        if (chegou(hospede, this.pontoBusca, 2.0)) {
            this.pontosVisitados++;
            this.pontoBusca = null;
            this.falhasNavegacao = 0;
            // O próximo ponto não é escolhido imediatamente: dá uma janela para o
            // jogador ouvir/estranhar o caminho em vez de assistir a uma IA robótica.
            if (this.pontosVisitados >= MAX_PONTOS_BUSCA) {
                mudarEstado(level, hospede, Estado.DESISTINDO, "BUSCA_CONCLUIDA");
            }
        }
        return false;
    }

    private void registrarConhecimento(Vec3 posicao, long tick, double certeza) {
        this.ultimaPosicaoConhecida = posicao;
        this.ultimaPosicaoTick = tick;
        this.confianca = limitar(certeza, 0.0, 1.0);
        this.semNoticia = 0;
    }

    private void mudarEstado(ServerLevel level, HospedeEntity hospede, Estado novo, String motivo) {
        if (this.estado == novo) {
            return;
        }
        if (Depuracao.ativo && hospede.getAlvo() != null) {
            long seg = level.getGameTime() / 20;
            Depuracao.log(hospede.getAlvo(), seg, String.format(Locale.ROOT,
                    "BUSCA id=%s %s -> %s motivo=%s conf=%.2f pontos=%d",
                    hospede.getIdManifestacao(), this.estado, novo, motivo, this.confianca, this.pontosVisitados));
        }
        this.estado = novo;
        this.buscaTicks = 0;
        if (novo == Estado.DESISTINDO) {
            hospede.getNavigation().stop();
        }
    }

    private boolean navegarPara(HospedeEntity hospede, Vec3 ponto, double fator) {
        return hospede.getNavigation().moveTo(ponto.x, ponto.y, ponto.z, fator * this.velocidade);
    }

    private boolean chegou(HospedeEntity hospede, Vec3 ponto, double raio) {
        return hospede.position().distanceToSqr(ponto) <= raio * raio;
    }

    @Nullable
    private Vec3 pontoInvestigacao(ServerLevel level, HospedeEntity h, Vec3 centro, ServerPlayer alvo) {
        RandomSource rnd = level.getRandom();
        for (int i = 0; i < 10; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double r = 3.5 + rnd.nextDouble() * 5.5;
            BlockPos chao = acharChao(level, centro.x + Math.cos(ang) * r, centro.y + 3, centro.z + Math.sin(ang) * r);
            if (valido(level, h, alvo, chao, 3.0, 16.0)) {
                return new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
            }
        }
        return null;
    }

    @Nullable
    private Vec3 novoPontoBusca(ServerLevel level, HospedeEntity h, ServerPlayer alvo) {
        RandomSource rnd = level.getRandom();
        Vec3 centro = this.ultimaPosicaoConhecida;
        double raio = RAIO_BUSCA[Math.min(this.pontosVisitados, RAIO_BUSCA.length - 1)];
        for (int i = 0; i < 18; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double r = raio * (0.6 + rnd.nextDouble() * 0.4);
            BlockPos chao = acharChao(level, centro.x + Math.cos(ang) * r, centro.y + 5, centro.z + Math.sin(ang) * r);
            if (valido(level, h, alvo, chao, 4.0, 22.0)) {
                return new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5);
            }
        }
        return null;
    }

    private boolean valido(ServerLevel level, HospedeEntity h, ServerPlayer alvo, @Nullable BlockPos chao, double min, double max) {
        if (chao == null) {
            return false;
        }
        if (Diretor.emZonaCalma(alvo, chao.getX(), chao.getY(), chao.getZ())) {
            return false;
        }
        double d = alvo.position().distanceToSqr(new Vec3(chao.getX() + 0.5, chao.getY(), chao.getZ() + 0.5));
        d = Math.sqrt(d);
        if (d < min || d > max) {
            return false;
        }
        // Não escolhe um ponto que esteja claramente no campo de visão atual.
        Vec3 ponto = new Vec3(chao.getX() + 0.5, chao.getY() + 1.2, chao.getZ() + 0.5);
        if (d < 20 && alvo.getViewVector(1.0F).dot(ponto.subtract(alvo.getEyePosition()).normalize()) > Percepcao.coneSeguro(alvo)) {
            return false;
        }
        return level.getBlockState(chao).isAir();
    }

    /** Chão firme com dois blocos de ar em cima: na caça ele se abaixa e cabe onde o jogador cabe. */
    @Nullable
    static BlockPos acharChao(ServerLevel level, double x, double yBase, double z) {
        for (int dy = 7; dy >= -14; dy--) {
            BlockPos pos = BlockPos.containing(x, yBase + dy, z);
            BlockPos baixo = pos.below();
            BlockState s0 = level.getBlockState(pos);
            BlockState s1 = level.getBlockState(pos.above());
            if (s0.isAir() && s1.isAir()
                    && !level.getBlockState(baixo).getCollisionShape(level, baixo).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static double limitar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

}
