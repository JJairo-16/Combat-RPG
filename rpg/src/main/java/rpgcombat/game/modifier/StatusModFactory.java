package rpgcombat.game.modifier;

import java.util.Map;
import java.util.function.Predicate;

import menu.action.MenuAction;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.config.StatusModConfig;
import rpgcombat.models.characters.Character;

public final class StatusModFactory {
    private StatusModFactory() {
    }

    public static StatusMod create(StatusModConfig cfg) {
        validate(cfg);

        return new StatusMod(
                cfg.priority(),
                cfg.minCharges(),
                cfg.maxCharges(),
                cfg.minStacks(),
                cfg.maxStacks(),
                cfg.minRemainingTurns(),
                cfg.maxRemainingTurns(),
                cfg.label(),
                cfg.actionKey(),
                StatusModActionRegistry.resolve(cfg.actionKey()),
                StatusModActionRegistry.resolveAvailability(cfg.actionKey()));
    }

    public static StatusMod create(
            StatusModConfig cfg,
            Map<String, MenuAction<Action, Character>> customActions,
            Map<String, Predicate<Character>> customAvailability) {
        validate(cfg);

        return new StatusMod(
                cfg.priority(),
                cfg.minCharges(),
                cfg.maxCharges(),
                cfg.minStacks(),
                cfg.maxStacks(),
                cfg.minRemainingTurns(),
                cfg.maxRemainingTurns(),
                cfg.label(),
                cfg.actionKey(),
                StatusModActionRegistry.resolve(cfg.actionKey(), customActions),
                StatusModActionRegistry.resolveAvailability(cfg.actionKey(), customAvailability));
    }

    private static void validate(StatusModConfig cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("StatusModConfig no pot ser null");
        }

        if (cfg.label() == null || cfg.label().isBlank()) {
            throw new IllegalArgumentException("El label del StatusMod no pot estar buit");
        }

        if (cfg.actionKey() == null || cfg.actionKey().isBlank()) {
            throw new IllegalArgumentException("El actionKey del StatusMod no pot estar buit");
        }
    }
}