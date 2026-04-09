```md
# Guia pràctica per extendre el projecte

Aquest apartat està pensat perquè puguis implementar contingut nou sense coneixement previ del projecte.

## 1. Afegir una arma nova

### Passos mínims

1. implementar o reutilitzar una `Attack`
2. opcionalment crear passives `WeaponPassive`
3. afegir una entrada nova a `Arsenal`

### Exemple

```java
NEW_WEAPON(
    "Martell tempesta",
    "Una arma pesada que guanya dany si el rival s'ha defensat.",
    90, 0.10, 1.50,
    PHYSICAL,
    Skills::stormHammer,
    40,
    Passives.executor(0.25, 0.20)
)
```

### On tocar

- `models/weapons/Skills.java`
- `models/weapons/Arsenal.java`
- opcionalment `models/weapons/passives/Passives.java`

## 2. Afegir una skill nova

### Patró recomanat

```java
public static AttackResult stormHammer(Weapon weapon, Statistics stats, Random rng) {
    double manaCost = weapon.getManaPrice();
    if (manaCost > 0 && !stats.consumeMana(manaCost)) {
        return new AttackResult(0, "intenta invocar la tempesta, però no té prou mana.");
    }

    double damage = weapon.basicAttack(stats, rng);
    damage *= 1.25;

    return new AttackResult(damage, "descarrega el martell tempesta.");
}
```

### Bones pràctiques

- si consumeix mana, comprova-ho abans
- retorna sempre un `AttackResult`
- no apliquis directament `defend`, `dodge` ni `getDamage`
- no intentis accedir a `HitContext` des de la skill: encara no existeix en aquest moment

## 3. Afegir una passiva d'arma nova

### Exemple

```java
public static WeaponPassive antiGuard() {
    return new WeaponPassive() {
        @Override
        public String beforeDefense(Weapon weapon, HitContext ctx, Random rng) {
            if (ctx.defenderAction() == Action.DEFEND) {
                ctx.multiplyDamage(1.30);
                return ctx.attacker().getName() + " trenca la guàrdia del rival.";
            }
            return null;
        }
    };
}
```

### Quan s'aplicarà
A la fase `BEFORE_DEFENSE`.

## 4. Afegir un efecte persistent nou

### Exemple conceptual: `BleedEffect`

```java
public final class BleedEffect implements Effect {
    private final EffectState state = new EffectState(0, 1, 3, 0);

    @Override
    public String key() {
        return "BLEED";
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        state.refreshDuration(3);
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng) {
        ctx.defender().getStatistics().damage(12);
        state.tickDuration();
        return new EffectResult("L'hemorràgia infligeix 12 de dany.");
    }
}
```

### On s'aplica

Has de cridar `character.addEffect(...)` en algun punt del combat, per exemple des d'una passiva `AFTER_HIT`.

## 5. Afegir una nova acció de combat

Això és més profund, perquè l'enum `Action` actual només té:

- `ATTACK`
- `DEFEND`
- `DODGE`

Per afegir una quarta acció, hauràs de tocar:

- `Action.java`
- `GameLoop.playTurn(...)`
- `DefaultTurnPriorityPolicy`
- `TurnResolver`

### Recomanació
Si la nova acció és només una variant ofensiva, sol ser millor mantenir `Action.ATTACK` i representar la diferència a l'arma o skill.

## 6. Afegir una raça nova

Hauràs de tocar:

- `Breed.java`
- crear una subclasse de `Character` si vols comportament especial
- `CharacterCreator.convert(...)` perquè pugui instanciar-la

## 7. Afegir una nova fase del pipeline

Això és una extensió estructural.

Hauràs de tocar:

- `HitContext.Phase`
- `Effect.onPhase(...)`
- `WeaponPassive.onPhase(...)`
- `TurnResolver` per inserir la nova fase en l'ordre correcte

### Quan val la pena fer-ho
Només si cap de les fases actuals et serveix. Sovint és millor reutilitzar:

- `BEFORE_ATTACK`
- `ROLL_CRIT`
- `MODIFY_DAMAGE`
- `BEFORE_DEFENSE`
- `AFTER_HIT`

## 8. Quina és la manera més segura d'extendre el projecte

Ordre recomanat de menor a major impacte:

1. nova entrada a `Arsenal`
2. nova skill a `Skills`
3. nova passiva a `Passives`
4. nou `Effect`
5. nova política de prioritat
6. nova raça
7. nova `Action`
8. nova fase del pipeline
```
