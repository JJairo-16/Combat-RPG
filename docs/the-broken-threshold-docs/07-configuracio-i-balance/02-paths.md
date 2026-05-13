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
```

---

## ▌Què es carrega des de fitxer extern?

- definicions d'armes
- modificadors de menú
- balance de combat

---

## ▌Implicació

El projecte està preparat perquè una part important del comportament sigui configurable sense recompilar.

---

## ▌Atenció

Al zip analitzat no s'han inclòs aquests JSON de dades, però el codi deixa clarament definides les seves rutes i responsabilitats.
