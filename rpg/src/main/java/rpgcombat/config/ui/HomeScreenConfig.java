package rpgcombat.config.ui;

/** Configuració de la pantalla d'inici i del retorn des del final. */
public record HomeScreenConfig(
        boolean enabled,
        boolean allowReturnFromEnd,
        String title) {

    public HomeScreenConfig {
        if (title == null || title.isBlank()) {
            title = "El Llindar Trencat";
        }
    }

    public static HomeScreenConfig defaultConfig() {
        return new HomeScreenConfig(true, true, "El Llindar Trencat");
    }

    public static HomeScreenConfig disabled() {
        return new HomeScreenConfig(false, false, "El Llindar Trencat");
    }
}
