package com.sussurros.assombracao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

import com.sussurros.entidade.HospedeEntity;
import com.sussurros.rede.PacoteSentidos;

/**
 * Tudo o que o Diretor sabe sobre um jogador NESTA sessão (não é salvo no disco).
 * O que precisa sobreviver entre sessões fica em {@link Memoria}, {@link Lugares} e {@link Perfil}.
 */
final class EstadoJogador {
	// --- Criatura ---
	@Nullable HospedeEntity criatura;

	// --- Obsessão: escalada LENTA (a pressão é a tensão do momento) (v0.4.2) ---
	double obsessao;
	boolean obsessaoCarregada;

	// --- Sequência de ameaça (estado AMEACANDO) (v0.4.2) ---
	enum Cena {
		NENHUMA, PRESENCA, ESPREITANDO, PAUSA, GOLPE
	}

	Cena cena = Cena.NENHUMA;
	long cenaDesde;
	long cenaAte;
	boolean golpeDado;
	long ameacaLiberadaEm;

	// --- Cena "Ele voltou com você" (v0.4.2a) ---
	enum CenaCasa {
		NENHUMA, ESPERA, SOM_FORA, PORTA, ESPERA_DENTRO, PRESENCA, ESPREITANDO, SILENCIO
	}

	CenaCasa cenaCasa = CenaCasa.NENHUMA;
	long cenaCasaAte;
	long cenaCasaDesde;
	boolean cenaCasaTeste;
	long cenaCasaLiberadaEm;
	int cenasCasaFeitas;
	long longeDesde = -1;
	boolean voltaPendente;

	// --- Cena "Algo no túnel" (0.4.3-exp1) ---
	enum CenaTunel {
		NENHUMA, ESPERA, ECO, RUIDO, PRESENCA, ESPREITANDO, SILENCIO
	}

	CenaTunel cenaTunel = CenaTunel.NENHUMA;
	long cenaTunelDesde;
	long cenaTunelAte;
	long cenaTunelLiberadaEm;
	int cenasTunelFeitas;
	long subsoloDesde = -1;
	boolean tentouCenaTunel;
	boolean cenaTunelTeste;


	// --- Contexto do mundo (0.5-alpha): casa, subsolo, area aberta ou outro ---
	ContextoMundo.Tipo contexto = ContextoMundo.Tipo.OUTRO;
	long contextoDesde = -1;
	ContextoMundo.Tipo contextoCandidato = ContextoMundo.Tipo.OUTRO;
	long contextoCandidatoDesde = -1;
	final Atmosfera.Estado atmosfera = new Atmosfera.Estado();
	long abertoDesde = -1;
	boolean tentouCenaCampo;

	// --- Cena "Do outro lado do vidro" (0.5.0-alpha4): manifestação doméstica junto a uma janela ---
	enum CenaJanela {
		NENHUMA, ESPERA, APARICAO, OBSERVANDO, TOQUE, SILENCIO
	}

	CenaJanela cenaJanela = CenaJanela.NENHUMA;
	long cenaJanelaDesde;
	long cenaJanelaAte;
	long cenaJanelaLiberadaEm;
	long cenaJanelaVistaDesde = -1;
	int cenasJanelaFeitas;
	boolean cenaJanelaTeste;
	@Nullable BlockPos cenaJanelaPos;

	// --- Cena "Foi aqui" (0.5-alpha2): revisita um marco persistente de uma reação forte ---
	enum CenaMarco {
		NENHUMA, ESPERA, ECO, PRESENCA, OBSERVANDO, SILENCIO
	}

	CenaMarco cenaMarco = CenaMarco.NENHUMA;
	long cenaMarcoDesde;
	long cenaMarcoAte;
	long cenaMarcoLiberadaEm;
	boolean cenaMarcoTeste;
	long marcoPendente = Long.MIN_VALUE;
	@Nullable BlockPos cenaMarcoPos;
	final Set<Long> marcosUsadosSessao = new HashSet<>();

	// --- Cena "Na linha das arvores" (0.5-alpha) ---
	enum CenaCampo {
		NENHUMA, ESPERA, PRIMEIRA, OBSERVANDO_1, PAUSA, SEGUNDA, OBSERVANDO_2, SILENCIO
	}

