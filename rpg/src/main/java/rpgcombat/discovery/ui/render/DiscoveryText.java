package rpgcombat.discovery.ui.render;

import static rpgcombat.discovery.ui.render.DiscoveryUiStyle.*;

import java.util.ArrayList;
import java.util.List;

/** Funcions de format i text sense estat del visor. */
public final class DiscoveryText {

    /** Evita crear instàncies d'aquesta utilitat. */
    private DiscoveryText() {
    }

    /** Afegeix text de detall ajustat a l'amplada. */
    static void addDetailWrapped(List<String> lines, String text, int width, String style) {
        int contentWidth = detailContentWidth(width);

        for (String line : wrapPlain(text, contentWidth)) {
            lines.add(detailCell(line, width, style));
        }
    }

    /** Afegeix una llista amb vinyeta ajustada a l'amplada. */
    static void addBulletWrapped(List<String> lines, String text, int width, String style) {
        int contentWidth = detailContentWidth(width);
        int continuationIndent = displayWidth(BULLET);
        int wrappedWidth = Math.max(DETAIL_CONTENT_MIN_WIDTH, contentWidth - continuationIndent);

        List<String> wrapped = wrapPlain(text, wrappedWidth);

        for (int i = 0; i < wrapped.size(); i++) {
            String prefix = i == 0 ? BULLET : " ".repeat(continuationIndent);
            String plain = prefix + wrapped.get(i);

            if (hasText(style)) {
                lines.add(detailCell(plain, width, style));
            } else {
                lines.add(detailStatCell(plain, width));
            }
        }
    }

    /** Divideix text pla en línies. */
    static List<String> wrapPlain(String text, int width) {
        List<String> lines = new ArrayList<>();
        String safe = text == null ? "" : text.trim();
        int maxWidth = Math.max(DETAIL_CONTENT_MIN_WIDTH, width);

        if (safe.isBlank()) {
            lines.add(EMPTY_INFORMATION);
            return lines;
        }

        while (!safe.isEmpty()) {
            if (displayWidth(safe) <= maxWidth) {
                lines.add(safe);
                break;
            }

            int cut = findCut(safe, maxWidth);
            lines.add(safe.substring(0, cut).trim());
            safe = safe.substring(Math.min(cut, safe.length())).trim();
        }

        return lines;
    }

    /** Crea una cel·la amb estil opcional. */
    static String cell(String text, int width, String style) {
        String plain = padPlain(text, width);
        if (!hasText(style)) {
            return plain;
        }
        return style + plain + RESET;
    }

    /** Crea una cel·la marcada com a seleccionada. */
    static String selectedCell(String text, int width) {
        return SELECTED + padPlain(text, width) + RESET;
    }

    /** Crea una cel·la del panell de detall. */
    static String detailCell(String text, int width, String style) {
        int contentWidth = detailContentWidth(width);
        String content = padPlain(text, contentWidth);

        if (!hasText(style)) {
            return detailPadding() + content + detailPadding();
        }

        return detailPadding() + style + content + RESET + detailPadding();
    }

    /** Crea una insígnia de detall. */
    static String detailBadgeCell(String text, int width, String style) {
        int contentWidth = detailContentWidth(width);
        String fitted = fitPlain(text, contentWidth);
        int missing = Math.max(0, contentWidth - displayWidth(fitted));

        return detailPadding()
                + style
                + fitted
                + RESET
                + " ".repeat(missing)
                + detailPadding();
    }

    /** Crea una línia de detall amb valors acolorits. */
    static String detailStatCell(String plain, int width) {
        int contentWidth = detailContentWidth(width);
        String fitted = fitPlain(plain, contentWidth);
        String styled = colorizeStatLine(fitted);
        int missing = Math.max(0, contentWidth - displayWidth(fitted));

        return detailPadding()
                + styled
                + " ".repeat(missing)
                + detailPadding();
    }

    /** Crea una cel·la estadística amb valors acolorits. */
    static String statCell(String plain, int width) {
        String fitted = fitPlain(plain, width);
        String styled = colorizeStatLine(fitted);
        int missing = Math.max(0, width - displayWidth(fitted));
        return styled + " ".repeat(missing);
    }

    /** Emplena una cadena amb estil fins a l'amplada indicada. */
    static String padStyled(String styled, String plain, int width) {
        int missing = Math.max(0, width - displayWidth(plain));
        return styled + " ".repeat(missing);
    }

    /** Retalla i emplena text pla. */
    static String padPlain(String text, int width) {
        String fitted = fitPlain(text, width);
        int missing = Math.max(0, width - displayWidth(fitted));
        return fitted + " ".repeat(missing);
    }

