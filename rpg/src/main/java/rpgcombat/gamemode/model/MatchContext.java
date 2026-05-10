package rpgcombat.gamemode.model;

/** Context immutable de regles seleccionades per a una partida concreta. */
public record MatchContext(
        GameModeDefinition mode,
        boolean chaosActive) {

    public MatchContext {
        mode = mode == null ? GameModeDefinition.normal() : mode;
    }

    public GameModeRules rules() {
        return mode.rules();
    }
}
