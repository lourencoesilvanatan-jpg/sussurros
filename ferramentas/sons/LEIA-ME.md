# Sons e trilha do Sussurros

Todo som novo do mod sai de `gerar_sons.py`: ruído filtrado, ressonâncias e envelopes. Não há gravação nem amostra de terceiros. A semente é fixa, então rodar de novo gera os mesmos arquivos, byte a byte.

Os doze sons antigos (`pano`, `respiracao`, `madeira`, `arrasto`, `estalo`, `grave`) não são gerados aqui e o script não mexe neles.

## Instalar (uma vez)

No Git Bash, na raiz do repositório, com o Python 3.12:

```bash
python -m venv ferramentas/sons/.venv
ferramentas/sons/.venv/Scripts/python -m pip install -r ferramentas/sons/requirements.txt
```

Não precisa de ffmpeg. A pasta `.venv/` fica fora do git.

## Rodar

```bash
# gera os 46 arquivos, atualiza o sounds.json e confere (cerca de um minuto)
ferramentas/sons/.venv/Scripts/python ferramentas/sons/gerar_sons.py

# gera só alguns
ferramentas/sons/.venv/Scripts/python ferramentas/sons/gerar_sons.py --so assobio giz

# só mede o que já está na pasta
ferramentas/sons/.venv/Scripts/python ferramentas/sons/gerar_sons.py --conferir

# imprime a tabela abaixo
ferramentas/sons/.venv/Scripts/python ferramentas/sons/gerar_sons.py --tabela
```

Os arquivos vão para `src/main/resources/assets/sussurros/sounds/`. O `sounds.json` mantém as entradas antigas e ganha (ou refaz) as do catálogo.

## O que a conferência mede

Você não precisa ouvir para saber se algo quebrou. Para cada arquivo decodificado, a tabela mostra duração, canais, pico e RMS (dBFS), componente contínua, centroide do espectro e tamanho. O script sai com código diferente de zero se:

- o pico passar de -3 dBFS, ou uma camada de fundo passar de -23 dBFS de RMS;
- os canais não forem os do catálogo (mono = som com posição; estéreo = sem posição);
- a duração sair mais de 15% da faixa pedida, ou um loop não tiver o número exato de amostras;
- houver componente contínua, clique na primeira ou na última amostra, NaN ou arquivo vazio;
- a emenda de um loop aparecer (ver abaixo);
- o `sounds.json` for inválido ou citar um arquivo que não existe.

Nos loops a emenda é medida de três jeitos: o **salto** do último valor para o primeiro, a **quina** (mudança de inclinação nesse ponto) e o **estalo** (energia acima de 1,5 kHz nos 12 ms em volta da emenda, em dB acima do trecho vizinho mais forte). Os dois primeiros são comparados com o que as amostras vizinhas já fazem de uma para a outra.

## Convenções

- Efeitos avulsos são `nome_1.ogg`, `nome_2.ogg`... Camadas em loop não têm número (`fundo_grave.ogg`).
- Mais de 5 s leva `"stream": true`.
- As três camadas de perseguição têm exatamente 19,2 s (846.720 amostras, 32 tempos a 100 bpm) e começam juntas. O primeiro tempo cai **20 ms depois** do início do arquivo, nas três: assim a emenda fica no respiro antes do tempo forte.
- As outras camadas em loop são giradas para a emenda cair no seu momento mais calmo.
- Cada som novo ainda precisa ser registrado em `ModSons.java` e ganhar a legenda `subtitles.sussurros.<nome>` nos arquivos de `lang/`.

## Tabela de sons

