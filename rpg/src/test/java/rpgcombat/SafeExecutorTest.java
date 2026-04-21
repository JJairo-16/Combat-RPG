package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rpgcombat.debug.SafeExecutor;

/** Proves de comportament de SafeExecutor. */
class SafeExecutorTest {

    private SafeExecutor executor;
    private Path tempDir;
    private Path crashReportsDir;

    /** Prepara l'executor i la carpeta temporal de reports. */
    @BeforeEach
    void setUp() throws IOException {
        executor = new SafeExecutor();
        tempDir = Files.createTempDirectory("safe-executor-test-");
        crashReportsDir = tempDir.resolve("crash-reports");
    }

    /** Elimina els fitxers temporals generats durant la prova. */
    @AfterEach
    void tearDown() throws IOException {
        deleteRecursively(tempDir);
    }

    /** Comprova que una execució correcta es marca com a reeixida. */
    @Test
    void shouldReturnSuccessfulReportWhenExecutionDoesNotFail() {
        SafeExecutor.ExecutionReport report = executor.run("Success test", () -> "ok");

        assertNotNull(report);
        assertTrue(report.isSuccess());
        assertNull(report.getThrowable());
        assertFalse(report.hasCrashReportFile());
    }

    /** Comprova que una excepció genera un report fallit amb detall. */
    @Test
    void shouldReturnFailedReportWhenExecutionThrowsException() {
        SafeExecutor.ExecutionReport report = executor.run("Failure test", () -> {
            throw new IllegalStateException("Boom");
        });

        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertNotNull(report.getThrowable());
        assertTrue(report.getThrowable() instanceof IllegalStateException);
        assertTrue(report.getDetailedReport().contains("Execution failed."));
        assertTrue(report.getDetailedReport().contains("IllegalStateException"));
        assertTrue(report.getDetailedReport().contains("Boom"));
    }

    /** Comprova que també es capturen errors, no només excepcions. */
    @Test
    void shouldCaptureErrorsAsFailures() {
        SafeExecutor.ExecutionReport report = executor.run("Error test", () -> {
            throw new AssertionError("Serious failure");
        });

        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertNotNull(report.getThrowable());
        assertTrue(report.getThrowable() instanceof AssertionError);
        assertTrue(report.getDetailedReport().contains("AssertionError"));
        assertTrue(report.getDetailedReport().contains("Serious failure"));
    }

    /** Comprova que el report inclou la cadena de causes. */
    @Test
    void shouldIncludeCauseChainInDetailedReport() {
        SafeExecutor.ExecutionReport report = executor.run("Cause chain test", () -> {
            IllegalArgumentException cause = new IllegalArgumentException("Root cause");
            throw new IllegalStateException("Outer failure", cause);
        });

        assertFalse(report.isSuccess());
        assertTrue(report.getDetailedReport().contains("Causal chain"));
        assertTrue(report.getDetailedReport().contains("IllegalStateException"));
        assertTrue(report.getDetailedReport().contains("Outer failure"));
        assertTrue(report.getDetailedReport().contains("IllegalArgumentException"));
        assertTrue(report.getDetailedReport().contains("Root cause"));
    }

    /** Comprova que no es desa cap report automàticament per defecte. */
    @Test
    void shouldNotWriteCrashReportAutomaticallyByDefault() {
        SafeExecutor.ExecutionReport report = executor.run("Default auto-write test", () -> {
            throw new RuntimeException("Crash without persistence");
        });

        assertFalse(report.isSuccess());
        assertFalse(report.hasCrashReportFile());
        assertFalse(Files.exists(crashReportsDir));
    }

    /** Comprova que el report es desa automàticament si s'activa l'opció. */
    @Test
    void shouldWriteCrashReportAutomaticallyWhenEnabled() {
        SafeExecutor executorWithAutoWrite = SafeExecutor.withAutomaticCrashReports(crashReportsDir);

        SafeExecutor.ExecutionReport report = executorWithAutoWrite.run("Auto-write test", () -> {
            throw new RuntimeException("Crash with persistence");
        });

        assertFalse(report.isSuccess());
        assertTrue(report.hasCrashReportFile());
        assertNotNull(report.getCrashReportFile());
        assertTrue(Files.exists(report.getCrashReportFile()));
    }

    /** Comprova que el fitxer generat conté informació útil del crash. */
    @Test
    void shouldWriteCrashReportFileWithUsefulInformation() throws Exception {
        SafeExecutor executorWithAutoWrite = SafeExecutor.withAutomaticCrashReports(crashReportsDir);

        SafeExecutor.ExecutionReport report = executorWithAutoWrite.run("Persistent crash test", () -> {
            throw new IllegalArgumentException("Invalid data");
        });

        assertFalse(report.isSuccess());
        assertTrue(report.hasCrashReportFile());

        String content = Files.readString(report.getCrashReportFile());

        assertTrue(content.contains("Crash ID:"));
        assertTrue(content.contains("Task name: Persistent crash test"));
        assertTrue(content.contains("Detailed report"));
        assertTrue(content.contains("IllegalArgumentException"));
        assertTrue(content.contains("Invalid data"));
    }

    /** Comprova que printIfFailed no llença errors en cap cas. */
    @Test
    void shouldPrintOnlyWhenExecutionFails() {
        SafeExecutor.ExecutionReport success = executor.run("Print success test", () -> "ok");
        SafeExecutor.ExecutionReport failure = executor.run("Print failure test", () -> {
            throw new RuntimeException("Printable failure");
        });

        assertDoesNotThrow(success::printIfFailed);
        assertDoesNotThrow(failure::printIfFailed);
    }

    /** Elimina recursivament una carpeta temporal. */
    private void deleteRecursively(Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            return;
        }

        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(current -> {
                    try {
                        Files.deleteIfExists(current);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}