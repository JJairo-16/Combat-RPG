package rpgcombat.utils.rng;

import java.util.Random;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.ui.Prettier;

/**
 * Utilitat per gestionar la tirada del "Llamado de los espíritus".
 *
 * <p>
 * La distribució base es manté relativament estable, però incorpora pics
 * direccionals ocasionals. El carisma no altera la part central de la tirada:
 * només inclina suaument la direcció i intensitat d'aquests pics segons la
 * predisposició dels déus envers el personatge.
 * </p>
 */
public final class SpiritualCallingDie {

    private SpiritualCallingDie() {
    }

    /** Percentatge mínim de curació (7%). */
    private static final double MIN = 0.07;

    /** Percentatge màxim de curació (20%). */
    private static final double MAX = 0.20;

    public record RollResult(int face, double percent) {
    }

    /**
     * Paràmetres interns per controlar la tirada direccional.
     *
     * @param downwardChance probabilitat que el pic vagi cap avall
     * @param lowExponent    intensitat de la caiguda (menor = més extrema)
     * @param highExponent   intensitat de la pujada (menor = més extrema)
     */
    private record SpikeProfile(
            double downwardChance,
            double lowExponent,
            double highExponent) {
    }

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
    public static int rollFace(Random rng, Statistics stats) {
        int charisma = stats.getCharisma();

        int roll1 = rollBell(rng, charisma);
        int roll2 = rollBell(rng, charisma);

        double advantageChance = Math.min(0.30, stats.getLuck() * 0.01);

        if (rng.nextDouble() < advantageChance) {
            return Math.max(roll1, roll2);
        }

        return roll1;
    }

    /** Distribució base amb forma de campana suau i pics direccionals ocasionals. */
    private static int rollBell(Random rng, int charisma) {
        double specialChance = 0.15;

        if (rng.nextDouble() < specialChance) {
            return rollDirectionalSpike(rng, charisma);
        }

        double uniform = rng.nextDouble();
        double softBell = (rng.nextDouble() + rng.nextDouble()) / 2.0;

        // 0.0 = uniforme
        // 1.0 = campana suau completa
        double bellWeight = 0.18;

        double t = lerp(uniform, softBell, bellWeight);
        int face = 1 + (int) Math.round(t * 19.0);
        return clamp1to20(face);
    }

    /**
     * Genera un pic direccional ocasional.
     *
     * <p>
     * El carisma no força un resultat, però inclina lleugerament:
     * <ul>
     *   <li>si cau bé als déus, hi ha més tendència a pujada,</li>
     *   <li>si cau malament, hi ha més tendència a baixada,</li>
     *   <li>si és neutral, es manté la base.</li>
     * </ul>
     * </p>
     */
    private static int rollDirectionalSpike(Random rng, int charisma) {
        SpikeProfile profile = resolveSpikeProfile(charisma);

        boolean downward = rng.nextDouble() < profile.downwardChance();
        if (downward) {
            return rollLowSpike(rng, profile.lowExponent());
        }

        return rollHighSpike(rng, profile.highExponent());
    }

    /**
     * Resol els paràmetres del pic segons la relació del personatge amb els déus.
     *
     * <p>
     * L'efecte ha de ser visible però no determinant. Per això:
     * <ul>
     *   <li>la direcció es mou uns quants punts percentuals,</li>
     *   <li>la intensitat es modula lleugerament segons el tram exacte.</li>
     * </ul>
     * </p>
     */
    private static SpikeProfile resolveSpikeProfile(int charisma) {
        DivineCharismaAffinity.Standing standing =
                DivineCharismaAffinity.classifyStanding(charisma);

        DivineCharismaAffinity.Band band =
                DivineCharismaAffinity.classifyBand(charisma);

        double downwardChance = 0.65;
        double lowExponent = 2.00;
        double highExponent = 2.00;

        if (standing == DivineCharismaAffinity.Standing.FAVORED) {
            downwardChance = 0.57;
            lowExponent = 2.10;
            highExponent = 1.80;
        } else if (standing == DivineCharismaAffinity.Standing.DISLIKED) {
            downwardChance = 0.73;
            lowExponent = 1.80;
            highExponent = 2.10;
        }

        if (band == DivineCharismaAffinity.Band.FAVORED) {
            downwardChance -= 0.03;
            highExponent -= 0.08;
        } else if (band == DivineCharismaAffinity.Band.DISLIKED_LOW
                || band == DivineCharismaAffinity.Band.DISLIKED_HIGH) {
            downwardChance += 0.03;
            lowExponent -= 0.08;
        }

        downwardChance = Math.clamp(downwardChance, 0.52, 0.78);
        lowExponent = Math.clamp(lowExponent, 1.65, 2.30);
        highExponent = Math.clamp(highExponent, 1.65, 2.30);

        return new SpikeProfile(downwardChance, lowExponent, highExponent);
    }

    /**
     * Pic cap avall.
     *
     * <p>
     * Exponents més baixos carreguen més la tirada cap als valors baixos.
     * </p>
     */
    private static int rollLowSpike(Random rng, double exponent) {
        double t = Math.pow(rng.nextDouble(), exponent);
        int face = 1 + (int) Math.round(t * 8.0);
        return clamp1to20(face);
    }

    /**
     * Pic cap amunt.
     *
     * <p>
     * Exponents més baixos carreguen més la tirada cap als valors alts.
     * </p>
     */
    private static int rollHighSpike(Random rng, double exponent) {
        double t = 1.0 - Math.pow(rng.nextDouble(), exponent);
        int face = 20 - (int) Math.round((1.0 - t) * 8.0);
        return clamp1to20(face);
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