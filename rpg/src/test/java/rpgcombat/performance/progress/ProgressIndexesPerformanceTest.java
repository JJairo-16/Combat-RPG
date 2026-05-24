package rpgcombat.performance.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import rpgcombat.achievements.AchievementEvent;
import rpgcombat.achievements.AchievementProgress;
import rpgcombat.achievements.AchievementSystem;
import rpgcombat.achievements.AchievementUpdate;
import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.achievements.config.AchievementVisibility;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryProgress;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.discovery.config.DiscoveryCatalog;
import rpgcombat.discovery.config.DiscoveryCatalogConfig;
import rpgcombat.discovery.config.DiscoveryEntryConfig;
import rpgcombat.performance.PerformanceTestSupport;
import rpgcombat.performance.PerformanceTestSupport.TimedResult;
import rpgcombat.weapons.Arsenal;

class ProgressIndexesPerformanceTest {
    private static final int ITEMS = 4_096;
    private static final int LOOKUPS = 20_000;
    private static final int WARMUP = 2_000;
    private static final Path REPORT_PATH = Path.of("target", "performance-reports", "progress-indexes.txt");

    @BeforeAll
    static void preloadWeapons() throws IOException {
        Arsenal.preload(Path.of("data", "weapons.json"));
    }

    @Test
    void indexedProgressCountersBeatLinearCounts(TestReporter reporter) throws IOException {
        AchievementScenario achievements = achievements();
        DiscoveryScenario discoveries = discoveries();

        PerformanceTestSupport.repeat(() -> linearCompletedCount(achievements.progress()), WARMUP);
        PerformanceTestSupport.repeat(achievements.system()::completedCount, WARMUP);
        PerformanceTestSupport.repeat(() -> linearCategoryCount(discoveries.progress()), WARMUP);
        PerformanceTestSupport.repeat(() -> discoveries.system().discoveredCount(DiscoveryCategory.PERKS), WARMUP);
        PerformanceTestSupport.repeat(() -> linearTaggedCount(discoveries.progress(), discoveries.catalog()), WARMUP);
        PerformanceTestSupport.repeat(
                () -> discoveries.system().discoveredCount(DiscoveryCategory.PERKS, "benchmark"),
                WARMUP);

        TimedResult linearAchievements =
                PerformanceTestSupport.time(() -> linearCompletedCount(achievements.progress()), LOOKUPS);
        TimedResult indexedAchievements = PerformanceTestSupport.time(achievements.system()::completedCount, LOOKUPS);
        TimedResult linearCategory =
                PerformanceTestSupport.time(() -> linearCategoryCount(discoveries.progress()), LOOKUPS);
        TimedResult indexedCategory = PerformanceTestSupport.time(
                () -> discoveries.system().discoveredCount(DiscoveryCategory.PERKS),
                LOOKUPS);
        TimedResult linearTag =
                PerformanceTestSupport.time(() -> linearTaggedCount(discoveries.progress(), discoveries.catalog()), LOOKUPS);
        TimedResult indexedTag = PerformanceTestSupport.time(
                () -> discoveries.system().discoveredCount(DiscoveryCategory.PERKS, "benchmark"),
                LOOKUPS);

        int taggedCount = ITEMS / 2;
        assertEquals(ITEMS * LOOKUPS, linearAchievements.result());
        assertEquals(ITEMS * LOOKUPS, indexedAchievements.result());
        assertEquals(ITEMS * LOOKUPS, linearCategory.result());
        assertEquals(ITEMS * LOOKUPS, indexedCategory.result());
        assertEquals(taggedCount * LOOKUPS, linearTag.result());
        assertEquals(taggedCount * LOOKUPS, indexedTag.result());
        assertFaster("achievement count", linearAchievements, indexedAchievements);
        assertFaster("discovery category", linearCategory, indexedCategory);
        assertFaster("discovery tag", linearTag, indexedTag);

        String report = report(
                linearAchievements,
                indexedAchievements,
                linearCategory,
                indexedCategory,
                linearTag,
                indexedTag);
        reporter.publishEntry("progress-indexes-performance", report);
        PerformanceTestSupport.writeReport(REPORT_PATH, report);
    }

