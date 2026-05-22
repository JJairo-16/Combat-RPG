package rpgcombat.game.menu;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import menu.DynamicMenu;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.MenuStatusModifier;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.interactive.PlayerProgressMenu;

public class MenuCenter {
    private static final String BASE_SNAP = "base";

    private final PlayerProgressMenu selector1;
    private final PlayerProgressMenu selector2;
    private boolean infoVisible1 = false;
    private boolean infoVisible2 = false;

    private final Character player1;
    private final Character player2;
    private final DynamicMenu<Action, Character> menu1;
    private final DynamicMenu<Action, Character> menu2;

    private final MenuStatusModifier mod1;
    private final MenuStatusModifier mod2;
    private final GameModeRules rules;
    private Function<Character, String> missionTextProvider = player -> "";
    private Function<Character, String> terrainHintTextProvider = player -> "";

    private int completedAchievementsBadgeCount;

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers,
            Map<String, String> information) {
        this(player1, player2, changeWeaponHandler, showPlayerInfoHandler, modifiers, information,
                GameModeRules.unrestricted());
    }

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers,
            Map<String, String> information, GameModeRules rules) {

        this.player1 = player1;
        this.player2 = player2;
        this.rules = rules == null ? GameModeRules.unrestricted() : rules;

        this.selector1 = new PlayerProgressMenu(information);
        this.selector2 = new PlayerProgressMenu(information);

        DynamicMenu<Action, Character> baseMenuAction1 = MenuBuilder.build(selector1::getOption, changeWeaponHandler,
                showPlayerInfoHandler, this.rules);
        DynamicMenu<Action, Character> baseMenuAction2 = MenuBuilder.build(selector2::getOption, changeWeaponHandler,
                showPlayerInfoHandler, this.rules);

        this.menu1 = baseMenuAction1.createChildMenu("Accions de " + player1.getName(), player1);
        this.menu2 = baseMenuAction2.createChildMenu("Accions de " + player2.getName(), player2);

        this.mod1 = configMenu(menu1, player1, modifiers, this.rules);
        this.mod2 = configMenu(menu2, player2, modifiers, this.rules);
    }

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers,
            Map<String, String> information,
            Function<Character, String> missionTextProvider) {
        this(player1, player2, changeWeaponHandler, showPlayerInfoHandler, modifiers, information,
                GameModeRules.unrestricted());
        setMissionTextProvider(missionTextProvider);
    }

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers,
            Map<String, String> information,
            Function<Character, String> missionTextProvider,
            GameModeRules rules) {
        this(player1, player2, changeWeaponHandler, showPlayerInfoHandler, modifiers, information, rules);
        setMissionTextProvider(missionTextProvider);
    }

    public void setMissionTextProvider(Function<Character, String> missionTextProvider) {
        this.missionTextProvider = missionTextProvider == null ? player -> "" : missionTextProvider;
    }

    public void setTerrainHintTextProvider(Function<Character, String> terrainHintTextProvider) {
        this.terrainHintTextProvider = terrainHintTextProvider == null ? player -> "" : terrainHintTextProvider;
    }

    public Action playPlayer1() {
        selector1.setInformationVisible(infoVisible1);
        selector1.setProgressText(missionTextProvider.apply(player1));
        selector1.setTerrainHintText(terrainHintTextProvider.apply(player1));
        selector1.setCompletedAchievementsBadgeCount(completedAchievementsBadgeCount);

        mod1.mod(BASE_SNAP);
        Action action = menu1.run();

        this.infoVisible1 = selector1.getInformationVisible();
        selector1.setProgressText("");
        selector1.setTerrainHintText("");
        return action;
    }

    public Action playPlayer2() {
        selector2.setInformationVisible(infoVisible2);
        selector2.setProgressText(missionTextProvider.apply(player2));
        selector2.setTerrainHintText(terrainHintTextProvider.apply(player2));
        selector2.setCompletedAchievementsBadgeCount(completedAchievementsBadgeCount);

        mod2.mod(BASE_SNAP);
        Action action = menu2.run();

        this.infoVisible2 = selector2.getInformationVisible();
        selector2.setProgressText("");
        selector2.setTerrainHintText("");
        return action;
    }

    public DynamicMenu<Action, Character> getMenu1() {
        mod1.mod(BASE_SNAP);
        return menu1;
    }

    public DynamicMenu<Action, Character> getMenu2() {
        mod2.mod(BASE_SNAP);
        return menu2;
    }

    public void setCompletedAchievementsBadgeCount(int count) {
        this.completedAchievementsBadgeCount = Math.max(0, count);
    }

    private static MenuStatusModifier configMenu(DynamicMenu<Action, Character> menu, Character player,
            Map<String, List<StatusMod>> modifiers, GameModeRules rules) {
        menu.saveCurrentAs(BASE_SNAP);
        return new MenuStatusModifier(player, menu, modifiers, rules);
    }
}