	CenaCampo cenaCampo = CenaCampo.NENHUMA;
	long cenaCampoDesde;
	long cenaCampoAte;
	long cenaCampoLiberadaEm;
	int cenasCampoFeitas;
	boolean cenaCampoTeste;
	int cenaCampoLado = 1;

	// --- Telemetria (0.4.2a-test+): só para o log, não muda nada ---
	String cenaCasaId = "-";
	String cenaTunelId = "-";
	String cenaCampoId = "-";
	String cenaMarcoId = "-";
	String cenaJanelaId = "-";
	String ameacaId = "-";
	String silencioMotivo = "";
	String silencioCena = "-";

	// --- Intensidade-alvo: abatimento depois de algo forte (v0.4.2a) ---
	long ultimoForteSeg = -100000;

	// --- Rastro e âncoras (v0.4.2) ---
	final Rastro rastro = new Rastro();
	final Map<String, Integer> usosPortas = new HashMap<>();

	// --- Agenda ---
	long proximoEvento = -1;
	long ultimoEventoSeg = -1;
	long esperandoDesde = -1;
	/** Eventos sorteados neste segundo que não couberam no mundo: ficam fora do próximo sorteio do mesmo segundo. */
	final EnumSet<Evento> falhasAgora = EnumSet.noneOf(Evento.class);
	/** Depois de um segundo em que nada coube, ele só volta a tentar a partir daqui. */
	long semLugarAte = -1;

	// --- Ritmo e estados ---
	double pressao;
	EstadoDiretor estado = EstadoDiretor.CALMO;
	long estadoDesde = -1;
	int duracaoEstado = 60;
	int testesSemReacao;
	final EnumSet<Evento.Categoria> testadasNoTeste = EnumSet.noneOf(Evento.Categoria.class);
	boolean contato;
	double ultimaConfianca;
	long ultimaConfiancaSeg = -100000; // quando a última reação válida foi lida
	int testesFrustrados; // rodadas de TESTANDO que terminaram sem reação nenhuma (v0.4.1)
	boolean forcando; // true enquanto um evento roda por COMANDO: não conta para nada (v0.4.1)
	final ArrayDeque<Double> confiancasRecentes = new ArrayDeque<>();
	int ultimoV;

	// --- Anti-repetição e cadeias ---
	final ArrayDeque<Evento> recentes = new ArrayDeque<>();
	// Chunks usados por aparições recentes: memória curta de posicionamento, só na sessão.
	final ArrayDeque<Long> aparicoesRecentes = new ArrayDeque<>();
	final ArrayDeque<String> pares = new ArrayDeque<>(); // "SOM>VISAO": sequências de tipos de medo
	@Nullable Evento ultimoEvento;
	@Nullable Evento sequencia;
	int elosCadeia;
	long carenciaAte = -1;
	boolean ecoPerto;

	// --- Aprendizado curto (volta ao neutro com o tempo) ---
	final EnumMap<Evento, Double> curtoEv = new EnumMap<>(Evento.class);
	final EnumMap<Evento.Categoria, Double> curtoCat = new EnumMap<>(Evento.Categoria.class);
	final EnumMap<Evento.Categoria, Double> interesse = new EnumMap<>(Evento.Categoria.class);

	// --- Leitura das reações ---
	final Leitura leitura = new Leitura();

	// --- Percepção atrasada (coisas que ele só nota depois) ---
	record Atrasado(Evento evento, Vec3 pos, long expira) {
	}

	final List<Atrasado> atrasados = new ArrayList<>();

	// --- Movimento por segundo ---
	boolean temUltimo;
	double ultX;
	double ultZ;
	float ultYaw;
	double velocidade;

	// --- Acumuladores do minuto (perfil) ---
	int minEscuro;
	int minEscuroCauteloso;
	int minNoiteOuSubsolo;
	int minComLuz;
	int minNoite;
	int minEmCasa;
	int minOlhadas;
	int minChunksNovos;

	// --- Memória sensorial ---
	// Ações recentes: O QUE você fez, ONDE e QUANDO (v0.4.2a; 0.4.2 guardava só o som).
	enum TipoAcao {
		QUEBRA, PORTA
	}

	record Acao(TipoAcao tipo, SoundEvent som, double x, double y, double z, long seg) {
	}

