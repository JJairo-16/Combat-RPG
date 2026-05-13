# ▌Regles de desbloqueig

[← Tornar a l'índex](../INDEX.md)

---

## ▌Objectiu

Les regles de desbloqueig indiquen quan una arma pot aparèixer a la selecció d'armes.

Aquestes regles no decideixen si l'arma existeix ni si apareix al grimori. Només decideixen si el jugador la pot triar.

---

## ▌Estructura conceptual

Una arma pot declarar un bloc `unlock` amb:

- mode de combinació
- llista de requisits

El mode pot ser:

- `ALL`: cal complir tots els requisits
- `ANY`: n'hi ha prou amb complir-ne un

---

## ▌Requisits recomanats

Els requisits poden observar progrés global ja existent:

- assoliment concret completat
- nombre total d'assoliments
- descobriment concret
- nombre total de descobriments
- descobriments dins una categoria

Això permet lligar armes a aprenentatge real del sistema.

---

## ▌Exemples de desbloqueig

Una arma de primer progrés pot demanar un assoliment simple:

```json
{
  "type": "ACHIEVEMENT",
  "id": "FIRST_OATH"
}
```

Una arma elemental pot demanar descobriments concrets:

```json
{
  "type": "DISCOVERY",
  "category": "EFFECTS",
  "id": "BURN"
}
```

Una arma més avançada pot combinar condicions:

```json
{
  "mode": "ALL",
  "requirements": [
    { "type": "ACHIEVEMENT", "id": "REALITY_YIELDS" },
    { "type": "DISCOVERY", "category": "EFFECTS", "id": "CHAOS" }
  ]
}
```

---

## ▌Context de progrés

El sistema d'unlock necessita una lectura compacta del progrés global:

- assoliments completats
- descobriments obtinguts
- recompte per categoria

Aquesta lectura ha de ser només de consulta. El sistema d'unlock no ha de completar assoliments ni descobrir entrades.

---

## ▌Bones pràctiques

- no fer servir requisits invisibles o impossibles d'intuir
- preferir requisits que ensenyin una mecànica abans de desbloquejar-ne una variant
- evitar que armes fortes depenguin només de moltes victòries
- mantenir identificadors estables
- actualitzar tests quan s'afegeixi un tipus nou de requisit
