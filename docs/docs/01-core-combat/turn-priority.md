```md
# Prioritat de torn: `TurnPriorityPolicy`

## Contracte

La interfície és molt simple:

```java
boolean player1First(Character p1, Action a1, Character p2, Action a2, Random rng);
```

Retorna `true` si el jugador 1 ha d'actuar abans.

## Implementació actual: `DefaultTurnPriorityPolicy`

La lògica actual té dues "lligues" de velocitat:

### Lliga ràpida
- `DEFEND`
- `DODGE`

### Lliga normal
- `ATTACK`

Per tant:

- si un jugador defensa o esquiva i l'altre ataca, el que defensa/esquiva va primer
- si tots dos estan a la mateixa lliga, es calcula iniciativa

## Fórmula d'iniciativa

```java
initiative = dexterity * 1.0 + luck * 0.25 + random(0..10)
```

### Implicacions reals

- la destresa és l'estadística principal de velocitat
- la sort ajuda, però menys
- hi ha un petit factor aleatori perquè no hi hagi empats constants

## Casos pràctics

### Cas 1: `ATTACK` vs `DEFEND`
Guanya sempre `DEFEND`.

### Cas 2: `ATTACK` vs `DODGE`
Guanya sempre `DODGE`.

### Cas 3: `DEFEND` vs `DODGE`
Mateixa lliga: es decideix per iniciativa.

### Cas 4: `ATTACK` vs `ATTACK`
Mateixa lliga: es decideix per iniciativa.

## Com reemplaçar la política

Només cal implementar la interfície i injectar-la a `CombatSystem`.

```java
public class MyPriorityPolicy implements TurnPriorityPolicy {
    @Override
    public boolean player1First(Character p1, Action a1, Character p2, Action a2, Random rng) {
        // lògica pròpia
        return true;
    }
}
```

I crear el combat així:

```java
CombatSystem combat = new CombatSystem(player1, player2, new MyPriorityPolicy());
```

## Quan hauries de tocar això

Toca aquesta capa si vols:

- accions amb prioritat especial
- iniciativa basada en arma equipada
- estats com "stun", "slow", "haste"
- sistema de velocitat més complex per barra de temps o energia
```
