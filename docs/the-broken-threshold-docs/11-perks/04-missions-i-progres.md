# ▌Missions i progrés

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/mission/MissionDefinition.java`
- `perks/mission/MissionLoader.java`
- `perks/mission/MissionRegistry.java`
- `perks/mission/MissionProgress.java`
- `perks/mission/MissionUpdate.java`
- `perks/mission/MissionEvent.java`
- `perks/mission/ObjectiveType.java`

---

## ▌Què és una missió?

Una missió és un objectiu temporal assignat a un jugador durant el combat.

Quan es completa:

- es marca com a finalitzada
- es desbloqueja una recompensa (perk)

---

## ▌Progrés

`MissionProgress` guarda:

- valor actual
- objectiu
- estat de completat

---

## ▌Actualització

Els esdeveniments del joc generen `MissionEvent`.

Aquests es transformen en actualitzacions:

```java
progress.apply(update);
```

---

## ▌Tipus d’objectius

Definits per `ObjectiveType`:

- kills
- dany
- accions específiques

---

## ▌Registre

`MissionRegistry` conté totes les missions disponibles.

---

## ▌Cicle de vida

1. assignació
2. actualització per esdeveniments
3. completat
4. notificació al sistema de perks

---

## ▌Notes

- desacoblament total entre combat i missions
- sistema extensible via configuració