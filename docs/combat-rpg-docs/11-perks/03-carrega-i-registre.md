# ▌Càrrega i registre de perks

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `perks/PerkLoader.java`
- `perks/PerkRegistry.java`
- `perks/config/PerkConfig.java`

---

## ▌Càrrega des de JSON

`PerkLoader` llegeix un fitxer JSON i transforma cada entrada en una `PerkDefinition`.

El procés és:

1. obrir el fitxer amb UTF-8
2. parsejar-lo amb Gson
3. ignorar entrades nul·les o sense `id`
4. transformar cada `PerkConfig` en `PerkDefinition`
5. retornar una llista immutable

---

## ▌Exemple real

```java
public static List<PerkDefinition> load(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        PerkConfig[] raw = GSON.fromJson(reader, PerkConfig[].class);
        if (raw == null) return List.of();

        List<PerkDefinition> result = new ArrayList<>(raw.length);
        for (PerkConfig cfg : raw) {
            if (cfg == null || cfg.id() == null || cfg.id().isBlank()) continue;
            result.add(toDefinition(cfg));
        }
        return List.copyOf(result);
    }
}
```

---

## ▌Valors per defecte

Durant la transformació, el loader aplica valors segurs:

- `name`: si és buit, usa l'`id`
- `description`: si és buida, usa una cadena buida
- `family`: si és buida, usa `STRATEGY`
- `trigger`: si és buit, usa `AFTER_HIT`
- `weight`: si és nul, usa `1`; si és menor que `1`, es força a `1`
- `conditions` i `actions`: si són buides, es converteixen en llistes buides

---

## ▌Validació d'enums

Els camps que apunten a enums es validen amb `Enum.valueOf(...)`.

Si el valor no existeix, es llença una `IllegalArgumentException` amb el camp i l'identificador de la perk.

Això evita que una configuració incorrecta falli silenciosament durant el combat.

---

## ▌Registre global

`PerkRegistry` manté la llista global de perks carregades.

```java
public static void initialize(List<PerkDefinition> loaded) {
    perks = loaded == null ? List.of() : List.copyOf(loaded);
}
```

El registre no carrega fitxers directament. Només guarda les definicions que li passen des del bootstrap o des del sistema de configuració.

---

## ▌Generació d'opcions

`rollOptions(...)` genera les opcions que es mostraran al jugador.

En mode normal intenta oferir:

- una perk d'estratègia
- una perk de sort
- una perk de caos

Si falten opcions, completa la llista amb perks no corruptes.

En mode corrupte només ofereix perks de la família `CORRUPTED`.

---

## ▌Selecció per pes

La selecció aleatòria usa el camp `weight`.

Les perks amb més pes tenen més probabilitat d'aparèixer, però no es repeteixen dins d'una mateixa tirada d'opcions.
