package rpgcombat.config.paths;

/** Rutes de fitxers de configuració. */
public record PathsConfig(
        String weaponsConfig,
        String statusMenuModifier,
        String balanceConfig,
        String menuDescriptions,
        String missionsConfig,
        String perksConfig,
        String divinePerksConfig,
        String synergiesConfig) {

    public static final String DEFAULT_WEAPONS_CONFIG = "rpg/data/weapons.json";
    public static final String DEFAULT_STATUS_MENU_MODIFIER = "rpg/data/menuModifiers.json";
    public static final String DEFAULT_BALANCE_CONFIG = "rpg/data/combatBalance.json";
    public static final String DEFAULT_MENU_DESCRIPTIONS = "rpg/data/menuDescription.json";
    public static final String DEFAULT_MISSIONS_CONFIG = "rpg/data/missions.json";
    public static final String DEFAULT_PERKS_CONFIG = "rpg/data/perks.json";
    public static final String DEFAULT_DIVINE_PERKS_CONFIG = "rpg/data/divinePerks.json";
    public static final String DEFAULT_SYNERGIES_CONFIG = "rpg/data/synergies.json";

    public PathsConfig {
        weaponsConfig = fallback(weaponsConfig, DEFAULT_WEAPONS_CONFIG);
        statusMenuModifier = fallback(statusMenuModifier, DEFAULT_STATUS_MENU_MODIFIER);
        balanceConfig = fallback(balanceConfig, DEFAULT_BALANCE_CONFIG);
        menuDescriptions = fallback(menuDescriptions, DEFAULT_MENU_DESCRIPTIONS);
        missionsConfig = fallback(missionsConfig, DEFAULT_MISSIONS_CONFIG);
        perksConfig = fallback(perksConfig, DEFAULT_PERKS_CONFIG);
        divinePerksConfig = fallback(divinePerksConfig, DEFAULT_DIVINE_PERKS_CONFIG);
        synergiesConfig = fallback(synergiesConfig, DEFAULT_SYNERGIES_CONFIG);
    }

    public static PathsConfig defaultConfig() {
        return new PathsConfig(
                DEFAULT_WEAPONS_CONFIG,
                DEFAULT_STATUS_MENU_MODIFIER,
                DEFAULT_BALANCE_CONFIG,
                DEFAULT_MENU_DESCRIPTIONS,
                DEFAULT_MISSIONS_CONFIG,
                DEFAULT_PERKS_CONFIG,
                DEFAULT_DIVINE_PERKS_CONFIG,
                DEFAULT_SYNERGIES_CONFIG);
    }

    private static String fallback(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }

}
