# Sussurros — plano das novas mecânicas (SPOILERS)

Plano escrito em 08/10/2026 a partir do `PESQUISA-E-ANALISE.md`. Ele diz o que construir, em que ordem e como testar cada parte. É uma proposta: o `ROADMAP.md` não foi alterado.

**Objetivo:** o mod é para assustar o dono e, talvez, alguns amigos. Não vai ser publicado. Então a régua é uma só: o susto funciona em quem joga, inclusive depois de muitas horas e inclusive em quem escreveu o mod.

---

## Os sete princípios deste plano

1. **Perceptível antes de inteligente.** Se o jogador não ouviu nem viu, não aconteceu. O Diretor só aprende com o que foi confirmado como perceptível.
2. **Mexer na percepção, não no mundo.** Sempre que der, a mudança existe só para um jogador (som, bloco, criatura). A construção de ninguém é estragada, e entre amigos cada um vive uma coisa diferente.
3. **Longe e curto pode ser frequente; perto e físico tem de ser raro.** É o que o From The Fog faz: vultos distantes a cada um ou dois minutos, que somem em um segundo, e eventos físicos uma vez a cada várias horas. Raro demais também falha: o jogador conclui que o mod quebrou.
4. **Menos eventos, todos sentidos.** Hoje acontece algo a cada ~65 s e a maioria passa despercebida. A meta é o contrário.
5. **Regra aprendida é regra que pode ser quebrada.** Se "encarar faz sumir" sempre funciona, deixa de assustar. De vez em quando não funciona.
6. **Nem o autor sabe.** Cada mundo sorteia quais mecânicas estão ligadas e quando. Sem isso, quem leu o plano não se assusta.
7. **O medo está no antes.** O momento que os jogadores mais citam é o som distante antes do encontro, não o encontro. O mesmo aviso serve para evento falso e para evento real, para nunca virar dica confiável.

Os princípios 3 e 7 vêm da segunda rodada de pesquisa (seções 12, 13 e 17 do `PESQUISA-E-ANALISE.md`).

---

## Visão geral

| Etapa | O que você vai sentir | Tamanho | Depende de |
|---|---|---|---|
| 0. Destravar o git | Nada no jogo | pequeno | você mesclar os PRs |
| 1. Ouvir e ver de verdade | Os sons distantes passam a existir; o mod para de aprender errado | médio | etapa 0 |
| 2. Sentidos do cliente | A criatura some aos poucos; nunca nasce dentro da tela | médio | etapa 1 |
| 3. Aparições "vi coisa" | Vultos no canto da tela que somem antes de você olhar | médio | etapa 2 |
| 4. Som que perturba | Sussurro com voz; o mundo emudece na hora do susto | médio, mais gravação | etapa 2 |
| 5. Miragens | Tochas e portas que só você vê diferentes | médio | etapa 1 |
| 6. Sustos grandes | Um pico raro por sessão longa, nunca o mesmo duas vezes seguidas | grande | etapas 3, 4 e 5 |
| 7. Amigos | Cada um vive uma assombração; ninguém confia no relato do outro | médio | etapas 3, 4 e 5 |
| 8. Surpresa para o autor | Você não sabe o que está ligado no seu mundo | pequeno | etapa 6 |

Tamanho: pequeno é uma sessão de trabalho, médio são duas ou três, grande são várias. As etapas 3, 4 e 5 podem andar em paralelo.

---

## O primeiro marco: a primeira noite com os amigos

Servidor de amigos dura umas duas semanas, e o plano inteiro leva bem mais que isso para construir. Então a ordem não é "fazer tudo e depois jogar". É chegar a um conjunto pequeno que já funciona em grupo, jogar com eles e deixar o resto ser guiado pelo que funcionou.

**Entra no marco**

- Etapas 0, 1 e 2 inteiras, com o arquivo de configuração.
- Da etapa 3: o relance (3a), só o alvo vê (3d), as regras de lugar (3f) e a silhueta (3g).
- Da etapa 4: som privado (4a), silêncio de verdade (4c) e o chat de voz por proximidade (4g). O sussurro com voz (4b) entra se já estiver gravado.
- Da etapa 5: o sistema de miragem e duas ou três miragens.
- Da etapa 7: quem é assombrado (7a).

