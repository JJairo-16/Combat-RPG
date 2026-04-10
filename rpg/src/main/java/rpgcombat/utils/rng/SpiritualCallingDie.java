package rpgcombat.utils.rng;

import java.util.Random;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.ui.Prettier;

/**
 * Utilitat per gestionar la tirada del "Llamado de los espíritus".
 *
 * <p>
 * Aquesta classe separa tres responsabilitats:
 * </p>
 * <ul>
 * <li>Tirada del dau (1–20) amb possible avantatge segons la sort</li>
 * <li>Càlcul del percentatge de curació a partir de la tirada</li>
 * <li>(Externament) animació del resultat</li>
 * </ul>
 *
 * <p>
 * Això permet desacoblar la lògica del RNG de la representació visual.
 * </p>
 */
public final class SpiritualCallingDie {

    private SpiritualCallingDie() {
    }

    /** Percentatge mínim de curació (7%). */
    private static final double MIN = 0.07;

    /** Percentatge màxim de curació (20%). */
    private static final double MAX = 0.20;

    public static double roll(Random rng, Statistics stats) {
        int face = rollFace(rng, stats);

        try {
            D20Terminal.animateDie(face, rng);
        } catch (InterruptedException e) {
            Prettier.error("Ha hagut un error amb l'animació. El resultat ha sigut " + face + ".");
        }

        return computeHealPercent(face);

    }

    /**
     * Realitza la tirada del dau de 20 cares.
     *
     * <p>
     * Pot aplicar avantatge en funció de la sort del personatge.
     * </p>
     *
     * @param rng   generador aleatori del personatge
     * @param stats estadístiques del personatge
     * @return valor entre 1 i 20 (inclòs)
     */
    private static int rollFace(Random rng, Statistics stats) {
        int roll1 = rollBell(rng);
        int roll2 = rollBell(rng);

        double advantageChance = Math.min(0.30, stats.getLuck() * 0.01);

        int chosen = (rng.nextDouble() < advantageChance)
                ? Math.max(roll1, roll2)
                : roll1;

        return applyHouseBias(chosen);
    }

    private static int rollBell(Random rng) {
        double t = (rng.nextDouble() + rng.nextDouble() + rng.nextDouble()) / 3.0;
        int face = 1 + (int) Math.round(t * 19.0);
        return clamp1to20(face);
    }

    private static int applyHouseBias(int face) {
        double t = (face - 1) / 19.0;

        // Exponente > 1 desplaza ligeramente hacia valores bajos.
        double biased = Math.pow(t, 1.15);

        int result = 1 + (int) Math.round(biased * 19.0);
        return clamp1to20(result);
    }

    /**
     * Calcula el percentatge de curació a partir de la cara del dau.
     *
     * <p>
     * El valor es normalitza i es transforma mitjançant una interpolació lineal
     * entre {@link #MIN} i {@link #MAX}.
     * </p>
     *
     * @param face resultat del dau (1–20)
     * @return percentatge de curació (entre MIN i MAX)
     */
    private static double computeHealPercent(int face) {
        double normalized = (face - 1) / 19.0;
        return lerp(MIN, MAX, normalized);
    }

    /**
     * Interpolació lineal entre dos valors.
     *
     * @param a valor mínim
     * @param b valor màxim
     * @param t factor normalitzat (0–1)
     * @return valor interpolat
     */
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clamp1to20(int n) {
        return Math.clamp(n, 1, 20);
    }
}