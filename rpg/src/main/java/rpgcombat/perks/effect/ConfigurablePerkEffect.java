package rpgcombat.perks.effect;

import java.util.List;
import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Efecte permanent creat a partir d'una perk configurable. */
public final class ConfigurablePerkEffect implements Effect {
    private final PerkDefinition perk;
    private final EffectState state = new EffectState(0, 0, Integer.MAX_VALUE, 0);
    private final List<PerkCondition> conditions;
    private final List<PerkAction> actions;

    public ConfigurablePerkEffect(PerkDefinition perk) {
        this.perk = perk;
        this.conditions = perk.conditions().stream().map(PerkRuleFactory::condition).toList();
        this.actions = perk.actions().stream().map(PerkRuleFactory::action).toList();
    }

    @Override
    public String key() {
        return "PERK_" + perk.id();
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.IGNORE;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner) {
        if (phase != perk.trigger()) return EffectResult.none();

        PerkContext context = new PerkContext(ctx, phase, rng, owner);
        for (PerkCondition condition : conditions) {
            if (!condition.matches(context)) return EffectResult.none();
        }

        EffectResult last = EffectResult.none();
        for (PerkAction action : actions) {
            EffectResult result = action.apply(context);
            if (result != null && result.message() != null) last = result;
        }
        return last;
    }
}
