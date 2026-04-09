package rpgcombat.models.characters;

/**
 * Representa el resultat d'una acció sobre un personatge.
 *
 * @param recived Quantitat rebuda (dany o curació)
 * @param message Missatge associat al resultat
 */
public record Result(double recived, String message) {
}