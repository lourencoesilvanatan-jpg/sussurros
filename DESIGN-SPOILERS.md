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
5. **Em metade das vezes**, a partir do meio, ele está parado de lado, a 12–17 blocos, dentro do que a neblina deixa ver. Vale a regra de sempre do mundo normal: visto por um instante, ele some. Se não for visto, some com o Véu.
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

---
# Versão 0.9.0-alpha6 — "O Avesso, segunda parte"

Fecha o que a alpha5 deixou para depois. Corrige também uma frase dela: na segunda visita ele usava o modo de observar do mundo normal e sumia ao ser visto por meio segundo. Agora, do outro lado, ele tem modo próprio.

## Como ele é do outro lado

Modo `AVESSO` da criatura. As regras de cá não valem: olhar para ele não o faz sumir nem o segura, e a zona da vela não o afasta.

| Visita | O que há |
|---|---|
| 1 | Só o lugar. Nada acontece |
| 2 | Aos 20 s ele está lá, de lado, a 10–16 blocos, parado, olhando. Fica até o fim |
| 3 em diante | O mesmo, e aos 45 s **ele vem**: 2,2 blocos por segundo (metade do passo do jogador), sem parar, olhado ou não |

Encostar (2,3 blocos, com linha de visão) é acordar, e mais nada: sem dano, sem marca. Quem quiser ficar até o fim do tempo tem de andar; quem se afastar mais de 40 blocos também acorda.

## A medida errada

Da terceira visita em diante a cópia tem uma fatia repetida: dois blocos a leste do ponto de chegada, um plano inteiro aparece duas vezes, e tudo depois dele está um bloco mais longe. A sala em que ele chega ficou um bloco mais comprida. Somado aos blocos de parede que faltam (dois a mais por visita, até seis).

## Ser levado na captura

Depois da primeira visita (que é sempre pela cama), 35% das capturas não deslocam o jogador: ele acorda do outro lado, no ponto onde foi pego, e volta depois para esse mesmo ponto. No máximo uma vez por dia de jogo, e só com o mod no cliente.

O resto da captura acontece igual: o item da mão fica caído no mundo normal, a vida cai, a marca entra, o Diretor recua. A visita conta como qualquer outra (a terceira já tem a medida errada e ele vindo).

---
# Versão 0.9.0-alpha7 — duas limpezas

## A caçada que ficou devendo

Antes, sair do jogo no meio de uma caçada, ou fugir para longe (élitro, cavalo, barco, portal), acabava com ela sem consequência. Pior: a mais de 128 blocos de qualquer jogador o próprio jogo apagava a criatura sem avisar o mod, e a caçada sumia sem registro.

Agora a caçada não é cancelada, só adiada:

- **Fica devendo** quem sai do jogo com uma caçada em curso (`DESLOGOU`), quem fica a mais de 110 blocos dele (`FUGIU_LONGE`) e quem troca de dimensão (`TROCOU_DE_MUNDO`). Caçada de teste não deixa dívida. Morrer ou ser pego não é fugir.
- **Ela volta** um a dois minutos depois de ele entrar no jogo ou de ter fugido, quando as condições de sempre valem: no escuro, fora da base (nunca na casa nem a 24 blocos da cama), fora da vela, sem estar voando, montado ou caindo, sem um amigo colado.
- O aviso é o de sempre (os oito a dez segundos com ele parado). Não há castigo extra: é a mesma caçada.
- Só uma dívida por vez. Ela fica guardada com o personagem.

A criatura deixou de ser apagada pelo jogo quando fica longe de todos: quem decide quando ela some é ela.

## Som só para quem é assombrado

Dezenove sons que ainda tocavam para qualquer um por perto passaram a tocar só para o alvo: os presságios e as perturbações de ambiente, os sons das quatro cenas compostas, o som atrás do jogador, a respiração, os sinais falsos e as respostas distantes do sino.

Continuam tocando para todos, de propósito, porque aconteceram de verdade no mundo: a caixa de música, a tigela, a linha de cinza que arrasta, o fio que rompe, os itens quando usados e os sons que saem do corpo da criatura (que os amigos também veem).

---
# Versão 0.9.0-alpha8 — "A Casa do Vigia"

A casa de quem escreveu o diário (`CasaDoVigia`). Uma por jogador.

- **Quando aparece:** fase 2 ou mais, com 20 chunks visitados; a cada 20 s, 15% de chance. De 90 a 140 blocos do jogador, na superfície, fora da tela, a mais de 90 blocos da cama e da porta dele, em terreno livre.
- **Como ele a encontra:** do lado de fora há uma fogueira acesa sobre um fardo de feno. A fumaça sobe alto e se vê de longe.
- **O que é:** uma cabana de abeto de 7 x 5 por fora, com porta ao sul e três janelas. Dentro: a cama dele; um baú (uma Caixa de Música, duas páginas do diário, quatro cinzas, dois Ossos de Agouro, duas velas, pão e um livro); a Tigela de Oferenda com cinzas; o Lampião Pálido apagado e sem cinza, pendurado no teto; uma placa na parede do fundo com 37 riscos, em grupos de quatro; e, no chão diante da porta, a Linha de Cinza dele: gasta de um lado, riscada do outro e rompida no meio.
- **Não há ninguém.** Nenhum evento é disparado pela casa. O que ela conta, conta pelos objetos: ele usava as mesmas coisas que o jogador está aprendendo a usar, e a linha dele rompeu.
- **Ela muda quando o jogador vai embora.** Entrar (chegar a 5 blocos do centro) conta uma visita. Com ele a mais de 40 blocos, uma coisa muda, e a placa ganha um risco:
  - depois da primeira visita, a fogueira está fria e a porta, aberta;
  - depois da segunda, o lampião está aceso e a tigela, limpa;
  - depois, a porta alterna entre aberta e fechada, e os riscos continuam.
- As mudanças só mexem no que ainda é o que a casa pôs. O que o jogador tirou ou trocou fica como ele deixou.

---
# Versão 0.9.0-alpha9 — "O baralho"

O último item do plano da expansão. É o que faz cada mundo ter a sua ordem.

Cada jogador tem um baralho de doze cartas, embaralhado com a semente do mundo, a identidade dele e o número do ciclo. Uma carta é virada a cada 40 a 90 minutos de jogo, da fase 2 em diante. Três são "nada". As outras nove não inventam coisa nova: cada uma adianta, fora de hora, algo que o mod já sabe fazer.

| Carta | O que adianta |
|---|---|
| `VEU` | O Véu abre agora, sem respeitar o intervalo (fase 3 ou mais, com o mod no cliente) |
| `SOLEIRA` | A Soleira é erguida perto, a 30–55 blocos, se ainda não existe |
| `BONECO` | O Boneco começa a vir esta noite; se já vem, pula um passo |
| `SONO` | Na próxima vez que ele deitar, é levado ao Avesso, sem sorteio e sem o intervalo de três dias (fase 3 ou mais) |
| `CACADA` | Uma caçada fica devendo: vem quando as condições de sempre valerem (fase 4) |
| `PRESENTE` | Na próxima manhã há algo ao lado da cama, sem sorteio: uma página, ou cinza se as páginas acabaram |
| `CHAMAS` | Por dez minutos, até três tochas a mais empalidecem |
| `CASA` | A Casa do Vigia é erguida agora, se ainda não existe |
| `LINHAS` | Na próxima manhã, a Linha de Cinza mais perto da cama amanhece rompida |

- Uma carta que não vale naquela fase, ou cujo lugar já existe, é gasta como "nada".
- Uma carta que não cabe agora (o Véu com a vela acesa; sem terreno para a casa) espera um minuto e tenta de novo, até dez vezes.
- Nenhuma é virada com criatura presente, com o Véu aberto ou com o jogador em perigo de verdade.
- Esgotado o baralho, ele é embaralhado de novo, em outra ordem.

Quem lê o código sabe quais são as cartas, mas não a ordem delas num mundo: ela só existe depois que o mundo existe.

---
# Versão 0.9.0-alpha11 — depois da revisão externa

Uma revisão de fora (`pesquisa/2026-10-09-revisao-externa-0.9.md`) propôs que a próxima fase seja de medir e cortar, não de acrescentar. Esta versão faz o que dela era conferência e ferramenta.

- **Sons do jogo usados pela assombração passam a ser só do alvo.** A alpha7 tinha convertido os sons próprios do mod e deixado passar doze sons do próprio jogo: os passos fantasmas, o passo único, o eco de ação, a porta e as batidas do sinal falso, a batida na porta, o som atrás, a porta do presságio e o passo da cena da casa. Um teste novo põe dois jogadores lado a lado e confere que o segundo não recebe nada disso, e que recebe a caixa de música (que é do mundo).
- **O fundo cai quando o Diretor recua.** No estado `RECUANDO` o "peso" vale a metade: a cor volta um pouco e o fundo grave quase some. Antes o fundo só sabia da fase e da obsessão, e seguia tocando no silêncio depois do pico.
- **Analisador de log** (`ferramentas/log/analisar.py`): transforma o `sussurros-debug.log` num relatório com fases, ritmo (saídas perceptíveis por hora e intervalo entre elas), criatura, tema, reações, itens e Conta, caçadas e o que não coube.

O primeiro número que o analisador deu, no log de 08/10 (antes da expansão): 45 saídas perceptíveis por hora, mediana de 56 s entre uma e outra, 34 manifestações da criatura por hora. É denso. O passo seguinte é o orçamento de atenção por jogador.

---
# Versão 0.9.0-alpha12 — o orçamento de atenção

O primeiro risco apontado pela revisão externa, confirmado pelo log de 08/10: cada sistema tinha o seu relógio e ninguém decidia a soma. Naquela sessão, antes da expansão, a soma já dava 45 saídas perceptíveis por hora, com mediana de 56 s entre uma e outra.

## Como funciona (`Atencao`)

Um saldo por jogador, de no máximo 60 pontos, que volta devagar. Tudo o que o mod empurra para o jogador gasta desse mesmo saldo.

| Saída | Custo |
|---|---|
| evento do Diretor | 12 + a intensidade dele (de 16 a mais de 40) |
| presságio | 14 |
| perturbação de ambiente | 20 |
| começo de uma cena composta | 36 |
| aviso da Conta | 10 |
| o Véu aberto por uma carta | 32 |
| caçada devida | 50 |

| Fase | O saldo volta (por segundo) | Respiro mínimo depois de uma saída |
|---|---|---|
| 0 | 0,06 | 120 s |
| 1 | 0,08 | 110 s |
| 2 | 0,10 | 90 s |
| 3 | 0,14 | 60 s |
| 4 | 0,20 | 45 s |

- **Para começar** algo novo é preciso ter saldo para o mais barato e ter passado o respiro. Vale para o sorteio normal do Diretor, o piso, os presságios, as perturbações, o começo das cenas, os avisos da Conta e as cartas do baralho.
- **O que já começou termina.** Elos de cadeia, a sequência de ameaça e os passos de uma cena não esperam: só gastam. O saldo pode ficar negativo (até -60), e aí o silêncio seguinte é mais longo. Depois de uma cena densa vem uma pausa de verdade.
- **Com o Diretor recuando nada começa e o saldo não volta.** A trégua passou a ser trégua para todos os sistemas.
- **Ficam de fora:** o que é contínuo (cor, fundo sonoro); a resposta direta a uma ação do jogador (usar um item, atravessar a Soleira); e o que ele só encontra se olhar (as chamas pálidas, o boneco, a tigela, a casa).
- Comando de teste não gasta nem espera.

## Ele vê de perto (correção na caçada)

A sessão sintética reproduziu aqui o teste de captura que falhava de vez em quando só no GitHub, e o log mostrou um defeito de verdade: `CACA ... FIM motivo=PERDEU_RASTRO ... dist=3.7`. Ele chegava ao último lugar conhecido (a tolerância de "cheguei" é 2,8 blocos), parava a mais de 2,4 blocos do jogador (o alcance do toque), olhava em volta, "não achava ninguém" e ia embora, com o jogador imóvel à vista.

Agora ele também vê: a até 8 blocos e sem nada no meio (3,5 blocos se o jogador estiver agachado), sabe onde o jogador está e vai direto até ele. Ficar imóvel continua escondendo quem tem uma parede entre os dois, e quem está agachado a mais de três blocos e meio. Na busca, o motivo aparece como `VIU`.

## Exposição (só medida)

Cada manifestação passa a registrar quantos ticks ficou na tela do alvo, e quantos deles a menos de 25 blocos com luz 8 ou mais (`EXPOSICAO id=... modo=... naTela=14t perto=0t`). O analisador soma por hora. Ainda não há teto: primeiro o número de uma sessão de verdade.

## Três ritmos

`/sussurros ritmo calmo|padrao|intenso` (sem argumento, mostra o atual). Vale para o servidor inteiro e fica guardado em `config/sussurros-ajustes.properties`. O calmo multiplica a volta do saldo por 0,7 e alonga o respiro; o intenso multiplica por 3 e encurta o respiro a um terço, o que fica perto do que o mod fazia antes. É o único ajuste pensado para o dono mexer, e o nome não entrega nada.

## Medição

A sessão sintética (`TestesDeSessao`, desligada no dia a dia) põe dois jogadores de mentira andando em campo aberto por uma hora de jogo, um começando na fase 2 e outro na fase 4, com o relógio do servidor acelerado. O log passa pelo analisador.

Três execuções em 09/10/2026 (saídas perceptíveis por hora; mediana e menor intervalo entre uma saída e a seguinte):

| Jogador de mentira | Sem o limite (1 h) | Com o limite, primeiros números (1 h) | Com o limite, números atuais (50 min) |
|---|---|---|---|
| começa na fase 2 | 15 por hora; na fase 2, mediana de 99 s e menor de 22 s | 35 por hora; na fase 2, mediana de 128 s e menor de 60 s | 10 por hora; na fase 2, mediana de 153 s e menor de 90 s |
| fase 4 | 33 por hora; mediana de 46 s e menor de 0 s (duas coisas no mesmo segundo) | 16 por hora; mediana de 84 s e menor de 31 s | 18 por hora; mediana de 89 s e menor de 45 s |

Para comparar, a sessão de verdade de 08/10 (antes da expansão, fases 0 a 3): 45 por hora, mediana de 56 s, menor de 7 s.

A referência da revisão externa era mediana de 180 s ou mais na fase 2 e de 90 s ou mais na fase 4. A fase 4 está no alvo; a fase 2 ficou perto, com poucos intervalos medidos. Com um jogador de verdade, que puxa o Diretor para estados mais ativos, quem passa a mandar é o saldo: na conta, cerca de 220 s entre saídas na fase 2 e 110 s na fase 4.

Limite da medição: o jogador de mentira não reage, e o Diretor o trata como alguém indiferente (fica a maior parte do tempo observando). Um jogador de verdade puxa o Diretor para estados mais ativos, e aí é o orçamento que segura. As execuções variam bastante entre si, porque o Diretor sorteia. O que a sessão garante é a forma: acabaram as rajadas, e o intervalo mínimo é o respiro.

---
# Versão 0.9.0-alpha13 — o orçamento de atenção, consertado

## O que deu errado na alpha12

A primeira sessão de verdade com a expansão (09/10/2026, mundo novo, 55 minutos, até a fase 3) teve **cinco eventos do Diretor, nenhuma aparição da criatura e a obsessão em 100**. O que o jogador viu foi quase só ambiente (nove presságios) e os avisos da Conta (oito). Ele disse depois que o jogo parecia parado, e tinha razão.

A causa foi o orçamento de atenção da alpha12. Todos os sistemas disputavam um saldo só, em pé de igualdade, e o mais barato ganhava sempre: o presságio (14) e o aviso da Conta (10) gastavam o saldo assim que ele chegava lá, e o Diretor, que precisava de 16 ou mais, nunca alcançava. Em 55 minutos houve cinco sorteios de evento, quatro deles pelo piso.

A sessão sintética não pegou o defeito porque eu olhei só os intervalos entre saídas, e não o que estava saindo. E o jogador de mentira não reagia nem usava itens, então o Diretor mal competia.

## O conserto

- **Dois saldos, um respiro.** O Diretor (eventos, cenas, cartas, caçada) tem o seu saldo. O ambiente (presságios e perturbações) tem outro, menor e mais lento. Nenhum gasta o do outro. O que os une é o respiro: depois de qualquer saída, os dois esperam.
- **Os avisos da Conta saíram do orçamento.** São consequência do que o próprio jogador fez com os itens. É como era até a alpha11.
- **Custos mais baixos e mais parecidos.** Um evento do Diretor custa 10 mais metade da intensidade (de 12 a 25, e não de 16 a mais de 40): evento forte deixou de ser quase impossível de pagar. Cena, 24; caçada devida, 40.

| Fase | Saldo do Diretor (por segundo) | Saldo do ambiente (por segundo) | Respiro comum |
|---|---|---|---|
| 0 | não há eventos | 0,05 | 90 s |
| 1 | 0,05 | 0,03 | 75 s |
| 2 | 0,065 | 0,025 | 60 s |
| 3 | 0,10 | 0,025 | 45 s |
| 4 | 0,16 | 0,025 | 30 s |

O Diretor guarda até 60 pontos e o ambiente, até 30. O que já começou (elo de cadeia, sequência de ameaça, passos de cena) continua sem esperar.

## Medição

O que se olha agora é a **mistura**: de onde veio o que o jogador percebeu, e quantas vezes a criatura se manifestou. Por hora de jogo:

| Sessão | Total | Eventos do Diretor | Ambiente | Conta | Manifestações da criatura |
|---|---|---|---|---|---|
| De verdade, 09/10, alpha12 (fases 0 a 3, 55 min) | 25 | 5 | 11 | 9 | **0** |
| Sintética, alpha13, começa na fase 2 | 27 | 23 | 4 | 0 | 6 |
| Sintética, alpha13, fase 4 | 40 | 32 | 5 | 0 | 8 |
| Sintética, alpha13, começa na fase 2 (outra execução) | 17 | 11 | 6 | 0 | 2 |
| De verdade, 08/10, antes do orçamento (fases 0 a 3, 1h10) | 45 | — | — | — | 34 |

Na execução da fase 4 houve, além dos eventos, um Véu e uma sequência de ameaça completa (espreita, caçada, captura), seguida da trégua de sempre: quase nove minutos de silêncio e mais cinco até o primeiro evento. É o maior silêncio da tabela de intervalos, e é de propósito.

Intervalo entre uma saída e a seguinte (mediana), na primeira execução: 81 s na fase 2, 130 s na fase 3 e de 56 a 73 s na fase 4. A referência da revisão externa era 180 s ou mais na fase 2 e 90 s ou mais na fase 4. Fiquei abaixo dela de propósito: a única opinião de quem jogou, até aqui, é que estava parado. Quem achar demais tem o `/sussurros ritmo calmo`.

Limites desta medição:

- O jogador de mentira reage sempre igual e não usa itens. Os avisos da Conta, que na sessão de verdade foram um terço do que o jogador percebeu, não aparecem nas sintéticas.
- **Os números variam muito de uma execução para outra** (17 e 27 por hora com os mesmos valores). Duas execuções não fecham número nenhum; servem para ver que o Diretor voltou a ser a voz principal e que a criatura aparece.
- Uma execução anterior, com a volta do saldo um pouco mais alta ({0, 0,06, 0,075, 0,12, 0,19}), deu 31 e 49 por hora: a fase 4 passava do que havia antes de o orçamento existir (45). Por isso os valores atuais são um pouco mais baixos.
- Outra execução foi a que mostrou a criatura parada no tempo (abaixo): o jogador da fase 4 ficou com 13 saídas por hora porque o Diretor esperou por ela 52 minutos.

## A criatura parada no tempo

Achado pela sessão sintética, não pela sessão de verdade. Um vulto nasceu a 71 blocos do jogador, num chunk que estava carregado mas fora da distância de simulação. Ali o mundo existe e não anda: a criatura não envelhece e não some. Como o Diretor não começa nada enquanto ela existir, ele ficou mudo por 52 minutos.

Num jogo de um jogador só isso é raro. Num servidor de amigos, onde a distância de simulação costuma ser de 4 a 6 chunks, e com quem viaja depressa, não é.

- O vulto deixou de nascer onde o mundo não anda (`level.isPositionEntityTicking`, além de `isLoaded`).
- O Diretor vigia o relógio da criatura (`tickCount`) uma vez por segundo. Se não mudou em cinco segundos, ela é tirada, com a linha `HOSPEDE ... sumiu motivo=PARADA_NO_TEMPO`. Se era uma caçada de verdade, fica devendo, como quando o jogador foge para longe.

## O que mais a sessão de verdade mostrou (sem mexer em nada)

Ficam anotados. Uma sessão só, e com comandos de teste no meio, não justifica mudar número.

- **Os lugares e os itens chegaram antes da criatura.** A Soleira foi erguida aos 24 minutos e a Casa do Vigia aos 25 (logo depois da fase 2, como está escrito), e ele visitou a Casa aos 30, com tudo o que há dentro. Somando as três estruturas que ele gerou por comando, teve meia dúzia de itens na mão sem nunca ter visto do que eles tratam. Cheguei a subir as exigências das duas (fase 3, mais chunks, chance menor) e desfiz antes de enviar: com a queixa dele sendo "parado", tirar da primeira hora uma das poucas coisas que ele encontrou é decisão de desenho, não de conserto.
- **Dois itens viraram botão.** O Sino foi tocado 16 vezes e a Caixa de Música, 9, em meia hora. A Conta estourou quatro vezes e cobrou (o sino sozinho três vezes, a caixa sozinha uma). O sistema fez o que devia; para quem joga, foi som sem dono. A revisão externa sugere um "recibo" no diário depois de uma cobrança.
- **As fases andam depressa.** Fase 3 aos 39 minutos.
- **O jogador reage, e o mod lê.** Das cinco leituras, duas foram claras (giro de mais de 60 graus, parada).
- Ele usou comandos do `LEIA-ME.md` (gerar estruturas, teleporte, ver e apagar a memória). O roteiro passou a dizer quais não usar numa sessão de verdade.

**O que este conserto não resolve.** Outra sessão do Claude Code analisou o mesmo log a pedido do dono, pelo lado do desenho: `pesquisa/2026-10-09-analise-de-design.md`, na branch `docs/analise-de-design` (ainda fora da `main` quando isto foi escrito). A tese dela: devolver a voz ao Diretor é necessário e não basta, porque o que ele tem para dizer nas três primeiras fases é pequeno, as ferramentas chegam antes da ameaça e o jogo não ensina a usá-las. O dono pediu mais pesquisa e análise antes de estruturar as próximas versões; o que vem depois desta sai de lá, não daqui.

## Ferramentas

