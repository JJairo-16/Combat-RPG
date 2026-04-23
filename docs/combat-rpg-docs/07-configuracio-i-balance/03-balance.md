# ▌Balance de combat

[← Tornar a l'índex](../INDEX.md)

---

## ▌Paquet

`balance/` i `balance/config/`

---

## ▌Configuració arrel

`CombatBalanceConfig` agrupa:

- stamina
- momentum
- attackDefenseVariance
- guardBreak
- adrenaline
- chargedAttack
- antiStall
- bloodPact

```java
public record CombatBalanceConfig(
    StaminaConfig stamina,
    MomentumConfig momentum,
    AttackDefenseVarianceConfig attackDefenseVariance,
    GuardBreakConfig guardBreak,
    AdrenalineConfig adrenaline,
    ChargedAttackConfig chargedAttack,
    AntiStallConfig antiStall,
    BloodPactConfig bloodPact
) {}
```

---

## ▌On s'utilitza?

Diverses classes del domini consulten el registre global de balance, per exemple:

- `Character`
- `Statistics`
- `CombatSystem`
- `TurnResolver`

---

## ▌Bon patró del projecte

Les regles globals numèriques no estan hardcodejades només en una classe; hi ha una capa específica de balance.
