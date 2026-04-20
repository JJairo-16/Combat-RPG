package rpgcombat.models.characters;

import rpgcombat.combat.models.Action;

/**
 * Emmagatzema les estadístiques base i els valors dinàmics (vida i mana).
 */
public class Statistics {

    // Estadístiques base
    private final int strength;
    private final int dexterity;
    private final int constitution; // vida
    private final int intelligence;
    private final int wisdom;
    private final int charisma;
    private final int luck;

    // Límits màxims
    private final double maxHealth;
    private final double maxMana;
    private final double maxStamina;
    private final double maxResistance;

    // Valors actuals
    private double health;
    private double mana;
    private double stamina;
    private double resistance;

    private static final int MAX_CONSTITUTION_FULL_EFFECT = 20;
    private static final double CONSTITUTION_VALUE = 50.0;

    private static final double HEALTH_SOFTCAP_FACTOR = 0.08;
    private static final double REGEN_SOFTCAP_FACTOR = 0.10;

    private static final double STAMINA_PRESSURE_START = 0.58;
    private static final double RESISTANCE_PRESSURE_START = 0.55;

    private boolean invulnerable = false;

    /**
     * Construeix les estadístiques a partir d'un array de 7 valors en ordre fix.
     *
     * @param stats força, destresa, constitució, intel·ligència, saviesa, carisma,
     *              sort
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

    public int getStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getConstitution() {
        return constitution;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getWisdom() {
        return wisdom;
    }

    public int getCharisma() {
        return charisma;
    }

    public int getLuck() {
        return luck;
    }

    public double getHealth() {
        return health;
    }

    public double getMana() {
        return mana;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getMaxMana() {
        return maxMana;
    }

    public double getMaxStamina() {
        return maxStamina;
    }

    public double getMaxResistance() {
        return maxResistance;
    }

    public double getStamina() {
        return stamina;
    }

    public double getResistance() {
        return resistance;
    }

    /**
     * Regenera vida i mana segons constitució i intel·ligència, sense superar els
     * màxims.
     */
    public void reg() {
        double hp = calculateHealthRegen(constitution);

        double ma = intelligence * 0.9;

        health = affectClamp(health, hp, maxHealth, 0);
        mana = affectClamp(mana, ma, maxMana, 0);
    }

    public void reg(double hpBonus, double manaBonus) {
        double hp = calculateHealthRegen(constitution * hpBonus);

        double ma = (intelligence * manaBonus) * 0.9;

        health = affectClamp(health, hp, maxHealth, 0);
        mana = affectClamp(mana, ma, maxMana, 0);
    }

    /**
     * Aplica dany a la vida (mai baixa de 0).
     *
     * @param dmg dany rebut
     */
    public void damage(double dmg) {
        health = Math.max(0, health - dmg);
    }

    /**
     * Consumeix mana si n'hi ha prou.
     *
     * @param price cost de mana
     * @return {@code true} si s'ha pogut pagar; {@code false} si no hi ha mana
     *         suficient
     */
    public boolean consumeMana(double price) {
        if (price > mana) {
            return false;
        }

        mana -= price;
        return true;
    }

    /**
     * Cura vida fins al màxim i retorna la curació real aplicada.
     *
     * @param amount quantitat de curació sol·licitada
     * @return quantitat real curada
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
     * Cura vida sense límit màxim i retorna la curació real aplicada (pot
     * sobrecarregar).
     * 
     * @param amount quantitat de curació sol·licitada
     * @return quantitat real curada
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
     * Restaura mana fins al màxim i retorna la quantitat real restaurada.
     *
     * @param amount quantitat de restauració sol·licitada
     * @return quantitat real restaurada
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
     * Restaura mana sense límit màxim i retorna la quantitat real restaurada (pot
     * sobrecarregar).
     * 
     * @param amount quantitat de restauració sol·licitada
     * @return quantitat real restaurada
     */
    public double overloadRestoreMana(double amount) {
        if (amount <= 0) {
            return 0;
        }

        double before = mana;
        mana += amount;
        return mana - before;
    }

    /** Calcula la vida màxima amb soft cap suau a partir de 20 de constitució. */
    private double calculateMaxHealth(int con) {
        double effectiveCon = softenStat(con, MAX_CONSTITUTION_FULL_EFFECT, HEALTH_SOFTCAP_FACTOR);
        return effectiveCon * CONSTITUTION_VALUE;
    }

    /**
     * Calcula la regeneració de vida amb un soft cap una mica més fort que la vida
     * màxima.
     */
    private double calculateHealthRegen(double con) {
        double effectiveCon = softenStat(con, MAX_CONSTITUTION_FULL_EFFECT, REGEN_SOFTCAP_FACTOR);
        return effectiveCon * 2.35;
    }

