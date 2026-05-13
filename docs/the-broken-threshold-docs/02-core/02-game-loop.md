# ▌Game loop

[← Tornar a l'índex](../INDEX.md)

---

## ▌Responsabilitat

`GameLoop` controla el bucle principal del combat fins que hi ha un guanyador.

---

## ▌Flux principal

```java
public void init() {
    cls.clear();

    Winner winner;
    do {
        Action action1 = menu.playPlayer1();
        Action action2 = menu.playPlayer2();

        cls.clear();
        winner = combatSystem.play(action1, action2);
        Menu.pause();
    } while (winner == Winner.NONE);

    finish(winner);
}
```

---

## ▌Què no fa `GameLoop`

`GameLoop` **no** calcula el combat. Només:

- demana accions
- delega la resolució a `CombatSystem`
- mostra el final

Això és important perquè la lògica real de combat queda desacoblada del flux d'entrada.

---

## ▌Canvi d'arma dins del loop

La classe també encapsula el canvi d'arma amb `WeaponMenu`, aplicant filtres i comprovant si l'arma és equipable segons les estadístiques del personatge.

---

## ▌Detall arquitectònic interessant

En el constructor es genera la preferència divina de la partida:

```java
DivineCharismaAffinity.rollForRun(new Random());
```

Això vol dir que la partida sencera comparteix un únic perfil diví.
