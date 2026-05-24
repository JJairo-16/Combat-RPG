package rpgcombat.gamemode.model;

import rpgcombat.terrain.model.TerrainDefinition;

/** Context immutable de regles seleccionades per a una partida concreta. */
public record MatchContext(
        GameModeDefinition mode,
        boolean chaosActive,
        TerrainDefinition terrain) {

    public MatchContext {
        mode = mode == null ? GameModeDefinition.normal() : mode;
        terrain = terrain == null ? TerrainDefinition.none() : terrain;
    }

    public MatchContext(GameModeDefinition mode, boolean chaosActive) {
        this(mode, chaosActive, TerrainDefinition.none());
    }

    public GameModeRules rules() {
        return mode.rules();
    }
}
