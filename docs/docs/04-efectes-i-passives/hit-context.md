# `HitContext`: el context mutable del cop

## Per què existix

`HitContext` centralitza l'estat d'un atac mentre es resol.

Sense aquest objecte, cada passiva hauria de rebre massa informació separada o acoblar-se al motor de combat. Amb el context, totes les capes poden llegir i modificar el mateix estat del cop.

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
- modificadors plans acumulats
- multiplicadors acumulats

### Crític

- `criticalChance`
- `criticalMultiplier`
- `criticalForced`
- `criticalForbidden`
- `criticalResolved`
- `critical`

### Resultat defensiu i aplicació real

- `defenderResult`
- `damageDealt`

### Metadades flexibles

- `Map<String, Object> meta`

### Esdeveniments

- `EnumSet<Event> events`

## Fases disponibles

L'ordre de fases definit a `HitContext.Phase` és:

1. `START_TURN`
2. `BEFORE_ATTACK`
3. `ROLL_CRIT`
4. `MODIFY_DAMAGE`
5. `BEFORE_DEFENSE`
6. `AFTER_DEFENSE`
7. `AFTER_HIT`
8. `END_TURN`

Aquest ordre és important perquè les passives fan el despatx segons aquestes fases.

## Esdeveniments disponibles

A `HitContext.Event` hi ha actualment:

- `ON_CRIT`
- `ON_HIT`
- `ON_DAMAGE_DEALT`
- `ON_DAMAGE_TAKEN`
- `ON_DODGE`
- `ON_DEFEND`
- `ON_KILL`

El context permet marcar-los i consultar-los durant la resolució.

## Dany base i modificadors

### `setBaseDamage(double)`
Fixa el dany base inicial del cop.

### `addFlatDamage(double)`
Afig un bonus o penalització plana.

### `multiplyDamage(double)`
Afig un multiplicador. Només accepta valors positius.

### `flatDamageBonus()`
Suma tots els modificadors plans.

### `damageMultiplier()`
Multiplica tots els multiplicadors acumulats.

### `damageToResolve()`
Calcula el dany final abans de defensa:

```java
double d = (baseDamage + flatDamageBonus()) * damageMultiplier();
return Math.max(0, round2(d));
```

Això és important perquè separa clarament:

- dany base
- modificadors plans
- multiplicadors
- defensa posterior

## Sistema de crític

### Configuració

El context pot rebre:

- `setCriticalChance(...)`
- `setCriticalMultiplier(...)`

I també pot forçar o prohibir el crític:

- `forceCritical()`
- `forbidCritical()`

### Resolució

`resolveCritical()` aplica aquesta política:

1. si el crític està forçat, és crític
2. si està prohibit, no és crític
3. si no, es tira `rng.nextDouble() < criticalChance`

Si hi ha crític:

- multiplica `baseDamage` per `criticalMultiplier`
- registra l'esdeveniment `ON_CRIT`
- guarda `CRIT = true` a les metadades

A més, el mètode només resol una vegada; si es torna a cridar, reutilitza el resultat ja decidit.

## Resultat real després de defensar

### `setDefenderResult(...)`
Guarda el resultat defensiu del cop.

### `setDamageDealt(...)`
Guarda el dany real aplicat després de defensa.

### `damageDealt()`
És el valor que després usen passives com `lifeSteal(...)`.

## Objectiu del colp

### `target()`

Retorna l'objectiu definit per l'`AttackResult`.

Si encara no n'hi ha cap, assumix `Target.ENEMY`.

Això permet que el pipeline sàpia si el cop va cap a l'enemic o cap al mateix atacant.

## Metadades

Les metadades servixen per guardar informació auxiliar sense haver d'afegir un camp nou cada vegada.

### Operacions disponibles

- `putMeta(String key, Object value)`
- `getMeta(String key)`
- `getMeta(String key, Class<T> type, T def)`

Exemples útils:

- `CRIT`
- comptadors de colps
- claus internes d'una skill
- marques temporals d'una passiva

## Esdeveniments

### `registerEvent(Event)`
Marca que un esdeveniment ha ocorregut.

### `hasEvent(Event)`
Permet consultar-lo.

### `events()`
Retorna una còpia del conjunt d'esdeveniments actuals.

## Recomanació de disseny

Quan vulgues afegir mecàniques noves, el patró més net és:

1. deixar que la skill cree l'`AttackResult`
2. reconstruir o ajustar el colp dins del `HitContext`
3. aplicar les passives per fases
4. usar metadades i esdeveniments per comunicar estat temporal

Això evita escampar lògica ad hoc per moltes classes diferents.
