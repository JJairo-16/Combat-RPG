# ▌Com ampliar el projecte

[← Tornar a l'índex](../INDEX.md)

---

## ▌Afegir una arma nova

1. afegir definició al JSON d'armes
2. assegurar que `attackSkill` existeix a `AttackRegistry`
3. assegurar que les passives existeixen a `PassiveFactory`
4. verificar requisits d'equipament

---

## ▌Afegir una passiva nova

1. crear implementació de `WeaponPassive`
2. decidir la fase
3. registrar-la al `PassiveFactory`
4. referenciar-la des del JSON

---

## ▌Afegir un efecte nou

1. implementar `Effect`
2. decidir `key`, `stackingRule` i estat
3. connectar-lo al lloc on s'aplica
4. provar-ne el comportament dins del `EffectPipeline`

---

## ▌Afegir una acció contextual de menú

1. implementar la lògica a `Actions`
2. registrar la clau a `StatusModActionRegistry`
3. crear configuració a `menuModifiers.json`
4. definir disponibilitat

---

## ▌Modificar el balance

1. afegir o editar el camp a `balance/config`
2. validar-lo a `CombatBalanceValidator`
3. carregar-lo amb `CombatBalanceLoader`
4. consumir-lo des del domini

---

## ▌Consell final

Quan dubtis on posar una nova funcionalitat, pregunta't primer:

- és de domini de combat?
- és d'interfície/menú?
- és estat del personatge?
- és configuració?
- és RNG/balance?

Aquesta classificació encaixa molt bé amb l'arquitectura actual.
