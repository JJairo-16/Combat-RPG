# ▌Integració amb descobriments

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Les armes desbloquejables han d'aparèixer sempre dins els descobriments, encara que no estiguin disponibles per triar.

Això converteix el grimori en una eina de progressió: el jugador pot veure que existeix contingut pendent i rebre una pista sobre com avançar.

---

## ▌Dues pistes per arma

Per armes desbloquejables, una sola pista no és suficient.

Cal distingir:

- pista de desbloqueig
- pista de descobriment

La pista de desbloqueig s'utilitza quan l'arma encara no està disponible.

La pista de descobriment s'utilitza quan l'arma ja està disponible però encara no s'ha descobert completament.

---

## ▌Pista de desbloqueig

La pista de desbloqueig orienta el jugador sobre què ha de fer per poder triar l'arma.

Ha de suggerir el camí, no descriure l'arma.

Exemple:

```text
Quan dues marques oposades apareguin al combat, alguna cosa respondrà.
```

---

## ▌Pista de descobriment

La pista de descobriment dona una idea de la identitat o del comportament de l'arma.

Ha de ser menys directa que una descripció completa.

Exemple:

```text
Mai repeteix exactament la mateixa naturalesa dues vegades seguides.
```

---

## ▌Comportament visual

El visor ha de triar el text segons l'estat:

- si l'arma no està disponible: mostra `unlockHint`
- si l'arma està disponible però no descoberta: mostra `discoveryHint`
- si l'arma està descoberta: mostra la fitxa completa

La fitxa completa continua venint del catàleg final de descobriments.

---

## ▌Relació amb entrades automàtiques

Les armes poden tenir entrada automàtica al catàleg, però les armes importants haurien de tenir override manual.

Això permet:

- evitar pistes genèriques
- controlar l'ordre
- ajustar el text bloquejat
- adaptar la pista al requisit real de desbloqueig

---

## ▌Regla important

No s'ha de filtrar `DiscoveryCatalog` amb el sistema d'unlock.

El catàleg de descobriments veu totes les armes.
La selecció d'armes veu només les disponibles.
