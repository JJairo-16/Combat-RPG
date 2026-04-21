package rpgcombat.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Executa codi i genera informes detallats quan es produeix una fallada. */
public final class SafeExecutor {

    private final CrashReportWriter crashReportWriter;
    private final boolean autoWriteCrashReport;

    /** Crea un executor sense desat automàtic de crash-reports. */
    public SafeExecutor() {
        this(null, false);
    }

    /** Crea un executor configurant el desat automàtic i el seu escriptor. */
    public SafeExecutor(CrashReportWriter crashReportWriter, boolean autoWriteCrashReport) {
        this.crashReportWriter = crashReportWriter;
        this.autoWriteCrashReport = autoWriteCrashReport;
    }

    /** Crea un executor amb desat automàtic en una carpeta concreta. */
    public static SafeExecutor withAutomaticCrashReports(Path directory) {
        return new SafeExecutor(new CrashReportWriter(directory), true);
    }

    /** Retorna si el desat automàtic està activat. */
    public boolean isAutoWriteCrashReportEnabled() {
        return autoWriteCrashReport;
    }

    /** Executa una tasca sense retorn. */
    public ExecutionReport run(ThrowingRunnable action) {
        Objects.requireNonNull(action, "action");
        return run("Unnamed task", () -> {
            action.run();
            return null;
        });
    }

    /** Executa una tasca sense retorn amb nom. */
    public ExecutionReport run(String taskName, ThrowingRunnable action) {
        Objects.requireNonNull(action, "action");
        return run(taskName, () -> {
            action.run();
            return null;
        });
    }

    /** Executa una tasca amb retorn i en genera un informe. */
    public <T> ExecutionReport run(ThrowingSupplier<T> action) {
        return run("Unnamed task", action);
    }

    /** Executa una tasca amb retorn i en genera un informe. */
    public <T> ExecutionReport run(String taskName, ThrowingSupplier<T> action) {
        Objects.requireNonNull(taskName, "taskName");
        Objects.requireNonNull(action, "action");

        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();

        try {
            T result = action.get();
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            return ExecutionReport.success(taskName, startedAt, durationMillis, result);
        } catch (Throwable throwable) {
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            ExecutionReport report = ExecutionReport.failure(taskName, startedAt, durationMillis, throwable);

            if (autoWriteCrashReport && crashReportWriter != null) {
                try {
                    Path writtenFile = crashReportWriter.write(report);
                    report = report.withCrashReportFile(writtenFile);
                } catch (Exception writeFailure) {
                    report = report.withCrashReportWriteFailure(writeFailure);
                }
            }

            return report;
        }
    }

    /** Representa una operació que pot llençar qualsevol Throwable. */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /** Representa una operació amb retorn que pot llençar qualsevol Throwable. */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    /** Conté el resultat de l’execució i, si cal, el detall de l’error. */
    public static final class ExecutionReport {
        private final boolean success;
        private final String taskName;
        private final Instant startedAt;
        private final long durationMillis;
        private final Object result;
        private final Throwable throwable;
        private final String detailedReport;
        private final Path crashReportFile;
        private final Throwable crashReportWriteFailure;

        private ExecutionReport(
                boolean success,
                String taskName,
                Instant startedAt,
                long durationMillis,
                Object result,
                Throwable throwable,
                String detailedReport,
                Path crashReportFile,
                Throwable crashReportWriteFailure
        ) {
            this.success = success;
            this.taskName = taskName;
            this.startedAt = startedAt;
            this.durationMillis = durationMillis;
            this.result = result;
            this.throwable = throwable;
            this.detailedReport = detailedReport;
            this.crashReportFile = crashReportFile;
            this.crashReportWriteFailure = crashReportWriteFailure;
        }

        /** Crea un informe d’execució correcta. */
        public static ExecutionReport success(String taskName, Instant startedAt, long durationMillis, Object result) {
            StringBuilder report = new StringBuilder();
            report.append("Execution completed successfully.\n");
            report.append("Task name: ").append(taskName).append('\n');
            report.append("Started at: ").append(startedAt).append('\n');
            report.append("Duration: ").append(durationMillis).append(" ms\n");
            report.append("Result type: ").append(result == null ? "null" : result.getClass().getName()).append('\n');
            report.append("Result value: ").append(String.valueOf(result)).append('\n');

            return new ExecutionReport(
                    true,
                    taskName,
                    startedAt,
                    durationMillis,
                    result,
                    null,
                    report.toString(),
                    null,
                    null
            );
        }

