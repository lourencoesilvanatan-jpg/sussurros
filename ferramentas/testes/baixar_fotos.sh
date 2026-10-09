#!/usr/bin/env bash
# Baixa as fotos que o teste de cliente tirou no GitHub para uma branch.
#
#   ferramentas/testes/baixar_fotos.sh <branch> [pasta-de-destino]
#
# Espera a execução mais recente da branch terminar, baixa o artefato "fotos-do-jogo" e lista os arquivos.
# As fotos mostram o que o mod faz: são para quem programa, não para o dono do projeto.
set -euo pipefail
REPO="lourencoesilvanatan-jpg/sussurros"
BRANCH="${1:?informe a branch}"
DESTINO="${2:-${TEMP:-/tmp}/sussurros-fotos}"

RUN=$(gh run list --repo "$REPO" --branch "$BRANCH" --limit 1 --json databaseId --jq '.[0].databaseId')
echo "execução: $RUN"
gh run watch "$RUN" --repo "$REPO" --interval 15 >/dev/null 2>&1 || true
gh run view "$RUN" --repo "$REPO" --json jobs --jq '.jobs[] | "\(.name): \(.conclusion)"'
rm -rf "$DESTINO"
mkdir -p "$DESTINO"
gh run download "$RUN" --repo "$REPO" --name fotos-do-jogo --dir "$DESTINO"
find "$DESTINO" -type f | sort
