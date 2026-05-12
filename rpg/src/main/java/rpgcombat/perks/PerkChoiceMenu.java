package rpgcombat.perks;

import static rpgcombat.utils.ui.Ansi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.characters.Character;
import rpgcombat.perks.synergy.SynergyPreview;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalInput;
import rpgcombat.utils.terminal.TerminalSession;

/** Menú visual per triar una perk de recompensa. */
public final class PerkChoiceMenu {
    private static final int MIN_CARD_WIDTH = 24;
    private static final int MAX_CARD_WIDTH = 34;
    private static final int CARD_HEIGHT = 15;
    private static final int CARD_GAP = 3;
    private static final int TITLE_ROW = 2;
    private static final int CARDS_ROW = 5;

    private PerkChoiceMenu() {}

    public static PerkDefinition choose(Character player, List<PerkDefinition> options) {
        return choose(player, options, Map.of());
    }

    public static PerkDefinition choose(Character player, List<PerkDefinition> options,
                                        Map<String, SynergyPreview> previews) {
        String playerName = player == null ? "Jugador" : player.getName();
        return choose(playerName, options, previews);
    }

    public static PerkDefinition choose(String playerName, List<PerkDefinition> options) {
        return choose(playerName, options, Map.of());
    }

    public static PerkDefinition choose(String playerName, List<PerkDefinition> options,
                                        Map<String, SynergyPreview> previews) {
        List<PerkDefinition> ordered = ordered(options);
        if (ordered.isEmpty()) return null;

        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();

            try {
                BindingReader reader = new BindingReader(terminal.reader());
                KeyMap<Action> keyMap = buildKeyMap(terminal);
                int cursor = 0;

                renderFull(terminal, playerName, ordered, previews, cursor);

                while (true) {
                    Action action = TerminalInput.readBindingIgnoringMouse(reader, keyMap, terminal, Action.IGNORE);
                    if (action == null) {
                        renderFull(terminal, playerName, ordered, previews, cursor);
                        continue;
                    }

                    int oldCursor = cursor;

                    switch (action) {
                        case PREVIOUS -> cursor = Math.max(0, cursor - 1);
                        case NEXT -> cursor = Math.min(ordered.size() - 1, cursor + 1);
                        case FIRST -> cursor = 0;
                        case SECOND -> cursor = Math.min(1, ordered.size() - 1);
                        case THIRD -> cursor = Math.min(2, ordered.size() - 1);
                        case SELECT -> {
                            clearScreen(terminal);
                            terminal.flush();
                            return ordered.get(cursor);
                        }
                        case IGNORE -> {
                        }
                    }

                    if (oldCursor != cursor) {
                        renderSelectionChange(terminal, ordered, previews, oldCursor, cursor);
                    }
                }
            } finally {
                terminal.flush();
            }
        } catch (IOException e) {
            System.out.println("No s'ha pogut obrir el menú de perks: " + e.getMessage());
            return ordered.get(0);
        }
    }

    private enum Action { PREVIOUS, NEXT, FIRST, SECOND, THIRD, SELECT, IGNORE }

    private static KeyMap<Action> buildKeyMap(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();
        map.bind(Action.PREVIOUS, "a", "A", "w", "W");
        map.bind(Action.NEXT, "d", "D", "s", "S");
        map.bind(Action.FIRST, "1");
        map.bind(Action.SECOND, "2");
        map.bind(Action.THIRD, "3");
        map.bind(Action.SELECT, "\r", "\n");

        String left = KeyMap.key(terminal, Capability.key_left);
        String right = KeyMap.key(terminal, Capability.key_right);
        String up = KeyMap.key(terminal, Capability.key_up);
        String down = KeyMap.key(terminal, Capability.key_down);

        if (left != null) map.bind(Action.PREVIOUS, left);
        if (up != null) map.bind(Action.PREVIOUS, up);
        if (right != null) map.bind(Action.NEXT, right);
        if (down != null) map.bind(Action.NEXT, down);
        TerminalInput.bindMouseIgnore(map, terminal, Action.IGNORE);

        return map;
    }

    private static List<PerkDefinition> ordered(List<PerkDefinition> options) {
        if (options == null) return List.of();
        return options.stream()
                .filter(p -> p != null)
                .sorted(Comparator.comparingInt(p -> p.family().ordinal()))
                .toList();
    }

    private static void renderFull(Terminal terminal, String playerName,
                                   List<PerkDefinition> options,
                                   Map<String, SynergyPreview> previews,
                                   int cursor) {
        hideCursor(terminal);
        clearScreen(terminal);

        int terminalWidth = Math.max(terminal.getWidth(), 80);
        int cardWidth = cardWidth(terminalWidth, options.size());
        int totalWidth = options.size() * cardWidth + Math.max(0, options.size() - 1) * CARD_GAP;
        int startCol = Math.max(2, (terminalWidth - totalWidth) / 2 + 1);

        printCentered(terminal, TITLE_ROW, terminalWidth,
                BOLD + MAGENTA + "╔═ Recompensa de missió ═╗" + RESET);
        printCentered(terminal, TITLE_ROW + 1, terminalWidth,
                DARK_GRAY + safe(playerName) + ", tria una millora permanent" + RESET);

        for (int i = 0; i < options.size(); i++) {
            int col = startCol + i * (cardWidth + CARD_GAP);
            PerkDefinition perk = options.get(i);
            drawCard(terminal, CARDS_ROW, col, cardWidth, perk, previewFor(previews, perk), i, cursor == i);
        }

        printCentered(terminal, CARDS_ROW + CARD_HEIGHT + 2, terminalWidth,
                DARK_GRAY + "[←/→ o A/D] moure   [1/2/3] triar carta   [Enter] seleccionar" + RESET);
        terminal.flush();
    }

    private static void renderSelectionChange(Terminal terminal, List<PerkDefinition> options,
                                              Map<String, SynergyPreview> previews,
                                              int oldCursor, int newCursor) {
        int terminalWidth = Math.max(terminal.getWidth(), 80);
        int cardWidth = cardWidth(terminalWidth, options.size());
        int totalWidth = options.size() * cardWidth + Math.max(0, options.size() - 1) * CARD_GAP;
        int startCol = Math.max(2, (terminalWidth - totalWidth) / 2 + 1);

        drawCardAtIndex(terminal, options, previews, oldCursor, cardWidth, startCol, false);
        drawCardAtIndex(terminal, options, previews, newCursor, cardWidth, startCol, true);
        terminal.flush();
    }

    private static void drawCardAtIndex(Terminal terminal, List<PerkDefinition> options,
                                        Map<String, SynergyPreview> previews,
                                        int index, int cardWidth, int startCol,
                                        boolean selected) {
        if (index < 0 || index >= options.size()) return;
        int col = startCol + index * (cardWidth + CARD_GAP);
        PerkDefinition perk = options.get(index);
        drawCard(terminal, CARDS_ROW, col, cardWidth, perk, previewFor(previews, perk), index, selected);
    }

    private static int cardWidth(int terminalWidth, int count) {
        if (count <= 0) return MAX_CARD_WIDTH;
        int available = terminalWidth - 4 - Math.max(0, count - 1) * CARD_GAP;
        int width = available / count;
        return Math.clamp(width, MIN_CARD_WIDTH, MAX_CARD_WIDTH);
    }

    private static void drawCard(Terminal terminal, int row, int col, int width,
                                 PerkDefinition perk, SynergyPreview preview,
                                 int index, boolean selected) {
        String color = color(perk.family());
        String accent = selected ? BOLD + color : color;
        String marker = selected ? "◆" : "◇";

        String top = selected ? "╔" + "═".repeat(width - 2) + "╗" : "┌" + "─".repeat(width - 2) + "┐";
        String sep = selected ? "╠" + "═".repeat(width - 2) + "╣" : "├" + "─".repeat(width - 2) + "┤";
        String bottom = selected ? "╚" + "═".repeat(width - 2) + "╝" : "└" + "─".repeat(width - 2) + "┘";

        String sideLeft = selected ? "║" : "│";
        String sideRight = selected ? "║" : "│";
        int inner = width - 4;

        List<String> description = wrap(perk.description(), inner);
        while (description.size() < 4) description.add("");

        List<String> synergyLines = synergyLines(preview, inner);
        while (synergyLines.size() < 2) synergyLines.add("");

        printAt(terminal, row, col, accent + top + RESET);
        printAt(terminal, row + 1, col, line(accent, sideLeft, center(marker + " " + (index + 1), inner), sideRight));
        printAt(terminal, row + 2, col,
                line(accent, sideLeft, BOLD + color + center(familyLabel(perk.family()).toUpperCase(), inner) + RESET, sideRight));
        printAt(terminal, row + 3, col, accent + sep + RESET);
        printAt(terminal, row + 4, col, line(accent, sideLeft, BOLD + center(trim(perk.name(), inner), inner) + RESET, sideRight));
        printAt(terminal, row + 5, col, line(accent, sideLeft, " ".repeat(inner), sideRight));

        for (int i = 0; i < 4; i++) {
            printAt(terminal, row + 6 + i, col, line(accent, sideLeft, pad(description.get(i), inner), sideRight));
        }

        printAt(terminal, row + 10, col, line(accent, sideLeft, pad(synergyLines.get(0), inner), sideRight));
        printAt(terminal, row + 11, col, line(accent, sideLeft, pad(synergyLines.get(1), inner), sideRight));
        printAt(terminal, row + 12, col, line(accent, sideLeft, " ".repeat(inner), sideRight));
        printAt(terminal, row + 13, col,
                line(accent, sideLeft, DARK_GRAY + center(shortTrigger(perk), inner) + RESET, sideRight));
        printAt(terminal, row + 14, col, accent + bottom + RESET);
    }

    private static SynergyPreview previewFor(Map<String, SynergyPreview> previews, PerkDefinition perk) {
        if (previews == null || perk == null) return SynergyPreview.empty();
        return previews.getOrDefault(perk.id(), SynergyPreview.empty());
    }

    private static final String SYNERGY_TAG = "✦";

    private static String synergyTagLevel(int level) {
        if (level <= 0) throw new IllegalArgumentException("El nivell de sinergia no pot ser menor a 1.");

        return SYNERGY_TAG.repeat(level) + " ";
    }

    private static List<String> synergyLines(SynergyPreview preview, int width) {
        if (preview == null || !preview.hasAny()) return new ArrayList<>(List.of("", ""));
        List<String> lines = new ArrayList<>();

        if (!preview.activatedSynergyNames().isEmpty()) {
            String tag = synergyTagLevel(1);
            lines.add(GREEN + trim(tag + String.join(", ", preview.activatedSynergyNames()), width) + RESET);
        }
        if (!preview.upgradedSynergyNames().isEmpty()) {
            String tag = synergyTagLevel(2);
            lines.add(CYAN + trim(tag + String.join(", ", preview.upgradedSynergyNames()), width) + RESET);
        }
        return new ArrayList<>(lines.stream().limit(2).toList());
    }

    private static String line(String color, String left, String content, String right) {
        return color + left + RESET + " " + content + " " + color + right + RESET;
    }

    private static String shortTrigger(PerkDefinition perk) {
        return switch (perk.trigger()) {
            case START_TURN -> "Inici de torn";
            case BEFORE_ATTACK -> "Abans d'atacar";
            case ROLL_CRIT -> "Crític";
            case MODIFY_DAMAGE -> "Dany";
            case BEFORE_DEFENSE -> "Abans defensa";
            case AFTER_HIT -> "En impactar";
            case END_TURN -> "Final de torn";
            default -> "Altre";
        };
    }

    private static String familyLabel(PerkFamily family) {
        return switch (family) {
            case STRATEGY -> "Estratègia";
            case LUCK -> "Sort";
            case CHAOS -> "Caos";
            case CORRUPTED -> "Corrupte";
            default -> "Error";
        };
    }

    private static String color(PerkFamily family) {
        return switch (family) {
            case STRATEGY -> BRIGHT_BLUE;
            case LUCK -> YELLOW;
            case CHAOS -> BRIGHT_RED;
            case CORRUPTED -> MAGENTA;
            default -> "";
        };
    }

    private static List<String> wrap(String text, int width) {
        if (text == null || text.isBlank()) return new ArrayList<>(List.of(""));
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return new ArrayList<>(lines.stream().limit(4).toList());
    }

    private static String center(String text, int width) {
        String safe = trim(text, width);
        int left = Math.max(0, (width - visibleLength(safe)) / 2);
        int right = Math.max(0, width - visibleLength(safe) - left);
        return spaces(left) + safe + spaces(right);
    }

    private static String pad(String text, int width) {
        String safe = trim(text, width);
        return safe + spaces(Math.max(0, width - visibleLength(safe)));
    }

    private static String trim(String text, int width) {
        String safe = safe(text);
        if (visibleLength(safe) <= width) return safe;
        if (width <= 0) return "";
        String plain = safe.replaceAll("\\u001B\\[[;\\d]*m", "");
        return plain.substring(0, Math.max(0, width - 1)) + "…";
    }

    private static String spaces(int count) { return " ".repeat(Math.max(0, count)); }
    private static String safe(String text) { return text == null ? "" : text; }

    private static void printCentered(Terminal terminal, int row, int terminalWidth, String text) {
        int visible = visibleLength(text);
        int col = Math.max(1, (terminalWidth - visible) / 2 + 1);
        printAt(terminal, row, col, text);
    }

    private static void printAt(Terminal terminal, int row, int col, String text) {
        move(terminal, row, col);
        terminal.writer().print(text);
    }

    private static int visibleLength(String text) {
        return safe(text).replaceAll("\\u001B\\[[;\\d]*m", "").length();
    }

    private static void clearScreen(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }
        move(terminal, 1, 1);
    }

    private static void move(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    private static void hideCursor(Terminal terminal) { terminal.writer().print("\033[?25l"); }
}
