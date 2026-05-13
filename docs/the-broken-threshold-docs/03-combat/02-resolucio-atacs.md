# ▌Resolució d'atacs

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classe clau

`AttackResolver`

Aquesta classe tradueix el dany calculat en el comportament concret segons la resposta del defensor.

---

## ▌Casos suportats

- `DODGE` → el target intenta esquivar
- `DEFEND` → el target defensa
- qualsevol altre cas → rep dany directe

```java
public Result resolveAttack(double damage, Character target, Action targetAction) {
    return switch (targetAction) {
        case DODGE -> target.dodge(damage);
        case DEFEND -> target.defend(damage);
        default -> {
            if (damage <= 0) {
                yield new Result(-1, "");
            }
            target.resetGuardStacks();
            yield target.getDamage(damage);
        }
    };
}
```

---

## ▌Selecció d'objectiu

El sistema també permet atacs que no sempre van a l'enemic. `AttackResult` té un `Target` i `chooseTarget(...)` decideix si l'impacte és sobre:

- enemic
- atacant

Això obre la porta a habilitats de risc, autoinfligides o de pacte.

---

## ▌On es calcula el dany base?

No aquí. `AttackResolver` aplica el resultat final d'un atac ja calculat.
El càlcul del dany i els modificadors passen abans, principalment a:

- `Weapon`
- `Attack`
- `TurnResolver`
- `EffectPipeline`
