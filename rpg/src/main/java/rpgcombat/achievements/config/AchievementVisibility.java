package rpgcombat.achievements.config;

/** Indica quina informació d'un assoliment es mostra abans de completar-lo. */
public record AchievementVisibility(
        boolean showNameBeforeComplete,
        boolean showDescriptionBeforeComplete) {

    /** @return visibilitat predeterminada. */
    public static AchievementVisibility visible() {
        return new AchievementVisibility(true, true);
    }
}
