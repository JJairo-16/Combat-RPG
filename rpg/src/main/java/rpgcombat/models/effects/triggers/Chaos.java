package rpgcombat.models.effects.triggers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.ChaosConfig;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.Fatigue;
import rpgcombat.weapons.passives.HitContext;

/**
 * Trigger permanent que altera el torn del portador amb un resultat aleatori.
 */
public class Chaos extends Trigger {
    public static final String INTERNAL_EFFECT_KEY = "CHAOS";

    public static final String META_SELF_HIT = "CHAOS_SELF_HIT";
    public static final String META_SELF_HIT_MULTIPLIER = "CHAOS_SELF_HIT_MULTIPLIER";
    public static final String META_SELF_HIT_CAN_KILL = "CHAOS_SELF_HIT_CAN_KILL";

    private static final String BLIND_KEY = "BLIND";

    private Outcome lastOutcome;
    private boolean lastWasSevere;
    private Outcome pendingOutcome;
    private long activationCounter;
    private long lastSeed;

    /**
     * Crea el trigger de Caos.
     */
    public Chaos() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * Aplica Caos a l'inici del torn si el portador el té actiu.
     *
     * @return l'acció final després de possibles canvis
     */
    public static Action applyStartTurn(Character owner, Character opponent, Action selectedAction,
            CombatMessageBuffer out) {
        return applyStartTurn(owner, opponent, selectedAction, out, action -> true);
    }

    public static Action applyStartTurn(Character owner, Character opponent, Action selectedAction,
            CombatMessageBuffer out, Predicate<Action> actionAllowed) {
        if (owner == null || selectedAction == null) {
            return selectedAction;
        }

        Effect effect = owner.getEffect(INTERNAL_EFFECT_KEY);
        if (!(effect instanceof Chaos chaos)) {
            return selectedAction;
        }

        return chaos.beginTurn(owner, opponent, selectedAction, out,
                actionAllowed == null ? action -> true : actionAllowed);
    }

    /**
     * Resol l'activació inicial de Caos.
     */
    private Action beginTurn(Character owner, Character opponent, Action selectedAction, CombatMessageBuffer out,
            Predicate<Action> actionAllowed) {
        ChaosConfig cfg = cfg();
        if (cfg == null || !cfg.enabled()) {
            pendingOutcome = null;
            return selectedAction;
        }

        lastSeed = buildSeed(owner, opponent, selectedAction, cfg);
        Random localRng = new Random(lastSeed);
        Outcome outcome = rollOutcome(localRng, cfg, owner, actionAllowed);

        pendingOutcome = outcome;
        lastOutcome = outcome;
        lastWasSevere = outcome.severe;

        Action finalAction = mutateAction(selectedAction, outcome, localRng, actionAllowed);
        applyImmediateOutcome(owner, outcome, localRng, cfg, out, actionAllowed.test(Action.CHARGE));

        addStartMessage(owner, selectedAction, finalAction, outcome, out);

        return finalAction;
    }

    /**
     * Cancel·la l'atac si Caos provoca un error d'acció.
     */
    @Override
    public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
        if (!isPendingForAttacker(ctx, owner)) {
            return EffectResult.none();
        }

        if (pendingOutcome == Outcome.FAIL_ACTION) {
            ctx.setBaseDamage(0);
            ctx.markEffectFail(INTERNAL_EFFECT_KEY);
            ctx.putMeta("chaosFailAction", true);
            return chaosResult(MessageSymbol.WARNING,
                    "El caos devora l'acció de " + owner.getName() + ".");
        }