- O analisador passou a abrir o relatório com **alertas**: nenhuma aparição em vinte minutos de fase 2 ou mais, poucos eventos do Diretor, o ambiente falando mais que o Diretor, item usado como botão, Conta cobrando demais, fase 3 cedo.
- A sessão sintética ganhou um jogador que reage (para e vira o rosto a cada coisa que acontece), para o Diretor sair do estado de observar. E deixou de usar `helper.onEachTick`, que agenda de uma vez uma tarefa para cada tick até o fim do teste.
- `JogadorDeTeste.remover` solta os chunks que tinham sido forçados para aquele jogador (menos os que ainda são de outro).

---
# Versão 0.9.0-alpha14 — a primeira hora

Nada novo: reordenar e garantir. É a primeira entrega depois da análise de design de 09/10 (`pesquisa/2026-10-09-analise-de-design.md`, seções 6.1 a 6.4, corrigidas pela seção 10) e da pesquisa de 10/10 sobre primeiro encontro e ensino de ferramentas. O que a sessão de 09/10 mostrou: em 55 minutos, nenhuma aparição; meia dúzia de itens na mão sem o jogador saber do que tratam; doze das vinte e sete coisas percebidas vindas de uma regra que ele não vê.

## O primeiro contato (`PrimeiroContato`)

A primeira vez que o jogador vê a criatura deixou de depender de sorteio, de fase e do portão de vulnerabilidade. É regra, por jogador.

- **Quando.** De 15 a 25 minutos de jogo, na primeira vez em que ele estiver fora de casa com pouca luz (luz 7 ou menos, noite, ou fim de tarde). Não exige inquietação.
- **Prazo final.** Passados os 25 minutos, acontece com qualquer luz e em qualquer lugar onde caiba. De dia e a céu aberto, a forma é a mesma, mais longe (26 a 36 blocos em vez de 18 a 26), porque de perto e ao sol ele parece só um boneco parado.
- **O aviso.** É o da caçada: o mundo emudece, uma luz perto falha, um grave. Aqui ele diz a verdade (há alguém perto), então a primeira vez que o jogador ouve o aviso ele aprende o que o aviso quer dizer.
- **Onde.** Fora da tela, de lado e para trás (100° a 165° do olhar), num lugar em que dá para vê-lo ao virar. Parado.
- **Para ser visto.** Dois passos vindos dele, dois segundos depois, dizem o lado. Se ainda não foi visto, mais dois aos treze segundos. Ele espera quarenta segundos.
- **Visto.** Olhado direto por cerca de um segundo (ou dois de canto), dissolve. Não toca, não persegue, não captura. É a única aparição do começo que aguenta mais que um relance, porque é a única que precisa ser vista com certeza.
- **O que fica.** A primeira Cinza Pálida, que não some com o tempo, e um vestígio no lugar.
- **Só conta quando foi visto.** Se o jogador não o teve na mira (olhar direto, nem que por um instante, ou tempo de tela bastante para ele dissolver), o contato continua devendo: nada de cinza, e uma nova tentativa de quatro a seis minutos depois. Nascer fora da tela é o jeito mais fácil de ninguém ver; por isso, a cada tentativa que ninguém viu, ele nasce mais perto da borda da tela (100°–165°, depois 80°–140°, depois 62°–110°).
- **Atenção.** Não espera saldo nem passa pelo portão de V. Gasta o custo de uma cena, para o que vier depois respeitar o respiro.
- **Mundos antigos.** Quem já viu a criatura (`vezes_visto` ou `cinzas_geradas` acima de zero) é dispensado.

### O que espera o primeiro contato

Até ele acontecer (ou até a fase 3, o que vier antes):

- o vulto distante, que era a única aparição possível antes da fase 3;
- os lugares que entregam ferramenta: o Posto de Vigília, o Nicho Selado e a Casa do Vigia;
- o baralho.

O Marco de Estrada (não tem nada para pegar), a Soleira e o Boneco continuam como eram.

**Fora do alcance desta regra:** os baús do próprio jogo continuam podendo trazer itens do mod desde o primeiro minuto (`LootDoDiario`). É raro e não foi mexido.

## O sino diz a verdade na primeira vez

A pesquisa de 10/10 supunha que o sino responde ao que o primeiro contato deixa no lugar, sem ter lido o código. Conferido: respondia, mas só em 38% das vezes, sem texto nenhum, e o silêncio não tinha estado próprio. Na sessão de 09/10 foram 16 toques: 11 respostas vindas do caminho do próprio jogador e 5 silêncios mudos.

Enquanto o sino não deu a sua primeira resposta de verdade (`sino_respondeu`):

- com criatura a até 52 blocos, ou um vestígio a até 42, a resposta vem dali, **sempre**, com uma frase que dá o lado ("Algo responde, à esquerda de você.") e, se foi um vestígio, cinza subindo no lugar;
- sem nada por perto, a resposta é o silêncio, com frase própria ("O sino soa no vazio. Nada responde."). Não há mais resposta inventada vinda do caminho dele;
- a mentira (o sino que aponta errado depois de quatro usos) fica desligada.

Depois da primeira resposta de verdade tudo volta a ser como o diário descreve: o caminho pode responder, a mentira pode acontecer. O silêncio continua tendo a sua frase, sempre.

## Um verbo por vez: páginas e receitas (`Ensino`)

- **A página chega com o item.** Na primeira vez em que ele tem um item na mochila, a página do diário daquele item é pedida: é ela que a próxima Página Rasgada mostra, e vinte segundos depois uma página é deixada para ele do jeito de sempre (quatro passos atrás, a página no chão), fora de qualquer aparição. Itens com página: Cinza (10), Sino (11), Vela (5), Olho (6), Fio (12), Isca (15), Tigela (27), Caixa (28). A página da Linha de Cinza (26) vem na primeira vez em que ele risca uma.
- **O diário deixou de ser estritamente sequencial.** `Diario` guarda as páginas lidas como máscara. O cabeçalho mostra o número de verdade ("página arrancada (11 de 28)"), então dá para ver que há páginas faltando.
- **As receitas chegam uma de cada vez.** Antes, as oito receitas com Cinza Pálida apareciam juntas com a primeira cinza. Agora:

| Receita | Quando aparece no livro |
|---|---|
| Sino Oco | Com a primeira Cinza Pálida (como antes) |
| Vela Pálida | Quando o sino dá a primeira resposta de verdade; ou na fase 2; ou ao achar uma vela |
| Caderno de Vestígios | Na primeira cobrança da Conta; ou ao achar um |
| Fio, Isca, Lampião, Tigela, Ossos | Ao achar o item num lugar; ou na fase 4 |
| Caixa de Música | Ao ter uma caixa (como antes) |

- **A Caixa de Música saiu da passagem para a fase 2.** A análise propunha que ela chegasse "depois da primeira vez que ele ouvir a cantiga". Isso não é possível como está escrito: a cantiga não toca em lugar nenhum antes de a caixa ser usada (o assobio exige três toques da caixa). A caixa passa a ser achada onde já existia, no baú da Casa do Vigia, com a página dela.
- **O que é deixado para o jogador não some mais.** A página, o Olho e a página de item caem atrás dele como item sem prazo. Como item comum, quem não se virasse em cinco minutos perdia a entrega para sempre (a marca de "já entregue" fica na memória).

## As dicas dos itens

Cada item tem até três linhas: a frase de clima que já existia; uma linha que diz como se usa; e, nos nove itens que somam na Conta, uma linha igual em todos: "Tem um preço, e ele não é cobrado na hora." O Lampião Pálido e a Tigela de Oferenda, que não tinham texto nenhum, ganharam dica. A dica da Caixa dizia "a música você já ouviu em algum lugar", e não tinha ouvido: foi trocada.

O princípio: esconder as regras do monstro, nunca os verbos do jogador. As dicas não dizem limite, número nem o que a criatura faz.

## A Conta: carência, marcador, cobrança no uso, recibo

A regra continua escondida (o limite, quanto cada item pesa). O que deixou de ficar escondido é a ligação entre causa e efeito.

- **Carência.** Os três primeiros usos de cada item, na vida do personagem, não somam.
- **Marcador.** Todo uso que soma faz o mesmo som, baixo e só para ele, na hora: um risco de giz. É um som que já existia no mod e nunca tinha tocado; não foi criado som novo. A primeira vez que ele soa é a virada: dali em diante aquele item conta.
- **A cobrança é sempre no próprio item, no uso seguinte.** Ao estourar, a conta zera na hora e o item em que ele mais se apoiou fica marcado. Acabou a cobrança solta, de meio minuto a dois depois, "quando ele já tiver esquecido o que fez".

| Item | A cobrança, no uso seguinte |
|---|---|
| Vela | Dura a metade, e a frase ao acender é outra ("A chama pega fraca. Não vai durar.") |
| Olho | Dez segundos de escuridão, e ele vem (fase 3 ou mais) |
| Sino | Continua tocando sozinho, duas vezes, e ninguém responde |
| Fio | Alguém o dedilha três vezes logo depois de armado, sem atravessar |
| Isca | Não arma: a cinza escorre e não se prende (antes era só ignorada depois, sem sinal) |
| Caixa | Toca arruinada desta vez |
| Ossos | A jogada é "presença", sem sorteio |
| Linha | Já nasce gasta (dois estágios) |
| Lampião | A chama esfria por um minuto assim que é alimentada |

- **Recibo.** Depois de uma cobrança, o Caderno de Vestígios ganha uma linha por item cobrado, escrita como coisa que aconteceu no mundo e sem tom de bronca ("O sino continuou soando sozinho. Eu vinha perguntando demais."). A receita do caderno aparece na primeira cobrança.
- **Os três avisos** (chamas, cantiga falhada, zumbido) continuam como eram.
- De passagem: o relógio de esfriar ficava velho, e um uso isolado depois de um tempo parado era apagado no segundo seguinte. Corrigido.

## O analisador

`ferramentas/log/analisar.py` ganhou a seção "A primeira hora: os critérios", que troca o que se conta: aparições que estiveram na **mira** do jogador (não as criadas), sinais com lugar seguidos de **algo ali** em até dois minutos, saídas fortes por hora, e a parte da Conta no que ele percebeu. Três viram alerta: mundo novo sem aparição na mira até os 25 minutos; uma hora (ou 40 minutos, numa sessão curta) sem nenhuma saída forte; Conta acima de um terço. "De 6 a 12 aparições por hora" está suspenso e não vira alerta. Os números são ponto de partida e não foram validados.

A sessão sintética ganhou um terceiro jogador, que começa do zero, como num mundo novo.

## O que ficou de fora, de propósito

- Os números da seção 6.7 e as fases mais curtas da 6.8: só depois da próxima sessão do dono.
- Evento, item, lugar ou som novo.
- As propostas de `pesquisa/2026-10-10-inteligencia-e-direcao.md` (ele notar o jogador, responder na hora, ter um paradeiro): são comportamento novo e ficam para a entrega seguinte.
- Os defeitos listados no Apêndice D daquele documento, fora os três que esta entrega tocava de qualquer jeito (a entrega que sumia, a cobrança invisível da isca, o relógio da Conta). Em especial, o evento `LUZ_ERRADA` continua podendo quebrar uma tocha de verdade na fase 3 em diante: fere uma regra do mod e merece um conserto só dele.

## Não verificado por ninguém

Se o aviso e os dois passos bastam para o jogador virar e ver; se um segundo de olhar é muito ou pouco; se a forma diurna, a 26–36 blocos, lê como alguém ou como um boneco; se o som de giz se ouve, e se é ligado ao uso do item; se as dicas explicam o bastante sem estragar o clima; se a ordem sino, depois vela, deixa o jogador desprotegido por tempo demais.

---
# Versão 0.9.0-alpha15 — consertos depois da revisão da alpha14

