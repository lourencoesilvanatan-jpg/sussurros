# Gravações de terceiros usadas pelos sons

Até a 0.9.0-alpha26 todo som novo do mod era sintetizado. A voz da caçada (`voz_perto`, `voz_longe`, desde a 0.9.0-alpha27) é a exceção: o dono pediu, em 10/10/2026, que as vozes viessem de "gravações livres e bibliotecas de sons". Elas saem destas três gravações, trabalhadas por `gerar_sons.py` (mais graves, mais lentas, ásperas, e as de longe abafadas e com sala). O script lê os arquivos daqui; rodar de novo gera os mesmos sons.

Todas são **CC0 1.0** (domínio público, sem exigência de crédito), conferido na página de cada uma em 16/10/2026. O crédito fica aqui assim mesmo, porque o repositório é público. Os arquivos são as prévias de alta qualidade que o Freesound serve sem login (`-hq.ogg`), sem nenhuma mudança; o `sha1` é o do arquivo como foi baixado.

| arquivo | título no Freesound | autor | página | licença | sha1 |
|---|---|---|---|---|---|
| `freesound_257358.ogg` | Male Aggressive Growls | Kodack | https://freesound.org/people/Kodack/sounds/257358/ | CC0 1.0 | `6363c8f341b8b54c5d6e4b8d0dcbe1e5cd359547` |
| `freesound_556976.ogg` | Male Screams [1] | marc3122 | https://freesound.org/people/marc3122/sounds/556976/ | CC0 1.0 | `43583422fc60e72570b13ab1d8dc120615768c84` |
| `freesound_173944.ogg` | human male scream multi | JohnsonBrandEditing | https://freesound.org/people/JohnsonBrandEditing/sounds/173944/ | CC0 1.0 | `06e948b7865a793909dfae721c24991f1e5d6634` |

Quais trechos de cada uma viram qual som está em `gerar_voz` (no script). Para trocar uma gravação: baixe a nova para cá, anote a linha acima (título, autor, página, licença conferida na página, sha1) e mude os trechos no script. Só entra gravação CC0; MIT, CC-BY e parecidas pedem aviso de terceiros, e CC-BY-NC ou "todos os direitos reservados" não entram.
