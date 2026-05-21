package rpgcombat.terrain.ui;

import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.terrain.model.TerrainDefinition;
import rpgcombat.utils.ui.TerminalClear;

import static rpgcombat.utils.ui.Ansi.BOLD;
import static rpgcombat.utils.ui.Ansi.CYAN;
import static rpgcombat.utils.ui.Ansi.DARK_GRAY;
import static rpgcombat.utils.ui.Ansi.GREEN;
import static rpgcombat.utils.ui.Ansi.RED;
import static rpgcombat.utils.ui.Ansi.RESET;
import static rpgcombat.utils.ui.Ansi.WHITE;

/**
 * Pinta el selector de terrenys per zones fixes del terminal.
 */
final class TerrainSelectionRenderer {
    private static final int MIN_WIDTH = 84;
    private static final int MIN_HEIGHT = 22;
    private static final int TITLE_ROW = 2;
    private static final int PANELS_ROW = 4;
    private static final int OUTER_COL = 3;
    private static final int PANEL_GAP = 3;
    private static final int CONTROL_SEPARATOR_OFFSET = 4;
    private static final int INFO_MIN_WIDTH = 46;
    private static final int INFO_MAX_WIDTH = 72;
    private static final int CARD_MIN_WIDTH = 18;
    private static final int CARD_MAX_WIDTH = 25;
    private static final int CARD_HEIGHT = 5;
    private static final int CARD_COLUMN_GAP = 3;
    private static final int CARD_ROW_GAP = 1;

    /**
     * Calcula la vista actual a partir de la mida del terminal.
     *
     * @param terminal terminal que es pinta
     * @param terrainCount nombre de terrenys disponibles
     * @return geometria de la pantalla i de la graella visible
     */
    View view(Terminal terminal, int terrainCount) {
        int width = Math.max(1, terminal.getWidth());
        int height = Math.max(1, terminal.getHeight());
        int panelHeight = Math.max(1, height - PANELS_ROW - CONTROL_SEPARATOR_OFFSET);
        int usableWidth = Math.max(1, width - OUTER_COL * 2 + 1);
        int infoWidth = Math.clamp((int) Math.round(width * 0.52), INFO_MIN_WIDTH, INFO_MAX_WIDTH);
        int gridWidth = Math.max(1, usableWidth - infoWidth - PANEL_GAP);
        int gridCol = OUTER_COL + infoWidth + PANEL_GAP;
        Grid grid = grid(gridWidth, panelHeight, terrainCount);
        return new View(width, height, panelHeight, infoWidth, gridCol, gridWidth, grid);
    }

    /**
     * Pinta tota la pantalla després d'entrar al selector o canviar-ne la mida.
     *
     * @param terminal terminal que es pinta
     * @param terrains terrenys disponibles
     * @param selected índex seleccionat
     * @param view geometria vigent
     */
    void renderFull(Terminal terminal, List<TerrainDefinition> terrains, int selected, View view) {
        TerminalClear.clear(terminal);
        if (!view.canRender()) {
            renderSmallTerminal(terminal);
            return;
        }

        replaceLine(terminal, TITLE_ROW, OUTER_COL, BOLD + CYAN + "Tria el terreny" + RESET);
        renderInformationPanel(terminal, terrains.get(selected), view);
        renderTerrainPanel(terminal, terrains, selected, view);
        renderControls(terminal, view);
    }

    /**
     * Redibuixa les zones que depenen de la selecció actual.
     *
     * @param terminal terminal que es pinta
     * @param terrains terrenys disponibles
     * @param previous índex seleccionat abans del moviment
     * @param selected índex seleccionat després del moviment
     * @param view geometria vigent
     */
    void redrawSelection(
            Terminal terminal,
            List<TerrainDefinition> terrains,
            int previous,
            int selected,
            View view) {
        if (!view.canRender()) {
            renderFull(terminal, terrains, selected, view);
            return;
        }
        renderInformationContent(terminal, terrains.get(selected), view);
        if (view.pageFor(previous) != view.pageFor(selected)) {
            renderTerrainPanel(terminal, terrains, selected, view);
            return;
        }
        renderVisibleCard(terminal, terrains, previous, false, view);
        renderVisibleCard(terminal, terrains, selected, true, view);
    }

