package rpgcombat.utils.cinematic.style;

/**
 * Colors ANSI disponibles per a la terminal.
 */
public enum CinematicColor {
    RESET("\033[0m"),
    BLACK("\033[30m"),
    RED("\033[31m"),
    GREEN("\033[32m"),
    YELLOW("\033[33m"),
    BLUE("\033[34m"),
    PURPLE("\033[35m"),
    CYAN("\033[36m"),
    WHITE("\033[37m"),
    GRAY("\033[90m"),
    BRIGHT_RED("\033[91m"),
    BRIGHT_GREEN("\033[92m"),
    BRIGHT_YELLOW("\033[93m"),
    BRIGHT_BLUE("\033[94m"),
    BRIGHT_PURPLE("\033[95m"),
    BRIGHT_CYAN("\033[96m"),
    BRIGHT_WHITE("\033[97m");

    private final String ansi;

    CinematicColor(String ansi) {
        this.ansi = ansi;
    }

    /**
     * Retorna el codi ANSI del color.
     */
    public String ansi() {
        return ansi;
    }

    /**
     * Converteix un nom de tag en un color.
     */
    public static CinematicColor fromTag(String tagName) {
        if (tagName == null) return null;

        return switch (tagName.toLowerCase()) {
            case "black" -> BLACK;
            case "red" -> RED;
            case "green" -> GREEN;
            case "yellow" -> YELLOW;
            case "blue" -> BLUE;
            case "purple" -> PURPLE;
            case "cyan" -> CYAN;
            case "white" -> WHITE;
            case "gray", "grey" -> GRAY;
            case "bright_red" -> BRIGHT_RED;
            case "bright_green" -> BRIGHT_GREEN;
            case "bright_yellow" -> BRIGHT_YELLOW;
            case "bright_blue" -> BRIGHT_BLUE;
            case "bright_purple" -> BRIGHT_PURPLE;
            case "bright_cyan" -> BRIGHT_CYAN;
            case "bright_white" -> BRIGHT_WHITE;
            default -> null;
        };
    }
}
