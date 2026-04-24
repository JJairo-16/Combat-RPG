package rpgcombat.utils.terminal;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * Proporciona una instància compartida de Terminal per a tota l'aplicació.
 */
public final class SharedTerminal {
    private static Terminal terminal;

    /**
     * Constructor privat per evitar instanciació.
     */
    private SharedTerminal() {
    }

    /**
     * Retorna el Terminal compartit, creant-lo si cal.
     *
     * @return terminal compartit
     * @throws IOException si falla la creació
     */
    public static Terminal get() throws IOException {
        if (terminal == null) {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .nativeSignals(true)
                    .build();
        }

        return terminal;
    }

    /**
     * Obre una nova sessió de terminal.
     *
     * @return nova sessió de terminal
     * @throws IOException si falla l'accés al terminal
     */
    public static TerminalSession openSession() throws IOException {
        return new TerminalSession(get());
    }

    /**
     * Inicialitza el terminal anticipadament.
     *
     * @throws IOException si falla la creació
     */
    public static void preload() throws IOException {
        get();
    }

    /**
     * Tanca el terminal compartit si està obert.
     *
     * @throws IOException si falla el tancament
     */
    public static void close() throws IOException {
        if (terminal != null) {
            terminal.close();
            terminal = null;
        }
    }
}