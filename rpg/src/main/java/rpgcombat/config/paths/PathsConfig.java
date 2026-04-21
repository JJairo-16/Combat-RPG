package rpgcombat.config.paths;

/** Rutes de fitxers de configuració. */
public record PathsConfig(
        String weaponsConfig,
        String statusMenuModifier) {

    public static final String DEFAULT_WEAPONS_CONFIG = "rpg/data/weapons.json";
    public static final String DEFAULT_STATUS_MENU_MODIFIER = "rpg/data/menuModifiers.json";

    public PathsConfig {
        if (weaponsConfig == null || weaponsConfig.isBlank()) {
            weaponsConfig = DEFAULT_WEAPONS_CONFIG;
        }
        if (statusMenuModifier == null || statusMenuModifier.isBlank()) {
            statusMenuModifier = DEFAULT_STATUS_MENU_MODIFIER;
        }
    }

    public static PathsConfig defaultConfig() {
        return new PathsConfig(DEFAULT_WEAPONS_CONFIG, DEFAULT_STATUS_MENU_MODIFIER);
    }
}
