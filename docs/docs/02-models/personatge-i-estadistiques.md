```md
# Personatge i estadístiques

## `Character`

`Character` representa el combatent base.

### Camps principals

- `name`
- `age`
- `breed`
- `stats`
- `weapon`
- `rng`
- `effects`

### Validacions de construcció

A `Character(String name, int age, int[] stats, Breed breed)` es valida:

- nom no nul ni buit
- edat major que 0
- array d'estadístiques amb 7 valors
- suma total d'estadístiques
- mínims per estadística
- mínim específic de constitució

Després aplica el bonus racial amb `applyBreed(stats, breed)` i crea `Statistics`.

## Atac

### `attack()`

```java
public AttackResult attack() {
    if (weapon == null) {
        return attackUnarmed();
    }
    return weapon.attack(stats, rng);
}
```

Per tant, el personatge delega l'atac a l'arma si n'hi ha.

### Atac sense arma

```java
protected AttackResult attackUnarmed() {
    return new AttackResult(
        WeaponType.PHYSICAL.getBasicDamage(5, stats),
        "ataca amb les mans desnudes.");
}
```

## Defensa

### `defend(double attack)`

La defensa actual redueix el dany al 60%.

```java
double recived = attack * 0.6;
stats.damage(recived);
```

Interpretació real: defensar bloqueja un 40% del dany entrant.

## Esquiva

### `dodge(double attack)`

Passos:

1. si el dany és 0 o menys, retorna missatge d'esquivar l'aire
2. calcula `dodgeProb` amb `tryToDodge()`
3. si la tirada surt bé, multiplicador = 0
4. si falla, multiplicador = 1
5. aplica el dany resultant

### Fórmula de probabilitat d'esquiva

```java
double dexComponent = (stats.getDexterity() - 10) * 0.02;
double luckComponent = stats.getLuck() * 0.0015;
double dodgeProb = dexComponent + luckComponent;
return Math.clamp(dodgeProb, 0.05, 0.75);
```

Conseqüències:

- el mínim d'esquiva és 5%
- el màxim és 75%
- la destresa pesa molt més que la sort

## Dany directe

### `getDamage(double attack)`

Aplica dany sense esquiva ni defensa.

## Regeneració

### `regen()`

Per defecte:

```java
stats.reg();
```

Les subclasses racials poden sobreescriure aquest comportament.

## Efectes persistents

`Character` manté `List<Effect> effects`.

### `addEffect(Effect incoming)`

Si entra un efecte amb la mateixa `key()`, la resolució depèn de `StackingRule`:

- `IGNORE` -> no fa res
- `REPLACE` -> substitueix l'efecte existent
- `REFRESH` o `STACK` -> crida `existing.mergeFrom(incoming)`

Després ordena per `priority` descendent.

### `triggerEffects(...)`

Per cada `Effect` actiu:

1. crida `e.onPhase(ctx, phase, rng)`
2. recull el missatge si n'hi ha
3. al final elimina efectes expirats

## `Statistics`

Aquesta classe separa:

- estadístiques base
- màxims derivats
- valors actuals de vida i mana

### Estadístiques base

En aquest ordre:

1. strength
2. dexterity
3. constitution
4. intelligence
5. wisdom
6. charisma
7. luck

### Derivades

- `maxHealth` depèn de constitució amb softcap
- `maxMana` = `intelligence * 30.0`

### Regeneració base

```java
hp = calculateHealthRegen(constitution);
ma = intelligence * 0.9;
```

### Softcaps

- vida màxima: softcap a partir de 20 de constitució
- regeneració de vida: també amb softcap, una mica més agressiu

Això evita que la constitució escali linealment per sempre.

### Operacions útils

- `damage(double dmg)`
- `consumeMana(double price)`
- `heal(double amount)`
- `restoreMana(double amount)`

## Lectura pràctica del model

- la vida i el mana són estat mutable del combat
- força/destreza/intel·ligència afecten el dany segons el tipus d'arma
- constitució afecta vida màxima i regeneració
- sort entra en crítics, esquiva i algunes skills
```
