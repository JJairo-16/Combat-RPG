package rpgcombat.models.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import rpgcombat.models.breeds.Breed;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;
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
    /** Mínim de constitució. */
    private static final int MIN_CONSTITUTION = MIN_STAT + 2;
    private static final double SPIRITUAL_CALLING_THRESHOLD = 0.20;

    protected final String name;
    protected final int age;
    protected final Breed breed;

    protected final Statistics stats;
    protected Weapon weapon;

    protected final Random rng = new Random();
    protected final List<Effect> effects = new ArrayList<>();

    private int spiritualCallingCooldown = 0;

    /** Crea un personatge validant nom, edat i estadístiques. */
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

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Breed getBreed() {
        return breed;
    }

    public Statistics getStatistics() {
        return stats;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public int getSpiritualCallingCooldown() {
        return spiritualCallingCooldown;
    }

    public void setSpiritualCallingCooldown(int turns) {
        this.spiritualCallingCooldown = Math.max(0, turns);
    }

    public void tickSpiritualCallingCooldown() {
        if (spiritualCallingCooldown > 0) {
            spiritualCallingCooldown--;
        }
    }

    public boolean hasEffect(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        for (Effect effect : effects) {
            if (effect != null && !effect.isExpired() && key.equals(effect.key())) {
                return true;
            }
        }

        return false;
    }

    /** Retorna un efecte actiu per clau, o {@code null} si no existeix. */
    public Effect getEffect(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        for (Effect effect : effects) {
            if (effect != null && !effect.isExpired() && key.equals(effect.key())) {
                return effect;
            }
        }

        return null;
    }

    /** Elimina un efecte per clau. */
    public boolean removeEffect(String key) {
        if (key == null || key.isBlank() || effects.isEmpty()) {
            return false;
        }

        return effects.removeIf(effect -> effect != null && key.equals(effect.key()));
    }

    public boolean isAtOrBelowHealthRatio(double ratio) {
        double maxHealth = stats.getMaxHealth();
        if (maxHealth <= 0) {
            return false;
        }

        return (stats.getHealth() / maxHealth) <= ratio;
    }

    public boolean canUseSpiritualCalling() {
        return hasEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY)
                && spiritualCallingCooldown <= 0
                && isAtOrBelowHealthRatio(SPIRITUAL_CALLING_THRESHOLD);
    }

    /** Equipa una arma si compleix els requisits. */
    public boolean setWeapon(Weapon w) {
        if (w == null) {
            return false;
        }

        if (!w.canEquip(stats)) {
            return false;
        }

        weapon = w;
        return true;
    }

    /** Ataca amb l'arma equipada o sense arma. */
    public AttackResult attack() {
        if (weapon == null) {
            return attackUnarmed();
        }

        return weapon.attack(stats, rng);
    }

    /** Executa un atac sense arma. */
    protected AttackResult attackUnarmed() {
        return new AttackResult(
                WeaponType.PHYSICAL.getBasicDamage(5, stats),
                "ataca amb les mans desnudes.");
    }

    /** Defensa reduint el dany rebut. */
    public Result defend(double attack) {
        if (attack <= 0) {
            return new Result(0, name + " ha bloquejat... sense raó aparent.");
        }

        double recived = attack * 0.6;
        stats.damage(recived);
        return new Result(recived, name + " ha bloquejat l'atac.");
    }

    /** Intenta esquivar; si falla, rep tot el dany. */
    public Result dodge(double attack) {
        DodgeResult dodgeResult = internalDodge(attack);
        if (dodgeResult.noAttack)
            return new Result(0, name + " ha esquivat... l'aire.");

        double recived = dodgeResult.recived();
        stats.damage(recived);

        if (recived <= 0) {
            return new Result(0, name + " ha esquivat l'atac.");
        }

        return new Result(recived, name + " ha rebut l'atac de ple.");
    }

    /** Resultat intern d'una esquiva. */
    protected record DodgeResult(double recived, boolean noAttack) {
    }

    /** Resol internament el càlcul de l'esquiva. */
    protected DodgeResult internalDodge(double attack) {
        if (attack <= 0) {
            return new DodgeResult(0, true);
        }

        double dodgeProb = tryToDodge();
        double multiplier = (rng.nextDouble() < dodgeProb ? 0 : 1);
        double recived = attack * multiplier;

        return new DodgeResult(recived, false);
    }

    /** Calcula la probabilitat d'esquiva. */
    protected double tryToDodge() {
        double dexComponent = (stats.getDexterity() - 10) * 0.02;
        double luckComponent = stats.getLuck() * 0.0015;

        double dodgeProb = dexComponent + luckComponent;

        return Math.clamp(dodgeProb, 0.05, 0.75);
    }

    /** Aplica dany directe. */
    public Result getDamage(double attack) {
        stats.damage(attack);
        return new Result(attack, name + " ha rebut l'atac de ple.");
    }

    /** Indica si el personatge continua viu. */
    public boolean isAlive() {
        return stats.getHealth() > 0;
    }

    /** Aplica la regeneració base. */
    public void regen() {
        stats.reg();
    }

    /** Retorna el generador aleatori del personatge. */
    public Random rng() {
        return rng;
    }

    /** Afegeix un efecte segons la seva regla d'acumulació. */
    public void addEffect(Effect incoming) {
        if (incoming == null) {
            return;
        }

        if (effects.isEmpty()) {
            effects.add(incoming);
            return;
        }

        for (int i = 0; i < effects.size(); i++) {
            Effect existing = effects.get(i);

            if (!existing.key().equals(incoming.key())) {
                continue;
            }

            StackingRule rule = existing.stackingRule();

            switch (rule) {
                case IGNORE:
                    return;

                case REPLACE:
                    effects.set(i, incoming);
                    return;

                case REFRESH, STACK:
                    existing.mergeFrom(incoming);
                    return;
            }
        }

        effects.add(incoming);
        effects.sort(Comparator.comparingInt(Effect::priority).reversed());
    }

    public void clearEffects() {
        effects.clear();
    }

    /** Executa els efectes d'una fase i retorna els missatges. */
    public List<String> triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng) {
        if (effects.isEmpty()) {
            return List.of();
        }

        List<String> messages = new ArrayList<>();
        triggerEffects(ctx, phase, rng, messages);
        return messages;
    }

    /** Executa els efectes d'una fase i afegeix els missatges a la sortida. */
    public void triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng, List<String> out) {
        if (effects.isEmpty()) {
            return;
        }

        for (Effect e : effects) {
            if (!e.isActive()) {
                continue;
            }

            EffectResult r = e.onPhase(ctx, phase, rng, this);
            if (r != null && r.message() != null && !r.message().isBlank()) {
                out.add(r.message());
            }
        }

        cleanupExpiredEffects();
    }

    /** Elimina efectes expirats. */
    protected void cleanupExpiredEffects() {
        if (effects.isEmpty()) {
            return;
        }

        effects.removeIf(Effect::isExpired);
    }

    /** Retorna una còpia immutable dels efectes actius. */
    public List<Effect> getEffects() {
        return List.copyOf(effects);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nom no pot ser nul ni buit");
        }
    }

    private static void validateAge(int age) {
        if (age <= 0) {
            throw new IllegalArgumentException("L'edat ha de ser major que 0");
        }
    }

    /** Valida longitud, mínims i suma de les estadístiques. */
    private static void validateStats(int[] stats) {
        if (stats == null) {
            throw new IllegalArgumentException("L'array d'estadístiques no pot ser nul");
        }

        if (stats.length != 7) {
            throw new IllegalArgumentException("Les estadístiques han de contenir exactament 7 valors");
        }

        int sum = 0;

        for (int stat : stats) {
            if (stat < MIN_STAT) {
                throw new IllegalArgumentException("Cada estadística ha de ser com a mínim " + MIN_STAT);
            }
            sum += stat;
        }

        if (stats[2] < MIN_CONSTITUTION) {
            throw new IllegalArgumentException("La constitució (vida) ha de ser com a mínim " + MIN_CONSTITUTION);
        }

        if (sum != TOTAL_POINTS) {
            throw new IllegalArgumentException(
                    "La suma total de punts ha de ser exactament " + TOTAL_POINTS + ". Suma actual: " + sum);
        }
    }

    /** Aplica els modificadors de raça a les estadístiques base. */
    protected static int[] applyBreed(int[] stats, Breed breed) {
        Stat[] statValues = Stat.values();
        int[] effectiveStats = stats.clone();

        for (int i = 0; i < stats.length; i++) {
            effectiveStats[i] = Breed.effectiveStat(stats[i], statValues[i], breed);
        }

        return effectiveStats;
    }

    public void setInvulnerable(boolean invulnerable) {
        stats.setInvulnerable(invulnerable);
    }

    public void applyInvulnerability() {
        stats.applyInvulnerability();
    }
}