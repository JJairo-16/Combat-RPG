# ▌Selecció d'armes i proves

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

La selecció d'armes és l'únic punt on les armes bloquejades han de desaparèixer.

El combat, les habilitats i les passives no necessiten saber si una arma estava bloquejada. Si una arma arriba equipada al combat, es resol com qualsevol altra.

---

## ▌Catàleg complet i catàleg disponible

Cal mantenir dues vistes diferents:

- catàleg complet: totes les armes carregades
- catàleg disponible: armes que compleixen l'unlock

El catàleg complet alimenta descobriments.
El catàleg disponible alimenta la selecció d'armes.

---

## ▌Punt d'aplicació del filtre

El filtre s'ha d'aplicar al menú o servei que prepara les opcions d'equipament.

No s'ha d'aplicar a:

- `Weapon.attack()`
- `Skills`
- `Passives`
- `CombatSystem`
- `DiscoveryCatalog`

Això manté el combat desacoblat del metaprogrés.

---

## ▌Validacions recomanades

Quan s'afegeix una arma desbloquejable, cal comprovar:

- existeix a `weapons.json`
- té entrada o override a `discoveryCatalog.json`
- els requisits d'unlock apunten a ids reals
- si forma part d'assoliments globals d'armes, s'han actualitzat les llistes
- la selecció no la mostra abans de complir requisits
- la selecció la mostra després de complir requisits
- el grimori la mostra sempre
- la pista canvia segons disponibilitat i descobriment

---

## ▌Tests mínims

Un test de desbloqueig hauria de cobrir:

- arma sense `unlock`: disponible per defecte
- arma amb assoliment pendent: no disponible
- arma amb assoliment completat: disponible
- arma amb descobriment pendent: no disponible
- arma amb descobriment completat: disponible
- mode `ALL`
- mode `ANY`

---

## ▌Tests d'integració

Un test d'integració hauria de comprovar el flux complet:

1. carregar armes
2. carregar assoliments i descobriments
3. construir context de progrés
4. filtrar armes disponibles
5. construir vista de descobriments
6. verificar que l'arma bloquejada no surt a selecció
7. verificar que l'arma bloquejada sí surt al grimori
8. completar el requisit
9. verificar que l'arma passa a estar disponible

---

## ▌Notes de manteniment

Cada vegada que s'afegeixin armes al joc, cal revisar:

- balance per tipus d'arma
- logros de col·lecció d'armes
- pistes del grimori
- tests de càrrega i desbloqueig
- si l'arma necessita mecànica pròpia, test de mecànica
