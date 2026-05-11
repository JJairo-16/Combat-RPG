package rpgcombat.gamemode.io;

import java.util.List;
import java.util.Map;

/** DTO directe del JSON de modes de joc. */
record GameModeConfig(
        String id,
        String name,
        String description,
        GameModePresentationConfig presentation,
        GameModeUnlockConfig unlock,
        GameModeRulesConfig rules,
        ModeCinematicsConfig cinematics) {
}

/** DTO dels textos que es mostren a la carta del mode. */
record GameModePresentationConfig(
        String shortDescription,
        List<String> details,
        String lockedTitle,
        String lockedDescription,
        List<String> lockedHints) {
}

/** Configuració declarativa de desbloqueig de mode. */
record GameModeUnlockConfig(
        String mode,
        List<GameModeUnlockRequirementConfig> requirements) {
}

/** Requisit de desbloqueig de mode. */
record GameModeUnlockRequirementConfig(
        String type,
        String id,
        String category,
        int amount) {
}

/** DTO de regles configurables del mode. */
record GameModeRulesConfig(
        List<String> allowedActions,
        Boolean specialActionsEnabled,
        Integer maxPerks,
        Boolean divinePerksEnabled,
        Boolean showUnlockableWeapons,
        ChaosRulesConfig chaos,
        List<ModeEffectConfig> modeEffects) {
}

/** DTO de regles de Caos. */
record ChaosRulesConfig(
        String activation,
        Double probability) {
}

/** DTO d'efecte inicial declarat pel mode. */
record ModeEffectConfig(
        String id,
        String target,
        Map<String, Double> parameters) {
}

/** DTO de cinemàtiques de mode. */
record ModeCinematicsConfig(
        List<String> postCreationPool,
        String chaosPostCreation) {
}