**Fica para depois do marco:** os sustos grandes (etapa 6), o sósia, a voz roubada, o isolamento e o baralho da sessão.

**Antes de chamar os amigos**

1. Tornar o repositório privado. Hoje ele é público, com todos os spoilers.
2. Montar um pacote só com o que eles instalam: Sussurros, Fabric API e Simple Voice Chat.
3. Combinar que a voz é a do jogo, não a do Discord.
4. Não contar o que o mod faz.

---

## Como vamos trabalhar

1. Cada parte vira uma branch e um PR para a `main`.
2. Quem programa compila e roda os testes antes de cada commit.
3. O dono mescla o PR, joga 30 a 40 minutos com `/sussurros debug on` e manda o arquivo de log.
4. O log é lido e os números são ajustados. Foi um log assim que revelou os onze problemas da análise.

Quem programa não joga. Build e testes verdes não provam que algo funciona dentro do jogo, então cada etapa abaixo diz o que conferir.

Técnica que nunca foi testada na 26.2 ganha primeiro um comando de experimento (`/sussurros teste ...`). Só depois que ele funcionar em jogo é que uma mecânica é construída em cima.

**Os três primeiros experimentos** vão juntos num PR pequeno, logo depois da etapa 1. Cada um decide se uma etapa inteira segue como planejada:

1. `/sussurros teste sussurro`: um arquivo estéreo toca sem direção?
2. `/sussurros teste miragem`: a tocha falsa ilumina? O que acontece ao clicar nela?
3. `/sussurros teste sosia <jogador>`: a pele aparece e o rótulo some?

---

## Etapa 0 — Destravar o git

**Por quê:** a `main` está dois passos atrás do código real. Todo trabalho novo mexe em `Diretor`, `Atmosfera` e `Leitura`, e precisa partir da versão já dividida.

**Passos**

1. O dono mescla o PR #3 (correções). Ele já jogou 44 minutos nessa branch sem erro.
2. O dono tira o PR #2 de rascunho e mescla.
3. `refactor/passo-3-cenas` é reaplicada sobre a `main` nova e ganha um PR.
4. O dono mescla o PR do passo 3.

---

## Etapa 1 — Ouvir e ver de verdade

Corrige o que o log mostrou. São PRs pequenos e separados, porque cada um muda um comportamento.

### 1a. Alcance dos sons

- **O que muda:** `Atmosfera.sinalDistante`, `ruidoRetorno`, `passagem` e o presságio `RUIDO_NA_PAREDE` passam a usar `Diretor.volumePara`. Em `Diretor.sinalFalso` (subsolo) e em `ruidoRetorno`, o lugar da ação antiga só é usado se estiver a até 30 blocos.
- **Como testar:** parado num lugar quieto, `/sussurros evento sinal_distante` e `/sussurros evento ruido_retorno`. Agora dá para ouvir, baixo e ao longe.
- **No log:** nenhuma linha `SINAL` com `dist` acima de 45.

### 1b. Evento que falha tenta outro

- **O que muda:** em `Diretor.decidir`, se o evento sorteado não executar, sorteia outro entre os que sobraram (até três tentativas no mesmo segundo). A agenda só anda depois que algo de fato aconteceu. `Atmosfera.podeEvento` passa a checar o mesmo que a execução exige.
- **No log:** toda linha `SELECAO` é seguida de `EVENTO` ou de uma linha nova `SELECAO falhou evento=... tentando=...`.

### 1c. Leitura de reação

- **O que muda em `Leitura.avaliar`:**
  - o giro vira uma rampa (começa a contar em 30°, vale o máximo em 90°) em vez de um corte em 60°;
  - "fugiu" exige se afastar da fonte ou começar a correr logo depois do evento, não só estar mais rápido;
  - "investigou" exige mudar de rumo em direção à fonte, comparando com o rumo dos 3 s anteriores;
  - "percepção confirmada" só com giro e olhar para a fonte.
