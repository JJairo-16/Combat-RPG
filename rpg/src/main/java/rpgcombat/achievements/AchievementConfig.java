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

    /** Condició declarativa dins del JSON. */
    public record ConditionConfig(String field, String operator, Object value) {}

    /** Objectiu de progrés dins del JSON. */
    public record ObjectiveConfig(
            String type,
            String event,
            String successEvent,
            String resetEvent,
            List<String> sequence,
            Double target,
            Double value,
            String valueField,
            String uniqueField,
            List<String> requiredValues,
            Double targetPerValue,
            List<ConditionConfig> conditions) {}
}
