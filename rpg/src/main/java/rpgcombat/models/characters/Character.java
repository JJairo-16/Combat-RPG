package rpgcombat.models.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.impl.Exhaustion;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponType;
import rpgcombat.weapons.passives.HitContext;

/**
 * Representa un personatge amb estadístiques, raça, arma i efectes.
 */
public class Character {

    private static final int TOTAL_POINTS = 140;
    private static final int MIN_STAT = 10;
    private static final int MIN_CONSTITUTION = MIN_STAT + 2;
    private static final double SPIRITUAL_CALLING_THRESHOLD = 0.20;
    private static final int MAX_GUARD_STACKS = 4;
    private static final int GUARD_BREAK_THRESHOLD = 3;
    private static final double DODGE_FAIL_MULTIPLIER = 1.25;
    private static final double CHARGED_ATTACK_MULTIPLIER = 1.50;
    private static final double VULNERABLE_DAMAGE_MULTIPLIER = 1.30;
    private static final double BLEED_MAX_HEALTH_RATIO = 0.025;
    private static final double BLEED_DEFEND_REDUCTION = 0.40;
    private static final double STAGGER_ATTACK_MULTIPLIER = 0.78;
    private static final double STAGGER_DEFEND_MULTIPLIER = 0.78;
    private static final double STAGGER_DODGE_MULTIPLIER = 0.72;
    private static final int MAX_MOMENTUM_STACKS = 3;
    private static final double MOMENTUM_ATTACK_BONUS_PER_STACK = 0.06;
    private static final double MOMENTUM_DODGE_BONUS_PER_STACK = 0.025;

    private static final double DESPERATE_HEALTH_RATIO = 0.30;
    private static final double ADRENALINE_TRIGGER_HEALTH_RATIO = 0.18;
    private static final double ADRENALINE_BASE = 38.0;
    private static final double ADRENALINE_GAP_BONUS = 24.0;
    private static final double ADRENALINE_DESPERATE_BONUS = 8.0;
    private static final double ADRENALINE_MAX = 72.0;
    private static final double UNDERDOG_DAMAGE_MAX_BONUS = 0.12;
    private static final double UNDERDOG_DAMAGE_TAKEN_MAX_REDUCTION = 0.10;
    private static final double UNDERDOG_GAP_REFERENCE = 0.45;
    private static final double MOMENTUM_SUPPRESSION_WHEN_TARGET_LOW = 0.55;

    protected final String name;
    protected final int age;
    protected final Breed breed;

    protected final Statistics stats;
    protected Weapon weapon;

    protected final Random rng = new Random();
    protected final List<Effect> effects = new ArrayList<>();

    private int spiritualCallingCooldown = 0;
    private int guardStacks = 0;
    private boolean chargedAttack = false;

    private int vulnerableTurns = 0;
    private int bleedTurns = 0;
    private int staggerTurns = 0;
    private int momentumStacks = 0;
    private boolean adrenalineSurgeUsed = false;
    private double adrenaline = 0.0;

    private double attackModifierThisTurn = 1.0;
    private double defenseModifierThisTurn = 1.0;
    private double dodgeModifierThisTurn = 1.0;

