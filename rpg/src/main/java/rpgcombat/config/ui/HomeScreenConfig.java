package rpgcombat.config.ui;

/** Configuració de la pantalla d'inici i del retorn des del final. */
public record HomeScreenConfig(
        boolean enabled,
        boolean allowReturnFromEnd,
        String title) {

    public HomeScreenConfig {
        if (title == null || title.isBlank()) {
            title = "RPG Combat";
        }
    }

    public static HomeScreenConfig defaultConfig() {
        return new HomeScreenConfig(true, true, "RPG Combat");
    }

    public static HomeScreenConfig disabled() {
        return new HomeScreenConfig(false, false, "RPG Combat");
    }
}
