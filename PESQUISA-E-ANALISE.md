# Sussurros — pesquisa e análise (SPOILERS)

Registro do que foi encontrado em 07–08/10/2026: leitura do código, análise de um log de jogo real e pesquisa de outros mods de terror. O que fazer com isso está no `PLANO-MECANICAS.md`.

**Contexto que muda as prioridades:** o mod não vai ser publicado. Ele existe para assustar o dono e, talvez, alguns amigos num servidor pequeno. Então página de loja, documentação para administrador e compatibilidade de licença pesam pouco; o que pesa é o susto funcionar, inclusive entre amigos.

**De onde vêm os números de linha:** da branch `refactor/passo-3-cenas` (commit `11a62ca`), que já contém o PR #2 e o PR #3. Na `main` os mesmos trechos ainda estão dentro do `Diretor.java`. Os nomes das funções valem para as duas.

**Como ler as marcas de confiança da pesquisa**

- **[C]** o código-fonte foi lido no repositório.
- **[P]** a página ou o artigo foi lido.
- **[B]** veio só de um resultado de busca.
- **(não verificado)** é memória ou inferência.

---

## 1. Veredito

O cérebro do mod (o Diretor) é a parte forte: ritmo com trégua, memória de lugares, leitura de reação e falsos positivos são coisas que a maioria dos mods de terror não tem. O ponto fraco é o que chega ao jogador, som e imagem. E o log mostra que o cérebro está aprendendo com dados ruins, porque vários eventos não podiam ser ouvidos nem vistos.

---

## 2. O que o log mostrou

Sessão analisada: `sussurros-debug.log`, dos 480 s aos 2640 s (jogador `Jogador`, branch `fix/isca-fio-alteracoes`).

| Medida | Valor |
|---|---|
| Eventos do Diretor entre 600 s e 2640 s | 23 |
| Presságios e cenas da Atmosfera no mesmo período | 9 |
| Ritmo médio | um acontecimento a cada ~65 s |
| Reações medidas | 22 |
| Reações com confiança zero | 14 |
| Reações claras (confiança de 0,35 ou mais) | 7 |
| Aparições naturais do Hóspede | 0 |
| Aparições vindas do Olho | 3 (2503 s, 2531 s, 2619 s) |
| Chegada à fase 3 | 2361 s, cerca de 39 min (o design diz 55 min) |

Observações:

- As três aparições nasceram de dia (luz 14–15), em campo aberto, sem cobertura, a 33–37 blocos, e sumiram cerca de 1 s depois de serem encaradas.
- A inquietação chegou a 200 (o máximo) e ficou lá do segundo 2100 em diante. No subsolo escuro ela sobe 4 por segundo, então satura em menos de um minuto. Com ela acima de 100 o tempo de assombração corre em dobro, por isso a fase 3 chegou cedo.
- Em 1557 s a leitura registrou velocidade de 35,9 blocos por segundo: foi um teleporte ou respawn no meio de uma medição.

---

## 3. Problemas encontrados

Do mais grave ao menos grave. Os itens 1 e 8 foram conferidos no bytecode do Minecraft 26.2 instalado na máquina.

### 3.1 `SINAL_DISTANTE` nunca é audível

- **Onde:** `Atmosfera.sinalDistante` (linha 507).
- **O que acontece:** o som toca a 18–42 blocos com volume 0,32 ou 0,45. Para volume até 1, o servidor só envia o som a quem está a até 16 blocos (`SoundEvent.getRange`: `volume > 1 ? 16 * volume : 16`). Então ninguém recebe esse som.
- **Mesma falha, em parte:** `Atmosfera.ruidoRetorno` (pontos a 9–30 blocos com volume 0,34 ou 0,52), `Atmosfera.passagem` (10–16 blocos com volume 0,38, quase inaudível no fim) e o presságio `RUIDO_NA_PAREDE` (9–18 blocos com volume 0,36).
- **Causa:** falta o `Diretor.volumePara`, que o resto do mod usa para aumentar o alcance com a distância.
- **No log:** 751 s (29,8 blocos), 1049 s (20,2), 1297 s (42,6), 2284 s (34,1).
- **Como conferir:** parado num lugar quieto, `/sussurros evento sinal_distante`. O log registra o evento; não se ouve nada.

### 3.2 O Diretor aprende com reações que não existiram

- **Onde:** `Leitura.avaliar` (linhas 153–178).
- **O que acontece:** "investigou" vale para quem anda 3 blocos na direção da fonte, e "fugiu" vale para quem acelera ou começa a correr. Nenhum dos dois compara com o que o jogador já estava fazendo. Como muitas fontes ficam no próprio Rastro, voltar pelo caminho conta como investigar.
- **Consequência:** "investigou" também marca "percebeu", que sobe a observabilidade para 0,80 e faz o aprendizado pesar mais.
- **No log:** em 1052 s o Diretor registrou confiança 0,56 e abriu uma cadeia por causa de um `SINAL_DISTANTE` inaudível (item 3.1). Em 823 s marcou "fugiu" e "investigou" ao mesmo tempo, o que é contraditório.

### 3.3 Um som tocou a 236 blocos

- **Onde:** `Diretor.sinalFalso`, ramo `SUBSOLO` (linhas 1820–1826).
- **O que acontece:** sem ponto do Rastro por perto, o som cai no lugar da quebra antiga. `CenaAlgoNoTunel.sortearQuebraRecente` filtra por idade (até 360 s), não por distância.
- **Consequência:** o evento contou para pressão, agenda e aprendizado sem ser ouvido.
- **No log:** 2192 s, `dist=236.6`.

### 3.4 Evento sorteado que não consegue executar queima o ciclo

- **Onde:** `Diretor.decidir` (linhas 799–803) e `Atmosfera.podeEvento` contra as funções de execução.
- **O que acontece:** a checagem prévia exige menos do que a execução. Exemplo: `TRILHA_INTERROMPIDA` passa na checagem com orçamento 1,8 e Rastro de 4 pontos quaisquer, mas a execução pede orçamento 2,0 e três pontos a 5–26 blocos com 8 a 180 s de idade. Além disso, a agenda (`proximoEvento`) é empurrada antes de saber se o evento rodou.
- **Consequência:** com vulnerabilidade alta ele tenta de novo a cada segundo, gastando sorteios; com vulnerabilidade baixa perde o ciclo inteiro.
- **No log:** 1188–1190 s (três sorteios seguidos), 1614 s e 1640 s. `TRILHA_INTERROMPIDA` foi sorteada duas vezes e nunca aconteceu.

