# ▌Configuració d'aplicació

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes

- `config/app/AppConfig.java`
- `config/app/AppConfigLoader.java`
- `config/app/AppConfigProfiles.java`

---

## ▌Què agrupa `AppConfig`

- `paths`
- `ui`
- `cinematic`
- `gameMode`
- `debug`
- `characters`
- `homeScreen`

És la configuració arrel de l'aplicació.

Els ajustos de jugador que es poden canviar des del menú no viuen dins `AppConfig`. Es llegeixen a partir de `paths.userSettingsConfig` i es desen a `paths.userSettingsSaveFile`.

Ara mateix aquests ajustos inclouen:

- visibilitat dels missatges d'impuls
- forma de triar el terreny (`NONE`, `MANUAL`, `RANDOM`)

---

## ▌Càrrega

`AppConfigLoader` permet dues maneres:

- carregar un JSON complet
- activar un perfil (`useProfile`)

```java
boolean useProfile = getBoolean(json, "useProfile", false);

if (useProfile) {
    return AppConfigProfiles.from(getProfile(json));
}
```

---

## ▌Avantatge

Això fa que el joc pugui arrancar ràpidament en:

- mode normal
- mode debug
- perfils personalitzats

sense editar manualment tots els camps del JSON.
