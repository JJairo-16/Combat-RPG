package rpgcombat.achievements.persistence;

import java.util.Map;

/** Progrés serialitzat d'un assoliment. */
public record AchievementSavedProgress(
        double progress,
        int sequenceIndex,
        boolean completed,
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
}
