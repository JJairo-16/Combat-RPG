# ▌Bootstrap i arrencada

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe clau

La classe central és `GameBootstrap`.

La seva feina és:

<<<<<<< HEAD
- carregar `appConfig.json`
- precarregar armes, menú d'armes i modificadors
- carregar la configuració de balance
- crear els personatges
=======
- rebre la configuració i els recursos ja precarregats
- crear els personatges segons el mode
- resoldre el terreny segons els ajustos d'usuari
- aplicar efectes de mode, terreny i Caos
>>>>>>> develop
- aplicar opcions de debug
- retornar un `GameLoop`

---

## ▌Flux intern

```java
<<<<<<< HEAD
public GameLoop createGame() {
    loadAppConfig();
    preloadResources();
    ensureModifiersLoaded();

    Character p1 = createCharacter(config.characters().player1());
    clearBetweenCharactersIfNeeded();
    Character p2 = createCharacter(config.characters().player2());

    applyDebugOptionsIfNeeded(p1, p2);

    return new GameLoop(p1, p2, modifiers);
}
```

=======
Character p1 = createCharacter(config.characters().player1(), creationOptions);
clearBetweenCharactersIfNeeded();
Character p2 = createCharacter(config.characters().player2(), creationOptions);
TerrainDefinition effectiveTerrain = terrain == null ? selectTerrain() : terrain;

p1.setSpecialActionsEnabled(effectiveMode.rules().specialActionsEnabled());
p2.setSpecialActionsEnabled(effectiveMode.rules().specialActionsEnabled());
ModeEffectApplier.apply(effectiveMode.rules(), p1, p2);
TerrainEffectApplier.apply(effectiveTerrain, p1, p2);
```

La selecció de terreny queda entre la creació dels personatges i l'inici real de la partida:

```java
private TerrainDefinition selectTerrain() {
    TerrainSelectionMode mode = UserSettingsRuntime.terrainSelectionMode();
    return switch (mode) {
        case NONE -> TerrainRegistry.none();
        case RANDOM -> TerrainRegistry.randomPlayable(new Random());
        case MANUAL -> TerrainSelectionScreen.choose(TerrainRegistry.all());
    };
}
```

Si el mode de selecció no és `MANUAL`, no s'obre cap pantalla interactiva de terrenys.

>>>>>>> develop
---

## ▌Preload important

<<<<<<< HEAD
Durant `preload()` es fan tres coses crítiques:
=======
Durant `ResourcePreloader.preloadMatchStatic(...)` es preparen recursos crítics:
>>>>>>> develop

- `Arsenal.preload(...)`
- `StatusModLoader.load(...)`
- `CombatBalanceRegistry.initialize(...)`
<<<<<<< HEAD

Això implica que **armes, modifiers i balance** han d'estar disponibles abans que comenci la partida.
=======
- `MissionRegistry`, `PerkRegistry`, `DivinePerkRegistry` i `SynergyRegistry`
- `TerrainRegistry.initialize(...)`

Això implica que **armes, modifiers, balance, perks i terrenys** han d'estar disponibles abans que comenci la partida.
>>>>>>> develop

---

## ▌Què passa si falta configuració?

Si `appConfig.json` no es pot llegir, s'utilitza configuració per defecte amb `AppConfigLoader.defaultConfig()`.

<<<<<<< HEAD
Això és útil per arrancar ràpid, però pot ocultar errors si s'espera una configuració personalitzada.
=======
Si falta el catàleg de terrenys, `TerrainLoader` conserva l'opció neutra:

```java
if (path == null || !Files.exists(path)) {
    return List.of(TerrainDefinition.none());
}
```

Això permet arrencar sense terrenys jugables, però manté el flux de partida coherent.
>>>>>>> develop
