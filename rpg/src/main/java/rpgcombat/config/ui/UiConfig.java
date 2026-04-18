package rpgcombat.config.ui;

/**
 * Opcions visuals d'arrencada.
 */
public record UiConfig(
        boolean showLoadingIntro,
        String loadingAuthor,
        boolean clearBetweenCharacterCreation) {

    public UiConfig {
        if (loadingAuthor == null || loadingAuthor.isBlank()) {
            loadingAuthor = "Jairo Linares";
        }
    }
}
