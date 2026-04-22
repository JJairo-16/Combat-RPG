package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rpgcombat.combat.CombatSystem;
import rpgcombat.combat.models.Action;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.impl.SuddenDeathPoisonEffect;

/**
 * Proves de la mecànica anti-stall:
 * si el combat supera 15 torns, s'aplica un verí global
 * que augmenta la seva potència cada 2 torns.
 *
 * Aquest test assumeix que els efectes de final de torn (END_TURN)
 * també es processen en torns no ofensius com DEFEND.
 */
class SuddenDeathPoisonCombatTest {

    private static final double DELTA = 0.0001;

    private TestCharacter player1;
    private TestCharacter player2;
    private CombatSystem combat;

    @BeforeAll
    static void initBalance() {
        TestCombatBalance.init();
    }

    /**
     * Prepara dos personatges de prova sense regeneració per facilitar els asserts.
     */
    @BeforeEach
    void setUp() {
        player1 = new TestCharacter("Player 1");
        player2 = new TestCharacter("Player 2");
        combat = new CombatSystem(player1, player2);
    }

    /** Comprova que abans del torn 16 no s'aplica el verí global. */
    @Test
    void shouldNotApplySuddenDeathPoisonBeforeTurn16() {
        double p1InitialHp = player1.getStatistics().getHealth();
        double p2InitialHp = player2.getStatistics().getHealth();

        advanceRounds(15);

        assertFalse(player1.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));
        assertFalse(player2.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));

        assertEquals(p1InitialHp, player1.getStatistics().getHealth(), DELTA);
        assertEquals(p2InitialHp, player2.getStatistics().getHealth(), DELTA);
    }

    /** Comprova que el verí apareix al torn 16 i fa 1 de dany per torn. */
    @Test
    void shouldApplySuddenDeathPoisonOnTurn16() {
        advanceRounds(15);

        double p1BeforeTurn16 = player1.getStatistics().getHealth();
        double p2BeforeTurn16 = player2.getStatistics().getHealth();

        playDefensiveRound(); // torn 16

        assertTrue(player1.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));
        assertTrue(player2.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));

        assertEquals(p1BeforeTurn16 - 1.0, player1.getStatistics().getHealth(), DELTA);
        assertEquals(p2BeforeTurn16 - 1.0, player2.getStatistics().getHealth(), DELTA);
    }

    /** Comprova que la potència es manté dos torns i després puja. */
    @Test
    void shouldIncreasePoisonDamageEveryTwoTurnsAfterTurn15() {
        advanceRounds(15);

        assertRoundDamage(1.0); // torn 16
        assertRoundDamage(1.0); // torn 17
        assertRoundDamage(2.0); // torn 18
        assertRoundDamage(2.0); // torn 19
        assertRoundDamage(3.0); // torn 20
    }

    /** Comprova que el verí continua actiu una vegada aplicat. */
    @Test
    void shouldKeepSuddenDeathPoisonActiveAfterItStarts() {
        advanceRounds(15);

        playDefensiveRound(); // torn 16
        assertTrue(player1.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));
        assertTrue(player2.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));

        playDefensiveRound(); // torn 17
        assertTrue(player1.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));
        assertTrue(player2.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));

        playDefensiveRound(); // torn 18
        assertTrue(player1.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));
        assertTrue(player2.hasEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY));
    }

    /** Avança diverses rondes amb DEFEND vs DEFEND. */
    private void advanceRounds(int totalRounds) {
        for (int i = 0; i < totalRounds; i++) {
            playDefensiveRound();
        }
    }

    /** Juga una ronda defensiva per forçar l'allargament del combat. */
    private void playDefensiveRound() {
        combat.playRound(Action.DEFEND, Action.DEFEND);
    }

    /** Comprova el dany rebut pels dos personatges en una ronda. */
    private void assertRoundDamage(double expectedDamage) {
        double p1Before = player1.getStatistics().getHealth();
        double p2Before = player2.getStatistics().getHealth();

        playDefensiveRound();

        assertEquals(p1Before - expectedDamage, player1.getStatistics().getHealth(), DELTA);
        assertEquals(p2Before - expectedDamage, player2.getStatistics().getHealth(), DELTA);
    }

    /**
     * Personatge de prova amb stats vàlides i sense regeneració,
     * per poder mesurar exactament el dany del verí.
     */
    private static final class TestCharacter extends Character {

        private static final int[] TEST_STATS = CharacterCreator.getDummyStats().clone();

        TestCharacter(String name) {
            super(name, 18, TEST_STATS, Breed.HUMAN);
        }

        @Override
        public void regen() {
            // Desactivem la regeneració per facilitar els asserts del verí per torn.
        }
    }
}