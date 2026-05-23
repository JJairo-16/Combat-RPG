package rpgcombat.perks.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.perks.synergy.model.MemberAlteration;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Efecte de perk amb regles i alteracions de sinergia aplicades. */
public final class AugmentedPerkEffect implements Effect {
    private final PerkDefinition perk;
    private final List<RuleSet> ruleSets;
    private final boolean hasExplicitOwnerScope;
    private final EffectState state = new EffectState(0, 0, Integer.MAX_VALUE, 0);

    /**
     * Crea l’efecte a partir del perk i les alteracions.
     */
    public AugmentedPerkEffect(PerkDefinition perk, List<MemberAlteration> alterations) {
        this.perk = Objects.requireNonNull(perk);
        this.ruleSets = buildRuleSets(perk, alterations);
        this.hasExplicitOwnerScope = computeHasExplicitOwnerScope(this.ruleSets);
    }

    /** Retorna la clau única de l’efecte. */
    @Override
    public String key() {
        return ConfigurablePerkEffect.keyFor(perk);
    }

    /** Defineix que aquest efecte substitueix l’anterior. */
    @Override
    public StackingRule stackingRule() {
        return StackingRule.REPLACE;
    }

    /** Estat intern de l’efecte. */
    @Override
    public EffectState state() {
        return state;
    }

    /**
     * Executa les regles del perk en una fase concreta del combat.
     */
    @Override
    public EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner) {
        if (!defaultOwnerScopeMatches(ctx, phase, owner)) return EffectResult.none();

        List<String> messages = new ArrayList<>();
        boolean consumedCharge = false;
        boolean changedState = false;

        for (RuleSet ruleSet : ruleSets) {
            if (ruleSet.trigger() != phase) continue;

            PerkContext context = new PerkContext(ctx, phase, rng, owner, state);
            boolean matches = true;

            for (PerkCondition condition : ruleSet.conditions()) {
                if (!condition.matches(context)) {
                    matches = false;
                    break;
                }
            }

            if (!matches) continue;

            for (PerkAction action : ruleSet.actions()) {
                EffectResult result = action.apply(context);
                if (result == null) continue;

                consumedCharge |= result.consumedCharge();
                changedState |= result.changedState();

                if (result.message() != null
                        && result.message().text() != null
                        && !result.message().text().isBlank()) {
                    messages.add(result.message().text());
                }
            }
        }

        if (messages.isEmpty()) {
            return new EffectResult(null, consumedCharge, changedState);
        }

        String text = perk.name() + ": " + String.join(" ", messages);
        CombatMessage styled = CombatMessage.of(perk.family().symbol(), perk.family().color(), text);
        return new EffectResult(styled, consumedCharge, true);
    }

    /**
     * Construeix els conjunts de regles base i de sinergia.
     */
    private static List<RuleSet> buildRuleSets(PerkDefinition perk, List<MemberAlteration> alterations) {
        List<RuleSet> result = new ArrayList<>();
        result.add(toRuleSet(perk.trigger(), perk.conditions(), perk.actions()));

        if (alterations != null) {
            for (MemberAlteration alteration : alterations) {
                if (alteration != null) {
                    result.add(toRuleSet(alteration.trigger(), alteration.conditions(), alteration.actions()));
                }
            }
        }

        return List.copyOf(result);
    }

    /**
     * Converteix regles crues en condicions i accions executables.
     */
    private static RuleSet toRuleSet(Phase trigger, List<Rule> conditions, List<Rule> actions) {
        List<Rule> rawConditions = safeRules(conditions);

        return new RuleSet(
                trigger,
                rawConditions.stream().map(PerkRuleFactory::condition).toList(),
                safeRules(actions).stream().map(PerkRuleFactory::action).toList(),
                rawConditions);
    }

    /**
     * Comprova si el propietari coincideix amb l’àmbit per defecte.
     */
    private boolean defaultOwnerScopeMatches(HitContext ctx, Phase phase, Character owner) {
        if (ctx == null || owner == null || hasExplicitOwnerScope) return true;

        return switch (phase) {
            case BEFORE_ATTACK, ROLL_CRIT, MODIFY_DAMAGE, AFTER_HIT -> owner == ctx.attacker();
            case BEFORE_DEFENSE, AFTER_DEFENSE -> owner == ctx.defender();
            case START_TURN, END_TURN -> true;
        };
    }

    /**
     * Calcula una sola vegada si hi ha una condició explícita sobre el rol del propietari.
     */
    private static boolean computeHasExplicitOwnerScope(List<RuleSet> ruleSets) {
        return ruleSets.stream()
                .flatMap(ruleSet -> ruleSet.rawConditions().stream())
                .anyMatch(rule -> "OWNER_IS_ATTACKER".equals(rule.type())
                        || "OWNER_IS_DEFENDER".equals(rule.type()));
    }

    /** Filtra regles nul·les. */
    private static List<Rule> safeRules(List<Rule> rules) {
        return rules == null ? List.of() : rules.stream().filter(Objects::nonNull).toList();
    }

    /** Conjunt de regles agrupades per fase. */
    private record RuleSet(
            Phase trigger,
            List<PerkCondition> conditions,
            List<PerkAction> actions,
            List<Rule> rawConditions) {
    }
}
