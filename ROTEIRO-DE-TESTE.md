# Sussurros — roteiro de teste

Este arquivo **não tem spoiler**. Ele diz o que fazer, não o que vai acontecer. O que cada coisa faz está nos arquivos marcados como SPOILERS, que você escolheu não abrir.

## O que entrou (em termos gerais)

Versão 0.9.0-alpha6: aprofunda a novidade da alpha5. Não muda nada no que você precisa fazer.

Versão 0.9.0-alpha5, quinta parte da expansão:

- a maior novidade até aqui. Não há o que fazer para encontrá-la: jogue normalmente, numa sobrevivência longa, e durma quando for noite.

Se, ao abrir o seu mundo, o jogo mostrar um aviso sobre "configurações experimentais", pode confirmar: é só porque o mod acrescenta coisas ao mundo.

Da versão 0.9.0-alpha4, que você ainda não jogou:

- coisas novas para encontrar no mundo, fora de casa. Vale explorar a pé e olhar em volta, de dia e de noite.

Da versão 0.9.0-alpha3, que você ainda não jogou:

- um acontecimento novo, raro, das fases mais avançadas.

Da versão 0.9.0-alpha2, que você ainda não jogou:

- itens e blocos novos, e mudanças em alguns dos antigos;
- as receitas do mod agora aparecem sozinhas no livro de receitas do jogo, conforme você joga. Não precisa procurar em lugar nenhum;
- um detalhe novo no ambiente.

Da versão 0.9.0-alpha1, que você ainda não jogou:

- sons novos e uma trilha;
- uma camada nova no jogo, do lado do cliente;
- acontecimentos novos, espalhados pelas fases;
- o momento de caçada foi refeito;
- o mod deixou de tirar blocos do seu mundo.

## Como testar

1. No GitHub Desktop, trocar para a branch `main` e clicar em "Pull origin".
2. Abrir o jogo de teste (`runClient`).
3. Rodar `/sussurros debug on`. O log fica em `run/sussurros-debug.log`. Não abra esse arquivo.
4. **Jogar normalmente.** De preferência sessões longas, num mundo de sobrevivência, com som ligado (fone é melhor).
5. No menu de som do jogo, deixar "Criaturas hostis" e "Ambiente" audíveis. Boa parte dos sons do mod sai por aí.

Se quiser adiantar o relógio do mod em vez de esperar, `/sussurros fase 2`, `3` ou `4` continua valendo.

Vale a pena jogar em **sobrevivência**, e não em criativo: boa parte do que é novo você encontra, recebe ou fabrica. Experimente o que aparecer, do jeito que parecer natural. Nada do que é novo mata você nem estraga a sua construção.

Se algum efeito de tela incomodar, dá para desligar sem perder o resto: `/sussurros_tela cor nao` e `/sussurros_tela borda nao` (`sim` liga de volta).

## O que mandar de volta

Basta dizer que jogou e por quanto tempo. Os logs são lidos direto da pasta.

Se quiser ajudar mais, responda sem descrever o que viu:

- Teve algum momento que pareceu **defeito** do jogo, e não parte do mod? (Essa pergunta ficou mais importante nesta versão: se algo sumir, mudar ou "bugar" de um jeito que pareça erro, anote a hora e me diga só isso. Eu confiro no log se foi o mod.)
- Teve algum som **alto demais** ou irritante?
- Teve algum momento em que você ficou **preso**, sem saber o que fazer, e isso irritou em vez de assustar?
- Teve algum item que você **não entendeu para que serve**, mesmo depois de usar algumas vezes?
- Algum item pareceu **forte demais** (resolve tudo) ou **inútil**?
- Em algum momento pareceu **injusto**?
- Em algum momento o jogo **travou** por um instante (um segundo ou mais parado)? Se sim, o que você estava fazendo?
- De 0 a 10, quanto a sessão te deixou desconfortável?

## Já conferido antes desta versão

Pelos logs das sessões de 08/10/2026 e pelas suas respostas: o jogo abre e roda sem erro do mod; as cenas rodam do começo ao fim; você ouviu e gostou dos sons distantes; as aparições somem do jeito que você pediu ("tem como perceber algo piscando, e é justamente isso que eu queria").

## Conferido por teste automático nesta versão

Você não precisa repetir isto. Fica registrado para quem programa.

- O mod carrega num servidor de verdade e todos os eventos rodam sem erro.
- A caçada foi exercitada contra um jogador de mentira em cinco situações, e o desfecho foi o esperado em todas.
- Cada efeito de tela foi fotografado no máximo, dentro do jogo, numa máquina do GitHub.
- Cada item e bloco novo foi usado por um jogador de mentira (nove situações) e fotografado dentro do jogo.
- As receitas carregam e aparecem no livro na hora certa.
- O acontecimento novo foi exercitado em duas situações num servidor de verdade e fotografado dentro do jogo.
- O que há de novo para encontrar no mundo foi gerado e usado por um jogador de mentira, e fotografado.
- A maior novidade desta versão foi percorrida de ponta a ponta dentro do jogo, numa máquina do GitHub, com fotos e verificações.

O que nenhum teste automático mede: se assusta, e como soa. Isso é com você.
