#!/usr/bin/env python3
"""Gera as texturas 16x16 dos blocos e itens novos do Sussurros.

Uso (de qualquer pasta):

    python ferramentas/texturas/gerar_itens.py
    python ferramentas/texturas/gerar_itens.py --prancha C:/Temp/prancha.png

É determinístico: a semente é fixa e cada textura sorteia a partir do próprio nome,
então rodar de novo grava exatamente os mesmos arquivos. Cada textura tem a sua função,
mais abaixo; para mexer numa, mexa só na função dela e rode de novo.

`--prancha` grava também uma imagem com todas as texturas ampliadas 8x, lado a lado, para
conferir no olho. Aponte para fora do repositório: ela não é para ser commitada.

Precisa do Pillow (`pip install pillow`).

Regras da casa (as mesmas das texturas que já existiam):
- 16x16, poucos tons, paleta pálida e fria (cinza, osso, cera, ferro escurecido);
- contorno escuro discreto; nada de cor viva;
- nas texturas de bloco, o alfa é sempre 0 ou 255. O jogo decide sozinho se a face é
  recortada olhando a textura, e um pixel meio transparente a tornaria translúcida.
"""

import argparse
import random
import sys
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
TEXTURAS = RAIZ / "src" / "main" / "resources" / "assets" / "sussurros" / "textures"

SEMENTE = 1907
T = 16  # lado da textura

# ---------------------------------------------------------------------------------------
# Paleta comum
# ---------------------------------------------------------------------------------------

# Cinza pálida, da mais escura à mais clara (na mesma faixa de tons do item cinza_palida).
CINZA = [(131, 128, 121), (160, 157, 147), (190, 186, 173), (211, 207, 191), (228, 223, 205)]

RISCO = (58, 54, 56)  # arranhão na cinza

# Ferro escurecido do lampião.
FERRO_FUNDO = (30, 29, 33)
FERRO_ESCURO = (46, 45, 50)
FERRO = (60, 59, 65)
FERRO_CLARO = (80, 78, 86)

# Anel cor de osso logo abaixo da tampa do lampião (é o que o separa do lampião do jogo).
ANEL = (150, 144, 132)
ANEL_SOMBRA = (116, 110, 102)

# Chama pálida, do brilho no vidro ao miolo.
CHAMA = [(170, 160, 122), (222, 212, 170), (244, 238, 208), (255, 252, 236)]
VIDRO_MORNO = (70, 66, 57)  # vidro em volta de uma chama pequena

# Chama fria e o vidro escuro em volta dela.
FRIA = [(92, 108, 126), (146, 164, 180), (206, 218, 228)]
VIDRO_FRIO = (26, 28, 35)
VIDRO_FRIO_CLARO = (38, 42, 51)

# Vidro apagado.
VIDRO = (27, 27, 30)
VIDRO_CLARO = (41, 41, 45)

# Barro pálido da tigela, do mais escuro ao mais claro.
BARRO = [(88, 77, 71), (116, 103, 93), (146, 132, 119), (174, 160, 144), (198, 186, 170)]

# Madeira escura e gasta da caixa de música.
MADEIRA_CONTORNO = (30, 25, 24)
MADEIRA_ESCURA = (52, 42, 37)
MADEIRA = (72, 59, 50)
MADEIRA_CLARA = (97, 81, 68)
MADEIRA_GASTA = (132, 118, 102)
VAO = (14, 12, 14)

# Metal velho (manivela, pente da caixa de música).
METAL_ESCURO = (84, 80, 76)
METAL = (140, 134, 122)
METAL_CLARO = (204, 199, 183)

# Osso.
OSSO_RACHA = (70, 64, 62)
OSSO_SOMBRA = (170, 160, 144)
OSSO = (214, 208, 196)

# Fio: o vermelho apagado que o fio de vigília já usa no nó.
FIO = (113, 31, 30)
FIO_ESCURO = (81, 13, 13)


# ---------------------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------------------

def sorteio(nome):
    """Um gerador por textura: mexer numa não muda o sorteio das outras."""
    return random.Random(f"{SEMENTE}:{nome}")


