package rpgcombat.creator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Stat;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.Ansi;

/** Formulari interactiu de terminal per crear personatges. */
final class CharacterCreationEditor {
    private static final int LEFT_COL = 4;
    private static final int RIGHT_COL = 66;
    private static final int CONTENT_COL = 7;
    private static final int LEFT_BOX_WIDTH = 56;
    private static final int RIGHT_BOX_WIDTH = 68;
    private static final int STAT_BAR_WIDTH = 18;
    private static final int LABEL_WIDTH = 16;
    private static final int NAME_INPUT_WIDTH = CharacterCreator.MAX_NAME_LEN + 1;
    private static final int AGE_INPUT_WIDTH = 11;
    private static final int HELP_ROW = 33;
    private static final int MESSAGE_ROW = 36;

    private int cursor;
    private EditorAction editing;
    private StringBuilder editBuffer = new StringBuilder();
    private int editCursor;
    private String message = "";

    /** Edita l'esborrany fins que es confirma o es tanca. */
    void edit(CharacterDraft draft) {
        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();
            BindingReader reader = new BindingReader(terminal.reader());
            KeyMap<InputAction> keyMap = buildKeyMap(terminal);

            render(terminal, draft);

            while (true) {
                InputAction input = reader.readBinding(keyMap);

                if (input == null) {
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
            case EDIT_BREED -> message = "La raça es canvia amb ←/→ o A/D. Mira'n la informació a la dreta.";
            case RANDOMIZE -> {
                draft.replaceGeneration(CharacterCreator.autoGenerate());
                message = "Valors aleatoris generats.";
                render(terminal, draft);
            }
            case CONFIRM -> {
                if (canConfirm(draft)) {
                    return true;
                }
                message = invalidMessage(draft);
                render(terminal, draft);
            }
            default -> message = "Ajusta aquesta estadística amb ←/→ o A/D.";
        }

        if (action != EditorAction.RANDOMIZE && action != EditorAction.CONFIRM) {
            render(terminal, draft);
        }
        return false;
    }

    /** Crea el mapa de tecles del formulari. */
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

    /** Afegeix una tecla del terminal al mapa. */
    private void bindTerminalKey(KeyMap<InputAction> map, InputAction action, Terminal terminal,
            Capability capability) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }

    /** Mou la selecció verticalment. */
    private void moveCursor(int delta, CharacterDraft draft, Terminal terminal) {
        int next = cursor + delta;
        if (next < 0 || next >= fields().length) {
            return;
        }

        cursor = next;
        message = "";
        render(terminal, draft);
    }

    /** Ajusta el camp actual si és modificable amb fletxes. */
    private void adjustCurrentField(CharacterDraft draft, int delta, Terminal terminal) {
        switch (currentAction()) {
            case EDIT_BREED -> draft.setBreed(nextBreed(draft.breed(), delta));
            case EDIT_STRENGTH -> adjustStat(draft, 0, delta, CharacterCreator.MIN_STAT);
            case EDIT_DEXTERITY -> adjustStat(draft, 1, delta, CharacterCreator.MIN_STAT);
            case EDIT_CONSTITUTION -> adjustStat(draft, 2, delta, CharacterCreator.MIN_CONSTITUTION);
            case EDIT_INTELLIGENCE -> adjustStat(draft, 3, delta, CharacterCreator.MIN_STAT);
            case EDIT_WISDOM -> adjustStat(draft, 4, delta, CharacterCreator.MIN_STAT);
            case EDIT_CHARISMA -> adjustStat(draft, 5, delta, CharacterCreator.MIN_STAT);
            case EDIT_LUCK -> adjustStat(draft, 6, delta, CharacterCreator.MIN_STAT);
            default -> {
            }
        }

        message = "";
        render(terminal, draft);
    }

    /** Redibuixa tot el formulari. */
    private void render(Terminal terminal, CharacterDraft draft) {
        clearScreen(terminal);
        writeRaw(terminal, 1, LEFT_COL, title(draft));
        drawIdentityBox(terminal, draft);
        drawStatsBox(terminal, draft);
        drawBreedInfoBox(terminal, draft);
        drawActionsBox(terminal, draft);
        drawControls(terminal);
        drawMessage(terminal, draft);
        terminal.flush();
    }

