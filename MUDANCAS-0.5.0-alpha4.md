# Sussurros 0.5.0-alpha4 — Marcas e Soleiras

A alpha4 continua a expansão da 0.5 sem colocar outro medidor invisível no Diretor. O foco é **Corpo + Mundo + Ferramentas**: deixar a assombração mais física, mais doméstica e mais investigável.

## 1. Vestígios persistentes
O mundo agora guarda uma memória pequena (máximo 18 pontos por jogador) de lugares onde a assombração deixou uma marca real:
- desaparecimentos qualificados do Hóspede;
- Fio de Vigília rompido;
- fim de um evento PEGADAS.

Esses pontos sobrevivem entre sessões e envelhecem. Depois de aproximadamente quatro horas de jogo deixam de ser priorizados pelas ferramentas, para o mapa não ficar dominado para sempre por acontecimentos antigos.

O **Olho Sussurrante** mudou: se não há Hóspede ativo, ele pode apontar para um vestígio próximo e fazer a cinza reaparecer por alguns instantes naquele ponto.

O **Sino Oco** também pode responder a um vestígio. Assim, o jogador começa a investigar a história do próprio mundo, e não apenas a posição atual da criatura.

## 2. Isca Pálida
Novo consumível fabricado com Cinza Pálida, barbante, farinha de osso e carne podre. Cada receita produz duas iscas.

Ao usar, o ponto atual fica marcado por 120 s. A próxima PRESENÇA pode tentar nascer perto dali, ainda respeitando:
- distância do jogador;
- terreno livre;
- tela segura;
- zona da Vela;
- preferência por cobertura.

A Isca **não força** a criatura. Depois de algumas utilizações bem-sucedidas, o Hóspede ganha uma chance crescente de ignorá-la. A ferramenta portanto começa útil e gradualmente deixa de ser uma regra confiável.

A Isca também aparece muito raramente em algumas estruturas antigas.

## 3. PEGADAS — vestígio sem confirmação
Novo evento ambiental de fase 2, intensidade média.

Ele escolhe 4–7 pontos reais do Rastro recente e cria pequenas nuvens de cinza acompanhadas por alguns passos discretos. A sequência pode caminhar em direção ao passado recente ou na direção oposta e termina antes de alcançar o jogador.

Não existe Hóspede garantido. Em jogo normal, o último ponto vira um vestígio persistente. Em evento forçado por comando, nada persistente é salvo.

Isso amplia a regra iniciada por SINAL: **nem toda evidência significa que a criatura está ali agora**.

## 4. Cena “Do outro lado do vidro”
Nova cena doméstica, fase 3+, à noite, depois de o jogador permanecer algum tempo em casa.

O Diretor usa janelas reais encontradas no cache do ambiente. Procura um ponto do lado de fora, fora da tela e com terreno válido, e coloca o Hóspede atrás do vidro sem anúncio.

Se o jogador virar para a janela e permanecer olhando, ele desaparece. Se não perceber, depois de alguns segundos há um toque/estalo discreto no vidro antes de ele ir embora. A cena termina com 55–100 s de silêncio.

A cena possui cooldown longo e bloqueia as outras composições enquanto está acontecendo.

Comando de teste: `/sussurros cena janela`.

## 5. Corpo menos uniforme
Sem GeckoLib ainda, cada manifestação recebe deterministicamente uma de três pequenas variações de silhueta. O ID da própria manifestação decide a variante, portanto nenhum RNG extra do Diretor é consumido.

Algumas PRESENÇAS/ESPREITAS/CAÇAS também podem carregar dois pontos pálidos no rosto. Eles:
- não são emissivos de verdade;
- não aparecem em toda manifestação;
- podem desaparecer depois que o jogador encontra a criatura;
- piscam por regra determinística, não aleatória.

A intenção é criar dúvida visual sem transformar os olhos em um marcador permanente do Hóspede.

## 6. Diário e progressão
O diário cresce de 14 para **18 páginas**. As novas entradas introduzem:
- a Isca Pálida;
- a aparição na janela;
- o fato de o Olho localizar marcas antigas;
- a ideia de que as ferramentas também ensinam o Hóspede sobre o jogador.

O conjunto de verbos fica:
- Vela = afastar;
- Olho = procurar criatura ou marcas;
- Sino = perguntar;
- Fio = confirmar localmente;
- Isca = sugerir um lugar;
- Cinza = coletar/provar/fabricar.

## 7. Consolidação técnica
O wrapper do Gradle que veio nas builds anteriores chamava `gradle-wrapper.jar` com `java -jar`, embora o JAR não possua `Main-Class`. `gradlew` e `gradlew.bat` agora usam `org.gradle.wrapper.GradleWrapperMain` via classpath, como o wrapper espera.

Neste ambiente o wrapper passou dessa falha e tentou baixar o Gradle 9.4.0. A compilação real ainda não pôde ser concluída aqui porque este ambiente não tem acesso de rede aos repositórios e está com JDK 21, enquanto o projeto usa JDK 25.

## Smoke test sugerido
1. `/sussurros esquecer`
2. `/sussurros debug on`
3. `/sussurros fase 3`
4. dentro de uma casa com janela: `/sussurros cena janela`
5. caminhe por 1–2 minutos e force `/sussurros evento pegadas`
6. pegue `Isca Pálida` no criativo, arme-a e depois force PRESENÇA algumas vezes
7. use Olho e Sino perto de locais onde PEGADAS/Fio/Hóspede deixaram vestígios
8. force algumas PRESENÇAS para observar as variações corporais/olhos pálidos.
