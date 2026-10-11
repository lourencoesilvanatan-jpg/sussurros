# A sessão de 10–11/10 e o que o dono pediu depois dela (SPOILERS)

> Escrita em 11/10/2026 pela sessão que programa o mod. O dono jogou a alpha16 num mundo novo e disse: "testei 100% sem código do mod, só o debug", "dizer se é necessário testar mais ou também seguirmos para expandir mais, justamente nessa imprevisibilidade", "senti falta de algumas ações que podem 'irritar' o monstro, ou também ele realmente aparecer na janela do nada sem avisar, e sair indo pro lado e não sumindo, fora que poderia ter mais interações assim; pode pesquisar se quiser". Ele não lê este arquivo.

**O que eu li.** O `sussurros-debug.log` (o terceiro trecho, linhas 1108 a 1493), os três logs do jogo (`2026-10-10-1`, `2026-10-11-1`, `latest`) e o `command_history.txt`. O relatório do `analisar.py`. O código das cenas e da criatura que os pedidos dele tocam. As pesquisas que já estão no repositório e nos PRs #32 e #33. Quatro buscas na rede, de página de mod (listadas no fim, com o limite delas).

**O que eu não fiz.** Não joguei, não vi nem ouvi nada. Nada aqui foi implementado.

---

## 1. A sessão

Um mundo novo, sobrevivência, 63 minutos de jogo em duas aberturas: 29 minutos em 10/10 (08:58 às 09:30) e 31 em 11/10 (11:48 às 12:24). Comandos: `/sussurros esquecer` e `/sussurros debug on` no primeiro minuto, e `/give` de pão, tochas e pedregulho. Nenhum comando de teste. Nenhum erro do mod nos logs do jogo.

| Medida | 09/10 (alpha12) | 10–11/10 (alpha16) |
|---|---|---|
| Tempo de jogo | 55 min | 60 min (com log ligado) |
| Fases 1, 2 e 3 (relógio do mundo) | 11, 21 e 40 min | 12, 30 e 50 min |
| Saídas por hora | 25 | 22 |
| Eventos do Diretor | 5 | 14 |
| Ambiente (presságios) | 10 | 7 |
| Avisos e cobranças da Conta | 12 | 0 |
| Manifestações da criatura | 0 | 4 (3 entraram na tela) |
| Primeiro contato | não existia | 1 tentativa, **visto na primeira**, aos 31 min de relógio |
| Saídas fortes | 0 | 3 (o contato e os dois eventos da sequência de ameaça) |
| Tempo dele na tela, somado | 0 | **2 segundos** |
| Alertas do analisador | — | nenhum |

**A ordem em que as coisas chegaram:**

- 0 a 11 min: quase nada, dois presságios. Como desenhado.
- 11,5 min: os primeiros três eventos (um passo, passos, um eco). Ele reagiu forte aos dois primeiros (correu, virou 51 e 62 graus) e o Diretor escalou na hora.
- 12 a 26 min: base com cama e porta, mina. Um vestígio, uma luz distante, um "vigia".
- 26,5 a 31 min: o Diretor recuou (silêncio de 4 minutos).
- **31 min: o primeiro contato**, pela forma do prazo, na base dele. Nasceu a 19 blocos, ele o percebeu em 9 segundos, pôs a mira (14 ticks) e ele dissolveu. A cinza ficou no lugar. **Um minuto depois ele fechou o jogo** (não sei se por isso).
- 32 min (segunda abertura): a Casa do Vigia foi erguida a 96 blocos e ele entrou nela em 50 segundos. Pegou a Caixa, cinzas e a Vela. As três páginas vieram (duas ele já tinha na mão para ler; a terceira foi entregue 13 segundos depois).
- 37 a 50 min: um "vigia" com reação clara (virou 115 graus e foi ver), um vulto a 77 blocos (visto), duas "passagens", pegadas. Riscou oito blocos de Linha de Cinza, atravessou a Soleira e tocou a Caixa ali.
- 50,7 min: a primeira **sequência de ameaça**. A espreita ele não viu (estava se afastando; ela desistiu a 52 blocos). O golpe foi o "atrás de você": a 11 blocos, no escuro de uma caverna (luz 0), 13 ticks na tela.
- 52 a 63 min: a trégua, um presságio, os Ossos em casa.

