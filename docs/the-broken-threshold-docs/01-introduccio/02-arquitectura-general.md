# ▌Arquitectura general

[← Tornar a l'índex](../INDEX.md)

---

## ▌Visió d'alt nivell

L'aplicació segueix aquest recorregut general:

```text
App
 └── SafeExecutor
     └── GameBootstrap
         ├── AppConfigLoader
         ├── Arsenal.preload(...)
         ├── StatusModLoader.load(...)
         ├── CombatBalanceRegistry.initialize(...)
         └── GameLoop
             ├── MenuCenter
             └── CombatSystem
```

---

## ▌Responsabilitats per capa

### ▌Arrencada
`App` i `GameBootstrap` preparen l'entorn, carreguen la configuració i construeixen una partida llesta per jugar.

### ▌Core del joc
`GameLoop` coordina els torns de jugador i pregunta accions via menús.

### ▌Domini de combat
`CombatSystem`, `TurnResolver` i `AttackResolver` apliquen regles, resolen dany, efectes i regeneració.

### ▌Domini de personatge
`Character` i `Statistics` mantenen l'estat viu del combatent.

### ▌Catàleg d'armes
`WeaponLoader`, `WeaponDefinition`, `AttackRegistry` i `PassiveFactory` permeten que gran part del comportament es construeixi des de configuració.

---

## ▌Patrons útils dins del projecte

- **Registry**: `CombatBalanceRegistry`, `AttackRegistry`, `StatusModActionRegistry`
- **Factory**: `PassiveFactory`, `StatusModFactory`
- **Template / default interface methods**: `Effect`, `WeaponPassive`
- **DTOs / records**: configuracions i resultats de combat
- **Preload + runtime instance**: `WeaponDefinition` crea una `Weapon` viva quan cal

---

## ▌Conseqüència pràctica

Si vols modificar una funcionalitat, primer identifica si pertany a:

- estat persistent del personatge
- lògica d'una ronda
- una arma concreta
- un efecte/passiva
- una regla global de balance
- un menú o flux d'entrada

Això et porta ràpidament al paquet correcte.
