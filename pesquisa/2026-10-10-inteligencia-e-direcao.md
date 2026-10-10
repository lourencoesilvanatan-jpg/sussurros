# Inteligência e direção: o que faz parecer que existe alguém ali (SPOILERS)

> Escrita em 10/10/2026 por uma sessão do Claude Code, a pedido do dono. Ele não lê este arquivo. É pesquisa: nenhum código foi mexido.

**A pergunta.** A criatura parece inteligente para quem joga? Hoje, "inteligente" no Sussurros quer dizer "o Diretor ajusta pesos pelo que assustou". O que mais pode fazer o jogador sentir que existe alguém ali, com intenção, sem roteiro fixo e sem trapaça?

**Como foi feito.**

- Li eu mesmo: `CLAUDE.md`, `DESIGN-SPOILERS.md` inteiro, a situação do `PLANO-EXPANSAO.md`, `ROADMAP.md`, `ROTEIRO-DE-TESTE.md`, `DIVIDAS-DESIGN.md`, a revisão externa de 09/10, a análise de design de 09/10 (branch `docs/analise-de-design`, PR #32), o `analisar.py`, `TestesDeSessao`, o `sussurros-debug.log` e o `command_history.txt`.
- Três subagentes leram o código inteiro e devolveram inventários com `arquivo:linha`: o Diretor e as cenas; a criatura, a caçada e a memória; itens, mundo, som e cliente. Conferi no código os achados que sustentam as conclusões (Apêndice B).
- Quatro subagentes fizeram a pesquisa externa abrindo as fontes. As marcas de confiança são as deles; eu não reabri cada fonte. As citações literais passaram pelo extrator de páginas da ferramenta: confira a redação antes de republicar alguma.
- Conferi nos JARs da 26.2 e na API do Modrinth o que as propostas precisam do jogo (Apêndice C).

**O estado do repositório.** A `fix/ritmo-travado` já entrou na `main` como 0.9.0-alpha13 (PR #31). O log da pasta de jogo termina em 09/10 às 21:50: **a alpha13 ainda não foi jogada**. Tudo o que digo do log é da alpha12 ou de antes.

**O que ninguém fez.** Ninguém jogou, ouviu os sons nem viu a criatura. Nenhuma proposta foi testada.

**Como ler as marcas.** `[log]` = está no log. `[código]` = está escrito no código, na linha citada. `[suponho]` = dedução minha. Nas fontes externas: VERIFICADO (a fonte foi aberta e lida), PROVÁVEL (fonte secundária ou resumo), ESPECULAÇÃO (extrapolação; nenhuma fonte afirma).

---

## 0. Resposta curta

**Hoje, pouco.** O motivo resume o documento: a inteligência do Sussurros está em quem decide (o Diretor), e quase nada dela chega a quem aparece (o Hóspede) nem ao que se ouve.

- O Diretor mede muito e lembra muito. Quase tudo vira peso de sorteio, e peso de sorteio não se vê.
- O Hóspede, fora da caçada, é uma figura parada que já nasce virada para o jogador e some em um terço de segundo. Não anda, não nota, não reage e não faz nada além de olhar.
- Os sons não têm dono. Dos 33 eventos, 25 nunca envolvem a criatura. Nenhum som quer dizer "é ele, e ele está ali". Quem vai conferir não acha nada.
- O que há de mais esperto (a busca na caçada, as respostas às contramedidas) fica depois dos 90 minutos e, por regra, acontece quando o jogador não está olhando.

A pesquisa externa aponta toda para o mesmo lado. O jogador sente intenção quando há: **atenção com alvo** (ele olha para uma coisa minha), **resposta na hora** ao que eu faço, **coerência** (o som, a marca e o corpo contam a mesma história), **memória mostrada** e **erro que se vê**. Nenhuma dessas coisas pede sistema grande. Quase todas pedem mostrar o que já existe.

A fase que eu proponho não acrescenta eventos. Acrescenta quatro capacidades pequenas: ele **nota**, ele **responde na hora**, ele **está em algum lugar**, e **o som tem dono**. O resto das propostas sai delas.

---

## 1. Inventário de capacidades

"Percebe?" responde a duas perguntas: o jogador consegue saber que aquilo aconteceu, e consegue ligar à causa? "Primeira vez" é o tempo nominal de jogo num mundo novo, um jogador, ritmo padrão. `[log]` Na sessão de verdade as fases chegaram antes do nominal: fase 1 aos 10 min, fase 2 aos 20, fase 3 aos 39.

### 1.1 Perceber: o que o mod mede do jogador

| Capacidade | Onde | O jogador percebe? Como? | Primeira vez |
|---|---|---|---|
| Por onde ele andou (Rastro: um ponto a cada 2 s, 180 pontos, só na sessão) | `Rastro.java`; `Diretor.java:509` | Fraco. É a medida mais usada do mod (sons, aparições, cenas). Mas o caminho dele quase sempre está atrás dele, onde um ponto sorteado "atrás" também cairia | 10 min |
| Para onde ele olha, com o campo de visão real do cliente | `Percepcao.java` | Não. Garante que nada nasce na tela: é uma ausência | sempre |
| Reação em 3 s: girou, olhou para a fonte, congelou, fugiu, agachou, investigou | `Leitura.java` | Não. Vira peso e estado. A única resposta é a cadeia, de 4 a 40 s depois e em outro lugar | 10 min |
| Escuro, noite, subsolo, tipo de lugar (casa, subsolo, aberto, floresta) | `Diretor.java:515-550`; `ContextoMundo.java` | Médio: "acontece mais quando estou no escuro, sozinho e longe" | 10 min |
| O que ele quebrou e que porta usou (10 últimas ações, com som, lugar e hora) | `Diretor.java:168-193` | Sim, em parte: o som da picareta dele volta, às vezes do lugar certo | 10 min |
| A cama (a casa) | `Diretor.java:3195-3221` | Sim: o Boneco, as manhãs, a volta para casa, a frase da 4ª noite | 30 a 55 min |
| A porta mais usada | `Diretor.java:3119-3181` | Só numa casa com mais de uma porta | 30 min |
| O que ele escreve no chat (4 últimas falas) | `Diretor.java:239-248` | Sim, sem dúvida. Quase não ocorre jogando sozinho | 55 min |
| Onde morreu | `Diretor.java:217-226` | Sim, se reconhecer o lugar | 55 min |
| Minutos por chunk e chunks visitados | `Lugares.java` | Não. A "rota" é o centro de um chunk onde ele ficou 20 minutos | — |
| Giros de mais de 120° ("olha para trás") | `Diretor.java:497-501` | Não | — |
| Quantas vezes o viu, o encarou, o feriu | `Diretor.java:3499-3578` | Fraco e lento (ele passa a aguentar mais o olhar) | 55 min em diante |
| Uso de cada item | `Memoria`, `Conta.java` | Em parte (a vela dura menos; a caixa soa gasta). O resto lê como "o item falhou" | conforme o item |
| Baú aberto | `Diretor.java:194-196` | Não. Só serve de ruído na caçada; não é lembrado | — |
| Outros jogadores por perto (48 e 7 blocos) | `Diretor.java:826, 3337` | Não | — |
| Na caçada: ouve movimento, quebra de bloco, porta e baú; vê a 8 blocos | `HospedeBusca.java` | Sim, dentro da caçada | 90 min |

**Não mede** `[código]`: item na mão, inventário, colocar bloco, o que ele constrói, comer, pular, bioma, a hora de cada hábito, de onde para onde ele costuma ir.

### 1.2 Lembrar

| Capacidade | Onde | O jogador percebe? | Primeira vez |
|---|---|---|---|
| Pesos por evento e por categoria (memória longa, salva, e curta, de sessão) | `Diretor.java:1261-1276` | Não. Uma reação forte multiplica o peso por cerca de 1,4, num sorteio de 10 a 20 candidatos, 10 a 30 vezes por hora | 10 min |
| Perfil de seis traços (cautela, luz, caseiro, explorador, confronto, fuga) | `Perfil.java` | Não. Só age acima de 65. `[log]` Em 2h06 de jogo os traços ficaram entre 28 e 66; só "explorador" passou de 65, por instantes | — |
| Obsessão, inquietação, pressão | `Diretor.java:560-674` | Fraco: a cor drena e um fundo grave sobe, ao longo de uma hora | 10 min |
| Ousadia (vezes visto ÷ 2, mais vezes ferido) | `Diretor.java:1295` | Sim, devagar: ele aguenta mais o olhar e nasce mais perto. É a adaptação mais visível do mod | 55 min em diante |
| Marcos: o lugar de uma reação forte, para a cena "Foi aqui" | `Lugares.java`; `Diretor.java:1695` | Em tese sim. Na prática quase nunca nasce: só dois eventos, com reação forte, criam marco | 55 min em diante |
| Vestígios (18 marcas de onde ele esteve) | `Vestigios.java` | Só pelas ferramentas (Olho, Sino, Ossos). Nem a criatura nem o Diretor decidem nada com eles | conforme o item |
| A cantiga, depois de três toques da caixa | `Cantiga.java`; `Diretor.java:1098` | Sim, se ele ligar a melodia à caixa | 30 min em diante |
| Velas usadas, vezes que atravessou parede, caçadas, golpes levados | `Memoria` | Sim para a vela; fraco para o resto | 90 min |
| A Conta (a dívida dos itens) | `Conta.java` | Só os efeitos soltos. Não há recibo | conforme o uso |
| O que o mundo guarda: passo do Boneco, travessias da Soleira, visitas à Casa do Vigia e ao Avesso | `Erguidos`, `CasaDoVigia`, `Avesso` | Sim: são físicos e repetem | 30 min em diante |
| Sete chaves gravadas e nunca lidas (`olhos`, `veus`, `capturas`, `tochas_roubadas`, `fios_armados`, `iscas_armadas`, `caderno_usos`) e o "último dia visitado" de cada chunk | `Memoria`, `Lugares.java:68` | — | — |

**Não lembra** `[código]`: como o jogador escapou de uma caçada (só vai para o log), onde ele se escondeu, nem o encontro anterior. A memória da própria criatura morre com ela a cada aparição.

### 1.3 Decidir

| Capacidade | Onde | O jogador percebe? | Primeira vez |
|---|---|---|---|
| Quando agir: vulnerabilidade V, portões por intensidade, "oportunidade" | `Diretor.java:817-852`; `Seletor.java:104` | Médio. É a adaptação mais sentida: "ele espera eu estar mal" | 10 min |
| O que fazer: sorteio com pesos, anti-repetição, teto por categoria, exploração, alvo de intensidade | `Seletor.java`; `Diretor.java:1185` | Não. Lê como variedade. A subida de intensidade lê como escalada, não como adaptação | 10 min |
| Estados (calmo, observando, testando, escalando, ameaçando, recuando) e a trégua | `EstadoDiretor.java` | Só a trégua: é alívio de verdade, com a cor voltando | — |
| Cadeias: outro evento depois de uma reação | `Diretor.java:1704-1792` | Talvez. O elo não acontece no lugar do anterior | 10 min |
| Onde nasce: fora da tela, com cobertura, na penumbra, visível se ele virar | `Aparicao.java` | Médio: "atrás da árvore" parece escolha | 30 min |
| Lugares pessoais: a isca, o caminho (45% das presenças), a "rota" (12%), o lugar da morte, a janela, a porta, a borda da vela | `Diretor.java:1459-1502` | Lugar da morte, janela e porta: sim. Caminho: pouco. "Rota": não | 30 a 55 min |
| Cinco cenas compostas, com construção, pico e alívio | `Cena*.java` | Sim. "Cheguei em casa e a porta mexeu" é o momento mais bem ancorado do mod. O pico só existe da fase 3 em diante | 30 min (som); 55 min (com ele) |
| Sequência de ameaça: espreita, pausa, golpe | `Diretor.java:3034-3112` | Sim, se acontecer | 55 a 90 min |
| Na caçada: ir ao último lugar conhecido, olhar em volta, fechar o cerco, fingir que desistiu, atalho, atravessar, esperar na borda da vela, soprar a vela | `Cacada.java`; `HospedeBusca.java` | Em parte. O "pensar" só roda quando o jogador não vê | 90 min |
| Contra-adaptações: o sino passa a mentir, a isca é ignorada, a vela dura menos, ele sopra, atravessa mais depressa, bater deixa de funcionar | vários | Metade lê como "o item falhou" | 90 min |

**Não decide** `[código]`: para onde o jogador vai (não há previsão de destino); para o que olhar que não seja o jogador; um objetivo para a noite.

### 1.4 Fazer

| Capacidade | O jogador percebe? | Primeira vez |
|---|---|---|
| 33 eventos (25 sem criatura), 8 presságios, 7 perturbações, 12 cartas | Sim como coisas soltas. Poucas levam a algo (seção 2.4) | 2 min (presságio); 10 min (evento) |
| A criatura: seis modos. Fora da caçada e do Avesso é uma estátua virada para o jogador, que some ao ser vista. Toda mudança de lugar é teleporte fora da tela | Um relance | 30 min (vulto distante); 55 min (de perto) |
| A caçada: aviso, perseguição que só anda fora da tela, portas, atravessar, vela, linha, captura sem morte, marca | Sim. É o trecho mais legível do mod | 90 min |
| 33 sons próprios e os sons do jogo tocados de mentira | Sim. Nenhum é exclusivo dele (seção 2.6) | 2 min |
| Mundo: miragens só para o alvo, blocos temporários, porta de verdade, Boneco, Soleira, Casa do Vigia, Véu, Avesso | Sim para os físicos | 30 min em diante |
| Tela: cor, borda escura, neblina, piscada, trilha em camadas | Borda e batimento: sim. Cor e fundo: pouco | 10 min |
| Texto: 28 páginas, 10 sussurros, cinco frases com o nome do jogador | Sim | 10 min |

### 1.5 O que dá para afirmar do inventário

- As quatro tabelas têm 44 linhas, e várias juntam mais de uma capacidade. Em umas doze o jogador percebe com clareza, e quase todas são **lugares e objetos** (o Boneco, a linha gasta com pegada, a porta, a janela, o lugar da morte, a Casa do Vigia, o Avesso), **texto** (a fala do chat, a frase da cama) ou a **caçada**.
- Nenhuma das capacidades percebidas com clareza está na criatura quando ela aparece fora da caçada.
- O que o Diretor aprende sobre o jeito de jogar (pesos, perfil, exploração) não tem saída visível.

---

## 2. Diagnóstico

### 2.1 O que o log mostra, e o que eu suponho

Os dois trechos do arquivo: o antigo (07–08/10, 1h10, antes das correções de alcance de som e de leitura de reação, com comandos de teste no meio) e o de 09/10 (alpha12, 55 min, mundo novo, com estruturas geradas por comando e um teleporte).

`[log]`

- **A criatura apareceu por conta do Diretor três vezes em 2h06.** No trecho antigo houve 40 manifestações: 33 por comando, 4 chamadas pelo Olho e 3 do Diretor. Dessas três, um vulto que sumiu em 11 s sem entrar na tela, e duas dentro de uma mesma cena aos 71 minutos (as duas entraram na tela; uma existiu por 1 s e a outra por 7 s). No trecho de 09/10, nenhuma.
- **Nenhuma aparição do Diretor usou um lugar do jogador.** As três nasceram por geometria (`motivoPosicao=VULTO` e `CENA_CAMPO`).
- **Todas as linhas de sinal trazem `semCriatura=sim`:** 33 no trecho antigo e 7 no de 09/10. Nenhuma traz `nao`.
- **O Sino respondeu 16 vezes: 11 de um ponto do caminho do próprio jogador, 5 com silêncio.** Não havia criatura em nenhuma.
- **A porta de sempre e a cama foram aprendidas** por volta dos 32 minutos em 09/10. Nenhum evento usou a porta naquela sessão. O mais perto disso foi um eco de porta, com leitura de reação 0,10.
- **O perfil mal saiu do lugar.** Nos dois trechos os seis traços ficaram entre 28 e 66. O código só age acima de 65.
- **O Boneco andou três passos** (48, 38 e 29 blocos da cama) e a Casa do Vigia mudou depois da visita. Não há como saber pelo log se ele notou.
- **Leituras de reação:** 5 em 09/10 (2 claras, as duas no mesmo tipo de evento, a borda escura falsa). No trecho antigo, 34 leituras: 19 com confiança zero e 5 acima do limiar de escalar.

`[suponho]`

- Que os sons soaram "jogados", como o dono disse, porque nenhum deles levava a nada. O log mostra a falta de referente; a ligação com a impressão dele é minha.
- Que as duas reações claras de 09/10 (giros de 107° e 69° nos três segundos depois de um evento sem criatura) podem ser coincidência. Duas leituras não provam nada para nenhum lado.
- O trecho antigo não serve de evidência do que funciona: as reações são de antes da correção.

### 2.2 Inteligência que existe e não aparece

**a) Aprender pesos.** `[código]` O aprendizado é real: por evento e por categoria, longo e curto. `[suponho]` É invisível por aritmética: uma inclinação de 40% num sorteio de 10 a 20 candidatos, 10 a 30 vezes por hora, não se distingue de sorte. E a medida é ruidosa: parar três segundos para pôr uma tocha vale "congelou" (0,50), ligar a corrida seguindo em frente vale "fugiu da fonte" (0,52), e as duas passam do limiar que faz o Diretor escalar (0,45). A Bungie descreve ter jogado fora exatamente este tipo de variável (seção 3.1): difusa, com limiar e decaimento, impossível de o jogador imaginar.

**b) O perfil.** Seis traços, e nenhum produz algo que se veja. O efeito mais forte é mudar a faixa de ângulo de uma aparição de 55–80° para 56–70°.

