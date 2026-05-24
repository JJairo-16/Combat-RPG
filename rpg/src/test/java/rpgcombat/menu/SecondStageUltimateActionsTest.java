package rpgcombat.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import menu.model.MenuResult;
import rpgcombat.TestCombatBalance;
import rpgcombat.achievements.AchievementEvent;
import rpgcombat.achievements.AchievementUpdate;
import rpgcombat.combat.models.Action;
import rpgcombat.game.menu.MenuCenter;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.ultimate.UltimateActionEffect;
import rpgcombat.game.modifier.ultimate.UltimateActionType;
import rpgcombat.game.modifier.ultimate.UltimateActionUnlocks;
import rpgcombat.game.modifier.ultimate.UltimateChargeBoost;
import rpgcombat.unlocks.UnlockRequirement;
import rpgcombat.unlocks.UnlockRequirementType;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.Effect;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponType;
import rpgcombat.weapons.passives.HitContext;

/**
 * Proves del comportament de les ultis de segona etapa.
 * Verifica quan apareixen, desapareixen i quin efecte apliquen en activar-se.
 */
class SecondStageUltimateActionsTest {

    private static final double DELTA = 0.0001;
    private static final int BASE_OPTIONS = 6;
    private static final String CHARGE_LABEL = Action.CHARGE.label();

    private Character dummy;
    private Character enemyDummy;
    private MenuCenter menuCenter;

    @BeforeAll
    static void initBalance() {
        TestCombatBalance.init();
    }

    /**
     * Inicialitza un menú bàsic i força els desbloquejos per aïllar les
     * condicions pròpies de cada ulti.
     */
    @BeforeEach
    void setUp() throws Exception {
        setUltimateUnlockOverride(Boolean.TRUE);

        dummy = createCharacter(Breed.HUMAN, balancedStats());
        enemyDummy = createCharacter(Breed.HUMAN, balancedStats());
        menuCenter = createMenuCenter(dummy, enemyDummy);
    }

    /** Restaura l'estat global de desbloqueig de proves. */
    @AfterEach
    void tearDown() throws Exception {
        setUltimateUnlockOverride(null);
    }

    /** Mostra l'opció de càrrega mentre no hi ha cap atac carregat. */
    @Test
    void shouldShowChargeOptionWhenAttackIsNotCharged() {
        assertTrue(hasOption(CHARGE_LABEL),
                "L'opció de carregar atac hauria d'aparèixer quan no hi ha cap càrrega activa.");
        assertEquals(BASE_OPTIONS, countOptions(),
                "El menú base hauria de mantenir les sis opcions inicials.");
    }

    /** Amaga l'opció de càrrega mentre la càrrega està preparada. */
    @Test
    void shouldHideChargeOptionWhenAttackIsAlreadyCharged() {
        dummy.prepareChargedAttack();

        assertFalse(hasOption(CHARGE_LABEL),
                "Quan l'atac ja està carregat, l'opció de carregar atac hauria de desaparèixer.");
    }

    /** No mostra cap ulti quan encara no s'ha desbloquejat. */
    @Test
    void shouldHideUltimateOptionsWhenTheyAreNotUnlocked() throws Exception {
        setUltimateUnlockOverride(Boolean.FALSE);

        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "La Sobrecàrrega arcana no hauria d'aparèixer si no està desbloquejada.");

