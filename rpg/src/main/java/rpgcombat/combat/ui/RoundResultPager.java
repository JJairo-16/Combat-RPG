package rpgcombat.combat.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import rpgcombat.combat.models.CombatRoundResult;
import rpgcombat.combat.models.CombatantStatus;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.combat.ui.messages.CombatMessageFormatter;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.Ansi;

/**
 * Mostra el resultat d'una ronda en dues pàgines.
 */
public final class RoundResultPager {
    private static final int KEY_ESC = 27;
    private static final int KEY_SPACE = 32;
    private static final int KEY_A_LOWER = 'a';
    private static final int KEY_A_UPPER = 'A';
    private static final int KEY_D_LOWER = 'd';
    private static final int KEY_D_UPPER = 'D';
    private static final int KEY_LEFT = 1_001;
    private static final int KEY_RIGHT = 1_002;

    private static final int ESC_TIMEOUT_MS = 35;
    private static final int QUIET_MS = 90;
    private static final int MAX_RELEASE_WAIT_MS = 800;
    private static final int WIDTH = CombatRenderer.DIV_WIDTH;

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

            if (isPrevious(key)) {
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
        terminal.writer().print("\033[H\033[2J\033[3J");
        terminal.writer().print(Ansi.DARK_GRAY + "═".repeat(WIDTH) + Ansi.RESET + "\n");
        terminal.writer().printf("%s%s%s %sPàgina %d/%d%s%n",
                Ansi.BOLD, page.title(), Ansi.RESET,
                Ansi.DARK_GRAY, index + 1, pages.size(), Ansi.RESET);
        terminal.writer().print(Ansi.DARK_GRAY + "═".repeat(WIDTH) + Ansi.RESET + "\n\n");
        terminal.writer().print(page.body());
        terminal.writer().print("\n");
        terminal.writer().print(Ansi.DARK_GRAY + "─".repeat(WIDTH) + Ansi.RESET + "\n");
        terminal.writer().print(controls(index == pages.size() - 1));
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
            if (key == KEY_SPACE || isPrevious(key) || isNext(key)) {
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
        StringBuilder sb = new StringBuilder();
        renderer.appendTurnResult(sb, turn);
        List<String> parsed = Arrays.stream(sb.toString().split("\\R", -1)).toList();
        lines.addAll(parsed.stream().filter(line -> !line.isBlank()).toList());
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
        return Ansi.DARK_GRAY + "└" + "─".repeat(WIDTH - 2) + "┘" + Ansi.RESET;
    }

    private void appendBoxLine(StringBuilder sb, String line) {
        String safe = line == null ? "" : line;
        int contentWidth = WIDTH - 4;
        int padding = Math.max(0, contentWidth - visibleLength(safe));
        sb.append(Ansi.DARK_GRAY).append("│ ").append(Ansi.RESET)
                .append(safe)
                .append(" ".repeat(padding))
                .append(Ansi.DARK_GRAY).append(" │").append(Ansi.RESET)
                .append("\n");
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
                + Ansi.BOLD + action + Ansi.RESET + "\n";
    }

    private void printFallback(List<Page> pages) {
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            System.out.println(Ansi.DARK_GRAY + "═".repeat(WIDTH) + Ansi.RESET);
            System.out.printf("%s%s%s %sPàgina %d/%d%s%n",
                    Ansi.BOLD, page.title(), Ansi.RESET,
                    Ansi.DARK_GRAY, i + 1, pages.size(), Ansi.RESET);
            System.out.println(Ansi.DARK_GRAY + "═".repeat(WIDTH) + Ansi.RESET);
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
        return CombatMessageFormatter.stripAnsi(text).length();
    }

    private record Page(String title, String body) {
    }
}
