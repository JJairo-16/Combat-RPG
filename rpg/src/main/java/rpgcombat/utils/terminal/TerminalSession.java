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
    private static final long INTERACTIVE_RESTORE_DELAY_MS = 75L;
    private static final ScheduledExecutorService INTERACTIVE_RESTORE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "terminal-interactive-restore");
                thread.setDaemon(true);
                return thread;
            });

    private static int openSessions;
    private static long restoreGeneration;
    private static InteractiveState interactiveState;

    private final Terminal terminal;
    private boolean closed;

    /**
     * Crea una sessió interactiva apilable.
     *
     * @param terminal terminal compartit
     */
    TerminalSession(Terminal terminal) {
        this.terminal = terminal;

        boolean mustEnterInteractiveMode;
        synchronized (TerminalSession.class) {
            restoreGeneration++;
            mustEnterInteractiveMode = openSessions == 0 && interactiveState == null;
            openSessions++;
        }

        if (mustEnterInteractiveMode) {
            Attributes originalAttributes = terminal.enterRawMode();
            Terminal.MouseTracking originalMouseTracking = terminal.getCurrentMouseTracking();
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.cursor_invisible);
            boolean mouseTrackedByJLine = terminal.trackMouse(Terminal.MouseTracking.Button);
            if (!mouseTrackedByJLine) {
                terminal.writer().print(ENABLE_MOUSE_FALLBACK);
            }
            terminal.flush();

            synchronized (TerminalSession.class) {
                interactiveState = new InteractiveState(
                        terminal,
                        originalAttributes,
                        originalMouseTracking,
                        mouseTrackedByJLine);
            }
        } else {
            // Una pantalla anterior pot haver canviat la visibilitat del cursor.
            terminal.puts(Capability.cursor_invisible);
            terminal.flush();
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
        boolean shouldScheduleRestore;
        long generation;

        synchronized (TerminalSession.class) {
            if (closed) {
                return;
            }

            closed = true;
            openSessions = Math.max(0, openSessions - 1);
            shouldScheduleRestore = openSessions == 0 && interactiveState != null;
            generation = restoreGeneration;
        }

        if (shouldScheduleRestore) {
            scheduleInteractiveRestore(generation);
        }
    }

    /** Restaura immediatament el terminal compartit abans de tancar l'aplicació. */
    static void restoreNow(Terminal terminal) {
        InteractiveState state;

        synchronized (TerminalSession.class) {
            if (interactiveState == null || interactiveState.terminal() != terminal) {
                return;
            }
            restoreGeneration++;
            openSessions = 0;
            state = detachInteractiveState();
        }

        restore(state);
    }

    private static void scheduleInteractiveRestore(long generation) {
        INTERACTIVE_RESTORE_EXECUTOR.schedule(() -> {
            InteractiveState state;

            synchronized (TerminalSession.class) {
                if (openSessions != 0 || generation != restoreGeneration || interactiveState == null) {
                    return;
                }
                restoreGeneration++;
                state = detachInteractiveState();
            }

            restore(state);
        }, INTERACTIVE_RESTORE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private static InteractiveState detachInteractiveState() {
        InteractiveState state = interactiveState;
        interactiveState = null;
        return state;
    }

    private static void restore(InteractiveState state) {
        if (state == null) {
            return;
        }

        Terminal activeTerminal = state.terminal();
        try {
            activeTerminal.setAttributes(state.originalAttributes());
            activeTerminal.puts(Capability.keypad_local);
            if (state.mouseTrackedByJLine()) {
                activeTerminal.trackMouse(state.originalMouseTracking() == null
                        ? Terminal.MouseTracking.Off
                        : state.originalMouseTracking());
            } else {
                activeTerminal.writer().print(DISABLE_MOUSE_FALLBACK);
            }
            activeTerminal.puts(Capability.cursor_visible);
            activeTerminal.puts(Capability.exit_ca_mode);
            activeTerminal.flush();
        } catch (RuntimeException ignored) {
            // El terminal pot haver-se tancat en sortir de l'aplicació.
        }
    }

    private record InteractiveState(
            Terminal terminal,
            Attributes originalAttributes,
            Terminal.MouseTracking originalMouseTracking,
            boolean mouseTrackedByJLine) {
    }
}
