# Análise de design depois da primeira sessão de verdade (SPOILERS)

> Escrita em 09/10/2026 por uma sessão do Claude Code que não programou a 0.9, a pedido do dono. Ele jogou a alpha12 por uma hora e disse: "parece parado, nenhum evento de fato pra assustar", "muitos sons, que às vezes estão meio jogados", "te dropa itens pra você usar, no qual você não sabe exatamente como usar, e fica nisso talvez até aparecer a criatura". Pediu honestidade e comparação com outros mods. Ele não lê este arquivo.

**O que eu li:** o `sussurros-debug.log` e o `command_history.txt` da sessão dele, o relatório do `analisar.py`, `DESIGN-SPOILERS.md`, `PESQUISA-E-ANALISE.md`, `PLANO-MECANICAS.md`, a revisão externa de 09/10, o `CLAUDE.md`, e trechos do `Diretor`, do `Diario` e do arquivo de textos na `main` (3f8fdd0). Não li a caçada, o Avesso nem o Véu no código: o que digo deles vem do `DESIGN-SPOILERS.md`.

**O que eu não fiz:** não joguei, não ouvi nenhum som e não vi a criatura. O que está aqui é leitura de log, de código e de documento.

---

## 1. Veredito

A ideia é boa e as peças combinam no tema. O que falha é a ordem em que elas chegam a quem joga. Em duas horas de log não houve um único momento forte, e o que o jogador recebeu foram sinais de uma ameaça que ele ainda não conheceu e ferramentas para lidar com ela. A impressão dele ("um mod com muitos sons e itens que eu não sei usar") descreve com exatidão o que o log mostra.

Uma parte disso é defeito de ajuste da alpha12, que já está sendo corrigido (seção 3). A outra parte é de desenho e a correção de ritmo não resolve (seção 4).

---

## 2. O que a sessão de 09/10 mostrou

Sessão real de 20:53 a 21:51, mundo novo, alpha12. No arquivo é o segundo trecho (o relógio volta de 4716 s para 60 s na linha 756). São 55 minutos de jogo.

| Medida | Valor |
|---|---|
| Chegada às fases 1, 2 e 3 | 11 min, 21 min, 40 min |
| Eventos do Diretor | 5 (`VIGIA` duas vezes, `PASSO_UNICO`, `SINAL`, `ECO`) |
| Desses, sorteados no modo `PISO` | 4 |
| Maior intensidade de um evento | 10 |
| Presságios e cenas de ambiente | 9 e 1 |
| Avisos e cobranças da Conta | 8 e 4 |
| Aparições da criatura | 0 |
| Eventos fortes (intensidade 22 ou mais), caçadas, capturas | 0, 0, 0 |
| Pressão máxima; obsessão no fim | 10; 100 |
| Saldo de atenção dos 15 min até o fim | entre -6 e 3 |
| Sinais marcados `semCriatura=sim` | todos |
| Sino Oco | 16 usos: 11 respostas `RASTRO`, 5 `SILENCIO` |
| Caixa de Música | 9 usos |
| Vela, Fio, Ossos, Linha | 1, 1, 2, 2 |

Três leituras:

- **Quase metade do que ele percebeu veio de uma regra que ele não vê.** O Diretor e a Atmosfera produziram 15 saídas; a Conta produziu 12. Para quem joga, um sino que toca sozinho 30 a 120 s depois é igual a um som aleatório.
- **Ele estava testando os itens, não usando.** Dezesseis toques de sino em 35 minutos é alguém tentando descobrir o que o objeto faz. A resposta "silêncio" está certa (não havia criatura), mas se lê como "não funcionou".
- **Ressalva:** ele gerou três estruturas por comando (`/sussurros estrutura nicho|posto|marco`) e se teleportou para uma vila. Por isso teve tantos itens tão cedo. A Caixa na fase 2 e as páginas são do desenho normal.

O trecho anterior do mesmo arquivo (70 minutos, versão anterior, com comandos de teste no meio) é o oposto: 40 manifestações da criatura. Somando os dois: 2h06 de jogo, nenhum evento forte, nenhuma caçada.

---

## 3. O defeito de ajuste (já em correção)

