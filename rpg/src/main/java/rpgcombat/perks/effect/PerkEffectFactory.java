package rpgcombat.perks.effect;

import rpgcombat.models.effects.Effect;
import rpgcombat.perks.PerkDefinition;

/** Converteix una definició de perk en efecte de combat. */
public final class PerkEffectFactory {
    private PerkEffectFactory() {}

    public static Effect create(PerkDefinition perk) {
        return new ConfigurablePerkEffect(perk);
    }
}
