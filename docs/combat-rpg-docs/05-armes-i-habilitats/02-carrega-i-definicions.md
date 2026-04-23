# ▌Càrrega i definicions d'armes

[← Tornar a l'índex](../INDEX.md)

---

## ▌Lector

`weapons/WeaponLoader.java`

Fa servir Gson per llegir un JSON extern i convertir-lo a `WeaponConfig[]`.

---

## ▌Flux de càrrega

```java
WeaponConfig[] configs = GSON.fromJson(reader, WeaponConfig[].class);

List<WeaponDefinition> list = new ArrayList<>(configs.length);
for (WeaponConfig config : configs) {
    list.add(toDefinition(config));
}
```

---

## ▌DTO del JSON

`WeaponConfig` és un `record` molt directe:

- `id`
- `name`
- `description`
- `baseDamage`
- `criticalProb`
- `criticalDamage`
- `type`
- `manaPrice`
- `attackSkill`
- `passives`

---

## ▌Implicació arquitectònica

L'arma no s'escriu necessàriament a mà en Java.
Moltes variants es poden definir per configuració sempre que:

- existeixi l'`attackSkill`
- existeixin les passives corresponents
- el tipus sigui vàlid

---

## ▌Què revisar si una arma no carrega

- JSON d'armes
- `WeaponType.valueOf(cfg.type())`
- `AttackRegistry.resolve(attackSkill)`
- `PassiveFactory.create(...)`