def nova():
    return Image.new("RGBA", (T, T), (0, 0, 0, 0))


def ponto(img, x, y, cor):
    img.putpixel((x, y), (cor[0], cor[1], cor[2], 255))


def de_grade(linhas, cores):
    """Desenha a partir de 16 linhas de 16 letras; `cores` diz a cor de cada letra e
    '.' é transparente."""
    assert len(linhas) == T, f"a grade tem {len(linhas)} linhas"
    img = nova()
    for y, linha in enumerate(linhas):
        assert len(linha) == T, f"linha {y} tem {len(linha)} letras: {linha!r}"
        for x, letra in enumerate(linha):
            if letra != ".":
                ponto(img, x, y, cores[letra])
    return img


def campo_suave(rng):
    """Ruído 16x16 de 0 a 1, amaciado. Emenda da borda esquerda para a direita, para que
    a faixa de cinza continue sem degrau no bloco vizinho."""
    bruto = [[rng.random() for _ in range(T)] for _ in range(T)]
    suave = [[0.0] * T for _ in range(T)]
    for y in range(T):
        for x in range(T):
            soma, peso = 0.0, 0.0
            for dy in (-1, 0, 1):
                if not 0 <= y + dy < T:
                    continue
                for dx in (-1, 0, 1):
                    p = 2.0 if (dx == 0 and dy == 0) else 1.0
                    soma += p * bruto[y + dy][(x + dx) % T]
                    peso += p
            suave[y][x] = soma / peso
    menor = min(min(l) for l in suave)
    maior = max(max(l) for l in suave)
    return [[(v - menor) / (maior - menor) for v in l] for l in suave]


def na_borda(mascara, x, y):
    """O pixel encosta num vazio? (Para os lados a faixa continua no bloco vizinho.)"""
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        yy = y + dy
        if not 0 <= yy < T or not mascara[yy][(x + dx) % T]:
            return True
    return False


def pintar_cinza(mascara, tom, contorno=True, apagar=0):
    """Pinta a cinza onde a máscara manda. `apagar` escurece tudo em tantos tons."""
    img = nova()
    for y in range(T):
        for x in range(T):
            if not mascara[y][x]:
                continue
            t = tom[y][x]
            i = 1 if t < 0.16 else 2 if t < 0.48 else 3 if t < 0.84 else 4
            if contorno and na_borda(mascara, x, y):
                i -= 2
            ponto(img, x, y, CINZA[max(0, min(4, i - apagar))])
    return img


# ---------------------------------------------------------------------------------------
# Bloco: cinza espalhada (cinco estados)
# ---------------------------------------------------------------------------------------

def faixa():
    """A faixa de cinza que `intacta`, `riscada` e `rompida` têm em comum: corre de oeste a
    leste, de borda a borda, com as beiradas de cima e de baixo irregulares."""
    rng = sorteio("faixa")
    beira = campo_suave(rng)
    mancha = campo_suave(rng)
    # manchas largas com um pouco de grão por cima: a cinza é pó, não tinta
    tom = [[0.62 * mancha[y][x] + 0.38 * rng.random() for x in range(T)] for y in range(T)]
    mascara = [[False] * T for _ in range(T)]
    for x in range(T):
        cima, baixo = beira[0][x], beira[T - 1][x]
        topo = 1 if cima < 0.42 else 2 if cima < 0.86 else 3
        fundo = 14 if baixo < 0.42 else 13 if baixo < 0.86 else 12
        for y in range(topo, fundo + 1):
            mascara[y][x] = True
    return mascara, tom


def graos(img, rng, onde, chance, tons=(1, 2)):
    """Grãos soltos de cinza nas posições dadas, só onde ainda não há nada."""
    for x, y in onde:
        if img.getpixel((x, y))[3] == 0 and rng.random() < chance:
            ponto(img, x, y, CINZA[rng.choice(tons)])


def cinza_espalhada_intacta():
    mascara, tom = faixa()
    img = pintar_cinza(mascara, tom)
    # uns grãos soltos nas duas beiradas
    graos(img, sorteio("intacta"), [(x, y) for y in (0, 15) for x in range(T)], 0.2)
    return img


