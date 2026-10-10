# Ferramentas de detecção e proteção no terror: como são entregues, ensinadas e cobradas (SPOILERS do Sussurros)

Data da pesquisa: 10/10/2026. Notas para quem vai escrever o relatório; nenhuma alteração em repositório.

**Legenda das fontes**

- **[C]** código-fonte lido.
- **[P]** página aberta e lida.
- **[B]** só o trecho que veio no resultado de busca; a página não foi lida.

**Dois avisos sobre o [P].** (1) As páginas de wikis `fandom.com` e os resumos de artigos científicos (Europe PMC) foram lidos em texto bruto pela API pública, então as citações são literais. (2) Todas as outras páginas [P] (Steam, Kotaku, Wikipedia, Modrinth, CurseForge, docs do Patchouli, blogs) passaram pela ferramenta de leitura, que usa um modelo auxiliar para extrair os trechos: as citações dessas podem ter pequenas diferenças do original.

**O que estas notas não repetem.** O arquivo `pesquisa/2026-10-08-itens-e-sentidos.md` já cobre: os sete objetos amaldiçoados do Phasmophobia e seus custos, a entrevista do Thomas Grip sobre a sanidade do Amnesia, a lanterna de dínamo e o gerador do The Bunker, habilidades e eventos noturnos do Darkwood, Krampus e os degraus de sanidade do Don't Starve, Voices of the Void, Fear & Hunger, The Forest (sanidade sem efeito, efígies), rádio do Silent Hill, Insight do Bloodborne, leitmotiv, Warden e Mau Presságio, e a lista de mods de terror com itens (The Hollow, The Obsessed, From The Fog pelo código, Sanity, Whisperwoods). Aqui o foco é o que faltava: **quando a ferramenta chega, o que o primeiro uso mostra, como o custo é aprendido, e como um valor escondido fica legível.**

---

## 1. Ferramenta por ferramenta: quando o jogador recebe, como o retorno se lê, se o primeiro uso é garantido, e como o custo é aprendido

### Takeaway

Nos jogos que ensinam bem, a ferramenta chega **antes** de a ameaça estar solta (ou num lugar seguro) e o primeiro uso é **encenado para sempre mostrar alguma coisa**: o treino do Phasmophobia só abre a porta seguinte quando o EMF marca 5 e quando a Spirit Box responde. Onde o retorno não existe (incenso do Phasmophobia, efígies do Sons of the Forest, custo sonoro do rastreador do Alien: Isolation), o jogador aprende por morte, por guia externo ou cria teoria errada.

### Cited Findings

**Alien: Isolation (2014) — rastreador de movimento**

- Descrição do item dentro do jogo: "The motion tracker is your key to survival, use it well." — [P] https://alienisolation.fandom.com/wiki/Motion_Tracker
- Leitura: o movimento aparece como um círculo na tela; o aparelho "cannot detect enemies when they are not moving and cannot determine if the Alien is up in the ducts or on ground level". No modo Nightmare "the motion tracker is unreliable". — [P] https://en.wikipedia.org/wiki/Alien:_Isolation
- Ele também aponta o próximo objetivo como uma barra clara na tela. — [B] https://www.gamepro.de/artikel/alien-isolation-nackte-panik-haeufige-tode,3032145,seite2.html (o trecho veio de um conjunto de resultados; a página exata não foi identificada)
- Custo: "if the Alien is close enough, it will be attracted by the tracker's sound, forcing the player to use the tracker wisely and remove it as soon as it detects motion." — [P] https://en.wikipedia.org/wiki/Alien:_Isolation
- Quando chega: depois da morte de Axel, a primeira vítima da criatura; a cena "serves more to introduce the lethality of the Alien than anything else". — [P] https://media.gdcvault.com/gdc2017/GameNarrativeReview/LeaTalbot_NarrativeReview_Alien.pdf . "After acquiring a modified motion tracker, Ripley successfully contacts Samuels". — [B] mesmo PDF. Ou seja: o jogador **vê a criatura matar primeiro, recebe o detector em seguida**, e só depois ela passa a caçá-lo solta. As páginas das missões 2 e 3 na wiki estão vazias, então a missão exata não foi confirmada. — [P] https://alienisolation.fandom.com/wiki/Mission_3_-_Encounters
- Como o custo é aprendido: por morte e por guia. Kotaku (Kirk Hamilton, 07/10/2014): "The alien can hear it. If you're standing in a locker and get ripped apart by the alien, it's probably because the beast could hear the soft beeps of your motion tracker." E: "Isolation doesn't do a very good job of explaining itself, and doesn't even really give players a tutorial before throwing them into the alien-filled deep end." — [P] https://kotaku.com/tips-for-playing-alien-isolation-1643391269
- Teoria errada criada pelo jogador (tópico "Some thoughts on Nightmare Mode", Steam, 25/03/2021): "Then it's entirely coincidental when my motion tracker beeps ('ambient') and seconds later the alien drops out of the ceiling?" Resposta de veterano: "It can only hear your motion tracker if you have it raised"; "the motion tracker gives an ambience beep when the alien is very very close". O jogador inverteu causa e efeito porque os dois sinais chegam com segundos de diferença. — [P] https://steamcommunity.com/app/214490/discussions/0/3079873089694471240
- Por que o retorno assusta: "Seeing the dot of the Xenomorph on the tracker is incredibly distressing, but even more so when it disappears, and the player can't help but keep glancing at it every few seconds." — [P] https://media.gdcvault.com/gdc2017/GameNarrativeReview/LeaTalbot_NarrativeReview_Alien.pdf

**Phasmophobia — treino (versão "TrainingV2", em uso desde a 0.9.0 "Ascension", de 17/08/2023)**

- O treino é um galpão linear de **11 etapas** separadas por portas trancadas; cada etapa entrega uma ferramenta de nível I na entrada da sala feita para ela. As instruções vêm "by both text written on white boards and vocal instructions from the man over the radio", com post-its mostrando os controles; dá para voltar às salas anteriores. — [P] https://phasmophobia.fandom.com/wiki/Training
- A ordem ensina o medidor antes das ferramentas: etapa 2 é Sanidade (o jogador precisa tomar o remédio para avançar), etapa 3 é Luzes (queda passiva e disjuntor), só depois vêm EMF, UV, temperatura, D.O.T.S, orbes, escrita, Spirit Box, e por último a caçada. — [P] mesma página
- **Primeiro uso garantido.** EMF: "an invisible force will throw the items around the room, creating EMF readings. The player must find an EMF 5 reading with the reader to proceed". Spirit Box: "the player must attempt to obtain an answer to proceed to the next stage". — [P] mesma página
- **Primeira caçada sem morte.** "If the player is spotted by the ghost, they will be teleported back to the beginning of the stage." O fantasma só aparece depois que o jogador entrou num esconderijo e desligou os eletrônicos. — [P] mesma página
- Segundo a wiki não há fantasma de verdade no treino: o comportamento é "emulated via scripted triggers" (a própria wiki marca essa frase como "a verificar"). — [P] mesma página
- Versão anterior (antes da 0.9.0): uma casa normal com fantasma sorteado "that could give evidence but could not hunt", com dicas na TV da sala. — [P] mesma página

**Phasmophobia — leitor de EMF (níveis I a III existem desde a 0.9.0)**

- O retorno fica mais rico a cada nível, sem mudar a função: nível I é um ponteiro que balança e estala; nível II tem "five LED lights" e alto-falante; nível III tem tela que mostra "the range, a directional display and the intensity" de até três pontos, com bipe mais agudo para leitura mais forte. — [P] https://phasmophobia.fandom.com/wiki/EMF_Reader
- Custo: "Holding an active EMF reader within 7.5 metres of a ghost [...] during a hunt on the same floor will attract the ghost"; durante a caçada o aparelho "will malfunction and display random values". — [P] mesma página

**Phasmophobia — Spirit Box**

- A descrição do item diz as condições e o retorno: "Make sure to stand near to any paranormal activity, turn off any lights in that room, and ask some questions". **Três estados visíveis:** "If the question has been heard, a red light will flash on the Spirit Box, if it flashes white, you've got a response." No nível II é um X preto (ouviu, sem resposta) ou um ícone de fantasma (respondeu). — [P] https://phasmophobia.fandom.com/wiki/Spirit_Box
- O silêncio continua ambíguo num caso: "If nothing happens upon asking a question, it is possible the game did not recognize any valid phrase, or the player's voice recognition is not working properly." — [P] mesma página
- Custo: usar bem "requires a player to remain alone in the dark with the ghost for several seconds, thus considerably reducing their sanity"; o chiado constante "may interfere with the ability to hear ghost activities"; ligada, atrai o fantasma a até 7,5 m durante a caçada. A wiki registra que "Using the spirit box by itself does not increase ghost activity". — [P] mesma página

