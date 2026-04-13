package rpgcombat.utils.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
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

/**
 * Menú d'armes interactiu amb JLine.
 *
 * <p>Aquesta implementació només conserva la variant amb filtres per simplificar
 * el codi. Quan només canvia la selecció, es redibuixen únicament:
 *
 * <ul>
 *   <li>la fila anterior,</li>
 *   <li>la fila actual,</li>
 *   <li>el bloc de detall inferior.</li>
 * </ul>
 *
 * <p>Quan canvien els filtres, es fa un redibuix complet perquè la llista
 * visible pot canviar de mida i contingut.
 */
public final class WeaponMenu {

    private static final TextWrapCache WRAP_CACHE = new TextWrapCache();

    private static final int NAME_WIDTH = 28;
    private static final int TYPE_WIDTH = 16;
    private static final int EQUIP_WIDTH = 13;
    private static final int DETAIL_WRAP_WIDTH = 78;
    private static final int LIST_START_ROW = 5;
    private static final int DETAIL_BLOCK_HEIGHT = 18;
    private static final String SEPARATOR = "────────────────────────────────────────────────────────";

    private WeaponMenu() {
    }

    /** Mostra el menú amb filtres. */
    public static int chooseWeaponWithFilters(List<WeaponDefinition> weapons, String title, Statistics stats) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        State state = new State();
        return runInteractiveMenu(weapons, title, stats, state);
    }

    /** Retorna directament l'arma escollida amb filtres. */
    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats) {

        ensureWeapons(weapons);

        int idx = chooseWeaponWithFilters(weapons, title, stats);
        return idx < 0 ? null : weapons.get(idx);
    }

    /** Mostra el menú amb estat extern de filtres. */
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
        internal.onlyEquippable = state.isOnlyEquippable();
        internal.typeFilter = state.getTypeFilter();

        int result = runInteractiveMenu(weapons, title, stats, internal);

        state.setOnlyEquippable(internal.onlyEquippable);
        state.setTypeFilter(internal.typeFilter);

        return result;
    }

    /** Retorna directament l'arma escollida amb estat extern. */
    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            FilterState state) {

        ensureWeapons(weapons);

        int idx = chooseWeaponWithFilters(weapons, title, stats, state);
        return idx < 0 ? null : weapons.get(idx);
    }

    /** Estat extern dels filtres. */
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

    /** Filtres de tipus d'arma. */
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

    /** Element filtrat amb índex original. */
    private record FilteredItem(int index, boolean equippable) {
    }

    /** Estat intern del menú. */
    private static final class State {
        private int cursor = 0;
        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;
    }

    /** Accions del menú. */
    private enum Action {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        TOGGLE_EQUIPPABLE,
        SELECT,
        QUIT,
        NONE
    }

    /** Tipus de redibuix necessari després d'una acció. */
    private enum RedrawMode {
        NONE,
        SELECTION_ONLY,
        FULL
    }

    /** Obre el menú interactiu. */
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

    /** Bucle principal del menú. */
    private static int runMenuLoop(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state) {

        Attributes originalAttributes = terminal.enterRawMode();
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Action> keyMap = buildKeyMap(terminal);

        boolean firstRender = true;

        try {
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.cursor_invisible);
            terminal.flush();

            while (true) {
                List<FilteredItem> filtered = buildFilteredItems(
                        weapons,
                        stats,
                        state.onlyEquippable,
                        state.typeFilter);

                normalizeCursor(state, filtered.size());

                if (firstRender) {
                    clearScreen(terminal);
                    renderMenu(terminal, weapons, filtered, title, stats, state);
                    firstRender = false;
                }

                Action action = reader.readBinding(keyMap);
                if (action == null) {
                    action = Action.NONE;
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
                RedrawMode redraw = applyAction(action, state, filtered.size());

                if (redraw == RedrawMode.NONE) {
                    continue;
                }

                if (redraw == RedrawMode.FULL) {
                    List<FilteredItem> newFiltered = buildFilteredItems(
                            weapons,
                            stats,
                            state.onlyEquippable,
                            state.typeFilter);

                    normalizeCursor(state, newFiltered.size());
                    clearScreen(terminal);
                    renderMenu(terminal, weapons, newFiltered, title, stats, state);
                    continue;
                }

                redrawSelectionChange(
                        terminal,
                        weapons,
                        filtered,
                        stats,
                        oldCursor,
                        state.cursor);
            }
        } finally {
            terminal.setAttributes(originalAttributes);
            terminal.puts(Capability.keypad_local);
            terminal.puts(Capability.cursor_visible);
            terminal.flush();
        }
    }

    /** Ajusta el cursor a l'interval vàlid. */
    private static void normalizeCursor(State state, int filteredSize) {
        if (filteredSize <= 0) {
            state.cursor = 0;
            return;
        }

        if (state.cursor < 0) {
            state.cursor = 0;
        } else if (state.cursor >= filteredSize) {
            state.cursor = filteredSize - 1;
        }
    }

    /** Aplica una acció de teclat i retorna quin redibuix cal fer. */
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
                    return RedrawMode.FULL;
                }
                return RedrawMode.NONE;

            case RIGHT:
                TypeFilter next = state.typeFilter.next();
                if (next != state.typeFilter) {
                    state.typeFilter = next;
                    state.cursor = 0;
                    return RedrawMode.FULL;
                }
                return RedrawMode.NONE;

            case TOGGLE_EQUIPPABLE:
                state.onlyEquippable = !state.onlyEquippable;
                state.cursor = 0;
                return RedrawMode.FULL;

            case NONE, SELECT, QUIT:
            default:
                return RedrawMode.NONE;
        }
    }

    /** Renderitza el menú complet. */
    private static void renderMenu(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            String title,
            Statistics stats,
            State state) {

        AttributedStringBuilder out = new AttributedStringBuilder(4096);

        JLineAnsi.appendLine(out, JLineAnsi.BOLD, safe(title));
        JLineAnsi.newLine(out);

        JLineAnsi.append(out, JLineAnsi.DARK_GRAY, "Filtres:");
        JLineAnsi.append(out, " [E] Equipables: ");
        JLineAnsi.append(
                out,
                state.onlyEquippable ? JLineAnsi.GREEN_BOLD : JLineAnsi.DARK_GRAY,
                state.onlyEquippable ? "ON" : "OFF");
        JLineAnsi.append(out, "   [←/→ o A/D] Tipus: ");
        JLineAnsi.appendLine(out, JLineAnsi.BOLD, state.typeFilter.getLabel());

        JLineAnsi.newLine(out);

        renderCompactList(terminal, out, weapons, filtered, stats, state.cursor);

        JLineAnsi.newLine(out);
        JLineAnsi.appendLine(out, JLineAnsi.DARK_GRAY, SEPARATOR);

        if (!filtered.isEmpty()) {
            FilteredItem selectedItem = filtered.get(state.cursor);
            WeaponDefinition selected = weapons.get(selectedItem.index());
            renderSelectedWeaponDetail(out, selected, stats, selectedItem.equippable());
        } else {
            JLineAnsi.appendLine(out, JLineAnsi.DARK_GRAY, "Sense selecció.");
        }

        JLineAnsi.appendLine(out, JLineAnsi.DARK_GRAY, SEPARATOR);
        JLineAnsi.appendLine(out, "[↑/↓ o W/S] moure   [←/→ o A/D] canviar tipus");
        JLineAnsi.appendLine(out, "[E] toggle equipables   [Enter] seleccionar   [Q] cancel·lar");

        terminal.writer().print(out.toAnsi(terminal));
        terminal.flush();
    }

    /** Renderitza la llista superior. */
    private static void renderCompactList(
            Terminal terminal,
            AttributedStringBuilder out,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int cursor) {

        if (filtered.isEmpty()) {
            JLineAnsi.appendLine(out, JLineAnsi.DARK_GRAY, "No hi ha armes amb aquests filtres.");
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            out.appendAnsi(buildRowAnsi(terminal, weapons, filtered.get(i), stats, i == cursor));
            JLineAnsi.newLine(out);
        }
    }

    /** Renderitza el detall de l'arma seleccionada. */
    private static void renderSelectedWeaponDetail(
            AttributedStringBuilder out,
            WeaponDefinition weapon,
            Statistics stats,
            boolean equippable) {

        if (weapon == null) {
            return;
        }

        JLineAnsi.append(out, JLineAnsi.WHITE.bold(), safe(weapon.getName()));
        JLineAnsi.append(out, " ");
        JLineAnsi.append(
                out,
                JLineAnsi.weaponTypeStyle(weapon.getType()),
                "[" + typeName(weapon.getType()) + "]");

        if (stats != null) {
            JLineAnsi.append(out, " ");
            JLineAnsi.append(
                    out,
                    equippable ? JLineAnsi.GREEN_BOLD : JLineAnsi.DARK_GRAY,
                    equippable ? "(EQUIPABLE)" : "(NO EQUIPABLE)");
        }

        JLineAnsi.newLine(out);
        JLineAnsi.newLine(out);

        String desc = weapon.getDescription();
        if (desc != null && !desc.isBlank()) {
            for (String line : WRAP_CACHE.get(desc, DETAIL_WRAP_WIDTH)) {
                JLineAnsi.appendLine(out, JLineAnsi.DARK_GRAY, line);
            }
            JLineAnsi.newLine(out);
        }

        JLineAnsi.append(out, JLineAnsi.GREEN, "Dany: ");
        JLineAnsi.append(out, JLineAnsi.BOLD, String.valueOf(weapon.getBaseDamage()));
        JLineAnsi.append(out, "   ");

        JLineAnsi.append(out, JLineAnsi.YELLOW, "Crit: ");
        JLineAnsi.append(out, JLineAnsi.BOLD, roundPer(weapon.getCriticalProb()) + "%");
        JLineAnsi.append(out, "   ");

        JLineAnsi.append(out, JLineAnsi.YELLOW, "Mult: ");
        JLineAnsi.append(out, JLineAnsi.BOLD, "x" + round2(weapon.getCriticalDamage()));
        JLineAnsi.append(out, "   ");

        double manaPrice = weapon.getManaPrice();
        if (manaPrice > 0) {
            JLineAnsi.append(out, JLineAnsi.BRIGHT_BLUE, "Mana: ");
            JLineAnsi.append(out, JLineAnsi.BOLD, String.valueOf(Math.round(manaPrice)));
        } else {
            JLineAnsi.append(out, JLineAnsi.DARK_GRAY, "Mana: -");
        }

        JLineAnsi.newLine(out);
    }

    /** Construeix la llista filtrada. */
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

    /** Crea el mapa de tecles. */
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

    /** Redibuixa només el necessari quan canvia la selecció. */
    private static void redrawSelectionChange(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int oldCursor,
            int newCursor) {

        if (filtered.isEmpty()) {
            redrawDetailBlock(terminal, weapons, filtered, stats, newCursor);
            terminal.flush();
            return;
        }

        redrawRow(terminal, weapons, filtered, stats, oldCursor, false);
        redrawRow(terminal, weapons, filtered, stats, newCursor, true);
        redrawDetailBlock(terminal, weapons, filtered, stats, newCursor);
        terminal.flush();
    }

    /** Redibuixa una sola fila de la llista. */
    private static void redrawRow(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int rowIndex,
            boolean selected) {

        if (rowIndex < 0 || rowIndex >= filtered.size()) {
            return;
        }

        int screenRow = LIST_START_ROW + rowIndex;
        moveCursor(terminal, screenRow, 1);
        clearCurrentLine(terminal);
        terminal.writer().print(buildRowAnsi(terminal, weapons, filtered.get(rowIndex), stats, selected));
    }

    /**
     * Redibuixa el bloc de detall.
     *
     * <p>No escriu salts de línia lliurement. El bloc es construeix com
     * una llista de línies i es repinta fila per fila.
     */
    private static void redrawDetailBlock(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int cursor) {

        int startRow = detailStartRow(filtered.size());
        List<String> lines = buildDetailBlockLines(terminal, weapons, filtered, stats, cursor);

        for (int i = 0; i < DETAIL_BLOCK_HEIGHT; i++) {
            moveCursor(terminal, startRow + i, 1);
            clearCurrentLine(terminal);

            if (i < lines.size()) {
                terminal.writer().print(lines.get(i));
            }
        }
    }

    /** Construeix el bloc de detall com una llista de línies ANSI. */
    private static List<String> buildDetailBlockLines(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int cursor) {

        List<String> lines = new ArrayList<>();

        lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, SEPARATOR));

        if (!filtered.isEmpty() && cursor >= 0 && cursor < filtered.size()) {
            FilteredItem selectedItem = filtered.get(cursor);
            WeaponDefinition selected = weapons.get(selectedItem.index());
            lines.addAll(buildSelectedWeaponDetailLines(terminal, selected, stats, selectedItem.equippable()));
        } else {
            lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, "Sense selecció."));
        }

        lines.add(toAnsiLine(terminal, JLineAnsi.DARK_GRAY, SEPARATOR));
        lines.add(toPlainAnsiLine(terminal, "[↑/↓ o W/S] moure   [←/→ o A/D] canviar tipus"));
        lines.add(toPlainAnsiLine(terminal, "[E] toggle equipables   [Enter] seleccionar   [Q] cancel·lar"));

        return lines;
    }

    /** Construeix les línies ANSI del detall de l'arma seleccionada. */
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
            for (String line : WRAP_CACHE.get(desc, DETAIL_WRAP_WIDTH)) {
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

    /** Construeix una sola fila com a ANSI. */
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

    /** Calcula la fila on comença el bloc inferior. */
    private static int detailStartRow(int listSize) {
        return LIST_START_ROW + listSize + 1;
    }

    /** Mou el cursor a una posició concreta. */
    private static void moveCursor(Terminal terminal, int row, int col) {
        terminal.writer().print("\033[" + row + ";" + col + "H");
    }

    /** Neteja la línia actual del terminal. */
    private static void clearCurrentLine(Terminal terminal) {
        if (!terminal.puts(Capability.clr_eol)) {
            terminal.writer().print("\033[2K");
        }
    }

    /** Construeix una línia ANSI amb estil. */
    private static String toAnsiLine(Terminal terminal, AttributedStyle style, String text) {
        AttributedStringBuilder out = new AttributedStringBuilder(text.length() + 16);
        JLineAnsi.append(out, style, safe(text));
        return out.toAnsi(terminal);
    }

    /** Construeix una línia ANSI sense estil especial. */
    private static String toPlainAnsiLine(Terminal terminal, String text) {
        AttributedStringBuilder out = new AttributedStringBuilder(text.length() + 8);
        JLineAnsi.append(out, safe(text));
        return out.toAnsi(terminal);
    }

    /** Neteja la pantalla. */
    private static void clearScreen(Terminal terminal) {
        if (terminal.puts(Capability.clear_screen)) {
            terminal.flush();
            return;
        }

        terminal.writer().print("\033[H\033[2J");
        terminal.flush();
    }

    /** Retorna el nom complet del tipus. */
    private static String typeName(WeaponType type) {
        return type == null ? "?" : safe(type.getName());
    }

    /** Retorna el nom curt del tipus. */
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

    /** Retalla o omple un text a amplada fixa. */
    private static String fixed(String text, int width) {
        return padRight(trimToWidth(text, width), width);
    }

    /** Omple un text a la dreta. */
    private static String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }

        if (text.length() >= width) {
            return text;
        }

        return text + " ".repeat(width - text.length());
    }

    /** Retalla un text si cal. */
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

    /** Evita nulls en textos. */
    private static String safe(String text) {
        return text == null ? "" : text;
    }

    /** Arrodoneix a 2 decimals. */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    /** Converteix a percentatge enter. */
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