package rpgcombat.utils.rng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpgcombat.utils.ui.ColorGradient;

/**
 * Utilitat per animar el llançament d'un d20 a la terminal.
 */
public final class D20Terminal {

    private D20Terminal() {}

    private static final int FACES = 20;

    private static final String ANSI_HIDE_CURSOR = "\033[?25l";
    private static final String ANSI_SHOW_CURSOR = "\033[?25h";
    private static final String ANSI_CLEAR_LINE = "\033[2K";
    private static final String ANSI_MOVE_UP = "\033[1A";
    private static final String ANSI_CR = "\r";

    private static final String TITLE = "Tirant d20";
    private static final int NUMBER_WIDTH = 10;

    private static List<String> preloadedFrames;

   /** Carrega manualment els frames si encara no existeixen. */
    public static void preloadFrames() {
        ensureFramesLoaded();
    }

   /** Inicia l'animació amb un RNG per defecte. */
    public static void animateDie(int finalResult) throws InterruptedException {
        animateDie(finalResult, new Random());
    }

    /**
     * Anima el dau fins al resultat final indicat.
     *
     * @param finalResult resultat final (1–20)
     * @param rng generador aleatori
     */
    public static void animateDie(int finalResult, Random rng) throws InterruptedException {
        if (finalResult < 1 || finalResult > FACES) {
            throw new IllegalArgumentException("El resultat ha d'estar entre 1 i 20");
        }

        List<String> frames = getFrames();

        int current = rng.nextInt(FACES) + 1;
        int previousLines = 0;

        final long durationMs = 1900L;
        final long start = System.currentTimeMillis();

        System.out.print(ANSI_HIDE_CURSOR);
        System.out.flush();

        try {
            while (true) {
                long now = System.currentTimeMillis();
                long elapsed = now - start;
                double t = Math.clamp((double) elapsed / durationMs, 0.0, 1.0);

                if (t >= 1.0) {
                    break;
                }

                current = nextAnimatedValue(current, t, rng);

                String frame = frames.get(current - 1);
                clearBlock(previousLines);
                printBlock(frame);
                previousLines = countLines(frame);

                Thread.sleep(calculateDelay(t));
            }

            // Fase final amb més inèrcia visual
            while (current != finalResult) {
                current = advance(current, 1);

                String frame = frames.get(current - 1);
                clearBlock(previousLines);
                printBlock(frame);
                previousLines = countLines(frame);

                Thread.sleep(calculateFinishingDelay(current, finalResult));
            }

            String finalFrame = frames.get(finalResult - 1);
            clearBlock(previousLines);
            printBlock(finalFrame);
            System.out.println();

        } finally {
            System.out.print(ANSI_SHOW_CURSOR);
            System.out.flush();
        }
    }

   /** Retorna els frames, carregant-los en el primer ús si cal. */
    private static List<String> getFrames() {
        ensureFramesLoaded();
        return preloadedFrames;
    }

   /** Garanteix la càrrega lazy dels frames. */
    private static void ensureFramesLoaded() {
        if (preloadedFrames != null) {
            return;
        }

        synchronized (D20Terminal.class) {
            if (preloadedFrames == null) {
                preloadedFrames = buildPreloadedFrames();
            }
        }
    }

   /** Precalcula tots els frames del dau. */
    private static List<String> buildPreloadedFrames() {
        List<String> frames = new ArrayList<>(FACES);

        for (int value = 1; value <= FACES; value++) {
            String colorPrefix = getNumberColorPrefix(value);
            frames.add(buildFrame(value, colorPrefix));
        }

        return List.copyOf(frames);
    }

    /**
     * Retorna el prefix de color del número.
     *
     * <p>Actualment no aplica cap color.</p>
     */
    private static String getNumberColorPrefix(int value) {
        double percent = (value - 1) / 19.0;
        return ColorGradient.getColor(percent);
    }

   /** Calcula el següent valor animat segons la fase temporal. */
    private static int nextAnimatedValue(int current, double t, Random rng) {
        if (t < 0.22) {
            return advance(current, randomBetween(rng, 3, 6));
        }
        if (t < 0.48) {
            return advance(current, randomBetween(rng, 2, 4));
        }
        if (t < 0.72) {
            return advance(current, randomBetween(rng, 1, 3));
        }
        if (t < 0.88) {
            return advance(current, rng.nextDouble() < 0.75 ? 1 : 2);
        }
        return advance(current, 1);
    }

   /** Calcula el retard entre frames segons el progrés. */
    private static long calculateDelay(double t) {
        if (t < 0.18) return 18L;
        if (t < 0.34) return 28L;
        if (t < 0.50) return 42L;
        if (t < 0.66) return 62L;
        if (t < 0.80) return 88L;
        if (t < 0.90) return 118L;
        return 150L;
    }

   /** Calcula el retard final en funció de la distància al resultat. */
    private static long calculateFinishingDelay(int current, int target) {
        int distance = circularDistance(current, target);

        return switch (distance) {
            case 0 -> 0L;
            case 1 -> 185L;
            case 2 -> 150L;
            case 3 -> 120L;
            case 4 -> 95L;
            default -> 78L;
        };
    }

   /** Avança el valor circularment dins del rang del dau. */
    private static int advance(int value, int steps) {
        return ((value - 1 + steps) % FACES) + 1;
    }

   /** Distància circular entre dos valors del dau. */
    private static int circularDistance(int from, int to) {
        return (to - from + FACES) % FACES;
    }

   /** Genera un enter aleatori dins d'un rang. */
    private static int randomBetween(Random rng, int min, int max) {
        return rng.nextInt(max - min + 1) + min;
    }

   /** Construeix el frame de text a mostrar. */
    private static String buildFrame(int value, String colorPrefix) {
        String number = colorPrefix + "==" + String.format("%2d", value) + "==" + ColorGradient.RESET;
        return TITLE + "\n" + center(number, NUMBER_WIDTH);
    }

   /** Centra un text dins d'un ample fix. */
    private static String center(String text, int width) {
        if (text.length() >= width) {
            return text;
        }

        int leftSpaces = (width - text.length()) / 2;
        int rightSpaces = width - text.length() - leftSpaces;
        return " ".repeat(leftSpaces) + text + " ".repeat(rightSpaces);
    }

   /** Imprimeix un bloc a la terminal. */
    private static void printBlock(String block) {
        System.out.print(block);
        System.out.flush();
    }

   /** Esborra un bloc anterior de la terminal. */
    private static void clearBlock(int lines) {
        if (lines <= 0) {
            return;
        }

        for (int i = 1; i < lines; i++) {
            System.out.print(ANSI_MOVE_UP);
        }

        for (int i = 0; i < lines; i++) {
            System.out.print(ANSI_CR);
            System.out.print(ANSI_CLEAR_LINE);
            if (i < lines - 1) {
                System.out.print("\n");
            }
        }

        for (int i = 1; i < lines; i++) {
            System.out.print(ANSI_MOVE_UP);
        }

        System.out.print(ANSI_CR);
        System.out.flush();
    }

   /** Compta les línies d'un text. */
    private static int countLines(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}