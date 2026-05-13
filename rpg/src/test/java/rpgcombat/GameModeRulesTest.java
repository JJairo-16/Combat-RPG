package rpgcombat;

import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import rpgcombat.achievements.AchievementEvent;
import rpgcombat.achievements.AchievementProgress;
import rpgcombat.achievements.AchievementUpdate;
import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementLoader;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.achievements.config.AchievementVisibility;
import rpgcombat.combat.CombatSystem;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.DefaultTurnPriorityPolicy;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.game.menu.MenuCenter;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.config.DiscoveryCatalog;
import rpgcombat.gamemode.chaos.ChaosRules;
import rpgcombat.gamemode.cinematics.ModeCinematics;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.io.GameModeLoader;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.gamemode.effects.ModeEffectApplier;
import rpgcombat.gamemode.effects.ModeEffectDefinition;
import rpgcombat.gamemode.effects.ModeEffectTarget;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.breeds.Dwarf;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.triggers.BleedEmphasisTrigger;
import rpgcombat.models.effects.triggers.gamemode.FragmentedFaceTrigger;
import rpgcombat.models.effects.triggers.gamemode.SelfDirectedAttackTrigger;
import rpgcombat.models.effects.triggers.gamemode.UniversalLifeStealTrigger;
import rpgcombat.unlocks.UnlockMode;
import rpgcombat.unlocks.UnlockRequirement;
import rpgcombat.unlocks.UnlockRequirementType;
import rpgcombat.unlocks.UnlockRule;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.config.WeaponType;
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
    void gameModeJsonLoadsPresentationCardTexts() throws Exception {
        GameModeDefinition beginner = GameModeLoader.load(Path.of("data/gameModes.json")).stream()
                .filter(mode -> mode.id().equals("BEGINNER"))
                .findFirst()
                .orElseThrow();

        assertEquals("El primer llindar.", beginner.presentation().shortDescription());
        assertTrue(beginner.presentation().details().contains("La càrrega roman segellada"));
        assertTrue(beginner.presentation().details().contains("Cap pacte diví desperta en aquest llindar"));
        assertTrue(beginner.presentation().details().size() >= 5);
        assertEquals("???", beginner.presentation().lockedTitle());
        assertFalse(beginner.presentation().lockedHints().isEmpty());
    }

    @Test
    void bloodHungerModeLoadsUnlocksEffectsAndNormalCinematics() throws Exception {
        GameModeDefinition mode = GameModeLoader.load(Path.of("data/gameModes.json")).stream()
                .filter(gameMode -> gameMode.id().equals("BLOOD_HUNGER"))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of("DEFAULT_RANDOM"), mode.cinematics().postCreationPool());
        assertEquals("CHAOS_MIND", mode.cinematics().chaosPostCreation());
        assertEquals(List.of("ETERNAL_HUNGER", "FLESH_BENDS"), mode.unlockRule().requirements().stream()
                .map(UnlockRequirement::id)
                .toList());
        assertTrue(mode.rules().modeEffects().stream()
                .anyMatch(effect -> effect.id().equals(UniversalLifeStealTrigger.INTERNAL_EFFECT_KEY)));
    }

    @Test
    void discoveryCatalogIncludesGameModeEntries() {
        DiscoveryCatalog catalog = DiscoveryCatalog.build(null);

        assertEquals("Modes de joc", catalog.categories().getFirst().title());
        assertTrue(catalog.contains(DiscoveryCategory.GAME_MODES, "NORMAL"));
        assertTrue(catalog.contains(DiscoveryCategory.GAME_MODES, "BEGINNER"));
        assertFalse(catalog.entries(DiscoveryCategory.GAME_MODES).isEmpty());
        var beginner = catalog.find(DiscoveryCategory.GAME_MODES, "BEGINNER").orElseThrow();
        assertTrue(beginner.shortDescription().contains("sense càrrega"));
        assertTrue(beginner.description().contains("Aquest camí estreny el combat fins als gestos essencials."));
        assertTrue(beginner.description().contains("Els pactes divins i el Caos resten fora d'aquest llindar."));
        assertFalse(beginner.description().stream().anyMatch(line -> line.startsWith("Accions:")));
        assertFalse(beginner.description().stream().anyMatch(line -> line.startsWith("Perks")));
        assertFalse(beginner.description().contains("La càrrega roman segellada"));
        var bloodHunger = catalog.find(DiscoveryCategory.GAME_MODES, "BLOOD_HUNGER").orElseThrow();
        assertTrue(bloodHunger.description().contains(
                "La vida ja no torna per costum; només respon a la ferida oberta."));
    }

    @Test
    void beginnerModeAchievementCompletesFromModeSelectionEvent() throws Exception {
        var definition = AchievementLoader.load(Path.of("data/achievements.json")).stream()
                .filter(achievement -> achievement.id().equals("FIRST_THRESHOLD"))
                .findFirst()
                .orElseThrow();

        assertEquals(AchievementEvent.GAME_MODE_SELECTED, definition.objective().event());
        AchievementProgress progress = new AchievementProgress(definition);

        assertFalse(progress.update(AchievementUpdate.gameModeSelected("NORMAL")));
        assertFalse(progress.completed());
        assertTrue(progress.update(AchievementUpdate.gameModeSelected("BEGINNER")));
        assertTrue(progress.completed());
    }

    @Test
    void consecutiveAchievementsKeepSeparateStreaksPerActor() {
        AchievementProgress progress = new AchievementProgress(testAchievement(
                AchievementObjectiveType.CONSECUTIVE_EVENT,
                AchievementEvent.LIFE_STEAL,
                List.of(),
                2));
        Character player1 = character("P1");
        Character player2 = character("P2");

        progress.update(AchievementUpdate.simple(player1, AchievementEvent.LIFE_STEAL));
        progress.update(AchievementUpdate.simple(player2, AchievementEvent.ACTION_ATTACK));

        assertEquals(1.0, progress.progress());

        progress.update(AchievementUpdate.simple(player1, AchievementEvent.LIFE_STEAL));

        assertTrue(progress.completed());
    }

    @Test
    void lifeStealStreakIgnoresNonComparableUpdatesFromSameActor() {
        AchievementProgress progress = new AchievementProgress(testAchievement(
                AchievementObjectiveType.CONSECUTIVE_EVENT,
                AchievementEvent.LIFE_STEAL,
                List.of(),
                2));
        Character player1 = character("P1");
        Character player2 = character("P2");

        progress.update(AchievementUpdate.simple(player1, AchievementEvent.LIFE_STEAL));
        progress.update(AchievementUpdate.fromMatchFinished(player1, player2, Winner.PLAYER1, 1));

        assertEquals(1.0, progress.progress());

        progress.update(AchievementUpdate.simple(player1, AchievementEvent.LIFE_STEAL));

        assertTrue(progress.completed());
    }

    @Test
    void lifeStealStreakResetsWhenSameActorAttacksWithoutStealing() {
        AchievementProgress progress = new AchievementProgress(testAchievement(
                AchievementObjectiveType.CONSECUTIVE_EVENT,
                AchievementEvent.LIFE_STEAL,
                List.of(),
                2));
        Character player = character("P1");

        progress.update(AchievementUpdate.simple(player, AchievementEvent.LIFE_STEAL));
        progress.update(AchievementUpdate.simple(player, AchievementEvent.ACTION_ATTACK));
        progress.update(AchievementUpdate.simple(player, AchievementEvent.LIFE_STEAL));

        assertFalse(progress.completed());
        assertEquals(1.0, progress.progress());
    }

    @Test
    void eternalHungerCompletesFromConcreteLifeStealTurnResults() throws Exception {
        var definition = AchievementLoader.load(Path.of("data/achievements.json")).stream()
                .filter(achievement -> achievement.id().equals("ETERNAL_HUNGER"))
                .findFirst()
                .orElseThrow();
        AchievementProgress progress = new AchievementProgress(definition);
        Character player1 = character("P1");
        Character player2 = character("P2");

        for (int turn = 1; turn <= 10; turn++) {
            progress.update(AchievementUpdate.fromTurn(player1, player2, Action.ATTACK, Action.DEFEND,
                    lifeStealTurnResult(0.0, true), turn));
            progress.update(AchievementUpdate.fromTurn(player2, player1, Action.ATTACK, Action.DEFEND,
                    attackTurnResult(), turn));
        }

        assertTrue(progress.completed());
        assertEquals(10, progress.viewProgress());
    }

    @Test
    void actionSequenceAchievementsKeepSeparateProgressPerActor() {
        AchievementProgress progress = new AchievementProgress(testAchievement(
                AchievementObjectiveType.ACTION_SEQUENCE,
                null,
                List.of(Action.ATTACK, Action.ATTACK),
                2));
        Character player1 = character("P1");
        Character player2 = character("P2");

        progress.update(AchievementUpdate.fromTurn(player1, player2, Action.ATTACK, Action.DEFEND, null, 1));
        progress.update(AchievementUpdate.fromTurn(player2, player1, Action.DEFEND, Action.ATTACK, null, 1));

        assertEquals(1.0, progress.progress());

        progress.update(AchievementUpdate.fromTurn(player1, player2, Action.ATTACK, Action.DEFEND, null, 2));

        assertTrue(progress.completed());
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
    void universalLifeStealTriggerStealsLimitedLifeAfterAnyHit() {
        Character attacker = character("Attacker");
        Character defender = character("Defender");
        attacker.getStatistics().damage(30.0);
        Effect effect = new UniversalLifeStealTrigger(0.10, 0.05, true);
        HitContext ctx = new HitContext(attacker, defender, testWeapon(), new Random(0), Action.ATTACK, Action.DEFEND);
        ctx.setDamageDealt(100.0);
        double expectedHeal = Math.min(10.0, attacker.getStatistics().getMaxHealth() * 0.05);

        effect.afterHit(ctx, new Random(0), attacker);

        assertEquals(expectedHeal, ctx.getMeta("LIFE_STOLEN"));
        assertEquals(Boolean.TRUE, ctx.getMeta("modeLifeSteal"));
    }

    @Test
    void universalLifeStealModeSuppressesOnlyPassiveHealthRegeneration() {
        Character dwarf = new Dwarf("Dwarf", 40, new int[] { 20, 20, 20, 20, 20, 20, 20 });
        dwarf.getStatistics().damage(30.0);
        dwarf.getStatistics().consumeMana(20.0);
        dwarf.addEffect(new UniversalLifeStealTrigger());

        double healthBefore = dwarf.getStatistics().getHealth();
        double manaBefore = dwarf.getStatistics().getMana();

        dwarf.regen();

        assertEquals(healthBefore, dwarf.getStatistics().getHealth());
        assertTrue(dwarf.getStatistics().getMana() > manaBefore);
    }

    @Test
    void fragmentedFaceNonLethalDamageDoesNotCrashBelowOneHealth() {
        Character player = character("Player");
        player.getStatistics().damage(player.getStatistics().getHealth() - 0.5);
        FragmentedFaceTrigger trigger = new FragmentedFaceTrigger();

        assertDoesNotThrow(() -> trigger.onRoundStart(player, 1, new FixedIntRandom(
                FragmentedFaceTrigger.Buff.EDGE_OF_GLASS.ordinal(),
                FragmentedFaceTrigger.Debuff.COLD_PULSE.ordinal()), null));
        assertEquals(0.5, player.getStatistics().getHealth(), 0.001);
    }

    @Test
    void fragmentedFaceRestoresClearedRoundOnlyBleed() {
        Character player = character("Player");
        player.applyBleed(2);
        FragmentedFaceTrigger trigger = new FragmentedFaceTrigger();

        trigger.onRoundStart(player, 1, new FixedIntRandom(
                FragmentedFaceTrigger.Buff.SUTURED_MARK.ordinal(),
                FragmentedFaceTrigger.Debuff.DULLED_EDGE.ordinal()), null);

        assertFalse(player.isBleeding());

        trigger.onRoundEnd(player);

        assertTrue(player.isBleeding());
        assertEquals(2, player.bleedTurnsRemaining());
    }

    @Test
    void fragmentedFaceRemovesItsOwnRoundOnlyBleed() {
        Character player = character("Player");
        FragmentedFaceTrigger trigger = new FragmentedFaceTrigger();

        trigger.onRoundStart(player, 1, new FixedIntRandom(
                FragmentedFaceTrigger.Buff.EDGE_OF_GLASS.ordinal(),
                FragmentedFaceTrigger.Debuff.OPEN_MARK.ordinal()), null);

        assertTrue(player.isBleeding());

        trigger.onRoundEnd(player);

        assertFalse(player.isBleeding());
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
        Effect effect = attacker.getEffect(SelfDirectedAttackTrigger.INTERNAL_EFFECT_KEY);
        HitContext ctx = new HitContext(attacker, defender, testWeapon(), new Random(0), Action.ATTACK, Action.DEFEND);

        effect.modifyDamage(ctx, new Random(0), attacker);

        assertEquals(Boolean.TRUE, ctx.getMeta("selfDirectedAttack"));
        assertEquals(1.0, ctx.getMeta("selfDirectedAttackMultiplier"));
        assertEquals(Boolean.TRUE, ctx.getMeta("selfDirectedAttackCanKill"));
    }

    private static GameModeDefinition newMode(String id, UnlockRule rule) {
        return new GameModeDefinition(
                id,
                id,
                "",
                rule,
                GameModeRules.unrestricted(),
                null,
                ModeCinematics.normal());
    }

    private static AchievementDefinition testAchievement(
            AchievementObjectiveType type,
            AchievementEvent event,
            List<Action> sequence,
            int target) {
        return new AchievementDefinition(
                "TEST_" + type.name(),
                "Prova",
                "Prova",
                target,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        type,
                        event,
                        null,
                        null,
                        sequence,
                        target,
                        0));
    }

    private static Character character(String name) {
        return new Character(name, 30, new int[] { 20, 20, 20, 20, 20, 20, 20 }, Breed.HUMAN);
    }

    private static TurnResult lifeStealTurnResult(double lifeStolen, boolean triggered) {
        return new TurnResult(
                "P1",
                "ataca.",
                List.of(),
                List.of(),
                "rep el cop.",
                List.of(),
                List.of(),
                10.0,
                false,
                false,
                false,
                false,
                null,
                10.0,
                0.0,
                lifeStolen,
                "TEST_MODE_WEAPON",
                "Arma de prova",
                Map.of("lifeStealTriggered", triggered));
    }

    private static TurnResult attackTurnResult() {
        return new TurnResult(
                "P2",
                "ataca.",
                List.of(),
                List.of(),
                "rep el cop.",
                List.of(),
                List.of(),
                10.0,
                false,
                false,
                false,
                false,
                null,
                10.0,
                0.0,
                0.0,
                "TEST_MODE_WEAPON",
                "Arma de prova",
                Map.of());
    }

    private static Weapon testWeapon() {
        return new Weapon(
                "TEST_MODE_WEAPON",
                "Arma de prova",
                "Arma estable per provar modes de joc.",
                100,
                0.0,
                1.0,
                WeaponType.PHYSICAL,
                (weapon, stats, rng) -> new AttackResult(100.0, "colpeja."),
                0.0,
                List.of());
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

    private static final class FixedIntRandom extends Random {
        private final int[] values;
        private int index;

        FixedIntRandom(int... values) {
            this.values = values == null || values.length == 0 ? new int[] { 0 } : values.clone();
        }

        @Override
        public int nextInt(int bound) {
            int value = values[Math.min(index, values.length - 1)];
            index++;
            return Math.floorMod(value, bound);
        }
    }
}
