"""Move uma seção do Diretor.java para uma classe própria do mesmo pacote, sem reescrever o corpo.

Uso: python extrair_secao.py <pasta do pacote assombracao> <NomeDaClasse> <trecho do banner> <arquivo com o javadoc>

A seção é a que começa no banner "// ===== / // <texto> / // =====" e vai até o banner seguinte.
Depois de rodar: ./gradlew build e verificar_movimento.py.

O corpo dos métodos é recortado e colado como está. As únicas mudanças no texto são:
  - prefixo "Diretor." nas chamadas a membros que continuam no Diretor;
  - prefixo "<NomeDaClasse>." no Diretor (e nos outros arquivos do pacote) para o que foi movido;
  - "private static" -> "static" só onde a outra classe precisa enxergar.
Strings e comentários nunca são tocados.
"""
import pathlib
import re
import sys

DECL = re.compile(r'^\t(?:(private|public|protected) )?static [\w<>\[\], .@?]+ (\w+)\(')
CAMPO = re.compile(r'^\t(?:(private|public|protected) )?static (?:final )?[\w<>\[\], .]+ (\w+)(?: = .*|;)\s*$')
TIPO = re.compile(r'^\t(?:(private|public|protected) )?(?:static )?(?:final )?(?:record|enum|class|interface) (\w+)')


def segmentos(texto):
    """Divide em (eh_codigo, trecho). Strings, chars e comentários ficam fora do código."""
    saida = []
    i, n, ini = 0, len(texto), 0

    def fecha(j, eh_codigo):
        nonlocal ini
        if j > ini:
            saida.append((eh_codigo, texto[ini:j]))
        ini = j

    while i < n:
        c = texto[i]
        d = texto[i:i + 2]
        if d == '//':
            fecha(i, True)
            j = texto.find('\n', i)
            i = n if j < 0 else j
            fecha(i, False)
        elif d == '/*':
            fecha(i, True)
            j = texto.find('*/', i + 2)
            i = n if j < 0 else j + 2
            fecha(i, False)
        elif texto.startswith('"""', i):
            fecha(i, True)
            j = texto.find('"""', i + 3)
            i = n if j < 0 else j + 3
            fecha(i, False)
        elif c in '"\'':
            fecha(i, True)
            j = i + 1
            while j < n and texto[j] != c:
                if texto[j] == '\\':
                    j += 1
                j += 1
            i = min(j + 1, n)
            fecha(i, False)
        else:
            i += 1
    fecha(n, True)
    return saida


def no_codigo(texto, fn):
    return ''.join(fn(t) if eh else t for eh, t in segmentos(texto))


def so_codigo(texto):
    return ''.join(t if eh else ' ' for eh, t in segmentos(texto))


def chama(codigo, nome):
    return re.search(r'(?<![\w.])' + re.escape(nome) + r'\(', codigo) is not None


def cita(codigo, nome):
    return re.search(r'(?<![\w.])' + re.escape(nome) + r'\b', codigo) is not None


def qualificador(nomes, prefixo, so_chamada):
    if not nomes:
        return lambda t: t
    fim = r'(?=\()' if so_chamada else r'\b'
    padrao = re.compile(r'(?<![\w.])(' + '|'.join(map(re.escape, sorted(nomes))) + r')' + fim)
    return lambda t: padrao.sub(lambda m: prefixo + '.' + m.group(1), t)


def abrir(linhas, nomes, regex):
    """private static -> static nas declarações dos nomes dados. Devolve quantas mudou."""
    mudou = 0
    for i, l in enumerate(linhas):
        m = regex.match(l)
        if m and m.group(2) in nomes and m.group(1) == 'private':
            linhas[i] = l.replace('\tprivate ', '\t', 1)
            mudou += 1
    return mudou


def ler(arq):
    bruto = arq.read_bytes().decode('utf-8')
    nl = '\r\n' if '\r\n' in bruto else '\n'
    return bruto.replace('\r\n', '\n'), nl


def gravar(arq, texto, nl):
    arq.write_bytes(texto.replace('\n', nl).encode('utf-8'))


