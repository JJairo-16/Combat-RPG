package rpgcombat.combat.ui.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import rpgcombat.utils.ui.Ansi;

/**
 * Dona estil visual als missatges del combat.
 */
public final class CombatMessageFormatter {
    private static final Pattern LINE_SPLIT = Pattern.compile("\\R");

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
                lines.addAll(renderLines(message));
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

        if (message.placement() == CombatMessagePlacement.EFFECT_PANEL && ansi != null) {
            return "  " + ansi + message.symbol().glyph() + " " + clean(message.text()) + Ansi.RESET;
        }

        String prefix = ansi == null
                ? message.symbol().glyph()
                : ansi + message.symbol().glyph() + Ansi.RESET;
        return "  " + prefix + " " + clean(message.text());
    }

    private List<String> renderLines(CombatMessage message) {
        String text = message.text();
        if (text == null || !text.contains("\n")) {
            return List.of(render(message));
        }

        List<String> lines = new ArrayList<>();
        String[] rawLines = LINE_SPLIT.split(text);
        for (String rawLine : rawLines) {
            if (isBlank(rawLine)) {
                continue;
            }
            CombatMessage lineMessage = new CombatMessage(
                message.symbol(),
                message.color(),
                rawLine,
                message.phase(),
                message.kind(),
                message.placement()
            );
            lines.add(render(lineMessage));
        }
        return lines;
    }

    private String clean(String text) {
        return CombatMessage.stripAnsi(text).replace("\u001B", "").trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