**Phasmophobia — crucifixo**

- O alcance aparece na hora de colocar: segurar o botão "will show the default effective range", uma esfera translúcida de 3 / 4 / 5 m. — [P] https://phasmophobia.fandom.com/wiki/Crucifix
- O uso deixa marca visível e permanente: "a burning effect will appear, and the crucifix will glow"; "On first use, one arm of the crucifix will disappear". Também cria uma leitura de EMF 2, e o crucifixo que estava na mão é jogado no chão sozinho. — [P] mesma página

**Phasmophobia — incenso (antigo "smudge sticks")**

- A descrição diz a função ("stop a ghost from initiating a hunt for a long duration"), mas o resultado não tem confirmação: "There is no explicit indication of whether the incense was used effectively or not." — [P] https://phasmophobia.fandom.com/wiki/Incense
- O efeito dura 90 s (60 s para Demon, 180 s para Spirit). O jogo dá um relógio para o jogador contar sozinho: o cronômetro do relógio de pulso "can help players to keep track of incense timers". — [P] https://phasmophobia.fandom.com/wiki/Incense e https://phasmophobia.fandom.com/wiki/Watch
- Ordem de liberação: EMF e Spirit Box são equipamento inicial; sensor de movimento no nível 3, sal no 8, incenso no 14. As ferramentas de proteção chegam depois das de detecção. — [P] páginas de cada item na mesma wiki

**Phasmophobia — sal e sensor de movimento (parentes de "linha na soleira" e "fio")**

- Sal: a descrição recomenda onde pôr ("in doorways and near turns in hallways"). Quando o fantasma pisa, "it will leave an indent in the pile", e nos segundos seguintes "it will create audible footsteps". O sal de nível III "will act like a barrier, forcing the ghost to turn around". — [P] https://phasmophobia.fandom.com/wiki/Salt
- Sensor de movimento: "it will flash any time something moves in front of it, even things you can't see." — [P] https://phasmophobia.fandom.com/wiki/Motion_Sensor

**Fatal Frame — Câmera Obscura**

- Chega como herança ou achado ("Usually acquired as a hand-me-down or discovered in some place"). No primeiro jogo, Mafuyu usa a câmera na mansão e Miku "finds the camera and picks up where her brother left off". — [P] https://fatalframe.fandom.com/wiki/Camera_Obscura
- O visor tem dois retornos separados. Detecção: um filamento no topo; "A blue glow from the filament means that a benign ghost is present, or a clue is nearby [...] A red glow means that a hostile ghost is present", e "the glow of the filament brightens as the player faces towards and gets closer to the source". Ataque: o círculo de captura "is illuminated more and more until it is fully charged". — [P] mesma página
- Recurso: "Without any film loaded, the camera is unable to take photographs"; o filme básico (Type-07) não acaba, os melhores são "of limited quantity". — [P] mesma página

**Amnesia: The Dark Descent (2010) — lampião**

- O manual do jogo avisa: "This item is found early on and will help lighting up your close surroundings." Fica no Old Archives, numa mesa; pegar "triggers a cutscene and is hard to miss". — [P] https://amnesia.fandom.com/wiki/Lantern_(The_Dark_Descent)
- O Old Archives é a segunda área do jogo e não tem encontro com inimigo ("encounters: None (safe)"). A ferramenta chega em lugar seguro. — [P] https://amnesia.fandom.com/wiki/Old_Archives
- Descrição no inventário, só função: "The lantern will light up dark places." — [P] https://amnesia.fandom.com/wiki/Lantern_(The_Dark_Descent)
- Custo: com o lampião aceso num encontro, "Daniel will be illuminated, making it easier for the Gatherers to see, pursue, and kill him". — [P] mesma página
- Os barris de óleo se ajustam ao jogador: "The amount of oil they contain depends on how well you are doing on oil." — [P] mesma página

**Amnesia: The Bunker (2023) — dicas de morte**

- O custo das ferramentas é ensinado **depois da morte**, em frases curtas na voz de soldados, não em texto de tutorial: "These new flashlights are loud as hell. Useless in the field, the Germans would hear us cranking them miles away."; "The enemy is always listening. Never run, your feet will betray you." — [P] https://amnesia.fandom.com/wiki/Death_hints

**Outlast (2013) e Outlast 2 (2017) — filmadora**

- Os protagonistas começam com pilhas, o que indica filmadora desde o início: "All protagonists start off with two batteries on Normal difficulty and only one on anything above that". Em Whistleblower ela é pega de um tripé "just as the breakout begins". — [P] https://outlast.fandom.com/wiki/Camcorder
- Limite por dificuldade: "A maximum of ten batteries can be held on Normal difficulty, five on Hard and two on Nightmare and Insane." — [P] mesma página
- O medidor é a própria imagem: "When a battery's power is about to be depleted, the night vision will flicker." Sem pilha a visão noturna não some: "the night vision's range will be reduced substantially". — [P] mesma página
- Há momentos em que o jogo levanta a câmera sozinho ("scripted instances where the protagonist will use their camcorder regardless of the player's input"), e a visão noturna revela o Walrider invisível. — [P] mesma página
- Outlast 2 soma um segundo sentido à mesma ferramenta: "sound detectors for left and right dynamics, which are used by the player to locate visually obscured enemies". — [P] mesma página

**Darkwood (1.0 em 2017) — defesas do esconderijo**

- O prólogo "act as the tutorial section": o jogador aprende a atacar derrubando uma árvore do caminho e vê as luzes apagarem quando o gerador seca. — [P] https://darkwood.fandom.com/wiki/Prologue
- No primeiro esconderijo há um bilhete no chão ao lado do gerador ("Generator Note"): a instrução é um objeto do mundo. — [P] https://darkwood.fandom.com/wiki/Dry_Meadow_Hideout
- Barricadas custam tábuas e pregos; janelas e portas podem ser barricadas; consertar uma porta destruída custa 5 tábuas e 9 pregos. — [P] https://darkwood.fandom.com/wiki/Barricade
- A loja declara a postura: "There are no quest markers or hand holding, and you'll need to learn and experiment on your own." — [P] https://store.steampowered.com/app/274520/Darkwood/

**Lethal Company — scanner, prancheta e terminal (wiki lida em 10/2026)**

- O scanner vem no traje desde o início (botão direito). Verde para sucata com o valor, vermelho para criaturas, azul para pontos de interesse. "Scanning a new entity uploads its creature file to the terminal's Bestiary." — [P] https://lethal-company.fandom.com/wiki/Scanner
- Duas ameaças ficam de fora de propósito: "Ghost girls and Masked cannot be scanned. It also has no data recorded in the terminal." — [P] mesma página
- O tutorial é um item: a prancheta ("Used as a tutorial for Lethal Company") aparece na mesa da nave quando um lobby novo é carregado, com um "SERVICE MANUAL" que diz "Press [RMB] to ping the scanner while looking towards objects of interest." — [P] https://lethal-company.fandom.com/wiki/Clipboard
- O manual é assumidamente incompleto: "Certain cautions or warnings are absent to increase readability; We are not responsible for injury or death resulting from the improper use of these tools and information." E a wiki registra que ele descreve as cores do scanner de forma errada. — [P] https://lethal-company.fandom.com/wiki/Clipboard e https://lethal-company.fandom.com/wiki/Scanner
- O terminal tem `HELP`, `BESTIARY` e `INFO [item]` ("To get info on an item from the Company Store"). — [P] https://lethal-company.fandom.com/wiki/Terminal
- Recepção: "With little instructions and even less of a story, half the fun of Lethal Company is experimenting". — [B] https://www.commonsensemedia.org/game-reviews/lethal-company . O bestiário "gives hints as to what players can expect from these monsters", e para escanear é preciso chegar perto. — [B] https://chantillynews.org/14045/arts-and-entertainment/lethal-company-sets-a-higher-standard-for-early-access-games

**Don't Starve — sanidade**

- O valor é visível o tempo todo (ícone de cérebro) e a taxa também: "A small animated arrow appears over the icon, indicating an increase or decrease in Sanity. The size of the arrow signifies the rate of gain or loss." O jogador vê na hora que um item mágico está custando. — [P] https://dontstarve.fandom.com/wiki/Sanity

**Subnautica (2018) — scanner**

