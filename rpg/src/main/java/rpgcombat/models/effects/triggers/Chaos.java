package rpgcombat.models.effects.triggers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.ChaosConfig;
import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.Fatigue;
import rpgcombat.utils.ui.Ansi;
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
    public static Action applyStartTurn(Character owner, Character opponent, Action selectedAction, List<String> out) {
        if (owner == null || selectedAction == null) {
            return selectedAction;
        }

        Effect effect = owner.getEffect(INTERNAL_EFFECT_KEY);
        if (!(effect instanceof Chaos chaos)) {
            return selectedAction;
        }

        return chaos.beginTurn(owner, opponent, selectedAction, out);
    }

    /**
     * Resol l'activació inicial de Caos.
     */
    private Action beginTurn(Character owner, Character opponent, Action selectedAction, List<String> out) {
        ChaosConfig cfg = cfg();
        if (cfg == null || !cfg.enabled()) {
            pendingOutcome = null;
            return selectedAction;
        }

        lastSeed = buildSeed(owner, opponent, selectedAction, cfg);
        Random localRng = new Random(lastSeed);
        Outcome outcome = rollOutcome(localRng, cfg, owner);

        pendingOutcome = outcome;
        lastOutcome = outcome;
        lastWasSevere = outcome.severe;

        Action finalAction = mutateAction(selectedAction, outcome, localRng);
        applyImmediateOutcome(owner, outcome, localRng, cfg, out);

        if (out != null) {
            out.add(buildStartMessage(owner, selectedAction, finalAction, outcome));
        }

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
            return EffectResult.msg(chaosMsg("!", "El caos devora l'acció de " + owner.getName() + "."));
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
            case FORCE_CRIT -> {
                ctx.forceCritical();
                yield EffectResult.msg(chaosMsg("+", "El caos força un cop crític."));
            }
            case FORBID_CRIT -> {
                ctx.forbidCritical();
                yield EffectResult.msg(chaosMsg("-", "El caos apaga qualsevol opció de crític."));
            }
            case CRIT_FLIP -> {
                if (rng.nextDouble() < cfg().crit().flipForceChance()) {
                    ctx.forceCritical();
                    yield EffectResult.msg(chaosMsg("+", "La moneda caòtica cau de cara: crític forçat."));
                }
                ctx.forbidCritical();
                yield EffectResult.msg(chaosMsg("-", "La moneda caòtica cau de creu: crític prohibit."));
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
            case DAMAGE_UP -> {
                ctx.multiplyDamage(cfg.damage().upMultiplier());
                yield EffectResult.msg(chaosMsg("+", "El caos potencia el cop."));
            }
            case DAMAGE_DOWN -> {
                ctx.multiplyDamage(cfg.damage().downMultiplier());
                yield EffectResult.msg(chaosMsg("-", "El caos distorsiona el cop i en redueix la força."));
            }
            case OVERLOAD -> {
                ctx.multiplyDamage(cfg.damage().overloadMultiplier());
                owner.applyVulnerable(cfg.status().vulnerableTurns());
                owner.multiplyNextIncomingDamage(cfg.damage().overloadIncomingMultiplier());
                yield EffectResult.msg(chaosMsg("!", "Sobrecàrrega caòtica: més dany, però "
                        + owner.getName() + " queda exposat."));
            }
            case UNSTABLE_GUARD -> {
                if (ctx.attackerAction() == Action.ATTACK) {
                    ctx.multiplyDamage(cfg.damage().downMultiplier());
                    yield EffectResult.msg(chaosMsg("-", "La guàrdia inestable fa tremolar l'atac."));
                }
                yield EffectResult.none();
            }
            case SELF_HIT -> {
                ctx.putMeta(META_SELF_HIT, true);
                ctx.putMeta(META_SELF_HIT_MULTIPLIER, cfg.damage().selfHitMultiplier());
                ctx.putMeta(META_SELF_HIT_CAN_KILL, cfg.damage().selfHitCanKill());
                yield EffectResult.msg(chaosMsg("!", "El caos gira el cop contra el seu origen."));
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
    private Outcome rollOutcome(Random rng, ChaosConfig cfg, Character owner) {
        Outcome selected = weightedRoll(rng, cfg.outcomes());
        int maxRerolls = Math.max(0, cfg.antiFrustration().maxRerolls());

        for (int i = 0; i < maxRerolls && shouldReroll(selected, cfg, owner); i++) {
            selected = weightedRoll(rng, cfg.outcomes());
        }

        return selected;
    }

    /**
     * Indica si el resultat s'ha de tornar a tirar.
     */
    private boolean shouldReroll(Outcome selected, ChaosConfig cfg, Character owner) {
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

        return selected == Outcome.FREE_CHARGE && owner.hasChargedAttack();
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
    private Action mutateAction(Action selectedAction, Outcome outcome, Random rng) {
        if (outcome != Outcome.ACTION_SWAP) {
            return selectedAction;
        }

        return switch (selectedAction) {
            case ATTACK -> rng.nextBoolean() ? Action.DEFEND : Action.CHARGE;
            case DEFEND -> rng.nextBoolean() ? Action.ATTACK : Action.DODGE;
            case DODGE -> rng.nextBoolean() ? Action.ATTACK : Action.CHARGE;
            case CHARGE -> rng.nextBoolean() ? Action.ATTACK : Action.DEFEND;
        };
    }

    /**
     * Aplica efectes immediats no lligats a l'atac.
     */
    private void applyImmediateOutcome(Character owner, Outcome outcome, Random rng, ChaosConfig cfg,
            List<String> out) {
        switch (outcome) {
            case GAIN_MOMENTUM -> owner.gainMomentum();
            case FREE_CHARGE -> owner.prepareChargedAttack();
            case BLOOD_RUSH -> {
                owner.gainMomentum();
                owner.applyBleed(cfg.status().bleedTurns());
            }
            case MANA_SPIKE -> {
                double restored = owner.getStatistics().restoreMana(
                        owner.getStatistics().getMaxMana() * cfg.mana().spikeRestoreMaxManaRatio());
                owner.applyStagger(cfg.status().staggerTurns());
                if (out != null && restored > 0) {
                    out.add(chaosMsg("+", owner.getName() + " recupera " + round2(restored) + " de manà caòtic."));
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
    private String buildStartMessage(Character owner, Action selectedAction, Action finalAction, Outcome outcome) {
        String base = chaosMsg("?", "Caos s'activa sobre " + owner.getName() + ": " + outcome.label + ".");

        if (selectedAction != finalAction) {
            return base + " " + Ansi.MAGENTA
                    + "L'acció canvia de " + selectedAction.label()
                    + " a " + finalAction.label() + "."
                    + Ansi.RESET;
        }

        return base;
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
    private static String chaosMsg(String symbol, String text) {
        return Ansi.MAGENTA + symbol + " " + text + Ansi.RESET;
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
        UNSTABLE_GUARD("guàrdia inestable", false);

        private final String label;
        private final boolean severe;

        Outcome(String label, boolean severe) {
            this.label = label;
            this.severe = severe;
        }
    }
}