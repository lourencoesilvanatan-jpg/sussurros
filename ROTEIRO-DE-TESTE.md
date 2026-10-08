# Sussurros — roteiro de teste no jogo (SPOILERS)

Tudo o que entrou na `main` sem ninguém ter jogado, em ordem. Cada parte diz o que fazer no jogo e o que conferir. Quem programa acrescenta uma seção a cada mudança; depois de um teste, as seções conferidas saem daqui.

**Antes de começar**

1. No GitHub Desktop, trocar para a branch `main` e clicar em "Pull origin".
2. Abrir o jogo de teste (`runClient`) num mundo com cheats.
3. Rodar `/sussurros debug on`. O log fica em `sussurros-debug.log`, na pasta do jogo.

**O que mandar de volta:** o arquivo `sussurros-debug.log` e as respostas da parte 6.

---

## 1. Refatoração (PRs #2 e #5)

Só mudou o lugar do código. O comportamento tem de ser o de sempre.

- Rodar, uma de cada vez: `/sussurros cena tunel`, `/sussurros cena campo`, `/sussurros cena marco`, `/sussurros cena janela` (precisa de vidro por perto) e `/sussurros cena casa`.
- No log, cada cena vai de `INICIO` a `FIM`, e não aparece `Erro no Diretor`.

## 2. Sons distantes (PR #6)

Parado num lugar quieto:

- `/sussurros evento sinal_distante`: antes não se ouvia nada. Agora deve dar para ouvir, baixo e ao longe.
- `/sussurros evento ruido_retorno` (depois de quebrar alguns blocos e andar um pouco).
- `/sussurros evento passagem`: quatro sons passando de um lado para o outro.
- `/sussurros evento batida` (perto de uma porta de madeira, de costas para ela).

Anotar se algum ficou alto demais ou continuou inaudível. No log, nenhuma linha `SINAL ...` com `dist` acima de 45.

## 3. Evento que não cabe (PR #7)

Jogar normalmente. No log:

- toda linha `SELECAO ... escolhido=` é seguida de `EVENTO` ou de `SELECAO falhou evento=...`;
- não há três `SELECAO` em três segundos seguidos sem nenhum `EVENTO`.

## 4. Leitura de reação (PR #8)

Jogar normalmente, e em algum momento usar `/tp` para longe. No log:

- nenhuma linha `REACAO` com `fugiu=true` e `investigou=true` juntos;
- depois do `/tp` aparece `SALTO: teleporte, respawn ou portal; ...`;
- `percepção confirmada pela reação` só em linhas com giro de 45° ou mais e `olhou=true`.

## 5. Olho (PR #9)

Com `/sussurros fase 3`, num lugar sem vestígios, usar o Olho cinco vezes seguidas:

- no máximo uma aparição;
- no log, `OLHO chamou aparicao ...` uma vez e depois `OLHO nada ... recarga=sim`.

## 6. Experimentos

Cada comando responde a uma pergunta de que uma etapa do plano depende. Anotar a resposta ao lado.

### Som sem direção

Usar fone de ouvido. Rodar os quatro e comparar:

| Comando | O que deve acontecer | Pergunta |
|---|---|---|
| `/sussurros teste sussurro lado` | Respiração vinda da esquerda | É a referência: veio da esquerda? |
| `/sussurros teste sussurro cabeca` | Respiração do mod "dentro da cabeça" | Soou no meio, sem lado? Continuou no meio ao girar a câmera? |
| `/sussurros teste sussurro folego` | Respiração do próprio jogo "dentro da cabeça" | Qual das duas respirações assusta mais? |
| `/sussurros teste sussurro estereo` | Um som de água, posicionado à esquerda | Soou no meio (sem lado) ou veio da esquerda? |

### Bloco que só você vê

Fazer num lugar escuro.

| Comando | O que deve acontecer | Perguntas |
|---|---|---|
| `/sussurros teste miragem` | Uma tocha aparece 4 blocos à frente por 20 s | Ela ilumina o chão em volta? O que acontece ao clicar nela? Ela some sozinha depois de 20 s? |
| `/sussurros teste miragem apagar` (perto de uma tocha de verdade) | A tocha some por 20 s e volta | A luz some junto ou fica? O que acontece ao clicar no lugar? |

### Sósia

| Comando | O que deve acontecer | Perguntas |
|---|---|---|
| `/sussurros teste sosia <nome>` | Um boneco com a pele do jogador, 8 blocos à frente, por 30 s | A pele apareceu? Tem algum rótulo embaixo do nome? Ele ficou virado para você? |
| `/sussurros teste sosia <nome> agachado` | O mesmo, agachado | A pose funcionou? |
| `/sussurros teste sosia <nome> deitado` | O mesmo, deitado | A pose funcionou? |

Testar com o nome da sua conta de verdade e, se der, com o de um amigo. No jogo de teste o seu nome é `Jogador`, que não tem pele própria.

## 7. Silêncio de verdade e respiração no sussurro

Para ter música tocando: `/playsound minecraft:music.creative music @s`.

- Com a música tocando e animais por perto, rodar `/sussurros evento presenca` três ou quatro vezes. Na maioria delas a música corta na hora e os animais ficam uns 25 s sem fazer som. No log, `SILENCIO_REAL motivo=APARICAO`.
- `/sussurros evento sinal` algumas vezes: de vez em quando a música corta sem aparecer nada (`SILENCIO_REAL motivo=SINAL`).
- `/sussurros evento sussurro`: o texto de sempre, agora com uma respiração sem direção.

Perguntas: o corte da música dá medo ou parece defeito? A respiração junto do texto ajuda ou atrapalha?

## 8. Vulto distante e regras de lugar

De dia, ao ar livre, num lugar com vista (campo, praia, alto de um morro):

- `/sussurros evento vulto`, e depois girar a câmera devagar até achar a silhueta, a 50–80 blocos. Ela some um segundo depois de você mirar nela.
- Repetir e, em vez de mirar, andar na direção dela: some quando você chega a uns 36 blocos.
- No log: `evento=VULTO modo=VULTO` e `sumiu motivo=VULTO_MIRADO` ou `CHEGOU_PERTO`.
- `/sussurros evento presenca` de dia em campo aberto, várias vezes: ele não aparece mais a menos de 25 blocos sem algo na frente (tronco, parede).

Perguntas: dá para ver a silhueta a essa distância? Ela some rápido ou devagar demais? Sumir de um quadro para o outro incomoda de longe? Se a sua "distância de entidades" (opções de vídeo) estiver abaixo de 100%, o vulto pode nem ser desenhado: anotar o valor.
