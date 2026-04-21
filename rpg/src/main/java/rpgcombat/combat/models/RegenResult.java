package rpgcombat.combat.models;

/**
 * Representa la regeneració aplicada al final d'una ronda.
 *
 * @param healthRecovered vida recuperada
 * @param manaRecovered mana recuperat
 */
public record RegenResult(double healthRecovered, double manaRecovered) {
    public static final RegenResult ZERO = new RegenResult(0, 0);
}