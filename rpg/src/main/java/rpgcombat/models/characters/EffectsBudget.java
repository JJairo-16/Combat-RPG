package rpgcombat.models.characters;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.types.ActionMenuHintEffect;
import rpgcombat.models.effects.types.EndRoundRecoveryEffect;
import rpgcombat.models.effects.types.MenuTurnEffect;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.weapons.passives.HitContext;

/**
 * Contenidor indexat d'efectes preparat per substituir els recorreguts lineals
 * que encara viuen a {@link Character}.
 *
 * <p>
 * El pressupost manté l'ordre per prioritat i crea índexs per clau, fase del
 * pipeline i jerarquia de tipus. Els tipus s'indexen descobrint automàticament
 * classes i interfícies de cada efecte; un tipus nou queda disponible sense
 * afegir un bucket manual aquí. Encara no és la font de veritat de
 * {@code Character}: es pot provar i completar en paral·lel abans de migrar
 * l'emmagatzematge existent.
 * </p>
 */
public final class EffectsBudget {
    private static final Comparator<Effect> EFFECT_ORDER =
            Comparator.comparingInt(Effect::priority).reversed();

    /**
     * Detectar la implementació de fases és una propietat del tipus, no de cada
     * instància. El cache evita repetir reflexió quan un efecte s'aplica sovint.
     */
    private static final Map<Class<?>, Set<HitContext.Phase>> PHASES_BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> INDEXED_TYPES_BY_TYPE = new ConcurrentHashMap<>();

    private final List<Effect> effects = new ArrayList<>();
    private final Map<String, Effect> effectsByKey = new HashMap<>();
    private final EnumMap<HitContext.Phase, List<Effect>> phaseEffects =
            new EnumMap<>(HitContext.Phase.class);
    private final Map<Class<?>, List<Effect>> effectsByType = new HashMap<>();

    private final Runnable clearSpecialMenuActionUsedThisTurn;

    /**
     * Crea un contenidor sense callback de menú.
     */
    public EffectsBudget() {
        this(() -> {
        });
    }

    /**
     * Crea un contenidor i rep l'acció que neteja l'ús d'accions especials al
     * final del torn de menú.
     */
    public EffectsBudget(Runnable clearSpecialMenuActionUsedThisTurn) {
        this.clearSpecialMenuActionUsedThisTurn =
                clearSpecialMenuActionUsedThisTurn == null ? () -> {
                } : clearSpecialMenuActionUsedThisTurn;

        for (HitContext.Phase phase : HitContext.Phase.values()) {
            phaseEffects.put(phase, new ArrayList<>());
        }
    }

    /**
     * Indica si no hi ha efectes gestionats.
     */
    public boolean isEmpty() {
        return effects.isEmpty();
    }

    /**
     * Retorna el nombre d'efectes gestionats.
     */
    public int size() {
        return effects.size();
    }

    /**
     * Retorna una instantània immutable ordenada per prioritat.
     */
    public List<Effect> getEffects() {
        return List.copyOf(effects);
    }

    /**
     * Retorna els efectes que poden reaccionar a una fase concreta.
     */
    public List<Effect> effectsForPhase(HitContext.Phase phase) {
        List<Effect> indexed = phaseEffects.get(phase);
        return indexed == null ? List.of() : List.copyOf(indexed);
    }

    /**
     * Retorna una instantània dels efectes indexats per classe o interfície.
     *
     * <p>
     * La consulta no necessita que el tipus estigui declarat a aquest objecte.
     * Quan un efecte nou implementa una interfície nova, el bucket s'alimenta en
     * afegir-lo i aquesta consulta el pot recuperar directament.
     * </p>
     */
    public <T> List<T> effectsOfType(Class<T> type) {
        if (type == null) {
            return List.of();
        }

        List<Effect> indexed = effectsByType.get(type);
        if (indexed == null || indexed.isEmpty()) {
            return List.of();
        }

        List<T> typed = new ArrayList<>(indexed.size());
        for (Effect effect : indexed) {
            typed.add(type.cast(effect));
        }
        return List.copyOf(typed);
    }

    /**
     * Consulta O(1) per saber si hi ha almenys un efecte d'un tipus indexat.
     */
    public boolean containsType(Class<?> type) {
        List<Effect> indexed = effectsByType.get(type);
        return indexed != null && !indexed.isEmpty();
    }

    /**
     * Consulta O(1) per saber si hi ha algun trigger.
     */
    public boolean containsTrigger() {
        return containsType(Trigger.class);
    }

    /**
     * Consulta O(1) d'un trigger concret per clau.
     */
    public boolean hasTrigger(String key) {
        Effect effect = getEffect(key);
        return effect instanceof Trigger;
    }

    /**
     * Retorna un trigger concret per clau sense recórrer la col·lecció.
     */
    public Trigger getTrigger(String key) {
        Effect effect = getEffect(key);
        return effect instanceof Trigger trigger ? trigger : null;
    }