- O jogo avisa que existe algo a escanear mesmo com a ferramenta guardada: "If the Scanner is not held in the player's hand and the player has not yet scanned the object, a small Scanner icon will appear in the bottom right of the screen to inform the player." — [P] https://subnautica.fandom.com/wiki/Scanner_(Subnautica)
- Retorno do uso: um círculo branco de progresso, cerca de dez segundos; se interromper, "the player can resume scanning later with no loss of progress". Gasta bateria. — [P] mesma página
- A mesma ferramenta lê um medidor escondido do próprio jogador: o autoescaneamento mostra "the progress of the infection in the player's body". — [P] mesma página

**The Mortuary Assistant (2022)**

- O tutorial é um turno diurno completo, com um aviso por passo, e pode ser refeito ("Reset Intro"). As primeiras assombrações acontecem dentro dele e são desmentidas pelo mentor: a janela bate e ele diz "These old windows never stay latched"; depois um sussurro diz "You'll die here" e ele nega ter ouvido. — [P] https://the-mortuary-assistant.fandom.com/wiki/Tutorial
- As regras do sobrenatural vêm em três fitas cassete que ficam no inventário e "can be replayed". — [P] https://the-mortuary-assistant.fandom.com/wiki/Items
- Tira de sangria ("letting strip"): o jogador anda com ela na mão; "Once it starts to smolder or burn, this is a sign that you're getting close to a sigil." — [B] https://gamerjournalist.com/how-to-use-lettering-strips-in-the-mortuary-assistant/
- Cinzas e fósforo dão uma pista forte uma vez só: "It is only possible to use Ashes once per night shift." — [P] https://the-mortuary-assistant.fandom.com/wiki/Items
- Reclamação típica, quando o retorno falha: "I got a blank sigil on the door in the hallway as well. I was confused and unsure if it was a glitch." (tópico de 04/08/2022; era defeito, confirmado pelo desenvolvedor). — [P] https://steamcommunity.com/app/1295920/discussions/0/3457094584913988448
- Um guia chamado "oficial" diz que o que mais confunde são as assombrações. — [B] https://gamepretty.com/the-mortuary-assistant-official-guide-identifying-marks-embalming-demonic-mechanics-and-endings/

**Devour (2021) — itens de ritual**

- Cada mapa tem o seu trio de itens (Farmhouse: galão, cabra, feno; Carnival: cabeças de boneca, **caixas de música**, moedas). — [P] https://devour.fandom.com/wiki/Ritual_Items
- Tópico "Not fun for new players" (Steam, 24/07/2023): "This game has NO instructions to start off."; "you have to watch youtube videos and get 'seasoned' players to join just to win". Respostas: "This game needs a tutorial level"; do outro lado, "everything is right in front of you" e "The levels are also designed to build on each other". — [P] https://steamcommunity.com/app/1274570/discussions/0/3808407347833668817

**Dead by Daylight — tutorial e oferendas**

- O tutorial dá uma regra de custo por frase curta, na hora da ação: "When you run, you leave Scratch Marks that are only visible to the Killer. Walk or crouch to prevent Scratch Marks from spawning." — [P] https://deadbydaylight.fandom.com/wiki/In-Game_Tutorial
- A wiki diz que a página existe para estar "expanding on and also correcting some of the information found in the In-Game Tutorials". — [P] mesma página
- As oferendas são apresentadas por um diário dentro da ficção, que conta uma descoberta por acidente: "I fumbled and dropped it into the fire. [...] The next day something had changed." — [P] https://deadbydaylight.fandom.com/wiki/Offerings

**Demonologist (2023) — Salt Barrier**

- A descrição do item já diz que a proteção gasta, sem números: "The more the ghost attacks, the lower the chance of protection will be." Os números só estão na wiki: 90%, 70%, 30%. Libera no nível 10 e é a ferramenta mais cara. — [P] https://demonologist.fandom.com/wiki/Salt_Barrier

**Sons of the Forest — efígies**

- Jogadores perguntam se funcionam; há relato de dez ou mais efígies e ataques continuando, e de testes de jogadores concluindo que funcionam em quantidade. — [B] https://steamcommunity.com/app/1326470/discussions/0/4301571519134961481 (a página não abriu; só o trecho de busca)

### Inferences

- **Padrão de entrega.** Em Alien: Isolation, Amnesia, Phasmophobia e Mortuary Assistant, o jogador recebe a ferramenta num momento em que não pode morrer (sala de treino, área segura, turno diurno) ou logo depois de ver a ameaça agir sobre outra pessoa. Em nenhum dos casos lidos o primeiro uso é deixado ao acaso.
- **O Sino Oco é o parente direto da Spirit Box.** A Spirit Box distingue três coisas com uma luz: não ouvi, ouvi e não respondo, respondi. Na sessão de teste do Sussurros, 16 toques sem resposta entendida são o caso que a Spirit Box evita no treino (a porta só abre com resposta) e sinaliza no uso normal (luz vermelha).
- **O incenso é o caso mais parecido com os itens de proteção do mod:** efeito real, nenhuma confirmação. O Phasmophobia compensa com um cronômetro no pulso e com a descrição do item; mesmo assim a wiki precisa dizer que "não há indicação".
- **O crucifixo mostra duas coisas que servem à Vela e à Linha de Cinza:** o alcance aparece na hora de colocar, e cada uso arranca um pedaço visível do objeto.
- **O sal e o sensor mostram "prova de vida":** a ferramenta registra uma passagem que o jogador não viu. Isso ensina a função sem texto.
- **Jogadores constroem teoria causal com sinais a segundos de distância, inclusive teoria errada** (bipe do rastreador e queda do alien). Com 30 a 120 s de distância, o provável é não construir teoria nenhuma (ver seção 2).

### Gaps

- **Alien: Isolation:** missão exata em que o rastreador é pego e se o primeiro uso mostra um sinal garantido. As páginas de missão da wiki estão vazias; os artigos de mão na massa (PC Gamer) vieram truncados. Também não achei fala de desenvolvedor sobre como pretendiam ensinar que a criatura ouve o aparelho.
- **Fatal Frame:** se o prólogo do primeiro jogo tem um primeiro fantasma garantido para ensinar a câmera. A wiki só confirma quem usa a câmera.
- **Amnesia:** como o jogo ensina que os inimigos veem o lampião (dica na tela ou só por experiência) e a quantidade/ritmo das pederneiras não foram relidos (o arquivo anterior tem os números).
- **Outlast:** não confirmei o texto do aviso que ensina a visão noturna na primeira área escura.
- **Signalis e Resident Evil:** nada de novo além do que o arquivo anterior já traz sobre inventário; não pesquisei como ensinam itens escassos.
- **Pacify:** a wiki não respondeu; nada verificado.
- **Sons of the Forest e The Forest:** recepção das efígies só por trecho de busca.
- **Lethal Company:** não achei fala do Zeekerss sobre a ausência de tutorial; a entrevista lida fala de riso e de histórias entre amigos, não de ensino. — [P] https://www.pushtotalk.gg/p/how-lethal-company-sold-10-million-copies

---

## 2. Medidores e custos escondidos: como ficam legíveis, o que os jogadores reclamam, e quando a consequência atrasada é bem recebida

### Takeaway

Os jogos lidos tornam um valor escondido legível por um destes cinco caminhos: leitura sob demanda num objeto (monitor do caminhão, rabisco no papel, autoescaneamento), seta contínua no HUD, sinal curto no instante da ação (clarão e som do Metro), resumo no fim do capítulo (Dishonored), ou dica depois da morte (The Bunker). A pesquisa de psicologia diz que atraso entre causa e efeito derruba a percepção de causa, **a menos que a pessoa saiba que deve esperar um atraso ou receba um marcador que delimite a ação**: sem isso, o efeito é lido como acaso.

### Cited Findings

**Leitura sob demanda, num objeto do mundo**

- Phasmophobia: os valores "can be read at any time on the truck's sanity monitor or roughly visualized on the watch". A página não cita nenhum outro meio de leitura (não fala de efeito de tela ou de som ligado ao valor) e diz que, ao chegar a 0%, "nothing noteworthy occurs". A wiki admite que o efeito do valor "may not always be readily apparent over the course of an investigation". — [P] https://phasmophobia.fandom.com/wiki/Sanity
- O monitor mente um pouco de propósito: "will oscillate by ±2% per player in order for sanity to be perceived as more organic". Nas dificuldades Nightmare e Insanity ele quebra ("will give random readings"), o anel do relógio some, e o Tabuleiro Ouija passa a ser "the only feasible way to get a good estimate". Tirar a leitura é tratado como aumento de dificuldade, não como padrão. — [P] https://phasmophobia.fandom.com/wiki/Sanity_Monitor e https://phasmophobia.fandom.com/wiki/Watch
- O diário, sempre acessível, tem uma seção "Home" que "outlines the fundamentals and basic game mechanics", com subseções de sanidade e de ferramentas. — [P] https://phasmophobia.fandom.com/wiki/Journal
- The Mortuary Assistant: o nível de possessão "can be checked at any time by scribbling with the Paper and Pencil". O rabisco muda de linhas para círculos, para um símbolo demoníaco, até o símbolo da casa do demônio; marquinhas embaixo contam de 1 a 9. — [P] https://the-mortuary-assistant.fandom.com/wiki/Possession
- A instrução vem na ficção, numa fita: "The only way I've found to tell how far along a possession is on your own is to scribble on a piece of paper. Don't think about it... Just scribble. Look carefully. If you notice anything strange, it's starting to take hold." O papel só fica disponível nos turnos da noite, depois do tutorial. — [P] https://the-mortuary-assistant.fandom.com/wiki/Paper_and_Pencil
- Subnautica: o autoescaneamento mostra o avanço da infecção no corpo do jogador. — [P] https://subnautica.fandom.com/wiki/Scanner_(Subnautica)

