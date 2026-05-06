package rpgcombat.achievements.config;

import java.util.List;

import rpgcombat.achievements.AchievementEvent;
import rpgcombat.combat.models.Action;

/** Defineix la condició configurable que fa avançar un assoliment. */
public record AchievementObjective(
        AchievementObjectiveType type,
        AchievementEvent event,
        AchievementEvent successEvent,
        AchievementEvent resetEvent,
        List<Action> sequence,
        double target,
        double value,
        List<AchievementCondition> conditions,
        String valueKey,
        String uniqueKey,
        List<String> requiredValues,
        boolean requireAllValues) {
}