**O que deu certo, contra o que a análise de 09/10 pedia:**

- O contato foi visto na primeira tentativa. A definição mais exigente da alpha15 (mira e luz) não atrapalhou: `naTela=23t naMira=14t`.
- Ferramenta depois da ameaça: os lugares esperaram o contato (a Casa nasceu 66 segundos depois dele).
- A página chegou com o item, inclusive com três itens de uma vez.
- A Conta saiu do caminho: zero avisos, cinco usos na carência, cinco somados.
- O Diretor voltou a ser a voz principal (14 eventos contra 7 presságios) e houve um pico (a ameaça), seguido de trégua.

**O que a sessão mostra de fraco:**

1. **O contato veio seis minutos depois do prazo.** A janela (15 a 25 min) passou com ele na mina e dentro de casa. Na mina não havia lugar com luz para ele nascer (regra da alpha15); em casa a busca não acha linha livre. É um caso só. Se a próxima sessão repetir, o prazo precisa de uma forma que funcione com o jogador entocado (a janela dele, a boca do túnel).
2. **Ele viu a criatura por dois segundos em uma hora**, em três relances, e nos três ela sumiu no instante em que foi olhada. É o que ele pediu em 08/10 ("será que eu vi?"), e continua certo como regra. Mas sem nenhuma exceção a criatura nunca **faz** nada na frente dele. O pedido de agora ("sair indo pro lado e não sumindo") é essa exceção.
3. **Nenhum sinal com lugar foi seguido de algo naquele lugar** (0 de 4). É o "sinal sem referente" da análise de 09/10, intocado. A resposta planejada é o paradeiro (proposta 3 da pesquisa de inteligência, atualização B).
4. **A cadeia do sino não aconteceu.** Ele não foi buscar a cinza do contato (saiu do jogo), pegou cinzas no baú da Casa e gastou as oito em Linha de Cinza. O Sino não foi feito; a Vela não foi acesa. O primeiro uso verdadeiro do Sino, que a alpha14 preparou, segue sem teste de gente.
5. **Nada do que só existe da fase 3 em diante dentro de casa aconteceu**: nos 14 minutos de fase 3 ele esteve em casa três vezes, nunca por dois minutos seguidos.

**O que continua sem teste de gente:** a caçada (fase 4), a captura, o Véu, o Avesso, as cobranças da Conta, a cena da janela, a volta para casa. E as respostas dele: para que acha que cada item serve; se o contato foi um momento.

---

## 2. Testar mais, ou expandir?

**As duas coisas, e não é preciso repetir a primeira hora.**

- A primeira hora está validada nas regras: nenhum alerta, e os cinco defeitos de desenho da análise de 09/10 que ela atacava saíram do log (ferramenta antes da ameaça, nenhum contato, páginas fora de ordem, Conta como ruído, ritmo sem pico). Outro mundo novo mediria de novo a mesma coisa.
- O que falta medir está **depois** dela, e se mede continuando **no mesmo mundo**: com mais uns 30 minutos ele chega à fase 4.
- Expandir agora não atropela esse teste, desde que a expansão mexa no que ele já alcança (as aparições de observação das fases 2 e 3), e não acrescente mais coisa atrás da fase 4.
- A regra "variedade só depois de duas ou três caçadas" (análise da caçada) continua valendo **para a caçada**. O que ele pede agora é variedade no que vem antes dela.

---

## 3. O que ele pediu, contra o que existe

