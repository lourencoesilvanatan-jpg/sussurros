#!/usr/bin/env python3
"""Confere os arquivos de recursos do Sussurros (texturas, modelos, estados de bloco,
receitas, saque e tags) sem abrir o jogo.

Uso (de qualquer pasta):

    python ferramentas/texturas/conferir_recursos.py

O que confere, em `src/main/resources`:

1. todo `.json` de `assets/` e `data/` é JSON válido;
2. todo modelo citado por um estado de bloco existe;
3. todo modelo citado por uma definição de item (`items/`) existe;
4. em cada modelo, o pai e as texturas citadas existem;
5. o resultado de cada receita (e cada ingrediente do mod) tem definição em `items/`;
6. o que as tabelas de saque soltam tem definição em `items/`, e cada tabela e cada
   entrada de tag de bloco do mod tem o seu estado de bloco;
7. toda textura de bloco e de item é PNG 16x16 RGBA, e as de bloco só têm alfa 0 ou 255
   (um pixel meio transparente faria o jogo desenhar a face como translúcida).

O que é do namespace `minecraft:` não precisa existir no repositório: vem do jogo.
`textures/entity` e o ícone do mod ficam de fora da regra dos 16x16.

Sai com código 0 se estiver tudo certo e 1 se achar problema. Precisa do Pillow.

Isto não substitui olhar dentro do jogo: garante só que nada aponta para um arquivo que
não existe.
"""

import json
import sys
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
RECURSOS = RAIZ / "src" / "main" / "resources"
ASSETS = RECURSOS / "assets"
DATA = RECURSOS / "data"
MOD = "sussurros"

problemas = []
contagem = {}


def problema(arquivo, texto):
    problemas.append(f"{arquivo.relative_to(RAIZ).as_posix()}: {texto}")


def contar(o_que, quantos=1):
    contagem[o_que] = contagem.get(o_que, 0) + quantos


def separar(ident):
    """'sussurros:block/x' -> ('sussurros', 'block/x'); sem namespace é do jogo."""
    if ":" in ident:
        ns, caminho = ident.split(":", 1)
        return ns, caminho
    return "minecraft", ident


def existe(ident, pasta, extensao, base=ASSETS):
    """O recurso existe no repositório? O que é do jogo conta como existente."""
    ns, caminho = separar(ident)
    if ns == "minecraft":
        return True
    return (base / ns / pasta / f"{caminho}{extensao}").is_file()


def ler_todos_os_json():
    """Passo 1. Devolve {caminho: conteúdo} só dos que abriram."""
    lidos = {}
    for raiz in (ASSETS, DATA):
        for arquivo in sorted(raiz.rglob("*.json")):
            contar("arquivos .json válidos")
            try:
                lidos[arquivo] = json.loads(arquivo.read_text(encoding="utf-8"))
            except (json.JSONDecodeError, UnicodeDecodeError) as erro:
                contar("arquivos .json válidos", -1)
                problema(arquivo, f"JSON inválido: {erro}")
    return lidos


def modelos_citados(no):
    """Todos os textos guardados em chaves "model" (ou "base"), em qualquer profundidade.
    Serve para estados de bloco (variants/multipart) e para definições de item."""
    if isinstance(no, dict):
        for chave, valor in no.items():
            if chave in ("model", "base") and isinstance(valor, str):
                yield valor
            else:
                yield from modelos_citados(valor)
    elif isinstance(no, list):
        for item in no:
            yield from modelos_citados(item)


def conferir_estados_de_bloco(lidos):
    for arquivo, conteudo in lidos.items():
        if arquivo.parent != ASSETS / MOD / "blockstates":
            continue
        contar("estados de bloco")
        citados = list(modelos_citados(conteudo))
        if not citados:
            problema(arquivo, "não cita nenhum modelo")
        for modelo in citados:
            contar("modelos citados por estados de bloco")
            if not existe(modelo, "models", ".json"):
                problema(arquivo, f"modelo que não existe: {modelo}")


def conferir_definicoes_de_item(lidos):
    for arquivo, conteudo in lidos.items():
        if arquivo.parent != ASSETS / MOD / "items":
            continue
        contar("definições de item")
        citados = list(modelos_citados(conteudo))
        if not citados:
            problema(arquivo, "não cita nenhum modelo")
        for modelo in citados:
            if not existe(modelo, "models", ".json"):
                problema(arquivo, f"modelo que não existe: {modelo}")


