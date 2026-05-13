package rpgcombat.discovery.ui.state;

import static rpgcombat.discovery.ui.filter.DiscoveryFilter.hasFilters;
import static rpgcombat.discovery.ui.render.DiscoveryText.clamp;

import java.util.ArrayList;
import java.util.List;

import rpgcombat.discovery.ui.filter.DiscoveryFilter;
import rpgcombat.discovery.ui.models.DiscoveryCategoryView;
import rpgcombat.discovery.ui.models.DiscoveryEntryView;
import rpgcombat.discovery.ui.models.DiscoveryOverview;

/** Estat mutable de navegació i filtratge del visor. */
public final class DiscoveryUiState {
    private int selectedCategory;
    private int selectedEntry;
    private int categoryScroll;
    private int entryScroll;
    private Panel activePanel = Panel.CATEGORIES;
    private boolean categoriesCollapsed;
    private DiscoveryFilter activeFilter = DiscoveryFilter.ALL;

    /** Panells navegables del visor. */
    public enum Panel {
        /** Panell de categories. */
        CATEGORIES,

        /** Panell d'entrades. */
        ENTRIES
    }

    /** Índex de la categoria seleccionada. */
    public int selectedCategory() {
        return selectedCategory;
    }

    /** Índex de l'entrada seleccionada. */
    public int selectedEntry() {
        return selectedEntry;
    }

    /** Desplaçament vertical de categories. */
    public int categoryScroll() {
        return categoryScroll;
    }

    /** Desplaçament vertical d'entrades. */
    public int entryScroll() {
        return entryScroll;
    }

    /** Panell actiu actual. */
    public Panel activePanel() {
        return activePanel;
    }

    /** Indica si les categories estan plegades. */
    public boolean categoriesCollapsed() {
        return categoriesCollapsed;
    }

    /** Filtre actiu actual. */
    public DiscoveryFilter activeFilter() {
        return activeFilter;
    }

    /** Mou la selecció cap amunt. */
    public void moveUp() {
        if (activePanel == Panel.CATEGORIES) {
            selectedCategory = Math.max(0, selectedCategory - 1);
            selectedEntry = 0;
            entryScroll = 0;
            return;
        }

        selectedEntry = Math.max(0, selectedEntry - 1);
    }

    /** Mou la selecció cap avall. */
    public void moveDown(DiscoveryOverview overview) {
        if (activePanel == Panel.CATEGORIES) {
            selectedCategory = clamp(
                    selectedCategory + 1,
                    0,
                    Math.max(0, safeCategories(overview).size() - 1));
            selectedEntry = 0;
            entryScroll = 0;
            return;
        }

        DiscoveryCategoryView category = selectedCategoryView(overview);
        int max = category == null ? 0 : Math.max(0, filteredEntries(category).size() - 1);
        selectedEntry = Math.min(max, selectedEntry + 1);
    }

    /** Mou el focus cap a l'esquerra. */
    public void moveLeft() {
        if (activePanel == Panel.ENTRIES && categoriesCollapsed) {
            categoriesCollapsed = false;
            return;
        }

        if (activePanel == Panel.ENTRIES) {
            activePanel = Panel.CATEGORIES;
        }
    }

    /** Mou el focus cap a la dreta o plega categories. */
    public void moveRight() {
        if (activePanel == Panel.CATEGORIES) {
            activePanel = Panel.ENTRIES;
            return;
        }

        categoriesCollapsed = true;
    }

    /** Activa el filtre següent de la categoria. */
    public void nextFilter(DiscoveryOverview overview) {
        DiscoveryCategoryView category = selectedCategoryView(overview);
        if (!hasFilters(category)) {
            activeFilter = DiscoveryFilter.ALL;
            return;
        }

        activeFilter = DiscoveryFilter.next(category, activeFilter);
        selectedEntry = 0;
        entryScroll = 0;
    }

