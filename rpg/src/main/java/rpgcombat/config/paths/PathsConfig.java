package rpgcombat.config.paths;

/** Rutes de fitxers de configuració. */
public record PathsConfig(
        String weaponsConfig,
        String statusMenuModifier,
        String balanceConfig) {

    public static final String DEFAULT_WEAPONS_CONFIG = "rpg/data/weapons.json";
    public static final String DEFAULT_STATUS_MENU_MODIFIER = "rpg/data/menuModifiers.json";
    public static final String DEFAULT_BALANCE_CONFIG = "rpg/data/combatBalance.json";

    public PathsConfig {
        if (weaponsConfig == null || weaponsConfig.isBlank()) {
            weaponsConfig = DEFAULT_WEAPONS_CONFIG;
        }
        if (statusMenuModifier == null || statusMenuModifier.isBlank()) {
            statusMenuModifier = DEFAULT_STATUS_MENU_MODIFIER;
        }

        if (balanceConfig == null || balanceConfig.isBlank()) {
            balanceConfig = DEFAULT_BALANCE_CONFIG;
        }
    }

    public static PathsConfig defaultConfig() {
        return new PathsConfig(DEFAULT_WEAPONS_CONFIG, DEFAULT_STATUS_MENU_MODIFIER, DEFAULT_BALANCE_CONFIG);
    }
}
