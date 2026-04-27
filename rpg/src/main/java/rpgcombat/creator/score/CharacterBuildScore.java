package rpgcombat.creator.score;

import java.util.Arrays;

import rpgcombat.creator.CharacterCreator;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Stat;

/** Puntua una build seguint les regles del generador aleatori. */
public final class CharacterBuildScore {
    private static final int STAR_STEPS = 10;
    private static final int CORRUPT_POINT_DIFF = 20;

    private static final Stat[] STATS = Stat.values();
    private static final int STAT_COUNT = STATS.length;

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

    /** Calcula la qualitat global de la build. */
    public static Rating rate(int[] stats, Breed breed) {
        if (stats == null || stats.length != STAT_COUNT || breed == null) {
            return Rating.invalid(0.0);
        }

        int pointDiff = Math.abs(CharacterCreator.TOTAL_POINTS - Arrays.stream(stats).sum());
        boolean corrupt = pointDiff > CORRUPT_POINT_DIFF;

        int primary = primaryAttack(stats);
        int forbidden = forbiddenAttack(primary);

        double quality = 0.0;
        quality += attackScore(stats, primary) * 0.22;
        quality += focusBudgetScore(stats, primary, forbidden) * 0.22;
        quality += constitutionScore(stats) * 0.24;
        quality += supportScore(stats, primary, forbidden) * 0.14;
        quality += dumpScore(stats, forbidden) * 0.08;
        quality += breedScore(stats, breed, primary, forbidden) * 0.05;
        quality += spreadScore(stats, forbidden) * 0.05;

        double score = quality * 5.0;
        score *= budgetScore(stats);
        score -= strengthIntelligencePenalty(stats) * 1.85;
        score -= forbiddenAttackPenalty(stats, forbidden) * 1.10;
        score -= weakSurvivalPenalty(stats, primary) * 1.15;
        score -= excessiveDumpPenalty(stats, forbidden) * 0.85;
        score -= wastePenalty(stats, primary, forbidden) * 0.65;
        score = Math.min(score, qualityCeiling(stats, primary, forbidden));

        return new Rating(roundHalf(Math.clamp(score, 0.0, 5.0)), pointDiff == 0, corrupt);
    }

    /** Detecta si força i intel·ligència entren en conflicte. */
    public static PowerConflict detectPowerConflict(int[] stats) {
        if (stats == null || stats.length != STAT_COUNT) {
            return PowerConflict.invalid();
        }

        int primary = primaryAttack(stats);
        int forbidden = forbiddenAttack(primary);
        int dumpLimit = CharacterCreator.MIN_STAT + dumpExtraCap();
        double severity = internalConflictSeverity(stats);
        boolean conflict = severity >= 0.25;

        return new PowerConflict(
                true,
                conflict,
                STATS[primary],
                forbiddenStat(forbidden),
                stats[primary],
                statValue(stats, forbidden),
                dumpLimit,
                roundTwo(severity),
                conflictMessage(conflict));
    }

    /** Retorna la severitat del conflicte STR+INT. */
    public static double powerConflictSeverity(int[] stats) {
        if (stats == null || stats.length != STAT_COUNT) {
            return 0.0;
        }
        return roundTwo(internalConflictSeverity(stats));
    }

    /** Indica si hi ha conflicte intern real. */
    public static boolean hasPowerConflict(int[] stats) {
        return powerConflictSeverity(stats) >= 0.25;
    }

    /** Crea el missatge del conflicte detectat. */
    private static String conflictMessage(boolean conflict) {
        if (!conflict) {
            return "Fonts de poder coherents.";
        }
        return "Conflicte: Força i Intel·ligència competeixen entre elles.";
    }

    /** Valora que s'hagin gastat els punts correctes. */
    private static double budgetScore(int[] stats) {
        int diff = Math.abs(CharacterCreator.TOTAL_POINTS - Arrays.stream(stats).sum());
        return diff == 0 ? 1.0 : Math.clamp(1.0 - diff / 24.0, 0.0, 1.0);
    }

    /** Detecta la font principal d'atac. */
    private static int primaryAttack(int[] stats) {
        if (stats[DEX] >= stats[STR] && stats[DEX] >= stats[INT]) {
            return DEX;
        }
        return stats[STR] >= stats[INT] ? STR : INT;
    }

    /** Retorna l'atac prohibit per oposició estricta. */
    private static int forbiddenAttack(int primary) {
        if (primary == STR) {
            return INT;
        }
        if (primary == INT) {
            return STR;
        }
        return -1;
    }

    /** Indica si una stat és l'atac prohibit. */
    private static boolean isForbidden(int stat, int forbidden) {
        return forbidden >= 0 && stat == forbidden;
    }

