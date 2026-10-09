# Telemetria da 0.4.3-exp1

A telemetria da 0.4.2a-test foi mantida. A 0.4.3-exp1 adiciona gameplay experimental e preserva os mesmos IDs de manifestação/cena para comparar percepção e comportamento.
Ligue com `/sussurros debug on`. Tudo abaixo só é escrito com o log ligado.

## IDs
- `M001, M002...` cada Hóspede (manifestação), do nascimento ao sumiço. Vale para todas as origens
  (DIRETOR, OLHO, COMANDO); a origem aparece em `origem=`. O ID fica na própria criatura.
- `C001, C002...` cada cena: `VOLTOU_COM_VOCE` (casa) e `AMEACA` (sequência de ameaça).
- Os dois contadores são simples (+1). Reiniciam quando o mundo/servidor fecha. Não são salvos.

## Linhas novas

### Hóspede
```
HOSPEDE id=M017 criado origem=DIRETOR evento=PRESENCA modo=OBSERVAR pos=(x,y,z) dist=27.4 motivoPosicao=RASTRO idadeRastro=62s distRastro=27 cena=-
HOSPEDE id=M017 primeira vista: origem=DIRETOR evento=PRESENCA aprende=sim
HOSPEDE id=M017 PERCEBEU n=1 dist=27.4 ang=39 luz=5 modo=OBSERVAR
HOSPEDE id=M017 PERCEBEU n=2 dist=22.8 ang=31 luz=4 modo=ESPREITAR foraDaTela=6.5s
HOSPEDE id=M017 ENCAROU n=1 dist=25.8 ang=8 luz=5 modo=OBSERVAR
HOSPEDE id=M017 sumiu motivo=VISTO_DEMAIS viveu=19s dist=24.0 vezesNaTela=2 reposicoes=1
```
- `PERCEBEU` = entrou na sua tela (cone ~45°). Só repete depois de pelo menos 1 s fora da tela
  (`foraDaTela`), para não encher o log quando ele fica na borda.
- `ENCAROU` = você olhou direto (cone ~15°). Uma vez por criatura.
- `ang` = graus entre o centro da sua visão e ele. `luz` = luz efetiva no lugar dele (0–15).

`motivoPosicao`:
- `NORMAL ang=.. distSorteada=.. tentativa=..`: lugar sorteado em volta de você.
- `RASTRO idadeRastro=..s distRastro=..`: um ponto por onde você passou.
- `ROTA chunk=(cx,cz)`: um chunk onde você costuma passar.
- `TUMULO`, `BORDA_DA_VELA`.
- `CENA_CASA+...`: o mesmo, dentro da cena de casa.

`motivo` do sumiço: `TEMPO_ESGOTADO`, `ZONA_CALMA`, `CHEGOU_PERTO`, `VISTO_DEMAIS`, `ENCARADO_DEMAIS`
(caça), `TOCOU`, `FERIDO`, `PERDEU_CONTATO` (espreita esgotou), `SUMIR` (espreita sorteou sumir),
`LIMITE_DA_CENA`, `SUBSTITUIDA_POR_COMANDO`, `SEM_ALVO`.

### Espreita
```
ESPREITA id=M017 step=1/3 acao=APROXIMAR dist=31.2->22.8 motivo=RASTRO+COBERTURA cabecaVisivel=sim idadeRastro=62s candidatos=9 pos=(x,y,z)
ESPREITA id=M017 step=2/3 acao=FICAR dist=22.8 motivo=SORTEIO
ESPREITA id=M017 step=2/3 acao=FICAR dist=22.8 motivo=SEM_LUGAR pretendia=TROCAR_LADO
ESPREITA id=M017 step=3/3 acao=SUMIR dist=20.1
ESPREITA id=M017 step=fim/3 acao=PERDEU_CONTATO dist=20.1
```
`acao`: `APROXIMAR`, `TROCAR_LADO`, `FICAR`, `SUMIR`, `PERDEU_CONTATO`. `motivo` do lugar: `RASTRO`, `COBERTURA`,
`RASTRO+COBERTURA`, `NORMAL`. `candidatos` = quantos lugares válidos ele comparou.

