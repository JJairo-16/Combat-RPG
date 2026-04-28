package rpgcombat.perks.effect;

import java.util.List;
import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Implementació d'un efecte permanent basat en una perk configurable.
 * Avalua condicions i executa accions quan es compleix el trigger.
 */
public final class ConfigurablePerkEffect implements Effect {
    private final PerkDefinition perk;
    private final EffectState state = new EffectState(0, 0, Integer.MAX_VALUE, 0);
    private final List<PerkCondition> conditions;
    private final List<PerkAction> actions;

    /**
     * Crea l'efecte a partir d'una definició de perk.
     *
     * @param perk definició de la perk
     */
    public ConfigurablePerkEffect(PerkDefinition perk) {
        this.perk = perk;
        this.conditions = perk.conditions().stream().map(PerkRuleFactory::condition).toList();
        this.actions = perk.actions().stream().map(PerkRuleFactory::action).toList();
    }

    /**
     * @return clau única de l'efecte
     */
    @Override
    public String key() {
        return "PERK_" + perk.id();
    }

    /**
     * @return regla d'apilament (ignorada)
     */
    @Override
    public StackingRule stackingRule() {
        return StackingRule.IGNORE;
    }

    /**
     * @return estat intern de l'efecte
     */
    @Override
    public EffectState state() {
        return state;
    }

    /**
     * Processa una fase de combat: valida condicions i aplica accions.
     *
     * @param ctx context de l'impacte
     * @param phase fase actual
     * @param rng generador aleatori
     * @param owner personatge propietari
     * @return resultat de l'efecte
     */
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
            if (result != null && result.message() != null) {
                last = withFamilyStyle(result);
            }
        }
        return last;
    }

    /**
     * Aplica l'estil visual de la família de la perk al missatge.
     *
     * @param result resultat original
     * @return resultat amb missatge estilitzat
     */
    private EffectResult withFamilyStyle(EffectResult result) {
        CombatMessage original = result.message();
        CombatMessage styled = CombatMessage.of(perk.family().symbol(), perk.family().color(), original.text());
        return new EffectResult(styled, result.consumedCharge(), result.changedState());
    }
}