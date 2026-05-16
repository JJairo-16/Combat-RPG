package rpgcombat.combat.ui.messages;

import java.util.regex.Pattern;

/**
 * Missatge de combat amb estil explícit i metadades de presentació.
 */
public record CombatMessage(
        MessageSymbol symbol,
        MessageColor color,
        String text,
        CombatMessagePhase phase,
        CombatMessageKind kind,
        CombatMessagePlacement placement
) {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;?0-9]*[ -/]*[@-~]");

    public CombatMessage {
        symbol = symbol == null ? MessageSymbol.POSITIVE : symbol;
        color = color == null ? MessageColor.DEFAULT : color;
        text = clean(text);
        phase = phase == null ? CombatMessagePhase.DURING_CROSS : phase;
        kind = kind == null ? CombatMessageKind.NORMAL : kind;
        placement = placement == null ? placementFor(kind) : placement;
    }

    /**
     * Crea un missatge normal.
     */
    public static CombatMessage of(MessageSymbol symbol, MessageColor color, String text) {
        return of(symbol, color, text, CombatMessageKind.NORMAL);
    }

    /**
     * Crea un missatge amb tipus semàntic.
     */
    public static CombatMessage of(MessageSymbol symbol, MessageColor color, String text, CombatMessageKind kind) {
        return new CombatMessage(symbol, color, text, null, kind, placementFor(kind));
    }

    /**
     * Crea un missatge explícitament destinat al panell d'efectes.
     */
    public static CombatMessage statusEffect(MessageSymbol symbol, MessageColor color, String text) {
        return new CombatMessage(symbol, color, normalizeStatusEffectText(text),
                null, CombatMessageKind.STATUS_EFFECT, CombatMessagePlacement.EFFECT_PANEL);
    }

    /**
     * Crea un missatge de passiva divina. De moment es mostra com a missatge normal.
     */
    public static CombatMessage divinePerk(MessageSymbol symbol, MessageColor color, String text) {
        return new CombatMessage(symbol, color, text,
                null, CombatMessageKind.DIVINE_PERK, CombatMessagePlacement.MAIN_TIMELINE);
    }

    /**
     * Crea un missatge de mode de joc. De moment es mostra com a missatge normal.
     */
    public static CombatMessage gamemode(MessageSymbol symbol, MessageColor color, String text) {
        return new CombatMessage(symbol, color, text,
                null, CombatMessageKind.GAMEMODE, CombatMessagePlacement.MAIN_TIMELINE);
    }

    /**
     * Crea un missatge de mode de joc destinat al panell lateral d'efectes.
     */
    public static CombatMessage gamemodeEffect(MessageSymbol symbol, MessageColor color, String text) {
        return new CombatMessage(symbol, color, text,
                null, CombatMessageKind.GAMEMODE, CombatMessagePlacement.EFFECT_PANEL);
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
     * Crea un missatge de caos. Caos pertany a la categoria de mode de joc
     * i de moment es mostra com un missatge normal de la línia principal.
     */
    public static CombatMessage chaos(String text) {
        return gamemode(MessageSymbol.CHAOS, MessageColor.DEFAULT, text);
    }

    /**
     * Crea un missatge d'impacte.
     */
    public static CombatMessage hit(String text) {
        return of(MessageSymbol.HIT, MessageColor.DEFAULT, text);
    }

    /**
     * Retorna el mateix missatge amb una fase assignada si venia sense fase explícita.
     */
    public CombatMessage withPhase(CombatMessagePhase newPhase) {
        return new CombatMessage(symbol, color, text, newPhase, kind, placement);
    }

    /**
     * Retorna el mateix missatge amb tipus i col·locació nous, mantenint text i estil.
     */
    public CombatMessage asKind(CombatMessageKind newKind) {
        return new CombatMessage(symbol, color, text, phase, newKind, placementFor(newKind));
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

    private static CombatMessagePlacement placementFor(CombatMessageKind kind) {
        return kind == CombatMessageKind.STATUS_EFFECT
                ? CombatMessagePlacement.EFFECT_PANEL
                : CombatMessagePlacement.MAIN_TIMELINE;
    }

    private static String normalizeStatusEffectText(String text) {
        String clean = clean(text);
        while (clean.endsWith(".") || clean.endsWith("。")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        return clean;
    }

    private static String clean(String text) {
        return stripAnsi(text).replace("\u001B", "").trim();
    }
}