### Cenas
```
CENA id=C003 tipo=VOLTOU_COM_VOCE INICIO teste=nao espera=14s
CENA id=C003 etapa=RASTRO som=3xPASSO+ESTALO motivoPosicao=RASTRO idadeRastro=48s pos=(x,y,z) dist=19.0
CENA id=C003 etapa=PORTA porta=(x,y,z) acao=MEXEU_DUAS_VEZES habitual=sim dist=6.2
CENA id=C003 etapa=PRESENCA manifestacao=M021 anuncio=nenhum      (ou PULADA motivo=FASE_2 / SEM_LUGAR)
CENA id=C003 etapa=SILENCIO duracao=117s silencioAte=4312s
CENA id=C003 FIM

CENA id=C004 tipo=AMEACA INICIO obsessao=63 V=71 fase=3
CENA id=C004 etapa=ESPREITA manifestacao=M022
CENA id=C004 etapa=PAUSA (perdeu contato) duracao=22s
CENA id=C004 etapa=GOLPE aguardando escuro (até 120s)
CENA id=C004 etapa=GOLPE evento=ATRAS manifestacao=M023
CENA id=C004 FIM motivo=GOLPE_CONCLUIDO obsessao=66->26
```
Fim da ameaça: `GOLPE_CONCLUIDO`, `GOLPE_SEM_MOMENTO`, `SEM_LUGAR`, `TEMPO_ESGOTADO`.
O Hóspede criado por uma cena traz `cena=C00x` na linha `criado`.

### Silêncio
```
SILENCIO inicio motivo=POS_AMEACA cena=C004 duracao=213s silencioAte=4525s
SILENCIO fim motivo=POS_AMEACA cena=C004
```
`motivo`: `POS_AMEACA`, `PRESSAO_ALTA`, `TEMPO_ESCALANDO` (os três são o estado RECUANDO) e `FIM_CENA_CASA`.
Uma linha no começo e uma no fim, nada no meio.

### Eco
```
ECO origemAcao=QUEBRA idade=143s posOriginal=(x,y,z) posSom=(x,y,z) motivo=LUGAR_DA_ACAO distOriginal=14 distSom=14.2
ECO origemAcao=PORTA idade=61s posOriginal=(...) posSom=(...) motivo=RASTRO idadeRastro=40s distSom=17.9
ECO origemAcao=QUEBRA idade=12s posOriginal=(...) posSom=(...) motivo=PERTO distSom=12.0
```
Todo eco repete o som de uma ação sua (`QUEBRA` de bloco ou `PORTA`). `motivo` diz de onde o som saiu:
`LUGAR_DA_ACAO` (onde você fez), `RASTRO`, `DIRECAO_DE_CASA`, `PERTO` (ponto sorteado perto),
`PERTO_CADEIA` (eco mais perto, continuação de uma cadeia).

### Seleção (intensidade-alvo + evento escolhido)
```
SELECAO estado=ESCALANDO modo=NORMAL V=66 alvoBase=17.0 bonusObsessao=5.0 bonusV=1.9 abatimento=0.0 alvoFinal=23.9 maisForte=30 escolhido=PRESENCA intensidade=22
SELECAO estado=TESTANDO modo=NORMAL V=44 alvo=- escolhido=ECO intensidade=10
```
`alvo=-` fora de ESCALANDO (lá a intensidade-alvo não existe). Substitui a antiga linha `alvo:`.
Vem logo depois da linha `escolha[...]` (pesos) e antes do `EVENTO`.

### Resumo de cada minuto e `/sussurros memoria`
Ganharam `manifestacaoAtiva=M017` (ou `-`) e `cenaAtiva=C003` (ou `-`).

### Reação (mudou na 0.8.1)

A linha `REACAO` tem o mesmo formato, mas quatro campos mudaram de significado:

- `giro`: passa a contar em rampa. Abaixo de 30° vale zero; de 60° para cima vale o máximo. Antes era um corte em 60°.
- `olhou=true`: depois do giro, ele ficou de frente para a fonte. Já estar virado para lá por acaso não conta.
- `fugiu=true`: acelerou ou começou a correr, e não foi na direção da fonte. `fugiu` e `investigou` não aparecem mais juntos.
- `investigou=true`: mudou de rumo para a fonte. Quem já vinha andando para ela não investigou.

`percepção confirmada pela reação` agora só aparece quando ele virou 45° ou mais e ficou olhando para a fonte.

Linha nova, quando há teleporte, respawn ou volta de outra dimensão:

```
SALTO: teleporte, respawn ou portal; leitura cancelada e Rastro esquecido
```

A punição `indiferença: assombração avança 300 s` só conta eventos com `obs` de 0,70 ou mais.

