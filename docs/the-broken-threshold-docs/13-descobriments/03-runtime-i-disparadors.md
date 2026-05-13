# ▌Runtime i disparadors de descobriment

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Els descobriments s'activen quan una part del joc comunica que un contingut ha estat vist, utilitzat o generat.

El sistema no intenta deduir-ho tot des de la UI. Cada peça del joc dispara el descobriment en el moment més proper al fet real.

---

## ▌DiscoveryRuntime

`DiscoveryRuntime` actua com a pont global.

Permet que sistemes com armes, efectes o perks puguin registrar descobriments sense rebre una dependència directa de `DiscoverySystem`.

Exemple conceptual:

```java
DiscoveryRuntime.discover(DiscoveryCategory.EFFECTS, effect.key());
```

---

## ▌Moments habituals de descobriment

Els descobriments solen passar quan:

- s'equipa una arma
- una raça entra en joc
- una acció s'utilitza
- un efecte s'aplica
- una missió de perk és assignada
- una perk és obtinguda
- una perk divina és assignada
- una sinergia s'activa

---

## ▌Validació contra el catàleg

`DiscoverySystem.discover(...)` només registra una entrada si existeix al catàleg.

Això evita desaments amb claus desconegudes i obliga que el contingut descobrible tingui una fitxa definida, automàtica o manual.

---

## ▌Relació amb assoliments

Descobriments i assoliments són sistemes independents.

Un mateix fet pot provocar:

- un descobriment, si revela contingut
- un assoliment, si fa avançar un objectiu

Per exemple, equipar una arma pot descobrir-ne la fitxa i també avançar un assoliment basat en `WEAPON_EQUIPPED` o `WEAPON_USED`.

---

## ▌Bones pràctiques

- disparar el descobriment quan el jugador veu o usa el contingut real
- no descobrir contingut només perquè existeix al JSON
- no dependre de textos renderitzats per descobrir entrades
- no registrar claus que no existeixen al catàleg
