package rpgcombat.gamemode.model;

import java.util.List;

/** Textos de presentació que el menú de modes renderitza com a cartes. */
public record GameModePresentation(
        String shortDescription,
        List<String> details,
        String lockedTitle,
        String lockedDescription,
        List<String> lockedHints) {

    public GameModePresentation {
        shortDescription = shortDescription == null ? "" : shortDescription.trim();
        details = normalize(details);
        lockedTitle = blankToDefault(lockedTitle, "???");
        lockedDescription = blankToDefault(lockedDescription, "Camí velat");
        lockedHints = normalize(lockedHints);
    }

    public static GameModePresentation fallback(String description) {
        return new GameModePresentation(
                description,
                List.of(),
                "???",
                "Camí velat",
                List.of("La senda encara no ha revelat el seu preu."));
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
