# ▌Estadístiques

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`models/characters/Statistics.java`

---

## ▌Què encapsula?

Les 7 estadístiques principals:

- força
- destresa
- constitució
- intel·ligència
- saviesa
- carisma
- sort

I també els valors derivats i variables:

- vida i vida màxima
- manà i manà màxim
- estamina i estamina màxima
- resistència
- invulnerabilitat

---

## ▌Càlculs derivats

Alguns exemples visibles al constructor:

```java
this.maxHealth = calculateMaxHealth(constitution);
this.maxMana = intelligence * 30.0;
this.maxStamina = calculateMaxStamina();
this.maxResistance = calculateMaxResistance();
```

Això confirma que `Statistics` no és només un contenidor, sinó una classe de domini amb lògica pròpia.

---

## ▌Relació amb el balance

`Statistics` consulta `CombatBalanceRegistry` per a regles de:

- cost d'estamina
- recuperació
- fatiga
- multiplicadors

Per tant, si el canvi és numèric i global, primer mira si ja està cobert al `balance/`.

---

## ▌Recomanació

Evita posar fórmules derivades repartides entre moltes classes. Si depenen clarament d'una stat, `Statistics` és el millor lloc.
