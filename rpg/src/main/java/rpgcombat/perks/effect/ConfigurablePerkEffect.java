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
import rpgcombat.perks.PerkDefinition;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Implementació d'un efecte permanent basat en una perk configurable.
 * Avalua condicions i executa accions quan es compleix el trigger.
 */
public final class ConfigurablePerkEffect implements Effect {
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[|,]");

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
        this.conditions = safeRules(perk.conditions()).stream().map(PerkRuleFactory::condition).toList();
        this.actions = safeRules(perk.actions()).stream().map(PerkRuleFactory::action).toList();
    }

    /**
     * @return clau única de l'efecte
     */
    @Override
    public String key() {
        return keyFor(perk);
    }

    /**
     * Retorna la clau que tindrà l'efecte d'una perk sense haver-lo d'instanciar.
     *
     * @param perk definició de perk
     * @return clau estable de l'efecte
     */
    public static String keyFor(PerkDefinition perk) {
        return perk == null ? "PERK_NULL" : "PERK_" + perk.id();
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
        if (phase != perk.trigger() || !defaultOwnerScopeMatches(ctx, phase, owner)) return EffectResult.none();

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
            registerPerkActivation(ctx);
        }

        if (messages.isEmpty()) {
            return new EffectResult(null, consumedCharge, changedState);
        }

        String text = perk.name() + ": " + String.join(" ", messages);
        CombatMessage styled = CombatMessage.of(perk.family().symbol(), perk.family().color(), text);
        return new EffectResult(styled, consumedCharge, true);
    }

    /** Registra l'activació al context flexible perquè els assoliments la puguin observar. */
    private void registerPerkActivation(HitContext ctx) {
        if (ctx == null || perk == null) return;
        appendToken(ctx, "activatedPerkIds", perk.id());
        appendToken(ctx, "activatedPerkNames", perk.name());
        appendToken(ctx, "activatedPerkFamilies", perk.family() == null ? null : perk.family().name());
        for (String tag : perk.tags()) {
            appendToken(ctx, "activatedPerkTags", tag);
        }
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
        String[] tokens = TOKEN_SPLIT_PATTERN.split(text);
        for (String existing : tokens) {
            if (token.equals(existing.trim())) return;
        }
        ctx.putMeta(key, text + "|" + token);
    }

    /**
     * Evita que una perk s'activi pel combatent equivocat quan la seva configuració
     * no declara explícitament el rol del propietari.
     */
    private boolean defaultOwnerScopeMatches(HitContext ctx, Phase phase, Character owner) {
        if (ctx == null || owner == null || hasExplicitOwnerScope()) return true;

        return switch (phase) {
            case BEFORE_ATTACK, ROLL_CRIT, MODIFY_DAMAGE, AFTER_HIT -> owner == ctx.attacker();
            case BEFORE_DEFENSE, AFTER_DEFENSE -> owner == ctx.defender();
            case START_TURN, END_TURN -> true;
        };
    }

    /** @return si la configuració ja conté una condició de rol del propietari. */
    private boolean hasExplicitOwnerScope() {
        return safeRules(perk.conditions()).stream()
                .anyMatch(rule -> "OWNER_IS_ATTACKER".equals(rule.type()) || "OWNER_IS_DEFENDER".equals(rule.type()));
    }

    /** Evita nuls a les llistes de regles configurables. */
    private static List<PerkDefinition.Rule> safeRules(List<PerkDefinition.Rule> rules) {
        return rules == null ? List.of() : rules.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean isExpired() {
        return false;
    }
}
