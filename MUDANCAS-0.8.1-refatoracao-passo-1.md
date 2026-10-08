# Sussurros — Refatoração 0.8.1 — Passo 1

Primeiro passo da divisão estrutural.

- A fila de tarefas foi extraída para Agenda.
- Os três campos temporários de spawn foram agrupados em PedidoManifestacao.
- O Diretor e as cenas usam agora um pedido tipado durante a criação do Hóspede.
- O comportamento de teste continua usando `forcando`, sem substituir essa semântica durante a refatoração.
- Não foram alterados deliberadamente números, chances, durações ou condições de gameplay.


## Correções da revisão

Foram corrigidos o import de `Iterator`, o delegate de compatibilidade de `agendar(...)`, a preservação da semântica de `forcando`, os cinco `finally` que apenas reatribuíam uma variável local e o comentário TODO da nota de estratégia pré-existente.
