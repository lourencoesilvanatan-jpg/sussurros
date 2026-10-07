# Sussurros 0.5.0-alpha4-fix2

Hotfix de compilacao sobre a alpha4-fix1.

## Corrigido
`Diretor.sinalFalso(...)` declarava `Vec3 lugar` e `String tipo` sem valor inicial e atribuía ambos dentro de um `switch` statement sobre `ContextoMundo.Tipo`. Mesmo cobrindo todos os valores atuais do enum, o compilador Java nao usa esse tipo de `switch` statement como garantia de definite assignment sem um caminho `default`; por isso a leitura de `lugar` depois do `switch` falhava com `variable lugar might not have been initialized`.

Foi adicionado apenas um `default` defensivo que lança `IllegalStateException` para um contexto desconhecido. Para os cinco contextos atuais (`CASA`, `SUBSOLO`, `ABERTO`, `FLORESTA`, `OUTRO`), o comportamento e RNG permanecem iguais.
