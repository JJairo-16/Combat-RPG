# ▌Motor genèric d'objectius

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

El motor d'assoliments no té lògica específica per a cada assoliment. Cada entrada de `achievements.json` declara quin tipus d'objectiu vol observar i amb quines condicions.

Això permet crear assoliments nous sense afegir classes noves, sempre que es puguin expressar amb els tipus d'objectiu existents.

---

## ▌Estructura d'un assoliment

Cada assoliment defineix:

- `id`: identificador estable
- `name`: nom visible
- `description`: explicació visible
- `goal`: objectiu numèric principal
- `visibility`: control de visibilitat abans de completar-lo
- `objective`: regla que fa avançar el progrés

---

## ▌Tipus d'objectiu

Els tipus principals són:

- `COUNT_EVENT`: compta ocurrències d'un esdeveniment
- `SUM_VALUE`: suma un valor numèric d'un camp
- `CONDITIONAL_COUNT`: compta només si es compleixen condicions
- `CONDITIONAL_SUM`: suma només si es compleixen condicions
- `CONSECUTIVE_EVENT`: exigeix repeticions consecutives
- `AVOID_EVENT_FOR_TURNS`: avança mentre no passi un esdeveniment
- `ACTION_SEQUENCE`: segueix una seqüència concreta d'accions
- `STATE_REACHED`: completa quan s'arriba a un estat
- `STATE_MAINTAINED`: manté progrés mentre un estat continua actiu
- `ALL_UNIQUE_VALUES`: exigeix veure tots els valors d'una llista
- `EACH_UNIQUE_VALUE_COUNT`: exigeix comptadors separats per valor
- `MAX_VALUE_REACHED`: completa quan un camp supera un llindar

---

## ▌Condicions

Les condicions permeten filtrar una actualització per camps.

Exemples d'ús habitual:

- comprovar si `weaponId` és una arma concreta
- comprovar si `chaosMode` és actiu
- comprovar si `damage` supera un mínim
- comprovar si `ownerAction` o `opponentAction` encaixen amb una acció

Les condicions no generen progrés per si soles. Només decideixen si una actualització és vàlida per a l'objectiu.

---

## ▌Valors únics

Els objectius basats en valors únics fan servir:

- `uniqueField`
- `requiredValues`

Això és útil per assoliments com:

- utilitzar totes les armes conegudes
- guanyar amb cada arma
- usar totes les armes en mode caòtic

Quan s'afegeixen armes noves al joc, aquests assoliments s'han d'actualitzar perquè la seva llista continuï representant el catàleg real.

---

## ▌Persistència del progrés

`AchievementProgress` desa:

- progrés numèric general
- índex de seqüència
- estat completat
- data de completat
- progrés per valor únic
- progrés de seqüència per actor

Això permet que objectius de col·lecció i seqüències sobrevisquin entre execucions.
