package rpgcombat.gamemode.ui;

import static rpgcombat.utils.ui.Ansi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.gamemode.chaos.ChaosActivation;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.model.GameModePresentation;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalInput;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.TerminalClear;

/** Menú interactiu de cartes per escollir mode de joc abans de crear la partida. */
public final class GameModeSelectionMenu {
    private static final int CARD_WIDTH = 54;
    private static final int CARD_HEIGHT = 16;
    private static final int GAP = 2;
    private static final int HEADER_LINES = 3;
    private static final int FOOTER_LINES = 3;
    private static final int CONTENT_PADDING = 2;
    private static final int DETAIL_LINES = 8;

    private GameModeSelectionMenu() {
    }

    private enum Action {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        PREVIOUS_PAGE,
        NEXT_PAGE,
        SELECT,
        EXIT,
        IGNORE
    }

    public static GameModeDefinition show(
            List<GameModeDefinition> modes,
            AchievementSystem achievements,
            DiscoverySystem discoveries,
            String defaultModeId) {

        List<GameModeDefinition> available = modes == null || modes.isEmpty()
                ? List.of(GameModeRegistry.getOrDefault(defaultModeId))
                : List.copyOf(modes);

        if (available.size() == 1 && available.get(0).isUnlocked(achievements, discoveries)) {
            return available.get(0);
        }

        try (TerminalSession session = SharedTerminal.openSession()) {
            return showInteractive(available, achievements, discoveries, defaultModeId, session.terminal());
        } catch (IOException e) {
            return fallbackMode(available, achievements, discoveries, defaultModeId);
        }
    }

    private static GameModeDefinition showInteractive(
            List<GameModeDefinition> modes,
            AchievementSystem achievements,
            DiscoverySystem discoveries,
            String defaultModeId,
            Terminal terminal) {

        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Action> keys = keys(terminal);

        terminal.puts(Capability.cursor_invisible);
        TerminalClear.clear(terminal);

        int selected = initialSelection(modes, achievements, discoveries, defaultModeId);
        int page = 0;
        String lastFrame = "";
        int lastWidth = -1;
        int lastHeight = -1;

        try {
            while (true) {
                int width = Math.max(1, terminal.getWidth());
                int height = Math.max(1, terminal.getHeight());
                boolean dimensionsChanged = width != lastWidth || height != lastHeight;
                Grid grid = grid(width, height);

                int pageSize = grid.pageSize();
                int totalPages = totalPages(modes.size(), pageSize);
                page = Math.clamp(selected / pageSize, 0, Math.max(0, totalPages - 1));

                String frame = renderFrame(modes, achievements, discoveries, selected, page, width, height, grid);
                if (dimensionsChanged || !frame.equals(lastFrame)) {
                    paintFrame(terminal, frame, height, dimensionsChanged);
                    terminal.flush();
                    lastFrame = frame;
                    lastWidth = width;
                    lastHeight = height;
                }

                Action action = TerminalInput.readBindingIgnoringMouse(reader, keys, terminal, Action.IGNORE);
                if (action == null) {
                    paintFrame(terminal, frame, height, true);
                    terminal.flush();
                    continue;
                }

                switch (action) {
                    case LEFT -> selected = Math.max(0, selected - 1);
                    case RIGHT -> selected = Math.min(modes.size() - 1, selected + 1);
                    case UP -> selected = Math.max(0, selected - grid.cols());
                    case DOWN -> selected = Math.min(modes.size() - 1, selected + grid.cols());
                    case PREVIOUS_PAGE -> {
                        if (totalPages > 1) {
                            selected = Math.max(0, selected - pageSize);
                        }
                    }
                    case NEXT_PAGE -> {
                        if (totalPages > 1) {
                            selected = Math.min(modes.size() - 1, selected + pageSize);
                        }
                    }
                    case SELECT -> {
                        GameModeDefinition mode = modes.get(selected);
                        if (mode.isUnlocked(achievements, discoveries)) {
                            return mode;
                        }
                    }
                    case EXIT -> {
                        return null;
                    }
                    case IGNORE -> {}
                }
            }
        } finally {
            terminal.writer().print(RESET);
            terminal.flush();
        }
    }

