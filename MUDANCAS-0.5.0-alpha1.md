# Sussurros 0.5.0-alpha1 — O Mundo Lembra

Atualização grande e experimental construída em cima da `0.4.3-exp1`.

A tese desta versão é simples: o Diretor já sabe bastante sobre o jogador, então o próximo salto não deve ser outra variável invisível. O mundo precisa **expressar** essa inteligência.

## 1. Contexto do mundo
O Diretor classifica o local atual como:

- **CASA** — perto da cama ou da porta habitual conhecida;
- **SUBSOLO** — subterrâneo, usando a leitura que o mod já fazia;
- **ABERTO** — céu visível e poucas obstruções próximas;
- **OUTRO** — restante.

Isso não substitui Perfil, Lugares, Rastro, pressão ou obsessão. É uma camada de contexto para decidir **como** a mesma assombração se manifesta.

### Exemplos de peso
- Casa: PORTA, BATIDA, SINAL e SUSSURRO ficam mais naturais; PRESENÇA direta é um pouco menos provável.
- Subsolo: ECO, SINAL, passos e ATRÁS ganham espaço; portas quase deixam de ser escolhidas.
- Área aberta: PRESENÇA e SINAL ficam mais fortes; eventos de porta/tocha são praticamente descartados.

O log registra `CONTEXTO ...` e cada `SELECAO` inclui `contexto=`.

## 2. SINAL — falso positivo contextual
Novo evento de fase 2, intensidade baixa. Ele cria alguma coisa suspeita, mas **não cria o Hóspede**.

Dependendo do lugar, pode ser:
- a porta habitual rangendo sem se mover;
- uma respiração/pano em algum ponto da casa;
- uma quebra antiga sua ecoada uma única vez no subsolo;
- algo arrastando no túnel;
- um ruído em um ponto do seu Rastro ao ar livre;
- um estalo/pano sem fonte aparente.

Objetivo: quebrar a regra mental `som estranho = existe monstro para procurar`.

Telemetria: `SINAL contexto=... tipo=... semCriatura=sim ...`.

## 3. Cena “Na linha das árvores”
Nova composição para área aberta, fase 3+.

Naturalmente, depois de ficar algum tempo em campo aberto, longe da casa e com Rastro suficiente:
1. espera alguns segundos;
2. tenta colocar o Hóspede distante, fora da tela e preferencialmente junto de cobertura/penumbra;
3. quando essa manifestação termina, há uma pausa real;
4. ele tenta reaparecer do **lado oposto**, mais perto, em ESPREITAR e com no máximo um reposicionamento;
5. termina com 75–130 s de silêncio.

A cena é rara e tem cooldown grande. Ela não é uma caça: é uma pequena história espacial para vender a ideia de que ele está acompanhando a rota do jogador.

Teste: `/sussurros cena campo`.

## 4. PRESENÇA com cobertura
A PRESENÇA natural mantém Rastro/rota/fallback normal, mas agora tem uma tentativa intermediária de procurar cobertura real entre o jogador e a criatura. A intenção é obter mais aparições parcialmente ocultas por árvore, quina, pilar ou parede, sem depender de um renderer novo.

## 5. Tochas menos destrutivas
TOCHA deixa de significar “perdeu uma tocha” na maioria dos casos.

- normalmente apaga por cerca de 2–6 s e volta se o espaço continuar livre;
- em fase 4, obsessão muito alta, ainda existe uma pequena chance de a tocha ser levada de verdade.

Isso preserva manipulação ambiental sem transformar o terror em griefing repetitivo.

## 6. Som próprio do Hóspede
Alguns picos ainda usavam sons muito reconhecíveis do Minecraft. Nesta alpha:

- início de CAÇA deixa de usar heartbeat do Warden;
- toque deixa de usar shrieker;
- desaparecimento deixa de usar teleporte de Enderman.

São usados os sons próprios já existentes do Sussurros, com desaparecimentos comuns quase silenciosos.

## 7. Cenas não se sobrepõem
Casa, túnel e campo se bloqueiam mutuamente, inclusive nos comandos de teste. AMEAÇANDO também espera as três terminarem.

## Teste rápido
1. `/sussurros esquecer`
2. `/sussurros debug on`
3. `/sussurros evento sinal`
4. `/sussurros cena campo`
5. `/sussurros cena tunel`
6. `/sussurros evento presenca`

Use um comando de cada vez e espere a cena/manifestação terminar antes do próximo.

## Teste natural
Depois do smoke test, jogue 45–75 minutos sem forçar eventos. O objetivo é descobrir se casa, mina e campo começam a parecer **três manifestações do mesmo perseguidor**, e não apenas um saco de eventos aleatórios.

## Não entrou nesta alpha
- GeckoLib;
- olhos emissivos;
- partículas/pegadas próprias;
- animais sincronizados;
- configuração externa completa;
- refatoração grande do `Diretor.java`.

Essas mudanças continuam candidatas, mas não foram misturadas com esta rodada porque aumentam bastante o risco de API/renderização sem serem necessárias para testar a nova direção de design.
