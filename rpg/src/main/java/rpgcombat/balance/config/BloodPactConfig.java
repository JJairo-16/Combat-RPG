package rpgcombat.balance.config;

public record BloodPactConfig(
    double manaThreshold,
    double baseHpCostPercent,
    double wisdomReduction,
    double minHpCostPercent
) {
    
}
