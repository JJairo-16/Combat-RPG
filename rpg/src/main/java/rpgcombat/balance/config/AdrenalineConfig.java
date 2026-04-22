package rpgcombat.balance.config;

/**
 * Configuració d'adrenalina i bonificacions d'underdog.
 */
public record AdrenalineConfig(
    double desperateHealthRatio,
    double triggerHealthRatio,
    double base,
    double gapBonusMax,
    double desperateBonus,
    double max,
    double gapReference,
    boolean singleUsePerCombat,
    RegenBonusConfig regenBonus,
    UnderdogBonusesConfig underdogBonuses
) {

    public record RegenBonusConfig(
        double healthBasePct,
        double healthFromAdrenalineDivisor,
        double manaBasePct,
        double manaFromAdrenalineDivisor
    ) {}

    public record UnderdogBonusesConfig(
        double damageMaxBonus,
        double damageTakenMaxReduction,
        double incomingDamageReductionCap,
        double desperateFlatAttackBonus,
        double desperateFlatDamageReduction
    ) {}
}