package rpgcombat.unlocks;

/** Requisit concret d'una regla de desbloqueig. */
public record UnlockRequirement(
        UnlockRequirementType type,
        String id,
        String category,
        int amount) {
}
