package rpgcombat.achievements;

import static rpgcombat.utils.ui.Ansi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;

/**
 * Visor interactiu d'assoliments en format de graella amb pàgines horitzontals.
 */
public final class AchievementGridViewer {
    private static final int CARD_WIDTH = 45;
    private static final int CARD_HEIGHT = 9;
    private static final int GAP = 2;
    private static final int HEADER_LINES = 4;
    private static final int CONTENT_PADDING = 2;

    private AchievementGridViewer() {
    }

    /**
     * Accions disponibles dins del visor.
     */
    private enum Action {
        PREVIOUS_PAGE, NEXT_PAGE, EXIT
    }

    /**
     * Mostra una graella de prova amb assoliments en diferents estats.
     */
    public static void showDemo() {
        show(List.of(
                new Achievement("Primer cop", "Fes el teu primer atac.", 1, 1, true, true),
                new Achievement("Aprenent arcà", "Llança 5 habilitats màgiques.", 3, 5, true, true),
                new Achievement("Cap perfecte", "Derrota un cap sense rebre cap mena de dany durant tot el combat.", 0,
                        1, false, false),
                new Achievement("Col·leccionista", "Aconsegueix 10 objectes diferents al llarg de l'aventura.", 7, 10,
                        true, true),
                new Achievement("Ombra silenciosa", "Completa una missió sense alertar cap enemic.", 1, 1, false, true),
                new Achievement("Resistent", "Sobreviu a un combat amb molt poca vida restant.", 0, 1, true, false),
                new Achievement("Campió", "Guanya 25 combats.", 25, 25, true, true),
                new Achievement("Explorador", "Visita 8 zones diferents del món.", 4, 8, true, true),
                new Achievement("Destí ocult", "Descobreix una ruta secreta.", 0, 1, false, false),
                new Achievement("Mestre d'armes", "Fes servir 6 tipus d'arma diferents.", 2, 6, true, true),
                new Achievement("Imparable", "Guanya 5 combats seguits sense perdre cap torn important.", 5, 5, true,
                        true),
                new Achievement("Relíquia perduda", "Troba una relíquia antiga amagada en una zona opcional.", 0, 1,
                        false, false)));
    }

    /**
     * Mostra el visor interactiu d'assoliments.
     *
     * @param achievements llista d'assoliments que es vol mostrar
     */
    public static void show(List<Achievement> achievements) {
        if (achievements == null || achievements.isEmpty())
            return;

        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();
            BindingReader reader = new BindingReader(terminal.reader());
            KeyMap<Action> keys = keys(terminal);

            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.cursor_invisible);
            terminal.writer().print("\033[2J\033[H");
            terminal.flush();

            int page = 0;
            String lastFrame = "";

            while (true) {
                int width = Math.max(1, terminal.getWidth());
                int height = Math.max(1, terminal.getHeight());

                int cols = columns(width);
                int rows = visibleRows(height);
                int pageSize = Math.max(1, cols * rows);
                int totalPages = totalPages(achievements.size(), pageSize);

                page = Math.clamp(page, 0, Math.max(0, totalPages - 1));

                String frame = renderFrame(achievements, page, width, height);

                if (!frame.equals(lastFrame)) {
                    terminal.writer().print("\033[H");
                    terminal.writer().print(frame);
                    terminal.flush();
                    lastFrame = frame;
                }

                Action action = reader.readBinding(keys);
                if (action == null)
                    continue;

                switch (action) {
                    case PREVIOUS_PAGE -> page--;
                    case NEXT_PAGE -> page++;
                    case EXIT -> {
                        terminal.writer().print(RESET);
                        terminal.puts(Capability.exit_ca_mode);
                        terminal.puts(Capability.cursor_visible);
                        terminal.flush();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No s'ha pogut obrir el visor d'assoliments: " + e.getMessage());
        }
    }

    /**
     * Crea el mapa de tecles del visor.
     *
     * @param terminal terminal actual
     * @return mapa de tecles configurat
     */
    private static KeyMap<Action> keys(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.PREVIOUS_PAGE, "a", "A");
        map.bind(Action.NEXT_PAGE, "d", "D");
        map.bind(Action.EXIT, "q", "Q", "\033");

        bindTerminalKey(map, terminal, Capability.key_left, Action.PREVIOUS_PAGE);
        bindTerminalKey(map, terminal, Capability.key_right, Action.NEXT_PAGE);

        return map;
    }

    /**
     * Vincula una tecla especial del terminal a una acció.
     */
    private static void bindTerminalKey(
            KeyMap<Action> map,
            Terminal terminal,
            Capability capability,
            Action action) {
        String key = KeyMap.key(terminal, capability);

        if (key != null)
            map.bind(action, key);
    }

