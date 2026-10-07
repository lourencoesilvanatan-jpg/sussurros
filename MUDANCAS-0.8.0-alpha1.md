# Sussurros 0.8.0-alpha1 — O Hóspede procura

Objetivo: separar o comportamento de caça da decisão do Diretor. O Diretor continua escolhendo quando uma ameaça é permitida; o Hóspede agora possui uma memória transitória de localização e uma busca própria.

## 1. `HospedeBusca`

Nova classe dedicada a:
- última posição conhecida;
- confiança da última informação;
- ouvir movimento em janelas discretas, sem GPS;
- investigação da área;
- seleção de pontos de busca;
- desistência após perder o rastro.

A implementação usa a `Navigation`/pathfinding de `PathfinderMob` do próprio Minecraft, sem biblioteca externa. Isso mantém a dependência do mod pequena e segue a API de entidades/pathfinding documentada para 26.2.

## 2. Mudança de `HospedeEntity`

O modo `CACAR` não navega mais diretamente para a posição atual do jogador a cada poucos ticks. Quando o jogador não está na tela, a entidade usa apenas a memória da busca ou um novo ruído de movimento plausível.

Quando o jogador é visto, a posição atual é confirmada e a busca é atualizada. Quando ele sai da visão, o Hóspede volta a investigar.

## 3. Estrutura

O Diretor continua sendo responsável pelo QUANDO. `Aparicao` continua responsável pelo ONDE. `HospedeBusca` passa a ser responsável pelo COMO durante a caça.

Fluxo:
`Diretor -> Aparicao -> HospedeEntity -> HospedeBusca`

## 4. Não adicionado

Nenhuma biblioteca externa foi incluída nesta alpha. GeckoLib continua reservado para a próxima camada de animação quando houver valor visual real; SmartBrainLib não é necessária enquanto a máquina de estados própria continuar simples.

## 5. Próxima validação

O teste desta alpha deve observar se a criatura parece realmente procurar, se consegue perder o jogador e se a perseguição deixa de parecer uma linha reta. A telemetria de `BUSCA` deve ser usada para diagnosticar travamentos ou busca excessivamente longa, sem abrir o documento de spoilers.
