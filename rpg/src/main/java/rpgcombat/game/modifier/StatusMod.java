package rpgcombat.game.modifier;

import java.util.function.Predicate;

import menu.action.MenuAction;
import rpgcombat.combat.Action;
import rpgcombat.models.characters.Character;

public record StatusMod(
        int priority,
        Integer minCharges,
        Integer maxCharges,
        Integer minStacks,
        Integer maxStacks,
        Integer minRemainingTurns,
        Integer maxRemainingTurns,
        String label,
        String actionKey,
        MenuAction<Action, Character> action,
        Predicate<Character> availability) {

    public StatusMod(
            int priority,
            int minCharges,
            int maxCharges,
            int minStacks,
            int maxStacks,
            int minRemainingTurns,
            int maxRemainingTurns,
            String label,
            String actionKey,
            MenuAction<Action, Character> action,
            Predicate<Character> availability) {
        this(
                priority,
                Integer.valueOf(minCharges), Integer.valueOf(maxCharges),
                Integer.valueOf(minStacks), Integer.valueOf(maxStacks),
                Integer.valueOf(minRemainingTurns), Integer.valueOf(maxRemainingTurns),
                label,
                actionKey,
                action,
                availability);
    }

    public StatusMod(
            int priority,
            String label,
            String actionKey,
            MenuAction<Action, Character> action,
            Predicate<Character> availability) {
        this(priority, 0, -1, 0, -1, 1, -1, label, actionKey, action, availability);
    }

    public boolean isAvailable(Character player) {
        return availability != null && availability.test(player);
    }
}