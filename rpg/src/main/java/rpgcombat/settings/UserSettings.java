package rpgcombat.settings;

import rpgcombat.terrain.model.TerrainSelectionMode;

/** Ajustos persistents de l'usuari. */
public record UserSettings(
        boolean showMomentumMessages,
        TerrainSelectionMode terrainSelectionMode) {

    public UserSettings {
        terrainSelectionMode = terrainSelectionMode == null ? TerrainSelectionMode.NONE : terrainSelectionMode;
    }

    /** Constructor compatible amb ajustos antics. */
    public UserSettings(boolean showMomentumMessages) {
        this(showMomentumMessages, TerrainSelectionMode.NONE);
    }

    /** Retorna els ajustos inicials quan encara no hi ha cap fitxer desat. */
    public static UserSettings defaults() {
        return new UserSettings(true, TerrainSelectionMode.NONE);
    }
}
