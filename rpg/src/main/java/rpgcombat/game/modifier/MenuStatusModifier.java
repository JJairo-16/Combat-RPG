package rpgcombat.game.modifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import menu.DynamicMenu;
import rpgcombat.combat.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;

public class MenuStatusModifier {
    private final Character player;
    private final DynamicMenu<Action, Character> menu;
    private final Map<String, List<StatusMod>> modifiers;

    public MenuStatusModifier(
            Character player,
            DynamicMenu<Action, Character> menu,
            Map<String, List<StatusMod>> modifiers) {
        this.player = player;
        this.menu = menu;
        this.modifiers = modifiers;
    }

    public void mod(String baseSnap) {
        List<StatusMod> pending = new ArrayList<>();
        List<Effect> effects = player.getEffects();

        if (effects.isEmpty() || modifiers.isEmpty()) {
            return;
        }

        for (Effect effect : effects) {
            if (effect == null || effect.isExpired()) {
                continue;
            }

            List<StatusMod> effectMods = modifiers.get(effect.key());
            if (effectMods == null || effectMods.isEmpty()) {
                continue;
            }

            for (StatusMod mod : effectMods) {
                if (matches(effect.state(), mod)) {
                    pending.add(mod);
                }
            }
        }

        if (pending.isEmpty()) {
            return;
        }

        pending.sort(Comparator.comparingInt(StatusMod::priority).reversed());

        menu.useSnapshot(baseSnap);
        for (StatusMod mod : pending) {
            menu.addOptionAt(menu.optionCount() - 1, mod.label(), mod.action());
        }
    }

    private boolean matches(EffectState state, StatusMod mod) {
        return inRange(state.charges(), mod.minCharges(), mod.maxCharges())
                && inRange(state.stacks(), mod.minStacks(), mod.maxStacks())
                && inRange(state.remainingTurns(), mod.minRemainingTurns(), mod.maxRemainingTurns());
    }

    private boolean inRange(int value, Integer min, Integer max) {
        if (min != null && value < min) {
            return false;
        }

        if (max == -1)
            return true;

        return (max != null && value <= max);
    }
}