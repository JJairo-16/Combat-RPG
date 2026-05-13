# ▌Catàleg i entrades de descobriment

[← Tornar a l'índex](../INDEX.md)

---

## ▌Catàleg final

`DiscoveryCatalog` construeix un catàleg final combinant dues fonts:

1. entrades automàtiques generades des dels registres del joc
2. overrides manuals definits a `discoveryCatalog.json`

Això evita duplicar dades bàsiques i alhora permet personalitzar textos, pistes i ordre.

---

## ▌Entrades automàtiques

El catàleg genera entrades per contingut registrat:

- armes des d'`Arsenal.values()`
- races des de `Breed.values()`
- perks des de `PerkRegistry`
- perks divines des de `DivinePerkRegistry`
- missions des de `MissionRegistry`
- accions des d'`Action.values()`
- sinergies des de `SynergyRegistry`

Les entrades automàtiques donen una base funcional encara que no hi hagi cap override manual.

---

## ▌Overrides manuals

`discoveryCatalog.json` pot modificar una entrada automàtica o crear-ne una de nova.

Els camps habituals són:

- `category`
- `id`
- `title`
- `lockedTitle`
- `shortDescription`
- `description`
- `discoveredWhen`
- `hint`
- `tags`
- `hiddenUntilDiscovered`
- `sortOrder`

Quan l'entrada automàtica ja existeix, l'override només substitueix els camps declarats.

---

## ▌Entrades bloquejades

Una entrada no descoberta pot mostrar-se o ocultar-se segons:

- `hiddenUntilDiscovered` de l'entrada
- `showLockedEntries` de la categoria

Si es mostra bloquejada, el visor usa:

- `lockedTitle`
- `hint`

Si ja està descoberta, usa:

- `title`
- `shortDescription`
- `description`
- `discoveredWhen`
- `hint`

---

## ▌Pistes personalitzades

Les pistes han de ser escrites per entrada quan el contingut ho necessiti.

En armes, és especialment important evitar pistes genèriques com si totes estiguessin disponibles des de l'inici. Si una arma forma part del sistema de desbloqueig, la pista ha d'explicar el camí sense revelar-la completament.

---

## ▌Bones pràctiques

- no canviar l'`id` d'una entrada ja publicada
- usar `sortOrder` per mantenir categories llegibles
- mantenir `lockedTitle` curt
- escriure pistes evocadores, no descripcions completes
- crear overrides manuals per contingut important encara que existeixi entrada automàtica
