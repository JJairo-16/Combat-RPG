package rpgcombat.game.modifier;

import java.util.Map;
import java.util.function.Predicate;

import menu.action.MenuAction;
import rpgcombat.combat.Action;
import rpgcombat.models.characters.Character;

public final class StatusModActionRegistry {

    private static final Map<String, MenuAction<Action, Character>> ACTIONS = Map.of(
            "spiritualCalling", Actions::spiritualCalling
    );

    private static final Map<String, Predicate<Character>> AVAILABILITY = Map.of(
            "spiritualCalling", Character::canUseSpiritualCalling
    );

    private StatusModActionRegistry() {
    }

    public static MenuAction<Action, Character> resolve(String key) {
        MenuAction<Action, Character> action = ACTIONS.get(key);
        if (action == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return action;
    }

    public static Predicate<Character> resolveAvailability(String key) {
        Predicate<Character> predicate = AVAILABILITY.get(key);
        if (predicate == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return predicate;
    }

    public static MenuAction<Action, Character> resolve(
            String key,
            Map<String, MenuAction<Action, Character>> customActions) {
        MenuAction<Action, Character> action = customActions.get(key);
        if (action == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return action;
    }

    public static Predicate<Character> resolveAvailability(
            String key,
            Map<String, Predicate<Character>> customAvailability) {
        Predicate<Character> predicate = customAvailability.get(key);
        if (predicate == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return predicate;
    }
}