def cinza_espalhada_riscada():
    img = cinza_espalhada_intacta()
    mascara, _ = faixa()
    # três arranhões quase paralelos, de cima a baixo, como de garra; as pontas afinam
    for x0, y0, y1 in ((2, 1, 12), (6, 2, 14), (10, 1, 13)):
        for y in range(y0, y1 + 1):
            x = x0 + round((y - y0) * 0.3)
            if mascara[y][x]:
                ponto(img, x, y, CINZA[0] if y in (y0, y1) else RISCO)
    return img


def cinza_espalhada_gasta():
    mascara, tom = faixa()
    rng = sorteio("gasta")
    mancha = campo_suave(rng)
    for y in range(T):
        for x in range(T):
            # buracos pequenos, meio agrupados: ruído amaciado misturado com chuvisco
            falha = 0.5 * mancha[y][x] + 0.5 * rng.random()
            if falha < 0.44 or y in (1, 14):
                mascara[y][x] = False
    img = pintar_cinza(mascara, tom, contorno=False, apagar=1)
    graos(img, rng, [(x, y) for y in (1, 14) for x in range(T)], 0.2, tons=(0, 1))
    return img


def cinza_espalhada_rompida():
    mascara, tom = faixa()
    rng = sorteio("rompida")
    for y in range(T):
        # o vão: limpo, no meio, atravessando a faixa de cima a baixo
        esquerda = 6 - (1 if rng.random() < 0.2 else 0)
        direita = 9 + (1 if rng.random() < 0.2 else 0)
        for x in range(esquerda, direita + 1):
            mascara[y][x] = False
        # perto do vão as beiradas da faixa ficaram desfeitas
        if y in (1, 2, 13, 14):
            for x in (3, 4, 5, 10, 11, 12):
                if rng.random() < (0.7 if y in (1, 14) else 0.35):
                    mascara[y][x] = False
    img = pintar_cinza(mascara, tom)
    # a cinza que estava no vão foi empurrada para os lados: grãos soltos em volta da boca
    boca = [(x, y) for y in (0, 1, 14, 15) for x in (1, 2, 3, 4, 5, 10, 11, 12, 13, 14)]
    graos(img, rng, boca, 0.5, tons=(1, 2, 3))
    return img


def cinza_espalhada_pegada():
    # Pé direito, comprido demais e estreito: quatro dedos finos e longos, soltos da planta;
    # a planta afina no arco (do lado de dentro, à esquerda) e volta a abrir no calcanhar.
    # Aponta para o norte.
    linhas = [
        "......c.........",
        "...c..c.c.......",
        "...c..c.c..c....",
        "....c.c.c.c.....",
        "....b.b.b.b.....",
        "................",
        ".....bcdcb......",
        ".....cdddc......",
        ".....cdddc......",
        "......cddb......",
        ".......cdb......",
        ".......cdb......",
        ".......cdc......",
        "......cddc......",
        "......cddc......",
        ".......cc.......",
    ]
    img = de_grade(linhas, {"b": CINZA[1], "c": CINZA[2], "d": CINZA[4]})
    # um pouco de pó em volta: sem encostar na planta e bem longe dos dedos, para não embolar
    rng = sorteio("pegada/pó")

    def livre(x, y, raio):
        return not any(
            0 <= y + dy < T and 0 <= x + dx < T and linhas[y + dy][x + dx] != "."
            for dy in range(-raio, raio + 1) for dx in range(-raio, raio + 1)
        )

    em_volta = [(x, y) for y in range(T) for x in range(T) if livre(x, y, 2 if y < 7 else 1)]
    graos(img, rng, em_volta, 0.09)
    return img


# ---------------------------------------------------------------------------------------
# Bloco: lampião pálido (quatro chamas)
# ---------------------------------------------------------------------------------------

