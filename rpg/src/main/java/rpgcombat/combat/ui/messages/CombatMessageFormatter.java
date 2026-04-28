package rpgcombat.combat.ui.messages;

import java.util.ArrayList;
import java.util.List;

import rpgcombat.utils.ui.Ansi;

/**
 * Dona estil visual als missatges del combat.
 */
public final class CombatMessageFormatter {

    /**
     * Formata el missatge principal de l'acció.
     */
    public String attacker(String message) {
        if (isBlank(message)) {
            return null;
        }
        return Ansi.BOLD + clean(message) + Ansi.RESET;
    }

    /**
     * Formata un missatge de defensa.
     */
    public String defense(String message) {
        if (isBlank(message)) {
            return null;
        }
        return "  " + Ansi.DARK_GRAY + MessageSymbol.HIT.glyph() + Ansi.RESET + " " + clean(message);
    }

    /**
     * Formata missatges d'efectes.
     */
    public List<String> effects(List<CombatMessage> messages) {
        List<String> lines = new ArrayList<>();
        if (messages == null) {
            return lines;
        }

        for (CombatMessage message : messages) {
            if (message != null && !isBlank(message.text())) {
                lines.add(render(message));
            }
        }
        return lines;
    }

    /**
     * Elimina codis ANSI per mesurar text visible.
     */
    public static String stripAnsi(String text) {
        return CombatMessage.stripAnsi(text);
    }

    private String render(CombatMessage message) {
        MessageColor color = message.color() == MessageColor.DEFAULT
                ? message.symbol().defaultColor()
                : message.color();
        String ansi = color == null ? null : color.ansi();
        String prefix = ansi == null
                ? message.symbol().glyph()
                : ansi + message.symbol().glyph() + Ansi.RESET;
        return "  " + prefix + " " + clean(message.text());
    }

    private String clean(String text) {
        return CombatMessage.stripAnsi(text).replace("\u001B", "").trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
