# ▌Afinitat divina

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`utils/rng/DivineCharismaAffinity.java`

---

## ▌Què fa?

Genera un perfil ocult per partida que classifica el carisma en bandes.

La documentació interna del codi ho descriu així:

```text
cau malament - normal - cau bé - normal - cau malament
```

---

## ▌Funció dins del joc

No modifica tot el combat, sinó sobretot la predisposició dels déus en mecàniques com la **crida espiritual**.

---

## ▌Punts clau

- hi ha un únic perfil per partida
- el perfil es genera amb `rollForRun(Random rng)`
- hi ha `classifyStanding(...)` i `classifyBand(...)`
- hi ha API per injectar perfil manualment en tests

---

## ▌Valor per desenvolupament

És una mecànica molt bona per afegir variabilitat entre partides sense trencar el sistema base.