### 3.5 Limiar de giro em degrau

- **Onde:** `Leitura.avaliar` (linha 131).
- **O que acontece:** o giro só conta a partir de 60°. Virar 58° e olhar para a fonte vale zero.
- **No log:** 1701 s, `LUZ_ERRADA`, `giro=58 olhou=true c=0.00`.

### 3.6 A punição por indiferença contou eventos que não dava para perceber

- **Onde:** `Diretor.concluirLeitura` (linhas 1338–1347).
- **O que acontece:** seis eventos seguidos sem reação avançam a assombração em 300 s. Entraram na conta cinza no chão, passos baixos durante a mineração, um passo no instante do teleporte e a luz do item 3.5.
- **No log:** 1967 s, `indiferença: assombração avança 300 s`.

### 3.7 O Olho virou botão de invocar

- **Onde:** `Diretor.usarOlho` (linhas 3075–3086) e `Diretor.criaturaFoiVista`.
- **O que acontece:** na fase 3+, sem criatura e sem vestígio por perto, cada uso tem 40% de chance de chamar uma aparição alguns segundos depois, sem recarga. Aparições de origem `OLHO` contam para `VEZES_VISTO`, que aumenta a ousadia.
- **No log:** três usos em dois minutos, três aparições.

### 3.8 Vidro e folhas escondem a criatura do próprio mod

- **Onde:** `Diretor.estaOlhando`, que usa `p.hasLineOfSight(alvo)`.
- **O que acontece:** essa função usa o modo `COLLIDER` e mira só na altura dos olhos do alvo. Vidro e folhas têm colisão, então bloqueiam. O jogador enxerga o Hóspede por uma janela ou entre folhas, mas o mod conclui que não: ele não some e o Diretor não registra o avistamento.
- **Observação:** existe a variante `hasLineOfSight(alvo, ClipContext.Block.VISUAL, ...)`, e a cena da janela já contorna o problema medindo só a direção.

### 3.9 O campo de visão é um palpite

- **Onde:** `HospedeEntity.CONE_TELA_SEGURA` (0,57, cerca de 55°) e `CONE_PERCEBEU` (0,70, cerca de 45°).
- **O que acontece:** o cone cobre a tela com FOV 70 em 16:9. Com FOV 90 a metade horizontal da tela chega a ~61°, com FOV 110 a ~68°, e correr já alarga o FOV. `PRESENCA` nasce a 55–80° do olhar, então nesses casos pode nascer dentro da tela.

### 3.10 Tochas removidas de verdade

- **Onde:** `Diretor.aoAcordar` (linhas 2714–2727): até três tochas perto da cama, sem drop, na fase 3+. Também `HospedeEntity.apagarTochaProxima` (na caça) e `Diretor.roubarTocha` (fase 4).
- **O que acontece:** contraria o princípio 5 do `ROADMAP.md`. O caso do acordar não está no `DIVIDAS-DESIGN.md`.

### 3.11 Menores

- Teleporte, respawn e troca de dimensão não cancelam a leitura em andamento nem limpam o Rastro e as ações guardadas.
- Todo som do mod usa `level.playSound(null, ...)`, que envia para qualquer jogador no alcance. Em multiplayer, um amigo ouve a assombração do outro. O Hóspede também é visível para todos.
- `Diretor.angulosPresencaAdaptativa` calcula o nome da estratégia e não o registra (já anotado como dívida).

---

## 4. Som e atmosfera

- **Sussurro não tem som.** `Diretor.sussurro`, `ecoDaFala` e o evento `VISTO` só mandam texto para cima da hotbar.
- **Biblioteca pequena.** Seis sons próprios, doze arquivos de 5 a 19 KB, todos toques curtos e sintetizados. Não há camada de fundo nem variação por lugar.
- **O silêncio não é silêncio.** A trégua significa "sem eventos"; a música e o ambiente do jogo continuam.
- **Sumiço seco.** `HospedeEntity.sumir` chama `discard()`: a criatura some de um quadro para o outro, na frente do jogador.
- **Vestígios quase invisíveis.** Partícula de cinza e um bloco de pedregulho numa caverna não chamam atenção.
- **Nada roda no cliente.** O lado cliente tem um registro de renderizador e o modelo. Não há pacotes próprios nem mixins.

---

## 5. Estrutura e git (em 08/10/2026)

- A `main` só tem o passo 1 da refatoração. O PR #3 (correções) e o PR #2 (rascunho) estão abertos; `refactor/passo-3-cenas` está empilhada em cima dos dois, sem PR.
- O `Diretor` tem 4.719 linhas na `main` e 3.615 depois do passo 3. Ainda guarda os eventos, a lógica dos itens, a cama e os helpers de comando.
- Só o `Seletor` tem teste. A `Leitura` é matemática pura e foi onde apareceram mais defeitos.
- Os números de ajuste estão fixos no código.

---

## 6. O mod do Reddit

**Don't Let It Learn** (criatura "The Hollow"), Forge 1.20.1, alpha 0.1.0, código fechado. [P]

- Página: https://www.curseforge.com/minecraft/mc-mods/dont-let-it-learn
- Post: https://www.reddit.com/r/feedthebeast/comments/1wygibp/i_made_a_horror_mod_that_studies_you_instead_of/ (lido pelo arquivo Arctic Shift, porque o Reddit bloqueou o acesso direto)

**O que faz:** cinco encontros (escanear construção, observar de longe, observar pela janela, aparecer à frente numa rota aprendida, escanear o jogador). Não mata nem destrói. Estágios atuais: observar, aprender, prever; planejados: imitar e substituir.

**Como "estuda":** regras com dados salvos, sem IA de verdade. Guarda construções, encontros, reações e com que frequência o jogador vai de uma área a outra. Não tem relógio fixo: pode decidir não fazer nada.

**Elogios:** parece inteligente sem "correr gritando"; o objetivo dele não é óbvio; pedidos de interação sem combate (fazer amizade, enganar o que ele aprende).

**Críticas:** falta de documentação dos comportamentos (comentário mais votado, 601 pontos); "o que ele faz além de olhar?" (280 pontos); jeitos fáceis de escapar (base no céu, muralha, invisibilidade); "depois que você morre uma vez, perde a graça"; desconfiança por o post ter sido escrito com IA.

