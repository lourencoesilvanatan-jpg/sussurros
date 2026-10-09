# Sussurros — design completo (SPOILERS)

## Fases (tempo de assombração, em segundos de jogo no mundo normal)
| Fase | Começa em | O que libera |
|---|---|---|
| 0 | 0 | Nada. Silêncio. |
| 1 | 10 min | Passos atrás de você; ecos dos blocos que VOCÊ quebrou. Uma página cai atrás de você. |
| 2 | 30 min | Sussurros, portas que abrem/fecham sozinhas, tochas que somem atrás de você. Páginas ao lado da cama. |
| 3 | 55 min | Ele aparece ao longe (lateral da visão) e logo atrás de você no escuro. Você recebe o Olho. |
| 4 | 90 min | Ele caça: só se move quando você não olha, apaga tochas, toca em você. Espera do lado de fora da vela. |

O tempo corre 2x quando a inquietação está alta. Ler páginas, usar o Olho e ignorar os sustos também aceleram.

## Inquietação (0-200, invisível)
Sobe no escuro, no subsolo e à noite longe da cama. Desce na luz forte e dentro da vela.
Quanto maior, mais rápido os eventos acontecem.

## Aprendizado
Depois de cada evento, o mod espera 2 s: se você virou a câmera mais de 70° ou saiu correndo,
aquele tipo de evento fica 25% mais provável (até 4x). Se não reagiu, fica 10% menos provável.
Ignorar 6 eventos seguidos o irrita: a assombração avança 5 minutos.

## A criatura
- Some se você chegar perto, se ficar tempo demais olhando, ou se você a ferir (ela não morre).
- Cada vez que é vista fica mais ousada: aparece mais perto e aguenta ser vista por mais tempo.
- Cada vez que é ferida fica mais rápida na caça.
- Na caça congela enquanto você olha. Encarar tempo suficiente a faz desistir.

## Itens
- **Vela Pálida** (favo de mel + linha + farinha de osso = 2): zona de calma de 8 blocos.
  90 s na primeira, depois cai 12 s a cada 2 velas (mínimo 30 s). Na fase 4, depois de 2 velas,
  ele passa a esperar do lado de fora e entra quando a chama apaga.
- **Olho Sussurrante**: faz a criatura brilhar e diz direção e distância. Custa escuridão e
  avança a assombração; na fase 3+, usar sem ela por perto pode chamá-la.
- **Página Rasgada**: sempre mostra a próxima página do diário (10 no total).
  Fontes: transições de fase, ao acordar (fase 2+), baús de masmorra, mina abandonada,
  biblioteca de fortaleza, mansão, cidade ancestral e naufrágio.

## Cama
Dormir sempre na mesma cama (4+ vezes) faz ele comentar. Na fase 3+ ele apaga tochas perto da cama enquanto você dorme.

---
# Versão 0.3.0 — o Diretor ficou mais esperto

Inspirado no "menace gauge" de Alien: Isolation e no diretor de Left 4 Dead.

## Pressão e trégua
Cada evento soma pressão (intensidade de 4 a 45). A presença da criatura também soma.
A pressão cai sozinha com o tempo. Ao passar de 75, o Diretor RECUA: 2,5 a 5 minutos sem
nada acontecer. O silêncio depois do pico é proposital: é o contraste que faz o próximo susto funcionar.

## Anti-repetição
O último evento fica com 15% do peso, o penúltimo com 40% e o antepenúltimo com 70%.

## Aprendizado por categoria
Além de cada evento, ele aprende o TIPO de medo: SOM, AMBIENTE, MENTE, VISÃO, AMEAÇA.
Se você se assusta com sons, todo som novo já nasce mais provável.

## Reações detectadas (2 s após o evento)
Virar a câmera mais de 70°, correr, agachar, ou CONGELAR (estava andando e parou).

## Cadeias
Se você reagiu, 65% de chance de uma continuação:
passos → um passo só, bem perto | eco → eco mais perto | porta → batida ou sussurro |
batida → porta | tocha → ele atrás de você (fase 3+) | sussurro → um passo |
sua fala repetida → sussurro | presença → atrás de você.
Se você NÃO reagiu, 30% de chance de ele subir o tom (presença ou tocha).
Ser visto tem 50% de chance de render um sussurro "você me viu" depois.

## Ramos destravados pelo seu jeito de jogar
- Olha muito para trás sem motivo (12+ vezes): ele passa a aparecer NOS LADOS, não atrás.
- Fica muito em casa à noite (5+ min): destrava as BATIDAS na porta.
- Passa muito tempo no subsolo (15+ min): ecos ficam mais frequentes.
- Morreu? Ele pode aparecer parado exatamente onde você morreu (fase 3+, entre 16 e 72 blocos).
- O que você escreve no chat pode voltar como sussurro (fase 3+).

## Novos eventos
PASSO_UNICO (um passo e mais nada, o "falso positivo"), BATIDA, ECO_CHAT, TUMULO, VISTO.

---
# Versão 0.4.0 — "O Diretor te conhece"

Mudanças só no cérebro (Diretor). Eventos, itens, criatura, fases e diário iguais à 0.3.

- **Reação por mudança:** baseline dos 3 s anteriores (amostra a cada 0,25 s) vs. janela de 3 s depois.
  Sinais: giro súbito (+olhou para a fonte), congelou, fugiu (+da fonte), agachou, investigou. Viram confiança c (0 a 1).
- **Ele percebeu?** Cada evento tem observabilidade (distância, volume, jogador ocupado). Abaixo de 0,15 não aprende.
  Aparições só são avaliadas quando a criatura é AVISTADA. Tochas e portas não percebidas são avaliadas quando o
  jogador olha para o lugar depois (até 5 min).
- **Medo x interesse:** investigar a fonte vira "engajamento", que alimenta o traço confronto e o interesse por categoria.
- **Duas memórias:** longa (salva, lenta, 0,5–2,0) e curta (sessão, rápida, volta ao neutro em ~20 min de jogo).
  Teto de 45% por categoria. Exploração: 10% (40% testando; 5% quando "seguro"; 25% quando "perdido").
- **Estados:** CALMO → OBSERVANDO → TESTANDO → ESCALANDO → AMEAÇANDO → RECUANDO (durações sorteadas).
- **Vulnerabilidade V (0–100):** escuro 25, sozinho 15, longe/subsolo 15, rotina 15, inquietação 10, marco 10, tempo 0–20.
  Bloqueios: vida < 6, dano nos últimos 10 s, água/lava, caindo, montado, dormindo.
  Portões: intensidade ≤10 exige V≥20; ≤20 → 40; ≤30 → 55; acima → 70. Espera o momento por até 2 min.
- **Piso:** 12 min (fase 1) ou 8 min (fase 2+) sem nada → um evento bem fraco, mesmo em segurança.
- **Cadeias:** só escalando/ameaçando; até 2 elos; continuação sorteada (com opção "nada"); carência depois.
  Aversão a repetir pares de tipos de medo (SOM>VISAO etc.).
- **Perfil (6 traços 0–100):** cautela, luz, caseiro, explorador, confronto, fuga. Acima de 65 mudam a estratégia.
- **Lugares:** mapa de calor por chunk; rotas (20+ min), marcos (onde algo forte funcionou: V+10 ao voltar);
  12% das presenças surgem numa rota; 12% dos ecos longe de casa vêm da direção de casa.
- **Removido:** o sussurro que comentava a indiferença do jogador (a escalada agora é silenciosa).

---
# Versão 0.4.1 — correções do primeiro teste

Ajustes feitos depois do primeiro log de jogo real (33 min, 8 eventos naturais, nunca escalou).

## Agenda
- Ao entrar em TESTANDO, ESCALANDO ou AMEAÇANDO, o próximo evento é puxado para 15–45 s.
  (0.4: valia o cronômetro antigo, sorteado em OBSERVANDO; perdia a primeira noite inteira.)
- **Oportunidade:** fora de OBSERVANDO, com V ≥ 60 e pelo menos metade do intervalo mínimo
  (mín. 30 s) desde o último evento, ele age sem esperar o cronômetro.
- OBSERVANDO multiplica o intervalo por 1,5 (era 2,0).

## Leitura e aprendizado
- **Parado antes e depois** (sem andar nem girar a câmera) = "sem dados", não "não reagiu".
  Chat aberto, menu ou AFK não ensinam mais nada nem contam como teste fracassado.
