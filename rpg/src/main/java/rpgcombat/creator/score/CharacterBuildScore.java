package rpgcombat.creator.score;

import rpgcombat.creator.CharacterCreator;

import java.util.Arrays;

import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Stat;

/** Puntua la qualitat d'una build de personatge. */
public final class CharacterBuildScore {
    private static final int STAR_STEPS = 10;
    private static final int CORRUPT_POINT_DIFF = 20;

    private static final int STR = Stat.STRENGTH.ordinal();
    private static final int DEX = Stat.DEXTERITY.ordinal();
    private static final int CON = Stat.CONSTITUTION.ordinal();
    private static final int INT = Stat.INTELLIGENCE.ordinal();
    private static final int WIS = Stat.WISDOM.ordinal();
    private static final int CHA = Stat.CHARISMA.ordinal();
    private static final int LUCK = Stat.LUCK.ordinal();

    private static final int CON_FULL_EFFECT = 20;
    private static final double CON_HEALTH_VALUE = 50.0;
    private static final double HEALTH_SOFTCAP_FACTOR = 0.08;
    private static final double REGEN_SOFTCAP_FACTOR = 0.10;

    private CharacterBuildScore() {
    }

    /**
     * Calcula la valoració global de la build.
     *
     * @param stats valors de les estadístiques
     * @param breed raça del personatge
     * @return puntuació en estrelles i estat de validesa
     */
    public static Rating rate(int[] stats, Breed breed) {
        if (stats == null || stats.length != Stat.values().length || breed == null) {
            return Rating.invalid(0.0);
        }

        int pointDiff = Math.abs(CharacterCreator.TOTAL_POINTS - Arrays.stream(stats).sum());
        boolean corrupt = pointDiff > CORRUPT_POINT_DIFF;

        double score = 0.0;
        score += budgetScore(stats) * 0.30;
        score += attackFocusScore(stats) * 0.85;
        score += repulsionScore(stats) * 0.75;
        score += constitutionScore(stats) * 1.10;
        score += survivalFitScore(stats) * 0.90;
        score += supportBalanceScore(stats) * 1.25;
        score += distributionScore(stats) * 0.95;
        score += raceScore(stats, breed) * 0.25;

        score -= lowConstitutionPenalty(stats) * 1.00;
        score -= dumpPenalty(stats) * 0.75;
        score -= variancePenalty(stats) * 0.50;
        score -= overSpecializationPenalty(stats) * 0.85;

        score = Math.min(score, qualityCeiling(stats));
        return new Rating(roundHalf(Math.clamp(score, 0.0, 5.0)), pointDiff == 0, corrupt);
    }

    /** Valora si s'han gastat correctament els punts disponibles. */
    private static double budgetScore(int[] stats) {
        int diff = Math.abs(CharacterCreator.TOTAL_POINTS - Arrays.stream(stats).sum());
        return diff == 0 ? 1.0 : Math.clamp(1.0 - diff / 24.0, 0.0, 1.0);
    }

    /** Valora la força de l'estadística ofensiva principal. */
    private static double attackFocusScore(int[] stats) {
        int primary = primaryAttack(stats);
        int opposite = oppositeAttack(primary);
        double height = normalized(stats[primary], CharacterCreator.MIN_STAT + 10, CharacterCreator.MAX_STAT);
        double separation = separationFromOtherAttacks(stats, primary);
        double oppositeLoad = opposite >= 0
                ? normalized(oppositeAttackValue(stats, primary), CharacterCreator.MIN_STAT, CharacterCreator.MAX_STAT)
                : 0.0;
        return Math.clamp(height * 0.55 + separation * 0.50 - oppositeLoad * 0.45, 0.0, 1.0);
    }

    /** Valora la constitució segons vida i regeneració. */
    private static double constitutionScore(int[] stats) {
        int value = stats[CON];
        int min = CharacterCreator.MIN_CONSTITUTION;
        double health = calculateMaxHealth(value);
        double baseline = calculateMaxHealth(min);
        double strong = calculateMaxHealth(CON_FULL_EFFECT + 6);
        double healthScore = Math.clamp((health - baseline) / (strong - baseline), 0.0, 1.0);

        double regenCon = softenConstitution(value, REGEN_SOFTCAP_FACTOR);
        double regenScore = normalized((int) Math.round(regenCon), min, CON_FULL_EFFECT + 5);
        double overflowPenalty = Math.max(0, value - (CON_FULL_EFFECT + 10)) / 26.0;

        return Math.clamp(healthScore * 0.75 + regenScore * 0.25 - overflowPenalty, 0.0, 1.0);
    }

