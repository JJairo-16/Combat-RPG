# ▌Creació de personatges

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`creator/CharacterCreator.java`

---

## ▌Modes de creació

Hi ha dues vies:

- automàtica
- manual

```java
boolean autoGenerate = getter.getBoolean(
        "Vol generar el personatge automàticament? [S/N] ",
        true,
        "S",
        "N");

Generation gen = autoGenerate ? autoGenerate() : manualGenerate();
```

---

## ▌Flux manual

1. demanar nom i edat
2. escollir raça
3. repartir exactament 140 punts
4. validar mínims
5. crear `Character`

---

## ▌Flux automàtic

Fa servir `StatsBudget.generate(TOTAL_POINTS)` per obtenir:

- stats base
- raça coherent amb el resultat

---

## ▌Detalls útils per integrar-se

- el nom té validació de longitud i format
- hi ha `createDebugCharacter()` per proves ràpides
- existeix un `dummy()` que s'utilitza en menús i tests

---

## ▌Quan tocar aquesta classe

- si vols canviar el procés d'onboarding dels jugadors
- si vols afegir noves regles de validació
- si vols introduir presets o plantilles de personatge
