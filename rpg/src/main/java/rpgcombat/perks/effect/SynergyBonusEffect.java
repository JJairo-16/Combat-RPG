package rpgcombat.perks.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.perks.PerkFamily;
import rpgcombat.perks.synergy.model.SynergyDefinition;
import rpgcombat.perks.synergy.model.SynergyLevel;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Efecte addicional generat per una sinergia de tipus bonus. */
public final class SynergyBonusEffect implements Effect {
    private static final Pattern TOKENS_PATTERN = Pattern.compile("[|,]");

    private final SynergyDefinition synergy;
    private final SynergyLevel level;
    private final EffectState state = new EffectState(0, 0, Integer.MAX_VALUE, 0);
    private final List<PerkCondition> conditions;
    private final List<PerkAction> actions;

    /**
     * Inicialitza l’efecte segons la sinergia i el nivell.
     */
    public SynergyBonusEffect(SynergyDefinition synergy, SynergyLevel level) {
        this.synergy = Objects.requireNonNull(synergy);
        this.level = Objects.requireNonNull(level);
        this.conditions = level.conditions().stream().map(PerkRuleFactory::condition).toList();
        this.actions = level.actions().stream().map(PerkRuleFactory::action).toList();
    }

    /** Retorna la clau única de la sinergia. */
    @Override
    public String key() {
        return "SYNERGY_" + synergy.id();
    }

    /** Aquest efecte substitueix qualsevol anterior amb la mateixa clau. */
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
     * Executa el bonus de sinergia si es compleixen les condicions.
     */
    @Override
    public EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner) {
        if (phase != level.trigger() || ctx == null || owner == null) return EffectResult.none();

        PerkContext context = new PerkContext(ctx, phase, rng, owner, state);
        for (PerkCondition condition : conditions) {
            if (!condition.matches(context)) return EffectResult.none();
        }

        List<String> messages = new ArrayList<>();
        boolean consumedCharge = false;
        boolean changedState = false;

        for (PerkAction action : actions) {
            EffectResult result = action.apply(context);
            if (result == null) continue;
            consumedCharge |= result.consumedCharge();
            changedState |= result.changedState();
            if (result.message() != null && result.message().text() != null && !result.message().text().isBlank()) {
                messages.add(result.message().text());
            }
        }

        boolean activated = consumedCharge || changedState || !messages.isEmpty() || !actions.isEmpty();
        if (activated) {
            registerSynergyTrigger(ctx);
        }

        if (messages.isEmpty()) {
            return new EffectResult(null, consumedCharge, changedState);
        }

        String text = synergy.name() + ": " + String.join(" ", messages);
        CombatMessage styled = CombatMessage.of(PerkFamily.STRATEGY.symbol(), PerkFamily.STRATEGY.color(), text);
        return new EffectResult(styled, consumedCharge, true);
    }

    /** Registra que el bonus de sinergia s'ha disparat al context flexible. */
    private void registerSynergyTrigger(HitContext ctx) {
        if (ctx == null || synergy == null) return;
        appendToken(ctx, "triggeredSynergyIds", synergy.id());
        appendToken(ctx, "triggeredSynergyNames", synergy.name());
    }

    /** Afegeix un token textual en format estable separat per '|'. */
    private void appendToken(HitContext ctx, String key, String value) {
        if (ctx == null || key == null || value == null || value.isBlank()) return;
        Object previous = ctx.getMeta(key);
        String token = value.trim();
        if (previous == null || String.valueOf(previous).isBlank()) {
            ctx.putMeta(key, token);
            return;
        }
        String text = String.valueOf(previous);
        String[] existingTokens = TOKENS_PATTERN.split(text);
        for (String existing : existingTokens) {
            if (token.equals(existing.trim())) return;
        }
        ctx.putMeta(key, text + "|" + token);
    }

    /**
     * Genera la clau estàndard per una sinergia.
     */
    public static String keyFor(SynergyDefinition synergy) {
        return synergy == null ? "SYNERGY_NULL" : "SYNERGY_" + synergy.id();
    }
}
