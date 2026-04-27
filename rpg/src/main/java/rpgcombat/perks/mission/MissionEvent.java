package rpgcombat.perks.mission;

/** Esdeveniments simples que pot observar una missió. */
public enum MissionEvent {
    ACTION_ATTACK,
    ACTION_DEFEND,
    ACTION_DODGE,
    ACTION_CHARGE,
    HIT,
    DAMAGE_DEALT,
    CRIT,
    CHARGED_HIT,
    SELF_HIT,
    LOW_HEALTH,
    HIGH_MOMENTUM,
    LOW_STAMINA,
    SURVIVE_TURN
}
