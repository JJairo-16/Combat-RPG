package rpgcombat.weapons.config;

/** DTO directe del JSON per a un requisit de desbloqueig. */
public record UnlockRequirementConfig(
        String type,
        String id,
        String category,
        int amount) {
}