## Exemplo de reconstrução
```
HOSPEDE id=M017 criado ... motivoPosicao=RASTRO idadeRastro=62s       -> nasceu onde você estava 62 s antes
HOSPEDE id=M017 PERCEBEU n=1 ...                                       -> você o viu
ESPREITA id=M017 step=1/3 acao=APROXIMAR ... motivo=RASTRO+COBERTURA   -> você desviou; ele chegou mais perto, atrás de algo
HOSPEDE id=M017 PERCEBEU n=2 ... foraDaTela=7.0s                       -> você o reencontrou
HOSPEDE id=M017 sumiu motivo=VISTO_DEMAIS ...                          -> sumiu
SILENCIO inicio motivo=POS_AMEACA cena=C004 ...                        -> o Diretor deu silêncio
```


## 0.4.3-exp1 — cena de túnel
A cena experimental usa o mesmo `cenaId` das outras composições. Procure por:

- `CENA id=C... tipo=ALGO_NO_TUNEL INICIO`
- `etapa=ECO` — repete uma quebra recente do próprio jogador
- `etapa=RUIDO` — pano, madeira ou arrasto vindo do rastro/túnel
- `etapa=PRESENCA manifestacao=M...` — Hóspede em ESPREITAR, sem anúncio
- `FIM_CENA_TUNEL` — silêncio de 60–100 s após a composição

A postura visual do Hóspede agora também recebe o modo sincronizado (`OBSERVAR`, `ESPREITAR`, `ESPERAR`, `CACAR`), mas isso não adiciona uma linha de log por frame.

## 0.5.0-alpha1 — contexto e mundo

### Contexto
Quando o tipo de lugar muda:
```
CONTEXTO OUTRO -> ABERTO
CONTEXTO ABERTO -> CASA
```
A linha `SELECAO` agora inclui `contexto=CASA|SUBSOLO|ABERTO|FLORESTA|OUTRO`.

### SINAL
```
SINAL contexto=SUBSOLO tipo=ECO_CURTO_DA_ACAO semCriatura=sim pos=(x,y,z) dist=18.4
SINAL contexto=CASA tipo=PORTA_SEM_MOVER semCriatura=sim pos=(x,y,z) dist=5.2
```
O ponto importante é `semCriatura=sim`: o jogador pode reagir ao evento, mas não existe manifestação para encontrar.

### Cena de campo
```
CENA id=C007 tipo=LINHA_DAS_ARVORES INICIO teste=nao ladoInicial=esquerda espera=13s
CENA id=C007 etapa=PRIMEIRA manifestacao=M031 cobertura=sim ang=-74 dist=38.0 anuncio=nenhum
CENA id=C007 etapa=PAUSA duracao=21s
CENA id=C007 etapa=SEGUNDA manifestacao=M032 cobertura=sim ang=61 dist=25.1 anuncio=nenhum
CENA id=C007 etapa=SILENCIO duracao=101s silencioAte=...
SILENCIO fim motivo=FIM_CENA_CAMPO cena=C007
CENA id=C007 FIM
```

### Tocha
```
TOCHA acao=PISCOU pos=(x,y,z)
TOCHA acao=LEVOU pos=(x,y,z)
```
`PISCOU` é o comportamento comum; `LEVOU` fica reservado para fase 4 + obsessão alta e continua raro.

## 0.5.0-alpha2 — memória física

### Contexto FLORESTA
A transição pode aparecer como:
```
CONTEXTO ABERTO -> FLORESTA
CONTEXTO FLORESTA -> OUTRO
```
`SELECAO` pode mostrar `contexto=FLORESTA`.

### SEGUIDOR
```
SEGUIDOR pontos=4 idade=81s->54s dist=31.0->16.2 semCriatura=sim
```
- `pontos`: quantidade de posições reais do Rastro usadas;
- `idade`: idade do primeiro e último ponto da sequência;
- `dist`: distância atual desses pontos ao jogador;
- `semCriatura=sim`: o evento não materializou o Hóspede por conta própria.

### Marcos persistentes
Quando uma reação forte cria um marco novo:
```
MARCO criado evento=PRESENCA pos=(x,y,z)
```
Ao entrar novamente no chunk:
```
MARCO revisitado chunk=(cx,cz) pendente=sim
```
Se a oportunidade natural não disparar:
```
MARCO revisitado: o lugar foi reconhecido, mas ficou quieto desta vez
```

