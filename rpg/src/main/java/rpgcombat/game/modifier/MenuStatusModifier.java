package rpgcombat.game.modifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import menu.DynamicMenu;
import rpgcombat.combat.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;

/**
 * Aplica modificadors d'estat al menú segons els efectes actius del personatge.
 * Evita recomputacions i actualitzacions innecessàries mitjançant hash i cache.
 */
public class MenuStatusModifier {
    private final Character player;
    private final DynamicMenu<Action, Character> menu;
    private final Map<String, List<StatusMod>> modifiers;

    private long lastHash = 0;
    private int lastModifiers = 0;
    private final List<StatusMod> pending = new ArrayList<>();

    private static final Comparator<StatusMod> PRIORITY_DESC =
            Comparator.comparingInt(StatusMod::priority).reversed();

    /**
     * @param player Personatge del qual es llegeixen els efectes
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
     * Actualitza el menú segons els efectes actuals.
     * Només aplica canvis si el resultat difereix del darrer estat.
     */
    public void mod(String baseSnap) {
        List<Effect> effects = player.getEffects();

        if (hasNoSourceData(effects)) {
            clearMenuIfNeeded(baseSnap);
            return;
        }

        collectPendingMods(effects);

        if (pending.isEmpty()) {
            clearMenuIfNeeded(baseSnap);
            return;
        }

        sortPendingIfNeeded();

        long currentHash = computeHash(pending);
        if (!hasChanged(currentHash)) {
            return;
        }

        applyPendingToMenu(baseSnap);
        rememberAppliedState(currentHash);
    }

   /** Indica si no hi ha dades per generar modificadors. */
    private boolean hasNoSourceData(List<Effect> effects) {
        return effects.isEmpty() || modifiers.isEmpty();
    }

   /** Restaura el menú si abans hi havia modificadors aplicats. */
    private void clearMenuIfNeeded(String baseSnap) {
        if (lastModifiers == 0) {
            return;
        }

        menu.useSnapshot(baseSnap);
        resetAppliedState();
    }

   /** Reinicia l'estat cachejat. */
    private void resetAppliedState() {
        lastModifiers = 0;
        lastHash = 0L;
    }

   /** Recull els modificadors aplicables a partir dels efectes. */
    private void collectPendingMods(List<Effect> effects) {
        pending.clear();

        for (Effect effect : effects) {
            collectEffectMods(effect);
        }
    }

   /** Afegeix els modificadors d'un efecte si compleixen les condicions. */
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

   /** Ordena els modificadors per prioritat descendent si cal. */
    private void sortPendingIfNeeded() {
        if (pending.size() > 1) {
            pending.sort(PRIORITY_DESC);
        }
    }

   /** Comprova si el resultat actual difereix de l'anterior. */
    private boolean hasChanged(long currentHash) {
        return lastModifiers != pending.size() || lastHash != currentHash;
    }

   /** Aplica els modificadors al menú reconstruint-lo des del snapshot base. */
    private void applyPendingToMenu(String baseSnap) {
        menu.useSnapshot(baseSnap);

        for (StatusMod mod : pending) {
            menu.addOptionAt(menu.optionCount() - 1, mod.label(), mod.action());
        }
    }

   /** Desa l'estat actual per evitar recomputacions futures. */
    private void rememberAppliedState(long currentHash) {
        lastModifiers = pending.size();
        lastHash = currentHash;
    }

   /** Verifica si un modificador compleix les condicions de l'estat. */
    private boolean matches(EffectState state, StatusMod mod) {
        return inRange(state.charges(), mod.minCharges(), mod.maxCharges())
                && inRange(state.stacks(), mod.minStacks(), mod.maxStacks())
                && inRange(state.remainingTurns(), mod.minRemainingTurns(), mod.maxRemainingTurns());
    }

   /** Comprova si un valor està dins d'un rang (amb límits opcionals). */
    private boolean inRange(int value, Integer min, Integer max) {
        if (min != null && value < min)
            return false;

        if (max == null || max == -1)
            return true;

        return value <= max;
    }

   /** Genera un hash del conjunt de modificadors per detectar canvis. */
    private long computeHash(List<StatusMod> mods) {
        long hash = 1469598103934665603L;

        for (int i = 0; i < mods.size(); i++) {
            StatusMod mod = mods.get(i);

            hash ^= System.identityHashCode(mod);
            hash *= 1099511628211L;
        }

        return hash;
    }
}