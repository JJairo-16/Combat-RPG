package rpgcombat.terrain.model;

import java.util.List;

/**
 * Definició immutable d'un terreny de combat.
 *
 * @param id identificador estable del terreny
 * @param name nom que es mostra al jugador
 * @param shortDescription resum curt per als selectors
 * @param description descripció detallada del terreny
 * @param effectLines línies llegibles amb els efectes principals
 * @param rules regles del motor genèric, si el terreny no delega en un trigger
 * @param trigger trigger personalitzat, si el terreny no usa regles genèriques
 */
public record TerrainDefinition(
        String id,
        String name,
        String shortDescription,
        String description,
        List<String> effectLines,
        TerrainRulesDefinition rules,
        TerrainTriggerDefinition trigger) {

    public static final String NONE_ID = "NONE";

    /**
     * Normalitza els camps del terreny i valida la via d'execució triada.
     */
    public TerrainDefinition {
        id = requireId(id);
        name = fallback(name, id);
        shortDescription = shortDescription == null ? "" : shortDescription;
        description = description == null ? "" : description;
        effectLines = effectLines == null ? List.of() : List.copyOf(effectLines);
        if (rules != null && trigger != null) {
            throw new IllegalArgumentException("El terreny " + id + " no pot declarar rules i trigger alhora.");
        } else if (rules == null && trigger == null && !NONE_ID.equals(id)) {
            throw new IllegalArgumentException("El terreny " + id + " ha de declarar rules o trigger.");
        }
    }

    /**
     * Indica si aquesta definició representa l'absència de terreny.
     *
     * @return {@code true} quan no s'ha d'aplicar cap efecte d'escenari
     */
    public boolean isNone() {
        return NONE_ID.equals(id);
    }

    /**
     * Crea la definició neutra que manté el combat sense modificadors de terreny.
     *
     * @return terreny neutre
     */
    public static TerrainDefinition none() {
        return new TerrainDefinition(
                NONE_ID,
                "Cap terreny",
                "Combat sense modificadors d'escenari.",
                "No s'aplica cap efecte global. El combat funciona amb les regles del mode seleccionat.",
                List.of("Sense efectes addicionals."),
                null,
                null);
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El terreny necessita un id.");
        }
        return value.trim().toUpperCase();
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
