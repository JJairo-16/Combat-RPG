package rpgcombat.achievements;

import java.util.List;

/** Estructura JSON d'un assoliment. */
public record AchievementConfig(
        String id,
        String name,
        String description,
        Integer goal,
        VisibilityConfig visibility,
        ObjectiveConfig objective) {

    /** Opcions de visibilitat dins del JSON. */
    public record VisibilityConfig(Boolean showNameBeforeComplete, Boolean showDescriptionBeforeComplete) {}

    /** Objectiu de progrés dins del JSON. */
    public record ObjectiveConfig(
            String type,
            String event,
            String successEvent,
            String resetEvent,
            List<String> sequence,
            Double target,
            Double value,
            List<ConditionConfig> conditions,
            String valueKey,
            String uniqueKey,
            List<String> requiredValues,
            Boolean requireAllValues) {}

    /** Condició declarativa dins del JSON. */
    public record ConditionConfig(
            String key,
            String operator,
            String value,
            List<String> values) {}
}
