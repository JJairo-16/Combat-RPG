package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.menu.MenuCenter;
import rpgcombat.game.modifier.Actions;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;
import rpgcombat.utils.rng.DivineCharismaAffinity;

/**
 * Tests del comportament de l'efecte SpiritualCallingFlag.
 * Verifica quan apareix, desapareix i es bloqueja l'opció d'invocació.
 */
class SpiritualCallingFlagTest {

	private static final String EFFECT_KEY = SpiritualCallingFlag.INTERNAL_EFFECT_KEY;
	private static final String OPTION_LABEL = "Invocar espíritus";

	private static final int BASE_OPTIONS = 6;

	private Character dummy;
	private MenuCenter menuCenter;

	@BeforeAll
	static void initBalance() {
		TestCombatBalance.init();
	}

	/**
	 * Inicialitza personatges i menú amb el modificador d'invocació.
	 */
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
				modifiers,
				Collections.emptyMap());

		DivineCharismaAffinity.rollForRun(new Random());
	}

	/** No afegeix l'opció si la vida està per sobre del llindar. */
	@Test
	void shouldKeepBaseOptionsWhenHealthIsAboveThreshold() {
		setHealthTo(0.21, dummy);

		assertEquals(BASE_OPTIONS, countOptions(),
				"No s'hauria d'afegir cap opció per sobre del llindar.");
	}

	/** Afegeix l'opció quan la vida baixa del llindar. */
	@Test
	void shouldAddSummonSpiritsOptionWhenHealthDropsBelowThreshold() {
		assertEquals(BASE_OPTIONS, countOptions(),
				"El menú base hauria de tenir cinc opcions.");

		setHealthTo(0.15, dummy);

		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"S'hauria d'afegir l'opció d'invocar esperits.");
	}

	/** Afegeix l'opció exactament al llindar (20%). */
	@Test
	void shouldAddSummonSpiritsOptionAtThreshold() {
		setHealthTo(0.20, dummy);

		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"L'opció s'hauria d'afegir al 20% exacte de vida.");
	}

	/** L'opció apareix i desapareix segons la vida. */
	@Test
	void shouldAddAndRemoveOptionInCycle() {
		assertEquals(BASE_OPTIONS, countOptions(),
				"El menú base hauria de tenir cinc opcions.");

		setHealthTo(0.15, dummy);
		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"No s'ha afegit l'opció per invocar esperits.");

		setHealthTo(1.0, dummy);
		assertEquals(BASE_OPTIONS, countOptions(),
				"L'opció s'hauria d'eliminar en recuperar prou vida.");
	}

	/** Sense l'efecte actiu, no apareix l'opció. */
	@Test
	void shouldNotAddOptionWithoutFlagEffect() {
		dummy.clearEffects();
		setHealthTo(0.10, dummy);

		assertEquals(BASE_OPTIONS, countOptions(),
				"Sense l'efecte, no s'hauria d'afegir l'opció.");
	}

	/** Usar l'habilitat aplica cooldown i elimina l'opció. */
	@Test
	void shouldSetCooldownAndRemoveOptionAfterUsingSpiritualCalling() {
		setHealthTo(0.10, dummy);
		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"L'opció hauria d'estar disponible abans d'usar-la.");

		double vidaAbans = dummy.getStatistics().getHealth();

		Actions.spiritualCalling(dummy);

		assertEquals(3, dummy.getSpiritualCallingCooldown(),
				"El cooldown hauria de quedar a tres torns.");
		assertEquals(BASE_OPTIONS, countOptions(),
				"Després d'usar l'acció, l'opció s'hauria d'eliminar del menú.");
		assertTrue(dummy.getStatistics().getHealth() >= vidaAbans,
				"La invocació hauria de curar o, com a mínim, no reduir la vida.");
	}

	/** Amb cooldown actiu, l'opció no es mostra. */
	@Test
	void shouldNotShowOptionWhileCooldownIsActiveEvenIfHealthIsLow() {
		setHealthTo(0.10, dummy);
		Actions.spiritualCalling(dummy);

		setHealthTo(0.10, dummy);

		assertEquals(BASE_OPTIONS, countOptions(),
				"Amb el cooldown actiu, l'opció no s'hauria de mostrar.");
	}

	/** L'opció reapareix quan acaba el cooldown si la vida segueix baixa. */
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
		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"En acabar el cooldown i mantenir-se la vida baixa, l'opció hauria de tornar.");
	}

	/** Manté l'opció si les condicions no canvien. */
	@Test
	void shouldKeepOptionAfterTickIfConditionsStillMet() {
		setHealthTo(0.10, dummy);

		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"L'opció hauria d'estar disponible inicialment.");

		dummy.tickSpiritualCallingCooldown();

		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"Després d'un tick sense canvis rellevants, l'opció hauria de mantenir-se.");
	}

	/**
	 * Ajusta la vida del personatge a un percentatge del màxim.
	 */
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

	/** Retorna el nombre d'opcions del menú principal. */
	private int countOptions() {
		return menuCenter.getMenu1().optionCount();
	}
}