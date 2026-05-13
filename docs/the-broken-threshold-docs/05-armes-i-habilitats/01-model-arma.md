# ▌Model d'arma

[← Tornar a l'índex](../INDEX.md)

---

## ▌Classes clau

- `weapons/Weapon.java`
- `weapons/config/WeaponDefinition.java`
- `weapons/config/WeaponType.java`

---

## ▌Dues capes diferents

### ▌`WeaponDefinition`
Plantilla immutable precarregada des de JSON.

### ▌`Weapon`
Instància viva usada durant el combat.

Aquesta separació és molt bona perquè permet:

- carregar catàleg una sola vegada
- crear còpies operatives quan un jugador equipa una arma
- evitar estat compartit accidental entre jugadors

---

## ▌Exemple real

```java
public Weapon create() {
    Attack attack = AttackRegistry.resolve(attackSkill);
    List<WeaponPassive> builtPassives = passives.stream()
            .map(PassiveFactory::create)
            .toList();

    return new Weapon(
            id, name, description, baseDamage,
            criticalProb, criticalDamage, type,
            attack, manaPrice, builtPassives);
}
```

---

## ▌Què té una arma?

- identificador i nom
- descripció
- dany base
- probabilitat i multiplicador de crític
- tipus
- atac/habilitat principal
- cost de manà
- passives
