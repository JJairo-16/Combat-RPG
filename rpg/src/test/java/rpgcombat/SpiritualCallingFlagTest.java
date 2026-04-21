package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.MenuCenter;
import rpgcombat.game.modifier.Actions;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;

class SpiritualCallingFlagTest {

        private static final String EFFECT_KEY = SpiritualCallingFlag.INTERNAL_EFFECT_KEY;
        private static final String OPTION_LABEL = "Invocar espíritus";

        private Character dummy;
        private MenuCenter menuCenter;

        @BeforeEach
        void setUp() {
                dummy = CharacterCreator.dummy();
                Character enemyDummy = CharacterCreator.dummy();

                Map<String, List<StatusMod>> modifiers = Map.of(
                                EFFECT_KEY,
                                List.of(new StatusMod(
                                                0,
                                                0, -1,
                                                0, -1,
                                                0, -1,
                                                OPTION_LABEL,
                                                "spiritualCalling",
                                                Actions::spiritualCalling,
                                                Character::canUseSpiritualCalling)));

                menuCenter = new MenuCenter(
                                dummy,
                                enemyDummy,
                                c -> System.out.println("Canviar arma"),
                                c -> System.out.println("Mostrar informació"),
                                modifiers);
        }

        @Test
        void shouldKeepBaseOptionsWhenHealthIsAboveThreshold() {
                setHealthTo(0.21, dummy);

                assertEquals(5, countOptions(),
                                "No s'hauria d'afegir cap opció per sobre del llindar.");
        }

        @Test
        void shouldAddSummonSpiritsOptionWhenHealthDropsBelowThreshold() {
                assertEquals(5, countOptions(),
                                "El menú base hauria de tenir cinc opcions.");

                setHealthTo(0.15, dummy);

                assertEquals(6, countOptions(),
                                "S'hauria d'afegir l'opció d'invocar esperits.");
        }

        @Test
        void shouldAddSummonSpiritsOptionAtThreshold() {
                setHealthTo(0.20, dummy);

                assertEquals(6, countOptions(),
                                "L'opció s'hauria d'afegir al 20% exacte de vida.");
        }

        @Test
        void shouldAddAndRemoveOptionInCycle() {
                assertEquals(5, countOptions(),
                                "El menú base hauria de tenir cinc opcions.");

                setHealthTo(0.15, dummy);
                assertEquals(6, countOptions(),
                                "No s'ha afegit l'opció per invocar esperits.");

                setHealthTo(1.0, dummy);
                assertEquals(5, countOptions(),
                                "L'opció s'hauria d'eliminar en recuperar prou vida.");
        }

        @Test
        void shouldNotAddOptionWithoutFlagEffect() {
                dummy.clearEffects();
                setHealthTo(0.10, dummy);

                assertEquals(5, countOptions(),
                                "Sense l'efecte, no s'hauria d'afegir l'opció.");
        }

        @Test
        void shouldSetCooldownAndRemoveOptionAfterUsingSpiritualCalling() {
                setHealthTo(0.10, dummy);
                assertEquals(6, countOptions(),
                                "L'opció hauria d'estar disponible abans d'usar-la.");

                double vidaAbans = dummy.getStatistics().getHealth();

                Actions.spiritualCalling(dummy);

                assertEquals(3, dummy.getSpiritualCallingCooldown(),
                                "El cooldown hauria de quedar a tres torns.");
                assertEquals(5, countOptions(),
                                "Després d'usar l'acció, l'opció s'hauria d'eliminar del menú.");
                assertTrue(dummy.getStatistics().getHealth() >= vidaAbans,
                                "La invocació hauria de curar o, com a mínim, no reduir la vida.");
        }

        @Test
        void shouldNotShowOptionWhileCooldownIsActiveEvenIfHealthIsLow() {
                setHealthTo(0.10, dummy);
                Actions.spiritualCalling(dummy);

                setHealthTo(0.10, dummy);

                assertEquals(5, countOptions(),
                                "Amb el cooldown actiu, l'opció no s'hauria de mostrar.");
        }

        @Test
        void shouldShowOptionAgainWhenCooldownEndsAndHealthIsStillLow() {
                setHealthTo(0.10, dummy);
                Actions.spiritualCalling(dummy);

                dummy.tickSpiritualCallingCooldown();
                dummy.tickSpiritualCallingCooldown();
                dummy.tickSpiritualCallingCooldown();
                setHealthTo(0.10, dummy);

                assertEquals(0, dummy.getSpiritualCallingCooldown(),
                                "El cooldown hauria d'haver acabat.");
                assertEquals(6, countOptions(),
                                "En acabar el cooldown i mantenir-se la vida baixa, l'opció hauria de tornar.");
        }

        @Test
        void shouldKeepOptionAfterTickIfConditionsStillMet() {
                setHealthTo(0.10, dummy);

                assertEquals(6, countOptions(),
                                "L'opció hauria d'estar disponible inicialment.");

                dummy.tickSpiritualCallingCooldown();

                assertEquals(6, countOptions(),
                                "Després d'un tick sense canvis rellevants, l'opció hauria de mantenir-se.");
        }

        private void setHealthTo(double percent, Character player) {
                Statistics stats = player.getStatistics();

                double maxHealth = stats.getMaxHealth();
                double currentHealth = stats.getHealth();
                double targetHealth = maxHealth * percent;
                double difference = targetHealth - currentHealth;

                if (difference > 0) {
                        stats.heal(difference);
                } else if (difference < 0) {
                        player.getDamage(-difference);
                }
        }

        private int countOptions() {
                return menuCenter.getMenu1().optionCount();
        }
}