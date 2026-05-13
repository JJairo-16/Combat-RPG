package rpgcombat.discovery.config;

import java.util.List;

import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryKey;

/** Definició final, ja barrejada amb overrides, d'una entrada del catàleg. */
public record DiscoveryEntryDefinition(
        DiscoveryCategory category,
        String id,
        String title,
        String lockedTitle,
        String shortDescription,
        List<String> description,
        String discoveredWhen,
        String hint,
        String unlockHint,
        String discoveryHint,
        List<String> tags,
        boolean hiddenUntilDiscovered,
        int sortOrder) {

    /** Normalitza els valors nuls o buits de l'entrada. */
    public DiscoveryEntryDefinition {
        if (title == null || title.isBlank()) title = id;
        if (lockedTitle == null || lockedTitle.isBlank()) lockedTitle = "???";
        shortDescription = shortDescription == null ? "" : shortDescription;
        description = description == null ? List.of() : List.copyOf(description);
        discoveredWhen = discoveredWhen == null ? "" : discoveredWhen;
        hint = hint == null ? "" : hint;
        unlockHint = unlockHint == null || unlockHint.isBlank() ? hint : unlockHint;
        discoveryHint = discoveryHint == null || discoveryHint.isBlank() ? hint : discoveryHint;
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /** Clau estable de l'entrada. */
    public DiscoveryKey key() {
        return new DiscoveryKey(category, id);
    }

    /** Aplica un override manual sobre aquesta entrada. */
    public DiscoveryEntryDefinition withOverride(DiscoveryEntryConfig override) {
        if (override == null) return this;
        return new DiscoveryEntryDefinition(
                category,
                id,
                textOr(override.title(), title),
                textOr(override.lockedTitle(), lockedTitle),
                textOr(override.shortDescription(), shortDescription),
                override.description() == null ? description : override.description(),
                textOr(override.discoveredWhen(), discoveredWhen),
                textOr(override.hint(), hint),
                textOr(override.unlockHint(), unlockHint),
                textOr(override.discoveryHint(), discoveryHint),
                override.tags() == null ? tags : override.tags(),
                override.hiddenUntilDiscovered() == null ? hiddenUntilDiscovered : override.hiddenUntilDiscovered(),
                override.sortOrder() == null ? sortOrder : override.sortOrder());
    }

    /** Retorna el text o el valor alternatiu. */
    private static String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
