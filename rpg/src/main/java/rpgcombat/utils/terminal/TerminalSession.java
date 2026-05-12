package rpgcombat.utils.terminal;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Representa una sessió temporal sobre el terminal compartit.
 */
public final class TerminalSession implements AutoCloseable {
    private static final String ENABLE_MOUSE_FALLBACK =
            "\033[?1000h\033[?1002h\033[?1005h\033[?1006h\033[?1015h\033[?1016h";
    private static final String DISABLE_MOUSE_FALLBACK =
            "\033[?1000l\033[?1002l\033[?1003l\033[?1005l\033[?1006l\033[?1015l\033[?1016l";
    private static final long CURSOR_RESTORE_DELAY_MS = 75L;
    private static final ScheduledExecutorService CURSOR_RESTORE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "terminal-cursor-restore");
                thread.setDaemon(true);
                return thread;
            });

    private static int openSessions;
    private static long cursorRestoreGeneration;

    private final Terminal terminal;
    private final boolean owner;
    private final Attributes originalAttributes;
    private final Terminal.MouseTracking originalMouseTracking;
    private final boolean mouseTrackedByJLine;
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
            this.originalMouseTracking = terminal.getCurrentMouseTracking();
            synchronized (TerminalSession.class) {
                cursorRestoreGeneration++;
            }
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.cursor_invisible);
            this.mouseTrackedByJLine = terminal.trackMouse(Terminal.MouseTracking.Button);
            if (!mouseTrackedByJLine) {
                terminal.writer().print(ENABLE_MOUSE_FALLBACK);
            }
            terminal.flush();
        } else {
            this.originalAttributes = null;
            this.originalMouseTracking = null;
            this.mouseTrackedByJLine = false;
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
            long generation;
            synchronized (TerminalSession.class) {
                generation = cursorRestoreGeneration;
            }
            terminal.setAttributes(originalAttributes);
            terminal.puts(Capability.keypad_local);
            if (mouseTrackedByJLine) {
                terminal.trackMouse(originalMouseTracking == null
                        ? Terminal.MouseTracking.Off
                        : originalMouseTracking);
            } else {
                terminal.writer().print(DISABLE_MOUSE_FALLBACK);
            }
            terminal.puts(Capability.exit_ca_mode);
            terminal.flush();
            scheduleCursorRestore(terminal, generation);
        }
    }

    private static void scheduleCursorRestore(Terminal terminal, long generation) {
        CURSOR_RESTORE_EXECUTOR.schedule(() -> {
            synchronized (TerminalSession.class) {
                if (openSessions != 0 || generation != cursorRestoreGeneration) {
                    return;
                }
                cursorRestoreGeneration++;
            }

            try {
                terminal.puts(Capability.cursor_visible);
                terminal.flush();
            } catch (RuntimeException ignored) {
                // El terminal pot haver-se tancat en sortir de l'aplicació.
            }
        }, CURSOR_RESTORE_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}