    /** Dibuixa el bloc d'identitat. */
    private void drawIdentityBox(Terminal terminal, CharacterDraft draft) {
        int row = drawBoxTop(terminal, 3, LEFT_COL, LEFT_BOX_WIDTH, "Identitat");
        row = field(terminal, row, EditorAction.EDIT_NAME, "Nom", draft.name(), "Enter per editar", NAME_INPUT_WIDTH);
        row = field(terminal, row, EditorAction.EDIT_AGE, "Edat", String.valueOf(draft.age()), "Enter per editar",
                AGE_INPUT_WIDTH);
        row = field(terminal, row, EditorAction.EDIT_BREED, "Raça", draft.breed().getName(), "←/→", 0);
        drawBoxBottom(terminal, row, LEFT_COL, LEFT_BOX_WIDTH);
    }

    /** Dibuixa el bloc d'estadístiques. */
    private void drawStatsBox(Terminal terminal, CharacterDraft draft) {
        int row = drawBoxTop(terminal, 10, LEFT_COL, LEFT_BOX_WIDTH, "Estadístiques");
        row = stat(terminal, row, draft, EditorAction.EDIT_STRENGTH, "Força", 0, CharacterCreator.MIN_STAT);
        row = stat(terminal, row, draft, EditorAction.EDIT_DEXTERITY, "Destresa", 1, CharacterCreator.MIN_STAT);
        row = stat(terminal, row, draft, EditorAction.EDIT_CONSTITUTION, "Constitució", 2,
                CharacterCreator.MIN_CONSTITUTION);
        row = stat(terminal, row, draft, EditorAction.EDIT_INTELLIGENCE, "Intel·ligència", 3,
                CharacterCreator.MIN_STAT);
        row = stat(terminal, row, draft, EditorAction.EDIT_WISDOM, "Saviesa", 4, CharacterCreator.MIN_STAT);
        row = stat(terminal, row, draft, EditorAction.EDIT_CHARISMA, "Carisma", 5, CharacterCreator.MIN_STAT);
        row = stat(terminal, row, draft, EditorAction.EDIT_LUCK, "Sort", 6, CharacterCreator.MIN_STAT);
        drawBoxBottom(terminal, row, LEFT_COL, LEFT_BOX_WIDTH);
    }

    /** Dibuixa la informació de la raça escollida. */
    private void drawBreedInfoBox(Terminal terminal, CharacterDraft draft) {
        Breed breed = draft.breed();
        int row = drawBoxTop(terminal, 3, RIGHT_COL, RIGHT_BOX_WIDTH, "Informació de la raça");

        writeRaw(terminal, row++, RIGHT_COL + 3, Ansi.BOLD + Ansi.CYAN + breed.getName() + Ansi.RESET);
        row++;

        String bonus = "+" + (int) Math.round(breed.bonus() * 100.0) + "% a " + breed.bonusStat().getName();
        writeRaw(terminal, row++, RIGHT_COL + 3, Ansi.GREEN + "Bonus racial" + Ansi.RESET
                + Ansi.DARK_GRAY + "  │  " + Ansi.RESET + Ansi.BOLD + bonus + Ansi.RESET);
        row++;

        writeRaw(terminal, row++, RIGHT_COL + 3, Ansi.BOLD + "Descripció" + Ansi.RESET);
        for (String line : wrap(breed.getDescription(), RIGHT_BOX_WIDTH - 6)) {
            writeRaw(terminal, row++, RIGHT_COL + 3, Ansi.DARK_GRAY + line + Ansi.RESET);
        }

        int bottom = Math.max(row + 1, 15);
        drawBoxBottom(terminal, bottom, RIGHT_COL, RIGHT_BOX_WIDTH);

        drawBreedHint(terminal, bottom + 2);
    }

    /** Dibuixa el consell de canvi de raça. */
    private void drawBreedHint(Terminal terminal, int row) {
        writeRaw(terminal, row, RIGHT_COL,
                Ansi.DARK_GRAY + "Consell: selecciona la raça i usa ←/→ per comparar-les sense sortir del formulari."
                        + Ansi.RESET);
    }

    /** Dibuixa el bloc d'accions. */
    private void drawActionsBox(Terminal terminal, CharacterDraft draft) {
        int row = drawBoxTop(terminal, 22, LEFT_COL, LEFT_BOX_WIDTH, "Accions");
        row++;
        row = action(terminal, row, EditorAction.RANDOMIZE, "Generar valors aleatoris", false);
        row = action(terminal, row, EditorAction.CONFIRM, "Confirmar personatge", !canConfirm(draft));
        row++;
        drawBoxBottom(terminal, row, LEFT_COL, LEFT_BOX_WIDTH);
    }