### Cena Foi aqui
```
CENA id=C011 tipo=FOI_AQUI INICIO teste=nao espera=9s contexto=FLORESTA marcoExato=(x,y,z)
CENA id=C011 etapa=ECO memoriaDoLugar=sim som=ESTALO motivoPosicao=MARCO_EXATO pos=(x,y,z) dist=17.0
HOSPEDE id=M044 criado ... motivoPosicao=CENA_MARCO+MARCO_EXATO distMarco=2.2 cobertura=sim cena=C011
CENA id=C011 etapa=PRESENCA manifestacao=M044 memoriaDoLugar=sim anuncio=nenhum
CENA id=C011 etapa=SILENCIO duracao=91s silencioAte=...
SILENCIO fim motivo=FIM_CENA_MARCO cena=C011
CENA id=C011 FIM
```
Marcos antigos podem mostrar `marcoExato=nao`; nesse caso a cena continua usando Rastro/cobertura/fallback.

### Geometria adaptativa
A estratégia é gravada junto do motivo de spawn quando não for sobrescrita por uma escolha mais específica. Para investigar diferenças por perfil, compare o resumo dos traços (`cautela`, `confronto`, `fuga`) com o `ang=` das manifestações e com `contexto=`.

### Estado visual após percepção
Não há log por frame. O primeiro `PERCEBEU` continua sendo a referência temporal: depois dele, a entidade sincroniza o estado visual usado pelo modelo para reduzir discretamente a postura torta.

---
## 0.5.0-alpha3 — Vestígios e Contramedidas

### Cinza Pálida
Quando uma manifestação natural qualificada deixa resíduo:
```
VESTIGIO CINZA motivo=VISTO_DEMAIS manifestacao=M014 pos=(x,y,z)
```

### Sino Oco
```
SINO tocado usos=3
SINO resposta=HOSPEDE manifestacao=M014 dist=23.7
SINO resposta=RASTRO idade=84s pos=(x,y,z)
SINO resposta=ISCA_RASTRO idade=61s pos=(x,y,z)
SINO resposta=SILENCIO
```
`ISCA_RASTRO` só passa a ser possível depois de uso repetido: significa que havia Hóspede perto, mas a resposta foi desviada para outro ponto.

### Fio de Vigília
```
VIGILIA armada pos=(x,y,z) raio=6.0 duracao=180s
VIGILIA rompeu manifestacao=M021 distCentro=4.3 pos=(x,y,z)
VIGILIA expirou intacta pos=(x,y,z)
```
O rompimento não é sorteio: exige a entidade ativa realmente entrar no raio naquele segundo.

---
## 0.5.0-alpha4 — Marcas e Soleiras

### PEGADAS
```
PEGADAS pontos=6 sentido=RECENTE idade=93s->51s distFim=18.3 semCriatura=sim vestigio=sim
```
Em evento forçado, `vestigio=nao`; comandos de teste não criam memória persistente.

### Isca Pálida
```
ISCA armada pos=(x,y,z) duracao=120s
ISCA ignorada atendidas=4 chance=0.24
HOSPEDE id=M... criado ... motivoPosicao=ISCA distIsca=...
ISCA expirou pos=(x,y,z)
```
A chance de ignorar cresce apenas depois de a isca ter funcionado algumas vezes.

### Vestígios
```
OLHO vestigio tipo=PEGADAS idade=71s dist=22 pos=(x,y,z)
SINO resposta=VESTIGIO tipo=VIGILIA idade=114s pos=(x,y,z)
```
São coordenadas persistentes reais, não manifestações ativas.

### Cena da janela
```
CENA id=C014 tipo=DO_OUTRO_LADO_DO_VIDRO INICIO teste=nao espera=11s
HOSPEDE id=M052 criado ... motivoPosicao=CENA_JANELA+JANELA vidro=(x,y,z) cena=C014
CENA id=C014 etapa=APARICAO manifestacao=M052 vidro=(x,y,z) anuncio=nenhum
CENA id=C014 etapa=VIU_ATRAVES_DO_VIDRO manifestacao=M052
CENA id=C014 etapa=TOQUE_NO_VIDRO
CENA id=C014 etapa=SILENCIO duracao=73s silencioAte=...
CENA id=C014 FIM
```
`TOQUE_NO_VIDRO` só aparece quando o jogador não encontrou a presença imediatamente.

### Aparência
Variação corporal e olhos pálidos são apresentação cliente e não geram uma linha por frame. Use o ID `M...` para correlacionar uma manifestação visual com os eventos normais de telemetria.

---
## 0.6.0-alpha1 — O Mundo Já Estava Errado

### Presságios
Presságios existem fora do seletor normal e nunca contam para aprendizado:
```
PRESSAGIO tipo=ANIMAIS_VAZIO fase=0 contexto=ABERTO semAprendizado=sim
PRESSAGIO tipo=LUZ_DISTANTE fase=0 contexto=SUBSOLO semAprendizado=sim
```