**Parecido:** *Understudy* (Forge 1.20.1, fechado): mantém um registro do jogador, tem a entidade "Yesterday" (um replay do seu dia anterior), modo "Builder safe" e efeitos de quarta parede que podem ser desligados. https://www.curseforge.com/minecraft/mc-mods/understudy-horror [P]

---

## 7. Mods de referência

O repositório do Sussurros é CC0. Código MIT ou BSD pode ser reaproveitado mantendo o aviso de licença; GPL, LGPL e CC BY-NC-SA servem para estudar e reescrever. Fechado ("All Rights Reserved") é só inspiração.

| Mod | Licença e link | O que estudar |
|---|---|---|
| Him (Fabric 1.21.4) | CC-BY-4.0 no Modrinth, sem arquivo de licença [C][P] — https://github.com/Yegiyan/Him | ~2.000 linhas na mesma base: posição fora do cone, três relógios, linha de chat falsa, susto ao dormir |
| Man From The Fog Reimagined (Fabric 1.20.1) | GPL-3.0, arquivado [C] — https://github.com/zenolth/man-from-the-fog-reimagined | Visão checada no cliente, som e entidade por jogador, eco de chat, alvo anti-grupo |
| From The Fog | CC BY-NC-SA 4.0, datapack [C] — https://github.com/LunarEclipseStudios/From-The-Fog | Mais de 20 mecânicas nomeadas; registro de blocos colocados; sonda invisível antes de aparecer |
| Cave Dweller Evolved | MIT, descontinuado [P][C] — https://github.com/SiverDX/cave_dweller (port Fabric: https://github.com/Thiov/cave_dweller-fabric) | Sorteio perseguir/encarar/fugir, contagem de olhadas, ruído como presságio |
| Sanity: Descent Into Madness (Forge) | MIT [C] — https://github.com/croissantnova/SanityDescentIntoMadness | Passos falsos, camadas de som em loop, troca de sons de animais |
| Eyes in the Darkness | BSD-3 [C] — https://github.com/gigaherz/EyesInTheDarkness | Olhos no escuro, teste de olhar preciso, relógio real |
| Weeping Angels | Código visível, todos os direitos reservados [C] — https://github.com/Jeryn99/Weeping-Angels | Raio de 8 cantos, "ver através" de blocos, apagar luzes |
| The Midnight Lurker | MIT [P] — https://github.com/Voxla/midnightlurker | Variantes: de costas, escondido, seguidor invisível |
| The Legend of Herobrine | LGPL-3.0 [C] — https://github.com/Alex-MacLean/TheLegendOfHerobrine | Construtor com modelos de estrutura |
| Sound Physics Remastered | GPL-3.0 [C] — https://github.com/henkelmax/sound-physics-remastered | Abafamento atrás de parede |
| AmbientSounds | LGPL-3.0 [C] — https://github.com/CreativeMD/AmbientSounds | Classificar o ambiente por "bolsão de ar" |
| The Broken Script | Fechado [P] — https://modrinth.com/mod/the-broken-script (eventos: https://thebrokenscript.wiki.gg/wiki/Random_Events) | Ideias de quarta parede |
| The Obsessed | Fechado [P] — https://www.curseforge.com/minecraft/mc-mods/obsessed | Humor e oferendas, imitar construção e chat, modo intruso |
| The Knocker, The One Who Watches, The Anomaly | Fechados [P] | Só o conceito |

---

## 8. Catálogo de ideias

Marcação: **novo** (o Sussurros não tem), **parcial** (tem algo parecido), **já tem**. Nível: leve, médio, grande.

### Aparições

| # | Ideia | Nível | Vem de | Situação |
|---|---|---|---|---|
| 1 | Observador na janela | médio | From The Fog [C], Don't Let It Learn [P] | já tem (cena "Do outro lado do vidro") |
| 2 | Espiar de trás da cobertura e escorregar para fora de vista em vez de sumir | leve | From The Fog `slip_behind` [C] | parcial (escolhe cobertura, não tem a saída) |
| 3 | Aparecer no ponto para onde o jogador está indo | médio | Don't Let It Learn [P] | parcial (usa rotas como lugar, não prevê destino) |
| 4 | Figura de costas, que não reage | leve | Midnight Lurker [C, pelo nome] | novo |
| 5 | Só se aproxima quando você não olha, com limite de olhadas | médio | Cave Dweller [C], Weeping Angels [C] | já tem (espreita e caça) |
| 6 | Olhos que só existem no escuro | leve | Eyes in the Darkness [C] | novo |
| 7 | Rebanho que encara | leve | From The Fog [C] | já tem |
| 8 | "Ontem": uma figura refaz o caminho do jogador | médio | Understudy [P], The Obsessed [P] | novo |
| 9 | Relance: só existe no canto da tela e some antes de ser encarado | leve | pedido do dono; Eyes in the Darkness some com olhar preciso [C] | novo |
| 10 | Nem sempre some ao ser encarado (inclina a cabeça, dá um passo, foge) | médio | From The Fog `tilt_head` [C], Man From The Fog `StareState` [C] | novo |

### Som

| # | Ideia | Nível | Vem de | Situação |
|---|---|---|---|---|
| 11 | Passos com trajetória e material certo | leve | From The Fog [C], Sanity [C] | já tem |
| 12 | Os animais emudecem ou soam errado | leve | Sanity `MixinClientLevel` [C] | novo |
| 13 | Camada contínua ligada à pressão (drone, coração) | leve | Sanity [C] | novo |
| 14 | Cortar tudo no pico em vez de tocar um acorde | médio | Akira Yamaoka [P] | novo |
| 15 | Som abafado atrás da parede | leve | Sound Physics Remastered [C] | novo |
| 16 | Batida na porta habitual | médio | The Knocker [P] | já tem |
| 17 | Som familiar na metade do tom | leve | Sanity [C], Him [C] | novo |
| 18 | Som-assinatura que avisa, e às vezes mente | leve | Cave Dweller [C], Left 4 Dead [P] | parcial (anúncio de 2 passos) |
| 19 | Legenda fantasma | leve | From The Fog [C] | novo |
| 20 | Cães como detector | leve | From The Fog [P], The Obsessed [P] | parcial (animais reagem) |
| 21 | Sussurro com voz, sem direção | leve | Cave Dweller (som na posição do ouvinte) [C] | novo |
| 22 | Som só para o alvo | leve | Man From The Fog [C], Weeping Angels [C] | novo |

### Mundo

