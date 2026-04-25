package rpgcombat.combat.models;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;

/**
 * Estat d'un combatent en un instant concret.
 */
public record CombatantStatus(
        String name,
        double health,
        double maxHealth,
        double mana,
        double maxMana) {

    /**
     * Captura l'estat actual del personatge.
     */
    public static CombatantStatus from(Character character) {
        Statistics stats = character.getStatistics();
        return new CombatantStatus(
                character.getName(),
                stats.getHealth(),
                stats.getMaxHealth(),
                stats.getMana(),
                stats.getMaxMana());
    }
}
