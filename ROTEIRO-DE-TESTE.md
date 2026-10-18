# Sussurros — roteiro de teste

Este arquivo **não tem spoiler**. Ele diz o que fazer, não o que vai acontecer. O que cada coisa faz está nos arquivos marcados como SPOILERS, que você escolheu não abrir.

## Lista curta: o que testar em cada versão

Você vai jogar várias versões de uma vez. Não precisa testar uma por uma: é o **mesmo mundo**, jogando como sempre. Esta lista só diz onde vale passar um tempo e o que eu vou perguntar depois.

| Versão | Onde passar um tempo | O que eu vou perguntar |
|---|---|---|
| alpha18 e alpha19 | Em casa, fora, e **um bom tempo debaixo da terra** (caverna ou mina) | Alguma coisa pareceu "de boneco", artificial? Alguma coisa pareceu perto demais? |
| alpha20 | O mesmo. Vale ficar um tempo **parado num lugar apertado** (um canto de caverna, uma mina estreita) e jogar **mais de meia hora seguida** | Teve algum momento em que você sentiu que ia acontecer alguma coisa e não aconteceu nada? Você (ou a sua irmã) voltou a perguntar "cadê o bicho?" |
| alpha21 | **Durma.** Deite numa cama, à noite, em pelo menos **três noites de jogo** (não precisam ser seguidas), e espere uns segundos deitado em vez de levantar logo | Teve alguma coisa nesta sessão que você nunca tinha visto? |
| alpha22 e alpha23 | **Debaixo da terra**, de novo: pelo menos **uns quarenta minutos seguidos** numa caverna ou mina funda, **minerando de verdade**, e não só passando. Jogue como você joga: monte o seu acampamento lá embaixo, guarde as suas coisas, vá e volte pelos túneis | A caverna ainda pareceu repetitiva? Alguma coisa lá embaixo pareceu **defeito do jogo**, e não parte do mod? Em algum momento você ficou **preso** ou o jogo te **puxou de volta** andando por um túnel? |
| alpha24 | Não dá para marcar hora: é jogar bastante, **longe de casa e de noite ou debaixo da terra**. Se voltar a acontecer aquele momento em que alguma coisa vem atrás de você (o de 11/10, na caverna), **faça o que você faria de verdade para escapar**, do jeito que vier à cabeça. E **se aparecer uma página no chão, pegue e leia** | Em algum momento ele pareceu **preso** ou **travado** de novo? Depois de ler a página, deu para entender o que você podia ter feito? Alguma coisa nele pareceu **artificial**, "de boneco"? |
| alpha25 | Em qualquer lugar. Quando ele aparecer, **faça com ele o que der vontade** (inclusive o que você fez em 11/10, na caverna), e repare no que acontece **na hora** e **nas noites seguintes, quando você estiver em casa**. Use também o que você já tem e sabe usar | Deu para perceber que ele reagiu ao que você fez? Na hora, depois, ou as duas coisas? Você descobriu alguma coisa que **piora** e alguma que **acalma**? |
| alpha26 | Como na alpha24: jogar bastante **longe de casa, de noite ou debaixo da terra**, sem comando. Se aquele momento em que alguma coisa vem atrás de você acontecer, **faça o que você faria de verdade**, do jeito que vier à cabeça, e **leia a página que aparecer** nos primeiros minutos da sessão | Deu tempo de reagir? Em algum momento pareceu **injusto**? E o contrário: pareceu **fácil demais** de escapar? Depois, deu para continuar jogando ou você ficou numa situação sem saída? |
| alpha27 | O mesmo da alpha26, **com som ligado** (fone é melhor) e o volume de "Criaturas hostis" audível | Algum som pareceu **alto demais**, estourado ou de mau gosto? Algum pareceu **falso**, "de filme"? Alguma coisa importante ficou **difícil de ouvir** por causa de outro som? |
| alpha28 | **Explore cavernas novas**, das que você ainda não acendeu, e passe um tempo longe de casa e volte. Durma em casa de vez em quando | Você reparou em alguma coisa que **não lembra de ter feito**? Teve algo que pareceu **defeito do jogo**, ou que te incomodou de um jeito ruim (e não de um jeito bom)? |
| alpha29 | O mesmo da alpha26: longe de casa, de noite ou debaixo da terra. **Ligue o `/sussurros debug on` toda vez que abrir o jogo** | Ele ainda pareceu travado no começo? Correu o bastante, ou demais? Deu para escapar de algum jeito? |

