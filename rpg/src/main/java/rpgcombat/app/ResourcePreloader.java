package rpgcombat.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rpgcombat.balance.CombatBalanceLoader;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.config.app.AppConfig;
import rpgcombat.config.paths.PathsConfig;
import rpgcombat.game.menu.MenuDescriptionsLoader;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.config.StatusModLoader;
import rpgcombat.perks.PerkLoader;
import rpgcombat.perks.PerkRegistry;
import rpgcombat.perks.mission.MissionLoader;
import rpgcombat.perks.mission.MissionRegistry;
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

        menuInformation = MenuDescriptionsLoader.load(Path.of(paths.menuDescriptions()));

        MissionRegistry.initialize(MissionLoader.load(Path.of(paths.missionsConfig())));
        PerkRegistry.initialize(PerkLoader.load(Path.of(paths.perksConfig())));

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