**Sinal no instante da ação, sem explicação**

- Metro 2033 (2010): "This system is never explained to the player". Ao ganhar um ponto, "the screen flashes light blue and whispers or the sound of water dripping can be heard"; ao perder, "the screen darkens and an ominous sound can be heard". São 99 pontos possíveis (91 no original) e 50 dão o final bom. — [P] https://metrovideogame.fandom.com/wiki/Moral_Points
- Leitura de um crítico (Space-Biff, 10/08/2012): o sinal "it's easy to miss, or misconstrue as a mere visual effect", e "the game makes it easy to act as though you aren't being judged". O autor elogia o desenho; o mesmo texto cita que só 3,9% dos jogadores tinham chegado ao final bom. — [P] https://spacebiff.com/2012/08/10/metro-2033-act-three/

**Resumo no fim do capítulo**

- Dishonored (2012): os valores de caos "are hidden, but are tracked internally throughout the playthrough"; quando passam do limite da missão, "a ranking of High Chaos will be awarded in the end mission screen". O mundo responde com mais guardas, ratos e infectados. — [P] https://dishonored.fandom.com/wiki/Chaos
- Reclamação (tópico "I don't understand this game...", Steam, 12/08/2021): "It feels like it's telling me: 'look at all the cool and fun ways to kill people! But you are bad for having fun!'"; "why are you forced to a particular playstyle for a specific ending...in a game that markets itself with freedom?". Defesa: "I reject the notion that you're 'punished' for killing people. [...] the ending is darker but it's not a 'bad ending'". — [P] https://steamcommunity.com/app/205100/discussions/0/5800060047318548011

**Dica depois da morte**

- The Bunker ensina o custo de barulho das ferramentas em citações de soldados que aparecem ao morrer (ver seção 1). — [P] https://amnesia.fandom.com/wiki/Death_hints

**Seta contínua**

- Don't Starve mostra a taxa de perda de sanidade com uma seta cujo tamanho indica a velocidade (ver seção 1). — [P] https://dontstarve.fandom.com/wiki/Sanity

**O que a psicologia diz sobre causa e efeito separados no tempo**

- Buehner e May (2003): estudos anteriores mostraram que "people fail to identify causal relations if cause and effect are separated by more than two seconds". Os autores preveem e confirmam que "delays should generally hinder reasoning performance, but that this hindrance should be alleviated if reasoners have knowledge of the delay". — [P] https://europepmc.org/article/MED/12850993
- Buehner e May (2004): "the negative influence of delay can be abolished completely by a subtle change in the experimental instructions". — [P] https://europepmc.org/article/MED/15204115
- Greville, Cassar, Johansen e Buehner (2013): um tom que delimita cada tentativa, **sem explicar o que significa**, já "significantly increased the accuracy of causal judgments in the presence of delays"; dizer o que o tom significa "completely abolished the effect of delays". — [P] https://europepmc.org/article/MED/23580340

**Regras que o jogador inventa quando a ligação não aparece**

- Phasmophobia: uma atualização (data não conferida) passou a fazer o fantasma ouvir a voz do jogador na caçada porque, segundo os desenvolvedores, "lots of people thought it was a feature". A crença dos jogadores veio antes da regra. — [B] https://gamespot.com/articles/phasmophobia-adds-scary-feature-that-people-thought-it-already-had/1100-6486154/ (a página devolveu erro 403)
- Alien: Isolation: jogador conclui que o bipe do rastreador chama a criatura (ver seção 1). — [P] https://steamcommunity.com/app/214490/discussions/0/3079873089694471240

**Um precedente de "sons falsos ligados a um valor escondido" que foi retirado**

- Histórico da wiki do Phasmophobia: em 04/02/2020, "More fake ghost sounds will play the lower the sanity an individual player has." Em 11/09/2020, "Removed fake ghost sounds caused by low sanity." — [P] https://phasmophobia.fandom.com/wiki/Sanity

**Consequência atrasada que é bem recebida**

- The Witcher (2007): "A time-delayed decision-consequence system means that the repercussions of players' decisions will make themselves apparent in plot devices in later acts of the game"; o sistema "helps avert a save-reload approach to decision making". — [P] https://en.wikipedia.org/wiki/The_Witcher_(video_game)
- Segundo o designer Michał Madej, a equipe usou "flashback cutscenes that remind the player of their decisions and the outcomes". A consequência atrasada vem **assinada**: o jogo mostra de novo a causa na hora do efeito. — [B] https://gamebanshee.com/39ad
- Phasmophobia: depois de uma caçada amaldiçoada, todas as seguintes duram 20 s a mais (já está no arquivo anterior). O custo é atrasado, mas tem a mesma forma do evento que o causou.

### Inferences

- **O problema medido no teste do Sussurros é o previsto pela literatura.** Um som 30 a 120 s depois do uso do item, sem aviso de que haverá atraso e sem marcador no momento do uso, cai no caso em que as pessoas "fail to identify causal relations". O jogador ouvir a cobrança como "barulho aleatório" é o resultado esperado, não um azar da sessão. (Ressalva: os experimentos usam atrasos de segundos e tarefas de apertar botão; levar isso para 30 a 120 s num jogo é extrapolação minha.)
- **A mesma literatura aponta três consertos, do mais discreto ao mais explícito:** (a) um marcador sensorial no instante do uso, igual para todos os itens, mesmo sem explicar o que é (o tom do estudo de 2013, o clarão do Metro); (b) fazer a cobrança ter a "cara" do item que a causou (a caçada mais longa do Phasmophobia, o flashback do Witcher); (c) dizer uma vez, dentro da ficção, que as coisas "voltam depois" (a fita do Mortuary Assistant).
- **Leitura sob demanda combina com o Caderno.** O papel e lápis do Mortuary Assistant é um medidor que o jogador escolhe consultar, é diegético, tem degraus desenhados e uma contagem discreta. O monitor do Phasmophobia mostra que dá para a leitura ser imprecisa de propósito (±2%) sem deixar de ser leitura.
- **Sinal sem explicação divide o público.** O Metro é o exemplo admirado e, ao mesmo tempo, aquele em que quase ninguém entendeu a tempo. Serve quando perder a ligação não estraga a sessão; no Sussurros, perder a ligação transformou metade do que o jogador percebeu em ruído.
- **Custo que soa como bronca gera rejeição mesmo quando é legível** (Dishonored). O que os jogadores defendem ali é que o resultado "mais sombrio" não seja um castigo, e sim outra versão do mundo.
- **A retirada dos sons falsos do Phasmophobia é um dado a pesar**, mas o motivo não foi encontrado: pode ter sido confusão com os sons reais do fantasma, pode ter sido outra coisa.

### Gaps

- Motivo da retirada dos "fake ghost sounds" do Phasmophobia em 09/2020: não achei nota dos desenvolvedores.
- Não li a fala original do Madej (só o trecho de busca) nem achei entrevista da CD Projekt sobre Witcher 3 com o mesmo ponto.
- Não achei estudo de UX de jogos (GDC, artigo) medindo atribuição de causa com atrasos de dezenas de segundos; a base é de psicologia experimental.
- Dishonored 2: não consegui ler o que a Arkane mudou para deixar o caos mais legível (o artigo da PC Gamer veio truncado).
- O número de 3,9% do final bom do Metro vem de um blog de 2012; não conferi em estatística oficial.

---

## 3. Sobreposição: várias ferramentas para o mesmo verbo

### Takeaway

Onde várias ferramentas fazem a mesma coisa do mesmo jeito, jogadores e críticos dizem que não usam: em Alien: Isolation, três distrações têm o mesmo efeito e um guia admite que "you probably won't use your gadgets". O Phasmophobia tem muito mais ferramentas e escapa disso porque cada uma responde a uma pergunta diferente, com uma forma de retorno diferente, e porque os níveis mudam a riqueza do retorno, não a função.

