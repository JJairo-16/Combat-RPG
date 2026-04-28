package rpgcombat.utils.cinematic.typing;

import rpgcombat.utils.cinematic.style.CinematicColor;

/**
 * Representa una acció que executa el motor d'escriptura.
 */
class TypingAction {
    private final ActionType type;
    private final char character;
    private final long delayMillis;
    private final CinematicColor color;

    /**
     * Constructor intern.
     */
    private TypingAction(ActionType type, char character, long delayMillis, CinematicColor color) {
        this.type = type;
        this.character = character;
        this.delayMillis = delayMillis;
        this.color = color;
    }

    /**
     * Crea una acció d'impressió de caràcter.
     */
    static TypingAction print(char character, long delayMillis) {
        return new TypingAction(ActionType.PRINT, character, delayMillis, null);
    }

    /**
     * Crea una pausa.
     */
    static TypingAction pause(long delayMillis) {
        return new TypingAction(ActionType.PAUSE, '\0', delayMillis, null);
    }

    /**
     * Crea un salt de línia.
     */
    static TypingAction newLine() {
        return new TypingAction(ActionType.NEW_LINE, '\n', 0, null);
    }

    /**
     * Crea un canvi de color.
     */
    static TypingAction color(CinematicColor color) {
        return new TypingAction(ActionType.COLOR, '\0', 0, color);
    }

    /**
     * Retorna el tipus d'acció.
     */
    ActionType type() { return type; }

    /**
     * Retorna el caràcter a imprimir.
     */
    char character() { return character; }

    /**
     * Retorna el retard associat.
     */
    long delayMillis() { return delayMillis; }

    /**
     * Retorna el color associat.
     */
    CinematicColor color() { return color; }
}