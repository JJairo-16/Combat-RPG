package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.character.BloodPactConfig;
import rpgcombat.combat.CombatSystem;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.MenuCenter;
import rpgcombat.game.modifier.Actions;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.MagicalTiredness;

class BloodPactTest {

	private static final String EFFECT_KEY = MagicalTiredness.INTERNAL_EFFECT_KEY;
	private static final String OPTION_LABEL = "Pacte de sang";
	private static final int BASE_OPTIONS = 6;

	private Character dummy;
	private Character enemyDummy;
	private MenuCenter menuCenter;
	private CombatSystem combatSystem;

	@BeforeAll
	static void initBalance() {
		TestCombatBalance.init();
	}

	@BeforeEach
	void setUp() {
		dummy = CharacterCreator.dummy();
		enemyDummy = CharacterCreator.dummy();

		Map<String, List<StatusMod>> modifiers = Map.of(
				EFFECT_KEY,
				List.of(new StatusMod(
						0,
						OPTION_LABEL,
						"bloodPact",
						Actions::bloodPact,
						p -> true)));

		menuCenter = new MenuCenter(
				dummy,
				enemyDummy,
				c -> System.out.println("Canviar arma"),
				c -> System.out.println("Mostrar informació"),
				modifiers);

		combatSystem = new CombatSystem(dummy, enemyDummy);
	}

	@Test
	void shouldKeepBaseOptionsWhenManaIsAboveThreshold() {
		setManaTo(1.0, dummy);
		syncBloodPactEffect();

		assertFalse(dummy.hasEffect(EFFECT_KEY),
				"No s'hauria d'aplicar la fatiga màgica per sobre del llindar de mana.");
		assertEquals(BASE_OPTIONS, countOptions(),
				"No s'hauria d'afegir cap opció per sobre del llindar.");
	}

