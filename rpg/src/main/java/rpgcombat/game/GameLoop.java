package rpgcombat.game;

import java.util.List;
import java.util.Map;
import rpgcombat.achievements.AchievementSystem;
import rpgcombat.combat.CombatSystem;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.Winner;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.config.ui.HomeScreenConfig;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryRuntime;
import rpgcombat.game.cinematics.CinematicBuilder;
import rpgcombat.game.menu.EndGameMenu;
import rpgcombat.game.menu.MenuCenter;
import rpgcombat.game.modifier.Actions;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.ui.MatchPresentation;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.gamemode.model.MatchContext;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.types.ActionMenuHintEffect;
import rpgcombat.models.effects.triggers.gamemode.Chaos;
import rpgcombat.perks.CombatPerkSystem;

import rpgcombat.utils.input.Menu;
import rpgcombat.utils.interactive.WeaponMenu;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.utils.ui.TerminalClear;

import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.config.WeaponDefinition;

/**
 * Controla el bucle principal del combat per torns entre dos jugadors.
 */
public class GameLoop {
    private final MenuCenter menu;

    private final Character player1;
    private final Character player2;

    private final CombatSystem combatSystem;
    private final CombatPerkSystem perkSystem;
    private final CinematicsOptions cinematicsOptions;
    private final HomeScreenConfig homeScreenConfig;
    private final MatchContext matchContext;
    private final GameModeRules rules;
    private final AchievementSystem achievementSystem;
    private final MatchPresentation presentation;
    // Cache d'armes disponibles (assumim que no canvia durant la partida)
    private final List<WeaponDefinition> entries;

    public GameLoop(Character player1, Character player2, Map<String, List<StatusMod>> modifiers,
            Map<String, String> information, CinematicsOptions cinematicsOptions, HomeScreenConfig homeScreenConfig,
            AchievementSystem achievementSystem) {
        this(player1, player2, modifiers, information, cinematicsOptions, homeScreenConfig,
                new MatchContext(null, false), achievementSystem);
    }

    public GameLoop(Character player1, Character player2, Map<String, List<StatusMod>> modifiers,
            Map<String, String> information, CinematicsOptions cinematicsOptions, HomeScreenConfig homeScreenConfig,
            MatchContext matchContext, AchievementSystem achievementSystem) {
        this.player1 = player1;
        this.player2 = player2;
        this.matchContext = matchContext == null ? new MatchContext(null, false) : matchContext;
        this.rules = this.matchContext.rules();
        this.perkSystem = new CombatPerkSystem(player1, player2, achievementSystem, rules);
        this.combatSystem = new CombatSystem(player1, player2,
                new rpgcombat.combat.turnservice.DefaultTurnPriorityPolicy(), perkSystem, achievementSystem, rules);

        this.menu = new MenuCenter(player1, player2, this::changeWeapon, this::showPlayerInfoWrapper, modifiers,
                information, rules);
        this.menu.setMissionTextProvider(perkSystem::missionSummary);
        this.menu.setTerrainHintTextProvider(this::actionMenuHints);
        this.cinematicsOptions = cinematicsOptions;
        this.homeScreenConfig = homeScreenConfig;
        this.achievementSystem = achievementSystem;
        this.presentation = new MatchPresentation(this.matchContext);
        this.entries = Arsenal.availableValues(rules);

        Actions.configureAchievementTracking(achievementSystem, combatSystem::roundNumber);
    }

    /**
     * Inicia el combat i el manté en execució fins que hi hagi un vencedor (o
     * empat).
     */
    public EndGameAction init() {
        CinematicBuilder.playInit(cinematicsOptions, matchContext);
        registerChaosStartIfNeeded();
        int completedAchievementsLastTurn = achievementSystem.consumePendingCompletedCount();

        Winner winner;
        do {
            achievementSystem.onRoundStart();
            DiscoveryRuntime.onRoundStart();
            menu.setCompletedAchievementsBadgeCount(completedAchievementsLastTurn);

            Action action1 = menu.playPlayer1();
            Action action2 = menu.playPlayer2();

            winner = combatSystem.play(action1, action2);
            completedAchievementsLastTurn = achievementSystem.consumePendingCompletedCount();

            if (winner == Winner.NONE) {
                perkSystem.resolvePendingChoices(player1);
                perkSystem.resolvePendingChoices(player2);

                if (cinematicsOptions.antiStall() && combatSystem.preAntiStall()) {
                    CinematicBuilder.playAntiStall();
                }
            }
        } while (winner == Winner.NONE);

        achievementSystem.onMatchFinished(winner, player1, player2, combatSystem.roundNumber());
        achievementSystem.saveIfDirty();
        DiscoveryRuntime.saveIfDirty();
        return finish(winner);
    }

    /** Registra els assoliments inicials relacionats amb el mode caòtic. */
    private void registerChaosStartIfNeeded() {
        if (achievementSystem == null) return;
        boolean p1Chaos = player1.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        boolean p2Chaos = player2.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        if (p1Chaos) achievementSystem.onChaosTriggerAdded(player1, combatSystem.roundNumber());
        if (p2Chaos) achievementSystem.onChaosTriggerAdded(player2, combatSystem.roundNumber());
        if (p1Chaos || p2Chaos) achievementSystem.onChaosMatchStarted(player1, player2, combatSystem.roundNumber());
    }

    /** Mostra el resultat final del combat i demana què fer després. */
    private EndGameAction finish(Winner winner) {
        presentation.showWinner(winner, player1, player2);
        CinematicBuilder.playEnd(winner, matchContext);

        return EndGameMenu.ask(homeScreenConfig.allowReturnFromEnd());
    }

    private void showPlayerInfoWrapper(Character player) {
        presentation.showPlayerInfo(player);
    }

    /**
     * Permet al jugador equipar una arma de l'arsenal (amb filtres) si compleix
     * requisits.
     */
    private void changeWeapon(Character player) {
        Weapon weapon = null;
        boolean loop = true;

        Statistics stats = player.getStatistics();

        WeaponMenu.FilterState filters = new WeaponMenu.FilterState();
        filters.setOnlyEquippable(true);

        do {
            TerminalClear.clearShared();

            WeaponDefinition selected = WeaponMenu.chooseWeaponEntryWithFilters(
                    entries,
                    "Armes de l'arsenal",
                    stats,
                    filters);

            if (selected == null) {
                loop = false; // Cancel·lar
                continue;
            }

            weapon = selected.create();

            if (weapon.canEquip(stats)) {
                loop = false;
            } else {
                Prettier.warn("No compleixes els requisits per equipar aquesta arma.");
                Menu.pause();
            }
        } while (loop);

        if (weapon != null) {
            player.setWeapon(weapon);
            if (weapon.getId() != null) {
                DiscoveryRuntime.discover(DiscoveryCategory.WEAPONS, weapon.getId());
            }
            achievementSystem.onWeaponEquipped(player);
        }
    }

    /** Recull pistes d'efectes que necessiten parlar abans de triar acció. */
    private String actionMenuHints(Character player) {
        if (player == null) {
            return "";
        }

        StringBuilder hints = new StringBuilder();
        for (ActionMenuHintEffect hint : player.effectsOfType(ActionMenuHintEffect.class)) {
            String text = hint.actionMenuHint(player, combatSystem.roundNumber() + 1);
            if (text == null || text.isBlank()) {
                continue;
            }
            if (!hints.isEmpty()) {
                hints.append("\n\n");
            }
            hints.append(text.trim());
        }
        return hints.toString();
    }

}
