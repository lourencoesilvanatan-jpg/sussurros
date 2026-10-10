# Sussurros — truques de Minecraft, variedade da caçada e captura sem morte (SPOILERS)

Data: 10/10/2026. Continua a pesquisa `pesquisa/2026-10-08-cacada.md` (chamada aqui de "pesquisa de 08/10"). Não repete o que está lá; onde achei erro ou buraco nela, está na seção 6.

Só leitura: nenhum arquivo foi criado ou alterado além deste. Nada foi testado no jogo.

**Marcas**

- **[C]** = li código-fonte ou arquivo de dados (o caminho vem junto).
- **[P]** = abri e li a página.
- **[B]** = só apareceu em resultado de busca; a página não foi aberta ou não abriu.
- **(literal)** = texto lido cru (API do Modrinth, API do GitHub, API do rastreador de bugs da Mojang, API do Fandom, arquivo no GitHub). A citação é exata.
- **(via resumo)** = a página passou pelo leitor automático, que resume. Fatos e números são confiáveis; a citação pode ter saído um pouco diferente do original.
- **(inferência)** = dedução minha.

**Sobre o "vanilla 26.2".** Conferi no jar da própria máquina: o `minecraft-common.jar` da 26.2 no cache do Gradle. Para arquivos de dados usei `unzip -p`; para classes, `javap -p -c` (bytecode, sem descompilar). Não existe URL para isso: cito a classe e o método.

---

## 0. Resumo

1. **Barco e carrinho hoje pegam a criatura.** Na cópia em disco do mod, `HospedeEntity` tem 0,7 de largura, estende `PathfinderMob` e não sobrescreve `canRide`. Pela regra do vanilla 26.2, um barco parado no caminho dela a recolhe, e um carrinho em movimento também. Não testei no jogo.
2. **A resposta padrão custa uma linha.** Warden, Wither e Dragão devolvem `false` em `canRide`. Cave Dweller, Man From The Fog Reimagined e os chefes do Cataclysm fazem o mesmo. A tag `cannot_be_pushed_onto_boats` só protege contra barco; carrinho não consulta tag nenhuma.
3. **A Mojang não fecha todo truque.** Fechou o barco do Warden porque a IA dele parava de funcionar montada. Recusou como "inválido" o relato de que o Creaking é fácil de prender. Alguns mods transformam o truque em conteúdo (conquista "Dinosaur Train" do Alex's Caves; o Grottol do Mowzie's Mobs, que foge de carrinho).
4. **Repetição: o que as fontes mostram é habituação, com pouca relação com o número de variantes.** Jogadores falam em "primeira hora" (Bunker), "primeiras horas" e "20+ horas" (Phasmophobia, que tem mais de vinte tipos de fantasma). O que devolve frescor nas fontes: aparecer em lugar diferente, mudar algo no cenário depois de uma falha, comportamento raro visto tarde, e aparecer pouco.
5. **Variedade tem custo.** Quando a regra muda sem aviso, o jogador lê como defeito ou trapaça ("unexplained alerts", "suddenly easily detectable", "brokenly powerful").
6. **Pegar sem matar: o precedente mais próximo é o GoatMan 3.0**, que agarra, guarda os itens perto do lugar, larga o jogador em outro ponto com meio coração e piora a cada vez, com o nível caindo depois de alguns dias. Não achei recepção dele. No mod Weeping Angels (pegar = teleportar) as reclamações são sobre ficar preso sem volta e perder item para sempre.
7. **Não achei medida de tensão na terceira ou na décima captura** em nenhuma fonte. O que existe é indireto (seção 3).

---

## 1. Tópico 1 — truques de Minecraft contra quem persegue

### 1.1 O que as fontes dizem

#### Barco e carrinho no vanilla 26.2

- **Regra do barco** [C] `net.minecraft.world.entity.vehicle.boat.AbstractBoat`, método `tick`: o barco só recolhe quando roda no servidor **e não tem jogador no comando** (`!(getControllingPassenger() instanceof Player)`). Para cada entidade encostada: há vaga, ela não está montada em nada, `hasEnoughSpaceFor` (largura dela menor que a do barco), é `LivingEntity` e **não está na tag `CANNOT_BE_PUSHED_ONTO_BOATS`**. Aí chama `entity.startRiding(barco)`. Senão, só empurra.
- **A tag** [C] `data/minecraft/tags/entity_type/cannot_be_pushed_onto_boats.json`: `player`, `elder_guardian`, `cod`, `pufferfish`, `salmon`, `tropical_fish`, `dolphin`, `squid`, `glow_squid`, `tadpole`, `creaking`, `nautilus`, `zombie_nautilus`, `sulfur_cube`.
- **Quem recusa montar** [C]: varri as 716 classes de `net.minecraft.world.entity`. Só três sobrescrevem `canRide` e as três devolvem `false`: `Warden`, `WitherBoss`, `EnderDragon`. O padrão em `Entity.canRide` é "não está agachado e `boardingCooldown <= 0`".
- **Regra do carrinho** [C] `vehicle.minecart.NewMinecartBehavior.pickupEntities` e `OldMinecartBehavior.pushAndPickupEntities`: carrinho montável, com velocidade horizontal ao quadrado acima de 0,01, recolhe qualquer entidade que não seja `Player`, `IronGolem` ou outro carrinho. **Não consulta tag e não confere largura.** Só `canRide` barra.
- A wiki concorda: "A mob can ride a boat if it is narrower than a boat, and is neither an aquatic nor ambient animal"; "Mobs cannot exit a boat and is trapped until the boat gets destroyed." [P, via resumo] https://minecraft.wiki/w/Boat · carrinho: não pega "Ender dragons, wardens, and withers", nem morcego e golem de ferro no Java; "Even large mobs like ghasts can enter minecarts" [P, via resumo] https://minecraft.wiki/w/Minecart
- Aranha, Ravager, golem de ferro e Ghast ficam fora do barco só por largura (lista da wiki, mesma página). O número 1,375 para a largura do barco é de memória; não conferi no jar.

#### Por que a Mojang excluiu cada um

- **Warden.** Relato MC-250259, "Warden AI doesn't function properly when it rides on entities": montado, ele "Can't use close range attacks", "Can't use sonic boom attacks", não ruge e não fareja. Resolvido como *Fixed* na 1.19 Pre-release 2. [P, literal] https://bugs.mojang.com/browse/MC-250259 — o motivo registrado foi a IA quebrar, não "anti-truque" declarado. O relato não descreve a correção; que ela tenha sido o `canRide` falso é inferência minha, a partir do jar. Ainda aberto: MC-252695, "Warden shoot backwards when on boats" [P, literal, só o título].
- **Wither.** MC-8487 (2013): dentro do carrinho ele não regenerava vida e ficava "really easy to defeat". *Fixed* na 13w05a. [P, literal] https://bugs.mojang.com/browse/MC-8487
- **Creaking.** Está na tag do barco, mas a Mojang aceitou o resto:
  - MC-277225, "The player can easily trap the creaking" ("especially when we dig beneath it, making it the easiest trap"): resolvido como **Invalid**. [P, literal] https://bugs.mojang.com/browse/MC-277225
  - MC-277107, "Creakings can be pushed by minecarts": **Works As Intended**. [P, literal] https://bugs.mojang.com/browse/MC-277107
  - MC-277183 (empurrado por entidades quando ninguém olha) e MC-277068 (empurrado por líquido): **Works As Intended**. [P, literal]
  - A wiki confirma: "Creakings cannot enter minecarts [Bedrock Edition only] or boats"; pode ser movido por "water, lava, explosions, pistons, falling blocks, wind charges". [P, via resumo] https://minecraft.wiki/w/Creaking
