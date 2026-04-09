```md
# `Arsenal` i `Skills`

## `Arsenal`

`Arsenal` és un enum que funciona com a catàleg immutable d'armes. Cada entrada defineix tots els paràmetres necessaris per construir una `Weapon`.

### Camps definits per cada entrada

- nom
- descripció
- `baseDamage`
- `criticalProb`
- `criticalDamage`
- `WeaponType`
- `Attack`
- `manaPrice` opcional
- `WeaponPassive...` opcionals

### Creació d'una arma

```java
public Weapon create() {
    return new Weapon(
        name, description,
        baseDamage, criticalProb, criticalDamage,
        type, attack, manaPrice,
        passives, this);
}
```

## Armes actuals del projecte

### `EXPLOSIVE_CROSSBOW`
- tipus: `RANGE`
- skill: `Skills::explosiveShot`

### `VAMPIRICS_DAGGERS`
- tipus: `PHYSICAL`
- skill: `Skills::nothing`
- passiva: `lifeSteal(0.10)`

### `ARCANE_DISRUPTION_STAFF`
- tipus: `MAGICAL`
- skill: `Skills::arcaneDisruption`
- mana: 200

### `SCIMITAR`
- tipus: `PHYSICAL`
- skill: `Skills::nothing`

### `BALLISTA`
- tipus: `RANGE`
- skill: `Skills::luckyBallista`

### `GRIMORIE`
- tipus: `MAGICAL`
- skill: `Skills::grimoriCipher`
- mana: 135

### `BLOODPIERCER_THROWING_DAGGER`
- tipus: `RANGE`
- skill: `Skills::perforatingThrow`
- passiva: `trueHarm(0.003)`

### `EXECUTIONERS_EDGE`
- tipus: `PHYSICAL`
- skill: `Skills::nothing`
- passiva: `executor(0.30, 0.25)`

### `CHRONO_WEAVER_STAFF`
- tipus: `MAGICAL`
- skill: `Skills::chronoWeave`
- mana: 95

## Skills actuals

### `nothing`
No fa res especial. Crida `weapon.basicAttackWithMessage(stats, rng)`.

### `explosiveShot`
Pot impactar l'enemic o a si mateix.

- calcula un dany base
- té probabilitat d'autodispar influïda per la sort
- si impacta l'enemic, multiplica dany per 1.10
- si impacta a si mateix, multiplica dany per 0.50 i target = `SELF`

### `crossCut`
Fa dos atacs seguits i suma el dany.

- no crea dos `HitContext`
- simplement suma dos `basicAttack(...)` dins la mateixa skill

### `arcaneDisruption`
Skill màgica amb consum de mana i probabilitat de fallar.

Detalls rellevants:

- comprova si hi ha prou mana
- després torna a consumir mana una segona vegada
- si hi ha crític, consumeix un 50% extra

A nivell pràctic, el codi actual acaba fent doble consum base en el camí d'èxit. Això és una particularitat important del codi existent.

### `luckyBallista`
Dispara entre 1 i 4 projectils.

- probabilitat inicial de continuar depèn de la sort
- cada tret degrada la probabilitat de continuar
- cada tret degrada el multiplicador de dany
- un crític millora lleugerament la probabilitat de continuar

### `grimoriCipher`
És un minijoc de mecanografiat.

Flux:

1. comprova mana
2. genera una seqüència amb `GrimoriCodeGenerator`
3. l'usuari l'ha d'escriure
4. mesura el temps
5. calcula un multiplicador segons velocitat i encert
6. aplica `weapon.basicAttack(stats, rng)` i multiplica

Aquesta skill és l'exemple més clar de mecànica "fora del combat pur" integrada al model d'atac.

### `perforatingThrow`
Daga de penetració.

- parteix del `basicAttack`
- aplica un multiplicador base 1.18
- la destresa augmenta la penetració
- si hi ha crític, afegeix un bonus més

### `chronoWeave`
Simula 3 futurs possibles.

- genera tres danys
- els ordena
- segons intel·ligència i sort, tria millor, mig o pitjor resultat

Particularitat de lectura:
el missatge del `switch` i els comentaris interns no coincideixen perfectament amb l'índex escollit després de l'ordenació. És un bon lloc a revisar si es vol pulir coherència interna.

## Patró general per implementar skills noves

Una skill bona en aquest projecte acostuma a fer això:

1. comprovar i consumir recursos si cal
2. calcular un o més `basicAttack(...)`
3. transformar el dany o el target
4. retornar un `AttackResult`

No aplica defensa ni passives directament; això ho farà el motor després.
```