    private AchievementScenario achievements() throws IOException {
        List<AchievementDefinition> definitions = new ArrayList<>(ITEMS);
        List<AchievementProgress> progress = new ArrayList<>(ITEMS);
        for (int i = 0; i < ITEMS; i++) {
            AchievementDefinition definition = achievement("BENCHMARK_" + i);
            definitions.add(definition);
            progress.add(new AchievementProgress(definition, 1, 0, true, Instant.EPOCH));
        }

        Path savePath = Files.createTempDirectory("achievement-progress-index").resolve("achievements.json");
        AchievementSystem system = AchievementSystem.load(definitions, savePath.toString());
        system.onTurn(AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK));
        return new AchievementScenario(system, List.copyOf(progress));
    }

    private DiscoveryScenario discoveries() throws IOException {
        List<DiscoveryEntryConfig> entries = new ArrayList<>(ITEMS);
        for (int i = 0; i < ITEMS; i++) {
            entries.add(new DiscoveryEntryConfig(
                    DiscoveryCategory.PERKS.name(),
                    "BENCHMARK_" + i,
                    "Benchmark " + i,
                    "???",
                    "",
                    List.of(),
                    "",
                    "",
                    "",
                    "",
                    i % 2 == 0 ? List.of("benchmark") : List.of("other"),
                    false,
                    i));
        }

        DiscoveryCatalog catalog = DiscoveryCatalog.build(new DiscoveryCatalogConfig(1, List.of(), entries));
        Path savePath = Files.createTempDirectory("discovery-progress-index").resolve("discoveries.json");
        DiscoverySystem system = DiscoverySystem.load(catalog, savePath.toString());
        for (int i = 0; i < ITEMS; i++) {
            system.discover(DiscoveryCategory.PERKS, "BENCHMARK_" + i);
        }
        return new DiscoveryScenario(system, catalog, List.copyOf(system.progress()));
    }

    private AchievementDefinition achievement(String id) {
        return new AchievementDefinition(
                id,
                id,
                "",
                1,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        AchievementObjectiveType.COUNT_EVENT,
                        AchievementEvent.ACTION_ATTACK,
                        null,
                        null,
                        List.of(),
                        1,
                        0));
    }

    private int linearCompletedCount(List<AchievementProgress> progress) {
        int count = 0;
        for (AchievementProgress item : progress) {
            if (item.completed()) {
                count++;
            }
        }
        return count;
    }

    private int linearCategoryCount(List<DiscoveryProgress> progress) {
        int count = 0;
        for (DiscoveryProgress item : progress) {
            if (item.key().category() == DiscoveryCategory.PERKS) {
                count++;
            }
        }
        return count;
    }

    private int linearTaggedCount(List<DiscoveryProgress> progress, DiscoveryCatalog catalog) {
        int count = 0;
        for (DiscoveryProgress item : progress) {
            if (item.key().category() != DiscoveryCategory.PERKS) {
                continue;
            }
            if (catalog.find(item.key().category(), item.key().id())
                    .map(entry -> entry.tags().contains("benchmark"))
                    .orElse(false)) {
                count++;
            }
        }
        return count;
    }

    private void assertFaster(String label, TimedResult linear, TimedResult indexed) {
        assertTrue(indexed.wallNanos() < linear.wallNanos(),
                label + " linear=" + PerformanceTestSupport.compactMetrics(linear)
                        + " indexed=" + PerformanceTestSupport.compactMetrics(indexed));
    }

    private String report(
            TimedResult linearAchievements,
            TimedResult indexedAchievements,
            TimedResult linearCategory,
            TimedResult indexedCategory,
            TimedResult linearTag,
            TimedResult indexedTag) {
        return String.join(System.lineSeparator(),
                "Progress indexes performance",
                "========================================",
                "Items   : " + PerformanceTestSupport.integerText(ITEMS),
                "Lookups : " + PerformanceTestSupport.integerText(LOOKUPS),
                "",
                "Note: ramDelta is an approximate heap delta before/after each measured loop.",
                "",
                section("ACHIEVEMENT COMPLETED COUNT", linearAchievements, indexedAchievements),
                "",
                section("DISCOVERY CATEGORY COUNT", linearCategory, indexedCategory),
                "",
                section("DISCOVERY TAG COUNT", linearTag, indexedTag),
                "========================================");
    }

    private String section(String title, TimedResult linear, TimedResult indexed) {
        return String.join(System.lineSeparator(),
                title,
                "  Linear scan",
                PerformanceTestSupport.metrics(linear),
                "",
                "  Indexed lookup",
                PerformanceTestSupport.metrics(indexed),
                "",
                "  Speedup",
                "    wall : " + PerformanceTestSupport.speedupText(linear.wallNanos(), indexed.wallNanos()));
    }

    private record AchievementScenario(AchievementSystem system, List<AchievementProgress> progress) {
    }

    private record DiscoveryScenario(
            DiscoverySystem system,
            DiscoveryCatalog catalog,
            List<DiscoveryProgress> progress) {
    }
}