- **O que muda no `Diretor`:** teleporte, respawn e troca de dimensão cancelam a leitura em andamento e limpam o Rastro e as ações guardadas. A punição por indiferença só conta eventos com observabilidade de 0,7 ou mais.
- **Testes:** `LeituraTest`, com casos tirados do log (58° olhando para a luz; andar pelo próprio rastro; começar a correr sem motivo; teleporte).
- **No log:** some a combinação `fugiu=true ... investigou=true` na mesma linha.

### 1d. O Olho deixa de ser botão

- **O que muda:** em `Diretor.usarOlho`, a chance de chamar uma aparição cai a cada uso recente e há uma recarga de alguns minutos. Aparição de origem `OLHO` não soma em `VEZES_VISTO`.
- **Como testar:** usar o Olho três vezes seguidas na fase 3. No máximo uma aparição.

---

## Etapa 2 — Sentidos do cliente

**O que você vai sentir:** a criatura some aos poucos em vez de piscar para fora. Ela nunca nasce dentro da tela, mesmo com FOV alto ou correndo. Atrás de vidro e de folhas ela passa a "saber" que foi vista.

É a fundação das etapas 3 e 4. Hoje o servidor adivinha o que está na tela com um cone fixo; aqui o jogo do jogador passa a informar.

**O que muda**

- Três pacotes novos, registrados com `PayloadTypeRegistry`:
  - `visao` (cliente para servidor): para cada Hóspede carregado, se ele está de fato na tela e a quantos graus do centro. O cliente usa `Camera.getCullFrustum()` e um raio visual até a cabeça, o tronco e os pés. Enviado quando muda e a cada 10 ticks.
  - `campo` (cliente para servidor): o FOV efetivo (`Camera.getFov()`) e a proporção da janela. Enviado quando muda.
  - `efeito` (servidor para cliente): ordens de apresentação, como "suma aos poucos", "cale a música", "nível do fundo sonoro".
- Classe nova `Percepcao` no servidor: guarda o último relato de cada jogador. `Diretor.estaVendo` e `naTela` passam a consultá-la. Se o relato tiver mais de 1 s, vale o cone de hoje.
- `HospedeEntity`: dado sincronizado novo para o sumiço. `sumir` com fade marca o dado e descarta a entidade alguns ticks depois.
- `HospedeRenderer`: usa `getModelTint` e um tipo de desenho translúcido enquanto o fade corre.

**Como testar**

- `/sussurros debug visao`: mostra "NA TELA" ou "FORA" acima da hotbar, ao vivo. Conferir atrás de vidro, entre folhas, na borda da tela, com FOV 110 e correndo.
- `/sussurros evento presenca` várias vezes com FOV alto: ele nunca aparece do nada dentro da tela.

**Arquivo de configuração (faz parte desta etapa):** os números de ajuste saem do código e vão para um arquivo, com `/sussurros recarregar`. O método de trabalho é jogar, ler o log e ajustar; sem isso cada ajuste exige recompilar. Quase todos os números são do servidor, então ajustar entre uma noite e outra não obriga os amigos a baixar nada.

---

## Etapa 3 — Aparições que parecem "eu vi alguma coisa"

### 3a. Relance

**O que você vai sentir:** um vulto no canto do olho. Você vira e não há nada. Às vezes, depois, o Olho aponta para aquele lugar: tinha.

- **Como funciona:** modo novo `RELANCE` no `HospedeEntity` e evento novo `RELANCE` (categoria VISAO, intensidade baixa, a partir da fase 2).
- **Onde nasce:** logo depois da borda real da tela (5° a 25° para fora), a 14–28 blocos, num ponto que dá para ver ao virar. Usa `Aparicao.buscarAoRedor` com o FOV informado pelo cliente.
- **Quando some:** de 0,25 a 0,6 s depois de entrar na tela, ou no instante em que ficar a menos de 20° do centro. Some com fade, sem som e sem partícula.
- **Se ninguém olhar:** some sozinho depois de 20 a 40 s e não conta como evento percebido.
- **Prova:** em uma minoria das vezes fica um vestígio (`Vestigios`), que o Olho e o Sino conseguem apontar depois.
- **Aprendizado:** a leitura começa no primeiro quadro em que o cliente confirma "na tela". É o primeiro evento do mod com percepção confirmada.
- **Não conta** para `VEZES_VISTO` nem para a ousadia.
- **Como testar:** `/sussurros evento relance`. No log, linhas `RELANCE criado`, `RELANCE na tela ang=...`, `RELANCE sumiu motivo=TEMPO|CENTRO|NINGUEM_VIU`.

