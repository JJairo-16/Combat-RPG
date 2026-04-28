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

    private static final long DEFAULT_RELEASE_QUIET_MILLIS = 20;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Terminal terminal;

    private long ignoreInputUntilNanos;
    private long lastIgnoredKeyNanos;
    private long releaseQuietNanos = DEFAULT_RELEASE_QUIET_MILLIS * NANOS_PER_MILLI;
    private boolean waitingForRelease;

    /**
     * Crea un lector d'entrada per al terminal.
     */
    public CinematicInput(Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Reinicia la finestra d'entrada.
     *
     * Durant els primers {@code minimumPlaybackMillis} es descarta qualsevol tecla.
     * A més, si l'usuari ja estava mantenint una tecla premuda, no es llegirà cap
     * acció fins que hi hagi com a mínim {@code releaseQuietMillis} sense entrada.
     */
    public void resetInputGate(long minimumPlaybackMillis, long releaseQuietMillis) throws IOException {
        long now = System.nanoTime();

        this.ignoreInputUntilNanos = now + Math.max(0, minimumPlaybackMillis) * NANOS_PER_MILLI;
        this.releaseQuietNanos = Math.max(1, releaseQuietMillis) * NANOS_PER_MILLI;
        this.lastIgnoredKeyNanos = now;
        this.waitingForRelease = true;

        drainPendingInput(now);
    }

    /**
     * Descarta tota l'entrada que ja estigui al buffer del terminal.
     */
    public void discardPendingInput() throws IOException {
        drainPendingInput(System.nanoTime());
    }

    /**
     * Llegeix una acció si hi ha entrada disponible.
     */
    public Action readActionIfAvailable() throws IOException {
        NonBlockingReader reader = terminal.reader();
        int key = reader.read(1);

        if (key == NonBlockingReader.READ_EXPIRED || key < 0) {
            unlockIfReleased(System.nanoTime());
            return Action.NONE;
        }

        return mapKeyToAction(key);
    }

    /**
     * Llegeix una acció només si la tecla és nova, no mantenida des d'abans.
     */
    public Action readFreshActionIfAvailable() throws IOException {
        NonBlockingReader reader = terminal.reader();
        int key = reader.read(1);
        long now = System.nanoTime();

        if (key == NonBlockingReader.READ_EXPIRED || key < 0) {
            unlockIfReleased(now);
            return Action.NONE;
        }

        if (now < ignoreInputUntilNanos || waitingForRelease) {
            lastIgnoredKeyNanos = now;
            return Action.NONE;
        }

        return mapKeyToAction(key);
    }

    private void drainPendingInput(long now) throws IOException {
        NonBlockingReader reader = terminal.reader();

        while (true) {
            int key = reader.read(1);

            if (key == NonBlockingReader.READ_EXPIRED || key < 0) {
                return;
            }

            lastIgnoredKeyNanos = now;
        }
    }

    private void unlockIfReleased(long now) {
        if (!waitingForRelease) {
            return;
        }

        if (now >= ignoreInputUntilNanos && now - lastIgnoredKeyNanos >= releaseQuietNanos) {
            waitingForRelease = false;
        }
    }

    private Action mapKeyToAction(int key) {
        if (key == SPACE) {
            return Action.SPACE;
        }

        if (key == ENTER || key == LF) {
            return Action.ENTER;
        }

        return Action.NONE;
    }
}
