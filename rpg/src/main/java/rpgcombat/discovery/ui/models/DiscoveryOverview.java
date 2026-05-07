package rpgcombat.discovery.ui.models;

import java.util.List;

/** Model visual complet del visor temporal de descobriments. */
public record DiscoveryOverview(
        int discovered,
        int total,
        List<DiscoveryCategoryView> categories) {
}
