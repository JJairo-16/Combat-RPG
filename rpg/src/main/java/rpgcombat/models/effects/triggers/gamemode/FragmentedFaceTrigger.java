package rpgcombat.models.effects.triggers.gamemode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryRuntime;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.weapons.passives.HitContext;

/** Trigger de mode que assigna un reflex positiu i un de negatiu a cada ronda. */
public final class FragmentedFaceTrigger extends Trigger implements RoundScopedEffect {
    public static final String INTERNAL_EFFECT_KEY = "FRAGMENTED_FACE";
    private static final String BLIND_KEY = "BLIND";

    private static final double OUTGOING_UP = 1.16;
    private static final double OUTGOING_DOWN = 0.86;
    private static final double INCOMING_DOWN = 0.88;
    private static final double INCOMING_UP = 1.14;
    private static final double GUARD_STRONG = 0.78;
    private static final double GUARD_WEAK = 1.20;
    private static final double DODGE_STRONG = 0.76;
    private static final double DODGE_WEAK = 1.22;
    private static final double CRIT_TWIST_CHANCE = 0.24;
    private static final double BLOOD_MULTIPLIER_UP = 1.18;
    private static final double BLOOD_MULTIPLIER_DOWN = 0.84;
    private static final double ROUND_RESOURCE_RATIO = 0.10;
    private static final double ROUND_HEALTH_RATIO = 0.035;
    private static final double THORN_RATIO = 0.12;
    private static final double THORN_MAX_HEALTH_RATIO = 0.04;
    private static final double MANA_MIRROR_RATIO = 0.08;
    private static final double RECOVERY_HEALTH_RATIO = 0.025;
    private static final double RECOVERY_MANA_RATIO = 0.06;

    private Buff activeBuff;
    private Debuff activeDebuff;
    private int activeRound;
    private boolean startMessageShown;
    private RoundSnapshot roundSnapshot;

