# ▌Rutes i fitxers externs

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`config/paths/PathsConfig.java`

---

## ▌Rutes per defecte

```java
public static final String DEFAULT_WEAPONS_CONFIG = "rpg/data/weapons.json";
public static final String DEFAULT_STATUS_MENU_MODIFIER = "rpg/data/menuModifiers.json";
public static final String DEFAULT_BALANCE_CONFIG = "rpg/data/combatBalance.json";
public static final String DEFAULT_GAME_MODES_CONFIG = "rpg/data/gameModes.json";
public static final String DEFAULT_TERRAINS_CONFIG = "rpg/data/terrains.json";
public static final String DEFAULT_USER_SETTINGS_CONFIG = "rpg/data/userSettings.json";
```

---

## ▌Què es carrega des de fitxer extern?

- definicions d'armes
- modificadors de menú
- balance de combat
- modes de joc
- missions, perks i sinergies
- assoliments i catàleg de descobriments
- terrenys
- valors per defecte dels ajustos d'usuari

---

## ▌Implicació

El projecte està preparat perquè una part important del comportament sigui configurable sense recompilar.

---

## ▌Terrenys i ajustos d'usuari

El catàleg de terrenys es resol amb `paths.terrainsConfig`. Els valors per defecte dels ajustos es llegeixen des de `paths.userSettingsConfig`, mentre que el desament real de l'usuari es resol amb `paths.userSettingsSaveFile`.

```json
{
  "paths": {
    "terrainsConfig": "rpg/data/terrains.json",
    "userSettingsConfig": "rpg/data/userSettings.json",
    "userSettingsSaveFile": "settings.json"
  }
}
```
