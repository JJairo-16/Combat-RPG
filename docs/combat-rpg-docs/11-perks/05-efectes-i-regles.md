# ▌Efectes i regles de perks

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/effect/ConfigurablePerkEffect.java`
- `perks/effect/PerkEffectFactory.java`
- `perks/effect/PerkRuleFactory.java`
- `perks/effect/PerkCondition.java`
- `perks/effect/PerkAction.java`
- `perks/effect/PerkContext.java`

---

## ▌De perk a efecte

Quan el jugador selecciona una perk, es crea un efecte actiu.

```java
Effect effect = PerkEffectFactory.create(perk);
```

---

## ▌`ConfigurablePerkEffect`

Implementa el comportament en temps d’execució.

Funcions:

- avaluar condicions
- executar accions
- respondre a triggers

---

## ▌Condicions

`PerkCondition` decideix si l’efecte s’activa.

Exemples:

- estat del jugador
- context del combat
- tipus d’esdeveniment

---

## ▌Accions

`PerkAction` defineix què passa:

- aplicar dany
- modificar stats
- generar efectes

---

## ▌Context

`PerkContext` encapsula:

- jugador
- objectiu
- estat del combat

---

## ▌Regles

`PerkRuleFactory` construeix la lògica combinant:

- condicions
- accions

---

## ▌Notes

- arquitectura basada en composició
- permet crear perks complexes sense codi nou