```md
# Arquitectura general del projecte

## Paquets principals

### `rpgcombat`

Punt d'entrada de l'aplicació.

- `App.java`: arrencada del joc, mode debug i mode normal

### `rpgcombat.game`

Control del loop principal de la partida.

- `GameLoop.java`: demana accions als jugadors, crida el motor de combat i mostra el resultat

### `rpgcombat.combat`

Nucli del combat.

- `CombatSystem.java`: orquestra un round complet
- `AttackResolver.java`: resol com rep el cop el defensor
- `EffectPipeline.java`: executa efectes i passives per fases
- `RoundRecoveryService.java`: bonus de regeneració al final del round
- `Action.java`, `Winner.java`, `CombatRoundResult.java`, `RegenResult.java`

### `rpgcombat.combat.turnservice`

Serveis específics de resolució de torn.

- `TurnResolver.java`: resol un torn individual
- `TurnResult.java`: resultat d'un torn
- `TurnPriorityPolicy.java`: interfície de prioritat
- `DefaultTurnPriorityPolicy.java`: implementació actual

### `rpgcombat.models.characters`

Model principal del combatent.

- `Character.java`
- `Statistics.java`
- `Result.java`
- `Stat.java`

### `rpgcombat.models.breeds`

Races i subclasses concretes de `Character`.

- `Breed.java`: enum de races i bonus racials
- `Orc`, `Elf`, `Dwarf`, `Gnome`, `Tiefling`, `Halfling`

### `rpgcombat.models.weapons`

Sistema d'armes i habilitats.

- `Weapon.java`: encapsula dany, crític, tipus, passives i skill
- `WeaponType.java`: requisits i fórmula de dany base
- `Attack.java`: interfície funcional de skill
- `AttackResult.java`: resultat immutable de l'atac
- `Arsenal.java`: catàleg d'armes
- `Skills.java`: implementacions de skills

### `rpgcombat.models.weapons.passives`

Passives per fases.

- `WeaponPassive.java`: contracte per passives
- `Passives.java`: fàbrica de passives reutilitzables
- `HitContext.java`: context mutable central de la resolució d'un cop

### `rpgcombat.models.effects`

Sistema d'efectes persistents o temporals.

- `Effect.java`
- `EffectState.java`
- `EffectResult.java`
- `StackingRule.java`
- `templates/ConstantDamageEffect.java`

## Idea arquitectònica principal

El projecte separa el combat en tres capes:

1. **decisió externa del jugador**
   - `GameLoop` demana què farà cada jugador

2. **orquestració del round**
   - `CombatSystem` decideix ordre, resol dos torns, calcula guanyador i regeneració

3. **resolució detallada del cop**
   - `TurnResolver` crea i transforma un `HitContext`
   - `AttackResolver` decideix com es rep el dany
   - `EffectPipeline` dispara efectes i passives a cada fase

## Responsabilitats reals

### `GameLoop` no calcula res de combat
Només demana accions i mostra informació.

### `CombatSystem` no calcula skills ni passives
Coordina l'ordre i l'agregació del resultat.

### `TurnResolver` és el cervell del cop
És on es construeix el `HitContext`, es resol crític, es passa per fases i s'aplica defensa.

### `HitContext` és la peça més important per extendre
Si vols afegir mecàniques noves, gairebé sempre tocaràs o llegiràs aquest context.

## Patrons que fa servir el projecte

- **pipeline per fases** per a passives i efectes
- **context mutable** (`HitContext`) per compartir estat entre mòduls
- **interfaces petites** per extendre sense tocar el core:
  - `Attack`
  - `WeaponPassive`
  - `Effect`
  - `TurnPriorityPolicy`
- **subclasses de `Character`** per representar diferències racials específiques
```
