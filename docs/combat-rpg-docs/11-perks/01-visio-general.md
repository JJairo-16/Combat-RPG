# ▌Sistema de perks

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

El sistema de perks afegeix recompenses permanents durant el combat a partir de missions assignades als jugadors.

Cada jugador rep una missió inicial. Quan la completa:

1. es marca el progrés com a complet
2. es genera una elecció de perks
3. el jugador tria una perk des del menú
4. la perk es converteix en un efecte actiu persistent

Els efectes s’avaluen en fases concretes del combat mitjançant triggers.

---

## ▌Flux general

1. assignació de missió
2. actualització de progrés per esdeveniments
3. completat de missió
4. generació d’elecció de perks
5. selecció del jugador
6. aplicació d’efectes
7. execució durant el combat

---

## ▌Classes clau

- `perks/CombatPerkSystem.java`
- `perks/PlayerPerkState.java`
- `perks/PerkRegistry.java`
- `perks/effect/PerkEffectFactory.java`
- `perks/mission/MissionRegistry.java`

---

## ▌Responsabilitats

- `CombatPerkSystem`: coordinació global
- `PlayerPerkState`: estat per jugador
- `PerkRegistry`: registre de perks disponibles
- `MissionRegistry`: registre de missions
- `PerkEffectFactory`: creació d’efectes actius

---

## ▌Notes

- el sistema és totalment data-driven (JSON)
- perks i missions es poden afegir sense modificar codi
- els efectes són desacoblats i composables