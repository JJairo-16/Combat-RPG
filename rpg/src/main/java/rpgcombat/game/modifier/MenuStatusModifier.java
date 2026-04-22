package rpgcombat.game.modifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import menu.DynamicMenu;
import menu.editor.MenuEditor;
import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;

/**
 * Aplica modificadors d'estat al menú segons els efectes actius del personatge
 * i segons determinats estats interns del combat.
 *
 * <p>
 * Evita recomputacions i actualitzacions innecessàries mitjançant hash i cache.
 * </p>
 *
 * <p>
 * A més dels efectes, aquest modificador pot adaptar opcions base del menú
 * segons l'estat del personatge. Actualment:
 * </p>
 * <ul>
 * <li>Si el jugador té l'atac carregat, s'oculta l'opció {@link Action#CHARGE}</li>
 * <li>Si el jugador té l'atac carregat, l'opció {@link Action#ATTACK} mostra un
 * indicatiu visual</li>
 * </ul>
 *
 * <p>
 * El menú sempre es reconstrueix des del snapshot base abans d'aplicar
 * modificacions, de manera que quan l'estat deixa de complir-se, tot torna a la
 * normalitat automàticament.
 * </p>
 */
public class MenuStatusModifier {
    private static final String CHARGED_ATTACK_LABEL = "Atacar [CARREGAT]";

    private final Character player;
    private final DynamicMenu<Action, Character> menu;
    private final Map<String, List<StatusMod>> modifiers;

    private long lastHash = 0L;
    private int lastModifiers = 0;
    private boolean lastCharged = false;

    private final List<StatusMod> pending = new ArrayList<>();

    private static final Comparator<StatusMod> PRIORITY_DESC =
            Comparator.comparingInt(StatusMod::priority).reversed();

    /**
     * @param player Personatge del qual es llegeixen els efectes i estats interns
     * @param menu Menú a modificar
     * @param modifiers Map d'efectes a modificadors
     */
    public MenuStatusModifier(
            Character player,
            DynamicMenu<Action, Character> menu,
            Map<String, List<StatusMod>> modifiers) {
        this.player = player;
        this.menu = menu;
        this.modifiers = modifiers;
    }

    /**
     * Actualitza el menú segons els efectes actuals i l'estat de càrrega del
     * personatge.
     *
     * <p>
     * El procés és:
     * </p>
     * <ol>
     * <li>Recollir modificadors d'efectes actius</li>
     * <li>Detectar si el personatge té l'atac carregat</li>
     * <li>Comprovar si cal recalcular</li>
     * <li>Reconstruir des del snapshot base</li>
     * <li>Aplicar ajustos estructurals del menú</li>
     * <li>Aplicar modificadors d'efectes</li>
     * </ol>
     *
     * @param baseSnap snapshot base del menú sense modificacions transitòries
     */
    public void mod(String baseSnap) {
        List<Effect> effects = player.getEffects();
        boolean charged = player.hasChargedAttack();

        collectPendingMods(effects);
        sortPendingIfNeeded();

        long currentHash = computeHash(pending);
        if (!hasChanged(currentHash, charged)) {
            return;
        }

        menu.useSnapshot(baseSnap);

        applyChargedStateToBaseMenu(charged);
        applyPendingToMenu();

        rememberAppliedState(currentHash, charged);
    }

    /**
     * Recull els modificadors aplicables a partir dels efectes actius.
     *
     * <p>
     * Si no hi ha efectes o no hi ha mapa de modificadors, la llista pendent queda
     * buida.
     * </p>
     *
     * @param effects llista d'efectes actius del personatge
     */
    private void collectPendingMods(List<Effect> effects) {
        pending.clear();

        if (effects == null || effects.isEmpty() || modifiers == null || modifiers.isEmpty()) {
            return;
        }

        for (Effect effect : effects) {
            collectEffectMods(effect);
        }
    }

    /**
     * Afegeix els modificadors d'un efecte si compleixen les condicions.
     *
     * @param effect efecte a avaluar
     */
    private void collectEffectMods(Effect effect) {
        if (effect == null || effect.isExpired()) {
            return;
        }

        List<StatusMod> effectMods = modifiers.get(effect.key());
        if (effectMods == null || effectMods.isEmpty()) {
            return;
        }

        EffectState state = effect.state();
        for (StatusMod mod : effectMods) {
            if (!matches(state, mod) || !mod.isAvailable(player)) {
                continue;
            }

            pending.add(mod);
        }
    }

