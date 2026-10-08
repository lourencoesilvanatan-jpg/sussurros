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

## 2. Experimentos

As respostas dadas em 08/10 (tocha falsa e sósia em pé) estão em "Já conferido". O que ficou sem resposta está na parte 7.

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

## 7. Miragens e o som sem direção (PR #16)

**Som.** A resposta de 08/10 ("soou na esquerda") não disse qual dos sons. O comando novo toca os três em sequência, numerados no chat:

- `/sussurros teste sussurro tudo`, de fone, parado e sem girar a câmera por 15 s.
- **Pergunta:** o som 2 veio do meio, da esquerda ou da direita? E o som 3, do meio ou da direita?

**Miragens.**

- `/sussurros cena luzfim`, de noite ou numa caverna: aparece uma tocha a 14–34 blocos. Andar até ela: a 6 blocos ela não está mais lá.
- `/sussurros teste miragem vermelha`, perto de uma tocha sua: ela vira tocha de redstone por 20 s.
- `/sussurros teste miragem apagar`: ficou sem resposta em 08/10. A luz da tocha some junto ou fica?
- `/sussurros teste sosia Jogador deitado` e `agachado`: em 08/10 os dois falharam por falta de chão; agora o comando acha chão sozinho.

**Perguntas:** a luz no fim dá vontade de ir ver? Sumir a 6 blocos é cedo, tarde ou bom? A tocha vermelha chama atenção?

No log: `LUZ_ERRADA tipo=FANTASMA miragem=sim` e depois `MIRAGEM fim ... por=CHEGOU_PERTO`.

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
| Tocha que só o jogador vê (PR #10) | Ilumina, e some ao clicar nela (resposta do dono) |
| Sósia em pé (PR #10) | Apareceu, sem rótulo visível (resposta do dono) |
