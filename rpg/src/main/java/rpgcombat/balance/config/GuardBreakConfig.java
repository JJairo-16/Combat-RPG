package rpgcombat.balance.config;

/**
 * Configuració de guard break i vulnerabilitat.
 *
 * @param maxGuardStacks màxim de càrregues de guàrdia
 * @param breakThreshold llindar per trencar la guàrdia
 * @param normalMitigationRatio mitigació amb guàrdia activa
 * @param brokenGuardMitigationRatio mitigació amb guàrdia trencada
 * @param finalMitigationClampMin límit inferior de mitigació final
 * @param finalMitigationClampMax límit superior de mitigació final
 * @param gainOnSuccessfulDefend càrregues guanyades en defensar amb èxit
 * @param resetWhenActionIsNotDefendAtTurnStart reinicia si no es defensa a l'inici del torn
 * @param resetWhenReceivingDirectHit reinicia en rebre un cop directe
 * @param applyVulnerableTurnsOnBreak torns de vulnerabilitat en trencar-se
 * @param vulnerableDamageMultiplier multiplicador de dany en estat vulnerable
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