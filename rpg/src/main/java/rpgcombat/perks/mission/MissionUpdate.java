package rpgcombat.perks.mission;

import java.util.EnumSet;
import java.util.Set;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;

/**
 * Representa la informació d'un torn rellevant per al progrés de missions.
 */
public record MissionUpdate(
        Character owner,
        Character opponent,
        Action ownerAction,
        Action opponentAction,
        TurnResult result,
        int roundNumber,
        Set<MissionEvent> events) {

    /**
     * Crea una actualització a partir de les dades del torn.
     */
    public static MissionUpdate from(Character owner, Character opponent, Action ownerAction, Action opponentAction,
            TurnResult result, int roundNumber) {
        EnumSet<MissionEvent> events = EnumSet.noneOf(MissionEvent.class);

        if (ownerAction == Action.ATTACK) events.add(MissionEvent.ACTION_ATTACK);
        if (ownerAction == Action.DEFEND) events.add(MissionEvent.ACTION_DEFEND);
        if (ownerAction == Action.DODGE) events.add(MissionEvent.ACTION_DODGE);
        if (ownerAction == Action.CHARGE) events.add(MissionEvent.ACTION_CHARGE);

        if (result != null) {
            if (result.damageDealt() > 0) {
                events.add(MissionEvent.HIT);
                events.add(MissionEvent.DAMAGE_DEALT);
            }
            if (result.critical()) events.add(MissionEvent.CRIT);
            if (result.chargedHit()) events.add(MissionEvent.CHARGED_HIT);
            if (result.selfHit()) events.add(MissionEvent.SELF_HIT);
        }

        if (owner != null) {
            if (owner.healthRatio() <= 0.35) events.add(MissionEvent.LOW_HEALTH);
            if (owner.getMomentumStacks() >= 2) events.add(MissionEvent.HIGH_MOMENTUM);
            if (owner.getStatistics().getStamina() / Math.max(1.0, owner.getStatistics().getMaxStamina()) <= 0.40) {
                events.add(MissionEvent.LOW_STAMINA);
            }
            if (owner.isAlive()) events.add(MissionEvent.SURVIVE_TURN);
        }

        return new MissionUpdate(owner, opponent, ownerAction, opponentAction, result, roundNumber, Set.copyOf(events));
    }

    /**
     * Indica si s'ha produït un esdeveniment.
     */
    public boolean has(MissionEvent event) {
        return event != null && events.contains(event);
    }

    /**
     * Retorna el valor associat a un esdeveniment.
     */
    public double amountFor(MissionEvent event) {
        if (event == MissionEvent.DAMAGE_DEALT && result != null) return Math.max(0.0, result.damageDealt());
        return has(event) ? 1.0 : 0.0;
    }
}