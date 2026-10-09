# Sussurros — mod de terror (Fabric, Minecraft 26.2)

## Preparar (só na primeira vez)
1. Instale o **JDK 25** (Temurin, em adoptium.net) e o **IntelliJ IDEA Community**.
2. Extraia esta pasta em algum lugar fácil (ex.: Documentos/sussurros).
3. IntelliJ: File > Open > escolha a pasta `sussurros`. Clique em "Trust Project".
4. File > Settings > Build, Execution, Deployment > Build Tools > Gradle:
   em "Gradle JVM" escolha o 25. Espere o Gradle terminar (a 1ª vez demora 5-15 min).

## Rodar o jogo
Aba **Gradle** (lado direito) > sussurros > Tasks > fabric > **runClient**.

No jogo de teste você agora se chama sempre **Jogador** (antes o nome mudava a cada vez, e o mod
achava que era outra pessoa: a fase voltava a 0). Isso também significa que a memória do mod
**continua de uma sessão para a outra**. Para começar um teste do zero, use `/sussurros esquecer`.

## Shaders
Baixe no Modrinth as versões para **26.2** do Iris e do Sodium e coloque os .jar em `run/mods`.
Coloque um shader pack (.zip) em `run/shaderpacks`. Ative em Opções > Vídeo > Shader Packs.

## Gerar o .jar final
Gradle > Tasks > build > **build**. Sai em `build/libs/` (use o sussurros-0.9.0-alpha1.jar, NÃO o que termina em -sources).

## Som e tela
- Deixe audíveis, no menu de som do jogo, "Criaturas hostis" e "Ambiente". Fone é melhor.
- Se algum efeito de tela incomodar, dá para desligar sem perder o resto: `/sussurros_tela cor nao` e `/sussurros_tela borda nao` (`sim` liga de volta). Não precisa de cheats.

## 0.9.0-alpha1
Primeira parte de uma expansão grande: sons e trilha, uma camada nova no cliente, acontecimentos novos e a caçada refeita. Sem detalhes aqui, de propósito. O que testar está no `ROTEIRO-DE-TESTE.md`, que não tem spoiler.



## 0.8.0-alpha1 — Aparições 2.0 + Hóspede que procura

Esta alpha introduz um sistema reutilizável para escolher pontos plausíveis de aparição. As manifestações agora compartilham uma camada de posicionamento que considera campo de visão, cobertura, iluminação, distância e reutilização recente de lugares.

A criatura e as cenas existentes continuam sendo usadas; o objetivo é melhorar a qualidade das posições escolhidas, não transformar o mod em spam de aparições.

Detalhes em `MUDANCAS-0.8.0-alpha1.md`.

## 0.6.0-alpha1 — O Mundo Já Estava Errado
Esta é a primeira alpha da 0.6 focada em **conteúdo ambiental e exploração**:

- **Presságios na fase 0**: animais, ecos, cinza, ruídos e luz impossível sem revelar o Hóspede cedo;
- oito eventos novos para preencher a **fase 1**;
- sistema de **orçamento atmosférico + cooldown por família + anti-repetição**;
- animais podem encarar o vazio, o jogador ou acompanhar algo invisível;
- tochas podem piscar, migrar, surgir, apagar em sequência e raramente quebrar com drop em fase alta;
- alterações físicas temporárias se restauram sem sobrescrever mudanças do jogador;
- contexto ABERTO/FLORESTA/OUTRO ganhou **histerese**, reduzindo troca a cada poucos blocos;
- três pequenas estruturas narrativas: **Marco de Estrada, Posto de Vigília e Nicho Selado**;
- novo **Caderno de Vestígios**, uma progressão investigativa sem radar/coordenadas;
- diário ampliado para **22 páginas**.

Detalhes em `MUDANCAS-0.6.0-alpha1.md`.

## 0.5.0-alpha4 — Marcas e Soleiras
Esta alpha expande **Corpo + Mundo + Ferramentas**:

- novo evento **PEGADAS**, que desenha um vestígio curto sobre pontos reais do seu Rastro sem garantir criatura;
- **Vestígios persistentes**: Olho e Sino podem reencontrar lugares onde algo realmente aconteceu;
- nova **Isca Pálida**: sugere onde a próxima PRESENÇA deve acontecer, mas o Hóspede aprende a ignorá-la;
- nova cena doméstica **Do outro lado do vidro**, usando janelas reais da casa;
- três pequenas variações de silhueta e olhos pálidos ocasionais no Hóspede, ainda sem dependência externa;
- diário ampliado para **18 páginas**;
- wrapper Gradle corrigido para chamar `GradleWrapperMain` corretamente.

Receita da Isca Pálida: Cinza Pálida + barbante + farinha de osso + carne podre → 2 iscas.

Detalhes em `MUDANCAS-0.5.0-alpha4.md`.


## 0.5.0-alpha3 — Vestígios e Contramedidas
Esta alpha expande a parte mais crua do mod hoje: **o que o jogador pode fazer com a assombração**, e não só o que o Diretor faz com o jogador.

- manifestações naturais percebidas podem deixar **Cinza Pálida** no mundo;
- a cinza vira recurso para novas ferramentas e também pode aparecer raramente em estruturas antigas;
- **Sino Oco**: chama uma resposta. Ela pode vir do Hóspede, de um ponto antigo do seu Rastro ou não vir. Depois de muito uso, o Diretor pode aprender a responder do lugar errado;
- **Fio de Vigília**: marca por 3 minutos uma pequena área. Não repele nada; se o Hóspede realmente cruzar o ponto, o fio se rompe;
- o diário cresce de 10 para 14 páginas e passa a explicar essas contramedidas dentro da lore;
- loot de mansão, cidade ancestral, biblioteca de fortaleza e mina abandonada pode antecipar raramente esses itens.

