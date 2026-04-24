package rpgcombat.utils.cinematic.markup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Analitza text amb tags i el converteix en tokens.
 */
public class MarkupParser {
    private static final Set<String> RANGE_TAGS = Set.of(
            "slow", "fast", "urgent", "tense", "dramatic", "whisper", "normal",
            "black", "red", "green", "yellow", "blue", "purple", "cyan", "white",
            "gray", "grey", "bright_red", "bright_green", "bright_yellow",
            "bright_blue", "bright_purple", "bright_cyan", "bright_white");

    private static final Set<String> INSTANT_TAGS = Set.of(
            "pause", "br", "reset", "speed", "mood", "color");

    /**
     * Constructor privat per evitar instanciació.
     */
    private MarkupParser() {
    }

    /**
     * Analitza un text amb tags i retorna els tokens resultants.
     */
    public static List<MarkupToken> parse(String input) {
        List<MarkupToken> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            char current = input.charAt(i);

            if (current != '<') {
                buffer.append(current);
                i++;
                continue;
            }

            int end = input.indexOf('>', i);
            if (end == -1) {
                buffer.append(current);
                i++;
                continue;
            }

            String raw = input.substring(i + 1, end).trim();
            MarkupToken tag = parseTag(raw);

            if (tag == null) {
                buffer.append(input, i, end + 1);
                i = end + 1;
                continue;
            }

            if (!buffer.isEmpty()) {
                tokens.add(MarkupToken.text(buffer.toString()));
                buffer.setLength(0);
            }

            tokens.add(tag);
            i = end + 1;
        }

        if (!buffer.isEmpty()) {
            tokens.add(MarkupToken.text(buffer.toString()));
        }

        return tokens;
    }

    /**
     * Interpreta un tag individual.
     */
    private static MarkupToken parseTag(String raw) {
        if (raw.isBlank())
            return null;

        if (raw.startsWith("/")) {
            String name = normalize(raw.substring(1));
            return RANGE_TAGS.contains(name) ? MarkupToken.close(name) : null;
        }

        String name = raw;
        String value = null;
        int separator = raw.indexOf(':');

        if (separator >= 0) {
            name = raw.substring(0, separator);
            value = raw.substring(separator + 1).trim();
        }

        name = normalize(name);

        if (value == null && RANGE_TAGS.contains(name)) {
            return MarkupToken.open(name);
        }

        if (INSTANT_TAGS.contains(name)) {
            return MarkupToken.instant(name, value);
        }

        return null;
    }

    /**
     * Normalitza el nom del tag.
     */
    private static String normalize(String value) {
        return value.trim().toLowerCase().replace('-', '_');
    }
}