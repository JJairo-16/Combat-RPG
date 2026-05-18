package rpgcombat.settings.ui;

import static rpgcombat.utils.ui.Ansi.BOLD;
import static rpgcombat.utils.ui.Ansi.CYAN;
import static rpgcombat.utils.ui.Ansi.DARK_GRAY;
import static rpgcombat.utils.ui.Ansi.GREEN;
import static rpgcombat.utils.ui.Ansi.RED;
import static rpgcombat.utils.ui.Ansi.RESET;
import static rpgcombat.utils.ui.Ansi.YELLOW;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.config.paths.PathsConfig;
import rpgcombat.settings.UserSettings;
import rpgcombat.settings.UserSettingsStore;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalInput;
import rpgcombat.utils.terminal.TerminalSession;

/** Pantalla interactiva per editar els ajustos de l'usuari. */
public final class SettingsScreen {
    private static final int TITLE_ROW = 1;
    private static final int LEFT_COL = 4;
    private static final int RIGHT_COL = 64;
    private static final int CONTENT_COL = 7;
    private static final int OPTIONS_ROW = 3;
    private static final int STATUS_ROW = 9;
    private static final int MESSAGE_ROW = 22;
    private static final int HELP_ROW = 25;
    private static final int RIGHT_BOX_WIDTH = 68;
    private static final int FULL_LINE_WIDTH = 132;

    private final UserSettingsStore store;
    private UserSettings saved;
    private Draft draft;
    private int cursor;
    private String message = "";
    private boolean resizePending;
    private int terminalWidth;
    private int terminalHeight;
    private final Map<String, String> paintedCells = new HashMap<>();

    private SettingsScreen(UserSettings initialSettings, UserSettingsStore store) {
        this.store = store == null
                ? new UserSettingsStore(PathsConfig.DEFAULT_USER_SETTINGS_CONFIG, PathsConfig.DEFAULT_USER_SETTINGS_SAVE_FILE)
                : store;
        this.saved = initialSettings == null ? UserSettings.defaults() : initialSettings;
        this.draft = Draft.from(saved);
    }

    /** Obre la pantalla d'ajustos i retorna els últims valors desats. */
    public static UserSettings show(UserSettings initialSettings, UserSettingsStore store) {
        return new SettingsScreen(initialSettings, store).run();
    }

