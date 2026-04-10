package rpgcombat.game.modifier;

import java.util.Map;

import menu.action.MenuAction;
import rpgcombat.combat.Action;
import rpgcombat.models.characters.Character;

public final class StatusModActionRegistry {
    private static final Map<String, MenuAction<Action, Character>> ACTIONS = Map.of(
        "exampleKey", Actions::exampleKey
    );

    private StatusModActionRegistry() {}

    public static MenuAction<Action, Character> resolve(String key) {
        MenuAction<Action, Character> action = ACTIONS.get(key);
        if (action == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return action;
    }

    public static MenuAction<Action, Character> resolve(String key, Map<String, MenuAction<Action, Character>> customActions) {
        MenuAction<Action, Character> action = customActions.get(key);
        if (action == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return action;
    }
}