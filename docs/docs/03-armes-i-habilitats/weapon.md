# `Weapon`, `WeaponType`, `Attack` i `AttackResult`

## `Weapon`

`Weapon` és la instància viva d'una arma durant el combat.

## Què encapsula

### Identitat i definició

- `id`
- `name`
- `description`

### Potència ofensiva

- `damage` base intern
- `criticalProb`
- `criticalDamage`
- `WeaponType`
- `Attack`
- `manaPrice`

### Extensió

- `List<WeaponPassive>`

### Estat informatiu de l'últim atac

- `lastWasCrit`
- `lastAttackDamage`
- `lastNonCriticalDamage`

Aquest estat no és persistència de combat complexa; servix sobretot perquè les skills i el resolutor puguen saber què ha passat en l'últim càlcul.

## Construcció

La classe no es construïx directament des d'`Arsenal`, sinó a través de `WeaponDefinition.create()`.

Això permet:

- separar configuració i instància viva
- generar una arma nova cada vegada
- resoldre la skill i les passives en el moment de crear-la

## Flux de `attack(stats, rng)`

```java
public AttackResult attack(Statistics stats, Random rng) {
    if (stats.getMana() < manaPrice) {
        return new AttackResult(
                WeaponType.PHYSICAL.getBasicDamage(5, stats),
                "no li quedava mana, aixi que li dona un cop.");
    }
    return attack.execute(this, stats, rng);
}
```

Això implica dues coses:

- si no hi ha prou mana per usar la skill, l'arma fa un **fallback físic bàsic**
- el fallback no usa el tipus real de l'arma, sinó `WeaponType.PHYSICAL`

És una decisió important, perquè fins i tot una arma màgica o de rang acaba fent un colp físic senzill quan no hi ha mana.

## `basicAttack(stats, rng)`

Aquest és el mètode base més important del paquet.

### Passos

1. calcula el dany base segons `type.getBasicDamage(damage, stats)`
2. aplica variància de dany
3. arrodonix a 2 decimals
4. guarda aquest valor a `lastNonCriticalDamage`
5. resol si hi ha crític
6. si no hi ha crític, guarda el dany com a `lastAttackDamage`
7. si hi ha crític, aplica el multiplicador i guarda el resultat final

## Variància de dany

La variància usa dues tirades aleatòries i en fa la mitjana:

```java
double roll = (rng.nextDouble() + rng.nextDouble()) / 2.0;
return DOWN_VARIANCE + roll * UP_VARIANCE;
```

Constants actuals:

- `DAMAGE_VARIANCE = 0.07`
- `DOWN_VARIANCE = 0.93`
- `UP_VARIANCE = 0.14`

Per tant, la variància es mou aproximadament entre `0.93` i `1.07`, amb una distribució més centrada que una sola tirada uniforme.

## `basicAttackWithMessage(...)`

És un envoltori de conveniència sobre `basicAttack(...)`.

Retorna:

- `"llença un cop crític."` si l'atac ha sigut crític
- `"llença un atac."` si no ho ha sigut

## Sistema de crític de `Weapon`

### Probabilitat efectiva

```java
criticalProb + (stats.getLuck() * 0.002)
```

Es limita entre `0.0` i `0.95`.

### Multiplicador efectiu

```java
Math.max(1.0, criticalDamage + stats.getLuck() * 0.01)
```

La sort augmenta tant la probabilitat com el multiplicador de crític.

## Estat de l'últim atac

### `lastNonCriticalDamage()`
Retorna el dany base després de variància però abans del multiplicador crític.

### `lastWasCritic()`
Indica si l'últim atac calculat per l'arma ha sigut crític.

### `lastAttackDamage()`
Retorna el dany final calculat per l'arma en l'últim atac.

### `registerResolvedAttack(boolean wasCrit, double finalDamage)`
Permet que el resolutor extern reescriga l'estat informatiu de l'arma amb el resultat final real del cop, no sols amb el càlcul preliminar de `basicAttack(...)`.

## `canEquip(stats)`

Delegació directa a `WeaponType.canEquip(stats)`.

## `triggerPhase(...)`

És el mecanisme que connecta l'arma amb el pipeline de passives.

Variants disponibles:

- `triggerPhase(HitContext ctx, Random rng, Phase phase)`
- `triggerPhase(HitContext ctx, Random rng, Phase phase, List<String> out)`
- `triggerAfterHit(...)`

Funcionament:

1. recorre totes les `WeaponPassive`
2. crida `p.onPhase(this, ctx, rng, phase)`
3. si el resultat és un missatge no buit, l'afig al log

## `WeaponType`

Hi ha tres tipus d'arma:

- `PHYSICAL`
- `MAGICAL`
- `RANGE`

Cada tipus porta dos conceptes diferents:

- requisits mínims per poder-se equipar
- fórmula de dany base

### Requisits actuals

- `PHYSICAL`: força mínima `20`
- `MAGICAL`: intel·ligència mínima `20`
- `RANGE`: força mínima `10` i destresa mínima `20`

La resta de stats mínimes del constructor de l'enum estan a `0`.

### Fórmula de dany

```java
double base = inputBase * 1.2;

switch (this) {
    case PHYSICAL -> base += stats.getStrength() * 1.2;
    case MAGICAL -> base += stats.getIntelligence() * 1.2;
    default -> base += stats.getDexterity() * 1.25;
}

return round2(base + stats.getDexterity() * 0.2);
```

Interpretació:

- totes les armes partixen de `inputBase * 1.2`
- cada tipus té una stat principal
- la destresa sempre dona una aportació comuna extra
- el resultat final s'arrodonix a 2 decimals

## `Attack`

`Attack` és una interfície funcional:

```java
@FunctionalInterface
public interface Attack {
    AttackResult execute(Weapon weapon, Statistics stats, Random rng);
}
```

Això permet implementar skills com a referències a mètode, per exemple `Skills::explosiveShot`.

## `AttackResult`

`AttackResult` és un `record` immutable amb aquests camps:

- `damage`
- `message`
- `target`

Té també un constructor de conveniència que assumix `Target.ENEMY` quan no s'indica objectiu.

## `Target`

Els objectius possibles actuals són:

- `SELF`
- `ENEMY`

Això permet skills que es danyen a si mateixes, com `explosiveShot`.