    /** Genera el títol amb l'estat dels punts. */
    private String title(CharacterDraft draft) {
        int remaining = draft.remainingPoints();
        String status;

        if (remaining == 0) {
            status = Ansi.GREEN + "punts correctes" + Ansi.RESET;
        } else if (remaining > 0) {
            status = Ansi.YELLOW + "queden " + remaining + " punts" + Ansi.RESET;
        } else {
            status = Ansi.RED + "sobren " + Math.abs(remaining) + " punts" + Ansi.RESET;
        }

        return Ansi.BOLD + Ansi.MAGENTA + "Creació de personatge" + Ansi.RESET
                + Ansi.DARK_GRAY + "  ·  " + Ansi.RESET
                + draft.totalStats() + "/" + CharacterCreator.TOTAL_POINTS
                + Ansi.DARK_GRAY + "  ·  " + Ansi.RESET + status;
    }

    /** Dibuixa un camp de text del formulari. */
    private int field(Terminal terminal, int row, EditorAction action, String label, String value, String hint,
            int inputWidth) {
        boolean selected = currentAction() == action;
        boolean active = editing == action;
        String display = active ? inputBox(editBuffer.toString(), editCursor, inputWidth)
                : Ansi.BOLD + value + Ansi.RESET;
        String line = String.format("%-" + LABEL_WIDTH + "s %s", label + ":", display);

        if (!active) {
            line += Ansi.DARK_GRAY + "  " + hint + Ansi.RESET;
        }

        printSelectable(terminal, row, line, false, selected && !active);
        return row + 1;
    }

    /** Dibuixa una estadística amb barra visual. */
    private int stat(Terminal terminal, int row, CharacterDraft draft, EditorAction action, String label, int index,
            int min) {
        int value = draft.stat(index);
        boolean bonusStat = draft.breed().bonusStat() == statByIndex(index);
        String bar = statBar(value, min, CharacterCreator.MAX_STAT, STAT_BAR_WIDTH, bonusStat);
        String line = String.format("%-" + LABEL_WIDTH + "s %s%3d%s  %s  %s[%2d-%2d]%s",
                label + ":", Ansi.BOLD, value, Ansi.RESET, bar, Ansi.DARK_GRAY, min, CharacterCreator.MAX_STAT,
                Ansi.RESET);

        printSelectable(terminal, row, line, false, currentAction() == action);
        return row + 1;
    }

    /** Dibuixa una acció seleccionable. */
    private int action(Terminal terminal, int row, EditorAction action, String label,
            boolean disabled) {
        boolean selected = currentAction() == action;
        printSelectable(terminal, row, label, disabled, selected);
        return row + 1;
    }

    /** Escriu una línia seleccionable. */
    private void printSelectable(Terminal terminal, int row, String text, boolean disabled,
            boolean selected) {
        String prefix = selected ? Ansi.BOLD + Ansi.CYAN + "›" + Ansi.RESET + " " : "  ";
        String value;

        if (disabled) {
            value = Ansi.RED + text + Ansi.RESET + Ansi.DARK_GRAY + "  · corregeix els punts" + Ansi.RESET;
        } else if (selected) {
            value = Ansi.BOLD + text + Ansi.RESET;
        } else {
            value = text;
        }

        writeRaw(terminal, row, CONTENT_COL, prefix + value);
    }

    /** Crea la caixa visual d'un camp editable. */
    private String inputBox(String value, int cursor, int width) {
        int safeWidth = Math.max(1, width);
        String clipped = value == null ? "" : value;
        if (clipped.length() > safeWidth) {
            clipped = clipped.substring(0, safeWidth);
        }

        int safeCursor = Math.clamp(cursor, 0, clipped.length());
        int displayCursor = Math.min(safeCursor, safeWidth - 1);
        String padded = clipped + " ".repeat(Math.max(0, safeWidth - clipped.length()));
        String left = padded.substring(0, displayCursor);
        String current = String.valueOf(padded.charAt(displayCursor));
        String right = padded.substring(displayCursor + 1, safeWidth);

        return Ansi.CYAN + "[" + Ansi.RESET
                + Ansi.BOLD + left + Ansi.RESET
                + "\u001b[7m" + current + Ansi.RESET
                + Ansi.BOLD + right + Ansi.RESET
                + Ansi.CYAN + "]" + Ansi.RESET;
    }

