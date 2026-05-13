# ▌Persistència i visor de descobriments

[← Tornar a l'índex](../INDEX.md)

---

## ▌On es desa

La ruta de desament es defineix a `appConfig.json`:

- `paths.discoverySaveFile`

`DiscoveryStore` resol aquesta ruta dins l'espai persistent de l'aplicació i desa el progrés global.

Si la ruta és relativa, es desa dins el directori d'aplicació resolt per `AppDataPaths`:

- Windows: `%APPDATA%/The Broken Threshold`
- Linux amb `XDG_DATA_HOME`: `$XDG_DATA_HOME/the-broken-threshold`
- Fallback: `~/.the-broken-threshold`

Durant la càrrega, si el fitxer nou encara no existeix, es pot llegir l'antiga ubicació (`RPGCombat`, `rpgcombat` o `.rpgcombat`) per conservar progrés anterior al canvi de nom. Les noves escriptures van a la ubicació actual.

---

## ▌Què es desa

El desament guarda les entrades descobertes, no tot el catàleg.

La clau estable és:

- categoria
- identificador

Això permet que el catàleg canviï o creixi sense duplicar dades descriptives dins el fitxer de progrés.

---

## ▌Construcció de la vista

`DiscoverySystem.toOverview()` combina:

- categories del catàleg
- entrades del catàleg
- progrés desat

El resultat és un `DiscoveryOverview` amb categories i entrades preparades per renderitzar.

---

## ▌Entrada no descoberta

Quan una entrada encara no està descoberta, el visor mostra:

- títol bloquejat
- estat no descobert
- pista

No mostra la descripció completa.

---

## ▌Entrada descoberta

Quan una entrada ja està descoberta, el visor mostra:

- títol real
- descripció curta
- detall complet
- condició de descobriment
- pista o nota complementària

---

## ▌Filtres i lectura

La UI de descobriments separa el model de dades del renderitzat.

Això facilita:

- filtrar per categoria
- navegar per entrades bloquejades i descobertes
- mostrar progrés per categoria
- canviar el layout sense tocar la persistència
