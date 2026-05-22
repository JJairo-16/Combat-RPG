package rpgcombat.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.achievements.config.AchievementVisibility;

class AchievementBudgetPerformanceTest {
    private static final int ACHIEVEMENT_COUNT = 4_096;
    private static final int LOOKUPS = 30_000;
    private static final int WARMUP = 2_000;
    private static final Path REPORT_PATH = Path.of("target", "performance-reports", "achievement-budget.txt");

    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();

    @Test
    void indexedCandidatesBeatLinearScans(TestReporter reporter) throws IOException {
        List<AchievementProgress> linear = progressItems();
        AchievementBudget budget = new AchievementBudget(linear);
        AchievementUpdate update = AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK);

        int expectedCandidates = 1;

        repeat(() -> linearCandidates(linear, update).size(), WARMUP);
        repeat(() -> budget.candidates(update).size(), WARMUP);

        TimedResult linearCandidates = time(() -> linearCandidates(linear, update).size());
        TimedResult indexedCandidates = time(() -> budget.candidates(update).size());

        assertEquals(expectedCandidates * LOOKUPS, linearCandidates.candidateCount());
        assertEquals(expectedCandidates * LOOKUPS, indexedCandidates.candidateCount());
        assertTrue(
                indexedCandidates.wallNanos() < linearCandidates.wallNanos(),
                summary(linearCandidates, indexedCandidates));

