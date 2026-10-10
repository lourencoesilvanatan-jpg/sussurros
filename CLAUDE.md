# Sussurros — instruções para o Claude Code

Mod de terror para Minecraft 26.2 (Fabric, Java 25). Código, comentários, documentação e commits são em português.

## Sem spoiler para o dono

O dono do projeto joga o mod para se assustar e pediu, em 08/10/2026, para não saber mais o que ele faz. Vale para tudo o que ele lê:

- Respostas a ele, títulos e corpos de PR, mensagens de commit e o `ROTEIRO-DE-TESTE.md` dizem o que foi mexido em termos gerais e o que ele precisa fazer. Não descrevem o que acontece no jogo, não citam nome de evento novo, não dizem o que um item faz.
- Os detalhes ficam nos arquivos marcados SPOILERS (`DESIGN-SPOILERS.md`, `PLANO-EXPANSAO.md`, `TELEMETRIA.md`, `pesquisa/`) e nos comentários do código.
- Se ele perguntar diretamente o que foi alguma coisa que viu, pode responder.

## Comandos

- `./gradlew build` (no Windows, `gradlew.bat build`): compila e roda os testes JUnit.
- `./gradlew runGameTest`: sobe um servidor de verdade, sem janela, e roda `src/gametest` com jogadores de mentira. Leva menos de um minuto.
- `./gradlew runClient`: abre o jogo de teste, sempre com o usuário `Jogador`. É o dono quem usa.
- `./gradlew runClientGameTest`: abre o jogo e tira fotos. **Não rode na máquina do dono**: abre uma janela e mostra o que ele não quer ver. Roda sozinho no GitHub em todo PR (job `fotos`); `ferramentas/testes/baixar_fotos.sh <branch>` espera a execução e baixa as fotos.

Rode o build e o `runGameTest` antes de cada commit. Não commite se falharem.

## O que você consegue verificar, e o que não

Três níveis: JUnit (matemática pura), teste de servidor (as regras, contra um jogador de mentira) e fotos do cliente (o que aparece na tela). Use os três. Mesmo assim ninguém ouviu os sons e ninguém sabe se assusta: isso só o dono descobre jogando. Ao entregar, diga o que foi verificado por qual nível e o que ficou sem verificação.

Cuidados com os testes de servidor (`JogadorDeTeste`):

- O jogador de mentira não tem cliente: `Rede.temCliente` é falso, e o que só existe no cliente não é sorteado para ele. Leia o resultado em `Diretor.sentidos(p)`.
- Ele não carrega o mundo em volta de si. `JogadorDeTeste` força os chunks vizinhos; sem isso, o que nasce a vinte blocos fica parado.
- A estrutura do teste fica acima do chão e tem piso próprio. Use `criarNoChao`, que o põe no chão de verdade, fora dela.
- O mundo de teste fica em pacífico: de noite os monstros do jogo matam o jogador de mentira.
- O log de decisões fica ligado nos testes, em `build/run/gameTest/sussurros-debug.log`. Quando um teste falha, a causa está lá. O arquivo acumula as execuções: a última está no fim.
- Os testes rodam todos ao mesmo tempo, lado a lado. Os dos itens põem o jogador a cem blocos uns dos outros (`dz` de -100 a -1000), porque a criatura de um teste esbarra no que o outro pôs no chão (uma Linha de Cinza alheia já derrubou um teste da caçada).
- `./gradlew build` já roda o `runGameTest` junto.
- Para medir ritmo: `SUSSURROS_SESSAO=1 ./gradlew runGameTest` roda uma hora de jogo com dois jogadores de mentira que reagem (de quatro a vinte minutos de relógio, conforme a máquina; `SUSSURROS_SESSAO_MINUTOS=30` encurta), e `ferramentas/log/analisar.py --jogador <nome>` lê o log (os dois nomes saem numa linha `[sessao]` da saída do Gradle). `SUSSURROS_SESSAO_SEM_LIMITE=1` mede sem o orçamento de atenção, para comparar. Os números variam bastante de uma execução para outra: uma execução só não decide número.
- Num teste longo não use `helper.onEachTick` nem `JogadorDeTeste.acompanhar`: eles agendam de uma vez uma tarefa para cada tick até o `maxTicks`, e o servidor percorre todas a cada tick. Use uma tarefa que agenda a seguinte (`cadaTick` em `TestesDeSessao`).
- Teste que depende de uma caçada terminar pode falhar no GitHub sem falhar aqui (aconteceu duas vezes em doze execuções, sem causa achada). Quando o job `build` falha, o log de decisões sobe como artefato `log-dos-testes`: baixe com `gh run download <id> -n log-dos-testes` antes de mexer em qualquer coisa. Teste novo que só precisa da criatura por perto use `Avesso.criaturaParaTeste`, que não tem sorteio.

## Fluxo de trabalho

