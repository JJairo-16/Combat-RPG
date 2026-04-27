package rpgcombat.perks;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.triggers.Chaos;
import rpgcombat.perks.effect.PerkEffectFactory;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionProgress;
import rpgcombat.perks.mission.MissionRegistry;
import rpgcombat.perks.mission.MissionUpdate;
import rpgcombat.utils.input.Menu;
import rpgcombat.utils.ui.Prettier;

/** Coordina missions i selecció de perks durant el combat. */
public final class CombatPerkSystem {
    private final Map<Character, PlayerPerkState> states = new IdentityHashMap<>();
    private final Random rng = new Random();

    public CombatPerkSystem(Character player1, Character player2) {
        states.put(player1, new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng))));
        states.put(player2, new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng))));
    }

    public void showInitialMissions(Character player1, Character player2) {
        printMission(player1);
        printMission(player2);
        Menu.pause();
    }

    public void afterTurn(Character actor, Character opponent, Action actorAction, Action opponentAction,
            TurnResult result, int roundNumber) {
        PlayerPerkState state = states.get(actor);
        if (state == null || state.mission() == null || state.mission().rewardClaimed()) return;

        state.mission().update(MissionUpdate.from(actor, opponent, actorAction, opponentAction, result, roundNumber));
        state.updatePendingChoice();
    }

    public void resolvePendingChoices(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null || !state.pendingChoice()) return;

        boolean corruptedOnly = player.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        List<PerkDefinition> options = PerkRegistry.rollOptions(corruptedOnly, 3, rng);
        if (options.isEmpty()) {
            state.clearPendingChoice();
            return;
        }

        PerkDefinition chosen = PerkChoiceMenu.choose(player, options);
        if (chosen != null) {
            player.addEffect(PerkEffectFactory.create(chosen));
            Prettier.info(player.getName() + " obté la perk: " + chosen.name() + ".");
            Menu.pause();
        }
        state.clearPendingChoice();
    }

    private void printMission(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null || state.mission() == null || state.mission().definition() == null) return;

        MissionDefinition mission = state.mission().definition();
        Prettier.printTitle("Missió de " + player.getName());
        System.out.println(mission.name());
        System.out.println(mission.description());
        System.out.println();
    }
}