    /**
     * Mostra l'avís quan el terminal no admet el layout interactiu.
     *
     * @param terminal terminal que es pinta
     */
    private void renderSmallTerminal(Terminal terminal) {
        replaceLine(terminal, TITLE_ROW, OUTER_COL,
                RED + BOLD + "El terminal és massa petit per triar terrenys." + RESET);
        replaceLine(terminal, TITLE_ROW + 2, OUTER_COL,
                DARK_GRAY + "Augmenta la mida de la finestra." + RESET);
    }

    /**
     * Pinta el marc i el contingut del panell d'informació.
     *
     * @param terminal terminal que es pinta
     * @param terrain terreny seleccionat
     * @param view geometria vigent
     */
    private void renderInformationPanel(Terminal terminal, TerrainDefinition terrain, View view) {
        drawPanel(terminal, PANELS_ROW, OUTER_COL, view.infoWidth(), view.panelHeight(), "Informació");
        renderInformationContent(terminal, terrain, view);
    }

    /**
     * Actualitza només el contingut llegible del panell d'informació.
     *
     * @param terminal terminal que es pinta
     * @param terrain terreny seleccionat
     * @param view geometria vigent
     */
    private void renderInformationContent(Terminal terminal, TerrainDefinition terrain, View view) {
        int contentCol = OUTER_COL + 2;
        int contentWidth = view.infoWidth() - 4;
        int row = PANELS_ROW + 2;
        int lastContentRow = PANELS_ROW + view.panelHeight() - 2;

        writeAt(terminal, row++, contentCol, BOLD + WHITE + fit(terrain.name(), contentWidth) + RESET);
        if (!terrain.shortDescription().isBlank() && row <= lastContentRow) {
            writeAt(terminal, row++, contentCol,
                    DARK_GRAY + fit(terrain.shortDescription(), contentWidth) + RESET);
        }
        if (row <= lastContentRow) {
            writeAt(terminal, row++, contentCol, fit("", contentWidth));
        }

        int effectRows = Math.clamp(terrain.effectLines().size() + 1L, 2, 4);
        int descriptionRows = Math.max(1, lastContentRow - row - effectRows);
        for (String line : wrap(terrain.description(), contentWidth, descriptionRows)) {
            if (row > lastContentRow) {
                break;
            }
            writeAt(terminal, row++, contentCol, WHITE + fit(line, contentWidth) + RESET);
        }
        if (row <= lastContentRow) {
            writeAt(terminal, row++, contentCol, fit("", contentWidth));
        }
        if (row <= lastContentRow) {
            writeAt(terminal, row++, contentCol, BOLD + WHITE + fit("Efectes", contentWidth) + RESET);
        }

        List<String> effects = terrain.effectLines().isEmpty()
                ? List.of("Sense efectes addicionals.")
                : terrain.effectLines();
        for (String effect : effects) {
            for (String line : wrap(effect, Math.max(1, contentWidth - 2), lastContentRow - row + 1)) {
                if (row > lastContentRow) {
                    break;
                }
                writeAt(terminal, row++, contentCol,
                        GREEN + "· " + RESET + fit(line, Math.max(1, contentWidth - 2)));
            }
            if (row > lastContentRow) {
                break;
            }
        }
        clearPanelContent(terminal, row, lastContentRow, contentCol, contentWidth);
    }

    /**
     * Pinta el marc, les cartes i l'indicador de desplaçament del panell dret.
     *
     * @param terminal terminal que es pinta
     * @param terrains terrenys disponibles
     * @param selected índex seleccionat
     * @param view geometria vigent
     */
    private void renderTerrainPanel(
            Terminal terminal,
            List<TerrainDefinition> terrains,
            int selected,
            View view) {
        int page = view.pageFor(selected);
        drawPanel(terminal, PANELS_ROW, view.gridCol(), view.gridWidth(), view.panelHeight(),
                panelTitle(page, view.totalPages()));
        renderCards(terminal, terrains, selected, page, view);
        renderScroll(terminal, page, view);
    }

