```md
# `AttackResolver` i `RoundRecoveryService`

## `AttackResolver`

Aquesta classe té dues feines.

### 1. Aplicar el dany segons l'acció defensiva

```java
public Result resolveAttack(double damage, Character target, Action targetAction)
```

Implementació actual:

- si `targetAction == DODGE` -> `target.dodge(damage)`
- si `targetAction == DEFEND` -> `target.defend(damage)`
- en qualsevol altre cas:
  - si `damage <= 0`, retorna `new Result(-1, "")`
  - si no, aplica `target.getDamage(damage)`

### 2. Decidir objectiu real del cop

```java
public Character chooseTarget(Character attacker, Character defender, AttackResult attackResult)
```

Regla:

- `Target.ENEMY` o `null` -> objectiu = defensor
- qualsevol altre target actual -> objectiu = atacant

## Conseqüència important

El sistema d'autoatac o self-cast està molt simplificat:

- actualment només diferencia entre "enemic" i "atacant"
- si algun dia vols més objectius, hauràs d'ampliar `Target`, `AttackResolver` i probablement el model general del combat

---

## `RoundRecoveryService`

Gestiona el bonus de regeneració de final de round associat a una bona defensa.

### `registerDefenseBonus(...)`

```java
public void registerDefenseBonus(
    Action defenderAction,
    Result defenderResult,
    double incomingDamage,
    EndRoundRegenBonus bonus)
```

Regles actuals:

- si no hi ha bonus o el dany entrant és 0 o menys, no fa res
- si l'acció és `DODGE` i el dany rebut és exactament 0, afegeix:
  - `+0.005` a la vida percentual
  - `+0.01` al mana percentual
- si l'acció és `DEFEND` i el dany rebut és menor que el dany entrant, afegeix:
  - `+0.004` a la vida percentual
  - `+0.01` al mana percentual

### `applyEndRoundBonus(...)`

Converteix aquests percentatges en quantitat absoluta:

```java
double hpAmount = stats.getMaxHealth() * bonus.bonusHealthPct();
double manaAmount = stats.getMaxMana() * bonus.bonusManaPct();
```

Després usa:

- `stats.heal(hpAmount)`
- `stats.restoreMana(manaAmount)`

## Idea de disseny

Hi ha una separació entre:

- **regeneració natural** del personatge (`Character.regen`)
- **premi per bon joc defensiu** (`RoundRecoveryService`)

Això et permet extendre una sense tocar l'altra.

## Com extendre aquesta part

Pots afegir noves fonts de recuperació:

- bonus per crític
- bonus per matar enemic
- bonus per no rebre dany al round
- penalització si defenses massa rounds seguits
```
