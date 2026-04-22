package rpgcombat.balance.config;

/**
 * Configuració arrel del balanceig de combat.
 */
public record CombatBalanceConfig(
    StaminaConfig stamina,
    MomentumConfig momentum,
    AttackDefenseVarianceConfig attackDefenseVariance,
    GuardBreakConfig guardBreak,
    AdrenalineConfig adrenaline,
    ChargedAttackConfig chargedAttack,
    AntiStallConfig antiStall,
    BloodPactConfig bloodPact
) {}