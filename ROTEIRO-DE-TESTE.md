# Sussurros — roteiro de teste

Este arquivo **não tem spoiler**. Ele diz o que fazer, não o que vai acontecer. O que cada coisa faz está nos arquivos marcados como SPOILERS, que você escolheu não abrir.

## O que entrou (em termos gerais)

Versão 0.9.0-alpha14: **mexe na primeira hora de um mundo novo.** Não tem coisa nova. Mudei a ordem em que as coisas chegam até você e o que o jogo te diz sobre os itens: a dica de cada item (o texto que aparece ao passar o mouse em cima dele) agora diz como ele se usa. Esta versão foi feita para ser testada **numa sessão só, de uma hora, num mundo novo**. O passo a passo está logo abaixo.

Versão 0.9.0-alpha13: corrige um erro meu da alpha12. O limite de ritmo que eu tinha posto ficou apertado demais e, na sua sessão de 09/10, calou quase tudo o que importa. O "parado" que você sentiu veio daí, não do seu jeito de jogar.

## Como testar: uma hora, num mundo novo

1. No GitHub Desktop, trocar para a branch `main` e clicar em "Pull origin".
2. Abrir o jogo de teste (`runClient`).
3. Criar um **mundo novo**, em **sobrevivência**. Não continue o mundo de 09/10.
4. Assim que entrar, rodar `/sussurros debug on`. O log fica em `run/sussurros-debug.log`. Não abra esse arquivo.
5. Deixar o som ligado (fone é melhor). No menu de som do jogo, "Criaturas hostis" e "Ambiente" precisam estar audíveis: boa parte dos sons do mod sai por aí.
6. **Jogar uma hora seguida**, do jeito que você jogaria qualquer mundo novo: juntar recurso, fazer um abrigo, explorar um pouco, dormir quando quiser. Não precisa procurar nada.
7. Se aparecer algum item do mod, **passe o mouse em cima e leia a dica antes de usar**. Depois use do jeito que parecer natural.
8. No fim, sair do mundo normalmente e me dizer que jogou.

Para a sessão valer como sessão de verdade:

- de comando do mod, só o `/sussurros debug on`. Os outros (`/sussurros fase`, `estrutura`, `evento`, `cena`, `teste`, `memoria`, `esquecer`) são ferramentas de quem programa: adiantam coisas fora de hora, e alguns mostram o que você pediu para não ver;
- `/give` de tocha e de comida, tudo bem. Evite `/give` de item do mod, `/tp` para longe e `/time set`;
- se quiser trocar o ritmo, o `/sussurros ritmo calmo`, `padrao` ou `intenso` continua valendo. Me diga qual usou;
- se algum efeito de tela incomodar, dá para desligar sem perder o resto: `/sussurros_tela cor nao` e `/sussurros_tela borda nao` (`sim` liga de volta).

Se, ao criar o mundo, o jogo mostrar um aviso sobre "configurações experimentais", pode confirmar: é só porque o mod acrescenta coisas ao mundo.

Nada do que o mod faz mata você nem estraga a sua construção.

## O que mandar de volta

Basta dizer que jogou e por quanto tempo. Os logs são lidos direto da pasta.

A pergunta mais importante desta versão, para responder sem olhar nada além do que o jogo te mostrou:

- **Para cada item do mod que apareceu para você: com as suas palavras, para que você acha que ele serve?** Se não fizer ideia de algum, diga isso. É por aí que eu vou saber se o jogo está conseguindo se explicar.

Se quiser ajudar mais, responda sem descrever o que viu:

- Teve algum momento em que você **parou o que estava fazendo** para prestar atenção? Mais ou menos em que altura da hora?
- Teve algum trecho que pareceu **parado**? Qual?
- Algum som pareceu **jogado**, sem ter a ver com nada?
- Teve algum som **alto demais** ou irritante?
- Teve algum momento que pareceu **defeito** do jogo, e não parte do mod? (Se algo sumir, mudar ou "bugar" de um jeito que pareça erro, anote a hora e me diga só isso. Eu confiro no log se foi o mod.)
- Em algum momento pareceu **injusto**, ou você ficou **preso** sem saber o que fazer?
- Em algum momento o jogo **travou** por um instante (um segundo ou mais parado)? Se sim, o que você estava fazendo?
- De 0 a 10, quanto a sessão te deixou desconfortável?

## O que as versões anteriores trouxeram (você jogou pouco ou nada disto)

Versão 0.9.0-alpha12: o ritmo do mod foi espaçado, e passou a ter três opções (`/sussurros ritmo`).

Versão 0.9.0-alpha11: correções por baixo do pano (uma delas só aparece jogando com amigos) e uma ferramenta para eu ler as suas sessões mais depressa.

Versão 0.9.0-alpha10: os textos do mod (nomes de itens, mensagens, legendas de som, páginas) aparecem em português do Brasil, mesmo com o jogo em inglês.

Versão 0.9.0-alpha9: fecha o plano da expansão. A ordem em que as coisas acontecem muda de um mundo para outro.

Versão 0.9.0-alpha8: mais uma coisa para encontrar no mundo, longe de casa.

Versão 0.9.0-alpha7: duas correções por baixo do pano.

Versão 0.9.0-alpha6: aprofunda a novidade da alpha5.

Versão 0.9.0-alpha5: a maior novidade da expansão. Não há o que fazer para encontrá-la: é jogar normalmente, numa sobrevivência longa.

Versão 0.9.0-alpha4: coisas novas para encontrar no mundo, fora de casa.

Versão 0.9.0-alpha3: um acontecimento novo, raro, das fases mais avançadas.

Versão 0.9.0-alpha2: itens e blocos novos, e mudanças em alguns dos antigos; as receitas do mod aparecem sozinhas no livro de receitas do jogo, conforme você joga; um detalhe novo no ambiente.

Versão 0.9.0-alpha1: sons novos e uma trilha; uma camada nova no jogo, do lado do cliente; acontecimentos novos, espalhados pelas fases; o momento de caçada foi refeito; o mod deixou de tirar blocos do seu mundo.

Numa sessão de uma hora num mundo novo você não vai chegar à maior parte disso, e está certo assim: esta versão é sobre o começo.

## Já conferido antes desta versão

Pelos logs das sessões de 08/10 e 09/10/2026 e pelas suas respostas: o jogo abre e roda sem erro do mod; as cenas rodam do começo ao fim; você ouviu e gostou dos sons distantes; as aparições somem do jeito que você pediu ("tem como perceber algo piscando, e é justamente isso que eu queria").

## Conferido por teste automático nesta versão

Você não precisa repetir isto. Fica registrado para quem programa.

- O mod carrega num servidor de verdade e todos os eventos rodam sem erro.
- O que mudou na primeira hora foi exercitado contra um jogador de mentira em seis situações, e o desfecho foi o esperado em todas.
- Os itens e blocos continuam sendo usados por um jogador de mentira e fotografados dentro do jogo.
- As receitas carregam e aparecem no livro na hora certa (que mudou nesta versão).
- A caçada, o acontecimento raro, os lugares e a maior novidade da expansão continuam passando nos testes de antes.

O que nenhum teste automático mede: se assusta, como soa, e se você entende os itens. Isso é com você.
