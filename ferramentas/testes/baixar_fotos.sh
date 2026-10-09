#!/usr/bin/env bash
# Baixa as fotos que o teste de cliente tirou no GitHub para uma branch.
#
#   ferramentas/testes/baixar_fotos.sh <branch> [pasta-de-destino]
#
# Espera a execução do último commit enviado da branch terminar, baixa o artefato "fotos-do-jogo" e lista
# os arquivos. As fotos mostram o que o mod faz: são para quem programa, não para o dono do projeto.
set -euo pipefail
REPO="lourencoesilvanatan-jpg/sussurros"
BRANCH="${1:?informe a branch}"
DESTINO="${2:-${TEMP:-/tmp}/sussurros-fotos}"

git fetch -q origin "$BRANCH"
SHA=$(git rev-parse "origin/$BRANCH")
RUN=""
# A execução leva alguns segundos para aparecer depois do push.
for _ in $(seq 1 40); do
	RUN=$(gh run list --repo "$REPO" --branch "$BRANCH" --limit 5 --json databaseId,headSha \
		--jq "[.[] | select(.headSha == \"$SHA\")][0].databaseId // empty")
	[ -n "$RUN" ] && break
	sleep 5
done
[ -n "$RUN" ] || { echo "nenhuma execução para o commit $SHA"; exit 1; }
echo "execução: $RUN (commit ${SHA:0:7})"
gh run watch "$RUN" --repo "$REPO" --interval 15 >/dev/null 2>&1 || true
gh run view "$RUN" --repo "$REPO" --json jobs --jq '.jobs[] | "\(.name): \(.conclusion)"'
rm -rf "$DESTINO"
mkdir -p "$DESTINO"
gh run download "$RUN" --repo "$REPO" --name fotos-do-jogo --dir "$DESTINO"
find "$DESTINO" -type f | sort
