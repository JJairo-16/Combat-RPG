# ▌Atacs i resultats

[← Tornar a l'índex](../INDEX.md)

---

## ▌Paquet

`weapons/attack/`

Classes principals:

- `Attack`
- `AttackRegistry`
- `AttackResult`
- `Target`

---

## ▌Idea del model

Una habilitat d'arma s'encapsula com un `Attack`.
El seu resultat no és només un número: retorna un `AttackResult`.

Això permet comunicar:

- dany
- missatge
- objectiu
- tipus de fallada

---

## ▌Mostra real

```java
public static AttackResult resourceFail(String message) {
    return new AttackResult(0, message, Target.ENEMY, FAIL_KIND_RESOURCE);
}

public static AttackResult skillFail(String message) {
    return new AttackResult(0, message, Target.ENEMY, FAIL_KIND_SKILL);
}
```

---

## ▌Per què és important?

Aquesta API evita tractar totes les fallades igual.

Exemples:

- no tens manà suficient
- l'habilitat ha fallat per la seva mecànica pròpia
- l'objectiu real és el mateix atacant

---

## ▌Quan afegir un nou `Attack`

Quan l'habilitat necessita lògica pròpia i no es resol només amb dany base o passives.