**c) O Rastro e os Lugares.** O Rastro é bem usado, mas a escolha "um ponto por onde ele passou" e a escolha "um ponto atrás dele" dão quase no mesmo lugar. O mapa de lugares frequentados entra em 12% de um único evento, como o centro de um chunk.

**d) A caçada pensa sem plateia.** `[código]` Na tela ela congela, virada para a posição real do jogador, mesmo quando não sabe onde ele está (`Cacada.java:166, 198`). Ir ao último lugar conhecido, parar, varrer ±70° com a cabeça, fechar o cerco: tudo isso só roda fora da tela. E olhar para ela lhe entrega a posição com certeza total: quem espia de um esconderijo corrige o erro dela.

**e) As contra-adaptações.** O sino que passa a mentir e a isca que passa a ser ignorada leem como "o item não funcionou". A terceira pancada nela não tem som nem recuo: o jogador não recebe o sinal de que aquilo deixou de valer. Só a vela que dura menos, a vela soprada e a parede atravessada são legíveis, e as duas últimas vivem na fase 4.

**f) Ramos pelo jeito de jogar que não discriminam.** `[código]` "Olha muito para trás" conta qualquer giro de 120° num segundo, desde o primeiro minuto, sem decaimento; com 12 ele muda o evento para sempre (`Diretor.java:497-501, 1505`). `[suponho]` Todo jogador passa de 12 em minutos, então a variante "bem atrás de você" quase não existe.

### 2.3 A criatura, quando aparece

`[código]`

- Nasce já olhando para o jogador (`HospedeEntity.java:159`) e é virada para ele, corpo e cabeça juntos, a cada tick (`:691-699`). Não existe o momento "ela virou a cabeça para mim".
- Não reage a ser vista. Conta ticks e some: 0,35 s de olhar direto com ousadia zero.
- Não anda fora da caçada e do Avesso.
- A pose "antes de ser vista" e os olhos trocam no mesmo tick em que ela entra na tela, sem transição. `[suponho]` É trabalho de modelo que o jogador quase não vê.
- É uma entidade comum: num servidor, os amigos também a veem. Só o olhar do alvo conta e só os sons são dirigidos.

Uma estátua que já está olhando e some responde à pergunta "tem alguma coisa ali?". Não responde a "o que ela quer?". É a queixa mais votada contra o mod que mais investiu em aprendizado: "o que ele faz além de olhar?" (seção 3.3).

### 2.4 Eventos: quais têm referente

"Referente" quer dizer: o que o jogador percebeu aponta para algo que existe e que ele pode conferir.

| Classe | Eventos | Quantos |
|---|---|---|
| **Real**: ele está lá, ou fica uma consequência que se confere | `VULTO`, `PRESENCA`, `ATRAS`, `TUMULO`, `ESPREITA`, `CACA`, `ESPERA`, `VEU` (em metade), `PORTA` (a porta muda de verdade), `VISTO` | 10 |
| **Pessoal, sem ninguém**: usa uma ação, o caminho ou a porta do jogador, e não há nada lá | `ECO`, `RUIDO_RETORNO`, `SEGUIDOR`, `PEGADAS`, `VESTIGIO`, `TRILHA_INTERROMPIDA`, `SINAL`, `BATIDA`, `ECO_CHAT`, `CANTIGA`, `OBJETO_FORA_LUGAR` | 11 |
| **Ruído solto** | `PASSOS`, `PASSO_UNICO`, `PASSAGEM`, `ANIMAIS`, `LUZ_ERRADA`, `SINAL_DISTANTE`, `ECO_PASSOS`, `SUSSURRO`, `NEBLINA`, `TOCHA` | 10 |
| **Mente de propósito** | `VIGIA`, `PRENUNCIO` | 2 |

Presságios e perturbações (15 tipos): nenhum cria criatura; cinco usam o caminho, a ação ou a porta do jogador; o resto é solto.

Três observações:

1. **Dos dez "reais", só dois existem antes dos 55 minutos:** o vulto (de dia, por menos de um segundo) e a porta que muda, os dois a partir dos 30. Antes disso tudo é das outras três classes.
2. **Os dois que mentem chegam antes da verdade.** A borda escura falsa (`VIGIA`) existe desde a fase 1, quando a verdadeira é impossível. O jogador aprende que a borda não quer dizer nada antes de ela querer dizer alguma coisa. O crítico que formulou a regra (seção 3.2) põe a ordem ao contrário: primeiro treinar o sinal como honesto, depois torná-lo inconsistente.
3. **Os "pessoais sem ninguém" são a melhor matéria-prima do mod.** O som da sua picareta vindo de onde você minerou é a página 3 do diário. Falta só haver alguém lá.

**O que falta para haver construção, pico e alívio dentro de uma sessão.**

- **O pico não cabe.** `[código]` O que é forte exige fase 3, o Diretor escalando e V de 55 ou mais (na prática, escuro). A caçada nunca é sorteada: só vem pela sequência de ameaça, pela dívida ou pela vela.
- **A pressão não fecha o ciclo.** `[suponho]` Com o respiro da alpha13, a queda de 0,4 por segundo come de 12 a 30 pontos entre uma saída e a seguinte, e a pressão quase nunca chega aos 75 que fazem o Diretor recuar. O ciclo virou "quinze minutos escalando e trégua", sem pico no meio.
- **Não existe "sessão".** O estado do Diretor zera ao reabrir o mundo, e nada pergunta "o que acontece hoje?".
- **As cenas têm a forma certa**, mas a primeira com criatura está aos 55 minutos, duas delas dependem de sorteio de 55 a 62%, e uma cena que não acha lugar gasta a vez e o intervalo.

### 2.5 Itens: os verbos do jogador

Princípio da análise de 09/10, que eu mantenho: esconder as regras do monstro, nunca os verbos do jogador.

| Item | Verbo | Sobrepõe com | Como se aprende hoje | Situação |
|---|---|---|---|---|
| Vela Pálida | Afastar | Linha, Tigela | Dica, página 5, mensagem ao acender | Clara. Mas a cor do mundo, que é "o sinal de que ela funciona", não volta na fase 2 (Apêndice D) |
| Olho Sussurrante | Procurar | Sino, Lampião, Fio | Dica, página 6, texto com direção e distância | Claro |
| Página, Caderno | Ler, conferir | — | O próprio texto | Claros |
| Sino Oco | Perguntar | Olho, Lampião, Fio | Página 11. Sem texto de resposta. O toque não soa como sino | Fraco. Antes da fase 3 só devolve o caminho do próprio jogador ou silêncio |
| Fio de Vigília | Confirmar | Lampião | Dica boa, página 12. Expira sem aviso | Sem resposta antes da fase 3 |
| Isca Pálida | Chamar | Olho, Ossos, Soleira | Dica boa, página 15. Expira sem aviso | Quase nunca responde: depende de um sorteio em dois minutos |
| Lampião Pálido | Vigiar um lugar | Olho, Sino, Fio | **Nenhum texto.** Apagado por ele e apagado por falta de cinza são o mesmo estado | Ilegível. A receita pede resina |
| Linha de Cinza | Barrar | Vela | Página **26** | Legível depois de vista gasta |
| Tigela de Oferenda | Negociar | Vela, Linha | Página **27**. Resultado só de manhã | Legível em parte |
| Caixa de Música | Acalmar, despistar | Vela, Ossos | Página **28**, a última | A música é clara. O que ela faz, não |
| Ossos de Agouro | Tirar a sorte | Olho, Isca | **Nenhum texto.** Sete padrões de queda sem legenda | Ilegível. E pode chamar a criatura em qualquer fase |

- **Sete coisas respondem "ele está perto?"**: Olho, Sino, Fio, Lampião, Caixa, Ossos e a borda da tela. Três delas mentem por regra.
- **A ordem está invertida.** As páginas saem em ordem fixa, e as que explicam Linha, Tigela e Caixa são as três últimas. Os itens chegam entre 10 e 30 minutos.
- **Ferramenta antes da ameaça.** Antes da fase 3 o jogador pode ter tudo. Fio, Lampião e Isca não têm o que detectar.
- **A Conta não tem recibo.** A cobrança vem de 30 a 120 s depois, de propósito "quando ele já tiver esquecido o que fez". Quatro das nove cobranças só acontecem no uso seguinte, e a da Isca é invisível. `[log]` Em 09/10, 12 das 27 coisas que o jogador percebeu vieram da Conta.

### 2.6 Som: o que cada som promete

`[código]`

- **Não existe som que seja só dele.** Do corpo da criatura saem `pano`, `grave`, `arrasto`, `respiracao` e passos. Todos servem também a alarme falso, e `pano`, `grave` e `estalo` servem ainda aos próprios itens do jogador.
- **`grave` tem dois sentidos opostos:** "você usou o Olho ou o Sino" e "a caçada vai começar".
- **O tema é apresentado como objeto do jogador.** A cantiga só existe na Caixa. Vira "dele" depois de três toques. Quem nunca dá corda nunca ouve o assobio nem o cantarolar. A dica da Caixa diz "a música você já ouviu em algum lugar", e não ouviu.
- **O assobio nunca vem de onde ele está.** Sai de um ponto sorteado a 24–42 blocos (`Diretor.java:2157-2165`), inclusive no aviso verdadeiro da caçada, em que ele nasce atrás do jogador.
- **O único som de voz honesto é o cantarolar**, e ele é sem direção.
- **O batimento denuncia o aviso verdadeiro.** No aviso real a caçada manda 0,2 e o batimento começa em 0,2; no aviso falso o valor é 0,15 (`Cacada.java:141`, `Sentidos.java:63-65`, `TrilhaCliente.java:89`). A intenção escrita é que os dois fossem iguais.
- **Há quatro famílias de voz ou sopro humano sintetizados** (`sussurro_voz`, `cantarolar`, `assobio`, `chamado`) e cinco sons registrados que nunca tocam (`chamado`, `vento_oco`, `ranger`, `giz`, `sonho`).

Promessas cumpridas hoje: o fio que range e sobe de tom (ele está chegando), o arrasto na linha (algo forçou), o arranhar antes de atravessar (saia de perto), o batimento (quão perto), o sopro na vela. São todos da caçada ou de um item armado. Fora disso, um som do Sussurros não promete nada.

### 2.7 Arco: as três primeiras horas e as duas semanas

**Três horas, hoje** (nominal, `[código]`; a coluna do log é da sessão de 09/10):

| Quando | O que o jogador vê, ouve e entende | No log |
|---|---|---|
| 0–10 min | Dois ou três presságios soltos. Já pode achar itens do mod em baús do jogo | 2 presságios |
| 10 min | Quatro passos atrás, uma página no chão. Começam os eventos fracos, quase todos som sem dono | 1 evento |
| 30 min | A Caixa e outra página. O vulto distante fica possível, de dia. Soleira, Casa do Vigia e Boneco podem surgir. Cenas, só com som | Casa do Vigia visitada aos 30 min; meia dúzia de itens na mão |
| 55 min | O Olho. Ele de perto, no escuro, com o Diretor escalando. As cenas com ele. O Véu | Fase 3 aos 39 min; nenhuma aparição até os 55 |
| 90 min | Primeira caçada possível | não chegou |
| 90–180 min | Caçadas a cada 25–40 min; a ousadia começa a aparecer | — |

