```md
# `CombatSystem`

## Responsabilitat

`CombatSystem` és l'orquestrador d'un round complet entre dos personatges. No implementa ell mateix la lògica detallada de crític, passives o habilitats; en lloc d'això, coordina serveis especialitzats.

## Dependències internes

A la construcció crea:

```java
private final CombatRenderer renderer = new CombatRenderer();
private final AttackResolver attackResolver = new AttackResolver();
private final EffectPipeline effectPipeline = new EffectPipeline();
private final RoundRecoveryService recoveryService = new RoundRecoveryService();
private final TurnResolver turnResolver =
    new TurnResolver(attackResolver, effectPipeline, recoveryService);
```

A més, rep una `TurnPriorityPolicy` que per defecte és `DefaultTurnPriorityPolicy`.

## API pública

### `play(Action a1, Action a2)`

És el mètode que usa `GameLoop`.

Fa dues coses:

1. obté un `CombatRoundResult` amb `playRound`
2. pinta per consola el resultat del round i retorna el `Winner`

### `playRound(Action a1, Action a2)`

És el mètode realment interessant a nivell de domini.

## Desglossament exacte de `playRound`

### 1. Captura de l'estat inicial

```java
double p1HealthBefore = p1Stats.getHealth();
double p2HealthBefore = p2Stats.getHealth();
```

També crea dos `EndRoundRegenBonus`, un per jugador.

Aquests objectes acumulen bonus percentual de vida i mana que s'aplicaran al final del round si la defensa o esquiva han estat reeixides.

### 2. Decisió d'ordre de torn

```java
boolean p1First = priorityPolicy.player1First(player1, a1, player2, a2, combatRng);
```

Això determina quin `resolveTurn` s'executa primer.

### 3. Resolució dels dos torns

Cas 1: `player1` va primer.

```java
firstTurn = turnResolver.resolveTurn(player1, player2, a1, a2, p2Bonus);
secondTurn = turnResolver.resolveTurn(player2, player1, a2, a1, p1Bonus);
```

Observa un detall important:

- el cinquè argument és el **bonus del defensor**
- si `player1` ataca `player2`, qualsevol bonus per defensa/esquiva exitosa s'acumula a `p2Bonus`

### 4. Càlcul del dany rebut real

Després de resoldre els dos torns, es mira la vida restant i es calcula el dany rebut:

```java
double p1DamageTaken = p1HealthBefore - p1HealthAfterAttacks;
double p2DamageTaken = p2HealthBefore - p2HealthAfterAttacks;
```

No es basa només en `TurnResult`, sinó en l'estat real dels `Statistics`.

### 5. Resolució de guanyador

```java
Winner winner = resolveWinner(player1, player2);
```

Regles:

- si tots dos vius: `NONE`
- si només viu P1: `PLAYER1`
- si només viu P2: `PLAYER2`
- si tots dos morts: `TIE`

### 6. Regeneració de final de round

Només s'executa si el guanyador és `NONE`.

Primer guarda vida i mana prèvies. Després:

```java
player1.regen();
player2.regen();

recoveryService.applyEndRoundBonus(player1, p1Bonus);
recoveryService.applyEndRoundBonus(player2, p2Bonus);
```

Això significa que hi ha dues capes de recuperació:

1. regeneració natural del personatge (`Character.regen`)
2. bonus addicionals per haver defensat o esquivat bé

## Què no fa `CombatSystem`

No fa directament cap d'aquestes coses:

- no calcula dany base de les armes
- no resol crítics
- no aplica passives d'arma
- no executa `Effect`
- no fa `DEFEND` ni `DODGE`
- no selecciona objectiu real d'un `AttackResult`

Tot això passa més avall.

## Conseqüència arquitectònica

Per extendre el sistema:

- si vols canviar l'ordre de torns, toca `TurnPriorityPolicy`
- si vols canviar com es resol un cop, toca `TurnResolver`
- si vols canviar defensa o esquiva, toca `AttackResolver` i/o `Character`
- si vols afegir capes de post-processat, toca `EffectPipeline`
```
