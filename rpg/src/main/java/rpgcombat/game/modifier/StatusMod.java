package rpgcombat.game.modifier;

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
            MenuAction<Action, Character> action) {

        public StatusMod(
            int priority,
            int minCharges,
            int maxCharges,
            int minStacks,
            int maxStacks,
            int minRemainingTurns,
            int maxRemainingTurns,
            String label,
            MenuAction<Action, Character> action
        ) {
            this(
                priority,
                Integer.valueOf(minCharges), Integer.valueOf(maxCharges),
                Integer.valueOf(minStacks), Integer.valueOf(maxStacks),
                Integer.valueOf(minRemainingTurns), Integer.valueOf(maxRemainingTurns),
                label, action);
        }

        public StatusMod(int priority, String label, MenuAction<Action, Character> action) {
            this(priority, 0, -1, 0, -1, 1, -1, label, action);
        }
    }