Na primeira hora o jogador entende: "há sons, há itens, alguém escreveu um diário". Não entende o que os itens fazem nem viu do que o diário fala.

**Duas semanas, hoje.** `[código]` O relógio das fases se gasta inteiro na primeira hora e meia. Depois dele o mod fica na fase 4 para sempre, e o que ainda muda são sistemas lentos e soltos entre si: uma carta do baralho a cada 40–90 minutos (três em doze são "nada"), o Boneco em ciclos de cinco a oito dias, o Avesso a cada três dias, a ousadia. Não há fim. O texto promete um ("falta pouco para eu ser você", página 9: "ele quer saber como é ser eu") e nada no comportamento mostra esse "pouco" diminuindo.

**Uma coisa que não combina.** No Avesso, a cópia da casa do jogador "erra mais a cada visita": faltam blocos, uma fatia se repete. Se o Hóspede estuda o jogador para ser ele, a cópia dele deveria ficar **mais certa** com o tempo, e o estranho deveria ser o quanto ela acerta. Hoje a imagem diz o contrário do texto.

### 2.8 O que está certo e não deve mudar

- Nunca alto, nunca matar, nunca estragar a construção. A pesquisa confirma de três lados: o modo sem morte do SOMA funciona enquanto a criatura continua hostil; a morte e a repetição são o que mais transforma medo em rotina; e os mods novos anunciam "não mexe nos seus blocos" como recurso.
- Som e miragem só para o alvo.
- Os lugares e objetos que repetem: o Boneco, a linha gasta com a pegada do lado de fora, a Casa do Vigia que muda. São os melhores exemplos de intenção do mod, e todos seguem a mesma fórmula: uma coisa do jogador, uma mudança feita fora da vista, repetida com variação.
- A cena da volta para casa e a da janela.
- O relance que o dono aprovou. A seção 3.5 trata da tensão entre ele e a legibilidade.
- A trégua de verdade depois do pico.

---

## 3. Achados da pesquisa externa

Cada linha traz a fonte e a marca. Os relatórios completos dos subagentes, com as citações, não foram para o repositório; o que importa deles está aqui.

### 3.1 IA clássica de jogos

| Referência | O que faz o jogador sentir intenção | O que cabe aqui | O que não cabe | Marca |
|---|---|---|---|---|
| **Alien: Isolation.** Tommy Thompson, "The Perfect Organism" e "Revisiting the AI of Alien: Isolation" (gamedeveloper.com); reportagem da MCV com falas dos designers | Dois cérebros: o diretor sabe onde o jogador está, o alien tem de descobrir, e o diretor só aponta a região. Nos dutos o alien continua simulado ("it's not a fake out: the alien is actually still moving around") e os sons vêm de onde ele está. Comportamentos destravam depois que o jogador repete uma tática (armários, dutos, lança-chamas): "We say it learns, but that's not quite true". Regra: nenhum destrave vem de algo que o jogador não consiga ligar a uma ação sua. A busca volta atrás de propósito, para parecer dúvida. "The fact that the head turns and looks at you was a massive thing for us" | Quase tudo. Diretor que sabe e criatura que não sabe. **Ele existir em algum lugar o tempo todo.** Destrave por tática repetida. A cabeça | No Alien o jogador nota o comportamento novo porque morre. Sem morte, o destrave precisa de outra consequência visível. Árvore de cem nós é escopo de estúdio | PROVÁVEL para o que a Creative Assembly disse (são relatos de palestras em vídeo); VERIFICADO para os artigos |
| **Left 4 Dead.** Michael Booth, "The AI Systems of Left 4 Dead" (2009) | Pouca coisa, e esse é o achado: o diretor dá ritmo, não um alguém. Ciclo: construção, pico sustentado (3–5 s), saída do pico, descanso (30–45 s). "Algorithm adjusts pacing, not difficulty". "Not purely random, nor deterministically uniform" | O ciclo de quatro tempos. A "saída do pico": não começar o descanso antes de a cena acabar | A intensidade é medida por dano. As escalas de tempo são de jogo de tiro | VERIFICADO |
| **F.E.A.R.** Jeff Orkin, "Three States and a Plan" (GDC 2006) e "Combat Dialogue in F.E.A.R." (Game AI Pro 2) | Os soldados não se coordenavam; as falas é que faziam o jogador ler um plano. "If the AI didn't say it, it didn't happen". O pedido de reforços nunca foi programado: os inimigos seguintes foram lidos como os reforços. Uma fala explica a inação ("I've got nowhere to go!") | A regra: nenhuma decisão dele sem um sinal que se observe. O truque do reforço: um sinal, depois algo que ia acontecer de qualquer jeito, e o jogador liga os dois. Mostrar que ele quis e não pôde | Falas são verbais, altas e pedem dois. O planejador (GOAP) é exagero para uma criatura com poucas ações | VERIFICADO |
| **Halo.** Butcher e Griesemer, "The Illusion of Intelligence" (GDC 2002), com as notas de fala; Damian Isla, "Handling Complexity in the Halo 2 AI" (2005) | Com inimigos mais resistentes, "muito inteligente" subiu de 8% para 43% (mudaram vida e dano juntos; o teste não isola a duração). "If it isn't totally obvious, it's too subtle". Jogaram fora variáveis difusas de emoção por serem impossíveis de imaginar. "Unpredictable does not mean random". Uma pausa posta de propósito foi lida como defeito | **O alerta direto para um Diretor por pesos.** Tempo de exposição: quem some na hora não chega a parecer nada. Poder ser enganado prova que havia uma crença | Toda a legibilidade de Halo é alta e exagerada. O teste mede um jogo de tiro | VERIFICADO |
| **Sistema Nemesis.** Reportagens com falas de Michael de Plater (DICE 2015) e Chris Hoge (GDC 2018); duas entrevistas | A memória é exibida, não deduzida: o orc cita o último encontro, carrega a cicatriz e ganha um título. Poucas "âncoras": só os momentos que o jogador também lembra. Raro de propósito. Uma fraqueza fixa que nunca some. O post-mortem conta que cortaram barras e facções | Âncoras: gastar a memória dele em três ou quatro momentos que o jogador lembra com certeza. A marca que aponta de volta para o jogador. A fraqueza fixa | Apoia-se em morte, falas dubladas, muitos indivíduos e uma tela que mostra a hierarquia | PROVÁVEL (não há palestra de de Plater na GDC 2015; a de 2015 é da DICE) |
| **Percepção de inteligência.** Steve Rabin, "The Illusion of Intelligence" (Game AI Pro 3); Kevin Dill, "What Is Game AI?"; Soren Johnson, "Our Cheatin' Hearts"; Mick West, "Intelligent Mistakes"; Tynan Sylvester, "The Simulation Dream"; Lankoski e Björk (DiGRA 2007) | O jogador quer acreditar. Olhar um segundo antes de escolher "can telegraph deep intelligence". Criaturas inteligentes hesitam. "AI characters should have a reason to exist beyond the player". "Anything in the Game Model that doesn't copy into the Player Model is worthless". Acaso contra o jogador é lido como intenção ou trapaça | O olhar é a alavanca mais barata e a mais citada. Agenda própria. Coincidência ruim precisa de sinal antes | Personalidade forte pede animação e voz | VERIFICADO |
| **Dois experimentos.** Takayama, Dooley e Ju (HRI 2011, 273 pessoas); Denisova e Cairns (CHI PLAY 2015) | Mostrar premeditação deixou as pessoas mais seguras da leitura, mas não fez o robô parecer mais esperto. **Reagir ao resultado fez** (4,72 contra 3,86), inclusive quando ele falhava. No outro: dizer que havia IA adaptativa fez todos os participantes "verem" adaptação que não existia | Quando o jogador frustra o Hóspede, mostrar que ele percebeu vale mais do que acertar | Robô em vídeo e amostras pequenas de estudantes; não é terror | VERIFICADO |

### 3.2 Jogos de terror e som

| Referência | O que faz o jogador sentir intenção | O que cabe aqui | O que não cabe | Marca |
|---|---|---|---|---|
| **SOMA e Amnesia.** Thomas Grip: entrevista a Kris Graft (2015), "4-Layers", "The SSM Framework", "The Complexity Fallacy" (espelhos em gamedeveloper.com); Fredrik Olsson sobre The Bunker (GamingBolt) | "If it's too mechanical, it's a very uninteresting, systemic feel. And if it's too random, monster interactions will be a haze". A criatura migra da história para o sistema conforme o jogador aprende. "If all of that system was replaced with a random number generator, the player wouldn't notice". Olsson: lógica clara ligada às ações do jogador, mais incerteza, variação ou escalada | Duas ou três regras que o jogador consegue dizer em voz alta, e uma coisa que muda. O tema "ele aprende" já dá a mudança | Caçada sistêmica como centro: em duas semanas o jogador "dança em volta" | VERIFICADO (pelos espelhos; o blog original não abriu) |
| **O modo sem morte do SOMA.** Game Informer, GameCritics, The Ringer | Monstro que não mata continua assustando enquanto for hostil se o jogador abusar. Um resenhista ainda se escondia por instinto. Outro nota que as criaturas "rarely reacted" e viravam obstáculo | É o precedente direto do "nunca matar". A condição: **sem morte, reação é obrigatória** | — | VERIFICADO |
| **Phasmophobia.** Textos do diário do jogo (via guia); tópico da Steam; respostas da Kinetic Games | A regra é contada como temperamento ("a Shade is known to be very shy"). A entidade ouve o que o jogador diz. Depois de dominada: "the stronger a grasp you have on mechanics, the less scary it is" | Poucos traços que só se descobrem observando, nunca escritos. Escalar tornando-o mais característico, não mais frequente | Catálogo de tipos e lista de evidências: empurra para a postura de detetive | VERIFICADO (wikis bloqueadas; guia secundário) |
| **Mímica de voz.** Skinwalkers e Mirage (mods de Lethal Company), MIMESIS, R.E.P.O. (por mod), Murky Divers | Quase todos são gravação repetida, sem síntese. Sem contexto vira piada ("Oooh I found a steering wheel"). **O caso que mais enganou foi um som não verbal**: o teclado mecânico do amigo. Diretor do MIMESIS: dois segundos de ilusão criam uma desconfiança longa. No Mirage cada inimigo imita sempre o mesmo jogador | Imitar som não verbal (passos, porta, mineração) no lugar e na hora plausíveis. O sósia de dois segundos. Ser um jogador só por vez | Gravar microfone: privacidade, e falha como piada. Voz sintetizada: nenhuma fonte mostra funcionando | VERIFICADO (MIMESIS por entrevista traduzida) |
| **Darkwood.** Acid Wizard no PlayStation Blog; Wikipedia | Mostra o efeito e esconde o autor: a porta abre, o móvel se mexe. À noite a ordem dos sons (longe, perto, na porta) lê como propósito | Efeito sem autor com o que o jogo já faz e desfaz. A ordem de aproximação | A invasão que quebra a barricada e mata | VERIFICADO em parte ("habitantes que observam": nenhuma fonte) |
| **Som como promessa.** Garner e Grimshaw, "A Climate of Fear" (2011, lido em texto cru); equipe de áudio do Alien: Isolation (MCV); Naughty Dog sobre o Clicker; Celia Wagar, "Making Good Horror" (crítica) | Treinar o sinal como honesto e depois torná-lo inconsistente. O aviso deve conotar ameaça sem dar tamanho, posição nem velocidade. O Clicker tem voz com estados, tratada "like dialog of a character", feita com sons humanos. "A sound that plays a second or two late can mean the difference between abject horror and catastrophic comedy" | Uma assinatura com dois ou três estados. Honesta no começo. A traição justa é **o sinal sem a criatura**, quase nunca a criatura sem o sinal | Sinal sempre honesto vira radar; sempre mentiroso vira ruído | VERIFICADO (Wagar é opinião) |
| **Voz gravada contra sintetizada.** Randy Thom, "Designing Creature Voices"; Diel e Lewis (2024); a voz invertida de Twin Peaks | Thom: voz humana "animalizada" por software soa "inorganic, synthetic/electronic". A estranheza boa vem de voz orgânica desviada (invertida, mais grave), não de voz sintética | Síntese para o que não é voz. Para o que deve soar como alguém, gravação real processada | Fala sintetizada em frases | VERIFICADO (Thom); PROVÁVEL (Diel e Lewis, só o resumo) |
| **Voz sem corpo** (Michel Chion, "acusmêtre") | A voz ouvida sem corpo parece ver e saber tudo; mostrar o corpo tira esse poder | Corpo visível, mudo. A voz vem de onde não há ninguém | — | PROVÁVEL (só resumos) |
| **Entre amigos.** The Blackout Club (80.lv); Mirage contra Skinwalkers | A paranoia nasce do relato que ninguém confirma. Sincronizar tudo dá história compartilhada e desfaz a dúvida | O Sussurros já tem a parte difícil (só o alvo percebe). O compartilhado deve ser raro e físico | Disfarce que faça alguém perder item ou construção por confiar | VERIFICADO |

### 3.3 Mods de terror de Minecraft

Nenhum mod lido usa "IA": todos são contadores, limiares e sorteio com pesos. A diferença entre eles está no que **mostram**.

