package rpgcombat.balance.config;

import rpgcombat.balance.config.character.AdrenalineConfig;
import rpgcombat.balance.config.character.BloodPactConfig;
import rpgcombat.balance.config.character.ChargedAttackConfig;
import rpgcombat.balance.config.character.GuardBreakConfig;
import rpgcombat.balance.config.character.MomentumConfig;
import rpgcombat.balance.config.character.StaminaConfig;
import rpgcombat.balance.config.character.UnarmedFallbackConfig;

/**
 * Configuració arrel del balanceig de combat.
 *
 * @param stamina configuració de resistència
 * @param momentum configuració de momentum
 * @param attackDefenseVariance configuració de variància d'atac/defensa/esquiva
 * @param guardBreak configuració de trencament de guàrdia
 * @param adrenaline configuració d'adrenalina
 * @param chargedAttack configuració d'atac carregat
 * @param antiStall configuració d'anti-stall
 * @param bloodPact configuració de pacte de sang
 * @param fracture configuració de fractura
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
    UnarmedFallbackConfig unarmedFallback
) {}