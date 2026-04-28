# ▌Efectes base i plantilles

[← Tornar a l'índex](../INDEX.md)

---

## ▌Ubicació

`models/effects/templates/`

Plantilles detectades:

- `BooleanFlagEffect`
- `ConstantDamageEffect`
- `DamageModifierEffect`
- `MissChanceEffect`
- `TimedEffect`

---

## ▌Per què existeixen?

Aquestes classes serveixen per evitar repetir patrons comuns.

Per exemple:

- flags booleans amb durada
- dany constant al llarg del temps
- modificadors de dany
- probabilitats de fallada
- efectes temporals genèrics

---

## ▌Recomanació

Abans de crear un efecte nou des de zero, revisa si una plantilla ja resol el 80% del cas.

Això ajuda a mantenir el codi coherent i a no duplicar gestió de durada, expiració o estat.
