package rpgcombat.achievements.config;

import java.util.List;
import java.util.Locale;

import rpgcombat.achievements.AchievementUpdate;

/** Condició declarativa que filtra quan un objectiu pot avançar. */
public record AchievementCondition(
        String key,
        AchievementConditionOperator operator,
        String value,
        List<String> values) {

    /** Avalua la condició contra una actualització del motor. */
    public boolean matches(AchievementUpdate update) {
        if (update == null || key == null || key.isBlank()) return false;
        String actual = update.text(key);
        Double actualNumber = update.number(key);
        AchievementConditionOperator op = operator == null ? AchievementConditionOperator.EQUALS : operator;

        return switch (op) {
            case EXISTS -> actual != null || actualNumber != null || update.hasEventNamed(key);
            case NOT_EXISTS -> actual == null && actualNumber == null && !update.hasEventNamed(key);
            case EQUALS -> equalsIgnoreCase(actual, value) || numberEquals(actualNumber, value);
            case NOT_EQUALS -> !(equalsIgnoreCase(actual, value) || numberEquals(actualNumber, value));
            case IN -> contains(values, actual);
            case NOT_IN -> !contains(values, actual);
            case GREATER_THAN -> compare(actualNumber, value) > 0;
            case GREATER_OR_EQUALS -> compare(actualNumber, value) >= 0;
            case LESS_THAN -> compare(actualNumber, value) < 0;
            case LESS_OR_EQUALS -> compare(actualNumber, value) <= 0;
            case TRUE -> Boolean.parseBoolean(String.valueOf(actual));
            case FALSE -> !Boolean.parseBoolean(String.valueOf(actual));
        };
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static boolean contains(List<String> values, String actual) {
        if (values == null || values.isEmpty() || actual == null) return false;
        return values.stream().anyMatch(value -> value != null && value.equalsIgnoreCase(actual));
    }

    private static boolean numberEquals(Double actual, String expected) {
        if (actual == null) return false;
        try {
            return Double.compare(actual, Double.parseDouble(expected)) == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int compare(Double actual, String expected) {
        if (actual == null) return -1;
        try {
            return Double.compare(actual, Double.parseDouble(expected));
        } catch (Exception ignored) {
            return -1;
        }
    }

    /** Normalitza noms d'operador més còmodes per al JSON. */
    public static AchievementConditionOperator operatorFrom(String raw) {
        if (raw == null || raw.isBlank()) return AchievementConditionOperator.EQUALS;
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "=", "==", "EQ" -> AchievementConditionOperator.EQUALS;
            case "!=", "NE", "NOT_EQUAL" -> AchievementConditionOperator.NOT_EQUALS;
            case ">", "GT" -> AchievementConditionOperator.GREATER_THAN;
            case ">=", "GTE" -> AchievementConditionOperator.GREATER_OR_EQUALS;
            case "<", "LT" -> AchievementConditionOperator.LESS_THAN;
            case "<=", "LTE" -> AchievementConditionOperator.LESS_OR_EQUALS;
            default -> AchievementConditionOperator.valueOf(normalized);
        };
    }
}
