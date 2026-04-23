# ▌Flux de l'aplicació

[← Tornar a l'índex](../INDEX.md)

---

## ▌Flux complet

1. `App.main()` crea `App`
2. `App.run()` executa bootstrap amb `SafeExecutor`
3. `GameBootstrap.createGame()` carrega config i recursos
4. Es creen els dos personatges
5. `GameLoop.init()` entra al bucle de combat
6. Cada ronda obté dues accions i les envia a `CombatSystem.play(...)`
7. El combat acaba quan hi ha vencedor o empat

---

## ▌Mostra de codi real

```java
public void run() {
    ExecutionReport bootstrapReport = executor.run("Bootstrap", () -> {
        bootstrap = new GameBootstrap();
        game = bootstrap.createGame();
    });

    if (endIfCrashed(bootstrapReport))
        return;

    ExecutionReport report = executor.run("RPG Combat", () -> game.init());
    endIfCrashed(report);
}
```

Aquest fragment mostra que el joc separa clarament:

- fase de construcció
- fase d'execució
- tractament d'errors

---

## ▌On mirar primer si alguna cosa falla

- problema abans d'entrar al joc → `App`, `GameBootstrap`, `AppConfigLoader`
- problema en crear personatges → `CharacterCreator`
- problema en una ronda → `CombatSystem`, `TurnResolver`, `AttackResolver`
- problema en habilitats/armes → `Weapon`, `AttackRegistry`, `PassiveFactory`
