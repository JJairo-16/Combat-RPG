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

Quan el jugador tria una perk, `PerkEffectFactory` crea un efecte permanent.

```java
public static Effect create(PerkDefinition perk) {
    return new ConfigurablePerkEffect(perk);
}
```

Aquest efecte s'afegeix al personatge i participa en el mateix pipeline que la resta d'efectes de combat.

---

## ▌Efecte configurable

`ConfigurablePerkEffect` adapta una `PerkDefinition` al contracte `Effect`.

La seva clau és estable i depèn de l'identificador de la perk.

```java
@Override
public String key() {
    return "PERK_" + perk.id();
}
```

La regla d'apilament és `IGNORE`, de manera que la mateixa perk no s'acumula repetidament.

---

## ▌Execució per fase

L'efecte només actua quan la fase rebuda coincideix amb el `trigger` de la perk.

Després:

1. crea un `PerkContext`
2. avalua totes les condicions
3. si alguna condició falla, no fa res
4. executa les accions en ordre
5. aplica l'estil visual de la família al missatge resultant

---

## ▌Condicions disponibles

`PerkRuleFactory.condition(...)` converteix les regles JSON en predicats executables.

Condicions implementades:

- `CHANCE`
- `OWNER_HEALTH_BELOW`
- `TARGET_HEALTH_BELOW`
- `HAS_MOMENTUM`
- `OWNER_ACTION_IS`
- `TARGET_ACTION_IS`
- `DAMAGE_AT_LEAST`
- `EVENT`
- `META_TRUE`
- `OWNER_IS_ATTACKER`
- `OWNER_IS_DEFENDER`

Si el tipus no existeix, la condició retorna `false`.

---

## ▌Accions disponibles

`PerkRuleFactory.action(...)` converteix les regles JSON en accions de combat.

Accions implementades:

- `MULTIPLY_DAMAGE`
- `ADD_FLAT_DAMAGE`
- `ADD_CRIT_CHANCE`
- `MULTIPLY_CRIT_DAMAGE`
- `DEAL_EXTRA_DAMAGE`
- `HEAL_OWNER`
- `RESTORE_MANA`
- `RESTORE_STAMINA`
- `GAIN_MOMENTUM`
- `APPLY_STATUS`
- `MULTIPLY_NEXT_INCOMING_DAMAGE`
- `SELF_DAMAGE`

Si el tipus no existeix, l'acció no fa res.

---

## ▌Context d'execució

`PerkContext` agrupa la informació necessària per executar una regla.

Conté:

- `HitContext`: dades de l'impacte o fase
- `Phase`: fase actual
- `Random`: generador aleatori
- `Character owner`: propietari de la perk

Això evita passar molts paràmetres separats a cada condició i acció.

---

## ▌Missatges estilitzats

Quan una acció retorna un missatge, `ConfigurablePerkEffect` el reescriu amb el símbol i color de la família de la perk.

Així totes les perks d'una mateixa família mantenen una identitat visual coherent.
