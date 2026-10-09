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

## Pesquisa desta fase e o que ela mudou (versão 2 do plano)

Os três relatórios estão em `pesquisa/` (caçada, lugares e dimensão, itens e sentidos), com fontes e marcas de confiança. **Onde esta seção discorda do que está acima, vale esta seção.**

### Itens (substitui a tabela do Pilar II)

A lista final é menor e gira em torno de um sistema só.

**A Conta.** Valor escondido por jogador. Cada uso de item ligado à criatura soma (Vela 1, Sino 1, Fio 1, Isca 2, Olho 3, Caixa 2, Ossos 1, Linha 1 por bloco). Cai 1 a cada 10 minutos sem usar nada. O limite é sorteado a cada ciclo (6 a 10). Antes de cobrar, três avisos sem texto: a Cinza Pálida no inventário escurece; as chamas perto do jogador encolhem por alguns segundos; a cantiga toca com uma nota a menos. A cobrança vem na moeda do item mais usado e nunca mata. Depois zera e sorteia outro limite. É o que deixa os itens serem bons de verdade.

| Item | Resumo |
|---|---|
| **Caixa de Música** | Toca o tema. A inquietação cai; se ele estiver a até 20 blocos, algo cantarola junto, sem direção (confirma presença, não posição). Na caçada serve de isca, cada vez menos. Pelo total de usos: mais lenta, depois com nota faltando, depois **o tema volta sem a caixa**, assobiado de longe. O assobio só existe depois que ele aprendeu, e é honesto nas duas primeiras vezes. A caixa é entregue ao jogador na passagem para a fase 2 e também está na Casa do Vigia |
| **Linha de Cinza** | Cinza Pálida usada no chão vira um bloco fino. Ele não cruza uma linha intacta; cada tentativa a desgasta (três estágios) e na manhã seguinte ela está riscada: prova de que ele esteve ali. Rompida, deixa de valer. O mesmo bloco, em outro estado, é a pegada de cinza que ele deixa. Só a linha intacta devolve a Cinza ao ser quebrada |
| **Tigela de Oferenda** | Bloco. O que for posto nela ao anoitecer pode ser aceito: trégua em casa por uma noite. O critério não aparece; repetir a mesma oferenda vale menos; depois de algumas aceitas, pular uma noite é desfeita e ele vem à porta. De manhã ela está intacta (recusou), vazia com rastro de cinza (aceitou) ou com outra coisa dentro (ele deixou algo) |
| **Lampião Pálido** | Bloco de luz que queima Cinza. A chama encolhe quando ele olha para o jogador ou está perto, e apaga quando ele passa. É o sinal "preso a um objeto" que a pesquisa pede no lugar de filtro de tela |
| **Ossos de Agouro** | Consumível de sorteio, sete desfechos (nada 25%, silêncio 15%, trégua 15%, apontam para o último vestígio 15%, presença agora 15%, ele aparece para o amigo mais próximo 10%, a Conta sobe 5%). O resultado se lê em como os ossos caem. O Diretor vicia o sorteio para o que mais fez o jogador reagir |

Saem do plano: o Espelho e a Infusão (ver "lugares", abaixo). O Giz fica como ideia, só se sobrar tempo.

Ajustes nos itens antigos: a **Vela** põe uma vela de verdade no chão e ele pode soprá-la; o **Fio** vibra segundos antes de romper.

**Chamas Pálidas.** A progressão também se lê no mundo, sem tela: tochas do próprio jogador queimando pálidas (miragem de tocha de almas, uma ou duas por vez), bichos da base virados para o lado de onde ele vem. A drenagem de cor continua, mas precisa de uma opção de desligar (filtro de tela sem opção é reclamação recorrente).

### Caçada (substitui III.1)

Regra em uma frase: ele só anda quando você não está olhando, ele ouve o que você faz, e ele não entra na luz da vela.

