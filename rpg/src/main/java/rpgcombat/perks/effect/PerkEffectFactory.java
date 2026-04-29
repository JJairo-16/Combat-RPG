package rpgcombat.perks.effect;

import java.util.List;

import rpgcombat.models.effects.Effect;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.synergy.MemberAlteration;
import rpgcombat.perks.synergy.SynergyDefinition;
import rpgcombat.perks.synergy.SynergyLevel;

/** Factoria per crear efectes de combat a partir de perks i sinergies. */
public final class PerkEffectFactory {
    private PerkEffectFactory() {}

    /** Crea un efecte bàsic a partir d’un perk. */
    public static Effect create(PerkDefinition perk) {
        return new ConfigurablePerkEffect(perk);
    }

    /**
     * Crea un efecte amb alteracions de sinergia si n’hi ha.
     */
    public static Effect createAugmented(PerkDefinition perk, List<MemberAlteration> alterations) {
        if (alterations == null || alterations.isEmpty()) return create(perk);
        return new AugmentedPerkEffect(perk, alterations);
    }

    /**
     * Crea un efecte de bonus de sinergia per nivell.
     */
    public static Effect createSynergyBonus(SynergyDefinition synergy, SynergyLevel level) {
        return new SynergyBonusEffect(synergy, level);
    }

    /** Retorna la clau única associada a un perk. */
    public static String keyFor(PerkDefinition perk) {
        return ConfigurablePerkEffect.keyFor(perk);
    }
}