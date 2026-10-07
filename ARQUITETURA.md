# Arquitetura do Sussurros

## Princípio central

O Sussurros é construído como um sistema de horror dirigido por contexto, e não como uma sequência de jumpscares.

```text
                 DIRETOR
                decide QUANDO
                     │
                 intenção
                     ▼
                AMBIENTE /
                 APARIÇÃO
                decide ONDE
                     │
                     ▼
                  HÓSPEDE
                 decide COMO
                     │
             ┌───────┴───────┐
             ▼               ▼
           MUNDO           JOGADOR
             │               │
             └───────┬───────┘
                     ▼
                  MEMÓRIA
                     ▼
                  DIRETOR
```

## Responsabilidades

### Diretor
Administra ritmo, pressão, vulnerabilidade, seleção, aprendizado, memória e contexto.

### Aparição
Resolve posicionamento plausível e reutilizável para manifestações.

### Hóspede
Executa comportamentos da entidade. Não deve receber conhecimento perfeito do jogador quando isso puder ser evitado.

### Atmosfera
Responsável por atividade ambiental que não precisa escalar para presença visual.

### Memória / Perfil / Lugares
Fornecem contexto e histórico para que decisões possam mudar ao longo da sessão.

## Regra de expansão
Uma nova feature deve preferencialmente ser implementada como uma capacidade reutilizável que possa servir a várias situações.

Evitar criar uma classe exclusiva para cada pequena variação de evento.

## Dependências
- Minecraft/Fabric: base do mod.
- GeckoLib: candidato para animação e renderização avançada da entidade quando a necessidade for real.
- SmartBrainLib: candidato apenas se a complexidade da IA do Hóspede justificar.

Nenhuma dependência deve entrar apenas por conveniência.
