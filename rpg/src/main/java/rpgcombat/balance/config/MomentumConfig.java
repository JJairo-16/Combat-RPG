package rpgcombat.balance.config;

/**
 * Configuració de momentum.
 */
public record MomentumConfig(
    int maxStacks,
    double attackBonusPerStack,
    double dodgeBonusPerStack,
    double suppressionWhenTargetLowHealth,
    double suppressionHealthOffset,
    GainConfig gain,
    LossConfig loss,
    boolean noDecayWhileDesperateCharging
) {

    public record GainConfig(
        int onSuccessfulHit,
        int onSuccessfulDodge,
        int underdogExtraOnSuccessfulDodge
    ) {}

    public record LossConfig(
        int onBeingHit,
        int onFailedAttack,
        int onAttackDodged,
        int onPassiveTurnDefend,
        int onPassiveTurnCharge
    ) {}
}