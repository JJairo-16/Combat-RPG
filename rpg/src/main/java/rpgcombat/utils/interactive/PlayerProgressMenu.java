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
 * Menú d'accions que afegeix un panell independent per consultar missions i
 * perks del jugador sense acoblar-ho al menú lateral d'informació.
 */
public final class PlayerProgressMenu extends MenuWithInformation {
    private static final int PANEL_WIDTH = 48;
    private static final int PANEL_MAX_LINES = 20;
    private static final int PANEL_CONTROLS_GAP_ROWS = 1;
    private static final int BOTTOM_SAFE_MARGIN_ROWS = 2;

    private static final Pattern SECTION_SEPARATOR_PATTERN = Pattern.compile("\\R---\\R");
    private static final Pattern FIRST_LINE_PATTERN = Pattern.compile("\\R");
    private static final Pattern LINE_PATTERN = Pattern.compile("\\R");

    private String progressText = "";
    private int selectedSection;
    private int lastPanelRow = -1;
    private int lastPanelHeight;

    public PlayerProgressMenu(Map<String, String> information) {
        super(information);
    }

    /** Defineix el text de missions/perks que es mostra al panell propi. */
    public void setProgressText(String text) {
        this.progressText = formatNumbers(safe(text));
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
        drawProgressPanel(terminal, terminal.getHeight() - BOTTOM_SAFE_MARGIN_ROWS);
        terminal.flush();
    }

    @Override
    protected void afterContentRendered(Terminal terminal, List<String> options, int cursor, int controlsRow) {
        drawProgressPanel(terminal, controlsRow);
    }

    @Override
    protected void beforeDynamicAreaCleared(Terminal terminal) {
        clearPreviousPanel(terminal);
    }

    private void drawProgressPanel(Terminal terminal, int controlsRow) {
        clearPreviousPanel(terminal);
        List<ProgressSection> sections = sections();
        if (sections.isEmpty())
            return;

        selectedSection = Math.clamp(selectedSection, 0, sections.size() - 1);
        ProgressSection section = sections.get(selectedSection);

        int width = Math.clamp(terminal.getWidth() - leftPadding - 2L, 24, PANEL_WIDTH);
        List<PanelLine> lines = buildLines(section.body(), width - 4);
        if (lines.size() > PANEL_MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, PANEL_MAX_LINES));
            PanelLine last = lines.get(PANEL_MAX_LINES - 1);
            lines.set(PANEL_MAX_LINES - 1, new PanelLine(trimToWidth(last.text(), width - 5) + "…", last.style()));
        }

        int contentHeight = lines.size() + 2;
        int row = Math.max(TITLE_ROW + 1, controlsRow - contentHeight - PANEL_CONTROLS_GAP_ROWS);
        int col = Math.max(leftPadding, terminal.getWidth() - width);
        lastPanelRow = row;
        lastPanelHeight = contentHeight;

        String suffix = sections.size() > 1 ? " [P]" : "";
        String header = trimToWidth(section.title() + suffix, Math.max(1, width - 7));
        moveCursor(terminal, row++, col);
        terminal.writer().print(CYAN + "┌─ " + BOLD + header + RESET + CYAN + " "
                + "─".repeat(Math.max(0, width - header.length() - 5)) + RESET);

        for (PanelLine line : lines) {
            moveCursor(terminal, row++, col);
            terminal.writer().print(CYAN + "│ " + RESET + line.style() + trimToWidth(line.text(), width - 4) + RESET);
        }
    }

    private List<ProgressSection> sections() {
        String text = safe(progressText).trim();
        if (text.isBlank())
            return List.of();
        String[] raw = SECTION_SEPARATOR_PATTERN.split(text);
        List<ProgressSection> result = new ArrayList<>();
        for (String block : raw) {
            String[] parts = FIRST_LINE_PATTERN.split(block.trim(), 2);
            if (parts.length == 0 || parts[0].isBlank())
                continue;
            result.add(new ProgressSection(parts[0].trim(), parts.length > 1 ? parts[1].trim() : ""));
        }
        return result;
    }

    /**
     * Converteix el cos d'una secció en línies renderitzables.
     * <p>
     * Manté el format de cada bloc:
     * nom, descripció i línia d'estat. Després de cada línia de tancament
     * de bloc, com {@code Progrés: ...} o {@code Activació: ...}, afegeix
     * una línia buida si encara queda més contingut.
     *
     * @param text  cos de la secció
     * @param width amplada màxima disponible per línia
     * @return línies preparades per pintar
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

            for (String wrappedLine : wrap(lineText, width)) {
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

    /**
     * Indica si encara queda alguna línia amb contingut a partir d'una posició.
     *
     * @param lines línies originals
     * @param from  índex inicial de cerca
     * @return {@code true} si queda contingut visible
     */
    private boolean hasMoreContent(String[] lines, int from) {
        for (int i = from; i < lines.length; i++) {
            if (!lines[i].trim().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void clearPreviousPanel(Terminal terminal) {
        if (lastPanelRow < 0 || lastPanelHeight <= 0)
            return;
        int width = Math.clamp(terminal.getWidth() - leftPadding - 2L, 24, PANEL_WIDTH);
        int col = Math.max(leftPadding, terminal.getWidth() - width);
        for (int i = 0; i < lastPanelHeight; i++) {
            moveCursor(terminal, lastPanelRow + i, col);
            terminal.writer().print(" ".repeat(width + 2));
        }
        lastPanelRow = -1;
        lastPanelHeight = 0;
    }

    private static final Pattern FORMAT_NUMBERS_PATTERN = Pattern.compile("(?<!\\d)(\\d+)\\.0(?!\\d)");

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
