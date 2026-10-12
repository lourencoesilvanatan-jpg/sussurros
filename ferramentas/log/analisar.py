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
  - o que não coube (seleção que falhou, falta de lugar), e por quê: quantos lugares cada procura tentou e
    em que exigência eles caíram;
  - o que acabou sem ninguém, separando o vazio por falha (com o plano B que saiu no lugar) do vazio por
    escolha, e o que a cota de corpo fez (0.9.0-alpha20);
  - de que família foi o que saiu em cada tipo de lugar (luz, som, marca, objeto, corpo, cabeça), e, debaixo
    da terra, quanto tempo ele ficou sem nada que se toque (0.9.0-alpha22).

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
    # 0.9.0-alpha20: a tentativa da própria cota (a que vem pelo Diretor já conta como evento) e o plano B.
    ('cota', re.compile(r'^COTA tentativa=(\d+) origem=REGRA')),
    ('plano B', re.compile(r'^PLANO_B tipo=(?!NENHUM)(\S+)')),
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
    ('aparição chamada pelo Olho não coube', re.compile(r'^OLHO aparicao=nao_coube')),
    ('vulto do outro lado não coube', re.compile(r'^AVESSO vulto=nao_coube')),
    ('boneco sem lugar', re.compile(r'^BONECO SEM_LUGAR')),
    ('carta adiada', re.compile(r'^BARALHO carta=(\S+) adiada')),
    ('carta que não coube', re.compile(r'^BARALHO carta=(\S+) efeito=nao_coube')),
    ('captura sem destino', re.compile(r'^CAPTURA deslocou .* para=- ')),
    ('cena da janela sem vidro por perto', re.compile(r'^CENA tipo=DO_OUTRO_LADO_DO_VIDRO pulada motivo=SEM_JANELA')),
]


# De que família é cada saída, para os logs de antes da alpha22 (que não têm a linha FAMILIA). Da alpha22 em
# diante quem diz é o jogo (Familias.java), e esta tabela não é usada: se as duas discordarem, vale a do jogo.
FAMILIA_ANTIGA = {
    'LUZ': ['LUZ_ERRADA', 'TOCHA', 'LUZ_DISTANTE', 'LUZ_NO_FIM', 'PASSOU_PELA_MINA', 'CHAMA_PALIDA'],
    'SOM': ['PASSOS', 'PASSO_UNICO', 'ECO', 'PASSAGEM', 'SINAL_DISTANTE', 'RUIDO_RETORNO', 'ECO_PASSOS', 'SINAL', 'SEGUIDOR',
            'CANTIGA', 'BATIDA', 'PRENUNCIO', 'PASSO_DISTANTE', 'ECO_TARDIO', 'RUIDO_NA_PAREDE', 'NAO_FOI_VOCE'],
    'MARCA': ['VESTIGIO', 'TRILHA_INTERROMPIDA', 'PEGADAS', 'CINZA_NO_RASTRO'],
    'OBJETO': ['OBJETO_FORA_LUGAR', 'PORTA', 'O_CAMINHO_MUDOU', 'MARCA_IMPOSSIVEL'],
    'CORPO': ['ANIMAIS', 'ANIMAIS_VAZIO', 'ANIMAIS_JOGADOR', 'ANIMAIS_MOVIMENTO', 'TODOS_OLHANDO', 'HA_ALGO_NO_CURRAL', 'HOSPEDE'],
    'CABECA': ['VIGIA', 'SUSSURRO', 'NEBLINA', 'ECO_CHAT', 'VEU', 'VISTO'],
}
FAMILIA_DE = {nome: familia for familia, nomes in FAMILIA_ANTIGA.items() for nome in nomes}
# O que se pode tocar: são as duas famílias que a caverna do dono não teve em 11/10/2026.
FISICAS = ('OBJETO', 'CORPO')
# O teto que a pesquisa pede para uma família só, e quantas saídas no subsolo bastam para o número valer.
TETO_DE_FAMILIA = 0.40
MINIMO_PARA_FAMILIA = 10


