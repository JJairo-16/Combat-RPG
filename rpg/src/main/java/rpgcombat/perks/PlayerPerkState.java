package rpgcombat.perks;

import rpgcombat.perks.mission.MissionProgress;

/** Estat de missions i perks d'un jugador dins el combat. */
public final class PlayerPerkState {
    private final MissionProgress mission;
    private boolean pendingChoice;

    public PlayerPerkState(MissionProgress mission) {
        this.mission = mission;
    }

    public MissionProgress mission() {
        return mission;
    }

    public boolean pendingChoice() {
        return pendingChoice;
    }

    public void updatePendingChoice() {
        if (mission != null && mission.completed() && !mission.rewardClaimed()) {
            pendingChoice = true;
        }
    }

    public void clearPendingChoice() {
        pendingChoice = false;
        if (mission != null) mission.markRewardClaimed();
    }
}