- **Percepção confirmada pela reação:** se o jogador virou para a fonte ou foi até ela,
  a observabilidade sobe para pelo menos 0,80 (a fórmula de distância subestimava ecos).
- Limiar de "isso funciona" caiu de 0,50 para **0,45** (virar e olhar para a fonte dava 0,48).
- **Reação forte em OBSERVANDO** (c ≥ 0,45) escala direto para ESCALANDO.
- Entrar em TESTANDO não zera uma reação forte dos últimos 120 s.
- **Indiferença:** fase 2+, duas rodadas de TESTANDO terminadas por 3 testes sem reação
  e ele escala mesmo assim (cumpre o que a página 6 do diário promete).

## Criatura
- ATRÁS: só some se você chegar a 4 blocos (0.4: nascia a 9–14 e sumia abaixo de 12 no mesmo tick).
- Tempo mínimo de visão antes de sumir: 1 s + 0,2 s por ousadia (era 0,15 s).
- **Visibilidade:** a mais de 20 blocos, num lugar com luz < 4, ela não conta como "vista"
  (uma silhueta preta no breu não aparece na tela).

## Casa
- Clicar numa cama (mesmo de dia, só para marcar o respawn) já registra a casa.
  "Você sempre dorme aqui" continua contando só noites dormidas.

## Comandos de teste
- `evento`: não mexe em agenda, pressão, anti-repetição, aprendizado nem memória da criatura;
  só remove a criatura atual se o novo evento também usa a criatura; diz o que faltou quando falha.
- `fase`: subindo entrega páginas e Olho das fases puladas; descendo avisa que o tempo voltou.
- Bater num Hóspede do ovo gerador (ou num criado por comando) não conta como ferimento.


---
# Versão 0.4.2 — "O Rastro"

Antes o Diretor sabia COMO você reage. Agora o Hóspede sabe POR ONDE você vive.

## Obsessão (0–100, salva)
Separada da pressão. Pressão = tensão do momento (cai rápido, faz ele recuar). Obsessão = escalada
lenta (cai pouco, libera a ameaça).
- Sobe: +0,05/s escalando (+0,03 se V ≥ 60), +0,02/s testando, +2×c quando um susto funciona,
  +1,5 quando você ignora, +3 quando você o vê, +5 quando ele some por ter sido visto.
- Desce: −0,02/s recuando, −5 ao acender a vela, ×0,4 depois de um golpe (×0,7/×0,8 se a sequência falhar).
- **AMEAÇANDO** agora exige: fase 3+, obsessão ≥ 60, V ≥ 60, pressão < 60, nenhuma criatura presente
  e 10 minutos desde a última sequência. (0.4.1 exigia pressão ≥ 50, que nunca acontecia.)

## Sequência de ameaça (substitui o sorteio no estado AMEAÇANDO)
1. **Presença que espreita** (evento ESPREITA): nasce no seu rastro ou logo fora da tela, a 22–38 blocos, anunciada por passos.
2. **Espreita**: 2–3 reposicionamentos (ver abaixo), depois some.
3. **Perda de contato**: silêncio de 15–35 s.
4. **Golpe**, espera o escuro por até 2 min: CAÇA na fase 4, ATRÁS na fase 3.
5. **Silêncio real**: RECUANDO de 3 a 5 minutos.
Nenhum evento aleatório acontece durante a sequência.

## Intensidade-alvo (ESCALANDO)
Começa em 10 e chega a 30 em 10 minutos (limitada ao evento mais forte que cabe agora).
Peso de cada evento × exp(−((intensidade − alvo)/10)²), com piso de 15%.
Aparece no log como `escolha[...] alvo=NN`.

## Rastro
Um ponto a cada 2 s (se andou 2+ blocos), últimos ~6 minutos. Usado por:
- PRESENÇA (45%): num ponto por onde você passou há 20–180 s, a 18–40 blocos, fora da tela.
- ESPREITA: início (50%) e reposicionamentos.
- ECO (25%): o som vem de um lugar por onde você passou há 30–240 s.
Log: `RASTRO ...` e `HOSPEDE ... [rastro idade=NNs]`.

## Presença visível
55–80° do seu olhar (logo fora da tela), 18–35 blocos, 25 s de duração, preferindo lugares onde dá
para enxergá-lo. Anunciada por dois passos vindos dele. Regra geral: ele **nunca nasce dentro da tela**.

## Dois cones
PERCEBEU (~45°, está na tela): conta como avistado, congela a caça.
ENCAROU (~15°): pesa o dobro para ele sumir; encarar a caça a faz desistir.
Observando: some com 1,5 s encarado ou 3 s de canto (+ousadia).

## Espreita (modo ESPREITAR)
Quando você fica 1,5–5 s sem vê-lo (12 s antes da primeira vez que você o vê, desde a 0.4.2a): 15% some (a partir da 2ª vez), 15% fica, 20% troca de lado,
50% aproxima 4–9 blocos (mínimo 9–10). O lugar é escolhido entre 16 candidatos fora da tela,
com nota para cobertura (bloco sólido entre ele e você), cabeça visível e ponto do rastro.
30% das vezes o deslocamento faz um passo baixinho vindo do novo lugar.
Some se você chegar a 8 blocos, ou com 3 s encarado / 6 s de canto.

## Âncora: a porta habitual
Cada porta de madeira que você abre é contada. Com 3+ usos (e mais que a anterior, ou 5+ se a antiga
não foi usada na sessão) ela vira "a porta de sempre". PORTA e BATIDA a preferem (80%) até 24 blocos.

## Origem gravada na criatura
DIRETOR / OLHO / COMANDO + evento. Só aparições do Diretor de categoria VISÃO ensinam alguma coisa.
Log: `HOSPEDE origem=... evento=... modo=... dist=...` e `avistou a criatura (origem=..., evento=...)`.

## Som
Volume acima de 1 aumenta o alcance (16 blocos × volume). Ecos e anúncios agora escalam o volume
com a distância para serem ouvidos (antes, sons a mais de 16 blocos eram inaudíveis).

## Teste
- `runClient` usa sempre o nome **Jogador**: a memória sobrevive entre sessões (use `/sussurros esquecer`).
- `/sussurros obsessao <0-100>` e `/sussurros evento espreita` para testar partes isoladas.

## Ainda não feito (depois da 0.4.2a)
Olhos que brilham, animais que olham para o mesmo ponto, pegadas interrompidas,
mais microcenas e números num arquivo de configuração. Sons próprios e eco de ação já existem.

---
# Versão 0.4.2a — "O Rastro Vivo"

Pequena, em cima da 0.4.2, sem refatorar: coisas para SENTIR jogando.

## Cena "Ele voltou com você"
Gatilho: casa conhecida (cama clicada), você fica 3+ min a 55+ blocos dela e volta a 16 blocos.
Fase 2+, fora de AMEAÇANDO/RECUANDO, sem criatura. Chance 60% na 1ª vez da sessão, 25% depois;
intervalo mínimo de 25 min. Um sorteio por volta.
1. Nada por 10–20 s.
2. Passos (2–3, às vezes um estalo) num ponto do rastro por onde você voltou, do lado de fora (>12 blocos da cama).
3. 6–12 s depois, a porta de sempre se mexe (50%: mexe de novo 1–2 s depois, "alguém entrou"). Espera até 15 s você ficar de costas.
4. Fase 2: acaba aqui. Fase 3+: 10–25 s depois ele está num ponto do caminho de volta, SEM anúncio, em modo espreita com 1 reposicionamento.
5. 90–150 s de silêncio.
Nenhum evento aleatório acontece durante a cena. Log: `CENA CASA: ...`.

## Anúncio sem regra
PRESENÇA e início da ESPREITA: 55% dois passos, 30% nada, 15% um único som discreto (pano, estalo ou arrasto).
Log: `anúncio: ...`.

## Eco de ação
O mod guarda as últimas 10 ações (QUEBRA de bloco, PORTA usada) com som, lugar e hora. Minerar num lugar
vira uma lembrança só. O ECO toca uma delas:
- do lugar onde você a fez, se faz 45+ s e fica a 8–24 blocos;
- senão (65%), de um ponto do rastro a 8–24 blocos;
- senão, como antes (direção de casa ou ponto próximo).
Porta: abre e, ~1 s depois, fecha. Log: `ECO DE ACAO: ...`.