    /** Activa el filtre anterior de la categoria. */
    public void previousFilter(DiscoveryOverview overview) {
        DiscoveryCategoryView category = selectedCategoryView(overview);
        if (!hasFilters(category)) {
            activeFilter = DiscoveryFilter.ALL;
            return;
        }

        activeFilter = DiscoveryFilter.previous(category, activeFilter);
        selectedEntry = 0;
        entryScroll = 0;
    }

    /** Ajusta l'estat als límits de les dades actuals. */
    public void normalise(DiscoveryOverview overview) {
        List<DiscoveryCategoryView> categories = safeCategories(overview);
        selectedCategory = clamp(selectedCategory, 0, Math.max(0, categories.size() - 1));

        DiscoveryCategoryView category = selectedCategoryView(overview);
        resetFilterIfNeeded(category);

        int entryCount = category == null ? 0 : filteredEntries(category).size();
        selectedEntry = clamp(selectedEntry, 0, Math.max(0, entryCount - 1));

        if (categories.isEmpty()) {
            activePanel = Panel.CATEGORIES;
            categoriesCollapsed = false;
            activeFilter = DiscoveryFilter.ALL;
        }
    }

    /** Actualitza els desplaçaments visibles. */
    public void updateScrolls(int categoryCount, int entryCount, int visibleRows) {
        categoryScroll = scrollFor(selectedCategory, categoryScroll, visibleRows, categoryCount);
        entryScroll = scrollFor(selectedEntry, entryScroll, visibleRows, entryCount);
    }

    /** Retorna la categoria seleccionada. */
    public DiscoveryCategoryView selectedCategoryView(DiscoveryOverview overview) {
        List<DiscoveryCategoryView> categories = safeCategories(overview);
        if (categories.isEmpty()) {
            return null;
        }
        return categories.get(clamp(selectedCategory, 0, categories.size() - 1));
    }

    /** Retorna l'entrada seleccionada. */
    public DiscoveryEntryView selectedEntryView(List<DiscoveryEntryView> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.get(clamp(selectedEntry, 0, entries.size() - 1));
    }

    /** Retorna les entrades filtrades d'una categoria. */
    public List<DiscoveryEntryView> filteredEntries(DiscoveryCategoryView category) {
        List<DiscoveryEntryView> entries = safeEntries(category);

        if (entries.isEmpty()) {
            return entries;
        }

        if (!hasFilters(category)) {
            return entries;
        }

        resetFilterIfNeeded(category);

        if (activeFilter == DiscoveryFilter.ALL) {
            return entries;
        }

        List<DiscoveryEntryView> filtered = new ArrayList<>();
        for (DiscoveryEntryView entry : entries) {
            if (activeFilter.matches(entry)) {
                filtered.add(entry);
            }
        }

        return filtered;
    }

    /** Reinicia el filtre si no és vàlid. */
    public void resetFilterIfNeeded(DiscoveryCategoryView category) {
        if (!hasFilters(category)) {
            activeFilter = DiscoveryFilter.ALL;
            return;
        }

        if (!activeFilter.availableFor(category)) {
            activeFilter = DiscoveryFilter.ALL;
        }
    }

    /** Retorna categories segures, mai nul·les. */
    public static List<DiscoveryCategoryView> safeCategories(DiscoveryOverview overview) {
        return overview == null || overview.categories() == null ? List.of() : overview.categories();
    }

    /** Retorna entrades segures, mai nul·les. */
    static List<DiscoveryEntryView> safeEntries(DiscoveryCategoryView category) {
        return category == null || category.entries() == null ? List.of() : category.entries();
    }

    /** Calcula el desplaçament necessari per veure la selecció. */
    private static int scrollFor(int selected, int scroll, int visibleRows, int total) {
        if (total <= 0) {
            return 0;
        }

        int safeRows = Math.max(1, visibleRows);
        int maxScroll = Math.max(0, total - safeRows);
        int next = clamp(scroll, 0, maxScroll);

        if (selected < next) {
            next = selected;
        } else if (selected >= next + safeRows) {
            next = selected - safeRows + 1;
        }

        return clamp(next, 0, maxScroll);
    }
}