    /**
     * Pinta les cartes visibles de la pàgina actual.
     *
     * @param terminal terminal que es pinta
     * @param terrains terrenys disponibles
     * @param selected índex seleccionat
     * @param page pàgina visible
     * @param view geometria vigent
     */
    private void renderCards(
            Terminal terminal,
            List<TerrainDefinition> terrains,
            int selected,
            int page,
            View view) {
        int first = page * view.pageSize();
        int last = Math.min(terrains.size(), first + view.pageSize());
        int startRow = PANELS_ROW + 2;
        int startCol = view.gridCol() + 2;

        for (int index = first; index < last; index++) {
            int localIndex = index - first;
            renderCard(terminal,
                    cardRow(startRow, localIndex, view),
                    cardCol(startCol, localIndex, view),
                    view.cardWidth(),
                    terrains.get(index),
                    index == selected);
        }
    }

    /**
     * Redibuixa una carta concreta quan continua dins la pàgina visible.
     *
     * @param terminal terminal que es pinta
     * @param terrains terrenys disponibles
     * @param index índex de la carta
     * @param selected estat de selecció que s'ha de mostrar
     * @param view geometria vigent
     */
    private void renderVisibleCard(
            Terminal terminal,
            List<TerrainDefinition> terrains,
            int index,
            boolean selected,
            View view) {
        int page = view.pageFor(index);
        int localIndex = index - page * view.pageSize();
        renderCard(terminal,
                cardRow(PANELS_ROW + 2, localIndex, view),
                cardCol(view.gridCol() + 2, localIndex, view),
                view.cardWidth(),
                terrains.get(index),
                selected);
    }

    /**
     * Calcula la fila inicial d'una carta dins la pàgina.
     *
     * @param startRow primera fila de la graella
     * @param localIndex posició dins la pàgina
     * @param view geometria vigent
     * @return fila inicial de la carta
     */
    private int cardRow(int startRow, int localIndex, View view) {
        return startRow + localIndex / view.columns() * (CARD_HEIGHT + CARD_ROW_GAP);
    }

    /**
     * Calcula la columna inicial d'una carta dins la pàgina.
     *
     * @param startCol primera columna de la graella
     * @param localIndex posició dins la pàgina
     * @param view geometria vigent
     * @return columna inicial de la carta
     */
    private int cardCol(int startCol, int localIndex, View view) {
        return startCol + localIndex % view.columns() * (view.cardWidth() + CARD_COLUMN_GAP);
    }

    /**
     * Pinta una carta de terreny amb l'estat visual indicat.
     *
     * @param terminal terminal que es pinta
     * @param row fila inicial
     * @param col columna inicial
     * @param width amplada de la carta
     * @param terrain terreny representat
     * @param selected indica si la carta és la selecció activa
     */
    private void renderCard(
            Terminal terminal,
            int row,
            int col,
            int width,
            TerrainDefinition terrain,
            boolean selected) {
        String borderColor = selected ? GREEN + BOLD : DARK_GRAY;
        String titleColor = selected ? GREEN + BOLD : WHITE + BOLD;
        String horizontal = selected ? "═" : "─";
        String vertical = selected ? "║" : "│";
        String topLeft = selected ? "╔" : "┌";
        String topRight = selected ? "╗" : "┐";
        String bottomLeft = selected ? "╚" : "└";
        String bottomRight = selected ? "╝" : "┘";
        int innerWidth = Math.max(0, width - 2);

        writeAt(terminal, row, col,
                borderColor + topLeft + horizontal.repeat(innerWidth) + topRight + RESET);
        writeAt(terminal, row + 1, col,
                borderColor + vertical + RESET + fit("", innerWidth) + borderColor + vertical + RESET);
        writeAt(terminal, row + 2, col,
                borderColor + vertical + RESET
                        + titleColor + centered(terrain.name(), innerWidth) + RESET
                        + borderColor + vertical + RESET);
        writeAt(terminal, row + 3, col,
                borderColor + vertical + RESET + fit("", innerWidth) + borderColor + vertical + RESET);
        writeAt(terminal, row + 4, col,
                borderColor + bottomLeft + horizontal.repeat(innerWidth) + bottomRight + RESET);
    }

