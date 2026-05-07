package rpgcombat.discovery.config;

import rpgcombat.discovery.DiscoveryCategory;

/** Definició final d'una categoria del catàleg. */
public record DiscoveryCategoryDefinition(
        DiscoveryCategory id,
        String title,
        String description,
        boolean showLockedEntries,
        int sortOrder) {
}
