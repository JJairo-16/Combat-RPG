```md
# Sistema d'efectes i passives

## Dues famílies diferents

El projecte té **dues capes d'extensió** que sovint semblen similars, però no són exactament el mateix.

### 1. `WeaponPassive`
Passives lligades a una arma concreta.

- viuen dins `Weapon`
- s'executen via `weapon.triggerPhase(...)`
- no tenen estat propi persistent per defecte
- són ideals per a comportaments de l'arma

### 2. `Effect`
Efectes associats a un personatge.

- viuen a `Character.effects`
- tenen `EffectState`
- es poden apilar, refrescar o reemplaçar
- són ideals per a buffs, debuffs i estats persistents

---

## `EffectPipeline`

`EffectPipeline` no té estat intern. És un executor.

### `runPhase(...)`

Ordre d'execució:

1. efectes de l'atacant
2. efectes del defensor
3. passives de l'arma

```java
attacker.triggerEffects(ctx, phase, attackerRng, out);
defender.triggerEffects(ctx, phase, defenderRng, out);

if (weapon != null) {
    weapon.triggerPhase(ctx, attackerRng, phase, out);
}
```

Això és molt important perquè defineix la precedència real.

### `runAttackerOnly(...)`

Només dispara efectes de l'atacant. Es fa servir a `START_TURN` i `END_TURN`.

---

## `WeaponPassive`

És una interfície amb mètodes default per fase.

```java
default String modifyDamage(Weapon weapon, HitContext ctx, Random rng) { return null; }
```

Retorna un `String` opcional per al log.

### Quan usar una passiva

Fes servir `WeaponPassive` si l'efecte:

- depèn de l'arma
- no necessita durar múltiples turns
- vol actuar sobre el cop actual
- ha d'estar encapsulat dins l'arma

---

## `Effect`

És una interfície més rica.

### Mètodes clau

- `key()`
- `priority()`
- `stackingRule()`
- `maxCharges()`
- `maxStacks()`
- `state()`
- `mergeFrom(Effect incoming)`
- `onPhase(...)`

### `EffectState`

Guarda:

- càrregues
- stacks
- turns restants
- cooldown

### Caducitat

Per defecte, `Effect.isExpired()` mira principalment càrregues. Tot i així, `EffectState` també té helpers per duració. Això indica que el sistema està preparat per a efectes amb duració, però la política final la decideix l'efecte o el contenidor.

### `EffectResult`

Permet retornar missatges o canvis visibles des d'un `Effect`.

### `StackingRule`

El projecte contempla:

- `IGNORE`
- `REPLACE`
- `REFRESH`
- `STACK`

## Exemple real existent: `ConstantDamageEffect`

Hi ha una plantilla a `models/effects/templates/ConstantDamageEffect.java`. Serveix com a model per veure com implementar un efecte amb estat propi i comportament per fase.

## Passives actuals a `Passives.java`

### `lifeSteal(pct)`
Després de `AFTER_HIT`, cura l'atacant segons `ctx.damageDealt()`.

### `trueHarm(pct)`
Després de `AFTER_HIT`, aplica dany extra directe a la vida màxima del defensor.

### `executor(thresholdLife, damageBonus)`
A `MODIFY_DAMAGE`, si la vida relativa del defensor està per sota d'un llindar, multiplica el dany.

## Quan triar `Effect` o `WeaponPassive`

### Usa `WeaponPassive` si...
- el comportament és inherent a l'arma
- no necessita estat persistent entre rounds
- no ha de sobreviure a canvis d'arma o target

### Usa `Effect` si...
- és un buff/debuff de personatge
- necessita stacks, duració o cooldown
- s'ha d'aplicar encara que canviï l'arma
- vols reutilitzar-lo fora del sistema d'armes
```
