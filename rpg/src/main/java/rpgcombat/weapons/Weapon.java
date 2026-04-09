package rpgcombat.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpgcombat.models.characters.Statistics;
import rpgcombat.weapons.attack.Attack;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponType;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.WeaponPassive;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Representa una arma amb dany base, probabilitat/multiplicador de crític,
 * un atac associat i una llista de passius que s'activen per fases.
 */
public class Weapon {

    private final String id;
    private final String name;
    private final String description;

    private final int damage;
    private final double criticalProb;
    private final double criticalDamage;

    private final WeaponType type;
    private final Attack attack;
    private final double manaPrice;

    private final List<WeaponPassive> passives;

    // Estat intern informatiu de l'últim atac generat per l'arma.
    private boolean lastWasCrit = false;
    private double lastAttackDamage = 0;
    private double lastNonCriticalDamage = 0;

   /** Crea una nova arma. */
    public Weapon(
            String id,
            String name,
            String description,
            int damage,
            double criticalProb,
            double criticalDamage,
            WeaponType type,
            Attack attack,
            double price,
            List<WeaponPassive> passives) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.damage = damage;
        this.criticalProb = criticalProb;
        this.criticalDamage = criticalDamage;
        this.type = type;
        this.attack = attack;
        this.manaPrice = price;
        this.passives = (passives == null) ? List.of() : List.copyOf(passives);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getBaseDamage() {
        return damage;
    }

    public double getCriticalProb() {
        return criticalProb;
    }

    public double getCriticalDamage() {
        return criticalDamage;
    }

    public WeaponType getType() {
        return type;
    }

    public double getManaPrice() {
        return manaPrice;
    }

    public AttackResult attack(Statistics stats, Random rng) {
        if (stats.getMana() < manaPrice) {
            return new AttackResult(
                    WeaponType.PHYSICAL.getBasicDamage(5, stats),
                    "no li quedava mana, aixi que li dona un cop.");
        }
        return attack.execute(this, stats, rng);
    }

    public double basicAttack(Statistics stats, Random rng) {
        double baseDamage = type.getBasicDamage(damage, stats);
        baseDamage *= damageVariance(rng);
        baseDamage = round2(baseDamage);

        lastNonCriticalDamage = baseDamage;
        lastWasCrit = rollsCritical(stats, rng);

        if (!lastWasCrit) {
            lastAttackDamage = baseDamage;
            return baseDamage;
        }

        double multiplier = resolveCriticalMultiplier(stats);
        lastAttackDamage = round2(baseDamage * multiplier);
        return lastAttackDamage;
    }

    public AttackResult basicAttackWithMessage(Statistics stats, Random rng) {
        double dmg = basicAttack(stats, rng);
        return lastWasCrit
                ? new AttackResult(dmg, "llença un cop crític.")
                : new AttackResult(dmg, "llença un atac.");
    }

    public double resolveCriticalChance(Statistics stats) {
        double probTotal = criticalProb + (stats.getLuck() * 0.002);
        return Math.clamp(probTotal, 0.0, 0.95);
    }

    public double resolveCriticalMultiplier(Statistics stats) {
        return Math.max(1.0, criticalDamage + stats.getLuck() * 0.01);
    }

    public double lastNonCriticalDamage() {
        return lastNonCriticalDamage;
    }

    public boolean lastWasCritic() {
        return lastWasCrit;
    }

    public double lastAttackDamage() {
        return lastAttackDamage;
    }

    public void registerResolvedAttack(boolean wasCrit, double finalDamage) {
        this.lastWasCrit = wasCrit;
        this.lastAttackDamage = round2(finalDamage);
    }

    public boolean canEquip(Statistics stats) {
        return type.canEquip(stats);
    }

    public List<String> triggerPhase(HitContext ctx, Random rng, Phase phase) {
        if (passives.isEmpty()) {
            return List.of();
        }

        List<String> messages = new ArrayList<>();
        triggerPhase(ctx, rng, phase, messages);
        return messages;
    }

    public void triggerPhase(HitContext ctx, Random rng, Phase phase, List<String> out) {
        if (passives.isEmpty()) {
            return;
        }

        for (WeaponPassive p : passives) {
            String msg = p.onPhase(this, ctx, rng, phase);
            if (msg != null && !msg.isBlank()) {
                out.add(msg);
            }
        }
    }

    public void triggerAfterHit(HitContext ctx, Random rng, List<String> out) {
        triggerPhase(ctx, rng, Phase.AFTER_HIT, out);
    }

    public List<String> triggerAfterHit(HitContext ctx, Random rng) {
        return triggerPhase(ctx, rng, Phase.AFTER_HIT);
    }

    private static final double DAMAGE_VARIANCE = 0.07;
    private static final double DOWN_VARIANCE = 1.0 - DAMAGE_VARIANCE;
    private static final double UP_VARIANCE = DAMAGE_VARIANCE * 2.0;

    private double damageVariance(Random rng) {
        double roll = (rng.nextDouble() + rng.nextDouble()) / 2.0;
        return DOWN_VARIANCE + roll * UP_VARIANCE;
    }

    private boolean rollsCritical(Statistics stats, Random rng) {
        return rng.nextDouble() < resolveCriticalChance(stats);
    }

    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}