O orçamento de atenção da alpha12 deixava todos os sistemas disputarem o mesmo saldo, e o mais barato ganhava sempre: o presságio (14) e o aviso da Conta (10) gastavam o saldo assim que ele aparecia, e o Diretor (16 ou mais) quase nunca alcançava. Outra sessão chegou à mesma conclusão pelo mesmo log e está corrigindo na branch `fix/ritmo-travado` (reserva para o Diretor; avisos da Conta fora do orçamento). Não mexi nisso.

O que a correção não cobre:

- Ela devolve a voz ao Diretor, mas o que o Diretor tem para dizer nas fases 1 a 3 continua sendo coisa pequena. Mais eventos de intensidade 4 a 10 não criam um momento.
- Com os avisos da Conta fora do orçamento, a parte "regra invisível" do que o jogador percebe tende a crescer, não a diminuir.
- **A métrica que guiou a alpha12 mede quantidade, não peso.** "Mediana de 180 s entre saídas" foi atingida numa sessão em que nada aconteceu. A regra que a outra sessão já escreveu no código ("confira a mistura, não só o ritmo") é a certa; falta virar número (seção 6).

---

## 4. Os problemas de desenho

### 4.1 Ferramenta antes da ameaça

Os itens servem para detectar, afastar, atrair e confirmar uma criatura. Na sessão, o jogador recebeu a Caixa aos 21 min e meia dúzia de itens aos 25, e a criatura não apareceu em 55. Um detector sem nada para detectar é um brinquedo.

A ordem que funciona é: a ameaça se mostra, a ferramenta chega, a ferramenta é testada contra a ameaça. Hoje é o contrário.

### 4.2 Os verbos do jogador estão escondidos

O princípio "sistema vago de propósito" está sendo aplicado às ferramentas, e ele só vale para o monstro.

- As dicas dos itens são de clima, não de uso ("Não tem badalo. Mesmo assim, alguma coisa responde.").
- O canal que ensina é o diário, com 28 páginas entregues **em ordem fixa** (`Diario.lerProxima`): a página do Sino é a 10, a do Fio é a 11, e a da Caixa é uma das três últimas. O jogador recebe a Caixa na fase 2 e a explicação dela depois de outras 25 páginas.
- A Conta foi desenhada para esconder a ligação entre causa e efeito ("quando ele já não liga uma coisa à outra"). Isso torna a punição indistinguível de ruído. A revisão externa já pedia um "recibo".
- O dono não lê spoilers. O jogo é a única fonte dele.

Thomas Grip, sobre os monstros do SOMA: se for mecânico demais fica sem graça, e "se for aleatório demais, as interações viram uma névoa que o jogador não consegue entender". É a palavra que o dono usou: "jogados".

### 4.3 Não existe primeiro contato

Pelos limiares (`LIMIAR_FASE = {0, 600, 1800, 3300, 5400}`), a criatura aparece na fase 3 (55 min de tempo de assombração) e a caçada só existe na fase 4 (90 min), no escuro, com inquietação de 60 ou mais e longe da base. Quem joga de dia e dorme à noite pode nunca reunir essas condições; na sessão a inquietação ficou em zero quase o tempo todo.

Todo o conteúdo mais forte da 0.9 (caçada em estágios, captura, Avesso, Véu) está atrás de um tempo que duas horas de jogo não alcançaram. A própria pesquisa registra que servidor de amigos dura umas duas semanas e que "zero vezes vira 'meu mod quebrou'".

### 4.4 Sinais sem referente

O plano diz "o medo está no antes" e "o mesmo aviso serve para evento falso e para evento real". Isso exige que o real exista. Na primeira hora, 100% dos sinais foram falsos por construção (`semCriatura=sim`). Som que nunca dá em nada vira papel de parede.

Josh Bycer, sobre tensão em jogos de terror: "tem de haver sempre uma construção seguida de uma liberação; não dá para fazer só uma das duas", e quando o jogo só acumula, "o jogador acaba se acalmando e deixa de ficar tenso".

### 4.5 O ritmo é plano