## Sons próprios (6, com 2 variações cada)
Sintetizados do zero (ruído filtrado, ressonâncias, envelopes): pano, respiração, madeira, arrasto, estalo, grave.
Usados raramente e nunca como certeza de que ele está ali:
- PASSO ÚNICO: 25% vira estalo/pano/respiração, sem ninguém (falso positivo).
- Anúncio discreto (15%) e deslocamento da espreita (metade das vezes que faz barulho).
- ATRÁS: 25% você ouve ele respirar.
- CAÇA: 50% um baque grave junto com o coração.
Legendas existem (para quem joga com legendas ligadas).

## Intensidade-alvo com contexto
alvo = base (10→30 em 10 min de ESCALANDO) + obsessão×0,08 (até +8) + 6×(V−50)/50 (até +6)
− abatimento (até −8, some em 150 s depois de algo de intensidade 22+). Limitado entre 8 e o mais forte possível.
Log: `alvo: base=... bonusObsessao=... bonusV=... abatimento=... final=...`.

## Comandos de teste
`/sussurros cena casa`, `/sussurros cena eco` (não aprendem; criatura de origem COMANDO).


---
# Versão 0.4.3-exp1 — "Presença Física"

Primeira build depois do Rastro Vivo que volta a adicionar experiência perceptível, sem trocar a arquitetura do Diretor.

## Cena "Algo no túnel"
Gatilho natural: fase 2+, 2+ minutos no subsolo, uma quebra recente, sem outra cena/ameaça.
Primeira chance da sessão: 55%; próximas: 20%; no máximo uma natural a cada ~20 min e um sorteio por descida longa.

1. 5–10 s de espera.
2. Um som de uma quebra REAL do jogador volta de um ponto antigo do Rastro ou do local da ação.
3. 4–8 s depois, pano/madeira/arrasto vem de outro ponto do túnel.
4. Fase 2 termina aí. Fase 3+: depois de 5–11 s, o Hóspede aparece silencioso num ponto do Rastro em ESPREITAR, com no máximo 1 reposicionamento.
5. 60–100 s de silêncio.

Comando de teste: `/sussurros cena tunel`.

## Corpo do Hóspede
O modo da criatura agora é sincronizado com o cliente. Sem GeckoLib ainda:
- ESPREITAR: postura mais inclinada e assimétrica;
- ESPERAR: quase imóvel na borda da vela;
- CACAR: tronco mais à frente, braços e passadas mais agressivos;
- OBSERVAR mantém a cabeça inclinada original.

## Correções perceptíveis
- Spawn usa um cone de segurança maior que o cone de percepção para não materializar na borda da tela.
- TÚMULO não nasce se o ponto da morte estiver na tela.
- A cena de casa espera a porta habitual; olhar para ela não faz mais a cena escolher outra porta aleatória.

---
# Versão 0.5.0-alpha1 — “O Mundo Lembra”

A 0.4.x ensinou o Diretor a medir reação e lembrar caminhos. A 0.5-alpha começa a fazer o **mundo** expressar essa inteligência.

## Contexto
Uma leitura barata, uma vez por segundo, classifica o jogador em CASA, SUBSOLO, ABERTO ou OUTRO. A seleção normal de eventos recebe multiplicadores diferentes por contexto. Isso não é um quarto sistema de pacing: é uma lente sobre o mesmo seletor adaptativo.

## SINAL
Novo evento de falso positivo, fase 2, intensidade 8. O sinal varia por contexto e nunca cria a criatura por conta própria. Ele pode reutilizar porta habitual, ação antiga e Rastro. Se o jogador reage, a leitura normal ainda pode aprender que esse tipo de manipulação funciona.

## Cena “Na linha das árvores”
Fase 3+, área aberta por ~90 s, longe da casa e com Rastro suficiente. Duas manifestações sem anúncio garantido: a primeira distante, a segunda do lado oposto e mais próxima. Cobertura e penumbra pesam na escolha. Depois vem 75–130 s de silêncio.

## Cobertura
PRESENÇA ganhou uma tentativa específica de spawn que exige cobertura entre Hóspede e jogador, mas mantém linha de visão potencial quando o jogador vira. É uma forma barata de produzir aparições parciais sem esconder a criatura por shader ou animação.

## Manipulação ambiental sem griefing
TOCHA normalmente pisca e é restaurada. Só na fase 4, com obsessão alta, uma minoria pode ser levada definitivamente.

## Identidade sonora
CAÇA, toque e desaparecimento evitam sons emblemáticos de Warden, shrieker e Enderman e usam a biblioteca própria do mod. O objetivo é que o jogador não traduza o evento para “som de outro mob vanilla”.

---
# Versão 0.5.0-alpha2 — “Memória Física”

A alpha1 fez o lugar mudar o repertório. A alpha2 tenta uma coisa mais difícil: fazer o jogador perceber que **um lugar antigo continua existindo na memória do Diretor**.

## FLORESTA
O contexto passa a ter `FLORESTA`. Não é detecção de bioma; é uma leitura geométrica barata: céu visível e obstáculos verticais em várias direções próximas. O objetivo é reconhecer ambientes onde uma silhueta pode ficar parcialmente escondida mesmo fora de uma floresta vanilla específica.

FLORESTA aumenta principalmente `SEGUIDOR`, `PRESENCA`, passos e `SINAL`, reduzindo eventos domésticos.

## SEGUIDOR
Evento de fase 2, categoria SOM, intensidade 16. Em vez de criar passos atrás do jogador por fórmula, seleciona pontos reais do `Rastro` dos últimos minutos e toca uma sequência curta ao longo deles.

O evento deliberadamente não garante Hóspede. Uma cadeia posterior pode continuar a ideia, mas o primeiro significado é: **alguma coisa parece estar usando o caminho que você acabou de usar**.

## Marcos exatos
`Lugares` já marcava chunks onde uma reação forte aconteceu. Marcos criados nesta versão também guardam X/Y/Z do foco do evento. Dados antigos continuam válidos, só sem precisão de coordenada.

Condição para criar marco continua conservadora: evento forte (`intensidade >= 28`), reação forte (`c >= 0.5`) e fonte espacial conhecida.

## Cena “Foi aqui”
Quando o jogador volta a um chunk marcado em uma sessão futura, fase 3+, o Diretor pode montar `FOI_AQUI`.

Fluxo:
1. espera curta;
2. ruído de memória — usa a coordenada exata antiga se ela ainda estiver a uma distância útil e fora da tela; senão usa Rastro/fallback;
3. manifestação em `ESPREITAR`, tentando primeiro perto da coordenada persistida, depois cobertura/Rastro/fallback;
4. no máximo um reposicionamento;
5. 70–110 s de silêncio.

Cada marco recebe uma oportunidade natural por sessão, e a cena tem cooldown aproximado de 15 min. A intenção é não transformar marcos em “armadilhas que sempre disparam”.

## Perfil espacial
O Perfil não altera apenas pesos de eventos:
- `CAUTELA` alta estreita PRESENCA para laterais que contornam o hábito de olhar para trás;
- `CONFRONTO` alto leva PRESENCA mais para trás;
- `FUGA` alta usa ângulo intermediário;
- `FLORESTA` amplia o lado/traseira.

Isso é deliberadamente simples. A meta é testar se estilos diferentes começam a produzir relatos diferentes antes de criar uma IA de navegação mais complexa.

## Corpo depois de ser visto
`HospedeEntity` sincroniza um booleano visual depois da primeira percepção. O renderer passa isso ao modelo. A cabeça/tronco ficam discretamente menos tortos depois que o jogador o encontra, como se a postura anterior não fosse para ter sido vista.

É apresentação somente: esse booleano não entra no Diretor nem muda os estados do Hóspede.

---
# Versão 0.5.0-alpha3 — “Vestígios e Contramedidas”

A alpha3 introduz uma segunda direção de aprendizado: o jogador começa a investigar o Hóspede.

## Cinza Pálida
Manifestações naturais percebidas podem deixar um item real na coordenada de desaparecimento. Isso transforma uma aparição em evidência física e cria a matéria-prima das novas ferramentas. A chance cai depois das primeiras unidades para não criar uma fazenda previsível.

## Sino Oco
O sino não localiza de forma determinística. Ele pede uma resposta:
- criatura próxima: resposta pode vir dela;
- sem criatura: um ponto antigo do Rastro pode responder;
- silêncio também é válido;
- com uso repetido, a chance de uma resposta-isca do Rastro cresce, mesmo quando a criatura está em outro lugar.

Assim o objeto é útil sem virar sonar perfeito — e expressa a tese central do mod: **ele aprende como você investiga**.