    /**
     * Genera el fotograma complet del visor.
     */
    private static String renderFrame(
            List<Achievement> achievements,
            int page,
            int width,
            int height) {
        StringBuilder out = new StringBuilder();

        out.append(RESET);
        out.append("\033[H");

        if (width < CARD_WIDTH + 2 || height < HEADER_LINES + CARD_HEIGHT) {
            appendLine(out, width, RED + BOLD + "El terminal és massa petit per mostrar els assoliments." + RESET);
            appendLine(out, width, DARK_GRAY + "Augmenta la mida de la finestra." + RESET);
            fillRest(out, width, height, 2);
            return out.toString();
        }

        int cols = columns(width);
        int rows = visibleRows(height);
        int pageSize = Math.max(1, cols * rows);
        int totalPages = totalPages(achievements.size(), pageSize);
        int startIndex = page * pageSize;

        appendLine(out, width, BOLD + MAGENTA + "VISOR D'ASSOLIMENTS" + RESET);
        appendLine(out, width, DARK_GRAY + "Fletxes esquerra/dreta o A/D: canviar pàgina · Q/Esc: sortir" + RESET);
        appendLine(out, width, "");

        for (int gridRow = 0; gridRow < rows; gridRow++) {
            String[] lines = new String[CARD_HEIGHT];

            for (int i = 0; i < CARD_HEIGHT; i++)
                lines[i] = "";

            for (int col = 0; col < cols; col++) {
                int index = startIndex + gridRow * cols + col;

                String[] card = index < achievements.size()
                        ? cardLines(achievements.get(index))
                        : emptyCard();

                for (int line = 0; line < CARD_HEIGHT; line++) {
                    lines[line] += card[line];

                    if (col < cols - 1)
                        lines[line] += " ".repeat(GAP);
                }
            }

            for (String line : lines)
                appendLine(out, width, line);
        }

        appendLine(out, width, "");
        appendLine(out, width, DARK_GRAY + "Pàgina " + (page + 1) + "/" + Math.max(1, totalPages) + RESET);

        fillRest(out, width, height, HEADER_LINES + rows * CARD_HEIGHT + 2);

        return out.toString();
    }

    /**
     * Genera les línies visuals d'una targeta d'assoliment.
     */
    private static String[] cardLines(Achievement achievement) {
        boolean completed = achievement.completed();
        boolean hidden = achievement.hidden();

        Status achievementStatus = Status.getStatus(completed, hidden);

        String border = achievementStatus.borderColor;
        String titleColor = achievementStatus.titleColor;
        String textColor = hidden ? DARK_GRAY : WHITE;
        String progressColor = achievementStatus.progressColor;

        String name = completed || achievement.showNameBeforeComplete() ? achievement.name() : "???";
        String description = completed || achievement.showDescriptionBeforeComplete() ? achievement.description()
                : "Assoliment ocult";

        int innerWidth = CARD_WIDTH - 2;
        int paddedWidth = innerWidth - CONTENT_PADDING * 2;

        List<String> descriptionLines = wrap(description, paddedWidth, 2);

        String progress = completed || achievement.showDescriptionBeforeComplete()
                ? progressBar(achievement, paddedWidth)
                : "";

        String status = achievementStatus.statusText;

        return new String[] {
                border + "┌" + "─".repeat(CARD_WIDTH - 2) + "┐" + RESET,
                border + "│" + RESET + titleColor + paddedCell(name, innerWidth) + RESET + border + "│" + RESET,
                border + "│" + RESET + textColor + paddedCell(descriptionLines.get(0), innerWidth) + RESET + border
                        + "│" + RESET,
                border + "│" + RESET + textColor + paddedCell(descriptionLines.get(1), innerWidth) + RESET + border
                        + "│" + RESET,
                border + "│" + RESET + fit("", innerWidth) + border + "│" + RESET,
                border + "│" + RESET + progressColor + paddedCell(progress, innerWidth) + RESET + border + "│" + RESET,
                border + "│" + RESET + fit("", innerWidth) + border + "│" + RESET,
                border + "│" + RESET + progressColor + paddedCell(status, innerWidth) + RESET + border + "│" + RESET,
                border + "└" + "─".repeat(CARD_WIDTH - 2) + "┘" + RESET
        };
    }

    /**
     * Genera una targeta buida per mantenir l'alineació de la graella.
     */
    private static String[] emptyCard() {
        String blank = " ".repeat(CARD_WIDTH);

        return new String[] {
                blank, blank, blank, blank, blank, blank, blank, blank, blank
        };
    }