        dummy = createCharacter(Breed.ORC, new int[] { 35, 20, 20, 10, 25, 15, 15 });
        dummy.setWeapon(createWeapon(WeaponType.PHYSICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(UltimateActionType.COLOSSAL_BREAK.label()),
                "El Trencament colossal no hauria d'aparèixer si no està desbloquejat.");

        dummy = createCharacter(Breed.ELF, new int[] { 10, 25, 20, 10, 25, 20, 30 });
        dummy.setWeapon(createWeapon(WeaponType.RANGE));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(UltimateActionType.ELVEN_OPENING_SHOT.label()),
                "El Tret èlfic no hauria d'aparèixer si no està desbloquejat.");
    }


    /** Cada ulti es desbloqueja només amb CHARGE descobert i el seu assoliment propi. */
    @Test
    void shouldUseChargeDiscoveryAndOneSpecificAchievementAsUnlockRule() {
        assertUnlockRule(UltimateActionType.ARCANE_OVERLOAD, "ARCANE_INITIATE");
        assertUnlockRule(UltimateActionType.COLOSSAL_BREAK, "FULL_FORCE");
        assertUnlockRule(UltimateActionType.ELVEN_OPENING_SHOT, "EAGLE_EYE");
    }

    /** Mostra una ulti desbloquejada quan tots els requisits contextuals es compleixen. */
    @Test
    void shouldShowUnlockedUltimateWhenAllContextRequirementsAreMet() {
        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertTrue(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Una ulti desbloquejada hauria d'aparèixer quan compleix requisits, càrrega i cooldown.");
    }

    /** No mostra cap ulti si no hi ha atac carregat, encara que la resta de requisits es compleixin. */
    @Test
    void shouldHideUltimateOptionsWhenAttackIsNotCharged() {
        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertTrue(hasOption(CHARGE_LABEL),
                "Sense càrrega activa, l'opció de carregar atac hauria d'estar disponible.");
        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Sense càrrega activa, la Sobrecàrrega arcana no hauria d'aparèixer.");

        dummy = createCharacter(Breed.ORC, new int[] { 35, 20, 20, 10, 25, 15, 15 });
        dummy.setWeapon(createWeapon(WeaponType.PHYSICAL));
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertTrue(hasOption(CHARGE_LABEL),
                "Sense càrrega activa, l'opció de carregar atac hauria d'estar disponible.");
        assertFalse(hasOption(UltimateActionType.COLOSSAL_BREAK.label()),
                "Sense càrrega activa, el Trencament colossal no hauria d'aparèixer.");

        dummy = createCharacter(Breed.ELF, new int[] { 10, 25, 20, 10, 25, 20, 30 });
        dummy.setWeapon(createWeapon(WeaponType.RANGE));
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertTrue(hasOption(CHARGE_LABEL),
                "Sense càrrega activa, l'opció de carregar atac hauria d'estar disponible.");
        assertFalse(hasOption(UltimateActionType.ELVEN_OPENING_SHOT.label()),
                "Sense càrrega activa, el Tret èlfic no hauria d'aparèixer.");
    }

    /** No mostra cap ulti durant el cooldown inicial, encara que la resta de requisits es compleixin. */
    @Test
    void shouldKeepUltimateOptionsHiddenWhileInitialCooldownIsActive() {
        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        dummy.prepareChargedAttack();
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(CHARGE_LABEL),
                "La càrrega activa hauria d'amagar l'opció de carregar atac.");
        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "La Sobrecàrrega arcana no hauria d'aparèixer mentre el cooldown inicial sigui actiu.");
    }

    /** La Sobrecàrrega arcana apareix només quan hi ha càrrega, mana alt i arma màgica. */
    @Test
    void shouldShowArcaneOverloadOnlyWhenMagicalRequirementsAreMet() {
        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        menuCenter = createMenuCenter(dummy, enemyDummy);

        advanceUltimateCooldowns(dummy, 5);
        assertTrue(hasOption(CHARGE_LABEL),
                "Sense càrrega activa, l'opció de carregar atac hauria de continuar disponible.");
        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Sense càrrega d'atac, la Sobrecàrrega arcana no hauria d'aparèixer.");

        dummy.prepareChargedAttack();
        setManaTo(0.74, dummy);
        assertFalse(hasOption(CHARGE_LABEL),
                "La càrrega activa hauria d'amagar l'opció de carregar atac.");
        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Amb menys del 75% de mana, la Sobrecàrrega arcana no hauria d'aparèixer.");

        setManaTo(0.75, dummy);
        assertTrue(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Amb arma màgica, càrrega i mana suficient, la Sobrecàrrega arcana hauria d'aparèixer.");
    }

    /** En activar la Sobrecàrrega arcana es consumeix càrrega, mana i es prepara el boost. */
    @Test
    void shouldActivateArcaneOverloadAndApplyItsBoost() {
        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(CHARGE_LABEL),
                "Abans d'activar la ulti, l'opció de carregar atac hauria d'estar amagada.");
        assertTrue(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "La Sobrecàrrega arcana hauria d'estar disponible abans d'activar-la.");

        double manaBefore = dummy.getStatistics().getMana();

        assertTrue(UltimateActionEffect.activate(dummy, UltimateActionType.ARCANE_OVERLOAD),
                "La Sobrecàrrega arcana s'hauria d'activar correctament.");
        assertFalse(dummy.hasChargedAttack(),
                "La Sobrecàrrega arcana hauria de consumir la càrrega d'atac.");
        assertTrue(hasOption(CHARGE_LABEL),
                "En consumir-se la càrrega, l'opció de carregar atac hauria de tornar a aparèixer.");
        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Després d'usar una ulti, la seva opció hauria de desaparèixer del menú.");
        assertTrue(dummy.getStatistics().getMana() < manaBefore,
                "La Sobrecàrrega arcana hauria de consumir mana addicional.");
        assertTrue(UltimateChargeBoost.isArmed(dummy),
                "La Sobrecàrrega arcana hauria de preparar un boost d'un sol cop.");

        HitContext ctx = createHitContext(dummy, enemyDummy, dummy.getWeapon());
        ctx.setBaseDamage(100.0);
        activeBoost(dummy).modifyDamage(ctx, new Random(1), dummy);

        assertEquals(205.0, ctx.damageToResolve(), DELTA,
                "El multiplicador de la Sobrecàrrega arcana hauria de ser x2.05.");
    }

    /** El Trencament colossal apareix amb càrrega i arma física, i consumeix recursos ocults. */
    @Test
    void shouldActivateColossalBreakAndConsumeHiddenResources() {
        dummy = createCharacter(Breed.ORC, new int[] { 35, 20, 20, 10, 25, 15, 15 });
        dummy.setWeapon(createWeapon(WeaponType.PHYSICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(CHARGE_LABEL),
                "La càrrega activa hauria d'amagar l'opció de carregar atac.");
        assertTrue(hasOption(UltimateActionType.COLOSSAL_BREAK.label()),
                "El Trencament colossal hauria d'aparèixer amb càrrega i arma física.");

        Statistics stats = dummy.getStatistics();
        double staminaBefore = stats.getStamina();
        double resistanceBefore = stats.getResistance();

        assertTrue(UltimateActionEffect.activate(dummy, UltimateActionType.COLOSSAL_BREAK),
                "El Trencament colossal s'hauria d'activar correctament.");
        assertTrue(hasOption(CHARGE_LABEL),
                "En consumir-se la càrrega, l'opció de carregar atac hauria de tornar a aparèixer.");
        assertFalse(hasOption(UltimateActionType.COLOSSAL_BREAK.label()),
                "El Trencament colossal hauria de desaparèixer després d'usar-se.");
        assertTrue(stats.getStamina() < staminaBefore,
                "El Trencament colossal hauria de consumir estamina oculta.");
        assertTrue(stats.getResistance() < resistanceBefore,
                "El Trencament colossal hauria de consumir resistència oculta.");

        HitContext ctx = createHitContext(dummy, enemyDummy, dummy.getWeapon());
        ctx.setBaseDamage(100.0);
        activeBoost(dummy).modifyDamage(ctx, new Random(1), dummy);

        assertEquals(195.0, ctx.damageToResolve(), DELTA,
                "El multiplicador del Trencament colossal hauria de ser x1.95.");
    }

    /** El Tret èlfic requereix elf, destresa alta, arma de rang i afegeix crític. */
    @Test
    void shouldRequireElfAndHighDexterityForElvenOpeningShot() {
        dummy = createCharacter(Breed.HUMAN, new int[] { 10, 25, 20, 10, 25, 20, 30 });
        dummy.setWeapon(createWeapon(WeaponType.RANGE));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(UltimateActionType.ELVEN_OPENING_SHOT.label()),
                "El Tret èlfic no hauria d'aparèixer si el personatge no és elf.");

        dummy = createCharacter(Breed.ELF, new int[] { 10, 21, 20, 10, 29, 20, 30 });
        dummy.setWeapon(createWeapon(WeaponType.RANGE));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(UltimateActionType.ELVEN_OPENING_SHOT.label()),
                "El Tret èlfic no hauria d'aparèixer si la destresa efectiva no arriba a 25.");

        dummy = createCharacter(Breed.ELF, new int[] { 10, 25, 20, 10, 25, 20, 30 });
        dummy.setWeapon(createWeapon(WeaponType.RANGE));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(CHARGE_LABEL),
                "La càrrega activa hauria d'amagar l'opció de carregar atac.");
        assertTrue(hasOption(UltimateActionType.ELVEN_OPENING_SHOT.label()),
                "El Tret èlfic hauria d'aparèixer amb elf, destresa alta, arma de rang i càrrega.");

        assertTrue(UltimateActionEffect.activate(dummy, UltimateActionType.ELVEN_OPENING_SHOT),
                "El Tret èlfic s'hauria d'activar correctament.");
        assertTrue(hasOption(CHARGE_LABEL),
                "En consumir-se la càrrega, l'opció de carregar atac hauria de tornar a aparèixer.");
        assertFalse(hasOption(UltimateActionType.ELVEN_OPENING_SHOT.label()),
                "El Tret èlfic hauria de desaparèixer després d'usar-se.");

        HitContext ctx = createHitContext(dummy, enemyDummy, dummy.getWeapon());
        ctx.setBaseDamage(100.0);
        ctx.setCriticalChance(0.10);

        UltimateChargeBoost boost = activeBoost(dummy);
        boost.rollCrit(ctx, new Random(1), dummy);
        boost.modifyDamage(ctx, new Random(1), dummy);

        assertEquals(0.25, ctx.criticalChance(), DELTA,
                "El Tret èlfic hauria d'afegir un 15% de probabilitat crítica.");
        assertEquals(175.0, ctx.damageToResolve(), DELTA,
                "El multiplicador del Tret èlfic hauria de ser x1.75.");
    }

    /** Després d'usar una ulti, cap altra es mostra ni es pot activar en el mateix combat. */
    @Test
    void shouldAllowOnlyOneUltimatePerCombat() {
        dummy = createCharacter(Breed.ORC, new int[] { 35, 20, 20, 10, 25, 15, 15 });
        dummy.setWeapon(createWeapon(WeaponType.PHYSICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);

        assertTrue(UltimateActionEffect.activate(dummy, UltimateActionType.COLOSSAL_BREAK),
                "La primera ulti del combat s'hauria de poder activar.");

        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);

        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "Després d'usar una ulti, cap altra ulti hauria d'aparèixer en el menú del mateix combat.");
        assertFalse(UltimateActionEffect.canActivate(dummy, UltimateActionType.ARCANE_OVERLOAD),
                "Després d'usar una ulti, cap altra ulti hauria de poder activar-se en el mateix combat.");
    }

    /** Crea correctament els esdeveniments d'assoliment de la Sobrecàrrega arcana. */
    @Test
    void shouldCreateAchievementUpdateForArcaneOverloadUse() {
        assertUltimateAchievementUpdate(
                UltimateActionType.ARCANE_OVERLOAD,
                AchievementEvent.ARCANE_OVERLOAD_USED,
                "MAGICAL");
    }

    /** Crea correctament els esdeveniments d'assoliment del Trencament colossal. */
    @Test
    void shouldCreateAchievementUpdateForColossalBreakUse() {
        assertUltimateAchievementUpdate(
                UltimateActionType.COLOSSAL_BREAK,
                AchievementEvent.COLOSSAL_BREAK_USED,
                "PHYSICAL");
    }

    /** Crea correctament els esdeveniments d'assoliment del Tret èlfic. */
    @Test
    void shouldCreateAchievementUpdateForElvenOpeningShotUse() {
        assertUltimateAchievementUpdate(
                UltimateActionType.ELVEN_OPENING_SHOT,
                AchievementEvent.ELVEN_OPENING_SHOT_USED,
                "RANGE");
    }

    /** Les ultis no poden aparèixer si ja s'ha usat una acció especial de menú aquest torn. */
    @Test
    void shouldHideUltimateWhenSpecialMenuActionWasUsedThisTurn() {
        dummy = createCharacter(Breed.GNOME, new int[] { 10, 20, 20, 35, 20, 20, 15 });
        dummy.setWeapon(createWeapon(WeaponType.MAGICAL));
        dummy.prepareChargedAttack();
        advanceUltimateCooldowns(dummy, 5);
        menuCenter = createMenuCenter(dummy, enemyDummy);

        assertTrue(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "La Sobrecàrrega arcana hauria d'aparèixer abans d'usar cap acció especial.");

        dummy.markSpecialMenuActionUsedThisTurn();

        assertFalse(hasOption(UltimateActionType.ARCANE_OVERLOAD.label()),
                "La ulti no hauria d'aparèixer si ja s'ha usat una acció especial de menú aquest torn.");
        assertFalse(hasOption(CHARGE_LABEL),
                "La càrrega continua activa i, per tant, l'opció de carregar atac hauria de seguir amagada.");
    }

    /** Comprova els camps i esdeveniments que fan avançar els assoliments d'ulti. */
    private void assertUltimateAchievementUpdate(UltimateActionType type, AchievementEvent specificEvent,
            String expectedWeaponType) {
        AchievementUpdate update = AchievementUpdate.ultimateUsed(
                dummy,
                type.discoveryId(),
                type.label(),
                expectedWeaponType,
                7);

        assertTrue(update.has(AchievementEvent.SPECIAL_ACTION_USED),
                "L'ús d'una ulti també hauria de comptar com a acció especial.");
        assertTrue(update.has(AchievementEvent.ULTIMATE_USED),
                "L'ús d'una ulti hauria de generar l'esdeveniment genèric ULTIMATE_USED.");
        assertTrue(update.has(specificEvent),
                "L'ús d'una ulti hauria de generar el seu esdeveniment específic.");
        assertEquals(type.discoveryId(), update.stringField("ultimateId"),
                "L'actualització hauria de conservar l'identificador de la ulti.");
        assertEquals(type.label(), update.stringField("ultimateName"),
                "L'actualització hauria de conservar el nom visible de la ulti.");
        assertEquals(expectedWeaponType, update.stringField("ultimateWeaponType"),
                "L'actualització hauria de conservar el tipus d'arma associat.");
        assertTrue(update.booleanField("ultimateUsed"),
                "L'actualització hauria de marcar explícitament que s'ha usat una ulti.");
    }


    /** Comprova que una regla d'ulti només demana CHARGE i l'assoliment propi. */
    private void assertUnlockRule(UltimateActionType type, String achievementId) {
        List<UnlockRequirement> requirements = UltimateActionUnlocks.ruleFor(type).requirements();

        assertEquals(2, requirements.size(),
                "Cada ulti hauria de demanar només CHARGE i el seu assoliment propi.");
        assertTrue(requirements.stream().anyMatch(req ->
                req.type() == UnlockRequirementType.ACHIEVEMENT
                        && achievementId.equals(req.id())),
                "La regla hauria de requerir l'assoliment propi de la ulti.");
        assertTrue(requirements.stream().anyMatch(req ->
                req.type() == UnlockRequirementType.DISCOVERY
                        && "ACTIONS".equals(req.category())
                        && "CHARGE".equals(req.id())),
                "La regla hauria de requerir haver descobert l'acció de carregar atac.");
    }

    /** Crea el menú amb els tres modificadors d'ulti. */
    private MenuCenter createMenuCenter(Character player, Character enemy) {
        return new MenuCenter(
                player,
                enemy,
                c -> System.out.println("Canviar arma"),
                c -> System.out.println("Mostrar informació"),
                ultimateModifiers(),
                Collections.emptyMap());
    }

    /** Modificadors de menú de les tres ultis. */
    private Map<String, List<StatusMod>> ultimateModifiers() {
        return Map.of(
                UltimateActionType.ARCANE_OVERLOAD.effectKey(),
                List.of(createUltimateMod(UltimateActionType.ARCANE_OVERLOAD)),
                UltimateActionType.COLOSSAL_BREAK.effectKey(),
                List.of(createUltimateMod(UltimateActionType.COLOSSAL_BREAK)),
                UltimateActionType.ELVEN_OPENING_SHOT.effectKey(),
                List.of(createUltimateMod(UltimateActionType.ELVEN_OPENING_SHOT)));
    }

    /** Crea un modificador d'ulti amb la mateixa forma que els altres mods de menú. */
    private StatusMod createUltimateMod(UltimateActionType type) {
        return new StatusMod(
                100,
                type.label(),
                type.discoveryId(),
                p -> MenuResult.repeatLoop(),
                p -> UltimateActionEffect.canActivate(p, type));
    }

    /** Avança el cooldown inicial de les ultis. */
    private void advanceUltimateCooldowns(Character player, int turns) {
        for (int i = 0; i < turns; i++) {
            player.onMenuTurnEnd();
        }
    }

    /** Retorna el boost actiu de la ulti. */
    private UltimateChargeBoost activeBoost(Character player) {
        for (Effect effect : player.getEffects()) {
            if (effect instanceof UltimateChargeBoost boost && !boost.isExpired()) {
                return boost;
            }
        }
        throw new AssertionError("No s'ha trobat cap boost d'ulti actiu.");
    }

    /** Crea un context de cop per provar els modificadors de dany. */
    private HitContext createHitContext(Character attacker, Character defender, Weapon weapon) {
        return new HitContext(attacker, defender, weapon, new Random(1), Action.ATTACK, Action.ATTACK);
    }

    /** Crea un personatge de prova amb estadístiques vàlides. */
    private Character createCharacter(Breed breed, int[] stats) {
        Character character = new Character("Tester", 18, stats, breed);
        ensureUltimateFlags(character);
        return character;
    }

    /** Assegura que el personatge té els efectes interns de les ultis. */
    private void ensureUltimateFlags(Character player) {
        for (UltimateActionType type : UltimateActionType.values()) {
            if (UltimateActionEffect.find(player, type) == null) {
                player.addInternalEffect(new UltimateActionEffect(type));
            }
        }
    }

    /** Stats equilibrades que sumen 140 punts. */
    private int[] balancedStats() {
        return new int[] { 20, 20, 20, 20, 20, 20, 20 };
    }

    /** Crea una arma mínima del tipus indicat. */
    private Weapon createWeapon(WeaponType type) {
        return new Weapon(
                "TEST_" + type.name(),
                "Arma de prova " + type.name(),
                "Arma de prova per a ultis.",
                70,
                0.0,
                1.25,
                type,
                (weapon, stats, rng) -> new AttackResult(100.0, "ataca."),
                0.0,
                List.of());
    }

    /** Ajusta el mana actual a un percentatge del màxim. */
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

    /** Retorna el nombre d'opcions del menú principal. */
    private int countOptions() {
        return menuCenter.getMenu1().optionCount();
    }

    /** Indica si el menú principal conté una opció concreta. */
    private boolean hasOption(String label) {
        return menuCenter.getMenu1().hasOption(label);
    }

    /** Activa o desactiva el bypass de desbloqueig només per a tests. */
    private void setUltimateUnlockOverride(Boolean value) throws Exception {
        Method method = UltimateActionUnlocks.class.getDeclaredMethod("setTestUnlockedOverride", Boolean.class);
        method.setAccessible(true);
        method.invoke(null, value);
    }
}
