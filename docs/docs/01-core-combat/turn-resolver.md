```md
# `TurnResolver`

## Rol dins del sistema

`TurnResolver` resol **un únic torn**. Això inclou:

- interpretació de l'acció de l'atacant
- creació del `HitContext`
- execució de passives i efectes per fases
- resolució del crític
- aplicació de defensa/esquiva del defensor
- registre d'esdeveniments de combat
- construcció del `TurnResult`

És la classe més important per entendre el combat real.

## Signatura principal

```java
public TurnResult resolveTurn(
    Character attacker,
    Character defender,
    Action attackerAction,
    Action defenderAction,
    EndRoundRegenBonus defenderBonus)
```

## Ramificació principal

La primera decisió és aquesta:

```java
if (attackerAction != ATTACK) {
    return resolveNonAttackTurn(defender, defenderAction);
}
```

Això implica un comportament molt concret del sistema actual:

- només es construeix un cop real si l'acció de l'atacant és `ATTACK`
- si l'acció del jugador actiu és `DEFEND` o `DODGE`, només es resol el missatge defensiu amb dany 0

## Flux complet d'un torn d'atac

### 1. Preparar missatges de log

Es creen quatre llistes:

- `startMessages`
- `preDefenseMessages`
- `postDefenseMessages`
- `endTurnMessages`

Això permet separar clarament les fases del log.

### 2. Missatge especial del Grimori

Si l'arma abans de l'atac és `Arsenal.GRIMORIE`, afegeix:

```text
“… el Grimori s'activa …”
```

És una excepció visual, no una mecànica general.

### 3. Generar `AttackResult` base

```java
AttackResult attackResult = attacker.attack();
```

Aquesta crida pot acabar a:

- `Character.attackUnarmed()`
- `Weapon.attack(stats, rng)`
- una override racial (`Orc.attack`, `Halfling.attack`, `Tiefling.attack`)

### 4. Determinar objectiu real

```java
Character realTarget = attackResolver.chooseTarget(attacker, defender, attackResult);
```

Normalment el target és `ENEMY`, però una skill pot retornar `Target.SELF`.

### 5. Cas d'autoobjectiu

Si el target real és l'atacant:

- el dany s'aplica directament a l'atacant
- no es crea pipeline complet de defensa
- el `TurnResult` es construeix gairebé immediatament

Això es fa servir, per exemple, amb l'habilitat `explosiveShot` si hi ha autodispar.

### 6. Construir `HitContext`

```java
HitContext ctx = new HitContext(attacker, defender, weapon, attackerRng, attackerAction, defenderAction);
ctx.setAttackResult(attackResult);
```

Aquest context centralitza tot l'estat mutable del cop.

### 7. Reconfigurar el context a partir de l'arma

`configureHitContext(...)` fa una feina important:

- reconstrueix el `baseDamage`
- injecta `criticalChance`
- injecta `criticalMultiplier`
- escriu metadades com:
  - `WEAPON_NAME`
  - `RAW_DAMAGE`
  - `ORIGINAL_WEAPON_CRIT`

Això existeix perquè el `Weapon.basicAttack()` ja resol crític internament, però el motor modern vol tornar a controlar-lo al `HitContext`.

### 8. Fases del pipeline abans de defensar

L'ordre és exactament aquest:

```text
START_TURN        -> només atacant
BEFORE_ATTACK     -> atacant, defensor, arma
ROLL_CRIT         -> atacant, defensor, arma
resolveCritical() -> decisió final del crític
MODIFY_DAMAGE     -> atacant, defensor, arma
BEFORE_DEFENSE    -> atacant, defensor, arma
```

### 9. Resoldre defensa real

```java
Result defenderResult = attackResolver.resolveAttack(damageToResolve, defender, defenderAction);
```

A partir d'aquí:

- `DEFEND` redueix dany
- `DODGE` pot anul·lar-lo
- en qualsevol altre cas rep el cop directe

### 10. Registrar bonus de regeneració defensiva

```java
recoveryService.registerDefenseBonus(defenderAction, defenderResult, damageToResolve, defenderBonus);
```

Només s'acumula bonus si la defensa o esquiva ha funcionat realment.

### 11. Persistir resultat informatiu a l'arma

```java
weapon.registerResolvedAttack(ctx.wasCritical(), damageToResolve);
```

Això actualitza els camps interns `lastWasCrit` i `lastAttackDamage` perquè reflecteixin el resultat final real del pipeline.

### 12. Registrar esdeveniments de combat

`registerCombatEvents(ctx, defender, defenderAction)` pot marcar:

- `ON_DODGE`
- `ON_DEFEND`
- `ON_HIT`
- `ON_DAMAGE_DEALT`
- `ON_DAMAGE_TAKEN`
- `ON_KILL`

Aquests events poden ser consultats per efectes i passives.

### 13. Fases posteriors

Després de defensa:

```text
AFTER_DEFENSE     -> sempre
AFTER_HIT         -> només si ctx.damageDealt() > 0
END_TURN          -> només atacant
```

### 14. Construcció del `TurnResult`

Retorna:

- nom de l'actor
- missatge principal de l'atacant
- missatges per fase
- missatge de defensa
- dany final infligit
- si hi ha hagut crític

## `resolveNonAttackTurn`

Quan el jugador actiu no ataca, es fa:

```java
Result defenderResult = attackResolver.resolveAttack(0, defender, defenderAction);
```

Això és útil perquè:

- si el defensor estava en `DEFEND`, retorna el missatge de "bloquejar l'aire"
- si estava en `DODGE`, retorna el missatge d'esquivar l'aire
- no hi ha dany

## Conclusió operativa

Si vols afegir mecàniques de combat profundes, `TurnResolver` és la primera classe que has d'entendre.
```
