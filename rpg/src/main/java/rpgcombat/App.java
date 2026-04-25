package rpgcombat;

import java.nio.file.Path;

import rpgcombat.app.AppController;
import rpgcombat.debug.SafeExecutor;
import rpgcombat.debug.SafeExecutor.ExecutionReport;

public class App {
    public static void main(String[] args) {
        new App().run();
    }

    private final SafeExecutor executor = SafeExecutor.withAutomaticCrashReports(Path.of("rpg/crash-reports"));

    public void run() {
        ExecutionReport report = executor.run("RPG Combat", () -> new AppController().run());
        endIfCrashed(report);
    }

    private boolean endIfCrashed(ExecutionReport report) {
        if (report.isSuccess()) {
            return false;
        }

        System.err.println(report.getDetailedReport());
        report.printIfFailed();
        return true;
    }
}
