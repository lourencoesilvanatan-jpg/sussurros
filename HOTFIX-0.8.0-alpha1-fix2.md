# HOTFIX 0.8.0-alpha1-fix2

## Correção

Corrigido `HospedeEntity.configurar`: `level` é um campo privado de `Entity` nas mappings atuais e não pode ser acessado diretamente pela subclasse.

A inicialização de `HospedeBusca` agora usa `this.level().getGameTime()`.

Nenhuma mudança de comportamento foi feita.
