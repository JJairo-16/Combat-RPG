package rpgcombat.perks.synergy;

import java.util.List;

/** Defineix l’escalat d’una sinergia segons els seus nivells. */
public record SynergyScaling(boolean enabled, List<SynergyLevel> levels) {

    /**
     * Normalitza la llista de nivells evitant valors nuls.
     */
    public SynergyScaling {
        levels = levels == null ? List.of() : List.copyOf(levels);
    }
}