### Cited Findings

- Alien: Isolation, lista feita por jogadores (Steam, 28/02/2015): "The EMP Mine, Flare and Noisemaker all distract the Alien for about the same period of time."; "EMPs are largely useless as they only affect regular Working Joes"; "Smoke bombs are vastly underrated."; "molotovs should be avoided, as pipe bombs are better allrounders". — [P] https://steamcommunity.com/app/214490/discussions/0/617329150703357934
- Kotaku: "If you're anything like me, you probably won't use your gadgets in the game very often." — [P] https://kotaku.com/tips-for-playing-alien-isolation-1643391269
- Existe um tópico inteiro no Giant Bomb com o título "Anyone ever actually use the flares?". — [B] https://www.giantbomb.com/alien-isolation/3030-44296/forums/anyone-ever-actually-use-the-flares-1496758/
- A PC Gamer, por outro lado, creditou ao sistema de fabricação "a lot of unexpected depth". — [P] https://en.wikipedia.org/wiki/Alien:_Isolation
- Phasmophobia, detecção: cada ferramenta tem uma modalidade própria de retorno. EMF é ponteiro, luzes ou tela; Spirit Box é luz vermelha ou branca mais voz; sal é marca no chão mais passos; sensor é um clarão. — [P] https://phasmophobia.fandom.com/wiki/EMF_Reader , https://phasmophobia.fandom.com/wiki/Spirit_Box , https://phasmophobia.fandom.com/wiki/Salt , https://phasmophobia.fandom.com/wiki/Motion_Sensor
- Phasmophobia, proteção: crucifixo impede a caçada de **começar** dentro de uma esfera e gasta carga visível; incenso impede por 90 s ou cega o fantasma **durante** a caçada; sal nível III barra a passagem. Três verbos de tempo diferentes (antes, durante, onde). — [P] https://phasmophobia.fandom.com/wiki/Crucifix , https://phasmophobia.fandom.com/wiki/Incense , https://phasmophobia.fandom.com/wiki/Salt
- Com os níveis da 0.9.0, cada equipamento do Phasmophobia passou a ter três versões e a contagem foi de 22 para 60. — [B] https://www.sportskeeda.com/esports/phasmophobia-ascension-major-update-progression-system-reworks-equipment-tiers-reward-system-changes
- Demonologist diferencia duas proteções por alcance social e preço: a Salt Barrier "may be more beneficial to multiple players rather than buying a Crucifix for each player". — [P] https://demonologist.fandom.com/wiki/Salt_Barrier
- Outlast vai no sentido oposto: uma ferramenta só, que ganha um segundo sentido na continuação (microfone direcional). — [P] https://outlast.fandom.com/wiki/Camcorder

### Inferences

- **O que separa ferramenta útil de redundante, nos casos lidos, é a pergunta que ela responde e a forma do retorno, não o nome.** Três itens que "distraem por mais ou menos o mesmo tempo" viram um item e dois enfeites.
- Para os cerca de 13 itens do Sussurros, a régua do Phasmophobia seria: cada item tem um verbo próprio (onde ele está, se passou por aqui, se está olhando, afastar antes, afastar durante) e um canal sensorial próprio. Onde dois itens respondem "ele está perto?" com o mesmo tipo de som, a evidência sugere que o jogador vai usar um e esquecer o outro.
- A sobreposição também atrapalha o ensino: se o Sino e a Cantiga respondem com sons parecidos, o jogador não consegue aprender nenhum dos dois pelo contraste.

### Gaps

- Não achei crítica profissional dizendo, com todas as letras, que os equipamentos do Phasmophobia ficaram redundantes depois dos níveis.
- O tópico do Giant Bomb não foi lido (só o título).
- Não pesquisei Resident Evil, Signalis nem Dead Space sobre redundância de itens.

---

## 4. Escassez contra abundância, e a crítica "não vire jogo de progressão"

### Takeaway

A evidência nova aponta dois riscos opostos. Piorar de propósito a ferramenta inicial para vender a melhorada irritou os jogadores do Phasmophobia a ponto de os desenvolvedores recuarem. E repetir o mesmo gesto de proteção muitas vezes gasta o medo: "By the 300th time I dived under a table [...] I wasn't scared anymore — I was annoyed."

### Cited Findings

- Phasmophobia, depois da 0.9.0, os desenvolvedores reconheceram em nota ("Adjusting the Ascension"): "Leveling rates are currently too slow"; "Equipment Tier unlocks are too linear and too slow". Problemas citados de ferramentas de nível I: o D.O.T.S era "slightly too hard to use"; o ponteiro do EMF "often gets too close or even goes past the [5] mark". Mudanças: menos XP por nível, liberar vários níveis de uma vez, travar o ponteiro do EMF. — [P] https://devtrackers.gg/phasmophobia/p/a2f1c815-adjusting-the-ascension
- Guias da época aconselhavam largar os itens de nível I o quanto antes. — [B] (trecho de busca sem página identificada; ver lista de pendências)
- Alien: Isolation, crítica da Polygon: "By the 300th time I dived under a table or into a locker, I wasn't scared anymore — I was annoyed." IGN: "unnecessarily long, repetitive, and unforgiving". — [P] https://en.wikipedia.org/wiki/Alien:_Isolation
- Análise narrativa apresentada na GDC: "It is unfortunate that the game loses some of its intensity through its length: the story is often drawn out through meaningless steps (get the card key, find the password [...])". — [P] https://media.gdcvault.com/gdc2017/GameNarrativeReview/LeaTalbot_NarrativeReview_Alien.pdf
- Lança-chamas: "The flamethrower doesn't work all that consistently, but it can be a way to buy yourself an extra life after being spotted." A arma que espanta é mantida pouco confiável. — [P] https://kotaku.com/tips-for-playing-alien-isolation-1643391269 . Na wiki: a criatura "may (but will not always) pause" e "may (but will not always) retreat". — [P] https://alienisolation.fandom.com/wiki/Alien
- Outlast ajusta a escassez por dificuldade no limite de carga (10, 5 ou 2 pilhas) em vez de no número de ferramentas. — [P] https://outlast.fandom.com/wiki/Camcorder
- Amnesia ajusta a oferta ao jogador: o barril dá mais óleo a quem está com pouco. — [P] https://amnesia.fandom.com/wiki/Lantern_(The_Dark_Descent)
- Lethal Company: as ferramentas são compradas na loja do terminal, e o manual recomenda um "Survival Kit [...] for beginners". As melhorias que o terminal lista são da nave ("ship upgrades"); não li nada sobre níveis de ferramenta. — [P] https://lethal-company.fandom.com/wiki/Clipboard e https://lethal-company.fandom.com/wiki/Terminal
- Demonologist cobra da proteção forte em nível (10) e em preço (a mais cara). — [P] https://demonologist.fandom.com/wiki/Salt_Barrier

### Inferences

- **Ferramenta básica ruim de ler é o pior dos mundos:** o jogador novo, que mais precisa de retorno claro, recebe o retorno mais ambíguo. No Phasmophobia isso virou correção oficial. Para um mod sem níveis, a lição é que a primeira versão de cada item precisa ser a mais legível, não a mais fraca.
- **Escassez funciona melhor no recurso do que na quantidade de itens** (pilha, óleo, filme): poucos objetos, um combustível. Isso já é a linha do arquivo anterior (Cinza como moeda única); a evidência nova a reforça.
- **Proteção 100% confiável vira rotina; proteção com "pode ou não" preserva o medo**, ao custo de parecer injusta se o jogador não souber que é probabilística. O Demonologist resolve dizendo isso na descrição, sem números.
- Com 13 itens, o risco do Sussurros é mais o de "coleção para completar" do que o de escassez. Nada do que li mostra um jogo de terror elogiado por ter muitas ferramentas; os elogiados por elas (Phasmophobia, Lethal Company) são cooperativos e de partidas curtas, onde a variedade serve à divisão de tarefas.

### Gaps

- Não achei crítica específica a mods de terror de Minecraft por terem itens demais (além da frase sobre "progression" já citada no `PESQUISA-E-ANALISE.md`).
- Reddit ficou inacessível nesta rodada (o arquivo Arctic Shift devolveu erro 522 e os espelhos, 403/404/503), então faltam opiniões de r/PhasmophobiaGame, r/HorrorMinecraft e r/feedthebeast.
- Recepção crítica da 0.9.0 do Phasmophobia só pela nota dos desenvolvedores e por trechos de busca.

---

