package rpgcombat.creator.editor;

import rpgcombat.creator.CharacterCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jline.terminal.Terminal;

import rpgcombat.models.breeds.Breed;
import rpgcombat.perks.divine.DivinePerkDefinition;
import rpgcombat.models.characters.Stat;
import rpgcombat.creator.score.CharacterBuildScore;
import rpgcombat.utils.ui.Ansi;

/** Renderitza el formulari de creació. */
final class CharacterCreationRenderer {
    private final TerminalPainter painter = new TerminalPainter();
    private static final Pattern LINE_SPLITTER = Pattern.compile("\\s+");

    /** Dibuixa tot el formulari. */
    void renderAll(Terminal terminal, CharacterDraft draft, EditorAction selected, EditorAction editing,
            String editValue, int editCursor, String message) {
        painter.clear(terminal);
        invalidateRightPanelCache();
        renderTitle(terminal, draft);
        renderIdentityBox(terminal, draft, selected, editing, editValue, editCursor);
        renderStatsBox(terminal, draft, selected);
        renderBreedInfoBox(terminal, draft);
        renderActionsBox(terminal, draft, selected);
        renderControls(terminal, editing);
        renderMessage(terminal, draft, selected, message);
        terminal.flush();
    }

    /** Redibuixa només el canvi de selecció. */
    void renderSelectionChange(Terminal terminal, CharacterDraft draft, EditorAction oldAction, EditorAction newAction,
            String message) {
        renderField(terminal, draft, oldAction, newAction, null, "", 0);
        renderField(terminal, draft, newAction, newAction, null, "", 0);
        renderMessage(terminal, draft, newAction, message);
        terminal.flush();
    }

    /** Redibuixa el canvi d'una acció. */
    void renderActionChange(Terminal terminal, CharacterDraft draft, EditorAction action, EditorAction selected,
            String message) {
        renderTitle(terminal, draft);
        if (action == EditorAction.EDIT_BREED || action == EditorAction.EDIT_DIVINE_PERK) {
            renderField(terminal, draft, action, selected, null, "", 0);

            if (action == EditorAction.EDIT_BREED) {
                renderField(terminal, draft, EditorAction.EDIT_DIVINE_PERK, selected, null, "", 0);
            }

            renderStatsBox(terminal, draft, selected);
            renderBreedInfoBox(terminal, draft);
            renderBuildScore(terminal, draft, EditorLayout.BUILD_SCORE_ROW);
        } else if (action.isStat()) {
            renderField(terminal, draft, action, selected, null, "", 0);
            renderField(terminal, draft, EditorAction.CONFIRM, selected, null, "", 0);
            renderBuildScore(terminal, draft, EditorLayout.BUILD_SCORE_ROW);
        } else {
            renderField(terminal, draft, action, selected, null, "", 0);
        }
        renderMessage(terminal, draft, selected, message);
        terminal.flush();
    }

    /** Prepara l'edició d'un camp. */
    void renderTextEditStart(Terminal terminal, CharacterDraft draft, EditorAction action, String editValue,
            int editCursor,
            String message) {
        renderField(terminal, draft, action, action, action, editValue, editCursor);
        renderControls(terminal, action);
        renderMessage(terminal, draft, action, message);
        terminal.flush();
    }

    /** Redibuixa només el camp de text actiu. */
    void renderTextEdit(Terminal terminal, CharacterDraft draft, EditorAction action, String editValue,
            int editCursor) {
        renderField(terminal, draft, action, action, action, editValue, editCursor);
        terminal.flush();
    }

    /** Redibuixa el camp i el missatge d'edició. */
    void renderTextEditMessage(Terminal terminal, CharacterDraft draft, EditorAction action, String editValue,
            int editCursor, String message) {
        renderField(terminal, draft, action, action, action, editValue, editCursor);
        renderMessage(terminal, draft, action, message);
        terminal.flush();
    }