| som | arquivos | canais | duração (s) | o que é |
|---|---|---|---|---|
| `assobio` | `assobio_1.ogg` a `_3.ogg` | mono | 4,3 / 4,6 / 4,6 | alguém assobiando de longe um pedaço do tema (compassos 1-2, 5-6 e 3-4) |
| `cantarolar` | `cantarolar_1.ogg` a `_2.ogg` | mono | 4,8 / 4,9 | o tema cantarolado de boca fechada (uma e duas oitavas abaixo) |
| `caixa_musica` | `caixa_musica_1.ogg` | mono, stream | 20,0 | o tema inteiro, limpo, a 96 bpm, com ritardando no fim |
| `caixa_gasta` | `caixa_gasta_1.ogg` a `_2.ogg` | mono, stream | 20,4 / 27,2 | o tema gasto: 80 e 66 bpm, desafinado, notas falhando; na 2 o mi vira mi bemol |
| `caixa_quebrada` | `caixa_quebrada_1.ogg` | mono | 2,2 | três notas do tema e um tranco metálico |
| `batimento` | `batimento_1.ogg` a `_2.ogg` | mono | 0,8 / 0,8 | um batimento de coração (tum-tum): calmo e disparado |
| `arranhar` | `arranhar_1.ogg` a `_3.ogg` | mono | 1,2 / 0,9 / 1,6 | unhas arranhando madeira (longo; três curtos) e pedra |
| `pancada` | `pancada_1.ogg` a `_2.ogg` | mono | 0,5 / 0,5 | batida pesada e surda numa parede (madeira; pedra) |
| `sussurro_voz` | `sussurro_voz_1.ogg` a `_6.ogg` | mono | 1,5 / 1,4 / 1,4 / 1,9 / 1,8 / 2,4 | frases sussurradas sem sentido, de 3 a 7 sílabas |
| `chamado` | `chamado_1.ogg` a `_2.ogg` | mono | 3,0 / 3,0 | um chamado distante, entre bicho e gente |
| `sino_longe` | `sino_longe_1.ogg` a `_2.ogg` | mono, stream | 6,0 / 5,6 | um sino tocando longe (grande, uma badalada; rachado, duas) |
| `vento_oco` | `vento_oco_1.ogg` a `_3.ogg` | mono, stream | 6,0 / 5,0 / 7,0 | rajada de vento numa fresta, com ressonância oca |
| `zumbido` | `zumbido_1.ogg` | estéreo | 4,0 | zumbido de ouvido, bem baixo |
| `ranger` | `ranger_1.ogg` a `_3.ogg` | mono | 0,9 / 1,9 / 1,3 | madeira rangendo (tábua, porta, peça sob tensão) |
| `sopro` | `sopro_1.ogg` | mono | 0,5 | alguém soprando uma chama |
| `apagao` | `apagao_1.ogg` | estéreo | 2,5 | tudo some: ruído que cresce invertido, corte seco, grave que desce |
| `despertar` | `despertar_1.ogg` | estéreo | 1,5 | acordar de repente: inspiração brusca |
| `tigela` | `tigela_1.ogg` | mono | 0,6 | cerâmica raspando e batendo de leve |
| `giz` | `giz_1.ogg` a `_2.ogg` | mono | 0,6 / 0,6 | giz riscando pedra (um traço; dois traços) |
| `sonho` | `sonho_1.ogg` | estéreo, stream | 8,0 | passagem para um sonho: acorde suave que desafina e afunda |
| `fundo_grave` | `fundo_grave.ogg` | estéreo, stream | 48,0 | loop: drone escuro, quase inaudível |
| `fundo_vigia` | `fundo_vigia.ogg` | estéreo, stream | 32,0 | loop: tons finos de vidro entre 2 e 4 kHz (estar sendo olhado) |
| `caca_pulso` | `caca_pulso.ogg` | estéreo, stream | 19,2 | loop de perseguição, camada 1: pulso grave, 100 bpm, 32 tempos |
| `caca_cordas` | `caca_cordas.ogg` | estéreo, stream | 19,2 | loop de perseguição, camada 2: cordas dissonantes em trêmulo |
| `caca_tema` | `caca_tema.ogg` | estéreo, stream | 19,2 | loop de perseguição, camada 3: pedaços do tema em sinos graves |
| `avesso_ar` | `avesso_ar.ogg` | estéreo, stream | 64,0 | loop: ambiente de um lugar morto, tocado ao contrário |

## O tema

Cantiga de caixinha de música em lá menor, valsa 3/4, oito compassos. Fica em `TEMA` no começo do script e é a mesma em todas as variantes (caixinha, assobio, cantarolar, sinos da perseguição):

| compasso | notas |
|---|---|
| 1 | A4 C5 E5 |
| 2 | D5 C5 B4 |
| 3 | A4 C5 E5 |
| 4 | F5 (mínima) E5 |
| 5 | D5 F5 A5 |
| 6 | G#5 E5 C5 |
| 7 | B4 D5 C5 |
| 8 | A4 (mínima pontuada) |
