package rpgcombat.config.ui;

/** Opcions visuals d'arrencada. */
public record UiConfig(
        boolean showLoadingIntro,
        String loadingAuthor,
        boolean clearBetweenCharacterCreation) {

    public static final String DEFAULT_LOADING_AUTHOR = "Jairo Linares";

    public UiConfig {
        if (loadingAuthor == null || loadingAuthor.isBlank()) {
            loadingAuthor = DEFAULT_LOADING_AUTHOR;
        }
    }

    public static UiConfig defaultConfig() {
        return new UiConfig(true, DEFAULT_LOADING_AUTHOR, true);
    }

    public static UiConfig debugConfig() {
        return new UiConfig(false, DEFAULT_LOADING_AUTHOR, false);
    }
}
