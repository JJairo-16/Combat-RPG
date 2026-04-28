package rpgcombat.models.characters;

import java.util.Map;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.character.StaminaConfig;
import rpgcombat.balance.config.character.StaminaConfig.AttackCostConfig;
import rpgcombat.balance.config.character.StaminaConfig.DamageMultiplierConfig;
import rpgcombat.balance.config.character.StaminaConfig.FatigueChanceConfig;
import rpgcombat.balance.config.character.StaminaConfig.MaxConfig;
import rpgcombat.balance.config.character.StaminaConfig.RecoveryConfig;
import rpgcombat.combat.models.Action;

/**
 * Emmagatzema les estadístiques base i els valors dinàmics del personatge.
 */
public class Statistics {

    private final int strength;
    private final int dexterity;
    private final int constitution;
    private final int intelligence;
    private final int wisdom;
    private final int charisma;
    private final int luck;

    private final double maxHealth;
    private final double maxMana;
    private final double maxStamina;
    private final double maxResistance;

    private double health;
    private double mana;
    private double stamina;
    private double resistance;

    private static final int MAX_CONSTITUTION_FULL_EFFECT = 20;
    private static final double CONSTITUTION_VALUE = 50.0;

    private static final double HEALTH_SOFTCAP_FACTOR = 0.08;
    private static final double REGEN_SOFTCAP_FACTOR = 0.10;

    private static final double RESISTANCE_PRESSURE_START = 0.55;

    private boolean invulnerable = false;

    /**
     * Crea les estadístiques a partir del bloc base de stats.
     */
    public Statistics(int[] stats) {
        this.strength = stats[0];
        this.dexterity = stats[1];
        this.constitution = stats[2];
        this.intelligence = stats[3];
        this.wisdom = stats[4];
        this.charisma = stats[5];
        this.luck = stats[6];

        this.maxHealth = calculateMaxHealth(constitution);
        this.maxMana = intelligence * 30.0;
        this.maxStamina = calculateMaxStamina();
        this.maxResistance = calculateMaxResistance();

        this.health = maxHealth;
        this.mana = maxMana;
        this.stamina = maxStamina;
        this.resistance = maxResistance;
    }

    /**
     * Retorna la força.
     */
    public int getStrength() {
        return strength;
    }

    /**
     * Retorna la destresa.
     */
    public int getDexterity() {
        return dexterity;
    }

    /**
     * Retorna la constitució.
     */
    public int getConstitution() {
        return constitution;
    }

    /**
     * Retorna la intel·ligència.
     */
    public int getIntelligence() {
        return intelligence;
    }

    /**
     * Retorna la saviesa.
     */
    public int getWisdom() {
        return wisdom;
    }

    /**
     * Retorna el carisma.
     */
    public int getCharisma() {
        return charisma;
    }

    /**
     * Retorna la sort.
     */
    public int getLuck() {
        return luck;
    }

    /**
     * Retorna la vida actual.
     */
    public double getHealth() {
        return health;
    }

    /**
     * Retorna el mana actual.
     */
    public double getMana() {
        return mana;
    }

    /**
     * Retorna la vida màxima.
     */
    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Retorna el mana màxim.
     */
    public double getMaxMana() {
        return maxMana;
    }

    /**
     * Retorna l'estamina màxima.
     */
    public double getMaxStamina() {
        return maxStamina;
    }

    /**
     * Retorna la resistència màxima.
     */
    public double getMaxResistance() {
        return maxResistance;
    }

    /**
     * Retorna l'estamina actual.
     */
    public double getStamina() {
        return stamina;
    }

    /**
     * Retorna la resistència actual.
     */
    public double getResistance() {
        return resistance;
    }

    /**
     * Regenera vida i mana base.
     */
    public void reg() {
        double hp = calculateHealthRegen(constitution);
        double ma = intelligence * 0.9;
        health = affectClamp(health, hp, maxHealth, 0);
        mana = affectClamp(mana, ma, maxMana, 0);
    }

    /**
     * Regenera vida i mana amb bonus.
     */
    public void reg(double hpBonus, double manaBonus) {
        double hp = calculateHealthRegen(constitution * hpBonus);
        double ma = (intelligence * manaBonus) * 0.9;
        health = affectClamp(health, hp, maxHealth, 0);
        mana = affectClamp(mana, ma, maxMana, 0);
    }

    /**
     * Aplica dany directe a la vida.
     */
    public void damage(double dmg) {
        health = Math.max(0, health - dmg);
    }