| Mod | O que faz o jogador sentir intenção | O que cabe aqui | Como falha | Marca |
|---|---|---|---|---|
| **Don't Let It Learn** (Forge 1.20.1; a criatura se chama The Hollow) | O aprendizado é real: rota em células de 8 blocos, vale depois de 3 passagens, confiança mínima de 0,55. O que funcionou foi o que se vê: ele **olha para alvos** da casa, um de cada vez (janelas, portas, baús, a cama), e reage ao olhar do jogador **em degraus** (cerca de 2 s e vira a cabeça; 6 s e avisa; 10 s e reage, com cinco reações sorteadas e penalidade de repetição). Espera à frente na rota aprendida e erra: "the prediction is not always correct" | Olhar com alvo. Degraus. Reação com memória. Errar a previsão | Os comentários mais votados: pedido de documentação (601 pontos) e "What does it do? Does it just watch you?" (280). **Aprender de verdade não bastou** | VERIFICADO (constantes lidas no jar; a lógica não foi descompilada; 100 de 201 comentários) |
| **Araz (The Anomaly)** (Fabric; sem versão para a 26.2) | "Aprender" é contar, e a conta é mostrada: uma placa perto da cama com o número verdadeiro ("14 TIMES / YOU OPENED IT"), e "você fez X, ele aprendeu Y". Mostra-se uma vez nos primeiros minutos do mundo. Antes dele, o mundo cala | O recibo com um fato que se confere. O primeiro contato garantido | Mata e vira jogo de progressão (o oposto daqui) | VERIFICADO (jar e página); recepção não encontrada |
| **From The Fog** (até a 26.2) | Atrás de vidro ele não some: fica olhando. Visto de longe, inclina a cabeça e demora 3 s (1%). Escorrega para trás do bloco ao lado. Uma das 12 placas usa o nome do jogador. O cão domado encara e rosna | Ficar olhando pela janela. A inclinação de longe. O cão | Apaga tochas e folhas de vez. A página promete placas que comentam o que você fez; o arquivo tem 12 frases fixas | VERIFICADO (código) |
| **The Man From The Fog** | Olhar é uma aposta (some ou ataca); não olhar garante que chega mais perto | A regra simples que dá peso ao olhar | O autor foi removendo sistemas: sanidade, neblina, dimensão | VERIFICADO (changelog) |
| **Cave Dweller** e variantes | O som tem dono e cresce com a proximidade; só some se você não estiver olhando | Som com dono | As variantes novas não têm IA nova; uma anuncia "sistema de progressão" | VERIFICADO (dependências; código lido na pesquisa de 08/10) |
| **The Hollow** (`hollow-dread`, Fabric 26.2) | Não alega aprender. "While you can see it, it cannot move", e congela também a animação. Desenhado sem luz: silhueta mesmo ao lado da tocha. Bater nele diz onde você está. Pega sem matar | Congelar a animação quando olhado. A silhueta sem luz. Usa o modelo humanoide do jogo, sem GeckoLib | "If you play it safe, you will never see this mod" | VERIFICADO (jar) |
| **The Mimicer** | Não imita o jogador: a página do autor diz que é o Cave Dweller com outra aparência e falas gravadas | Nada | — | VERIFICADO (página) |
| **Existence** | Sósia com a pele do amigo, só para o alvo, sempre três blocos atrás do olhar; **espelha** o agachar, o braço e a direção do olhar | Espelhar é barato. O sósia | Apaga tochas de vez | VERIFICADO (código) |
| **The Obsessed** | Repete uma em cada dez mensagens de chat. Primeiro contato forçado aos 60 minutos se não houver antes. A armadilha dele se desfaz sozinha em cinco minutos | O contato forçado | O topo da página diz "mind of its own"; o fim diz que o aprendizado é "future update" | VERIFICADO (página) |
| **The Knocker** (13 milhões de downloads) | "Sabe onde você mora"; aparece com tocha na mão, como um jogador. Modelo humanoide do jogo | A tocha na mão | O autor removeu cinco eventos, e alguém fez um mod só para limitar as placas dele | VERIFICADO (changelog e jar) |
| **Understudy, LocalHost, TheWatcher** | Um vulto refaz o seu caminho de ontem. Notas em que "everything on them is true". Frases que citam um fato ("did you move the bed"). **Um passo a mais** depois que você para. A última porta que você usou fecha quando você se afasta. "Do something you have never done in front of it, and it forgets its lines" | O passo a mais. O fato conferível. Fazer algo novo o desarma | Largura enorme e nenhuma opinião de jogador | VERIFICADO só a estrutura (TheWatcher: código) |

Padrão: quanto mais abstrata a palavra da página ("learns", "adaptive AI", "mind of its own"), menos há por trás. E os autores que duram cortam eventos.

### 3.4 Atribuição de mente e leitura visual

| Achado | Fonte | O que vira aqui | Marca |
|---|---|---|---|
| **Apontar basta.** Formas com movimento aleatório que só apontam para o alvo são lidas como perseguidoras; viradas a 90°, o efeito some | Gao, McCarthy e Scholl (2010), "The wolfpack effect" | Corpo e cabeça virados para o jogador já dizem "está atrás de mim". De lado, a leitura some: serve para quando ele **não** deve parecer estar caçando | VERIFICADO (página do laboratório) |
| **Dois regimes de aproximação.** Desvio de até 30° da linha reta: a perseguição é vista na hora. Entre 90° e 120°: o perseguidor chega perto sem ser notado | Gao, Newman e Scholl (2009), "The psychophysics of chasing" | Espreita de lado; caçada de frente | VERIFICADO |
| **Contingência basta, mesmo sem rosto.** Bebês tratam como agente um objeto sem olhos que respondeu a eles | Watson (1972) e Johnson, Slaughter e Carey (1998), por fontes secundárias | Uma reação por avistamento, logo depois de uma ação do jogador | VERIFICADO (secundária) |
| **Tempo de reação.** 0,2 s no mínimo; 0,4 s quando há comparação | Rabin (2017) | Reação de 0,3 a 0,8 s depois. Instantânea parece máquina | VERIFICADO |
| **Na dúvida, o olhar é "para mim".** À noite ou com ruído, as pessoas assumem que estão sendo olhadas | Mareschal, Calder e Clifford (2013) | Silhueta distante voltada para o jogador já funciona, sem olhos | VERIFICADO |
| **Justiça: só sabe o que viu; mostra que sabe antes de usar.** "The player seen by an opponent they do not see often feels cheated" | Tom Leonard (Thief); Thompson; contraexemplos Hello Neighbor e The Thing (2002) | Só hábitos acumulados, nunca a posição exata. Nunca usar o que sabe para prender | VERIFICADO |
| **Personalidade sai da regra de mira.** Os quatro fantasmas usam a mesma navegação; só o alvo muda. Um mira quatro casas à frente e é lido como emboscada. Ataques em ondas | Jamey Pittman, "The Pac-Man Dossier"; Craig Reynolds (1999) | Mirar onde o jogador vai estar. Recuo depois de cada aparição | VERIFICADO |
| **Conhecimento limitado.** "If the player was not perceived by any NPCs, his location was never updated" | Travis McIntosh, The Last of Us (Game AI Pro 2) | Ele procura no último lugar visto, à vista | VERIFICADO |
| **Objetivo, arco e fim.** A SA-X imita a protagonista; os primeiros encontros são cenas em que ela passa e não te vê; o arco fecha com reabsorção. No ECHO as cópias só fazem o que você fez | Wikipedia e Dread Central | Primeiros encontros em que ele está ocupado com outra coisa. Ele só faz o que o jogador fez. Um fim que não seja mais uma perseguição | VERIFICADO (secundária) |
| **Mensagem ambiental.** Lê-se como "alguém fez isso para mim" quando é não aleatória, no ponto de atenção habitual, feita fora da vista e repetida com variação. Fora do ponto de atenção, a cegueira à mudança engole | Smith e Worch (GDC 2010); Silent Hill 4; P.T.; síntese do subagente | Uma mudança por visita, na porta, na cama, no baú | PROVÁVEL (a síntese é dele) |
| **Vale da estranheza.** O movimento amplia; tempo errado sozinho já perturba; o estranho vem de uma parte que destoa | Mori (1970); Chattopadhyay e MacDorman (2016) | Um defeito de cada vez; tempo errado antes de proporção errada | ESPECULAÇÃO quando aplicado a um boneco de blocos |

**Três ressalvas que mudam decisões.**

- **Clima não faz ninguém ver coisas.** O único teste direto lido (Maij e colegas, 2019, seis experimentos) não achou que ameaça aumente a detecção falsa de agência. O "será que eu vi?" precisa de avistamentos reais.
- **"Errar aumenta a inteligência percebida" não tem apoio experimental direto.** Mirnig e colegas (2017) acharam mais simpatia, sem diferença em inteligência. O que as fontes de jogos sustentam é que erro visível e explicável dá **credibilidade e justiça**. O que tem apoio para "parecer esperto" é reagir ao resultado (Takayama).
- **O efeito dos "olhos que observam" tem replicações falhas.** Não usar como base.

### 3.5 Duas tensões que a pesquisa não resolve

**Sutil contra legível.** Halo diz "se não é óbvio, é sutil demais". O dono aprovou o relance. As duas coisas são verdade. A saída que eu proponho: separar **presença** de **mente**. O relance continua sendo a prova de presença. A mente aparece por outros canais, que não pedem olhar demorado: o que ele estava fazendo quando foi visto, o que ele deixou, o que ele respondeu. E, raramente, de longe, um ou dois segundos a mais. Essa última parte mexe num tempo que o dono aprovou, então vira pergunta (seção 7).

**Pesos contra causa e efeito.** Nenhuma fonte lida usa pesos aprendidos para escolher o comportamento de algo que deve parecer intencional. Halo descartou a ideia; Left 4 Dead usa um número difuso, mas só para ritmo. A saída: os pesos continuam decidindo **quando** e **quanto**. O **quê** e o **onde** passam a vir de fatos discretos sobre o jogador, que dá para mostrar.

---

## 4. Propostas

Quinze, em ordem de valor. "Ajustar" e "reordenar" mexem no que existe; "construir" é coisa nova. Custo: pequeno (um arquivo, um ou dois dias de trabalho com testes), médio, grande.

Valem para todas: nunca alto, nunca matar, nunca estragar a construção; som e criatura só para o alvo; ele só "sabe" o que poderia ter visto; nenhuma delas pune o jogador com o que ele sabe.

### 1. Ele nota (ajustar; pequeno)

**O que o jogador percebe.** Quando ele o vê, o Hóspede não está olhando para ele. Está virado para **uma coisa dele**: a porta de sempre, a cama, o buraco que ele acabou de cavar, o lugar onde ele estava há dois minutos. Um instante depois de entrar na tela, a cabeça vira para o jogador (o corpo, não) e ele some como hoje. Com a ousadia alta, em vez de sempre sumir, ele sorteia entre sumir, mudar de lugar e continuar olhando, ou inclinar a cabeça e ficar três segundos (só de longe), sem repetir a última.