## 5. Canais de ensino próprios do Minecraft e como mods de terror e de magia os usam

### Takeaway

O Minecraft oferece cinco canais que não quebram a ficção por si sós: conquistas (a wiki as define como guia para novatos), livro de receitas, dica do item, livro-guia dentro do jogo e páginas de informação do JEI. Os mods de magia lidos usam o livro-guia como porta de entrada; os mods de terror lidos quase não documentam nada dentro do jogo e mandam para página externa, FAQ ou Discord. Não consegui ler opiniões de jogadores no Reddit, então a parte de "elogiado contra criticado" está fraca e vai marcada como tal.

### Cited Findings

**Conquistas (advancements)**

- A wiki antiga define o sistema como ensino: "Advancements are a way to gradually guide new players into Minecraft and give them challenges to complete". Cada conquista mostra um aviso deslizante no canto e, se a regra estiver ligada, uma mensagem no chat. — [P] https://minecraft.fandom.com/wiki/Advancement (wiki antiga; conferir detalhes na 26.2)
- From The Fog tem 21 conquistas próprias e uma opção de configuração para desligá-las. — [B] https://www.curseforge.com/minecraft/mc-mods/from-the-fog
- O arquivo anterior já registrou, lendo o código, que achar o santuário do From The Fog dispara uma conquista junto com o raio e a aparição.

**Livro de receitas**

- É um catálogo cuja primeira aba "contains every unlocked recipe"; receita sem material aparece em vermelho. — [P] https://minecraft.fandom.com/wiki/Recipe_book (wiki antiga)
- O livro de receitas diz **como fazer**, não **para que serve**. (O `CLAUDE.md` do Sussurros já exige o desbloqueio de receita por conquista para cada item.)

**Livro-guia dentro do jogo**

- Patchouli permite prender verbetes a conquistas: "Entries show up locked unless there's at least one unlocked entry within them." O jogador pode desligar todas as travas na configuração. — [P] https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/advancement-locking
- A própria documentação desaconselha travar páginas avulsas: é "allowed but not exactly encouraged, as it can be confusing to players if you aren't conveying the information properly". — [P] mesma página
- Occultism: o livro se chama Dictionary of Spirits e a entrada é um gesto comum do jogo: "Break grass until you drop Demon's Dream Seeds [...] Craft the seeds with a book to obtain the Dictionary of Spirits"; depois, "Follow the instructions in the dictionary". A página também aponta para uma wiki externa e para um vídeo. — [P] https://modrinth.com/mod/occultism
- Eidolon: Repraised: "craft the Ars Ecclesia: This book contains documentation of all the mod's features, including crafting recipes." Limite declarado: "It won't adapt to custom recipes or recipe removals!" — [P] https://modrinth.com/mod/eWX2m1eM
- Hexerei: a página do mod não diz como aprender nada; a descrição inteira é "I am absolutely horrible at writing these things, so if you like being a witch then this is perfect for you :)", com links para Discord e GitHub. — [P] https://modrinth.com/mod/hexerei
- Opinião de fórum sobre livros-guia: "Many guide books are either barebones, factual, or litter everything with stupid jokes". — [B] https://forum.feed-the-beast.com/goto/post?id=340457 (atribuição incerta: o trecho veio de um conjunto de resultados do fórum do FTB)

**Demonstração animada (Ponder, do Create)**

- "Create is a pioneer in the mod world by providing an amazing in-game tutorial/documentation system called 'Ponder'. If you ever wish to learn more about how a block works, you can hold the 'W' key." — [B] https://dominionhandbook.notion.site/Create-9b633e718f6b46f9b696b42365dd8fb2
- Num tópico de r/feedthebeast sobre mods de alta qualidade, o Ponder é descrito como uma mudança tão forte quanto foram WAILA e JEI. — [B] https://reddit.rtrace.io/r/feedthebeast/comments/1b0zi2e/what_are_some_high_quality_mods_and_why_do_they (o espelho devolveu erro 503)

**Dica do item (tooltip)**

- A convenção "Hold Shift for details..." é comum a ponto de existir mod só para aplicá-la a qualquer item: mostra uma linha curta e revela o resto com Shift. — [B] https://modrinth.com/mod/umCLwNyK
- Fora do Minecraft, dois modelos de texto de item apareceram nesta pesquisa. Só função: "The lantern will light up dark places." (Amnesia) — [P] https://amnesia.fandom.com/wiki/Lantern_(The_Dark_Descent) . Função com aviso vago de custo: "The more the ghost attacks, the lower the chance of protection will be." (Demonologist) — [P] https://demonologist.fandom.com/wiki/Salt_Barrier . Função, condições e leitura do retorno num parágrafo só (Spirit Box do Phasmophobia). — [P] https://phasmophobia.fandom.com/wiki/Spirit_Box

**Páginas de informação do JEI**

- A API tem método próprio para isso: `addIngredientInfo(ItemLike itemLike, Component... descriptionComponents)`, documentado como "Description pages show in the recipes for an ItemStack and tell the player a little about it." Linhas longas quebram sozinhas e textos longos viram várias páginas. — [C] https://github.com/mezz/JustEnoughItems/blob/26.3/Common/src/api/java/mezz/jei/api/registration/IRecipeRegistration.java
- O ramo padrão do repositório do JEI em 10/10/2026 chama-se `26.3`. — [C] https://api.github.com/repos/mezz/JustEnoughItems

**Mods de terror com itens: como documentam**

- From The Fog: há um FAQ no blog do CurseForge (06/05/2026) que responde "How to spawn the entity": ele aparece sozinho depois de 3 dias de jogo por padrão, e o jogador pode acelerar "building a Classic Herobrine Shrine (gold blocks, mossy cobblestone, and Netherrack)" e acendendo. Também diz o limite: "there is no weapon or ritual to kill him". A regra do santuário é ensinada fora do jogo. — [P] https://blog.curseforge.com/from-the-fog-frequently-asked-questions/
- Weeping Angels: dois itens, cada um com uma frase de função na página. O Timey Wimey Detector "will alert you when there are angels nearby"; o Chronodyne Generator serve para "zap out of existence" os anjos. A página não cita livro, dica de item nem wiki; aponta para Discord e GitHub. — [P] https://modrinth.com/mod/weeping-angels
- Scape and Run: Parasites: a página não explica as fases de evolução; manda "Please read the FAQ" numa wiki externa. — [P] https://www.curseforge.com/minecraft/mc-mods/scape-and-run-parasites
- Sanity: Descent Into Madness: entre os chamados abertos no GitHub há "Sitting down (in general) lowers your sanity." (nº 90, 04/01/2025) e um pedido de configuração do lado do cliente (nº 105). Só li os títulos. — [P] https://github.com/croissantnova/SanityDescentIntoMadness/issues?q=is%3Aissue
- Whisperwoods (do arquivo anterior): o FAQ precisa responder "Why isn't the Hidebehind doing anything or moving?", porque a regra invisível é lida como defeito.

### Inferences

- **Há um buraco entre os canais.** O livro de receitas e o JEI dizem como fabricar; a conquista diz que algo aconteceu; nenhum dos dois diz o que o item fez **agora**. Nos jogos de terror lidos, essa parte é sempre retorno no mundo (luz vermelha, braço queimado, marca no sal). Os canais do Minecraft servem para a apresentação do item, não para o retorno do uso.
- **Para um dono que não quer spoiler, o canal mais compatível é o que só fala depois do fato:** conquista disparada no primeiro uso bem-sucedido (com descrição curta, na ficção) e verbete do Caderno que aparece depois que a coisa aconteceu. É o modelo do bestiário do Lethal Company (o arquivo só existe depois do scan) e das travas por conquista do Patchouli.
- **A dica do item pode seguir o modelo do Demonologist:** uma linha de função e uma linha que admite que há um preço, sem dizer qual. Isso resolve "eu não sabia que custava" sem virar tabela.
- **Um livro-guia completo logo de início é o padrão dos mods de magia, não dos de terror.** Os de terror lidos escolheram não documentar no jogo e pagar com FAQ externo. Para um mod privado entre amigos, o FAQ externo não existe: ou o jogo ensina, ou ninguém ensina.
- Página de informação do JEI é barata de fazer e fica fora do caminho de quem não procura, mas depende de o JEI (ou REI/EMI) estar instalado e existir para a 26.2 no Fabric, o que não conferi.

### Gaps

