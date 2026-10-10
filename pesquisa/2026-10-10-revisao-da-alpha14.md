# Revisão da alpha14, "a primeira hora" (SPOILERS)

> Escrita em 10/10/2026 pela sessão que fez a análise de design, a pedido do dono ("analise as atualizações dele"). Cobre o PR #34 (de `b3f2623` a `3012587`). O dono não lê este arquivo.

**Como foi feita.** Duas leituras separadas. A minha comparou a entrega com o combinado (seções 6.1 a 6.4 e 10 da análise de design, e a pesquisa de 10/10). A outra foi de um subagente que recebeu só o código e as regras do `CLAUDE.md`, sem a minha opinião, e procurou defeito. Dos achados dele, eu conferi no código e no log os três primeiros; os demais estão marcados como relatados. Ninguém rodou o jogo, o Gradle nem ouviu nada.

**Veredito.** A entrega faz o que foi pedido e não acrescenta conteúdo. O problema está em dois pontos do próprio primeiro contato, que é o que a próxima sessão do dono vai testar: uma tentativa que ninguém vê cala o Diretor, e "foi visto" pode valer sem ele ter visto. Recomendo uma rodada só de conserto antes de ele jogar.

---

## 1. Conferência contra o combinado

| Pedido | Situação |
|---|---|
| 6.1: primeiro contato por regra, com prazo final, por jogador, sem toque, deixando a cinza | Feito (`PrimeiroContato`). O relógio é tempo de jogo, não tempo de assombração |
| Seção 10: só conta quando foi visto | Feito em parte: ver o achado 2 |
| Seção 9: passar por cima do portão de V | Feito |
| 6.2: item depois do monstro, um verbo por vez, página junto com o item | Feito (`Ensino`); as fontes de ferramenta esperam o contato. Ver o achado 3 |
| 6.3: primeiro uso verdadeiro e silêncio com estado próprio | Feito. A dica do Sino manda usá-lo "onde algo esteve", o que resolve o primeiro toque dado em casa |
| 6.3: uma linha de uso em cada item | Feito. Li as dicas: dizem o verbo, não dizem número nem regra da criatura |
| 6.4: carência, marcador, cobrança no uso seguinte, recibo | Feito. Os recibos estão escritos como fato, sem bronca |
| 6.7: critérios no `analisar.py` | Feito. Ver os achados 5 a 7 |

Desvios, os dois justificados: a Caixa saiu da passagem para a fase 2 e passou a ser achada (a minha proposta, "depois de ouvir a cantiga", era impossível: a cantiga depende da caixa); o marcador da Conta usa um som que já existia.

A receita do Sino pede uma cinza, e o contato deixa exatamente uma. A cadeia fecha: contato, cinza, página, receita, sino.

Rodei o analisador novo no log de 09/10: ele dá os alertas certos (nenhuma aparição na mira até os 25 minutos, hora sem saída forte, Conta em 35%, nenhuma manifestação em 35 minutos de fase 2 ou mais). Serve de régua.

---

## 2. Achados

### 2.1 Uma tentativa de contato que ninguém vê cala o Diretor (conferido)

- **Onde:** `PrimeiroContato.java:156` (`Atencao.gastar(..., Atencao.CENA, ...)`) e a condição da linha 102, que não olha `RECUANDO`.
- **O que acontece:** cada tentativa gasta 24 de atenção sem conferir saldo. Na fase 1 o saldo volta 3 por minuto. Tentativa não vista se repete a cada 4 a 6 minutos e gasta mais do que volta.
- **No log da sessão sintética** (`build/run/gameTest/sussurros-debug.log`, gravado às 06:44 de 10/10):
  - `Teste39`: tentativas aos 984, 1316, 1623 e 1928 s; visto só na quarta. Evento do Diretor aos 831 s e o seguinte aos 2362 s: **25 minutos e meio sem evento, com quatro avisos de caçada no meio**.
  - `Teste41`: tentativas aos 984 e 1283 s (a segunda com o Diretor em `RECUANDO` desde os 1269 s); evento aos 796 s e o seguinte aos 2019 s.
- **Por que importa:** aviso repetido sem nada à vista é o "sinal sem referente" que esta versão veio tirar. Um jogador de verdade que não se vire na primeira tentativa recebe isso, e mais nada, por vinte minutos.
- **Ressalva:** o jogador de mentira não se vira para o som. Um jogador de verdade pode ver na primeira. O mecanismo existe de qualquer jeito.
- **Sugestão:** a tentativa só gasta atenção quando foi vista (ou devolve o gasto em `visto=nao`); tentativa nova respeita `RECUANDO`, menos a do prazo final; e o aviso inteiro toca só na primeira tentativa, as seguintes ficam com os passos.