def main():
    pasta = pathlib.Path(sys.argv[1])
    classe = sys.argv[2]
    banner = sys.argv[3]
    doc = pathlib.Path(sys.argv[4]).read_text(encoding='utf-8').rstrip('\n').split('\n')

    arq_diretor = pasta / 'Diretor.java'
    arq_novo = pasta / (classe + '.java')
    if arq_novo.exists():
        sys.exit('ja existe: ' + str(arq_novo))

    texto, nl = ler(arq_diretor)
    linhas = texto.split('\n')

    # --- localiza a seção: régua, banner, régua ... até a régua da seção seguinte ---
    achados = [i for i, l in enumerate(linhas) if l.startswith('\t// ') and banner in l]
    if len(achados) != 1:
        sys.exit('banner nao encontrado exatamente uma vez: %r (%d)' % (banner, len(achados)))
    b = achados[0]
    if not (linhas[b - 1].startswith('\t// =====') and linhas[b + 1].startswith('\t// =====')):
        sys.exit('banner sem as duas reguas')
    ini = b - 1
    fim = next(i for i in range(b + 2, len(linhas)) if linhas[i].startswith('\t// ====='))

    antes = linhas[:ini]
    corpo = linhas[b + 2:fim]
    depois = linhas[fim:]
    while corpo and not corpo[0].strip():
        corpo.pop(0)
    while corpo and not corpo[-1].strip():
        corpo.pop()
    while antes and not antes[-1].strip():
        antes.pop()
    resto = antes + [''] + depois

    # --- quem declara o quê ---
    do_bloco = [m.group(2) for l in corpo if (m := DECL.match(l))]
    set_bloco = set(do_bloco)
    do_resto = {m.group(2) for l in resto if (m := DECL.match(l))}
    if set_bloco & do_resto:
        sys.exit('nome declarado dentro e fora da secao: %s' % sorted(set_bloco & do_resto))

    cod_bloco = so_codigo('\n'.join(corpo))
    cod_resto = so_codigo('\n'.join(resto))
    for nome in set_bloco | do_resto:
        if re.search(r'::' + re.escape(nome) + r'\b', cod_bloco) and nome in do_resto:
            sys.exit('referencia de metodo (::) a ' + nome + ' na secao: tratar a mao')
        if re.search(r'::' + re.escape(nome) + r'\b', cod_resto) and nome in set_bloco:
            sys.exit('referencia de metodo (::) a ' + nome + ' no Diretor: tratar a mao')

    metodos_do_diretor = {n for n in do_resto if chama(cod_bloco, n)}
    campos_resto = {m.group(2) for l in resto if (m := CAMPO.match(l))}
    campos_do_diretor = {n for n in campos_resto if cita(cod_bloco, n)}

    # --- tipos aninhados do Diretor usados pela seção: move se só ela usa, senão qualifica ---
    tipos_movidos = []
    tipos_do_diretor = set()
    for i, l in enumerate(list(resto)):
        m = TIPO.match(l)
        if not m or not cita(cod_bloco, m.group(2)):
            continue
        nome = m.group(2)
        j = resto.index(l)
        k = j
        if l.rstrip().endswith('{'):
            while resto[k] != '\t}':
                k += 1
        sem_decl = resto[:j] + resto[k + 1:]
        if cita(so_codigo('\n'.join(sem_decl)), nome):
            tipos_do_diretor.add(nome)
        else:
            tipos_movidos.append((nome, resto[j:k + 1]))
            if j > 0 and not sem_decl[j - 1].strip() and j < len(sem_decl) and not sem_decl[j].strip():
                del sem_decl[j]
            resto = sem_decl
    cod_resto = so_codigo('\n'.join(resto))

    # --- outros arquivos do pacote que chamavam Diretor.<movido>( ---
    usados_de_fora = {n for n in set_bloco if chama(cod_resto, n)}
    outros = {}
    padrao_fora = re.compile(r'(?<![\w.])Diretor\.(' + '|'.join(map(re.escape, sorted(set_bloco))) + r')(?=\()')
    for arq in sorted(pasta.glob('*.java')):
        if arq in (arq_diretor, arq_novo):
            continue
        t, nl_o = ler(arq)
        vistos = set()

        def troca(m):
            vistos.add(m.group(1))
            return classe + '.' + m.group(1)

        t2 = no_codigo(t, lambda trecho: padrao_fora.sub(troca, trecho))
        if t2 != t:
            outros[arq] = (t2, nl_o)
            usados_de_fora |= vistos

    # --- transforma a seção ---
    q_met = qualificador(metodos_do_diretor, 'Diretor', True)
    q_id = qualificador(campos_do_diretor | tipos_do_diretor, 'Diretor', False)
    novo_corpo = no_codigo('\n'.join(corpo), lambda t: q_id(q_met(t))).split('\n')
    abertos_novo = abrir(novo_corpo, usados_de_fora, DECL)

    # --- transforma o Diretor ---
    q_novo = qualificador(usados_de_fora, classe, True)
    resto = no_codigo('\n'.join(resto), q_novo).split('\n')
    abertos_dir = abrir(resto, metodos_do_diretor, DECL)
    abertos_dir += abrir(resto, campos_do_diretor, CAMPO)
    abertos_dir += abrir(resto, tipos_do_diretor, TIPO)

    # --- imports: a classe nova leva os que usa; o Diretor perde os que deixou de usar ---
    idx = [i for i, l in enumerate(resto) if l.startswith('import ')]
    p, u = idx[0], idx[-1]
    grupos, atual = [], []
    for l in resto[p:u + 1]:
        if l.startswith('import '):
            atual.append(l)
        elif atual:
            grupos.append(atual)
            atual = []
    if atual:
        grupos.append(atual)

    def simples(l):
        return l.rstrip(';').split('.')[-1]

    tipos_txt = [l for _, ls in tipos_movidos for l in ls]
    cod_novo = so_codigo('\n'.join(tipos_txt + novo_corpo))
    cod_dir = so_codigo('\n'.join(resto[u + 1:]))
    txt_dir = '\n'.join(resto[u + 1:])

    def usado(cod, l):
        return re.search(r'(?<![\w.])' + re.escape(simples(l)) + r'\b', cod) is not None

    imp_novo = [[l for l in g if usado(cod_novo, l)] for g in grupos]
    imp_dir = [[l for l in g if usado(cod_dir, l) or ('{@link ' + simples(l)) in txt_dir] for g in grupos]
    removidos = [l for g, g2 in zip(grupos, imp_dir) for l in g if l not in g2]

    def bloco_imports(gs):
        saida = []
        for g in gs:
            if g:
                saida += g + ['']
        return saida[:-1]

    resto = resto[:p] + bloco_imports(imp_dir) + resto[u + 1:]

    # --- monta a classe nova ---
    novo = ['package com.sussurros.assombracao;', ''] + bloco_imports(imp_novo) + ['']
    novo += doc
    novo += ['final class %s {' % classe, '\tprivate %s() {' % classe, '\t}', '']
    for _, ls in tipos_movidos:
        novo += ls + ['']
    novo += novo_corpo + ['}', '']

    gravar(arq_novo, '\n'.join(novo), nl)
    gravar(arq_diretor, '\n'.join(resto), nl)
    for arq, (t2, nl_o) in outros.items():
        gravar(arq, t2, nl_o)

    print('classe            :', classe)
    print('linhas movidas    :', len(corpo))
    print('metodos movidos   :', ', '.join(do_bloco))
    print('entradas (static) :', ', '.join(sorted(usados_de_fora)), '(%d abertas)' % abertos_novo)
    print('usa do Diretor    :', ', '.join(sorted(metodos_do_diretor)))
    print('campos do Diretor :', ', '.join(sorted(campos_do_diretor)) or '-')
    print('tipos do Diretor  :', ', '.join(sorted(tipos_do_diretor)) or '-')
    print('tipos movidos     :', ', '.join(n for n, _ in tipos_movidos) or '-')
    print('abertos no Diretor:', abertos_dir)
    print('imports removidos :', ', '.join(simples(l) for l in removidos) or '-')
    print('outros arquivos   :', ', '.join(a.name for a in outros) or '-')


if __name__ == '__main__':
    main()
