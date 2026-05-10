package rpgcombat.game.modifier;

import java.util.function.IntSupplier;

import menu.model.MenuResult;
import rpgcombat.achievements.AchievementSystem;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.character.BloodPactConfig;
import rpgcombat.combat.models.Action;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryRuntime;
import rpgcombat.game.modifier.ui.Messages;
import rpgcombat.game.modifier.ultimate.UltimateActionEffect;
import rpgcombat.game.modifier.ultimate.UltimateActionType;
import rpgcombat.game.modifier.ui.Messages.CALL_SPIRITS;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.impl.menu.MagicalTiredness;
import rpgcombat.models.effects.impl.menu.SpiritualCallingFlag;
import rpgcombat.utils.input.Menu;
import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.rng.SpiritualCallingDie;
import rpgcombat.utils.rng.SpiritualCallingDie.RollResult;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.utils.ui.Cleaner;
import static rpgcombat.game.modifier.ui.Format.*;

/**
 * Classe utilitària que conté accions especials del sistema de combat RPG.
 * <p>
 * Aquesta classe no és instanciable i només proporciona mètodes estàtics
 * per executar habilitats com "Spiritual Calling" i "Blood Pact".
 */
public final class Actions {

    /** Utilitat per netejar la consola abans de mostrar informació */
    private static final Cleaner cleaner = new Cleaner();

    /** Configuració del sistema de Blood Pact extreta del registre de balanç */
    private static final BloodPactConfig bloodPactConfig = CombatBalanceRegistry.get().bloodPact();

    /** Nombre de torns de cooldown per a Spiritual Calling */
    private static final int SPIRITUAL_CALLING_COOLDOWN = 3;

    /** Sistema opcional per registrar assoliments d'accions especials. */
    private static AchievementSystem achievementSystem;

    /** Proveïdor del número de ronda actual. */
    private static IntSupplier roundSupplier = () -> 0;

    /**
     * Constructor privat per evitar la instanciació d'aquesta classe utilitària.
     */
    private Actions() {
    }

    /** Configura el seguiment opcional d'assoliments per a accions especials. */
    public static void configureAchievementTracking(AchievementSystem system, IntSupplier supplier) {
        achievementSystem = system;
        roundSupplier = supplier == null ? () -> 0 : supplier;
    }

    /**
     * Executa l'acció "Spiritual Calling".
     * <p>
     * Aquesta habilitat permet al jugador curar-se en funció d'una tirada
     * basada en el seu carisma. També aplica un cooldown després de l'ús.
     *
     * @param player el personatge que utilitza l'habilitat
     * @return un {@link MenuResult} que indica continuar el bucle del menú
     */
    public static MenuResult<Action> spiritualCalling(Character player) {
        cleaner.clear();

        if (!player.specialActionsEnabled()) {
            cannotUseSpecialActionsInThisMode();
            return MenuResult.repeatLoop();
        }

        if (player.hasSpecialMenuActionUsedThisTurn()) {
            cannotCombineSpecialActions();
            return MenuResult.repeatLoop();
        }

        if (!player.hasEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY)) {
            cannotUseSpiritualCalling();
            return MenuResult.repeatLoop();
        }