Receitas:
- Sino Oco: ferro + osso + barbante + Cinza Pálida;
- Fio de Vigília: barbante + farinha de osso + Cinza Pálida → 3 fios.

Detalhes em `MUDANCAS-0.5.0-alpha3.md`.


## 0.5.0-alpha2 — Memória Física
A alpha2 faz a memória ficar perceptível na gameplay, sem trocar o núcleo do Diretor:

- novo contexto **FLORESTA**, baseado na geometria do lugar e não no nome do bioma;
- novo evento **SEGUIDOR**: passos percorrem pontos reais do seu Rastro, sem garantir criatura;
- marcos persistentes novos guardam também o ponto exato de um acontecimento forte;
- nova cena **Foi aqui**: ao revisitar um marco, o lugar antigo pode ser reutilizado em som + manifestação + espreita + silêncio;
- Perfil começa a mudar a geometria da PRESENCA, não apenas seus pesos;
- o Hóspede muda discretamente a postura depois que é percebido pela primeira vez.

Detalhes em `MUDANCAS-0.5.0-alpha2.md`. Preserve a alpha1: esta ainda é uma build experimental grande.

## 0.5.0-alpha1 — O Mundo Lembra
Esta build é uma atualização grande de gameplay em cima da 0.4.3-exp1. O Diretor agora distingue **casa**, **subsolo**, **área aberta** e **outros lugares**, e usa o mesmo cérebro de formas diferentes conforme o ambiente.

Principais mudanças:
- nova cena **Na linha das árvores**, com duas aparições espaciais em lados diferentes e preferência por cobertura;
- novo evento **SINAL**, que produz falsos positivos contextuais e não garante que exista criatura;
- PRESENÇA comum passa a tentar posições com cobertura antes do fallback normal;
- tochas normalmente apagam por alguns segundos e voltam, em vez de serem sempre removidas;
- sons de caça, toque e desaparecimento deixam de depender das assinaturas do Warden, shrieker e Enderman;
- seleção de eventos recebe peso por contexto: casa favorece porta/batida, subsolo favorece eco/ruído, campo aberto favorece presença;
- telemetria registra contexto e a nova cena para comparar percepção com o que o Diretor realmente fez.

É uma **alpha experimental**. Preserve sua versão anterior e teste primeiro em um mundo de desenvolvimento.

## Comandos de teste (precisa de cheats ligados)
- `/sussurros fase <0-4>` pula para uma fase. Subindo, entrega o que as fases puladas dariam
  (página, Olho). Descendo, o tempo de assombração volta junto (o chat avisa).
- `/sussurros evento <nome>` força um evento. Ele acontece de verdade, mas **não conta** para o
  aprendizado nem para o ritmo, e a criatura criada assim não deixa marcas na memória.
  Se não puder acontecer, o chat diz exatamente o que falta.
- `/sussurros obsessao <0-100>` ajusta a obsessão dele, para testar a sequência de ameaça sem esperar
  (ela ainda precisa de fase 3+ e de um bom momento: escuro, sozinho, longe de casa).
- `/sussurros cena casa` começa a cena "Ele voltou com você" (teste: não conta para o aprendizado).
- `/sussurros cena eco` toca um eco de algo que você fez, vindo de onde você fez (teste).
- `/sussurros cena tunel` força a cena "Algo no túnel" sem aprendizado.
- `/sussurros cena campo` força a cena "Na linha das árvores" sem aprendizado.
- `/sussurros cena marco` força a cena "Foi aqui" sem exigir um marco real e sem aprendizado.
- `/sussurros cena janela` força a cena "Do outro lado do vidro" (precisa de vidro próximo).
- `/sussurros evento sinal` força um falso positivo contextual sem criar o Hóspede.
- `/sussurros evento seguidor` força passos que percorrem pontos reais do Rastro (precisa caminhar antes).
- `/sussurros evento pegadas` força um vestígio de cinza pelo Rastro (não salva a marca por ser comando).
- `/sussurros pressagio` força um presságio leve sem aprendizado.
- `/sussurros cena animais` força animais a encarar um ponto estranho.
- `/sussurros cena luz` força uma sequência de luzes (precisa de tochas próximas).
- `/sussurros cena caminho` força uma pequena alteração física temporária fora da tela.
- `/sussurros cena curral` força comportamento animal de rastreamento.
- `/sussurros estrutura marco|posto|nicho` gera uma estrutura narrativa fora da tela para teste.
- `/sussurros memoria` mostra o que o mod sabe sobre você (spoiler)
- `/sussurros esquecer` zera tudo. **Use antes de começar um teste novo** se o mundo já foi
  bagunçado com comandos.
- O ovo gerador do Hóspede é só para ver o modelo: bater nele não afeta a criatura de verdade.

## Spoilers
Tudo sobre como o mod funciona está em `DESIGN-SPOILERS.md`. Só leia se quiser.

## Log de depuração (para ajustar o mod sem estragar a surpresa)
`/sussurros debug on` grava as decisões do Diretor em `run/sussurros-debug.log`.
**Não abra esse arquivo**: ele explica tudo o que acontece. Só envie para quem está ajustando o mod.
`/sussurros debug off` desliga. `/sussurros debug` sozinho mostra se está ligado.

## Se os sons próprios derem erro ao compilar
Os sons do mod ficam isolados em `src/main/java/com/sussurros/registro/ModSons.java`.
Se só esse arquivo der erro, copie `alternativas/ModSons_sem_sons.java` por cima dele
(mantendo o nome `ModSons.java`). O resto do mod funciona igual, só sem esses sons.
