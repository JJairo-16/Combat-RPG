package rpgcombat;

import java.nio.file.Path;

import rpgcombat.app.AppController;
import rpgcombat.debug.SafeExecutor;
import rpgcombat.debug.SafeExecutor.ExecutionReport;
import rpgcombat.utils.terminal.SharedTerminal;

public class App {
    public static void main(String[] args) {
        new App().run();
    }

    private final SafeExecutor executor = SafeExecutor.withAutomaticCrashReports(Path.of("rpg/crash-reports"));

    public void run() {
        try {
            ExecutionReport report = executor.run("El Llindar Trencat", () -> new AppController().run());
            endIfCrashed(report);
        } finally {
            closeTerminal();
        }
    }

    private boolean endIfCrashed(ExecutionReport report) {
        if (report.isSuccess()) {
            return false;
        }

        System.err.println(report.getDetailedReport());
        report.printIfFailed();
        return true;
    }

    private void closeTerminal() {
        try {
            SharedTerminal.close();
        } catch (Exception ignored) {
            // El procés ja està sortint; no hi ha cap recuperació útil.
        }
    }
}