- **Onde começa:** nunca na base, nunca na zona da vela, nunca com o jogador montado ou a até 7 blocos de um amigo.
- **Aviso de 8 a 10 s:** o mundo emudece e uma luz falha; ele nasce e não se mexe. O mesmo aviso acontece sem caçada cerca de duas vezes para cada caçada real (evento `PRENUNCIO`).
- **Velocidade fora da tela:** começa entre a caminhada e a corrida do jogador e sobe até um pouco acima da corrida, abaixo da corrida pulando. O campo `velocidade` existia e não era lido.
- **Encarar segura, não vence:** some o fim por "encarado demais". O relógio da caçada só anda com ele fora da tela. Depois de 4 a 6 s na tela vem um piscar forçado de ~0,4 s em que ele avança até 3 blocos; os seguintes vêm a cada 3 a 5 s.
- **Bater:** ele recua (reaparece fora da tela a 10–14 blocos) duas vezes por caçada; a terceira não faz nada.
- **Portas e vãos:** caixa de colisão de 1,9 de altura na caça (antes 3,0: ele não entrava em casa nenhuma); abre porta de madeira em 1,5 a 3 s.
- **Luz:** apaga tochas por miragem (voltam depois). A vela continua intransponível; ele espera na borda no máximo 20 a 40 s.
- **Toque:** a 2,4 blocos e só com linha de visão.
- **Atalho:** 3 s fora da tela e (mais de 16 blocos ou sem caminho) → reaparece a 8–12 blocos, fora da tela, à frente do movimento do jogador. Recarga de 10 s. Desligado na primeira caçada da vida.
- **Ouvir:** quebrar bloco, porta e baú a até 16 blocos entregam a posição; agachado e parado é silêncio. A chance de ouvir sobe 10% a cada 10 s de busca, até +50%.
- **Busca:** para 2 a 3 s e olha em volta ao chegar; raio que encolhe (12, 8, 5).
- **Fingir que desistiu:** 35% das desistências, uma vez por caçada: fica calado 8 a 15 s e volta. **Sinal honesto de fim:** o som ambiente só volta quando ele foi embora de verdade.
- **Sem caminho não é sem saída:** 10 s sabendo onde o jogador está e sem caminho → aviso de 2,5 s num bloco ao lado do jogador (som abafado, partículas escuras); se o jogador ficar a até 1,5 bloco, toque; se sair, ele aparece ali. Não quebra bloco. Cada uso repetido encurta a espera (mínimo 4 s) e o aviso (mínimo 1,5 s).
- **Tetos:** 45 s de contato, 40 s de busca, 120 s no total.
- **Ser pego:** apagão de 2 a 3 s; acorda a 20–40 blocos, no escuro; vida nunca abaixo de 2 corações; o item da mão fica caído onde foi pego. **Marca:** −1 coração de vida máxima até dormir com uma vela acesa. Nas vezes seguintes pode ser o Avesso.
- **Som:** longe, tirar som; perto, os passos dele (que significam "você não está olhando"); colado, batimento. Sem música que cubra os passos: as camadas de perseguição ficam baixas.

### Lugares e dimensão (substitui III.2 e parte de III.3)

- **O jogador nunca escolhe entrar no Avesso.** Sem item, sem portal. Primeira vez dormindo (fase 3+, chance baixa); depois, em parte das capturas. Meta: 3 a 5 visitas por jogador em duas semanas.
- **Visita curta:** 45 a 180 s, teto de 4 minutos. Sai sozinho (acorda), andando para longe da casa ou deitando na cama da cópia. Volta ao ponto de onde saiu, com tudo. Morrer lá é acordar.
- **Arco de três visitas:** a primeira é idêntica à casa (só vazia e sem luz); a segunda tem dois ou três detalhes errados e ele dentro da casa, visto de fora; a terceira tem medida errada (um bloco a mais, porta em parede cega). Às vezes, nenhuma mudança.
- **Trazer de volta dúvida, não recompensa.** Sem Ninho, sem baú, sem chave. Por 2 a 5 minutos depois de voltar, uma ou duas coisas da cópia aparecem na casa real por miragem.
- **O Véu** (novo, e mais frequente que a dimensão): no mundo real, por 30 a 60 s, a neblina fecha, a cor some, o som some, os bichos e os amigos deixam de aparecer e dois ou três blocos da casa são trocados por miragem.
- **Soleira:** porta sozinha na paisagem. Atravessar três vezes abre o Véu, não a dimensão.
- **Casa do Vigia** e **Boneco:** continuam. Estruturas só são colocadas com ninguém por perto.
- A dimensão é o único lugar em que o mod estraga a casa.

