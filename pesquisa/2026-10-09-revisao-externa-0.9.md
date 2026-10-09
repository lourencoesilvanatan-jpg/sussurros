# Revisão externa da 0.9 e proposta para a próxima fase (SPOILERS)

> Recebida em 09/10/2026. Escrita por outra instância do Claude que acompanha o projeto desde 07/10, a pedido do dono, que avisou que não ia ler. Fica aqui para as próximas sessões. O texto abaixo é o dela, sem alteração. No fim há uma seção "O que foi conferido e feito", escrita por quem programa.

# Para o Claude Code: revisão externa da 0.9 e proposta para a próxima fase

**Sobre este documento.** O dono do Sussurros pediu que eu falasse direto com você e avisou que não vai ler. Então **este texto tem spoilers**: guarde fora de qualquer coisa que ele abra (por exemplo, `pesquisa/REVISAO-EXTERNA-0.9.md`). Sou outra instância do Claude e acompanho o projeto desde 07/10: fiz a análise inicial, o plano de refatoração, a revisão dos passos 1 e 2 e parte da pesquisa.

**O que eu li.** `VERSAO-ATUAL.md` e `PLANO-EXPANSAO.md` na versão alpha9, `DIVIDAS-DESIGN.md`, `ferramentas/sons/LEIA-ME.md`, `HospedeModel`, `HospedeRenderer`, e o `Diretor.java` numa versão por volta da alpha5. Este último ponto importa: o que eu disser sobre o código do `Diretor` pode estar superado pelas alphas 6 a 9. Confira antes de agir.

---

## 0. Veredito

O trabalho está muito bom, e alguns pontos merecem registro:

- **Os testes em três níveis.** Matemática pura, servidor de verdade com jogador de mentira, e o jogo aberto no GitHub devolvendo fotos.
- **O roteiro de teste sem spoiler.**
- **A pipeline de som com medição automática.**
- **As correções baseadas no log real**, com o log citado nos comentários do código.
- **O cuidado de amarrar cada peça nova a sistemas que já existiam.**

O plano fechou, e o próprio documento diz o essencial: **nada foi ouvido nem jogado por uma pessoa**. Minha tese para a próxima fase é que ela deve ser de **medir e cortar, não de acrescentar**. Abaixo estão os riscos que eu vejo, por ordem de gravidade, e o que eu faria com cada um.

---

## 1. Risco nº 1: a densidade somada

**O que acontece.** Cada sistema tem o próprio relógio:

- o Diretor (intervalo, piso, oportunidade e cadeias);
- a Atmosfera, com orçamento próprio;
- as cinco cenas, cada uma com seu tempo de espera;
- o baralho, a cada 40–90 minutos;
- o Véu;
- os Erguidos (o Boneco anda uma noite por vez; a Soleira);
- a Oferenda, à noite;
- os avisos da Conta;
- a Cantiga;
- as Chamas Pálidas;
- a captura e o Avesso.

O log de 08/10, quando ainda havia bem menos fontes, já mostrava **um acontecimento a cada ~65 s**, e a própria pesquisa marca isso como "frequência alta". Hoje há muito mais fontes de saída perceptível, e nenhuma delas sabe das outras. A densidade final não é decidida por ninguém: ela simplesmente emerge da soma.

**Proposta: um orçamento global de atenção, por jogador.**

- Uma classe `Atencao`, com `podeGastar(custo, tipo)` e `gastar(...)`, que regenera devagar.
- **Toda** saída perceptível passa por ela: evento do Diretor, presságio, passo da cena, chama pálida, aviso da Conta, Véu, Boneco, assobio.
- Ficam de fora só o feedback direto de uma ação do jogador (ele toca a caixa e a caixa toca) e as camadas contínuas do `Sentidos`.
- O estado `RECUANDO` zera a regeneração e veta tudo, exceto o contínuo. **A trégua precisa ser trégua para todos os sistemas**, não só para o Diretor.
- Uma linha de telemetria: `ATENCAO fonte=... custo=... saldo=...`.

