# ▌Flux de l'aplicació

[← Tornar a l'índex](../INDEX.md)

---

## ▌Flux complet

1. `App.main()` crea el controlador de l'aplicació
2. `ResourcePreloader` carrega els recursos estàtics de partida
3. es resol el mode de joc
4. `GameBootstrap.createGame(...)` crea els dos personatges
5. es tria el terreny segons els ajustos d'usuari
6. s'apliquen efectes de mode, terreny, Caos i opcions de debug
7. `GameLoop.init()` entra al bucle de combat
8. cada ronda obté dues accions i les envia a `CombatSystem.play(...)`
9. el combat acaba quan hi ha vencedor o empat

---

## ▌Mostra de codi real

```java
TerrainDefinition effectiveTerrain = terrain == null ? selectTerrain() : terrain;

ModeEffectApplier.apply(effectiveMode.rules(), p1, p2);
TerrainEffectApplier.apply(effectiveTerrain, p1, p2);

boolean chaosActive = ChaosPolicy.apply(effectiveMode, p1, p2, new Random());
MatchContext matchContext = new MatchContext(effectiveMode, chaosActive, effectiveTerrain);
```

Aquest fragment mostra que el joc separa clarament:

- creació de personatges
- selecció de context de partida
- aplicació d'efectes globals
- entrada posterior al bucle de combat

---

## ▌On mirar primer si alguna cosa falla

- problema abans d'entrar al joc → `App`, `GameBootstrap`, `AppConfigLoader`
- problema en crear personatges → `CharacterCreator`
- problema en seleccionar o aplicar terreny → `TerrainSelectionScreen`, `TerrainRegistry`, `TerrainEffectApplier`
- problema en una ronda → `CombatSystem`, `TurnResolver`, `AttackResolver`
- problema en habilitats/armes → `Weapon`, `AttackRegistry`, `PassiveFactory`
