package rpgcombat.perks.divine;

/** Informació visible de l'estat actual del despertar d'una perk divina en combat. */
public interface DivineAwakeningView {
    /** Identificador de la perk divina. */
    String divinePerkId();

    /** Nom visible amb l'estat de despertar, acolorit segons el déu. */
    String awakenedDisplayName();

    /** Descripció visible compacta amb valors actuals i complets. */
    String awakenedDescription();
}