    /** Retorna la stat prohibida, si existeix. */
    private static Stat forbiddenStat(int forbidden) {
        return forbidden >= 0 ? STATS[forbidden] : null;
    }

    /** Retorna el valor d'una stat opcional. */
    private static int statValue(int[] stats, int stat) {
        return stat >= 0 ? stats[stat] : 0;
    }

    /** Valora la potència de l'atac principal. */
    private static double attackScore(int[] stats, int primary) {
        double height = normalized(stats[primary], CharacterCreator.MIN_STAT + 12, CharacterCreator.MAX_STAT - 6);
        double separation = normalized(stats[primary] - strongestAllowedCompetitor(stats, primary), 0, 18);
        return Math.clamp(height * 0.72 + separation * 0.28, 0.0, 1.0);
    }

    /** Valora si el pressupost segueix el repartiment del generador. */
    private static double focusBudgetScore(int[] stats, int primary, int forbidden) {
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        double expectedFocusExtra = remaining * focusShare();

        int focusExtra = stats[primary] - CharacterCreator.MIN_STAT;
        int secondary = strongestFocusSecondary(stats, primary, forbidden);
        if (secondary != -1) {
            focusExtra += stats[secondary] - CharacterCreator.MIN_STAT;
        }

        double lowerFit = normalized(focusExtra, (int) Math.round(expectedFocusExtra * 0.72),
                (int) Math.round(expectedFocusExtra));
        double upperWaste = normalized(focusExtra, (int) Math.round(expectedFocusExtra * 1.34),
                (int) Math.round(expectedFocusExtra * 1.65));

        return Math.clamp(lowerFit - upperWaste * 0.35, 0.0, 1.0);
    }

    /** Valora la vida real i el mínim de supervivència. */
    private static double constitutionScore(int[] stats) {
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        int floor = CharacterCreator.MIN_STAT + (int) Math.round(remaining * conFloorShare());
        int excellent = floor + 13;

        double health = calculateMaxHealth(stats[CON]);
        double floorHealth = calculateMaxHealth(floor);
        double excellentHealth = calculateMaxHealth(excellent);
        double healthFit = safeRatio(health - floorHealth, excellentHealth - floorHealth);

        double regenCon = softenConstitution(stats[CON], REGEN_SOFTCAP_FACTOR);
        double regenFit = normalized((int) Math.round(regenCon), floor, excellent);
        double overflow = normalized(stats[CON], excellent + 7, CharacterCreator.MAX_STAT);

        return Math.clamp(healthFit * 0.88 + regenFit * 0.12 - overflow * 0.22, 0.0, 1.0);
    }

    /** Valora les sinergies no prohibides. */
    private static double supportScore(int[] stats, int primary, int forbidden) {
        double total = 0.0;
        double weightTotal = 0.0;

        for (int i = 0; i < stats.length; i++) {
            if (i == primary || isForbidden(i, forbidden)) {
                continue;
            }
            double weight = supportWeight(primary, i);
            if (weight <= 0.0) {
                continue;
            }
            total += normalized(stats[i], CharacterCreator.MIN_STAT + 4, CharacterCreator.MAX_STAT - 10) * weight;
            weightTotal += weight;
        }

        return weightTotal == 0.0 ? 0.0 : Math.clamp(total / weightTotal, 0.0, 1.0);
    }

    /** Valora que l'atac prohibit es mantingui baix. */
    private static double dumpScore(int[] stats, int forbidden) {
        if (forbidden < 0) {
            return 1.0;
        }
        int dumpLimit = CharacterCreator.MIN_STAT + dumpExtraCap();
        return 1.0 - normalized(stats[forbidden], dumpLimit, CharacterCreator.MAX_STAT);
    }

    /** Valora la raça segons la mateixa idea del generador. */
    private static double breedScore(int[] stats, Breed breed, int primary, int forbidden) {
        int bonus = breed.bonusStat().ordinal();
        if (isForbidden(bonus, forbidden)) {
            return 0.0;
        }
        if (bonus == primary) {
            return 1.0;
        }
        if (bonus == CON) {
            return constitutionScore(stats);
        }
        if (isSoftSynergy(primary, bonus)) {
            return normalized(stats[bonus], CharacterCreator.MIN_STAT + 4, CharacterCreator.MAX_STAT - 8);
        }
        return 0.45;
    }

    /** Evita premiar builds amb massa stats al mínim. */
    private static double spreadScore(int[] stats, int forbidden) {
        int usefulLow = 0;
        for (int i = 0; i < stats.length; i++) {
            if (isForbidden(i, forbidden)) {
                continue;
            }
            if (stats[i] <= CharacterCreator.MIN_STAT + 1) {
                usefulLow++;
            }
        }
        return Math.clamp(1.0 - usefulLow * 0.28, 0.0, 1.0);
    }

