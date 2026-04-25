package rpgcombat.models.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.AttackDefenseVarianceConfig;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.character.AdrenalineConfig;
import rpgcombat.balance.config.character.ChargedAttackConfig;
import rpgcombat.balance.config.character.GuardBreakConfig;
import rpgcombat.balance.config.character.MomentumConfig;
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
import rpgcombat.weapons.passives.HitContext;

/**
 * Representa un personatge amb estadístiques, raça, arma i efectes.
 */
public class Character {
    private static final int TOTAL_POINTS = 140;
    private static final int MIN_STAT = 10;
    private static final int MIN_CONSTITUTION = MIN_STAT + 2;
    private static final double SPIRITUAL_CALLING_THRESHOLD = 0.20;

    private static final double BLEED_MAX_HEALTH_RATIO = 0.025;
    private static final double BLEED_DEFEND_REDUCTION = 0.40;
    private static final double STAGGER_ATTACK_MULTIPLIER = 0.78;
    private static final double STAGGER_DEFEND_MULTIPLIER = 0.78;
    private static final double STAGGER_DODGE_MULTIPLIER = 0.72;

    protected final String name;
    protected final int age;
    protected final Breed breed;

    protected final Statistics stats;
    protected Weapon weapon;

    protected final Random rng = new Random();
    protected final List<Effect> effects = new ArrayList<>();
    protected final UnarmedAttack unarmedAttack;

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
    private double nextIncomingDamageMultiplier = 1.0;

    public Character(String name, int age, int[] stats, Breed breed) {
        validateName(name);
        validateAge(age);
        validateStats(stats);

        int[] effectiveStats = applyBreed(stats, breed);

        this.name = name;
        this.age = age;
        this.breed = breed;
        this.stats = new Statistics(effectiveStats);
        this.unarmedAttack = new UnarmedAttack(this.stats, rng);
    }

    /**
     * Obté la configuració global de balance de combat.
     */
    private static CombatBalanceConfig balance() {
        return CombatBalanceRegistry.get();
    }

    /**
     * Obté la configuració de guard break.
     */
    private static GuardBreakConfig guardBreak() {
        return balance().guardBreak();
    }

    /**
     * Obté la configuració de momentum.
     */
    private static MomentumConfig momentumCfg() {
        return balance().momentum();
    }

    /**
     * Obté la configuració de variació d'atac/defensa.
     */
    private static AttackDefenseVarianceConfig varianceCfg() {
        return balance().attackDefenseVariance();
    }

    /**
     * Obté la configuració d'adrenalina.
     */
    private static AdrenalineConfig adrenalineCfg() {
        return balance().adrenaline();
    }

