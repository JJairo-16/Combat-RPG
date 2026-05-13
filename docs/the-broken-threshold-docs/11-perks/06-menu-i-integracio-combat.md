# ▌Menú i integració amb combat

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/CombatPerkSystem.java`
- `perks/PlayerPerkState.java`
- `perks/PerkChoiceMenu.java`
- `perks/PerkRegistry.java`

---

## ▌Estat per jugador

`PlayerPerkState` guarda:

- missió activa
- progrés
- perks actives
- elecció pendent

---

## ▌Elecció de perk

Quan una missió es completa:

```java
state.updatePendingChoice();
```

Això activa el menú de selecció.

---

## ▌Menú

`PerkChoiceMenu` mostra opcions al jugador.

Funcions:

- renderitzar perks
- gestionar selecció
- confirmar elecció

---

## ▌Integració amb combat

`CombatPerkSystem`:

- escolta esdeveniments
- actualitza missions
- executa efectes de perks

---

## ▌Execució d’efectes

Durant el combat:

```java
effect.apply(context);
```

---

## ▌Flux complet

1. combat genera esdeveniment
2. missions s’actualitzen
3. possible completat
4. menú de perks
5. selecció
6. efecte registrat
7. execució en triggers

---

## ▌Notes

- integració desacoblada via esdeveniments
- sistema escalable i modular