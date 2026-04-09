package rpgcombat.weapons.config;

import java.util.List;

import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.Attack;
import rpgcombat.weapons.attack.AttackRegistry;
import rpgcombat.weapons.passives.PassiveFactory;
import rpgcombat.weapons.passives.WeaponPassive;

/**
 * Plantilla immutable d'una arma precarregada en memòria.
 * No és la instància viva de combat: des d'aquí es crea una Weapon nova cada cop.
 */
public final class WeaponDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final int baseDamage;
    private final double criticalProb;
    private final double criticalDamage;
    private final WeaponType type;
    private final String attackSkill;
    private final double manaPrice;
    private final List<PassiveConfig> passives;

    public WeaponDefinition(
            String id,
            String name,
            String description,
            int baseDamage,
            double criticalProb,
            double criticalDamage,
            WeaponType type,
            String attackSkill,
            double manaPrice,
            List<PassiveConfig> passives) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description;
        this.baseDamage = baseDamage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attackSkill = requireText(attackSkill, "attackSkill");
        this.manaPrice = manaPrice;
        this.passives = passives == null ? List.of() : List.copyOf(passives);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public double getCriticalProb() {
        return criticalProb;
    }

    public double getCriticalDamage() {
        return criticalDamage;
    }

    public WeaponType getType() {
        return type;
    }

    public double getManaPrice() {
        return manaPrice;
    }

    public String getAttackSkill() {
        return attackSkill;
    }

    public List<PassiveConfig> getPassiveConfigs() {
        return passives;
    }

    public Weapon create() {
        Attack attack = AttackRegistry.resolve(attackSkill);
        List<WeaponPassive> builtPassives = passives.stream()
                .map(PassiveFactory::create)
                .toList();

        return new Weapon(
                id,
                name,
                description,
                baseDamage,
                criticalProb,
                criticalDamage,
                type,
                attack,
                manaPrice,
                builtPassives);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El camp " + field + " no pot estar buit.");
        }
        return value;
    }
}
