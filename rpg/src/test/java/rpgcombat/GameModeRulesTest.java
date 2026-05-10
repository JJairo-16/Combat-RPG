package rpgcombat;

import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import menu.model.MenuResult;
import rpgcombat.combat.CombatSystem;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.turnservice.DefaultTurnPriorityPolicy;
import rpgcombat.game.menu.MenuCenter;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.gamemode.chaos.ChaosRules;
import rpgcombat.gamemode.cinematics.ModeCinematics;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.io.GameModeLoader;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.gamemode.effects.ModeEffectApplier;
import rpgcombat.gamemode.effects.ModeEffectDefinition;
import rpgcombat.gamemode.effects.ModeEffectTarget;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.triggers.BleedEmphasisTrigger;
import rpgcombat.models.effects.triggers.SelfDirectedAttackTrigger;
import rpgcombat.unlocks.UnlockMode;
import rpgcombat.unlocks.UnlockRequirement;
import rpgcombat.unlocks.UnlockRequirementType;
import rpgcombat.unlocks.UnlockRule;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.passives.HitContext;

class GameModeRulesTest {

    @BeforeAll
    static void init() throws Exception {
        TestCombatBalance.init();
        Arsenal.preload(Path.of("data/weapons.json"));
    }

    @Test
    void unrestrictedRulesAllowEveryAction() {
        GameModeRules rules = GameModeRules.unrestricted();

        assertFalse(rules.hasActionRestrictions());
        for (Action action : Action.values()) {
            assertTrue(rules.allowsAction(action));
        }
    }

    @Test
    void gameModeJsonCanOmitAllowedActionsForNoRestrictions() throws Exception {
        GameModeDefinition normal = GameModeLoader.load(Path.of("data/gameModes.json")).stream()
                .filter(mode -> mode.id().equals("NORMAL"))
                .findFirst()
                .orElseThrow();

        assertFalse(normal.rules().hasActionRestrictions());
        for (Action action : Action.values()) {
            assertTrue(normal.rules().allowsAction(action));
        }
    }

    @Test
    void modeUnlockRulesCanDependOnAchievementsAndDiscoveries() {
        UnlockRule achievementRule = new UnlockRule(UnlockMode.ALL,
                List.of(new UnlockRequirement(UnlockRequirementType.ACHIEVEMENT, "WIN_FIRST_MATCH", null, 0)));
        UnlockRule discoveryRule = new UnlockRule(UnlockMode.ALL,
                List.of(new UnlockRequirement(UnlockRequirementType.DISCOVERY, "CHAOS_FRAGMENT", "WEAPONS", 0)));

        assertFalse(newMode("ACHIEVEMENT_MODE", achievementRule).isUnlocked(null, null));
        assertFalse(newMode("DISCOVERY_MODE", discoveryRule).isUnlocked(null, null));
        assertTrue(newMode("OPEN_MODE", new UnlockRule(UnlockMode.ALL, List.of())).isUnlocked(null, null));
    }

    @Test
    void beginnerRulesDisableChargeSpecialsUnlockablesAndChaos() {
        GameModeRules rules = GameModeRules.beginner();

        assertTrue(rules.allowsAction(Action.ATTACK));
        assertTrue(rules.allowsAction(Action.DEFEND));
        assertTrue(rules.allowsAction(Action.DODGE));
        assertFalse(rules.allowsAction(Action.CHARGE));
        assertFalse(rules.specialActionsEnabled());
        assertEquals(1, rules.maxPerks());
        assertFalse(rules.divinePerksEnabled());
        assertFalse(rules.showUnlockableWeapons());
        assertFalse(rules.chaos().shouldActivate(new Random(0)));
    }

    @Test
    void beginnerMenuOmitsChargeAndDynamicSpecialOptions() {
        Character player1 = character("P1");
        Character player2 = character("P2");
        player1.addEffect(new TestEffect("SPECIAL", 3, 1, 1));

        Map<String, List<StatusMod>> modifiers = Map.of("SPECIAL", List.of(
                new StatusMod(10, "Mode especial", "special", p -> MenuResult.repeatLoop(), p -> true)));

        MenuCenter center = new MenuCenter(player1, player2, p -> {
        }, p -> {
        }, modifiers, Map.of(), GameModeRules.beginner());

        assertTrue(center.getMenu1().hasOption(Action.ATTACK.label()));
        assertTrue(center.getMenu1().hasOption(Action.DEFEND.label()));
        assertTrue(center.getMenu1().hasOption(Action.DODGE.label()));
        assertFalse(center.getMenu1().hasOption(Action.CHARGE.label()));
        assertFalse(center.getMenu1().hasOption("Mode especial"));
    }

