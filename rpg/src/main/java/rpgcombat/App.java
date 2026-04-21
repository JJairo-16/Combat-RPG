package rpgcombat;

import java.nio.file.Path;
import rpgcombat.debug.SafeExecutor;
import rpgcombat.debug.SafeExecutor.ExecutionReport;
import rpgcombat.game.GameLoop;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private final SafeExecutor executor = SafeExecutor.withAutomaticCrashReports(Path.of("rpg/crash-reports"));
    
    private GameBootstrap bootstrap;
    private GameLoop game;

    public void run() {
        ExecutionReport bootstrapReport = executor.run("Bootstrap", () -> {
            bootstrap = new GameBootstrap();
            game = bootstrap.createGame();
        });

        if (endIfCrashed(bootstrapReport))
            return;

        ExecutionReport report = executor.run("RPG Combat", () -> game.init());
        endIfCrashed(report);
    }

    private boolean endIfCrashed(ExecutionReport report) {
        if (report.isSuccess())
            return false;

        System.err.println(report.getDetailedReport());
        report.printIfFailed();
        return true;
    }
}