- Não achei declaração da Mojang explicando a tag do Creaking nem a do Dragão. Fica como lacuna.

#### Outras defesas que o vanilla dá a certos mobs [C, mesma varredura]

| O que | Quem sobrescreve |
|---|---|
| `makeStuckInBlock` (teia não segura) | `Spider`, `WitherBoss` |
| `isPushable` | `Warden` (não é empurrado enquanto cava ou emerge), `Creaking` |
| `canUsePortal` | `EnderDragon`, `WitherBoss`, `Creaking` |
| `ignoreExplosion` | `Warden` |
| `isPushedByFluid` | `Drowned` |
| `isSensitiveToWater` (água fere) | `Blaze`, `EnderMan`, `Strider` |

#### Como os mods tratam barco e carrinho

| Mod (versão lida) | O que faz | Fonte |
|---|---|---|
| **Cave Dweller** (ramo `1.19.2`, SiverDX) | `canRide` e `startRiding` devolvem `false`, a não ser que `allow_riding` esteja ligado. Comentário da opção: "Allow the Cave Dweller to follow vanilla riding logic (e.g. boats)"; padrão `false` | [C] https://github.com/SiverDX/cave_dweller/blob/1.19.2/src/main/java/de/cadentem/cave_dweller/entities/CaveDwellerEntity.java (linhas 165–181) e `config/ServerConfig.java` linha 112 |
| **Man From The Fog Reimagined** (`master`, Fabric) | `canStartRiding` devolve `false`; `canUsePortals` também | [C] https://github.com/zenolth/man-from-the-fog-reimagined/blob/master/src/main/java/dev/zenolth/the_fog/common/entity/the_man/TheManEntity.java (linhas 526–533) |
| **The Midnight Lurker** (Forge; repositório do autor) | Deixa montar e desmonta no tick seguinte: `if (entity.isPassenger()) { entity.stopRiding(); }`. Página: "(Version 1.1.9 and up: If the Lurker gets trapped in a boat/minecart he will immediately exit the boat/minecart)". E: "The Lurker will swim somewhat faster if the player is in a boat." | [C] https://github.com/Voxla/midnightlurker/blob/master/src/main/java/net/mcreator/midnightlurker/procedures/MidnightLurkerAggressiveOnEntityTickUpdateProcedure.java (linhas 59–60) · [P, literal] https://modrinth.com/mod/the-midnight-lurker |
| **L_Ender's Cataclysm** (`new1.20.1`) | As duas classes-base de chefe devolvem `false` em `canRide` e em `canChangeDimensions` | [C] https://github.com/lender544/new1.20.1/blob/HEAD/src/main/java/com/github/L_Ender/cataclysm/entity/InternalAnimationMonster/IABossMonsters/IABoss_monster.java (linhas 286–304) e `.../AnimationMonster/BossMonsters/LLibrary_Boss_Monster.java` (linhas 224–243) |
| **Weeping Angels** (ramo `mc/1.20.1`, Jeryn99) | Não achei `canRide` nem tratamento de barco nas classes do anjo. Tem imunidade a teia (`makeStuckInBlock` ignora `COBWEB`), `knockback` vazio e resistência 1,0 | [C] https://github.com/Jeryn99/Weeping-Angels/blob/mc/1.20.1/common/src/main/java/dev/jeryn/angels/common/entity/angel/AbstractWeepingAngel.java (linhas 139–143 e 273–275) |
| **Alex's Caves** | Faz o contrário: há uma conquista por pôr dinossauros em carrinhos. "Dinosaur Train": "Put five different types of dinosaur in minecarts near each other" | [C] https://github.com/AlexModGuy/AlexsCaves/blob/HEAD/src/main/java/com/github/alexmodguy/alexscaves/server/entity/living/DinosaurEntity.java (linhas 333–346) e `assets/alexscaves/lang/en_us.json` |
| **Mowzie's Mobs** (repositório público, `main`) | O Grottol **procura** carrinho sobre trilho para fugir (`EntityAIGrottolFindMinecart`), o carrinho é acelerado e ele é ejetado quando para ou sai do trilho. É mecânica do bicho, não falha. Na busca de código só `EntityFrozenController` sobrescreve `canRide` | [C] https://github.com/BobMowzie/MowziesMobs-Public/blob/main/src/main/java/com/bobmowzie/mowziesmobs/server/entity/grottol/EntityGrottol.java (linhas 288–306) e `server/ai/EntityAIGrottolFindMinecart.java` |
| **SCP: Lockdown** | Na busca de código, só a cadeira (`ChairEntity`) mexe em `canRide`. Nenhum SCP recusa montar | [C, só busca] https://github.com/ConnorTron110/SCPLockdown |
| **Enhanced AI** (ramo `1.21.1`; 646 mil downloads) | Módulo "Anti-Cheese": por padrão os monstros **quebram** o veículo (`breakVehicles = true`); opção para proibir de montar (`preventRidingVehicles = false`), tudo por tags | [C] https://github.com/Insane96/EnhancedAI/blob/1.21.1/src/main/java/insane96mcp/enhancedai/module/mobs/anticheese/VehicleAntiCheese.java · [P, literal] https://modrinth.com/mod/enhanced-ai |
| **No Boat Cheesing** (Fabric, lista a 26.2) | Cancela a montaria se o mob tem meta ativa de atacar, fugir ou ser atraído. Página: "mobs will leave their vehicle if they are targeting something, fleeing from danger, or tempted by food" | [C] https://github.com/roland-a/no-boat-cheesing/blob/HEAD/src/main/java/roland_a/mc_mods/no_boat_cheesing/core/helper/ShouldCancelAddPassenger.kt · [P, literal] https://modrinth.com/mod/no-boat-cheesing |
| **Monster Boat AI** (plugin Paper, 1.21.x) | Chance por mob de entrar, sair ao levar dano ou quebrar o barco. "Boats are no longer free mob prisons." | [P, literal] https://modrinth.com/mod/monster-boat-ai |
| **GoatMan, Born in Chaos, The Knocker, The One Who Watches** | Código fechado. As páginas não falam de barco nem de carrinho | [P, literal] https://modrinth.com/mod/goatman · https://modrinth.com/mod/borninchaos · https://modrinth.com/mod/the-knocker · https://modrinth.com/mod/the-one-who-watches |
| **Scape and Run: Parasites**, **Mimic Dweller** | Não achei nada sobre barco | lacuna |

#### Os outros truques

