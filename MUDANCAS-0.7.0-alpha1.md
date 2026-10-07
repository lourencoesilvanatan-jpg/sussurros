# Sussurros 0.7.0-alpha1 — Aparições e Stalking 2.0

Objetivo: transformar o posicionamento de manifestações em uma capacidade reutilizável, inspirada em sistemas de stalking que procuram posições plausíveis em vez de usar coordenadas fixas. A implementação é original e não copia código de outros projetos.

## 1. Sistema `Aparicao`

Nova classe `com.sussurros.assombracao.Aparicao`. Ela produz candidatos ao redor do jogador e pontua cada um por:

- distância e posição relativa ao olhar;
- não estar dentro do cone seguro da tela;
- visibilidade depois que o jogador gira;
- cobertura parcial;
- penumbra;
- pequena variação aleatória;
- reutilização recente do chunk.

Quando um candidato perfeito não existe, os chamadores podem ajustar a configuração da intenção em vez de duplicar o algoritmo.

## 2. Integração

As aparições genéricas do Diretor agora passam pelo mesmo seletor, assim como a cena espacial em área aberta. O objetivo é que futuras janelas, árvores, cantos, entradas e outras âncoras reutilizem a mesma lógica sem criar um evento independente para cada posição.

## 3. Memória curta de lugares

`EstadoJogador` guarda os últimos chunks usados por manifestações. Reutilização não é proibida: apenas perde prioridade temporariamente para preservar surpresa e evitar que o mesmo pedaço do mundo pareça uma plataforma de spawn.

## 4. Não foi adicionado

Esta alpha não adiciona uma nova biblioteca de IA, não altera a narrativa, não cria uma nova entidade e não muda os limites globais do Diretor. É uma evolução de posicionamento.

## 5. Próximo passo recomendado

Usar o sistema em uma rodada de teste real antes de criar mais classes de aparição. O dado mais importante será se os pontos encontrados parecem naturais e memoráveis, e não apenas se o algoritmo encontra candidatos.