    public FragmentedFaceTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        beginRound(owner, roundNumber, rng == null ? owner.rng() : rng);
    }

    @Override
    public void onRoundEnd(Character owner) {
        revertRound(owner);
        clearRound();
    }

    private void beginRound(Character owner, int round, Random rng) {
        clearRound();
        roundSnapshot = RoundSnapshot.capture(owner);
        activeRound = round;
        activeBuff = Buff.values()[rng.nextInt(Buff.values().length)];
        activeDebuff = rollDebuff(rng, activeBuff);

        DiscoveryRuntime.discover(DiscoveryCategory.FRAGMENT_RESULTS, activeBuff.id());
        DiscoveryRuntime.discover(DiscoveryCategory.FRAGMENT_RESULTS, activeDebuff.id());

        applyImmediateBuff(owner, activeBuff);
        applyImmediateDebuff(owner, activeDebuff);
    }

    @Override
    public EffectResult startTurn(HitContext ctx, Random rng, Character owner) {
        if (!active() || ctx == null || owner == null || owner != ctx.attacker()) {
            return EffectResult.none();
        }
        if (startMessageShown) {
            return EffectResult.none();
        }
        startMessageShown = true;
        return EffectResult.msg(CombatMessage.info(
                "+ " + activeBuff.title() + ": " + activeBuff.description()
                        + "\n- " + activeDebuff.title() + ": " + activeDebuff.description()));
    }

    private Debuff rollDebuff(Random rng, Buff buff) {
        Debuff[] values = Debuff.values();
        Debuff selected = values[rng.nextInt(values.length)];
        if (values.length <= 1) {
            return selected;
        }
        while (selected.ordinal() == buff.ordinal()) {
            selected = values[rng.nextInt(values.length)];
        }
        return selected;
    }

    private void clearRound() {
        activeBuff = null;
        activeDebuff = null;
        activeRound = 0;
        startMessageShown = false;
        roundSnapshot = null;
    }

    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!active() || ctx == null || owner != ctx.attacker()) {
            return EffectResult.none();
        }
        if (hasBuff(Buff.TRUE_ANGLE) && rng.nextDouble() < CRIT_TWIST_CHANCE) {
            ctx.forceCritical();
            ctx.putMeta("fragmentBuff", activeBuff.id());
            return result(MessageSymbol.POSITIVE, owner.getName() + " troba un angle net dins la fractura.");
        }
        if (hasDebuff(Debuff.BROKEN_ANGLE) && rng.nextDouble() < CRIT_TWIST_CHANCE) {
            ctx.forbidCritical();
            ctx.putMeta("fragmentDebuff", activeDebuff.id());
            return result(MessageSymbol.NEGATIVE, "L'angle trencat apaga el crític de " + owner.getName() + ".");
        }
        return EffectResult.none();
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (!active() || ctx == null || owner == null) {
            return EffectResult.none();
        }

        List<String> messages = new ArrayList<>();

        if (owner == ctx.attacker()) {
            multiply(ctx, messages, hasBuff(Buff.EDGE_OF_GLASS), OUTGOING_UP,
                    "La vora de vidre afila el cop de " + owner.getName() + ".");
            multiply(ctx, messages, hasDebuff(Debuff.DULLED_EDGE), OUTGOING_DOWN,
                    "La vora esmussada roba força al cop de " + owner.getName() + ".");
            multiply(ctx, messages, hasBuff(Buff.BLOOD_CONDUCTOR) && ctx.defender().isBleeding(), BLOOD_MULTIPLIER_UP,
                    "La sang oberta condueix millor el dany.");
            multiply(ctx, messages, hasDebuff(Debuff.BLOOD_DRAG) && owner.isBleeding(), BLOOD_MULTIPLIER_DOWN,
                    "La pròpia sang pesa sobre l'atac.");
            multiply(ctx, messages, hasBuff(Buff.PATIENT_HAND) && ctx.defenderAction() != Action.ATTACK, 1.12,
                    owner.getName() + " espera el gest rival i colpeja més net.");
            multiply(ctx, messages, hasDebuff(Debuff.TREMBLING_HAND) && ctx.defenderAction() != Action.ATTACK, 0.88,
                    owner.getName() + " dubta quan el rival no entra al xoc.");
        }

        if (owner == ctx.defender()) {
            multiply(ctx, messages, hasBuff(Buff.SECOND_SKIN), INCOMING_DOWN,
                    "La segona pell redueix el dany entrant.");
            multiply(ctx, messages, hasDebuff(Debuff.OPEN_SKIN), INCOMING_UP,
                    "La pell oberta amplifica el dany entrant.");
            multiply(ctx, messages, hasBuff(Buff.STILL_GUARD) && ctx.defenderAction() == Action.DEFEND, GUARD_STRONG,
                    owner.getName() + " arrela la guàrdia.");
            multiply(ctx, messages, hasDebuff(Debuff.CRACKED_GUARD) && ctx.defenderAction() == Action.DEFEND, GUARD_WEAK,
                    "La guàrdia esquerdada deixa passar més impacte.");
            multiply(ctx, messages, hasBuff(Buff.LIGHT_STEP) && ctx.defenderAction() == Action.DODGE, DODGE_STRONG,
                    owner.getName() + " esquiva amb un pas lleuger.");
            multiply(ctx, messages, hasDebuff(Debuff.HEAVY_STEP) && ctx.defenderAction() == Action.DODGE, DODGE_WEAK,
                    "El pas feixuc torna l'esquiva menys neta.");
        }

        if (messages.isEmpty()) {
            return EffectResult.none();
        }
        return result(MessageSymbol.INFO, String.join(" ", messages));
    }

    @Override
    public EffectResult afterDefense(HitContext ctx, Random rng, Character owner) {
        if (!active() || ctx == null || owner == null || owner != ctx.defender() || ctx.damageDealt() <= 0) {
            return EffectResult.none();
        }

        List<String> messages = new ArrayList<>();
        if (hasBuff(Buff.THORN_MEMORY)) {
            double reflected = Math.min(
                    ctx.damageDealt() * THORN_RATIO,
                    owner.getStatistics().getMaxHealth() * THORN_MAX_HEALTH_RATIO);
            if (reflected > 0) {
                reflected = nonLethalDamage(ctx.attacker(), reflected);
                if (reflected > 0) {
                    messages.add("La memòria d'espines retorna " + round2(reflected) + " de dany.");
                }
            }
        }
        if (hasDebuff(Debuff.NUMB_MEMORY)) {
            double extra = Math.min(
                    ctx.damageDealt() * THORN_RATIO,
                    owner.getStatistics().getMaxHealth() * THORN_MAX_HEALTH_RATIO);
            if (extra > 0) {
                owner.getDamage(extra);
                ctx.setDamageDealt(ctx.damageDealt() + extra);
                messages.add("La memòria adormida converteix el cop en " + round2(extra) + " de dany extra.");
            }
        }
        if (hasBuff(Buff.MIRRORED_SKIN)) {
            double restored = owner.getStatistics().restoreMana(owner.getStatistics().getMaxMana() * MANA_MIRROR_RATIO);
            if (restored > 0) {
                messages.add(owner.getName() + " recupera " + round2(restored) + " de manà reflectit.");
            }
        }
        if (hasDebuff(Debuff.HOLLOW_SKIN)) {
            double drained = consumeMana(owner, owner.getStatistics().getMaxMana() * MANA_MIRROR_RATIO);
            if (drained > 0) {
                messages.add(owner.getName() + " perd " + round2(drained) + " de manà pel buit de la pell.");
            }
        }

        if (messages.isEmpty()) {
            return EffectResult.none();
        }
        return result(MessageSymbol.INFO, String.join(" ", messages));
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        if (!active() || ctx == null || owner == null || owner != ctx.attacker()) {
            return EffectResult.none();
        }
        if (ctx.attackerAction() != Action.DEFEND && ctx.attackerAction() != Action.CHARGE) {
            return EffectResult.none();
        }

        List<String> messages = new ArrayList<>();
        if (hasBuff(Buff.FOCUSED_RECOVERY)) {
            double healed = owner.getStatistics().heal(owner.getStatistics().getMaxHealth() * RECOVERY_HEALTH_RATIO);
            double mana = owner.getStatistics().restoreMana(owner.getStatistics().getMaxMana() * RECOVERY_MANA_RATIO);
            if (healed > 0 || mana > 0) {
                messages.add(owner.getName() + " aprofita la pausa i recupera forces.");
            }
        }
        if (hasDebuff(Debuff.BROKEN_RECOVERY)) {
            double damage = owner.getStatistics().getMaxHealth() * RECOVERY_HEALTH_RATIO;
            double mana = consumeMana(owner, owner.getStatistics().getMaxMana() * RECOVERY_MANA_RATIO);
            nonLethalDamage(owner, damage);
            messages.add(owner.getName() + " no troba descans dins la pausa"
                    + (mana > 0 ? " i perd manà." : "."));
        }

        if (messages.isEmpty()) {
            return EffectResult.none();
        }
        return result(MessageSymbol.INFO, String.join(" ", messages));
    }

    private void applyImmediateBuff(Character owner, Buff buff) {
        Statistics stats = owner.getStatistics();
        RoundSnapshot snapshot = roundSnapshot;
        switch (buff) {
            case DEEP_BREATH -> snapshot.restoredStamina = stats.restoreStamina(stats.getMaxStamina() * ROUND_RESOURCE_RATIO);
            case CLEAR_VEIN -> snapshot.restoredMana = stats.restoreMana(stats.getMaxMana() * ROUND_RESOURCE_RATIO);
            case WARM_PULSE -> snapshot.restoredHealth = stats.heal(stats.getMaxHealth() * ROUND_HEALTH_RATIO);
            case SUTURED_MARK -> {
                if (owner.isBleeding()) {
                    snapshot.clearedBleed = true;
                    owner.clearBleed();
                }
            }
            case STEADY_AXIS -> {
                if (owner.isStaggered()) {
                    snapshot.clearedStagger = true;
                    owner.clearStagger();
                }
            }
            case CLEAR_GAZE -> {
                if (owner.hasEffect(BLIND_KEY)) {
                    snapshot.clearedBlind = true;
                    owner.removeEffect(BLIND_KEY);
                }
            }
            case GATHERED_RHYTHM -> {
                snapshot.addedMomentum = true;
                owner.gainMomentum();
            }
            case HELD_CHARGE -> {
                snapshot.addedCharge = !owner.hasChargedAttack();
                owner.prepareChargedAttack();
            }
            default -> {
            }
        }
    }

    private void applyImmediateDebuff(Character owner, Debuff debuff) {
        Statistics stats = owner.getStatistics();
        RoundSnapshot snapshot = roundSnapshot;
        switch (debuff) {
            case SHALLOW_BREATH -> snapshot.consumedStamina = stats.consumeStamina(stats.getMaxStamina() * ROUND_RESOURCE_RATIO);
            case THIRSTING_VEIN -> snapshot.consumedMana = consumeMana(owner, stats.getMaxMana() * ROUND_RESOURCE_RATIO);
            case COLD_PULSE -> snapshot.consumedHealth = nonLethalDamage(owner, stats.getMaxHealth() * ROUND_HEALTH_RATIO);
            case OPEN_MARK -> {
                snapshot.addedBleed = !owner.isBleeding();
                owner.applyBleed(1);
            }
            case CROOKED_AXIS -> {
                snapshot.addedStagger = !owner.isStaggered();
                owner.applyStagger(1);
            }
            case VEILED_GAZE -> {
                BlindEffect blind = new BlindEffect(1, 0.18);
                snapshot.addedBlind = !owner.hasEffect(BLIND_KEY);
                snapshot.fragmentBlind = blind;
                owner.addEffect(blind);
            }
            case LOST_RHYTHM -> {
                snapshot.removedMomentum = owner.getMomentumStacks() > 0;
                owner.loseMomentum();
            }
            case SCATTERED_CHARGE -> {
                if (owner.hasChargedAttack()) {
                    snapshot.removedCharge = true;
                    owner.consumeChargedAttack();
                }
            }
            default -> {
            }
        }
    }

    private void revertRound(Character owner) {
        RoundSnapshot snapshot = roundSnapshot;
        if (owner == null || snapshot == null) {
            return;
        }

        Statistics stats = owner.getStatistics();
        removeTemporaryGain(stats.getStamina(), snapshot.stamina, snapshot.restoredStamina, stats::consumeStamina);
        removeTemporaryGain(stats.getMana(), snapshot.mana, snapshot.restoredMana, FragmentedFaceTrigger.manaConsumer(stats));
        removeTemporaryHealthGain(owner, snapshot);

        restoreTemporaryLoss(stats.getStamina(), snapshot.stamina, snapshot.consumedStamina, stats::restoreStamina);
        restoreTemporaryLoss(stats.getMana(), snapshot.mana, snapshot.consumedMana, stats::restoreMana);
        restoreTemporaryHealthLoss(owner, snapshot);

        if (snapshot.clearedBleed && !owner.isBleeding()) {
            owner.applyBleed(snapshot.bleedTurns);
        }
        if (snapshot.addedBleed && owner.bleedTurnsRemaining() <= 1) {
            owner.clearBleed();
        }
        if (snapshot.clearedStagger && !owner.isStaggered()) {
            owner.applyStagger(snapshot.staggerTurns);
        }
        if (snapshot.addedStagger && owner.staggerTurnsRemaining() <= 1) {
            owner.clearStagger();
        }
        if (snapshot.clearedBlind && !owner.hasEffect(BLIND_KEY) && snapshot.blindEffect != null) {
            owner.addInternalEffect(snapshot.blindEffect);
        }
        if (snapshot.addedBlind && owner.getEffect(BLIND_KEY) == snapshot.fragmentBlind) {
            owner.removeEffect(BLIND_KEY);
        }
        if (snapshot.addedMomentum && owner.getMomentumStacks() > snapshot.momentumStacks) {
            owner.loseMomentum();
        }
        if (snapshot.removedMomentum && owner.getMomentumStacks() < snapshot.momentumStacks) {
            owner.gainMomentum();
        }
        if (snapshot.addedCharge && !snapshot.chargedAttack && owner.hasChargedAttack()) {
            owner.clearChargedAttack();
        }
        if (snapshot.removedCharge && snapshot.chargedAttack && !owner.hasChargedAttack()) {
            owner.prepareChargedAttack();
        }
    }

    private boolean active() {
        return activeRound > 0 && activeBuff != null && activeDebuff != null;
    }

    private boolean hasBuff(Buff buff) {
        return activeBuff == buff;
    }

    private boolean hasDebuff(Debuff debuff) {
        return activeDebuff == debuff;
    }

    private void multiply(HitContext ctx, List<String> messages, boolean condition, double multiplier, String message) {
        if (!condition) {
            return;
        }
        ctx.multiplyDamage(multiplier);
        messages.add(message);
    }

    private static double consumeMana(Character owner, double amount) {
        if (owner == null || amount <= 0) {
            return 0.0;
        }
        Statistics stats = owner.getStatistics();
        double spent = Math.min(stats.getMana(), amount);
        if (spent <= 0) {
            return 0.0;
        }
        stats.consumeMana(spent);
        return spent;
    }

    private static double nonLethalDamage(Character owner, double amount) {
        if (owner == null || amount <= 0) {
            return 0.0;
        }
        double health = owner.getStatistics().getHealth();
        double applied = Math.min(amount, Math.max(0.0, health - 1.0));
        if (applied > 0) {
            owner.getStatistics().damage(applied);
        }
        return applied;
    }

    private static void removeTemporaryGain(double current, double baseline, double amount, AmountConsumer consumer) {
        if (amount <= 0 || current <= baseline) {
            return;
        }
        consumer.accept(Math.min(amount, current - baseline));
    }

    private static void restoreTemporaryLoss(double current, double baseline, double amount, AmountConsumer restorer) {
        if (amount <= 0 || current >= baseline) {
            return;
        }
        restorer.accept(Math.min(amount, baseline - current));
    }

    private static void removeTemporaryHealthGain(Character owner, RoundSnapshot snapshot) {
        double health = owner.getStatistics().getHealth();
        if (snapshot.restoredHealth <= 0 || health <= snapshot.health) {
            return;
        }
        nonLethalDamage(owner, Math.min(snapshot.restoredHealth, health - snapshot.health));
    }

    private static void restoreTemporaryHealthLoss(Character owner, RoundSnapshot snapshot) {
        double health = owner.getStatistics().getHealth();
        if (snapshot.consumedHealth <= 0 || health >= snapshot.health || !owner.isAlive()) {
            return;
        }
        owner.getStatistics().heal(Math.min(snapshot.consumedHealth, snapshot.health - health));
    }

    private static AmountConsumer manaConsumer(Statistics stats) {
        return stats::consumeMana;
    }

    private static EffectResult result(MessageSymbol symbol, String text) {
        return EffectResult.msg(CombatMessage.of(symbol, MessageColor.MAGENTA, text));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @FunctionalInterface
    private interface AmountConsumer {
        void accept(double amount);
    }

    private static final class RoundSnapshot {
        private final double health;
        private final double mana;
        private final double stamina;
        private final int bleedTurns;
        private final int staggerTurns;
        private final Effect blindEffect;
        private final boolean chargedAttack;
        private final int momentumStacks;

        private double restoredHealth;
        private double restoredMana;
        private double restoredStamina;
        private double consumedHealth;
        private double consumedMana;
        private double consumedStamina;
        private boolean clearedBleed;
        private boolean addedBleed;
        private boolean clearedStagger;
        private boolean addedStagger;
        private boolean clearedBlind;
        private boolean addedBlind;
        private Effect fragmentBlind;
        private boolean addedMomentum;
        private boolean removedMomentum;
        private boolean addedCharge;
        private boolean removedCharge;

        private RoundSnapshot(Character owner) {
            Statistics stats = owner.getStatistics();
            health = stats.getHealth();
            mana = stats.getMana();
            stamina = stats.getStamina();
            bleedTurns = owner.bleedTurnsRemaining();
            staggerTurns = owner.staggerTurnsRemaining();
            blindEffect = owner.getEffect(BLIND_KEY);
            chargedAttack = owner.hasChargedAttack();
            momentumStacks = owner.getMomentumStacks();
        }

        private static RoundSnapshot capture(Character owner) {
            return new RoundSnapshot(owner);
        }
    }

    /** Resultats positius possibles. */
    public enum Buff {
        EDGE_OF_GLASS("Vora de vidre", "El cop troba una vora més afilada durant aquesta ronda."),
        SECOND_SKIN("Segona pell", "El cos recorda una capa defensiva que redueix el dany entrant."),
        STILL_GUARD("Guàrdia immòbil", "La defensa arrela millor quan el combatent decideix bloquejar."),
        LIGHT_STEP("Pas lleuger", "L'esquiva deixa menys superfície al cop entrant."),
        TRUE_ANGLE("Angle veritable", "Una possibilitat de crític s'obre quan el cop busca la seva línia."),
        DEEP_BREATH("Alè profund", "La ronda comença amb una reserva extra d'estamina."),
        CLEAR_VEIN("Vena clara", "La ronda comença amb una recuperació de manà."),
        WARM_PULSE("Pols càlid", "La vida torna una mica abans que el xoc es resolgui."),
        SUTURED_MARK("Marca suturada", "La ferida oberta pot tancar-se abans d'actuar."),
        STEADY_AXIS("Eix estable", "El desequilibri desapareix abans que la ronda giri."),
        CLEAR_GAZE("Mirada clara", "La ceguesa menor es dissipa abans del torn."),
        GATHERED_RHYTHM("Ritme reunit", "El combatent recupera impuls al començar la ronda."),
        HELD_CHARGE("Càrrega sostinguda", "La fractura conserva una càrrega preparada."),
        THORN_MEMORY("Memòria d'espines", "Part d'un impacte rebut torna cap a qui l'ha causat."),
        MIRRORED_SKIN("Pell mirall", "Rebre dany pot retornar una mica de manà."),
        BLOOD_CONDUCTOR("Conductor de sang", "Les ferides obertes del rival guien millor el cop."),
        PATIENT_HAND("Mà pacient", "La mà colpeja millor quan el rival evita el xoc directe."),
        FOCUSED_RECOVERY("Recuperació enfocada", "Les accions de pausa poden recuperar vida i manà.");

        private final String title;
        private final String description;

        Buff(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String id() {
            return "BUFF_" + name();
        }

        public String title() {
            return title;
        }

        public String description() {
            return description;
        }

        public String hint() {
            return description;
        }
    }

    /** Resultats negatius possibles, en el mateix ordre conceptual que els positius. */
    public enum Debuff {
        DULLED_EDGE("Vora esmussada", "El cop perd tall durant aquesta ronda."),
        OPEN_SKIN("Pell oberta", "El cos rep el dany amb menys resistència."),
        CRACKED_GUARD("Guàrdia esquerdada", "Bloquejar deixa passar més impacte del que hauria."),
        HEAVY_STEP("Pas feixuc", "L'esquiva arrossega massa pes per sortir neta."),
        BROKEN_ANGLE("Angle trencat", "Una possibilitat de crític pot quedar tancada."),
        SHALLOW_BREATH("Alè curt", "La ronda comença amb part de l'estamina consumida."),
        THIRSTING_VEIN("Vena assedegada", "La ronda comença amb una pèrdua de manà."),
        COLD_PULSE("Pols fred", "Una mica de vida es perd abans del xoc."),
        OPEN_MARK("Marca oberta", "La sang torna a obrir-se abans d'actuar."),
        CROOKED_AXIS("Eix tort", "El cos entra a la ronda desequilibrat."),
        VEILED_GAZE("Mirada velada", "Una boira breu pot fer fallar el cop."),
        LOST_RHYTHM("Ritme perdut", "L'impuls acumulat es desfà al començar la ronda."),
        SCATTERED_CHARGE("Càrrega dispersa", "Una càrrega preparada pot trencar-se abans d'usar-la."),
        NUMB_MEMORY("Memòria adormida", "Part d'un impacte rebut s'enfonsa més dins el cos."),
        HOLLOW_SKIN("Pell buida", "Rebre dany pot buidar una mica de manà."),
        BLOOD_DRAG("Llast de sang", "La pròpia ferida arrossega el cop cap avall."),
        TREMBLING_HAND("Mà tremolosa", "La mà dubta quan el rival evita el xoc directe."),
        BROKEN_RECOVERY("Recuperació trencada", "Les accions de pausa poden desgastar vida i manà.");

        private final String title;
        private final String description;

        Debuff(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String id() {
            return "DEBUFF_" + name();
        }

        public String title() {
            return title;
        }

        public String description() {
            return description;
        }

        public String hint() {
            return description;
        }
    }
}
