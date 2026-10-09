#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera a biblioteca de sons e a trilha do Sussurros.

Tudo aqui é sintetizado por código (ruído filtrado, ressonâncias, envelopes).
Não há gravação nem amostra de terceiros. A semente é fixa: rodar de novo gera
exatamente os mesmos arquivos.

Uso (com o ambiente virtual de ferramentas/sons/.venv):

    python gerar_sons.py                 gera tudo, atualiza o sounds.json e confere
    python gerar_sons.py --so assobio    gera só os sons citados (e confere esses)
    python gerar_sons.py --conferir      só mede os arquivos que já estão na pasta
    python gerar_sons.py --tabela        imprime a tabela de sons do LEIA-ME

Regras que o script segue e confere (sai com código diferente de zero se falhar):
  * mono = som com posição no mundo; estéreo = som "dentro da cabeça";
  * nunca alto: pico sempre abaixo de -3 dBFS, camadas de fundo bem abaixo;
  * sem componente contínua, sem clique nas pontas, loops emendando sem salto.

Os doze sons antigos (pano, respiracao, madeira, arrasto, estalo, grave) não são
gerados aqui e o script não toca neles.

Cada som é pensado a partir do mecanismo: o que vibra, o que excita e por onde
o som passa até o ouvido. Os comentários de cada gerador dizem qual é.
"""

from __future__ import annotations

import argparse
import json
import struct
import sys
import zlib
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

import numpy as np
import soundfile as sf
from scipy import fft as sfft
from scipy import ndimage, signal

SR = 44100
SEMENTE = 20261008
QUALIDADE_VORBIS = 0.3  # de 0 a 1; 0,3 dá arquivos do tamanho dos doze sons originais
QUALIDADES_LOOP = (0.30, 0.34, 0.38, 0.42, 0.46, 0.50, 0.55, 0.60)   # ver gravar_ogg
PISO_EMENDA = 0.0005    # -66 dBFS
ESTALO_MAXIMO_DB = 3.0  # quanto a emenda de um loop pode passar dos transientes vizinhos

RAIZ = Path(__file__).resolve().parents[2]
PASTA_SONS = RAIZ / "src" / "main" / "resources" / "assets" / "sussurros" / "sounds"
SOUNDS_JSON = PASTA_SONS.parent / "sounds.json"

DOIS_PI = 2.0 * np.pi


# ---------------------------------------------------------------------------
# Utilitários gerais
# ---------------------------------------------------------------------------

def rng_de(*chave) -> np.random.Generator:
    """Gerador aleatório próprio de cada som: a ordem de geração não muda nada."""
    texto = "/".join(str(c) for c in chave)
    return np.random.default_rng([SEMENTE, zlib.crc32(texto.encode("utf-8"))])


def ganho_db(db: float) -> float:
    return 10.0 ** (db / 20.0)


def em_db(valor: float) -> float:
    return 20.0 * np.log10(max(float(valor), 1e-12))


def amostras(segundos: float) -> int:
    return int(round(segundos * SR))


def tempo(n: int) -> np.ndarray:
    return np.arange(n) / SR


_SEMITONS = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def hz(nota: str) -> float:
    """'A4' -> 440.0; aceita sustenido (#) e bemol (b)."""
    letra, resto = nota[0], nota[1:]
    acidente = 0
    while resto[0] in "#b":
        acidente += 1 if resto[0] == "#" else -1
        resto = resto[1:]
    midi = 12 * (int(resto) + 1) + _SEMITONS[letra] + acidente
    return 440.0 * 2.0 ** ((midi - 69) / 12.0)


def cents(c):
    return 2.0 ** (np.asarray(c, dtype=float) / 1200.0)


def pico_1(x):
    """Devolve x com pico 1 (para misturar camadas por proporção)."""
    return x / (np.max(np.abs(x)) + 1e-12)


def rms_1(x):
    """Devolve x com desvio 1."""
    return x / (np.std(x) + 1e-12)


def domar(x, crista=4.0):
    """Arredonda os picos raros que passam de `crista` vezes o RMS do trecho que soa.

    Sem isso um grão solitário manda no pico e o resto do som fica baixo demais.
    """
    x = np.asarray(x, dtype=float)
    soando = np.abs(x) > 0.02 * np.max(np.abs(x))
    teto = crista * np.sqrt(np.mean(x[soando] ** 2))
    return teto * np.tanh(x / teto)


# --- filtros ---------------------------------------------------------------

def pb(x, fc, ordem=2):
    """Passa-baixa Butterworth."""
    return signal.sosfilt(signal.butter(ordem, fc, "low", fs=SR, output="sos"), x, axis=0)


def pa(x, fc, ordem=2):
    """Passa-alta Butterworth."""
    return signal.sosfilt(signal.butter(ordem, fc, "high", fs=SR, output="sos"), x, axis=0)


def pf(x, f1, f2, ordem=2):
    """Passa-faixa Butterworth."""
    return signal.sosfilt(signal.butter(ordem, [f1, f2], "band", fs=SR, output="sos"), x, axis=0)


def ressoar(x, fc, q, ganho=1.0):
    """Um modo de vibração: passa-faixa de dois polos com ganho 1 no pico."""
    w0 = DOIS_PI * fc / SR
    alfa = np.sin(w0) / (2.0 * q)
    b = [alfa, 0.0, -alfa]
    a = [1.0 + alfa, -2.0 * np.cos(w0), 1.0 - alfa]
    return ganho * signal.lfilter(b, a, x, axis=0)


def banco(x, modos, escala=1.0):
    """Soma de modos (fc, q, ganho): o corpo que o som atravessa ou faz vibrar."""
    y = np.zeros_like(np.asarray(x, dtype=float))
    for fc, q, g in modos:
        y += ressoar(x, fc * escala, q, g)
    return y


def modos_amortecidos(rng, n, modos):
    """Resposta de um corpo percutido: senoides amortecidas (f, t60, amplitude)."""
    t = tempo(n)
    y = np.zeros(n)
    for f, t60, amp in modos:
        if f >= 0.45 * SR:
            continue
        y += amp * np.sin(DOIS_PI * f * t + rng.uniform(0, DOIS_PI)) * np.exp(-6.908 * t / t60)
    return y


def filtrar_loop(x, ganho: Callable[[np.ndarray], np.ndarray]):
    """Filtro de fase zero aplicado em círculo: não quebra a emenda de um loop."""
    n = len(x)
    g = ganho(sfft.rfftfreq(n, 1.0 / SR))
    if np.ndim(x) == 2:
        g = g[:, None]
    return sfft.irfft(sfft.rfft(x, axis=0) * g, n, axis=0)


# --- ruído -----------------------------------------------------------------

def ruido_formado(rng, n, ganho: Callable[[np.ndarray], np.ndarray], sr=SR):
    """Ruído com o espectro dado por `ganho(f)`. É periódico em n (serve para loop)."""
    espectro = sfft.rfft(rng.standard_normal(n))
    y = sfft.irfft(espectro * ganho(sfft.rfftfreq(n, 1.0 / sr)), n)
    return y / (np.std(y) + 1e-12)


def ruido_rosa(rng, n):
    return ruido_formado(rng, n, lambda f: 1.0 / np.sqrt(np.maximum(f, 20.0)))


def ruido_na_faixa(rng, n, f1, f2, inclinacao=0.0):
    """Ruído periódico em n, limitado a f1-f2 com bordas suaves; `inclinacao` em dB/oitava."""
    def ganho(f):
        f = np.maximum(f, 1e-3)
        g = 1.0 / np.sqrt((1.0 + (f1 / f) ** 6) * (1.0 + (f / f2) ** 6))
        return g * (f / f1) ** (inclinacao / 6.02)
    return ruido_formado(rng, n, ganho)


def lento(rng, n, fc, ordem=4):
    """Flutuação aleatória lenta (ruído passa-baixa em fc), desvio 1, periódica em n."""
    def ganho(f):
        g = 1.0 / (1.0 + (f / fc) ** ordem)
        g[0] = 0.0
        return g
    passo = 64
    if fc <= 12.0 and n % passo == 0 and n >= passo * 256:
        # flutuação lenta em sinal comprido: calcula a 1/64 da taxa e interpola (em círculo)
        m = n // passo
        base = ruido_formado(rng, m, ganho, SR / passo)
        return np.interp(np.arange(n), np.arange(m) * passo, base, period=n)
    return ruido_formado(rng, n, ganho)


def ruido_em_banda(rng, fc, largura, n, ordem=1):
    """Ruído de banda estreita cujo centro pode andar no tempo.

    Ruído complexo passa-baixa (metade da largura) deslocado para fc(t). Com
    ordem 1 a banda tem a mesma forma de sino de uma ressonância de verdade.
    """
    fc = np.broadcast_to(np.asarray(fc, dtype=float), (n,))
    z = rng.standard_normal(n) + 1j * rng.standard_normal(n)
    z = signal.sosfilt(signal.butter(ordem, largura / 2.0, "low", fs=SR, output="sos"), z)
    fase = DOIS_PI * np.cumsum(fc) / SR
    y = (z * np.exp(1j * fase)).real
    return y / (np.std(y) + 1e-12)


# --- envelopes e controle --------------------------------------------------

def pontos(n, pares):
    """Envelope por trechos de reta: pares (segundo, valor)."""
    ts, vs = zip(*pares)
    return np.interp(tempo(n), ts, vs)


def alisar(x, ms, circular=False):
    """Suaviza um sinal de controle (três médias móveis: quase uma gaussiana)."""
    largura = max(1, int(2.0 * ms * SR / 1000.0))
    y = np.asarray(x, dtype=float)
    for _ in range(3):
        y = ndimage.uniform_filter1d(y, largura, mode="wrap" if circular else "nearest")
    return y


def degrau_suave(x):
    """0 até 1 sem quinas (x de 0 a 1)."""
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3.0 - 2.0 * x)


