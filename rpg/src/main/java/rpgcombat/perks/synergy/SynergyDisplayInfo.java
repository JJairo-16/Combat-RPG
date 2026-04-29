package rpgcombat.perks.synergy;

/** Informació bàsica per mostrar una sinergia a la UI. */
public record SynergyDisplayInfo(
        String name,
        String description,
        SynergyType type
) {
}