def conferir_modelos(lidos):
    pasta = ASSETS / MOD / "models"
    for arquivo, conteudo in lidos.items():
        if pasta not in arquivo.parents:
            continue
        contar("modelos")
        pai = conteudo.get("parent")
        if pai is not None and not existe(pai, "models", ".json"):
            problema(arquivo, f"modelo pai que não existe: {pai}")
        for nome, valor in conteudo.get("textures", {}).items():
            # na 26.2 a textura pode ser um texto ou {"sprite": ..., "force_translucent": ...}
            textura = valor.get("sprite") if isinstance(valor, dict) else valor
            if not isinstance(textura, str):
                problema(arquivo, f'textura "{nome}" em formato desconhecido: {valor!r}')
                continue
            if textura.startswith("#"):
                continue  # aponta para outra variável do próprio modelo
            contar("texturas citadas por modelos")
            if not existe(textura, "textures", ".png"):
                problema(arquivo, f'textura "{nome}" que não existe: {textura}')


def tem_item(ident):
    return existe(ident, "items", ".json")


def conferir_receitas(lidos):
    pasta = DATA / MOD / "recipe"
    for arquivo, conteudo in lidos.items():
        if pasta not in arquivo.parents:
            continue
        contar("receitas")
        resultado = conteudo.get("result")
        ident = resultado.get("id") if isinstance(resultado, dict) else resultado
        if not isinstance(ident, str):
            problema(arquivo, "receita sem resultado")
        elif not tem_item(ident):
            problema(arquivo, f"o resultado {ident} não tem definição em items/")
        for ingrediente in ingredientes(conteudo):
            if not ingrediente.startswith("#") and not tem_item(ingrediente):
                problema(arquivo, f"o ingrediente {ingrediente} não tem definição em items/")


def ingredientes(receita):
    """Os itens pedidos por uma receita, com ou sem forma (as tags, com '#', vêm junto)."""
    def achatar(no):
        if isinstance(no, str):
            yield no
        elif isinstance(no, list):
            for item in no:
                yield from achatar(item)
    yield from achatar(receita.get("ingredients", []))
    yield from achatar(receita.get("ingredient", []))
    for valor in receita.get("key", {}).values():
        yield from achatar(valor)


def itens_do_saque(no):
    if isinstance(no, dict):
        if no.get("type") == "minecraft:item" and isinstance(no.get("name"), str):
            yield no["name"]
        for valor in no.values():
            yield from itens_do_saque(valor)
    elif isinstance(no, list):
        for item in no:
            yield from itens_do_saque(item)


def conferir_saque_e_tags(lidos):
    blocos = DATA / MOD / "loot_table" / "blocks"
    for arquivo, conteudo in lidos.items():
        if arquivo.parent == blocos:
            contar("tabelas de saque de bloco")
            if not (ASSETS / MOD / "blockstates" / arquivo.name).is_file():
                problema(arquivo, f"não há estado de bloco para {MOD}:{arquivo.stem}")
            for item in itens_do_saque(conteudo):
                if not tem_item(item):
                    problema(arquivo, f"solta {item}, que não tem definição em items/")
        elif "tags" in arquivo.parts and "block" in arquivo.parts:
            contar("tags de bloco")
            for valor in conteudo.get("values", []):
                ident = valor.get("id") if isinstance(valor, dict) else valor
                if not isinstance(ident, str):
                    problema(arquivo, f"entrada de tag em formato desconhecido: {valor!r}")
                elif not ident.startswith("#") and not existe(ident, "blockstates", ".json"):
                    problema(arquivo, f"não há estado de bloco para {ident}")


def conferir_texturas():
    for tipo in ("block", "item"):
        for arquivo in sorted((ASSETS / MOD / "textures" / tipo).glob("**/*.png")):
            contar("texturas 16x16 RGBA")
            with Image.open(arquivo) as img:
                if img.size != (16, 16):
                    problema(arquivo, f"tem {img.size[0]}x{img.size[1]}, devia ter 16x16")
                if img.mode != "RGBA":
                    problema(arquivo, f"está em {img.mode}, devia estar em RGBA")
                    continue
                if tipo == "block":
                    alfas = set(img.getchannel("A").tobytes())
                    if alfas - {0, 255}:
                        problema(arquivo, "textura de bloco com pixel meio transparente")


def main():
    sys.stdout.reconfigure(encoding="utf-8")  # acentos certos também quando a saída é redirecionada
    lidos = ler_todos_os_json()
    conferir_estados_de_bloco(lidos)
    conferir_definicoes_de_item(lidos)
    conferir_modelos(lidos)
    conferir_receitas(lidos)
    conferir_saque_e_tags(lidos)
    conferir_texturas()

    for o_que, quantos in contagem.items():
        print(f"{quantos:4d}  {o_que}")
    if problemas:
        print(f"\n{len(problemas)} problema(s):")
        for linha in problemas:
            print(f"  - {linha}")
        return 1
    print("\nTudo certo: nenhum arquivo aponta para algo que não existe.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