## Fio de Vigília
Uma única área temporária de raio 6, 180 s. Se a criatura realmente entrar, o fio rompe; se não entrar, expira. É deliberadamente o oposto do Sino: pequeno alcance, mas uma confirmação local mais confiável.

## Progressão de itens
O conjunto agora forma cinco verbos:
- Vela = afastar;
- Olho = procurar;
- Sino = perguntar;
- Fio = confirmar;
- Cinza = coletar/provar.

O diário sobe para 14 páginas para ensinar isso como descoberta do personagem anterior, não como tutorial de HUD.

---
# Versão 0.5.0-alpha4 — “Marcas e Soleiras”

A alpha4 transforma acontecimentos em **lugares que continuam significando alguma coisa depois que o Hóspede foi embora**.

## Vestígios
`Vestigios` guarda até 18 coordenadas persistentes por jogador. Tipos atuais: desaparecimento, vigília e pegadas. Olho e Sino consultam essas marcas quando não há uma resposta mais imediata. Vestígios muito antigos deixam de dominar as ferramentas.

## Isca Pálida
A Isca marca por dois minutos um ponto sugerido pelo jogador. A próxima PRESENÇA tenta usar a área da isca antes dos caminhos normais de Rastro/rota/cobertura. O Hóspede aprende usos bem-sucedidos e passa a ter chance crescente de ignorá-la.

## PEGADAS
Evento ambiental que percorre coordenadas reais do Rastro com pequenas partículas de cinza e sons de passo. Não cria a criatura. O último ponto se torna uma marca persistente em jogo normal.

## Do outro lado do vidro
Cena de casa noturna que procura vidro real no ambiente e tenta colocar a criatura do lado externo da janela. A cena não depende da linha de visão normal da entidade, porque o vidro pode bloquear o raycast: ela mede se o jogador virou para a direção do corpo. Ver a criatura a faz desaparecer; não vê-la permite um estalo no vidro antes do fim. Depois há silêncio.

## Variação corporal
O modelo vanilla continua sendo usado, mas a manifestação ganha uma das três pequenas assimetrias corporais e, em uma minoria de eventos visuais, olhos pálidos não emissivos. Tudo é derivado do ID da manifestação para não perturbar o RNG do Diretor.

## Ferramentas como linguagem de mão dupla
A progressão de itens deixa de ser só proteção. O jogador consegue perguntar, confirmar, chamar e investigar marcas; ao mesmo tempo, o mod registra quantas vezes essas ferramentas funcionaram. A intenção é que contramedidas eficientes virem informação para o próprio Hóspede, e não soluções permanentes.

---
# Versão 0.6.0-alpha1 — “O Mundo Já Estava Errado”

A 0.6 muda o gargalo do projeto: o Diretor já sabia observar, lembrar e dosar; faltava **repertório para dirigir**, especialmente nos primeiros 30 minutos.

## Presságios
Fase 0 não recebe Hóspede natural. Em vez disso, `Atmosfera` agenda presságios espaçados que não entram no aprendizado: animais, ecos, ruídos, cinza e luzes temporárias. O objetivo é conservar a revelação da criatura enquanto elimina a sensação de “mod desligado”.

## Atmosfera e orçamento
O conteúdo ambiental possui orçamento próprio, regeneração lenta, cooldown por família e memória das últimas cinco ocorrências. Isso é separado de pressão/obsessão de propósito: pressão decide o arco dramático; orçamento atmosférico impede que o mundo vire uma máquina de truques.

Famílias atuais: ANIMAIS, LUZ, OBJETO, RUIDO e VESTIGIO.

## Animais
Animais vanilla não viram monstros. Por poucos segundos eles podem interromper navegação e olhar todos para uma coordenada vazia, para o jogador ou para um ponto invisível em movimento. O efeito é raro e não precisa produzir manifestação depois.

## Luz e realidade física
A maior parte das mudanças em tochas/blocos é auto-reversível. `AlteracoesTemporarias` guarda o estado original e só restaura se o jogador não tiver mudado o bloco. Uma exceção rara em fase alta pode quebrar uma tocha de verdade, mas usa drop normal para não apagar recurso silenciosamente.

## Repertório de fase 1
PASSAGEM, ANIMAIS, VESTIGIO, LUZ_ERRADA, SINAL_DISTANTE, RUIDO_RETORNO, OBJETO_FORA_LUGAR e TRILHA_INTERROMPIDA preenchem intensidades 11–15. Isso evita o problema observado no log em que a intensidade-alvo subia, mas a fase 1 só tinha eventos de intensidade <=10 disponíveis.

## Contexto estável
CASA/SUBSOLO continuam responsivos; ABERTO/FLORESTA/OUTRO exigem estabilidade temporal. O contexto passa a representar uma área, não uma amostra instantânea de geometria.

## Estruturas narrativas
Três estruturas pequenas podem ser introduzidas fora da tela: Marco de Estrada, Posto de Vigília e Nicho Selado. Elas são conteúdo de descoberta e memória, não dungeons. Nesta alpha ainda são inseridas em runtime; uma versão futura pode migrar estruturas consolidadas para worldgen/data-driven quando o layout estiver aprovado em playtest.

## Investigação
O Caderno de Vestígios transforma provas já coletadas em quatro estágios de entendimento, sem fornecer coordenadas. A progressão depende de vestígios, locais descobertos e contramedidas usadas; investigar continua opcional.

## Regra de design
Nem toda perturbação deve terminar no Hóspede. Se o jogador aprender `acontecimento estranho = procurar criatura`, o repertório perdeu a incerteza. A 0.6 usa falsos positivos e mudanças auto-reversíveis para manter dúvida sem depender de jumpscare ou griefing.


# Versão 0.7.0-alpha1 — Aparições e Stalking 2.0

A 0.7 introduz `Aparicao`, uma camada de seleção de pontos reutilizável. O objetivo é separar a intenção do Diretor ("quero uma manifestação aqui/ao redor") do problema geométrico de encontrar uma posição plausível.

## Sistema de candidatos

`Aparicao.Config` controla ângulo, distância, tentativas, cobertura, penumbra, visibilidade depois que o jogador vira e lado preferido. Cada candidato é pontuado por uma combinação de variação aleatória, cobertura, penumbra, visibilidade, distância preferida e penalidade de reutilização recente do chunk.

O sistema nunca escolhe um ponto dentro do cone seguro da tela atual quando `evitarTela=true`. A visibilidade "depois que vira" é testada por uma linha amostrada entre o olho do jogador e a altura de leitura da manifestação.

## Memória de posicionamento

`EstadoJogador.aparicoesRecentes` mantém até 8 chunks usados por manifestações naturais durante a sessão. Não há bloqueio permanente: um lugar apenas perde prioridade temporariamente. Comandos continuam fora dessa memória.

## Integração

O seletor agora é usado pelas aparições genéricas do Diretor e pela cena espacial em área aberta. A cena doméstica que depende de uma janela real mantém seu seletor específico porque precisa de uma âncora física de vidro.

## Intenção de design

Inspirado por padrões públicos de stalking dinâmico: procurar posições plausíveis, preferir cobertura e permitir pequenos fallbacks sem transformar o conteúdo em coordenadas fixas. Nenhum código externo foi copiado.


## v0.8.0-alpha1 — Hóspede que procura

A caça deixou de ser um movimento direto para o alvo. `HospedeEntity` delega a busca a `HospedeBusca`. A classe mantém uma última posição conhecida transitória, recebe confirmações visuais e ruídos discretos de movimento, investiga a área, escolhe pontos de busca e pode desistir.

Estados internos da busca: `ULTIMA_POSICAO`, `INVESTIGANDO`, `PROCURANDO`, `DESISTINDO`.

A criatura continua sendo uma `PathfinderMob` e usa a `Navigation` vanilla para pathfinding. Não foi adicionada SmartBrainLib.

O log de debug pode registrar `BUSCA id=...` com transições, confiança e pontos visitados.

## v0.8.1 — o Olho deixa de ser botão

Na fase 3+, usar o Olho sem criatura e sem vestígio por perto pode chamar uma aparição. Isso continua, com dois freios:

- a chance começa em 40% e cai pela metade a cada uso nos últimos 5 minutos;
- depois de chamar uma aparição, o Olho fica 5 minutos sem poder chamar outra.

Aparições chamadas pelo Olho não contam para a ousadia (`VEZES_VISTO`). Pressão e inquietação continuam subindo.