### 3b. Sumir quando algo passa na frente

**O que você vai sentir:** ele estava ali; você andou, um tronco tampou a visão por um instante, e ele não saiu do outro lado.

- **Como funciona:** depois de visto, se o cliente relatar "tampado" por 3 ticks ou mais, a entidade é descartada sem fade. Vale para `OBSERVAR`.

### 3c. Saída pela cobertura, de costas e reação sorteada

- **Saída pela cobertura:** quando vai sumir e há um bloco sólido ao lado, ele dá um passo para o lado escondido e só então é descartado.
- **De costas:** variante visual sorteada. Fica de costas e só vira a cabeça quando você desvia o olhar e volta.
- **Reação ao ser encarado:** a partir de certo número de avistamentos, encarar deixa de ser garantia. A maioria das vezes ele some; às vezes inclina a cabeça e fica mais um pouco; raramente dá um passo na sua direção e some quando você piscar a câmera.
- **Como testar:** `/sussurros evento presenca` com `VEZES_VISTO` alto (comando de teste para ajustar esse número).

### 3d. Só o alvo vê

- **Como funciona:** `HospedeEntity` sobrescreve `broadcastToPlayer` e só é enviado ao jogador que ele assombra. Os sons dele vão só para esse jogador.
- **Como testar:** com um amigo ao lado, `/sussurros evento presenca`. Só você vê.

### 3e. Olhos no breu (opcional)

- Dois pontos claros no escuro total, sem corpo. Somem se a luz no lugar subir ou se forem encarados com precisão. Precisa de desenho que brilha no cliente.

### 3f. Regras de lugar para toda aparição

Vêm do que os jogadores dizem ("looks great behind the tree but just looks too goofy out in the open") e dos números do From The Fog. No log analisado, as três aparições nasceram de dia, em campo aberto e sem cobertura.

- **De dia e em campo aberto:** só a 25 blocos ou mais, ou meio encoberta por tronco, parede ou neblina. Nunca perto e inteira.
- **Vulto distante (novo):** a 50–100 blocos, some 1 s depois de mirado. Pode ser mais frequente que os outros, porque é negável.
- **Sem plateia:** se ninguém olhar em cerca de 30 s, some sozinha.
- **Lugar ruim cancela:** se a cabeça ficar dentro de folhas ou de um bloco, ou se cair na água, some sem contar como evento.
- **Som de sumiço:** no máximo em metade das vezes.
- **Duração na tela:** de 2 a 5 s para as aparições paradas, salvo o Relance, que é mais curto.

### 3g. Silhueta

**O que você vai sentir:** de longe, um recorte preto contra a paisagem. De perto, nenhum detalhe para se acostumar.

- **Por quê:** o veredito da análise é que o ponto fraco é som e imagem, e os sustos grandes mostram a criatura de perto. O mod The Hollow resolve isso desenhando a entidade sem luz, como uma silhueta chapada com dois olhos pálidos.
- **Como funciona:** o `HospedeRenderer` passa a desenhar o corpo sem a iluminação do lugar (sempre escuro, até de dia). Em uma minoria das aparições, os olhos brilham.
- **Muda uma decisão antiga:** o `DESIGN-SPOILERS.md` registra os olhos como não emissivos de propósito. Por isso entra primeiro como experimento (`/sussurros teste silhueta`), para o dono comparar as duas formas em jogo.
- **Animação de verdade:** o GeckoLib tem versão para Fabric 26.2 (5.5.5). Continua fora até a silhueta se mostrar insuficiente.

---

## Etapa 4 — Som que perturba

### 4a. Som privado

- **O que muda:** `ModSons` ganha `tocarPara(jogador, ...)`, que envia `ClientboundSoundPacket` só para ele. Os sons da assombração passam a usá-lo. Sons de coisas que mudaram de verdade no mundo (uma porta que abriu) continuam públicos.

### 4b. Sussurro com voz

**O que você vai sentir:** uma voz baixa, sem direção, que diz o seu nome. Às vezes vem de trás de uma parede.

