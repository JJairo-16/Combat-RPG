package rpgcombat.combat;

/**
 * Representa els bonus addicionals de regeneració al final del torn.
 */
public final class EndRoundRegenBonus {
    private double bonusHealthPct;
    private double bonusManaPct;

    /**
     * Afegeix percentatges extra de regeneració.
     *
     * @param hpPct Percentatge de vida
     * @param manaPct Percentatge de manà
     */
    public void add(double hpPct, double manaPct) {
        bonusHealthPct += hpPct;
        bonusManaPct += manaPct;
    }

   /** @return Percentatge extra de regeneració de vida */
    public double bonusHealthPct() {
        return bonusHealthPct;
    }

   /** @return Percentatge extra de regeneració de manà */
    public double bonusManaPct() {
        return bonusManaPct;
    }
}