# ▌Bootstrap i arrencada

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe clau

La classe central és `GameBootstrap`.

La seva feina és:

- rebre la configuració i els recursos ja precarregats
- crear els personatges segons el mode
- resoldre el terreny segons els ajustos d'usuari
- aplicar efectes de mode, terreny i Caos
- aplicar opcions de debug
- retornar un `GameLoop`

---

## ▌Flux intern

```java
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

---

## ▌Preload important

Durant `ResourcePreloader.preloadMatchStatic(...)` es preparen recursos crítics:

- `Arsenal.preload(...)`
- `StatusModLoader.load(...)`
- `CombatBalanceRegistry.initialize(...)`
- `MissionRegistry`, `PerkRegistry`, `DivinePerkRegistry` i `SynergyRegistry`
- `TerrainRegistry.initialize(...)`

Això implica que **armes, modifiers, balance, perks i terrenys** han d'estar disponibles abans que comenci la partida.

---

## ▌Què passa si falta configuració?

Si `appConfig.json` no es pot llegir, s'utilitza configuració per defecte amb `AppConfigLoader.defaultConfig()`.

Si falta el catàleg de terrenys, `TerrainLoader` conserva l'opció neutra:

```java
if (path == null || !Files.exists(path)) {
    return List.of(TerrainDefinition.none());
}
```

Això permet arrencar sense terrenys jugables, però manté el flux de partida coherent.