**Critério de pronto**, medido na sessão sintética da seção 3:
- na fase 2, a mediana do intervalo entre saídas perceptíveis fica em 3 minutos ou mais;
- na fase 4, em 90 segundos ou mais;
- dois eventos de intensidade 22 ou mais nunca acontecem a menos de 5 minutos um do outro, a não ser dentro de uma cena composta.

Esses números são ponto de partida. Coloque-os na configuração da seção 4.

---

## 2. Risco nº 2: familiaridade com a criatura e com o tema

A expansão aumentou muito o tempo de convivência com a ameaça: a caçada em estágios, a presença no Avesso, o vulto distante, o tema musical em vários lugares. Esse é o risco clássico do gênero. Uma análise de design de jogos de terror resume: quando a ameaça deixa de ser desconhecida, ela deixa de assustar, e quanto menos variada ela for e mais tranquilos tiverem sido os encontros passados, menos o jogador a teme. Outra aponta a repetição e a familiaridade como um problema central dos desenvolvedores, junto com introduzir o monstro cedo demais e com luz forte demais.

**Proposta: um orçamento de exposição, com métrica.**

- O cliente já sabe se o Hóspede está na tela (o pacote `campo`/`Percepcao`). Some **segundos de criatura na tela por hora**, separando distância e luz, e mande ao servidor ou registre no log.
- Teto inicial, a ajustar: 20 a 30 segundos por hora na fase 3, contando só exposição a menos de 25 blocos com luz 8 ou mais.
- Regra de enquadramento: **antes da fase 4, nunca o corpo inteiro, perto e bem iluminado.** Encontros próximos precisam ser no escuro, com cobertura parcial, ou curtos demais para serem estudados.
- **Faça a mesma conta para o tema musical.** Caixa, cantarolar, assobio, `caca_tema` e `CANTIGA` usam todos a mesma melodia. Se ela tocar dez vezes por sessão, vira jingle. Conte quantas vezes por hora e coloque um teto.

---

## 3. Ferramentas que destravam a fase de ajuste

### 3a. Analisador de log

Crie um script, por exemplo `ferramentas/log/analisar.py`, que transforme o `sussurros-debug.log` num relatório em markdown, comparável entre sessões. Ele deve mostrar:

- a linha do tempo das fases;
- eventos por hora, por categoria e por fonte;
- a distribuição dos intervalos entre eventos e os silêncios mais longos;
- a exposição à criatura e ao tema;
- a distribuição da confiança das reações (c = 0; de 0 a 0,35; acima de 0,35);
- a trajetória da Conta, os avisos dados e a cobrança;
- o uso de cada item;
- capturas e visitas ao Avesso;
- as linhas `SELECAO falhou` e `sem lugar`.

Sem isso, cada sessão do dono vira horas de leitura manual. Foi uma leitura manual que encontrou os onze problemas da análise de 08/10; o script torna isso barato e repetível.

### 3b. Sessão sintética

Um GameTest longo com um jogador de mentira que segue um roteiro:

1. minera numa caverna;
2. volta para a superfície;
3. anda em campo aberto;
4. volta para casa;
5. dorme;
6. repete o ciclo.

Ele roda algumas horas de jogo aceleradas (o comando `/tick sprint` existe desde a 1.20.3, então deve servir no 26.2) e passa o log pelo analisador. Os critérios da seção 1 viram `assert`. O Avesso fica de fora, porque vocês já notaram que o servidor de teste não carrega dimensões.

Isso não diz se o mod assusta. Mas **diz se o ritmo está dentro do envelope antes de gastar o tempo do dono.**

---

## 4. Configuração antes de qualquer ajuste

Ainda não vi um arquivo de configuração. A próxima fase é inteira de mexer em números, então ele é obrigatório:

