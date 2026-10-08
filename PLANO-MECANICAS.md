# Sussurros — plano das novas mecânicas (SPOILERS)

Plano escrito em 08/10/2026 a partir do `PESQUISA-E-ANALISE.md`. Ele diz o que construir, em que ordem e como testar cada parte. É uma proposta: o `ROADMAP.md` não foi alterado.

**Objetivo:** o mod é para assustar o dono e, talvez, alguns amigos. Não vai ser publicado. Então a régua é uma só: o susto funciona em quem joga, inclusive depois de muitas horas e inclusive em quem escreveu o mod.

---

## Os seis princípios deste plano

1. **Perceptível antes de inteligente.** Se o jogador não ouviu nem viu, não aconteceu. O Diretor só aprende com o que foi confirmado como perceptível.
2. **Mexer na percepção, não no mundo.** Sempre que der, a mudança existe só para um jogador (som, bloco, criatura). A construção de ninguém é estragada, e entre amigos cada um vive uma coisa diferente.
3. **Raridade em camadas.** Um fundo constante quase inaudível; coisas sutis de vez em quando; coisas médias raramente; um susto grande uma vez por sessão longa, ou nenhuma.
4. **Menos eventos, todos sentidos.** Hoje acontece algo a cada ~65 s e a maioria passa despercebida. A meta é o contrário.
5. **Regra aprendida é regra que pode ser quebrada.** Se "encarar faz sumir" sempre funciona, deixa de assustar. De vez em quando não funciona.
6. **Nem o autor sabe.** Cada mundo sorteia quais mecânicas estão ligadas e quando. Sem isso, quem leu o plano não se assusta.

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

## Como vamos trabalhar

1. Cada parte vira uma branch e um PR para a `main`.
2. Quem programa compila e roda os testes antes de cada commit.
3. O dono mescla o PR, joga 30 a 40 minutos com `/sussurros debug on` e manda o arquivo de log.
4. O log é lido e os números são ajustados. Foi um log assim que revelou os onze problemas da análise.

Quem programa não joga. Build e testes verdes não provam que algo funciona dentro do jogo, então cada etapa abaixo diz o que conferir.

Técnica que nunca foi testada na 26.2 ganha primeiro um comando de experimento (`/sussurros teste ...`). Só depois que ele funcionar em jogo é que uma mecânica é construída em cima.

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

**Opcional nesta etapa:** tirar os números de ajuste do código e pôr num arquivo de configuração, com `/sussurros recarregar`. Encurta cada rodada de ajuste, porque dispensa recompilar.

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

---

## Etapa 4 — Som que perturba

### 4a. Som privado

- **O que muda:** `ModSons` ganha `tocarPara(jogador, ...)`, que envia `ClientboundSoundPacket` só para ele. Os sons da assombração passam a usá-lo. Sons de coisas que mudaram de verdade no mundo (uma porta que abriu) continuam públicos.

### 4b. Sussurro com voz

**O que você vai sentir:** uma voz baixa, sem direção, que diz o seu nome. Às vezes vem de trás de uma parede.

- **Produção:** o dono grava de 10 a 15 frases curtas sussurradas (celular serve). Se os amigos toparem, gravam também. A receita de tratamento (tom, eco invertido, corte de agudos) entra neste arquivo depois da segunda rodada de pesquisa.
- **O que muda:** `Diretor.sussurro` toca o áudio; o texto em cima da hotbar vira legenda opcional. Duas formas: "na cabeça" (sem posição) e posicional abafado.
- **Experimento antes:** conferir em jogo se um `.ogg` estéreo toca sem posição ou se é preciso tocar pelo cliente com `SimpleSoundInstance.forUI`.

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

---

## Etapa 5 — Miragens: o mundo que só você vê

**O que você vai sentir:** uma tocha acesa no fim do túnel que não está lá quando você chega. A porta da sua casa aberta, e o amigo ao lado jurando que está fechada.

### 5a. Sistema de miragem

- **Como funciona:** classe nova `Miragem`. Envia `ClientboundBlockUpdatePacket(pos, estadoFalso)` para um jogador e guarda a lista. Desfaz reenviando o estado real quando o jogador chega perto, depois de um tempo ou quando ele sai do mundo.
- **O mundo real não muda.** Não precisa de restauração ao fechar o servidor.
- **Experimento antes:** `/sussurros teste miragem`. Conferir se a tocha falsa ilumina no cliente e o que acontece quando o jogador clica nela.
- **Migração:** `LUZ_ERRADA`, `TOCHA` (piscar), `OBJETO_FORA_LUGAR` e as tochas apagadas ao acordar passam a ser miragens. Isso resolve o item 3.10 da análise sem perder o efeito.

### 5b. Miragens novas

