package rpgcombat.utils.input;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.cache.TextWrapCache;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.config.WeaponType;

/**
 * Menú d'armes amb navegació dinàmica i vista compacta.
 */
public final class WeaponMenu {

    private static final TextWrapCache WRAP_CACHE = new TextWrapCache();

    private static final int NAME_WIDTH = 28;
    private static final int TYPE_WIDTH = 16;
    private static final int EQUIP_WIDTH = 13;
    private static final int DETAIL_WRAP_WIDTH = 78;

    private WeaponMenu() {
    }

    /**
     * Mostra un menú d'armes sense filtres.
     *
     * @param weapons armes a mostrar
     * @param title   títol del menú
     * @return índex 0-based o -1 si es cancel·la
     */
    public static int chooseWeapon(List<WeaponDefinition> weapons, String title) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        State state = new State();
        return runInteractiveMenu(weapons, title, null, state, false);
    }

    /**
     * Retorna directament l'arma escollida.
     *
     * @param weapons armes a mostrar
     * @param title   títol del menú
     * @return arma escollida o {@code null} si es cancel·la
     */
    public static WeaponDefinition chooseWeaponEntry(List<WeaponDefinition> weapons, String title) {
        if (weapons == null || weapons.isEmpty()) {
            throw new IllegalArgumentException();
        }

        int idx = chooseWeapon(weapons, title);
        return (idx < 0) ? null : weapons.get(idx);
    }

    /**
     * Mostra un menú d'armes amb filtres.
     *
     * @param weapons armes a mostrar
     * @param title   títol del menú
     * @param stats   estadístiques per calcular si l'arma és equipable
     * @return índex 0-based o -1 si es cancel·la
     */
    public static int chooseWeaponWithFilters(List<WeaponDefinition> weapons, String title, Statistics stats) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        State state = new State();
        return runInteractiveMenu(weapons, title, stats, state, true);
    }

    /**
     * Retorna directament l'arma escollida amb filtres.
     *
     * @param weapons armes a mostrar
     * @param title   títol del menú
     * @param stats   estadístiques per calcular si l'arma és equipable
     * @return arma escollida o {@code null} si es cancel·la
     */
    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons, String title, Statistics stats) {
        ensureWeapons(weapons);
        int idx = chooseWeaponWithFilters(weapons, title, stats);
        return (idx < 0) ? null : weapons.get(idx);
    }

    /**
     * Mostra un menú d'armes amb estat de filtres persistent.
     *
     * @param weapons armes a mostrar
     * @param title   títol del menú
     * @param stats   estadístiques per calcular si l'arma és equipable
     * @param state   estat extern dels filtres
     * @return índex 0-based o -1 si es cancel·la
     */
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

        int result = runInteractiveMenu(weapons, title, stats, internal, true);

        state.setOnlyEquippable(internal.onlyEquippable);
        state.setTypeFilter(internal.typeFilter);

        return result;
    }

    /**
     * Retorna directament l'arma escollida amb estat de filtres persistent.
     *
     * @param weapons armes a mostrar
     * @param title   títol del menú
     * @param stats   estadístiques per calcular si l'arma és equipable
     * @param state   estat extern dels filtres
     * @return arma escollida o {@code null} si es cancel·la
     */
    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            FilterState state) {

        ensureWeapons(weapons);

        int idx = chooseWeaponWithFilters(weapons, title, stats, state);
        return (idx < 0) ? null : weapons.get(idx);
    }

    /**
     * Estat extern dels filtres.
     */
    public static final class FilterState {

        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;

        public FilterState() {
        }

        public FilterState(boolean onlyEquippable, TypeFilter typeFilter) {
            this.onlyEquippable = onlyEquippable;
            this.typeFilter = (typeFilter == null) ? TypeFilter.ALL : typeFilter;
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
            this.typeFilter = (typeFilter == null) ? TypeFilter.ALL : typeFilter;
        }
    }

    /**
     * Element filtrat amb índex original.
     */
    private record FilteredItem(int index, boolean equippable) {
    }

    /**
     * Estat intern del menú.
     */
    private static final class State {
        private int cursor = 0;
        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;
    }

    /**
     * Accions del menú.
     */
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

    /**
     * Filtres de tipus d'arma.
     */
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

        public boolean matches(WeaponType t) {
            if (this == ALL) {
                return true;
            }

            if (t == null) {
                return false;
            }

            return switch (this) {
                case RANGE -> t == WeaponType.RANGE;
                case PHYSICAL -> t == WeaponType.PHYSICAL;
                case MAGICAL -> t == WeaponType.MAGICAL;
                default -> true;
            };
        }

        public TypeFilter next() {
            return switch (this) {
                case ALL -> RANGE;
                case RANGE -> PHYSICAL;
                case PHYSICAL -> MAGICAL;
                case MAGICAL -> ALL;
            };
        }

        public TypeFilter previous() {
            return switch (this) {
                case ALL -> MAGICAL;
                case RANGE -> ALL;
                case PHYSICAL -> RANGE;
                case MAGICAL -> PHYSICAL;
            };
        }
    }

    /**
     * Executa el menú interactiu.
     */
    private static int runInteractiveMenu(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state,
            boolean allowFilters) {

        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .nativeSignals(true)
                .build()) {

            return runMenuLoop(terminal, weapons, title, stats, state, allowFilters);

        } catch (Exception e) {
            Prettier.warn("No s'ha pogut obrir el menú interactiu: %s", e.getMessage());
            Menu.pause();
            return -1;
        }
    }

    /**
     * Bucle principal del menú.
     */
    private static int runMenuLoop(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state,
            boolean allowFilters) {

        Attributes originalAttributes = terminal.enterRawMode();
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Action> keyMap = buildKeyMap(terminal, allowFilters);

        boolean dirty = true;

        try {
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.cursor_invisible);
            terminal.flush();

            while (true) {
                List<FilteredItem> filtered = buildFilteredItems(
                        weapons,
                        stats,
                        allowFilters && state.onlyEquippable,
                        allowFilters ? state.typeFilter : TypeFilter.ALL);

                normalizeCursor(state, filtered.size());

                if (dirty) {
                    clearScreen(terminal);
                    renderMenu(terminal, weapons, filtered, title, stats, state, allowFilters);
                    dirty = false;
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

                dirty = applyAction(action, state, filtered.size(), allowFilters);
            }
        } finally {
            terminal.setAttributes(originalAttributes);
            terminal.puts(Capability.keypad_local);
            terminal.puts(Capability.cursor_visible);
            terminal.flush();
        }
    }

    /**
     * Ajusta el cursor als límits de la llista filtrada.
     */
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

    /**
     * Aplica una acció de teclat.
     */
    private static boolean applyAction(Action action, State state, int filteredSize, boolean allowFilters) {
        switch (action) {
            case UP -> {
                if (filteredSize > 0 && state.cursor > 0) {
                    state.cursor--;
                    return true;
                }
                return false;
            }
            case DOWN -> {
                if (filteredSize > 0 && state.cursor < filteredSize - 1) {
                    state.cursor++;
                    return true;
                }
                return false;
            }
            case LEFT -> {
                if (!allowFilters) {
                    return false;
                }

                TypeFilter previous = state.typeFilter.previous();
                if (previous != state.typeFilter) {
                    state.typeFilter = previous;
                    state.cursor = 0;
                    return true;
                }
                return false;
            }
            case RIGHT -> {
                if (!allowFilters) {
                    return false;
                }

                TypeFilter next = state.typeFilter.next();
                if (next != state.typeFilter) {
                    state.typeFilter = next;
                    state.cursor = 0;
                    return true;
                }
                return false;
            }
            case TOGGLE_EQUIPPABLE -> {
                if (!allowFilters) {
                    return false;
                }

                state.onlyEquippable = !state.onlyEquippable;
                state.cursor = 0;
                return true;
            }
            case NONE, SELECT, QUIT -> {
                return false;
            }
        }

        return false;
    }

    /**
     * Renderitza el menú complet.
     */
    private static void renderMenu(
            Terminal terminal,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            String title,
            Statistics stats,
            State state,
            boolean allowFilters) {

        PrintWriter out = terminal.writer();

        out.println(Ansi.BOLD + safe(title) + Ansi.RESET);
        out.println();

        if (allowFilters) {
            String eq = state.onlyEquippable
                    ? Ansi.GREEN + "ON" + Ansi.RESET
                    : Ansi.DARK_GRAY + "OFF" + Ansi.RESET;

            out.println(
                    Ansi.DARK_GRAY + "Filtres:" + Ansi.RESET
                            + " [E] Equipables: " + eq
                            + "   [←/→ o A/D] Tipus: " + Ansi.BOLD + state.typeFilter.getLabel() + Ansi.RESET);
        } else {
            out.println(
                    Ansi.DARK_GRAY + "Navegació:" + Ansi.RESET
                            + " [↑/↓ o W/S] moure   [Enter] seleccionar   [Q] cancel·lar");
        }

        out.println();

        renderCompactList(out, weapons, filtered, stats, state.cursor);

        out.println();
        out.println(Ansi.DARK_GRAY + "────────────────────────────────────────────────────────" + Ansi.RESET);

        if (!filtered.isEmpty()) {
            FilteredItem selectedItem = filtered.get(state.cursor);
            WeaponDefinition selected = weapons.get(selectedItem.index());
            renderSelectedWeaponDetail(out, selected, stats, selectedItem.equippable());
        } else {
            out.println(Ansi.DARK_GRAY + "Sense selecció." + Ansi.RESET);
        }

        out.println(Ansi.DARK_GRAY + "────────────────────────────────────────────────────────" + Ansi.RESET);

        if (allowFilters) {
            out.println("[↑/↓ o W/S] moure   [←/→ o A/D] canviar tipus");
            out.println("[E] toggle equipables   [Enter] seleccionar   [Q] cancel·lar");
        } else {
            out.println("[↑/↓ o W/S] moure   [Enter] seleccionar   [Q] cancel·lar");
        }

        terminal.flush();
    }

    /**
     * Renderitza la llista superior en columnes fixes.
     */
    private static void renderCompactList(
            PrintWriter out,
            List<WeaponDefinition> weapons,
            List<FilteredItem> filtered,
            Statistics stats,
            int cursor) {

        if (filtered.isEmpty()) {
            out.println(Ansi.DARK_GRAY + "No hi ha armes amb aquests filtres." + Ansi.RESET);
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            FilteredItem item = filtered.get(i);
            WeaponDefinition weapon = weapons.get(item.index());

            String pointer = (i == cursor)
                    ? Ansi.CYAN + Ansi.BOLD + ">" + Ansi.RESET
                    : " ";

            String plainName = fixed(safe(weapon.getName()), NAME_WIDTH);
            String plainType = fixed("[" + shortTypeName(weapon.getType()) + "]", TYPE_WIDTH);

            out.print(pointer);
            out.print(" ");
            out.print(Ansi.WHITE + Ansi.BOLD + plainName + Ansi.RESET);
            out.print(" ");
            out.print(colorByType(weapon.getType()) + plainType + Ansi.RESET);

            if (stats != null) {
                String equipText = item.equippable() ? "EQUIPABLE" : "NO EQUIPABLE";
                String plainEquip = fixed(equipText, EQUIP_WIDTH);

                out.print(" ");
                out.print(item.equippable()
                        ? Ansi.GREEN + Ansi.BOLD + plainEquip + Ansi.RESET
                        : Ansi.DARK_GRAY + plainEquip + Ansi.RESET);
            }

            out.println();
        }
    }

    /**
     * Renderitza el panell inferior amb el detall de l'arma seleccionada.
     */
    private static void renderSelectedWeaponDetail(
            PrintWriter out,
            WeaponDefinition weapon,
            Statistics stats,
            boolean equippable) {

        if (weapon == null) {
            return;
        }

        out.print(Ansi.WHITE + Ansi.BOLD + safe(weapon.getName()) + Ansi.RESET);
        out.print(" ");
        out.print(colorByType(weapon.getType()) + "[" + typeName(weapon.getType()) + "]" + Ansi.RESET);

        if (stats != null) {
            out.print(" ");
            out.print(equippable
                    ? Ansi.GREEN + Ansi.BOLD + "(EQUIPABLE)" + Ansi.RESET
                    : Ansi.DARK_GRAY + "(NO EQUIPABLE)" + Ansi.RESET);
        }

        out.println();
        out.println();

        String desc = weapon.getDescription();
        if (desc != null && !desc.isBlank()) {
            for (String line : WRAP_CACHE.get(desc, DETAIL_WRAP_WIDTH)) {
                out.println(Ansi.DARK_GRAY + line + Ansi.RESET);
            }
            out.println();
        }

        out.print(Ansi.GREEN + "Dany: " + Ansi.RESET + Ansi.BOLD + weapon.getBaseDamage() + Ansi.RESET);
        out.print("   ");
        out.print(Ansi.YELLOW + "Crit: " + Ansi.RESET + Ansi.BOLD + roundPer(weapon.getCriticalProb()) + "%"
                + Ansi.RESET);
        out.print("   ");
        out.print(Ansi.YELLOW + "Mult: " + Ansi.RESET + Ansi.BOLD + "x" + round2(weapon.getCriticalDamage())
                + Ansi.RESET);
        out.print("   ");

        double manaPrice = weapon.getManaPrice();
        if (manaPrice > 0) {
            out.print(Ansi.BRIGHT_BLUE + "Mana: " + Ansi.RESET + Ansi.BOLD + Math.round(manaPrice) + Ansi.RESET);
        } else {
            out.print(Ansi.DARK_GRAY + "Mana: -" + Ansi.RESET);
        }

        out.println();
    }

    /**
     * Construeix la llista filtrada.
     */
    private static List<FilteredItem> buildFilteredItems(
            List<WeaponDefinition> weapons,
            Statistics stats,
            boolean onlyEquippable,
            TypeFilter typeFilter) {

        List<FilteredItem> out = new ArrayList<>();

        for (int i = 0; i < weapons.size(); i++) {
            WeaponDefinition weapon = weapons.get(i);
            WeaponType weaponType = (weapon == null) ? null : weapon.getType();

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

    /**
     * Crea el mapa de tecles del menú.
     */
    private static KeyMap<Action> buildKeyMap(Terminal terminal, boolean allowFilters) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.UP, "w", "W");
        map.bind(Action.DOWN, "s", "S");
        map.bind(Action.SELECT, "\r", "\n");
        map.bind(Action.QUIT, "q", "Q");

        if (allowFilters) {
            map.bind(Action.LEFT, "a", "A");
            map.bind(Action.RIGHT, "d", "D");
            map.bind(Action.TOGGLE_EQUIPPABLE, "e", "E");
        }

        String up = KeyMap.key(terminal, Capability.key_up);
        String down = KeyMap.key(terminal, Capability.key_down);

        if (up != null) {
            map.bind(Action.UP, up);
        }
        if (down != null) {
            map.bind(Action.DOWN, down);
        }

        if (allowFilters) {
            String left = KeyMap.key(terminal, Capability.key_left);
            String right = KeyMap.key(terminal, Capability.key_right);

            if (left != null) {
                map.bind(Action.LEFT, left);
            }
            if (right != null) {
                map.bind(Action.RIGHT, right);
            }
        }

        return map;
    }

    /**
     * Neteja la pantalla del terminal.
     */
    private static void clearScreen(Terminal terminal) {
        if (terminal.puts(Capability.clear_screen)) {
            terminal.flush();
            return;
        }

        terminal.writer().print("\033[H\033[2J");
        terminal.flush();
    }

    /**
     * Retorna el color segons el tipus d'arma.
     */
    private static String colorByType(WeaponType type) {
        if (type == null) {
            return Ansi.WHITE;
        }

        return switch (type) {
            case PHYSICAL -> Ansi.MAGENTA;
            case RANGE -> Ansi.BRIGHT_BLUE;
            case MAGICAL -> Ansi.ORANGE;
            default -> Ansi.WHITE;
        };
    }

    /**
     * Retorna el nom complet del tipus.
     */
    private static String typeName(WeaponType type) {
        return (type == null) ? "?" : safe(type.getName());
    }

    /**
     * Retorna el nom curt del tipus per a la llista compacta.
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
     * Retalla o omple un text fins a una amplada fixa.
     */
    private static String fixed(String text, int width) {
        return padRight(trimToWidth(text, width), width);
    }

    /**
     * Omple a la dreta fins a l'amplada indicada.
     */
    private static String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }

        if (text.length() >= width) {
            return text;
        }

        return text + " ".repeat(width - text.length());
    }

    /**
     * Retalla un text si supera l'amplada indicada.
     */
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

    /**
     * Evita nulls en textos.
     */
    private static String safe(String text) {
        return (text == null) ? "" : text;
    }

    /**
     * Arrodoneix a 2 decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    /**
     * Converteix una probabilitat a percentatge enter.
     */
    private static int roundPer(double n) {
        return (int) Math.round(n * 100.0);
    }

    private static void ensureWeapons(List<WeaponDefinition> weapons) {
        Objects.requireNonNull(weapons, "La llista d'armes no pot ser nul·la.");
    }
}