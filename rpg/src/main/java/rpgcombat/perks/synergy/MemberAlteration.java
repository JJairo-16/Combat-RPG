package rpgcombat.perks.synergy;

import java.util.List;

import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Alteració aplicada a un perk membre dins d’una sinergia.
 */
public record MemberAlteration(
        String memberPerkId,
        Phase trigger,
        List<Rule> conditions,
        List<Rule> actions,
        String descriptionAppend) {

    /**
     * Normalitza valors nuls a col·leccions buides o text buit.
     */
    public MemberAlteration {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        descriptionAppend = descriptionAppend == null ? "" : descriptionAppend;
    }
}