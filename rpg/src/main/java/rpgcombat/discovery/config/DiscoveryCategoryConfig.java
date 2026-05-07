package rpgcombat.discovery.config;

/** Configuració visual d'una categoria de descobriments. */
public record DiscoveryCategoryConfig(
        String id,
        String title,
        String description,
        Boolean showLockedEntries,
        Integer sortOrder) {
}
