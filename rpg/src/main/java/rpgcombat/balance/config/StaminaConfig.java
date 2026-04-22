package rpgcombat.balance.config;

import java.util.Map;

/**
 * Configuració de stamina: màxim, cost d'atac, recuperació i pressió.
 */
public record StaminaConfig(
    MaxConfig max,
    AttackCostConfig attackCost,
    RecoveryConfig recovery,
    double pressureThreshold,
    DamageMultiplierConfig damageMultiplier,
    FatigueChanceConfig fatigueChance
) {

    public record MaxConfig(
        double base,
        double constitutionMultiplier,
        double dexterityMultiplier,
        double luckMultiplier
    ) {}

    public record AttackCostConfig(
        double base,
        double dexterityReductionPerPoint,
        double wisdomReductionPerPoint,
        double minimum
    ) {}

    public record RecoveryConfig(
        double base,
        double constitutionMultiplier,
        double wisdomMultiplier,
        double minimumActionMultiplier,
        Map<String, Double> actionMultipliers
    ) {}

    public record DamageMultiplierConfig(
        double base,
        double penaltyMultiplier,
        double pressureExponent
    ) {}

    public record FatigueChanceConfig(
        double base,
        double pressureExponent,
        double luckMitigationFloor,
        double luckMitigationPerPoint,
        double max
    ) {}
}