        return EffectResult.none();
    }

    /**
     * Força o prohibeix crítics segons el resultat pendent.
     */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!isPendingForAttacker(ctx, owner)) {
            return EffectResult.none();
        }

        return switch (pendingOutcome) {
            case PERFECT_CHAOS -> {
                ctx.forceCritical();
                ctx.putMeta("chaosPerfect", true);
                ctx.putMeta("chaosForceCrit", true);
                yield chaosResult(MessageSymbol.POSITIVE,
                        "El caos esdevé simetria perfecta: crític concedit.");
            }
            case FORCE_CRIT -> {
                ctx.forceCritical();
                ctx.putMeta("chaosForceCrit", true);
                yield chaosResult(MessageSymbol.POSITIVE,
                        "El caos força un cop crític.");
            }
            case FORBID_CRIT -> {
                ctx.forbidCritical();
                ctx.putMeta("chaosForbidCrit", true);
                yield chaosResult(MessageSymbol.NEGATIVE,
                        "El caos apaga qualsevol opció de crític.");
            }
            case CRIT_FLIP -> {
                if (rng.nextDouble() < cfg().crit().flipForceChance()) {
                    ctx.forceCritical();
                    ctx.putMeta("chaosForceCrit", true);
                    yield chaosResult(MessageSymbol.POSITIVE,
                            "La moneda caòtica cau de cara: crític forçat.");
                }

                ctx.forbidCritical();
                ctx.putMeta("chaosForbidCrit", true);
                yield chaosResult(MessageSymbol.NEGATIVE,
                        "La moneda caòtica cau de creu: crític prohibit.");
            }
            default -> EffectResult.none();
        };
    }

    /**
     * Modifica el dany o marca l'autoimpacte segons el resultat pendent.
     */
    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (!isPendingForAttacker(ctx, owner)) {
            return EffectResult.none();
        }

        ChaosConfig cfg = cfg();

        return switch (pendingOutcome) {
            case PERFECT_CHAOS -> {
                ctx.multiplyDamage(cfg.damage().upMultiplier());
                ctx.putMeta("chaosPerfect", true);
                ctx.putMeta("chaosDamageMultiplier", cfg.damage().upMultiplier());
                yield chaosResult(MessageSymbol.POSITIVE,
                        "La ruptura perfecta amplifica el dany.");
            }
            case DAMAGE_UP -> {
                ctx.multiplyDamage(cfg.damage().upMultiplier());
                ctx.putMeta("chaosDamageMultiplier", cfg.damage().upMultiplier());
                ctx.putMeta("chaosDamageUp", true);
                yield chaosResult(MessageSymbol.POSITIVE,
                        "El caos potencia el cop.");
            }
            case DAMAGE_DOWN -> {
                ctx.multiplyDamage(cfg.damage().downMultiplier());
                ctx.putMeta("chaosDamageMultiplier", cfg.damage().downMultiplier());
                ctx.putMeta("chaosDamageDown", true);
                yield chaosResult(MessageSymbol.NEGATIVE,
                        "El caos distorsiona el cop i en redueix la força.");
            }
            case OVERLOAD -> {
                ctx.multiplyDamage(cfg.damage().overloadMultiplier());
                ctx.putMeta("chaosDamageMultiplier", cfg.damage().overloadMultiplier());
                ctx.putMeta("chaosOverload", true);
                owner.applyVulnerable(cfg.status().vulnerableTurns());
                owner.multiplyNextIncomingDamage(cfg.damage().overloadIncomingMultiplier());

                yield chaosResult(MessageSymbol.WARNING,
                        "Sobrecàrrega caòtica: més dany, però "
                                + owner.getName() + " queda exposat.");
            }
            case UNSTABLE_GUARD -> {
                if (ctx.attackerAction() == Action.ATTACK) {
                    ctx.multiplyDamage(cfg.damage().downMultiplier());
                    ctx.putMeta("chaosUnstableGuard", true);
                    ctx.putMeta("chaosDamageMultiplier", cfg.damage().downMultiplier());
                    yield chaosResult(MessageSymbol.NEGATIVE,
                            "La guàrdia inestable fa tremolar l'atac.");
                }

                yield EffectResult.none();
            }
            case SELF_HIT -> {
                ctx.putMeta(META_SELF_HIT, true);
                ctx.putMeta(META_SELF_HIT_MULTIPLIER, cfg.damage().selfHitMultiplier());
                ctx.putMeta(META_SELF_HIT_CAN_KILL, cfg.damage().selfHitCanKill());
                ctx.putMeta("chaosSelfHit", true);

                yield chaosResult(MessageSymbol.WARNING,
                        "El caos gira el cop contra el seu origen.");
            }
            default -> EffectResult.none();
        };
    }

    /**
     * Neteja el resultat pendent en acabar el torn de l'atacant.
     */
    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        if (ctx != null && owner == ctx.attacker()) {
            pendingOutcome = null;
        }
        return EffectResult.none();
    }

    /**
     * Comprova si Caos s'ha d'aplicar a l'atacant actual.
     */
    private boolean isPendingForAttacker(HitContext ctx, Character owner) {
        return pendingOutcome != null && ctx != null && owner != null && owner == ctx.attacker();
    }

    /**
     * Tria un resultat amb regles antifrustració.
     */
    private Outcome rollOutcome(Random rng, ChaosConfig cfg, Character owner, Predicate<Action> actionAllowed) {
        Outcome selected = weightedRoll(rng, cfg.outcomes());
        int maxRerolls = Math.max(0, cfg.antiFrustration().maxRerolls());

        for (int i = 0; i < maxRerolls && shouldReroll(selected, cfg, owner, actionAllowed); i++) {
            selected = weightedRoll(rng, cfg.outcomes());
        }

        return selected;
    }

    /**
     * Indica si el resultat s'ha de tornar a tirar.
     */
    private boolean shouldReroll(Outcome selected, ChaosConfig cfg, Character owner, Predicate<Action> actionAllowed) {
        if (selected == null) {
            return true;
        }
        if (cfg.antiFrustration().noRepeatOutcome() && selected == lastOutcome) {
            return true;
        }
        if (cfg.antiFrustration().noDoubleSevere() && selected.severe && lastWasSevere) {
            return true;
        }
        if (selected == Outcome.MANA_SPIKE && owner.getStatistics().getMana() >= owner.getStatistics().getMaxMana()) {
            return true;
        }

        return selected == Outcome.FREE_CHARGE
                && (owner.hasChargedAttack() || !actionAllowed.test(Action.CHARGE));
    }

    /**
     * Fa una tirada ponderada entre els resultats disponibles.
     */
    private Outcome weightedRoll(Random rng, Map<String, Integer> weights) {
        List<Outcome> outcomes = new ArrayList<>();
        int total = 0;

        for (Outcome outcome : Outcome.values()) {
            int weight = weights == null ? 0 : Math.max(0, weights.getOrDefault(outcome.name(), 0));
            if (weight <= 0) {
                continue;
            }
            outcomes.add(outcome);
            total += weight;
        }

        if (total <= 0 || outcomes.isEmpty()) {
            return Outcome.DAMAGE_DOWN;
        }

        int roll = rng.nextInt(total);
        int cursor = 0;
        for (Outcome outcome : outcomes) {
            cursor += Math.max(0, weights.getOrDefault(outcome.name(), 0));
            if (roll < cursor) {
                return outcome;
            }
        }

        return outcomes.get(outcomes.size() - 1);
    }

    /**
     * Canvia l'acció seleccionada si Caos ho requereix.
     */
    private Action mutateAction(Action selectedAction, Outcome outcome, Random rng, Predicate<Action> actionAllowed) {
        if (outcome != Outcome.ACTION_SWAP) {
            return selectedAction;
        }

        List<Action> candidates = switch (selectedAction) {
            case ATTACK -> List.of(Action.DEFEND, Action.CHARGE);
            case DEFEND -> List.of(Action.ATTACK, Action.DODGE);
            case DODGE -> List.of(Action.ATTACK, Action.CHARGE);
            case CHARGE -> List.of(Action.ATTACK, Action.DEFEND);
        };
        List<Action> allowed = candidates.stream().filter(actionAllowed).toList();
        return allowed.isEmpty() ? selectedAction : allowed.get(rng.nextInt(allowed.size()));
    }

    /**
     * Aplica efectes immediats no lligats a l'atac.
     */
    private void applyImmediateOutcome(Character owner, Outcome outcome, Random rng, ChaosConfig cfg,
            CombatMessageBuffer out, boolean chargeAllowed) {
        switch (outcome) {
            case PERFECT_CHAOS -> {
                owner.gainMomentum();
                if (chargeAllowed) {
                    owner.prepareChargedAttack();
                }
                double restored = owner.getStatistics().restoreMana(
                        owner.getStatistics().getMaxMana() * cfg.mana().spikeRestoreMaxManaRatio());
                if (out != null) {
                    out.styled(MessageColor.MAGENTA, MessageSymbol.POSITIVE,
                            owner.getName() + " rep una simetria impossible del caos"
                                    + (restored > 0 ? " i recupera " + round2(restored) + " de manà." : "."));
                }
            }
            case GAIN_MOMENTUM -> owner.gainMomentum();
            case FREE_CHARGE -> {
                if (chargeAllowed) {
                    owner.prepareChargedAttack();
                }
            }
            case BLOOD_RUSH -> {
                owner.gainMomentum();
                owner.applyBleed(cfg.status().bleedTurns());
            }
            case MANA_SPIKE -> {
                double restored = owner.getStatistics().restoreMana(
                        owner.getStatistics().getMaxMana() * cfg.mana().spikeRestoreMaxManaRatio());
                owner.applyStagger(cfg.status().staggerTurns());
                if (out != null && restored > 0) {
                    out.styled(MessageColor.MAGENTA, MessageSymbol.POSITIVE,
                            owner.getName() + " recupera " + round2(restored) + " de manà caòtic.");
                }
            }
            case RANDOM_DEBUFF -> applyRandomDebuff(owner, rng, cfg);
            case CLEANSE_MINOR -> cleanseMinor(owner, rng);
            case UNSTABLE_GUARD -> owner.increaseGuardStacks();
            default -> {
            }
        }
    }

    /**
     * Aplica un perjudici aleatori.
     */
    private void applyRandomDebuff(Character owner, Random rng, ChaosConfig cfg) {
        int pick = rng.nextInt(5);
        switch (pick) {
            case 0 -> owner.addEffect(new BlindEffect(cfg.status().blindTurns(), cfg.status().blindMissChance()));
            case 1 -> owner.applyBleed(cfg.status().bleedTurns());
            case 2 -> owner.applyStagger(cfg.status().staggerTurns());
            case 3 -> owner.applyVulnerable(cfg.status().vulnerableTurns());
            default -> owner.addEffect(new Fatigue(cfg.status().fatigueTurns()));
        }
    }

    /**
     * Elimina un estat negatiu menor aleatori.
     */
    private void cleanseMinor(Character owner, Random rng) {
        List<Runnable> cleanses = new ArrayList<>();

        if (owner.hasEffect(BLIND_KEY)) {
            cleanses.add(() -> owner.removeEffect(BLIND_KEY));
        }
        if (owner.hasEffect(Fatigue.INTERNAL_EFFECT_KEY)) {
            cleanses.add(() -> owner.removeEffect(Fatigue.INTERNAL_EFFECT_KEY));
        }
        if (owner.isBleeding()) {
            cleanses.add(owner::clearBleed);
        }
        if (owner.isStaggered()) {
            cleanses.add(owner::clearStagger);
        }
        if (owner.isVulnerable()) {
            cleanses.add(owner::clearVulnerable);
        }

        if (!cleanses.isEmpty()) {
            cleanses.get(rng.nextInt(cleanses.size())).run();
        }
    }

    /**
     * Construeix el missatge d'activació de Caos.
     */
    private void addStartMessage(
            Character owner,
            Action selectedAction,
            Action finalAction,
            Outcome outcome,
            CombatMessageBuffer out) {

        if (out == null) {
            return;
        }

        out.styled(MessageColor.MAGENTA, MessageSymbol.CHAOS,
                "Caos s'activa sobre " + owner.getName() + ": " + outcome.label + ".");

        if (selectedAction != finalAction) {
            out.styled(MessageColor.MAGENTA, MessageSymbol.INFO,
                    "L'acció canvia de " + selectedAction.label()
                            + " a " + finalAction.label() + ".");
        }
    }

    /**
     * Genera la llavor usada per resoldre Caos.
     */
    private long buildSeed(Character owner, Character opponent, Action action, ChaosConfig cfg) {
        activationCounter++;
        long seed = cfg.seed().combatSalt();
        seed ^= (System.identityHashCode(owner)) * cfg.seed().ownerSalt();
        seed ^= (System.identityHashCode(opponent)) * 0x9E3779B97F4A7C15L;
        seed ^= activationCounter * cfg.seed().turnSalt();
        seed ^= ((long) action.ordinal()) << 32;
        seed ^= owner.rng().nextLong();
        return mix64(seed);
    }

    /**
     * Barreja bits per millorar la distribució d'una llavor.
     */
    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    /** Retorna el resultat caòtic més recent. */
    public String lastOutcomeName() {
        return lastOutcome == null ? null : lastOutcome.name();
    }

    /** Retorna l'etiqueta visible del resultat caòtic més recent. */
    public String lastOutcomeLabel() {
        return lastOutcome == null ? null : lastOutcome.label;
    }

    /** Indica si l'últim resultat caòtic era sever. */
    public boolean lastOutcomeSevere() {
        return lastOutcome != null && lastOutcome.severe;
    }

    /**
     * Retorna la configuració actual de Caos.
     */
    private static ChaosConfig cfg() {
        return CombatBalanceRegistry.get().chaos();
    }

    /**
     * Arrodoneix un número a dos decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    /**
     * Retorna l'última llavor usada.
     */
    public long lastSeed() {
        return lastSeed;
    }

    /**
     * Dona format a un missatge de Caos.
     */
    private static EffectResult chaosResult(MessageSymbol symbol, String text) {
        return EffectResult.msg(CombatMessage.of(symbol, MessageColor.MAGENTA, text));
    }

    /**
     * Resultat possible d'una activació de Caos.
     */
    private enum Outcome {
        DAMAGE_UP("dany augmentat", false),
        DAMAGE_DOWN("dany reduït", false),
        ACTION_SWAP("acció alterada", true),
        SELF_HIT("autoimpacte", true),
        RANDOM_DEBUFF("debilitament aleatori", false),
        FORCE_CRIT("crític forçat", false),
        FORBID_CRIT("crític prohibit", false),
        CRIT_FLIP("crític inestable", false),
        GAIN_MOMENTUM("impuls sobtat", false),
        FREE_CHARGE("càrrega accidental", false),
        FAIL_ACTION("col·lapse de l'acció", true),
        OVERLOAD("sobrecàrrega", true),
        CLEANSE_MINOR("neteja menor", false),
        BLOOD_RUSH("frenesí de sang", false),
        MANA_SPIKE("pic de manà", false),
        UNSTABLE_GUARD("guàrdia inestable", false),
        PERFECT_CHAOS("caos perfecte", false);

        private final String label;
        private final boolean severe;

        Outcome(String label, boolean severe) {
            this.label = label;
            this.severe = severe;
        }
    }
}
