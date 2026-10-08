# Sussurros — Dívidas de design

Este arquivo registra comportamentos ou decisões de arquitetura que foram deliberadamente deixados fora da refatoração atual. Uma dívida só deve ser resolvida em uma mudança consciente, com testes e revisão do impacto no comportamento do Diretor.

## Teto de categorias

- Com apenas duas categorias, o teto de 45% é matematicamente inviável: as duas categorias precisam somar 100%.
- O algoritmo atual faz duas voltas e, no caso 900/900/100, termina em aproximadamente 55% SOM e 45% MENTE.
- O resultado também é sensível à precisão de ponto flutuante e à ordem/forma das operações. Não alterar durante a refatoração.

## Telemetria da estratégia

- A nota/variável `ESTRATEGIA` atualmente não chega ao log.
- Decidir posteriormente se ela deve ser registrada, removida ou usada em outro ponto.

## Remoção de tochas

- `roubarTocha` e `apagarTochaProxima` permanecem como comportamento existente.
- Decidir posteriormente se as duas responsabilidades devem ser unificadas ou se a distinção atual é necessária.

## `forcando` e `pedido.ehTeste()`

- Existe uma possível sobreposição conceitual entre `forcando` e `pedido.ehTeste()`.
- Não unificar agora: primeiro mapear todos os usos e confirmar se representam realmente o mesmo estado.

