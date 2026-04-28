package rpgcombat.creator.editor;

import rpgcombat.creator.CharacterCreator;

import java.io.IOException;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.breeds.Breed;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;

/** Formulari interactiu de terminal per crear personatges. */
public final class CharacterCreationEditor {
    private final CharacterCreationRenderer renderer = new CharacterCreationRenderer();

    private int cursor;
    private EditorAction editing;
    private StringBuilder editBuffer = new StringBuilder();
    private int editCursor;
    private String message = "";
    private volatile boolean resizePending;
    private int terminalWidth;
    private int terminalHeight;

    /** Edita l'esborrany fins a confirmar-lo. */
    public void edit(CharacterDraft draft) {
        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();
            BindingReader reader = new BindingReader(terminal.reader());
            KeyMap<InputAction> keyMap = buildKeyMap(terminal);
            Terminal.SignalHandler previousWinch = terminal.handle(Terminal.Signal.WINCH,
                    signal -> resizePending = true);

            terminalWidth = terminal.getWidth();
            terminalHeight = terminal.getHeight();
            renderAll(terminal, draft);
            try {
                while (true) {
                    if (consumeResize(terminal, draft)) {
                        continue;
                    }
                    InputAction input = reader.readBinding(keyMap);
                    if (consumeResize(terminal, draft) || input == null) {
                        continue;
                    }
                    switch (input) {
                        case UP -> moveCursor(-1, draft, terminal);
                        case DOWN -> moveCursor(1, draft, terminal);
                        case LEFT -> adjustCurrentField(draft, -1, terminal);
                        case RIGHT -> adjustCurrentField(draft, 1, terminal);
                        case SELECT -> {
                            if (handleSelect(draft, terminal)) {
                                return;
                            }
                        }
                        case IGNORE -> {
                        }
                    }
                }
            } finally {
                terminal.handle(Terminal.Signal.WINCH, previousWinch);
            }
        } catch (IOException e) {
            System.out.println("No s'ha pogut obrir el formulari interactiu: " + e.getMessage());
        }
    }

    /** Gestiona l'acció seleccionada. */
    private boolean handleSelect(CharacterDraft draft, Terminal terminal) throws IOException {
        EditorAction action = currentAction();
        switch (action) {
            case EDIT_NAME -> editTextField(terminal, draft);
            case EDIT_AGE -> editAgeField(terminal, draft);
            case EDIT_BREED -> {
                message = "La raça es canvia amb ←/→ o A/D. Mira'n la informació a la dreta.";
                renderSelectionState(terminal, draft);
            }
            case RANDOMIZE -> {
                draft.replaceGeneration(CharacterCreator.autoGenerate());
                message = "Valors aleatoris generats.";
                renderAll(terminal, draft);
            }
            case CONFIRM -> {
                if (canConfirm(draft)) {
                    return true;
                }
                message = invalidMessage(draft);
                renderSelectionState(terminal, draft);
            }
            default -> {
                message = "Ajusta aquesta estadística amb ←/→ o A/D.";
                renderSelectionState(terminal, draft);
            }
        }
        return false;
    }

    /** Crea el mapa de tecles. */
    private KeyMap<InputAction> buildKeyMap(Terminal terminal) {
        KeyMap<InputAction> map = new KeyMap<>();
        map.bind(InputAction.UP, "w", "W");
        map.bind(InputAction.DOWN, "s", "S");
        map.bind(InputAction.LEFT, "a", "A");
        map.bind(InputAction.RIGHT, "d", "D");
        map.bind(InputAction.SELECT, "\r", "\n");
        map.bind(InputAction.IGNORE, "\033[1;5A", "\033[1;5B", "\033[1;5C", "\033[1;5D");
        map.bind(InputAction.IGNORE, "\033[5C", "\033[5D");
        bindTerminalKey(map, InputAction.UP, terminal, Capability.key_up);
        bindTerminalKey(map, InputAction.DOWN, terminal, Capability.key_down);
        bindTerminalKey(map, InputAction.LEFT, terminal, Capability.key_left);
        bindTerminalKey(map, InputAction.RIGHT, terminal, Capability.key_right);
        return map;
    }

    /** Afegeix una tecla del terminal. */
    private void bindTerminalKey(KeyMap<InputAction> map, InputAction action, Terminal terminal,
            Capability capability) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }

    /** Mou la selecció vertical. */
    private void moveCursor(int delta, CharacterDraft draft, Terminal terminal) {
        EditorAction oldAction = currentAction();

        int length = fields().length;
        cursor = Math.floorMod(cursor + delta, length);

        message = "";
        renderer.renderSelectionChange(terminal, draft, oldAction, currentAction(), message);
    }

    /** Ajusta el camp actual. */
    private void adjustCurrentField(CharacterDraft draft, int delta, Terminal terminal) {
        EditorAction action = currentAction();
        switch (action) {
            case EDIT_BREED -> draft.setBreed(nextBreed(draft.breed(), delta));
            case EDIT_STRENGTH, EDIT_DEXTERITY, EDIT_INTELLIGENCE, EDIT_WISDOM, EDIT_CHARISMA,
                    EDIT_LUCK ->
                adjustStat(draft, action.statIndex(), delta, CharacterCreator.MIN_STAT);
            case EDIT_CONSTITUTION -> adjustStat(draft, action.statIndex(), delta, CharacterCreator.MIN_CONSTITUTION);
            default -> {
                return;
            }
        }
        message = "";
        renderer.renderActionChange(terminal, draft, action, currentAction(), message);
    }

    /** Permet editar el nom. */
    private void editTextField(Terminal terminal, CharacterDraft draft) throws IOException {
        startEditing(EditorAction.EDIT_NAME, draft.name(), terminal, draft, "Edita el nom com en un camp de text.");
        while (true) {
            if (consumeResize(terminal, draft)) {
                continue;
            }
            TextKey input = readTextInput(terminal);
            if (consumeResize(terminal, draft)) {
                continue;
            }
            switch (input.type()) {
                case CANCEL -> {
                    cancelEditing(terminal, draft);
                    return;
                }
                case SAVE -> {
                    String value = editBuffer.toString().trim();
                    if (!isValidName(value)) {
                        message = "El nom ha de tenir entre " + CharacterCreator.MIN_NAME_LEN + " i "
                                + CharacterCreator.MAX_NAME_LEN + " caràcters i no pot ser només un número.";
                        renderTextEditMessage(terminal, draft);
                        continue;
                    }
                    draft.setName(value);
                    finishEditing(terminal, draft, "Nom actualitzat.");
                    return;
                }
                default -> applyTextInput(input, this::insertNameCharacter);
            }
            renderTextEdit(terminal, draft);
        }
    }

    /** Permet editar l'edat. */
    private void editAgeField(Terminal terminal, CharacterDraft draft) throws IOException {
        startEditing(EditorAction.EDIT_AGE, String.valueOf(draft.age()), terminal, draft,
                "Edita l'edat com en un camp de text.");
        while (true) {
            if (consumeResize(terminal, draft)) {
                continue;
            }
            TextKey input = readTextInput(terminal);
            if (consumeResize(terminal, draft)) {
                continue;
            }
            switch (input.type()) {
                case CANCEL -> {
                    cancelEditing(terminal, draft);
                    return;
                }
                case SAVE -> {
                    try {
                        int value = Integer.parseInt(editBuffer.toString());
                        if (value < CharacterCreator.MIN_AGE || value > CharacterCreator.MAX_AGE) {
                            message = "L'edat ha de ser com a mínim " + CharacterCreator.MIN_AGE + ".";
                            renderTextEditMessage(terminal, draft);
                            continue;
                        }
                        draft.setAge(value);
                        finishEditing(terminal, draft, "Edat actualitzada.");
                        return;
                    } catch (NumberFormatException e) {
                        message = "L'edat ha de ser un número vàlid.";
                        renderTextEditMessage(terminal, draft);
                        continue;
                    }
                }
                default -> applyTextInput(input, this::insertAgeCharacter);
            }
            renderTextEdit(terminal, draft);
        }
    }

    /** Inicia l'edició de text. */
    private void startEditing(EditorAction action, String value, Terminal terminal, CharacterDraft draft,
            String startMessage) {
        editing = action;
        editBuffer = new StringBuilder(value);
        editCursor = editBuffer.length();
        message = startMessage;
        renderTextEditStart(terminal, draft);
    }

    /** Aplica una tecla de text. */
    private void applyTextInput(TextKey input, CharacterInserter inserter) {
        switch (input.type()) {
            case LEFT -> editCursor = Math.max(0, editCursor - 1);
            case RIGHT -> editCursor = Math.min(editBuffer.length(), editCursor + 1);
            case WORD_LEFT -> moveWordLeft();
            case WORD_RIGHT -> moveWordRight();
            case HOME -> editCursor = 0;
            case END -> editCursor = editBuffer.length();
            case BACKSPACE -> deleteBeforeCursor();
            case DELETE -> deleteAtCursor();
            case CLEAR_BEFORE -> clearBeforeCursor();
            case CLEAR_AFTER -> clearAfterCursor();
            case DELETE_WORD_BEFORE -> deleteWordBeforeCursor();
            case CHARACTER -> inserter.insert(input.character());
            default -> {
            }
        }
    }

    /** Llegeix una tecla d'un camp. */
    private TextKey readTextInput(Terminal terminal) throws IOException {
        int ch = terminal.reader().read();
        if (ch == KeyCode.ESCAPE) {
            return readEscapeInput(terminal);
        }
        if (ch == KeyCode.CARRIAGE_RETURN || ch == KeyCode.LINE_FEED) {
            return TextKey.SAVE;
        }
        if (isBackspace(ch)) {
            return TextKey.BACKSPACE;
        }
        TextKey control = readControlInput(ch);
        if (control.type() != TextInputType.NONE) {
            return control;
        }
        if (!java.lang.Character.isISOControl(ch)) {
            return TextKey.character((char) ch);
        }
        return TextKey.NONE;
    }

    /** Interpreta combinacions Ctrl. */
    private TextKey readControlInput(int ch) {
        return switch (ch) {
            case KeyCode.CTRL_A -> TextKey.HOME;
            case KeyCode.CTRL_D -> TextKey.DELETE;
            case KeyCode.CTRL_E -> TextKey.END;
            case KeyCode.CTRL_K -> TextKey.CLEAR_AFTER;
            case KeyCode.CTRL_U -> TextKey.CLEAR_BEFORE;
            case KeyCode.CTRL_W -> TextKey.DELETE_WORD_BEFORE;
            default -> TextKey.NONE;
        };
    }

    /** Interpreta seqüències Esc. */
    private TextKey readEscapeInput(Terminal terminal) throws IOException {
        int first = readPending(terminal);
        if (first < 0 || (first != KeyCode.CSI_PREFIX && first != KeyCode.SS3_PREFIX)) {
            return TextKey.CANCEL;
        }
        int second = readPending(terminal);
        if (second < 0) {
            return TextKey.CANCEL;
        }
        return switch (second) {
            case KeyCode.ARROW_LEFT -> TextKey.LEFT;
            case KeyCode.ARROW_RIGHT -> TextKey.RIGHT;
            case KeyCode.HOME_H, KeyCode.END_F -> second == KeyCode.HOME_H ? TextKey.HOME : TextKey.END;
            case KeyCode.CSI_ONE, KeyCode.CSI_DELETE, KeyCode.CSI_FOUR, KeyCode.CSI_FIVE, KeyCode.CSI_SEVEN,
                    KeyCode.CSI_EIGHT ->
                readCsiInput(terminal, second);
            default -> TextKey.NONE;
        };
    }

    /** Interpreta seqüències CSI. */
    private TextKey readCsiInput(Terminal terminal, int firstCode) throws IOException {
        StringBuilder sequence = new StringBuilder().append((char) firstCode);
        int ch;
        do {
            ch = readPending(terminal);
            if (ch < 0) {
                return TextKey.NONE;
            }
            sequence.append((char) ch);
        } while (!isCsiFinal(ch));

        String value = sequence.toString();
        char finalChar = value.charAt(value.length() - 1);
        if (finalChar == KeyCode.ARROW_LEFT && (value.contains(KeyCode.CTRL_MODIFIER) || value.equals("5D"))) {
            return TextKey.WORD_LEFT;
        }
        if (finalChar == KeyCode.ARROW_RIGHT && (value.contains(KeyCode.CTRL_MODIFIER) || value.equals("5C"))) {
            return TextKey.WORD_RIGHT;
        }
        if (finalChar != KeyCode.TILDE) {
            return TextKey.NONE;
        }
        String code = value.substring(0, value.length() - 1);
        return switch (code) {
            case "1", "7" -> TextKey.HOME;
            case "4", "8" -> TextKey.END;
            case "3" -> TextKey.DELETE;
            default -> TextKey.NONE;
        };
    }

    /** Indica si tanca una seqüència CSI. */
    private boolean isCsiFinal(int ch) {
        return ch >= KeyCode.CSI_FINAL_MIN && ch <= KeyCode.CSI_FINAL_MAX;
    }

    /** Llegeix amb temps límit curt. */
    private int readPending(Terminal terminal) throws IOException {
        return terminal.reader().read(EditorLayout.ESC_READ_TIMEOUT_MS);
    }

    /** Insereix un caràcter al nom. */
    private void insertNameCharacter(char character) {
        if (editBuffer.length() >= CharacterCreator.MAX_NAME_LEN || java.lang.Character.isISOControl(character)) {
            return;
        }
        editBuffer.insert(editCursor++, character);
    }

    /** Insereix un dígit a l'edat. */
    private void insertAgeCharacter(char character) {
        if (!java.lang.Character.isDigit(character) || editBuffer.length() >= EditorLayout.AGE_MAX_DIGITS) {
            return;
        }
        editBuffer.insert(editCursor++, character);
    }

    /** Mou el cursor una paraula a l'esquerra. */
    private void moveWordLeft() {
        while (editCursor > 0 && java.lang.Character.isWhitespace(editBuffer.charAt(editCursor - 1))) {
            editCursor--;
        }
        while (editCursor > 0 && !java.lang.Character.isWhitespace(editBuffer.charAt(editCursor - 1))) {
            editCursor--;
        }
    }

    /** Mou el cursor una paraula a la dreta. */
    private void moveWordRight() {
        while (editCursor < editBuffer.length() && java.lang.Character.isWhitespace(editBuffer.charAt(editCursor))) {
            editCursor++;
        }
        while (editCursor < editBuffer.length() && !java.lang.Character.isWhitespace(editBuffer.charAt(editCursor))) {
            editCursor++;
        }
    }

    /** Esborra el caràcter anterior. */
    private void deleteBeforeCursor() {
        if (editCursor > 0) {
            editBuffer.deleteCharAt(--editCursor);
        }
    }

    /** Esborra el caràcter actual. */
    private void deleteAtCursor() {
        if (editCursor < editBuffer.length()) {
            editBuffer.deleteCharAt(editCursor);
        }
    }

    /** Esborra el text anterior. */
    private void clearBeforeCursor() {
        if (editCursor > 0) {
            editBuffer.delete(0, editCursor);
            editCursor = 0;
        }
    }

    /** Esborra el text posterior. */
    private void clearAfterCursor() {
        if (editCursor < editBuffer.length()) {
            editBuffer.delete(editCursor, editBuffer.length());
        }
    }

    /** Esborra la paraula anterior. */
    private void deleteWordBeforeCursor() {
        if (editCursor <= 0) {
            return;
        }
        int end = editCursor;
        int start = end;
        while (start > 0 && java.lang.Character.isWhitespace(editBuffer.charAt(start - 1))) {
            start--;
        }
        while (start > 0 && !java.lang.Character.isWhitespace(editBuffer.charAt(start - 1))) {
            start--;
        }
        editBuffer.delete(start, end);
        editCursor = start;
    }

    /** Cancel·la l'edició. */
    private void cancelEditing(Terminal terminal, CharacterDraft draft) {
        editing = null;
        editBuffer.setLength(0);
        message = "Edició cancel·lada.";
        renderer.renderAfterTextEdit(terminal, draft, currentAction(), message);
    }

    /** Finalitza l'edició. */
    private void finishEditing(Terminal terminal, CharacterDraft draft, String successMessage) {
        editing = null;
        editBuffer.setLength(0);
        message = successMessage;
        renderer.renderAfterTextEdit(terminal, draft, currentAction(), message);
    }

    /** Comprova si és retrocés. */
    private boolean isBackspace(int ch) {
        return ch == KeyCode.BACKSPACE || ch == KeyCode.DELETE_BACKSPACE;
    }

    /** Valida el nom del personatge. */
    private boolean isValidName(String value) {
        if (value == null || value.length() < CharacterCreator.MIN_NAME_LEN
                || value.length() > CharacterCreator.MAX_NAME_LEN) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /** Indica si es pot confirmar. */
    private boolean canConfirm(CharacterDraft draft) {
        return draft.totalStats() == CharacterCreator.TOTAL_POINTS && isValidName(draft.name())
                && draft.age() >= CharacterCreator.MIN_AGE;
    }

    /** Retorna el missatge d'error. */
    private String invalidMessage(CharacterDraft draft) {
        int remaining = draft.remainingPoints();
        if (remaining > 0) {
            return "Encara has de repartir " + remaining + " punts.";
        }
        if (remaining < 0) {
            return "Has repartit " + Math.abs(remaining) + " punts de més.";
        }
        return "Revisa les dades del personatge abans de confirmar.";
    }

    /** Ajusta una estadística. */
    private void adjustStat(CharacterDraft draft, int index, int delta, int min) {
        draft.setStat(index, clampLong((long) draft.stat(index) + delta, min, CharacterCreator.MAX_STAT));
    }

    /** Retorna la raça següent. */
    private Breed nextBreed(Breed current, int delta) {
        Breed[] values = Breed.values();
        return values[Math.floorMod(current.ordinal() + delta, values.length)];
    }

    /** Limita un valor a un rang. */
    private int clampLong(long value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return (int) value;
    }

    /** Redibuixa si el terminal ha canviat de mida. */
    private boolean consumeResize(Terminal terminal, CharacterDraft draft) {
        int width = terminal.getWidth();
        int height = terminal.getHeight();
        boolean changed = resizePending || width != terminalWidth || height != terminalHeight;
        if (!changed) {
            return false;
        }
        resizePending = false;
        terminalWidth = width;
        terminalHeight = height;
        renderAll(terminal, draft);
        return true;
    }

    /** Redibuixa tot el formulari. */
    private void renderAll(Terminal terminal, CharacterDraft draft) {
        renderer.renderAll(terminal, draft, currentAction(), editing, editBuffer.toString(), editCursor, message);
    }

    /** Redibuixa l'estat de selecció. */
    private void renderSelectionState(Terminal terminal, CharacterDraft draft) {
        renderer.renderActionChange(terminal, draft, currentAction(), currentAction(), message);
    }

    /** Inicia el dibuix del camp editat. */
    private void renderTextEditStart(Terminal terminal, CharacterDraft draft) {
        renderer.renderTextEditStart(terminal, draft, currentAction(), editBuffer.toString(), editCursor, message);
    }

    /** Redibuixa només el camp editat. */
    private void renderTextEdit(Terminal terminal, CharacterDraft draft) {
        renderer.renderTextEdit(terminal, draft, currentAction(), editBuffer.toString(), editCursor);
    }

    /** Redibuixa el camp editat i el missatge. */
    private void renderTextEditMessage(Terminal terminal, CharacterDraft draft) {
        renderer.renderTextEditMessage(terminal, draft, currentAction(), editBuffer.toString(), editCursor, message);
    }

    /** Retorna l'acció seleccionada. */
    private EditorAction currentAction() {
        return fields()[cursor].action();
    }

    /** Retorna els camps navegables. */
    private FormField[] fields() {
        return FormField.VALUES;
    }

    /** Insereix caràcters filtrats. */
    private interface CharacterInserter {
        void insert(char character);
    }

    /** Tecla de camp de text. */
    private record TextKey(TextInputType type, char character) {
        private static final TextKey NONE = new TextKey(TextInputType.NONE, '\0');
        private static final TextKey SAVE = new TextKey(TextInputType.SAVE, '\0');
        private static final TextKey CANCEL = new TextKey(TextInputType.CANCEL, '\0');
        private static final TextKey LEFT = new TextKey(TextInputType.LEFT, '\0');
        private static final TextKey RIGHT = new TextKey(TextInputType.RIGHT, '\0');
        private static final TextKey WORD_LEFT = new TextKey(TextInputType.WORD_LEFT, '\0');
        private static final TextKey WORD_RIGHT = new TextKey(TextInputType.WORD_RIGHT, '\0');
        private static final TextKey HOME = new TextKey(TextInputType.HOME, '\0');
        private static final TextKey END = new TextKey(TextInputType.END, '\0');
        private static final TextKey BACKSPACE = new TextKey(TextInputType.BACKSPACE, '\0');
        private static final TextKey DELETE = new TextKey(TextInputType.DELETE, '\0');
        private static final TextKey CLEAR_BEFORE = new TextKey(TextInputType.CLEAR_BEFORE, '\0');
        private static final TextKey CLEAR_AFTER = new TextKey(TextInputType.CLEAR_AFTER, '\0');
        private static final TextKey DELETE_WORD_BEFORE = new TextKey(TextInputType.DELETE_WORD_BEFORE, '\0');

        /** Crea una tecla de caràcter. */
        private static TextKey character(char character) {
            return new TextKey(TextInputType.CHARACTER, character);
        }
    }

    /** Tipus d'entrada de text. */
    private enum TextInputType {
        NONE,
        SAVE,
        CANCEL,
        LEFT,
        RIGHT,
        WORD_LEFT,
        WORD_RIGHT,
        HOME,
        END,
        BACKSPACE,
        DELETE,
        CLEAR_BEFORE,
        CLEAR_AFTER,
        DELETE_WORD_BEFORE,
        CHARACTER
    }

    /** Accions principals de teclat. */
    private enum InputAction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        SELECT,
        IGNORE
    }

    /** Codis de tecla sense nombres màgics. */
    private static final class KeyCode {
        private static final int CTRL_A = 1;
        private static final int CTRL_D = 4;
        private static final int CTRL_E = 5;
        private static final int CTRL_K = 11;
        private static final int CTRL_U = 21;
        private static final int CTRL_W = 23;
        private static final int ESCAPE = 27;
        private static final int CARRIAGE_RETURN = '\r';
        private static final int LINE_FEED = '\n';
        private static final int BACKSPACE = 8;
        private static final int DELETE_BACKSPACE = 127;
        private static final int CSI_PREFIX = '[';
        private static final int SS3_PREFIX = 'O';
        private static final int ARROW_LEFT = 'D';
        private static final int ARROW_RIGHT = 'C';
        private static final int HOME_H = 'H';
        private static final int END_F = 'F';
        private static final int CSI_ONE = '1';
        private static final int CSI_DELETE = '3';
        private static final int CSI_FOUR = '4';
        private static final int CSI_FIVE = '5';
        private static final int CSI_SEVEN = '7';
        private static final int CSI_EIGHT = '8';
        private static final int CSI_FINAL_MIN = 0x40;
        private static final int CSI_FINAL_MAX = 0x7E;
        private static final char TILDE = '~';
        private static final String CTRL_MODIFIER = ";5";

        private KeyCode() {
        }
    }
}