### Orçamento atmosférico
O resumo por minuto e `/sussurros memoria` incluem `atm=`. É um orçamento curto separado de pressão/obsessão; eventos ambientais gastam pontos e o valor regenera lentamente.

### Animais
```
ANIMAIS tipo=VAZIO qtd=4 duracao=5s semCriatura=sim
ANIMAIS tipo=JOGADOR qtd=3 duracao=4s semCriatura=sim
ANIMAIS tipo=MOVIMENTO qtd=2 duracao=5s semCriatura=sim
```

### Luz Errada
```
LUZ_ERRADA tipo=FANTASMA pos=(x,y,z) duracao=7s semCriatura=sim
LUZ_ERRADA tipo=PISCA pos=(x,y,z) semCriatura=sim
LUZ_ERRADA tipo=MIGRA pos=(x,y,z) semCriatura=sim
LUZ_ERRADA tipo=SEQUENCIA qtd=3 semCriatura=sim
LUZ_ERRADA tipo=QUEBROU_COM_DROP pos=(x,y,z) semCriatura=sim
```
`QUEBROU_COM_DROP` é raro e só entra em fase alta; as outras alterações são temporárias.

### Mundo fora do lugar
```
OBJETO tipo=CAMINHO_MUDOU pos=(x,y,z) duracao=73s
OBJETO tipo=MARCA_IMPOSSIVEL pos=(x,y,z) duracao=183s
OBJETO tipo=NAO_FOI_VOCE porta=BlockPos{...} semCriatura=sim
PASSAGEM lado=esquerda semCriatura=sim
VESTIGIO tipo=CINZA_RASTRO pos=(x,y,z) semCriatura=sim
SINAL_DISTANTE pos=(x,y,z) dist=31.4 semCriatura=sim
RUIDO_RETORNO origemAcao=QUEBRA idade=102s pos=(x,y,z)
TRILHA_INTERROMPIDA pontos=4 fim=(x,y,z) semCriatura=sim
```

### Microcenas ambientais
```
ATMOSFERA cena=TODOS_OLHANDO teste=nao contexto=FLORESTA
ATMOSFERA cena=LUZ_NO_FIM teste=nao contexto=SUBSOLO
ATMOSFERA cena=PASSOU_PELA_MINA teste=nao contexto=SUBSOLO
ATMOSFERA cena=O_CAMINHO_MUDOU teste=nao contexto=ABERTO
ATMOSFERA cena=HA_ALGO_NO_CURRAL teste=nao contexto=CASA
ATMOSFERA cena=NAO_FOI_VOCE teste=nao contexto=CASA
ATMOSFERA cena=MARCA_IMPOSSIVEL teste=nao contexto=OUTRO
```

### Contexto com histerese
Transições ambientais não domésticas aparecem somente depois de estabilidade:
```
CONTEXTO OUTRO -> FLORESTA (estavel=4s)
CONTEXTO CASA -> ABERTO (estavel=2s)
CONTEXTO OUTRO -> CASA (estavel=0s)
```

### Estruturas
```
ESTRUTURA gerada=MARCO_ESTRADA pos=BlockPos{...} foraDaTela=sim
ESTRUTURA gerada=POSTO_VIGILIA pos=BlockPos{...} foraDaTela=sim
ESTRUTURA gerada=NICHO_SELADO pos=BlockPos{...} foraDaTela=sim
ESTRUTURA descoberta=POSTO_VIGILIA pos=(x,y,z)
```

### Caderno
O Caderno não revela coordenadas no log nem no HUD; os seus estágios são derivados de vestígios, estruturas e contramedidas já persistidas. `CADERNO_USOS` existe apenas para diagnóstico/progressão futura.

## 0.8.1 — evento sorteado que não coube

Antes, quando o evento sorteado não conseguia executar (sem chão livre, sem ponto do Rastro, sem orçamento), a linha `SELECAO` aparecia sem `EVENTO` depois e o ciclo era perdido. Agora o Diretor sorteia outro, até três por segundo:

```
SELECAO estado=ESCALANDO modo=NORMAL contexto=SUBSOLO V=73 ... escolhido=TRILHA_INTERROMPIDA intensidade=15
SELECAO falhou evento=TRILHA_INTERROMPIDA tentativa=1/3
SELECAO estado=ESCALANDO modo=NORMAL contexto=SUBSOLO V=73 ... escolhido=VESTIGIO intensidade=12
EVENTO VESTIGIO (estado=ESCALANDO, obs=0.50, pressao=12)
```

Se nenhuma das três tentativas couber, ele espera de 15 a 30 s antes de tentar de novo, sem mexer na agenda:

