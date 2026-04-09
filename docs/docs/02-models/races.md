```md
# Races (`Breed` i subclasses)

## `Breed` enum

L'enum `Breed` defineix:

- l'estadística principal que rep bonus racial
- el nom visible
- la descripció

El bonus racial aplicat és del **15%** sobre una sola estadística principal.

```java
private static final double STAT_BONUS_BY_BREED = 1.15;
```

### Correspondència de races

- `ORC` -> `STRENGTH`
- `ELF` -> `DEXTERITY`
- `DWARF` -> `CONSTITUTION`
- `GNOME` -> `INTELLIGENCE`
- `HUMAN` -> `WISDOM`
- `TIEFLING` -> `CHARISMA`
- `HALFLING` -> `LUCK`

## Subclasses especialitzades de `Character`

A més de l'enum, el projecte té subclasses concretes amb comportament extra.

### `Orc`

Canvis:

- no pot equipar armes màgiques
- si l'arma és física, ataca amb unes stats modificades
- el bonus físic de força s'aplica també als punys
- atac sense arma usa base 15 en lloc de 5

Implicació: és la raça més especialitzada en combat físic.

### `Elf`

Sobreescriu `tryToDodge()`.

```java
return super.tryToDodge() * (1 + DODGE_LUCK);
```

Amb `DODGE_LUCK = 0.15`.

És a dir, millora la probabilitat final d'esquiva un 15%.

### `Dwarf`

Sobreescriu `regen()` per donar bonus de vida.

```java
stats.reg(HP_BONUS, 1);
```

Amb `HP_BONUS = 1.15`.

### `Gnome`

Sobreescriu `regen()` per donar bonus de mana.

```java
stats.reg(1, MANA_BONUS);
```

Amb `MANA_BONUS = 1.15`.

### `Tiefling`

Sobreescriu `attack()`.

Després d'obtenir l'atac normal, té un 5% de probabilitat de duplicar el dany:

```java
if (rng.nextDouble() < DOUBLE_ATTACK_PROB)
    return new AttackResult(attack.damage() * 2.0, attack.message(), attack.target());
```

No genera un segon cop separat; simplement duplica el dany de l'`AttackResult`.

### `Halfling`

Fa servir unes estadístiques modificades només per atacar.

- aplica el bonus racial
- després torna a escalar la sort amb `ATTACK_LUCK_BONUS = 1.1`
- usa aquestes stats alterades a `weapon.attack(...)`

Per tant, la seva sort efectiva ofensiva és superior a la visible al `stats` base del personatge.

## Observació important de disseny

Hi ha dues maneres de representar una raça en aquest projecte:

1. com a `Breed` per al bonus base
2. com a subclasse de `Character` per a comportament especial

Quan afegeixis una raça nova, probablement necessitaràs tocar totes dues capes.
```