    /**
     * Pinta l'indicador de desplaçament integrat al panell de cartes.
     *
     * @param terminal terminal que es pinta
     * @param page pàgina visible
     * @param view geometria vigent
     */
    private void renderScroll(Terminal terminal, int page, View view) {
        String label = switch (scrollState(page, view.totalPages())) {
            case ONLY -> "";
            case FIRST -> "↓ més terrenys";
            case MIDDLE -> "↑ ↓ més terrenys";
            case LAST -> "↑ més terrenys";
        };
        writeAt(terminal, PANELS_ROW + view.panelHeight() - 2, view.gridCol() + 2,
                DARK_GRAY + fit(label, view.gridWidth() - 4) + RESET);
    }

    /**
     * Pinta la línia de controls inferior amb el seu marge visual.
     *
     * @param terminal terminal que es pinta
     * @param view geometria vigent
     */
    private void renderControls(Terminal terminal, View view) {
        int separatorRow = view.height() - CONTROL_SEPARATOR_OFFSET + 1;
        replaceLine(terminal, separatorRow, OUTER_COL,
                DARK_GRAY + "─".repeat(Math.max(0, view.width() - OUTER_COL * 2 + 1)) + RESET);
        replaceLine(terminal, separatorRow + 2, OUTER_COL,
                DARK_GRAY
                        + "[↑↓/WS] files    [←→/AD] columnes    [Enter] triar    [Q/Esc] cap terreny"
                        + RESET);
    }

    /**
     * Dibuixa un panell amb marc i interior net.
     *
     * @param terminal terminal que es pinta
     * @param row fila inicial
     * @param col columna inicial
     * @param width amplada del panell
     * @param height alçada del panell
     * @param title títol del marc
     */
    private void drawPanel(Terminal terminal, int row, int col, int width, int height, String title) {
        int innerWidth = Math.max(0, width - 2);
        String label = " " + title + " ";
        String top = "┌─" + trim(label, Math.max(0, innerWidth - 1));
        top += "─".repeat(Math.max(0, width - visibleLength(top) - 1)) + "┐";
        replaceLine(terminal, row, col, DARK_GRAY + top + RESET);
        for (int panelRow = row + 1; panelRow < row + height - 1; panelRow++) {
            replaceLine(terminal, panelRow, col,
                    DARK_GRAY + "│" + RESET + fit("", innerWidth) + DARK_GRAY + "│" + RESET);
        }
        replaceLine(terminal, row + height - 1, col,
                DARK_GRAY + "└" + "─".repeat(innerWidth) + "┘" + RESET);
    }

    /**
     * Esborra el tram interior restant d'un panell sense tocar-ne els marcs.
     *
     * @param terminal terminal que es pinta
     * @param firstRow primera fila interior
     * @param lastRow darrera fila interior
     * @param col columna inicial del contingut
     * @param width amplada netejable
     */
    private void clearPanelContent(Terminal terminal, int firstRow, int lastRow, int col, int width) {
        for (int row = firstRow; row <= lastRow; row++) {
            writeAt(terminal, row, col, fit("", width));
        }
    }

