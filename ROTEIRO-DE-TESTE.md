# Sussurros — roteiro de teste no jogo (SPOILERS)

O que entrou na `main` e ainda precisa de alguém jogando. Cada parte diz o que fazer e o que conferir. Quem programa acrescenta uma seção a cada mudança; o que já foi confirmado vai para o fim, em "Já conferido".

**Antes de começar**

1. No GitHub Desktop, trocar para a branch `main` e clicar em "Pull origin".
2. Abrir o jogo de teste (`runClient`) num mundo com cheats.
3. Rodar `/sussurros debug on`. O log fica em `run/sussurros-debug.log`.

**O que mandar de volta:** dizer que jogou (os logs são lidos direto da pasta) e as respostas das perguntas.

**Para testar uma cena atrás da outra:** `/sussurros cena parar` interrompe a cena em andamento, inclusive o silêncio do fim.

---

## 1. Sósia: as poses

`/sussurros teste sosia Jogador agachado` e `/sussurros teste sosia Jogador deitado`.

**Pergunta:** as duas poses apareceram? O boneco deitado fica no chão ou flutuando?

## 2. Cena da janela (PR #5)

Dentro de uma casa com janela de vidro: `/sussurros cena janela`. No log, a cena vai de `INICIO` a `FIM`. Ela ainda não rodou em nenhuma sessão.

## 3. Olho (PR #9)

Com `/sussurros fase 3`, num lugar sem vestígios, usar o Olho cinco vezes seguidas:

- no máximo uma aparição;
- no log, `OLHO chamou aparicao ...` uma vez e depois `OLHO nada ... recarga=sim`.

## 4. Sons em caverna (PR #14)

Numa caverna, depois de ter andado em outro andar (acima ou abaixo): `/sussurros evento eco`, `/sussurros evento sinal` e `/sussurros cena tunel`. Os sons devem chegar mesmo quando a fonte está em outro nível.

## 5. Tocha vermelha (PR #16)

`/sussurros teste miragem vermelha`, perto de uma tocha sua.

**Pergunta:** a tocha vermelha chama atenção? A luz em volta fica visivelmente mais fraca?

## 6. Perguntas em aberto sobre o que você já ouviu e viu

- O corte da música quando ele aparece dá medo ou parece defeito?
- A respiração junto do texto do sussurro ajuda ou atrapalha?

---

## Problema conhecido

- **Luz no fim dentro de caverna.** `/sussurros cena luzfim` funcionou ao ar livre, mas o dono não conseguiu fazê-la acontecer numa caverna. Está na lista de próximos passos do `PLANO-MECANICAS.md`.

---

## Já conferido

Pelos logs das sessões de 08/10/2026 e pelas respostas do dono:

| O quê | Como ficou |
|---|---|
| O jogo abre e roda | Sem erro do mod no `latest.log`, nas duas sessões |
| Cenas do túnel, do marco e da casa (PRs #2 e #5) | Rodaram de `INICIO` a `FIM`, sem `Erro no Diretor` |
| Cena do campo | Rodou sozinha, sem comando, até o silêncio do fim |
| Sons distantes (PR #6) | O dono ouviu e gostou dos sons |
| Evento que não cabe (PR #7) | O `VULTO` não achou lugar e o Diretor sorteou `PASSOS` no mesmo segundo |
| Leitura de reação (PR #8) | Nenhum `fugiu` com `investigou`; `SALTO` apareceu nos dois `/tp` |
| Silêncio de verdade (PR #11) | `SILENCIO_REAL` saiu com aparição, com `SINAL` e com presságio |
| Vulto distante (PR #12) | Silhuetas a 60–80 blocos, vistas |
| Sumiço rápido (PR #13) | Aprovado pelo dono: "às vezes é difícil de ver, mas tem como perceber algo piscando, e é justamente isso que eu queria". No log, de 0 a 1 s entre `PERCEBEU` e `sumiu` |
| Tocha que só o jogador vê (PR #10) | Ilumina, e some ao clicar nela |
| Tocha escondida por miragem | A luz some junto |
| Som sem direção | O som preso ao jogador e o arquivo estéreo soam no meio |
| Sósia em pé (PR #10) | Apareceu, sem rótulo visível |
| Luz no fim (PR #16) | Funcionou ao ar livre; as miragens se desfizeram ao chegar perto e pelo tempo. "Dá vontade de ir ver" |