### Tela

Filtro de tela como sinal principal é lido como truque barato. A cor drenada fica, discreta, com comando para desligar; os sinais principais passam a estar em objetos (chamas, bichos, a linha, a tigela, os próprios itens).

---

## Situação

| Parte | Situação |
|---|---|
| Testes dentro do jogo | Feito (#18) |
| Sons e trilha | Feito (0.9.0-alpha1) |
| Sentidos (pacotes, cor, borda, neblina, piscar, trilha em camadas, eco de passo, opções do cliente) | Feito (0.9.0-alpha1) |
| Eventos `ECO_PASSOS`, `VIGIA`, `NEBLINA`, `CANTIGA`, `PRENUNCIO` | Feito (0.9.0-alpha1). `CANTIGA` e o assobio do aviso esperam a Caixa de Música |
| Caçada em estágios, captura e marca | Feito (0.9.0-alpha1), com cinco testes de servidor |
| Tochas por miragem; "luz no fim" em túnel | Feito (0.9.0-alpha1) |
| A Conta e os cinco itens novos (Caixa de Música, Linha de Cinza, Lampião Pálido, Tigela de Oferenda, Ossos de Agouro) | Feito (0.9.0-alpha2), com nove testes de servidor e fotos de cada bloco. O viés "para o que mais fez o jogador reagir" nos Ossos ficou de fora |
| Vela física, Fio que vibra, Chamas Pálidas | Feito (0.9.0-alpha2). Os "bichos da base virados para o lado de onde ele vem" ficaram de fora |
| Receitas que aparecem sozinhas no livro do jogo | Feito (0.9.0-alpha2) |
| O Véu | Feito (0.9.0-alpha3), com dois testes de servidor e quatro fotos. Entrou o primeiro mixin do mod (cliente), para esconder entidades |
| A Soleira e o Boneco | Feito (0.9.0-alpha4), com dois testes de servidor e três fotos (`Erguidos`) |
| A Casa do Vigia | A fazer |
| O Avesso | Feito (0.9.0-alpha5 e alpha6): a dimensão, a cópia apagada da casa, a entrada pelo sono, as quatro saídas, a primeira visita vazia e ele presente da segunda em diante. Conferido no teste de cliente (o servidor de teste não carrega dimensões). Na 0.9.0-alpha6 entraram: ser levado em 35% das capturas, o modo próprio da criatura lá dentro (não some ao ser vista; da terceira visita em diante vem andando) e a "medida errada" da terceira visita |
| O baralho | A fazer |
| Som da assombração só para o alvo (as chamadas antigas de `ModSons.tocar`) | A fazer |
| Caçada: lembrar "deslogou no meio" e "fugiu voando" | A fazer |

**Não verificado por ninguém:** como os sons soam; se a caçada assusta ou irrita; se a velocidade está boa contra um jogador de verdade correndo; a pose abaixada em jogo real; se os avisos da Conta são percebidos ou passam batido; se o limite de 6 a 10 é curto ou folgado numa sessão de verdade; se a Linha de Cinza deixa a caçada fácil demais; se o Véu é percebido como parte do mod ou como defeito; se meio minuto é muito ou pouco; se o jogador entende que a Soleira responde a ser atravessada; se o Boneco é notado antes de chegar perto; se a primeira visita ao Avesso, em que nada acontece, é tensa ou só vazia; quanto demora o apagão da chegada numa máquina de verdade (a cópia gera chunks na hora).
