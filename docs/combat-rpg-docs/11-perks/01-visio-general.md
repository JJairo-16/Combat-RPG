# ▌Sistema de perks

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

El sistema de perks afegeix recompenses permanents durant el combat a partir de missions assignades als jugadors.

Cada jugador rep una missió inicial. Quan la completa, el joc obre un menú de selecció i el jugador tria una perk. La perk escollida es converteix en un efecte permanent que s'avalua en fases concretes del combat.

---

## ▌Classes clau

- `perks/CombatPerkSystem.java`
- `perks/PlayerPerkState.java`
- `perks/PerkDefinition.java`
- `perks/PerkFamily.java`
- `perks/PerkRegistry.java`
- `perks/PerkLoader.java`
- `perks/PerkChoiceMenu.java`
- `perks/effect/ConfigurablePerkEffect.java`
- `perks/effect/PerkRuleFactory.java`
- `perks/mission/MissionProgress.java`
- `perks/mission/MissionRegistry.java`
- `perks/mission/MissionUpdate.java`

---

## ▌Flux general

1. `CombatPerkSystem` crea un estat de perks per a cada jugador.
2. Cada estat rep una `MissionProgress` amb una missió seleccionada per `MissionRegistry`.
3. Després de cada torn, `afterTurn(...)` converteix el resultat del torn en una `MissionUpdate`.
4. La missió actualitza el seu progrés segons el tipus d'objectiu.
5. Quan la missió es completa, `PlayerPerkState` marca una elecció pendent.
6. `resolvePendingChoices(...)` genera opcions amb `PerkRegistry`.
7. `PerkChoiceMenu` mostra les opcions i retorna la perk triada.
8. `PerkEffectFactory` transforma la perk en un `Effect` i l'afegeix al personatge.

---

## ▌Responsabilitat principal

El sistema separa tres conceptes:

- la missió, que decideix quan el jugador ha guanyat una recompensa
- la perk, que descriu quina recompensa es pot aplicar
- l'efecte, que executa la recompensa durant el combat

Aquesta separació permet afegir noves perks o noves missions sense tocar el bucle principal del combat.
