package rpgcombat.gamemode.model;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import rpgcombat.combat.models.Action;
import rpgcombat.gamemode.chaos.ChaosRules;
import rpgcombat.gamemode.effects.ModeEffectDefinition;

/** Regles efectives que un mode de joc aplica sobre una partida. */
public record GameModeRules(
        Set<Action> allowedActions,
        boolean specialActionsEnabled,
        int maxPerks,
        boolean divinePerksEnabled,
        boolean showUnlockableWeapons,
        ChaosRules chaos,
        List<ModeEffectDefinition> modeEffects) {

    public static final int DEFAULT_MAX_PERKS = 4;

    public GameModeRules {
        allowedActions = normalizeActions(allowedActions);
        maxPerks = Math.max(0, maxPerks);
        chaos = chaos == null ? ChaosRules.defaultRules() : chaos;
        modeEffects = modeEffects == null ? List.of() : List.copyOf(modeEffects);
    }

    /** Mode normal sense restriccions d'acció. */
    public static GameModeRules unrestricted() {
        return new GameModeRules(
                Set.of(),
                true,
                DEFAULT_MAX_PERKS,
                true,
                true,
                ChaosRules.defaultRules(),
                List.of());
    }

    /** Mode principiant amb mecàniques reduïdes. */
    public static GameModeRules beginner() {
        return new GameModeRules(
                EnumSet.of(Action.ATTACK, Action.DEFEND, Action.DODGE),
                false,
                1,
                false,
                false,
                ChaosRules.disabled(),
                List.of());
    }

    /** Una llista buida o absent significa que no hi ha restriccions. */
    public boolean hasActionRestrictions() {
        return !allowedActions.isEmpty();
    }

    /** Indica si l'acció és vàlida en aquest mode. */
    public boolean allowsAction(Action action) {
        return action != null && (!hasActionRestrictions() || allowedActions.contains(action));
    }

    /** Valida una acció o falla ràpid si un caller intenta saltar-se el mode. */
    public Action requireAllowed(Action action) {
        if (!allowsAction(action)) {
            String label = action == null ? "null" : action.name();
            throw new IllegalArgumentException("L'acció " + label + " no està permesa en aquest mode de joc.");
        }
        return action;
    }

    private static Set<Action> normalizeActions(Set<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(actions);
    }
}