    /**
     * Obté la configuració d'atac carregat.
     */
    private static ChargedAttackConfig chargedAttackCfg() {
        return balance().chargedAttack();
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

    public int getGuardStacks() {
        return guardStacks;
    }

    public boolean hasChargedAttack() {
        return chargedAttack;
    }

    public boolean isVulnerable() {
        return vulnerableTurns > 0;
    }

    public boolean isBleeding() {
        return bleedTurns > 0;
    }

    public boolean isStaggered() {
        return staggerTurns > 0;
    }

    public int getMomentumStacks() {
        return momentumStacks;
    }

    public boolean hasUsedAdrenalineSurge() {
        return adrenalineSurgeUsed;
    }

    public double getAdrenaline() {
        return adrenaline;
    }

    public void setSpiritualCallingCooldown(int turns) {
        this.spiritualCallingCooldown = Math.max(0, turns);
    }

    public void tickSpiritualCallingCooldown() {
        if (spiritualCallingCooldown > 0)
            spiritualCallingCooldown--;
    }

    public boolean hasEffect(String key) {
        if (key == null || key.isBlank())
            return false;
        for (Effect effect : effects) {
            if (effect != null && !effect.isExpired() && key.equals(effect.key()))
                return true;
        }
        return false;
    }

    public Effect getEffect(String key) {
        if (key == null || key.isBlank())
            return null;
        for (Effect effect : effects) {
            if (effect != null && !effect.isExpired() && key.equals(effect.key()))
                return effect;
        }
        return null;
    }

    public boolean removeEffect(String key) {
        if (key == null || key.isBlank() || effects.isEmpty())
            return false;
        return effects.removeIf(effect -> effect != null && key.equals(effect.key()));
    }

    /**
     * Retorna la vida actual normalitzada entre 0 i 1.
     */
    public double healthRatio() {
        double maxHealth = stats.getMaxHealth();
        if (maxHealth <= 0)
            return 0;
        return Math.clamp(stats.getHealth() / maxHealth, 0.0, 1.0);
    }

    /**
     * Indica si el personatge està en estat desesperat segons la vida.
     */
    public boolean isDesperate() {
        return isAlive() && healthRatio() <= adrenalineCfg().desperateHealthRatio();
    }

    /**
     * Comprova si la vida està per sota o igual a un percentatge concret.
     */
    public boolean isAtOrBelowHealthRatio(double ratio) {
        double maxHealth = stats.getMaxHealth();
        if (maxHealth <= 0)
            return false;
        return (stats.getHealth() / maxHealth) <= ratio;
    }

    /**
     * Determina si pot activar la crida espiritual.
     */
    public boolean canUseSpiritualCalling() {
        return hasEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY)
                && spiritualCallingCooldown <= 0
                && isAtOrBelowHealthRatio(SPIRITUAL_CALLING_THRESHOLD);
    }

    /**
     * Intenta equipar una arma si compleix els requisits.
     */
    public boolean setWeapon(Weapon w) {
        if (w == null)
            return false;
        if (!w.canEquip(stats))
            return false;
        weapon = w;
        return true;
    }

    /**
     * Executa un atac amb arma o desarmat si no n'hi ha.
     */
    public AttackResult attack() {
        if (weapon == null)
            return unarmedAttack.attackUnarmed();

        double manaCost = weapon.getManaPrice();
        double mana = stats.getMana();
        if (manaCost > mana)
            return unarmedAttack.fallbackAttack();

        return weapon.attack(stats, rng);
    }

    /**
     * Prepara l'estat del personatge a l'inici del torn.
     */
    public void onTurnStart(Action action, List<String> out) {
        attackModifierThisTurn = 1.0;
        defenseModifierThisTurn = 1.0;
        dodgeModifierThisTurn = 1.0;

        applyBleedTick(action, out);
        applyStaggerPenalty(action, out);
    }

