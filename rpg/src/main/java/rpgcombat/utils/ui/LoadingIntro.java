package rpgcombat.utils.ui;

import java.io.PrintStream;

import static rpgcombat.utils.ui.Ansi.*;

public class LoadingIntro {
    private final long frameMillis;
    private final String[] loadingFrames;
    private final PrintStream out;
    private final String madeByText;
    private final long minDisplayTime;

    /** Seqüència ANSI per amagar el cursor. */
    private static final String HIDE_CURSOR = "\u001B[?25l";

    /** Seqüència ANSI per mostrar el cursor. */
    private static final String SHOW_CURSOR = "\u001B[?25h";

    public LoadingIntro(long frameMillis, String[] loadingFrames, String creator, long minDisplayTime) {
        if (frameMillis <= 0)
            throw new IllegalArgumentException("frameMillis ha de ser major que 0");

        if (minDisplayTime < 0)
            throw new IllegalArgumentException("minDisplayTime no pot ser negatiu");

        if (loadingFrames == null || loadingFrames.length == 0)
            throw new IllegalArgumentException("loadingFrames no pot ser null o buit");

        if (creator == null || creator.isBlank())
            throw new IllegalArgumentException("creator no pot ser null o buit");

        String[] copy = new String[loadingFrames.length];
        System.arraycopy(loadingFrames, 0, copy, 0, loadingFrames.length);

        this.frameMillis = frameMillis;
        this.loadingFrames = copy;
        this.out = System.out;
        this.madeByText = "Creat per " + creator;
        this.minDisplayTime = minDisplayTime;
    }

    public LoadingIntro(String creator) {
        this(300, new String[] {
                "Carregant   ",
                "Carregant.  ",
                "Carregant.. ",
                "Carregant..."
        }, creator, 0);
    }

    public void start(Runnable task) {
        if (task == null)
            throw new IllegalArgumentException("task no pot ser null");

        new Cleaner().clear();
        hideCursor();

        Thread worker = new Thread(task, "loading-intro-task");
        worker.start();

        int i = 0;
        long startTime = System.currentTimeMillis();

        try {
            while (true) {
                long elapsed = System.currentTimeMillis() - startTime;
                boolean minTimeReached = elapsed >= minDisplayTime;
                boolean workerFinished = !worker.isAlive();

                if (workerFinished && minTimeReached) {
                    break;
                }

                out.print("\r" + CYAN + loadingFrames[i++ % loadingFrames.length] + RESET);
                out.flush();

                Thread.sleep(frameMillis);
            }

            worker.join();

            clearLine();
            showCreator();
            clearLine();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            showCursor();
        }
    }

    /** Mostra el text de l'autor amb animació. */
    private void showCreator() throws InterruptedException {
        if (madeByText == null || madeByText.isBlank()) {
            return;
        }

        int width = visibleLength(madeByText);

        for (int i = 1; i <= madeByText.length(); i++) {
            out.print("\r" + MAGENTA + BOLD + madeByText.substring(0, i) + RESET);
            out.flush();
            Thread.sleep(60);
        }

        Thread.sleep(1200);

        for (int i = madeByText.length(); i >= 0; i--) {
            String visible = madeByText.substring(0, i);
            out.print("\r" + MAGENTA + BOLD + padRight(visible, width) + RESET);
            out.flush();
            Thread.sleep(45);
        }
    }

    /** Neteja la línia actual. */
    private void clearLine() {
        int width = Math.max(loadingMaxWidth(), visibleLength(madeByText)) + 4;
        out.print("\r" + " ".repeat(width) + "\r");
        out.flush();
    }

    /** Amaga el cursor del terminal. */
    private void hideCursor() {
        out.print(HIDE_CURSOR);
        out.flush();
    }

    /** Torna a mostrar el cursor del terminal. */
    private void showCursor() {
        out.print(SHOW_CURSOR);
        out.flush();
    }

    private int loadingMaxWidth() {
        int max = 0;
        for (String frame : loadingFrames) {
            if (frame != null && frame.length() > max) {
                max = frame.length();
            }
        }
        return max;
    }

    private int visibleLength(String text) {
        return text == null ? 0 : text.length();
    }

    private String padRight(String text, int width) {
        int missing = width - visibleLength(text);
        if (missing <= 0) {
            return text;
        }
        return text + " ".repeat(missing);
    }

    public static void main(String[] args) {
        LoadingIntro intro = new LoadingIntro(
                300,
                new String[] {
                        "Carregant   ",
                        "Carregant.  ",
                        "Carregant.. ",
                        "Carregant..."
                },
                "Jairo Linares",
                5000
        );

        intro.start(() -> {
            try {
                Thread.sleep(1200);
                Thread.sleep(900);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("Programa iniciat.");
    }
}