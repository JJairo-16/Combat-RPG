# ▌Armes desbloquejables

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Les armes desbloquejables permeten que el catàleg d'armes creixi amb el progrés global del jugador sense convertir les armes tardanes en millores directes.

El sistema separa clarament dues idees:

- aparèixer al grimori de descobriments
- estar disponible a la selecció d'armes

Totes les armes poden aparèixer al grimori. Només les armes disponibles poden aparèixer a la selecció.

---

## ▌Regla principal

El catàleg complet d'armes existeix sempre.

El filtre de desbloqueig només s'aplica quan el jugador ha de triar o equipar una arma.

Això evita que una arma bloquejada desaparegui del sistema de descobriments i permet mostrar pistes abans que pugui ser triada.

---

## ▌Flux general

1. `weapons.json` defineix totes les armes
2. `WeaponLoader` carrega totes les definicions
3. `Arsenal` manté el catàleg complet
4. `DiscoveryCatalog` genera o rep entrades per totes les armes
5. el sistema de desbloqueig avalua quines armes estan disponibles
6. la selecció d'armes mostra només les disponibles
7. el grimori mostra totes les armes segons el seu estat de descobriment

---

## ▌Estats d'una arma

Una arma pot estar en tres estats pràctics:

- no disponible i no descoberta
- disponible però no descoberta
- descoberta

Aquests estats afecten només la presentació i la selecció, no el funcionament de l'arma en combat.

---

## ▌Responsabilitats

- `weapons.json`: defineix armes i requisits de desbloqueig
- sistema d'unlock: avalua disponibilitat
- selecció d'armes: filtra armes no disponibles
- descobriments: mostra totes les armes amb la pista adequada
- combat: usa l'arma equipada sense conèixer si abans estava bloquejada

---

## ▌Principi de balance

En un joc de combats jugador contra jugador, desbloquejar armes ha d'afegir opcions, no poder brut.

Les armes desbloquejables haurien de ser sidegrades:

- més condicionals
- més tècniques
- més reactives
- més dependents de lectura del rival

No haurien de tenir més dany mitjà simplement perquè es desbloquegen més tard.
