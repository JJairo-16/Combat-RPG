# ▌Crida espiritual

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe

`utils/rng/SpiritualCallingDie.java`

---

## ▌Paper dins del sistema

Gestiona la tirada especial de la crida espiritual, incloent:

- resultat del dau
- percentatge de curació
- influència suau del carisma i la sort
- animació de D20

---

## ▌Detalls visibles al codi

```java
private static final double MIN = 0.07;
private static final double MAX = 0.20;
````

Això implica que la curació escala aproximadament entre el **7%** i el **20%**.

---

## ▌Lògica de tirada

* la base és una campana suau (no uniforme)
* hi ha pics direccionals ocasionals
* la sort pot donar avantatge
* el carisma no força el resultat, però n’inclina la distribució

---

## ▌Distribució del dau

El dau de la crida espiritual **no és completament uniforme**.

En lloc de tenir la mateixa probabilitat per a tots els valors:

* hi ha una **lleugera concentració al centre**
* els valors extrems són menys freqüents
* el comportament s’aproxima a una **distribució en campana suau**

Això aporta:

* més consistència en el resultat
* menys volatilitat extrema constant
* millor control del balance

---

## ▌Biaix base del sistema

Per defecte, la tirada està **lleugerament inclinada cap a resultats baixos**.

Objectiu:

* evitar que la mecànica sigui massa fiable
* mantenir el risc com a factor clau
* impedir curacions massa consistents

Aquest biaix forma part del disseny base del sistema RNG.

---

## ▌Influència de la sort

La estadística de **Sort (Luck)** actua com a mecanisme de compensació:

* redueix el biaix negatiu
* incrementa la probabilitat de resultats alts
* pot activar avantatge en la tirada

No garanteix bons resultats, però:

* desplaça la distribució cap amunt
* fa més probables els resultats favorables

---

## ▌Tirada salvatge

Existeix una probabilitat de generar una **tirada salvatge**.

Característiques:

* crea un pic sobtat en la distribució
* pot resultar en un valor molt alt o molt baix
* trenca temporalment la campana base

La direcció del pic:

* es decideix de forma aleatòria
* té una **lleugera tendència negativa (cap avall)**

Això reforça:

* la tensió
* la imprevisibilitat
* moments clutch o fallides crítiques

---

## ▌Afinitat divina

L’afinitat amb els déus pot influir indirectament en el resultat:

* pot reduir el biaix negatiu
* pot augmentar la probabilitat de pics positius
* no elimina la incertesa

No és un control directe, sinó un **factor contextual** que modifica subtilment la tirada.

---

## ▌Resum de comportament

El sistema combina diversos factors:

* distribució no uniforme (campana suau)
* biaix base negatiu
* compensació per sort
* pics aleatoris (tirades salvatges)
* modificadors contextuals (afinitat divina)

El resultat és un RNG que:

* no és completament aleatori
* no és totalment controlable
* manté equilibri entre risc i recompensa

---

## ▌Bona decisió de disseny

El sistema evita tant:

* la uniformitat pura (massa previsible)
* com el caos total (massa frustrant)

Aconseguint un punt intermig on:

* el jugador pot influir en el resultat
* però mai controlar-lo completament
