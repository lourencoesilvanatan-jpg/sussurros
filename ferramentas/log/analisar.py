"""Lê o sussurros-debug.log e monta um relatório em Markdown, comparável entre sessões.

Uso:
    python ferramentas/log/analisar.py run/sussurros-debug.log
    python ferramentas/log/analisar.py run/sussurros-debug.log --jogador Jogador --saida relatorio.md

O relatório TEM SPOILERS: ele diz o que aconteceu na sessão. É para quem programa, não para o dono.

O que ele responde:
  - em que fase o jogador esteve, e por quanto tempo em cada estado do Diretor;
  - quantas coisas o jogador pôde perceber por hora, de que fonte, e com que intervalo entre uma e outra
    (é o "ritmo": muitas fontes independentes somam, e ninguém decide a soma);
  - quanto a criatura e a cantiga apareceram (o que aparece demais deixa de assustar);
  - como o jogador reagiu (a confiança das leituras);
  - o que os itens e a Conta fizeram;
  - caçadas, capturas, visitas ao outro lado;
  - o que não coube (seleção que falhou, falta de lugar).

Só usa a biblioteca padrão do Python.
"""
import argparse
import collections
import io
import re
import statistics
import sys

LINHA = re.compile(r'^\[(\d+)s\]\[([^\]]+)\] (.*)$')

# Um buraco maior que isto sem nenhuma linha quer dizer que o jogador estava fora do jogo
# (com ele dentro, sai uma linha de situação a cada minuto).
FORA_DO_JOGO = 150

# O que conta como "saída perceptível": uma coisa que o mod empurra para o jogador, fora do que é contínuo
# (cor, fundo sonoro) e fora da resposta direta a uma ação dele (usar um item).
# Cada entrada: (fonte, expressão que reconhece a linha).
PERCEPTIVEIS = [
    ('evento', re.compile(r'^EVENTO (\S+) \(estado=')),
    ('pressagio', re.compile(r'^PRESSAGIO tipo=(\S+)')),
    ('atmosfera', re.compile(r'^ATMOSFERA cena=(\S+) teste=nao')),
    ('cena', re.compile(r'^CENA id=\S+ tipo=(\S+) INICIO teste=nao')),
    ('veu', re.compile(r'^VEU abriu .*teste=nao')),
    ('conta', re.compile(r'^CONTA aviso=(\d)/3')),
    ('cacada', re.compile(r'^PRENUNCIO real=sim')),
    ('contato', re.compile(r'^CONTATO tentativa=(\d+)')),
    ('captura', re.compile(r'^CAPTURA inicio .*teste=nao')),
    ('avesso', re.compile(r'^AVESSO levado origem=(\S+) .*teste=nao')),
]

# O tema musical, em todas as formas em que ele toca.
TEMA = [
    ('caixa (o jogador deu corda)', re.compile(r'^CAIXA tocou')),
    ('cantarolar', re.compile(r'^CAIXA ele cantarolou')),
    ('assobio (evento)', re.compile(r'^CANTIGA pos=')),
    ('assobio (aviso)', re.compile(r'^PRENUNCIO .*assobio=sim')),
    ('cantiga falhada (Conta)', re.compile(r'^CONTA aviso=2/3 como=CANTIGA_FALHADA')),
    ('caixa sozinha (Conta)', re.compile(r'A_CAIXA_TOCA_SOZINHA')),
]

ITENS = [
    ('Vela', re.compile(r'^CONTA (?:\+\d+|carencia) item=VELA')),
    ('Olho', re.compile(r'^OLHO usado')),
    ('Sino', re.compile(r'^CONTA (?:\+\d+|carencia) item=SINO')),
    ('Fio', re.compile(r'^CONTA (?:\+\d+|carencia) item=FIO')),
    ('Isca', re.compile(r'^ISCA armada')),
    ('Caixa de Música', re.compile(r'^CAIXA tocou')),
    ('Ossos', re.compile(r'^OSSOS desfecho=')),
    ('Linha de Cinza (blocos)', re.compile(r'^CONTA (?:\+\d+|carencia) item=LINHA')),
    ('Lampião (cargas)', re.compile(r'^CONTA (?:\+\d+|carencia) item=LAMPIAO')),
    ('Oferenda posta', re.compile(r'^OFERENDA posta')),
]

