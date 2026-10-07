# Sussurros 0.5.0-alpha2 — Memória Física

Esta build continua a linha da 0.5.0-alpha1: em vez de acrescentar outro "cérebro" invisível, faz a memória do Diretor aparecer na gameplay.

## 1. FLORESTA virou contexto próprio
O contexto não depende do nome do bioma. O Diretor olha a geometria ao redor: céu visível + vários obstáculos verticais próximos significa um lugar bom para ocultar silhuetas.

Em FLORESTA:
- PRESENCA continua importante;
- SEGUIDOR ganha bastante peso;
- passos e sinais ficam mais prováveis;
- porta/tocha praticamente saem da seleção;
- a geometria de PRESENCA prefere ângulos mais laterais/traseiros.

A intenção é diferenciar "campo aberto" de "mata/borda fechada" sem depender de uma lista de biomas.

## 2. SEGUIDOR — o seu caminho faz barulho
Novo evento de fase 2, intensidade 16.

O Diretor escolhe 3–5 pontos reais do Rastro recente e reproduz passos nesses pontos em sequência. O som percorre o caminho que o jogador realmente fez, em vez de nascer em coordenadas aleatórias.

O evento, sozinho, NÃO cria o Hóspede. Ele pode terminar em nada ou, quando uma cadeia fizer sentido, levar depois a outro evento.

Telemetria:
`SEGUIDOR pontos=4 idade=81s->54s dist=31.0->16.2 semCriatura=sim`

## 3. Marcos agora lembram o ponto exato
Lugares já guardava chunks onde um evento forte produziu uma reação forte. Agora marcos novos também persistem a coordenada exata do acontecimento.

Isso é retrocompatível: marcos antigos continuam funcionando, apenas sem a coordenada precisa.

## 4. Cena "Foi aqui"
Ao voltar a um marco persistente, fase 3+, o Diretor pode reconhecer o lugar e montar uma cena curta:

1. espera;
2. produz um som ligado à memória do lugar, preferindo o ponto exato do evento antigo quando ainda faz sentido;
3. tenta colocar o Hóspede perto daquele mesmo ponto, fora da tela e preferencialmente com cobertura;
4. deixa uma única ESPREITA;
5. termina com 70–110 s de silêncio.

A cena é rara: cada marco só recebe uma oportunidade natural por sessão e há cooldown global aproximado de 15 min.

Comando de teste: `/sussurros cena marco`.

## 5. Perfil começa a mudar espaço, não só probabilidade
PRESENCA agora pode alterar seus ângulos conforme o perfil aprendido:
- CAUTELA alta: tenta contornar o hábito de checar as costas, trabalhando mais pelas laterais;
- CONFRONTO alto: favorece ângulos mais traseiros, difíceis de encarar imediatamente;
- FUGA alta: usa uma faixa intermediária, como se tentasse interceptar a rota;
- FLORESTA amplia o uso de ângulos laterais/traseiros.

CAUTELA e FUGA também passam a influenciar o peso de SEGUIDOR.

A intenção não é "punir" o estilo do jogador; é fazer pessoas diferentes receberem geografias de assombração diferentes.

## 6. O corpo reage a ser percebido
A entidade sincroniza um estado visual depois da primeira vez em que entra na tela.

Antes de ser encontrada, OBSERVAR/ESPREITAR deixam a silhueta mais torta. Depois da primeira percepção, o modelo se recompõe discretamente: cabeça e tronco ficam menos inclinados. É visual; não muda a decisão do servidor.

Isso foi feito sobre o renderer/modelo atual, sem GeckoLib.

## 7. Compatibilidade com as cenas existentes
Casa, túnel, campo, marco e AMEACANDO continuam mutuamente exclusivos. O log e `/sussurros memoria` mostram `cenaMarco`/`cenaAtiva` para reconstruir a sessão.

## Teste rápido sugerido
1. `/sussurros esquecer`
2. `/sussurros debug on`
3. caminhe por um tempo e use `/sussurros evento seguidor`
4. `/sussurros fase 3`
5. `/sussurros cena marco`
6. `/sussurros evento presenca`

Depois faça uma sessão natural separada. A cena de marco natural precisa primeiro de um evento forte que marque um lugar e de uma visita futura ao mesmo chunk.

## Fora desta build
Ainda não entram GeckoLib, olhos emissivos, partículas próprias ou um segundo monstro. O objetivo desta alpha é provar memória espacial e perseguição antes de aumentar o custo de render/animação.
