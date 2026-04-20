package rpgcombat.utils.interactive;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import rpgcombat.weapons.config.WeaponType;

/**
 * Utilitats de color i estil per a JLine.
 */
public final class JLineAnsi {

    public static final AttributedStyle RESET = AttributedStyle.DEFAULT;

    public static final AttributedStyle BOLD = AttributedStyle.DEFAULT.bold();

    public static final AttributedStyle WHITE = fg(AttributedStyle.WHITE);
    public static final AttributedStyle DARK_GRAY = fg(AttributedStyle.BRIGHT + AttributedStyle.BLACK);

    public static final AttributedStyle GREEN = fg(AttributedStyle.GREEN);
    public static final AttributedStyle GREEN_BOLD = GREEN.bold();

    public static final AttributedStyle YELLOW = fg(AttributedStyle.YELLOW);
    public static final AttributedStyle YELLOW_BOLD = YELLOW.bold();

    public static final AttributedStyle CYAN = fg(AttributedStyle.CYAN);
    public static final AttributedStyle CYAN_BOLD = CYAN.bold();

    public static final AttributedStyle BRIGHT_BLUE = fg(AttributedStyle.BLUE);
    public static final AttributedStyle MAGENTA = fg(AttributedStyle.MAGENTA);

   /** JLine no té taronja ANSI estàndard; es fa servir groc brillant. */
    public static final AttributedStyle ORANGE = fg(AttributedStyle.BRIGHT + AttributedStyle.YELLOW);

    private JLineAnsi() {
    }

   /** Afegeix text amb estil. */
    public static AttributedStringBuilder append(
            AttributedStringBuilder out,
            AttributedStyle style,
            String text) {

        return out.style(style).append(safe(text)).style(RESET);
    }

   /** Afegeix text sense estil. */
    public static AttributedStringBuilder append(
            AttributedStringBuilder out,
            String text) {

        return out.append(safe(text));
    }

   /** Afegeix una línia amb estil. */
    public static AttributedStringBuilder appendLine(
            AttributedStringBuilder out,
            AttributedStyle style,
            String text) {

        return append(out, style, text).append('\n');
    }

   /** Afegeix una línia sense estil. */
    public static AttributedStringBuilder appendLine(
            AttributedStringBuilder out,
            String text) {

        return append(out, text).append('\n');
    }

   /** Afegeix un salt de línia. */
    public static AttributedStringBuilder newLine(AttributedStringBuilder out) {
        return out.append('\n');
    }

   /** Retorna l'estil segons el tipus d'arma. */
    public static AttributedStyle weaponTypeStyle(WeaponType type) {
        if (type == null) {
            return WHITE;
        }

        return switch (type) {
            case PHYSICAL -> MAGENTA;
            case RANGE -> BRIGHT_BLUE;
            case MAGICAL -> ORANGE;
            default -> WHITE;
        };
    }

    private static AttributedStyle fg(int color) {
        return AttributedStyle.DEFAULT.foreground(color);
    }

    private static String safe(String text) {
        return (text == null) ? "" : text;
    }
}