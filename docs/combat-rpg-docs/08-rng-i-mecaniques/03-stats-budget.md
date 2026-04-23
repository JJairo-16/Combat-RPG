# ▌Stats budget

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`utils/rng/StatsBudget.java`

---

## ▌Per a què serveix?

S'utilitza principalment durant la generació automàtica del personatge.

Retorna un resultat que inclou:

- estadístiques base
- raça associada

---

## ▌On es veu clarament

A `CharacterCreator`:

```java
private static Generation autoGenerate() {
    Result res = StatsBudget.generate(TOTAL_POINTS);
    return new Generation(res.baseStats(), res.breed());
}
```

---

## ▌Per què és rellevant?

Aquesta classe concentra la lògica de repartiment automàtic.  
Si es vol canviar el perfil dels personatges generats aleatòriament, aquest és el primer lloc a revisar.
