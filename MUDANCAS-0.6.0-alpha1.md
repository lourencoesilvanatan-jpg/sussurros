# Sussurros 0.6.0-alpha1 — O Mundo Já Estava Errado

A 0.6 começa a tratar Sussurros como um mod de conteúdo de horror, não apenas um Diretor com um stalker. O objetivo desta alpha é preencher o começo da partida e fazer o mundo vanilla participar da assombração sem transformar tudo em spam ou griefing.

## 1. Presságios antes da fase 1
A fase 0 continua sem Hóspede natural, mas deixa de ser vazia. Depois de uma graça inicial de ~2–4 minutos, acontecimentos muito leves podem aparecer com bastante espaço entre eles:

- passo/ruído distante;
- eco tardio de uma ação do jogador;
- animais encarando o mesmo ponto vazio;
- animais encarando o jogador por alguns segundos;
- animais acompanhando com a cabeça um ponto invisível em movimento;
- pequena cinza num ponto do Rastro;
- tocha fantasma temporária fora da tela;
- madeira/estalo do lado errado de uma parede.

Presságios **não** contam como evento do Diretor: não aumentam pressão, não alimentam obsessão e não treinam pesos. São textura para os primeiros minutos.

## 2. Fase 1 com repertório real
A fase 1 ganhou oito eventos normais para não ficar presa a PASSOS/ECO/PASSO_UNICO:

- `PASSAGEM` — ruído cruza lateralmente o ambiente;
- `ANIMAIS` — comportamento animal anormal;
- `VESTIGIO` — cinza curta num ponto do caminho recente;
- `LUZ_ERRADA` — luz pisca, migra, aparece e, muito raramente em fase alta, quebra com drop;
- `SINAL_DISTANTE` — som em posição distante/Rastro;
- `RUIDO_RETORNO` — devolve uma ação real do jogador;
- `OBJETO_FORA_LUGAR` — pequena alteração física temporária fora da tela;
- `TRILHA_INTERROMPIDA` — uma sequência curta de cinza que simplesmente para.

OBSERVANDO aceita intensidade até 13 nesta versão; TESTANDO/ESCALANDO continuam assumindo os eventos mais fortes. O piso de fase 1/2 também foi encurtado para diminuir trechos mortos sem transformar o jogo em susto constante.

## 3. Atmosfera com orçamento, famílias e anti-repetição
Nova classe `Atmosfera` controla conteúdo ambiental de forma separada do Diretor.

Ela usa:
- **orçamento atmosférico** limitado, que regenera lentamente;
- **cooldown por família** (`ANIMAIS`, `LUZ`, `OBJETO`, `RUIDO`, `VESTIGIO`);
- memória das últimas 5 manifestações ambientais para reduzir repetição;
- bloqueio durante cenas do Diretor, zona da Vela, dano recente e testes em andamento.

A consequência prática é que ter 20+ manifestações disponíveis não significa executá-las em sequência.

## 4. Animais perturbados
Animais vanilla próximos podem, raramente:
- parar e encarar o mesmo ponto vazio;
- encarar o jogador;
- acompanhar lentamente um alvo invisível que atravessa o ambiente.

O efeito dura poucos segundos, possui cooldown de vários minutos e não garante criatura depois. A navegação normal volta ao terminar.

## 5. Luz Errada
A família de luz foi expandida:
- **PISCA**: uma tocha some e volta;
- **MIGRA**: uma tocha some enquanto outra aparece temporariamente em outro lugar;
- **APARECE/FANTASMA**: uma tocha temporária surge onde o jogador não colocou;
- **SEQUENCIA**: duas ou três tochas apagam em sequência e depois voltam;
- **QUEBROU_COM_DROP**: somente fase 3+, uma chance pequena dentro do evento pode realmente quebrar uma tocha — com drop do item.

A maior parte das alterações usa `AlteracoesTemporarias`: o estado original é guardado e só é restaurado se o jogador não tiver mexido naquele bloco durante o evento.

## 6. Microcenas ambientais
Além das cenas tradicionais do Hóspede, a Atmosfera pode montar pequenas perturbações sem criatura:

- `TODOS_OLHANDO`;
- `LUZ_NO_FIM`;
- `PASSOU_PELA_MINA`;
- `O_CAMINHO_MUDOU`;
- `HA_ALGO_NO_CURRAL`;
- `NAO_FOI_VOCE`;
- `MARCA_IMPOSSIVEL`.