| Pedido | O que o mod tem hoje | O que falta |
|---|---|---|
| "Aparecer na janela do nada, sem avisar" | A cena "Do outro lado do vidro" existe desde a 0.5: fase 3 ou mais, de noite, em casa há 45 s, com vidro de verdade; sem anúncio nenhum; 48% a cada dois minutos na primeira vez, depois 18%, com 25 minutos de descanso. Visto, ele some; não visto, estala o vidro | Ele não chegou lá: a fase 3 veio aos 50 minutos, e a cena só tem a primeira chance depois de dois minutos seguidos em casa, de noite. Na fase 3 ele esteve em casa três vezes, a mais longa de 86 segundos. O portão está tarde e estreito demais para uma sessão de uma hora. E não se sabe se a base dele tem vidro (o log não diz quando a cena é pulada por falta de janela) |
| "Sair indo pro lado e não sumindo" | Não existe. Toda aparição de observação termina dissolvendo ou sumindo fora da tela. A espreita troca de lugar, mas por teleporte, quando não é vista | É comportamento novo: uma saída andando |
| "Ações que podem irritar o monstro" | Há contadores, e nenhum responde na hora: bater nele soma `VEZES_FERIDO` (ele fica mais ousado e mais rápido na caçada; uma mensagem a cada dois golpes); investigar puxa o traço `CONFRONTO`; oferecer uma arma contra ele na Tigela é "afronta" (as batidas na porta ganham peso por quatro minutos); a Conta cobra o uso demais | Nada disso é legível como "eu o irritei, e ele respondeu". Falta o degrau visível |
| "Sem avisar" | 30% das aparições comuns já vêm sem anúncio, 15% com um som discreto, 55% com dois passos; as cenas não anunciam. Mas o mundo emudece por 25 s em toda aparição, e isso também avisa | Pouca coisa. O que ele sentiu como aviso foi o do contato (de propósito, na primeira vez) e os dois passos da espreita |
| "Mais interações assim" | As propostas 1, 2 e 8 da pesquisa de inteligência (PR #33): ele nota, ele responde na hora, ele percebeu | Nenhuma foi implementada: ficaram fora da alpha14 por serem comportamento novo |

---

## 4. O que a pesquisa diz

Do que já está no repositório (`PESQUISA-E-ANALISE.md`, `pesquisa/2026-10-08-cacada.md`, PR #33), tudo conferido em código ou em página por quem escreveu:

- **Man From The Fog:** olhar é uma aposta. Quatro segundos olhado: some, persegue ou foge; doze sem ser olhado: ronda mais perto. A regra simples é o que dá peso ao olhar.
- **Don't Let It Learn (The Hollow):** reage ao olhar **em degraus**: uns 2 s e vira a cabeça, 6 s e avisa, 10 s e reage, com cinco reações sorteadas e penalidade para repetir a última.
- **From The Fog:** atrás de vidro ele **não some**: fica olhando. Visto de longe, às vezes inclina a cabeça e demora três segundos. Escorrega para trás do bloco ao lado.
- **Bracken (Lethal Company):** encará-lo parado soma raiva; a chance de enfurecer é raiva vezes 9%.
- **Warden:** raiva com números que o jogador aprende: sobe por vibração, sobe muito se atacado, cai um ponto por segundo, e aos 80 ele ruge **antes** de atacar.
- Das reclamações de jogador lidas na análise da caçada: fechar truque barato é seguro, tirar a saída do jogador não é; e **mudança de regra sem sinal é lida como defeito**.

Das buscas de hoje (páginas de mod e de comunidade; não li código nem comentários):

- **The One Who Will Arrive:** o perseguidor nasce com o mundo, não some sozinho e observa de trás de árvores, do alto de morros e "do lado de fora de uma janela". Escala quando o jogador entra no primeiro abrigo.
- **Cave Dweller Overhaul / Reimagined:** ao avistar o jogador ele para e encara, com um som próprio, antes de vir; muitas vezes só foge; a página fala em "atitudes" que mudam (passivo, neutro, hostil e intermediários).
- **Phasmophobia:** dizer o nome do fantasma o irrita um pouco, insultá-lo irrita bastante. **Amnesia: The Bunker:** barulho chama a Besta; barulho alto quase garante.

O que isso sustenta: (a) o olhar e a agressão do jogador como gatilhos, porque são as duas coisas que ele já sabe fazer sem aprender nada; (b) resposta em degraus, com o primeiro degrau barato e imediato; (c) o medidor pode ser escondido, mas cada subida tem de aparecer; (d) atrás de vidro é o lugar certo para ele demorar.

O que não sustenta: que raiva deva virar ataque. Nos mods em que vira, o monstro mata; aqui não mata, e a caçada é rara de propósito.

---

## 5. Proposta: "ele reage a você"

Três peças pequenas e uma média, todas em cima do que existe. Valem as regras de sempre: nunca alto, nunca matar, nunca estragar a construção; som e criatura só para o alvo; a caçada sempre avisa.

### 5.1 Saídas: ele nem sempre some (ajustar; pequeno a médio)

**O que o jogador percebe.** Na maioria das vezes, o relance de hoje. De vez em quando, visto, ele **não dissolve**: dá dois ou três passos para o lado, para trás de alguma coisa ou para fora do quadro da janela, e quando o jogador chega ali não há ninguém. Mais raro, e só de longe ou atrás de vidro: fica um ou dois segundos, a cabeça inclina, e então sai andando.

**Como.** Uma "saída" sorteada quando ele nasce (pelo ID da manifestação, sem gastar sorteio do mundo), sem repetir a última: `SOME` (hoje), `RECUA` (anda até um ponto fora da vista e só é retirado depois de dez ticks sem ser visto) e `FICA` (segura, inclina, e então recua). Só no modo de observar; o vulto distante fica como está, que foi o que ele aprovou. Nunca a menos de dez blocos: de perto, andando, ele vira boneco. O primeiro contato continua como é.

**Reaproveita.** `HospedeEntity.tickObservar`, a navegação que a caçada já usa, `Aparicao` (para achar o ponto fora da vista), o modelo (a cabeça já gira separada do corpo).

**Risco.** É o oposto do que ele pediu em 08/10. Por isso exceção, não regra: uma em cada três ou quatro aparições vistas, e nenhuma antes do contato. Outro risco: o caminho falha e ele fica parado à vista. Aí vale a saída de hoje (dissolve) depois de dois segundos.

**Teste.** GameTest: o jogador olha; ele continua existindo enquanto é visto, mudou de lugar pelo menos dois blocos de lado, e some depois de sair da vista. Linha `HOSPEDE ... saida=RECUA`. No analisador: quantas aparições vistas terminaram de cada jeito.

### 5.2 A janela, mais cedo (ajustar; pequeno)

**O que muda.** A cena passa a valer da fase 2 em diante, depois do primeiro contato (hoje é fase 3), e a primeira chance vem com 45 segundos em casa (hoje, na prática, com dois minutos seguidos). De noite, em casa e com vidro continuam valendo. Ela usa a saída `RECUA` como padrão: visto, ele sai andando ao longo da parede, para fora do quadro. Não visto, o estalo no vidro, como hoje.

**E uma linha de log** quando a cena é pulada por não haver janela, para sabermos se a base dele tem vidro.

**Risco.** A fase 2 é quando ele ainda está entendendo os itens. Uma vez por noite no máximo, e o descanso de 25 minutos continua.

**Teste.** O `TestesDeCliente` já fotografa essa cena; acrescentar a foto da saída. GameTest da regra de fase.

### 5.3 Irritá-lo (construir; médio)

**O que o jogador percebe.** O que ele faz **com o Hóspede** tem resposta na hora, e resposta maior se insistir.

- **Encarar** enquanto ele fica (saída `FICA`): a cabeça endireita e ele dá **um passo na direção do jogador** antes de sair.
- **Andar para cima dele**: ele não some; recua de frente, sem dar as costas.
- **Bater**: hoje ele some com um som grave. Passa a haver o terceiro golpe: ele não recua, e a cabeça inclina.
- **Mexer no que é dele** (derrubar o Boneco, esvaziar o baú da Casa do Vigia, atravessar a Soleira de novo, tocar o Sino ou a Caixa com ele à vista): uma batida, uma luz que falha, e nessa noite ele vem à janela ou à porta sem sorteio.

**Como.** Um número só por jogador, a `afronta`, de 0 a 100, que cai pela metade a cada dia do jogo. Cada gatilho soma e **dispara a sua resposta no mesmo segundo**. Dois degraus acumulados: acima de 40, a próxima noite em casa tem a janela ou a porta garantidas; acima de 80 e na fase 4, a próxima sequência de ameaça vem antes do descanso acabar (com todos os avisos dela). Os Ossos e a Oferenda aceita baixam a afronta: passam a ter um efeito que o jogador sente.

**Por que assim.** É a regra do Warden e do Bracken (o medidor é escondido, cada subida aparece) com os degraus do Don't Let It Learn. E é o contrário da Conta antiga, que cobrava "quando ele já tinha esquecido o que fez".

**O que não faz.** Não tira vida, não tira item, não mexe em bloco, não mata. Quem não quer irritar, não irrita: nenhum gatilho é coisa que se faz sem querer jogando normalmente.

**Risco.** Vira botão ("deixa eu irritar para ver"). É aceitável, e até desejável nas primeiras vezes: é o jogador testando a regra. O teto por noite (uma visita garantida) segura o excesso. Segundo risco: somar com a Conta e com a obsessão e ninguém mais saber por que algo aconteceu. Por isso a linha de log dela diz sempre o gatilho.

**Teste.** Um GameTest por gatilho: a afronta sobe e a resposta sai no mesmo segundo; cai com o tempo; os Ossos a baixam. Linha `AFRONTA +15 por=ENCAROU total=35 resposta=PASSO`. Tabela no analisador.

### 5.4 O que eu não faria agora

- **Raiva que vira caçada na hora.** A caçada é o momento mais caro do mod e sempre avisa. A afronta pode adiantá-la, nunca dispensá-la do aviso.
- **Mais itens, lugares ou eventos.** Ele tem meia dúzia de coisas que ainda não usou.
- **O paradeiro** (atualização B). Continua sendo o conserto certo para "sinal sem referente", e é maior. Vem depois desta.
- **Mexer no prazo do contato** por uma sessão só.

### 5.5 Ordem

1. 5.1 e 5.2 juntas (uma versão): é o que ele pediu com as palavras dele, e a janela depende da saída.
2. Ele joga mais uma hora no mesmo mundo. O log diz se a cena veio, de que jeito ele saiu, e se a fase 4 chegou.
3. 5.3 (outra versão), já com o que a primeira mostrou.

---

## 6. Perguntas para o dono (sem entregar nada)

1. A sua base tem janela de vidro?
2. No fim da primeira parte (10/10), aconteceu alguma coisa que te fez fechar o jogo, ou foi só a hora?
3. Dos itens que apareceram: para que você acha que cada um serve? (É a pergunta do roteiro, ainda sem resposta.)

---

### Respostas do dono (11/10/2026) e o que foi feito

1. A base tem janela de vidro, "mas tem outros lugares com janela pois estou numa vila".
2. "Fechei pra dormir": o fim da primeira parte não teve a ver com o contato.
3. "Linha proteção? rádio pra acalmar a criatura? osso alguns formatos indicam algo da criatura? e o resto não lembro." Os três que ele usou, com a ideia certa (o "rádio" é a Caixa de Música); dos que só teve na mão, nada ficou.

A etapa 1 (5.1 e 5.2) entrou na 0.9.0-alpha17. O que saiu diferente do escrito acima: a saída `FICA` com a cabeça inclinada ficou para depois (pede mexer no modelo e foto); no lugar dela, toda saída andando segura o olhar de 0,4 a 1 segundo antes de virar. A etapa 2 (5.3) não foi começada.

## 7. Limites

- Uma sessão, um jogador, em duas partes. O que digo sobre o que ele sentiu é inferência do log, fora as frases dele.
- As buscas de hoje foram em páginas de mod e de comunidade, resumidas por ferramenta; não li código nem comentários desses mods. O que vem das pesquisas anteriores tem a marca de quem as fez.
- Nenhum número da proposta foi testado.

## Fontes de hoje

- The Man From The Fog (guia): https://www.minecraft-guides.com/mod/the-man-from-the-fog/
- Cave Dweller Overhaul: https://www.modpackindex.com/mod/166591/cave-dweller-overhaul
- The One Who Will Arrive: https://www.curseforge.com/minecraft/mc-mods/the-one-who-will-arrive
- PNG Stalkers: https://modrinth.com/mod/png-stalkers
- Phasmophobia, comportamento do fantasma: https://phasmophobia.fandom.com/wiki/Ghost
- Amnesia: The Bunker, esconder-se e barulho: https://amnesia.fandom.com/wiki/Hiding
