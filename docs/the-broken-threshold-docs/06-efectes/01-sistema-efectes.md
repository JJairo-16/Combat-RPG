# ▌Sistema d'efectes

[← Tornar a l'índex](../INDEX.md)

---

## ▌Nucli

`models/effects/Effect.java`

Els efectes tenen estat propi i també funcionen per fases, igual que les passives.

---

## ▌Característiques del model

Cada efecte defineix:

- `key()`
- `priority()`
- `stackingRule()`
- `maxCharges()`
- `maxStacks()`
- `state()`
- comportament per fase

---

## ▌Mostra de codi real

```java
default StackingRule stackingRule() {
    return StackingRule.REPLACE;
}
```

Això mostra que l'efecte no només executa lògica, sinó que defineix com conviu amb altres efectes iguals.

---

## ▌Implementacions detectades

A `models/effects/impl/` hi ha, entre altres:

- `BlindEffect`
- `Exhaustion`
- `Fatigue`
- `MagicalTiredness`
- `PoisonEffect`
- `SpiritualCallingFlag`
- `SuddenDeathPoisonEffect`

---

## ▌Quan usar un efecte i no una passiva?

- si necessita estat propi persistent
- si té cooldown, càrregues o stacks
- si ha de viure al personatge i no a l'arma
