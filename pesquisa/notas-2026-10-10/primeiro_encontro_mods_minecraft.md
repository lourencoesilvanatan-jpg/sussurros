# Primeiro avistamento e primeiro encontro em mods de terror de Minecraft (SPOILERS)

Pesquisa feita em 2026-10-10. Complementa `PESQUISA-E-ANALISE.md` (seções 6, 10, 12, 13 e 14) e não repete o que já está lá; o foco aqui é a **primeira hora**.

**Marcação das fontes**

- **[C]** li o código ou o arquivo do datapack (caminho do arquivo no link).
- **[P]** abri e li a página (páginas do Modrinth foram lidas pela API pública do Modrinth, que devolve o texto da página; wiki e CurseForge foram lidas por uma ferramenta que resume a página, então as citações vêm como a ferramenta as devolveu).
- **[P*]** post do Reddit lido **só em parte**: título e as primeiras 200 a 330 letras do texto, **sem os comentários**. Veio de uma única passada pelo arquivo Pullpush. Ver "O que continua sem verificação".
- **[B]** só o trecho que apareceu no resultado de busca.

Conta usada em todo o texto: 1 dia de Minecraft = 24.000 ticks = 20 minutos reais; a primeira noite começa aos 10 minutos reais.

---

## 1. Regras concretas de cada mod para a primeira aparição

### Takeaway
Nos mods cujo código foi lido, depois que a condição de início é cumprida, o primeiro sinal chega em segundos ou poucos minutos (Man From The Fog Reimagined: ~1 min de noite; Him: 1 a 8 min; Cave Dweller Evolved: 5 a 20 min debaixo da terra; From The Fog: 1 a 2 min). A espera longa vem sempre de um **portão anterior** (3 dias de jogo no From The Fog, altar no Him, "não na primeira noite" no The One Who Watches), e não do sorteio em si.

### Cited Findings

