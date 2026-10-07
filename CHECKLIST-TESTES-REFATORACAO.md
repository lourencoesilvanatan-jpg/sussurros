# Sussurros — Checklist de testes da refatoração

Este arquivo cresce junto com a refatoração. A regra é validar primeiro o que é puro no CI e depois confirmar no Minecraft o comportamento que depende do runtime.

## Antes de fechar cada passo

- `./gradlew build` precisa passar.
- Os testes automatizados precisam passar.
- Não podem aparecer novos erros no `latest.log`.
- Quando o passo toca o Diretor, fazer um playtest curto (10–15 min) e comparar invariantes, não a sequência exata do RNG.
- Conferir as linhas de telemetria relevantes antes do merge.

## Passo 1 — Agenda e PedidoManifestacao

### Agenda

- `/sussurros debug on`
- `/sussurros evento tocha`: a tarefa agenda a extinção/restauração sem erro.
- `/sussurros evento passos`: os passos agendados realmente acontecem.
- Não aparece `Erro numa tarefa agendada`.

### Origem das manifestações

- `/sussurros evento presenca` registra `origem=COMANDO`.
- Uma manifestação natural registra `origem=DIRETOR`.
- `motivoPosicao` continua identificando a estratégia usada quando aplicável, em vez de cair para `NORMAL` sem motivo.

### Cenas

Testar os comandos de cena existentes:

- túnel
- campo
- casa
- marco
- janela

Confirmar criação do Hóspede e `motivoPosicao` coerente com a cena.

### Logs

- Não aparece `Erro no Diretor`.
- Não aparece stack trace inesperada nas rotinas refatoradas.

## Passo 2 — Seletor

Os testes JUnit cobrem diretamente:

- comportamento atual do teto de categoria (neste caso 900/900/100, a implementação de duas voltas termina com SOM em 55%; corrigir isso fica para uma mudança explícita de gameplay);
- piso de `pesoIntensidade`;
- anti-repetição do evento mais recente;
- aversão a pares repetidos;
- sorteio ponderado;
- portões de intensidade;
- taxa de exploração com baixa confiança;
- taxa de exploração durante escalada;
- taxa de exploração fora da escalada.

No Minecraft, depois do merge, conferir que as linhas `escolha[...]` e `SELECAO ...` continuam aparecendo com o mesmo formato e que a seleção continua acontecendo no mesmo fluxo.

## Regressão geral futura

À medida que novos passos extraírem cenas, acontecimentos, manifestação e entidade, adicionar neste arquivo os comandos e invariantes específicos de cada módulo antes do respectivo merge.
