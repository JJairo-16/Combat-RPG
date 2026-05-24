package rpgcombat.utils.interactive;

import static rpgcombat.utils.ui.Ansi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;

/**
 * Menú d'accions que afegeix panells independents per consultar missions,
 * perks i pistes de terreny sense acoblar-los al menú lateral d'informació.
 */
public final class PlayerProgressMenu extends MenuWithInformation {
    private static final int PANEL_WIDTH = 48;
    private static final int PANEL_MAX_LINES = 20;
    private static final int TERRAIN_HINT_MAX_LINES = 5;
    private static final int PANEL_CONTROLS_GAP_ROWS = 1;
    private static final int PANEL_STACK_GAP_ROWS = 1;
    private static final int BOTTOM_SAFE_MARGIN_ROWS = 2;
    private static final int INFO_PANEL_SAFE_ROWS = 10;

    private static final Pattern SECTION_SEPARATOR_PATTERN = Pattern.compile("\\R---\\R");
    private static final Pattern LINE_PATTERN = Pattern.compile("\\R");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern FORMAT_NUMBERS_PATTERN = Pattern.compile("(?<!\\d)(\\d+)\\.0(?!\\d)");
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*m");

    private String progressText = "";
    private String terrainHintText = "";
    private int selectedSection;

    private int lastPanelRow = -1;
    private int lastPanelHeight;
    private int lastPanelCol;
    private int lastPanelWidth;
    private int lastTerrainPanelRow = -1;
    private int lastTerrainPanelHeight;
    private int lastTerrainPanelCol;
    private int lastTerrainPanelWidth;

    private int completedAchievementsBadgeCount;

    public PlayerProgressMenu(Map<String, String> information) {
        super(information);
    }

    /** Defineix el text de missions/perks que es mostra al panell propi. */
    public void setProgressText(String text) {
        this.progressText = formatNumbers(safe(text));
    }

    /** Defineix la pista de terreny que es mostra en un panell propi. */
    public void setTerrainHintText(String text) {
        this.terrainHintText = formatNumbers(safe(text));
    }

    public void setCompletedAchievementsBadgeCount(int count) {
        this.completedAchievementsBadgeCount = Math.max(0, count);
    }

    @Override
    protected KeyMap<Action> buildKeyMap(Terminal terminal) {
        KeyMap<Action> map = super.buildKeyMap(terminal);
        map.bind(Action.PROGRESS, "p", "P");
        return map;
    }

    /** Canvia entre missions i perks sense tocar MenuCenter. */
    @Override
    protected void handleProgressAction(Terminal terminal, String title, List<String> options, int cursor) {
        List<ProgressSection> sections = sections();
        if (sections.size() > 1) {
            selectedSection = (selectedSection + 1) % sections.size();
        } else {
            super.handleProgressAction(terminal, title, options, cursor);
            return;
        }
        int controlsRow = terminal.getHeight() - BOTTOM_SAFE_MARGIN_ROWS;
        clearPreviousTerrainHintPanel(terminal);
        drawProgressPanel(terminal, controlsRow);
        drawTerrainHintPanel(terminal, controlsRow);
        terminal.flush();
    }

    /** Recol·loca la pista quan s'obre o es tanca la informació d'accions. */
    @Override
    protected void handleInfoAction(Terminal terminal, String title, List<String> options, int cursor) {
        super.handleInfoAction(terminal, title, options, cursor);
        drawTerrainHintPanel(terminal, terminal.getHeight() - BOTTOM_SAFE_MARGIN_ROWS);
        terminal.flush();
    }

    @Override
    protected void afterContentRendered(Terminal terminal, List<String> options, int cursor, int controlsRow) {
        drawProgressPanel(terminal, controlsRow);
        drawTerrainHintPanel(terminal, controlsRow);
        drawAchievementBadge(terminal);
    }

    @Override
    protected void beforeDynamicAreaCleared(Terminal terminal) {
        clearPreviousPanel(terminal);
        clearPreviousTerrainHintPanel(terminal);
    }

    private void drawProgressPanel(Terminal terminal, int controlsRow) {
        clearPreviousPanel(terminal);

        List<ProgressSection> sections = sections();
        if (sections.isEmpty()) {
            return;
        }

        selectedSection = Math.clamp(selectedSection, 0, sections.size() - 1);
        ProgressSection section = sections.get(selectedSection);

        int width = Math.clamp(terminal.getWidth() - leftPadding - 2L, 24, PANEL_WIDTH);
        int innerWidth = width - 4;

        List<PanelLine> lines = limitLines(buildLines(section.body(), innerWidth), PANEL_MAX_LINES, width);

        int contentHeight = lines.size() + 2;
        int row = Math.max(TITLE_ROW + 1, controlsRow - contentHeight - PANEL_CONTROLS_GAP_ROWS);
        int col = Math.max(leftPadding, terminal.getWidth() - width);

        lastPanelRow = row;
        lastPanelHeight = contentHeight;
        lastPanelCol = col;
        lastPanelWidth = width;

        String suffix = sections.size() > 1 ? " [P]" : "";
        drawPanelHeader(terminal, row++, col, width, section.title() + suffix);
        drawPanelLines(terminal, row, col, innerWidth, lines);
    }

