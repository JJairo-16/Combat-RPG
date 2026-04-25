package rpgcombat.utils.interactive;

import java.io.IOException;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;

import static rpgcombat.utils.ui.Ansi.*;

/**
 * Menú interactiu simple per terminal.
 */
public class SimpleMenu {

    protected static final int TITLE_ROW = 1;
    protected static final int OPTIONS_START_ROW = 2;
    protected static final int CONTROLS_SPACER_ROWS = 1;
    private static final int DEFAULT_LEFT_PADDING = 3;

    protected final int leftPadding;

    public SimpleMenu(int leftPadding) {
        if (leftPadding < 0) throw new IllegalArgumentException("El padding no pot ser menor a 0.");

        this.leftPadding = leftPadding;
    }

    public SimpleMenu() {
        this(DEFAULT_LEFT_PADDING);
    }

    /** Accions disponibles al menú. */
    protected enum Action {
        UP,
        DOWN,
        SELECT,
        EXTRA
    }

    /**
     * Mostra el menú i retorna l'opció seleccionada.
     *
     * @param options opcions disponibles
     * @param title   títol del menú
     * @return opció seleccionada, començant per 1
     */
    public int getOption(List<String> options, String title) {
        if (options == null || options.isEmpty()) {
            return 1;
        }

        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();
            BindingReader reader = new BindingReader(terminal.reader());
            KeyMap<Action> keyMap = buildKeyMap(terminal);

            int cursor = 0;
            renderFull(terminal, title, options, cursor);

            while (true) {
                Action action = reader.readBinding(keyMap);

                if (action == null) {
                    continue;
                }

                int oldCursor = cursor;

                switch (action) {
                    case UP -> {
                        if (cursor > 0) {
                            cursor--;
                        }
                    }
                    case DOWN -> {
                        if (cursor < options.size() - 1) {
                            cursor++;
                        }
                    }
                    case SELECT -> {
                        return cursor + 1;
                    }
                    case EXTRA -> {
                        handleExtraAction(terminal, title, options, cursor);
                        renderFull(terminal, title, options, cursor);
                    }
                }

                if (oldCursor != cursor) {
                    redrawSelection(terminal, options, oldCursor, cursor);
                    afterSelectionChanged(terminal, title, options, cursor);
                }
            }

        } catch (IOException e) {
            System.out.println("No s'ha pogut obrir el menú interactiu: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Gestiona una acció extra.
     *
     * @param terminal terminal actual
     * @param title    títol del menú
     * @param options  opcions disponibles
     * @param cursor   selecció actual
     */
    protected void handleExtraAction(Terminal terminal, String title, List<String> options, int cursor) {
    }

    /**
     * S'executa quan canvia la selecció.
     *
     * @param terminal terminal actual
     * @param title    títol del menú
     * @param options  opcions disponibles
     * @param cursor   selecció actual
     */
    protected void afterSelectionChanged(Terminal terminal, String title, List<String> options, int cursor) {
    }

    /**
     * Crea el mapa de tecles.
     *
     * @param terminal terminal actual
     * @return mapa d'accions
     */
    protected KeyMap<Action> buildKeyMap(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.UP, "w", "W");
        map.bind(Action.DOWN, "s", "S");
        map.bind(Action.SELECT, "\r", "\n");

        String up = KeyMap.key(terminal, Capability.key_up);
        String down = KeyMap.key(terminal, Capability.key_down);

        if (up != null) {
            map.bind(Action.UP, up);
        }

        if (down != null) {
            map.bind(Action.DOWN, down);
        }

        return map;
    }

    /**
     * Dibuixa tot el menú.
     *
     * @param terminal terminal actual
     * @param title    títol del menú
     * @param options  opcions disponibles
     * @param cursor   selecció actual
     */
    protected void renderFull(Terminal terminal, String title, List<String> options, int cursor) {
        clearScreen(terminal);

        moveCursor(terminal, TITLE_ROW, 1);
        terminal.writer().print(formatTitle(title));

        for (int i = 0; i < options.size(); i++) {
            drawOptionRow(terminal, options, i, cursor);
        }

        drawControls(terminal, options.size());
        terminal.flush();
    }

    /**
     * Redibuixa només la selecció.
     *
     * @param terminal  terminal actual
     * @param options   opcions disponibles
     * @param oldCursor selecció anterior
     * @param newCursor selecció nova
     */
    protected void redrawSelection(Terminal terminal, List<String> options, int oldCursor, int newCursor) {
        drawOptionRow(terminal, options, oldCursor, newCursor);
        drawOptionRow(terminal, options, newCursor, newCursor);
        terminal.flush();
    }

    /**
     * Dibuixa una fila d'opció.
     *
     * @param terminal terminal actual
     * @param options  opcions disponibles
     * @param rowIndex índex de la fila
     * @param cursor   selecció actual
     */
    protected void drawOptionRow(Terminal terminal, List<String> options, int rowIndex, int cursor) {
        int row = OPTIONS_START_ROW + rowIndex;

        moveCursor(terminal, row, leftPadding);
        clearCurrentLine(terminal);

        String option = safe(options.get(rowIndex));

        if (rowIndex == cursor) {
            terminal.writer().print(CYAN + "> " + RESET + BOLD + option + RESET);
        } else {
            terminal.writer().print("  " + option);
        }
    }

    /**
     * Dibuixa els controls inferiors.
     *
     * @param terminal    terminal actual
     * @param optionCount nombre d'opcions
     */
    protected void drawControls(Terminal terminal, int optionCount) {
        int row = OPTIONS_START_ROW + optionCount + CONTROLS_SPACER_ROWS;

        moveCursor(terminal, row, leftPadding);
        clearCurrentLine(terminal);
        terminal.writer().print(DARK_GRAY + "[↑/↓ o W/S] moure" + RESET);

        moveCursor(terminal, row + 1, leftPadding);
        clearCurrentLine(terminal);
        terminal.writer().print(DARK_GRAY + "[Enter] seleccionar" + RESET);
    }

    /**
     * Neteja la pantalla.
     *
     * @param terminal terminal actual
     */
    protected void clearScreen(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }

        moveCursor(terminal, 1, 1);
    }

    /**
     * Mou el cursor.
     *
     * @param terminal terminal actual
     * @param row      fila
     * @param col      columna
     */
    protected void moveCursor(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /**
     * Neteja la línia actual.
     *
     * @param terminal terminal actual
     */
    protected void clearCurrentLine(Terminal terminal) {
        if (!terminal.puts(Capability.clr_eol)) {
            terminal.writer().print("\033[2K");
        }
    }

    /**
     * Dona format al títol.
     *
     * @param title títol original
     * @return títol formatat
     */
    protected String formatTitle(String title) {
        return BOLD + MAGENTA + "=== " + safe(title) + " ===" + RESET;
    }

    /**
     * Evita textos nuls.
     *
     * @param text text original
     * @return text segur
     */
    protected String safe(String text) {
        return text == null ? "" : text;
    }
}