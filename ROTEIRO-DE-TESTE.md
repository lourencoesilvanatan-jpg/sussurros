# Sussurros — roteiro de teste no jogo (SPOILERS)

O que entrou na `main` e ainda precisa de alguém jogando. Cada parte diz o que fazer e o que conferir. Quem programa acrescenta uma seção a cada mudança; o que um log já confirmou vai para o fim, em "Já conferido".

**Antes de começar**

1. No GitHub Desktop, trocar para a branch `main` e clicar em "Pull origin".
2. Abrir o jogo de teste (`runClient`) num mundo com cheats.
3. Rodar `/sussurros debug on`. O log fica em `run/sussurros-debug.log`.

**O que mandar de volta:** dizer que jogou (os logs são lidos direto da pasta) e as respostas das perguntas marcadas com "Pergunta".

**Para testar uma cena atrás da outra:** `/sussurros cena parar` interrompe a cena em andamento, inclusive o silêncio do fim.

---

## 1. Ainda dá para focar nele? (PR #13)

De dia, ao ar livre, num lugar com vista:

- `/sussurros evento vulto` e girar a câmera devagar até achar a silhueta. Repetir umas cinco vezes.
- `/sussurros evento presenca` umas cinco vezes, de dia e de noite.

**Pergunta:** ainda dá para focar nele? Ou ficou rápido demais, a ponto de nem perceber que tinha algo?

No log, entre `PERCEBEU` e `sumiu` deve passar no máximo um segundo. Os motivos novos são `VULTO_MIRADO`, `VULTO_VISTO`, `VULTO_DESVIOU` e `SUMIU_NO_DESVIO`.

## 2. Experimentos: as respostas que faltam

Os comandos rodaram em 08/10, mas as respostas não ficam no log. Usar fone de ouvido para os de som.

| Comando | Pergunta |
|---|---|
| `/sussurros teste sussurro lado` | Veio da esquerda? (É a referência.) |
| `/sussurros teste sussurro cabeca` | Soou no meio, sem lado? Continuou no meio ao girar a câmera? |
| `/sussurros teste sussurro folego` | Qual respiração assusta mais: a do mod ou esta, do próprio jogo? |
| `/sussurros teste sussurro estereo` | O som de água soou no meio ou veio da esquerda? |
| `/sussurros teste miragem` (no escuro) | A tocha falsa ilumina o chão em volta? O que acontece ao clicar nela? |
| `/sussurros teste miragem apagar` | A luz da tocha some junto ou fica? O que acontece ao clicar no lugar? |
| `/sussurros teste sosia Jogador` | Apareceu um boneco? Tem algum rótulo embaixo do nome? Ficou virado para você? |
| `/sussurros teste sosia Jogador agachado` | A pose funcionou? |
| `/sussurros teste sosia Jogador deitado` | A pose funcionou? |

Os comandos de miragem e de sósia agora acham chão sozinhos, perto de você; em 08/10 eles falharam várias vezes com "não achei chão livre".

## 3. Cena da janela (PR #5)

Dentro de uma casa com janela de vidro: `/sussurros cena janela`. No log, a cena vai de `INICIO` a `FIM`. Em 08/10 ela não chegou a rodar, porque outra cena estava em andamento.

## 4. Olho (PR #9)

Com `/sussurros fase 3`, num lugar sem vestígios, usar o Olho cinco vezes seguidas:

- no máximo uma aparição;
- no log, `OLHO chamou aparicao ...` uma vez e depois `OLHO nada ... recarga=sim`.

## 5. Sons em caverna (PR #14)

Numa caverna, depois de ter andado em outro andar (acima ou abaixo): `/sussurros evento eco`, `/sussurros evento sinal` e `/sussurros cena tunel`. Os sons devem chegar mesmo quando a fonte está em outro nível.

## 6. Perguntas em aberto sobre o que você já ouviu e viu

- O corte da música quando ele aparece dá medo ou parece defeito?
- A respiração junto do texto do sussurro ajuda ou atrapalha?
- A silhueta do vulto dá para ver bem a 50–80 blocos?

---

## Já conferido

Pelo log e pelo histórico de comandos da sessão de 08/10/2026:

| O quê | Como ficou |
|---|---|
| Cenas do túnel, do marco e da casa (PRs #2 e #5) | Rodaram de `INICIO` a `FIM`, sem `Erro no Diretor` |
| Cena do campo | Rodou sozinha, sem comando, até o silêncio do fim |
| Sons distantes (PR #6) | Os eventos rodaram; o dono ouviu e gostou dos sons |
| Evento que não cabe (PR #7) | Em 4086 s o `VULTO` não achou lugar e o Diretor sorteou `PASSOS` no mesmo segundo |
| Leitura de reação (PR #8) | Nenhum `fugiu` com `investigou`; `SALTO` apareceu nos dois `/tp` |
| Silêncio de verdade (PR #11) | `SILENCIO_REAL` saiu com aparição, com `SINAL` e com presságio |
| Vulto distante (PR #12) | Seis silhuetas a 60–80 blocos, cinco delas vistas |
| O jogo abre e roda | Sem erro do mod no `latest.log` |
