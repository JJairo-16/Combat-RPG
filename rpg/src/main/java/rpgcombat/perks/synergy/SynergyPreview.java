package rpgcombat.perks.synergy;

import java.util.List;

/** Resultat resumit de sinergies actives i millorades. */
public record SynergyPreview(
        boolean hasAny,
        List<String> activatedSynergyNames,
        List<String> upgradedSynergyNames) {

    /**
     * Normalitza les llistes evitant valors nuls.
     */
    public SynergyPreview {
        activatedSynergyNames = activatedSynergyNames == null ? List.of() : List.copyOf(activatedSynergyNames);
        upgradedSynergyNames = upgradedSynergyNames == null ? List.of() : List.copyOf(upgradedSynergyNames);
    }

    /** Retorna una previsualització buida. */
    public static SynergyPreview empty() {
        return new SynergyPreview(false, List.of(), List.of());
    }
}