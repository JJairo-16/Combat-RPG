package rpgcombat.combat.ui.messages;

/**
 * Símbols semàntics dels missatges de combat.
 */
public enum MessageSymbol {
    POSITIVE("+", MessageColor.GREEN),
    NEGATIVE("-", MessageColor.RED),
    WARNING("!", MessageColor.YELLOW),
    CHAOS("?", MessageColor.MAGENTA),
    HIT("→", MessageColor.DARK_GRAY),
    INFO("·", MessageColor.CYAN),
    EQUAL("=", MessageColor.CYAN);

    private final String glyph;
    private final MessageColor defaultColor;

    MessageSymbol(String glyph, MessageColor defaultColor) {
        this.glyph = glyph;
        this.defaultColor = defaultColor;
    }

    /**
     * Retorna el símbol visible.
     */
    public String glyph() {
        return glyph;
    }

    /**
     * Retorna el color per defecte.
     */
    public MessageColor defaultColor() {
        return defaultColor;
    }
}
