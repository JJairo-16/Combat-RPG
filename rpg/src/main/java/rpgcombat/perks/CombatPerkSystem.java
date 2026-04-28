package rpgcombat.perks;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.triggers.Chaos;
import rpgcombat.perks.effect.PerkEffectFactory;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionProgress;
import rpgcombat.perks.mission.MissionRegistry;
import rpgcombat.perks.mission.MissionUpdate;

/** Coordina missions i elecció de perks durant el combat. */
public final class CombatPerkSystem {
    private final Map<Character, PlayerPerkState> states = new IdentityHashMap<>();
    private final Random rng = new Random();

    /** Assigna una missió inicial a cada jugador. */
    public CombatPerkSystem(Character player1, Character player2) {
        states.put(player1, new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng))));
        states.put(player2, new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng))));
    }

    /** Actualitza les missions del personatge després d'un torn. */
    public void afterTurn(Character actor, Character opponent, Action actorAction, Action opponentAction,
            TurnResult result, int roundNumber) {
        PlayerPerkState state = states.get(actor);
        if (state == null)
            return;

        MissionUpdate update = MissionUpdate.from(actor, opponent, actorAction, opponentAction, result, roundNumber);
        for (MissionProgress mission : state.missions()) {
            if (!mission.rewardClaimed()) {
                mission.update(update);
            }
        }
        state.updatePendingChoice();
    }

    /** Retorna un resum textual de missions i perks del jugador. */
    public String missionSummary(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null)
            return "";

        StringBuilder sb = new StringBuilder();
        if (!state.missions().isEmpty()) {
            sb.append("Missions");
            
            for (MissionProgress progress : state.missions()) {
                if (progress.definition() == null)
                    continue;
                MissionDefinition mission = progress.definition();
                
                String status;
                if (!progress.completed())
                    status = progress.progressText();
                else if (progress.rewardClaimed())
                    status = "Reclamada";
                else
                    status = "Completada";

                sb.append("\n\n").append(mission.name())
                        .append("\n").append(mission.description())
                        .append("\nProgrés: ").append(status);
            }
        }

        if (!state.perks().isEmpty()) {
            if (!sb.isEmpty())
                sb.append("\n---\n");
            sb.append("Perks");
            for (PerkDefinition chosen : state.perks()) {
                sb.append("\n").append(chosen.name())
                        .append("\n").append(chosen.description())
                        .append("\nActivació: ").append(triggerLabel(chosen.trigger()));
            }
        }

        return sb.toString();
    }

    /** Resol l'elecció de perk pendent, si n'hi ha. */
    public void resolvePendingChoices(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null || !state.pendingChoice())
            return;

        if (!state.canGainMorePerks()) {
            state.clearPendingChoice();
            return;
        }

        boolean corruptedOnly = player.hasEffect(Chaos.INTERNAL_EFFECT_KEY);

        List<PerkDefinition> options = PerkRegistry.rollOptions(corruptedOnly, 24, rng).stream()
                .filter(perk -> !state.hasPerk(perk.id()))
                .filter(perk -> !player.hasEffect(PerkEffectFactory.keyFor(perk)))
                .limit(3)
                .toList();

        if (options.isEmpty()) {
            state.clearPendingChoice();
            return;
        }

        PerkDefinition chosen = PerkChoiceMenu.choose(player, options);

        if (chosen != null && !state.hasPerk(chosen.id())) {
            Effect effect = PerkEffectFactory.create(chosen);

            player.removeEffect(effect.key());
            player.addEffect(effect);

            state.addPerk(chosen);
            state.clearPendingChoice();

            if (state.canGainMorePerks()) {
                MissionDefinition nextMission = MissionRegistry.rollExcluding(rng, state.missionIds());
                if (nextMission != null) {
                    state.addMission(new MissionProgress(nextMission));
                }
            }
        } else {
            state.clearPendingChoice();
        }
    }

    /** Etiqueta visible de la fase que activa una perk. */
    private static String triggerLabel(rpgcombat.weapons.passives.HitContext.Phase trigger) {
        if (trigger == null)
            return "Desconeguda";
        return switch (trigger) {
            case START_TURN -> "Inici de torn";
            case BEFORE_ATTACK -> "Abans d'atacar";
            case ROLL_CRIT -> "Tirada crítica";
            case MODIFY_DAMAGE -> "Modificació de dany";
            case BEFORE_DEFENSE -> "Abans de defensar";
            case AFTER_DEFENSE -> "Després de defensar";
            case AFTER_HIT -> "Després d'impactar";
            case END_TURN -> "Final de torn";
        };
    }
}
