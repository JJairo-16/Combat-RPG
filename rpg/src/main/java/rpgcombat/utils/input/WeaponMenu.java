package rpgcombat.utils.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.cache.TextWrapCache;
import rpgcombat.utils.ui.JLineAnsi;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.config.WeaponType;

public final class WeaponMenu {
    private static final TextWrapCache WRAP_CACHE = new TextWrapCache();

    private static final int NAME_WIDTH = 28;
    private static final int TYPE_WIDTH = 16;
    private static final int EQUIP_WIDTH = 13;
    private static final int DETAIL_WRAP_WIDTH = 78;

    private static final int HEADER_ROWS = 4;
    private static final int LIST_START_ROW = 5;

    private static final int LIST_DETAIL_SPACER_ROWS = 1;
    private static final int DETAIL_CONTROLS_SPACER_ROWS = 1;
    private static final int CONTROL_ROWS = 2;

    private static final int BOTTOM_SAFE_MARGIN_ROWS = 1;
    private static final int RESIZE_COMPENSATION_ROWS = 2;

    private static final int MIN_VISIBLE_ROWS = 1;
    private static final int MIN_DETAIL_ROWS = 4;

    private static final int DEFAULT_TERMINAL_HEIGHT = 24;
    private static final int DEFAULT_TERMINAL_WIDTH = 80;

    private static final String SEPARATOR = "────────────────────────────────────────────────────────";

    private WeaponMenu() {
    }

