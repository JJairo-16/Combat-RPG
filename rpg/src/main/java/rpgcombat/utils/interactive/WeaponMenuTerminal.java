package rpgcombat.utils.interactive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.cache.TextWrapCache;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.config.WeaponType;

/**
 * Gestiona el terminal del menú d'armes amb JLine.
 */
final class WeaponMenuTerminal implements AutoCloseable {

    private static final TextWrapCache WRAP_CACHE = new TextWrapCache();

    private static final int NAME_WIDTH = 30;
    private static final int TYPE_WIDTH = 16;
    private static final int EQUIP_WIDTH = 13;
    private static final int DETAIL_WRAP_WIDTH = 78;

    private static final int HEADER_ROWS = 5;
    private static final int LIST_START_ROW = 6;

    private static final int LIST_DETAIL_SPACER_ROWS = 1;
    private static final int DETAIL_CONTROLS_SPACER_ROWS = 1;
    private static final int CONTROL_ROWS = 3;

    private static final int BOTTOM_SAFE_MARGIN_ROWS = 1;
    private static final int RESIZE_COMPENSATION_ROWS = 2;

    private static final int MIN_VISIBLE_ROWS = 1;
    private static final int MIN_DETAIL_ROWS = 4;

    private static final int DEFAULT_TERMINAL_HEIGHT = 24;
    private static final int DEFAULT_TERMINAL_WIDTH = 80;

    private static final String SEPARATOR = "────────────────────────────────────────────────────────";

    private final Terminal terminal;
    private final BindingReader reader;
    private final KeyMap<WeaponMenu.Action> keyMap;

    private final Attributes originalAttributes;
    private final Terminal.SignalHandler previousWinchHandler;

    /** Amplada actual del terminal. */
    private int width;

    /** Alçada actual del terminal. */
    private int height;

    /**
     * Crea el controlador del terminal i activa el mode interactiu.
     *
     * @param terminal     terminal compartit
     * @param winchHandler gestor del senyal de redimensionament
     */
    private WeaponMenuTerminal(Terminal terminal, Terminal.SignalHandler winchHandler) {
        this.terminal = terminal;
        this.originalAttributes = terminal.enterRawMode();
        this.reader = new BindingReader(terminal.reader());
        this.keyMap = buildKeyMap(terminal);
        this.previousWinchHandler = terminal.handle(Terminal.Signal.WINCH, winchHandler);

        refreshSize();

        terminal.puts(Capability.enter_ca_mode);
        terminal.puts(Capability.keypad_xmit);
        terminal.puts(Capability.cursor_invisible);
        terminal.flush();
    }

    /**
     * Obre una sessió de terminal per al menú.
     *
     * @param winchHandler gestor del senyal WINCH
     * @return instància preparada per al menú
     * @throws IOException si no es pot crear el terminal
     */
    static WeaponMenuTerminal open(Terminal.SignalHandler winchHandler) throws IOException {
        return new WeaponMenuTerminal(SharedTerminal.get(), winchHandler);
    }

    /**
     * Inicialitza el terminal compartit si encara no existeix.
     *
     * @throws IOException si no es pot crear el terminal
     */
    static void preloadSharedTerminal() throws IOException {
        SharedTerminal.preload();
    }

    /**
     * Llegeix la següent acció de l'usuari.
     *
     * @return acció associada a la tecla premuda
     */
    WeaponMenu.Action readAction() {
        return reader.readBinding(keyMap);
    }

    /**
     * Actualitza les dimensions en memòria cau.
     *
     * @return {@code true} si la mida ha canviat
     */
    boolean refreshSize() {
        int newWidth = terminalWidth(terminal);
        int newHeight = terminalHeight(terminal);
        boolean changed = newWidth != width || newHeight != height;
        width = newWidth;
        height = newHeight;
        return changed;
    }

