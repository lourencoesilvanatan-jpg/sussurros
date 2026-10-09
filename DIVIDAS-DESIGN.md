# Sussurros — Dívidas de design

Este arquivo registra comportamentos ou decisões de arquitetura que foram deliberadamente deixados fora da refatoração atual. Uma dívida só deve ser resolvida em uma mudança consciente, com testes e revisão do impacto no comportamento do Diretor.

## Teto de categorias

- Com apenas duas categorias, o teto de 45% é matematicamente inviável: as duas categorias precisam somar 100%.
- O algoritmo atual faz duas voltas e, no caso 900/900/100, termina em aproximadamente 55% SOM e 45% MENTE.
- O resultado também é sensível à precisão de ponto flutuante e à ordem/forma das operações. Não alterar durante a refatoração.

## Telemetria da estratégia

- A nota/variável `ESTRATEGIA` atualmente não chega ao log.
- Decidir posteriormente se ela deve ser registrada, removida ou usada em outro ponto.

## Remoção de tochas (resolvida na 0.9)

- Nenhuma tocha sai mais do mundo: a que pisca, a "levada", as apagadas ao acordar, as das cenas de luz e as que ele apaga na caça são miragens (só o jogador vê sumir, a luz some junto e ela volta sozinha).
- `apagarTochaProxima` deixou de existir; quem apaga luz é `ApoioCaca.apagarLuzPerto`.
- Continua mexendo no mundo de verdade, por pouco tempo e com restauração: `O_CAMINHO_MUDOU` e `MARCA_IMPOSSIVEL` (`AlteracoesTemporarias`).

## `forcando` e `pedido.ehTeste()`

- Existe uma possível sobreposição conceitual entre `forcando` e `pedido.ehTeste()`.
- Não unificar agora: primeiro mapear todos os usos e confirmar se representam realmente o mesmo estado.

## Cenas (depois do passo 3)

- As cinco cenas repetem a mesma máquina de estados (verificar, iniciar, conduzir, silêncio) e a mesma checagem de "nenhuma outra cena ativa". Unificar é mudança de estrutura, não movimento: fazer num passo próprio, com testes.
- As classes de cena chamam 31 helpers que continuam no `Diretor` e por isso deixaram de ser `private`. Quando manifestação, rastro, som e geometria tiverem os seus módulos, as cenas passam a depender deles e a visibilidade pode fechar de novo.
- Os nomes dos métodos foram mantidos (`CenaAlgoNoTunel.verificarCenaTunel`). Encurtar para `verificar`/`iniciar`/`conduzir` fica para depois.
- `sortearQuebraRecente` mora em `CenaAlgoNoTunel` porque estava declarado na seção do túnel, mas também é usado pelo evento `SINAL` no subsolo. Mover para um lugar comum quando os acontecimentos forem extraídos.
- O estado de cada cena é um enum em `EstadoJogador` (`EstadoJogador.CenaTunel`) com nome parecido com o da classe (`CenaAlgoNoTunel`). Decidir depois se o estado vai morar na classe da cena.