| # | Ideia | Nível | Vem de | Situação |
|---|---|---|---|---|
| 23 | Só mexer no que o jogador colocou; tocha vira de redstone | leve | From The Fog [C] | parcial (mexe em tochas, sem registro de dono) |
| 24 | Presente no baú e livro com dados do jogador | médio | From The Fog [C], The Broken Script [P] | novo |
| 25 | Placa com as palavras do jogador | médio | From The Fog [P][C], The Obsessed [P] | novo |
| 26 | Quadros que mudam quando não se olha | leve | From The Fog [C] | novo |
| 27 | Marcos estranhos longe da base | médio | From The Fog [C], Him [C] | já tem (três estruturas) |
| 28 | Minerador fantasma | médio | From The Fog [C][P] | parcial (eco da mineração) |
| 29 | Cópia torta da casa do jogador | grande, lento | The Obsessed [P], Understudy [P] | novo |
| 30 | Blocos que só um jogador vê | leve | ideia própria; técnica comum em plugins | novo |
| 31 | A luz cede perto dele (velas, lanternas) | médio | Weeping Angels [C] | parcial (só tochas) |
| 32 | "Ele entrou enquanto você estava fora" | médio a grande | The Obsessed [P] | parcial (cena "Ele voltou com você") |

### Quarta parede

| # | Ideia | Nível | Vem de | Situação |
|---|---|---|---|---|
| 33 | Entrada ou saída falsa de jogador; a própria entrada repetida | médio | The Broken Script [P], From The Fog [P] | novo |
| 34 | Eco do chat com um erro | médio | Man From The Fog [C], The Obsessed [P] | parcial (eco sem erro, só texto) |
| 35 | Título da janela e número de versão | médio | The Broken Script [P], Understudy [P] | novo |
| 36 | Caderno de observações em arquivo | médio | Understudy [P] | novo |
| 37 | Conquista oculta | leve | The Broken Script [P], From The Fog [C] | novo |
| 38 | Erros falsos, falsa desconexão, brilho resetado | médio a grande | The Broken Script [P], Eternal Darkness [P] | novo |
| 39 | A pausa não protege | médio | Him (só a fala) [C] | novo |
| 40 | Relógio real (mais ativo de madrugada) | leve | Eyes in the Darkness [C] | novo |

### Ritmo e direção

| # | Ideia | Vem de | Situação |
|---|---|---|---|
| 41 | Fases subir, sustentar, esvaziar e relaxar; a intensidade não cai enquanto houver ameaça | Left 4 Dead [P] | parcial |
| 42 | Imprevisibilidade estruturada: vários relógios sobrepostos e cartas "nada" | Left 4 Dead [P], Him [C] | parcial (três relógios; "nada" só nas cadeias) |
| 43 | O diretor nunca diz à criatura exatamente onde o jogador está | Alien: Isolation [P] | já tem (`HospedeBusca`) |
| 44 | Comportamentos destravados pelo hábito do jogador | Alien: Isolation [P] | parcial |
| 45 | Auto-reconfiguração: o mod sorteia a própria configuração por dia | From The Fog "Give Him Control" [C] | novo |
| 46 | Humor, oferendas e contra-jogo real | The Obsessed [P], pedidos no Reddit [P] | parcial (cinco itens) |
| 47 | Nunca matar: o contato vira apagão e deslocamento | Thomas Grip [P], The Obsessed [P] | parcial (5 de dano e some) |
| 48 | Sistema vago de propósito | Thomas Grip sobre o Amnesia [P] | já tem |

### Multiplayer

| # | Ideia | Nível | Vem de | Situação |
|---|---|---|---|---|
| 49 | O alvo é quem está sozinho | médio | Man From The Fog [C], The Broken Script [P] | parcial (estar só soma vulnerabilidade) |
| 50 | Realidades divergentes: um vê e ouve, o outro não | leve | Man From The Fog [C] | novo |
| 51 | Imitar um amigo (nome, chat) | médio | Man From The Fog `MimicEntity` [C] | novo |
| 52 | Voz gravada dos jogadores | grande | Mimicked [P] | novo |

### Sustos maiores

| # | Ideia | Vem de | Situação |
|---|---|---|---|
| 53 | Golpe por não olhar | From The Fog "Sneaky Strike" [C][P] | novo |
| 54 | Salto de aproximação com zoom e coração | SCP: Containment Breach [C] | novo |
| 55 | Visita ao pé da cama e sono negado | Him [C], From The Fog [P] | novo |
| 56 | Tremor de projeção e neblina que fecha | Man From The Fog [C] | novo |

---

## 9. Técnicas vistas em código real

**"Está olhando para mim?"**

- Cone por produto escalar no servidor: Him (`Stalk.java`, `StalkPlayerGoal.java`). [C]
- Olhar preciso, no estilo do Enderman: Eyes in the Darkness (`EyesEntity.java`). [C]
- Dois limiares (0,99 para "diretamente", 0,3 para "virado para mim"): Cave Dweller (`CaveDwellerEntity.java`). [C]
- Raio para os oito cantos e blocos que não tampam a visão: Weeping Angels (`ViewUtil.java`). [C]
- Frustum real no cliente, enviado ao servidor a cada 5 ticks: Man From The Fog Reimagined (`ModClientEvents.java`, `MonitorPlayerLineOfSight.java`). É o mais fiel, porque respeita FOV e zoom. [C]

**Aparecer fora de vista**

- Him `getSpawnPosition`: 100 tentativas, topo sem folhas, dois blocos de ar, fora do cone. [C]
- From The Fog: perseguição a 25–46 blocos, espreita a 50–100; usa uma sonda invisível e refaz o avistamento se o olhar do jogador achar a sonda antes do modelo existir. [C]
- Cave Dweller `Utils.trySpawnMob`: 40 tentativas; pode exigir caminho alcançável até a vítima. [C]

**Reação ao ser visto**

- Cave Dweller: sorteia perseguir, encarar, encarar ou fugir. No encarar, congela quando olhado; depois de 8 a 15 olhadas, 10% some e 30% sorteia de novo. [C]
- Man From The Fog: 4 s olhado leva a sumir, perseguir ou fugir; 12 s sem ser olhado leva a rondar. [C]

**Som**

