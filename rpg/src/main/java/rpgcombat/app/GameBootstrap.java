package rpgcombat.app;

import rpgcombat.config.app.AppConfig;
import rpgcombat.config.character.CharacterCreationMode;
import rpgcombat.config.debug.DebugRuntime;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.GameLoop;
import rpgcombat.game.cinematics.CinematicBuilder;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.ui.Cleaner;

/** Construeix una partida nova amb els recursos ja precarregats. */
public class GameBootstrap {
    private final AppConfig config;
    private final ResourcePreloader preloader;

    /** Crea el constructor de partides. */
    public GameBootstrap(AppConfig config, ResourcePreloader preloader) {
        this.config = config;
        this.preloader = preloader;
    }

    /** Crea una partida llesta per iniciar-se. */
    public GameLoop createGame() {
        CinematicsOptions cinematicsOptions = config.cinematic();
        if (cinematicsOptions.preCreation()) {
            CinematicBuilder.playPreCreation();
        }

        Character p1 = createCharacter(config.characters().player1());
        clearBetweenCharactersIfNeeded();
        Character p2 = createCharacter(config.characters().player2());

        applyDebugOptionsIfNeeded(p1, p2);

        return new GameLoop(
                p1,
                p2,
                preloader.modifiers(),
                preloader.menuInformation(),
                cinematicsOptions,
                config.homeScreen());
    }

    /** Crea un personatge segons el mode indicat. */
    private Character createCharacter(CharacterCreationMode mode) {
        return switch (mode) {
            case DEBUG -> CharacterCreator.createDebugCharacter();
            case NORMAL -> CharacterCreator.createNewCharacter();
        };
    }

    /** Neteja la pantalla entre creacions si cal. */
    private void clearBetweenCharactersIfNeeded() {
        if (config.ui().clearBetweenCharacterCreation()) {
            new Cleaner().clear();
        }
    }

    /** Aplica opcions de depuració si estan activades. */
    private void applyDebugOptionsIfNeeded(Character p1, Character p2) {
        if (!config.debug().enabled()) {
            return;
        }

        DebugRuntime.applyDebugOptions(p1, config.debug());
        DebugRuntime.applyDebugOptions(p2, config.debug());
    }
}