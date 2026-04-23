package rpgcombat.balance.config;

/**
 * Configuració d'adrenalina i bonificacions d'underdog.
 *
 * @param desperateHealthRatio llindar de vida per estat desesperat
 * @param triggerHealthRatio llindar de vida per activar l'adrenalina
 * @param base valor base d'adrenalina
 * @param gapBonusMax bonus màxim per diferència de poder
 * @param desperateBonus bonus addicional en estat desesperat
 * @param max valor màxim d'adrenalina
 * @param gapReference referència de diferència per calcular bonus
 * @param singleUsePerCombat indica si només s'activa un cop per combat
 * @param regenBonus configuració de regeneració associada
 * @param underdogBonuses configuració de bonificacions d'underdog
 */
public record AdrenalineConfig(
    double desperateHealthRatio,
    double triggerHealthRatio,
    double base,
    double gapBonusMax,
    double desperateBonus,
    double max,
    double gapReference,
    boolean singleUsePerCombat,
    RegenBonusConfig regenBonus,
    UnderdogBonusesConfig underdogBonuses
) {

    /**
     * Configuració de la regeneració vinculada a l'adrenalina.
     *
     * @param healthBasePct percentatge base de regeneració de vida
     * @param healthFromAdrenalineDivisor divisor per escalar amb adrenalina (vida)
     * @param manaBasePct percentatge base de regeneració de mana
     * @param manaFromAdrenalineDivisor divisor per escalar amb adrenalina (mana)
     */
    public record RegenBonusConfig(
        double healthBasePct,
        double healthFromAdrenalineDivisor,
        double manaBasePct,
        double manaFromAdrenalineDivisor
    ) {}

    /**
     * Bonificacions per situació d'underdog.
     *
     * @param damageMaxBonus bonus màxim de dany
     * @param damageTakenMaxReduction reducció màxima de dany rebut
     * @param incomingDamageReductionCap límit de reducció de dany entrant
     * @param desperateFlatAttackBonus bonus pla d'atac en estat desesperat
     * @param desperateFlatDamageReduction reducció plana de dany en estat desesperat
     */
    public record UnderdogBonusesConfig(
        double damageMaxBonus,
        double damageTakenMaxReduction,
        double incomingDamageReductionCap,
        double desperateFlatAttackBonus,
        double desperateFlatDamageReduction
    ) {}
}