- Som para um jogador só: enviar o pacote de som direto para ele (Man From The Fog `ParanoiaStatusEffect`; Weeping Angels com volume 0,25 e trava de 5 s). [C]
- Som "dentro da cabeça": pacote próprio e, no cliente, um som na posição do ouvinte (Cave Dweller `HandleCaveSound.java`). [C]
- Passos falsos: som do bloco sob o jogador, 2 blocos atrás, 2 a 4 passos a cada 7 ticks, volume 0,5, recarga de 600 a 1200 ticks (Sanity `SoundPlayback.java`). [C]
- Camadas em loop com volume guiado por variável: Sanity `InsanitySoundInstance.java`. [C]
- Trocar ou cancelar sons do mundo: Sanity `MixinClientLevel.java`. [C]

**Mundo, chat e tela**

- Registro por tipo de bloco colocado e sorteio só entre eles: From The Fog (`block_events/detection/placed/`). [C]
- Linha de chat falsa: Him transmite `<§kHerobrine§r> ...`. [C]
- Eco de chat: Man From The Fog guarda mensagens por jogador com validade e as reenvia por uma entidade imitadora. [C]
- Tremor na matriz de projeção e neblina interpolada, sem shader: Man From The Fog (`GameRendererMixin`, `BackgroundRendererMixin`). [C]

**Ritmo**

- Him: três contadores com intervalo sorteado, refeitos a cada disparo. [C]
- From The Fog: próximo avistamento em 60–120 s de dia e 30–60 s à noite, dividido pelo multiplicador de atividade; atraso inicial de 3 dias; ao ser visto, atividade em dobro por 360 s. [C]
- Left 4 Dead: sustentar o pico por 3–5 s, esperar uma pausa natural, relaxar por 30–45 s; "o algoritmo ajusta o ritmo, não a dificuldade". [P] https://steamcdn-a.akamaihd.net/apps/valve/2009/ai_systems_of_l4d_mike_booth.pdf
- Alien: Isolation: o diretor sabe onde o jogador está, mas dá à criatura só a área geral; no limite do medidor de ameaça, manda a criatura para os bastidores. [P] https://www.gamedeveloper.com/design/the-perfect-organism-the-ai-of-alien-isolation

---

## 10. Armadilhas vistas em outros mods

| Armadilha | Fonte | Vale para o Sussurros? |
|---|---|---|
| "Só observa; e daí?" | Reddit, 280 pontos [P] | Em parte: 44 min sem aparição natural no log |
| Morte e respawn matam o medo | Reddit [P]; Thomas Grip [P] | Pouco: o toque dá 5 de dano e some |
| Susto barato e monstro gritando | Reddit [P] | Não |
| O mod vira o foco e cansa em horas | Reddit [P] | A observar em sessões longas |
| Jeitos fáceis de escapar | Reddit [P] | Sim: "subsolo" depende da luz do céu e só o Overworld funciona |
| Estragar construção | https://github.com/Yegiyan/Him/issues/12 [P] | Sim: tochas ao acordar e na fase 4 |
| Frequência alta | https://github.com/Yegiyan/Him/issues/7 [P] | Sim: um acontecimento a cada ~65 s |
| Som para todo mundo | https://github.com/SiverDX/cave_dweller/issues/12 [P] | Sim: todos os sons |
| Som numa categoria que o jogador zerou | https://github.com/zenolth/man-from-the-fog-reimagined/issues/17 [P] | A conferir com os amigos: os sons próprios usam "criaturas hostis" |
| Detecção através de paredes | https://github.com/SiverDX/cave_dweller/issues/10 [P] | Não: combina ângulo e linha de visão |
| Quarta parede sem opção de desligar | https://github.com/zenolth/man-from-the-fog-reimagined/issues/4 [P] | Ainda não se aplica |
| Varredura de blocos pesada | Him varre ~17 milhões de blocos por jogador [C] | Não: a varredura do Sussurros é de 5.625 blocos, no máximo a cada 10 s |

---

## 11. O que o Minecraft 26.2 oferece (conferido nos jars da máquina)

Conferido com `javap` em `~/.gradle/caches/fabric-loom/26.2/`. "Existe" quer dizer que a classe e o método estão lá; nenhum foi testado em jogo.

| Para quê | O que existe |
|---|---|
| Som para um jogador só | `ClientboundSoundPacket(Holder<SoundEvent>, SoundSource, x, y, z, volume, pitch, seed)` enviado por `ServerPlayer.connection.send(...)` |
| Parar música ou ambiente de um jogador | `ClientboundStopSoundPacket(Identifier, SoundSource)` |
| Bloco que só um jogador vê | `ClientboundBlockUpdatePacket(BlockPos, BlockState)`; para desfazer, `ClientboundBlockUpdatePacket(BlockGetter, BlockPos)` |
| Criatura visível só para o alvo | `Entity.broadcastToPlayer(ServerPlayer)`, que pode ser sobrescrito |
| Calar um animal | `Entity.setSilent(boolean)` |
| Linha de visão que ignora o que é transparente | `LivingEntity.hasLineOfSight(Entity, ClipContext.Block.VISUAL, ClipContext.Fluid, double)` |
| Saber o que está na tela (cliente) | `GameRenderer.mainCamera()`, `Camera.getCullFrustum()`, `Frustum.isVisible(AABB)`, `Camera.getFov()`, `Camera.forwardVector()` |
| Sumir aos poucos (cliente) | `LivingEntityRenderer.getModelTint(S)` e `getRenderType(S, boolean, boolean, boolean)` |
| Som em loop e sem posição (cliente) | `AbstractTickableSoundInstance`, `SimpleSoundInstance.forUI(...)`, `SoundManager.play/stop`, `MusicManager.stopPlaying()` |
| Brilho e FOV do jogador (cliente) | `Options.gamma()`, `Options.fov()`, `Options.fovEffectScale()` |
| Pacotes próprios | `PayloadTypeRegistry`, `ServerPlayNetworking`, `ClientPlayNetworking` (Fabric API 0.155.2) |
| Respawn, entrada, saída, troca de dimensão | `ServerPlayerEvents.AFTER_RESPAWN/JOIN/LEAVE`, `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` |
| Livro escrito, placa, conquista | `DataComponents.WRITTEN_BOOK_CONTENT`, `SignBlockEntity.setText`, `PlayerAdvancements.award` |
| Mensagem de saída de jogador | chave de tradução `multiplayer.player.left` |
| Figura com a pele de um jogador | `net.minecraft.world.entity.decoration.Mannequin`, com um perfil (`ResolvableProfile`). O `setProfile` é privado, então falta testar como definir o perfil pelo código |
| Respiração sem gravar nada | `SoundEvents.PLAYER_BREATH` (som do jogo que quase nunca toca) e `SoundEvents.AMBIENT_CAVE` |

