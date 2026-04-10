package rpgcombat.game;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import menu.DynamicMenu;
import rpgcombat.combat.Action;
import rpgcombat.game.modifier.MenuStatusModifier;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.models.characters.Character;

public class MenuCenter {
    private static final String BASE_SNAP = "base";

    private final DynamicMenu<Action, Character> menu1;
    private final DynamicMenu<Action, Character> menu2;

    private final MenuStatusModifier mod1;
    private final MenuStatusModifier mod2;

    public MenuCenter(Character player1, Character player2, Consumer<Character> changeWeaponHandler,
            Consumer<Character> showPlayerInfoHandler, Map<String, List<StatusMod>> modifiers) {
        DynamicMenu<Action, Character> baseMenuAction = MenuBuilder.build(changeWeaponHandler, showPlayerInfoHandler);

        this.menu1 = baseMenuAction.createChildMenu("Accions de " + player1.getName(), player1);
        this.menu2 = baseMenuAction.createChildMenu("Accions de " + player2.getName(), player2);

        this.menu1.saveCurrentAs(BASE_SNAP);
        this.menu2.saveCurrentAs(BASE_SNAP);

        this.mod1 = new MenuStatusModifier(player1, menu1, modifiers);
        this.mod2 = new MenuStatusModifier(player2, menu2, modifiers);
    }

    public Action playPlayer1() {
        mod1.mod(BASE_SNAP);
        return menu1.run();
    }

    public Action playPlayer2() {
        mod2.mod(BASE_SNAP);
        return menu2.run();
    }
}