def familias_por_lugar(linhas):
    """Devolve (contagem por lugar e família, de onde veio cada uma, se o log já traz a linha FAMILIA)."""
    por_lugar = collections.defaultdict(collections.Counter)
    origem = collections.defaultdict(collections.Counter)
    do_jogo = any(texto.startswith('FAMILIA familia=') for _, texto in linhas)
    lugar = 'OUTRO'
    for _, texto in linhas:
        m = re.match(r'^CONTEXTO \S+ -> (\S+)', texto)
        if m:
            lugar = m.group(1)
            continue
        if do_jogo:
            m = re.match(r'^FAMILIA familia=(\S+) de=(\S+) lugar=(\S+)', texto)
            if m:
                por_lugar[m.group(3)][m.group(1)] += 1
                origem[(m.group(3), m.group(1))][m.group(2)] += 1
            continue
        if 'forçado por comando' in texto:
            continue
        nome = None
        for expr in (r'^EVENTO (\S+) \(estado=', r'^PRESSAGIO tipo=(\S+)', r'^ATMOSFERA cena=(\S+) teste=nao', r'^(CHAMA_PALIDA) pos='):
            m = re.match(expr, texto)
            if m:
                nome = m.group(1)
                break
        if nome is None and re.match(r'^HOSPEDE id=\S+ criado origem=(?!COMANDO)', texto):
            nome = 'HOSPEDE'
        # A aparição já conta como HOSPEDE: o evento que a pediu não conta de novo.
        if nome in ('PRESENCA', 'ATRAS', 'TUMULO', 'ESPREITA', 'VULTO', 'CACA', 'ESPERA'):
            continue
        familia = FAMILIA_DE.get(nome)
        if familia:
            por_lugar[lugar][familia] += 1
            origem[(lugar, familia)][nome] += 1
    return por_lugar, origem, do_jogo


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


# Eventos que põem o corpo da criatura no mundo: quando a seleção de um deles falha, faltou lugar para ela.
COM_CORPO = ('PRESENCA', 'ATRAS', 'VULTO', 'TUMULO', 'CACA', 'ESPERA', 'ESPREITA')

# Uma construção inteira que acabou sem ninguém: a sequência de ameaça, uma cena, o vulto do Véu, o Olho.
# É o vazio por FALHA. Da alpha20 em diante cada uma dessas linhas termina em "planoB=<o que saiu>".
SEM_NINGUEM = [
    ('sequência de ameaça', re.compile(r'^CENA id=\S+ FIM motivo=SEM_LUGAR')),
    ('cena', re.compile(r'^CENA id=\S+ etapa=\S+ SEM_LUGAR')),
    ('vulto do Véu', re.compile(r'^VEU vulto=nao_coube')),
    ('Olho', re.compile(r'^OLHO aparicao=nao_coube')),
]
PLANO_B = re.compile(r' planoB=(\S+)')

# O vazio por ESCOLHA: o mod prepara e, de propósito, não entrega. Faz parte do ritmo (esperar e não receber
# mantém o alerta) e tem de continuar existindo: o plano B e a cota acabam com o vazio por falha, não com este.
VAZIO_ESCOLHIDO = [
    ('falso aviso de caçada', re.compile(r'^PRENUNCIO real=nao')),
    ('sensação de ser olhado, sem ninguém', re.compile(r'^VIGIA falsa')),
    ('cena que não mostra ninguém antes da fase 3', re.compile(r'^CENA id=\S+ etapa=PRESENCA PULADA')),
]