- Uma branch por trabalho e um PR para a `main`. Nunca commite direto na `main`.
- Outra IA também trabalha neste repositório. Não edite uma branch que não foi aberta por você.
- Se o dono estiver com o jogo aberto, não troque de branch nem rode o Gradle na mesma pasta: use um `git worktree`.
- Prefixos de commit: `fix:`, `refactor:`, `test:`, `docs:`, `ci:`, `chore:`.

## Expansão 0.9 (em andamento)

O plano, com o que já foi feito e o que falta, está no `PLANO-EXPANSAO.md` (leia a seção "versão 2" antes de qualquer coisa: ela corrige o começo do arquivo). Três regras valem para tudo o que entrar: nunca alto, nunca matar, nunca estragar a construção do jogador.

Sons e texturas são gerados por código (`ferramentas/sons`, `ferramentas/texturas`). Para mudar um som, mude o script e gere de novo; não edite o `.ogg`.

## Refatoração 0.8.1 (suspensa durante a expansão)

O `Diretor` está sendo dividido em passos. Em cada passo:

- Mover sem reescrever. Não mude números, fórmulas, condições, linhas de log nem a ordem das chamadas ao RNG.
- Correção de bug ou mudança de comportamento vai em commit e PR separados.
- O que ficar para depois de propósito entra no `DIVIDAS-DESIGN.md`.
- Cada passo ganha um `MUDANCAS-0.8.1-refatoracao-passo-N.md` e uma seção no `CHECKLIST-TESTES-REFATORACAO.md`.
- Para mover uma seção inteira do `Diretor`, use `ferramentas/refatoracao/extrair_secao.py` e confira com `ferramentas/refatoracao/verificar_movimento.py`.

## Armadilhas do código

- **Memoria dentro do tick.** `Memoria.de(p)` devolve uma cópia. `Diretor.segundo()` abre uma no começo e salva no fim, então toda função chamada de dentro do tick deve receber esse `m`. Quem abre outra cópia e salva ali perde a alteração.
- **Alterações no mundo.** O que é temporário passa por `AlteracoesTemporarias`, que restaura no tempo certo e ao fechar o mundo. Não remova nem coloque blocos de forma permanente sem uma decisão de design (princípio 5 do `ROADMAP.md`).
- **Testes por comando.** O que é forçado por `/sussurros` (`e.forcando`, `PedidoManifestacao.deComando`) não pode contar para aprendizado, pressão, agenda nem memória.
- **RNG.** A telemetria (`Depuracao.log`) nunca pode consumir números aleatórios: isso mudaria as decisões do Diretor.
- **Textos do jogo.** Só existe `lang/en_us.json`, e ele está em português do Brasil de propósito: é o arquivo que o jogo usa como base em qualquer idioma, então o mod aparece em português mesmo com o jogo em inglês (o dono pediu isso em 09/10/2026). Não crie `pt_br.json` nem traduza para inglês sem ele pedir. Texto novo entra só nesse arquivo.
- **Spoilers.** `README.md`, `VERSAO-ATUAL.md`, `LEIA-ME.md` e `ROTEIRO-DE-TESTE.md` não detalham eventos. Os detalhes ficam em `DESIGN-SPOILERS.md` e `TELEMETRIA.md`.
- **Som só para o alvo.** Tudo o que é da assombração de um jogador usa `ModSons.tocarPara`, `tocarNaCabeca` ou `tocarEventoPara` (este para os sons do próprio jogo: passo, porta, batida). `ModSons.tocar` e `level.playSound(null, ...)` são para o que aconteceu de verdade no mundo. O teste `oSomDaAssombracaoESoDoAlvo` confere com dois jogadores lado a lado; `JogadorDeTeste.sonsRecebidos` conta os sons que chegaram a cada um.
- **Apresentação não usa o sorteio do mundo.** `Sentidos`, `Cacada` e `Captura` têm gerador próprio ou decidem pelo ID da manifestação: usar `level.getRandom()` ali mudaria os sorteios do Diretor.
- **Categoria de som.** O silêncio do mod corta "Música" e "Ambiente" do jogador. Som próprio que precisa continuar tocando vai em "Criaturas hostis".
- **Atenção.** Tudo o que o mod empurra para o jogador passa por `Atencao`, que tem dois saldos e um respiro comum: o do Diretor (`podeGastar` e `gastar`: eventos, cenas, cartas, caçada) e o do ambiente (`podeGastarAmbiente` e `gastarAmbiente`: presságios e perturbações). Sistema novo que fale com o jogador por conta própria (sem ser resposta a uma ação dele, e sem ser coisa que ele só encontra se olhar) entra num dos dois. Nunca ponha um consumidor barato no saldo do Diretor: na alpha12 havia um saldo só, o mais barato ganhava sempre, e a criatura não apareceu nenhuma vez em 55 minutos de jogo de verdade.
- **Confira a mistura, não só o ritmo.** Depois de mexer em qualquer número de ritmo, rode a sessão sintética e leia os alertas e a tabela de fontes do analisador: quantos eventos do Diretor por hora, quantas manifestações da criatura. Intervalo bonito com mistura errada foi exatamente o erro da alpha12.
- **Carregado não é andando.** Além da distância de simulação o chunk existe (`level.isLoaded`) e nada nele anda: entidade posta ali não envelhece nem some. Para pôr a criatura longe do jogador, confira também `level.isPositionEntityTicking`. O Diretor tira a criatura cujo relógio parou (`PARADA_NO_TEMPO`), mas isso é rede de segurança, não licença.
- **A Conta.** Todo item ligado à criatura soma na dívida escondida (`Conta`). De dentro do tick use `Conta.somar(p, m, item, vezes)` com a Memoria do tick; o `Conta.somar(p, item)` abre e salva a sua própria e é só para uso de item e clique em bloco. Item novo entra no enum `Conta.Item` e ganha a sua cobrança em `Conta.cobrar`.
- **Item mostrado no mundo.** O que aparece deitado (a oferenda na tigela, os ossos caídos) é um `item_display` criado por `Mostruario`, com etiqueta. Quem cria limpa: ao esvaziar, ao quebrar o bloco e por tempo.
- **Mixin.** Há um só, no cliente (`EsconderNoVeuMixin`). Nenhum teste local o carrega: só o job de fotos prova que ele ainda encaixa no jogo. Antes de criar outro, veja se um evento da Fabric ou uma miragem resolve.
- **De olhos fechados.** O que precisa mudar de uma vez na tela muda durante uma piscada (`Rede.efeito(p, PISCAR, ...)` e, no meio dela, o pacote novo): o cliente pula direto para o valor novo enquanto a tela está preta.
- **Dimensão nos testes.** O servidor de teste (`runGameTest`) não carrega dimensões de pacotes de dados: é o próprio jogo que as deixa de fora. Tudo o que depende do Avesso existir é conferido no teste de cliente (`TestesDeCliente.avesso`), com verificações que derrubam o teste. Formato dos arquivos de dimensão e bioma: copie de um arquivo do jar da 26.2, não de tutorial antigo (mudou muito).
- **Blocos por cor.** Na 26.2 não existe `Blocks.GRAY_BED`: camas (e outros blocos coloridos) são uma coleção, `Blocks.BED.pick(DyeColor.GRAY)`.
- **Tags do jogo.** Na 26.2 a tag `dirt` só tem terra; a grama está em `grass_blocks`. Antes de usar uma tag, abra o JSON dela no jar do jogo.
- **Receita nova.** Além do arquivo em `data/sussurros/recipe`, precisa do desbloqueio em `data/sussurros/advancement/recipes`, senão não aparece no livro de receitas e o dono (que não lê spoiler) não tem como descobrir. `ferramentas/texturas/conferir_recursos.py` confere texturas, modelos e estados de bloco.

