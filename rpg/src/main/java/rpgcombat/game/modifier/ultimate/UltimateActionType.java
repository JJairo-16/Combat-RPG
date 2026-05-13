package rpgcombat.game.modifier.ultimate;

import rpgcombat.weapons.config.WeaponType;

/**
 * Tipus d'ulti de segona etapa associada a una família d'arma.
 *
 * <p>Cada valor defineix les dades necessàries per identificar, mostrar i
 * aplicar una habilitat definitiva: clau d'efecte, identificador de
 * descobriment, etiqueta visible, tipus d'arma requerit, multiplicador de dany,
 * costos d'activació i bonificació de crític.</p>
 */
public enum UltimateActionType {

    /**
     * Definitiva màgica centrada en un multiplicador de dany elevat i un cost de
     * mana.
     */
    ARCANE_OVERLOAD(
            "ULTIMATE_ARCANE_OVERLOAD",
            "ARCANE_OVERLOAD",
            "Sobrecàrrega arcana",
            WeaponType.MAGICAL,
            2.05,
            0.0,
            0.0,
            0.22,
            0.0),

    /**
     * Definitiva física amb un multiplicador de dany alt i costos de stamina i
     * resistència.
     */
    COLOSSAL_BREAK(
            "ULTIMATE_COLOSSAL_BREAK",
            "COLOSSAL_BREAK",
            "Trencament colossal",
            WeaponType.PHYSICAL,
            1.95,
            0.30,
            0.20,
            0.0,
            0.0),

    /**
     * Definitiva a distància pròpia d'elfs, amb bonificació de probabilitat de
     * crític i cost de stamina.
     */
    ELVEN_OPENING_SHOT(
            "ULTIMATE_ELVEN_OPENING_SHOT",
            "ELVEN_OPENING_SHOT",
            "Tret èlfic d’obertura",
            WeaponType.RANGE,
            1.75,
            0.24,
            0.0,
            0.0,
            0.15);

    private final String effectKey;
    private final String discoveryId;
    private final String label;
    private final WeaponType weaponType;
    private final double damageMultiplier;
    private final double staminaCostRatio;
    private final double resistanceCostRatio;
    private final double manaCostRatio;
    private final double critChanceBonus;

    /**
     * Crea un tipus de definitiva amb totes les seves propietats de configuració.
     *
     * @param effectKey clau única de l'efecte intern associat
     * @param discoveryId identificador utilitzat per al sistema de descobriment
     * @param label nom visible de la definitiva
     * @param weaponType tipus d'arma necessari per activar aquesta definitiva
     * @param damageMultiplier multiplicador aplicat al dany de l'atac definitiu
     * @param staminaCostRatio proporció de stamina màxima consumida en activar-la
     * @param resistanceCostRatio proporció de resistència màxima consumida en activar-la
     * @param manaCostRatio proporció de mana màxim consumida en activar-la
     * @param critChanceBonus bonificació addicional a la probabilitat de crític
     */
    UltimateActionType(String effectKey, String discoveryId, String label, WeaponType weaponType,
            double damageMultiplier, double staminaCostRatio, double resistanceCostRatio,
            double manaCostRatio, double critChanceBonus) {
        this.effectKey = effectKey;
        this.discoveryId = discoveryId;
        this.label = label;
        this.weaponType = weaponType;
        this.damageMultiplier = damageMultiplier;
        this.staminaCostRatio = staminaCostRatio;
        this.resistanceCostRatio = resistanceCostRatio;
        this.manaCostRatio = manaCostRatio;
        this.critChanceBonus = critChanceBonus;
    }

    /**
     * Retorna la clau única de l'efecte intern associat a aquesta definitiva.
     *
     * @return clau de l'efecte
     */
    public String effectKey() {
        return effectKey;
    }

    /**
     * Retorna l'identificador utilitzat pel sistema de descobriment o
     * desbloqueig.
     *
     * @return identificador de descobriment
     */
    public String discoveryId() {
        return discoveryId;
    }

    /**
     * Retorna el nom visible de la definitiva.
     *
     * @return etiqueta de la definitiva
     */
    public String label() {
        return label;
    }

    /**
     * Retorna el tipus d'arma requerit per utilitzar aquesta definitiva.
     *
     * @return tipus d'arma associat
     */
    public WeaponType weaponType() {
        return weaponType;
    }

    /**
     * Retorna el multiplicador de dany aplicat a l'atac definitiu.
     *
     * @return multiplicador de dany
     */
    public double damageMultiplier() {
        return damageMultiplier;
    }

    /**
     * Retorna la proporció de stamina màxima que es consumeix en activar aquesta
     * definitiva.
     *
     * @return proporció de cost de stamina
     */
    public double staminaCostRatio() {
        return staminaCostRatio;
    }

    /**
     * Retorna la proporció de resistència màxima que es consumeix en activar
     * aquesta definitiva.
     *
     * @return proporció de cost de resistència
     */
    public double resistanceCostRatio() {
        return resistanceCostRatio;
    }

    /**
     * Retorna la proporció de mana màxim que es consumeix en activar aquesta
     * definitiva.
     *
     * @return proporció de cost de mana
     */
    public double manaCostRatio() {
        return manaCostRatio;
    }

    /**
     * Retorna la bonificació addicional a la probabilitat de crític.
     *
     * @return bonificació de probabilitat de crític
     */
    public double critChanceBonus() {
        return critChanceBonus;
    }
}