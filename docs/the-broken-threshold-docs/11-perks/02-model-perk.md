# ▌Model de perk

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/PerkDefinition.java`
- `perks/PerkFamily.java`
- `perks/config/PerkConfig.java`

---

## ▌`PerkDefinition`

`PerkDefinition` és el model immutable que representa una perk carregada i validada.

Conté:

- `id`: identificador únic
- `name`: nom visible
- `description`: descripció
- `family`: agrupació visual/lògica
- `trigger`: moment d’activació
- `weight`: pes per randomització
- `conditions`: condicions d’activació
- `actions`: accions a executar

---

## ▌Trigger

Defineix quan s’avalua la perk dins del combat.

Exemples típics:

- inici de torn
- atac
- defensa
- mort d’entitat

---

## ▌Condicions i accions

El comportament real no està codificat directament a la perk, sinó en:

- condicions (`PerkCondition`)
- accions (`PerkAction`)

Això permet:

- reutilització
- configuració flexible
- extensió sense tocar el model

---

## ▌`PerkConfig`

Representa la versió carregada des de JSON.

Responsabilitats:

- deserialització
- validació bàsica
- transformació a `PerkDefinition`

---

## ▌Notes

- el model és immutable per evitar inconsistències
- la lògica real està externalitzada en factories