Elas usam o mesmo orçamento/cooldown e normalmente ficam separadas por vários minutos.

## 7. Alterações físicas reversíveis
`AlteracoesTemporarias` registra dimensão, posição, estado original e estado colocado. Na restauração:

- só desfaz a mudança se o bloco ainda for exatamente o que Sussurros colocou;
- respeita alterações feitas pelo jogador;
- não mistura alterações entre dimensões.

Isso permite caminhos/marcas/luzes estranhas sem usar griefing como principal fonte de terror.

## 8. Contexto com histerese
O log anterior mostrou `ABERTO/FLORESTA/OUTRO` trocando rápido demais. Agora:

- CASA e SUBSOLO entram imediatamente;
- sair de CASA/SUBSOLO precisa de ~2 s estáveis;
- ABERTO/FLORESTA/OUTRO precisam de ~4 s estáveis antes da troca.

Assim os pesos do Diretor não mudam porque o jogador passou ao lado de uma única árvore.

## 9. Estruturas narrativas pequenas
A primeira rodada de conteúdo de exploração entra como estruturas compactas geradas fora da tela e registradas na memória:

### Marco de Estrada
Pequena marca de pedra/cerca que pode surgir depois de alguma exploração.

### Posto de Vigília
Ruína curta de pedra e madeira, longe da casa. Pode conter Página Rasgada, Cinza Pálida e raramente Caderno/Fio.

### Nicho Selado
Pequeno nicho subterrâneo de deepslate com barril e vestígios de investigação anterior.

São deliberadamente pequenas e únicas por jogador/mundo nesta alpha. Não são dungeons. `/sussurros esquecer` preserva os flags/coordenadas de geração para não duplicar blocos físicos a cada reset de teste.

## 10. Caderno de Vestígios
Novo item opcional de investigação.

Ele não mostra coordenadas nem funciona como radar. Resume apenas o quanto o jogador já conseguiu provar em quatro estágios, usando:
- vestígios persistentes;
- estruturas encontradas;
- fios rompidos;
- sino/iscas;
- encontros prévios.

Receita: **Livro + Carvão Vegetal + Cinza Pálida**.

O Caderno também pode aparecer raramente no Posto de Vigília e em loot vanilla compatível.

## 11. Diário
O diário cresce de 18 para **22 páginas**. As novas entradas apresentam:
- animais encarando algo que não aparece;
- luz impossível na mina;
- um Posto de Vigília num caminho familiar;
- a ideia de que o próprio mundo parece participar da observação.

## 12. Estrutura de código
Esta alpha evita empilhar tudo no `Diretor.java`. O conteúdo novo foi separado em:

- `Atmosfera.java`;
- `Pressagio.java`;
- `Perturbacao.java`;
- `AlteracoesTemporarias.java`;
- `EstruturasSussurros.java`;
- `ProgressoInvestigacao.java`;
- `CadernoVestigiosItem.java`.

O Diretor continua decidindo ritmo, memória e aprendizado. A Atmosfera cuida do repertório ambiental e do seu limite de recorrência.

## Comandos de teste novos
- `/sussurros pressagio`
- `/sussurros cena animais`
- `/sussurros cena luz`
- `/sussurros cena caminho`
- `/sussurros cena curral`
- `/sussurros estrutura marco`
- `/sussurros estrutura posto`
- `/sussurros estrutura nicho`

Os eventos de fase 1 também podem ser forçados com `/sussurros evento <nome>`.

## Smoke test recomendado
1. `/sussurros esquecer`
2. `/sussurros debug on`
3. `/sussurros pressagio` algumas vezes em superfície, perto de animais e em caverna
4. `/sussurros fase 1`
5. force `animais`, `luz_errada`, `passagem`, `vestigio`, `ruido_retorno` e `objeto_fora_lugar`
6. `/sussurros cena animais`, `/sussurros cena luz`, `/sussurros cena caminho`
7. teste as três estruturas, uma por vez
8. pegue o Caderno de Vestígios no criativo e use antes/depois de gerar evidências
9. faça depois uma sessão natural separada, começando de fase 0.

## Limitações desta alpha
- As três estruturas são geração narrativa em runtime, não um sistema completo de worldgen por biome/structure set ainda.
- O Hóspede continua sem GeckoLib.
- Os olhos ainda não são emissivos reais.
- A build deve ser considerada experimental até passar pelo `runClient`/build local com JDK 25 e dependências Fabric/Minecraft reais.
