package rpgcombat.achievements.config;

/** Condició declarativa per filtrar quan avança un assoliment. */
public record AchievementCondition(
        String field,
        String operator,
        String value) {
}