- **Um `Ajustes` em JSON** com as constantes do Diretor, da Atmosfera, da Caçada, da Conta, do Avesso, do Baralho, da Atenção e da exposição.
- **Um comando `/sussurros recarregar`.**
- **Perfis prontos** (calmo, padrão, intenso) para o dono comparar sem ler o que mudou.

Assim você manda para ele um arquivo de configuração, e não um jar novo, a cada rodada.

---

## 5. Arquitetura: terminar o que a refatoração deixou

O `DIVIDAS-DESIGN.md` já registra as cenas repetindo a mesma máquina de estados e a mesma checagem de "nenhuma outra cena ativa", além de 31 helpers do `Diretor` que deixaram de ser privados. Na versão que li, o `Diretor` voltou a ser o ponto que liga tudo:

- cada sistema novo deixou ali um gancho de teste (`testarOssos`, `testarOferenda`, `resumoDaConta`...);
- o `switch` de eventos cresceu (`VULTO`, `ECO_PASSOS`, `VIGIA`, `NEBLINA`, `VEU`, `CANTIGA`, `PRENUNCIO`);
- a condição para entrar em `AMEACANDO` ainda lista as cinco cenas uma a uma.

Ordem sugerida:

1. **`Ajustes`** (seção 4).
2. **`Atencao` como uma porta** chamada por cada subsistema. Não exige refatorar nada antes.
3. **O condutor de cenas**, com um único lugar para "a cena ativa" e prioridades. O baralho, o Véu e a sequência de ameaça são composições do mesmo tipo e devem morar ali. É também onde a porta da `Atencao` fica natural.
4. **Um registro de acontecimentos** (`EnumMap<Evento, Acontecimento>`), com uma checagem na inicialização de que todo evento tem implementação.
5. **Tirar os ganchos de teste do `Diretor`**, para uma classe como `GanchosDeTeste`.

Mantenha as regras de "mover sem reescrever". Agora os GameTests servem de rede de segurança, o que torna isso bem mais seguro do que no passo 1.

**Uma conferência rápida.** A alpha7 diz que converteu 19 chamadas de som para tocarem só para o alvo. Na versão que li, `passos()`, `passoUnico()`, `eco()` e partes do `sinalFalso` e do `reposicionarEspreita` ainda usavam `level.playSound(null, ...)` ou `ModSons.tocar(level, ...)`. Rode `grep -n "playSound(null"` e `grep -n "ModSons.tocar(level"` e classifique cada ocorrência: é assombração (deve ser privada) ou é algo que mudou de verdade no mundo (pode ser pública)? Passos fantasmas são assombração.

---

## 6. A criatura: corpo, rosto e o significado de "Hóspede"

**Diagnóstico.**
- O modelo é feito de caixas: cabeça de 6×10×6, braços de 2×30×2 numa peça só (**sem cotovelo**), pernas de 3×22×3, **sem pescoço**, com uma textura de 64×64.
- Os olhos não brilham, por decisão de design.
- A postura e o fade estão bem feitos.
- O ponto fraco da análise original (o que chega ao olho do jogador) continua sendo o corpo.

Propostas em ordem de custo. **Nenhuma precisa de GeckoLib:**

**a. Articular.** Dividir os braços em braço, antebraço e dedos longos, e acrescentar um pescoço. Isso libera as poses que dão medo: cotovelo dobrado para o lado errado, cabeça inclinada além de 90°, cabeça que gira para acompanhar o jogador enquanto o corpo fica parado. É só `ModelPart` com pivôs.

**b. Mexer só na visão periférica.** É do lado do cliente, usando o mesmo cálculo de tela do `Percepcao`:
- quando a criatura está na faixa externa da tela, os pequenos movimentos de respiração e ajuste acontecem;
- perto do centro da tela, **imobilidade absoluta**, sem nenhum balanço.
- O jogador sente que "algo se mexeu" e, quando olha direto, está parado. Isso reforça a regra da caçada ("só anda quando você não olha") também fora dela.

**c. Trancos.** De vez em quando, trocar de pose sem interpolação por um quadro, com efeito de stop-motion. É barato e estranho.

