package rpgcombat.debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Desa crash-reports en una carpeta concreta. */
public class CrashReportWriter {
    private static final DateTimeFormatter CRASH_ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");

    private final Path directory;
    private final ZoneId zoneId;

    /** Crea un escriptor per a una carpeta determinada. */
    public CrashReportWriter(Path directory) {
        this(directory, ZoneId.systemDefault());
    }

    /** Crea un escriptor per a una carpeta i zona horària determinades. */
    public CrashReportWriter(Path directory, ZoneId zoneId) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    /** Retorna la carpeta on es desaran els reports. */
    public Path getDirectory() {
        return directory;
    }

    /** Desa el report i en retorna el fitxer generat. */
    public Path write(SafeExecutor.ExecutionReport report) throws IOException {
        Objects.requireNonNull(report, "report");

        if (report.isSuccess()) {
            throw new IllegalArgumentException("Cannot write a crash report for a successful execution.");
        }

        Files.createDirectories(directory);

        String crashId = generateCrashId(report.getStartedAt());
        String fileName = "crash-" + crashId + ".log";
        Path file = directory.resolve(fileName);

        StringBuilder content = new StringBuilder();
        content.append("Crash ID: ").append(crashId).append('\n');
        content.append("Generated at: ").append(Instant.now()).append('\n');
        content.append("Task name: ").append(report.getTaskName()).append('\n');
        content.append("Started at: ").append(report.getStartedAt()).append('\n');
        content.append("Duration before failure: ").append(report.getDurationMillis()).append(" ms").append('\n');
        content.append('\n');
        content.append("Environment").append('\n');
        content.append("-----------").append('\n');
        content.append("Java version: ").append(System.getProperty("java.version")).append('\n');
        content.append("Java vendor: ").append(System.getProperty("java.vendor")).append('\n');
        content.append("JVM name: ").append(System.getProperty("java.vm.name")).append('\n');
        content.append("JVM version: ").append(System.getProperty("java.vm.version")).append('\n');
        content.append("OS name: ").append(System.getProperty("os.name")).append('\n');
        content.append("OS version: ").append(System.getProperty("os.version")).append('\n');
        content.append("OS architecture: ").append(System.getProperty("os.arch")).append('\n');
        content.append("User directory: ").append(System.getProperty("user.dir")).append('\n');
        content.append("Working timezone: ").append(zoneId).append('\n');
        content.append("Available processors: ").append(Runtime.getRuntime().availableProcessors()).append('\n');
        content.append("Max memory (bytes): ").append(Runtime.getRuntime().maxMemory()).append('\n');
        content.append("Total memory (bytes): ").append(Runtime.getRuntime().totalMemory()).append('\n');
        content.append("Free memory (bytes): ").append(Runtime.getRuntime().freeMemory()).append('\n');
        content.append('\n');
        content.append("Detailed report").append('\n');
        content.append("---------------").append('\n');
        content.append(report.getDetailedReport());

        Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /** Genera un identificador de crash amb data i hora fins a segons. */
    public String generateCrashId(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        ZonedDateTime dateTime = instant.atZone(zoneId);
        return CRASH_ID_FORMAT.format(dateTime);
    }
}