Log: `OLHO chamou aparicao usosRecentes=N chance=0.NN` e `OLHO nada usosRecentes=N chance=0.NN recarga=sim|nao`.

## v0.8.1 — silêncio de verdade e respiração no sussurro

**Silêncio.** A trégua do Diretor sempre foi "sem eventos": a música e o ambiente do jogo continuavam. Agora o mundo pode emudecer de fato para um jogador: a música e o som ambiente dele são cortados, e os mobs em volta (24 blocos) ficam alguns segundos sem som de fundo. Nada muda no mundo nem fica salvo.

- Quando o Hóspede nasce (fora da tela), isso acontece em 70% das vezes, por 25 s. O jogador costuma perceber o silêncio antes de virar e ver.
- O evento `SINAL` (falso positivo) emudece em 30% das vezes, e os presságios em 20%, por 15 s. É o mesmo aviso, sem criatura: aviso que nunca falha vira dica.
- Não há acorde de susto. O contraste é o silêncio.

**Sussurro.** `SUSSURRO` e `ECO_CHAT` continuam sendo texto, mas agora vêm com uma respiração "dentro da cabeça": o som fica preso ao próprio jogador, então não tem direção, e só ele ouve. É provisório, até existirem as gravações de voz.

## v0.8.1 — vulto distante e regras de lugar

**Vulto distante (evento `VULTO`, fase 2, categoria VISÃO, intensidade 9).** Uma silhueta parada a 48–80 blocos, só de dia e com o jogador ao ar livre. Nasce fora da tela, num lugar com linha livre até o jogador, de preferência num ponto alto (recortada contra o céu). É a "silhueta no alto do morro" da página 3 do diário.

- Some sem som um segundo depois de o jogador mirar nela (cerca de 25° do centro da tela).
- Some também se o jogador chegar a 36 blocos, ou sozinha depois de 30 a 45 s sem ninguém olhar.
- Não conta para a ousadia. Ao ser mirada, em 25% das vezes deixa uma marca que o Olho e o Sino conseguem apontar depois. Se o jogador chega perto, vale a regra normal da Cinza Pálida.
- Longe e curta assim, é negável. Por isso é fraca e pode acontecer mais vezes que as outras aparições (é o que o From The Fog faz).
- É a primeira aparição antes da fase 3. A revelação de perto continua guardada para a fase 3.

**Regras de lugar.**

- De dia, a céu aberto e sem nada na frente, o Hóspede não nasce a menos de 25 blocos do jogador. De perto e inteiro, ele parece só um boneco parado.
- O som de sumiço (quando o jogador chega perto ou encara demais) agora toca só em metade das vezes. Som que sempre confirma o sumiço tira a dúvida de "eu vi mesmo?".

## v0.8.1 — sumir mais rápido ("será que eu vi?")

Pedido do dono depois do primeiro teste: nas aparições de observação dava para focar na criatura e ter certeza de que ela estava ali. O log confirmou: o vulto pedia um segundo inteiro de mira, e a presença pedia 0,75 s de olhar direto. Quem olhava por menos tempo e desviava encontrava a criatura no mesmo lugar ao olhar de novo.

**Vulto distante.** Agora some:

- 2 a 5 ticks (0,1 a 0,25 s) depois de entrar no miolo da tela (30° do centro), ou seja, enquanto o jogador ainda está virando para ele;
- depois de 0,5 a 0,8 s na borda da tela, mesmo sem ser mirado;
- assim que sai da tela depois de ter sido visto. Quando o jogador olha de novo, não há nada.

**Presença (modo de observação).**

- O tempo que ele aguenta ser visto passou a ser 12 + 6 por ponto de ousadia (em ticks; olhar direto conta em dobro). Com ousadia 0 é um relance: 0,3 s de olhar direto ou 0,6 s de canto. Com ousadia 3 é o que era antes; com ousadia 10 chega a 1,8 s.
- Enquanto a ousadia é menor que 4, basta olhar direto e desviar: ele some fora da tela (`SUMIU_NO_DESVIO`). Conta como "foi visto", então a ousadia sobe e, com o tempo, ele passa a ficar.

A progressão continua a mesma ideia do design original (cada vez que é visto, fica mais ousado), só que começa de um ponto bem mais tímido.

**Leitura do avistamento.** A reação mais comum a uma aparição é virar e olhar direto para ela, e esse giro acontece antes de a leitura começar (é ele que põe a criatura na tela). Todo avistamento estava sendo lido como "não reagiu": ensinava ao Diretor que aparições não funcionam e contava para a punição por indiferença. Agora, se o jogador encarou a criatura, o avistamento vale pelo menos 0,35 de confiança.

**Aparições no Rastro.** O Rastro guarda por onde o jogador andou, inclusive a caverna embaixo dos pés dele. Aparições nesses pontos nasciam a 30 blocos de profundidade, onde ninguém vê. Agora o ponto precisa estar a até 12 blocos acima ou abaixo do jogador.

## v0.8.1 — miragens

Uma miragem é um bloco que só um jogador vê. O servidor manda para ele um bloco que não existe; o mundo de verdade não muda. Não há o que restaurar ao fechar o mundo, a construção de ninguém é tocada, e quem está ao lado não vê nada.

O experimento `/sussurros teste miragem` confirmou em jogo (08/10/2026): a tocha falsa ilumina, e some quando o jogador clica nela, porque o próprio jogo reenvia o bloco de verdade. Tocar na coisa faz ela sumir.

O que passou a ser miragem:

- **Luz no fim** (cena `LUZ_NO_FIM`): uma tocha a 14–34 blocos, por 25 a 45 s. Quando o jogador chega a 6 blocos, ela não está mais lá. É a página 19 do diário. Antes era uma tocha de verdade que durava 6 a 14 s.
- **Luz distante** (presságio): continua um brilho curto, de 4 a 9 s, agora sem mexer no mundo.
- **Tocha que aparece** (`LUZ_ERRADA`, tipo `APARECE`): 20 a 45 s; some a 5 blocos.
- **Tocha que muda de lugar** (tipo `MIGRA`): a de origem ainda some de verdade por alguns segundos e volta; a de destino é miragem.
- **Tocha vermelha** (tipo novo, `VERMELHA`): uma tocha do jogador passa a ser de redstone, só para ele, por 25 a 45 s. Volta ao normal quando ele chega a 3 blocos ou clica nela. A luz em volta fica mais fraca.

O que continua mexendo no mundo de verdade, por enquanto: a tocha que pisca, as tochas apagadas ao acordar e as que o Hóspede apaga na caça. Falta saber se, ao esconder uma tocha por miragem, a luz dela some junto.

---
# Versão 0.9.0-alpha1 — "Sentir e ser caçado"

Primeira entrega da expansão descrita no `PLANO-EXPANSAO.md`. As pesquisas que embasam os números estão em `pesquisa/`.

## Sentidos

O servidor passa a enviar a cada jogador, uma vez por segundo, quatro medidas de 0 a 1. O cliente as transforma em apresentação. Nada disso entra em pressão, agenda ou aprendizado.

| Medida | De onde vem | O que o jogador percebe |
|---|---|---|
| `peso` | fase (0 / 0,10 / 0,28 / 0,48 / 0,66) mais até 0,34 da obsessão. Dentro da vela vale 35%; de dia em casa, 80% | A cor do mundo drena em quatro degraus (saturação 0,90 / 0,80 / 0,70 / 0,60) e um fundo grave sobe. Dentro da vela a cor volta |
| `vigia` | Há criatura que vê o jogador sem estar na tela dele, a até 72 blocos. Só 7 em cada 10 manifestações são "sentidas" (decidido pelo ID, sem sorteio). O evento `VIGIA` produz a mesma sensação sem ninguém | Borda escura que respira e uma camada fina de som |
| `caca` | Caçada: 0,2 no aviso; 0,25 longe; de 0,45 a 1 quando ele está a menos de 12 blocos e sem parede no meio; 0 quando ele finge que desistiu | Batimento (de 24 a 11 ticks entre batidas) e três camadas baixas de perseguição |
| `neblina` | Evento `NEBLINA` | A neblina fecha (até 22 blocos no máximo) |

Sem o mod no cliente nada disso existe, e os eventos que dependem só do cliente não são sorteados.

Efeitos pontuais: `PISCAR` (a tela fecha por 8 ticks), `APAGAO` (fecha e segura), `ACORDAR` (abre devagar).