PROBLEMAS = [
    ('seleção falhou', re.compile(r'^SELECAO falhou evento=(\S+)')),
    ('sem lugar para nada', re.compile(r'^SELECAO sem lugar')),
    ('vulto do Véu não coube', re.compile(r'^VEU vulto=nao_coube')),
    ('vulto do outro lado não coube', re.compile(r'^AVESSO vulto=nao_coube')),
    ('boneco sem lugar', re.compile(r'^BONECO SEM_LUGAR')),
    ('carta adiada', re.compile(r'^BARALHO carta=(\S+) adiada')),
    ('carta que não coube', re.compile(r'^BARALHO carta=(\S+) efeito=nao_coube')),
    ('captura sem destino', re.compile(r'^CAPTURA deslocou .* para=- ')),
]


def ler(caminho, jogador):
    linhas = []
    jogadores = collections.Counter()
    with io.open(caminho, encoding='utf-8', errors='replace') as arquivo:
        for bruta in arquivo:
            m = LINHA.match(bruta.rstrip('\r\n'))
            if not m:
                continue
            jogadores[m.group(2)] += 1
            linhas.append((int(m.group(1)), m.group(2), m.group(3)))
    if not jogadores:
        return [], None, jogadores
    if jogador is None:
        jogador = jogadores.most_common(1)[0][0]
    return [(t, texto) for t, quem, texto in linhas if quem == jogador], jogador, jogadores


def trechos_de_jogo(linhas):
    """Divide a sessão em trechos contínuos (sem buracos de jogador fora do jogo)."""
    trechos = []
    comeco = anterior = None
    for t, _ in linhas:
        if comeco is None:
            comeco = anterior = t
        elif t - anterior > FORA_DO_JOGO or t < anterior:
            trechos.append((comeco, anterior))
            comeco = t
        anterior = t
    if comeco is not None:
        trechos.append((comeco, anterior))
    return trechos


def jogado_ate(trechos, t):
    """Segundos de jogo, sem os buracos de jogador fora do mundo, do começo do log até o instante t."""
    total = 0
    for a, b in trechos:
        if t >= b:
            total += b - a
        elif t > a:
            total += t - a
    return total


