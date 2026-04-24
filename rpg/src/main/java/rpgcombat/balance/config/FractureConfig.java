package rpgcombat.balance.config;

/**
 * Configuració dels paràmetres de fractura.
 *
 * @param minRate taxa mínima d'activació
 * @param maxRate taxa màxima d'activació
 * @param C constant base del càlcul
 * @param n exponent del càlcul
 * @param damageMultiplier multiplicador de dany aplicat
 * @param duration durada de la fractura (en ticks o segons)
 */
public record FractureConfig(
    double minRate,
    double maxRate,
    int C,
    double n,
    double damageMultiplier,
    int duration
) {

}