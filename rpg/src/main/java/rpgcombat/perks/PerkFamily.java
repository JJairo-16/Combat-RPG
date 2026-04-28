package rpgcombat.perks;

import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;

/**
 * Representa la família d'una perk, amb la seva presentació visual.
 */
public enum PerkFamily {
    STRATEGY("Estratègia", MessageSymbol.STRATEGY, MessageColor.BRIGHT_BLUE),
    LUCK("Sort", MessageSymbol.LUCK, MessageColor.YELLOW),
    CHAOS("Caos", MessageSymbol.CHAOTIC, MessageColor.BRIGHT_RED),
    CORRUPTED("Corrupte", MessageSymbol.CORRUPTED, MessageColor.MAGENTA);

    private final String label;
    private final MessageSymbol symbol;
    private final MessageColor color;

    PerkFamily(String label, MessageSymbol symbol, MessageColor color) {
        this.label = label;
        this.symbol = symbol;
        this.color = color;
    }

    /** @return nom visible de la família */
    public String label() {
        return label;
    }

    /** @return símbol visual dels efectes de la família */
    public MessageSymbol symbol() {
        return symbol;
    }

    /** @return color visual dels efectes de la família */
    public MessageColor color() {
        return color;
    }
}