package rpgcombat.terrain.model;

/**
 * Forma en què es tria el terreny d'una partida.
 */
public enum TerrainSelectionMode {
    NONE("Sense terreny"),
    MANUAL("Manual"),
    RANDOM("A l'atzar");

    private final String label;

    TerrainSelectionMode(String label) {
        this.label = label;
    }

    private static final TerrainSelectionMode[] VALUES = values();

    /**
     * Converteix un valor persistent en un mode de selecció.
     *
     * @param value valor textual llegit de la configuració
     * @param fallback mode usat quan el valor no és vàlid
     * @return mode resolt
     */
    public static TerrainSelectionMode from(String value, TerrainSelectionMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? NONE : fallback;
        }
        try {
            return TerrainSelectionMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback == null ? NONE : fallback;
        }
    }

    /**
     * Avança al mode següent del selector d'ajustos.
     *
     * @return mode següent
     */
    public TerrainSelectionMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /**
     * Retrocedeix al mode anterior del selector d'ajustos.
     *
     * @return mode anterior
     */
    public TerrainSelectionMode previous() {
        return VALUES[Math.floorMod(ordinal() - 1, VALUES.length)];
    }

    /**
     * Retorna l'etiqueta curta que es mostra als ajustos.
     *
     * @return etiqueta del mode
     */
    public String label() {
        return label;
    }
}
