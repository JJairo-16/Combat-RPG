package rpgcombat.gamemode.effects;

import java.util.Map;

/** Definición declarativa de un efecto inicial aplicado por un modo. */
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
            throw new IllegalArgumentException("El efecto de modo necesita un id.");
        }
        return value.trim().toUpperCase();
    }
}