    /** Crea la barra visual d'una estadística. */
    private String statBar(int value, int min, int max, int width, boolean bonusStat) {
        int filled = Math.round(((float) (value - min) / Math.max(1, max - min)) * width);
        filled = Math.clamp(filled, 0, width);

        String color;
        if (bonusStat)
            color = Ansi.MAGENTA;
        else if (value <= min)
            color = Ansi.YELLOW;
        else
            color = Ansi.CYAN;

        return color + "|"
                + "█".repeat(filled)
                + Ansi.DARK_GRAY + "░".repeat(width - filled)
                + color + "|" + Ansi.RESET;
    }

    /** Dibuixa la part superior d'una caixa. */
    private int drawBoxTop(Terminal terminal, int row, int col, int width, String title) {
        String line = "┌─ " + title + " " + "─".repeat(Math.max(0, width - title.length() - 5)) + "┐";
        writeRaw(terminal, row, col, Ansi.DARK_GRAY + line + Ansi.RESET);
        return row + 1;
    }

    /** Dibuixa la part inferior d'una caixa. */
    private void drawBoxBottom(Terminal terminal, int row, int col, int width) {
        writeRaw(terminal, row, col, Ansi.DARK_GRAY + "└" + "─".repeat(width - 2) + "┘" + Ansi.RESET);
    }

    /** Dibuixa l'ajuda de controls. */
    private void drawControls(Terminal terminal) {
        if (editing == null) {
            writeRaw(terminal, HELP_ROW, CONTENT_COL,
                    Ansi.DARK_GRAY
                            + "[↑/↓ o W/S] moure    [←/→ o A/D] ajustar estadístiques i raça    [Enter] editar o confirmar"
                            + Ansi.RESET);
            return;
        }

        writeRaw(terminal, HELP_ROW, CONTENT_COL,
                Ansi.DARK_GRAY
                        + "Editant camp: [←/→] moure    [Ctrl+←/→] saltar paraules    [Inici/Fi o Ctrl+A/E] saltar"
                        + Ansi.RESET);
        writeRaw(terminal, HELP_ROW + 1, CONTENT_COL,
                Ansi.DARK_GRAY + "[Retrocés] esborrar abans    [Supr/Ctrl+D] esborrar actual    [Ctrl+U/K/W] netejar"
                        + Ansi.RESET);
        writeRaw(terminal, HELP_ROW + 2, CONTENT_COL,
                Ansi.DARK_GRAY + "[Enter] guardar    [Esc] cancel·lar" + Ansi.RESET);
    }

    /** Dibuixa el missatge d'estat. */
    private void drawMessage(Terminal terminal, CharacterDraft draft) {
        String text = message;

        if (text == null || text.isBlank()) {
            int remaining = draft.remainingPoints();
            if (remaining > 0) {
                text = "Encara queden " + remaining + " punts per repartir.";
            } else if (remaining < 0) {
                text = "Has repartit " + Math.abs(remaining) + " punts de més.";
            } else {
                text = "El personatge es pot confirmar.";
            }
        }

        String color = canConfirm(draft) ? Ansi.GREEN : Ansi.YELLOW;
        if (!canConfirm(draft) && currentAction() == EditorAction.CONFIRM) {
            color = Ansi.RED;
        }

        writeRaw(terminal, MESSAGE_ROW, CONTENT_COL, color + text + Ansi.RESET);
    }