    public Character(String name, int age, int[] stats, Breed breed) {
        validateName(name);
        validateAge(age);
        validateStats(stats);

        int[] effectiveStats = applyBreed(stats, breed);

        this.name = name;
        this.age = age;
        this.breed = breed;
        this.stats = new Statistics(effectiveStats);
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public Breed getBreed() { return breed; }
    public Statistics getStatistics() { return stats; }
    public Weapon getWeapon() { return weapon; }
    public int getSpiritualCallingCooldown() { return spiritualCallingCooldown; }
    public int getGuardStacks() { return guardStacks; }
    public boolean hasChargedAttack() { return chargedAttack; }
    public boolean isVulnerable() { return vulnerableTurns > 0; }
    public boolean isBleeding() { return bleedTurns > 0; }
    public boolean isStaggered() { return staggerTurns > 0; }
    public int getMomentumStacks() { return momentumStacks; }
    public boolean hasUsedAdrenalineSurge() { return adrenalineSurgeUsed; }
    public double getAdrenaline() { return adrenaline; }

    public void setSpiritualCallingCooldown(int turns) { this.spiritualCallingCooldown = Math.max(0, turns); }
    public void tickSpiritualCallingCooldown() { if (spiritualCallingCooldown > 0) spiritualCallingCooldown--; }

    public boolean hasEffect(String key) {
        if (key == null || key.isBlank()) return false;
        for (Effect effect : effects) {
            if (effect != null && !effect.isExpired() && key.equals(effect.key())) return true;
        }
        return false;
    }

    public Effect getEffect(String key) {
        if (key == null || key.isBlank()) return null;
        for (Effect effect : effects) {
            if (effect != null && !effect.isExpired() && key.equals(effect.key())) return effect;
        }
        return null;
    }

    public boolean removeEffect(String key) {
        if (key == null || key.isBlank() || effects.isEmpty()) return false;
        return effects.removeIf(effect -> effect != null && key.equals(effect.key()));
    }


    public double healthRatio() {
        double maxHealth = stats.getMaxHealth();
        if (maxHealth <= 0) return 0;
        return Math.clamp(stats.getHealth() / maxHealth, 0.0, 1.0);
    }

    public boolean isDesperate() {
        return isAlive() && healthRatio() <= DESPERATE_HEALTH_RATIO;
    }

    public boolean isAtOrBelowHealthRatio(double ratio) {
        double maxHealth = stats.getMaxHealth();
        if (maxHealth <= 0) return false;
        return (stats.getHealth() / maxHealth) <= ratio;
    }

    public boolean canUseSpiritualCalling() {
        return hasEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY)
                && spiritualCallingCooldown <= 0
                && isAtOrBelowHealthRatio(SPIRITUAL_CALLING_THRESHOLD);
    }

    public boolean setWeapon(Weapon w) {
        if (w == null) return false;
        if (!w.canEquip(stats)) return false;
        weapon = w;
        return true;
    }

    public AttackResult attack() {
        if (weapon == null) return attackUnarmed();
        return weapon.attack(stats, rng);
    }

    protected AttackResult attackUnarmed() {
        double damage = WeaponType.PHYSICAL.getBasicDamage(7, stats) * damageVariance(rng);
        return new AttackResult(damage, "ataca amb les mans desnudes.");
    }

    private static final double DAMAGE_VARIANCE = 0.07;
    private static final double DOWN_VARIANCE = 1.0 - DAMAGE_VARIANCE;
    private static final double UP_VARIANCE = DAMAGE_VARIANCE * 2.0;

    private double damageVariance(Random rng) {
        double roll = (rng.nextDouble() + rng.nextDouble()) / 2.0;
        return DOWN_VARIANCE + roll * UP_VARIANCE;
    }

    public void onTurnStart(Action action, List<String> out) {
        attackModifierThisTurn = 1.0;
        defenseModifierThisTurn = 1.0;
        dodgeModifierThisTurn = 1.0;

        applyBleedTick(action, out);
        applyStaggerPenalty(action, out);
    }

    private void applyBleedTick(Action action, List<String> out) {
        if (bleedTurns <= 0 || !isAlive()) return;

        double bleedDamage = stats.getMaxHealth() * BLEED_MAX_HEALTH_RATIO;
        if (action == Action.DEFEND) {
            bleedDamage *= (1.0 - BLEED_DEFEND_REDUCTION);
        }

        bleedDamage = round2(Math.max(0.0, bleedDamage));
        if (bleedDamage > 0) {
            stats.damage(bleedDamage);
            if (out != null) {
                String suffix = action == Action.DEFEND ? " però la defensa en redueix part." : ".";
                out.add(Ansi.RED + "  - " + name + " pateix " + bleedDamage + " de sagnat" + suffix);
            }
        }
        bleedTurns--;
    }

