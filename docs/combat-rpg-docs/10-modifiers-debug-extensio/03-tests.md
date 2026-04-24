# ▌Tests i simulacions

[← Tornar a l'índex](../INDEX.md)

---

## ▌Fitxers detectats

- `debug/test/DivineCharismaAffinityTest.java`
- `debug/test/SpiritualCallingDieTest.java`

---

## ▌Tipus de proves

No són només unit tests clàssics.  
Són sobretot **simulacions estadístiques** per validar distribucions i balance.

---

## ▌Exemple real

A `DivineCharismaAffinityTest`:

```java
private static final int SIMULATION_COUNT = 1_000_000;
private static final int TEST_CHARISMA = 16;
```

Això mostra una preocupació clara per verificar probabilitats i no només funcionalitat superficial.

---

## ▌Conclusió

Quan toquis sistemes de RNG o balance, aquesta carpeta és una bona base per afegir noves simulacions abans d'ajustar números a ull.
