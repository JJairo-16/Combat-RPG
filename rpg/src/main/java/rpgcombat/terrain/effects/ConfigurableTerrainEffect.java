package rpgcombat.terrain.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.CombatMessageKind;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.types.EndRoundRecoveryEffect;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.terrain.model.TerrainDefinition;
import rpgcombat.terrain.model.TerrainRule;
import rpgcombat.terrain.model.TerrainRuleSet;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Trigger infinit que executa un terreny sobre cada combatent.
 *
 * <p>Un terreny pot usar el motor genèric de regles o delegar tota la seva
 * lògica a un trigger personalitzat. En tots dos casos els missatges es
 * normalitzen com a missatges de mode de joc.</p>
 */
public final class ConfigurableTerrainEffect extends Trigger implements RoundScopedEffect, EndRoundRecoveryEffect {
    private final TerrainDefinition terrain;
    private final List<ExecutableRuleSet> ruleSets;
    private final TerrainTriggerDelegate delegate;

    /**
     * Crea l'efecte executable del terreny seleccionat.
     *
     * @param terrain definició del terreny
     */
    public ConfigurableTerrainEffect(TerrainDefinition terrain) {
        super(keyFor(terrain));
        this.terrain = terrain;
        this.delegate = compileDelegate(terrain);
        this.ruleSets = compileRuleSets(terrain);
    }

    /**
     * Construeix la clau interna de l'efecte de terreny.
     *
     * @param terrain definició d'origen
     * @return clau estable de l'efecte
     */
    public static String keyFor(TerrainDefinition terrain) {
        return "TERRAIN_" + (terrain == null ? TerrainDefinition.NONE_ID : terrain.id());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner) {
        if (terrain == null || terrain.isNone()) {
            return EffectResult.none();
        }
        if (delegate != null) {
            return normalize(delegate.onPhase(ctx, phase, rng, owner));
        }
        if (ruleSets.isEmpty()) {
            return EffectResult.none();
        }

        TerrainContext context = new TerrainContext(ctx, phase, rng, owner, state);
        List<String> messages = new ArrayList<>();
        boolean consumedCharge = false;
        boolean changedState = false;
        boolean ranAnyAction = false;

        for (ExecutableRuleSet ruleSet : ruleSets) {
            if (phase != ruleSet.trigger() || !defaultOwnerScopeMatches(ctx, phase, owner, ruleSet)) {
                continue;
            }
            if (!matchesAll(ruleSet.conditions(), context)) {
                continue;
            }
            for (TerrainAction action : ruleSet.actions()) {
                ranAnyAction = true;
                EffectResult result = action.apply(context);
                if (result == null) {
                    continue;
                }
                consumedCharge |= result.consumedCharge();
                changedState |= result.changedState();
                if (result.message() != null && !result.message().text().isBlank()) {
                    messages.add(result.message().text());
                }
            }
        }

        if (messages.isEmpty()) {
            return new EffectResult(null, consumedCharge, changedState || ranAnyAction);
        }

        return EffectResult.gamemode(
                MessageColor.CYAN,
                rpgcombat.combat.ui.messages.MessageSymbol.INFO,
                terrain.name() + ": " + String.join(" ", messages));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        if (delegate != null) {
            delegate.onRoundStart(owner, roundNumber, rng, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundEnd(Character owner) {
        if (delegate != null) {
            delegate.onRoundEnd(owner);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean suppressPassiveHealthRegen(Character owner) {
        return delegate != null && delegate.suppressPassiveHealthRegen(owner);
    }

    private EffectResult normalize(EffectResult result) {
        if (result == null || result.message() == null) {
            return result == null ? EffectResult.none() : result;
        }
        CombatMessage message = result.message();
        if (message.kind() == CombatMessageKind.GAMEMODE) {
            return result;
        }
        return new EffectResult(
                CombatMessage.gamemode(message.symbol(), message.color(), message.text()),
                result.consumedCharge(),
                result.changedState());
    }

    private static TerrainTriggerDelegate compileDelegate(TerrainDefinition terrain) {
        if (terrain == null || terrain.trigger() == null) {
            return null;
        }
        return new EffectTerrainTriggerDelegate(TerrainTriggerFactory.create(terrain.trigger()));
    }

    private static List<ExecutableRuleSet> compileRuleSets(TerrainDefinition terrain) {
        if (terrain == null || terrain.rules() == null) {
            return List.of();
        }
        return safeRuleSets(terrain.rules().entries()).stream()
                .map(ExecutableRuleSet::from)
                .toList();
    }

    private static boolean matchesAll(List<TerrainCondition> conditions, TerrainContext context) {
        for (TerrainCondition condition : conditions) {
            if (!condition.matches(context)) {
                return false;
            }
        }
        return true;
    }

    private boolean defaultOwnerScopeMatches(HitContext ctx, Phase phase, Character owner, ExecutableRuleSet ruleSet) {
        if (ctx == null || owner == null || hasExplicitOwnerScope(ruleSet)) {
            return true;
        }
        return switch (phase) {
            case BEFORE_ATTACK, ROLL_CRIT, MODIFY_DAMAGE, AFTER_HIT -> owner == ctx.attacker();
            case BEFORE_DEFENSE, AFTER_DEFENSE -> owner == ctx.defender();
            case START_TURN, END_TURN -> true;
        };
    }

    private boolean hasExplicitOwnerScope(ExecutableRuleSet ruleSet) {
        return ruleSet != null && ruleSet.rawConditions().stream()
                .anyMatch(rule -> "OWNER_IS_ATTACKER".equals(rule.type()) || "OWNER_IS_DEFENDER".equals(rule.type()));
    }

    private static List<TerrainRuleSet> safeRuleSets(List<TerrainRuleSet> ruleSets) {
        return ruleSets == null ? List.of() : ruleSets.stream().filter(Objects::nonNull).toList();
    }

    private static List<TerrainRule> safeRules(List<TerrainRule> rules) {
        return rules == null ? List.of() : rules.stream().filter(Objects::nonNull).toList();
    }

    private record ExecutableRuleSet(
            Phase trigger,
            List<TerrainRule> rawConditions,
            List<TerrainCondition> conditions,
            List<TerrainAction> actions) {

        private static ExecutableRuleSet from(TerrainRuleSet ruleSet) {
            List<TerrainRule> conditions = safeRules(ruleSet.conditions());
            return new ExecutableRuleSet(
                    ruleSet.trigger(),
                    conditions,
                    conditions.stream().map(TerrainRuleFactory::condition).toList(),
                    safeRules(ruleSet.actions()).stream().map(TerrainRuleFactory::action).toList());
        }
    }
}
