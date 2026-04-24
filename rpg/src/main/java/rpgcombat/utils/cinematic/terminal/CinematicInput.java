package rpgcombat.utils.cinematic.terminal;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;

/**
 * Gestiona la lectura d'entrada de l'usuari en la cinemàtica.
 */
public class CinematicInput {

    /**
     * Accions reconegudes.
     */
    public enum Action {
        NONE,
        SPACE,
        ENTER
    }

    private static final int SPACE = 32;
    private static final int ENTER = 13;
    private static final int LF = 10;

    private final Terminal terminal;

    /**
     * Crea un lector d'entrada per al terminal.
     */
    public CinematicInput(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Llegeix una acció si hi ha entrada disponible.
     */
    public Action readActionIfAvailable() throws IOException {
        NonBlockingReader reader = terminal.reader();

        int key = reader.read(1);

        if (key == NonBlockingReader.READ_EXPIRED || key < 0) {
            return Action.NONE;
        }

        if (key == SPACE) {
            return Action.SPACE;
        }

        if (key == ENTER || key == LF) {
            return Action.ENTER;
        }

        return Action.NONE;
    }
}