**Subir em pilar**
- Warden, estrondo sônico. Conferido no jar [C] `ai.behavior.warden.SonicBoom` e `Warden`: o estrondo fica bloqueado por **200 ticks (10 s)** depois que ele fixa o alvo e por **40 ticks** depois de cada golpe ou estrondo; alcance `DISTANCE_XZ = 15`, `DISTANCE_Y = 20`; som de carga aos 34 ticks (1,7 s). A wiki descreve como 10 s, 5 s e 14 blocos. [P, via resumo] https://minecraft.wiki/w/Warden
- Man From The Fog: nó de caminho "encostado na parede" e escalada a 0,7 [C] mesmo arquivo, `climbTick` (linhas 1436–1460). A escalada antiga por pilar está comentada no código.
- Midnight Lurker: "All Lurkers who walk or run will now be able to climb to get to the player." [P, literal]
- GoatMan: "Leap: Jumps when player is too far or high; destroying blocks". [P, literal]
- Enhanced AI: mobs sobem escadas de mão e trepadeiras. [P, literal]

**Cavar e se fechar**
- Vex: "can freely pass through any block, including water and lava [...] and bedrock"; os invocados pelo Evoker começam a tomar dano "after 30 to 119 seconds". [P, via resumo] https://minecraft.wiki/w/Vex — atravessa, mas tem prazo de vida.
- Midnight Lurker "Aggressive": "can duck and crawl into spaces [...] break glass and doors, and if underground, tunnel towards the player if there is space for it". [P, literal] No código, `world.destroyBlock(...)` em três alturas [C] mesmo arquivo, linhas 73–75. Relato #22: "ML breaking unbreakable blocks". [P, literal] https://github.com/Voxla/midnightlurker/issues/22
- "Reimagined cave dweller" (Valk & Naz, Forge 1.20.1): "It doesn't care about walls. It will break through blocks to reach you. [...] Your best bet is to get out of the caves entirely." [P, literal] https://modrinth.com/mod/reimagined-cave-dweller
- Enhanced AI "Miners": "Mobs are able to mine their way to the target. By default only zombies can". [P, literal]
- Enhanced AI "Teleport anti-cheese" faz o inverso: puxa **o jogador**. Depois de 50 ticks sem alcançar o alvo, acorda quem dorme, desmonta e teleporta o alvo para até 4 blocos do mob, com som de fruta do coro. Descrição: "Prevent players from abusing water or 2 high gaps to bully endermen." [C] https://github.com/Insane96/EnhancedAI/blob/1.21.1/src/main/java/insane96mcp/enhancedai/module/mobs/teleportanticheese/TeleportAntiCheeseGoal.java — o som toca depois do teleporte; não há aviso antes.

**Água e lava**
- Enderman: regra de fraqueza. "Endermen teleport erratically, as well as away from dangers such as lava, projectiles, and water"; "An enderman cannot teleport while it is in a minecart or boat"; precisa de três blocos livres no destino. [P, via resumo] https://minecraft.wiki/w/Enderman
- Warden: "immune to fire and lava"; "now slow down in water and are affected by bubble columns again" (22w14a). [P, via resumo]
- Man From The Fog: `canBreatheInWater` e `isFireImmune` verdadeiros, `setCanSwim(true)`, e sobe 0,5 por tick quando submerso com o alvo acima. [C] linhas 277, 541–548, 1513–1516
- Cave Dweller: mixin que dá a ele "depth strider" de 1,5 (`depth_strider_bonus`). [C] https://github.com/SiverDX/cave_dweller/blob/1.19.2/src/main/java/de/cadentem/cave_dweller/mixin/MixinLivingEntity.java
- Zumbi: 30 s com a cabeça na água começa a virar Afogado. [P, via resumo] https://minecraft.wiki/w/Zombie
- O blog da CurseForge recomenda a lava contra o Cave Dweller: "the most effective way to kill it would probably be to trap it inside lava which will slow them down". [P, via resumo] https://blog.curseforge.com/cave-dweller-minecraft-mod-frequently-asked-questions/ (6/4/2026)

**Portas e alçapões**
- Zumbi: "Up to 10% of zombies [...] can pathfind through closed wooden and copper doors, and on Hard difficulty can succeed in breaking them down"; "approximately 10 seconds". [P, via resumo]
- Midnight Lurker: quebra porta "unless the player's build uses iron doors". [P, literal]
- Weeping Angels: `OpenDoorGoal` (abre, não quebra). [C] `WeepingAngel.java` linhas 79 e 233. Relato #378: não usa a porta se houver bloco em cima dela. [P, literal, só o título]
- "The one who Slashes": "Iron doors also dont protect you because he has a AI that adapts and learns from you." [P, literal] https://modrinth.com/mod/the-one-who-slashes — alegação do autor; não verifiquei.
- Breeze: a rajada mexe em blocos, o que a regra `mobGriefing` controla; a lista de blocos não saiu literal. [P, via resumo] https://minecraft.wiki/w/Breeze
- Cave Dweller (3 s) e Man From The Fog (portas e alçapões): já na pesquisa de 08/10.

**Vão de 1 bloco**
- Cave Dweller e Man From The Fog trocam a caixa de colisão (pesquisa de 08/10). Os relatos mostram o custo: #16 "It can't chase in 2 blocks high paths and can't crawl through one block also. its stuck"; #11 "the dwellers can't go through a 1x1 hole if it's in the middle of a 3x3". O autor corrigiu em duas versões de teste e acrescentou: "also it no longer avoids rails for pathfinding". [P, literal] https://github.com/SiverDX/cave_dweller/issues/16 · https://github.com/SiverDX/cave_dweller/issues/11

**Teia**
- Cave Dweller #20, aberto desde 11/2/2024, sem resposta: "it gets stuck pretty easily in cobwebs". [P, literal] https://github.com/SiverDX/cave_dweller/issues/20
- Quem resolveu: Aranha e Wither no vanilla; Weeping Angels (acima).

**Cama**
- Vanilla [C] `ServerPlayer` (checagem da cama) e `Monster.isPreventingPlayerRest`: procura instâncias de **`Monster`** numa caixa de 8 × 5 × 8 e devolve `NOT_SAFE`. A wiki: "within 8 blocks of the bed head horizontally [...] and 5 blocks vertically"; "You may not rest now; there are monsters nearby". [P, via resumo] https://minecraft.wiki/w/Bed
- Phantom pune quem não dorme: "Time Since Last Rest" de 72000 ticks (3 dias de jogo), sem bloco opaco por cima. [P, via resumo] https://minecraft.wiki/w/Phantom
- Man From The Fog acorda quem dorme na perseguição [C] `ChaseState.java` linhas 143–144. Enhanced AI também (acima).
- Midnight Lurker usa a cama como alívio: "When the player sleeps through the night insanity will stop progressing until the next neutral Lurker spawns". [P, literal]

**Pérola do Ender, elytra, distância**
- Man From The Fog: `CHASE_DISTANCE = 200`, `TOO_FAR_AWAY_TICKS = 2400`, depois `teleportBehindTarget`. [C] linhas 87, 95, 1158–1164
- Enhanced AI "Pearlers": "Mobs with ender pearls will be able to use them to reach the target." [P, literal]
- Cataclysm: quem bate de longe perde dano. Acima de `RangeLimit` o dano cai em linha reta até zero em 1,5 vez o limite; há teto de dano por janela; sem alvo o chefe se cura e volta ao ponto de origem depois de `return_home` segundos (padrão 20). [C] `IABoss_monster.java` linhas 126–192 e 214–240; `config/CommonConfig.java` linhas 54–85 e 162–164
- Não achei no vanilla resposta própria a elytra ou pérola contra quem foge.

