package rpgcombat.utils.rng;

import java.util.Random;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.ui.Prettier;

/**
 * Utilitat per gestionar la tirada del "Llamado de los espíritus".
 *
 * <p>
 * Inclou una distribució suau que redueix lleugerament la probabilitat
 * de valors alts (15–20) sense fer-ho evident.
 * </p>
 */
public final class SpiritualCallingDie {

    private SpiritualCallingDie() {
    }

    /** Percentatge mínim de curació (7%). */
    private static final double MIN = 0.07;

    /** Percentatge màxim de curació (20%). */
    private static final double MAX = 0.20;

    public record RollResult(int face, double percent) {}

    public static RollResult roll(Random rng, Statistics stats) {
        int face = rollFace(rng, stats);

        try {
            D20Terminal.animateDie(face, rng);
        } catch (InterruptedException e) {
            Prettier.error("Ha hagut un error amb l'animació. El resultat ha sigut " + face + ".");
        }

        return new RollResult(face, computeHealPercent(face));
    }

   /** Realitza la tirada del dau amb possible avantatge. */
    private static int rollFace(Random rng, Statistics stats) {
        int roll1 = rollBell(rng);
        int roll2 = rollBell(rng);

        double advantageChance = Math.min(0.30, stats.getLuck() * 0.01);

        int chosen = (rng.nextDouble() < advantageChance)
                ? Math.max(roll1, roll2)
                : roll1;

        return applySoftHighBias(chosen);
    }

   /** Distribució base amb forma de campana suau. */
    private static int rollBell(Random rng) {
        double t = (rng.nextDouble() + rng.nextDouble() + rng.nextDouble()) / 3.0;
        int face = 1 + (int) Math.round(t * 19.0);
        return clamp1to20(face);
    }

    /**
     * Aplica una penalització molt suau als valors alts (15–20).
     *
     * <p>
     * Només afecta la part alta de la distribució i ho fa de manera progressiva.
     * Això evita que es noti artificial.
     * </p>
     */
    private static int applySoftHighBias(int face) {
        double t = (face - 1) / 19.0;

        // Llindar aproximat per començar a penalitzar (15/20 ≈ 0.74)
        double threshold = 0.74;

        if (t <= threshold) {
            return face;
        }

        // Compressió suau del tram superior
        double excess = (t - threshold) / (1.0 - threshold);

        // Funció suau (no lineal) per reduir lleugerament els valors alts
        double reduced = threshold + (Math.pow(excess, 1.25) * (1.0 - threshold));

        int result = 1 + (int) Math.round(reduced * 19.0);
        return clamp1to20(result);
    }

   /** Calcula el percentatge de curació a partir de la cara del dau. */
    private static double computeHealPercent(int face) {
        double normalized = (face - 1) / 19.0;
        return lerp(MIN, MAX, normalized);
    }

   /** Interpolació lineal. */
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clamp1to20(int n) {
        return Math.clamp(n, 1, 20);
    }
}