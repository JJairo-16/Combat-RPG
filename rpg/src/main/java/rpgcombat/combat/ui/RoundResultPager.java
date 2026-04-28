package rpgcombat.combat.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import rpgcombat.combat.models.CombatRoundResult;
import rpgcombat.combat.models.CombatantStatus;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.Ansi;

/**
 * Mostra el resultat d'una ronda en dues pàgines.
 */
public final class RoundResultPager {
    private static final int KEY_ESC = 27;
    private static final int KEY_ENTER = 10;
    private static final int KEY_CARRIAGE_RETURN = 13;
    private static final int KEY_SPACE = 32;
    private static final int KEY_A_LOWER = 'a';
    private static final int KEY_A_UPPER = 'A';
    private static final int KEY_D_LOWER = 'd';
    private static final int KEY_D_UPPER = 'D';
    private static final int KEY_LEFT = 1_001;
    private static final int KEY_RIGHT = 1_002;

    private static final int ESC_TIMEOUT_MS = 35;
    private static final int QUIET_MS = 35;
    private static final int MAX_RELEASE_WAIT_MS = 250;
    private static final int WIDTH = CombatRenderer.DIV_WIDTH;
    private static final int CONTENT_WIDTH = WIDTH - 4;
    private static final String BIG_DIV = Ansi.DARK_GRAY + "═".repeat(WIDTH) + Ansi.RESET;
    private static final String THIN_DIV = Ansi.DARK_GRAY + "─".repeat(WIDTH) + Ansi.RESET;
    private static final String BLOCK_BOTTOM = Ansi.DARK_GRAY + "└" + "─".repeat(WIDTH - 2) + "┘" + Ansi.RESET;

    private final CombatRenderer renderer;

    /**
     * Crea el visor.
     */
    public RoundResultPager(CombatRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Mostra el resultat paginat.
     */
    public void show(int roundNumber, Character player1, Character player2, CombatRoundResult round) {
        List<Page> pages = buildPages(roundNumber, player1, player2, round);
        if (pages.isEmpty()) {
            return;
        }

        try (TerminalSession session = SharedTerminal.openSession()) {
            run(session.terminal(), pages);
        } catch (IOException e) {
            printFallback(pages);
        }
    }

    private void run(Terminal terminal, List<Page> pages) throws IOException {
        NonBlockingReader reader = terminal.reader();
        int currentPage = 0;

        waitForQuiet(reader);
        while (true) {
            render(terminal, pages, currentPage);
            int key = readNavigationKey(reader);
            int lastPage = pages.size() - 1;

            if (isEnter(key)) {
                break;
            } else if (isPrevious(key)) {
                currentPage = Math.max(0, currentPage - 1);
            } else if (isNext(key)) {
                if (currentPage < lastPage) {
                    currentPage++;
                }
            } else if (key == KEY_SPACE) {
                if (currentPage == lastPage) {
                    break;
                }
                currentPage++;
            }

            waitForQuiet(reader);
        }
    }

    private void render(Terminal terminal, List<Page> pages, int index) {
        Page page = pages.get(index);
        StringBuilder screen = new StringBuilder(page.body().length() + 320);
        screen.append("\033[H\033[2J\033[3J");
        screen.append(BIG_DIV).append('\n');
        screen.append(Ansi.BOLD).append(page.title()).append(Ansi.RESET).append(' ')
                .append(Ansi.DARK_GRAY).append("Pàgina ").append(index + 1).append('/')
                .append(pages.size()).append(Ansi.RESET).append('\n');
        screen.append(BIG_DIV).append("\n\n");
        screen.append(page.body()).append('\n');
        screen.append(THIN_DIV).append('\n');
        screen.append(controls(index == pages.size() - 1));

        terminal.writer().print(screen);
        terminal.writer().flush();
    }

    private int readNavigationKey(NonBlockingReader reader) throws IOException {
        while (true) {
            int key = reader.read();
            if (key == KEY_ESC) {
                int arrow = readEscapeKey(reader);
                if (arrow != 0) {
                    return arrow;
                }
                continue;
            }
            if (isEnter(key) || key == KEY_SPACE || isPrevious(key) || isNext(key)) {
                return key;
            }
        }
    }

    private int readEscapeKey(NonBlockingReader reader) throws IOException {
        StringBuilder sequence = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int next = reader.read(ESC_TIMEOUT_MS);
            if (next == NonBlockingReader.READ_EXPIRED) {
                break;
            }
            sequence.append((char) next);
        }

        String text = sequence.toString();
        if (text.contains("[D") || text.contains("OD")) {
            return KEY_LEFT;
        }
        if (text.contains("[C") || text.contains("OC")) {
            return KEY_RIGHT;
        }
        return 0;
    }

