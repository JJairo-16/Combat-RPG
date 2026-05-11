package rpgcombat.discovery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rpgcombat.discovery.config.DiscoveryCatalog;
import rpgcombat.discovery.config.DiscoveryCategoryDefinition;
import rpgcombat.discovery.config.DiscoveryEntryDefinition;
import rpgcombat.discovery.persistence.DiscoveryStore;
import rpgcombat.discovery.ui.models.DiscoveryCategoryView;
import rpgcombat.discovery.ui.models.DiscoveryEntryView;
import rpgcombat.discovery.ui.models.DiscoveryOverview;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.unlocks.UnlockRuntime;

/** Coordina catàleg, progrés i persistència dels descobriments globals. */
public final class DiscoverySystem {
    private DiscoveryCatalog catalog;
    private final DiscoveryStore store;
    private final Path savePath;
    private final Map<DiscoveryKey, DiscoveryProgress> progressByKey;
    private boolean dirty;

    /** Crea el sistema amb el progrés carregat. */
    private DiscoverySystem(DiscoveryCatalog catalog, DiscoveryStore store, Path savePath,
            Map<DiscoveryKey, DiscoveryProgress> progressByKey) {
        this.catalog = catalog;
        this.store = store;
        this.savePath = savePath;
        this.progressByKey = new LinkedHashMap<>(progressByKey);
    }

    /** Crea el sistema carregant el progrés global des de l'AppData. */
    public static DiscoverySystem load(DiscoveryCatalog catalog, String configuredSavePath) {
        DiscoveryStore store = new DiscoveryStore();
        Path savePath = store.resolveAppDataPath(configuredSavePath);
        Map<DiscoveryKey, DiscoveryProgress> progress = store.load(savePath);
        DiscoverySystem system = new DiscoverySystem(catalog, store, savePath, progress);
        DiscoveryRuntime.configure(system);
        return system;
    }

    /** Indica si el catàleg visual i de validació ja està disponible. */
    public synchronized boolean hasCatalog() {
        return catalog != null;
    }

    /** Assigna el catàleg quan la resta de registres ja han estat carregats. */
    public synchronized void setCatalog(DiscoveryCatalog catalog) {
        if (catalog != null) {
            this.catalog = catalog;
        }
    }

    /** Registra una entrada descoberta si existeix al catàleg. */
    public synchronized void discover(DiscoveryCategory category, String id) {
        if (category == null || id == null || id.isBlank() || catalog == null) return;
        if (!catalog.contains(category, id)) return;

        DiscoveryKey key = new DiscoveryKey(category, id);
        DiscoveryProgress progress = progressByKey.get(key);
        if (progress != null) {
            return;
        }

        progressByKey.put(key, DiscoveryProgress.newlyDiscovered(key));
        dirty = true;
        saveIfDirty();
    }

    /** Converteix el progrés intern a models visuals per al visor temporal. */
    public synchronized DiscoveryOverview toOverview() {
        if (catalog == null) {
            return new DiscoveryOverview(0, 0, List.of());
        }

        List<DiscoveryCategoryView> categories = new ArrayList<>();
        int total = 0;
        int discovered = 0;

        for (DiscoveryCategoryDefinition category : catalog.categories()) {
            List<DiscoveryEntryDefinition> definitions = catalog.entries(category.id());
            int categoryTotal = definitions.size();
            int categoryDiscovered = 0;
            List<DiscoveryEntryView> entries = new ArrayList<>();

            for (DiscoveryEntryDefinition definition : definitions) {
                DiscoveryProgress progress = progressByKey.get(definition.key());
                boolean isDiscovered = progress != null;
                if (isDiscovered) categoryDiscovered++;

                if (!isDiscovered && definition.hiddenUntilDiscovered() && !category.showLockedEntries()) {
                    continue;
                }

                entries.add(toEntryView(definition, progress));
            }

            total += categoryTotal;
            discovered += categoryDiscovered;
            categories.add(new DiscoveryCategoryView(
                    category.title(),
                    category.description(),
                    categoryDiscovered,
                    categoryTotal,
                    List.copyOf(entries)));
        }

        return new DiscoveryOverview(discovered, total, List.copyOf(categories));
    }

    /** Desa el progrés si hi ha canvis pendents. */
    public synchronized void saveIfDirty() {
        if (!dirty) return;
        try {
            store.save(savePath, progressByKey.values());
            dirty = false;
        } catch (IOException e) {
            Prettier.warn("No s'ha pogut desar el progrés dels descobriments: " + e.getMessage());
        }
    }

    /** Retorna una còpia del progrés actual, útil per proves. */
    public synchronized Collection<DiscoveryProgress> progress() {
        return List.copyOf(progressByKey.values());
    }

    /** Indica si una entrada concreta ja ha estat descoberta. */
    public synchronized boolean isDiscovered(DiscoveryCategory category, String id) {
        if (category == null || id == null || id.isBlank()) {
            return false;
        }
        return progressByKey.containsKey(new DiscoveryKey(category, id));
    }

    /** Nombre total d'entrades descobertes. */
    public synchronized int discoveredCount() {
        return progressByKey.size();
    }

    /** Nombre d'entrades descobertes dins una categoria. */
    public synchronized int discoveredCount(DiscoveryCategory category) {
        if (category == null) {
            return 0;
        }
        int count = 0;
        for (DiscoveryKey key : progressByKey.keySet()) {
            if (key.category() == category) {
                count++;
            }
        }
        return count;
    }

    /** Converteix una definició en model visual d'entrada. */
    private DiscoveryEntryView toEntryView(DiscoveryEntryDefinition definition, DiscoveryProgress progress) {
        boolean discovered = progress != null;
        if (!discovered) {
            return new DiscoveryEntryView(
                    definition.lockedTitle(),
                    false,
                    "",
                    List.of(),
                    "",
                    lockedHint(definition));
        }

        return new DiscoveryEntryView(
                definition.title(),
                true,
                definition.shortDescription(),
                definition.description(),
                definition.discoveredWhen(),
                definition.hint());
    }

    /** Tria la pista bloquejada segons si una arma ja està disponible per triar-se. */
    private String lockedHint(DiscoveryEntryDefinition definition) {
        if (definition.category() != DiscoveryCategory.WEAPONS) {
            return definition.hint();
        }

        boolean available = UnlockRuntime.isWeaponAvailable(definition.id());
        String hint = available ? definition.discoveryHint() : definition.unlockHint();
        if (hint == null || hint.isBlank()) {
            return definition.hint();
        }
        return hint;
    }
}
