# Sussurros — instruções para o Claude Code

Mod de terror para Minecraft 26.2 (Fabric, Java 25). Código, comentários, documentação e commits são em português.

## Comandos

- `./gradlew build` (no Windows, `gradlew.bat build`): compila e roda os testes JUnit.
- `./gradlew runClient`: abre o jogo de teste, sempre com o usuário `Jogador`.

Rode o build antes de cada commit. Não commite se ele falhar.

## O que você não consegue verificar

Você não joga. Build e testes verdes não provam que algo funciona dentro do jogo. Ao entregar, diga isso claramente e deixe os comandos `/sussurros ...` e as linhas de telemetria que o dono do projeto deve conferir. As referências são o `CHECKLIST-TESTES-REFATORACAO.md` e o `TELEMETRIA.md`.

## Fluxo de trabalho

- Uma branch por trabalho e um PR para a `main`. Nunca commite direto na `main`.
- Outra IA também trabalha neste repositório. Não edite uma branch que não foi aberta por você.
- Se o dono estiver com o jogo aberto, não troque de branch nem rode o Gradle na mesma pasta: use um `git worktree`.
- Prefixos de commit: `fix:`, `refactor:`, `test:`, `docs:`, `ci:`, `chore:`.

## Refatoração 0.8.1 (em andamento)

O `Diretor` está sendo dividido em passos. Em cada passo:

- Mover sem reescrever. Não mude números, fórmulas, condições, linhas de log nem a ordem das chamadas ao RNG.
- Correção de bug ou mudança de comportamento vai em commit e PR separados.
- O que ficar para depois de propósito entra no `DIVIDAS-DESIGN.md`.
- Cada passo ganha um `MUDANCAS-0.8.1-refatoracao-passo-N.md` e uma seção no `CHECKLIST-TESTES-REFATORACAO.md`.
- Para mover uma seção inteira do `Diretor`, use `ferramentas/refatoracao/extrair_secao.py` e confira com `ferramentas/refatoracao/verificar_movimento.py`.

## Armadilhas do código

- **Memoria dentro do tick.** `Memoria.de(p)` devolve uma cópia. `Diretor.segundo()` abre uma no começo e salva no fim, então toda função chamada de dentro do tick deve receber esse `m`. Quem abre outra cópia e salva ali perde a alteração.
- **Alterações no mundo.** O que é temporário passa por `AlteracoesTemporarias`, que restaura no tempo certo e ao fechar o mundo. Não remova nem coloque blocos de forma permanente sem uma decisão de design (princípio 5 do `ROADMAP.md`).
- **Testes por comando.** O que é forçado por `/sussurros` (`e.forcando`, `PedidoManifestacao.deComando`) não pode contar para aprendizado, pressão, agenda nem memória.
- **RNG.** A telemetria (`Depuracao.log`) nunca pode consumir números aleatórios: isso mudaria as decisões do Diretor.
- **Spoilers.** `README.md` e `VERSAO-ATUAL.md` não detalham eventos. Os detalhes ficam em `DESIGN-SPOILERS.md` e `TELEMETRIA.md`.

## Onde olhar primeiro

- `ARQUITETURA.md` e `ROADMAP.md`: princípios e plano.
- `src/main/java/com/sussurros/assombracao/Diretor.java`: ritmo, estados, seleção e eventos.
- `Cena*.java`: as cinco cenas compostas. `Atmosfera`: presságios e perturbações. `Aparicao`: escolha de posição.
- `entidade/HospedeEntity` e `entidade/HospedeBusca`: a criatura.