    private static void paintFrame(Terminal terminal, String frame, int height, boolean clearFirst) {
        if (clearFirst) {
            clearScreen(terminal);
        }

        String[] lines = frame.split("\n", -1);
        for (int row = 0; row < height; row++) {
            terminal.writer().print("\033[" + (row + 1) + ";1H");
            terminal.writer().print(row < lines.length ? lines[row] : "\033[K");
        }
        terminal.writer().print("\033[1;1H");
    }

    private static void clearScreen(Terminal terminal) {
        TerminalClear.clear(terminal);
    }

    private static KeyMap<Action> keys(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.LEFT, "h", "H");
        map.bind(Action.RIGHT, "l", "L");
        map.bind(Action.UP, "w", "W");
        map.bind(Action.DOWN, "s", "S");
        map.bind(Action.PREVIOUS_PAGE, "a", "A");
        map.bind(Action.NEXT_PAGE, "d", "D");
        map.bind(Action.SELECT, "\r", "\n");
        map.bind(Action.EXIT, "q", "Q", "\033");

        bindTerminalKey(map, terminal, Capability.key_left, Action.LEFT);
        bindTerminalKey(map, terminal, Capability.key_right, Action.RIGHT);
        bindTerminalKey(map, terminal, Capability.key_up, Action.UP);
        bindTerminalKey(map, terminal, Capability.key_down, Action.DOWN);
        TerminalInput.bindMouseIgnore(map, terminal, Action.IGNORE);

