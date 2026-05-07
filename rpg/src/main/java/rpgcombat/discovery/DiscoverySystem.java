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
import rpgcombat.discovery.ui.DiscoveryCategoryView;
import rpgcombat.discovery.ui.DiscoveryEntryView;
import rpgcombat.discovery.ui.DiscoveryOverview;
import rpgcombat.utils.ui.Prettier;

/** Coordina catàleg, progrés i persistència dels descobriments globals. */
public final class DiscoverySystem {
    private final DiscoveryCatalog catalog;
    private final DiscoveryStore store;
    private final Path savePath;
    private final Map<DiscoveryKey, DiscoveryProgress> progressByKey;
    private boolean dirty;

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

    /** Registra una entrada descoberta si existeix al catàleg. */
    public void discover(DiscoveryCategory category, String id) {
        if (category == null || id == null || id.isBlank() || catalog == null) return;
        if (!catalog.contains(category, id)) return;

        DiscoveryKey key = new DiscoveryKey(category, id);
        DiscoveryProgress progress = progressByKey.get(key);
        if (progress == null) {
            progressByKey.put(key, DiscoveryProgress.newlyDiscovered(key));
        }
        dirty = true;
        saveIfDirty();
    }

    /** Converteix el progrés intern a models visuals per al visor temporal. */
    public DiscoveryOverview toOverview() {
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
    public void saveIfDirty() {
        if (!dirty) return;
        try {
            store.save(savePath, progressByKey.values());
            dirty = false;
        } catch (IOException e) {
            Prettier.warn("No s'ha pogut desar el progrés dels descobriments: " + e.getMessage());
        }
    }

    /** Retorna una còpia del progrés actual, útil per proves. */
    public Collection<DiscoveryProgress> progress() {
        return List.copyOf(progressByKey.values());
    }

    private DiscoveryEntryView toEntryView(DiscoveryEntryDefinition definition, DiscoveryProgress progress) {
        boolean discovered = progress != null;
        if (!discovered) {
            return new DiscoveryEntryView(
                    definition.lockedTitle(),
                    false,
                    "",
                    List.of(),
                    "",
                    definition.hint());
        }

        return new DiscoveryEntryView(
                definition.title(),
                true,
                definition.shortDescription(),
                definition.description(),
                definition.discoveredWhen(),
                definition.hint());
    }
}