- **Produção:** o dono grava de 10 a 15 frases curtas sussurradas (celular serve), cada uma duas ou três vezes para ter variação. Se os amigos toparem, gravam também.
- **Tratamento:** as receitas estão na seção 16 do `PESQUISA-E-ANALISE.md` (eco invertido, corte de agudos para "atrás da parede", tom mais grave). São ponto de partida e não foram testadas. O `ffmpeg` não está instalado na máquina; o Audacity resolve à mão.
- **O que muda:** `Diretor.sussurro` toca o áudio; o texto em cima da hotbar vira legenda opcional. Duas formas: "na cabeça" (sem posição) e posicional abafado.
- **Formato:** a wiki do Minecraft diz que arquivo mono tem posição e estéreo não. Então o sussurro "na cabeça" pode ser um `.ogg` estéreo, sem código no cliente. Conferir em jogo antes de gravar tudo.
- **De graça enquanto a voz não chega:** a respiração do jogador que o próprio jogo tem (`SoundEvents.PLAYER_BREATH`) e o ambiente de caverna tocado grave.
- **Nunca alto.** Volume alto é a reclamação mais repetida sobre mods de terror.

### 4c. Silêncio de verdade

**O que você vai sentir:** um segundo antes do susto, a música e o ambiente somem. Os animais param de fazer barulho.

- **Como funciona:** o pacote `efeito` manda o cliente baixar música e ambiente por 1 a 3 s no instante do avistamento. Reserva só pelo servidor: `ClientboundStopSoundPacket`.
- **Animais mudos:** `Entity.setSilent(true)` nos animais próximos por alguns segundos, com lista para restaurar no tempo certo e ao fechar o mundo (o mesmo cuidado de `AlteracoesTemporarias`).
- **Regra de design:** o pico não tem acorde de susto. O contraste é o silêncio.

### 4d. Fundo ligado à pressão

- Um som grave em loop, no limite do audível, com volume guiado por pressão e obsessão. Um coração só quando a caça está perto e fora da vista. Usa `AbstractTickableSoundInstance` no cliente.

### 4e. Abafado e fora do tom

- Versões já filtradas dos sons para "atrás da parede", escolhidas pela quantidade de blocos sólidos entre a fonte e o jogador.
- De vez em quando, um som conhecido do jogo (um baú abrindo) tocado perto e grave demais.

### 4f. O aviso que não é confiável

**O que você vai sentir:** um som distante que você aprende a temer. Na maioria das vezes não vem nada depois.

- **Como funciona:** um som-assinatura, longe, tocado antes das aparições e dos sustos grandes. O mesmo som toca sozinho, sem nada depois, em mais da metade das vezes.
- **Por quê:** aviso que sempre acerta "perde os dentes com veteranos"; aviso nenhum é lido como injusto. O aviso que mente fica no meio.
- **O que já existe:** o anúncio de dois passos (`Diretor.anunciar`) e os falsos positivos (`SINAL`). Esta parte junta os dois num som só.

### 4g. Chat de voz por proximidade

**O que você vai sentir:** quem se afasta do grupo deixa de ouvir os amigos de verdade.

- **Como funciona:** não é código do Sussurros. É instalar o Simple Voice Chat (existe para Fabric 26.2) e combinar de usar só a voz do jogo.
- **Por quê:** a regra "separação é o gatilho, reunião é o alívio" (7a) vira física.
- **Voz roubada (depois do primeiro marco):** o Hóspede repetir, do escuro, uma frase que um amigo disse minutos antes. A API do Simple Voice Chat tem as peças: ouvir o microfone no servidor e tocar áudio a partir de uma entidade. Para estudar, sem copiar: Revervox (Forge e NeoForge 1.20.1 e 1.21.1, GPL-3.0) e Mimicked. Precisa de uma pesquisa própria antes de começar.
- **Combinado com os amigos:** se o mod for guardar trechos de voz no servidor, eles são avisados antes. Não precisa dizer para quê.

---

## Etapa 5 — Miragens: o mundo que só você vê

**O que você vai sentir:** uma tocha acesa no fim do túnel que não está lá quando você chega. A porta da sua casa aberta, e o amigo ao lado jurando que está fechada.

### 5a. Sistema de miragem

