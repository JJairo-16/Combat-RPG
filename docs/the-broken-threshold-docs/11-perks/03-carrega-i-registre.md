# ▌Càrrega i registre de perks

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/PerkLoader.java`
- `perks/PerkRegistry.java`
- `perks/config/PerkConfig.java`

---

## ▌Càrrega des de JSON

`PerkLoader` llegeix fitxers JSON i construeix instàncies de `PerkDefinition`.

Procés:

1. lectura amb UTF-8
2. parseig amb Gson
3. validació mínima
4. conversió a model immutable
5. retorn de col·lecció

```java
List<PerkDefinition> perks = PerkLoader.load(file);
```

---

## ▌Registre

`PerkRegistry` emmagatzema totes les perks carregades.

Responsabilitats:

- accés per `id`
- llistat complet
- selecció aleatòria ponderada

---

## ▌Selecció aleatòria

Les perks es seleccionen segons `weight`.

Això permet:

- control de probabilitats
- balanceig sense tocar codi

---

## ▌Validacions

Durant la càrrega:

- ids duplicats es descarten o sobreescriuen
- configuracions incompletes s’ignoren

---

## ▌Notes

- el registre és global i immutable després de la càrrega
- la càrrega es fa en fase d’inicialització