# O que vai dentro do vidro: 4 colunas por 5 linhas, uma grade por chama.
VIDRO_POR_CHAMA = {
    # chama cheia, parada
    "calma": (
        ["1221",
         "2332",
         "2432",
         "2342",
         "1221"],
        {"1": CHAMA[0], "2": CHAMA[1], "3": CHAMA[2], "4": CHAMA[3]},
    ),
    # chama menor e torta, o vidro já meio escuro
    "inquieta": (
        ["vv1v",
         "vv21",
         "v24v",
         "v32v",
         "v1vv"],
        {"v": VIDRO_MORNO, "1": CHAMA[0], "2": CHAMA[1], "3": CHAMA[2], "4": CHAMA[3]},
    ),
    # chama pequena e fria, vidro escuro em volta
    "fria": (
        ["kkkk",
         "kk1k",
         "k13k",
         "k22k",
         "kKKk"],
        {"k": VIDRO_FRIO, "K": VIDRO_FRIO_CLARO, "1": FRIA[0], "2": FRIA[1], "3": FRIA[2]},
    ),
    # sem chama: vidro escuro e um resto de cinza no fundo
    "apagada": (
        ["kKkk",
         "Kkkk",
         "kkkk",
         "kkka",
         "aAAa"],
        {"k": VIDRO, "K": VIDRO_CLARO, "a": CINZA[0], "A": CINZA[2]},
    ),
}


def lampiao_palido(chama):
    """Textura de bloco no mapa UV do lampião do jogo (template_lantern e
    template_hanging_lantern):

    - colunas 1..4, linhas 0..1: lateral da tampa;
    - colunas 0..5, linhas 2..8: lateral do corpo (linha 2 e linha 8 são os aros);
    - colunas 0..5, linhas 9..14: tampo e fundo do corpo; o miolo 4x4 é o tampo da tampa;
    - colunas 11..13: a alça (de pé) e os elos da corrente (pendurado). De pé o jogo usa
      as linhas 1..2 e 10..11; pendurado usa 1..4 e 6..11.
    """
    armacao = [
        ".ECCE...........",
        ".nNNn......CEE..",
        "ECCCCE.....E.F..",
        "F....F.....E.F..",
        "E....E.....FEF..",
        "E....E..........",
        "E....E.....E.F..",
        "F....F.....FEF..",
        "ECCCCE..........",
        "EEeeEE..........",
        "EeCCeE.....CEE..",
        "eCCCCe.....E.F..",
        "eCCCCe..........",
        "EeCCeE..........",
        "EEeeEE..........",
        "................",
    ]
    cores = {
        "F": FERRO_FUNDO, "E": FERRO_ESCURO, "e": FERRO, "C": FERRO_CLARO,
        "N": ANEL, "n": ANEL_SOMBRA,
    }
    img = de_grade(armacao, cores)
    grade, cores_do_vidro = VIDRO_POR_CHAMA[chama]
    for j, linha in enumerate(grade):
        for i, letra in enumerate(linha):
            ponto(img, 1 + i, 3 + j, cores_do_vidro[letra])
    return img


# ---------------------------------------------------------------------------------------
# Bloco: tigela de oferenda
# ---------------------------------------------------------------------------------------

