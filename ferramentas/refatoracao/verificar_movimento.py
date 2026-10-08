"""Confere, de forma independente do extrair_secao.py, que um passo só MOVEU código do Diretor.

Uso: python verificar_movimento.py <repositorio> <commit base> <ClasseNova> [<ClasseNova> ...]

Compara o Diretor.java do commit base com o Diretor.java + as classes novas de agora:
  1. cada método (assinatura + corpo) existe exatamente uma vez antes e depois, com o mesmo texto;
  2. lista toda linha de código que apareceu ou sumiu (o esperado é só o esqueleto das classes
     novas e os banners das seções movidas);
  3. as classes novas não importam nada que o Diretor não importava.
As únicas diferenças toleradas são o prefixo de classe nas chamadas e "private" nas declarações.
"""
import collections
import pathlib
import re
import subprocess
import sys

PACOTE = 'src/main/java/com/sussurros/assombracao'
DECL = re.compile(r'^\t(?:(?:private|public|protected) )?static [\w<>\[\], .@?]+ (\w+)\(')


def normaliza(linha, classes):
    linha = re.sub(r'(?<![\w.])(?:' + '|'.join(classes) + r')\.(?=[A-Za-z_])', '', linha)
    linha = re.sub(r'^\t(?:private|public|protected) ', '\t', linha)
    return linha


def metodos(linhas):
    """nome -> lista de textos (assinatura até a chave que fecha)."""
    saida = collections.defaultdict(list)
    i = 0
    while i < len(linhas):
        m = DECL.match(linhas[i])
        if not m:
            i += 1
            continue
        j = i
        while linhas[j] != '\t}':
            j += 1
        saida[m.group(1)].append('\n'.join(linhas[i:j + 1]))
        i = j + 1
    return saida


def main():
    sys.stdout.reconfigure(encoding='utf-8')
    repo = pathlib.Path(sys.argv[1])
    base = sys.argv[2]
    antes_txt = subprocess.run(['git', '-C', str(repo), 'show', base + ':' + PACOTE + '/Diretor.java'],
                               capture_output=True, check=True).stdout.decode('utf-8').replace('\r\n', '\n')
    pasta = repo / PACOTE
    novas = [pasta / (nome + '.java') for nome in sorted(sys.argv[3:])]
    if not novas:
        sys.exit('informe ao menos uma classe nova')
    classes = ['Diretor'] + [p.stem for p in novas]

    def le(p):
        return p.read_bytes().decode('utf-8').replace('\r\n', '\n').split('\n')

    antes = [normaliza(l, classes) for l in antes_txt.split('\n')]
    depois_por_arquivo = {p.name: [normaliza(l, classes) for l in le(p)] for p in [pasta / 'Diretor.java'] + novas}
    depois = [l for ls in depois_por_arquivo.values() for l in ls]

    erros = 0

    # 1. métodos idênticos
    m_antes = metodos(antes)
    m_depois = collections.defaultdict(list)
    for ls in depois_por_arquivo.values():
        for nome, textos in metodos(ls).items():
            m_depois[nome] += textos
    for nome in sorted(set(m_antes) | set(m_depois)):
        if sorted(m_antes.get(nome, [])) != sorted(m_depois.get(nome, [])):
            erros += 1
            print('METODO DIFERENTE:', nome)
    total = sum(len(v) for v in m_antes.values())

    # 2. nenhuma linha de código a mais ou a menos
    def conta(linhas):
        return collections.Counter(l for l in linhas if l.strip() and not l.startswith('import '))

    c_antes, c_depois = conta(antes), conta(depois)
    sumiram = c_antes - c_depois
    surgiram = c_depois - c_antes
    print('--- linhas que sumiram (esperado: só reguas e banners das secoes movidas) ---')
    for l, n in sorted(sumiram.items()):
        print('%3dx %s' % (n, l))
    print('--- linhas que surgiram (esperado: só o esqueleto das classes novas) ---')
    for l, n in sorted(surgiram.items()):
        print('%3dx %s' % (n, l))

    # 3. imports das classes novas vêm todos do Diretor original
    imp_antes = {l for l in antes if l.startswith('import ')}
    for nome, ls in depois_por_arquivo.items():
        extra = [l for l in ls if l.startswith('import ') and l not in imp_antes]
        if extra:
            erros += 1
            print('IMPORT NOVO em', nome, extra)

    print('--- resumo ---')
    print('metodos comparados:', total, '| classes novas:', ', '.join(p.stem for p in novas) or '-')
    print('RESULTADO:', 'OK (metodos identicos)' if erros == 0 else 'FALHOU (%d)' % erros)
    sys.exit(1 if erros else 0)


if __name__ == '__main__':
    main()