	@Test
	void shouldAddBloodPactOptionWhenManaDropsBelowThreshold() {
		assertEquals(BASE_OPTIONS, countOptions(),
				"El menú base hauria de tenir sis opcions.");

		setManaTo(0.0, dummy);
		syncBloodPactEffect();

		assertTrue(dummy.hasEffect(EFFECT_KEY),
				"S'hauria d'aplicar la fatiga màgica amb el mana baix.");
		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"S'hauria d'afegir l'opció de Pacte de sang.");
	}

	@Test
	void shouldAddBloodPactOptionAtThreshold() {
		double threshold = CombatBalanceRegistry.get().bloodPact().manaThreshold();

		setManaTo(threshold, dummy);
		syncBloodPactEffect();

        if (!dummy.hasEffect(EFFECT_KEY)) {
            double percent = (dummy.getStatistics().getMana() / dummy.getStatistics().getMaxMana());
            throw new IllegalArgumentException("" + percent);
        }

		assertTrue(dummy.hasEffect(EFFECT_KEY),
				"L'efecte s'hauria d'aplicar al llindar exacte.");
		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"L'opció s'hauria d'afegir al llindar exacte de mana.");
	}

	@Test
	void shouldAddAndRemoveOptionInCycle() {
		assertEquals(BASE_OPTIONS, countOptions(),
				"El menú base hauria de tenir sis opcions.");

		setManaTo(0.0, dummy);
		syncBloodPactEffect();
		assertEquals(BASE_OPTIONS + 1, countOptions(),
				"No s'ha afegit l'opció de Pacte de sang.");

		setManaTo(1.0, dummy);
		syncBloodPactEffect();
		assertEquals(BASE_OPTIONS, countOptions(),
				"L'opció s'hauria d'eliminar en recuperar prou mana.");
	}

	@Test
	void shouldRestoreAllMissingManaAndConsumeHealthWhenUsingBloodPact() throws Exception {
		setManaTo(0.0, dummy);
		setHealthTo(1.0, dummy);
		syncBloodPactEffect();

		assertTrue(dummy.hasEffect(EFFECT_KEY),
				"El CombatSystem hauria d'haver aplicat l'efecte abans d'usar el pacte.");

		Statistics stats = dummy.getStatistics();
		BloodPactConfig cfg = CombatBalanceRegistry.get().bloodPact();

		double maxMana = stats.getMaxMana();
		double manaBefore = stats.getMana();
		double missingMana = maxMana - manaBefore;

		double hpBefore = stats.getHealth();
		double expectedHpCostPercent = Math.max(
				cfg.minHpCostPercent(),
				cfg.baseHpCostPercent() - stats.getWisdom() * cfg.wisdomReduction());

		double scaling = 1.0 + (missingMana / maxMana) * 0.5;
		double expectedHpCost = Math.clamp(
				missingMana * expectedHpCostPercent * scaling,
				0,
				hpBefore - 1);

		invokeBloodPactCore(dummy);

		assertEquals(maxMana, stats.getMana(), 0.0001,
				"El Pacte de sang hauria de restaurar tot el mana que faltava.");
		assertEquals(hpBefore - expectedHpCost, stats.getHealth(), 0.0001,
				"El cost de vida aplicat no coincideix amb el càlcul esperat.");
	}

	@Test
	void shouldNeverReduceHealthBelowOnePoint() throws Exception {
		setManaTo(0.0, dummy);
		setHealthToRaw(1.5, dummy);
		syncBloodPactEffect();

		invokeBloodPactCore(dummy);

		assertEquals(1.0, dummy.getStatistics().getHealth(), 0.0001,
				"El Pacte de sang no hauria de poder deixar el jugador per sota d'1 de vida.");
		assertEquals(dummy.getStatistics().getMaxMana(), dummy.getStatistics().getMana(), 0.0001,
				"El mana s'hauria de restaurar completament.");
	}

	@Test
	void shouldSetCooldownAfterUsingBloodPact() {
		setManaTo(0.0, dummy);
		syncBloodPactEffect();

		MagicalTiredness magicalTiredness = getMagicalTiredness();
		assertNotNull(magicalTiredness,
				"L'efecte hauria d'estar actiu abans d'usar Pacte de sang.");

		magicalTiredness.use();

		assertFalse(magicalTiredness.canActivate(),
				"Després d'usar el Pacte de sang, no s'hauria de poder activar immediatament.");
		assertEquals(MagicalTiredness.COOLDOWN_TURNS, magicalTiredness.cooldownTurns(),
				"El cooldown hauria de quedar a tres torns.");
	}

	@Test
	void shouldAllowUsingBloodPactAgainWhenCooldownEnds() {
		setManaTo(0.0, dummy);
		syncBloodPactEffect();

		MagicalTiredness magicalTiredness = getMagicalTiredness();
		magicalTiredness.use();

		magicalTiredness.onMenuTurnEnd(dummy);
		magicalTiredness.onMenuTurnEnd(dummy);
		magicalTiredness.onMenuTurnEnd(dummy);

		assertTrue(magicalTiredness.canActivate(),
				"Després de tres ticks, el Pacte de sang s'hauria de poder tornar a activar.");
		assertEquals(0, magicalTiredness.cooldownTurns(),
				"El cooldown hauria d'haver acabat.");
	}

	private void syncBloodPactEffect() {
        combatSystem.syncEffectsOnly();
	}

	private void invokeBloodPactCore(Character player) throws Exception {
		Method method = Actions.class.getDeclaredMethod("useBloodPact", Character.class);
		method.setAccessible(true);
		method.invoke(null, player);
	}

	private MagicalTiredness getMagicalTiredness() {
		return (MagicalTiredness) dummy.getEffect(EFFECT_KEY);
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

	private void setHealthToRaw(double targetHealth, Character player) {
		Statistics stats = player.getStatistics();

		double currentHealth = stats.getHealth();
		double difference = targetHealth - currentHealth;

		if (difference > 0) {
			stats.heal(difference);
		} else if (difference < 0) {
			player.getDamage(-difference);
		}
	}

	private void setManaTo(double percent, Character player) {
		Statistics stats = player.getStatistics();

		double maxMana = stats.getMaxMana();
		double currentMana = stats.getMana();
		double targetMana = maxMana * percent;
		double difference = targetMana - currentMana;

		if (difference > 0) {
			stats.restoreMana(difference);
		} else if (difference < 0) {
			stats.consumeMana(-difference);
		}
	}

	private int countOptions() {
		return menuCenter.getMenu1().optionCount();
	}
}