    /** Valora si la constitució encaixa amb l'atac principal. */
    private static double survivalFitScore(int[] stats) {
        int con = stats[CON];
        int primary = primaryAttack(stats);
        int expected = expectedConstitution(stats, primary);
        int excellent = Math.min(CharacterCreator.MAX_STAT, expected + 6);
        return normalized(con, CharacterCreator.MIN_CONSTITUTION, excellent)
                * 0.45 + normalized(con, expected - 4, excellent) * 0.55;
    }

    /** Penalitza una constitució massa baixa. */
    private static double lowConstitutionPenalty(int[] stats) {
        int primary = primaryAttack(stats);
        int expected = expectedConstitution(stats, primary);
        int value = stats[CON];
        if (value >= expected) {
            return 0.0;
        }

        double deficit = (expected - value) / 12.0;
        double danger = value < CharacterCreator.MIN_CONSTITUTION + 7
                ? (CharacterCreator.MIN_CONSTITUTION + 7 - value) / 8.0
                : 0.0;
        return Math.clamp(deficit * 0.75 + danger * 0.55, 0.0, 1.0);
    }

    /** Calcula la constitució esperada per al rol ofensiu. */
    private static int expectedConstitution(int[] stats, int primary) {
        int power = stats[primary];
        if (primary == STR) {
            return clampInt(18 + (power - 24) / 2, 20, 28);
        }
        if (primary == DEX) {
            return clampInt(17 + (power - 24) / 3, 19, 25);
        }
        if (primary == INT) {
            return clampInt(16 + (power - 26) / 4, 18, 23);
        }
        return 20;
    }

    /** Calcula la vida màxima derivada de constitució. */
    private static double calculateMaxHealth(int con) {
        return softenConstitution(con, HEALTH_SOFTCAP_FACTOR) * CON_HEALTH_VALUE;
    }

    /** Redueix l'efecte de la constitució per sobre del límit ple. */
    private static double softenConstitution(double value, double factor) {
        if (value <= CON_FULL_EFFECT) {
            return value;
        }
        double extra = value - CON_FULL_EFFECT;
        return CON_FULL_EFFECT + (extra / (1.0 + extra * factor));
    }

    /** Valora la sinergia entre build i bonus de raça. */
    private static double raceScore(int[] stats, Breed breed) {
        int bonusIndex = breed.bonusStat().ordinal();
        int primary = primaryAttack(stats);
        int value = stats[bonusIndex];

        if (bonusIndex == primary) {
            return normalized(value, CharacterCreator.MIN_STAT + 8, CharacterCreator.MAX_STAT);
        }
        if (bonusIndex == CON) {
            return constitutionScore(stats);
        }
        if (isSupportStat(bonusIndex, primary)) {
            return supportStatScore(value, supportTarget(bonusIndex));
        }
        return 0.40;
    }

    /** Penalitza invertir massa en rols ofensius oposats. */
    private static double repulsionScore(int[] stats) {
        double strengthVsMagic = repulsionPenalty(stats[STR], stats[INT], 1.0);
        double dexterityVsMagic = repulsionPenalty(stats[DEX], stats[INT], 0.65);
        return Math.clamp(1.0 - Math.max(strengthVsMagic, dexterityVsMagic), 0.0, 1.0);
    }

    /** Calcula penalització entre dos atributs ofensius. */
    private static double repulsionPenalty(int a, int b, double weight) {
        double sharedPower = normalized(Math.min(a, b), CharacterCreator.MIN_STAT + 5, CharacterCreator.MAX_STAT);
        double closeness = 1.0 - Math.clamp(Math.abs(a - b) / 16.0, 0.0, 1.0);
        return Math.clamp(sharedPower * closeness * weight, 0.0, 1.0);
    }

    /** Valora l'equilibri de les estadístiques de suport. */
    private static double supportBalanceScore(int[] stats) {
        int primary = primaryAttack(stats);
        int opposite = oppositeAttack(primary);
        double total = 0.0;
        int count = 0;

        for (int index = 0; index < stats.length; index++) {
            if (index == primary || index == opposite || index == CON) {
                continue;
            }
            total += supportStatScore(stats[index], supportTarget(index));
            count++;
        }

        return count == 0 ? 1.0 : total / count;
    }