De comando, só o `/sussurros debug on`, **toda vez que abrir o jogo** (ele desliga quando o jogo fecha).

## O que entrou (em termos gerais)

Versão 0.9.0-alpha29: **o que você disse da caçada em 18/10** ("ele não corre tanto, e parece meio travado até agir"). Medi num teste e você tinha razão nas duas coisas: ele ficava parado tempo demais no começo e corria menos do que você. Agora fica menos tempo parado e corre mais; correndo de verdade, ainda dá para escapar. Um aviso importante: na sua segunda abertura do jogo em 18/10 o registro estava desligado (o `/sussurros debug on` desliga quando o jogo fecha), então das duas caçadas eu só sei o horário. Ligue toda vez que abrir o jogo. E caçada forçada por comando vale para você ver, mas não conta para o que o mod aprende.

Versão 0.9.0-alpha28: **a sua resposta de 18/10** ("sim, até perto do que eu construí"). Entra uma coisa nova, debaixo da terra e perto da sua casa, que fica no mundo de verdade. Nada seu é quebrado, trocado ou tapado. Para testar: explore cavernas que você ainda não acendeu, e passe um tempo longe de casa (e volte). Não vou dizer o que é. Se alguma coisa te incomodar do jeito errado, me diga, que eu ajusto.

Versão 0.9.0-alpha27: **o som do momento que a alpha26 mexeu.** Entra o que você pediu sobre música e grito naquele momento, do jeito que você respondeu em 16/10: em volume normal, sem estouro, raro e variado. A música dele também deixa de ser sempre igual. É a primeira vez que o mod usa gravações de verdade (livres, de domínio público). Ninguém ouviu ainda: eu não tenho como ouvir, e a máquina do GitHub não tem som. Medi o volume por programa, e ficou no mesmo nível dos outros sons do mod. Se alguma coisa soar mal, me diga só "o som de tal hora ficou ruim", que eu acho no log. **Esta e a alpha26 são a versão para você jogar e me contar.**

Versão 0.9.0-alpha26: **não é pacote do plano: é a resposta ao que você disse depois da sessão de 12/10** ("o bicho teleporta, arranca coração de vida"). Mexi no momento mais forte das fases avançadas, o que você viveu duas vezes naquele dia. Ele continua sendo o momento mais pesado do mod, mas passa a te dar chance de verdade de reagir, e o que vem depois dele deixa de ser tão cruel. Também consertei uma página do diário que explicava mal uma coisa, e outra que chegava tarde demais passa a chegar na hora certa: deve aparecer uma para você nos primeiros minutos da próxima sessão. Não tem item nem lugar novo. A próxima versão (alpha27) mexe no som desse mesmo momento; **as duas juntas são a versão para você jogar e me contar como foi**, antes de eu mexer de novo nele.

Versão 0.9.0-alpha25: **o quarto pacote do plano de conteúdo, segunda parte.** É a resposta ao que você pediu em 11/10 ("ações que podem irritar o monstro"). O que você faz com ele passa a ficar guardado por um tempo, e a ter consequência na hora e nos dias seguintes; e vale para os dois lados, tem o que piora e tem o que acalma. Não tem item nem lugar novo. Não vou dizer o que conta: a graça é você descobrir. Se depois de algumas sessões nada tiver ficado claro, me diga, que isso é defeito meu e não seu.

Versão 0.9.0-alpha24: **o quarto pacote do plano de conteúdo, primeira parte.** É a resposta ao que você disse em 11/10 sobre o momento mais forte que você viveu ("ele estava preso num local"). Mexi nesse momento: no que ele consegue fazer, em duas brechas que o encerravam de graça, e no que o jogo te conta **depois** que ele acontece. Não tem item nem lugar novo. Um aviso: esse momento é raro, e pode levar mais de uma sessão para ele voltar. Não tente forçar com comando: os comandos de teste não contam para o que eu preciso ler no log. Nos primeiros minutos da próxima sessão deve aparecer uma página para você: leia. Se você já tiver uma página guardada sem ler, é essa.