def falhas_de_lugar(texto):
    """Lê uma linha "LUGAR falhou NORMAL[vezes=3 tentados=54 ...] DE_PERTO[...]": {procura: {motivo: n}}."""
    achado = collections.OrderedDict()
    for procura, dentro in re.findall(r'(\w+)\[([^\]]*)\]', texto):
        achado[procura] = collections.OrderedDict((nome, int(valor)) for nome, valor in re.findall(r'(\w+)=(\d+)', dentro))
    return achado


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
        # 0.9.0-alpha17: a exceção ao relance. Quantas vezes, visto, ele foi embora andando em vez de sumir.
        # 0.9.0-alpha19: quantas nasceram pela procura de lugar apertado, e quantas dessas abaixadas.
        de_perto = sum(1 for _, texto in linhas if re.match(r'^HOSPEDE id=\S+ criado .*motivoPosicao=DE_PERTO', texto))
        abaixadas = sum(1 for _, texto in linhas if re.match(r'^HOSPEDE id=\S+ criado .*postura=ABAIXADO', texto))
        out.append('Nasceu de perto (lugar apertado, de 8 a 14 blocos): %d vez(es); abaixado: %d.' % (de_perto, abaixadas))
        out.append('')
        recuou = sum(1 for _, texto in linhas if re.match(r'^HOSPEDE id=\S+ saida=RECUA', texto))
        escondido = sum(1 for _, texto in linhas if re.match(r'^HOSPEDE id=\S+ saida=RECUA .*escondido=sim', texto))
        out.append('Foi embora andando em vez de sumir: %d vez(es); em %d delas tinha onde sair da vista.' % (recuou, escondido))
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
    # 0.9.0-alpha24: o vão de um bloco, o piscar pelo caminho, e por que a navegação desistiu.
    de_quatro = collections.Counter(m.group(1) for m in (re.match(r'^CACA id=\S+ corpo=DE_QUATRO motivo=(\S+)', texto) for _, texto in linhas) if m)
    vaos = [(int(m.group(1)), float(m.group(2))) for m in (re.match(r'^CACA id=\S+ vao boca=\S+ espera=(\d+)t dist=([\d.]+)', texto) for _, texto in linhas) if m]
    piscadas = sum(1 for _, texto in linhas if re.match(r'^CACA id=\S+ piscar n=', texto))
    pelo_caminho = sum(1 for _, texto in linhas if re.match(r'^CACA id=\S+ piscar pelo caminho', texto))
    desistencias = sum(1 for _, texto in linhas if texto.startswith('BUSCA ') and 'motivo=CAMINHO_FALHOU' in texto)
    sem_caminho = [m.groups() for m in (re.match(r'^BUSCA id=\S+ sem_caminho ele=(\S+) alvo=(\S+) dist=([\d.]+) corpo=(\S+) caminho=(\S+)(?: fim=\S+ falta=([\d.]+))?', texto)
                                        for _, texto in linhas) if m]
    if fins or capturas or de_quatro or desistencias:
        out.append('De quatro: %d vez(es)%s; avisou na boca de um vão %d vez(es)%s. Piscadas: %d, das quais %d pelo caminho (a linha reta estava tampada).' % (
            sum(de_quatro.values()), ' (' + ', '.join('%s %d' % c for c in de_quatro.most_common()) + ')' if de_quatro else '',
            len(vaos), ', a %.1f blocos do jogador em média' % (sum(d for _, d in vaos) / len(vaos)) if vaos else '', piscadas, pelo_caminho))
        out.append('')
        if desistencias:
            tipos = collections.Counter(c[4] for c in sem_caminho)
            faltas = [float(c[5]) for c in sem_caminho if c[5]]
            out.append('A navegação desistiu (CAMINHO_FALHOU) **%d** vez(es)%s%s.' % (
                desistencias, ': ' + ', '.join('%s %d' % t for t in tipos.most_common()) if tipos else ' (log de antes da alpha24: sem o motivo)',
                '; nos MESMO, faltavam %.1f blocos em média até o alvo' % (sum(faltas) / len(faltas)) if faltas else ''))
            out.append('')
            for ele, alvo, dist, corpo, tipo, falta in sem_caminho[:6]:
                out.append('- ele em %s, alvo em %s, a %s blocos, corpo %s: %s%s' % (ele, alvo, dist, corpo, tipo, ', faltando ' + falta if falta else ''))
            if sem_caminho:
                out.append('')
    idas = collections.Counter(m.group(1) for m in (re.match(r'^AVESSO levado origem=(\S+)', texto) for _, texto in linhas) if m)
    voltas = [(m.group(1), int(m.group(2))) for m in (re.match(r'^AVESSO voltou motivo=(\S+) ficou=(\d+)s', texto) for _, texto in linhas) if m]
    out.append('Visitas ao outro lado: %d%s.' % (sum(idas.values()), ' (' + ', '.join('%s %d' % i for i in idas.most_common()) + ')' if idas else ''))
    if voltas:
        out.append('')
        out.append('Como voltou: ' + ', '.join('%s depois de %ds' % v for v in voltas) + '.')
    out.append('')
    # 0.9.0-alpha21: a primeira ida é garantida na terceira noite em que ele deita (da fase 3 em diante). Enquanto
    # ele nunca foi, cada vez que deita deixa uma linha com a noite e a decisão.
    deitou = [(t, int(m.group(1)), m.group(2)) for t, m in ((t, re.match(r'^AVESSO deitou noite=(\d+)/\d+ decisao=(\S+)', texto)) for t, texto in linhas) if m]
    adiadas = sum(1 for _, texto in linhas if texto.startswith('AVESSO garantia adiada'))
    if deitou:
        out.append('Antes da primeira ida: deitou %d vez(es) e chegou à noite %d de 3 (%s)%s.' % (
            len(deitou), max(n for _, n, _ in deitou),
            ', '.join('%s %d' % d for d in collections.Counter(d for _, _, d in deitou).most_common()),
            '; a garantia foi adiada %d vez(es) por haver caçada' % adiadas if adiadas else ''))
    else:
        out.append('Nenhuma linha `AVESSO deitou`: ou ele não deitou nenhuma vez nesta sessão (da fase 3 em diante), ou já tinha ido antes, '
                   'ou o log é de antes da alpha21.')
    out.append('')
    veus = sum(1 for _, texto in linhas if texto.startswith('VEU abriu'))
    # 0.9.0-alpha19: o que cada Véu tinha dentro. "certo" é o vulto que vem por não haver porta nem vidro.
    vultos = collections.Counter(m.group(1) for m in (re.match(r'^VEU abriu .*vulto=(\w+)', texto) for _, texto in linhas) if m)
    vulto_veio = sum(1 for _, texto in linhas if texto.startswith('VEU vulto=sim'))
    vazios = sum(1 for _, texto in linhas if re.match(r'^VEU abriu .*porta=nao vidro=nao vulto=nao', texto))
    out.append('Véu: abriu %d vez(es)%s. O vulto veio em %d; sem porta, sem vidro e sem vulto (vazio): **%d**.' % (
        veus, ' (vulto: ' + ', '.join('%s %d' % v for v in vultos.most_common()) + ')' if vultos else '', vulto_veio, vazios))
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

    # ----- por que não achou lugar (0.9.0-alpha19): as linhas "LUGAR falhou" -----
    # Cada uma soma as procuras que falharam desde a anterior: quantas vezes rodou, quantos lugares tentou e em
    # que exigência eles caíram. Até a alpha18 o log só dizia SEM_LUGAR, e o motivo tinha de ser refeito à mão.
    soma_lugar = collections.OrderedDict()
    for _, texto in linhas:
        if texto.startswith('LUGAR falhou '):
            for procura, motivos in falhas_de_lugar(texto).items():
                destino = soma_lugar.setdefault(procura, collections.Counter())
                for nome, valor in motivos.items():
                    destino[nome] += valor
    out.append('## Por que não achou lugar')
    out.append('')
    if soma_lugar:
        out.append('Uma linha por procura. "vezes" é quantas vezes ela rodou sem achar nada; o resto é onde os lugares '
                   'tentados caíram (sem chão, na tela, sem linha de visão, a céu aberto, na zona da vela...).')
        out.append('')
        out += tabela(['Procura', 'Vezes', 'Onde caiu'],
                      [(procura, c.get('vezes', 0), ', '.join('%s %d' % (nome, n) for nome, n in c.most_common() if nome != 'vezes') or '—')
                       for procura, c in soma_lugar.items()])
    else:
        out.append('Nenhuma linha `LUGAR falhou` (nenhuma procura falhou, ou o log é de antes da alpha19).')
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
    # 0.9.0-alpha20: toda aparição mede o "visto de verdade" (na mira por uns instantes, com luz), e a linha
    # EXPOSICAO diz. Num log de antes disso vale o primeiro tick de ENCAROU, como era.
    mede_visto = any(re.match(r'^EXPOSICAO .* visto=', texto) for _, texto in linhas)
    for t, texto in linhas:
        m = re.match(r'^HOSPEDE id=(\S+) criado origem=(\S+) evento=(\S+) modo=(\S+)', texto)
        if m:
            origem_de[m.group(1)] = m.group(2)
            forma_de[m.group(1)] = m.group(3) + '/' + m.group(4)
            if m.group(2) != 'COMANDO':
                do_mod += 1
            continue
        m = re.match(r'^EXPOSICAO id=(\S+) .* visto=sim', texto) if mede_visto else re.match(r'^HOSPEDE id=(\S+) ENCAROU', texto)
        if m and origem_de.get(m.group(1), 'COMANDO') != 'COMANDO' and m.group(1) not in do_contato:
            na_mira.append((t, forma_de.get(m.group(1), '?')))
            continue
        m = re.match(r'^CONTATO visto=sim .*manifestacao=(\S+)', texto)
        if m:
            na_mira.append((t, forma_de.get(m.group(1), '?') + ' (contato)'))
    # 0.9.0-alpha19: "tentei" e "ele viu" são contas diferentes. Uma construção que acabou sem ninguém (a
    # sequência de ameaça que esperou um minuto por um lugar, uma cena, o vulto do Véu) é uma tentativa perdida.
    sem_ninguem = []    # (t, o quê, por quê, plano B ou None)
    ultimo_lugar = None
    for t, texto in linhas:
        if texto.startswith('LUGAR falhou '):
            ultimo_lugar = (t, texto[len('LUGAR falhou '):])
            continue
        for nome, expr in SEM_NINGUEM:
            if expr.match(texto):
                motivo = ultimo_lugar[1] if ultimo_lugar is not None and 0 <= t - ultimo_lugar[0] <= 1 else ''
                plano = PLANO_B.search(texto)
                sem_ninguem.append((t, nome, motivo, plano.group(1) if plano else None))
                break
    # Sem plano B: a linha não tem "planoB=" (log de antes da alpha20, ou defeito), ou tem e não saiu nada
    # ("-": o Véu fechou antes; "NENHUM": não havia lugar fora da zona da vela).
    sem_plano = [s for s in sem_ninguem if s[3] in (None, '-', 'NENHUM')]
    com_plano = collections.Counter(s[3] for s in sem_ninguem if s[3] not in (None, '-', 'NENHUM'))
    tem_plano_b = any(s[3] is not None for s in sem_ninguem) or any(texto.startswith('PLANO_B ') for _, texto in linhas)
    escolhidos = [(nome, sum(1 for _, texto in linhas if expr.match(texto))) for nome, expr in VAZIO_ESCOLHIDO]
    corpo_falhou = collections.Counter(m.group(1) for m in (re.match(r'^SELECAO falhou evento=(\S+)', texto) for _, texto in linhas)
                                       if m and m.group(1) in COM_CORPO)
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

    # ----- a cota de corpo e o plano B (0.9.0-alpha20) -----
    out.append('## Nada termina em nada: o plano B e a cota de corpo')
    out.append('')
    out.append('Vazio por falha (uma construção que não achou onde pôr o corpo): **%d**%s.' % (
        len(sem_ninguem), '' if not sem_ninguem else ' (%s)' % ', '.join(
            '%s %d' % q for q in collections.Counter(s[1] for s in sem_ninguem).most_common())))
    out.append('')
    if sem_ninguem:
        out.append('Com o sinal do plano B no lugar: %d%s. **Sem sinal nenhum: %d**%s.' % (
            sum(com_plano.values()), ' (%s)' % ', '.join('%s %d' % c for c in com_plano.most_common()) if com_plano else '',
            len(sem_plano), '' if not sem_plano else ' (%s)' % ', '.join(
                '%s aos %s%s' % (s[1], hms(s[0] - inicio), '' if s[3] is None else ', planoB=' + s[3]) for s in sem_plano[:6])))
        out.append('')
    lados = collections.Counter(m.group(1) for m in (re.match(r'^PLANO_B tipo=(?!NENHUM)\S+ origem=\S+ lado=(\S+)', texto) for _, texto in linhas) if m)
    if lados:
        a_vista = sum(1 for _, texto in linhas if re.match(r'^PLANO_B tipo=(?!NENHUM).* aVista=sim', texto))
        out.append('De que lado veio o sinal: ' + ', '.join('%s %d' % l for l in lados.most_common()) + '. À vista dele: %d de %d.' % (
            a_vista, sum(lados.values())))
        out.append('')
    out.append('Vazio por escolha (o mod prepara e não entrega, de propósito): **%d**%s.' % (
        sum(n for _, n in escolhidos), '' if not any(n for _, n in escolhidos) else ' (%s)' % ', '.join('%s %d' % e for e in escolhidos if e[1])))
    out.append('')
    cota_tent = collections.Counter(m.group(1) for m in (re.match(r'^COTA tentativa=\d+ origem=(\S+)', texto) for _, texto in linhas) if m)
    cota_vista = collections.Counter(m.group(1) for m in (re.match(r'^COTA visto=(\S+)', texto) for _, texto in linhas) if m)
    cota_paga = [int(m.group(1)) for m in (re.match(r'^COTA paga relogio=(\d+)s', texto) for _, texto in linhas) if m]
    cota_deveu = sum(1 for _, texto in linhas if texto.startswith('COTA devendo'))
    if cota_tent or cota_deveu or cota_paga:
        out.append('Cota de corpo: ficou devendo %d vez(es) (vinte minutos sem ele ser visto de verdade). Tentativas: %d%s; vistas %d, não vistas %d. '
                   'Paga %d vez(es)%s.' % (
                       cota_deveu, sum(cota_tent.values()), ' (%s)' % ', '.join('%s %d' % c for c in cota_tent.most_common()) if cota_tent else '',
                       cota_vista.get('sim', 0), cota_vista.get('nao', 0), len(cota_paga),
                       ', com o relógio em ' + ', '.join(hms(s) for s in cota_paga) if cota_paga else ''))
    else:
        out.append('Cota de corpo: não ficou devendo nenhuma vez (ele foi visto de verdade a cada vinte minutos, a sessão foi curta, '
                   'está antes da fase 3 ou o log é de antes da alpha20).')
    out.append('')
    # O maior tempo de jogo, da fase 3 em diante, sem uma aparição vista de verdade: é o que a cota existe para limitar.
    fase3_em = fase_em.get(3, inicio if fase_de.get(inicio, 0) >= 3 else None)
    maior_sem_ver = None
    if fase3_em is not None and mede_visto:
        marcos = [jogado_ate(trechos, fase3_em)] + sorted(jogado_ate(trechos, t) for t, _ in na_mira if t >= fase3_em) + [jogado]
        maior_sem_ver = max(b - a for a, b in zip(marcos, marcos[1:]))
        out.append('Maior tempo de jogo, da fase 3 em diante, sem uma aparição vista de verdade: **%s** (a cota tenta sozinha aos 30 minutos).' % hms(maior_sem_ver))
        out.append('')

    # ----- as famílias, por lugar (0.9.0-alpha22) -----
    out.append('## As famílias do que saiu, por lugar')
    out.append('')
    por_lugar, origem_familia, familia_do_jogo = familias_por_lugar(linhas)
    ordem_familias = ['LUZ', 'SOM', 'MARCA', 'OBJETO', 'CORPO', 'CABECA']
    if not por_lugar:
        out.append('Nada saiu (ou o log não tem as linhas de contexto).')
        out.append('')
    else:
        out.append('De que família foi cada coisa que o mod fez, pelo tipo de lugar em que ele estava. As chamas pálidas e a '
                   'rachadura entram aqui (ele as percebe), embora não contem como "saída" no ritmo. %s' % (
                       'Quem diz a família é o jogo (linha `FAMILIA`).' if familia_do_jogo
                       else '**Log de antes da alpha22: a família foi estimada pelo nome de cada linha.**'))
        out.append('')
        linhas_tab = []
        for lugar_nome, conta in sorted(por_lugar.items(), key=lambda kv: -sum(kv[1].values())):
            total = sum(conta.values())
            linhas_tab.append([lugar_nome, total] + ['%d (%.0f%%)' % (conta[f], 100.0 * conta[f] / total) if conta[f] else '-' for f in ordem_familias])
        out += tabela(['Lugar', 'Saídas'] + [f.lower() for f in ordem_familias], linhas_tab)
        out.append('')
    subsolo = por_lugar.get('SUBSOLO', collections.Counter())
    total_subsolo = sum(subsolo.values())
    familia_demais = None
    if total_subsolo:
        maior, vezes = subsolo.most_common(1)[0]
        fisicas = sum(subsolo[f] for f in FISICAS)
        out.append('Debaixo da terra: %d saídas; a família que mais saiu foi **%s, com %.0f%%** (%s). Coisas que se tocam (objeto ou corpo): **%d**.' % (
            total_subsolo, maior.lower(), 100.0 * vezes / total_subsolo,
            ', '.join('%s %d' % o for o in origem_familia[('SUBSOLO', maior)].most_common(4)), fisicas))
        out.append('')
        if total_subsolo >= MINIMO_PARA_FAMILIA and vezes > TETO_DE_FAMILIA * total_subsolo:
            familia_demais = (maior, vezes, total_subsolo)
    # O relógio do subsolo sem nada que se toque: cada linha FAMILIA no subsolo diz quanto ele marcava.
    relogios = [(int(m.group(2)), m.group(1) in FISICAS) for m in (
        re.match(r'^FAMILIA familia=(\S+) de=\S+ lugar=SUBSOLO .*semFisico=(\d+)s', texto) for _, texto in linhas) if m]
    maior_sem_fisico = max((r for r, _ in relogios), default=None)
    dividas_pagas = collections.Counter(m.group(1) for m in (re.match(r'^FAMILIA divida paga=(\S+)', texto) for _, texto in linhas) if m)
    dividas_sem_lugar = sum(1 for _, texto in linhas if texto.startswith('FAMILIA divida sem_lugar'))
    barrou = sum(1 for _, texto in linhas if texto.startswith('FAMILIA barrou='))
    cedeu = sum(1 for _, texto in linhas if texto.startswith('FAMILIA barrou=') and texto.endswith('cedeu=sim'))
    if familia_do_jogo:
        if maior_sem_fisico is not None:
            out.append('Maior tempo de subsolo sem nada que se toque: **%s** (a dívida abre aos 15 minutos). Dívidas pagas: %d%s; tentativas sem lugar: %d.' % (
                hms(maior_sem_fisico), sum(dividas_pagas.values()),
                ' (%s)' % ', '.join('%s %d' % d for d in dividas_pagas.most_common()) if dividas_pagas else '', dividas_sem_lugar))
            out.append('')
        out.append('A regra das duas em cinco tirou alguma família do sorteio do Diretor %d vez(es); em %d delas cedeu, porque só havia aquela.' % (barrou, cedeu))
        out.append('')
    da_caverna = [
        ('rachadura', sum(1 for _, texto in linhas if re.match(r'^RACHADURA pos=.* teste=nao', texto))),
        ('pedrinha', sum(1 for _, texto in linhas if re.match(r'^PEDRINHA de=.* teste=nao', texto))),
        ('morcegos', sum(1 for _, texto in linhas if re.match(r'^MORCEGOS qtd=.* teste=nao', texto))),
        ('tampa do baú', sum(1 for _, texto in linhas if re.match(r'^TAMPA pos=.* teste=nao', texto))),
        ('passagem fechada', sum(1 for _, texto in linhas if re.match(r'^PASSAGEM_FECHADA pos=.* teste=nao', texto))),
    ]
    if familia_do_jogo:
        # Cada bloco da parede falsa deixa a sua linha de fim: conta os lugares, não os blocos.
        foi_ver = len({(t, texto.split(' pos=')[-1][:12]) for t, texto in linhas
                       if texto.startswith('MIRAGEM fim motivo=PASSAGEM_FECHADA por=CHEGOU_PERTO')})
        out.append('As coisas da caverna: ' + ', '.join('%s %d' % c for c in da_caverna) + '. Ele quebrou o bloco rachado para ver: %d vez(es). '
                   'Blocos de passagem fechada que se desfizeram porque ele chegou perto: %d.' % (
                       sum(1 for _, texto in linhas if texto.startswith('RACHADURA fim por=QUEBROU_O_BLOCO')), foi_ver))
        out.append('')

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
    criterios.append(('Vezes em que quis mostrá-lo (criadas pelo mod + construções que acabaram sem ninguém)',
                      '%d (%d + %d)' % (do_mod + len(sem_ninguem), do_mod, len(sem_ninguem))))
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
    if sem_ninguem:
        quais = collections.Counter(s[1] for s in sem_ninguem)
        alertas.append('**%d construção(ões) acabaram sem ninguém (SEM_LUGAR): %s.** É o "cadê o bicho?". %s %s' % (
            len(sem_ninguem), ', '.join('%s %d' % q for q in quais.most_common()),
            'Com o sinal do plano B: %d.' % sum(com_plano.values()) if tem_plano_b else 'O log é de antes do plano B (alpha20).',
            'A última, aos %s: `%s`.' % (hms(sem_ninguem[-1][0] - inicio), sem_ninguem[-1][2]) if sem_ninguem[-1][2]
            else 'O log não tem a linha `LUGAR falhou` junto delas (versão anterior à alpha19).'))
    if tem_plano_b and sem_plano:
        alertas.append('**%d construção(ões) acabaram sem ninguém E sem o sinal do plano B** (%s). Da alpha20 em diante isto só deveria '
                       'acontecer com o Véu fechado antes da hora (`planoB=-`) ou dentro da zona da vela (`NENHUM`).' % (
                           len(sem_plano), ', '.join('%s: %s' % (s[1], 'sem planoB=' if s[3] is None else 'planoB=' + s[3]) for s in sem_plano[:6])))
    # A prova do pacote "a caverna com corpo": nenhuma família acima de 40% do que saiu no subsolo, e nada de
    # vinte minutos (os quinze da dívida e cinco de folga) debaixo da terra sem uma coisa que se toque.
    if familia_demais:
        alertas.append('**Debaixo da terra, %.0f%% do que saiu foi %s** (%d de %d; referência: nenhuma família acima de 40%%). '
                       'O que mais pesou: %s.%s' % (
                           100.0 * familia_demais[1] / familia_demais[2], familia_demais[0].lower(), familia_demais[1], familia_demais[2],
                           ', '.join('%s %d' % o for o in origem_familia[('SUBSOLO', familia_demais[0])].most_common(3)),
                           '' if familia_do_jogo else ' O log é de antes da regra das famílias (alpha22).'))
    if maior_sem_fisico is not None and maior_sem_fisico > 20 * 60:
        alertas.append('**%s de subsolo sem nada que se toque** (a dívida deveria ter sido paga aos 15 minutos; pagas %d, sem lugar %d). '
                       'Veja as linhas `FAMILIA divida`.' % (hms(maior_sem_fisico), sum(dividas_pagas.values()), dividas_sem_lugar))
    # A prova do pacote "nada termina em nada": duas em cada três. Abaixo disso, o alerta.
    if do_mod + len(sem_ninguem) >= 4 and len(na_mira) * 3 < 2 * (do_mod + len(sem_ninguem)):
        alertas.append('**Ele %s em %d de %d vezes que o mod quis mostrá-la** (%d criadas, %d sem lugar). '
                       'Referência: duas em cada três.' % (
                           'viu a criatura de verdade (na mira, com luz)' if mede_visto else 'teve a criatura na mira',
                           len(na_mira), do_mod + len(sem_ninguem), do_mod, len(sem_ninguem)))
    if maior_sem_ver is not None and maior_sem_ver > 40 * 60:
        alertas.append('**%s de jogo, da fase 3 em diante, sem ele ver a criatura de verdade.** A cota de corpo deveria ter resolvido aos 30 minutos: '
                       'veja as linhas `COTA` (não achou lugar com luz? ele ficou em casa, na vela ou em trégua?).' % hms(maior_sem_ver))
    garantidas = [t for t, _, d in deitou if d == 'GARANTIDA']
    if garantidas and not any(t >= garantidas[0] and texto.startswith('AVESSO chegou') for t, texto in linhas):
        alertas.append('**A primeira ida garantida foi decidida %d vez(es) e ele não chegou lá.** Saiu da cama antes dos dois segundos, '
                       'estava sendo caçado (`AVESSO garantia adiada`), ou a travessia falhou: procure `AVESSO levado` depois dos %s.' % (
                           len(garantidas), hms(garantidas[0] - inicio)))
    if tempo_fase.get(3, 0) + tempo_fase.get(4, 0) >= 60 * 60 and not any(n for _, n in escolhidos):
        alertas.append('Nenhum vazio por escolha (falso aviso, vigia falsa) em mais de uma hora de fase 3 ou 4: o mod está entregando '
                       'tudo o que prepara, e é a espera sem entrega que mantém o alerta. Veja se algum peso zerou.')
    if sum(corpo_falhou.values()) >= 6:
        alertas.append('A seleção de um evento com corpo falhou %d vezes (%s): veja a seção "Por que não achou lugar".' % (
            sum(corpo_falhou.values()), ', '.join('%s %d' % c for c in corpo_falhou.most_common())))
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
    mesmo_caminho = sum(1 for c in sem_caminho if c[4] == 'MESMO')
    if mesmo_caminho:
        alertas.append('**Numa caçada ele parou %d vez(es) no ponto mais perto do alvo a que conseguia chegar** (CAMINHO_FALHOU, caminho=MESMO). '
                       'Há um lugar a que o jogador chega e ele não. As linhas `sem_caminho` dizem onde ele estava e onde o alvo estava: confira no mundo '
                       'com `ler_mundo.py` (queda de mais de três blocos? mais de 64 blocos andando? água?) antes de mexer em qualquer coisa.' % mesmo_caminho)
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
