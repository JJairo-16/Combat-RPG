# 04 - Triggers

[← Tornar a l'índex](./README.md)

---

## ▌Visió general

Els *triggers* són efectes passius reactius que s'executen en resposta a esdeveniments del combat (impactes, esquives, etc.).

No tenen durada ni expiren, i serveixen per encapsular comportament dinàmic sense acoblar-lo al flux principal del combat.

Tots:
- Implementen `Effect`
- Extenen `Trigger`
- Retornen un `EffectResult`

---

## ▌Classe base: Trigger

Classe abstracta que defineix el comportament comú.

### Característiques

- `key`: identificador únic
- `state`: estat fix (no orientat a durada)
- No expira mai:
  
```java
@Override
public boolean isExpired() {
    return false;
}
```

### Propòsit

- Proporcionar estructura comuna
- Evitar duplicació
- Garantir comportament consistent

---

## ▌Hooks disponibles

Els triggers funcionen mitjançant *hooks* invocats pel sistema de combat.

Exemples habituals:
- `afterHit(...)`
- (altres segons el sistema)

Cada hook:
1. Rep el context (ex: `HitContext`)
2. Avalua condicions
3. Pot aplicar efectes
4. Retorna `EffectResult`

---

## ▌Cicle d'execució

1. Es produeix un esdeveniment de combat
2. El sistema localitza triggers actius
3. Invoca el hook corresponent
4. Cada trigger decideix si actuar

---

## ▌Exemple: FractureTrigger

### Comportament

- S'activa en rebre un cop crític
- No s'aplica si el propietari és l'atacant
- Probabilitat basada en constitució
- Aplica l'efecte `Fracture`

### Flux

1. Rebre impacte
2. Comprovar:
   - No és auto-hit
   - És crític
3. Calcular probabilitat
4. Aplicar efecte si escau

### Fórmula

```java
minRate + (maxRate - minRate) / (1 + Math.pow(con / (double) C, n))
```

- `con`: constitució
- `C`, `n`: control de la corba
- Més constitució ⇒ menys probabilitat

---

## ▌Integració amb configuració

Els triggers utilitzen configuració externa:

```java
CombatBalanceRegistry.get().fracture()
```

Avantatges:
- Balanceig sense recompilar
- Centralització
- Ajust fi de mecàniques

---

## ▌EffectResult

Els triggers sempre retornen un `EffectResult`:

- `EffectResult.none()` → no passa res
- `EffectResult.msg(...)` → missatge de combat

---

## ▌Bones pràctiques

- Evitar càlculs pesats dins hooks
- Utilitzar configuració externa
- Fer condicions clares i ràpides
- No modificar estat global innecessàriament

---

## ▌Extensió

Per crear un trigger:

1. Extendre `Trigger`
2. Assignar `key`
3. Sobreescriure hook(s)
4. Implementar lògica
5. Retornar `EffectResult`

---

## ▌Notes finals

- Els triggers permeten dissenyar mecàniques complexes de forma modular
- Són essencials per efectes reactius (crítics, esquives, passius, etc.)
- Faciliten mantenibilitat i escalabilitat del sistema de combat