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

- teto de categoria: caso viável com 3 categorias + caracterização do caso 900/900/100, que termina em 55/45 por comportamento herdado;
- piso de `pesoIntensidade`;
- anti-repetição do evento mais recente;
- aversão a pares repetidos;
- sorteio ponderado;
- portões de intensidade;
- taxa de exploração com baixa confiança;
- taxa de exploração durante escalada;
- taxa de exploração fora da escalada.

No Minecraft, depois do merge, conferir que as linhas `escolha[...]` e `SELECAO ...` continuam aparecendo com o mesmo formato e que a seleção continua acontecendo no mesmo fluxo.

## Passo 3 — Cenas

Conferido automaticamente no próprio passo:

- `./gradlew build` e os testes do `Seletor` depois de cada cena movida;
- `ferramentas/refatoracao/verificar_movimento.py`: todos os métodos têm o mesmo texto do commit base.

No Minecraft, com `/sussurros debug on`, forçar cada cena e conferir no `sussurros-debug.log` que a sequência de linhas é a de sempre:

- `/sussurros cena tunel`: `tipo=ALGO_NO_TUNEL INICIO`, `etapa=ECO`, `etapa=RUIDO`, `etapa=PRESENCA manifestacao=M...`, `etapa=SILENCIO`, `FIM`.
- `/sussurros cena campo`: `tipo=LINHA_DAS_ARVORES INICIO`, `etapa=PRIMEIRA`, `etapa=PAUSA`, `etapa=SEGUNDA`, `etapa=SILENCIO`, `FIM`.
- `/sussurros cena marco`: `tipo=FOI_AQUI INICIO`, `etapa=ECO`, `etapa=PRESENCA`, `etapa=SILENCIO`, `FIM`.
- `/sussurros cena janela` (precisa de vidro por perto): `tipo=DO_OUTRO_LADO_DO_VIDRO INICIO`, `etapa=APARICAO`, depois `VIU_ATRAVES_DO_VIDRO` ou `TOQUE_NO_VIDRO`, `etapa=SILENCIO`, `FIM`.
- `/sussurros cena casa`: `tipo=VOLTOU_COM_VOCE INICIO`, `etapa=RASTRO`, `etapa=PORTA`, `etapa=PRESENCA`, `etapa=SILENCIO`, `FIM`.
- `/sussurros evento sinal` no subsolo, depois de quebrar alguns blocos: o `SINAL` continua podendo sair como `tipo=ECO_CURTO_DA_ACAO` (ele usa `sortearQuebraRecente`, que foi junto com a cena do túnel).

Em todas:

- o Hóspede criado registra `origem=COMANDO` e o `motivoPosicao` da cena (`CENA_TUNEL+...`, `CENA_CAMPO+...` etc.);
- não aparece `Erro no Diretor`.

## Regressão geral futura

À medida que novos passos extraírem acontecimentos, manifestação e entidade, adicionar neste arquivo os comandos e invariantes específicos de cada módulo antes do respectivo merge.