    /** Redibuixa l'estat després d'editar. */
    void renderAfterTextEdit(Terminal terminal, CharacterDraft draft, EditorAction action, String message) {
        renderField(terminal, draft, action, action, null, "", 0);
        renderControls(terminal, null);
        renderMessage(terminal, draft, action, message);
        terminal.flush();
    }

    /** Dibuixa el títol amb els punts. */
    void renderTitle(Terminal terminal, CharacterDraft draft) {
        painter.replaceLine(terminal, EditorLayout.TITLE_ROW, EditorLayout.LEFT_COL, title(draft));
    }

    /** Dibuixa un camp concret. */
    void renderField(Terminal terminal, CharacterDraft draft, EditorAction action, EditorAction selected,
            EditorAction editing, String editValue, int editCursor) {
        int row = EditorLayout.rowFor(action);
        String text = lineFor(draft, action, selected, editing, editValue, editCursor);
        painter.replaceLine(terminal, row, EditorLayout.CONTENT_COL, text);
    }

    /** Dibuixa la caixa d'identitat. */
    private void renderIdentityBox(Terminal terminal, CharacterDraft draft, EditorAction selected, EditorAction editing,
            String editValue, int editCursor) {
        int row = painter.boxTop(terminal, EditorLayout.IDENTITY_ROW, EditorLayout.LEFT_COL,
                EditorLayout.LEFT_BOX_WIDTH, "Identitat");
        renderField(terminal, draft, EditorAction.EDIT_NAME, selected, editing, editValue, editCursor);
        renderField(terminal, draft, EditorAction.EDIT_AGE, selected, editing, editValue, editCursor);
        renderField(terminal, draft, EditorAction.EDIT_BREED, selected, editing, editValue, editCursor);
        renderField(terminal, draft, EditorAction.EDIT_DIVINE_PERK, selected, editing, editValue, editCursor);
        painter.boxBottom(terminal, row + 4, EditorLayout.LEFT_COL, EditorLayout.LEFT_BOX_WIDTH);
    }

    /** Dibuixa la caixa d'estadístiques. */
    private void renderStatsBox(Terminal terminal, CharacterDraft draft, EditorAction selected) {
        int row = painter.boxTop(terminal, EditorLayout.STATS_ROW, EditorLayout.LEFT_COL,
                EditorLayout.LEFT_BOX_WIDTH, "Estadístiques");
        for (EditorAction action : statActions()) {
            renderField(terminal, draft, action, selected, null, "", 0);
            row++;
        }
        painter.boxBottom(terminal, row, EditorLayout.LEFT_COL, EditorLayout.LEFT_BOX_WIDTH);
    }

    private static final String DESCRIPTION_FOOT = "Un altre déu et concedeix una ajuda discreta… no per fe, sinó per pietat.";