    private UserSettings run() {
        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();
            BindingReader reader = new BindingReader(terminal.reader());
            KeyMap<InputAction> keyMap = buildKeyMap(terminal);
            Terminal.SignalHandler previousWinch = terminal.handle(Terminal.Signal.WINCH,
                    signal -> resizePending = true);

            terminalWidth = terminal.getWidth();
            terminalHeight = terminal.getHeight();
            renderAll(terminal, true);

            try {
                while (true) {
                    if (consumeResize(terminal)) {
                        continue;
                    }

                    InputAction input = TerminalInput.readBindingIgnoringMouse(reader, keyMap, terminal,
                            InputAction.IGNORE);

                    if (consumeResize(terminal)) {
                        continue;
                    }
                    if (input == null) {
                        renderAll(terminal, false);
                        continue;
                    }

                    if (handleInput(input, terminal)) {
                        return saved;
                    }
                }
            } finally {
                terminal.handle(Terminal.Signal.WINCH, previousWinch);
            }
        } catch (IOException e) {
            System.out.println("No s'ha pogut obrir la pantalla d'ajustos: " + e.getMessage());
            return saved;
        }
    }

    private boolean handleInput(InputAction input, Terminal terminal) throws IOException {
        return switch (input) {
            case UP -> {
                moveCursor(-1, terminal);
                yield false;
            }
            case DOWN -> {
                moveCursor(1, terminal);
                yield false;
            }
            case LEFT, RIGHT -> {
                adjustCurrentOption(terminal);
                yield false;
            }
            case SELECT -> handleSelectedAction(terminal);
            case SAVE -> {
                saveDraft(terminal);
                yield false;
            }
            case EXIT -> confirmExitIfNeeded(terminal);
            case IGNORE -> false;
        };
    }

    private boolean handleSelectedAction(Terminal terminal) {
        return switch (currentAction()) {
            case TOGGLE_MOMENTUM_MESSAGES -> {
                adjustCurrentOption(terminal);
                yield false;
            }
        };
    }

    private void moveCursor(int delta, Terminal terminal) {
        cursor = Math.floorMod(cursor + delta, SettingAction.values().length);
        message = "";
        renderAll(terminal, false);
    }

    private void adjustCurrentOption(Terminal terminal) {
        if (currentAction() != SettingAction.TOGGLE_MOMENTUM_MESSAGES) {
            return;
        }

        draft.showMomentumMessages = !draft.showMomentumMessages;
        message = "";
        renderAll(terminal, false);
    }

    private void saveDraft(Terminal terminal) {
        try {
            if (!hasChanges()) {
                message = "";
                renderAll(terminal, false);
                return;
            }
            saved = draft.toSettings();
            store.save(saved);
            message = "Ajustos desats. Pots continuar editant o tornar al menú.";
        } catch (IOException e) {
            message = "No s'han pogut desar els ajustos.";
        }
        renderAll(terminal, false);
    }

    private boolean confirmExitIfNeeded(Terminal terminal) throws IOException {
        if (!hasChanges()) {
            return true;
        }

        renderConfirmExit(terminal);
        while (true) {
            int key = terminal.reader().read();
            if (key == 's' || key == 'S') {
                return true;
            }
            if (key == 'n' || key == 'N' || key == 27) {
                message = "Sortida cancel·lada. Els canvis continuen pendents.";
                renderAll(terminal, false);
                return false;
            }
        }
    }

    private boolean hasChanges() {
        return !draft.toSettings().equals(saved);
    }

    private SettingAction currentAction() {
        return SettingAction.values()[cursor];
    }

    private KeyMap<InputAction> buildKeyMap(Terminal terminal) {
        KeyMap<InputAction> map = new KeyMap<>();
        map.bind(InputAction.UP, "w", "W");
        map.bind(InputAction.DOWN, "s", "S");
        map.bind(InputAction.LEFT, "a", "A");
        map.bind(InputAction.RIGHT, "d", "D");
        map.bind(InputAction.SELECT, "\r", "\n");
        map.bind(InputAction.SAVE, "g", "G");
        map.bind(InputAction.EXIT, "q", "Q", "\033");

        bindTerminalKey(map, InputAction.UP, terminal, Capability.key_up);
        bindTerminalKey(map, InputAction.DOWN, terminal, Capability.key_down);
        bindTerminalKey(map, InputAction.LEFT, terminal, Capability.key_left);
        bindTerminalKey(map, InputAction.RIGHT, terminal, Capability.key_right);
        TerminalInput.bindMouseIgnore(map, terminal, InputAction.IGNORE);
        return map;
    }

    private void bindTerminalKey(KeyMap<InputAction> map, InputAction action, Terminal terminal,
            Capability capability) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }

    private boolean consumeResize(Terminal terminal) {
        int width = terminal.getWidth();
        int height = terminal.getHeight();
        boolean changed = resizePending || width != terminalWidth || height != terminalHeight;
        if (!changed) {
            return false;
        }

        resizePending = false;
        terminalWidth = width;
        terminalHeight = height;
        renderAll(terminal, true);
        return true;
    }

    private void renderAll(Terminal terminal, boolean clearFirst) {
        if (clearFirst) {
            clear(terminal);
            paintedCells.clear();
        }
        replaceLine(terminal, TITLE_ROW, LEFT_COL, BOLD + CYAN + "Ajustos" + RESET);
        renderOptions(terminal);
        renderStatus(terminal);
        renderDetail(terminal);
        renderMessage(terminal);
        renderControls(terminal);
        terminal.flush();
    }

    private void renderOptions(Terminal terminal) {
        replaceLine(terminal, OPTIONS_ROW, LEFT_COL, sectionTitle("Combat"));
        replaceLine(terminal, OPTIONS_ROW + 2, CONTENT_COL, optionLine(
                SettingAction.TOGGLE_MOMENTUM_MESSAGES,
                "Missatges d'impuls",
                draft.showMomentumMessages ? "Activats" : "Desactivats"));
    }

    private void renderStatus(Terminal terminal) {
        replaceLine(terminal, STATUS_ROW, LEFT_COL, sectionTitle("Desament"));
        String status = hasChanges() ? YELLOW + "Pendent" + RESET : GREEN + "Sense canvis" + RESET;
        replaceLine(terminal, STATUS_ROW + 2, CONTENT_COL, String.format("%-24s %s", "Estat:", status));
        replaceLine(terminal, STATUS_ROW + 3, CONTENT_COL,
                DARK_GRAY + "Els canvis només s'escriuen quan prems G." + RESET);
    }

    private String optionLine(SettingAction action, String label, String value) {
        boolean selected = currentAction() == action;
        String prefix = selected ? BOLD + CYAN + "›" + RESET + " " : "  ";
        String line = String.format("%-24s %s", label + ":", GREEN + value + RESET);
        return selected ? prefix + BOLD + line + RESET : prefix + line;
    }

    private void renderDetail(Terminal terminal) {
        int col = detailCol();
        int row = detailStartRow();
        replaceLine(terminal, row++, col, sectionTitle("Detall"));
        row++;
        List<String> lines = detailLines();
        for (String line : lines) {
            replaceLine(terminal, row++, col, line);
        }
    }

    private int detailCol() {
        return terminalWidth >= RIGHT_COL + RIGHT_BOX_WIDTH ? RIGHT_COL : LEFT_COL;
    }

    private int detailStartRow() {
        return terminalWidth >= RIGHT_COL + RIGHT_BOX_WIDTH ? OPTIONS_ROW : 14;
    }

    private List<String> detailLines() {
        return switch (currentAction()) {
            case TOGGLE_MOMENTUM_MESSAGES -> List.of(
                    BOLD + "Missatges d'impuls" + RESET,
                    "",
                    "Controla si el combat mostra avisos quan un combatent",
                    "guanya, perd o aprofita l'impuls acumulat.",
                    "",
                    DARK_GRAY + "El càlcul de combat no canvia; només canvia el text." + RESET);
        };
    }

    private void renderControls(Terminal terminal) {
        replaceLine(terminal, HELP_ROW - 2, LEFT_COL, DARK_GRAY + "─".repeat(118) + RESET);
        replaceLine(terminal, HELP_ROW, CONTENT_COL,
                DARK_GRAY + "[↑/↓ o W/S] moure    [←/→ o A/D] canviar    [Enter] activar" + RESET);
        replaceLine(terminal, HELP_ROW + 1, CONTENT_COL,
                DARK_GRAY + "[G] guardar sense sortir    [Q/Esc] tornar" + RESET);
    }

    private void renderMessage(Terminal terminal) {
        String text = message;
        if (text == null || text.isBlank()) {
            text = "";
        }
        String color = hasChanges() ? YELLOW : GREEN;
        if (text.startsWith("No s'han")) {
            color = RED;
        }
        replaceLine(terminal, MESSAGE_ROW, CONTENT_COL, color + text + RESET);
    }

    private void renderConfirmExit(Terminal terminal) {
        replaceLine(terminal, MESSAGE_ROW, CONTENT_COL,
                YELLOW + "Hi ha canvis sense desar. Vols sortir igualment? [S/N]" + RESET);
        terminal.flush();
    }

    private String sectionTitle(String title) {
        return DARK_GRAY + "─ " + title + " " + "─".repeat(18) + RESET;
    }

    private void clear(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }
        moveTo(terminal, 1, 1);
    }

    private void replaceLine(Terminal terminal, int row, int col, String text) {
        String safeText = text == null ? "" : text;
        int padding = Math.max(0, spanWidth(col) - visibleLength(safeText));
        String rendered = safeText + " ".repeat(padding);
        String key = row + ":" + col;
        if (rendered.equals(paintedCells.get(key))) {
            return;
        }
        paintedCells.put(key, rendered);
        write(terminal, row, col, rendered);
    }

    private int spanWidth(int col) {
        if (col >= RIGHT_COL) {
            return RIGHT_BOX_WIDTH;
        }
        return Math.max(1, FULL_LINE_WIDTH - col);
    }

    private void write(Terminal terminal, int row, int col, String text) {
        moveTo(terminal, row, col);
        terminal.writer().print(text);
    }

    private void moveTo(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    private int visibleLength(String text) {
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

    private enum SettingAction {
        TOGGLE_MOMENTUM_MESSAGES
    }

    private enum InputAction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        SELECT,
        SAVE,
        EXIT,
        IGNORE
    }

    private static final class Draft {
        private boolean showMomentumMessages;

        private static Draft from(UserSettings settings) {
            Draft draft = new Draft();
            draft.showMomentumMessages = settings.showMomentumMessages();
            return draft;
        }

        private UserSettings toSettings() {
            return new UserSettings(showMomentumMessages);
        }
    }
}
