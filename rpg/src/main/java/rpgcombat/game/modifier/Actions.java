package rpgcombat.game.modifier;

import menu.model.MenuResult;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.character.BloodPactConfig;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.ui.Messages;
import rpgcombat.game.modifier.ui.Messages.CALL_SPIRITS;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.impl.MagicalTiredness;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;
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

    /**
     * Constructor privat per evitar la instanciació d'aquesta classe utilitària.
     */
    private Actions() {
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

        Effect e = player.getEffect(MagicalTiredness.INTERNAL_EFFECT_KEY);
        MagicalTiredness magicalTiredness = (MagicalTiredness) e;

        if (!magicalTiredness.canActivate()) {
            Messages.BLOOD_PACT.ALREDY_USED.print();
            System.out.println();
            Menu.pause();
            return MenuResult.repeatLoop();
        }

        magicalTiredness.use();

        Messages.BLOOD_PACT.USE_BLOOD_PACT.print();
        useBloodPact(player);

        System.out.println();
        Menu.pause();
        return MenuResult.repeatLoop();
    }

    /**
     * Aplica la lògica interna del "Blood Pact".
     * <p>
     * Calcula el mana que falta, el cost en vida corresponent i aplica
     * els canvis al personatge, incloent restauració de mana i dany rebut.
     *
     * @param player el personatge afectat pel pacte
     */
    private static void useBloodPact(Character player) {
        double maxMana = player.getStatistics().getMaxMana();
        double currentMana = player.getStatistics().getMana();
        double missingMana = Math.max(0, maxMana - currentMana);

        if (missingMana <= 0) {
            Messages.BLOOD_PACT.MANA_ALREADY_FULL.print();
            return;
        }

        double hpCostPercent = bloodPactHpCostPercent(player);

        double scaling = 1.0 + (missingMana / maxMana) * 0.5;
        double hpCost = missingMana * hpCostPercent * scaling;

        double currentHp = player.getStatistics().getHealth();
        hpCost = Math.clamp(hpCost, 0, currentHp - 1);

        player.getStatistics().restoreMana(missingMana);
        player.getStatistics().damage(hpCost);

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