O Diretor tem estados (calmo, observando, testando, escalando, ameaçando, recuando), mas a pressão não passou de 10 em 55 minutos. Não há arco dentro de uma sessão: subir, um pico, soltar. As duas versões medidas são planas em alturas diferentes: 45 coisas pequenas por hora em 08/10, 15 coisas pequenas por hora em 09/10.

### 4.6 A largura passou da validação

Foram trinta PRs em três dias: uns 13 itens, uma dimensão, três tipos de lugar, um baralho, uma dívida escondida e 52 sons sintetizados que ninguém ouviu antes de entrar. Uma pessoa jogou, por cerca de duas horas. A revisão externa disse o mesmo e eu concordo com a tese dela (medir e cortar). O que acrescento: o corte tem de olhar para **o que o jogador encontra nas três primeiras horas**, porque é ali que hoje há muito sistema e pouco momento.

### 4.7 O que está certo e não deve mudar

- Nunca alto, nunca matar, nunca estragar a construção. São exatamente as três reclamações mais repetidas sobre outros mods, e o Sussurros não tem nenhuma.
- Som e criatura só para o alvo.
- O tema amarra bem: cinza, chama pálida, o diário de quem veio antes, a casa dele, a cantiga.
- A aparição curta que o dono aprovou em 08/10 ("às vezes é difícil de ver, mas tem como perceber algo piscando"). Na sessão de 09/10 ela não aconteceu nenhuma vez.

---

## 5. Comparação com outros mods

As fontes e as citações estão no `PESQUISA-E-ANALISE.md` (seções 6, 10, 12 e 13).

