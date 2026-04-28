package rpgcombat.creator.editor;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.ui.Ansi;

/** Escriu al terminal amb posicions fixes. */
final class TerminalPainter {
    /** Neteja tota la pantalla. */
    void clear(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }
        moveTo(terminal, EditorLayout.SCREEN_START_ROW, EditorLayout.SCREEN_START_COL);
    }

    /** Escriu text en una posició. */
    void write(Terminal terminal, int row, int col, String text) {
        moveTo(terminal, row, col);
        terminal.writer().print(text);
    }

    /** Substitueix una zona amb una sola escriptura. */
    void replaceLine(Terminal terminal, int row, int col, String text) {
        String safeText = text == null ? "" : text;
        int padding = Math.max(0, spanWidth(row, col) - visibleLength(safeText));
        write(terminal, row, col, safeText + " ".repeat(padding));
    }

    /** Sobreescriu només el text necessari. */
    void overwrite(Terminal terminal, int row, int col, String text) {
        write(terminal, row, col, text == null ? "" : text);
    }

    /** Dibuixa la part superior d'una caixa. */
    int boxTop(Terminal terminal, int row, int col, int width, String title) {
        String line = "┌─ " + title + " " + "─".repeat(Math.max(0, width - title.length() - 5)) + "┐";
        write(terminal, row, col, Ansi.DARK_GRAY + line + Ansi.RESET);
        return row + 1;
    }

    /** Dibuixa la part inferior d'una caixa. */
    void boxBottom(Terminal terminal, int row, int col, int width) {
        write(terminal, row, col, Ansi.DARK_GRAY + "└" + "─".repeat(width - 2) + "┘" + Ansi.RESET);
    }

    /** Calcula l'amplada segura. */
    private int spanWidth(int row, int col) {
        if (row == EditorLayout.TITLE_ROW || row >= EditorLayout.HELP_ROW) {
            return EditorLayout.FULL_LINE_WIDTH;
        }
        if (col >= EditorLayout.RIGHT_COL) {
            return EditorLayout.RIGHT_BOX_WIDTH + EditorLayout.BREED_HINT_GAP;
        }
        return Math.max(1, EditorLayout.RIGHT_COL - col - 1);
    }

    /** Calcula caràcters visibles sense codis ANSI. */
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

    /** Mou el cursor físic. */
    private void moveTo(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }
}
