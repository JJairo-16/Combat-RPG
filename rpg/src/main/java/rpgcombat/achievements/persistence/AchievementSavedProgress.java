package rpgcombat.achievements.persistence;

import java.util.Map;

/** Progrés serialitzat d'un assoliment. */
public record AchievementSavedProgress(
        double progress,
        int sequenceIndex,
        boolean completed,
        String completedAt,
        Map<String, Double> valueProgress) {

    /** Constructor de compatibilitat amb desaments antics. */
    public AchievementSavedProgress(double progress, int sequenceIndex, boolean completed, String completedAt) {
        this(progress, sequenceIndex, completed, completedAt, Map.of());
    }
}
