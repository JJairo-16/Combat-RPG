package rpgcombat.balance;

/**
 * Error de càrrega o validació del balanceig de combat.
 */
public class CombatBalanceException extends RuntimeException {
    public CombatBalanceException(String message) {
        super(message);
    }

    public CombatBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