    public static int chooseWeaponWithFilters(List<WeaponDefinition> weapons, String title, Statistics stats) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        State state = new State();
        return runInteractiveMenu(weapons, title, stats, state);
    }

    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats) {

        ensureWeapons(weapons);
        int idx = chooseWeaponWithFilters(weapons, title, stats);
        return idx < 0 ? null : weapons.get(idx);
    }

    public static int chooseWeaponWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            FilterState state) {

        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        if (state == null) {
            state = new FilterState();
        }

        State internal = new State();
        internal.cursor = 0;
        internal.viewportStart = 0;
        internal.onlyEquippable = state.isOnlyEquippable();
        internal.typeFilter = state.getTypeFilter();

        int result = runInteractiveMenu(weapons, title, stats, internal);

        state.setOnlyEquippable(internal.onlyEquippable);
        state.setTypeFilter(internal.typeFilter);

        return result;
    }

    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            FilterState state) {

        ensureWeapons(weapons);
        int idx = chooseWeaponWithFilters(weapons, title, stats, state);
        return idx < 0 ? null : weapons.get(idx);
    }

    public static final class FilterState {
        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;

        public FilterState() {
        }

        public FilterState(boolean onlyEquippable, TypeFilter typeFilter) {
            this.onlyEquippable = onlyEquippable;
            this.typeFilter = typeFilter == null ? TypeFilter.ALL : typeFilter;
        }

        public boolean isOnlyEquippable() {
            return onlyEquippable;
        }

        public void setOnlyEquippable(boolean onlyEquippable) {
            this.onlyEquippable = onlyEquippable;
        }

        public TypeFilter getTypeFilter() {
            return typeFilter;
        }

        public void setTypeFilter(TypeFilter typeFilter) {
            this.typeFilter = typeFilter == null ? TypeFilter.ALL : typeFilter;
        }
    }

    public enum TypeFilter {
        ALL("Tots"),
        RANGE("Rang"),
        PHYSICAL("Físic"),
        MAGICAL("Màgic");

        private final String label;

        TypeFilter(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean matches(WeaponType type) {
            if (this == ALL) {
                return true;
            }
            if (type == null) {
                return false;
            }

            switch (this) {
                case RANGE:
                    return type == WeaponType.RANGE;
                case PHYSICAL:
                    return type == WeaponType.PHYSICAL;
                case MAGICAL:
                    return type == WeaponType.MAGICAL;
                case ALL:
                default:
                    return true;
            }
        }

        public TypeFilter next() {
            switch (this) {
                case ALL:
                    return RANGE;
                case RANGE:
                    return PHYSICAL;
                case PHYSICAL:
                    return MAGICAL;
                case MAGICAL:
                default:
                    return ALL;
            }
        }

        public TypeFilter previous() {
            switch (this) {
                case ALL:
                    return MAGICAL;
                case RANGE:
                    return ALL;
                case PHYSICAL:
                    return RANGE;
                case MAGICAL:
                default:
                    return PHYSICAL;
            }
        }
    }

    private record FilteredItem(int index, boolean equippable) {
    }

    private static final class State {
        private int cursor = 0;
        private int viewportStart = 0;
        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;
        private int lastTerminalWidth = -1;
        private int lastTerminalHeight = -1;
        private boolean resizePending = false;
    }

    private enum Action {
        UP, DOWN, LEFT, RIGHT, TOGGLE_EQUIPPABLE, SELECT, QUIT, NONE
    }

    private enum RedrawMode {
        NONE, SELECTION_ONLY, FULL
    }

    private static int runInteractiveMenu(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state) {

        try {
            Terminal terminal = getTerminal();
            return runMenuLoop(terminal, weapons, title, stats, state);
        } catch (IOException e) {
            Prettier.warn("No s'ha pogut obrir el menú interactiu: %s", e.getMessage());
            Menu.pause();
            return -1;
        }
    }

    private static int runMenuLoop(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state) {

        Attributes originalAttributes = terminal.enterRawMode();
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Action> keyMap = buildKeyMap(terminal);
        Terminal.SignalHandler previousWinchHandler = terminal.handle(
                Terminal.Signal.WINCH,
                signal -> state.resizePending = true);

        boolean firstRender = true;

        try {
            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.cursor_invisible);
            terminal.flush();

            while (true) {
                List<FilteredItem> filtered = buildFilteredItems(
                        weapons, stats, state.onlyEquippable, state.typeFilter);

                normalizeCursor(state, filtered.size());

                int visibleRows = computeVisibleRows(terminal);
                adjustViewport(state, filtered.size(), visibleRows);

                boolean sizeChanged = refreshTerminalSize(terminal, state);
                if (firstRender || state.resizePending || sizeChanged) {
                    state.resizePending = false;
                    clearScreen(terminal);
                    renderMenu(terminal, weapons, filtered, title, stats, state, visibleRows);
                    firstRender = false;
                }

                Action action = reader.readBinding(keyMap);
                if (action == null) {
                    continue;
                }

                if (action == Action.SELECT) {
                    if (!filtered.isEmpty()) {
                        return filtered.get(state.cursor).index();
                    }
                    continue;
                }

                if (action == Action.QUIT) {
                    return -1;
                }

                int oldCursor = state.cursor;
                int oldViewportStart = state.viewportStart;
                int oldWidth = state.lastTerminalWidth;
                int oldHeight = state.lastTerminalHeight;

                RedrawMode redraw = applyAction(action, state, filtered.size());
                if (redraw == RedrawMode.NONE) {
                    continue;
                }

                List<FilteredItem> currentFiltered = buildFilteredItems(
                        weapons, stats, state.onlyEquippable, state.typeFilter);

                normalizeCursor(state, currentFiltered.size());

                int currentVisibleRows = computeVisibleRows(terminal);
                adjustViewport(state, currentFiltered.size(), currentVisibleRows);

                boolean changedAfterAction = refreshTerminalSize(terminal, state);
                boolean geometryChanged = changedAfterAction
                        || state.resizePending
                        || state.lastTerminalWidth != oldWidth
                        || state.lastTerminalHeight != oldHeight;

                if (redraw == RedrawMode.FULL || geometryChanged || state.viewportStart != oldViewportStart) {
                    state.resizePending = false;
                    clearScreen(terminal);
                    renderMenu(terminal, weapons, currentFiltered, title, stats, state, currentVisibleRows);
                    continue;
                }

                redrawSelectionChange(
                        terminal,
                        weapons,
                        currentFiltered,
                        stats,
                        state,
                        currentVisibleRows,
                        oldCursor,
                        state.cursor);
            }
        } finally {
            terminal.handle(Terminal.Signal.WINCH, previousWinchHandler);
            terminal.setAttributes(originalAttributes);
            terminal.puts(Capability.keypad_local);
            terminal.puts(Capability.cursor_visible);
            terminal.puts(Capability.exit_ca_mode);
            terminal.flush();
        }
    }

    private static void normalizeCursor(State state, int filteredSize) {
        if (filteredSize <= 0) {
            state.cursor = 0;
            state.viewportStart = 0;
            return;
        }

        if (state.cursor < 0) {
            state.cursor = 0;
        } else if (state.cursor >= filteredSize) {
            state.cursor = filteredSize - 1;
        }
    }

    private static boolean refreshTerminalSize(Terminal terminal, State state) {
        int width = terminalWidth(terminal);
        int height = terminalHeight(terminal);
        boolean changed = width != state.lastTerminalWidth || height != state.lastTerminalHeight;
        state.lastTerminalWidth = width;
        state.lastTerminalHeight = height;
        return changed;
    }

    private static void adjustViewport(State state, int filteredSize, int visibleRows) {
        if (filteredSize <= 0) {
            state.viewportStart = 0;
            return;
        }

        int clampedVisibleRows = Math.max(MIN_VISIBLE_ROWS, visibleRows);
        int maxViewportStart = Math.max(0, filteredSize - clampedVisibleRows);

        if (state.viewportStart > maxViewportStart) {
            state.viewportStart = maxViewportStart;
        }

        if (state.cursor < state.viewportStart) {
            state.viewportStart = state.cursor;
        } else if (state.cursor >= state.viewportStart + clampedVisibleRows) {
            state.viewportStart = state.cursor - clampedVisibleRows + 1;
        }

        if (state.viewportStart < 0) {
            state.viewportStart = 0;
        } else if (state.viewportStart > maxViewportStart) {
            state.viewportStart = maxViewportStart;
        }
    }

    private static RedrawMode applyAction(Action action, State state, int filteredSize) {
        switch (action) {
            case UP:
                if (filteredSize > 0 && state.cursor > 0) {
                    state.cursor--;
                    return RedrawMode.SELECTION_ONLY;
                }
                return RedrawMode.NONE;

            case DOWN:
                if (filteredSize > 0 && state.cursor < filteredSize - 1) {
                    state.cursor++;
                    return RedrawMode.SELECTION_ONLY;
                }
                return RedrawMode.NONE;

            case LEFT:
                TypeFilter previous = state.typeFilter.previous();
                if (previous != state.typeFilter) {
                    state.typeFilter = previous;
                    state.cursor = 0;
                    state.viewportStart = 0;
                    return RedrawMode.FULL;
                }
                return RedrawMode.NONE;

            case RIGHT:
                TypeFilter next = state.typeFilter.next();
                if (next != state.typeFilter) {
                    state.typeFilter = next;
                    state.cursor = 0;
                    state.viewportStart = 0;
                    return RedrawMode.FULL;
                }
                return RedrawMode.NONE;

            case TOGGLE_EQUIPPABLE:
                state.onlyEquippable = !state.onlyEquippable;
                state.cursor = 0;
                state.viewportStart = 0;
                return RedrawMode.FULL;

            case NONE, SELECT, QUIT:
            default:
                return RedrawMode.NONE;
        }
    }

    private static void renderMenu(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            String title,
            Statistics stats,
            State state,
            int visibleRows) {

        List<String> lines = new ArrayList<>();

        lines.add(toAnsiLine(terminal, JLineAnsi.BOLD, safe(title)));
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
        lines.add("");

        List<String> listLines = buildVisibleListLines(terminal, weapons, filtered, stats, state, visibleRows);
        lines.addAll(listLines);

        int detailStartRow = detailStartRow(listLines.size());

        while (lines.size() < detailStartRow - 1) {
            lines.add("");
        }

        int detailRows = computeDetailRows(terminal, detailStartRow);
        List<String> detailLines = buildDetailBlockLines(
                terminal,
                weapons,
                filtered,
                stats,
                state.cursor,
                detailRows);

        lines.addAll(detailLines);

        int controlsStart = controlsStartRow(terminal);

        while (lines.size() < controlsStart - 1) {
            lines.add("");
        }

        lines.add(toPlainAnsiLine(terminal, "[↑/↓ o W/S] moure   [←/→ o A/D] canviar tipus"));
        lines.add(toPlainAnsiLine(terminal, "[E] toggle equipables   [Enter] seleccionar   [Q] cancel·lar"));

        paintScreenLines(terminal, lines);
    }

    private static List<String> buildVisibleListLines(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            State state,
            int visibleRows) {

        List<String> lines = new ArrayList<>();

        if (filtered.isEmpty()) {
            lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, "No hi ha armes amb aquests filtres."));
            return lines;
        }

        int start = state.viewportStart;
        int end = Math.min(filtered.size(), start + Math.max(MIN_VISIBLE_ROWS, visibleRows));

        for (int i = start; i < end; i++) {
            lines.add(buildRowAnsi(terminal, weapons, filtered.get(i), stats, i == state.cursor));
        }

        return lines;
    }

    private static List<FilteredItem> buildFilteredItems(
            List<WeaponDefinition> weapons,
            Statistics stats,
            boolean onlyEquippable,
            TypeFilter typeFilter) {

        List<FilteredItem> out = new ArrayList<>();

        for (int i = 0; i < weapons.size(); i++) {
            WeaponDefinition weapon = weapons.get(i);
            WeaponType weaponType = weapon == null ? null : weapon.getType();

            if (!typeFilter.matches(weaponType)) {
                continue;
            }

            boolean equippable = false;
            if (stats != null && weaponType != null) {
                equippable = weaponType.canEquip(stats);
            }

            if (!onlyEquippable || equippable) {
                out.add(new FilteredItem(i, equippable));
            }
        }

        return out;
    }

    private static KeyMap<Action> buildKeyMap(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.UP, "w", "W");
        map.bind(Action.DOWN, "s", "S");
        map.bind(Action.LEFT, "a", "A");
        map.bind(Action.RIGHT, "d", "D");
        map.bind(Action.TOGGLE_EQUIPPABLE, "e", "E");
        map.bind(Action.SELECT, "\r", "\n");
        map.bind(Action.QUIT, "q", "Q");

        String up = KeyMap.key(terminal, Capability.key_up);
        String down = KeyMap.key(terminal, Capability.key_down);
        String left = KeyMap.key(terminal, Capability.key_left);
        String right = KeyMap.key(terminal, Capability.key_right);

        if (up != null) {
            map.bind(Action.UP, up);
        }
        if (down != null) {
            map.bind(Action.DOWN, down);
        }
        if (left != null) {
            map.bind(Action.LEFT, left);
        }
        if (right != null) {
            map.bind(Action.RIGHT, right);
        }

        return map;
    }

    private static void redrawSelectionChange(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            State state,
            int visibleRows,
            int oldCursor,
            int newCursor) {

        if (filtered.isEmpty()) {
            redrawDetailBlock(terminal, weapons, filtered, stats, state, visibleRows, newCursor);
            redrawControls(terminal);
            terminal.flush();
            return;
        }

        redrawRow(terminal, weapons, filtered, stats, state, oldCursor, false);
        redrawRow(terminal, weapons, filtered, stats, state, newCursor, true);
        redrawDetailBlock(terminal, weapons, filtered, stats, state, visibleRows, newCursor);
        redrawControls(terminal);
        terminal.flush();
    }

    private static void redrawRow(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            State state,
            int rowIndex,
            boolean selected) {

        if (rowIndex < 0 || rowIndex >= filtered.size()) {
            return;
        }

        int visibleRows = computeVisibleRows(terminal);
        int renderedRows = visibleRowCount(filtered.size(), state.viewportStart, visibleRows);
        if (rowIndex < state.viewportStart || rowIndex >= state.viewportStart + renderedRows) {
            return;
        }

        int screenRow = LIST_START_ROW + (rowIndex - state.viewportStart);
        if (screenRow > maxPaintRow(terminal)) {
            return;
        }

        moveCursor(terminal, screenRow, 1);
        clearCurrentLine(terminal);
        terminal.writer().print(buildRowAnsi(terminal, weapons, filtered.get(rowIndex), stats, selected));
    }

    private static void redrawDetailBlock(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            State state,
            int visibleRows,
            int cursor) {

        int renderedListRows = visibleRowCount(filtered.size(), state.viewportStart, visibleRows);
        int startRow = detailStartRow(renderedListRows);
        int detailRows = computeDetailRows(terminal, startRow);
        List<String> lines = buildDetailBlockLines(terminal, weapons, filtered, stats, cursor, detailRows);

        for (int i = 0; i < detailRows; i++) {
            int row = startRow + i;
            if (row > maxPaintRow(terminal)) {
                return;
            }

            moveCursor(terminal, row, 1);
            clearCurrentLine(terminal);

            if (i < lines.size()) {
                terminal.writer().print(lines.get(i));
            }
        }

        int spacerRow = startRow + detailRows;
        int controlsStart = controlsStartRow(terminal);
        if (spacerRow < controlsStart && spacerRow <= maxPaintRow(terminal)) {
            moveCursor(terminal, spacerRow, 1);
            clearCurrentLine(terminal);
        }
    }

    private static void redrawControls(Terminal terminal) {
        int startRow = controlsStartRow(terminal);
        if (startRow <= 0) {
            return;
        }

        moveCursor(terminal, startRow, 1);
        clearCurrentLine(terminal);
        terminal.writer().print(toPlainAnsiLine(terminal, "[↑/↓ o W/S] moure   [←/→ o A/D] canviar tipus"));

        if (startRow + 1 <= maxPaintRow(terminal)) {
            moveCursor(terminal, startRow + 1, 1);
            clearCurrentLine(terminal);
            terminal.writer().print(toPlainAnsiLine(
                    terminal,
                    "[E] toggle equipables   [Enter] seleccionar   [Q] cancel·lar"));
        }
    }

    private static List<String> buildDetailBlockLines(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int cursor,
            int maxRows) {

        List<String> lines = new ArrayList<>();
        if (maxRows <= 0) {
            return lines;
        }

        lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, SEPARATOR));

        if (!filtered.isEmpty() && cursor >= 0 && cursor < filtered.size()) {
            FilteredItem selectedItem = filtered.get(cursor);
            WeaponDefinition selected = weapons.get(selectedItem.index());
            lines.addAll(buildSelectedWeaponDetailLines(terminal, selected, stats, selectedItem.equippable()));
        } else {
            lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, "Sense selecció."));
        }

        if (lines.size() > maxRows) {
            return new ArrayList<>(lines.subList(0, maxRows));
        }

        while (lines.size() < maxRows) {
            lines.add("");
        }

        return lines;
    }

    private static List<String> buildSelectedWeaponDetailLines(
            Terminal terminal,
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
            int wrapWidth = computeDetailWrapWidth(terminal);
            for (String line : WRAP_CACHE.get(desc, wrapWidth)) {
                lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, line));
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

    private static String buildRowAnsi(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            FilteredItem item,
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

    private static int detailStartRow(int renderedListRows) {
        return LIST_START_ROW + renderedListRows + LIST_DETAIL_SPACER_ROWS;
    }

    private static int controlsStartRow(Terminal terminal) {
        return maxPaintRow(terminal) - CONTROL_ROWS + 1;
    }

    private static int computeDetailRows(Terminal terminal, int detailStartRow) {
        int controlsStart = controlsStartRow(terminal);
        int lastDetailRow = controlsStart - DETAIL_CONTROLS_SPACER_ROWS - 1;
        return Math.max(0, lastDetailRow - detailStartRow + 1);
    }

    private static int computeVisibleRows(Terminal terminal) {
        int usableHeight = effectiveTerminalHeight(terminal);

        int reserved = HEADER_ROWS
                + LIST_DETAIL_SPACER_ROWS
                + MIN_DETAIL_ROWS
                + DETAIL_CONTROLS_SPACER_ROWS
                + CONTROL_ROWS;

        int available = usableHeight - reserved;
        return Math.max(MIN_VISIBLE_ROWS, available);
    }

    private static int visibleRowCount(int filteredSize, int viewportStart, int visibleRows) {
        if (filteredSize <= 0) {
            return 1;
        }

        int clampedVisibleRows = Math.max(MIN_VISIBLE_ROWS, visibleRows);
        int remaining = filteredSize - viewportStart;
        return Math.clamp(remaining, 1, clampedVisibleRows);
    }

    private static void paintScreenLines(Terminal terminal, List<String> lines) {
        int maxPaintRow = maxPaintRow(terminal);

        for (int row = 1; row <= maxPaintRow; row++) {
            moveCursor(terminal, row, 1);
            clearCurrentLine(terminal);

            int lineIndex = row - 1;
            if (lineIndex < lines.size()) {
                terminal.writer().print(lines.get(lineIndex));
            }
        }

        moveCursor(terminal, 1, 1);
        terminal.flush();
    }

    private static int maxPaintRow(Terminal terminal) {
        return Math.max(0, effectiveTerminalHeight(terminal));
    }

    private static int effectiveTerminalHeight(Terminal terminal) {
        int rawHeight = terminalHeight(terminal);
        int compensatedHeight = rawHeight - BOTTOM_SAFE_MARGIN_ROWS - RESIZE_COMPENSATION_ROWS;
        return Math.max(1, compensatedHeight);
    }

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

    private static int computeDetailWrapWidth(Terminal terminal) {
        int width = terminalWidth(terminal);
        return Math.clamp(width - 2L, 24, DETAIL_WRAP_WIDTH);
    }

    private static void moveCursor(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    private static void clearCurrentLine(Terminal terminal) {
        if (!terminal.puts(Capability.clr_eol)) {
            terminal.writer().print("\033[2K");
        }
    }

    private static String toAnsiLine(Terminal terminal, AttributedStyle style, String text) {
        AttributedStringBuilder out = new AttributedStringBuilder(text.length() + 16);
        JLineAnsi.append(out, style, safe(text));
        return out.toAnsi(terminal);
    }

    private static String toPlainAnsiLine(Terminal terminal, String text) {
        AttributedStringBuilder out = new AttributedStringBuilder(text.length() + 8);
        JLineAnsi.append(out, safe(text));
        return out.toAnsi(terminal);
    }

    private static void clearScreen(Terminal terminal) {
        if (!terminal.puts(Capability.clear_screen)) {
            terminal.writer().print("\033[H\033[2J");
        }

        moveCursor(terminal, 1, 1);
        terminal.flush();
    }

    private static String typeName(WeaponType type) {
        return type == null ? "?" : safe(type.getName());
    }

    private static String shortTypeName(WeaponType type) {
        if (type == null) {
            return "?";
        }

        switch (type) {
            case PHYSICAL:
                return "física";
            case RANGE:
                return "de rang";
            case MAGICAL:
                return "màgica";
            default:
                return safe(type.getName());
        }
    }

    private static String fixed(String text, int width) {
        return padRight(trimToWidth(text, width), width);
    }

    private static String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }

        if (text.length() >= width) {
            return text;
        }

        return text + " ".repeat(width - text.length());
    }

    private static String trimToWidth(String text, int width) {
        if (text == null) {
            return "";
        }

        if (width <= 0) {
            return "";
        }

        if (text.length() <= width) {
            return text;
        }

        if (width == 1) {
            return "…";
        }

        return text.substring(0, width - 1) + "…";
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static int roundPer(double n) {
        return (int) Math.round(n * 100.0);
    }

    private static void ensureWeapons(List<WeaponDefinition> weapons) {
        Objects.requireNonNull(weapons, "La llista d'armes no pot ser nul·la.");
    }

    private static Terminal sharedTerminal;

    private static Terminal buildTerminal() throws IOException {
        return TerminalBuilder.builder()
                .system(true)
                .nativeSignals(true)
                .build();
    }

    private static Terminal getTerminal() throws IOException {
        if (sharedTerminal == null) {
            sharedTerminal = buildTerminal();
        }
        return sharedTerminal;
    }

    public static void preloadTerminal() throws IOException {
        if (sharedTerminal == null) {
            sharedTerminal = buildTerminal();
        }
    }
}