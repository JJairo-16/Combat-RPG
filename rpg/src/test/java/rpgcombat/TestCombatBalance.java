package rpgcombat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import rpgcombat.balance.CombatBalanceLoader;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;

/**
 * Classe d'utilitat per inicialitzar la configuració de balanceig de combat en tests.
 */
public final class TestCombatBalance {

    private static boolean initialized = false;

    private TestCombatBalance() {
    }

    /**
     * Inicialitza el balanceig de combat si encara no s'ha fet.
     * Carrega el fitxer combatBalance.json des dels recursos.
     */
    public static void init() {
        if (initialized)
            return;

        try {
            URL resource = TestCombatBalance.class
                    .getClassLoader()
                    .getResource("combatBalance.json");

            if (resource == null) {
                throw new IllegalStateException("No s'ha trobat combatBalance.json a resources");
            }

            Path path = Paths.get(resource.toURI());

            CombatBalanceConfig balance = CombatBalanceLoader.load(path);
            CombatBalanceRegistry.initialize(balance);

            initialized = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}