    /** Dibuixa la pista de terreny en un apartat compacte separat del progrés. */
    private void drawTerrainHintPanel(Terminal terminal, int controlsRow) {
        clearPreviousTerrainHintPanel(terminal);

        String text = safe(terrainHintText).trim();
        if (text.isBlank()) {
            return;
        }

        int width = Math.clamp(terminal.getWidth() - leftPadding - 2L, 24, PANEL_WIDTH);
        int innerWidth = width - 4;
        int anchorRow = lastPanelRow >= 0 ? lastPanelRow : controlsRow;
        int minRow = getInformationVisible() ? TITLE_ROW + INFO_PANEL_SAFE_ROWS : TITLE_ROW + 1;
        int availableRows = anchorRow - PANEL_STACK_GAP_ROWS - minRow;
        if (availableRows < 3) {
            return;
        }

        int lineLimit = Math.min(TERRAIN_HINT_MAX_LINES, availableRows - 2);
        List<PanelLine> lines = limitLines(buildLines(text, innerWidth), lineLimit, width);
        int contentHeight = lines.size() + 2;
        int row = Math.max(minRow, anchorRow - contentHeight - PANEL_STACK_GAP_ROWS);
        int col = Math.max(leftPadding, terminal.getWidth() - width);

        lastTerrainPanelRow = row;
        lastTerrainPanelHeight = contentHeight;
        lastTerrainPanelCol = col;
        lastTerrainPanelWidth = width;

        drawPanelHeader(terminal, row++, col, width, "Pista del terreny");
        drawPanelLines(terminal, row, col, innerWidth, lines);
    }

    private List<ProgressSection> sections() {
        String text = safe(progressText).trim();
        if (text.isBlank()) {
            return List.of();
        }

        String[] raw = SECTION_SEPARATOR_PATTERN.split(text);
        List<ProgressSection> result = new ArrayList<>();

        for (String block : raw) {
            String[] parts = LINE_PATTERN.split(block.trim(), 2);
            if (parts.length == 0 || parts[0].isBlank()) {
                continue;
            }

            result.add(new ProgressSection(parts[0].trim(), parts.length > 1 ? parts[1].trim() : ""));
        }

        return result;
    }

    /**
     * Converteix el cos d'una secció en línies renderitzables.
     * Mesura l'amplada visible ignorant codis ANSI, perquè els colors no trenquin
     * el wrap ni el retall del panell.
     */
    private List<PanelLine> buildLines(String text, int width) {
        List<PanelLine> result = new ArrayList<>();
        String[] rawLines = LINE_PATTERN.split(safe(text), -1);

        int lineInBlock = 0;

        for (int i = 0; i < rawLines.length; i++) {
            String lineText = rawLines[i].trim();

            if (lineText.isBlank()) {
                continue;
            }

            String style = switch (lineInBlock) {
                case 0 -> BOLD;
                case 1 -> DARK_GRAY;
                default -> "";
            };

            for (String wrappedLine : wrapAnsiAware(lineText, width)) {
                result.add(new PanelLine(wrappedLine, style));
            }

            boolean closesBlock = lineText.startsWith("Progrés:")
                    || lineText.startsWith("Activació:")
                    || lineText.startsWith("Tipus:");

            if (closesBlock) {
                lineInBlock = 0;

                if (hasMoreContent(rawLines, i + 1)) {
                    result.add(new PanelLine("", ""));
                }
            } else {
                lineInBlock++;
            }
        }

        return result.isEmpty() ? List.of(new PanelLine("", "")) : result;
    }

    /** Limita les línies d'un panell i marca visualment el text retallat. */
    private List<PanelLine> limitLines(List<PanelLine> lines, int maxLines, int width) {
        int safeLimit = Math.max(1, maxLines);
        if (lines.size() <= safeLimit) {
            return lines;
        }

        List<PanelLine> limited = new ArrayList<>(lines.subList(0, safeLimit));
        PanelLine last = limited.get(safeLimit - 1);
        limited.set(safeLimit - 1,
                new PanelLine(truncateAnsi(last.text(), Math.max(1, width - 5)) + "…", last.style()));
        return limited;
    }

