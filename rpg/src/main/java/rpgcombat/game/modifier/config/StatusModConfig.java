package rpgcombat.game.modifier.config;

public record StatusModConfig(
    int priority,
    Integer minCharges,
    Integer maxCharges,
    Integer minStacks,
    Integer maxStacks,
    Integer minRemainingTurns,
    Integer maxRemainingTurns,
    String label,
    String actionKey
) {}