    /** Dibuixa la informació de raça. */
    private void renderBreedInfoBox(Terminal terminal, CharacterDraft draft) {
        int firstRow = EditorLayout.IDENTITY_ROW;
        int maxRow = rightPanelLastRow();
        List<String> lines = blankRightPanelLines(firstRow, maxRow);

        Breed breed = draft.breed();
        int row = EditorLayout.IDENTITY_ROW;
        putRightPanelLine(lines, firstRow, row++, rightBoxTop("Informació de la raça"));

        putRightPanelContent(lines, firstRow, row++, Ansi.BOLD + Ansi.CYAN + breed.getName() + Ansi.RESET);
        row++;

        String bonus = "+" + (int) Math.round(breed.bonus() * 100.0) + "% a " + breed.bonusStat().getName();
        putRightPanelContent(lines, firstRow, row++, Ansi.GREEN + "Bonus racial" + Ansi.RESET
                + Ansi.DARK_GRAY + "  │  " + Ansi.RESET + Ansi.BOLD + bonus + Ansi.RESET);
        row++;

        putRightPanelContent(lines, firstRow, row++, Ansi.BOLD + "Descripció" + Ansi.RESET);
        row = putRightPanelWrappedContent(lines, firstRow, maxRow, row, breed.getDescription());

        row++;
        DivinePerkDefinition perk = draft.divinePerk();
        putRightPanelContent(lines, firstRow, row++, Ansi.BOLD + "Perk divina" + Ansi.RESET);
        if (perk == null) {
            putRightPanelContent(lines, firstRow, row++,
                    Ansi.YELLOW + "No hi ha perks divines carregades per a aquesta raça." + Ansi.RESET);
        } else {
            putRightPanelContent(lines, firstRow, row++, Ansi.CYAN + perk.displayName() + Ansi.RESET);
            if (!perk.shortDescription().isBlank()) {
                putRightPanelContent(lines, firstRow, row++, Ansi.DARK_GRAY + perk.shortDescription() + Ansi.RESET);
            }
            row = putRightPanelWrappedContent(lines, firstRow, maxRow, row, perk.description());
        }

        int bottom = Math.max(row + 1, EditorLayout.BOX_MIN_BREED_BOTTOM);
        bottom = Math.min(bottom, maxRow);
        putRightPanelLine(lines, firstRow, bottom, rightBoxBottom());

        int footRow = bottom + EditorLayout.BREED_HINT_GAP;
        if (footRow < EditorLayout.HELP_ROW) {
            putRightPanelLine(lines, firstRow, footRow, Ansi.DARK_GRAY + DESCRIPTION_FOOT + Ansi.RESET);
        }

        paintRightPanelLines(terminal, firstRow, lines);
    }

    private List<String> lastRightPanelLines = List.of();

