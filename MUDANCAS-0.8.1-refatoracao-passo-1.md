# Sussurros — Refatoração 0.8.1 — Passo 1

Primeiro passo da divisão estrutural.

- A fila de tarefas foi extraída para Agenda.
- Os três campos temporários de spawn foram agrupados em PedidoManifestacao.
- O Diretor e as cenas usam agora um pedido tipado durante a criação do Hóspede.
- O comportamento de teste continua usando forçando; a passagem por parâmetros explícitos será concluída no próximo subpasso.
- Não foram alterados deliberadamente números, chances, durações ou condições de gameplay.

A próxima etapa remove pedidoSpawn de EstadoJogador e passa PedidoManifestacao diretamente pelos módulos de manifestação.
