package rpgcombat.perks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manté el registre global de perks disponibles i en genera opcions.
 */
public final class PerkRegistry {
    private static List<PerkDefinition> perks = List.of();

    private PerkRegistry() {}

    /**
     * Inicialitza el registre amb les perks carregades.
     *
     * @param loaded llista de perks
     */
    public static void initialize(List<PerkDefinition> loaded) {
        perks = loaded == null ? List.of() : List.copyOf(loaded);
    }

    /**
     * Genera opcions de perk segons el mode indicat.
     *
     * @param corruptedOnly si només es poden oferir perks corruptes
     * @param count nombre màxim d'opcions
     * @param rng generador aleatori
     * @return llista d'opcions generades
     */
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

    /** Genera perks aleatòries d'una família concreta. */
    private static List<PerkDefinition> rollFromFamily(PerkFamily family, int count, Random rng) {
        List<PerkDefinition> pool = perks.stream()
                .filter(p -> p.family() == family)
                .toList();
        return rollWeighted(pool, count, rng);
    }

    /** Selecciona perks per pes sense repetir-les. */
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

    /** Afegeix la primera perk si existeix. */
    private static void addIfPresent(List<PerkDefinition> result, List<PerkDefinition> picked) {
        if (picked != null && !picked.isEmpty()) result.add(picked.get(0));
    }

    /** Selecciona una perk segons el seu pes. */
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