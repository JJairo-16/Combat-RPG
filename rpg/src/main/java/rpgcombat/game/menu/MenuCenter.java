package rpgcombat.game.menu;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import menu.DynamicMenu;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.MenuStatusModifier;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.interactive.MenuWithInformation;

public class MenuCenter {
    private static final String BASE_SNAP = "base";

    private final MenuWithInformation selector;
    private boolean infoVisible1 = false;
    private boolean infoVisible2 = false;

    private final DynamicMenu<Action, Character> menu1;
    private final DynamicMenu<Action, Character> menu2;

    private final MenuStatusModifier mod1;
    private final MenuStatusModifier mod2;

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers, Map<String, String> information) {
        
        this.selector = new MenuWithInformation(information);
        DynamicMenu<Action, Character> baseMenuAction = MenuBuilder.build(selector::getOption, changeWeaponHandler, showPlayerInfoHandler);

        this.menu1 = baseMenuAction.createChildMenu("Accions de " + player1.getName(), player1);
        this.menu2 = baseMenuAction.createChildMenu("Accions de " + player2.getName(), player2);

        this.mod1 = configMenu(menu1, player1, modifiers);
        this.mod2 = configMenu(menu2, player2, modifiers);
    }

    public Action playPlayer1() {
        selector.setInformationVisible(infoVisible1);

        mod1.mod(BASE_SNAP);
        Action action = menu1.run();

        this.infoVisible1 = selector.getInformationVisible();
        return action;
    }

    public Action playPlayer2() {
        selector.setInformationVisible(infoVisible2);

        mod2.mod(BASE_SNAP);
        Action action = menu2.run();

        this.infoVisible2 = selector.getInformationVisible();
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
