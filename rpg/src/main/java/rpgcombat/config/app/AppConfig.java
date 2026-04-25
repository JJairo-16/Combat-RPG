package rpgcombat.config.app;

import rpgcombat.config.character.CharactersConfig;
import rpgcombat.config.debug.DebugOptions;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.config.ui.HomeScreenConfig;
import rpgcombat.config.ui.UiConfig;

/** Configuració principal de l'aplicació. */
public record AppConfig(
        PathsConfig paths,
        UiConfig ui,
        CinematicsOptions cinematic,
        DebugOptions debug,
        CharactersConfig characters,
        HomeScreenConfig homeScreen) {

    public AppConfig {
        if (paths == null) {
            paths = PathsConfig.defaultConfig();
        }

        if (ui == null) {
            ui = UiConfig.defaultConfig();
        }

        if (cinematic == null) {
            cinematic = CinematicsOptions.defaultConfig();
        }

        if (debug == null) {
            debug = DebugOptions.getFalse();
        }

        if (characters == null) {
            characters = CharactersConfig.defaultConfig();
        }

        if (homeScreen == null) {
            homeScreen = HomeScreenConfig.defaultConfig();
        }
    }
}
