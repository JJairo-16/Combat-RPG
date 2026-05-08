package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import rpgcombat.combat.models.Action;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.impl.elemental.BurnEffect;
import rpgcombat.models.effects.impl.elemental.ChilledEffect;
import rpgcombat.models.effects.impl.elemental.FrozenEffect;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Proves de mecàniques específiques de les armes desbloquejables noves.
 *
 * <p>
 * Aquest test no només comprova que les armes carreguen: valida una propietat
 * mecànica observable de cada arma nova, inclosa la Dualitat elemental.
 * </p>
 */
class UnlockableWeaponMechanicsTest {

    private static final List<String> NEW_WEAPON_IDS = List.of(
            "ELEMENTAL_DUALITY",
            "FIRST_OATH_SPEAR",
            "FIRST_BLOOD_KNIFE",
            "BROKEN_SHIELD",
            "ANCESTRAL_BELL",
            "CHAOS_FRAGMENT",
            "TACTICAL_MIRROR",
            "RETALIATION_SHORTBOW",
            "BAD_OMEN_SLING",
            "COLD_STRING_CROSSBOW");

    @BeforeAll
    static void loadResources() throws Exception {
        TestCombatBalance.init();
        Arsenal.preload(Path.of("data/weapons.json"));
    }

    @Test
    void everyNewWeaponShouldBeCreatableAndUsable() {
        for (String weaponId : NEW_WEAPON_IDS) {
            Character attacker = versatileCharacter("Tester-" + weaponId);
            Weapon weapon = Arsenal.create(weaponId);

            assertNotNull(weapon, "No s'ha pogut crear l'arma " + weaponId);
            assertTrue(weapon.canEquip(attacker.getStatistics()), "El personatge de prova hauria de poder equipar " + weaponId);

            AttackResult result = weapon.attack(attacker.getStatistics(), stableNonCritRandom());
            assertNotNull(result, "L'atac no hauria de retornar null per a " + weaponId);
            assertFalse(result.failed(), "L'arma no hauria de fallar amb recursos suficients: " + weaponId);
            assertTrue(result.damage() >= 0.0, "El dany no pot ser negatiu: " + weaponId);
        }
    }

    @Test
    void elementalDualityShouldAlternateModeAndApplyMatchingEffects() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("ELEMENTAL_DUALITY");

        AttackResult fireAttack = weapon.attack(attacker.getStatistics(), stableNonCritRandom());
        assertEquals("FIRE", fireAttack.meta("elementalMode"));
        assertEquals("FROST", fireAttack.meta("elementalNextMode"));
        assertEquals("FROST", weapon.getMeta("elementalDuality.nextMode"));

        HitContext fireCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        fireCtx.setAttackResult(fireAttack);
        fireCtx.setDamageDealt(25.0);
        weapon.triggerPhase(fireCtx, new ZeroRandom(), Phase.AFTER_HIT);

        assertTrue(defender.hasEffect(BurnEffect.INTERNAL_EFFECT_KEY), "El mode FIRE hauria d'aplicar cremada amb tirada garantida.");
        assertEquals(BurnEffect.INTERNAL_EFFECT_KEY, fireCtx.getMeta("elementalEffect"));
        assertEquals(true, fireCtx.getMeta("elementalBurnApplied"));

        Character frostDefender = versatileCharacter("C");
        AttackResult frostAttack = weapon.attack(attacker.getStatistics(), stableNonCritRandom());
        assertEquals("FROST", frostAttack.meta("elementalMode"));
        assertEquals("FIRE", frostAttack.meta("elementalNextMode"));
        assertEquals("FIRE", weapon.getMeta("elementalDuality.nextMode"));

        HitContext frostCtx = hitContext(attacker, frostDefender, weapon, Action.ATTACK, Action.ATTACK);
        frostCtx.setAttackResult(frostAttack);
        frostCtx.setDamageDealt(25.0);
        weapon.triggerPhase(frostCtx, new ZeroRandom(), Phase.AFTER_HIT);

