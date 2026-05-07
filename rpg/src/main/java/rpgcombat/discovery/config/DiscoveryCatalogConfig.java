package rpgcombat.discovery.config;

import java.util.List;

/** Configuració arrel del catàleg de descobriments. */
public record DiscoveryCatalogConfig(
        int version,
        List<DiscoveryCategoryConfig> categories,
        List<DiscoveryEntryConfig> entries) {
}