    private void applyStaggerPenalty(Action action, List<String> out) {
        if (staggerTurns <= 0) return;

        switch (action) {
            case ATTACK -> {
                attackModifierThisTurn = STAGGER_ATTACK_MULTIPLIER;
                if (out != null) out.add(Ansi.YELLOW + "  ! " + name + " està desequilibrat: el seu atac perd força.");
            }
            case DEFEND -> {
                defenseModifierThisTurn = STAGGER_DEFEND_MULTIPLIER;
                if (out != null) out.add(Ansi.YELLOW + "  ! " + name + " defensa mal posicionat.");
            }
            case DODGE -> {
                dodgeModifierThisTurn = STAGGER_DODGE_MULTIPLIER;
                if (out != null) out.add(Ansi.YELLOW + "  ! " + name + " intenta esquivar desequilibrat.");
            }
            case CHARGE -> {
                if (out != null) out.add(Ansi.YELLOW + "  ! " + name + " carrega lentament per l'aturdiment.");
            }
        }

        staggerTurns--;
    }

    public Result defend(double attack) {
        if (attack <= 0) return new Result(0, name + " ha bloquejat... sense raó aparent.");

        boolean guardBroken = isGuardBroken();
        double defenseVariance = 0.92 + rng.nextDouble() * 0.16;
        double mitigationRatio = guardBroken ? 0.32 : 0.6;
        mitigationRatio *= defenseModifierThisTurn;
        mitigationRatio = Math.clamp(mitigationRatio, 0.10, 0.75);

        double mitigated = attack * mitigationRatio * defenseVariance;
        double received = Math.max(0, attack - mitigated);
        stats.damage(received);

        if (guardBroken) {
            resetGuardStacks();
            applyVulnerable(1);
            return new Result(received, name + " intenta bloquejar, però la guàrdia es trenca i queda vulnerable.");
        }

        increaseGuardStacks();
        return new Result(received, name + " ha bloquejat l'atac.");
    }

    public Result dodge(double attack) {
        DodgeResult dodgeResult = internalDodge(attack);
        if (dodgeResult.noAttack) return new Result(0, name + " ha esquivat... l'aire.");

        double recived = dodgeResult.recived();
        stats.damage(recived);

        if (recived <= 0) return new Result(0, name + " ha esquivat l'atac.");
        return new Result(recived, name + " ha rebut l'atac de ple.");
    }

    protected record DodgeResult(double recived, boolean noAttack) {}

    protected DodgeResult internalDodge(double attack) {
        if (attack <= 0) return new DodgeResult(0, true);
        double dodgeProb = Math.clamp(tryToDodge() * dodgeModifierThisTurn * momentumDodgeMultiplier(), 0.03, 0.85);
        double multiplier = (rng.nextDouble() < dodgeProb ? 0 : DODGE_FAIL_MULTIPLIER);
        return new DodgeResult(attack * multiplier, false);
    }

    protected double tryToDodge() {
        double dexComponent = (stats.getDexterity() - 10) * 0.02;
        double luckComponent = stats.getLuck() * 0.0015;
        double dodgeProb = dexComponent + luckComponent;
        dodgeProb *= stats.resistanceDodgeMultiplier();
        if (hasEffect(Exhaustion.INTERNAL_EFFECT_KEY)) dodgeProb *= Exhaustion.DODGE_MULTIPLIER;
        return Math.clamp(dodgeProb, 0.05, 0.75);
    }

    public void increaseGuardStacks() { guardStacks = Math.min(MAX_GUARD_STACKS, guardStacks + 1); }
    public void resetGuardStacks() { guardStacks = 0; }
    public boolean isGuardBroken() { return guardStacks >= GUARD_BREAK_THRESHOLD; }
    public void prepareChargedAttack() { chargedAttack = true; }
    public boolean consumeChargedAttack() { boolean wasCharged = chargedAttack; chargedAttack = false; return wasCharged; }
    public double chargedAttackMultiplier() { return CHARGED_ATTACK_MULTIPLIER; }