        SpiritualCallingFlag effect = (SpiritualCallingFlag) player.getEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY);
        if (!effect.canActivate()) {
            cannotUseSpiritualCalling();
            return MenuResult.repeatLoop();
        }

        effect.use();
        player.markSpecialMenuActionUsedThisTurn();

        CALL_SPIRITS.CALL_INIT.print();

        int charisma = player.getStatistics().getCharisma();
        System.out.println(DivineCharismaAffinity.classifyStanding(charisma).toString());

        System.out.println();
        Menu.pause();
        System.out.println();

        RollResult result = SpiritualCallingDie.roll(
                player.rng(),
                player.getStatistics());

        int face = result.face();
        double percentage = result.percent();

        double maxHp = player.getStatistics().getMaxHealth();
        double healAmount = maxHp * percentage;

        player.getStatistics().heal(healAmount);
        player.setSpiritualCallingCooldown(SPIRITUAL_CALLING_COOLDOWN);
        DiscoveryRuntime.discover(DiscoveryCategory.ACTIONS, "SPIRITUAL_CALLING");
        registerSpiritualCalling(player, face, percentage, healAmount);

        System.out.println();
        CALL_SPIRITS.classifyShot(face).print();

        Menu.pause();

        return MenuResult.repeatLoop();
    }

    /**
     * Mostra un missatge indicant que no es pot utilitzar "Spiritual Calling",
     * normalment per cooldown o manca de l'efecte necessari.
     */
    private static void cannotUseSpiritualCalling() {
        CALL_SPIRITS.CALL_IN_COOLDOWN.print();
        Menu.pause();
    }

    /**
     * Executa l'acció "Blood Pact".
     * <p>
     * Aquesta habilitat permet convertir vida en mana, amb un cost escalat
     * segons el mana que falta i la saviesa del personatge.
     *
     * @param player el personatge que utilitza el pacte
     * @return un {@link MenuResult} que indica continuar el bucle del menú
     */
    public static MenuResult<Action> bloodPact(Character player) {
        cleaner.clear();

        if (!player.specialActionsEnabled()) {
            cannotUseSpecialActionsInThisMode();
            return MenuResult.repeatLoop();
        }

        if (player.hasSpecialMenuActionUsedThisTurn()) {
            cannotCombineSpecialActions();
            return MenuResult.repeatLoop();
        }

        Effect e = player.getEffect(MagicalTiredness.INTERNAL_EFFECT_KEY);
        MagicalTiredness magicalTiredness = (MagicalTiredness) e;

        if (!magicalTiredness.canActivate()) {
            Messages.BLOOD_PACT.ALREDY_USED.print();
            System.out.println();
            Menu.pause();
            return MenuResult.repeatLoop();
        }

        magicalTiredness.use();
        player.markSpecialMenuActionUsedThisTurn();

        Messages.BLOOD_PACT.USE_BLOOD_PACT.print();
        BloodPactResult result = useBloodPact(player);
        DiscoveryRuntime.discover(DiscoveryCategory.ACTIONS, "BLOOD_PACT");
        registerBloodPact(player, result);

        System.out.println();
        Menu.pause();
        return MenuResult.repeatLoop();
    }

    /** Activa la ulti màgica de segona etapa. */
    public static MenuResult<Action> arcaneOverload(Character player) {
        return useUltimate(player, UltimateActionType.ARCANE_OVERLOAD);
    }

    /** Activa la ulti física de segona etapa. */
    public static MenuResult<Action> colossalBreak(Character player) {
        return useUltimate(player, UltimateActionType.COLOSSAL_BREAK);
    }

    /** Activa la ulti èlfica de rang de segona etapa. */
    public static MenuResult<Action> elvenOpeningShot(Character player) {
        return useUltimate(player, UltimateActionType.ELVEN_OPENING_SHOT);
    }

    /** Executa la lògica comuna d'activació d'una ulti. */
    private static MenuResult<Action> useUltimate(Character player, UltimateActionType type) {
        cleaner.clear();

        if (!player.specialActionsEnabled()) {
            cannotUseSpecialActionsInThisMode();
            return MenuResult.repeatLoop();
        }

        if (!UltimateActionEffect.canActivate(player, type)) {
            Messages.ULTIMATE.CANNOT_USE.print();
            System.out.println();
            Menu.pause();
            return MenuResult.repeatLoop();
        }

        boolean activated = UltimateActionEffect.activate(player, type);
        if (!activated) {
            Messages.ULTIMATE.CHARGE_FAILS.print();
            System.out.println();
            Menu.pause();
            return MenuResult.repeatLoop();
        }

        DiscoveryRuntime.discover(DiscoveryCategory.ACTIONS, type.discoveryId());
        registerUltimate(player, type);

        Messages.ULTIMATE.releaseFor(type).print();
        Messages.ULTIMATE.PRICE_PAID.print();
        System.out.println();
        Menu.pause();
        return MenuResult.returnValue(Action.ATTACK);
    }

    /** Mostra que no es poden encadenar accions especials de menú en el mateix torn. */
    private static void cannotCombineSpecialActions() {
        Messages.ULTIMATE.CANNOT_COMBINE.print();
        System.out.println();
        Menu.pause();
    }

    /** Mostra que el mode actual no permet accions especials. */
    private static void cannotUseSpecialActionsInThisMode() {
        System.out.println("Aquest mode de joc no permet accions especials.");
        System.out.println();
        Menu.pause();
    }

    /**
     * Aplica la lògica interna del "Blood Pact".
     * <p>
     * Calcula el mana que falta, el cost en vida corresponent i aplica
     * els canvis al personatge, incloent restauració de mana i dany rebut.
     *
     * @param player el personatge afectat pel pacte
     */
    private static BloodPactResult useBloodPact(Character player) {
        double maxMana = player.getStatistics().getMaxMana();
        double currentMana = player.getStatistics().getMana();
        double maxHp = player.getStatistics().getMaxHealth();
        double currentHp = player.getStatistics().getHealth();
        double hpBeforePercent = percent(currentHp, maxHp);
        double manaBeforePercent = percent(currentMana, maxMana);
        double missingMana = Math.max(0, maxMana - currentMana);

        if (missingMana <= 0) {
            Messages.BLOOD_PACT.MANA_ALREADY_FULL.print();
            return new BloodPactResult(0.0, 0.0, 0.0, hpBeforePercent, hpBeforePercent,
                    manaBeforePercent, manaBeforePercent);
        }

        double hpCostPercent = bloodPactHpCostPercent(player);

        double scaling = 1.0 + (missingMana / maxMana) * 0.5;
        double hpCost = missingMana * hpCostPercent * scaling;

        hpCost = Math.clamp(hpCost, 0, currentHp - 1);

        player.getStatistics().restoreMana(missingMana);
        player.getStatistics().damage(hpCost);
        double hpAfterPercent = percent(player.getStatistics().getHealth(), maxHp);
        double manaAfterPercent = percent(player.getStatistics().getMana(), maxMana);

        System.out.println();
        System.out.println("  " + Ansi.GREEN + "+" + Ansi.RESET + " "
                + "El teu mana es refà del tot: "
                + Ansi.BRIGHT_BLUE + "+" + round2(missingMana) + Ansi.RESET + ".");

        System.out.println("  " + Ansi.RED + "-" + Ansi.RESET + " "
                + "La sang exigida pel pacte et consumeix "
                + Ansi.BRIGHT_RED + round2(hpCost) + Ansi.RESET + " de vida.");

        System.out.println("  " + Ansi.DARK_GRAY + "+" + Ansi.RESET + " "
                + "El tribut ha estat fixat en un "
                + Ansi.YELLOW + round2(hpCostPercent * 100.0) + "%" + Ansi.RESET
                + " del mana restaurat.");

        printBloodPactBars(player);
        return new BloodPactResult(missingMana, hpCost, hpCostPercent, hpBeforePercent, hpAfterPercent,
                manaBeforePercent, manaAfterPercent);
    }

    /** Registra l'acció de Crida Espiritual al sistema d'assoliments, si existeix. */
    private static void registerSpiritualCalling(Character player, int face, double percentage, double healAmount) {
        if (achievementSystem == null) return;
        achievementSystem.onSpiritualCallingUsed(player, face, percentage, healAmount, roundSupplier.getAsInt());
    }

    /** Registra l'acció de Pacte de Sang al sistema d'assoliments, si existeix. */
    private static void registerBloodPact(Character player, BloodPactResult result) {
        if (achievementSystem == null || result == null) return;
        achievementSystem.onBloodPactUsed(player, result.manaRestored(), result.hpCost(),
                result.hpCostPercent(), result.hpBeforePercent(), result.hpAfterPercent(),
                result.manaBeforePercent(), result.manaAfterPercent(), roundSupplier.getAsInt());
    }

    /** Registra l'ús d'una ulti de segona etapa. */
    private static void registerUltimate(Character player, UltimateActionType type) {
        if (achievementSystem == null || type == null) return;
        achievementSystem.onUltimateUsed(player, type.discoveryId(), type.label(), type.weaponType().name(),
                roundSupplier.getAsInt());
    }

    /** Resultat intern del Pacte de Sang per alimentar assoliments. */
    private record BloodPactResult(double manaRestored, double hpCost, double hpCostPercent,
            double hpBeforePercent, double hpAfterPercent, double manaBeforePercent, double manaAfterPercent) {}

    /** Calcula un percentatge segur per a metadades d'assoliments. */
    private static double percent(double value, double max) {
        if (max <= 0.0) return 0.0;
        return Math.clamp((value / max) * 100.0, 0.0, 100.0);
    }

    /**
     * Calcula el percentatge de vida que es consumirà en funció del mana restaurat.
     * <p>
     * Aquest percentatge es redueix segons la saviesa del personatge,
     * però mai baixa d'un mínim definit a la configuració.
     *
     * @param player el personatge del qual es calcula el cost
     * @return percentatge de vida a consumir (entre mínim i valor reduït)
     */
    private static double bloodPactHpCostPercent(Character player) {
        int wisdom = player.getStatistics().getWisdom();

        double reducedPercent = bloodPactConfig.baseHpCostPercent()
                - (wisdom * bloodPactConfig.wisdomReduction());

        return Math.max(bloodPactConfig.minHpCostPercent(), reducedPercent);
    }
}
