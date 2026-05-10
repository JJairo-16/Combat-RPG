package rpgcombat.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rpgcombat.achievements.config.AchievementLoader;
import rpgcombat.achievements.config.AchievementRegistry;
import rpgcombat.balance.CombatBalanceLoader;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.config.app.AppConfig;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.gamemode.io.GameModeLoader;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.game.menu.MenuDescriptionsLoader;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.config.StatusModLoader;
import rpgcombat.perks.PerkLoader;
import rpgcombat.perks.divine.DivinePerkLoader;
import rpgcombat.perks.divine.DivinePerkRegistry;
import rpgcombat.perks.PerkRegistry;
import rpgcombat.perks.mission.MissionLoader;
import rpgcombat.perks.mission.MissionRegistry;
import rpgcombat.perks.synergy.SynergyLoader;
import rpgcombat.perks.synergy.SynergyRegistry;
import rpgcombat.utils.rng.D20Terminal;
import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.weapons.Arsenal;

/** Gestiona les precàrregues segons el seu cicle de vida. */
public final class ResourcePreloader {
    private boolean appLoaded;
    private boolean gameStaticLoaded;

    private Map<String, List<StatusMod>> modifiers;
    private Map<String, String> menuInformation;

    /** Carrega recursos globals de l'aplicació. */
    public void preloadApp() throws IOException {
        if (appLoaded) {
            return;
        }

        SharedTerminal.preload();
        appLoaded = true;
    }

    /** Carrega recursos comuns a totes les partides. */
    public void preloadGameStatic(AppConfig config) throws IOException {
        if (gameStaticLoaded) {
            return;
        }

        PathsConfig paths = config.paths();

        Arsenal.preload(Path.of(paths.weaponsConfig()));
        modifiers = StatusModLoader.load(Path.of(paths.statusMenuModifier()));

        CombatBalanceConfig balance = CombatBalanceLoader.load(Path.of(paths.balanceConfig()));
        CombatBalanceRegistry.initialize(balance);

        GameModeRegistry.initialize(GameModeLoader.load(Path.of(paths.gameModesConfig())));

        menuInformation = MenuDescriptionsLoader.load(Path.of(paths.menuDescriptions()));

        MissionRegistry.initialize(MissionLoader.load(Path.of(paths.missionsConfig())));
        PerkRegistry.initialize(PerkLoader.load(Path.of(paths.perksConfig())));
        DivinePerkRegistry.initialize(DivinePerkLoader.load(Path.of(paths.divinePerksConfig())));
        SynergyRegistry.initialize(SynergyLoader.load(Path.of(paths.synergiesConfig())));
        AchievementRegistry.initialize(AchievementLoader.load(Path.of(paths.achievementsConfig())));

        gameStaticLoaded = true;
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
}
