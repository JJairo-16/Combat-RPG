package rpgcombat.game.modifier;

import menu.model.MenuResult;

import rpgcombat.models.characters.Character;
import rpgcombat.combat.Action;

public class Actions {
    private Actions() {
    }

    public static MenuResult<Action> exampleKey(Character player) {
        return MenuResult.repeatLoop();
    }
}
