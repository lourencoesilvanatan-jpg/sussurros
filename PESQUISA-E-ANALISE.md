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

**Não existe pronto:** um evento do Fabric para "o jogador colocou um bloco". Há `ItemEvents.USE_ON` e `BlockEvents.USE_ITEM_ON`; falta conferir se servem ou se é preciso um mixin.

---

## 12. Limites desta pesquisa

- O Reddit bloqueou o acesso; o post veio do arquivo Arctic Shift, com a pontuação do momento da captura.
- O código dos mods fechados (The Broken Script, The Obsessed, Don't Let It Learn, Understudy, The Knocker, The One Who Watches, The Anomaly) não foi lido.
- Do Midnight Lurker foram lidos só o README e os nomes dos arquivos.
- Nenhuma técnica dos outros mods foi testada na 26.2.
- A análise do log cobre uma sessão só, de um jogador.
