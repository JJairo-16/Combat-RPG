package rpgcombat.discovery.ui;

import java.util.List;

import rpgcombat.utils.input.Menu;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.utils.ui.Cleaner;
import rpgcombat.utils.ui.Prettier;

/** Visor temporal en format de llista mentre no existeix el visor definitiu. */
public final class DiscoveryListViewer {
    private DiscoveryListViewer() {
    }

    /** Mostra tots els descobriments en una llista agrupada i fa una pausa. */
    public static void show(DiscoveryOverview overview) {
        new Cleaner().clear();
        if (overview == null) {
            Prettier.warn("No hi ha cap catàleg de descobriments carregat.");
            Menu.pause();
            return;
        }

        StringBuilder sb = new StringBuilder(16_000);
        appendHeader(sb, overview);

        for (DiscoveryCategoryView category : overview.categories()) {
            appendCategory(sb, category);
        }

        System.out.print(sb.toString());
        Menu.pause();
    }

    private static void appendHeader(StringBuilder sb, DiscoveryOverview overview) {
        sb.append(Ansi.BOLD).append("DESCOBRIMENTS").append(Ansi.RESET).append('\n');
        sb.append("Total descobert: ")
                .append(Ansi.CYAN).append(overview.discovered()).append(" / ").append(overview.total())
                .append(Ansi.RESET)
                .append("\n\n");
    }

    private static void appendCategory(StringBuilder sb, DiscoveryCategoryView category) {
        sb.append(Ansi.BOLD)
                .append(category.title())
                .append(Ansi.RESET)
                .append("  ")
                .append(Ansi.DARK_GRAY)
                .append("(")
                .append(category.discovered())
                .append(" / ")
                .append(category.total())
                .append(")")
                .append(Ansi.RESET)
                .append('\n');

        if (category.description() != null && !category.description().isBlank()) {
            sb.append("  ").append(Ansi.DARK_GRAY).append(category.description()).append(Ansi.RESET).append('\n');
        }

        List<DiscoveryEntryView> entries = category.entries();
        if (entries == null || entries.isEmpty()) {
            sb.append("  ").append(Ansi.DARK_GRAY).append("Encara no hi ha entrades en aquesta categoria.")
                    .append(Ansi.RESET).append("\n\n");
            return;
        }

        for (DiscoveryEntryView entry : entries) {
            appendEntry(sb, entry);
        }
        sb.append('\n');
    }

    private static void appendEntry(StringBuilder sb, DiscoveryEntryView entry) {
        String marker = entry.discovered() ? "*" : "-";
        String color = entry.discovered() ? Ansi.GREEN : Ansi.DARK_GRAY;
        sb.append("  ").append(color).append(marker).append(Ansi.RESET).append(' ')
                .append(entry.discovered() ? Ansi.WHITE : Ansi.DARK_GRAY)
                .append(entry.title())
                .append(Ansi.RESET);

        sb.append('\n');

        if (entry.discovered()) {
            appendDiscoveredDetails(sb, entry);
        } else if (entry.hint() != null && !entry.hint().isBlank()) {
            sb.append("    ").append(Ansi.DARK_GRAY).append("Pista: ").append(entry.hint()).append(Ansi.RESET)
                    .append('\n');
        }
    }

    private static void appendDiscoveredDetails(StringBuilder sb, DiscoveryEntryView entry) {
        if (entry.shortDescription() != null && !entry.shortDescription().isBlank()) {
            sb.append("    ").append(entry.shortDescription()).append('\n');
        }

        if (entry.details() != null) {
            for (String line : entry.details()) {
                if (line == null || line.isBlank())
                    continue;
                if (entry.shortDescription() != null && line.trim().equals(entry.shortDescription().trim()))
                    continue;
                sb.append("    ").append(Ansi.DARK_GRAY).append("- ").append(Ansi.RESET).append(line).append('\n');
            }
        }

        if (entry.discoveredWhen() != null && !entry.discoveredWhen().isBlank()) {
            sb.append("    ").append(Ansi.DARK_GRAY).append("Descoberta: ").append(entry.discoveredWhen())
                    .append(Ansi.RESET).append('\n');
        }
    }
}
