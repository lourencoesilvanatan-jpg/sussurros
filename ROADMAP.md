# Roadmap — Sussurros

## Princípios do projeto

1. Toda nova mecânica precisa reforçar o ciclo **Diretor → mundo → Hóspede → reação → aprendizado**.
2. Preferir **capacidades reutilizáveis** a eventos isolados.
3. Bibliotecas só entram quando resolverem um problema concreto e puderem ser isoladas.
4. O terror deve alternar silêncio, atividade ambiental e picos de ameaça; não queremos spam de aparições.
5. O mundo do jogador não deve ser destruído arbitrariamente. Alterações físicas devem ser limitadas, reversíveis ou claramente justificadas.
6. Telemetria acompanha cada grande mudança para validar comportamento real em jogo.

## 0.8.1 — Ambiente Inteligente
Objetivo: aumentar a sensação de presença entre as manifestações visuais.

- Criar uma camada de **Intenção** para separar o que o Diretor quer provocar de como o mundo executa.
- Expandir as interferências ambientais existentes sem inflar o número de eventos principais.
- Criar famílias de microatividade, interferência e manifestação.
- Aplicar cooldown e orçamento por família para evitar saturação.
- Usar memória espacial e memória do Hóspede para escolher onde uma interferência faz sentido.
- Registrar no debug por que uma interferência foi escolhida e qual contexto a motivou.

## 0.8.2 — Consequências da Presença
Objetivo: fazer o ambiente refletir o comportamento do Hóspede.

- Permitir que uma busca perdida, observação ou passagem deixe consequências ambientais discretas.
- Introduzir alterações temporárias/reversíveis vinculadas a contexto.
- Criar relações entre última posição conhecida, rota provável e interferências.
- Garantir que o sistema possa decidir **não fazer nada**.

## 0.8.3 — Sensores
Objetivo: dar ao Hóspede informação imperfeita sobre o mundo.

- Sensor de movimento.
- Sensor de ruído simples.
- Sensor de linha de visão.
- Sensor de dano.
- Memória de confiança das percepções.

A entidade deve receber sinais, não o estado perfeito do jogador.

## 0.9 — Aparições Cinemáticas
Objetivo: transformar boas posições em boas cenas.

- Sistema de Âncoras (janela, canto, árvore, porta, cobertura, distância etc.).
- Candidatos de posição reutilizáveis.
- Controle de entrada, permanência e saída.
- Maior variedade de poses e comportamento visual.
- Avaliar integração com GeckoLib 5 para animações da entidade e sincronização de efeitos.

## 0.10 — Hóspede Caçador
Objetivo: transformar investigação em caça coerente.

- Busca baseada em última posição conhecida.
- Investigação por ruído.
- Reavaliação de alvo.
- Perda de contato.
- Desistência.
- Estratégias de rota e cobertura.
- Avaliar SmartBrainLib apenas se a máquina de estados própria começar a ficar complexa demais.

## 0.11 — Mundo Reativo
- Mais comportamentos de animais.
- Relação entre iluminação e presença.
- Objetos e portas contextuais.
- Rotas habituais.
- Lugares persistentes.

## 0.12 — Investigação
- Mais ferramentas do jogador.
- Evidências e vestígios.
- Itens com benefício + consequência.
- Progressão investigativa mais conectada ao mundo.

## 0.13+ — Conteúdo
Só aumentar substancialmente a quantidade de eventos depois que as capacidades acima estiverem estáveis.

Priorizar experiências compostas, não uma lista de sustos independentes.

## Métricas de cada versão

### IA
- taxa de decisões válidas;
- taxa de repetição;
- taxa de perda do alvo;
- qualidade de posicionamento;
- variedade de estratégias.

### Horror
- densidade de presença;
- densidade ambiental;
- frequência de manifestações visuais;
- tempo entre picos;
- oportunidade perdida;
- concentração de uma mesma família.

### Engenharia
- tamanho das classes centrais;
- dependências externas;
- custo por tick;
- erros/crashes;
- cobertura de testes e validações.