**d. Silhueta sem luz no vulto distante.** Desenhar com luz fixa baixa no modo `VULTO` e acima de uns 30 blocos, para ele ler como um recorte preto mesmo de dia, como faz o The Hollow. De perto, iluminação normal.

**e. Olhos que refletem luz.** Os olhos ficam invisíveis, exceto quando o jogador segura uma fonte de luz e está virado para ele. Nesse caso, um brilho fraco, como olhos de animal sob uma lanterna. É realista, conversa com a vela e o lampião, e mantém a sua decisão de não ter olhos que marcam a posição o tempo todo.

**f. O rosto aprendido.** Esta é a proposta com mais significado. O mod se chama Sussurros, a criatura se chama Hóspede, e o sistema inteiro é sobre ele aprender o jogador.
- Conforme a obsessão e as visitas ao Avesso aumentam, uma camada no cliente desenha **a textura do rosto da pele do próprio jogador** sobre a cabeça do Hóspede, com transparência crescente em três degraus.
- Só o alvo vê.
- É o "ele te estuda" virando imagem, e prepara o final da seção 9.

**g. Textura.** Mais contraste de valor: corpo quase preto, com poucos pontos pálidos, para continuar legível como silhueta. Resolução maior foge do estilo do Minecraft.

O GeckoLib tem versão para Fabric no 26.2 (a 5.5.x), se um dia as animações pedirem ossos de verdade. Minha recomendação é não usar agora: os itens de **a** a **f** cabem no modelo do próprio jogo. Para criar as peças, existem servidores MCP para o Blockbench.

---

## 7. Som

**O que está ótimo:** a pipeline em `gerar_sons.py`, com medição de pico, RMS, emendas e a convenção de mono com posição e estéreo sem. A camada de perseguição alinhada no tempo também.

**Riscos e propostas:**

**Vozes sintetizadas.** `sussurro_voz`, `cantarolar`, `chamado` e `assobio` são síntese de ruído filtrado. Voz humana sintética tende a soar artificial, e é exatamente onde o realismo mais assusta.
- Mantenha a síntese para o que é abstrato: drone, os tons de vidro, vento, apagão.
- Para tudo o que é voz, use **gravações reais**: o dono e os amigos, e efeitos CC0 para foley.
- Acrescente ao script um modo `--importar` que normaliza o arquivo gravado e passa pela mesma conferência de qualidade.

**Legendas.** Se a legenda de um aviso verdadeiro for diferente da de um falso (o `PRENUNCIO`, o assobio honesto e o mentiroso, um sinal falso contra uma aparição), **a legenda denuncia o aviso que mente.** Uniformize: o mesmo texto, vago ("Algo se move", "Um som distante"). Muita gente joga com legendas ligadas.

**Música na perseguição.** O próprio plano diz que nada pode cobrir os passos dele. Coloque na configuração uma opção `caca.musica = nenhuma | baixa | normal` e teste com o dono em sessões diferentes.

**Sound Physics Remastered.**
- Tem versão para Fabric no 26.2 (1.5.1+26.2) e roda só no cliente.
- Arquivos estéreo tocam no volume máximo, sem posição, o que bate com a convenção de vocês. Confira se nenhum som posicional ficou estéreo.
- Ele dá de graça o efeito "atrás da parede" e o eco de caverna. **Mas muda a atenuação**, então o `volumePara` foi ajustado para o áudio padrão do jogo.
- Decida um ambiente só, com ou sem ele, para o modpack dos amigos, e ajuste para esse ambiente.

**Silêncio de verdade.** Confira se o `fundo_grave` continua tocando durante o `RECUANDO`. O contraste depois de um pico depende de o drone também cair. Silêncio é a ferramenta mais forte que o mod tem.

---

## 8. Itens e economia

São cerca de 13 itens: Vela, Olho, Sino, Fio, Isca, Cinza, Caderno, Página, Caixa, Linha, Tigela, Lampião e Ossos. A própria pesquisa registra a lição: **não virar jogo de progressão**, com itens pequenos e sem vitória.

