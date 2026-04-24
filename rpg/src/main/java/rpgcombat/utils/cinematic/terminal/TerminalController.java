package rpgcombat.utils.cinematic.terminal;

import org.jline.terminal.Terminal;
import org.jline.terminal.Size;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.cinematic.style.CinematicColor;

/**
 * Proporciona utilitats per gestionar la terminal amb JLine.
 */
public class TerminalController {
    /**
     * Constructor privat per evitar instanciació.
     */
    private TerminalController() {
    }

    /**
     * Activa el mode interactiu (pantalla alternativa i cursor ocult).
     */
    public static void enterInteractiveMode(Terminal terminal) {
        terminal.puts(Capability.enter_ca_mode);
        terminal.puts(Capability.keypad_xmit);
        terminal.puts(Capability.cursor_invisible);
        terminal.flush();
    }

    /**
     * Restaura el mode normal de la terminal.
     */
    public static void exitInteractiveMode(Terminal terminal) {
        terminal.puts(Capability.keypad_local);
        terminal.puts(Capability.cursor_visible);
        terminal.puts(Capability.exit_ca_mode);
        terminal.flush();
    }

    /**
     * Neteja tota la pantalla.
     */
    public static void clearScreen(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J\033[3J");
        }

        moveCursor(terminal, 1, 1);
        terminal.flush();
    }

    /**
     * Neteja la línia actual.
     */
    public static void clearCurrentLine(Terminal terminal) {
        if (!terminal.puts(Capability.clr_eol)) {
            terminal.writer().print("\033[K");
        }

        terminal.flush();
    }

    /**
     * Mou el cursor a una posició concreta.
     */
    private static void moveCursor(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /**
     * Restableix el color per defecte.
     */
    public static void resetColor(Terminal terminal) {
        terminal.writer().print(CinematicColor.RESET.ansi());
        terminal.flush();
    }

    /**
     * Aplica un color al text.
     */
    public static void color(Terminal terminal, CinematicColor color) {
        terminal.writer().print(color.ansi());
        terminal.flush();
    }

    /**
     * Retorna l'amplada de la terminal.
     */
    static int terminalWidth(Terminal terminal) {
        if (terminal == null) {
            return 80;
        }

        int width = terminal.getWidth();
        if (width > 0) {
            return width;
        }

        Size size = terminal.getSize();
        if (size != null && size.getColumns() > 0) {
            return size.getColumns();
        }

        return 80;
    }

    /**
     * Retorna l'amplada útil per al contingut.
     */
    static int contentWidth(Terminal terminal) {
        return Math.clamp(terminalWidth(terminal) - 2L, 24, 100);
    }
}