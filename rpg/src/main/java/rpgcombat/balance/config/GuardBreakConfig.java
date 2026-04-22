package rpgcombat.balance.config;

/**
 * Configuració de guard break i vulnerabilitat.
 */
public record GuardBreakConfig(
    int maxGuardStacks,
    int breakThreshold,
    double normalMitigationRatio,
    double brokenGuardMitigationRatio,
    double finalMitigationClampMin,
    double finalMitigationClampMax,
    int gainOnSuccessfulDefend,
    boolean resetWhenActionIsNotDefendAtTurnStart,
    boolean resetWhenReceivingDirectHit,
    int applyVulnerableTurnsOnBreak,
    double vulnerableDamageMultiplier
) {}