## Ler uma sessão do dono

`python ferramentas/log/analisar.py <pasta principal>/run/sussurros-debug.log` monta o relatório da sessão (tem spoilers; é para você, não para ele). Comece por ele, e pelos alertas que abrem o relatório, antes de abrir o log. O arquivo acumula as sessões (quando o relógio no começo da linha volta para perto de zero, começou outra): separe as linhas da última num arquivo à parte antes de analisar. O `run/command_history.txt` mostra os comandos que ele digitou, de todas as sessões. Mudança de número vem depois de uma linha desse relatório, não antes. A revisão externa que orienta a fase de ajuste está em `pesquisa/2026-10-09-revisao-externa-0.9.md`, com uma tabela no fim do que já foi conferido e feito.

## Onde olhar primeiro

- `ARQUITETURA.md` e `ROADMAP.md`: princípios e plano.
- `src/main/java/com/sussurros/assombracao/Diretor.java`: ritmo, estados, seleção e eventos.
- `Cena*.java`: as cinco cenas compostas. `Atmosfera`: presságios e perturbações. `Aparicao`: escolha de posição.
- `entidade/HospedeEntity`, `entidade/HospedeBusca` e `entidade/Cacada`: a criatura, a busca e a caçada.
- `assombracao/Sentidos` e `rede/`: o que o servidor manda o cliente mostrar e tocar. `client/`: como ele mostra.
- `assombracao/Captura` e `assombracao/ApoioCaca`: o que a caçada faz ao jogador.
- `assombracao/Conta`, `Cantiga`, `Oferenda`, `Ossos`, `ChamasPalidas` e `bloco/`: os itens da 0.9 e a dívida que os une.
- `assombracao/Veu` e `client/mixin/`: o Véu e o único mixin do mod.
- `assombracao/Avesso` e `data/sussurros/{dimension,dimension_type,worldgen/biome}`: a dimensão. O Diretor só cuida do mundo normal; lá quem conduz é a própria classe.
- `assombracao/Erguidos`, `CasaDoVigia` e `EstruturasSussurros`: o que o mod põe no mundo.
- `assombracao/Baralho`: as cartas que adiantam, fora de hora, o que os outros sistemas já fazem. Sistema novo que mereça carta entra no enum `Carta` e em `aplicar`; não invente efeito que só exista na carta. (`Lugares` é outra coisa: o mapa dos lugares que o jogador frequenta.)
