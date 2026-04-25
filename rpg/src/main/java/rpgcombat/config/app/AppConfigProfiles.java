package rpgcombat.config.app;

import rpgcombat.config.character.CharactersConfig;
import rpgcombat.config.debug.DebugOptions;
import rpgcombat.config.debug.DebugProfile;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.config.ui.HomeScreenConfig;
import rpgcombat.config.ui.UiConfig;

/** Perfils tancats de configuració. */
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
                PathsConfig.defaultConfig(),
                UiConfig.defaultConfig(),
                CinematicsOptions.defaultConfig(),
                DebugOptions.getFalse(),
                CharactersConfig.defaultConfig(),
                HomeScreenConfig.defaultConfig());
    }

    public static AppConfig fastTest() {
        return new AppConfig(
                PathsConfig.defaultConfig(),
                UiConfig.debugConfig(),
                CinematicsOptions.debugConfig(),
                new DebugOptions(true, false, true),
                CharactersConfig.debugConfig(),
                HomeScreenConfig.disabled());
    }

    public static AppConfig debugInvulnerable() {
        return new AppConfig(
                PathsConfig.defaultConfig(),
                UiConfig.debugConfig(),
                CinematicsOptions.debugConfig(),
                new DebugOptions(true, true, true),
                CharactersConfig.debugConfig(),
                HomeScreenConfig.disabled());
    }

    public static AppConfig fullDebug() {
        return new AppConfig(
                PathsConfig.defaultConfig(),
                UiConfig.debugConfig(),
                CinematicsOptions.debugConfig(),
                new DebugOptions(true, true, true),
                CharactersConfig.debugConfig(),
                HomeScreenConfig.defaultConfig());
    }
}
