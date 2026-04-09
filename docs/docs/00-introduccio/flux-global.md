```md
# Flux global: des de `main` fins al final d'un round

## 1. Arrencada

El punt d'entrada és `App.main`.

```java
public static void main(String[] args) {
    App app = new App();
    app.run();
}
```

`App.run()` tria entre:

- `newDebugGame()`: crea dos personatges de debug
- `newGame()`: llança introducció, creació interactiva i combat real

## 2. Creació del `GameLoop`

`GameLoop` rep dos `Character`.

```java
public GameLoop(Character player1, Character player2) {
    this.player1 = player1;
    this.player2 = player2;
    this.combatSystem = new CombatSystem(player1, player2);
}
```

## 3. Bucle principal de combat

`GameLoop.init()` fa això:

```java
do {
    Action action1 = playTurn(player1);
    Action action2 = playTurn(player2);

    winner = combatSystem.play(action1, action2);
    Menu.pause();
} while (winner == Winner.NONE);
```

Per tant, cada iteració del loop és un **round**.

## 4. Què pot fer un jugador en un torn

`playTurn(player)` mostra un menú amb:

- canviar arma
- atacar
- defensar-se
- esquivar
- veure informació

Només quan tria `ATTACK`, `DEFEND` o `DODGE` es retorna una `Action`.

## 5. Execució del round a `CombatSystem`

El round real passa dins `CombatSystem.play(a1, a2)`:

1. crida `playRound(a1, a2)`
2. renderitza el resultat dels dos torns
3. mostra resum de dany rebut
4. si no hi ha vencedor, mostra regeneració
5. retorna `Winner`

## 6. Flux intern exacte de `playRound`

```text
guardar vida inicial P1/P2
crear bonus de regeneració de final de round
decidir qui va primer amb TurnPriorityPolicy
resoldre primer torn amb TurnResolver
resoldre segon torn amb TurnResolver
calcular dany rebut per cada jugador
comprovar guanyador
si no hi ha vencedor:
    guardar vida/mana pre-regeneració
    cridar player.regen() de cada jugador
    aplicar bonus de regeneració per defensa/esquiva
    construir RegenResult
retornar CombatRoundResult
```

## 7. Aspecte important: el segon torn sempre es resol

El codi actual resol els dos torns abans de comprovar guanyador. Això implica:

- si el primer cop deixa el rival a 0, el segon torn igualment pot intentar-se
- el guanyador es resol després dels dos torns
- això permet empats si tots dos queden derrotats al mateix round

## 8. Resum mental del projecte

Pensa el sistema així:

- `GameLoop` = UI i interacció
- `CombatSystem` = director del round
- `TurnResolver` = motor d'un cop
- `HitContext` = estat temporal del cop
- `Weapon` + `Skills` = dany base i habilitat
- `EffectPipeline` = passives i efectes per fases
- `Character` = lloc on s'apliquen esquiva, defensa, vida i efectes
```
