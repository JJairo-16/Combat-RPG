# ▌Bootstrap i arrencada

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe clau

La classe central és `GameBootstrap`.

La seva feina és:

- carregar `appConfig.json`
- precarregar armes, menú d'armes i modificadors
- carregar la configuració de balance
- crear els personatges
- aplicar opcions de debug
- retornar un `GameLoop`

---

## ▌Flux intern

```java
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

---

## ▌Preload important

Durant `preload()` es fan tres coses crítiques:

- `Arsenal.preload(...)`
- `StatusModLoader.load(...)`
- `CombatBalanceRegistry.initialize(...)`

Això implica que **armes, modifiers i balance** han d'estar disponibles abans que comenci la partida.

---

## ▌Què passa si falta configuració?

Si `appConfig.json` no es pot llegir, s'utilitza configuració per defecte amb `AppConfigLoader.defaultConfig()`.

Això és útil per arrancar ràpid, però pot ocultar errors si s'espera una configuració personalitzada.
