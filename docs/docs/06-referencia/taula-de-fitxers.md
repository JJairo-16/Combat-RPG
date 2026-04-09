```md
# Taula de fitxers i responsabilitats

## Entrypoint i loop

- `App.java` — arrencada de l'aplicació
- `game/GameLoop.java` — bucle principal de la partida

## Combat core

- `combat/Action.java` — accions base del combat
- `combat/CombatSystem.java` — coordinació d'un round
- `combat/AttackResolver.java` — aplicació del dany segons defensa
- `combat/EffectPipeline.java` — execució d'efectes i passives
- `combat/RoundRecoveryService.java` — bonus defensius de regeneració
- `combat/CombatRoundResult.java` — resultat agregat del round
- `combat/RegenResult.java` — recuperació de vida i mana
- `combat/EndRoundRegenBonus.java` — acumulador de bonus percentual
- `combat/Winner.java` — estat de victòria

## Turn service

- `combat/turnservice/TurnResolver.java` — resolució completa d'un torn
- `combat/turnservice/TurnPriorityPolicy.java` — contracte de prioritat
- `combat/turnservice/DefaultTurnPriorityPolicy.java` — implementació actual
- `combat/turnservice/TurnResult.java` — resultat d'un torn

## Models de personatge

- `models/characters/Character.java` — personatge base
- `models/characters/Statistics.java` — estadístiques i recursos
- `models/characters/Stat.java` — enum d'estadístiques
- `models/characters/Result.java` — resultat simple d'una defensa/impacte

## Races

- `models/breeds/Breed.java` — enum de races
- `models/breeds/Orc.java`
- `models/breeds/Elf.java`
- `models/breeds/Dwarf.java`
- `models/breeds/Gnome.java`
- `models/breeds/Tiefling.java`
- `models/breeds/Halfling.java`

## Sistema d'armes

- `models/weapons/Weapon.java`
- `models/weapons/WeaponType.java`
- `models/weapons/Attack.java`
- `models/weapons/AttackResult.java`
- `models/weapons/Target.java`
- `models/weapons/Skills.java`
- `models/weapons/Arsenal.java`

## Passives

- `models/weapons/passives/WeaponPassive.java`
- `models/weapons/passives/Passives.java`
- `models/weapons/passives/HitContext.java`

## Efectes

- `models/effects/Effect.java`
- `models/effects/EffectState.java`
- `models/effects/EffectResult.java`
- `models/effects/StackingRule.java`
- `models/effects/templates/ConstantDamageEffect.java`

## Creació de personatges

- `creator/CharacterCreator.java`

## Utilitats

- `utils/input/...` — entrada per consola i menús
- `utils/ui/...` — render i helpers visuals
- `utils/rng/...` — generació de stats i codi del grimori
- `utils/cache/...` — caches de render
```