**Por que parece inteligente.** Junta os três achados mais repetidos: atenção com alvo (Don't Let It Learn, lição 1 dos mods), contingência com atraso humano (Watson, Rabin) e a cabeça que vira (Alien: Isolation). Dá a ele uma agenda própria: estava fazendo outra coisa. E transforma o sumiço, que hoje é uma regra, numa reação. É a dosagem da SA-X: nos primeiros encontros ela não te vê.

**Reaproveita.** `HospedeEntity` (o `olharPara` e o controle de olhar), `Percepcao` (o instante em que entra na tela), a porta de sempre, a cama, as ações recentes, o Rastro. `[código]` O modelo já gira a cabeça separada do corpo (`HospedeModel.java:96`); hoje o servidor é que escreve corpo e cabeça iguais. Não há trabalho de modelo.

**Risco.** Com 0,35 s de tela, a virada pode não dar para ver. Por isso a pergunta 1 da seção 7. Sem a resposta, usar só a mais de 20 blocos e dentro do tempo de hoje. Outro risco: virado para outra coisa ele deixa de parecer que observa. É de propósito (ele estuda, não caça), e ao notar ele aponta para o jogador. A conferir: se o controle de rotação de corpo do próprio jogo puxa o corpo de volta.

**Como testar sem o dono.** GameTest com `JogadorDeTeste`: criatura com alvo de atenção; antes de entrar na tela, a cabeça não aponta para o jogador; N ticks depois, aponta, e o corpo não mudou. Foto no job do GitHub. Linhas novas: `HOSPEDE ... atencao=PORTA|CAMA|ACAO|RASTRO` e `NOTOU atraso=8t`. Métrica no `analisar.py`: quantas aparições tiveram alvo de atenção.

**Quando.** No primeiro contato (15–25 min) e em toda aparição de observação.

### 2. Ele responde na hora (ajustar; pequeno)

**O que o jogador percebe.** Quatro respostas pequenas, de 0,3 a 0,8 s depois de uma ação dele:

- ele para de andar e o eco dos passos dá **um passo a mais**;
- ele vira para o lugar de um som e, logo depois, um passo mais baixo vem de três a cinco blocos **além** dali (a fonte recuou);
- ele vai até o lugar do som e acha uma marca no chão;
- os passos que seguem o caminho dele param quando ele para e voltam quando ele anda.

**Por que parece inteligente.** Contingência é a pista mais barata de "isso me percebeu", e a pesquisa de mods traz o passo a mais como um dos cinco truques de melhor custo. Responde à condição do SOMA: sem morte, reação é obrigatória. E dá destino à `Leitura`, que hoje mede tudo isso só para mexer em peso.

**Reaproveita.** `Leitura` (olhou, investigou, congelou), as cadeias do Diretor, `ECO_PASSOS` e `EcoDePasso` no cliente, `SEGUIDOR`, a pegada de cinza.

**Risco.** A leitura é ruidosa: um giro por acaso dispara um passo que recua. É inofensivo (um som baixo, uma vez por evento). O risco real é virar tique: limitar a uma resposta por evento e não responder em todas.

**Como testar.** GameTest: dispara `PASSOS`, vira o jogador para a fonte um segundo depois, e confere que um segundo som chegou em 6 a 16 ticks, mais longe na mesma direção (o `JogadorDeTeste.sonsRecebidos` precisa passar a guardar a posição). Linha `RESPOSTA gatilho=OLHOU atraso=9t`. Métrica: fração das reações claras que tiveram resposta em até um segundo.

**Quando.** Fase 1 (10 min).

### 3. Ele está em algum lugar (construir; médio)

**O que o jogador percebe.** Os sons deixam de vir de qualquer lado. Vêm de um lugar, e esse lugar anda: primeiro longe, no caminho por onde ele passou; depois mais perto; depois do outro lado da porta. O Sino, o Lampião, os bichos e as chamas pálidas concordam entre si sobre onde ele está. A aparição acontece onde o último som tocou. Depois de visto, ele se afasta, e o som seguinte vem daquele lado, mais longe.

**O que é.** Um ponto por jogador, o **paradeiro**: onde o Hóspede está agora, mesmo sem corpo. Uma vez por segundo ele anda (no máximo o passo de uma pessoa) para um destino dado pela intenção do ciclo (proposta 5). Todo som que é "dele", toda marca e toda aparição tiram a posição desse ponto. Quando o Diretor recua, o ponto vai para longe. Não é uma entidade e não tem navegação: é uma posição com três estados (longe, rondando, perto).

**Por que parece inteligente.** É o "backstage" do Alien: Isolation: ele continua existindo quando não é visto, e os sons vêm de onde ele está. É a ordem de aproximação do Darkwood. É o truque do reforço do F.E.A.R.: um som, e depois algo naquele lugar, e o jogador liga os dois. Resolve de uma vez "sinal sem referente" e "ferramenta antes da ameaça": com um paradeiro, o Lampião e o Fio têm o que detectar desde a fase 1, sem mostrar a criatura.

**Reaproveita.** Rastro, Lugares, cama e porta (os destinos); `ModSons.tocarPara`; `Aparicao` (valida o ponto para nascer); os bichos da `Atmosfera`, que hoje olham para um ponto vazio; `ChamasPalidas`; o Sino, o Lampião e o Fio.

**Risco.** É a única coisa nova de porte médio que eu proponho, e mexe em muitos pontos do `Diretor`. Dois modos de falhar: o jogador triangula e "resolve" o monstro (por isso o sinal honesto é raro e pobre: dá o lado, não a distância exata); e o ponto vai parar num lugar impossível (por isso a validação da `Aparicao`). Para não quebrar o que funciona, entrar em dois passos: primeiro o ponto existe e só é gravado no log, sem mudar nada; depois os sons passam a usá-lo.

**Como testar.** Passo 1: linha `PARADEIRO pos=... estado=...` por minuto, e a sessão sintética mede a que distância dele caem os sons de hoje. Passo 2: GameTest em que dois sons seguidos saem a menos de N blocos um do outro; métrica "sons com referente" no `analisar.py`, com alerta abaixo de 50%.

**Quando.** Fase 0 (ele chegando de longe) em diante.

### 4. O som tem dono (reordenar; pequeno. Regravar as vozes: médio)

**O que o jogador percebe.** Um som que só existe quando é ele, e só sai de onde ele está. Raro. Com dois estados que se ouvem: espaçado quando está longe e distraído, mais junto quando está atento. Perto e visível, nenhum som: o mundo cala, como já cala.

**O que muda.**

- Escolher a assinatura entre os sons que já existem e **não** são voz. Há três variações de `ranger` que nunca tocam.
- Tirar `pano`, `estalo` e `grave` do serviço triplo. O Sino passa a soar como sino (o `sino_longe` existe). O Olho ganha o seu som. O `grave` fica só para o aviso da caçada.
- O assobio e o cantarolar saem do paradeiro, não de um ponto sorteado.
- Corrigir o batimento que denuncia o aviso verdadeiro.
- A borda escura falsa só passa a existir depois da primeira verdadeira. O mesmo para o aviso falso da caçada.
- **Vozes.** Manter a síntese para o que não é voz. Para sussurro, cantarolar e assobio, trocar por gravação real processada pelo mesmo script (o modo `--importar` que a revisão externa já pediu). Até lá, usar menos: o sussurro com voz sai em 70% das vezes e as sílabas não têm relação com a frase mostrada.

**Por que parece inteligente.** Cave Dweller: o som tem dono. Clicker: voz com estados, tratada como fala de personagem. Wagar: treinar o sinal como honesto antes de mentir. Chion: corpo mudo, voz sem corpo. Thom: voz sintetizada soa inorgânica.

**Reaproveita.** `ModSons`, `sounds.json`, `gerar_sons.py`, `Sentidos`, `TrilhaCliente`, o silêncio (`emudecer`).

**Risco.** Som honesto vira radar. Por isso raro, com dois estados só, e dando o lado sem dar a distância. A mentira justa continua existindo: o sinal sem ele, nunca ele sem o sinal nas primeiras horas. Outro risco: ninguém ouviu nenhum destes sons. A pergunta 3 da seção 7 existe por isso.

**Como testar.** Um teste que falha se um som marcado "dele" for tocado fora do paradeiro ou do corpo. Uma tabela no código: cada som tem uma promessa (dele, do item, do mundo, falso), e um JUnit acusa som usado em duas. O teste `oSomDaAssombracaoESoDoAlvo` passa a cobrir os sons do corpo.

**Quando.** Tudo.

### 5. Um ciclo, uma intenção, um pico (ajustar; pequeno a médio)

**O que o jogador percebe.** Cada noite tem um assunto. Numa, ele ronda a casa: algo na porta, a linha testada, o Boneco, e no fim a janela. Em outra, ele vem atrás: passos no caminho, pegadas, e no fim ele num ponto por onde o jogador passou. O jogador consegue contar a noite numa frase.

**O que é.** A cada ciclo do Diretor (do calmo ao recuo), escolher uma de três ou quatro intenções possíveis ali: rondar a casa, seguir, estudar de longe, testar uma defesa. A intenção multiplica os pesos dos eventos da sua família (como o contexto já faz) e dá o destino do paradeiro. Cada ciclo fecha com um desfecho da família, que é uma das cenas que já existem. Se o ciclo passar de um tempo sem nada forte, o desfecho é obrigatório e passa por cima do portão de V (a análise de 09/10 já pedia isso). Depois, a trégua de sempre.

**Por que parece inteligente.** Rabin: "What is their agenda for today?". Halo: previsível o bastante para formar modelo. Left 4 Dead: construção, pico, alívio. O `ROADMAP.md` já previa uma "camada de Intenção" na 0.8.1.

**Reaproveita.** `Seletor`, `ContextoMundo` (o mesmo mecanismo de multiplicador), os estados do Diretor, as cinco cenas, a `Atencao`.

**Risco.** Virar roteiro: sempre a mesma ordem. Por isso a intenção só inclina o sorteio, e o desfecho é sorteado dentro da família. Outro: a noite errada para o jogador (rondar a casa de quem está na mina). A escolha olha o contexto.

**Como testar.** Sessão sintética: por ciclo, pelo menos 70% dos eventos são da família; no máximo um desfecho; nenhum ciclo de mais de 40 minutos sem intensidade 22 ou mais. Linha `INTENCAO ciclo=N tipo=RONDAR`. Seção nova no `analisar.py`: ciclos, coerência, ciclos com pico.

**Quando.** Fase 1 em diante. O primeiro desfecho por volta dos 40–50 min da primeira sessão.

### 6. Chegou antes (ajustar; pequeno a médio)

**O que o jogador percebe.** Ele volta para casa depois de um tempo fora e alguém já esteve lá: a porta de sempre aparece entreaberta, há pegadas de cinza da soleira para dentro. Da fase 3 em diante, às vezes, de fora, pela janela, ele o vê **dentro** da casa, virado para a cama ou para o baú. Ele nota. Some.

**O que é.** Uma variante da cena "Ele voltou com você", com a ordem invertida. O gatilho é o rumo: fora de casa há três minutos ou mais, andando na direção da cama por dez segundos, entre 40 e 120 blocos. O paradeiro vai para a porta antes dele.

**Por que parece inteligente.** Antecipação é a única coisa da lista que o mod não tem em lugar nenhum. A regra do fantasma que mira à frente no Pac-Man é o exemplo clássico: a mesma navegação com outro alvo é lida como emboscada. É justo porque ele sabe um hábito acumulado (onde o jogador mora), não a posição dele.

**Reaproveita.** `CenaVoltouComVoce` (o "longe há quanto tempo"), o buscador de vidro da `CenaDoOutroLadoDoVidro`, a porta de sempre, a pegada de cinza, `Miragem`.

**Risco.** A previsão erra: o jogador muda de rumo. É o erro plausível da proposta 9: a marca fica lá, velha, para quando ele voltar. Risco de regra: `[código]` o evento `PORTA` hoje deixa a porta aberta de verdade; aqui a porta entreaberta tem de ser miragem. Nunca nascer dentro da zona da vela, nem a dois blocos de uma linha, nem com o jogador dentro de casa.

**Como testar.** Precisa de um jogador sintético **com rotina** (cama, porta, um trajeto de ida e volta). GameTest: aproximação a partir de 80 blocos; na chegada, a miragem e as pegadas já existem, com hora anterior à chegada. Linha `ANTECIPOU destino=CASA acertou=sim|nao`.

**Quando.** Fase 2 (só as marcas); fase 3 (ele pela janela).

### 7. O ensaio: ele faz o que você faz (ajustar; pequeno a médio)

**O que o jogador percebe.** Coisas dele, feitas por outro, fora de hora:

- o som do **baú** dele abrindo e fechando no cômodo de onde ele acabou de sair (o eco já faz isso com porta e picareta);
- ele, visto de longe ou pela janela, parado diante da bancada ou do baú que o jogador mais usa, virado para o móvel;
- na última etapa do Boneco, a nove blocos da cama, a abóbora dá lugar à **cabeça do jogador**.

**Por que parece inteligente.** É o objetivo do diário virando comportamento (página 9: "abriu a porta do jeito que eu abro"). É a regra do ECHO: ele só faz o que o jogador fez, o que também o torna justo. É a lição 1 dos mods: atenção com alvo mais um fato que se confere.

**Reaproveita.** O eco de ação (`acoes`), o clique em bloco que o mod já escuta, a proposta 1, `Erguidos` (o Boneco), `Mostruario`. `[JAR]` O jogo tem o bloco `player_head` com o perfil do jogador (entra pelo componente `profile`; não há setter público) e conta o uso de cada item nas estatísticas.

**Risco.** O jogador não percebe que é **o** baú dele e não um baú qualquer. Por isso escolher coisas únicas (a cama, a bancada do cômodo) e repetir com variação: a segunda vez mata a hipótese de coincidência. A cabeça no Boneco pode ler como defeito: conferir pela foto do GitHub se o bloco de cabeça ou um `item_display` ampliado lê melhor.

**Como testar.** GameTest: o jogador de mentira abre o mesmo baú dez vezes e a linha `ENSAIO alvo=BAU pos=...` escolhe aquele. O Boneco na etapa final tem o bloco com o perfil do jogador. Foto.

**Quando.** Sons: fase 2. Ele: fase 3. O Boneco: na quinta à sétima noite de um ciclo.

### 8. Ele percebeu: contramedidas, a Conta e o recibo (ajustar; pequeno a médio)

**O que o jogador percebe.**

- Na primeira vez que uma defesa funciona (a vela o segura, a linha o para, a caixa o atrai), ele **olha para a coisa**, não para o jogador, por um ou dois segundos.
- Na vez seguinte ele começa diferente, de um jeito ligado àquela defesa: já está na borda de onde a vela alcança; confere primeiro o esconderijo da última vez.
- Quando o jogador se apoia demais num item, quem cobra é **ele usando o item**: o sino toca de onde ele está, a caixa toca do lado de fora. Depois, uma linha nova no Caderno.
- Bater pela terceira vez tem um sinal: ele não recua, e a cabeça inclina.

**Por que parece inteligente.** Takayama: reagir ao resultado é o que fez um robô parecer mais esperto, inclusive falhando. Alien: Isolation: o lança-chamas lembrado, e a regra de só destravar pelo que o jogador liga a uma ação sua. Nemesis: a memória mostrada e a fraqueza fixa (aqui: a vela sempre vale pelo menos vinte segundos). Araz: o recibo.

**Reaproveita.** `Cacada`, `HospedeBusca`, os contadores da `Memoria` (`velas`, `caca_atravessou`, `cacadas`), o evento `ESPERA`, `Conta`, `ProgressoInvestigacao`, o paradeiro. Falta gravar duas coisas que hoje só vão para o log: como o jogador escapou e onde estava quando ele desistiu.

**Risco.** A caçada vive na fase 4, então o grosso disto não aparece nas primeiras horas. Por isso a mesma regra vale fora da caçada (a vela contra uma aparição, a linha na porta). A Conta com dono soma sons: no máximo uma cobrança a cada dez minutos. `[código]` O evento `ESPERA` quase não dispara hoje (pede V de 55 dentro da luz da vela); para ele ser a resposta à vela, o portão precisa cair nesse caso.

**Como testar.** Testes novos em `TestesDaCacada`: "a vela segura e ele olha para a vela"; "a segunda caçada confere primeiro o esconderijo anterior". Em `TestesDosItens`: depois do estouro, o som da cobrança sai do paradeiro e o Caderno ganha a linha. Linha `CONTRAMEDIDA meio=VELA primeira=sim` e uma tabela no `analisar.py`.

**Quando.** Vela e linha fora da caçada: fase 2. O resto: fase 4.

### 9. O erro com imagem (ajustar; pequeno)

**O que o jogador percebe.**

- Ele muda a cama de lugar, e naquela noite o Boneco ainda está virado para a cama antiga. Na seguinte, virou.
- Ele troca de porta, e a próxima coisa de porta acontece na antiga.
- Na caçada, escondido, ele espia e o vê parado, de costas ou de lado, com a cabeça varrendo o lugar onde ele **estava**.

**Por que parece inteligente.** Poder ser enganado prova que havia uma crença (Isla; McIntosh). É o que dá justiça. Ressalva da seção 3.4: isto sustenta credibilidade; não há experimento dizendo que erro faz parecer mais esperto.

**Reaproveita.** A porta de sempre e a cama (guardar a anterior por uma noite), `Erguidos`, `HospedeBusca`.

**O que muda na caçada.** `[código]` Hoje ela trapaceia de dois jeitos que o jogador sente: parada, está sempre virada para a posição real dele, mesmo sem saber onde ele está; e ser olhada lhe entrega a posição com certeza total. A mudança: quando não sabe, fica virada para onde acha que ele está, com a cabeça varrendo; e espiar de longe, com ela de costas, não entrega nada.

**Risco.** A caçada fica mais fácil para quem se esconde bem. É o lado certo de errar: mais justo.

**Como testar.** `TestesDaCacada`: "espiar não entrega" (a certeza não sobe quando ele a vê de costas, a mais de N blocos). GameTest do Boneco: mudar a cama; a face do Boneco aponta para a antiga por uma noite. Linha `CRENCA cama=antiga`.

**Quando.** Boneco e porta: fase 2. Caçada: fase 4.

### 10. Marcas que não mentem (reordenar; pequeno)

**O que o jogador percebe.** Três sinais com sentido fixo, que ele aprende sozinho:

- **pegada de cinza**: ele pisou aqui, e foi para lá (a pegada aponta);
- **chama pálida**: ele passou perto desta tocha;
- **porta entreaberta**: ele passou por aqui.

Regra: marcas nunca mentem; sons podem. A sequência de uma noite para a outra (fora, soleira, dentro, ao lado da cama) é a mensagem.

**Por que parece inteligente.** Darkwood: o efeito sem o autor. A síntese da pesquisa sobre mensagem ambiental: não aleatória, no ponto de atenção, feita fora da vista, repetida com variação, escalada por proximidade. O Boneco já é isso.

**Reaproveita.** `CinzaEspalhadaBlock` (o estado de pegada), `ChamasPalidas` (hoje sorteia a tocha; passa a escolher a mais próxima do paradeiro), `Miragem`, `Vestigios`.

**Risco.** Marca demais vira decoração: teto por noite. Pegada em piso construído: manter a regra de hoje (chão natural e soleira).

**Como testar.** GameTest: toda pegada nasce onde o paradeiro esteve nos últimos N segundos; a tocha pálida fica a menos de seis blocos dele. Métrica: marcas com referente, que tem de dar 100%.

**Quando.** Fase 2.

### 11. O experimento com vestígio (ajustar; pequeno a médio)

**O que o jogador percebe.** Ele testa uma coisa e deixa o resultado onde o jogador acha.

- Bate na porta. Se o jogador foi conferir, na noite seguinte a batida vem, e a pegada está no lugar de onde o jogador **olhou** da última vez.
- O Sino responde do lugar errado (isso já existe). Depois, quem procura acha uma pegada atrás de onde estava quando tocou: respondeu de lá, mas estava aqui.
- A linha testada à noite, com a pegada do lado de fora, já é isto e é o modelo.

**Por que parece inteligente.** O estado `TESTANDO` do Diretor já faz experimentos; só que invisíveis. Alien: Isolation: voltar atrás para parecer dúvida. F.E.A.R.: a falha anterior muda o plano seguinte, à vista.

**Reaproveita.** `BATIDA`, `Leitura` (investigou), o Sino, as pegadas, o paradeiro (sem um lugar verdadeiro, a mentira não tem do que divergir).

**Risco.** Sutil demais (Halo): um terço dos jogadores não liga os pontos. Só vale no ponto de atenção habitual e repetindo.

**Como testar.** Jogador com rotina: depois de `BATIDA` com "investigou", o evento seguinte da família sai com `motivo=EXPERIMENTO`, e a pegada cai a até dois blocos de onde o jogador parou.

**Quando.** Fase 2 em diante. Depende da 3 e da 10.

### 12. O que ele já sabe: o arco em três atos (construir; médio)

**O que o jogador percebe.** Ao longo dos dias, ele sabe mais. Primeiro observa de fora. Depois faz o que o jogador faz, dentro, quando o jogador sai. E o que ele sabe aparece: o Boneco com a cabeça, a cantiga, a cópia da casa no Avesso que fica **mais certa** a cada visita.

**O que é.** Uma lista curta de **saberes**, fatos discretos sobre este jogador: a cama, a porta, a bancada, a mina, a cantiga, o nome, a ferramenta, o rosto. Cada um com três degraus: observou, ensaiou, domina. Substitui o perfil de seis traços como "o que ele aprendeu". As fases continuam existindo como destrave de repertório nas primeiras sessões; o arco longo passa a ser dos saberes, em três atos:

| Ato | Quando (a acertar com a pergunta 2) | O que ele faz | O que o jogador vê |
|---|---|---|---|
| **Estudo** | As duas ou três primeiras sessões | Observa de fora. Aprende a cama, a porta, o caminho | Ele virado para as coisas do jogador; marcas do lado de fora; o Boneco começa a vir |
| **Ensaio** | O meio das duas semanas | Faz o que o jogador faz, dentro, quando ele sai | Os sons das ações dele fora de hora; "chegou antes"; a cantiga; o Boneco com a cabeça; a cópia do Avesso mais certa |
| **Troca** | Os últimos dias | Fecha (proposta 15) | Ele mesmo, visto de fora; a última página; depois, quase nada |

**Por que parece inteligente.** Halo: trocar variáveis difusas por eventos discretos. Nemesis: poucas âncoras, memória exibida. Araz: aprender é contar, e mostrar a conta. Dá ao Hóspede um objetivo próprio com progresso que se lê.

**Reaproveita.** `Memoria` (quase todos os fatos já estão gravados), as propostas 7 e 10 como saída visível, o Avesso, a Casa do Vigia.

**Risco.** Virar barra de progresso. Não tem tela nem número: só as saídas. Outro: o jogador que não faz nada de pessoal (não dorme, não tem casa). O arco precisa de um caminho mínimo por tempo.

**Como testar.** JUnit para a progressão. Linha `SABER +CAMA nivel=1`. Linha do tempo de saberes no `analisar.py`. Jogador com rotina chega a três saberes em duas horas.

**Quando.** Ato I nas duas primeiras sessões; ato II do terceiro dia em diante. Os números dependem da pergunta 2.

### 13. Com amigos (ajustar e construir; pequeno a médio)

Em ordem de custo:

- **Só o alvo vê** (pequeno). `[código]` Hoje a criatura é uma entidade comum e os sons do corpo dela são do mundo. `[JAR]` `Entity.broadcastToPlayer(ServerPlayer)` existe na 26.2. Com isso o Lampião do amigo esfria sem ele ver nada: é o relato que ninguém confirma, com uma prova física ao lado.
- **O marcado da noite** (pequeno). Um alvo por noite. Os outros têm sossego, mas as marcas (pegadas, o Boneco) ficam no lugar do marcado, onde todos podem ver. Quem se afasta do grupo passa a ser o marcado.
- **O sósia de dois segundos** (médio). O manequim do jogo com a pele de um amigo que está longe, visto de relance, só pelo alvo, com um tique como única pista (para e encara; não balança o braço). Junto, um som **não verbal** do amigo: a cadência dos passos, a porta, a picareta, pelo mesmo mecanismo do eco. `[JAR]` `Mannequin` existe, aceita o perfil do jogador, esconde o rótulo e tem as poses de pé, agachado e deitado. `[código]` Já há um experimento por comando.
- **Versões que divergem.** Cada jogador tem os seus saberes e o seu Boneco.

**Por que parece inteligente.** Entre amigos a intenção aparece como escolha: por que eu, por que agora, por que com a cara dele. Da pesquisa sobre mímica: o que mais enganou foi som não verbal; dois segundos de ilusão criam desconfiança longa; sem contexto, vira piada.

**Reaproveita.** `HospedeEntity`, `ModSons` (o som só para o alvo), a vulnerabilidade (que já soma quando ele está sozinho), o desfecho "amigo" dos Ossos de Agouro (o único ponto do mod que hoje usa outro jogador), o experimento `sosia`, o eco de ação.

**Fica de fora.** Imitar voz pelo Simple Voice Chat. Tem versão para a 26.2 e a ideia é viável, mas grava o microfone dos amigos e, nos relatos, falha como piada. Se um dia entrar: só trechos curtos, e depois do resto.

**Risco.** A conferir antes de prometer: mostrar um `Mannequin` a um jogador só pede uma classe própria ou pacotes enviados à mão. O riso é o modo normal de falha entre amigos; protege-se o sistema com raridade e curta duração.

**Como testar.** GameTest com dois jogadores de mentira: só o alvo recebe a entidade e os sons do corpo.

**Quando.** Quando houver servidor marcado. "Só o alvo vê" pode entrar antes.

### 14. O corpo (ajustar: pequeno. Articular e rosto: médio)

O que dá para fazer **com o modelo do próprio jogo** (o `HospedeModel` atual):

| Mudança | Custo | Observação |
|---|---|---|
| Cabeça independente, olhar para um objeto, virar com atraso | nenhum de modelo | É a proposta 1. O modelo já faz |
| Imobilidade total no centro da tela; o balanço lento só na periferia | pequeno | Hoje a cabeça e o tronco balançam sempre. O cálculo de tela já existe |
| Congelar também a animação quando olhado | pequeno | Como o The Hollow |
| Tempo errado: o passo a 0,5–0,7 da velocidade, na caçada e no Avesso | pequeno | Mori: tempo errado sozinho já perturba |
| Trocar de pose sem transição, raramente | pequeno | — |
| Silhueta sem luz para o vulto distante | pequeno a médio | Como o The Hollow. Lê como recorte mesmo de dia |
| Cotovelo, pescoço, dedos (peças filhas com pivô) | médio | Ninguém vê o resultado a não ser pelas fotos |
| O rosto aprendido: a pele do jogador sobre a cabeça, em degraus | médio | `[JAR]` O cliente tem a textura (`AbstractClientPlayer.getSkin().body()`) |

O que **exigiria GeckoLib**: animação por quadros-chave feita no Blockbench, com transições e controladores (rastejar, escalar, mãos com dedos animados). `[Modrinth]` A GeckoLib 5.5.5 tem versão para Fabric 26.2. Não testei com o projeto. Nenhuma proposta deste documento precisa dela, e os dois mods de vulto humanoide mais relevantes também não usam.

**Por que parece inteligente.** O que muda a leitura de intenção num humanoide parado é, nesta ordem: para onde a cabeça aponta, se ele se mexe só quando não é olhado, e o tempo do movimento. Proporção e textura vêm depois.

**Reaproveita.** `HospedeModel`, `HospedeRenderer`, `HospedeRenderState` e o cálculo de tela da `Percepcao`.

**Risco.** Tudo aqui é visual e só o dono vê. Entrar por último e pouco de cada vez.

**Como testar.** As fotos do job do GitHub, uma por pose, e um GameTest para o que é estado (animação congelada quando olhado).

**Quando.** Depois das atualizações A a D. A imobilidade no centro da tela e a animação congelada podem vir antes, por serem pequenas.

### 15. A Troca: um fim (construir; grande)

**O que o jogador percebe.** Depois de dias, com os saberes completos: de fora de casa, pela janela, ele vê **a si mesmo** lá dentro por dois segundos, parado na bancada ou deitado na cama. Na última noite, uma página nova, escrita com fatos verdadeiros dele e assinada com o nome dele. Depois, o mod fica raro e quieto.

**Por que.** SA-X: um arco que fecha sem ser mais uma perseguição. Nemesis e Araz: o fato verdadeiro. A revisão externa já propôs isto como "fase 5"; eu concordo e mudo duas coisas: os fatos da página têm de ser conferíveis (noites dormidas, a porta, o que ele mais usou), e a cópia do Avesso tem de ter ficado certa antes.

**Reaproveita.** O manequim, a `Memoria`, o diário (a página 14 já usa o nome do jogador), o Avesso.

**Risco.** Um fim que chega cedo demais encerra o mod no meio do servidor; tarde demais, ninguém vê. E o manequim visto de perto é só um boneco com a pele do jogador: tem de ser de longe, por dois segundos, uma vez.

**Como testar.** GameTest: o manequim nasce com o perfil e a pose certos e some no tempo. Foto. JUnit para o texto da página, montado a partir de uma memória de exemplo.

**Quando.** Depois de tudo. Depende da pergunta 2.

---

## 5. Sequência proposta

Depois da `fix/ritmo-travado`, que já entrou. Cada atualização é pequena, tem teste que roda sem o dono, e termina com ele jogando e o log sendo lido contra critérios escritos antes.

### Atualização A: a primeira hora (já combinada em 10/10)

O que a análise de 09/10 propôs (primeiro contato garantido entre 15 e 25 minutos, a página do item junto com o item, o primeiro uso com resposta clara, a Conta com carência e recibo, os critérios no analisador). Acrescento três coisas pequenas:

- o primeiro contato usa a **proposta 1**: ele está virado para um lugar do jogador, nota, some, deixa a primeira cinza;
- a **proposta 2**, que é quase toda de cliente e de cadeia;
- os consertos do Apêndice D que são de regra ou de entrega (a tocha quebrada de verdade, o batimento, os presentes largados atrás, a cor da vela).

**Teste.** A sessão sintética hoje começa na fase 2 e na fase 4: falta um jogador que comece na fase 0. Critérios da análise (primeira aparição até os 25 minutos; pelo menos uma saída forte por hora; Conta abaixo de um terço do percebido) viram alertas do `analisar.py`.

### Atualização B: ele está em algum lugar

Propostas **3** (em dois passos: primeiro só no log), **4** (sem regravar vozes) e **10**.

**Teste.** "Sons com referente" acima de 50% na sessão sintética; marcas com referente em 100%; o teste de promessa dos sons.

### Atualização C: uma noite, uma intenção

Propostas **5**, **6** e a parte de casa da **9**. Um jogador sintético com rotina.

**Teste.** Coerência do ciclo de 70% ou mais; um pico por ciclo; antecipação registrada em pelo menos uma de três voltas para casa.

### Atualização D: ele aprende você

Propostas **7**, **8**, **11** e o registro mínimo da **12**.

**Teste.** Os testes de contramedida; os saberes no log.

### Depois, quando o dono pedir

A **13** quando houver servidor marcado (o "só o alvo vê" pode vir antes). A **14** aos poucos. A **15** saiu da fila: em 10/10 o dono disse que por enquanto o mod não tem fim (seção 7).

Entre uma atualização e a seguinte: o dono joga, eu leio o log, e número só muda com mais de uma sessão.

### As três primeiras horas, depois de A, B e C

É o alvo, não uma medida. Compare com a tabela da seção 2.7.

| Quando | O que o jogador vê, ouve e entende |
|---|---|
| 0–10 min | Quase nada. Um ou dois sinais vindos do mesmo lado: é ele chegando de longe |
| 10–15 min | A primeira página. Os passos que a acompanham vêm de onde ele está |
| 15–25 min | **O primeiro contato.** Ele está num lugar do jogador, virado para esse lugar; nota; some; deixa a primeira cinza. O jogador sai sabendo: existe, chega perto, olha para as minhas coisas, deixa matéria |
| 25–60 min | A primeira ferramenta nasce do encontro e responde de verdade. Os sons vêm de um lugar que anda. Uma noite com um assunto, fechada por uma cena. O jogador entende: som quer dizer alguma coisa |
| 60–120 min | "Chegou antes" na volta para casa. Os primeiros sons dele fazendo o que o jogador faz. A primeira caçada no fim da primeira sessão ou no começo da segunda (a análise de 09/10 propõe fases mais curtas para isso) |
| 120–180 min | Ele responde às defesas e mostra que percebeu. O Boneco, a Casa do Vigia. Os primeiros sinais que mentem, agora que há verdade para mentir |

---

## 6. O que eu cortaria ou adiaria

**Consertar já, porque fere uma regra.** O evento `LUZ_ERRADA`, na fase 3 em diante, quebra uma tocha do jogador de verdade em 7% dos sorteios (`Atmosfera.java:395-398`). O `DIVIDAS-DESIGN.md` diz que nenhuma tocha sai mais do mundo. Sai.

**Cortar ou desligar até haver arco.**

| O quê | Por quê |
|---|---|
| A borda escura falsa (`VIGIA`) e o aviso falso (`PRENUNCIO`) antes da primeira verdade | Ensinam que o sinal não quer dizer nada |
| `NEBLINA` | Lê como clima. Sem causa e sem leitura de reação |
| `O_CAMINHO_MUDOU` e `MARCA_IMPOSSIVEL` (blocos temporários no caminho) | Sem sentido que o jogador possa aprender; risco de ler como defeito. Voltam se uma sessão mostrar que ele notou |
| Presságios soltos da fase 0 | Com paradeiro, viram "ele chegando". Sem ele, são o "meio jogados" |
| Ossos de Agouro nos baús e na Casa do Vigia | Sete padrões sem legenda, sobrepõe três itens, e pode ser a primeira aparição por acidente |
| Isca Pálida | Quase nunca responde, e a cobrança dela é invisível. Ou vira parte da caçada, ou sai |
| Itens do mod em baús do próprio jogo desde o minuto zero | Ferramenta antes da ameaça |
| O baralho | Adianta coisas fora de hora, e três cartas em doze são nada. Com as intenções, a carta passa a escolher o assunto da noite |
| O perfil de seis traços | Em 2h06 não mudou nada. Os saberes fazem o trabalho, e dá para mostrá-los |

**Rebaixar, sem apagar.** O aprendizado de pesos fica como está e deixa de ser "a inteligência". Não vale consertar o ruído da leitura agora. Vale tirar dela a autoridade de escalar o Diretor com uma leitura só.

**Adiar.**

| O quê | Por quê |
|---|---|
| Itens, lugares, eventos e dimensões novas | A tese da revisão externa e da análise de 09/10 continua valendo |
| A "medida errada" do Avesso e mais erro na cópia | Vai contra o tema (seção 2.7) |
| GeckoLib | Nenhuma proposta precisa |
| Imitação de voz | Privacidade; falha como piada |
| A proposta 15 | Só depois de o arco existir |

**Boas ideias da pesquisa que não cabem.**

| Ideia | De onde | Por que não |
|---|---|---|
| Catálogo de tipos de entidade com evidências | Phasmophobia | Empurra para a postura de detetive, que é a que mata o medo |
| Árvore de comportamento grande e planejador por objetivos | Alien: Isolation, F.E.A.R. | Escopo de estúdio. O que interessa é a encenação |
| Falas que explicam o plano | F.E.A.R., Halo | Verbais, altas, pedem dois |
| Reconhecer o que o jogador diz ao microfone | Phasmophobia, The Blackout Club | Frágil, depende de idioma, privacidade |
| Cicatrizes e títulos | Nemesis | Apoiam-se em morte e em muitos indivíduos |
| Invasão que quebra a barricada | Darkwood | Fere duas regras |
| Copiar a construção do jogador com blocos pobres | The Obsessed | Fere a regra, a não ser como miragem; caro |
| Placa com texto e número | Araz | Boa. Aqui o recibo cabe melhor no Caderno e nos riscos da placa da Casa do Vigia, que já existem |
| Responder a palavras no chat | The Broken Script, LocalHost | Quarta parede. O eco do chat já existe e basta |
| Ordens com cobrança ("agache virado para o norte") | LocalHost | Interessante, mas é outro mod: põe o jogador para obedecer |

---

## 7. Perguntas para o dono (sem entregar nada)

1. Hoje, quando alguma coisa aparece, é um piscar, do jeito que você pediu. Você toparia que **de vez em quando, e só de longe**, desse para olhar por um ou dois segundos? Ou prefere que seja sempre um piscar?
2. Quanto tempo dura uma sessão sua, e por quantos dias vocês costumam manter um servidor? Você quer que o mod **tenha um fim** dentro desse tempo, ou que continue enquanto vocês jogarem?
3. Dos sons que você já ouviu, algum pareceu "de sintetizador", falso? E você toparia **gravar alguns sons com o microfone**, sem eu dizer para quê? Ou prefere que eu use gravações livres?
4. Com amigos: você prefere que cada um viva a sua história sem os outros verem, ou que algumas coisas todos vejam? E vocês usam chat de voz dentro do jogo ou só o Discord?
5. Você prefere começar com **poucos itens** e ir ganhando os outros com o tempo, mesmo que alguns dos que você já viu demorem a aparecer num mundo novo?

Continuam sem resposta as cinco perguntas da análise de 09/10. A 2 e a 4 de lá (sons que parecem falsos; dicas dos itens) pesam nas propostas daqui.

### Respostas do dono (10/10/2026) e o que elas mudam

As respostas estão entre aspas, como ele escreveu. O que vem depois de cada uma é leitura minha.

**1. "Sim de vez em quando, pode ate ter algum som, ou de vez em quando ter som tmbem, pra dar aquele sustinho".**

- A variante longa da **proposta 1** está liberada: de vez em quando, de longe, um ou dois segundos. O relance continua sendo a regra. Ponto de partida: no máximo uma em cada quatro ou cinco aparições, só a mais de 20 blocos.
- Ele aceita **um som junto, às vezes**. Isso muda a **proposta 4**: "perto e visível, nenhum som" deixa de ser absoluto. Passa a ser o normal, com uma exceção rara: um som curto que sai dele no instante em que ele nota o jogador, ou no instante em que some.
- Leitura minha: "sustinho" é um som curto e baixo, não um estouro. A regra "nunca alto" continua valendo; ele mesmo a repetiu no pedido desta pesquisa. Se ele quiser mais forte que isso, precisa dizer.
- Cuidado de execução, da equipe de áudio do Alien: Isolation (seção 3.2): o som tem de sair no mesmo tick do gesto. Um segundo de atraso vira comédia, e num servidor o pacote viaja.

**2. "Por enquanto sem fim, vamos falar disso depois".**

- A **proposta 15** sai da fila até ele voltar ao assunto.
- A **proposta 12** fica com dois atos (Estudo e Ensaio) e sem data. O risco passa a ser o platô: depois que ele "domina" tudo, o que muda? Resposta dentro do que já está proposto: os saberes envelhecem. O jogador muda de cama, de porta, de caminho, e ele volta a observar e a errar (proposta 9). O arco vira um ciclo que acompanha as mudanças do jogador, não uma contagem até um fim.
- A cópia do Avesso ficando mais certa continua valendo; só não precisa chegar a lugar nenhum.

**3. "acho que ainda não, usar gravações livres e bibliotecas de sons".**

- Nenhum som pareceu falso para ele até agora. Ele ouviu poucos: isso baixa a urgência de regravar, não prova que as vozes sintetizadas estão boas.
- Para o que é voz ou sopro (sussurro, cantarolar, assobio), a fonte passa a ser **gravação livre**, processada pelo mesmo script. Não pedir gravação a ele.
- Como o repositório é público: preferir CC0; anotar a origem e a licença de cada arquivo importado, num arquivo ao lado do script; não usar som tirado de outros jogos (um dos mods lidos credita sons de Dead Space e SOMA; aqui não).

**4. "acho que uma ou outra coisa todos veem, bom se fosse pra jogar realmente usariamos no jogo eua cho".**

- Confirma a **proposta 13** como está: quase tudo por jogador, e poucas coisas físicas compartilhadas (as marcas, o Boneco).
- Com chat de voz dentro do jogo, quem se afasta deixa de ouvir os outros de verdade, e a separação do grupo passa a pesar sem o mod fazer nada. Vale conferir a combinação com o Simple Voice Chat quando houver servidor marcado.
- A imitação de voz continua adiada pelos motivos da seção 3.2. A resposta só tira dela o rótulo de hipótese remota.

**5. "faça o que achar melhor".**

- Fica como a análise de 09/10 e a seção 6 propõem: nas duas primeiras horas, dois verbos (afastar e perguntar). Os outros itens chegam depois, pelos lugares, cada um com a página que o explica. Os Ossos e a Isca saem do começo.

**A sequência da seção 5 não muda.** A atualização A ganha a variante longa e o som raro da resposta 1.

---

## Apêndice A. O log, em números

| Medida | Trecho antigo (07–08/10) | 09/10 (alpha12) |
|---|---|---|
| Tempo de jogo | 1h10 | 55 min |
| Saídas percebidas por hora | 45 | 25 |
| Eventos do Diretor | 35 | 5 |
| Manifestações da criatura | 40 (33 por comando, 4 pelo Olho, 3 do Diretor) | 0 |
| Do Diretor, vistas pelo jogador | 2, na mesma cena, aos 71 min | 0 |
| Do Diretor, em lugar do jogador | 0 | 0 |
| Linhas de sinal com `semCriatura=sim` (nenhuma com `nao`) | 33 | 7 |
| Leituras de reação: total, com confiança zero, acima de 0,45 | 34, 19, 5 | 5, 0, 2 |
| Traços do perfil (menor e maior valor, nos dois trechos) | cautela 50–55, luz 41–54, caseiro 34–50, explorador 48–66, confronto 48–55, fuga 28–53 | |
| Sino | — | 16 usos: 11 respostas do caminho dele, 5 silêncios |
| Conta: avisos e cobranças | — | 8 e 4 |

O trecho antigo é de antes das correções de alcance de som e de leitura de reação e tem comandos de teste no meio. O de 09/10 teve três estruturas geradas por comando e um teleporte. Nenhum dos dois é uma sessão limpa.

## Apêndice B. O que eu conferi no código

| Afirmação | Onde | Conferido |
|---|---|---|
| A criatura é virada para o jogador, corpo e cabeça iguais, a cada tick | `HospedeEntity.java:574-578, 691-699` | Sim |
| O modelo já aplica a rotação da cabeça separada do corpo | `HospedeModel.java:96-97` | Sim |
| O assobio sai de um ponto sorteado | `Diretor.java:2157-2165` | Sim |
| O aviso falso manda 0,15 e o verdadeiro 0,2; o batimento começa em 0,2 | `Sentidos.java:63-65`; `Cacada.java:141`; `TrilhaCliente.java:89` | Sim |
| Os presentes de fase são largados atrás do jogador como item comum, e a entrega já fica marcada | `Diretor.java:752-784, 4445-4454` | Sim. Que somem em cinco minutos é o comportamento normal de item no chão; não rodei |
| `LUZ_ERRADA` quebra uma tocha de verdade na fase 3 em diante | `Atmosfera.java:395-398` | Sim |
| A vela não devolve a cor na fase 2 (0,28 × 0,35 = 0,098; o degrau só desce com 0,095) | `Sentidos.java:36-39`; `CorDrenada.java:81-89` | Sim, pela conta |
| "Olhadas" acumula sem decaimento e com 12 muda o evento para sempre | `Diretor.java:497-501, 1505-1509` | Sim. Que todo jogador chega a 12 em minutos é suposição |
| A caçada devida por criatura parada no tempo é gravada numa cópia da memória e sobrescrita no fim do tick | `Diretor.java:456, 463, 3419-3421, 736` | Li e o caminho bate com a armadilha descrita no `CLAUDE.md`. Não rodei |
| O mod já escuta clique em bloco, quebra de bloco, sono, chat, dano e morte | `Diretor.java:165-254` | Sim |

O resto do inventário (seção 1 e os itens do Apêndice D marcados "relatado") vem dos três subagentes, com linha citada, e eu não reli.

## Apêndice C. O que eu conferi no Minecraft 26.2

Com o `javap` do JDK 25 nos JARs de `~/.gradle/caches/fabric-loom/26.2/`, e com o código descompilado pelo `minecraft-dev`.

| Preciso de | Existe? | Observação |
|---|---|---|
| Entidade visível só para um jogador | `Entity.broadcastToPlayer(ServerPlayer)` | Vale para a classe da criatura, que é do mod |
| Cabeça separada do corpo | `LivingEntity.yHeadRot`, `yBodyRot`, `setYHeadRot`, `setYBodyRot` | — |
| Olhar para um ponto | `LookControl.setLookAt(double, double, double)` | — |
| Manequim com a pele de um jogador | `net.minecraft.world.entity.decoration.Mannequin`: perfil, rótulo que se esconde, imóvel | Poses válidas: de pé, agachado, nadando, planando, **dormindo** |
| Cabeça de jogador como bloco | `Blocks.PLAYER_HEAD`; `SkullBlockEntity` guarda o perfil | Não há setter público: o perfil entra pelo componente `DataComponents.PROFILE` |
| Perfil a partir do jogador | `ResolvableProfile.createResolved(GameProfile)` | — |
| O item mais usado, baús abertos, noites dormidas | `Stats.ITEM_USED`, `OPEN_CHEST`, `SLEEP_IN_BED`; `ServerPlayer.getStats().getValue(...)` | São totais, sem posição. Para "o baú mais usado" o mod precisa contar por lugar |
| A pele do jogador no cliente | `AbstractClientPlayer.getSkin()`, `PlayerSkin.body()` | — |

Pela API do Modrinth, em 10/10/2026, para Fabric 26.2: GeckoLib 5.5.5 (06/09/2026); Simple Voice Chat 2.6.24 (20/09/2026); Sound Physics Remastered 1.5.1 (18/06/2026).

**Não conferi:** se dá para mostrar um `Mannequin` do jogo a um jogador só sem classe própria; se o controle de rotação de corpo do jogo interfere na proposta 1; se a GeckoLib compila com o projeto.

## Apêndice D. Defeitos achados de passagem

Nenhum foi corrigido. "Conferido" quer dizer que eu li a linha; "relatado" quer dizer que veio da leitura de um subagente e eu não reli.

| # | Defeito | Onde | Situação |
|---|---|---|---|
| 1 | `LUZ_ERRADA` quebra uma tocha de verdade (fase 3 em diante, 7%) | `Atmosfera.java:395-398` | Conferido. Fere "nunca estragar a construção" |
| 2 | O batimento denuncia o aviso verdadeiro da caçada | `Sentidos.java:63-65`; `TrilhaCliente.java:89` | Conferido |
| 3 | Página, Olho e Caixa são largados atrás do jogador como item comum; a entrega já fica marcada e a receita da Caixa só destrava tendo uma | `Diretor.java:752-784, 4445-4454` | Conferido o código. `[suponho]` Se ele não se virar em cinco minutos, perde o item para sempre |
| 4 | A vela não devolve a cor na fase 2, e só um degrau nas fases 3 e 4 | `CorDrenada.java:81-89` | Conferido pela conta |
| 5 | A caçada devida por criatura parada no tempo se perde | `Diretor.java:3419-3421, 736` | Lido; não rodei |
| 6 | "Olha para trás" dispara para todo jogador e mata uma variante de evento | `Diretor.java:497-501` | Conferido o código; o efeito é suposição |
| 7 | O evento `PORTA` deixa a porta do jogador aberta de verdade | `Diretor.java:2333-2342` | Relatado. À noite, fora do pacífico, entra monstro |
| 8 | A espera na borda da vela quase não dispara (pede V de 55 dentro da luz) | `Diretor.java:1083`; `Seletor.java:104` | Relatado; suposição |
| 9 | Só dois eventos criam marco; a cena "Foi aqui" quase não tem do que partir | `Diretor.java:1695` | Relatado |
| 10 | No ambiente, o presságio (14) passa na frente da perturbação (20), como o erro da alpha12 | `Atmosfera.java:131-155` | Relatado; suposição |
| 11 | A leitura conta como "ignorado" o que aconteceu fora da tela (`LUZ_ERRADA` tem observabilidade fixa de 0,70) | `Atmosfera.java:412` | Relatado |
| 12 | O ouvido da caçada usa `getDeltaMovement`, que na 26.2 não é o movimento vindo do cliente | `HospedeBusca.java:173-181` | Relatado; não medido |
| 13 | A página da manhã, a linha testada e o sorteio do Avesso rodam em todo "sair da cama", mesmo sem dormir | `Diretor.java:3244, 3287` | Relatado |
| 14 | O presente que ele deixa na tigela é avaliado como oferenda na noite seguinte | `Oferenda.java:146-203` | Relatado |
| 15 | O sussurro "eu sei onde você dorme" sai mesmo sem cama registrada | `Diretor.java:2139-2143` | Relatado |
| 16 | A dica da Caixa diz "a música você já ouviu em algum lugar"; o tema não toca antes da Caixa | `lang/en_us.json:118` | Relatado |
| 17 | Rodar o gerador de sons de novo mistura a caixa "gasta" com a "arruinada" (o `sounds.json` foi ajustado à mão) | `gerar_sons.py:1712, 1883` | Relatado |
| 18 | Um uso isolado de item de peso 1 é apagado da Conta no segundo seguinte | `Conta.java:87-101` | Relatado |
| 19 | Uma cena que não acha lugar gasta a vez e o intervalo | `Cena*.java` | Relatado |
| 20 | Código e dados sem uso: `criaturaTocou` (a frase "peguei você" nunca aparece), a mensagem "irritado", o efeito `TREMOR`, cinco sons, sete chaves da memória | vários | Relatado |

Documento contra código: a caçada não exige inquietação de 60 (a condição só existe num ramo que nunca roda); o piso é de 7 minutos nas fases 1 e 2, não 12; "Foi aqui" vale na mesma sessão; nem todo item soma na Conta (Tigela, Caderno e Página não somam).

## Apêndice E. Limites

- Ninguém jogou. Todas as frases sobre o que o jogador "percebe" são dedução de código, de log e de pesquisa.
- Duas sessões, um jogador, nenhuma delas limpa, nenhuma com a alpha13.
- O inventário vem de três leituras de subagentes. Conferi os pontos que sustentam conclusões, não tudo.
- As fontes externas foram abertas por subagentes. As citações literais passaram pelo extrator da ferramenta, com exceção das de Garner e Grimshaw. Para os mods, parte veio de ler a lista de classes e as constantes dos jars, sem descompilar a lógica, e nenhum mod foi executado.
- Os números das propostas (ticks, blocos, porcentagens) são pontos de partida.
- A proposta 3 é a aposta maior. Se o passo "só no log" mostrar que os sons de hoje já caem perto de um ponto coerente, ela encolhe; se mostrar o contrário, ela se justifica.

## Apêndice F. Fontes principais

As fontes já citadas no `PESQUISA-E-ANALISE.md`, em `pesquisa/2026-10-08-*.md` e na análise de 09/10 não se repetem aqui.

**IA clássica**

- Tommy Thompson, "The Perfect Organism: The AI of Alien: Isolation": https://www.gamedeveloper.com/design/the-perfect-organism-the-ai-of-alien-isolation
- Tommy Thompson, "Revisiting the AI of Alien: Isolation": https://www.gamedeveloper.com/design/revisiting-the-ai-of-alien-isolation
- MCV, "18 things we learned about Alien: Isolation last night": https://www.mcvuk.com/development-news/18-things-we-learned-about-alien-isolation-last-night/
- Michael Booth, "The AI Systems of Left 4 Dead" (2009): https://cdn.fastly.steamstatic.com/apps/valve/2009/ai_systems_of_l4d_mike_booth.pdf
- Jeff Orkin, "Three States and a Plan: The A.I. of F.E.A.R." (GDC 2006): https://pages.cs.wisc.edu/~dyer/cs540/handouts/gdc2006_orkin_jeff_fear.pdf
- Jeff Orkin, "Combat Dialogue in F.E.A.R.: The Illusion of Communication": http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter02_Combat_Dialogue_in_FEAR_The_Illusion_of_Communication.pdf
- Butcher e Griesemer, "The Illusion of Intelligence" (GDC 2002), com as notas de fala: https://halo.bungie.org/misc/gdc.2002.haloai/talk.html
- Damian Isla, "Handling Complexity in the Halo 2 AI" (GDC 2005): https://www.gamedeveloper.com/programming/gdc-2005-proceeding-handling-complexity-in-the-i-halo-2-i-ai
- Nemesis, DICE 2015 (GamesBeat): https://gamesbeat.com/shadow-of-mordors-nemesis-system-draws-from-burnout-football-and-an-architecture-book/
- Nemesis, GDC 2018 (PC Gamer): https://www.pcgamer.com/the-guy-who-designed-shadow-of-wars-orcs-is-super-proud-that-theyre-such-dicks/
- Steve Rabin, "The Illusion of Intelligence" (Game AI Pro 3): http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter01_The_Illusion_of_Intelligence.pdf
- Kevin Dill, "What Is Game AI?" (Game AI Pro): http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter01_What_is_Game_AI.pdf
- Soren Johnson, "Our Cheatin' Hearts": https://www.designer-notes.com/game-developer-column-7-our-cheain-hearts/
- Mick West, "Intelligent Mistakes": https://www.gamedeveloper.com/programming/intelligent-mistakes-how-to-incorporate-stupidity-into-your-ai-code
- Tynan Sylvester, "The Simulation Dream": https://www.gamedeveloper.com/design/the-simulation-dream
- Lankoski e Björk (DiGRA 2007): https://dl.digra.org/index.php/dl/article/download/262/262
- Denisova e Cairns (CHI PLAY 2015): https://www-users.york.ac.uk/~pc530/pubs/Denisova2_CHIPLAY15.pdf
- Takayama, Dooley e Ju (HRI 2011): https://www.wendyju.com/publications/151r-takayama.pdf

**Terror e som**

- Thomas Grip, "The tricky design problem of SOMA's memorable monsters": https://www.gamedeveloper.com/design/the-tricky-design-problem-of-i-soma-i-s-memorable-monsters
- Thomas Grip, "4-Layers, A Narrative Design Approach": https://www.gamedeveloper.com/design/4-layers-a-narrative-design-approach
- Thomas Grip, "The SSM Framework of Game Design": https://www.gamedeveloper.com/design/the-ssm-framework-of-game-design
- Thomas Grip, "The Complexity Fallacy": https://www.gamedeveloper.com/design/the-complexity-fallacy
- Fredrik Olsson sobre Amnesia: The Bunker: https://gamingbolt.com/amnesia-the-bunker-interview-semi-open-world-structure-stalker-and-more
- O modo sem morte do SOMA (Game Informer): https://gameinformer.com/b/features/archive/2017/12/06/soma-s-safe-mode-proves-horror-games-can-work-without-danger
- O modo sem morte do SOMA (GameCritics): https://gamecritics.com/corey-motley/soma-safe-mode-review/
- Phasmophobia, "Game not as scary as it used to be" (Steam): https://steamcommunity.com/app/739630/discussions/0/4132683013933644876
- Skinwalkers: https://thunderstore.io/c/lethal-company/p/RugbugRedfern/Skinwalkers/
- Mirage: https://github.com/qwbarch/mirage
- MIMESIS, entrevista (em japonês): https://www.gamebusiness.jp/article/2026/08/08/27651.html
- Relatos sobre o Skinwalkers (Steam): https://steamcommunity.com/app/1966720/discussions/0/4034727291140348454 e https://steamcommunity.com/app/1966720/discussions/0/4038103329148535905
- Darkwood (PlayStation Blog): https://blog.playstation.com/archive/2019/05/01/darkwood-is-a-ps4-survival-horror-that-favours-creepy-atmosphere-over-cheap-jump-scares/
- Garner e Grimshaw, "A Climate of Fear" (2011): https://vbn.aau.dk/ws/files/61576216/climateOfFear_MG.pdf
- Áudio do Alien: Isolation (MCV): https://mcvuk.com/?p=75051
- Naughty Dog sobre o Clicker: https://naughtydog.com/blog/the_last_of_us_part_i_hbo_tv_show_clickers
- Celia Wagar, "Making Good Horror": https://critpoints.net/2016/03/22/making-good-horror/
- Randy Thom, "Designing Creature Voices, Part 2": https://sound-ideas.com/blogs/sound-ideas/designing-creature-voices-part-2
- Diel e Lewis (2024), vale da estranheza vocal: https://doi.org/10.1016/j.chbr.2024.100430
- The Blackout Club (80.lv): https://80.lv/articles/the-blackout-club-horror-elements-atmosphere

**Mods de Minecraft**

- Don't Let It Learn: https://www.curseforge.com/minecraft/mc-mods/dont-let-it-learn e o tópico https://www.reddit.com/r/feedthebeast/comments/1wygibp/
- Araz (The Anomaly): https://www.curseforge.com/minecraft/mc-mods/araz-the-anomaly
- From The Fog: https://modrinth.com/mod/from-the-fog e https://github.com/LunarEclipseStudios/From-The-Fog
- The Man From The Fog: https://modrinth.com/mod/the-man-from-the-fog
- Cave Dweller Evolved: https://modrinth.com/mod/cave-dweller-evolved
- The Hollow (`hollow-dread`): https://modrinth.com/mod/hollow-dread
- The Mimicer: https://modrinth.com/mod/the-mimicer
- Existence: https://modrinth.com/mod/existence e https://github.com/CipherXOR/Existence
- Copycat | Mimic: https://modrinth.com/mod/copycat-mimic
- Mimicked: https://modrinth.com/mod/mimicked
- The Obsessed: https://www.curseforge.com/minecraft/mc-mods/obsessed
- The Knocker: https://modrinth.com/mod/the-knocker
- Understudy: https://www.curseforge.com/minecraft/mc-mods/understudy-horror
- LocalHost: https://modrinth.com/mod/localhost-horror
- TheWatcher: https://modrinth.com/mod/thewatcher e https://github.com/Al-Capone11/TheWatcher
- GeckoLib, versões: https://modrinth.com/mod/geckolib/versions

**Atribuição de mente e leitura visual**

- Heider e Simmel (1944): https://www.cs.uky.edu/~sgware/reading/papers/heider1944experimental.pdf
- Scholl e Tremoulet (2000): https://www.gwern.net/doc/philosophy/mind/2000-scholl.pdf
- Gao, Newman e Scholl (2009), "The psychophysics of chasing": https://perception.yale.edu/papers/09-Gao-Newman-Scholl-CogPsych.pdf
- Gao, McCarthy e Scholl (2010), "The wolfpack effect": https://perception.yale.edu/Brian/demos/Animacy-Wolfpack.html
- Maij, van Schie e van Elk (2019): https://pure.uva.nl/ws/files/32525276/2153599X.2017.pdf
- McAndrew e Koehnke (2016), "On the nature of creepiness": https://www.gwern.net/doc/psychology/collecting/2016-mcandrew.pdf
- Butko e Movellan (2010), sobre Watson (1972): https://mplab.ucsd.edu/46/media/2010-ButkoMovellan-NeuNet-Contingency.pdf
- Mareschal, Calder e Clifford (2013): https://pmc.ncbi.nlm.nih.gov/articles/PMC3918857
- Tom Leonard, "Building an AI Sensory System" (Thief): https://www.gamedeveloper.com/programming/building-an-ai-sensory-system-examining-the-design-of-i-thief-the-dark-project-i-
- Jamey Pittman, "The Pac-Man Dossier": https://www.gamedeveloper.com/design/the-pac-man-dossier
- Craig Reynolds, "Steering Behaviors For Autonomous Characters": https://www.red3d.com/cwr/steer/gdc99/
- Travis McIntosh, "Human Enemy AI in The Last of Us": http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter34_Human_Enemy_AI_in_The_Last_of_Us.pdf
- Mirnig e colegas (2017), "To Err Is Robot": https://www.frontiersin.org/articles/10.3389/frobt.2017.00021/full
- Smith e Worch, "What Happened Here?" (GDC 2010, transcrição): https://slidetodoc.com/level-design-workshop-what-happened-here-environmental-storytelling/
- Mori, "The Uncanny Valley" (tradução de 2012): https://spectrum.ieee.org/the-uncanny-valley
- Chattopadhyay e MacDorman (2016): https://pmc.ncbi.nlm.nih.gov/articles/PMC5024669
