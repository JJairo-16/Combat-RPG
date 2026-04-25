package rpgcombat.utils.interactive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;

import static rpgcombat.utils.ui.Ansi.*;

/**
 * Menú amb informació lateral.
 */
public class MenuWithInformation extends SimpleMenu {

    private static final int MIN_INFO_COLUMN = 34;
    private static final int INFO_WIDTH = 52;
    private static final int INFO_MIN_HEIGHT = 9;
    private static final int INFO_MAX_LINES = 6;
    private static final int CLEAR_ROWS = 20;
    private static final int CONTROL_ROWS = 1;
    private static final int BOTTOM_SAFE_MARGIN_ROWS = 2;

    private int lastControlsRow = -1;

    private final Map<String, String> information;
    private boolean informationVisible;

    /**
     * Crea un menú amb informació.
     *
     * @param information informació per etiqueta
     */
    public MenuWithInformation(Map<String, String> information) {
        super();
        this.information = information == null ? Map.of() : information;
    }

    public boolean getInformationVisible() {
        return informationVisible;
    }

    public void setInformationVisible(boolean newState) {
        this.informationVisible = newState;
    }

    /** Crea el mapa de tecles. */
    @Override
    protected KeyMap<Action> buildKeyMap(Terminal terminal) {
        KeyMap<Action> map = super.buildKeyMap(terminal);
        map.bind(Action.EXTRA, "i", "I");
        return map;
    }

    /** Mostra o amaga la informació. */
    @Override
    protected void handleExtraAction(Terminal terminal, String title, List<String> options, int cursor) {
        informationVisible = !informationVisible;
        clearDynamicArea(terminal);
        redrawAllContent(terminal, options, cursor);
        terminal.flush();
    }

    /** Actualitza la informació quan canvia la selecció. */
    @Override
    protected void afterSelectionChanged(Terminal terminal, String title, List<String> options, int cursor) {
        if (informationVisible) {
            drawInformation(terminal, options, cursor);
            terminal.flush();
        }
    }

    /** Dibuixa tot el menú. */
    @Override
    protected void renderFull(Terminal terminal, String title, List<String> options, int cursor) {
        clearScreen(terminal);

        moveCursor(terminal, TITLE_ROW, 1);
        terminal.writer().print(formatTitle(title));

        redrawAllContent(terminal, options, cursor);
        terminal.flush();
    }

    /** Redibuixa el contingut variable. */
    private void redrawAllContent(Terminal terminal, List<String> options, int cursor) {
        for (int i = 0; i < options.size(); i++) {
            drawOptionRow(terminal, options, i, cursor);
        }

        drawInformation(terminal, options, cursor);
        drawControls(terminal, options.size());
    }

    /** Dibuixa els controls inferiors. */
    @Override
    protected void drawControls(Terminal terminal, int optionCount) {
        clearPreviousControls(terminal);

        int fallbackRow = OPTIONS_START_ROW + optionCount + CONTROLS_SPACER_ROWS;
        int bottomRow = terminal.getHeight() - BOTTOM_SAFE_MARGIN_ROWS - CONTROL_ROWS + 1;
        int row = Math.max(fallbackRow, bottomRow);

        lastControlsRow = row;

        moveCursor(terminal, row, leftPadding);
        clearCurrentLine(terminal);

        terminal.writer().print(DARK_GRAY);
        terminal.writer().print("[↑/↓ o W/S] moure   ");
        terminal.writer().print("[I] mostrar/amagar informació   ");
        terminal.writer().print("[Enter] seleccionar");
        terminal.writer().print(RESET);
    }

    /** Dibuixa la informació lateral. */
    private void drawInformation(Terminal terminal, List<String> options, int cursor) {
        clearInformationBlock(terminal, options);

        if (!informationVisible || cursor < 0 || cursor >= options.size()) {
            return;
        }

        String label = safe(options.get(cursor));
        String text = safe(information.get(label));

        if (text.isBlank()) {
            text = "No hi ha informació disponible.";
        }

        List<String> lines = wrap(text, INFO_WIDTH - 4);

        if (lines.size() > INFO_MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, INFO_MAX_LINES));
            lines.set(INFO_MAX_LINES - 1,
                    trimToWidth(lines.get(INFO_MAX_LINES - 1), INFO_WIDTH - 5) + "…");
        }

        int col = getInfoColumn(options);
        int row = TITLE_ROW;

        moveCursor(terminal, row++, col);
        terminal.writer().print(
                CYAN + "┌─ " + BOLD + "Informació" + RESET + CYAN +
                        " " + "─".repeat(Math.max(0, INFO_WIDTH - 14)) +
                        RESET);

        for (String line : lines) {
            moveCursor(terminal, row++, col);
            terminal.writer().print(
                    CYAN + "│ " + RESET + line);
        }

        while (row < TITLE_ROW + INFO_MIN_HEIGHT) {
            moveCursor(terminal, row++, col);
            terminal.writer().print(CYAN + "│" + RESET);
        }
    }

    /** Neteja l'àrea variable. */
    private void clearDynamicArea(Terminal terminal) {
        for (int i = OPTIONS_START_ROW; i < OPTIONS_START_ROW + CLEAR_ROWS; i++) {
            moveCursor(terminal, i, 1);
            clearCurrentLine(terminal);
        }

        lastControlsRow = -1;
    }

    /** Neteja els controls anteriors. */
    private void clearPreviousControls(Terminal terminal) {
        if (lastControlsRow < 0) {
            return;
        }

        moveCursor(terminal, lastControlsRow, leftPadding);
        clearCurrentLine(terminal);
    }

    /** Neteja el bloc lateral. */
    private void clearInformationBlock(Terminal terminal, List<String> options) {
        int col = getInfoColumn(options);

        for (int i = 0; i < getInformationHeight(); i++) {
            moveCursor(terminal, TITLE_ROW + i, col);
            terminal.writer().print(" ".repeat(INFO_WIDTH + 6));
        }
    }

    /** Retorna la columna del bloc d'informació. */
    private int getInfoColumn(List<String> options) {
        int longest = 0;

        for (String option : options) {
            longest = Math.max(longest, safe(option).length());
        }

        return Math.max(MIN_INFO_COLUMN, leftPadding + longest + 12);
    }

    /** Retorna l'alçada del bloc d'informació. */
    private int getInformationHeight() {
        return 2 + INFO_MAX_LINES;
    }

    /** Parteix un text en línies. */
    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return List.of("");
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > width) {
                lines.add(line.toString());
                line.setLength(0);
            }

            if (!line.isEmpty()) {
                line.append(' ');
            }

            line.append(word);
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    /** Retalla un text a una amplada. */
    private String trimToWidth(String text, int width) {
        if (text == null || text.length() <= width) {
            return safe(text);
        }

        return text.substring(0, Math.max(0, width));
    }

    public static void main(String[] args) {
        MenuWithInformation menu = new MenuWithInformation(Map.of(
                "Atacar", "Canvies d'arma amb decisió, preparant-te per adaptar-te a les exigències del combat que tens davant.",
                "Defensar", "Redueix el dany rebut durant aquest torn.",
                "Inventari", "Obre la bossa d'objectes disponibles.",
                "Sortir", "Tanca el menú actual."));

        int option = menu.getOption(
                List.of("Atacar", "Defensar", "Inventari", "Sortir"),
                "Menú principal");

        System.out.println("Opció seleccionada: " + option);

        option = menu.getOption(
                List.of("Atacar", "Defensar", "Inventari", "Sortir"),
                "Menú principal");

        System.out.println("Opció seleccionada: " + option);
        
    }
}