```
SELECAO sem lugar: nada coube agora; tenta de novo em 22s
```

Para conferir: toda linha `SELECAO ... escolhido=` é seguida de `EVENTO` ou de `SELECAO falhou`.

## 0.8.1 — silêncio de verdade

Quando o mundo emudece para um jogador (música e som ambiente cortados, mobs em volta sem som de fundo por alguns segundos):

```
SILENCIO_REAL motivo=APARICAO duracao=25s mobs=6
SILENCIO_REAL motivo=SINAL duracao=15s mobs=0
SILENCIO_REAL motivo=PRESSAGIO duracao=15s mobs=3
```

`APARICAO` sai logo depois de `HOSPEDE ... criado`, em cerca de 70% das vezes. `SINAL` e `PRESSAGIO` são os avisos falsos: o mesmo silêncio, sem criatura.

## 0.8.1 — vulto distante

```
HOSPEDE id=M004 criado origem=DIRETOR evento=VULTO modo=VULTO pos=(...) dist=63.2 motivoPosicao=VULTO dist=63 altura=+7 cena=-
HOSPEDE id=M004 PERCEBEU n=1 dist=61.0 ang=38 luz=15 modo=VULTO
HOSPEDE id=M004 sumiu motivo=VULTO_MIRADO viveu=6s dist=60.4 vezesNaTela=1 reposicoes=0
```

Motivos de sumiço do vulto: `VULTO_MIRADO` (o jogador mirou nele por um segundo), `CHEGOU_PERTO` (chegou a 36 blocos) e `TEMPO_ESGOTADO` (ninguém olhou). `altura` é quantos blocos ele está acima ou abaixo do jogador.

## 0.8.1 — sumiço rápido e avistamento

Motivos novos de sumiço:

| Motivo | Quando |
|---|---|
| `VULTO_MIRADO` | o vulto entrou no miolo da tela (30° do centro) |
| `VULTO_VISTO` | o vulto ficou 0,5 a 0,8 s na borda da tela |
| `VULTO_DESVIOU` | o vulto foi visto e saiu da tela |
| `SUMIU_NO_DESVIO` | presença com ousadia menor que 4: foi olhada direto e o jogador desviou |

Para conferir o pedido "não dar tempo de focar": entre `PERCEBEU` e `sumiu` de um vulto ou de uma presença com ousadia baixa deve passar, no máximo, um segundo.

Linhas novas:

```
avistamento encarado: conta como reação (c 0.00 -> 0.35)
OLHO usado fase=1 tempo=742 (+90)
```

A linha de cada minuto ganhou `tempo=NNN` (segundos de assombração acumulados), logo depois de `fase=`. As fases começam em 600, 1800, 3300 e 5400. Usar o Olho soma 90, ler uma página soma 60 e a punição por indiferença soma 300.

## 0.8.1 — miragens

As linhas de luz ganharam `miragem=sim` quando o bloco existe só para o jogador:

```
LUZ_ERRADA tipo=FANTASMA miragem=sim pos=(-569,49,602) duracao=31s semCriatura=sim
LUZ_ERRADA tipo=APARECE miragem=sim pos=(...) semCriatura=sim
LUZ_ERRADA tipo=VERMELHA miragem=sim pos=(...) semCriatura=sim
```

Quando a miragem se desfaz:

```
MIRAGEM fim motivo=LUZ_FANTASMA por=CHEGOU_PERTO pos=(-569,49,602)
MIRAGEM fim motivo=TOCHA_VERMELHA por=TEMPO pos=(...)
```

Se o jogador clica na miragem, o jogo a desfaz sozinho na hora e não há linha de log; a linha `MIRAGEM fim` aparece depois, quando o tempo dela vence.

## 0.9.0-alpha1 — sentidos, eventos novos e caçada

### Eventos novos

```
ECO_PASSOS janela=31s
VIGIA falsa forca=0.62 duracao=22s
NEBLINA forca=0.58 duracao=94s
CANTIGA pos=(x,y,z) dist=33
PRENUNCIO real=nao luz=1 assobio=nao
PRENUNCIO real=sim luz=0 assobio=sim
```

`PRENUNCIO real=sim` sai no começo de toda caçada; `real=nao` é o evento que só avisa. `luz` é quantas luzes falharam.

### Caçada

Toda linha começa por `CACA id=M...` (o mesmo ID da manifestação).