        assertTrue(frostDefender.hasEffect(FrozenEffect.INTERNAL_EFFECT_KEY), "El mode FROST hauria d'aplicar congelació amb tirada garantida.");
        assertEquals(FrozenEffect.INTERNAL_EFFECT_KEY, frostCtx.getMeta("elementalEffect"));
        assertEquals(true, frostCtx.getMeta("elementalFrozenApplied"));
    }

    @Test
    void firstOathSpearShouldOnlyGainDamageWhenBothCombatantsAttack() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("FIRST_OATH_SPEAR");

        HitContext clashCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        clashCtx.setBaseDamage(100.0);
        weapon.triggerPhase(clashCtx, stableNonCritRandom(), Phase.MODIFY_DAMAGE);

        assertEquals(108.0, clashCtx.damageToResolve(), 0.001);
        assertEquals(true, clashCtx.getMeta("firstOathClash"));

        HitContext safeCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.DEFEND);
        safeCtx.setBaseDamage(100.0);
        weapon.triggerPhase(safeCtx, stableNonCritRandom(), Phase.MODIFY_DAMAGE);

        assertEquals(100.0, safeCtx.damageToResolve(), 0.001);
        assertEquals(null, safeCtx.getMeta("firstOathClash"));
    }

    @Test
    void firstBloodKnifeShouldFavorFrequentSmallCriticals() {
        Character attacker = versatileCharacter("A");
        Weapon knife = Arsenal.create("FIRST_BLOOD_KNIFE");
        Weapon scimitar = Arsenal.create("SCIMITAR");

        assertTrue(
                knife.resolveCriticalChance(attacker.getStatistics()) > scimitar.resolveCriticalChance(attacker.getStatistics()),
                "El ganivet hauria de tenir més probabilitat crítica que una arma física estable com la cimitarra.");
        assertTrue(
                knife.getCriticalDamage() < scimitar.getCriticalDamage(),
                "El ganivet hauria de compensar la freqüència crítica amb menys multiplicador crític.");

        AttackResult critResult = knife.attack(attacker.getStatistics(), new SequenceRandom(0.5, 0.5, 0.0));
        assertTrue(knife.lastWasCritic(), "La tirada controlada hauria de forçar un crític.");
        assertTrue(critResult.damage() > knife.lastNonCriticalDamage(), "El crític hauria d'augmentar el dany del cop base.");
    }

    @Test
    void brokenShieldShouldRewardExistingGuardStacks() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("BROKEN_SHIELD");

        HitContext withoutGuard = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        withoutGuard.setBaseDamage(100.0);
        weapon.triggerPhase(withoutGuard, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(100.0, withoutGuard.damageToResolve(), 0.001);
        assertEquals(null, withoutGuard.getMeta("guardCounter"));

        attacker.increaseGuardStacks();
        HitContext withGuard = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        withGuard.setBaseDamage(100.0);
        weapon.triggerPhase(withGuard, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(110.0, withGuard.damageToResolve(), 0.001);
        assertEquals(true, withGuard.getMeta("guardCounter"));
    }

    @Test
    void ancestralBellShouldOnlyBoostAndHealWhenDesperateAndRespectCooldown() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("ANCESTRAL_BELL");

        HitContext healthyCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        healthyCtx.setBaseDamage(100.0);
        weapon.triggerPhase(healthyCtx, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(100.0, healthyCtx.damageToResolve(), 0.001);
        assertEquals(null, healthyCtx.getMeta("ancestralBellDesperate"));

        attacker.getStatistics().damage(attacker.getStatistics().getMaxHealth() * 0.90);
        double beforeHeal = attacker.getStatistics().getHealth();

        HitContext desperateCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        desperateCtx.setBaseDamage(100.0);
        weapon.triggerPhase(desperateCtx, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(108.0, desperateCtx.damageToResolve(), 0.001);
        assertEquals(true, desperateCtx.getMeta("ancestralBellDesperate"));

        desperateCtx.setDamageDealt(40.0);
        weapon.triggerPhase(desperateCtx, stableNonCritRandom(), Phase.AFTER_HIT);
        assertEquals(beforeHeal + 16.0, attacker.getStatistics().getHealth(), 0.001);
        assertEquals(16.0, desperateCtx.getMeta("ancestralBellHeal", Double.class, 0.0), 0.001);
        assertEquals(3, getWeaponMeta(weapon, "ancestralBell.cooldown", 0));

        double afterFirstHeal = attacker.getStatistics().getHealth();
        HitContext cooldownCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        cooldownCtx.setDamageDealt(40.0);
        weapon.triggerPhase(cooldownCtx, stableNonCritRandom(), Phase.AFTER_HIT);
        assertEquals(afterFirstHeal, attacker.getStatistics().getHealth(), 0.001);
        assertEquals(2, getWeaponMeta(weapon, "ancestralBell.cooldown", 0));
    }

    @Test
    void chaosFragmentShouldExposeLowStableAndHighBranches() {
        Character lowCaster = versatileCharacter("Low");
        Character stableCaster = versatileCharacter("Stable");
        Character highCaster = versatileCharacter("High");

        Weapon lowWeapon = Arsenal.create("CHAOS_FRAGMENT");
        AttackResult low = lowWeapon.attack(lowCaster.getStatistics(), new SequenceRandom(0.5, 0.5, 1.0, 0.0));
        assertEquals(0.92, low.numericMeta("chaosFragmentMultiplier"), 0.001);
        assertTrue(low.booleanMeta("chaosFragmentLow"));
        assertFalse(low.booleanMeta("chaosFragmentHigh"));
        assertEquals(round2(lowWeapon.lastNonCriticalDamage() * 0.92), low.damage(), 0.001);

        Weapon stableWeapon = Arsenal.create("CHAOS_FRAGMENT");
        AttackResult stable = stableWeapon.attack(stableCaster.getStatistics(), new SequenceRandom(0.5, 0.5, 1.0, 0.5));
        assertEquals(1.0, stable.numericMeta("chaosFragmentMultiplier"), 0.001);
        assertFalse(stable.booleanMeta("chaosFragmentLow"));
        assertFalse(stable.booleanMeta("chaosFragmentHigh"));
        assertEquals(stableWeapon.lastNonCriticalDamage(), stable.damage(), 0.001);

        Weapon highWeapon = Arsenal.create("CHAOS_FRAGMENT");
        AttackResult high = highWeapon.attack(highCaster.getStatistics(), new SequenceRandom(0.5, 0.5, 1.0, 0.9));
        assertEquals(1.08, high.numericMeta("chaosFragmentMultiplier"), 0.001);
        assertFalse(high.booleanMeta("chaosFragmentLow"));
        assertTrue(high.booleanMeta("chaosFragmentHigh"));
        assertEquals(round2(highWeapon.lastNonCriticalDamage() * 1.08), high.damage(), 0.001);
    }

    @Test
    void tacticalMirrorShouldPunishRepeatedDefenderActionsOnly() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("TACTICAL_MIRROR");

        HitContext firstRead = hitContext(attacker, defender, weapon, Action.ATTACK, Action.DEFEND);
        firstRead.setBaseDamage(100.0);
        weapon.triggerPhase(firstRead, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(100.0, firstRead.damageToResolve(), 0.001);
        assertEquals(null, firstRead.getMeta("tacticalMirrorRead"));

        HitContext repeated = hitContext(attacker, defender, weapon, Action.ATTACK, Action.DEFEND);
        repeated.setBaseDamage(100.0);
        weapon.triggerPhase(repeated, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(110.0, repeated.damageToResolve(), 0.001);
        assertEquals(true, repeated.getMeta("tacticalMirrorRead"));

        HitContext changed = hitContext(attacker, defender, weapon, Action.ATTACK, Action.DODGE);
        changed.setBaseDamage(100.0);
        weapon.triggerPhase(changed, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(100.0, changed.damageToResolve(), 0.001);
        assertEquals(null, changed.getMeta("tacticalMirrorRead"));
    }

    @Test
    void retaliationShortbowShouldPunishDefendAndNothingElse() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("RETALIATION_SHORTBOW");

        HitContext againstDefend = hitContext(attacker, defender, weapon, Action.ATTACK, Action.DEFEND);
        againstDefend.setBaseDamage(100.0);
        weapon.triggerPhase(againstDefend, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(110.0, againstDefend.damageToResolve(), 0.001);
        assertEquals(true, againstDefend.getMeta("retaliationAgainstDefend"));

        HitContext againstAttack = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        againstAttack.setBaseDamage(100.0);
        weapon.triggerPhase(againstAttack, stableNonCritRandom(), Phase.MODIFY_DAMAGE);
        assertEquals(100.0, againstAttack.damageToResolve(), 0.001);
        assertEquals(null, againstAttack.getMeta("retaliationAgainstDefend"));
    }

    @Test
    void badOmenSlingShouldBuildCritChanceAfterNonCriticalTurnsAndResetOnCritical() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("BAD_OMEN_SLING");

        HitContext firstMissedCrit = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        firstMissedCrit.setCriticalChance(0.19);
        firstMissedCrit.resolveCritical();
        assertFalse(firstMissedCrit.wasCritical());
        weapon.triggerPhase(firstMissedCrit, stableNonCritRandom(), Phase.END_TURN);
        assertEquals(1, getWeaponMeta(weapon, "badOmen.stacks", 0));
        assertEquals(1, (int) firstMissedCrit.getMeta("badOmenStacks", Integer.class, 0));

        HitContext secondMissedCrit = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        secondMissedCrit.setCriticalChance(0.19);
        secondMissedCrit.resolveCritical();
        weapon.triggerPhase(secondMissedCrit, stableNonCritRandom(), Phase.END_TURN);
        assertEquals(2, getWeaponMeta(weapon, "badOmen.stacks", 0));

        HitContext bonusCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        bonusCtx.setCriticalChance(0.19);
        weapon.triggerPhase(bonusCtx, stableNonCritRandom(), Phase.ROLL_CRIT);
        assertEquals(0.25, bonusCtx.criticalChance(), 0.001);
        assertEquals(0.06, bonusCtx.getMeta("badOmenCritBonus", Double.class, 0.0), 0.001);

        HitContext criticalCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        criticalCtx.setCriticalChance(1.0);
        criticalCtx.resolveCritical();
        assertTrue(criticalCtx.wasCritical());
        weapon.triggerPhase(criticalCtx, stableNonCritRandom(), Phase.END_TURN);
        assertEquals(0, getWeaponMeta(weapon, "badOmen.stacks", -1));
        assertEquals(true, criticalCtx.getMeta("badOmenReset"));
    }

    @Test
    void coldStringCrossbowShouldApplyChilledAndChilledShouldReduceOutgoingDamage() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("COLD_STRING_CROSSBOW");

        HitContext applyCtx = hitContext(attacker, defender, weapon, Action.ATTACK, Action.ATTACK);
        applyCtx.setDamageDealt(20.0);
        weapon.triggerPhase(applyCtx, new ZeroRandom(), Phase.AFTER_HIT);

        assertTrue(defender.hasEffect(ChilledEffect.INTERNAL_EFFECT_KEY), "La ballesta hauria d'aplicar Fred menor amb tirada garantida.");
        assertEquals(true, applyCtx.getMeta("chilledApplied"));

        HitContext chilledAttackCtx = hitContext(defender, attacker, weapon, Action.ATTACK, Action.ATTACK);
        chilledAttackCtx.setBaseDamage(100.0);
        defender.triggerEffects(chilledAttackCtx, Phase.MODIFY_DAMAGE, stableNonCritRandom());

        assertEquals(94.0, chilledAttackCtx.damageToResolve(), 0.001);
    }

    private static HitContext hitContext(Character attacker, Character defender, Weapon weapon, Action attackerAction, Action defenderAction) {
        return new HitContext(attacker, defender, weapon, stableNonCritRandom(), attackerAction, defenderAction);
    }

    private static Character versatileCharacter(String name) {
        return new Character(name, 30, new int[] { 20, 20, 20, 20, 20, 20, 20 }, Breed.HUMAN);
    }

    private static Random stableNonCritRandom() {
        return new SequenceRandom(0.5, 0.5, 1.0, 1.0, 1.0, 1.0);
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    /** Random determinista que devuelve una secuencia fija para nextDouble(). */
    private static final class SequenceRandom extends Random {
        private final double[] values;
        private int index = 0;

        SequenceRandom(double... values) {
            this.values = values == null || values.length == 0 ? new double[] { 0.5 } : values.clone();
        }

        @Override
        public double nextDouble() {
            double value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }
    }

    /** Random que garantiza probabilidades positivas cuando se usa como tirada de aplicación. */
    private static final class ZeroRandom extends Random {
        @Override
        public double nextDouble() {
            return 0.0;
        }
    }

    private static int getWeaponMeta(Weapon weapon, String key, int def) {
        return (int) weapon.getMeta(key, Integer.class, def);
    }
}
