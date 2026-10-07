# Sussurros — Refatoração 0.8.1 — Passo 2: Seletor

Segundo passo da divisão estrutural.

## Objetivo

Extrair do Diretor a lógica pura que decide pesos, repetição, exploração, intensidade, teto de categoria, portões e sorteio de candidatos.

## Extraído para Seletor

- `aplicarTeto(...)`
- `taxaExploracao(...)`
- `antiRepeticao(...)`
- `aversaoSequencia(...)`
- `pesoIntensidade(...)`
- `sortearIndice(...)`
- `portao(...)`

O `Diretor` continua responsável pelo contexto do jogo, pelas memórias e pelo controle do fluxo. Ele apenas passa dados simples para o `Seletor`.

## Regra de preservação

Não foram alterados deliberadamente números, fórmulas, linhas de log ou a ordem das chamadas aleatórias. Durante a extração, um teste revelou que a implementação existente de `aplicarTeto` faz duas voltas e, no caso 900/900/100, termina com 55% para SOM; isso foi mantido como caracterização para evitar uma mudança de gameplay. O `rnd.nextDouble()` continua no `Diretor`, no mesmo ponto do fluxo e uma única vez para o sorteio normal.

`longo`, `longoCat`, `curto`, `curtoCat`, `aprender`, `decairCurto` e `sortearContinuacao` continuam no `Diretor`.

## Testes

Foi adicionado JUnit 5 para testar as invariantes do Seletor sem subir o Minecraft.

A versão usada é JUnit Jupiter 5.14.3.
