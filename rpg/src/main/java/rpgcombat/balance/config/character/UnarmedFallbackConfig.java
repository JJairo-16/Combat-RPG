package rpgcombat.balance.config.character;

/**
 * Configuració de l'atac improvisat quan una arma màgica es queda sense mana.
 *
 * @param improviseChance probabilitat d'improvisar segons saviesa i intel·ligència
 * @param weaponType configuració per decidir entre físic o rang
 * @param damage configuració del càlcul de dany improvisat
 */
public record UnarmedFallbackConfig(
    ImproviseChanceConfig improviseChance,
    WeaponTypeConfig weaponType,
    DamageConfig damage
) {

    /**
     * Paràmetres de la probabilitat d'improvisar.
     *
     * @param minRate probabilitat mínima
     * @param maxRate probabilitat màxima
     * @param curve forma de la corba
     * @param wisdomCenter punt central de saviesa
     * @param intPenalty penalització per intel·ligència
     * @param intK suavitzat de la penalització
     */
    public record ImproviseChanceConfig(
        double minRate,
        double maxRate,
        double curve,
        double wisdomCenter,
        double intPenalty,
        double intK
    ) {}

    /**
     * Paràmetres per decidir el tipus d'arma improvisada.
     *
     * @param strIntRepulsion repulsió entre força i intel·ligència
     * @param dexIntSynergy sinergia entre destresa i intel·ligència
     * @param typeK suavitzat de les relacions
     * @param wisdomWeight pes de la saviesa en la decisió
     */
    public record WeaponTypeConfig(
        double strIntRepulsion,
        double dexIntSynergy,
        double typeK,
        double wisdomWeight
    ) {}

    /**
     * Paràmetres del càlcul de dany improvisat.
     *
     * @param baseMin dany base mínim
     * @param physicalBaseScale escala per físic
     * @param rangeBaseScale escala per rang
     * @param intDamageRepulsion penalització de dany per intel·ligència en físic
     * @param intDamageSynergy sinergia de dany per intel·ligència en rang
     * @param damageK suavitzat del càlcul de dany
     * @param qualityMin qualitat mínima
     * @param qualityMax qualitat màxima
     * @param qualityCenter punt central de qualitat
     * @param fallbackPower multiplicador global del fallback
     */
    public record DamageConfig(
        double baseMin,
        double physicalBaseScale,
        double rangeBaseScale,
        double intDamageRepulsion,
        double intDamageSynergy,
        double damageK,
        double qualityMin,
        double qualityMax,
        double qualityCenter,
        double fallbackPower
    ) {}
}