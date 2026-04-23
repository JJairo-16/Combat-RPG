package rpgcombat.balance.config;

/**
 * Configuració de variància d'atac, defensa i esquiva.
 *
 * @param attack configuració de la variància d'atac
 * @param defense configuració de la variància de defensa
 * @param dodge configuració de l'esquiva
 */
public record AttackDefenseVarianceConfig(
    AttackConfig attack,
    DefenseConfig defense,
    DodgeConfig dodge
) {

    /**
     * Paràmetres de variància de l'atac.
     *
     * @param spread dispersió de la variància
     * @param minMultiplier multiplicador mínim
     * @param maxMultiplier multiplicador màxim
     */
    public record AttackConfig(
        double spread,
        double minMultiplier,
        double maxMultiplier
    ) {}

    /**
     * Paràmetres de variància de la defensa.
     *
     * @param minMultiplier multiplicador mínim
     * @param maxMultiplier multiplicador màxim
     */
    public record DefenseConfig(
        double minMultiplier,
        double maxMultiplier
    ) {}

    /**
     * Paràmetres de l'esquiva.
     *
     * @param dexterityOffsetFrom10Multiplier multiplicador segons diferència de destresa respecte a 10
     * @param luckMultiplier multiplicador de sort
     * @param baseMinClamp límit inferior base
     * @param baseMaxClamp límit superior base
     * @param finalMinClamp límit inferior final
     * @param finalMaxClamp límit superior final
     * @param failedDodgeDamageMultiplier multiplicador de dany si falla l'esquiva
     */
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