# ▌Sistema d'assoliments

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

El sistema d'assoliments registra progrés global del jugador a partir d'esdeveniments del joc.

No forma part del combat com a mecànica directa: observa què passa, actualitza objectius i desa el resultat perquè el progrés es mantingui entre partides.

---

## ▌Classes clau

- `achievements/AchievementSystem.java`
- `achievements/AchievementUpdate.java`
- `achievements/AchievementEvent.java`
- `achievements/AchievementProgress.java`
- `achievements/config/AchievementDefinition.java`
- `achievements/config/AchievementObjective.java`
- `achievements/config/AchievementRegistry.java`
- `achievements/config/AchievementLoader.java`
- `achievements/persistence/AchievementStore.java`

---

## ▌Fitxers externs

- definicions: `data/achievements.json`
- ruta configurada: `paths.achievementsConfig`
- desament: `paths.achievementSaveFile`

El fitxer de definicions descriu els assoliments disponibles. El fitxer de desament només guarda el progrés obtingut pel jugador.

---

## ▌Flux general

1. es carrega `achievements.json`
2. es construeix l'`AchievementRegistry`
3. `AchievementSystem.load(...)` recupera el progrés desat
4. el joc emet `AchievementUpdate` durant combat, equipament o final de partida
5. cada `AchievementProgress` comprova si l'actualització encaixa amb el seu objectiu
6. si hi ha canvis, es marca el sistema com a brut
7. el progrés es desa automàticament

---

## ▌Responsabilitats

- `AchievementSystem`: coordinació, entrada d'esdeveniments i persistència
- `AchievementProgress`: càlcul del progrés d'un assoliment concret
- `AchievementUpdate`: paquet d'esdeveniments i camps del moment actual
- `AchievementDefinition`: definició immutable carregada des de JSON
- `AchievementStore`: lectura i escriptura del progrés persistent

---

## ▌Notes

- els assoliments són globals, no depenen d'una partida concreta
- un mateix `AchievementUpdate` pot activar diversos assoliments
- els assoliments poden observar accions, armes, resultats, danys, modes especials i estat del combat
- el sistema està pensat per afegir contingut nou des de JSON sempre que ja existeixi l'esdeveniment necessari