    /**
     * Fins al llindar, l'estadística té efecte complet.
     * A partir d'aquí, cada punt extra aporta una mica menys que l'anterior.
     */
    private double softenStat(double stat, int threshold, double factor) {
        if (stat <= threshold) {
            return stat;
        }

        double extra = stat - threshold;
        return threshold + (extra / (1.0 + extra * factor));
    }

    /** Aplica un increment i limita el resultat dins del rang indicat. */
    private double affectClamp(double act, double amount, double max, double min) {
        return Math.clamp(act + amount, min, max);
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public void applyInvulnerability() {
        if (invulnerable) {
            health = maxHealth;
        }
    }


    /**
     * La stamina baixa només en atacar.
     * El cost és prou suau perquè atacar repetidament continuï sent viable,
     * però prou real perquè alternar amb altres accions sigui lleugerament millor.
     */
    public void consumeStaminaOnAttack() {
        double cost = Math.max(10.0, 18.0 - dexterity * 0.10 - wisdom * 0.05);
        stamina = affectClamp(stamina, -cost, maxStamina, 0);
    }

    /** Regeneració de stamina en qualsevol acció que no sigui atacar. */
    public void recoverStaminaOnNonAttack(double multiplier) {
        double regen = (12.0 + constitution * 0.12 + wisdom * 0.08) * Math.max(0.5, multiplier);
        stamina = affectClamp(stamina, regen, maxStamina, 0);
    }

    /** La resistencia puja quan el personatge adopta una acció ofensiva. */
    public void recoverResistanceOnAttack() {
        double regen = 10.0 + constitution * 0.10 + dexterity * 0.06;
        resistance = affectClamp(resistance, regen, maxResistance, 0);
    }

    /** La resistencia baixa només en defensar de debò. */
    public void consumeResistanceOnDefend() {
        double cost = Math.max(8.5, 12.0 - constitution * 0.07 - wisdom * 0.06);
        resistance = affectClamp(resistance, -cost, maxResistance, 0);
    }

    /** La esquiva és més exigent que bloquejar. */
    public void consumeResistanceOnDodge() {
        double cost = Math.max(12.0, 18.0 - dexterity * 0.10 - wisdom * 0.05);
        resistance = affectClamp(resistance, -cost, maxResistance, 0);
    }

    public void onActionStart(Action action) {
        if (action == null) {
            return;
        }

        switch (action) {
            case ATTACK -> {
                consumeStaminaOnAttack();
                recoverResistanceOnAttack();
            }
            case DEFEND -> recoverStaminaOnNonAttack(1.15);
            case DODGE -> recoverStaminaOnNonAttack(1.05);
        }
    }

    public double staminaDamageMultiplier() {
        return 1.0 - 0.16 * Math.pow(staminaPressure(), 1.35);
    }

    public double resistanceIncomingDamageMultiplier() {
        return 1.0 + 0.18 * Math.pow(resistancePressure(), 1.40);
    }

    public double resistanceDodgeMultiplier() {
        return 1.0 - 0.16 * Math.pow(resistancePressure(), 1.25);
    }

    public double fatigueChance() {
        double luckMitigation = Math.max(0.70, 1.0 - luck * 0.004);
        return Math.clamp(0.30 * Math.pow(staminaPressure(), 1.80) * luckMitigation, 0.0, 0.30);
    }

    public double exhaustionChance() {
        double luckMitigation = Math.max(0.68, 1.0 - luck * 0.004);
        return Math.clamp(0.34 * Math.pow(resistancePressure(), 1.85) * luckMitigation, 0.0, 0.34);
    }

    public double staminaRatio() {
        if (maxStamina <= 0) {
            return 1.0;
        }
        return Math.clamp(stamina / maxStamina, 0.0, 1.0);
    }

    public double resistanceRatio() {
        if (maxResistance <= 0) {
            return 1.0;
        }
        return Math.clamp(resistance / maxResistance, 0.0, 1.0);
    }

    private double staminaPressure() {
        return normalizedPressure(staminaRatio(), STAMINA_PRESSURE_START);
    }

    private double resistancePressure() {
        return normalizedPressure(resistanceRatio(), RESISTANCE_PRESSURE_START);
    }

    private double normalizedPressure(double ratio, double threshold) {
        if (ratio >= threshold) {
            return 0.0;
        }

        double span = Math.max(0.05, threshold);
        return Math.clamp((threshold - ratio) / span, 0.0, 1.0);
    }

    private double calculateMaxStamina() {
        return 75.0 + constitution * 3.0 + dexterity * 1.0 + luck * 0.5;
    }

    private double calculateMaxResistance() {
        return 80.0 + constitution * 3.2 + wisdom * 0.75 + luck * 0.4;
    }

}