	final ArrayDeque<Acao> acoes = new ArrayDeque<>();
	final ArrayDeque<String> falas = new ArrayDeque<>();
	final ArrayDeque<Long> quebrasRecentes = new ArrayDeque<>();
	long ultimoDanoTick = -100000;

	// --- Vela ---
	double zonaX;
	double zonaY;
	double zonaZ;
	double zonaRaio = 8;
	long zonaAteTick = -1;

	// --- Isca Pálida (0.5.0-alpha4): tenta puxar a próxima PRESENCA para um ponto escolhido ---
	double iscaX;
	double iscaY;
	double iscaZ;
	long iscaAteTick = -1;
	boolean iscaAtiva;

	// --- Fio de Vigília (0.5.0-alpha3): detector local, não é zona segura ---
	double vigiaX;
	double vigiaY;
	double vigiaZ;
	double vigiaRaio = 6;
	long vigiaAteTick = -1;
	boolean vigiaAtiva;

	// --- Olho Sussurrante (0.8.1): usar muito não pode virar um botão de chamar a criatura ---
	/** Momentos (em segundos) dos usos recentes do Olho sem criatura por perto. */
	final ArrayDeque<Long> olhoUsos = new ArrayDeque<>();
	/** Depois de o Olho chamar uma aparição, ele só pode chamar outra a partir daqui. */
	long olhoRecargaAte = -1;

	/** Segundo em que o jogador olhou direto para a criatura pela última vez (para a leitura do avistamento). */
	long encarouSeg = -1000;

	// --- Cache do ambiente (portas e tochas por perto) ---
	long cacheTick = -100000;
	double cacheX;
	double cacheZ;
	final List<BlockPos> portas = new ArrayList<>();
	final List<BlockPos> tochas = new ArrayList<>();
	final List<BlockPos> janelas = new ArrayList<>();

	// --- Sentidos (0.9): o que o cliente deste jogador mostra e toca. Só apresentação. ---
	/** O último pacote calculado. Os testes leem daqui, porque jogador de teste não recebe pacote. */
	PacoteSentidos sentidos = PacoteSentidos.NEUTRO;
	/** Valores impostos por comando de teste, enquanto durar. */
	@Nullable PacoteSentidos sentidosForcados;
	long sentidosForcadosAte = -1;
	/** Sensação de estar sendo olhado sem ninguém ali (o aviso que mente). */
	long vigiaFalsaAte = -1;
	float vigiaFalsaForca;
	long neblinaAte = -1;
	float neblinaForca;
	/** Janela em que os próprios passos ecoam. */
	long ecoPassoAte = -1;
	long semMusicaAte = -1;
	// --- O Véu (0.9) ---
	long veuDesde;
	long veuAte;
	long veuProximoEm;
	long veuVultoEm = -1;
	boolean veuFechou;
	// --- A Soleira (0.9): de que lado da porta ele estava na última olhada ---
	double soleiraFrente = Double.NaN;
	double soleiraLado;
	/** O aviso de uma caçada: a trilha sobe um pouco antes de ele aparecer (ou de não aparecer). */
	long cacaAvisoAte = -1;

	// --- Itens da 0.9 ---
	/** A Caixa de Música: onde está tocando e até quando. */
	double caixaX;
	double caixaY;
	double caixaZ;
	long caixaDesdeTick;
	long caixaAteTick = -1;
	boolean caixaCantarolou;
	/** Trégua comprada pelos ossos: nenhum acontecimento até aqui. */
	long treguaAte = -1;
	/** Trégua da oferenda aceita: dentro de casa, nada até aqui. */
	long treguaCasaAte = -1;
	/** Ele veio cobrar a oferenda que faltou (ou a afronta): as batidas na porta ganham peso até aqui. */
	long desfeitaAte = -1;
	/** A Conta estourou: a cobrança sai neste tick. */
	long cobrancaEm = -1;
	/** Ossos de Agouro: quantas vezes jogou hoje (o dia do relógio do mundo). */
	long ossosDia = -1;
	int ossosNoDia;
	/** Onde está a vela de verdade da zona de calma, se coube colocar uma. */
	@Nullable BlockPos velaBloco;

	// --- Lugar atual ---
	long ultimoChunk = Long.MIN_VALUE;
	boolean chunkEhMarco;
}