    /**
     * Aplica el tick de sagnat a l'inici del torn.
     */
    private void applyBleedTick(Action action, List<String> out) {
        if (bleedTurns <= 0 || !isAlive())
            return;

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

    /**
     * Aplica la penalització d'aturdiment segons l'acció del torn.
     */
    private void applyStaggerPenalty(Action action, List<String> out) {
        if (staggerTurns <= 0)
            return;

        switch (action) {
            case ATTACK -> {
                attackModifierThisTurn = STAGGER_ATTACK_MULTIPLIER;
                if (out != null)
                    out.add(Ansi.YELLOW + "  ! " + name + " està desequilibrat: el seu atac perd força.");
            }
            case DEFEND -> {
                defenseModifierThisTurn = STAGGER_DEFEND_MULTIPLIER;
                if (out != null)
                    out.add(Ansi.YELLOW + "  ! " + name + " defensa mal posicionat.");
            }
            case DODGE -> {
                dodgeModifierThisTurn = STAGGER_DODGE_MULTIPLIER;
                if (out != null)
                    out.add(Ansi.YELLOW + "  ! " + name + " intenta esquivar desequilibrat.");
            }
            case CHARGE -> {
                if (out != null)
                    out.add(Ansi.YELLOW + "  ! " + name + " carrega lentament per l'aturdiment.");
            }
        }

        staggerTurns--;
    }

    /**
     * Resol una defensa contra un atac entrant.
     */
    public Result defend(double attack) {
        if (attack <= 0)
            return new Result(0, name + " ha bloquejat... sense raó aparent.");

        GuardBreakConfig cfg = guardBreak();
        AttackDefenseVarianceConfig.DefenseConfig defCfg = varianceCfg().defense();

        boolean guardBroken = isGuardBroken();
        double defenseVariance = defCfg.minMultiplier()
                + rng.nextDouble() * (defCfg.maxMultiplier() - defCfg.minMultiplier());

        double mitigationRatio = guardBroken
                ? cfg.brokenGuardMitigationRatio()
                : cfg.normalMitigationRatio();

        mitigationRatio *= defenseModifierThisTurn;
        mitigationRatio = Math.clamp(
                mitigationRatio,
                cfg.finalMitigationClampMin(),
                cfg.finalMitigationClampMax());

        double mitigated = attack * mitigationRatio * defenseVariance;
        double received = Math.max(0, attack - mitigated);
        stats.damage(received);

        if (guardBroken) {
            resetGuardStacks();
            applyVulnerable(cfg.applyVulnerableTurnsOnBreak());
            return new Result(received, name + " intenta bloquejar, però la guàrdia es trenca i queda vulnerable.");
        }

        increaseGuardStacks();
        return new Result(received, name + " ha bloquejat l'atac.");
    }

    /**
     * Resol una esquiva contra un atac entrant.
     */
    public Result dodge(double attack) {
        DodgeResult dodgeResult = internalDodge(attack);
        if (dodgeResult.noAttack)
            return new Result(0, name + " ha esquivat... l'aire.");

        double recived = dodgeResult.recived();
        stats.damage(recived);

        if (recived <= 0)
            return new Result(0, name + " ha esquivat l'atac.");
        return new Result(recived, name + " ha rebut l'atac de ple.");
    }

    /**
     * Resultat intern d'una esquiva.
     */
    protected record DodgeResult(double recived, boolean noAttack) {
    }

    /**
     * Calcula internament el resultat d'una esquiva.
     */
    protected DodgeResult internalDodge(double attack) {
        if (attack <= 0)
            return new DodgeResult(0, true);

        AttackDefenseVarianceConfig.DodgeConfig cfg = varianceCfg().dodge();
        double dodgeProb = Math.clamp(
                tryToDodge() * dodgeModifierThisTurn * momentumDodgeMultiplier(),
                cfg.finalMinClamp(),
                cfg.finalMaxClamp());

        double multiplier = (rng.nextDouble() < dodgeProb ? 0 : cfg.failedDodgeDamageMultiplier());
        return new DodgeResult(attack * multiplier, false);
    }

    /**
     * Calcula la probabilitat base d'esquiva.
     */
    protected double tryToDodge() {
        AttackDefenseVarianceConfig.DodgeConfig cfg = varianceCfg().dodge();

        double dexComponent = (stats.getDexterity() - 10) * cfg.dexterityOffsetFrom10Multiplier();
        double luckComponent = stats.getLuck() * cfg.luckMultiplier();
        double dodgeProb = dexComponent + luckComponent;

        dodgeProb *= stats.resistanceDodgeMultiplier();
        if (hasEffect(Exhaustion.INTERNAL_EFFECT_KEY))
            dodgeProb *= Exhaustion.DODGE_MULTIPLIER;

        return Math.clamp(dodgeProb, cfg.baseMinClamp(), cfg.baseMaxClamp());
    }

    /**
     * Incrementa les càrregues de guàrdia fins al màxim configurat.
     */
    public void increaseGuardStacks() {
        guardStacks = Math.min(guardBreak().maxGuardStacks(), guardStacks + 1);
    }

    /**
     * Reinicia les càrregues de guàrdia.
     */
    public void resetGuardStacks() {
        guardStacks = 0;
    }

    /**
     * Indica si la guàrdia està trencada.
     */
    public boolean isGuardBroken() {
        return guardStacks >= guardBreak().breakThreshold();
    }

    /**
     * Prepara un atac carregat.
     */
    public void prepareChargedAttack() {
        if (chargedAttackCfg().canStoreOnlyOneCharge() && chargedAttack)
            return;
        chargedAttack = true;
    }

    /**
     * Consumeix la càrrega d'atac si la configuració ho indica.
     */
    public boolean consumeChargedAttack() {
        boolean wasCharged = chargedAttack;
        if (chargedAttackCfg().consumedOnNextAttack()) {
            chargedAttack = false;
        }
        return wasCharged;
    }

    /**
     * Retorna el multiplicador d'atac carregat.
     */
    public double chargedAttackMultiplier() {
        return chargedAttackCfg().damageMultiplier();
    }

    /**
     * Aplica vulnerabilitat durant un nombre de torns.
     */
    public void applyVulnerable(int turns) {
        vulnerableTurns = Math.max(vulnerableTurns, turns);
    }

    /**
     * Consumeix el multiplicador de dany entrant per vulnerabilitat.
     */
    public double consumeIncomingDamageMultiplier() {
        double multiplier = isVulnerable() ? guardBreak().vulnerableDamageMultiplier() : 1.0;
        multiplier *= nextIncomingDamageMultiplier;
        nextIncomingDamageMultiplier = 1.0;
        if (vulnerableTurns > 0)
            vulnerableTurns--;
        return multiplier;
    }

    /**
     * Multiplica el proper dany entrant directe.
     */
    public void multiplyNextIncomingDamage(double multiplier) {
        if (multiplier > 0) {
            nextIncomingDamageMultiplier *= multiplier;
        }
    }

    /**
     * Aplica l'estat de sagnat.
     */
    public void applyBleed(int turns) {
        bleedTurns = Math.max(bleedTurns, turns);
    }

    /**
     * Elimina el sagnat actiu.
     */
    public void clearBleed() {
        bleedTurns = 0;
    }

    /**
     * Elimina la vulnerabilitat activa.
     */
    public void clearVulnerable() {
        vulnerableTurns = 0;
    }

    /**
     * Elimina l'aturdiment actiu.
     */
    public void clearStagger() {
        staggerTurns = 0;
    }

    /**
     * Aplica l'estat d'aturdiment.
     */
    public void applyStagger(int turns) {
        staggerTurns = Math.max(staggerTurns, turns);
    }

    /**
     * Retorna el modificador d'atac actiu aquest torn.
     */
    public double getAttackModifierThisTurn() {
        return attackModifierThisTurn;
    }

    /**
     * Guanya una càrrega de momentum fins al màxim configurat.
     */
    public void gainMomentum() {
        momentumStacks = Math.min(momentumCfg().maxStacks(), momentumStacks + 1);
    }

    /**
     * Perd una càrrega de momentum.
     */
    public void loseMomentum() {
        momentumStacks = Math.max(0, momentumStacks - 1);
    }

    /**
     * Reinicia el momentum.
     */
    public void resetMomentum() {
        momentumStacks = 0;
    }

    /**
     * Calcula el multiplicador ofensiu de momentum contra un defensor.
     */
    public double momentumAttackMultiplierAgainst(Character defender) {
        MomentumConfig cfg = momentumCfg();

        double scale = 1.0;
        if (defender != null
                && (defender.isDesperate() || defender.healthRatio() + cfg.suppressionHealthOffset() < healthRatio())) {
            scale = cfg.suppressionWhenTargetLowHealth();
        }

        return 1.0 + momentumStacks * cfg.attackBonusPerStack() * scale;
    }

    /**
     * Calcula el multiplicador defensiu d'esquiva per momentum.
     */
    public double momentumDodgeMultiplier() {
        MomentumConfig cfg = momentumCfg();
        return 1.0 + momentumStacks * cfg.dodgeBonusPerStack();
    }

    /**
     * Calcula el multiplicador de comeback ofensiu segons la diferència de vida.
     */
    public double comebackAttackMultiplierAgainst(Character opponent) {
        AdrenalineConfig cfg = adrenalineCfg();
        AdrenalineConfig.UnderdogBonusesConfig underdog = cfg.underdogBonuses();

        double bonus = 1.0;
        if (isDesperate()) {
            bonus += underdog.desperateFlatAttackBonus();
        }

        double gap = healthGapRatioAgainst(opponent);
        if (gap > 0) {
            bonus += Math.min(underdog.damageMaxBonus(), (gap / cfg.gapReference()) * underdog.damageMaxBonus());
        }

        return round2(bonus);
    }

    /**
     * Calcula el multiplicador de dany entrant segons el bonus de comeback.
     */
    public double comebackIncomingDamageMultiplierAgainst(Character opponent) {
        AdrenalineConfig cfg = adrenalineCfg();
        AdrenalineConfig.UnderdogBonusesConfig underdog = cfg.underdogBonuses();

        double reduction = 0.0;
        if (isDesperate()) {
            reduction += underdog.desperateFlatDamageReduction();
        }

        double gap = healthGapRatioAgainst(opponent);
        if (gap > 0) {
            reduction += Math.min(
                    underdog.damageTakenMaxReduction(),
                    (gap / cfg.gapReference()) * underdog.damageTakenMaxReduction());
        }

        reduction = Math.min(underdog.incomingDamageReductionCap(), reduction);
        return round2(1.0 - reduction);
    }

    /**
     * Intenta activar una pujada d'adrenalina i afegir bonus de regeneració.
     */
    public boolean tryTriggerAdrenalineSurge(Character opponent, rpgcombat.combat.services.EndRoundRegenBonus bonus,
            List<String> out) {
        AdrenalineConfig cfg = adrenalineCfg();

        if (bonus == null || adrenalineSurgeUsed || !isAlive() || healthRatio() > cfg.triggerHealthRatio()) {
            return false;
        }

        adrenalineSurgeUsed = true;
        adrenaline = buildAdrenalineFromCrisis(opponent);

        AdrenalineConfig.RegenBonusConfig regenCfg = cfg.regenBonus();
        double hpPct = round2(regenCfg.healthBasePct() + (adrenaline / regenCfg.healthFromAdrenalineDivisor()));
        double manaPct = round2(regenCfg.manaBasePct() + (adrenaline / regenCfg.manaFromAdrenalineDivisor()));
        bonus.add(hpPct, manaPct);

        if (out != null) {
            out.add("[GREEN|!] " + name
                    + " entra en adrenalina: accelera la seva regeneració per al final de la ronda.");
        }

        return true;
    }

    /**
     * Construeix el valor d'adrenalina acumulada segons la situació de crisi.
     */
    private double buildAdrenalineFromCrisis(Character opponent) {
        AdrenalineConfig cfg = adrenalineCfg();

        double gap = healthGapRatioAgainst(opponent);
        double built = cfg.base()
                + Math.min(cfg.gapBonusMax(), (gap / cfg.gapReference()) * cfg.gapBonusMax());

        if (isDesperate()) {
            built += cfg.desperateBonus();
        }

        adrenaline = round2(Math.clamp(built, cfg.base(), cfg.max()));
        return adrenaline;
    }

    /**
     * Calcula la diferència relativa de vida respecte l'oponent.
     */
    private double healthGapRatioAgainst(Character opponent) {
        if (opponent == null) {
            return 0.0;
        }

        double myMax = Math.max(1.0, stats.getMaxHealth());
        double gap = opponent.getStatistics().getHealth() - stats.getHealth();
        return Math.max(0.0, gap / myMax);
    }

    /**
     * Aplica dany directe sense defensa ni esquiva.
     */
    public Result getDamage(double attack) {
        stats.damage(attack);
        return new Result(attack, name + " ha rebut l'atac de ple.");
    }

    /**
     * Indica si el personatge continua viu.
     */
    public boolean isAlive() {
        return stats.getHealth() > 0;
    }

    /**
     * Aplica la regeneració natural del personatge.
     */
    public void regen() {
        stats.reg();
    }

    /**
     * Retorna el generador aleatori propi del personatge.
     */
    public Random rng() {
        return rng;
    }

    /**
     * Afegeix un efecte al personatge segons la seva regla d'apilament.
     */
    public void addEffect(Effect incoming) {
        if (incoming == null)
            return;
        if (effects.isEmpty()) {
            effects.add(incoming);
            return;
        }

        for (int i = 0; i < effects.size(); i++) {
            Effect existing = effects.get(i);
            if (!existing.key().equals(incoming.key()))
                continue;
            StackingRule rule = existing.stackingRule();
            switch (rule) {
                case IGNORE -> {
                    return;
                }
                case REPLACE -> {
                    effects.set(i, incoming);
                    return;
                }
                case REFRESH, STACK -> {
                    existing.mergeFrom(incoming);
                    return;
                }
            }
        }

        effects.add(incoming);
        effects.sort(Comparator.comparingInt(Effect::priority).reversed());
    }

    /**
     * Elimina tots els efectes actius.
     */
    public void clearEffects() {
        effects.clear();
    }

    /**
     * Dispara els efectes d'una fase i retorna els missatges generats.
     */
    public List<String> triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng) {
        if (effects.isEmpty())
            return List.of();

        List<String> messages = new ArrayList<>();
        triggerEffects(ctx, phase, rng, messages);
        return messages;
    }

