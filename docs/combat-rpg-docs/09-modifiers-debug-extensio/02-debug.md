# ▌Debug i crash reports

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes

- `debug/SafeExecutor.java`
- `debug/CrashReportWriter.java`
- `config/debug/*`

---

## ▌Objectiu

Protegir fases crítiques del programa i generar informes detallats quan una execució falla.

---

## ▌Exemple real

```java
private final SafeExecutor executor =
        SafeExecutor.withAutomaticCrashReports(Path.of("rpg/crash-reports"));
```

---

## ▌Com funciona

`SafeExecutor` embolcalla una tasca i retorna un `ExecutionReport` amb:

- èxit o error
- durada
- resultat
- stacktrace detallat
- fitxer de crash report si s'ha pogut escriure

---

## ▌Quan és útil

- bootstrap trencat
- error runtime no controlat
- proves manuals
- diagnòstic en entorns locals
