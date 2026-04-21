package rpgcombat.config.app;

import rpgcombat.config.character.CharactersConfig;
import rpgcombat.config.debug.DebugOptions;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.config.ui.UiConfig;

/** Configuració principal de l'aplicació. */
public record AppConfig(
        PathsConfig paths,
        UiConfig ui,
        DebugOptions debug,
        CharactersConfig characters) {

    public AppConfig {
        if (paths == null) {
            paths = PathsConfig.defaultConfig();
        }

        if (ui == null) {
            ui = UiConfig.defaultConfig();
        }

        if (debug == null) {
            debug = DebugOptions.getFalse();
        }

        if (characters == null) {
            characters = CharactersConfig.debugConfig();
        }
    }
}