    /** Valora una estadística de suport respecte al seu objectiu. */
    private static double supportStatScore(int value, int target) {
        return normalized(value, CharacterCreator.MIN_STAT + 2, Math.min(target, CharacterCreator.MAX_STAT));
    }

    /** Retorna l'objectiu recomanat per a una estadística de suport. */
    private static int supportTarget(int statIndex) {
        if (statIndex == DEX || statIndex == LUCK) {
            return CharacterCreator.MIN_STAT + 8;
        }
        if (statIndex == WIS) {
            return CharacterCreator.MIN_STAT + 7;
        }
        if (statIndex == CHA) {
            return CharacterCreator.MIN_STAT + 6;
        }
        return CharacterCreator.MIN_STAT + 6;
    }

    /** Valora si la distribució evita extrems innecessaris. */
    private static double distributionScore(int[] stats) {
        int primary = primaryAttack(stats);
        int opposite = oppositeAttack(primary);
        int dumped = 0;
        int capped = 0;

        for (int value : stats) {
            if (value <= CharacterCreator.MIN_STAT + 1) {
                dumped++;
            }
            if (value >= CharacterCreator.MAX_STAT - 1) {
                capped++;
            }
        }

        int allowedDump = opposite >= 0 ? 1 : 0;
        double dumpCost = Math.max(0, dumped - allowedDump) * 0.16;
        double capCost = Math.max(0, capped - 1) * 0.10;
        double supportCost = lowestSupportPenalty(stats, primary, opposite);
        return Math.clamp(1.0 - dumpCost - capCost - supportCost, 0.0, 1.0);
    }

    /** Penalitza estadístiques de suport massa baixes. */
    private static double lowestSupportPenalty(int[] stats, int primary, int opposite) {
        int weak = 0;
        for (int i = 0; i < stats.length; i++) {
            if (i == primary || i == opposite || i == CON) {
                continue;
            }
            if (stats[i] <= CharacterCreator.MIN_STAT + 2) {
                weak++;
            }
        }
        return weak * 0.12;
    }

    /** Penalitza abusar de valors mínims. */
    private static double dumpPenalty(int[] stats) {
        int primary = primaryAttack(stats);
        int opposite = oppositeAttack(primary);
        int dumped = 0;
        int weakSupports = 0;

        for (int i = 0; i < stats.length; i++) {
            if (stats[i] <= CharacterCreator.MIN_STAT + 1) {
                dumped++;
            }
            if (i != primary && i != opposite && i != CON && stats[i] <= CharacterCreator.MIN_STAT + 2) {
                weakSupports++;
            }
        }

        int allowedDump = opposite >= 0 ? 1 : 0;
        double excessDump = Math.max(0, dumped - allowedDump) / 5.0;
        double supportDump = weakSupports / 4.0;
        return Math.clamp(excessDump + supportDump, 0.0, 1.0);
    }

    /** Penalitza maximitzar l'atac sense prou suport. */
    private static double overSpecializationPenalty(int[] stats) {
        int primary = primaryAttack(stats);
        int primaryValue = stats[primary];
        if (primaryValue < CharacterCreator.MAX_STAT - 4) {
            return 0.0;
        }

        double weakCon = Math.clamp((expectedConstitution(stats, primary) - stats[CON]) / 10.0, 0.0, 1.0);
        double weakSupports = Math.clamp((18.0 - supportAverage(stats, primary, oppositeAttack(primary))) / 8.0, 0.0,
                1.0);
        return Math.clamp(weakCon * 0.70 + weakSupports * 0.30, 0.0, 1.0);
    }

    /** Limita la nota màxima segons mancances greus. */
    private static double qualityCeiling(int[] stats) {
        int primary = primaryAttack(stats);
        int primaryValue = stats[primary];
        int expectedCon = expectedConstitution(stats, primary);
        double supportAverage = supportAverage(stats, primary, oppositeAttack(primary));

        double ceiling = 5.0;
        if (stats[CON] < expectedCon - 4) {
            ceiling = Math.min(ceiling, 4.0);
        }
        if (stats[CON] < expectedCon - 7) {
            ceiling = Math.min(ceiling, 3.5);
        }
        if (primaryValue >= CharacterCreator.MAX_STAT - 1 && stats[CON] < expectedCon) {
            ceiling = Math.min(ceiling, 4.5);
        }
        if (primaryValue >= CharacterCreator.MAX_STAT - 1 && supportAverage < 18.0) {
            ceiling = Math.min(ceiling, 4.5);
        }
        if (stats[CON] < CharacterCreator.MIN_CONSTITUTION + 4 && supportAverage < 17.0) {
            ceiling = Math.min(ceiling, 4.0);
        }
        return ceiling;
    }

