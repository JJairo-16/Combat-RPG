# ▌Passives i fases

[← Tornar a l'índex](../INDEX.md)

---

## ▌Interfície clau

`weapons/passives/WeaponPassive.java`

És una interfície orientada a fases del combat.

---

## ▌Fases disponibles

- `START_TURN`
- `BEFORE_ATTACK`
- `ROLL_CRIT`
- `MODIFY_DAMAGE`
- `BEFORE_DEFENSE`
- `AFTER_DEFENSE`
- `AFTER_HIT`
- `END_TURN`

---

## ▌Dispatcher real

```java
default String onPhase(Weapon weapon, HitContext ctx, Random rng, Phase phase) {
    return switch (phase) {
        case START_TURN -> startTurn(weapon, ctx, rng);
        case BEFORE_ATTACK -> beforeAttack(weapon, ctx, rng);
        case ROLL_CRIT -> rollCrit(weapon, ctx, rng);
        case MODIFY_DAMAGE -> modifyDamage(weapon, ctx, rng);
        case BEFORE_DEFENSE -> beforeDefense(weapon, ctx, rng);
        case AFTER_DEFENSE -> afterDefense(weapon, ctx, rng);
        case AFTER_HIT -> afterHit(weapon, ctx, rng);
        case END_TURN -> endTurn(weapon, ctx, rng);
    };
}
```

---

## ▌Conclusió pràctica

Si vols una passiva nova, normalment només has de decidir **en quina fase viu**.

És un dels punts més extensibles del projecte.
