# ▌Model de perk

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/PerkDefinition.java`
- `perks/PerkFamily.java`
- `perks/config/PerkConfig.java`

---

## ▌`PerkDefinition`

`PerkDefinition` és el model immutable que representa una perk ja carregada i validada.

Conté:

- `id`: identificador únic
- `name`: nom visible
- `description`: text descriptiu
- `family`: família visual i lògica
- `trigger`: fase de combat que activa la perk
- `weight`: pes per a selecció aleatòria
- `conditions`: condicions que s'han de complir
- `actions`: accions que s'executen si les condicions passen

---

## ▌Exemple real

```java
public record PerkDefinition(
        String id,
        String name,
        String description,
        PerkFamily family,
        Phase trigger,
        int weight,
        List<Rule> conditions,
        List<Rule> actions) {
}
```

---

## ▌Regles configurables

Cada condició o acció es representa amb una `Rule`.

```java
public record Rule(String type, Map<String, Object> params) {}
```

Això permet que el JSON defineixi comportaments sense crear una classe nova per a cada perk.

---

## ▌Famílies de perks

`PerkFamily` agrupa les perks per categoria i defineix la seva presentació visual.

Famílies disponibles:

- `STRATEGY`: perks d'estratègia
- `LUCK`: perks de sort
- `CHAOS`: perks de caos
- `CORRUPTED`: perks corruptes

Cada família té:

- etiqueta visible
- símbol de missatge
- color de missatge