    /** Pinta la capçalera oberta d'un panell auxiliar. */
    private void drawPanelHeader(Terminal terminal, int row, int col, int width, String title) {
        String header = truncateAnsi(title, Math.max(1, width - 7));
        int headerVisible = visibleLength(header);

        moveCursor(terminal, row, col);
        terminal.writer().print(CYAN + "┌─ " + BOLD + header + RESET + CYAN + " "
                + "─".repeat(Math.max(0, width - headerVisible - 5)) + RESET);
    }

    /** Pinta les línies interiors d'un panell auxiliar. */
    private void drawPanelLines(Terminal terminal, int row, int col, int innerWidth, List<PanelLine> lines) {
        for (PanelLine line : lines) {
            moveCursor(terminal, row++, col);

            String style = line.style();
            String text = fitAnsi(line.text(), innerWidth);
            if (!style.isEmpty()) {
                text = text.replace(RESET, RESET + style);
            }

            terminal.writer().print(CYAN + "│ " + RESET + style + text + RESET);
        }
    }

    private void drawAchievementBadge(Terminal terminal) {
        if (completedAchievementsBadgeCount <= 0 || lastPanelRow < 0) {
            return;
        }

        int row = Math.max(TITLE_ROW + 1, lastPanelRow - 1);
        int col = lastPanelCol + 2;

        String text = YELLOW + BOLD
                + "✦ +" + completedAchievementsBadgeCount
                + " assoliment" + (completedAchievementsBadgeCount == 1 ? "" : "s")
                + RESET;

        moveCursor(terminal, row, col);
        terminal.writer().print(fitAnsi(text, Math.max(1, lastPanelWidth - 4)));
    }

    private List<String> wrapAnsiAware(String text, int maxWidth) {
        String safeText = safe(text).trim();
        if (safeText.isEmpty()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        String[] words = SPACE_PATTERN.split(safeText);
        for (String word : words) {
            if (line.isEmpty()) {
                line.append(word);
                continue;
            }

            int nextLength = visibleLength(line.toString()) + 1 + visibleLength(word);
            if (nextLength <= maxWidth) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    private static String fitAnsi(String text, int width) {
        String safeText = safe(text);
        int visible = visibleLength(safeText);

        if (visible <= width) {
            return safeText + " ".repeat(Math.max(0, width - visible));
        }

        return truncateAnsi(safeText, width) + RESET;
    }

    private static String truncateAnsi(String text, int maxVisible) {
        String safeText = safe(text);
        StringBuilder out = new StringBuilder();
        int visible = 0;

        int i = 0;

        while (i < safeText.length() && visible < maxVisible) {
            char ch = safeText.charAt(i);

            if (ch == '\u001B' && i + 1 < safeText.length() && safeText.charAt(i + 1) == '[') {
                int end = safeText.indexOf('m', i);
                if (end >= 0) {
                    out.append(safeText, i, end + 1);
                    i = end + 1;
                    continue;
                }
            }

            out.append(ch);
            visible++;
            i++;
        }

        return out.toString();
    }

    private static int visibleLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return ANSI_PATTERN.matcher(text).replaceAll("").length();
    }

    private boolean hasMoreContent(String[] lines, int from) {
        for (int i = from; i < lines.length; i++) {
            if (!lines[i].trim().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void clearPreviousPanel(Terminal terminal) {
        if (lastPanelRow < 0 || lastPanelHeight <= 0) {
            return;
        }

        int startRow = Math.max(TITLE_ROW + 1, lastPanelRow - 1);

        for (int row = startRow; row < lastPanelRow + lastPanelHeight; row++) {
            moveCursor(terminal, row, lastPanelCol);
            terminal.writer().print(" ".repeat(lastPanelWidth + 2));
        }

        lastPanelRow = -1;
        lastPanelHeight = 0;
        lastPanelCol = 0;
        lastPanelWidth = 0;
    }

    /** Neteja la pista de terreny dibuixada a la iteració anterior. */
    private void clearPreviousTerrainHintPanel(Terminal terminal) {
        if (lastTerrainPanelRow < 0 || lastTerrainPanelHeight <= 0) {
            return;
        }

        for (int row = lastTerrainPanelRow; row < lastTerrainPanelRow + lastTerrainPanelHeight; row++) {
            moveCursor(terminal, row, lastTerrainPanelCol);
            terminal.writer().print(" ".repeat(lastTerrainPanelWidth + 2));
        }

        lastTerrainPanelRow = -1;
        lastTerrainPanelHeight = 0;
        lastTerrainPanelCol = 0;
        lastTerrainPanelWidth = 0;
    }

    private String formatNumbers(String text) {
        String safeText = safe(text);
        Matcher matcher = FORMAT_NUMBERS_PATTERN.matcher(safeText);
        return matcher.replaceAll("$1");
    }

    private record ProgressSection(String title, String body) {
    }

    private record PanelLine(String text, String style) {
    }
}