- **Como funciona:** classe nova `Miragem`. Envia `ClientboundBlockUpdatePacket(pos, estadoFalso)` para um jogador e guarda a lista. Desfaz reenviando o estado real quando o jogador chega perto, depois de um tempo ou quando ele sai do mundo.
- **O mundo real não muda.** Não precisa de restauração ao fechar o servidor.
- **Experimento antes:** `/sussurros teste miragem`. Conferir se a tocha falsa ilumina no cliente e o que acontece quando o jogador clica nela.
- **A miragem se desfaz sozinha:** o jogo reenvia o estado real quando o jogador clica no bloco ou quando o chunk recarrega. Faz parte do efeito (tocar na coisa faz ela sumir), mas nenhuma mecânica pode depender de a miragem durar.
- **Migração:** `LUZ_ERRADA`, `TOCHA` (piscar), `OBJETO_FORA_LUGAR` e as tochas apagadas ao acordar passam a ser miragens. Isso resolve o item 3.10 da análise sem perder o efeito.

### 5b. Miragens novas

- Tocha no fim do túnel que some quando você chega a 6 blocos.
- Porta que parece aberta (o som toca só para você).
- Um bloco faltando na parede da casa, por alguns segundos.
- Uma tocha sua que virou tocha de redstone.
- Todas as portas por perto parecendo abertas ao mesmo tempo.
- O baú que você abriu fecha sozinho (isto é real, mas não estraga nada).

### 5c. Coisas escritas

- **Livro:** num baú seu ou ao lado da cama, um livro escrito a partir do `Perfil` e da `Memoria` ("dorme sempre no mesmo lugar", "olha para trás quando ouve passos"). Casa com a página 17 do diário.
- **Placa:** longe da base, uma placa com uma frase que você escreveu no chat, com uma palavra trocada.

---

## Etapa 6 — Sustos grandes, raros

**O que você vai sentir:** na maior parte das sessões, nada grande. Numa sessão longa, uma coisa que você vai contar depois.

### 6a. Baralho de picos

- **Como funciona:** um baralho por mundo, embaralhado, com cartas de susto e cartas "nada". Uma carta é virada a cada 60 a 120 minutos de jogo. Cada carta tem condições (lugar, hora, fase); se não couber, espera. Uma carta só volta depois que o baralho acabar.
- **De onde vem:** o The Obsessed tira os modos de um saco embaralhado para que todos apareçam sem ordem previsível, e o The Broken Script tem um evento que não faz nada, de propósito.
- **Sem morte.** Nenhuma carta mata. Quase morrer (ficar com um ou dois corações) é o que os jogadores relatam como marcante; morrer e renascer acaba com o medo.
- **Sempre** com preparação antes e silêncio longo depois, como as cenas de hoje.
- **No log:** `BARALHO carta=... estado=VIRADA|ESPERANDO|EXECUTADA`.

### 6b. Cartas iniciais

- **Ao pé da cama:** você deita, o sono é negado, e ele está a dois blocos do pé da cama. Some quando você levanta.
- **Golpe por não olhar:** ele aparece perto e atrás. Se você não o notar em alguns segundos, um toque de 1 de dano, um baque e ele some. Ensina a vigiar as costas.
- **Intruso:** na volta de uma viagem longa, várias mudanças pequenas de uma vez: portas abertas, miragens, um item mudado de um baú para outro. Nada é destruído.
- **Ontem:** ao longe, uma figura refaz o seu caminho de minutos atrás, com as suas paradas. Usa o `Rastro`.
- **Salto de aproximação:** na caça, quando ele encurta a distância fora da sua vista, um aperto de câmera e um batimento. Com recarga longa.
- **O esconderijo não segura:** você se tranca e ele encontra um jeito de olhar para dentro. É o momento mais citado do Cave Dweller. Usa miragem (um bloco "faltando") em vez de quebrar a parede.
- **Neblina que fecha:** a distância de visão encolhe por alguns minutos e um vulto distante fica no limite dela. Precisa do cliente.

---

## Etapa 7 — Amigos

**O que você vai sentir:** você ouviu; ele não. Ele viu; você não. Ninguém tem certeza de nada.

