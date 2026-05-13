package rpgcombat.gamemode.effects;

import java.util.Map;

/** Definició declarativa d'un efecte inicial aplicat per un mode. */
public record ModeEffectDefinition(
        String id,
        ModeEffectTarget target,
        Map<String, Double> parameters) {

    public ModeEffectDefinition {
        id = requireText(id);
        target = target == null ? ModeEffectTarget.BOTH : target;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("L'efecte de mode requereix un id.");
        }
        return value.trim().toUpperCase();
    }
}
