package rpgcombat.achievements.ui;

import static rpgcombat.utils.ui.Ansi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;

/**
 * Visor interactiu d'assoliments en format de graella amb pàgines horitzontals,
 * filtres i ordenació.
 */
public final class AchievementGridViewer {
    private static final int CARD_WIDTH = 45;
    private static final int CARD_HEIGHT = 9;
    private static final int GAP = 2;
    private static final int HEADER_LINES = 5;
    private static final int FOOTER_LINES = 2;
    private static final int CONTENT_PADDING = 2;

    private AchievementGridViewer() {
    }

    /**
     * Accions disponibles dins del visor.
     */
    private enum Action {
        PREVIOUS_PAGE,
        NEXT_PAGE,
        NEXT_FILTER,
        NEXT_SORT,
        REVERSE_SORT,
        CLEAR_OPTIONS,
        EXIT,
        IGNORE
    }

    /**
     * Filtres disponibles per mostrar només una part dels assoliments.
     */
    private enum AchievementFilter {
        ALL("Tots"),
        COMPLETED("Completats"),
        PENDING("Pendents");

        private final String label;

        AchievementFilter(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public AchievementFilter next() {
            AchievementFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public boolean accepts(Achievement achievement) {
            return switch (this) {
                case ALL -> true;
                case COMPLETED -> achievement.completed();
                case PENDING -> !achievement.completed();
            };
        }
    }

    /**
     * Criteris disponibles per ordenar els assoliments visibles.
     */
    private enum AchievementSort {
        ORIGINAL("Original"),
        NAME("Nom"),
        PROGRESS("Progrés");

        private final String label;

        AchievementSort(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public AchievementSort next() {
            AchievementSort[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /**
     * Estat actual dels controls de filtratge i ordenació.
     */
    private static final class ViewOptions {
        private AchievementFilter filter = AchievementFilter.ALL;
        private AchievementSort sort = AchievementSort.ORIGINAL;
        private boolean reversed = false;

        public void nextFilter() {
            filter = filter.next();
        }

        public void nextSort() {
            sort = sort.next();
        }

        public void reverseSort() {
            reversed = !reversed;
        }

        public void clear() {
            filter = AchievementFilter.ALL;
            sort = AchievementSort.ORIGINAL;
            reversed = false;
        }

        public AchievementFilter filter() {
            return filter;
        }

        public AchievementSort sort() {
            return sort;
        }

        public boolean reversed() {
            return reversed;
        }
    }

    /**
     * Assoliment amb la seva posició original per poder restaurar l'ordre inicial.
     */
    private record IndexedAchievement(int index, Achievement achievement) {
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
            ViewOptions options = new ViewOptions();

            while (true) {
                int width = Math.max(1, terminal.getWidth());
                int height = Math.max(1, terminal.getHeight());

                List<Achievement> visibleAchievements = visibleAchievements(achievements, options);

                int cols = columns(width);
                int rows = visibleRows(height);
                int pageSize = Math.max(1, cols * rows);
                int totalPages = totalPages(visibleAchievements.size(), pageSize);

                page = Math.clamp(page, 0, Math.max(0, totalPages - 1));

                String frame = renderFrame(achievements, visibleAchievements, options, page, width, height);

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
                    case NEXT_FILTER -> {
                        options.nextFilter();
                        page = 0;
                    }
                    case NEXT_SORT -> {
                        options.nextSort();
                        page = 0;
                    }
                    case REVERSE_SORT -> {
                        options.reverseSort();
                        page = 0;
                    }
                    case CLEAR_OPTIONS -> {
                        options.clear();
                        page = 0;
                    }
                    case IGNORE -> {}
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
        map.bind(Action.NEXT_FILTER, "f", "F");
        map.bind(Action.NEXT_SORT, "o", "O");
        map.bind(Action.REVERSE_SORT, "r", "R");
        map.bind(Action.CLEAR_OPTIONS, "c", "C");
        map.bind(Action.EXIT, "q", "Q", "\033");

        bindTerminalKey(map, terminal, Capability.key_left, Action.PREVIOUS_PAGE);
        bindTerminalKey(map, terminal, Capability.key_right, Action.NEXT_PAGE);

        bindTerminalKey(map, terminal, Capability.key_up, Action.IGNORE);
        bindTerminalKey(map, terminal, Capability.key_down, Action.IGNORE);

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
            List<Achievement> allAchievements,
            List<Achievement> visibleAchievements,
            ViewOptions options,
            int page,
            int width,
            int height) {
        StringBuilder out = new StringBuilder();

        out.append(RESET);

        if (width < CARD_WIDTH + 2 || height < HEADER_LINES + CARD_HEIGHT + FOOTER_LINES) {
            appendLine(out, width, RED + BOLD + "El terminal és massa petit per mostrar els assoliments." + RESET);
            appendLine(out, width, DARK_GRAY + "Augmenta la mida de la finestra." + RESET);
            fillRest(out, width, height, 2);
            return out.toString();
        }

        int cols = columns(width);
        int rows = visibleRows(height);
        int pageSize = Math.max(1, cols * rows);
        int totalPages = totalPages(visibleAchievements.size(), pageSize);
        int startIndex = page * pageSize;
        int completed = (int) allAchievements.stream().filter(Achievement::completed).count();

        appendLine(out, width, "");
        appendLine(out, width, BOLD + MAGENTA + "ASSOLIMENTS" + RESET);
        appendLine(out, width, GREEN + "Completats " + completed + "/" + allAchievements.size()
                + DARK_GRAY + " · Filtre: " + options.filter().label()
                + " · Ordre: " + sortLabel(options) + RESET);
        appendLine(out, width, DARK_GRAY + "Pàgina " + (page + 1) + "/" + totalPages
                + " · Mostrats " + visibleAchievements.size() + "/" + allAchievements.size() + RESET);
        appendLine(out, width, "");

        if (visibleAchievements.isEmpty()) {
            appendLine(out, width, YELLOW + "No hi ha cap assoliment que coincideixi amb el filtre actual." + RESET);
            fillRest(out, width, height - FOOTER_LINES, HEADER_LINES + 1);
        } else {
            for (int gridRow = 0; gridRow < rows; gridRow++) {
                String[] lines = new String[CARD_HEIGHT];

                for (int i = 0; i < CARD_HEIGHT; i++)
                    lines[i] = "";

                for (int col = 0; col < cols; col++) {
                    int index = startIndex + gridRow * cols + col;

                    String[] card = index < visibleAchievements.size()
                            ? cardLines(visibleAchievements.get(index))
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

            int usedLines = HEADER_LINES + rows * CARD_HEIGHT;

            fillRest(out, width, height - FOOTER_LINES, usedLines);
        }

        appendLine(out, width, "");
        appendLine(out, width, DARK_GRAY
                + "[←/→ o A/D] canviar pàgina · [F] filtre · [O] ordenar · [R] invertir · [C] netejar · [Q/Esc] sortir"
                + RESET);

        return out.toString();
    }

    /**
     * Retorna els assoliments visibles segons el filtre i l'ordre actuals.
     */
    private static List<Achievement> visibleAchievements(List<Achievement> achievements, ViewOptions options) {
        List<IndexedAchievement> indexed = new ArrayList<>();

        for (int i = 0; i < achievements.size(); i++) {
            Achievement achievement = achievements.get(i);

            if (options.filter().accepts(achievement))
                indexed.add(new IndexedAchievement(i, achievement));
        }

        indexed.sort(comparator(options.sort()));

        if (options.reversed())
            indexed = indexed.reversed();

        return indexed.stream()
                .map(IndexedAchievement::achievement)
                .toList();
    }

    /**
     * Crea el comparador corresponent al criteri d'ordenació indicat.
     */
    private static Comparator<IndexedAchievement> comparator(AchievementSort sort) {
        return switch (sort) {
            case ORIGINAL -> Comparator.comparingInt(IndexedAchievement::index);
            case NAME -> Comparator
                    .comparing((IndexedAchievement item) -> item.achievement().name(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(IndexedAchievement::index);
            case PROGRESS -> Comparator
                    .comparingDouble((IndexedAchievement item) -> progressPercent(item.achievement()))
                    .thenComparing((IndexedAchievement item) -> item.achievement().name(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(IndexedAchievement::index);
        };
    }

    /**
     * Genera el text visible de l'ordre actual, incloent-ne la direcció.
     */
    private static String sortLabel(ViewOptions options) {
        if (options.sort() == AchievementSort.ORIGINAL)
            return options.reversed()
                    ? options.sort().label() + " ↓"
                    : options.sort().label();

        return options.sort().label() + (options.reversed() ? " ↓" : " ↑");
    }

    /**
     * Calcula el percentatge de progrés d'un assoliment.
     */
    private static double progressPercent(Achievement achievement) {
        if (achievement.completed())
            return 100.0;

        if (achievement.goal() <= 0)
            return 0.0;

        return Math.clamp((achievement.progress() * 100.0) / achievement.goal(), 0.0, 100.0);
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
        return Math.max(1, (height - HEADER_LINES - FOOTER_LINES) / CARD_HEIGHT);
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
