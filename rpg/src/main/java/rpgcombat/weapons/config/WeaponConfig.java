package rpgcombat.weapons.config;

import java.util.List;

/**
 * DTO directe del JSON de configuració d'armes.
 */
public record WeaponConfig(
    String id,
    String name,
    String description,

    int baseDamage,
    double criticalProb,
    double criticalDamage,

    String type,
    double manaPrice,

    String attackSkill,
    List<PassiveConfig> passives
) {}