- Tocha no fim do túnel que some quando você chega a 6 blocos.
- Porta que parece aberta (o som toca só para você).
- Um bloco faltando na parede da casa, por alguns segundos.
- Uma tocha sua que virou tocha de redstone.

### 5c. Coisas escritas

- **Livro:** num baú seu ou ao lado da cama, um livro escrito a partir do `Perfil` e da `Memoria` ("dorme sempre no mesmo lugar", "olha para trás quando ouve passos"). Casa com a página 17 do diário.
- **Placa:** longe da base, uma placa com uma frase que você escreveu no chat, com uma palavra trocada.

---

## Etapa 6 — Sustos grandes, raros

**O que você vai sentir:** na maior parte das sessões, nada grande. Numa sessão longa, uma coisa que você vai contar depois.

### 6a. Baralho de picos

- **Como funciona:** um baralho por mundo, embaralhado, com cartas de susto e cartas "nada". Uma carta é virada a cada 60 a 120 minutos de jogo. Cada carta tem condições (lugar, hora, fase); se não couber, espera. Uma carta só volta depois que o baralho acabar.
- **Sempre** com preparação antes e silêncio longo depois, como as cenas de hoje.
- **No log:** `BARALHO carta=... estado=VIRADA|ESPERANDO|EXECUTADA`.

### 6b. Cartas iniciais

- **Ao pé da cama:** você deita, o sono é negado, e ele está a dois blocos do pé da cama. Some quando você levanta.
- **Golpe por não olhar:** ele aparece perto e atrás. Se você não o notar em alguns segundos, um toque de 1 de dano, um baque e ele some. Ensina a vigiar as costas.
- **Intruso:** na volta de uma viagem longa, várias mudanças pequenas de uma vez: portas abertas, miragens, um item mudado de um baú para outro. Nada é destruído.
- **Ontem:** ao longe, uma figura refaz o seu caminho de minutos atrás, com as suas paradas. Usa o `Rastro`.
- **Salto de aproximação:** na caça, quando ele encurta a distância fora da sua vista, um aperto de câmera e um batimento. Com recarga longa.

---

## Etapa 7 — Amigos

**O que você vai sentir:** você ouviu; ele não. Ele viu; você não. Ninguém tem certeza de nada.

- **Realidades divergentes:** já vem das etapas 3d, 4a e 5a. Fica como padrão.
- **Alvo isolado:** os eventos preferem quem se afastou do grupo. Hoje estar sozinho só soma vulnerabilidade.
- **"Fulano saiu do jogo":** a linha amarela de saída, enviada só para um jogador, com o amigo ainda lá. Ou a entrada de alguém que não está online.
- **Eco de amigo:** o eco de chat passa a usar frases dos amigos também.
- **Voz de amigo:** se eles gravarem, o sussurro pode vir na voz de quem não está por perto.
- **Figura com o nome do amigo:** ao longe, à noite, alguém com o nome dele sobre a cabeça. Some ao ser encarada. Versão simples primeiro (silhueta e nome); pele de jogador de verdade fica para depois.

---

## Etapa 8 — Surpresa para o autor

- **Baralho da sessão:** ao criar o mundo, o mod sorteia quais mecânicas das etapas 3 a 7 estão ligadas e em que dia cada uma destrava. Fica salvo no mundo. Nenhum comando mostra, a não ser `/sussurros spoiler`.
- **Variantes escondidas:** quem programa pode criar variações sem descrevê-las, registrando-as num arquivo de spoilers que o dono escolhe não abrir. A explicação depois de cada entrega diz o que testar, não o que acontece.

---

## Fica de fora por enquanto

- **Cópia torta da casa:** é a ideia mais fiel ao tema e a mais cara. Só depois da etapa 6.
- **Falsa desconexão, erro falso, título da janela, brilho resetado:** são a maior fonte de reclamação em outros mods. Se entrarem, entram como cartas do baralho, desligadas por padrão.
- **GeckoLib e SmartBrainLib:** continuam valendo as regras do `ARQUITETURA.md`. Nada neste plano precisa delas.
- **Registro de blocos colocados pelo jogador:** o Fabric não tem um evento pronto para isso; falta investigar. As miragens dispensam esse registro na maior parte dos casos.

---

## Decisões que são do dono

1. **Modo surpresa.** Quer que as variantes dos sustos grandes sejam escondidas de você? Recomendação: sim nas etapas 6 a 8, não nas fundações.
2. **Tochas.** As que hoje somem de verdade podem virar miragem (só você vê apagar)? Recomendação: sim.
3. **Voz.** Topa gravar as frases? E os amigos? Recomendação: sim; é o que mais separa este mod dos outros.
4. **Entre amigos.** Cada um vê só o seu Hóspede? Recomendação: sim.
5. **Quarta parede.** A falsa saída de amigo entra? Recomendação: sim. Falsa desconexão: não por enquanto.

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
