# ▌Races

[← Tornar a l'índex](../INDEX.md)

---

## ▌Ubicació

`models/breeds/`

Classes detectades:

- `Breed`
- `Dwarf`
- `Elf`
- `Gnome`
- `Halfling`
- `Orc`
- `Tiefling`

---

## ▌Paper dins del sistema

La raça s'aplica durant la construcció del personatge i modifica les estadístiques efectives.

A `Character` es veu clarament aquesta idea:

```java
int[] effectiveStats = applyBreed(stats, breed);
this.stats = new Statistics(effectiveStats);
```

---

## ▌Conseqüència

La raça no és només informativa. Entra en el càlcul del personatge abans que existeixin els valors derivats.

---

## ▌Per afegir una nova raça

Cal revisar com a mínim:

- `models/breeds/`
- el punt on `Breed.values()` o `Breed.getNamesList()` alimenta menús
- qualsevol mapping de `StatsBudget` cap a raça