**From The Fog (datapack v1.11.3-26.2, release de 2026-10-06; HEAD lido em 2026-10-10)**
- O atraso inicial é a opção `haunting_delay`, padrão `3` (dias). [C] [defaults.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/config/defaults.mcfunction)
- O portão é `started_events`: a cada segundo o datapack divide o tempo do relógio do mundo por 24.000 e, se o dia atual for maior ou igual ao atraso, chama `start_events`. [C] [1_second.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/1_second.mcfunction), [check_day.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/shrine/haunting_delay/check_day.mcfunction)
- O site oficial confirma: "It will take 3 in-game days after installing the datapack to have the major events to start happening by default." e "The shrine's only purpose is to skip the 3 day delay." [P] [lunareclipse.studio](https://lunareclipse.studio/creations/from-the-fog)
- **O atraso não bloqueia tudo.** Antes do portão, a cada 5 minutos reais o datapack já tenta: colocar estruturas (pirâmide com musgo, pirâmide de areia, carta, cruz de madeira), "dreadful donation", "fearful footsteps" e "ghost doors". O comentário no código diz: "Anything past this point can only happen after started events equals one." [C] [5_minutes.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction)
- Cada uma dessas tentativas só roda se a opção correspondente estiver ligada. Para as pegadas, a chance "default" é 1 em 25 por tentativa (insane 3, common 6, rare 50, scarce 125), dividida pelo multiplicador de atividade; a tabela usada por portas e doações não foi lida. [C] [fearful_footsteps/event.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/fearful_footsteps/event.mcfunction), [determine_chance/common.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/utils/determine_chance/common.mcfunction)
- Acender o santuário pula o atraso **e** liga uma rajada: multiplicador de atividade 6 por 360 s e o próximo ciclo de eventos em 150 s (em vez de 300). [C] [found_shrine.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/shrine/found_shrine.mcfunction)
- Ao iniciar os eventos, pode mandar no chat a mensagem falsa de entrada "Herobrine joined the game" (opção `eerie_entrance`). [C] [start_events.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/start_events.mcfunction)
- Depois do portão, intervalo padrão até o próximo avistamento: 60 a 120 s de dia e 30 a 60 s de noite, dividido pelo multiplicador. O contador só é sorteado quando não há avistamento em andamento. [C] [roll_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/roll_spawn_timer.mcfunction), [set_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/set_spawn_timer.mcfunction)
- "Noite" é o intervalo 13.000 a 24.000 do relógio. [C] [is_night.json](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/predicate/checks/is_night.json)
- Sorteio do tipo: 1 em 7 "creeping" (desligado por padrão, então sorteia de novo no segundo seguinte), 2 em 7 "stalking", 2 em 7 "lurking", 2 em 7 "dwelling". [C] [roll_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/roll_spawn_timer.mcfunction)
- Cada avistamento some sozinho em 30 s (`despawn_timer:30`; pesadelo 60) e é removido se nenhum jogador estiver dentro de `max_distance` (creeping 16, stalking 50, dwelling 64, lurking 128). [C] [presets/lurking.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/presets/lurking.mcfunction), [presets/stalking.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/presets/stalking.mcfunction), [presets/dwelling.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/presets/dwelling.mcfunction)
- Primeira coisa que acontece **ao jogador**: só se ele chegar a 2 blocos do Herobrine. A câmera é virada para ele, toca o som do susto, ele some 10 ticks depois e a atividade dobra por 360 s. Não há dano (o soco só existe com "Give Him Control"). A opção `sudden_scare` vem ligada; a que derruba o jogo (`malicious_malfunction`) vem desligada. [C] [sudden_scare/init.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/sudden_scare/init.mcfunction), [sudden_scare/jumpscare.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/sudden_scare/jumpscare.mcfunction)
- Há um cão de guarda contra travamento: a cada 5 minutos, se não existir nenhum Herobrine, roda a limpeza de avistamentos. Comentário: "Make sure the world doesn't get stuck in limbo with no sightings." [C] [5_minutes.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction)

**Cave Dweller Evolved (fork MIT, arquivado; último release `test_build-1.20.1-1.6.4`, 2023-09-26)**
- O relógio é por dimensão e **só anda quando existe pelo menos um jogador "relevante"**: Y menor ou igual a 40, luz do céu até 8, luz de bloco até 15, e fora da superfície (superfície vem desligada). [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java), [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java)
- Tempo até poder nascer: 300 a 600 s, com 40% de chance de ser 1.200 s. Depois disso, 0,5% de chance por tick. [C] [Timer.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/util/Timer.java), [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java)
- O dono do fork explica igual: "The spawn timer by default can be between 5 - 10 minutes with a 40 % chance of it being 20 minutes". E dá os números do mod original: "Spawn timer: 480 - 1080 seconds with a chance of 800 - 1600 seconds ... Noise timer: 100 - 180 seconds". [P] [issue 2](https://github.com/SiverDX/cave_dweller/issues/2)
- Onde nasce: até 40 tentativas a no máximo 35 blocos na horizontal e 6 na vertical, nunca a menos de 16 blocos de um jogador, e só onde há caminho até a vítima. Nasce invisível. [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java), [Utils.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/util/Utils.java)
- **O som de caverna não depende de a criatura existir.** Toca quando o relógio de ruído (240 a 360 s) vence e já existe uma criatura **ou** o relógio de nascimento passou da metade do máximo (300 s). Toca também no momento em que ela nasce. [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java)
- Quanto fica: 300 s; depois que a perseguição começa, 30 s. [C] [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java), [CaveDwellerEntity.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/entities/CaveDwellerEntity.java)
- O que acontece ao ser olhada (alcance de detecção 60 blocos): sorteio entre perseguir, encarar, encarar e fugir (25% / 50% / 25%). Se o jogador chegar a 12 blocos, perseguição forçada. [C] [CaveDwellerTargetSeesMeGoal.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/entities/goals/CaveDwellerTargetSeesMeGoal.java), [CaveDwellerEntity.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/entities/CaveDwellerEntity.java)
- No modo encarar ela anda na direção do jogador quando ele não olha e para quando ele olha; depois de 4 a 13 olhadas, passa a ter 30% por tick de trocar para perseguir ou fugir. [C] [CaveDwellerStareGoal.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/entities/goals/CaveDwellerStareGoal.java)
- Primeira consequência: dano de 6 por golpe (3 corações), com 60 de vida e velocidade 0,5. [C] [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java)

**Man From The Fog Reimagined (GPL-3.0, GitHub arquivado e movido para o Codeberg; release 0.6.3 de 2024-11-14, último push 2026-02-01)**
- Uma tentativa a cada 10 s (`timeBetweenSpawnAttempts`), só de noite (`spawnInDay = false`), com chance 0,33. Quando passa: 60% das vezes toca só um som assustador num ponto atrás do jogador (`fakeSpawnChance = 0.6`) e 40% das vezes nasce a criatura. [C] [ModConfig.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/config/ModConfig.java), [ServerEvents.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/server/ServerEvents.java)
- Distância: 20 a 60 blocos, atrás do jogador (direção oposta ao olhar, com até 60 graus de desvio). [C] [ModConfig.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/config/ModConfig.java), [WorldHelper.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/util/WorldHelper.java)
- Estado ao nascer: se há linha de visão livre até o alvo, "encarar"; se há bloco no caminho, "seguir". [C] [TheManEntity.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/entity/the_man/TheManEntity.java)
- Encarar: olhado por 4 s, sorteia um de três (sumir, perseguir, fugir). Sem ser olhado por 12 s, passa a seguir. Persegue se o jogador chegar a 15 blocos com visão livre (30 blocos no modo seguir, quando olhado). [C] [StareState.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/state_machine/states/the_man/StareState.java), [StalkState.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/state_machine/states/the_man/StalkState.java), [TheManEntity.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/entity/the_man/TheManEntity.java)
- A perseguição dura de 30 a 120 s (o relógio só corre durante ela) e a criatura some quando amanhece. [C] [TheManEntity.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/entity/the_man/TheManEntity.java)
- O som assustador usa volume 5 e `playSound(null, ...)`: é ouvido por todos no raio, não só pelo alvo. [C] [ServerEvents.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/server/ServerEvents.java)

**The Man From The Fog (original, Forge, fechado)**
- A página só diz que ele "appears in the Night and stalks you. Eventually it will start to chase you kill you", com configuração `spawn_rate` (often, normal, rare), `vanish_distance: 30`, perseguição de 400 a 1.000 ticks e `spawn_at_day: false`. Não há número de atraso inicial. [P] [Modrinth](https://modrinth.com/mod/the-man-from-the-fog)

**Him (Herobrine, Fabric 1.21.4; HEAD com último push em 2025-03-16)**
- Nada acontece até o jogador acender o altar. A única exceção é a semente famosa do Herobrine, que ativa sozinha e força os três relógios para 15 s, 30 s e 45 s. [C] [Him.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/Him.java); confirmado pelo autor: "Yes, you need to build the shrine to activate" [P] [issue 17](https://github.com/Yegiyan/Him/issues/17)
- Com o altar ativo, três relógios independentes: perseguir de longe 60 a 480 s, assombrar 60 a 600 s, estragar 60 a 600 s. [C] [HimConfig.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/HimConfig.java)
- O README chama isso de ritmo lento: "the default values will cause more of a slow burn!" [C] [README.md](https://github.com/Yegiyan/Him/blob/HEAD/README.md)
- Aparição: jogador aleatório, a 42 a 64 blocos, no topo do terreno, fora de um cone de 70 graus do olhar. [C] [Stalk.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/events/Stalk.java)
- Depois de 2 s com linha de visão para o jogador ele começa a se afastar, e é removido assim que o jogador não está virado para o lado dele. [C] [StalkPlayerGoal.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/goals/StalkPlayerGoal.java)
- Primeira consequência: se um jogador chegar a 24 blocos, Escuridão por 3 s, um som a volume 0,75 e ele some 1,5 s depois. Sem dano. [C] [HerobrineEntity.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/entities/HerobrineEntity.java)

**TheWatcher (Fabric/NeoForge 1.20.1 e 1.21.1; HEAD de 2026-08-08, comentários do código citam a 0.1.8)**
- Medo de 0 a 100 por jogador, recalculado uma vez por segundo: +1 se a luz for 3 ou menos, +1 se parado há mais de 5 s; -2 se perto de fogueira acesa ou se for dia com céu aberto. Tocha na mão corta 40% do ganho. [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java), [TheWatcherConfig.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/TheWatcherConfig.java)
- Degraus: medo 20 libera sons falsos e ecos das próprias ações (10 a 30 ticks depois); 35 mexe no inventário; 40 a 80 pode simular queda do jogo; 45 quebra tocha próxima; acima de 50 os animais encaram; **100** (padrão de `shadowFearThreshold`) faz o vulto nascer. [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java)
- O vulto nasce **à frente** do jogador: 6 a 9 blocos adiante e 3 a 5 para o lado, em luz 7 ou menos. Some quando a mira do jogador cruza a caixa dele (e o medo cai para 90). Se não for olhado, pula 2 blocos para mais perto a cada 5 s. Não causa dano. [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java)
- A barra de medo aparece na tela por padrão (dá para desligar com `/watcherfearbar off`). [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java)

**The Hollow / hollow-dread (Fabric 26.2, fechado, atualizado em 2026-07-24)**
- Pavor de 0 a 100 por jogador. De 0 a 25: "Nothing at all. No HUD, no sounds." A criatura só existe de 75 a 100. [P] [Modrinth](https://modrinth.com/mod/hollow-dread)
- Primeiro avistamento: "it spawns 14–22 blocks out, in darkness, deliberately preferring places you are not currently facing. The first sighting is always a surprise." [P] [Modrinth](https://modrinth.com/mod/hollow-dread)
- Sai com luz 13 ou mais, com pavor abaixo de 55, ou depois de "about 90 seconds". Se alcança o jogador: "It blinds you, buries you in Darkness and Weakness, pins your dread to maximum, and leaves." [P] [Modrinth](https://modrinth.com/mod/hollow-dread)

**The Broken Script (NeoForge 1.21.1, fechado; atualizado em 2026-09-25)**
- "Null joins the game at the first midnight", que acontece "18000 ticks (or 15 minutes in real-world time) after the world is created", com mensagem falsa de entrada, e passa a aparecer na lista de jogadores. [P] [wiki, Null](https://thebrokenscript.wiki.gg/wiki/Null)
- FAQ da wiki: "Null should join within the first night, but it might take you a few nights in your world before you start seeing more entities and for the game to ramp up." [P] [wiki, FAQ](https://thebrokenscript.wiki.gg/wiki/Frequently_Asked_Questions)
- FAQ do Modrinth: "Events are primarily tied to game time, in terms of when they start to happen. However, you can increase the max number of possible events per day". [P] [Modrinth](https://modrinth.com/mod/the-broken-script)
- A lua controla o ritmo: "At the start of the game, the moon looks and behaves as normal."; no estágio 0 de corrupção a chance por checagem só sai de zero na segunda metade do ciclo lunar e chega a 0,85%. [P] [wiki, The Moon](https://thebrokenscript.wiki.gg/wiki/The_Moon)
- Variantes do Null e o que fazem ao jogador: "Watching" (só de noite, 6 min 40 s; chegar a 20 blocos dispara um de dez desfechos), "Flying" (só de noite; a 20 blocos some em 70% das vezes), "Invade Base" (parado na base, some em 4 min 10 s), "Mining" (subsolo, 1 min, 6 de dano por golpe), "Scare" (0,5 s, 5 de dano ou nenhum), "Chase" (10 corações por golpe, cerca de 22,5 s). [P] [wiki, Null](https://thebrokenscript.wiki.gg/wiki/Null)

**The Obsessed (Forge/NeoForge, atualizado em 2026-09-18)**
- O primeiro contato é com a versão "Targeter", que nasce ao acaso em qualquer bioma: "Upon getting close to this version and crossing its line of sight, you will become locked on as an obsession target." [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)
- Tem garantia: "If not targeted within the first 60 minutes (configurable) in your world, targeting will be forced once." [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)
- Depois: "once you are targeted, spawning increases gradually and reaches its peak after an hour" e "Spawns 4x more frequently at night." [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)

**The Midnight Lurker (Forge, MIT)**
- "has a chance to spawn at night or in caves"; "The mob works by spawning near the player and waits for the player to find them." Olhar o espanta; chegar perto sem olhar o irrita e ele ataca por cerca de um minuto. [P] [Modrinth](https://modrinth.com/mod/the-midnight-lurker)
- `lurker_spawn_rate` padrão 3; com 3, cada tentativa de nascimento natural passa em 40% das vezes (`Math.random() >= 0.6`), e só se não houver outra variante numa caixa de 700 blocos (`multi_spawning` desligado). [C] [LurkerconfigProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/LurkerconfigProcedure.java), [MidnightLurkerNaturalEntitySpawningConditionProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/MidnightLurkerNaturalEntitySpawningConditionProcedure.java)
- Insanidade: `insanity_countdown_time` padrão 3, que o próprio arquivo explica: "1 is 5mins, 2 is 10mins, 3 is 20mins, and 4 is 30mins". Ao vencer, 70% de chance de subir um estágio; o agressivo vem no estágio 7. [C] [LurkerconfigProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/LurkerconfigProcedure.java), [InsanityStageTimerProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/InsanityStageTimerProcedure.java)
- O relógio de insanidade só começa quando uma variante "Neutral/Runaway" nasce perto: "it will start the insanity timer of the player closest". [P] [Modrinth](https://modrinth.com/mod/the-midnight-lurker)

**Eyes in the Darkness (BSD-3)**
- Sem atraso inicial: um ciclo de nascimento a cada 150 ticks (50 perto da meia-noite; não conferi se é a do jogo ou a do relógio real), até 2 pares de olhos por jogador (3 perto da meia-noite), a no máximo 64 blocos. O susto vem ligado e aplica veneno nível 1. [C] [ConfigData.java](https://github.com/gigaherz/EyesInTheDarkness/blob/HEAD/src/main/java/dev/gigaherz/eyes/config/ConfigData.java), [EyesSpawningManager.java](https://github.com/gigaherz/EyesInTheDarkness/blob/HEAD/src/main/java/dev/gigaherz/eyes/EyesSpawningManager.java)

**The Legend of Herobrine (LGPL-3.0)**
- Padrões `"HerobrineAlwaysSpawns": false` e `"AltarRequiresShrine": true`: as formas do Herobrine só nascem depois que o jogador ativa o altar. O espião observa a 32 blocos (`HerobrineSpyObservationDistance`). [C] [herobrine.json](https://github.com/Alex-MacLean/TheLegendOfHerobrine/blob/HEAD/src/main/resources/data/herobrine/default_config/herobrine.json)

**Mods fechados com pouca documentação**
- The One Who Watches: "He will not spawn during the first day/night cycle" e "He may not show up every night"; ele "CAN kill you but doesn't HAVE to kill you". [P] [Modrinth](https://modrinth.com/mod/the-one-who-watches)
- The Anomaly Rephased: "The mod is meant for long playthroughs, you will not encounter the entity during the first days". O autor avisa que não responde perguntas sobre o comportamento. [P] [Modrinth](https://modrinth.com/mod/the-anomaly-rephased)
- The Silence: "A custom shadow entity that stalks the player across multiple in game days" e "A 6 phase escalation system"; "No jumpscares. No monsters with health bars." [P] [Modrinth](https://modrinth.com/mod/the-silence-mod)
- The Knocker: a página só diz "It will stalk, jumpscare or attack you based on various situations. Thanks to the enhanced mechanics it knows where you live!" [P] [Modrinth](https://modrinth.com/mod/the-knocker). Um artigo diz que ele começa com batidas e "After a few nights, The Knocker loses patience and begins actively attacking you". [B] [Sportskeeda](https://www.sportskeeda.com/minecraft/what-minecraft-the-knocker-mod)
- GoatMan (3.0): tem presságios antes da criatura: "Goat Omen: spawns a few goats that marks GoatMan's territory", "Watched: goats will sometimes stare silently at the player", "Followed: a goat will sometimes follow player at a distance". Sem números. [P] [Modrinth](https://modrinth.com/mod/goatman)
- Don't Let It Learn (0.1.0-alpha.2, Forge 1.20.1): a página não dá nenhum número de tempo, distância ou frequência. [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dont-let-it-learn)
- Existence: alucinações com intervalo mínimo de 600 ticks (30 s), chance ligada ao estresse. [C] [ServerStressManager.java](https://github.com/CipherXOR/Existence/blob/HEAD/common/src/main/java/me/cipher/existence/server/ServerStressManager.java)

### Conferência da pesquisa existente (`PESQUISA-E-ANALISE.md`)
- **Seção 13, "nada acontece nos três primeiros dias de jogo": incompleto.** O atraso de 3 dias está certo, mas pegadas, portas fantasma, doações e estruturas já são tentadas antes dele, e o santuário pula o atraso. [C] [5_minutes.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction), [found_shrine.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/shrine/found_shrine.mcfunction)
- **Seção 13, multiplayer: confirmado.** O avistamento é chamado com `@r[gamemode=!spectator]`. [C] [select_sighting.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/select_sighting.mcfunction)
- **Seção 13, tempos de sumiço e intervalo: confirmados** (0,1 / 0,5 / 0,5 / 1 s; some sozinho em 30 s; tabela de intervalos igual). [C] [defaults.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/config/defaults.mcfunction), [roll_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/roll_spawn_timer.mcfunction)
- **Seção 10, "frequência alta" no Him (issue 7): a queixa tinha um defeito por trás.** A configuração do usuário tinha `minSecondsStalk: 5`, e o autor respondeu "I messed up in an earlier version and left it at 5 for testing purposes". [P] [issue 7](https://github.com/Yegiyan/Him/issues/7)
- **Seção 12, Cave Dweller ("heard his 'warning' sounds. I started to think my mod is broken"): o código explica.** O som toca mesmo sem criatura nenhuma existir, bastando o relógio de nascimento ter passado da metade. [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java)
- **Seção 14, Midnight Lurker ("um estágio a cada 20 min"): é o padrão de uma opção (5, 10, 20 ou 30 min), cada vencimento só sobe o estágio em 70% das vezes, e o relógio só começa depois que uma variante específica nasce.** [C] [InsanityStageTimerProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/InsanityStageTimerProcedure.java)
- **Seção 14, The Obsessed ("intensidade sobe ao longo de cerca de 1 hora"): faltava a garantia dos 60 minutos.** [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)
- **Seção 7: confirmado** que Cave Dweller Evolved e Man From The Fog Reimagined estão arquivados no GitHub; o segundo avisa no README que mudou para o Codeberg. [C] [README](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/README.md)

### Inferences
- **From The Fog, sem santuário, não mostra o Herobrine na primeira hora real.** 3 dias de jogo são 72.000 ticks, ou 60 minutos reais se ninguém dormir. Dormir adianta o relógio do mundo, então com sono toda noite o portão abre mais cedo (algo como 35 a 40 minutos; estimativa minha, não medida).
- **Cave Dweller Evolved:** espera média de 0,6 × 450 s + 0,4 × 1.200 s = 750 s, ou 12,5 minutos **de tempo debaixo da terra**, mais uns 10 s do sorteio por tick. O primeiro som chega a partir dos 5 minutos, mesmo quando a criatura só vem aos 20.
- **Man From The Fog Reimagined:** a cada 10 s de noite, 13,2% de nascer de verdade e 19,8% de tocar só o som. Em média, o primeiro som vem em ~30 s de noite e a criatura em ~76 s. A chance de passar a primeira noite inteira (60 tentativas) sem ela é de cerca de 0,02%.
- **Him:** média de 4,5 minutos até a primeira aparição depois do altar. Mas pelo código cerca de 82% das aparições nascem atrás do jogador (ângulo aceito de 70 a 180 graus de cada lado; só a faixa de 70 a 90 fica à frente) e são removidas 2 s depois se ele não se virar. Ou seja, muitas aparições acontecem sem ninguém ver, embora o log do servidor registre todas. Isso é leitura minha do código, não foi testado.
- **TheWatcher:** no escuro total o medo chega a 20 em 20 s (10 s se parado) e a 100 em 100 s (50 s se parado; ~167 s com tocha na mão). De dia, na superfície, o medo cai 2 por segundo e nada acontece nunca.
- **The Hollow** é o único que declara que um jogador cuidadoso pode nunca ver nada, e a página não diz quão rápido o pavor sobe.

### Gaps
- O código de The Broken Script, The Obsessed, The Knocker, The One Who Watches, The Anomaly, The Silence, The Hollow, GoatMan e Don't Let It Learn é fechado: os números deles vêm só das páginas.
- Não li como o Cave Dweller deixa de ser invisível (`CaveDwellerBreakInvisGoal`), nem os pesos dos tipos de assombração do Him (`Haunt.java`), nem as distâncias exatas de cada posição do From The Fog (os valores 3–5, 6–12, 25–46 e 50–100 da pesquisa anterior não foram reconferidos).
- Não confirmei o que `time of minecraft:overworld query time` devolve na 26.2 (tempo total do relógio ou hora do dia); o datapack divide por 24.000 "to get the day", o que só faz sentido com tempo total.
- Não encontrei nada utilizável sobre Understudy, The Mimic/Mimic Dweller nem Sanity: Descent Into Madness para a primeira hora.
- Uma página de perguntas do blog do CurseForge diz que o Cave Dweller só nasce "below Y=0" e com "light level of 0" [B] [blog.curseforge.com](https://blog.curseforge.com/cave-dweller-minecraft-mod-frequently-asked-questions/); isso **contradiz** o código do Evolved (Y até 40, luz do céu até 8) e pode se referir a outra versão. Não abri a página.

---

## 2. Quem garante uma "prova de vida" cedo, e quem depende só de sorteio

### Takeaway
Os mods mais baixados têm uma âncora que não depende de sorte: mensagem falsa de entrada aos 15 minutos (The Broken Script), garantia aos 60 minutos (The Obsessed), ação do próprio jogador que é respondida em segundos (santuário do From The Fog, altar do Him), ou um som que antecede a criatura (Cave Dweller, Man From The Fog). Os que deixam tudo ao acaso ou à cautela do jogador são os que geram perguntas do tipo "está funcionando?".

### Cited Findings
- **Âncora por relógio:** The Broken Script põe o Null na lista de jogadores aos 15 minutos reais, com mensagem de entrada. [P] [wiki, Null](https://thebrokenscript.wiki.gg/wiki/Null)
- **Garantia de último recurso:** The Obsessed força o primeiro alvo se nada tiver acontecido em 60 minutos de mundo, e o valor é configurável. [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)
- **Ação do jogador respondida na hora:** no From The Fog, acender o santuário multiplica a atividade por 6 durante 360 s. [C] [found_shrine.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/shrine/found_shrine.mcfunction)
- **Avistamentos distantes e frequentes depois do portão:** From The Fog, 60 a 120 s de dia e 30 a 60 s de noite. [C] [roll_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/roll_spawn_timer.mcfunction)
- **Som antes da criatura:** Cave Dweller toca o ruído de caverna a partir da metade do relógio de nascimento [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java); Man From The Fog Reimagined toca só o som em 60% dos sorteios que passam [C] [ServerEvents.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/server/ServerEvents.java).
- **Medidor visível:** TheWatcher mostra a barra de medo na tela por padrão. [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java)
- **Presságio com animais:** GoatMan usa cabras que marcam território, encaram ou seguem antes de a criatura aparecer. [P] [Modrinth](https://modrinth.com/mod/goatman)
- **Proteção contra travar:** From The Fog refaz a fila de avistamentos a cada 5 minutos se não houver Herobrine. [C] [5_minutes.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction)
- **Só sorteio ou só cautela do jogador:** Midnight Lurker (40% por tentativa natural, de noite ou em caverna) [C] [MidnightLurkerNaturalEntitySpawningConditionProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/MidnightLurkerNaturalEntitySpawningConditionProcedure.java); The Hollow ("If you play it safe, you will never see this mod.") [P] [Modrinth](https://modrinth.com/mod/hollow-dread); The One Who Watches ("He may not show up every night") [P] [Modrinth](https://modrinth.com/mod/the-one-who-watches).
- **O que os jogadores dizem dos que dependem de sorteio ou de portão longo** (detalhes na pergunta 3): "played about 8 Minecraft days of this mod and haven't seen anything" (The Obsessed) [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1v8qb3y/); "when I have played in the world for more then 3 days, I have not seen him spawn at all" (From The Fog) [P*] [r/FromTheFog](https://www.reddit.com/r/FromTheFog/comments/1ajsjuz/).
- Tamanho de público, só no Modrinth, em 2026-10-10: From The Fog 5,77 milhões de downloads; The Man From The Fog 4,94 milhões; The Broken Script 3,24 milhões; The Knocker 2,59 milhões; Midnight Lurker 1,98 milhão; The One Who Watches 1,52 milhão; The Obsessed 1,22 milhão; Him 136 mil; TheWatcher 4,7 mil; The Silence 2,8 mil; The Hollow 495. [P] página de cada projeto no Modrinth, lida pela API (`api.modrinth.com/v2/project/<slug>`), por exemplo [From The Fog](https://modrinth.com/mod/from-the-fog), [The Broken Script](https://modrinth.com/mod/the-broken-script) e [The Hollow](https://modrinth.com/mod/hollow-dread). Os números não incluem o CurseForge.

### Inferences
- Dá para separar três tipos de prova de vida: **(a) do mundo** (mensagem no chat, som, animais, medidor), **(b) da criatura de longe** (vulto a 40–100 blocos por poucos segundos) e **(c) resposta a uma ação do jogador**. Nenhum dos mods lidos depende do tipo (b) sozinho para os primeiros minutos; os que têm (b) frequente (From The Fog) colocam um portão antes.
- A garantia de 60 minutos do The Obsessed é o caso mais parecido com o problema do Sussurros (55 e 69 minutos sem aparição): o autor tratou "uma hora sem nada" como defeito a ser impedido por regra, não por ajuste de probabilidade.
- A queixa do Reddit sobre The Obsessed (8 dias de jogo, mais de 2 horas reais, sem ver nada) não bate com essa garantia. Pode ser versão antiga, o alvo ter sido travado sem o jogador notar, ou a garantia não funcionar como descrito. Não dá para decidir com o que foi lido.
- O som desacoplado do Cave Dweller funciona como prova de vida, mas a pesquisa anterior já tinha uma queixa de quem ouviu o aviso várias vezes e nunca viu a criatura. Um presságio que nunca é pago vira suspeita de defeito.

### Gaps
- Não achei nenhum mod que documente uma primeira aparição "roteirizada" (fixa, igual para todo mundo). O mais próximo é o Null aos 15 minutos.
- Não sei quanto tempo o pavor do The Hollow leva para chegar a 75; a página dá só "strongly rising" e "slowly rising".

---

## 3. O que os jogadores relatam da primeira sessão

### Takeaway
As queixas de "nada acontece" aparecem em mods com portão longo ou sorteio puro, e vêm acompanhadas de dúvida sobre se o mod está instalado direito. As queixas de "cedo demais" que encontrei são todas sobre encontros que **matam, expulsam do mundo ou se repetem**, e não sobre um avistamento inofensivo.

A amostra é pequena e parcial: o Reddit ficou inacessível, então só li o começo de cada post e nenhum comentário.

### Cited Findings

**"Está funcionando?" / "nada acontece"**
- From The Fog: "How do I know if it's working ... Is there any tell to know if it's working" [P*] [r/FromTheFog](https://www.reddit.com/r/FromTheFog/comments/18wqx34/)
- From The Fog: "when I have played in the world for more then 3 days, I have not seen him spawn at all" [P*] [r/FromTheFog](https://www.reddit.com/r/FromTheFog/comments/1ajsjuz/)
- From The Fog: "i made the shrine 3 times and havent see and appearance i know the mod working because the tree leaves are gone" [P*] [r/FromTheFog](https://www.reddit.com/r/FromTheFog/comments/18nho0j/)
- From The Fog: "on day 54, Herobrine just seemed to dissapear completley" [P*] [r/FromTheFog](https://www.reddit.com/r/FromTheFog/comments/1cd652z/)
- The Obsessed: "I've probably played about 8 Minecraft days of this mod and haven't seen anything and was wondering if that was normal." [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1v8qb3y/)
- The Obsessed: um desenho feito "while waiting for him to spawn in my world" [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1v5191s/)
- Dois mods não identificados: "12 minutes of nothing happening ... Can someone tell me why nothing happened? where they not active or smhn?" [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1wr4cgt/)
- Belowland: "Ive been playing belowland for an hour and i think it might be broken" [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1vp3h8g/)
- The Broken Script: "I think I'm on day 4/5 and it's pretty quiet. I've only had Null Normal, Faraway, and Sub 1, but otherwise nothing ... I'm starting to think it's abnormal" [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1vwf0fe/)
- The Broken Script 2.0: "almost every night there is nothing literally nothing ... someone knows how to raise the events?" [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1vwakui/)
- Cave Dweller Evolved: "the entity does not do anything, no sound effects, does not spawn normally"; o usuário pergunta "If it has something to do with the timer and it taking to long to start, where in the file is that?" e depois "I want to have it spawn frequently and make noises very often". [P] [issue 2](https://github.com/SiverDX/cave_dweller/issues/2)
- Texto de apoio de um modpack, que mostra a pergunta sendo esperada: "If nothing is happening, just wait as the broken script mod takes a while." [B] origem provável, não aberta: [modpackindex](https://www.modpackindex.com/modpack/146704/a-broken-world-the-broken-script)

**"Cedo demais" / "demais"**
- Man From The Fog Reimagined: "an option to prevent spawn for a given period on new worlds would be lovely, as being slaughtered repeatedly on night one is not ideal"; o mesmo usuário diz que muita gente "would prefer the Man(tm) spawn more rarely to keep things fresh and unpredictable". [P] [issue 6](https://github.com/zenolth/man-from-the-fog-reimagined/issues/6)
- The Broken Script (complemento spectrum_11): "got banned from my world as integrity spaened on the first night" [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1w1zhhd/)
- The Broken Script 2.0: "we got banned after an 2 or 3 hrs. We want to start a new world but don't exactly want to invest time to just get banned again." [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1vyhscf/)
- Midnight Lurker: "anytime I kill the Midnight lurker, he spawns again and again and again. There was no cool down." [P] [issue 7](https://github.com/Voxla/midnightlurker/issues/7)
- The Knocker: existe um datapack de terceiros só para reduzir a frequência das placas: "Limits signs placed by 'The Knocker' to one per 20 minutes." [P] [Modrinth](https://modrinth.com/project/the-knocker-sign-limiter)
- Gênero em geral: "tired of horror mods just being 'spooky thing spawns directly behind/near you and kills you one shot or is literally unbeatable'" [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1vq19uj/)
- Gênero em geral: "nothing interesting about a mod that adds a monster that has the same spawn rates as all of the vanilla mobs but they make a loud noise and gives you blindness when you get close" [P*] [r/ihatethissmug](https://www.reddit.com/r/ihatethissmug/comments/1vfr92y/)
- Quem quer montar pacote para amigos: "I can't seem to find anything great that isn't too overbearing ... Subtle or startling. Stuff like From the Fo[g]" [P*] [r/feedthebeast](https://www.reddit.com/r/feedthebeast/comments/1wd7pzb/)

**Primeiro encontro descrito como marcante**
- The Broken Script, dia 2: "I confess I'm very scared, I was playing at day 2 on my recent world and suddenly this message just popped up on the chat and a loud noise played." [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1si1mme/)
- The Broken Script: "its my first time seeing this guy. was collecting sugarcane and looking for cows one night" [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1umi7xo/)
- The Broken Script 2.0, descrição do ritmo normal: "the mod goes like usual. Nothing happens at first, then some of the spooky stuff starts, Null joins the world, etc etc." [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1vvrhb6/)

**Desgaste depois do começo**
- "At a certain point we end up getting bored because ethier it feels repetitive or like there's nothing good to do." [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1vfd981/)
- "due to the repetitive and predictable nature of these mod[s]" [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1v6mvqt/)

### Inferences
- O limite de paciência que aparece nos títulos é bem curto em tempo real: "12 minutes", "an hour", "3 days" (60 min), "day 4/5" (80 a 100 min). Ninguém nesses posts interpreta o silêncio como suspense; todos o leem como possível defeito. Isso vale para jogadores que **sabem** que instalaram um mod de terror, que é o caso do dono e dos amigos.
- As queixas de "cedo demais" que achei têm consequência pesada (morte repetida na primeira noite, expulsão do mundo, reaparecer sem intervalo). Não achei queixa de alguém incomodado por ter **visto** algo inofensivo cedo. Para um mod que nunca mata, o risco de aparecer cedo parece estar mais em gastar a novidade do que em frustrar.
- No Him, o próprio desenho produz "atividade" que o jogador não percebe (ver inferência na pergunta 1): o usuário da issue 7 notou que o Herobrine estava "really active" olhando o console, não o jogo.

### Gaps
- **Nenhum comentário do Reddit foi lido nesta rodada**, e de cada post só o começo. Não sei como a comunidade respondeu a essas perguntas (por exemplo, se alguém disse "é normal, espere").
- Não encontrei vídeos de primeira jogada com tempos marcados (descrição ou comentário fixado do YouTube). Não busquei no YouTube diretamente.
- Não li as seções de comentários do CurseForge nem do Modrinth (o Modrinth não tem comentários; os do CurseForge não vieram na leitura da página).
- Não achei relatos de primeira sessão de The Knocker, The One Who Watches, The Anomaly, The Hollow, TheWatcher, The Silence ou Don't Let It Learn.

---

## 4. Adaptação ao lugar, e se jogar de dia ou numa base iluminada evita tudo

### Takeaway
Vários mods trocam o tipo de aparição conforme o lugar (caverna, superfície, cama, base), mas quase todos têm uma condição que o jogador consegue evitar por completo: só de noite, só no escuro, só abaixo de certa altura. Há crítica nos dois sentidos: fuga fácil demais, e também a criatura (ou o clima) invadindo a base onde o jogador queria sossego.

### Cited Findings
- From The Fog troca o tipo pelo lugar: pedido de "lurking" ou "stalking" com o jogador no subsolo vira "dwelling"; pedido de "dwelling" na superfície vira "stalking"; no Nether é sempre "dwelling"; existe um tipo só para a cama ("nightmare", ao pé da cama, 60 s). [C] [presets/lurking.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/presets/lurking.mcfunction), [presets/dwelling.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/presets/dwelling.mcfunction), [presets/nightmare.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/presets/nightmare.mcfunction)
- From The Fog aparece de dia também (`sighting_window` padrão "always"), só que com metade da frequência. [C] [defaults.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/config/defaults.mcfunction), [set_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/set_spawn_timer.mcfunction)
- The Broken Script tem variantes por lugar e hora: "Mining" no subsolo, "Invade Base" dentro da base, "Watching" e "Flying" só à noite. [P] [wiki, Null](https://thebrokenscript.wiki.gg/wiki/Null)
- The Obsessed: "Lurker mode: Spawns right next to the player when indoors and near their spawnpoint, ideally behind a window." e nasce "in optimal places out of your FOV". [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)
- Don't Let It Learn: observação pela janela se a criatura "already knows a building", e "may appear ahead of the player and wait where it expects them to go". [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dont-let-it-learn)
- Evitável por luz ou hora, por desenho:
  - The Hollow: "If you play it safe, you will never see this mod. It stays completely invisible until you've earned it, no ambient jumpscares fired at a calm player standing in a lit base." [P] [Modrinth](https://modrinth.com/mod/hollow-dread)
  - TheWatcher: de dia com céu aberto o medo cai 2 por segundo; só sobe com luz 3 ou menos, ou parado. [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java)
  - Cave Dweller Evolved: só com Y até 40 e pouca luz do céu; superfície desligada por padrão. [C] [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java)
  - Man From The Fog Reimagined: só de noite; some ao amanhecer. [C] [ServerEvents.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/server/ServerEvents.java), [TheManEntity.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/entity/the_man/TheManEntity.java)
  - Midnight Lurker: o autor fala em "an area with lots of light where the Lurker literally can't spawn". [P] [issue 4](https://github.com/Voxla/midnightlurker/issues/4)
- Him não se adapta a caverna: a aparição é posta no topo do terreno a 42–64 blocos. O autor reconhece a falta: "I definitely want to add a few more spooky events to happen when the player is caving". [C] [Stalk.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/events/Stalk.java); [P] [issue 9](https://github.com/Yegiyan/Him/issues/9)
- Crítica a clima sem ameaça dentro da base: "the ambience playing randomly in the middle of a peaceful farming session in a fortified base ruins the appeal ... it just kills the mood during the wait for him to spawn". O autor mudou o mod para não tocar onde a criatura não pode nascer. [P] [issue 4](https://github.com/Voxla/midnightlurker/issues/4)
- Crítica a estragar a base: "2x2 tunnel straight through my house ... shouldnt be on the surface destroying my builds" [P] [Him, issue 12](https://github.com/Yegiyan/Him/issues/12)
- Jogadores constroem para escapar: "Short watchtower I made to survive ... It doesn't stop the rake from spawning up there sadly but it has shielded me from goatman and mimicer." [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1v9c84v/)
- Há quem peça exatamente uma criatura que só observa: "I want to find a mod where the monster does absolutely nothing but follow you. No attacks, no jumpscares, no dialogue. Just in the distance looking at you." [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1w9gnqe/)

### Inferences
- Para sessões de 1 hora, as condições "só de noite" e "só no subsolo" reduzem muito a janela: numa hora há no máximo três noites, e um jogador que constrói de dia e dorme pode passar a sessão inteira fora da condição. From The Fog é o único dos lidos que aparece de dia por padrão.
- A queixa do Midnight Lurker mostra um custo do som de ambiente como prova de vida: quando toca num lugar onde o jogador sabe que nada pode acontecer, o som deixa de ser lido como ameaça.
- Não achei, nesta rodada, queixa direta do tipo "dá para evitar tudo ficando na base iluminada". As críticas de fuga fácil que existem estão na pesquisa anterior (base no céu, muralha, torre). O que achei aqui é o contrário: autores que assumem a fuga pela luz como regra do jogo (The Hollow) ou que a ampliam a pedido (Midnight Lurker).

### Gaps
- Não sei se The Hollow recebe críticas por ser invisível para quem joga com cuidado: tem só 495 downloads e não achei discussão.
- Não li como The Knocker escolhe entre base, cama e superfície (fechado e sem documentação).

---

## 5. Multiplayer: quem recebe o primeiro encontro num servidor pequeno

### Takeaway
A regra mais comum é "um jogador sorteado, uma criatura para o servidor inteiro", o que divide a frequência pelo número de jogadores. Só um mod lido dá preferência a quem está sozinho, e os mods com medidor por jogador são os únicos em que cada um tem o seu ritmo. Queixas de "um amigo vê tudo e os outros nada" eu **não encontrei** de forma direta.

### Cited Findings
- From The Fog: cada avistamento vai para `@r[gamemode=!spectator]`; pegadas e minerador fantasma também usam `@r`. Existe um Herobrine por vez. [C] [select_sighting.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/select_sighting.mcfunction), [5_minutes.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction)
- From The Fog: o intervalo entre avistamentos não muda com o número de jogadores. [C] [set_spawn_timer.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/spawning_rules/set_spawn_timer.mcfunction)
- Cave Dweller Evolved: uma vítima por dimensão, sorteada entre os jogadores que cumprem a condição; a mesma vítima fica até uma tentativa de nascimento falhar. O som vai para **todos** os jogadores na condição, a menos que se ligue `only_play_noise_to_target` (padrão desligado). [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java), [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java)
- Queixa correspondente: "if one person is being stalked by the Cave Dweller, everyone on the server can hear the noises as if he is right next to you. Makes it too annoying to be usable." [P] [issue 12](https://github.com/SiverDX/cave_dweller/issues/12)
- Man From The Fog Reimagined: o alvo é sorteado com peso `1 − clamp(jogadores no grupo ÷ (total − 1), 0,2, 0,8)`, ou seja, quem está isolado pesa até 4 vezes mais que quem está em grupo. A chance não cresce com o número de jogadores (`spawnChanceScalesWithPlayerCount = false`). [C] [ServerEvents.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/server/ServerEvents.java), [ModConfig.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/config/ModConfig.java)
- Him: jogador aleatório a cada aparição. [C] [Stalk.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/events/Stalk.java)
- The Obsessed: "It can target one player max, or multiple players at a time if enabled in config" e "will lose interest in players who are not present for long periods (configurable)". [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed)
- Por jogador: TheWatcher guarda o medo nos dados de cada jogador [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java); The Hollow diz "Dread is tracked per player, and having someone nearby genuinely calms you both down" (outro jogador a 16 blocos faz o pavor cair) [P] [Modrinth](https://modrinth.com/mod/hollow-dread); no Midnight Lurker o relógio de insanidade começa para "the player closest" [P] [Modrinth](https://modrinth.com/mod/the-midnight-lurker).
- The Broken Script é compatível com multiplayer desde a 1.10.0, mas a wiki não diz quem é o alvo. [P] [wiki, FAQ](https://thebrokenscript.wiki.gg/wiki/Frequently_Asked_Questions)
- Relatos de experiência desigual ou diferente em grupo:
  - "me and my friend have been playing TBS for 2 days now, and like no Void Holes have spawned, I played solo a bit before to make sure the mod was working right and they were spawning" [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1w6y6pe/)
  - "me and my friends made a world to play it about 2 days ago, ended up being VERY fun. while i didnt experience much of the horror aspects, i was transported to some of the liminal worlds" (wonderland.jar) [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1v42t2m/)
- Pedido de dono de servidor que quer esconder o mod dos amigos: "is it required to build a shrine? I'm trying to prank my friend and I think building a herobrine shrine would make it pretty obvious" [P] [Him, issue 17](https://github.com/Yegiyan/Him/issues/17); outro pede "a config where herobrine spawns with the world instead of lighting a shrine" [P] [Him, issue 9](https://github.com/Yegiyan/Him/issues/9).
- Don't Let It Learn ainda não funciona em multiplayer: "Multiplayer-Support in Development". [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dont-let-it-learn)

### Inferences
- No modelo do From The Fog, com 4 amigos online o intervalo **por jogador** fica cerca de 4 vezes maior (de 1–2 minutos para 4–8 minutos de dia), e o primeiro avistamento do servidor cai em um jogador ao acaso. Para sessões de 1 hora ainda dá várias aparições por pessoa, porque a frequência de base é alta.
- No modelo de alvo único (The Obsessed por padrão; Cave Dweller enquanto a vítima não muda), um amigo concentra tudo por desenho. O único mecanismo de correção que vi é o "perde o interesse em quem some" do The Obsessed.
- O peso contra grupos do Man From The Fog Reimagined resolve outro problema (a criatura aparecer no meio do grupo e virar piada), não o de distribuição: quem gosta de explorar sozinho recebe mais.
- Mensagens no chat (o Null entrando, "Herobrine joined the game") são a única prova de vida que chega igual a todos ao mesmo tempo.

### Gaps
- **Não encontrei nenhum tópico em que jogadores reclamem de um amigo ver tudo e os outros nada.** A busca no Reddit foi curta e sem comentários, então a ausência não prova que a queixa não existe.
- Não sei como The Knocker, The One Who Watches e The Broken Script escolhem o alvo em servidor.

---

## 6. Contra-evidência: começos longos e quietos que são elogiados

### Takeaway
O começo lento é ponto de venda declarado dos dois maiores mods do gênero (From The Fog e The Broken Script) e de vários menores. Mas nos dois grandes o período quieto não é vazio: há eventos físicos raros ou um sinal fixo aos 15 minutos. Elogio explícito de jogadores a um começo sem **nada** eu não encontrei; o elogio que achei é a travar criaturas **perigosas** atrás de progresso.

### Cited Findings
- The Broken Script se apresenta como "a slow burn, story focused horror mod" e tem 3,24 milhões de downloads no Modrinth. [P] [Modrinth](https://modrinth.com/mod/the-broken-script)
- O que ele faz durante o começo: Null entra aos 15 minutos e fica na lista de jogadores; a lua "looks and behaves as normal". [P] [wiki, Null](https://thebrokenscript.wiki.gg/wiki/Null), [wiki, The Moon](https://thebrokenscript.wiki.gg/wiki/The_Moon)
- From The Fog (5,77 milhões de downloads no Modrinth) mantém os 3 dias por padrão e, durante eles, já pode deixar pegadas, abrir portas, pôr itens em baús e erguer estruturas. [C] [5_minutes.mcfunction](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction); [P] [Modrinth](https://modrinth.com/mod/from-the-fog)
- Him: "the default values will cause more of a slow burn!" [C] [README.md](https://github.com/Yegiyan/Him/blob/HEAD/README.md)
- The Anomaly Rephased: "meant for long playthroughs, you will not encounter the entity during the first days, make sure to make the most of it." [P] [Modrinth](https://modrinth.com/mod/the-anomaly-rephased)
- The Silence: "replaces traditional horror with slow building dread" e "Best experienced knowing as little as possible." [P] [Modrinth](https://modrinth.com/mod/the-silence-mod)
- The One Who Watches: sem aparição no primeiro ciclo de dia e noite. [P] [Modrinth](https://modrinth.com/mod/the-one-who-watches)
- Elogio de jogador a progressão travada: "The pack being progressive with its horror mobs is nice, instead of being hit with everything right from the start, mobs are locked behind progression so you somewhat can't get one tapped off rip and are given a lot of time to prepare and explore." [P*] [r/HorrorMinecraft](https://www.reddit.com/r/HorrorMinecraft/comments/1vregmb/)
- Pedido de raridade: "a lot of people, myself included, would prefer the Man(tm) spawn more rarely to keep things fresh and unpredictable". [P] [issue 6](https://github.com/zenolth/man-from-the-fog-reimagined/issues/6)
- Jogadores do The Broken Script descrevem o começo quieto como o esperado, sem reclamar: "the mod goes like usual. Nothing happens at first, then some of the spooky stuff starts". [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1vvrhb6/)
- O mesmo mod gera a dúvida oposta poucos dias depois ("Day 4/5 ... I'm starting to think it's abnormal"). [P*] [r/TheBrokenScript](https://www.reddit.com/r/TheBrokenScript/comments/1vwf0fe/)

### Inferences
- "Começo lento" nesses mods é medido em dias de jogo de um mundo que dura semanas ou meses. Em tempo real, os portões são 15 minutos (The Broken Script), 20 minutos (The One Who Watches) e 60 minutos (From The Fog). Num servidor que vive duas semanas com sessões de 1 hora, um portão de 60 minutos consome a primeira sessão inteira, que é justamente o que os tópicos de "não aparece" descrevem.
- O que os começos lentos elogiados seguram é a **ameaça** (perseguição, dano, destruição), não todo sinal. O argumento a favor da espera, nos relatos, é "tempo para se preparar e explorar", que não se aplica a uma criatura que não mata.
- A evidência a favor de "cedo é melhor" é fraca em um ponto: não achei ninguém elogiando um mod **por** ter mostrado a criatura nos primeiros minutos. O que existe é ausência de queixa nos mods que fazem isso sem punir (Him depois do altar, From The Fog depois do santuário) e presença de queixa nos que punem cedo.
- A popularidade não separa as duas escolhas: tanto mods de portão longo quanto mods de primeira noite têm milhões de downloads.

### Gaps
- Não achei análise ou depoimento longo de jogador comparando um começo lento com um rápido no mesmo mod (por exemplo, From The Fog com e sem santuário).
- Não sei quantos jogadores do From The Fog usam o santuário ou mudam o atraso na configuração.

---

## Tabela-resumo

| Mod (versão lida) | Regra da primeira aparição | Primeira consequência para o jogador | Fonte |
|---|---|---|---|
| From The Fog (v1.11.3-26.2) | Portão de 3 dias de jogo (60 min reais sem dormir) ou santuário; depois, avistamento a cada 60–120 s de dia e 30–60 s de noite, some em 30 s ou ao ser mirado. Antes do portão já tenta pegadas, portas, baús e estruturas a cada 5 min | Só a 2 blocos: câmera virada, som de susto, some em 0,5 s. Sem dano | [C] [defaults](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/config/defaults.mcfunction), [5_minutes](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/main/timed_ticks/5_minutes.mcfunction), [jumpscare](https://github.com/LunarEclipseStudios/From-The-Fog/blob/main/data/lunareclipse.watching/function/sightings/sudden_scare/jumpscare.mcfunction) |
| Cave Dweller Evolved (1.6.4, arquivado) | Relógio só anda no subsolo (Y ≤ 40, pouca luz do céu): 300–600 s, 40% de chance de 1.200 s; nasce a 16–35 blocos, invisível; som de caverna a partir de 5 min, mesmo sem criatura; fica 300 s | Olhada: 25% persegue, 50% encara, 25% foge; a 12 blocos persegue. 6 de dano por golpe | [C] [CaveDweller.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/CaveDweller.java), [ServerConfig.java](https://github.com/SiverDX/cave_dweller/blob/HEAD/src/main/java/de/cadentem/cave_dweller/config/ServerConfig.java) |
| Cave Dweller original | 480–1.080 s, com chance de 800–1.600 s; ruído a cada 100–180 s (segundo o dono do fork) | Não lido | [P] [issue 2](https://github.com/SiverDX/cave_dweller/issues/2) |
| Man From The Fog Reimagined (0.6.3, arquivado) | Só de noite; a cada 10 s, 33% de chance; 60% só som, 40% nasce a 20–60 blocos atrás do jogador | Encarado por 4 s: um terço some, um terço persegue, um terço foge; a 15 blocos persegue (30–120 s) | [C] [ModConfig.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/config/ModConfig.java), [ServerEvents.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/server/ServerEvents.java), [StareState.java](https://github.com/zenolth/man-from-the-fog-reimagined/blob/HEAD/src/main/java/dev/zenolth/the_fog/common/state_machine/states/the_man/StareState.java) |
| The Man From The Fog (original) | "appears in the Night and stalks you"; taxa often, normal ou rare | "Eventually it will start to chase you kill you" | [P] [Modrinth](https://modrinth.com/mod/the-man-from-the-fog) |
| Him (HEAD de 2025-03-16) | Nada até acender o altar; depois 60–480 s; a 42–64 blocos, fora do cone de 70° do olhar; recua após 2 s e some quando o jogador desvia | A 24 blocos: Escuridão por 3 s e som; some. Sem dano | [C] [Him.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/Him.java), [Stalk.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/events/Stalk.java), [HerobrineEntity.java](https://github.com/Yegiyan/Him/blob/HEAD/src/main/java/com/him/entities/HerobrineEntity.java) |
| TheWatcher (HEAD de 2026-08-08) | Medo por jogador sobe 1–2 por segundo no escuro ou parado e cai de dia; sons a partir de 20; vulto aos 100, 6–9 blocos à frente e 3–5 ao lado | Vulto some ao ser mirado e se aproxima se ignorado; sem dano. Antes disso: ecos, inventário mexido, tocha quebrada, queda falsa do jogo | [C] [CommonFearSystem.java](https://github.com/Al-Capone11/TheWatcher/blob/HEAD/common/src/main/java/com/josem/thewatcher/game/CommonFearSystem.java) |
| The Hollow / hollow-dread (26.2) | Nada com pavor abaixo de 25; criatura só com 75+, a 14–22 blocos, no escuro, fora do olhar | Se alcança: cegueira, Escuridão e Fraqueza; não mata | [P] [Modrinth](https://modrinth.com/mod/hollow-dread) |
| The Broken Script (1.21.1) | Null entra com mensagem falsa aos 15 min reais; mais entidades "a few nights" depois; chance ligada à fase da lua | Varia: de "Watching" inofensivo a "Chase" com 10 corações por golpe | [P] [wiki, Null](https://thebrokenscript.wiki.gg/wiki/Null), [wiki, FAQ](https://thebrokenscript.wiki.gg/wiki/Frequently_Asked_Questions) |
| The Obsessed | "Targeter" nasce ao acaso; cruzar a linha de visão dele trava o alvo; garantia forçada aos 60 min de mundo; pico de aparições uma hora depois | Não documentado na parte lida | [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed) |
| The Midnight Lurker | De noite ou em caverna; 40% por tentativa natural no padrão; "waits for the player to find them" | Olhar espanta; chegar perto sem olhar faz atacar por ~1 min. Insanidade sobe a cada 20 min (70%) | [C] [LurkerconfigProcedure.java](https://github.com/Voxla/midnightlurker/blob/HEAD/src/main/java/net/mcreator/midnightlurker/procedures/LurkerconfigProcedure.java); [P] [Modrinth](https://modrinth.com/mod/the-midnight-lurker) |
| Eyes in the Darkness | Sem atraso; ciclo a cada 150 ticks à noite; até 2 por jogador, a até 64 blocos | Susto com veneno nível 1 | [C] [ConfigData.java](https://github.com/gigaherz/EyesInTheDarkness/blob/HEAD/src/main/java/dev/gigaherz/eyes/config/ConfigData.java) |
| The Legend of Herobrine | Só depois de ativar o altar (`HerobrineAlwaysSpawns: false`); espião a 32 blocos | Não lido | [C] [herobrine.json](https://github.com/Alex-MacLean/TheLegendOfHerobrine/blob/HEAD/src/main/resources/data/herobrine/default_config/herobrine.json) |
| The One Who Watches | Não nasce no primeiro ciclo de dia e noite; "may not show up every night" | "CAN kill you but doesn't HAVE to" | [P] [Modrinth](https://modrinth.com/mod/the-one-who-watches) |
| The Anomaly Rephased | "you will not encounter the entity during the first days" | Não documentado | [P] [Modrinth](https://modrinth.com/mod/the-anomaly-rephased) |
| The Silence | Sombra que segue por "multiple in game days"; 6 fases | Sem sustos nem barra de vida; itens somem, túmulos aparecem | [P] [Modrinth](https://modrinth.com/mod/the-silence-mod) |
| The Knocker | Sem documentação; "knows where you live", "may visit you while sleeping" | Ataca "after a few nights" | [P] [Modrinth](https://modrinth.com/mod/the-knocker); [B] [Sportskeeda](https://www.sportskeeda.com/minecraft/what-minecraft-the-knocker-mod) |
| GoatMan (3.0) | Presságios com cabras (território, encarar, seguir) antes da criatura; sem números | Persegue, luta ou foge; "Abduction" em vez de matar sempre | [P] [Modrinth](https://modrinth.com/mod/goatman) |
| Don't Let It Learn (0.1.0-alpha.2) | Cinco tipos de encontro; nenhum número publicado | Só observa | [P] [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dont-let-it-learn) |

---

## O que continua sem verificação

1. **Reddit ficou inacessível e eu não contornei o bloqueio.** O Reddit devolveu tela de login, o front-end Redlib que respondeu tem verificação contra robôs, o arquivo Arctic Shift estava fora do ar (erro 522) e o Pullpush, depois de duas consultas, respondeu que não oferece raspagem gratuita para agentes. Parei ali. Todos os itens [P*] são título e começo do texto, **sem comentários e sem pontuação confiável** (o arquivo mostra quase tudo com 1 ponto). Não dá para dizer qual opinião é majoritária.
2. **Nenhum vídeo do YouTube foi consultado.** Não há tempos de primeira jogada vindos de vídeo.
3. **Mods fechados:** os números de The Broken Script, The Obsessed, The Hollow, The One Who Watches, The Anomaly, The Silence, The Knocker, GoatMan e Don't Let It Learn vêm só das páginas; não há como conferir no código.
4. **Wiki e CurseForge foram lidas por uma ferramenta que resume a página.** As citações (15 minutos do Null, 60 minutos do The Obsessed, 0,85% da lua, durações das variantes do Null) vêm como a ferramenta devolveu; vale abrir as páginas antes de usar um número desses como base de decisão.
5. **Contradição não resolvida:** a garantia de 60 minutos do The Obsessed contra o relato de 8 dias de jogo sem ver nada.
6. **Contradição não resolvida:** o texto do blog do CurseForge sobre o Cave Dweller ("below Y=0", "light level of 0") contra o código do Evolved.
7. **Trechos de código não lidos:** como o Cave Dweller sai da invisibilidade; pesos dos tipos de assombração do Him; distâncias de cada posição do From The Fog; a outra tabela de chances do From The Fog (a de 1 em 100 citada na pesquisa anterior); o padrão de `eerie_entrance` e de `fearful_footsteps`; o efeito de "Give Him Control" sobre o atraso inicial.
8. **Semântica de comando na 26.2:** o que `time of minecraft:overworld query time` devolve (tempo total ou hora do dia). A conta dos 60 minutos depende de ser tempo total.
9. **Inferências minhas sem teste:** a estimativa de que ~82% das aparições do Him passam sem ser vistas; os 35–40 minutos do From The Fog com sono; as médias de espera calculadas (12,5 min, 76 s, 4,5 min).
10. **Versões:** o que foi lido é o HEAD de cada repositório em 2026-10-10. Cave Dweller Evolved e Man From The Fog Reimagined estão arquivados no GitHub (o segundo continua no Codeberg, que não foi lido), então as versões em uso podem ter números diferentes. Não confirmei o número de versão do Him nem do TheWatcher além do que o código e as issues citam.
11. **Sem informação útil:** Understudy, The Mimic/Mimic Dweller, Sanity: Descent Into Madness, Cave Dweller Reimagined e a versão em mod (não datapack) do From The Fog.
12. **Multiplayer:** não achei queixa direta de distribuição desigual entre amigos; a ausência pode ser só efeito da busca curta.