    /**
     * Consumeix mana si n'hi ha prou.
     */
    public boolean consumeMana(double price) {
        if (price > mana) {
            return false;
        }
        mana -= price;
        return true;
    }

    /**
     * Cura sense superar el màxim.
     */
    public double heal(double amount) {
        if (amount <= 0) {
            return 0;
        }
        double before = health;
        health = Math.min(maxHealth, health + amount);
        return health - before;
    }

    /**
     * Cura sense límit superior.
     */
    public double overloadHeal(double amount) {
        if (amount <= 0) {
            return 0;
        }
        double before = health;
        health += amount;
        return health - before;
    }

    /**
     * Recupera mana sense superar el màxim.
     */
    public double restoreMana(double amount) {
        if (amount <= 0) {
            return 0;
        }
        double before = mana;
        mana = Math.min(maxMana, mana + amount);
        return mana - before;
    }

    /**
     * Recupera mana sense límit superior.
     */
    public double overloadRestoreMana(double amount) {
        if (amount <= 0) {
            return 0;
        }
        double before = mana;
        mana += amount;
        return mana - before;
    }

    /**
     * Activa o desactiva la invulnerabilitat.
     */
    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    /**
     * Restaura la vida al màxim si és invulnerable.
     */
    public void applyInvulnerability() {
        if (invulnerable) {
            health = maxHealth;
        }
    }

    /**
     * Consumeix l'estamina d'un atac.
     */
    public void consumeStaminaOnAttack() {
        AttackCostConfig cfg = staminaConfig().attackCost();

        double cost = Math.max(
                cfg.minimum(),
                cfg.base()
                        - dexterity * cfg.dexterityReductionPerPoint()
                        - wisdom * cfg.wisdomReductionPerPoint());

        stamina = affectClamp(stamina, -cost, maxStamina, 0);
    }

    /**
     * Recupera estamina en una acció no ofensiva.
     */
    public void recoverStaminaOnNonAttack(double multiplier) {
        RecoveryConfig cfg = staminaConfig().recovery();

        double regen = (cfg.base()
                + constitution * cfg.constitutionMultiplier()
                + wisdom * cfg.wisdomMultiplier())
                * Math.max(cfg.minimumActionMultiplier(), multiplier);

        stamina = affectClamp(stamina, regen, maxStamina, 0);
    }

    /**
     * Recupera estamina sense superar el màxim.
     */
    public double restoreStamina(double amount) {
        if (amount <= 0) {
            return 0;
        }
        double before = stamina;
        stamina = Math.min(maxStamina, stamina + amount);
        return stamina - before;
    }


    /**
     * Recupera resistència en atacar.
     */
    public void recoverResistanceOnAttack() {
        double regen = 10.0 + constitution * 0.10 + dexterity * 0.06;
        resistance = affectClamp(resistance, regen, maxResistance, 0);
    }

    /**
     * Consumeix resistència en defensar.
     */
    public void consumeResistanceOnDefend() {
        double cost = Math.max(8.5, 12.0 - constitution * 0.07 - wisdom * 0.06);
        resistance = affectClamp(resistance, -cost, maxResistance, 0);
    }

    /**
     * Consumeix resistència en esquivar.
     */
    public void consumeResistanceOnDodge() {
        double cost = Math.max(12.0, 18.0 - dexterity * 0.10 - wisdom * 0.05);
        resistance = affectClamp(resistance, -cost, maxResistance, 0);
    }

    /**
     * Aplica els canvis de recursos a l'inici de l'acció.
     */
    public void onActionStart(Action action) {
        if (action == null) {
            return;
        }

        RecoveryConfig cfg = staminaConfig().recovery();
        Map<String, Double> multipliers = cfg.actionMultipliers();

        switch (action) {
            case ATTACK -> {
                consumeStaminaOnAttack();
                recoverResistanceOnAttack();
            }
            case DEFEND -> recoverStaminaOnNonAttack(multipliers.getOrDefault("DEFEND", 1.15));
            case DODGE -> recoverStaminaOnNonAttack(multipliers.getOrDefault("DODGE", 1.05));
            case CHARGE -> recoverStaminaOnNonAttack(multipliers.getOrDefault("CHARGE", 0.9));
            default -> {
            }
        }
    }

    /**
     * Retorna el multiplicador de dany segons la pressió d'estamina.
     */
    public double staminaDamageMultiplier() {
        DamageMultiplierConfig cfg = staminaConfig().damageMultiplier();

        return cfg.base()
                - cfg.penaltyMultiplier() * Math.pow(staminaPressure(), cfg.pressureExponent());
    }