Versão 0.9.0-alpha23: **o terceiro pacote do plano de conteúdo, segunda parte.** Mais duas coisas novas debaixo da terra, do mesmo tipo das da alpha22. Para testar, é o mesmo: um bom tempo seguido numa caverna ou mina funda, jogando como você joga (com o seu acampamento, as suas coisas guardadas, indo e voltando pelos túneis). As duas aparecem só para quem é o alvo.

Versão 0.9.0-alpha22: **o terceiro pacote do plano de conteúdo, primeira parte.** É a resposta ao que você disse da caverna em 11/10. Entram coisas novas debaixo da terra, de tipos que faltavam lá, e o que se repetia demais passa a dar a vez para o resto. Na superfície e em casa nada muda nesta versão. Para testar, é o que está na lista acima: um bom tempo seguido numa caverna ou mina funda, minerando. Um aviso para quando jogar com a sua irmã: quase tudo o que é novo aparece só para um de vocês, mas uma das coisas os dois veem.

Versão 0.9.0-alpha21: **o segundo pacote do plano de conteúdo.** Não tem nada novo: uma parte grande do mod, que já existia e dependia só de sorte para acontecer pela primeira vez, passa a ter um prazo. Para testar, é o que está na lista acima: dormir normalmente, em pelo menos três noites de jogo.

Versão 0.9.0-alpha20: **o primeiro pacote do plano de conteúdo** (o "pode todos" de 11/10). Não tem item nem lugar novo. São duas garantias: uma preparação do mod que não cabe no lugar em que você está deixa de terminar em nada, e nas fases em que você já está o mod passa a conferir se a parte principal dele chegou mesmo até você. Para testar, continue no **mesmo mundo**; as perguntas estão na lista acima.

Versão 0.9.0-alpha19: **a primeira etapa do que combinamos em 11/10** (as duas coisas a que você respondeu "por favor"). Não tem coisa nova para achar: duas coisas que já existiam passam a conseguir acontecer em lugares onde antes o mod desistia. Para testar, continue no **mesmo mundo** e jogue como da última vez: um tempo em casa, um tempo fora e **um bom tempo debaixo da terra** (caverna ou mina). Duas perguntas só para esta versão, para responder depois de jogar: **alguma coisa pareceu "de boneco", artificial?** e **alguma coisa pareceu perto demais, a ponto de parecer injusta ou de perder a graça?**

Versão 0.9.0-alpha18: **corrige a alpha17, que não funcionou na sua casa.** O que ela prometia dependia do terreno, e o terreno em volta da sua base (capim e flor por toda parte) fazia o mod desistir. O mesmo defeito escondia outras coisas, então você deve ver mais coisa acontecendo. Também diminuí bastante uma coisa que você achou repetitiva na caverna. Continua valendo o teste no **mesmo mundo**.

Versão 0.9.0-alpha17: a primeira parte do que você pediu depois da sessão de 10 e 11/10. Não tem coisa nova para achar: muda o jeito como uma parte do que já existe acontece. Vale o teste descrito abaixo, no **mesmo mundo**. Uma pergunta só para esta versão, para responder depois de jogar: **alguma coisa que você viu pareceu "de boneco", artificial no jeito de se mexer?**

Versão 0.9.0-alpha16: consertos num momento das fases mais avançadas, que uma sessão de uma hora num mundo novo não alcança. Não muda nada no que você precisa fazer.

Versão 0.9.0-alpha15: consertos em cima da alpha14, achados numa revisão antes de você jogar. Não muda nada no que você precisa fazer: vale o roteiro abaixo.

Versão 0.9.0-alpha14: **mexe na primeira hora de um mundo novo.** Não tem coisa nova. Mudei a ordem em que as coisas chegam até você e o que o jogo te diz sobre os itens: a dica de cada item (o texto que aparece ao passar o mouse em cima dele) agora diz como ele se usa. Esta versão foi feita para ser testada **numa sessão só, de uma hora, num mundo novo**. O passo a passo está logo abaixo.

Versão 0.9.0-alpha13: corrige um erro meu da alpha12. O limite de ritmo que eu tinha posto ficou apertado demais e, na sua sessão de 09/10, calou quase tudo o que importa. O "parado" que você sentiu veio daí, não do seu jeito de jogar.

## Como testar agora: continue no mesmo mundo

