package rpgcombat.models.characters;

import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.AttackDefenseVarianceConfig;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.character.UnarmedFallbackConfig;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponType;

public class UnarmedAttack {
    private final Statistics stats;
    private final Random rng;

    private final CombatBalanceConfig balance = CombatBalanceRegistry.get();
    private final AttackDefenseVarianceConfig varianceConfig = balance.attackDefenseVariance();
    private final UnarmedFallbackConfig fallbackConfig = balance.unarmedFallback();

    public UnarmedAttack(Statistics stats, Random rng) {
        this.stats = stats;
        this.rng = rng;
    }

    /**
     * Executa un atac desarmat bàsic.
     */
    public AttackResult attackUnarmed() {
        double damage = WeaponType.PHYSICAL.getBasicDamage(7, stats) * damageVariance(rng);
        return new AttackResult(round2(damage), "ataca amb les mans desnudes.");
    }

    /**
     * Executa un atac desarmat amb un dany base concret.
     */
    public AttackResult attackUnarmed(int base, Statistics otherStats) {
        double damage = WeaponType.PHYSICAL.getBasicDamage(base, otherStats) * damageVariance(rng);
        return new AttackResult(round2(damage), "ataca amb les mans desnudes.");
    }

    /**
     * Intenta improvisar un atac quan una arma màgica queda sense mana.
     */
    public AttackResult fallbackAttack() {
        if (rng.nextDouble() > getImproviseChance(stats)) {
            return new AttackResult(
                    0,
                    "Sense mana, intenta improvisar... però l'arma queda inert.");
        }

        WeaponType improvisedType = getWeaponType(stats, rng);
        double damage = getImprovisedDamage(stats, improvisedType);

        String message = switch (improvisedType) {
            case PHYSICAL -> "Sense mana, usa l'arma màgica com una arma física improvisada.";
            case RANGE -> "Sense mana, força un atac de rang improvisat amb l'arma apagada.";
            default -> "Sense mana, improvisa un atac desesperat.";
        };

        return AttackResult.fallback(damage, message);
    }

    /**
     * Calcula la probabilitat d'improvisar segons la saviesa.
     */
    private double getImproviseChance(Statistics stats) {
        UnarmedFallbackConfig.ImproviseChanceConfig cfg = fallbackConfig.improviseChance();

        double wisdom = stats.getWisdom();
        double intelligence = stats.getIntelligence();

        double wisdomPart = Math.pow(wisdom, cfg.curve())
                / (Math.pow(wisdom, cfg.curve()) + Math.pow(cfg.wisdomCenter(), cfg.curve()));

        double intModifier = 1.0 - cfg.intPenalty() * intelligence / (intelligence + cfg.intK());

        return Math.clamp(
                cfg.minRate() + (cfg.maxRate() - cfg.minRate()) * wisdomPart * intModifier,
                cfg.minRate(),
                cfg.maxRate());
    }

    /**
     * Decideix si la improvisació serà física o de rang.
     */
    private WeaponType getWeaponType(Statistics stats, Random rng) {
        UnarmedFallbackConfig.WeaponTypeConfig cfg = fallbackConfig.weaponType();

        double strength = stats.getStrength();
        double dexterity = stats.getDexterity();
        double intelligence = stats.getIntelligence();
        double wisdom = stats.getWisdom();

        double physicalScore = strength
                * (1.0 - cfg.strIntRepulsion() * intelligence / (intelligence + cfg.typeK()))
                + wisdom * cfg.wisdomWeight();

        double rangeScore = dexterity
                * (1.0 + cfg.dexIntSynergy() * intelligence / (intelligence + cfg.typeK()))
                + wisdom * cfg.wisdomWeight();

        double rangeChance = rangeScore / (rangeScore + physicalScore);

        return rng.nextDouble() < rangeChance
                ? WeaponType.RANGE
                : WeaponType.PHYSICAL;
    }

    /**
     * Calcula un dany improvisat moderat perquè només sigui una ajuda.
     */
    private double getImprovisedDamage(Statistics stats, WeaponType improvisedType) {
        UnarmedFallbackConfig.DamageConfig cfg = fallbackConfig.damage();

        double strength = stats.getStrength();
        double dexterity = stats.getDexterity();
        double intelligence = stats.getIntelligence();
        double wisdom = stats.getWisdom();

        double improvisedBaseDamage;

        if (improvisedType == WeaponType.PHYSICAL) {
            improvisedBaseDamage = cfg.baseMin()
                    + cfg.physicalBaseScale()
                    * Math.sqrt(wisdom * strength)
                    * (1.0 - cfg.intDamageRepulsion() * intelligence / (intelligence + cfg.damageK()));
        } else {
            improvisedBaseDamage = cfg.baseMin()
                    + cfg.rangeBaseScale()
                    * Math.sqrt(wisdom * dexterity)
                    * (1.0 + cfg.intDamageSynergy() * intelligence / (intelligence + cfg.damageK()));
        }

        int roundedBaseDamage = (int) Math.round(improvisedBaseDamage);

        double typeDamage = improvisedType.getBasicDamage(roundedBaseDamage, stats);

        double quality = cfg.qualityMin()
                + (cfg.qualityMax() - cfg.qualityMin())
                * wisdom / (wisdom + cfg.qualityCenter());

        double damage = typeDamage * quality * cfg.fallbackPower() * damageVariance(rng);

        return round2(Math.max(1.0, damage));
    }

    /**
     * Aplica la variació de dany configurada al combat.
     */
    private double damageVariance(Random rng) {
        AttackDefenseVarianceConfig.AttackConfig cfg = varianceConfig.attack();
        double roll = (rng.nextDouble() + rng.nextDouble()) / 2.0;
        return cfg.minMultiplier() + roll * (cfg.maxMultiplier() - cfg.minMultiplier());
    }

    /**
     * Arrodoneix a dos decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}