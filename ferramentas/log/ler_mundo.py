"""Desenha os blocos em volta de um ponto de um mundo salvo (SPOILERS: mostra a base do dono).

    python -I ferramentas/log/ler_mundo.py <cópia de r.X.Z.mca> x y z [raio]
    python -I ferramentas/log/ler_mundo.py exportar <cópia de r.X.Z.mca> x0 y0 z0 x1 y1 z1 <saída.txt>

O segundo jeito (0.9.0-alpha19) grava uma caixa de blocos num arquivo que um teste de servidor monta
(src/gametest/resources/terrenos e Terreno.java): o que depende de terreno passa a ser testado no chão em que
o dono jogou, e não num gramado raspado. O arquivo não leva as coordenadas de origem.

Serve para conferir, contra o terreno em que o dono jogou de verdade, por que alguma coisa não achou lugar
(foi assim que apareceu, em 11/10/2026, que capim e flor derrubavam toda a procura de lugar do mod).

Copie antes o arquivo de run/saves/<mundo>/dimensions/minecraft/overworld/region/ para fora da pasta do jogo:
isto só lê, mas o jogo pode estar com o mundo aberto. O arquivo certo é r.A.B.mca, com A = x de bloco dividido por 512 e B = z dividido por 512, arredondados para baixo.
Entende o formato de chunk com "sections" e "block_states" (paleta, e longs em que um bloco não atravessa de
um long para o outro). A lista do que "não tem corpo" é aproximada: é uma régua, não o jogo.

Legenda: . ar   , planta ou coisa sem corpo   # sólido   W madeira   G vidro   D porta   F cerca ou muro
         t tocha ou lanterna   p caminho   f terra arada   L folhas   ~ água   ? chunk fora do arquivo
"""
import gzip
import io
import struct
import sys
import zlib


def ler_nbt(dados):
    f = io.BytesIO(dados)

    def nome():
        n = struct.unpack('>H', f.read(2))[0]
        return f.read(n).decode('utf-8', 'replace')

    def valor(tipo):
        if tipo == 1:
            return struct.unpack('>b', f.read(1))[0]
        if tipo == 2:
            return struct.unpack('>h', f.read(2))[0]
        if tipo == 3:
            return struct.unpack('>i', f.read(4))[0]
        if tipo == 4:
            return struct.unpack('>q', f.read(8))[0]
        if tipo == 5:
            return struct.unpack('>f', f.read(4))[0]
        if tipo == 6:
            return struct.unpack('>d', f.read(8))[0]
        if tipo == 7:
            n = struct.unpack('>i', f.read(4))[0]
            return f.read(n)
        if tipo == 8:
            return nome()
        if tipo == 9:
            t = struct.unpack('>b', f.read(1))[0]
            n = struct.unpack('>i', f.read(4))[0]
            return [valor(t) for _ in range(n)]
        if tipo == 10:
            d = {}
            while True:
                t = struct.unpack('>b', f.read(1))[0]
                if t == 0:
                    return d
                k = nome()
                d[k] = valor(t)
        if tipo == 11:
            n = struct.unpack('>i', f.read(4))[0]
            return list(struct.unpack('>%di' % n, f.read(4 * n)))
        if tipo == 12:
            n = struct.unpack('>i', f.read(4))[0]
            return list(struct.unpack('>%dq' % n, f.read(8 * n)))
        raise ValueError('tipo %d' % tipo)

    t = struct.unpack('>b', f.read(1))[0]
    nome()
    return valor(t)