    /**
     * Comprova si hi ha un efecte no expirat amb la clau indicada.
     */
    public boolean hasEffect(String key) {
        return getEffect(key) != null;
    }

    /**
     * Retorna l'efecte no expirat amb la clau indicada.
     */
    public Effect getEffect(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        Effect effect = effectsByKey.get(key);
        return effect == null || effect.isExpired() ? null : effect;
    }

    /**
     * Elimina l'efecte associat a una clau.
     */
    public boolean removeEffect(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        Effect effect = effectsByKey.get(key);
        if (effect == null) {
            return false;
        }

        return remove(effect);
    }

    /**
     * Afegeix un efecte aplicant la seva regla d'apilament.
     */
    public void addEffect(Effect incoming) {
        if (incoming == null) {
            return;
        }

        Effect existing = effectsByKey.get(incoming.key());
        if (existing != null) {
            StackingRule rule = existing.stackingRule();
            switch (rule) {
                case IGNORE -> {
                    return;
                }
                case REPLACE -> {
                    replace(existing, incoming);
                    return;
                }
                case REFRESH, STACK -> {
                    existing.mergeFrom(incoming);
                    return;
                }
            }
        }

        add(incoming);
    }

    /**
     * Elimina tots els efectes i tots els índexs.
     */
    public void clearEffects() {
        effects.clear();
        effectsByKey.clear();
        effectsByType.clear();
        for (List<Effect> indexed : phaseEffects.values()) {
            indexed.clear();
        }
    }

    /**
     * Executa només efectes indexats per final de torn de menú.
     */
    public void onMenuTurnEnd(Character owner) {
        try {
            for (MenuTurnEffect effect : effectsOfType(MenuTurnEffect.class)) {
                effect.onMenuTurnEnd(owner);
            }
            cleanupExpiredEffects();
        } finally {
            clearSpecialMenuActionUsedThisTurn.run();
        }
    }

