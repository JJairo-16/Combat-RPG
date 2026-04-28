package rpgcombat.utils.interactive.helpers;

import java.io.IOException;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

/**
 * Porta d'entrada per a menús interactius.
 * Evita llegir input fins que el menú és visible i l'usuari ha deixat de prémer tecles.
 */
public class MenuInputGate {
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Terminal terminal;
    private final long minimumVisibleMillis;
    private final long releaseQuietMillis;

    /**
     * Crea una porta amb valors per defecte.
     * @param terminal terminal activa
     */
    public MenuInputGate(Terminal terminal) {
        this(terminal, 80, 20);
    }

    /**
     * Crea una porta amb valors personalitzats.
     * @param terminal terminal activa
     * @param minimumVisibleMillis temps mínim que el menú ha d'estar visible (ms)
     * @param releaseQuietMillis temps sense input necessari per considerar-lo alliberat (ms)
     */
    public MenuInputGate(Terminal terminal, long minimumVisibleMillis, long releaseQuietMillis) {
        this.terminal = terminal;
        this.minimumVisibleMillis = Math.max(0, minimumVisibleMillis);
        this.releaseQuietMillis = Math.max(1, releaseQuietMillis);
    }

    /**
     * Espera fins que el menú és visible i no hi ha tecles en curs.
     * @throws IOException si hi ha un error de lectura del terminal
     */
    public void waitUntilReady() throws IOException {
        NonBlockingReader reader = terminal.reader();

        long now = System.nanoTime();
        long ignoreUntil = now + minimumVisibleMillis * NANOS_PER_MILLI;
        long quietNanos = releaseQuietMillis * NANOS_PER_MILLI;
        long lastInputNanos = now;

        while (true) {
            int key = reader.read(1);
            now = System.nanoTime();

            if (key != NonBlockingReader.READ_EXPIRED && key >= 0) {
                lastInputNanos = now;
                continue;
            }

            boolean menuWasVisibleLongEnough = now >= ignoreUntil;
            boolean userReleasedInput = now - lastInputNanos >= quietNanos;

            if (menuWasVisibleLongEnough && userReleasedInput) {
                return;
            }
        }
    }
}