        /** Crea un informe d’execució fallida. */
        public static ExecutionReport failure(String taskName, Instant startedAt, long durationMillis, Throwable throwable) {
            String report = buildDetailedErrorReport(taskName, startedAt, durationMillis, throwable);

            return new ExecutionReport(
                    false,
                    taskName,
                    startedAt,
                    durationMillis,
                    null,
                    throwable,
                    report,
                    null,
                    null
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTaskName() {
            return taskName;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public Object getResult() {
            return result;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        /** Retorna l’informe complet, preparat per a diagnòstic. */
        public String getDetailedReport() {
            StringBuilder sb = new StringBuilder(detailedReport);

            if (crashReportFile != null) {
                sb.append('\n');
                sb.append("Crash report file").append('\n');
                sb.append("-----------------").append('\n');
                sb.append("Saved at: ").append(crashReportFile.toAbsolutePath()).append('\n');
            }

            if (crashReportWriteFailure != null) {
                sb.append('\n');
                sb.append("Crash report persistence failure").append('\n');
                sb.append("-------------------------------").append('\n');
                sb.append("Type: ").append(crashReportWriteFailure.getClass().getName()).append('\n');
                sb.append("Message: ").append(nullToPlaceholder(crashReportWriteFailure.getMessage())).append('\n');
            }

            return sb.toString();
        }

        /** Indica si el report s’ha desat a disc. */
        public boolean hasCrashReportFile() {
            return crashReportFile != null;
        }

        /** Retorna el fitxer del crash-report, si existeix. */
        public Path getCrashReportFile() {
            return crashReportFile;
        }

        /** Indica si ha fallat el desat automàtic del report. */
        public boolean hasCrashReportWriteFailure() {
            return crashReportWriteFailure != null;
        }

        /** Retorna l’error de desat del report, si n’hi ha. */
        public Throwable getCrashReportWriteFailure() {
            return crashReportWriteFailure;
        }

        /** Imprimeix l’informe només si l’execució ha fallat. */
        public void printIfFailed() {
            if (!success) {
                System.err.println(getDetailedReport());
            }
        }

        @Override
        public String toString() {
            return getDetailedReport();
        }

        private ExecutionReport withCrashReportFile(Path crashReportFile) {
            return new ExecutionReport(
                    success,
                    taskName,
                    startedAt,
                    durationMillis,
                    result,
                    throwable,
                    detailedReport,
                    crashReportFile,
                    crashReportWriteFailure
            );
        }

        private ExecutionReport withCrashReportWriteFailure(Throwable crashReportWriteFailure) {
            return new ExecutionReport(
                    success,
                    taskName,
                    startedAt,
                    durationMillis,
                    result,
                    throwable,
                    detailedReport,
                    crashReportFile,
                    crashReportWriteFailure
            );
        }

        private static String buildDetailedErrorReport(
                String taskName,
                Instant startedAt,
                long durationMillis,
                Throwable throwable
        ) {
            StringBuilder sb = new StringBuilder();

            sb.append("Execution failed.\n");
            sb.append("Task name: ").append(taskName).append('\n');
            sb.append("Started at: ").append(startedAt).append('\n');
            sb.append("Duration before failure: ").append(durationMillis).append(" ms\n");
            sb.append("Thread: ").append(Thread.currentThread().getName()).append('\n');
            sb.append('\n');

            sb.append("Root throwable summary").append('\n');
            sb.append("----------------------").append('\n');
            sb.append("Type: ").append(throwable.getClass().getName()).append('\n');
            sb.append("Message: ").append(nullToPlaceholder(throwable.getMessage())).append('\n');
            sb.append("Localized message: ").append(nullToPlaceholder(throwable.getLocalizedMessage())).append('\n');
            sb.append('\n');

            List<Throwable> chain = extractCausalChain(throwable);

            sb.append("Causal chain").append('\n');
            sb.append("------------").append('\n');
            for (int i = 0; i < chain.size(); i++) {
                Throwable current = chain.get(i);
                sb.append('#').append(i).append('\n');
                sb.append("Type: ").append(current.getClass().getName()).append('\n');
                sb.append("Message: ").append(nullToPlaceholder(current.getMessage())).append('\n');

                StackTraceElement[] stack = current.getStackTrace();
                if (stack.length > 0) {
                    sb.append("First stack frame: ").append(formatStackTraceElement(stack[0])).append('\n');
                } else {
                    sb.append("First stack frame: <not available>").append('\n');
                }

                Throwable[] suppressed = current.getSuppressed();
                sb.append("Suppressed count: ").append(suppressed.length).append('\n');

                for (int j = 0; j < suppressed.length; j++) {
                    Throwable suppressedThrowable = suppressed[j];
                    sb.append("  Suppressed[").append(j).append("]: ")
                            .append(suppressedThrowable.getClass().getName())
                            .append(" - ")
                            .append(nullToPlaceholder(suppressedThrowable.getMessage()))
                            .append('\n');
                }

                sb.append('\n');
            }

            Throwable rootCause = chain.get(chain.size() - 1);
            sb.append("Likely root cause").append('\n');
            sb.append("-----------------").append('\n');
            sb.append("Type: ").append(rootCause.getClass().getName()).append('\n');
            sb.append("Message: ").append(nullToPlaceholder(rootCause.getMessage())).append('\n');
            if (rootCause.getStackTrace().length > 0) {
                sb.append("Origin: ").append(formatStackTraceElement(rootCause.getStackTrace()[0])).append('\n');
            } else {
                sb.append("Origin: <not available>").append('\n');
            }
            sb.append('\n');

            sb.append("Full stack trace").append('\n');
            sb.append("----------------").append('\n');
            sb.append(stackTraceToString(throwable));

            return sb.toString();
        }

        private static List<Throwable> extractCausalChain(Throwable throwable) {
            List<Throwable> chain = new ArrayList<>();
            Throwable current = throwable;

            while (current != null && !chain.contains(current)) {
                chain.add(current);
                current = current.getCause();
            }

            return Collections.unmodifiableList(chain);
        }

        private static String stackTraceToString(Throwable throwable) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        }

        private static String formatStackTraceElement(StackTraceElement element) {
            String fileName = element.getFileName() == null ? "Unknown Source" : element.getFileName();
            return element.getClassName()
                    + "."
                    + element.getMethodName()
                    + "("
                    + fileName
                    + ":"
                    + element.getLineNumber()
                    + ")";
        }

        private static String nullToPlaceholder(String value) {
            return value == null ? "<no message>" : value;
        }
    }
}