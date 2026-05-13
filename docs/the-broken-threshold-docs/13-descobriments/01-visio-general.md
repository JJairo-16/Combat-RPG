# ▌Sistema de descobriments

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

El sistema de descobriments manté un grimori global del contingut que el jugador ha vist o activat.

A diferència dels assoliments, els descobriments no mesuren objectius. La seva funció és revelar fitxes del catàleg a mesura que el jugador entra en contacte amb armes, races, accions, efectes, missions, perks o sinergies.

---

## ▌Classes clau

- `discovery/DiscoverySystem.java`
- `discovery/DiscoveryRuntime.java`
- `discovery/DiscoveryCategory.java`
- `discovery/DiscoveryKey.java`
- `discovery/DiscoveryProgress.java`
- `discovery/config/DiscoveryCatalog.java`
- `discovery/config/DiscoveryCatalogLoader.java`
- `discovery/config/DiscoveryEntryDefinition.java`
- `discovery/persistence/DiscoveryStore.java`
- `discovery/ui/DiscoveryInteractiveViewer.java`

---

## ▌Fitxers externs

- catàleg manual: `data/discoveryCatalog.json`
- ruta configurada: `paths.discoveryCatalogConfig`
- desament: `paths.discoverySaveFile`

El catàleg descriu què es pot mostrar. El desament registra què s'ha descobert.

---

## ▌Categories

Les categories principals són:

- `BREEDS`
- `WEAPONS`
- `ACTIONS`
- `EFFECTS`
- `MISSIONS`
- `PERKS`
- `DIVINE_PERKS`
- `SYNERGIES`

Cada categoria pot tenir títol, descripció, ordre i política de mostrar entrades bloquejades.

---

## ▌Flux general

1. es construeix el catàleg final
2. es carrega el progrés desat
3. `DiscoveryRuntime` queda configurat amb el sistema actiu
4. el joc crida `discover(category, id)` quan passa un fet rellevant
5. si l'entrada existeix al catàleg, es marca com descoberta
6. el visor mostra fitxa completa o pista segons l'estat

---

## ▌Responsabilitats

- `DiscoveryCatalog`: defineix totes les entrades possibles
- `DiscoverySystem`: coordina progrés i persistència
- `DiscoveryRuntime`: accés estàtic per descobrir des de sistemes desacoblats
- `DiscoveryStore`: desa i carrega el progrés
- UI de descobriments: transforma el catàleg i el progrés en una vista navegable
