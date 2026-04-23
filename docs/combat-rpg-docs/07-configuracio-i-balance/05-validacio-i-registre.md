# ▌Validació i registre global

[← Tornar a l'índex](../INDEX.md)

---

## ▌Registre global

`CombatBalanceRegistry` guarda la configuració de combat activa.

---

## ▌Comportament

- `initialize(config)` només permet inicialitzar una vegada
- `replace(config)` substitueix la configuració
- `get()` falla si encara no s'ha inicialitzat
- `clearForTests()` existeix per a entorns de prova

```java
public static CombatBalanceConfig get() {
    if (current == null) {
        throw new CombatBalanceException("L'equilibri de combat no està inicialitzat");
    }
    return current;
}
```

---

## ▌Validació

Abans de quedar registrada, la configuració passa per `CombatBalanceValidator`.

Això és important perquè el sistema assumeix que els valors globals són coherents.

---

## ▌Conseqüència pràctica

Si un nou mòdul necessita configuració global de combat, el patró correcte és:

1. afegir camp al config
2. validar-lo
3. exposar-lo via registre
4. consumir-lo des de les classes de domini
