# Sistema de passives d'arma

## Abast real d'aquest paquet

En el codi pujat dins `weapons.zip`, el sistema documentat ací és el de **passives d'arma**. No hi apareixen classes d'un sistema general d'`Effect` dins aquest paquet concret.

Per això, la capa actual i verificable és:

- `WeaponPassive`
- `Passives`
- `PassiveFactory`
- configuració `PassiveConfig`
- integració amb `Weapon` i `HitContext`

## `WeaponPassive`

`WeaponPassive` és una interfície orientada a fases del cop.

Cada mètode retorna opcionalment un `String` per al log:

- `startTurn(...)`
- `beforeAttack(...)`
- `rollCrit(...)`
- `modifyDamage(...)`
- `beforeDefense(...)`
- `afterDefense(...)`
- `afterHit(...)`
- `endTurn(...)`

La interfície també oferix `onPhase(...)`, que fa el despatx segons `HitContext.Phase`.

### Quan usar una `WeaponPassive`

És la millor opció si l'efecte:

- depén de l'arma
- no necessita un contenidor extern de persistència
- actua sobre el cop actual
- s'ha de resoldre per fases del pipeline

## Integració amb `Weapon`

`Weapon.triggerPhase(...)` recorre totes les passives de l'arma i executa la fase demanada.

Això fa possible que una arma:

- modifique dany
- reaccione després del colp
- genere missatges de log
- amplie comportament sense tocar la implementació de la skill

## `PassiveFactory`

`PassiveFactory` convertix una configuració JSON en una passiva real.

### Entrada

Rep un `PassiveConfig`, que té:

- `type`
- `params`

### Resolució actual

Tipus suportats actualment:

- `lifeSteal`
- `trueHarm`
- `executor`

Si falta el tipus, falta un paràmetre o el nom és desconegut, es llança `IllegalArgumentException`.

## `PassiveConfig`

`PassiveConfig` és un `record` molt simple:

```java
public record PassiveConfig(String type, Map<String, Object> params) {}
```

Això permet definir passives des de JSON amb una estructura flexible.

## Passives existents a `Passives`

### `lifeSteal(pct)`

S'executa a `AFTER_HIT`.

Comportament:

1. calcula `healAmount = ctx.damageDealt() * pct`
2. cura l'atacant amb `heal(...)`
3. si la curació real és major que 0, genera missatge

Punts importants:

- es basa en `damageDealt()`, no en el dany teòric previ
- per tant, la curació depén del dany real després de defensa i resolució

### `trueHarm(pct)`

També s'executa a `AFTER_HIT`.

Comportament:

1. llig la vida màxima del defensor
2. calcula `extra = maxHealth * pct`
3. aplica aquest dany directament amb `damage(extra)`
4. genera un missatge percentual

Punts importants:

- no usa `ctx.damageDealt()`
- el càlcul depén de la vida màxima del rival
- el dany extra s'aplica després de l'impacte principal

### `executor(thresholdLife, damageBonus)`

S'executa a `MODIFY_DAMAGE`.

Comportament:

1. calcula el percentatge actual de vida del defensor
2. si està per damunt del llindar, no fa res
3. si està al llindar o per davall, aplica `ctx.multiplyDamage(1.0 + damageBonus)`
4. genera un missatge d'execució

Punts importants:

- modifica el dany abans de la defensa
- treballa sobre la vida relativa actual del defensor
- és una passiva contextual, no un efecte persistent

## Diferència entre skill i passiva

En aquest paquet convé separar molt bé les dues capes:

### Skill
- es resol quan es crida `Attack.execute(...)`
- crea l'`AttackResult` base
- sol decidir dany inicial, missatge i objectiu

### Passiva
- s'executa després, per fases
- modifica el `HitContext`
- pot alterar crític, dany o efectes posteriors
- pot escriure missatges addicionals al log

## Patró per afegir una passiva nova

1. crear un nou helper a `Passives`
2. afegir el cas corresponent a `PassiveFactory`
3. definir el seu format dins del JSON
4. assegurar que actua en la fase adequada del `HitContext`
