package rpgcombat.perks;

import static rpgcombat.utils.ui.Ansi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.characters.Character;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;

/**
 * Menú visual per triar una perk de recompensa.
 */
public final class PerkChoiceMenu {
    private static final int MIN_CARD_WIDTH = 24;
    private static final int MAX_CARD_WIDTH = 34;
    private static final int CARD_HEIGHT = 13;
    private static final int CARD_GAP = 3;
    private static final int TITLE_ROW = 2;
    private static final int CARDS_ROW = 5;

    private PerkChoiceMenu() {}

    /** Mostra el menú de recompensa per al personatge. */
    public static PerkDefinition choose(Character player, List<PerkDefinition> options) {
        String playerName = player == null ? "Jugador" : player.getName();
        return choose(playerName, options);
    }

    /** Mostra el menú de recompensa amb un nom de jugador explícit. */
    public static PerkDefinition choose(String playerName, List<PerkDefinition> options) {
        List<PerkDefinition> ordered = ordered(options);
        if (ordered.isEmpty()) return null;

        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();

            try {
                BindingReader reader = new BindingReader(terminal.reader());
                KeyMap<Action> keyMap = buildKeyMap(terminal);
                int cursor = 0;

                renderFull(terminal, playerName, ordered, cursor);

                while (true) {
                    Action action = reader.readBinding(keyMap);
                    if (action == null) continue;

                    int oldCursor = cursor;

                    switch (action) {
                        case PREVIOUS -> cursor = Math.max(0, cursor - 1);
                        case NEXT -> cursor = Math.min(ordered.size() - 1, cursor + 1);
                        case FIRST -> cursor = 0;
                        case SECOND -> cursor = Math.min(1, ordered.size() - 1);
                        case THIRD -> cursor = Math.min(2, ordered.size() - 1);
                        case SELECT -> {
                            showCursor(terminal);
                            clearScreen(terminal);
                            terminal.flush();
                            return ordered.get(cursor);
                        }
                    }

                    if (oldCursor != cursor) {
                        renderSelectionChange(terminal, ordered, oldCursor, cursor);
                    }
                }
            } finally {
                showCursor(terminal);
                terminal.flush();
            }
        } catch (IOException e) {
            System.out.println("No s'ha pogut obrir el menú de perks: " + e.getMessage());
            return ordered.get(0);
        }
    }

    /** Accions possibles dins del menú. */
    private enum Action { PREVIOUS, NEXT, FIRST, SECOND, THIRD, SELECT }

    /** Crea el mapa de tecles del menú. */
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

        return map;
    }

    /** Ordena les perks per família. */
    private static List<PerkDefinition> ordered(List<PerkDefinition> options) {
        if (options == null) return List.of();

        return options.stream()
                .filter(p -> p != null)
                .sorted(Comparator.comparingInt(p -> p.family().ordinal()))
                .toList();
    }

    /** Dibuixa tot el menú. */
    private static void renderFull(Terminal terminal, String playerName,
                                   List<PerkDefinition> options, int cursor) {
        hideCursor(terminal);
        clearScreen(terminal);

        int terminalWidth = Math.max(terminal.getWidth(), 80);
        int cardWidth = cardWidth(terminalWidth, options.size());
        int totalWidth = options.size() * cardWidth
                + Math.max(0, options.size() - 1) * CARD_GAP;
        int startCol = Math.max(2, (terminalWidth - totalWidth) / 2 + 1);

        printCentered(terminal, TITLE_ROW, terminalWidth,
                BOLD + MAGENTA + "╔═ Recompensa de missió ═╗" + RESET);
        printCentered(terminal, TITLE_ROW + 1, terminalWidth,
                DARK_GRAY + safe(playerName) + ", tria una millora permanent" + RESET);

        for (int i = 0; i < options.size(); i++) {
            int col = startCol + i * (cardWidth + CARD_GAP);
            drawCard(terminal, CARDS_ROW, col, cardWidth, options.get(i), i, cursor == i);
        }

        printCentered(terminal, CARDS_ROW + CARD_HEIGHT + 2, terminalWidth,
                DARK_GRAY + "[←/→ o A/D] moure   [1/2/3] triar carta   [Enter] seleccionar" + RESET);

        terminal.flush();
    }

    /** Redibuixa només les cartes afectades pel canvi de selecció. */
    private static void renderSelectionChange(Terminal terminal, List<PerkDefinition> options,
                                              int oldCursor, int newCursor) {
        int terminalWidth = Math.max(terminal.getWidth(), 80);
        int cardWidth = cardWidth(terminalWidth, options.size());
        int totalWidth = options.size() * cardWidth
                + Math.max(0, options.size() - 1) * CARD_GAP;
        int startCol = Math.max(2, (terminalWidth - totalWidth) / 2 + 1);

        drawCardAtIndex(terminal, options, oldCursor, cardWidth, startCol, false);
        drawCardAtIndex(terminal, options, newCursor, cardWidth, startCol, true);

        terminal.flush();
    }

    /** Dibuixa una carta segons el seu índex. */
    private static void drawCardAtIndex(Terminal terminal, List<PerkDefinition> options,
                                        int index, int cardWidth, int startCol,
                                        boolean selected) {
        if (index < 0 || index >= options.size()) return;

        int col = startCol + index * (cardWidth + CARD_GAP);
        drawCard(terminal, CARDS_ROW, col, cardWidth, options.get(index), index, selected);
    }

    /** Calcula l'amplada de cada carta segons el terminal. */
    private static int cardWidth(int terminalWidth, int count) {
        if (count <= 0) return MAX_CARD_WIDTH;

        int available = terminalWidth - 4 - Math.max(0, count - 1) * CARD_GAP;
        int width = available / count;

        return Math.clamp(width, MIN_CARD_WIDTH, MAX_CARD_WIDTH);
    }

    /** Dibuixa una carta de perk. */
    private static void drawCard(Terminal terminal, int row, int col, int width,
                                 PerkDefinition perk, int index, boolean selected) {
        String color = color(perk.family());
        String accent = selected ? BOLD + color : color;
        String marker = selected ? "◆" : "◇";

        String top = selected
                ? "╔" + "═".repeat(width - 2) + "╗"
                : "┌" + "─".repeat(width - 2) + "┐";
        String sep = selected
                ? "╠" + "═".repeat(width - 2) + "╣"
                : "├" + "─".repeat(width - 2) + "┤";
        String bottom = selected
                ? "╚" + "═".repeat(width - 2) + "╝"
                : "└" + "─".repeat(width - 2) + "┘";

        String sideLeft = selected ? "║" : "│";
        String sideRight = selected ? "║" : "│";
        int inner = width - 4;

        List<String> description = wrap(perk.description(), inner);
        while (description.size() < 4) description.add("");

        printAt(terminal, row, col, accent + top + RESET);
        printAt(terminal, row + 1, col,
                line(accent, sideLeft, center(marker + " " + (index + 1), inner), sideRight));
        printAt(terminal, row + 2, col,
                line(accent, sideLeft,
                        BOLD + color + center(familyLabel(perk.family()).toUpperCase(), inner) + RESET,
                        sideRight));
        printAt(terminal, row + 3, col, accent + sep + RESET);
        printAt(terminal, row + 4, col,
                line(accent, sideLeft,
                        BOLD + center(trim(perk.name(), inner), inner) + RESET,
                        sideRight));
        printAt(terminal, row + 5, col,
                line(accent, sideLeft, " ".repeat(inner), sideRight));

        for (int i = 0; i < 4; i++) {
            printAt(terminal, row + 6 + i, col,
                    line(accent, sideLeft, pad(description.get(i), inner), sideRight));
        }

        printAt(terminal, row + 10, col,
                line(accent, sideLeft, " ".repeat(inner), sideRight));
        printAt(terminal, row + 11, col,
                line(accent, sideLeft,
                        DARK_GRAY + center(shortTrigger(perk), inner) + RESET,
                        sideRight));
        printAt(terminal, row + 12, col, accent + bottom + RESET);
    }

    /** Construeix una línia amb vores i contingut. */
    private static String line(String color, String left, String content, String right) {
        return color + left + RESET + " " + content + " " + color + right + RESET;
    }

    /** Retorna una etiqueta curta del trigger. */
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

    /** Retorna el nom visible de la família. */
    private static String familyLabel(PerkFamily family) {
        return switch (family) {
            case STRATEGY -> "Estratègia";
            case LUCK -> "Sort";
            case CHAOS -> "Caos";
            case CORRUPTED -> "Corrupte";
        };
    }

    /** Retorna el color ANSI de la família. */
    private static String color(PerkFamily family) {
        return switch (family) {
            case STRATEGY -> BRIGHT_BLUE;
            case LUCK -> YELLOW;
            case CHAOS -> BRIGHT_RED;
            case CORRUPTED -> MAGENTA;
        };
    }

    /** Parteix un text en línies curtes. */
    private static List<String> wrap(String text, int width) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>(List.of(""));
        }

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

    /** Centra un text dins d'una amplada. */
    private static String center(String text, int width) {
        String safe = trim(text, width);
        int left = Math.max(0, (width - safe.length()) / 2);
        int right = Math.max(0, width - safe.length() - left);

        return spaces(left) + safe + spaces(right);
    }

    /** Omple un text fins a una amplada. */
    private static String pad(String text, int width) {
        String safe = trim(text, width);
        return safe + spaces(Math.max(0, width - safe.length()));
    }

    /** Retalla un text si supera l'amplada. */
    private static String trim(String text, int width) {
        String safe = safe(text);

        if (safe.length() <= width) return safe;
        if (width <= 0) return "";

        return safe.substring(0, Math.max(0, width - 1)) + "…";
    }

    /** Retorna espais segurs. */
    private static String spaces(int count) {
        return " ".repeat(Math.max(0, count));
    }

    /** Evita valors null en textos. */
    private static String safe(String text) {
        return text == null ? "" : text;
    }

    /** Escriu text centrat en una fila. */
    private static void printCentered(Terminal terminal, int row, int terminalWidth, String text) {
        int visible = visibleLength(text);
        int col = Math.max(1, (terminalWidth - visible) / 2 + 1);

        printAt(terminal, row, col, text);
    }

    /** Escriu text en una posició concreta. */
    private static void printAt(Terminal terminal, int row, int col, String text) {
        move(terminal, row, col);
        terminal.writer().print(text);
    }

    /** Calcula la longitud visible ignorant codis ANSI. */
    private static int visibleLength(String text) {
        return text.replaceAll("\\u001B\\[[;\\d]*m", "").length();
    }

    /** Neteja la pantalla del terminal. */
    private static void clearScreen(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }

        move(terminal, 1, 1);
    }

    /** Mou el cursor a una posició. */
    private static void move(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /** Amaga el cursor del terminal. */
    private static void hideCursor(Terminal terminal) {
        terminal.writer().print("\033[?25l");
    }

    /** Mostra el cursor del terminal. */
    private static void showCursor(Terminal terminal) {
        terminal.writer().print("\033[?25h");
    }
}