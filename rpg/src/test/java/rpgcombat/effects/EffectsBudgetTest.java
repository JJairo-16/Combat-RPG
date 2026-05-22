package rpgcombat.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import rpgcombat.TestCombatBalance;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.EffectsBudget;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.types.ActionMenuHintEffect;
import rpgcombat.models.effects.types.EndRoundRecoveryEffect;
import rpgcombat.models.effects.types.MenuTurnEffect;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.weapons.passives.HitContext;

class EffectsBudgetTest {
    @BeforeAll
    static void init() {
        TestCombatBalance.init();
    }

    @Test
    void indexesPhaseOverridesWithoutIncludingUnrelatedEffects() {
        EffectsBudget budget = new EffectsBudget();
        Effect idle = new IdleEffect("idle", 1);
        Effect start = new StartTurnEffect("start", 3);
        Effect before = new BeforeAttackEffect("before", 2);
        Effect delegated = new DelegatedPhaseEffect("delegated", 4);

        budget.addEffect(idle);
        budget.addEffect(start);
        budget.addEffect(before);
        budget.addEffect(delegated);

        assertEquals(List.of(delegated, start), budget.effectsForPhase(HitContext.Phase.START_TURN));
        assertEquals(List.of(delegated, before), budget.effectsForPhase(HitContext.Phase.BEFORE_ATTACK));
        assertFalse(budget.effectsForPhase(HitContext.Phase.BEFORE_ATTACK).contains(idle));
        assertTrue(budget.effectsForPhase(HitContext.Phase.END_TURN).contains(idle));
    }

    @Test
    void keepsKeyAndPhaseIndexesSyncedWhenEffectsReplaceAndExpire() {
        EffectsBudget budget = new EffectsBudget();
        Effect replaced = new StartTurnEffect("shared", 1);
        Effect replacement = new BeforeAttackEffect("shared", 2);
        ExpiringBeforeAttackEffect expiring = new ExpiringBeforeAttackEffect("one-shot", 3);

        budget.addEffect(replaced);
        budget.addEffect(replacement);
        budget.addEffect(expiring);

        assertSame(replacement, budget.getEffect("shared"));
        assertFalse(budget.effectsForPhase(HitContext.Phase.START_TURN).contains(replaced));
        assertTrue(budget.effectsForPhase(HitContext.Phase.BEFORE_ATTACK).contains(replacement));

        budget.triggerEffects(null, null, HitContext.Phase.BEFORE_ATTACK, new Random(0));

        assertEquals(1, expiring.calls);
        assertFalse(budget.hasEffect("one-shot"));
        assertFalse(budget.effectsForPhase(HitContext.Phase.BEFORE_ATTACK).contains(expiring));
    }

    @Test
    void drivesIndexedCapabilityBuckets() {
        AtomicInteger clearedMenuUse = new AtomicInteger();
        EffectsBudget budget = new EffectsBudget(clearedMenuUse::incrementAndGet);
        LifecycleEffect effect = new LifecycleEffect("lifecycle", 5);

        budget.addEffect(effect);

        budget.onMenuTurnEnd(null);
        budget.onCombatRoundStart(null, 7, new Random(0), new CombatMessageBuffer());
        budget.onCombatRoundEnd(null);

        assertEquals(1, effect.menuTurns);
        assertEquals(1, clearedMenuUse.get());
        assertEquals(1, effect.roundStarts);
        assertEquals(1, effect.roundEnds);
        assertTrue(budget.suppressesPassiveHealthRegen(null));
        assertEquals(List.of("Ronda 8"), budget.actionMenuHints(null, 8));
    }

    @Test
    void indexesNewInterfacesAndTriggerLookupsWithoutDedicatedBuckets() {
        EffectsBudget budget = new EffectsBudget();
        NovelRoleEffect roleEffect = new NovelRoleEffect("role", 1);
        TestTrigger trigger = new TestTrigger("trigger");

        budget.addEffect(roleEffect);
        budget.addEffect(trigger);

        assertTrue(budget.containsType(NovelEffectRole.class));
        assertEquals(List.of(roleEffect), budget.effectsOfType(NovelEffectRole.class));
        assertTrue(budget.containsTrigger());
        assertTrue(budget.hasTrigger("trigger"));
        assertSame(trigger, budget.getTrigger("trigger"));
        assertFalse(budget.hasTrigger("role"));
    }

    @Test
    void characterExposesIndexedEffectsByType() {
        Character owner = new Character("Budget", 18, balancedStats(), Breed.HUMAN);
        NovelRoleEffect roleEffect = new NovelRoleEffect("role", 1);
        TestTrigger trigger = new TestTrigger("trigger");

        owner.addInternalEffect(roleEffect);
        owner.addInternalEffect(trigger);

        assertEquals(List.of(roleEffect), owner.effectsOfType(NovelEffectRole.class));
        assertTrue(owner.effectsOfType(Trigger.class).contains(trigger));
    }