    /** Penalitza el híbrid STR+INT. */
    private static double strengthIntelligencePenalty(int[] stats) {
        return internalConflictSeverity(stats);
    }

    /** Mesura el conflicte real entre STR i INT. */
    private static double internalConflictSeverity(int[] stats) {
        int safeLimit = CharacterCreator.MIN_STAT + 6;
        int hardLimit = CharacterCreator.MIN_STAT + 20;
        return normalized(Math.min(stats[STR], stats[INT]), safeLimit, hardLimit);
    }

    /** Penalitza invertir massa en l'atac prohibit. */
    private static double forbiddenAttackPenalty(int[] stats, int forbidden) {
        if (forbidden < 0) {
            return 0.0;
        }
        int dumpLimit = CharacterCreator.MIN_STAT + dumpExtraCap();
        return normalized(stats[forbidden], dumpLimit + 2, CharacterCreator.MAX_STAT);
    }

    /** Penalitza poca vida segons el tipus de build. */
    private static double weakSurvivalPenalty(int[] stats, int primary) {
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        int floor = CharacterCreator.MIN_STAT + (int) Math.round(remaining * conFloorShare());
        int expected = primary == INT ? floor + 3 : floor + 6;
        return normalized(expected - stats[CON], 0, 10);
    }

    /** Penalitza hundir massa stats útils. */
    private static double excessiveDumpPenalty(int[] stats, int forbidden) {
        int low = 0;
        for (int i = 0; i < stats.length; i++) {
            if (isForbidden(i, forbidden)) {
                continue;
            }
            if (stats[i] <= CharacterCreator.MIN_STAT + 1) {
                low++;
            }
        }
        return Math.clamp(Math.max(0, low - 1) * 0.35, 0.0, 1.0);
    }

    /** Penalitza punts en llocs poc coherents. */
    private static double wastePenalty(int[] stats, int primary, int forbidden) {
        double waste = 0.0;
        double maxWaste = 0.0;
        int dumpLimit = CharacterCreator.MIN_STAT + dumpExtraCap();

        if (forbidden >= 0) {
            waste += Math.max(0, stats[forbidden] - dumpLimit) * 1.40;
            maxWaste += CharacterCreator.MAX_STAT - dumpLimit;
        }

        for (int i = 0; i < stats.length; i++) {
            if (i == primary || isForbidden(i, forbidden) || i == CON) {
                continue;
            }
            double weight = supportWeight(primary, i);
            if (weight < 0.30) {
                waste += Math.max(0, stats[i] - CharacterCreator.MIN_STAT - 12) * (0.35 - weight);
                maxWaste += 18.0;
            }
        }

        return maxWaste <= 0.0 ? 0.0 : Math.clamp(waste / maxWaste, 0.0, 1.0);
    }

    /** Limita la nota màxima en errors greus. */
    private static double qualityCeiling(int[] stats, int primary, int forbidden) {
        double ceiling = 5.0;
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        int conFloor = CharacterCreator.MIN_STAT + (int) Math.round(remaining * conFloorShare());
        int dumpLimit = CharacterCreator.MIN_STAT + dumpExtraCap();

        if (stats[CON] < conFloor) {
            ceiling = Math.min(ceiling, 4.0);
        }
        if (stats[CON] < conFloor - 2) {
            ceiling = Math.min(ceiling, 3.5);
        }
        if (primary != INT && stats[CON] < conFloor + 3 && stats[primary] >= CharacterCreator.MAX_STAT - 2) {
            ceiling = Math.min(ceiling, 4.0);
        }
        if (forbidden >= 0 && stats[forbidden] > dumpLimit + 8) {
            ceiling = Math.min(ceiling, 4.0);
        }
        if (internalConflictSeverity(stats) >= 0.25) {
            ceiling = Math.min(ceiling, 3.5);
        }
        return ceiling;
    }