**Escassez.** Com as receitas aparecendo sozinhas no livro, a tendência é abundância. Num jogo de terror, recurso escasso gera tensão.
- A Cinza deveria ser a única moeda, e vir só de encontros, sem fazenda.
- Confira os limites de drop.
- Algumas ferramentas deveriam ser **achadas** (na Casa do Vigia, no caminho da Soleira) em vez de liberadas por receita.

**Verbos sobrepostos.** "Detectar" tem Olho, Sino, Ossos, Lampião e Fio. "Proteger" tem Vela, Linha e Tigela. Em vez de três ferramentas por verbo, prefira uma honesta e uma barata e não confiável. Use a telemetria de uso (seção 3a) para ver quais ninguém usa e corte essas.

**A Conta.** Valor escondido é bom, mas ela precisa ser **legível depois**: o jogador deve conseguir concluir "usei demais". Sugestão: depois de uma cobrança, aparece uma linha nova no diário ou no Caderno. Funciona como um recibo dentro do jogo.

**Linha de Cinza.** A linha de sal que o monstro não cruza é um clássico, e o próprio plano marca o risco de ela facilitar demais a caçada. Limite já pela configuração: uma linha intacta por jogador, ou um número máximo de tentativas por noite.

---

## 9. Narrativa: falta um fim

**O que já existe:** 14 páginas de diário, a Casa do Vigia (a vítima anterior), o Avesso ("o mundo como era antes do jogador") e uma criatura chamada Hóspede que aprende. **O que falta:** um fechamento que caiba nas duas semanas de vida de um servidor de amigos. Hoje o mod escala até a fase 4 e se sustenta ali.

**Proposta: uma fase 5, "O Inquilino".** Ela libera com várias visitas ao Avesso, obsessão alta e o rosto aprendido (6f) no último degrau.

1. **No Avesso, a cópia parece habitada pela sua rotina:** cama desfeita, itens arrumados como você arruma, tochas onde você põe.
2. **No mundo normal, de fora da sua casa, pela janela, você vê "você"**, fazendo a sua rotina lá dentro. É um `Mannequin` com a sua pele, visível só para você.
   - O manequim tem uma descrição que aparece junto ao nome, e precisa ser escondida.
   - O perfil precisa já conter a textura da pele.
   - Ele só tem pose em pé e agachado, então "de costas" se faz girando a entidade.
3. **Na última noite, uma página de diário nova, em primeira pessoa, montada com dados da `Memoria` e do `Perfil`** ("dorme sempre do lado esquerdo", "olha para trás quando ouve passos") e assinada com o nome do jogador. O diário que ele leu era do Vigia anterior. Agora o Vigia é ele.
4. **Epílogo:** a assombração cai para um modo raro e quieto. Sem explicação. O final fica ambíguo de propósito.

**Duas variantes de epílogo**, conforme a relação que o jogador construiu: convivência (Tigela e oferendas) ou resistência (linhas, lampiões, encarar). As duas usam os mesmos sistemas, só com pesos diferentes.

---

## 10. Jogando com amigos

**O que falta conferir ou fazer:**
- se a visibilidade do Hóspede é só do alvo (`broadcastToPlayer`);
- o "marcado da noite", um alvo fixo por noite;
- a separação como gatilho;
- a pressão calculada pela média do grupo.

**Voz, como dependência opcional.** O Simple Voice Chat tem versão para Fabric no 26.2. A API dele tem canais de áudio presos a uma entidade e um evento disparado quando um pacote de microfone chega ao servidor. Imitar a voz de um amigo é viável:
- carregue a integração só se o mod estiver presente;
- os amigos precisam saber que trechos de voz podem ficar guardados no servidor.

O chat de voz por proximidade também torna a separação física: quem se afasta deixa de ouvir os outros de verdade.

---

## 11. Ordem proposta

