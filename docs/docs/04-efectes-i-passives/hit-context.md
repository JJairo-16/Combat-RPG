```md
# `HitContext`: el context central del cop

## Per què existeix

Sense `HitContext`, cada passiva o efecte hauria de conèixer massa detalls del motor. El context crea un espai compartit i mutable perquè totes les capes del combat puguin llegir i modificar l'estat del cop.

## Què conté

### Participants
- `attacker`
- `defender`
- `weapon`
- `rng`

### Accions
- `attackerAction`
- `defenderAction`

### Resultat base
- `attackResult`

### Dany
- `baseDamage`
- llista de modificadors plans
- llista de multiplicadors

### Crític
- `criticalChance`
- `criticalMultiplier`
- `criticalForced`
- `criticalForbidden`
- `criticalResolved`
- `critical`

### Resultat final
- `defenderResult`
- `damageDealt`

### Metadades flexibles
- `Map<String, Object> meta`

### Esdeveniments
- `EnumSet<Event> events`

## Fases disponibles

L'enum `Phase` defineix l'ordre del pipeline:

1. `START_TURN`
2. `BEFORE_ATTACK`
3. `ROLL_CRIT`
4. `MODIFY_DAMAGE`
5. `BEFORE_DEFENSE`
6. `AFTER_DEFENSE`
7. `AFTER_HIT`
8. `END_TURN`

## Esdeveniments disponibles

- `ON_CRIT`
- `ON_HIT`
- `ON_DAMAGE_DEALT`
- `ON_DAMAGE_TAKEN`
- `ON_DODGE`
- `ON_DEFEND`
- `ON_KILL`

## Operacions habituals que faran les teves passives o efectes

### Modificar dany pla

```java
ctx.addFlatDamage(15.0);
```

### Multiplicar dany

```java
ctx.multiplyDamage(1.20);
```

### Forçar o prohibir crític

El context ja té camps per a això. Segons els mètodes disponibles, la teva passiva pot intervenir abans de `resolveCritical()`.

### Guardar metadades

```java
ctx.putMeta("BLEED_STACKS", 3);
```

### Consultar esdeveniments

```java
if (ctx.hasEvent(HitContext.Event.ON_HIT)) { ... }
```

## Idea de càlcul del dany final

El context separa:

- dany base
- bonus plans
- multiplicadors
- crític
- resultat defensiu final

Això evita haver de recalcular-ho tot a cada passiva.

## Recomanació de disseny

Quan afegeixis mecàniques noves, prioritza tocar el `HitContext` i el pipeline abans que posar lògica ad hoc a `CombatSystem`.
```
