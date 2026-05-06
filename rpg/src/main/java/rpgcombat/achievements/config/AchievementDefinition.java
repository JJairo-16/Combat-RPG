package rpgcombat.achievements.config;

/** Definició immutable d'un assoliment carregat des de JSON. */
public record AchievementDefinition(
        String id,
        String name,
        String description,
        int goal,
        AchievementVisibility visibility,
        AchievementObjective objective) {
}