```
CACA id=M031 AVISO -> PERSEGUE motivo=FIM_DO_AVISO
CACA id=M031 piscar n=2 dist=18.4
CACA id=M031 atalho n=1 de=22 para=11
CACA id=M031 abriu porta pos=x, y, z
CACA id=M031 ferido n=1 recuou=sim
CACA id=M031 PERSEGUE -> ESPERA_VELA motivo=ALVO_NA_VELA soprar=nao
CACA id=M031 soprou a vela
CACA id=M031 PERSEGUE -> ATRAVESSA motivo=SEM_CAMINHO bloco=x, y, z
CACA id=M031 ATRAVESSA -> PERSEGUE motivo=ATRAVESSOU_VAZIO
CACA id=M031 PERSEGUE -> FINGE motivo=FINGIU_PERDEU_RASTRO
CACA id=M031 FINGE -> PERSEGUE motivo=VOLTOU
CACA id=M031 CAPTURA como=TOCOU duracao=27s contato=13s
CACA id=M031 FIM motivo=TETO_BUSCA duracao=82s contato=0s piscadas=0 atalhos=0 golpes=0 fingiu=sim
CACA terminou motivo=TETO_BUSCA cacadas=3 proximaAmeacaEm=1766s
```

- Estágios: `AVISO`, `PERSEGUE`, `ESPERA_VELA`, `FINGE`, `ATRAVESSA`.
- `motivo` do fim: `TETO_CONTATO`, `TETO_BUSCA`, `TETO_TOTAL`, `PERDEU_RASTRO`, `VELA`, `TOCOU`, `ZONA_CALMA` (a vela foi acesa com ele dentro), `SEM_ALVO`.
- `CAPTURA como=`: `TOCOU` ou `ATRAVESSOU`.
- `contato` é o relógio que só anda com ele fora da tela e sabendo onde o jogador está.
- A linha `CACA terminou` só sai em caçada que conta (não em teste por comando).
- A busca ganhou o estado `OLHANDO` e o motivo `OUVIU_QUEBRA`, `OUVIU_PORTA`, `OUVIU_BAU`, `OUVIU_ATALHO`.

### Captura e marca

```
CAPTURA inicio pos=(x,y,z) vida=20 teste=nao
CAPTURA deslocou de=(x,y,z) para=(x,y,z) dist=38 vida=12 marcas=1
ESTADO ESCALANDO -> RECUANDO (captura)
MARCA curada (dormiu com a vela acesa)
```

### Luz

`LUZ_ERRADA tipo=FANTASMA` ganhou `lugar=RASTRO|EM_VOLTA`. `PISCA` e `MIGRA` ganharam `miragem=sim`. As miragens novas aparecem em `MIRAGEM fim motivo=...` como `LUZ_APAGADA` (caça, aviso, acordar, depois da captura), `TOCHA_PISCA`, `TOCHA_LEVADA`, `LUZ_PISCA`, `LUZ_MIGRA_ORIGEM` e `LUZ_SEQUENCIA`.

### Comandos de teste novos

- `/sussurros teste sentidos <peso> <vigia> <caca> <neblina> [flags]`: impõe os quatro valores por dois minutos.
- `/sussurros teste efeito piscar|apagao|acordar [ticks]`.
- `/sussurros evento eco_passos|vigia|neblina|cantiga|prenuncio|caca`.

## 0.9.0-alpha2 — itens e a Conta

### A Conta

```
CONTA +2 item=CAIXA total=5 limite=8
CONTA esfriou total=4 limite=8
CONTA aviso=1/3 como=CHAMAS lampioes=2 tochas=1 total=5 limite=8
CONTA aviso=2/3 como=CANTIGA_FALHADA total=6 limite=8
CONTA aviso=3/3 como=ZUMBIDO total=7 limite=8
CONTA estourou total=8 limite=8 cobraEm=74s
CONTA cobranca item=VELA como=NO_PROXIMO_USO total=8
CONTA cobrada na VELA: dura 45s
CONTA cobrada no OLHO
```

- `como` do aviso 2 é `SINO` para quem nunca deu corda na caixa.
- `como` da cobrança: `NO_PROXIMO_USO` (Vela, Olho, Isca, Ossos), `O_SINO_TOCA_SOZINHO`, `DEDILHA_O_FIO`, `A_CAIXA_TOCA_SOZINHA`, `ROMPEU_UMA_LINHA pos=...`, `SEM_LINHA_PERTO->VELA`, `LAMPIOES_FRIOS n=...`.
- `/sussurros teste conta` mostra o total, o limite e os usos por item de quem rodou o comando.

### Caixa de Música

```
CAIXA tocou usos=3 som=CAIXA_MUSICA pos=(x,y,z)
CAIXA ele cantarolou manifestacao=M012 distDaCaixa=14
CAIXA quebrou
```