O cliente informa o FOV efetivo e a proporção da janela. Os cones "está na tela" e "é seguro nascer aqui" passam a ser calculados por jogador (`Percepcao`). Vidro não tampa mais a visão que o mod usa, e basta uma de três alturas do corpo aparecer.

Quando ele some na frente do jogador, dissolve em 4 ticks. O vulto distante continua sumindo de um quadro para o outro.

A cor e a borda podem ser desligadas por quem joga: `/sussurros_tela cor nao` e `/sussurros_tela borda nao` (comando do cliente, sem permissão; fica em `config/sussurros-cliente.properties`).

## Eventos novos

| Evento | Fase | Categoria | Intensidade | O que é |
|---|---|---|---|---|
| `ECO_PASSOS` | 1 | SOM | 9 | Por 25 a 40 s, cerca de 70% dos passos do próprio jogador tocam de novo 4 a 7 ticks depois, mais baixos e 1,6 bloco atrás. Para quando ele para |
| `VIGIA` | 1 | MENTE | 7 | A sensação de estar sendo olhado, por 15 a 30 s, sem criatura |
| `NEBLINA` | 2 | AMBIENTE | 13 | A neblina fecha por 60 a 120 s, só a céu aberto. Sem leitura de reação |
| `CANTIGA` | 2 | SOM | 12 | Um pedaço do tema assobiado a 24–42 blocos, de um lugar fora da tela, só para o jogador. **Só existe depois que a Caixa de Música tocou três vezes** (o item chega na entrega seguinte) |
| `PRENUNCIO` | 3 | MENTE | 14 | O aviso da caçada (o mundo emudece, uma luz perto falha, um grave) sem caçada. Só acontece onde uma caçada poderia começar |

O `SUSSURRO` passa a vir com voz (sussurro que não dá para entender) em 70% das vezes.

## A caçada

Regra em uma frase: ele só anda quando você não está olhando, ele ouve o que você faz, e ele não entra na luz da vela.

**Onde começa.** No escuro, com inquietação de 60 ou mais, fora da base (contexto diferente de CASA e a mais de 24 blocos da cama), nunca com o jogador montado ou planando, nunca a menos de 7 blocos de outro jogador. O golpe da sequência de ameaça espera essas condições.

**Estágios**

1. **Aviso (8 a 10 s).** O mundo emudece para o alvo, uma luz a até 9 blocos falha por alguns segundos, um grave baixo. Se ele já aprendeu a cantiga, assobia em 60% das vezes. Ele nasce a 18–26 blocos, atrás, e **não se mexe**.
2. **Perseguição.** Anda quando não é visto. Velocidade de 4,9 blocos/s, subindo 4% por segundo de contato até 6,3 (o jogador anda a 4,3, corre a 5,6 e corre pulando a 7,1); cai 1% por segundo sem contato. Na luz forte (12 ou mais) fica 15% mais lento. Apaga, por miragem, uma luz a até 4 blocos a cada 1,5 s. Os passos dele são ouvidos pelo alvo até uns 24 blocos.
3. **Encarar.** Na tela, ele congela e sabe onde o jogador está. Depois de 4 a 6 s seguidos, a tela do jogador pisca e ele avança até 3 blocos; os piscares seguintes vêm a cada 3 a 5 s. O relógio de contato só anda com ele fora da tela.
4. **Toque.** A 2,4 blocos, com linha de visão, olhando ou não.
5. **Bater.** As duas primeiras vezes o fazem recuar (reaparece fora da tela a 10–14 blocos); da terceira em diante, nada.
6. **Portas.** Abre porta de madeira depois de 1,5 a 3 s mexendo na maçaneta. A caixa de colisão na caça tem 1,9 de altura; o modelo dobra o corpo sob teto baixo.
7. **Atalho.** Com 3 s fora da tela, sabendo onde o jogador está e a mais de 16 blocos, reaparece a 8–12 blocos, fora da tela, de preferência aos lados da direção em que o jogador anda. Recarga de 10 s.
8. **Ouvir.** Além do movimento, quebrar bloco, usar porta e abrir baú a até 16 blocos entregam a posição. Agachado e quase parado é silêncio. A chance de ouvir sobe 10% a cada 10 s sem notícia, até +50%.
9. **Busca.** Ao chegar ao último lugar conhecido, para 2 a 3 s e olha em volta. Os pontos de busca fecham o cerco (raios 12, 8, 5, 4).
10. **Vela.** Com o alvo dentro da zona, ele espera na borda por 20 a 40 s e vai embora. Na fase 4, para quem já usou 4 velas ou mais, em 35% das vezes ele a sopra no meio da espera.
11. **Sem caminho.** Sabendo onde o jogador está, a até 8 blocos no chão (a altura não conta) e sem caminho por 10 s: 2,5 s de batidas e arranhões num bloco ao lado do jogador (o de baixo, se ele estiver no alto), com poeira escura. Quem ainda estiver a 1,9 bloco do centro daquele bloco é pego; quem saiu, ele aparece ali. Cada uso encurta a espera em 2 s (mínimo 4 s) e o aviso em 0,25 s (mínimo 1,5 s); o contador fica na memória.
12. **Fingir.** Em 35% das desistências, uma vez por caçada: fica calado 8 a 15 s e volta.
13. **Tetos.** 45 s de contato, 40 s de busca, 120 s no total.

Na primeira caçada da vida do jogador, o atalho e o "sem caminho" ficam desligados.

**Fim honesto.** Enquanto ele existe, o mundo fica mudo para o alvo. O som só volta de 3,5 a 5 s depois de ele ir embora de verdade. Na falsa desistência o silêncio continua.

## Ser pego

Ele não mata.

1. Tudo emudece, a tela fecha.
2. O que estava na mão fica caído onde ele foi pego, sem prazo para sumir. O lugar vira um vestígio (o Olho e o Sino apontam para lá).
3. O jogador acorda a 20–40 blocos, de preferência num ponto escuro do próprio Rastro. A vida cai 8, nunca abaixo de 4. As luzes em volta aparecem apagadas por um minuto.
4. **A marca:** um coração a menos de vida máxima por captura, até três. Some ao dormir de verdade com uma vela pálida acesa. Volta depois de morrer e renascer.
5. O Diretor recua por 6 a 10 minutos e a próxima sequência de ameaça só é possível 30 a 45 minutos depois. Depois de qualquer caçada, 25 a 40 minutos.

Criatura criada por comando faz tudo isso, menos a marca e os contadores.

## Sons

46 arquivos novos, sintetizados por `ferramentas/sons/gerar_sons.py` (medidos, nunca ouvidos por quem os fez). O tema é uma cantiga em lá menor, valsa, oito compassos: aparece na caixa (limpa, gasta e arruinada), assobiada, cantarolada e, em fragmentos graves, numa das camadas da perseguição. As camadas em loop tocam na categoria "Criaturas hostis", porque o silêncio do mod corta a categoria "Ambiente".

## O mundo deixa de ser tocado pelas luzes

A tocha que pisca, a tocha "levada" na fase 4, as luzes apagadas ao acordar, as das cenas de luz e as que ele apaga na caça passaram a ser miragem. A "luz no fim" procura o lugar primeiro nos pontos do Rastro, o que a faz funcionar dentro de túnel.

## Diário

Três páginas novas (23 a 25 de 25) ensinam, na voz de quem escreveu, que encarar só compra tempo, que ele atravessa o que não tem caminho e como a marca sai.

---
# Versão 0.9.0-alpha2 — "Trocar"

Segunda entrega da expansão: os itens novos e a dívida escondida que os une. As pesquisas que embasam as escolhas estão em `pesquisa/2026-10-08-itens-e-sentidos.md`.

## A Conta

Um número escondido por jogador, salvo com o personagem. É o que permite aos itens serem bons de verdade: o preço não está em cada uso, está em se apoiar demais.

| Uso | Soma |
|---|---|
| Vela, Sino, Fio, Ossos, um bloco de Linha de Cinza, uma carga no Lampião | 1 |
| Isca, Caixa de Música | 2 |
| Olho | 3 |
| Ossos com o desfecho "a conta sobe" | 4 |

