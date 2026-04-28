# ▌Status menu modifiers

[← Tornar a l'índex](../INDEX.md)

---

## ▌Paquet

`game/modifier/`

---

## ▌Què resol?

Aquest sistema permet que el menú d'accions canviï dinàmicament segons l'estat del personatge.

No és un efecte de combat directe; és un **efecte sobre la interfície i les opcions disponibles**.

---

## ▌Peces principals

- `StatusMod`
- `StatusModFactory`
- `StatusModLoader`
- `StatusModActionRegistry`
- `MenuStatusModifier`

---

## ▌Exemple real de registre

```java
private static final Map<String, MenuAction<Action, Character>> ACTIONS = Map.of(
        "spiritualCalling", Actions::spiritualCalling,
        "bloodPact", Actions::bloodPact
);
```

---

## ▌Lectura arquitectònica

Això fa que les accions contextuals del menú estiguin dirigides per:

- configuració
- claus (`actionKey`)
- disponibilitat
- estat del personatge

---

## ▌Quan tocar aquest paquet

- si vols afegir una nova opció contextual de menú
- si una acció només ha d'aparèixer sota certes condicions
- si vols carregar comportament de menú des de JSON
