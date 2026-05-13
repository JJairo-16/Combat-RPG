# ▌Persistència i visualització d'assoliments

[← Tornar a l'índex](../INDEX.md)

---

## ▌On es desa

La ruta de desament es defineix a `appConfig.json`:

- `paths.achievementSaveFile`

`AchievementStore` resol aquesta ruta dins l'espai persistent de l'aplicació i carrega o desa el progrés global.

Si la ruta és relativa, es desa dins el directori d'aplicació resolt per `AppDataPaths`:

- Windows: `%APPDATA%/The Broken Threshold`
- Linux amb `XDG_DATA_HOME`: `$XDG_DATA_HOME/the-broken-threshold`
- Fallback: `~/.the-broken-threshold`

Durant la càrrega, si el fitxer nou encara no existeix, es pot llegir l'antiga ubicació (`RPGCombat`, `rpgcombat` o `.rpgcombat`) per conservar progrés anterior al canvi de nom. Les noves escriptures van a la ubicació actual.

---

## ▌Què es desa

El desament no replica tota la definició de l'assoliment. Només desa l'estat necessari:

- `id`
- progrés numèric
- índex de seqüència
- completat o no
- data de completat
- valors únics ja obtinguts
- progrés de seqüència per actor

Les definicions continuen venint de `data/achievements.json`.

---

## ▌Càrrega

Durant l'arrencada:

1. es llegeixen les definicions
2. es carreguen les dades persistents
3. si falta progrés per algun assoliment nou, es crea buit
4. si existeix progrés antic, es restaura

Aquest comportament permet afegir assoliments nous sense esborrar el progrés existent.

---

## ▌Visualització

`AchievementSystem.toViewModels()` converteix el progrés intern en models visuals.

La UI no necessita conèixer el motor d'objectius. Només rep:

- nom
- descripció
- progrés visible
- objectiu visible
- regles de visibilitat

---

## ▌Visibilitat

Cada assoliment pot controlar si mostra el nom i la descripció abans de completar-se.

Això permet assoliments:

- totalment visibles
- parcialment ocults
- secrets fins que es completen

---

## ▌Canvis que requereixen revisió

Cal revisar `achievements.json` quan:

- s'afegeix una arma nova
- s'afegeix una acció especial nova
- s'afegeix un mode de combat nou
- es canvia l'identificador d'una arma, perk, efecte o acció
- es modifica el significat d'un camp usat per condicions