    /**
     * Ordena els modificadors per prioritat descendent si n'hi ha més d'un.
     */
    private void sortPendingIfNeeded() {
        if (pending.size() > 1) {
            pending.sort(PRIORITY_DESC);
        }
    }

    /**
     * Indica si l'estat efectiu del menú ha canviat respecte a l'última aplicació.
     *
     * @param currentHash hash actual dels modificadors d'efecte
     * @param charged estat actual de càrrega de l'atac
     * @return {@code true} si s'han de reaplicar canvis al menú
     */
    private boolean hasChanged(long currentHash, boolean charged) {
        return lastModifiers != pending.size()
                || lastHash != currentHash
                || lastCharged != charged;
    }

    /**
     * Aplica els ajustos estructurals del menú derivats de l'estat de càrrega.
     *
     * <p>
     * Si l'atac està carregat:
     * </p>
     * <ul>
     * <li>S'oculta l'opció {@link Action#CHARGE}</li>
     * <li>Es renombra l'opció {@link Action#ATTACK}</li>
     * </ul>
     *
     * <p>
     * Si no està carregat, no cal fer res perquè el menú ja s'ha reconstruït des
     * del snapshot base.
     * </p>
     *
     * @param charged {@code true} si el jugador té l'atac carregat
     */
    private void applyChargedStateToBaseMenu(boolean charged) {
        if (!charged) {
            return;
        }

        MenuEditor.remove(menu)
            .whereLabelEquals(Action.CHARGE.label())
            
            .thenReplace()
            .whereLabelEquals(Action.ATTACK.label())
            .label(CHARGED_ATTACK_LABEL)
            .execute();

    }

    /**
     * Aplica al menú els modificadors pendents recollits dels efectes.
     *
     * <p>
     * S'assumeix que el menú ja ha estat reconstruït des del snapshot base abans de
     * cridar aquest mètode.
     * </p>
     */
    private void applyPendingToMenu() {
        for (StatusMod mod : pending) {
            menu.addOptionAt(menu.optionCount() - 1, mod.label(), mod.action());
        }
    }

    /**
     * Desa l'estat aplicat per evitar recomputacions futures innecessàries.
     *
     * @param currentHash hash actual dels modificadors d'efecte
     * @param charged estat actual de càrrega de l'atac
     */
    private void rememberAppliedState(long currentHash, boolean charged) {
        lastModifiers = pending.size();
        lastHash = currentHash;
        lastCharged = charged;
    }

    /**
     * Verifica si un modificador compleix les condicions de l'estat.
     *
     * @param state estat actual de l'efecte
     * @param mod modificador a validar
     * @return {@code true} si el modificador és aplicable
     */
    private boolean matches(EffectState state, StatusMod mod) {
        return inRange(state.charges(), mod.minCharges(), mod.maxCharges())
                && inRange(state.stacks(), mod.minStacks(), mod.maxStacks())
                && inRange(state.remainingTurns(), mod.minRemainingTurns(), mod.maxRemainingTurns());
    }

    /**
     * Comprova si un valor està dins d'un rang amb límits opcionals.
     *
     * @param value valor a validar
     * @param min límit mínim, o {@code null} si no n'hi ha
     * @param max límit màxim, o {@code null}/{@code -1} si no n'hi ha
     * @return {@code true} si el valor és dins del rang permès
     */
    private boolean inRange(int value, Integer min, Integer max) {
        if (min != null && value < min) {
            return false;
        }

        if (max == null || max == -1) {
            return true;
        }

        return value <= max;
    }

    /**
     * Genera un hash del conjunt de modificadors per detectar canvis.
     *
     * @param mods llista de modificadors pendents
     * @return hash estable per a la sessió actual
     */
    private long computeHash(List<StatusMod> mods) {
        long hash = 1469598103934665603L;

        for (StatusMod mod : mods) {
            hash ^= System.identityHashCode(mod);
            hash *= 1099511628211L;
        }

        return hash;
    }
}