# Modes de joc

Els modes de joc es defineixen per dades a `rpg/data/gameModes.json` i es carreguen abans de crear els personatges. Això permet decidir les regles, el Caos i la cinemàtica inicial abans que comenci la partida.

El fitxer que es carrega es configura a `paths.gameModesConfig` dins `rpg/data/appConfig.json`. La selecció de mode es controla amb `gameMode.selectionEnabled` i `gameMode.defaultMode`.

## Estructura mínima

```json
{
  "id": "NORMAL",
  "name": "Normal",
  "description": "Experiència completa.",
  "presentation": {
    "shortDescription": "El duel sense concessions.",
    "details": [
      "El combat conserva totes les formes conegudes",
      "Les armes trobades poden tornar a aparèixer",
      "El Caos pot escoltar"
    ],
    "lockedTitle": "???",
    "lockedDescription": "Pacte encara sense nom",
    "lockedHints": [
      "El primer camí sempre roman obert."
    ]
  },
  "unlock": {
    "mode": "ALL",
    "requirements": []
  },
  "rules": {
    "specialActionsEnabled": true,
    "maxPerks": 4,
    "divinePerksEnabled": true,
    "showUnlockableWeapons": true,
    "chaos": {
      "activation": "PROBABILITY",
      "probability": 0.15
    },
    "modeEffects": []
  },
  "cinematics": {
    "postCreationPool": ["DEFAULT_RANDOM"],
    "chaosPostCreation": "CHAOS_MIND"
  }
}
```

## Regles disponibles

- `allowedActions`: llista opcional d'accions permeses (`ATTACK`, `DEFEND`, `DODGE`, `CHARGE`). Si no existeix o és buida, no hi ha restriccions.
- `specialActionsEnabled`: activa o bloqueja accions especials de menú.
- `maxPerks`: límit de perks per jugador durant la partida.
- `divinePerksEnabled`: permet o bloqueja perks divines durant la creació.
- `showUnlockableWeapons`: si és `false`, el selector d'armes no mostra armes amb regla de desbloqueig.
- `chaos`: política de Caos del mode, independent de cinemàtiques.
- `modeEffects`: efectes inicials aplicats als jugadors quan es crea la partida.

## Presentació al menú

`presentation` forma part de cada definició del mode i alimenta el menú interactiu de cartes. El menú no inventa textos: només decideix si mostrar la versió desbloquejada o la bloquejada.

- `shortDescription`: frase curta quan el mode està disponible.
- `details`: trets visibles quan el mode està disponible.
- `lockedTitle`: títol mostrat quan el mode encara està bloquejat.
- `lockedDescription`: frase curta del bloqueig.
- `lockedHints`: indicis o pistes de desbloqueig.

Els textos han de mantenir un to una mica ambigu, semblant al vocabulari de descobriments i assoliments.

## Caos per mode

`chaos.activation` pot ser:

- `DISABLED`: el mode no pot entrar en Caos.
- `PROBABILITY`: usa `probability` com a probabilitat d'activació.
- `FORCED`: activa Caos sempre.

La decisió es fa a `ChaosPolicy` i s'aplica als personatges abans de crear el `GameLoop`. Les cinemàtiques només consulten el resultat ja calculat.

## Desbloqueig del mode

`unlock.requirements` usa el mateix model genèric de desbloqueig que altres sistemes:

```json
"unlock": {
  "mode": "ALL",
  "requirements": [
    { "type": "ACHIEVEMENT", "id": "FIRST_WIN" },
    { "type": "DISCOVERY", "category": "ACTIONS", "id": "CHARGE" }
  ]
}
```

`mode` pot combinar requisits segons el valor de `UnlockMode`. Un mode sense requisits queda disponible des de l'inici.

## Efectes de mode

Els efectes de mode són triggers inicials declarats a `modeEffects`. Són útils per crear variants més flexibles que simples multiplicadors.

```json
"modeEffects": [
  {
    "id": "BLEED_EMPHASIS",
    "target": "BOTH",
    "parameters": {
      "damageMultiplier": 1.2,
      "criticalBleedTurns": 3,
      "deepCutBleedTurns": 2
    }
  }
]
```

`target` pot ser `BOTH`, `PLAYER1` o `PLAYER2`. Cada jugador rep una instància nova de l'efecte.

Efectes disponibles:

- `BLEED_EMPHASIS`: dona més pes al sagnat.
- `SELF_DIRECTED_ATTACK`: redirigeix atacs del portador contra si mateix. Està pensat per proves o modes experimentals; no s'ha de deixar en `gameModes.json` si no es vol exposar al menú normal.

Per afegir un efecte nou:

1. Crear una classe `Effect` o `Trigger` a `rpgcombat.models.effects`.
2. Registrar-la a `ModeEffectFactory`.
3. Afegir-ne un test a `GameModeRulesTest` o a una prova específica del comportament.
4. Declarar-la a `modeEffects` només en els modes que l'hagin d'usar.

## Cinemàtiques de mode

`cinematics.postCreationPool` defineix les cinemàtiques que poden aparèixer després de crear personatges. Si el mode activa Caos i defineix `chaosPostCreation`, aquesta clau té prioritat.

Les claus actuals especials són:

- `DEFAULT_RANDOM`: usa la cinemàtica d'inici normal aleatòria.
- `CHAOS_MIND`: inici especial quan el mode entra en Caos.
- `BEGINNER_INTRO`: inici personalitzat del mode principiant.

Per afegir una clau nova, ampliar `CinematicBuilder.buildByKey`.

## Mode principiant

El mode `BEGINNER` és l'exemple de mode restringit:

- `allowedActions`: `ATTACK`, `DEFEND`, `DODGE`.
- `specialActionsEnabled`: `false`.
- `maxPerks`: `1`.
- `divinePerksEnabled`: `false`.
- `showUnlockableWeapons`: `false`.
- `chaos.activation`: `DISABLED`.

Aquest patró és el punt de partida recomanat per crear modes de tutorial o variants reduïdes.
