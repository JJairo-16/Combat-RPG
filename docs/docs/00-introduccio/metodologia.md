```md
# Metodologia d'aquesta documentació

Aquesta documentació s'ha escrit analitzant el codi del fitxer ZIP del projecte Java.

## Fitxers clau revisats

- `App.java`
- `game/GameLoop.java`
- `combat/CombatSystem.java`
- `combat/AttackResolver.java`
- `combat/EffectPipeline.java`
- `combat/RoundRecoveryService.java`
- `combat/turnservice/TurnResolver.java`
- `combat/turnservice/DefaultTurnPriorityPolicy.java`
- `models/characters/Character.java`
- `models/characters/Statistics.java`
- `models/breeds/*`
- `models/weapons/Weapon.java`
- `models/weapons/WeaponType.java`
- `models/weapons/Attack.java`
- `models/weapons/AttackResult.java`
- `models/weapons/Arsenal.java`
- `models/weapons/Skills.java`
- `models/weapons/passives/*`
- `models/effects/*`
- `creator/CharacterCreator.java`

## Què no fa aquesta documentació

No documenta a fons:

- detalls purament visuals de consola
- caches de render
- totes les utilitats de menú i entrada

Però sí documenta tot el que cal per entendre i ampliar el sistema de combat del projecte.
```