        String report = report(linearCandidates, indexedCandidates);
        reporter.publishEntry("achievement-budget-performance", report);
        writeReport(report);
    }

    private List<AchievementProgress> progressItems() {
        List<AchievementProgress> progress = new ArrayList<>(ACHIEVEMENT_COUNT + 1);

        for (int i = 0; i < ACHIEVEMENT_COUNT; i++) {
            AchievementDefinition definition = countAchievement(
                    "irrelevant-" + i,
                    i % 2 == 0 ? AchievementEvent.ACTION_DEFEND : AchievementEvent.ACTION_ATTACK);
            progress.add(i % 2 == 0
                    ? new AchievementProgress(definition)
                    : completedProgress(definition));
        }

        progress.add(new AchievementProgress(countAchievement("attack-target", AchievementEvent.ACTION_ATTACK)));
        return List.copyOf(progress);
    }

    private AchievementProgress completedProgress(AchievementDefinition definition) {
        return new AchievementProgress(definition, 1.0, 0, true, Instant.EPOCH);
    }

    private AchievementDefinition countAchievement(String id, AchievementEvent event) {
        return new AchievementDefinition(
                id,
                id,
                "",
                1,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        AchievementObjectiveType.COUNT_EVENT,
                        event,
                        null,
                        null,
                        List.of(),
                        1.0,
                        0.0));
    }

    private List<AchievementProgress> linearCandidates(
            List<AchievementProgress> progressItems,
            AchievementUpdate update) {

        List<AchievementProgress> candidates = new ArrayList<>();
        for (AchievementProgress progress : progressItems) {
            if (observes(progress, update)) {
                candidates.add(progress);
            }
        }
        return List.copyOf(candidates);
    }

    private boolean observes(AchievementProgress progress, AchievementUpdate update) {
        if (progress == null || progress.completed() || progress.definition() == null) {
            return false;
        }

        AchievementObjective objective = progress.definition().objective();
        if (objective == null || objective.type() == null) {
            return true;
        }

        if (objective.type() == AchievementObjectiveType.ACTION_SEQUENCE) {
            return observesActionSequence(update);
        }

        if (observesEveryUpdate(objective.type()) || objective.event() == null) {
            return true;
        }

        return update != null && update.has(objective.event());
    }

    private boolean observesEveryUpdate(AchievementObjectiveType type) {
        return type == AchievementObjectiveType.AVOID_EVENT_FOR_TURNS
                || type == AchievementObjectiveType.CONSECUTIVE_EVENT
                || type == AchievementObjectiveType.STATE_MAINTAINED;
    }

    private boolean observesActionSequence(AchievementUpdate update) {
        return update != null
                && (update.has(AchievementEvent.ACTION_ATTACK)
                        || update.has(AchievementEvent.ACTION_DEFEND)
                        || update.has(AchievementEvent.ACTION_DODGE)
                        || update.has(AchievementEvent.ACTION_CHARGE));
    }

    private TimedResult time(CandidateQuery query) {
        Runtime runtime = Runtime.getRuntime();

        long ramBefore = usedMemory(runtime);
        long cpuBefore = currentThreadCpuTime();
        long wallBefore = System.nanoTime();

        int candidateCount = repeat(query, LOOKUPS);

        long wallAfter = System.nanoTime();
        long cpuAfter = currentThreadCpuTime();
        long ramAfter = usedMemory(runtime);

        return new TimedResult(
                wallAfter - wallBefore,
                cpuAfter - cpuBefore,
                ramAfter - ramBefore,
                candidateCount);
    }

    private int repeat(CandidateQuery query, int repetitions) {
        int candidateCount = 0;

        for (int i = 0; i < repetitions; i++) {
            candidateCount += query.count();
        }

        return candidateCount;
    }

    private String summary(TimedResult linear, TimedResult indexed) {
        return "AchievementBudget perf [candidates] "
                + "linear=" + compactMetrics(linear)
                + " indexed=" + compactMetrics(indexed);
    }

    private String compactMetrics(TimedResult result) {
        return "wall=" + millisText(result.wallNanos()) + "ms "
                + "cpu=" + millisText(result.cpuNanos()) + "ms "
                + "ramDelta=" + kib(result.ramDeltaBytes()) + "KiB";
    }

    private String report(TimedResult linear, TimedResult indexed) {
        return String.join(System.lineSeparator(),
                "AchievementBudget performance",
                "========================================",
                "Progress items : " + integerText(ACHIEVEMENT_COUNT + 1),
                "Lookups        : " + integerText(LOOKUPS),
                "Candidates     : 1 per lookup",
                "",
                "Note: ramDelta is an approximate heap delta before/after each measured loop.",
                "",
                "CANDIDATE LOOKUP",
                "  Linear scan",
                metrics(linear),
                "",
                "  Indexed lookup",
                metrics(indexed),
                "",
                "  Speedup",
                "    wall : " + speedupText(linear.wallNanos(), indexed.wallNanos()),
                "========================================");
    }

    private String metrics(TimedResult result) {
        return String.join(System.lineSeparator(),
                "    wall : " + millisText(result.wallNanos()) + " ms",
                "    cpu  : " + millisText(result.cpuNanos()) + " ms",
                "    ram  : " + kib(result.ramDeltaBytes()) + " KiB");
    }

    private void writeReport(String report) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        Files.writeString(REPORT_PATH, report + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private long currentThreadCpuTime() {
        if (!THREAD_BEAN.isCurrentThreadCpuTimeSupported()) {
            return 0L;
        }

        if (!THREAD_BEAN.isThreadCpuTimeEnabled()) {
            THREAD_BEAN.setThreadCpuTimeEnabled(true);
        }

        return THREAD_BEAN.getCurrentThreadCpuTime();
    }

    private long usedMemory(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String integerText(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private String millisText(long nanos) {
        return String.format(Locale.US, "%.2f", nanos / 1_000_000.0);
    }

    private long kib(long bytes) {
        return bytes / 1024;
    }

    private String speedupText(long slower, long faster) {
        if (faster <= 0) {
            return "N/A";
        }

        return String.format(Locale.US, "%.2fx", (double) slower / faster);
    }

    @FunctionalInterface
    private interface CandidateQuery {
        int count();
    }

    private record TimedResult(
            long wallNanos,
            long cpuNanos,
            long ramDeltaBytes,
            int candidateCount) {
    }
}