        return map;
    }

    private static void bindTerminalKey(KeyMap<Action> map, Terminal terminal, Capability capability, Action action) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }

    private static String renderFrame(
            List<GameModeDefinition> modes,
            AchievementSystem achievements,
            DiscoverySystem discoveries,
            int selected,
            int page,
            int width,
            int height,
            Grid grid) {

        StringBuilder out = new StringBuilder();
        out.append(RESET);

        if (width < CARD_WIDTH + 2 || height < HEADER_LINES + CARD_HEIGHT + FOOTER_LINES) {
            appendLine(out, width, RED + BOLD + "El terminal és massa petit per mostrar els modes." + RESET);
            appendLine(out, width, DARK_GRAY + "Augmenta la mida de la finestra." + RESET);
            fillRest(out, width, height, 2);
            return out.toString();
        }

        int pageSize = grid.pageSize();
        int totalPages = totalPages(modes.size(), pageSize);
        int startIndex = page * pageSize;

        appendLine(out, width, "");
        String pageIndicator = totalPages > 1
                ? DARK_GRAY + "                                      Pàgina " + (page + 1) + " / " + totalPages + RESET
                : "";
        appendLine(out, width, BOLD + "Modes de joc" + RESET + pageIndicator);
        appendLine(out, width, "");

        for (int row = 0; row < grid.rows(); row++) {
            String[] lines = emptyGridRow();

            for (int col = 0; col < grid.cols(); col++) {
                int index = startIndex + row * grid.cols() + col;
                String[] card = index < modes.size()
                        ? cardLines(modes.get(index), index == selected,
                                modes.get(index).isUnlocked(achievements, discoveries))
                        : emptyCard();

                for (int line = 0; line < CARD_HEIGHT; line++) {
                    lines[line] += card[line];
                    if (col < grid.cols() - 1) {
                        lines[line] += " ".repeat(GAP);
                    }
                }
            }

            for (String line : lines) {
                appendLine(out, width, line);
            }
        }

        int usedLines = HEADER_LINES + grid.rows() * CARD_HEIGHT;
        fillRest(out, width, height - FOOTER_LINES, usedLines);
        appendLine(out, width, "");
        String pageControl = totalPages > 1 ? " · [A/D] pàgina" : "";
        appendLine(out, width, DARK_GRAY
                + "[←/→] carta · [↑/↓] fila" + pageControl + " · [Enter] triar · [Q/Esc] tornar"
                + RESET);
        appendLine(out, width, "");

        return out.toString();
    }

    private static String[] cardLines(GameModeDefinition mode, boolean selected, boolean unlocked) {
        GameModePresentation presentation = mode.presentation();

        String border;
        String titleColor;

        if (selected) {
            border = GREEN + BOLD;
            titleColor = GREEN + BOLD;
        } else if (unlocked) {
            border = WHITE;
            titleColor = WHITE + BOLD;
        } else {
            border = DARK_GRAY;
            titleColor = DARK_GRAY + BOLD;
        }

        String textColor = unlocked ? WHITE : DARK_GRAY;
        String statusColor = unlocked ? GREEN : YELLOW;

        String title = unlocked ? mode.name() : presentation.lockedTitle();
        String subtitle = unlocked ? firstText(presentation.shortDescription(), mode.description())
                : presentation.lockedDescription();
        List<String> details = unlocked ? menuDetails(mode) : presentation.lockedHints();
        String section = unlocked ? "Trets" : "Indicis";
        String status = unlocked ? "Disponible" : "Bloquejat";

        int innerWidth = CARD_WIDTH - 2;
        int paddedWidth = innerWidth - CONTENT_PADDING * 2;
        List<String> subtitleLines = wrap(subtitle, paddedWidth, 2);
        List<String> detailLines = bulletLines(details, paddedWidth, DETAIL_LINES);

        String topLeft = selected ? "╔" : "┌";
        String topRight = selected ? "╗" : "┐";
        String bottomLeft = selected ? "╚" : "└";
        String bottomRight = selected ? "╝" : "┘";
        String horizontal = selected ? "═" : "─";
        String vertical = selected ? "║" : "│";

        List<String> lines = new ArrayList<>(CARD_HEIGHT);
        lines.add(border + topLeft + horizontal.repeat(CARD_WIDTH - 2) + topRight + RESET);
        lines.add(cardLine(border, vertical, titleColor, title, innerWidth));
        lines.add(cardLine(border, vertical, textColor, subtitleLines.get(0), innerWidth));
        lines.add(cardLine(border, vertical, textColor, subtitleLines.get(1), innerWidth));
        lines.add(cardLine(border, vertical, textColor, section, innerWidth));
        for (String detailLine : detailLines) {
            lines.add(cardLine(border, vertical, textColor, detailLine, innerWidth));
        }
        lines.add(border + vertical + RESET + fit("", innerWidth) + border + vertical + RESET);
        lines.add(cardLine(border, vertical, statusColor, status, innerWidth));
        lines.add(border + bottomLeft + horizontal.repeat(CARD_WIDTH - 2) + bottomRight + RESET);
        return lines.toArray(String[]::new);
    }

    private static String cardLine(String border, String vertical, String color, String text, int innerWidth) {
        return border + vertical + RESET + color + paddedCell(text, innerWidth) + RESET + border + vertical + RESET;
    }

    private static List<String> menuDetails(GameModeDefinition mode) {
        List<String> details = new ArrayList<>(mode.presentation().details());
        if (details.size() >= DETAIL_LINES) {
            return details;
        }

        if (!mode.rules().hasActionRestrictions()) {
            addIfRoom(details, "Cap gest conegut queda fora del duel");
        } else if (!mode.rules().allowsAction(rpgcombat.combat.models.Action.CHARGE)) {
            addIfRoom(details, "La càrrega roman fora de l'aprenentatge");
        }

        if (!mode.rules().specialActionsEnabled()) {
            addIfRoom(details, "Les veus especials callen abans d'aparèixer");
        }

        if (mode.rules().maxPerks() <= 1) {
            addIfRoom(details, "Només una benedicció pot arrelar");
        } else {
            addIfRoom(details, "Les benediccions poden ramificar-se");
        }

        if (!mode.rules().divinePerksEnabled()) {
            addIfRoom(details, "Cap pacte diví desperta en aquest llindar");
        } else {
            addIfRoom(details, "Els pactes divins romanen desperts");
        }

        if (!mode.rules().showUnlockableWeapons()) {
            addIfRoom(details, "Les armes revelades no responen aquí");
        }

        if (mode.rules().chaos().activation() == ChaosActivation.DISABLED) {
            addIfRoom(details, "El Caos no travessa la porta");
        } else {
            addIfRoom(details, "El Caos pot escoltar");
        }

        return details;
    }

    private static void addIfRoom(List<String> details, String value) {
        if (details.size() >= DETAIL_LINES || value == null || value.isBlank() || details.contains(value)) {
            return;
        }
        details.add(value);
    }

    private static List<String> bulletLines(List<String> values, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        List<String> effective = values == null || values.isEmpty()
                ? List.of("La senda encara no ha escrit cap signe.")
                : values;

        for (String value : effective) {
            if (lines.size() >= maxLines) {
                break;
            }
            List<String> wrapped = wrap(value, Math.max(1, width - 2), maxLines - lines.size());
            for (int i = 0; i < wrapped.size() && lines.size() < maxLines; i++) {
                String prefix = i == 0 ? "• " : "  ";
                lines.add(prefix + wrapped.get(i));
            }
        }

        while (lines.size() < maxLines) {
            lines.add("");
        }
        return lines;
    }

    private static List<String> wrap(String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        String safe = text == null ? "" : text.trim();

        while (!safe.isEmpty() && lines.size() < maxLines) {
            if (safe.length() <= width) {
                lines.add(safe);
                break;
            }

            int cut = safe.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width;
            }

            String line = safe.substring(0, cut).trim();
            safe = safe.substring(Math.min(cut, safe.length())).trim();
            if (lines.size() == maxLines - 1 && !safe.isEmpty()) {
                line = ellipsis(line, width);
            }
            lines.add(line);
        }

        while (lines.size() < maxLines) {
            lines.add("");
        }
        return lines;
    }

    private static String ellipsis(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        return text.length() <= width - 1
                ? text + "…"
                : text.substring(0, Math.max(0, width - 1)) + "…";
    }

    private static int initialSelection(
            List<GameModeDefinition> modes,
            AchievementSystem achievements,
            DiscoverySystem discoveries,
            String defaultModeId) {
        for (int i = 0; i < modes.size(); i++) {
            GameModeDefinition mode = modes.get(i);
            if (mode.id().equalsIgnoreCase(defaultModeId) && mode.isUnlocked(achievements, discoveries)) {
                return i;
            }
        }
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).isUnlocked(achievements, discoveries)) {
                return i;
            }
        }
        return 0;
    }

    private static GameModeDefinition fallbackMode(
            List<GameModeDefinition> modes,
            AchievementSystem achievements,
            DiscoverySystem discoveries,
            String defaultModeId) {
        GameModeDefinition fallback = GameModeRegistry.getOrDefault(defaultModeId);
        if (fallback.isUnlocked(achievements, discoveries)) {
            return fallback;
        }
        for (GameModeDefinition mode : modes) {
            if (mode.isUnlocked(achievements, discoveries)) {
                return mode;
            }
        }
        return fallback;
    }

    private static Grid grid(int width, int height) {
        int cols = Math.max(1, (width + GAP) / (CARD_WIDTH + GAP));
        int rows = Math.max(1, (height - HEADER_LINES - FOOTER_LINES) / CARD_HEIGHT);
        return new Grid(cols, rows);
    }

    private static int totalPages(int totalItems, int pageSize) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    private static String[] emptyGridRow() {
        String[] lines = new String[CARD_HEIGHT];
        for (int i = 0; i < CARD_HEIGHT; i++) {
            lines[i] = "";
        }
        return lines;
    }

    private static String[] emptyCard() {
        String blank = " ".repeat(CARD_WIDTH);
        String[] lines = new String[CARD_HEIGHT];
        for (int i = 0; i < CARD_HEIGHT; i++) {
            lines[i] = blank;
        }
        return lines;
    }

    private static String firstText(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private static String fit(String text, int width) {
        String safe = text == null ? "" : text;
        return safe.length() > width
                ? safe.substring(0, Math.max(0, width - 1)) + "…"
                : safe + " ".repeat(Math.max(0, width - safe.length()));
    }

    private static String paddedCell(String text, int width) {
        int contentWidth = Math.max(0, width - CONTENT_PADDING * 2);
        return " ".repeat(CONTENT_PADDING)
                + fit(text, contentWidth)
                + " ".repeat(CONTENT_PADDING);
    }

    private static void appendLine(StringBuilder out, int width, String text) {
        out.append(text).append("\033[K");
        int visible = visibleLength(text);
        if (visible < width) {
            out.append(" ".repeat(width - visible));
        }
        out.append("\n");
    }

    private static int visibleLength(String text) {
        int length = 0;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escape) {
                if (ch == 'm') {
                    escape = false;
                }
            } else if (ch == '\u001b') {
                escape = true;
            } else {
                length++;
            }
        }
        return length;
    }

    private static void fillRest(StringBuilder out, int width, int height, int usedLines) {
        for (int i = usedLines; i < height; i++) {
            appendLine(out, width, "");
        }
    }

    private record Grid(int cols, int rows) {
        int pageSize() {
            return Math.max(1, cols * rows);
        }
    }
}
