package rpgcombat.balance;

import rpgcombat.balance.config.CombatBalanceConfig;

/**
 * Registre global per accedir a l'equilibri de combat.
 */
public final class CombatBalanceRegistry {
    private static CombatBalanceConfig current;

    private CombatBalanceRegistry() {
    }

    /**
     * Inicialitza el registre si encara no està definit.
     *
     * @param config configuració a establir
     */
    public static synchronized void initialize(CombatBalanceConfig config) {
        if (current != null) {
            throw new CombatBalanceException("L'equilibri de combat ja està inicialitzat");
        }

        CombatBalanceValidator.validate(config);
        current = config;
    }

    /**
     * Substitueix la configuració actual.
     *
     * @param config nova configuració
     */
    public static synchronized void replace(CombatBalanceConfig config) {
        CombatBalanceValidator.validate(config);
        current = config;
    }

    /**
     * Retorna la configuració actual.
     *
     * @return configuració activa
     */
    public static CombatBalanceConfig get() {
        if (current == null) {
            throw new CombatBalanceException("L'equilibri de combat no està inicialitzat");
        }
        return current;
    }

    /**
     * Indica si el registre està inicialitzat.
     *
     * @return true si hi ha configuració
     */
    public static boolean isInitialized() {
        return current != null;
    }

    /**
     * Neteja el registre (ús en tests).
     */
    public static synchronized void clearForTests() {
        current = null;
    }
}