def fade(x, entrada_ms=4.0, saida_ms=30.0):
    """Entrada e saída em meio cosseno: nenhum arquivo começa ou termina num degrau."""
    x = np.array(x, dtype=float)
    n = len(x)
    ne = min(n // 2, max(2, amostras(entrada_ms / 1000.0)))
    ns = min(n // 2, max(2, amostras(saida_ms / 1000.0)))
    rampa_e = 0.5 - 0.5 * np.cos(np.pi * np.arange(ne) / ne)
    rampa_s = 0.5 + 0.5 * np.cos(np.pi * (np.arange(ns) + 1) / ns)
    if x.ndim == 2:
        rampa_e, rampa_s = rampa_e[:, None], rampa_s[:, None]
    x[:ne] *= rampa_e
    x[n - ns:] *= rampa_s
    return x


def somar(dest, trecho, inicio_s, ganho=1.0):
    """Soma `trecho` em `dest` a partir de `inicio_s`; o que passar do fim é cortado."""
    i = int(round(inicio_s * SR))
    if i < 0:
        trecho, i = trecho[-i:], 0
    m = min(len(trecho), len(dest) - i)
    if m > 0:
        dest[i:i + m] += ganho * trecho[:m]


def somar_circular(dest, trecho, inicio_s, ganho=1.0):
    """Como `somar`, mas o que passar do fim volta para o começo (camadas em loop)."""
    n = len(dest)
    i = int(round(inicio_s * SR)) % n
    pos = 0
    while pos < len(trecho):
        m = min(len(trecho) - pos, n - i)
        dest[i:i + m] += ganho * trecho[pos:pos + m]
        pos += m
        i = 0


def ajustar_tamanho(x, n):
    x = np.asarray(x, dtype=float)
    if len(x) >= n:
        return x[:n].copy()
    falta = [(0, n - len(x))] + [(0, 0)] * (x.ndim - 1)
    return np.pad(x, falta)


def panoramica(x, posicao):
    """Mono -> estéreo com potência constante. posicao: -1 (esquerda) a +1 (direita)."""
    angulo = (np.asarray(posicao, dtype=float) + 1.0) * np.pi / 4.0
    return np.stack([x * np.cos(angulo), x * np.sin(angulo)], axis=1)


# --- espaço ----------------------------------------------------------------

def resposta_reverb(rng, rt60, brilho=3500.0, predelay=0.02, difusao=0.010):
    """Resposta ao impulso de uma sala: ruído que decai, com os agudos morrendo antes."""
    n = amostras(rt60 * 1.15)
    t = tempo(n)
    bruto = rng.standard_normal(n)
    graves = pb(bruto, brilho, 1)
    agudos = bruto - graves
    ir = graves * np.exp(-6.908 * t / rt60) + agudos * np.exp(-6.908 * t / (0.4 * rt60))
    ir *= 1.0 - np.exp(-t / difusao)  # a sala demora um pouco para encher
    ir = fade(ir, 0.5, 1000.0 * rt60 * 0.25)
    ir = np.concatenate([np.zeros(amostras(predelay)), ir])
    return ir / np.sqrt(np.sum(ir ** 2))


def reverberar(x, rng, rt60, mistura, brilho=3500.0, predelay=0.02, circular=False):
    """Mono -> mono. `mistura` é a parte reverberada (0 a 1). Devolve com a cauda."""
    ir = resposta_reverb(rng, rt60, brilho, predelay)
    if circular:
        n = len(x)
        molhado = sfft.irfft(sfft.rfft(x) * sfft.rfft(ir, n), n)
        seco = x
    else:
        molhado = signal.fftconvolve(x, ir)
        seco = ajustar_tamanho(x, len(molhado))
    return (1.0 - mistura) * seco + mistura * molhado


def reverb_estereo(x, rng, rt60, mistura, brilho=3500.0, predelay=0.02, circular=False):
    """Mono ou estéreo -> estéreo, com uma sala diferente em cada ouvido."""
    x = np.asarray(x, dtype=float)
    if x.ndim == 1:
        x = np.stack([x, x], axis=1)
    lados = [reverberar(x[:, c], rng, rt60, mistura, brilho, predelay, circular) for c in range(2)]
    return np.stack(lados, axis=1)


def ao_longe(x, rng, corte, rt60, mistura, predelay=0.04):
    """Distância: o ar e os obstáculos comem os agudos e sobra mais sala que som direto."""
    y = pb(pb(x, corte, 1), corte * 1.6, 1)
    return reverberar(y, rng, rt60, mistura, brilho=corte, predelay=predelay)


# --- acabamento ------------------------------------------------------------

def acabar(x, pico_db, dur=None, entrada_ms=4.0, saida_ms=40.0, corte_grave=28.0):
    """Efeito avulso: tamanho final, sem contínua, pontas suaves e pico no alvo."""
    x = np.asarray(x, dtype=float)
    if dur is not None:
        x = ajustar_tamanho(x, amostras(dur))
    x = pa(x, corte_grave, 2)
    x = fade(x, entrada_ms, saida_ms)
    pico = np.max(np.abs(x))
    return x * (ganho_db(pico_db) / max(pico, 1e-9))


def acabar_loop(x, rms_db, corte_grave=24.0, girar=False, nivelar=0.0, janela=1.5):
    """Camada em loop: tira a contínua sem quebrar a periodicidade e põe o RMS no alvo.

    Com `nivelar` (0 a 1), um ganho lento segura as ondas que sobem acima do
    nível médio e levanta os vales: fundo não pode ter momento alto.
    Com `girar`, a camada (que é um círculo) é rodada para a emenda cair no seu
    momento mais calmo. Só serve para as camadas sem pulso: as da perseguição
    têm de começar no primeiro tempo.
    """
    y = filtrar_loop(np.asarray(x, dtype=float),
                     lambda f: np.where(f > 0, 1.0 / np.sqrt(1.0 + (corte_grave / np.maximum(f, 1e-6)) ** 8), 0.0))
    if nivelar:
        nivel = np.sqrt(ndimage.uniform_filter1d((y ** 2).sum(axis=1), amostras(janela), mode="wrap"))
        ganho = alisar((nivel / np.median(nivel)) ** (-nivelar), 1000.0 * janela / 4.0, circular=True)
        y = y * ganho[:, None]
    if girar:
        potencia = ndimage.uniform_filter1d((y ** 2).sum(axis=1), amostras(0.25), mode="wrap")
        y = np.roll(y, -int(np.argmin(potencia)), axis=0)
    rms = np.sqrt(np.mean(y ** 2))
    return y * (ganho_db(rms_db) / max(rms, 1e-9))


# ---------------------------------------------------------------------------
# O tema: cantiga de caixinha de música, lá menor, valsa 3/4, oito compassos
# ---------------------------------------------------------------------------

TEMA = [
    [("A4", 1), ("C5", 1), ("E5", 1)],
    [("D5", 1), ("C5", 1), ("B4", 1)],
    [("A4", 1), ("C5", 1), ("E5", 1)],
    [("F5", 2), ("E5", 1)],
    [("D5", 1), ("F5", 1), ("A5", 1)],
    [("G#5", 1), ("E5", 1), ("C5", 1)],
    [("B4", 1), ("D5", 1), ("C5", 1)],
    [("A4", 3)],
]
# Segunda voz: a tônica (ou o baixo do acorde) de cada compasso.
BAIXO = ["A3", "E3", "A3", "F3", "D3", "E3", "E3", "A3"]


def notas_do_tema(compassos=range(8), troca=None):
    """Lista de (tempo de início, nota, duração em tempos, compasso)."""
    saida, b = [], 0
    for c in compassos:
        for nome, tempos in TEMA[c]:
            saida.append((b, (troca or {}).get(nome, nome), tempos, c))
            b += tempos
    return saida


def trecho_do_tema(*compassos):
    """As notas de alguns compassos, como [(nota, tempos)]."""
    return [nota for c in compassos for nota in TEMA[c - 1]]


def grade_de_tempos(bpm, n_tempos=24, rit_de=None, rit=0.0, oscilacao=0.0, rng=None, pausas=None):
    """Instante do começo de cada tempo (n_tempos + 1 valores), com ritardando e tropeços."""
    dur = np.full(n_tempos, 60.0 / bpm)
    if rit_de is not None:
        s = np.clip((np.arange(n_tempos) - rit_de) / (n_tempos - rit_de), 0.0, 1.0)
        dur *= 1.0 + rit * s ** 1.5
    if oscilacao and rng is not None:
        dur *= 1.0 + oscilacao * np.clip(rng.standard_normal(n_tempos), -2.0, 2.0)
    for tempo_b, extra in (pausas or {}).items():
        dur[tempo_b] += extra * 60.0 / bpm
    return np.concatenate([[0.0], np.cumsum(dur)])


# ---------------------------------------------------------------------------
# Caixinha de música
# ---------------------------------------------------------------------------

def lamina(rng, f, forca=1.0, brilho=1.0, t60_base=3.0, tipo="normal"):
    """Uma lâmina do pente de aço, levantada e solta por um pino do cilindro.

    O que vibra: uma haste presa numa ponta. O fundamental domina; os modos de
    cima são levemente inarmônicos e morrem cedo; o parcial de 6,27x é o brilho
    metálico do ataque. O "clique" é a lâmina escapando do pino.
    """
    t60 = t60_base * (440.0 / f) ** 0.6
    if tipo == "muda":        # lâmina quebrada: só o pino batendo no toco
        t60 *= 0.035
    elif tipo == "abafada":   # lâmina encostando em alguma coisa
        t60 *= 0.16
    n = amostras(min(t60 * 1.25, 6.0) + 0.03)
    t = tempo(n)
    y = np.zeros(n)
    parciais = ((1.0, 1.0, 1.0), (2.006, 0.20, 0.5), (3.02, 0.07, 0.3),
                (4.05, 0.03, 0.22), (6.27, 0.11, 0.06))
    for i, (razao, amp, fracao) in enumerate(parciais):
        fp = f * razao
        if fp > 15000.0:
            continue
        fase = 0.0 if i == 0 else rng.uniform(0, DOIS_PI)
        a = amp if i == 0 else amp * brilho
        y += a * np.sin(DOIS_PI * fp * t + fase) * np.exp(-6.908 * t / (t60 * fracao))
    # dois modos quase iguais (a lâmina nunca é perfeita) dão um batimento lento
    y += 0.16 * np.sin(DOIS_PI * f * cents(rng.uniform(2.0, 5.0)) * t + 1.0) * np.exp(-6.908 * t / t60)
    y *= 1.0 - np.exp(-t / 0.0008)
    if tipo == "abafada":     # zumbe contra o que a abafa
        y *= 1.0 + 0.6 * np.sign(np.sin(DOIS_PI * 93.0 * t)) * np.exp(-t / 0.08)
    nc = amostras(0.02)
    clique = pf(rng.standard_normal(nc), 2500.0, 8000.0, 2) * np.exp(-tempo(nc) / 0.0025)
    y[:nc] += (0.20 if tipo != "muda" else 0.45) * brilho * pico_1(clique)
    return forca * y


def tocar_pente(rng, n, toques, t60_base=3.0, brilho=1.0):
    """Mistura os toques (t, f, forca, tipo). Uma lâmina que toca de novo cala a anterior."""
    notas = []
    for tq in sorted(toques, key=lambda q: q["t"]):
        tipo = tq.get("tipo", "normal")
        f, forca = tq["f"], tq.get("forca", 1.0)
        if tipo == "dupla":   # o pino engancha, escapa fraco e solta de novo
            atraso = rng.uniform(0.045, 0.07)
            notas.append([amostras(tq["t"]), round(f, 1), lamina(rng, f, 0.4 * forca, brilho, t60_base, "abafada")])
            notas.append([amostras(tq["t"] + atraso), round(f, 1), lamina(rng, f, forca, brilho, t60_base)])
        else:
            notas.append([amostras(tq["t"]), round(f, 1), lamina(rng, f, forca, brilho, t60_base, tipo)])
    y = np.zeros(n)
    for i, (i0, chave, vetor) in enumerate(notas):
        for j0, chave2, _ in notas[i + 1:]:
            if chave2 == chave:
                corte = j0 - i0 - amostras(0.012)
                if 0 < corte < len(vetor):
                    vetor[corte:] *= np.exp(-np.arange(len(vetor) - corte) / (0.004 * SR))
                break
        somar(y, vetor, i0 / SR)
    return y


def corpo_caixinha(x):
    """A caixa de madeira que irradia o som do pente (e não tem graves)."""
    y = x + 0.5 * ressoar(x, 620.0, 3.0) + 0.6 * ressoar(x, 1450.0, 4.0) + 0.35 * ressoar(x, 3100.0, 3.5)
    return pa(y, 170.0, 2)


def mecanismo(rng, velocidade, aspereza=0.0):
    """Ruído do cilindro girando: tiques da engrenagem, chiado do freio de ar, ronco do eixo.

    `velocidade` é um vetor (1 = andamento normal, 0 = parado). Com `aspereza`
    entra a ferrugem: raspadas irregulares. O resultado tem pico perto de 1.
    """
    n = len(velocidade)
    v = np.clip(velocidade, 0.0, None)
    fase = np.cumsum(v) / SR
    chiado = pf(rng.standard_normal(n), 900.0, 4200.0, 2)
    chiado = rms_1(chiado) * (0.6 + 0.4 * np.sin(DOIS_PI * 23.0 * fase)) * v ** 1.5
    idx = np.flatnonzero(np.diff(np.floor(fase * 11.0)) > 0) + 1
    impulsos = np.zeros(n)
    impulsos[idx] = rng.uniform(0.35, 1.0, len(idx))
    tiques = pico_1(banco(impulsos, [(2300.0, 8.0, 1.0), (3900.0, 10.0, 0.7), (5600.0, 9.0, 0.4)]))
    ronco = rms_1(pf(rng.standard_normal(n), 140.0, 420.0, 2)) * (0.7 + 0.3 * np.sin(DOIS_PI * 3.1 * fase)) * v
    y = 0.10 * chiado + 0.9 * tiques + 0.08 * ronco
    if aspereza > 0.0:
        raspa = ruido_em_banda(rng, 1700.0 + 500.0 * lento(rng, n, 1.5), 900.0, n)
        pulso = np.clip(lento(rng, n, 2.5) - 0.5, 0.0, None)
        y += aspereza * 0.35 * raspa * pico_1(pulso) ** 1.5 * v
    return y


def _caixinha(rng, bpm, afinacao=0.0, erro_lamina=0.0, troca=None, falhas=None,
              baixo_falha=(), sem_baixo=(), rit_de=14, rit=2.2, oscilacao=0.0, pausas=None,
              nivel_mecanismo=0.02, aspereza=0.0, brilho=1.0, corte=None, resto=4.6, acorde_final=True):
    """Toca o tema inteiro numa caixinha. Devolve o som já com caixa e sala."""
    grade = grade_de_tempos(bpm, 24, rit_de, rit, oscilacao, rng, pausas)
    inicio = 0.35
    t_ultima = inicio + grade[21]
    n = amostras(t_ultima + resto)
    desvio = {}  # cada lâmina tem a sua desafinação própria, fixa

    def freq(nome):
        if nome not in desvio:
            desvio[nome] = rng.normal(0.0, erro_lamina) if erro_lamina else 0.0
        return hz(nome) * cents(afinacao + desvio[nome])

    toques = []
    for i, (b, nome, _, _) in enumerate(notas_do_tema(troca=troca)):
        acento = 1.0 if b % 3 == 0 else 0.82
        toques.append({"t": inicio + grade[b], "f": freq(nome), "forca": acento * rng.uniform(0.92, 1.0),
                       "tipo": (falhas or {}).get(i, "normal")})
    for compasso, nome in enumerate(BAIXO):
        if compasso in sem_baixo:
            continue
        toques.append({"t": inicio + grade[compasso * 3] + 0.004, "f": freq(nome), "forca": 0.5,
                       "tipo": "abafada" if compasso in baixo_falha else "normal"})
    if acorde_final:   # a última nota vem com o baixo e, um instante depois, a quinta (lá, lá, mi)
        toques.append({"t": inicio + grade[21] + 0.07, "f": freq("E4"), "forca": 0.45})
    pente = tocar_pente(rng, n, toques, brilho=brilho)
    # o cilindro acompanha o andamento e para um pouco depois da última nota
    t = tempo(n)
    meio = 0.5 * (grade[:-1] + grade[1:]) + inicio
    vel = np.interp(t, meio, (60.0 / bpm) / np.diff(grade))
    vel *= np.clip((t_ultima + resto - 1.0 - t) / (resto - 2.4), 0.0, 1.0) ** 0.7
    vel *= np.clip(t / 0.25, 0.0, 1.0)
    y = corpo_caixinha(pente + nivel_mecanismo * mecanismo(rng, alisar(vel, 40.0), aspereza))
    if corte:
        y = pb(y, corte, 2)
    y = reverberar(y, rng, 0.25, 0.10, 5000.0, 0.004)   # a própria caixa
    y = reverberar(y, rng, 1.1, 0.10, 3500.0, 0.02)     # o cômodo
    return y[:n]


def gerar_caixa_musica(v, rng):
    y = _caixinha(rng, 96.0)
    return acabar(y, -7.0, 20.0, 10.0, 700.0)


def gerar_caixa_gasta(v, rng):
    if v == 1:
        y = _caixinha(rng, 80.0, afinacao=-35.0, erro_lamina=11.0,
                      falhas={7: "muda", 13: "abafada", 17: "dupla"}, baixo_falha=(3,),
                      rit_de=16, rit=0.9, oscilacao=0.05, resto=4.2,
                      nivel_mecanismo=0.05, aspereza=0.5, brilho=0.7, corte=6000.0)
    else:
        # A segunda está pior: o mi virou mi bemol, o cilindro engasga e a corda vai acabando.
        y = _caixinha(rng, 66.0, afinacao=-70.0, erro_lamina=19.0, troca={"E5": "Eb5"},
                      falhas={4: "muda", 9: "abafada", 12: "dupla", 15: "muda", 19: "abafada"},
                      baixo_falha=(1, 6), sem_baixo=(2, 5), rit_de=12, rit=1.3, oscilacao=0.09,
                      pausas={11: 0.7}, nivel_mecanismo=0.08, aspereza=1.0, brilho=0.5, corte=4500.0,
                      acorde_final=False)
    return acabar(y, -8.0, None, 10.0, 500.0)


def gerar_caixa_quebrada(v, rng):
    """As três primeiras notas do tema; a terceira mal soa e o mecanismo dá um tranco."""
    dur = 2.2
    n = amostras(dur)
    t_tranco = 1.375
    toques = [{"t": 0.05, "f": hz("A4"), "forca": 1.0}, {"t": 0.675, "f": hz("C5"), "forca": 0.85},
              {"t": 1.30, "f": hz("E5") * cents(-25.0), "forca": 0.9}]
    pente = tocar_pente(rng, n, toques)
    vel = np.where(tempo(n) < t_tranco, 1.0, 0.0)
    pente += 0.03 * mecanismo(rng, alisar(vel, 2.0), 0.5)
    # o tranco cala as lâminas: o pente leva o baque junto
    i = amostras(t_tranco)
    pente[i:] *= np.exp(-np.arange(n - i) / (0.09 * SR))
    # o tranco: a mola escapa e bate na chapa; várias lâminas são raspadas de uma vez
    nt = amostras(0.8)
    tt = tempo(nt)
    chapa = modos_amortecidos(rng, nt, [(318.0, 0.30, 1.0), (744.0, 0.42, 0.8), (1187.0, 0.34, 0.7),
                                         (1769.0, 0.26, 0.55), (2594.0, 0.2, 0.4), (3420.0, 0.15, 0.3),
                                         (4875.0, 0.1, 0.2)])
    golpe = pb(rng.standard_normal(nt), 3000.0, 1) * np.exp(-tt / 0.004)
    baque = np.sin(DOIS_PI * 118.0 * tt) * np.exp(-tt / 0.035)
    tranco = 0.55 * chapa + 0.9 * pico_1(golpe) + 0.8 * baque
    tranco *= 1.0 - np.exp(-tt / 0.0005)
    # a catraca escapa: meia dúzia de dentes batendo, cada vez mais devagar e mais fraco
    atraso, passo_c = 0.03, 0.019
    for k in range(6):
        nd = amostras(0.12)
        dente_c = modos_amortecidos(rng, nd, [(744.0, 0.07, 0.6), (1769.0, 0.06, 0.8), (2594.0, 0.05, 1.0),
                                              (3420.0, 0.04, 0.8), (4875.0, 0.03, 0.5)])
        dente_c += 0.6 * pico_1(pb(rng.standard_normal(nd), 3500.0, 1) * np.exp(-tempo(nd) / 0.0015))
        somar(tranco, dente_c, atraso, 0.34 * 0.78 ** k)
        atraso += passo_c
        passo_c *= 1.28
    raspadas = [{"t": t_tranco + 0.012 * k, "f": hz(nome), "forca": 0.55 - 0.07 * k, "tipo": "abafada"}
                for k, nome in enumerate(["F5", "D5", "C5", "B4", "G#4"])]
    y = pente + tocar_pente(rng, n, raspadas, brilho=1.4)
    somar(y, tranco, t_tranco, 0.75)
    # um último dente da engrenagem assentando
    dente = banco(np.concatenate([[1.0], np.zeros(amostras(0.08))]), [(2300.0, 8.0, 1.0), (3900.0, 10.0, 0.6)])
    somar(y, pico_1(dente), t_tranco + 0.47, 0.1)
    y = corpo_caixinha(y)
    y = reverberar(y, rng, 0.9, 0.12, 3500.0, 0.015)
    return acabar(y, -6.0, dur, 5.0, 250.0)


# ---------------------------------------------------------------------------
# Vozes: assobio, cantarolar, chamado
# ---------------------------------------------------------------------------

def frasear(rng, notas, bpm, transpor=1.0, erro=0.0, vies=0.0, rubato=0.05, antes=0.08):
    """[(nota, tempos)] -> [(início, fim, Hz)] com o que uma pessoa faz: rubato e afinação errada."""
    saida, t = [], antes
    for nome, tempos in notas:
        d = tempos * 60.0 / bpm * (1.0 + rng.uniform(-rubato, rubato))
        desvio = float(np.clip(rng.normal(0.0, erro), -1.5 * erro, 1.5 * erro)) if erro else 0.0
        saida.append((t, t + d, hz(nome) * transpor * cents(vies + desvio)))
        t += d
    return saida


def assobiar(rng, frase, ligado=True, vibrato=14.0, queda_final=0.0, sopro=0.10):
    """Assobio: o ar dos lábios faz a boca ressoar como uma garrafa. Quase uma senoide pura.

    O que denuncia que é gente: a nota chega por baixo, treme (vibrato), escorrega
    de uma para a outra, perde fôlego, e junto vem o sopro que não virou som.
    """
    n = amostras(frase[-1][1] + 0.25)
    t = tempo(n)
    lf = np.full(n, np.log2(frase[0][2]))
    amp, prof, arrasto = np.zeros(n), np.zeros(n), np.zeros(n)
    for k, (t0, t1, f) in enumerate(frase):
        i0, i1 = amostras(t0), min(n, amostras(t1))
        ultima = k == len(frase) - 1
        lf[i0:] = np.log2(f)
        d = t1 - t0
        calado = 0.035 if ligado else min(0.09, 0.3 * d)
        j1 = i1 - amostras(calado)
        local = (t[i0:i1] - t0) / d
        nivel = rng.uniform(0.8, 1.0)
        forma = (1.0 - local) ** 0.7 if ultima else 1.0 - 0.25 * local
        amp[i0:j1] = (nivel * forma)[:j1 - i0]
        if ligado and not ultima:
            amp[j1:i1] = 0.5 * nivel
        prof[i0:i1] = vibrato * np.clip((t[i0:i1] - t0 - 0.16) / 0.25, 0.0, 1.0)
        if not ligado or k == 0:
            arrasto[i0:i1] -= rng.uniform(50.0, 110.0) * np.exp(-(t[i0:i1] - t0) / 0.03)
        if ultima and queda_final:
            arrasto[i0:] -= queda_final * np.clip((t[i0:] - t0) / d, 0.0, 1.2) ** 3
    lf, amp, prof = alisar(lf, 13.0), np.clip(alisar(amp, 9.0), 0.0, None), alisar(prof, 30.0)
    vib = np.sin(DOIS_PI * np.cumsum(5.5 + 0.5 * lento(rng, n, 0.8)) / SR)
    f = 2.0 ** lf * cents(arrasto + prof * vib + 5.0 * lento(rng, n, 3.0) + 3.0 * lento(rng, n, 25.0))
    fase = DOIS_PI * np.cumsum(f) / SR
    tom = np.sin(fase) + 0.03 * np.sin(2 * fase + 1.0) + 0.012 * np.sin(3 * fase + 2.0)
    tom *= amp * (1.0 + 0.10 * lento(rng, n, 14.0)) * (f / np.mean(f)) ** 0.5
    # o sopro: tem a cor da boca (fica em volta da nota), mais um chiado nos lábios;
    # começa um instante antes de cada nota
    folego = np.clip(alisar(np.concatenate([amp[amostras(0.02):], np.zeros(amostras(0.02))]), 18.0), 0.0, None) ** 0.6
    ar = sopro * ruido_em_banda(rng, f, 900.0, n) + 0.25 * sopro * rms_1(pa(rng.standard_normal(n), 2500.0, 1))
    return tom + ar * folego


def gerar_assobio(v, rng):
    if v == 1:    # compassos 1-2, ligado e tranquilo
        frase = frasear(rng, trecho_do_tema(1, 2), 118.0, 2.0, erro=9.0, vies=-6.0)
        y = ao_longe(assobiar(rng, frase, True, 13.0), rng, 3200.0, 1.7, 0.45)
        dur = frase[-1][1] + 1.2
    elif v == 2:  # compassos 5-6, nota por nota, mais longe, mais desafinado
        frase = frasear(rng, trecho_do_tema(5, 6), 110.0, 2.0, erro=13.0, vies=-12.0, rubato=0.09)
        y = ao_longe(assobiar(rng, frase, False, 9.0, sopro=0.14), rng, 2400.0, 2.3, 0.60, 0.06)
        dur = frase[-1][1] + 1.3
    else:         # compassos 3-4, a última nota desiste e cai
        frase = frasear(rng, trecho_do_tema(3, 4), 108.0, 2.0, erro=11.0, vies=-9.0, rubato=0.07)
        y = ao_longe(assobiar(rng, frase, True, 20.0, queda_final=70.0, sopro=0.17), rng, 2800.0, 2.0, 0.52)
        dur = frase[-1][1] + 1.25
    return acabar(y, -10.0, dur, 8.0, 450.0)


def resposta_formantes(f, formantes):
    """Ganho do trato vocal na frequência f: soma de ressonâncias (centro, largura, ganho)."""
    h = 0.0
    for centro, largura, g in formantes:
        h = h + g / np.sqrt(1.0 + ((f - centro) / (0.5 * largura)) ** 2)
    return h


def voz(f0, formantes, inclinacao=2.0, fmax=5000.0):
    """Pregas vocais (harmônicos que caem `inclinacao` por oitava) passando pelos formantes.

    Síntese aditiva: cada harmônico k*f0(t) recebe o ganho do trato vocal na
    frequência em que está naquele instante. Devolve (som, fase do fundamental).
    """
    fase = DOIS_PI * np.cumsum(f0) / SR
    y = np.zeros(len(f0))
    for k in range(1, int(fmax / np.min(f0)) + 1):
        fk = k * f0
        y += k ** (-inclinacao) * resposta_formantes(fk, formantes) / (1.0 + (fk / fmax) ** 8) * np.sin(k * fase)
    return y, fase


def cantarolar(rng, frase, grave=False):
    """Cantarolar de boca fechada: a voz sai pelo nariz.

    Fonte: as pregas vocais. Filtro: o murmúrio nasal (uma ressonância forte em
    270 Hz e quase nada acima). O que faz parecer gente é a altura: chega por
    baixo, oscila, tem tremor de ciclo para ciclo e ar escapando junto.
    """
    n = amostras(frase[-1][1] + 0.3)
    t = tempo(n)
    fim = frase[-1][1]
    lf = np.full(n, np.log2(frase[0][2]))
    amp, prof, arrasto = np.zeros(n), np.zeros(n), np.zeros(n)
    for k, (t0, t1, f) in enumerate(frase):
        i0, i1 = amostras(t0), min(n, amostras(t1))
        ultima = k == len(frase) - 1
        lf[i0:] = np.log2(f)
        d = t1 - t0
        local = (t[i0:i1] - t0) / d
        nivel = rng.uniform(0.85, 1.0)
        forma = np.clip((1.0 - local) / 0.6, 0.0, 1.0) ** 0.8 if ultima else np.ones(i1 - i0)
        amp[i0:i1] = nivel * forma
        if not ultima:   # entre as notas a voz quase para ("hm-hm")
            amp[i1 - amostras(0.055):i1] = 0.5 * nivel
        prof[i0:i1] = (26.0 if grave else 20.0) * np.clip((t[i0:i1] - t0 - 0.28) / 0.3, 0.0, 1.0)
        arrasto[i0:i1] -= (150.0 if k == 0 else 40.0) * np.exp(-(t[i0:i1] - t0) / 0.07)
        if ultima:
            arrasto[i0:] -= 55.0 * np.clip((t[i0:] - t0) / d, 0.0, 1.2) ** 2
    amp *= 0.8 + 0.2 * np.sin(np.pi * np.clip(t / fim, 0.0, 1.0))
    lf, amp, prof = alisar(lf, 30.0), np.clip(alisar(amp, 24.0), 0.0, None), alisar(prof, 40.0)
    vib = np.sin(DOIS_PI * np.cumsum(4.7 + 0.5 * lento(rng, n, 0.7)) / SR)
    desvio = arrasto + prof * vib + 9.0 * lento(rng, n, 1.8) + 5.0 * lento(rng, n, 9.0) + 7.0 * lento(rng, n, 70.0)
    f0 = 2.0 ** lf * cents(desvio)
    nasal = 270.0 * (1.0 + 0.04 * lento(rng, n, 1.2))
    formantes = [(nasal, 110.0, 1.0), (1050.0, 260.0, 0.10), (2250.0, 320.0, 0.05), (3100.0, 400.0, 0.02)]
    y, fase = voz(f0, formantes, 1.4 if grave else 1.6)
    y *= amp * (1.0 + 0.05 * lento(rng, n, 40.0) + 0.06 * lento(rng, n, 4.0))
    if grave:   # voz grave começa rangendo: as pregas batem de duas em duas
        y *= 1.0 - 0.55 * np.exp(-t / 0.16) * (0.5 + 0.5 * np.cos(0.5 * fase))
    # o ar que escapa pelo nariz, pulsando com as pregas vocais
    ar = ruido_em_banda(rng, 2300.0, 1800.0, n) * (1.0 + 0.7 * np.cos(fase)) * amp ** 0.7
    return y + 0.018 * np.max(np.abs(y)) * ar


def gerar_cantarolar(v, rng):
    if v == 1:   # compassos 1-2, uma oitava abaixo: voz de mulher, embalando
        frase = frasear(rng, trecho_do_tema(1, 2), 88.0, 0.5, erro=10.0, vies=-8.0, rubato=0.06, antes=0.1)
        y = cantarolar(rng, frase)
        y = reverberar(pb(y, 3200.0, 1), rng, 0.9, 0.22, 2500.0, 0.015)
    else:        # compassos 3-4, duas oitavas abaixo: voz de homem, mais lenta
        frase = frasear(rng, trecho_do_tema(3, 4), 84.0, 0.25, erro=9.0, vies=-9.0, rubato=0.08, antes=0.1)
        y = cantarolar(rng, frase, grave=True)
        y = reverberar(pb(y, 2600.0, 1), rng, 1.3, 0.30, 2000.0, 0.02)
    return acabar(y, -11.0, min(5.0, frase[-1][1] + 0.6), 10.0, 350.0, corte_grave=60.0)


def gerar_chamado(v, rng):
    """Um chamado ao longe, entre bicho e gente.

    Voz forte (harmônicos caindo pouco), formantes de garganta curta demais para
    ser de adulto, altura que cai. A distância faz o resto: agudos cortados, um
    eco de encosta e muita reverberação.
    """
    if v == 1:
        # "gente": duas alturas, terça menor descendo, como quem chama um nome (ê-ôôô)
        n = amostras(1.65)
        f0 = pontos(n, [(0, 425), (0.09, 523), (0.55, 520), (0.70, 441), (1.15, 432), (1.65, 345)])
        amp = pontos(n, [(0, 0), (0.07, 1.0), (0.5, 0.85), (0.62, 0.6), (0.75, 1.0), (1.2, 0.75), (1.65, 0)])
        troca = degrau_suave((tempo(n) - 0.52) / 0.2)
        escala = 1.12
        f1 = (470.0 + 20.0 * troca) * escala
        f2 = (1950.0 - 1020.0 * troca) * escala
        f3 = 2600.0 * escala
        aspereza, eco, corte, rt60, mistura = 0.10, 0.34, 2300.0, 2.6, 0.62
    else:
        # "bicho": sobe depressa, quebra para cima e cai devagar, rouco (a-uuu)
        n = amostras(1.95)
        f0 = pontos(n, [(0, 232), (0.22, 352), (0.78, 364), (0.84, 432), (1.0, 414), (1.55, 306), (1.95, 238)])
        amp = pontos(n, [(0, 0), (0.12, 0.9), (0.7, 1.0), (0.8, 0.8), (1.3, 0.7), (1.95, 0)])
        troca = degrau_suave((tempo(n) - 0.35) / 0.9)
        escala = 1.2
        f1 = (720.0 - 340.0 * troca) * escala
        f2 = (1250.0 - 470.0 * troca) * escala
        f3 = (2600.0 - 300.0 * troca) * escala
        aspereza, eco, corte, rt60, mistura = 0.32, 0.27, 2000.0, 2.9, 0.68
    amp = np.clip(alisar(amp, 15.0), 0.0, None)
    f0 = alisar(f0, 12.0) * cents(14.0 * lento(rng, n, 5.0) + 9.0 * lento(rng, n, 40.0))
    y, fase = voz(f0, [(f1, 130.0, 1.0), (f2, 170.0, 0.7), (f3, 240.0, 0.35), (3500.0 * escala, 300.0, 0.15)], 1.25)
    # rouquidão: as pregas vibram irregulares (modulação por ruído de 30-70 Hz)
    y *= amp * (1.0 + aspereza * ruido_em_banda(rng, 50.0, 40.0, n))
    y += 0.03 * np.max(np.abs(y)) * ruido_em_banda(rng, f2, 600.0, n) * amp
    seco = np.zeros(n + amostras(eco) + 10)
    somar(seco, y, 0.0)
    somar(seco, pb(y, 1200.0, 1), eco, 0.4)     # o eco de uma encosta
    return acabar(ao_longe(seco, rng, corte, rt60, mistura, 0.07), -11.0, 3.0, 12.0, 900.0, corte_grave=80.0)


# ---------------------------------------------------------------------------
# Sussurro
# ---------------------------------------------------------------------------

# Formantes (F1, F2, F3) das vogais. No sussurro o F1 sobe um pouco.
VOGAIS = {
    "a": (760.0, 1320.0, 2500.0), "e": (430.0, 2050.0, 2650.0), "E": (590.0, 1850.0, 2550.0),
    "i": (310.0, 2300.0, 3050.0), "o": (440.0, 900.0, 2450.0), "O": (590.0, 1020.0, 2500.0),
    "u": (330.0, 820.0, 2350.0),
}
# Consoantes: (tipo, duração, F2 de onde a vogal parte, faixa do ruído, nível).
CONSOANTES = {
    "s": ("fricativa", 0.085, 1750.0, (4300.0, 9500.0), 0.55),
    "x": ("fricativa", 0.095, 2000.0, (2100.0, 5600.0), 0.65),
    "f": ("fricativa", 0.070, 1000.0, (1400.0, 8000.0), 0.22),
    "t": ("oclusiva", 0.055, 1750.0, (3200.0, 8500.0), 0.75),
    "k": ("oclusiva", 0.060, 2300.0, (1400.0, 3600.0), 0.80),
    "p": ("oclusiva", 0.060, 900.0, (400.0, 2200.0), 0.45),
    "m": ("nasal", 0.060, 1000.0, None, 0.28),
    "n": ("nasal", 0.055, 1650.0, None, 0.28),
    "l": ("liquida", 0.045, 1300.0, None, 0.50),
    "r": ("liquida", 0.030, 1500.0, None, 0.35),
    "h": ("sopro", 0.050, None, None, 0.45),
}


def _ler_silaba(s):
    """'ssa:\\'' -> ('s', 2, 'a', tônica, alongamento). ' marca a tônica; : alonga a vogal."""
    tonica = "'" in s
    alongar = 1.35 ** s.count(":")
    s = s.replace("'", "").replace(":", "")
    cons, rep = "", 1
    if s[0] in CONSOANTES:
        cons = s[0]
        while len(s) > 1 and s[1] == cons:
            rep += 1
            s = s[1:]
        s = s[1:]
    return cons, rep, s[0], tonica, alongar


def sussurrar(rng, frase, escala=1.0, ritmo=1.0):
    """Sussurro: sem pregas vocais, só o ar raspando na garganta e passando pela boca.

    Fonte: ruído. Filtro: os formantes das vogais, que andam de uma sílaba para a
    outra (é isso, mais o ritmo de 4 a 6 sílabas por segundo, que faz o ouvido
    reconhecer fala e não vento). Consoantes: chiados curtos (s, x, f) e
    estalos depois de um instante de silêncio (t, k, p). As sílabas não formam
    palavra nenhuma.
    """
    silabas = frase.split()
    trechos = [(0.04, *[f * escala for f in VOGAIS[_ler_silaba(silabas[0])[2]]], 0.0)]
    ruidos = []
    t = 0.04
    for k, s in enumerate(silabas):
        cons, rep, vogal, tonica, alongar = _ler_silaba(s)
        ultima = k == len(silabas) - 1
        f1, f2, f3 = (f * escala * rng.uniform(0.96, 1.04) for f in VOGAIS[vogal])
        f1 *= 1.12
        queda = 1.0 - 0.3 * k / max(len(silabas) - 1, 1)   # a frase perde força até o fim
        if cons:
            tipo, dc, locus, faixa, nivel = CONSOANTES[cons]
            dc *= ritmo ** 0.7 * (1.0 + 0.9 * (rep - 1))
            f2c = locus * escala if locus else f2
            if tipo == "fricativa":
                ruidos.append((t, dc, faixa, nivel * queda, False))
                trechos.append((dc, 330.0 * escala, f2c, 2700.0 * escala, 0.05))
            elif tipo == "oclusiva":
                trechos.append((dc, 330.0 * escala, f2c, 2600.0 * escala, 0.0))
                ruidos.append((t + dc - 0.004, 0.03, faixa, nivel * queda, True))
            elif tipo == "nasal":
                trechos.append((dc, 300.0 * escala, f2c, 2500.0 * escala, nivel * queda))
            elif tipo == "liquida":
                trechos.append((dc, 400.0 * escala, f2c, 2400.0 * escala, nivel * queda))
            else:
                trechos.append((dc, f1, f2, f3, nivel * queda))
            t += dc
        dv = rng.uniform(0.105, 0.15) * ritmo * (1.35 if tonica else 1.0) * alongar * (1.45 if ultima else 1.0)
        nivel = (1.25 if tonica else 0.9) * queda
        if ultima:   # a última vogal se desfaz em sopro
            trechos += [(0.55 * dv, f1, f2, f3, nivel), (0.45 * dv, f1, f2 * 0.97, f3, 0.4 * nivel)]
        else:
            trechos.append((dv, f1, f2, f3, nivel))
        t += dv
    trechos.append((0.12, *trechos[-1][1:4], 0.0))

    n = amostras(sum(tr[0] for tr in trechos))
    trilhas = np.zeros((4, n))
    i = 0
    for d, f1, f2, f3, a in trechos:
        j = min(n, i + amostras(d))
        trilhas[:, i:j] = np.array([f1, f2, f3, a])[:, None]
        i = j
    trilhas[:, i:] = trilhas[:, i - 1:i]
    f1, f2, f3 = (alisar(trilhas[k], 16.0) for k in range(3))
    a = np.clip(alisar(trilhas[3], 7.0), 0.0, None)
    # bandas de ordem 2: entre um formante e outro tem de sobrar um vale, senão vira chiado
    vogais = (0.7 * ruido_em_banda(rng, f1, 210.0, n, 2) + 1.0 * ruido_em_banda(rng, f2, 260.0, n, 2)
              + 0.7 * ruido_em_banda(rng, f3, 340.0, n, 2) + 0.35 * ruido_em_banda(rng, 3650.0 * escala, 420.0, n, 2)
              + 0.15 * ruido_em_banda(rng, 4600.0 * escala, 600.0, n, 2)
              + 0.09 * rms_1(pf(ruido_rosa(rng, n), 500.0, 5000.0, 1)))
    y = vogais * a * (1.0 + 0.18 * lento(rng, n, 35.0))
    for t0, d, (lo, hi), nivel, estouro in ruidos:
        m = amostras(d + 0.03)
        r = rms_1(pf(rng.standard_normal(m), lo * escala ** 0.5, hi * escala ** 0.5, 2))
        if estouro:
            tm = tempo(m)
            r *= np.exp(-tm / 0.007) * (1.0 - np.exp(-tm / 0.0005)) * 2.2
        else:
            r = fade(r, 22.0, 34.0)
        somar(y, r, t0, 1.6 * nivel)
    return pa(y, 280.0, 2)


def gerar_sussurro_voz(v, rng):
    # (sílabas sem sentido, tamanho da garganta, ritmo, sala: rt60 e mistura)
    receita = {
        1: ("xe ko' su:", 0.95, 1.45, 0.5, 0.10),                    # 3 sílabas, devagar, perto
        2: ("na si' to ra", 1.10, 1.05, 0.6, 0.12),                  # 4, voz mais fina
        3: ("fe xa' ku mi sa", 1.00, 0.85, 0.5, 0.10),               # 5, com pressa
        4: ("so te' ka la xi' nu:", 1.17, 0.95, 0.8, 0.16),          # 6, voz de criança, cantada
        5: ("ti sa ke' mo su fa' xe", 0.92, 0.80, 0.5, 0.10),        # 7, grave e atropelada
        6: ("ssa:' ho me ssi:", 1.04, 1.30, 1.0, 0.26),              # 4, arrastada, num lugar grande
    }[v]
    frase, escala, ritmo, rt60, mistura = receita
    y = sussurrar(rng, frase, escala, ritmo)
    if v == 6:   # uma segunda voz repete por cima, atrasada
        outra = sussurrar(rng, frase, 1.16, ritmo * 0.97)
        y = ajustar_tamanho(y, len(outra) + amostras(0.24))
        somar(y, outra, 0.24, 0.4)
    y = reverberar(y, rng, rt60, mistura, 5000.0, 0.008)
    dur = max(1.25, len(y) / SR - rt60 * 0.55)
    return acabar(y, -8.0, dur, 6.0, 180.0, corte_grave=120.0)


# ---------------------------------------------------------------------------
# Sopros: apagar a chama, acordar de repente
# ---------------------------------------------------------------------------

def gerar_sopro(v, rng):
    """Alguém sopra uma chama: jato de ar pelos lábios em bico.

    Começa com o estalo surdo dos lábios abrindo, o chiado é largo e escurece
    conforme o fôlego acaba; por baixo, o ar batendo (grave) e a chama tremendo
    antes de sumir.
    """
    dur = 0.5
    n = amostras(dur)
    t = tempo(n)
    env = (1.0 - np.exp(-t / 0.012)) * (0.4 + 0.6 * np.exp(-t / 0.07)) * np.clip((0.43 - t) / 0.2, 0.0, 1.0) ** 1.5
    centro = 1000.0 + 2400.0 * np.exp(-t / 0.10)
    jato = (ruido_em_banda(rng, centro, 2200.0, n) + 0.4 * ruido_em_banda(rng, 560.0, 300.0, n)
            + 0.35 * rms_1(pf(rng.standard_normal(n), 800.0, 7000.0, 1)))
    vento = rms_1(pb(rng.standard_normal(n), 170.0, 2)) * env ** 2
    tc = np.clip(t - 0.09, 0.0, None)
    chama = rms_1(pb(rng.standard_normal(n), 500.0, 2)) * (0.5 + 0.5 * np.sin(DOIS_PI * 38.0 * t)) \
        * np.exp(-tc / 0.06) * (t > 0.09)
    y = jato * env + 0.9 * vento + 0.35 * chama
    y = reverberar(y, rng, 0.3, 0.08, 4000.0, 0.005)
    return acabar(y, -8.0, dur, 3.0, 60.0, corte_grave=40.0)


def gerar_despertar(v, rng):
    """Acordar de repente: uma inspiração brusca, de boca aberta, que trava no alto.

    Ruído (o ar entrando) pelos formantes de uma boca que se abre: eles sobem
    junto com a força. Antes, um abafado grave que se solta, como sair de baixo
    d'água. Estéreo: o mesmo gesto com ar diferente em cada ouvido.
    """
    dur = 1.5
    n = amostras(dur)
    t = tempo(n)
    t_ini, t_fim = 0.36, 0.98
    subida = np.clip((t - t_ini) / (t_fim - t_ini), 0.0, 1.0)
    env = (1.0 - np.exp(-np.clip(t - t_ini, 0.0, None) / 0.07)) * (0.5 + 0.5 * subida) \
        * np.clip((t_fim + 0.03 - t) / 0.045, 0.0, 1.0)
    f1 = 480.0 + 520.0 * subida ** 1.3
    f2 = 1150.0 + 950.0 * subida
    f3 = 2500.0 + 700.0 * subida
    # um fio de voz no meio do ar (o susto), igual nos dois ouvidos
    f0 = (185.0 + 120.0 * subida) * cents(20.0 * lento(rng, n, 30.0))
    fio, _ = voz(f0, [(f1, 200.0, 1.0), (f2, 260.0, 0.6), (f3, 320.0, 0.3)], 1.8, 4000.0)
    fio = pico_1(fio) * env
    # o abafado do sono: incha devagar e se rompe quando o ar entra
    solta = rms_1(pb(rng.standard_normal(n), 240.0, 2)) * np.clip(t / t_ini, 0.0, 1.0) ** 2 \
        * np.clip((t_ini + 0.14 - t) / 0.14, 0.0, 1.0)
    def ar():
        return (1.0 * ruido_em_banda(rng, f1, 260.0, n) + 0.9 * ruido_em_banda(rng, f2, 320.0, n)
                + 0.5 * ruido_em_banda(rng, f3, 420.0, n)
                + 0.35 * rms_1(pa(rng.standard_normal(n), 3500.0, 1)) * subida) * env * (1.0 + 0.2 * lento(rng, n, 30.0))

    centro = ar()   # a maior parte é igual nos dois ouvidos (é a própria boca); o resto abre o som
    y = np.stack([0.8 * centro + 0.5 * ar(), 0.8 * centro + 0.5 * ar()], axis=1)
    y += (0.22 * fio + 0.45 * solta)[:, None] * np.max(np.abs(y)) / 3.0
    y = reverb_estereo(y, rng, 0.45, 0.14, 4000.0, 0.008)
    return acabar(y, -8.0, dur, 5.0, 250.0, corte_grave=40.0)


# ---------------------------------------------------------------------------
# Pancadas e batimentos
# ---------------------------------------------------------------------------

def gerar_batimento(v, rng):
    """Um ciclo do coração: tum (válvulas fechando, mais grave e longo) e tum (mais seco).

    O que se ouve é o peito vibrando: poucos modos graves, muito amortecidos. Os
    modos de 90-140 Hz e uma leve saturação garantem que apareça em caixa pequena.
    """
    dur = 0.8
    n = amostras(dur)
    if v == 1:   # calmo
        intervalo, f, tau, forca2 = 0.31, 1.0, 1.0, 0.72
    else:        # disparado: mais curto, mais alto, a segunda bulha quase igual à primeira
        intervalo, f, tau, forca2 = 0.235, 1.18, 0.82, 0.9

    def bulha(modos):
        m = amostras(0.4)
        t = tempo(m)
        y = np.zeros(m)
        for fm, tm, amp in modos:
            y += amp * np.sin(DOIS_PI * fm * f * t) * np.exp(-t / (tm * tau))
        y += 0.25 * rms_1(pb(rng.standard_normal(m), 160.0, 2)) * np.exp(-t / 0.03)
        return pico_1(y * (1.0 - np.exp(-t / 0.005)))

    y = np.zeros(n)
    somar(y, bulha([(52.0, 0.07, 0.8), (78.0, 0.06, 0.9), (108.0, 0.05, 0.9), (142.0, 0.035, 0.5)]), 0.04)
    somar(y, bulha([(64.0, 0.05, 0.7), (96.0, 0.04, 0.9), (128.0, 0.032, 0.8), (168.0, 0.022, 0.4)]),
          0.04 + intervalo, forca2)
    y = np.tanh(1.5 * y)
    y = pb(y, 320.0, 2)
    return acabar(y, -6.0, dur, 4.0, 150.0, corte_grave=30.0)


def gerar_pancada(v, rng):
    """Um corpo pesado e mole batendo numa parede.

    Corpo mole = contato demorado (mais de 10 ms), então quase não há agudo: a
    força é um pulso arredondado que põe a parede para vibrar nos modos graves.
    """
    dur = 0.5
    n = amostras(dur)
    t = tempo(n)
    if v == 1:   # parede de madeira: mais médios e alguma coisa solta que chacoalha
        modos = [(62.0, 0.28, 0.8), (96.0, 0.22, 1.0), (141.0, 0.16, 0.9), (212.0, 0.11, 0.6),
                 (318.0, 0.08, 0.35), (455.0, 0.06, 0.2), (640.0, 0.045, 0.1)]
        contato, corte_baque = 0.007, 300.0
    else:        # parede de pedra: mais funda e mais seca, com areia caindo
        modos = [(52.0, 0.30, 0.6), (78.0, 0.24, 1.0), (112.0, 0.17, 1.0), (160.0, 0.10, 0.6), (236.0, 0.06, 0.25)]
        contato, corte_baque = 0.008, 220.0
    nc = amostras(contato)
    forca = np.sin(np.pi * np.arange(nc) / nc) ** 2
    corpo = signal.fftconvolve(modos_amortecidos(rng, n, modos), forca)[:n]
    baque = signal.fftconvolve(pb(rng.standard_normal(n), corte_baque, 2) * np.exp(-t / 0.02), forca)[:n]
    y = pico_1(corpo) + 0.5 * pico_1(baque)
    if v == 1:
        for atraso, g in ((0.031, 0.05), (0.058, 0.035), (0.097, 0.02)):
            solto = modos_amortecidos(rng, amostras(0.08), [(1150.0, 0.05, 1.0), (1900.0, 0.04, 0.7), (2750.0, 0.03, 0.4)])
            somar(y, solto, atraso, g)
    else:
        graos = (rng.random(n) < 300.0 / SR * np.exp(-np.clip(t - 0.03, 0, None) / 0.12) * (t > 0.03)) \
            * rng.uniform(0.3, 1.0, n)
        y += 0.10 * pf(graos, 2200.0, 6000.0, 2)
    y = np.tanh(1.3 * y)
    y = reverberar(y, rng, 0.45, 0.14, 1500.0, 0.008)
    return acabar(y, -5.0, dur, 2.0, 160.0, corte_grave=30.0)


# ---------------------------------------------------------------------------
# Atrito: arranhar, ranger, giz, tigela
# ---------------------------------------------------------------------------

def atrito(rng, vel, taxa_base, tranco_ms=1.2, irregular=0.18, chiado=0.12):
    """Fonte de atrito seco: prende e solta.

    A ponta que arrasta (unha, giz, pé de cerâmica) fica presa, verga e escapa,
    dezenas ou centenas de vezes por segundo: cada escapada é um estalinho de
    ruído. Quanto maior a velocidade `vel` (0 a 1), mais escapadas por segundo
    e mais fortes. Entre elas sobra um chiado fino.
    """
    n = len(vel)
    taxa = taxa_base * (0.35 + 0.65 * vel) * (1.0 + irregular * lento(rng, n, 40.0))
    fase = np.cumsum(np.clip(taxa, 5.0, None)) / SR
    idx = np.flatnonzero(np.diff(np.floor(fase)) > 0) + 1
    pulsos = np.zeros(n)
    pulsos[idx] = rng.uniform(0.4, 1.0, len(idx)) * vel[idx]
    env = signal.lfilter([1.0], [1.0, -np.exp(-1.0 / (tranco_ms / 1000.0 * SR))], pulsos)
    return rng.standard_normal(n) * (env + chiado * vel)


def _risco_de_unha(rng, dur, material):
    """Uma unha arrastada. Mais rápido = mais trancos por segundo, mais forte e mais agudo."""
    n = amostras(dur)
    u = tempo(n) / dur
    vel = np.clip(np.sin(np.pi * u ** 0.6) ** 1.2 * (1.0 + 0.25 * lento(rng, n, 18.0)), 0.0, None)
    if material == "madeira":   # a tábua responde com os modos dela; a unha, com um chiado médio
        fonte = atrito(rng, vel, rng.uniform(150.0, 210.0))
        y = 0.5 * pf(fonte, 1400.0, 4800.0, 1) + 2.2 * banco(fonte, [(330.0, 8.0, 0.6), (540.0, 10.0, 0.7),
                                                                   (870.0, 10.0, 0.55), (1300.0, 11.0, 0.45),
                                                                   (2100.0, 9.0, 0.3)])
        y = pb(y, 6500.0, 2)
    else:                       # pedra: não ressoa, é mais áspera, mais aguda e chia mais
        fonte = atrito(rng, vel, rng.uniform(260.0, 380.0), 0.8, 0.25, 0.3)
        y = pf(fonte, 2400.0, 8000.0, 1) + 0.35 * banco(fonte, [(1700.0, 6.0, 0.5), (3400.0, 7.0, 0.5),
                                                               (5200.0, 6.0, 0.4)])
    return y * (0.55 + 0.45 * vel) + 0.4 * pb(y, 1200.0, 1) * (1.0 - vel)


def _arranhao(rng, dur, material, dedos=4):
    """Quatro unhas, cada uma um pouco atrás da outra."""
    y = np.zeros(amostras(dur * 1.1 + 0.1))
    atraso = 0.0
    for _ in range(dedos):
        somar(y, _risco_de_unha(rng, dur * rng.uniform(0.9, 1.05), material), atraso, rng.uniform(0.6, 1.0))
        atraso += rng.uniform(0.006, 0.02)
    return y


def gerar_arranhar(v, rng):
    if v == 1:     # um arranhão comprido e lento na madeira
        dur, material, golpes = 1.25, "madeira", [(0.03, 1.02, 1.0)]
    elif v == 2:   # três arranhões curtos e nervosos, o último mais forte
        dur, material, golpes = 0.95, "madeira", [(0.02, 0.19, 0.7), (0.29, 0.19, 0.8), (0.55, 0.27, 1.0)]
    else:          # pedra: um longo, uma pausa, outro curto, e areia caindo
        dur, material, golpes = 1.55, "pedra", [(0.03, 0.72, 1.0), (0.92, 0.4, 0.8)]
    y = np.zeros(amostras(dur))
    for inicio, d, g in golpes:
        somar(y, _arranhao(rng, d, material), inicio, g)
    if material == "pedra":
        n = len(y)
        t = tempo(n)
        areia = (rng.random(n) < 500.0 / SR * np.exp(-np.clip(t - 0.75, 0, None) / 0.3) * (t > 0.2)) \
            * rng.uniform(0.3, 1.0, n)
        y += 3.5 * np.std(y) * pf(areia, 3000.0, 9000.0, 2)
    y = reverberar(domar(y, 3.5), rng, 0.4, 0.10, 3500.0, 0.006)
    return acabar(y, -7.0, dur, 3.0, 90.0, corte_grave=80.0)


MODOS_MADEIRA = [(125.0, 5.0, 0.3), (250.0, 7.0, 0.5), (395.0, 9.0, 0.8), (560.0, 10.0, 1.0),
                 (790.0, 12.0, 0.9), (1180.0, 12.0, 0.6), (1750.0, 10.0, 0.4), (2600.0, 9.0, 0.25)]


def _rangido(rng, taxa, forca, escala=1.0, irregular=0.08):
    """Madeira presa que escorrega em trancos.

    Cada tranco é um pulso que excita os modos da peça. Devagar ouvem-se os
    trancos um a um; depressa eles se juntam numa nota que sobe e desce.
    """
    n = len(taxa)
    fase = np.cumsum(taxa * (1.0 + irregular * lento(rng, n, 25.0))) / SR
    idx = np.flatnonzero(np.diff(np.floor(fase)) > 0) + 1
    exc = np.zeros(n)
    exc[idx] = forca[idx] * rng.uniform(0.55, 1.0, len(idx))
    exc = signal.lfilter([1.0], [1.0, -np.exp(-1.0 / (0.0004 * SR))], exc)   # pulso curto, não clique
    return pb(banco(exc, MODOS_MADEIRA, escala), 3800.0 * escala ** 0.5, 2)


def gerar_ranger(v, rng):
    if v == 1:     # tábua do chão cedendo sob um peso: gemido curto e grave, com um estalo
        dur = 0.95
        n = amostras(dur)
        taxa = pontos(n, [(0, 22), (0.25, 48), (0.5, 72), (0.72, 40), (0.9, 22)])
        forca = pontos(n, [(0, 0), (0.06, 0.7), (0.5, 1.0), (0.74, 0.5), (0.9, 0)])
        y = _rangido(rng, taxa, forca, 0.8)
        estalo = modos_amortecidos(rng, amostras(0.12), [(640.0, 0.09, 1.0), (1320.0, 0.07, 0.8),
                                                         (2450.0, 0.05, 0.6), (3900.0, 0.03, 0.3)])
        somar(y, pico_1(estalo), 0.74, 0.9 * np.max(np.abs(y)))
    elif v == 2:   # porta abrindo devagar: engasga no meio e termina subindo
        dur = 1.9
        n = amostras(dur)
        taxa = pontos(n, [(0, 14), (0.5, 30), (0.9, 34), (1.2, 60), (1.6, 118), (1.85, 135)])
        forca = pontos(n, [(0, 0), (0.05, 0.7), (0.62, 0.8), (0.7, 0.0), (0.82, 0.0), (0.9, 0.8),
                           (1.6, 1.0), (1.82, 0.5), (1.87, 0)])
        y = _rangido(rng, taxa, forca, 1.0)
    else:          # peça pequena sob tensão: estalos que aceleram e viram um guincho
        dur = 1.3
        n = amostras(dur)
        taxa = pontos(n, [(0, 7), (0.45, 16), (0.62, 60), (0.9, 190), (1.25, 150)])
        forca = pontos(n, [(0, 0.9), (0.45, 0.9), (0.62, 0.7), (0.9, 1.0), (1.15, 0.6), (1.24, 0)])
        y = _rangido(rng, taxa, forca, 1.7, 0.05)
    y = reverberar(y, rng, 0.5, 0.12, 3000.0, 0.008)
    return acabar(y, -7.0, dur, 3.0, 80.0, corte_grave=60.0)


def gerar_giz(v, rng):
    """Giz riscando pedra: prende e solta bem depressa (por isso às vezes canta) e esfarela."""
    def risco(d, forca):
        n = amostras(d)
        u = tempo(n) / d
        vel = np.clip(np.sin(np.pi * u ** 0.8) ** 0.8 * (1.0 + 0.2 * lento(rng, n, 30.0)), 0.0, None)
        fonte = atrito(rng, vel, rng.uniform(380.0, 520.0), 0.6, 0.22, 0.35)
        canto = ruido_em_banda(rng, 2900.0 * (0.9 + 0.2 * vel), 140.0, n) * vel ** 2 * np.std(fonte)
        return forca * (pf(fonte, 1500.0, 6500.0, 2) + 0.5 * canto + 0.3 * pf(fonte, 250.0, 800.0, 1))

    def toque(forca):   # o giz encostando: um "tec" seco
        m = amostras(0.04)
        return forca * modos_amortecidos(rng, m, [(3400.0, 0.02, 1.0), (5300.0, 0.012, 0.5), (700.0, 0.02, 0.6)])

    if v == 1:   # um traço só, comprido
        dur = 0.55
        y = np.zeros(amostras(dur))
        somar(y, risco(0.44, 1.0), 0.03)
        y = domar(y, 3.5)
        somar(y, toque(0.6 * np.max(np.abs(y))), 0.02)
    else:        # dois traços curtos, um cruzando o outro
        dur = 0.62
        y = np.zeros(amostras(dur))
        somar(y, risco(0.17, 0.9), 0.03)
        somar(y, risco(0.24, 1.0), 0.30)
        y = domar(y, 3.5)
        pico = np.max(np.abs(y))
        somar(y, toque(0.5 * pico), 0.02)
        somar(y, toque(0.65 * pico), 0.29)
    y = reverberar(y, rng, 0.35, 0.08, 4000.0, 0.005)
    return acabar(y, -9.0, dur, 2.0, 50.0, corte_grave=120.0)


def gerar_tigela(v, rng):
    """Uma tigela de cerâmica arrastada um palmo e assentando.

    O pé sem esmalte raspa na pedra (ruído em grãos) e faz a própria tigela
    cantar baixinho; quando ela assenta, bate duas vezes. Apoiada, a cerâmica
    soa seca: os modos (bem inarmônicos) morrem em um décimo de segundo.
    """
    dur = 0.6
    y = np.zeros(amostras(dur))
    nr = amostras(0.27)
    u = tempo(nr) / 0.27
    vel = np.clip(np.sin(np.pi * u ** 0.7) ** 1.2 * (1.0 + 0.3 * lento(rng, nr, 25.0)), 0.0, None)
    fonte = atrito(rng, vel, 240.0, 0.9, 0.25, 0.25)
    raspa = (pf(fonte, 1800.0, 7000.0, 2) + 0.6 * banco(fonte, [(1340.0, 30.0, 1.0), (2870.0, 35.0, 0.7),
                                                               (4480.0, 30.0, 0.5)])
             + 0.3 * pf(fonte, 300.0, 900.0, 1))
    somar(y, pico_1(domar(raspa, 3.5)), 0.03, 0.6)
    for inicio, forca in ((0.33, 1.0), (0.415, 0.45)):
        m = amostras(0.2)
        tm = tempo(m)
        toque = modos_amortecidos(rng, m, [(1340.0, 0.16, 1.0), (2870.0, 0.12, 0.7), (4480.0, 0.09, 0.5),
                                           (6150.0, 0.06, 0.3), (8300.0, 0.04, 0.15)])
        toque += 0.8 * np.sin(DOIS_PI * 410.0 * tm) * np.exp(-tm / 0.012)      # o "toc" da mesa
        toque += 0.5 * pico_1(pa(rng.standard_normal(m), 3000.0, 2) * np.exp(-tm / 0.0015))
        toque *= 1.0 - np.exp(-tm / 0.0004)
        somar(y, pico_1(toque), inicio, forca)
    y = reverberar(y, rng, 0.35, 0.08, 4500.0, 0.005)
    return acabar(y, -8.0, dur, 2.0, 60.0, corte_grave=120.0)


# ---------------------------------------------------------------------------
# Sino, vento, zumbido
# ---------------------------------------------------------------------------

# Parciais de um sino de terça menor: (razão, amplitude, fração da duração do zumbido).
PARCIAIS_SINO = [(0.5, 0.55, 1.0), (1.0, 0.75, 0.62), (1.19, 0.6, 0.5), (1.5, 0.3, 0.38), (2.0, 1.0, 0.32),
                 (2.51, 0.3, 0.22), (2.66, 0.28, 0.2), (3.01, 0.4, 0.17), (4.1, 0.28, 0.11),
                 (5.43, 0.18, 0.07), (6.8, 0.12, 0.05), (8.2, 0.08, 0.035)]


def badalada(rng, f, dur, t60=8.0, rachado=0.0, escorrega=0.0):
    """Um sino batido pelo badalo.

    Os parciais não são harmônicos (zumbido uma oitava abaixo, terça menor, quinta...).
    Cada um é um par de modos quase iguais, que batem entre si: é o que faz o som
    ondular. O zumbido grave é o que dura; os agudos somem em instantes.
    """
    n = amostras(dur)
    t = tempo(n)
    y = np.zeros(n)
    curva = cents(-escorrega * (1.0 - np.exp(-t / 0.8)))   # sino "torto": a altura cede
    for razao, amp, fracao in PARCIAIS_SINO:
        fp = f * razao
        if fp > 9000.0:
            continue
        abre = rng.uniform(0.0006, 0.002) * (1.0 + 4.0 * rachado)
        decai = np.exp(-6.908 * t / (t60 * fracao * (1.0 - 0.5 * rachado)))
        for lado, peso in ((1.0, 1.0), (-1.0, rng.uniform(0.4, 0.9))):
            fase = DOIS_PI * np.cumsum(fp * (1.0 + lado * abre) * curva) / SR
            y += amp * peso * np.sin(fase + rng.uniform(0, DOIS_PI)) * decai
    golpe = pico_1(pf(rng.standard_normal(n), 800.0, 5000.0, 1) * np.exp(-t / 0.006))
    return (y + 0.5 * golpe) * (1.0 - np.exp(-t / 0.0015))


def gerar_sino_longe(v, rng):
    if v == 1:   # sino grande (lá), uma badalada só
        dur = 6.0
        y = badalada(rng, 220.0, dur, 16.0)
        vento, corte, rt60, mistura = 0.22, 2200.0, 2.6, 0.50
    else:        # sino menor e rachado, quase um mi; duas badaladas, bem mais longe
        dur = 5.6
        y = badalada(rng, hz("E4") * cents(-32.0), dur, 11.0, rachado=0.6)
        somar(y, badalada(rng, hz("E4") * cents(-32.0), dur - 2.3, 11.0, rachado=0.6), 2.3, 0.7)
        vento, corte, rt60, mistura = 0.4, 1500.0, 3.0, 0.66
    n = len(y)
    y = ao_longe(y, rng, corte, rt60, mistura, 0.06)[:n]
    y *= 1.0 + vento * np.clip(lento(rng, n, 0.7), -2.0, 2.0)     # o vento traz e leva o som
    return acabar(y, -8.0, dur, 6.0, 1500.0, corte_grave=45.0)


def gerar_vento_oco(v, rng):
    """Uma rajada passando por uma fresta que dá para um oco.

    O ar turbulento é ruído; o oco (um cano, um vão) ressoa numa nota e nos
    harmônicos ímpares dela, e essa nota sobe quando o vento aperta.
    """
    if v == 1:     # oco grande e grave, uma rajada só
        dur, picos, f_res, largura, a3, a5, ar, bate = 6.0, [(2.6, 1.25, 1.0)], 172.0, 28.0, 0.35, 0.15, 0.30, 0.0
    elif v == 2:   # fresta estreita: assobia, duas rajadas
        dur, picos, f_res, largura, a3, a5, ar, bate = 5.0, [(1.3, 0.6, 0.7), (3.2, 0.75, 1.0)], 430.0, 22.0, 0.22, 0.08, 0.22, 0.0
    else:          # vão largo, mais sopro que nota, e alguma coisa solta batendo
        dur, picos, f_res, largura, a3, a5, ar, bate = 7.0, [(1.8, 0.8, 0.6), (3.9, 1.0, 1.0), (5.6, 0.6, 0.5)], 255.0, 80.0, 0.3, 0.2, 0.55, 0.35
    n = amostras(dur)
    t = tempo(n)
    vel = sum(alt * np.exp(-0.5 * ((t - tc) / larg) ** 2) for tc, larg, alt in picos)
    vel = np.clip(vel * (1.0 + 0.12 * lento(rng, n, 2.5) + 0.06 * lento(rng, n, 9.0)), 0.0, None)
    centro = f_res * (0.82 + 0.36 * vel)
    oco = (ruido_em_banda(rng, centro, largura, n) * vel ** 1.6
           + a3 * ruido_em_banda(rng, 3.0 * centro, 2.0 * largura, n) * vel ** 2.2
           + a5 * ruido_em_banda(rng, 5.0 * centro, 3.0 * largura, n) * vel ** 2.8)
    bruto = ruido_rosa(rng, n)
    sopro = (rms_1(pf(bruto, 120.0, 700.0, 1)) * (1.0 - 0.6 * vel) + rms_1(pf(bruto, 700.0, 3200.0, 1)) * 0.8 * vel) \
        * vel ** 1.3
    ronco = rms_1(pb(rng.standard_normal(n), 110.0, 2)) * vel
    y = oco + ar * sopro + 0.4 * ronco
    if bate:
        y *= 1.0 + bate * vel * np.sin(DOIS_PI * np.cumsum(9.0 + 4.0 * vel) / SR)
    y = reverberar(y, rng, 1.2, 0.2, 2500.0, 0.015)
    return acabar(y, -9.0, dur, 300.0, 600.0, corte_grave=40.0)


def gerar_zumbido(v, rng):
    """Zumbido de ouvido: um fio agudo que entra e sai devagar.

    Em cada ouvido, o tom e um vizinho muito próximo e mais fraco: batem devagar,
    num ritmo diferente de cada lado. Um abafado grave por baixo dá a sensação de
    ouvido tampado.
    """
    dur = 4.0
    n = amostras(dur)
    t = tempo(n)
    env = degrau_suave(t / 1.3) * degrau_suave((dur - 0.05 - t) / 1.9)
    lados = []
    for base, vizinho in ((7230.0, 2.1), (7231.0, -2.6)):
        deriva = DOIS_PI * np.cumsum(3.0 * lento(rng, n, 0.4)) / SR
        tom = np.sin(DOIS_PI * base * t + deriva) + 0.28 * np.sin(DOIS_PI * (base + vizinho) * t + deriva + 1.0)
        tom += 0.09 * np.sin(DOIS_PI * (base * 1.094) * t + 2.0) * degrau_suave((t - 0.8) / 1.5)
        abafado = rms_1(pb(rng.standard_normal(n), 260.0, 2))
        lados.append((tom + 0.05 * abafado) * env)
    return acabar(np.stack(lados, axis=1), -18.0, dur, 20.0, 100.0, corte_grave=40.0)


# ---------------------------------------------------------------------------
# Transições (estéreo): apagão, sonho
# ---------------------------------------------------------------------------

def gerar_apagao(v, rng):
    """Tudo some. Um ruído cresce como som tocado de trás para a frente, corta seco,
    e fica um grave que desce, como uma máquina grande perdendo a força."""
    dur = 2.5
    n = amostras(dur)
    t = tempo(n)
    tc = 1.2
    ic = amostras(tc)
    cresce = np.exp((t - tc) / 0.30) * (t < tc)
    abre = np.clip(t / tc, 0.0, 1.0) ** 2
    # o que estava soando é sugado: notas graves ao contrário, iguais nos dois ouvidos
    tt = tempo(ic)
    sinos = np.zeros(ic)
    for f in (110.0, 164.8, 233.1, 311.1):
        sinos += np.sin(DOIS_PI * f * tt + rng.uniform(0, DOIS_PI)) * np.exp(-tt / 0.45)
    sinos = pico_1(sinos[::-1])
    lados = []
    for _ in range(2):
        r = rng.standard_normal(n)
        chiado = (2.0 * rms_1(pb(r, 450.0, 2)) * (1.0 - abre) + rms_1(pf(r, 350.0, 4200.0, 1)) * abre) * cresce
        lado = 0.75 * pico_1(chiado)
        lado[:ic] += 0.35 * sinos
        lado[:ic] = fade(lado[:ic], 5.0, 2.5)      # o corte seco (2,5 ms, para não estalar)
        lados.append(lado)
    tg = np.clip(t - tc, 0.0, None)
    fase = DOIS_PI * np.cumsum(36.0 + 62.0 * np.exp(-tg / 0.38)) / SR
    grave = (np.sin(fase) + 0.35 * np.sin(2 * fase) + 0.12 * np.sin(3 * fase)) \
        * (1.0 - np.exp(-tg / 0.012)) * np.exp(-tg / 0.5) * (t >= tc)
    y = np.stack(lados, axis=1) + (1.0 * pico_1(grave))[:, None]
    return acabar(y, -8.0, dur, 5.0, 300.0, corte_grave=30.0)


def gerar_sonho(v, rng):
    """Passagem para um sonho: um acorde de lá menor, suave, que desafina e afunda.

    Cada voz escorrega para baixo num ritmo próprio e treme cada vez mais, como
    fita perdendo velocidade; ao mesmo tempo tudo fica abafado.
    """
    dur = 8.0
    n = amostras(dur)
    t = tempo(n)
    t0 = 2.6
    desce = np.clip((t - t0) / (dur - t0), 0.0, 1.0)
    corte = 3800.0 * (1.0 - desce) ** 2 + 420.0

    def abafar(f):
        return 1.0 / np.sqrt(1.0 + (f / corte) ** 4)

    y = np.zeros((n, 2))
    # (nota, posição, entrada, quanto afunda em cents)
    for nota, pos, entrada, afunda in (("A3", -0.5, 0.0, 140.0), ("E4", 0.4, 0.3, 300.0), ("A4", -0.2, 0.55, 200.0),
                                       ("C5", 0.6, 0.8, 460.0), ("E5", -0.6, 1.1, 360.0)):
        voz_ = np.zeros(n)
        for desafina, peso in ((0.0, 1.0), (rng.uniform(3.0, 6.0), 0.7)):
            f = hz(nota) * cents(desafina - afunda * desce ** 1.8 + (4.0 + 22.0 * desce) * lento(rng, n, 1.2))
            fase = DOIS_PI * np.cumsum(f) / SR + rng.uniform(0, DOIS_PI)
            voz_ += peso * (abafar(f) * np.sin(fase) + 0.18 * abafar(2 * f) * np.sin(2 * fase)
                            + 0.07 * abafar(3 * f) * np.sin(3 * fase))
        env = degrau_suave((t - entrada) / 1.5) * (1.0 - desce ** 1.6)
        y += panoramica(voz_ * env * (220.0 / hz(nota)) ** 0.3, pos)
    for c in range(2):   # um pouco de ar por cima
        y[:, c] += 0.05 * ruido_em_banda(rng, 1500.0, 1400.0, n) * degrau_suave(t / 2.0) * (1.0 - desce) * 3.0
    y = reverb_estereo(y, rng, 2.4, 0.4, 3000.0, 0.03)
    return acabar(y, -12.0, dur, 30.0, 1500.0, corte_grave=40.0)


# ---------------------------------------------------------------------------
# Camadas em loop
#
# Tudo aqui é periódico por construção: as senoides dão um número inteiro de
# ciclos no arquivo, o ruído e os filtros são feitos em círculo (pela FFT), os
# eventos que passam do fim voltam para o começo e a reverberação dá a volta.
# ---------------------------------------------------------------------------

def freq_loop(f, n):
    """A frequência mais próxima de f que cabe um número inteiro de vezes em n amostras."""
    dur = n / SR
    return np.round(np.asarray(f, dtype=float) * dur) / dur


def gerar_fundo_grave(v, rng):
    """Drone escuro, quase inaudível.

    Pares de senoides graves (mi, lá e dó, de 41 a 65 Hz) separadas por frações
    de hertz: batem devagar, cada par no seu tempo, e o parceiro fica acima num
    ouvido e abaixo no outro. Harmônicos fracos para existir em caixa pequena e
    um "ar" de ruído de 120 a 800 Hz que respira.
    """
    dur = 48.0
    n = amostras(dur)
    t = tempo(n)
    # (frequência, afastamento do parceiro em ciclos por loop, amplitude)
    vozes = [(41.25, 3, 1.0), (55.0, 5, 0.9), (65.41, 2, 0.5), (82.5, 4, 0.34),
             (110.0, 7, 0.26), (123.47, 3, 0.12), (164.81, 5, 0.07)]
    ondas = [0.6 + 0.4 * np.tanh(lento(rng, n, 0.06)) for _ in vozes]
    fases = [(rng.uniform(0, DOIS_PI), rng.uniform(0, DOIS_PI)) for _ in vozes]
    lados = []
    for lado in (1.0, -1.0):
        y = np.zeros(n)
        for (f, afasta, amp), onda, (fa, fb) in zip(vozes, ondas, fases):
            f1 = freq_loop(f, n)
            f2 = f1 + lado * afasta / dur
            y += amp * onda * (np.sin(DOIS_PI * f1 * t + fa) + 0.8 * np.sin(DOIS_PI * f2 * t + fb))
        ar = ruido_na_faixa(rng, n, 120.0, 800.0, -4.0) * (0.55 + 0.45 * np.tanh(lento(rng, n, 0.12)))
        lados.append(y + 0.11 * ar)
    return acabar_loop(np.stack(lados, axis=1), -30.0, girar=True, nivelar=0.5, janela=3.0)


def gerar_fundo_vigia(v, rng):
    """A sensação de estar sendo olhado.

    Oito tons finos, de vidro, apertados entre 2 e 4 kHz. Cada um aparece e some
    por conta própria, treme de um jeito irregular e passeia entre os ouvidos.
    Nunca soam todos juntos: o agrupamento muda o tempo todo.
    """
    dur = 32.0
    n = amostras(dur)
    t = tempo(n)
    y = np.zeros((n, 2))
    for f in (2093.0, 2217.5, 2637.0, 2793.8, 3136.0, 3322.4, 3520.0, 3951.1):
        f = freq_loop(f * cents(rng.uniform(-18.0, 18.0)), n)
        presenca = np.clip(np.tanh(0.9 * lento(rng, n, 0.25) + 0.15), 0.0, None) ** 2
        tremor = 1.0 + 0.35 * np.tanh(lento(rng, n, 5.0))
        fase = DOIS_PI * f * t + 4.0 * lento(rng, n, 0.7) + rng.uniform(0, DOIS_PI)
        tom = np.sin(fase) + 0.06 * np.sin(DOIS_PI * freq_loop(2.32 * f, n) * t)
        y += panoramica(tom * presenca * tremor, 0.85 * np.tanh(lento(rng, n, 0.15)))
    y = reverb_estereo(y, rng, 2.2, 0.35, 6000.0, 0.02, circular=True)
    for c in range(2):
        y[:, c] += 0.02 * np.std(y) * ruido_na_faixa(rng, n, 5000.0, 9000.0) * (0.6 + 0.4 * np.tanh(lento(rng, n, 0.2)))
    return acabar_loop(y, -31.0, girar=True, nivelar=0.5, janela=1.0)


# --- perseguição: três camadas de 32 tempos a 100 bpm (19,2 s exatos) -------

BPM_CACA = 100.0
TEMPO_CACA = 60.0 / BPM_CACA
N_CACA = amostras(32 * TEMPO_CACA)
# O arquivo começa 20 ms ANTES do primeiro tempo (882 amostras, igual nas três camadas).
# A emenda cai no respiro que antecede o tempo forte, e não em cima do ataque do tambor
# e do sino: é onde o som está mais baixo e onde o Vorbis menos erra.
ANTES_DO_TEMPO = 0.02


def fechar_caca(y, rms_db, corte_grave=24.0):
    """Roda a camada (montada com o primeiro tempo no zero) e dá o acabamento de loop."""
    return acabar_loop(np.roll(y, amostras(ANTES_DO_TEMPO), axis=0), rms_db, corte_grave)


def _tambor(rng, f0, queda, tau, baqueta):
    """Tambor abafado: pele frouxa (a nota cai logo depois do golpe) com um pano por cima."""
    n = amostras(min(tau * 7.0, 1.6))
    t = tempo(n)
    fase = DOIS_PI * np.cumsum(f0 * (1.0 + queda * np.exp(-t / 0.025))) / SR
    y = np.sin(fase) * np.exp(-t / tau)
    y += 0.6 * np.sin(1.59 * fase + 1.0) * np.exp(-t / (0.6 * tau)) + 0.35 * np.sin(2.14 * fase + 2.0) * np.exp(-t / (0.4 * tau))
    y += baqueta * rms_1(pb(rng.standard_normal(n), 700.0, 2)) * np.exp(-t / 0.012)
    return pb(y * (1.0 - np.exp(-t / 0.002)), 900.0, 2)


def gerar_caca_pulso(v, rng):
    """Perseguição, camada 1: o pulso. Tambor grave e abafado em 3+3+2, com um
    tambor menor respondendo fraco e um golpe fundo a cada dois compassos."""
    n = N_CACA
    seco = np.zeros(n)
    passo = TEMPO_CACA / 4.0   # semicolcheia
    comp_a = {0: ("g", 1.0), 6: ("g", 0.8), 12: ("g", 0.85), 10: ("t", 0.3), 14: ("t", 0.35)}
    comp_b = {0: ("g", 1.0), 6: ("g", 0.8), 12: ("g", 0.85), 14: ("g", 0.6), 3: ("t", 0.25), 10: ("t", 0.3)}
    virada = {0: ("g", 1.0), 6: ("g", 0.8), 8: ("t", 0.3), 10: ("t", 0.4), 12: ("g", 0.8), 13: ("t", 0.45),
              14: ("g", 0.7), 15: ("t", 0.6)}
    for compasso, desenho in enumerate([comp_a, comp_b, comp_a, comp_b, comp_a, comp_b, comp_a, virada]):
        for passo_i, (qual, forca) in desenho.items():
            forca *= rng.uniform(0.9, 1.0)
            if qual == "g":
                som = _tambor(rng, 66.0, 0.7, 0.16, 0.25)
            else:
                som = _tambor(rng, 104.0, 0.5, 0.09, 0.35)
            somar(seco, som, (compasso * 16 + passo_i) * passo, forca)
        if compasso % 2 == 0:
            somar(seco, _tambor(rng, 41.0, 0.5, 0.42, 0.1), compasso * 16 * passo, 0.5)
    # Um instante antes do primeiro tempo a pele é abafada (como a mão do tocador):
    # só a sala atravessa a emenda, que assim fica num ponto de quase silêncio.
    fim = n - amostras(ANTES_DO_TEMPO + 0.005)
    seco[:fim] = fade(seco[:fim], 0.0, 30.0)
    seco[fim:] = 0.0
    seco = np.tanh(1.2 * pico_1(seco))
    y = reverb_estereo(seco, rng, 0.7, 0.16, 1200.0, 0.012, circular=True)
    return fechar_caca(y, -25.0, 26.0)


FORMANTES_CORDAS = [(290.0, 180.0, 1.0), (480.0, 200.0, 0.8), (1050.0, 400.0, 0.5), (2500.0, 700.0, 0.35)]


def gerar_caca_cordas(v, rng):
    """Perseguição, camada 2: cordas em trêmulo, em segundas menores.

    Cada nota são três "instrumentos": dentes de serra desafinados entre si,
    passados pelos formantes da caixa de um instrumento de arco. O trêmulo são
    arcadas curtas na fusa (13,3 por segundo), nunca iguais, com um acento em
    cada tempo para andar junto com o pulso.
    """
    n = N_CACA
    t = tempo(n)
    dur = n / SR
    # (nota, volume em cada um dos 8 compassos)
    partes = [("A3", [0.6, 0.7, 0.8, 0.8, 0.9, 0.9, 1.0, 0.9]), ("Bb3", [0.5, 0.7, 0.8, 0.8, 0.0, 0.0, 1.0, 0.8]),
              ("E4", [0.0, 0.0, 0.7, 0.8, 0.0, 0.0, 0.9, 0.6]), ("F4", [0.0, 0.0, 0.6, 0.8, 0.0, 0.0, 0.9, 0.5]),
              ("G#3", [0.0, 0.0, 0.0, 0.0, 0.8, 0.9, 0.0, 0.0]), ("B3", [0.0, 0.0, 0.0, 0.0, 0.7, 0.8, 0.0, 0.0]),
              ("C4", [0.0, 0.0, 0.0, 0.0, 0.7, 0.8, 0.5, 0.0]), ("B4", [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.7, 0.3]),
              ("C5", [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.6, 0.3])]
    arcadas = 32 * 8
    y = np.zeros((n, 2))
    for nota, volumes in partes:
        nivel = alisar(np.repeat(volumes, n // 8), 220.0, circular=True)
        for desafina, pos in ((-7.0, -0.6), (0.0, 0.1), (6.0, 0.6)):
            f = freq_loop(hz(nota) * cents(desafina + rng.uniform(-2.0, 2.0)), n)
            fase = DOIS_PI * f * t + rng.uniform(0, DOIS_PI)
            serra = np.zeros(n)
            for k in range(1, int(5200.0 / f) + 1):
                serra += resposta_formantes(k * f, FORMANTES_CORDAS) / k / (1.0 + (k * f / 3200.0) ** 4) \
                    * np.sin(k * fase)
            # trêmulo: cada arcada ataca e cede; cada instrumento um tico fora do outro
            pos_arco = (t / dur * arcadas + rng.uniform(-0.12, 0.12)) % arcadas
            qual = np.floor(pos_arco).astype(int)
            dentro = pos_arco - qual
            forca = rng.uniform(0.7, 1.0, arcadas) * np.where(np.arange(arcadas) % 2 == 0, 1.0, 0.85)
            forca[::8] *= 1.3
            arco = forca[qual] * (0.3 + 0.7 * np.exp(-3.0 * dentro)) * (1.0 - np.exp(-dentro / 0.07))
            y += panoramica(serra * alisar(arco, 1.5, circular=True) * nivel, pos)
    chiado = np.stack([ruido_na_faixa(rng, n, 2000.0, 6000.0) for _ in range(2)], axis=1)
    y += 0.03 * np.std(y) * chiado * alisar(np.repeat([0.6, 0.7, 0.8, 0.8, 0.9, 0.9, 1.0, 0.8], n // 8), 220.0, True)[:, None]
    y = reverb_estereo(y, rng, 1.3, 0.25, 3500.0, 0.015, circular=True)
    return fechar_caca(y, -27.0)


def gerar_caca_tema(v, rng):
    """Perseguição, camada 3: pedaços do tema duas oitavas abaixo, em sinos lentos.

    Compassos 1-2 do tema (uma nota a cada dois tempos), silêncio, começo dos
    compassos 5-6, e um sino ao contrário que desemboca no recomeço. Os sinos
    são "tortos" (a altura cede depois do golpe) e passam por uma saturação.
    """
    n = N_CACA
    y = np.zeros((n, 2))
    sequencia = [("A2", 0, 1.0), ("C3", 2, 0.85), ("E3", 4, 0.9), ("D3", 6, 0.9), ("C3", 8, 0.8), ("B2", 10, 0.85),
                 ("D3", 16, 1.0), ("F3", 18, 0.85), ("A3", 20, 0.95), ("G#3", 22, 0.9), ("E3", 24, 1.0)]
    for i, (nota, tempo_b, forca) in enumerate(sequencia):
        sino = badalada(rng, hz(nota), 6.0, 9.0, rachado=0.25, escorrega=28.0)
        somar_circular(y, panoramica(fade(sino, 0.0, 900.0), 0.35 if i % 2 else -0.35), tempo_b * TEMPO_CACA, forca)
    volta = fade(badalada(rng, hz("A2"), 2.4, 3.0, rachado=0.25)[::-1], 300.0, 12.0)
    somar_circular(y, panoramica(volta, 0.0), n / SR - 2.4 - ANTES_DO_TEMPO - 0.005, 0.5)
    y = np.tanh(2.2 * y / np.max(np.abs(y)))
    y = filtrar_loop(y, lambda f: 1.0 / np.sqrt(1.0 + (f / 2600.0) ** 4))
    y = reverb_estereo(y, rng, 2.2, 0.3, 2500.0, 0.02, circular=True)
    return fechar_caca(y, -27.0, 30.0)


def gerar_avesso_ar(v, rng):
    """O ar de um lugar morto, tocado ao contrário.

    A camada é montada para a frente e invertida no fim: notas sopradas que
    nasceriam de repente e morreriam devagar viram ondas que crescem do nada e
    cortam; a reverberação vem antes do som. Longe, metal gemendo sob tensão.
    Raramente (três vezes em 64 s), uma nota de caixinha, também ao contrário.
    """
    dur = 64.0
    n = amostras(dur)
    y = np.zeros((n, 2))
    # almofada: ruído muito estreito em volta de cada nota (um sopro afinado)
    notas = ["A2", "E3", "A3", "B3", "C4", "E4", "Eb4"]
    faixas = {}
    for nota in notas:
        f0 = hz(nota)
        faixas[nota] = [ruido_formado(rng, n, lambda f: np.exp(-0.5 * ((f - f0) / (0.012 * f0)) ** 2)
                                      + 0.25 * np.exp(-0.5 * ((f - 2.0 * f0) / (0.02 * f0)) ** 2))
                        for _ in range(2)]
    t_ev = 0.0
    while t_ev < dur - 2.0:
        nota = notas[int(rng.integers(0, len(notas) - 1))] if rng.random() > 0.12 else "Eb4"
        d = rng.uniform(6.0, 10.0)
        m = amostras(d)
        tm = tempo(m)
        env = (1.0 - np.exp(-tm / 0.12)) * np.exp(-tm / (d / 4.5)) * np.clip((d - tm) / 0.5, 0.0, 1.0)
        i0 = amostras(t_ev)
        idx = (i0 + np.arange(m)) % n
        pos = rng.uniform(-0.7, 0.7)
        forca = rng.uniform(0.55, 0.85)
        ang = (pos + 1.0) * np.pi / 4.0
        y[idx, 0] += forca * np.cos(ang) * faixas[nota][0][idx] * env
        y[idx, 1] += forca * np.sin(ang) * faixas[nota][1][idx] * env
        t_ev += rng.uniform(2.0, 4.0)
    # gemidos de metal: parciais inarmônicos graves que entortam devagar e vibram ásperos
    for t_gem in (7.0, 23.5, 38.0, 55.0):
        d = rng.uniform(2.8, 4.8)
        m = amostras(d)
        tm = tempo(m)
        base = rng.uniform(75.0, 150.0)
        curva = cents(rng.uniform(-90.0, 90.0) * np.sin(np.pi * tm / d) + rng.uniform(-60.0, 60.0) * tm / d)
        gem = np.zeros(m)
        for razao, amp in ((1.0, 1.0), (1.47, 0.7), (2.09, 0.6), (2.56, 0.4), (3.3, 0.3), (4.4, 0.15)):
            gem += amp * np.sin(DOIS_PI * np.cumsum(base * razao * curva) / SR + rng.uniform(0, DOIS_PI))
        aspero = 1.0 + 0.5 * ruido_em_banda(rng, 27.0, 14.0, m)
        gem *= np.sin(np.pi * tm / d) ** 1.5 * aspero
        somar_circular(y, panoramica(pb(gem, 1100.0, 2), rng.uniform(-0.8, 0.8)), t_gem + rng.uniform(-1.5, 1.5), 0.12)
    # notas de caixinha (vão soar ao contrário depois da inversão)
    for t_nota, nota, pos in ((14.0, "E5", -0.4), (41.0, "A4", 0.5), (42.1, "C5", 0.3)):
        som = corpo_caixinha(lamina(rng, hz(nota) * cents(-40.0), 1.0, 0.8))
        somar_circular(y, panoramica(som, pos), t_nota, 0.16)
    y = reverb_estereo(y, rng, 4.0, 0.5, 2800.0, 0.03, circular=True)
    y = acabar_loop(y[::-1].copy(), -28.0, 35.0, nivelar=0.5, janela=1.5)
    for c in range(2):   # o chão de ar, parado (depois do nivelamento, para não respirar junto)
        y[:, c] += 0.22 * np.std(y) * ruido_na_faixa(rng, n, 300.0, 2500.0, -5.0) * (0.6 + 0.4 * np.tanh(lento(rng, n, 0.1)))
    return acabar_loop(y, -28.0, 35.0, girar=True)


# ---------------------------------------------------------------------------
# Catálogo
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class Som:
    nome: str
    variantes: int                    # 0 = camada em loop (arquivo sem número)
    canais: int
    duracao: tuple                    # faixa pedida, em segundos (mín, máx)
    gerar: Callable
    stream: bool = False
    loop: bool = False
    o_que_e: str = ""

    def arquivos(self):
        if self.loop:
            return [(0, self.nome)]
        return [(v, f"{self.nome}_{v}") for v in range(1, self.variantes + 1)]


CATALOGO = [
    Som("assobio", 3, 1, (2.5, 5.0), gerar_assobio,
        o_que_e="alguém assobiando de longe um pedaço do tema (compassos 1-2, 5-6 e 3-4)"),
    Som("cantarolar", 2, 1, (4.0, 6.0), gerar_cantarolar,
        o_que_e="o tema cantarolado de boca fechada (uma e duas oitavas abaixo)"),
    Som("caixa_musica", 1, 1, (20.0, 20.0), gerar_caixa_musica, stream=True,
        o_que_e="o tema inteiro, limpo, a 96 bpm, com ritardando no fim"),
    Som("caixa_gasta", 2, 1, (20.0, 28.0), gerar_caixa_gasta, stream=True,
        o_que_e="o tema gasto: 80 e 66 bpm, desafinado, notas falhando; na 2 o mi vira mi bemol"),
    Som("caixa_quebrada", 1, 1, (2.0, 2.0), gerar_caixa_quebrada,
        o_que_e="três notas do tema e um tranco metálico"),
    Som("batimento", 2, 1, (0.8, 0.8), gerar_batimento,
        o_que_e="um batimento de coração (tum-tum): calmo e disparado"),
    Som("arranhar", 3, 1, (0.8, 1.6), gerar_arranhar,
        o_que_e="unhas arranhando madeira (longo; três curtos) e pedra"),
    Som("pancada", 2, 1, (0.5, 0.5), gerar_pancada,
        o_que_e="batida pesada e surda numa parede (madeira; pedra)"),
    Som("sussurro_voz", 6, 1, (1.2, 2.5), gerar_sussurro_voz,
        o_que_e="frases sussurradas sem sentido, de 3 a 7 sílabas"),
    Som("chamado", 2, 1, (2.0, 3.0), gerar_chamado,
        o_que_e="um chamado distante, entre bicho e gente"),
    Som("sino_longe", 2, 1, (5.0, 6.0), gerar_sino_longe, stream=True,
        o_que_e="um sino tocando longe (grande, uma badalada; rachado, duas)"),
    Som("vento_oco", 3, 1, (4.0, 7.0), gerar_vento_oco, stream=True,
        o_que_e="rajada de vento numa fresta, com ressonância oca"),
    Som("zumbido", 1, 2, (4.0, 4.0), gerar_zumbido,
        o_que_e="zumbido de ouvido, bem baixo"),
    Som("ranger", 3, 1, (0.8, 2.0), gerar_ranger,
        o_que_e="madeira rangendo (tábua, porta, peça sob tensão)"),
    Som("sopro", 1, 1, (0.5, 0.5), gerar_sopro,
        o_que_e="alguém soprando uma chama"),
    Som("apagao", 1, 2, (2.5, 2.5), gerar_apagao,
        o_que_e="tudo some: ruído que cresce invertido, corte seco, grave que desce"),
    Som("despertar", 1, 2, (1.5, 1.5), gerar_despertar,
        o_que_e="acordar de repente: inspiração brusca"),
    Som("tigela", 1, 1, (0.6, 0.6), gerar_tigela,
        o_que_e="cerâmica raspando e batendo de leve"),
    Som("giz", 2, 1, (0.4, 0.7), gerar_giz,
        o_que_e="giz riscando pedra (um traço; dois traços)"),
    Som("sonho", 1, 2, (8.0, 8.0), gerar_sonho, stream=True,
        o_que_e="passagem para um sonho: acorde suave que desafina e afunda"),
    Som("fundo_grave", 0, 2, (48.0, 48.0), gerar_fundo_grave, stream=True, loop=True,
        o_que_e="loop: drone escuro, quase inaudível"),
    Som("fundo_vigia", 0, 2, (32.0, 32.0), gerar_fundo_vigia, stream=True, loop=True,
        o_que_e="loop: tons finos de vidro entre 2 e 4 kHz (estar sendo olhado)"),
    Som("caca_pulso", 0, 2, (19.2, 19.2), gerar_caca_pulso, stream=True, loop=True,
        o_que_e="loop de perseguição, camada 1: pulso grave, 100 bpm, 32 tempos"),
    Som("caca_cordas", 0, 2, (19.2, 19.2), gerar_caca_cordas, stream=True, loop=True,
        o_que_e="loop de perseguição, camada 2: cordas dissonantes em trêmulo"),
    Som("caca_tema", 0, 2, (19.2, 19.2), gerar_caca_tema, stream=True, loop=True,
        o_que_e="loop de perseguição, camada 3: pedaços do tema em sinos graves"),
    Som("avesso_ar", 0, 2, (64.0, 64.0), gerar_avesso_ar, stream=True, loop=True,
        o_que_e="loop: ambiente de um lugar morto, tocado ao contrário"),
]


# ---------------------------------------------------------------------------
# Gravação (OGG Vorbis reprodutível)
# ---------------------------------------------------------------------------

_INVERTE_BITS = bytes(int(f"{i:08b}"[::-1], 2) for i in range(256))


def _crc_ogg(dados: bytes) -> int:
    """CRC das páginas Ogg (polinômio 0x04C11DB7 sem reflexão), usando o crc32 do zlib."""
    bruto = zlib.crc32(dados.translate(_INVERTE_BITS), 0xFFFFFFFF) ^ 0xFFFFFFFF
    return int(f"{bruto:032b}"[::-1], 2)


def _fixar_serial(dados: bytes, serial: int) -> bytes:
    """Troca o número de série do fluxo Ogg (a libsndfile sorteia um a cada gravação)."""
    saida = bytearray()
    pos = 0
    while pos < len(dados):
        if dados[pos:pos + 4] != b"OggS":
            raise ValueError("página Ogg inválida")
        n_seg = dados[pos + 26]
        tamanho = 27 + n_seg + sum(dados[pos + 27:pos + 27 + n_seg])
        pagina = bytearray(dados[pos:pos + tamanho])
        pagina[14:18] = struct.pack("<I", serial)
        pagina[22:26] = b"\0\0\0\0"
        pagina[22:26] = struct.pack("<I", _crc_ogg(bytes(pagina)))
        saida += pagina
        pos += tamanho
    return bytes(saida)


def _codificar(caminho: Path, x: np.ndarray, qualidade: float):
    canais = 1 if x.ndim == 1 else x.shape[1]
    with sf.SoundFile(str(caminho), "w", samplerate=SR, channels=canais, format="OGG",
                      subtype="VORBIS", compression_level=1.0 - qualidade) as arq:
        for i in range(0, len(x), 4096):   # em blocos: a libsndfile não gosta de escritas enormes
            arq.write(x[i:i + 4096])
    dados = caminho.read_bytes()
    caminho.write_bytes(_fixar_serial(dados, zlib.crc32(caminho.stem.encode("utf-8"))))


def medir_emenda(x: np.ndarray) -> dict:
    """A descontinuidade de um loop, medida de três jeitos.

    salto: do último valor para o primeiro. quina: a mudança de inclinação nesse
    ponto (segunda diferença). As versões `_rel` dividem pelo que as amostras
    vizinhas (93 ms de cada lado) já fazem de uma para a outra: três vezes o
    percentil 99, com piso de 0,0005 (-66 dBFS). Até 1, a emenda não se distingue
    do próprio som nem amostra por amostra.

    estalo: o que o ouvido perceberia. Um degrau na onda é um clique, e clique é
    energia aguda concentrada num instante. Mede a energia acima de 1,5 kHz nos
    12 ms em volta da emenda, em dB acima da janela de 12 ms mais forte entre
    as vizinhas (186 ms para cada lado). Se der zero ou menos, o som em volta
    tem transientes maiores que a emenda.
    """
    x = np.asarray(x, dtype=float).reshape(len(x), -1)
    lados = (x[-4096:], x[:4096])
    d1 = np.concatenate([np.abs(np.diff(lado, axis=0)) for lado in lados])
    d2 = np.concatenate([np.abs(np.diff(lado, 2, axis=0)) for lado in lados])
    salto = np.abs(x[0] - x[-1])
    quina = np.maximum(np.abs(x[0] - 2 * x[-1] + x[-2]), np.abs(x[1] - 2 * x[0] + x[-1]))
    limite1 = np.maximum(3.0 * np.percentile(d1, 99, axis=0), PISO_EMENDA)
    limite2 = np.maximum(3.0 * np.percentile(d2, 99, axis=0), PISO_EMENDA)
    meio, janela = 8192, 512
    junto = np.concatenate([x[-meio:], x[:meio]])     # o fim colado no começo, como o jogo toca
    agudos = signal.sosfiltfilt(signal.butter(4, 1500.0, "high", fs=SR, output="sos"), junto, axis=0) ** 2
    soma = np.cumsum(np.concatenate([np.zeros((1, x.shape[1])), agudos]), axis=0)
    inicios = np.arange(janela, 2 * meio - 2 * janela + 1, janela // 2)   # janelas de 12 ms, de 6 em 6 ms
    energia = soma[inicios + janela] - soma[inicios]
    centro = meio - janela // 2                                           # a janela centrada na emenda
    na_emenda = energia[inicios == centro][0]
    vizinhas = energia[np.abs(inicios - centro) >= janela].max(axis=0)
    estalo = 10.0 * np.log10((na_emenda + 1e-18) / (vizinhas + 1e-18))
    return {"salto": float(np.max(salto)), "quina": float(np.max(quina)),
            "salto_rel": float(np.max(salto / limite1)), "quina_rel": float(np.max(quina / limite2)),
            "estalo": float(np.max(estalo))}


def gravar_ogg(caminho: Path, x: np.ndarray, loop=False):
    """Grava o som. Nos loops, confere a emenda no arquivo já comprimido.

    O Vorbis erra um pouco em cada bloco. Dentro do arquivo os blocos se
    sobrepõem e o erro não se ouve, mas na emenda o erro do último bloco
    encontra o do primeiro sem transição e pode sobrar um degrau minúsculo. O
    tamanho dele muda de forma imprevisível com a qualidade, então o loop é
    gravado em algumas qualidades e fica a primeira que emenda bem (ou a melhor).
    """
    x = np.asarray(x, dtype=np.float64)
    if not np.all(np.isfinite(x)):
        raise ValueError(f"{caminho.name}: o som tem NaN ou infinito")
    if not loop:
        _codificar(caminho, x, QUALIDADE_VORBIS)
        return
    bruto = medir_emenda(x)
    if max(bruto["salto_rel"], bruto["quina_rel"]) > 1.0:
        raise ValueError(f"{caminho.name}: o loop não é periódico antes de comprimir ({bruto})")
    melhor = None
    for qualidade in QUALIDADES_LOOP:
        _codificar(caminho, x, qualidade)
        lido, _ = sf.read(str(caminho), always_2d=True, dtype="float64")
        emenda = medir_emenda(lido)
        nota = max(emenda["salto_rel"], emenda["quina_rel"])
        if melhor is None or nota < melhor[0]:
            melhor = (nota, caminho.read_bytes())
        if nota <= 0.7:
            break
    caminho.write_bytes(melhor[1])


# ---------------------------------------------------------------------------
# sounds.json
# ---------------------------------------------------------------------------

def entrada_json(som: Som) -> dict:
    lista = []
    for _, arquivo in som.arquivos():
        nome = f"sussurros:{arquivo}"
        lista.append({"name": nome, "stream": True} if som.stream else nome)
    return {"subtitle": f"subtitles.sussurros.{som.nome}", "sounds": lista}


def atualizar_sounds_json():
    """Mantém as entradas que já existem e acrescenta (ou refaz) as do catálogo."""
    dados = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))
    for som in CATALOGO:
        dados[som.nome] = entrada_json(som)
    SOUNDS_JSON.write_text(json.dumps(dados, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def conferir_sounds_json() -> list:
    """Devolve a lista de problemas: JSON inválido, arquivo citado que não existe, entrada errada."""
    problemas = []
    try:
        dados = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))
    except (OSError, ValueError) as erro:
        return [f"sounds.json inválido: {erro}"]
    for evento, entrada in dados.items():
        for item in entrada.get("sounds", []):
            nome = item["name"] if isinstance(item, dict) else item
            arquivo = PASTA_SONS / (nome.split(":", 1)[1] + ".ogg")
            if not arquivo.is_file():
                problemas.append(f"sounds.json: {evento} cita {nome}, mas {arquivo.name} não existe")
    for som in CATALOGO:
        if som.nome not in dados:
            problemas.append(f"sounds.json: falta a entrada {som.nome}")
        elif dados[som.nome] != entrada_json(som):
            problemas.append(f"sounds.json: a entrada {som.nome} está diferente do catálogo")
    return problemas


# ---------------------------------------------------------------------------
# Conferência
# ---------------------------------------------------------------------------

PICO_MAXIMO_DB = -3.0
RMS_MAXIMO_LOOP_DB = -23.0
DC_MAXIMO = 0.001            # -60 dBFS
PONTA_MAXIMA = 0.003         # -50 dBFS na primeira e na última amostra de um efeito avulso
TOLERANCIA_DURACAO = 0.15


def medir(caminho: Path, som: Som) -> tuple:
    """Decodifica o arquivo e mede. Devolve (dicionário de medidas, lista de problemas)."""
    problemas = []
    if not caminho.is_file() or caminho.stat().st_size == 0:
        return {}, ["arquivo vazio ou inexistente"]
    x, sr = sf.read(str(caminho), always_2d=True, dtype="float64")
    n, canais = x.shape
    if n == 0:
        return {}, ["arquivo sem amostras"]
    if not np.all(np.isfinite(x)):
        return {}, ["tem NaN ou infinito"]
    m = {"dur": n / sr, "canais": canais, "kb": caminho.stat().st_size / 1024.0}
    m["pico"] = em_db(np.max(np.abs(x)))
    m["rms"] = em_db(np.sqrt(np.mean(x ** 2)))
    m["dc"] = float(np.max(np.abs(x.mean(axis=0))))
    f, p = signal.welch(x.mean(axis=1), sr, nperseg=min(n, 4096))
    m["centroide"] = float(np.sum(f * p) / max(np.sum(p), 1e-30))

    if sr != SR:
        problemas.append(f"taxa {sr} Hz")
    if canais != som.canais:
        problemas.append(f"{canais} canal(is), pedido {som.canais}")
    if m["pico"] > PICO_MAXIMO_DB:
        problemas.append(f"pico {m['pico']:.1f} dBFS acima de {PICO_MAXIMO_DB:.0f}")
    if m["rms"] < -70.0:
        problemas.append("arquivo praticamente mudo")
    if m["dc"] > DC_MAXIMO:
        problemas.append(f"contínua de {m['dc']:.4f}")
    minimo, maximo = som.duracao
    if som.loop:
        if n != amostras(minimo):
            problemas.append(f"loop com {n} amostras, pedido {amostras(minimo)}")
        if m["rms"] > RMS_MAXIMO_LOOP_DB:
            problemas.append(f"camada de fundo com RMS {m['rms']:.1f} dBFS")
        m.update(medir_emenda(x))
        if m["estalo"] > ESTALO_MAXIMO_DB:
            problemas.append(f"estalo na emenda ({m['estalo']:+.1f} dB sobre os vizinhos)")
        if m["salto_rel"] > 1.0:
            problemas.append(f"salto na emenda ({m['salto']:.5f}, {m['salto_rel']:.1f}x o limite)")
        if m["quina_rel"] > 1.0:
            problemas.append(f"quina na emenda ({m['quina']:.5f}, {m['quina_rel']:.1f}x o limite)")
    else:
        if not (minimo * (1 - TOLERANCIA_DURACAO) <= m["dur"] <= maximo * (1 + TOLERANCIA_DURACAO)):
            problemas.append(f"duração {m['dur']:.2f} s fora de {minimo:g}-{maximo:g} s")
        ponta = float(max(np.max(np.abs(x[0])), np.max(np.abs(x[-1]))))
        if ponta > PONTA_MAXIMA:
            problemas.append(f"clique na ponta ({ponta:.4f})")
    if not som.stream and m["dur"] > 5.25:
        problemas.append("som de mais de 5 s sem stream")
    return m, problemas


def conferir(sons, completo=True) -> bool:
    """Imprime a tabela e devolve True se nada falhou."""
    print(f"{'arquivo':18s} {'dur(s)':>7s} {'can':>3s} {'pico':>6s} {'rms':>6s} {'dc':>8s} "
          f"{'centr.':>7s} {'KB':>6s}  {'emenda: salto / quina / estalo':46s} situação")
    falhas, total_kb, quantos = [], 0.0, 0
    for som in sons:
        for _, arquivo in som.arquivos():
            quantos += 1
            m, problemas = medir(PASTA_SONS / f"{arquivo}.ogg", som)
            if not m:
                print(f"{arquivo:18s} {'-':>7s}  FALHOU: {'; '.join(problemas)}")
                falhas.append(arquivo)
                continue
            total_kb += m["kb"]
            emenda = (f"{m['salto']:.5f} ({m['salto_rel']:.2f}x) / {m['quina']:.5f} ({m['quina_rel']:.2f}x) / "
                      f"{m['estalo']:+.1f} dB") if som.loop else ""
            situacao = "ok" if not problemas else "FALHOU: " + "; ".join(problemas)
            print(f"{arquivo:18s} {m['dur']:7.2f} {m['canais']:3d} {m['pico']:6.1f} {m['rms']:6.1f} "
                  f"{m['dc']:8.5f} {m['centroide']:7.0f} {m['kb']:6.1f}  {emenda:46s} {situacao}")
            if problemas:
                falhas.append(arquivo)
    print(f"total: {total_kb / 1024.0:.2f} MB em {quantos} arquivos")
    # As três camadas da perseguição tocam juntas: têm de ter o mesmo tamanho, amostra por amostra.
    caca = [PASTA_SONS / f"{s.nome}.ogg" for s in sons if s.nome.startswith("caca_")]
    tamanhos = {c.name: sf.info(str(c)).frames for c in caca if c.is_file()}
    if len(set(tamanhos.values())) > 1:
        print(f"FALHOU: camadas da perseguição com tamanhos diferentes: {tamanhos}")
        falhas.append("caca_*")
    problemas_json = conferir_sounds_json() if completo else []
    for p in problemas_json:
        print("FALHOU:", p)
    if completo and not problemas_json:
        print("sounds.json: válido, todos os arquivos citados existem")
    if falhas or problemas_json:
        print(f"\n{len(falhas) + len(problemas_json)} problema(s).")
        return False
    print("\nTudo certo.")
    return True


# ---------------------------------------------------------------------------
# Linha de comando
# ---------------------------------------------------------------------------

def tabela_markdown():
    """Imprime a tabela de sons do LEIA-ME a partir do catálogo e dos arquivos gravados."""
    print("| som | arquivos | canais | duração (s) | o que é |")
    print("|---|---|---|---|---|")
    for som in CATALOGO:
        durs = [sf.info(str(PASTA_SONS / f"{arquivo}.ogg")).duration for _, arquivo in som.arquivos()]
        if som.loop:
            arquivos = f"`{som.nome}.ogg`"
        elif som.variantes == 1:
            arquivos = f"`{som.nome}_1.ogg`"
        else:
            arquivos = f"`{som.nome}_1.ogg` a `_{som.variantes}.ogg`"
        canais = ("mono" if som.canais == 1 else "estéreo") + (", stream" if som.stream else "")
        duracao = " / ".join(f"{d:.1f}".replace(".", ",") for d in durs)
        print(f"| `{som.nome}` | {arquivos} | {canais} | {duracao} | {som.o_que_e} |")


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Gera e confere os sons do Sussurros.")
    ap.add_argument("--conferir", action="store_true", help="só mede os arquivos que já existem")
    ap.add_argument("--so", nargs="+", metavar="NOME", help="gera só estes sons (nomes do catálogo)")
    ap.add_argument("--tabela", action="store_true", help="imprime a tabela de sons em Markdown (para o LEIA-ME)")
    args = ap.parse_args(argv)
    for saida in (sys.stdout, sys.stderr):
        if hasattr(saida, "reconfigure"):
            saida.reconfigure(encoding="utf-8")
    if args.tabela:
        tabela_markdown()
        return 0

    sons = CATALOGO
    if args.so:
        nomes = {s.nome for s in CATALOGO}
        desconhecidos = [n for n in args.so if n not in nomes]
        if desconhecidos:
            ap.error("som desconhecido: " + ", ".join(desconhecidos))
        sons = [s for s in CATALOGO if s.nome in args.so]

    if not args.conferir:
        PASTA_SONS.mkdir(parents=True, exist_ok=True)
        for som in sons:
            for v, arquivo in som.arquivos():
                x = som.gerar(v, rng_de(som.nome, v))
                gravar_ogg(PASTA_SONS / f"{arquivo}.ogg", x, loop=som.loop)
                print(f"gerado {arquivo}.ogg", flush=True)
        if not args.so:
            atualizar_sounds_json()
        print()
    return 0 if conferir(sons, completo=not args.so) else 1


if __name__ == "__main__":
    sys.exit(main())
