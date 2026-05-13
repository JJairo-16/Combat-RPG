package rpgcombat.perks.synergy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Definició d’una sinergia entre perks. */
public record SynergyDefinition(
        String id,
        String name,
        String description,
        SynergyType type,
        List<String> requiredPerks,
        List<String> requiredTags,
        int minMembers,
        SynergyScaling scaling,
        List<MemberAlteration> alterations) {

    /**
     * Normalitza llistes i evita valors nuls o duplicats.
     */
    public SynergyDefinition {
        requiredPerks = normalize(requiredPerks, false);
        requiredTags = normalize(requiredTags, true);
        alterations = alterations == null ? List.of() : List.copyOf(alterations);
    }

    /**
     * Neteja i normalitza textos (opcionalment en majúscules).
     */
    private static List<String> normalize(List<String> values, boolean upper) {
        if (values == null || values.isEmpty())
            return List.of();

        Set<String> set = new LinkedHashSet<>();
        for (String v : values) {
            if (v == null || v.isBlank())
                continue;

            String s = v.trim();
            if (upper)
                s = s.toUpperCase();

            set.add(s);
        }

        return List.copyOf(set);
    }
}