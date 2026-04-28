package rpgcombat.utils.terminal;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/**
 * Representa una sessió temporal sobre el terminal compartit.
 */
public final class TerminalSession implements AutoCloseable {
    private static int openSessions;

    private final Terminal terminal;
    private final boolean owner;
    private final Attributes originalAttributes;
    private boolean closed;

    /**
     * Crea una sessió interactiva apilable.
     *
     * @param terminal terminal compartit
     */
    TerminalSession(Terminal terminal) {
        this.terminal = terminal;

        synchronized (TerminalSession.class) {
            this.owner = openSessions == 0;
            openSessions++;
        }

        if (owner) {
            this.originalAttributes = terminal.enterRawMode();
            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.cursor_invisible);
            terminal.flush();
        } else {
            this.originalAttributes = null;
        }
    }

    /**
     * Retorna el terminal associat.
     *
     * @return terminal de la sessió
     */
    public Terminal terminal() {
        return terminal;
    }

    /**
     * Restaura el terminal quan es tanca l'última sessió.
     */
    @Override
    public void close() {
        boolean shouldRestore;

        synchronized (TerminalSession.class) {
            if (closed) {
                return;
            }

            closed = true;
            openSessions = Math.max(0, openSessions - 1);
            shouldRestore = owner && openSessions == 0;
        }

        if (shouldRestore) {
            terminal.setAttributes(originalAttributes);
            terminal.puts(Capability.keypad_local);
            terminal.puts(Capability.cursor_visible);
            terminal.puts(Capability.exit_ca_mode);
            terminal.flush();
        }
    }
}
