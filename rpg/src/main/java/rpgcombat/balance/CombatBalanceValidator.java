package rpgcombat.balance;

import java.util.Objects;

import rpgcombat.balance.config.AdrenalineConfig;
import rpgcombat.balance.config.AntiStallConfig;
import rpgcombat.balance.config.AttackDefenseVarianceConfig;
import rpgcombat.balance.config.BloodPactConfig;
import rpgcombat.balance.config.ChargedAttackConfig;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.GuardBreakConfig;
import rpgcombat.balance.config.MomentumConfig;
import rpgcombat.balance.config.StaminaConfig;

/**
 * Valida la configuració d'equilibri de combat.
 */
public final class CombatBalanceValidator {
    private CombatBalanceValidator() {
    }

    /**
     * Valida tota la configuració principal.
     *
     * @param config configuració a validar
     */
    public static void validate(CombatBalanceConfig config) {
        require(config, "combat balance config");

        require(config.stamina(), "stamina");
        require(config.momentum(), "momentum");
        require(config.attackDefenseVariance(), "attackDefenseVariance");
        require(config.guardBreak(), "guardBreak");
        require(config.adrenaline(), "adrenaline");
        require(config.chargedAttack(), "chargedAttack");
        require(config.antiStall(), "antiStall");

        validateStamina(config.stamina());
        validateMomentum(config.momentum());
        validateVariance(config.attackDefenseVariance());
        validateGuardBreak(config.guardBreak());
        validateAdrenaline(config.adrenaline());
        validateChargedAttack(config.chargedAttack());
        validateAntiStall(config.antiStall());
        validateBloodPact(config.bloodPact());
    }

    /**
     * Valida la configuració d'estamina.
     */
    private static void validateStamina(StaminaConfig config) {
        require(config.max(), "stamina.max");
        require(config.attackCost(), "stamina.attackCost");
        require(config.recovery(), "stamina.recovery");
        require(config.damageMultiplier(), "stamina.damageMultiplier");
        require(config.fatigueChance(), "stamina.fatigueChance");

        positive(config.max().base(), "stamina.max.base");
        nonNegative(config.attackCost().minimum(), "stamina.attackCost.minimum");
        clamp01(config.pressureThreshold(), "stamina.pressureThreshold");
        positive(config.fatigueChance().max(), "stamina.fatigueChance.max");
    }

    /**
     * Valida la configuració de momentum.
     */
    private static void validateMomentum(MomentumConfig config) {
        positive(config.maxStacks(), "momentum.maxStacks");
        require(config.gain(), "momentum.gain");
        require(config.loss(), "momentum.loss");
        nonNegative(config.attackBonusPerStack(), "momentum.attackBonusPerStack");
        nonNegative(config.dodgeBonusPerStack(), "momentum.dodgeBonusPerStack");
    }

    /**
     * Valida la variància d'atac/defensa.
     */
    private static void validateVariance(AttackDefenseVarianceConfig config) {
        require(config.attack(), "attackDefenseVariance.attack");
        require(config.defense(), "attackDefenseVariance.defense");
        require(config.dodge(), "attackDefenseVariance.dodge");
    }

    /**
     * Valida la configuració de trencament de defensa.
     */
    private static void validateGuardBreak(GuardBreakConfig config) {
        positive(config.maxGuardStacks(), "guardBreak.maxGuardStacks");
        positive(config.breakThreshold(), "guardBreak.breakThreshold");
        nonNegative(config.vulnerableDamageMultiplier(), "guardBreak.vulnerableDamageMultiplier");
    }

    /**
     * Valida la configuració d'adrenalina.
     */
    private static void validateAdrenaline(AdrenalineConfig config) {
        require(config.regenBonus(), "adrenaline.regenBonus");
        require(config.underdogBonuses(), "adrenaline.underdogBonuses");
        nonNegative(config.base(), "adrenaline.base");
        nonNegative(config.max(), "adrenaline.max");
    }

    /**
     * Valida la configuració d'atac carregat.
     */
    private static void validateChargedAttack(ChargedAttackConfig config) {
        nonNegative(config.damageMultiplier(), "chargedAttack.damageMultiplier");
        nonNegative(config.staggerTurnsOnHit(), "chargedAttack.staggerTurnsOnHit");
    }

    /**
     * Valida la configuració anti-estancament.
     */
    private static void validateAntiStall(AntiStallConfig config) {
        positive(config.startTurn(), "antiStall.startTurn");
        positive(config.increaseEveryTurns(), "antiStall.increaseEveryTurns");
        nonNegative(config.initialDamage(), "antiStall.initialDamage");
        nonNegative(config.damageIncreasePerStep(), "antiStall.damageIncreasePerStep");
    }

    private static void validateBloodPact(BloodPactConfig config) {
        nonNegative(config.manaThreshold(), "bloodPact.lifeThreshold");
        positive(config.baseHpCostPercent(), "bloodPact.baseHpCost");
        nonNegative(config.wisdomReduction(), "bloodPact.wisdomReduction");
        nonNegative(config.minHpCostPercent(), "bloodPact.minHpCost");
    }

    /**
     * Comprova que un valor no sigui nul.
     */
    private static void require(Object value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " config cannot be null");
    }

    /**
     * Comprova que el valor sigui > 0.
     */
    private static void positive(double value, String fieldName) {
        if (value <= 0) {
            throw new CombatBalanceException(fieldName + " must be > 0, but was " + value);
        }
    }

    /**
     * Comprova que el valor sigui > 0.
     */
    private static void positive(int value, String fieldName) {
        if (value <= 0) {
            throw new CombatBalanceException(fieldName + " must be > 0, but was " + value);
        }
    }

    /**
     * Comprova que el valor sigui >= 0.
     */
    private static void nonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new CombatBalanceException(fieldName + " must be >= 0, but was " + value);
        }
    }

    /**
     * Comprova que el valor sigui >= 0.
     */
    private static void nonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new CombatBalanceException(fieldName + " must be >= 0, but was " + value);
        }
    }

    /**
     * Comprova que el valor estigui entre 0 i 1.
     */
    private static void clamp01(double value, String fieldName) {
        if (value < 0 || value > 1) {
            throw new CombatBalanceException(fieldName + " must be between 0 and 1, but was " + value);
        }
    }
}