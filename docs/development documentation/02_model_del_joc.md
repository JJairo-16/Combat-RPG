# Enginyeria inversa del sistema d’efectes

Aquest diagrama representa l’arquitectura d’un sistema d’efectes d’estat (status effects) típic en un joc per torns. A continuació s’explica cada component i com interactuen entre si.

---

## Visió general

El sistema està dissenyat amb una jerarquia basada en interfícies i classes base, cosa que permet reutilitzar la lògica comuna i afegir fàcilment nous efectes (com verí, cremada, sagnat, etc.).

---

## Components principals

### 1. `Effect` (Interfície)

És el contracte principal que tots els efectes han de complir.

**Mètodes clau:**
- `key()`: Identificador únic de l’efecte.
- `stackingRule()`: Defineix com s’acumula l’efecte.
- `state()`: Retorna l’estat actual.
- `isExpired()`: Indica si l’efecte ha finalitzat.
- `onPhase(...)`: Executa la lògica segons la fase del torn.

---

### 2. `ConstantDamageEffect` (Classe abstracta)

És una plantilla base per a efectes que fan dany constant per torn.

**Atributs:**
- `key`: Identificador de l’efecte
- `state`: Estat intern (`EffectState`)
- `damagePerTurn`: Dany aplicat cada torn

**Mètodes:**
- `endTurn(...)`: Aplica dany al final del torn
- `isExpired()`: Comprova si l’efecte ha acabat

Aquesta classe evita duplicar lògica entre efectes similars.

---

### 3. Efectes concrets

Classes que hereten de `ConstantDamageEffect`:

- `BleedEffect` (sagnat)
- `PoisonEffect` (verí)
- `BurnEffect` (cremada)

**Característiques:**
- Tots reben:
  - `turns`: durada
  - `damagePerTurn`: dany per torn
- Poden implementar comportament específic si cal.

---

### 4. `EffectState`

Gestiona l’estat intern de l’efecte.

**Atributs:**
- `charges`: càrregues disponibles
- `stacks`: nombre d’acumulacions
- `remainingTurns`: torns restants
- `cooldownTurns`: temps de recuperació

**Mètodes:**
- `ofCharges(...)`: afegeix càrregues
- `ofDuration(...)`: defineix la durada
- `ofStacks(...)`: afegeix acumulacions
- `tickDuration()`: redueix la durada
- `tickCooldown()`: redueix el cooldown

---

### 5. `StackingRule` (Enum)

Defineix com interactuen múltiples aplicacions del mateix efecte:

- `IGNORE`: ignora nous efectes
- `REPLACE`: substitueix l’existent
- `REFRESH`: reinicia la durada
- `STACK`: acumula efectes

---

### 6. `EffectResult` (Record)

Representa el resultat d’aplicar un efecte.

**Camps:**
- `message`: missatge descriptiu
- `consumedCharge`: indica si s’ha consumit una càrrega
- `changedState`: indica si l’estat ha canviat

**Mètodes:**
- `none()`: sense efecte
- `msg(...)`: missatge simple
- `consumed(...)`: indica consum
- `changed(...)`: indica canvi

---

## Flux de funcionament

1. S’aplica un efecte (per exemple, `PoisonEffect`).
2. Es crea amb una durada i un dany per torn.
3. A cada torn:
   - S’executa `endTurn(...)`.
   - S’aplica el dany.
   - Es redueix la durada (`tickDuration()`).
4. Es comprova si ha expirat (`isExpired()`).
5. Es retorna un `EffectResult`.

---

## Disseny i patrons utilitzats

- Herència i abstracció: `ConstantDamageEffect` centralitza la lògica comuna.
- Composició: `EffectState` encapsula l’estat.
- Patró Strategy: `StackingRule` defineix el comportament dinàmic.
- Immutabilitat parcial: `EffectResult` com a record.

---

## Avantatges del disseny

- Facilitat per afegir nous efectes
- Reutilització de codi
- Separació clara de responsabilitats
- Sistema escalable i mantenible

---

## Possibles millores

- Afegir efectes no lineals (per exemple, dany creixent)
- Suport per a efectes combinats
- Sistema d’esdeveniments més complet (`onPhase` més ampli)
