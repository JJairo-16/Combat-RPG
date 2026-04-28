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

/**
 * Coordina missions i elecció de perks durant el combat.
 */
public final class CombatPerkSystem {
    private final Map<Character, PlayerPerkState> states = new IdentityHashMap<>();
    private final Random rng = new Random();

    /**
     * Assigna una missió inicial a cada jugador.
     */
    public CombatPerkSystem(Character player1, Character player2) {
        states.put(player1, new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng))));
        states.put(player2, new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng))));
    }

    /**
     * Actualitza la missió del personatge després d'un torn.
     */
    public void afterTurn(Character actor, Character opponent, Action actorAction, Action opponentAction,
            TurnResult result, int roundNumber) {
        PlayerPerkState state = states.get(actor);
        if (state == null || state.mission() == null || state.mission().rewardClaimed())
            return;

        state.mission().update(MissionUpdate.from(actor, opponent, actorAction, opponentAction, result, roundNumber));
        state.updatePendingChoice();
    }

    /**
     * Retorna un resum textual de la missió del jugador.
     */
    public String missionSummary(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null || state.mission() == null || state.mission().definition() == null) {
            return "";
        }

        MissionProgress progress = state.mission();

        if (progress.rewardClaimed()) {
            PerkDefinition chosen = state.chosenPerk();
            if (chosen == null) {
                return "Perk\nRecompensa reclamada\nNo hi ha cap perk registrada";
            }

            return "Perk\n" + chosen.name()
                    + "\n" + chosen.description()
                    + "\nActivació: " + triggerLabel(chosen.trigger());
        }

        MissionDefinition mission = progress.definition();
        String status = progress.completed() ? "Completada" : progress.progressText();
        return mission.name() + "\n" + mission.description() + "\nProgrés: " + status;
    }

    /**
     * Resol l'elecció de perk pendent, si n'hi ha.
     */
    public void resolvePendingChoices(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null || !state.pendingChoice())
            return;

        boolean corruptedOnly = player.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        List<PerkDefinition> options = PerkRegistry.rollOptions(corruptedOnly, 12, rng).stream()
                .filter(perk -> !player.hasEffect(PerkEffectFactory.keyFor(perk)))
                .limit(3)
                .toList();
        if (options.isEmpty()) {
            state.clearPendingChoice();
            return;
        }

        PerkDefinition chosen = PerkChoiceMenu.choose(player, options);
        if (chosen != null) {
            Effect effect = PerkEffectFactory.create(chosen);
            player.addEffect(effect);
            state.setChosenPerk(chosen);

            // Defensa explícita: després de triar una perk, el jugador ha de tenir-ne l'efecte actiu.
            if (!player.hasEffect(effect.key())) {
                player.removeEffect(effect.key());
                player.addEffect(effect);
            }
        }
        state.clearPendingChoice();
    }

    /** Etiqueta visible de la fase que activa una perk. */
    private static String triggerLabel(rpgcombat.weapons.passives.HitContext.Phase trigger) {
        if (trigger == null) return "Desconeguda";
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
