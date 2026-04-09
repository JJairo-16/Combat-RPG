```md
# `Weapon`, `WeaponType`, `Attack` i `AttackResult`

## `Weapon`

És l'objecte central del combat ofensiu.

### Què encapsula

- nom i descripció
- dany base intern
- probabilitat base de crític
- multiplicador base de crític
- tipus d'arma
- habilitat d'atac (`Attack`)
- cost de mana de la skill
- passives (`List<WeaponPassive>`)
- identificador d'arsenal

També manté estat informatiu de l'últim atac:

- `lastWasCrit`
- `lastAttackDamage`
- `lastNonCriticalDamage`

## Flux de `Weapon.attack(stats, rng)`

```java
if (stats.getMana() < manaPrice) {
    return new AttackResult(
        WeaponType.PHYSICAL.getBasicDamage(5, stats),
        "no li quedava mana, aixi que li dona un cop.");
}
return attack.execute(this, stats, rng);
```

Això vol dir:

- si no hi ha prou mana, fa un fallback físic bàsic
- si n'hi ha, executa la skill associada

## `basicAttack(stats, rng)`

Aquest mètode és importantíssim perquè moltes skills el fan servir.

Passos:

1. calcula dany base segons `WeaponType`
2. aplica variància de dany
3. resol crític intern
4. si hi ha crític, multiplica el dany
5. guarda estat informatiu

### Fórmula de dany base per tipus

Es delega a `WeaponType.getBasicDamage(inputBase, stats)`.

## `WeaponType`

Hi ha tres tipus:

- `PHYSICAL`
- `MAGICAL`
- `RANGE`

### Requisits d'equipament

- `PHYSICAL`: força mínima 20
- `MAGICAL`: intel·ligència mínima 20
- `RANGE`: força mínima 10 i destresa mínima 20

### Fórmula de dany

```java
double base = inputBase * 1.2;

switch (this) {
    case PHYSICAL -> base += stats.getStrength() * 1.2;
    case MAGICAL -> base += stats.getIntelligence() * 1.2;
    default      -> base += stats.getDexterity() * 1.25;
}

return round2(base + stats.getDexterity() * 0.2);
```

Interpretació:

- cada tipus té una estadística principal
- la destresa sempre afegeix una petita aportació comuna
- el resultat es redondeja a 2 decimals

## Sistema de crític de `Weapon`

### Probabilitat efectiva

```java
criticalProb + (stats.getLuck() * 0.002)
```

clamp entre 0 i 0.95.

### Multiplicador efectiu

```java
Math.max(1.0, criticalDamage + stats.getLuck() * 0.01)
```

La sort augmenta tant la probabilitat com el multiplicador.

## `Attack`

És una interfície funcional:

```java
@FunctionalInterface
public interface Attack {
    AttackResult execute(Weapon weapon, Statistics stats, Random rng);
}
```

Això permet definir skills com a referències a mètode, per exemple:

```java
Skills::explosiveShot
```

## `AttackResult`

És un `record` immutable:

- `damage`
- `message`
- `target`

Si no s'indica target, assumeix `Target.ENEMY`.

## `registerResolvedAttack(...)`

Després de passar pel `HitContext`, `TurnResolver` truca aquest mètode perquè l'estat de l'arma reflecteixi el crític i el dany final real, no només el resultat intern preliminar de `basicAttack`.

## `triggerPhase(...)`

L'arma pot activar passives per fases del `HitContext`.

Aquest mecanisme permet que una arma:

- afegeixi dany
- curi després del cop
- modifiqui crític
- reaccioni abans o després de la defensa

Sense tocar ni `CombatSystem` ni `TurnResolver`.
```
