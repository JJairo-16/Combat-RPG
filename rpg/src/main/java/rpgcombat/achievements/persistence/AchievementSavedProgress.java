package rpgcombat.achievements.persistence;

import java.util.Map;

/** Progrés serialitzat d'un assoliment. */
public record AchievementSavedProgress(
        Double progress,
        Integer sequenceIndex,
        Boolean completed,
        String completedAt,
        Map<String, Double> valueProgress,
        Map<String, Integer> actorSequenceProgress) {

    /** Constructor de compatibilitat amb desaments antics. */
    public AchievementSavedProgress(double progress, int sequenceIndex, boolean completed, String completedAt) {
        this(progress, sequenceIndex, completed, completedAt, Map.of(), Map.of());
    }

    /** Constructor de compatibilitat amb desaments sense cursors per actor. */
    public AchievementSavedProgress(double progress, int sequenceIndex, boolean completed, String completedAt,
            Map<String, Double> valueProgress) {
        this(progress, sequenceIndex, completed, completedAt, valueProgress, Map.of());
    }

    /** Desa només l'estat necessari d'un assoliment completat. */
    public static AchievementSavedProgress completed(String completedAt) {
        return new AchievementSavedProgress(null, null, true, completedAt, null, null);
    }

    /** Desa el progrés mutable que encara necessita reprendre's. */
    public static AchievementSavedProgress pending(double progress, int sequenceIndex,
            Map<String, Double> valueProgress, Map<String, Integer> actorSequenceProgress) {
        return new AchievementSavedProgress(
                progress,
                sequenceIndex == 0 ? null : sequenceIndex,
                null,
                null,
                emptyToNull(valueProgress),
                emptyToNull(actorSequenceProgress));
    }

    private static <K, V> Map<K, V> emptyToNull(Map<K, V> values) {
        return values == null || values.isEmpty() ? null : values;
    }
}
