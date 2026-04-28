package rpgcombat.game.menu;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import menu.DynamicMenu;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.MenuStatusModifier;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.interactive.MenuWithInformation;

public class MenuCenter {
    private static final String BASE_SNAP = "base";

    private final MenuWithInformation selector1;
    private final MenuWithInformation selector2;
    private boolean infoVisible1 = false;
    private boolean infoVisible2 = false;

    private final Character player1;
    private final Character player2;
    private final DynamicMenu<Action, Character> menu1;
    private final DynamicMenu<Action, Character> menu2;

    private final MenuStatusModifier mod1;
    private final MenuStatusModifier mod2;
    private Function<Character, String> missionTextProvider = player -> "";

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers, Map<String, String> information) {

        this.player1 = player1;
        this.player2 = player2;

        this.selector1 = new MenuWithInformation(information);
        this.selector2 = new MenuWithInformation(information);

        DynamicMenu<Action, Character> baseMenuAction1 = MenuBuilder.build(selector1::getOption, changeWeaponHandler,
                showPlayerInfoHandler);
        DynamicMenu<Action, Character> baseMenuAction2 = MenuBuilder.build(selector2::getOption, changeWeaponHandler,
                showPlayerInfoHandler);

        this.menu1 = baseMenuAction1.createChildMenu("Accions de " + player1.getName(), player1);
        this.menu2 = baseMenuAction2.createChildMenu("Accions de " + player2.getName(), player2);

        this.mod1 = configMenu(menu1, player1, modifiers);
        this.mod2 = configMenu(menu2, player2, modifiers);
    }

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers, Map<String, String> information,
            Function<Character, String> missionTextProvider) {
        this(player1, player2, changeWeaponHandler, showPlayerInfoHandler, modifiers, information);
        setMissionTextProvider(missionTextProvider);
    }

    public void setMissionTextProvider(Function<Character, String> missionTextProvider) {
        this.missionTextProvider = missionTextProvider == null ? player -> "" : missionTextProvider;
    }

    public Action playPlayer1() {
        selector1.setInformationVisible(infoVisible1);
        selector1.setBottomRightMissionText(missionTextProvider.apply(player1));

        mod1.mod(BASE_SNAP);
        Action action = menu1.run();

        this.infoVisible1 = selector1.getInformationVisible();
        selector1.setBottomRightMissionText("");
        return action;
    }

    public Action playPlayer2() {
        selector2.setInformationVisible(infoVisible2);
        selector2.setBottomRightMissionText(missionTextProvider.apply(player2));

        mod2.mod(BASE_SNAP);
        Action action = menu2.run();

        this.infoVisible2 = selector2.getInformationVisible();
        selector2.setBottomRightMissionText("");
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

    private static MenuStatusModifier configMenu(DynamicMenu<Action, Character> menu, Character player,
            Map<String, List<StatusMod>> modifiers) {
        menu.saveCurrentAs(BASE_SNAP);
        return new MenuStatusModifier(player, menu, modifiers);
    }
}