**Não existe pronto:** um evento do Fabric para "o jogador colocou um bloco". Há `ItemEvents.USE_ON` e `BlockEvents.USE_ITEM_ON`; falta conferir se servem ou se é preciso um mixin.

---

## 12. Segunda rodada: o que os jogadores dizem

Fonte: tópicos do Reddit lidos pelo arquivo Arctic Shift (r/feedthebeast, r/ModdedMinecraft, r/TheBrokenScript, r/HorrorMinecraft, r/FromTheFog). A cobertura é parcial. Todas as citações são [P].

### Os momentos descritos como mais assustadores

- **Cave Dweller**, por quem não sabia que o mod estava instalado: "these cave noises are wild. then turned a corner and watched this abomination crack its neck towards me. barricade myself in a spot and make a tiny window. Big mistake, the horror of watching that thing crawl its way into my little hidy spot". São três ingredientes: jogador desavisado, som antes e o esconderijo violado. https://www.reddit.com/r/feedthebeast/comments/150tfjj/
- **Quase morrer marca mais que morrer:** "60-70% of my encounters with it it doesn't kill me, just chases me and damages me to where I'm at 1-2 hearts" (mesmo tópico).
- **The Broken Script:** "the most intense feeling comes before the chase"; "when he's in another cave and you can hear him in the distant talking"; a criatura "lurking in the distance, slightly obscured by the render distance fog". https://www.reddit.com/r/TheBrokenScript/comments/1wxqbm4/
- **Aviso divide opiniões:** uns preferem o susto sem aviso; outros respondem que o aviso "adds an element of fairness for a novice... but it blunts his fangs against veterans" (mesmo tópico).
- **Pedidos de jogadores:** "Look at the player while they are sleeping from somewhere that they could been barely seen"; "scan for a new position when you look away... just move him from one hiding spot to another"; "Looks great behind the tree but just looks too goofy out in the open". https://www.reddit.com/r/ModdedMinecraft/comments/1ug7mj0/

### Quando e por que deixa de assustar

Ninguém deu um número de horas. O que aparece:

- **Um truque só:** "Ok for 1 cheap jumpscare but offer nothing more... in survival they're more annoying than scary". https://www.reddit.com/r/feedthebeast/comments/1q9ag7j/
- **Frequência alta:** "scale back the spawn rate so you're not dealing with it so often it becomes not scary. It should be an event". https://www.reddit.com/r/feedthebeast/comments/1hgukjg/
- **Duas vezes em dois dias já foi lido como "o tempo todo".** https://www.reddit.com/r/feedthebeast/comments/176nk4z/
- **Invadir a base:** "make it that it doesent spawn in underground bases, its really anoying".
- **Jeito fácil de vencer:** "really easy to tower up to beat him".
- **Injustiça:** "most horror mods are just unfair, not even scary". https://www.reddit.com/r/ModdedMinecraft/comments/1rhfpa1/
- **Volume alto:** "earrape screamfests"; "that shit hurts my ears every. Single. Time."
- **Virar jogo de progressão:** sobre a versão 2.0 do Broken Script, "the new dimensions, the boss fights, the progression... feel out of place". https://www.reddit.com/r/TheBrokenScript/comments/1wxhj1y/
- **Servidor de amigos dura pouco:** "play for about 2 weeks with friends. Then everyone loses the itch and the server dies." https://www.reddit.com/r/ModdedMinecraft/comments/1uieiba/

### Raro demais também é problema

- "never encountered him... even though I heard his 'warning' sounds. I started to think my mod is broken" (Cave Dweller).
- Em r/FromTheFog há vários tópicos do tipo "How do I know if it's working" e "Herobrine isnt spawning".

### O que os jogadores de longa data elogiam

- "The last 'horror' 'mod' you could actually PLAY with was From the Fog. The rest... focus on mob griefing more than tension". https://www.reddit.com/r/HorrorMinecraft/comments/1wyl8ok/
- Um dono de servidor pede exatamente a proposta do Sussurros: "subtle horror mods that wouldn't be obvious right away? I want them to think they're going crazy". https://www.reddit.com/r/ModdedMinecraft/comments/1ugibq7/

### O que faz parecer "eu vi alguma coisa?" e não "um mob sumiu"

- **Distância e atraso.** No From The Fog a aparição distante fica a 50–100 blocos e some 1 s depois de ser mirada; a média fica a 25–46 e some em 0,5 s. Sem ninguém olhar, some sozinha em 30 s. [C]
- **Som só às vezes.** O ruído de sumiço do From The Fog toca em metade das vezes. [C]
- **Lugar ruim cancela.** Ele é removido se a cabeça ficar dentro de folhas ou de um bloco, ou se estiver na água. [C]
- **Silhueta.** O mod The Hollow desenha a entidade sem luz, "flat silhouette with two pale eyes", a 14–22 blocos. [P]
- **Fora do olhar.** A Ghost Girl de Lethal Company nunca aparece a menos de 8 unidades nem dentro de 80° da direção do olhar. [P] https://lethal-company.fandom.com/wiki/Ghost_Girl
- **Curta.** No Phasmophobia a aparição parada dura de 2 a 5 s. [P] https://phasmophobia.fandom.com/wiki/Ghost_Event
- **Meio encoberta.** Neblina e tronco de árvore são as duas coisas que os jogadores citam.

---

## 13. From The Fog em números

Lido no datapack. [C] https://github.com/LunarEclipseStudios/From-The-Fog/tree/main/data/lunareclipse.watching/function/

**Início:** nada acontece nos três primeiros dias de jogo.

**Intervalo até o próximo avistamento** (só conta quando não há um em andamento):

| Opção | De dia | De noite |
|---|---|---|
| insane | 15–30 s | 8–15 s |
| common | 30–60 s | 15–30 s |
| padrão | 60–120 s | 30–60 s |
| rare | 120–240 s | 60–120 s |
| scarce | 240–360 s | 120–180 s |

**Tipos de avistamento:**

| Tipo | Distância | Some depois de mirado em | Observação |
|---|---|---|---|
| creeping | 3–5 blocos, atrás | 0,1 s | desligado por padrão |
| dwelling | 6–12 | 0,5 s | em caverna |
| stalking | 25–46 | 0,5 s | |
| lurking | 50–100 | 1 s | |
| nightmare | 2 blocos do pé da cama | 0,1 s | dura 60 s |

