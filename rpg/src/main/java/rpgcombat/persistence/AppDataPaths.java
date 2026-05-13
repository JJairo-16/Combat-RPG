package rpgcombat.persistence;

import java.nio.file.Path;

/** Resol rutes de dades persistents de l'aplicació. */
public final class AppDataPaths {
    public static final String WINDOWS_APP_FOLDER = "The Broken Threshold";
    public static final String UNIX_APP_FOLDER = "the-broken-threshold";
    public static final String UNIX_HIDDEN_APP_FOLDER = ".the-broken-threshold";

    private static final String LEGACY_WINDOWS_APP_FOLDER = "RPGCombat";
    private static final String LEGACY_UNIX_APP_FOLDER = "rpgcombat";
    private static final String LEGACY_UNIX_HIDDEN_APP_FOLDER = ".rpgcombat";

    private AppDataPaths() {
    }

    /** Retorna el directori principal de dades de l'aplicació. */
    public static Path appDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, WINDOWS_APP_FOLDER);
        }

        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, UNIX_APP_FOLDER);
        }

        return Path.of(System.getProperty("user.home", "."), UNIX_HIDDEN_APP_FOLDER);
    }

    /** Retorna l'antiga ubicació d'AppData per migrar progrés existent. */
    public static Path legacyAppDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, LEGACY_WINDOWS_APP_FOLDER);
        }

        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, LEGACY_UNIX_APP_FOLDER);
        }

        return Path.of(System.getProperty("user.home", "."), LEGACY_UNIX_HIDDEN_APP_FOLDER);
    }

    /** Resol una ruta relativa dins AppData. */
    public static Path resolve(String configuredPath, String fallbackFile) {
        String file = configuredPath == null || configuredPath.isBlank() ? fallbackFile : configuredPath;
        Path path = Path.of(file);
        if (path.isAbsolute()) {
            return path;
        }
        return appDataDirectory().resolve(path).normalize();
    }

    /** Converteix una ruta nova en l'equivalent del directori antic, si escau. */
    public static Path legacyEquivalent(Path newPath) {
        if (newPath == null || newPath.isAbsolute() && !newPath.startsWith(appDataDirectory())) {
            return null;
        }

        Path base = appDataDirectory();
        if (!newPath.startsWith(base)) {
            return null;
        }
        return legacyAppDataDirectory().resolve(base.relativize(newPath)).normalize();
    }
}
