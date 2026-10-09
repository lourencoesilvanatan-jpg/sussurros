package com.sussurros;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sussurros.assombracao.Atencao;
import com.sussurros.assombracao.Avesso;
import com.sussurros.assombracao.ComandoSussurros;
import com.sussurros.assombracao.Diretor;
import com.sussurros.assombracao.Lugares;
import com.sussurros.assombracao.LootDoDiario;
import com.sussurros.assombracao.Memoria;
import com.sussurros.assombracao.Vestigios;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModBlocos;
import com.sussurros.registro.ModEntidades;
import com.sussurros.registro.ModItems;
import com.sussurros.registro.ModSons;
import com.sussurros.rede.Rede;

/**
 * Classe principal do mod. O Fabric chama onInitialize() quando o jogo carrega.
 */
public class Sussurros implements ModInitializer {
	public static final String MOD_ID = "sussurros";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// A ordem importa: a criatura antes dos itens (o ovo de spawn usa a criatura).
		ModEntidades.inicializar();
		ModBlocos.inicializar(); // antes dos itens: alguns itens colocam blocos
		ModItems.inicializar();
		ModSons.inicializar();
		Rede.inicializar();
		Memoria.inicializar();
		Vestigios.inicializar();
		Lugares.inicializar();
		Atencao.carregar();
		Diretor.inicializar();
		Avesso.inicializar();
		LootDoDiario.inicializar();
		ComandoSussurros.inicializar();
		LOGGER.info("Sussurros carregado.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