**Deslogar**
- Só achei plugins de PvP que matam quem sai em combate (CombatLog+, Combat Log Preventer). [B]
- Não achei mod de terror de Minecraft com regra para isso. O "STOP HIDING" do SCP está na pesquisa de 08/10.

#### Relatos de jogadores e o que o autor fez

- **Enhanced AI #12** (2021): "It doesn't matter how dangerous the creeper or zombie is if it gets incapacitated by a wall of boats. Even an illager raid [...] are no match for falling into a boat and remaining there politely until their deaths." O autor fez primeiro o bloqueio ("Method 1 was much much easier", 2022) e depois o "quebrar o veículo" ("Well, it took me just two years", 2023). [P, literal] https://github.com/Insane96/EnhancedAI/issues/12
- **Cave Dweller #3**: o Enhanced AI apagava as metas de alvo do Cave Dweller e ele "only flees; it acts just like a passive mob". Resposta do autor: lista negra na configuração do outro mod. [P, literal] https://github.com/SiverDX/cave_dweller/issues/3 — mod de "IA melhor" de terceiros pode quebrar a criatura.
- **Midnight Lurker #26**: o Lurker nasceu dentro de uma fazenda de mobs, morreu na queda sobre espeleotema e derrubou o servidor. O autor disse ter corrigido na 3.3.3; o jogador respondeu que continuava e atribuiu a outros mods do pacote. [P, literal] https://github.com/Voxla/midnightlurker/issues/26
- **Mojang**: os relatos do Creaking acima.

#### Reclamações de "trapaceia" ou "dá para burlar" em mods de terror de Minecraft

