package rpgcombat.creator;

import rpgcombat.gamemode.model.GameModeRules;

/** Opcions de creació de personatge derivades del mode de joc. */
public record CharacterCreationOptions(
        boolean specialActionsEnabled,
        boolean divinePerksEnabled) {

    public static CharacterCreationOptions defaultOptions() {
        return new CharacterCreationOptions(true, true);
    }

    public static CharacterCreationOptions from(GameModeRules rules) {
        if (rules == null) {
            return defaultOptions();
        }
        return new CharacterCreationOptions(rules.specialActionsEnabled(), rules.divinePerksEnabled());
    }
}
