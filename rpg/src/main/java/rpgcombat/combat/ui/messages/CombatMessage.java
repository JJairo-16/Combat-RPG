package rpgcombat.combat.ui.messages;

import java.util.regex.Pattern;

/**
 * Missatge de combat amb estil explícit.
 */
public record CombatMessage(MessageSymbol symbol, MessageColor color, String text) {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;?0-9]*[ -/]*[@-~]");

    /**
     * Crea un missatge d'efecte.
     */
    public static CombatMessage of(MessageSymbol symbol, MessageColor color, String text) {
        MessageSymbol safeSymbol = symbol == null ? MessageSymbol.POSITIVE : symbol;
        MessageColor safeColor = color == null ? MessageColor.DEFAULT : color;
        return new CombatMessage(safeSymbol, safeColor, clean(text));
    }

    /**
     * Crea un missatge sense estil especial.
     */
    public static CombatMessage info(String text) {
        return of(MessageSymbol.INFO, MessageColor.DEFAULT, text);
    }

    /**
     * Crea un missatge positiu.
     */
    public static CombatMessage positive(String text) {
        return of(MessageSymbol.POSITIVE, MessageColor.DEFAULT, text);
    }

    /**
     * Crea un missatge negatiu.
     */
    public static CombatMessage negative(String text) {
        return of(MessageSymbol.NEGATIVE, MessageColor.DEFAULT, text);
    }

    /**
     * Crea un avís.
     */
    public static CombatMessage warning(String text) {
        return of(MessageSymbol.WARNING, MessageColor.DEFAULT, text);
    }

    /**
     * Crea un missatge de caos.
     */
    public static CombatMessage chaos(String text) {
        return of(MessageSymbol.CHAOS, MessageColor.DEFAULT, text);
    }

    /**
     * Crea un missatge d'impacte.
     */
    public static CombatMessage hit(String text) {
        return of(MessageSymbol.HIT, MessageColor.DEFAULT, text);
    }

    /**
     * Adaptador per a codi antic sense etiquetes noves.
     */
    public static CombatMessage legacy(String text) {
        String clean = clean(text);
        if (clean.isBlank()) {
            return info("");
        }

        char first = clean.charAt(0);
        if (first == '+') {
            return positive(clean.substring(1).trim());
        }
        if (first == '-') {
            return negative(clean.substring(1).trim());
        }
        if (first == '!') {
            return warning(clean.substring(1).trim());
        }
        if (first == '?') {
            return chaos(clean.substring(1).trim());
        }
        if (first == '→') {
            return hit(clean.substring(1).trim());
        }
        return positive(clean);
    }

    /**
     * Elimina codis ANSI del text.
     */
    public static String stripAnsi(String text) {
        if (text == null) {
            return "";
        }
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    private static String clean(String text) {
        return stripAnsi(text).replace("\u001B", "").trim();
    }
}
