package rpgcombat.config.app;

import rpgcombat.config.character.CharacterCreationMode;
import rpgcombat.config.character.CharacterCreationOptions;
import rpgcombat.config.character.CharactersConfig;
import rpgcombat.config.debug.DebugOptions;
import rpgcombat.config.debug.DebugProfile;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.config.ui.UiConfig;

/**
 * Configuració principal de l'aplicació.
 */
public record AppConfig(
        boolean useProfile,
        DebugProfile profile,
        PathsConfig paths,
        UiConfig ui,
        DebugOptions debug,
        CharactersConfig characters) {

    public AppConfig {
        if (profile == null) {
            profile = DebugProfile.NORMAL;
        }

        if (paths == null) {
            paths = new PathsConfig("rpg/data/weapons.json", "rpg/data/menuModifiers.json");
        }

        if (ui == null) {
            ui = new UiConfig(true, "Jairo Linares", true);
        }

        if (debug == null) {
            debug = new DebugOptions(false, false, false);
        }

        if (characters == null) {
            characters = new CharactersConfig(
                    new CharacterCreationOptions(CharacterCreationMode.NORMAL),
                    new CharacterCreationOptions(CharacterCreationMode.NORMAL));
        }
    }
}