def hms(segundos):
    segundos = int(segundos)
    return '%dh%02dm' % (segundos // 3600, segundos % 3600 // 60) if segundos >= 3600 else '%dm%02ds' % (segundos // 60, segundos % 60)


def por_hora(n, segundos):
    return n * 3600.0 / segundos if segundos > 0 else 0.0


def percentil(valores, p):
    if not valores:
        return 0
    ordenados = sorted(valores)
    return ordenados[min(len(ordenados) - 1, int(round(p * (len(ordenados) - 1))))]


def tabela(cabecalho, linhas):
    saida = ['| ' + ' | '.join(cabecalho) + ' |', '|' + '|'.join(['---'] * len(cabecalho)) + '|']
    for linha in linhas:
        saida.append('| ' + ' | '.join(str(c) for c in linha) + ' |')
    return saida


def analisar(linhas, jogador, caminho):
    out = []
    trechos = trechos_de_jogo(linhas)
    jogado = sum(b - a for a, b in trechos) or 1
    out.append('# Relatório da sessão (SPOILERS)')
    out.append('')
    out.append('- Arquivo: `%s`' % caminho)
    out.append('- Jogador: **%s**. Linhas dele: %d.' % (jogador, len(linhas)))
    out.append('- Tempo de jogo coberto pelo log: **%s**, em %d trecho(s) contínuo(s).' % (hms(jogado), len(trechos)))
    out.append('')

    # ----- fases e estados -----
    fase = 0
    fase_em = {}
    estado = None
    tempo_estado = collections.Counter()
    tempo_fase = collections.Counter()
    fase_de = {}  # t -> fase vigente (para classificar os intervalos)
    ultimo = None
    situacao = re.compile(r'^estado=(\S+) .* fase=(\d)')
    for t, texto in linhas:
        m = situacao.match(texto)
        if m:
            if ultimo is not None and 0 < t - ultimo <= FORA_DO_JOGO:
                tempo_estado[m.group(1)] += t - ultimo
                tempo_fase[int(m.group(2))] += t - ultimo
            ultimo = t
            estado = m.group(1)
            fase = int(m.group(2))
        m = re.match(r'^FASE -> (\d)', texto)
        if m:
            fase = int(m.group(1))
            fase_em.setdefault(fase, t)
        m = re.match(r'^COMANDO fase \d -> (\d)', texto)
        if m:
            fase = int(m.group(1))
            fase_em.setdefault(fase, t)
        fase_de[t] = fase
    out.append('## Fases e estados')
    out.append('')
    if fase_em:
        out.append('Chegada a cada fase: ' + ', '.join('fase %d aos %s' % (f, hms(t - linhas[0][0])) for f, t in sorted(fase_em.items())) + '.')
        out.append('')
    if tempo_fase:
        out += tabela(['Fase', 'Tempo', '% do jogo'], [(f, hms(s), '%.0f%%' % (100.0 * s / jogado)) for f, s in sorted(tempo_fase.items())])
        out.append('')
    if tempo_estado:
        out += tabela(['Estado do Diretor', 'Tempo', '% do jogo'],
                      [(e, hms(s), '%.0f%%' % (100.0 * s / jogado)) for e, s in tempo_estado.most_common()])
        out.append('')

    # ----- ritmo -----
    saidas = []  # (t, fonte, detalhe)
    intensidade = {}
    for t, texto in linhas:
        m = re.match(r'^SELECAO .* escolhido=(\S+) intensidade=(\d+)', texto)
        if m:
            intensidade[(t, m.group(1))] = int(m.group(2))
        if 'forçado por comando' in texto:
            continue
        for fonte, expr in PERCEPTIVEIS:
            m = expr.match(texto)
            if m:
                saidas.append((t, fonte, m.group(1) if m.groups() else ''))
                break
    out.append('## Ritmo: o que o jogador pôde perceber')
    out.append('')
    out.append('Conta o que o mod empurra para o jogador (eventos, presságios, cenas, avisos). Não conta o que é contínuo '
               '(cor, fundo sonoro) nem a resposta direta a um item que ele usou.')
    out.append('')
    out.append('**%d saídas em %s: %.1f por hora.**' % (len(saidas), hms(jogado), por_hora(len(saidas), jogado)))
    out.append('')
    fontes = collections.Counter(f for _, f, _ in saidas)
    out += tabela(['Fonte', 'Vezes', 'Por hora'], [(f, n, '%.1f' % por_hora(n, jogado)) for f, n in fontes.most_common()])
    out.append('')
    eventos = collections.Counter(d for _, f, d in saidas if f == 'evento')
    if eventos:
        out.append('Eventos do Diretor: ' + ', '.join('%s %d' % (e, n) for e, n in eventos.most_common()) + '.')
        out.append('')

    intervalos = collections.defaultdict(list)
    todos = []
    for (t0, _, _), (t1, _, _) in zip(saidas, saidas[1:]):
        d = t1 - t0
        if 0 <= d <= 3600 and not any(a <= t0 and t1 <= b for a, b in []):
            # descarta o intervalo que atravessa um buraco de jogador fora do jogo
            if any(a <= t0 and t1 <= b for a, b in trechos):
                intervalos[fase_de.get(t1, 0)].append(d)
                todos.append(d)
    if todos:
        out.append('Intervalo entre uma saída e a seguinte:')
        out.append('')
        linhas_tab = []
        for f in sorted(intervalos):
            v = intervalos[f]
            linhas_tab.append(('fase %d' % f, len(v), '%ds' % statistics.median(v), '%ds' % percentil(v, 0.1), '%ds' % percentil(v, 0.9), '%ds' % min(v)))
        linhas_tab.append(('**tudo**', len(todos), '%ds' % statistics.median(todos), '%ds' % percentil(todos, 0.1), '%ds' % percentil(todos, 0.9), '%ds' % min(todos)))
        out += tabela(['Quando', 'Intervalos', 'Mediana', '10% mais curtos', '10% mais longos', 'Menor'], linhas_tab)
        out.append('')
        out.append('Referência da revisão externa (ponto de partida, não regra): mediana de 180 s ou mais na fase 2, e de 90 s ou mais na fase 4.')
        out.append('')
        silencios = sorted(((t1 - t0, t0) for (t0, _, _), (t1, _, _) in zip(saidas, saidas[1:])
                            if any(a <= t0 and t1 <= b for a, b in trechos)), reverse=True)[:5]
        out.append('Maiores silêncios: ' + ', '.join('%s (aos %s)' % (hms(d), hms(t - linhas[0][0])) for d, t in silencios) + '.')
        out.append('')
    gastos = [float(m.group(1)) for m in (re.match(r'^ATENCAO fonte=\S+ custo=\S+ saldo=(-?[\d.]+)', texto) for _, texto in linhas) if m]
    if gastos:
        out.append('Orçamento de atenção: %d gastos; o saldo foi de %.0f a %.0f (a capacidade é 60).' % (len(gastos), min(gastos), max(gastos)))
        out.append('')
    fortes = [(t, d) for t, f, d in saidas if f == 'evento' and intensidade.get((t, d), 0) >= 22]
    colados = [(t0, d0, t1, d1) for (t0, d0), (t1, d1) in zip(fortes, fortes[1:]) if t1 - t0 < 300]
    out.append('Eventos fortes (intensidade 22 ou mais): %d. Pares a menos de 5 minutos um do outro: **%d**%s' % (
        len(fortes), len(colados), '.' if not colados else ': ' + '; '.join('%s e %s com %ds' % (a, b, t1 - t0) for t0, a, t1, b in colados[:8]) + '.'))
    out.append('')

    # ----- criatura -----
    criadas = []
    sumicos = []
    percebeu = 0
    encarou = 0
    for t, texto in linhas:
        m = re.match(r'^HOSPEDE id=(\S+) criado origem=(\S+) evento=(\S+) modo=(\S+) .*?dist=([\d.]+)', texto)
        if m:
            criadas.append((t, m.group(2), m.group(3), m.group(4), float(m.group(5))))
        m = re.match(r'^HOSPEDE id=(\S+) sumiu motivo=(\S+) viveu=(\d+)s dist=([\d.]+) vezesNaTela=(\d+)', texto)
        if m:
            sumicos.append((m.group(2), int(m.group(3)), float(m.group(4)), int(m.group(5))))
        if re.match(r'^HOSPEDE id=\S+ PERCEBEU', texto):
            percebeu += 1
        if re.match(r'^HOSPEDE id=\S+ ENCAROU', texto):
            encarou += 1
    out.append('## A criatura')
    out.append('')
    out.append('**%d manifestações: %.1f por hora.** Entrou na tela %d vezes; foi encarada %d vezes.' % (
        len(criadas), por_hora(len(criadas), jogado), percebeu, encarou))
    out.append('')
    if criadas:
        modos = collections.Counter(c[3] for c in criadas)
        out += tabela(['Modo', 'Vezes', 'Distância mediana ao nascer'],
                      [(modo, n, '%.0f blocos' % statistics.median([c[4] for c in criadas if c[3] == modo])) for modo, n in modos.most_common()])
        out.append('')
    if sumicos:
        vistos = [s for s in sumicos if s[3] > 0]
        out.append('Tempo de vida somado: %s (%.0f s por hora). Manifestações que chegaram a entrar na tela: %d de %d.' % (
            hms(sum(s[1] for s in sumicos)), por_hora(sum(s[1] for s in sumicos), jogado), len(vistos), len(sumicos)))
        out.append('')
        motivos = collections.Counter(s[0] for s in sumicos)
        out.append('Como sumiu: ' + ', '.join('%s %d' % (mo, n) for mo, n in motivos.most_common()) + '.')
        out.append('')
    exposicao = [re.match(r'^EXPOSICAO .*naTela=(\d+)t perto=(\d+)t', texto) for _, texto in linhas]
    exposicao = [m for m in exposicao if m]
    if exposicao:
        na_tela = sum(int(m.group(1)) for m in exposicao) / 20.0
        perto = sum(int(m.group(2)) for m in exposicao) / 20.0
        out.append('**Exposição medida: %.0f s na tela (%.0f s por hora), dos quais %.0f s a menos de 25 blocos e com luz 8 ou mais (%.0f s por hora).**' % (
            na_tela, por_hora(na_tela, jogado), perto, por_hora(perto, jogado)))
        out.append('')
        out.append('Referência da revisão externa (ponto de partida): 20 a 30 s por hora de exposição perto e iluminada, na fase 3.')
        out.append('')
    else:
        out.append('O log não tem linhas `EXPOSICAO` (segundos de criatura na tela): esta versão do mod ainda não mede isso.')
        out.append('')

    # ----- tema -----
    out.append('## O tema musical')
    out.append('')
    tema = [(nome, sum(1 for _, texto in linhas if expr.search(texto))) for nome, expr in TEMA]
    total_tema = sum(n for _, n in tema)
    out.append('**Tocou %d vezes: %.1f por hora.**' % (total_tema, por_hora(total_tema, jogado)))
    out.append('')
    out += tabela(['Forma', 'Vezes'], [(nome, n) for nome, n in tema if n])
    out.append('')

    # ----- reações -----
    confs = [float(m.group(1)) for m in (re.match(r'^REACAO .* c=([\d.]+)', texto) for _, texto in linhas) if m]
    out.append('## Como o jogador reagiu')
    out.append('')
    if confs:
        zero = sum(1 for c in confs if c == 0)
        baixa = sum(1 for c in confs if 0 < c <= 0.35)
        alta = sum(1 for c in confs if c > 0.35)
        out += tabela(['Confiança da leitura', 'Vezes', '%'], [
            ('nenhuma reação (c = 0)', zero, '%.0f%%' % (100.0 * zero / len(confs))),
            ('fraca (até 0,35)', baixa, '%.0f%%' % (100.0 * baixa / len(confs))),
            ('clara (acima de 0,35)', alta, '%.0f%%' % (100.0 * alta / len(confs))),
        ])
    else:
        out.append('Nenhuma linha `REACAO`.')
    out.append('')

    # ----- itens e Conta -----
    out.append('## Itens e a Conta')
    out.append('')
    usos = [(nome, sum(1 for _, texto in linhas if expr.match(texto))) for nome, expr in ITENS]
    out += tabela(['Item', 'Usos', 'Por hora'], [(nome, n, '%.1f' % por_hora(n, jogado)) for nome, n in usos])
    out.append('')
    sem_uso = [nome for nome, n in usos if n == 0]
    if sem_uso:
        out.append('Sem nenhum uso nesta sessão: ' + ', '.join(sem_uso) + '.')
        out.append('')
    picos = [int(m.group(1)) for m in (re.match(r'^CONTA \+\d+ item=\S+ total=(\d+)', texto) for _, texto in linhas) if m]
    avisos = sum(1 for _, texto in linhas if texto.startswith('CONTA aviso='))
    cobrancas = [m.group(1) + ' (' + m.group(2) + ')' for m in (re.match(r'^CONTA cobranca item=(\S+) como=(\S+)', texto) for _, texto in linhas) if m]
    # Da alpha14 em diante: a cobrança fica marcada no estouro e acontece no próprio item, no uso seguinte.
    cobrancas += [m.group(1) + ' (no uso seguinte)' for m in (re.match(r'^CONTA cobrada no uso item=(\S+)', texto) for _, texto in linhas) if m]
    estouros = sum(1 for _, texto in linhas if texto.startswith('CONTA estourou'))
    carencia = sum(1 for _, texto in linhas if texto.startswith('CONTA carencia'))
    marcados = sum(1 for _, texto in linhas if re.match(r'^CONTA \+\d+ item=', texto))
    out.append('Conta: maior valor %d; avisos dados %d; estouros %d; cobranças %d%s.' % (
        max(picos) if picos else 0, avisos, estouros, len(cobrancas), ': ' + ', '.join(cobrancas) if cobrancas else ''))
    out.append('')
    out.append('Usos na carência (não somam e não fazem o som do marcador): %d. Usos que somaram (com o marcador): %d.' % (carencia, marcados))
    out.append('')

    # ----- caçadas, capturas, o outro lado -----
    out.append('## Caçadas, capturas e o outro lado')
    out.append('')
    fins = [(m.group(1), int(m.group(2))) for m in (re.match(r'^CACA id=\S+ FIM motivo=(\S+) duracao=(\d+)s', texto) for _, texto in linhas) if m]
    capturas = [(m.group(1), int(m.group(2))) for m in (re.match(r'^CACA id=\S+ CAPTURA como=(\S+) duracao=(\d+)s', texto) for _, texto in linhas) if m]
    devidas = collections.Counter(m.group(1) for m in (re.match(r'^CACA devida motivo=(\S+)', texto) for _, texto in linhas) if m)
    out.append('Caçadas que acabaram sem pegar: %d%s.' % (len(fins), ' (' + ', '.join('%s %ds' % f for f in fins) + ')' if fins else ''))
    out.append('')
    out.append('Capturas: %d%s.' % (len(capturas), ' (' + ', '.join('%s aos %ds' % c for c in capturas) + ')' if capturas else ''))
    out.append('')
    if devidas:
        out.append('Caçadas que ficaram devendo: ' + ', '.join('%s %d' % d for d in devidas.most_common()) + '.')
        out.append('')
    idas = collections.Counter(m.group(1) for m in (re.match(r'^AVESSO levado origem=(\S+)', texto) for _, texto in linhas) if m)
    voltas = [(m.group(1), int(m.group(2))) for m in (re.match(r'^AVESSO voltou motivo=(\S+) ficou=(\d+)s', texto) for _, texto in linhas) if m]
    out.append('Visitas ao outro lado: %d%s.' % (sum(idas.values()), ' (' + ', '.join('%s %d' % i for i in idas.most_common()) + ')' if idas else ''))
    if voltas:
        out.append('')
        out.append('Como voltou: ' + ', '.join('%s depois de %ds' % v for v in voltas) + '.')
    out.append('')
    veus = sum(1 for _, texto in linhas if texto.startswith('VEU abriu'))
    out.append('Véu: abriu %d vez(es).' % veus)
    out.append('')

    # ----- lugares e baralho -----
    out.append('## Lugares e baralho')
    out.append('')
    achados = [texto for _, texto in linhas if re.match(r'^(SOLEIRA|BONECO|CASA_DO_VIGIA|BARALHO|ESTRUTURA) ', texto)]
    if achados:
        contagem = collections.Counter(re.match(r'^(\S+ \S+)', a).group(1) for a in achados)
        out += tabela(['Linha', 'Vezes'], contagem.most_common(30))
    else:
        out.append('Nada.')
    out.append('')

    # ----- o que não coube -----
    out.append('## O que não coube')
    out.append('')
    achou = False
    for nome, expr in PROBLEMAS:
        casos = [m for m in (expr.match(texto) for _, texto in linhas) if m]
        if casos:
            achou = True
            detalhe = collections.Counter(m.group(1) for m in casos if m.groups())
            out.append('- %s: %d%s' % (nome, len(casos), ' (' + ', '.join('%s %d' % d for d in detalhe.most_common(8)) + ')' if detalhe else ''))
    if not achou:
        out.append('Nada.')
    out.append('')

    # ----- a primeira hora: os critérios da análise de 09/10 (seção 6.7, corrigida pela seção 10) -----
    # O que se conta mudou: aparições que estiveram na MIRA do jogador (não as criadas), sinais seguidos de
    # algo no MESMO LUGAR (não "sem criatura"), e o peso da Conta no que ele percebeu. Os números são ponto de
    # partida e não foram validados; "de 6 a 12 aparições por hora" está suspenso e por isso não vira alerta.
    inicio = linhas[0][0]
    mundo_novo = bool(re.search(r' fase=0 ', next((texto for _, texto in linhas if texto.startswith('estado=')), '')))
    origem_de = {}
    forma_de = {}
    do_mod = 0          # manifestações que não vieram de comando
    na_mira = []        # (t, forma) das que o jogador encarou
    # A aparição do primeiro contato tem a sua definição de "visto" (olhado direto por uns instantes, com luz),
    # e quem a aplica é o mod: aqui ela vale pela linha CONTATO visto=sim, não pelo primeiro tick de ENCAROU.
    # Assim o relatório e o jogo nunca dizem coisas diferentes sobre o mesmo contato.
    do_contato = set(m.group(1) for m in (re.match(r'^CONTATO tentativa=.* manifestacao=(\S+)', texto) for _, texto in linhas) if m)
    for t, texto in linhas:
        m = re.match(r'^HOSPEDE id=(\S+) criado origem=(\S+) evento=(\S+) modo=(\S+)', texto)
        if m:
            origem_de[m.group(1)] = m.group(2)
            forma_de[m.group(1)] = m.group(3) + '/' + m.group(4)
            if m.group(2) != 'COMANDO':
                do_mod += 1
            continue
        m = re.match(r'^HOSPEDE id=(\S+) ENCAROU', texto)
        if m and origem_de.get(m.group(1), 'COMANDO') != 'COMANDO' and m.group(1) not in do_contato:
            na_mira.append((t, forma_de.get(m.group(1), '?')))
            continue
        m = re.match(r'^CONTATO visto=sim .*manifestacao=(\S+)', texto)
        if m:
            na_mira.append((t, forma_de.get(m.group(1), '?') + ' (contato)'))
    tentativas = sum(1 for _, texto in linhas if texto.startswith('CONTATO tentativa='))
    contato_visto = next((t for t, texto in linhas if texto.startswith('CONTATO visto=sim')), None)

    posicao = re.compile(r'pos(?:Som)?=\((-?\d+),(-?\d+),(-?\d+)\)')
    sinais = []         # (t, x, z) de cada som que aponta para um lugar
    coisas = []         # (t, x, z) do que se pode achar num lugar: a criatura, um vestígio, uma cinza
    for t, texto in linhas:
        if 'forçado por comando' in texto:
            continue
        m = posicao.search(texto)
        if not m:
            continue
        x, z = int(m.group(1)), int(m.group(3))
        if re.match(r'^(SINAL|SINAL_DISTANTE|ECO|RUIDO_RETORNO|CANTIGA|SEGUIDOR) ', texto):
            sinais.append((t, x, z))
        elif re.match(r'^(HOSPEDE id=\S+ criado|VESTIGIO|PEGADAS)', texto):
            coisas.append((t, x, z))
    seguidos = sum(1 for t, x, z in sinais
                   if any(0 <= t2 - t <= 120 and (x2 - x) ** 2 + (z2 - z) ** 2 <= 12 * 12 for t2, x2, z2 in coisas))

    # Uma tentativa de contato que ninguém viu não é saída forte: só a que foi vista.
    fortes_t = [t for t, f, d in saidas if (f == 'evento' and intensidade.get((t, d), 0) >= 22) or f in ('cena', 'cacada', 'captura', 'veu')]
    if contato_visto is not None:
        fortes_t.append(contato_visto)
    # Os eventos da sequência de ameaça não passam pelo sorteio, e por isso não têm a linha SELECAO com a
    # intensidade: sem isto, uma espreita seguida de "atrás de você" não contava como saída forte.
    fortes_t += [t for t, texto in linhas if texto.startswith('EVENTO ') and '(estado=AMEACANDO' in texto
                 and 'forçado por comando' not in texto and t not in fortes_t]
    fase1_em = fase_em.get(1, inicio if not mundo_novo else None)
    horas_sem_forte = []
    if fase1_em is not None:
        # Anda pelo tempo de JOGO, não pelo relógio do log: o tempo fora do mundo normal (Nether, End, o
        # outro lado, espectador) não deixa linha nenhuma e não pode contar como hora parada.
        fortes_j = [jogado_ate(trechos, t) for t in fortes_t]
        fase1_j = jogado_ate(trechos, fase1_em)
        h = fase1_j
        while h + 3600 <= jogado:
            if not any(h <= t < h + 3600 for t in fortes_j):
                horas_sem_forte.append(h)
            h += 3600
        # Sessão de menos de uma hora cheia: quarenta minutos sem nada forte já é o caso que interessa.
        if not horas_sem_forte and jogado - fase1_j >= 40 * 60 and not any(t >= fase1_j for t in fortes_j):
            horas_sem_forte.append(fase1_j)
    parte_conta = fontes.get('conta', 0) / float(len(saidas)) if saidas else 0.0

    out.append('## A primeira hora: os critérios')
    out.append('')
    out.append('Trocam o que se conta: o que esteve na mira do jogador, e não o que foi criado; sinais seguidos de algo '
               'no mesmo lugar; o peso da Conta. Os números são ponto de partida, não regra.')
    out.append('')
    criterios = []
    if tentativas:
        criterios.append(('Primeiro contato', '%d tentativa(s); visto %s' % (
            tentativas, 'aos ' + hms(contato_visto - inicio) if contato_visto is not None else '**ainda não**')))
    criterios.append(('Primeira aparição na mira dele (fora as de comando)',
                      'aos %s (%s)' % (hms(na_mira[0][0] - inicio), na_mira[0][1]) if na_mira else '**nenhuma**'))
    criterios.append(('Aparições na mira / criadas pelo mod', '%d de %d' % (len(na_mira), do_mod)))
    criterios.append(('Formas diferentes entre as vistas', ', '.join(sorted(set(f for _, f in na_mira))) or '—'))
    criterios.append(('Sinais com lugar seguidos de algo ali em até 2 min', '%d de %d' % (seguidos, len(sinais))))
    criterios.append(('Saídas fortes (evento de 22 ou mais ou da sequência de ameaça, cena, contato visto, caçada, captura, Véu)', str(len(fortes_t))))
    criterios.append(('Horas (ou 40 min, numa sessão curta), da fase 1 em diante, sem nenhuma saída forte',
                      ', '.join('a que começa aos ' + hms(h) for h in horas_sem_forte) or 'nenhuma'))
    criterios.append(('Parte da Conta no que ele percebeu', '%.0f%% (%d de %d)' % (100 * parte_conta, fontes.get('conta', 0), len(saidas))))
    out += tabela(['Critério', 'Nesta sessão'], criterios)
    out.append('')
    out.append('O que o log não mede: se ele consegue dizer o que cada item faz. Isso se pergunta a ele depois da sessão.')
    out.append('')

    # ----- alertas: o que merece ser olhado primeiro (vão para o começo do relatório) -----
    alertas = []
    # O prazo final do contato só COMEÇA a tentar aos 25 minutos (PrimeiroContato.JANELA_FECHA); ele ainda
    # precisa achar lugar e ser olhado. Com o limite nos 25 em ponto, o alerta disparava justamente quando o
    # prazo funcionava. Cinco minutos de folga, contados em tempo de jogo.
    limite = 30 * 60
    if any(b < a for (_, a), (b, _) in zip(trechos, trechos[1:])):
        alertas.append('**O relógio do log volta para trás: há mais de uma sessão neste arquivo.** Separe as linhas da última '
                       'num arquivo à parte antes de ler os números abaixo.')
    visto_cedo = any(jogado_ate(trechos, t) <= limite for t, _ in na_mira)
    if mundo_novo and jogado >= limite and not visto_cedo:
        alertas.append('**Mundo novo e nenhuma aparição na mira dele até os 30 minutos de jogo** (o prazo do primeiro contato é aos 25, '
                       'com cinco de folga). Houve %d tentativa(s).' % tentativas)
    if horas_sem_forte:
        alertas.append('**%d trecho(s) de uma hora (ou de 40 minutos, numa sessão curta), da fase 1 em diante, sem nenhuma saída forte.** É o "parado" que ele sentiu em 09/10.' % len(horas_sem_forte))
    if len(saidas) >= 9 and parte_conta > 1 / 3.0:
        alertas.append('**A Conta foi %.0f%% do que ele percebeu** (referência: menos de um terço).' % (100 * parte_conta))
    tempo_fase2 = sum(seg for f, seg in tempo_fase.items() if f >= 2)
    tempo_ativo = sum(seg for f, seg in tempo_fase.items() if f >= 1)
    eventos_total = sum(eventos.values())
    do_ambiente = fontes.get('pressagio', 0) + fontes.get('atmosfera', 0)
    if tempo_fase2 >= 20 * 60 and not criadas:
        alertas.append('**Nenhuma manifestação da criatura em %s de fase 2 ou mais.** O mod ficou sem a sua parte principal: '
                       'veja se algum limite está travando o Diretor.' % hms(tempo_fase2))
    if tempo_ativo >= 30 * 60 and por_hora(eventos_total, tempo_ativo) < 6:
        alertas.append('**Só %d eventos do Diretor em %s de fase 1 ou mais (%.1f por hora).** Nas sessões sintéticas da alpha13 foram de 11 a 32 por hora.'
                       % (eventos_total, hms(tempo_ativo), por_hora(eventos_total, tempo_ativo)))
    if len(saidas) >= 10 and do_ambiente > 2 * eventos_total:
        alertas.append('**O ambiente falou mais que o Diretor:** %d presságios e perturbações contra %d eventos.' % (do_ambiente, eventos_total))
    for nome, n in usos:
        if n >= 8 and por_hora(n, jogado) >= 8:
            alertas.append('O jogador usou **%s** %d vezes (%.0f por hora): está usando como botão.' % (nome, n, por_hora(n, jogado)))
    if len(cobrancas) >= 3:
        alertas.append('A Conta cobrou %d vezes: ele se apoia muito nos itens, ou o limite está curto.' % len(cobrancas))
    if 3 in fase_em and fase_em[3] - linhas[0][0] < 45 * 60:
        alertas.append('Chegou à fase 3 com %s de jogo: a progressão das fases está rápida.' % hms(fase_em[3] - linhas[0][0]))
    bloco = ['## Alertas', '']
    bloco += ['- ' + alerta for alerta in alertas] if alertas else ['Nenhum.']
    bloco.append('')
    corte = out.index('## Fases e estados')
    out[corte:corte] = bloco
    return '\n'.join(out) + '\n'


def main():
    parser = argparse.ArgumentParser(description='Relatório de uma sessão a partir do sussurros-debug.log (tem spoilers).')
    parser.add_argument('log')
    parser.add_argument('--jogador', help='nome do jogador (padrão: o que tem mais linhas)')
    parser.add_argument('--saida', help='arquivo .md para gravar (padrão: mostra na tela)')
    args = parser.parse_args()
    linhas, jogador, jogadores = ler(args.log, args.jogador)
    if not linhas:
        print('Nenhuma linha reconhecida em %s. Jogadores vistos: %s' % (args.log, dict(jogadores)), file=sys.stderr)
        return 1
    relatorio = analisar(linhas, jogador, args.log)
    if args.saida:
        with io.open(args.saida, 'w', encoding='utf-8', newline='\n') as arquivo:
            arquivo.write(relatorio)
        print('Relatório gravado em', args.saida)
    else:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stdout.write(relatorio)
    return 0


if __name__ == '__main__':
    sys.exit(main())
