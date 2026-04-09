```md
# Exemple d'implementació: arma nova amb efecte persistent

Aquest exemple mostra una manera coherent d'extendre el projecte actual.

## Objectiu

Crear una llança que:

- fa un atac normal
- si impacta, aplica un efecte de sagnat de 3 turns
- si el rival es defensa, fa una mica més de dany

## 1. Efecte `BleedEffect`

```java
package rpgcombat.models.effects.custom;

import java.util.Random;

import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.weapons.passives.HitContext;

public final class BleedEffect implements Effect {
    private final EffectState state = new EffectState(0, 1, 3, 0);

    @Override
    public String key() {
        return "BLEED";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        state.refreshDuration(3);
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng) {
        ctx.defender().getStatistics().damage(8);
        state.tickDuration();
        return new EffectResult("L'hemorràgia infligeix 8 de dany.");
    }
}
```

## 2. Passiva d'arma per aplicar l'efecte

```java
public static WeaponPassive applyBleedOnHit() {
    return new WeaponPassive() {
        @Override
        public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
            ctx.defender().addEffect(new BleedEffect());
            return ctx.defender().getName() + " comença a sagnar.";
        }
    };
}
```

## 3. Skill de la llança

```java
public static AttackResult ruptureSpear(Weapon weapon, Statistics stats, Random rng) {
    double damage = weapon.basicAttack(stats, rng);
    return new AttackResult(damage, "clava la llança amb una estocada profunda.");
}
```

## 4. Entrada nova a `Arsenal`

```java
RUPTURE_SPEAR(
    "Llança de Ruptura",
    "Una llança que provoca sagnat després d'un impacte.",
    72, 0.14, 1.36,
    PHYSICAL,
    Skills::ruptureSpear,
    Passives.antiGuard(),
    Passives.applyBleedOnHit()
)
```

## 5. Resultat funcional

Sense tocar `CombatSystem` ni `TurnResolver`, has afegit:

- arma nova
- skill nova
- passiva nova
- efecte persistent nou

Això és exactament el tipus d'extensió que l'arquitectura actual facilita millor.
```