A primeira hora de um mundo novo você já testou (10 e 11/10) e ela funcionou como planejado. O que falta testar vem **depois** dela. Por isso, daqui em diante:

- abra o **mesmo mundo** dessa sessão, em vez de criar outro;
- jogue mais uma hora, ou duas se der, do jeito de sempre: parte do tempo em casa, parte fora, de dia e de noite;
- o resto vale como está escrito abaixo (só o `/sussurros debug on`, som ligado, ler a dica dos itens).

Os passos abaixo são os da primeira hora. Servem de novo se um dia você começar outro mundo.

### A primeira hora, num mundo novo

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

Pelo log da sessão de 10 e 11/10/2026 (mundo novo, uma hora, só com o `/sussurros debug on`): o jogo rodou sem erro do mod; a primeira hora deixou de ser parada, e a parte principal do mod apareceu nela; as explicações dos itens chegaram junto com os itens. Faltam as suas respostas às perguntas de cima, que o log não tem como dar.

Pelos logs das sessões de 08/10 e 09/10/2026 e pelas suas respostas: o jogo abre e roda sem erro do mod; as cenas rodam do começo ao fim; você ouviu e gostou dos sons distantes; as aparições somem do jeito que você pediu ("tem como perceber algo piscando, e é justamente isso que eu queria").

## Conferido por teste automático nesta versão

Você não precisa repetir isto. Fica registrado para quem programa.

- O mod carrega num servidor de verdade e todos os eventos rodam sem erro.
- O que mudou na alpha29 foi medido num teste novo, que confere quanto tempo ele fica parado no começo e quanto demora para sair do lugar quando você para de olhar. Os testes antigos da caçada continuam passando.
- O que mudou na alpha28 foi exercitado contra um jogador de mentira em cinco situações: um túnel de pedra, um pedaço da sua caverna de verdade e uma casinha com porta, janela e cama. Em todas, nada do que já existia mudou (a casinha é comparada bloco por bloco antes e depois), e nada aparece com o jogador olhando.
- O que mudou na alpha27 foi conferido em números (seis casos), contra um jogador de mentira (inclusive que quem está ao seu lado não ouve), e o volume dos sons novos foi medido por programa contra os sons que o mod já tinha. Como soa, nenhum teste diz.
- O que mudou na alpha26 foi exercitado contra um jogador de mentira em cinco situações novas, que medem quanto tempo você tem para reagir, e as contas por trás disso em nove casos. As situações antigas desse momento continuam passando. O log do mod passa a ter uma linha por vez em que esse momento acontece, e eu conferi a leitura dela no seu log de 12/10.
- O que mudou na alpha23 foi exercitado contra um jogador de mentira em mais duas situações, no mesmo túnel, e fotografado dentro do jogo de verdade. Desta vez os testes pesados rodaram só na máquina do GitHub: a sua estava sem memória livre para eles.
- O que mudou na alpha22 foi exercitado contra um jogador de mentira em seis situações, dentro de um túnel montado para isso, e a regra nova é conferida também em números (seis casos). Uma das situações confere que quem está colado em você não vê nem ouve o que é só seu. O que aparece na tela foi fotografado dentro do jogo de verdade, na máquina do GitHub.
- O que mudou na alpha21 é uma regra só: ela é conferida em números (cinco casos), contra um jogador de mentira, e de ponta a ponta dentro do jogo de verdade, na máquina do GitHub.
- O que mudou na alpha20 foi exercitado contra um jogador de mentira em nove situações, em lugares montados para isso, e o desfecho foi o esperado em todas. Uma delas confere que quem está ao seu lado não vê nem ouve o que é só seu.
- O que mudou na alpha19 continua passando nas suas sete situações. Duas delas rodam em cima de um pedaço de caverna copiado do seu mundo (só pedra, ar e tochas, sem as coordenadas).
- O que mudou na primeira hora (alpha14) continua passando nas suas seis situações.
- Os itens e blocos continuam sendo usados por um jogador de mentira e fotografados dentro do jogo.
- As receitas carregam e aparecem no livro na hora certa (que mudou nesta versão).
- A caçada, o acontecimento raro, os lugares e a maior novidade da expansão continuam passando nos testes de antes.

O que nenhum teste automático mede: se assusta, como soa, e se você entende os itens. Isso é com você.