- "I can't hit him or do anything to him. When aggressive mode appears, it hits us until it disappears for a few minutes and it is very annoying not to be able to hit it." (Midnight Lurker #14; era a opção `lurker_invulnerable`) [P, literal] https://github.com/Voxla/midnightlurker/issues/14
- "anytime I kill the Midnight lurker, he spawns again and again and again. There was no cool down." (#7) [P, literal] https://github.com/Voxla/midnightlurker/issues/7
- "the ambience playing randomly in the middle of a peaceful farming session in a fortified base ruins the appeal" (#4; o autor mudou para só tocar onde o Lurker pode nascer) [P, literal] https://github.com/Voxla/midnightlurker/issues/4
- "it makes you practically invincible if oyu use something that removes hunger" (Man From The Fog #11, sobre a fome travada na perseguição) [P, literal] https://github.com/zenolth/man-from-the-fog-reimagined/issues/11
- "makes them unplayable since some dimension mods basically leave you stuck in that dimension with no way out, also should be an option to turn off the item deletion" (Weeping Angels #343) [P, literal] https://github.com/Jeryn99/Weeping-Angels/issues/343
- "If that one takes your pickaxe there's no way to ever get it back." (Weeping Angels #426) [P, literal] https://github.com/Jeryn99/Weeping-Angels/issues/426
- A página do próprio Midnight Lurker admite contramedidas a truque: escudo entra em recarga de 2,5 s "so the player can't shield spam to kill the Lurker"; o dano "scales with the player's armor, so they are never safe, even when overpowered". [P, literal]
- As citações do Reddit sobre Cave Dweller ("our roomate", "more annoying than scary") já estão na pesquisa de 08/10.

### 1.2 Inferências

- O truque do barco tem **três famílias de resposta**: recusar montar (vanilla, Cave Dweller, Man From The Fog, Cataclysm), montar e sair (Midnight Lurker) e quebrar o veículo (Enhanced AI). Para um mod que nunca estraga o que o jogador fez, só a primeira serve: quebrar o barco destrói um item do jogador, e "montar e sair" aparece na tela como tranco.
- Recusar montar é **silencioso**: o truque simplesmente não acontece. Nenhuma fonte reclama disso. As reclamações que achei são de respostas que tiram algo do jogador (item, saída, capacidade de reagir).
- A Mojang trata prisão como problema **quando ela decide a luta**. O Creaking pode ser preso num buraco porque prender não o vence (ele é invulnerável e volta ao coração). O Warden não pode ir para o barco porque ali ele para de atacar.
- Há mods que tratam o truque como parte do jogo. Isso é evidência de que "resistir a tudo" não é o único desenho aceito.
- Teia, vão estreito e pathfinding são onde os mods de perseguição mais falham na prática (Cave Dweller #11, #16, #20). Falha de caminho vira "ele ficou preso", que o jogador lê como burla fácil.

### 1.3 Lacunas

- Comentários da CurseForge não foram lidos (a página só devolveu o cabeçalho). O Modrinth não tem comentários. Faltam, portanto, relatos de jogadores que prenderam Cave Dweller, Goatman, Knocker ou One Who Watches num barco.
- Reddit: o leitor respondeu "unable to fetch from www.reddit.com". Não contornei.
- Mods fechados (Born in Chaos, GoatMan, The Knocker, The One Who Watches, SRP): só li a página.
- Twilight Forest e parte do Mowzie's Mobs não estão no índice de busca de código do GitHub; não li classe por classe.
- Não achei regra de logout em mod de terror, nem recepção do "quebrar o barco" do Enhanced AI.

---

## 2. Tópico 2 — repetição e variedade

### 2.1 O que as fontes dizem

#### O que os designers dizem

- **Gary Napper (Alien: Isolation), 2014:** "We couldn't make an enemy that was scripted...You're going to die a lot, which means restarting a lot, and if the alien was scripted, you'd see the same behaviour. That makes the alien become predicatable, and a lot less scary." E: "It learns, but that's not quite true. It has a set of behavioural designs that unlock as encounters occur." [P, via resumo] https://www.mcvuk.com/development-news/18-things-we-learned-about-alien-isolation-last-night/
- **Alistair Hope (Alien: Isolation):** "The joy for us is that you die, start again and it's like playing for the first time: you can't predict anything." [P, via resumo] https://kotaku.com/alien-isolations-worst-aspect-the-unpredictable-alien-1607096520
- "As soon as the player can predict a pattern in the Alien, it stops being scary." (Napper) [B] techradar; a página não abriu.
- **Thomas Grip (Frictional), 2014:** depois da primeira morte a tensão passa a depender de "will I have to replay the mission?", "a lot less potent from a horror perspective". [P, via resumo] https://www.dsogaming.com/news/frictional-games-dev-on-alien-isolation-horror-game-suffers-from-two-major-horror-design-issues · sobre quem morre no vídeo de demonstração: "They're laughing, and feeling relief." [P, via resumo] https://kotaku.com/there-are-things-worse-than-death-in-video-games-1573207459
- **Thomas Grip, 2020:** "Repetition and frustration took players out of their immersion, replacing fantasies of real monsters with abstract notions of the gameplay system." Em The Dark Descent: "they were teleported back a bit, and something changed in the environment." [P, via resumo] https://blog.playstation.com/?p=341757
- **Thomas Grip sobre SOMA:** regras "strict—yet still fuzzy" fazem o jogador projetar inteligência; e "it's time to leave this 'wait-and-run' behavior a bit behind". [P, via resumo] https://www.gamedeveloper.com/design/the-tricky-design-problem-of-i-soma-i-s-memorable-monsters
- **Fredrik Olsson (Amnesia: The Bunker), 2024:** "It's very rare that it pops up in the same place"; "if the stalker had been scripted, that would also take away"; "If you replay The Bunker again – you play it for the challenge". [P, via resumo] https://www.superjumpmagazine.com/the-making-of-amnesia-the-bunker — recursos, armadilhas e códigos mudam de lugar a cada partida. [B] gamingbolt

#### Um perseguidor, vários jeitos de caçar

- **Phasmophobia.** Além dos casos da pesquisa de 08/10, a página de caçada lista regras por fantasma: Demon espera 20 s entre caçadas em vez de 25; Banshee "cannot detect nor kill non-targets" enquanto o alvo está na área; Yokai só ouve voz e eletrônico a 2,5 m (em vez de 9 e 7,5); Onryo não caça com fogo aceso a 4 m, mas apaga a chama; Dayan anda a 2,25 m/s se o jogador se move a até 10 m e a 1,2 m/s se ele fica parado; Obake troca de modelo por um instante; incenso cega o Moroi por 7 s. [P, literal] https://phasmophobia.fandom.com/wiki/Hunt
- **Alien: Isolation.** Ramos que destravam com os encontros (acima e pesquisa de 08/10).
- **Monstrum.** Um de três monstros por partida. Brute: o mais rápido, visão forte, "telegraphs his presence with heavy footsteps and an ominous growl". Hunter: "prefers to wait", sai de dutos. Fiend: "produces no footsteps, preferring to hover", denunciado por luz piscando. O crítico pôs como ponto a favor "The procedural nature gives each run a different feel" e deu 6/10 ("a small package"). [P, via resumo] https://www.playstationcountry.com/?p=15766
- **Nemesis no remake de RE3.** "you get to a point when you can feel the Nemesis is coming"; "rarely as scary or interesting as Mr. X". [P, via resumo] https://www.denofgeek.com/games/resident-evil-3-review/ — as formas diferentes dele não salvaram a recepção, porque as aparições são marcadas.
- **Lethal Company.** A variedade vem do elenco: cada entidade tem uma regra, e as regras se sobrepõem. [B] unwinnable.com (a página devolveu 403)
- **Hello Neighbor.** IA que põe armadilha "in places you often visit". O crítico chama o vizinho de "brokenly powerful" e o jogo de "annoying and flat-out broken" (3,75/10). [P, via resumo] https://gameinformer.com/games/hello_neighbor/b/pc/archive/2017/12/12/miserable-stealth-action

#### Mods de Minecraft

- **Cave Dweller** [C] `entities/goals/`: ao ser visto, sorteia entre `CHASE, STARE, STARE, FLEE` (25% persegue, 50% encara, 25% foge) em `CaveDwellerTargetSeesMeGoal.java` linha 39. Encarando, depois de um número de olhadas, a cada tick há 10% de sumir se ninguém olha e 30% de sortear `CHASE` ou `FLEE` (`CaveDwellerStareGoal.java` linhas 73–80). A menos de 12 blocos é sempre `CHASE` (`CaveDwellerTargetTooCloseGoal.java` linha 36). https://github.com/SiverDX/cave_dweller/tree/1.19.2/src/main/java/de/cadentem/cave_dweller/entities/goals
- **Man From The Fog Reimagined** [C]: cinco estados (`STARE`, `STALK`, `CHASE`, `FLEE`, `PREPARE_LUNGE`), alucinações só durante a perseguição, e **adaptação a pancada**: a chance de bloquear começa em 0,25, sobe 0,1 por golpe (mais se ele já foi morto antes), vai até 0,9 e cai 0,01 por tick. Não toma dano se houver bloco ou entidade entre ele e quem bate. `TheManEntity.java` linhas 93–94, 768–790, 1288–1294; `state_machine/states/TheManState.java`
- **The Midnight Lurker** [P, literal]: variantes por lugar e por situação. "Shadow" em lugares escuros como cavernas; "Shapeshifter" aparece em vilas e finge ser aldeão; variante do Nether; "Hider" se enterra quando olhado; "Creep" teleporta para perto em vez de para longe; "Aggressive" só no estágio 7 de insanidade, e cava se estiver no subsolo; "Unprovoked": "will become aggressive for unknown reasons". Entidades invisíveis fazem "Lurker running sounds as a sort of fake out". https://modrinth.com/mod/the-midnight-lurker
- **GoatMan 3.0** [P, literal]: estados Stroll, Creep, Stare, Chase, Flee, Vanish; reage ao entorno: "actively avoids and flees during chase when wolves, villagers, and iron golems are nearby"; ataca aldeão isolado; presságio com cabras ("Watched", "Followed"). https://modrinth.com/mod/goatman
- **The One Who Watches** [P, literal]: "a stalker who CAN kill you but doesn't HAVE to kill you"; "He may not show up every night". https://modrinth.com/mod/the-one-who-watches
- **From The Fog** [P, via resumo]: observa "from a distance, behind trees, at the foot of your bed, or even through windows"; não pode ser morto. Também "burn your base once you leave". https://blog.curseforge.com/from-the-fog-frequently-asked-questions/
- **Scape and Run: Parasites**: "adapt to other sources of damage, eventually becoming immune. It is possible to reset immunity by dealing fire or lightning damage." [P, via resumo] https://rlcraft.wiki.gg/wiki/Scape_and_Run_Parasites

#### Quando começa a cansar (o que há de número)

| Jogo | O que foi dito | Fonte |
|---|---|---|
| Amnesia: The Bunker | "I just wasn't scared by it past the first hour." (Batboyo) | [P, via resumo] https://steamcommunity.com/app/1944430/discussions/0/3808406410968520048 |
| Amnesia: The Bunker | "familiarity breeds contempt"; "by the final act, I was very much over The Beast's bullshit" (7/10) | [P, via resumo] https://thejimquisition.com/post/amnesia-the-bunker-entrenching-review |
| Amnesia: The Bunker | "I don't think any game can make people continue to be scared once they've been exposed enough times." (Iggy Wolf) | [P, via resumo] https://steamcommunity.com/app/1944430/discussions/0/5264192561412755446 |
| Phasmophobia | "Fear happens when you play the first couple hours due to fear of the unknown." (Dimi); outro fala em 20+ horas | [P, via resumo] https://steamcommunity.com/app/739630/discussions/0/4638238788728973198 |
| Phasmophobia | "Every time I load in I do feel a little less dread" (2020) | [P, via resumo] https://steamcommunity.com/app/739630/discussions/0/4477100383812142060 |
| Subnautica | "Built my first base where he spawns. Got used to him after awhile, we are friends now." | [P, via resumo] https://steamcommunity.com/app/264710/discussions/0/1327844097129333332 |
| Alien: Isolation | resenhas falam em passar do ponto e repetir demais | [B] |

São relatos soltos. Não achei contagem de encontros nem estudo.

#### O que devolve frescor barato, segundo as fontes

- **Lugar e abertura diferentes.** Olsson: raro aparecer no mesmo lugar. Crítica oposta no mesmo jogo: "as soon as the game loads into these new areas the monster scurries right in front of me and jumps out approx 20 seconds later"; "predictable behavior really breaks immersion". [P, via resumo] thread 3808406410968520048
- **Mudar o cenário depois de uma falha.** Grip sobre The Dark Descent (acima).
- **Comportamento raro, visto tarde.** Um jogador de Phasmophobia conta que o cantarolar da Mare o assustou de verdade depois de muito tempo de jogo. [P, via resumo] thread 4477100383812142060. O Alien só teleporta duas vezes na campanha (pesquisa de 08/10).
- **Aparecer pouco.** "I'd rather have a game where the monster shows rarely and for a short time so I only have a vague idea of how it acts" (Batboyo). [P, via resumo]
- **Destravar comportamento com o uso.** Napper (acima).
- **Sorteio no mesmo gatilho.** Cave Dweller: o mesmo "fui visto" dá três desfechos.

#### Contraevidência: variedade que vira "aleatório"

- Alien: "unexplained alerts and unpredictable outcomes seemed to wrestle control of my fate away from me" (Leon Hurley, 18/7/2014). No mesmo texto ele elogia o contrário do Outlast, cujos inimigos "settled into avoidable patterns". [P, via resumo] kotaku 1607096520
- Bunker: "after hours of being able to safely hide under desks and beds, I was suddenly easily detectable" (Sterling). Regra que muda sem sinal acelera a passagem de "ameaça" para "chateação". [P, via resumo]
- Bunker: "The monster AI is not really dynamic, its more of a rubber band" (KajFlo). [P, via resumo]
- Rebirth: "the enemies are incredibly difficult to predict, primarily because you are punished for looking at them" (Andrew Haining, 5/1/2021). [P, via resumo] https://www.gamedeveloper.com/design/amnesia-rebirth-critical-analysis
- Phasmophobia, os dois lados. Em 2020: "there is barely any difrence between the ghosts". Resposta: se cada fantasma tiver um traço exclusivo, "'Uh, what ghost can flip couches again?' 'Demon' 'ok its a demon' -MISSION COMPLETE-". [P, via resumo] https://steamcommunity.com/app/739630/discussions/0/2844543519809928905 — e em 2024, já com mais de vinte tipos, "hunts have become predictable".
- Hello Neighbor (acima): adaptação lida como injusta.
- Midnight Lurker: a variante "Unprovoked" é aleatória por definição do autor. Não achei recepção específica dela.

### 2.2 Inferências

- Há **duas repetições diferentes**. Uma é do roteiro (sempre o mesmo lugar, a mesma abertura): resolve-se variando lugar, direção e desfecho. A outra é habituação à criatura em si: nenhuma fonte mostra que mais variantes a resolvam; o Phasmophobia tem regra de caçada própria para muitos dos seus tipos e os jogadores relatam o mesmo "primeiras horas".
- O que as fontes tratam como seguro de variar: **onde começa, de onde vem, como termina, e o que o cenário mostra depois**. O que elas tratam como arriscado: **a regra de detecção e de segurança** (o esconderijo que valia e deixou de valer).
- "Caçar de jeitos diferentes" nos mods lidos é quase sempre **um sorteio de estado no mesmo gatilho** mais **variação por lugar**. É barato e é o que Cave Dweller, GoatMan e Midnight Lurker fazem.
- Adaptação ao hábito do jogador é bem recebida quando **tem causa visível e volta atrás** (bloqueio do Man From The Fog decai; imunidade do parasita zera com fogo; nível de abdução do GoatMan cai em dias). É mal recebida quando parece onisciência (Alien, na pesquisa de 08/10; Hello Neighbor).
- A frequência pesa mais que a variedade. É a mesma lição do Mr. X na pesquisa de 08/10, agora com uma citação direta de jogador.

### 2.3 Lacunas

- Nenhuma palestra da GDC foi lida. Não li o blog da Frictional direto; as falas de Grip vêm de entrevistas e de texto dele no blog da PlayStation.
- Recepção das variantes do Midnight Lurker e do GoatMan: não achei.
- Lethal Company: só trecho de busca nesta rodada.
- Nemesis de 1999 (aparições sorteadas e escolhas) não foi pesquisado em fonte lida.
- Não achei mod de Minecraft cuja criatura mude de tática conforme o hábito do jogador além do bloqueio do Man From The Fog e da adaptação a dano do SRP. "The one who Slashes" alega isso sem detalhe.

---

## 3. Tópico 3 — caçada que termina sem morte

### 3.1 O que as fontes dizem

- **GoatMan 3.0 (Minecraft).** "Abduction: instead of always killing you, grabs you instead. Steals your items and stashes it near the last location you were abducted, places you somewhere else with half-heart that leaves debuffs depending on the level of abduction. Becomes more punishing the more you are abducted with the chance of just killing you on the third abduction. Each level of abduction decays after a few days without interacting with GoatMan". [P, literal] https://modrinth.com/mod/goatman — 725 mil downloads; código fechado; sem recepção encontrada.
- **Weeping Angels (Minecraft).** No golpe: 50% de chance (`teleport_chance`) de teleportar o jogador para até 400 blocos (`teleport_range`), por padrão para uma dimensão sorteada (`interdimensional_teleporting = true`); senão, rouba item e dá dano. [C] https://github.com/Jeryn99/Weeping-Angels/blob/mc/1.20.1/common/src/main/java/dev/jeryn/angels/common/entity/angel/WeepingAngel.java (linhas 101–147) e `WAConfiguration.java` (linhas 103–113). Recepção nos relatos: pedidos para desligar a troca de dimensão (#335, #343, #345), para os anjos matarem em vez disso (#334), e para desligar o roubo de picareta (#361, #426). [P, literal] https://github.com/Jeryn99/Weeping-Angels/issues
- **SOMA, Safe Mode.** Grip: "It turns out that it worked way better than we expected and it fit really nicely." O crítico: "Even in safe mode, Soma still scared me", com os monstros "content to leave me alone and incapable of killing me". [P, via resumo] https://www.theringer.com/2017/12/8/16750916/soma-safe-mode-frictional-games — repare que o medo relatado vem do ambiente; a perseguição em si quase deixa de existir nesse modo. No modo normal, outro crítico: "death is just a nuisance"; o jogo assusta mais "when the monsters weren't visible". [P, via resumo] https://kotaku.com/soma-is-scarier-when-the-monsters-aren-t-around-1734378666
- **Amnesia: Rebirth.** Intenção (Grip, 2020): o fracasso muda a aparência da personagem e pesa na história. Resultado para um desenvolvedor que analisou o jogo: "most encounters for me ended in me being caught and 'skipped' past the encounter"; "there are no options available to you". [P, via resumo] blog.playstation.com/?p=341757 e gamedeveloper.com/design/amnesia-rebirth-critical-analysis
- **Amnesia: The Bunker** (mata, mas sem custo): "I was never really worried about dying. Most runs last 5-10 minutes and there's no penalty for death." e "When the monster shows up, there's no fear-creating doubt in my mind". [P, via resumo] thread 5264192561412755446
- **Hello Neighbor.** Ser pego: "your progress through the level doesn't reset when you're caught, and you get to keep whatever items you have on you". Avaliação: chateação, não medo. [P, via resumo]
- **Wallmaster (Zelda).** "If it catches Link, it takes him back to the beginning of the Dungeon", sem dano. Aviso: "their shadow appears beneath him, slowly growing bigger" e "a warning sound". [P, via resumo] https://zeldawiki.wiki/wiki/Wallmaster
- **Subnautica.** O Reaper já está na pesquisa de 08/10. Recepção: jogadores passam de medo a rotina ("we are friends now"). [P, via resumo]
- **The One Who Watches (Minecraft).** "He may let you escape though only because He would much rather see you run in fear". [P, literal]
- **"The Hollow".** Não identifiquei o mod pedido. Achei "The Hollow" (FireTiger369, Forge 1.19.2), que adiciona "dwellers" e um bioma; a página não diz o que acontece ao ser pego. [P, via resumo] https://fgguides.com/minecraft/the-hollow/ — outros nomes parecidos só em busca: "Hollow Steve", "The Hollow Stalker", "HOLLOWED". [B]
- The Forest e a dimensão de bolso do SCP: nada novo além da pesquisa de 08/10.

### 3.2 Inferências

- **Não há dado direto sobre a terceira ou a décima vez.** O que as fontes permitem dizer: a tensão da perseguição acompanha (a) se ser pego tira algo que o jogador sente, e (b) a habituação geral à criatura, que acontece com ou sem morte.
- **Sem custo, a perseguição vira ruído** (Hello Neighbor, Bunker, Rebirth). **Com custo sem volta, vira raiva** (Weeping Angels: preso em outra dimensão, picareta perdida).
- Os desenhos que ficam entre os dois têm três traços em comum: o custo **cresce** com a reincidência, **pode ser desfeito** pelo jogador (itens guardados perto, saída andando), e **esfria com o tempo** (GoatMan). O Wallmaster acrescenta um quarto: aviso claro antes de pegar.
- O que os designers mudaram para manter a tensão sem morte: mudar o cenário depois da falha (The Dark Descent), tornar a consequência visível no corpo da personagem (Rebirth, com execução criticada), tirar a caça e deixar a presença (SOMA Safe Mode). Nenhum deles relata ter mantido a tensão da **perseguição** em si por muitas repetições.

### 3.3 Lacunas

- Recepção da abdução do GoatMan.
- The Forest: não achei relato sobre ser capturado várias vezes.
- SOMA Safe Mode: as duas matérias da PC Gamer e a da Vice não abriram; fiquei com The Ringer.
- "The Hollow": precisa do link certo.

---

## 4. Tabela do Tópico 1

| Truque | Quem responde | Como | Como avisa | Recepção | Fonte |
|---|---|---|---|---|---|
| Barco | Warden, Wither, Dragão (vanilla 26.2) | `canRide` devolve `false` | Não avisa; o barco só empurra | Sem reclamação encontrada. Motivo oficial do Warden: IA quebrada montado | [C] jar · [P] MC-250259 |
| Barco | Creaking (vanilla) | Tag `cannot_be_pushed_onto_boats` | Não avisa | Carrinho ainda o empurra: "Works As Intended" | [C] jar · [P] MC-277107 |
| Barco e carrinho | Cave Dweller | `canRide`/`startRiding` falsos; opção `allow_riding` | Não avisa | Sem relato de barco no rastreador | [C] |
| Barco e carrinho | Man From The Fog Reimagined | `canStartRiding` falso | Não avisa | Sem relato | [C] |
| Barco e carrinho | Midnight Lurker | Desmonta no tick seguinte; nada mais rápido se o jogador está de barco | Texto na página do mod | Sem relato | [C] · [P] |
| Barco e carrinho | Cataclysm (chefes) | `canRide` falso | Não avisa | Não pesquisada | [C] |
| Barco e carrinho | Enhanced AI | Quebra o veículo (padrão) ou proíbe montar | Não li a meta que quebra | Pedido de jogador que virou recurso | [C] · [P] issue #12 |
| Barco e carrinho | No Boat Cheesing (26.2) | Cancela montaria de mob que está atacando, fugindo ou sendo atraído | Não avisa | Não encontrada | [C] |
| Carrinho | Alex's Caves | Não responde: dá conquista | Conquista | Truque virou conteúdo | [C] |
| Carrinho | Mowzie's Mobs (Grottol) | O bicho usa o carrinho para fugir | Comportamento visível | Mecânica planejada | [C] |
| Pilar | Warden | Estrondo que atravessa bloco; 10 s após fixar alvo; 15 × 20 blocos | Carga de 1,7 s com som | Aceito (pesquisa de 08/10) | [C] jar · [P] wiki |
| Pilar | Man From The Fog, Midnight Lurker, Cave Dweller | Escalam | Sem aviso próprio; a subida é visível | Cave Dweller #11: travava ao escalar | [C] · [P] |
| Pilar | GoatMan | Salto "when player is too far or high", destruindo blocos | Não descrito | Não encontrada | [P] |
| Cavar e se fechar | Vex | Atravessa qualquer bloco; vida de 30–119 s | Sem aviso próprio | Não pesquisada | [P] |
| Cavar e se fechar | Midnight Lurker Aggressive; Reimagined cave dweller | Quebra blocos | Não descrito | #22: quebrava bloco "inquebrável" | [C] · [P] |
| Cavar e se fechar | Enhanced AI | Zumbi minerador; ou puxa o jogador para perto após 2,5 s | Som só depois do teleporte | Não encontrada | [C] |
| Água | Enderman | Fraqueza declarada: água fere, não teleporta montado | Regra fixa | Truque clássico; o Enhanced AI o chama de "bully endermen" | [P] · [C] |
| Água e lava | Man From The Fog; Cave Dweller; Warden | Respira e nada; depth strider 1,5; imune a lava | Não avisa | Blog recomenda lava contra o Cave Dweller | [C] · [P] |
| Porta | Zumbi | Até 10% tentam; quebra em ~10 s no Difícil | Não li (de memória: pancadas e rachadura na porta) | Não pesquisada | [P] |
| Porta | Midnight Lurker; Weeping Angels | Quebra, menos porta de ferro; abre | Não descrito | Não encontrada | [P] · [C] |
| Vão de 1 bloco | Cave Dweller; Man From The Fog | Caixa de colisão menor, rasteja | Sem aviso próprio; fica lento e visível | O momento mais citado (08/10); #16 e #11 mostram travamento | [C] · [P] |
| Teia | Aranha, Wither; Weeping Angels | `makeStuckInBlock` ignorado | Não avisa | Cave Dweller #20: preso em teia, sem correção | [C] · [P] |
| Cama | Vanilla | `Monster` a 8 × 5 blocos impede dormir | Mensagem "monsters nearby" | Não pesquisada | [C] · [P] |
| Cama | Man From The Fog; Enhanced AI | Acorda o jogador | — | Não encontrada | [C] |
| Elytra, pérola, distância | Man From The Fog | Teleporta para trás do alvo após 2 min a mais de 200 blocos | Não avisa | Não encontrada | [C] |
| Bater de longe | Cataclysm | Dano cai com a distância; teto de dano; cura e volta para casa | Não avisa | Não pesquisada | [C] |
| Escudo, armadura forte | Midnight Lurker | Recarga de 2,5 s no escudo; dano escala com a armadura | Texto na página | #14: "very annoying" (invulnerável) | [P] |
| Deslogar | — | Só plugins de PvP (matam) | — | — | [B] |

---

## 5. Recomendações para o Sussurros (todas são inferência)

1. **Fazer a criatura recusar veículo com `canRide` devolvendo `false`.** Vem de: Warden, Wither e Dragão no jar; Cave Dweller, Man From The Fog e Cataclysm no código. Cobre barco e carrinho; a tag só cobre barco. Vale pôr também na tag, por garantia. Não usar "montar e sair" (tranco na tela) nem "quebrar o barco" (destrói item do jogador). Conferir com um teste de servidor: barco parado no caminho, carrinho passando.
2. **Tratar teia como a Aranha e o Wither tratam**, ou garantir que teia caia na regra "sem caminho" da pesquisa de 08/10. Vem de: Cave Dweller #20 e Weeping Angels. Hoje não há `makeStuckInBlock` na criatura.
3. **Não fechar todo truque.** Decidir quais só atrasam e deixá-los. Vem de: a Mojang recusar o relato do Creaking preso; Alex's Caves e Grottol; o blog recomendando lava. Critério sugerido: fecha-se o truque que **encerra** a caçada de graça; mantém-se o que só compra segundos, desde que o teto de tempo exista.
4. **Variar o começo, a direção e o fim; não a regra.** Vem de: Olsson, Grip, o sorteio do Cave Dweller; e, do outro lado, Sterling ("suddenly easily detectable") e Hurley ("unexplained alerts"). Na prática: a frase-regra continua uma só, e o que muda por lugar (caverna, mata, casa) é de onde ele vem e como a cena fecha.
5. **Guardar um ou dois comportamentos raros**, que apareçam uma vez em muitas horas e não tragam regra nova. Vem de: o relato da Mare no Phasmophobia e os dois teleportes do Alien.
6. **Se ele aprender com o hábito do jogador, que tenha causa visível e esfrie com o tempo.** Vem de: bloqueio do Man From The Fog (sobe por golpe, decai), parasitas do SRP (zera com fogo), GoatMan (cai em dias); e das reclamações de onisciência (Alien, Hello Neighbor).
7. **Antes de criar variante, conferir a frequência.** Vem de: "I'd rather have a game where the monster shows rarely..." e do Phasmophobia, que tem muitas regras e a mesma queixa. O relatório de sessão já conta caçadas por hora; é o primeiro número a olhar quando o dono disser que repetiu.
8. **Na captura: custo que cresce, que o jogador consegue desfazer, e que esfria.** Vem de: GoatMan (itens guardados perto, piora por nível, decai), Weeping Angels (as queixas são de ficar preso e de perder item para sempre), Hello Neighbor e Rebirth (sem custo vira ruído). O lugar para onde o jogador é levado precisa ter saída sempre, e nada tirado dele pode sumir.
9. **Decidir a cama de propósito.** A criatura não é `Monster`, então o vanilla não bloqueia o sono nem mostra "monsters nearby" [C]. Ou a caçada impede dormir com sinal próprio (Man From The Fog acorda o jogador), ou dormir é saída aceita. Cuidado inverso: se um dia ela virar `Monster`, a mensagem do vanilla entrega que ela está a 8 blocos.
10. **Elytra, pérola e logout: deixar escapar e lembrar.** Não achei precedente de recepção para punir essas saídas num mod de terror; os precedentes são teleporte sem aviso (Man From The Fog) e morte no logout em PvP, que não servem aqui. Mantém-se a proposta da pesquisa de 08/10 (retomar do aviso), sabendo que ela não tem evidência por trás.

---

## 6. Correções e acréscimos à pesquisa de 08/10

| Onde | O que estava | O que achei |
|---|---|---|
| 3.7, linha "Barco / água", coluna "Hoje" | "Não lido" | Lido: `HospedeEntity` (0,7 × 3,0, `PathfinderMob`, `MobCategory.MISC`) não sobrescreve `canRide`, `startRiding` nem `makeStuckInBlock`. Pelo vanilla, barco e carrinho a recolhem. [C] `src/main/java/com/sussurros/entidade/HospedeEntity.java` linha 38; `registro/ModEntidades.java` linhas 21–22. Cópia em disco em 10/10; não sei em que branch está |
| 3.7, linha "Dormir" | "(não verificado)" | O vanilla só barra o sono por instâncias de `Monster`; a criatura não é. O `Diretor` consulta `p.isSleeping()` e `p.isPassenger()` (linhas 835–836 e 3392), mas não li o contexto |
| 1.9 e seção 5, Warden | "alguns segundos"; limiar marcado como não conferido | No jar: 200 ticks (10 s) após fixar o alvo; 40 ticks após golpe ou estrondo; alcance 15 × 20; carga de 34 ticks. A wiki escreve 14 blocos |
| 1.11, Cave Dweller | `can_disable_shields` listado entre os anti-truques | No ramo `1.19.2` o padrão é `false` (`ServerConfig.java` linha 108). `allow_riding = false` e `target_invisible = true` conferem |
| 1.10, Creaking | Regra dos 5 s no mesmo bloco | Confere. Acréscimo: a Mojang aceitou como intencional ou inválido prender e empurrar o Creaking |
| 3.7, referência do barco | Só `allow_riding` | Acréscimo: a tag não protege contra carrinho, e o barco só recolhe sem jogador no comando |

Não achei outro erro de fato na pesquisa de 08/10 nos pontos que cruzei.

---

## 7. O que ficou sem verificação

- **Nada foi testado no jogo.** Que o barco recolhe a criatura é leitura de código dos dois lados. Não sei o que o código de reposicionamento do mod faz com ela montada.
- A largura 1,375 do barco é de memória.
- Citações marcadas "(via resumo)" podem diferir do original em palavras.
- Mods fechados: tudo o que digo deles vem da página do autor.
- A busca de código do GitHub não indexa todos os repositórios; "não achei `canRide`" em SCP: Lockdown e Mowzie's Mobs é ausência na busca, não leitura de todas as classes.
- O repositório do Midnight Lurker parou em 10/2024; a página do mod está na 4.0.0. O desmontar automático pode ter mudado.
- O ramo padrão do Cave Dweller é `1.19.2`; as versões 1.20.1 podem diferir.
- Não li: GDC Vault, blog da Frictional, comentários da CurseForge, Reddit.
- A resenha do Monstrum na Push Square, a matéria da GameSpot sobre fantasmas, a da Softpedia sobre o Alien e a da Unwinnable devolveram 403.

## 8. Como foi feito

- Busca e leitura de páginas pelo leitor automático; arquivos de código lidos crus do GitHub; descrições de mods pela API pública do Modrinth; relatos de bug pela API pública do rastreador da Mojang; a página "Hunt" do Phasmophobia pela API do Fandom (a leitura normal devolveu 402).
- As consultas ao GitHub usaram o `gh` já logado na máquina, só para ler e buscar em repositórios públicos.
- O jar do Minecraft 26.2 foi lido com `unzip -p` e `javap`, sem extrair nada para o disco.
- Li três trechos do repositório do Sussurros com busca de texto (seção 6). Não usei git nem Gradle.
- Chamei por engano, uma vez, o guia do conector Claude Docs. É só leitura: nenhum documento foi criado.
- Nenhuma ferramenta gravou arquivo fora deste.