    /**
     * Executa només efectes que preparen estat de ronda.
     */
    public void onCombatRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        Random effectiveRng = rng == null && owner != null ? owner.rng() : rng;
        for (RoundScopedEffect effect : effectsOfType(RoundScopedEffect.class)) {
            effect.onRoundStart(owner, roundNumber, effectiveRng, out);
        }
        cleanupExpiredEffects();
    }

    /**
     * Executa només efectes que netegen estat transitori de ronda.
     */
    public void onCombatRoundEnd(Character owner) {
        for (RoundScopedEffect effect : effectsOfType(RoundScopedEffect.class)) {
            effect.onRoundEnd(owner);
        }
        cleanupExpiredEffects();
    }

    /**
     * Consulta efectes capaços de bloquejar la regeneració passiva de vida.
     */
    public boolean suppressesPassiveHealthRegen(Character owner) {
        for (EndRoundRecoveryEffect effect : effectsOfType(EndRoundRecoveryEffect.class)) {
            if (effect.suppressPassiveHealthRegen(owner)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recull les pistes d'acció indexades sense recórrer efectes aliens.
     */
    public List<String> actionMenuHints(Character owner, int nextRound) {
        if (!containsType(ActionMenuHintEffect.class)) {
            return List.of();
        }

        List<String> hints = new ArrayList<>();
        for (ActionMenuHintEffect effect : effectsOfType(ActionMenuHintEffect.class)) {
            String hint = effect.actionMenuHint(owner, nextRound);
            if (hint != null && !hint.isBlank()) {
                hints.add(hint.trim());
            }
        }
        return List.copyOf(hints);
    }

    /**
     * Dispara els efectes indexats per una fase i retorna els missatges generats.
     */
    public List<CombatMessage> triggerEffects(
            Character owner,
            HitContext ctx,
            HitContext.Phase phase,
            Random rng) {

        if (phase == null || effects.isEmpty()) {
            return List.of();
        }

        CombatMessageBuffer messages = new CombatMessageBuffer();
        triggerEffects(owner, ctx, phase, rng, messages);
        return messages.messages();
    }

    /**
     * Dispara els efectes indexats per una fase sobre un buffer existent.
     */
    public void triggerEffects(
            Character owner,
            HitContext ctx,
            HitContext.Phase phase,
            Random rng,
            CombatMessageBuffer out) {

        if (effects.isEmpty()) {
            return;
        }

        List<Effect> indexed = phaseEffects.get(phase);
        if (indexed != null) {
            for (Effect effect : List.copyOf(indexed)) {
                if (!effect.isActive()) {
                    continue;
                }

                EffectResult result = effect.onPhase(ctx, phase, rng, owner);
                if (result != null && result.message() != null && out != null) {
                    out.add(result.message());
                }
            }
        }

        cleanupExpiredEffects();
    }

    /**
     * Elimina els efectes expirats dels índexs i de la llista principal.
     */
    public void cleanupExpiredEffects() {
        if (effects.isEmpty()) {
            return;
        }

        Iterator<Effect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            if (!effect.isExpired()) {
                continue;
            }

            iterator.remove();
            unindex(effect);
            removeKeyIndex(effect);
        }
    }

    private void add(Effect effect) {
        insertOrdered(effects, effect);
        effectsByKey.put(effect.key(), effect);
        index(effect);
    }

    private void replace(Effect existing, Effect incoming) {
        int index = effects.indexOf(existing);
        if (index < 0) {
            add(incoming);
            return;
        }

        effects.remove(index);
        unindex(existing);
        removeKeyIndex(existing);
        add(incoming);
    }

    private boolean remove(Effect effect) {
        if (!removeIdentity(effects, effect)) {
            return false;
        }

        unindex(effect);
        removeKeyIndex(effect);
        return true;
    }

    private void index(Effect effect) {
        for (HitContext.Phase phase : phasesFor(effect)) {
            insertOrdered(phaseEffects.get(phase), effect);
        }

        for (Class<?> type : indexedTypesFor(effect)) {
            insertOrdered(effectsByType.computeIfAbsent(type, ignored -> new ArrayList<>()), effect);
        }
    }

    private void unindex(Effect effect) {
        for (List<Effect> indexed : phaseEffects.values()) {
            removeIdentity(indexed, effect);
        }

        for (Class<?> type : indexedTypesFor(effect)) {
            List<Effect> indexed = effectsByType.get(type);
            if (indexed == null) {
                continue;
            }
            removeIdentity(indexed, effect);
            if (indexed.isEmpty()) {
                effectsByType.remove(type);
            }
        }
    }

    private void removeKeyIndex(Effect effect) {
        effectsByKey.computeIfPresent(effect.key(), (key, indexed) -> indexed == effect ? null : indexed);
    }

    private boolean removeIdentity(List<Effect> source, Effect effect) {
        return source.removeIf(candidate -> candidate == effect);
    }

    private static Set<HitContext.Phase> phasesFor(Effect effect) {
        return PHASES_BY_TYPE.computeIfAbsent(effect.getClass(), EffectsBudget::detectPhases);
    }

    private static Set<Class<?>> indexedTypesFor(Effect effect) {
        return INDEXED_TYPES_BY_TYPE.computeIfAbsent(effect.getClass(), EffectsBudget::detectIndexedTypes);
    }

    private static Set<Class<?>> detectIndexedTypes(Class<?> effectType) {
        Set<Class<?>> types = new LinkedHashSet<>();
        addTypeHierarchy(effectType, types);
        types.remove(Object.class);
        return Collections.unmodifiableSet(types);
    }

    private static void addTypeHierarchy(Class<?> type, Set<Class<?>> types) {
        if (type == null || !types.add(type)) {
            return;
        }
        for (Class<?> implemented : type.getInterfaces()) {
            addTypeHierarchy(implemented, types);
        }
        addTypeHierarchy(type.getSuperclass(), types);
    }

    private static Set<HitContext.Phase> detectPhases(Class<?> effectType) {
        if (overrides(effectType, "onPhase",
                HitContext.class,
                HitContext.Phase.class,
                Random.class,
                Character.class)) {
            return Collections.unmodifiableSet(EnumSet.allOf(HitContext.Phase.class));
        }

        EnumSet<HitContext.Phase> phases = EnumSet.of(HitContext.Phase.END_TURN);
        indexOverride(phases, effectType, HitContext.Phase.START_TURN, "startTurn");
        indexOverride(phases, effectType, HitContext.Phase.BEFORE_ATTACK, "beforeAttack");
        indexOverride(phases, effectType, HitContext.Phase.ROLL_CRIT, "rollCrit");
        indexOverride(phases, effectType, HitContext.Phase.MODIFY_DAMAGE, "modifyDamage");
        indexOverride(phases, effectType, HitContext.Phase.BEFORE_DEFENSE, "beforeDefense");
        indexOverride(phases, effectType, HitContext.Phase.AFTER_DEFENSE, "afterDefense");
        indexOverride(phases, effectType, HitContext.Phase.AFTER_HIT, "afterHit");
        return Collections.unmodifiableSet(phases);
    }

    private static void indexOverride(
            EnumSet<HitContext.Phase> phases,
            Class<?> effectType,
            HitContext.Phase phase,
            String methodName) {

        if (overrides(effectType, methodName, HitContext.class, Random.class, Character.class)) {
            phases.add(phase);
        }
    }

    private static boolean overrides(Class<?> effectType, String methodName, Class<?>... argumentTypes) {
        try {
            Method method = effectType.getMethod(methodName, argumentTypes);
            return !Objects.equals(method.getDeclaringClass(), Effect.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            // Si una implementació no es pot inspeccionar, mantenim el comportament
            // conservador: serà present a END_TURN però no a fases optimitzades.
            return false;
        }
    }

    private void insertOrdered(List<Effect> target, Effect effect) {
        int index = 0;
        while (index < target.size() && EFFECT_ORDER.compare(target.get(index), effect) <= 0) {
            index++;
        }
        target.add(index, effect);
    }
}
