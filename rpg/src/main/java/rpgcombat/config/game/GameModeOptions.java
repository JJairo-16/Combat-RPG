package rpgcombat.config.game;

/** Opcions generals de selecció del mode de joc. */
public record GameModeOptions(
        boolean selectionEnabled,
        String defaultMode) {

    public static final String DEFAULT_MODE = "NORMAL";

    public GameModeOptions {
        defaultMode = fallback(defaultMode, DEFAULT_MODE);
    }

    /** Configuració normal: demana mode abans de crear la partida. */
    public static GameModeOptions defaultConfig() {
        return new GameModeOptions(true, DEFAULT_MODE);
    }

    /** Configuració de depuració: evita menús extra i usa el mode normal. */
    public static GameModeOptions debugConfig() {
        return new GameModeOptions(false, DEFAULT_MODE);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
