package rpgcombat.balance;

import java.util.Objects;

import rpgcombat.balance.config.AntiStallConfig;
import rpgcombat.balance.config.AttackDefenseVarianceConfig;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.FractureConfig;
import rpgcombat.balance.config.character.AdrenalineConfig;
import rpgcombat.balance.config.character.BloodPactConfig;
import rpgcombat.balance.config.character.ChargedAttackConfig;
import rpgcombat.balance.config.character.GuardBreakConfig;
import rpgcombat.balance.config.character.MomentumConfig;
import rpgcombat.balance.config.character.StaminaConfig;
import rpgcombat.balance.config.character.UnarmedFallbackConfig;

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
        require(config.bloodPact(), "bloodPact");
        require(config.fracture(), "fracture");
        require(config.unarmedFallback(), "unarmedFallback");

        validateStamina(config.stamina());
        validateMomentum(config.momentum());
        validateVariance(config.attackDefenseVariance());
        validateGuardBreak(config.guardBreak());
        validateAdrenaline(config.adrenaline());
        validateChargedAttack(config.chargedAttack());
        validateAntiStall(config.antiStall());
        validateBloodPact(config.bloodPact());
        validateFractureConfig(config.fracture());
        validateUnarmedFallback(config.unarmedFallback());
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

    /**
     * Valida la configuració de pacte de sang.
     *
     * @param config configuració a validar
     */
    private static void validateBloodPact(BloodPactConfig config) {
        clamp01(config.manaThreshold(), "bloodPact.lifeThreshold");
        clamp01(config.baseHpCostPercent(), "bloodPact.baseHpCost");
        nonNegative(config.wisdomReduction(), "bloodPact.wisdomReduction");
        nonNegative(config.minHpCostPercent(), "bloodPact.minHpCost");
    }

    /**
     * Valida la configuració de fractura.
     *
     * @param config configuració a validar
     */
    private static void validateFractureConfig(FractureConfig config) {
        clamp01(config.minRate(), "fracture.minRate");
        clamp01(config.maxRate(), "fracture.maxRate");
        clamp(config.C(), 10, 50, "fracture.C");
        clamp(config.n(), 0, 2, "fracture.n");
        clamp01(config.damageMultiplier(), "fracture.damageMultiplier");
        nonNegative(config.duration(), "fracture.duration");

        assertMinMax(config.minRate(), "fracture.minRate", config.maxRate(), "fracture.maxRate");
    }

    /**
     * Valida la configuració d'atac desarmat i improvisació.
     *
     * @param config configuració a validar
     */
    private static void validateUnarmedFallback(UnarmedFallbackConfig config) {
        UnarmedFallbackConfig.ImproviseChanceConfig improviseChance = config.improviseChance();
        UnarmedFallbackConfig.WeaponTypeConfig weaponType = config.weaponType();
        UnarmedFallbackConfig.DamageConfig damage = config.damage();

        require(improviseChance, "unarmedFallback.improviseChance");
        require(weaponType, "unarmedFallback.weaponType");
        require(damage, "unarmedFallback.damage");

        clamp01(improviseChance.minRate(), "unarmedFallback.improviseChance.minRate");
        clamp01(improviseChance.maxRate(), "unarmedFallback.improviseChance.maxRate");
        assertMinMax(
                improviseChance.minRate(),
                "unarmedFallback.improviseChance.minRate",
                improviseChance.maxRate(),
                "unarmedFallback.improviseChance.maxRate");
        positive(improviseChance.curve(), "unarmedFallback.improviseChance.curve");
        positive(improviseChance.wisdomCenter(), "unarmedFallback.improviseChance.wisdomCenter");
        clamp01(improviseChance.intPenalty(), "unarmedFallback.improviseChance.intPenalty");
        positive(improviseChance.intK(), "unarmedFallback.improviseChance.intK");

        clamp01(weaponType.strIntRepulsion(), "unarmedFallback.weaponType.strIntRepulsion");
        nonNegative(weaponType.dexIntSynergy(), "unarmedFallback.weaponType.dexIntSynergy");
        positive(weaponType.typeK(), "unarmedFallback.weaponType.typeK");
        nonNegative(weaponType.wisdomWeight(), "unarmedFallback.weaponType.wisdomWeight");

        nonNegative(damage.baseMin(), "unarmedFallback.damage.baseMin");
        nonNegative(damage.physicalBaseScale(), "unarmedFallback.damage.physicalBaseScale");
        nonNegative(damage.rangeBaseScale(), "unarmedFallback.damage.rangeBaseScale");
        clamp01(damage.intDamageRepulsion(), "unarmedFallback.damage.intDamageRepulsion");
        nonNegative(damage.intDamageSynergy(), "unarmedFallback.damage.intDamageSynergy");
        positive(damage.damageK(), "unarmedFallback.damage.damageK");

        clamp01(damage.qualityMin(), "unarmedFallback.damage.qualityMin");
        clamp01(damage.qualityMax(), "unarmedFallback.damage.qualityMax");
        assertMinMax(
                damage.qualityMin(),
                "unarmedFallback.damage.qualityMin",
                damage.qualityMax(),
                "unarmedFallback.damage.qualityMax");
        positive(damage.qualityCenter(), "unarmedFallback.damage.qualityCenter");

        nonNegative(damage.fallbackPower(), "unarmedFallback.damage.fallbackPower");
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
        clamp(value, 0, 1, fieldName);
    }

    /**
     * Comprova que el valor estigui dins d'un rang.
     *
     * @param value     valor a validar
     * @param min       mínim permès
     * @param max       màxim permès
     * @param fieldName nom del camp
     */
    private static void clamp(double value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new CombatBalanceException(
                    fieldName + " must be between " + min + " and " + max + ", but was" + value);
        }
    }

    /**
     * Comprova que el mínim no superi el màxim.
     *
     * @param min          valor mínim
     * @param minFieldName nom del camp mínim
     * @param max          valor màxim
     * @param maxFieldName nom del camp màxim
     */
    private static void assertMinMax(double min, String minFieldName, double max, String maxFieldName) {
        if (min > max) {
            throw new CombatBalanceException(minFieldName + " cannot be superior than " + maxFieldName
                    + ", but was {min: " + min + ", max: " + max + "}");
        }
    }
}