- **Opinião de jogadores sobre mods aprendíveis sem wiki contra mods que exigem wiki:** não consegui ler nenhum tópico do Reddit; o que há são trechos de busca. Não tenho citações lidas sobre Botania, Thaumcraft, Twilight Forest, Ars Nouveau nem Malum.
- **Modonomicon:** não li a documentação nem confirmei que o Occultism o usa (a página do Modrinth não diz qual mod de livro usa).
- **Disponibilidade para Minecraft 26.2 no Fabric** de Patchouli, Modonomicon, JEI, REI e EMI: não conferida. Um site de terceiros anuncia "Patchouli (26.2)". — [B] https://www.9minecraft.net/patchouli-9minecraft-mod/wiki/ (site de redistribuição, pouco confiável)
- **The Obsessed e Cave Dweller:** nada novo sobre como documentam; continuam como no arquivo anterior.
- **Scape and Run: Parasites:** como o jogador fica sabendo da fase dentro do jogo. A única página detalhada que achei (pontos, espera de 20 minutos entre fases, "mostrar um relógio a uma Factory") é da versão do mod para **Terraria**, não da de Minecraft, e por isso não foi usada. — [P] https://terrariamods.wiki.gg/wiki/Scape_and_Run_Parasites/Evolution_Phases
- **Mensagens de primeiro uso no chat ou na barra de ação:** não achei mod de terror que documente usar isso, nem crítica a favor ou contra.
- Chamados do mod Sanity: só títulos da primeira página, não o conteúdo.

---

## 6. Contraevidência: ferramentas sem explicação que são amadas, e casos em que explicar matou a tensão

### Takeaway

Há evidência forte dos dois lados. Jogadores de Phasmophobia dizem que o medo vinha de não entender as regras e some com o domínio; Don't Starve, Tunic e Lethal Company são elogiados por não explicar. O que esses casos têm em comum é que **o jogador percebe que existe algo a entender**: o que fica escondido é a regra, não a existência do sistema.

### Cited Findings

**Explicar tira o medo**

- Phasmophobia (tópico de 03/10/2023, Steam): "Most of the horror in this game comes from not understanding the mechanics or possible events. That will fade in time no matter what"; "Most things are scary because they are unknown. Once you've played it a hundred times, it ceases to be scary."; "Real horror is the fear of the unknown." — [P] https://steamcommunity.com/app/739630/discussions/0/3883849331775033367
- Outro tópico (25/07/2023): "Once breaking down the hunt game mechanic, Phasmophobia is not that scary." — [P] https://steamcommunity.com/app/739630/discussions/0/3807281445021448647
- O mesmo tópico de outubro traz o outro lado do domínio: "Graphics and Horror elements is what gets people into a game like this, but gameplay keeps players interested into it for longer periods". — [P] https://steamcommunity.com/app/739630/discussions/0/3883849331775033367
- (Do arquivo anterior, sem repetir: Grip diz que a vagueza fez os jogadores do Amnesia inventarem teorias e terem mais medo, e que parte deles passou a tratar tudo como sistema.)

**Não explicar, de propósito, e ser elogiado**

- Don't Starve: a Klei tirou tutorial e metas. Segundo Jamie Cheng, quem testava a versão com objetivos "would get bored after a few minutes once they completed the goal laid before them". — [P] https://articles.retroware.com/2022/01/19/the-uncompromising-history-of-dont-starve/
- Tunic (2022): o manual é coletado página por página, fora de ordem, e "The majority of the manual is in a constructed writing system". Ele ensina por desenho coisas que o jogador podia fazer desde o início, como oferecer itens a um santuário, "which otherwise gives no indication that it can be used that way". O autor queria "a sense of being presented with something that has meaning, but which was not understandable by the player". — [P] https://en.wikipedia.org/wiki/Tunic_(video_game)
- Recepção do Tunic: GameSpot chamou os enigmas de "utterly fantastic"; Rock Paper Shotgun e Game Informer notaram que o jogador pode ficar travado, sobretudo perto do fim. — [P] mesma página
- Subnautica: a palestra de Charlie Cleveland na GDC 2019 lista "mysterious tooltips" entre os recursos usados para criar "feelings of exploration, discovery and the unknown", junto com sinais de rádio para dar estrutura e "story beats that didn't overly direct or guide the player". — [P] https://www.gamedeveloper.com/design/video-inside-the-design-of-i-subnautica-i-
- Lethal Company: o manual da nave omite avisos de propósito e diz isso na capa ("Certain cautions or warnings are absent to increase readability"). — [P] https://lethal-company.fandom.com/wiki/Clipboard
- Metro 2033: o sistema moral nunca é explicado e é elogiado justamente por isso (ver seção 2). — [P] https://spacebiff.com/2012/08/10/metro-2033-act-three/
- Dead by Daylight apresenta as oferendas como descoberta acidental de um personagem, não como regra. — [P] https://deadbydaylight.fandom.com/wiki/Offerings

**Não explicar e ser criticado**

- Devour: "This game has NO instructions to start off." — [P] https://steamcommunity.com/app/1274570/discussions/0/3808407347833668817
- Alien: Isolation: "doesn't do a very good job of explaining itself". — [P] https://kotaku.com/tips-for-playing-alien-isolation-1643391269
- Phasmophobia, incenso: sem indicação de que funcionou. — [P] https://phasmophobia.fandom.com/wiki/Incense
- The Mortuary Assistant: quando um símbolo aparece em branco, o jogador não sabe se é regra ou defeito. — [P] https://steamcommunity.com/app/1295920/discussions/0/3457094584913988448

### Inferences

- **A linha que separa os dois grupos não é "quanto se explica", e sim "o jogador sabe que há uma pergunta?".** No Tunic há páginas faltando, visíveis como faltando. No Lethal Company há um manual que avisa que omite. No Metro há um clarão. No Don't Starve há um ícone com seta. Em todos, o jogador sabe que existe um sistema e tem um lugar para olhar. Nos casos criticados (incenso, efígies, Devour), o jogador não recebe nem a pergunta.
- **O Sussurros, no teste, estava no grupo criticado por um motivo específico:** o custo existia, mas nada dizia ao jogador que havia custo. Isso é diferente de esconder a regra do custo.
- **O que a contraevidência recomenda preservar:** os números, os limites e a regra exata da cobrança podem continuar escondidos (Amnesia, Metro, Krampus). O que ela não sustenta é esconder a existência da ligação entre usar o item e ouvir algo depois.
- **O medo que vem de não saber acaba de qualquer jeito** (Phasmophobia). Segurar a explicação compra tempo de mistério; não compra medo permanente. Para um mod jogado pelo próprio dono e amigos, esse tempo é o que importa, mas ele só vale se as primeiras horas forem legíveis o bastante para o jogador continuar usando os itens.
- **O manual do Tunic é um modelo direto para o Caderno:** páginas que chegam aos poucos, parte ilegível, ensinando por desenho coisas que o jogador já podia fazer.

### Gaps

- Não achei um caso documentado em que um desenvolvedor **acrescentou** instruções claras a uma ferramenta de terror e mediu queda de tensão. A evidência de "explicar mata" é de jogadores falando de domínio com o tempo, não de mudança de texto no jogo.
- Não li entrevista do Andrew Shouldice na íntegra (só a Wikipedia).
- Não achei fala de desenvolvedor de mod de terror de Minecraft defendendo não documentar os itens.

---

## Tabela-resumo e pendências (pedido na tarefa)

### Tabela: ferramenta, quando chega, como o primeiro uso se lê, como o custo é aprendido

