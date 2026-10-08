# Sussurros — Refatoração 0.8.1 — Passo 3: Cenas

Terceiro passo da divisão estrutural.

## Objetivo

Tirar do `Diretor` as cinco cenas compostas. Cada cena passa a ter a sua classe, no mesmo pacote (`com.sussurros.assombracao`).

## Movido

| Cena | Classe nova | Métodos |
|---|---|---|
| Algo no túnel | `CenaAlgoNoTunel` | `verificarCenaTunel`, `temQuebraRecente`, `sortearQuebraRecente`, `iniciarCenaTunel`, `conduzirCenaTunel`, `presencaNoTunel`, `silencioCenaTunel` |
| Na linha das árvores | `CenaLinhaDasArvores` | `verificarCenaCampo`, `iniciarCenaCampo`, `conduzirCenaCampo`, `invocarCenaCampo`, `silencioCenaCampo` |
| Foi aqui | `CenaFoiAqui` | `verificarCenaMarco`, `iniciarCenaMarco`, `conduzirCenaMarco`, `presencaNoMarco`, `silencioCenaMarco` |
| Do outro lado do vidro | `CenaDoOutroLadoDoVidro` | `verificarCenaJanela`, `iniciarCenaJanela`, `conduzirCenaJanela`, `acharJanelaCena`, `silencioCenaJanela` e o record `JanelaAlvo` |
| Ele voltou com você | `CenaVoltouComVoce` | `verificarVoltaParaCasa`, `iniciarCenaCasa`, `conduzirCenaCasa`, `silencioCenaCasa`, `somNoCaminho`, `portaNaCena`, `presencaNoCaminho` |

O `Diretor` foi de 4.641 para 3.615 linhas.

## Mantido no Diretor

- A ordem do tick: `segundo()` chama os `verificar...` e os `conduzir...` na mesma sequência de antes.
- A sequência de ameaça (`conduzirAmeaca`, `fimAmeaca`), que é um estado do Diretor e não uma cena.
- Os comandos de teste (`testarCenaCasa` etc.), que agora chamam `iniciar...` na classe da cena.
- Os helpers de manifestação, rastro, som, porta e geometria que as cenas usam.

O estado das cenas continua em `EstadoJogador`.

## Regra de preservação

O corpo dos métodos foi recortado e colado por script (`ferramentas/refatoracao/extrair_secao.py`), sem edição. As únicas mudanças de texto são:

- prefixo de classe nas chamadas: `Diretor.x(...)` dentro das cenas e `CenaX.y(...)` dentro do `Diretor`;
- `private` removido de 34 declarações (31 nomes) de helpers do `Diretor`, que passam a ser visíveis só dentro do pacote;
- `private` removido dos métodos de cena que o `Diretor` chama;
- os cinco banners de seção saíram do `Diretor`;
- o import de `ArrayDeque`, que já não era usado no `Diretor`, foi removido.

Não foram alterados números, fórmulas, condições, linhas de log ou a ordem das chamadas aleatórias. Strings e comentários não foram tocados.

## Como foi conferido

- `./gradlew build` e os 11 testes do `Seletor` passaram depois de cada cena movida (um commit por cena).
- `ferramentas/refatoracao/verificar_movimento.py` compara o `Diretor` do commit base com o `Diretor` + as cinco classes novas: os 178 métodos existem uma única vez antes e depois, com o mesmo texto; as únicas linhas que sumiram são os banners, e as únicas que surgiram são o esqueleto das classes novas.
- **Não foi testado em jogo.** Os comandos estão no `CHECKLIST-TESTES-REFATORACAO.md`.

Para repetir a conferência:

```
python ferramentas/refatoracao/verificar_movimento.py . <commit base> CenaAlgoNoTunel CenaLinhaDasArvores CenaFoiAqui CenaDoOutroLadoDoVidro CenaVoltouComVoce
```

## O que este passo não resolve

Registrado no `DIVIDAS-DESIGN.md`:

- as cinco cenas ainda repetem a mesma máquina de estados;
- as classes de cena dependem de helpers que continuam no `Diretor`;
- os nomes dos métodos foram mantidos (`CenaAlgoNoTunel.verificarCenaTunel`);
- `sortearQuebraRecente` é usado também pelo evento `SINAL`.