A ideia central vem de Lethal Company e Phasmophobia: entre amigos, o alvo não é o jogador, é a confiança entre eles. As fontes e as quinze ideias estão na seção 15 do `PESQUISA-E-ANALISE.md`.

### 7a. Quem é assombrado

- **Realidades divergentes:** já vem das etapas 3d, 4a e 5a. Fica como padrão.
- **Se um amigo encosta, some:** a aparição de um jogador desaparece se outro passar por cima dela. Ninguém consegue confirmar o relato.
- **Marcado da noite:** a cada noite o Diretor fixa um alvo, que recebe a maior parte do que acontece. Os outros quase nada.
- **Separação é o gatilho, reunião é o alívio:** os eventos fortes só acontecem com o jogador longe dos outros. A menos de 16 blocos de um amigo, a pressão cai. Hoje estar sozinho só soma vulnerabilidade.
- **Pressão pela média do grupo:** o amigo calmo passa a ser assombrado porque o outro entrou em pânico.

### 7b. Mentir com a interface e com os amigos

- **"Fulano saiu do jogo":** a linha amarela de saída, enviada só para um jogador. Ou a entrada de alguém que não está online. A versão simples é só a linha, e funciona melhor quando o amigo está longe da vista. Ele continua na lista de jogadores (Tab); tirá-lo de lá sem quebrar o desenho dele precisa de experimento.
- **Eco de amigo:** uma frase antiga de um jogador reaparece só para outro. Faz duvidar do amigo, não do jogo.
- **Voz de amigo:** se eles gravarem, o sussurro pode vir na voz de quem não está por perto.
- **Batida só com um em casa:** quando os outros voltam, não há nada.

### 7c. O sósia

**O que você vai sentir:** seu amigo parado na linha das árvores, de costas. Ele está do outro lado do mapa.

- **Como funciona:** uma figura com a pele de um amigo que está longe. Fica parada ou de costas, encara de 2 a 5 s quando é notada e some ao ser abordada.
- **Caminho mais curto:** a entidade `Mannequin` do próprio jogo, que mostra a pele de um jogador. Conferido no código da 26.2:
  - o perfil entra pelos dados salvos (campo `profile`). O método que o define é privado, então o caminho é criar a entidade a partir desses dados;
  - por padrão aparece um rótulo embaixo do nome. O campo `hide_description` tem de ir ligado, senão o susto vira piada;
  - as poses aceitas são em pé, agachado, nadando, planando e deitado. Não há "inclinar a cabeça";
  - para um amigo online, o servidor já tem o perfil com a pele.
- **Caminho alternativo:** o do mod Existence (código aberto): um mob comum cujo renderizador usa o modelo de jogador e a textura do amigo, e não desenha nada para quem não é o alvo.
- **Experimento antes:** `/sussurros teste sosia <jogador>`. Conferir a pele, o rótulo escondido e as poses.

### 7d. Carta grande de grupo

- **Isolamento:** por um a três minutos, um jogador fica invisível e mudo para os outros. O grupo acha que ele saiu; ele acha que foi ignorado. Entra no baralho da etapa 6. São três coisas a esconder: o corpo, o chat e a voz. A voz sai pela API do Simple Voice Chat; o corpo é a parte difícil.

---

## Etapa 8 — Surpresa para o autor

- **Baralho da sessão:** ao criar o mundo, o mod sorteia quais mecânicas das etapas 3 a 7 estão ligadas e em que dia cada uma destrava. Fica salvo no mundo. Nenhum comando mostra, a não ser `/sussurros spoiler`.
- **Cabe em duas semanas:** servidores de amigos costumam durar umas duas semanas. A escalada inteira tem de caber em cerca de dez a quinze sessões, com alguma coisa nova destravando em quase todas.
- **Um jogador desavisado vale ouro:** o relato mais forte da pesquisa é de alguém que não sabia que o mod estava instalado. Para os amigos, não contar o que o mod faz.
- **Variantes escondidas:** quem programa pode criar variações sem descrevê-las, registrando-as num arquivo de spoilers que o dono escolhe não abrir. A explicação depois de cada entrega diz o que testar, não o que acontece.
- **Quem dá para surpreender de verdade são os amigos.** O dono já leu o catálogo inteiro; o sorteio ajuda com o "quando" e o "qual", não com o "o quê". Proteger os amigos dos spoilers (repositório privado, não contar) vale mais do que esconder coisas do dono.

