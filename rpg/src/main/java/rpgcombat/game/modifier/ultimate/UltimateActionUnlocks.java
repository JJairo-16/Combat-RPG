package rpgcombat.game.modifier.ultimate;

import java.util.List;

import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.unlocks.UnlockEvaluator;
import rpgcombat.unlocks.UnlockMode;
import rpgcombat.unlocks.UnlockRequirement;
import rpgcombat.unlocks.UnlockRequirementType;
import rpgcombat.unlocks.UnlockRule;
import rpgcombat.unlocks.UnlockRuntime;

/**
 * Regles de desbloqueig de les ultis de segona etapa.
 *
 * <p>Aquesta classe centralitza les condicions necessàries per desbloquejar
 * cada definitiva segons el seu tipus. Les regles combinen assoliments i
 * descobriments, i es comproven contra l'estat global d'execució proporcionat
 * per {@link UnlockRuntime}.</p>
 */
public final class UltimateActionUnlocks {

    /**
     * Sobreescriptura opcional usada en proves per forçar el resultat del
     * desbloqueig sense modificar la persistència global.
     */
    private static Boolean testUnlockedOverride;

    /**
     * Regla de desbloqueig per a la definitiva màgica.
     */
    private static final UnlockRule ARCANE_RULE = new UnlockRule(UnlockMode.ALL, List.of(
            achievement("ARCANE_INITIATE"),
            discovery(DiscoveryCategory.ACTIONS, "CHARGE")));

    /**
     * Regla de desbloqueig per a la definitiva física.
     */
    private static final UnlockRule PHYSICAL_RULE = new UnlockRule(UnlockMode.ALL, List.of(
            achievement("FULL_FORCE"),
            discovery(DiscoveryCategory.ACTIONS, "CHARGE")));

    /**
     * Regla de desbloqueig per a la definitiva a distància.
     */
    private static final UnlockRule RANGE_RULE = new UnlockRule(UnlockMode.ALL, List.of(
            achievement("EAGLE_EYE"),
            discovery(DiscoveryCategory.ACTIONS, "CHARGE")));

    /**
     * Constructor privat per evitar instanciació.
     */
    private UltimateActionUnlocks() {
    }

    /**
     * Indica si una definitiva està desbloquejada segons les regles actuals.
     *
     * <p>Si hi ha una sobreescriptura de proves configurada, aquesta té
     * prioritat sobre l'avaluació real de les regles.</p>
     *
     * @param type tipus de definitiva a comprovar
     * @return {@code true} si la definitiva està desbloquejada;
     *         {@code false} en cas contrari
     */
    public static boolean isUnlocked(UltimateActionType type) {
        if (testUnlockedOverride != null) {
            return testUnlockedOverride.booleanValue();
        }
        return UnlockEvaluator.isUnlocked(ruleFor(type), UnlockRuntime.achievements(), UnlockRuntime.discoveries());
    }

    /**
     * Permet a les proves activar o desactivar els desbloquejos sense tocar la
     * persistència global.
     *
     * @param override valor que força el resultat del desbloqueig, o
     *                 {@code null} per tornar al comportament normal
     */
    static void setTestUnlockedOverride(Boolean override) {
        testUnlockedOverride = override;
    }

    /**
     * Retorna la regla de desbloqueig associada a un tipus de definitiva.
     *
     * <p>Si el tipus és {@code null}, retorna una regla buida amb mode
     * {@link UnlockMode#ALL}.</p>
     *
     * @param type tipus de definitiva
     * @return regla de desbloqueig corresponent
     */
    public static UnlockRule ruleFor(UltimateActionType type) {
        if (type == null) {
            return new UnlockRule(UnlockMode.ALL, List.of());
        }
        return switch (type) {
            case ARCANE_OVERLOAD -> ARCANE_RULE;
            case COLOSSAL_BREAK -> PHYSICAL_RULE;
            case ELVEN_OPENING_SHOT -> RANGE_RULE;
        };
    }

    /**
     * Crea un requisit de desbloqueig basat en un assoliment.
     *
     * @param id identificador de l'assoliment requerit
     * @return requisit de tipus {@link UnlockRequirementType#ACHIEVEMENT}
     */
    private static UnlockRequirement achievement(String id) {
        return new UnlockRequirement(UnlockRequirementType.ACHIEVEMENT, id, null, 0);
    }

    /**
     * Crea un requisit de desbloqueig basat en un descobriment.
     *
     * @param category categoria del descobriment requerit
     * @param id identificador del descobriment requerit
     * @return requisit de tipus {@link UnlockRequirementType#DISCOVERY}
     */
    private static UnlockRequirement discovery(DiscoveryCategory category, String id) {
        return new UnlockRequirement(UnlockRequirementType.DISCOVERY, id, category.name(), 0);
    }
}
