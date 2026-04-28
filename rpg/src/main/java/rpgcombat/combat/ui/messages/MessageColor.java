package rpgcombat.combat.ui.messages;

import rpgcombat.utils.ui.Ansi;

/**
 * Colors disponibles per als missatges de combat.
 */
public enum MessageColor {
    DEFAULT(null),
    RED(Ansi.RED),
    GREEN(Ansi.GREEN),
    YELLOW(Ansi.YELLOW),
    BLUE(Ansi.BLUE),
    MAGENTA(Ansi.MAGENTA),
    CYAN(Ansi.CYAN),
    WHITE(Ansi.WHITE),
    DARK_GRAY(Ansi.DARK_GRAY),
    BRIGHT_RED(Ansi.BRIGHT_RED),
    BRIGHT_BLUE(Ansi.BRIGHT_BLUE);

    private final String ansi;

    MessageColor(String ansi) {
        this.ansi = ansi;
    }

    /**
     * Retorna el codi ANSI del color.
     */
    public String ansi() {
        return ansi;
    }
}
