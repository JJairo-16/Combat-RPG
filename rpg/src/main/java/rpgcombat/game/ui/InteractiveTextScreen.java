package rpgcombat.game.ui;

import static rpgcombat.utils.ui.Ansi.BOLD;
import static rpgcombat.utils.ui.Ansi.CYAN;
import static rpgcombat.utils.ui.Ansi.DARK_GRAY;
import static rpgcombat.utils.ui.Ansi.RESET;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.input.Menu;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalInput;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.TerminalClear;

/**
 * Pantalla de text amb marc, scroll i sessió interactiva pròpia.
 */
final class InteractiveTextScreen {
    private static final Pattern LINE_SPLIT = Pattern.compile("\\R");
    private static final int CONTENT_WIDTH = 86;
    private static final int LEFT_PADDING = 1;
    private static final int HEADER_ROWS = 4;
    private static final int FOOTER_ROWS = 3;

    private InteractiveTextScreen() {
    }

    static void show(String title, String content) {
        List<String> lines = lines(content);

        try (TerminalSession session = SharedTerminal.openSession()) {
            run(session.terminal(), safe(title), lines);
        } catch (IOException e) {
            TerminalClear.clearShared();
            System.out.print(content == null ? "" : content);
            Menu.pause();
        }
    }

    private static void run(Terminal terminal, String title, List<String> lines) {
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Action> keys = keys(terminal);
        int scroll = 0;

        terminal.puts(Capability.cursor_invisible);

        while (true) {
            View view = view(terminal, lines, scroll);
            scroll = view.scroll();
            render(terminal, title, lines, view);

            Action action = TerminalInput.readBindingIgnoringMouse(reader, keys, terminal, Action.IGNORE);
            if (action == null) {
                continue;
            }

            switch (action) {
                case UP -> scroll = Math.max(0, scroll - 1);
                case DOWN -> scroll = Math.min(view.maxScroll(), scroll + 1);
                case PAGE_UP -> scroll = Math.max(0, scroll - view.viewportHeight());
                case PAGE_DOWN -> scroll = Math.min(view.maxScroll(), scroll + view.viewportHeight());
                case IGNORE -> {
                }
                case EXIT -> {
                    terminal.writer().print(RESET);
                    terminal.flush();
                    return;
                }
            }
        }
    }

    private static View view(Terminal terminal, List<String> lines, int scroll) {
        int width = terminalWidth(terminal);
        int height = terminalHeight(terminal);
        int contentWidth = Math.min(CONTENT_WIDTH, Math.max(1, width - LEFT_PADDING - 1));
        int viewportHeight = Math.max(1, height - HEADER_ROWS - FOOTER_ROWS);
        int maxScroll = Math.max(0, lines.size() - viewportHeight);
        return new View(width, contentWidth, LEFT_PADDING, viewportHeight, maxScroll, Math.clamp(scroll, 0, maxScroll));
    }

    private static void render(Terminal terminal, String title, List<String> lines, View view) {
        int end = Math.min(lines.size(), view.scroll() + view.viewportHeight());
        int bottomPadding = Math.max(0, view.viewportHeight() - (end - view.scroll()));
        StringBuilder frame = new StringBuilder(14_000);

        frame.append("\033[H\033[J");
        appendHeader(frame, title, view);
        for (int i = view.scroll(); i < end; i++) {
            appendContentLine(frame, view, lines.get(i));
        }
        appendBlankContentRows(frame, view, bottomPadding);
        appendFooter(frame, view);

        terminal.writer().print(frame);
        terminal.flush();
    }

    private static void appendHeader(StringBuilder out, String title, View view) {
        appendAtMargin(out, view, DARK_GRAY + "═".repeat(view.contentWidth()) + RESET);
        appendAtMargin(out, view, BOLD + CYAN + title + RESET);
        appendAtMargin(out, view, DARK_GRAY + "─".repeat(view.contentWidth()) + RESET);
        out.append('\n');
    }

    private static void appendFooter(StringBuilder out, View view) {
        appendAtMargin(out, view, DARK_GRAY + "─".repeat(view.contentWidth()) + RESET);
        appendAtMargin(out, view, DARK_GRAY + controls(view) + RESET);
        out.append('\n');
    }

    private static void appendBlankContentRows(StringBuilder out, View view, int rows) {
        for (int i = 0; i < rows; i++) {
            appendContentLine(out, view, "");
        }
    }

    private static void appendContentLine(StringBuilder out, View view, String line) {
        out.append(" ".repeat(view.margin())).append(line == null ? "" : line).append('\n');
    }

    private static void appendAtMargin(StringBuilder out, View view, String line) {
        out.append(" ".repeat(view.margin())).append(line).append('\n');
    }

    private static String controls(View view) {
        if (view.maxScroll() <= 0) {
            return "[Enter/Q/Esc] tornar";
        }

        String position = "Línia " + (view.scroll() + 1) + "/" + (view.maxScroll() + 1);
        if (view.width() < 76) {
            return "[W/S] moure  [PgUp/PgDn] saltar  [Q/Esc] tornar  " + position;
        }
        return "[Fletxes o W/S] desplaçar  [PgUp/PgDn] pàgina  [Enter/Q/Esc] tornar  " + position;
    }

    private static List<String> lines(String content) {
        String safe = content == null || content.isEmpty() ? " " : content;
        return List.of(LINE_SPLIT.split(safe, -1));
    }

    private static int terminalWidth(Terminal terminal) {
        return terminal == null ? 80 : Math.max(1, terminal.getWidth());
    }

    private static int terminalHeight(Terminal terminal) {
        return terminal == null ? 24 : Math.max(1, terminal.getHeight());
    }

    private static String safe(String title) {
        return title == null || title.isBlank() ? " " : title;
    }

    private static KeyMap<Action> keys(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.UP, "w", "W");
        map.bind(Action.DOWN, "s", "S");
        map.bind(Action.EXIT, "q", "Q", "\033", "\r", "\n");

        bind(map, terminal, Capability.key_up, Action.UP);
        bind(map, terminal, Capability.key_down, Action.DOWN);
        bind(map, terminal, Capability.key_ppage, Action.PAGE_UP);
        bind(map, terminal, Capability.key_npage, Action.PAGE_DOWN);
        TerminalInput.bindMouseIgnore(map, terminal, Action.IGNORE);

        return map;
    }

    private static void bind(KeyMap<Action> map, Terminal terminal, Capability capability, Action action) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }

    private enum Action {
        UP,
        DOWN,
        PAGE_UP,
        PAGE_DOWN,
        EXIT,
        IGNORE
    }

    private record View(
            int width,
            int contentWidth,
            int margin,
            int viewportHeight,
            int maxScroll,
            int scroll) {
    }
}
