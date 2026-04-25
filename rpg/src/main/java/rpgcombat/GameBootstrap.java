package rpgcombat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import rpgcombat.balance.CombatBalanceLoader;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.config.app.AppConfig;
import rpgcombat.config.app.AppConfigLoader;
import rpgcombat.config.character.CharacterCreationMode;
import rpgcombat.config.debug.DebugRuntime;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.GameLoop;
import rpgcombat.game.cinematics.CinematicBuilder;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.config.StatusModLoader;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.rng.D20Terminal;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.ui.Cleaner;
import rpgcombat.utils.ui.LoadingIntro;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.weapons.Arsenal;

/** Prepara l'entorn i construeix una partida llesta per iniciar-se. */
public class GameBootstrap {
    private static final String APP_CONFIG_PATH = "rpg/data/appConfig.json";

    private AppConfig config;
    private Map<String, List<StatusMod>> modifiers;

    /** Crea una partida completament inicialitzada. */
    public GameLoop createGame() {
        loadAppConfig();
        preloadResources();
        ensureModifiersLoaded();

        CinematicsOptions cinematicsOptions = config.cinematic();
        if (cinematicsOptions.preCreation()) {
            CinematicBuilder.playPreCreation();
        }

        Character p1 = createCharacter(config.characters().player1());
        clearBetweenCharactersIfNeeded();
        Character p2 = createCharacter(config.characters().player2());

        applyDebugOptionsIfNeeded(p1, p2);

        return new GameLoop(p1, p2, modifiers, cinematicsOptions);
    }

    private void loadAppConfig() {
        try {
            config = AppConfigLoader.load(Path.of(APP_CONFIG_PATH));
        } catch (IOException e) {
            Prettier.error("No s'ha pogut carregar appConfig.json. S'usarà la configuració per defecte.");
            config = AppConfigLoader.defaultConfig();
        }
    }

    private void preloadResources() {
        if (mustShowLoadingIntro()) {
            LoadingIntro intro = new LoadingIntro(config.ui().loadingAuthor());
            intro.start(this::preload);
            return;
        }

        preload();
    }

    private void preload() {
        try {
            Arsenal.preload(Path.of(config.paths().weaponsConfig()));
            SharedTerminal.preload();
            modifiers = StatusModLoader.load(Path.of(config.paths().statusMenuModifier()));

            CombatBalanceConfig balance = CombatBalanceLoader.load(Path.of(config.paths().balanceConfig()));
            CombatBalanceRegistry.initialize(balance);
        } catch (IOException e) {
            Prettier.error("Ha hagut un error durant la precarrega.");
        }

        D20Terminal.preloadFrames();
    }

    private boolean mustShowLoadingIntro() {
        if (config.debug().preloadWithoutIntro()) {
            return false;
        }

        return config.ui().showLoadingIntro();
    }

    private Character createCharacter(CharacterCreationMode mode) {
        return switch (mode) {
            case DEBUG -> CharacterCreator.createDebugCharacter();
            case NORMAL -> CharacterCreator.createNewCharacter();
        };
    }

    private void clearBetweenCharactersIfNeeded() {
        if (config.ui().clearBetweenCharacterCreation()) {
            new Cleaner().clear();
        }
    }

    private void applyDebugOptionsIfNeeded(Character p1, Character p2) {
        if (!config.debug().enabled()) {
            return;
        }

        DebugRuntime.applyDebugOptions(p1, config.debug());
        DebugRuntime.applyDebugOptions(p2, config.debug());
    }

    private void ensureModifiersLoaded() {
        Objects.requireNonNull(modifiers, "No s'han carregat els modificadors correctament.");
    }
}