    /** Calcula la mitjana de les estadístiques de suport. */
    private static double supportAverage(int[] stats, int primary, int opposite) {
        double total = 0.0;
        int count = 0;
        for (int i = 0; i < stats.length; i++) {
            if (i == primary || i == opposite || i == CON) {
                continue;
            }
            total += stats[i];
            count++;
        }
        return count == 0 ? CharacterCreator.MIN_STAT : total / count;
    }

    /** Penalitza una dispersió excessiva dels valors. */
    private static double variancePenalty(int[] stats) {
        int primary = primaryAttack(stats);
        int opposite = oppositeAttack(primary);
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < stats.length; i++) {
            if (i == opposite && stats[i] <= CharacterCreator.MIN_STAT + 2) {
                continue;
            }
            sum += stats[i];
            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        double mean = sum / count;

        double variance = 0.0;
        for (int i = 0; i < stats.length; i++) {
            if (i == opposite && stats[i] <= CharacterCreator.MIN_STAT + 2) {
                continue;
            }
            variance += Math.pow(stats[i] - mean, 2);
        }
        double stdDev = Math.sqrt(variance / Math.max(1, count));
        return Math.clamp((stdDev - 8.5) / 19.0, 0.0, 1.0);
    }

    /** Indica si una estadística és de suport per al rol actual. */
    private static boolean isSupportStat(int statIndex, int primary) {
        return statIndex != primary && statIndex != oppositeAttack(primary) && statIndex != CON;
    }

    /** Retorna l'estadística ofensiva principal. */
    private static int primaryAttack(int[] stats) {
        int primary = STR;
        if (stats[DEX] > stats[primary]) {
            primary = DEX;
        }
        if (stats[INT] > stats[primary]) {
            primary = INT;
        }
        return primary;
    }

    /** Retorna l'atac oposat al principal. */
    private static int oppositeAttack(int primary) {
        if (primary == STR || primary == DEX) {
            return INT;
        }
        if (primary == INT) {
            return STR;
        }
        return -1;
    }

    /** Calcula la separació respecte als altres atacs. */
    private static double separationFromOtherAttacks(int[] stats, int primary) {
        return Math.clamp((stats[primary] - oppositeAttackValue(stats, primary)) / 20.0, 0.0, 1.0);
    }

    /** Retorna el valor ofensiu oposat més rellevant. */
    private static int oppositeAttackValue(int[] stats, int primary) {
        if (primary == STR || primary == DEX) {
            return stats[INT];
        }
        if (primary == INT) {
            return Math.max(stats[STR], stats[DEX]);
        }
        return CharacterCreator.MIN_STAT;
    }

    /** Normalitza un valor entre 0 i 1. */
    private static double normalized(int value, int min, int max) {
        if (max <= min) {
            return value >= max ? 1.0 : 0.0;
        }
        return Math.clamp((value - min) / (double) (max - min), 0.0, 1.0);
    }

    /** Arrodoneix al mig punt més proper. */
    private static double roundHalf(double value) {
        return Math.round(value * 2.0) / 2.0;
    }

    /** Limita un enter dins d'un rang. */
    private static int clampInt(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    /** Resultat immutable de la puntuació. */
    public record Rating(double stars, boolean validPoints, boolean corrupt) {
        public Rating {
            stars = roundHalf(Math.clamp(stars, 0.0, 5.0));
        }

        /**
         * Crea una puntuació invàlida.
         *
         * @param stars estrelles inicials
         * @return valoració invàlida
         */
        public static Rating invalid(double stars) {
            return new Rating(stars, false, true);
        }

        /**
         * Retorna la puntuació com a estrelles.
         *
         * @return text amb estrelles o creus
         */
        public String starsText() {
            if (corrupt) {
                return "✖✖✖✖✖";
            }
            int halfSteps = (int) Math.round(stars * 2.0);
            StringBuilder out = new StringBuilder(STAR_STEPS / 2);
            for (int i = 0; i < STAR_STEPS; i += 2) {
                if (halfSteps >= i + 2) {
                    out.append('★');
                } else if (halfSteps == i + 1) {
                    out.append('⯨');
                } else {
                    out.append('☆');
                }
            }
            return out.toString();
        }
    }
}