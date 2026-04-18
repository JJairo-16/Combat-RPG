package rpgcombat.config.app;

import rpgcombat.config.character.CharacterCreationMode;
import rpgcombat.config.character.CharacterCreationOptions;
import rpgcombat.config.character.CharactersConfig;
import rpgcombat.config.debug.DebugOptions;
import rpgcombat.config.debug.DebugProfile;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.config.ui.UiConfig;

/**
 * Perfils tancats de configuració.
 */
public final class AppConfigProfiles {
    private AppConfigProfiles() {
    }

    public static AppConfig from(DebugProfile profile) {
        if (profile == null) {
            return normal();
        }

        return switch (profile) {
            case NORMAL -> normal();
            case FAST_TEST -> fastTest();
            case DEBUG_INVULNERABLE -> debugInvulnerable();
            case FULL_DEBUG -> fullDebug();
        };
    }

    public static AppConfig normal() {
        return new AppConfig(
                true,
                DebugProfile.NORMAL,
                new PathsConfig("rpg/data/weapons.json", "rpg/data/menuModifiers.json"),
                new UiConfig(true, "Jairo Linares", true),
                new DebugOptions(false, false, false),
                new CharactersConfig(
                        new CharacterCreationOptions(CharacterCreationMode.NORMAL),
                        new CharacterCreationOptions(CharacterCreationMode.NORMAL)));
    }

    public static AppConfig fastTest() {
        return new AppConfig(
                true,
                DebugProfile.FAST_TEST,
                new PathsConfig("rpg/data/weapons.json", "rpg/data/menuModifiers.json"),
                new UiConfig(false, "Jairo Linares", false),
                new DebugOptions(true, false, true),
                new CharactersConfig(
                        new CharacterCreationOptions(CharacterCreationMode.DEBUG),
                        new CharacterCreationOptions(CharacterCreationMode.DEBUG)));
    }

    public static AppConfig debugInvulnerable() {
        return new AppConfig(
                true,
                DebugProfile.DEBUG_INVULNERABLE,
                new PathsConfig("rpg/data/weapons.json", "rpg/data/menuModifiers.json"),
                new UiConfig(false, "Jairo Linares", false),
                new DebugOptions(true, true, true),
                new CharactersConfig(
                        new CharacterCreationOptions(CharacterCreationMode.DEBUG),
                        new CharacterCreationOptions(CharacterCreationMode.DEBUG)));
    }

    public static AppConfig fullDebug() {
        return new AppConfig(
                true,
                DebugProfile.FULL_DEBUG,
                new PathsConfig("rpg/data/weapons.json", "rpg/data/menuModifiers.json"),
                new UiConfig(false, "Jairo Linares", false),
                new DebugOptions(true, true, true),
                new CharactersConfig(
                        new CharacterCreationOptions(CharacterCreationMode.DEBUG),
                        new CharacterCreationOptions(CharacterCreationMode.DEBUG)));
    }
}