### 2.2 "Foi visto" pode valer sem ele ter visto (conferido em parte)

- **Onde:** `HospedeEntity.java:622-631`; `PrimeiroContato.java:189`; `Diretor.estaVendo`.
- **(a) De canto.** 37 ticks em qualquer parte da tela dão `VISTO_DEMAIS`, e isso conta. Nos contatos dados como vistos na sessão sintética a linha é `visto=sim ... naTela=37t encarado=nao`. Para o mesmo jogador, o analisador diz "primeira aparição na mira dele: nenhuma". As duas definições se contradizem, e a seção 10 pede a do analisador. Conferido no log.
- **(b) O "tímido".** `SUMIU_NO_DESVIO` vem antes de `contarVisto` e não exclui `this.contato`. Um tick com a mira nele seguido de quatro fora da tela (uma varrida de câmera) o faz sumir sem dissolver, e conta. Conferido no código.
- **(c) No escuro.** `estaVendo` devolve verdadeiro a menos de 20 blocos sem olhar a luz, e o contato noturno nasce a 18–26. Numa caverna ele pode ser dado como visto no breu, com a cinza caindo num lugar que o jogador não sabe que existe. A lógica está conferida; o quanto ele aparece com luz 0 no cliente, não.
- **Sugestão:** para o contato, "visto" é olhar direto por alguns ticks seguidos, num lugar com luz bastante (ou com a criatura contra o céu). Tempo de canto pode dissolvê-lo, mas o contato continua devendo. O ramo do "tímido" não vale para `contato`. E uma definição só para o Java e para o analisador.

### 2.3 Uma entrega de página para várias páginas pedidas (conferido)

- **Onde:** `Ensino.java:84-113` e `EstadoJogador.paginaDeItemApos`.
- **O que acontece:** só existe um agendamento, em memória. Um segundo item pego antes da entrega não agenda outra; depois de uma entrega o campo volta a -1 mesmo havendo página pedida; sair do mundo dentro da espera perde o agendamento.
- **Cenário comum:** o baú da Casa do Vigia entrega vários itens de uma vez. Quatro páginas pedidas, uma deixada.
- **Efeito:** nada se perde (a próxima Página Rasgada mostra a pedida), mas a página deixa de chegar junto com o item, que era o ponto da 6.2.
- **Sugestão:** reagendar enquanto `Diario.temPedida(m)`, e rearmar na entrada do jogador.

### 2.4 A tocha quebrada de verdade continua (conferido)

- **Onde:** `Atmosfera.java:397`, `level.destroyBlock(pos, true)`, no ramo de `LUZ_ERRADA` da fase 3 em diante.
- **Por que agora:** o `ROTEIRO-DE-TESTE.md` diz ao dono que nada estraga a construção dele. É o defeito 1 do Apêndice D da pesquisa de inteligência, deixado de fora de propósito. Numa sessão de uma hora ele chega à fase 3.
- **Sugestão:** virar miragem, como as outras luzes.

### 2.5 O alerta de "nenhuma aparição até os 25 minutos" dispara quando o prazo funciona (relatado; o subagente rodou)

- **Onde:** `analisar.py:485-488`.
- O alerta exige vista até 25:00, e o caminho do prazo só começa a tentar aos 25:00. Um log com tentativa aos 1500 s e vista aos 1509 s dá o alerta. Sugestão: uma folga de alguns minutos, ou medir pelo `relogio=` das linhas `CONTATO`.

### 2.6 "Hora sem saída forte" conta o tempo fora do mundo normal (relatado; o subagente rodou)

- **Onde:** `analisar.py:446-457`. O laço anda pelo relógio bruto e ignora os trechos. Nether, End, Avesso e espectador viram "hora parada".

### 2.7 Critérios da seção nova que não medem o que dizem (relatado)

- `SEGUIDOR` e `PEGADAS` nunca entram nas listas: as linhas deles não têm `pos=`.
- `VESTIGIO tipo=CINZA_RASTRO ... semCriatura=sim` e criatura de comando contam como "algo ali".
- Duas sessões coladas num arquivo são misturadas sem aviso.

### 2.8 A página chega com quatro passos nas costas, fora da `Atencao` (relatado)