**Eventos físicos** (passos, minerador fantasma, placas, golpe por não olhar): uma tentativa a cada 5 minutos reais, com chance padrão de 1 em 100 para a maioria e 1 em 25 para passos e quadros. Dá, em média, passos a cada 2 horas e minerador fantasma a cada 8 horas.

**Leitura para o Sussurros:** o From The Fog não é "raro" em tudo. Ele faz avistamentos frequentes, distantes e curtos, e eventos físicos muito raros.

**"Give Him Control":** liga todos os tipos, zera a espera inicial e, a cada dia de jogo, sorteia de novo cerca de 42 opções da própria configuração. Ao ser visto, a atividade dobra por 360 s.

**Multiplayer:** o alvo é um jogador aleatório e só existe um Herobrine por vez. Nada é por jogador.

---

## 14. Mais ideias de outros mods

### The Broken Script: eventos que faltavam

Fonte: https://thebrokenscript.wiki.gg/wiki/Random_Events [P]. A página não documenta chance nem intervalo.

| Evento | O que o jogador vive |
|---|---|
| Isolation | Em multiplayer, um jogador fica invisível e mudo para os outros por 3,5 min |
| OpenGL Error | Quatro linhas de erro no chat; a última diz "Here I am." |
| Null Interface Trigger | Abre sozinho um contêiner vazio com o título "help" ou "behind you" |
| Close Menu | O baú que você abriu fecha sozinho |
| Null Getting Advancement | Um jogador falso ganha uma conquista ligada ao seu progresso |
| Breathe | Toca o som de respiração do próprio jogo |
| Doors | Todas as portas de madeira por perto abrem juntas |
| Hallucination | Algo nasce fora da visão; se você olha, corre até você e não dá dano |
| Moon Glitch | A neblina fecha para dois chunks por 5 min |
| JFrame | Uma caixinha preta cobre parte da tela por 1 s |
| Entity Discard | O mob que você acabou de acertar desaparece |
| Noop | Não faz nada. Está na lista de propósito |

### The Obsessed

Fonte: https://www.curseforge.com/minecraft/mc-mods/obsessed [P]

- Os modos saem de um saco embaralhado: todos aparecem, sem ordem previsível.
- Aparece 4 vezes mais à noite; a intensidade sobe ao longo de cerca de 1 hora.
- Aparece dentro de casa agachado, de preferência atrás de uma janela, e foge se for olhado.
- Alucinações: atrás de você, pendurado no teto, e saltando de um canto quando você quebra um bloco.
- Pode perder o interesse em jogadores ausentes.

### Midnight Lurker

Fonte: https://github.com/Voxla/midnightlurker [C, só os nomes das variantes]

- Variantes: agressivo, de costas, rastejante, falso, escondido, invisível, fugitivo, sombra, sombra com olhos, metamorfo (vaca, porco, aldeão), parado encarando, cabeça fantasma.
- Estágios de insanidade de 0 a 7 por jogador, um a cada 20 min, voltando a zero no fim.
- Quatro variantes de arquivo para cada tipo de som.

### Mods sutis que faltavam na primeira rodada

| Mod | Onde roda | Código | Por que interessa |
|---|---|---|---|
| The Hollow — https://modrinth.com/mod/hollow-dread | **Fabric 26.2** | fechado | Roda na mesma versão do Sussurros. Medo de 0 a 100; abaixo de 25 não acontece nada. Outro jogador a 16 blocos faz o medo cair. Ser pego cega e enfraquece, não mata |
| TheWatcher — https://modrinth.com/mod/thewatcher | Fabric/Forge 1.20.1–1.21.1 | licença fechada, com fonte em https://github.com/Al-Capone11/TheWatcher (não lida) | Conceito quase igual ao do Sussurros: vulto fora da visão, ecos das suas ações, portas e tochas alteradas, animais encarando, itens da hotbar trocando de lugar, nomes de itens virando sussurros |
| Existence — https://modrinth.com/mod/existence | Fabric/Forge 1.20–1.20.1 | aberto, LGPL-3.0: https://github.com/CipherXOR/Existence | Terror entre amigos: fantasmas com a pele, o nome e a voz dos amigos |
| The Silence — https://modrinth.com/mod/the-silence-mod | NeoForge 1.21.1 | fechado | Itens somem dos baús; aparecem túmulos para os animais que você matou |

---

## 15. Terror entre amigos

### De onde vêm as mecânicas

- **Lethal Company, Masked:** imita o comportamento de um jogador (vira, espia cantos); ao ver alguém, para e encara de 2 a 5 s antes de agir. O único sinal é o movimento estranho. [P] https://lethal-company.fandom.com/wiki/Masked
- **Lethal Company, Ghost Girl:** "invisible to everyone but the employee she's haunting". Se outro jogador encosta nela, some. [P]
- **Phasmophobia:** o alvo de um evento é o jogador mais próximo; a sanidade é de cada um, mas a atividade usa a média do grupo, então o calmo sofre pelos colegas. Um tipo de fantasma fixa um alvo no começo e vai atrás dele em dois terços das vezes. [P] https://phasmophobia.fandom.com/wiki/Sanity
- **Skinwalkers** (mod de Lethal Company): grava a voz dos jogadores e a reproduz a partir dos inimigos. [P]

### Como fazer o sósia

1. **Entidade do próprio jogo.** O `Mannequin` mostra a pele de um jogador sem ele estar conectado. Existe na 26.2 (seção 11). O From The Fog já usa. [C]
2. **A técnica do Existence.** Um mob comum com dois identificadores sincronizados (dono da pele e alvo). O renderizador usa o modelo de jogador, pega a textura na lista de jogadores do cliente e não desenha nada para quem não é o alvo. São cerca de 50 linhas. Limite: o amigo precisa estar online. [C]
3. **Jogador falso de verdade, como o do Carpet.** Aparece para todos e conta como jogador; é exagero aqui. [C]

### Quinze ideias