- Cai 1 a cada 10 minutos (não cai enquanto uma cobrança está marcada).
- O limite é sorteado a cada ciclo, de 6 a 10. O jogador nunca o vê.
- **Três avisos sem texto**, um por degrau, quando faltam 3, 2 e 1 para o limite: (1) os lampiões a até 16 blocos ficam inquietos e uma ou duas luzes perto falham por alguns segundos; (2) a cantiga toca falhada, ao longe (para quem nunca deu corda na caixa, um sino distante); (3) um zumbido no ouvido e 20 s de sensação de vigia.
- **Estourou:** a cobrança não vem na hora. Vem de 30 a 120 s depois, quando ele já não liga uma coisa à outra. Depois tudo zera e sorteia outro limite.
- **A cobrança vem na moeda do item em que ele mais se apoiou** (uso × peso):

| Item mais usado | Cobrança |
|---|---|
| Vela | A próxima vela dura a metade |
| Olho | No próximo uso, dez segundos de escuridão e ele vem (na fase 3 ou mais) |
| Isca | A próxima isca é ignorada: ele não vem para onde foi chamado |
| Ossos | A próxima jogada é "presença", sem sorteio |
| Sino | O sino toca sozinho três vezes, espaçadas |
| Fio | Três estalos de fio dedilhado, sem rompimento |
| Caixa | A caixa toca sozinha, arruinada, de dentro da mochila |
| Linha | Uma linha a até 16 blocos é gasta de uma vez, até romper |
| Lampião | Todos os lampiões a até 24 blocos ficam com a chama fria por um minuto |

Nunca mata e nunca estraga a construção. Uso por comando de teste não soma.

## Caixa de Música

Item com 12 voltas de corda. É **deixada para o jogador na passagem para a fase 2** (como a página e o Olho); a receita só aparece depois de ele ter uma.

- Toca o tema por uns 20 s, no lugar onde ele deu corda (é som do mundo: quem está perto ouve). Enquanto toca, não dá para dar corda de novo.
- A até 10 blocos da caixa, a inquietação cai 3 por segundo.
- Se o Hóspede existe e está a até 20 blocos da caixa, depois de 5 s **algo cantarola junto, dentro da cabeça do dono**: confirma que ele está por ali, não onde.
- **Na caçada é isca:** ele vai até a música. Funciona bem na primeira vez da caçada (certeza 0,7), mal na segunda (0,45), quase nada depois (0,2); e não adianta se ele acabou de ver o jogador.
- **O preço é ele aprender.** Com 3 usos a caixa soa gasta e passam a existir o evento `CANTIGA` e o assobio no aviso da caçada. Com 6, soa arruinada. A corda arrebenta no 12º uso.

## Linha de Cinza

Cinza Pálida usada no chão vira um bloco fino (`cinza_espalhada`). A faixa atravessa na frente de quem a põe, ou continua a linha vizinha.

- **Na caçada ele não cruza uma linha que segura.** Vale a linha que está no caminho dele (o bloco que ele pisaria); linha ao lado não o segura. A cada tentativa ela perde um estágio, arrasta e o deixa parado 3 s: intacta, riscada, gasta, rompida. Quem está atrás da linha não é pego enquanto ela dura. Cada bloco de linha no caminho compra uns nove segundos; ele também não a salta no piscar.
- **Na porta:** a batida e a maçaneta, se há linha segurando a até 2 blocos da porta, não mexem na porta; gastam um estágio da linha.
- **Dormindo:** na fase 2 ou mais, em 45% das manhãs uma linha a até 12 blocos da cama amanhece um estágio mais gasta, com uma pegada de cinza do lado de fora, virada para ela. Sem som. A pegada some em um dia de jogo.
- Aparições não nascem a menos de 2 blocos de uma linha que segura.
- Só a linha intacta devolve a Cinza ao ser quebrada.

O mesmo bloco, no estado `pegada`, é a marca que ele deixa. Não segura nada.

## Lampião Pálido

Bloco de luz (no chão ou pendurado) que queima Cinza Pálida: até 3 cargas, em média uma a cada sete minutos. Sai da bancada aceso e com uma carga. Cinza na mão recarrega; mão vazia apaga ou reacende.

| Chama | Luz | Quando |
|---|---|---|
| calma | 12 | nada por perto |
| inquieta | 8 | ele a até 20 blocos |
| fria | 4 | ele a até 9 blocos |
| apagada | 0 | ele passou a até 4 blocos, ou acabou a cinza |

Vale para qualquer Hóspede, visto ou não: é o "sinal preso a um objeto" que a pesquisa pede no lugar de um medidor na tela. Olha em volta uma vez por segundo, e cinco vezes por segundo quando já há algo perto. Apagado por ele, só reacende na mão.

## Tigela de Oferenda

Bloco com um item dentro (aparece deitado na tigela). O jogador põe uma coisa; uma vez por noite, depois da meia-noite e sem ninguém olhando, ele decide. Quem dorme a noite inteira tem a decisão tomada ao acordar.

- **Chance de aceitar:** 45% + 15% × valor (comida 1, Cinza Pálida 2, ouro/diamante/esmeralda/ametista/maçã ou cenoura dourada 3, o resto 0,5), até 92%. Repetir o mesmo item multiplica por 0,6 a cada vez.
- **Aceita:** o item some, ficam cinzas na tigela e até três pegadas saem dela para o lado oposto ao da cama. A inquietação cai 30, a obsessão cai 3 × valor, e **até amanhecer nada começa enquanto o jogador está em casa**. Fora de casa tudo continua.
- **Depois de duas aceitas**, em 25% das vezes ele deixa algo na tigela: uma página do diário enquanto houver, depois cinza ou um osso.
- **Recusada:** nada muda.
- **Afronta:** oferecer Vela, Olho, Sino, Fio ou Isca. Ele não leva, e por quatro minutos a batida e a porta ficam quatro vezes mais prováveis.
- **Desfeita:** depois de três noites seguidas aceitas, uma noite com a tigela vazia tem o mesmo efeito da afronta.

Não pode virar "seja bonzinho e está salvo": a trégua vale só dentro de casa e só até amanhecer.

## Ossos de Agouro

Consumível. Três ossos caem no chão à frente e ficam 15 s. O desfecho não é dito; lê-se em como caíram.

| Desfecho | Peso | O que faz | Como caem |
|---|---|---|---|
| nada | 25 | nada | espalhados |
| silêncio | 15 | o mundo emudece por 25 s | três lado a lado |
| trégua | 15 | dez minutos sem nada novo começar | em estrela, as três pontas se tocando |
| apontam | 15 | alinham-se para o vestígio mais próximo (ou para ele) | em fila |
| presença | 15 | ele aparece em 3 a 6 s | dois cruzados, um afastado |
| amigo | 10 | ele aparece para o jogador mais próximo | em fila, apontando para o amigo |
| conta | 5 | a Conta sobe 4 | só dois: o terceiro se desfaz em cinza |

Cada jogada no mesmo dia tira 6 pontos de cada desfecho bom e os dá à presença (até 12). Sem alvo, "apontam" vira "nada" e "amigo" vira "presença".

## Ajustes nos itens antigos

- **Vela:** enquanto a zona dura há uma vela acesa de verdade no chão (se couber). Tirar o bloco acaba com a zona; soprada na caçada, o bloco some junto.
- **Fio:** vibra antes de romper. Com a coisa a até 7 blocos da borda, range baixo a cada 2 s e o tom sobe conforme ela chega. Dá antecedência e distância, não direção.

## Chamas Pálidas

Não é evento. Da fase 2 em diante, uma tocha do próprio jogador (duas na fase 4; mais uma com obsessão de 60 ou mais) passa a queimar como tocha de almas, só para ele, por 2 a 5 minutos. A troca só acontece com a tocha fora da tela, a até 24 blocos; dentro da vela nenhuma empalidece. É miragem: clicar nela a desfaz.

## Receitas

Aparecem sozinhas no livro de receitas do jogo: todas as que levam Cinza Pálida, quando ele pega a primeira cinza; a da Vela, com a primeira cinza ou a primeira página; a da Caixa, quando ele tem uma caixa.

| Item | Receita (sem forma) |
|---|---|
| Lampião Pálido | lanterna + cinza pálida + resina |
| Tigela de Oferenda | tigela + bola de argila + cinza pálida |
| Ossos de Agouro (2) | 2 ossos + linha + cinza pálida |
| Caixa de Música | bloco musical + pepita de ferro + cinza pálida |

## Diário

Três páginas novas (26 a 28 de 28) ensinam a linha na soleira, a oferenda e o que acontece ao esquecê-la, e a caixa que ele aprende.

---
# Versão 0.9.0-alpha3 — "O Véu"

