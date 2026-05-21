package rpgcombat.terrain.model;

/**
 * Forma en què es tria el terreny d'una partida.
 */
public enum TerrainSelectionMode {
    NONE,
    MANUAL,
    RANDOM;

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
        TerrainSelectionMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Retrocedeix al mode anterior del selector d'ajustos.
     *
     * @return mode anterior
     */
    public TerrainSelectionMode previous() {
        TerrainSelectionMode[] values = values();
        return values[Math.floorMod(ordinal() - 1, values.length)];
    }

    /**
     * Retorna l'etiqueta curta que es mostra als ajustos.
     *
     * @return etiqueta del mode
     */
    public String label() {
        return switch (this) {
            case NONE -> "Sense terreny";
            case MANUAL -> "Manual";
            case RANDOM -> "A l'atzar";
        };
    }
}