A alpha14 foi revista por outra sessão antes de o dono jogar (`pesquisa/2026-10-10-revisao-da-alpha14.md`, no PR #32). Esta versão é só conserto: cada achado foi conferido no código antes de mexer, e nada novo entrou.

## O que cada achado virou

| Achado | Conferência | O que foi feito |
|---|---|---|
| 2.1 Tentativa de contato que ninguém vê cala o Diretor | **Confirmado**, e era pior do que o saldo: além de gastar 24 de atenção, cada tentativa zerava o relógio do piso (`ultimoEventoSeg`). Com tentativa a cada 4–6 minutos e piso de 7, o piso nunca chegava | A tentativa impõe o respiro e não gasta (`Atencao.respirar`); o custo (24) vem quando o contato é visto. Só a primeira traz o aviso inteiro e mexe no relógio do piso; as seguintes ficam com os passos. Com o Diretor recuando nenhuma tentativa acontece, fora a primeira do prazo final |
| 2.2 (a) Visto de canto de olho | **Confirmado** (`VISTO_DEMAIS` contava sempre) | "Visto" passou a ter uma definição só, em `HospedeEntity.contatoFoiVisto`: na mira dele por 6 ticks seguidos (0,3 s), com luz. O tempo de canto ainda o dissolve, mas o contato continua devendo, e isso não soma pressão nem `VEZES_VISTO` (somava, e no segundo seguinte o contato era "dispensado" sem cinza) |
| 2.2 (b) O "tímido" numa virada de câmera | **Confirmado** | O ramo `SUMIU_NO_DESVIO` só vale para o contato depois de ele ter sido visto |
| 2.2 (c) Visto no breu | **Confirmado na lógica** (a menos de 20 blocos a luz não era olhada, e a criatura não tem nada que brilhe) | O contato só nasce onde há luz para enxergá-lo (`Diretor.invocarOndeHaLuz`, luz efetiva 4 ou mais: céu aberto de noite passa, caverna sem tocha não), e a mira só conta com essa luz |
| 2.3 Uma página para várias pedidas | **Confirmado** | Quem decide a entrega é a Memoria (as páginas pedidas), não um agendamento em memória. Uma página de cada vez; a seguinte vem depois de ele ler a que tem; nunca mais páginas deixadas do que pedidas; e nenhuma enquanto houver uma na mochila ou caída a 16 blocos |
| 2.4 A tocha quebrada de verdade | **Confirmado** (`destroyBlock` no ramo `QUEBROU_COM_DROP`) | Virou miragem (`LUZ_ERRADA tipo=CAIU miragem=sim`): o som de quebrar só para ele, a tocha some só para ele, e volta quando ele chega a três blocos ou em meio minuto. Era o ponto que a revisão apontou; as alterações temporárias (`AlteracoesTemporarias`) e o que o mod ergue no mundo não foram auditados de novo nesta rodada |
| 2.5 Alerta de "nenhuma aparição até os 25 minutos" | **Confirmado** | O limite foi para 30 minutos de jogo (o prazo só começa a tentar aos 25) |
| 2.6 "Hora sem saída forte" contando tempo fora do mundo | **Confirmado** | As horas andam pelo tempo de jogo (`jogado_ate`), não pelo relógio do log |
| 2.7 Critérios que não medem o que dizem | Só o das duas sessões coladas | O relatório avisa quando o relógio do log volta para trás. O resto fica para depois da sessão do dono |
| 2.8 Passos da página fora da `Atencao` | É decisão de desenho | Não mexi. Atenção: com o conserto do 2.3 chegam mais páginas (uma por item), cada uma com os seus quatro passos |
| 2.9 Menores | — | Não mexi |

Achados meus, ao ler a sessão sintética depois dos consertos:

- **Quem põe a mira nele na hora em que ele dissolve viu.** Com trinta ticks de canto e a mira chegando no fim, ele dissolvia com três ou quatro ticks de mira, abaixo dos seis: o jogador via a dissolução no meio da tela e o contato não contava. Agora, se ele dissolve com a mira nele e com luz, conta.
- **Uma tentativa que ninguém viu deixou de ser "saída forte" no analisador** (só a vista é), e os eventos da sequência de ameaça passaram a ser (não tinham a linha `SELECAO`, e uma espreita seguida de "atrás de você" valia zero). Os dois erravam o alerta de "hora parada", um para cada lado.

## Medição

Sessão sintética de uma hora, jogador que começa do zero. O que a revisão mediu na alpha14 foi 25 minutos e meio sem evento do Diretor, com quatro avisos de caçada no meio. Agora: nove tentativas de contato (o jogador de mentira não se vira para o som e nunca o vê), **um aviso só**, 19 eventos do Diretor na hora, e o maior silêncio entre duas saídas foi de 4 minutos e 55 segundos. Os outros dois jogadores (fases 2 e 4) ficaram em 26 e 31 eventos por hora, na faixa do que a alpha13 mediu.

O que a sessão sintética não mede: se um jogador de verdade vê o contato. O de mentira nunca põe a mira nele, então a definição nova de "visto" só foi exercitada pelos testes de servidor.

## Os testes

Três novos e dois refeitos em `TestesDaPrimeiraHora` (48 no total):

- `oContatoQueNinguemViuContinuaDevendo` passou a provar que a tentativa não gasta atenção, que a segunda tentativa existe, que ela espera o Diretor sair do recuo e que vem sem o aviso inteiro;
- `oContatoSoContaQuandoEVisto` ganhou a virada de câmera (dois ticks de mira e o outro lado): ele não some e o contato não conta;
- `oContatoDeCantoDeOlhoNaoConta`: 37 ticks a trinta graus da mira dissolvem, não contam, não deixam cinza e não somam `VEZES_VISTO`;
- `oContatoVistoQuandoDissolveConta`: o caso da mira que chega no fim;
- `duasPaginasParaDoisItens`.

Sem teste: o breu (o mundo de teste não tem lugar escuro; a regra da luz foi conferida só lendo), a forma diurna do prazo, a tocha (o ramo é sorteado dentro do evento) e os testes 1, 3 e 5 da seção 3 da revisão.

## Para observar na sessão do dono

Além do que a revisão lista na seção 5: **quantas tentativas até o `visto=sim`** pesa mais agora. A definição ficou mais exigente, e as repetições são discretas (quatro passos, sem aviso). Se o log mostrar três ou mais tentativas com `naTela` maior que zero e `visto=nao`, o limite de seis ticks ou a luz mínima estão altos demais para um jogador de verdade.

---
# Versão 0.9.0-alpha16 — a caçada, sem as saídas de graça

Parte de `pesquisa/2026-10-10-a-cacada.md` (PR #32), seção 7: oito consertos, sem sistema novo. A regra da rodada: fechar o que **encerra a caçada de graça**, não toda saída. Correr para longe, élitro e pérola continuam valendo (e continuam virando dívida a mais de 110 blocos).

Cada item foi conferido no código do mod e, quando dependia do jogo, no código do Minecraft 26.2, antes de mexer.

## Os oito consertos

| # | Truque | Conferência | O que foi feito |
|---|---|---|---|
| 1 | Barco no caminho dele, carrinho em movimento, laço | **Confirmado no jogo.** `AbstractBoat` recolhe todo `LivingEntity` que encosta quando não há jogador no leme, a menos da tag `cannot_be_pushed_onto_boats`; `Mob.canBeLeashed` só recusa quem é `Enemy` | `HospedeEntity.canRide` e `canBeLeashed` devolvem falso (como o Warden e o Wither), e a tag entrou nos dados. Vale em todos os modos, não só na caça |
| 2 | Andar para longe durante o aviso | **Confirmado.** A busca é inicializada quando ele nasce; depois de 8 a 10 s de aviso ele saía atrás do lugar antigo, e a mais de 42 blocos não ouve | No fim do aviso ele ouve onde o jogador está (certeza 0,9). Fora da primeira caçada, três segundos depois vem o atalho. **E o ouvido passou a ler `getKnownMovement()`**: ver a ressalva abaixo |
| 3 | Pilar olhando para ele; anel fechado de 9 a 15 blocos | **Confirmado.** O teste de caminho ficava depois do `return` do ramo "na tela"; e entre o alcance do atravessar (8) e o do atalho (16) não valia regra nenhuma | O teste de caminho roda também enquanto ele é encarado. De 8 a 16 blocos, três segundos sem caminho liberam o atalho (`semCaminhoLonge`). A conta de atravessar continua só andando de perto |
| 4 | Água funda, longe da margem | **Confirmado no mod** (sem bloco firme perto do jogador a conta zerava). A profundidade em que o caminho falha não foi medida | Quando não há bloco firme, o aviso é no próprio bloco de água em que o jogador está, com bolhas. Sair de perto resolve, como nos outros |
| 5 | Pilar de 40; subir mais durante o aviso | **Confirmado.** Havia um limite de 32 de desnível; e a captura media em três dimensões a partir do bloco escolhido no começo do aviso | O limite saiu. Quando o bloco do aviso é o que está debaixo dos pés (`avisoPorBaixo`), a captura mede só no plano: o que escapa é sair da coluna |
| 6 | Tampar-se com blocos e ficar parado | **Confirmado em parte.** Colocar bloco não era ouvido; os pontos de busca ficavam a no mínimo 4 blocos do jogador de verdade | Clicar num bloco com um bloco na mão é ouvido a até 16 blocos (`OUVIU_COLOCOU`), como quebrar. O mínimo dos pontos de busca desceu para 2. Quem se tampa **antes** de ele chegar e fica em silêncio continua escondido: isso é esconderijo, não furo |
| 7 | Onde ele alcançava sem aviso legível | **Confirmados os três** | Depois de atravessar e não achar ninguém ele fica um segundo e meio parado; o avanço do piscar testa o trajeto e para em vidro, grade, cerca, porta e parede (`andarLivre`); o atalho sempre faz som |
| 8 | O descanso sumia ao fechar o mundo; a caçada não nascia em corredor de dois | **Confirmados** | `ameaca_liberada_em` fica na Memoria (vale o maior entre o guardado e o da sessão). Na caça ele nasce onde há dois blocos de ar (`Aparicao.buscarAoRedor` com `arLivre`), porque é a altura que ele tem abaixado |

## A ressalva do ouvido

O ouvido somava "velocidade do alvo x 3,6" e o salto, lendo `getDeltaMovement()`. Num jogador de verdade esse valor fica perto de zero no servidor (quem anda é o cliente; o próprio jogo usa `getKnownMovement()` para jogadores). Ou seja: andar e pular nunca contaram, só correr e a distância desde a última vez em que ele foi ouvido.

Com a troca, o termo passa a valer. Na conta, a chance por segundo de ele ouvir um jogador **andando** a 20 blocos sobe de uns 36% para uns 55%; **correndo**, de uns 49% para uns 73%. Agachado e quase parado continua sendo silêncio. Isto não fecha um furo: afina um sentido que estava mais surdo do que o código dizia. Ninguém mediu em jogo, e os jogadores de mentira não mandam pacote de movimento, então nenhum teste vê a diferença. É o item desta rodada para vigiar na primeira caçada de verdade (linhas `BUSCA ... conf=`), e o primeiro candidato a voltar atrás.

## O que não foi tocado, de propósito

**As três decisões de desenho da seção 7 são do dono:**

1. A vela acesa com ele a até 8 blocos encerra a caçada na hora (`ZONA_CALMA`). Deve?
2. Deitar na cama com a caçada em curso. O jogo deixa.
3. A caçada cabe na primeira sessão? Hoje só existe na fase 4.

**Furos que a análise lista e que são condição de começo, não de fim:** estar montado, com o pé na água, com menos de três corações ou com luz acima de 4 nos pés impede a caçada de começar; clicar numa cama cria "casa". São desenho antigo ("a caçada nunca começa na base...") e ficaram como estão.

**Também ficou:** os avisos do atravessar encurtam para sempre, e a conta sobe até em tentativa falha; o atalho não testa parede (pode cair num cômodo fechado); alçapão aberto sobre poço; Lentidão, teia e mel; toque por cima de cerca; a vela que acaba vira caçada sem aviso.

## Achados de passagem

- **Tão alto, a captura não leva o jogador a lugar nenhum.** `Captura.escolherDestino` só aceita chão a até 10 blocos de desnível. Num pilar mais alto que isso o jogador é pego (vida, item da mão, marca) e fica onde está (`CAPTURA deslocou ... para=-`). Já era assim de 11 a 32 blocos; sem o limite de 32, vale para qualquer altura. Não mexi.
- **A "primeira caçada" de uma caçada de comando.** `Cacada` é criada dentro de `configurar`, antes de `definirOrigem`: nessa hora `ehTeste()` ainda é falso, e uma caçada de comando num jogador que nunca foi caçado conta como primeira (sem atalho e sem atravessar). Só afeta comando e teste; os testes que precisam do resto põem `CACADAS` em 1.
- **O atalho depois do aviso depende de duas contas de três segundos.** A notícia do fim do aviso vale por 60 ticks e o atalho pede 60 ticks fora da tela: as duas se encontram num tick só. Funciona (está no teste), mas quem mexer num dos dois números quebra o outro sem perceber.

## Os testes

Oito novos em `TestesDaCacada` (56 no total): `barcoELacoNaoOSeguram`, `encararDeCimaDoPilarNaoVence`, `subirMaisNaoEscapaDoPilar`, `quemSaiDePertoTemUmRespiro`, `andarParaLongeNoAvisoNaoBasta`, `oPiscarNaoAtravessaVidro`, `colocarBlocoEntregaAPosicao` e `aCacadaComecaDebaixoDeTetoBaixo`. O descanso guardado é conferido dentro de `aCapturaDeVerdadeDeixaAMarca`.

Sem teste: a água funda (o mundo de teste é plano e raso; conferido só lendo), o carrinho, a leitura do descanso ao abrir o mundo, o ouvido novo e o som do atalho.

**Os testes mudaram de lugar.** Acrescentar oito testes mudou a grade em que o jogo os distribui, e dois defeitos antigos apareceram: cinco testes da caçada usavam a mesma coluna a 12 blocos da origem (`dz = -12`) e foram parar em cima da estrutura de outro teste, a doze blocos do chão; e cinco testes da primeira hora repetiam colunas de outras classes (um som de um vizinho derrubou `oSinoNaoInventaResposta`). Agora cada teste tem uma coluna só sua.

## Medição

Sessão sintética de uma hora, três jogadores de mentira: duas caçadas, as duas terminadas por toque, sem nada fora do lugar; ritmo na mesma faixa das medições anteriores (30, 39 e 49 saídas por hora). Jogador de mentira não usa truque nenhum: o que esta versão muda só aparece nos testes de servidor e, depois, numa caçada de verdade.

---
# Versão 0.9.0-alpha17 — ele nem sempre some

É a etapa 1 de `pesquisa/2026-10-11-sessao-e-interacoes.md` (seções 5.1 e 5.2), pedida pelo dono com as palavras dele depois da sessão de 10 e 11/10: "ele realmente aparecer na janela do nada sem avisar, e sair indo pro lado e não sumindo". Naquela hora de jogo ele viu a criatura por dois segundos somados, em três relances.

## A saída andando

Toda aparição de observação acabava dissolvendo no instante em que era olhada. Isso continua sendo a regra (foi o que ele pediu em 08/10: "será que eu vi?"). A exceção nova, `HospedeEntity.Saida.RECUA`:

1. visto o bastante para sumir, ele **não some**: segura o olhar de 0,4 a 1 segundo;
2. vira e vai embora andando, a 3 blocos por segundo, **de lado** em relação ao jogador, para um lugar a 3,5 a 9 blocos, de preferência onde o jogador deixa de vê-lo (atrás de uma árvore, de uma parede, fora do quadro da janela);
3. meio segundo depois de sair da vista, ele não está mais lá, sem dissolver;
4. se chega ao lugar e continua à vista (campo aberto), anda mais dez blocos, de costas para o jogador; ao fim de oito segundos, ou travado, dissolve como antes;
5. onde ele parou podem ficar o vestígio e a cinza, pela mesma regra de quem some visto.

Quem escolhe o lugar é `assombracao/Retirada`: testa o caminho antes, não gasta o sorteio do mundo (o lado e o tempo de segurar o olhar saem do ID da manifestação) e, sem lugar, devolve nada: aí ele some como sempre. Correr para cima dele continua valendo o "chegou perto, ele não está mais lá".

**Quem tem a saída andando:**

- **a cena da janela**, sempre (ver abaixo);
- **a presença comum** (`PRESENCA` sorteada pelo Diretor): uma em três, nunca duas seguidas, só depois do primeiro contato (ou da fase 3) e só com ele a mais de 14 blocos (de perto, andando, ele parece um boneco).

Ficam como estavam: o primeiro contato, o vulto distante (que o dono aprovou), o "atrás de você", a espreita, as outras cenas e a caçada.

## A janela, mais cedo

A cena "Do outro lado do vidro" existe desde a 0.5 e não anuncia nada. O dono a pediu sem saber que existia, porque não chegou nela: pedia fase 3 (veio aos 50 minutos) e, na prática, dois minutos seguidos em casa, de noite; na fase 3 a estadia mais longa dele em casa foi de 86 segundos.

| | Antes | Agora |
|---|---|---|
| Fase | 3 | 2, para quem já teve o primeiro contato; 3 para os outros |
| Primeira chance numa estadia em casa | aos 120 s | aos 45 s; depois a cada 120 s |
| Visto pelo vidro | dissolve | sai andando ao longo da parede, para fora do quadro |
| Sem vidro a 12 blocos | nada, em silêncio | nada, com a linha `pulada motivo=SEM_JANELA` |

De noite, em casa e com o descanso de 25 minutos entre uma e outra: como antes. O estalo no vidro para quem não olha: como antes.

O dono mora numa vila: há vidro nas casas vizinhas, e a cena as usa quando ficam a até 12 blocos dele. Mas só com ele na casa dele (contexto `CASA`).

## O que ele respondeu em 11/10 (pesquisa de 11/10, seção 6)

- A base tem janela de vidro, e as casas em volta também.
- Fechou o jogo em 10/10 "pra dormir", não por causa do contato.
- Para que serve cada item: "linha proteção? rádio pra acalmar a criatura? osso alguns formatos indicam algo da criatura? e o resto não lembro". São os três que ele usou, e os três com a ideia certa. Da Vela e da Cinza, que ele teve na mão e leu a página, não ficou nada. **Usar ensina; a página sozinha, não.** (O "rádio" é a Caixa de Música.)

## Os testes

- JUnit: `CenaDoOutroLadoDoVidroTest` (a fase e a hora da chance).
- Servidor, três novos em `TestesDasAparicoes` (59 no total): no campo aberto ele vai embora andando, continua existindo enquanto é olhado e some quando deixa de ser; sem a saída andando, o relance continua igual; e pela janela, com parede e vidro de verdade, ele sai de lado sem dissolver.

Sem teste: o sorteio de uma em três na presença comum (está só lido), e a aparência do andar. O modelo tem animação de caminhada, que a caçada já usa, mas ninguém o viu andar devagar e de pé: **é a primeira coisa a perguntar ao dono**, e a primeira a rever se parecer um boneco deslizando.

**Os testes ganharam uma origem só.** Com sessenta testes, a grade em que o jogo espalha as estruturas passou dos cem blocos de fundo, e dois testes em colunas "a cem blocos" uma da outra ficavam a quinze: a criatura de uma caçada de teste apagou o lampião da Casa do Vigia de outro teste (`aCasaMudaQuandoEleVaiEmbora`, uma falha em cinco execuções). `JogadorDeTeste.chao` passou a contar todas as colunas a partir da estrutura do primeiro teste. A regra de uma coluna por teste, escrita na alpha16, só vale de verdade com isto.

## O que ficou para a etapa 2

A `afronta` (seção 5.3 da pesquisa): o que o jogador faz com ele ter resposta na hora. E a inclinação de cabeça antes de sair, que pede mexer no modelo e foto.

---
# Versão 0.9.0-alpha18 — o terreno de verdade

## O que deu errado na alpha17

O dono jogou a alpha17 por uma hora e meia e, no fim, forçou a cena da janela por comando, duas vezes, na casa dele. Nas duas o Hóspede apareceu no vidro e **sumiu do jeito antigo** (`sumiu motivo=VISTO_DEMAIS`, sem a linha `saida=RECUA`). A saída andando não aconteceu nenhuma vez no jogo de verdade.

Li uma cópia do mundo salvo (`ferramentas/log/ler_mundo.py`) e refiz a escolha de lugar em cima do terreno real em volta da casa: **dos 24 lugares para onde ele poderia ir, nenhum tinha "chão"**. O quintal é todo capim e flor. A procura de chão pedia três blocos de **ar**: um pé de capim não é ar (ele "não cabia") e também não é chão (não tem corpo), então a coluna inteira era descartada. Com a regra nova, 21 dos 24 servem.

Os meus testes não pegaram porque o mundo de teste é um gramado raspado, sem uma planta.

## O defeito era maior do que a janela

A mesma exigência de ar estava em toda a procura de lugar do mod: onde ele nasce (`Aparicao`, `Diretor.acharChao`), o vulto distante, os pontos de busca e o atalho da caçada, o lugar para onde a captura leva. Num campo ou numa vila, quase toda coluna tem capim. Na mesma sessão:

- duas das quatro sequências de ameaça acabaram com `FIM motivo=SEM_LUGAR`, sem aparição (uma delas com ele em casa, na vila);
- a única `PRESENCA` sorteada falhou (`SELECAO falhou`);
- a espreita não achou para onde se mexer duas vezes.

É boa parte do "cadê o bicho?" que a irmã dele perguntou. Não é a explicação inteira: uma das sequências sem lugar foi no fundo de uma caverna, onde não há capim (ver a pesquisa de 11/10, segunda parte).

## O conserto

- **`Diretor.livre(level, pos)`**: dá para ocupar o bloco? Ar, ou qualquer coisa sem corpo (capim, flor, plantação, neve rasa); nunca líquido, fogo, teia, neve fofa nem o que dá luz. Substitui o `isAir()` em `Diretor.acharChao`, `Aparicao.acharChao`, no vulto, em `HospedeBusca.acharChao` e `valido`, na saída do atravessar e em `Captura.chaoParaJogador`.
- **`Retirada`**: o chão é procurado na altura em que ele está (de cima para baixo, ao lado de uma casa, o primeiro chão é o telhado); todos os lugares têm o caminho testado, não só os seis melhores; se nenhum de lado serve, ele recua de costas para longe; e quando nada serve fica a linha `saida=RECUA falhou ...`.

## As chamas pálidas, uma de cada vez

O dono, da caverna: "os eventos e coisas ficaram muito focados em mudar a tocha de cor". O log: **90 tochas empalidecidas em 90 minutos**. A cota (duas ou três ao mesmo tempo na fase 4) era reposta a cada vinte segundos; numa caverna iluminada por tochas, é o que mais se vê.

Agora passam pelo menos três minutos entre uma tocha que empalidece e a seguinte. A cota continua a mesma, mas na prática fica uma, às vezes duas. O acréscimo que a carta do baralho dá por dez minutos não espera.

## Os testes

Três novos (62 no total): a cena da janela com capim e flor em todas as colunas (`noQuintalComCapimEleTambemSaiAndando`), a caçada que acha onde começar num campo de capim (`noCapimEleAchaOndeAparecer`) e as chamas uma de cada vez (`asChamasVemUmaDeCadaVez`). Os dois primeiros falham com a regra antiga.

Sem teste: o que só o terreno dele tem (desnível, casa vizinha, telhado). Para isso serve o replay no mundo salvo, que não é automático.

## Lição

Teste em mundo plano e raspado não diz nada sobre o mundo onde se joga. Quando uma coisa depende do terreno, o teste precisa pôr o terreno: capim, flor, desnível, teto. E, havendo um mundo de verdade à mão, vale ler o mundo.

---
# Versão 0.9.0-alpha19 — de perto, e o Véu nunca vazio

É a etapa 1 do cardápio de 11/10/2026, proposta ao dono com estas palavras: "Ele aparecer em lugar apertado (caverna, dentro de casa), e o 'tudo branco' nunca vir vazio. São as duas que respondem 'cadê o bicho?'". Ele respondeu "por favor".

## O que faltava

Na sessão de 11/10 à tarde (uma hora e meia, fases 3 e 4, boa parte numa caverna):

- o Diretor quis mostrá-lo 8 vezes e conseguiu 3;
- uma sequência de ameaça esperou um minuto por um lugar, no fundo de uma caverna, e acabou sem ninguém (`FIM motivo=SEM_LUGAR`); outra, com ele debaixo da própria casa, também;
- o Véu abriu duas vezes, as duas sem porta, sem vidro e sem vulto: só a cor e a neblina.

A alpha18 tirou o capim do caminho, mas a caverna tinha outro motivo. Fora da caça ele media 0,7 por 3,0 blocos e só nascia de 14 a 38 blocos do jogador. Num túnel de dois blocos de altura ele não cabia em lugar nenhum, e num salão de caverna de vinte blocos não há um ponto a 24 blocos com linha livre.

## A forma de perto

**A procura** (`Aparicao.buscarDePerto`). A de sempre sorteia um ângulo e uma distância e vê se ali há chão. Num túnel quase todo sorteio cai dentro da pedra. A nova não sorteia: anda, a partir dos pés do jogador, pelo espaço em que ele está (células com chão e dois blocos livres, subindo um degrau ou descendo até três), até 14 blocos, e fica com as células de 8 a 14 blocos que:

- estão fora da tela (a regra de sempre: ele nunca nasce na frente do jogador);
- dá para ver de onde o jogador está, se ele virar o rosto (o peito ou a cabeça);
- são lugar fechado: nenhuma luz do céu chega ali, ou há teto de verdade a até quatro blocos do chão (copa de árvore não é teto);
- não estão na zona da vela nem coladas numa Linha de Cinza;
- não são o "exposto demais" de sempre (de dia, a céu aberto, sem nada na frente).

Entre elas, a nota: luz para ele ser visto (`Diretor.temLuzParaVer`, +2,0), quina (uma célula vizinha onde ele caberia e que o jogador não enxerga: "um passo o tiraria da vista", +1,5), o meio da faixa (até +0,5) e um desempate que sai da posição e do segundo do jogo (até +0,6). **Não usa o sorteio do mundo**: quando ela acha lugar, os sorteios do Diretor ficam como estavam.

**Quando ela roda** (`Diretor.invocar` e `Diretor.invocarDePerto`):

- na presença comum, debaixo da terra (`ContextoMundo.Tipo.SUBSOLO`), ela vem **antes** de todas as outras, menos da isca (que é pedido do jogador). O rastro e a rota, numa mina, punham-no a trinta blocos, depois de duas curvas, onde ninguém via;
- em qualquer `invocar` de quem observa (o "atrás de você", o vulto do Véu, o Olho, os Ossos), debaixo da terra, também antes;
- nos outros lugares, e para a espreita (que começa longe para poder chegar perto), só quando a procura de sempre não acha nada. É o que cobre "dentro de casa" e a mina debaixo da base, onde o contexto é `CASA`.

**O corpo.** O sinal de "abaixado" deixou de ser `modo == CACAR`: há um dado sincronizado próprio (`HospedeEntity.definirAbaixado`). `Diretor.criar` decide pelo teto do lugar, para qualquer modo que não seja a caça: com dois blocos livres ele nasce dobrado (caixa de 0,7 por 1,9, o `CORPO_NA_CACA`), com três fica de pé. Na espreita, quando ele muda de lugar, a postura é a do lugar novo. O desenho obedece (`HospedeRenderer`): na caça continua olhando o teto a cada quadro, porque ali ele anda.

**O que não muda.**

- De perto ele é só relance. O limite de "visto demais" é o da presença comum (0,3 s olhando direto, com ousadia zero) também na espreita, que de longe aguenta três segundos (`HospedeEntity.definirDePerto`). Mais perto não virou mais tempo.
- Quem nasceu de perto nunca vai embora andando, seja qual for a saída que a cena pediu (`comecarRetirada` recusa).
- Se o jogador chega a cinco blocos sem olhar, ele não está mais lá (`DIST_SUMIR_DE_PERTO`; o "atrás de você" mantém os seus quatro).
- A caça e o outro lado não usam a procura nova. As proteções da casa (caçada nunca em casa nem a 24 blocos da cama) ficam como estavam. O primeiro contato também: ver "O que ficou de fora".

## O Véu nunca vazio

- **O vulto é certo quando não há porta nem vidro** (`Veu.abrir`). O sorteio continua sendo feito no mesmo lugar (a ordem dos sorteios seguintes do jogador não muda); depois de saber que não há o que trocar na casa, ele deixa de valer. O log diz `vulto=certo`.
- **Ele tenta de novo.** Até a alpha18 o vulto tinha uma tentativa, no meio do Véu. Agora, se não coube (de frente para um paredão, no meio de um pulo), tenta a cada segundo enquanto sobrarem quatro segundos de Véu. `VEU vulto=nao_coube` só sai no fim, se não coube nenhuma vez, com a linha `LUGAR falhou` logo depois.
- **De dia, em campo aberto, ele cabe.** `Diretor.expostoDemais` recusava qualquer lugar a menos de 25 blocos, de dia, a céu aberto e sem cobertura, e o vulto do Véu pede de 12 a 17: num campo, de dia, ele nunca vinha. Com o Véu aberto a neblina fecha a menos de trinta blocos e a cor é a do outro lado: a regra do "boneco" não vale ali, e só ali.
- **Debaixo da terra** ele usa a procura de perto antes da outra (é um `invocar` de quem observa). Sem isso o log diria `nao_coube`, ou pior: `sim`, num bolsão de caverna que o jogador não vê (a procura de sempre não exige linha de visão para o vulto do Véu).

## O log diz por quê

Até a alpha18 o log dizia `SEM_LUGAR` e mais nada: para achar o capim foi preciso copiar o mundo do dono e refazer a procura à mão.

- `HOSPEDE ... criado ... motivoPosicao=` diz qual procura achou o lugar (`APARICAO2`, `APARICAO2_COBERTURA`, `RASTRO`, e agora `DE_PERTO dist= luz= quina= abaixado= candidatos=`), e a linha termina em `postura=ABAIXADO` ou `DE_PE`.
- `LUGAR falhou NORMAL[vezes=3 tentados=54 semChao=40 tela=6 semVisao=8] DE_PERTO[vezes=3 celulas=41 naFaixa=12 tela=8 semVisao=4]`: cada procura que falhou desde a linha anterior, quantas vezes rodou, quantos lugares tentou e em que exigência eles caíram. Sai junto de `SELECAO falhou`, do fim de uma sequência de ameaça, do `SEM_LUGAR` das cenas, de `VEU vulto=nao_coube`, de `OLHO aparicao=nao_coube` (linha nova) e, no primeiro contato, a cada minuto sem lugar. É só contagem (`Aparicao.Falhas`): não gasta sorteio.
- O analisador ganhou: alerta para toda construção que acabou sem ninguém (com o motivo da última), alerta quando ele teve a criatura na mira em menos da metade das vezes em que o mod quis mostrá-la, a seção "Por que não achou lugar", quantas nasceram de perto e abaixadas, e o que cada Véu tinha dentro.

Instrumentadas: a procura de sempre (`NORMAL`, `COBERTURA`, `COM_LUZ`), o rastro e a de perto. Não instrumentadas: a isca, a borda da vela, o marco, a rota, o túmulo e o vulto distante.

## O terreno de verdade

A lição da alpha18 era "havendo um mundo de verdade à mão, vale ler o mundo". Agora isso é teste.

- `ferramentas/log/ler_mundo.py exportar <região> x0 y0 z0 x1 y1 z1 <saída>` grava uma caixa de blocos num arquivo de texto (só os nomes dos blocos; **sem as coordenadas de origem**). `Terreno.montar` (em `src/gametest`) monta esse arquivo no mundo de teste, dentro de uma casca de pedra.
- `src/gametest/resources/terrenos/caverna.txt` é o salão de caverna em que a sequência de ameaça do dono acabou sem ninguém em 11/10: 47 por 16 por 43 blocos de ardósia, tufo, minério, cascalho, um pouco de água e as tochas dele. Não tem nada da base.
- Nesse salão, de dois lugares junto das tochas, olhando para os quatro lados, a procura de perto acha lugar em **sete das oito vezes**: no salão, entre dezenas de candidatos (de pé ou abaixado, conforme o teto do lugar escolhido); no bolsão ao lado, abaixado, no único lugar que há. A oitava é esse bolsão com o jogador virado justamente para esse lugar: `DE_PERTO[vezes=1 celulas=46 naFaixa=21 tela=19 semVisao=2]`. Ele nunca nasce na tela, e ali não aparece.
- A sequência de ameaça inteira, nesse salão, vai até `GOLPE_CONCLUIDO`, com duas aparições a nove blocos.
- **O que o terreno de verdade ensinou.** A primeira versão desse teste falhou: num canto escuro do salão, com o jogador de mentira parado e de rosto fixo, a sequência acabou em `SEM_LUGAR` mesmo com a procura nova (`DE_PERTO[vezes=61 celulas=2013 naFaixa=305 tela=305]`: os cinco lugares de 8 a 14 blocos ficavam todos à frente dele, durante os sessenta segundos). O teste passou a virar o rosto do jogador um quarto de volta a cada dois segundos. Fica o limite, conhecido: **num bolsão pequeno, quem fica parado olhando para o único lado que tem espaço não vê ninguém aparecer.** A resposta a isso é a ideia 2.9 do cardápio (o plano B), não afrouxar "nunca na tela".

## Os testes

Sete novos, em `TestesDePerto` (69 no total). O túnel de teste é de pedra, um bloco de largura por dois de altura, com uma curva dez blocos atrás do jogador e uma tocha num nicho junto da curva, tudo dentro de um bloco maciço (lá dentro não chega luz do céu).

- `noTunelEleApareceDePertoEAbaixado`: a presença, forçada, nasce na curva (iluminada, e quina), a dez blocos, abaixada, sem estar dentro da pedra; olhada, some em menos de um segundo e meio, sem sair andando.
- `aAmeacaNoTunelNaoAcabaSemNinguem`: a sequência de ameaça vai até `GOLPE_CONCLUIDO`, com a espreita e o golpe na curva, abaixados.
- `oVeuSemPortaNemVidroTrazOVulto`: num campo, com a semente do jogador escolhida para o sorteio dar "sem vulto", o vulto vem.
- `noTunelOVeuTambemTrazOVulto`: o mesmo no túnel; o vulto nasce na curva, abaixado.
- `aCeuAbertoEleNaoVemDePerto`: num gramado, a procura de perto não acha nada (`aberto=291`).
- `naCavernaDeVerdadeEleAchaLugar` e `aAmeacaNaCavernaDeVerdadeNaoAcabaSemNinguem`: os dois de cima.

Os três testes de terreno da alpha18 continuam passando.

Ganchos de teste novos no `Diretor`: `ameacaParaTeste`, `fimDaAmeacaParaTeste`, `etapaDaAmeacaParaTeste`, `contextoParaTeste` e `dePertoParaTeste` (também como `/sussurros teste deperto`; com `parado` ele não some ao ser olhado, para as fotos).

## O que ficou de fora, de propósito

- **O primeiro contato** continua com a procura dele (`invocarOndeHaLuz`, de 18 a 36 blocos, de pé). Quem está numa mina aos vinte minutos de um mundo novo continua esperando sair dela. Usar a forma de perto ali muda a primeira impressão do mod inteiro; fica para quando o dono disser.
- **A espreita não anda pelo túnel.** `reposicionarEspreita` continua pedindo três blocos de altura e pelo menos nove de distância: no túnel ela fica onde nasceu (`acao=FICAR motivo=SEM_LUGAR`) e some depois de duas ou três esperas.
- **Passar por um bloco** (ideia 2.7) e **o plano B** (2.9).
- **O quintal do dono como terreno de teste.** A caverna é pedra e tocha; o quintal é a casa dele, e o repositório é público. Pergunta para ele.

## Não verificado por ninguém

- Se, de perto e abaixado, ele assusta ou vira boneco. As fotos mostram a pose parada (ver "As fotos", no fim); o instante em que o jogador vira e ele some, só jogando.
- Os números: de 8 a 14 blocos, cinco blocos para sumir, os pesos da nota, os quatro blocos de teto. São ponto de partida, sem sessão.
- Se, debaixo da terra, ele passa a aparecer demais. A presença comum tem o mesmo peso de antes; o que muda é que agora ela acha lugar. O analisador conta as que nasceram de perto: é o primeiro número a ler na próxima sessão.
- O som do anúncio (dois passos vindos dele) a dez blocos, num túnel.

## Medição

`./gradlew build` três vezes seguidas: 69 testes passaram nas três.

Sessão sintética de uma hora, três jogadores de mentira, em campo aberto (11/10/2026): 26, 36 e 42 saídas por hora, na faixa das medições anteriores (30, 39 e 49 na alpha16); 9, 12 e 12 manifestações da criatura por hora; **nenhuma nasceu de perto** (em campo aberto não há lugar fechado, e é o que se queria: a mistura da superfície não mudou); nenhuma linha `LUGAR falhou`. O único Véu da sessão abriu com `vulto=certo`, e o vulto veio na primeira tentativa.

Uma execução só, e os números variam bastante de uma para outra. A sessão sintética não tem caverna: o que esta versão muda debaixo da terra só aparece nos testes de servidor e, depois, na sessão do dono.

O alerta novo do analisador ("ele teve a criatura na mira em N de M vezes") dispara sempre para jogador de mentira, que não mira em nada. Vale para sessão de gente.

## As fotos (40 a 42)

Tiradas no túnel de teste, com a procura de verdade e ele no modo que não some (`/sussurros teste deperto parado`). Abri as três.

- **40, a dez blocos, à luz da tocha da curva** (é o que o jogador vê ao virar): uma figura fina e escura no fim do túnel, ocupando a altura toda dele, com as pernas, os braços e a cabeça distinguíveis contra a pedra iluminada. Lê-se bem. O que **não** se lê é "abaixado": de frente, o tronco dobrado some atrás da cabeça, e ele parece só uma figura de dois blocos de altura.
- **41, a quatro blocos, e 42, a mesma com o teto trocado por vidro**: a cabeça e os ombros, com os dois pontos pálidos dos olhos (esta manifestação os tinha). Não vi nada atravessado na parede.
- **A cabeça passa do teto.** Pela conta do modelo, com o tronco dobrado a 72 graus o pescoço fica a 1,72 bloco do chão e o topo da cabeça a uns 2,3: um terço de bloco dentro do teto de dois blocos. A foto 42 é compatível com a conta (a cabeça chega à linha do teto). Não é desta versão (é a pose da caça, desde que ela existe), e de frente o próprio teto esconde; parado, ele está sempre de frente para o alvo. Aparece de lado (um segundo jogador, ou a caça passando) e por um teto de vidro. **Não mexi no modelo:** é mudança no desenho da caça, às cegas quanto ao perfil. Para quem for mexer (a ideia 2.7 pede uma pose nova de qualquer jeito): dobrar o tronco a 90 graus e erguer a cabeça 45 dá um topo a 1,95 bloco, e uma foto de perfil precisa de uma criatura que não vire para o jogador (a de hoje vira sempre).

A câmera das fotos 41 e 42 olha um pouco para cima (o comando `facing entity ... eyes` mede a partir dos pés do jogador), e por isso ele aparece na metade de baixo da imagem.

---
# Versão 0.9.0-alpha20 — nada termina em nada (pacote 1)

É o primeiro dos dez pacotes do plano de conteúdo de 11/10/2026 (`pesquisa/2026-10-11-pesquisa-profunda.md`, arquivo local). O dono autorizou os dez de uma vez ("por mim pode todos") e joga depois; o pacote 1 são as ideias 2.9 e 2.10 do cardápio. O fato a que ele responde: em noventa minutos nas fases 3 e 4 o Diretor quis mostrar a criatura oito vezes e conseguiu três, duas de quatro sequências de ameaça acabaram sem ninguém, e quem jogava com o dono perguntou "cadê o bicho?".

A alpha18 e a alpha19 atacaram a causa (a procura de lugar). Esta ataca o que sobra depois delas: o caso em que não há mesmo onde pôr o corpo, e o caso em que ele foi posto e ninguém viu.

## O plano B (`PlanoB`)

**O que o jogador percebe.** Quando o mod prepara uma aparição e não acha onde pôr o corpo, a preparação não termina mais em silêncio. Termina num sinal físico, com lugar, do lado em que ele estaria:

- **`PO`**: cai pó do teto, em três punhados ao longo de um segundo, com um toque surdo no bloco de onde ele sai. As partículas são as do próprio bloco do teto (`FALLING_DUST` e `BLOCK`). Só onde há teto de dois a sete blocos acima do lugar, e só quando o jogador enxerga o lugar.
- **`TOCHA`**: uma tocha de verdade daquele lado (a até oito blocos do lugar e dezesseis do jogador) apaga por quatro a sete segundos, com o som de chama apagando. É miragem: só ele vê, e o bloco continua no mundo.
- **`PASSOS`**: dois passos se afastam do jogador, a partir do lugar, com o som do chão de verdade de cada um (tom 0,6, o dos passos dele).

**De que lado.** "O lado em que ele estaria" é o **quase lugar** (`Aparicao.Quase`): o melhor lugar em que a procura que acabou de falhar quis pô-lo e não pôde, com a exigência em que ele caiu. As duas procuras instrumentadas (a de sempre e a de perto) guardam um por tick; primeiro o que o jogador enxerga, depois o mais perto. Lugares que caíram pela zona da vela ou por uma Linha de Cinza não entram.

- O caso que a alpha19 deixou conhecido, o bolsão em que o único espaço fica à frente do jogador: todo lugar de 8 a 14 blocos cai por estar na tela (ele nunca nasce na tela), e o quase lugar é o mais perto deles, **à vista**. É ali que o pó cai. A regra "nunca na tela" continua valendo para o corpo; o sinal, ao contrário, é melhor quando é visto.
- Se o quase lugar fica a mais de 14 blocos (a procura de sempre tenta lugares a trinta), o sinal vem do mesmo lado, a nove blocos: passos a trinta blocos não são com ninguém.
- Sem quase lugar, vale um ponto por onde o jogador passou (de 5 a 14 blocos; de preferência fora da tela, o mais perto de nove). Sem isso, atrás dele: seis, dez ou catorze blocos, o primeiro que estiver fora da zona da vela.

**Qual sinal** (`PlanoB.escolherTipo`, função pura, com teste JUnit). À vista do jogador, o que se vê vem primeiro: pó, tocha, passos. Fora da vista, o que se ouve: passos, tocha (pó que ninguém vê não é sinal). Havendo escolha, nunca o mesmo duas vezes seguidas.

**Onde entra.** Nos quatro pontos em que o analisador já contava "construção que acabou sem ninguém":

- `Diretor.fimAmeaca`, quando o motivo é `SEM_LUGAR` (a sequência de ameaça que esperou um minuto);
- o `SEM_LUGAR` das quatro cenas com presença (túnel, marco, campo nas duas aparições, casa);
- o vulto do Véu: na última tentativa que cabia no tempo, com quatro ou cinco segundos de Véu pela frente (se o Véu é rasgado antes por uma vela, não sai nada: `planoB=-`);
- a aparição chamada pelo Olho que não coube (o Olho já tinha dito "ele sentiu").

**Regras.**

- Só o alvo vê e ouve: as partículas saem por `sendParticles(jogador, ...)` e os sons por `tocarEventoPara`. Um teste de servidor confere com um segundo jogador colado nele.
- Nada dentro da zona da vela. Se não sobra lugar fora dela, não sai nada e o log diz `NENHUM`.
- Não é manifestação, e o analisador não a conta como uma: a linha `SEM_LUGAR` continua lá e continua nos alertas, agora com `planoB=`.
- Não gasta saldo de atenção (é o fim de algo que já tinha começado; a sequência e as cenas entram em silêncio logo depois), só impõe o respiro.
- **Não usa o sorteio do mundo**: o que sai depende do lugar e do que saiu da última vez. Os sorteios do Diretor ficam como estavam.

**O vazio por escolha continua.** O relatório pedia que o log separasse o vazio por falha do vazio por escolha, e que uma parcela pequena de vazios escolhidos continuasse existindo (esperar e não receber mantém o alerta; é a regra da 0.6). Minha leitura, que vale conferir: o plano B cobre **toda** falha, e o vazio escolhido é o que o mod já fazia de propósito e não passa por aqui: o falso aviso de caçada (`PRENUNCIO real=nao`), a sensação de ser olhado sem ninguém (`VIGIA falsa`) e as cenas que não mostram ninguém antes da fase 3 (`PRESENCA PULADA`, que a cena do túnel passou a registrar). Não criei um sorteio de "plano B que não sai": para o jogador ele seria igual a uma falha. O analisador conta os dois separados e alerta se, em mais de uma hora de fase 3 ou 4, não houve nenhum vazio escolhido.

## A cota de corpo (`Cota`)

**A ideia.** O primeiro contato garante que ele seja visto uma vez. Depois disso, para o Diretor, "tentei" e "ele viu" contavam igual. A cota passa a contar o que foi visto.

**"Visto" tem a definição de sempre**, agora medida em toda aparição e não só na do contato (`HospedeEntity.foiVistoDeVerdade`): seis ticks seguidos na mira do jogador, com luz para enxergá-lo, ou dissolver com a mira em cima (o "visto demais" de quem olhou direto). Ficar no canto da tela não conta; uma virada de câmera não conta; olhar para onde ele está no breu não conta. A linha `EXPOSICAO` do log passou a dizer `naMira=` e `visto=`.

**A regra**, por jogador, depois do primeiro contato e da fase 3:

- um relógio na `Memoria` (`cota_relogio`) conta os segundos de jogo desde a última aparição vista. Zera quando qualquer aparição que não seja de comando é vista de verdade, e também numa caçada em que ele esteve ao menos um segundo na tela (ali ele é visto correndo, quase sempre no escuro, e ninguém pergunta onde ele está: é a única folga na definição);
- **aos 20 minutos** a cota fica devendo. A presença comum seguinte, que o Diretor escolher por conta própria, usa a colocação fácil antes de todas as outras (menos da isca) e é anunciada sempre pelos dois passos que dizem o lado, repetidos uma vez se ele ainda não olhou (o anúncio do primeiro contato, sem o aviso da caçada);
- **aos 30 minutos** ela não espera mais o Diretor: tenta sozinha, como o primeiro contato. Não passa pelo portão de vulnerabilidade nem pelo saldo de atenção; respeita o respiro, e não acontece com criatura presente, cena em curso, sequência de ameaça, Diretor recuando, Véu aberto, zona da vela, trégua ou jogador em perigo;
- a tentativa que ninguém viu **não gasta atenção** (só segura os outros pelo respiro); vista, custa o que custa uma presença. A seguinte espera de quatro a seis minutos, mais dois a cada tentativa, até dez a doze.

**A colocação fácil** (`Cota.colocar`): fora de casa, primeiro de perto, num lugar fechado **e com luz** (`Aparicao.buscarDePerto` ganhou o parâmetro `soComLuz`); senão, o lugar com luz que ele enxerga ao virar (`invocarOndeHaLuz`), perto da borda da tela (de 80 a 140 graus na primeira tentativa, de 62 a 110 nas seguintes), a 18 a 26 blocos (26 a 36 de dia, a céu aberto). Sem luz não há tentativa: no breu ele estaria lá e, pela definição, ninguém o teria visto.

**O que não muda.** Ele continua sendo relance (some ao ser olhado, e de perto nunca sai andando), nunca nasce na tela, e de perto só em lugar fechado. **A cota não põe ninguém dentro da casa do jogador**: com o contexto `CASA` ela pula a procura de perto. De dentro de uma casa com janela de vidro o lugar com luz quase nunca tem linha de visão (o vidro tem corpo), então para quem não sai de casa a cota espera; a janela é assunto da cena dela.

**O risco, e por que ele é aceito.** A colocação fácil é a que mais mostra, e o que aparece demais deixa de assustar (Grip, na pesquisa). A cota só existe enquanto ele **não** foi visto: com o mod funcionando ela não dispara nunca. Os números (20 e 30 minutos, a espera) são o palpite do cardápio, sem sessão de gente.

## Consertos de passagem

- **O vulto do Véu passou a exigir que o jogador o enxergue ao virar.** A procura de sempre, para ele, não pedia linha de visão: numa caverna ela o punha no bolsão ao lado, o log dizia `vulto=sim` e o jogador não via ninguém. Achado pelo teste novo (a cela fechada do teste ficava dentro de um bloco de pedra pequeno, e o vulto nascia no gramado do lado de fora). Em campo aberto não muda nada.
- **A ordem das linhas de log** nas cenas, no Véu e no Olho: `LUGAR falhou` vem antes do `SEM_LUGAR`, como na sequência de ameaça. O analisador só juntava o motivo à construção quando a ordem era essa.
- **O `README.md`** dizia alpha1. Agora aponta para o `gradle.properties` e o `VERSAO-ATUAL.md`, para não envelhecer a cada versão.

## O analisador

- Fontes novas no ritmo: `plano B` e `cota` (só a tentativa da própria cota; a que vem pelo Diretor já é um evento).
- Seção nova, "Nada termina em nada": vazio por falha (com e sem o sinal), de que lado o sinal veio e quantos à vista, vazio por escolha, o que a cota fez, e o maior tempo de jogo, da fase 3 em diante, sem uma aparição vista de verdade.
- "Na mira" passou a ser o `visto=sim` da linha `EXPOSICAO` (num log de antes da alpha20, o primeiro tick de `ENCAROU`, como era). O alerta "ele viu a criatura em N de M vezes" dispara abaixo de **duas em cada três** (era abaixo da metade): é a prova que o relatório pede para este pacote.
- Alertas novos: construção sem ninguém **e sem sinal**; mais de quarenta minutos, da fase 3 em diante, sem vê-lo; nenhum vazio por escolha em mais de uma hora de fase 3 ou 4.

## Os testes

Nove de servidor (78 no total) e quatro de JUnit.

`TestesDoPlanoB` monta os dois lugares em que a procura falha de verdade: uma cela de pedra fechada, de três por três, e um corredor sem saída de doze blocos que só vai para a frente do jogador.

- `naCelaFechadaAAmeacaAcabaEmPassos`: a sequência de ameaça espera o minuto dela, acaba em `SEM_LUGAR`, ninguém aparece, e o alvo recebe os dois passos (nenhuma partícula).
- `noCorredorSemSaidaCaiPoAVistaDele`: no corredor, olhando para o único lado com espaço, ninguém nasce; o fim é `SEM_LUGAR` com `planoB=PO`, o alvo recebe as partículas e o toque, e **o amigo parado ao lado não recebe partícula nenhuma nem som nenhum em volta do sinal**.
- `doisSinaisSeguidosNaoSaoOMesmo`: no corredor com uma tocha no fundo, o primeiro sinal é o pó e o segundo é a tocha; a tocha apagada é miragem (o bloco continua lá) e volta sozinha.
- `aCenaQueNaoAchaLugarTambemTemSinal`: a cena do túnel, na cela.
- `oVeuEmQueOVultoNaoCabeTambemTemSinal`: o Véu, na cela; o sinal sai com o Véu ainda aberto.

`TestesDaCota` usa o túnel de `TestesDePerto` (a curva iluminada dez blocos atrás do jogador).

- `aCotaTentaSozinhaESoCustaQuandoEVista`: com trinta minutos no relógio ele nasce na curva em menos de três segundos, pela cota; três segundos sem olhar e o saldo de atenção não caiu; olhado, some, o relógio zera e a atenção é cobrada.
- `semSerVistaACotaContinuaDevendo`: ninguém olha, ele vai embora aos quarenta segundos, o relógio não zera e a tentativa seguinte não vem em seguida.
- `aPresencaDoDiretorPagaACotaQuandoElaDeve`: sem a cota dever, a presença é a de sempre; com vinte minutos no relógio a cota não tenta sozinha, e a presença que o Diretor roda é a dela.
- `aCotaNaoValeAntesDaFase3NemDentroDeCasa`: na fase 2 o relógio nem anda; na fase 3, com a cama ali (contexto `CASA`), ninguém aparece de perto; tirada a cama, aparece.

`PlanoBTest` (JUnit): a escolha do sinal, à vista e fora dela, e a regra de não repetir.

Ganchos novos no `Diretor`: `planoBParaTeste`, `testarPlanoB` (também como `/sussurros teste planob`) e `eventoDoDiretorParaTeste`, que roda um evento como o Diretor rodaria (o `forcarEvento`, de comando, não conta para nada, e a cota depende de contar). `JogadorDeTeste.recebidos` separa sons, partículas e blocos trocados.

Duas coisas que os testes ensinaram, e que estão no `CLAUDE.md`: classe de teste nova só roda depois de entrar no `fabric.mod.json` de `src/gametest/resources` (a primeira execução passou com os 69 de antes, sem os novos); e o pacote que o servidor manda a um jogador só sai da fila na vez da conexão, então um teste que confere o que chegou tem de olhar no tick seguinte.

## O que ficou de fora, de propósito

- **A espreita, o "atrás de você" e as cenas não usam a colocação fácil da cota.** Só a presença comum e a tentativa da própria cota. É a menor mudança que cumpre a garantia.
- **A cota não entra em casa** (ver acima), e não usa a saída andando de propósito: a presença do Diretor continua com a regra de uma em três.
- **O primeiro contato** continua como estava (sem a procura de perto).
- **Um sinal próprio para cada cena.** O plano B é o mesmo nos quatro pontos. As ideias de caverna com corpo (pacote 3) são outra coisa: aqui o pó é pouco e curto de propósito.
- **O vazio escolhido como sorteio novo** (ver "O vazio por escolha continua").

## Não verificado por ninguém

- Se o pó aparece e se lê como pó. As fotos 43 a 45 mostram a tela (ver "As fotos"); o movimento, só jogando.
- Como soam o toque no teto e os dois passos, e se, a nove blocos atrás de uma parede, os passos parecem alguém ou parecem ruído. A máquina do GitHub não tem som.
- Os números da cota. Vinte e trinta minutos são palpite. O que ler na primeira sessão: o maior tempo sem vê-lo, quantas tentativas a cota precisou, e os segundos de exposição por hora (a referência da revisão externa é de 20 a 30 por hora, perto e com luz, na fase 3).
- Se a colocação fácil, de dia, a 26 blocos e na borda da tela, lê como presença ou como boneco. É a forma diurna do primeiro contato, que ele viu uma vez e não reclamou.
- O plano B dentro do Véu (as partículas com a neblina fechada e a cor do outro lado).

## Medição

Sessão sintética de uma hora, três jogadores de mentira, em campo aberto (11/10/2026, uma execução):

| | Fase 2 | Fase 4 | Mundo novo |
|---|---|---|---|
| Saídas por hora | 33 | 41 | 31 |
| Manifestações da criatura por hora | 10 | 9 | 12 |
| Tentativas da cota | 2 | 2 | 0 |
| Segundos na tela por hora (perto e com luz) | 5 (0) | 5 (1) | 5 (2) |

Na faixa das medições anteriores (26, 36 e 42 saídas na alpha19; 30, 39 e 49 na alpha16). Nenhuma construção acabou sem ninguém (em campo aberto há lugar), então **o plano B não apareceu na sessão**: ele só é exercitado pelos testes de servidor. A cota tentou duas vezes por hora em cada um dos dois jogadores que já tinham o primeiro contato, sempre pela regra (aos 30 minutos), a 32 blocos, de dia; nenhuma foi vista, porque jogador de mentira não mira em nada. É o pior caso: duas aparições a mais por hora para quem nunca olha. No jogador da fase 4 a cota foi paga pela caçada.

Três avisos sobre esta medição:

- **A sessão mudou, e por isso não se compara linha a linha com as anteriores.** Os dois jogadores que começam nas fases 2 e 4 passaram a começar com o primeiro contato feito (`TestesDeSessao`), como num mundo de verdade. Antes, sem mirar em nada, eles recebiam tentativas de primeiro contato a hora inteira (oito cada um, numa execução desta versão feita antes da mudança: 42, 48 e 26 saídas por hora), e a cota, que só vale depois do contato, nunca entrava.
- Em duas execuções, **a presença comum não foi sorteada nenhuma vez** em campo aberto (as aparições foram vulto, espreita, "atrás de você", caçada e as tentativas de contato ou da cota). O caminho "a presença do Diretor paga a cota" só foi exercitado pelo teste de servidor.
- Os alertas "ele viu a criatura em N de M" e "mais de quarenta minutos sem vê-la" disparam sempre para jogador de mentira. Valem para sessão de gente.

`./gradlew build`: ver o corpo do PR para a contagem exata. O teste do amigo ao lado falhou duas vezes antes de ficar certo, por defeito do teste: ele contava todo som que chegava ao amigo, e o amigo recebe sons que não são do plano B (cinco de uma vez, numa execução; não descobri quais eram, e a hipótese é um bicho andando em cima do bloco de pedra). Passou a conferir pelo lugar: o toque do pó vem do teto, oito blocos à frente, e é esse som que o amigo não pode ter recebido.

## As fotos (43 a 45)

Tiradas no corredor sem saída dos testes (pedra, doze blocos, uma tocha no fundo), com `/sussurros teste planob`. Abri as três.

- **43, o corredor antes:** escuro perto do jogador, a tocha acesa no fundo. É a referência.
- **44, o pó:** um punhado de pontos claros junto do teto, oito blocos à frente, um pouco acima da mira, contra a parede iluminada pela tocha. Vê-se, e lê-se como algo no teto; é pequeno, do tamanho de um bloco visto a oito blocos, que é o que se queria (um sinal, não um efeito). A cor é a da pedra. **No escuro, sem uma luz atrás, eu não apostaria que se vê**: nesta foto ele aparece porque a tocha do fundo o recorta. Num bolsão sem tocha o que sobra é o toque no teto, que só se confere ouvindo.
- **45, a tocha apagada:** a tela fica preta, só com a barra de itens. Era a única luz do corredor. É bem mais forte do que eu esperava de "uma tocha apaga por uns segundos": quando a tocha escolhida é a única por perto, o jogador fica de quatro a sete segundos no breu. Não mexi: é reversível, é curto, e é só para o alvo; mas é o primeiro ponto a perguntar a ele (sem dizer o quê) e o primeiro a rever se parecer defeito do jogo.

O que as fotos não dizem: o movimento do pó caindo (são três punhados em um segundo) e o som.

---
# Versão 0.9.0-alpha21 — a primeira travessia, garantida (pacote 2)

Segundo pacote do plano de 11/10/2026: a ideia 6.1 do cardápio. O fato a que responde: nenhuma visita ao Avesso em duas horas e meia de mundo, com o jogador na fase 4 e dormindo. É o maior pedaço do mod que o dono não viu, e ele perguntou por ele ("a dimensão é isso?", sobre o Véu).

## O que faltava

As duas portas eram sorteio: quinze em cem ao deitar, da fase 3 em diante, e trinta e cinco em cem das capturas, mas só depois de já ter ido uma vez. Quem dorme dez noites tem uma chance em cinco de nunca ter ido; quem dorme pouco, como ele, pode passar semanas.

## A regra

- Da fase 3 em diante, enquanto ele **nunca foi**, cada dia de jogo em que ele deita conta uma noite (`avesso_noites` na `Memoria`; deitar duas vezes na mesma noite conta uma só). O sorteio de quinze em cem continua valendo nas duas primeiras. **Na terceira ele vai, sem sorteio.**
- Só a primeira visita. Da segunda em diante nada muda: três dias de jogo no mínimo, oito em cem, a carta `SONO` do baralho e a captura.
- Se ele levanta antes dos dois segundos de olhos fechados, não é levado (como sempre), e a noite seguinte em que deitar leva.
- **A garantia não é saída de graça de uma caçada.** Se ele deita sendo caçado na noite em que a garantia valeria, ela fica para a próxima (`AVESSO garantia adiada`). O sorteio e a carta continuam como eram nesse caso: "deitar na cama no meio da caçada" é uma das três decisões da caçada que ficaram para o dono, e eu não quis decidir por ele nem para um lado nem para o outro.
- A decisão é uma função pura (`Avesso.decidir`), e o número do jogador só é gasto quando ela depende dele (`dependeDoSorteio`).

**A carta `SONO`** não mudou. Ela continua sendo o jeito de ir antes da terceira noite e o jeito de voltar lá fora de hora; o relatório a citava entre o que este pacote "estende", e o que conferi é que ela convive com a garantia (a carta passa por cima de tudo, uma vez).

**A primeira visita continua quieta** (de 45 a 75 segundos, só o lugar). O relatório sustenta isso ("you need to go in blind") e aponta o risco: ser lida como "é só isso?", a pergunta que ele já fez sobre o Véu. O que responde a esse risco é o pacote 8, não este.

## O marco

O relatório acrescenta à 6.1 a ideia de **marco vivido**: a primeira ida passa a ser o que abre o que vem depois (o pacote 6, o que fica na casa, só começa depois dela). O marco já existe na `Memoria`: `avesso_visitas >= 1`, que só sobe quando ele volta de uma visita de verdade. Não criei outra chave. Quem fizer o pacote 6 lê essa.

## Os testes

- **JUnit** (`AvessoTest`, cinco testes): quem nunca foi vai na terceira noite mesmo sem sorte; antes dela o sorteio continua valendo; a garantia é só da primeira visita; a carta passa por cima de tudo; o sorteio só é gasto quando vale.
- **Servidor** (`TestesDoAvesso.aPrimeiraIdaFicaGarantidaNaTerceiraNoite`, 79 no total): a contagem na `Memoria` de um jogador de verdade. Antes da fase 3 a noite não conta; duas vezes no mesmo dia contam uma; a terceira noite dá `GARANTIDA`; depois da primeira visita, sem sorte, nada. O servidor de teste não carrega a dimensão: aqui ninguém é levado.
- **Cliente** (`TestesDeCliente.primeiraIdaGarantida`, no job `fotos`): o caminho de verdade. Uma cama, três noites de jogo, o sorteio do jogador forçado para "sem sorte"; nas duas primeiras ele continua no mundo normal, na terceira está do outro lado, na visita 1, e a volta conta a visita. Sem foto nova: não há nada novo na tela. Roda antes de a casa das fotos ser montada, porque a volta apaga por uns minutos as luzes que houver por perto.

## O analisador

- Na seção "Caçadas, capturas e o outro lado": quantas vezes ele deitou antes da primeira ida, a que noite chegou, e as decisões.
- Alerta novo: a garantia foi decidida e ele não chegou lá.

## Não verificado por ninguém

- Se três noites é o número certo. É o do cardápio, sem sessão.
- Se a primeira visita, vindo cedo e com data marcada, parece um acontecimento ou parece "é só isso?".
- O caso em que ele joga sem nunca dormir: a garantia depende de deitar. O analisador diz quando não há nenhuma linha `AVESSO deitou`.

---
# Versão 0.9.0-alpha22 — a caverna com corpo (pacote 3, primeira parte)

Terceiro pacote do plano de 11/10/2026: as ideias 3.1, 3.2, 3.3 e 3.8 do cardápio. O fato a que responde, nas palavras do dono: "na caverna achei que os eventos e coisas ficaram muito focados em mudar a tocha de cor, e sons, poderiam ter mais interações físicas".

## O que o log dele dizia

O analisador ganhou a conta das famílias e eu a rodei no log da sessão de 11/10 (alpha17, duas horas e meia). Do que saiu com ele **debaixo da terra**: 40 coisas, **21 de luz (52%)**, 8 de som, 5 "de cabeça" (a borda escura, o texto, o Véu), 3 marcas, 1 objeto e 2 aparições. Das 21 de luz, **18 eram chamas pálidas**. A queixa era literal. Em casa foi parecido (30 de 52 foram chamas), mas a casa fica para o pacote 6.

Um segundo achado, que mudou o desenho: nessa sessão inteira houve **14 presságios e 1 perturbação** da Atmosfera. O saldo do ambiente volta devagar, o presságio custa 14 e a perturbação custa 20: o presságio leva o saldo assim que ele chega a 14, e a perturbação quase nunca tem a vez. É o erro da alpha12 ("o mais barato ganha sempre") dentro do saldo do ambiente. Não mexi nisso (é número de ritmo, vale em todo lugar e pede sessão sintética própria), mas é por isso que a segunda regra abaixo não espera saldo: como perturbação comum, a pedrinha e os morcegos quase nunca sairiam.

## As famílias (`Familias`)

Tudo o que o mod empurra para o jogador cai numa de seis famílias: as cinco do cardápio (luz, som, marca, objeto, corpo) e mais uma, **cabeça**, para o que não acontece no mundo (`VIGIA`, `SUSSURRO`, `VEU`, `NEBLINA`, `ECO_CHAT`, `VISTO`). Sem a sexta, essas coisas teriam de entrar em "som" ou "luz" e falseariam a conta. Quem faz a saída chama `Familias.registrar`: os eventos (em `posEvento`), toda aparição (em `Diretor.criar`, uma vez só: o evento que a pediu não registra de novo), os presságios e perturbações, as chamas pálidas, o plano B, a rachadura e o começo da cena do marco (as outras cenas registram os seus passos pelos eventos que já chamavam). O que vem de comando não entra.

As regras valem **só com o contexto `SUBSOLO`**:

1. **Nenhuma família passa de duas nas últimas cinco saídas.** O cardápio dizia "nunca três seguidas". Com essa regra uma família ainda podia ser dois terços do que sai (luz, luz, outra, luz, luz, outra), e a prova que a pesquisa pede é ninguém acima de 40%. Com duas em cinco o teto é um terço, e o "nunca três seguidas" vem junto. É desvio do cardápio, e é o número que eu mudaria primeiro se a caverna ficar calada.
   - **Objeto e corpo nunca são barrados.** A regra existe para tirar luz e som de cima; segurar o que faltava seria o contrário do pacote.
   - Quem pergunta é quem ia fazer a saída. O **sorteio do Diretor** tira do sorteio os eventos da família barrada, e **cede** quando só sobram eles (variedade não pode virar silêncio: o Diretor nunca fica mais quieto do que ficaria). Os **presságios e perturbações** pulam a opção barrada. As **chamas pálidas não cedem**: a tocha espera até outra família ter a vez. É a peça que mais pesa, pelo log. Só o acréscimo de uma carta do baralho (várias tochas de uma vez) passa por cima.
   - Elos de cadeia, a sequência de ameaça, as cenas, o primeiro contato, a cota e o plano B não são barrados (são composições ou regras); só registram.
2. **A dívida do subsolo.** A cada quinze minutos de subsolo sem nada de objeto nem de corpo (`subsoloSemFisico`, que só anda no subsolo e não zera ao sair), a próxima coisa do ambiente é uma das que se tocam: os morcegos e a pedrinha se revezam, e quando as cotas deles acabam (ou não há lugar) vale a pedra que aparece no caminho por onde ele veio (`O_CAMINHO_MUDOU`, que já existia). É regra, como a cota de corpo: não espera o saldo do ambiente, só o respiro, a trégua, o Véu e o Diretor recuando; gasta o saldo depois. Sem lugar, tenta de novo em um minuto. Uma aparição dele também paga (é corpo).

Por que só no subsolo: na superfície quase tudo o que o Diretor sorteia é som, e barrar ali mudaria a mistura inteira. A sessão sintética é em campo aberto e não muda por construção (ver "Medição").

## A caverna (`Caverna`)

- **A rachadura (marca, 3.1).** Ele minera; no bloco ao lado do que acabou de quebrar, rachaduras crescem em um segundo até a metade e param, com duas batidas surdas (`PANCADA`, volume 0,6) vindas do bloco. Somem em quatorze segundos. Se ele quebra o bloco: pedra, e nada atrás (a marca sai junto, e o log diz `QUEBROU_O_BLOCO`).
  - É o `ClientboundBlockDestructionPacket` do jogo mandado só pela conexão dele, com um número negativo (os de entidade são positivos): o mundo não muda e quem está ao lado não vê. Etapas 0 a 4 de 9.
  - Só em pedra natural (a tag `base_stone_overworld`: pedra, granito, diorito, andesito, tufo, ardósia): nunca num bloco que ele possa ter posto, num baú ou num minério. De preferência um vizinho de lado (o de trás é o próximo que ele vai quebrar), com a face nova virada para ele.
  - Quando: contexto `SUBSOLO`, fase 2 ou mais, noventa segundos de descida, minerando (três quebras em meio minuto), na quarta à oitava quebra que conta (sai do segundo em que a descida começou, não de sorteio). **Uma por descida**, e dez minutos no mínimo entre duas. O que cala os outros sistemas cala este: respiro, Diretor recuando ou ameaçando, cena, criatura presente, trégua, Véu, vela.
  - É resposta a uma ação dele: não gasta atenção, só impõe o respiro.
- **A pedrinha (objeto, 3.2).** Alguma coisa pequena quica no chão: a 3,4 blocos, a 1,7 e um resto de rolar, com lascas do chão do lugar (partícula de bloco) e um estalo em cada toque, cada um mais agudo e mais fraco. Só para ele: partículas e som pela conexão dele. Sem item, sem entidade, sem dano. Vem do lado em que o espaço continua mais fundo e termina mais escuro, de preferência à frente dele (onde ele vê as lascas). **Duas por sessão**, vinte minutos entre uma e outra.
  - **O som é o `ESTALO` que o mod já tinha, mais agudo** (tom de 1,5 a 1,95). O cardápio pede "um estalo seco" e avisa que não pode parecer goteira nem cascalho do jogo. Não gerei um som novo: ninguém aqui ouve, e o ambiente de gerar sons não está montado nesta pasta. Se ele disser que pareceu galho, o conserto é um som próprio em `ferramentas/sons`.
  - Não usei o `item_display` opcional do cardápio: seria uma entidade de verdade, que todos veem.
- **Os morcegos (corpo, 3.3).** Um baque surdo (`GRAVE`, só para ele) numa boca de túnel fora da tela, e meio segundo depois de três a cinco morcegos saem de lá, passam pela cabeça dele e seguem. A boca é o lugar que a procura de perto daria à criatura (`Aparicao.bocaDeTunel`: de 8 a 14 blocos, fora da tela, lugar fechado, com linha livre até os olhos dele), sem contar como falta de lugar quando não acha.
  - **São morcegos de verdade: todos veem e ouvem.** É a única coisa do pacote que não é só do alvo. Não fazem mal. O voo é conduzido por três segundos e meio (o morcego do jogo voa para pontos sorteados; aqui a velocidade é reposta a cada tick) e depois é um morcego qualquer.
  - Etiquetados (`sussurros_morcego`) e tirados em dez a quinze segundos: depois dos dez, assim que o jogador não os vê; aos quinze, de qualquer jeito. Ao fechar o mundo saem todos. Um que tenha ficado salvo (queda do jogo) é tirado quando o chunk carrega.
  - **Três revoadas por sessão**, vinte minutos entre uma e outra.
  - **"A próxima coisa com lugar acontece naquele túnel."** Por dez minutos, a procura de perto dá cinco pontos a mais (mais do que todo o resto somado) aos lugares a até três blocos e meio da boca. Ninguém é chamado por causa dos morcegos: se uma aparição de perto vier nesse tempo, vem dali.

Os dois últimos são perturbações da Atmosfera (`PEDRINHA`, `MORCEGOS`): entram no sorteio dela quando o contexto é `SUBSOLO` e são o que paga a dívida. Comandos de teste: `/sussurros cena rachadura` (racha o bloco para onde ele olha), `cena pedrinha`, `cena morcegos`.

## Os testes

- **JUnit** (`FamiliasTest`, seis testes): duas nas últimas cinco barram; o que saiu há mais de cinco não conta; objeto e corpo nunca são barrados; quem tenta a mesma família sempre que pode fica com um terço do que sai; tudo o que o mod faz tem família; os números da rachadura e dos morcegos ficam na faixa.
- **Servidor** (`TestesDaCaverna`, seis testes; 85 no total), todos num túnel de pedra montado pelo teste:
  - `minerandoOBlocoAoLadoRacha`: na fase 0 nada racha; na fase 2, minerando, o vizinho de lado racha na quebra prevista; as cinco etapas e as duas batidas chegam ao alvo e **não chegam ao amigo colado nele**; o bloco continua pedra; nenhuma segunda rachadura na mesma descida; quebrado o bloco, a marca sai.
  - `aDividaDoSubsoloTrazOQueSeToca`: com o relógio em quinze minutos, os morcegos vêm em três segundos, de três a cinco, da curva do túnel; passam a menos de três blocos dele; não sobra nenhum em quinze segundos. Com a dívida aberta de novo, a pedrinha vem quando o respiro deixa: partículas e três estalos à frente do alvo, nada para o amigo, o chão igual.
  - `aProximaAparicaoPrefereOTunelDosMorcegos`: sem morcegos ele nasce na curva; com os morcegos tendo saído de outro trecho (o teste tampa a curva enquanto eles saem), a aparição seguinte nasce junto desse trecho e não na curva.
  - `debaixoDaTerraALuzEsperaAVez`: com duas de luz registradas, vinte e dois segundos sem nenhuma chama pálida, com três tochas às costas dele; destravada, a chama vem.
  - `naSuperficieARegraDasFamiliasNaoVale` e `oMorcegoQueSobrouETirado`.
- **Cliente** (`TestesDeCliente.caverna`, fotos 46 a 49): a rachadura, a pedrinha em dois toques e os morcegos, no mesmo túnel.

## As fotos (46 a 49)

Abertas e conferidas, do job `fotos` do PR #43.

- **47, a rachadura.** Desenha: o risco de quebra do jogo aparece no bloco do fundo, na metade. Era a dúvida que só a foto tirava (nenhum código aberto lido manda esse pacote só para um jogador, com um número que não é de entidade). Com a luz de uma tocha a dois blocos, lê-se bem.
- **48, a pedrinha (dois toques).** As lascas aparecem nos dois toques, do tamanho das de um bloco sendo quebrado. Na foto são marrons porque o chão do túnel de teste é o gramado do mundo plano; numa caverna de verdade saem da cor da pedra. Se parecerem grandes demais para "uma pedrinha", é o número de lascas (4, 5 e 2) que se mexe.
- **49, os morcegos (vindo e passando).** Vêm juntos pelo meio do túnel, escuros contra a luz da curva, e crescem na tela de uma foto para a outra. Num túnel de um bloco eles ficam empilhados; em lugar largo se espalham para os lados (isso nenhuma foto mostra).

## O analisador

- Seção nova, "As famílias do que saiu, por lugar": a tabela de famílias por tipo de lugar, a família que mais saiu no subsolo com o que mais pesou nela, o maior tempo de subsolo sem nada que se toque, as dívidas pagas, quantas vezes a regra barrou e cedeu, e as três coisas da caverna. Num log de antes da alpha22 a família é estimada pelo nome das linhas (é assim que saiu o número de 11/10).
- Dois alertas: uma família acima de 40% do que saiu no subsolo (com dez saídas ou mais), e mais de vinte minutos de subsolo sem nada que se toque.

## Consertos de passagem

- `TestesDeServidor.oSomDaAssombracaoESoDoAlvo` contava qualquer som que chegasse ao amigo em três segundos, e no gramado do mundo de teste há cavalos e vacas andando: falhou uma vez em cinco execuções locais, sem nada do mod ter tocado. Agora conta só o que não é som de bicho (`JogadorDeTeste.sonsQueNaoSaoDeBicho`).
- Dois achados sobre os testes de som, que estão no `CLAUDE.md`: um morcego do mundo guincha para todos (contar pelo nome do som), e a posição de um som chega arredondada quando o jogo põe as estruturas de teste muito longe da origem (dar folga ao lugar).

## O que ficou de fora, de propósito

- **3.4, a passagem que de longe parece fechada.** É miragem sólida: se ele encostar antes de ela se desfazer, o jogo o puxa de volta e parece travamento. É a de maior risco do pacote e pede terreno de teste próprio.
- **3.5, a tampa do baú.** Continua sem código conferido; e o baú do acampamento é coisa dele (encosta no pacote 6).
- **"Alguém esteve aqui antes"** (tochas e sinais de outra pessoa na caverna). Põe bloco de verdade em caverna natural: é decisão do dono, e a pergunta ainda não foi respondida. Sem resposta, nenhum bloco de verdade.
- O saldo do ambiente (presságio contra perturbação), a regra das famílias em casa, e a definição de subsolo (hoje: sem céu **e abaixo do Y 50**; uma mina de encosta no Y 55 não é subsolo para o mod). São três achados desta versão que ficam anotados.

## Não verificado por ninguém

- Se a rachadura se lê como "alguma coisa bateu do outro lado" ou como defeito do jogo. As fotos dizem se desenha; o resto é com ele.
- Como soam as batidas e o estalo da pedrinha.
- Se os morcegos passam "bonito" num túnel largo e numa caverna aberta: o teste é num túnel de um bloco, onde eles se espremem no teto.
- Se duas em cinco é apertado demais numa caverna sem tochas, nas fases 1 e 2, em que quase tudo o que existe é som (o Diretor cede, mas as outras fontes não).
- Os quinze minutos da dívida e os dez do túnel dos morcegos: palpites do cardápio, sem sessão.

---
# Versão 0.9.0-alpha23 — a caverna com corpo (pacote 3, segunda parte)

O que tinha ficado de fora da alpha22 e não dependia de resposta do dono: as ideias 3.5 e 3.4 do cardápio. São mais duas coisas da família "objeto" para o subsolo, que era a que a caverna dele menos teve (uma em quarenta saídas).

## A tampa do baú (`Caverna.tampa`, 3.5)

Um baú que ele pôs lá embaixo levanta a tampa, fica um segundo aberto e fecha, com o som de baú abrindo e fechando. Dentro, tudo igual.

- É o evento de bloco do jogo (`ClientboundBlockEventPacket(pos, bloco, 1, 1)`, e depois com 0) mandado só pela conexão dele, com os dois sons também só para ele. O baú de verdade não abre: o contador de quem o tem aberto fica em zero, quem está ao lado não vê, e nada dispara (um baú com armadilha não dá sinal). O cardápio marcava a ideia como "conferir"; conferi no código do jogo e a foto mostra a tampa levantada.
- Qual baú: de dois e meio a quatorze blocos, fora da zona da vela, e que ninguém esteja usando de verdade. De preferência um que esteja **na borda da tela** (é onde se percebe "alguma coisa mexeu ali"); depois um fora da tela (ele só ouve, e vira); por último um bem no meio. Se alguém abre o baú de verdade nesse segundo, o mod não manda o "fechar".
- **Só com o contexto `SUBSOLO`.** O cardápio dizia "no acampamento da mina ou em casa"; a parte de casa a pesquisa põe no pacote 6 (o que fica na casa), e aqui ela ficou de fora de propósito: o baú de casa é o lugar seguro dele, e mexer ali pede o desenho daquele pacote (raridade, a vela que trata).
- Duas por sessão, vinte minutos entre uma e outra. Vale para baú comum, com armadilha e de cobre (todo `ChestBlock`); barril e baú do Fim não.

## A passagem fechada (`Caverna.passagem`, 3.4)

O caminho por onde ele veio, visto de longe: há pedra onde ficava a passagem. Quando ele chega a seis blocos, está aberto.

- São miragens (`Miragem`, motivo `PASSAGEM_FECHADA`): só ele vê, o mundo não muda, e clicar nelas as desfaz, como toda miragem.
- Onde: num ponto do `Rastro` (ele passou por ali há dez segundos ou mais), de dez a vinte e seis blocos dele, **fora da tela na hora de pôr** e **à vista se ele virar** (linha livre até os olhos). De preferência a uns dezesseis blocos.
- Só numa **passagem estreita**: o vão, no plano atravessado ao caminho, tem de ser de ar, ter no máximo seis blocos e estar todo cercado de bloco inteiro (`Caverna.vaoDaPassagem`). Um salão, uma bifurcação, um vão com tocha, escada, trilho ou laje não servem: ali a parede falsa ficaria com buraco, ou esconderia uma coisa dele.
- A pedra é a pedra natural que mais cerca o vão (senão, pedra comum), para a parede ter a cor do túnel.
- **O cuidado que o cardápio pedia.** Bloco de miragem é sólido para o jogo dele: encostando, ele ficaria preso num bloco que não existe, e isso parece travamento. Por isso ela se desfaz a **seis blocos** (o cardápio pedia "2,5 ou mais": a miragem só confere a cada cinco ticks, e correndo ele anda um bloco e meio nesse tempo), e nunca é posta a menos de dez. Nunca a três blocos de água ou de lava (o líquido "atravessaria" a pedra). Dura de sessenta a noventa segundos se ele não for ver.
- Duas por sessão, vinte minutos entre uma e outra, e nunca duas ao mesmo tempo.

As duas são perturbações da Atmosfera (`TAMPA`, `PASSAGEM_FECHADA`) e pagam a dívida do subsolo, junto com os morcegos e a pedrinha. A dívida passou a tentar primeiro a que saiu menos vezes na sessão (`Atmosfera.ordemDaDivida`, função pura); a pedra no caminho continua sendo a última. Como perturbações de caminho que são, não acontecem com a criatura presente, numa cena, numa caçada, em trégua nem com o Véu aberto. Comandos de teste: `/sussurros cena tampa` e `cena passagem`.

## Os testes

- **JUnit** (`FamiliasTest`, um teste a mais, sete no total): a ordem em que a dívida tenta as coisas, e que todas são das famílias que se tocam.
- **Servidor** (`TestesDaCaverna`, dois testes a mais; 87 no total):
  - `aTampaDoBauLevantaSoParaEle`: o evento de abrir e o de fechar chegam ao alvo, com um som cada; nada chega ao amigo colado nele; o baú de verdade não abre em momento nenhum; o que está dentro continua lá.
  - `aPassagemPorOndeEleVeioPareceFechadaDeLonge`: o jogador de mentira anda quinze blocos por um túnel (com os pés no chão, para deixar caminho); duas pedras de miragem fecham o túnel entre dez e vinte e seis blocos dele; o túnel de verdade continua aberto; quando ele volta, a parede se desfaz com ele a mais de quatro blocos e meio, e ele nunca chega mais perto do que isso de uma pedra que não existe.
- **Cliente** (`TestesDeCliente.caverna`, fotos 49t e 49p): a tampa levantada e de volta; a passagem fechada de longe e aberta de perto.

**Esta versão foi conferida no GitHub, não na máquina do dono.** A compilação e os testes JUnit rodaram aqui; o servidor de teste não subiu, porque a máquina estava sem memória livre (o arquivo de paginação no limite, com o navegador, as IDEs e vários chats antigos abertos). Os testes de servidor rodaram no job `build` do PR #44, três execuções do mesmo commit, as três com os 87 passando, e as fotos no job `fotos`. A sessão sintética não foi rodada: nada desta versão acontece em campo aberto, como na alpha22.

## As fotos (49t e 49p)

Abertas e conferidas, do job `fotos` do PR #44.

- **49t, a tampa.** Levantada na primeira foto e de volta na segunda: o evento de bloco mandado só para um jogador abre a tampa no jogo dele, com o baú de verdade fechado (o teste confere o contador em zero na mesma hora).
- **49p, a passagem.** De longe (onze blocos e meio), o túnel termina numa face de pedra escura onde antes se via a curva iluminada. De perto (cinco blocos), a parede já se desfez e vê-se a curva de novo. De longe ela é **escura**: a pedra falsa tapa a luz que vinha de trás, e lê-se mais como "o túnel acaba ali" do que como parede nítida. É o efeito pedido, mas é sutil; com uma tocha dele perto do ponto fica mais clara.

## Um achado sobre a sessão sintética

O jogador de mentira não tem os pés no chão para o jogo (`onGround` falso), e o `Rastro` só anota quem está no chão: **na sessão sintética o caminho andado tem sempre zero pontos**, e nada do que depende dele acontece lá (vestígios, pegadas, o seguidor, a trilha, as cenas que usam o rastro). É por isso que a família "marca" não aparece na tabela dos três jogadores sintéticos. O teste da passagem liga isso à mão (`setOnGround(true)`). Ligar na sessão sintética mudaria todos os números dela de uma vez: fica para quando alguém for mexer em ritmo, com a comparação antes e depois.

## O que ficou de fora, de propósito

- **"Alguém esteve aqui antes"** (blocos de verdade postos em caverna natural). Continua esperando a resposta do dono. A versão só em miragem já existe há tempo (a tocha que ele não pôs).
- A tampa do baú em casa (pacote 6).
- Barril e baú do Fim.

## Não verificado por ninguém

- Se a passagem fechada se lê de longe como "fecharam o caminho" ou só como um trecho escuro: a foto mostra um caso, com tocha por perto.
- Se a parede some "na cara" dele: a seis blocos, andando, ele pode estar olhando para ela quando ela se desfaz. Toda miragem do mod faz isso, e ele disse em 08/10 que perceber uma coisa piscando "é justamente isso que eu queria"; mas uma parede inteira é maior que uma tocha.
- O caso em que a parede fica entre ele e a saída enquanto ele é surpreendido por outra coisa: a caçada não começa com perturbação em curso, mas uma que comece depois encontra a miragem ainda lá, até ele chegar a seis blocos.
- Como soa a tampa (são os sons do próprio jogo).

---
# Versão 0.9.0-alpha24 — ele passa (pacote 4, primeira parte)

O pacote 4 do plano ("ele passa, e ele guarda") é médio a grande e mexe na caçada, que é a parte mais instável do projeto. Entra em duas versões, como o pacote 3: esta é a da caçada (a ideia 2.7 do cardápio, com a página da 7.4); a "afronta" como estado de dias (2.8) fica para a alpha25.

O que ele disse em 11/10: "quando ele apareceu mais fortemente na caverna e eu ataquei ele, ele estava preso num local", e pediu que ele se espremesse ou agachasse para passar por um ou dois blocos.

## O que a falha de 11/10 era, e o que não era

Na caçada de 11/10 (`M002`, aos 5520 s) a busca escreveu `CAMINHO_FALHOU` três vezes em sete segundos. O cardápio pedia para reproduzir isso no terreno de verdade antes de qualquer pose nova. Foi feito, e o resultado mudou o que entrou.

- **Exportei a galeria** em que a caçada aconteceu (`src/gametest/resources/terrenos/galeria.txt`, 46 × 14 × 50 blocos, sem coordenadas) e pus a criatura onde o log diz que ela nasceu (a boca de um túnel, no alto). Atenção ao ler posições do log: `pos=(x,y,z)` arredonda o centro do bloco, e em coordenada positiva isso dá o bloco seguinte (o `z=248` do log era o bloco 247).
- **Com o código da alpha23**, dos 881 lugares da galeria em que alguém fica de pé, só 38 a que um jogador chega andando ficaram sem caminho para ela, todos no fundo do corredor de baixo, a mais de 64 blocos **andando** (é o limite do jogo para o comprimento de um caminho, e vem do alcance de 64 da criatura); e mesmo ali ela chegou, porque o caminho parcial a leva para perto e a conta seguinte fecha. Com o jogador parado em onze lugares diferentes, a caçada chegou aos onze sem um `CAMINHO_FALHOU`. **Não reproduzi a falha.** Não era "ele não cabe".
- **O que `CAMINHO_FALHOU` quer dizer, lido no código do jogo (26.2).** `PathNavigation.moveTo` devolve "não" em dois casos: quando não há como calcular (a criatura está no ar e fora d'água: `canUpdatePath`), e quando o caminho novo é **igual** ao que ela acabou de andar (`sameAs`), porque aí o caminho guardado, já terminado, não é trocado. O segundo caso é ele parado no ponto mais perto do alvo a que o corpo e os limites dele deixam chegar: um vão de um bloco no meio, uma queda de mais de três blocos (o jogo não deixa uma criatura planejar queda maior), mais de 64 blocos de caminhada, ou o orçamento de 1024 nós da conta. Três "não" seguidos e a busca vai investigar um ponto sorteado.
- **Por que em 11/10 isso não teve resposta.** Era a primeira caçada da vida dele, e na primeira o atalho e o atravessar ficam desligados de propósito. E o "preso" que ele viu foi sobretudo a regra: ele o encarou, e encarado ele não anda (as três piscadas o trouxeram de 13,6 para 4,8 blocos). Ele não sabia a regra, e bateu.

Como não reproduzi, **não mexi em nenhum limite** (queda, comprimento de caminho, orçamento). O log passou a dizer qual dos dois casos foi e onde (a linha `BUSCA ... sem_caminho`, abaixo), e o analisador abre com alerta quando for o segundo. Na próxima caçada dele em que isso acontecer, dá para conferir o lugar no mundo salvo.

## De quatro (2.7)

Ele tem um terceiro corpo: **0,7 × 0,9** (`HospedeEntity.CORPO_DE_QUATRO`). Com ele, um vão de um bloco deixa de ser parede.

- **O caminho é medido de quatro.** O jogo calcula o caminho com o tamanho que a criatura tem na hora da conta. `NavegacaoDoHospede` (a navegação dele; é a de sempre com um método a mais) troca o tamanho que a conta lê, só durante a conta, por `fixupDimensions`: a caixa de colisão e o lugar dele não mudam, e nada vai para o cliente. Vale nos estágios da caçada em que ele anda (`PERSEGUE` e `ESPERA_VELA`); fora da caça a navegação é a de sempre.
- **O corpo de verdade só muda onde o lugar pede** (`Cacada.ajustarCorpo`, a cada tick em que ele não está na tela): de quatro quando o corpo abaixado não cabe onde ele está, ou quando não cabe num dos dois próximos passos do caminho (a até dois blocos), ou quando o passo é um degrau para cima debaixo de teto baixo. Levanta-se um quarto de segundo depois de caber de novo.
- **Ele nunca troca de corpo na tela.** Olhado, ele não anda nem muda: o jogador vê uma pose de cada vez. É a ideia do cardápio ("a cada piscada, uma pose"), e dispensa animar o rastejar.
- **De quatro ele é lento:** 55% do passo (`LENTO_DE_QUATRO`). O Cave Dweller faz o mesmo (0,35 contra 0,5), e é no vão que o jogador tem tempo de vê-lo entrar.
- **O aviso.** Um vão é a proteção de alguém quando está perto dele. Se a boca do vão está a até oito blocos do jogador (no chão), ele para ali de quatro por um segundo e meio a três, e algo se arrasta e arranha na boca (os sons `ARRASTO` e `ARRANHAR`, que já existiam, e um pouco de cinza), só para o alvo. É o papel da maçaneta atrás de uma porta: toda resposta dele avisa. Longe do jogador o vão é só caminho, sem aviso; quem vem de longe por um túnel de um bloco avisa quando o trecho que falta entra nos oito blocos. Não avisa de novo antes de dez segundos.
- **Vale na primeira caçada.** A primeira caçada não tem atalho nem atravessar porque são exceções à regra. Entrar por um buraco aberto não é exceção: é andar.
- **O desenho** (`HospedeModel.posarDeQuatro`): o tronco deitado, a cabeça erguida e torta olhando para a frente, um braço estendido adiante, o outro recolhido ao longo do corpo, as pernas arrastadas atrás. O braço da frente troca a cada bloco (pela paridade do lugar): entre uma olhada e outra, as poses parecem um rastejar. O topo da cabeça fica a uns 14 pixels do chão. O desenho é muito mais comprido que a caixa de colisão (uns quatro blocos e meio), então a caixa de corte da tela foi aumentada (`getBoundingBoxForCulling`).

**Onde me afastei do cardápio.** Ele sugeria uma caixa de 0,6 × 0,9; usei 0,7 de largura, igual à dos outros corpos, para trocar de corpo nunca empurrá-lo para o lado (o jogo reposiciona quem cresce para os lados). E o cardápio falava em três poses escolhidas "pela altura livre da célula": de pé (3,0) continua não existindo na caça, que já era abaixada (1,9).

## Duas brechas da caçada, fechadas

A regra da caçada é fechar o que a encerra de graça. Achei duas ao testar, e a segunda existia sem vão nenhum.

- **Encarar por um buraco.** O avanço do piscar ia em linha reta e parava em qualquer coisa com corpo. Com o caminho passando por uma fresta (ou dando a volta numa parede), quem o encarava o deixava parado do outro lado até o teto de dois minutos: havia caminho, então ele nunca atravessava, e a linha reta estava tampada, então ele nunca avançava. Agora, com a linha reta tampada e havendo caminho até o jogador, o piscar o traz **pelo caminho**, até três blocos de cada vez (`Cacada.avancarPeloCaminho`), parando antes de uma Linha de Cinza, da zona da vela e de encostar, como antes. O caminho só passa por onde ele cabe: o piscar continua não atravessando vidro nem parede. Onde o passo seguinte é um vão, ele já fica de quatro: é a pose que o jogador vê ao abrir os olhos.
- **Encarar debaixo de um teto.** O avanço em linha reta procurava o chão do lugar de chegada de cima para baixo, a partir de cinco blocos acima dele. Debaixo de um teto de até cinco blocos de espessura (uma casa, uma caverna rasa, a copa de uma árvore) o "chão" achado era o lado de cima do teto, o avanço era recusado por desnível, e ele não saía do lugar. Vi isso num teste: 26 piscadas, todas a 11,0 blocos, até a caçada acabar. Na caverna funda de 11/10 o piscar funcionou porque havia muita pedra em cima. Agora o chão é o do andar em que o avanço chegou, ou até três blocos abaixo (`Cacada.chaoNoAndar`).

## "Chegou" quando chegou

Ao testar na galeria dele, num dos lugares a caçada levava 24 s ou 70 s conforme a sorte. A busca dava o último lugar conhecido por alcançado a 2,8 blocos em linha reta: a dois passos do jogador, com uma quina de pedra entre os dois, ele "chegava", parava, olhava em volta e saía procurando noutro lugar, com o caminho pela metade. Agora "chegou" é estar a 2,8 blocos **e enxergar o lugar**, ou não ter mais para onde andar (`HospedeBusca.enxerga`). Quem enxerga o lugar e não vê ninguém é que chegou e não achou. Nesse lugar a caçada passou a levar de 19 a 21 s, em seis execuções.

## As páginas chegam depois (7.4)

O diário já tinha páginas sobre a caçada (a 23, a 24 e a 25), mas só na ordem, perto do fim. Ele viveu a primeira caçada sem ter lido nenhuma.

- **A página 23** ("olhar só compra tempo; use o tempo para andar para trás, até a luz": os dois verbos dele) é pedida depois da primeira caçada, seja qual for o fim dela. Vale para quem foi caçado antes desta versão: é o caso dele, e a página deve chegar nos primeiros minutos da próxima sessão.
- **Uma página nova, a 29**, na mesma voz, conta o vão: chega depois da primeira vez em que ele avisa numa boca e entra (`caca_vaos` na Memoria). Sempre depois, nunca antes: a primeira vez é para ser vivida sem manual.
- A entrega é a de toda página pedida (`Ensino.entregarPagina`): uma de cada vez, fora de qualquer aparição. O diário passa a ter 29 páginas.

## Os testes

- **Servidor** (`TestesDoVao`, sete testes; 94 no total):
  - `eleEntraPeloVaoDeUmBloco`: fechado numa sala de pedra, de costas para um buraco de um bloco rente ao chão. Ele vem pelo buraco, de quatro, com menos de um bloco de altura; antes de entrar fica 28 ticks ou mais parado na boca, e o alvo recebe dois ou mais sons de arrastar vindos dali; nunca usa o atravessar; nunca fica dentro de um bloco.
  - `encararPelaJanelaDeUmBlocoNaoVence`: a mesma sala com uma janela na altura dos olhos, e o jogador olhando para ele por ela o tempo todo. O piscar o traz pela janela, de quatro, e ele pega o jogador sem atravessar nada.
  - `depoisDoVaoEleSeLevanta`: duas câmaras ligadas por um túnel de um por um, com o jogador longe. Ele entra de quatro, sai e se levanta; longe do jogador não há aviso.
  - `peloTunelCompridoEleAvisaAoChegarPerto`: um túnel de um por um, de doze blocos, que desemboca na sala do jogador. Ele entra de quatro a quinze blocos, sem aviso, e avisa (parado, com os sons) quando o que falta entra nos oito blocos.
  - `naGaleriaDeVerdadeACacadaChega`: a galeria do dono, ele nascendo onde nasceu, o jogador parado em três lugares. Chega aos três em menos de 45 s, sem a navegação desistir e sem ficar dentro de um bloco. **Não é a prova de que a falha de 11/10 acabou** (ela não acontecia com o jogador parado nem antes): é a guarda de que o corpo novo não estragou o que funcionava num terreno de verdade, e o teste do "chegou".
  - `encararDebaixoDeTetoNaoVence`: debaixo de um teto de um bloco, três acima do chão, encarando. Ele avança a cada piscar e chega.
  - `asPaginasDaCacadaChegamDepois`: quem nunca foi caçado não tem página pedida; quem foi (e viu o vão) recebe a 23 e depois a 29.
- **Cliente** (`TestesDeCliente.deQuatro`, fotos 55 a 60): ele num buraco de um bloco, de frente, a cinco blocos e de perto; de lado em campo aberto, com cada um dos braços à frente; em cima de um bloco; e de cima. O teste confere que de quatro ele tem 0,9 de altura e cabe no buraco sem encostar em nada.
- `Terreno.montar` passou a pôr pedra também **embaixo** da caixa: o chão do mundo de teste não é plano, e onde ele é mais baixo que o canto a caverna montada ficava com o fundo aberto.

**Uma armadilha de teste que custou uma rodada:** a lista de colunas usadas que o `CLAUDE.md` ensinava a tirar não via os testes que montam terreno por função (`montarTunel(helper, dz)` e parecidas). Pus os testes novos em sete colunas que já tinham dono, e um deles achou no chão as páginas que outro tinha deixado. O `CLAUDE.md` agora manda listar todo número negativo terminado em 00.

## As fotos (55 a 60)

Abertas e conferidas, do job `fotos` do PR #45.

- **55 e 56, no buraco, de frente** (a cinco blocos e a três). É a vista que importa, e é a que funciona: a cabeça, erguida e um pouco torta, enche o buraco de um bloco, com os dois pontos claros; um braço sai pelo chão na direção de quem olha, com a mão clara na ponta; o outro some para trás. Cabe sem atravessar a pedra em volta.
- **60, de cima.** O corpo inteiro como foi desenhado: a cabeça, um braço adiante, o tronco, as duas pernas juntas para trás, o outro braço recolhido ao lado do tronco. Uns quatro blocos e meio de ponta a ponta.
- **57, 58 e 59, de lado.** Um corpo comprido e chato no chão, com a cabeça de pé a um terço. De lado e em campo aberto ele parece mais um boneco deitado do que de frente; não é de onde ele vai ser visto (de quatro ele está dentro de um vão ou debaixo de um teto baixo), mas é a pose a rever se o dono disser que pareceu artificial. As primeiras fotos de lado saíram a uns 35 graus: ele nasce virado para o jogador e o corpo leva mais de um segundo para acabar de virar para o rumo pedido. Achei que faltavam as pernas e pedi as fotos 59 e 60 para tirar a dúvida; o desenho estava certo, a foto é que tinha sido tirada cedo. O teste agora espera três segundos.
- **Um achado de passagem, que já existia:** o ombro esquerdo dele tem um quadrado claro em cima. Os dois pontos dos olhos usam, na textura, um canto que também é do braço esquerdo. De pé quase não se vê; de quatro e de cima, vê-se. Não mexi (mudar a textura é trabalho do gerador de texturas, e muda todas as fotos antigas).

## O que ficou de fora, de propósito

- **A afronta** (2.8) e a tabela dela no analisador: alpha25.
- **Os limites da navegação** (queda de três blocos, 64 blocos de caminho, 1024 nós): sem a falha reproduzida, mexer neles seria palpite. O log novo diz quando forem a causa.
- **O fiapo na passagem** (3.7), que dependia desta: um vestígio preso no vão por onde ele passou. Pequeno, e fica para quando houver uma caçada dele com vão para olhar.
- Som novo para o arrastar. O ambiente de gerar sons continua sem existir nas duas pastas; os dois sons usados já estavam no mod.
- A resposta dele na primeira caçada quando **não** há caminho nenhum (continua sem atalho e sem atravessar, como foi desenhada).

## Medição

A sessão sintética (uma hora, três jogadores de mentira em campo aberto) deu 30, 42 e 26 saídas por hora e 10, 9 e 8 aparições por hora (fase 2, fase 4, mundo novo). Na alpha22 tinham sido 30, 42 e 29, e 8, 10 e 10: a mistura não mudou, como se esperava de uma versão que só mexe na caçada. Nenhuma caçada aconteceu nessa hora; o que é novo aqui só os testes de servidor exercitam.

## Não verificado por ninguém

- **Se a pose convence.** As fotos mostram que desenha e que cabe; se assusta ou se parece um boneco deitado, só ele diz. É a primeira coisa a perguntar depois de uma caçada com vão.
- **Como soa o aviso na boca do vão.** São dois sons que ele já ouviu noutros lugares do mod (o arrastar na Linha de Cinza, o arranhar do atravessar).
- **Com a irmã ou os amigos olhando.** A regra "ele só anda quando não é olhado" é a do alvo. Outro jogador que esteja vendo enxerga a pose de quatro deslizando pelo chão, quase sem mexer os membros. É o mesmo limite que a caçada já tinha de pé (para quem não é o alvo ele anda normalmente), mas de quatro o deslizar aparece mais.
- **Atalhos em caverna natural.** O caminho medido de quatro passa por frestas de um bloco que um jogador não atravessa. Na galeria dele isso não mudou nenhum dos três trajetos testados (ele não ficou de quatro nenhuma vez), mas numa caverna cheia de frestas ele pode chegar por um lado inesperado. É o efeito pedido; se parecer trapaça, é ali que se olha.
- **O atalho no meio de um túnel.** A mais de dezesseis blocos e três segundos sem ser visto continua valendo o atalho da caçada (da segunda caçada em diante): ele some de dentro do túnel e reaparece a oito a doze blocos do jogador, fora da tela. Num teste isso o tirou de um túnel de dezoito blocos antes de ele chegar perto. É a regra de sempre, e o corpo se ajusta ao lugar novo; mas quer dizer que um túnel comprido de um bloco raramente será atravessado inteiro.
- **Debaixo de folhagem baixa** ele também se abaixa de quatro (folha a um bloco do chão é teto). Não testei numa floresta.
- **O piscar pelo caminho quando o caminho é uma volta comprida.** Encarado por uma janela de vidro com a porta aberta do outro lado da casa, ele agora vem pela porta, três blocos por piscada. É "fechar o que encerra de graça", mas é a mudança desta versão que mais muda a caçada dentro de uma casa.

---
# Versão 0.9.0-alpha25 — ele guarda (pacote 4, segunda parte)

A segunda metade do pacote 4: a ideia 2.8 do cardápio, na forma que a pesquisa de 11/10 pediu (um estado que atravessa os dias e anda para os dois lados, e não só uma reação de um segundo).

O que ele disse: "senti falta de algumas ações que pode 'irritar' o monstro" (11/10, de manhã). À tarde, na caverna, bateu nele, e a resposta foi a de sempre. Havia contadores (bater somava num número que o deixava mais ousado; oferecer uma arma contra ele na Tigela pesava a porta por quatro minutos), e nenhum dava para ler como "eu o irritei, e ele respondeu".

## A afronta (`Afronta`)

Um número só por jogador, de 0 a 100, escondido, guardado na Memoria, que **cai pela metade a cada dia do jogo** (um dia do jogo são vinte minutos: três golpes viram quase nada em uma hora se ele parar).

| O que ele faz | Pontos | Onde é notado |
|---|---|---|
| Bate nele (`BATEU`) | +25 | `Diretor.criaturaFerida`, inclusive na caçada |
| Corre para cima dele, com a mira nele, até ele sumir (`INVESTIU`) | +10 | `HospedeEntity.notarInvestida`; andando não conta |
| Põe um bloco entre os dois, com ele a até dez blocos (`FECHOU`) | +15 | o clique de pôr bloco; uma vez por aparição |
| Oferece na Tigela uma ferramenta contra ele (`OFENDEU`) | +20 | `Oferenda.resolver` |
| Deixa a Tigela vazia depois de três noites aceitas (`DESFEZ`) | +15 | `Oferenda.resolver` |
| Tem a oferenda aceita (`OFERECEU`) | −15 | `Oferenda.resolver` |
| Joga os Ossos pela primeira vez no dia (`CONSULTOU`) | −5 | `Ossos.jogar` |
| Vê-o de verdade e não faz nada (`DEIXOU`) | −3 | `Diretor.criaturaSumiu`; só aparição do Diretor, nunca a caçada |

Nenhum dos que somam é coisa que se faz sem querer. Um golpe só (25) não chega aos 40; dois no mesmo dia chegam.

## A resposta na hora

É o "ele registrou", e é **sempre a mesma** (o achado de SOMA na pesquisa: o jogador não lê ameaça ambígua; um sinal que se repete dá para aprender):

- **As chamas em volta do jogador ficam pálidas todas juntas** (até três tochas, a até doze blocos), por dez a quinze segundos. É miragem (`Miragem`, motivo `AFRONTA`): só ele vê, a tocha de verdade não muda. Ao contrário das chamas pálidas de sempre, estas mudam **na tela**: é para ele ver acontecer. É o primeiro degrau que o cardápio pedia para a caverna ("no golpe, a tocha mais próxima empalidece na hora").
- **Sem chama por perto, o mundo emudece** por oito segundos.
- **Fechar a passagem tem a sua**: duas batidas do outro lado do bloco que ele pôs (`PANCADA`, só para ele) e o bloco racha (o desenho de rachadura do jogo, mandado só para ele; o bloco de verdade fica inteiro). É o segundo degrau do cardápio.
- O que acontece na Tigela (ofensa, desfeita) não tem resposta na hora: é de noite, sem ninguém olhando, e a consequência dela já existia (a porta).
- Duas afrontas em menos de dois segundos dão uma resposta só.

## O que muda nos dias seguintes

- **O sorteio.** Enquanto o número está alto, a batida e a porta pesam mais (até quatro vezes com 100: `Afronta.peso`), e a presença e o "atrás de você" um pouco mais (até duas vezes). A batida, que só entrava no sorteio de quem passa muito tempo em casa, entra para quem está de 40 para cima. É "a mistura de eventos" que o plano pedia para ver no log.
- **A visita (de 40 para cima).** Na primeira noite em que ele está em casa há meio minuto, ele vem **sem sorteio**: a cena da janela, se a casa tem vidro; a batida na porta, se não tem. Uma vez por noite. Uma parte da conta fica paga (−20). Passa pelo mesmo portão das outras cenas (a trégua da oferenda aceita, o saldo de atenção, nada por cima de outra cena, nunca com ele recuando ou numa sequência de ameaça) e gasta atenção como elas.
- **A cobrança (de 80 para cima, na fase 4).** O descanso até a próxima sequência de ameaça cai para dois minutos, e uma parte grande da conta fica paga (−40). A sequência vem como sempre vem, com todos os avisos dela: isto só tira a espera. É o "a afronta pode adiantar a caçada, nunca tirar o aviso" do cardápio.

## A página chega depois

Como as da caçada (alpha24): depois da **primeira visita** por afronta, a página 30 do diário é pedida. Ela conta, na voz de sempre, o que soma e o que faz esquecer. Antes de ele viver uma vez, nada é explicado. O diário passa a ter 30 páginas.

## Onde me afastei da proposta de 11/10, e por quê

- **Encarar não soma.** A proposta tinha "encarar enquanto ele fica" (+15). Na caçada, encarar é o verbo que a página 23 acabou de ensinar: cobrar por ele seria punir a jogada certa. Fora da caçada, quem é olhado some em menos de dois segundos; não há "ficar encarando" para medir. Fica para quando existir a saída em que ele fica (a `FICA` da pesquisa de 11/10).
- **Mexer no que é dele não entrou** (derrubar o Boneco, esvaziar o baú da Casa do Vigia, tocar o Sino com ele à vista). O último é a ideia 7.2, do pacote 7; os outros dois pedem ganchos nos blocos, e nenhum apareceu numa sessão dele ainda.
- **"Ele não some, recua de frente" e "a cabeça inclina no terceiro golpe" não entraram.** São poses e movimento na frente do jogador; a resposta na hora ficou num sinal só, do mundo e não do corpo dele, para dar para aprender.
- **"Deixar em paz" tira, não soma.** A página 7 do diário diz que não reagir "acho que isso o irritou"; ali é sobre os sustos, não sobre ele. A pesquisa cita Sons of the Forest (não reagir pode baixar a agressão) e o risco de a relação virar bichinho. Ficou a mão que dá sentido a "quem não quer irritar, não irrita".
- **Não criei um lado "aplacado".** O número para em zero. A Tigela já tem a sua trégua, e um Hóspede "contente" é o caminho para o "kinda cute" que a pesquisa registra sobre The Obsessed.

## Os testes

- **JUnit** (`AfrontaTest`, cinco testes): quanto esfria por dia, os limites, que nenhum motivo é neutro e que um golpe só não chama a visita, o peso por evento, e as condições da visita.
- **Servidor** (`TestesDaAfronta`, seis testes; 100 no total):
  - `baterNeleFicaGuardadoEAsChamasRespondem`: no golpe, as duas tochas em volta dele empalidecem para ele e só para ele (o amigo ao lado não tem miragem nenhuma), a tocha de verdade não muda, e o número vai a 25 e a 50.
  - `fecharAPassagemNaCaraDeleTemResposta`: duas batidas vindas do bloco, a rachadura chega a ele, o bloco de verdade fica inteiro, o número vai a 15, e o segundo bloco diante da mesma aparição não soma.
  - `aOferendaBaixaEAOfensaSobe`: de 50 a 35 com o pão aceito, e a 55 com a vela oferecida, sem chama pálida nenhuma.
  - `irritadoEleVemAJanela`: com 50, a visita começa a cena da janela, o número cai a 30, a visita fica contada, ele aparece do lado de fora do vidro e a página 30 é pedida.
  - `furiosoODescansoAcabaEComOsDiasEsfria`: com 90 na fase 4 o descanso cai para dois minutos e o número a 50; na fase 3 nada muda; e 80 de dois dias atrás vira 20.
  - `deixarEmPazBaixaEInvestirSobe`: no túnel, vê-lo e não fazer nada tira 3; correr para cima dele com a mira nele soma 10 e empalidece a tocha da curva.
- **Não há foto nova.** A rachadura mandada só para um jogador foi fotografada na alpha22 (foto 46), e a tocha de almas no lugar da tocha é o mesmo mecanismo das chamas pálidas, que ele já viu dezenas de vezes. O que é novo é o momento em que acontecem, e isso foto não mostra.

## O analisador

Seção nova, "A afronta": o que ele fez, quantas vezes e quantos pontos; o maior valor e o do fim da sessão; as respostas na hora; quantas vezes ele veio por causa dela e quantas adiantou a ameaça; e **batidas e portas por hora com o número de 40 para cima e abaixo de 40** (é a prova que o plano pede: a afronta mudando a mistura).

## Medição

A sessão sintética (uma hora, três jogadores de mentira em campo aberto) deu 37, 42 e 29 saídas por hora e 10, 12 e 11 aparições por hora (fase 2, fase 4, mundo novo). Na alpha24: 30, 42 e 26, e 10, 9 e 8. Está dentro da variação de uma execução para outra, e era o esperado: sem o jogador fazer nada a ele, a afronta fica em zero e não pesa em nada. Em uma hora houve uma linha `AFRONTA` nos três (um `DEIXOU`, com o número já em zero). O que é novo só os testes exercitam.

## O que ficou de fora, de propósito

- Os gatilhos listados acima (encarar, mexer no que é dele) e as duas poses.
- Dizer o estado dele por um item (os Ossos caindo de um jeito quando ele está irritado). É a melhor ideia para tornar o número legível sob demanda, e é assunto do pacote 7 (itens que pagam).
- Qualquer efeito da afronta dentro do Avesso.

## Não verificado por ninguém

- **Se dá para ler.** O sinal é um só e se repete, mas ninguém viu as chamas mudarem na frente de um jogador de verdade. Pode passar despercebido de dia, ao ar livre, sem tocha por perto (aí o mundo só emudece, e isso é fácil de não notar no meio de um golpe).
- **Tocha que já está pálida não empalidece de novo.** Se as chamas em volta dele já estiverem pálidas pelo motivo de sempre (as chamas pálidas da fase 2 em diante), a resposta na hora passa para as outras; se todas estiverem, vira silêncio. Dois testes meus falharam uma vez por isto antes de eu entender.
- **Os números** (25, 40, 80, a metade por dia): são os da proposta de 11/10, sem sessão. O primeiro a olhar é quanto tempo o número fica acima de 40 numa sessão dele, e se a visita veio na noite certa.
- **Vira botão?** "Deixa eu bater para ver." É aceitável nas primeiras vezes (é ele testando a regra), e a visita é uma por noite. Se virar rotina, o analisador mostra: muitos `BATEU` e muitas visitas.
- **Somado com a Conta e com a obsessão**, pode ficar difícil saber por que uma coisa aconteceu. Por isso toda linha `AFRONTA` diz o motivo, e a visita e a cobrança têm linha própria.
- **Com amigos:** a afronta é de quem fez. A visita à janela é uma aparição de verdade: quem estiver na casa vê.

# Versão 0.9.0-alpha26 — a caçada com chance de verdade

Não é pacote do plano de dez: é o ajuste que a sessão do dono de 12/10/2026 pediu, antes do pacote 5 (seções 0.10 e 0.11 da passagem). Palavras dele: "mas po o bicho teleporta, arranca coração de vida". O log deu razão: duas sequências de ameaça em 58 minutos, **as duas com captura, 14 e 22 segundos depois do aviso**; três saltos no meio (dois atalhos e uma travessia) e um golpe dele que o pôs **nas costas** do jogador, que foi tocado dois segundos depois; e, quatro minutos e meio depois da primeira captura, ele morreu para um zumbi, com cinco corações. A segunda o largou com três corações, no escuro, no fundo de uma mina, e ele saiu pelo modo espectador.

A tese, da pesquisa sobre a IA de Alien: Isolation (`pesquisa/2026-10-15-analise-das-ideias-de-fora.md`): o jogador tem de se sentir impotente e, ao mesmo tempo, achar que é mais esperto que o bicho. Teletransporte é exceção (duas vezes numa campanha inteira, lá). O "aprender" é ilusão: partes do comportamento começam trancadas e **destrancam pelo que o jogador faz**, nunca por morte dele.

As contas estão numa classe pura, `assombracao/Saltos` (JUnit em `SaltosTest`); quem as usa é a `Cacada`.

## Os saltos destrancam pelo truque que ele repete

"Salto" é toda vez que ele muda de lugar sem andar, perto do jogador: o atalho, o atravessar e o recuo de quem foi ferido.

- **O atalho de longe** (o jogador a mais de dezesseis blocos) só vale depois de **duas caçadas em que o jogador fugiu para longe**. Fugir é ficar a vinte e quatro blocos ou mais dele e a doze ou mais do mais perto em que já esteve, no aviso ou na perseguição (`Saltos.fugiu`). O que o dono fez em 12/10 (de 19 para 33 blocos no aviso) conta.
- **Atravessar** (sem caminho, a até oito blocos) e **o atalho do anel** (sem caminho, de oito a dezesseis) só valem depois de **duas caçadas em que o jogador ficou onde não havia caminho e funcionou** (pilar, muro, buraco, anel). As travessias de antes desta versão contam como truque (o dono tem uma).
- **Seis caçadas destrancam tudo** de qualquer jeito: é o gatilho de segurança do Alien, que dá a incerteza sem parecer arbitrário.
- Na primeira caçada da vida dele nada está destrancado, como antes. Até a alpha25 era só isso: da segunda em diante valia tudo.
- O que ele fez numa caçada fica guardado quando ela acaba (`Diretor.cacadaTerminou`, chaves `caca_fugas` e `caca_truques`), uma vez por caçada. **Captura não destranca nada.**
- Para ele, isto dá a regra legível: "aquilo que eu sempre faço deixou de funcionar".

Hoje o dono tem três caçadas, nenhuma fuga anotada e uma travessia: o atalho de longe só volta depois de duas fugas ou de mais três caçadas; atravessar, depois de mais um truque.

## Nenhum salto nos quinze segundos depois do aviso

É quando o jogador decide entre correr, se esconder ou encarar. Vale para o atalho e para atravessar. O recuo de quem bateu nele não conta: é resposta a um golpe.

## Onde ele cai, e quanto espera

- **Nunca nas costas.** "Costas" é a menos de sessenta graus do ponto bem atrás de para onde o jogador está virado (só o plano do chão). Com o jogador parado, até a alpha25 o lugar do salto era **atrás dele**; agora é dos lados, entre a borda da tela e o começo das costas, de onde virar a cabeça o encontra. Com o jogador andando, continua "à frente de para onde ele vai, fora da tela", sem as costas.
- **O atalho de longe cai de doze a dezesseis blocos** (era de oito a doze). O do anel continua de oito a doze: é o lado de dentro de um muro, e a doze ele cairia do lado de fora de novo. A espera cobre a diferença.
- **Depois de qualquer salto ele fica parado**, olhando para o jogador, um tempo que é **maior quanto mais perto ele caiu**: a janela que se quer até o primeiro toque possível é de quatro segundos e meio, e a espera é o que falta para isso depois de descontar o tempo de chegar andando, entre dois e três segundos (`Saltos.esperaDepois`). O som de pano do atalho continua sendo o aviso de onde ele está.
- **A travessia que não acha ninguém** ("sair de perto resolve") usava um segundo e meio fixo; agora usa a mesma espera (perto do jogador, quase sempre três segundos).
- Toda mudança de lugar sem andar zera o embalo dele: o teste do salto pegou que ele chegava escorregando uns vinte centímetros na direção em que andava antes.

## Bater nele compra tempo

Nas duas primeiras vezes em que é ferido numa caçada, ele **sai da caçada** (estágio novo, `FERIDO`): some e reaparece de dezesseis a vinte e quatro blocos, fora da tela, nunca nas costas, de preferência onde o jogador não o enxerga, e fica ali parado **de seis a nove segundos**. Nos dois últimos segundos os passos dele se ouvem de onde ele está, e só então volta a perseguir, sabendo mais ou menos onde o jogador está. É o "volta para os dutos" do Alien. O que ele estava fazendo fica para trás (um piscar marcado, um vão, uma porta, um aviso de travessia). Da terceira vez em diante, nada acontece, como antes.

Até a alpha25 o recuo era de dez a catorze blocos, atrás de quem estava parado, e ele voltava a andar no tick seguinte.

Se na hora do golpe não há lugar longe (um quarto fechado), ele tenta de novo a cada segundo em que o jogador não está olhando; se não conseguir, a espera de um salto vem depois da volta, para quem ficou perto dele também ter como sair.

## A captura pesa, mas não larga para morrer

- **A vida não fica abaixo da metade** do que ele pode ter (já com a marca nova). Continuam saindo oito pontos de quem tem mais que isso; quem já estava abaixo da metade não piora. Até a alpha25 o piso era de dois corações. O caso da segunda captura de 12/10 (14 de vida, 14 de máximo): ficou com 6; agora ficaria com 7.
- **Onde ele acorda:** de preferência um lugar por onde já passou, a vinte a quarenta blocos, **com luz e sem monstro a doze blocos**. Até a alpha25 era o contrário: o lugar mais escuro, e as luzes em volta eram apagadas por um minuto. Um ponto do caminho dele sem monstro ganha sempre; se todos os do caminho têm monstro, os chãos quaisquer da faixa concorrem.
- Continuam: o item da mão fica caído onde ele foi pego, a marca (um coração de vida máxima, até três), a distância, o silêncio e o acordar devagar.

## O que salva tem de estar dito

- **A página 23** (os dois verbos) dizia "use o tempo para andar para trás, até a luz", e o dono carregava tochas. Agora diz "até a chama pálida de uma vela. Tocha não segura ele." Ele já leu a 23; isto vale para quem lê depois.
- **A página 25** (a da captura) existia e só chegava na ordem, perto do fim. Agora é pedida **depois da primeira captura** (`Ensino`, chave `teve_captura`), também para quem foi pego antes desta versão: é a que vai chegar para ele nos primeiros minutos da próxima sessão. Ela já dizia que a marca passa dormindo com a vela pálida acesa; ganhou uma frase que liga a chama pálida à caçada ("devia ter acendido uma assim que o mundo ficou mudo") e perdeu "no escuro" (ele não acorda mais no escuro).
- Ele já teve uma Vela (11/10) e leu a página dela. Não chegou a usar na caçada.

## Uma linha por caçada

Toda caçada, como quer que acabe, deixa `CACA id=... resumo` no log (TELEMETRIA.md), e o analisador junta numa tabela na seção "Caçadas, capturas e o outro lado": quando começou, do aviso ao fim, os saltos (com o segundo de cada um), a menor janela que um salto deu, do último salto ao fim, como acabou e o que estava destrancado. A tabela sai também de logs antigos (sem as duas colunas que só a linha nova tem). Dois alertas novos: salto nos quinze segundos depois do aviso, e captura a menos de três segundos de um salto (a travessia não conta: o aviso no bloco é a janela dela). **No log de 12/10 os dois aparecem**: atalho aos 4 s e aos 11 s e travessia aos 14 s do aviso; captura 2 s depois do golpe.

## O que não mudou, de propósito

- A frequência das caçadas (uma a cada 25 a 40 minutos de fase 4), o teto de tempo (dois minutos), o aviso de oito a dez segundos, a velocidade, o piscar, a vela, o vão, a porta. A passagem pedia para não mexer nisso antes de ver uma caçada dele com esta versão.
- O som da caçada. A voz é a alpha27, e a música que varia por encontro também fica para lá (é áudio).
- A resposta da afronta quando ele bate sem tocha por perto (`SILENCIO`, invisível no meio de uma caçada, que já está muda). Agora bater tem uma consequência que se vê (ele some e volta longe); a resposta da afronta fica como está.
- **A alpha26 e a alpha27 são o marco.** Até o dono jogar uma caçada com as duas, nada mais mexe na caçada.

## Os testes

- **JUnit** (`SaltosTest`, nove): o que destranca e o que não destranca, as travessias antigas contando, as seis caçadas, a espera maior quanto mais perto e sempre de dois a três segundos, a janela do atalho de longe nunca abaixo de quatro segundos na velocidade máxima, as costas (inclusive a borda de 120 graus dos dois lados), a fuga (com o caso de 12/10), e a vida depois da captura (com o caso de 12/10).
- **Servidor** (`TestesDosSaltos`, cinco; 105 no total):
  - `oSaltoNuncaCaiNasCostasEDaTempo`: seis saltos seguidos com o jogador parado; nenhum nas costas, nenhum na tela, nenhum a menos de onze blocos (a conta do lugar é pelo olho, a do teste pelos pés), ele parado nos dois segundos seguintes; e do último salto ao toque, três segundos ou mais.
  - `baterNeleCompraTempoEAVoltaTemPassos`: golpe a três blocos; ele reaparece a quinze ou mais, não nas costas, fica fora pelo menos seis segundos sem tocar ninguém, e os passos dele chegam ao jogador, vindos de onde ele está, antes da volta.
  - `oPilarFuncionaDuasVezes`: numa caçada de verdade, em cima do pilar, com um truque já anotado: nenhuma travessia, ninguém pego, e o truque fica anotado (dois). Na caçada seguinte ele atravessa, e só quinze segundos ou mais depois do aviso.
  - `fugirParaLongeFicaAnotado`: cinquenta blocos no aviso; ao fim da caçada, uma fuga e nenhum truque.
  - `aCapturaNaoLargaNoEscuro`: tochas de um lado só; a captura o leva para lá, com luz onde ele acorda, nenhuma tocha apagada em volta, e a vida na metade (estava em 12).
- Os testes antigos que precisavam de atravessar ou do atalho (`pilarNaoSegura`, os três do pilar, o da caixa de vidro, o de andar para longe no aviso, os do vão) passaram a pôr na memória os truques já feitos (`TestesDaCacada.jaUsouOTruque` e `jaFugiu`), em vez de só "não é a primeira caçada".
- **Não há foto nova.** O que muda na tela é onde ele está depois de um salto, e o teste de servidor mede isso melhor que uma foto. A foto 35 (onde ele acorda depois da captura) pode mudar: era a que mostrava o escuro.

## Não verificado por ninguém

- **Se assusta menos.** O risco desta versão é o contrário do de 12/10: com os saltos trancados, as próximas duas caçadas dele podem parecer fáceis de vencer correndo. É o que se quer (a terceira não), mas só ele diz se a perseguição ficou boa.
- **Se ele percebe que "o truque parou de funcionar".** É a tese; não há como testar sem ele.
- **Os números** (duas repetições, seis caçadas, quinze segundos, doze blocos, quatro segundos e meio, seis a nove fora, a metade da vida): ponto de partida, cada um com linha no log.
- **Os passos da volta**: o som é o passo do bloco em que ele pisa, só para o alvo. Ninguém ouviu.

# Versão 0.9.0-alpha27 — a voz da caçada

A segunda metade do marco da caçada (a primeira é a alpha26). O dono pediu "música e gritos" na perseguição (pelo texto de outra IA, 15/10) e, em 16/10, respondeu à pergunta do volume colando a recomendação dela: sim a um grito ou rugido, em volume normal, sem pico, raro e ligado ao momento, variado, mudando com a distância, e misturado com a trilha sem cobrir os outros sinais. Em 10/10 ele já tinha dito que vozes deviam vir de "gravações livres e bibliotecas de sons".

## O som

- **De gravação, não sintetizado.** É a primeira coisa do mod feita de gravação. Três gravações CC0 do Freesound, de vozes de homem: rosnados ("Male Aggressive Growls", Kodack) e gritos ("Male Screams [1]", marc3122; "human male scream multi", JohnsonBrandEditing). Ficam em `ferramentas/sons/fontes/`, com página, autor, licença conferida e `sha1` em `ORIGENS.md`. São as prévias de alta qualidade que o site serve sem login (o original pede conta).
- **O trabalho** (`gerar_voz_perto`, `gerar_voz_longe` no `gerar_sons.py`): tocar mais devagar (0,72 a 0,85 da velocidade: de três a seis semitons mais grave, e mais comprido), uma segunda cópia uma oitava abaixo só nos graves por baixo (o peito que um homem não tem), um pouco de aspereza (saturação suave). É o que faz a voz de gente virar a de uma coisa grande demais para ser gente, sem ficar com cara de efeito de dragão.
- **Duas famílias, três arquivos cada.** `voz_perto` (1,4 a 2,5 s): seca, curta, áspera; dois rosnados e um grito curto com rosnado por baixo. `voz_longe` (3,8 a 4,2 s): gritos abafados (o ar e as paredes comem os agudos: passa-baixa em 1,3 a 1,7 kHz) com mais sala que voz (1,1 s de reverberação); o terceiro é um rosnado que vira grito.
- **Volume medido, não no ouvido.** As duas famílias têm o mesmo nível: o RMS do trecho com som em −21 dBFS, o pico nunca acima de −6 (saíram entre −6,8 e −9,9). É o nível dos sons que já tocam na caçada (arranhar −21,5 de RMS; pano −21,4). "Mais urgente de perto" é timbre e duração; quem deixa a de longe mais baixa é a distância.
- As gravações de rosnado vêm saturadas na origem (picos em 0 dBFS). Mais devagar e abafado, isso se perde em boa parte; é o primeiro ponto a rever se ele achar a voz "estourada".

## Quando ele usa a voz (`assombracao/VozDaCacada`, pura)

- **Três momentos:** quando a perseguição começa (fim do aviso), em 60% das caçadas; no **primeiro golpe** do jogador, sempre (sai de onde ele foi ferido, antes de sumir: é a resposta ao golpe, e deixa o golpe legível); e quando ele **reencontra** o jogador depois de oito segundos sem saber dele, em 50% das vezes (inclusive depois de fingir que desistiu).
- **No máximo duas por caçada**, e quinze segundos entre uma e outra (o golpe não espera o intervalo).
- **Nunca em cima de outro sinal da caçada**: dois segundos longe do pano do atalho, do aviso na boca do vão, da maçaneta e do aviso no bloco. Esses dizem ao jogador o que fazer.
- **De longe (14 blocos ou mais) a abafada; de perto a seca.** A de longe sai com volume que só aumenta o alcance (o jogo não toca mais forte acima de 1), para chegar ao jogador com folga.
- A altura varia de 0,93 a 1,05 a cada vez, e o jogo sorteia um dos três arquivos: a mesma situação não soa sempre igual.
- **Só o alvo ouve**, como tudo da caçada (`ModSons.tocarPara`), em "Criaturas hostis" (o silêncio do mod não a corta).
- Sorteio da própria caçada, nunca o do mundo.

## A música da perseguição varia por encontro (`TrilhaCliente`)

Quando as três camadas da perseguição começam juntas, o cliente sorteia (sem tocar no sorteio do mundo) um andamento e um tom para as três ao mesmo tempo (0,94, 1 ou 1,06: um semitom abaixo, o normal ou um acima, com o andamento junto; as três com a mesma altura continuam sincronizadas) e a ordem em que as duas de cima entram (cordas antes dos sinos, ou o contrário). Nunca a mesma combinação duas vezes seguidas. O volume das camadas não mudou.

## O log e o analisador

`CACA id=... voz=PERTO|LONGE momento=INICIO|FERIDO|REENCONTRO dist=... n=...`, e o resumo da caçada ganhou `vozes=N`. A tabela "Uma linha por caçada" ganhou a coluna "Vozes". A variação da música é do cliente e não vai para o log.

## Os testes

- **JUnit** (`VozDaCacadaTest`, seis): no máximo duas, o intervalo (menos no golpe), nunca em cima de outro sinal, as chances de cada momento, a de longe sem ficar mais forte e sempre alcançando o jogador, e a faixa da altura.
- **Servidor** (`TestesDosSaltos.oGolpeTemVozSoParaOAlvo`; 106 no total): o primeiro golpe tem uma voz, a de perto, vinda de onde ele foi ferido; o amigo ao lado não recebe voz nenhuma; o segundo golpe não tem outra.
- Nas caçadas dos testes de servidor de uma execução: 15 sem voz, 17 com uma, 3 com duas (18 de longe no começo, 1 de perto no começo, 3 no golpe, 1 no reencontro).
- **O som em si:** `gerar_sons.py --so voz_perto voz_longe` confere pico, RMS, duração, contínua e pontas dos seis arquivos (todos ok), e rodar de novo gera os mesmos bytes.

## Medição

A sessão sintética (uma hora, três jogadores de mentira) deu 31, 43 e 29 saídas por hora e 8, 10 e 10 aparições por hora (fase 2, fase 4, mundo novo); na alpha26, 34, 41 e 31, e 8, 12 e 12. Uma caçada (a do jogador da fase 4), com captura e sem voz (o começo tem 60% de chance). A tabela "Uma linha por caçada" da sessão sintética é a linha de base do marco. Atenção ao ler o log dos testes: ele acumula as execuções, e os nomes dos jogadores de mentira se repetem entre uma e outra; separe a última execução antes de passar o analisador (eu misturei uma vez).

## Não verificado por ninguém

- **Como soa.** Ninguém ouviu: nem eu (não tenho como), nem o GitHub (a máquina de fotos não tem som). Escolhi as gravações e os trechos por medida (trechos sustentados, timbre, nível) e pelo nome. Pode soar como homem gritando devagar, que é o risco de quem trabalha voz de gente; pode soar "de filme". Só ele diz.
- Se a voz cobre os passos dele na perseguição (os passos não são sinal "parado", eles acompanham o andar; a regra dos dois segundos não os protege). A de longe, abafada e com sala, deveria passar por baixo; a de perto toca quando ele está parado (o golpe) ou acabou de ver o jogador.
- Se 60% no começo é muito: com uma caçada a cada 25 a 40 minutos, é uma voz a cada meia hora a uma hora de fase 4.
- A variação da música: só mudou a altura e a ordem; ninguém ouviu.

# Conferência: o mundo novo forçado pelos comandos (17/10/2026)

Não é versão: é medição, sem mudar o jogo. Em 15/10 um amigo do dono jogou em outro computador e, pelo que outra IA contou, achou que "mesmo forçando os níveis para o máximo demorou para presenciar acontecimentos". O log dele não chegou. A hipótese da passagem (0.11) era que `/sussurros fase 4` não mexe na obsessão e que a ameaça espera a obsessão subir.

A sessão sintética ganhou `SUSSURROS_SESSAO_FORCADO=fase|obsessao`: o jogador do mundo novo começa com `/sussurros fase 4` (e, com `obsessao`, também `/sussurros obsessao 100`), pelo mesmo caminho dos comandos. 45 minutos de jogo cada, alpha27.

| | primeiro evento | primeira aparição | primeira tentativa de contato | ameaça (AMEACANDO) | caçada |
|---|---|---|---|---|---|
| mundo novo de verdade (fase 0) | 5m56s | 15m55s | (relógio da primeira hora) | 39m02s (fase 3) | nenhuma em 60 min |
| forçado: fase 4 | 2m47s | 6m31s | ~16 min de jogo | 36m23s | 37m23s |
| forçado: fase 4 e obsessão 100 | 3m01s | 3m42s | ~16 min (relógio 958 s) | 37m46s | 38m19s |

**A hipótese estava errada.** Com a obsessão no máximo desde o primeiro segundo, a ameaça veio na mesma hora que sem ela. A trava é o **escuro**: para passar de `ESCALANDO` a `AMEACANDO` o Diretor pede `V >= 60` (vulnerabilidade: escuro, sozinho, longe de casa), e `ESCALANDO` dura no máximo quinze minutos (`no > 900`) antes de virar `RECUANDO`. Na sessão forçada, o primeiro `ESCALANDO` (aos 8 min) caiu no dia, com `V` entre 15 e 25, e expirou aos 23 min; o recuo e a calma levaram até os 30 min; o segundo `ESCALANDO` esperou a noite seguinte, e a ameaça veio aos 38 min, assim que `V` passou de 60.

O que isso diz para um jogador de verdade que força os níveis:

- o mod fala desde os três minutos (sons, vultos), mas os comandos não pulam o relógio da primeira hora: a primeira vez em que ele tenta ser visto de verdade é por volta dos dezesseis minutos de jogo, como num mundo comum. Sem o primeiro contato feito, a cota de corpo (aparições vistas de tempos em tempos) não vale, mesmo na fase 4;
- o momento mais forte depende de estar escuro na hora certa. Num mundo que começa de manhã, isso dá de meia hora a quarenta minutos.

**Nada foi mudado.** As duas coisas que encurtariam isso mexem no ritmo do Diretor (`ESCALANDO` não expirar por causa do dia; ou o comando de fase dar o primeiro contato por feito), e o combinado da 0.11 é não mexer no Diretor até o dono jogar o marco (alpha26 e alpha27). Ficam como proposta. Os jogadores de mentira nunca miram, então a sessão não mede "visto"; os tempos acima são de criado, não de visto.

# Versão 0.9.0-alpha28 — alguém esteve aqui

A parte do pacote 3 que esperava uma decisão do dono desde 11/10: pôr blocos de verdade, que ficam, em caverna natural. Em 18/10/2026 ele respondeu: "sim se possivel ate perto do que eu construi, é pra pertubar o jogador mesmo". É a decisão de design que o princípio 5 do `ROADMAP.md` pedia. Até aqui tudo o que o mod punha perto dele sumia sozinho (miragem, `AlteracoesTemporarias`) ou ficava longe da base (a Soleira, a Casa do Vigia, o Boneco, que anda mas é tirado ao andar). Isto fica.

A classe é `assombracao/AlguemEsteveAqui`. Não mexe no Diretor além de duas chamadas (no segundo e ao acordar), não gasta saldo de atenção (é coisa que ele só encontra se olhar) e não toca na caçada: o marco (alpha26 e alpha27) continua valendo.

## Na caverna

- **Quando:** da fase 2 em diante, com o primeiro contato feito (ou fase 3+), depois de quatro minutos seguidos debaixo da terra na mesma descida, com 12% de chance a cada vinte segundos; **uma vez por descida**, e no máximo a cada vinte e cinco minutos de mundo. Nunca com ele sendo assombrado de perto (criatura presente ou cena em curso).
- **Onde:** a caverna em que ele está é percorrida a partir dele (em largura, por onde se anda, até 2.500 células e 34 blocos); o trecho é o primeiro pedaço dela a dezesseis blocos ou mais que está **escuro** (sem céu e com luz de tocha de no máximo 2: ninguém acendeu), **fora da vista** dele (nem os pés nem a cabeça de quem ficasse ali) e a **dez blocos ou mais de por onde ele andou** (o `Rastro`, uns seis minutos). Na prática: o pedaço da caverna logo adiante, que ele ainda não iluminou.
- **O quê:** duas a quatro tochas de parede, a seis blocos ou mais uma da outra, na luz que ele usa (tocha ou tocha de alma, contando as que estão em volta dele). Uma só não conta (é só uma tocha) e é desfeita. Em 30% das vezes, se houver pedra para isso, **uma galeria** no lugar: três blocos de fundo e dois de altura cavados numa parede do trecho, com uma tocha no fundo, como um galho de mineração de outra pessoa.
- **A galeria só cava pedra natural** (`base_stone_overworld`): nunca minério, nunca com líquido ou bloco que cai em volta, sempre com chão firme, e só se nenhum dos seis blocos estiver à vista.
- **Na caverna que ele já acendeu, nada.** O pedaço da caverna dele exportado em 11/10 tem 110 células a dezesseis blocos ou mais da entrada, e nenhuma escura: ele acendeu tudo. Ali nada é posto (é um dos testes).

## Perto da casa

- **Quando:** com cama conhecida, da fase 2 em diante, em 45% dos dias do jogo, uma vez por dia; só com ele a **32 blocos ou mais da cama** (e a casa carregada), ou **ao acordar** de uma noite inteira de sono. Nunca com criatura presente ou cena.
- **O quê, em rodízio** (nunca a mesma duas vezes seguidas; se a da vez não cabe, a seguinte):
  1. **a tocha na janela:** uma tocha de parede do lado de fora da casa, presa na parede dele, ao lado de uma janela (vidro ou vidraça a até dez blocos da cama). O lado de fora é o que vê o céu; nunca colada numa porta;
  2. **a tocha no chão:** uma tocha fincada no chão a quatro a oito blocos da porta que ele mais usa (ou da cama);
  3. **a bancada:** uma bancada com uma tocha ao lado, a nove a dezesseis blocos da cama, como o acampamento da primeira noite de alguém.
- A luz é a que ele usa (contada em volta da cama).
- **Chão:** só lugar vazio, a céu aberto, sobre chão natural, e sem nada que não seja natural em volta (não encosta em cerca, horta, caminho nem parede dele). A tocha da janela é a única que encosta no que é dele: se apoia na parede, e mais nada.
- **Nunca à vista:** todo lugar é conferido contra a tela dele.

## Achar

As últimas seis coisas postas ficam lembradas (posição, tipo, quando). Quando ele passa a até quatro blocos e meio de uma delas, ela conta como **achada**: uma linha no log, e uma "marca" para as famílias (`Familias.registrar`, família `MARCA`), se ela ainda estava lá. Se ele a tirou antes, o log diz `esta=nao`.

## O que não muda

- Nada é tirado do mundo depois (fica). Ele pode quebrar tudo: são blocos comuns.
- Nada dele é quebrado, trocado ou tapado: é a regra "nunca estragar a construção", que a resposta dele não muda.
- O saldo de atenção, o Diretor, a caçada e o som.

## Os testes

- **Servidor** (`TestesDeAlguem`, cinco; 111 no total):
  - `naCavernaOTrechoQueEleNaoAndouAmanheceAceso`: túnel de pedra em L; as tochas vão para o braço que ele não vê, a catorze blocos ou mais, presas na parede, e só elas mudam no maciço.
  - `aGaleriaSoCavaPedraEFicaFora`: os seis blocos cavados eram pedra, nenhum à vista, a tocha no fundo, e nada mais muda.
  - `naCavernaQueEleJaAcendeuNadaMuda`: no pedaço da caverna do dono, que ele acendeu inteiro, nada é posto e nada muda.
  - `pertoDaCasaNadaDeleMuda`: uma casinha de tábua com porta, janela e cama; com o jogador a sessenta blocos, as três coisas da casa são postas do lado de fora (a tocha da janela apoiada na parede dele, a do chão a três a nove blocos da porta, a bancada com a tocha), nenhum bloco da casa muda, e passando ao lado da tocha do chão ele a acha.
  - `comEleOlhandoNadaAparece`: olhando para a casa a vinte blocos, nada é posto e nada muda.
- **Não há foto nova**: o que aparece são blocos comuns do jogo (tocha, bancada).
- **Sessão sintética não rodada**: os jogadores de mentira não descem nem têm cama, então nada disto acontece com eles, e nada do ritmo mudou.

## Não verificado por ninguém

- **Se perturba ou se parece "bug do mod".** Uma tocha na parede de fora da janela pode ser lida como "eu pus e esqueci". É o risco e é a graça. O primeiro dado é o `ALGUEM achado` e o que ele disser sem ser perguntado.
- **A frequência.** 45% dos dias e uma vez por descida são ponto de partida. Com mais de uma coisa por dia na casa vira cenário; com menos, ele não liga uma à outra.
- **Em servidor com amigos:** são blocos de verdade, todos veem. É a casa e a caverna de um jogador; quem estiver junto vê também.
- **O "não andou ali"** usa os últimos seis minutos de caminho (o `Rastro`) e a luz: uma caverna que ele visitou há uma hora sem acender pode receber tochas como "nova". A luz é a régua principal.

# Versão 0.9.0-alpha29 — o passo da caçada

Depois do marco. Em 18/10/2026 o dono jogou (alpha28) e forçou duas caçadas por comando, numa abertura do jogo em que o log não estava ligado (o `/sussurros debug on` vale até fechar o jogo). As palavras dele: "a caçada achei legal, mas ele nao corre tanto parece, e ele parece meio travado ate agir, como um delay? sla, de uma olhada boa".

Sem o log das duas caçadas, a leitura foi medida num teste novo (`TestesDoPasso.oPassoDaCacada`): caçada de comando, o jogador olha três segundos, vira as costas e corre a 5,6 blocos por segundo (a corrida do jogo) por dez segundos. Na alpha28:

- **parado no começo: 8,4 s** (o aviso, de 8 a 10 s);
- **saiu do lugar 0,25 s depois de o jogador virar**: não há atraso ao parar de olhar;
- **correndo, o jogador abriu de 21,5 para 42,8 blocos em 10 s** (ele veio a uns 3,5 b/s).

O "travado até agir" é o aviso. O "não corre tanto" é o passo: 4,9 b/s de partida, abaixo da corrida do jogador (5,6), e a aceleração de 4% por segundo só enquanto ele sabe onde o jogador está; cada olhada o congela. Com a alpha26 os saltos ficaram trancados (ele não aparece mais na frente de quem foge), então o passo virou o único perigo de quem corre, e ficou pouco.

O jogador de mentira faz menos barulho que um de verdade correndo (o jogo não conta o movimento dele no ouvido da criatura: `getKnownMovement` é zero), então ele ouve menos e a medida da fuga sai pessimista. Os 3,5 b/s são um piso; com o dono, o passo era o pedido (4,9 a 6,3).

## O que mudou

- **O aviso:** de 8 a 10 s para **5 a 6,5 s** parado. Continua sendo a chance de se preparar (o mundo emudece, uma luz falha). O silêncio e a marca de "aviso" para os sentidos (`cacaAvisoAte`, dez segundos) não mudaram: a caçada já começou quando eles acabam.
- **O passo:** começa em **5,3 b/s** (era 4,9; ainda abaixo da corrida), acelera **6% por segundo** sabendo onde o jogador está (era 4%) e vai até **6,9** (era 6,3; acima da corrida, abaixo da corrida com pulo, 7,1). Quem corre vê que ele vem e passa a perder terreno em uns três segundos; quem corre pulando ainda escapa. Os multiplicadores de antes continuam (o do Diretor, até 1,2; 0,85 na luz forte; 0,55 de quatro).
- Na mesma medida, depois da mudança: **aviso de 6,2 s; saiu do lugar em 0,35 s; correndo, de 21,3 para 34,7 blocos em 10 s.**

## Medição

Sessão sintética de uma hora: 35, 44 e 29 saídas por hora e 9, 11 e 12 aparições por hora (fase 2, fase 4, mundo novo); na alpha27, 31, 43 e 29, e 8, 10 e 10. Duas caçadas, as duas com captura (os jogadores de mentira não fogem: andam em quadrado e, numa delas, entraram nele no fim do aviso).

## O que não mudou

- A regra: parado quando olhado, o piscar, a vela, o vão, a porta, os saltos e as travas da alpha26, a voz da alpha27, a captura.
- A frequência das caçadas e o teto de tempo.

## Os testes

- `TestesDoPasso.oPassoDaCacada` (novo; 112 no total): o aviso de 4,5 a 7 s e o sair do lugar em até meio segundo. A distância de quem foge vai para o log sem ser exigida (o ouvido do jogador de mentira é pessimista).
- `TestesDaCacada.quemFicaDeCostasELevado`: a faixa de velocidade medida passou de 4,2–5,7 para 4,8–6,8 b/s (mediu 5,8).
- `SaltosTest`: as contas da janela usam a nova velocidade máxima (6,9 × 1,2); continuam passando.

## Não verificado por ninguém

- Se agora corre "o bastante" e se o aviso mais curto deixa tempo de reagir. A janela de reação dos saltos (alpha26) é proporcional à velocidade e continua garantida em conta, mas ninguém correu dele ainda.
- **O log das duas caçadas de 18/10 não existe.** A próxima sessão precisa do `/sussurros debug on` a cada vez que o jogo for aberto.
