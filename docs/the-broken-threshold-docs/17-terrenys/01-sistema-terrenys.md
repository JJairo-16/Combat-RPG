# ▌Sistema de terrenys

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Els terrenys són efectes globals de partida que s'apliquen als dos combatents per igual.

No són modes de joc ni perks:

- el mode defineix les regles generals de la partida
- una perk recompensa un jugador concret
- un terreny afegeix un escenari comú que es manté com a trigger infinit

Els missatges que produeix un terreny es mostren com a missatges de mode de joc.

---

## ▌Flux

1. `ResourcePreloader` carrega `paths.terrainsConfig`
2. `TerrainRegistry` conserva el catàleg i l'opció `Cap terreny`
3. es creen els dos personatges
4. `GameBootstrap` consulta `UserSettingsRuntime.terrainSelectionMode()`
5. el terreny es resol com a neutre, aleatori o manual
6. `TerrainEffectApplier` afegeix `ConfigurableTerrainEffect` als dos combatents
7. `MatchContext` conserva el terreny perquè el loop el pugui mostrar

```java
ModeEffectApplier.apply(effectiveMode.rules(), p1, p2);
TerrainEffectApplier.apply(effectiveTerrain, p1, p2);

MatchContext matchContext = new MatchContext(effectiveMode, chaosActive, effectiveTerrain);
```

---

## ▌Classes clau

- `terrain/model/TerrainDefinition.java`
- `terrain/io/TerrainLoader.java`
- `terrain/registry/TerrainRegistry.java`
- `terrain/effects/ConfigurableTerrainEffect.java`
- `terrain/effects/TerrainRuleFactory.java`
- `terrain/effects/TerrainTriggerFactory.java`
- `terrain/ui/TerrainSelectionScreen.java`

---

## ▌Selecció de terreny

La decisió de selecció pertany als ajustos d'usuari:

- `NONE`: usa `Cap terreny`
- `MANUAL`: obre el selector interactiu després de crear els personatges
- `RANDOM`: tria un terreny jugable del registre sense obrir selector

```java
return switch (mode) {
    case NONE -> TerrainRegistry.none();
    case RANDOM -> TerrainRegistry.randomPlayable(new Random());
    case MANUAL -> TerrainSelectionScreen.choose(TerrainRegistry.all());
};
```

El selector manual mostra informació del terreny seleccionat i una graella de cartes amb només el nom de cada opció.

---

## ▌Definició JSON

El catàleg viu a `rpg/data/terrains.json`. `TerrainLoader` hi afegeix sempre el terreny neutre; per això no cal declarar `Cap terreny` dins el JSON.

Cada definició ha d'escollir una sola via d'execució:

1. `rules`, per usar el motor genèric
2. `trigger`, per delegar a un trigger personalitzat

No es poden declarar les dues alhora.

```json
{
  "id": "TEST",
  "name": "Terreny de prova",
  "shortDescription": "Escenari neutre per validar triggers globals.",
  "description": "Un escenari simple per comprovar el motor de terrenys.",
  "effectLines": [
    "Els atacs fan un 500% més de dany."
  ],
  "trigger": {
    "file": "TestTerrainTrigger",
    "parameters": {
      "attackDamageMultiplier": 5.0
    }
  }
}
```

Els camps de presentació alimenten el selector i la fitxa que mostra el loop al jugador:

- `name`
- `shortDescription`
- `description`
- `effectLines`

---

## ▌Motor genèric

El motor genèric prepara regles per fase de `HitContext`. Una entrada conté:

- `trigger`
- `conditions`
- `actions`

```json
"rules": {
  "entries": [
    {
      "trigger": "MODIFY_DAMAGE",
      "conditions": [
        { "type": "RANDOM_CHANCE", "params": { "activationPercent": 25 } },
        { "type": "OWNER_IS_ATTACKER", "params": {} }
      ],
      "actions": [
        { "type": "MULTIPLY_DAMAGE", "params": { "multiplier": 1.2 } }
      ]
    }
  ]
}
```

Condicions disponibles:

- `CHANCE` o `RANDOM_CHANCE`
- `OWNER_HEALTH_BELOW`
- `TARGET_HEALTH_BELOW`
- `HAS_MOMENTUM`
- `OWNER_ACTION_IS` i `TARGET_ACTION_IS`
- `OWNER_ACTION_IN` i `OPPONENT_ACTION_IN`
- `DAMAGE_AT_LEAST`
- `OWNER_IS_ATTACKER` i `OWNER_IS_DEFENDER`

Per l'atzar es pot passar `value` com a probabilitat de `0` a `1`, o `percent`, `percentage` o `activationPercent` com a percentatge.

Accions disponibles:

- `MULTIPLY_DAMAGE`
- `ADD_FLAT_DAMAGE`
- `ADD_CRIT_CHANCE`
- `MULTIPLY_CRIT_DAMAGE`
- `HEAL_OWNER`
- `RESTORE_MANA`
- `RESTORE_STAMINA`
- `FORCE_CRITICAL`

Si una condició o una acció necessita inspeccionar un efecte concret o una lògica pròpia massa especialitzada, el terreny ha de delegar en un trigger personalitzat.

---

## ▌Triggers personalitzats

La via `trigger` no registra noms en una taula manual. `TerrainTriggerFactory` agafa `trigger.file`, normalitza el nom de fitxer i busca la classe dins:

```java
private static final String TRIGGER_PACKAGE = "rpgcombat.terrain.effects.triggers";
String className = TRIGGER_PACKAGE + "." + simpleName;
Class<?> rawType = Class.forName(className);
```

La classe ha de ser un `Effect` i pot exposar una d'aquestes signatures:

```java
CustomTerrainTrigger(Map<String, Double> parameters)
CustomTerrainTrigger(TerrainTriggerDefinition definition)
CustomTerrainTrigger()
```

La carpeta reservada per aquests triggers és:

```text
rpg/src/main/java/rpgcombat/terrain/effects/triggers
```

Aquest camí és l'adequat quan el terreny ha de fer més que una combinació de condicions i accions genèriques.

---

## ▌Regles de disseny

- un terreny sempre afecta els dos combatents
- cada combatent rep la seva instància de l'efecte
- el terreny neutre no aplica modificadors
- el selector manual només s'obre en mode de selecció `MANUAL`
- els textos del selector han d'explicar l'efecte sense convertir el terreny en una regla oculta

---

## ▌Proves recomanades

- carregar un catàleg amb `TerrainLoader`
- validar que `rules` i `trigger` no conviuen en la mateixa definició
- provar triggers personalitzats amb els dos combatents
- provar selecció `NONE`, `RANDOM` i `MANUAL` des del bootstrap