- **Onde:** `Ensino.java:110-111`, via `Diretor.deixarPagina`. É o som do evento `PASSOS`, até nove vezes por mundo, sem respiro e fora do relatório de ritmo. Logo depois do contato, cai dentro do respiro dele. Se conta como "resposta a uma ação do jogador" é decisão de desenho; a `transicao` já fazia igual.

### 2.9 Menores (relatados)

- Os passos do contato usam `level.playSound(null, ...)`: o amigo ao lado ouve (`PrimeiroContato.java:173`). O comentário justifica, e existe a versão só para o alvo em `ApoioCaca.passo`.
- Criatura de comando a até 52 blocos faz o primeiro toque do Sino gravar `SINO_RESPONDEU` e liberar a receita da vela (`Diretor.java:3850-3859`).
- `Ossos.jogar` com desfecho forçado soma na Conta e gasta carência (`Ossos.java:107`); o teste `osOssosCaemESomem` passou a afirmar isso.

### O que a segunda leitura conferiu e está certo

O uso da `Memoria` do tick; o estado por jogador em morte, troca de dimensão e saída; os critérios dos nove advancements de receita; nenhum log novo consumindo sorteio; as chaves de texto e os números de página; a aritmética de tick e segundo.

---

## 3. Testes que não provam o nome (relatado)

1. `oSinoNaoInventaResposta`: passa sem o comportamento (o código antigo também daria silêncio naquela montagem) e não confere a frase.
2. `oContatoQueNinguemViuContinuaDevendo`: não prova que existe segunda tentativa.
3. `antesDaJanelaNaoHaContato`: não toca a fronteira dos 15 minutos nem a regra de pouca luz e fora de casa.
4. `oContatoSoContaQuandoEVisto`: cobre só o olhar direto contínuo; não cobre o canto, o "tímido" nem a forma diurna do prazo.
5. `aContaTemCarenciaMarcadorERecibo`: o "recibo" é o contador, não a linha do Caderno; chama `Conta.somar` direto; oito dos nove itens não têm teste de cobrança.
6. `aPaginaChegaComOItem`: só o caso de um item.

---

## 4. Ordem que eu proponho: uma rodada só de conserto

1. O achado 2.1 (a tentativa não vista não pode calar o Diretor).
2. O achado 2.2 (uma definição só de "visto", com luz e com mira).
3. O achado 2.4 (a tocha).
4. Os achados 2.5 e 2.6 (para o relatório da próxima sessão não dar alarme falso).
5. O achado 2.3 e os testes da seção 3 que cobrem 1 e 2.

O resto pode esperar a sessão do dono.

## 5. O que observar na sessão dele (sem código)

- A cadeia depende de ele **ir até o lugar** buscar a cinza. Se o log mostrar `CONTATO visto=sim` e nenhuma coleta, a primeira ferramenta nunca nasce. Não há plano B hoje.
- Quantas tentativas até o `visto=sim`, e se foi `encarado=sim`.
- O intervalo entre o contato e o evento seguinte do Diretor.
- A resposta dele à pergunta do roteiro: para que serve cada item.

## 6. Sobre a análise externa de 10/10 (ChatGPT)

O dono colou uma análise feita por outro assistente. Ela parte de uma cópia velha do repositório (diz `0.8.0-alpha1` e 8 commits; a `main` está na `0.9.0-alpha14`, com 48), então a ordem que propõe segue o `ROADMAP.md` antigo e os avisos contra dimensão e muitos itens chegam tarde. O que ela acerta já está feito ou planejado: evento executado não é evento percebido; sinal, confirmação, consequência, trégua; começo protegido sem roteiro fixo; conteúdo só depois das capacidades. A proposta de a criatura raciocinar com hipóteses e graus de confiança é mais inteligência de bastidor: só vale a parte em que o erro dela aparece na tela (proposta 9 da pesquisa de inteligência). Das fontes: Distant Friends existe, é MIT e tem versão para Fabric 26.2 (https://modrinth.com/mod/distant-friends); o "Friend" de `github.com/ThompsonCBB/friend` dá 404 e não foi achado.

## 7. Limites

- O log sintético usado é o que estava na pasta de trabalho, gravado nove minutos antes do merge; as constantes batem com o código final, mas não confirmei que saiu exatamente dele.
- Os itens marcados "relatado" vêm da leitura do subagente e eu não reli linha a linha.
- Não li `Conta.java` inteiro, nem os testes, nem os documentos alterados além do `DESIGN-SPOILERS.md` e do `ROTEIRO-DE-TESTE.md`.
