package rpgcombat.utils.cinematic.typing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import rpgcombat.utils.cinematic.markup.MarkupParser;
import rpgcombat.utils.cinematic.markup.MarkupToken;
import rpgcombat.utils.cinematic.scene.TextBlock;
import rpgcombat.utils.cinematic.style.CinematicColor;

/**
 * Analitza text i tags per generar accions d'escriptura.
 */
public class TypingAnalyzer {
    private final Random random = new Random();

    /**
     * Analitza un bloc i retorna les accions resultants.
     */
    public List<TypingAction> analyze(TextBlock block) {
        List<TypingAction> actions = new ArrayList<>();
        List<MarkupToken> tokens = MarkupParser.parse(block.text());

        TypingStyle style = new TypingStyle(block.color(), block.mood());
        Deque<TypingStyle> stack = new ArrayDeque<>();

        actions.add(TypingAction.color(style.color()));

        for (MarkupToken token : tokens) {
            switch (token.kind()) {
                case TEXT -> analyzeText(token.text(), block, style, actions);
                case OPEN -> {
                    stack.push(style.copy());
                    applyRangeTag(token.name(), style);
                    actions.add(TypingAction.color(style.color()));
                }
                case CLOSE -> {
                    if (!stack.isEmpty()) {
                        style = stack.pop();
                        actions.add(TypingAction.color(style.color()));
                    }
                }
                case INSTANT -> applyInstantTag(token, style, actions);
            }
        }

        actions.add(TypingAction.color(CinematicColor.RESET));
        return actions;
    }

    /**
     * Analitza text pla caràcter a caràcter.
     */
    private void analyzeText(String text, TextBlock block, TypingStyle style, List<TypingAction> actions) {
        for (int i = 0; i < text.length(); i++) {
            TypingStyle effective = style.copy();

            if (block.smartTyping()) {
                applySmartAdjustments(text, i, effective);
            }

            long delay = calculateDelay(text, i, block, effective);
            actions.add(TypingAction.print(text.charAt(i), delay));
        }
    }

    /**
     * Aplica un tag de rang.
     */
    private void applyRangeTag(String name, TypingStyle style) {
        TypingMood mood = TypingMood.fromTag(name);
        if (mood != null) {
            style.applyMood(mood);
            return;
        }

        CinematicColor color = CinematicColor.fromTag(name);
        if (color != null) {
            style.color(color);
        }
    }

    /**
     * Aplica un tag instantani.
     */
    private void applyInstantTag(MarkupToken token, TypingStyle style, List<TypingAction> actions) {
        String name = token.name();
        String value = token.value();

        switch (name) {
            case "pause" -> actions.add(TypingAction.pause(parseLong(value, 250)));
            case "br" -> actions.add(TypingAction.newLine());
            case "reset" -> {
                style.color(CinematicColor.WHITE);
                actions.add(TypingAction.color(style.color()));
            }
            case "speed" -> applySpeed(value, style);
            case "mood" -> {
                TypingMood mood = TypingMood.fromTag(value);
                if (mood != null) style.applyMood(mood);
            }
            case "color" -> {
                CinematicColor color = CinematicColor.fromTag(value);
                if (color != null) {
                    style.color(color);
                    actions.add(TypingAction.color(color));
                }
            }
            default -> {
                // Tag ignorat.
            }
        }
    }