    /** Crea línies buides per al panell dret. */
    private List<String> blankRightPanelLines(int firstRow, int lastRow) {
        int count = Math.max(0, lastRow - firstRow + 1);
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add("");
        }
        return lines;
    }

    /** Escriu una línia completa del panell dret al buffer de dibuix. */
    private void putRightPanelLine(List<String> lines, int firstRow, int row, String text) {
        int index = row - firstRow;
        if (index >= 0 && index < lines.size()) {
            lines.set(index, text == null ? "" : text);
        }
    }

    /** Escriu una línia de contingut del panell dret al buffer de dibuix. */
    private void putRightPanelContent(List<String> lines, int firstRow, int row, String text) {
        putRightPanelLine(lines, firstRow, row, "   " + (text == null ? "" : text));
    }

    /** Escriu text partit al buffer del panell dret sense envair la zona d'ajuda. */
    private int putRightPanelWrappedContent(List<String> lines, int firstRow, int maxRow, int row, String text) {
        int width = EditorLayout.RIGHT_BOX_WIDTH - EditorLayout.BOX_HORIZONTAL_PADDING;
        for (String line : wrap(text, width)) {
            if (row > maxRow) {
                return row;
            }
            putRightPanelContent(lines, firstRow, row++, Ansi.DARK_GRAY + line + Ansi.RESET);
        }
        return row;
    }

    /** Dibuixa només les línies que han canviat del panell dret. */
    private void paintRightPanelLines(Terminal terminal, int firstRow, List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String previous = i < lastRightPanelLines.size() ? lastRightPanelLines.get(i) : null;
            if (!line.equals(previous)) {
                painter.replaceLine(terminal, firstRow + i, EditorLayout.RIGHT_COL, line);
            }
        }

        for (int i = lines.size(); i < lastRightPanelLines.size(); i++) {
            painter.replaceLine(terminal, firstRow + i, EditorLayout.RIGHT_COL, "");
        }

        lastRightPanelLines = List.copyOf(lines);
    }

    /** Invalida la memòria del panell dret després d'una neteja completa. */
    private void invalidateRightPanelCache() {
        lastRightPanelLines = List.of();
    }

    /** Retorna la vora superior de la caixa dreta. */
    private String rightBoxTop(String title) {
        String line = "┌─ " + title + " " + "─".repeat(Math.max(0, EditorLayout.RIGHT_BOX_WIDTH - title.length() - 5)) + "┐";
        return Ansi.DARK_GRAY + line + Ansi.RESET;
    }

    /** Retorna la vora inferior de la caixa dreta. */
    private String rightBoxBottom() {
        return Ansi.DARK_GRAY + "└" + "─".repeat(EditorLayout.RIGHT_BOX_WIDTH - 2) + "┘" + Ansi.RESET;
    }

    /** Retorna l'última fila segura del panell dret. */
    private int rightPanelLastRow() {
        return EditorLayout.HELP_ROW - 1;
    }

    /** Dibuixa la caixa d'accions. */
    private void renderActionsBox(Terminal terminal, CharacterDraft draft, EditorAction selected) {
        int row = painter.boxTop(terminal, EditorLayout.ACTIONS_ROW, EditorLayout.LEFT_COL,
                EditorLayout.LEFT_BOX_WIDTH, "Accions");
        renderBuildScore(terminal, draft, row++);
        renderField(terminal, draft, EditorAction.RANDOMIZE, selected, null, "", 0);
        renderField(terminal, draft, EditorAction.CONFIRM, selected, null, "", 0);
        painter.boxBottom(terminal, row + 3, EditorLayout.LEFT_COL, EditorLayout.LEFT_BOX_WIDTH);
    }

    /** Dibuixa la puntuació de la build. */
    private void renderBuildScore(Terminal terminal, CharacterDraft draft, int row) {
        CharacterBuildScore.Rating rating = CharacterBuildScore.rate(draft.statsCopy(), draft.breed());

        // --- Qualitat ---
        String color = rating.validPoints() ? Ansi.YELLOW : Ansi.RED;
        String text = Ansi.DARK_GRAY + "Qualitat:" + Ansi.RESET + " "
                + color + rating.starsText() + Ansi.RESET;

        if (rating.corrupt()) {
            text += Ansi.RED + "  estrelles corruptes" + Ansi.RESET;
        }

        // --- Conflicte intern ---
        double severity = CharacterBuildScore.detectPowerConflict(draft.statsCopy()).severity();

        if (severity >= 0.25) {
            String label;
            String symbol;
            String c;

            if (severity < 0.5) {
                label = "lleu";
                symbol = "⚠";
                c = Ansi.YELLOW;
            } else if (severity < 0.75) {
                label = "alt";
                symbol = "⚠";
                c = Ansi.RED;
            } else {
                label = "crític";
                symbol = "✖";
                c = Ansi.RED + Ansi.BOLD;
            }

            text += Ansi.DARK_GRAY + " · " + Ansi.RESET
                    + c + symbol + " " + label + Ansi.RESET;
        }

        painter.replaceLine(terminal, row, EditorLayout.CONTENT_COL, text);
    }

    /** Dibuixa l.ajuda. */
    private void renderControls(Terminal terminal, EditorAction editing) {
        painter.replaceLine(terminal, EditorLayout.HELP_ROW, EditorLayout.CONTENT_COL, "");
        painter.replaceLine(terminal, EditorLayout.HELP_ROW + 1, EditorLayout.CONTENT_COL, "");
        painter.replaceLine(terminal, EditorLayout.HELP_ROW + 2, EditorLayout.CONTENT_COL, "");

        if (editing == null) {
            painter.replaceLine(terminal, EditorLayout.HELP_ROW, EditorLayout.CONTENT_COL,
                    Ansi.DARK_GRAY
                            + "[↑/↓ o W/S] moure    [←/→ o A/D] ajustar estadístiques i dades    [Enter] editar o confirmar"
                            + Ansi.RESET);
            return;
        }

        painter.replaceLine(terminal, EditorLayout.HELP_ROW, EditorLayout.CONTENT_COL,
                Ansi.DARK_GRAY
                        + "Editant camp: [←/→] moure    [Ctrl+←/→] saltar paraules    [Inici/Fi o Ctrl+A/E] saltar"
                        + Ansi.RESET);
        painter.replaceLine(terminal, EditorLayout.HELP_ROW + 1, EditorLayout.CONTENT_COL,
                Ansi.DARK_GRAY + "[Retrocés] esborrar abans    [Supr/Ctrl+D] esborrar actual    [Ctrl+U/K/W] netejar"
                        + Ansi.RESET);
        painter.replaceLine(terminal, EditorLayout.HELP_ROW + 2, EditorLayout.CONTENT_COL,
                Ansi.DARK_GRAY + "[Enter] guardar    [Esc] cancel·lar" + Ansi.RESET);
    }

    /** Dibuixa el missatge d'estat. */
    private void renderMessage(Terminal terminal, CharacterDraft draft, EditorAction selected, String message) {
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
        if (!canConfirm(draft) && selected == EditorAction.CONFIRM) {
            color = Ansi.RED;
        }
        painter.replaceLine(terminal, EditorLayout.MESSAGE_ROW, EditorLayout.CONTENT_COL, color + text + Ansi.RESET);
    }

    /** Crea la línia d'una acció. */
    private String lineFor(CharacterDraft draft, EditorAction action, EditorAction selected, EditorAction editing,
            String editValue, int editCursor) {
        return switch (action) {
            case EDIT_NAME -> fieldLine("Nom", draft.name(), "Enter per editar", EditorLayout.NAME_INPUT_WIDTH,
                    action, selected, editing, editValue, editCursor);
            case EDIT_AGE -> fieldLine("Edat", String.valueOf(draft.age()), "Enter per editar",
                    EditorLayout.AGE_INPUT_WIDTH, action, selected, editing, editValue, editCursor);
            case EDIT_BREED -> fieldLine("Raça", draft.breed().getName(), "←/→", 0, action, selected, editing,
                    editValue, editCursor);
            case EDIT_DIVINE_PERK ->
                fieldLine("Perk divina", divinePerkLine(draft), "←/→", 0, action, selected, editing,
                        editValue, editCursor);
            case RANDOMIZE -> selectableLine("Generar valors aleatoris", false, selected == action);
            case CONFIRM -> selectableLine("Confirmar personatge", !canConfirm(draft), selected == action);
            default -> statLine(draft, action, selected == action);
        };
    }

    /** Retorna el text visible de la perk divina seleccionada. */
    private String divinePerkLine(CharacterDraft draft) {
        DivinePerkDefinition perk = draft.divinePerk();
        return perk == null ? "Sense perk" : perk.name();
    }

    /** Crea una línia de camp simple. */
    private String fieldLine(String label, String value, String hint, int inputWidth, EditorAction action,
            EditorAction selected, EditorAction editing, String editValue, int editCursor) {
        boolean active = editing == action;
        String display = active ? inputBox(editValue, editCursor, inputWidth) : Ansi.BOLD + value + Ansi.RESET;
        String line = String.format("%-" + EditorLayout.LABEL_WIDTH + "s %s", label + ":", display);
        if (!active) {
            line += Ansi.DARK_GRAY + "  " + hint + Ansi.RESET;
        }
        return selectableLine(line, false, selected == action && !active);
    }

    /** Crea una línia d'estadística. */
    private String statLine(CharacterDraft draft, EditorAction action, boolean selected) {
        int index = action.statIndex();
        int min = index == EditorAction.EDIT_CONSTITUTION.statIndex()
                ? CharacterCreator.MIN_CONSTITUTION
                : CharacterCreator.MIN_STAT;
        int value = draft.stat(index);
        boolean bonusStat = draft.breed().bonusStat() == Stat.values()[index];
        String bar = statBar(value, min, CharacterCreator.MAX_STAT, EditorLayout.STAT_BAR_WIDTH, bonusStat);
        String label = statLabel(action);
        String line = String.format("%-" + EditorLayout.LABEL_WIDTH + "s %s%3d%s  %s  %s[%2d-%2d]%s",
                label + ":", Ansi.BOLD, value, Ansi.RESET, bar, Ansi.DARK_GRAY, min, CharacterCreator.MAX_STAT,
                Ansi.RESET);
        return selectableLine(line, false, selected);
    }

    /** Crea una línia seleccionable. */
    private String selectableLine(String text, boolean disabled, boolean selected) {
        String prefix = selected ? Ansi.BOLD + Ansi.CYAN + "›" + Ansi.RESET + " " : "  ";
        String value;
        if (disabled) {
            value = Ansi.RED + text + Ansi.RESET + Ansi.DARK_GRAY + "  · corregeix els punts" + Ansi.RESET;
        } else if (selected) {
            value = Ansi.BOLD + text + Ansi.RESET;
        } else {
            value = text;
        }
        return prefix + value;
    }

    /** Crea la caixa visual d'edició. */
    private String inputBox(String value, int cursor, int width) {
        int safeWidth = Math.max(1, width);
        String clipped = value == null ? "" : value;
        if (clipped.length() > safeWidth) {
            clipped = clipped.substring(0, safeWidth);
        }
        int safeCursor = clamp(cursor, 0, clipped.length());
        int displayCursor = Math.min(safeCursor, safeWidth - 1);
        String padded = clipped + " ".repeat(Math.max(0, safeWidth - clipped.length()));
        String left = padded.substring(0, displayCursor);
        String current = String.valueOf(padded.charAt(displayCursor));
        String right = padded.substring(displayCursor + 1, safeWidth);
        return Ansi.CYAN + "[" + Ansi.RESET + Ansi.BOLD + left + Ansi.RESET + "\u001b[7m" + current
                + Ansi.RESET + Ansi.BOLD + right + Ansi.RESET + Ansi.CYAN + "]" + Ansi.RESET;
    }

    /** Crea la barra d'estadística. */
    private String statBar(int value, int min, int max, int width, boolean bonusStat) {
        int filled = Math.round(((float) (value - min) / Math.max(1, max - min)) * width);
        filled = clamp(filled, 0, width);

        String color;
        if (bonusStat)
            color = Ansi.MAGENTA;
        else if (value <= min)
            color = Ansi.YELLOW;
        else
            color = Ansi.CYAN;

        return color + "|" + "█".repeat(filled) + Ansi.DARK_GRAY + "░".repeat(width - filled) + color + "|"
                + Ansi.RESET;
    }

    /** Genera el títol. */
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
        return Ansi.BOLD + Ansi.MAGENTA + "Creació de personatge" + Ansi.RESET + Ansi.DARK_GRAY + "  ·  "
                + Ansi.RESET + status + Ansi.RESET;
    }

    /** Indica si es pot confirmar. */
    private boolean canConfirm(CharacterDraft draft) {
        return draft.totalStats() == CharacterCreator.TOTAL_POINTS
                && isValidName(draft.name())
                && draft.age() >= CharacterCreator.MIN_AGE;
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

    /** Limita un valor enter. */
    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    /** Retorna les accions d'estadística. */
    private EditorAction[] statActions() {
        return new EditorAction[] { EditorAction.EDIT_STRENGTH, EditorAction.EDIT_DEXTERITY,
                EditorAction.EDIT_CONSTITUTION, EditorAction.EDIT_INTELLIGENCE, EditorAction.EDIT_WISDOM,
                EditorAction.EDIT_CHARISMA, EditorAction.EDIT_LUCK };
    }

    /** Retorna l'etiqueta d'una estadística. */
    private String statLabel(EditorAction action) {
        return switch (action) {
            case EDIT_STRENGTH -> "Força";
            case EDIT_DEXTERITY -> "Destresa";
            case EDIT_CONSTITUTION -> "Constitució";
            case EDIT_INTELLIGENCE -> "Intel·ligència";
            case EDIT_WISDOM -> "Saviesa";
            case EDIT_CHARISMA -> "Carisma";
            case EDIT_LUCK -> "Sort";
            default -> "";
        };
    }

    /** Divideix text en línies curtes. */
    private List<String> wrap(String text, int maxWidth) {
        String safe = text == null ? "" : text.trim();
        if (safe.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        String[] words = LINE_SPLITTER.split(safe);
        for (String word : words) {
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
}
