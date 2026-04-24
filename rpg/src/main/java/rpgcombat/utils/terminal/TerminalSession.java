package rpgcombat.utils.terminal;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/**
 * Representa una sessió temporal sobre el terminal compartit.
 */
public final class TerminalSession implements AutoCloseable {
    private final Terminal terminal;
    private final Attributes originalAttributes;

    /**
     * Crea una sessió activant mode raw i configuració interactiva.
     *
     * @param terminal terminal compartit
     */
    TerminalSession(Terminal terminal) {
        this.terminal = terminal;
        this.originalAttributes = terminal.enterRawMode();

        terminal.puts(Capability.enter_ca_mode);
        terminal.puts(Capability.keypad_xmit);
        terminal.puts(Capability.cursor_invisible);
        terminal.flush();
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
     * Restaura l'estat original del terminal i tanca la sessió.
     */
    @Override
    public void close() {
        terminal.setAttributes(originalAttributes);
        terminal.puts(Capability.keypad_local);
        terminal.puts(Capability.cursor_visible);
        terminal.puts(Capability.exit_ca_mode);
        terminal.flush();
    }
}