def tigela_oferenda():
    """Uma textura só para a tigela inteira. O modelo usa:

    - colunas 3..12, linhas 3..12: a tigela vista de cima (aro claro, miolo em sombra);
    - colunas 3..12, linhas 0..1: lateral de fora do aro;
    - colunas 4..11, linha 2: lateral do corpo, debaixo do aro;
    - colunas 5..10, linha 13: lateral do pé;
    - colunas 3..12, linhas 14..15: lateral de dentro do aro.

    O resto é barro liso: as partículas do bloco sorteiam pedaços da textura toda.
    """
    rng = sorteio("tigela")
    img = nova()
    for y in range(T):
        for x in range(T):
            ponto(img, x, y, BARRO[3] if rng.random() < 0.72 else BARRO[2])

    # vista de cima
    for y in range(3, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (3, 12):
                cor = BARRO[4]                                          # aro
            else:
                # o miolo fica na sombra do aro, mais funda junto das paredes norte e oeste
                sombra = min(x - 4, y - 4)
                cor = BARRO[0] if sombra == 0 else BARRO[1] if sombra == 1 else BARRO[2]
            ponto(img, x, y, cor)

    # lateral de fora do aro, lateral do corpo, do pé e de dentro do aro
    for x in range(3, 13):
        ponto(img, x, 0, BARRO[4])
        ponto(img, x, 1, BARRO[3])
        ponto(img, x, 14, BARRO[1])
        ponto(img, x, 15, BARRO[0])
    for x in range(4, 12):
        ponto(img, x, 2, BARRO[2])
    for x in range(5, 11):
        ponto(img, x, 13, BARRO[1])

    # craquelado discreto: poucos traços finos, um tom abaixo do barro em volta
    for x, y, tom in (
        (5, 0, 3), (5, 1, 2), (6, 1, 2),         # lateral de fora: uma trinca que desce
        (10, 0, 3), (11, 1, 2),
        (6, 3, 3), (3, 8, 3), (12, 6, 3),        # aro
        (9, 12, 3), (4, 12, 3),
        (8, 2, 1),                               # corpo
        (8, 7, 1), (9, 8, 1), (9, 9, 1),         # fundo
    ):
        ponto(img, x, y, BARRO[tom])
    return img


def tigela_cinzas():
    """A cinza de dentro da tigela. O modelo mostra só o miolo (colunas e linhas 4..11);
    o resto é cinza também, para as partículas."""
    rng = sorteio("tigela_cinzas")
    mancha = campo_suave(rng)
    img = nova()
    for y in range(T):
        for x in range(T):
            t = 0.6 * mancha[y][x] + 0.4 * rng.random()
            i = 2 if t < 0.36 else 3 if t < 0.78 else 4
            # junto das paredes norte e oeste a cinza fica na sombra do aro
            if 4 <= x <= 11 and 4 <= y <= 11 and min(x - 4, y - 4) == 0:
                i -= 2
            ponto(img, x, y, CINZA[max(0, i)])
    # uns restos queimados
    for x, y in ((6, 10), (9, 6)):
        ponto(img, x, y, CINZA[0])
    return img


# ---------------------------------------------------------------------------------------
# Itens
# ---------------------------------------------------------------------------------------

def item_lampiao_palido():
    # O lampião aceso, de frente: argola, tampa, anel de osso, vidro com a chama calma.
    linhas = [
        "................",
        ".......EC.......",
        "......E..F......",
        "......E..F......",
        ".......EF.......",
        ".....FECCEF.....",
        ".....nNNNNn.....",
        "....ECCCCCCE....",
        "....F122221F....",
        "....E223322E....",
        "....E234432E....",
        "....E234432E....",
        "....E223322E....",
        "....F122221F....",
        "....FECCCCEF....",
        "................",
    ]
    return de_grade(linhas, {
        "F": FERRO_FUNDO, "E": FERRO_ESCURO, "C": FERRO_CLARO,
        "N": ANEL, "n": ANEL_SOMBRA,
        "1": CHAMA[0], "2": CHAMA[1], "3": CHAMA[2], "4": CHAMA[3],
    })


def item_caixa_de_musica():
    # Caixinha de madeira escura e gasta, tampa entreaberta (dobradiça à esquerda),
    # manivela de metal na lateral direita.
    linhas = [
        "................",
        "........ooo.....",
        ".....oooLLLo....",
        "..oooLLLlllo....",
        ".oLLLllloooo....",
        ".ollloookkko....",
        ".ooookkkkgko..MM",
        ".oWWWWWWWWWo..n.",
        ".owwwwwwwwwo..n.",
        ".owwdwwwswwonnM.",
        ".owwwwwwwdwo....",
        ".owswwwwwwwo....",
        ".owwwwdwwwwo....",
        ".odddddddddo....",
        "..ooooooooo.....",
        "................",
    ]
    return de_grade(linhas, {
        "o": MADEIRA_CONTORNO, "L": MADEIRA_CLARA, "l": MADEIRA,
        "W": MADEIRA_CLARA, "w": MADEIRA, "d": MADEIRA_ESCURA, "s": MADEIRA_GASTA,
        "k": VAO, "g": METAL,
        "n": METAL_ESCURO, "M": METAL_CLARO,
    })


def item_ossos_de_agouro():
    # Três ossinhos finos em leque, amarrados perto da ponta de baixo; o da direita rachado.
    linhas = [
        "................",
        "B.B...B.B....B.B",
        ".BBb..BBB....BBb",
        "..Bb...Bb....Bb.",
        "..Bb...Bb....B..",
        "...Bb..Bb...Bb..",
        "...Bb..Bb..x....",
        "....Bb.Bb.Bb....",
        "....Bb.Bb.Bb....",
        ".....B.B.B......",
        ".....B.B.B......",
        ".....rRrRr......",
        "......BBB.r.....",
        ".....B.B.B.r....",
        "....BB.B.BBb....",
        "................",
    ]
    return de_grade(linhas, {
        "B": OSSO, "b": OSSO_SOMBRA, "x": OSSO_RACHA,
        "r": FIO, "R": FIO_ESCURO,
    })


# ---------------------------------------------------------------------------------------
# Lista e gravação
# ---------------------------------------------------------------------------------------

def todas():
    """(caminho dentro de textures/, imagem), na ordem em que aparecem na prancha."""
    lista = [
        ("block/cinza_espalhada_intacta.png", cinza_espalhada_intacta()),
        ("block/cinza_espalhada_riscada.png", cinza_espalhada_riscada()),
        ("block/cinza_espalhada_gasta.png", cinza_espalhada_gasta()),
        ("block/cinza_espalhada_rompida.png", cinza_espalhada_rompida()),
        ("block/cinza_espalhada_pegada.png", cinza_espalhada_pegada()),
    ]
    for chama in ("calma", "inquieta", "fria", "apagada"):
        lista.append((f"block/lampiao_palido_{chama}.png", lampiao_palido(chama)))
    lista += [
        ("block/tigela_oferenda.png", tigela_oferenda()),
        ("block/tigela_cinzas.png", tigela_cinzas()),
        ("item/lampiao_palido.png", item_lampiao_palido()),
        ("item/caixa_de_musica.png", item_caixa_de_musica()),
        ("item/ossos_de_agouro.png", item_ossos_de_agouro()),
    ]
    return lista


def gravar_prancha(lista, destino, escala=8, margem=8):
    """Todas lado a lado, ampliadas, em duas fileiras: fundo de inventário e fundo escuro."""
    passo = T * escala + margem
    fundos = [(139, 139, 139, 255), (52, 50, 54, 255)]
    prancha = Image.new("RGBA", (margem + passo * len(lista), margem + passo * len(fundos)))
    for linha, fundo in enumerate(fundos):
        prancha.paste(fundo, (0, linha * passo, prancha.width, (linha + 1) * passo + margem))
        for coluna, (_, img) in enumerate(lista):
            grande = img.resize((T * escala, T * escala), Image.NEAREST)
            prancha.paste(grande, (margem + coluna * passo, margem + linha * passo), grande)
    destino.parent.mkdir(parents=True, exist_ok=True)
    prancha.save(destino)


def main():
    sys.stdout.reconfigure(encoding="utf-8")  # acentos certos também quando a saída é redirecionada
    parser = argparse.ArgumentParser(description="Gera as texturas dos blocos e itens novos.")
    parser.add_argument("--prancha", type=Path, metavar="ARQUIVO.png",
                        help="grava também uma prancha ampliada 8x (use um caminho fora do repositório)")
    args = parser.parse_args()

    lista = todas()
    for relativo, img in lista:
        assert img.size == (T, T) and img.mode == "RGBA", relativo
        destino = TEXTURAS / relativo
        destino.parent.mkdir(parents=True, exist_ok=True)
        img.save(destino)
        print(f"gravada  {destino.relative_to(RAIZ).as_posix()}")
    if args.prancha:
        gravar_prancha(lista, args.prancha)
        print(f"prancha  {args.prancha}  (da esquerda para a direita, na ordem acima)")


if __name__ == "__main__":
    main()