    /** Retorna la millor stat secundària de focus. */
    private static int strongestFocusSecondary(int[] stats, int primary, int forbidden) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < stats.length; i++) {
            if (i == primary || isForbidden(i, forbidden)) {
                continue;
            }
            int score = (int) Math.round(stats[i] * supportWeight(primary, i) * 100.0);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    /** Retorna la competència d'atac permesa més forta. */
    private static int strongestAllowedCompetitor(int[] stats, int primary) {
        int strongest = CharacterCreator.MIN_STAT;
        int forbidden = forbiddenAttack(primary);
        if (primary != STR && !isForbidden(STR, forbidden)) {
            strongest = Math.max(strongest, stats[STR]);
        }
        if (primary != DEX) {
            strongest = Math.max(strongest, stats[DEX]);
        }
        if (primary != INT && !isForbidden(INT, forbidden)) {
            strongest = Math.max(strongest, stats[INT]);
        }
        return strongest;
    }

    /** Pes d'una stat secundària segons l'atac principal. */
    private static double supportWeight(int primary, int stat) {
        if (stat == CON) {
            return primary == INT ? 0.82 : 1.00;
        }
        if (primary == STR) {
            if (stat == DEX)
                return 0.70;
            if (stat == WIS)
                return 0.36;
            if (stat == CHA)
                return 0.28;
            if (stat == LUCK)
                return 0.36;
        } else if (primary == DEX) {
            if (stat == STR)
                return 0.42;
            if (stat == INT)
                return 0.62;
            if (stat == WIS)
                return 0.34;
            if (stat == CHA)
                return 0.48;
            if (stat == LUCK)
                return 0.72;
        } else if (primary == INT) {
            if (stat == DEX)
                return 0.42;
            if (stat == WIS)
                return 0.78;
            if (stat == CHA)
                return 0.40;
            if (stat == LUCK)
                return 0.26;
        }
        return 0.0;
    }

    /** Indica si la raça aporta una sinergia suau. */
    private static boolean isSoftSynergy(int primary, int stat) {
        return supportWeight(primary, stat) >= 0.42;
    }

    /** Mateix càlcul de focusShare del generador per 140 punts. */
    private static double focusShare() {
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        double slackRatio = remaining / (double) CharacterCreator.TOTAL_POINTS;
        return Math.clamp(0.78 - 0.35 * slackRatio, 0.55, 0.75);
    }

    /** Mateix límit de dump que el generador. */
    private static int dumpExtraCap() {
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        double slackRatio = remaining / (double) CharacterCreator.TOTAL_POINTS;
        double share = Math.clamp(0.16 - 0.10 * slackRatio, 0.08, 0.15);
        return (int) Math.round(remaining * share);
    }

    /** Mateix terra de CON que el generador. */
    private static double conFloorShare() {
        int remaining = CharacterCreator.TOTAL_POINTS - CharacterCreator.MIN_STAT * STAT_COUNT;
        double slackRatio = remaining / (double) CharacterCreator.TOTAL_POINTS;
        return Math.clamp(0.14 - 0.08 * slackRatio, 0.08, 0.14);
    }

    /** Calcula la vida màxima derivada de CON. */
    private static double calculateMaxHealth(int constitution) {
        return softenConstitution(constitution, HEALTH_SOFTCAP_FACTOR) * CON_HEALTH_VALUE;
    }

    /** Redueix el valor de CON després del punt fort. */
    private static double softenConstitution(double value, double factor) {
        if (value <= CON_FULL_EFFECT) {
            return value;
        }
        double extra = value - CON_FULL_EFFECT;
        return CON_FULL_EFFECT + (extra / (1.0 + extra * factor));
    }

    /** Normalitza un enter entre 0 i 1. */
    private static double normalized(int value, int min, int max) {
        if (max <= min) {
            return value >= max ? 1.0 : 0.0;
        }
        return Math.clamp((value - min) / (double) (max - min), 0.0, 1.0);
    }

    /** Divideix sense valors invàlids. */
    private static double safeRatio(double value, double max) {
        if (max <= 0.0) {
            return value >= 0.0 ? 1.0 : 0.0;
        }
        return Math.clamp(value / max, 0.0, 1.0);
    }

    /** Arrodoneix a dos decimals. */
    private static double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Arrodoneix al mig punt més proper. */
    private static double roundHalf(double value) {
        return Math.round(value * 2.0) / 2.0;
    }

    /** Resultat públic del conflicte de poder. */
    public record PowerConflict(
            boolean valid,
            boolean conflict,
            Stat primaryPower,
            Stat forbiddenPower,
            int primaryValue,
            int forbiddenValue,
            int recommendedLimit,
            double severity,
            String message) {

        /** Crea un resultat invàlid. */
        public static PowerConflict invalid() {
            return new PowerConflict(false, false, null, null, 0, 0, 0, 0.0, "Stats invàlides.");
        }
    }

    /** Resultat immutable de la puntuació. */
    public record Rating(double stars, boolean validPoints, boolean corrupt) {
        public Rating {
            stars = roundHalf(Math.clamp(stars, 0.0, 5.0));
        }

        /** Crea una puntuació invàlida. */
        public static Rating invalid(double stars) {
            return new Rating(stars, false, true);
        }

        /** Retorna la puntuació en estrelles. */
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
