package rpgcombat.perks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Registre estàtic de perks disponibles. */
public final class PerkRegistry {
    private static List<PerkDefinition> perks = List.of();

    private PerkRegistry() {}

    public static void initialize(List<PerkDefinition> loaded) {
        perks = loaded == null ? List.of() : List.copyOf(loaded);
    }

    public static List<PerkDefinition> rollOptions(boolean corruptedOnly, int count, Random rng) {
        if (corruptedOnly) {
            return rollFromFamily(PerkFamily.CORRUPTED, count, rng);
        }

        List<PerkDefinition> result = new ArrayList<>(count);
        addIfPresent(result, rollFromFamily(PerkFamily.STRATEGY, 1, rng));
        addIfPresent(result, rollFromFamily(PerkFamily.LUCK, 1, rng));
        addIfPresent(result, rollFromFamily(PerkFamily.CHAOS, 1, rng));

        if (result.size() < count) {
            List<PerkDefinition> remaining = perks.stream()
                    .filter(p -> p.family() != PerkFamily.CORRUPTED)
                    .filter(p -> !result.contains(p))
                    .toList();
            result.addAll(rollWeighted(remaining, count - result.size(), rng));
        }
        return List.copyOf(result);
    }

    private static List<PerkDefinition> rollFromFamily(PerkFamily family, int count, Random rng) {
        List<PerkDefinition> pool = perks.stream()
                .filter(p -> p.family() == family)
                .toList();
        return rollWeighted(pool, count, rng);
    }

    private static List<PerkDefinition> rollWeighted(List<PerkDefinition> pool, int count, Random rng) {
        List<PerkDefinition> result = new ArrayList<>(count);
        List<PerkDefinition> remaining = new ArrayList<>(pool);
        while (!remaining.isEmpty() && result.size() < count) {
            PerkDefinition picked = pickWeighted(remaining, rng);
            result.add(picked);
            remaining.remove(picked);
        }
        return result;
    }

    private static void addIfPresent(List<PerkDefinition> result, List<PerkDefinition> picked) {
        if (picked != null && !picked.isEmpty()) result.add(picked.get(0));
    }

    private static PerkDefinition pickWeighted(List<PerkDefinition> pool, Random rng) {
        int total = pool.stream().mapToInt(PerkDefinition::weight).sum();
        int roll = rng.nextInt(Math.max(1, total));
        int acc = 0;
        for (PerkDefinition perk : pool) {
            acc += perk.weight();
            if (roll < acc) return perk;
        }
        return pool.get(pool.size() - 1);
    }
}
