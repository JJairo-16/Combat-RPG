package rpgcombat.perks.divine;

import java.util.List;

import rpgcombat.models.breeds.Breed;
import rpgcombat.perks.PerkDefinition;

/**
 * Definició d'una perk divina inicial seleccionable per raça.
 */
public record DivinePerkDefinition(
        PerkDefinition perk,
        String god,
        String shortDescription,
        List<Breed> allowedBreeds,
        int awakeningMaxCharge,
        String awakeningChargeBy,
        String awakeningDescription) {

    /**
     * Normalitza valors nuls i límits.
     */
    public DivinePerkDefinition {
        allowedBreeds = allowedBreeds == null ? List.of() : List.copyOf(allowedBreeds);
        god = god == null ? "" : god;
        shortDescription = shortDescription == null ? "" : shortDescription;
        awakeningMaxCharge = Math.max(0, awakeningMaxCharge);
        awakeningChargeBy = awakeningChargeBy == null ? "" : awakeningChargeBy;
        awakeningDescription = awakeningDescription == null ? "" : awakeningDescription;
    }

    /**
     * Retorna l'identificador de la perk.
     */
    public String id() { return perk.id(); }

    /**
     * Retorna el nom de la perk.
     */
    public String name() { return perk.name(); }

    /**
     * Retorna la descripció de la perk.
     */
    public String description() { return perk.description(); }

    /**
     * Indica si la raça pot utilitzar aquesta perk.
     */
    public boolean isAllowedFor(Breed breed) {
        return breed != null && allowedBreeds.contains(breed);
    }

    /**
     * Retorna el nom formatat per mostrar.
     */
    public String displayName() {
        return name() + " — " + god;
    }
}