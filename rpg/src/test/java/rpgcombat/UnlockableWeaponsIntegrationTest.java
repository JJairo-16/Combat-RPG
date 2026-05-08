package rpgcombat;

import static org.junit.Assert.assertEquals;
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
import rpgcombat.models.effects.impl.elemental.ChilledEffect;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

class UnlockableWeaponsIntegrationTest {

    private static final List<String> NEW_WEAPON_IDS = List.of(
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
    void shouldCreateAndAttackWithEveryNewWeapon() {
        Character attacker = versatileCharacter("Tester");

        for (String weaponId : NEW_WEAPON_IDS) {
            Weapon weapon = Arsenal.create(weaponId);
            assertNotNull(weapon, "No s'ha pogut crear l'arma " + weaponId);
            assertTrue(weapon.canEquip(attacker.getStatistics()), "El personatge de prova hauria de poder equipar " + weaponId);

            AttackResult result = weapon.attack(attacker.getStatistics(), new Random(7));
            assertNotNull(result, "L'atac no hauria de retornar null per a " + weaponId);
            assertFalse(result.failed(), "L'arma no hauria de fallar en una prova amb recursos suficients: " + weaponId);
            assertTrue(result.damage() >= 0, "El dany no pot ser negatiu: " + weaponId);
        }
    }

    @Test
    void firstOathSpearShouldGainDamageWhenBothAttack() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("FIRST_OATH_SPEAR");

        HitContext ctx = new HitContext(attacker, defender, weapon, new Random(1), Action.ATTACK, Action.ATTACK);
        ctx.setBaseDamage(100);

        weapon.triggerPhase(ctx, new Random(1), Phase.MODIFY_DAMAGE);

        assertTrue(ctx.damageToResolve() > 100, "La llança hauria de guanyar dany quan tots dos ataquen.");
        assertEquals("La metadata del bonus hauria de quedar marcada.", true, ctx.getMeta("firstOathClash"));
    }

    @Test
    void coldStringCrossbowShouldApplyChilledWithGuaranteedRoll() {
        Character attacker = versatileCharacter("A");
        Character defender = versatileCharacter("B");
        Weapon weapon = Arsenal.create("COLD_STRING_CROSSBOW");

        HitContext ctx = new HitContext(attacker, defender, weapon, new ZeroRandom(), Action.ATTACK, Action.ATTACK);
        ctx.setDamageDealt(1);

        weapon.triggerPhase(ctx, new ZeroRandom(), Phase.AFTER_HIT);

        assertTrue(defender.hasEffect(ChilledEffect.INTERNAL_EFFECT_KEY), "La ballesta freda hauria d'aplicar Fred menor.");
        assertEquals("La metadata de fred menor hauria de quedar marcada.", true, ctx.getMeta("chilledApplied"));
    }

    @Test
    void allNewWeaponDefinitionsShouldHaveUnlockRules() {
        for (String weaponId : NEW_WEAPON_IDS) {
            WeaponDefinition definition = Arsenal.getDefinition(weaponId);
            assertNotNull(definition.getUnlockRule(), "L'arma desbloquejable hauria de declarar unlock: " + weaponId);
            assertFalse(definition.getUnlockRule().requirements().isEmpty(), "L'arma hauria de tenir requisits: " + weaponId);
        }
    }

    private static Character versatileCharacter(String name) {
        return new Character(name, 30, new int[] { 20, 20, 20, 20, 20, 20, 20 }, Breed.HUMAN);
    }

    private static final class ZeroRandom extends Random {
        @Override
        public double nextDouble() {
            return 0.0;
        }
    }
}