    private void waitForQuiet(NonBlockingReader reader) throws IOException {
        long end = System.currentTimeMillis() + MAX_RELEASE_WAIT_MS;
        while (System.currentTimeMillis() < end) {
            int next = reader.read(QUIET_MS);
            if (next == NonBlockingReader.READ_EXPIRED) {
                return;
            }
        }
        drain(reader, 50);
    }

    private void drain(NonBlockingReader reader, int millis) throws IOException {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            reader.read(1);
        }
    }

    private boolean isEnter(int key) {
        return key == KEY_ENTER || key == KEY_CARRIAGE_RETURN;
    }

    private boolean isPrevious(int key) {
        return key == KEY_LEFT || key == KEY_A_LOWER || key == KEY_A_UPPER;
    }

    private boolean isNext(int key) {
        return key == KEY_RIGHT || key == KEY_D_LOWER || key == KEY_D_UPPER;
    }

    private List<Page> buildPages(int roundNumber, Character player1, Character player2, CombatRoundResult round) {
        return List.of(
                new Page("RONDA " + roundNumber + " — COMBAT", combatPage(round)),
                new Page("RONDA " + roundNumber + " — ESTAT", statusPage(player1, player2, round))
        );
    }

    private String combatPage(CombatRoundResult round) {
        StringBuilder sb = new StringBuilder();
        appendBlock(sb, "Intercanvi", turnLines(round));
        appendBlock(sb, "Resultat del dany", List.of(
                damageLine(round, true),
                damageLine(round, false)
        ));
        if (round.winner() != Winner.NONE) {
            appendBlock(sb, "Final del combat", List.of(winnerText(round.winner())));
        }
        return sb.toString();
    }

    private String statusPage(Character player1, Character player2, CombatRoundResult round) {
        StringBuilder sb = new StringBuilder();
        appendStatusBlock(sb, "Després del dany",
                statusOr(round.p1AfterDamage(), player1), statusOr(round.p2AfterDamage(), player2));

        if (round.winner() == Winner.NONE) {
            appendBlock(sb, "Regeneració", List.of(
                    regenLine(player1.getName(), round.p1Regen().healthRecovered(), round.p1Regen().manaRecovered()),
                    regenLine(player2.getName(), round.p2Regen().healthRecovered(), round.p2Regen().manaRecovered())
            ));
        }

        appendStatusBlock(sb, "Estat final",
                statusOr(round.p1Final(), player1), statusOr(round.p2Final(), player2));
        return sb.toString();
    }

    private List<String> turnLines(CombatRoundResult round) {
        List<String> lines = new ArrayList<>();
        appendTurn(lines, round.firstTurn());
        lines.add("");
        appendTurn(lines, round.secondTurn());
        return lines;
    }

    private void appendTurn(List<String> lines, TurnResult turn) {
        StringBuilder sb = new StringBuilder(256);
        renderer.appendTurnResult(sb, turn);
        appendNonBlankLines(lines, sb);
    }

    private void appendNonBlankLines(List<String> lines, StringBuilder source) {
        int start = 0;
        int length = source.length();
        for (int i = 0; i <= length; i++) {
            if (i == length || source.charAt(i) == '\n' || source.charAt(i) == '\r') {
                if (i > start) {
                    String line = source.substring(start, i);
                    if (!line.isBlank()) {
                        lines.add(line);
                    }
                }
                if (i + 1 < length && source.charAt(i) == '\r' && source.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
    }

    private void appendStatusBlock(StringBuilder sb, String title, CombatantStatus p1, CombatantStatus p2) {
        List<String> lines = new ArrayList<>();
        appendStatus(lines, p1);
        lines.add("");
        appendStatus(lines, p2);
        appendBlock(sb, title, lines);
    }

    private void appendStatus(List<String> lines, CombatantStatus status) {
        lines.add(Ansi.BOLD + status.name() + Ansi.RESET);
        lines.addAll(renderer.statusLines(status));
    }

    private void appendBlock(StringBuilder sb, String title, List<String> lines) {
        sb.append(blockTop(title)).append("\n");
        for (String line : lines) {
            appendBoxLine(sb, line);
        }
        sb.append(blockBottom()).append("\n\n");
    }

    private String blockTop(String title) {
        String label = " " + title + " ";
        int fill = Math.max(1, WIDTH - visibleLength(label) - 2);
        return Ansi.DARK_GRAY + "┌" + Ansi.RESET
                + Ansi.BOLD + label + Ansi.RESET
                + Ansi.DARK_GRAY + "─".repeat(fill) + "┐" + Ansi.RESET;
    }

    private String blockBottom() {
        return BLOCK_BOTTOM;
    }

    private void appendBoxLine(StringBuilder sb, String line) {
        for (String wrappedLine : wrapBoxLine(line, CONTENT_WIDTH)) {
            int padding = Math.max(0, CONTENT_WIDTH - visibleLength(wrappedLine));
            sb.append(Ansi.DARK_GRAY).append("│ ").append(Ansi.RESET)
                    .append(wrappedLine)
                    .append(Ansi.RESET)
                    .append(" ".repeat(padding))
                    .append(Ansi.DARK_GRAY).append(" │").append(Ansi.RESET)
                    .append("\n");
        }
    }

    private List<String> wrapBoxLine(String line, int width) {
        String safe = line == null ? "" : line;
        if (visibleLength(safe) <= width) {
            return List.of(safe);
        }

        List<String> wrapped = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int visible = 0;
        int lastSpaceRaw = -1;
        int index = 0;

        while (index < safe.length()) {
            int ansiEnd = ansiSequenceEnd(safe, index);
            if (ansiEnd > index) {
                current.append(safe, index, ansiEnd);
                index = ansiEnd;
                continue;
            }

            char ch = safe.charAt(index++);
            current.append(ch);
            visible++;

            if (java.lang.Character.isWhitespace(ch)) {
                lastSpaceRaw = current.length();
            }

            if (visible >= width && hasVisibleText(safe, index)) {
                if (lastSpaceRaw > 0) {
                    wrapped.add(trimTrailingSpaces(current.substring(0, lastSpaceRaw)));
                    current = new StringBuilder(trimLeadingSpaces(current.substring(lastSpaceRaw)));
                    visible = visibleLength(current.toString());
                    lastSpaceRaw = lastWhitespacePosition(current.toString());
                } else {
                    wrapped.add(current.toString());
                    current.setLength(0);
                    visible = 0;
                    lastSpaceRaw = -1;
                }
            }
        }

        if (!current.isEmpty() || wrapped.isEmpty()) {
            wrapped.add(trimTrailingSpaces(current.toString()));
        }
        return wrapped;
    }

    private int ansiSequenceEnd(String text, int start) {
        if (start >= text.length() || text.charAt(start) != '\u001B') {
            return start;
        }
        int index = start + 1;
        if (index < text.length() && text.charAt(index) == '[') {
            index++;
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch >= '@' && ch <= '~') {
                    return index;
                }
            }
        }
        return start + 1;
    }

    private boolean hasVisibleText(String text, int start) {
        int index = start;
        while (index < text.length()) {
            int ansiEnd = ansiSequenceEnd(text, index);
            if (ansiEnd > index) {
                index = ansiEnd;
            } else if (!java.lang.Character.isWhitespace(text.charAt(index++))) {
                return true;
            }
        }
        return false;
    }

    private int lastWhitespacePosition(String text) {
        int last = -1;
        int index = 0;
        while (index < text.length()) {
            int ansiEnd = ansiSequenceEnd(text, index);
            if (ansiEnd > index) {
                index = ansiEnd;
            } else {
                if (java.lang.Character.isWhitespace(text.charAt(index))) {
                    last = index + 1;
                }
                index++;
            }
        }
        return last;
    }

    private String trimLeadingSpaces(String text) {
        int index = 0;
        while (index < text.length() && java.lang.Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return text.substring(index);
    }

    private String trimTrailingSpaces(String text) {
        int index = text.length();
        while (index > 0 && java.lang.Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return text.substring(0, index);
    }

    private CombatantStatus statusOr(CombatantStatus status, Character fallback) {
        return status != null ? status : CombatantStatus.from(fallback);
    }

    private String damageLine(CombatRoundResult round, boolean player1) {
        String name = player1 ? nameOf(round.p1AfterDamage(), "Jugador 1") : nameOf(round.p2AfterDamage(), "Jugador 2");
        double damage = player1 ? round.p1DamageTaken() : round.p2DamageTaken();
        return Ansi.BOLD + name + Ansi.RESET + " ha rebut "
                + Ansi.BRIGHT_RED + renderer.format(Math.max(0, damage)) + Ansi.RESET + " de dany.";
    }

    private String regenLine(String name, double health, double mana) {
        return Ansi.BOLD + name + Ansi.RESET + " recupera "
                + Ansi.GREEN + "+" + renderer.format(Math.max(0, health)) + Ansi.RESET + " vida i "
                + Ansi.BRIGHT_BLUE + "+" + renderer.format(Math.max(0, mana)) + Ansi.RESET + " mana.";
    }

    private String nameOf(CombatantStatus status, String fallback) {
        return status != null && status.name() != null ? status.name() : fallback;
    }

    private String controls(boolean lastPage) {
        String action = lastPage ? "[ESPAI] Sortir" : "[ESPAI] Següent";
        return Ansi.BOLD + "[←/→ o A/D]" + Ansi.RESET + " Navegar     "
                + Ansi.BOLD + action + Ansi.RESET + "     "
                + Ansi.BOLD + "[ENTER] Saltar pàgines" + Ansi.RESET + "\n";
    }

    private void printFallback(List<Page> pages) {
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            System.out.println(BIG_DIV);
            System.out.printf("%s%s%s %sPàgina %d/%d%s%n",
                    Ansi.BOLD, page.title(), Ansi.RESET,
                    Ansi.DARK_GRAY, i + 1, pages.size(), Ansi.RESET);
            System.out.println(BIG_DIV);
            System.out.println(page.body());
        }
    }

    private String winnerText(Winner winner) {
        return switch (winner) {
            case PLAYER1 -> "Guanya el jugador 1.";
            case PLAYER2 -> "Guanya el jugador 2.";
            case TIE -> "Empat.";
            default -> "Sense guanyador.";
        };
    }

    private int visibleLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int visible = 0;
        int index = 0;
        while (index < text.length()) {
            int ansiEnd = ansiSequenceEnd(text, index);
            if (ansiEnd > index) {
                index = ansiEnd;
            } else {
                visible++;
                index++;
            }
        }
        return visible;
    }

    private record Page(String title, String body) {
    }
}