    /**
     * Calcula columnes, amplada de carta i paginació de la graella.
     *
     * @param panelWidth amplada del panell dret
     * @param panelHeight alçada del panell dret
     * @param terrainCount nombre de terrenys disponibles
     * @return geometria de la graella
     */
    private Grid grid(int panelWidth, int panelHeight, int terrainCount) {
        int contentWidth = Math.max(1, panelWidth - 4);
        int gridHeight = Math.max(1, panelHeight - 4);
        int columns = Math.max(1, (contentWidth + CARD_COLUMN_GAP) / (CARD_MIN_WIDTH + CARD_COLUMN_GAP));
        int cardWidth = Math.clamp(
                (contentWidth - Math.max(0, columns - 1) * CARD_COLUMN_GAP) / columns,
                Math.min(CARD_MIN_WIDTH, contentWidth),
                Math.min(CARD_MAX_WIDTH, contentWidth));
        columns = Math.max(1, (contentWidth + CARD_COLUMN_GAP) / (cardWidth + CARD_COLUMN_GAP));
        int rows = Math.max(1, (gridHeight + CARD_ROW_GAP) / (CARD_HEIGHT + CARD_ROW_GAP));
        int pageSize = Math.max(1, columns * rows);
        int totalPages = Math.max(1, (int) Math.ceil(terrainCount / (double) pageSize));
        return new Grid(columns, cardWidth, pageSize, totalPages);
    }

    /**
     * Escriu el títol del panell de terrenys amb la pàgina quan cal.
     *
     * @param page pàgina visible
     * @param totalPages pàgines totals
     * @return títol visible
     */
    private String panelTitle(int page, int totalPages) {
        return totalPages > 1 ? "Terrenys " + (page + 1) + "/" + totalPages : "Terrenys";
    }

    /**
     * Resol quin indicador de desplaçament correspon a la pàgina visible.
     *
     * @param page pàgina visible
     * @param totalPages pàgines totals
     * @return estat de desplaçament
     */
    private ScrollState scrollState(int page, int totalPages) {
        if (totalPages <= 1) {
            return ScrollState.ONLY;
        }
        if (page == 0) {
            return ScrollState.FIRST;
        }
        if (page == totalPages - 1) {
            return ScrollState.LAST;
        }
        return ScrollState.MIDDLE;
    }

