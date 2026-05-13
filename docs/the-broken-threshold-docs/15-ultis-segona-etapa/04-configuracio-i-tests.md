# ▌Configuració i proves

[← Tornar a l'índex](../INDEX.md)

---

## ▌Fitxers implicats

Les ultis de segona etapa depenen de diverses peces del projecte:

- `data/menuDescription.json`
- `data/menuModifiers.json`
- `data/achievements.json`
- `data/discoveryCatalog.json`
- `game/modifier/Actions.java`
- `game/modifier/ui/Messages.java`
- `game/modifier/ultimate/UltimateActionType.java`
- `game/modifier/ultimate/UltimateActionEffect.java`
- `game/modifier/ultimate/UltimateChargeBoost.java`
- `game/modifier/ultimate/UltimateActionUnlocks.java`

Els JSON defineixen presentació, descobriments i assoliments. El codi controla disponibilitat, activació i integració amb combat.

---

## ▌menuDescription.json

Aquest fitxer conté les descripcions que veu el jugador en el menú.

Les ultis hi han d'aparèixer com accions especials, amb una descripció curta que expliqui la seva funció sense revelar tots els detalls interns.

La descripció ha de deixar clar que són accions de ruptura associades a un atac carregat.

---

## ▌menuModifiers.json

Aquest fitxer connecta efectes interns amb opcions de menú.

Per cada ulti, la configuració associa:

- clau de l'efecte intern
- etiqueta visible
- identificador de descobriment
- acció a executar
- prioritat

El modificador només afegeix l'opció si l'efecte és actiu i el predicat de disponibilitat retorna cert.

---

## ▌achievements.json

Els assoliments es divideixen en dos grups.

Assoliments visibles de progrés:

- `ARCANE_INITIATE`
- `PHYSICAL_INITIATE`
- `EAGLE_EYE`

Assoliments ocults d'ús:

- `ARCANE_OVERLOAD_RELEASED`
- `COLOSSAL_BREAK_RELEASED`
- `ELVEN_OPENING_RELEASED`
- `THREE_HIDDEN_RUPTURES`

Els primers serveixen per desbloquejar. Els segons registren que el jugador ha trobat i utilitzat les ruptures secretes.

---

## ▌discoveryCatalog.json

Les ultis també han de tenir entrada de descobriment.

Això permet que el grimori o wiki mostri pistes, nom bloquejat o informació completa segons el progrés del jugador.

Com que són accions especials, les seves entrades han d'estar alineades amb la categoria d'accions i amb l'estil de les altres accions del menú.

---

## ▌Tests principals

El test principal de la mecànica ha de validar:

- les ultis no apareixen si no estan desbloquejades
- apareixen quan hi ha `CHARGE` descobert i l'assoliment correcte
- no apareixen sense atac carregat
- no apareixen durant cooldown inicial
- `Carregar atac` desapareix quan la càrrega està activa
- `Carregar atac` torna quan la ulti consumeix la càrrega
- cada ulti valida el seu tipus d'arma
- `Tret èlfic d'obertura` valida raça i destresa
- `Sobrecàrrega arcana` valida mana mínim
- només es pot usar una ulti per combat
- l'activació retorna `ATTACK`
- els boosts modifiquen el dany o crític esperat
- els esdeveniments d'assoliment es generen correctament

---

## ▌Proves visuals

Per fer proves visuals sense haver de completar el progrés global, es pot activar un bypass de desbloqueig només en entorns de test o debug.

També es pot desactivar temporalment el cooldown inicial per comprovar l'aparició immediata de les opcions.

Aquestes opcions no han de formar part de la configuració normal de partida. Han de quedar limitades a proves manuals, tests o codi de debug clarament identificat.

---

## ▌Precaucions

En modificar aquesta mecànica cal evitar:

- aplicar dany directament des de l'acció de menú
- permetre més d'una ulti per combat
- permetre usar una ulti sense consumir càrrega
- deixar el boost actiu després d'un cop
- mostrar una ulti bloquejada per progrés
- fer que `Trencament colossal` requereixi estamina o resistència mínima
- fer que els assoliments ocults revelin la mecànica abans d'hora

El principi és que el menú prepara la ruptura, però el combat continua resolent l'atac.

---