    public void applyVulnerable(int turns) { vulnerableTurns = Math.max(vulnerableTurns, turns); }
    public double consumeIncomingDamageMultiplier() {
        double multiplier = isVulnerable() ? VULNERABLE_DAMAGE_MULTIPLIER : 1.0;
        if (vulnerableTurns > 0) vulnerableTurns--;
        return multiplier;
    }

    public void applyBleed(int turns) { bleedTurns = Math.max(bleedTurns, turns); }
    public void applyStagger(int turns) { staggerTurns = Math.max(staggerTurns, turns); }
    public double getAttackModifierThisTurn() { return attackModifierThisTurn; }

    public void gainMomentum() {
        momentumStacks = Math.min(MAX_MOMENTUM_STACKS, momentumStacks + 1);
    }

    public void loseMomentum() {
        momentumStacks = Math.max(0, momentumStacks - 1);
    }

    public void resetMomentum() {
        momentumStacks = 0;
    }

    public double momentumAttackMultiplierAgainst(Character defender) {
        double scale = 1.0;
        if (defender != null && (defender.isDesperate() || defender.healthRatio() + 0.18 < healthRatio())) {
            scale = MOMENTUM_SUPPRESSION_WHEN_TARGET_LOW;
        }
        return 1.0 + momentumStacks * MOMENTUM_ATTACK_BONUS_PER_STACK * scale;
    }

    public double momentumDodgeMultiplier() {
        return 1.0 + momentumStacks * MOMENTUM_DODGE_BONUS_PER_STACK;
    }

    public double comebackAttackMultiplierAgainst(Character opponent) {
        double bonus = 1.0;
        if (isDesperate()) {
            bonus += 0.10;
        }

        double gap = healthGapRatioAgainst(opponent);
        if (gap > 0) {
            bonus += Math.min(UNDERDOG_DAMAGE_MAX_BONUS, (gap / UNDERDOG_GAP_REFERENCE) * UNDERDOG_DAMAGE_MAX_BONUS);
        }

        return round2(bonus);
    }

    public double comebackIncomingDamageMultiplierAgainst(Character opponent) {
        double reduction = 0.0;
        if (isDesperate()) {
            reduction += 0.08;
        }

        double gap = healthGapRatioAgainst(opponent);
        if (gap > 0) {
            reduction += Math.min(UNDERDOG_DAMAGE_TAKEN_MAX_REDUCTION, (gap / UNDERDOG_GAP_REFERENCE) * UNDERDOG_DAMAGE_TAKEN_MAX_REDUCTION);
        }

        reduction = Math.min(0.18, reduction);
        return round2(1.0 - reduction);
    }

    public boolean tryTriggerAdrenalineSurge(Character opponent, rpgcombat.combat.services.EndRoundRegenBonus bonus, List<String> out) {
        if (bonus == null || adrenalineSurgeUsed || !isAlive() || healthRatio() > ADRENALINE_TRIGGER_HEALTH_RATIO) {
            return false;
        }

        adrenalineSurgeUsed = true;
        adrenaline = buildAdrenalineFromCrisis(opponent);

        double hpPct = round2(0.028 + (adrenaline / 1000.0));
        double manaPct = round2(0.012 + (adrenaline / 1800.0));
        bonus.add(hpPct, manaPct);

        if (out != null) {
            out.add("[GREEN|!] " + name + " entra en adrenalina: accelera la seva regeneració per al final de la ronda.");
        }

        return true;
    }

    private double buildAdrenalineFromCrisis(Character opponent) {
        double gap = healthGapRatioAgainst(opponent);
        double built = ADRENALINE_BASE + Math.min(ADRENALINE_GAP_BONUS, (gap / UNDERDOG_GAP_REFERENCE) * ADRENALINE_GAP_BONUS);
        if (isDesperate()) {
            built += ADRENALINE_DESPERATE_BONUS;
        }
        adrenaline = round2(Math.clamp(built, ADRENALINE_BASE, ADRENALINE_MAX));
        return adrenaline;
    }

