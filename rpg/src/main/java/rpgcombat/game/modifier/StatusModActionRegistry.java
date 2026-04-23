package rpgcombat.game.modifier;

import java.util.Map;
import java.util.function.Predicate;

import menu.action.MenuAction;
import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;

/**
 * Registre d'accions i condicions de disponibilitat per a modificadors d'estat.
 */
public final class StatusModActionRegistry {

    private static final Map<String, MenuAction<Action, Character>> ACTIONS = Map.of(
            "spiritualCalling", Actions::spiritualCalling,
            "bloodPact", Actions::bloodPact
    );

    private static final Map<String, Predicate<Character>> AVAILABILITY = Map.of(
            "spiritualCalling", Character::canUseSpiritualCalling,
            "bloodPact", p -> true
    );

    private StatusModActionRegistry() {
    }

    /**
     * Resol una acció per la seva clau.
     *
     * @param key identificador de l'acció
     * @return acció associada
     */
    public static MenuAction<Action, Character> resolve(String key) {
        MenuAction<Action, Character> action = ACTIONS.get(key);
        if (action == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return action;
    }

    /**
     * Resol la condició de disponibilitat d'una acció.
     *
     * @param key identificador de l'acció
     * @return predicat de disponibilitat
     */
    public static Predicate<Character> resolveAvailability(String key) {
        Predicate<Character> predicate = AVAILABILITY.get(key);
        if (predicate == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return predicate;
    }

    /**
     * Resol una acció utilitzant un registre personalitzat.
     *
     * @param key identificador de l'acció
     * @param customActions mapa d'accions
     * @return acció associada
     */
    public static MenuAction<Action, Character> resolve(
            String key,
            Map<String, MenuAction<Action, Character>> customActions) {
        MenuAction<Action, Character> action = customActions.get(key);
        if (action == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return action;
    }

    /**
     * Resol la disponibilitat utilitzant un registre personalitzat.
     *
     * @param key identificador de l'acció
     * @param customAvailability mapa de disponibilitat
     * @return predicat de disponibilitat
     */
    public static Predicate<Character> resolveAvailability(
            String key,
            Map<String, Predicate<Character>> customAvailability) {
        Predicate<Character> predicate = customAvailability.get(key);
        if (predicate == null) {
            throw new IllegalArgumentException("Clau d'acció desconeguda: " + key);
        }
        return predicate;
    }
}