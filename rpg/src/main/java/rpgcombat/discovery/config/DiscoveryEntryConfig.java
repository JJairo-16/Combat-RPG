package rpgcombat.discovery.config;

import java.util.List;

/** Entrada manual o override d'una entrada automàtica del catàleg. */
public record DiscoveryEntryConfig(
        String category,
        String id,
        String title,
        String lockedTitle,
        String shortDescription,
        List<String> description,
        String discoveredWhen,
        String hint,
        List<String> tags,
        Boolean hiddenUntilDiscovered,
        Integer sortOrder) {
}
