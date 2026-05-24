package rpgcombat.performance;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Utilitats compartides pels informes comparatius de rendiment. */
public final class PerformanceTestSupport {
    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();

    private PerformanceTestSupport() {
    }

    /** Mesura una consulta repetida i acumula el resultat per evitar eliminació del JIT. */
    public static TimedResult time(IntQuery query, int repetitions) {
        Runtime runtime = Runtime.getRuntime();
        long ramBefore = usedMemory(runtime);
        long cpuBefore = currentThreadCpuTime();
        long wallBefore = System.nanoTime();

        int result = repeat(query, repetitions);

        long wallAfter = System.nanoTime();
        long cpuAfter = currentThreadCpuTime();
        long ramAfter = usedMemory(runtime);
        return new TimedResult(wallAfter - wallBefore, cpuAfter - cpuBefore, ramAfter - ramBefore, result);
    }

    /** Executa una consulta repetida durant l'escalfament o la mesura. */
    public static int repeat(IntQuery query, int repetitions) {
        int result = 0;
        for (int i = 0; i < repetitions; i++) {
            result += query.get();
        }
        return result;
    }

    /** Desa l'informe en un fitxer del directori de reports de rendiment. */
    public static void writeReport(Path reportPath, String report) throws IOException {
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    /** Formata mètriques compactes per missatges d'assert. */
    public static String compactMetrics(TimedResult result) {
        return "wall=" + millisText(result.wallNanos()) + "ms "
                + "cpu=" + millisText(result.cpuNanos()) + "ms "
                + "ramDelta=" + kib(result.ramDeltaBytes()) + "KiB";
    }

    /** Formata les mètriques d'una secció d'informe. */
    public static String metrics(TimedResult result) {
        return String.join(System.lineSeparator(),
                "    wall : " + millisText(result.wallNanos()) + " ms",
                "    cpu  : " + millisText(result.cpuNanos()) + " ms",
                "    ram  : " + kib(result.ramDeltaBytes()) + " KiB");
    }

    /** Formata enters amb separador de milers estable. */
    public static String integerText(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    /** Formata la millora relativa entre dues mesures. */
    public static String speedupText(long slower, long faster) {
        if (faster <= 0) {
            return "N/A";
        }
        return String.format(Locale.US, "%.2fx", (double) slower / faster);
    }

    private static long currentThreadCpuTime() {
        if (!THREAD_BEAN.isCurrentThreadCpuTimeSupported()) {
            return 0L;
        }
        if (!THREAD_BEAN.isThreadCpuTimeEnabled()) {
            THREAD_BEAN.setThreadCpuTimeEnabled(true);
        }
        return THREAD_BEAN.getCurrentThreadCpuTime();
    }

    private static long usedMemory(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static String millisText(long nanos) {
        return String.format(Locale.US, "%.2f", nanos / 1_000_000.0);
    }

    private static long kib(long bytes) {
        return bytes / 1024;
    }

    /** Consulta entera mesurable. */
    @FunctionalInterface
    public interface IntQuery {
        int get();
    }

    /** Resultat brut d'una mesura. */
    public record TimedResult(long wallNanos, long cpuNanos, long ramDeltaBytes, int result) {
    }
}