    /**
     * Calcula quantes files de la llista es poden mostrar.
     *
     * @return nombre de files visibles
     */
    int computeVisibleRows() {
        int usableHeight = effectiveTerminalHeight();

        int reserved = HEADER_ROWS
                + LIST_DETAIL_SPACER_ROWS
                + MIN_DETAIL_ROWS
                + DETAIL_CONTROLS_SPACER_ROWS
                + CONTROL_ROWS;

        int available = usableHeight - reserved;
        return Math.max(MIN_VISIBLE_ROWS, available);
    }

    /** Neteja la pantalla i situa el cursor a l'inici. */
    void clearScreen() {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }

        moveCursor(1, 1);
        terminal.flush();
    }

    /**
     * Renderitza tot el menú visible.
     *
     * @param weapons     llista original d'armes
     * @param filtered    armes visibles després dels filtres
     * @param title       títol del menú
     * @param stats       estadístiques del personatge
     * @param state       estat actual del menú
     * @param visibleRows files visibles de la llista
     */
    void renderMenu(
            List<WeaponDefinition> weapons,
            List<WeaponMenu.FilteredItem> filtered,
            String title,
            Statistics stats,
            WeaponMenu.State state,
            int visibleRows) {

        List<String> lines = new ArrayList<>();

        lines.add(toAnsiLine(JLineAnsi.BOLD, safe(title)));
        lines.add("");

        AttributedStringBuilder filtersLine = new AttributedStringBuilder(256);
        JLineAnsi.append(filtersLine, JLineAnsi.DARK_GRAY, "Filtres:");
        JLineAnsi.append(filtersLine, " [E] Equipables: ");
        JLineAnsi.append(
                filtersLine,
                state.onlyEquippable ? JLineAnsi.GREEN_BOLD : JLineAnsi.DARK_GRAY,
                state.onlyEquippable ? "ON" : "OFF");
        JLineAnsi.append(filtersLine, "   [←/→ o A/D] Tipus: ");
        JLineAnsi.append(filtersLine, JLineAnsi.BOLD, state.typeFilter.getLabel());
        lines.add(filtersLine.toAnsi(terminal));

        AttributedStringBuilder sortLine = new AttributedStringBuilder(256);
        JLineAnsi.append(sortLine, JLineAnsi.DARK_GRAY, "Ordenació:");
        JLineAnsi.append(sortLine, " [O] Criteri: ");
        JLineAnsi.append(sortLine, JLineAnsi.BOLD, state.sortCriterion.getLabel());
        JLineAnsi.append(sortLine, "   [R] Direcció: ");
        if (state.sortCriterion.supportsDirection()) {
            JLineAnsi.append(
                    sortLine,
                    state.sortAscending ? JLineAnsi.GREEN_BOLD : JLineAnsi.YELLOW_BOLD,
                    state.sortAscending ? "ASC" : "DESC");
        } else {
            JLineAnsi.append(sortLine, JLineAnsi.DARK_GRAY, "N/A");
        }
        lines.add(sortLine.toAnsi(terminal));

        lines.add("");

        List<String> listLines = buildVisibleListLines(weapons, filtered, stats, state, visibleRows);
        lines.addAll(listLines);

        int detailStartRow = detailStartRow(listLines.size());

        while (lines.size() < detailStartRow - 1) {
            lines.add("");
        }

        int detailRows = computeDetailRows(detailStartRow);
        List<String> detailLines = buildDetailBlockLines(
                weapons,
                filtered,
                stats,
                state.cursor,
                detailRows);

        lines.addAll(detailLines);

        int controlsStart = controlsStartRow();

        while (lines.size() < controlsStart - 1) {
            lines.add("");
        }

        lines.add(toPlainAnsiLine("[↑/↓ o W/S] moure   [←/→ o A/D] canviar tipus   [E] toggle equipables"));
        lines.add(toPlainAnsiLine("[O] canviar criteri d'ordenació   [R] invertir ASC/DESC"));
        lines.add(toPlainAnsiLine("[Enter] seleccionar   [Q] cancel·lar"));

        paintScreenLines(lines);
    }

    /**
     * Repinta només la selecció i el bloc de detall.
     *
     * @param weapons     llista original d'armes
     * @param filtered    armes visibles després dels filtres
     * @param stats       estadístiques del personatge
     * @param state       estat actual del menú
     * @param visibleRows files visibles de la llista
     * @param oldCursor   selecció anterior
     * @param newCursor   selecció nova
     */
    void redrawSelectionChange(
            List<WeaponDefinition> weapons,
            List<WeaponMenu.FilteredItem> filtered,
            Statistics stats,
            WeaponMenu.State state,
            int visibleRows,
            int oldCursor,
            int newCursor) {

        if (filtered.isEmpty()) {
            redrawDetailBlock(weapons, filtered, stats, state, visibleRows, newCursor);
            terminal.flush();
            return;
        }

        redrawRow(weapons, filtered, stats, state, oldCursor, false);
        redrawRow(weapons, filtered, stats, state, newCursor, true);
        redrawDetailBlock(weapons, filtered, stats, state, visibleRows, newCursor);
        terminal.flush();
    }

    /**
     * Repinta una fila visible de la llista.
     *
     * @param weapons  llista original d'armes
     * @param filtered armes visibles després dels filtres
     * @param stats    estadístiques del personatge
     * @param state    estat actual del menú
     * @param rowIndex índex lògic de la fila
     * @param selected si la fila està seleccionada
     */
    private void redrawRow(
            List<WeaponDefinition> weapons,
            List<WeaponMenu.FilteredItem> filtered,
            Statistics stats,
            WeaponMenu.State state,
            int rowIndex,
            boolean selected) {

        if (rowIndex < 0 || rowIndex >= filtered.size()) {
            return;
        }

        int visibleRows = computeVisibleRows();
        int renderedRows = visibleRowCount(filtered.size(), state.viewportStart, visibleRows);
        if (rowIndex < state.viewportStart || rowIndex >= state.viewportStart + renderedRows) {
            return;
        }

        int screenRow = LIST_START_ROW + (rowIndex - state.viewportStart);
        if (screenRow > maxPaintRow()) {
            return;
        }

        moveCursor(screenRow, 1);
        String line = buildRowAnsi(weapons, filtered.get(rowIndex), stats, selected);
        terminal.writer().print(padRight(line, width));
    }

    /**
     * Repinta el bloc de detall de l'arma seleccionada.
     *
     * @param weapons     llista original d'armes
     * @param filtered    armes visibles després dels filtres
     * @param stats       estadístiques del personatge
     * @param state       estat actual del menú
     * @param visibleRows files visibles de la llista
     * @param cursor      selecció actual
     */
    private void redrawDetailBlock(
            List<WeaponDefinition> weapons,
            List<WeaponMenu.FilteredItem> filtered,
            Statistics stats,
            WeaponMenu.State state,
            int visibleRows,
            int cursor) {

        int renderedListRows = visibleRowCount(filtered.size(), state.viewportStart, visibleRows);
        int startRow = detailStartRow(renderedListRows);
        int detailRows = computeDetailRows(startRow);
        List<String> lines = buildDetailBlockLines(weapons, filtered, stats, cursor, detailRows);

        for (int i = 0; i < detailRows; i++) {
            int row = startRow + i;
            if (row > maxPaintRow()) {
                return;
            }

            moveCursor(row, 1);
            clearCurrentLine();

            if (i < lines.size()) {
                terminal.writer().print(lines.get(i));
            }
        }

        int spacerRow = startRow + detailRows;
        int controlsStart = controlsStartRow();
        if (spacerRow < controlsStart && spacerRow <= maxPaintRow()) {
            moveCursor(spacerRow, 1);
            clearCurrentLine();
        }
    }

    /**
     * Construeix les files visibles de la llista.
     *
     * @param weapons     llista original d'armes
     * @param filtered    armes visibles després dels filtres
     * @param stats       estadístiques del personatge
     * @param state       estat actual del menú
     * @param visibleRows files visibles disponibles
     * @return línies ANSI de la llista
     */
    private List<String> buildVisibleListLines(
            List<WeaponDefinition> weapons,
            List<WeaponMenu.FilteredItem> filtered,
            Statistics stats,
            WeaponMenu.State state,
            int visibleRows) {

        List<String> lines = new ArrayList<>();

        if (filtered.isEmpty()) {
            lines.add(toAnsiLine(JLineAnsi.DARK_GRAY, "No hi ha armes amb aquests filtres i aquesta ordenació."));
            return lines;
        }

        int start = state.viewportStart;
        int end = Math.min(filtered.size(), start + Math.max(MIN_VISIBLE_ROWS, visibleRows));

        for (int i = start; i < end; i++) {
            lines.add(buildRowAnsi(weapons, filtered.get(i), stats, i == state.cursor));
        }

        return lines;
    }

    /**
     * Construeix les línies del bloc de detall.
     *
     * @param weapons  llista original d'armes
     * @param filtered armes visibles després dels filtres
     * @param stats    estadístiques del personatge
     * @param cursor   selecció actual
     * @param maxRows  màxim de files disponibles
     * @return línies ANSI del detall
     */
    private List<String> buildDetailBlockLines(
            List<WeaponDefinition> weapons,
            List<WeaponMenu.FilteredItem> filtered,
            Statistics stats,
            int cursor,
            int maxRows) {

        List<String> lines = new ArrayList<>();
        if (maxRows <= 0) {
            return lines;
        }

        lines.add(toAnsiLine(JLineAnsi.DARK_GRAY, SEPARATOR));

        if (!filtered.isEmpty() && cursor >= 0 && cursor < filtered.size()) {
            WeaponMenu.FilteredItem selectedItem = filtered.get(cursor);
            WeaponDefinition selected = weapons.get(selectedItem.index());
            lines.addAll(buildSelectedWeaponDetailLines(selected, stats, selectedItem.equippable()));
        } else {
            lines.add(toAnsiLine(JLineAnsi.DARK_GRAY, "Sense selecció."));
        }

        if (lines.size() > maxRows) {
            return new ArrayList<>(lines.subList(0, maxRows));
        }

        return lines;
    }

    /**
     * Construeix les línies de detall d'una arma.
     *
     * @param weapon     arma seleccionada
     * @param stats      estadístiques del personatge
     * @param equippable si es pot equipar
     * @return línies ANSI del detall
     */
    private List<String> buildSelectedWeaponDetailLines(
            WeaponDefinition weapon,
            Statistics stats,
            boolean equippable) {

        List<String> lines = new ArrayList<>();

        if (weapon == null) {
            return lines;
        }

        AttributedStringBuilder title = new AttributedStringBuilder(256);
        JLineAnsi.append(title, JLineAnsi.WHITE.bold(), safe(weapon.getName()));
        JLineAnsi.append(title, " ");
        JLineAnsi.append(
                title,
                JLineAnsi.weaponTypeStyle(weapon.getType()),
                "[" + typeName(weapon.getType()) + "]");

        if (stats != null) {
            JLineAnsi.append(title, " ");
            JLineAnsi.append(
                    title,
                    equippable ? JLineAnsi.GREEN_BOLD : JLineAnsi.DARK_GRAY,
                    equippable ? "(EQUIPABLE)" : "(NO EQUIPABLE)");
        }

        lines.add(title.toAnsi(terminal));
        lines.add("");

        String desc = weapon.getDescription();
        if (desc != null && !desc.isBlank()) {
            int wrapWidth = computeDetailWrapWidth();
            for (String line : WRAP_CACHE.get(desc, wrapWidth)) {
                lines.add(toAnsiLine(JLineAnsi.DARK_GRAY, line));
            }
            lines.add("");
        }

        AttributedStringBuilder statsLine = new AttributedStringBuilder(256);
        JLineAnsi.append(statsLine, JLineAnsi.GREEN, "Dany: ");
        JLineAnsi.append(statsLine, JLineAnsi.BOLD, String.valueOf(weapon.getBaseDamage()));
        JLineAnsi.append(statsLine, "   ");

        JLineAnsi.append(statsLine, JLineAnsi.YELLOW, "Crit: ");
        JLineAnsi.append(statsLine, JLineAnsi.BOLD, roundPer(weapon.getCriticalProb()) + "%");
        JLineAnsi.append(statsLine, "   ");

        JLineAnsi.append(statsLine, JLineAnsi.YELLOW, "Mult: ");
        JLineAnsi.append(statsLine, JLineAnsi.BOLD, "x" + round2(weapon.getCriticalDamage()));
        JLineAnsi.append(statsLine, "   ");

        double manaPrice = weapon.getManaPrice();
        if (manaPrice > 0) {
            JLineAnsi.append(statsLine, JLineAnsi.BRIGHT_BLUE, "Mana: ");
            JLineAnsi.append(statsLine, JLineAnsi.BOLD, String.valueOf(Math.round(manaPrice)));
        } else {
            JLineAnsi.append(statsLine, JLineAnsi.DARK_GRAY, "Mana: -");
        }

        lines.add(statsLine.toAnsi(terminal));
        return lines;
    }

    /**
     * Construeix una fila ANSI de la llista.
     *
     * @param weapons  llista original d'armes
     * @param item     element filtrat
     * @param stats    estadístiques del personatge
     * @param selected si està seleccionat
     * @return fila ANSI renderitzada
     */
    private String buildRowAnsi(
            List<WeaponDefinition> weapons,
            WeaponMenu.FilteredItem item,
            Statistics stats,
            boolean selected) {

        WeaponDefinition weapon = weapons.get(item.index());
        AttributedStringBuilder out = new AttributedStringBuilder(256);

        if (selected) {
            JLineAnsi.append(out, JLineAnsi.CYAN_BOLD, ">");
        } else {
            JLineAnsi.append(out, " ");
        }

        JLineAnsi.append(out, " ");
        JLineAnsi.append(out, JLineAnsi.BOLD, fixed(safe(weapon.getName()), NAME_WIDTH));
        JLineAnsi.append(out, " ");
        JLineAnsi.append(
                out,
                JLineAnsi.weaponTypeStyle(weapon.getType()),
                fixed("[" + shortTypeName(weapon.getType()) + "]", TYPE_WIDTH));

        if (stats != null) {
            String equipText = fixed(item.equippable() ? "EQUIPABLE" : "NO EQUIPABLE", EQUIP_WIDTH);
            JLineAnsi.append(out, " ");
            JLineAnsi.append(
                    out,
                    item.equippable() ? JLineAnsi.GREEN_BOLD : JLineAnsi.DARK_GRAY,
                    equipText);
        }

        return out.toAnsi(terminal);
    }

    /**
     * Calcula la fila inicial del bloc de detall.
     *
     * @param renderedListRows files renderitzades de la llista
     * @return fila inicial del detall
     */
    private int detailStartRow(int renderedListRows) {
        return LIST_START_ROW + renderedListRows + LIST_DETAIL_SPACER_ROWS;
    }

    /**
     * Calcula la fila inicial dels controls inferiors.
     *
     * @return fila inicial dels controls
     */
    private int controlsStartRow() {
        return maxPaintRow() - CONTROL_ROWS + 1;
    }

    /**
     * Calcula quantes files té disponibles el detall.
     *
     * @param detailStartRow fila inicial del detall
     * @return nombre de files disponibles
     */
    private int computeDetailRows(int detailStartRow) {
        int controlsStart = controlsStartRow();
        int lastDetailRow = controlsStart - DETAIL_CONTROLS_SPACER_ROWS - 1;
        return Math.max(0, lastDetailRow - detailStartRow + 1);
    }

    /**
     * Calcula quantes files de la llista es renderitzen.
     *
     * @param filteredSize  nombre total d'elements visibles
     * @param viewportStart inici del viewport
     * @param visibleRows   màxim de files visibles
     * @return nombre de files renderitzades
     */
    private int visibleRowCount(int filteredSize, int viewportStart, int visibleRows) {
        if (filteredSize <= 0) {
            return 1;
        }

        int clampedVisibleRows = Math.max(MIN_VISIBLE_ROWS, visibleRows);
        int remaining = filteredSize - viewportStart;
        return Math.clamp(remaining, 1, clampedVisibleRows);
    }

    /**
     * Pinta totes les línies visibles de pantalla.
     *
     * @param lines línies ANSI a mostrar
     */
    private void paintScreenLines(List<String> lines) {
        int maxPaintRow = maxPaintRow();

        for (int row = 1; row <= maxPaintRow; row++) {
            moveCursor(row, 1);
            clearCurrentLine();

            int lineIndex = row - 1;
            if (lineIndex < lines.size()) {
                terminal.writer().print(lines.get(lineIndex));
            }
        }

        moveCursor(1, 1);
        terminal.flush();
    }

    /**
     * Retorna l'última fila segura per pintar.
     *
     * @return última fila útil
     */
    private int maxPaintRow() {
        return Math.max(0, effectiveTerminalHeight());
    }

    /**
     * Calcula l'alçada útil del terminal.
     *
     * @return alçada disponible per al menú
     */
    private int effectiveTerminalHeight() {
        int compensatedHeight = height - BOTTOM_SAFE_MARGIN_ROWS - RESIZE_COMPENSATION_ROWS;
        return Math.max(1, compensatedHeight);
    }

    /**
     * Calcula l'amplada de wrapping del detall.
     *
     * @return amplada màxima de text
     */
    private int computeDetailWrapWidth() {
        return Math.clamp(width - 2L, 24, DETAIL_WRAP_WIDTH);
    }

    /**
     * Mou el cursor a una posició concreta.
     *
     * @param row fila de destí
     * @param col columna de destí
     */
    private void moveCursor(int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /** Esborra el contingut de la línia actual. */
    private void clearCurrentLine() {
        if (!terminal.puts(Capability.clr_eol)) {
            terminal.writer().print("\033[2K");
        }
    }

    /**
     * Construeix una línia ANSI amb estil.
     *
     * @param style estil ANSI
     * @param text  text a mostrar
     * @return línia ANSI renderitzada
     */
    private String toAnsiLine(AttributedStyle style, String text) {
        AttributedStringBuilder out = new AttributedStringBuilder(text.length() + 16);
        JLineAnsi.append(out, style, safe(text));
        return out.toAnsi(terminal);
    }

    /**
     * Construeix una línia ANSI sense estil addicional.
     *
     * @param text text a mostrar
     * @return línia ANSI renderitzada
     */
    private String toPlainAnsiLine(String text) {
        AttributedStringBuilder out = new AttributedStringBuilder(text.length() + 8);
        JLineAnsi.append(out, safe(text));
        return out.toAnsi(terminal);
    }

    /**
     * Construeix el mapa de tecles del menú.
     *
     * @param terminal terminal actual
     * @return mapa de tecles a accions
     */
    private static KeyMap<WeaponMenu.Action> buildKeyMap(Terminal terminal) {
        KeyMap<WeaponMenu.Action> map = new KeyMap<>();

        map.bind(WeaponMenu.Action.UP, "w", "W");
        map.bind(WeaponMenu.Action.DOWN, "s", "S");
        map.bind(WeaponMenu.Action.LEFT, "a", "A");
        map.bind(WeaponMenu.Action.RIGHT, "d", "D");
        map.bind(WeaponMenu.Action.TOGGLE_EQUIPPABLE, "e", "E");
        map.bind(WeaponMenu.Action.NEXT_SORT_CRITERION, "o", "O");
        map.bind(WeaponMenu.Action.TOGGLE_SORT_DIRECTION, "r", "R");
        map.bind(WeaponMenu.Action.SELECT, "\r", "\n");
        map.bind(WeaponMenu.Action.QUIT, "q", "Q");

        String up = KeyMap.key(terminal, Capability.key_up);
        String down = KeyMap.key(terminal, Capability.key_down);
        String left = KeyMap.key(terminal, Capability.key_left);
        String right = KeyMap.key(terminal, Capability.key_right);

        if (up != null) {
            map.bind(WeaponMenu.Action.UP, up);
        }
        if (down != null) {
            map.bind(WeaponMenu.Action.DOWN, down);
        }
        if (left != null) {
            map.bind(WeaponMenu.Action.LEFT, left);
        }
        if (right != null) {
            map.bind(WeaponMenu.Action.RIGHT, right);
        }

        return map;
    }

    /**
     * Obté l'alçada del terminal amb valors de suport.
     *
     * @param terminal terminal actual
     * @return nombre de files
     */
    private static int terminalHeight(Terminal terminal) {
        if (terminal == null) {
            return DEFAULT_TERMINAL_HEIGHT;
        }

        int height = terminal.getHeight();
        if (height > 0) {
            return height;
        }

        Size size = terminal.getSize();
        if (size != null && size.getRows() > 0) {
            return size.getRows();
        }

        return DEFAULT_TERMINAL_HEIGHT;
    }

    /**
     * Obté l'amplada del terminal amb valors de suport.
     *
     * @param terminal terminal actual
     * @return nombre de columnes
     */
    private static int terminalWidth(Terminal terminal) {
        if (terminal == null) {
            return DEFAULT_TERMINAL_WIDTH;
        }

        int width = terminal.getWidth();
        if (width > 0) {
            return width;
        }

        Size size = terminal.getSize();
        if (size != null && size.getColumns() > 0) {
            return size.getColumns();
        }

        return DEFAULT_TERMINAL_WIDTH;
    }

    /**
     * Retorna el nom complet d'un tipus d'arma.
     *
     * @param type tipus d'arma
     * @return nom visible o {@code "?"}
     */
    private static String typeName(WeaponType type) {
        return type == null ? "?" : safe(type.getName());
    }

    /**
     * Retorna el nom curt d'un tipus d'arma.
     *
     * @param type tipus d'arma
     * @return nom curt visible
     */
    private static String shortTypeName(WeaponType type) {
        if (type == null) {
            return "?";
        }

        return switch (type) {
            case PHYSICAL -> "física";
            case RANGE -> "de rang";
            case MAGICAL -> "màgica";
            default -> safe(type.getName());
        };
    }

    /**
     * Ajusta un text a una amplada fixa.
     *
     * @param text  text original
     * @param width amplada objectiu
     * @return text ajustat
     */
    private static String fixed(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }

        int len = text.length();

        if (len <= width) {
            int pad = width - len;
            return pad == 0 ? text : text + " ".repeat(pad);
        }

        if (width == 1) {
            return "…";
        }

        int end = width - 1;
        return text.substring(0, end) + "…";
    }

    /**
     * Afegeix espais a la dreta fins a l'amplada indicada.
     *
     * @param text  text original
     * @param width amplada final
     * @return text amb padding a la dreta
     */
    private static String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }

        if (width <= 0) {
            return "";
        }

        int len = text.length();
        if (len >= width) {
            return text;
        }

        return text + " ".repeat(width - len);
    }

    /**
     * Evita valors nuls en textos.
     *
     * @param text text original
     * @return text segur
     */
    private static String safe(String text) {
        return text == null ? "" : text;
    }

    /**
     * Arrodoneix un valor a dues decimals.
     *
     * @param n valor original
     * @return valor arrodonit
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    /**
     * Converteix una probabilitat a percentatge enter.
     *
     * @param n probabilitat entre 0 i 1
     * @return percentatge arrodonit
     */
    private static int roundPer(double n) {
        return (int) Math.round(n * 100.0);
    }

    /** Restaura el terminal al seu estat original. */
    @Override
    public void close() {
        terminal.handle(Terminal.Signal.WINCH, previousWinchHandler);
        terminal.setAttributes(originalAttributes);
        terminal.puts(Capability.keypad_local);
        terminal.puts(Capability.cursor_visible);
        terminal.puts(Capability.exit_ca_mode);
        terminal.flush();
    }
}