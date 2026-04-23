# ▌Pipeline d'efectes

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`combat/models/EffectPipeline.java`

---

## ▌Funció

Executa una fase concreta sobre:

- efectes de l'atacant
- efectes del defensor
- passives de l'arma

```java
attacker.triggerEffects(ctx, phase, attackerRng, out);
defender.triggerEffects(ctx, phase, defenderRng, out);

if (weapon != null) {
    weapon.triggerPhase(ctx, attackerRng, phase, out);
}
```

---

## ▌Idees importants

### ▌Ordre
Primer personatges, després arma.

### ▌Sortida
La pipeline rep una llista `out` on acumula missatges de log.

### ▌Context compartit
Tot treballa sobre `HitContext`, que serveix de bus de dades de la interacció.

---

## ▌Valor arquitectònic

Aquest és el punt que permet que moltes mecàniques noves s'afegeixin de forma composable, sense inserir `if` per tot arreu.
