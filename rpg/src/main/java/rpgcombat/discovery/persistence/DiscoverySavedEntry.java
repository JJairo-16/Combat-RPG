package rpgcombat.discovery.persistence;

/** Entrada descoberta desada a l'AppData. */
public record DiscoverySavedEntry(
        String category,
        String id,
        String firstDiscoveredAt) {
}