| Crítica feita a outros mods | Vale aqui? |
|---|---|
| "O que ele faz além de olhar?" (Don't Let It Learn, 280 pontos) | **Sim, é a principal.** 40 aparições e nenhuma consequência no trecho antigo; nem aparição no novo |
| Falta de documentação dos comportamentos (comentário mais votado, 601 pontos) | **Sim**, nos itens |
| "Nunca encontrei; achei que o mod estava quebrado" (Cave Dweller, From The Fog) | **Sim**, na alpha12 |
| Frequência alta | Valia em 08/10; a alpha12 corrigiu demais |
| Virar jogo de progressão (The Broken Script 2.0) | **Risco real**: 13 itens, receitas, dívida |
| Jeito fácil de escapar | Sim: jogar de dia e dormir evita a caçada |
| Volume alto, susto barato, morte injusta, estragar construção | Não |

O que os mods elogiados fazem e o Sussurros ainda não faz:

- **Cave Dweller:** o relato mais forte da pesquisa é som, depois a criatura na esquina, depois o esconderijo violado. O som tem dono.
- **From The Fog:** três dias de nada e, a partir daí, avistamentos distantes e curtos a cada um ou dois minutos, com eventos físicos muito raros. Há prova de vida constante.
- **The Hollow** (Fabric 26.2): ser pego cega e enfraquece, sem matar. É a mesma escolha do Sussurros, mas o jogador chega lá.

---

## 6. O que eu faria, em ordem

Nada aqui é sistema novo. É reordenar e garantir.

### 6.1 Um primeiro contato garantido na primeira sessão

Uma vez por mundo, entre 15 e 25 minutos de jogo, na primeira vez que o jogador estiver fora de casa com pouca luz (entardecer conta; não exigir inquietação):

1. o aviso que a caçada já tem (o mundo emudece, uma luz perto falha);
2. ele nasce atrás, a 18–26 blocos, e não se mexe;
3. encarado por um ou dois segundos, dissolve;
4. onde ele estava fica a primeira Cinza Pálida, com vestígio.

Sem toque e sem captura. São peças que já existem (estágio 1 da caçada, o fade, o vestígio). O jogador sai sabendo três coisas: existe algo, ele chega perto, e ele deixa matéria. A primeira cinza já destrava as receitas, então a primeira ferramenta passa a nascer do primeiro encontro.

### 6.2 Cada ferramenta chega depois da situação que ela responde, com a página dela

- A entrega de página deixa de ser estritamente sequencial: ao pegar um item pela primeira vez, a próxima página entregue é a daquele item.
- A Caixa sai da transição para a fase 2 e passa a chegar depois da primeira vez que ele ouvir a cantiga.
- Nas duas primeiras horas, dois verbos: afastar (Vela) e perguntar (Sino). O resto vem depois, pelos lugares e pelo baralho.

### 6.3 O primeiro uso de cada item sempre dá uma resposta inconfundível

Vale uma vez por item e por jogador. O primeiro toque do Sino responde de perto e deixa um vestígio visível no lugar; a mensagem diz a direção. O jogador aprende a gramática com um caso claro e só depois recebe os casos ambíguos. Uma linha de uso na dica do item não estraga o mistério do monstro.

### 6.4 A Conta com carência e com recibo

- Os três primeiros usos de cada item não somam.
- A cobrança acontece no próprio item e na hora do uso seguinte, não 30 a 120 s depois.
- Depois da primeira cobrança, uma linha nova no Caderno (o "recibo" da revisão externa).

O princípio: esconder as regras do monstro, nunca os verbos do jogador.

### 6.5 Sinal com referente, no começo

Nas duas primeiras horas de um mundo, pelo menos metade dos sinais sonoros é seguida, em 30 a 90 s, de algo que dá para ver naquele lugar (um vestígio, o vulto, uma porta de fato aberta). Só depois que "som quer dizer alguma coisa" o aviso que mente tem o que mentir.

### 6.6 Um pico por sessão

O orçamento guarda saldo para um momento forte por sessão de jogo e o enfeite é que cede. Se em 30 a 40 minutos desde a entrada no mundo não houve nada de intensidade 22 ou mais, o próximo sorteio é obrigatoriamente forte (uma cena composta ou uma aparição com consequência), com o silêncio de sempre depois.

### 6.7 Critérios de pronto que protegem do "parado"

Os da revisão externa protegem do excesso. Faltam os do outro lado, medidos pelo `analisar.py`:

| Critério | Hoje (09/10) |
|---|---|
| Num mundo novo, primeira aparição até os 25 min | não houve em 55 min |
| Em cada hora de jogo na fase 1 ou mais, ao menos uma saída de intensidade 22 ou mais | 0 em 2h06 |
| Da fase 2 em diante, no máximo 70% dos sinais sem criatura nem consequência | 100% |
| Aparições distantes e curtas: de 6 a 12 por hora da fase 2 em diante | 0 (alpha12); 34 (08/10) |
| Saídas da Conta: menos de um terço do total percebido | 44% |

São pontos de partida, como os dela.

### 6.8 Fases mais curtas

Com sessões de uma hora e servidores de duas semanas, a primeira caçada deveria caber no fim da primeira sessão ou no começo da segunda. Sugestão a testar: 8, 20, 35 e 55 minutos.

### 6.9 O que eu não faria agora

Item, lugar, evento ou dimensão nova; o corpo novo da criatura; a fase final. Tudo isso é bom e está certo na revisão externa, mas vem depois de uma sessão em que o dono saia com uma história para contar.

---

## 7. Perguntas para o dono (sem entregar nada)

1. Na sessão de 09/10, teve algum momento em que você parou de jogar para prestar atenção?
2. Dos sons, algum pareceu falso, de sintetizador?
3. Dos itens, qual você acha que entendeu? Qual não faz ideia?
4. Você prefere que as dicas dos itens digam como se usa, ou quer descobrir sozinho?
5. Suas sessões costumam ter quanto tempo?

---

## 8. Limites desta análise

- Uma sessão, um jogador, e com estruturas geradas por comando.
- Os números propostos não foram testados.
- A leitura dos outros mods vem da pesquisa de 08/10, com os limites que ela mesma registra.
- Não li o código da caçada nem do Avesso. Se algo aqui contradisser o que eles já fazem, vale o código.

## Fontes novas

- Josh Bycer, "The Balancing Act of Tension in Horror Game Design" (2015): https://www.gamedeveloper.com/design/the-balancing-act-of-tension-in-horror-game-design
- Thomas Grip em "The tricky design problem of SOMA's memorable monsters" (2015): https://www.gamedeveloper.com/design/the-tricky-design-problem-of-i-soma-i-s-memorable-monsters
