package rpgcombat.perks.divine;

import java.util.List;
import java.util.Optional;

import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PerkFamily;
import rpgcombat.perks.effect.PerkEffectFactory;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Registre global de perks divines inicials.
 */
public final class DivinePerkRegistry {
    public static final String NO_EFFECT_ID = "DEBUG_NO_DIVINE_PERK";

    /**
     * Perk neutra per a debug.
     */
    private static final DivinePerkDefinition NO_EFFECT = new DivinePerkDefinition(
            new PerkDefinition(
                    NO_EFFECT_ID,
                    "Sense efecte",
                    "Perk divina neutral per a personatges de debug. No modifica el combat.",
                    PerkFamily.DIVINE,
                    Phase.START_TURN,
                    1,
                    List.of(),
                    List.of(),
                    List.of("DIVINE", "DEBUG")),
            "Debug",
            "No modifica el combat.",
            List.of(Breed.values()),
            0,
            "DEBUG",
            "Aquesta opció no desperta ni altera cap valor de combat.");

    private static List<DivinePerkDefinition> perks = List.of();

    private DivinePerkRegistry() {}

    /**
     * Inicialitza el registre amb les perks carregades.
     */
    public static void initialize(List<DivinePerkDefinition> loaded) {
        perks = loaded == null ? List.of() : List.copyOf(loaded);
    }

    /**
     * Retorna totes les perks registrades.
     */
    public static List<DivinePerkDefinition> all() {
        return perks;
    }

    /**
     * Retorna les perks disponibles per a una raça.
     */
    public static List<DivinePerkDefinition> availableFor(Breed breed) {
        return perks.stream().filter(p -> p.isAllowedFor(breed)).toList();
    }

    /**
     * Cerca una perk pel seu id.
     */
    public static Optional<DivinePerkDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        if (NO_EFFECT_ID.equals(id)) return Optional.of(NO_EFFECT);
        return perks.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Retorna una perk per defecte per a la raça.
     */
    public static DivinePerkDefinition defaultFor(Breed breed) {
        List<DivinePerkDefinition> available = availableFor(breed);
        return available.isEmpty() ? null : available.get(0);
    }

    /**
     * Crea l'efecte associat a una perk.
     */
    public static Optional<Effect> create(String id) {
        return find(id).map(DivinePerkDefinition::perk).map(PerkEffectFactory::create);
    }

    /**
     * Retorna la perk activa d'un personatge.
     */
    public static Optional<DivinePerkDefinition> activeFor(Character character) {
        if (character == null) return Optional.empty();
        if (character.hasEffect(PerkEffectFactory.keyFor(NO_EFFECT.perk()))) return Optional.of(NO_EFFECT);
        return perks.stream()
                .filter(perk -> character.hasEffect(PerkEffectFactory.keyFor(perk.perk())))
                .findFirst();
    }
}