    /**
     * Retorna el multiplicador de dany rebut per pressió de resistència.
     */
    public double resistanceIncomingDamageMultiplier() {
        return 1.0 + 0.18 * Math.pow(resistancePressure(), 1.40);
    }

    /**
     * Retorna el multiplicador d'esquiva per pressió de resistència.
     */
    public double resistanceDodgeMultiplier() {
        return 1.0 - 0.16 * Math.pow(resistancePressure(), 1.25);
    }

    /**
     * Calcula la probabilitat de fatiga per estamina.
     */
    public double fatigueChance() {
        FatigueChanceConfig cfg = staminaConfig().fatigueChance();

        double luckMitigation = Math.max(
                cfg.luckMitigationFloor(),
                1.0 - luck * cfg.luckMitigationPerPoint());

        return Math.clamp(
                cfg.base() * Math.pow(staminaPressure(), cfg.pressureExponent()) * luckMitigation,
                0.0,
                cfg.max());
    }

    /**
     * Calcula la probabilitat d'esgotament per resistència.
     */
    public double exhaustionChance() {
        double luckMitigation = Math.max(0.68, 1.0 - luck * 0.004);
        return Math.clamp(0.34 * Math.pow(resistancePressure(), 1.85) * luckMitigation, 0.0, 0.34);
    }

    /**
     * Retorna el percentatge actual d'estamina.
     */
    public double staminaRatio() {
        if (maxStamina <= 0) {
            return 1.0;
        }
        return Math.clamp(stamina / maxStamina, 0.0, 1.0);
    }

    /**
     * Retorna el percentatge actual de resistència.
     */
    public double resistanceRatio() {
        if (maxResistance <= 0) {
            return 1.0;
        }
        return Math.clamp(resistance / maxResistance, 0.0, 1.0);
    }

    /**
     * Calcula la pressió actual d'estamina.
     */
    private double staminaPressure() {
        return normalizedPressure(staminaRatio(), staminaConfig().pressureThreshold());
    }

    /**
     * Calcula la pressió actual de resistència.
     */
    private double resistancePressure() {
        return normalizedPressure(resistanceRatio(), RESISTANCE_PRESSURE_START);
    }

    /**
     * Normalitza la pressió d'un recurs.
     */
    private double normalizedPressure(double ratio, double threshold) {
        if (ratio >= threshold) {
            return 0.0;
        }
        double span = Math.max(0.05, threshold);
        return Math.clamp((threshold - ratio) / span, 0.0, 1.0);
    }

    /**
     * Calcula la vida màxima.
     */
    private double calculateMaxHealth(int con) {
        double effectiveCon = softenStat(con, MAX_CONSTITUTION_FULL_EFFECT, HEALTH_SOFTCAP_FACTOR);
        return effectiveCon * CONSTITUTION_VALUE;
    }

    /**
     * Calcula la regeneració de vida base.
     */
    private double calculateHealthRegen(double con) {
        double effectiveCon = softenStat(con, MAX_CONSTITUTION_FULL_EFFECT, REGEN_SOFTCAP_FACTOR);
        return effectiveCon * 2.35;
    }

    /**
     * Aplica un softcap a una stat.
     */
    private double softenStat(double stat, int threshold, double factor) {
        if (stat <= threshold) {
            return stat;
        }
        double extra = stat - threshold;
        return threshold + (extra / (1.0 + extra * factor));
    }

    /**
     * Suma una quantitat i limita el resultat.
     */
    private double affectClamp(double act, double amount, double max, double min) {
        return Math.clamp(act + amount, min, max);
    }

    /**
     * Calcula l'estamina màxima.
     */
    private double calculateMaxStamina() {
        MaxConfig cfg = staminaConfig().max();

        return cfg.base()
                + constitution * cfg.constitutionMultiplier()
                + dexterity * cfg.dexterityMultiplier()
                + luck * cfg.luckMultiplier();
    }

    /**
     * Calcula la resistència màxima.
     */
    private double calculateMaxResistance() {
        return 80.0 + constitution * 3.2 + wisdom * 0.75 + luck * 0.4;
    }

    /**
     * Retorna la configuració global de combat.
     */
    private static CombatBalanceConfig balance() {
        return CombatBalanceRegistry.get();
    }

    /**
     * Retorna la configuració d'estamina.
     */
    private static StaminaConfig staminaConfig() {
        return balance().stamina();
    }
}