Terceira entrega da expansão. É o "eu vi isso mesmo?" do mod, e o ensaio, no mundo normal, das camadas que a dimensão vai usar.

## O Véu

Evento `VEU` (fase 3, categoria MENTE, intensidade 20). Dura de 30 a 60 s. O jogador não sai do lugar.

1. A tela pisca (12 ticks). O mundo troca com a tela fechada: quando abre, já está tudo diferente.
2. **O que ele sente:** a cor é a do Avesso, a neblina fecha (0,85), a música e o som ambiente somem, a camada sonora do Avesso sobe.
3. **O que deixa de aparecer:** tudo o que é entidade, menos o Hóspede. Bichos, monstros, itens caídos, quadros, suportes de armadura e os outros jogadores. Eles continuam lá de verdade (dá para esbarrar, ser atacado, falar no chat); só não são desenhados para ele.
4. **O que está errado em volta**, tudo por miragem: até 16 luzes a 14 blocos aparecem apagadas; uma porta fechada, fora da tela, aparece aberta; um vidro de janela, fora da tela, falta.
5. **Em metade das vezes**, a partir do meio, ele está parado de lado, a 12–17 blocos, dentro do que a neblina deixa ver. Fica até o fim e some com o Véu.
6. Outra piscada, e tudo voltou.

Regras:

- Só existe com o mod no cliente. Nunca abre com criatura presente, dentro da vela ou com o jogador em perigo de verdade (pouca vida, dano recente, caindo, na água, dormindo).
- Enquanto dura, nada novo começa: nem evento, nem cena, nem caçada.
- **Acender uma Vela Pálida o rasga na hora** (a cor, o som e as luzes voltam).
- De 30 a 50 minutos entre um e outro. Raro de propósito: repetido, vira efeito especial.
- Não lê reação, não conta para o aprendizado; conta para o ritmo como qualquer evento.
- Nada muda no mundo de verdade e quem está ao lado não vê nada.

## O primeiro mixin

`EsconderNoVeuMixin` (cliente) entra em `EntityRenderDispatcher.shouldRender` e responde "não" para as entidades escondidas. Não há evento da Fabric para isso, e esconder pelo servidor exigiria mexer no rastreamento de entidades. É o único mixin do mod; se uma versão nova do jogo mudar esse método, o jogo não abre e o job de fotos acusa.

## De olhos fechados

Mudança geral no cliente: quando a tela está fechada (piscada ou apagão), a cor, a neblina e a borda pulam direto para o valor novo em vez de deslizar. Vale também para o apagão da captura: ao abrir os olhos, o mundo já está como vai ficar.

---
# Versão 0.9.0-alpha4 — "Lugares"

Quarta entrega da expansão: duas coisas que o mod ergue no mundo (`Erguidos`). Só blocos do próprio jogo, só em lugar vazio, e só com ninguém olhando.

## A Soleira

Uma porta de abeto sozinha na paisagem, entre dois batentes de tijolo de pedra (liso, musgoso e rachado misturados), com uma verga em cima. Uma por jogador.

- **Quando aparece:** fase 2 ou mais, com 12 chunks visitados; a cada 20 s, 20% de chance. De 60 a 100 blocos do jogador, na superfície, fora da tela, a mais de 60 blocos da cama e da porta de casa.
- **Atravessar** (passar pelo vão, de um lado para o outro): a primeira vez responde com um grave baixo, só para ele; a segunda, com cinco segundos de silêncio; **a terceira abre o Véu**, a porta bate atrás dele e a soleira descansa até o dia seguinte. A contagem não vence com o tempo.
- Se o Véu não pode abrir naquela hora (criatura presente, vela acesa), a contagem fica em dois e a próxima travessia tenta de novo.
- É o único jeito de o jogador **escolher** ver o Véu. O intervalo de 30 a 50 minutos do Véu sorteado não vale para ela.
- Desmontada pelo jogador, ela é esquecida e outra pode nascer em outro lugar.

## O Boneco

Uma estaca de cerca, um fardo de feno e uma abóbora esculpida, com o rosto virado para a cama do jogador.

- **Quando existe:** fase 2 ou mais e uma cama conhecida.
- **A cada noite está mais perto**, sempre vindo da mesma direção (sorteada no começo do ciclo): a 48, 38, 29, 21, 14 e 9 blocos da cama. Fica duas noites nos 9 blocos e some. Volta de 5 a 8 dias depois, de outro lado.
- **Derrubar não resolve:** na noite seguinte está de pé, um passo adiante. O que o jogador tirou fica com ele.
- **Onde pisa:** só chão natural (terra, grama, areia, pedra, cascalho, neve, terracota), a céu aberto, com três blocos livres. Nunca em piso construído nem em telhado. Sem lugar válido naquela noite, fica onde está.
- **Quando anda:** depois das 20h do jogo, com o lugar velho e o novo fora da tela; ou ao acordar, para quem dormiu a noite inteira.
- **O que faz:** nada, até chegar. Nas noites em que está a 9 blocos, a obsessão sobe 2 por noite.

É o único elemento do mod que o jogador acompanha de um dia para o outro sem nenhum evento.

---
# Versão 0.9.0-alpha5 — "O Avesso"

Quinta entrega da expansão: a dimensão. O jogador nunca escolhe ir; é levado, fica pouco e volta ao ponto de onde saiu.

## O lugar

Dimensão `sussurros:avesso`. Usa o ruído de terreno do mundo normal com a mesma semente, então os morros, os rios e as cavernas são os mesmos, nas mesmas coordenadas. O bioma é um só (`sussurros:avesso`), morto: sem árvore, sem planta, sem bicho, sem minério, sem estrutura; grama e água cinzentas; céu parado, sem sol nem lua; pouca luz. Na superfície tudo é grama e terra, mesmo onde no mundo normal é deserto ou neve.

No cliente valem as camadas que o Véu já usava: a cor do Avesso, a neblina perto, a camada sonora própria e nenhuma música.

## A cópia

Na chegada, a região em volta do jogador (29 x 16 x 29 blocos) é copiada do mundo normal para as mesmas coordenadas. É a casa dele, vazia e apagada:

- tudo o que dá luz some (tochas, lanternas, fogo) ou vira pedra (blocos de luz inteiros); fornalha e fogueira ficam apagadas; lava parada vira obsidiana;
- portas de madeira ficam abertas;
- baús e outros recipientes vêm vazios; geradores de monstros não vêm; nenhuma entidade é copiada (quadros, suportes, bichos);
- a cópia sobrescreve o que houver lá, inclusive o que sobrou da visita anterior.

**A cada visita a cópia erra mais:** da segunda em diante faltam blocos de parede à altura do jogador, dois a mais por visita (até seis).

O mundo de verdade não é tocado.

## Lá dentro

- Não se quebra bloco, não se põe bloco e não se abre recipiente (senão a cópia viraria fábrica de itens). Portas, alçapões e porteiras funcionam.
- **Primeira visita:** só o lugar. Nada acontece. De 45 a 75 s.
- **Da segunda em diante:** depois de vinte segundos ele está lá, de lado, parado, a 10–16 blocos, até o fim. De 60 a 150 s. Ele não caça lá (por enquanto).

## Sair

Sempre é "acordar": apagão, e ele está de volta no ponto de onde saiu, com tudo o que tinha.

- o tempo da visita acaba;
- ele se afasta mais de 40 blocos do ponto de chegada;
- ele clica na cópia de uma cama;
- ele leva um dano que mataria (a vida fica em dois corações).

Depois de voltar: por 2 a 5 minutos, uma ou duas luzes da casa de verdade aparecem apagadas (miragem), e o Diretor o deixa em paz por dois minutos.

## Entrar

Nesta versão, só dormindo: da fase 3 em diante, ao deitar, 15% de chance na primeira vez e 8% nas seguintes, com pelo menos três dias de jogo entre uma visita e outra. Dois segundos depois de deitar, em vez de dormir, ele é levado. A noite não passa. Só com o mod no cliente.

Ficou para depois: ser levado numa parte das capturas, e a caçada lá dentro.

## Segurança

- Enquanto ele está lá, a Memoria guarda `avesso_dentro` e o ponto de volta. Se o jogo fechar com ele dentro, ao entrar de novo ele é devolvido no primeiro segundo.
- Num mundo em que a dimensão não exista, ninguém é levado.
- Em criativo e espectador as travas de lá não valem (para quem quiser olhar o lugar por comando).
