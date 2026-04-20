package rpgcombat.debug.test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.rng.DivineCharismaAffinity.Band;
import rpgcombat.utils.rng.DivineCharismaAffinity.Profile;
import rpgcombat.utils.rng.DivineCharismaAffinity.Standing;

public final class DivineCharismaAffinityTest {

    private static final int SIMULATION_COUNT = 1_000_000;
    private static final int TEST_CHARISMA = 16;

    private DivineCharismaAffinityTest() {
    }

    public static void main(String[] args) {
        SimulationResult result = runSimulation(SIMULATION_COUNT, TEST_CHARISMA);

        printProfileFrequencies(result.profileCounts(), result.simulationCount());
        printCharismaResult(result.standingCounts(), result.bandCounts(), result.simulationCount(),
                result.testCharisma());

        double averageFavorScore = calculateAverageFavorScore(result.standingCounts(), result.simulationCount());
        printAverageFavorConclusion(averageFavorScore, result.testCharisma());
    }

    private static SimulationResult runSimulation(int simulationCount, int testCharisma) {
        Map<Profile, Integer> profileCounts = new HashMap<>();
        Map<Standing, Integer> standingCounts = createStandingCounter();
        Map<Band, Integer> bandCounts = createBandCounter();

        for (int i = 0; i < simulationCount; i++) {
            Profile profile = rollProfile(i);

            incrementProfileCount(profileCounts, profile);
            accumulateCharismaResult(standingCounts, bandCounts, testCharisma, profile);
        }

        return new SimulationResult(simulationCount, testCharisma, profileCounts, standingCounts, bandCounts);
    }

    private static Profile rollProfile(int seed) {
        Random rng = new Random(seed);
        DivineCharismaAffinity.rollForRun(rng);
        return DivineCharismaAffinity.currentProfile();
    }

    private static void accumulateCharismaResult(
            Map<Standing, Integer> standingCounts,
            Map<Band, Integer> bandCounts,
            int charisma,
            Profile profile) {

        Standing standing = DivineCharismaAffinity.classifyStanding(charisma, profile);
        Band band = DivineCharismaAffinity.classifyBand(charisma, profile);

        incrementEnumCount(standingCounts, standing);
        incrementEnumCount(bandCounts, band);
    }

    private static Map<Standing, Integer> createStandingCounter() {
        return new EnumMap<>(Standing.class);
    }

    private static Map<Band, Integer> createBandCounter() {
        return new EnumMap<>(Band.class);
    }

    private static void incrementProfileCount(Map<Profile, Integer> profileCounts, Profile profile) {
        profileCounts.put(profile, profileCounts.getOrDefault(profile, 0) + 1);
    }

    private static <T> void incrementEnumCount(Map<T, Integer> counts, T key) {
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }

    private static void printProfileFrequencies(Map<Profile, Integer> profileCounts, int simulationCount) {
        System.out.println("=== DIVINE PROFILE FREQUENCIES ===");
        System.out.println("Simulations: " + simulationCount);
        System.out.println();

        printProfileFrequencyHeader();
        printSortedProfileRows(profileCounts, simulationCount);
    }

    private static void printProfileFrequencyHeader() {
        System.out.printf("%-8s %-8s %-8s %-12s %-12s%n",
                "Center", "FavRad", "NeuRad", "Count", "Percent");
        System.out.println("--------------------------------------------------------");
    }

    private static void printSortedProfileRows(Map<Profile, Integer> profileCounts, int simulationCount) {
        profileCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(a.getKey().favoredCenter(), b.getKey().favoredCenter()))
                .forEach(entry -> printProfileRow(entry.getKey(), entry.getValue(), simulationCount));
    }

    private static void printProfileRow(Profile profile, int frequency, int simulationCount) {
        double percent = toPercent(frequency, simulationCount);

        System.out.printf("%-8d %-8d %-8d %-12d %8.4f%%%n",
                profile.favoredCenter(),
                profile.favoredRadius(),
                profile.neutralRadius(),
                frequency,
                percent);
    }

    private static void printCharismaResult(
            Map<Standing, Integer> standingCounts,
            Map<Band, Integer> bandCounts,
            int simulationCount,
            int testCharisma) {

        System.out.println();
        System.out.println("=== RESULT FOR CHARISMA = " + testCharisma + " ===");
        System.out.println();

        printStandingTable(standingCounts, simulationCount);
        System.out.println();
        printBandTable(bandCounts, simulationCount);
    }

    private static void printStandingTable(Map<Standing, Integer> standingCounts, int simulationCount) {
        System.out.printf("%-15s %-12s %-12s%n", "Standing", "Count", "Percent");
        System.out.println("---------------------------------------------");

        for (Standing standing : Standing.values()) {
            printEnumRow(standing.name(), standingCounts.getOrDefault(standing, 0), simulationCount);
        }
    }

    private static void printBandTable(Map<Band, Integer> bandCounts, int simulationCount) {
        System.out.printf("%-15s %-12s %-12s%n", "Band", "Count", "Percent");
        System.out.println("---------------------------------------------");

        for (Band band : Band.values()) {
            printEnumRow(band.name(), bandCounts.getOrDefault(band, 0), simulationCount);
        }
    }

    private static void printEnumRow(String label, int frequency, int simulationCount) {
        double percent = toPercent(frequency, simulationCount);

        System.out.printf("%-15s %-12d %8.4f%%%n",
                label,
                frequency,
                percent);
    }

    private static double calculateAverageFavorScore(Map<Standing, Integer> standingCounts, int simulationCount) {
        double favoredScore = standingCounts.getOrDefault(Standing.FAVORED, 0);
        double dislikedScore = standingCounts.getOrDefault(Standing.DISLIKED, 0) * -1.0;

        return (favoredScore + dislikedScore) / simulationCount;
    }

    private static void printAverageFavorConclusion(double averageFavorScore, int testCharisma) {
        System.out.println();
        System.out.printf("Average favor score for CHA %d: %.4f%n", testCharisma, averageFavorScore);
        System.out.println(buildConclusionMessage(averageFavorScore, testCharisma));
    }

    private static String buildConclusionMessage(double averageFavorScore, int testCharisma) {
        if (averageFavorScore > 0.25) {
            return "Conclusion: CHA " + testCharisma
                    + " is too favored; consider reducing the range or tightening the radii.";
        }

        if (averageFavorScore < -0.25) {
            return "Conclusion: CHA " + testCharisma
                    + " is too penalized; consider expanding the range or shifting the center.";
        }

        return "Conclusion: CHA " + testCharisma + " is reasonably balanced.";
    }

    private static double toPercent(int frequency, int total) {
        return (frequency * 100.0) / total;
    }

    private record SimulationResult(
            int simulationCount,
            int testCharisma,
            Map<Profile, Integer> profileCounts,
            Map<Standing, Integer> standingCounts,
            Map<Band, Integer> bandCounts) {
    }
}