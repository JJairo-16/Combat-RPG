package rpgcombat.achievements.config;

import java.util.List;

import rpgcombat.achievements.AchievementEvent;
import rpgcombat.combat.models.Action;

/** Defineix la condició que fa avançar un assoliment. */
public record AchievementObjective(
        AchievementObjectiveType type,
        AchievementEvent event,
        AchievementEvent successEvent,
        AchievementEvent resetEvent,
        List<Action> sequence,
        double target,
        double value,
        String valueField,
        String uniqueField,
        List<String> requiredValues,
        double targetPerValue,
        List<AchievementCondition> conditions) {

    /** Constructor de compatibilitat per a assoliments antics. */
    public AchievementObjective(
            AchievementObjectiveType type,
            AchievementEvent event,
            AchievementEvent successEvent,
            AchievementEvent resetEvent,
            List<Action> sequence,
            double target,
            double value) {
        this(type, event, successEvent, resetEvent, sequence, target, value, null, null, List.of(), 1.0, List.of());
    }
}
