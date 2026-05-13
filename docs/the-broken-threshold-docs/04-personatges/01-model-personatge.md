# ▌Model de personatge

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe principal

`models/characters/Character.java`

És una classe gran perquè concentra:

- identitat del personatge
- raça
- estadístiques
- arma equipada
- efectes actius
- cooldowns i flags
- estat temporal de combat

---

## ▌Estat persistent i estat temporal

### ▌Persistent
- nom
- edat
- raça
- estadístiques base/derivades
- arma
- efectes

### ▌Temporal
- guard stacks
- charged attack
- vulnerable turns
- bleed turns
- stagger turns
- momentum
- adrenalina
- cooldown de crida espiritual

---

## ▌Constants importants

La classe conté decisions de disseny explícites, com ara:

- `TOTAL_POINTS = 140`
- `MIN_STAT = 10`
- `MIN_CONSTITUTION = MIN_STAT + 2`
- llindar de crida espiritual al `20%` de vida

Això indica que part del balance encara viu al model del personatge, no només a JSON.

---

## ▌Exemple real

```java
private static final int TOTAL_POINTS = 140;
private static final int MIN_STAT = 10;
private static final int MIN_CONSTITUTION = MIN_STAT + 2;
private static final double SPIRITUAL_CALLING_THRESHOLD = 0.20;
```

---

## ▌On tocar segons el canvi

- nova condició de combat temporal → `Character`
- nova relació entre stats i comportament → `Character` o `Statistics`
- nova regla d'efectes permanents → `Character` + `Effect`
