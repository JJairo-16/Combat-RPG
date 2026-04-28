package rpgcombat.perks;

import rpgcombat.perks.mission.MissionProgress;

/**
 * Manté l'estat de missions i recompenses pendents d'un jugador en combat.
 */
public final class PlayerPerkState {
    private final MissionProgress mission;
    private boolean pendingChoice;

    /**
     * Crea l'estat del jugador amb una missió assignada.
     *
     * @param mission progrés de la missió
     */
    public PlayerPerkState(MissionProgress mission) {
        this.mission = mission;
    }

    /** @return progrés de la missió */
    public MissionProgress mission() {
        return mission;
    }

    /** @return si hi ha una elecció de perk pendent */
    public boolean pendingChoice() {
        return pendingChoice;
    }

    /** Marca que hi ha una elecció pendent si la missió està completada. */
    public void updatePendingChoice() {
        if (mission != null && mission.completed() && !mission.rewardClaimed()) {
            pendingChoice = true;
        }
    }

    /** Neteja l'estat pendent i marca la recompensa com reclamada. */
    public void clearPendingChoice() {
        pendingChoice = false;
        if (mission != null) mission.markRewardClaimed();
    }
}