    /** Permet editar el nom. */
    private void editTextField(Terminal terminal, CharacterDraft draft) throws IOException {
        editing = EditorAction.EDIT_NAME;
        editBuffer = new StringBuilder(draft.name());
        editCursor = editBuffer.length();
        message = "Edita el nom com en un camp de text.";
        render(terminal, draft);

        while (true) {
            TextKey input = readTextInput(terminal);

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
                        render(terminal, draft);
                        continue;
                    }

                    draft.setName(value);
                    finishEditing(terminal, draft, "Nom actualitzat.");
                    return;
                }
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
                case CHARACTER -> insertNameCharacter(input.character());
                default -> {
                }
            }

            render(terminal, draft);
        }
    }

    /** Permet editar l'edat. */
    private void editAgeField(Terminal terminal, CharacterDraft draft) throws IOException {
        editing = EditorAction.EDIT_AGE;
        editBuffer = new StringBuilder(String.valueOf(draft.age()));
        editCursor = editBuffer.length();
        message = "Edita l'edat com en un camp de text.";
        render(terminal, draft);

        while (true) {
            TextKey input = readTextInput(terminal);

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
                            render(terminal, draft);
                            continue;
                        }

                        draft.setAge(value);
                        finishEditing(terminal, draft, "Edat actualitzada.");
                        return;
                    } catch (NumberFormatException e) {
                        message = "L'edat ha de ser un número vàlid.";
                        render(terminal, draft);
                        continue;
                    }
                }
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
                case CHARACTER -> insertAgeCharacter(input.character());
                default -> {
                }
            }

            render(terminal, draft);
        }
    }

    /** Llegeix una tecla dins d'un camp de text. */
    private TextKey readTextInput(Terminal terminal) throws IOException {
        int ch = terminal.reader().read();

        if (ch == 27) {
            return readEscapeInput(terminal);
        }

        if (ch == '\r' || ch == '\n') {
            return TextKey.SAVE;
        }

        if (isBackspace(ch)) {
            return TextKey.BACKSPACE;
        }

        if (ch == 127) {
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

    /** Interpreta combinacions amb Ctrl. */
    private TextKey readControlInput(int ch) {
        return switch (ch) {
            case 1 -> TextKey.HOME;
            case 4 -> TextKey.DELETE;
            case 5 -> TextKey.END;
            case 11 -> TextKey.CLEAR_AFTER;
            case 21 -> TextKey.CLEAR_BEFORE;
            case 23 -> TextKey.DELETE_WORD_BEFORE;
            default -> TextKey.NONE;
        };
    }

    /** Interpreta seqüències d'escapament. */
    private TextKey readEscapeInput(Terminal terminal) throws IOException {
        int first = readPending(terminal);
        if (first < 0) {
            return TextKey.CANCEL;
        }

        if (first != '[' && first != 'O') {
            return TextKey.CANCEL;
        }

        int second = readPending(terminal);
        if (second < 0) {
            return TextKey.CANCEL;
        }

        return switch (second) {
            case 'D' -> TextKey.LEFT;
            case 'C' -> TextKey.RIGHT;
            case 'H' -> TextKey.HOME;
            case 'F' -> TextKey.END;
            case '1', '3', '4', '5', '7', '8' -> readCsiInput(terminal, second);
            default -> TextKey.NONE;
        };
    }

    /** Interpreta seqüències CSI. */
    private TextKey readCsiInput(Terminal terminal, int firstCode) throws IOException {
        StringBuilder sequence = new StringBuilder();
        sequence.append((char) firstCode);

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

        if (finalChar == 'D' && (value.contains(";5") || value.equals("5D"))) {
            return TextKey.WORD_LEFT;
        }
        if (finalChar == 'C' && (value.contains(";5") || value.equals("5C"))) {
            return TextKey.WORD_RIGHT;
        }
        if (finalChar != '~') {
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

    /** Indica si un caràcter tanca una seqüència CSI. */
    private boolean isCsiFinal(int ch) {
        return ch >= 0x40 && ch <= 0x7E;
    }

    /** Llegeix una tecla pendent amb temps límit curt. */
    private int readPending(Terminal terminal) throws IOException {
        return terminal.reader().read(30L);
    }

    /** Insereix un caràcter al nom. */
    private void insertNameCharacter(char character) {
        if (editBuffer.length() >= CharacterCreator.MAX_NAME_LEN || java.lang.Character.isISOControl(character)) {
            return;
        }

        editBuffer.insert(editCursor, character);
        editCursor++;
    }

    /** Insereix un dígit a l'edat. */
    private void insertAgeCharacter(char character) {
        if (!java.lang.Character.isDigit(character) || editBuffer.length() >= 10) {
            return;
        }

        editBuffer.insert(editCursor, character);
        editCursor++;
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

    /** Esborra el caràcter anterior al cursor. */
    private void deleteBeforeCursor() {
        if (editCursor <= 0) {
            return;
        }

        editBuffer.deleteCharAt(editCursor - 1);
        editCursor--;
    }

    /** Esborra el caràcter sota el cursor. */
    private void deleteAtCursor() {
        if (editCursor >= editBuffer.length()) {
            return;
        }

        editBuffer.deleteCharAt(editCursor);
    }

    /** Esborra el text abans del cursor. */
    private void clearBeforeCursor() {
        if (editCursor <= 0) {
            return;
        }

        editBuffer.delete(0, editCursor);
        editCursor = 0;
    }

    /** Esborra el text després del cursor. */
    private void clearAfterCursor() {
        if (editCursor >= editBuffer.length()) {
            return;
        }

        editBuffer.delete(editCursor, editBuffer.length());
    }

    /** Esborra la paraula anterior al cursor. */
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

    /** Cancel·la l'edició actual. */
    private void cancelEditing(Terminal terminal, CharacterDraft draft) {
        editing = null;
        editBuffer.setLength(0);
        message = "Edició cancel·lada.";
        render(terminal, draft);
    }

    /** Finalitza l'edició actual. */
    private void finishEditing(Terminal terminal, CharacterDraft draft, String successMessage) {
        editing = null;
        editBuffer.setLength(0);
        message = successMessage;
        render(terminal, draft);
    }

    /** Comprova si una tecla és retrocés. */
    private boolean isBackspace(int ch) {
        return ch == 127 || ch == 8;
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

    /** Indica si el personatge es pot confirmar. */
    private boolean canConfirm(CharacterDraft draft) {
        return draft.totalStats() == CharacterCreator.TOTAL_POINTS
                && isValidName(draft.name())
                && draft.age() >= CharacterCreator.MIN_AGE;
    }

    /** Retorna el motiu pel qual no es pot confirmar. */
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

    /** Ajusta una estadística dins dels límits. */
    private void adjustStat(CharacterDraft draft, int index, int delta, int min) {
        draft.setStat(index, clampLong((long) draft.stat(index) + delta, min, CharacterCreator.MAX_STAT));
    }

    /** Retorna l'estadística segons l'índex. */
    private Stat statByIndex(int index) {
        return Stat.values()[index];
    }

    /** Retorna la raça següent o anterior. */
    private Breed nextBreed(Breed current, int delta) {
        Breed[] values = Breed.values();
        int next = Math.floorMod(current.ordinal() + delta, values.length);
        return values[next];
    }

    /** Limita un valor llarg a un rang enter. */
    private int clampLong(long value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return (int) value;
    }

    /** Divideix un text en línies curtes. */
    private List<String> wrap(String text, int maxWidth) {
        String safe = text == null ? "" : text.trim();
        if (safe.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : safe.split("\\s+")) {
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= maxWidth) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    /** Neteja la pantalla del terminal. */
    private void clearScreen(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }
        moveCursor(terminal, 1, 1);
    }

    /** Escriu text en una posició concreta. */
    private void writeRaw(Terminal terminal, int row, int col, String text) {
        moveCursor(terminal, row, col);
        terminal.writer().print(text);
    }

    /** Mou el cursor del terminal. */
    private void moveCursor(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /** Retorna l'acció del camp seleccionat. */
    private EditorAction currentAction() {
        return fields()[cursor].action;
    }

    /** Retorna tots els camps del formulari. */
    private FormField[] fields() {
        return FormField.VALUES;
    }

    /** Tecla llegida durant l'edició de text. */
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

    /** Tipus d'entrada en camps de text. */
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

    /** Accions principals del teclat. */
    private enum InputAction {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        SELECT,
        IGNORE
    }

    /** Accions disponibles al formulari. */
    private enum EditorAction {
        EDIT_NAME,
        EDIT_AGE,
        EDIT_BREED,
        EDIT_STRENGTH,
        EDIT_DEXTERITY,
        EDIT_CONSTITUTION,
        EDIT_INTELLIGENCE,
        EDIT_WISDOM,
        EDIT_CHARISMA,
        EDIT_LUCK,
        RANDOMIZE,
        CONFIRM
    }

    /** Camps navegables del formulari. */
    private enum FormField {
        NAME(EditorAction.EDIT_NAME),
        AGE(EditorAction.EDIT_AGE),
        BREED(EditorAction.EDIT_BREED),
        STRENGTH(EditorAction.EDIT_STRENGTH),
        DEXTERITY(EditorAction.EDIT_DEXTERITY),
        CONSTITUTION(EditorAction.EDIT_CONSTITUTION),
        INTELLIGENCE(EditorAction.EDIT_INTELLIGENCE),
        WISDOM(EditorAction.EDIT_WISDOM),
        CHARISMA(EditorAction.EDIT_CHARISMA),
        LUCK(EditorAction.EDIT_LUCK),
        RANDOMIZE(EditorAction.RANDOMIZE),
        CONFIRM(EditorAction.CONFIRM);

        private static final FormField[] VALUES = values();
        private final EditorAction action;

        /** Assigna l'acció del camp. */
        FormField(EditorAction action) {
            this.action = action;
        }
    }
}