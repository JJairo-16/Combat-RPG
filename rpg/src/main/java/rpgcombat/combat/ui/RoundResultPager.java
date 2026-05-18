package rpgcombat.combat.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import rpgcombat.combat.models.CombatRoundResult;
import rpgcombat.combat.models.CombatantStatus;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.CombatMessageFormatter;
import rpgcombat.combat.ui.messages.CombatMessagePhase;
import rpgcombat.combat.ui.messages.CombatMessageKind;
import rpgcombat.combat.ui.messages.CombatMessagePlacement;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalInput;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.utils.ui.TerminalClear;

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
    private static final int KEY_S_LOWER = 's';
    private static final int KEY_S_UPPER = 'S';
    private static final int KEY_W_LOWER = 'w';
    private static final int KEY_W_UPPER = 'W';
    private static final int KEY_LEFT = 1_001;
    private static final int KEY_RIGHT = 1_002;
    private static final int KEY_UP = 1_003;
    private static final int KEY_DOWN = 1_004;

    private static final int ESC_TIMEOUT_MS = 35;
    private static final int QUIET_MS = 35;
    private static final int MAX_RELEASE_WAIT_MS = 250;
    private static final int WIDTH = CombatRenderer.DIV_WIDTH;
    private static final int CONTENT_WIDTH = WIDTH - 4;
    private static final String BIG_DIV = Ansi.DARK_GRAY + "═".repeat(WIDTH) + Ansi.RESET;
    private static final String THIN_DIV = Ansi.DARK_GRAY + "─".repeat(WIDTH) + Ansi.RESET;
    private static final String BLOCK_BOTTOM = Ansi.DARK_GRAY + "└" + "─".repeat(WIDTH - 2) + "┘" + Ansi.RESET;

    private static final int SIDE_BLOCK_GAP = 1;
    private static final int PLAYER_BLOCK_WIDTH = 70;
    private static final int EFFECT_BLOCK_WIDTH = WIDTH - PLAYER_BLOCK_WIDTH - SIDE_BLOCK_GAP;
    private static final int SCROLL_STEP = 1;

    private final CombatRenderer renderer;
    private final CombatMessageFormatter messageFormatter = new CombatMessageFormatter();

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
        int[] scrollOffsets = new int[pages.size()];

        waitForQuiet(reader);
        while (true) {
            int maxScroll = maxScroll(terminal, pages.get(currentPage));
            scrollOffsets[currentPage] = Math.clamp(scrollOffsets[currentPage], 0, maxScroll);
            render(terminal, pages, currentPage, scrollOffsets[currentPage], maxScroll);
            int key = readNavigationKey(reader);
            int lastPage = pages.size() - 1;

            if (isEnter(key)) {
                break;
            } else if (isScrollUp(key)) {
                scrollOffsets[currentPage] = Math.max(0, scrollOffsets[currentPage] - SCROLL_STEP);
            } else if (isScrollDown(key)) {
                scrollOffsets[currentPage] = Math.min(maxScroll, scrollOffsets[currentPage] + SCROLL_STEP);
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

            if (!isScrollUp(key) && !isScrollDown(key)) {
                waitForQuiet(reader);
            }
        }
    }

    private void render(Terminal terminal, List<Page> pages, int index, int scrollOffset, int maxScroll) {
        Page page = pages.get(index);
        StringBuilder screen = new StringBuilder(page.body().length() + 320);
        List<String> bodyLines = bodyLines(page);
        int bodyHeight = bodyViewportHeight(terminal);
        int from = Math.clamp(scrollOffset, 0, maxScroll);
        int to = Math.min(bodyLines.size(), from + bodyHeight);

        repaintFromTop(terminal);
        screen.append(BIG_DIV).append('\n');
        screen.append(Ansi.BOLD).append(page.title()).append(Ansi.RESET).append(' ')
                .append(Ansi.DARK_GRAY).append("Pàgina ").append(index + 1).append('/')
                .append(pages.size()).append(Ansi.RESET).append('\n');
        screen.append(BIG_DIV).append("\n\n");
        for (int i = from; i < to; i++) {
            screen.append(bodyLines.get(i)).append('\n');
        }
        screen.append('\n');
        screen.append(THIN_DIV).append('\n');
        screen.append(controls(index == pages.size() - 1, maxScroll > 0, from, bodyLines.size()));

        terminal.writer().print(screen);
        terminal.writer().flush();
    }

    private void repaintFromTop(Terminal terminal) {
        if (terminal == null) {
            TerminalClear.clear(null);
            return;
        }
        terminal.writer().print("\033[H\033[J");
    }

    private int maxScroll(Terminal terminal, Page page) {
        return Math.max(0, bodyLines(page).size() - bodyViewportHeight(terminal));
    }

    private int bodyViewportHeight(Terminal terminal) {
        int height = terminal == null ? 24 : terminal.getHeight();
        return Math.max(1, height - 7);
    }

    private List<String> bodyLines(Page page) {
        String body = page == null || page.body() == null ? "" : page.body();
        return List.of(body.split("\\R", -1));
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
            if (isEnter(key) || key == KEY_SPACE || isPrevious(key) || isNext(key)
                    || isScrollUp(key) || isScrollDown(key)) {
                return key;
            }
        }
    }

    private int readEscapeKey(NonBlockingReader reader) throws IOException {
        int first = reader.read(ESC_TIMEOUT_MS);
        if (first == NonBlockingReader.READ_EXPIRED) {
            return 0;
        }
        int second = reader.read(ESC_TIMEOUT_MS);
        if (second == NonBlockingReader.READ_EXPIRED) {
            return 0;
        }

        String mousePrefix = TerminalInput.mousePrefixAfterEscape(first, second);
        if (mousePrefix != null) {
            drainMouseRemainder(reader, mousePrefix);
            return 0;
        }

        StringBuilder sequence = new StringBuilder();
        sequence.append((char) first).append((char) second);
        int immediateArrow = arrowFromEscapeSequence(sequence.toString());
        if (immediateArrow != 0) {
            return immediateArrow;
        }
        for (int i = 0; i < 4; i++) {
            int next = reader.read(ESC_TIMEOUT_MS);
            if (next == NonBlockingReader.READ_EXPIRED) {
                break;
            }
            sequence.append((char) next);
            int arrow = arrowFromEscapeSequence(sequence.toString());
            if (arrow != 0) {
                return arrow;
            }
        }

        return arrowFromEscapeSequence(sequence.toString());
    }

    private int arrowFromEscapeSequence(String text) {
        if (text.contains("[D") || text.contains("OD")) {
            return KEY_LEFT;
        }
        if (text.contains("[C") || text.contains("OC")) {
            return KEY_RIGHT;
        }
        if (text.contains("[A") || text.contains("OA")) {
            return KEY_UP;
        }
        if (text.contains("[B") || text.contains("OB")) {
            return KEY_DOWN;
        }
        return 0;
    }

    private void drainMouseRemainder(NonBlockingReader reader, String mousePrefix) throws IOException {
        if ("\033[M".equals(mousePrefix)) {
            for (int i = 0; i < 3; i++) {
                reader.read(ESC_TIMEOUT_MS);
            }
            return;
        }
        for (int i = 0; i < 32; i++) {
            int next = reader.read(ESC_TIMEOUT_MS);
            if (next == NonBlockingReader.READ_EXPIRED || next == 'M' || next == 'm') {
                return;
            }
        }
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

    private boolean isScrollUp(int key) {
        return key == KEY_UP || key == KEY_W_LOWER || key == KEY_W_UPPER;
    }

    private boolean isScrollDown(int key) {
        return key == KEY_DOWN || key == KEY_S_LOWER || key == KEY_S_UPPER;
    }

    private List<Page> buildPages(int roundNumber, Character player1, Character player2, CombatRoundResult round) {
        return List.of(
                new Page("RONDA " + roundNumber + " — COMBAT", combatPage(player1, player2, round)),
                new Page("RONDA " + roundNumber + " — ESTAT", statusPage(player1, player2, round)));
    }

    private String combatPage(Character player1, Character player2, CombatRoundResult round) {
        StringBuilder sb = new StringBuilder();
        appendPlayerTurnRow(sb, player1, turnOf(round, player1, true));
        appendPlayerTurnRow(sb, player2, turnOf(round, player2, false));
        appendBlock(sb, "Resultat del dany", List.of(
                damageLine(round, true),
                damageLine(round, false)));
        if (round.winner() != Winner.NONE) {
            appendBlock(sb, "Final del combat", List.of(winnerText(round.winner())));
        }
        return sb.toString();
    }

    private TurnResult turnOf(CombatRoundResult round, Character player, boolean firstFallback) {
        if (round == null || player == null) {
            return null;
        }
        if (matchesActor(round.firstTurn(), player)) {
            return round.firstTurn();
        }
        if (matchesActor(round.secondTurn(), player)) {
            return round.secondTurn();
        }
        return firstFallback ? round.firstTurn() : round.secondTurn();
    }

    private boolean matchesActor(TurnResult turn, Character player) {
        if (turn == null || player == null) {
            return false;
        }
        if (turn.actor() != null) {
            return turn.actor() == player;
        }
        String name = player.getName();
        return name != null && name.equals(turn.actorName());
    }

    private void appendPlayerTurnRow(StringBuilder sb, Character player, TurnResult turn) {
        Objects.requireNonNull(player, "El jugador no pot ser nul.");

        String name = turn != null && turn.actorName() != null
                ? turn.actorName()
                : player.getName();

        List<String> main = playerMainLines(turn);
        PanelContent panel = panelContent(turn);

        List<String> left = sideBlockLines(name, main, PLAYER_BLOCK_WIDTH);

        if (panel.lines().isEmpty()) {
            for (String leftLine : left) {
                sb.append(leftLine).append('\n');
            }
            sb.append('\n');
            return;
        }

        List<String> right = effectBlockLines(panel.title(), panel.lines(), EFFECT_BLOCK_WIDTH);
        int rows = Math.max(left.size(), right.size());

        for (int i = 0; i < rows; i++) {
            String leftLine = i < left.size() ? left.get(i) : emptyBoxPadding(PLAYER_BLOCK_WIDTH);
            String rightLine = i < right.size() ? right.get(i) : emptyBoxPadding(EFFECT_BLOCK_WIDTH);
            sb.append(leftLine).append(" ".repeat(SIDE_BLOCK_GAP)).append(rightLine).append('\n');
        }

        sb.append('\n');
    }

    private List<String> playerMainLines(TurnResult turn) {
        Objects.requireNonNull(turn, "El torn no pot ser nul");

        List<String> lines = new ArrayList<>();
        appendSection(lines, "ABANS",
                messageLines(messagesOf(turn, CombatMessagePhase.BEFORE_CROSS, CombatMessagePlacement.MAIN_TIMELINE)));

        List<String> during = new ArrayList<>();
        String actorLine = messageFormatter.attacker(turn.attackerMessage());
        if (actorLine == null && hasAnyMainMessage(turn)) {
            actorLine = messageFormatter.attacker(turn.actorName());
        }
        if (actorLine != null) {
            during.add(actorLine);
        }
        during.addAll(
                messageLines(messagesOf(turn, CombatMessagePhase.DURING_CROSS, CombatMessagePlacement.MAIN_TIMELINE)));
        String defenseLine = messageFormatter.defense(turn.defenseMessage());
        if (defenseLine != null) {
            during.add(defenseLine);
        }
        appendSection(lines, "DURANT", during);

        appendSection(lines, "DESPRÉS",
                messageLines(messagesOf(turn, CombatMessagePhase.AFTER_CROSS, CombatMessagePlacement.MAIN_TIMELINE)));
        return lines;
    }

    private void appendSection(List<String> lines, String title, List<String> sectionLines) {
        if (sectionLines == null || sectionLines.isEmpty()) {
            return;
        }
        if (!lines.isEmpty()) {
            lines.add("");
        }
        lines.add(Ansi.BOLD + title + Ansi.RESET);
        lines.addAll(sectionLines);
    }

    private List<String> messageLines(List<CombatMessage> messages) {
        return messageFormatter.effects(messages);
    }

    private List<CombatMessage> messagesOf(
            TurnResult turn,
            CombatMessagePhase phase,
            CombatMessagePlacement placement) {

        List<CombatMessage> result = new ArrayList<>();
        for (CombatMessage message : allMessages(turn)) {
            if (message.phase() == phase && message.placement() == placement) {
                result.add(message);
            }
        }
        return result;
    }

    private PanelContent panelContent(TurnResult turn) {
        List<CombatMessage> statusMessages = new ArrayList<>();
        List<CombatMessage> modeMessages = new ArrayList<>();

        for (CombatMessage message : panelMessages(turn)) {
            if (message.kind() == CombatMessageKind.GAMEMODE) {
                modeMessages.add(message);
            } else {
                statusMessages.add(message);
            }
        }

        if (modeMessages.isEmpty()) {
            return new PanelContent("Efectes", messageFormatter.effects(statusMessages));
        }
        if (statusMessages.isEmpty()) {
            return new PanelContent("Mode", messageFormatter.effects(modeMessages));
        }

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(Ansi.BOLD + "EFECTES" + Ansi.RESET);
        lines.addAll(messageFormatter.effects(statusMessages));
        lines.add("");
        lines.add(Ansi.BOLD + "MODE" + Ansi.RESET);
        lines.addAll(messageFormatter.effects(modeMessages));
        return new PanelContent("Estat i mode", lines);
    }

    private List<CombatMessage> panelMessages(TurnResult turn) {
        List<CombatMessage> result = new ArrayList<>();
        for (CombatMessage message : allMessages(turn)) {
            if (message.placement() == CombatMessagePlacement.EFFECT_PANEL) {
                result.add(message);
            }
        }
        return result;
    }

    private List<CombatMessage> allMessages(TurnResult turn) {
        List<CombatMessage> messages = new ArrayList<>();
        if (turn == null) {
            return messages;
        }
        addAll(messages, turn.startMessages());
        addAll(messages, turn.preDefenseMessages());
        addAll(messages, turn.postDefenseMessages());
        addAll(messages, turn.endTurnMessages());
        return messages;
    }

    private void addAll(List<CombatMessage> target, List<CombatMessage> source) {
        if (source != null) {
            target.addAll(source);
        }
    }

    private boolean hasAnyMainMessage(TurnResult turn) {
        return !messagesOf(turn, CombatMessagePhase.BEFORE_CROSS, CombatMessagePlacement.MAIN_TIMELINE).isEmpty()
                || !messagesOf(turn, CombatMessagePhase.DURING_CROSS, CombatMessagePlacement.MAIN_TIMELINE).isEmpty()
                || !messagesOf(turn, CombatMessagePhase.AFTER_CROSS, CombatMessagePlacement.MAIN_TIMELINE).isEmpty();
    }

    private List<String> sideBlockLines(String title, List<String> lines, int width) {
        List<String> result = new ArrayList<>();
        result.add(blockTop(title, width));
        for (String line : lines) {
            for (String wrappedLine : wrapBoxLine(line, width - 4)) {
                result.add(boxLine(wrappedLine, width));
            }
        }
        result.add(blockBottom(width));
        return result;
    }

    private List<String> effectBlockLines(String title, List<String> lines, int width) {
        List<String> result = new ArrayList<>();
        result.add(blockTop(title, width));
        for (String line : lines) {
            for (String wrappedLine : wrapBoxLine(line, width - 3)) {
                result.add(effectBoxLine(wrappedLine, width));
            }
        }
        result.add(blockBottom(width));
        return result;
    }

    private String boxLine(String line, int width) {
        int contentWidth = width - 4;
        int padding = Math.max(0, contentWidth - visibleLength(line));
        return Ansi.DARK_GRAY + "│ " + Ansi.RESET
                + line
                + Ansi.RESET
                + " ".repeat(padding)
                + Ansi.DARK_GRAY + " │" + Ansi.RESET;
    }

    private String effectBoxLine(String line, int width) {
        int contentWidth = width - 3;
        int padding = Math.max(0, contentWidth - visibleLength(line));
        return Ansi.DARK_GRAY + "│" + Ansi.RESET
                + line
                + Ansi.RESET
                + " ".repeat(padding)
                + Ansi.DARK_GRAY + " │" + Ansi.RESET;
    }

    private String emptyBoxPadding(int width) {
        return " ".repeat(width);
    }

    private String statusPage(Character player1, Character player2, CombatRoundResult round) {
        StringBuilder sb = new StringBuilder();
        appendStatusBlock(sb, "Després del dany",
                statusOr(round.p1AfterDamage(), player1), statusOr(round.p2AfterDamage(), player2));

        if (round.winner() == Winner.NONE) {
            appendBlock(sb, "Regeneració", List.of(
                    regenLine(player1.getName(), round.p1Regen().healthRecovered(), round.p1Regen().manaRecovered()),
                    regenLine(player2.getName(), round.p2Regen().healthRecovered(), round.p2Regen().manaRecovered())));
        }

        appendStatusBlock(sb, "Estat final",
                statusOr(round.p1Final(), player1), statusOr(round.p2Final(), player2));
        return sb.toString();
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

    private String blockTop(String title, int width) {
        String label = " " + title + " ";
        int fill = Math.max(1, width - visibleLength(label) - 2);
        return Ansi.DARK_GRAY + "┌" + Ansi.RESET
                + Ansi.BOLD + label + Ansi.RESET
                + Ansi.DARK_GRAY + "─".repeat(fill) + "┐" + Ansi.RESET;
    }

    private String blockBottom(int width) {
        return Ansi.DARK_GRAY + "└" + "─".repeat(width - 2) + "┘" + Ansi.RESET;
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
        String continuationIndent = continuationIndent(safe);
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
                    current = new StringBuilder(continuationIndent)
                            .append(trimLeadingSpaces(current.substring(lastSpaceRaw)));
                    visible = visibleLength(current.toString());
                    lastSpaceRaw = lastWhitespacePosition(current.toString());
                } else {
                    wrapped.add(current.toString());
                    current = new StringBuilder(continuationIndent);
                    visible = visibleLength(continuationIndent);
                    lastSpaceRaw = -1;
                }
            }
        }

        if (!current.isEmpty() || wrapped.isEmpty()) {
            wrapped.add(trimTrailingSpaces(current.toString()));
        }
        return wrapped;
    }

    private String continuationIndent(String text) {
        String visible = CombatMessageFormatter.stripAnsi(text);
        if (visible.isBlank()) {
            return "";
        }

        int index = 0;
        while (index < visible.length() && java.lang.Character.isWhitespace(visible.charAt(index))) {
            index++;
        }

        if (index < visible.length()) {
            int afterGlyph = index + 1;
            if (afterGlyph < visible.length() && java.lang.Character.isWhitespace(visible.charAt(afterGlyph))) {
                return " ".repeat(afterGlyph + 1) + leadingAnsiStyle(text);
            }
        }

        return " ".repeat(index) + leadingAnsiStyle(text);
    }

    private String leadingAnsiStyle(String text) {
        int index = 0;
        while (index < text.length()) {
            int ansiEnd = ansiSequenceEnd(text, index);
            if (ansiEnd > index) {
                String sequence = text.substring(index, ansiEnd);
                if (!Ansi.RESET.equals(sequence)) {
                    return sequence;
                }
                index = ansiEnd;
                continue;
            }
            if (!java.lang.Character.isWhitespace(text.charAt(index))) {
                return "";
            }
            index++;
        }
        return "";
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

    private String controls(boolean lastPage, boolean scrollable, int firstVisibleLine, int totalLines) {
        String action = lastPage ? "[ESPAI] Sortir" : "[ESPAI] Següent";
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.BOLD).append("[←/→ o A/D]").append(Ansi.RESET).append(" Navegar     ")
                .append(Ansi.BOLD).append(action).append(Ansi.RESET).append("     ")
                .append(Ansi.BOLD).append("[ENTER] Saltar pàgines").append(Ansi.RESET);
        if (scrollable) {
            sb.append("     ")
                    .append(Ansi.BOLD).append("[↑/↓ o W/S]").append(Ansi.RESET)
                    .append(" Scroll ")
                    .append(Ansi.DARK_GRAY).append(firstVisibleLine + 1).append('/').append(totalLines).append(Ansi.RESET);
        }
        return sb.append('\n').toString();
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

    private record PanelContent(String title, List<String> lines) {
    }
}
