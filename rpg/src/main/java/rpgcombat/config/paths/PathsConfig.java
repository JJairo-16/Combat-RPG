package rpgcombat.config.paths;

/** Rutes de fitxers de configuració. */
public record PathsConfig(
        String weaponsConfig,
        String statusMenuModifier,
        String balanceConfig,
        String gameModesConfig,
        String menuDescriptions,
        String missionsConfig,
        String perksConfig,
        String divinePerksConfig,
        String synergiesConfig,
        String achievementsConfig,
        String achievementSaveFile,
        String discoveryCatalogConfig,
        String discoverySaveFile) {

    public static final String PERKS_FOLDER = "rpg/data/perks";

    public static final String DEFAULT_WEAPONS_CONFIG = "rpg/data/weapons.json";
    public static final String DEFAULT_STATUS_MENU_MODIFIER = "rpg/data/menuModifiers.json";
    public static final String DEFAULT_BALANCE_CONFIG = "rpg/data/combatBalance.json";
    public static final String DEFAULT_GAME_MODES_CONFIG = "rpg/data/gameModes.json";
    public static final String DEFAULT_MENU_DESCRIPTIONS = "rpg/data/menuDescription.json";

    public static final String DEFAULT_MISSIONS_CONFIG = perks("missions.json");
    public static final String DEFAULT_PERKS_CONFIG = perks("perks.json");
    public static final String DEFAULT_DIVINE_PERKS_CONFIG = perks("divinePerks.json");
    public static final String DEFAULT_SYNERGIES_CONFIG = perks("synergies.json");

    public static final String DEFAULT_ACHIEVEMENTS_CONFIG = "rpg/data/achievements.json";
    public static final String DEFAULT_ACHIEVEMENT_SAVE_FILE = "achievements.json";
    public static final String DEFAULT_DISCOVERY_CATALOG_CONFIG = "rpg/data/discoveryCatalog.json";
    public static final String DEFAULT_DISCOVERY_SAVE_FILE = "discoveries.json";

    public PathsConfig {
        weaponsConfig = fallback(weaponsConfig, DEFAULT_WEAPONS_CONFIG);
        statusMenuModifier = fallback(statusMenuModifier, DEFAULT_STATUS_MENU_MODIFIER);
        balanceConfig = fallback(balanceConfig, DEFAULT_BALANCE_CONFIG);
        gameModesConfig = fallback(gameModesConfig, DEFAULT_GAME_MODES_CONFIG);
        menuDescriptions = fallback(menuDescriptions, DEFAULT_MENU_DESCRIPTIONS);
        missionsConfig = fallback(missionsConfig, DEFAULT_MISSIONS_CONFIG);
        perksConfig = fallback(perksConfig, DEFAULT_PERKS_CONFIG);
        divinePerksConfig = fallback(divinePerksConfig, DEFAULT_DIVINE_PERKS_CONFIG);
        synergiesConfig = fallback(synergiesConfig, DEFAULT_SYNERGIES_CONFIG);
        achievementsConfig = fallback(achievementsConfig, DEFAULT_ACHIEVEMENTS_CONFIG);
        achievementSaveFile = fallback(achievementSaveFile, DEFAULT_ACHIEVEMENT_SAVE_FILE);
        discoveryCatalogConfig = fallback(discoveryCatalogConfig, DEFAULT_DISCOVERY_CATALOG_CONFIG);
        discoverySaveFile = fallback(discoverySaveFile, DEFAULT_DISCOVERY_SAVE_FILE);
    }

    public static PathsConfig defaultConfig() {
        return new PathsConfig(
                DEFAULT_WEAPONS_CONFIG,
                DEFAULT_STATUS_MENU_MODIFIER,
                DEFAULT_BALANCE_CONFIG,
                DEFAULT_GAME_MODES_CONFIG,
                DEFAULT_MENU_DESCRIPTIONS,
                DEFAULT_MISSIONS_CONFIG,
                DEFAULT_PERKS_CONFIG,
                DEFAULT_DIVINE_PERKS_CONFIG,
                DEFAULT_SYNERGIES_CONFIG,
                DEFAULT_ACHIEVEMENTS_CONFIG,
                DEFAULT_ACHIEVEMENT_SAVE_FILE,
                DEFAULT_DISCOVERY_CATALOG_CONFIG,
                DEFAULT_DISCOVERY_SAVE_FILE);
    }

    public static String perks(String path) {
        return PERKS_FOLDER + "/" + path;
    }

    private static String fallback(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }

}
