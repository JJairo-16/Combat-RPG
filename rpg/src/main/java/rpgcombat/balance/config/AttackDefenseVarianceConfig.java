package rpgcombat.balance.config;

/**
 * Configuració de variància d'atac, defensa i esquiva.
 */
public record AttackDefenseVarianceConfig(
    AttackConfig attack,
    DefenseConfig defense,
    DodgeConfig dodge
) {

    public record AttackConfig(
        double spread,
        double minMultiplier,
        double maxMultiplier
    ) {}

    public record DefenseConfig(
        double minMultiplier,
        double maxMultiplier
    ) {}

    public record DodgeConfig(
        double dexterityOffsetFrom10Multiplier,
        double luckMultiplier,
        double baseMinClamp,
        double baseMaxClamp,
        double finalMinClamp,
        double finalMaxClamp,
        double failedDodgeDamageMultiplier
    ) {}
}