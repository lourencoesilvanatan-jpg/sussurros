# Sussurros — plano da expansão 0.9 "O Avesso" (SPOILERS)

**Dono do projeto: não abra este arquivo.** Em 08/10/2026 ele pediu para não saber mais nada do que o mod faz ("quero ser surpreendido"). Este arquivo existe para quem programa continuar o trabalho de uma conversa para a outra.

Pedido que originou a expansão: mais itens, com custo; mais atmosfera e interação com o ambiente (miragens ou coisas físicas); uma progressão "mais sensitiva ou visual"; uma caçada "inteligente e apelona"; sons e trilha; coisas grandes, como estruturas com interação e uma dimensão. Condição: "não é sair adicionando 500 mil coisas, é expandir com síntese; tem que encaixar e funcionar".

Vale tudo o que está no `PLANO-MECANICAS.md` (os sete princípios) e no `ROADMAP.md`. Este plano acrescenta três pilares e amarra cada peça nova em pelo menos dois sistemas que já existem.

---

## Regras de trabalho desta fase

1. **Sem spoiler para o dono.** PR, commit, `ROTEIRO-DE-TESTE.md` e respostas a ele dizem o que foi mexido em termos gerais e o que ele precisa fazer. Os detalhes ficam aqui, no `DESIGN-SPOILERS.md`, no `TELEMETRIA.md` e nos comentários do código.
2. **Testar o máximo sem ele.** Três níveis: JUnit (matemática), `./gradlew runGameTest` (servidor de verdade, sem janela, com jogador de mentira) e `runClientGameTest` (o jogo aberto numa tela virtual do GitHub, que devolve fotos). As fotos baixam com `gh run download <id> --name fotos-do-jogo`. Nunca rodar o teste de cliente na máquina dele.
3. **Nunca alto, nunca matar, nunca estragar a construção.** Valem para tudo o que entrar.
4. **Cada item novo tem quatro partes:** um benefício real, um sinal sensorial imediato, um custo que o Diretor guarda e um jeito de o Hóspede virar o item contra o jogador depois de aprender.
5. **O estado de volta à `main`** antes da expansão está na tag `antes-da-expansao`.

---

## Pilar I — Sentir

O cérebro do mod sempre foi forte e o que chegava ao jogador sempre foi fraco. Aqui o que estava escondido (fase, obsessão, pressão, criatura olhando) passa a ser percebido sem HUD. É a "progressão sensitiva" que o dono pediu.

### I.1 Canal servidor → cliente

Pacote `sussurros:sentidos`, enviado uma vez por segundo e quando algo muda:

| campo | faixa | de onde vem | o que o cliente faz |
|---|---|---|---|
| `peso` | 0–1 | fase e obsessão, lento | drena a cor do mundo em degraus e sobe o fundo grave |
| `vigia` | 0–1 | há criatura que vê o jogador sem ser vista (às vezes mente) | vinheta leve nas bordas e a camada `fundo_vigia` |
| `caca` | 0–1 | caçada em curso; cresce com a proximidade fora da vista | batimento e as três camadas de perseguição |
| `neblina` | 0–1 | eventos, caçada, Avesso | aproxima a neblina |
| `flags` | bits | — | `AVESSO` (ambiente próprio), `ECO_PASSO` (janela em que os próprios passos ecoam) |

O cliente interpola (cada valor anda um pouco por tick), então 1 Hz parece contínuo.

Pacote `sussurros:efeito` (servidor → cliente), pontual: `PISCAR` (preto por poucos ticks), `APAGAO` (fecha em preto e segura), `ACORDAR` (abre do preto), `TREMOR`.

Pacote `sussurros:campo` (cliente → servidor): o FOV efetivo e a proporção da janela. O servidor troca os cones fixos (`CONE_TELA_SEGURA`, `CONE_PERCEBEU`) por cones calculados para aquele jogador. Sem pacote (ou com mais de 5 s), vale o cone antigo. Resolve o item 3.9 da análise.

### I.2 Efeitos no cliente

- **Cor que drena:** cinco degraus de saturação (1,00 → 0,62), por `post_effect` próprio. Cada degrau é pequeno demais para ser visto na troca; o jogador só nota comparando com a lembrança. Volta um degrau dentro da zona da vela e de dia em casa.
- **Vinheta de vigia:** escurecimento nas bordas, com "respiração" lenta. Alfa máximo baixo.
- **Piscar forçado:** a tela fecha por 3 a 5 ticks. Usado na caçada (ver III.1).
- **Apagão / acordar:** transição para ser levado e para voltar.
- **Neblina que fecha:** ambiente de neblina próprio, inserido na lista do `FogRenderer`.
- **Sumiço com fade** da criatura, em vez de piscar para fora.
- **Passo em eco:** em janelas raras, o som do próprio passo do jogador toca de novo uma fração de segundo depois, mais baixo e um pouco atrás.
- **Trilha em camadas:** `fundo_grave`, `fundo_vigia`, `caca_pulso`, `caca_cordas`, `caca_tema`, `avesso_ar`. O volume de cada uma segue os campos do pacote.

