```md
# Punts delicats i detalls importants del codi actual

Aquest fitxer no critica el projecte; serveix per fer explícites algunes particularitats que és fàcil passar per alt quan t'hi adaptes.

## 1. `Weapon.basicAttack()` ja resol crític intern

Però després `TurnResolver.configureHitContext()` reconstrueix el dany base i torna a preparar crític dins `HitContext`.

### Implicació
El sistema està en una situació híbrida:

- part del crític es calcula a l'arma
- el motor modern ho vol controlar via context

Quan facis canvis profunds al crític, revisa tant `Weapon` com `HitContext`.

## 2. `TurnResolver` resol dos torns abans de decidir vencedor

Això permet:

- empats
- contraatacs encara que el primer atac hagi matat

Si algun dia vols un sistema on morir abans impedeixi actuar, hauràs de canviar `CombatSystem.playRound`.

## 3. `arcaneDisruption` consumeix mana més d'una vegada

Actualment:

- comprova si hi ha prou mana amb `consumeMana`
- després torna a consumir `baseManaCost`
- si és crític, torna a consumir un extra del 50%

Això pot ser voluntari o no, però és una realitat del codi actual.

## 4. `crossCut` no crea dos hits independents al pipeline

Suma dos danys dins la skill i retorna un únic `AttackResult`.

### Implicació
Passives i efectes veuen un únic cop gran, no dos cops separats.

## 5. `trueHarm` aplica dany fora del `damageDealt` principal

La passiva fa:

```java
ctx.defender().getStatistics().damage(extra);
```

Això significa que part del dany pot no quedar reflectit dins `ctx.damageDealt()` com a dany principal del cop.

## 6. `Character` combina model i lògica de combat

No és només una entitat de dades; també sap:

- atacar
- defensar
- esquivar
- regenerar
- gestionar efectes

Això és útil i pràctic, però cal tenir-ho en compte si algun dia es vol desacoblar més.

## 7. Hi ha dues representacions de "raça"

- `Breed` com a enum
- subclasses concretes de `Character`

Això fa el sistema flexible, però obliga a tocar més d'un lloc quan s'afegeix una raça nova.

## 8. `Target` actualment només s'usa com enemic o self

No hi ha model per:

- múltiples enemics
- aliats
- àrea
- objectius seleccionables

Si el projecte creix cap a combat grupal, aquesta serà una àrea important a redissenyar.

## 9. Els logs de combat estan molt integrats al flux

Molts components retornen `String` o llistes de missatges.

### Avantatge
És fàcil mostrar què ha passat.

### Cost
Si algun dia vols desacoblar UI i domini, potser voldràs passar a un model d'events o descriptors en lloc de text renderitzat tan aviat.

## 10. El sistema d'efectes està preparat per créixer

Tot i que el projecte actual no sembla explotar-lo al màxim, la presència de:

- `EffectState`
- `StackingRule`
- fases
- events
- prioritats

indica que és la millor base per construir mecàniques avançades.
```