    /**
     * Aplica un modificador de velocitat.
     */
    private void applySpeed(String value, TypingStyle style) {
        if (value == null) return;

        switch (value.toLowerCase()) {
            case "fast" -> style.speedMultiplier(0.70);
            case "slow" -> style.speedMultiplier(1.40);
            case "normal" -> style.speedMultiplier(1.0);
            default -> {
                try {
                    style.speedMultiplier(Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                    // Valor invàlid ignorat.
                }
            }
        }
    }

    /**
     * Ajusta el ritme segons el context de la frase.
     */
    private void applySmartAdjustments(String text, int index, TypingStyle style) {
        String sentence = currentSentence(text, index);

        if (sentence.contains("...")) {
            style.pauseMultiplier(1.25);
            style.variationMultiplier(1.15);
        }

        if (sentence.contains("¿") || sentence.contains("?")) {
            style.pauseMultiplier(1.15);
            style.variationMultiplier(1.20);
        }

        if (sentence.contains("¡") || sentence.contains("!")) {
            style.speedMultiplier(0.82);
            style.pauseMultiplier(0.85);
        }

        if (isMostlyUppercase(sentence)) {
            style.speedMultiplier(0.75);
            style.pauseMultiplier(0.90);
        }

        if (sentence.length() <= 14 && sentence.endsWith(".")) {
            style.pauseMultiplier(1.20);
        }
    }

    /**
     * Calcula el retard d'un caràcter.
     */
    private long calculateDelay(String text, int index, TextBlock block, TypingStyle style) {
        char c = text.charAt(index);

        double variation = block.randomVariationMillis() * style.variationMultiplier();
        long delay = block.baseDelayMillis() + random.nextInt((int) Math.max(1, variation + 1));
        delay = Math.round(delay * style.speedMultiplier());

        if (c == ' ') {
            delay = Math.round(delay * 0.45);
            if (isAfterLongWord(text, index)) {
                delay += Math.round(22 * style.pauseMultiplier());
            }
        }

        if (isInsideEllipsis(text, index)) {
            delay += Math.round(block.ellipsisFlowPauseMillis() * style.pauseMultiplier());
            return Math.max(5, delay);
        }

        if (isEndOfEllipsis(text, index)) {
            delay += Math.round(block.ellipsisEndPauseMillis() * style.pauseMultiplier());
            return Math.max(5, delay);
        }

        if (c == ',' || c == ';' || c == ':') {
            delay += Math.round(block.shortPauseMillis() * style.pauseMultiplier());
        } else if (c == '.') {
            delay += Math.round(block.mediumPauseMillis() * style.pauseMultiplier());
        } else if (c == '!' || c == '?' || c == '\n') {
            delay += Math.round(block.longPauseMillis() * style.pauseMultiplier());
        }

        if (isBeforeReveal(text, index)) {
            delay += Math.round(90 * style.pauseMultiplier());
        }

        return Math.max(5, delay);
    }

    /**
     * Indica si el punt forma part inicial d'uns punts suspensius.
     */
    private boolean isInsideEllipsis(String text, int index) {
        return text.charAt(index) == '.'
                && index + 1 < text.length()
                && text.charAt(index + 1) == '.';
    }

    /**
     * Indica si el punt tanca uns punts suspensius.
     */
    private boolean isEndOfEllipsis(String text, int index) {
        return text.charAt(index) == '.'
                && index >= 2
                && text.charAt(index - 1) == '.'
                && text.charAt(index - 2) == '.';
    }

    /**
     * Detecta una pausa natural després d'una paraula llarga.
     */
    private boolean isAfterLongWord(String text, int index) {
        if (index <= 0 || text.charAt(index) != ' ') return false;

        int length = 0;
        for (int i = index - 1; i >= 0 && Character.isLetter(text.charAt(i)); i--) {
            length++;
        }

        return length >= 8;
    }

    /**
     * Detecta paraules que solen anticipar revelació.
     */
    private boolean isBeforeReveal(String text, int index) {
        if (text.charAt(index) != ' ') return false;

        String remaining = text.substring(index).trim().toLowerCase();
        return remaining.startsWith("pero ")
                || remaining.startsWith("entonces ")
                || remaining.startsWith("sin embargo ")
                || remaining.startsWith("de pronto ");
    }

    /**
     * Retorna la frase que conté l'índex indicat.
     */
    private String currentSentence(String text, int index) {
        int start = 0;
        int end = text.length();

        for (int i = index; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                start = Math.min(i + 1, text.length());
                break;
            }
        }

        for (int i = index; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                end = Math.min(i + 1, text.length());
                break;
            }
        }

        return text.substring(start, end).trim();
    }

    /**
     * Indica si la frase està majoritàriament en majúscules.
     */
    private boolean isMostlyUppercase(String sentence) {
        int letters = 0;
        int uppercase = 0;

        for (char c : sentence.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) uppercase++;
            }
        }

        return letters >= 4 && uppercase >= letters * 0.75;
    }

    /**
     * Converteix text a long amb valor de reserva.
     */
    private long parseLong(String value, long fallback) {
        if (value == null) return fallback;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}