Com shader pack (Iris) o efeito de cor pode não aparecer; o resto continua.

### I.3 O tema

A cantiga da Caixa de Música (lá menor, valsa, oito compassos; notas no `ferramentas/sons/LEIA-ME.md`) é o tema do mod. Aparece limpa na caixa, gasta conforme a caixa é usada, assobiada e cantarolada no escuro, e em fragmentos graves na perseguição. O assobio é o **aviso que mente** (etapa 4f do plano antigo): toca antes de uma caçada, e toca sozinho, sem nada depois, em mais da metade das vezes.

---

## Pilar II — Trocar

Toda ferramenta é um trato. O jogador ganha algo e entrega algo, e o Hóspede aprende.

| Item | Benefício | Sinal | Custo | Como ele vira o jogo |
|---|---|---|---|---|
| **Lampião Pálido** (bloco) | Luz que reage: a chama fica inquieta, fria e apaga conforme ele chega perto. Perímetro que avisa | A cor e o tamanho da chama | Queima Cinza Pálida. Cada lampião aceso perto do jogador alimenta a obsessão | Chama fria sem ninguém (falso). Evento em que apagam um a um em direção à casa. Na caçada ele os apaga |
| **Caixa de Música** | Enquanto toca, ele vai até a caixa e fica ouvindo. Quebra a perseguição e serve de isca | A própria música | Corda limitada. Cada uso gasta o som (mais lento, desafinado) | Depois de três usos ele "aprendeu": a cantiga volta assobiada do escuro. Depois de cinco, às vezes ele ignora a caixa |
| **Tigela de Oferenda** (bloco) | Ele aceita o que está nela quando ninguém olha e dá trégua (obsessão cai, tempo sem eventos) | A tigela aparece com cinza no lugar da oferenda | Vira costume: sem oferenda, a casa fica mais agitada | Ele passa a deixar coisas na tigela. É por ela que devolve (ou cobra) o que tomou do jogador |
| **Giz de Cinza** | Marcar caminho em caverna | O risco na parede | Pouca durabilidade | Marcas que o jogador não fez (miragem) num túnel errado; marcas reais somem por instantes. Em casa, riscos de contagem aparecem perto da cama, um por noite: a casa conta as noites |
| **Espelho Embaçado** | Ver se há algo atrás sem virar. Desfaz miragens próximas | O vidro mostra um vulto, ou nada | Cada olhada é um "você olhou". Racha com o uso | Passa a mostrar vulto sem ninguém. Se quebrar, uma consequência marcada |
| **Infusão de Olho-flor** | Sono garantido, sem ser acordado | — | O sono é um sonho: o jogador acorda no Avesso | Cada infusão o deixa mais à vontade lá |

Itens antigos que ganham corpo: a **Vela Pálida** passa a pôr uma vela de verdade no chão enquanto dura (e na fase 4 ele a sopra antes do tempo); a **Cinza Pálida** passa a aparecer espalhada no chão de casa (bloco fino, coletável) como pegada física.

**O que ele toma.** Quando pega o jogador, ele leva o item da mão. O item não some: fica guardado (`Guardados`, anexo do jogador). Volta pela Tigela, em troca de oferenda, ou é buscado no Ninho, no Avesso.

---

## Pilar III — Ser levado

### III.1 Caçada 2.0

Hoje: anda quando não é visto, congela quando é visto, procura pela última posição, 5 de dano e some. Passa a ser em estágios:

1. **Aviso.** O assobio, longe e atrás. O fundo sobe. Lampiões reagem.
2. **Perseguição.** Anda quando não é visto. Abre portas. Sobe paredes quando não é visto (pilar e muro não seguram). Luz forte o deixa mais lento, por isso ele apaga as luzes por onde passa (miragem para tochas, de verdade para lampiões).
3. **Encarar não basta.** Encarar o congela, mas a cada 5–8 s o jogador é forçado a piscar, e no piscar ele avança. Depois de três piscadas na mesma caçada, ele anda devagar mesmo sendo visto.
4. **Cortar caminho.** Se o jogador corre em linha reta, ele some e reaparece à frente da rota prevista, de lado. No máximo duas vezes por caçada.
5. **O esconderijo não segura.** Jogador fechado num buraco: arranhões na parede, um bloco "falta" (miragem) e ele está olhando para dentro.
6. **Fingir que desistiu.** Ao perder o rastro, às vezes os passos se afastam, o fundo cai, e ele volta de outro lado meio minuto depois.
7. **Contra-jogo.** Quebrar a linha de visão e ficar quieto; a vela (ele espera; na fase 4, sopra); a Caixa de Música; luz.
8. **Ser pego não mata.** Apagão. Ele leva o item da mão. Depois, uma de duas: o jogador é **levado ao Avesso** (sempre na primeira vez; depois, em parte das vezes), ou acorda deslocado, num ponto antigo do próprio Rastro, com poucos corações e a noite mais adiantada.

