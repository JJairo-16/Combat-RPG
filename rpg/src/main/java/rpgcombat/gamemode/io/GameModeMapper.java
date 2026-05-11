package rpgcombat.gamemode.io;

import java.util.EnumSet;
import java.util.List;

import rpgcombat.combat.models.Action;
import rpgcombat.gamemode.chaos.ChaosActivation;
import rpgcombat.gamemode.chaos.ChaosRules;
import rpgcombat.gamemode.cinematics.ModeCinematics;
import rpgcombat.gamemode.effects.ModeEffectDefinition;
import rpgcombat.gamemode.effects.ModeEffectTarget;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.model.GameModePresentation;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.unlocks.UnlockMode;
import rpgcombat.unlocks.UnlockRequirement;
import rpgcombat.unlocks.UnlockRequirementType;
import rpgcombat.unlocks.UnlockRule;

/** Converteix DTOs de JSON en model de domini de modes de joc. */
final class GameModeMapper {
    private GameModeMapper() {
    }

    static GameModeDefinition toDefinition(GameModeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("GameModeConfig no pot ser null.");
        }
        return new GameModeDefinition(
                config.id(),
                config.name(),
                config.description(),
                unlockRule(config.unlock()),
                rules(config.rules()),
                presentation(config.presentation(), config.description()),
                cinematics(config.cinematics()));
    }

    private static GameModeRules rules(GameModeRulesConfig config) {
        if (config == null) {
            return GameModeRules.unrestricted();
        }
        return new GameModeRules(
                actions(config.allowedActions()),
                bool(config.specialActionsEnabled(), true),
                intValue(config.maxPerks(), GameModeRules.DEFAULT_MAX_PERKS),
                bool(config.divinePerksEnabled(), true),
                bool(config.showUnlockableWeapons(), true),
                chaos(config.chaos()),
                modeEffects(config.modeEffects()));
    }

    private static EnumSet<Action> actions(List<String> rawActions) {
        if (rawActions == null || rawActions.isEmpty()) {
            return null;
        }
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        for (String raw : rawActions) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            actions.add(Action.valueOf(raw.trim().toUpperCase()));
        }
        return actions.isEmpty() ? null : actions;
    }

    private static ChaosRules chaos(ChaosRulesConfig config) {
        if (config == null) {
            return ChaosRules.defaultRules();
        }
        ChaosActivation activation = enumValue(ChaosActivation.class, config.activation(), ChaosActivation.PROBABILITY);
        return new ChaosRules(activation, doubleValue(config.probability(), 0.0));
    }

    private static List<ModeEffectDefinition> modeEffects(List<ModeEffectConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        return configs.stream()
                .filter(config -> config != null && config.id() != null && !config.id().isBlank())
                .map(config -> new ModeEffectDefinition(
                        config.id(),
                        enumValue(ModeEffectTarget.class, config.target(), ModeEffectTarget.BOTH),
                        config.parameters()))
                .toList();
    }

    private static ModeCinematics cinematics(ModeCinematicsConfig config) {
        if (config == null) {
            return ModeCinematics.normal();
        }
        return new ModeCinematics(config.postCreationPool(), config.chaosPostCreation());
    }

    private static GameModePresentation presentation(GameModePresentationConfig config, String description) {
        if (config == null) {
            return GameModePresentation.fallback(description);
        }
        return new GameModePresentation(
                config.shortDescription(),
                config.details(),
                config.lockedTitle(),
                config.lockedDescription(),
                config.lockedHints());
    }

    private static UnlockRule unlockRule(GameModeUnlockConfig config) {
        if (config == null || config.requirements() == null || config.requirements().isEmpty()) {
            return new UnlockRule(UnlockMode.ALL, List.of());
        }
        UnlockMode mode = enumValue(UnlockMode.class, config.mode(), UnlockMode.ALL);
        List<UnlockRequirement> requirements = config.requirements().stream()
                .filter(req -> req != null && req.type() != null && !req.type().isBlank())
                .map(req -> new UnlockRequirement(
                        UnlockRequirementType.valueOf(req.type().trim().toUpperCase()),
                        req.id(),
                        req.category(),
                        req.amount()))
                .toList();
        return new UnlockRule(mode, requirements);
    }

    private static boolean bool(Boolean value, boolean fallback) {
        return value == null ? fallback : value.booleanValue();
    }

    private static int intValue(Integer value, int fallback) {
        return value == null ? fallback : value.intValue();
    }

    private static double doubleValue(Double value, double fallback) {
        return value == null ? fallback : value.doubleValue();
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Enum.valueOf(type, raw.trim().toUpperCase());
    }
}
