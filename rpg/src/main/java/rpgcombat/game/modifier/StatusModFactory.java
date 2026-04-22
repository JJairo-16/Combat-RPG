package rpgcombat.game.modifier;

import java.util.Map;
import java.util.function.Predicate;

import menu.action.MenuAction;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.config.StatusModConfig;
import rpgcombat.models.characters.Character;

/**
 * Fàbrica per a la creació d'instàncies de {@link StatusMod}.
 * <p>
 * Aquesta classe s'encarrega de construir objectes {@code StatusMod} a partir
 * d'una configuració {@link StatusModConfig}, resolent també les accions i
 * les condicions de disponibilitat associades.
 * <p>
 * Proporciona suport tant per accions registrades per defecte com per
 * accions personalitzades.
 */
public final class StatusModFactory {

    /**
     * Constructor privat per evitar la instanciació.
     */
    private StatusModFactory() {
    }

    /**
     * Crea un {@link StatusMod} utilitzant el registre d'accions per defecte.
     *
     * @param cfg configuració del StatusMod
     * @return una nova instància de {@link StatusMod}
     * @throws IllegalArgumentException si la configuració és invàlida
     */
    public static StatusMod create(StatusModConfig cfg) {
        validate(cfg);

        return new StatusMod(
                cfg.priority(),
                cfg.minCharges(),
                cfg.maxCharges(),
                cfg.minStacks(),
                cfg.maxStacks(),
                cfg.minRemainingTurns(),
                cfg.maxRemainingTurns(),
                cfg.label(),
                cfg.actionKey(),
                StatusModActionRegistry.resolve(cfg.actionKey()),
                StatusModActionRegistry.resolveAvailability(cfg.actionKey()));
    }

    /**
     * Crea un {@link StatusMod} utilitzant accions i disponibilitat personalitzades.
     * <p>
     * Si es proporcionen mapes personalitzats, aquests es fan servir per resoldre
     * l'acció i la seva disponibilitat en lloc del registre per defecte.
     *
     * @param cfg configuració del StatusMod
     * @param customActions mapa d'accions personalitzades indexades per {@code actionKey}
     * @param customAvailability mapa de predicats que indiquen si una acció està disponible
     * @return una nova instància de {@link StatusMod}
     * @throws IllegalArgumentException si la configuració és invàlida
     */
    public static StatusMod create(
            StatusModConfig cfg,
            Map<String, MenuAction<Action, Character>> customActions,
            Map<String, Predicate<Character>> customAvailability) {
        validate(cfg);

        return new StatusMod(
                cfg.priority(),
                cfg.minCharges(),
                cfg.maxCharges(),
                cfg.minStacks(),
                cfg.maxStacks(),
                cfg.minRemainingTurns(),
                cfg.maxRemainingTurns(),
                cfg.label(),
                cfg.actionKey(),
                StatusModActionRegistry.resolve(cfg.actionKey(), customActions),
                StatusModActionRegistry.resolveAvailability(cfg.actionKey(), customAvailability));
    }

    /**
     * Valida la configuració del {@link StatusMod}.
     * <p>
     * Comprova que:
     * <ul>
     *   <li>La configuració no sigui {@code null}</li>
     *   <li>El {@code label} no sigui nul ni buit</li>
     *   <li>L'{@code actionKey} no sigui nul ni buit</li>
     * </ul>
     *
     * @param cfg configuració a validar
     * @throws IllegalArgumentException si alguna validació falla
     */
    private static void validate(StatusModConfig cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("StatusModConfig no pot ser null");
        }

        if (cfg.label() == null || cfg.label().isBlank()) {
            throw new IllegalArgumentException("El label del StatusMod no pot estar buit");
        }

        if (cfg.actionKey() == null || cfg.actionKey().isBlank()) {
            throw new IllegalArgumentException("El actionKey del StatusMod no pot estar buit");
        }
    }
}