    private double healthGapRatioAgainst(Character opponent) {
        if (opponent == null) {
            return 0.0;
        }

        double myMax = Math.max(1.0, stats.getMaxHealth());
        double gap = opponent.getStatistics().getHealth() - stats.getHealth();
        return Math.max(0.0, gap / myMax);
    }

    public Result getDamage(double attack) {
        stats.damage(attack);
        return new Result(attack, name + " ha rebut l'atac de ple.");
    }

    public boolean isAlive() { return stats.getHealth() > 0; }
    public void regen() { stats.reg(); }
    public Random rng() { return rng; }

    public void addEffect(Effect incoming) {
        if (incoming == null) return;
        if (effects.isEmpty()) {
            effects.add(incoming);
            return;
        }

        for (int i = 0; i < effects.size(); i++) {
            Effect existing = effects.get(i);
            if (!existing.key().equals(incoming.key())) continue;
            StackingRule rule = existing.stackingRule();
            switch (rule) {
                case IGNORE -> { return; }
                case REPLACE -> { effects.set(i, incoming); return; }
                case REFRESH, STACK -> { existing.mergeFrom(incoming); return; }
            }
        }

        effects.add(incoming);
        effects.sort(Comparator.comparingInt(Effect::priority).reversed());
    }

    public void clearEffects() { effects.clear(); }

    public List<String> triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng) {
        if (effects.isEmpty()) return List.of();
        List<String> messages = new ArrayList<>();
        triggerEffects(ctx, phase, rng, messages);
        return messages;
    }

    public void triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng, List<String> out) {
        if (effects.isEmpty()) return;
        for (Effect e : effects) {
            if (!e.isActive()) continue;
            EffectResult r = e.onPhase(ctx, phase, rng, this);
            if (r != null && r.message() != null && !r.message().isBlank()) out.add(r.message());
        }
        cleanupExpiredEffects();
    }

    protected void cleanupExpiredEffects() {
        if (effects.isEmpty()) return;
        effects.removeIf(Effect::isExpired);
    }

    public List<Effect> getEffects() { return List.copyOf(effects); }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nom no pot ser nul ni buit");
    }

    private static void validateAge(int age) {
        if (age <= 0) throw new IllegalArgumentException("L'edat ha de ser major que 0");
    }

    private static void validateStats(int[] stats) {
        if (stats == null) throw new IllegalArgumentException("L'array d'estadístiques no pot ser nul");
        if (stats.length != 7) throw new IllegalArgumentException("Les estadístiques han de contenir exactament 7 valors");

        int sum = 0;
        for (int stat : stats) {
            if (stat < MIN_STAT) throw new IllegalArgumentException("Cada estadística ha de ser com a mínim " + MIN_STAT);
            sum += stat;
        }

        if (stats[2] < MIN_CONSTITUTION) {
            throw new IllegalArgumentException("La constitució (vida) ha de ser com a mínim " + MIN_CONSTITUTION);
        }
        if (sum != TOTAL_POINTS) {
            throw new IllegalArgumentException("La suma total de punts ha de ser exactament " + TOTAL_POINTS + ". Suma actual: " + sum);
        }
    }

    protected static int[] applyBreed(int[] stats, Breed breed) {
        Stat[] statValues = Stat.values();
        int[] effectiveStats = stats.clone();
        for (int i = 0; i < stats.length; i++) {
            effectiveStats[i] = Breed.effectiveStat(stats[i], statValues[i], breed);
        }
        return effectiveStats;
    }

    public void setInvulnerable(boolean invulnerable) { stats.setInvulnerable(invulnerable); }
    public void applyInvulnerability() { stats.applyInvulnerability(); }

    private static double round2(double n) { return Math.round(n * 100.0) / 100.0; }
}