    @Test
    void beginnerArsenalHidesUnlockableWeaponsEvenIfCatalogContainsThem() {
        List<WeaponDefinition> beginnerWeapons = Arsenal.availableValues(GameModeRules.beginner());

        assertFalse(beginnerWeapons.isEmpty());
        assertTrue(beginnerWeapons.stream().allMatch(definition -> definition.getUnlockRule() == null
                || definition.getUnlockRule().openByDefault()));
        assertTrue(Arsenal.values().stream().anyMatch(definition -> definition.getUnlockRule() != null
                && !definition.getUnlockRule().openByDefault()));
    }

    @Test
    void combatSystemRejectsDisallowedBeginnerActions() {
        CombatSystem combat = new CombatSystem(
                character("P1"),
                character("P2"),
                new DefaultTurnPriorityPolicy(),
                null,
                null,
                GameModeRules.beginner());

        assertThrows(IllegalArgumentException.class, () -> combat.playRound(Action.CHARGE, Action.ATTACK));
    }

    @Test
    void modeEffectsAttachFreshTriggersToPlayers() {
        Character player1 = character("P1");
        Character player2 = character("P2");
        GameModeRules rules = new GameModeRules(
                Set.of(),
                true,
                4,
                true,
                true,
                ChaosRules.disabled(),
                List.of(new ModeEffectDefinition("BLEED_EMPHASIS", ModeEffectTarget.BOTH, Map.of())));

        ModeEffectApplier.apply(rules, player1, player2);

        assertTrue(player1.hasEffect(BleedEmphasisTrigger.INTERNAL_EFFECT_KEY));
        assertTrue(player2.hasEffect(BleedEmphasisTrigger.INTERNAL_EFFECT_KEY));
        assertNotSame(player1.getEffect(BleedEmphasisTrigger.INTERNAL_EFFECT_KEY),
                player2.getEffect(BleedEmphasisTrigger.INTERNAL_EFFECT_KEY));

    }

    @Test
    void bleedEmphasisTriggerCanChangeCombatThroughTheEffectPipeline() {
        Character attacker = character("Attacker");
        Character defender = character("Defender");
        defender.applyBleed(1);

        Effect effect = new BleedEmphasisTrigger(1.20, 3, 2);
        HitContext ctx = new HitContext(attacker, defender, null, new Random(0), Action.ATTACK, Action.ATTACK);
        ctx.setBaseDamage(100.0);

        effect.modifyDamage(ctx, new Random(0), attacker);

        assertEquals(120.0, ctx.damageToResolve());
    }

    @Test
    void selfDirectedAttackModeRedirectsAttackDamageToTheAttacker() {
        Character attacker = character("Attacker");
        Character defender = character("Defender");
        GameModeRules rules = new GameModeRules(
                Set.of(),
                true,
                4,
                true,
                true,
                ChaosRules.disabled(),
                List.of(new ModeEffectDefinition(
                        SelfDirectedAttackTrigger.INTERNAL_EFFECT_KEY,
                        ModeEffectTarget.PLAYER1,
                        Map.of("damageMultiplier", 1.0, "canKill", 1.0))));

        ModeEffectApplier.apply(rules, attacker, defender);
        attacker.setWeapon(Arsenal.availableValues().getFirst().create());
        CombatSystem combat = new CombatSystem(attacker, defender, (p1, a1, p2, a2, rng) -> true, null, null, rules);

        double attackerHp = attacker.getStatistics().getHealth();
        double defenderHp = defender.getStatistics().getHealth();

        combat.playRound(Action.ATTACK, Action.DEFEND);

        assertTrue(attacker.getStatistics().getHealth() < attackerHp);
        assertEquals(defenderHp, defender.getStatistics().getHealth());
    }

    private static GameModeDefinition newMode(String id, UnlockRule rule) {
        return new GameModeDefinition(
                id,
                id,
                "",
                rule,
                GameModeRules.unrestricted(),
                ModeCinematics.normal());
    }

    private static Character character(String name) {
        return new Character(name, 30, new int[] { 20, 20, 20, 20, 20, 20, 20 }, Breed.HUMAN);
    }

    private static final class TestEffect implements Effect {
        private final String key;
        private final EffectState state;

        TestEffect(String key, int remainingTurns, int stacks, int charges) {
            this.key = key;
            this.state = new EffectState(charges, stacks, remainingTurns, 0);
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public EffectState state() {
            return state;
        }
    }
}