`som` passa a `CAIXA_GASTA` no 4º uso e a `CAIXA_ARRUINADA` no 7º. Na caçada, a isca aparece na busca como `OUVIU_CAIXA`.

### Linha de Cinza

```
LINHA segurou manifestacao=M031 pos=x, y, z ficou=RISCADA
LINHA testada na porta pos=x, y, z ficou=GASTA
LINHA testada de noite pos=x, y, z ficou=RISCADA pegada=sim
```

### Oferenda

```
OFERENDA posta item=minecraft:bread pos=x, y, z
OFERENDA recusada item=minecraft:bread valor=1.0 repetidas=0 chance=0.60
OFERENDA aceita item=minecraft:bread valor=1.0 repetidas=1 chance=0.36 aceitas=2 seguidas=2 tregua=274s pegadas=3 presente=-
OFERENDA afronta item=sussurros:vela_palida
OFERENDA faltou seguidas=1
OFERENDA faltou: desfeita por 240s
```

`presente` é o que ele deixou na tigela (`-` quando nada). `/sussurros teste oferenda aceitar|recusar` força a decisão sobre a tigela de quem rodou o comando.

### Ossos de Agouro

```
OSSOS desfecho=TREGUA jogadaDoDia=2 pos=(x,y,z)
```

`/sussurros teste ossos <nada|silencio|tregua|apontam|presenca|amigo|conta>` joga com o desfecho escolhido.

### Vela e Chamas Pálidas

```
VELA apagada antes da hora (o bloco saiu do lugar)
CHAMA_PALIDA pos=(x,y,z) duracao=214s ativas=1 cota=2
MIRAGEM fim motivo=CHAMA_PALIDA por=TEMPO pos=(x,y,z)
```

## 0.9.0-alpha3 — o Véu

```
SILENCIO_REAL motivo=VEU duracao=35s mobs=0
VEU abriu duracao=33s luzes=1 porta=sim vidro=sim vulto=sim teste=nao
VEU vulto=sim
VEU vulto=nao_coube
VEU fechou motivo=TEMPO
VEU fechou motivo=VELA
```

- `luzes`, `porta` e `vidro` dizem o que foi trocado por miragem. As miragens aparecem depois como `MIRAGEM fim motivo=VEU` e `motivo=LUZ_APAGADA` só se vencerem pelo tempo; fechadas pelo Véu, não deixam linha.
- `vulto=sim` na abertura quer dizer "vai tentar no meio"; a linha `VEU vulto=` diz se coube.
- `/sussurros evento veu` abre um Véu de teste, sem respeitar o intervalo.

## 0.9.0-alpha4 — a Soleira e o Boneco

```
SOLEIRA erguida pos=x, y, z eixo=0
SOLEIRA atravessada n=1 resposta=GRAVE
SOLEIRA atravessada n=2 resposta=SILENCIO
SOLEIRA atravessada n=0 resposta=VEU
SOLEIRA atravessada n=2 resposta=NAO_ABRIU
SOLEIRA atravessada (descansando hoje)
SOLEIRA desfeita (a porta não está mais lá)
BONECO ANDOU passo=3 dist=29 pos=x, y, z rosto=south
BONECO SUMIU
```

- `eixo=0`: atravessa-se de norte a sul. `n` é a contagem depois desta travessia (volta a 0 quando o Véu abre).
- Noite sem linha `BONECO`: ou já andou, ou alguém olhava, ou não achou lugar válido.
- `/sussurros teste lugar soleira` ergue uma cinco blocos à frente; `/sussurros teste lugar boneco` faz uma noite passar para ele.

## 0.9.0-alpha5 — o Avesso

```
AVESSO levado origem=SONO visita=1 teste=nao
AVESSO chegou visita=1 ancora=x, y, z duracao=58s erros=0
AVESSO vulto=sim
AVESSO saindo motivo=TEMPO
AVESSO voltou motivo=TEMPO ficou=58s visitas=1 residuo=2 pos=x, y, z
```

- `origem`: `SONO` ou `COMANDO`. `motivo` da saída: `TEMPO`, `LONGE`, `CAMA`, `MORTE`, `COMANDO`, `SEM_VISITA` (estava lá sem visita em curso: o jogo fechou com ele dentro).
- `erros` é quantos blocos de parede faltam na cópia. `residuo` é quantas luzes da casa de verdade ficaram apagadas por miragem na volta.
- `/sussurros teste avesso` leva agora, sem contar como visita; `/sussurros teste avesso voltar` traz de volta.
