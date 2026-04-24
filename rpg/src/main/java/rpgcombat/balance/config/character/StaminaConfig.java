package rpgcombat.balance.config.character;

import java.util.Map;

/**
 * Configuració de stamina: màxim, cost d'atac, recuperació i pressió.
 *
 * @param max configuració del màxim de stamina
 * @param attackCost configuració del cost d'atac
 * @param recovery configuració de recuperació
 * @param pressureThreshold llindar de pressió
 * @param damageMultiplier configuració del multiplicador de dany sota pressió
 * @param fatigueChance configuració de probabilitat de fatiga
 */
public record StaminaConfig(
    MaxConfig max,
    AttackCostConfig attackCost,
    RecoveryConfig recovery,
    double pressureThreshold,
    DamageMultiplierConfig damageMultiplier,
    FatigueChanceConfig fatigueChance
) {

    /**
     * Paràmetres del màxim de stamina.
     *
     * @param base valor base
     * @param constitutionMultiplier multiplicador per constitució
     * @param dexterityMultiplier multiplicador per destresa
     * @param luckMultiplier multiplicador per sort
     */
    public record MaxConfig(
        double base,
        double constitutionMultiplier,
        double dexterityMultiplier,
        double luckMultiplier
    ) {}

    /**
     * Paràmetres del cost d'atac.
     *
     * @param base cost base
     * @param dexterityReductionPerPoint reducció per punt de destresa
     * @param wisdomReductionPerPoint reducció per punt de saviesa
     * @param minimum cost mínim
     */
    public record AttackCostConfig(
        double base,
        double dexterityReductionPerPoint,
        double wisdomReductionPerPoint,
        double minimum
    ) {}

    /**
     * Paràmetres de recuperació de stamina.
     *
     * @param base recuperació base
     * @param constitutionMultiplier multiplicador per constitució
     * @param wisdomMultiplier multiplicador per saviesa
     * @param minimumActionMultiplier multiplicador mínim per acció
     * @param actionMultipliers multiplicadors per tipus d'acció
     */
    public record RecoveryConfig(
        double base,
        double constitutionMultiplier,
        double wisdomMultiplier,
        double minimumActionMultiplier,
        Map<String, Double> actionMultipliers
    ) {}

    /**
     * Configuració del multiplicador de dany sota pressió.
     *
     * @param base multiplicador base
     * @param penaltyMultiplier penalització sota pressió
     * @param pressureExponent exponent de la pressió
     */
    public record DamageMultiplierConfig(
        double base,
        double penaltyMultiplier,
        double pressureExponent
    ) {}

    /**
     * Configuració de la probabilitat de fatiga.
     *
     * @param base probabilitat base
     * @param pressureExponent exponent de la pressió
     * @param luckMitigationFloor mínim de mitigació per sort
     * @param luckMitigationPerPoint mitigació per punt de sort
     * @param max probabilitat màxima
     */
    public record FatigueChanceConfig(
        double base,
        double pressureExponent,
        double luckMitigationFloor,
        double luckMitigationPerPoint,
        double max
    ) {}
}