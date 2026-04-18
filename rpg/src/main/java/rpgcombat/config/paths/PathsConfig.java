package rpgcombat.config.paths;

/**
 * Rutes de fitxers de configuració.
 */
public record PathsConfig(
        String weaponsConfig,
        String statusMenuModifier) {

    public PathsConfig {
        if (weaponsConfig == null || weaponsConfig.isBlank()) {
            weaponsConfig = "rpg/data/weapons.json";
        }
        if (statusMenuModifier == null || statusMenuModifier.isBlank()) {
            statusMenuModifier = "rpg/data/menuModifiers.json";
        }
    }
}
