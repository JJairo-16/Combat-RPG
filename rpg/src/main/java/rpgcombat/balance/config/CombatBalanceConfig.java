package rpgcombat.balance.config;

import rpgcombat.balance.config.character.*;

/**
 * Configuració arrel del balanceig de combat.
 *
 * @param stamina               configuració de resistència
 * @param momentum              configuració de momentum
 * @param attackDefenseVariance configuració de variància d'atac/defensa/esquiva
 * @param guardBreak            configuració de trencament de guàrdia
 * @param adrenaline            configuració d'adrenalina
 * @param chargedAttack         configuració d'atac carregat
 * @param antiStall             configuració d'anti-stall
 * @param bloodPact             configuració de pacte de sang
 * @param fracture              configuració de fractura
 * @param unarmedFallback       configuració d'atac desarmat i improvisació
 * @param chaos                 configuració del caos
 */
public record CombatBalanceConfig(
        StaminaConfig stamina,
        MomentumConfig momentum,
        AttackDefenseVarianceConfig attackDefenseVariance,
        GuardBreakConfig guardBreak,
        AdrenalineConfig adrenaline,
        ChargedAttackConfig chargedAttack,
        AntiStallConfig antiStall,
        BloodPactConfig bloodPact,
        FractureConfig fracture,
        UnarmedFallbackConfig unarmedFallback,
        ChaosConfig chaos) {
}