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