    @Test
    void exposesImmutableSnapshotsWithoutLeakingLaterIndexMutations() {
        EffectsBudget budget = new EffectsBudget();
        BeforeAttackEffect beforeAttack = new BeforeAttackEffect("before", 3);
        NovelRoleEffect roleEffect = new NovelRoleEffect("role", 2);

        budget.addEffect(roleEffect);
        budget.addEffect(beforeAttack);

        List<Effect> allEffects = budget.getEffects();
        List<Effect> beforeAttackEffects = budget.effectsForPhase(HitContext.Phase.BEFORE_ATTACK);
        List<NovelEffectRole> roleEffects = budget.effectsOfType(NovelEffectRole.class);

        assertEquals(List.of(beforeAttack, roleEffect), allEffects);
        assertEquals(List.of(beforeAttack), beforeAttackEffects);
        assertEquals(List.of(roleEffect), roleEffects);
        assertThrows(UnsupportedOperationException.class, allEffects::clear);
        assertThrows(UnsupportedOperationException.class, beforeAttackEffects::clear);
        assertThrows(UnsupportedOperationException.class, roleEffects::clear);

        budget.clearEffects();

        assertEquals(List.of(beforeAttack, roleEffect), allEffects);
        assertEquals(List.of(beforeAttack), beforeAttackEffects);
        assertEquals(List.of(roleEffect), roleEffects);
        assertTrue(budget.getEffects().isEmpty());
        assertTrue(budget.effectsForPhase(HitContext.Phase.BEFORE_ATTACK).isEmpty());
        assertFalse(budget.containsType(NovelEffectRole.class));
    }

    @Test
    void phaseDispatchDoesNotRunEffectsAddedByAnotherEffectUntilNextDispatch() {
        EffectsBudget budget = new EffectsBudget();
        CountingBeforeAttackEffect addedDuringDispatch = new CountingBeforeAttackEffect("late", 1);
        MutatingBeforeAttackEffect mutating =
                new MutatingBeforeAttackEffect("mutating", 2, budget, addedDuringDispatch);

        budget.addEffect(mutating);

        budget.triggerEffects(null, null, HitContext.Phase.BEFORE_ATTACK, new Random(0));

        assertEquals(1, mutating.calls);
        assertEquals(0, addedDuringDispatch.calls);
        assertFalse(budget.hasEffect("mutating"));
        assertTrue(budget.hasEffect("late"));

        budget.triggerEffects(null, null, HitContext.Phase.BEFORE_ATTACK, new Random(0));

        assertEquals(1, mutating.calls);
        assertEquals(1, addedDuringDispatch.calls);
    }

    private int[] balancedStats() {
        return new int[] { 20, 20, 20, 20, 20, 20, 20 };
    }

    private static class IdleEffect implements Effect {
        private final String key;
        private final int priority;
        private final EffectState state = EffectState.ofStacks(1);

        IdleEffect(String key, int priority) {
            this.key = key;
            this.priority = priority;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public EffectState state() {
            return state;
        }
    }

    private static final class StartTurnEffect extends IdleEffect {
        StartTurnEffect(String key, int priority) {
            super(key, priority);
        }

        @Override
        public EffectResult startTurn(HitContext ctx, Random rng, Character owner) {
            return EffectResult.none();
        }
    }

    private static class BeforeAttackEffect extends IdleEffect {
        BeforeAttackEffect(String key, int priority) {
            super(key, priority);
        }

        @Override
        public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
            return EffectResult.none();
        }
    }

    private static final class DelegatedPhaseEffect extends IdleEffect {
        DelegatedPhaseEffect(String key, int priority) {
            super(key, priority);
        }

        @Override
        public EffectResult onPhase(HitContext ctx, HitContext.Phase phase, Random rng, Character owner) {
            return EffectResult.none();
        }
    }

    private static final class ExpiringBeforeAttackEffect extends BeforeAttackEffect {
        private final EffectState charges = EffectState.ofCharges(1);
        private int calls;

        ExpiringBeforeAttackEffect(String key, int priority) {
            super(key, priority);
        }

        @Override
        public int maxCharges() {
            return 1;
        }

        @Override
        public EffectState state() {
            return charges;
        }

        @Override
        public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
            calls++;
            charges.consumeCharge();
            return EffectResult.none();
        }
    }

    private static class CountingBeforeAttackEffect extends BeforeAttackEffect {
        int calls;

        CountingBeforeAttackEffect(String key, int priority) {
            super(key, priority);
        }

        @Override
        public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
            calls++;
            return EffectResult.none();
        }
    }

    private static final class MutatingBeforeAttackEffect extends CountingBeforeAttackEffect {
        private final String key;
        private final EffectsBudget budget;
        private final Effect addedDuringDispatch;

        MutatingBeforeAttackEffect(
                String key,
                int priority,
                EffectsBudget budget,
                Effect addedDuringDispatch) {
            super(key, priority);
            this.key = key;
            this.budget = budget;
            this.addedDuringDispatch = addedDuringDispatch;
        }

        @Override
        public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
            EffectResult result = super.beforeAttack(ctx, rng, owner);
            budget.addEffect(addedDuringDispatch);
            budget.removeEffect(key);
            return result;
        }
    }

    private static final class LifecycleEffect extends IdleEffect
            implements MenuTurnEffect, RoundScopedEffect, EndRoundRecoveryEffect, ActionMenuHintEffect {

        private int menuTurns;
        private int roundStarts;
        private int roundEnds;

        LifecycleEffect(String key, int priority) {
            super(key, priority);
        }

        @Override
        public void onMenuTurnEnd(Character owner) {
            menuTurns++;
        }

        @Override
        public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
            roundStarts++;
        }

        @Override
        public void onRoundEnd(Character owner) {
            roundEnds++;
        }

        @Override
        public boolean suppressPassiveHealthRegen(Character owner) {
            return true;
        }

        @Override
        public String actionMenuHint(Character owner, int nextRound) {
            return "Ronda " + nextRound;
        }
    }

    private interface NovelEffectRole {
    }

    private static final class NovelRoleEffect extends IdleEffect implements NovelEffectRole {
        NovelRoleEffect(String key, int priority) {
            super(key, priority);
        }
    }

    private static final class TestTrigger extends Trigger {
        TestTrigger(String key) {
            super(key);
        }
    }
}