    /**
     * Divideix un text en diverses línies segons una amplada màxima.
     */
    private static List<String> wrap(String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        String safe = text == null ? "" : text.trim();

        while (!safe.isEmpty() && lines.size() < maxLines) {
            if (safe.length() <= width) {
                lines.add(safe);
                break;
            }

            int cut = safe.lastIndexOf(' ', width);

            if (cut <= 0)
                cut = width;

            String line = safe.substring(0, cut).trim();
            safe = safe.substring(Math.min(cut, safe.length())).trim();

            if (lines.size() == maxLines - 1 && !safe.isEmpty())
                line = ellipsis(line, width);

            lines.add(line);
        }

        while (lines.size() < maxLines)
            lines.add("");

        return lines;
    }

    /**
     * Afegeix punts suspensius a un text sense superar l'amplada indicada.
     */
    private static String ellipsis(String text, int width) {
        if (text == null || width <= 0)
            return "";

        return text.length() <= width - 1
                ? text + "…"
                : text.substring(0, Math.max(0, width - 1)) + "…";
    }

    /**
     * Genera una barra de progrés textual.
     */
    private static String progressBar(Achievement achievement, int width) {
        if (achievement.goal() <= 0)
            return "—";

        int percent = achievement.completed()
                ? 100
                : Math.clamp((int) Math.round((achievement.progress() * 100.0) / achievement.goal()), 0, 100);

        int barWidth = Math.max(6, width - 6);
        int filled = (int) Math.round(barWidth * (percent / 100.0));

        return "█".repeat(filled)
                + "░".repeat(Math.max(0, barWidth - filled))
                + " " + percent + "%";
    }

    /**
     * Calcula quantes columnes caben al terminal.
     */
    private static int columns(int width) {
        return Math.max(1, (width + GAP) / (CARD_WIDTH + GAP));
    }

    /**
     * Calcula quantes files visibles caben al terminal.
     */
    private static int visibleRows(int height) {
        return Math.max(1, (height - HEADER_LINES - 2) / CARD_HEIGHT);
    }

    /**
     * Calcula el nombre total de pàgines.
     */
    private static int totalPages(int totalItems, int pageSize) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    /**
     * Ajusta un text a una amplada concreta.
     */
    private static String fit(String text, int width) {
        String safe = text == null ? "" : text;

        return safe.length() > width
                ? safe.substring(0, Math.max(0, width - 1)) + "…"
                : safe + " ".repeat(Math.max(0, width - safe.length()));
    }

    /**
     * Ajusta un text aplicant marge intern horitzontal.
     */
    private static String paddedCell(String text, int width) {
        int contentWidth = Math.max(0, width - CONTENT_PADDING * 2);

        return " ".repeat(CONTENT_PADDING)
                + fit(text, contentWidth)
                + " ".repeat(CONTENT_PADDING);
    }

    /**
     * Afegeix una línia al fotograma i neteja la resta de la línia.
     */
    private static void appendLine(StringBuilder out, int width, String text) {
        out.append(text).append("\033[K");

        int visible = visibleLength(text);

        if (visible < width)
            out.append(" ".repeat(width - visible));

        out.append("\n");
    }

    /**
     * Calcula la longitud visible d'un text, ignorant seqüències ANSI.
     */
    private static int visibleLength(String text) {
        int length = 0;
        boolean escape = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (escape) {
                if (ch == 'm')
                    escape = false;
            } else if (ch == '\u001b') {
                escape = true;
            } else {
                length++;
            }
        }

        return length;
    }

    /**
     * Omple la resta de la pantalla amb línies buides.
     */
    private static void fillRest(StringBuilder out, int width, int height, int usedLines) {
        for (int i = usedLines; i < height; i++)
            appendLine(out, width, "");
    }

    private enum Status {
        COMPLETED(GREEN, GREEN + BOLD, GREEN, "Completat"),
        HIDDEN(DARK_GRAY, DARK_GRAY + BOLD, DARK_GRAY, "Bloquejat"),
        DEFAULT(WHITE, WHITE + BOLD, YELLOW, "En progrés");

        public final String borderColor;
        public final String titleColor;
        public final String progressColor;
        public final String statusText;

        private Status(String borderColor, String titleColor, String progressColor, String statusText) {
            this.borderColor = borderColor;
            this.titleColor = titleColor;
            this.progressColor = progressColor;
            this.statusText = statusText;
        }

        public static Status getStatus(boolean completed, boolean hidden) {
            if (completed)
                return COMPLETED;

            if (hidden)
                return HIDDEN;

            return DEFAULT;
        }
    }

    public static void main(String[] args) {
        showDemo();
    }
}