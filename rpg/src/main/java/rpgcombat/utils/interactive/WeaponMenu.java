package rpgcombat.utils.interactive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.input.Menu;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.config.WeaponType;

/**
 * Menú interactiu de selecció d'armes amb filtres, ordenació i navegació per
 * terminal.
 */
public final class WeaponMenu {

    /** Classe d'utilitat: no es pot instanciar. */
    private WeaponMenu() {
    }

    /**
     * Mostra el menú interactiu i retorna l'índex de l'arma seleccionada.
     *
     * @param weapons llista d'armes disponibles
     * @param title   títol del menú
     * @param stats   estadístiques del personatge per comprovar equipament
     * @return índex de l'arma seleccionada o {@code -1} si es cancel·la
     */
    public static int chooseWeaponWithFilters(List<WeaponDefinition> weapons, String title, Statistics stats) {
        if (weapons == null || weapons.isEmpty()) {
            Prettier.warn("No hi ha armes disponibles.");
            Menu.pause();
            return -1;
        }

        State state = new State();
        return runInteractiveMenu(weapons, title, stats, state);
    }

    /**
     * Mostra el menú interactiu i retorna directament l'arma escollida.
     *
     * @param weapons llista d'armes disponibles
     * @param title   títol del menú
     * @param stats   estadístiques del personatge
     * @return arma seleccionada o {@code null} si es cancel·la
     */
    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats) {

        ensureWeapons(weapons);
        int idx = chooseWeaponWithFilters(weapons, title, stats);
        return idx < 0 ? null : weapons.get(idx);
    }

    /**
     * Mostra el menú interactiu reutilitzant l'estat dels filtres.
     *
     * @param weapons llista d'armes disponibles
     * @param title   títol del menú
     * @param stats   estadístiques del personatge
     * @param state   estat extern dels filtres i l'ordenació
     * @return índex de l'arma seleccionada o {@code -1} si es cancel·la
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
        internal.viewportStart = 0;
        internal.onlyEquippable = state.isOnlyEquippable();
        internal.typeFilter = state.getTypeFilter();
        internal.sortCriterion = state.getSortCriterion();
        internal.sortAscending = state.isSortAscending();

        int result = runInteractiveMenu(weapons, title, stats, internal);

        state.setOnlyEquippable(internal.onlyEquippable);
        state.setTypeFilter(internal.typeFilter);
        state.setSortCriterion(internal.sortCriterion);
        state.setSortAscending(internal.sortAscending);

        return result;
    }

    /**
     * Mostra el menú interactiu reutilitzant l'estat dels filtres i retorna l'arma
     * escollida.
     *
     * @param weapons llista d'armes disponibles
     * @param title   títol del menú
     * @param stats   estadístiques del personatge
     * @param state   estat extern dels filtres i l'ordenació
     * @return arma seleccionada o {@code null} si es cancel·la
     */
    public static WeaponDefinition chooseWeaponEntryWithFilters(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            FilterState state) {

        ensureWeapons(weapons);
        int idx = chooseWeaponWithFilters(weapons, title, stats, state);
        return idx < 0 ? null : weapons.get(idx);
    }

    /** Estat persistent dels filtres i de l'ordenació del menú. */
    public static final class FilterState {
        private boolean onlyEquippable = false;
        private TypeFilter typeFilter = TypeFilter.ALL;
        private SortCriterion sortCriterion = SortCriterion.ORIGINAL;
        private boolean sortAscending = true;

        /**
         * Crea un estat buit amb valors per defecte.
         */
        public FilterState() {
        }

        /**
         * Crea un estat completament inicialitzat.
         *
         * @param onlyEquippable mostra només armes equipables
         * @param typeFilter     filtre de tipus
         * @param sortCriterion  criteri d'ordenació
         * @param sortAscending  direcció d'ordenació
         */
        public FilterState(
                boolean onlyEquippable,
                TypeFilter typeFilter,
                SortCriterion sortCriterion,
                boolean sortAscending) {

            this.onlyEquippable = onlyEquippable;
            this.typeFilter = typeFilter == null ? TypeFilter.ALL : typeFilter;
            this.sortCriterion = sortCriterion == null ? SortCriterion.ORIGINAL : sortCriterion;
            this.sortAscending = sortAscending;
        }

        /**
         * Indica si només s'han de mostrar armes equipables.
         *
         * @return {@code true} si només s'han de mostrar equipables
         */
        public boolean isOnlyEquippable() {
            return onlyEquippable;
        }

        /**
         * Defineix si només s'han de mostrar armes equipables.
         *
         * @param onlyEquippable nou valor del filtre
         */
        public void setOnlyEquippable(boolean onlyEquippable) {
            this.onlyEquippable = onlyEquippable;
        }

        /**
         * Retorna el filtre de tipus actual.
         *
         * @return filtre de tipus
         */
        public TypeFilter getTypeFilter() {
            return typeFilter;
        }

        /**
         * Defineix el filtre de tipus actual.
         *
         * @param typeFilter nou filtre de tipus
         */
        public void setTypeFilter(TypeFilter typeFilter) {
            this.typeFilter = typeFilter == null ? TypeFilter.ALL : typeFilter;
        }

        /**
         * Retorna el criteri d'ordenació actual.
         *
         * @return criteri d'ordenació
         */
        public SortCriterion getSortCriterion() {
            return sortCriterion;
        }

        /**
         * Defineix el criteri d'ordenació actual.
         *
         * @param sortCriterion nou criteri
         */
        public void setSortCriterion(SortCriterion sortCriterion) {
            this.sortCriterion = sortCriterion == null ? SortCriterion.ORIGINAL : sortCriterion;
        }

        /**
         * Indica si l'ordenació actual és ascendent.
         *
         * @return {@code true} si és ascendent
         */
        public boolean isSortAscending() {
            return sortAscending;
        }

        /**
         * Defineix la direcció d'ordenació.
         *
         * @param sortAscending {@code true} per ASC, {@code false} per DESC
         */
        public void setSortAscending(boolean sortAscending) {
            this.sortAscending = sortAscending;
        }
    }

    /** Tipus de filtre aplicable sobre les armes visibles. */
    public enum TypeFilter {
        ALL("Tots"),
        RANGE("Rang"),
        PHYSICAL("Físic"),
        MAGICAL("Màgic");

        private static final TypeFilter[] VALUES = values();

        private final String label;

        TypeFilter(String label) {
            this.label = label;
        }

        /**
         * Retorna l'etiqueta visible del filtre.
         *
         * @return text visible del filtre
         */
        public String getLabel() {
            return label;
        }

        /**
         * Comprova si aquest filtre accepta el tipus indicat.
         *
         * @param type tipus d'arma
         * @return {@code true} si el tipus passa el filtre
         */
        public boolean matches(WeaponType type) {
            if (this == ALL) {
                return true;
            }
            if (type == null) {
                return false;
            }

            return switch (this) {
                case RANGE -> type == WeaponType.RANGE;
                case PHYSICAL -> type == WeaponType.PHYSICAL;
                case MAGICAL -> type == WeaponType.MAGICAL;
                case ALL -> true;
            };
        }

        /**
         * Retorna el següent filtre de la seqüència circular.
         *
         * @return següent filtre
         */
        public TypeFilter next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        /**
         * Retorna el filtre anterior de la seqüència circular.
         *
         * @return filtre anterior
         */
        public TypeFilter previous() {
            return VALUES[(ordinal() - 1 + VALUES.length) % VALUES.length];
        }
    }

    /** Criteris d'ordenació disponibles per a la llista visible. */
    public enum SortCriterion {
        ORIGINAL("Original"),
        NAME("Nom"),
        TYPE("Tipus");

        private static final SortCriterion[] VALUES = values();

        private final String label;

        SortCriterion(String label) {
            this.label = label;
        }

        /**
         * Retorna l'etiqueta visible del criteri.
         *
         * @return nom visible del criteri
         */
        public String getLabel() {
            return label;
        }

        /**
         * Indica si aquest criteri permet direcció ASC/DESC.
         *
         * @return {@code true} si permet invertir direcció
         */
        public boolean supportsDirection() {
            return this == NAME || this == TYPE;
        }

        /**
         * Retorna el següent criteri d'ordenació.
         *
         * @return següent criteri
         */
        public SortCriterion next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    /** Element filtrat amb l'índex real i si és equipable. */
    static record FilteredItem(int index, boolean equippable) {
    }

    /** Estat intern mutable del menú durant l'execució. */
    static final class State {
        int cursor = 0;
        int viewportStart = 0;
        boolean onlyEquippable = false;
        TypeFilter typeFilter = TypeFilter.ALL;
        SortCriterion sortCriterion = SortCriterion.ORIGINAL;
        boolean sortAscending = true;
        boolean resizePending = false;
    }

    /** Accions disponibles des del teclat. */
    enum Action {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        TOGGLE_EQUIPPABLE,
        NEXT_SORT_CRITERION,
        TOGGLE_SORT_DIRECTION,
        SELECT,
        QUIT,
        NONE
    }

    /** Grau de repintat necessari després d'una acció. */
    enum RedrawMode {
        NONE, SELECTION_ONLY, FULL
    }

    /**
     * Executa el menú interactiu amb el terminal especialitzat.
     *
     * @param weapons llista d'armes
     * @param title   títol del menú
     * @param stats   estadístiques del personatge
     * @param state   estat intern del menú
     * @return índex seleccionat o {@code -1}
     */
    private static int runInteractiveMenu(
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state) {

        try (WeaponMenuTerminal ui = WeaponMenuTerminal.open(signal -> state.resizePending = true)) {
            return runMenuLoop(ui, weapons, title, stats, state);
        } catch (IOException e) {
            Prettier.warn("No s'ha pogut obrir el menú interactiu: %s", e.getMessage());
            Menu.pause();
            return -1;
        }
    }

    /**
     * Bucle principal del menú.
     *
     * @param ui      terminal del menú
     * @param weapons llista d'armes
     * @param title   títol visible
     * @param stats   estadístiques del personatge
     * @param state   estat intern mutable
     * @return índex seleccionat o {@code -1}
     */
    private static int runMenuLoop(
            WeaponMenuTerminal ui,
            List<WeaponDefinition> weapons,
            String title,
            Statistics stats,
            State state) {

        boolean firstRender = true;

        while (true) {
            boolean sizeChanged = ui.refreshSize();
            boolean fullRedrawNeeded = firstRender || state.resizePending || sizeChanged;

            List<FilteredItem> filtered = buildFilteredItems(
                    weapons,
                    stats,
                    state.onlyEquippable,
                    state.typeFilter,
                    state.sortCriterion,
                    state.sortAscending);

            normalizeCursor(state, filtered.size());

            int visibleRows = ui.computeVisibleRows();
            adjustViewport(state, filtered.size(), visibleRows);

            if (fullRedrawNeeded) {
                state.resizePending = false;
                ui.clearScreen();
                ui.renderMenu(weapons, filtered, title, stats, state, visibleRows);
                firstRender = false;
            }

            Action action = ui.readAction();
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

            RedrawMode redraw = applyAction(action, state, filtered.size());
            if (redraw == RedrawMode.NONE) {
                continue;
            }

            boolean postActionSizeChanged = ui.refreshSize();
            boolean forceFullRedraw = state.resizePending || postActionSizeChanged;

            List<FilteredItem> currentFiltered = buildFilteredItems(
                    weapons,
                    stats,
                    state.onlyEquippable,
                    state.typeFilter,
                    state.sortCriterion,
                    state.sortAscending);

            normalizeCursor(state, currentFiltered.size());

            int currentVisibleRows = ui.computeVisibleRows();
            adjustViewport(state, currentFiltered.size(), currentVisibleRows);

            if (redraw == RedrawMode.FULL || forceFullRedraw || state.viewportStart != oldViewportStart) {
                state.resizePending = false;
                ui.clearScreen();
                ui.renderMenu(weapons, currentFiltered, title, stats, state, currentVisibleRows);
                continue;
            }

            ui.redrawSelectionChange(
                    weapons,
                    currentFiltered,
                    stats,
                    state,
                    currentVisibleRows,
                    oldCursor,
                    state.cursor);
        }
    }

    /**
     * Normalitza la posició del cursor dins del rang vàlid.
     *
     * @param state        estat del menú
     * @param filteredSize mida de la llista visible
     */
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

    /**
     * Ajusta el viewport per assegurar que la selecció és visible.
     *
     * @param state        estat del menú
     * @param filteredSize nombre d'elements visibles
     * @param visibleRows  nombre màxim de files visibles
     */
    private static void adjustViewport(State state, int filteredSize, int visibleRows) {
        if (filteredSize <= 0) {
            state.viewportStart = 0;
            return;
        }

        int clampedVisibleRows = Math.max(1, visibleRows);
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

    /**
     * Aplica una acció de teclat sobre l'estat del menú.
     *
     * @param action       acció rebuda
     * @param state        estat actual del menú
     * @param filteredSize nombre d'elements visibles
     * @return nivell de repintat necessari
     */
    private static RedrawMode applyAction(Action action, State state, int filteredSize) {
        return switch (action) {
            case UP -> {
                if (filteredSize > 0 && state.cursor > 0) {
                    state.cursor--;
                    yield RedrawMode.SELECTION_ONLY;
                }
                yield RedrawMode.NONE;
            }
            case DOWN -> {
                if (filteredSize > 0 && state.cursor < filteredSize - 1) {
                    state.cursor++;
                    yield RedrawMode.SELECTION_ONLY;
                }
                yield RedrawMode.NONE;
            }
            case LEFT -> {
                TypeFilter previous = state.typeFilter.previous();
                if (previous != state.typeFilter) {
                    state.typeFilter = previous;
                    state.cursor = 0;
                    state.viewportStart = 0;
                    yield RedrawMode.FULL;
                }
                yield RedrawMode.NONE;
            }
            case RIGHT -> {
                TypeFilter next = state.typeFilter.next();
                if (next != state.typeFilter) {
                    state.typeFilter = next;
                    state.cursor = 0;
                    state.viewportStart = 0;
                    yield RedrawMode.FULL;
                }
                yield RedrawMode.NONE;
            }
            case TOGGLE_EQUIPPABLE -> {
                state.onlyEquippable = !state.onlyEquippable;
                state.cursor = 0;
                state.viewportStart = 0;
                yield RedrawMode.FULL;
            }
            case NEXT_SORT_CRITERION -> {
                state.sortCriterion = state.sortCriterion.next();
                state.cursor = 0;
                state.viewportStart = 0;
                yield RedrawMode.FULL;
            }
            case TOGGLE_SORT_DIRECTION -> {
                if (state.sortCriterion.supportsDirection()) {
                    state.sortAscending = !state.sortAscending;
                    state.cursor = 0;
                    state.viewportStart = 0;
                    yield RedrawMode.FULL;
                }
                yield RedrawMode.NONE;
            }
            case NONE, SELECT, QUIT -> RedrawMode.NONE;
        };
    }

    /**
     * Construeix la llista visible aplicant filtres i ordenació.
     *
     * @param weapons        armes disponibles
     * @param stats          estadístiques del personatge
     * @param onlyEquippable només mostrar equipables
     * @param typeFilter     filtre de tipus
     * @param sortCriterion  criteri d'ordenació
     * @param sortAscending  direcció ASC/DESC
     * @return llista visible preparada per al menú
     */
    private static List<FilteredItem> buildFilteredItems(
            List<WeaponDefinition> weapons,
            Statistics stats,
            boolean onlyEquippable,
            TypeFilter typeFilter,
            SortCriterion sortCriterion,
            boolean sortAscending) {

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

        sortFilteredItems(out, weapons, sortCriterion, sortAscending);
        return out;
    }

    /**
     * Ordena la llista visible segons el criteri i la direcció actuals.
     *
     * @param items          elements visibles
     * @param weapons        llista original d'armes
     * @param sortCriterion  criteri d'ordenació
     * @param sortAscending  direcció ASC/DESC
     */
    private static void sortFilteredItems(
            List<FilteredItem> items,
            List<WeaponDefinition> weapons,
            SortCriterion sortCriterion,
            boolean sortAscending) {

        if (items.size() <= 1 || sortCriterion == null || sortCriterion == SortCriterion.ORIGINAL) {
            return;
        }

        Comparator<FilteredItem> comparator = switch (sortCriterion) {
            case NAME -> Comparator
                    .comparing(
                            (FilteredItem item) -> safeWeaponName(weapons, item.index()),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(FilteredItem::index);

            case TYPE -> Comparator
                    .comparing((FilteredItem item) -> sortableTypeLabel(weapons, item.index()))
                    .thenComparing(
                            (FilteredItem item) -> safeWeaponName(weapons, item.index()),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(FilteredItem::index);

            case ORIGINAL -> Comparator.comparingInt(FilteredItem::index);
        };

        if (!sortAscending) {
            comparator = comparator.reversed();
        }

        items.sort(comparator);
    }

    /**
     * Retorna el nom segur d'una arma.
     *
     * @param weapons llista original
     * @param index   índex real
     * @return nom no nul
     */
    private static String safeWeaponName(List<WeaponDefinition> weapons, int index) {
        if (index < 0 || index >= weapons.size()) {
            return "";
        }

        WeaponDefinition weapon = weapons.get(index);
        if (weapon == null || weapon.getName() == null) {
            return "";
        }

        return weapon.getName();
    }

    /**
     * Retorna l'etiqueta usada per ordenar per tipus.
     *
     * @param weapons llista original
     * @param index   índex real
     * @return etiqueta normalitzada de tipus
     */
    private static String sortableTypeLabel(List<WeaponDefinition> weapons, int index) {
        if (index < 0 || index >= weapons.size()) {
            return "zzz";
        }

        WeaponDefinition weapon = weapons.get(index);
        WeaponType type = weapon == null ? null : weapon.getType();

        if (type == null) {
            return "zzz";
        }

        return switch (type) {
            case PHYSICAL -> "1_fisic";
            case RANGE -> "2_rang";
            case MAGICAL -> "3_magic";
            default -> safe(type.getName()).toLowerCase();
        };
    }

    /**
     * Comprova que la llista d'armes no sigui nul·la.
     *
     * @param weapons llista d'armes
     */
    private static void ensureWeapons(List<WeaponDefinition> weapons) {
        Objects.requireNonNull(weapons, "La llista d'armes no pot ser nul·la.");
    }

    /**
     * Precarrega el terminal compartit per evitar una primera inicialització tardana.
     *
     * @throws IOException si no es pot preparar el terminal
     */
    public static void preloadTerminal() throws IOException {
        WeaponMenuTerminal.preloadSharedTerminal();
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
}