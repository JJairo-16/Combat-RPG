package rpgcombat.utils.ui;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.terminal.SharedTerminal;

/** Neteja ràpida del terminal per a pantalles interactives. */
public final class TerminalClear {
    private TerminalClear() {
    }

    /** Neteja el terminal JLine sense crear processos externs. */
    public static void clear(Terminal terminal) {
        if (terminal == null) {
            clearShared();
            return;
        }

        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }
        moveCursor(terminal, 1, 1);
    }

    /** Neteja el terminal compartit si no es disposa d'una sessió oberta. */
    public static void clearShared() {
        try {
            Terminal terminal = SharedTerminal.get();
            clear(terminal);
            terminal.flush();
        } catch (IOException ignored) {
            // Si JLine no està disponible, no fem cap clear costós ni obrim processos externs.
        }
    }

    private static void moveCursor(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }
}
