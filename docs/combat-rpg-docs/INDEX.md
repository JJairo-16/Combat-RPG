# ▌Documentació Combat RPG

> Guia d'integració i desenvolupament del projecte **Combat RPG**.  
> Aquesta documentació segueix l'estil del README original i està organitzada per temes per facilitar l'onboarding i l'extensió del codi.

---

## ▌Índex

### ▌Introducció
- [Visió general](01-introduccio/01-visio-general.md)
- [Arquitectura general](01-introduccio/02-arquitectura-general.md)
- [Flux de l'aplicació](01-introduccio/03-flux-aplicacio.md)

### ▌Core del joc
- [Bootstrap i arrencada](02-core/01-bootstrap.md)
- [Game loop](02-core/02-game-loop.md)
- [Sistema de menús](02-core/03-menu-system.md)

### ▌Combat
- [Sistema de combat](03-combat/01-sistema-combat.md)
- [Resolució d'atacs](03-combat/02-resolucio-atacs.md)
- [Torns i prioritats](03-combat/03-torns-i-prioritats.md)
- [Renderitzat de combat](03-combat/04-renderitzat-combat.md)

### ▌Personatges
- [Model de personatge](04-personatges/01-model-personatge.md)
- [Estadístiques](04-personatges/02-estadistiques.md)
- [Creació de personatges](04-personatges/03-creacio-personatge.md)
- [Races](04-personatges/04-races.md)

### ▌Armes i habilitats
- [Model d'arma](05-armes-i-habilitats/01-model-arma.md)
- [Càrrega i definicions](05-armes-i-habilitats/02-carrega-i-definicions.md)
- [Atacs i resultats](05-armes-i-habilitats/03-atacs-i-resultats.md)
- [Passives i fases](05-armes-i-habilitats/04-passives-i-fases.md)

### ▌Efectes
- [Sistema d'efectes](06-efectes/01-sistema-efectes.md)
- [Pipeline d'efectes](06-efectes/02-pipeline-efectes.md)
- [Efectes base i plantilles](06-efectes/03-plantilles.md)
- [Triggers](06-efectes/04-triggers.md)

### ▌Configuració i balance
- [Configuració d'aplicació](07-configuracio-i-balance/01-app-config.md)
- [Rutes i fitxers externs](07-configuracio-i-balance/02-paths.md)
- [Balance de combat](07-configuracio-i-balance/03-balance.md)
- [Corbes i scaling](07-configuracio-i-balance/04-corbes-i-scaling.md)
- [Validació i registre global](07-configuracio-i-balance/05-validacio-i-registre.md)

### ▌RNG i mecàniques especials
- [Afinitat divina](08-rng-i-mecaniques/01-afinitat-divina.md)
- [Crida espiritual](08-rng-i-mecaniques/02-crida-espiritual.md)
- [Stats budget](08-rng-i-mecaniques/03-stats-budget.md)

### ▌Cinemàtiques
- [Sistema de cinemàtiques](09-cinematiques/01-sistema-cinematic.md)
- [Escenes i blocs](09-cinematiques/02-escenes-i-blocs.md)
- [Tags i escriptura intel·ligent](09-cinematiques/03-tags-i-escriptura-intelligent.md)
- [Terminal i càrrega externa](09-cinematiques/04-terminal-i-carrega-externa.md)

### ▌Modifiers, debug i extensió
- [Status menu modifiers](10-modifiers-debug-extensio/01-status-modifiers.md)
- [Debug i crash reports](10-modifiers-debug-extensio/02-debug.md)
- [Tests i simulacions](10-modifiers-debug-extensio/03-tests.md)
- [Com ampliar el projecte](10-modifiers-debug-extensio/04-extensio.md)

---

## ▌Com llegir aquesta documentació

Ordre recomanat per integrar-se al projecte:

1. Introducció
2. Core del joc
3. Combat
4. Personatges
5. Armes i efectes
6. Configuració i extensió

---

## ▌Notes

- Els exemples de codi inclosos s'han tret de l'estructura real del projecte i s'han mantingut curts i útils.
- Quan una peça depèn de configuració externa, s'indica el fitxer o el registre corresponent.
- Aquesta documentació està orientada a **entendre, modificar i ampliar** el projecte.
