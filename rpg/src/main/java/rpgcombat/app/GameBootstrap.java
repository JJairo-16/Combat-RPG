package rpgcombat.app;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.config.app.AppConfig;
import rpgcombat.config.character.CharacterCreationMode;
import rpgcombat.config.debug.DebugRuntime;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.creator.CharacterCreationOptions;
import rpgcombat.game.GameLoop;
import rpgcombat.game.cinematics.CinematicBuilder;
import rpgcombat.gamemode.chaos.ChaosPolicy;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.gamemode.model.MatchContext;
import rpgcombat.gamemode.effects.ModeEffectApplier;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.ui.Cleaner;

/** Construeix una partida nova amb els recursos ja precarregats. */
public class GameBootstrap {
    private final AppConfig config;
    private final ResourcePreloader preloader;
    private final AchievementSystem achievementSystem;

    /** Crea el constructor de partides. */
    public GameBootstrap(AppConfig config, ResourcePreloader preloader, AchievementSystem achievementSystem) {
        this.config = config;
        this.preloader = preloader;
        this.achievementSystem = achievementSystem;
    }

    /** Crea una partida llesta per iniciar-se. */
    public GameLoop createGame() {
        return createGame(GameModeRegistry.getOrDefault(config.gameMode().defaultMode()));
    }

    /** Crea una partida llesta per iniciar-se amb el mode indicat. */
    public GameLoop createGame(GameModeDefinition gameMode) {
        GameModeDefinition effectiveMode = gameMode == null
                ? GameModeRegistry.getOrDefault(config.gameMode().defaultMode())
                : gameMode;
        CinematicsOptions cinematicsOptions = config.cinematic();
        if (cinematicsOptions.preCreation()) {
            CinematicBuilder.playPreCreation();
        }

        CharacterCreationOptions creationOptions = CharacterCreationOptions.from(effectiveMode.rules());

        Character p1 = createCharacter(config.characters().player1(), creationOptions);
        clearBetweenCharactersIfNeeded();
        Character p2 = createCharacter(config.characters().player2(), creationOptions);

        p1.setSpecialActionsEnabled(effectiveMode.rules().specialActionsEnabled());
        p2.setSpecialActionsEnabled(effectiveMode.rules().specialActionsEnabled());
        ModeEffectApplier.apply(effectiveMode.rules(), p1, p2);

        boolean chaosActive = ChaosPolicy.apply(effectiveMode, p1, p2, new java.util.Random());
        MatchContext matchContext = new MatchContext(effectiveMode, chaosActive);

        applyDebugOptionsIfNeeded(p1, p2);

        return new GameLoop(
                p1,
                p2,
                preloader.modifiers(),
                preloader.menuInformation(),
                cinematicsOptions,
                config.homeScreen(),
                matchContext,
                achievementSystem);
    }

    /** Crea un personatge segons el mode indicat. */
    private Character createCharacter(CharacterCreationMode mode, CharacterCreationOptions options) {
        return switch (mode) {
            case DEBUG -> CharacterCreator.createDebugCharacter(options);
            case NORMAL -> CharacterCreator.createNewCharacter(options);
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