    /** Retalla text pla amb punts suspensius si cal. */
    static String fitPlain(String text, int width) {
        String safe = text == null ? "" : text;
        int maxWidth = Math.max(0, width);

        if (displayWidth(safe) <= maxWidth) {
            return safe;
        }

        if (maxWidth <= 1) {
            return safe.substring(0, Math.min(safe.length(), maxWidth));
        }

        return safe.substring(0, Math.min(safe.length(), maxWidth - 1)) + "…";
    }

    /** Centra text pla dins una amplada. */
    static String centerPlain(String text, int width) {
        String fitted = fitPlain(text, width);
        int missing = Math.max(0, width - displayWidth(fitted));
        int left = missing / 2;
        int right = missing - left;
        return " ".repeat(left) + fitted + " ".repeat(right);
    }

    /** Acoloreix una línia de dades. */
    static String colorizeStatLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }

        int start = 0;
        StringBuilder out = new StringBuilder(line.length() + ANSI_EXTRA_CAPACITY);

        if (line.startsWith(BULLET)) {
            out.append(MUTED).append(BULLET).append(RESET);
            start = displayWidth(BULLET);
        } else if (line.startsWith(" ".repeat(displayWidth(BULLET)))) {
            out.append(" ".repeat(displayWidth(BULLET)));
            start = displayWidth(BULLET);
        }

        int colon = line.indexOf(':', start);

        if (colon >= start) {
            String label = line.substring(start, colon + 1);
            String value = line.substring(colon + 1);

            out.append(BOLD).append(WHITE).append(label).append(RESET);
            if (isDifficultyLabel(label)) {
                appendDifficultyStars(out, value);
                return out.toString();
            }
            appendColoredValue(out, value, styleForLabel(label));
            return out.toString();
        }

        appendColoredValue(out, line.substring(start), WHITE);
        return out.toString();
    }

    /** Oculta un títol no descobert. */
    static String maskTitle(String title) {
        if (title == null || title.isBlank()) {
            return LOCKED_VALUE;
        }

        if (title.contains("?")) {
            return title;
        }

        return LOCKED_VALUE;
    }

    /** Oculta una frase mantenint la puntuació final. */
    static String maskSentence(String text) {
        if (!hasText(text)) {
            return LOCKED_VALUE;
        }

        String trimmed = text.trim();
        int words = clamp(countWords(trimmed), MASK_MIN_WORDS, MASK_MAX_WORDS);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < words; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(LOCKED_VALUE);
        }

        if (endsWithSentenceMark(trimmed)) {
            out.append(trimmed.charAt(trimmed.length() - 1));
        }

        return out.toString();
    }

    /** Oculta el valor d'una línia de detall. */
    static String maskDetail(String detail) {
        if (!hasText(detail)) {
            return LOCKED_VALUE;
        }

        String trimmed = detail.trim();
        int colon = trimmed.indexOf(':');

        if (colon >= 0) {
            String label = trimmed.substring(0, colon + 1);
            return label + " " + LOCKED_VALUE;
        }

        int separator = indexOfSeparator(trimmed);
        if (separator >= 0) {
            String label = trimmed.substring(0, separator + 1);
            return label + " " + LOCKED_VALUE;
        }

        return LOCKED_VALUE;
    }

    /** Converteix text a majúscules. */
    static String upper(String text) {
        return text == null ? "" : text.toUpperCase();
    }

    /** Converteix text a minúscules. */
    public static String lower(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    /** Retorna una línia per índex o buit. */
    static String lineAt(List<String> lines, int index) {
        return lines != null && index >= 0 && index < lines.size() ? lines.get(index) : "";
    }

    /** Retorna l'amplada visible del text. */
    static int displayWidth(String text) {
        return text == null ? 0 : text.length();
    }

    /** Indica si el text té contingut. */
    public static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /** Indica si el text conté algun valor. */
    public static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /** Limita un valor entre mínim i màxim. */
    public static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.clamp(value, min, max);
    }

    /** Calcula l'amplada útil del panell de detall. */
    static int detailContentWidth(int width) {
        return Math.max(DETAIL_CONTENT_MIN_WIDTH, width - DETAIL_HORIZONTAL_PADDING * 2);
    }

    /** Troba un punt de tall adequat. */
    private static int findCut(String text, int width) {
        int safeWidth = clamp(text.length(), 1, width);
        int cut = text.lastIndexOf(' ', safeWidth);

        if (cut <= 0) {
            cut = safeWidth;
        }

        return cut;
    }

    /** Retorna l'espai lateral del detall. */
    private static String detailPadding() {
        return " ".repeat(DETAIL_HORIZONTAL_PADDING);
    }

    /** Afegeix valors acolorits dins un text. */
    private static void appendColoredValue(StringBuilder out, String text, String valueStyle) {
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (startsWith(text, i, LOCKED_VALUE)) {
                out.append(MUTED).append(LOCKED_VALUE).append(RESET);
                i += LOCKED_VALUE.length();
                continue;
            }

            if (isNumberStart(text, i)) {
                int end = i + 1;
                while (end < text.length() && isNumberPart(text.charAt(end))) {
                    end++;
                }

                out.append(valueStyle).append(BOLD).append(text, i, end).append(RESET);
                i = end;
                continue;
            }

            out.append(c);
            i++;
        }
    }

    /** Tria l'estil segons l'etiqueta. */
    private static String styleForLabel(String label) {
        String lower = lower(label);

        if (containsAny(lower, "mana", "mp", "màgia", "magia", "cost")) {
            return BLUE;
        }

        if (containsAny(lower, "crític", "critic", "crítico", "critico", "crítica", "critica", "%")) {
            return YELLOW;
        }

        if (containsAny(lower, "dany", "daño", "dano", "damage", "atac", "ataque", "força", "fuerza")) {
            return RED;
        }

        if (containsAny(lower, "vida", "salut", "health", "hp", "curació", "curacion", "cura")) {
            return GREEN;
        }

        if (containsAny(lower, "defensa", "armadura", "resistència", "resistencia", "resist")) {
            return CYAN;
        }

        if (containsAny(lower, "temps", "torn", "turn", "durada", "cooldown", "recàrrega", "recarga")) {
            return MAGENTA;
        }

        return WHITE;
    }

    /** Indica si un camp del detall representa la dificultat d'un terreny. */
    private static boolean isDifficultyLabel(String label) {
        return lower(label).contains("dificultat");
    }

    /** Acoloreix les estrelles de dificultat amb la mateixa escala que el selector. */
    private static void appendDifficultyStars(StringBuilder out, String value) {
        String safe = value == null ? "" : value;
        long stars = safe.chars().filter(ch -> ch == '★').count();
        String filledColor = switch ((int) Math.clamp(stars, 0, 5)) {
            case 0 -> DARK_GRAY;
            case 1 -> GREEN;
            case 2 -> CYAN;
            case 3 -> YELLOW;
            case 4 -> ORANGE;
            default -> RED;
        };

        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            if (ch == '★') {
                out.append(filledColor).append(ch).append(RESET);
            } else if (ch == '☆') {
                out.append(DARK_GRAY).append(ch).append(RESET);
            } else {
                out.append(ch);
            }
        }
    }

    /** Indica si comença un número. */
    private static boolean isNumberStart(String text, int index) {
        char c = text.charAt(index);

        if (Character.isDigit(c)) {
            return true;
        }

        if ((c == '+' || c == '-') && index + 1 < text.length()) {
            return Character.isDigit(text.charAt(index + 1));
        }

        return false;
    }

    /** Indica si un caràcter forma part d'un número. */
    private static boolean isNumberPart(char c) {
        return Character.isDigit(c)
                || c == '.'
                || c == ','
                || c == '%'
                || c == '+'
                || c == '-'
                || c == 'x'
                || c == 'X';
    }

    /** Indica si el text conté un prefix en una posició. */
    private static boolean startsWith(String text, int index, String prefix) {
        if (index + prefix.length() > text.length()) {
            return false;
        }

        for (int i = 0; i < prefix.length(); i++) {
            if (text.charAt(index + i) != prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    /** Retorna la posició del separador principal. */
    public static int indexOfSeparator(String text) {
        int hyphen = text.indexOf('-');
        int arrow = text.indexOf('→');

        if (hyphen < 0) {
            return arrow;
        }
        if (arrow < 0) {
            return hyphen;
        }

        return Math.min(hyphen, arrow);
    }

    /** Compta les paraules d'un text. */
    public static int countWords(String text) {
        int count = 0;
        boolean inWord = false;

        for (int i = 0; i < text.length(); i++) {
            boolean wordChar = !Character.isWhitespace(text.charAt(i));

            if (wordChar && !inWord) {
                count++;
            }

            inWord = wordChar;
        }

        return count;
    }

    /** Indica si acaba amb signe de frase. */
    private static boolean endsWithSentenceMark(String text) {
        if (text.isEmpty()) {
            return false;
        }

        char c = text.charAt(text.length() - 1);
        return c == '.' || c == '!' || c == '?';
    }
}