class Regiao:
    def __init__(self, caminho, rx, rz):
        self.dados = open(caminho, 'rb').read()
        self.rx, self.rz = rx, rz
        self.chunks = {}

    def chunk(self, cx, cz):
        chave = (cx, cz)
        if chave in self.chunks:
            return self.chunks[chave]
        lx, lz = cx - self.rx * 32, cz - self.rz * 32
        if not (0 <= lx < 32 and 0 <= lz < 32):
            self.chunks[chave] = None
            return None
        i = 4 * (lx + lz * 32)
        off = struct.unpack('>I', b'\x00' + self.dados[i:i + 3])[0]
        if off == 0:
            self.chunks[chave] = None
            return None
        ini = off * 4096
        tam, comp = struct.unpack('>IB', self.dados[ini:ini + 5])
        bruto = self.dados[ini + 5:ini + 4 + tam]
        if comp == 2:
            bruto = zlib.decompress(bruto)
        elif comp == 1:
            bruto = gzip.decompress(bruto)
        elif comp == 3:
            pass
        else:
            raise ValueError('compressão %d não suportada' % comp)
        nbt = ler_nbt(bruto)
        secoes = {}
        for s in nbt.get('sections', []):
            bs = s.get('block_states')
            if not bs:
                continue
            paleta = [p['Name'] for p in bs['palette']]
            secoes[s['Y']] = (paleta, bs.get('data'))
        self.chunks[chave] = secoes
        return secoes

    def bloco(self, x, y, z):
        secoes = self.chunk(x >> 4, z >> 4)
        if secoes is None:
            return '?'
        s = secoes.get(y >> 4)
        if s is None:
            return 'minecraft:air'
        paleta, data = s
        if data is None or len(paleta) == 1:
            return paleta[0]
        bits = max(4, (len(paleta) - 1).bit_length())
        por_long = 64 // bits
        idx = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15)
        longo = data[idx // por_long] & 0xFFFFFFFFFFFFFFFF
        return paleta[(longo >> ((idx % por_long) * bits)) & ((1 << bits) - 1)]


SEM_CORPO = ('air', 'cave_air', 'short_grass', 'tall_grass', 'fern', 'large_fern', 'dandelion', 'poppy', 'cornflower',
             'oxeye_daisy', 'azure_bluet', 'allium', 'tulip', 'torch', 'wall_torch', 'wheat', 'carrots', 'potatoes',
             'beetroots', 'sapling', 'sunflower', 'lilac', 'peony', 'rose_bush', 'bush', 'leaf_litter', 'wildflowers',
             'dead_bush', 'vine', 'sugar_cane', 'snow', 'rail', 'redstone', 'button', 'lever', 'sign', 'banner',
             'pressure_plate', 'firefly_bush')


def sem_corpo(nome):
    n = nome.split(':')[-1]
    return any(n == s or n.endswith('_' + s) or n.endswith(s) and s in ('tulip', 'sapling', 'sign', 'button', 'banner', 'pressure_plate') for s in SEM_CORPO)


def simbolo(nome):
    n = nome.split(':')[-1]
    if n in ('air', 'cave_air'):
        return '.'
    if 'glass' in n:
        return 'G'
    if 'door' in n:
        return 'D'
    if 'fence' in n or 'wall' in n and 'torch' not in n:
        return 'F'
    if 'torch' in n or 'lantern' in n:
        return 't'
    if sem_corpo(nome):
        return ','
    if 'water' in n:
        return '~'
    if 'leaves' in n:
        return 'L'
    if 'log' in n or 'planks' in n or 'stairs' in n or 'slab' in n:
        return 'W'
    if 'farmland' in n:
        return 'f'
    if 'path' in n:
        return 'p'
    return '#'


def exportar(caminho, x0, y0, z0, x1, y1, z1, saida):
    """Grava uma caixa de blocos num arquivo de texto que um teste de servidor monta (ver Terreno.java).

    Só os nomes dos blocos, em ordem (y, depois z, depois x), com repetições agrupadas. As coordenadas do mundo
    de origem NÃO vão para o arquivo: o repositório é público, e o lugar da base do dono não é da conta de ninguém.
    As propriedades dos blocos (lado da tocha, metade da porta) também não vão: o teste monta sem atualizar os
    vizinhos, e para saber onde alguém cabe basta o bloco.
    """
    partes = caminho.replace('\\', '/').split('/')[-1].split('.')
    reg = Regiao(caminho, int(partes[1]), int(partes[2]))
    x0, x1 = min(x0, x1), max(x0, x1)
    y0, y1 = min(y0, y1), max(y0, y1)
    z0, z1 = min(z0, z1), max(z0, z1)
    paleta = {}
    corridas = []
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                nome = reg.bloco(x, y, z)
                if nome == '?':
                    raise SystemExit('a caixa sai do arquivo de região em x=%d z=%d: escolha uma caixa que caiba em %s' % (x, z, caminho))
                indice = paleta.setdefault(nome, len(paleta))
                if corridas and corridas[-1][0] == indice:
                    corridas[-1][1] += 1
                else:
                    corridas.append([indice, 1])
    with io.open(saida, 'w', encoding='utf-8', newline='\n') as arquivo:
        arquivo.write('# Terreno de teste exportado por ferramentas/log/ler_mundo.py. Sem coordenadas do mundo de origem.\n')
        arquivo.write('tamanho %d %d %d\n' % (x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1))
        arquivo.write('paleta %d\n' % len(paleta))
        for nome, indice in sorted(paleta.items(), key=lambda par: par[1]):
            arquivo.write('%d %s\n' % (indice, nome))
        arquivo.write('blocos %d\n' % len(corridas))
        for i in range(0, len(corridas), 16):
            arquivo.write(' '.join('%d*%d' % (indice, vezes) for indice, vezes in corridas[i:i + 16]) + '\n')
    print('%d x %d x %d blocos, %d tipos, em %s' % (x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, len(paleta), saida))


if __name__ == '__main__':
    if sys.argv[1] == 'exportar':
        exportar(sys.argv[2], *[int(v) for v in sys.argv[3:9]], sys.argv[9])
        sys.exit(0)
    caminho = sys.argv[1]
    x0, y0, z0 = int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
    raio = int(sys.argv[5]) if len(sys.argv) > 5 else 12
    partes = caminho.replace('\\', '/').split('/')[-1].split('.')
    reg = Regiao(caminho, int(partes[1]), int(partes[2]))
    for dy in (2, 1, 0, -1):
        y = y0 + dy
        print('y=%d (x de %d a %d, da esquerda para a direita; z de %d a %d, de cima para baixo)' % (y, x0 - raio, x0 + raio, z0 - raio, z0 + raio))
        for z in range(z0 - raio, z0 + raio + 1):
            print(''.join(simbolo(reg.bloco(x, y, z)) for x in range(x0 - raio, x0 + raio + 1)))
        print()