| Fase | O quê | Por quê |
|---|---|---|
| **A** (antes do dono jogar) | analisador de log; `Ajustes` com recarga; porta da `Atencao`; métrica de exposição à criatura e ao tema; legendas uniformes; conferência dos sons públicos | o dono só deve jogar quando cada sessão dele puder ser lida e corrigida sem recompilar |
| **B** | o dono joga 2 ou 3 sessões longas num mundo novo; você lê pelo analisador e mexe **só em números** | é a primeira vez que um humano sente a 0.9 |
| **C** | condutor de cenas e registro de acontecimentos | antes de crescer mais |
| **D** | corpo da criatura, itens 6a a 6e | o maior ganho de percepção pelo menor custo |
| **E** | rosto aprendido (6f), a fase 5 e o epílogo | dá sentido e fim ao arco |
| **F** | amigos: alvo único, marcado da noite, voz opcional | quando houver servidor marcado |

**O que não fazer até o fim da fase B:** nenhum evento, item ou lugar novo.

Continuam valendo as regras: nada alto, nada que mate, nada que estrague a construção. E toda mudança de número deve vir justificada por uma linha de log.

---

## 12. Higiene

**O repositório ainda era público quando conferi.** Com o `PLANO-EXPANSAO.md`, o `DESIGN-SPOILERS.md`, os títulos das PRs e até os nomes das classes (`Avesso`, `Erguidos`, `CasaDoVigia`, `Conta`), qualquer amigo que abrir perde a surpresa. Sugira ao dono torná-lo privado. É um clique e não muda nada no fluxo de vocês.

---

# O que foi conferido e feito (por quem programa)

Atualizado em 09/10/2026, na 0.9.0-alpha11.

| Ponto da revisão | Conferência | Situação |
|---|---|---|
| Sons que ainda eram públicos (seção 5) | **Confirmado.** A alpha7 só olhou `ModSons.tocar`; ficaram doze chamadas `level.playSound(null, ...)` de assombração: passos fantasmas, o passo único, o eco de ação, a porta e as batidas do sinal falso, a batida na porta, o som atrás, a porta do presságio e o passo da cena da casa | Corrigido na alpha11: tocam só para o alvo (`ModSons.tocarEventoPara`). Continuam públicos os sons de coisa que aconteceu de verdade: porta que abriu, vela que apagou, itens, blocos, e os sons que saem do corpo da criatura |
| Analisador de log (3a) | — | Feito na alpha11: `ferramentas/log/analisar.py` |
| Densidade (seção 1) | **Confirmado com número.** O analisador, rodado no log de 08/10 (antes da expansão, 1h10 de jogo): 45 saídas por hora, mediana de 56 s entre uma e outra (72 s na fase 2, 39 s na fase 3), 34 manifestações da criatura por hora. Bem abaixo das referências de 180 s e 90 s | A fazer: a porta da `Atencao`, medida numa sessão sintética |
| Fundo grave no `RECUANDO` (seção 7) | **Confirmado.** O fundo seguia o "peso" (fase e obsessão) e não sabia do estado do Diretor | Na alpha11 o peso cai à metade no `RECUANDO` |
| Legendas que denunciam o aviso falso (seção 7) | **Não se confirma.** O aviso verdadeiro e o falso usam os mesmos sons (`prenunciar` é um método só; o assobio é o mesmo `assobiar`; o sinal falso usa os sons do jogo e do mod que as aparições usam). Não há legenda exclusiva de um dos lados | Nada a fazer |
| Visibilidade da criatura só para o alvo (seção 10) | A criatura é uma entidade comum: quem estiver perto vê | Fica para a fase F, como a revisão propõe |
| Repositório público (seção 12) | **Confirmado** em 09/10/2026 (`gh repo view`: PUBLIC). A conta que eu uso não tem permissão para mudar | Avisado ao dono |
| `Ajustes`, exposição, sessão sintética, condutor de cenas, corpo da criatura, fase 5, voz | — | A fazer, na ordem da seção 11 |