---

## Fica de fora por enquanto

- **Cópia torta da casa:** é a ideia mais fiel ao tema e a mais cara. Só depois da etapa 6.
- **Falsa desconexão, erro falso, título da janela, brilho resetado:** são a maior fonte de reclamação em outros mods. Se entrarem, entram como cartas do baralho, desligadas por padrão.
- **GeckoLib e SmartBrainLib:** continuam valendo as regras do `ARQUITETURA.md`. O GeckoLib existe para Fabric 26.2 e fica reservado para o caso de a silhueta (3g) não bastar.
- **Efeitos de tela com biblioteca:** a Veil só existe para a 1.21.1. O que o próprio jogo permite na 26.2 ainda não foi verificado.
- **Registro de blocos colocados pelo jogador:** o Fabric não tem um evento pronto para isso; falta investigar. As miragens dispensam esse registro na maior parte dos casos.

---

## Para jogar e comparar

Jogar outros mods é a melhor medida, porque o público deste mod é o próprio dono. Em cada um, anotar três momentos que pegaram e três que irritaram.

- **The Hollow** (https://modrinth.com/mod/hollow-dread): roda em Fabric 26.2, a mesma versão do Sussurros. É o mais fácil de instalar ao lado.
- **From The Fog:** o mais elogiado por quem joga há muito tempo. Reparar na distância e na duração dos avistamentos.
- **The Broken Script:** quarta parede. Reparar no que incomoda de verdade e no que só irrita.
- **TheWatcher:** conceito quase igual ao do Sussurros. O código está no GitHub e ainda não foi lido; vale uma leitura antes da etapa 3.

---

## Decisões que são do dono

1. **Modo surpresa.** Quer que as variantes dos sustos grandes sejam escondidas de você? Recomendação: sim nas etapas 6 a 8, não nas fundações.
2. **Tochas.** As que hoje somem de verdade podem virar miragem (só você vê apagar)? Recomendação: sim.
3. **Voz.** Topa gravar as frases? E os amigos? Recomendação: sim; é o que mais separa este mod dos outros.
4. **Entre amigos.** Cada um vê só o seu Hóspede? Recomendação: sim.
5. **Quarta parede.** A falsa saída de amigo entra? Recomendação: sim. Falsa desconexão: não por enquanto.
6. **Repositório privado.** Hoje ele é público. Recomendação: tornar privado antes de chamar os amigos, conferindo antes se o ChatGPT continua com acesso.
7. **Voz do jogo.** Usar o Simple Voice Chat com os amigos desde a primeira noite? Recomendação: sim. Gravar a voz deles fica para depois, e só com aviso.

---

## Pesquisa daqui para a frente

Nenhuma pesquisa nova antes da etapa 1. Depois, uma por tema, só quando a etapa correspondente estiver para começar, e cada uma termina com uma recomendação curta: o que fazer primeiro e o que não fazer.

| Pesquisa | Antes de |
|---|---|
| Ler o código do TheWatcher | etapa 3 |
| API do Simple Voice Chat na 26.2; Revervox e Mimicked | voz roubada (4g) |
| Efeitos de tela do próprio jogo na 26.2 | cartas "neblina que fecha" e "salto de aproximação" |
| Blockbench e GeckoLib | só se a silhueta não bastar |

O critério de corte é "isso assusta o dono e os amigos?", não "isso é legal".

---

## Relação com o `ROADMAP.md`

| Este plano | No roadmap |
|---|---|
| Etapa 1 | Correções da 0.8.x |
| Etapa 2 | Parte da 0.8.3 (sensor de linha de visão) e da 0.9 (entrada e saída das aparições) |
| Etapas 3 e 6 | 0.9 (aparições cinemáticas) |
| Etapa 4 | Não está no roadmap |
| Etapa 5 | 0.8.2 (alterações reversíveis) e 0.11 (mundo reativo) |
| Etapa 7 | Não está no roadmap |
| Etapa 8 | Não está no roadmap; atende a "poder decidir não fazer nada" da 0.8.2 |
