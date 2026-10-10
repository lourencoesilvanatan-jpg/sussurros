package com.sussurros.assombracao;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.sussurros.Sussurros;

/**
 * Tudo o que o mod "lembra" sobre um jogador, salvo junto com o personagem.
 * É só um mapa de texto -> número, o que deixa fácil adicionar coisas novas.
 *
 * Uso:
 *   Memoria m = Memoria.de(jogador);
 *   m.add(Memoria.INQUIETACAO, 3);
 *   m.salvar();
 */
public final class Memoria {
	// ===== Chaves =====
	public static final String TEMPO = "tempo";             // segundos de assombração acumulados
	public static final String FASE = "fase";               // 0 a 4
	public static final String INQUIETACAO = "inquietacao"; // 0 a 200 (escondido do jogador)
	public static final String PAGINAS_LIDAS = "paginas_lidas";
	public static final String PAGINAS_ENTREGUES = "paginas_entregues";
	public static final String RECEBEU_OLHO = "recebeu_olho";
	public static final String VEZES_VISTO = "vezes_visto";
	public static final String VEZES_FERIDO = "vezes_ferido";
	public static final String VELAS = "velas";
	public static final String OLHOS = "olhos";
	public static final String SEM_REACAO = "sem_reacao";
	public static final String TOCHAS_ROUBADAS = "tochas_roubadas";
	public static final String CAMA_X = "cama_x";
	public static final String CAMA_Y = "cama_y";
	public static final String CAMA_Z = "cama_z";
	public static final String TEM_CAMA = "tem_cama";
	public static final String CAMA_VEZES = "cama_vezes";
	public static final String PESO = "peso_"; // + nome do evento ou "cat_" + categoria
	public static final String MORTE_X = "morte_x";
	public static final String MORTE_Y = "morte_y";
	public static final String MORTE_Z = "morte_z";
	public static final String TEM_MORTE = "tem_morte";
	public static final String OLHADAS = "olhadas";     // quantas vezes olhou para trás "do nada"
	public static final String CASEIRO = "caseiro";     // segundos passados em casa à noite
	public static final String SUBSOLO = "subsolo";     // segundos passados no subsolo
	public static final String OBSESSAO = "obsessao";   // 0 a 1000 (décimos): escalada lenta até a caça (v0.4.2)
	public static final String TEM_PORTA = "tem_porta"; // porta habitual (âncora)
	public static final String PORTA_X = "porta_x";
	public static final String PORTA_Y = "porta_y";
	public static final String PORTA_Z = "porta_z";
	public static final String PORTA_USOS = "porta_usos";
	public static final String CINZAS_GERADAS = "cinzas_geradas"; // vestígios físicos deixados pelo Hóspede
	public static final String SINOS_USADOS = "sinos_usados";     // uso da ferramenta chama atenção
	public static final String FIOS_ARMADOS = "fios_armados";     // quantas vigílias o jogador preparou
	public static final String FIOS_ROMPIDOS = "fios_rompidos";   // quantas vezes algo cruzou uma vigília
	public static final String ISCAS_ARMADAS = "iscas_armadas";   // tentativas de chamar a presença a um ponto
	public static final String ISCAS_ATENDIDAS = "iscas_atendidas"; // quantas vezes a presença realmente usou a isca
	public static final String CHUNKS_VISITADOS = "chunks_visitados";
	public static final String CADERNO_USOS = "caderno_usos";
	public static final String ESTRUTURA_MARCO = "estrutura_marco";
	public static final String ESTRUTURA_MARCO_X = "estrutura_marco_x";
	public static final String ESTRUTURA_MARCO_Y = "estrutura_marco_y";
	public static final String ESTRUTURA_MARCO_Z = "estrutura_marco_z";
	public static final String ESTRUTURA_MARCO_VISTA = "estrutura_marco_vista";
	public static final String ESTRUTURA_POSTO = "estrutura_posto";
	public static final String ESTRUTURA_POSTO_X = "estrutura_posto_x";
	public static final String ESTRUTURA_POSTO_Y = "estrutura_posto_y";
	public static final String ESTRUTURA_POSTO_Z = "estrutura_posto_z";
	public static final String ESTRUTURA_POSTO_VISTA = "estrutura_posto_vista";
	public static final String ESTRUTURA_NICHO = "estrutura_nicho";
	public static final String ESTRUTURA_NICHO_X = "estrutura_nicho_x";
	public static final String ESTRUTURA_NICHO_Y = "estrutura_nicho_y";
	public static final String ESTRUTURA_NICHO_Z = "estrutura_nicho_z";
	public static final String ESTRUTURA_NICHO_VISTA = "estrutura_nicho_vista";

	// 0.9: a caçada
	public static final String CACADAS = "cacadas";                 // quantas caçadas de verdade já aconteceram
	public static final String CAPTURAS = "capturas";               // quantas vezes ele pegou o jogador
	public static final String MARCAS = "marcas";                   // corações de vida máxima perdidos (0 a 3); dormir com vela acesa cura
	public static final String CACA_DEVIDA = "caca_devida"; // 1: ele saiu do jogo ou fugiu para longe no meio de uma caçada; ela volta
	public static final String CACA_ATRAVESSOU = "caca_atravessou"; // vezes que ele precisou atravessar (pilar, buraco, muro): cada uma encurta o aviso

	// 0.9: os itens
	public static final String CAIXA_USOS = "caixa_usos"; // vezes que a Caixa de Música tocou: é assim que ele aprende a cantiga
	public static final String VEUS = "veus"; // quantas vezes o Véu abriu para este jogador
	public static final String RECEBEU_CAIXA = "recebeu_caixa"; // até a alpha13 a caixa era deixada na passagem para a fase 2; ficou só nos mundos antigos
	public static final String SINO_RESPONDEU = "sino_respondeu"; // 1: o sino já deu a primeira resposta de verdade (ver Diretor.usarSino)

	public static final int MAX_INQUIETACAO = 200;

	public static final AttachmentType<Map<String, Integer>> TIPO = AttachmentRegistry.create(
			Sussurros.id("memoria"),
			builder -> builder
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
					.copyOnDeath()
	);

	private final ServerPlayer jogador;
	private final Map<String, Integer> dados;

	private Memoria(ServerPlayer jogador, Map<String, Integer> dados) {
		this.jogador = jogador;
		this.dados = dados;
	}

	public static Memoria de(ServerPlayer jogador) {
		// Copiamos para um HashMap porque o mapa carregado do disco pode ser imutável.
		return new Memoria(jogador, new HashMap<>(jogador.getAttachedOrCreate(TIPO)));
	}

	public int get(String chave) {
		return this.dados.getOrDefault(chave, 0);
	}

	public int get(String chave, int padrao) {
		return this.dados.getOrDefault(chave, padrao);
	}

	public void set(String chave, int valor) {
		this.dados.put(chave, valor);
	}

	public void add(String chave, int valor) {
		this.dados.put(chave, get(chave) + valor);
	}

	public void limitar(String chave, int min, int max) {
		set(chave, Math.max(min, Math.min(max, get(chave))));
	}

	public void limpar() {
		this.dados.clear();
	}

	public Map<String, Integer> copia() {
		return new HashMap<>(this.dados);
	}

	public void salvar() {
		this.jogador.setAttached(TIPO, new HashMap<>(this.dados));
	}

	/** Só garante que a classe carregue e o anexo seja registrado. */
	public static void inicializar() {
	}
}
