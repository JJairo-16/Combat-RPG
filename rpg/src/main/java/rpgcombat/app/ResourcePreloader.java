package rpgcombat.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import rpgcombat.achievements.config.AchievementLoader;
import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementRegistry;
import rpgcombat.balance.CombatBalanceLoader;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.config.app.AppConfig;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.gamemode.io.GameModeLoader;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.game.cinematics.CinematicBuilder;
import rpgcombat.game.menu.MenuDescriptionsLoader;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.config.StatusModLoader;
import rpgcombat.perks.PerkLoader;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.divine.DivinePerkLoader;
import rpgcombat.perks.divine.DivinePerkDefinition;
import rpgcombat.perks.divine.DivinePerkRegistry;
import rpgcombat.perks.PerkRegistry;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionLoader;
import rpgcombat.perks.mission.MissionRegistry;
import rpgcombat.perks.synergy.SynergyDefinition;
import rpgcombat.perks.synergy.SynergyLoader;
import rpgcombat.perks.synergy.SynergyRegistry;
import rpgcombat.terrain.io.TerrainLoader;
import rpgcombat.terrain.model.TerrainDefinition;
import rpgcombat.terrain.registry.TerrainRegistry;
import rpgcombat.utils.rng.D20Terminal;
import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.WeaponLoader;
import rpgcombat.weapons.config.WeaponDefinition;

/** Gestiona les precàrregues segons el seu cicle de vida. */
public final class ResourcePreloader {
    private boolean appLoaded;
    private boolean homeStaticLoaded;
    private boolean matchStaticLoaded;

    private Map<String, List<StatusMod>> modifiers;
    private Map<String, String> menuInformation;

    /** Carrega recursos globals de l'aplicació. */
    public synchronized void preloadApp() throws IOException {
        if (appLoaded) {
            return;
        }

        SharedTerminal.preload();
        CinematicBuilder.preload();
        appLoaded = true;
    }

    /** Carrega els recursos necessaris per mostrar el menú inicial i seleccionar mode. */
    public synchronized void preloadHomeStatic(AppConfig config) throws IOException {
        if (homeStaticLoaded) {
            return;
        }

        PathsConfig paths = config.paths();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<List<GameModeDefinition>> modes = executor.submit(
                    () -> GameModeLoader.load(Path.of(paths.gameModesConfig())));
            Future<List<AchievementDefinition>> achievements = executor.submit(
                    () -> AchievementLoader.load(Path.of(paths.achievementsConfig())));

            GameModeRegistry.initialize(await(modes, "modes de joc"));
            AchievementRegistry.initialize(await(achievements, "assoliments"));
        }

        homeStaticLoaded = true;
    }

    /** Carrega recursos comuns a totes les partides. */
    public synchronized void preloadMatchStatic(AppConfig config) throws IOException {
        if (matchStaticLoaded) {
            return;
        }

        preloadHomeStatic(config);

        PathsConfig paths = config.paths();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<List<WeaponDefinition>> weapons = executor.submit(
                    () -> WeaponLoader.loadDefinitions(Path.of(paths.weaponsConfig())));
            Future<Map<String, List<StatusMod>>> loadedModifiers = executor.submit(
                    () -> StatusModLoader.load(Path.of(paths.statusMenuModifier())));
            Future<CombatBalanceConfig> balance = executor.submit(
                    () -> CombatBalanceLoader.load(Path.of(paths.balanceConfig())));
            Future<Map<String, String>> loadedMenuInformation = executor.submit(
                    () -> MenuDescriptionsLoader.load(Path.of(paths.menuDescriptions())));
            Future<List<MissionDefinition>> missions = executor.submit(
                    () -> MissionLoader.load(Path.of(paths.missionsConfig())));
            Future<List<PerkDefinition>> perks = executor.submit(
                    () -> PerkLoader.load(Path.of(paths.perksConfig())));
            Future<List<DivinePerkDefinition>> divinePerks = executor.submit(
                    () -> DivinePerkLoader.load(Path.of(paths.divinePerksConfig())));
            Future<List<SynergyDefinition>> synergies = executor.submit(
                    () -> SynergyLoader.load(Path.of(paths.synergiesConfig())));
            Future<List<TerrainDefinition>> terrains = executor.submit(
                    () -> TerrainLoader.load(Path.of(paths.terrainsConfig())));

            Arsenal.preload(await(weapons, "armes"));
            modifiers = await(loadedModifiers, "modificadors de menú");

            CombatBalanceRegistry.initialize(await(balance, "equilibri de combat"));

            menuInformation = await(loadedMenuInformation, "descripcions de menú");

            MissionRegistry.initialize(await(missions, "missions"));
            PerkRegistry.initialize(await(perks, "perks"));
            DivinePerkRegistry.initialize(await(divinePerks, "perks divines"));
            SynergyRegistry.initialize(await(synergies, "sinergies"));
            TerrainRegistry.initialize(await(terrains, "terrenys"));
        }

        matchStaticLoaded = true;
    }

    /** Manté compatibilitat amb el nom antic de la precàrrega completa. */
    public synchronized void preloadGameStatic(AppConfig config) throws IOException {
        preloadMatchStatic(config);
    }

    /** Prepara recursos que s'han de renovar en cada partida. */
    public void preloadNewMatch() {
        D20Terminal.preloadFrames();
        DivineCharismaAffinity.rollForRun(new Random());
    }

    public Map<String, List<StatusMod>> modifiers() {
        return modifiers;
    }

    public Map<String, String> menuInformation() {
        return menuInformation;
    }

    /** Espera una tasca de càrrega i conserva la causa real si falla. */
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
}
