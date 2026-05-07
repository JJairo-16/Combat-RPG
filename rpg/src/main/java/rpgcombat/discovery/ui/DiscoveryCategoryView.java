package rpgcombat.discovery.ui;

import java.util.List;

/** Model visual d'una categoria del visor temporal de descobriments. */
public record DiscoveryCategoryView(
        String title,
        String description,
        int discovered,
        int total,
        List<DiscoveryEntryView> entries) {
}