| # | Ideia | Por que funciona | Fonte | Dificuldade |
|---|---|---|---|---|
| 1 | Vulto só para um; some se um amigo passa por cima | Ninguém confirma o relato | Ghost Girl [P] | fácil a média |
| 2 | Marcado da noite: um alvo fixo por noite recebe a maior parte | "Por que só comigo?" | Phasmophobia [P] | fácil |
| 3 | Sósia na linha das árvores, com a pele de um amigo que está longe | O rosto conhecido no lugar errado | Existence [C] | média |
| 4 | Sósia dentro da base, de costas; encara de 2 a 5 s antes de reagir | O único sinal é o comportamento | Masked [P] | média |
| 5 | Uma frase antiga de A reaparece só para B | Faz duvidar do amigo, não do jogo | The Obsessed [P] | fácil |
| 6 | Voz roubada do microfone | A voz do amigo sem o amigo | Skinwalkers [P], Existence [C] | difícil |
| 7 | Isolamento: um jogador fica invisível e mudo para os outros por 1 a 3 min | O grupo acha que ele saiu | The Broken Script [P] | média |
| 8 | "Saiu do jogo" falso, ou nome repetido na lista, só para um | Usa a interface em que todos confiam | From The Fog [C] | fácil |
| 9 | Eventos fortes só com o jogador separado; juntar-se alivia | Dá uma defesa social e pune a separação sem matar | The Hollow [P], Phasmophobia [P] | fácil |
| 10 | O mesmo som de aviso para evento falso e real | O aviso nunca vira dica confiável | Phasmophobia [P] | fácil |
| 11 | Pressão pela média do grupo | O calmo é assombrado porque o outro entrou em pânico | Phasmophobia [P] | fácil |
| 12 | Prova que só quem segura o item vê | O resultado tem de ser contado em voz alta | Phasmophobia [P] | fácil |
| 13 | Batida na porta só quando há um jogador em casa | Quando os outros voltam, não há nada | The Knocker [P] | fácil |
| 14 | Um jogador sem nada por dias, outro com a noite cheia | O poupado desacredita os relatos | The Broken Script [P] | fácil |
| 15 | Túmulo ou placa citando algo que só um jogador fez | Acusação que os outros leem | The Silence [P] | média |

---

## 16. Áudio sem orçamento

### Regras do Minecraft

Fonte: https://minecraft.wiki/w/Sounds.json [P]

- **Mono tem posição; estéreo não.** Sussurro "na cabeça" e fundo sonoro podem ser arquivos estéreo; passos e ruídos no mundo devem ser mono.
- **`"stream": true`** para sons de mais de alguns segundos.
- **Tom:** vai de 0,5 a 2 (uma oitava para cada lado).
- **Volume acima de 1** não fica mais alto: aumenta o alcance (16 blocos × volume).
- **Variantes:** uma é sorteada a cada vez. Para voz, de 8 a 12 (inferência; repetição de voz se nota rápido).
- **Sons do jogo que servem de graça:** a respiração do jogador (`PLAYER_BREATH`), o ambiente de caverna e os discos 11 e 13 tocados bem graves.

### Receitas de tratamento

As fontes descrevem os passos, mas quase sem números. **Os valores abaixo são ponto de partida e não foram testados.** O `ffmpeg` não está instalado na máquina do dono; o Audacity faz o mesmo à mão.

- **Sussurro com pré-eco ("eco invertido"):** inverter o áudio, aplicar reverberação, inverter de novo. O eco passa a vir antes da voz. No Audacity: deixar silêncio no início, Reverse, Reverb (sala 70–85, reverberação 60–80, só o som molhado), Reverse, e misturar com o original uns 6 a 10 dB abaixo. [P] https://www.howtogeek.com/63091/how-to-recreate-popular-effects-by-reversing-audio-in-audacity/
- **Atrás da parede:** cortar os agudos acima de 700–1200 Hz e baixar 6 dB.
- **Voz que não parece humana:** descer uns 3 semitons.
- **Coro de sussurros:** três gravações da mesma frase, uma mais grave, uma mais aguda e uma invertida, com atraso de 30 a 80 ms entre elas.
- **Variantes de graça:** tocar o mesmo arquivo com tom sorteado entre 0,9 e 1,1.

### Bibliotecas

- **Sonniss GameAudioGDC:** pacotes anuais gratuitos, uso liberado sem crédito; só não pode redistribuir os arquivos soltos. Para um mod privado serve. [P] https://sonniss.com/gameaudiogdc/
- **Freesound:** filtrar por licença CC0 e procurar "whisper" e "breath". Nenhum pacote específico foi conferido.

### O que é real e o que é mito

- **Infrassom (17–19 Hz):** a base é um relato de 1998 e um experimento de 2003. Fone e caixa comuns não reproduzem isso. Tratar como mito; um som grave audível de 30–60 Hz é a alternativa. [P] https://en.wikipedia.org/wiki/Infrasound
- **Silêncio e incerteza:** têm apoio nos relatos de jogadores e no código do From The Fog.
- **Volume alto:** é rejeitado explicitamente pelos jogadores.

---

## 17. As dez lições

1. **O medo está no antes.** O som distante antes do encontro é o momento mais citado.
2. **Raro, mas com prova de vida.** Duas vezes em dois dias já é "o tempo todo"; zero vezes vira "meu mod quebrou".
3. **Aparição é longe, parada, meio encoberta e curta.** Nunca perto em campo aberto.
4. **Saco embaralhado e carta vazia.** Evita padrão aprendido.
5. **Aviso fixo perde força com veterano.** O mesmo aviso deve servir para evento falso e real.
6. **Injustiça, jeito fácil de vencer e invasão da base matam o medo.** Quase morrer marca mais que morrer.
7. **Não virar jogo de progressão.** Os itens de defesa ficam pequenos e sem "vitória".
8. **Entre amigos, o alvo é a confiança.** Um vê, o outro não.
9. **Separação é o gatilho; reunião é o alívio.**
10. **Planejar para duas semanas e áudio barato.** Sons do jogo em tom grave, a própria voz tratada e nunca volume alto.

---

## 18. Limites desta pesquisa

- O Reddit bloqueou o acesso; os tópicos vieram do arquivo Arctic Shift, com a pontuação do momento da captura. A busca por texto falhou várias vezes, então a amostra de opiniões é parcial.
- O código dos mods fechados (The Broken Script, The Obsessed, Don't Let It Learn, Understudy, The Knocker, The One Who Watches, The Anomaly, The Hollow, The Silence) não foi lido.
- O código do TheWatcher está no GitHub e não foi lido.
- Do Midnight Lurker foram lidos só os nomes dos arquivos.
- Comentários do CurseForge e do PlanetMinecraft não foram lidos.
- Nenhuma técnica dos outros mods foi testada na 26.2, e nenhuma receita de áudio foi testada.
- A análise do log cobre uma sessão só, de um jogador.