### III.2 O Avesso

Dimensão `sussurros:avesso`. Usa a mesma geração de terreno do mundo normal com um bioma único e morto: os mesmos morros, rios e cavernas, nas mesmas coordenadas, sem árvores, sem bichos, com grama cinza, água preta, neblina perto e um céu parado. É o mundo como era antes do jogador.

- **A cópia torta.** Na chegada, a região em volta da âncora (a cama, se estiver perto; senão o ponto da captura) é copiada bloco a bloco para as mesmas coordenadas, sem as luzes, com as portas abertas, os baús vazios e alguns blocos faltando. Nas visitas seguintes muda mais: espelhada, girada. Conteúdo de baú nunca é copiado.
- **Entrar:** ser pego; dormir depois da Infusão; raramente, dormir na fase 3+; atravessar uma Soleira; e uma carta rara em que a porta de sempre abre para lá.
- **Lá dentro:** primeiro, nada. Depois sinais: um vulto no limite da neblina. Depois ele vem, devagar, e lá encarar não o segura.
- **Sair:** dormir na cópia da própria cama. Sem cama por perto, uma Soleira acesa a algumas dezenas de blocos. A direção é dada pelo som: a cantiga toca baixinho de onde fica a saída.
- **Nunca prende:** a visita tem tempo máximo; dano letal lá vira "acordar".
- **O Ninho:** onde ele guarda o que tomou. Os itens do jogador estão lá de verdade. No meio, uma cabeça com o rosto do jogador.

O controle do Avesso é um diretor próprio e simples (`AvessoDiretor`); o Diretor normal só cuida do mundo normal.

### III.3 Lugares no mundo normal

- **Casa do Vigia:** a casa de quem escreveu o diário. Uma por jogador, longe da base, colocada fora da tela. Tem a cama dele, riscos de contagem pelas paredes, uma tigela, um lampião apagado e um baú (Caixa de Música, páginas, giz). Muda um pouco entre uma visita e outra.
- **Soleira:** uma porta sozinha na paisagem.
- **O Boneco:** uma figura de palha que aparece longe da base e, a cada noite, está mais perto e virada para a cama. Progressão que se vê. Destruir não resolve.

### III.4 O baralho

Um baralho por jogador, embaralhado com a semente do mundo, com cartas grandes e cartas "nada". Uma carta a cada 40–90 minutos de assombração, a partir da fase 2. Nem quem leu o código sabe a ordem naquele mundo.

---

## Ordem de construção

| PR | Conteúdo | Como é testado sem o dono |
|---|---|---|
| base | testes dentro do jogo e fotos no GitHub (#18, feito) | — |
| sons | biblioteca sintetizada e trilha | medição de pico, RMS, emenda dos loops |
| sentidos | pacotes, efeitos de tela, trilha em camadas | fotos de cada efeito no máximo; servidor de teste envia o pacote |
| itens | os seis itens, a cinza no chão, a vela física, receitas, saque | servidor: usar cada item com jogador de mentira; fotos dos blocos e ícones |
| caçada | os oito estágios | servidor: caçada forçada contra jogador parado, fechado, em pilar |
| avesso | dimensão, cópia, saída, Ninho | servidor: ir e voltar; fotos do lugar |
| lugares | Casa do Vigia, Soleira, Boneco, baralho | servidor: gerar cada um; fotos |

Cada PR atualiza o `ROTEIRO-DE-TESTE.md` (sem spoiler), o `DESIGN-SPOILERS.md` e o `TELEMETRIA.md`.

---

## Pesquisa desta fase

Três levantamentos (itens com custo e progressão sensorial; IA de perseguidor; lugares, dimensão e espaços "iguais mas errados") foram pedidos em 08/10/2026. Os relatórios entram no `PESQUISA-E-ANALISE.md` como seções 19 a 21, e o que mudar neste plano por causa deles fica anotado aqui embaixo.

### Ajustes vindos da pesquisa

(a preencher quando os relatórios chegarem)
