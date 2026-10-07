# Hotfix 0.5.0-alpha4-fix1

Corrige duas declarações de retorno anulável em tipos internos qualificados que o compilador Java/JSpecify rejeitava.

- `Rastro.Ponto` -> `Rastro.@Nullable Ponto`
- `EstadoJogador.Acao` -> `EstadoJogador.@Nullable Acao`

Nenhuma mecânica, chance, evento, RNG ou balanceamento foi alterado.
