package rpgcombat.config.paths;

/** Rutes de fitxers de configuració. */
public record PathsConfig(
        String weaponsConfig,
        String statusMenuModifier,
        String balanceConfig,
        String menuDescriptions,
        String missionsConfig,
        String perksConfig) {

    public static final String DEFAULT_WEAPONS_CONFIG = "rpg/data/weapons.json";
    public static final String DEFAULT_STATUS_MENU_MODIFIER = "rpg/data/menuModifiers.json";
    public static final String DEFAULT_BALANCE_CONFIG = "rpg/data/combatBalance.json";
    public static final String DEFAULT_MENU_DESCRIPTIONS = "rpg/data/menuDescription.json";
    public static final String DEFAULT_MISSIONS_CONFIG = "rpg/data/missions.json";
    public static final String DEFAULT_PERKS_CONFIG = "rpg/data/perks.json";

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
        if (menuDescriptions == null || menuDescriptions.isBlank()) {
            menuDescriptions = DEFAULT_MENU_DESCRIPTIONS;
        }
        if (missionsConfig == null || missionsConfig.isBlank()) {
            missionsConfig = DEFAULT_MISSIONS_CONFIG;
        }
        if (perksConfig == null || perksConfig.isBlank()) {
            perksConfig = DEFAULT_PERKS_CONFIG;
        }
    }

    public static PathsConfig defaultConfig() {
        return new PathsConfig(
                DEFAULT_WEAPONS_CONFIG,
                DEFAULT_STATUS_MENU_MODIFIER,
                DEFAULT_BALANCE_CONFIG,
                DEFAULT_MENU_DESCRIPTIONS,
                DEFAULT_MISSIONS_CONFIG,
                DEFAULT_PERKS_CONFIG);
    }
}