    /**
     * Dispara els efectes d'una fase i afegeix els missatges a la sortida donada.
     */
    public void triggerEffects(HitContext ctx, HitContext.Phase phase, Random rng, List<String> out) {
        if (effects.isEmpty())
            return;

        List<Effect> snapshot = List.copyOf(effects);

        for (Effect e : snapshot) {
            if (!e.isActive())
                continue;

            EffectResult r = e.onPhase(ctx, phase, rng, this);

            if (r != null && r.message() != null && !r.message().isBlank()) {
                out.add(r.message());
            }
        }

        cleanupExpiredEffects();
    }

    /**
     * Elimina els efectes expirats.
     */
    protected void cleanupExpiredEffects() {
        if (effects.isEmpty())
            return;
        effects.removeIf(Effect::isExpired);
    }

    /**
     * Retorna una còpia immutable dels efectes actius.
     */
    public List<Effect> getEffects() {
        return List.copyOf(effects);
    }

    /**
     * Valida el nom del personatge.
     */
    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("El nom no pot ser nul ni buit");
    }

    /**
     * Valida l'edat del personatge.
     */
    private static void validateAge(int age) {
        if (age <= 0)
            throw new IllegalArgumentException("L'edat ha de ser major que 0");
    }

    /**
     * Valida les estadístiques base del personatge.
     */
    private static void validateStats(int[] stats) {
        if (stats == null)
            throw new IllegalArgumentException("L'array d'estadístiques no pot ser nul");
        if (stats.length != 7)
            throw new IllegalArgumentException("Les estadístiques han de contenir exactament 7 valors");

        int sum = 0;
        for (int stat : stats) {
            if (stat < MIN_STAT)
                throw new IllegalArgumentException("Cada estadística ha de ser com a mínim " + MIN_STAT);
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

    /**
     * Aplica els modificadors racials a les estadístiques base.
     */
    protected static int[] applyBreed(int[] stats, Breed breed) {
        Stat[] statValues = Stat.values();
        int[] effectiveStats = stats.clone();
        for (int i = 0; i < stats.length; i++) {
            effectiveStats[i] = Breed.effectiveStat(stats[i], statValues[i], breed);
        }
        return effectiveStats;
    }

    /**
     * Marca manualment la invulnerabilitat del personatge.
     */
    public void setInvulnerable(boolean invulnerable) {
        stats.setInvulnerable(invulnerable);
    }

    /**
     * Aplica la invulnerabilitat pendent.
     */
    public void applyInvulnerability() {
        stats.applyInvulnerability();
    }

    /**
     * Arrodoneix un valor a dues xifres decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}
