package rpgcombat.discovery.ui.models;

import java.util.List;

/** Model visual d'una entrada del visor temporal de descobriments. */
public record DiscoveryEntryView(
        String title,
        boolean discovered,
        String shortDescription,
        List<String> details,
        String discoveredWhen,
        String hint) {
}
