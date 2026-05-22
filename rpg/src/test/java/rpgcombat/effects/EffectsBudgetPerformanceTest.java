package rpgcombat.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import rpgcombat.models.characters.EffectsBudget;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.triggers.Trigger;

class EffectsBudgetPerformanceTest {
    private static final int EFFECT_COUNT = 4_096;
    private static final int LOOKUPS = 30_000;
    private static final int WARMUP = 2_000;
    private static final Path REPORT_PATH = Path.of("target", "performance-reports", "effects-budget.txt");

    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();

    @Test
    void indexedHotLookupsBeatLinearScans(TestReporter reporter) throws IOException {
        EffectsBudget budget = new EffectsBudget();
        List<Effect> linear = new ArrayList<>(EFFECT_COUNT + 1);

        for (int i = 0; i < EFFECT_COUNT; i++) {
            Effect effect = new PassiveEffect("effect-" + i);
            budget.addEffect(effect);
            linear.add(effect);
        }

        Effect trigger = new PassiveTrigger("last-trigger");
        budget.addEffect(trigger);
        linear.add(trigger);

        repeat(() -> linearHasEffect(linear, "last-trigger"), WARMUP);
        repeat(() -> budget.hasEffect("last-trigger"), WARMUP);
        repeat(() -> linearContainsTrigger(linear), WARMUP);
        repeat(budget::containsTrigger, WARMUP);

        TimedResult linearKey = time(() -> linearHasEffect(linear, "last-trigger"));
        TimedResult indexedKey = time(() -> budget.hasEffect("last-trigger"));
        TimedResult linearTrigger = time(() -> linearContainsTrigger(linear));
        TimedResult indexedTrigger = time(budget::containsTrigger);

        assertEquals(LOOKUPS, linearKey.hits());
        assertEquals(LOOKUPS, indexedKey.hits());
        assertEquals(LOOKUPS, linearTrigger.hits());
        assertEquals(LOOKUPS, indexedTrigger.hits());

        assertTrue(
                indexedKey.wallNanos() < linearKey.wallNanos(),
                summary("key", linearKey, indexedKey));

        assertTrue(
                indexedTrigger.wallNanos() < linearTrigger.wallNanos(),
                summary("trigger", linearTrigger, indexedTrigger));

        String report = report(linearKey, indexedKey, linearTrigger, indexedTrigger);
        reporter.publishEntry("effects-budget-performance", report);
        writeReport(report);
    }

    private TimedResult time(BooleanQuery query) {
        Runtime runtime = Runtime.getRuntime();

        long ramBefore = usedMemory(runtime);
        long cpuBefore = currentThreadCpuTime();
        long wallBefore = System.nanoTime();

        int hits = repeat(query, LOOKUPS);

        long wallAfter = System.nanoTime();
        long cpuAfter = currentThreadCpuTime();
        long ramAfter = usedMemory(runtime);

        return new TimedResult(
                wallAfter - wallBefore,
                cpuAfter - cpuBefore,
                ramAfter - ramBefore,
                hits);
    }

    private int repeat(BooleanQuery query, int repetitions) {
        int hits = 0;

        for (int i = 0; i < repetitions; i++) {
            if (query.get()) {
                hits++;
            }
        }

        return hits;
    }

    private boolean linearHasEffect(List<Effect> effects, String key) {
        for (Effect effect : effects) {
            if (!effect.isExpired() && key.equals(effect.key())) {
                return true;
            }
        }

        return false;
    }

    private boolean linearContainsTrigger(List<Effect> effects) {
        for (Effect effect : effects) {
            if (effect instanceof Trigger) {
                return true;
            }
        }

        return false;
    }

    private String summary(String label, TimedResult linear, TimedResult indexed) {
        return "EffectsBudget perf [" + label + "] "
                + "linear=" + compactMetrics(linear)
                + " indexed=" + compactMetrics(indexed);
    }

    private String compactMetrics(TimedResult result) {
        return "wall=" + millisText(result.wallNanos()) + "ms "
                + "cpu=" + millisText(result.cpuNanos()) + "ms "
                + "ramDelta=" + kib(result.ramDeltaBytes()) + "KiB";
    }

    private String report(
            TimedResult linearKey,
            TimedResult indexedKey,
            TimedResult linearTrigger,
            TimedResult indexedTrigger) {

        return String.join(System.lineSeparator(),
                "EffectsBudget performance",
                "========================================",
                "Effects : " + integerText(EFFECT_COUNT + 1),
                "Lookups : " + integerText(LOOKUPS),
                "",
                "Note: ramDelta is an approximate heap delta before/after each measured loop.",
                "",
                section("KEY LOOKUP", linearKey, indexedKey),
                "",
                section("TRIGGER LOOKUP", linearTrigger, indexedTrigger),
                "========================================");
    }

    private String section(String title, TimedResult linear, TimedResult indexed) {
        return String.join(System.lineSeparator(),
                title,
                "  Linear scan",
                metrics(linear),
                "",
                "  Indexed lookup",
                metrics(indexed),
                "",
                "  Speedup",
                "    wall : " + speedupText(linear.wallNanos(), indexed.wallNanos()));
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
    private interface BooleanQuery {
        boolean get();
    }

    private record TimedResult(
            long wallNanos,
            long cpuNanos,
            long ramDeltaBytes,
            int hits) {
    }

    private static class PassiveEffect implements Effect {
        private final String key;
        private final EffectState state = EffectState.ofStacks(1);

        PassiveEffect(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public EffectState state() {
            return state;
        }
    }

    private static final class PassiveTrigger extends Trigger {
        PassiveTrigger(String key) {
            super(key);
        }
    }
}