package rpgcombat.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.achievements.config.AchievementRegistry;
import rpgcombat.achievements.ui.AchievementGridViewer;
import rpgcombat.config.app.AppConfig;
import rpgcombat.config.app.AppConfigLoader;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryRuntime;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.discovery.config.DiscoveryCatalog;
import rpgcombat.discovery.config.DiscoveryCatalogConfig;
import rpgcombat.discovery.config.DiscoveryCatalogLoader;
import rpgcombat.discovery.ui.DiscoveryInteractiveViewer;
import rpgcombat.game.EndGameAction;
import rpgcombat.game.GameLoop;
import rpgcombat.game.cinematics.CinematicBuilder;
import rpgcombat.game.menu.HomeMenu;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.gamemode.ui.GameModeSelectionMenu;
import rpgcombat.utils.ui.Cleaner;
import rpgcombat.utils.ui.LoadingIntro;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.unlocks.UnlockRuntime;

/** Controla el flux general de l'aplicació. */
public final class AppController {
    private static final String APP_CONFIG_PATH = "rpg/data/appConfig.json";

    private final ResourcePreloader preloader = new ResourcePreloader();
    private AppConfig config;
    private AchievementSystem achievementSystem;
    private DiscoverySystem discoverySystem;

    /** Inicia l'aplicació fins que l'usuari surt. */
    public void run() {
        new Cleaner().clear(0);
        loadConfig();

        if (!preloadResources()) {
            return;
        }

        boolean goHome = config.homeScreen().enabled();

        while (true) {
            if (goHome) {
                HomeMenu.Action action = HomeMenu.show(config.homeScreen().title());

                if (action == HomeMenu.Action.EXIT) {
                    return;
                }

                if (action == HomeMenu.Action.ACHIEVEMENTS) {
                    AchievementGridViewer.show(achievementSystem.toViewModels());
                    continue;
                }

                if (action == HomeMenu.Action.DISCOVERIES) {
                    if (!prepareMatchResources()) {
                        continue;
                    }
                    DiscoveryInteractiveViewer.show(discoverySystem.toOverview());
                    continue;
                }

                if (action == HomeMenu.Action.CREDITS) {
                    CinematicBuilder.playCredits();
                    continue;
                }
            }

            EndGameAction endAction = playOneMatch();

            switch (endAction) {
                case PLAY_AGAIN -> goHome = false;
                case HOME -> goHome = config.homeScreen().enabled();
                case EXIT -> {
                    new Cleaner().clear();
                    return;
                }
            }
        }
    }

    /** Crea i executa una partida. */
    private EndGameAction playOneMatch() {
        GameModeDefinition gameMode = selectGameMode();
        if (gameMode == null) {
            return EndGameAction.HOME;
        }
        if (!prepareMatchResources()) {
            return EndGameAction.HOME;
        }
        achievementSystem.onGameModeSelected(gameMode.id());
        DiscoveryRuntime.discover(DiscoveryCategory.GAME_MODES, gameMode.id());
        preloader.preloadNewMatch();

        GameBootstrap bootstrap = new GameBootstrap(config, preloader, achievementSystem);
        GameLoop game = bootstrap.createGame(gameMode);

        return game.init();
    }

    /** Selecciona el mode de joc abans de mostrar cap cinemàtica de partida. */
    private GameModeDefinition selectGameMode() {
        GameModeDefinition mode;
        if (!config.gameMode().selectionEnabled()) {
            mode = GameModeRegistry.getOrDefault(config.gameMode().defaultMode());
        } else {
            mode = GameModeSelectionMenu.show(
                    GameModeRegistry.all(),
                    achievementSystem,
                    discoverySystem,
                    config.gameMode().defaultMode());
        }
        
        return mode;
    }

    /** Carrega la configuració o usa la predeterminada. */
    private void loadConfig() {
        try {
            config = AppConfigLoader.load(Path.of(APP_CONFIG_PATH));
        } catch (IOException e) {
            Prettier.error("No s'ha pogut carregar appConfig.json. S'usarà la configuració per defecte.");
            config = AppConfigLoader.defaultConfig();
        }
    }

    /** Precarrega recursos amb intro o directament. */
    private boolean preloadResources() {
        if (!mustShowLoadingIntro()) {
            return preloadNow();
        }

        AtomicReference<Exception> error = new AtomicReference<>();
        LoadingIntro intro = new LoadingIntro(config.ui().loadingAuthor());
        intro.start(() -> {
            try {
                preloadAll();
            } catch (IOException | RuntimeException e) {
                error.set(e);
            }
        });

        if (error.get() != null) {
            Prettier.error("Hi ha hagut un error durant la precàrrega.");
            return false;
        }

        return true;
    }

    /** Precarrega recursos sense intro. */
    private boolean preloadNow() {
        try {
            preloadAll();
            return true;
        } catch (IOException | RuntimeException e) {
            Prettier.error("Hi ha hagut un error durant la precàrrega.");
            return false;
        }
    }

    /** Precarrega tots els recursos necessaris. */
    private void preloadAll() throws IOException {
        preloader.preloadApp();
        preloader.preloadHomeStatic(config);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<AchievementSystem> achievements = executor.submit(
                    () -> AchievementSystem.load(AchievementRegistry.all(), config.paths().achievementSaveFile()));
            Future<DiscoverySystem> discoveries = executor.submit(
                    () -> DiscoverySystem.load(null, config.paths().discoverySaveFile()));
            Future<DiscoveryCatalogConfig> catalogConfig = executor.submit(
                    () -> DiscoveryCatalogLoader.load(Path.of(config.paths().discoveryCatalogConfig())));
            Future<Void> matchStatic = executor.submit(() -> {
                preloader.preloadMatchStatic(config);
                return null;
            });

            achievementSystem = await(achievements, "progrés d'assoliments");
            discoverySystem = await(discoveries, "progrés de descobriments");
            await(matchStatic, "recursos de combat");
            ensureDiscoveryCatalogLoaded(await(catalogConfig, "catàleg de descobriments"));
        }

        UnlockRuntime.configure(achievementSystem, discoverySystem);
    }

    /** Carrega els recursos complets abans d'entrar en combat o mostrar descobriments. */
    private boolean prepareMatchResources() {
        try {
            preloader.preloadMatchStatic(config);
            if (discoverySystem != null && !discoverySystem.hasCatalog()) {
                ensureDiscoveryCatalogLoaded(DiscoveryCatalogLoader.load(Path.of(config.paths().discoveryCatalogConfig())));
            }
            return true;
        } catch (Exception e) {
            Prettier.error("Hi ha hagut un error durant la càrrega dels recursos necessaris.");
            return false;
        }
    }

    /** Garanteix que el catàleg complet de descobriments està construït. */
    private void ensureDiscoveryCatalogLoaded(DiscoveryCatalogConfig config) {
        if (discoverySystem == null || discoverySystem.hasCatalog()) {
            return;
        }
        DiscoveryCatalog catalog = DiscoveryCatalog.build(config);
        discoverySystem.setCatalog(catalog);
    }

    /** Espera una càrrega asíncrona i conserva la causa real si falla. */
    private static <T> T await(Future<T> future, String resourceName) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Càrrega interrompuda: " + resourceName, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException("No s'ha pogut carregar: " + resourceName, cause);
        }
    }

    /** Indica si cal mostrar la intro de càrrega. */
    private boolean mustShowLoadingIntro() {
        return !config.debug().preloadWithoutIntro() && config.ui().showLoadingIntro();
    }
}