    /**
     * Parteix un text en línies acotades per amplada i alçada.
     *
     * @param text text original
     * @param width amplada màxima
     * @param maxLines nombre màxim de línies
     * @return línies visibles
     */
    private List<String> wrap(String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        int safeWidth = Math.max(1, width);

        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (remaining.length() <= safeWidth) {
                lines.add(remaining);
                break;
            }
            int cut = remaining.lastIndexOf(' ', safeWidth);
            if (cut <= 0) {
                cut = safeWidth;
            }
            String line = remaining.substring(0, cut).trim();
            remaining = remaining.substring(Math.min(cut, remaining.length())).trim();
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) {
                line = ellipsis(line, safeWidth);
            }
            lines.add(line);
        }
        return lines;
    }

    /**
     * Centra un text dins l'amplada visible indicada.
     *
     * @param text text original
     * @param width amplada disponible
     * @return text centrat
     */
    private String centered(String text, int width) {
        String value = trim(text, width);
        int left = Math.max(0, (width - visibleLength(value)) / 2);
        return " ".repeat(left) + fit(value, width - left);
    }

    /**
     * Retalla i omple un text fins a l'amplada visible indicada.
     *
     * @param text text original
     * @param width amplada disponible
     * @return text ajustat
     */
    private String fit(String text, int width) {
        String value = trim(text, width);
        return value + " ".repeat(Math.max(0, width - visibleLength(value)));
    }

    /**
     * Retalla un text conservant un indicador de continuació.
     *
     * @param text text original
     * @param width amplada màxima
     * @return text retallat
     */
    private String trim(String text, int width) {
        String value = text == null ? "" : text;
        if (visibleLength(value) <= width) {
            return value;
        }
        if (width <= 1) {
            return "";
        }
        String plain = plainText(value);
        return plain.substring(0, width - 1) + "…";
    }

    /**
     * Tanca una línia truncada amb punts suspensius.
     *
     * @param text text original
     * @param width amplada màxima
     * @return text truncat
     */
    private String ellipsis(String text, int width) {
        if (width <= 1) {
            return "";
        }
        return text.length() <= width - 1 ? text + "…" : text.substring(0, width - 1) + "…";
    }

    /**
     * Mesura la longitud visible ignorant codis ANSI.
     *
     * @param text text amb format opcional
     * @return longitud visible
     */
    private int visibleLength(String text) {
        return plainText(text).length();
    }

    /**
     * Elimina els codis ANSI d'un text abans de mesurar-lo.
     *
     * @param text text amb format opcional
     * @return text pla
     */
    private String plainText(String text) {
        String value = text == null ? "" : text;
        StringBuilder plain = new StringBuilder(value.length());
        boolean ansi = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ansi) {
                ansi = ch != 'm';
            } else if (ch == '\u001b') {
                ansi = true;
            } else {
                plain.append(ch);
            }
        }
        return plain.toString();
    }

    /**
     * Reemplaça una línia del terminal des de la columna indicada.
     *
     * @param terminal terminal que es pinta
     * @param row fila del terminal
     * @param col columna inicial
     * @param text text nou
     */
    private void replaceLine(Terminal terminal, int row, int col, String text) {
        moveTo(terminal, row, col);
        terminal.writer().print(text);
        if (!terminal.puts(Capability.clr_eol)) {
            terminal.writer().print("\033[K");
        }
    }

    /**
     * Escriu text en una posició sense netejar el que hi ha al costat.
     *
     * @param terminal terminal que es pinta
     * @param row fila del terminal
     * @param col columna inicial
     * @param text text nou
     */
    private void writeAt(Terminal terminal, int row, int col, String text) {
        moveTo(terminal, row, col);
        terminal.writer().print(text);
    }

    /**
     * Mou el cursor del terminal a una posició concreta.
     *
     * @param terminal terminal que es pinta
     * @param row fila del terminal
     * @param col columna del terminal
     */
    private void moveTo(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /**
     * Geometria estable d'un render del selector.
     *
     * @param width amplada visible del terminal
     * @param height alçada visible del terminal
     * @param panelHeight alçada dels dos panells principals
     * @param infoWidth amplada del panell d'informació
     * @param gridCol columna inicial del panell de terrenys
     * @param gridWidth amplada del panell de terrenys
     * @param grid geometria de cartes i pàgines
     */
    record View(
            int width,
            int height,
            int panelHeight,
            int infoWidth,
            int gridCol,
            int gridWidth,
            Grid grid) {
        /**
         * Indica si el terminal té espai per als dos panells i les cartes.
         *
         * @return {@code true} quan es pot pintar el selector complet
         */
        boolean canRender() {
            return width >= MIN_WIDTH
                    && height >= MIN_HEIGHT
                    && panelHeight >= CARD_HEIGHT + 4
                    && gridWidth >= CARD_MIN_WIDTH + 4;
        }

        /**
         * Retorna les columnes visibles de la graella.
         *
         * @return nombre de columnes
         */
        int columns() {
            return grid.columns();
        }

        /**
         * Retorna l'amplada vigent de cada carta.
         *
         * @return amplada de carta
         */
        int cardWidth() {
            return grid.cardWidth();
        }

        /**
         * Retorna el nombre màxim de cartes visibles per pàgina.
         *
         * @return mida de pàgina
         */
        int pageSize() {
            return grid.pageSize();
        }

        /**
         * Retorna el nombre de pàgines necessàries per la graella.
         *
         * @return pàgines totals
         */
        int totalPages() {
            return grid.totalPages();
        }

        /**
         * Resol la pàgina que conté una selecció.
         *
         * @param selected índex seleccionat
         * @return pàgina visible
         */
        int pageFor(int selected) {
            return Math.clamp(selected / Math.max(1, pageSize()), 0, Math.max(0, totalPages() - 1));
        }
    }

    /**
     * Geometria interna de la graella de cartes.
     *
     * @param columns columnes visibles
     * @param cardWidth amplada de carta
     * @param pageSize cartes visibles per pàgina
     * @param totalPages pàgines totals
     */
    private record Grid(int columns, int cardWidth, int pageSize, int totalPages) {
    }

    /**
     * Estat de desplaçament que s'ha de comunicar a la graella.
     */
    private enum ScrollState {
        ONLY,
        FIRST,
        MIDDLE,
        LAST
    }
}
