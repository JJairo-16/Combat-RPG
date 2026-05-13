package rpgcombat.unlocks;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoverySystem;

/** Avalua requisits declaratius de desbloqueig de contingut. */
public final class UnlockEvaluator {
    private UnlockEvaluator() {}

    /**
     * Indica si una regla permet desbloquejar el contingut.
     *
     * @param rule regla a avaluar
     * @param achievements sistema d'assoliments
     * @param discoveries sistema de descobriments
     * @return {@code true} si el contingut queda desbloquejat
     */
    public static boolean isUnlocked(UnlockRule rule, AchievementSystem achievements, DiscoverySystem discoveries) {
        if (rule == null || rule.openByDefault()) {
            return true;
        }

        boolean any = false;
        for (UnlockRequirement req : rule.requirements()) {
            boolean ok = matches(req, achievements, discoveries);
            if (rule.mode() == UnlockMode.ALL && !ok) {
                return false;
            }
            any |= ok;
        }

        return rule.mode() == UnlockMode.ALL || any;
    }

    /**
     * Comprova si es compleix un requisit.
     *
     * @param req requisit a comprovar
     * @param achievements sistema d'assoliments
     * @param discoveries sistema de descobriments
     * @return {@code true} si el requisit es compleix
     */
    private static boolean matches(UnlockRequirement req, AchievementSystem achievements, DiscoverySystem discoveries) {
        if (req == null || req.type() == null) {
            return true;
        }

        return switch (req.type()) {
            case ACHIEVEMENT -> achievements != null && achievements.isCompleted(req.id());
            case TOTAL_ACHIEVEMENTS -> achievements != null && achievements.completedCount() >= req.amount();
            case DISCOVERY -> discoveries != null && discoveries.isDiscovered(parseCategory(req.category()), req.id());
            case TOTAL_DISCOVERIES -> discoveries != null && discoveries.discoveredCount() >= req.amount();
            case CATEGORY_DISCOVERIES -> discoveries != null && discoveries.discoveredCount(parseCategory(req.category())) >= req.amount();
            case TAGGED_DISCOVERIES -> discoveries != null
                    && discoveries.discoveredCount(parseCategory(req.category()), req.id()) >= req.amount();
        };
    }

    /**
     * Converteix un text en categoria de descobriment.
     *
     * @param category nom de la categoria
     * @return categoria corresponent, o {@code null} si no és vàlida
     */
    private static DiscoveryCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return DiscoveryCategory.valueOf(category.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
