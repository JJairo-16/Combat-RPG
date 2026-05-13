package rpgcombat.perks.synergy;

/** Informació bàsica per mostrar una sinergia a la UI i exposar-la als assoliments. */
public record SynergyDisplayInfo(
        String id,
        String name,
        String description,
        SynergyType type,
        int members,
        int rank
) {
}