| Ferramenta | Quando o jogador recebe | Como o primeiro uso se lê | Como o custo é aprendido | Fonte |
|---|---|---|---|---|
| Rastreador de movimento (Alien: Isolation) | Depois de ver a criatura matar Axel; antes de ela caçar solta (missão exata não confirmada) | Círculo na tela para o que se move; barra para o objetivo [B]; o sumiço do ponto também assusta | Morrendo num armário e por guias; o jogo "doesn't do a very good job of explaining itself" | [P] Wikipedia, Kotaku, PDF da GDC |
| Leitor de EMF (Phasmophobia) | Equipamento inicial; no treino, na etapa 4 | Garantido: objetos são arremessados e a porta só abre com EMF 5 | Eletrônico ligado atrai o fantasma na caçada (7,5 m). A etapa 11 do treino só solta o fantasma depois que o jogador se esconde e desliga os eletrônicos | [P] wiki Training e EMF_Reader |
| Spirit Box (Phasmophobia) | Equipamento inicial; no treino, na etapa 10 | Garantido: precisa obter resposta para avançar. Luz vermelha = ouviu; branca = respondeu | Descrição do item pede ficar no escuro; a perda de sanidade vem da regra geral de luz, ensinada nas etapas 2 e 3 | [P] wiki Training e Spirit_Box |
| Crucifixo (Phasmophobia) | Comprável | Esfera translúcida ao colocar; ao funcionar, queima, brilha e perde um braço | Cargas visíveis no próprio objeto (1 ou 2) | [P] wiki Crucifix |
| Incenso (Phasmophobia) | Nível 14 | Só fumaça: "There is no explicit indication" de que funcionou | Uso único, dito na descrição; duração contada pelo jogador no relógio de pulso | [P] wiki Incense e Watch |
| Sal (Phasmophobia) | Nível 8 | Marca na pilha e passos audíveis quando o fantasma pisa | Usos contados no frasco (2 ou 3) | [P] wiki Salt |
| Sanidade (Phasmophobia) | Etapa 2 do treino, antes de qualquer ferramenta | Monitor no caminhão (±2%), anel no relógio, seção no diário | Tomando o remédio no treino; em jogo, voltando ao caminhão. Em Nightmare a leitura é retirada | [P] wiki Sanity, Sanity_Monitor, Watch, Journal |
| Câmera Obscura (Fatal Frame) | Herdada ou achada no começo | Filamento azul (pista ou fantasma manso) ou vermelho (hostil), mais forte com a proximidade; círculo que carrega | Sem filme não fotografa; filmes fortes são limitados | [P] wiki Camera_Obscura |
| Lampião (Amnesia: TDD) | Segunda área, sem inimigos; cena própria, "hard to miss" | Ilumina; descrição só de função | Óleo no inventário; inimigos veem a luz (como o jogo ensina isso não foi confirmado) | [P] wiki Lantern e Old_Archives |
| Lanterna de dínamo (The Bunker) | (ver arquivo anterior) | Barulho alto na primeira puxada | Dica de morte em fala de soldado: "loud as hell" | [P] wiki Death_hints |
| Filmadora (Outlast) | Desde o início, com 2 pilhas no Normal | Visão noturna; momentos em que o jogo levanta a câmera sozinho | A imagem pisca quando a pilha acaba; sem pilha o alcance cai, não zera | [P] wiki Camcorder |
| Barricadas e gerador (Darkwood) | Prólogo-tutorial; bilhete ao lado do gerador no primeiro esconderijo | As luzes apagam quando o gerador seca, já no prólogo | Tábuas e pregos gastos; a loja avisa que não há "hand holding" | [P] wiki Prologue, Dry_Meadow_Hideout, Barricade; loja Steam |
| Scanner (Lethal Company) | No traje, desde o início; prancheta-manual na mesa da nave | Círculos verde, vermelho e azul; criatura nova vira ficha no terminal | Para escanear é preciso chegar perto [B]; duas ameaças não são escaneáveis | [P] wiki Scanner, Clipboard, Terminal |
| Sanidade (Don't Starve) | Sempre no HUD | Ícone de cérebro com seta cujo tamanho é a taxa | A seta aparece na hora em que o item mágico é usado | [P] wiki Sanity |
| Scanner (Subnautica) | Fabricado cedo (receita não conferida) | Ícone no canto avisa que há algo escaneável; círculo de progresso | Bateria; cerca de dez segundos parado perto do alvo | [P] wiki Scanner_(Subnautica) |
| Papel e lápis (The Mortuary Assistant) | Primeiro turno da noite, depois do tutorial; explicado numa fita | Rabisco que muda de linhas para símbolos; marcas de 1 a 9 | Não tem custo; é o leitor do custo (possessão) | [P] wiki Paper_and_Pencil e Possession |
| Tira de sangria e cinzas (The Mortuary Assistant) | Turnos da noite; regras em fitas reouvíveis | A tira fumega perto do símbolo [B]; as cinzas queimam num símbolo de casa | Cinzas uma vez por noite; errar o corpo leva a outro final | [P] wiki Items; [B] gamerjournalist |
| Itens de ritual (Devour) | Espalhados pelo mapa | Sem instrução: "NO instructions to start off" | Por tentativa, vídeos e jogadores veteranos | [P] wiki Ritual_Items; tópico Steam |
| Oferendas (Dead by Daylight) | Meta-jogo | Apresentadas por um diário como descoberta acidental | Consumidas ao usar (mecânica não relida) | [P] wiki Offerings |
| Salt Barrier (Demonologist) | Nível 10; a mais cara | Círculo de sal no chão | Descrição avisa que a chance cai a cada ataque; números (90/70/30%) só na wiki | [P] wiki Salt_Barrier |
| Efígies (Sons of the Forest) | Construção | Sem retorno claro; jogadores perguntam se funcionam | Testes de jogadores | [B] tópicos Steam |
| Pontos morais (Metro 2033) | Nunca apresentados | Clarão azul e som de gota ou sussurro; tela escurece ao perder | Não é aprendido pela maioria; só no final | [P] wiki Moral_Points; Space-Biff |
| Caos (Dishonored) | Dica geral; valor escondido | "High Chaos" na tela de fim de missão; mais ratos e guardas | Pelo resumo de missão e pelo mundo; parte dos jogadores sente como bronca | [P] wiki Chaos; tópico Steam |
| Manual (Tunic) | Páginas achadas fora de ordem | Desenhos e mapas; texto quase todo ilegível | Não tem custo; risco de travar perto do fim | [P] Wikipedia |
| Santuário (From The Fog) | Construção do jogador; receita em FAQ externo | Raio, conquista, aparição (lido no código, arquivo anterior) | FAQ: acelera a chegada; "there is no weapon or ritual to kill him" | [P] blog CurseForge |
| Dictionary of Spirits (Occultism) | Quebrar grama, pegar sementes, juntar com um livro | "Follow the instructions in the dictionary" | No próprio livro | [P] Modrinth |
| Ars Ecclesia (Eidolon: Repraised) | Fabricar no começo | Documenta "all the mod's features" | No próprio livro; não acompanha receitas alteradas | [P] Modrinth |
| Timey Wimey Detector (Weeping Angels) | Fabricar | "will alert you when there are angels nearby" (uma frase na página) | Não documentado na página | [P] Modrinth |

### O que continua sem verificação

1. **Alien: Isolation:** missão exata do rastreador; se o primeiro uso mostra um sinal garantido; fala de desenvolvedor sobre o custo sonoro; a barra de objetivo (só [B]). A frase "adquire o rastreador e então contata Samuels" é [B].
2. **Citações lidas pelo modelo auxiliar** (Steam, Kotaku, Wikipedia, Modrinth, CurseForge, Patchouli, Space-Biff, Retroware, DevTrackers, Game Developer): podem ter pequenas diferenças do original. As de `fandom.com`, Europe PMC e do código do JEI são literais.
3. **Phasmophobia:** a afirmação de que o treino não tem fantasma real está marcada "a verificar" na própria wiki; o motivo da retirada dos sons falsos de sanidade (09/2020) não foi achado; a notícia da GameSpot sobre o fantasma ouvir a voz é só [B]; o conselho de "largar o nível I" veio de trecho de busca sem página identificada.
4. **Fatal Frame:** existência de um primeiro fantasma garantido no prólogo do primeiro jogo.
5. **Amnesia e Outlast:** texto exato das dicas na tela que ensinam o lampião e a visão noturna.
6. **The Mortuary Assistant:** funcionamento da tira de sangria é [B]; a página própria do item não existe na wiki.
7. **Sons of the Forest, The Forest, Pacify, Signalis, Resident Evil:** sem leitura nova nesta rodada.
8. **The Witcher:** o uso de flashbacks para lembrar a decisão é [B] (entrevista do Madej não lida).
9. **Metro 2033:** o número de 3,9% vem de um blog de 2012.
10. **Dishonored 2:** mudanças na legibilidade do caos não lidas.
11. **Reddit inteiro:** inacessível nesta rodada. Faltam opiniões de jogadores sobre mods aprendíveis sem wiki, sobre itens de mods de terror e sobre a sanidade do Phasmophobia para novatos.
12. **Minecraft 26.2 no Fabric:** disponibilidade de Patchouli, Modonomicon, JEI, REI e EMI não conferida. O ramo padrão do JEI chamar `26.3` é fato lido; o que isso significa para a 26.2 não foi conferido.
13. **Occultism usar Modonomicon; Create/Ponder; convenção "Hold Shift":** só [B].
14. **Scape and Run: Parasites (Minecraft):** como a fase é comunicada no jogo. A fonte detalhada encontrada é da versão para Terraria e foi descartada.
15. **Wiki antiga do Minecraft** (`minecraft.fandom.com`) para conquistas e livro de receitas: conferir contra a 26.2.
16. **Extrapolação da psicologia:** os estudos de Buehner e colegas usam atrasos de segundos em tarefas de laboratório. Aplicar a conclusão a 30 a 120 s num jogo é inferência destas notas, não resultado medido.
