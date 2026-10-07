# Sussurros 0.5.0-alpha3 — Vestígios e Contramedidas

Esta build ataca a área mais crua depois das duas primeiras alphas da 0.5: **agência do jogador e conteúdo físico**.
O Diretor já observa, aprende, usa Rastro, casa, marcos e contexto. O jogador, porém, ainda tinha quase só Vela e Olho. A alpha3 começa a transformar a assombração em algo investigável.

## 1. Cinza Pálida — a criatura deixa matéria
Algumas manifestações naturais que foram realmente percebidas podem deixar um item físico no lugar onde o Hóspede desapareceu.

Condições conservadoras:
- nunca vem de Hóspede de comando/teste;
- origem precisa ser o Diretor;
- a aparição precisa ter sido percebida, ou o jogador precisa tê-la confrontado/ferido;
- motivos como `VISTO_DEMAIS`, `ENCARADO_DEMAIS`, `FERIDO`, `CHEGOU_PERTO` e algumas expirações percebidas podem qualificar;
- as primeiras cinzas são mais prováveis; depois ficam raras para não virar farm.

Telemetria:
`VESTIGIO CINZA motivo=VISTO_DEMAIS manifestacao=M014 pos=(...)`

A cinza também pode aparecer raramente em estruturas antigas para que a cadeia de itens não dependa 100% de um drop específico.

## 2. Sino Oco — perguntar sem ter certeza da resposta
Novo item reutilizável, 45 s de recarga.

Ao usar:
1. toca uma assinatura curta do próprio Sussurros;
2. aumenta um pouco inquietação/obsessão — chamar a atenção tem custo;
3. depois de ~1,5–3 s, algo pode responder.

Possíveis respostas:
- se o Hóspede estiver relativamente perto, a resposta pode vir da posição real dele;
- sem criatura, um ponto antigo do Rastro pode responder;
- pode haver silêncio;
- depois de várias utilizações, a assombração passa a ter uma chance crescente de **responder de um ponto do Rastro mesmo com o Hóspede em outro lugar**.

Ou seja: o sino é uma ferramenta, não um radar.

Telemetria:
- `SINO resposta=HOSPEDE ...`
- `SINO resposta=RASTRO ...`
- `SINO resposta=ISCA_RASTRO ...`
- `SINO resposta=SILENCIO`

## 3. Fio de Vigília — uma verdade local
Novo consumível.

Ao usar, o jogador arma por 180 s uma pequena área de raio 6 ao redor do ponto atual.

O fio:
- não é uma zona segura;
- não impede spawn;
- não afasta o Hóspede;
- não revela direção à distância.

Se o Hóspede realmente entrar naquela pequena área, o fio se rompe com um estalo e o jogador recebe a mensagem `O fio se rompeu.`
Se nada atravessar, ele simplesmente expira intacto.

A intenção é criar uma ferramenta complementar ao Sino:
- o Sino alcança longe, mas pode mentir;
- o Fio cobre pouco espaço, mas o rompimento significa que algo realmente cruzou ali.

Telemetria:
- `VIGILIA armada ...`
- `VIGILIA rompeu manifestacao=M...`
- `VIGILIA expirou intacta ...`

## 4. Progressão por exploração
A cadeia de loot foi ampliada:
- Mina Abandonada: pequena chance de Cinza/Fio;
- Mansão: Cinza, Fio e Sino raros;
- Cidade Ancestral: melhor chance de Cinza e pequena chance de Sino;
- Biblioteca de Fortaleza: versões mais raras desses achados.

Isso não substitui o drop da criatura. É uma forma de um mundo antigo parecer ter sido assombrado antes do jogador chegar.

## 5. Diário cresce para 14 páginas
As quatro novas páginas apresentam a Cinza, o Sino e o Fio pela voz do morador anterior.
A última ideia fica mais importante para o design: **o jogador começa a estudar o Hóspede, mas o Hóspede também aprende as ferramentas do jogador**.

## 6. Filosofia desta alpha
Vela, Olho, Sino e Fio formam papéis diferentes:
- **Vela**: segurança temporária;
- **Olho**: informação precisa, com custo e risco;
- **Sino**: informação ambígua e distante;
- **Fio**: confirmação local e limitada;
- **Cinza**: prova física + recurso.

Isso evita transformar item em “botão de vencer o monstro”. O jogador recebe agência, mas continua inseguro.

## Teste curto
1. Use criativo para pegar `Cinza Pálida`, `Sino Oco` e `Fio de Vigília`.
2. Toque o Sino com e sem Hóspede presente.
3. Arme o Fio e force `/sussurros evento presenca` perto da área.
4. Force uma presença, deixe-se perceber e faça ela sumir algumas vezes para validar `VESTIGIO CINZA`.
5. Leia páginas até 14 para validar a sequência do diário.

## Ainda cru / próximo alvo
- animação corporal continua limitada ao modelo vanilla;
- não há olho emissivo próprio;
- o Fio ainda é conceitualmente “estendido” no lugar, sem bloco/mesh físico;
- não existe menu/config externa de ritmo;
- `Diretor.java` continua grande e precisa de refatoração depois que o repertório estabilizar.
