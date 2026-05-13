# ▌Sistema de menús

[← Tornar a l'índex](../INDEX.md)

---

## ▌Peces principals

- `MenuBuilder`
- `MenuCenter`
- `MenuStatusModifier`
- llibreria externa `dynamic-menu`

---

## ▌Com es construeix el menú base

`MenuBuilder` defineix les opcions estàndard:

- canviar arma
- accions de combat (`Action.values()`)
- veure informació

```java
private static void buildActions(DynamicMenu<Action, Character> menu) {
    Action[] actions = Action.values();
    for (Action action : actions) {
        menu.addOption(action.label(), currentPlayer -> MenuResult.returnValue(action));
    }
}
```

---

## ▌Menús per jugador

`MenuCenter` crea un menú fill per a cada jugador a partir d'un menú base compartit.

A més, guarda un snapshot base del menú i després hi aplica modificacions dinàmiques segons l'estat del personatge.

---

## ▌Per què és important?

Això fa possible que opcions com la **crida espiritual** o altres accions contextuals apareguin o desapareguin segons:

- efectes actius
- stacks
- cooldown
- disponibilitat

Sense haver de reescriure el menú cada torn.
